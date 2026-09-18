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
    public static final String CODICE_ESTERNO = "codice_esterno";

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
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_ANAGRAFICA);
	}

	@Override
	public boolean cancellazionePossibile(DbInterno db, ContentValues val, Context ctx) {
		// TODO Auto-generated method stub
		ArrayList<Object> cantieri = db.eseguiSelect("Select count(*) as numero from " + Cantieri.NOME_TABELLA + " where "
				+ Cantieri.ID_ANAGRAFICA + "=" + val.getAsInteger(Anagrafica.ID_ANAGRAFICA), null);
		if (cantieri.size() > 0) {
			ContentValues cant = (ContentValues) cantieri.get(0);
			if (cant.getAsInteger("numero") > 0) {
				String messaggio = ctx.getString(R.string.cancellazione_non_possibile_cliente, cant.getAsString("numero"));
				Utility.mostraDialog(ctx.getString(R.string.attenzione), messaggio, ctx, "OK");
				return false;
			}
		}
		return super.cancellazionePossibile(db, val, ctx);
	}


	@Override
	protected int getMassimoNumeroRecordLicenzaGratis() {
		// TODO Auto-generated method stub
		return 1;
	}

}
