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
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import pfa.app.econtab.adapters.FornitoriLineeAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;

public class FornitoriLineeActivity extends EConTabActivity implements OnChildClickListener {

	ExpandableListView lista = null;
	HashMap<String, ArrayList<Object>> listChildData = null;
	ArrayList<Object> sezioni = null;
	ArrayList<String> sezioniCodici = null;

	FornitoriLineeAdapter adapter = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: FornitoriLineeActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fornitori_linee);
		lista = (ExpandableListView) findViewById(R.id.lista);
		lista.setOnChildClickListener(this);
		EditText filtro = (EditText) findViewById(R.id.editText_filtra);
		filtro.addTextChangedListener(new TextWatcher() {

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
		registerForContextMenu(lista);
		System.out.println("EConTab: FornitoriLineeActivity onCreate EXIT");
	}

	@Override
	protected void onResume() {
		System.out.println("EConTab: FornitoriLineeActivity onResume ENTER");
		// TODO Auto-generated method stub
		super.onResume();
		ricerca();
		System.out.println("EConTab: FornitoriLineeActivity onResume EXIT");
	}

	@Override
	protected void aggiornaDopoCancellazione() {
		System.out.println("EConTab: FornitoriLineeActivity aggiornaDopoCancellazione ENTER");
		// TODO Auto-generated method stub
		ricerca();
		System.out.println("EConTab: FornitoriLineeActivity aggiornaDopoCancellazione EXIT");
	}

	private void ricerca() {
		System.out.println("EConTab: FornitoriLineeActivity ricerca ENTER");
		// TODO Auto-generated method stub
		lista.invalidate();
		if (sezioni == null) {
			sezioni = new ArrayList<Object>();
			sezioniCodici = new ArrayList<String>();
		}
		if (listChildData == null) {
			listChildData = new HashMap<String, ArrayList<Object>>();
		}
		sezioni.clear();
		sezioniCodici.clear();
		listChildData.clear();

		DbInterno db = new DbInterno(this);
		String filtro = getTesto(R.id.editText_filtra);
		if (filtro.length() > 0) {
			filtro = "%" + filtro + "%";
		} else {
			filtro = "%";
		}

		String SQL = "Select " + Costruttori.NOME_TABELLA + ".*," + Linee.ID_LINEA + "," + Linee.NOME_LINEA + " from "
				+ Costruttori.NOME_TABELLA + " " + "left join " + Linee.NOME_TABELLA + " on " + Costruttori.NOME_TABELLA + "."
				+ Costruttori.ID_COSTRUTTORE + "=" + Linee.NOME_TABELLA + "." + Linee.ID_COSTRUTTORE + " where "
				+ Costruttori.RAGIONE_SOCIALE + " like ? order by " + Costruttori.RAGIONE_SOCIALE;
		ArrayList<Object> listaFornitori = db.eseguiSelect(SQL, new String[] { filtro });
		db.close();

		for (int i = 0; i < listaFornitori.size(); i++) {
			ContentValues forni = (ContentValues) listaFornitori.get(i);
			String key = forni.getAsString(Costruttori.ID_COSTRUTTORE);
			if (sezioniCodici.contains(key)) {
				if (forni.getAsString(Linee.ID_LINEA) != null) {
					ArrayList<Object> linee = listChildData.get(key);
					linee.add(forni);
				}
			} else {
				sezioni.add(forni);
				sezioniCodici.add(key);
				ArrayList<Object> linee = new ArrayList<Object>();

				if (forni.getAsString(Linee.ID_LINEA) != null) {

					linee.add(forni);
				}
				listChildData.put(key, linee);

			}
		}

		Iterator<String> iterChild = listChildData.keySet().iterator();
		while (iterChild.hasNext()) {
			ArrayList<Object> linee = listChildData.get(iterChild.next());
			ContentValues noLinea = new ContentValues();
			noLinea.put(Linee.ID_LINEA, 0);
			noLinea.put(Linee.NOME_LINEA, getString(R.string.altro_materiale));
			linee.add(noLinea);
		}

		if (adapter == null) {
			adapter = new FornitoriLineeAdapter(this, sezioniCodici, listChildData, R.layout.list_group_fornitore,
					R.layout.list_item_linea, sezioni);
			lista.setAdapter(adapter);
		} else {
			adapter.notifyDataSetChanged();

		}
		System.out.println("EConTab: FornitoriLineeActivity ricerca EXIT");
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		System.out.println("EConTab: FornitoriLineeActivity onCreateContextMenu ENTER");
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);

		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);

		if (type == 0) {
			int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);

			ContentValues val = (ContentValues) sezioni.get(group);

			menu.setHeaderTitle(val.getAsString(Costruttori.RAGIONE_SOCIALE));
			menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
			menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
		}

		if (type == 1) {
			int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			int child = ExpandableListView.getPackedPositionChild(info.packedPosition);

			ContentValues val = (ContentValues) listChildData.get(sezioniCodici.get(group)).get(child);

			if (val.getAsInteger(Linee.ID_LINEA) != 0) {

				menu.setHeaderTitle(val.getAsString(Linee.NOME_LINEA));
				menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
				menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
			}
		}
		System.out.println("EConTab: FornitoriLineeActivity onCreateContextMenu EXIT");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		System.out.println("EConTab: FornitoriLineeActivity onContextItemSelected ENTER");
		// TODO Auto-generated method stub
		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == 0) {
			int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			if (item.getItemId() == 1) {
				Intent intent = new Intent(this, CostruttoriDettaglioActivity.class);
				intent.putExtra("ID", Integer.parseInt(sezioniCodici.get(group)));
				apriFinestraModifica(intent, 1);
			}
			if (item.getItemId() == 2) {
				ContentValues val = new ContentValues();
				val.put(Costruttori.ID_COSTRUTTORE, sezioniCodici.get(group));
				confermaCancellazione(new Costruttori(), val, true);

			}
		}
		if (type == 1) {
			int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
			ContentValues val = (ContentValues) listChildData.get(sezioniCodici.get(group)).get(child);

			if (item.getItemId() == 1) {
				Intent intent = new Intent(this, LineeDettaglioModActivity.class);
				intent.putExtra("ID", val.getAsInteger(Linee.ID_LINEA));
				apriFinestraModifica(intent, 1);
			}
			if (item.getItemId() == 2) {

				confermaCancellazione(new Linee(), val, true);

			}

		}
		System.out.println("EConTab: FornitoriLineeActivity onContextItemSelected EXIT");
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onChildClick(ExpandableListView arg0, View arg1, int groupPosition, int childPosition, long arg4) {
		System.out.println("EConTab: FornitoriLineeActivity onChildClick ENTER");
		// TODO Auto-generated method stub
		Intent intent = new Intent(this, CodiciArticoliActivity.class);
		ContentValues val = (ContentValues) listChildData.get(sezioniCodici.get(groupPosition)).get(childPosition);
		int idLinea = val.getAsInteger(Linee.ID_LINEA);
		int idCostruttore = Integer.parseInt(sezioniCodici.get(groupPosition));

		intent.putExtra(Linee.ID_LINEA, idLinea);
		intent.putExtra(Costruttori.ID_COSTRUTTORE, idCostruttore);

		startActivity(intent);
		System.out.println("EConTab: FornitoriLineeActivity onChildClick EXIT");
		return true;
	}

	public void nuovoFornitore(View v) {
		Intent intent = new Intent(this, CostruttoriDettaglioActivity.class);
		apriFinestraInserimento(intent, 1, new Costruttori());
	}

}
