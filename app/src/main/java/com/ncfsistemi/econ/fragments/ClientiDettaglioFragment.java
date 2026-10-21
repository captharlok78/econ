package com.ncfsistemi.econ.fragments;

import android.content.ContentValues;

import com.ncfsistemi.econ.ClientiDettaglioActivity;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.form.ClienteForm;
import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.Form;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;

/**
 * Linguetta "Dati principali" della scheda cliente (GESTIONE_CLIENTI.md §6.8) sul template: le sezioni di ClienteForm
 * (dati principali e fatturazione) in sola visualizzazione, matita per la modifica. Indirizzi, referenti e cantieri
 * hanno la loro linguetta (ClienteElencoFragment).
 */
public class ClientiDettaglioFragment extends EconSchedaFragment {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new ClienteForm();
	}

	@Override
	protected ContentValues leggiRecord(DbInterno db) {
		ContentValues where = new ContentValues();
		where.put(Anagrafica.ID_ANAGRAFICA, getArguments().getInt(Anagrafica.ID_ANAGRAFICA));
		return db.getRecord(new Anagrafica(), where);
	}

	@Override
	protected void dopoCaricamento(Form form, ContentValues record) {
		if (record == null) return;
		titolo(record.getAsString(Anagrafica.RAGIONE_SOCIALE));
		if (FunzionalitaApp.puoModificare(getActivity(), FunzionalitaApp.CLIENTI)) {
			form.azione(0, FaIcone.MODIFICA, getString(R.string.modifica), x -> ((ClientiDettaglioActivity) getActivity()).modifica(x));
		}
	}
}
