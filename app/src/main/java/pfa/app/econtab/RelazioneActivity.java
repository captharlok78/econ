package pfa.app.econtab;

import android.content.ContentValues;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Aree;
import pfa.app.econtab.db.table.ComponentiCantiere;
import pfa.app.econtab.db.table.ComposizioniCantiere;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.Relazioni;
import pfa.app.econtab.views.EConTabSpinner;

public class RelazioneActivity extends EConTabActivity {

    public static final String PUNTO_DI_COMANDO = "PUNTOCOMANDO";
    public static final String PUNTO_COMANDATO = "PUNTOCOMANDATO";


	private class MyTextWatcher implements TextWatcher {

		View view = null;

		public MyTextWatcher(View view) {
			this.view = view;
		}

		@Override
		public void afterTextChanged(Editable s) {
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
                spinnerComponenteB.setValue("");
                spinnerComponenteB.setValoriSpinnerLibero(componentiElementoB);

            }



			_impostaVisibilitaComponenti();

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

	private int idCantiere = 0;
	private int idElementoPartenza = 0;
    private int idComponentePartenza = 0;

	private int idRelazione = 0;

	private int idLocalePartenza = 0;
	private int idLocaleDestinazione = 0;

	private ArrayList<Object> localiPerSpinner = new ArrayList<Object>();
	private ArrayList<Object> elementiLocaleA = new ArrayList<Object>();
	private ArrayList<Object> componentiElementoA = new ArrayList<Object>();

    private ArrayList<Object> elementiLocaleB = new ArrayList<Object>();
    private ArrayList<Object> componentiElementoB = new ArrayList<Object>();


	private EConTabSpinner spinnerLocaleA = null;
    private EConTabSpinner spinnerElementoA = null;
	private EConTabSpinner spinnerComponenteA = null;

    private EConTabSpinner spinnerLocaleB = null;
    private EConTabSpinner spinnerElementoB = null;
    private EConTabSpinner spinnerComponenteB = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: RelazioneActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
        setVisualizzazionePopup(50,3);
		setContentView(R.layout.activity_relazione);


		idElementoPartenza = getIntent().getIntExtra("ID_ELEMENTO", 0);
        idComponentePartenza = getIntent().getIntExtra("ID_COMPONENTE", 0);
		idRelazione = getIntent().getIntExtra(Relazioni.ID_RELAZIONE, 0);
      	/*
		if (idRelazione==0){
            setVisualizzazionePopup(30,2);
        }
        else{
            setVisualizzazionePopup();
        }
		*/
		spinnerLocaleA = (EConTabSpinner) findViewById(R.id.spinnerLocaleA);
		spinnerElementoA = (EConTabSpinner) findViewById(R.id.spinnerElementoA);
		spinnerComponenteA = (EConTabSpinner) findViewById(R.id.spinnerComponenteA);

        spinnerLocaleB = (EConTabSpinner) findViewById(R.id.spinnerLocaleB);
        spinnerElementoB = (EConTabSpinner) findViewById(R.id.spinnerElementoB);
        spinnerComponenteB = (EConTabSpinner) findViewById(R.id.spinnerComponenteB);

		spinnerLocaleA.addTextChangeListener(new MyTextWatcher(spinnerLocaleA));
		spinnerElementoA.addTextChangeListener(new MyTextWatcher(spinnerElementoA));
		spinnerComponenteA.addTextChangeListener(new MyTextWatcher(spinnerComponenteA));

        spinnerLocaleB.addTextChangeListener(new MyTextWatcher(spinnerLocaleB));
        spinnerElementoB.addTextChangeListener(new MyTextWatcher(spinnerElementoB));
        spinnerComponenteB.addTextChangeListener(new MyTextWatcher(spinnerComponenteB));

		if (idRelazione == 0) {

			_inizializzaInserimento();

			// ((Button) findViewById(R.id.button_annulla)).setText(getString(R.string.annulla));

		} else {


			_inizializzaModifica();
		}
		System.out.println("EConTab: RelazioneActivity onCreate EXIT");
	}



