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
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import pfa.app.econtab.adapters.ClientiAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;

/**
 * Prototipo dello standard di ricerca comune a Clienti/Cantieri/Listini/Preventivi/
 * Ordini/Rapportini: form filtri visibile all'apertura con gli "ultimi gestiti" sotto,
 * pulsante Cerca che riduce la form e mostra i risultati paginati.
 */
public class ClientiActivity extends EConTabActivity implements OnItemClickListener {

	private static final int RISULTATI_PER_PAGINA = 20;
	private static final int ULTIMI_GESTITI = 10;

	private ListView lista = null;
	private ClientiAdapter adapter = null;
	private ArrayList<Object> dati = null;

	private View cardFiltri = null;
	private View barraPaginazione = null;
	private TextView textViewPaginazione = null;
	private EditText filtro = null;

	/** false = si mostrano gli "ultimi gestiti" (nessuna ricerca ancora eseguita) */
	private boolean ricercaAttiva = false;
	private int paginaCorrente = 0; // 0-based
	private int totalePagine = 1;
	private int totaleRisultati = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_clienti);

		lista = findViewById(R.id.lista);
		lista.setOnItemClickListener(this);
		lista.setEmptyView(findViewById(R.id.textViewNessunDato));

		cardFiltri = findViewById(R.id.cardFiltri);
		barraPaginazione = findViewById(R.id.barraPaginazione);
		textViewPaginazione = findViewById(R.id.textViewPaginazione);

		filtro = findViewById(R.id.editText_filtra);
		filtro.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
				eseguiRicerca();
				return true;
			}
			return false;
		});

		((ImageButton) findViewById(R.id.buttonCerca)).setOnClickListener(v -> eseguiRicerca());
	}

	/** Icona filtro nella barra in alto: riapre la form se era stata nascosta dopo una ricerca. */
	public void toggleFiltri(View v) {
		cardFiltri.setVisibility(cardFiltri.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
	}

	/** Pulsante Cerca/tasto invio: nasconde la form e mostra i risultati (paginati) del filtro corrente. */
	private void eseguiRicerca() {
		nascondiTastiera(filtro);
		ricercaAttiva = true;
		paginaCorrente = 0;
		cardFiltri.setVisibility(View.GONE);
		caricaPagina();
	}

	private void nascondiTastiera(View v) {
		android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
				getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
	}

	/** Ultimi 10 clienti gestiti (per data modifica/inserimento), mostrati prima di ogni ricerca esplicita. */
	private void mostraUltimiGestiti() {
		ricercaAttiva = false;
		cardFiltri.setVisibility(View.VISIBLE);
		barraPaginazione.setVisibility(View.GONE);

		DbInterno db = new DbInterno(this);
		String sql = "Select * from " + Anagrafica.NOME_TABELLA
				+ " order by " + Anagrafica.DATA_MOD + " is null desc, " + Anagrafica.DATA_MOD
				+ " desc, " + Anagrafica.DATA_INS + " desc limit " + ULTIMI_GESTITI;
		ArrayList<Object> elems = db.eseguiSelect(sql, null);
		db.close();

		popolaLista(elems);
	}

	/** Query paginata (RISULTATI_PER_PAGINA) con il filtro testuale corrente. */
	private void caricaPagina() {
		String testo = filtro.getText().toString().trim();
		String like = testo.isEmpty() ? "%" : "%" + testo + "%";
		String[] argsFiltro = new String[]{like, like, like, like};

		String whereSql = Anagrafica.RAGIONE_SOCIALE + " like ? or " + Anagrafica.INDIRIZZO + " like ? or "
				+ Anagrafica.CITTA + " like ? or " + Anagrafica.CODICE_ESTERNO + " like ?";

		DbInterno db = new DbInterno(this);

		ArrayList<Object> conteggio = db.eseguiSelect(
				"Select count(*) as n from " + Anagrafica.NOME_TABELLA + " where " + whereSql, argsFiltro);
		totaleRisultati = conteggio.isEmpty() ? 0 : ((ContentValues) conteggio.get(0)).getAsInteger("n");
		totalePagine = Math.max(1, (int) Math.ceil(totaleRisultati / (double) RISULTATI_PER_PAGINA));
		if (paginaCorrente >= totalePagine) {
			paginaCorrente = totalePagine - 1;
		}

		String sql = "Select * from " + Anagrafica.NOME_TABELLA + " where " + whereSql
				+ " order by " + Anagrafica.RAGIONE_SOCIALE + " asc limit " + RISULTATI_PER_PAGINA
				+ " offset " + (paginaCorrente * RISULTATI_PER_PAGINA);
		ArrayList<Object> elems = db.eseguiSelect(sql, argsFiltro);
		db.close();

		popolaLista(elems);
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
		textViewPaginazione.setText("Pagina " + (paginaCorrente + 1) + " di " + totalePagine
				+ "  ·  " + RISULTATI_PER_PAGINA + " per pagina  ·  " + totaleRisultati + " risultati trovati");
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
		if (ricercaAttiva) {
			caricaPagina();
		} else {
			mostraUltimiGestiti();
		}
	}

	@Override
	protected void aggiornaDopoCancellazione() {
		if (ricercaAttiva) {
			caricaPagina();
		} else {
			mostraUltimiGestiti();
		}
	}
}
