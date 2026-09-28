package pfa.app.econtab.utils;

import android.content.ContentValues;
import android.content.Context;

import pfa.app.econtab.api.TokenManager;
import pfa.app.econtab.db.table.AbstractTable;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Listini;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.Rapportini;

/**
 * Funzionalità dell'app concesse dal pacchetto per il ruolo (PACCHETTI_E_FUNZIONALITA.md, RUOLI_E_CRUD.md): arrivano dal
 * server al login e a ogni apertura (campo "funzionalita"). Il server le applica comunque nella sync; qui servono a non
 * mostrare cio' che non si puo' fare. Finche' il server non ne ha mai mandate (server vecchio) e' tutto consentito.
 * <p>
 * CRUD: "MODULO.CREA", "MODULO.MODIFICA", "MODULO.ELIMINA" per Clienti, Cantieri, Listini, Preventivi, Ordini e
 * Rapportini. LEGGI non serve qui: senza, il server toglie il modulo dall'elenco dei moduli concessi.
 */
public final class FunzionalitaApp {

    public static final String CLIENTI_CREA = "CLIENTI.CREA";
    public static final String CANTIERI_CREA = "CANTIERI.CREA";
    public static final String RAPPORTINI_CREA = "RAPPORTINI.CREA";
    public static final String RAPPORTINI_MODIFICA = "RAPPORTINI.MODIFICA";
    public static final String RAPPORTINI_ALTRI = "RAPPORTINI.ALTRI";
    public static final String RAPPORTINI_COSTI = "RAPPORTINI.COSTI";
    public static final String DASHBOARD_OGGI = "DASHBOARD.OGGI";

    public static final String CLIENTI = "CLIENTI";
    public static final String CANTIERI = "CANTIERI";
    public static final String LISTINI = "LISTINI";
    public static final String PREVENTIVI = "PREVENTIVI";
    public static final String ORDINI = "ORDINI";
    public static final String RAPPORTINI = "RAPPORTINI";

    public static final String CREA = "CREA";
    public static final String MODIFICA = "MODIFICA";
    public static final String ELIMINA = "ELIMINA";

    private FunzionalitaApp() {
    }

    public static boolean ha(Context ctx, String codice) {
        return TokenManager.getInstance(ctx).haFunzionalita(codice);
    }

    /** "MODULO.AZIONE" (es. CLIENTI.MODIFICA). */
    public static String codice(String modulo, String azione) {
        return modulo + "." + azione;
    }

    /**
     * Modulo delle CRUD della tabella (null = nessun controllo). Per i preventivi serve il record (o il tipo): quelli di
     * tipo ordine sono del modulo Ordini; senza record si intende Preventivi.
     */
    public static String modulo(AbstractTable tabella, ContentValues record) {
        if (tabella instanceof Anagrafica) return CLIENTI;
        if (tabella instanceof Cantieri) return CANTIERI;
        if (tabella instanceof Listini) return LISTINI;
        if (tabella instanceof Rapportini) return RAPPORTINI;
        if (tabella instanceof Preventivi) {
            String tipo = record != null ? record.getAsString(Preventivi.TIPO) : null;
            return moduloPreventivo(tipo);
        }
        return null;
    }

    /** Modulo di un preventivo dal suo tipo (O = ordine). */
    public static String moduloPreventivo(String tipo) {
        return Preventivi.TIPO_ORDINE.equals(tipo) ? ORDINI : PREVENTIVI;
    }

    /**
     * Funzionalita' richiesta per inserire un record della tabella (null = nessuna); tipoPreventivo distingue gli ordini
     * (null = preventivo).
     */
    public static String perInserimento(AbstractTable tabella, String tipoPreventivo) {
        String modulo = tabella instanceof Preventivi ? moduloPreventivo(tipoPreventivo) : modulo(tabella, null);
        return modulo != null ? codice(modulo, CREA) : null;
    }

    /** L'utente puo' inserire record della tabella (vedi perInserimento). */
    public static boolean puoInserire(Context ctx, AbstractTable tabella, String tipoPreventivo) {
        String codice = perInserimento(tabella, tipoPreventivo);
        return codice == null || ha(ctx, codice);
    }

    /** L'utente puo' eliminare il record della tabella. */
    public static boolean puoEliminare(Context ctx, AbstractTable tabella, ContentValues record) {
        String modulo = modulo(tabella, record);
        return modulo == null || ha(ctx, codice(modulo, ELIMINA));
    }

    /** L'utente puo' modificare i record del modulo (null = nessun controllo). */
    public static boolean puoModificare(Context ctx, String modulo) {
        return modulo == null || ha(ctx, codice(modulo, MODIFICA));
    }
}
