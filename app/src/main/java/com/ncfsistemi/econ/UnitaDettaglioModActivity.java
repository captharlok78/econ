package com.ncfsistemi.econ;

import android.content.ContentValues;
import android.os.Bundle;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Placche;
import com.ncfsistemi.econ.db.table.Unita;
import com.ncfsistemi.econ.views.EconSpinner;

public class UnitaDettaglioModActivity extends EconDettaglioActivity {

	private EconSpinner spinner_linea = null;
	private EconSpinner spinner_placca = null;

	private String idLineaPrec = "0";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("Econ: UnitaDettaglioModActivity onCreate ENTER");
		setContentView(R.layout.activity_unita_dettaglio_mod);
		super.onCreate(savedInstanceState);

		if (getModalita() == INSERIMENTO) {
			// prendo la linea e la placca dal cantiere
			DbInterno db = new DbInterno(this);
			ContentValues where = new ContentValues();
			where.put(Cantieri.ID_CANTIERE, getIntent().getIntExtra(Cantieri.ID_CANTIERE, 0));
			ContentValues valCantiere = db.getRecord(new Cantieri(), where);
			db.close();
			if (valCantiere != null) {
				setText(R.id.econSpinner_linea, valCantiere.getAsString(Cantieri.ID_LINEA));
				setText(R.id.econSpinner_placca, valCantiere.getAsString(Cantieri.ID_PLACCA));
			}
		}
		spinner_linea = (EconSpinner) findViewById(R.id.econSpinner_linea);
		spinner_placca = (EconSpinner) findViewById(R.id.econSpinner_placca);
		spinner_linea.setTabella(new Linee());
		spinner_placca.setTabella(new Placche());
		spinner_placca.setMessaggioDisabilitato(getString(R.string.errore_selezione_linea));
		spinner_linea.setSpinnerCollegato(spinner_placca);
		System.out.println("Econ: UnitaDettaglioModActivity onCreate EXIT");
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("Econ: UnitaDettaglioModActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		super.inizializzaModifica();
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Unita.ID_UNITA, getIDModifica());
		ContentValues val = db.getRecord(new Unita(), where);
		db.close();
		if (val != null) {
			setText(R.id.editText_unita, val.getAsString(Unita.NOME));
			setText(R.id.econSpinner_linea, val.getAsString(Unita.ID_LINEA));
			idLineaPrec = val.getAsString(Unita.ID_LINEA);
			setText(R.id.econSpinner_placca, val.getAsString(Unita.ID_PLACCA));
			setText(R.id.editText_note, val.getAsString(Unita.NOTE));
		}
		System.out.println("Econ: UnitaDettaglioModActivity inizializzaModifica EXIT");
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("Econ: UnitaDettaglioModActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		Unita unita = new Unita();
		ContentValues val = unita.getValoriLogInserimento(db);
		val.put(Unita.ID_CANTIERE, getIntent().getIntExtra(Cantieri.ID_CANTIERE, 0));
		val.put(Unita.NOME, getTesto(R.id.editText_unita));
		val.put(Unita.ID_LINEA, getTesto(R.id.econSpinner_linea));
		val.put(Unita.ID_PLACCA, getTesto(R.id.econSpinner_placca));
		val.put(Unita.NOTE, getTesto(R.id.editText_note));
		unita.inserisciRecord(db, val);
		System.out.println("Econ: UnitaDettaglioModActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("Econ: UnitaDettaglioModActivity eseguiAggiornamento ENTER");
		Unita unita = new Unita();
		ContentValues val = unita.getValoriLogModifica(db);
		val.put(Unita.NOME, getTesto(R.id.editText_unita));
		val.put(Unita.ID_LINEA, getTesto(R.id.econSpinner_linea));
		val.put(Unita.ID_PLACCA, getTesto(R.id.econSpinner_placca));
		val.put(Unita.NOTE, getTesto(R.id.editText_note));

		ContentValues where = new ContentValues();
		where.put(Unita.ID_UNITA, getIDModifica());
		unita.aggiornaRecord(db, val, where);
		System.out.println("Econ: UnitaDettaglioModActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

	@Override
	public String getMessaggioConfermaSalvataggio() {
		System.out.println("Econ: UnitaDettaglioModActivity getMessaggioConfermaSalvataggio ENTER");
		// TODO Auto-generated method stub
		String idLineaNuova = getTesto(R.id.econSpinner_linea);
		if (idLineaNuova.equals("")) {
			idLineaNuova = "0";
		}
		if (getModalita() == MODIFICA && !idLineaNuova.equals(idLineaPrec)) {
			return getString(R.string.messaggio_variazione_linea);
		}
		System.out.println("Econ: UnitaDettaglioModActivity getMessaggioConfermaSalvataggio EXIT");
		return super.getMessaggioConfermaSalvataggio();
	}


	/** Gestione elettrica del cantiere (GESTIONE_ELETTRICA.md): senza ELETTRICO.MODIFICA la maschera e' in sola lettura. */
	@Override
	protected String moduloFunzionalita() {
		return com.ncfsistemi.econ.utils.FunzionalitaApp.ELETTRICO;
	}
}
