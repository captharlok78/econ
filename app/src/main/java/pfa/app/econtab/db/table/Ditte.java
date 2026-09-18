package pfa.app.econtab.db.table;

import android.content.ContentValues;
import android.content.Context;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.utils.Utility;

public class Ditte extends AbstractTable {
	public static final String NOME_TABELLA = "ditte";

	public static final String ID_DITTA = "id_ditta";
	public static final String RAGIONE_SOCIALE = "ragione_sociale";
	public static final String INDIRIZZO = "indirizzo";
	public static final String CITTA = "citta";
	public static final String PROVINCIA = "provincia";
	public static final String CAP = "cap";
	public static final String PARTITA_IVA = "partita_iva";
	public static final String CODICE_FISCALE = "codice_fiscale";
	public static final String METRI_CODA_SCATOLE = "metri_coda_scatole";
	public static final String METRI_CODA_UTILIZZATORI = "metri_coda_utilizzatori";

	public Ditte() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_DITTA);

		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(RAGIONE_SOCIALE, TEXT);
		aggiungiCampo(INDIRIZZO, TEXT);
		aggiungiCampo(CITTA, TEXT);
		aggiungiCampo(PROVINCIA, TEXT);
		aggiungiCampo(CAP, TEXT);
		aggiungiCampo(CODICE_FISCALE, TEXT);
		aggiungiCampo(PARTITA_IVA, TEXT);
		aggiungiCampo(METRI_CODA_SCATOLE, NUMERIC);
		aggiungiCampo(METRI_CODA_UTILIZZATORI, NUMERIC);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_DITTA);
	}

	@Override
	public ContentValues getValoriInsertDefault() {
		// TODO Auto-generated method stub
		ContentValues val = super.getValoriInsertDefault();
		val.put(ID_DITTA, VALORE_PRIMO_ID);
		val.put(RAGIONE_SOCIALE, "Azienda Demo");
		val.put(METRI_CODA_SCATOLE, 0.20);
		val.put(METRI_CODA_UTILIZZATORI, 0.50);

		return val;
	}

	/**
	 * Un'azienda � cancellabile se non ci sono cantieri
	 */
	@Override
	public boolean cancellazionePossibile(DbInterno db, ContentValues val, Context ctx) {

		// TODO Auto-generated method stub
		ArrayList<Object> cantieri = db.eseguiSelect("Select count(*) as numero from " + Cantieri.NOME_TABELLA + " where "
				+ Cantieri.ID_DITTA + "=" + val.getAsInteger(Ditte.ID_DITTA), null);
		if (cantieri.size() > 0) {
			ContentValues cant = (ContentValues) cantieri.get(0);
			if (cant.getAsInteger("numero") > 0) {
				String messaggio = ctx.getString(R.string.cancellazione_non_possibile_azienda, cant.getAsString("numero"));
				Utility.mostraDialog(ctx.getString(R.string.attenzione), messaggio, ctx, "OK");
				return false;
			}
		}
		return true;
	}

	@Override
	protected int getMassimoNumeroRecordLicenzaGratis() {
		// TODO Auto-generated method stub
		return 1;
	}

}