	private void _inizializzaInserimento() {
		System.out.println("EConTab: RelazioneActivity _inizializzaInserimento ENTER");
		_inizializzaLocaliCantiere();

        boolean isPuntoDiComando = true;
        if (idComponentePartenza==0){
           // findViewById(R.id.linearElementoB).setVisibility(View.GONE);
        }
        //altrimenti dipende dal parametro in entrata (aggiungi punto di comando o aggiungi punto comandato)
        else{
            String verso = getIntent().getStringExtra("VERSO_RELAZIONE");
            if (verso!=null){
                isPuntoDiComando = verso.equals(PUNTO_DI_COMANDO);
            }
        }


        spinnerLocaleB.setValue("" + idLocalePartenza);
        spinnerLocaleB.setValoriSpinnerLibero(localiPerSpinner);

        spinnerLocaleA.setValue("" + idLocalePartenza);
        spinnerLocaleA.setValoriSpinnerLibero(localiPerSpinner);

        if (isPuntoDiComando){
            elementiLocaleB = _inizializzaELementiLocale(idLocalePartenza);
            spinnerElementoB.setValue("" + idElementoPartenza);
            spinnerElementoB.setValoriSpinnerLibero(elementiLocaleB);
            componentiElementoB = _inizializzaComponentiElemento(idElementoPartenza);
            spinnerComponenteB.setValue(""+idComponentePartenza);
            spinnerComponenteB.setValoriSpinnerLibero(componentiElementoB);
        }
        else{
            elementiLocaleA = _inizializzaELementiLocale(idLocalePartenza);
            spinnerElementoA.setValue("" + idElementoPartenza);
            spinnerElementoA.setValoriSpinnerLibero(elementiLocaleA);
            componentiElementoA = _inizializzaComponentiElemento(idElementoPartenza);
            spinnerComponenteA.setValue(""+idComponentePartenza);
            spinnerComponenteA.setValoriSpinnerLibero(componentiElementoA);
        }



		_impostaVisibilitaComponenti();
		System.out.println("EConTab: RelazioneActivity _inizializzaInserimento EXIT");
	}

	private void _impostaVisibilitaComponenti() {
		System.out.println("EConTab: RelazioneActivity _impostaVisibilitaComponenti ENTER");
		// TODO Auto-generated method stub
		if (componentiElementoA.size() == 0) {
			findViewById(R.id.textView_componenteA).setVisibility(View.GONE);
			spinnerComponenteA.setVisibility(View.GONE);
		} else {
			findViewById(R.id.textView_componenteA).setVisibility(View.VISIBLE);
			spinnerComponenteA.setVisibility(View.VISIBLE);
		}

        if (componentiElementoB.size() == 0) {
            findViewById(R.id.textView_componenteB).setVisibility(View.GONE);
            spinnerComponenteB.setVisibility(View.GONE);
        } else {
            findViewById(R.id.textView_componenteB).setVisibility(View.VISIBLE);
            spinnerComponenteB.setVisibility(View.VISIBLE);
        }

		System.out.println("EConTab: RelazioneActivity _impostaVisibilitaComponenti EXIT");
	}

