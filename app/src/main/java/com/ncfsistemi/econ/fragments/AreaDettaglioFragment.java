package com.ncfsistemi.econ.fragments;

import android.content.ContentValues;
import android.content.Intent;

import com.ncfsistemi.econ.EconActivity;
import com.ncfsistemi.econ.AreaDettaglioModActivity;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Aree;
import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.Form;
import com.ncfsistemi.econ.form.AreaForm;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;

/** Scheda dell'area del cantiere (gestione elettrica) sul template: stesse sezioni di AreaForm, matita per la modifica. */
public class AreaDettaglioFragment extends EconSchedaFragment {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new AreaForm();
	}

	private int id() {
		return getArguments().getInt(Aree.ID_AREA);
	}

	@Override
	protected ContentValues leggiRecord(DbInterno db) {
		ContentValues where = new ContentValues();
		where.put(Aree.ID_AREA, id());
		return db.getRecord(new Aree(), where);
	}

	@Override
	protected void dopoCaricamento(Form form, ContentValues record) {
		if (record == null) return;

		if (!FunzionalitaApp.puoModificare(getActivity(), FunzionalitaApp.ELETTRICO)) return;
		form.azione(0, FaIcone.MODIFICA, getString(R.string.modifica), x -> {
			Intent intent = new Intent(getActivity(), AreaDettaglioModActivity.class);
			intent.putExtra("ID", id());
			((EconActivity) getActivity()).apriFinestraModifica(intent, 1);
		});
	}
}
