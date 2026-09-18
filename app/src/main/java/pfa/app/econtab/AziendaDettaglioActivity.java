package pfa.app.econtab;

import android.content.ContentValues;
import android.os.Bundle;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Ditte;

public class AziendaDettaglioActivity extends EConTabDettaglioActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: AziendaDettaglioActivity onCreate");
		setContentView(R.layout.activity_azienda_dettaglio);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("EConTab: AziendaDettaglioActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		super.inizializzaModifica();
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Ditte.ID_DITTA, getIntent().getIntExtra("ID", 0));
		ContentValues val = db.getRecord(new Ditte(), where);
		if (val != null) {
			setText(R.id.editText_ragionesociale, val.getAsString(Ditte.RAGIONE_SOCIALE), null);
			setText(R.id.editText_indirizzo, val.getAsString(Ditte.INDIRIZZO), null);
			setText(R.id.editText_cap, val.getAsString(Ditte.CAP), null);
			setText(R.id.editText_citta, val.getAsString(Ditte.CITTA), null);
			setText(R.id.editText_provincia, val.getAsString(Ditte.PROVINCIA), null);
			setText(R.id.editText_cf, val.getAsString(Ditte.CODICE_FISCALE), null);
			setText(R.id.editText_partitaiva, val.getAsString(Ditte.PARTITA_IVA), null);
			double metriCodaScatole = val.getAsDouble(Ditte.METRI_CODA_SCATOLE);
			double metriCodaUtilizzatori = val.getAsDouble(Ditte.METRI_CODA_UTILIZZATORI);
			setText(R.id.editText_codascatole, "" + (int) (metriCodaScatole * 100));
			setText(R.id.editText_codautilizzatori, "" + (int) (metriCodaUtilizzatori * 100));
		}
		db.close();
		System.out.println("EConTab: AziendaDettaglioActivity inizializzaModifica EXIT");
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("EConTab: AziendaDettaglioActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		Ditte ditteTab = new Ditte();
		ContentValues val = ditteTab.getValoriLogInserimento(db);
		val.put(Ditte.RAGIONE_SOCIALE, getTesto(R.id.editText_ragionesociale));
		val.put(Ditte.INDIRIZZO, getTesto(R.id.editText_indirizzo));
		val.put(Ditte.CAP, getTesto(R.id.editText_cap));
		val.put(Ditte.CITTA, getTesto(R.id.editText_citta));
		val.put(Ditte.PROVINCIA, getTesto(R.id.editText_provincia));
		val.put(Ditte.CODICE_FISCALE, getTesto(R.id.editText_cf));
		val.put(Ditte.PARTITA_IVA, getTesto(R.id.editText_partitaiva));
		String cmCodaScatole = getTesto(R.id.editText_codascatole);
		String cmCodaUtilizzatori = getTesto(R.id.editText_codautilizzatori);
		if (cmCodaScatole.trim().equals("")) {
			cmCodaScatole = "0";
		}
		if (cmCodaUtilizzatori.trim().equals("")) {
			cmCodaUtilizzatori = "0";
		}

		double metriCodaScatole = Integer.parseInt(cmCodaScatole) / 100;
		double metriCodautilizzatori = Integer.parseInt(cmCodaUtilizzatori) / 100;
		val.put(Ditte.METRI_CODA_SCATOLE, metriCodaScatole);
		val.put(Ditte.METRI_CODA_UTILIZZATORI, metriCodautilizzatori);
		ditteTab.inserisciRecord(db, val);
		System.out.println("EConTab: AziendaDettaglioActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("EConTab: AziendaDettaglioActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		Ditte ditteTab = new Ditte();
		ContentValues val = ditteTab.getValoriLogModifica(db);
		val.put(Ditte.RAGIONE_SOCIALE, getTesto(R.id.editText_ragionesociale));
		val.put(Ditte.INDIRIZZO, getTesto(R.id.editText_indirizzo));
		val.put(Ditte.CAP, getTesto(R.id.editText_cap));
		val.put(Ditte.CITTA, getTesto(R.id.editText_citta));
		val.put(Ditte.PROVINCIA, getTesto(R.id.editText_provincia));
		val.put(Ditte.CODICE_FISCALE, getTesto(R.id.editText_cf));
		val.put(Ditte.PARTITA_IVA, getTesto(R.id.editText_partitaiva));
		String cmCodaScatole = getTesto(R.id.editText_codascatole);
		String cmCodaUtilizzatori = getTesto(R.id.editText_codautilizzatori);
		if (cmCodaScatole.trim().equals("")) {
			cmCodaScatole = "0";
		}
		if (cmCodaUtilizzatori.trim().equals("")) {
			cmCodaUtilizzatori = "0";
		}

		double metriCodaScatole = Double.parseDouble(cmCodaScatole) / 100;
		double metriCodautilizzatori = Double.parseDouble(cmCodaUtilizzatori) / 100;
		val.put(Ditte.METRI_CODA_SCATOLE, metriCodaScatole);
		val.put(Ditte.METRI_CODA_UTILIZZATORI, metriCodautilizzatori);
		int idMod = getIntent().getIntExtra("ID", 0);

		ContentValues where = new ContentValues();
		where.put(Ditte.ID_DITTA, idMod);
		ditteTab.aggiornaRecord(db, val, where);
		System.out.println("EConTab: AziendaDettaglioActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

}
