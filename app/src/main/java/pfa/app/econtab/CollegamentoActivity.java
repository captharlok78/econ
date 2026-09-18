package pfa.app.econtab;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import pfa.app.econtab.adapters.TubiPassantiAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Aree;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.CategorieComponenti;
import pfa.app.econtab.db.table.CategorieGenerali;
import pfa.app.econtab.db.table.Collegamenti;
import pfa.app.econtab.db.table.ComponentiCantiere;
import pfa.app.econtab.db.table.ComposizioniCantiere;
import pfa.app.econtab.db.table.Ditte;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabSpinner;

public class CollegamentoActivity extends EConTabActivity implements OnItemClickListener {

	private class MyTextWatcher implements TextWatcher {

		View view = null;

		public MyTextWatcher(View view) {
			this.view = view;
		}

		@Override
		public void afterTextChanged(Editable s) {
			System.out.println("EConTab: CollegamentoActivity afterTextChanged");
			// TODO Auto-generated method stub
			if (view.getId() == spinnerLocaleA.getId()) {

				elementiLocaleA.clear();
				if (!spinnerLocaleA.getValue().equals("")) {
					elementiLocaleA = _inizializzaELementiLocale(Integer.parseInt(spinnerLocaleA.getValue()));

				}

				spinnerElementoA.setValue("");
				spinnerElementoA.setValoriSpinnerLibero(elementiLocaleA);
			}

			if (view.getId() == spinnerLocaleB.getId()) {
				elementiLocaleB.clear();
				if (!spinnerLocaleB.getValue().equals("")) {
					elementiLocaleB = _inizializzaELementiLocale(Integer.parseInt(spinnerLocaleB.getValue()));
				}
				spinnerElementoB.setValue("");
				spinnerElementoB.setValoriSpinnerLibero(elementiLocaleB);
			}

			if (view.getId() == spinnerElementoA.getId()) {
				componentiElementoA.clear();
				if (!spinnerElementoA.getValue().equals("")) {
					componentiElementoA = _inizializzaComponentiElemento(Integer.parseInt(spinnerElementoA.getValue()));
				}
				spinnerComponenteA.setValue("");
				spinnerComponenteA.setValoriSpinnerLibero(componentiElementoA);

			}

			if (view.getId() == spinnerElementoB.getId()) {
				componentiElementoB.clear();
				if (!spinnerElementoB.getValue().equals("")) {
					componentiElementoB = _inizializzaComponentiElemento(Integer.parseInt(spinnerElementoB.getValue()));

				}
				spinnerComponenteB.setValue("0");
				spinnerComponenteB.setValoriSpinnerLibero(componentiElementoB);

				ricercaTubi();
			}

			_impostaVisibilitaComponenti();
			_impostaSelezioneTuboECavo();
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			// TODO Auto-generated method stub

		}

	}

	public static final int TUBO = 0;
	public static final int CAVO = 1;
	public static final int TUBO_CAVO = 2;

	public static String ID_CAVO_GENERICO = "9999999";
	public static String ID_TUBO_GENERICO = "9999999";

	private int tipoCollegamento = 0;
	private int idElementoPartenza = 0;
	private int idCollegamento = 0;
	private int idCantiere = 0;
	private int idLocalePartenza = 0;
	private int idLocaleDestinazione = 0;
	private ArrayList<Object> localiPerSpinner = new ArrayList<Object>();
	private ArrayList<Object> elementiLocaleA = new ArrayList<Object>();
	private ArrayList<Object> elementiLocaleB = new ArrayList<Object>();

	private ArrayList<Object> componentiElementoA = new ArrayList<Object>();
	private ArrayList<Object> componentiElementoB = new ArrayList<Object>();

	private EConTabSpinner spinnerLocaleA = null;
	private EConTabSpinner spinnerLocaleB = null;
	private EConTabSpinner spinnerElementoA = null;
	private EConTabSpinner spinnerElementoB = null;
	private EConTabSpinner spinnerComponenteA = null;
	private EConTabSpinner spinnerComponenteB = null;
	private EConTabSpinner spinnerTubo = null;
	private EConTabSpinner spinnerCavo = null;

	private EditText cmCoda = null;
	private EditText metriTubo = null;
	private TextView metriCavo = null;
	private EditText metriCavoUni = null;
	private EditText qtaCavo = null;

	private int cmCodaScatoleDitta = 0;
	private int cmCodaUtilizzatoriDitta = 0;

	private int idOrdine = 0;

	private Button buttonTubo = null;
	private Button buttonCavo = null;
	private Button buttonTuboCavo = null;

	private ListView listaTubiPassanti = null;
	private TubiPassantiAdapter adapter = null;
	private ArrayList<Object> datiTubiPassanti = null;

	private int idCollegamentoTuboSelezionato = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: CollegamentoActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
        setVisualizzazionePopup();
		setContentView(R.layout.activity_collegamento);


		// prendo i cm di coda dall'azienda
		Ditte tabDitte = new Ditte();
		ContentValues whereDitta = new ContentValues();
		whereDitta.put(Ditte.ID_DITTA, Sessione.getDittaSelezionata());
		DbInterno db = new DbInterno(this);
		ContentValues valDitta = db.getRecord(tabDitte, whereDitta);
		if (valDitta != null) {
			cmCodaScatoleDitta = (int) (valDitta.getAsDouble(Ditte.METRI_CODA_SCATOLE) * 100);
			cmCodaUtilizzatoriDitta = (int) (valDitta.getAsDouble(Ditte.METRI_CODA_UTILIZZATORI) * 100);
		}
		db.close();

		tipoCollegamento = getIntent().getIntExtra("TIPO", TUBO_CAVO);

		if (getIntent().getExtras().containsKey("TIPO_FISSO")) {
			tipoCollegamento = getIntent().getIntExtra("TIPO_FISSO", TUBO);
			findViewById(R.id.linear_tipi_collegamento).setVisibility(View.GONE);
		}

		listaTubiPassanti = (ListView) findViewById(R.id.listViewTubiPassanti);
		listaTubiPassanti.setOnItemClickListener(this);

		idElementoPartenza = getIntent().getIntExtra("ID_ELEMENTO", 0);
		idCollegamento = getIntent().getIntExtra(Collegamenti.ID_COLLEGAMENTO, 0);
		idOrdine = getIntent().getIntExtra(Collegamenti.ID_ORDINE, 0);

		buttonCavo = (Button) findViewById(R.id.buttonCavo);
		buttonTubo = (Button) findViewById(R.id.buttonTubo);
		buttonTuboCavo = (Button) findViewById(R.id.buttonTuboCavo);

