package pfa.app.econtab;

import android.content.ContentValues;
import android.os.Bundle;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.views.EConTabSpinner;

public class LineeDettaglioModActivity extends EConTabDettaglioActivity {
	EConTabSpinner spinnerfornitore = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: LineeDettaglioModActivity onCreate ENTER");
		setContentView(R.layout.activity_linee_dettaglio_mod);
		spinnerfornitore = (EConTabSpinner) findViewById(R.id.econtabSpinner_fornitore);
		super.onCreate(savedInstanceState);

		if (getModalita() == MODIFICA) {
			spinnerfornitore.setEnabled(false);
		} else {
			int idcostruttore = getIntent().getIntExtra(Costruttori.ID_COSTRUTTORE, 0);
			if (idcostruttore != 0) {
				spinnerfornitore.setValue("" + idcostruttore);
			}
			spinnerfornitore.setTabella(new Costruttori());
		}
		System.out.println("EConTab: LineeDettaglioModActivity onCreate EXIT");
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("EConTab: LineeDettaglioModActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Linee.ID_LINEA, getIDModifica());
		ContentValues val = db.getRecord(new Linee(), where);
		db.close();
		if (val != null) {
			setText(R.id.econtabSpinner_fornitore, val.getAsString(Linee.ID_COSTRUTTORE));
			setText(R.id.editText_linea, val.getAsString(Linee.NOME_LINEA));
		}
		spinnerfornitore.setTabella(new Costruttori());
		System.out.println("EConTab: LineeDettaglioModActivity inizializzaModifica EXIT");
		super.inizializzaModifica();
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("EConTab: LineeDettaglioModActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		if (spinnerfornitore.getValue().equals("")) {
			spinnerfornitore.setError(getString(R.string.errore_selezione_fornitore));
			return getString(R.string.errore_selezione_fornitore);
		}
		Linee linee = new Linee();

		ContentValues val = linee.getValoriLogInserimento(db);
		val.put(Linee.ID_COSTRUTTORE, spinnerfornitore.getValue());
		val.put(Linee.NOME_LINEA, getTesto(R.id.editText_linea));
		linee.inserisciRecord(db, val);
		System.out.println("EConTab: LineeDettaglioModActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("EConTab: LineeDettaglioModActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		Linee linee = new Linee();

		ContentValues val = linee.getValoriLogModifica(db);
		val.put(Linee.NOME_LINEA, getTesto(R.id.editText_linea));

		ContentValues where = new ContentValues();
		where.put(Linee.ID_LINEA, getIDModifica());

		linee.aggiornaRecord(db, val, where);
		System.out.println("EConTab: LineeDettaglioModActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

}
