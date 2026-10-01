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
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Placche;
import pfa.app.econtab.utils.FaIcone;
import pfa.app.econtab.utils.FunzionalitaApp;
import pfa.app.econtab.utils.SezioneStandard;

/**
 * Linguetta "Gestione elettrica" della scheda del cantiere (GESTIONE_ELETTRICA.md), solo con il modulo ELETTRICO:
 * linea e placca standard (si cambiano dalla modifica del cantiere) e le associazioni fra punti comandati e punti di
 * comando (RelazioniListaFragment). Unita', aree e locali sono nel menu laterale, con la barra "+ Nuova unita'".
 */
public class CantiereElettricoFragment extends EConTabFragment {
	private int cantiere = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.fragment_cantiere_elettrico, container, false);
		cantiere = getArguments().getInt(Cantieri.ID_CANTIERE);

		View impianto = v.findViewById(R.id.sezione_impianto);
		SezioneStandard.imposta(impianto, FaIcone.IMPIANTO, "Linea e placca standard");
		// linea e placca sono campi del cantiere: servono la modifica del cantiere e quella della gestione elettrica
		if (FunzionalitaApp.puoModificare(getActivity(), FunzionalitaApp.CANTIERI)
				&& FunzionalitaApp.puoModificare(getActivity(), FunzionalitaApp.ELETTRICO)) {
			SezioneStandard.azione(impianto, FaIcone.MODIFICA, getString(R.string.modifica), x -> {
				Intent intent = new Intent(getActivity(), CantieriDettaglioModActivity.class);
				intent.putExtra("ID", cantiere);
				((EConTabActivity) getActivity()).apriFinestraModifica(intent, 1);
			});
		}
		SezioneStandard.imposta(v.findViewById(R.id.sezione_associazioni), FaIcone.COLLEGAMENTI, getString(R.string.relazioni_logiche));

		if (savedInstanceState == null) {
			RelazioniListaFragment relazioni = new RelazioniListaFragment();
			Bundle params = new Bundle(getArguments());
			params.putString("DA_CANTIERE", "SI");
			relazioni.setArguments(params);
			getChildFragmentManager().beginTransaction().replace(R.id.contenitore_associazioni, relazioni).commit();
		}
		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		refresh();
	}

	private void refresh() {
		View v = getView();
		if (v == null) return;
		DbInterno db = new DbInterno(getActivity());
		String sql = "SELECT co." + Costruttori.RAGIONE_SOCIALE + " AS costruttore, l." + Linee.NOME_LINEA + ", p." + Placche.NOME_PLACCA
				+ " FROM " + Cantieri.NOME_TABELLA + " c"
				+ " LEFT JOIN " + Linee.NOME_TABELLA + " l ON l." + Linee.ID_LINEA + " = c." + Cantieri.ID_LINEA
				+ " LEFT JOIN " + Costruttori.NOME_TABELLA + " co ON co." + Costruttori.ID_COSTRUTTORE + " = l." + Linee.ID_COSTRUTTORE
				+ " LEFT JOIN " + Placche.NOME_TABELLA + " p ON p." + Placche.ID_PLACCA + " = c." + Cantieri.ID_PLACCA
				+ " WHERE c." + Cantieri.ID_CANTIERE + " = " + cantiere;
		ArrayList<Object> records = db.eseguiSelect(sql, null);
		db.close();
		String linea = "", placca = "";
		if (!records.isEmpty()) {
			ContentValues val = (ContentValues) records.get(0);
			String costruttore = testo(val, "costruttore");
			linea = testo(val, Linee.NOME_LINEA);
			if (!costruttore.isEmpty() && !linea.isEmpty()) linea = costruttore + " - " + linea;
			placca = testo(val, Placche.NOME_PLACCA);
		}
		setText(R.id.editText_linea, linea.isEmpty() ? "—" : linea, v);
		setText(R.id.editText_placca, placca.isEmpty() ? "—" : placca, v);
	}

	private static String testo(ContentValues v, String campo) {
		String s = v.getAsString(campo);
		return s == null ? "" : s.trim();
	}
}
