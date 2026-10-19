package com.ncfsistemi.econ.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import com.ncfsistemi.econ.BuildConfig;
import com.ncfsistemi.econ.servizi.InstallazioneAggiornamento;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Aggiornamento dell'app dal server (AGGIORNAMENTO_APP.md §3): avviso con versione e note, conferma dell'utente,
 * scaricamento dell'APK con l'avanzamento, controllo dello SHA-256 e installazione con il PackageInstaller di Android
 * (che chiede ancora un tocco su "Aggiorna"). La prima volta Android chiede anche il permesso "installa app sconosciute"
 * per Econ: si apre la sua pagina e, al ritorno nell'activity, riprendi() continua.
 */
public final class AggiornaApp {

    public interface Rinuncia {
        /** L'utente ha scelto "Piu' tardi" (solo se l'aggiornamento non e' obbligatorio) o l'aggiornamento non e' riuscito. */
        void continua();
    }

    private final Activity activity;
    private final ControlloAvvio.Aggiornamento agg;
    private final Rinuncia rinuncia;
    /** In attesa del ritorno dalla pagina "installa app sconosciute". */
    private boolean attesaPermesso = false;

    private AggiornaApp(Activity activity, ControlloAvvio.Aggiornamento agg, Rinuncia rinuncia) {
        this.activity = activity;
        this.agg = agg;
        this.rinuncia = rinuncia;
    }

    /** Propone l'aggiornamento se ce n'e' uno; restituisce null (e non chiama rinuncia) se non c'e' niente da fare. */
    public static AggiornaApp proponi(Activity activity, ControlloAvvio.Aggiornamento agg, Rinuncia rinuncia) {
        if (agg == null || !agg.aggiornamento || agg.url == null) return null;
        AggiornaApp a = new AggiornaApp(activity, agg, rinuncia);
        a.mostraAvviso();
        return a;
    }

    private void mostraAvviso() {
        String installata = VersioneApp.salvata(activity);
        StringBuilder msg = new StringBuilder();
        msg.append("È disponibile iEcon ").append(agg.versione);
        msg.append(installata != null ? " (questa è la " + installata + ")" : "").append(".");
        if (agg.dimensione > 0) msg.append("\nDimensione: ").append(megabyte(agg.dimensione)).append(".");
        if (agg.note != null && !agg.note.trim().isEmpty()) msg.append("\n\n").append(agg.note.trim());
        if (agg.obbligatorio) msg.append("\n\nL'aggiornamento è obbligatorio: senza non si può usare l'app.");
        msg.append("\n\nI dati sul dispositivo restano.");

        AlertDialog.Builder b = new AlertDialog.Builder(activity)
                .setTitle(agg.obbligatorio ? "Aggiornamento obbligatorio" : "Aggiornamento disponibile")
                .setMessage(msg.toString())
                .setCancelable(false)
                .setPositiveButton("Aggiorna", (d, w) -> avvia());
        if (agg.obbligatorio) {
            b.setNegativeButton("Esci", (d, w) -> activity.finishAffinity());
        } else {
            b.setNegativeButton("Più tardi", (d, w) -> rinuncia.continua());
        }
        b.show();
    }

    /** Da chiamare in onResume dell'activity: dopo la pagina del permesso si riprova. */
    public void riprendi() {
        if (!attesaPermesso) return;
        attesaPermesso = false;
        avvia();
    }

