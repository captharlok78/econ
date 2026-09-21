package pfa.app.econtab.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import pfa.app.econtab.api.MercuryApiService;
import retrofit2.Response;

/**
 * Copia locale dei dati della ditta (anagrafica, note predefinite, logo), usata dai rapportini e dal
 * modale account. Si aggiorna nel sync di download: l'app manda la versione che possiede e il server
 * risponde con i dati solo se sono cambiati; il logo si riscarica solo se e' cambiato il nome file.
 */
public final class DittaLocale {

    private static final String TAG        = "DittaLocale";
    private static final String PREFS      = "DITTA_LOCALE";
    private static final String K_VERSIONE = "versione";
    private static final String K_DATI     = "dati";
    private static final String FILE_LOGO  = "logo_ditta";

    public enum Esito { INVARIATA, AGGIORNATA }

    private DittaLocale() {}

    /**
     * Allinea i dati locali col server. Blocca sulla rete: chiamare da un thread di background.
     * In caso di errore lancia l'eccezione e lascia invariati i dati locali (la versione si salva solo
     * a logo scaricato, cosi' al prossimo sync si riprova).
     */
    public static Esito sincronizza(Context ctx, MercuryApiService api) throws Exception {
        Response<MercuryApiService.DittaSyncResponse> resp = api.getDittaSync(versioneDaInviare(ctx)).execute();
        if (!resp.isSuccessful() || resp.body() == null) {
            throw new Exception("HTTP " + resp.code());
        }
        MercuryApiService.DittaSyncResponse body = resp.body();
        if (body.invariata || body.ditta == null) {
            return Esito.INVARIATA;
        }

        MercuryApiService.DittaDati nuovi = body.ditta;
        MercuryApiService.DittaDati attuali = getDati(ctx);
        String logoAttuale = attuali != null ? attuali.logo : null;

        if (nuovi.logo == null) {
            fileLogo(ctx).delete();
        } else if (!nuovi.logo.equals(logoAttuale) || !fileLogo(ctx).isFile()) {
            scaricaLogo(ctx, api);
        }

        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(K_VERSIONE, body.versione)
                .putString(K_DATI, new Gson().toJson(nuovi))
                .apply();
        Log.i(TAG, "Dati ditta aggiornati (" + nuovi.ragioneSociale + ")");
        return Esito.AGGIORNATA;
    }

    /** Dati ditta salvati, null se il download non e' ancora avvenuto. */
    public static MercuryApiService.DittaDati getDati(Context ctx) {
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(K_DATI, null);
        return json == null ? null : new Gson().fromJson(json, MercuryApiService.DittaDati.class);
    }

    /** Logo della ditta, null se assente o non decodificabile. */
    public static Bitmap getLogo(Context ctx) {
        File f = fileLogo(ctx);
        return f.isFile() ? BitmapFactory.decodeFile(f.getAbsolutePath()) : null;
    }

    /** File del logo (per stampe/PDF), null se assente. */
    public static File getFileLogo(Context ctx) {
        File f = fileLogo(ctx);
        return f.isFile() ? f : null;
    }

    /** Da chiamare quando l'account/la ditta cambia o si fa logout. */
    public static void cancella(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
        fileLogo(ctx).delete();
    }

    /** Versione locale; vuota se non c'e' o se manca il file del logo atteso, cosi' il server rimanda i dati. */
    private static String versioneDaInviare(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String versione = prefs.getString(K_VERSIONE, null);
        MercuryApiService.DittaDati dati = getDati(ctx);
        if (versione == null || dati == null || (dati.logo != null && !fileLogo(ctx).isFile())) {
            return "";
        }
        return versione;
    }

    private static void scaricaLogo(Context ctx, MercuryApiService api) throws Exception {
        Response<ResponseBody> resp = api.getLogoDitta().execute();
        if (!resp.isSuccessful() || resp.body() == null) {
            throw new Exception("Download logo: HTTP " + resp.code());
        }
        // scrive su file temporaneo e sostituisce a scaricamento completato: un download interrotto non corrompe il logo in uso
        File tmp = new File(ctx.getFilesDir(), FILE_LOGO + ".tmp");
        try (InputStream in = resp.body().byteStream(); OutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        File dest = fileLogo(ctx);
        if (!tmp.renameTo(dest)) {
            tmp.delete();
            throw new Exception("Impossibile salvare il logo");
        }
    }

    private static File fileLogo(Context ctx) {
        return new File(ctx.getFilesDir(), FILE_LOGO);
    }
}
