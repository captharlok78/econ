package pfa.app.econtab;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import pfa.app.econtab.adapters.EConTabListViewAdapter;
import pfa.app.econtab.adapters.EConTabViewHolder;
import pfa.app.econtab.adapters.CantiereMenuAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Aree;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.CategorieGenerali;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.Unita;
import pfa.app.econtab.export.ComposizioneScatoleXLS;
import pfa.app.econtab.fragments.AreaPagerFragment;
import pfa.app.econtab.fragments.CantierePagerFragment;
import pfa.app.econtab.fragments.LocalePagerFragment;
import pfa.app.econtab.fragments.PreventivoPagerFragment;
import pfa.app.econtab.fragments.UnitaPagerFragment;
import pfa.app.econtab.utils.AsyncTaskExecutorService;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabItemMenu;
import pfa.app.econtab.views.EConTabSplitPaneLayout;

public class CantiereSplitActivity extends EConTabActivity implements OnItemClickListener, OnClickListener {

	public boolean isMostraAltriOrdiniSuPiantina() {
		return mostraAltriOrdiniSuPiantina;
	}

	private class SelPreventiviViewHolder extends EConTabViewHolder{
		TextView txtNome = null;
		CheckBox chkAltriOrdini = null;
	}

	private class AdapterSelezionePreventivi extends EConTabListViewAdapter{



		public AdapterSelezionePreventivi(Context context, ArrayList<Object> dati, int layoutid) {
			super(context, dati, layoutid);
		}

		@Override
		protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
			// TODO Auto-generated method stub
			SelPreventiviViewHolder holder = new SelPreventiviViewHolder();
			holder.txtNome = (TextView)convertView.findViewById(R.id.nome);
			holder.chkAltriOrdini = (CheckBox)convertView.findViewById(R.id.chkAltriordini);

			return holder;
		}

		@Override
		protected void personalizzaView(int position, EConTabViewHolder viewholder) {
			 ContentValues val = (ContentValues)dati.get(position);
			((SelPreventiviViewHolder)viewholder).txtNome.setText(val.getAsString("nome"));
			((SelPreventiviViewHolder)viewholder).txtNome.setTag(position);


			((SelPreventiviViewHolder)viewholder).chkAltriOrdini.setVisibility(View.GONE);
			/*if (position<2){
				((SelPreventiviViewHolder)viewholder).chkAltriOrdini.setVisibility(View.GONE);
			}
			else {
				((SelPreventiviViewHolder)viewholder).chkAltriOrdini.setVisibility(View.VISIBLE);
				((SelPreventiviViewHolder)viewholder).chkAltriOrdini.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
						ContentValues valCurr = (ContentValues)dati.get((Integer)compoundButton.getTag());
						if (b){
							valCurr.put("mostraAltriOrdini", true);
						}
						else{
							valCurr.put("mostraAltriOrdini",false);
						}

					}
				});
			}

			((SelPreventiviViewHolder)viewholder).chkAltriOrdini.setTag(position);
			((SelPreventiviViewHolder)viewholder).chkAltriOrdini.setChecked(val.getAsBoolean("mostraAltriOrdini"));

		*/

			super.personalizzaView(position, viewholder);
		}

