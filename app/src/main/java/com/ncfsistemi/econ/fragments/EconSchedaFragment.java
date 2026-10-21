package com.ncfsistemi.econ.fragments;

import android.content.ContentValues;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.Form;

/**
 * Scheda in sola visualizzazione sul template form (STUDIO_FORM_APP.md §7.6): le stesse sezioni e righe della form di
 * modifica (stessa DefinizioneForm), ogni campo come etichetta e valore. Il modulo da' la definizione, il record e le
 * azioni nelle testate (matita, mappa); si ridisegna a ogni ritorno, cosi' dopo una modifica e' gia' aggiornata.
 */
public abstract class EconSchedaFragment extends EconFragment {

	protected abstract DefinizioneForm creaDefinizione();

	/** Il record da mostrare (null = non c'e' piu'). */
	protected abstract ContentValues leggiRecord(DbInterno db);

	/** Dopo il disegno: azioni nelle testate (form.azione), dati calcolati (form.aggiungiDato), titolo della pagina. */
	protected void dopoCaricamento(Form form, ContentValues record) {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.form_scheda, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();
		refresh();
	}

	public void refresh() {
		View v = getView();
		if (v == null || getActivity() == null) return;
		DbInterno db = new DbInterno(getActivity());
		ContentValues record = leggiRecord(db);
		db.close();
		Form form = creaDefinizione().disegnaScheda(getActivity(), v.findViewById(R.id.form_contenitore));
		if (record != null) form.carica(record);
		dopoCaricamento(form, record);
	}

	/** Titolo della testata della pagina (es. il nome del cliente). */
	protected void titolo(String testo) {
		TextView t = getActivity() == null ? null : getActivity().findViewById(R.id.headerTitolo);
		if (t != null && testo != null) t.setText(testo);
	}
}
