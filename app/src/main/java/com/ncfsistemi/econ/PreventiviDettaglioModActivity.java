package com.ncfsistemi.econ;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import java.util.ArrayList;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.Locali;
import com.ncfsistemi.econ.db.table.Preventivi;
import com.ncfsistemi.econ.db.table.PreventiviDettaglio;
import com.ncfsistemi.econ.utils.EconAutoCompleteContentValue;
import com.ncfsistemi.econ.utils.Utility;
import com.ncfsistemi.econ.views.EconCalendario;
import com.ncfsistemi.econ.views.EconSpinner;

public class PreventiviDettaglioModActivity extends EconDettaglioActivity implements AdapterView.OnItemClickListener, TextWatcher {

	private int codicecliente = 0;
	private int codicecantiere = 0;
	private ArrayAdapter<Object> adapter = null;

	private EconSpinner spinnerTipo = null;
	private EconSpinner spinner_cantiere = null;
	private EconSpinner spinner_stato = null;
	private AutoCompleteTextView edit_cliente = null;
	private EconCalendario data = null;

	private int anno = 0;

    private String stato_prec = "";

	private class DataTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			// TODO Auto-generated method stub
			if (getModalita() == INSERIMENTO) {

				setText(R.id.editText_numero, getProssimoPreventivo());
			}
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("Econ: PreventiviDettaglioModActivity onCreate ENTER");
		setContentView(R.layout.activity_preventivi_dettaglio_mod);
		edit_cliente = (AutoCompleteTextView) findViewById(R.id.editText_cliente);
		data = (EconCalendario) findViewById(R.id.econCaledanrio_data);
		data.addTextChangeListener(new DataTextWatcher());

		spinnerTipo = (EconSpinner) findViewById(R.id.econSpinner_tipo);

		spinner_cantiere = (EconSpinner) findViewById(R.id.econSpinner_cantiere);
		spinner_cantiere.setEnabled(false);
		spinner_stato = (EconSpinner) findViewById(R.id.econSpinner_stato);

		codicecantiere = getIntent().getIntExtra(Cantieri.ID_CANTIERE, 0);

		super.onCreate(savedInstanceState);

		if (getModalita() == MODIFICA) {
			edit_cliente.setEnabled(false);
			// spinner_cantiere.setEnabled(false);
			// data.setEnabled(false);
			spinnerTipo.setEnabled(false);
			findViewById(R.id.button_nuovo).setVisibility(View.GONE);
		} else {
			DbInterno db = new DbInterno(this);
			if (codicecantiere != 0) {
				spinner_cantiere.setValue("" + codicecantiere);

				ContentValues where = new ContentValues();
				where.put(Cantieri.ID_CANTIERE, codicecantiere);
				ContentValues valcantiere = db.getRecord(new Cantieri(), where);

				if (valcantiere != null) {

					ContentValues wherecliente = new ContentValues();
					wherecliente.put(Anagrafica.ID_ANAGRAFICA, valcantiere.getAsInteger(Cantieri.ID_ANAGRAFICA));
					ContentValues valcliente = db.getRecord(new Anagrafica(), wherecliente);
					codicecliente = valcantiere.getAsInteger(Cantieri.ID_ANAGRAFICA);
					setText(R.id.editText_cliente, valcliente.getAsString(Anagrafica.RAGIONE_SOCIALE));
					edit_cliente.setTextColor(Color.BLACK);
				}

			}
			// se c'� solo un cliente allora imposto quello come cliente del cantiere
			else {
				int numCl = db.eseguiCount(new Anagrafica(), new ContentValues());
				if (numCl == 1) {
					ContentValues valCl = db.getRecord(new Anagrafica(), new ContentValues());
					codicecliente = valCl.getAsInteger(Anagrafica.ID_ANAGRAFICA);
					setText(R.id.editText_cliente, valCl.getAsString(Anagrafica.RAGIONE_SOCIALE));
					edit_cliente.setTextColor(Color.BLACK);

				}

			}
			db.close();

			spinner_stato.setValue(Preventivi.STATO_APERTO);

			edit_cliente.addTextChangedListener(this);
			edit_cliente.setOnItemClickListener(this);

			String tipoDefault = Preventivi.TIPO_PREVENTIVO;
			if (getIntent().getStringExtra(Preventivi.TIPO) != null) {
				tipoDefault = getIntent().getStringExtra(Preventivi.TIPO);
			}
			spinnerTipo.setValue(tipoDefault);

		}
		spinner_cantiere.setTabella(new Cantieri());
		spinner_cantiere.setMessaggioDisabilitato(getString(R.string.errore_selezione_cliente));
		if (codicecliente != 0) {
			spinner_cantiere.setEnabled(true);
			ContentValues filtro = new ContentValues();
			filtro.put(Cantieri.ID_ANAGRAFICA, codicecliente);

			spinner_cantiere.setFiltroEAggiorna(filtro);
		}

