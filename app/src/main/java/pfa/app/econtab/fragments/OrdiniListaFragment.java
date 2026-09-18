package pfa.app.econtab.fragments;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;

import java.security.Key;
import java.util.ArrayList;
import java.util.Calendar;

import pfa.app.econtab.CantiereSplitActivity;
import pfa.app.econtab.PreventiviDettaglioModActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.adapters.PreventiviAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.utils.EConTabAutoCompleteContentValue;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabSpinner;

public class OrdiniListaFragment extends EConTabFragment implements OnItemClickListener, OnClickListener, TextWatcher {
	private int cantiere = 0;
	private ListView lista = null;

	private AutoCompleteTextView filtro = null;
	private int codiceClienteFiltro = 0;
	private EConTabSpinner spinnerFiltroStato = null;
	private EConTabSpinner spinnerFiltroAnno = null;

	private ArrayAdapter<Object> adapter_filtro = null;
	private PreventiviAdapter adapter = null;

	private ArrayList<Object> dati = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View v = super.onCreateView(inflater, container, savedInstanceState);
		v = inflater.inflate(R.layout.fragment_preventivi_lista, container, false);
		if (getArguments() != null) {
			cantiere = getArguments().getInt(Cantieri.ID_CANTIERE);
		}

		lista = (ListView) v.findViewById(R.id.lista);
		lista.setOnItemClickListener(this);
		registerForContextMenu(lista);

		v.findViewById(R.id.buttonnuovo).setOnClickListener(this);
		v.findViewById(R.id.buttonCerca).setOnClickListener(this);

