package pfa.app.econtab;

import android.os.Bundle;

import pfa.app.econtab.fragments.OrdiniListaFragment;

public class OrdiniActivity extends EConTabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: OrdiniActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ordini);
		System.out.println("EConTab: OrdiniActivity onCreate EXIT");
	}

	@Override
	protected void aggiornaDopoCancellazione() {
		OrdiniListaFragment fragment = (OrdiniListaFragment) getSupportFragmentManager().findFragmentById(R.id.fragment1);
		if (fragment != null) {
			fragment.ricerca();
		}
	}

}
