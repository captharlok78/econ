package com.ncfsistemi.econ;

import android.content.ContentValues;
import android.os.Bundle;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Aree;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Locali;
import com.ncfsistemi.econ.db.table.Placche;
import com.ncfsistemi.econ.db.table.Unita;
import com.ncfsistemi.econ.views.EconSpinner;

public class AreaDettaglioModActivity extends EconDettaglioActivity {

	private EconSpinner spinner_linea = null;
	private EconSpinner spinner_placca = null;

	private String idLineaPrec = "0";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("Econ: AreaDettaglioModActivity onCreate ENTER");
		setContentView(R.layout.activity_area_dettaglio_mod);
		super.onCreate(savedInstanceState);
		if (getModalita() == INSERIMENTO) {
			// prendo la linea e la placca dal cantiere
			DbInterno db = new DbInterno(this);
			ContentValues where = new ContentValues();
			where.put(Unita.ID_UNITA, getIntent().getIntExtra(Unita.ID_UNITA, 0));
			ContentValues valUnita = db.getRecord(new Unita(), where);
			db.close();
			if (valUnita != null) {
				setText(R.id.econSpinner_linea, valUnita.getAsString(Unita.ID_LINEA));
				setText(R.id.econSpinner_placca, valUnita.getAsString(Unita.ID_PLACCA));
			}
		}

		spinner_linea = (EconSpinner) findViewById(R.id.econSpinner_linea);
		spinner_placca = (EconSpinner) findViewById(R.id.econSpinner_placca);
		spinner_linea.setTabella(new Linee());
		spinner_placca.setTabella(new Placche());
		spinner_placca.setMessaggioDisabilitato(getString(R.string.errore_selezione_linea));
		spinner_linea.setSpinnerCollegato(spinner_placca);
		System.out.println("Econ: AreaDettaglioModActivity onCreate EXIT");
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("Econ: AreaDettaglioModActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		super.inizializzaModifica();
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Aree.ID_AREA, getIDModifica());
		ContentValues val = new Aree().getRecordPerChiave(db, getIDModifica());
		db.close();
		if (val != null) {
			setText(R.id.editText_area, val.getAsString(Aree.NOME));
			setText(R.id.econSpinner_linea, val.getAsString(Aree.ID_LINEA));
			idLineaPrec = val.getAsString(Locali.ID_LINEA);
			setText(R.id.econSpinner_placca, val.getAsString(Aree.ID_PLACCA));
			setText(R.id.editText_note, val.getAsString(Aree.NOTE));
		}
		System.out.println("Econ: AreaDettaglioModActivity inizializzaModifica EXIT");
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("Econ: AreaDettaglioModActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		Aree aree = new Aree();
		ContentValues val = aree.getValoriLogInserimento(db);
		val.put(Aree.ID_UNITA, getIntent().getIntExtra(Unita.ID_UNITA, 0));
		val.put(Aree.NOME, getTesto(R.id.editText_area));
		val.put(Aree.ID_LINEA, getTesto(R.id.econSpinner_linea));
		val.put(Aree.ID_PLACCA, getTesto(R.id.econSpinner_placca));
		val.put(Aree.NOTE, getTesto(R.id.editText_note));
		aree.inserisciRecord(db, val);
		System.out.println("Econ: AreaDettaglioModActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("Econ: AreaDettaglioModActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		Aree aree = new Aree();
		ContentValues val = aree.getValoriLogModifica(db);
		val.put(Aree.NOME, getTesto(R.id.editText_area));
		val.put(Aree.ID_LINEA, getTesto(R.id.econSpinner_linea));
		val.put(Aree.ID_PLACCA, getTesto(R.id.econSpinner_placca));
		val.put(Aree.NOTE, getTesto(R.id.editText_note));

		ContentValues where = new ContentValues();
		where.put(Aree.ID_AREA, getIDModifica());
		aree.aggiornaRecord(db, val, where);
		System.out.println("Econ: AreaDettaglioModActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

	@Override
	public String getMessaggioConfermaSalvataggio() {
		System.out.println("Econ: AreaDettaglioModActivity getMessaggioConfermaSalvataggio ENTER");
		// TODO Auto-generated method stub
		String idLineaNuova = getTesto(R.id.econSpinner_linea);
		if (idLineaNuova.equals("")) {
			idLineaNuova = "0";
		}
		if (getModalita() == MODIFICA && !idLineaNuova.equals(idLineaPrec)) {
			return getString(R.string.messaggio_variazione_linea);
		}
		System.out.println("Econ: AreaDettaglioModActivity getMessaggioConfermaSalvataggio EXIT");
		return super.getMessaggioConfermaSalvataggio();
	}


	/** Gestione elettrica del cantiere (GESTIONE_ELETTRICA.md): senza ELETTRICO.MODIFICA la maschera e' in sola lettura. */
	@Override
	protected String moduloFunzionalita() {
		return com.ncfsistemi.econ.utils.FunzionalitaApp.ELETTRICO;
	}
}
