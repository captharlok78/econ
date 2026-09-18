package pfa.app.econtab.fragments;

import android.content.ContentValues;
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
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Preventivi;

public class PreventivoPagerFragment extends EConTabFragment {

	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;
	PreventivoDettaglioFragment fragment = null;

	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {

			if (position == 0) {
				fragment = new PreventivoDettaglioFragment();
				fragment.setArguments(getArguments());
				return fragment;
			}
			return null;

		}

		@Override
		public int getCount() {
			// Show 3 total pages.

			return 1;

		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();

			switch (position) {
			case 0:
            {
                if (getArguments() != null) {
                    int idPreventivo = getArguments().getInt(Preventivi.ID_PREVENTIVO);
                    ContentValues where = new ContentValues();
                    where.put(Preventivi.ID_PREVENTIVO,idPreventivo);
                    DbInterno db = new DbInterno(getActivity());
                    ContentValues rec = db.getRecord(new Preventivi(),where);
                    db.close();
                    if (rec!=null && rec.getAsString(Preventivi.TIPO).equals(Preventivi.TIPO_ORDINE)){
                        return getString(R.string.dettaglio_ordine).toUpperCase(l);
                    }
                }
                return getString(R.string.dettaglio_preventivo).toUpperCase(l);
            }


			}

			return "";
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View v = super.onCreateView(inflater, container, savedInstanceState);
		v = inflater.inflate(R.layout.fragment_preventivo_pager, container, false);

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

	public void importaDaFileXLS(String path) {
		fragment.importaDaFileXLS(path);
	}

}
