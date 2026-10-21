package com.ncfsistemi.econ.fragments;

import android.content.ContentValues;
import android.content.Intent;

import com.ncfsistemi.econ.EconActivity;
import com.ncfsistemi.econ.LocaleDettaglioModActivity;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Locali;
import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.Form;
import com.ncfsistemi.econ.form.LocaleForm;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;

/** Scheda del locale del cantiere (gestione elettrica) sul template: stesse sezioni di LocaleForm, matita per la modifica. */
public class LocaleDettaglioFragment extends EconSchedaFragment {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new LocaleForm();
	}

	private int id() {
		return getArguments().getInt(Locali.ID_LOCALE);
	}

	@Override
	protected ContentValues leggiRecord(DbInterno db) {
		ContentValues where = new ContentValues();
		where.put(Locali.ID_LOCALE, id());
		return db.getRecord(new Locali(), where);
	}

	@Override
	protected void dopoCaricamento(Form form, ContentValues record) {
		if (record == null) return;
		titolo(record.getAsString(Locali.NOME));
		if (!FunzionalitaApp.puoModificare(getActivity(), FunzionalitaApp.ELETTRICO)) return;
		form.azione(0, FaIcone.MODIFICA, getString(R.string.modifica), x -> {
			Intent intent = new Intent(getActivity(), LocaleDettaglioModActivity.class);
			intent.putExtra("ID", id());
			((EconActivity) getActivity()).apriFinestraModifica(intent, 10);
		});
	}
}