    private void avvia() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Permesso necessario")
                    .setMessage("Per aggiornarsi iEcon deve poter installare app: nella pagina che si apre attiva "
                            + "\"Consenti da questa fonte\", poi torna indietro.")
                    .setCancelable(false)
                    .setPositiveButton("Apri", (d, w) -> {
                        attesaPermesso = true;
                        activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + activity.getPackageName())));
                    })
                    .setNegativeButton(agg.obbligatorio ? "Esci" : "Più tardi", (d, w) -> {
                        if (agg.obbligatorio) activity.finishAffinity();
                        else rinuncia.continua();
                    })
                    .show();
            return;
        }
        scarica();
    }

    @SuppressWarnings("deprecation")
    private void scarica() {
        ProgressDialog pd = new ProgressDialog(activity);
        pd.setTitle("Aggiornamento");
        pd.setMessage("Scaricamento di iEcon " + agg.versione + "...");
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setMax(100);
        pd.setCancelable(false);
        pd.show();
        Handler main = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            String errore = null;
            File apk = new File(new File(activity.getCacheDir(), "aggiornamento"), "econ-" + agg.versionCode + ".apk");
            try {
                if (!(apk.isFile() && agg.sha256 != null && agg.sha256.equalsIgnoreCase(sha256(apk)))) {
                    scaricaFile(apk, perc -> main.post(() -> pd.setProgress(perc)));
                    if (agg.sha256 != null && !agg.sha256.equalsIgnoreCase(sha256(apk))) {
                        //noinspection ResultOfMethodCallIgnored
                        apk.delete();
                        throw new IllegalStateException("il file scaricato non corrisponde (SHA-256): riprova");
                    }
                }
                main.post(() -> pd.setMessage("Installazione..."));
                installa(apk);
            } catch (Exception e) {
                errore = e.getMessage() != null ? e.getMessage() : e.toString();
            }
            final String err = errore;
            main.post(() -> {
                if (!activity.isFinishing()) pd.dismiss();
                if (err != null) fallito(err);
                // se riuscito, Android mostra la sua conferma (InstallazioneAggiornamento) e poi sostituisce l'app
            });
        }, "aggiorna-app").start();
    }

    private void fallito(String errore) {
        new AlertDialog.Builder(activity)
                .setTitle("Aggiornamento non riuscito")
                .setMessage(errore)
                .setCancelable(false)
                .setPositiveButton("Riprova", (d, w) -> avvia())
                .setNegativeButton(agg.obbligatorio ? "Esci" : "Continua", (d, w) -> {
                    if (agg.obbligatorio) activity.finishAffinity();
                    else rinuncia.continua();
                })
                .show();
    }

    private interface Avanzamento {
        void percentuale(int p);
    }

    private void scaricaFile(File dest, Avanzamento av) throws Exception {
        File cartella = dest.getParentFile();
        if (cartella != null && !cartella.isDirectory() && !cartella.mkdirs()) {
            throw new IllegalStateException("impossibile creare " + cartella);
        }
        // tolti gli APK di versioni precedenti
        File[] vecchi = cartella != null ? cartella.listFiles() : null;
        if (vecchi != null) for (File f : vecchi) if (!f.equals(dest)) //noinspection ResultOfMethodCallIgnored
            f.delete();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        try (Response resp = client.newCall(new Request.Builder().url(agg.url).build()).execute()) {
            ResponseBody body = resp.body();
            if (!resp.isSuccessful() || body == null) {
                throw new IllegalStateException("il server ha risposto " + resp.code());
            }
            long totale = body.contentLength() > 0 ? body.contentLength() : agg.dimensione;
            try (InputStream in = body.byteStream(); OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[64 * 1024];
                long letti = 0;
                int n, ultima = -1;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    letti += n;
                    if (totale > 0) {
                        int p = (int) (letti * 100 / totale);
                        if (p != ultima) {
                            ultima = p;
                            av.percentuale(p);
                        }
                    }
                }
            }
        }
    }

    private void installa(File apk) throws Exception {
        PackageInstaller pi = activity.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(activity.getPackageName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // senza il tocco su "Aggiorna" quando Android lo consente (l'app si e' gia' aggiornata da se' una volta)
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        }
        int id = pi.createSession(params);
        try (PackageInstaller.Session s = pi.openSession(id)) {
            try (InputStream in = new FileInputStream(apk); OutputStream out = s.openWrite("econ.apk", 0, apk.length())) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                s.fsync(out);
            }
            Intent esito = new Intent(activity, InstallazioneAggiornamento.class);
            int flag = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0);
            PendingIntent p = PendingIntent.getBroadcast(activity, id, esito, flag);
            s.commit(p.getIntentSender());
        }
    }

    private static String sha256(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format(Locale.ROOT, "%02x", b));
        return sb.toString();
    }

    private static String megabyte(long byte_) {
        return String.format(Locale.ITALY, "%.1f MB", byte_ / 1048576.0);
    }

    /** Versione di questa build per i messaggi: quella del server se nota, altrimenti il versionCode. */
    public static String questaVersione(Activity a) {
        String v = VersioneApp.salvata(a);
        return v != null ? v : "build " + BuildConfig.VERSION_CODE;
    }

    /** Messaggio breve per Stato sistema › Cerca aggiornamenti quando non c'e' niente di nuovo. */
    public static void nessunAggiornamento(Activity a, ControlloAvvio.Aggiornamento agg) {
        String msg = agg == null ? "Il server non ha risposto." :
                !agg.disponibile ? "Sul server non c'è ancora un'app pubblicata." :
                        "iEcon è aggiornata (" + questaVersione(a) + ").";
        Toast.makeText(a, msg, Toast.LENGTH_LONG).show();
    }
}
