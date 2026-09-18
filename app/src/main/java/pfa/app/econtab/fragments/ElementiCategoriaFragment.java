package pfa.app.econtab.fragments;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import pfa.app.econtab.ElementoModActivity;
import pfa.app.econtab.FinestraElementiActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.adapters.GrigliaElementiAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.CategorieGenerali;
import pfa.app.econtab.db.table.Elementi;

public class ElementiCategoriaFragment extends EConTabFragment implements OnClickListener, OnItemClickListener {

	private GridView lista = null;

	private ArrayList<Object> dati = null;
	private LinearLayout buttonPreferiti = null;
	private boolean soloPreferiti = true;
	private GrigliaElementiAdapter adapter = null;

	private int idCategoria = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View v = super.onCreateView(inflater, container, savedInstanceState);
		v = inflater.inflate(R.layout.fragment_elementi_categoria, container, false);

		idCategoria = getArguments().getInt(CategorieGenerali.ID_CATEGORIA_GENERALE);

		buttonPreferiti = (LinearLayout) v.findViewById(R.id.linear_preferiti);
		buttonPreferiti.setOnClickListener(this);

		v.findViewById(R.id.buttonnuovo).setOnClickListener(this);

		lista = (GridView) v.findViewById(R.id.griglia_elementi);
		lista.setOnItemClickListener(this);

		registerForContextMenu(lista);

		dati = null;
		adapter = null;

		EditText filtro = (EditText) v.findViewById(R.id.editText_filtra);
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

		return v;

	}

	public void ricerca() {
		// TODO Auto-generated method stub
		lista.invalidate();
		if (dati == null) {
			dati = new ArrayList<Object>();
			adapter = new GrigliaElementiAdapter(getActivity(), dati, R.layout.grid_item_elemento);
			lista.setAdapter(adapter);
			lista.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
		}

		dati.clear();
		String filtro = getTesto(R.id.editText_filtra, getView());
		DbInterno db = new DbInterno(getActivity());
		String filtroPreferito = "";
		if (soloPreferiti) {
			filtroPreferito = " and " + Elementi.PREFERITO_SN + "=1";
		}
		String SQL = "Select * from " + Elementi.NOME_TABELLA + " where " + Elementi.ID_CATEGORIA_GENERALE + " = " + idCategoria + " and "
				+ Elementi.NOME_ELEMENTO + " like ? " + filtroPreferito;
		String filtroRicerca = "%";
		if (filtro.length() > 0) {
			filtroRicerca = "%" + filtro + "%";
		}
		// dati.addAll(db.eseguiSelect(SQL,new String[]{filtroRicerca}));

		ArrayList<Object> elems = db.eseguiSelect(SQL, new String[] { filtroRicerca });
		dati.addAll(elems);
		db.close();
		adapter.notifyDataSetChanged();

		// Loader loadTask = new Loader(adapter, dati);
		// loadTask.execute();

	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		setSoloPreferiti(soloPreferiti);
		ricerca();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.linear_preferiti) {
			setSoloPreferiti(!soloPreferiti);
			ricerca();
		}
		if (v.getId() == R.id.buttonnuovo) {
			Intent intent = new Intent(getActivity(), ElementoModActivity.class);
			intent.putExtra(CategorieGenerali.ID_CATEGORIA_GENERALE, idCategoria);

			startActivity(intent);
		}
	}

	private void setSoloPreferiti(boolean preferiti) {
		ImageView img = (ImageView) getView().findViewById(R.id.image_Preferiti);
		TextView txtPref = (TextView) getView().findViewById(R.id.textView_preferiti);
		TextView txtTutti = (TextView) getView().findViewById(R.id.textView_tutti);
		if (!preferiti) {
			img.setImageResource(android.R.drawable.star_big_off);
			txtTutti.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
			txtPref.setTextColor(Color.parseColor("#CCCCCC"));
		} else {
			img.setImageResource(android.R.drawable.star_big_on);
			txtPref.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
			txtTutti.setTextColor(Color.parseColor("#CCCCCC"));
		}
		soloPreferiti = preferiti;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		// TODO Auto-generated method stub
		ContentValues val = (ContentValues) lista.getItemAtPosition(position);

		((FinestraElementiActivity) getEConTabActivity()).aggiungiElemento(val);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		ContentValues item = (ContentValues) lista.getItemAtPosition(info.position);
		menu.setHeaderTitle(item.getAsString(Elementi.NOME_ELEMENTO));
		menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
		menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (getUserVisibleHint()) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			ContentValues elemento = (ContentValues) lista.getItemAtPosition(info.position);

			if (item.getItemId() == 1) {
				Intent intent = new Intent(getActivity(), ElementoModActivity.class);
				intent.putExtra("ID", elemento.getAsInteger(Elementi.ID_ELEMENTO));
				intent.putExtra(CategorieGenerali.ID_CATEGORIA_GENERALE, idCategoria);
				getEConTabActivity().apriFinestraModifica(intent, 2);
			}
			if (item.getItemId() == 2) {
				getEConTabActivity().confermaCancellazione(new Elementi(), elemento, true);
			}
			return true;

		}

		return false;
	}

}
