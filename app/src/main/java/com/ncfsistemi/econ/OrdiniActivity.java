package com.ncfsistemi.econ;

import android.os.Bundle;

import com.ncfsistemi.econ.fragments.OrdiniListaFragment;

public class OrdiniActivity extends EconActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("Econ: OrdiniActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ordini);
		System.out.println("Econ: OrdiniActivity onCreate EXIT");
	}

	@Override
	protected void aggiornaDopoCancellazione() {
		OrdiniListaFragment fragment = (OrdiniListaFragment) getSupportFragmentManager().findFragmentById(R.id.fragment1);
		if (fragment != null) {
			fragment.ricerca();
		}
	}

}
