package com.ncfsistemi.econ;

import android.content.ContentValues;
import android.os.Bundle;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Placche;
import com.ncfsistemi.econ.views.EconSpinner;

public class PlaccheDettaglioModActivity extends EconDettaglioActivity {
	EconSpinner spinnerlinea = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("Econ: PlaccheDettaglioModActivity onCreate ENTER");
		setContentView(R.layout.activity_placche_dettaglio_mod);
		spinnerlinea = (EconSpinner) findViewById(R.id.econSpinner_linea);
		super.onCreate(savedInstanceState);

		if (getModalita() == MODIFICA) {
			spinnerlinea.setEnabled(false);
		} else {
			String idlineastring = getIntent().getStringExtra(Linee.ID_LINEA);
			int idlinea = 0;
			try {
				idlinea = Integer.parseInt(idlineastring);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block

			}
			if (idlinea != 0) {
				spinnerlinea.setValue("" + idlinea);
			}
			spinnerlinea.setTabella(new Linee());
		}
		System.out.println("Econ: PlaccheDettaglioModActivity onCreate EXIT");
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("Econ: PlaccheDettaglioModActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Placche.ID_PLACCA, getIDModifica());
		ContentValues val = db.getRecord(new Placche(), where);
		db.close();
		if (val != null) {

			spinnerlinea.setValue("" + val.getAsInteger(Placche.ID_LINEA));
			setText(R.id.editText_placca, val.getAsString(Placche.NOME_PLACCA));

		}

		spinnerlinea.setTabella(new Linee());
		System.out.println("Econ: PlaccheDettaglioModActivity inizializzaModifica EXIT");
		super.inizializzaModifica();
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("Econ: PlaccheDettaglioModActivity eseguiInserimento ENTER");
		if (spinnerlinea.getValue().equals("")) {
			spinnerlinea.setError(getString(R.string.errore_selezione_linea));
			return getString(R.string.errore_selezione_linea);
		}

		Placche placche = new Placche();
		ContentValues val = placche.getValoriLogInserimento(db);
		val.put(Placche.ID_LINEA, spinnerlinea.getValue());
		val.put(Placche.NOME_PLACCA, getTesto(R.id.editText_placca));
		placche.inserisciRecord(db, val);
		System.out.println("Econ: PlaccheDettaglioModActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("Econ: PlaccheDettaglioModActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		Placche placche = new Placche();
		ContentValues val = placche.getValoriLogModifica(db);

		val.put(Placche.NOME_PLACCA, getTesto(R.id.editText_placca));

		ContentValues where = new ContentValues();
		where.put(Placche.ID_PLACCA, getIDModifica());

		placche.aggiornaRecord(db, val, where);
		System.out.println("Econ: PlaccheDettaglioModActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

}
