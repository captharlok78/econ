package pfa.app.econtab.fragments;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;

import androidx.core.content.FileProvider;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import pfa.app.econtab.EConTabFileChooserActivity;
import pfa.app.econtab.CantiereSplitActivity;
import pfa.app.econtab.FinestraListinoBaseActivity;
import pfa.app.econtab.GestAbbonamentoActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.adapters.PreventiviDettaglioAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Aree;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCodici;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Listini;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Manodopera;
import pfa.app.econtab.db.table.PlaccheModuli;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.Rapportini;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.db.table.Unita;
import pfa.app.econtab.export.ConsuntivoXLS;
import pfa.app.econtab.export.PreventivoXLS;
import pfa.app.econtab.utils.AsyncTaskExecutorService;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabSpinner;
import pfa.app.econtab.views.PopupAggiungiPlaccaPreventivo;

public class PreventivoDettaglioFragment extends EConTabFragment implements OnClickListener, OnScrollListener {
	public static final String RIGA_LOCALE = "RIGA_LOCALE";

	private int idPreventivo = 0;
	private int idCantiere = 0;
	private String tipoPreventivoOrdine = "";
	private String descrizionePreventivo = "";

	private ListView lista = null;

	private PreventiviDettaglioAdapter adapter = null;

	private ArrayList<Object> dati = null;
	private ImageButton buttonSalva = null;
	private ImageButton buttonEsporta = null;
	private ImageButton buttonNuovo = null;
	private ImageButton buttonAssocia = null;
	private ImageButton buttonRicalcola = null;

	private TextView righeInModifica = null;
	private TextView textViewTotaleImponibile = null;
	private TextView textViewTotaleIVA = null;
	private TextView textViewTotaleImporto = null;

	private double totaleImponibile = 0.0f;
	private double totaleIva = 0.0f;
	private double totaleImporto = 0.0f;

	private HashMap<String, Double> mappa_iva = null;

	private LinearLayout linearTitolo = null;
	private TextView descrizioneLocale = null;
	private Handler scrollHandler = null;
	private Runnable scrollRunnable = null;