	private ArrayList<Object> _inizializzaComponentiElemento(int idElemento) {
		System.out.println("EConTab: RelazioneActivity _inizializzaComponentiElemento ENTER");
		ArrayList<Object> componenti = new ArrayList<Object>();
		DbInterno db = new DbInterno(this);
		Relazioni tabRelazioni = new Relazioni();
		ArrayList<Integer> componentiCollegati = tabRelazioni.getComponentiCollegatiElemento(db, idElemento);

		ComposizioniCantiere tabComposizioniCantiere = new ComposizioniCantiere();
		ArrayList<Object> recs = tabComposizioniCantiere.getComposizioneElemento(db, idElemento);
		for (int i = 0; i < recs.size(); i++) {
			ContentValues curr = (ContentValues) recs.get(i);
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
		System.out.println("EConTab: RelazioneActivity _inizializzaComponentiElemento EXIT");
		return componenti;
	}

	private ArrayList<Object> _inizializzaELementiLocale(int idLocale) {
		System.out.println("EConTab: RelazioneActivity _inizializzaELementiLocale ENTER");
		ArrayList<Object> elementi = new ArrayList<Object>();
		DbInterno db = new DbInterno(this);
		ElementiCantiere tabElementi = new ElementiCantiere();
      //  boolean escludiUtilizzatori = true;
       // if (idRelazione!=0 || idComponentePartenza!=0){
       //     escludiUtilizzatori = false;
       // }
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
		System.out.println("EConTab: RelazioneActivity _inizializzaELementiLocale EXIT");
		return elementi;
	}

	private void _inizializzaLocaliCantiere() {
		System.out.println("EConTab: RelazioneActivity _inizializzaLocaliCantiere ENTER");
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
		System.out.println("EConTab: RelazioneActivity _inizializzaLocaliCantiere EXIT");
		db.close();
	}



	private void _inizializzaModifica() {
		System.out.println("EConTab: RelazioneActivity _inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Relazioni.ID_RELAZIONE, idRelazione);
		ContentValues rec = db.getRecord(new Relazioni(), where);
		if (rec != null) {
			idElementoPartenza = rec.getAsInteger(Relazioni.ID_ELEMENTO_CANT1);
			int idElementoDestinazione = rec.getAsInteger(Relazioni.ID_ELEMENTO_CANT2);
			int idComponentePartenza = rec.getAsInteger(Relazioni.ID_COMPONENTE_CANT1);
			int idComponenteDestinazione = rec.getAsInteger(Relazioni.ID_COMPONENTE_CANT2);
			_inizializzaLocaliCantiere();

            ContentValues whereElem = new ContentValues();
            whereElem.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoPartenza);
            ContentValues recElem = db.getRecord(new ElementiCantiere(), whereElem);
            if (recElem != null) {
                idLocalePartenza = recElem.getAsInteger(ElementiCantiere.ID_LOCALE);
            }
            spinnerLocaleB.setValue("" + idLocalePartenza);
            spinnerLocaleB.setValoriSpinnerLibero(localiPerSpinner);

            elementiLocaleB = _inizializzaELementiLocale(idLocalePartenza);
            spinnerElementoB.setValue("" + idElementoPartenza);
            spinnerElementoB.setValoriSpinnerLibero(elementiLocaleB);
            componentiElementoB = _inizializzaComponentiElemento(idElementoPartenza);

            if (idComponentePartenza != 0) {
                spinnerComponenteB.setValue("" + idComponentePartenza);
            }
            spinnerComponenteB.setValoriSpinnerLibero(componentiElementoB);


			ContentValues whereElem2 = new ContentValues();
			whereElem2.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoDestinazione);
			ContentValues recElem2 = db.getRecord(new ElementiCantiere(), whereElem2);
			if (recElem2 != null) {
				idLocaleDestinazione = recElem2.getAsInteger(ElementiCantiere.ID_LOCALE);
			}
            spinnerLocaleA.setValue("" + idLocaleDestinazione);
            spinnerLocaleA.setValoriSpinnerLibero(localiPerSpinner);

			elementiLocaleA = _inizializzaELementiLocale(idLocaleDestinazione);
			spinnerElementoA.setValue("" + idElementoDestinazione);
			spinnerElementoA.setValoriSpinnerLibero(elementiLocaleA);
			componentiElementoA = _inizializzaComponentiElemento(idElementoDestinazione);

			if (idComponenteDestinazione != 0) {
                spinnerComponenteA.setValue("" + idComponenteDestinazione);
			}
            spinnerComponenteA.setValoriSpinnerLibero(componentiElementoA);

			_impostaVisibilitaComponenti();

		}
		System.out.println("EConTab: RelazioneActivity _inizializzaModifica EXIT");
		db.close();
	}

