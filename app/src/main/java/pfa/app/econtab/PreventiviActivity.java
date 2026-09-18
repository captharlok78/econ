package pfa.app.econtab;

import android.os.Bundle;
import androidx.fragment.app.FragmentManager;

import pfa.app.econtab.fragments.PreventiviListaFragment;

public class PreventiviActivity extends EConTabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: PreventiviActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_preventivi);
		System.out.println("EConTab: PreventiviActivity onCreate EXIT");
	}

	public void refresh() {
		System.out.println("EConTab: PreventiviActivity refresh ENTER");
		FragmentManager fm = getSupportFragmentManager();
		PreventiviListaFragment fragment = (PreventiviListaFragment) fm.findFragmentById(R.id.fragment1);
		if (fragment != null) {
			fragment.ricerca();
		}
		System.out.println("EConTab: PreventiviActivity refresh EXIT");
	}

	@Override
	protected void aggiornaDopoCancellazione() {
		refresh();
	}

}
