package pfa.app.econtab;

import android.content.ContentValues;
import android.os.Bundle;
import android.widget.EditText;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Listini;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabSpinner;

public class CodiceArticoloModActivity extends EConTabDettaglioActivity {
	private EConTabSpinner spinnerfornitore = null;
	private EConTabSpinner spinnerlinea = null;
	private EditText editCodice = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: CodiceArticoloModActivity onCreate ENTER");
		setContentView(R.layout.activity_codice_articolo_dettaglio_mod);
		spinnerfornitore = (EConTabSpinner) findViewById(R.id.econtabSpinner_fornitore);
		spinnerlinea = (EConTabSpinner) findViewById(R.id.econtabSpinner_linea);
		editCodice = (EditText) findViewById(R.id.editText_codice);

		super.onCreate(savedInstanceState);

		if (getModalita() == MODIFICA) {
			findViewById(R.id.editText_codice).setEnabled(false);
		} else {
			int idcostruttore = getIntent().getIntExtra(Listini.ID_COSTRUTTORE, 0);
			if (idcostruttore != 0) {
				spinnerfornitore.setValue("" + idcostruttore);
			}
			int idlinea = getIntent().getIntExtra(Listini.ID_LINEA, 0);
			if (idlinea != 0) {
				spinnerlinea.setValue("" + idlinea);
				// prendo il fornitore dalla linea
				DbInterno db = new DbInterno(this);
				ContentValues whereLinea = new ContentValues();
				whereLinea.put(Linee.ID_LINEA, idlinea);
				ContentValues valLinea = db.getRecord(new Linee(), whereLinea);
				if (valLinea != null) {

					spinnerfornitore.setValue("" + valLinea.getAsInteger(Linee.ID_COSTRUTTORE));
				}
				db.close();
			}
			spinnerfornitore.setTabella(new Costruttori());
			spinnerlinea.setTabella(new Linee());
			spinnerfornitore.setSpinnerCollegato(spinnerlinea);
		}
		System.out.println("EConTab: CodiceArticoloModActivity onCreate EXIT");
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("EConTab: CodiceArticoloModActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Listini.CODICE_ARTICOLO, getIDModificaStringa());
		ContentValues val = db.getRecord(new Listini(), where);
		db.close();
		if (val != null) {
			setText(R.id.editText_codice, getIDModificaStringa());
			setText(R.id.editText_descrizione, val.getAsString(Listini.DESCRIZIONE));
			setText(R.id.econtabSpinner_fornitore, val.getAsString(Listini.ID_COSTRUTTORE));
			setText(R.id.econtabSpinner_linea, val.getAsString(Listini.ID_LINEA));
			setText(R.id.editText_przAcq, Utility.formatNumero(val.getAsDouble(Listini.PRZ_ULTIMO_ACQUISTO), 2));
			setText(R.id.editText_przLis, Utility.formatNumero(val.getAsDouble(Listini.PRZ_LISTINO), 2));
			setText(R.id.editText_sconto, Utility.formatNumero(val.getAsDouble(Listini.SCONTO), 2));

		}

		spinnerfornitore.setTabella(new Costruttori());
		spinnerlinea.setTabella(new Linee());
		spinnerfornitore.setSpinnerCollegato(spinnerlinea);
		System.out.println("EConTab: CodiceArticoloModActivity inizializzaModifica EXIT");
		super.inizializzaModifica();
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("EConTab: CodiceArticoloModActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		String codice = editCodice.getText().toString().trim();
		if (codice.equals("")) {
			editCodice.setError(getString(R.string.errore_codice_articolo_obbligatorio));
			return getString(R.string.errore_codice_articolo_obbligatorio);
		}

		ContentValues whereChk = new ContentValues();
		whereChk.put(Listini.CODICE_ARTICOLO, codice);
		ContentValues valChk = db.getRecord(new Listini(), whereChk);
		if (valChk != null) {
			editCodice.setError(getString(R.string.errore_codice_articolo_esistente));
			return getString(R.string.errore_codice_articolo_esistente);
		}

		if (spinnerfornitore.getValue().equals("")) {
			spinnerfornitore.setError(getString(R.string.errore_selezione_fornitore));
			return getString(R.string.errore_selezione_fornitore);
		}

		Listini tabListini = new Listini();

		ContentValues val = tabListini.getValoriLogInserimento(db);

		val.put(Listini.CODICE_ARTICOLO, getTesto(R.id.editText_codice));
		val.put(Listini.DESCRIZIONE, getTesto(R.id.editText_descrizione));
		val.put(Listini.ID_COSTRUTTORE, spinnerfornitore.getValue());
		if (!spinnerlinea.getValue().equals("")) {
			val.put(Listini.ID_LINEA, spinnerlinea.getValue());
		}
		val.put(Listini.PRZ_ULTIMO_ACQUISTO, Utility.formatNumeroDB(getTesto(R.id.editText_przAcq)));
		val.put(Listini.PRZ_LISTINO, Utility.formatNumeroDB(getTesto(R.id.editText_przLis)));
		val.put(Listini.SCONTO, Utility.formatNumeroDB(getTesto(R.id.editText_sconto)));
		tabListini.inserisciRecord(db, val);
		System.out.println("EConTab: CodiceArticoloModActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("EConTab: CodiceArticoloModActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		if (spinnerfornitore.getValue().equals("")) {
			spinnerfornitore.setError(getString(R.string.errore_selezione_fornitore));
			return getString(R.string.errore_selezione_fornitore);
		}
		Listini tabListini = new Listini();

		ContentValues val = tabListini.getValoriLogModifica(db);

		val.put(Listini.DESCRIZIONE, getTesto(R.id.editText_descrizione));
		val.put(Listini.ID_COSTRUTTORE, spinnerfornitore.getValue());
		if (!spinnerlinea.getValue().equals("")) {
			val.put(Listini.ID_LINEA, spinnerlinea.getValue());
		} else {
			val.put(Listini.ID_LINEA, 0);
		}
		val.put(Listini.PRZ_ULTIMO_ACQUISTO, Utility.formatNumeroDB(getTesto(R.id.editText_przAcq)));
		val.put(Listini.PRZ_LISTINO, Utility.formatNumeroDB(getTesto(R.id.editText_przLis)));
		val.put(Listini.SCONTO, Utility.formatNumeroDB(getTesto(R.id.editText_sconto)));

		ContentValues where = new ContentValues();
		where.put(Listini.CODICE_ARTICOLO, getIDModificaStringa());

		tabListini.aggiornaRecord(db, val, where);
		System.out.println("EConTab: CodiceArticoloModActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

}
