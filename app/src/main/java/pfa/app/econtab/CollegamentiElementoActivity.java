package pfa.app.econtab;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.util.ArrayList;

import pfa.app.econtab.adapters.CollegamentiElementoAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Aree;
import pfa.app.econtab.db.table.CategorieGenerali;
import pfa.app.econtab.db.table.Collegamenti;
import pfa.app.econtab.db.table.ComponentiCantiere;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Preventivi;

public class CollegamentiElementoActivity extends EConTabActivity implements OnItemClickListener {
	private ListView lista = null;
	private CollegamentiElementoAdapter adapter = null;

	private ArrayList<Object> dati = null;

	private int idElemento = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: CollegamentiElementoActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
        setVisualizzazionePopup();
		setContentView(R.layout.activity_collegamenti_elemento);

		idElemento = getIntent().getIntExtra("ID_ELEMENTO", 0);
		lista = (ListView) findViewById(R.id.lista);
		lista.setOnItemClickListener(this);
		System.out.println("EConTab: CollegamentiElementoActivity onCreate EXIT");
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		ricerca();
	}

	private void ricerca() {
		System.out.println("EConTab: CollegamentiElementoActivity ricerca ENTER");
		// TODO Auto-generated method stub
		lista.invalidate();
		if (dati == null) {
			dati = new ArrayList<Object>();
		}
		dati.clear();

		DbInterno db = new DbInterno(this);

		ArrayList<Object> locali = _caricaLocaliCantiere(db, idElemento);

		ContentValues whereTubi = new ContentValues();
		whereTubi.put(Elementi.ID_CATEGORIA_GENERALE, CategorieGenerali.TUBI);
		ContentValues whereCavi = new ContentValues();
		whereCavi.put(Elementi.ID_CATEGORIA_GENERALE, CategorieGenerali.CAVI);
		ArrayList<Object> tubi = db.eseguiSelect(new Elementi(), whereTubi, null);
		ArrayList<Object> cavi = db.eseguiSelect(new Elementi(), whereCavi, null);

		Join j0 = new Join(Collegamenti.NOME_TABELLA, ElementiCantiere.NOME_TABELLA);
		j0.addCampiDiJoin(Collegamenti.ID_ELEMENTO_CANT1, ElementiCantiere.ID_ELEMENTO_CANT);

		Join j1 = new Join(Collegamenti.NOME_TABELLA, ElementiCantiere.NOME_TABELLA);
		j1.setAliasTabella("ElementiCantiere2");
		j1.addCampiDiJoin(Collegamenti.ID_ELEMENTO_CANT2, ElementiCantiere.ID_ELEMENTO_CANT);

		String filtroTubi = " and " + Collegamenti.ID_TUBO + "<>0";
		String filtroCavi = " and " + Collegamenti.ID_CAVO + "<>0";

		String SQL = "Select " + Collegamenti.NOME_TABELLA + ".*," + ElementiCantiere.NOME_TABELLA + "."
				+ ElementiCantiere.NOME_ELEMENTO_CANT + "," + ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.NUMERO_IDENTIFICATIVO
				+ "," + ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.ID_LOCALE + " as ID_LOCALE_A,ElementiCantiere2."
				+ ElementiCantiere.NOME_ELEMENTO_CANT + " as nome_elemento_cant2,ElementiCantiere2."
				+ ElementiCantiere.NUMERO_IDENTIFICATIVO + " as numero_identificativo_2,ElementiCantiere2." + ElementiCantiere.ID_LOCALE
				+ " as ID_LOCALE_B from " + Collegamenti.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where ("
				+ Collegamenti.ID_ELEMENTO_CANT1 + " = " + idElemento + " or " + Collegamenti.ID_ELEMENTO_CANT2 + " = " + idElemento + ") ";

		dati.addAll(db.eseguiSelect(SQL + filtroTubi, null));
		dati.addAll(db.eseguiSelect(SQL + filtroCavi, null));

		ContentValues whereComp = new ContentValues();
		ComponentiCantiere tabComp = new ComponentiCantiere();
		ContentValues recComp = null;
		for (int i = 0; i < dati.size(); i++) {
			ContentValues curr = (ContentValues) dati.get(i);
			// prendo i dati accessori (locale, componente ecc)
			_aggiungiTuboCavo(curr, tubi, cavi);
			_aggiungiLocali(curr, locali);

			curr.put("COMPONENTE_A", "");
			curr.put("COMPONENTE_B", "");
			int idComponente1 = curr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT1);
			if (idComponente1 != 0) {
				whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponente1);
				recComp = db.getRecord(tabComp, whereComp);
				if (recComp != null) {
					curr.put("COMPONENTE_A", recComp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
				}
			}

			int idComponente2 = curr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT2);
			if (idComponente2 != 0) {
				whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponente2);
				recComp = db.getRecord(tabComp, whereComp);
				if (recComp != null) {
					curr.put("COMPONENTE_B", recComp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
				}
			}
		}
		db.close();

		if (adapter == null) {
			adapter = new CollegamentiElementoAdapter(this, dati, R.layout.list_item_collegamento);
			lista.setAdapter(adapter);
			lista.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
			registerForContextMenu(lista);
		} else {
			adapter.notifyDataSetChanged();

		}
		System.out.println("EConTab: CollegamentiElementoActivity ricerca EXIT");
	}

	private void _aggiungiLocali(ContentValues curr, ArrayList<Object> locali) {
		System.out.println("EConTab: CollegamentiElementoActivity _aggiungiLocali ENTER");
		// TODO Auto-generated method stub
		Aree tabAree = new Aree();
		int idLocaleA = curr.getAsInteger("ID_LOCALE_A");
		if (idLocaleA != 0) {
			for (int i = 0; i < locali.size(); i++) {
				ContentValues locale = (ContentValues) locali.get(i);
				if (locale.getAsInteger(Locali.ID_LOCALE) == idLocaleA) {
					curr.put("LOCALE_A", locale.getAsString(tabAree.getAlias(Aree.NOME)) + " - " + locale.getAsString(Locali.NOME));
					break;
				}
			}
		}

		int idLocaleB = curr.getAsInteger("ID_LOCALE_B");
		if (idLocaleB != 0) {
			for (int i = 0; i < locali.size(); i++) {
				ContentValues locale = (ContentValues) locali.get(i);
				if (locale.getAsInteger(Locali.ID_LOCALE) == idLocaleB) {
					curr.put("LOCALE_B", locale.getAsString(tabAree.getAlias(Aree.NOME)) + " - " + locale.getAsString(Locali.NOME));
					break;
				}
			}
		}
		System.out.println("EConTab: CollegamentiElementoActivity _aggiungiLocali EXIT");
	}

	private ArrayList<Object> _caricaLocaliCantiere(DbInterno db, int idElemento) {
		// TODO Auto-generated method stub
		ArrayList<Object> locali = new ArrayList<Object>();
		ElementiCantiere tabElem = new ElementiCantiere();
		ContentValues where = new ContentValues();
		where.put(ElementiCantiere.ID_ELEMENTO_CANT, idElemento);
		ContentValues valElem = db.getRecord(tabElem, where);
		if (valElem != null) {
			int idCantiere = valElem.getAsInteger(ElementiCantiere.ID_CANTIERE);

			if (idCantiere == 0) {
				// prendo il cantiere dal preventivo
				int idOrdine = valElem.getAsInteger(ElementiCantiere.ID_PREVENTIVO);
				Preventivi tabPrev = new Preventivi();
				ContentValues whereOrdine = new ContentValues();
				whereOrdine.put(Preventivi.ID_PREVENTIVO, idOrdine);
				ContentValues valOrdine = db.getRecord(tabPrev, whereOrdine);
				if (valOrdine != null) {
					idCantiere = valOrdine.getAsInteger(Preventivi.ID_CANTIERE);
				}
			}
			Locali tabLocali = new Locali();
			Aree tabAree = new Aree();
			locali = tabLocali.getLocaliCantiereConArea(db, idCantiere);

		}

		return locali;
	}

	private void _aggiungiTuboCavo(ContentValues curr, ArrayList<Object> tubi, ArrayList<Object> cavi) {
		System.out.println("EConTab: CollegamentiElementoActivity _aggiungiTuboCavo ENTER");
		// TODO Auto-generated method stub
		int idtubo = curr.getAsInteger(Collegamenti.ID_TUBO);
		if (idtubo != 0) {
			for (int i = 0; i < tubi.size(); i++) {
				ContentValues tubo = (ContentValues) tubi.get(i);
				if (tubo.getAsInteger(Elementi.ID_ELEMENTO) == idtubo) {
					curr.put("NOME", tubo.getAsString(Elementi.NOME_ELEMENTO));
					break;
				}
			}
		}

		int idcavo = curr.getAsInteger(Collegamenti.ID_CAVO);
		if (idcavo != 0) {
			for (int i = 0; i < cavi.size(); i++) {
				ContentValues cavo = (ContentValues) cavi.get(i);
				if (cavo.getAsInteger(Elementi.ID_ELEMENTO) == idcavo) {
					curr.put("NOME", cavo.getAsString(Elementi.NOME_ELEMENTO));
					break;
				}
			}
		}
		System.out.println("EConTab: CollegamentiElementoActivity _aggiungiTuboCavo EXIT");
	}

	public void chiudi(View v) {
		finish();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		System.out.println("EConTab: CollegamentiElementoActivity onItemClick");
		// TODO Auto-generated method stub
		Intent intent = new Intent(this, CollegamentoActivity.class);
		ContentValues item = (ContentValues) lista.getItemAtPosition(position);
		intent.putExtra(Collegamenti.ID_COLLEGAMENTO, item.getAsInteger(Collegamenti.ID_COLLEGAMENTO));
		intent.putExtra(Collegamenti.ID_ORDINE, item.getAsInteger(Collegamenti.ID_ORDINE));
		startActivity(intent);
	}

}
