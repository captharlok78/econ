package pfa.app.econtab;

import android.content.ContentValues;
import android.os.Bundle;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.UnitaMisura;

public class UnitaMisuraDettaglioActivity extends EConTabDettaglioActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		System.out.println("EConTab: UnitaMisuraDettaglioActivity onCreate ENTER");
		setContentView(R.layout.activity_unita_misura_dettaglio);
		System.out.println("EConTab: UnitaMisuraDettaglioActivity onCreate EXIT");
		super.onCreate(savedInstanceState);
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("EConTab: UnitaMisuraDettaglioActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		UnitaMisura udmTab = new UnitaMisura();
		ContentValues val = udmTab.getValoriLogInserimento(db);
		val.put(UnitaMisura.UNITA_MISURA, getTesto(R.id.editText_udm));
		val.put(UnitaMisura.NOME, getTesto(R.id.editText_nome));
		long resultinsert = udmTab.inserisciRecord(db, val);
		if (resultinsert == -1) {
			return getString(R.string.errore_inserimento);
		}
		System.out.println("EConTab: UnitaMisuraDettaglioActivity eseguiInserimento EXIT");
		return SALVATAGGIO_OK;
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("EConTab: UnitaMisuraDettaglioActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		UnitaMisura udmTab = new UnitaMisura();
		ContentValues val = udmTab.getValoriLogModifica(db);
		val.put(UnitaMisura.UNITA_MISURA, getTesto(R.id.editText_udm));
		val.put(UnitaMisura.NOME, getTesto(R.id.editText_nome));

		ContentValues where = new ContentValues();
		where.put(UnitaMisura.UNITA_MISURA, getIDModificaStringa());

		udmTab.aggiornaRecord(db, val, where);
		System.out.println("EConTab: UnitaMisuraDettaglioActivity eseguiAggiornamento EXIT");
		return SALVATAGGIO_OK;
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("EConTab: UnitaMisuraDettaglioActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		super.inizializzaModifica();
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(UnitaMisura.UNITA_MISURA, getIntent().getStringExtra("ID"));
		ContentValues val = db.getRecord(new UnitaMisura(), where);
		disabilitaCampo(R.id.editText_udm);
		if (val != null) {
			setText(R.id.editText_udm, val.getAsString(UnitaMisura.UNITA_MISURA));
			setText(R.id.editText_nome, val.getAsString(UnitaMisura.NOME));
		}
		db.close();
		System.out.println("EConTab: UnitaMisuraDettaglioActivity inizializzaModifica EXIT");
	}

}
