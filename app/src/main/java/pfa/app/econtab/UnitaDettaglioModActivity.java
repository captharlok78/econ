package pfa.app.econtab;

import android.content.ContentValues;
import android.os.Bundle;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Placche;
import pfa.app.econtab.db.table.Unita;
import pfa.app.econtab.views.EConTabSpinner;

public class UnitaDettaglioModActivity extends EConTabDettaglioActivity {

	private EConTabSpinner spinner_linea = null;
	private EConTabSpinner spinner_placca = null;

	private String idLineaPrec = "0";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: UnitaDettaglioModActivity onCreate ENTER");
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
				setText(R.id.econtabSpinner_linea, valCantiere.getAsString(Cantieri.ID_LINEA));
				setText(R.id.econtabSpinner_placca, valCantiere.getAsString(Cantieri.ID_PLACCA));
			}
		}
		spinner_linea = (EConTabSpinner) findViewById(R.id.econtabSpinner_linea);
		spinner_placca = (EConTabSpinner) findViewById(R.id.econtabSpinner_placca);
		spinner_linea.setTabella(new Linee());
		spinner_placca.setTabella(new Placche());
		spinner_placca.setMessaggioDisabilitato(getString(R.string.errore_selezione_linea));
		spinner_linea.setSpinnerCollegato(spinner_placca);
		System.out.println("EConTab: UnitaDettaglioModActivity onCreate EXIT");
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("EConTab: UnitaDettaglioModActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		super.inizializzaModifica();
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Unita.ID_UNITA, getIDModifica());
		ContentValues val = db.getRecord(new Unita(), where);
		db.close();
		if (val != null) {
			setText(R.id.editText_unita, val.getAsString(Unita.NOME));
			setText(R.id.econtabSpinner_linea, val.getAsString(Unita.ID_LINEA));
			idLineaPrec = val.getAsString(Unita.ID_LINEA);
			setText(R.id.econtabSpinner_placca, val.getAsString(Unita.ID_PLACCA));
			setText(R.id.editText_note, val.getAsString(Unita.NOTE));
		}
		System.out.println("EConTab: UnitaDettaglioModActivity inizializzaModifica EXIT");
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("EConTab: UnitaDettaglioModActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		Unita unita = new Unita();
		ContentValues val = unita.getValoriLogInserimento(db);
		val.put(Unita.ID_CANTIERE, getIntent().getIntExtra(Cantieri.ID_CANTIERE, 0));
		val.put(Unita.NOME, getTesto(R.id.editText_unita));
		val.put(Unita.ID_LINEA, getTesto(R.id.econtabSpinner_linea));
		val.put(Unita.ID_PLACCA, getTesto(R.id.econtabSpinner_placca));
		val.put(Unita.NOTE, getTesto(R.id.editText_note));
		unita.inserisciRecord(db, val);
		System.out.println("EConTab: UnitaDettaglioModActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("EConTab: UnitaDettaglioModActivity eseguiAggiornamento ENTER");
		Unita unita = new Unita();
		ContentValues val = unita.getValoriLogModifica(db);
		val.put(Unita.NOME, getTesto(R.id.editText_unita));
		val.put(Unita.ID_LINEA, getTesto(R.id.econtabSpinner_linea));
		val.put(Unita.ID_PLACCA, getTesto(R.id.econtabSpinner_placca));
		val.put(Unita.NOTE, getTesto(R.id.editText_note));

		ContentValues where = new ContentValues();
		where.put(Unita.ID_UNITA, getIDModifica());
		unita.aggiornaRecord(db, val, where);
		System.out.println("EConTab: UnitaDettaglioModActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

	@Override
	public String getMessaggioConfermaSalvataggio() {
		System.out.println("EConTab: UnitaDettaglioModActivity getMessaggioConfermaSalvataggio ENTER");
		// TODO Auto-generated method stub
		String idLineaNuova = getTesto(R.id.econtabSpinner_linea);
		if (idLineaNuova.equals("")) {
			idLineaNuova = "0";
		}
		if (getModalita() == MODIFICA && !idLineaNuova.equals(idLineaPrec)) {
			return getString(R.string.messaggio_variazione_linea);
		}
		System.out.println("EConTab: UnitaDettaglioModActivity getMessaggioConfermaSalvataggio EXIT");
		return super.getMessaggioConfermaSalvataggio();
	}

}
