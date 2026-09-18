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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

import pfa.app.econtab.CantiereSplitActivity;
import pfa.app.econtab.CantieriDettaglioModActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.adapters.CantieriAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.utils.Sessione;

public  class CantieriListaFragment extends EConTabFragment implements OnItemClickListener {
	private int cliente = 0;
	private ListView lista = null;
	private CantieriAdapter adapter = null;
	
	private ArrayList<Object> dati = null;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View v = super.onCreateView(inflater, container, savedInstanceState);
		v = inflater.inflate(R.layout.fragment_cantieri_lista, container,false);
		if (getArguments()!=null){
			cliente = getArguments().getInt(Anagrafica.ID_ANAGRAFICA);
		}
		
		lista = (ListView)v.findViewById(R.id.lista);
		lista.setOnItemClickListener(this);
		EditText filtro = (EditText)v.findViewById(R.id.editText_filtra);
		filtro.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
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
		if (dati==null){
			dati = new ArrayList<Object>();
		}
		dati.clear();
		
		DbInterno db = new DbInterno(getActivity());
		Anagrafica ana = new Anagrafica();
		Cantieri cant = new Cantieri();
		String filtro = getTesto(R.id.editText_filtra);
		String filtroCliente = "";
		if (cliente!=0){
			filtroCliente = " and " + cant.getNomeCampoTabella(Cantieri.ID_ANAGRAFICA) + "=" + cliente;
		}
		
		if (filtro.length()>0){
			filtro = "%" + filtro +"%";
		}
		else{
			filtro = "%";
		}

		Join join1 = new Join(Cantieri.NOME_TABELLA, Anagrafica.NOME_TABELLA);
		join1.addCampiDiJoin(Cantieri.ID_ANAGRAFICA, Anagrafica.ID_ANAGRAFICA);
		
		String SQL = "Select "+ cant.getNomeCampoTabella("*") +","+ana.getNomeCampoTabella(Anagrafica.RAGIONE_SOCIALE)+" from " +
				" " + cant.getNomeTabella() + join1.getSQLJoin() +
				" where (" + Cantieri.NOME + " like ? or "+cant.getNomeCampoTabella(Cantieri.INDIRIZZO)+" like ? or " +cant.getNomeCampoTabella(Cantieri.CITTA) + " like ? ) "+filtroCliente+" and "+Cantieri.ID_DITTA+"="+Sessione.getDittaSelezionata()+
				" order by "+Cantieri.DATA_INS +" desc";
		
		dati.addAll(db.eseguiSelect(SQL,new String[]{filtro,filtro,filtro}));
		db.close();

		if (adapter==null){
			adapter = new CantieriAdapter(getActivity(), dati, R.layout.list_item_cantiere);
			lista.setAdapter(adapter);
			lista.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
			registerForContextMenu(lista);
		}
		else{
			adapter.notifyDataSetChanged();
		}
	}
	
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
	    ContentValues item = (ContentValues)lista.getItemAtPosition(info.position);
		menu.setHeaderTitle(item.getAsString(Cantieri.NOME));
		menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.visualizza));
		menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.modifica));
		menu.add(Menu.NONE, 3, Menu.NONE, getString(R.string.elimina));
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	    ContentValues cantiere = (ContentValues)lista.getItemAtPosition(info.position);
		if (item.getItemId()==1){
			
			Intent intent = new Intent(getActivity(),CantiereSplitActivity.class);
			intent.putExtra(Cantieri.ID_CANTIERE, cantiere.getAsInteger(Cantieri.ID_CANTIERE));
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
      	}
      	if (item.getItemId()==2){
      		Intent intent = new Intent(getActivity(),CantieriDettaglioModActivity.class);
      		intent.putExtra("ID", cantiere.getAsInteger(Cantieri.ID_CANTIERE));
      		getEConTabActivity().apriFinestraModifica(intent,2);
      	}
      	if (item.getItemId()==3){
      		getEConTabActivity().confermaCancellazione(new Cantieri(),cantiere,true);
      	}
		return super.onContextItemSelected(item);
	}
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		ricerca();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		// TODO Auto-generated method stub
		ContentValues cantiere = (ContentValues)lista.getItemAtPosition(position);
		Intent intent = new Intent(getActivity(),CantiereSplitActivity.class);
		intent.putExtra(Cantieri.ID_CANTIERE, cantiere.getAsInteger(Cantieri.ID_CANTIERE));
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
	}
	
	
	
}
