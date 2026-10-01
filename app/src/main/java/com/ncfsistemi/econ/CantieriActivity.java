package com.ncfsistemi.econ;

import android.content.Intent;
import android.os.Bundle;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.fragments.CantieriListaFragment;

public class CantieriActivity extends EconActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("Econ: CantieriActivity onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cantieri);
	}

	@Override
	protected void aggiornaDopoCancellazione() {
		CantieriListaFragment fragment = (CantieriListaFragment) getSupportFragmentManager().findFragmentById(R.id.fragment1);
		if (fragment != null) {
			fragment.ricerca();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		System.out.println("Econ: CantieriActivity onActivityResult");
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode==2){
			if (resultCode==RESULT_OK){
				DbInterno db = new DbInterno(this);
				int ultimoCantiere = db.getUltimoId(new Cantieri());
				db.close();
				
				Intent intent = new Intent(this,CantiereSplitActivity.class);
				intent.putExtra(Cantieri.ID_CANTIERE, ultimoCantiere);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
			}
		}
	}

}