	private boolean modificheBloccate = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View v = super.onCreateView(inflater, container, savedInstanceState);
		v = inflater.inflate(R.layout.fragment_preventivo_dettaglio, container, false);
		if (getArguments() != null) {
			idPreventivo = getArguments().getInt(Preventivi.ID_PREVENTIVO);

		}
		lista = (ListView) v.findViewById(R.id.lista_dett);
        lista.setFastScrollEnabled(false);
		linearTitolo = (LinearLayout) v.findViewById(R.id.LinearLayout_titolocorrente);
		linearTitolo.setVisibility(View.GONE);
		scrollHandler = new Handler();
		scrollRunnable = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				linearTitolo.setVisibility(View.INVISIBLE);

			}
		};

		descrizioneLocale = (TextView) linearTitolo.findViewById(R.id.localecorrente);

		buttonNuovo = (ImageButton) v.findViewById(R.id.button_nuovo);
		buttonNuovo.setOnClickListener(this);
		buttonAssocia = (ImageButton) v.findViewById(R.id.button_associa);
		buttonAssocia.setOnClickListener(this);

		buttonSalva = (ImageButton) v.findViewById(R.id.button_salva_prev);
		buttonSalva.setOnClickListener(this);

		buttonEsporta = (ImageButton) v.findViewById(R.id.button_esporta);
		buttonEsporta.setOnClickListener(this);

		buttonRicalcola = (ImageButton) v.findViewById(R.id.button_ricalcola);
		buttonRicalcola.setOnClickListener(this);

		// buttonSalva.setVisibility(View.GONE);

		righeInModifica = (TextView) v.findViewById(R.id.textViewModificati);
		righeInModifica.setVisibility(View.GONE);

		textViewTotaleImponibile = (TextView) v.findViewById(R.id.textViewTotaleImponibile);
		textViewTotaleIVA = (TextView) v.findViewById(R.id.textViewTotaleIva);
		textViewTotaleImporto = (TextView) v.findViewById(R.id.textViewTotaleImporto);

		mappa_iva = Utility.getMappaCodiciIva(getEConTabActivity());

		lista.setOnScrollListener(this);

		return v;

	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		ricerca();

		if (isModificheBloccate()) {
			buttonSalva.setVisibility(View.GONE);
			buttonAssocia.setVisibility(View.GONE);
			buttonNuovo.setVisibility(View.GONE);
			buttonRicalcola.setVisibility(View.GONE);
		} else {
			buttonSalva.setVisibility(View.VISIBLE);
			buttonAssocia.setVisibility(View.VISIBLE);
			buttonNuovo.setVisibility(View.VISIBLE);
			buttonRicalcola.setVisibility(View.GONE);
			//buttonRicalcola.setVisibility(View.VISIBLE);
		}
	}

	public void ricerca() {
		System.out.println("EConTab: PreventivoDettaglioFragment ricerca ENTER");
		lista.invalidate();
		if (dati == null) {
			dati = new ArrayList<Object>();
		}

		HashMap<Integer, ContentValues> righeInModifica = null;
		if (adapter != null) {
			righeInModifica = adapter.getRigheInModifica();
		}

		dati.clear();
		DbInterno db = new DbInterno(getActivity());

		// prendo il cantiere
		ContentValues wherePrev = new ContentValues();
		wherePrev.put(Preventivi.ID_PREVENTIVO, idPreventivo);
		ContentValues testataPrev = db.getRecord(new Preventivi(), wherePrev);

		// DL: here sometimes crashed
		try{
			if (!testataPrev.getAsString(Preventivi.STATO).equals(Preventivi.STATO_APERTO)) {
				setModificheBloccate(true);
			} else {
				setModificheBloccate(false);
			}
		}
		catch (Exception ex){
			setModificheBloccate(true);
		}

		idCantiere = testataPrev.getAsInteger(Preventivi.ID_CANTIERE);
		tipoPreventivoOrdine = testataPrev.getAsString(Preventivi.TIPO);

		System.out.println("EConTab: PreventivoDettaglioFragment ricerca idCantiere "+ idCantiere);

		ContentValues valLineaCant = null;

		Join j1 = new Join(PreventiviDettaglio.NOME_TABELLA, Locali.NOME_TABELLA, Join.LEFT_JOIN);
		j1.addCampiDiJoin(PreventiviDettaglio.ID_LOCALE, Locali.ID_LOCALE);

		Join j2 = new Join(PreventiviDettaglio.NOME_TABELLA, Componenti.NOME_TABELLA, Join.LEFT_JOIN);
		j2.addCampiDiJoin(PreventiviDettaglio.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j3 = new Join(PreventiviDettaglio.NOME_TABELLA, Elementi.NOME_TABELLA, Join.LEFT_JOIN);
		j3.addCampiDiJoin(PreventiviDettaglio.ID_ELEMENTO, Elementi.ID_ELEMENTO);

		Join j4 = new Join(Locali.NOME_TABELLA, Aree.NOME_TABELLA, Join.LEFT_JOIN);
		j4.addCampiDiJoin(Locali.ID_AREA, Aree.ID_AREA);

		PreventiviDettaglio tabPrev = new PreventiviDettaglio();
		Locali tabLoc = new Locali();

		String SQL = "Select " + tabPrev.getNomeCampoTabella("*") + "," + Aree.NOME_TABELLA+"."+Aree.NOME+" as nomeArea,"+ tabLoc.getNomeCampoTabella(Locali.NOME) + " from "
				+ PreventiviDettaglio.NOME_TABELLA + j1.getSQLJoin() + j2.getSQLJoin() + j3.getSQLJoin() + j4.getSQLJoin() + " where "
				+ PreventiviDettaglio.ID_PREVENTIVO + "=" + idPreventivo + " and " + PreventiviDettaglio.TIPO + " in ('"
				+ PreventiviDettaglio.MATERIALE + "','" + PreventiviDettaglio.MATERIALE_PREVENTIVO + "','" + PreventiviDettaglio.PLACCHE
				+ "','" + PreventiviDettaglio.PLACCHE_PREVENTIVO + "')  order by " + Aree.ID_UNITA + " desc,"
				+ tabLoc.getNomeCampoTabella(Locali.ID_AREA) + " desc," + tabLoc.getNomeCampoTabella(Locali.NOME)
				+ "," + Elementi.ID_CATEGORIA_GENERALE + "," + Componenti.ID_CATEGORIA_COMPONENTE + ","
				+ PreventiviDettaglio.DESCRIZIONE;

		// String SQL_TUBI_CAVI = "Select " + tabPrev.getNomeCampoTabella("*") + "," +
		// tabLoc.getNomeCampoTabella(Locali.NOME) + " from "
		// + PreventiviDettaglio.NOME_TABELLA + j1.getSQLJoin() + j2.getSQLJoin() + j3.getSQLJoin() + " where "
		// + PreventiviDettaglio.ID_PREVENTIVO + "=" + idPreventivo + " and " + PreventiviDettaglio.TIPO + " in ('"
		// + PreventiviDettaglio.MATERIALE + "','" + PreventiviDettaglio.MATERIALE_PREVENTIVO + "','" +
		// PreventiviDettaglio.PLACCHE
		// + "','" + PreventiviDettaglio.PLACCHE_PREVENTIVO + "') and " + Elementi.ID_CATEGORIA_GENERALE + "  in ("
		// + CategorieGenerali.TUBI + "," + CategorieGenerali.CAVI + ") order by " + PreventiviDettaglio.ID_LOCALE + ","
		// + Componenti.ID_CATEGORIA_COMPONENTE + "," + Elementi.ID_CATEGORIA_GENERALE + "," +
		// PreventiviDettaglio.DESCRIZIONE;

		System.out.println("EConTab: PreventivoDettaglioFragment ricerca SQL "+ SQL);

		ArrayList<Object> records = db.eseguiSelect(SQL, null);
		// ArrayList<Object> recordsTubi = db.eseguiSelect(SQL_TUBI_CAVI, null);

		// records.addAll(recordsTubi);

		System.out.println("EConTab: PreventivoDettaglioFragment ricerca records.size() "+ records.size());

		int idLocalePrec = Integer.MIN_VALUE;
		String tipoRigaPrec = "";
		for (int i = 0; i < records.size(); i++) {
			ContentValues recCurr = (ContentValues) records.get(i);
			ContentValues valLinea = null;

			System.out.println("EConTab: PreventivoDettaglioFragment ricerca id_elemento ----> " + recCurr.getAsInteger(PreventiviDettaglio.ID_ELEMENTO));
			System.out.println("EConTab: PreventivoDettaglioFragment ricerca id_componente ----> " + recCurr.getAsInteger(PreventiviDettaglio.ID_COMPONENTE));

			int idLocale = recCurr.getAsInteger(PreventiviDettaglio.ID_LOCALE);
			String tipoRiga = recCurr.getAsString(PreventiviDettaglio.TIPO);

			if (tipoRiga.equals(PreventiviDettaglio.PLACCHE) || tipoRiga.equals(PreventiviDettaglio.PLACCHE_PREVENTIVO)) {
				System.out.println("EConTab: PreventivoDettaglioFragment ricerca PLACCHE");
				PlaccheModuli tabPlacche = new PlaccheModuli();
				ContentValues valPlacche = tabPlacche.getRecordPlacca(db, recCurr.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI));
				if (valPlacche != null) {
					recCurr.put(PlaccheModuli.NUMERO_MODULI, valPlacche.getAsInteger(PlaccheModuli.NUMERO_MODULI));
				} else {
					recCurr.put(PlaccheModuli.NUMERO_MODULI, 0);
				}
			}

			if (idLocale != 0) {
				// prendo la linea da locale/area/unit�
				valLinea = tabLoc.getLineaLocale(db, idLocale);

			} else {
				// prendo la linea dal cantiere
				if (valLineaCant == null) {
					Cantieri tabCant = new Cantieri();
					valLinea = tabCant.getLineaCantiere(db, idCantiere);
					if (valLinea != null) {
						valLineaCant = new ContentValues();
						valLineaCant.putAll(valLinea);
					}

				} else {
					valLinea = new ContentValues();
					valLinea.putAll(valLineaCant);
				}
			}

			System.out.println("EConTab: PreventivoDettaglioFragment ricerca valLinea " + valLinea);

			if (!tipoRiga.equals(PreventiviDettaglio.PLACCHE) && !tipoRiga.equals(PreventiviDettaglio.PLACCHE_PREVENTIVO)) {
				System.out.println("EConTab: PreventivoDettaglioFragment ricerca NO PLACCHE");
				//System.out.println("EConTab: PreventivoDettaglioFragment ricerca recCurr.get(PreventiviDettaglio.ID_LINEA) " + recCurr.get(PreventiviDettaglio.ID_LINEA));
				if (recCurr.get(PreventiviDettaglio.ID_LINEA) != null && !recCurr.getAsString(PreventiviDettaglio.ID_LINEA).equals("null")) {
					int idLineaPrev = recCurr.getAsInteger(PreventiviDettaglio.ID_LINEA);
					System.out.println("EConTab: PreventivoDettaglioFragment ricerca idLineaPrev " + idLineaPrev);
					if (idLineaPrev != 0 && (valLinea == null || idLineaPrev != valLinea.getAsInteger(Linee.ID_LINEA))) {
						ContentValues whereLinea = new ContentValues();
						whereLinea.put(Linee.ID_LINEA, idLineaPrev);
						valLinea = db.getRecord(new Linee(), whereLinea);
						if (valLinea != null) {
							recCurr.put("DESCRI_LINEA", " (linea: " + valLinea.getAsString(Linee.NOME_LINEA) + ")");
						}

					}
				}
			}
			if (valLinea == null) {
				valLinea = new ContentValues();
				valLinea.put(Costruttori.SIGLA_METEL, "");
				valLinea.put(Linee.ID_LINEA, 0);
				valLinea.put(Linee.NOME_LINEA, "");
			}

			recCurr.putAll(valLinea);
			if (idLocale != idLocalePrec) {

				idLocalePrec = idLocale;

				tipoRigaPrec = tipoRiga;
				ContentValues valLocale = new ContentValues();
				valLocale.put(RIGA_LOCALE, "SI");

				valLocale.put(Locali.ID_LOCALE, idLocalePrec);
				valLocale.put(PreventiviDettaglio.TIPO, tipoRigaPrec);
				if (idLocale != 0) {
					valLocale.put("nomeArea", recCurr.getAsString("nomeArea"));
					valLocale.put(Locali.NOME, recCurr.getAsString(Locali.NOME));
				} else {
					valLocale.put("nomeArea","");
					valLocale.put(Locali.NOME, getString(R.string.altro_materiale));

				}
				valLocale.putAll(valLinea);
				dati.add(valLocale);

			}

			if (righeInModifica != null && righeInModifica.containsKey(recCurr.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO))) {
				recCurr.putAll(righeInModifica.get(recCurr.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO)));
			}
			dati.add(recCurr);
		}

		if(pfa.app.econtab.Globals.EXTRA_CAVI_TUBI_IN_PREVENTIVO == true) {
			List<ContentValues> extra_cables = new ArrayList<ContentValues>();
			// DL: Extra cavi e tubi
			String SQLExtraCaviTubi = "Select * from " + ElementiCantiere.NOME_TABELLA + " where "
					+ PreventiviDettaglio.ID_PREVENTIVO + "=" + idPreventivo + " order by " + ElementiCantiere.ID_ELEMENTO_CANT;
			System.out.println("EConTab: PreventivoDettaglioFragment ricerca SQLExtraCaviTubi " + SQLExtraCaviTubi);

			ArrayList<Object> recordExtraCaviTubi = db.eseguiSelect(SQLExtraCaviTubi, null);
			System.out.println("EConTab: PreventivoDettaglioFragment ricerca recordExtraCaviTubi.size() " + recordExtraCaviTubi.size());
			boolean header_added = false;
			int add_counter = 0;
			for (int i = 0; i < recordExtraCaviTubi.size(); i++) {
				ContentValues recCurr = (ContentValues) recordExtraCaviTubi.get(i);
				String data_ins = recCurr.getAsString(ElementiCantiere.DATA_INS);
				data_ins = data_ins.substring(0,8);
				System.out.println("EConTab: PreventivoDettaglioFragment ricerca ------------------------------------------------");
				System.out.println("EConTab: PreventivoDettaglioFragment ricerca data_ins      " + data_ins);
				int data_ins_number = 0;
				try {
					data_ins_number = Integer.parseInt(data_ins);
				} catch (NumberFormatException e) {
					data_ins_number = 19700101;
				}
				if(data_ins_number < pfa.app.econtab.Globals.EXTRA_CAVI_TUBI_IN_PREVENTIVO_SINCE_DATE)
				{
					continue;
				}
				int id_elemento = recCurr.getAsInteger(ElementiCantiere.ID_ELEMENTO);
				float metri_cavo_cant = recCurr.getAsFloat(ElementiCantiere.METRI_CAVO_CANT);
				int id_elemento_cavo = recCurr.getAsInteger(ElementiCantiere.ID_ELEMENTO_CAVO);
				float metri_tubo_cant = recCurr.getAsFloat(ElementiCantiere.METRI_TUBO_CANT);
				int id_elemento_tubo = recCurr.getAsInteger(ElementiCantiere.ID_ELEMENTO_TUBO);
				String nome_elemento_cant = recCurr.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT);
				int id_locale = recCurr.getAsInteger(ElementiCantiere.ID_LOCALE);
				String unita_misura = recCurr.getAsString(ElementiCantiere.UNITA_MISURA);
				// DL: test
				//id_elemento_cavo = 2;
				//id_elemento_tubo = 3;
				System.out.println("EConTab: PreventivoDettaglioFragment ricerca id_elemento      " + id_elemento);
				System.out.println("EConTab: PreventivoDettaglioFragment ricerca metri_cavo_cant  " + metri_cavo_cant);
				System.out.println("EConTab: PreventivoDettaglioFragment ricerca id_elemento_cavo " + id_elemento_cavo);
				System.out.println("EConTab: PreventivoDettaglioFragment ricerca metri_tubo_cant  " + metri_tubo_cant);
				System.out.println("EConTab: PreventivoDettaglioFragment ricerca id_elemento_tubo " + id_elemento_tubo);
				// do something only if metri_cavo e id_cavo are different than zero
				if(id_elemento_cavo > 0 && metri_cavo_cant > 0) {
					if(header_added == false)
					{
						ContentValues valLocale = new ContentValues();
						valLocale.put(RIGA_LOCALE, "SI");
						valLocale.put(Locali.ID_LOCALE, 0);
						valLocale.put(Locali.NOME, getString(R.string.extra_cables));
						valLocale.put(PreventiviDettaglio.TIPO, PreventiviDettaglio.EXTRA);
						valLocale.put(Costruttori.SIGLA_METEL, "");
						valLocale.put(Linee.ID_LINEA, 0);
						valLocale.put(Linee.NOME_LINEA, "");
						dati.add(valLocale);
						header_added = true;
					}
					// search in the local list if we already found this elemento
					int elemento_found_index = -1;
					for (int j = 0; j < extra_cables.size(); j++) {
						ContentValues temp = extra_cables.get(j);
						if(temp.getAsInteger(PreventiviDettaglio.ID_ELEMENTO) == id_elemento_cavo)
						{
							elemento_found_index = j;
							break;
						}
					}
					if(elemento_found_index < 0) {
						String SQLElementoCodiceCavo = "Select * from " + ElementiCodici.NOME_TABELLA + " where "
								+ ElementiCodici.ID_ELEMENTO + "=" + id_elemento_cavo;
						System.out.println("EConTab: PreventivoDettaglioFragment ricerca SQLElementoCodice " + SQLElementoCodiceCavo);
						ArrayList<Object> recordElementoCodiceCavo = db.eseguiSelect(SQLElementoCodiceCavo, null);
						System.out.println("EConTab: PreventivoDettaglioFragment ricerca recordElementoCodiceCavo.size() " + recordElementoCodiceCavo.size());
						if (recordElementoCodiceCavo.size() == 1) {
							ContentValues recCavo = (ContentValues) recordElementoCodiceCavo.get(0);
							String codice_articolo = recCavo.getAsString(ElementiCodici.CODICE_ARTICOLO);
							String SQLElementoCodiceArticolo = "Select * from " + Listini.NOME_TABELLA + " where "
									+ Listini.CODICE_ARTICOLO + "='" + codice_articolo + "'";
							System.out.println("EConTab: PreventivoDettaglioFragment ricerca SQLElementoCodiceArticolo " + SQLElementoCodiceArticolo);
							ArrayList<Object> recordElementoCodiceArticolo = db.eseguiSelect(SQLElementoCodiceArticolo, null);
							System.out.println("EConTab: PreventivoDettaglioFragment ricerca recordElementoCodiceArticolo.size() " + recordElementoCodiceArticolo.size());
							if (recordElementoCodiceArticolo.size() == 1) {
								ContentValues recArticolo = (ContentValues) recordElementoCodiceArticolo.get(0);
								float prezzo_acq = recArticolo.getAsFloat(Listini.PRZ_ULTIMO_ACQUISTO);
								float prezzo_vendita = recArticolo.getAsFloat(Listini.PRZ_LISTINO);
								float sconto = recArticolo.getAsFloat(Listini.SCONTO);
								String descrizione = recArticolo.getAsString(Listini.DESCRIZIONE);
								ContentValues fakeDettaglioPreventivo = new ContentValues();
								fakeDettaglioPreventivo.put(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO, (1000000 + add_counter));
								fakeDettaglioPreventivo.put(PreventiviDettaglio.ID_PREVENTIVO, idPreventivo);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.TIPO, PreventiviDettaglio.MATERIALE);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.DESCRIZIONE, descrizione);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.ID_LOCALE, id_locale);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.ID_ELEMENTO, id_elemento_cavo);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.ID_COMPONENTE, 0);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.ID_PLACCA_MODULI, 0);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.CODICE_ARTICOLO, codice_articolo);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.ID_MANODOPERA, 0);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.NUM_OPERATORI, 0);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.UNITA_MISURA, unita_misura);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.QUANTITA, (int) (metri_cavo_cant));
								fakeDettaglioPreventivo.put(PreventiviDettaglio.PREZZO_ACQ, prezzo_acq);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.RICARICO, 0);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.PREZZO_VEN, prezzo_vendita);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.SCONTO, sconto);
								float prezzo = prezzo_vendita * (int) (metri_cavo_cant);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.PREZZO, prezzo);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.CODICE_IVA, 20.0);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.STATO, sconto);
								fakeDettaglioPreventivo.put(PreventiviDettaglio.NOTE, "");
								extra_cables.add(fakeDettaglioPreventivo);
							}
						}
					}
					else
					{
						ContentValues temp = extra_cables.get(elemento_found_index);
						int current_quantity = temp.getAsInteger(PreventiviDettaglio.QUANTITA);
						float current_price = temp.getAsFloat(PreventiviDettaglio.PREZZO_VEN);
						System.out.println("EConTab: PreventivoDettaglioFragment ricerca current_quantity " + current_quantity);
						System.out.println("EConTab: PreventivoDettaglioFragment ricerca current_price " + current_price);
						current_quantity = current_quantity + (int) (metri_cavo_cant);
						float prezzo = current_price * current_quantity;
						temp.put(PreventiviDettaglio.QUANTITA, current_quantity);
						temp.put(PreventiviDettaglio.PREZZO, prezzo);
					}
				}
			}
			for (int j = 0; j < extra_cables.size(); j++) {
				ContentValues temp = extra_cables.get(j);
				dati.add(temp);
			}
		}

		// prendo i tubi e i cavi dai collegamenti (caso ordini)
		String SQLCOLLEGAMENTI = "Select " + tabPrev.getNomeCampoTabella("*") + " from " + PreventiviDettaglio.NOME_TABELLA + " where "
				+ PreventiviDettaglio.ID_PREVENTIVO + "=" + idPreventivo + " and " + PreventiviDettaglio.TIPO + " = '"
				+ PreventiviDettaglio.COLLEGAMENTI + "' order by " + PreventiviDettaglio.DESCRIZIONE;

		System.out.println("EConTab: PreventivoDettaglioFragment ricerca SQLCOLLEGAMENTI " + SQLCOLLEGAMENTI);

		ArrayList<Object> recordsCollegamenti = db.eseguiSelect(SQLCOLLEGAMENTI, null);
		if (recordsCollegamenti.size() > 0) {
			ContentValues valLocale = new ContentValues();
			valLocale.put(RIGA_LOCALE, "SI");
			valLocale.put(Locali.ID_LOCALE, 0);
			valLocale.put(Locali.NOME, getString(R.string.collegamenti));
			valLocale.put(PreventiviDettaglio.TIPO, PreventiviDettaglio.COLLEGAMENTI);
			dati.add(valLocale);
		}
		ContentValues valLineaColl = new ContentValues();
		valLineaColl.put(Costruttori.SIGLA_METEL, "");
		valLineaColl.put(Linee.ID_LINEA, 0);
		valLineaColl.put(Linee.NOME_LINEA, "");

		System.out.println("EConTab: PreventivoDettaglioFragment ricerca recordsCollegamenti.size() " + recordsCollegamenti.size());

		for (int i = 0; i < recordsCollegamenti.size(); i++) {
			ContentValues recCurr = (ContentValues) recordsCollegamenti.get(i);
			recCurr.putAll(valLineaColl);
			if (righeInModifica != null && righeInModifica.containsKey(recCurr.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO))) {
				recCurr.putAll(righeInModifica.get(recCurr.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO)));
			}

			dati.add(recCurr);
		}

		// prendo la manopera

		String SQLMANO = "Select " + tabPrev.getNomeCampoTabella("*") + " from " + PreventiviDettaglio.NOME_TABELLA + " where "
				+ PreventiviDettaglio.ID_PREVENTIVO + "=" + idPreventivo + " and " + PreventiviDettaglio.TIPO + " = '"
				+ PreventiviDettaglio.MANOPERA + "' order by " + PreventiviDettaglio.DESCRIZIONE;

		System.out.println("EConTab: PreventivoDettaglioFragment ricerca SQLMANO " + SQLMANO);

		ArrayList<Object> recordsMano = db.eseguiSelect(SQLMANO, null);
		if (recordsMano.size() > 0) {
			ContentValues valLocale = new ContentValues();
			valLocale.put(RIGA_LOCALE, "SI");
			valLocale.put(Locali.ID_LOCALE, 0);
			valLocale.put(Locali.NOME, getString(R.string.manodopera));
			valLocale.put(PreventiviDettaglio.TIPO, PreventiviDettaglio.MANOPERA);
			dati.add(valLocale);
		}
		System.out.println("EConTab: PreventivoDettaglioFragment ricerca recordsMano.size() "+ recordsMano.size());
		for (int i = 0; i < recordsMano.size(); i++) {
			ContentValues recCurr = (ContentValues) recordsMano.get(i);
			if (righeInModifica != null && righeInModifica.containsKey(recCurr.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO))) {
				recCurr.putAll(righeInModifica.get(recCurr.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO)));
			}

            //se sono in un ordine aggiungo i valori dei rapportini
            if (tipoPreventivoOrdine.equals(Preventivi.TIPO_ORDINE)){
                String SQLRAPP = "Select Sum("+ RapportiniDettaglio.ORE+") as ore_rapp from " + RapportiniDettaglio.NOME_TABELLA+" inner join "+ Rapportini.NOME_TABELLA+" on "+Rapportini.NOME_TABELLA+"."+Rapportini.ID_RAPPORTINO+"="+RapportiniDettaglio.NOME_TABELLA+"."+RapportiniDettaglio.ID_RAPPORTINO+" where "+RapportiniDettaglio.ID_MANODOPERA+"="+recCurr.getAsInteger(PreventiviDettaglio.ID_MANODOPERA)+" and " + Rapportini.ID_ORDINE+"="+idPreventivo;
                ArrayList<Object> rapp = db.eseguiSelect(SQLRAPP,null);
                double przUni = recCurr.getAsDouble(PreventiviDettaglio.PREZZO);
                double tot_ore = 0;
                if (rapp.size()>0 && ((ContentValues)rapp.get(0)).getAsDouble("ore_rapp")!=null){

                     tot_ore = ((ContentValues)rapp.get(0)).getAsDouble("ore_rapp");
                }
                double importoRapp = Utility.arrotonda(tot_ore*przUni,2);
                recCurr.put("ORE_RAPP",tot_ore);
                recCurr.put("IMPORTO_RAPP",importoRapp);
            }

			dati.add(recCurr);
		}

		// prendo le note
		String SQLNOTE = "Select " + tabPrev.getNomeCampoTabella("*") + " from " + PreventiviDettaglio.NOME_TABELLA + " where "
				+ PreventiviDettaglio.ID_PREVENTIVO + "=" + idPreventivo + " and " + PreventiviDettaglio.TIPO + " = '"
				+ PreventiviDettaglio.ALTRO + "' order by " + PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO;

		System.out.println("EConTab: PreventivoDettaglioFragment ricerca SQLNOTE " + SQLNOTE);

		ArrayList<Object> recordsNote = db.eseguiSelect(SQLNOTE, null);
		if (recordsNote.size() > 0) {
			ContentValues valLocale = new ContentValues();
			valLocale.put(RIGA_LOCALE, "SI");
			valLocale.put(Locali.ID_LOCALE, 0);
			valLocale.put(Locali.NOME, getString(R.string.note));
			valLocale.put(PreventiviDettaglio.TIPO, PreventiviDettaglio.ALTRO);
			dati.add(valLocale);
		}
		System.out.println("EConTab: PreventivoDettaglioFragment ricerca recordsNote.size() "+ recordsNote.size());
		for (int i = 0; i < recordsNote.size(); i++) {
			ContentValues recCurr = (ContentValues) recordsNote.get(i);
			if (righeInModifica != null && righeInModifica.containsKey(recCurr.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO))) {
				recCurr.putAll(righeInModifica.get(recCurr.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO)));
			}
			dati.add(recCurr);

		}

		db.close();

		if (adapter == null) {
			adapter = new PreventiviDettaglioAdapter(getActivity(), dati);
			adapter.setFragmnent(this);
			adapter.collegaTestata((ViewGroup) getView().findViewById(R.id.testata_dettaglio));
			lista.setAdapter(adapter);
			lista.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
			// registerForContextMenu(lista);
		} else {
			adapter.notifyDataSetChanged();

		}

		ricalcolaTotali();

		System.out.println("EConTab: PreventivoDettaglioFragment ricerca EXIT");
	}



	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

		if (v.getId() == R.id.button_nuovo) {
			nuovaRiga();
		}

		if (v.getId() == R.id.button_associa) {
			associaCodici();
		}

		if (v.getId() == R.id.button_esporta) {
			String[] opzioni = null;
			// if (tipoPreventivoOrdine.equals(Preventivi.TIPO_ORDINE)) {
			opzioni = new String[6];
			opzioni[0] = getString(R.string.esporta_materiale_xls);
            if (tipoPreventivoOrdine.equals(Preventivi.TIPO_ORDINE)){
                opzioni[1] = getString(R.string.esporta_ordine_xls);
            }
            else{
                opzioni[1] = getString(R.string.esporta_preventivo_xls);
            }

			opzioni[2] = getString(R.string.esporta_scatole_xls);
			opzioni[3] = getString(R.string.esporta_scatole_xls_senzapiantine);
			opzioni[4] = getString(R.string.importa_prezzi);
            opzioni[5] = getString(R.string.esporta_consuntivo_xls);
			// } else {
			// opzioni = new String[3];
			// opzioni[0] = getString(R.string.esporta_materiale_xls);
			// opzioni[1] = getString(R.string.esporta_preventivo_xls);
			// opzioni[2] = getString(R.string.importa_prezzi);
			// }
			Utility.mostraSelezioneDialog("", opzioni, getEConTabActivity(), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					if (which == 0) {
						esportaMaterialeXLS();
					}

					if (which == 1) {
                        if (Sessione.isLicenzaGratis(getEConTabActivity())){
                            Utility.mostraConfermaDialog("Upgrade EConTab",getActivity().getString(R.string.messaggio_licenza_funzionalita),getActivity(),getString(R.string.abbonati),getString(R.string.annulla),new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (i==DialogInterface.BUTTON_POSITIVE){
                                        Intent intent = new Intent(getActivity(), GestAbbonamentoActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                        startActivity(intent);
                                    }
                                }
                            });
                        }
                        else{
                            esportaPreventivoXLS();
                        }

					}
					// if (tipoPreventivoOrdine.equals(Preventivi.TIPO_ORDINE)) {
					if (which == 2) {
						esportaComposizioneScatoleXLS(true);
					}
					if (which == 3) {
						esportaComposizioneScatoleXLS(false);
					}
					// }

					if (which == 4) {
                        if (Sessione.isLicenzaGratis(getEConTabActivity())){
                            Utility.mostraConfermaDialog("Upgrade EConTab",getActivity().getString(R.string.messaggio_licenza_funzionalita),getActivity(),getString(R.string.abbonati),getString(R.string.annulla),new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (i==DialogInterface.BUTTON_POSITIVE){
                                        Intent intent = new Intent(getActivity(), GestAbbonamentoActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                        startActivity(intent);
                                    }
                                }
                            });
                        }
                        else {
                            if (modificheBloccate) {
                                Toast.makeText(getActivity(), getString(R.string.modifiche_non_permesse), Toast.LENGTH_SHORT).show();
                            } else {
                                importaPrezziDaXLS();
                            }
                        }

					}

                    if (which == 5){
                        if (Sessione.isLicenzaGratis(getEConTabActivity())){
                            Utility.mostraConfermaDialog("Upgrade EConTab",getActivity().getString(R.string.messaggio_licenza_funzionalita),getActivity(),getString(R.string.abbonati),getString(R.string.annulla),new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (i==DialogInterface.BUTTON_POSITIVE){
                                        Intent intent = new Intent(getActivity(), GestAbbonamentoActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                        startActivity(intent);
                                    }
                                }
                            });
                        }
                        else{
                            //prendo gli ordini per questo cantiere
                            DbInterno db = new DbInterno(getActivity());
                            Preventivi tabPrev = new Preventivi();
                            ContentValues whereOrdCant = new ContentValues();
                            whereOrdCant.put(Preventivi.ID_CANTIERE, idCantiere);
                            whereOrdCant.put(Preventivi.TIPO, Preventivi.TIPO_ORDINE);
                            final ArrayList<Object> listaOrdini = db.eseguiSelect(new Preventivi(), whereOrdCant, new String[]{Preventivi.DATA + " desc"});
                            db.close();
                            String[] items = new String[listaOrdini.size()];
                            final boolean[] selezionati = new boolean[listaOrdini.size()];
                            for (int i=0;i<listaOrdini.size();i++){
                                ContentValues currOrd = (ContentValues)listaOrdini.get(i);
                                int numero = currOrd.getAsInteger(Preventivi.NUMERO);
                                String data = Utility.numberToData(currOrd.getAsLong(Preventivi.DATA));
                                String titolo = currOrd.getAsString(Preventivi.TITOLO);
                                if (titolo.length() > 0) {
                                    titolo = System.getProperty("line.separator") + titolo;
                                }
                                String stato = currOrd.getAsString(Preventivi.STATO);
                                stato = tabPrev.getDescrizioneStatoPreventivo(stato, getActivity()).toString();
                                items[i] = getString(R.string.ordine_num_del, numero, data).trim() + " (" + stato + ")" + titolo;

                                if (idPreventivo== currOrd.getAsInteger(Preventivi.ID_PREVENTIVO)){
                                    selezionati[i] = true;
                                }
                            }



                            Utility.mostraSelezioneMultiplaDialog(getString(R.string.seleziona_ordini_da_consuntivare),items,selezionati,getActivity(),getString(R.string.conferma), getString(R.string.annulla), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int which) {
                                    if (which==DialogInterface.BUTTON_POSITIVE){
                                        ArrayList<Integer> ordiniSelezionati = new ArrayList<Integer>();
                                        for (int i=0;i<selezionati.length;i++){
                                            if (selezionati[i]){
                                                ContentValues curr = (ContentValues)listaOrdini.get(i);
                                                ordiniSelezionati.add(curr.getAsInteger(Preventivi.ID_PREVENTIVO));
                                            }
                                        }

                                        if (ordiniSelezionati.size()==0){

                                           Toast t =  Toast.makeText(getActivity(), getString(R.string.messaggio_selezione_ordine), Toast.LENGTH_LONG);
                                            t.setGravity(Gravity.CENTER,0,0);
                                            t.show();
                                        }
                                        else{
                                            esportaConsuntivoXLS(ordiniSelezionati);
                                        }

                                    }
                                }
                            });
                        }
                    }
				}
			});

		}

		if (v.getId() == R.id.button_salva_prev) {
			Utility.mostraConfermaSalvataggioDialog(getEConTabActivity(), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					if (which == DialogInterface.BUTTON_POSITIVE) {
						salvaModifiche();
					}

				}
			});

		}

		if (v.getId() == R.id.button_ricalcola){
			Utility.mostraConfermaDialog(getString(R.string.attenzione), getString(R.string.conferma_ricalcolo_preventivo_ordine), getEConTabActivity(), getString(R.string.conferma), getString(R.string.annulla), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE) {

					}
				}
			});
		}
	}

	protected void importaPrezziDaXLS() {
		// TODO Auto-generated method stub
		// File dirImport = new File(Environment.getExternalStorageDirectory() + File.separator + "ECONTAB" +
		// File.separator
		// + PreventiviDettaglio.PATH_EXPORT);

		Intent intent = new Intent(getActivity(), EConTabFileChooserActivity.class);
		intent.putExtra("ESTENSIONE", "xls");
		getActivity().startActivityForResult(intent, 100);
	}

	public void importaDaFileXLS(final String path) {
		View campi = View.inflate(getActivity(), R.layout.dialog_ricarico_layout, null);
		final EditText editRicarico = (EditText) campi.findViewById(R.id.editText1);

		editRicarico.setText("5");
		Utility.mostraDialogPersonalizzato(getString(R.string.ricarico_percentuale), getActivity(), campi, getString(R.string.conferma),
				getString(R.string.annulla), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						if (which == DialogInterface.BUTTON_POSITIVE) {
							_eseguiImportazioneCodiciPrezzi(path, editRicarico.getText().toString());
						}
					}
				});
	}

	private void esportaComposizioneScatoleXLS(boolean conPiantine) {
		// TODO Auto-generated method stub
		if (getActivity() instanceof CantiereSplitActivity) {
			if (conPiantine) {
                if (Sessione.isLicenzaGratis(getEConTabActivity())){
                    Utility.mostraConfermaDialog("Upgrade EConTab",getActivity().getString(R.string.messaggio_licenza_funzionalita),getActivity(),getString(R.string.abbonati),getString(R.string.annulla),new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i==DialogInterface.BUTTON_POSITIVE){
                                Intent intent = new Intent(getActivity(), GestAbbonamentoActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                startActivity(intent);
                            }
                        }
                    });
                }
                else{
                    ((CantiereSplitActivity) getActivity()).creaImmaginiPiantine(0, idPreventivo);
                }

			} else {
				((CantiereSplitActivity) getActivity()).stampaComposizioneScatole(idPreventivo, false);
			}

		}
	}

	private void associaCodici() {
		// TODO Auto-generated method stub

        String messaggio = getString(R.string.messaggio_associazione_codici_preventivo);
        if (tipoPreventivoOrdine.equals(Preventivi.TIPO_ORDINE)){
            messaggio = getString(R.string.messaggio_associazione_codici_ordini);
        }

		Utility.mostraConfermaDialog(getString(R.string.attenzione),messaggio,
				getEConTabActivity(), getString(R.string.conferma), getString(R.string.annulla), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						if (which == DialogInterface.BUTTON_POSITIVE) {
							String errore = "";
							DbInterno db = new DbInterno(getEConTabActivity());
							db.getReadableDatabase().beginTransaction();
							try {
								PreventiviDettaglio tabPrev = new PreventiviDettaglio();
								ArrayList<ContentValues> daSalvare = new ArrayList<ContentValues>();
								daSalvare.addAll(adapter.getRigheInModifica().values());

								for (int i = 0; i < daSalvare.size(); i++) {

									ContentValues valUpd = tabPrev.getValoriLogModifica(db);
									ContentValues valori = daSalvare.get(i);
									for (int c = 0; c < tabPrev.getNomiCampi().size(); c++) {
										String nomeCampo = tabPrev.getNomiCampi().get(c);
										if (valori.containsKey(nomeCampo) && !tabPrev.isChiave(nomeCampo)) {
											if (tabPrev.getTipoCampo(nomeCampo).equals(tabPrev.INTEGER)) {
												valUpd.put(nomeCampo, valori.getAsInteger(nomeCampo));
											}
											if (tabPrev.getTipoCampo(nomeCampo).equals(tabPrev.TEXT)) {
												valUpd.put(nomeCampo, valori.getAsString(nomeCampo));
											}
											if (tabPrev.getTipoCampo(nomeCampo).equals(tabPrev.NUMERIC)) {
												valUpd.put(nomeCampo, valori.getAsFloat(nomeCampo));
											}
										}
									}
									ContentValues whereUpd = new ContentValues();
									whereUpd.put(tabPrev.ID_PREVENTIVO_DETTAGLIO, valori.getAsInteger(tabPrev.ID_PREVENTIVO_DETTAGLIO));
									tabPrev.aggiornaRecord(db, valUpd, whereUpd);

									//tabPrev.aggiornaRigaPreventivo(db, idPreventivo, daSalvare.get(i), 0, null, false);
								}

								tabPrev.riassociaCodiciPreventivo(db, idPreventivo);

								db.getReadableDatabase().setTransactionSuccessful();
							} catch (Exception e) {
								errore = Log.getStackTraceString(e);
							} finally {
								db.getReadableDatabase().endTransaction();
								db.close();
							}

							if (errore.equals("")) {
								// faccio il clear prima della ricerca per pulire i flag delle righe in modifca
								dati.clear();
								if (righeInModifica.getAnimation() != null) {
									righeInModifica.getAnimation().cancel();
								}

								righeInModifica.setVisibility(View.GONE);
								buttonSalva.setBackgroundResource(R.drawable.bg_bottone_rotondo);
								ricerca();
							} else {
								Toast.makeText(getEConTabActivity(), errore, Toast.LENGTH_LONG).show();
							}

						}

					}
				});
	}

	private void esportaMaterialeXLS() {
		// new class for asynchronous task
		AsyncTaskExecutorService task = new AsyncTaskExecutorService() {
			AlertDialog di = null;
			@Override
			protected void onPreExecute() {
				AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
				ab.setMessage(getActivity().getResources().getString(R.string.messaggio_creazione_report));
				di = ab.create();
				di.show();
			}

			@Override
			protected Object doInBackground(Object o) {
				PreventivoXLS exp = new PreventivoXLS(getEConTabActivity());
				try {
					String percorsoFile = exp.generaReportMateriale(idPreventivo);
					return percorsoFile;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Object o) {
				di.cancel();
				if (o!=null){
					String percorsoFile = o.toString();
					File fileExport = new File(percorsoFile);
					System.out.println("EConTab: PrevebtivoDettaglioFragment esportaMaterialeXLS fileExport " + fileExport);
					try{
						Intent intent = new Intent(Intent.ACTION_VIEW);
						// DL: porting to Android 13
						// Uri photoURI = Uri.fromFile(createImageFile());
						// Uri photoURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", createImageFile());
						//intent.setDataAndType(Uri.fromFile(fileExport), Utility.XLS);
						intent.setDataAndType(FileProvider.getUriForFile(getEConTabActivity(), getEConTabActivity().getApplicationContext().getPackageName() + ".provider", fileExport), Utility.XLS);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						// DL: If you're using an intent to make the system open your file, you may need to add the following line of code
						intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						startActivity(intent);
					}
					catch (ActivityNotFoundException activityNotFound){
						System.out.println("EConTab: PrevebtivoDettaglioFragment esportaMaterialeXLS ActivityNotFoundException");
					}
				}
				else{
					Toast.makeText(getEConTabActivity(), "Errore nella generazione del file xls" , Toast.LENGTH_SHORT).show();
				}
			}
		};
		task.execute();
	}

	private void esportaPreventivoXLS() {
		// new class for asynchronous task
		AsyncTaskExecutorService task = new AsyncTaskExecutorService() {
			AlertDialog di = null;
			@Override
			protected void onPreExecute() {
				AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
				ab.setMessage(getActivity().getResources().getString(R.string.messaggio_creazione_report));
				di = ab.create();
				di.show();
			}

			@Override
			protected Object doInBackground(Object o) {
				PreventivoXLS exp = new PreventivoXLS(getEConTabActivity());
				try {
					String percorsoFile = exp.generaReportPreventivo(idPreventivo);
					return percorsoFile;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Object o) {
				di.cancel();
				if (o!=null){
					String percorsoFile = o.toString();
					File fileExport = new File(percorsoFile);
					System.out.println("EConTab: PrevebtivoDettaglioFragment esportaPreventivoXLS fileExport " + fileExport);
					try{
						Intent intent = new Intent(Intent.ACTION_VIEW);
						// DL: porting to Android 13
						// Uri photoURI = Uri.fromFile(createImageFile());
						// Uri photoURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", createImageFile());
						//intent.setDataAndType(Uri.fromFile(fileExport), Utility.XLS);
						intent.setDataAndType(FileProvider.getUriForFile(getEConTabActivity(), getEConTabActivity().getApplicationContext().getPackageName() + ".provider", fileExport), Utility.XLS);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						// DL: If you're using an intent to make the system open your file, you may need to add the following line of code
						intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						startActivity(intent);
					}
					catch (ActivityNotFoundException activityNotFound){
						System.out.println("EConTab: PrevebtivoDettaglioFragment esportaPreventivoXLS ActivityNotFoundException");
					}
				}
				else{
					Toast.makeText(getEConTabActivity(), "Errore nella generazione del file xls", Toast.LENGTH_SHORT)
							.show();
				}
			}
		};
		task.execute();
	}

    private void esportaConsuntivoXLS(final ArrayList<Integer> listaOrdini) {
		// new class for asynchronous task
		AsyncTaskExecutorService task = new AsyncTaskExecutorService() {
			AlertDialog di = null;
			@Override
			protected void onPreExecute() {
				AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
				ab.setMessage(getActivity().getResources().getString(R.string.messaggio_creazione_report));
				di = ab.create();
				di.show();
			}

			@Override
			protected Object doInBackground(Object o) {
				ConsuntivoXLS exp = new ConsuntivoXLS(getEConTabActivity());
				try {
					String percorsoFile = exp.generaReport(listaOrdini);
					return percorsoFile;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Object o) {
				di.cancel();
				if (o!=null){
					String percorsoFile = o.toString();
					//EConTab: PrevebtivoDettaglioFragment esportaMaterialeXLS fileExport /storage/emulated/0/ECONTAB/ABC Spa/preventivi/mat_ord_1_2025.xls
					File fileExport = new File(percorsoFile);
					Intent intent = new Intent(Intent.ACTION_VIEW);
					Uri uri = FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".provider",fileExport);
					if(uri != null)
					{
						intent.setDataAndType(uri, Utility.XLS);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						startActivity(intent);
					}
				}
				else{
					Toast.makeText(getEConTabActivity(), "Errore nella generazione del file xls", Toast.LENGTH_SHORT)
							.show();
				}
			}
		};
		task.execute();
    }

	private void nuovaRiga() {
		// TODO Auto-generated method stub
		String[] items = new String[4];
		items[0] = getString(R.string.materiale);
		items[1] = getString(R.string.placca);
		items[2] = getString(R.string.manodopera);
		items[3] = getString(R.string.nota);

		Utility.mostraSelezioneDialog(getString(R.string.nuova_riga_prev), items, getActivity(), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if (which == 0) {
					Intent intent = new Intent(getActivity(), FinestraListinoBaseActivity.class);

					intent.putExtra(Cantieri.ID_CANTIERE, idCantiere);
					intent.putExtra(Preventivi.ID_PREVENTIVO, idPreventivo);

					intent.putExtra("DESC_PREVENTIVO", getArguments().getString("DESC_PREVENTIVO"));

					startActivity(intent);
				}

				if (which == 1) {
					DbInterno db = new DbInterno(getEConTabActivity());
					Preventivi tabPrev = new Preventivi();

					ContentValues valCant = tabPrev.getCantierePreventivo(db, idPreventivo);

					db.close();
					PopupAggiungiPlaccaPreventivo popPlacca = new PopupAggiungiPlaccaPreventivo(getEConTabActivity(), valCant, idPreventivo,
							PreventivoDettaglioFragment.this);
					popPlacca.apriPopup();
				}

				if (which == 2) {
					View campi = View.inflate(getEConTabActivity(), R.layout.dialog_sel_manodopera_layout, null);
					final EConTabSpinner spinnerManodopera = (EConTabSpinner) campi.findViewById(R.id.econtabSpinner_manodopera);
					spinnerManodopera.setTabella(new Manodopera());
					DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							if (which == DialogInterface.BUTTON_POSITIVE) {
								String idManodopera = spinnerManodopera.getValue();
								if (!idManodopera.trim().equals("")) {
									DbInterno db = new DbInterno(getEConTabActivity());
									ContentValues whereManodopera = new ContentValues();
									whereManodopera.put(Manodopera.ID_MANODOPERA, idManodopera);
									ContentValues valMano = db.getRecord(new Manodopera(), whereManodopera);

									PreventiviDettaglio pd = new PreventiviDettaglio();
									pd.aggiornaRigaMaterialePreventivo(db, idPreventivo, valMano, 1, PreventiviDettaglio.MANOPERA, false);
									db.close();
									ricerca();
								}
							}
						}
					};

					Utility.mostraDialogPersonalizzato(getString(R.string.seleziona_manodopera), getEConTabActivity(), campi,
							getString(R.string.conferma), getString(R.string.annulla), listener);

				}

				if (which == 3) {
					View campi = View.inflate(getEConTabActivity(), R.layout.dialog_nota_preventivo_layout, null);
					final EditText campoNota = (EditText) campi.findViewById(R.id.editText_nota);
					final EditText campoImporto = (EditText) campi.findViewById(R.id.editText_importo);
					DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							if (which == DialogInterface.BUTTON_POSITIVE) {
								String nota = campoNota.getText().toString();
								String importo = campoImporto.getText().toString();
								DbInterno db = new DbInterno(getEConTabActivity());

								ContentValues valNota = new ContentValues();
								valNota.put(PreventiviDettaglio.NOTE, nota);
								valNota.put(PreventiviDettaglio.PREZZO, Utility.formatNumeroDB(importo));

								PreventiviDettaglio pd = new PreventiviDettaglio();
								pd.aggiornaRigaMaterialePreventivo(db, idPreventivo, valNota, 1, PreventiviDettaglio.ALTRO, false);
								db.close();
								ricerca();
							}
						}

					};
					Utility.mostraDialogPersonalizzato(getString(R.string.nuova_riga_prev), getEConTabActivity(), campi,
							getString(R.string.conferma), getString(R.string.annulla), listener);
				}
			}
		});
	}

	private void salvaModifiche() {
		String errore = "";
		// TODO Auto-generated method stub
		ArrayList<ContentValues> daSalvare = new ArrayList<ContentValues>();
		daSalvare.addAll(adapter.getRigheInModifica().values());

		DbInterno db = new DbInterno(getEConTabActivity());
		db.getReadableDatabase().beginTransaction();
		try {
			PreventiviDettaglio tabPrev = new PreventiviDettaglio();
			for (int i = 0; i < daSalvare.size(); i++) {

				ContentValues valUpd = tabPrev.getValoriLogModifica(db);
				ContentValues valori = daSalvare.get(i);
				for (int c = 0; c < tabPrev.getNomiCampi().size(); c++) {
					String nomeCampo = tabPrev.getNomiCampi().get(c);
					if (valori.containsKey(nomeCampo) && !tabPrev.isChiave(nomeCampo)) {
						if (tabPrev.getTipoCampo(nomeCampo).equals(tabPrev.INTEGER)) {
							valUpd.put(nomeCampo, valori.getAsInteger(nomeCampo));
						}
						if (tabPrev.getTipoCampo(nomeCampo).equals(tabPrev.TEXT)) {
							valUpd.put(nomeCampo, valori.getAsString(nomeCampo));
						}
						if (tabPrev.getTipoCampo(nomeCampo).equals(tabPrev.NUMERIC)) {
							valUpd.put(nomeCampo, valori.getAsFloat(nomeCampo));
						}
					}
				}
				ContentValues whereUpd = new ContentValues();
				whereUpd.put(tabPrev.ID_PREVENTIVO_DETTAGLIO, valori.getAsInteger(tabPrev.ID_PREVENTIVO_DETTAGLIO));
				tabPrev.aggiornaRecord(db, valUpd, whereUpd);
			}

			db.getReadableDatabase().setTransactionSuccessful();
		} catch (Exception e) {
			errore = Log.getStackTraceString(e);
		} finally {
			db.getReadableDatabase().endTransaction();
			db.close();
		}

		if (errore.equals("")) {
			// faccio il clear prima della ricerca per pulire i flag delle righe in modifca
			dati.clear();
			if (righeInModifica.getAnimation() != null) {
				righeInModifica.getAnimation().cancel();
			}

			righeInModifica.setVisibility(View.GONE);
			buttonSalva.setBackgroundResource(R.drawable.bg_bottone_rotondo);
			ricerca();
		} else {
			Toast.makeText(getEConTabActivity(), errore, Toast.LENGTH_LONG).show();
		}

	}

	public void setInModifica() {
		// TODO Auto-generated method stub

		ricalcolaTotali();
		righeInModifica.setVisibility(View.VISIBLE);
		AlphaAnimation animation = new AlphaAnimation(1, 0);
		animation.setDuration(200);
		animation.setStartOffset(300);
		animation.setFillAfter(true);

		animation.setRepeatCount(Animation.INFINITE);
		animation.setRepeatMode(Animation.REVERSE);

		righeInModifica.startAnimation(animation);
		buttonSalva.setBackgroundResource(R.drawable.bg_bottone_rotondo_rosso);

	}

	public boolean isRigaLocale(int position) {
		ContentValues val = (ContentValues) dati.get(position);
		if (val.containsKey(RIGA_LOCALE)) {
			return true;
		}
		return false;
	}

	public void ricalcolaTotali() {
		// TODO Auto-generated method stub
		if (dati != null) {
			totaleImponibile = 0.0d;
			totaleIva = 0.0d;
			Preventivi tabPrev = new Preventivi();
			HashMap<String, Double> mappaTotali = tabPrev.calcolaTotaliPreventivo(dati, mappa_iva, getEConTabActivity());

			totaleImponibile = mappaTotali.get("IMPONIBILE");
			totaleIva = mappaTotali.get("IVA");
			totaleImporto = mappaTotali.get("IMPORTO");

			textViewTotaleImponibile.setText(getString(R.string.tot_imponibile) + " " + Utility.formatPrezzo(totaleImponibile));
			textViewTotaleIVA.setText(getString(R.string.tot_iva) + " " + Utility.formatPrezzo(totaleIva));
			textViewTotaleImporto.setText(getString(R.string.tot_importo) + " " + Utility.formatPrezzo(totaleImporto));

		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemcount) {
		// TODO Auto-generated method stub

		// TODO Auto-generated method stub
		if (firstVisibleItem > 0) {
			linearTitolo.setVisibility(View.VISIBLE);

			scrollHandler.removeCallbacks(scrollRunnable);
			scrollHandler.postDelayed(scrollRunnable, 3000);

		} else {
			linearTitolo.setVisibility(View.GONE);
		}

		if (dati != null && dati.size() > 0) {
			ContentValues primoVisibile = (ContentValues) dati.get(firstVisibleItem);

			if (primoVisibile != null) {
				if (primoVisibile.getAsString(PreventiviDettaglio.TIPO).equals(PreventiviDettaglio.MATERIALE)) {
					descrizioneLocale.setText(primoVisibile.getAsString("nomeArea") + " - " + primoVisibile.getAsString(Locali.NOME));
					return;
				}
				if (primoVisibile.getAsString(PreventiviDettaglio.TIPO).equals(PreventiviDettaglio.PLACCHE)) {
					descrizioneLocale.setText(primoVisibile.getAsString("nomeArea") + " - " + primoVisibile.getAsString(Locali.NOME));
					return;
				}
				if (primoVisibile.getAsString(PreventiviDettaglio.TIPO).equals(PreventiviDettaglio.MATERIALE_PREVENTIVO)) {
					descrizioneLocale.setText(getString(R.string.altro_materiale));
					return;
				}
				if (primoVisibile.getAsString(PreventiviDettaglio.TIPO).equals(PreventiviDettaglio.PLACCHE_PREVENTIVO)) {
					descrizioneLocale.setText(getString(R.string.altro_materiale));
					return;
				}
				if (primoVisibile.getAsString(PreventiviDettaglio.TIPO).equals(PreventiviDettaglio.MANOPERA)) {
					descrizioneLocale.setText(getString(R.string.manodopera));
					return;
				}
				if (primoVisibile.getAsString(PreventiviDettaglio.TIPO).equals(PreventiviDettaglio.ALTRO)) {
					descrizioneLocale.setText(getString(R.string.note));
					return;
				}
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	public boolean isModificheBloccate() {
		return modificheBloccate;
	}

	public void setModificheBloccate(boolean modificheBloccate) {
		this.modificheBloccate = modificheBloccate;
	}

	/**
	 * Esegue l'importazione dei dati contenuti nell'xls passato I dati importati sono codice, prezzo acquisto Il
	 * confronto avviene attraverso il nome dell'elemento scorrendo tutte le righe del preventivo
	 * 
	 * @param path
     */
	private void _eseguiImportazioneCodiciPrezzi(String path, String ricarico) {
		// TODO Auto-generated method stub

		File xls = new File(path);
		int ric = 0;
		if (!ricarico.equals("")) {
			ric = Integer.parseInt(ricarico);
		}
		if (xls.exists()) {
			FileInputStream is = null;
			DbInterno db = null;
			try {
				is = new FileInputStream(xls);
				db = new DbInterno(getActivity());
				db.getReadableDatabase().beginTransaction();
				HSSFWorkbook wb = new HSSFWorkbook(is);
				HSSFSheet foglio = wb.getSheetAt(0);
				Iterator<Row> rowiterator = foglio.rowIterator();
				while (rowiterator.hasNext()) {
					Row riga = rowiterator.next();

					// la prima riga sono le intestazioni
					if (riga.getRowNum() > 0) {
						_importaRigaArticolo(db, riga, ric);

					}

				}
				is.close();
				db.getReadableDatabase().setTransactionSuccessful();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				Toast.makeText(getActivity(), Log.getStackTraceString(e), Toast.LENGTH_LONG).show();
			} finally {

				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (db != null) {
					db.getReadableDatabase().endTransaction();
					db.close();
				}
			}
			ricerca();

		}
	}

	private void _importaRigaArticolo(DbInterno db, Row riga, int ricarico) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("EConTab: PreventivoDettaglioFragment _importaRigaArticolo ENTER");
		Cell cellaCodice = riga.getCell(0);
		Cell cellaDescrizione = riga.getCell(1);
		Cell cellaMetel = riga.getCell(2);

		Cell cellaPrezzo = riga.getCell(6);
		Cell cellaDescrizioneEConTab = riga.getCell(10);
		Cell cellaLineaEConTab = riga.getCell(11);
		if (cellaDescrizioneEConTab != null && cellaCodice != null && cellaDescrizione != null && cellaPrezzo != null
				&& cellaLineaEConTab != null && cellaMetel != null) {
			// se passo i controlli significa che l'xls � formattato bene per l'importazione

			PreventiviDettaglio tabPrev = new PreventiviDettaglio();
			Cantieri tabCant = new Cantieri();
			Locali tabLoc = new Locali();
			Componenti tabComponenti = new Componenti();
			Listini tabListini = new Listini();
			ElementiCodici tabAss = new ElementiCodici();
			Costruttori tabCostruttori = new Costruttori();

			String codice = cellaCodice.getStringCellValue();
			System.out.println("EConTab: PreventivoDettaglioFragment _importaRigaArticolo codice " + codice);
			double prezzoAcq = cellaPrezzo.getNumericCellValue();
			double prezzo = prezzoAcq + (prezzoAcq * ricarico) / 100;
			ContentValues whereCod = new ContentValues();

			whereCod.put(Listini.CODICE_ARTICOLO, codice.trim());

			ContentValues recordCheck = db.getRecord(new Listini(), whereCod);

			// aggiorno la descrizione e i prezzi se cambiata
			if (recordCheck != null && codice.trim().length() > 0) {

				ContentValues valUpdate = tabListini.getValoriLogModifica(db);
				valUpdate.put(Listini.DESCRIZIONE, cellaDescrizione.getStringCellValue().trim());
				valUpdate.put(Listini.PRZ_ULTIMO_ACQUISTO, prezzoAcq);
				valUpdate.put(Listini.PRZ_LISTINO, prezzo);

				ContentValues whereUpd = new ContentValues();

				whereUpd.put(Listini.CODICE_ARTICOLO, codice.trim());

				tabListini.aggiornaRecord(db, valUpdate, whereUpd);

			}

			ContentValues whereRiga = new ContentValues();
			// 1) Aggiorno i prezzi delle righe con stesso codice
			if (codice.trim().length() > 0) {

				whereRiga.clear();
				whereRiga.put(PreventiviDettaglio.ID_PREVENTIVO, idPreventivo);
				whereRiga.put(PreventiviDettaglio.CODICE_ARTICOLO, codice);
				ArrayList<Object> righePreventivoCodice = db.eseguiSelect(new PreventiviDettaglio(), whereRiga, null);
				for (int i = 0; i < righePreventivoCodice.size(); i++) {
					ContentValues rigaPrev = (ContentValues) righePreventivoCodice.get(i);
					ContentValues valUPD = tabPrev.getValoriLogModifica(db);
					valUPD.put(PreventiviDettaglio.PREZZO_ACQ, prezzoAcq);
					valUPD.put(PreventiviDettaglio.RICARICO, ricarico);
					valUPD.put(PreventiviDettaglio.PREZZO_VEN, prezzo);
					valUPD.put(PreventiviDettaglio.SCONTO, 0);
					valUPD.put(PreventiviDettaglio.PREZZO, prezzo);
					ContentValues whereUPD = new ContentValues();
					whereUPD.put(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO,
							rigaPrev.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO));
					tabPrev.aggiornaRecord(db, valUPD, whereUPD);

					// inserisco l'associazione nella tabella Elementi_codici se non esiste

					ContentValues whereAss = new ContentValues();
					whereAss.put(ElementiCodici.ID_ELEMENTO, rigaPrev.getAsInteger(PreventiviDettaglio.ID_ELEMENTO));
					whereAss.put(ElementiCodici.ID_COMPONENTE, rigaPrev.getAsInteger(PreventiviDettaglio.ID_COMPONENTE));
					whereAss.put(ElementiCodici.ID_PLACCA_MODULI, rigaPrev.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI));
					whereAss.put(ElementiCodici.CODICE_ARTICOLO, codice.trim());

					ContentValues recCheckAss = db.getRecord(tabAss, whereAss);
					if (recCheckAss == null) {
						ContentValues recValAss = tabAss.getValoriLogInserimento(db);
						recValAss.putAll(whereAss);
						tabAss.inserisciRecord(db, recValAss);
					}

				}

				// 2) Inserisco i codici nelle righe senza codice
				String descrizioneEConTab = cellaDescrizioneEConTab.getStringCellValue();
				whereRiga.clear();
				// whereRiga.put(PreventiviDettaglio.DESCRIZIONE, descrizioneEConTab.trim());
				whereRiga.put(PreventiviDettaglio.ID_PREVENTIVO, idPreventivo);
				whereRiga.put(PreventiviDettaglio.CODICE_ARTICOLO, "");

				ArrayList<Object> righePreventivo = db.eseguiSelect(new PreventiviDettaglio(), whereRiga, null);

				boolean gestitoALinea = false;
				for (int i = 0; i < righePreventivo.size(); i++) {
					gestitoALinea = false;
					ContentValues rigaPrev = (ContentValues) righePreventivo.get(i);
					String tipo = rigaPrev.getAsString(PreventiviDettaglio.TIPO);
					int idComponente = rigaPrev.getAsInteger(PreventiviDettaglio.ID_COMPONENTE);
					int idElemento = rigaPrev.getAsInteger(PreventiviDettaglio.ID_ELEMENTO);
					int idPlaccaModuli = rigaPrev.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI);

					String descrizionePrev = rigaPrev.getAsString(PreventiviDettaglio.DESCRIZIONE).trim();
					// per le placche la descrizione � data dal nome della placca pi� il numero di moduli
					if (idPlaccaModuli != 0) {
						gestitoALinea = true;
						ContentValues wherePlacca = new ContentValues();
						wherePlacca.put(PlaccheModuli.ID_PLACCA_MODULI, idPlaccaModuli);
						ContentValues valPlacca = db.getRecord(new PlaccheModuli(), wherePlacca);

						if (valPlacca != null) {
							descrizionePrev = getString(R.string.placca) + " " + descrizionePrev + " "
									+ valPlacca.getAsInteger(PlaccheModuli.NUMERO_MODULI) + " " + getString(R.string.moduli);
						}

					}

					// se la descrizione non corrisponde continuo con la prossima riga
					if (!descrizioneEConTab.trim().equalsIgnoreCase(descrizionePrev)) {
						continue;
					}

					int idLocale = rigaPrev.getAsInteger(PreventiviDettaglio.ID_LOCALE);
					ContentValues valLinea = null;
					if (idLocale == 0) {
						valLinea = tabCant.getLineaCantiere(db, idCantiere);
					} else {
						valLinea = tabLoc.getLineaLocale(db, idLocale);
					}

					if (idComponente != 0) {
						ContentValues whereComp = new ContentValues();
						whereComp.put(Componenti.ID_COMPONENTE, idComponente);
						ContentValues valComp = db.getRecord(tabComponenti, whereComp);
						if (valComp != null && valComp.getAsInteger(Componenti.LINEA_SN) == 1) {
							gestitoALinea = true;
						}
					}

					// se gestito a linea allora verifico che la linea del preventivo sia la stessa dell'XLS

					if (gestitoALinea == true) {
						String nomeLineaPrev = valLinea.getAsString(Linee.NOME_LINEA);
						String siglaMetelPrev = valLinea.getAsString(Costruttori.SIGLA_METEL);
						int idLineaPrev = valLinea.getAsInteger(Linee.ID_LINEA);

						// se la linea dell'XLS � uguale a quella del prevenetivo allora aggiorno inserisco il codice
						// nel preventivo
						String nomelineaXLS = cellaLineaEConTab.getStringCellValue();
						String siglaMetelXLS = cellaMetel.getStringCellValue();

						if (nomeLineaPrev.trim().equalsIgnoreCase(nomelineaXLS.trim())
								&& siglaMetelPrev.trim().equals(siglaMetelXLS.trim())) {

							if (recordCheck == null) {

								ContentValues valInsert = tabListini.getValoriLogInserimento(db);

								valInsert.put(Listini.CODICE_ARTICOLO, codice.toString().trim());
								valInsert.put(Listini.DESCRIZIONE, cellaDescrizione.getStringCellValue().trim());
								valInsert.put(Listini.PRZ_ULTIMO_ACQUISTO, prezzoAcq);
								valInsert.put(Listini.PRZ_LISTINO, prezzo);
								valInsert.put(Listini.SCONTO, 0);

								valInsert.put(Listini.ID_LINEA, idLineaPrev);
								valInsert.put(Listini.ID_COSTRUTTORE, valLinea.getAsInteger(Costruttori.ID_COSTRUTTORE));

								tabListini.inserisciRecord(db, valInsert);
							}

							ContentValues whereAss = new ContentValues();
							whereAss.put(ElementiCodici.ID_ELEMENTO, idElemento);
							whereAss.put(ElementiCodici.ID_COMPONENTE, idComponente);
							whereAss.put(ElementiCodici.ID_PLACCA_MODULI, idPlaccaModuli);
							whereAss.put(ElementiCodici.CODICE_ARTICOLO, codice.trim());

							ContentValues recCheckAss = db.getRecord(tabAss, whereAss);
							if (recCheckAss == null) {
								ContentValues recValAss = tabAss.getValoriLogInserimento(db);
								recValAss.putAll(whereAss);
								tabAss.inserisciRecord(db, recValAss);
							}

							// aggiorno la riga di preventivo impostando prezzi e codici

							ContentValues valUPD = tabPrev.getValoriLogModifica(db);
							valUPD.put(PreventiviDettaglio.CODICE_ARTICOLO, codice);
							valUPD.put(PreventiviDettaglio.PREZZO_ACQ, prezzoAcq);
							valUPD.put(PreventiviDettaglio.RICARICO, ricarico);
							valUPD.put(PreventiviDettaglio.PREZZO_VEN, prezzo);
							valUPD.put(PreventiviDettaglio.SCONTO, 0);
							valUPD.put(PreventiviDettaglio.PREZZO, prezzo);
							ContentValues whereUPD = new ContentValues();
							whereUPD.put(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO,
									rigaPrev.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO));
							tabPrev.aggiornaRecord(db, valUPD, whereUPD);
						}

					}
					// se non gestito a linea allora inserisco solo il codice prendendo l'id fornitore dalla sigla
					// metel
					else {
						if (recordCheck == null) {

							String siglaMetel = cellaMetel.getStringCellValue();
							ContentValues recCostruttore = tabCostruttori.getCostruttoreDaSiglaMetel(db, siglaMetel.trim());
							if (recCostruttore == null) {
								// inserisco il costruttore on ragione sociale = sigla metel
								recCostruttore = tabCostruttori.getValoriLogInserimento(db);
								recCostruttore.put(Costruttori.RAGIONE_SOCIALE, siglaMetel.trim());
								recCostruttore.put(Costruttori.SIGLA_METEL, siglaMetel.trim());
								tabCostruttori.inserisciRecord(db, recCostruttore);
							}

							ContentValues valInsert = tabListini.getValoriLogInserimento(db);

							valInsert.put(Listini.CODICE_ARTICOLO, codice.toString().trim());
							valInsert.put(Listini.DESCRIZIONE, cellaDescrizione.getStringCellValue().trim());
							valInsert.put(Listini.PRZ_ULTIMO_ACQUISTO, prezzoAcq);
							valInsert.put(Listini.PRZ_LISTINO, prezzo);
							valInsert.put(Listini.SCONTO, 0);

							valInsert.put(Listini.ID_COSTRUTTORE, recCostruttore.getAsInteger(Costruttori.ID_COSTRUTTORE));

							tabListini.inserisciRecord(db, valInsert);
						}

						ContentValues whereAss = new ContentValues();
						whereAss.put(ElementiCodici.ID_ELEMENTO, idElemento);
						whereAss.put(ElementiCodici.ID_COMPONENTE, idComponente);
						whereAss.put(ElementiCodici.ID_PLACCA_MODULI, idPlaccaModuli);
						whereAss.put(ElementiCodici.CODICE_ARTICOLO, codice.trim());

						ContentValues recCheckAss = db.getRecord(tabAss, whereAss);
						if (recCheckAss == null) {
							ContentValues recValAss = tabAss.getValoriLogInserimento(db);
							recValAss.putAll(whereAss);
							tabAss.inserisciRecord(db, recValAss);
						}

						// aggiorno la riga di preventivo impostando prezzi e codici

						ContentValues valUPD = tabPrev.getValoriLogModifica(db);
						valUPD.put(PreventiviDettaglio.CODICE_ARTICOLO, codice);
						valUPD.put(PreventiviDettaglio.PREZZO_ACQ, prezzoAcq);
						valUPD.put(PreventiviDettaglio.RICARICO, ricarico);
						valUPD.put(PreventiviDettaglio.PREZZO_VEN, prezzo);
						valUPD.put(PreventiviDettaglio.SCONTO, 0);
						valUPD.put(PreventiviDettaglio.PREZZO, prezzo);
						ContentValues whereUPD = new ContentValues();
						whereUPD.put(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO,
								rigaPrev.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO));
						tabPrev.aggiornaRecord(db, valUPD, whereUPD);
					}
				}
			}
		}
	}

    public String getTipoPreventivoOrdine() {
        return tipoPreventivoOrdine;
    }
}
