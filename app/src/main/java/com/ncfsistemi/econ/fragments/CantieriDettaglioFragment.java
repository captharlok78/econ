package com.ncfsistemi.econ.fragments;

import android.content.ContentValues;
import android.content.Intent;

import com.ncfsistemi.econ.CantieriDettaglioModActivity;
import com.ncfsistemi.econ.EconActivity;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.form.CantiereForm;
import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.Form;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;
import com.ncfsistemi.econ.utils.Utility;

/**
 * Linguetta "Dati cantiere" della scheda del cantiere sul template: le sezioni di CantiereForm in sola visualizzazione
 * (cliente, indirizzo, linea e placca con la gestione elettrica), la durata (STUDIO_DATE_CANTIERI.md), matita e mappa
 * nella testata.
 */
public class CantieriDettaglioFragment extends EconSchedaFragment {

	private int cantiere() {
		return getArguments().getInt(Cantieri.ID_CANTIERE);
	}

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new CantiereForm();
	}

	@Override
	protected ContentValues leggiRecord(DbInterno db) {
		ContentValues where = new ContentValues();
		where.put(Cantieri.ID_CANTIERE, cantiere());
		return db.getRecord(new Cantieri(), where);
	}

	@Override
	protected void dopoCaricamento(Form form, ContentValues val) {
		if (val == null) return;
		form.aggiungiDato(0, "Durata", durata(val));
		if (FunzionalitaApp.puoModificare(getActivity(), FunzionalitaApp.CANTIERI)) {
			form.azione(0, FaIcone.MODIFICA, getString(R.string.modifica), x -> {
				Intent intent = new Intent(getActivity(), CantieriDettaglioModActivity.class);
				intent.putExtra("ID", cantiere());
				((EconActivity) getActivity()).apriFinestraModifica(intent, 1);
			});
		}
		String via = (testo(val, Cantieri.INDIRIZZO) + " " + testo(val, Cantieri.CIVICO)).trim();
		String luogo = (via + ", " + testo(val, Cantieri.CAP) + " " + testo(val, Cantieri.CITTA) + " " + testo(val, Cantieri.PROVINCIA)).trim();
		form.azione(0, FaIcone.MAPPA, "Mappa", x -> Utility.mostraMappa(getActivity(), luogo));
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
			s.append(giorno(chiusura) < giorno ? " · chiuso, ultimo giorno " : " · ultimo giorno di lavoro ").append(data(chiusura));
		}
		return s.toString();
	}

	/** yyyyMMdd da una data del DB, scritta come yyyyMMdd o yyyyMMddHHmmss. */
	private static long giorno(long data) {
		return data >= 10000000000L ? data / 1000000L : data;
	}

	private static String data(long valore) {
		String s = String.valueOf(giorno(valore));
		return s.length() < 8 ? "" : s.substring(6, 8) + "/" + s.substring(4, 6) + "/" + s.substring(0, 4);
	}

	private static String testo(ContentValues v, String campo) {
		String s = v.getAsString(campo);
		return s == null ? "" : s.trim();
	}
}
