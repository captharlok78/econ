package pfa.app.econtab.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;

import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Cantieri;

public class CantierePagerFragment extends EConTabFragment {
	private int cantiere = 0;
	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;

	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {

			if (position == 0) {
				CantieriDettaglioFragment fragment = new CantieriDettaglioFragment();
				fragment.setArguments(getArguments());
				return fragment;
			}

            if (position == 1) {
                RelazioniListaFragment fragment = new RelazioniListaFragment();
                getArguments().putString("DA_CANTIERE","SI");
                fragment.setArguments(getArguments());
                return fragment;
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
				return getString(R.string.dati_cantiere).toUpperCase(l);
			case 1:
                return getString(R.string.relazioni_logiche).toUpperCase(l);
				// return getString(R.string.preventivi).toUpperCase(l);
				// case 2:
				// return getString(R.string.rapportini).toUpperCase(l);
			}

			return "";
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View v = super.onCreateView(inflater, container, savedInstanceState);
		v = inflater.inflate(R.layout.fragment_cantiere_pager, container, false);
		cantiere = getArguments().getInt(Cantieri.ID_CANTIERE);

		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onViewCreated(view, savedInstanceState);
		mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) view.findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
	}

}
