package pfa.app.econtab;

import android.content.ContentValues;
import android.os.Bundle;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Costruttori;

public class CostruttoriDettaglioActivity extends EConTabDettaglioActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		setContentView(R.layout.activity_costruttori_dettaglio);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("EConTab: CostruttoriDettaglioActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		super.inizializzaModifica();
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Costruttori.ID_COSTRUTTORE, getIDModifica());
		ContentValues val = db.getRecord(new Costruttori(), where);
		if (val != null) {
			setText(R.id.editText_ragionesociale, val.getAsString(Costruttori.RAGIONE_SOCIALE));
			setText(R.id.editText_siglametel, val.getAsString(Costruttori.SIGLA_METEL));
			// Per i costruttori di default non si pu� modificare la sigla metel
			if (val.getAsInteger(Costruttori.ID_COSTRUTTORE) > 0) {
				disabilitaCampo(R.id.editText_siglametel);
			}
		}
		System.out.println("EConTab: CostruttoriDettaglioActivity inizializzaModifica EXIT");
		db.close();

	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("EConTab: CostruttoriDettaglioActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		Costruttori costruttoriTab = new Costruttori();
		ContentValues val = costruttoriTab.getValoriLogInserimento(db);
		val.put(Costruttori.RAGIONE_SOCIALE, getTesto(R.id.editText_ragionesociale));
		val.put(Costruttori.SIGLA_METEL, getTesto(R.id.editText_siglametel));
		costruttoriTab.inserisciRecord(db, val);
		System.out.println("EConTab: CostruttoriDettaglioActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("EConTab: CostruttoriDettaglioActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		Costruttori costruttoriTab = new Costruttori();
		ContentValues val = costruttoriTab.getValoriLogModifica(db);
		val.put(Costruttori.RAGIONE_SOCIALE, getTesto(R.id.editText_ragionesociale));
		val.put(Costruttori.SIGLA_METEL, getTesto(R.id.editText_siglametel));

		ContentValues where = new ContentValues();
		where.put(Costruttori.ID_COSTRUTTORE, getIDModifica());

		costruttoriTab.aggiornaRecord(db, val, where);
		System.out.println("EConTab: CostruttoriDettaglioActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

}
