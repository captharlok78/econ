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
import android.widget.ListView;

import java.util.ArrayList;

import pfa.app.econtab.adapters.PlaccheAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Placche;
import pfa.app.econtab.views.EConTabSpinner;

public class PlaccheActivity extends EConTabActivity implements OnItemClickListener, TextWatcher {

	private ListView lista = null;
	private PlaccheAdapter adapter = null;

	private ArrayList<Object> dati = null;

	private EConTabSpinner spinnerLinea = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: PlaccheActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setVisualizzazionePopup(7, 3);
		setContentView(R.layout.activity_placche);

		lista = (ListView) findViewById(R.id.lista);
		lista.setOnItemClickListener(this);

		spinnerLinea = (EConTabSpinner) findViewById(R.id.spinner_linea);

		int idLinea = getIntent().getIntExtra(Linee.ID_LINEA, 0);

		if (idLinea != 0) {
			spinnerLinea.setValue("" + idLinea);
		}

		spinnerLinea.setTabella(new Linee());

		spinnerLinea.addTextChangeListener(this);
		System.out.println("EConTab: PlaccheActivity onCreate EXIT");
	}

	private void ricerca() {
		System.out.println("EConTab: PlaccheActivity ricerca ENTER");
		// TODO Auto-generated method stub
		lista.invalidate();
		if (dati == null) {
			dati = new ArrayList<Object>();
		}
		dati.clear();

		DbInterno db = new DbInterno(this);

		String filtroLinea = "";
		Placche tabPlacche = new Placche();

		if (!spinnerLinea.getValue().equals("")) {
			filtroLinea = " where " + tabPlacche.getNomeCampoTabella(Placche.ID_LINEA) + "=" + spinnerLinea.getValue();
		}

		Join j0 = new Join(Placche.NOME_TABELLA, Linee.NOME_TABELLA, Join.LEFT_JOIN);
		j0.addCampiDiJoin(Placche.ID_LINEA, Linee.ID_LINEA);

		Join j1 = new Join(Linee.NOME_TABELLA, Costruttori.NOME_TABELLA, Join.LEFT_JOIN);
		j1.addCampiDiJoin(Linee.ID_COSTRUTTORE, Costruttori.ID_COSTRUTTORE);

		dati.addAll(db.eseguiSelect("Select " + Placche.NOME_TABELLA + ".*," + Costruttori.SIGLA_METEL + "," + Linee.NOME_LINEA + " from "
				+ Placche.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + filtroLinea, null));

		db.close();

		if (adapter == null) {
			adapter = new PlaccheAdapter(this, dati, R.layout.list_item_placca);
			lista.setAdapter(adapter);
			lista.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
			registerForContextMenu(lista);
		} else {
			adapter.notifyDataSetChanged();

		}
		System.out.println("EConTab: PlaccheActivity ricerca EXIT");
	}

	public void nuovaPlacca(View v) {
		System.out.println("EConTab: PlaccheActivity nuovaPlacca ENTER");
		Intent intent = new Intent(this, PlaccheDettaglioModActivity.class);

		if (!spinnerLinea.getValue().equals("")) {
			intent.putExtra(Linee.ID_LINEA, spinnerLinea.getValue());
		}
		apriFinestraInserimento(intent, 1, new Placche());
		System.out.println("EConTab: PlaccheActivity nuovaPlacca EXIT");
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		System.out.println("EConTab: PlaccheActivity onCreateContextMenu ENTER");
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		ContentValues item = (ContentValues) lista.getItemAtPosition(info.position);
		menu.setHeaderTitle(item.getAsString(Placche.NOME_PLACCA));
		menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
		menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
		System.out.println("EConTab: PlaccheActivity onCreateContextMenu EXIT");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		System.out.println("EConTab: PlaccheActivity onContextItemSelected ENTER");
		// TODO Auto-generated method stub
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		ContentValues placca = (ContentValues) lista.getItemAtPosition(info.position);
		if (item.getItemId() == 1) {
			Intent intent = new Intent(this, PlaccheDettaglioModActivity.class);
			intent.putExtra("ID", placca.getAsInteger(Placche.ID_PLACCA));

			apriFinestraModifica(intent, 1);
		}
		if (item.getItemId() == 2) {
			confermaCancellazione(new Placche(), placca, true);
		}
		System.out.println("EConTab: PlaccheActivity onContextItemSelected EXIT");
		return super.onContextItemSelected(item);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		System.out.println("EConTab: PlaccheActivity onItemClick ENTER");
		// TODO Auto-generated method stub
		Intent intent = new Intent(this, PlaccheDettaglioModActivity.class);
		ContentValues item = (ContentValues) lista.getItemAtPosition(position);
		intent.putExtra("ID", item.getAsInteger(Placche.ID_PLACCA));
		startActivity(intent);
		System.out.println("EConTab: PlaccheActivity onItemClick EXIT");
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

	public void chiudi(View v) {
		finish();
	}
}
