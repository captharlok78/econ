package pfa.app.econtab.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.utils.FaIcone;
import pfa.app.econtab.utils.FunzionalitaApp;
import pfa.app.econtab.utils.SezioneStandard;

/**
 * Linguette "Indirizzi/Cantieri" e "Referenti" della scheda cliente (GESTIONE_CLIENTI.md §6.8): testate dello standard
 * grafico con l'azione + a destra e il contenuto sotto, ricaricato a ogni ritorno sulla scheda.
 * Indirizzi/Cantieri: due tabelle (IndirizziCantieriCliente); + sugli indirizzi = indirizzo nuovo, + sui cantieri =
 * cantiere libero (indirizzo scritto a mano). Argomenti: Anagrafica.ID_ANAGRAFICA e ARG_TIPO.
 */
public class ClienteElencoFragment extends EConTabFragment {

	public static final String ARG_TIPO = "tipo";
	public static final int INDIRIZZI_CANTIERI = 1, REFERENTI = 2;

	private IndirizziCantieriCliente indirizziCantieri;
	private ReferentiCliente referenti;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		int cliente = getArguments().getInt(Anagrafica.ID_ANAGRAFICA);
		int tipo = getArguments().getInt(ARG_TIPO);

		ScrollView scroll = new ScrollView(getActivity());
		LinearLayout corpo = new LinearLayout(getActivity());
		corpo.setOrientation(LinearLayout.VERTICAL);
		corpo.setPadding(24, 16, 24, 16);
		scroll.addView(corpo);

		boolean modifica = FunzionalitaApp.puoModificare(getActivity(), FunzionalitaApp.CLIENTI);
		if (tipo == INDIRIZZI_CANTIERI) {
			View sIndirizzi = sezione(inflater, corpo, FaIcone.INDIRIZZO, "Indirizzi", true);
			LinearLayout tabIndirizzi = lista(corpo);
			View sCantieri = sezione(inflater, corpo, FaIcone.CANTIERE, "Cantieri", false);
			LinearLayout tabCantieri = lista(corpo);
			indirizziCantieri = new IndirizziCantieriCliente(getActivity(), tabIndirizzi, tabCantieri, cliente);
			if (modifica) SezioneStandard.azione(sIndirizzi, FaIcone.PIU, "Nuovo indirizzo", b -> indirizziCantieri.nuovoIndirizzo());
			if (CantieriCliente.puoCreare(getActivity(), cliente)) {
				SezioneStandard.azione(sCantieri, FaIcone.PIU, "Nuovo cantiere (indirizzo a mano)", b -> CantieriCliente.nuovoLibero(getActivity(), cliente));
			}
		} else {
			View sReferenti = sezione(inflater, corpo, FaIcone.REFERENTI, "Referenti", true);
			referenti = new ReferentiCliente(getActivity(), lista(corpo), cliente);
			if (modifica) SezioneStandard.azione(sReferenti, FaIcone.PIU, "Nuovo referente", b -> referenti.nuovo());
		}
		return scroll;
	}

	private View sezione(LayoutInflater inflater, LinearLayout corpo, String glifo, String titolo, boolean prima) {
		View sezione = inflater.inflate(R.layout.sezione_standard, corpo, false);
		if (prima) ((ViewGroup.MarginLayoutParams) sezione.getLayoutParams()).topMargin = 0;
		SezioneStandard.imposta(sezione, glifo, titolo);
		corpo.addView(sezione);
		return sezione;
	}

	private LinearLayout lista(LinearLayout corpo) {
		LinearLayout lista = new LinearLayout(getActivity());
		lista.setOrientation(LinearLayout.VERTICAL);
		corpo.addView(lista);
		return lista;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (indirizziCantieri != null) indirizziCantieri.carica();
		if (referenti != null) referenti.carica();
	}
}
