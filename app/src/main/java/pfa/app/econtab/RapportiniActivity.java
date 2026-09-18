package pfa.app.econtab;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

import pfa.app.econtab.adapters.RapportiniAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.Rapportini;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.db.table.Utenti;
import pfa.app.econtab.export.PreventivoXLS;
import pfa.app.econtab.export.RapportinoXLS;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabSpinner;

public class RapportiniActivity extends EConTabActivity implements OnItemClickListener {

	private ListView lista = null;
	private RapportiniAdapter adapter = null;
	
	private ArrayList<Object> dati = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: RapportiniActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rapportini);
		
		lista = (ListView)findViewById(R.id.lista);
		lista.setOnItemClickListener(this);

        EConTabSpinner spinnerPeriodo  = (EConTabSpinner)findViewById(R.id.spinner_periodo);

        ArrayList<Object> valoriPeriodo = new ArrayList<Object>();
        ContentValues valSettimana = new ContentValues();
        valSettimana.put(EConTabSpinner.VALORE,"S");
        valSettimana.put(EConTabSpinner.DESCRIZIONE,getString(R.string.ultima_settimana));
        valoriPeriodo.add(valSettimana);

        ContentValues valMese = new ContentValues();
        valMese.put(EConTabSpinner.VALORE,"M");
        valMese.put(EConTabSpinner.DESCRIZIONE,getString(R.string.ultimo_mese));
        valoriPeriodo.add(valMese);

        ContentValues val3Mese = new ContentValues();
        val3Mese.put(EConTabSpinner.VALORE,"3M");
        val3Mese.put(EConTabSpinner.DESCRIZIONE,getString(R.string.ultimi_3_mesi));
        valoriPeriodo.add(val3Mese);

        ContentValues val6Mese = new ContentValues();
        val6Mese.put(EConTabSpinner.VALORE,"6M");
        val6Mese.put(EConTabSpinner.DESCRIZIONE,getString(R.string.ultimi_6_mesi));
        valoriPeriodo.add(val6Mese);

        ContentValues valAnno = new ContentValues();
        valAnno.put(EConTabSpinner.VALORE,"A");
        valAnno.put(EConTabSpinner.DESCRIZIONE,getString(R.string.ultimo_anno));
        valoriPeriodo.add(valAnno);

        ContentValues valSempre = new ContentValues();
        valSempre.put(EConTabSpinner.VALORE,"");
        valSempre.put(EConTabSpinner.DESCRIZIONE,getString(R.string.sempre));
        valoriPeriodo.add(valSempre);


        spinnerPeriodo.setValue("S");
        spinnerPeriodo.setValoriSpinnerLibero(valoriPeriodo);


        spinnerPeriodo.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                ricerca();
            }
        });
		
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
		System.out.println("EConTab: RapportiniActivity onCreate EXIT");
	}

	public void ricerca() {
		System.out.println("EConTab: RapportiniActivity ricerca ENTER");
		// TODO Auto-generated method stub
		lista.invalidate();
		if (dati==null){
			dati = new ArrayList<Object>();
		}
		dati.clear();
		
		DbInterno db = new DbInterno(this);

        Join j0 = new Join(Rapportini.NOME_TABELLA, Preventivi.NOME_TABELLA);
        j0.addCampiDiJoin(Rapportini.ID_ORDINE, Preventivi.ID_PREVENTIVO);

		Join j0_1 = new Join(Rapportini.NOME_TABELLA, Utenti.NOME_TABELLA);
		j0_1.addCampiDiJoin(Rapportini.ID_OPERATORE, Utenti.ID_UTENTE);

        Join j1 = new Join(Preventivi.NOME_TABELLA, Cantieri.NOME_TABELLA);
        j1.addCampiDiJoin(Preventivi.ID_CANTIERE, Cantieri.ID_CANTIERE);

        Join j2 = new Join(Cantieri.NOME_TABELLA,Anagrafica.NOME_TABELLA);
        j2.addCampiDiJoin(Cantieri.ID_ANAGRAFICA, Anagrafica.ID_ANAGRAFICA);

		String filtro = getTesto(R.id.editText_filtra);
        String filtroPeriodo = getTesto(R.id.spinner_periodo);
        Calendar cal = Calendar.getInstance();
        if (filtroPeriodo.equals("S")){
            cal.add(Calendar.DATE,-7);
        }
        if (filtroPeriodo.equals("M")){
            cal.add(Calendar.DATE,-30);
        }
        if (filtroPeriodo.equals("3M")){
            cal.add(Calendar.DATE,-90);
        }
        if (filtroPeriodo.equals("6M")){
            cal.add(Calendar.DATE,-180);
        }
        if (filtroPeriodo.equals("A")){
            cal.add(Calendar.DATE,-365);
        }
        long dataNumber = Utility.dataToNumber(cal);

        if (!filtroPeriodo.equals("")){
            filtroPeriodo = " and " + Rapportini.DATA_RAPPORTINO+">="+dataNumber;
        }

		String SQL = "Select "+Rapportini.NOME_TABELLA+".*,"+Anagrafica.RAGIONE_SOCIALE+","+ Preventivi.NUMERO +","+ Preventivi.TITOLO +","+ Preventivi.DATA+" as data_ordine,cantieri.nome as nome_cantiere," +
                "(select sum("+ RapportiniDettaglio.ORE+") from "+RapportiniDettaglio.NOME_TABELLA+" where " + RapportiniDettaglio.NOME_TABELLA+"."+RapportiniDettaglio.ID_RAPPORTINO + "=" + Rapportini.NOME_TABELLA+"."+Rapportini.ID_RAPPORTINO +" ) as tot_ore,utenti.nome as nome_operatore,"+Utenti.COGNOME+" as cognome_operatore " +
                "from " + Rapportini.NOME_TABELLA + j0.getSQLJoin()+j0_1.getSQLJoin()+j1.getSQLJoin()+j2.getSQLJoin() +" " +
                "where " + Anagrafica.RAGIONE_SOCIALE + " like ? "+ filtroPeriodo +" and "+Rapportini.NOME_TABELLA+"."+Rapportini.ID_DITTA+"="+ Sessione.getDittaSelezionata()+"  order by "+Rapportini.DATA_RAPPORTINO +" desc";
		if (filtro.length()>0){
			filtro = "%" + filtro +"%";
		}
		else{
			filtro = "%";
		}
		dati.addAll(db.eseguiSelect(SQL, new String[]{filtro}));
		db.close();

		if (adapter==null){
			adapter = new RapportiniAdapter(this, dati, R.layout.list_item_rapportino);
			lista.setAdapter(adapter);
			lista.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
			registerForContextMenu(lista);
		}
		else{
			adapter.notifyDataSetChanged();
			
		}

		System.out.println("EConTab: RapportiniActivity ricerca EXIT");
	}

	public void nuovoRapportino(View v){
		System.out.println("EConTab: RapportiniActivity nuovoRapportino ENTER");
		Intent intent = new Intent(this,RapportinoDettaglioModActivity.class);
		apriFinestraInserimento(intent, 1,new Rapportini());
		System.out.println("EConTab: RapportiniActivity nuovoRapportino EXIT");
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {
		System.out.println("EConTab: RapportiniActivity onCreateContextMenu ENTER");
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
	    ContentValues item = (ContentValues)lista.getItemAtPosition(info.position);
		menu.setHeaderTitle(getString(R.string.rapportino_del)+" "+ Utility.numberToData(item.getAsLong(Rapportini.DATA_RAPPORTINO)));
		menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
		menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
		menu.add(Menu.NONE, 3, Menu.NONE, getString(R.string.esporta_rapportino_xls));
		menu.add(Menu.NONE, 4, Menu.NONE, getString(R.string.esporta_rapportino_multiplo_xls));
		System.out.println("EConTab: RapportiniActivity onCreateContextMenu EXIT");
	}
	
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		System.out.println("EConTab: RapportiniActivity onContextItemSelected ENTER");
		// TODO Auto-generated method stub
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	    ContentValues rapportino = (ContentValues)lista.getItemAtPosition(info.position);
		if (item.getItemId()==1){
      		modifica(rapportino);
      	}
      	if (item.getItemId()==2){
      		confermaCancellazione(new Rapportini(), rapportino, true);
      	}
		if (item.getItemId()==3){
			esportaRapportino(rapportino.getAsInteger(Rapportini.ID_RAPPORTINO),false);
		}
		if (item.getItemId()==4){
			esportaRapportino(rapportino.getAsInteger(Rapportini.ID_RAPPORTINO),true);
		}
		System.out.println("EConTab: RapportiniActivity onContextItemSelected EXIT");
		return super.onContextItemSelected(item);
	}

	public void esportaRapportino(final int idRapportino,final boolean multiplo) {
		System.out.println("EConTab: RapportiniActivity esportaRapportino ENTER");
		AsyncTask task = new AsyncTask() {
			AlertDialog di = null;

			@Override
			protected void onPreExecute() {
				AlertDialog.Builder ab = new AlertDialog.Builder(RapportiniActivity.this);
				ab.setMessage(getString(R.string.messaggio_creazione_report));
				di = ab.create();
				di.show();
			}

			@Override
			protected Object doInBackground(Object[] objects) {
				RapportinoXLS exp = new RapportinoXLS(RapportiniActivity.this);
				try {
					String percorsoFile = exp.generaReportRapportino(idRapportino,multiplo);
					return percorsoFile;
				} catch (Exception e) {
					return "Errore:" + Log.getStackTraceString(e);
				}

			}

			@Override
			protected void onPostExecute(Object o) {
				super.onPostExecute(o);
				di.cancel();
				if (!o.toString().startsWith("Errore")){
					String percorsoFile = o.toString();
					File fileExport = new File(percorsoFile);
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(fileExport), Utility.XLS);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
				}
				else{
					Toast.makeText(RapportiniActivity.this, o.toString(), Toast.LENGTH_LONG)
							.show();
				}
			}
		};
		task.execute();
		System.out.println("EConTab: RapportiniActivity esportaRapportino EXIT");
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		System.out.println("EConTab: RapportiniActivity onItemClick ENTER");
		// TODO Auto-generated method stub

	    ContentValues item = (ContentValues)lista.getItemAtPosition(position);
        Intent intent = new Intent(this,RapportinoDettaglioModActivity.class);
        intent.putExtra("ID", item.getAsInteger(Rapportini.ID_RAPPORTINO));
        apriFinestraModifica(intent, 1);
		System.out.println("EConTab: RapportiniActivity onItemClick EXIT");
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

	public void modifica(ContentValues rapportino){
		System.out.println("EConTab: RapportiniActivity modifica ENTER");
		Intent intent = new Intent(this,RapportinoDettaglioModActivity.class);
		intent.putExtra("ID", rapportino.getAsInteger(Rapportini.ID_RAPPORTINO));
		apriFinestraModifica(intent, 1);
		System.out.println("EConTab: RapportiniActivity modifica EXIT");
	}

}