	public void salva(View v) {
		System.out.println("EConTab: RelazioneActivity salva ENTER");
		if (spinnerElementoA.getValue().equals("")) {
			spinnerElementoA.setError(getString(R.string.errore_selezione_elemento_comando));
			Toast.makeText(this, getString(R.string.errore_selezione_elemento_comando), Toast.LENGTH_SHORT).show();
			return;
		}


		if (componentiElementoA.size() > 0 && spinnerComponenteA.getValue().equals("")) {
			spinnerComponenteA.setError(getString(R.string.errore_selezione_componente_comando));
			Toast.makeText(this, getString(R.string.errore_selezione_componente_comando), Toast.LENGTH_SHORT).show();
			return;
		}

        if (idRelazione!=0 || idComponentePartenza!=0){
            if (spinnerElementoB.getValue().equals("")) {
                spinnerElementoB.setError(getString(R.string.errore_selezione_elemento_comando));
                Toast.makeText(this, getString(R.string.errore_selezione_elemento_comando), Toast.LENGTH_SHORT).show();
                return;
            }


            if (componentiElementoB.size() > 0 && spinnerComponenteB.getValue().equals("")) {
                spinnerComponenteB.setError(getString(R.string.errore_selezione_componente_comando));
                Toast.makeText(this, getString(R.string.errore_selezione_componente_comando), Toast.LENGTH_SHORT).show();
                return;
            }
        }

		_eseguiSalvataggio();

		System.out.println("EConTab: RelazioneActivity salva EXIT");
	}

	private void _eseguiSalvataggio() {
		System.out.println("EConTab: RelazioneActivity _eseguiSalvataggio ENTER");
		// TODO Auto-generated method stub
		// inserisco o modifico
		if (idRelazione == 0) {
			DbInterno db = new DbInterno(this);
			Relazioni tabRelazioni = new Relazioni();
            ContentValues valInsert = tabRelazioni.getValoriLogInserimento(db);

            valInsert.put(Relazioni.ID_ELEMENTO_CANT1,  spinnerElementoB.getValue());

            valInsert.put(Relazioni.ID_ELEMENTO_CANT2, spinnerElementoA.getValue());
            String comp1 = spinnerComponenteB.getValue();
            if (comp1.equals("")){
                comp1 = "0";
            }
            valInsert.put(Relazioni.ID_COMPONENTE_CANT1, comp1);
            String comp2 = spinnerComponenteA.getValue();
            if (comp2.equals("")){
                comp2 = "0";
            }
            valInsert.put(Relazioni.ID_COMPONENTE_CANT2, comp2);

            tabRelazioni.inserisciRecord(db, valInsert);

			db.close();
		} else {
			DbInterno db = new DbInterno(this);
            Relazioni tabRelazioni = new Relazioni();
			ContentValues valUpd = tabRelazioni.getValoriLogModifica(db);
            valUpd.put(Relazioni.ID_ELEMENTO_CANT1, spinnerElementoB.getValue());
			valUpd.put(Relazioni.ID_ELEMENTO_CANT2, spinnerElementoA.getValue());
            String comp2 = spinnerComponenteA.getValue();
            if (comp2.equals("")){
                comp2 = "0";
            }
            valUpd.put(Relazioni.ID_COMPONENTE_CANT2, comp2);
            String comp1 = spinnerComponenteB.getValue();
            if (comp1.equals("")){
                comp1 = "0";
            }
            valUpd.put(Relazioni.ID_COMPONENTE_CANT1, comp1);


			ContentValues where = new ContentValues();
			where.put(Relazioni.ID_RELAZIONE, idRelazione);

			tabRelazioni.aggiornaRecord(db, valUpd, where);
			db.close();
		}
		System.out.println("EConTab: RelazioneActivity _eseguiSalvataggio EXIT");
		chiudi(findViewById(R.id.button_salva));
	}

	public void chiudi(View v) {
		finish();
	}

}
