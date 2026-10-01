package com.ncfsistemi.econ;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.View;

import java.util.Locale;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.fragments.ClienteElencoFragment;
import com.ncfsistemi.econ.fragments.ClientiDettaglioFragment;

public class ClientiDettaglioActivity extends EconFragmentActivity {

	int tabselezionata = 0;
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
		System.out.println("Econ: ClientiDettaglioActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_clienti_dettaglio);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		System.out.println("Econ: ClientiDettaglioActivity onCreate EXIT");
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		/** Linguette (GESTIONE_CLIENTI.md §6.8): Dati principali (con la fatturazione), Indirizzi/Cantieri, Referenti. */
		@Override
		public Fragment getItem(int position) {
			Bundle arg = getIntent().getExtras() != null ? new Bundle(getIntent().getExtras()) : new Bundle();
			if (position == 0) {
				ClientiDettaglioFragment fragment = new ClientiDettaglioFragment();
				fragment.setArguments(arg);
				return fragment;
			}
			ClienteElencoFragment fragment = new ClienteElencoFragment();
			arg.putInt(ClienteElencoFragment.ARG_TIPO, position == 1 ? ClienteElencoFragment.INDIRIZZI_CANTIERI : ClienteElencoFragment.REFERENTI);
			fragment.setArguments(arg);
			return fragment;
		}

		@Override
		public int getCount() {
			return TITOLI.length;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return TITOLI[position].toUpperCase(Locale.getDefault());
		}
	}

	private static final String[] TITOLI = {"Dati principali", "Indirizzi/Cantieri", "Referenti"};

	public void modifica(View v) {
		System.out.println("Econ: ClientiDettaglioActivity modifica ENTER");
		Intent intent = new Intent(this, ClientiDettaglioModActivity.class);
		intent.putExtra("ID", getIntent().getIntExtra(Anagrafica.ID_ANAGRAFICA, 0));
		apriFinestraModifica(intent, 1);
		System.out.println("Econ: ClientiDettaglioActivity modifica EXIT");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		System.out.println("Econ: ClientiDettaglioActivity onActivityResult ENTER");
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
		System.out.println("Econ: ClientiDettaglioActivity onActivityResult EXIT");
	}

}
