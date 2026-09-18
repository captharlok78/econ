package pfa.app.econtab;

import android.content.ContentValues;
import android.os.Bundle;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Manodopera;
import pfa.app.econtab.utils.Utility;

public class ManodoperaDettaglioActivity extends EConTabDettaglioActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: ManodoperaDettaglioActivity onCreate ENTER");
		setContentView(R.layout.activity_manodopera_dettaglio);
		super.onCreate(savedInstanceState);

		if (getModalita() == INSERIMENTO) {

			setText(R.id.editText_numero_op, "1");
			setText(R.id.editText_costo_orario, "30,00");
		}
		System.out.println("EConTab: ManodoperaDettaglioActivity onCreate EXIT");
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("EConTab: ManodoperaDettaglioActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		Manodopera manodoperaTab = new Manodopera();
		ContentValues val = manodoperaTab.getValoriLogInserimento(db);
		val.put(Manodopera.NOME, getTesto(R.id.editText_manodopera));
		String numOperatori = getTesto(R.id.editText_numero_op);
		int numOpInt = 1;
		if (numOperatori.trim().equals("") || numOperatori.trim().equals("0")) {
			numOpInt = 1;
		} else {
			numOpInt = Integer.parseInt(numOperatori);
		}
		val.put(Manodopera.NUM_OPERATORI_DEFAULT, numOpInt);
		val.put(Manodopera.COSTO_ORARIO, Utility.formatNumeroDB(getTesto(R.id.editText_costo_orario)));
		long resultinsert = manodoperaTab.inserisciRecord(db, val);

		if (resultinsert == -1) {
			return getString(R.string.errore_inserimento);
		}
		System.out.println("EConTab: ManodoperaDettaglioActivity eseguiInserimento EXIT");
		return SALVATAGGIO_OK;
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("EConTab: ManodoperaDettaglioActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		Manodopera manodoperaTab = new Manodopera();
		ContentValues val = manodoperaTab.getValoriLogModifica(db);
		val.put(Manodopera.NOME, getTesto(R.id.editText_manodopera));
		String numOperatori = getTesto(R.id.editText_numero_op);
		int numOpInt = 1;
		if (numOperatori.trim().equals("") || numOperatori.trim().equals("0")) {
			numOpInt = 1;
		} else {
			numOpInt = Integer.parseInt(numOperatori);
		}
		val.put(Manodopera.NUM_OPERATORI_DEFAULT, numOpInt);
		val.put(Manodopera.COSTO_ORARIO, Utility.formatNumeroDB(getTesto(R.id.editText_costo_orario)));

		ContentValues where = new ContentValues();
		where.put(Manodopera.ID_MANODOPERA, getIDModifica());

		manodoperaTab.aggiornaRecord(db, val, where);
		System.out.println("EConTab: ManodoperaDettaglioActivity eseguiAggiornamento EXIT");
		return SALVATAGGIO_OK;
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("EConTab: ManodoperaDettaglioActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		super.inizializzaModifica();
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Manodopera.ID_MANODOPERA, getIDModifica());
		ContentValues val = db.getRecord(new Manodopera(), where);
		// disabilitaCampo(R.id.editText_udm);
		if (val != null) {
			setText(R.id.editText_manodopera, val.getAsString(Manodopera.NOME));
			setText(R.id.editText_numero_op, val.getAsString(Manodopera.NUM_OPERATORI_DEFAULT));
			setText(R.id.editText_costo_orario, Utility.formatNumero(val.getAsFloat(Manodopera.COSTO_ORARIO), 2));
		}
		db.close();
		System.out.println("EConTab: ManodoperaDettaglioActivity inizializzaModifica EXIT");
	}

}