		filtro = (AutoCompleteTextView) v.findViewById(R.id.autoComplete_filtroCliente);
		filtro.setOnItemClickListener(this);
		filtro.addTextChangedListener(this);
		filtro.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					eseguiRicercaLibera();
					return true;
				}
				return false;
			}
		});

		spinnerFiltroAnno = (EConTabSpinner) v.findViewById(R.id.econtabSpinner_filtroAnno);

		spinnerFiltroAnno.addTextChangeListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				ricerca();
			}
		});

		spinnerFiltroStato = (EConTabSpinner)v.findViewById(R.id.econtabSpinner_filtroStato);
		spinnerFiltroStato.addTextChangeListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				ricerca();
			}
		});

		return v;
	}

	public void ricerca() {
		// TODO Auto-generated method stub
		String anno = spinnerFiltroAnno.getValue();
		if (anno.equals("")) {
			anno = "2014";

		}
		lista.invalidate();
		if (dati == null) {
			dati = new ArrayList<Object>();
		}
		dati.clear();

		DbInterno db = new DbInterno(getActivity());
		Anagrafica ana = new Anagrafica();
		Cantieri cant = new Cantieri();
		Preventivi prev = new Preventivi();

		String filtroCliente = "";
		String[] parametriRicercaLibera = null;

		if (codiceClienteFiltro != 0) {
			// Cliente scelto puntualmente dai suggerimenti dell'autocomplete
			filtroCliente = " and " + ana.getNomeCampoTabella(Anagrafica.ID_ANAGRAFICA) + "=" + codiceClienteFiltro;
		} else if (filtro.getText().length() > 0) {
			// Testo libero (nessun suggerimento selezionato): ricerca "contiene" sulla ragione sociale
			filtroCliente = " and " + ana.getNomeCampoTabella(Anagrafica.RAGIONE_SOCIALE) + " like ?";
			parametriRicercaLibera = new String[]{"%" + filtro.getText().toString() + "%"};
		}

		String stato = spinnerFiltroStato.getValue();
		String filtroStato = "";
		if (!stato.equals("")){
			filtroStato = " and " + Preventivi.STATO +"='" + stato +"' " ;
		}

		Join join1 = new Join(Preventivi.NOME_TABELLA, Cantieri.NOME_TABELLA);
		join1.addCampiDiJoin(Preventivi.ID_CANTIERE, Cantieri.ID_CANTIERE);

		Join join2 = new Join(Cantieri.NOME_TABELLA, Anagrafica.NOME_TABELLA);
		join2.addCampiDiJoin(Cantieri.ID_ANAGRAFICA, Anagrafica.ID_ANAGRAFICA);

		String SQL = "Select " + prev.getNomeCampoTabella("*") + "," + ana.getNomeCampoTabella(Anagrafica.RAGIONE_SOCIALE) + ","
				+ cant.getNomeCampoTabella(Cantieri.NOME) + " from " + " " + prev.getNomeTabella() + join1.getSQLJoin()
				+ join2.getSQLJoin() + " where " + Preventivi.ANNO + "=" + anno + filtroCliente + filtroStato +  " and " + Preventivi.TIPO + "='"
				+ Preventivi.TIPO_ORDINE + "' and " + cant.getNomeCampoTabella(Cantieri.ID_DITTA) + "=" + Sessione.getDittaSelezionata()
				+ " order by " + Preventivi.NUMERO + " desc";

		ArrayList<Object> elems = db.eseguiSelect(SQL, parametriRicercaLibera);

		dati.addAll(elems);

		db.close();

		if (adapter == null) {
			adapter = new PreventiviAdapter(getActivity(), dati, R.layout.list_item_preventivo);
			lista.setAdapter(adapter);
			lista.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
			// registerForContextMenu(lista);
		} else {
			adapter.notifyDataSetChanged();

		}

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		ContentValues item = (ContentValues) lista.getItemAtPosition(info.position);
		menu.setHeaderTitle(item.getAsString(Preventivi.NUMERO) + " - " + Utility.numberToData(item.getAsLong(Preventivi.DATA)));

		menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
		menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (getUserVisibleHint()) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			ContentValues preventivo = (ContentValues) lista.getItemAtPosition(info.position);

			if (item.getItemId() == 1) {
				Intent intent = new Intent(getActivity(), PreventiviDettaglioModActivity.class);
				intent.putExtra("ID", preventivo.getAsInteger(Preventivi.ID_PREVENTIVO));
				getEConTabActivity().apriFinestraModifica(intent, 2);
			}
			if (item.getItemId() == 2) {
				getEConTabActivity().confermaCancellazione(new Preventivi(), preventivo, true);
			}
			return true;
		}

		return false;
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		DbInterno db = new DbInterno(getActivity());
		ArrayList<Object> valori = db.eseguiSelect("Select distinct " + Preventivi.ANNO + " from " + Preventivi.NOME_TABELLA + " order by "
				+ Preventivi.ANNO + " desc", null);

		db.close();

		ArrayList<Object> anni = new ArrayList<Object>();
		if (valori.size() > 0) {
			for (int i = 0; i < valori.size(); i++) {
				ContentValues valCurr = (ContentValues) valori.get(i);
				ContentValues annoCorrente = new ContentValues();
				annoCorrente.put(EConTabSpinner.VALORE, valCurr.getAsString(Preventivi.ANNO));
				annoCorrente.put(EConTabSpinner.DESCRIZIONE, valCurr.getAsString(Preventivi.ANNO));
				anni.add(annoCorrente);
				if (spinnerFiltroAnno.getValue().equals("")) {
					spinnerFiltroAnno.setValue(valCurr.getAsString(Preventivi.ANNO));
				}

			}
		} else {
			ContentValues annoCorrente = new ContentValues();
			annoCorrente.put(EConTabSpinner.VALORE, "" + Calendar.getInstance().get(Calendar.YEAR));
			annoCorrente.put(EConTabSpinner.DESCRIZIONE, "" + Calendar.getInstance().get(Calendar.YEAR));
			anni.add(annoCorrente);
			spinnerFiltroAnno.setValue("" + Calendar.getInstance().get(Calendar.YEAR));
		}

		spinnerFiltroAnno.setValoriSpinnerLibero(anni);

		ArrayList<Object> stati = new Preventivi().getStatiOrdine(getActivity());
		ContentValues valTutti = new ContentValues();
		valTutti.put("VAL", "");
		valTutti.put("DESC", "");

		stati.add(0, valTutti);
		spinnerFiltroStato.setValue(Preventivi.STATO_APERTO);
		spinnerFiltroStato.setValoriSpinnerLibero(stati);


		adapter_filtro = Utility.getArrayAdapterTabella(getActivity(), "Select * from " + Anagrafica.NOME_TABELLA,
				Anagrafica.RAGIONE_SOCIALE);
		filtro.setAdapter(adapter_filtro);
		ricerca();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		// TODO Auto-generated method stub
		if (arg0 == lista) {
			ContentValues ordine = (ContentValues) lista.getItemAtPosition(position);
			// if (ordine.getAsString(Preventivi.STATO).equals(Preventivi.STATO_APERTO)) {
			Intent intent = new Intent(getActivity(), CantiereSplitActivity.class);
			intent.putExtra(Cantieri.ID_CANTIERE, ordine.getAsInteger(Preventivi.ID_CANTIERE));
			intent.putExtra(Preventivi.ID_PREVENTIVO, ordine.getAsInteger(Preventivi.ID_PREVENTIVO));
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
			// }
		}
		// Evento sulla selezione del cliente
		else {
			if (filtro.hasFocus()) {
				ContentValues val = ((EConTabAutoCompleteContentValue) arg0.getItemAtPosition(position)).getContentValue();
				codiceClienteFiltro = val.getAsInteger(Anagrafica.ID_ANAGRAFICA);
				ricerca();
			}
		}

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.buttonnuovo) {
			Intent intent = new Intent(getActivity(), PreventiviDettaglioModActivity.class);
			intent.putExtra(Preventivi.TIPO, Preventivi.TIPO_ORDINE);
			getEConTabActivity().apriFinestraInserimento(intent, 1, new Preventivi());
		}
		if (v.getId() == R.id.buttonCerca) {
			eseguiRicercaLibera();
		}
	}

	/**
	 * Avvia la ricerca dal pulsante/tasto invio: nasconde la tastiera e lancia la query
	 * con il testo digitato, anche se il cliente non è stato scelto dai suggerimenti.
	 */
	private void eseguiRicercaLibera() {
		android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
				getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(filtro.getWindowToken(), 0);
		}
		ricerca();
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		// TODO Auto-generated method stub
		if (filtro.getText().toString().length() == 0) {
			ricerca();
		}
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		codiceClienteFiltro = 0;

	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

}
