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

import java.util.ArrayList;

import pfa.app.econtab.adapters.ClientiAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;

public class ClientiActivity extends EConTabActivity implements OnItemClickListener {

	private ListView lista = null;
	private ClientiAdapter adapter = null;
	
	private ArrayList<Object> dati = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: ClientiActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_clienti);
		
		lista = (ListView)findViewById(R.id.lista);
		lista.setOnItemClickListener(this);
		
		EditText filtro = (EditText)findViewById(R.id.editText_filtra);
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
		System.out.println("EConTab: ClientiActivity onCreate EXIT");
	}

	private void ricerca() {
		System.out.println("EConTab: ClientiActivity ricerca ENTER");
		// TODO Auto-generated method stub
		lista.invalidate();
		if (dati==null){
			dati = new ArrayList<Object>();
		}
		dati.clear();
		
		DbInterno db = new DbInterno(this);
		
		String filtro = getTesto(R.id.editText_filtra);
		String SQL = "Select * from " + Anagrafica.NOME_TABELLA + " where " + Anagrafica.RAGIONE_SOCIALE + " like ? or "+Anagrafica.INDIRIZZO+" like ? or " +Anagrafica.CITTA + " like ? or "+Anagrafica.CODICE_ESTERNO+" like ? order by "+Anagrafica.RAGIONE_SOCIALE +" asc";
		System.out.println("EConTab: ClientiActivity ricerca SQL " + SQL);
		if (filtro.length()>0){
			filtro = "%" + filtro +"%";
		}
		else{
			filtro = "%";
		}

		ArrayList<Object> elems = db.eseguiSelect(SQL, new String[]{filtro,filtro,filtro,filtro});
		System.out.println("EConTab: ClientiActivity ricerca elems " + elems.toString());
		dati.addAll(elems);

		//dati.addAll(db.eseguiSelect(SQL,new String[]{filtro,filtro,filtro,filtro}));
		db.close();

		if (adapter==null){
			adapter = new ClientiAdapter(this, dati, R.layout.list_item_cliente);
			lista.setAdapter(adapter);
			lista.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
			registerForContextMenu(lista);
		}
		else{
			adapter.notifyDataSetChanged();
			
		}
		System.out.println("EConTab: ClientiActivity ricerca EXIT");
		
	}

	public void nuovoCliente(View v){
		System.out.println("EConTab: ClientiActivity nuovoCliente");
		Intent intent = new Intent(this,ClientiDettaglioModActivity.class);
		apriFinestraInserimento(intent, 1,new Anagrafica());
		
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {
		System.out.println("EConTab: ClientiActivity onCreateContextMenu ENTER");
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
	    ContentValues item = (ContentValues)lista.getItemAtPosition(info.position);
		menu.setHeaderTitle(item.getAsString(Anagrafica.RAGIONE_SOCIALE));
		menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
		menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
		System.out.println("EConTab: ClientiActivity onCreateContextMenu EXIT");
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		System.out.println("EConTab: ClientiActivity onContextItemSelected ENTER");
		// TODO Auto-generated method stub
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	    ContentValues cliente = (ContentValues)lista.getItemAtPosition(info.position);
		if (item.getItemId()==1){
      		Intent intent = new Intent(this,ClientiDettaglioModActivity.class);
      		intent.putExtra("ID", cliente.getAsInteger(Anagrafica.ID_ANAGRAFICA));
			apriFinestraModifica(intent, 1);
      	}
      	if (item.getItemId()==2){
      		confermaCancellazione(new Anagrafica(),cliente,true);
      	}
		System.out.println("EConTab: ClientiActivity onContextItemSelected EXIT");
		return super.onContextItemSelected(item);
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		System.out.println("EConTab: ClientiActivity onItemClick");
		// TODO Auto-generated method stub
		Intent intent = new Intent(this,ClientiDettaglioActivity.class);
	    ContentValues item = (ContentValues)lista.getItemAtPosition(position);
		intent.putExtra(Anagrafica.ID_ANAGRAFICA, item.getAsInteger(Anagrafica.ID_ANAGRAFICA));
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
}
