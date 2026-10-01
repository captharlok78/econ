package pfa.app.econtab.fragments;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import pfa.app.econtab.CantieriDettaglioModActivity;
import pfa.app.econtab.EConTabActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.utils.FaIcone;
import pfa.app.econtab.utils.FunzionalitaApp;
import pfa.app.econtab.utils.SezioneStandard;
import pfa.app.econtab.utils.Utility;

/**
 * Linguetta "Dati cantiere" della scheda del cantiere, standard grafico (STANDARD_GRAFICO.md): sezione con modifica e
 * mappa a icona, dati in sola lettura. Linea e placca standard sono nella linguetta "Gestione elettrica"
 * (CantiereElettricoFragment, GESTIONE_ELETTRICA.md).
 */
public class CantieriDettaglioFragment extends EConTabFragment {
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
				((EConTabActivity) getActivity()).apriFinestraModifica(intent, 1);
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
	}

	private static String valore(String s) {
		return s.isEmpty() ? "—" : s;
	}

	private static String testo(ContentValues v, String campo) {
		String s = v.getAsString(campo);
		return s == null ? "" : s.trim();
	}
}
