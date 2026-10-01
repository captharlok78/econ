package pfa.app.econtab.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Preferenze del dispositivo (Impostazioni › Preferenze del dispositivo, STATO_SISTEMA_APP.md §5): valgono solo su questo
 * dispositivo e non vanno al server. Ogni preferenza ha valori fissi (VALORI_*) con le etichette (ETICHETTE_*).
 */
public final class PreferenzeDispositivo {

    public static final String SYNC_AUTOMATICA = "PREF_SYNC_AUTOMATICA_MIN";
    public static final String LISTE_SUBITO = "PREF_LISTE_SUBITO";
    public static final String RIGHE_PAGINA = "PREF_RIGHE_PAGINA";
    public static final String PERIODO_RAPPORTINI = "PREF_PERIODO_RAPPORTINI";

    /** Minuti fra due sincronizzazioni automatiche (0 = spenta; WorkManager non scende sotto i 15). */
    public static final String[] VALORI_SYNC = {"0", "15", "30", "60"};
    public static final String[] ETICHETTE_SYNC = {"Spenta (solo a mano e all'apertura)", "Ogni 15 minuti", "Ogni 30 minuti", "Ogni ora"};

    public static final String[] VALORI_LISTE = {"0", "1"};
    public static final String[] ETICHETTE_LISTE = {"Solo i filtri: i dati dopo Cerca", "Subito i risultati"};

    public static final String[] VALORI_RIGHE = {"10", "20", "50", "100"};
    public static final String[] ETICHETTE_RIGHE = {"10 righe", "20 righe", "50 righe", "100 righe"};

    /** Stessi codici dello spinner dei rapportini (RapportiniActivity). */
    public static final String[] VALORI_PERIODO = {"S", "M", "3M", "6M", "A", ""};
    public static final String[] ETICHETTE_PERIODO = {"Ultima settimana", "Ultimo mese", "Ultimi 3 mesi", "Ultimi 6 mesi", "Ultimo anno", "Sempre"};

    private PreferenzeDispositivo() {
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
    }

    public static String leggi(Context ctx, String chiave, String predefinito) {
        return prefs(ctx).getString(chiave, predefinito);
    }

    public static void salva(Context ctx, String chiave, String valore) {
        prefs(ctx).edit().putString(chiave, valore).apply();
        if (SYNC_AUTOMATICA.equals(chiave)) {
            applicaSyncAutomatica(ctx);
        }
    }

    public static int minutiSyncAutomatica(Context ctx) {
        try {
            return Integer.parseInt(leggi(ctx, SYNC_AUTOMATICA, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean listeSubito(Context ctx) {
        return "1".equals(leggi(ctx, LISTE_SUBITO, "0"));
    }

    /** Righe per pagina delle liste; senza preferenza quelle della lista. */
    public static int righePerPagina(Context ctx, int predefinito) {
        try {
            return Integer.parseInt(leggi(ctx, RIGHE_PAGINA, String.valueOf(predefinito)));
        } catch (NumberFormatException e) {
            return predefinito;
        }
    }

    public static String periodoRapportini(Context ctx) {
        return leggi(ctx, PERIODO_RAPPORTINI, "S");
    }

    /** Pianifica o toglie la sincronizzazione in background secondo la preferenza (anche all'apertura dell'app). */
    public static void applicaSyncAutomatica(Context ctx) {
        int minuti = minutiSyncAutomatica(ctx);
        if (minuti <= 0) {
            pfa.app.econtab.servizi.SyncWorker.cancelSync(ctx);
        } else {
            pfa.app.econtab.servizi.SyncWorker.schedulePeriodicSync(ctx, minuti);
        }
    }

    /** Etichetta del valore salvato (o del predefinito). */
    public static String etichetta(Context ctx, String chiave, String predefinito, String[] valori, String[] etichette) {
        String v = leggi(ctx, chiave, predefinito);
        for (int i = 0; i < valori.length; i++) {
            if (valori[i].equals(v)) return etichette[i];
        }
        return v;
    }
}
