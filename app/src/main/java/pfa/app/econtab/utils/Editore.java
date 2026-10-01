package pfa.app.econtab.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import pfa.app.econtab.BuildConfig;
import pfa.app.econtab.api.MercuryApiService;

/**
 * Editore dell'app (EDITORE_E_FIRMA.md): nome, sito ed e-mail di assistenza. Valori predefiniti nella build
 * (BuildConfig.EDITORE_*); il server li manda con la sua versione (GET api/version/mercury) e l'app ricorda gli ultimi,
 * cosi' si cambiano senza una nuova versione dell'app.
 */
public final class Editore {

    private static final String NOME = "EDITORE_NOME", SITO = "EDITORE_SITO", EMAIL = "EDITORE_EMAIL_ASSISTENZA",
            TELEFONO = "EDITORE_TELEFONO_ASSISTENZA";

    private Editore() {
    }

    public static String nome(Context ctx) {
        return leggi(ctx, NOME, BuildConfig.EDITORE_NOME);
    }

    public static String sito(Context ctx) {
        return leggi(ctx, SITO, BuildConfig.EDITORE_SITO);
    }

    public static String emailAssistenza(Context ctx) {
        return leggi(ctx, EMAIL, BuildConfig.EDITORE_EMAIL_ASSISTENZA);
    }

    /** Telefono dell'assistenza, "" se non impostato (allora non si propone la chiamata). */
    public static String telefonoAssistenza(Context ctx) {
        return leggi(ctx, TELEFONO, BuildConfig.EDITORE_TELEFONO_ASSISTENZA).trim();
    }

    /** Apre il compositore con il numero dell'assistenza (senza chiamare da solo). */
    public static void chiamaAssistenza(Context ctx) {
        ctx.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + telefonoAssistenza(ctx).replace(" ", ""))));
    }

    /** Salva i valori arrivati dal server (quelli vuoti restano i precedenti; il telefono si puo' anche togliere). */
    public static void salva(Context ctx, MercuryApiService.VersionResponse v) {
        if (v == null) return;
        android.content.SharedPreferences.Editor e = ctx.getApplicationContext()
                .getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE).edit();
        if (v.editore != null && !v.editore.isEmpty()) e.putString(NOME, v.editore);
        if (v.sito != null && !v.sito.isEmpty()) e.putString(SITO, v.sito);
        if (v.emailAssistenza != null && !v.emailAssistenza.isEmpty()) e.putString(EMAIL, v.emailAssistenza);
        if (v.telefonoAssistenza != null) e.putString(TELEFONO, v.telefonoAssistenza);
        e.apply();
    }

    /** Apre il sito dell'editore nel browser. */
    public static void apriSito(Context ctx) {
        String s = sito(ctx);
        if (!s.startsWith("http")) s = "https://" + s;
        ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(s)));
    }

    /** E-mail all'assistenza con oggetto e, in fondo, i dati del dispositivo e delle versioni. */
    public static void scriviAssistenza(Context ctx, String oggetto, String testo) {
        String corpo = (testo == null ? "" : testo + "\n\n") + "—\n"
                + "Dispositivo: " + DatiDispositivo.marca() + " " + Build.MODEL + " · " + DatiDispositivo.sistema() + "\n"
                + "App: " + VersioneApp.etichetta(ctx) + " · Server: " + (VersioneApp.server(ctx) != null ? VersioneApp.server(ctx) : "—") + "\n"
                + "ID dispositivo: " + DatiDispositivo.seriale(ctx);
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAssistenza(ctx)});
        i.putExtra(Intent.EXTRA_SUBJECT, oggetto);
        i.putExtra(Intent.EXTRA_TEXT, corpo);
        ctx.startActivity(Intent.createChooser(i, "Scrivi all'assistenza"));
    }

    private static String leggi(Context ctx, String chiave, String predefinito) {
        return ctx.getApplicationContext().getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE).getString(chiave, predefinito);
    }
}
