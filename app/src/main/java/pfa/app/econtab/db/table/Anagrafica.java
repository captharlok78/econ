package pfa.app.econtab.db.table;

import android.content.ContentValues;
import android.content.Context;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.utils.Utility;

public class Anagrafica extends AbstractTable {
	public static final String NOME_TABELLA = "anagrafica";

	public static final String ID_ANAGRAFICA = "id_anagrafica";
	public static final String RAGIONE_SOCIALE = "ragione_sociale";
	public static final String INDIRIZZO = "indirizzo";
	public static final String CITTA = "citta";
	public static final String PROVINCIA = "provincia";
	public static final String CAP = "cap";
	public static final String CODICE_FISCALE = "codice_fiscale";
	public static final String PARTITA_IVA = "partita_iva";
	public static final String CELLULARE = "cellulare";
	public static final String CELLULARE1 = "cellulare1";
	public static final String TELEFONO = "telefono";
	public static final String MAIL = "mail";
	public static final String TIPO_CF = "tipo_cf";
	public static final String NOTE = "note";
	public static final String CODICE_IVA = "codice_iva";
    /** Codice del gestionale della ditta (allineamenti). */
    public static final String CODICE_ESTERNO = "codice_esterno";
    /** Codice per riconoscere il cliente (colonna "Cod."), schema 27. */
    public static final String CODICE = "codice";
    /** Fatturazione (schema 28). */
    public static final String PEC = "pec";
    public static final String CODICE_SDI = "codice_sdi";
    public static final String TIPO_AZIENDA = "G";
    public static final String TIPO_PERSONA_FISICA = "F";

	public Anagrafica() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_ANAGRAFICA);

		aggiungiCampo(ID_ANAGRAFICA, INTEGER);
		aggiungiCampo(RAGIONE_SOCIALE, TEXT);
		aggiungiCampo(INDIRIZZO, TEXT);
		aggiungiCampo(CITTA, TEXT);
		aggiungiCampo(PROVINCIA, TEXT);
		aggiungiCampo(CAP, TEXT);
		aggiungiCampo(CODICE_FISCALE, TEXT);
		aggiungiCampo(PARTITA_IVA, TEXT);
		aggiungiCampo(CELLULARE, TEXT);
		aggiungiCampo(CELLULARE1, TEXT);
		aggiungiCampo(TELEFONO, TEXT);
		aggiungiCampo(MAIL, TEXT);
		aggiungiCampo(TIPO_CF, TEXT);
		aggiungiCampo(NOTE, TEXT);
		aggiungiCampo(CODICE_IVA, TEXT);
        aggiungiCampo(CODICE_ESTERNO, TEXT);
		aggiungiCampo(CODICE, TEXT);
		aggiungiCampo(PEC, TEXT);
		aggiungiCampo(CODICE_SDI, TEXT);
		aggiungiCampo(ATTIVO, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_ANAGRAFICA);
	}

	/**
	 * Un cliente usato da cantieri o rapportini non si elimina: si disattiva dal form del cliente (GESTIONE_CLIENTI.md C1;
	 * il server rifiuterebbe comunque l'eliminazione).
	 */
	@Override
	public boolean cancellazionePossibile(DbInterno db, ContentValues val, Context ctx) {
		int id = val.getAsInteger(Anagrafica.ID_ANAGRAFICA);
		int cantieri = conta(db, "SELECT count(*) AS numero FROM " + Cantieri.NOME_TABELLA + " WHERE " + Cantieri.ID_ANAGRAFICA + " = " + id);
		int rapportini = conta(db, "SELECT count(*) AS numero FROM " + Rapportini.NOME_TABELLA + " WHERE " + Rapportini.ID_CLIENTE + " = " + id);
		if (cantieri > 0 || rapportini > 0) {
			String usato = (cantieri > 0 ? cantieri + (cantieri == 1 ? " cantiere" : " cantieri") : "")
					+ (cantieri > 0 && rapportini > 0 ? " e " : "")
					+ (rapportini > 0 ? rapportini + (rapportini == 1 ? " rapportino" : " rapportini") : "");
			Utility.mostraDialog(ctx.getString(R.string.attenzione), "Il cliente è usato da " + usato
					+ ": non si può eliminare. Per toglierlo dagli elenchi disattivalo (Modifica cliente, casella Attivo).", ctx, "OK");
			return false;
		}
		return super.cancellazionePossibile(db, val, ctx);
	}

	private static int conta(DbInterno db, String sql) {
		ArrayList<Object> r = db.eseguiSelect(sql, null);
		if (r.isEmpty()) return 0;
		Integer n = ((ContentValues) r.get(0)).getAsInteger("numero");
		return n == null ? 0 : n;
	}


}
