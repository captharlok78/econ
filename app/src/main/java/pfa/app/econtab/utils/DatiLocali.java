package pfa.app.econtab.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import pfa.app.econtab.SincronizzazioneActivity;
import pfa.app.econtab.api.TokenManager;
import pfa.app.econtab.db.DbInterno;

/**
 * A chi appartengono i dati sul dispositivo (account e ditta dell'ultima sincronizzazione). Se entra un account o una
 * ditta diversi i dati locali (clienti, cantieri, rapportini…) vanno svuotati e riscaricati: la sync scarica solo le
 * novita', quindi i dati della ditta precedente resterebbero visibili, e le modifiche non inviate finirebbero nella ditta
 * sbagliata. Un account vale per una sola ditta (ACCESSI_E_LICENZE.md §11).
 */
public final class DatiLocali {

    private static final String PREFS = "DATI_LOCALI";
    private static final String K_PROPRIETARIO = "proprietario";

    private DatiLocali() {
    }

    /** "idUtente@idDitta" dell'account collegato, null se non c'e' un accesso. */
    public static String accountCorrente(Context ctx) {
        TokenManager tm = TokenManager.getInstance(ctx);
        if (!tm.hasToken() || tm.getUserId() <= 0 || tm.getIdDitta() <= 0) return null;
        return tm.getUserId() + "@" + tm.getIdDitta();
    }

    /** Proprietario dei dati locali, null se non ancora noto (app precedente a questo controllo). */
    public static String proprietario(Context ctx) {
        return prefs(ctx).getString(K_PROPRIETARIO, null);
    }

    public static void segna(Context ctx, String account) {
        prefs(ctx).edit().putString(K_PROPRIETARIO, account).apply();
    }

    /** Modifiche locali non ancora inviate al server (righe con in_server = 0 nelle tabelle che si inviano). */
    public static int modificheNonInviate(Context ctx) {
        int totale = 0;
        DbInterno db = new DbInterno(ctx);
        try {
            SQLiteDatabase sqlite = db.getReadableDatabase();
            for (String t : SincronizzazioneActivity.tabelleInviate()) {
                if (!haColonna(sqlite, t, "in_server")) continue;
                try (Cursor c = sqlite.rawQuery("SELECT COUNT(*) FROM " + t + " WHERE in_server = 0", null)) {
                    if (c.moveToFirst()) totale += c.getInt(0);
                }
            }
        } finally {
            db.close();
        }
        return totale;
    }

    /**
     * Svuota i dati scaricati dal server e azzera gli allineamenti (sync, catalogo, dati ditta): la prossima
     * sincronizzazione riscarica tutto per l'account collegato. Gli account locali con password restano.
     */
    public static void svuota(Context ctx) {
        DbInterno db = new DbInterno(ctx);
        try {
            SQLiteDatabase sqlite = db.getWritableDatabase();
            sqlite.beginTransaction();
            try {
                for (String t : SincronizzazioneActivity.tabelleScaricate()) {
                    if (!esiste(sqlite, t)) continue;
                    if (SyncUtil.TABELLA_OPERATORI.equals(t)) {
                        sqlite.execSQL("DELETE FROM " + t + " WHERE password IS NULL OR password = ''");
                    } else {
                        sqlite.execSQL("DELETE FROM " + t);
                    }
                }
                sqlite.setTransactionSuccessful();
            } finally {
                sqlite.endTransaction();
            }
        } finally {
            db.close();
        }
        ctx.getSharedPreferences("ECONTAB", Context.MODE_PRIVATE).edit().remove("DATA_ULTIMA_SINCRONIZZAZIONE").apply();
        ctx.getSharedPreferences("CATALOGO_LOCALE", Context.MODE_PRIVATE).edit().clear().apply();
        DittaLocale.cancella(ctx);
    }

    private static boolean esiste(SQLiteDatabase db, String tabella) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", new String[]{tabella})) {
            return c.moveToFirst();
        }
    }

    private static boolean haColonna(SQLiteDatabase db, String tabella, String colonna) {
        if (!esiste(db, tabella)) return false;
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + tabella + ")", null)) {
            while (c.moveToNext()) {
                if (colonna.equalsIgnoreCase(c.getString(1))) return true;
            }
        }
        return false;
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
