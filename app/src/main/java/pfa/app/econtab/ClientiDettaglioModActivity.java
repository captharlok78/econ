package pfa.app.econtab;

import android.content.ContentValues;
import android.os.Bundle;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Iva;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.views.EConTabSpinner;

public class ClientiDettaglioModActivity extends EConTabDettaglioActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: ClientiDettaglioModActivity onCreate ENTER");
		setContentView(R.layout.activity_clienti_dettaglio_mod);
		super.onCreate(savedInstanceState);

		if (getModalita() == INSERIMENTO) {
			((EConTabSpinner) findViewById(R.id.spinner_iva)).setTabella(new Iva());
		}
		//per le licenze server non psso modificare il codice che arriva dal server (modifica per PFA)
		if (Sessione.isLicenzaBusiness(this)){
			findViewById(R.id.editText_codice).setEnabled(false);
		}
		System.out.println("EConTab: ClientiDettaglioModActivity onCreate EXIT");
	}

	@Override
	protected String getTitoloDettaglio() {
		return getModalita() == INSERIMENTO ? "Nuovo cliente" : "Modifica cliente";
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("EConTab: ClientiDettaglioModActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		super.inizializzaModifica();
		int id = getIntent().getIntExtra("ID", 0);
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Anagrafica.ID_ANAGRAFICA, id);
		ContentValues val = db.getRecord(new Anagrafica(), where);
		db.close();
		if (val != null) {
			setText(R.id.editText_ragionesociale, val.getAsString(Anagrafica.RAGIONE_SOCIALE));
			setText(R.id.editText_codice, val.getAsString(Anagrafica.CODICE_ESTERNO));
			setText(R.id.editText_indirizzo, val.getAsString(Anagrafica.INDIRIZZO));
			setText(R.id.editText_cap, val.getAsString(Anagrafica.CAP));
			setText(R.id.editText_citta, val.getAsString(Anagrafica.CITTA));
			setText(R.id.editText_provincia, val.getAsString(Anagrafica.PROVINCIA));
			setText(R.id.editText_cf, val.getAsString(Anagrafica.CODICE_FISCALE));
			setText(R.id.editText_partitaiva, val.getAsString(Anagrafica.PARTITA_IVA));
			setText(R.id.editText_cellulare1, val.getAsString(Anagrafica.CELLULARE));
			setText(R.id.editText_cellulare2, val.getAsString(Anagrafica.CELLULARE1));
			setText(R.id.editText_telefono, val.getAsString(Anagrafica.TELEFONO));
			setText(R.id.spinner_iva, val.getAsString(Anagrafica.CODICE_IVA));
			setText(R.id.editText_mail, val.getAsString(Anagrafica.MAIL));
			setText(R.id.editText_note, val.getAsString(Anagrafica.NOTE));
		}

		((EConTabSpinner) findViewById(R.id.spinner_iva)).setTabella(new Iva());
		System.out.println("EConTab: ClientiDettaglioModActivity inizializzaModifica EXIT");
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("EConTab: ClientiDettaglioModActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		if (getTesto(R.id.spinner_iva).equals("")) {
			((EConTabSpinner) findViewById(R.id.spinner_iva)).setError(getString(R.string.errore_selezione_iva));
			return getString(R.string.errore_selezione_iva);
		}
		Anagrafica tabella = new Anagrafica();
		ContentValues val = tabella.getValoriLogInserimento(db);
		val.put(Anagrafica.RAGIONE_SOCIALE, getTesto(R.id.editText_ragionesociale));
		val.put(Anagrafica.CODICE_ESTERNO, getTesto(R.id.editText_codice));
		val.put(Anagrafica.INDIRIZZO, getTesto(R.id.editText_indirizzo));
		val.put(Anagrafica.CAP, getTesto(R.id.editText_cap));
		val.put(Anagrafica.CITTA, getTesto(R.id.editText_citta));
		val.put(Anagrafica.PROVINCIA, getTesto(R.id.editText_provincia));
		val.put(Anagrafica.CODICE_FISCALE, getTesto(R.id.editText_cf));
		val.put(Anagrafica.PARTITA_IVA, getTesto(R.id.editText_partitaiva));
		val.put(Anagrafica.CELLULARE, getTesto(R.id.editText_cellulare1));
		val.put(Anagrafica.CELLULARE1, getTesto(R.id.editText_cellulare2));
		val.put(Anagrafica.TELEFONO, getTesto(R.id.editText_telefono));
		val.put(Anagrafica.MAIL, getTesto(R.id.editText_mail));
		val.put(Anagrafica.CODICE_IVA, getTesto(R.id.spinner_iva));
		val.put(Anagrafica.NOTE, getTesto(R.id.editText_note));

		tabella.inserisciRecord(db, val);
		System.out.println("EConTab: ClientiDettaglioModActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("EConTab: ClientiDettaglioModActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		if (getTesto(R.id.spinner_iva).equals("")) {
			((EConTabSpinner) findViewById(R.id.spinner_iva)).setError(getString(R.string.errore_selezione_iva));
			return getString(R.string.errore_selezione_iva);
		}

		Anagrafica tabella = new Anagrafica();
		ContentValues val = tabella.getValoriLogModifica(db);
		val.put(Anagrafica.RAGIONE_SOCIALE, getTesto(R.id.editText_ragionesociale));
		val.put(Anagrafica.CODICE_ESTERNO, getTesto(R.id.editText_codice));
		val.put(Anagrafica.INDIRIZZO, getTesto(R.id.editText_indirizzo));
		val.put(Anagrafica.CAP, getTesto(R.id.editText_cap));
		val.put(Anagrafica.CITTA, getTesto(R.id.editText_citta));
		val.put(Anagrafica.PROVINCIA, getTesto(R.id.editText_provincia));
		val.put(Anagrafica.CODICE_FISCALE, getTesto(R.id.editText_cf));
		val.put(Anagrafica.PARTITA_IVA, getTesto(R.id.editText_partitaiva));
		val.put(Anagrafica.CELLULARE, getTesto(R.id.editText_cellulare1));
		val.put(Anagrafica.CELLULARE1, getTesto(R.id.editText_cellulare2));
		val.put(Anagrafica.TELEFONO, getTesto(R.id.editText_telefono));
		val.put(Anagrafica.MAIL, getTesto(R.id.editText_mail));
		val.put(Anagrafica.CODICE_IVA, getTesto(R.id.spinner_iva));
		val.put(Anagrafica.NOTE, getTesto(R.id.editText_note));

		ContentValues where = new ContentValues();
		where.put(Anagrafica.ID_ANAGRAFICA, getIDModifica());

		tabella.aggiornaRecord(db, val, where);
		System.out.println("EConTab: ClientiDettaglioModActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

}
