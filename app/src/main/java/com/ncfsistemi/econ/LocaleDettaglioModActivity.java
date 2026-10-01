package com.ncfsistemi.econ;

import android.content.ContentValues;
import android.os.Bundle;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Aree;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Locali;
import com.ncfsistemi.econ.db.table.Placche;
import com.ncfsistemi.econ.views.EconSpinner;

public class LocaleDettaglioModActivity extends EconDettaglioActivity {

	private EconSpinner spinner_linea = null;
	private EconSpinner spinner_placca = null;

	private String idLineaPrec = "0";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("Econ: LocaleDettaglioModActivity onCreate ENTER");
		setContentView(R.layout.activity_locale_dettaglio_mod);
		super.onCreate(savedInstanceState);
		if (getModalita() == INSERIMENTO) {
			// prendo la linea e la placca dal cantiere
			DbInterno db = new DbInterno(this);
			ContentValues where = new ContentValues();
			where.put(Aree.ID_AREA, getIntent().getIntExtra(Aree.ID_AREA, 0));
			ContentValues valArea = db.getRecord(new Aree(), where);
			db.close();
			if (valArea != null) {
				setText(R.id.econSpinner_linea, valArea.getAsString(Aree.ID_LINEA));
				setText(R.id.econSpinner_placca, valArea.getAsString(Aree.ID_PLACCA));
			}
		}
		spinner_linea = (EconSpinner) findViewById(R.id.econSpinner_linea);
		spinner_placca = (EconSpinner) findViewById(R.id.econSpinner_placca);
		spinner_linea.setTabella(new Linee());
		spinner_placca.setTabella(new Placche());
		spinner_placca.setMessaggioDisabilitato(getString(R.string.errore_selezione_linea));
		spinner_linea.setSpinnerCollegato(spinner_placca);
		System.out.println("Econ: LocaleDettaglioModActivity onCreate EXIT");
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("Econ: LocaleDettaglioModActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		super.inizializzaModifica();
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Locali.ID_LOCALE, getIDModifica());
		ContentValues val = db.getRecord(new Locali(), where);
		db.close();
		if (val != null) {
			setText(R.id.editText_locale, val.getAsString(Locali.NOME));
			setText(R.id.econSpinner_linea, val.getAsString(Locali.ID_LINEA));
			idLineaPrec = val.getAsString(Locali.ID_LINEA);
			setText(R.id.econSpinner_placca, val.getAsString(Locali.ID_PLACCA));
			setText(R.id.editText_note, val.getAsString(Locali.NOTE));
		}
		System.out.println("Econ: LocaleDettaglioModActivity inizializzaModifica EXIT");
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("Econ: LocaleDettaglioModActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		Locali locali = new Locali();
		ContentValues val = locali.getValoriLogInserimento(db);
		val.put(Locali.ID_AREA, getIntent().getIntExtra(Aree.ID_AREA, 0));
		val.put(Locali.NOME, getTesto(R.id.editText_locale));
		val.put(Locali.ID_LINEA, getTesto(R.id.econSpinner_linea));
		val.put(Locali.ID_PLACCA, getTesto(R.id.econSpinner_placca));
		val.put(Locali.NOTE, getTesto(R.id.editText_note));

		locali.inserisciRecord(db, val);
		System.out.println("Econ: LocaleDettaglioModActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("Econ: LocaleDettaglioModActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		Locali locali = new Locali();
		ContentValues val = locali.getValoriLogModifica(db);

		val.put(Locali.NOME, getTesto(R.id.editText_locale));
		val.put(Locali.ID_LINEA, getTesto(R.id.econSpinner_linea));
		val.put(Locali.ID_PLACCA, getTesto(R.id.econSpinner_placca));
		val.put(Locali.NOTE, getTesto(R.id.editText_note));

		ContentValues where = new ContentValues();
		where.put(Locali.ID_LOCALE, getIDModifica());
		locali.aggiornaRecord(db, val, where);
		System.out.println("Econ: LocaleDettaglioModActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

	@Override
	public String getMessaggioConfermaSalvataggio() {
		System.out.println("Econ: LocaleDettaglioModActivity getMessaggioConfermaSalvataggio ENTER");
		// TODO Auto-generated method stub
		String idLineaNuova = getTesto(R.id.econSpinner_linea);
		if (idLineaNuova.equals("")) {
			idLineaNuova = "0";
		}
		if (getModalita() == MODIFICA && !idLineaNuova.equals(idLineaPrec)) {
			return getString(R.string.messaggio_variazione_linea);
		}
		System.out.println("Econ: LocaleDettaglioModActivity getMessaggioConfermaSalvataggio EXIT");
		return super.getMessaggioConfermaSalvataggio();
	}


	/** Gestione elettrica del cantiere (GESTIONE_ELETTRICA.md): senza ELETTRICO.MODIFICA la maschera e' in sola lettura. */
	@Override
	protected String moduloFunzionalita() {
		return com.ncfsistemi.econ.utils.FunzionalitaApp.ELETTRICO;
	}
}
