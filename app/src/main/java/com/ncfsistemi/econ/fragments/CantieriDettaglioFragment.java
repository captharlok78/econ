package com.ncfsistemi.econ.fragments;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import com.ncfsistemi.econ.CantieriDettaglioModActivity;
import com.ncfsistemi.econ.EconActivity;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;
import com.ncfsistemi.econ.utils.SezioneStandard;
import com.ncfsistemi.econ.utils.Utility;

/**
 * Linguetta "Dati cantiere" della scheda del cantiere, standard grafico (STANDARD_GRAFICO.md): sezione con modifica e
 * mappa a icona, dati in sola lettura. Linea e placca standard sono nella linguetta "Gestione elettrica"
 * (CantiereElettricoFragment, GESTIONE_ELETTRICA.md).
 */
public class CantieriDettaglioFragment extends EconFragment {
	private int cantiere = 0;
	private String luogo = "";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.fragment_cantieri_dettaglio, container, false);
		cantiere = getArguments().getInt(Cantieri.ID_CANTIERE);
		View sezione = v.findViewById(R.id.sezione_dati);
		SezioneStandard.imposta(sezione, FaIcone.CANTIERE, getString(R.string.dati_cantiere));
		if (FunzionalitaApp.puoModificare(getActivity(), FunzionalitaApp.CANTIERI)) {
			SezioneStandard.azione(sezione, FaIcone.MODIFICA, getString(R.string.modifica), x -> {
				Intent intent = new Intent(getActivity(), CantieriDettaglioModActivity.class);
				intent.putExtra("ID", cantiere);
				((EconActivity) getActivity()).apriFinestraModifica(intent, 1);
			});
		}
		SezioneStandard.azione(sezione, FaIcone.MAPPA, "Mappa", x -> Utility.mostraMappa(getActivity(), luogo));
		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		refresh();
	}

	public void refresh() {
		DbInterno db = new DbInterno(getActivity());
		String sql = "SELECT c.*, a." + Anagrafica.RAGIONE_SOCIALE + " FROM " + Cantieri.NOME_TABELLA + " c LEFT JOIN "
				+ Anagrafica.NOME_TABELLA + " a ON a." + Anagrafica.ID_ANAGRAFICA + " = c." + Cantieri.ID_ANAGRAFICA
				+ " WHERE c." + Cantieri.ID_CANTIERE + " = " + cantiere;
		ArrayList<Object> records = db.eseguiSelect(sql, null);
		db.close();
		View v = getView();
		if (v == null || records.isEmpty()) {
			return;
		}
		ContentValues val = (ContentValues) records.get(0);
		String via = (testo(val, Cantieri.INDIRIZZO) + " " + testo(val, Cantieri.CIVICO)).trim();
		String paese = (testo(val, Cantieri.CAP) + " " + testo(val, Cantieri.CITTA)
				+ (testo(val, Cantieri.PROVINCIA).isEmpty() ? "" : " (" + testo(val, Cantieri.PROVINCIA) + ")")).trim();
		luogo = (via + ", " + paese).trim();
		setText(R.id.editText_cantiere, valore(testo(val, Cantieri.NOME)), v);
		setText(R.id.editText_ragionesociale, valore(testo(val, Anagrafica.RAGIONE_SOCIALE)), v);
		setText(R.id.editText_indirizzo, valore(via), v);
		setText(R.id.editText_luogo, valore(paese), v);
		setText(R.id.editText_note, valore(testo(val, Cantieri.NOTE)), v);
		setText(R.id.text_durata, durata(val), v);
	}

	/** "Permanente (segue il cliente)" oppure "A termine · aperto dal … · chiude il … / chiuso dal …" (STUDIO_DATE_CANTIERI.md). */
	private static String durata(ContentValues val) {
		Long apertura = val.getAsLong(Cantieri.DATA_APERTURA);
		Long chiusura = val.getAsLong(Cantieri.DATA_CHIUSURA);
		Integer permanente = val.getAsInteger(Cantieri.PERMANENTE);
		StringBuilder s = new StringBuilder(permanente != null && permanente == 1 ? "Permanente (segue il cliente)" : "A termine");
		if (apertura != null && apertura > 0) {
			s.append(" · aperto dal ").append(data(apertura));
		}
		if ((permanente == null || permanente == 0) && chiusura != null && chiusura > 0) {
			java.util.Calendar oggi = java.util.Calendar.getInstance();
			long giorno = oggi.get(java.util.Calendar.YEAR) * 10000L + (oggi.get(java.util.Calendar.MONTH) + 1) * 100L + oggi.get(java.util.Calendar.DAY_OF_MONTH);
			s.append(chiusura / 1000000L < giorno ? " · chiuso, ultimo giorno " : " · ultimo giorno di lavoro ").append(data(chiusura));
		}
		return s.toString();
	}

	private static String data(long yyyyMMddHHmmss) {
		String s = String.valueOf(yyyyMMddHHmmss / 1000000L);
		return s.length() < 8 ? "" : s.substring(6, 8) + "/" + s.substring(4, 6) + "/" + s.substring(0, 4);
	}

	private static String valore(String s) {
		return s.isEmpty() ? "—" : s;
	}

	private static String testo(ContentValues v, String campo) {
		String s = v.getAsString(campo);
		return s == null ? "" : s.trim();
	}
}
