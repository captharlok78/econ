package com.ncfsistemi.econ;

import android.os.Bundle;
import androidx.fragment.app.FragmentManager;

import com.ncfsistemi.econ.fragments.PreventiviListaFragment;

public class PreventiviActivity extends EconActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("Econ: PreventiviActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_preventivi);
		System.out.println("Econ: PreventiviActivity onCreate EXIT");
	}

	public void refresh() {
		System.out.println("Econ: PreventiviActivity refresh ENTER");
		FragmentManager fm = getSupportFragmentManager();
		PreventiviListaFragment fragment = (PreventiviListaFragment) fm.findFragmentById(R.id.fragment1);
		if (fragment != null) {
			fragment.ricerca();
		}
		System.out.println("Econ: PreventiviActivity refresh EXIT");
	}

	@Override
	protected void aggiornaDopoCancellazione() {
		refresh();
	}

}
