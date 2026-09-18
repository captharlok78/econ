package pfa.app.econtab;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import pfa.app.econtab.adapters.ComposizioneAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Aree;
import pfa.app.econtab.db.table.CategorieComponenti;
import pfa.app.econtab.db.table.CategorieGenerali;
import pfa.app.econtab.db.table.Collegamenti;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.db.table.ComponentiCantiere;
import pfa.app.econtab.db.table.Composizioni;
import pfa.app.econtab.db.table.ComposizioniCantiere;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.Relazioni;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabComponenteScatola;

public class ComposizioneActivity extends EConTabActivity implements OnItemClickListener, OnClickListener {

	private LinearLayout scatola = null;
	private TextView aggiungiScatola = null;
	private int idElemento = 0;
	private int idElementoCantiere = 0;

	private ListView lista = null;

	private ArrayList<Object> dati = null;
	private ComposizioneAdapter adapter = null;

	private ContentValues componenteSelezionato = null;
	private ImageView iconaElemento = null;

	private int postiOspitati = 0;
	private int idPreventivo = 0;
	private boolean modificheBloccate = false;
	private boolean mostraRelazioni = false;
	private int idCategoria = 0;

	private HashMap<Integer, ArrayList<Object>> mappaCollegamenti = null;
    private HashMap<Integer, ArrayList<Object>> mappaRelazioni = null;
	private boolean elementoAltroOrdine=false;
	private int idPreventivoOrigine = 0;
	private int idLocale = 0;
	private int idPreventivoSelezionato = 0;
	private boolean aggiornaPreventivo=false;
	private float metri_cavo_cant = 0;
	private int id_elemento_cavo = 0;
	private float metri_tubo_cant = 0;
	private int id_elemento_tubo = 0;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: ComposizioneActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setVisualizzazionePopup();
		setContentView(R.layout.activity_composizione);

		idElemento = getIntent().getIntExtra(Elementi.ID_ELEMENTO, 0);
		System.out.println("EConTab: ComposizioneActivity onCreate idElemento " + idElemento);
		elementoAltroOrdine = getIntent().getBooleanExtra("ELEMENTO_ALTRO_ORDINE", false);
		if (idElemento == 0) {
			idElementoCantiere = getIntent().getIntExtra(ElementiCantiere.ID_ELEMENTO_CANT, 0);
			System.out.println("EConTab: ComposizioneActivity onCreate idElementoCantiere " + idElementoCantiere);
			DbInterno db = new DbInterno(this);
			ContentValues where = new ContentValues();
			where.put(ElementiCantiere.ID_ELEMENTO_CANT,idElementoCantiere);
			ContentValues elementoCant =  db.getRecord(new ElementiCantiere(), where);
			db.close();
			if (elementoCant!=null){
				idLocale = elementoCant.getAsInteger(ElementiCantiere.ID_LOCALE);
				metri_cavo_cant = elementoCant.getAsFloat(ElementiCantiere.METRI_CAVO_CANT);
				id_elemento_cavo = elementoCant.getAsInteger(ElementiCantiere.ID_ELEMENTO_CAVO);
				metri_tubo_cant = elementoCant.getAsFloat(ElementiCantiere.METRI_TUBO_CANT);
				id_elemento_tubo = elementoCant.getAsInteger(ElementiCantiere.ID_ELEMENTO_TUBO);
				System.out.println("EConTab: ComposizioneActivity onCreate metri_cavo_cant  " + metri_cavo_cant);
				System.out.println("EConTab: ComposizioneActivity onCreate id_elemento_cavo " + id_elemento_cavo);
				System.out.println("EConTab: ComposizioneActivity onCreate metri_tubo_cant  " + metri_tubo_cant);
				System.out.println("EConTab: ComposizioneActivity onCreate id_elemento_tubo " + id_elemento_tubo);
			}
		} else {
			findViewById(R.id.linearCampi).setVisibility(View.GONE);
		}

		iconaElemento = (ImageView) findViewById(R.id.imageView_iconaElemento);
		scatola = (LinearLayout) findViewById(R.id.linear_scatola);
		aggiungiScatola = (TextView) findViewById(R.id.aggiungi_scatola);
		lista = (ListView) findViewById(R.id.listView_componenti);
		lista.setOnItemClickListener(this);
		registerForContextMenu(lista);
		idPreventivoOrigine = getIntent().getIntExtra("id_preventivo_origine",0);
		idPreventivoSelezionato = getIntent().getIntExtra("ID_PREVENTIVO_SELEZIONATO",0);
		String preventivoOrigine = "";
		if (idPreventivoOrigine!=0){
			DbInterno db = new DbInterno(this);
			ContentValues where = new ContentValues();
			where.put(Preventivi.ID_PREVENTIVO,idPreventivoOrigine);
			ContentValues recPrevOr =  db.getRecord(new Preventivi(), where);
			db.close();
			if (recPrevOr!=null){
				preventivoOrigine = "(Ordine di partenza n. " + recPrevOr.getAsString(Preventivi.NUMERO)+")";
				setText(R.id.textView_sottotitolo,preventivoOrigine);
				findViewById(R.id.textView_sottotitolo).setVisibility(View.VISIBLE);
			}
		}

