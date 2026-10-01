package com.ncfsistemi.econ;

import android.content.ContentValues;
import android.os.Bundle;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Iva;

public class IvaDettaglioActivity extends EconDettaglioActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("Econ: IvaDettaglioActivity onCreate ENTER");
		setContentView(R.layout.activity_iva_dettaglio);
		super.onCreate(savedInstanceState);
		System.out.println("Econ: IvaDettaglioActivity onCreate EXIT");
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("Econ: IvaDettaglioActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		Iva ivaTab = new Iva();
		ContentValues val = ivaTab.getValoriLogInserimento(db);
		val.put(Iva.CODICE_IVA, getTesto(R.id.editText_codice));
		val.put(Iva.ALIQUOTA, getTesto(R.id.editText_aliquota));
		long resultinsert = ivaTab.inserisciRecord(db, val);
		if (resultinsert == -1) {
			return getString(R.string.errore_inserimento);
		}
		System.out.println("Econ: IvaDettaglioActivity eseguiInserimento EXIT");
		return SALVATAGGIO_OK;
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("Econ: IvaDettaglioActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		Iva ivaTab = new Iva();
		ContentValues val = ivaTab.getValoriLogModifica(db);
		val.put(Iva.CODICE_IVA, getTesto(R.id.editText_codice));
		val.put(Iva.ALIQUOTA, getTesto(R.id.editText_aliquota));

		ContentValues where = new ContentValues();
		where.put(Iva.CODICE_IVA, getIDModificaStringa());

		ivaTab.aggiornaRecord(db, val, where);
		System.out.println("Econ: IvaDettaglioActivity eseguiAggiornamento EXIT");
		return SALVATAGGIO_OK;
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("Econ: IvaDettaglioActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		super.inizializzaModifica();
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Iva.CODICE_IVA, getIntent().getStringExtra("ID"));
		ContentValues val = db.getRecord(new Iva(), where);
		disabilitaCampo(R.id.editText_codice);
		if (val != null) {
			setText(R.id.editText_codice, val.getAsString(Iva.CODICE_IVA));
			setText(R.id.editText_aliquota, val.getAsString(Iva.ALIQUOTA));
		}
		db.close();
		System.out.println("Econ: IvaDettaglioActivity inizializzaModifica EXIT");
	}

}
