package com.ncfsistemi.econ.utils;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Data e ora dell'ultimo download e dell'ultimo upload riusciti (manuali, di avvio o in background), per la schermata
 * Sincronizzazione (SincronizzaActivity) e la barra in fondo al menu. Ora del tablet.
 */
public final class UltimeSync {

    private static final String DOWNLOAD = "ULTIMO_DOWNLOAD_MS";
    private static final String UPLOAD = "ULTIMO_UPLOAD_MS";

    private UltimeSync() {
    }

    public static void download(Context ctx) {
        salva(ctx, DOWNLOAD);
    }

    public static void upload(Context ctx) {
        salva(ctx, UPLOAD);
    }

    /** "01/10/2026 11:34", "Mai" se non e' mai avvenuto. */
    public static String etichettaDownload(Context ctx) {
        return etichetta(ctx, DOWNLOAD);
    }

    public static String etichettaUpload(Context ctx) {
        return etichetta(ctx, UPLOAD);
    }

    /** Per la barra in fondo al menu: "Download 16/10 11:34" e "Upload 16/10 11:30" su due righe, "Mai" se non e' mai avvenuto. */
    public static String riepilogoBreve(Context ctx) {
        return "Download " + breve(ctx, DOWNLOAD) + "\nUpload " + breve(ctx, UPLOAD);
    }

    private static String breve(Context ctx, String chiave) {
        long ms = ctx.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE).getLong(chiave, 0);
        return ms == 0 ? "Mai" : new SimpleDateFormat("dd/MM HH:mm", Locale.ITALY).format(new Date(ms));
    }

    private static void salva(Context ctx, String chiave) {
        ctx.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE).edit().putLong(chiave, System.currentTimeMillis()).apply();
    }

    private static String etichetta(Context ctx, String chiave) {
        long ms = ctx.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE).getLong(chiave, 0);
        return ms == 0 ? "Mai" : new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(new Date(ms));
    }
}
