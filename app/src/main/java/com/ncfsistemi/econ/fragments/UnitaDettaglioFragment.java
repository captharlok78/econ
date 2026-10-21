package com.ncfsistemi.econ.fragments;

import android.content.ContentValues;
import android.content.Intent;

import com.ncfsistemi.econ.EconActivity;
import com.ncfsistemi.econ.UnitaDettaglioModActivity;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Unita;
import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.Form;
import com.ncfsistemi.econ.form.UnitaForm;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;

/** Scheda dell'unità del cantiere (gestione elettrica) sul template: stesse sezioni di UnitaForm, matita per la modifica. */
public class UnitaDettaglioFragment extends EconSchedaFragment {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new UnitaForm();
	}

	private int id() {
		return getArguments().getInt(Unita.ID_UNITA);
	}

	@Override
	protected ContentValues leggiRecord(DbInterno db) {
		ContentValues where = new ContentValues();
		where.put(Unita.ID_UNITA, id());
		return db.getRecord(new Unita(), where);
	}

	@Override
	protected void dopoCaricamento(Form form, ContentValues record) {
		if (record == null) return;

		if (!FunzionalitaApp.puoModificare(getActivity(), FunzionalitaApp.ELETTRICO)) return;
		form.azione(0, FaIcone.MODIFICA, getString(R.string.modifica), x -> {
			Intent intent = new Intent(getActivity(), UnitaDettaglioModActivity.class);
			intent.putExtra("ID", id());
			((EconActivity) getActivity()).apriFinestraModifica(intent, 1);
		});
	}
}