		spinnerTipo.setValoriSpinnerLibero(new Preventivi().getTipiDocumento(this));

		spinnerTipo.addTextChangeListener(new TextWatcher() {

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

				if (spinnerTipo.getValue().equals(Preventivi.TIPO_PREVENTIVO)) {
					setText(R.id.editText_numero, getProssimoPreventivo());
					spinner_stato.setValoriSpinnerLibero(new Preventivi().getStatiPreventivo(PreventiviDettaglioModActivity.this));
				} else {
					setText(R.id.editText_numero, getProssimoOrdine());
					spinner_stato.setValoriSpinnerLibero(new Preventivi().getStatiOrdine(PreventiviDettaglioModActivity.this));
				}

			}
		});
		System.out.println("Econ: PreventiviDettaglioModActivity onCreate EXIT");
	}

	private String getProssimoPreventivo() {
		System.out.println("Econ: PreventiviDettaglioModActivity getProssimoPreventivo ENTER");
		// TODO Auto-generated method stub
		anno = data.getAnno();
		String prossimo = "1";
		DbInterno db = new DbInterno(this);
		Preventivi tabPrev = new Preventivi();
		int prossimoInt = tabPrev.getProssimoNumeroPreventivo(db, anno);
		prossimo = "" + prossimoInt;
		db.close();
		System.out.println("Econ: PreventiviDettaglioModActivity getProssimoPreventivo EXIT");
		return prossimo;
	}

	private String getProssimoOrdine() {
		System.out.println("Econ: PreventiviDettaglioModActivity getProssimoOrdine ENTER");
		// TODO Auto-generated method stub
		anno = data.getAnno();
		String prossimo = "1";
		DbInterno db = new DbInterno(this);
		Preventivi tabPrev = new Preventivi();
		int prossimoInt = tabPrev.getProssimoNumeroOrdine(db, anno);
		prossimo = "" + prossimoInt;
		db.close();
		System.out.println("Econ: PreventiviDettaglioModActivity getProssimoOrdine EXIT");
		return prossimo;
	}

	@Override
	protected void onResume() {
		System.out.println("Econ: PreventiviDettaglioModActivity onResume ENTER");
		// TODO Auto-generated method stub
		super.onResume();
		if (getModalita() == INSERIMENTO) {

			adapter = Utility.getArrayAdapterTabella(this, "Select * from " + Anagrafica.NOME_TABELLA, Anagrafica.RAGIONE_SOCIALE);
			edit_cliente.setAdapter(adapter);
			if (spinnerTipo.getValue().equals(Preventivi.TIPO_PREVENTIVO)) {
				setText(R.id.editText_numero, getProssimoPreventivo());
				spinner_stato.setValoriSpinnerLibero(new Preventivi().getStatiPreventivo(this));
			} else {
				setText(R.id.editText_numero, getProssimoOrdine());
				spinner_stato.setValoriSpinnerLibero(new Preventivi().getStatiOrdine(this));
			}

		}
		System.out.println("Econ: PreventiviDettaglioModActivity onResume EXIT");
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("Econ: PreventiviDettaglioModActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub

		super.inizializzaModifica();
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Preventivi.ID_PREVENTIVO, getIDModifica());
		ContentValues val = db.getRecord(new Preventivi(), where);

		if (val != null) {
            spinner_cantiere.setEnabled(false);
            edit_cliente.setEnabled(false);

            stato_prec = val.getAsString(Preventivi.STATO);
			codicecantiere = val.getAsInteger(Preventivi.ID_CANTIERE);

			where = new ContentValues();
			where.put(Cantieri.ID_CANTIERE, codicecantiere);
			ContentValues valCantiere = db.getRecord(new Cantieri(), where);
			if (valCantiere != null) {
				where = new ContentValues();
				where.put(Anagrafica.ID_ANAGRAFICA, valCantiere.getAsInteger(Cantieri.ID_ANAGRAFICA));
				codicecliente = valCantiere.getAsInteger(Cantieri.ID_ANAGRAFICA);
				ContentValues valCliente = db.getRecord(new Anagrafica(), where);
				if (valCliente != null) {

					setText(R.id.editText_cliente, valCliente.getAsString(Anagrafica.RAGIONE_SOCIALE));
					edit_cliente.setTextColor(Color.BLACK);
				}
			}

			spinner_cantiere.setValue("" + codicecantiere);


			setText(R.id.editText_numero, val.getAsString(Preventivi.NUMERO));
			data.setValue("" + val.getAsLong(Preventivi.DATA));
			setText(R.id.editText_descrizione, val.getAsString(Preventivi.TITOLO));
			spinner_stato.setValue(val.getAsString(Preventivi.STATO));
			setText(R.id.editText_note, val.getAsString(Anagrafica.NOTE));
			spinnerTipo.setValue(val.getAsString(Preventivi.TIPO));
			if (val.getAsString(Preventivi.TIPO).equals(Preventivi.TIPO_PREVENTIVO)){
				spinner_stato.setValoriSpinnerLibero(new Preventivi().getStatiPreventivo(this));
			}
			else{
				spinner_stato.setValoriSpinnerLibero(new Preventivi().getStatiOrdine(this));
				if (spinner_stato.getValue().equals(Preventivi.STATO_CHIUSO)){
					spinner_stato.setEnabled(false);
				}
			}
		}
		db.close();
		System.out.println("Econ: PreventiviDettaglioModActivity inizializzaModifica EXIT");
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("Econ: PreventiviDettaglioModActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		AutoCompleteTextView edit_cliente = (AutoCompleteTextView) findViewById(R.id.editText_cliente);

		if (codicecliente == 0) {
			edit_cliente.setError(getString(R.string.errore_selezione_cliente));
			return getString(R.string.errore_selezione_cliente);
		}

		if (spinner_cantiere.getValue().equals("")) {
			spinner_cantiere.setError(getString(R.string.errore_selezione_cantiere));
			return getString(R.string.errore_selezione_cantiere);
		}

		Preventivi prev = new Preventivi();
		ContentValues val = prev.getValoriLogInserimento(db);
		val.put(Preventivi.ID_CANTIERE, spinner_cantiere.getValue());

		val.put(Preventivi.NUMERO, getTesto(R.id.editText_numero));
		val.put(Preventivi.ANNO, data.getAnno());
		val.put(Preventivi.DATA, Long.parseLong(data.getValue()));
		val.put(Preventivi.TITOLO, getTesto(R.id.editText_descrizione));
		val.put(Preventivi.NOTE, getTesto(R.id.editText_note));
		val.put(Preventivi.STATO, spinner_stato.getValue());
		val.put(Preventivi.TIPO, spinnerTipo.getValue());

		prev.inserisciRecord(db, val);
		System.out.println("Econ: PreventiviDettaglioModActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("Econ: PreventiviDettaglioModActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		Preventivi cant = new Preventivi();

		ContentValues val = cant.getValoriLogModifica(db);

		System.out.println("Econ: PreventiviDettaglioModActivity eseguiAggiornamento spinner_cantiere.getValue() -> " + spinner_cantiere.getValue());
		System.out.println("Econ: PreventiviDettaglioModActivity eseguiAggiornamento Long.parseLong(data.getValue()) -> " + Long.parseLong(data.getValue()));
		System.out.println("Econ: PreventiviDettaglioModActivity eseguiAggiornamento getTesto(R.id.editText_descrizione) -> " + getTesto(R.id.editText_descrizione));
		System.out.println("Econ: PreventiviDettaglioModActivity eseguiAggiornamento getTesto(R.id.editText_note) -> " + getTesto(R.id.editText_note));
		System.out.println("Econ: PreventiviDettaglioModActivity eseguiAggiornamento spinner_stato.getValue() -> " + spinner_stato.getValue());

		val.put(Preventivi.ID_CANTIERE, spinner_cantiere.getValue());

		val.put(Preventivi.DATA, Long.parseLong(data.getValue()));
		val.put(Preventivi.TITOLO, getTesto(R.id.editText_descrizione));
		val.put(Preventivi.NOTE, getTesto(R.id.editText_note));
		val.put(Preventivi.STATO, spinner_stato.getValue());

		System.out.println("Econ: PreventiviDettaglioModActivity eseguiAggiornamento getIDModifica() -> " + getIDModifica());

		ContentValues where = new ContentValues();
		where.put(Preventivi.ID_PREVENTIVO, getIDModifica());

		cant.aggiornaRecord(db, val, where);
        if (spinner_stato.getValue().equals(Preventivi.STATO_APERTO) && (stato_prec.equals(Preventivi.STATO_ACCETTATO) || stato_prec.equals(Preventivi.STATO_RIFIUTATO))) {
            //aggiorno le placche del preventivo dai locali
            Locali tabLoc = new Locali();
            ArrayList<Object> locali =  tabLoc.getLocaliCantiere(db, Integer.parseInt(spinner_cantiere.getValue()));
            PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();
           // ArrayList<Object> righePlacche = tabPrevDett.getRighePreventivo(db, getIDModifica(), PreventiviDettaglio.PLACCHE);

            for (int i=0;i<locali.size();i++){

				tabPrevDett.aggiornaPlaccheLocalePreventivo(db,getIDModifica(),((ContentValues)locali.get(i)).getAsInteger(Locali.ID_LOCALE));
               // tabLoc.aggiornaPlaccheDaLocale(righePlacche,(ContentValues)locali.get(i),((ContentValues)locali.get(i)).getAsInteger(Locali.ID_LOCALE),getIDModifica(),db);
            }
        }

		//se lo stato precedente era aperto e adesso lo sto chiudendo allora effettuo lo spsostamento degli elementi nel cantiere
		if (spinnerTipo.getValue().equals(Preventivi.TIPO_ORDINE) && spinner_stato.getValue().equals(Preventivi.STATO_CHIUSO) && stato_prec.equals(Preventivi.STATO_APERTO)){
			Preventivi tabPrev = new Preventivi();

			tabPrev.chiudiOrdine(db,getIDModifica());

		}
		System.out.println("Econ: PreventiviDettaglioModActivity eseguiAggiornamento EXIT");

		return super.eseguiAggiornamento(db);
	}

	public void nuovoCliente(View v) {
		System.out.println("Econ: PreventiviDettaglioModActivity nuovoCliente ENTER");
		Intent intent = new Intent(this, ClientiDettaglioModActivity.class);
		apriFinestraInserimento(intent, 1, new Anagrafica());
		System.out.println("Econ: PreventiviDettaglioModActivity nuovoCliente EXIT");
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

		codicecliente = 0;
		spinner_cantiere.setEnabled(false);
		edit_cliente.setTextColor(Color.GRAY);

	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		System.out.println("Econ: PreventiviDettaglioModActivity onItemClick ENTER");
		// TODO Auto-generated method stub
		ContentValues val = ((EconAutoCompleteContentValue) arg0.getItemAtPosition(position)).getContentValue();
		codicecliente = val.getAsInteger(Anagrafica.ID_ANAGRAFICA);
		spinner_cantiere.setEnabled(true);
		ContentValues filtro = new ContentValues();
		filtro.put(Cantieri.ID_ANAGRAFICA, codicecliente);

		edit_cliente.setTextColor(Color.BLACK);

		spinner_cantiere.setFiltroEAggiorna(filtro);
		System.out.println("Econ: PreventiviDettaglioModActivity onItemClick EXIT");
	}


    @Override
    public String getMessaggioConfermaSalvataggio() {
		System.out.println("Econ: PreventiviDettaglioModActivity getMessaggioConfermaSalvataggio ENTER");
        if (getModalita()==MODIFICA) {
            String nuovoStato = spinner_stato.getValue();
            if (nuovoStato.equals(Preventivi.STATO_APERTO) && (stato_prec.equals(Preventivi.STATO_ACCETTATO) || stato_prec.equals(Preventivi.STATO_RIFIUTATO))) {
                return getString(R.string.messaggio_variazione_placche);
            }

			if (spinnerTipo.getValue().equals(Preventivi.TIPO_ORDINE) && nuovoStato.equals(Preventivi.STATO_CHIUSO) && stato_prec.equals(Preventivi.STATO_APERTO)){
				return getString(R.string.messaggio_chiusura_ordine);
			}
        }
		System.out.println("Econ: PreventiviDettaglioModActivity getMessaggioConfermaSalvataggio EXIT");
        return super.getMessaggioConfermaSalvataggio();
    }
	/** Preventivo o ordine, secondo il tipo del documento aperto. */
	@Override
	protected String moduloFunzionalita() {
		return com.ncfsistemi.econ.utils.FunzionalitaApp.moduloPreventivo(spinnerTipo != null ? spinnerTipo.getValue() : null);
	}

}