		spinnerLocaleA = (EConTabSpinner) findViewById(R.id.spinnerLocaleA);
		spinnerLocaleB = (EConTabSpinner) findViewById(R.id.spinnerLocaleB);
		spinnerElementoA = (EConTabSpinner) findViewById(R.id.spinnerElementoA);
		spinnerElementoB = (EConTabSpinner) findViewById(R.id.spinnerElementoB);
		spinnerComponenteA = (EConTabSpinner) findViewById(R.id.spinnerComponenteA);
		spinnerComponenteB = (EConTabSpinner) findViewById(R.id.spinnerComponenteB);

		spinnerLocaleA.addTextChangeListener(new MyTextWatcher(spinnerLocaleA));
		spinnerLocaleB.addTextChangeListener(new MyTextWatcher(spinnerLocaleB));
		spinnerElementoA.addTextChangeListener(new MyTextWatcher(spinnerElementoA));
		spinnerElementoB.addTextChangeListener(new MyTextWatcher(spinnerElementoB));
		spinnerComponenteA.addTextChangeListener(new MyTextWatcher(spinnerComponenteA));
		spinnerComponenteB.addTextChangeListener(new MyTextWatcher(spinnerComponenteB));

		spinnerTubo = (EConTabSpinner) findViewById(R.id.spinnerTubo);
		spinnerCavo = (EConTabSpinner) findViewById(R.id.spinnerCavo);

		cmCoda = (EditText) findViewById(R.id.editTextCmCodaCavo);
		metriTubo = (EditText) findViewById(R.id.editTextMetriTubo);
		if (tipoCollegamento == TUBO_CAVO) {
			metriTubo.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					// TODO Auto-generated method stub
					metriCavoUni.setText(metriTubo.getText().toString());
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					// TODO Auto-generated method stub

				}

