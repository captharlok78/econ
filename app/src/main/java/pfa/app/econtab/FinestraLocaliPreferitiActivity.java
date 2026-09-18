package pfa.app.econtab;

import android.content.ContentValues;
import android.content.DialogInterface;
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
import android.widget.GridView;
import android.widget.Toast;

import java.util.ArrayList;

import pfa.app.econtab.adapters.GrigliaLocaliPreferitiAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.utils.Utility;

public class FinestraLocaliPreferitiActivity extends EConTabActivity implements OnItemClickListener {
	private GridView lista = null;
	private ArrayList<Object> dati = null;
	private GrigliaLocaliPreferitiAdapter adapter = null;

	private int idPreventivoSelezionato = 0;
	private int idLocale = 0;
	private int[] elementi = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: FinestraLocaliPreferitiActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
        setVisualizzazionePopup();
		setContentView(R.layout.activity_finestra_locali_preferiti);


		idPreventivoSelezionato = getIntent().getIntExtra(Preventivi.ID_PREVENTIVO, 0);
		idLocale = getIntent().getIntExtra(Locali.ID_LOCALE, 0);
		elementi = getIntent().getIntArrayExtra("ELEMENTI");

		lista = (GridView) findViewById(R.id.griglia_locali);
		lista.setOnItemClickListener(this);
		registerForContextMenu(lista);

		dati = null;
		adapter = null;

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
		System.out.println("EConTab: FinestraLocaliPreferitiActivity onCreate EXIT");
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		ricerca();
	}

	public void ricerca() {
		System.out.println("EConTab: FinestraLocaliPreferitiActivity ricerca ENTER");
		// TODO Auto-generated method stub
		lista.invalidate();
		if (dati == null) {
			dati = new ArrayList<Object>();
			adapter = new GrigliaLocaliPreferitiAdapter(this, dati, R.layout.grid_item_locale_preferito);
			lista.setAdapter(adapter);
			lista.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
		}

		dati.clear();
		String filtro = getTesto(R.id.editText_filtra);
		DbInterno db = new DbInterno(this);

		String SQL = "Select * from " + Locali.NOME_TABELLA + " where " + Locali.PREFERITO_SN + " = 1  and " + Locali.NOME
				+ " like ? order by " + Locali.NOME;
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
		System.out.println("EConTab: FinestraLocaliPreferitiActivity ricerca EXIT");
	}

	public void chiudi(View v) {

		finish();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		System.out.println("EConTab: FinestraLocaliPreferitiActivity onCreateContextMenu ENTER");
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		ContentValues item = (ContentValues) lista.getItemAtPosition(info.position);
		menu.setHeaderTitle(item.getAsString(Locali.NOME));
		menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.importa_locale_con_elementi));
		menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.importa_locale_senza_elementi));
		// menu.add(Menu.NONE, 3, Menu.NONE, getString(R.string.modifica));
		menu.add(Menu.NONE, 4, Menu.NONE, getString(R.string.elimina));
		System.out.println("EConTab: FinestraLocaliPreferitiActivity onCreateContextMenu EXIT");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		System.out.println("EConTab: FinestraLocaliPreferitiActivity onContextItemSelected ENTER");
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		ContentValues val = (ContentValues) lista.getItemAtPosition(info.position);
		if (item.getItemId() == 1) {
			_importaLocale(true, val.getAsInteger(Locali.ID_LOCALE));
		}
		if (item.getItemId() == 2) {
			_importaLocale(false, val.getAsInteger(Locali.ID_LOCALE));
		}
		if (item.getItemId() == 3) {

		}
		if (item.getItemId() == 4) {
			confermaCancellazione(new Locali(), val, true);
		}
		System.out.println("EConTab: FinestraLocaliPreferitiActivity onContextItemSelected EXIT");
		return super.onContextItemSelected(item);
	}

	private void _importaLocale(final boolean conElementi, final int idLocalePreferito) {
		System.out.println("EConTab: FinestraLocaliPreferitiActivity _importaLocale ENTER");
		// TODO Auto-generated method stub
		String messaggio = "";
		if (conElementi) {
			messaggio = getString(R.string.messaggio_importazione_locale_elementi);
		} else {
			messaggio = getString(R.string.messaggio_importazione_locale_forma);
		}
		Utility.mostraConfermaDialog(getString(R.string.attenzione), messaggio, this, getString(R.string.conferma),
				getString(R.string.annulla), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						if (which == DialogInterface.BUTTON_POSITIVE) {
							DbInterno db = new DbInterno(FinestraLocaliPreferitiActivity.this);
							db.getReadableDatabase().beginTransaction();
							try {
								Locali tablocali = new Locali();
								tablocali.importaDaPreferito(db, idLocale, idPreventivoSelezionato, idLocalePreferito, conElementi,
										elementi);
								PreventiviDettaglio tabPrev = new PreventiviDettaglio();
								tabPrev.aggiornaRighePreventivoLocale(db,idPreventivoSelezionato,idLocale);
								db.getReadableDatabase().setTransactionSuccessful();
							} catch (Exception e) {
								Toast.makeText(FinestraLocaliPreferitiActivity.this, Log.getStackTraceString(e), Toast.LENGTH_LONG).show();
							}
							db.getReadableDatabase().endTransaction();
							db.close();
							setResult(RESULT_OK);
							finish();
						}
					}
				});
		System.out.println("EConTab: FinestraLocaliPreferitiActivity _importaLocale EXIT");
	}

	@Override
	protected void aggiornaDopoCancellazione() {
		System.out.println("EConTab: FinestraLocaliPreferitiActivity aggiornaDopoCancellazione");
		// TODO Auto-generated method stub
		ricerca();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
		System.out.println("EConTab: FinestraLocaliPreferitiActivity onItemClick");
		// TODO Auto-generated method stub
		v.showContextMenu();
	}

}
