package com.ncfsistemi.econ.db.table;

import android.content.ContentValues;

import java.util.ArrayList;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.utils.Sessione;

public class Rapportini extends AbstractTable {
	public static final String NOME_TABELLA = "rapportini";


	/** Dallo schema 26 la tabella usa i nomi del server (GESTIONE_RAPPORTINI.md §11.4): chiave "id". */
	public static final String ID = "id";
    public static final String ID_DITTA = "id_ditta";
    /**
     * Cliente, cantiere e ordine: facoltativi ma almeno uno. Con l'ordine il cantiere e' quello dell'ordine, con il
     * cantiere il cliente e' quello del cantiere (lo stesso fa il server, Rapportini::allineaRiferimenti()).
     */
    public static final String ID_CLIENTE = "id_cliente";
    public static final String ID_CANTIERE = "id_cantiere";
    public static final String ID_ORDINE = "id_ordine";
    /** Autore del rapportino come persona della ditta (utenti.id_utente_ditta). */
    public static final String ID_UTENTE_DITTA = "id_utente_ditta";
    /** Stato del documento (stati_documento, ambito "rapportini"): decide se si modifica o si cancella. */
    public static final String ID_STATO = "id_stato";
    public static final String DATA_RAPPORTINO = "data_rapportino";
    public static final String NOTE = "note";
    /** Numero per ditta e anno (schema 32, NUMERAZIONE_DOCUMENTI.md): lo da' il server alla conferma, qui solo si legge. */
    public static final String NUMERO = "numero";
    public static final String ANNO = "anno";
    /**
     * Firma del cliente (schema 29, GESTIONE_RAPPORTINI.md §13): PNG in base64 (FirmaActivity), nome di chi firma, quando.
     * Vuota = non firmato; il server non cancella mai una firma per una vuota in arrivo.
     */
    public static final String FIRMA = "firma";
    public static final String FIRMA_NOME = "firma_nome";
    public static final String FIRMA_DATA = "firma_data";


    public static final String PATH_EXPORT_RAPPORTINI = "rapportini";



	public Rapportini() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID);

		aggiungiCampo(ID, INTEGER);
        aggiungiCampo(ID_DITTA, INTEGER);
        aggiungiCampo(ID_CLIENTE, INTEGER);
        aggiungiCampo(ID_CANTIERE, INTEGER);
        aggiungiCampo(ID_ORDINE, INTEGER);
        aggiungiCampo(ID_UTENTE_DITTA, INTEGER);
        aggiungiCampo(ID_STATO, INTEGER);
        aggiungiCampo(DATA_RAPPORTINO, DATE);
        aggiungiCampo(NOTE, TEXT);
        aggiungiCampo(NUMERO, INTEGER);
        aggiungiCampo(ANNO, INTEGER);
        aggiungiCampo(FIRMA, TEXT);
        aggiungiCampo(FIRMA_NOME, TEXT);
        aggiungiCampo(FIRMA_DATA, DATE);

		aggiungiCampo(ATTIVO, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}


    /** "12/2026", oppure "" se il rapportino non ha ancora il numero (bozza, o confermato sul tablet e non ancora inviato). */
    public static String numeroDocumento(ContentValues r) {
        Integer n = r != null ? r.getAsInteger(NUMERO) : null;
        Integer a = r != null ? r.getAsInteger(ANNO) : null;
        return n != null && n > 0 ? n + "/" + (a != null ? a : "") : "";
    }

    /** Ultimo rapportino dell'autore (id_utente_ditta), per proporre gli stessi riferimenti in un rapportino nuovo. */
    public ContentValues getUltimoRapportinoOperatore(DbInterno dbcl, int idUtenteDitta) {
        String SQL = "Select * from " + NOME_TABELLA+" where "+ID_UTENTE_DITTA+"="+idUtenteDitta+" and "+ID_DITTA+"="+ Sessione.getDittaSelezionata() +" order by "+DATA_RAPPORTINO+" desc";
        ArrayList<Object> recs = dbcl.eseguiSelect(SQL,null);
        if (recs.size()>0){
            return (ContentValues)recs.get(0);
        }
        return null;

    }


    /** Si cancella solo in uno stato cancellabile (GESTIONE_RAPPORTINI.md §10; il server fa lo stesso controllo). */
    @Override
    public boolean cancellazionePossibile(DbInterno db, ContentValues val, android.content.Context ctx) {
        Integer idStato = val.getAsInteger(ID_STATO);
        ContentValues stato = com.ncfsistemi.econ.utils.RegoleRapportino.stato(db, idStato != null ? idStato : 0);
        if (!com.ncfsistemi.econ.utils.RegoleRapportino.isCancellabile(stato)) {
            com.ncfsistemi.econ.utils.Utility.mostraDialog(ctx.getString(com.ncfsistemi.econ.R.string.attenzione), ctx.getString(
                    com.ncfsistemi.econ.R.string.rapportino_non_cancellabile, stato.getAsString(StatiDocumento.NOME)), ctx, "OK");
            return false;
        }
        return super.cancellazionePossibile(db, val, ctx);
    }

    @Override
    protected void eliminaCorrelati(DbInterno db, ContentValues val) {
        int rapportino = val.getAsInteger(ID);
        RapportiniDettaglio tabRappDett = new RapportiniDettaglio();
        ContentValues where = new ContentValues();
        where.put(RapportiniDettaglio.ID_RAPPORTINO, rapportino);
        ArrayList<Object> dett = db.eseguiSelect(tabRappDett, where, null);
        for (int i = 0; i < dett.size(); i++) {
            ContentValues valDett = (ContentValues) dett.get(i);
            tabRappDett.cancellaRecord(db, valDett);
        }
        super.eliminaCorrelati(db, val);

    }


}