				@Override
				public void afterTextChanged(Editable s) {
					// TODO Auto-generated method stub

				}
			});
		}
		metriCavo = (TextView) findViewById(R.id.editTextMetriCavo);
		metriCavoUni = (EditText) findViewById(R.id.editTextMetriCavoUni);
		metriCavoUni.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				ricalcolaMetriCavoTotale();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub

			}
		});
		qtaCavo = (EditText) findViewById(R.id.text_qta);
		qtaCavo.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				ricalcolaMetriCavoTotale();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub

			}
		});

		ContentValues filtroTubi = new ContentValues();
		filtroTubi.put(Elementi.ID_CATEGORIA_GENERALE, CategorieGenerali.TUBI);
		spinnerTubo.setFiltro(filtroTubi);
		spinnerTubo.setTabella(new Elementi());

		ContentValues filtroCavo = new ContentValues();
		filtroCavo.put(Elementi.ID_CATEGORIA_GENERALE, CategorieGenerali.CAVI);
		spinnerCavo.setFiltro(filtroCavo);
		spinnerCavo.setTabella(new Elementi());

		if (idCollegamento == 0) {
			findViewById(R.id.linearElementoADett).setVisibility(View.GONE);
			findViewById(R.id.textView_partenzaDesc).setVisibility(View.VISIBLE);
			_inizializzaInserimento();

			// ((Button) findViewById(R.id.button_annulla)).setText(getString(R.string.annulla));

		} else {
			findViewById(R.id.linear_tipi_collegamento).setVisibility(View.GONE);
			findViewById(R.id.linearElementoADett).setVisibility(View.VISIBLE);
			findViewById(R.id.textView_partenzaDesc).setVisibility(View.GONE);
			_inizializzaModifica();
		}
		System.out.println("EConTab: CollegamentoActivity onCreate EXIT");
	}

	private void ricalcolaMetriCavoTotale() {
		System.out.println("EConTab: CollegamentoActivity ricalcolaMetriCavoTotale ENTER");
		// TODO Auto-generated method stub
		int qta = 0;
		double metriUni = 0d;
		String qtaTxt = qtaCavo.getText().toString();
		if (!qtaTxt.equals("")) {
			qta = Integer.parseInt(qtaTxt);
		}
		String metriCavoUniTxt = metriCavoUni.getText().toString();
		if (!metriCavoUniTxt.equals("")) {
			metriUni = Utility.formatNumeroDB(metriCavoUniTxt);
		}

		metriCavo.setText(Utility.formatNumero(Utility.arrotonda((metriUni * qta), 2), 2));
		System.out.println("EConTab: CollegamentoActivity ricalcolaMetriCavoTotale EXIT");
	}

	private void _inizializzaInserimento() {
		System.out.println("EConTab: CollegamentoActivity _inizializzaInserimento ENTER");
		// TODO Auto-generated method stub
		if (tipoCollegamento == TUBO) {
			// setText(R.id.textView_titolo, getString(R.string.tubo));

			selezionaTipo(buttonTubo);
		}
		if (tipoCollegamento == CAVO) {
			// setText(R.id.textView_titolo, getString(R.string.cavo));

			selezionaTipo(buttonCavo);
		}
		if (tipoCollegamento == TUBO_CAVO) {
			// setText(R.id.textView_titolo, getString(R.string.tubo_cavo));
			selezionaTipo(buttonTuboCavo);
		}

		_inizializzaLocaliCantiere();
		spinnerLocaleA.setValue("" + idLocalePartenza);
		idLocaleDestinazione = idLocalePartenza;
		spinnerLocaleB.setValue("" + idLocaleDestinazione);
		spinnerLocaleA.setValoriSpinnerLibero(localiPerSpinner);
		spinnerLocaleB.setValoriSpinnerLibero(localiPerSpinner);

		elementiLocaleA = _inizializzaELementiLocale(idLocalePartenza);
		elementiLocaleB = new ArrayList<Object>(elementiLocaleA);

		spinnerElementoA.setValue("" + idElementoPartenza);
		spinnerElementoA.setValoriSpinnerLibero(elementiLocaleA);
		spinnerElementoB.setValoriSpinnerLibero(elementiLocaleB);

		componentiElementoA = _inizializzaComponentiElemento(idElementoPartenza);
		componentiElementoB = new ArrayList<Object>(componentiElementoA);

		if (getIntent().getExtras().containsKey("ID_COMPONENTE")) {
			spinnerComponenteA.setValue("" + getIntent().getIntExtra("ID_COMPONENTE", 0));
		}

		spinnerComponenteA.setValoriSpinnerLibero(componentiElementoA);
		spinnerComponenteB.setValoriSpinnerLibero(componentiElementoB);

		_impostaVisibilitaComponenti();
		_impostaSelezioneTuboECavo();

		try{
			DbInterno db = new DbInterno(this);
			ContentValues locale = db.getRecord("Select * from locali where id_locale="+idLocalePartenza);
			ContentValues elemento = db.getRecord("Select * from elementi_cantiere where id_elemento_cant="+idElementoPartenza);
			String componente = "";
			if (getIntent().getExtras().containsKey("ID_COMPONENTE")){
				ContentValues comp = db.getRecord("Select componenti_cantiere.*,composizioni_cantiere.posizione_iniziale from componenti_cantiere inner join composizioni_cantiere on componenti_cantiere.id_componente_cant=composizioni_cantiere.id_componente_cant where componenti_cantiere.id_componente_cant="+getIntent().getIntExtra("ID_COMPONENTE", 0));
				if (comp!=null){
					String pos = comp.getAsString(ComposizioniCantiere.POSIZIONE_INIZIALE);
					if (!pos.equals("0")){
						componente = " (" + pos +" - "+ comp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT)+")";
					}
					else {
						componente = " ("+comp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT)+")";
					}

				}

			}
			db.close();
			((TextView)findViewById(R.id.textView_partenzaDesc)).setText(elemento.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO)+" - " + elemento.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT)+componente);
		}
		catch (Exception ex){
			DbInterno db = new DbInterno(this);
			ContentValues locale = db.getRecord("Select * from locali where id_locale="+idLocalePartenza);
			db.close();
			((TextView)findViewById(R.id.textView_partenzaDesc)).setText("");
		}

		System.out.println("EConTab: CollegamentoActivity _inizializzaInserimento EXIT");
	}

	private void _impostaVisibilitaComponenti() {
		System.out.println("EConTab: CollegamentoActivity _impostaVisibilitaComponenti ENTER");
		// TODO Auto-generated method stub
		if (componentiElementoA.size() == 0 || tipoCollegamento == TUBO) {
			findViewById(R.id.textView_componenteA).setVisibility(View.GONE);
			spinnerComponenteA.setVisibility(View.GONE);
		} else {
			findViewById(R.id.textView_componenteA).setVisibility(View.VISIBLE);
			spinnerComponenteA.setVisibility(View.VISIBLE);
		}

		if (componentiElementoB.size() == 0 || tipoCollegamento == TUBO) {
			findViewById(R.id.textView_componenteB).setVisibility(View.GONE);
			spinnerComponenteB.setVisibility(View.GONE);
		} else {
			findViewById(R.id.textView_componenteB).setVisibility(View.VISIBLE);
			spinnerComponenteB.setVisibility(View.VISIBLE);
		}
		System.out.println("EConTab: CollegamentoActivity _impostaVisibilitaComponenti EXIT");
	}

	private ArrayList<Object> _inizializzaComponentiElemento(int idElemento) {

		ArrayList<Object> componenti = new ArrayList<Object>();
		DbInterno db = new DbInterno(this);
		Collegamenti tabCollegamenti = new Collegamenti();
		ArrayList<Integer> componentiCollegati = tabCollegamenti.getComponentiCollegatiElemento(db, idElemento);

		ComposizioniCantiere tabComposizioniCantiere = new ComposizioniCantiere();
		ArrayList<Object> recs = tabComposizioniCantiere.getComposizioneElemento(db, idElemento);
		for (int i = 0; i < recs.size(); i++) {
			ContentValues curr = (ContentValues) recs.get(i);

            String tipoCategoriaComponente = curr.getAsString(CategorieComponenti.TIPO);
            if (tipoCategoriaComponente.equals(CategorieComponenti.TIPO_COPRISCATOLA) || tipoCategoriaComponente.equals(CategorieComponenti.TIPO_PORTAFRUTTI)){
                continue;
            }
			ContentValues valSpinner = new ContentValues();
			int idComp = curr.getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT);
			valSpinner.put(EConTabSpinner.VALORE, idComp);
			String moduliOccupati = curr.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
			if (!moduliOccupati.trim().equals("")) {
				moduliOccupati = moduliOccupati + " - ";
			}
			String visto = "";
			if (componentiCollegati.contains(idComp)) {
				visto = " \u221a";
			}

			valSpinner.put(EConTabSpinner.DESCRIZIONE, moduliOccupati + curr.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT) + visto);
			componenti.add(valSpinner);
		}
		db.close();
		return componenti;
	}

	private ArrayList<Object> _inizializzaELementiLocale(int idLocale) {

		ArrayList<Object> elementi = new ArrayList<Object>();
		DbInterno db = new DbInterno(this);
		ElementiCantiere tabElementi = new ElementiCantiere();
		ArrayList<Object> recs = tabElementi.getElementiLocaleNoPreventivi(db, idLocale,false);
		for (int i = 0; i < recs.size(); i++) {
			ContentValues curr = (ContentValues) recs.get(i);
			ContentValues valSpinner = new ContentValues();
			valSpinner.put(EConTabSpinner.VALORE, curr.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT));
			valSpinner.put(
					EConTabSpinner.DESCRIZIONE,
					curr.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO) + " - "
							+ curr.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
			elementi.add(valSpinner);
		}
		db.close();
		return elementi;
	}

	private void _inizializzaLocaliCantiere() {
		System.out.println("EConTab: CollegamentoActivity _inizializzaLocaliCantiere ENTER");
		// TODO Auto-generated method stub
		DbInterno db = new DbInterno(this);
		ElementiCantiere tabElem = new ElementiCantiere();
		ContentValues where = new ContentValues();
		where.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoPartenza);
		ContentValues valElem = db.getRecord(tabElem, where);
		if (valElem != null) {
			idCantiere = valElem.getAsInteger(ElementiCantiere.ID_CANTIERE);
			idLocalePartenza = valElem.getAsInteger(ElementiCantiere.ID_LOCALE);
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
			if (idCantiere==0) {
				ContentValues cantloc = tabLocali.getCantiereLocale(db, idLocalePartenza);
				if (cantloc != null) {
					idCantiere = cantloc.getAsInteger(Cantieri.ID_CANTIERE);
				}
			}
			Aree tabAree = new Aree();
			ArrayList locali = tabLocali.getLocaliCantiereConArea(db, idCantiere);
			localiPerSpinner = new ArrayList<Object>();
			for (int i = 0; i < locali.size(); i++) {
				ContentValues curr = (ContentValues) locali.get(i);
				ContentValues loc = new ContentValues();
				loc.put(EConTabSpinner.VALORE, curr.getAsInteger(Locali.ID_LOCALE));
				loc.put(EConTabSpinner.DESCRIZIONE, curr.getAsString(tabAree.getAlias(Aree.NOME)) + " - " + curr.getAsString(Locali.NOME));
				localiPerSpinner.add(loc);
			}

		}
		System.out.println("EConTab: CollegamentoActivity _inizializzaLocaliCantiere EXIT");
		db.close();
	}

	private void _impostaSelezioneTuboECavo() {
		System.out.println("EConTab: CollegamentoActivity _impostaSelezioneTuboECavo ENTER");
		// SOLO IN INSERUIMENTO VERIFICO IMPOSTO IL TUBO E IL CAVO DI DEFAULT

		if (idCollegamento == 0) {

			DbInterno db = new DbInterno(this);
			if (tipoCollegamento == TUBO || tipoCollegamento == TUBO_CAVO) {
				if (spinnerTubo.getValue().equals("")) {
					// Se ho l'elemento A selezionato e no ha componenti allora guardo l'elemento altrimenti guardo sui
					// componenti
					if (!spinnerElementoA.getValue().equals("")) {
						if (componentiElementoA.size() == 0) {
							ContentValues whereElem = new ContentValues();
							whereElem.put(ElementiCantiere.ID_ELEMENTO_CANT, spinnerElementoA.getValue());
							ContentValues val = db.getRecord(new ElementiCantiere(), whereElem);
							if (val != null) {
								spinnerTubo.setValue("" + val.getAsInteger(ElementiCantiere.ID_ELEMENTO_TUBO));
								spinnerTubo.setTabella(new Elementi());
							}
						} else {
							if (!spinnerComponenteA.getValue().equals("")) {
								ContentValues whereElem = new ContentValues();
								whereElem.put(ComponentiCantiere.ID_COMPONENTE_CANT, spinnerComponenteA.getValue());
								ContentValues val = db.getRecord(new ComponentiCantiere(), whereElem);
								if (val != null) {
									spinnerTubo.setValue("" + val.getAsInteger(ComponentiCantiere.ID_ELEMENTO_TUBO));
									spinnerTubo.setTabella(new Elementi());
								}
							}
						}
					}
				}
			}

			if (tipoCollegamento == CAVO || tipoCollegamento == TUBO_CAVO) {
				if (spinnerCavo.getValue().equals("")) {
					// Se ho l'elemento A selezionato e no ha componenti allora guardo l'elemento altrimenti guardo sui
					// componenti
					if (!spinnerElementoA.getValue().equals("")) {
						if (componentiElementoA.size() == 0) {
							ContentValues whereElem = new ContentValues();
							whereElem.put(ElementiCantiere.ID_ELEMENTO_CANT, spinnerElementoA.getValue());
							ContentValues val = db.getRecord(new ElementiCantiere(), whereElem);
							if (val != null) {
								spinnerCavo.setValue("" + val.getAsInteger(ElementiCantiere.ID_ELEMENTO_CAVO));
								spinnerCavo.setTabella(new Elementi());
							}
						} else {
							if (!spinnerComponenteA.getValue().equals("")) {
								ContentValues whereElem = new ContentValues();
								whereElem.put(ComponentiCantiere.ID_COMPONENTE_CANT, spinnerComponenteA.getValue());
								ContentValues val = db.getRecord(new ComponentiCantiere(), whereElem);
								if (val != null) {
									spinnerCavo.setValue("" + val.getAsInteger(ComponentiCantiere.ID_ELEMENTO_CAVO));
									spinnerCavo.setTabella(new Elementi());
								}
							}
						}
					}
				}

				if (cmCoda.getText().toString().equals("")) {
					int cmTotali = 0;
					if (!spinnerElementoA.getValue().equals("") && !spinnerElementoB.getValue().equals("")) {

						int idElemA = Integer.parseInt(spinnerElementoA.getValue());
						ContentValues whereA = new ContentValues();
						whereA.put(ElementiCantiere.ID_ELEMENTO_CANT, idElemA);
						ContentValues valA = db.getRecord(new ElementiCantiere(), whereA);
						if (valA != null) {
							int idElemento = valA.getAsInteger(ElementiCantiere.ID_ELEMENTO);
							ContentValues whereElem = new ContentValues();
							whereElem.put(Elementi.ID_ELEMENTO, idElemento);
							ContentValues valElem = db.getRecord(new Elementi(), whereElem);
							if (valElem != null) {
								int idCategoria = valElem.getAsInteger(Elementi.ID_CATEGORIA_GENERALE);
								if (idCategoria == CategorieGenerali.UTILIZZATORI) {
									cmTotali = cmTotali + cmCodaUtilizzatoriDitta;
								} else {
									cmTotali = cmTotali + cmCodaScatoleDitta;
								}
							}
						}

						int idElemB = Integer.parseInt(spinnerElementoB.getValue());
						ContentValues whereB = new ContentValues();
						whereB.put(ElementiCantiere.ID_ELEMENTO_CANT, idElemB);
						ContentValues valB = db.getRecord(new ElementiCantiere(), whereB);
						if (valB != null) {
							int idElemento = valB.getAsInteger(ElementiCantiere.ID_ELEMENTO);
							ContentValues whereElem = new ContentValues();
							whereElem.put(Elementi.ID_ELEMENTO, idElemento);
							ContentValues valElem = db.getRecord(new Elementi(), whereElem);
							if (valElem != null) {
								int idCategoria = valElem.getAsInteger(Elementi.ID_CATEGORIA_GENERALE);
								if (idCategoria == CategorieGenerali.UTILIZZATORI) {
									cmTotali = cmTotali + cmCodaUtilizzatoriDitta;
								} else {
									cmTotali = cmTotali + cmCodaScatoleDitta;
								}
							}
						}
					}

					if (cmTotali != 0) {
						cmCoda.setText("" + cmTotali);
					}
				}

			}

			// in inserimento se non ho impostato i metri di cavo verifico che ci sia un tubo già passato tra i due
			// elementi
			// e imposto i metri dal tubo
			if (metriCavoUni.getText().toString().equals("") && tipoCollegamento == CAVO && idCollegamento == 0) {
				if (!spinnerElementoA.getValue().equals("") && !spinnerElementoB.getValue().equals("")) {
					int idElementoA = Integer.parseInt(spinnerElementoA.getValue());
					int idElementoB = Integer.parseInt(spinnerElementoB.getValue());
					String SQL_TUBO = "Select * from " + Collegamenti.NOME_TABELLA + " where ((" + Collegamenti.ID_ELEMENTO_CANT1 + "="
							+ idElementoA + " and " + Collegamenti.ID_ELEMENTO_CANT2 + "=" + idElementoB + ") or ("
							+ Collegamenti.ID_ELEMENTO_CANT1 + "=" + idElementoB + " and " + Collegamenti.ID_ELEMENTO_CANT2 + "="
							+ idElementoA + "))  and " + Collegamenti.ID_TUBO + "<>0";
					ArrayList<Object> tubi = db.eseguiSelect(SQL_TUBO, null);
					if (tubi.size() > 0) {
						ContentValues tubo = (ContentValues) tubi.get(0);
						metriCavoUni.setText("" + tubo.getAsDouble(Collegamenti.METRI));
						Toast.makeText(this, getString(R.string.messaggio_metri_cavo_da_tubo), Toast.LENGTH_LONG).show();
					}
				}
			}

			db.close();
		}
		System.out.println("EConTab: CollegamentoActivity _impostaSelezioneTuboECavo EXIT");
	}

	private void _inizializzaModifica() {
		System.out.println("EConTab: CollegamentoActivity _inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Collegamenti.ID_COLLEGAMENTO, idCollegamento);
		ContentValues rec = db.getRecord(new Collegamenti(), where);
		if (rec != null) {
			int idTubo = rec.getAsInteger(Collegamenti.ID_TUBO);
			int idCavo = rec.getAsInteger(Collegamenti.ID_CAVO);
			if (idTubo != 0) {
				tipoCollegamento = TUBO;
				// setText(R.id.textView_titolo, getString(R.string.tubo));
				findViewById(R.id.linearCavoMetri).setVisibility(View.GONE);
				findViewById(R.id.linear_codaqta).setVisibility(View.GONE);
				String metriTxt = "" + rec.getAsDouble(Collegamenti.METRI);
				if (metriTxt.contains(".0")) {
					metriTxt = metriTxt.replace(".0", "");
				}
				metriTubo.setText(metriTxt);
				spinnerTubo.setValue("" + idTubo);
				spinnerTubo.setTabella(new Elementi());
			}
			if (idCavo != 0) {
				tipoCollegamento = CAVO;
				// setText(R.id.textView_titolo, getString(R.string.cavo));
				findViewById(R.id.linearTuboMetri).setVisibility(View.GONE);
				String metriTxt = "" + rec.getAsDouble(Collegamenti.METRI);
				if (metriTxt.contains(".0")) {
					metriTxt = metriTxt.replace(".0", "");
				}
				metriCavo.setText(metriTxt);
				metriCavoUni.setText("" + rec.getAsDouble(Collegamenti.METRI_CAVO_UNI));
				qtaCavo.setText("" + rec.getAsInteger(Collegamenti.QTA_CAVO));
				spinnerCavo.setValue("" + idCavo);
				spinnerCavo.setTabella(new Elementi());
				idCollegamentoTuboSelezionato = rec.getAsInteger(Collegamenti.ID_COLLEGAMENTO_TUBO);
			}
			cmCoda.setText("" + (int) (rec.getAsDouble(Collegamenti.METRI_CODA) * 100));
			idElementoPartenza = rec.getAsInteger(Collegamenti.ID_ELEMENTO_CANT1);
			int idElementoDestinazione = rec.getAsInteger(Collegamenti.ID_ELEMENTO_CANT2);
			int idComponentePartenza = rec.getAsInteger(Collegamenti.ID_COMPONENTE_CANT1);
			int idComponenteDestinazione = rec.getAsInteger(Collegamenti.ID_COMPONENTE_CANT2);
			_inizializzaLocaliCantiere();

			spinnerLocaleA.setValue("" + idLocalePartenza);
			spinnerLocaleA.setValoriSpinnerLibero(localiPerSpinner);

			ContentValues whereElem2 = new ContentValues();
			whereElem2.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoDestinazione);
			ContentValues recElem2 = db.getRecord(new ElementiCantiere(), whereElem2);
			if (recElem2 != null) {
				idLocaleDestinazione = recElem2.getAsInteger(ElementiCantiere.ID_LOCALE);
			}
			spinnerLocaleB.setValue("" + idLocaleDestinazione);
			spinnerLocaleB.setValoriSpinnerLibero(localiPerSpinner);

			elementiLocaleA = _inizializzaELementiLocale(idLocalePartenza);
			elementiLocaleB = _inizializzaELementiLocale(idLocaleDestinazione);

			spinnerElementoA.setValue("" + idElementoPartenza);
			spinnerElementoA.setValoriSpinnerLibero(elementiLocaleA);
			spinnerElementoB.setValue("" + idElementoDestinazione);
			spinnerElementoB.setValoriSpinnerLibero(elementiLocaleB);

			componentiElementoA = _inizializzaComponentiElemento(idElementoPartenza);
			componentiElementoB = _inizializzaComponentiElemento(idElementoDestinazione);

			if (idComponentePartenza != 0) {
				spinnerComponenteA.setValue("" + idComponentePartenza);
			}
			spinnerComponenteA.setValoriSpinnerLibero(componentiElementoA);
			if (idComponenteDestinazione != 0) {
				spinnerComponenteB.setValue("" + idComponenteDestinazione);
			}
			spinnerComponenteB.setValoriSpinnerLibero(componentiElementoB);

			_impostaVisibilitaComponenti();
			_impostaSelezioneTuboECavo();
		}
		System.out.println("EConTab: CollegamentoActivity _inizializzaModifica EXIT");
		db.close();
	}

	public void salva(View v) {
		System.out.println("EConTab: CollegamentoActivity salva ENTER");
		if (spinnerElementoA.getValue().equals("")) {
			spinnerElementoA.setError(getString(R.string.errore_selezione_elemento_partenza));
			Toast.makeText(this, getString(R.string.errore_selezione_elemento_partenza), Toast.LENGTH_SHORT).show();
			return;
		}
		if (spinnerElementoB.getValue().equals("")) {
			spinnerElementoB.setError(getString(R.string.errore_selezione_elemento_destinazione));
			Toast.makeText(this, getString(R.string.errore_selezione_elemento_destinazione), Toast.LENGTH_SHORT).show();
			return;
		}

		if (tipoCollegamento != TUBO && componentiElementoA.size() > 0 && spinnerComponenteA.getValue().equals("")) {
			spinnerComponenteA.setError(getString(R.string.errore_selezione_componente_partenza));
			Toast.makeText(this, getString(R.string.errore_selezione_componente_partenza), Toast.LENGTH_SHORT).show();
			return;
		}

		if (tipoCollegamento != TUBO && componentiElementoB.size() > 0 && spinnerComponenteB.getValue().equals("")) {
			spinnerComponenteB.setError(getString(R.string.errore_selezione_componente_destinazione));
			Toast.makeText(this, getString(R.string.errore_selezione_componente_destinazione), Toast.LENGTH_SHORT).show();
			return;
		}

		if (tipoCollegamento == CAVO && idCollegamentoTuboSelezionato == 0) {
            //Utility.mostraDialog(getString(R.string.attenzione), getString(R.string.messaggio_selezione_tubo_per_cavo),this,"OK");
			Utility.mostraConfermaDialog(getString(R.string.attenzione), getString(R.string.messaggio_nessun_tubo_selezionato), this, "OK",
					getString(R.string.annulla), new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							if (which == DialogInterface.BUTTON_POSITIVE) {
								_eseguiSalvataggio();
							}
						}
					});
		} else {
			_eseguiSalvataggio();
		}
		System.out.println("EConTab: CollegamentoActivity salva EXIT");
	}

	private void _eseguiSalvataggio() {
		System.out.println("EConTab: CollegamentoActivity _eseguiSalvataggio ENTER");
		// TODO Auto-generated method stub
		// inserisco o modifico
		if (idCollegamento == 0) {
			DbInterno db = new DbInterno(this);
			Collegamenti tabCollegamenti = new Collegamenti();

			int idCollCavoInserito = 0;
			int idCollTuboInserito = 0;
			if (tipoCollegamento == CAVO || tipoCollegamento == TUBO_CAVO) {
				ContentValues valInsert = tabCollegamenti.getValoriLogInserimento(db);
				valInsert.put(Collegamenti.ID_ORDINE, idOrdine);
				valInsert.put(Collegamenti.ID_ELEMENTO_CANT1, spinnerElementoA.getValue());
				valInsert.put(Collegamenti.ID_ELEMENTO_CANT2, spinnerElementoB.getValue());
				if (!spinnerComponenteA.getValue().equals("")) {
					valInsert.put(Collegamenti.ID_COMPONENTE_CANT1, spinnerComponenteA.getValue());
				}
				if (!spinnerComponenteB.getValue().equals("")) {
					valInsert.put(Collegamenti.ID_COMPONENTE_CANT2, spinnerComponenteB.getValue());
				}
				String idCavoTxt = spinnerCavo.getValue();
				if (idCavoTxt.equals("")) {
					idCavoTxt = ID_CAVO_GENERICO;
				}
				valInsert.put(Collegamenti.ID_CAVO, idCavoTxt);
				valInsert.put(Collegamenti.METRI, Utility.formatNumeroDB(metriCavo.getText().toString()));
				valInsert.put(Collegamenti.METRI_CAVO_UNI, Utility.formatNumeroDB(metriCavoUni.getText().toString()));
				if (tipoCollegamento == CAVO) {
					valInsert.put(Collegamenti.ID_COLLEGAMENTO_TUBO, idCollegamentoTuboSelezionato);
				}
				String qtaCavoTxt = qtaCavo.getText().toString();
				if (qtaCavoTxt.trim().equals("") || qtaCavoTxt.equals("0")) {
					qtaCavoTxt = "1";
				}
				valInsert.put(Collegamenti.QTA_CAVO, qtaCavoTxt);
				String cmCodaTot = cmCoda.getText().toString();
				if (cmCodaTot.trim().equals("")) {
					cmCodaTot = "0";
				}
				double metriCoda = Double.parseDouble(cmCodaTot) / 100;
				valInsert.put(Collegamenti.METRI_CODA, metriCoda);
				tabCollegamenti.inserisciRecord(db, valInsert);
				idCollCavoInserito = valInsert.getAsInteger(Collegamenti.ID_COLLEGAMENTO);
			}
			if (tipoCollegamento == TUBO || tipoCollegamento == TUBO_CAVO) {
				ContentValues valInsert = tabCollegamenti.getValoriLogInserimento(db);
				valInsert.put(Collegamenti.ID_ORDINE, idOrdine);
				valInsert.put(Collegamenti.ID_ELEMENTO_CANT1, spinnerElementoA.getValue());
				valInsert.put(Collegamenti.ID_ELEMENTO_CANT2, spinnerElementoB.getValue());
				/*
				 * if (!spinnerComponenteA.getValue().equals("")) { valInsert.put(Collegamenti.ID_COMPONENTE_CANT1,
				 * spinnerComponenteA.getValue()); } if (!spinnerComponenteB.getValue().equals("")) {
				 * valInsert.put(Collegamenti.ID_COMPONENTE_CANT2, spinnerComponenteB.getValue()); }
				 */
				String idTuboTxt = spinnerTubo.getValue();
				if (idTuboTxt.equals("")) {
					idTuboTxt = ID_TUBO_GENERICO;
				}
				valInsert.put(Collegamenti.ID_TUBO, idTuboTxt);
				valInsert.put(Collegamenti.METRI, Utility.formatNumeroDB(metriTubo.getText().toString()));
				tabCollegamenti.inserisciRecord(db, valInsert);
				idCollTuboInserito = valInsert.getAsInteger(Collegamenti.ID_COLLEGAMENTO);
			}

			if (tipoCollegamento == TUBO_CAVO) {
				ContentValues valUpd = tabCollegamenti.getValoriLogModifica(db);
				ContentValues whereUpd = new ContentValues();
				whereUpd.put(Collegamenti.ID_COLLEGAMENTO, idCollCavoInserito);
				valUpd.put(Collegamenti.ID_COLLEGAMENTO_TUBO, idCollTuboInserito);
				tabCollegamenti.aggiornaRecord(db, valUpd, whereUpd);
			}

			db.close();


			Utility.mostraConfermaDialog("", getString(R.string.messaggio_nuovo_collegamento), this, getString(R.string.si), getString(R.string.no), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					if (i==DialogInterface.BUTTON_NEGATIVE){
						chiudi(findViewById(R.id.button_salva));
					}
					if (i==DialogInterface.BUTTON_POSITIVE){
						tipoCollegamento = CAVO;
						String localeB = spinnerLocaleB.getValue();
						String elementoB = spinnerElementoB.getValue();
						String componenteB = spinnerComponenteB.getValue();
						_inizializzaInserimento();
						spinnerLocaleB.setValue(localeB);
						spinnerLocaleB.setValoriSpinnerLibero(localiPerSpinner);
						spinnerElementoB.setValue(elementoB);
						spinnerElementoB.setValoriSpinnerLibero(elementiLocaleB);
						spinnerComponenteB.setValue(componenteB);
						spinnerComponenteB.setValoriSpinnerLibero(componentiElementoB);
						spinnerCavo.setValue("");
						spinnerCavo.setTabella(new Elementi());

					}
				}
			});
		} else {
			DbInterno db = new DbInterno(this);
			Collegamenti tabCollegamenti = new Collegamenti();
			ContentValues valUpd = tabCollegamenti.getValoriLogModifica(db);
			valUpd.put(Collegamenti.ID_ELEMENTO_CANT1, spinnerElementoA.getValue());
			valUpd.put(Collegamenti.ID_ELEMENTO_CANT2, spinnerElementoB.getValue());
			if (tipoCollegamento == CAVO && !spinnerComponenteA.getValue().equals("")) {
				valUpd.put(Collegamenti.ID_COMPONENTE_CANT1, spinnerComponenteA.getValue());
			} else {
				valUpd.put(Collegamenti.ID_COMPONENTE_CANT1, "0");
			}
			if (tipoCollegamento == CAVO && !spinnerComponenteB.getValue().equals("")) {
				valUpd.put(Collegamenti.ID_COMPONENTE_CANT2, spinnerComponenteB.getValue());
			} else {
				valUpd.put(Collegamenti.ID_COMPONENTE_CANT2, "0");
			}
			if (tipoCollegamento == TUBO) {
				String idTuboTxt = spinnerTubo.getValue();
				if (idTuboTxt.equals("")) {
					idTuboTxt = ID_TUBO_GENERICO;
				}
				valUpd.put(Collegamenti.ID_TUBO, idTuboTxt);
				valUpd.put(Collegamenti.METRI, Utility.formatNumeroDB(metriTubo.getText().toString()));

			}
			if (tipoCollegamento == CAVO) {
				String idCavoTxt = spinnerCavo.getValue();
				if (idCavoTxt.equals("")) {
					idCavoTxt = ID_CAVO_GENERICO;
				}
				valUpd.put(Collegamenti.ID_CAVO, idCavoTxt);
				valUpd.put(Collegamenti.METRI, Utility.formatNumeroDB(metriCavo.getText().toString()));
				valUpd.put(Collegamenti.METRI_CAVO_UNI, Utility.formatNumeroDB(metriCavoUni.getText().toString()));
				String qtaCavoTxt = qtaCavo.getText().toString();
				if (qtaCavoTxt.trim().equals("") || qtaCavoTxt.equals("0")) {
					qtaCavoTxt = "1";
				}
				valUpd.put(Collegamenti.QTA_CAVO, qtaCavoTxt);
				String cmCodaTot = cmCoda.getText().toString();
				if (cmCodaTot.trim().equals("")) {
					cmCodaTot = "0";
				}
				double metriCoda = Double.parseDouble(cmCodaTot) / 100;
				valUpd.put(Collegamenti.METRI_CODA, metriCoda);

				valUpd.put(Collegamenti.ID_COLLEGAMENTO_TUBO, idCollegamentoTuboSelezionato);

			}

			ContentValues where = new ContentValues();
			where.put(Collegamenti.ID_COLLEGAMENTO, idCollegamento);

			tabCollegamenti.aggiornaRecord(db, valUpd, where);
			db.close();

			chiudi(findViewById(R.id.button_salva));
		}

		System.out.println("EConTab: CollegamentoActivity _eseguiSalvataggio EXIT");
	}

	public void chiudi(View v) {
		finish();
	}

	public void cambiaQta(View v) {
		System.out.println("EConTab: CollegamentoActivity cambiaQta ENTER");
		String qtaTxt = qtaCavo.getText().toString();
		int qta = 0;
		if (!qtaTxt.equals("") && !qtaTxt.equals("0")) {
			qta = Integer.parseInt(qtaTxt);
		}

		if (v.getId() == R.id.buttonPiu) {
			qta = qta + 1;

		}
		if (v.getId() == R.id.buttonMeno) {
			qta = qta - 1;
			if (qta <= 0) {
				qta = 1;
			}
		}
		qtaCavo.setText("" + qta);
		System.out.println("EConTab: CollegamentoActivity cambiaQta EXIT");
	}

	public void selezionaTipo(View v) {
		System.out.println("EConTab: CollegamentoActivity selezionaTipo ENTER");
		findViewById(R.id.linearCavoMetri).setVisibility(View.VISIBLE);
		findViewById(R.id.linear_codaqta).setVisibility(View.VISIBLE);
		findViewById(R.id.linearTuboMetri).setVisibility(View.VISIBLE);

		buttonTubo.setSelected(false);
		buttonCavo.setSelected(false);
		buttonTuboCavo.setSelected(false);

		buttonTubo.setTextColor(getResources().getColor(android.R.color.black));
		buttonCavo.setTextColor(getResources().getColor(android.R.color.black));
		buttonTuboCavo.setTextColor(getResources().getColor(android.R.color.black));

		v.setSelected(true);
		((Button) v).setTextColor(getResources().getColor(android.R.color.white));
		if (v == buttonTubo) {
			tipoCollegamento = TUBO;
			findViewById(R.id.linearCavoMetri).setVisibility(View.GONE);
			findViewById(R.id.linear_codaqta).setVisibility(View.GONE);
			ricercaTubi();

		}
		if (v == buttonCavo) {
			tipoCollegamento = CAVO;
			findViewById(R.id.linearTuboMetri).setVisibility(View.GONE);
			ricercaTubi();

		}
		if (v == buttonTuboCavo) {
			tipoCollegamento = TUBO_CAVO;
			ricercaTubi();
		}
		System.out.println("EConTab: CollegamentoActivity selezionaTipo EXIT");
	}

	private void ricercaTubi() {
		System.out.println("EConTab: CollegamentoActivity ricercaTubi ENTER");
		if (tipoCollegamento == TUBO) {
			findViewById(R.id.linearCollTuboUsato).setVisibility(View.GONE);
			if (idCollegamento != 0) {
				findViewById(R.id.linearCollCaviPassati).setVisibility(View.VISIBLE);
				DbInterno db = new DbInterno(this);
                Join j0 = new Join(Collegamenti.NOME_TABELLA,Elementi.NOME_TABELLA,Join.LEFT_JOIN);
                j0.addCampiDiJoin(Collegamenti.ID_CAVO,Elementi.ID_ELEMENTO);



				ArrayList<Object> recs = db.eseguiSelect("Select count(*) as NUM_COLL, sum(" + Collegamenti.QTA_CAVO
						+ ") as NUM_CAVI,"+Elementi.NOME_ELEMENTO+" as nome_cavo from " + Collegamenti.NOME_TABELLA + j0.getSQLJoin()+ " where " + Collegamenti.ID_COLLEGAMENTO_TUBO + "="
						+ idCollegamento + " group by " + Collegamenti.ID_CAVO+","+Elementi.NOME_ELEMENTO, null);

				db.close();
                String testoTipoCavi = "";
				if (recs.size() > 0) {

                    for (int i=0;i<recs.size();i++){
                        ContentValues count = (ContentValues) recs.get(i);
                        int num_cavi = 0;
                        int num_coll = 0;
                        String nome_Cavo = "";
                        try {
                            num_cavi = count.getAsInteger("NUM_CAVI");
                            num_coll = count.getAsInteger("NUM_COLL");
                            if (count.containsKey("nome_cavo") && count.getAsString("nome_cavo")!=null){
                                nome_Cavo = count.getAsString("nome_cavo");

                            }

                            testoTipoCavi = testoTipoCavi + System.getProperty("line.separator")+ nome_Cavo + " : " + num_cavi + " " + getString(R.string.cavi) + " ("+num_coll+" " + getString(R.string.collegamenti)+")";
                        } catch (Exception e) {

                        }

                    }




					//setText(R.id.textView_caviPassanti,
					//		getString(R.string.messaggio_cavi_passanti, "" + num_cavi) + " (" + count.getAsInteger("NUM_COLL") + " "
				//					+ getString(R.string.collegamenti) + ")");
				}
                if (testoTipoCavi.equals("")){
                    testoTipoCavi =  " 0 " + getString(R.string.cavi);

                }
                String testoCavi = getString(R.string.messaggio_cavi_passanti)+testoTipoCavi;
                setText(R.id.textView_caviPassanti, testoCavi);
			} else {
				findViewById(R.id.linearCollCaviPassati).setVisibility(View.GONE);
			}

		}
		if (tipoCollegamento == CAVO) {
			findViewById(R.id.linearCollTuboUsato).setVisibility(View.VISIBLE);
			findViewById(R.id.linearCollCaviPassati).setVisibility(View.GONE);

			// TODO Auto-generated method stub
			listaTubiPassanti.invalidate();
			if (datiTubiPassanti == null) {
				datiTubiPassanti = new ArrayList<Object>();
			}
			datiTubiPassanti.clear();

			if (!spinnerElementoA.getValue().equals("") && !spinnerElementoB.getValue().equals("")) {

				DbInterno db = new DbInterno(this);

				Join j0 = new Join(Collegamenti.NOME_TABELLA, Elementi.NOME_TABELLA);
				j0.addCampiDiJoin(Collegamenti.ID_TUBO, Elementi.ID_ELEMENTO);
				String SQL = "Select " + Collegamenti.ID_COLLEGAMENTO + "," + Collegamenti.METRI + "," + Elementi.NOME_ELEMENTO
						+ ",(select sum(" + Collegamenti.QTA_CAVO + ") from " + Collegamenti.NOME_TABELLA + " as cavi where cavi."
						+ Collegamenti.ID_COLLEGAMENTO_TUBO + "=" + Collegamenti.NOME_TABELLA + "." + Collegamenti.ID_COLLEGAMENTO
						+ ") as NUM_CAVI,(select count(*) from " + Collegamenti.NOME_TABELLA + " as countcavi where countcavi."
						+ Collegamenti.ID_COLLEGAMENTO_TUBO + "=" + Collegamenti.NOME_TABELLA + "." + Collegamenti.ID_COLLEGAMENTO
						+ ") as COUNT_CAVI  from " + Collegamenti.NOME_TABELLA + j0.getSQLJoin() + " where " + Collegamenti.ID_TUBO
						+ "<>0 and ((" + Collegamenti.ID_ELEMENTO_CANT1 + " = " + spinnerElementoA.getValue() + " and "
						+ Collegamenti.ID_ELEMENTO_CANT2 + "=" + spinnerElementoB.getValue() + ") or (" + Collegamenti.ID_ELEMENTO_CANT2
						+ " = " + spinnerElementoA.getValue() + " and " + Collegamenti.ID_ELEMENTO_CANT1 + "="
						+ spinnerElementoB.getValue() + "))";

				datiTubiPassanti.addAll(db.eseguiSelect(SQL, null));

				db.close();
				for (int i = 0; i < datiTubiPassanti.size(); i++) {
					ContentValues curr = (ContentValues) datiTubiPassanti.get(i);
					if (curr.getAsInteger(Collegamenti.ID_COLLEGAMENTO) == idCollegamentoTuboSelezionato) {
						curr.put("SELEZIONATO", true);
					} else {
						curr.put("SELEZIONATO", false);
					}

				}

			}
			if (adapter == null) {
				adapter = new TubiPassantiAdapter(this, datiTubiPassanti, R.layout.list_item_tubo_passante);
				listaTubiPassanti.setAdapter(adapter);
				// listaTubiPassanti.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

				// registerForContextMenu(listaTubiPassanti);
			} else {
				adapter.notifyDataSetChanged();

			}

			if (datiTubiPassanti.size() > 0) {
				findViewById(R.id.textView_nessunTubo).setVisibility(View.GONE);
			} else {
				findViewById(R.id.textView_nessunTubo).setVisibility(View.GONE);
			}

			_impostaAltezzaListaTubi();

		}
		if (tipoCollegamento == TUBO_CAVO) {
			findViewById(R.id.linearCollTuboUsato).setVisibility(View.GONE);
			findViewById(R.id.linearCollCaviPassati).setVisibility(View.GONE);
		}
		System.out.println("EConTab: CollegamentoActivity ricercaTubi EXIT");
	}

	private void _impostaAltezzaListaTubi() {
		System.out.println("EConTab: CollegamentoActivity _impostaAltezzaListaTubi ENTER");
		if (adapter != null) {
			int totalHeight = 0;
			int desiredWidth = MeasureSpec.makeMeasureSpec(listaTubiPassanti.getWidth(), MeasureSpec.UNSPECIFIED);
			View listItem = null;
			for (int i = 0; i < adapter.getCount(); i++) {
				try {
					listItem = adapter.getView(i, listItem, listaTubiPassanti);
					listItem.findViewById(R.id.imgSeleziona).setVisibility(View.GONE);
					if (i == 0) {

						listItem.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, LayoutParams.WRAP_CONTENT));
					}

					listItem.measure(desiredWidth, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
					totalHeight += listItem.getMeasuredHeight();
					listItem.findViewById(R.id.imgSeleziona).setVisibility(View.VISIBLE);
				} catch (Exception e) {
					totalHeight += 20;
				}

			}

			ViewGroup.LayoutParams params = listaTubiPassanti.getLayoutParams();
			params.height = totalHeight + (listaTubiPassanti.getDividerHeight() * (adapter.getCount() - 1));

			listaTubiPassanti.setLayoutParams(params);
			listaTubiPassanti.requestLayout();
		}
		System.out.println("EConTab: CollegamentoActivity _impostaAltezzaListaTubi EXIT");
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		System.out.println("EConTab: CollegamentoActivity onItemClick");
		// TODO Auto-generated method stub

		ContentValues val = (ContentValues) datiTubiPassanti.get(position);
		int idCliccato = val.getAsInteger(Collegamenti.ID_COLLEGAMENTO);

		for (int i = 0; i < datiTubiPassanti.size(); i++) {
			ContentValues curr = (ContentValues) datiTubiPassanti.get(i);
			if (idCliccato != curr.getAsInteger(Collegamenti.ID_COLLEGAMENTO)) {
				curr.put("SELEZIONATO", false);
			}
		}

		val.put("SELEZIONATO", !val.getAsBoolean("SELEZIONATO"));
		if (val.getAsBoolean("SELEZIONATO") == true) {
			idCollegamentoTuboSelezionato = idCliccato;
            metriCavoUni.setText("" + val.getAsDouble(Collegamenti.METRI));
		} else {
			idCollegamentoTuboSelezionato = 0;
		}
		adapter.notifyDataSetChanged();
	}

}
