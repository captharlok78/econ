package pfa.app.econtab;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.View;

import java.util.Locale;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.fragments.CantieriListaFragment;
import pfa.app.econtab.fragments.ClientiDettaglioFragment;

public class ClientiDettaglioActivity extends EConTabFragmentActivity {

	int tabselezionata = 0;
	CantieriListaFragment fragmentCantieri = null;
	/**
	 * The {@link androidx.core.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link androidx.core.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link androidx.core.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: ClientiDettaglioActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_clienti_dettaglio);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		System.out.println("EConTab: ClientiDettaglioActivity onCreate EXIT");
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			if (position == 0) {
				ClientiDettaglioFragment fragment = new ClientiDettaglioFragment();
				fragment.setArguments(getIntent().getExtras());
				return fragment;
			}

			if (position == 1) {
				fragmentCantieri = null;
				fragmentCantieri = new CantieriListaFragment();
				fragmentCantieri.setArguments(getIntent().getExtras());
				return fragmentCantieri;
			}
			

			return null;
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1_clienti)
						.toUpperCase(l);
			case 1:
				return getString(R.string.title_section2_clienti)
						.toUpperCase(l);
			case 2:
				return getString(R.string.title_section3_clienti)
						.toUpperCase(l);
			

			}
			return null;
		}
	}

	public void modifica(View v) {
		System.out.println("EConTab: ClientiDettaglioActivity modifica ENTER");
		Intent intent = new Intent(this, ClientiDettaglioModActivity.class);
		intent.putExtra("ID", getIntent().getIntExtra(Anagrafica.ID_ANAGRAFICA, 0));
		apriFinestraModifica(intent, 1);
		System.out.println("EConTab: ClientiDettaglioActivity modifica EXIT");
	}

	public void nuovoCantiere(View v) {
		System.out.println("EConTab: ClientiDettaglioActivity nuovoCantiere ENTER");
		Intent intent = new Intent(this, CantieriDettaglioModActivity.class);
		intent.putExtras(getIntent().getExtras());
		apriFinestraInserimento(intent, 2, new Cantieri());
		System.out.println("EConTab: ClientiDettaglioActivity nuovoCantiere EXIT");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		System.out.println("EConTab: ClientiDettaglioActivity onActivityResult ENTER");
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				Intent intent = new Intent(this, ClientiDettaglioActivity.class);
				intent.putExtras(getIntent().getExtras());
				startActivity(intent);
				finish();
			}
		}

		if (requestCode == 2) {
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
		System.out.println("EConTab: ClientiDettaglioActivity onActivityResult EXIT");
	}

	@Override
	protected void aggiornaDopoCancellazione() {
		// TODO Auto-generated method stub
		if (fragmentCantieri != null) {
			fragmentCantieri.ricerca();
		}
	}

}
