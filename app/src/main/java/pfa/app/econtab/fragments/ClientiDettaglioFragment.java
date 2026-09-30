package pfa.app.econtab.fragments;

import android.content.ContentValues;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.utils.FaIcone;
import pfa.app.econtab.utils.SezioneStandard;
import pfa.app.econtab.utils.Utility;

/**
 * Linguetta "Dati principali" della scheda cliente (GESTIONE_CLIENTI.md §6.8): dati del cliente e, in fondo, i dati di
 * fatturazione (P.IVA, CF, IVA, PEC, SDI). Indirizzi, referenti e cantieri hanno la loro linguetta
 * (ClienteElencoFragment). L'indirizzo scritto sul cliente (copia della sede) qui non si mostra: c'e' la linguetta
 * Indirizzi.
 */
public class ClientiDettaglioFragment extends EConTabFragment {
	private int cliente = 0;
	private View radice;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View v = super.onCreateView(inflater, container, savedInstanceState);
		v = inflater.inflate(R.layout.fragment_clienti_dettaglio, container, false);
		radice = v;
		// P.IVA e codice fiscale: nella sezione Fatturazione in fondo
		v.findViewById(R.id.riga_4).setVisibility(View.GONE);
		v.findViewById(R.id.riga_5).setVisibility(View.GONE);
		// indirizzo (e mappa): nella tabella degli indirizzi
		v.findViewById(R.id.riga_2).setVisibility(View.GONE);
		v.findViewById(R.id.riga_3).setVisibility(View.GONE);
		cliente = getArguments().getInt(Anagrafica.ID_ANAGRAFICA);
		// matita dello standard grafico (niente pulsante tondo), solo per chi puo' modificare i clienti
		TextView matita = v.findViewById(R.id.button_edit);
		FaIcone.applica(matita, FaIcone.MODIFICA, "Modifica");
		matita.setVisibility(pfa.app.econtab.utils.FunzionalitaApp.puoModificare(getActivity(), pfa.app.econtab.utils.FunzionalitaApp.CLIENTI)
				? View.VISIBLE : View.GONE);
		DbInterno db = new DbInterno(getActivity());
		ContentValues where = new ContentValues();
		where.put(Anagrafica.ID_ANAGRAFICA, cliente);
		ContentValues val = db.getRecord(new Anagrafica(), where);

		db.close();

		v.findViewById(R.id.buttonMappa).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String indirizzo = getTesto(R.id.editText_indirizzo) + "," + getTesto(R.id.editText_citta);

				Utility.mostraMappa(getActivity(), indirizzo);
			}
		});

		if (val != null) {
			TextView headerTitolo = getActivity().findViewById(R.id.headerTitolo);
			if (headerTitolo != null) {
				headerTitolo.setText(val.getAsString(Anagrafica.RAGIONE_SOCIALE));
			}

			setText(R.id.editText_ragionesociale, val.getAsString(Anagrafica.RAGIONE_SOCIALE), v);
			setText(R.id.editText_codice, val.getAsString(Anagrafica.CODICE), v);
			setText(R.id.editText_indirizzo, val.getAsString(Anagrafica.INDIRIZZO), v);
			setText(R.id.editText_cap, val.getAsString(Anagrafica.CAP), v);
			setText(R.id.editText_citta, val.getAsString(Anagrafica.CITTA), v);
			setText(R.id.editText_provincia, val.getAsString(Anagrafica.PROVINCIA), v);
			setText(R.id.editText_cf, val.getAsString(Anagrafica.CODICE_FISCALE), v);
			setText(R.id.editText_partitaiva, val.getAsString(Anagrafica.PARTITA_IVA), v);
			setText(R.id.editText_cellulare1, val.getAsString(Anagrafica.CELLULARE), v);
			// il secondo cellulare non si usa piu' (ci sono i referenti)
			v.findViewById(R.id.editText_cellulare2).setVisibility(View.GONE);
			setText(R.id.editText_telefono, val.getAsString(Anagrafica.TELEFONO), v);
			setText(R.id.spinner_iva, val.getAsString(Anagrafica.CODICE_IVA), v);
			setText(R.id.editText_mail, val.getAsString(Anagrafica.MAIL), v);
			setText(R.id.editText_note, val.getAsString(Anagrafica.NOTE), v);
		}

		return v;

	}

	@Override
	public void onResume() {
		super.onResume();
		mostraFatturazione(); // di nuovo dopo una modifica
	}

	/** Dati di fatturazione (etichetta, colonna); il codice IVA e' gia' nei dati sopra. */
	private static final String[] FATTURAZIONE = {
			"Partita IVA", Anagrafica.PARTITA_IVA, "Codice fiscale", Anagrafica.CODICE_FISCALE,
			"PEC", Anagrafica.PEC, "Codice destinatario SDI", Anagrafica.CODICE_SDI,
	};

	/** Sezione "Fatturazione" in fondo ai dati principali, con lo stesso aspetto dei campi sopra. */
	private void mostraFatturazione() {
		if (radice == null) return;
		LinearLayout corpo = radice.findViewById(R.id.fatturazione);
		corpo.removeAllViews();
		corpo.setDividerDrawable(getResources().getDrawable(android.R.drawable.divider_horizontal_bright, null));
		corpo.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
		DbInterno db = new DbInterno(getActivity());
		ContentValues where = new ContentValues();
		where.put(Anagrafica.ID_ANAGRAFICA, cliente);
		ContentValues v = db.getRecord(new Anagrafica(), where);
		db.close();

		View sezione = LayoutInflater.from(getActivity()).inflate(R.layout.sezione_standard, corpo, false);
		SezioneStandard.imposta(sezione, FaIcone.FATTURAZIONE, "Fatturazione");
		corpo.addView(sezione);
		int margine = getResources().getDimensionPixelSize(R.dimen.margine);
		for (int i = 0; i + 1 < FATTURAZIONE.length; i += 2) {
			LinearLayout riga = new LinearLayout(getActivity());
			riga.setOrientation(LinearLayout.VERTICAL);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			lp.setMargins(0, margine, 0, margine);
			TextView etichetta = new TextView(getActivity(), null, 0, R.style.TestoIntestazioneNoPadding);
			etichetta.setText(FATTURAZIONE[i]);
			TextView valore = new TextView(getActivity());
			String s = v != null ? v.getAsString(FATTURAZIONE[i + 1]) : null;
			valore.setText(s == null ? "" : s.trim());
			valore.setTextIsSelectable(true);
			riga.addView(etichetta);
			riga.addView(valore);
			corpo.addView(riga, lp);
		}
	}
}
