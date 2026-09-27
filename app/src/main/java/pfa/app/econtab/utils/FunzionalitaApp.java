package pfa.app.econtab.utils;

import android.content.Context;

import pfa.app.econtab.api.TokenManager;
import pfa.app.econtab.db.table.AbstractTable;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Rapportini;

/**
 * Funzionalità dell'app concesse dal pacchetto per il ruolo (PACCHETTI_E_FUNZIONALITA.md, fase 6): arrivano dal server
 * al login e a ogni apertura (campo "funzionalita"). Il server le applica comunque nella sync; qui servono a non
 * mostrare cio' che non si puo' fare. Finche' il server non ne ha mai mandate (server vecchio) e' tutto consentito.
 */
public final class FunzionalitaApp {

    public static final String CLIENTI_CREA = "CLIENTI.CREA";
    public static final String CANTIERI_CREA = "CANTIERI.CREA";
    public static final String RAPPORTINI_CREA = "RAPPORTINI.CREA";
    public static final String RAPPORTINI_ALTRI = "RAPPORTINI.ALTRI";
    public static final String RAPPORTINI_COSTI = "RAPPORTINI.COSTI";
    public static final String DASHBOARD_OGGI = "DASHBOARD.OGGI";

    private FunzionalitaApp() {
    }

    public static boolean ha(Context ctx, String codice) {
        return TokenManager.getInstance(ctx).haFunzionalita(codice);
    }

    /** Funzionalita' richiesta per inserire un record della tabella (null = nessuna). */
    public static String perInserimento(AbstractTable tabella) {
        if (tabella instanceof Anagrafica) return CLIENTI_CREA;
        if (tabella instanceof Cantieri) return CANTIERI_CREA;
        if (tabella instanceof Rapportini) return RAPPORTINI_CREA;
        return null;
    }

    /** L'utente puo' inserire record della tabella (vedi perInserimento). */
    public static boolean puoInserire(Context ctx, AbstractTable tabella) {
        String codice = perInserimento(tabella);
        return codice == null || ha(ctx, codice);
    }
}
