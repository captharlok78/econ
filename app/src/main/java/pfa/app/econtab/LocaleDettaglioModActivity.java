package pfa.app.econtab;

import android.content.ContentValues;
import android.os.Bundle;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Aree;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Placche;
import pfa.app.econtab.views.EConTabSpinner;

public class LocaleDettaglioModActivity extends EConTabDettaglioActivity {

	private EConTabSpinner spinner_linea = null;
	private EConTabSpinner spinner_placca = null;

	private String idLineaPrec = "0";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: LocaleDettaglioModActivity onCreate ENTER");
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
				setText(R.id.econtabSpinner_linea, valArea.getAsString(Aree.ID_LINEA));
				setText(R.id.econtabSpinner_placca, valArea.getAsString(Aree.ID_PLACCA));
			}
		}
		spinner_linea = (EConTabSpinner) findViewById(R.id.econtabSpinner_linea);
		spinner_placca = (EConTabSpinner) findViewById(R.id.econtabSpinner_placca);
		spinner_linea.setTabella(new Linee());
		spinner_placca.setTabella(new Placche());
		spinner_placca.setMessaggioDisabilitato(getString(R.string.errore_selezione_linea));
		spinner_linea.setSpinnerCollegato(spinner_placca);
		System.out.println("EConTab: LocaleDettaglioModActivity onCreate EXIT");
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("EConTab: LocaleDettaglioModActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		super.inizializzaModifica();
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Locali.ID_LOCALE, getIDModifica());
		ContentValues val = db.getRecord(new Locali(), where);
		db.close();
		if (val != null) {
			setText(R.id.editText_locale, val.getAsString(Locali.NOME));
			setText(R.id.econtabSpinner_linea, val.getAsString(Locali.ID_LINEA));
			idLineaPrec = val.getAsString(Locali.ID_LINEA);
			setText(R.id.econtabSpinner_placca, val.getAsString(Locali.ID_PLACCA));
			setText(R.id.editText_note, val.getAsString(Locali.NOTE));
		}
		System.out.println("EConTab: LocaleDettaglioModActivity inizializzaModifica EXIT");
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("EConTab: LocaleDettaglioModActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		Locali locali = new Locali();
		ContentValues val = locali.getValoriLogInserimento(db);
		val.put(Locali.ID_AREA, getIntent().getIntExtra(Aree.ID_AREA, 0));
		val.put(Locali.NOME, getTesto(R.id.editText_locale));
		val.put(Locali.ID_LINEA, getTesto(R.id.econtabSpinner_linea));
		val.put(Locali.ID_PLACCA, getTesto(R.id.econtabSpinner_placca));
		val.put(Locali.NOTE, getTesto(R.id.editText_note));

		locali.inserisciRecord(db, val);
		System.out.println("EConTab: LocaleDettaglioModActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("EConTab: LocaleDettaglioModActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		Locali locali = new Locali();
		ContentValues val = locali.getValoriLogModifica(db);

		val.put(Locali.NOME, getTesto(R.id.editText_locale));
		val.put(Locali.ID_LINEA, getTesto(R.id.econtabSpinner_linea));
		val.put(Locali.ID_PLACCA, getTesto(R.id.econtabSpinner_placca));
		val.put(Locali.NOTE, getTesto(R.id.editText_note));

		ContentValues where = new ContentValues();
		where.put(Locali.ID_LOCALE, getIDModifica());
		locali.aggiornaRecord(db, val, where);
		System.out.println("EConTab: LocaleDettaglioModActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

	@Override
	public String getMessaggioConfermaSalvataggio() {
		System.out.println("EConTab: LocaleDettaglioModActivity getMessaggioConfermaSalvataggio ENTER");
		// TODO Auto-generated method stub
		String idLineaNuova = getTesto(R.id.econtabSpinner_linea);
		if (idLineaNuova.equals("")) {
			idLineaNuova = "0";
		}
		if (getModalita() == MODIFICA && !idLineaNuova.equals(idLineaPrec)) {
			return getString(R.string.messaggio_variazione_linea);
		}
		System.out.println("EConTab: LocaleDettaglioModActivity getMessaggioConfermaSalvataggio EXIT");
		return super.getMessaggioConfermaSalvataggio();
	}

}