		public boolean mostraAltriOrdini(int i) {
			ContentValues valCurr = (ContentValues)dati.get(i);
			return valCurr.getAsBoolean("mostraAltriOrdini");
		}
	}

	public static final int PREVENTIVO = 0;

	public static final int CANTIERE = 1;
	public static final int UNITA = 2;
	public static final int AREA = 3;
	public static final int LOCALE = 4;

	EConTabSplitPaneLayout split = null;

	ListView lista = null;
	CantiereMenuAdapter adapter = null;
	ArrayList<Object> dati = null;

	PreventivoPagerFragment fragmentPagerPreventivo = null;
	CantierePagerFragment fragmentPagerCantiere = null;
	UnitaPagerFragment fragmentPagerUnita = null;
	AreaPagerFragment fragmentPagerArea = null;
	LocalePagerFragment fragmentPagerLocale = null;

	private int tipoElementoSelezionato = CANTIERE;
	private int selezionato = 0;

	int cantiere = 0;

	String titoloCantiere = "";

	private LinearLayout categorie = null;

	private RelativeLayout menuinferiore = null;

	private TextView testoPreventivoSelezionato = null;
	private String titoloPreventivoSelezionato = "";
    private String tipoPreventivoOrdine = Preventivi.TIPO_PREVENTIVO;
	private int idPreventivoSelezionato = 0;
	private boolean mostraAltriOrdiniSuPiantina = false;

	private String[] preventiviCantiere = null;
	private boolean[] mostraAltriOrdiniArray = null;
	private ArrayList<Integer> listaIdPreventivi = null;

	private AlertDialog di;
	private int idPreventivoDaStampare = 0;

	private boolean cancellaPreventivo = false;
	private boolean nuovoPreventivo = false;

	private boolean modificheBloccate = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: CantiereSplitActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

		setContentView(R.layout.activity_cantiere_split);
		cantiere = getIntent().getIntExtra(Cantieri.ID_CANTIERE, 0);
		idPreventivoSelezionato = getIntent().getIntExtra(Preventivi.ID_PREVENTIVO, 0);
		// se non ho nessun preventivo seleionato prendo l'ultimo ordine ed eventualmente l'ultimo preventivo per
		// questocantiere
		if (idPreventivoSelezionato == 0) {
			DbInterno db = new DbInterno(this);
			ContentValues whereOrd = new ContentValues();
			whereOrd.put(Preventivi.ID_CANTIERE, cantiere);
			whereOrd.put(Preventivi.TIPO, Preventivi.TIPO_ORDINE);
			whereOrd.put(Preventivi.STATO, Preventivi.STATO_APERTO);

			ArrayList<Object> ordini = db.eseguiSelect(new Preventivi(), whereOrd, new String[] { Preventivi.DATA + " desc" });
			if (ordini.size() == 0) {
				ContentValues wherePrev = new ContentValues();
				wherePrev.put(Preventivi.ID_CANTIERE, cantiere);
				wherePrev.put(Preventivi.TIPO, Preventivi.TIPO_PREVENTIVO);
				wherePrev.put(Preventivi.STATO, Preventivi.STATO_APERTO);
				ArrayList<Object> preventivi = db.eseguiSelect(new Preventivi(), wherePrev, new String[] { Preventivi.DATA + " desc" });
				if (preventivi.size() > 0) {
					ContentValues prev = (ContentValues) preventivi.get(0);
					idPreventivoSelezionato = prev.getAsInteger(Preventivi.ID_PREVENTIVO);
				}
			} else {
				ContentValues ord = (ContentValues) ordini.get(0);
				idPreventivoSelezionato = ord.getAsInteger(Preventivi.ID_PREVENTIVO);
			}
			db.close();
		}

		lista = (ListView) findViewById(R.id.lista);
		pfa.app.econtab.utils.FaIcone.applica((TextView) findViewById(R.id.imageView1), pfa.app.econtab.utils.FaIcone.PREVENTIVO, null);
		lista.setOnItemClickListener(this);
		registerForContextMenu(lista);
		split = (EConTabSplitPaneLayout) findViewById(R.id.split);

		categorie = (LinearLayout) findViewById(R.id.categorie);
		menuinferiore = (RelativeLayout) findViewById(R.id.menuinferiore);

		testoPreventivoSelezionato = (TextView) findViewById(R.id.textView_preventivo);

		findViewById(R.id.preventivo_attivo).setOnClickListener(this);
		findViewById(R.id.button_cambiapreventivo).setOnClickListener(this);

		creaMenuCategorie();

		if (idPreventivoSelezionato == 0) {
			Toast t = Toast.makeText(this,
					getString(R.string.attenzione).toUpperCase(Locale.getDefault()) + System.getProperty("line.separator")
							+ getString(R.string.nessun_preventivo_selezionato_esteso), Toast.LENGTH_LONG);
			t.setGravity(Gravity.CENTER, 0, 0);
			t.show();
		}

		Sessione.setIdPreventivoSelezionato(idPreventivoSelezionato);

		System.out.println("EConTab: CantiereSplitActivity onCreate EXIT");
	}

	/** Scrive il titolo del cantiere nella testata standard (headerTitolo). */
	private void impostaTitoloCantiere() {
		TextView tvTitolo = (TextView) findViewById(R.id.headerTitolo);
		if (tvTitolo != null) {
			tvTitolo.setText(titoloCantiere.trim());
		}
	}

	private void creaMenu() {
		System.out.println("EConTab: CantiereSplitActivity creaMenu ENTER");
		// TODO Auto-generated method stub
		lista.invalidate();
		if (dati == null) {
			dati = new ArrayList<Object>();
		}
		dati.clear();

		DbInterno db = new DbInterno(this);

		ContentValues where = new ContentValues();
		where.put(Cantieri.ID_CANTIERE, cantiere);
		ContentValues recCantiere = db.getRecord(new Cantieri(), where);
		if (recCantiere == null) {
			db.close();
			finish();
			return;
		}

		ContentValues whereUnita = new ContentValues();
		whereUnita.put(Unita.ID_CANTIERE, cantiere);
		ArrayList<Object> unita = db.eseguiSelect(new Unita(), whereUnita, new String[] { Unita.ID_UNITA + " desc" });

		Aree tabAree = new Aree();
		ArrayList<Object> aree = tabAree.getAreeCantiere(db, cantiere);

		Locali tabLocali = new Locali();
		ArrayList<Object> locali = tabLocali.getLocaliCantiereOrdineAlfabetico(db, cantiere);
		db.close();

		_mostraRigaPreventivo(false);

		ContentValues val = new ContentValues();
		val.put("TIPO", CANTIERE);
		titoloCantiere = recCantiere.getAsString(Cantieri.NOME);
		while (titoloCantiere.length()<50){
			titoloCantiere = titoloCantiere+" ";
		}
			impostaTitoloCantiere();
		val.put("NOME", recCantiere.getAsString(Cantieri.NOME));
		val.put("ID", cantiere);
		dati.add(val);
		// Itero le unit�
		for (int i = 0; i < unita.size(); i++) {
			ContentValues valUni = (ContentValues) unita.get(i);
			int idUnita = valUni.getAsInteger(Unita.ID_UNITA);
			ContentValues val_u = new ContentValues();
			val_u.put("TIPO", UNITA);
			val_u.put("NOME", valUni.getAsString(Unita.NOME));
			val_u.put("ID", valUni.getAsInteger(Unita.ID_UNITA));
			dati.add(val_u);

			// Itero le aree
			for (int j = 0; j < aree.size(); j++) {
				ContentValues valArea = (ContentValues) aree.get(j);
				int idUnitaArea = valArea.getAsInteger(Aree.ID_UNITA);
				if (idUnita == idUnitaArea) {
					int idArea = valArea.getAsInteger(Aree.ID_AREA);
					ContentValues val_a = new ContentValues();
					val_a.put("TIPO", AREA);
					val_a.put("NOME", valArea.getAsString(Aree.NOME));
					val_a.put("ID", valArea.getAsInteger(Aree.ID_AREA));
					dati.add(val_a);

					// Itero i locali
					for (int k = 0; k < locali.size(); k++) {
						ContentValues valLocale = (ContentValues) locali.get(k);
						int idAreaLocale = valLocale.getAsInteger(Locali.ID_AREA);
						if (idAreaLocale == idArea) {
							ContentValues val_l = new ContentValues();
							val_l.put("TIPO", LOCALE);
							val_l.put("NOME", valLocale.getAsString(Locali.NOME));
							val_l.put("ID", valLocale.getAsInteger(Locali.ID_LOCALE));
							dati.add(val_l);
						}
					}
				}
			}
		}

		if (adapter == null) {
			adapter = new CantiereMenuAdapter(this, dati);
			lista.setAdapter(adapter);
			lista.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
			registerForContextMenu(lista);
		} else {
			adapter.notifyDataSetChanged();

		}

		System.out.println("EConTab: CantiereSplitActivity creaMenu EXIT");
	}

	private void _mostraRigaPreventivo(boolean notifyDatSetChanged) {
		System.out.println("EConTab: CantiereSplitActivity _mostraRigaPreventivo ENTER");
		// TODO Auto-generated method stub
		if (idPreventivoSelezionato != 0) {
			if (dati.size() > 0) {
				ContentValues riga0 = (ContentValues) dati.get(0);
				if (riga0.getAsInteger("TIPO") == PREVENTIVO) {
					dati.remove(0);
					selezionato = selezionato - 1;
				}

			}
			ContentValues valPrev = new ContentValues();
			valPrev.put("TIPO", PREVENTIVO);
			valPrev.put("NOME", titoloPreventivoSelezionato);
			valPrev.put("ID", 0);
            valPrev.put("TIPO_PREV_ORD",tipoPreventivoOrdine);
			dati.add(0, valPrev);

			if (dati.size() > 1) {
				// se ero gi� dentro al cantiere allora mantengo la vecchia selezione
				// altrimenti attivo la selezione sul preventivo se arrivo dalla lista dei preventivi
				selezionato = selezionato + 1;
			} else {
				if (getIntent().getIntExtra(Preventivi.ID_PREVENTIVO, 0) != 0 && selezionato == 0) {

					tipoElementoSelezionato = PREVENTIVO;

				}

			}

		} else {
			if (dati.size() > 0) {
				ContentValues riga0 = (ContentValues) dati.get(0);
				if (riga0.getAsInteger("TIPO") == PREVENTIVO) {
					dati.remove(0);
					if (selezionato > 0) {
						selezionato = selezionato - 1;
					} else {
						// significa che era selezionato il preventivo quindi seleziono il cantiere
						onItemClick(lista, null, 0, 1);
					}

				}

			}
		}

		if (notifyDatSetChanged) {

			adapter.notifyDataSetChanged();

		}
		System.out.println("EConTab: CantiereSplitActivity _mostraRigaPreventivo EXIT");
	}

	public void modifica(View v) {
		System.out.println("EConTab: CantiereSplitActivity modifica ENTER");
		if (v.getId() == R.id.button_edit_cantiere) {
			Intent intent = new Intent(this, CantieriDettaglioModActivity.class);
			intent.putExtra("ID", getIntent().getIntExtra(Cantieri.ID_CANTIERE, 0));
			apriFinestraModifica(intent, 1);
		}
		if (v.getId() == R.id.button_edit_unita) {
			Intent intent = new Intent(this, UnitaDettaglioModActivity.class);
			intent.putExtra("ID", fragmentPagerUnita.getArguments().getInt(Unita.ID_UNITA));
			apriFinestraModifica(intent, 1);
		}
		if (v.getId() == R.id.button_edit_area) {
			Intent intent = new Intent(this, AreaDettaglioModActivity.class);
			intent.putExtra("ID", fragmentPagerArea.getArguments().getInt(Aree.ID_AREA));
			apriFinestraModifica(intent, 1);
		}
		if (v.getId() == R.id.button_edit_locale) {
			Intent intent = new Intent(this, LocaleDettaglioModActivity.class);
			intent.putExtra("ID", fragmentPagerLocale.getArguments().getInt(Locali.ID_LOCALE));
			apriFinestraModifica(intent, 10);
		}
		System.out.println("EConTab: CantiereSplitActivity modifica EXIT");
	}

	@Override
	protected void onResume() {
		System.out.println("EConTab: CantiereSplitActivity onResume ENTER");
		// TODO Auto-generated method stub
		super.onResume();

		creaMenu();

		if (tipoElementoSelezionato == PREVENTIVO) {
			if (fragmentPagerPreventivo == null) {
				onItemClick(lista, null, 0, 1);
			}
		}

		if (tipoElementoSelezionato == CANTIERE) {
			if (fragmentPagerCantiere == null) {
				if (idPreventivoSelezionato != 0) {
					onItemClick(lista, null, 1, 1);
				} else {
					onItemClick(lista, null, 0, 1);
				}
			}
		}

		// carico i preventivi da mostrare nelle voci
		caricaPreventiviCantiere();
		impostaSelezionePreventivo();
		if (nuovoPreventivo == true) {
			nuovoPreventivo = false;
			mostraSelezionePreventivi();
		}

		// creaImmaginiPiantine(0);
		System.out.println("EConTab: CantiereSplitActivity onResume EXIT");
	}

	private void caricaPreventiviCantiere() {
		System.out.println("EConTab: CantiereSplitActivity caricaPreventiviCantiere ENTER");
		// TODO Auto-generated method stub
		if (listaIdPreventivi == null) {
			listaIdPreventivi = new ArrayList<Integer>();
		}
		listaIdPreventivi.clear();
		DbInterno db = new DbInterno(this);
		ContentValues wherePrevCant = new ContentValues();
		wherePrevCant.put(Preventivi.ID_CANTIERE, cantiere);
		// wherePrevCant.put(Preventivi.STATO, Preventivi.STATO_APERTO);
		wherePrevCant.put(Preventivi.TIPO, Preventivi.TIPO_PREVENTIVO);
		ArrayList<Object> listaPrev = db.eseguiSelect(new Preventivi(), wherePrevCant, new String[] { Preventivi.ID_PREVENTIVO + " desc" });

		ContentValues whereOrdCant = new ContentValues();
		whereOrdCant.put(Preventivi.ID_CANTIERE, cantiere);
		// whereOrdCant.put(Preventivi.STATO, Preventivi.STATO_APERTO);
		whereOrdCant.put(Preventivi.TIPO, Preventivi.TIPO_ORDINE);
		ArrayList<Object> listaOrdini = db
				.eseguiSelect(new Preventivi(), whereOrdCant, new String[] { Preventivi.ID_PREVENTIVO + " desc" });

		listaPrev.addAll(listaOrdini);

		db.close();
		Preventivi tabPrev = new Preventivi();
		preventiviCantiere = new String[listaPrev.size() + 2];
		mostraAltriOrdiniArray = new boolean[listaPrev.size() + 2];
		preventiviCantiere[0] = "[ + " + getResources().getString(R.string.crea_nuovo).toUpperCase() + " ]";
		preventiviCantiere[1] = "[ " + getResources().getString(R.string.nessun_preventivo_selezionato) + " ]";
		mostraAltriOrdiniArray[0] = false;
		mostraAltriOrdiniArray[1] = false;
		listaIdPreventivi.add(0);
		for (int i = 0; i < listaPrev.size(); i++) {
			ContentValues prevCurr = (ContentValues) listaPrev.get(i);
			listaIdPreventivi.add(prevCurr.getAsInteger(Preventivi.ID_PREVENTIVO));
			String tipo = prevCurr.getAsString(Preventivi.TIPO);
			int numero = 0;
			String data = Utility.numberToData(prevCurr.getAsLong(Preventivi.DATA));
			if (tipo.equals(Preventivi.TIPO_PREVENTIVO)) {
				numero = prevCurr.getAsInteger(Preventivi.NUMERO);
				String titolo = prevCurr.getAsString(Preventivi.TITOLO);
				if (titolo.length() > 0) {
					titolo = "\n" + titolo;
				}
				String stato = prevCurr.getAsString(Preventivi.STATO);
				stato = tabPrev.getDescrizioneStatoPreventivo(stato, this).toString();
				preventiviCantiere[i + 2] = getString(R.string.preventivo_num_del, numero, data).trim() + " (" + stato + ")" + titolo;
				mostraAltriOrdiniArray[i+2] = false;
			}
			if (tipo.equals(Preventivi.TIPO_ORDINE)) {
				numero = prevCurr.getAsInteger(Preventivi.NUMERO);
				String titolo = prevCurr.getAsString(Preventivi.TITOLO);
				if (titolo.length() > 0) {
					titolo = "\n" + titolo;
				}
				String stato = prevCurr.getAsString(Preventivi.STATO);
				stato = tabPrev.getDescrizioneStatoPreventivo(stato, this).toString();
				preventiviCantiere[i + 2] = getString(R.string.ordine_num_del, numero, data).trim() + " (" + stato + ")" + titolo;
				mostraAltriOrdiniArray[i+2] = false;
			}
		}
		System.out.println("EConTab: CantiereSplitActivity caricaPreventiviCantiere EXIT");
	}

	private void impostaSelezionePreventivo() {
		System.out.println("EConTab: CantiereSplitActivity impostaSelezionePreventivo ENTER");
		// TODO Auto-generated method stub
		setModificheBloccate(false);
		DbInterno db = new DbInterno(this);

		if (idPreventivoSelezionato == 0) {
			testoPreventivoSelezionato.setText(getString(R.string.nessun_preventivo_selezionato));
			titoloPreventivoSelezionato = "";

		} else {
			Preventivi tabPrev = new Preventivi();
			ContentValues where = new ContentValues();
			where.put(Preventivi.ID_PREVENTIVO, idPreventivoSelezionato);
			ContentValues prev = db.getRecord(new Preventivi(), where);

			if (prev != null) {

				if (!prev.getAsString(Preventivi.STATO).equals(Preventivi.STATO_APERTO)) {
					setModificheBloccate(true);
				}

				String tipo = prev.getAsString(Preventivi.TIPO);
                tipoPreventivoOrdine = tipo;
				if (tipo.equals(Preventivi.TIPO_PREVENTIVO)) {
					testoPreventivoSelezionato.setText(getString(R.string.preventivo_num_del, prev.getAsInteger(Preventivi.NUMERO),
							Utility.numberToData(prev.getAsLong(Preventivi.DATA)))
							+ " - " + tabPrev.getDescrizioneStatoPreventivo(prev.getAsString(Preventivi.STATO), this).toString());


				}
				if (tipo.equals(Preventivi.TIPO_ORDINE)) {
					testoPreventivoSelezionato.setText(getString(R.string.ordine_num_del, prev.getAsInteger(Preventivi.NUMERO),
							Utility.numberToData(prev.getAsLong(Preventivi.DATA)))
							+ " - " + tabPrev.getDescrizioneStatoPreventivo(prev.getAsString(Preventivi.STATO), this).toString());

				}
				titoloPreventivoSelezionato = prev.getAsString(Preventivi.TITOLO);
				if (titoloPreventivoSelezionato.equals("")) {
					titoloPreventivoSelezionato = testoPreventivoSelezionato.getText().toString();
				}
			}
		}

		db.close();
		_mostraRigaPreventivo(true);
		System.out.println("EConTab: CantiereSplitActivity impostaSelezionePreventivo EXIT");
	}

	public float getSplitterPositionPercent() {
		// TODO Auto-generated method stub
		return split.getSplitterPositionPercent();
	}

	public void setSplitterPositionPercent(float position) {
		split.setSplitterPositionPercent(position);
		split.requestLayout();
	}

	@Override
	public void onItemClick(AdapterView<?> ad, View arg1, int position, long arg3) {
		System.out.println("EConTab: CantiereSplitActivity onItemClick ENTER");
		// TODO Auto-generated method stub
		ContentValues val = (ContentValues) dati.get(position);
		tipoElementoSelezionato = val.getAsInteger("TIPO");
		selezionato = position;
		lista.invalidate();
		adapter.notifyDataSetChanged();

		if (tipoElementoSelezionato == PREVENTIVO) {
			System.out.println("EConTab: CantiereSplitActivity onItemClick PREVENTIVO");
			fragmentPagerPreventivo = new PreventivoPagerFragment();
			Bundle params = getIntent().getExtras();
			params.putInt(Preventivi.ID_PREVENTIVO, idPreventivoSelezionato);
			fragmentPagerPreventivo.setArguments(params);
			impostaFragment(R.id.destra, fragmentPagerPreventivo);
			impostaTitoloCantiere();

			// Il dettaglio del preventivo occupa tutta la larghezza, come le altre maschere: il menu del
			// cantiere si riapre con la freccia sul divisore.
			if (getSplitterPositionPercent() > 0.2f) {
				split.collapse();
			}

		}

		if (tipoElementoSelezionato == CANTIERE) {
			System.out.println("EConTab: CantiereSplitActivity onItemClick CANTIERE");
			fragmentPagerCantiere = new CantierePagerFragment();
			fragmentPagerCantiere.setArguments(getIntent().getExtras());
			impostaFragment(R.id.destra, fragmentPagerCantiere);
			setText(R.id.nuovo_elemento, getResources().getString(R.string.nuova_unita));

			impostaTitoloCantiere();

		}
		if (tipoElementoSelezionato == UNITA) {
			System.out.println("EConTab: CantiereSplitActivity onItemClick UNITA");
			fragmentPagerUnita = new UnitaPagerFragment();
			Bundle params = getIntent().getExtras();
			params.putInt(Unita.ID_UNITA, val.getAsInteger("ID"));
			fragmentPagerUnita.setArguments(params);
			impostaFragment(R.id.destra, fragmentPagerUnita);
			setText(R.id.nuovo_elemento, getResources().getString(R.string.nuova_area));

			impostaTitoloCantiere();
		}
		if (tipoElementoSelezionato == AREA) {
			System.out.println("EConTab: CantiereSplitActivity onItemClick AREA");
			fragmentPagerArea = new AreaPagerFragment();
			Bundle params = getIntent().getExtras();
			params.putInt(Aree.ID_AREA, val.getAsInteger("ID"));
			fragmentPagerArea.setArguments(params);
			impostaFragment(R.id.destra, fragmentPagerArea);
			setText(R.id.nuovo_elemento, getResources().getString(R.string.nuovo_locale));

			impostaTitoloCantiere();
		}

		if (tipoElementoSelezionato == LOCALE) {
			System.out.println("EConTab: CantiereSplitActivity onItemClick LOCALE");
			fragmentPagerLocale = new LocalePagerFragment();
			Bundle params = getIntent().getExtras();
			params.putInt(Locali.ID_LOCALE, val.getAsInteger("ID"));
			params.putBoolean("MODIFICHE_BLOCCATE", isModificheBloccate());
			fragmentPagerLocale.setArguments(params);
			impostaFragment(R.id.destra, fragmentPagerLocale);
			findViewById(R.id.button_nuovo_elem).setVisibility(View.GONE);
			if (isModificheBloccate()) {
				menuinferiore.setVisibility(View.GONE);

			} else {
				menuinferiore.setVisibility(View.VISIBLE);
			}

		} else {
			if (tipoElementoSelezionato == PREVENTIVO) {
				findViewById(R.id.button_nuovo_elem).setVisibility(View.GONE);
			} else {
				findViewById(R.id.button_nuovo_elem).setVisibility(View.VISIBLE);
			}

			menuinferiore.setVisibility(View.GONE);
		}
		System.out.println("EConTab: CantiereSplitActivity onItemClick EXIT");
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		System.out.println("EConTab: CantiereSplitActivity onCreateContextMenu ENTER");
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		ContentValues item = (ContentValues) lista.getItemAtPosition(info.position);
		menu.setHeaderTitle(CantiereMenuAdapter.getTipoStringa(item, this) + ": " + item.getAsString("NOME"));
		menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
		menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));

		System.out.println("EConTab: CantiereSplitActivity onCreateContextMenu EXIT");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		System.out.println("EConTab: CantiereSplitActivity onContextItemSelected ENTER");
		// TODO Auto-generated method stub
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		ContentValues val = (ContentValues) lista.getItemAtPosition(info.position);
		int tipo = val.getAsInteger("TIPO");
		int idElemento = val.getAsInteger("ID");
		if (item.getItemId() == 1) {
			if (tipo == PREVENTIVO) {
				Intent intent = new Intent(this, PreventiviDettaglioModActivity.class);
				intent.putExtra("ID", idPreventivoSelezionato);
				apriFinestraModifica(intent, 1);
			}
			if (tipo == CANTIERE) {
				Intent intent = new Intent(this, CantieriDettaglioModActivity.class);
				intent.putExtra("ID", idElemento);
				apriFinestraModifica(intent, 1);
			}
			if (tipo == UNITA) {
				Intent intent = new Intent(this, UnitaDettaglioModActivity.class);
				intent.putExtra("ID", idElemento);
				apriFinestraModifica(intent, 1);
			}
			if (tipo == AREA) {
				Intent intent = new Intent(this, AreaDettaglioModActivity.class);
				intent.putExtra("ID", idElemento);
				apriFinestraModifica(intent, 1);
			}
			if (tipo == LOCALE) {
				Intent intent = new Intent(this, LocaleDettaglioModActivity.class);
				intent.putExtra("ID", idElemento);
				apriFinestraModifica(intent, 1);
			}
		}
		if (item.getItemId() == 2) {

			if (tipo == PREVENTIVO) {
				ContentValues valDel = new ContentValues();
				valDel.put(Preventivi.ID_PREVENTIVO, idPreventivoSelezionato);
				cancellaPreventivo = true;
				confermaCancellazione(new Preventivi(), valDel, true);

			}
			if (tipo == CANTIERE) {
				ContentValues valDel = new ContentValues();
				valDel.put(Cantieri.ID_CANTIERE, idElemento);
				confermaCancellazione(new Cantieri(), valDel, true);
			}
			if (tipo == UNITA) {
				ContentValues valDel = new ContentValues();
				valDel.put(Unita.ID_UNITA, idElemento);
				valDel.put(Unita.ID_CANTIERE, cantiere);
				confermaCancellazione(new Unita(), valDel, true);
			}
			if (tipo == AREA) {
				ContentValues valDel = new ContentValues();
				valDel.put(Aree.ID_AREA, idElemento);
				confermaCancellazione(new Aree(), valDel, true);
			}
			if (tipo == LOCALE) {
				ContentValues valDel = new ContentValues();
				valDel.put(Locali.ID_LOCALE, idElemento);
				confermaCancellazione(new Locali(), valDel, true);
			}

		}
		System.out.println("EConTab: CantiereSplitActivity onContextItemSelected EXIT");
		return super.onContextItemSelected(item);
	}

	public int getSelezionato() {
		return selezionato;
	}

	@Override
	protected void aggiornaDopoCancellazione() {
		// TODO Auto-generated method stub
		if (cancellaPreventivo) {
			idPreventivoSelezionato = 0;
			Sessione.setIdPreventivoSelezionato(idPreventivoSelezionato);
			caricaPreventiviCantiere();
			impostaSelezionePreventivo();
			cancellaPreventivo = false;

		}
		creaMenu();
		if (idPreventivoSelezionato != 0) {
			selezionato = 1;

		} else {
			selezionato = 0;
		}

		tipoElementoSelezionato = CANTIERE;

		fragmentPagerCantiere = new CantierePagerFragment();
		fragmentPagerCantiere.setArguments(getIntent().getExtras());
		impostaFragment(R.id.destra, fragmentPagerCantiere);
		setText(R.id.nuovo_elemento, getResources().getString(R.string.nuova_unita));
		findViewById(R.id.button_nuovo_elem).setVisibility(View.VISIBLE);
		menuinferiore.setVisibility(View.GONE);
	}

	public void nuovoElemento(View v) {
		System.out.println("EConTab: CantiereSplitActivity nuovoElemento ENTER");
		ContentValues val = (ContentValues) dati.get(selezionato);
		int tipo = val.getAsInteger("TIPO");
		int id = val.getAsInteger("ID");
		if (tipo == CANTIERE) {
			Intent intent = new Intent(this, UnitaDettaglioModActivity.class);
			intent.putExtra(Cantieri.ID_CANTIERE, cantiere);
			apriFinestraInserimento(intent, 1, new Unita());
		}
		if (tipo == UNITA) {
			Intent intent = new Intent(this, AreaDettaglioModActivity.class);
			intent.putExtra(Unita.ID_UNITA, id);
			apriFinestraInserimento(intent, 1, new Aree());
		}
		if (tipo == AREA) {
			Intent intent = new Intent(this, LocaleDettaglioModActivity.class);
			intent.putExtra(Aree.ID_AREA, id);

			apriFinestraInserimento(intent, 1, new Locali());
		}
		System.out.println("EConTab: CantiereSplitActivity nuovoElemento EXIT");
	}

	private void creaMenuCategorie() {
		System.out.println("EConTab: CantiereSplitActivity creaMenuCategorie ENTER");
		// TODO Auto-generated method stub
		DbInterno db = new DbInterno(this);
		CategorieGenerali tabCat = new CategorieGenerali();
		ContentValues where = new ContentValues();
		where.put(CategorieGenerali.POSIZIONABILE_SN, 1);
		ArrayList<Object> lista = db.eseguiSelect(tabCat, where, null);

		db.close();

		System.out.println("EConTab: CantiereSplitActivity creaMenuCategorie lista.size() -> " + lista.size());

		for (int i = 0; i < lista.size(); i++) {
			ContentValues val = (ContentValues) lista.get(i);
			EConTabItemMenu item = new EConTabItemMenu(this);
			System.out.println("EConTab: CantiereSplitActivity creaMenuCategorie NOME -> " + val.getAsString(CategorieGenerali.NOME) + ", " + val.getAsString(CategorieGenerali.NOME).length());
			item.setTesto(val.getAsString(CategorieGenerali.NOME).trim());
			item.setIcona(CategorieGenerali.PATH_ICONE, val.getAsString(CategorieGenerali.ICONA));
			item.setOnClickListener(this);
			item.setTag(val.getAsInteger(CategorieGenerali.ID_CATEGORIA_GENERALE));

			categorie.addView(item);

		}
		System.out.println("EConTab: CantiereSplitActivity creaMenuCategorie EXIT");
	}

	@Override
	public void onClick(View v) {
		System.out.println("EConTab: CantiereSplitActivity onClick ENTER");
		// TODO Auto-generated method stub
		if (v instanceof EConTabItemMenu) {
			ContentValues val = (ContentValues) dati.get(selezionato);
			Intent intent = new Intent(this, FinestraElementiActivity.class);
			intent.putExtra(CategorieGenerali.ID_CATEGORIA_GENERALE, Integer.parseInt(v.getTag().toString()));
			intent.putExtra(Cantieri.ID_CANTIERE, cantiere);
			intent.putExtra(Cantieri.NOME, titoloCantiere);
			intent.putExtra(Preventivi.ID_PREVENTIVO, idPreventivoSelezionato);
			intent.putExtra(Locali.ID_LOCALE, val.getAsInteger("ID"));
			if (idPreventivoSelezionato == 0) {
				intent.putExtra("DESC_PREVENTIVO", getString(R.string.nessun_preventivo_selezionato_esteso));
			} else {
				intent.putExtra("DESC_PREVENTIVO", testoPreventivoSelezionato.getText().toString());
			}

			startActivityForResult(intent, 1);
		}
		if (v.getId() == R.id.button_cambiapreventivo || v.getId() == R.id.preventivo_attivo) {
			mostraSelezionePreventivi();
		}
		System.out.println("EConTab: CantiereSplitActivity onClick EXIT");
	}

	public void mostraSelezionePreventivi() {
		System.out.println("EConTab: CantiereSplitActivity mostraSelezionePreventivi ENTER");
		AlertDialog di = null;
		AlertDialog.Builder ab = new AlertDialog.Builder(this);
		ab.setTitle(getString(R.string.title_activity_preventivi));
		final ListView listaPreventivi = new ListView(this);
		listaPreventivi.setBackgroundColor(getResources().getColor(android.R.color.transparent));

		final ArrayList<Object> data = new ArrayList<Object>();
		for (int i=0;i<preventiviCantiere.length;i++){
			ContentValues val = new ContentValues();
			val.put("nome",preventiviCantiere[i]);
			val.put("mostraAltriOrdini",mostraAltriOrdiniArray[i]);
			data.add(val);
		}
		final AdapterSelezionePreventivi adapterSel = new AdapterSelezionePreventivi(this,data,R.layout.list_item_selezione_ordine);
		listaPreventivi.setAdapter(adapterSel);
		lista.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

		listaPreventivi.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				if (i == 0) {
					nuovoPreventivo = true;
					// nuovo preventivo
					Intent intent = new Intent(CantiereSplitActivity.this, PreventiviDettaglioModActivity.class);
					intent.putExtra(Cantieri.ID_CANTIERE, cantiere);
					intent.putExtra(Preventivi.TIPO, Preventivi.TIPO_PREVENTIVO);
					((AlertDialog)listaPreventivi.getTag()).cancel();
					startActivityForResult(intent, 2);

				} else {
					idPreventivoSelezionato = listaIdPreventivi.get(i - 1);
					Sessione.setIdPreventivoSelezionato(idPreventivoSelezionato);
					impostaSelezionePreventivo();
					if (i>1){
						mostraAltriOrdiniSuPiantina = adapterSel.mostraAltriOrdini(i);
						mostraAltriOrdiniArray[i] = adapterSel.mostraAltriOrdini(i);
					}
					if (tipoElementoSelezionato == PREVENTIVO) {
						fragmentPagerPreventivo = new PreventivoPagerFragment();
						Bundle params = getIntent().getExtras();
						params.putInt(Preventivi.ID_PREVENTIVO, idPreventivoSelezionato);
						fragmentPagerPreventivo.setArguments(params);
						impostaFragment(R.id.destra, fragmentPagerPreventivo);
					} else {

						aggiornaElementiPiantina(false);
					}


					((AlertDialog)listaPreventivi.getTag()).cancel();
				}


			}
		});
		ab.setView(listaPreventivi);
		/*ab.setItems(preventiviCantiere, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int which) {
				if (which == 0) {
					nuovoPreventivo = true;
					// nuovo preventivo
					Intent intent = new Intent(CantiereSplitActivity.this, PreventiviDettaglioModActivity.class);
					intent.putExtra(Cantieri.ID_CANTIERE, cantiere);
					intent.putExtra(Preventivi.TIPO, Preventivi.TIPO_PREVENTIVO);
					startActivityForResult(intent, 2);

				} else {
					idPreventivoSelezionato = listaIdPreventivi.get(which - 1);
					impostaSelezionePreventivo();
					if (tipoElementoSelezionato == PREVENTIVO) {
						fragmentPagerPreventivo = new PreventivoPagerFragment();
						Bundle params = getIntent().getExtras();
						params.putInt(Preventivi.ID_PREVENTIVO, idPreventivoSelezionato);
						fragmentPagerPreventivo.setArguments(params);
						impostaFragment(R.id.destra, fragmentPagerPreventivo);
					} else {
						aggiornaElementiPiantina(false);
					}

				}
			}
		});*/
		ab.setIcon(android.R.drawable.ic_dialog_info);
		di = ab.create();
		di.show();
		di.setCancelable(true);
		di.setCanceledOnTouchOutside(true);
		listaPreventivi.setTag(di);
		System.out.println("EConTab: CantiereSplitActivity mostraSelezionePreventivi EXIT");
	}

	public int getIdPreventivoSelezionato() {
		return idPreventivoSelezionato;
	}

	public void setIdPreventivoSelezionato(int idPreventivoSelezionato) {
		this.idPreventivoSelezionato = idPreventivoSelezionato;
		Sessione.setIdPreventivoSelezionato(idPreventivoSelezionato);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent arg2) {
		// TODO Auto-generated method stub

		super.onActivityResult(requestCode, resultCode, arg2);
		if (requestCode == 1) {
			aggiornaElementiPiantina(true);
		}
		if (requestCode == 2) {
			aggiornaElementiPiantina(true);
		}
		// ritorno da selezione preferito
		if (requestCode == 3) {
			aggiornaElementiPiantina(false);
			if (tipoElementoSelezionato == LOCALE) {
				fragmentPagerLocale.aggiornaElementiPortePiantina();
			}
		}

		if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
			fragmentPagerPreventivo.importaDaFileXLS(arg2.getStringExtra("PATH"));
		}
	}

	private void aggiornaElementiPiantina(boolean dragattivo) {
		if (tipoElementoSelezionato == LOCALE) {
			fragmentPagerLocale.aggiornaElementiPiantina(dragattivo, isModificheBloccate());
			if (isModificheBloccate()) {
				menuinferiore.setVisibility(View.GONE);
			} else {
				menuinferiore.setVisibility(View.VISIBLE);
			}
		}
	}

	public void creaImmaginiPiantine(int inizio, int idPreventivoStampa) {
		split.collapse();
		if (inizio == 0) {
			AlertDialog.Builder ab = new AlertDialog.Builder(this);
			ab.setMessage("Creazione immagini piantine...");
			di = ab.create();
			di.show();
			idPreventivoDaStampare = idPreventivoStampa;

		}

		boolean trovato = false;
		for (int i = inizio; i < dati.size(); i++) {
			ContentValues val = (ContentValues) dati.get(i);
			int tipoSel = val.getAsInteger("TIPO");
			if (tipoSel == LOCALE) {
				fragmentPagerLocale = new LocalePagerFragment();
				Bundle params = getIntent().getExtras();
				params.putInt(Locali.ID_LOCALE, val.getAsInteger("ID"));
				params.putInt("PROSSIMO_ID", i + 1);
				fragmentPagerLocale.setArguments(params);
				impostaFragment(R.id.destra, fragmentPagerLocale);
				findViewById(R.id.button_nuovo_elem).setVisibility(View.GONE);
				menuinferiore.setVisibility(View.VISIBLE);
				trovato = true;
				break;
			}
		}

		if (!trovato) {
			onItemClick(lista, null, selezionato, 0l);
			di.cancel();
			if (idPreventivoDaStampare != 0) {
				stampaComposizioneScatole(idPreventivoDaStampare, true);
				idPreventivoDaStampare = 0;
			}
		}
	}

	public void stampaComposizioneScatole(final int idPreventivo,final boolean conpiantine) {
		// new class for asynchronous task
		AsyncTaskExecutorService task = new AsyncTaskExecutorService() {
			AlertDialog di = null;
			@Override
			protected void onPreExecute() {
				AlertDialog.Builder ab = new AlertDialog.Builder(CantiereSplitActivity.this);
				ab.setMessage(CantiereSplitActivity.this.getResources().getString(R.string.messaggio_creazione_report));
				di = ab.create();
				di.show();
			}

			@Override
			protected Object doInBackground(Object o) {
				ComposizioneScatoleXLS report = new ComposizioneScatoleXLS(CantiereSplitActivity.this);
				String percorsoFile = null;
				try {
					percorsoFile = report.generaReport(idPreventivo, conpiantine);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return percorsoFile;
			}

			@Override
			protected void onPostExecute(Object o) {
				di.cancel();
				if (o!=null){
					String percorsoFile = o.toString();
					System.out.println("EConTab: CantiereSplitActivity onPostExecute percorsoFile " + percorsoFile);
					File fileExport = new File(percorsoFile);
					Intent intent = new Intent(Intent.ACTION_VIEW);
					System.out.println("EConTab: CantiereSplitActivity onPostExecute getApplicationContext().getPackageName() " + getApplicationContext().getPackageName());
					/*
					EConTab: CantiereSplitActivity onPostExecute percorsoFile /storage/emulated/0/ECONTAB/ABC Spa/ordini/scatole_O1_2025.xls
					EConTab: CantiereSplitActivity onPostExecute getApplicationContext().getPackageName() pfa.app.econtab
					*/
					Uri uri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider",fileExport);
					if(uri != null)
					{
						//intent.setDataAndType(Uri.fromFile(fileExport), "application/vnd.ms-excel");
						intent.setDataAndType(uri, "application/vnd.ms-excel");
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						startActivity(intent);
					}
				}
			}
		};
		task.execute();
		/*
        AsyncTask task = new AsyncTask() {
            AlertDialog di = null;
            @Override
            protected void onPreExecute() {
                AlertDialog.Builder ab = new AlertDialog.Builder(CantiereSplitActivity.this);
                ab.setMessage(CantiereSplitActivity.this.getResources().getString(R.string.messaggio_creazione_report));
                di = ab.create();
                di.show();
            }

            @Override
            protected Object doInBackground(Object[] objects) {
                ComposizioneScatoleXLS report = new ComposizioneScatoleXLS(CantiereSplitActivity.this);
                String percorsoFile = null;
                try {
                    percorsoFile = report.generaReport(idPreventivo, conpiantine);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return percorsoFile;

            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                di.cancel();
                if (o!=null){
                    String percorsoFile = o.toString();
                    File fileExport = new File(percorsoFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(fileExport), "application/vnd.ms-excel");
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }

            }
        };
        task.execute();
		*/
	}

	public void setModificheBloccate(boolean bloccate) {
		// TODO Auto-generated method stub
		this.modificheBloccate = bloccate;
	}

	public boolean isModificheBloccate() {
		return modificheBloccate;
	}

	public void cercaElemento() {

		final View campi = View.inflate(this, R.layout.dialog_note_layout, null);
		campi.findViewById(R.id.textView_note).setVisibility(View.INVISIBLE);
		final EditText campoNota = (EditText) campi.findViewById(R.id.editText_nota);
		campoNota.setInputType(InputType.TYPE_CLASS_NUMBER);
		campoNota.setEms(5);

		Utility.mostraDialogPersonalizzato("Cerca elemento per numero",this,campi,"Ok","Annulla", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				if (i==DialogInterface.BUTTON_POSITIVE){
					_cercaNumeroElementoSuCantiere(campoNota.getText().toString());
				}
			}
		});
	}

	private void _cercaNumeroElementoSuCantiere(String s) {
		int numero = 0;
		try{
			numero = Integer.parseInt(s);
			DbInterno db = new DbInterno(this);
			ContentValues val =  db.getRecord("select locali.nome as nome_locale,aree.nome as nome_area from elementi_cantiere inner join locali on elementi_cantiere.id_locale=locali.id_locale inner join aree on locali.id_area=aree.id_area inner join unita on aree.id_unita=unita.id_unita where unita.id_cantiere="+cantiere+" and elementi_cantiere.numero_identificativo="+numero);
			db.close();
			if (val!=null){
				Utility.mostraDialog("Ricerca elemento","L' elemento num. " + numero+ " si trova in: "+System.getProperty("line.separator") +"AREA: " + val.getAsString("nome_area")+ System.getProperty("line.separator") +"LOCALE: " + val.getAsString("nome_locale"),this,"Ok");
			}
			else{
				Utility.mostraDialog("Ricerca elemento","Elemento non trovato",this,"Ok");
			}
		}
		catch (Exception ex){

		}

	}
}
