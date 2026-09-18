package pfa.app.econtab;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import pfa.app.econtab.adapters.ClientiAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;

/**
 * Prototipo dello standard di ricerca comune a Clienti/Cantieri/Listini/Preventivi/
 * Ordini/Rapportini: all'apertura si vede SOLO la form filtri (nessun caricamento dati).
 * Tre pulsanti nella form (Cerca / Reset filtri / Ultimi 20): qualunque venga premuto
 * chiude la form e avvia la ricerca vera e propria — il caricamento dati avviene solo lì.
 */
public class ClientiActivity extends EConTabActivity implements OnItemClickListener {

	private static final int RISULTATI_PER_PAGINA_DEFAULT = 20;

	private ListView lista = null;
	private ClientiAdapter adapter = null;
	private ArrayList<Object> dati = null;

	private View cardFiltri = null;
	private View barraPaginazione = null;
	private TextView textViewPaginaInfo = null;
	private TextView textViewPaginaInfo2 = null;
	private TextView textViewNessunDato = null;
	private EditText editTextRisultatiPerPagina = null;
	private EditText filtro = null;

	/** false = nessuna ricerca ancora eseguita (si vede solo la form) */
	private boolean ricercaAttiva = false;
	private boolean ordinaPerRecenti = false;
	private int risultatiPerPagina = RISULTATI_PER_PAGINA_DEFAULT;
	private int paginaCorrente = 0; // 0-based
	private int totalePagine = 1;
	private int totaleRisultati = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_clienti);

		lista = findViewById(R.id.lista);
		lista.setOnItemClickListener(this);
		textViewNessunDato = findViewById(R.id.textViewNessunDato);

		cardFiltri = findViewById(R.id.cardFiltri);
		barraPaginazione = findViewById(R.id.barraPaginazione);
		textViewPaginaInfo = findViewById(R.id.textViewPaginaInfo);
		textViewPaginaInfo2 = findViewById(R.id.textViewPaginaInfo2);

		editTextRisultatiPerPagina = findViewById(R.id.editTextRisultatiPerPagina);
		editTextRisultatiPerPagina.setText(String.valueOf(risultatiPerPagina));
		editTextRisultatiPerPagina.setOnFocusChangeListener((v, hasFocus) -> {
			if (!hasFocus) {
				applicaRisultatiPerPagina();
			}
		});
		editTextRisultatiPerPagina.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				nascondiTastiera(editTextRisultatiPerPagina);
				applicaRisultatiPerPagina();
				return true;
			}
			return false;
		});

		filtro = findViewById(R.id.editText_filtra);
		filtro.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
				eseguiRicerca(false);
				return true;
			}
			return false;
		});

		findViewById(R.id.buttonCerca).setOnClickListener(v -> eseguiRicerca(false));
		findViewById(R.id.buttonResetFiltri).setOnClickListener(v -> {
			filtro.setText("");
			eseguiRicerca(false);
		});
		findViewById(R.id.buttonUltimiGestiti).setOnClickListener(v -> {
			filtro.setText("");
			eseguiRicerca(true);
		});
	}

	/** Icona filtro nella barra in alto: riapre la form se era stata nascosta dopo una ricerca. */
	public void toggleFiltri(View v) {
		cardFiltri.setVisibility(cardFiltri.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
	}

	/**
	 * Mostra solo la form filtri, senza alcun caricamento dati: stato di apertura del
	 * modulo e ripristinato dopo il logout/tra una visita e l'altra della schermata.
	 */
	private void mostraSoloForm() {
		ricercaAttiva = false;
		cardFiltri.setVisibility(View.VISIBLE);
		barraPaginazione.setVisibility(View.GONE);
		textViewNessunDato.setVisibility(View.GONE);
		popolaLista(new ArrayList<>());
	}

	/**
	 * Pulsante Cerca/Reset filtri/Ultimi 20/tasto invio: nasconde la form e mostra i
	 * risultati paginati. "ordinaPerRecenti" true solo per "Ultimi 20" (ordina per data
	 * modifica/inserimento invece che alfabeticamente).
	 */
	private void eseguiRicerca(boolean ordinaPerRecenti) {
		nascondiTastiera(filtro);
		this.ordinaPerRecenti = ordinaPerRecenti;
		ricercaAttiva = true;
		paginaCorrente = 0;
		cardFiltri.setVisibility(View.GONE);
		caricaPagina();
	}

	/**
	 * Cambio del numero di risultati per pagina: ricalcola pagine e ricarica mantenendo
	 * il filtro corrente. Valore non valido/vuoto -> ripristina quello precedente.
	 */
	private void applicaRisultatiPerPagina() {
		String testo = editTextRisultatiPerPagina.getText().toString().trim();
		int nuovoValore;
		try {
			nuovoValore = Integer.parseInt(testo);
		} catch (NumberFormatException e) {
			nuovoValore = 0;
		}
		if (nuovoValore <= 0) {
			editTextRisultatiPerPagina.setText(String.valueOf(risultatiPerPagina));
			return;
		}
		if (nuovoValore == risultatiPerPagina) {
			return;
		}
		risultatiPerPagina = nuovoValore;
		paginaCorrente = 0;
		if (ricercaAttiva) {
			caricaPagina();
		}
	}

	private void nascondiTastiera(View v) {
		android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
				getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
	}

	/** Query paginata (RISULTATI_PER_PAGINA) con il filtro testuale e l'ordinamento correnti. */
	private void caricaPagina() {
		String testo = filtro.getText().toString().trim();
		String like = testo.isEmpty() ? "%" : "%" + testo + "%";
		String[] argsFiltro = new String[]{like, like, like, like};

		String whereSql = Anagrafica.RAGIONE_SOCIALE + " like ? or " + Anagrafica.INDIRIZZO + " like ? or "
				+ Anagrafica.CITTA + " like ? or " + Anagrafica.CODICE_ESTERNO + " like ?";
		String ordineSql = ordinaPerRecenti
				? Anagrafica.DATA_MOD + " is null asc, " + Anagrafica.DATA_MOD + " desc, " + Anagrafica.DATA_INS + " desc"
				: Anagrafica.RAGIONE_SOCIALE + " asc";

		DbInterno db = new DbInterno(this);

		ArrayList<Object> conteggio = db.eseguiSelect(
				"Select count(*) as n from " + Anagrafica.NOME_TABELLA + " where " + whereSql, argsFiltro);
		totaleRisultati = conteggio.isEmpty() ? 0 : ((ContentValues) conteggio.get(0)).getAsInteger("n");
		totalePagine = Math.max(1, (int) Math.ceil(totaleRisultati / (double) risultatiPerPagina));
		if (paginaCorrente >= totalePagine) {
			paginaCorrente = totalePagine - 1;
		}

		String sql = "Select * from " + Anagrafica.NOME_TABELLA + " where " + whereSql
				+ " order by " + ordineSql + " limit " + risultatiPerPagina
				+ " offset " + (paginaCorrente * risultatiPerPagina);
		ArrayList<Object> elems = db.eseguiSelect(sql, argsFiltro);
		db.close();

		popolaLista(elems);
		textViewNessunDato.setVisibility(elems.isEmpty() ? View.VISIBLE : View.GONE);
		aggiornaBarraPaginazione();
	}

	private void popolaLista(ArrayList<Object> elems) {
		if (dati == null) {
			dati = new ArrayList<>();
		}
		dati.clear();
		dati.addAll(elems);

		if (adapter == null) {
			adapter = new ClientiAdapter(this, dati, R.layout.list_item_cliente);
			lista.setAdapter(adapter);
			lista.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
			registerForContextMenu(lista);
		} else {
			adapter.notifyDataSetChanged();
		}
	}

	private void aggiornaBarraPaginazione() {
		barraPaginazione.setVisibility(View.VISIBLE);
		textViewPaginaInfo.setText("Pagina " + (paginaCorrente + 1) + " di " + totalePagine + "  ·  ");
		textViewPaginaInfo2.setText("  per pagina  ·  " + totaleRisultati + " risultati trovati");
		if (!editTextRisultatiPerPagina.isFocused()) {
			editTextRisultatiPerPagina.setText(String.valueOf(risultatiPerPagina));
		}
		findViewById(R.id.buttonPaginaPrec).setEnabled(paginaCorrente > 0);
		findViewById(R.id.buttonPaginaPrec).setAlpha(paginaCorrente > 0 ? 1f : 0.3f);
		findViewById(R.id.buttonPaginaSucc).setEnabled(paginaCorrente < totalePagine - 1);
		findViewById(R.id.buttonPaginaSucc).setAlpha(paginaCorrente < totalePagine - 1 ? 1f : 0.3f);
	}

	public void paginaPrecedente(View v) {
		if (paginaCorrente > 0) {
			paginaCorrente--;
			caricaPagina();
		}
	}

	public void paginaSuccessiva(View v) {
		if (paginaCorrente < totalePagine - 1) {
			paginaCorrente++;
			caricaPagina();
		}
	}

	public void nuovoCliente(View v) {
		Intent intent = new Intent(this, ClientiDettaglioModActivity.class);
		apriFinestraInserimento(intent, 1, new Anagrafica());
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		ContentValues item = (ContentValues) lista.getItemAtPosition(info.position);
		menu.setHeaderTitle(item.getAsString(Anagrafica.RAGIONE_SOCIALE));
		menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
		menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		ContentValues cliente = (ContentValues) lista.getItemAtPosition(info.position);
		if (item.getItemId() == 1) {
			Intent intent = new Intent(this, ClientiDettaglioModActivity.class);
			intent.putExtra("ID", cliente.getAsInteger(Anagrafica.ID_ANAGRAFICA));
			apriFinestraModifica(intent, 1);
		}
		if (item.getItemId() == 2) {
			confermaCancellazione(new Anagrafica(), cliente, true);
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		Intent intent = new Intent(this, ClientiDettaglioActivity.class);
		ContentValues item = (ContentValues) lista.getItemAtPosition(position);
		intent.putExtra(Anagrafica.ID_ANAGRAFICA, item.getAsInteger(Anagrafica.ID_ANAGRAFICA));
		startActivity(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mostraSoloForm();
	}

	@Override
	protected void aggiornaDopoCancellazione() {
		if (ricercaAttiva) {
			caricaPagina();
		}
	}
}
