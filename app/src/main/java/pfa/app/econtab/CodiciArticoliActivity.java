package pfa.app.econtab;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;

import pfa.app.econtab.adapters.CodiciArticoliAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.AssCodiciLinee;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Listini;
import pfa.app.econtab.utils.AsyncTaskExecutorService;
import pfa.app.econtab.views.EConTabSpinner;

public class CodiciArticoliActivity extends EConTabActivity implements OnItemClickListener, TextWatcher {

	private ListView lista = null;
	private CodiciArticoliAdapter adapter = null;

	private ArrayList<Object> dati = null;
	private EConTabSpinner spinnerFornitore = null;
	private EConTabSpinner spinnerLinea = null;
private ProgressBar progressBarAttesa = null;
    boolean inRicerca = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: CodiciArticoliActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_codici_articoli);

        progressBarAttesa = (ProgressBar)findViewById(R.id.progressBarAttesa);
        progressBarAttesa.setVisibility(View.GONE);

		lista = (ListView) findViewById(R.id.lista);
		lista.setOnItemClickListener(this);
        lista.setFastScrollEnabled(false);

		EditText filtro = (EditText) findViewById(R.id.editText_filtra);
		filtro.addTextChangedListener(this);

		spinnerFornitore = (EConTabSpinner) findViewById(R.id.spinner_fornitore);
		spinnerLinea = (EConTabSpinner) findViewById(R.id.spinner_linea);
		int idFornitore = getIntent().getIntExtra(Costruttori.ID_COSTRUTTORE, 0);
		int idLinea = getIntent().getIntExtra(Linee.ID_LINEA, 0);
		if (idFornitore != 0) {
			spinnerFornitore.setValue("" + idFornitore);
		}
		if (idLinea != 0) {
			spinnerLinea.setValue("" + idLinea);
		}
		spinnerFornitore.setTabella(new Costruttori());
		spinnerLinea.setTabella(new Linee());
		spinnerFornitore.setSpinnerCollegato(spinnerLinea);

		spinnerFornitore.addTextChangeListener(this);
		spinnerLinea.addTextChangeListener(this);
		System.out.println("EConTab: CodiciArticoliActivity onCreate EXIT");
	}

	private  void ricerca() {
		// TODO Auto-generated method stub
		// new class for asynchronous task
		final AsyncTaskExecutorService taskricerca = new AsyncTaskExecutorService() {
			String valforn = spinnerFornitore.getValue();
			String vallinea = spinnerLinea.getValue();

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				inRicerca = true;
				lista.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (inRicerca){
							progressBarAttesa.setVisibility(View.VISIBLE);
						}

					}
				},1500);
			}

			@Override
			protected Object doInBackground(Object o) {
				System.out.println("EConTab: CodiciArticoliActivity doInBackground ENTER");
				DbInterno db = new DbInterno(CodiciArticoliActivity.this);

				String filtro = getTesto(R.id.editText_filtra);

				if (filtro.length() > 0) {
					filtro = "%" + filtro + "%";
				} else {
					filtro = "%";
				}

				String filtroFornitoreLinea = "";
				Listini tabListini = new Listini();

				if (!valforn.equals("")) {
					filtroFornitoreLinea = " and " + tabListini.getNomeCampoTabella(Listini.ID_COSTRUTTORE) + "=" + valforn;
					if (vallinea.equals("")) {
						filtroFornitoreLinea = filtroFornitoreLinea + " and " + tabListini.getNomeCampoTabella(Listini.ID_LINEA) + "=0";
					}
				}
				if (!vallinea.equals("")) {
					filtroFornitoreLinea = " and " + tabListini.getNomeCampoTabella(Listini.ID_LINEA) + "=" + vallinea;
				}

				Join j0 = new Join(Listini.NOME_TABELLA, Costruttori.NOME_TABELLA, Join.LEFT_JOIN);
				j0.addCampiDiJoin(Listini.ID_COSTRUTTORE, Costruttori.ID_COSTRUTTORE);

				Join j1 = new Join(Listini.NOME_TABELLA, Linee.NOME_TABELLA, Join.LEFT_JOIN);
				j1.addCampiDiJoin(Listini.ID_LINEA, Linee.ID_LINEA);

				ArrayList datiNew = db.eseguiSelect("Select " + Listini.NOME_TABELLA + ".*," + Costruttori.SIGLA_METEL + "," + Linee.NOME_LINEA + ",(select count(*) from ass_codici_linee where codice_articolo=listini.codice_articolo) as asscodicilinee from "
						+ Listini.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where (" + Listini.CODICE_ARTICOLO + " like ? or "
						+ Listini.DESCRIZIONE + " like ? ) " + filtroFornitoreLinea, new String[]{filtro, filtro});

				db.close();
				System.out.println("EConTab: CodiciArticoliActivity doInBackground EXIT");
				return datiNew;
			}

			@Override
			protected void onPostExecute(Object o) {
				inRicerca = false;
				progressBarAttesa.setVisibility(View.GONE);
				lista.invalidate();
				if (dati == null) {
					dati = new ArrayList<Object>();
				}
				dati.clear();
				dati.addAll((ArrayList)o);

				if (adapter == null) {
					adapter = new CodiciArticoliAdapter(CodiciArticoliActivity.this, dati, R.layout.list_item_codice_articolo);
					lista.setAdapter(adapter);
					lista.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
					registerForContextMenu(lista);
				} else {
					adapter.notifyDataSetChanged();

				}
			}
		};

		synchronized (this){
			taskricerca.execute();
		}
	}

	public void nuovoCodice(View v) {

		Intent intent = new Intent(this, CodiceArticoloModActivity.class);
		if (!spinnerFornitore.getValue().equals("")) {
			intent.putExtra(Listini.ID_COSTRUTTORE, Integer.parseInt(spinnerFornitore.getValue()));
		}
		if (!spinnerLinea.getValue().equals("")) {
			intent.putExtra(Listini.ID_LINEA, Integer.parseInt(spinnerLinea.getValue()));
		}
		apriFinestraInserimento(intent, 1, new Listini());

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		ContentValues item = (ContentValues) lista.getItemAtPosition(info.position);
		menu.setHeaderTitle(item.getAsString(Listini.CODICE_ARTICOLO));
		menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
		menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
		menu.add(Menu.NONE,3,Menu.NONE,"Visualizza associazioni con linee alternative");

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		ContentValues articolo = (ContentValues) lista.getItemAtPosition(info.position);
		if (item.getItemId() == 1) {
			Intent intent = new Intent(this, CodiceArticoloModActivity.class);
			intent.putExtra("ID", articolo.getAsString(Listini.CODICE_ARTICOLO));

			apriFinestraModifica(intent, 1);
		}
		if (item.getItemId() == 2) {
			confermaCancellazione(new Listini(), articolo, true);
		}
		if (item.getItemId() == 3) {
			Intent intent = new Intent(this, AssCodiciLineeActivity.class);
			intent.putExtra(AssCodiciLinee.CODICE_ARTICOLO, articolo.getAsString(Listini.CODICE_ARTICOLO));
			startActivity(intent);
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		// TODO Auto-generated method stub
		Intent intent = new Intent(this, CodiceArticoloModActivity.class);
		ContentValues item = (ContentValues) lista.getItemAtPosition(position);
		intent.putExtra("ID", item.getAsString(Listini.CODICE_ARTICOLO));
		startActivity(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		ricerca();

	}

	@Override
	protected void aggiornaDopoCancellazione() {
		// TODO Auto-generated method stub
		ricerca();
	}

	@Override
	public void afterTextChanged(Editable s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
		ricerca();
	}


}
