package com.ncfsistemi.econ.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.utils.FunzionalitaApp;

/**
 * Linguette della scheda del cantiere, secondo i moduli della persona: Dati cantiere sempre; Gestione elettrica con il
 * modulo ELETTRICO (GESTIONE_ELETTRICA.md); Rapportini e Materiali con il modulo Rapportini (STORICO_CANTIERE.md).
 */
public class CantierePagerFragment extends EconFragment {
	private static final String DATI = "DATI", ELETTRICO = "ELETTRICO", RAPPORTINI = "RAPPORTINI", MATERIALI = "MATERIALI";

	private final List<String> linguette = new ArrayList<>();
	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;

	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			String linguetta = linguette.get(position);
			Fragment fragment;
			Bundle params = new Bundle(getArguments());
			if (ELETTRICO.equals(linguetta)) {
				fragment = new CantiereElettricoFragment();
			} else if (RAPPORTINI.equals(linguetta) || MATERIALI.equals(linguetta)) {
				fragment = new CantiereStoricoFragment();
				params.putString(CantiereStoricoFragment.MODO, RAPPORTINI.equals(linguetta)
						? CantiereStoricoFragment.RAPPORTINI : CantiereStoricoFragment.MATERIALI);
			} else {
				fragment = new CantieriDettaglioFragment();
			}
			fragment.setArguments(params);
			return fragment;
		}

		@Override
		public int getCount() {
			return linguette.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (linguette.get(position)) {
			case ELETTRICO:
				return getString(R.string.gestione_elettrica).toUpperCase(l);
			case RAPPORTINI:
				return getString(R.string.rapportini).toUpperCase(l);
			case MATERIALI:
				return getString(R.string.materiali).toUpperCase(l);
			default:
				return getString(R.string.dati_cantiere).toUpperCase(l);
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.fragment_cantiere_pager, container, false);
		linguette.clear();
		linguette.add(DATI);
		if (FunzionalitaApp.haModulo(getActivity(), FunzionalitaApp.ELETTRICO)) {
			linguette.add(ELETTRICO);
		}
		if (FunzionalitaApp.haModulo(getActivity(), FunzionalitaApp.RAPPORTINI)) {
			linguette.add(RAPPORTINI);
			linguette.add(MATERIALI);
		}
		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());
		mViewPager = (ViewPager) view.findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
	}

}