		setText(R.id.textView_titolo, getIntent().getStringExtra(Elementi.NOME_ELEMENTO));
		mappaCollegamenti = new HashMap<Integer, ArrayList<Object>>();
        mappaRelazioni = new HashMap<Integer, ArrayList<Object>>();
		System.out.println("EConTab: ComposizioneActivity onCreate EXIT");
	}

	@Override
	protected void onResume() {
		System.out.println("EConTab: ComposizioneActivity onResume ENTER");
		// TODO Auto-generated method stub
		super.onResume();
		if (idElementoCantiere != 0) {
			EConTabAsyncTask taskRicerca = new EConTabAsyncTask();
			// taskRicerca.setMostraAttesa(true);
			taskRicerca.execute();
		} else {
			ricerca();
		}
		System.out.println("EConTab: ComposizioneActivity onResume EXIT");
	}

	@Override
	protected void esecuzioneAsincrona() {
		// TODO Auto-generated method stub
		super.esecuzioneAsincrona();
		ricerca();
	}

	public void ricerca() {
		System.out.println("EConTab: ComposizioneActivity ricerca ENTER");
		// TODO Auto-generated method stub

		if(pfa.app.econtab.Globals.EXTRA_CAVI_TUBI_IN_PREVENTIVO == true) {
			// DL: change
			LinearLayout linearLayoutMetriCavo = findViewById(R.id.linearMetriCavo);
			if(linearLayoutMetriCavo != null)
			{
				linearLayoutMetriCavo.setVisibility(View.VISIBLE);
			}
			LinearLayout linearMetriMetriTubo = findViewById(R.id.linearMetriTubo);
			if(linearMetriMetriTubo != null)
			{
				linearMetriMetriTubo.setVisibility(View.VISIBLE);
			}
		}

		ArrayList<Object> componenti = new ArrayList<Object>();
		mappaCollegamenti.clear();
        mappaRelazioni.clear();
		DbInterno db = new DbInterno(this);
		int idElementoPerIcona = 0;
		if (idElemento != 0) {
			idElementoPerIcona = idElemento;
			Join j0 = new Join(Composizioni.NOME_TABELLA, Componenti.NOME_TABELLA);
			j0.addCampiDiJoin(Composizioni.ID_COMPONENTE, Componenti.ID_COMPONENTE);

			Join j1 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
			j1.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);
			Componenti tabComp = new Componenti();

			String SQL_COMP = "Select " + Composizioni.ID_ELEMENTO + "," + Composizioni.ID_COMPOSIZIONE + ","
					+ Composizioni.POSIZIONE_INIZIALE + "," + Composizioni.MODULI_OCCUPATI + "," + tabComp.getNomeCampoTabella("*") + ","
					+ CategorieComponenti.TIPO + "  from " + Composizioni.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where "
					+ Composizioni.ID_ELEMENTO + "=" + idElemento + " order by  " + Composizioni.POSIZIONE_INIZIALE + ","
					+ tabComp.getNomeCampoTabella(Componenti.ID_CATEGORIA_COMPONENTE);
			System.out.println("EConTab: ComposizioneActivity ricerca SQL_COMP " + SQL_COMP);
			componenti = db.eseguiSelect(SQL_COMP, null);
		}
		if (idElementoCantiere != 0) {
			ContentValues where_elem = new ContentValues();
			where_elem.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
			ContentValues valElem = db.getRecord(new ElementiCantiere(), where_elem);
			if (valElem != null) {
				setText(R.id.editText_altezza, "" + valElem.getAsInteger(ElementiCantiere.ALTEZZA_DA_TERRA));
				setText(R.id.editText_etichetta, "" + valElem.getAsInteger(ElementiCantiere.NUMERO_IDENTIFICATIVO));
				if(pfa.app.econtab.Globals.EXTRA_CAVI_TUBI_IN_PREVENTIVO == true) {
					// DL: change
					setText(R.id.editText_MetriCavo, "" + valElem.getAsFloat(ElementiCantiere.METRI_CAVO_CANT));
					setText(R.id.editText_MetriTubo, "" + valElem.getAsFloat(ElementiCantiere.METRI_TUBO_CANT));
				}
				idElementoPerIcona = valElem.getAsInteger(ElementiCantiere.ID_ELEMENTO);
				idPreventivo = valElem.getAsInteger(ElementiCantiere.ID_PREVENTIVO);
				if (idPreventivo != 0) {
					ContentValues wherePrev = new ContentValues();
					wherePrev.put(Preventivi.ID_PREVENTIVO, idPreventivo);
					ContentValues recPrev = db.getRecord(new Preventivi(), wherePrev);
					if (recPrev != null) {
						if (!recPrev.getAsString(Preventivi.STATO).equals(Preventivi.STATO_APERTO) ) {
							modificheBloccate = true;
						}
						if (recPrev.getAsString(Preventivi.TIPO).equals(Preventivi.TIPO_ORDINE)) {
							mostraRelazioni = true;
						}
					}
				} else {
					mostraRelazioni = true;
				}
			}

			if (elementoAltroOrdine){
				modificheBloccate = true;
			}

			Join j0 = new Join(ComposizioniCantiere.NOME_TABELLA, ComponentiCantiere.NOME_TABELLA);
			j0.addCampiDiJoin(ComposizioniCantiere.ID_COMPONENTE_CANT, ComponentiCantiere.ID_COMPONENTE_CANT);

			Join j1 = new Join(ComponentiCantiere.NOME_TABELLA, Componenti.NOME_TABELLA);
			j1.addCampiDiJoin(ComponentiCantiere.ID_COMPONENTE, Componenti.ID_COMPONENTE);

			Join j2 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
			j2.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);
			ComponentiCantiere tabCompCant = new ComponentiCantiere();
			Componenti tabComp = new Componenti();

			String SQL_COMP = "Select " + ComposizioniCantiere.ID_ELEMENTO_CANT + "," + ComposizioniCantiere.POSIZIONE_INIZIALE + ","
					+ ComposizioniCantiere.MODULI_OCCUPATI_CANT + "," + tabCompCant.getNomeCampoTabella("*") + ","
					+ tabComp.getNomeCampoTabella(Componenti.ICONA) + "," + tabComp.getNomeCampoTabella(Componenti.ID_CATEGORIA_COMPONENTE)
					+ "," + CategorieComponenti.TIPO + "  from " + ComposizioniCantiere.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin()
					+ j2.getSQLJoin() + " where " + ComposizioniCantiere.ID_ELEMENTO_CANT + "=" + idElementoCantiere + " order by  "
					+ ComposizioniCantiere.POSIZIONE_INIZIALE + "," + tabComp.getNomeCampoTabella(Componenti.ID_CATEGORIA_COMPONENTE);
			System.out.println("EConTab: ComposizioneActivity ricerca SQL_COMP " + SQL_COMP);
			componenti = db.eseguiSelect(SQL_COMP, null);

			_caricaCollegamenti(db, componenti);
            _caricaRelazioniLogiche(db,componenti);

		}

		if (idElementoPerIcona != 0) {
			ContentValues where_elem = new ContentValues();
			where_elem.put(Elementi.ID_ELEMENTO, idElementoPerIcona);
			ContentValues valElem = db.getRecord(new Elementi(), where_elem);
			if (valElem != null) {
				idCategoria = valElem.getAsInteger(Elementi.ID_CATEGORIA_GENERALE);
                iconaElemento.setImageBitmap(Utility.getIcona(this, Elementi.PATH_ICONE, valElem.getAsString(Elementi.ICONA)));
                if (idElementoCantiere!=0){
                    iconaElemento.setVisibility(View.GONE);//nsacondo l'icona nella composizione cantiere
                }
			}
		}

		db.close();

		System.out.println("EConTab: ComposizioneActivity ricerca componenti.size() " + componenti.size());

		ContentValues componenteScatola = null;
		ContentValues componentePortaFrutti = null;
		ContentValues componenteCopriScatola = null;
		ArrayList<ContentValues> frutti = new ArrayList<ContentValues>();
		for (int i = 0; i < componenti.size(); i++) {
			ContentValues comp = (ContentValues) componenti.get(i);

			System.out.println("EConTab: ComposizioneActivity ricerca comp.getAsString(CategorieComponenti.TIPO) " + comp.getAsString(CategorieComponenti.TIPO));

			//escludo gli eventuali componenti liberi
			if (isComponenteAggiuntivoScatola(comp)){
				continue;
			}
			if (comp.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.SCATOLE) {
				System.out.println("EConTab: ComposizioneActivity ricerca IS Componenti.SCATOLE");
				componenteScatola = comp;
			}
			if (comp.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.PORTAFRUTTI) {
				System.out.println("EConTab: ComposizioneActivity ricerca IS Componenti.PORTAFRUTTI");
				componentePortaFrutti = comp;
			}
			if (comp.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.COPRISCATOLA) {
				System.out.println("EConTab: ComposizioneActivity ricerca IS Componenti.COPRISCATOLA");
				componenteCopriScatola = comp;
			}

			if (comp.getAsString(CategorieComponenti.TIPO).equals(CategorieComponenti.TIPO_FRUTTO)) {
				frutti.add(comp);
			}
		}

		if (componenteScatola != null) {
			/*if (componentePortaFrutti == null && frutti.size() > 0) {
				ContentValues cvpf = new ContentValues();
				cvpf.put(CategorieComponenti.TIPO, CategorieComponenti.TIPO_PORTAFRUTTI);
				cvpf.put(Composizioni.MODULI_OCCUPATI, "");
				cvpf.put(Componenti.NOME_COMPONENTE, "-");
				cvpf.put(Componenti.ICONA, "");
				cvpf.put(Componenti.ID_CATEGORIA_COMPONENTE, Componenti.PORTAFRUTTI);
				componenti.add(1, cvpf);
			}*/

			if (componenteCopriScatola == null && frutti.size() == 0) {
				ContentValues cvcs = new ContentValues();
				cvcs.put(CategorieComponenti.TIPO, CategorieComponenti.TIPO_COPRISCATOLA);
				cvcs.put(Composizioni.MODULI_OCCUPATI, "");
				cvcs.put(Componenti.NOME_COMPONENTE, "-");
				cvcs.put(Componenti.ICONA, "");
				cvcs.put(Componenti.ID_CATEGORIA_COMPONENTE, Componenti.COPRISCATOLA);
				componenteCopriScatola = cvcs;
				componenti.add(1, cvcs);
			}
		}
		if (componenteScatola != null) {
			try {
				disegnaScatola(componenteScatola, frutti, componenteCopriScatola);
			} catch (Exception e) {
				// Se va in errore il disegnaScatola significa che c'� qualcosa
				// che non va a livello di db e quindi ripristino la situazione
				// di partenza
				scatola.removeAllViews();
				aggiungiScatola.setVisibility(View.VISIBLE);
				scatola.addView(aggiungiScatola);
				Toast.makeText(this, "Errore nella visualizzazione della scatola", Toast.LENGTH_SHORT).show();
				DbInterno dbEx = new DbInterno(this);

				ComposizioniCantiere tabComposizioniCantiere = new ComposizioniCantiere();
				Composizioni tabComposizioni = new Composizioni();

				if (idElemento != 0) {
					ContentValues where = new ContentValues();
					where.put(Composizioni.ID_ELEMENTO, idElemento);
					tabComposizioni.cancellaRecord(dbEx, where, false);
				}

				if (idElementoCantiere != 0) {
					ContentValues where = new ContentValues();
					where.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
					tabComposizioniCantiere.cancellaRecord(dbEx, where, false);
				}

				dbEx.close();
				componenti.clear();

			}

		}

		lista.invalidate();
		if (dati == null) {
			dati = new ArrayList<Object>();
		}
		dati.clear();

		dati.addAll(componenti);

		if (idElementoCantiere != 0) {
			// aggiungo la riga per aggiungere un nuovo componente
			ContentValues valVuoto = new ContentValues();
			valVuoto.put(CategorieComponenti.TIPO, "");
			valVuoto.put(Composizioni.MODULI_OCCUPATI, "");
			valVuoto.put(Componenti.ICONA, "");
			valVuoto.put(Componenti.ID_CATEGORIA_COMPONENTE, 0);
			valVuoto.put(ComponentiCantiere.NOME_COMPONENTE_CANT, "+ " + getString(R.string.aggiungi_componente));
			dati.add(valVuoto);
		}

		if (adapter == null) {
			adapter = new ComposizioneAdapter(this, dati, R.layout.list_item_composizione_scatola);
			adapter.setModificheBloccate(modificheBloccate);
			if (idElementoCantiere != 0) {
				adapter.setComposizioneCantiere(true);
				adapter.setMostraRelazioni(mostraRelazioni);
				adapter.setCollegamenti(mappaCollegamenti);
                adapter.setRelazioni(mappaRelazioni);
				adapter.setIdpreventivo(idPreventivoSelezionato);
				adapter.setIdLocale(idLocale);


			}

			lista.setAdapter(adapter);
			lista.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);

		} else {
			adapter.notifyDataSetChanged();

		}

		if (idElementoCantiere!=0 && aggiornaPreventivo){
			aggiornaPreventivo = false;
			DbInterno dbAggPrev = new DbInterno(this);
			PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();
			tabPrevDett.aggiornaRighePreventivoLocale(dbAggPrev,idPreventivoSelezionato,idLocale);
			dbAggPrev.close();
		}

		System.out.println("EConTab: ComposizioneActivity ricerca EXIT");
	}



    private void disegnaScatola(ContentValues comp, ArrayList<ContentValues> frutti, ContentValues copriscatola) {
		System.out.println("EConTab: ComposizioneActivity disegnaScatola ENTER");
		// TODO Auto-generated method stub
		scatola.removeAllViews();
		aggiungiScatola.setVisibility(View.GONE);
		scatola.addView(aggiungiScatola);

		if (idElemento != 0) {
			postiOspitati = comp.getAsInteger(Componenti.SPAZI_OSPITATI);

		}
		if (idElementoCantiere != 0) {
			postiOspitati = comp.getAsInteger(ComponentiCantiere.SPAZI_OSPITATI_CANT);
		}

		if (copriscatola != null) {
			EConTabComponenteScatola componente = new EConTabComponenteScatola(this);

            //i copriscatola li faccio sempre da 3 per fare le immagini fatte bene
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(
					R.dimen.altezza_scatola)
					* 3/2, getResources().getDimensionPixelSize(R.dimen.altezza_scatola));
			//params.setMargins(1, 1, 1, 1);
			componente.setLayoutParams(params);

			if (idElemento != 0) {
				componente.setTesto(copriscatola.getAsString(Componenti.NOME_COMPONENTE));
			}
			if (idElementoCantiere != 0) {
				componente.setTesto(copriscatola.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
			}
			componente.setIcona(copriscatola.getAsString(Componenti.ICONA));
			componente.setItem(copriscatola);
			componente.setOnClickListener(this);
			registerForContextMenu(componente);
			scatola.addView(componente);

		}

		else {
			for (int i = 0; i < frutti.size(); i++) {
				EConTabComponenteScatola componente = new EConTabComponenteScatola(this);
				int postiOccupati = 0;
				if (idElemento != 0) {
					postiOccupati = frutti.get(i).getAsInteger(Componenti.SPAZI_OCCUPATI);
				}
				if (idElementoCantiere != 0) {
					postiOccupati = frutti.get(i).getAsInteger(ComponentiCantiere.SPAZI_OCCUPATI_CANT);
				}

				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(
						R.dimen.altezza_scatola)
						* postiOccupati/2, getResources().getDimensionPixelSize(R.dimen.altezza_scatola));
				//params.setMargins(1, 1, 1, 1);
				componente.setLayoutParams(params);

				if (idElemento != 0) {
					componente.setTesto(frutti.get(i).getAsString(Composizioni.MODULI_OCCUPATI));
				}
				if (idElementoCantiere != 0) {
					componente.setTesto(frutti.get(i).getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT));
				}
				componente.setIcona(frutti.get(i).getAsString(Componenti.ICONA));
				componente.setItem(frutti.get(i));
				componente.setOnClickListener(this);
				registerForContextMenu(componente);
				scatola.addView(componente);

				// salto al prossimo posto libero se il componente occupa pi� di un posto
				// i = i + postiOccupati - 1;

			}
		}

        if (idElemento!=0){
            try {

                _creaImmagineScatola();

            } catch (Exception e) {
               // Toast.makeText(this, Log.getStackTraceString(e),Toast.LENGTH_LONG).show();
            }
        }

		System.out.println("EConTab: ComposizioneActivity disegnaScatola EXIT");

	}

    private void _creaImmagineScatola() {
		System.out.println("EConTab: ComposizioneActivity _creaImmagineScatola ENTER");
        scatola.postDelayed(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub


                //elimino la vecchia immagine prima di salvare la nuova

                DbInterno db = new DbInterno(ComposizioneActivity.this);
                ContentValues where = new ContentValues();
                where.put(Elementi.ID_ELEMENTO, idElemento);
                Elementi elementi = new Elementi();
                ContentValues valElem =  db.getRecord(elementi,where);
                if (valElem !=null){
                    String oldIcona = valElem.getAsString(Elementi.ICONA);
                    //cambio l'icona solo se non è stata inserita manualmente
                    if (oldIcona.startsWith("ICO_C_") || oldIcona.equals("")) {
                        Bitmap btm = Utility.getBitmapFromView(scatola);
                        String nomeIcona = "ICO_C_" + System.currentTimeMillis() + ".png";
                        Utility.salvaIcona(ComposizioneActivity.this, Elementi.PATH_ICONE, nomeIcona, btm, false,db);
                        File dirApp = getDir(Elementi.PATH_ICONE, Context.MODE_PRIVATE);
                        File fileIco = new File(dirApp, oldIcona);
                        if (fileIco.exists()) {
                            fileIco.delete();
                        }
                        ContentValues val = elementi.getValoriLogModifica(db);
                        val.put(Elementi.ICONA, nomeIcona);
                        elementi.aggiornaRecord(db, val, where);
                        iconaElemento.setImageBitmap(Utility.getIcona(ComposizioneActivity.this, Elementi.PATH_ICONE, nomeIcona));
                    }
                }
                db.close();


            }
        }, 400);

		System.out.println("EConTab: ComposizioneActivity _creaImmagineScatola EXIT");

    }

    public void chiudi(View v) {

		finish();
	}

	public void selezionaScatola(View v) {
		_selezionaScatola(false,-1);

	}

	private void _selezionaScatola(boolean modifica,int numSpazi) {
		System.out.println("EConTab: ComposizioneActivity _selezionaScatola ENTER");
		Intent intent = new Intent(this, FinestraComponentiActivity.class);
		intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE, Componenti.SCATOLE);
		if (idElemento != 0) {
			intent.putExtra(Elementi.ID_ELEMENTO, idElemento);
		}
		if (idElementoCantiere != 0) {
			intent.putExtra(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
			if (componenteSelezionato!=null){
				intent.putExtra("NUM_SPAZI_ORIGINE",componenteSelezionato.getAsInteger(ComponentiCantiere.SPAZI_OSPITATI_CANT));
			}
		}

		intent.putExtra("MODIFICA", modifica);
		if (numSpazi>0){
			intent.putExtra("NUM_SPAZI", numSpazi);
		}

		System.out.println("EConTab: ComposizioneActivity _selezionaScatola EXIT");
		startActivity(intent);
	}

	private void _selezionaFrutto(int posizioneIniziale) {
		System.out.println("EConTab: ComposizioneActivity _selezionaFrutto ENTER");
		Intent intent = new Intent(this, FinestraComponentiActivity.class);
		intent.putExtra("FRUTTI", true);
		if (idElemento != 0) {
			intent.putExtra(Elementi.ID_ELEMENTO, idElemento);
			intent.putExtra(Composizioni.POSIZIONE_INIZIALE, posizioneIniziale);
		}
		if (idElementoCantiere != 0) {
			intent.putExtra(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
			intent.putExtra(ComposizioniCantiere.POSIZIONE_INIZIALE, posizioneIniziale);
		}
		System.out.println("EConTab: ComposizioneActivity _selezionaFrutto EXIT");
		startActivity(intent);
	}

	private void _eliminaFrutto(ContentValues frutto) {
		System.out.println("EConTab: ComposizioneActivity _eliminaFrutto ENTER");
		// TODO Auto-generated method stub
		DbInterno db = new DbInterno(this);
		if (idElemento != 0) {
			Composizioni tabComp = new Composizioni();
			tabComp.eliminaFrutto(db, frutto, idElemento);
		}

		if (idElementoCantiere != 0) {
			if (mappaCollegamenti.containsKey(frutto.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT)) || mappaRelazioni.containsKey(frutto.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT))) {
				Toast.makeText(this, getString(R.string.messaggio_eliminazione_frutto_relazioni), Toast.LENGTH_SHORT).show();
			} else {
				ComposizioniCantiere tabComp = new ComposizioniCantiere();
				tabComp.eliminaFrutto(db, frutto, idElementoCantiere);


			}
		}
		System.out.println("EConTab: ComposizioneActivity _eliminaFrutto EXIT");
		db.close();
		ricerca();
	}

	private void _eliminaPortafrutti(ContentValues portafrutti) {
		// TODO Auto-generated method stub
		DbInterno db = new DbInterno(this);
		if (idElemento != 0) {
			Composizioni tabComp = new Composizioni();
			tabComp.eliminaPortafrutti(db, portafrutti, idElemento);
		}

		if (idElementoCantiere != 0) {
			if (mappaCollegamenti.containsKey(portafrutti.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT)) || mappaRelazioni.containsKey(portafrutti.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT))) {
				Toast.makeText(this, getString(R.string.messaggio_eliminazione_frutto_relazioni), Toast.LENGTH_SHORT).show();
			} else {
				ComposizioniCantiere tabComp = new ComposizioniCantiere();
				tabComp.eliminaPortafrutti(db, portafrutti, idElementoCantiere);


			}
		}

		db.close();
		ricerca();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		System.out.println("EConTab: ComposizioneActivity onCreateContextMenu ENTER");

		super.onCreateContextMenu(menu, v, menuInfo);

		componenteSelezionato = null;

		ContentValues item = null;
		if (v instanceof EConTabComponenteScatola) {
			System.out.println("EConTab: ComposizioneActivity onCreateContextMenu EConTabComponenteScatola");
			item = ((EConTabComponenteScatola) v).getItem();

		} else {
			System.out.println("EConTab: ComposizioneActivity onCreateContextMenu ContentValues");
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			item = (ContentValues) lista.getItemAtPosition(info.position);
		}

		//se sto modificando un elemento di un ordine e ho selezionato un altro ordine allora mostro il messaggio
		if (idElementoCantiere!=0 && item!=null && item.containsKey(ComponentiCantiere.ID_PREVENTIVO)){
			if (item.getAsInteger(ComponentiCantiere.ID_PREVENTIVO)!=0 && idPreventivoSelezionato!=item.getAsInteger(ComponentiCantiere.ID_PREVENTIVO)){
				Utility.mostraDialog(getString(R.string.attenzione),"L'ordine attualmente selezionato non corrisponde al numero ordine del componente",this,"OK");
				return;
			}
		}

		componenteSelezionato = item;
		if (item.getAsString(CategorieComponenti.TIPO).equals(CategorieComponenti.TIPO_FRUTTO)) {
			System.out.println("EConTab: ComposizioneActivity onCreateContextMenu idElemento " + idElemento);
			System.out.println("EConTab: ComposizioneActivity onCreateContextMenu idElementoCantiere " + idElementoCantiere);
			if (idElemento != 0) {
				menu.setHeaderTitle(item.getAsString(Composizioni.MODULI_OCCUPATI) + " - " + item.getAsString(Componenti.NOME_COMPONENTE));
			}
			if (idElementoCantiere != 0) {
				menu.setHeaderTitle(item.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT) + " - "
						+ item.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
			}

		} else {
			String local_title = CategorieComponenti.getDescrizioneTipo(getResources(), item.getAsString(CategorieComponenti.TIPO));
			System.out.println("EConTab: ComposizioneActivity onCreateContextMenu local_title " + local_title);
			menu.setHeaderTitle(local_title);
		}
		if (item.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == 0) {
			if (controllaBloccoModifiche()) {
				aggiornaPreventivo = true;
				Intent intent = new Intent(this, FinestraComponentiActivity.class);
				intent.putExtra("COMPOSIZIONELIBERA", true);
				intent.putExtra("COMPONENTE_AGGIUNTIVO_SCATOLA", true);
				// intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE, Componenti.COMPONENTI);
				if (idElemento != 0) {
					intent.putExtra(Elementi.ID_ELEMENTO, idElemento);
				}

				if (idElementoCantiere != 0) {
					intent.putExtra(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
				}
				startActivity(intent);
			}
		}
		else {
			//se sono in una scatola e ho aggiunto un componente libero allora mostro le opzioni per il
			//componente libero
			if (isComponenteAggiuntivoScatola(item)){
				menu.add(Menu.NONE, 11, Menu.NONE, getString(R.string.elimina));
				menu.add(Menu.NONE, 12, Menu.NONE, getString(R.string.modifica));
				if (idElementoCantiere!=0){
					menu.add(Menu.NONE, 10, Menu.NONE, getString(R.string.nota));
				}

			}
			else{
				if (item.getAsString(CategorieComponenti.TIPO).equals(CategorieComponenti.TIPO_FRUTTO)) {
					if (idElemento != 0) {
						if (item.getAsInteger(Componenti.TAPPO_SN) == 1) {
							menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.sostituisci_frutto));
							menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.modifica_frutto));

							// _selezionaFrutto(componenteSelezionato.getAsInteger(Composizioni.POSIZIONE_INIZIALE));

						} else {
							menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.sostituisci_frutto));
							menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.modifica_frutto));
							menu.add(Menu.NONE, 3, Menu.NONE, getString(R.string.elimina_frutto));
						}
					}
					if (idElementoCantiere != 0) {
						if (item.getAsInteger(ComponentiCantiere.TAPPO_SN_CANT) == 1) {

							menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.sostituisci_frutto));
							menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.modifica_frutto));
							// _selezionaFrutto(componenteSelezionato.getAsInteger(ComposizioniCantiere.POSIZIONE_INIZIALE));

						} else {
							menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.sostituisci_frutto));
							menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.modifica_frutto));
							menu.add(Menu.NONE, 3, Menu.NONE, getString(R.string.elimina_frutto));
						}
					}

				}

				if (item.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.SCATOLE) {
					menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.cambia_scatola));
					menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.modifica_scatola));
				}

				if (item.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.PORTAFRUTTI) {
					menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.cambia_portafrutti));
					menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.modifica_portafrutti));
					menu.add(Menu.NONE, 3, Menu.NONE, getString(R.string.sostituisci_con_copriscatola));
					menu.add(Menu.NONE, 4, Menu.NONE, getString(R.string.elimina));

				}
				if (item.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.COPRISCATOLA) {
					menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.cambia_copriscatola));
					menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.modifica_copriscatola));
					menu.add(Menu.NONE, 3, Menu.NONE, getString(R.string.sostituisci_con_portafrutti));
				}

				if (idElementoCantiere != 0) {


					menu.add(Menu.NONE, 10, Menu.NONE, getString(R.string.nota));
					if (mostraRelazioni && item.containsKey("MOSTRA_OPZIONI_RELAZIONI") && !modificheBloccate) {
						menu.add(Menu.NONE, 11, Menu.NONE, getString(R.string.aggiungi_punto_di_comando));
						menu.add(Menu.NONE, 12, Menu.NONE, getString(R.string.aggiungi_punto_comandato));
					}

				}
			}

		}
		System.out.println("EConTab: ComposizioneActivity onCreateContextMenu EXIT");
	}

	private boolean controllaBloccoModifiche() {
		if (modificheBloccate) {
			Toast.makeText(this, getString(R.string.modifiche_non_permesse), Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		System.out.println("EConTab: ComposizioneActivity onContextItemSelected ENTER");
		// TODO Auto-generated method stub
		if (componenteSelezionato != null) {
			aggiornaPreventivo = true;
			if (isComponenteAggiuntivoScatola(componenteSelezionato)){
				if (item.getItemId() == 11){
					//elimino
					DbInterno db = new DbInterno(this);
					ComposizioniCantiere tabComp = new ComposizioniCantiere();
					ContentValues where = new ContentValues();
					where.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
					where.put(ComposizioniCantiere.ID_COMPONENTE_CANT, componenteSelezionato.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
					tabComp.cancellaRecord(db, where);
					db.close();
					ricerca();
				}

				if (item.getItemId() == 12) {
					Intent intent = new Intent(this, ComponenteModActivity.class);

					if (idElemento != 0) {
						intent.putExtra("ID", componenteSelezionato.getAsInteger(Componenti.ID_COMPONENTE));
					}
					if (idElementoCantiere != 0) {
						intent.putExtra("ID", componenteSelezionato.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
						intent.putExtra("CANTIERE", "SI");
					}
					intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE,
							componenteSelezionato.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE));
					startActivity(intent);
				}

				if (item.getItemId() == 10) {
					final View campi = View.inflate(this, R.layout.dialog_note_layout, null);
					final EditText campoNota = (EditText) campi.findViewById(R.id.editText_nota);
					final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							if (which == DialogInterface.BUTTON_POSITIVE) {
								DbInterno db = new DbInterno(ComposizioneActivity.this);
								ComponentiCantiere tabComp = new ComponentiCantiere();
								ContentValues where = new ContentValues();
								where.put(ComponentiCantiere.ID_COMPONENTE_CANT,
										componenteSelezionato.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
								ContentValues valUpd = tabComp.getValoriLogModifica(db);
								valUpd.put(ComponentiCantiere.NOTA, campoNota.getText().toString());
								tabComp.aggiornaRecord(db, valUpd, where);
								db.close();
								componenteSelezionato.put(ComponentiCantiere.NOTA, campoNota.getText().toString());
								adapter.notifyDataSetChanged();
							}

							((ViewGroup) campi.getParent()).removeView(campi);
						}
					};
					campoNota.setText(componenteSelezionato.getAsString(ComponentiCantiere.NOTA));
					Utility.mostraDialogPersonalizzato(componenteSelezionato.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT), this, campi,
							getString(R.string.conferma), getString(R.string.annulla), listener);
				}
			}
			else{
				if (componenteSelezionato.getAsString(CategorieComponenti.TIPO).equals(CategorieComponenti.TIPO_FRUTTO)) {
					if (item.getItemId() == 1) {
						if (idElemento != 0) {
							_selezionaFrutto(componenteSelezionato.getAsInteger(Composizioni.POSIZIONE_INIZIALE));
						}
						if (idElementoCantiere != 0) {
							if (controllaBloccoModifiche()) {
								_selezionaFrutto(componenteSelezionato.getAsInteger(ComposizioniCantiere.POSIZIONE_INIZIALE));
							}
						}
					}
					if (item.getItemId() == 2) {
						Intent intent = new Intent(this, ComponenteModActivity.class);

						if (idElemento != 0) {
							intent.putExtra("ID", componenteSelezionato.getAsInteger(Componenti.ID_COMPONENTE));

						}
						if (idElementoCantiere != 0) {
							intent.putExtra("ID", componenteSelezionato.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
							intent.putExtra("CANTIERE", "SI");

						}
						intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE,
								componenteSelezionato.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE));
						startActivity(intent);
					}
					if (item.getItemId() == 3) {
						if (controllaBloccoModifiche()) {
							Utility.mostraConfermaCancellazioneDialog(this, new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									if (which == DialogInterface.BUTTON_POSITIVE) {
										_eliminaFrutto(componenteSelezionato);
									}
								}
							});
						}

					}
				}

				if (componenteSelezionato.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.SCATOLE) {
					if (item.getItemId() == 1) {
						if (controllaBloccoModifiche()) {
							if (mappaCollegamenti.size() > 0 || mappaRelazioni.size()>0) {
								Utility.mostraConfermaDialog(getString(R.string.attenzione), getString(R.string.messaggio_sostituzione_scatola_relazioni), this, "OK", getString(R.string.annulla), new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										if (i==DialogInterface.BUTTON_POSITIVE){
											int numSpazi = componenteSelezionato.getAsInteger(ComponentiCantiere.SPAZI_OSPITATI_CANT);
											_selezionaScatola(true,numSpazi);
										}
									}
								});

							} else {
								_selezionaScatola(true,-1);
							}
						}
					}
					if (item.getItemId() == 2) {
						Intent intent = new Intent(this, ComponenteModActivity.class);

						if (idElemento != 0) {
							intent.putExtra("ID", componenteSelezionato.getAsInteger(Componenti.ID_COMPONENTE));

						}
						if (idElementoCantiere != 0) {
							intent.putExtra("ID", componenteSelezionato.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
							intent.putExtra("CANTIERE", "SI");

						}
						intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE,
								componenteSelezionato.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE));
						startActivity(intent);
					}
				}

				if (componenteSelezionato.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.COPRISCATOLA) {
					if (item.getItemId() == 1) {
						if (controllaBloccoModifiche()) {
							Intent intent = new Intent(this, FinestraComponentiActivity.class);
							intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE, Componenti.COPRISCATOLA);
							if (idElemento != 0) {
								intent.putExtra(Elementi.ID_ELEMENTO, idElemento);
							}
							if (idElementoCantiere != 0) {
								intent.putExtra(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
								intent.putExtra("CANTIERE", "SI");
							}

							intent.putExtra(Componenti.SPAZI_OSPITATI, postiOspitati);
							startActivity(intent);
						}
					}
					if (item.getItemId() == 2) {
						Intent intent = new Intent(this, ComponenteModActivity.class);

						if (idElemento != 0) {
							intent.putExtra("ID", componenteSelezionato.getAsInteger(Componenti.ID_COMPONENTE));
						}
						if (idElementoCantiere != 0) {
							intent.putExtra("ID", componenteSelezionato.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
							intent.putExtra("CANTIERE", "SI");
						}
						intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE,
								componenteSelezionato.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE));
						startActivity(intent);
					}
					if (item.getItemId() == 3) {
						if (controllaBloccoModifiche()) {
							Intent intent = new Intent(this, FinestraComponentiActivity.class);
							intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE, Componenti.PORTAFRUTTI);
							if (idElemento != 0) {
								intent.putExtra(Elementi.ID_ELEMENTO, idElemento);
							}
							if (idElementoCantiere != 0) {
								intent.putExtra(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
								intent.putExtra("CANTIERE", "SI");
							}
							intent.putExtra(Componenti.SPAZI_OSPITATI, postiOspitati);
							startActivity(intent);
						}
					}


				}

				if (componenteSelezionato.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.PORTAFRUTTI) {
					if (item.getItemId() == 1) {
						if (controllaBloccoModifiche()) {
							Intent intent = new Intent(this, FinestraComponentiActivity.class);
							intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE, Componenti.PORTAFRUTTI);
							if (idElemento != 0) {
								intent.putExtra(Elementi.ID_ELEMENTO, idElemento);
							}
							if (idElementoCantiere != 0) {
								intent.putExtra(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
								intent.putExtra("CANTIERE", "SI");
							}
							intent.putExtra(Componenti.SPAZI_OSPITATI, postiOspitati);
							startActivity(intent);
						}
					}
					if (item.getItemId() == 2) {
						Intent intent = new Intent(this, ComponenteModActivity.class);

						if (idElemento != 0) {
							intent.putExtra("ID", componenteSelezionato.getAsInteger(Componenti.ID_COMPONENTE));
						}
						if (idElementoCantiere != 0) {
							intent.putExtra("ID", componenteSelezionato.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
							intent.putExtra("CANTIERE", "SI");
						}
						intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE,
								componenteSelezionato.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE));
						startActivity(intent);
					}
					if (item.getItemId() == 3) {
						if (controllaBloccoModifiche()) {
							if (mappaCollegamenti.size() > 0 || mappaRelazioni.size()>0) {
								Utility.mostraConfermaDialog(getString(R.string.attenzione), "Verificare che non ci siano collegamenti nel portafrutti e nei frutti prima di procedere. Continuare?", this, getString(R.string.si), getString(R.string.no), new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										if (i==DialogInterface.BUTTON_POSITIVE){
											Intent intent = new Intent(ComposizioneActivity.this, FinestraComponentiActivity.class);
											intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE, Componenti.COPRISCATOLA);
											if (idElemento != 0) {
												intent.putExtra(Elementi.ID_ELEMENTO, idElemento);
											}
											if (idElementoCantiere != 0) {
												intent.putExtra(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
											}
											intent.putExtra(Componenti.SPAZI_OSPITATI, postiOspitati);
											startActivity(intent);
										}
									}
								});
								//Toast.makeText(this, getString(R.string.messaggio_sostituzione_portafrutti_relazioni), Toast.LENGTH_SHORT).show();
							} else {
								Intent intent = new Intent(this, FinestraComponentiActivity.class);
								intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE, Componenti.COPRISCATOLA);
								if (idElemento != 0) {
									intent.putExtra(Elementi.ID_ELEMENTO, idElemento);
								}
								if (idElementoCantiere != 0) {
									intent.putExtra(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
								}
								intent.putExtra(Componenti.SPAZI_OSPITATI, postiOspitati);
								startActivity(intent);
							}
						}
					}
					if (item.getItemId() == 4) {
						if (controllaBloccoModifiche()) {
							//elimino il portafrutti
							_eliminaPortafrutti(componenteSelezionato);
						}
					}

				}

				if (item.getItemId() == 10) {
					final View campi = View.inflate(this, R.layout.dialog_note_layout, null);
					final EditText campoNota = (EditText) campi.findViewById(R.id.editText_nota);
					final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							if (which == DialogInterface.BUTTON_POSITIVE) {
								DbInterno db = new DbInterno(ComposizioneActivity.this);
								ComponentiCantiere tabComp = new ComponentiCantiere();
								ContentValues where = new ContentValues();
								where.put(ComponentiCantiere.ID_COMPONENTE_CANT,
										componenteSelezionato.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
								ContentValues valUpd = tabComp.getValoriLogModifica(db);
								valUpd.put(ComponentiCantiere.NOTA, campoNota.getText().toString());
								tabComp.aggiornaRecord(db, valUpd, where);
								db.close();
								componenteSelezionato.put(ComponentiCantiere.NOTA, campoNota.getText().toString());
								adapter.notifyDataSetChanged();
							}

							((ViewGroup) campi.getParent()).removeView(campi);
						}
					};
					campoNota.setText(componenteSelezionato.getAsString(ComponentiCantiere.NOTA));
					Utility.mostraDialogPersonalizzato(componenteSelezionato.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT), this, campi,
							getString(R.string.conferma), getString(R.string.annulla), listener);
				}

				if (item.getItemId() == 11) {
					Intent intent = new Intent(this, RelazioneActivity.class);
					intent.putExtra("ID_ELEMENTO", idElementoCantiere);
					intent.putExtra("ID_COMPONENTE", componenteSelezionato.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
					intent.putExtra("VERSO_RELAZIONE",RelazioneActivity.PUNTO_DI_COMANDO);
					startActivity(intent);
				}

				if (item.getItemId() == 12) {
					Intent intent = new Intent(this, RelazioneActivity.class);
					intent.putExtra("ID_ELEMENTO", idElementoCantiere);
					intent.putExtra("ID_COMPONENTE", componenteSelezionato.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
					intent.putExtra("VERSO_RELAZIONE",RelazioneActivity.PUNTO_COMANDATO);
					startActivity(intent);
				}
			}

		}

		System.out.println("EConTab: ComposizioneActivity onContextItemSelected EXIT");

		return super.onContextItemSelected(item);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		System.out.println("EConTab: ComposizioneActivity onItemClick");
		// TODO Auto-generated method stub
		arg0.showContextMenuForChild(arg1);

	}

	@Override
	public void onClick(View v) {
		System.out.println("EConTab: ComposizioneActivity onClick");
		// TODO Auto-generated method stub
		v.showContextMenu();
	}

	public void salvaDatiGenerali(View v) {
		System.out.println("EConTab: ComposizioneActivity salvaDatiGenerali ENTER");
		DbInterno db = new DbInterno(this);

		ContentValues val = new ContentValues();
		val.put(ElementiCantiere.NUMERO_IDENTIFICATIVO, getTesto(R.id.editText_etichetta));

		String altezzaTxt = getTesto(R.id.editText_altezza);
		if (altezzaTxt.equals("")) {
			altezzaTxt = "0";
		}
		val.put(ElementiCantiere.ALTEZZA_DA_TERRA, Double.parseDouble(altezzaTxt));

		if(pfa.app.econtab.Globals.EXTRA_CAVI_TUBI_IN_PREVENTIVO == true) {
			// DL: change
			try {
				String MetriCavoTxt = getTesto(R.id.editText_MetriCavo);
				if (MetriCavoTxt.equals("") == false) {
					val.put(ElementiCantiere.METRI_CAVO_CANT, (float)(Double.parseDouble(MetriCavoTxt)));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				String MetriTuboTxt = getTesto(R.id.editText_MetriTubo);
				if (MetriTuboTxt.equals("") == false) {
					val.put(ElementiCantiere.METRI_TUBO_CANT, (float)(Double.parseDouble(MetriTuboTxt)));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		ContentValues where = new ContentValues();
		where.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
		ElementiCantiere tabElementi = new ElementiCantiere();
		tabElementi.aggiornaRecord(db, val, where);

		db.close();

		Toast.makeText(this, getString(R.string.messaggio_salvataggio_eseguito), Toast.LENGTH_SHORT).show();
		System.out.println("EConTab: ComposizioneActivity salvaDatiGenerali EXIT");
	}

	public void apriModifica(View v) {
		System.out.println("EConTab: ComposizioneActivity apriModifica ENTER");
		Intent intent = new Intent(this, ElementoModActivity.class);
		intent.putExtra("ID", idElementoCantiere);
		intent.putExtra("CANTIERE", "SI");
		intent.putExtra(CategorieGenerali.ID_CATEGORIA_GENERALE, idCategoria);
		System.out.println("EConTab: ComposizioneActivity apriModifica EXIT");
		startActivity(intent);
	}

	private void _caricaCollegamenti(DbInterno db, ArrayList<Object> componenti) {
		System.out.println("EConTab: ComposizioneActivity _caricaCollegamenti ENTER");
		// TODO Auto-generated method stub
		Collegamenti tabColl = new Collegamenti();
		ArrayList<Object> locali = tabColl.caricaLocaliCantiere(db, idElementoCantiere);

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

		String SQL = "Select " + Collegamenti.NOME_TABELLA + ".*," + ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.ID_ELEMENTO
				+ "," + ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.NOME_ELEMENTO_CANT + "," + ElementiCantiere.NOME_TABELLA
				+ "." + ElementiCantiere.NUMERO_IDENTIFICATIVO + "," + ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.ID_LOCALE
				+ " as ID_LOCALE_A,ElementiCantiere2." + ElementiCantiere.ID_ELEMENTO + " as idElemento_2,ElementiCantiere2."
				+ ElementiCantiere.NOME_ELEMENTO_CANT + " as nome_elemento_cant2,ElementiCantiere2."
				+ ElementiCantiere.NUMERO_IDENTIFICATIVO + " as numero_identificativo_2,ElementiCantiere2." + ElementiCantiere.ID_LOCALE
				+ " as ID_LOCALE_B from " + Collegamenti.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where ("
				+ Collegamenti.ID_ELEMENTO_CANT1 + " = " + idElementoCantiere + " or " + Collegamenti.ID_ELEMENTO_CANT2 + " = "
				+ idElementoCantiere + ") ";
		ArrayList<Object> relazioni = new ArrayList<Object>();
		relazioni.addAll(db.eseguiSelect(SQL + filtroTubi, null));

        ArrayList<Object> relazioniCavi = db.eseguiSelect(SQL + filtroCavi, null);
        for (int i=0;i<relazioniCavi.size();i++){
            boolean trovato = false;
            ContentValues curr = (ContentValues)relazioniCavi.get(i);
            int idCollTubo = curr.getAsInteger(Collegamenti.ID_COLLEGAMENTO_TUBO);
            if (idCollTubo==0){

                relazioni.add(curr);
                trovato = true;
            }
            else{
                for (int j=0;j<relazioni.size();j++){
                    ContentValues currTubo = (ContentValues)relazioni.get(j);
                    int idTubo = currTubo.getAsInteger(Collegamenti.ID_COLLEGAMENTO);
                    if (idTubo==idCollTubo){
                        currTubo.put("ID_TUBO_PER_ORDINAMENTO",idTubo);
                        curr.put("ID_TUBO_PER_ORDINAMENTO",idTubo);
                        relazioni.add(j+1,curr);
                        trovato = true;
                        break;
                    }
                }
            }
            if (!trovato){

                relazioni.add(curr);
            }
        }

		//relazioni.addAll(db.eseguiSelect(SQL + filtroCavi, null));

		ContentValues whereComp = new ContentValues();
		ComponentiCantiere tabComp = new ComponentiCantiere();
		ContentValues recComp = null;

		for (int i = 0; i < relazioni.size(); i++) {
			ContentValues curr = (ContentValues) relazioni.get(i);
			// prendo i dati accessori (locale, componente ecc)
			tabColl.aggiungiTuboCavo(curr, tubi, cavi, this);
			tabColl.aggiungiLocali(curr, locali);

			curr.put("COMPONENTE_A", "");
			curr.put("COMPONENTE_B", "");
			int idComponente1 = curr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT1);
			if (idComponente1 != 0) {
				whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponente1);
				recComp = db.getRecord(tabComp, whereComp);
				if (recComp != null) {
					// prendo la composizione
					String moduliOccupati = "";
					ContentValues whereCompos = new ContentValues();
					whereCompos.put(ComposizioniCantiere.ID_ELEMENTO_CANT, curr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT1));
					whereCompos.put(ComposizioniCantiere.ID_COMPONENTE_CANT, idComponente1);

					ContentValues valCompos = db.getRecord(new ComposizioniCantiere(), whereCompos);
					if (valCompos != null) {
						moduliOccupati = valCompos.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
                        if (!moduliOccupati.trim().equals("")){
                            moduliOccupati = moduliOccupati + " - ";
                        }
					}

					curr.put("COMPONENTE_A", moduliOccupati + recComp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
				}
			}

			int idComponente2 = curr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT2);
			if (idComponente2 != 0) {
				whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponente2);
				recComp = db.getRecord(tabComp, whereComp);
				if (recComp != null) {

					// prendo la composizione
					String moduliOccupati = "";
					ContentValues whereCompos = new ContentValues();
					whereCompos.put(ComposizioniCantiere.ID_ELEMENTO_CANT, curr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT2));
					whereCompos.put(ComposizioniCantiere.ID_COMPONENTE_CANT, idComponente2);

					ContentValues valCompos = db.getRecord(new ComposizioniCantiere(), whereCompos);
					if (valCompos != null) {
						moduliOccupati = valCompos.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
                        if (!moduliOccupati.trim().equals("")){
                            moduliOccupati = moduliOccupati + " - ";
                        }
					}

					curr.put("COMPONENTE_B", moduliOccupati +recComp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
				}
			}
		}

		for (int i = 0; i < componenti.size(); i++) {
			ContentValues comp = (ContentValues) componenti.get(i);
			int idCatgeoriaComp = comp.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE);
			int idComponente = comp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT);
			// se sono nella scatola cerco i tubi (id componente = 0)
			if (idCatgeoriaComp == Componenti.SCATOLE) {
				_aggiungiCollegamenti(idComponente, relazioni, db, CollegamentoActivity.TUBO);
                //aggiungo anche i collegamenti cavi sulle scatole (aggiunta del 13/02/2015)
                _aggiungiCollegamenti(idComponente, relazioni, db, CollegamentoActivity.CAVO);
			} else {
				if (idCatgeoriaComp != Componenti.COPRISCATOLA && idCatgeoriaComp != Componenti.PORTAFRUTTI) {
					_aggiungiCollegamenti(idComponente, relazioni, db, CollegamentoActivity.CAVO);
				}
			}

		}



        Iterator iterCollegamenti = mappaCollegamenti.keySet().iterator();
        while (iterCollegamenti.hasNext()){
            ArrayList<Object> listaCurr = mappaCollegamenti.get(iterCollegamenti.next());
            if (listaCurr.size()>1) {
                Collections.sort(listaCurr, new Comparator<Object>() {
                    @Override
                    public int compare(Object o, Object o2) {
                        ContentValues cv1 = (ContentValues) o;
                        ContentValues cv2 = (ContentValues) o2;
                        int num1 = cv1.getAsInteger("numero_identificativo_2");
                        int num2 = cv2.getAsInteger("numero_identificativo_2");
                        if (num1 > num2) {
                            return 1;
                        }
                        if (num2 > num1) {
                            return -1;
                        }

                        int idTuboPerOrdine1 = 0;
                        int idTuboPerOrdine2 = 0;
                        if (cv1.containsKey("ID_TUBO_PER_ORDINAMENTO")) {
                            idTuboPerOrdine1 = cv1.getAsInteger("ID_TUBO_PER_ORDINAMENTO");
                        }
                        if (cv2.containsKey("ID_TUBO_PER_ORDINAMENTO")) {
                            idTuboPerOrdine2 = cv2.getAsInteger("ID_TUBO_PER_ORDINAMENTO");
                        }

                        if (idTuboPerOrdine1 > idTuboPerOrdine2) {
                            return 1;
                        }
                        if (idTuboPerOrdine1 < idTuboPerOrdine2) {
                            return -1;
                        }

                        return 0;
                    }
                });
            }

            for (int i=0;i<listaCurr.size();i++){
                ContentValues curr = (ContentValues)listaCurr.get(i);
                int idCavo = curr.getAsInteger(Collegamenti.ID_CAVO);
                //se è un cavo verifico che esista il suo tubo nello stesso insieme di collegamenti
                //altrimenti deveo mostrare un cavo da solo (quindi per lo meno dovrò visualizzare il numero nell'adapter)
                if (idCavo!=0){
                    int idCollTubo = curr.getAsInteger(Collegamenti.ID_COLLEGAMENTO_TUBO);
                    for (int j=0;j<listaCurr.size();j++){
                        ContentValues currTubo = (ContentValues)listaCurr.get(j);
                        if (currTubo.getAsInteger(Collegamenti.ID_COLLEGAMENTO)==idCollTubo){
                            curr.put("TUBO_PRESENTE","1");
                            break;
                        }
                    }
                }
            }
        }
		System.out.println("EConTab: ComposizioneActivity _caricaCollegamenti EXIT");
	}



    private void _caricaRelazioniLogiche(DbInterno db, ArrayList<Object> componenti) {
		System.out.println("EConTab: ComposizioneActivity _caricaRelazioniLogiche ENTER");
        Join j0 = new Join(Relazioni.NOME_TABELLA,ElementiCantiere.NOME_TABELLA);
        j0.addCampiDiJoin(Relazioni.ID_ELEMENTO_CANT2,ElementiCantiere.ID_ELEMENTO_CANT);

        Join j0_2 = new Join(Relazioni.NOME_TABELLA,ElementiCantiere.NOME_TABELLA);
        j0_2.addCampiDiJoin(Relazioni.ID_ELEMENTO_CANT1,ElementiCantiere.ID_ELEMENTO_CANT);

        Join j1 = new Join(ElementiCantiere.NOME_TABELLA, Locali.NOME_TABELLA);
        j1.addCampiDiJoin(ElementiCantiere.ID_LOCALE,Locali.ID_LOCALE);

        Join j2 = new Join(Locali.NOME_TABELLA, Aree.NOME_TABELLA);
        j2.addCampiDiJoin(Locali.ID_AREA,Aree.ID_AREA);

        //relazioni come elemento comandato
        String SQLREL = "Select "+Relazioni.NOME_TABELLA+".*,"+Aree.NOME_TABELLA+"."+Aree.NOME+" as nome_area,"+Locali.NOME_TABELLA+"."+Locali.NOME+" as nome_locale,"
                +ElementiCantiere.ID_ELEMENTO+","+ElementiCantiere.NUMERO_IDENTIFICATIVO+","+ElementiCantiere.NOME_ELEMENTO_CANT+" " +
                "from "+ Relazioni.NOME_TABELLA  + j0.getSQLJoin()+j1.getSQLJoin()+j2.getSQLJoin()+" where " + Relazioni.ID_ELEMENTO_CANT1+"="+idElementoCantiere;

        //relazioni come elemento di comando
        String SQLREL2 = "Select "+Relazioni.NOME_TABELLA+".*,"+Aree.NOME_TABELLA+"."+Aree.NOME+" as nome_area,"+Locali.NOME_TABELLA+"."+Locali.NOME+" as nome_locale,"
                +ElementiCantiere.ID_ELEMENTO+","+ElementiCantiere.NUMERO_IDENTIFICATIVO+","+ElementiCantiere.NOME_ELEMENTO_CANT+" " +
                "from "+ Relazioni.NOME_TABELLA  + j0_2.getSQLJoin()+j1.getSQLJoin()+j2.getSQLJoin()+" where " + Relazioni.ID_ELEMENTO_CANT2+"="+idElementoCantiere;

        ArrayList<Object> relazioniLogiche = new ArrayList<Object>();
        relazioniLogiche.addAll(db.eseguiSelect(SQLREL, null));
        relazioniLogiche.addAll(db.eseguiSelect(SQLREL2, null));
        ContentValues whereComp = new ContentValues();
        ContentValues recComp = null;


        ComponentiCantiere tabComp = new ComponentiCantiere();

        for (int i=0;i<relazioniLogiche.size();i++) {

            ContentValues curr = (ContentValues) relazioniLogiche.get(i);
            if (curr.getAsInteger(Relazioni.ID_ELEMENTO_CANT1)==idElementoCantiere){
                curr.put("COMPONENTE_B", "");
                int idComponente2 = 0;
                try {
                    idComponente2 = curr.getAsInteger(Relazioni.ID_COMPONENTE_CANT2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (idComponente2 != 0) {
                    whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponente2);
                    recComp = db.getRecord(tabComp, whereComp);
                    if (recComp != null) {
                        // prendo la composizione
                        String moduliOccupati = "";
                        ContentValues whereCompos = new ContentValues();
                        whereCompos.put(ComposizioniCantiere.ID_ELEMENTO_CANT, curr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT2));
                        whereCompos.put(ComposizioniCantiere.ID_COMPONENTE_CANT, idComponente2);

                        ContentValues valCompos = db.getRecord(new ComposizioniCantiere(), whereCompos);
                        if (valCompos != null) {
                            moduliOccupati = valCompos.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
                        }

                        curr.put("COMPONENTE_B", " - Posto " + moduliOccupati + " (" + recComp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT) + ")");
                    }
                }
            }

            if (curr.getAsInteger(Relazioni.ID_ELEMENTO_CANT2)==idElementoCantiere){
                curr.put("COMPONENTE_B", "");
                int idComponente1 = 0;
                try {
                    idComponente1 = curr.getAsInteger(Relazioni.ID_COMPONENTE_CANT1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (idComponente1 != 0) {
                    whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponente1);
                    recComp = db.getRecord(tabComp, whereComp);
                    if (recComp != null) {
                        // prendo la composizione
                        String moduliOccupati = "";
                        ContentValues whereCompos = new ContentValues();
                        whereCompos.put(ComposizioniCantiere.ID_ELEMENTO_CANT, curr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT1));
                        whereCompos.put(ComposizioniCantiere.ID_COMPONENTE_CANT, idComponente1);

                        ContentValues valCompos = db.getRecord(new ComposizioniCantiere(), whereCompos);
                        if (valCompos != null) {
                            moduliOccupati = valCompos.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
                        }

                        curr.put("COMPONENTE_B", " - Posto " + moduliOccupati + " (" + recComp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT) + ")");
                    }
                }
            }



        }

        for (int i = 0; i < componenti.size(); i++) {
            ContentValues comp = (ContentValues) componenti.get(i);
            int idCatgeoriaComp = comp.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE);
            int idComponente = comp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT);

            if (idCatgeoriaComp != Componenti.COPRISCATOLA && idCatgeoriaComp != Componenti.PORTAFRUTTI && idCatgeoriaComp!=Componenti.SCATOLE) {
                    _aggiungiRelazioni(idComponente, relazioniLogiche, db, CollegamentoActivity.CAVO);
                }


        }

		System.out.println("EConTab: ComposizioneActivity _caricaRelazioniLogiche EXIT");
    }

	private void _aggiungiCollegamenti(int idComponente, ArrayList<Object> relazioni, DbInterno db, int tipo) {
		System.out.println("EConTab: ComposizioneActivity _aggiungiCollegamenti ENTER");
		// TODO Auto-generated method stub
		for (int i = 0; i < relazioni.size(); i++) {

			ContentValues relCurr = (ContentValues) relazioni.get(i);
			int idElementoPartenza = relCurr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT1);
			int idElementoDestinazione = relCurr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT2);
			int idComponentePartenza = relCurr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT1);
			int idComponenteDestinazione = relCurr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT2);
			int idElemententoRel = 0;
			if (idElementoPartenza == idElementoCantiere) {
				idElemententoRel = relCurr.getAsInteger("idElemento_2");

			}
			if (idElementoDestinazione == idElementoCantiere) {
				// se l'elemento visualizzato � elemento di destinazione del collegamento allora mostro i dati
				// dell'elemento di partenza
				idElemententoRel = relCurr.getAsInteger(ElementiCantiere.ID_ELEMENTO);
				relCurr.put("LOCALE_B", relCurr.getAsString("LOCALE_A"));
				relCurr.put("numero_identificativo_2", relCurr.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO));
				relCurr.put("nome_elemento_cant2", relCurr.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
				relCurr.put("COMPONENTE_B", relCurr.getAsString("COMPONENTE_A"));

			}
			int idTubo = relCurr.getAsInteger(Collegamenti.ID_TUBO);
			int idCavo = relCurr.getAsInteger(Collegamenti.ID_CAVO);
			if (tipo == CollegamentoActivity.TUBO && idTubo != 0) {

				if (idElemententoRel != 0) {
					ContentValues whereElem = new ContentValues();
					whereElem.put(Elementi.ID_ELEMENTO, idElemententoRel);
					ContentValues valElem = db.getRecord(new Elementi(), whereElem);
					if (valElem != null) {
						int idCategoria = valElem.getAsInteger(Elementi.ID_CATEGORIA_GENERALE);
						relCurr.put("ID_CATEGORIA", idCategoria);

					}
				}

				if (mappaCollegamenti.containsKey(idComponente)) {
					ArrayList<Object> listaCollegamenti = mappaCollegamenti.get(idComponente);
					listaCollegamenti.add(relCurr);

				} else {
					ArrayList<Object> listaCollegamenti = new ArrayList<Object>();
					listaCollegamenti.add(relCurr);
					mappaCollegamenti.put(idComponente, listaCollegamenti);
				}
			}

			if (tipo == CollegamentoActivity.CAVO && idCavo != 0
					&& (idComponente == idComponentePartenza || idComponente == idComponenteDestinazione)) {
				if (idElemententoRel != 0) {
					ContentValues whereElem = new ContentValues();
					whereElem.put(Elementi.ID_ELEMENTO, idElemententoRel);
					ContentValues valElem = db.getRecord(new Elementi(), whereElem);
					if (valElem != null) {
						int idCategoria = valElem.getAsInteger(Elementi.ID_CATEGORIA_GENERALE);
						relCurr.put("ID_CATEGORIA", idCategoria);

					}
				}

				if (mappaCollegamenti.containsKey(idComponente)) {
					ArrayList<Object> listaCollegamenti = mappaCollegamenti.get(idComponente);
					listaCollegamenti.add(relCurr);

				} else {
					ArrayList<Object> listaCollegamenti = new ArrayList<Object>();
					listaCollegamenti.add(relCurr);
					mappaCollegamenti.put(idComponente, listaCollegamenti);
				}
			}
		}


		System.out.println("EConTab: ComposizioneActivity _aggiungiCollegamenti EXIT");
	}


    private void _aggiungiRelazioni(int idComponente, ArrayList<Object> relazioni, DbInterno db, int tipo) {
		System.out.println("EConTab: ComposizioneActivity _aggiungiRelazioni ENTER");
        for (int i = 0; i < relazioni.size(); i++) {

            ContentValues relCurr = (ContentValues) relazioni.get(i);
            int idComponentePartenza = relCurr.getAsInteger(Relazioni.ID_COMPONENTE_CANT1);
            int idComponenteDestinazione = relCurr.getAsInteger(Relazioni.ID_COMPONENTE_CANT2);
            if (idComponente==idComponentePartenza||idComponente==idComponenteDestinazione) {
                if (mappaRelazioni.containsKey(idComponente)) {
                    //controllo che non ci sia già la relazione per questo componente (può succedere ad esempio se un interruttore comanda una presa nella stessa scatola)
                    boolean giaPresente = false;
                    ArrayList<Object> listaRelazioni = mappaRelazioni.get(idComponente);
                    int idRel = relCurr.getAsInteger(Relazioni.ID_RELAZIONE);
                    for (int r=0;r<listaRelazioni.size();r++){
                        ContentValues currRel = (ContentValues)listaRelazioni.get(r);
                        if (currRel.getAsInteger(Relazioni.ID_RELAZIONE)==idRel){
                            giaPresente = true;
                        }
                    }
                    if (!giaPresente){
                        listaRelazioni.add(relCurr);
                    }


                } else {
                    ArrayList<Object> listaRelazioni = new ArrayList<Object>();
                    listaRelazioni.add(relCurr);
                    mappaRelazioni.put(idComponente, listaRelazioni);
                }
            }
        }
		System.out.println("EConTab: ComposizioneActivity _aggiungiRelazioni EXIT");
    }

private boolean isComponenteAggiuntivoScatola(ContentValues val){
	if (idElementoCantiere!=0 && val.get(ComposizioniCantiere.POSIZIONE_INIZIALE)!=null && val.getAsInteger(ComposizioniCantiere.POSIZIONE_INIZIALE)==100){
		return true;
	}
	return false;
}

}
