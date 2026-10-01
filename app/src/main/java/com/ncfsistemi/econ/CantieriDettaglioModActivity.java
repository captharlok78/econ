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
import android.widget.EditText;

import java.util.ArrayList;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Aree;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Locali;
import com.ncfsistemi.econ.db.table.Placche;
import com.ncfsistemi.econ.db.table.Unita;
import com.ncfsistemi.econ.utils.EconAutoCompleteContentValue;
import com.ncfsistemi.econ.utils.Utility;
import com.ncfsistemi.econ.views.EconSpinner;

public class CantieriDettaglioModActivity extends EconDettaglioActivity implements AdapterView.OnItemClickListener, TextWatcher {

	private int codicecliente;
	private ArrayAdapter<Object> adapter = null;
	private EconSpinner spinner_linea = null;
	private EconSpinner spinner_placca = null;
	private AutoCompleteTextView edit_cliente = null;
	private String idLineaPrec = "0";
    private boolean cambiaLineaLocali = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("Econ: CantieriDettaglioModActivity onCreate ENTER");
		setContentView(R.layout.activity_cantieri_dettaglio_mod);
		super.onCreate(savedInstanceState);
		codicecliente = getIntent().getIntExtra(Anagrafica.ID_ANAGRAFICA, 0);

		// provo a vedere se viene passato come stringa
		if (codicecliente == 0) {
			String codClieStr = getIntent().getStringExtra(Anagrafica.ID_ANAGRAFICA);
			if (codClieStr != null && codClieStr.length() > 0) {
				try {
					codicecliente = Integer.parseInt(codClieStr);
				} catch (Exception e) {

				}
			}
		}

		edit_cliente = (AutoCompleteTextView) findViewById(R.id.editText_cliente);

		spinner_linea = (EconSpinner) findViewById(R.id.econSpinner_linea);
		spinner_placca = (EconSpinner) findViewById(R.id.econSpinner_placca);

		if (getModalita() == MODIFICA) {
			edit_cliente.setEnabled(false);
			findViewById(R.id.button_nuovo).setVisibility(View.GONE);
		} else {
			// se c'� solo un cliente allora imposto quello come cliente del cantiere
			DbInterno dbcl = new DbInterno(this);
			int numCl = dbcl.eseguiCount(new Anagrafica(), new ContentValues());
			if (numCl == 1) {
				ContentValues valCl = dbcl.getRecord(new Anagrafica(), new ContentValues());
				codicecliente = valCl.getAsInteger(Anagrafica.ID_ANAGRAFICA);
			}
			dbcl.close();

			// cantiere da un indirizzo del cliente (GESTIONE_CLIENTI.md §6): extra ClientiIndirizzi.EXTRA_INDIRIZZO
			idIndirizzoCliente = getIntent().getIntExtra(EXTRA_INDIRIZZO, 0);
			if (idIndirizzoCliente != 0) {
				DbInterno dbi = new DbInterno(this);
				ContentValues w = new ContentValues();
				w.put(com.ncfsistemi.econ.db.table.ClientiIndirizzi.ID, idIndirizzoCliente);
				ContentValues ind = dbi.getRecord(new com.ncfsistemi.econ.db.table.ClientiIndirizzi(), w);
				dbi.close();
				if (ind != null) {
					codicecliente = ind.getAsInteger(com.ncfsistemi.econ.db.table.ClientiIndirizzi.ID_ANAGRAFICA);
				}
			}
			if (codicecliente != 0) {
				DbInterno db = new DbInterno(this);
				ContentValues where = new ContentValues();
				where.put(Anagrafica.ID_ANAGRAFICA, codicecliente);
				ContentValues val = db.getRecord(new Anagrafica(), where);
				db.close();
				setText(R.id.editText_cliente, val.getAsString(Anagrafica.RAGIONE_SOCIALE));
				edit_cliente.setTextColor(Color.BLACK);
				// cantiere "libero" dalla scheda cliente: solo il cliente, nome e indirizzo si scrivono a mano
				if (!getIntent().getBooleanExtra(EXTRA_LIBERO, false)) {
					impostaIndirizzoCliente(val, idIndirizzoCliente);
				}

				((EditText) findViewById(R.id.editText_cantiere)).requestFocus();

			}
			// imposto i listener solo in inserimento e dopo aver chiamato il primo settext
			edit_cliente.setOnItemClickListener(this);
			edit_cliente.addTextChangedListener(this);

		}
		spinner_linea.setTabella(new Linee());
		spinner_placca.setTabella(new Placche());
		spinner_placca.setMessaggioDisabilitato(getString(R.string.errore_selezione_linea));
		spinner_linea.setSpinnerCollegato(spinner_placca);
		// linea e placca standard: solo con la gestione elettrica (GESTIONE_ELETTRICA.md); nascoste, i valori restano
		if (!com.ncfsistemi.econ.utils.FunzionalitaApp.haModulo(this, com.ncfsistemi.econ.utils.FunzionalitaApp.ELETTRICO)) {
			findViewById(R.id.linear_linea_placca).setVisibility(View.GONE);
		}
		System.out.println("Econ: CantieriDettaglioModActivity onCreate EXIT");
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("Econ: CantieriDettaglioModActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub

		super.inizializzaModifica();
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Cantieri.ID_CANTIERE, getIDModifica());
		ContentValues val = db.getRecord(new Cantieri(), where);
		db.close();
		if (val != null) {

			codicecliente = val.getAsInteger(Cantieri.ID_ANAGRAFICA);
			db = new DbInterno(this);
			where = new ContentValues();
			where.put(Anagrafica.ID_ANAGRAFICA, codicecliente);
			ContentValues valCliente = db.getRecord(new Anagrafica(), where);
			db.close();
			if (valCliente != null) {
				setText(R.id.editText_cliente, valCliente.getAsString(Anagrafica.RAGIONE_SOCIALE));
				((AutoCompleteTextView) findViewById(R.id.editText_cliente)).setTextColor(Color.BLACK);
			}

			setText(R.id.editText_cantiere, val.getAsString(Cantieri.NOME));
			setText(R.id.editText_indirizzo, val.getAsString(Cantieri.INDIRIZZO));
			setText(R.id.editText_civico, val.getAsString(Cantieri.CIVICO));
			setText(R.id.editText_cap, val.getAsString(Cantieri.CAP));
			setText(R.id.editText_citta, val.getAsString(Cantieri.CITTA));
			setText(R.id.editText_provincia, val.getAsString(Cantieri.PROVINCIA));
			setText(R.id.econSpinner_linea, val.getAsString(Cantieri.ID_LINEA));
			idLineaPrec = val.getAsString(Cantieri.ID_LINEA);
			setText(R.id.econSpinner_placca, val.getAsString(Cantieri.ID_PLACCA));
			setText(R.id.editText_note, val.getAsString(Anagrafica.NOTE));
		}
		System.out.println("Econ: CantieriDettaglioModActivity inizializzaModifica EXIT");
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("Econ: CantieriDettaglioModActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		AutoCompleteTextView edit_cliente = (AutoCompleteTextView) findViewById(R.id.editText_cliente);

		if (codicecliente == 0) {
			edit_cliente.setError(getString(R.string.errore_selezione_cliente));
			return getString(R.string.errore_selezione_cliente);
		}
		Cantieri cant = new Cantieri();
		ContentValues val = cant.getValoriLogInserimento(db);
		val.put(Cantieri.ID_ANAGRAFICA, codicecliente);
		val.put(Cantieri.ID_CLIENTE_INDIRIZZO, idIndirizzoCliente);
		val.put(Cantieri.NOME, getTesto(R.id.editText_cantiere));
		val.put(Cantieri.INDIRIZZO, getTesto(R.id.editText_indirizzo));
		val.put(Cantieri.CIVICO, getTesto(R.id.editText_civico));
		val.put(Cantieri.CITTA, getTesto(R.id.editText_citta));
		val.put(Cantieri.CAP, getTesto(R.id.editText_cap));
		val.put(Cantieri.PROVINCIA, getTesto(R.id.editText_provincia));
		String idLinea = getTesto(R.id.econSpinner_linea);
		if (idLinea.equals("")) {
			idLinea = "0";
		}
		val.put(Cantieri.ID_LINEA, idLinea);
		String idPlacca = getTesto(R.id.econSpinner_placca);
		if (idPlacca.equals("")) {
			idPlacca = "0";
		}
		val.put(Cantieri.ID_PLACCA, idPlacca);
		val.put(Cantieri.NOTE, getTesto(R.id.editText_note));

		cant.inserisciNuovoCantiere(val, db, this);
		System.out.println("Econ: CantieriDettaglioModActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("Econ: CantieriDettaglioModActivity eseguiAggiornamento ENTER");
        //prima cambio la linea su tutti i locali
        String idLineaNuova = getTesto(R.id.econSpinner_linea);
        if (idLineaNuova.equals("")) {
            idLineaNuova = "0";
        }
        String idPlaccaNuova = getTesto(R.id.econSpinner_placca);
        if (idPlaccaNuova.equals("")) {
            idPlaccaNuova = "0";
        }
        if (!idLineaNuova.equals(idLineaPrec) && cambiaLineaLocali==true) {
            Locali tabLoc = new Locali();
            ArrayList<Object> listaLocali = tabLoc.getLocaliCantiere(db, getIDModifica());
            for (int i=0;i<listaLocali.size();i++){
                ContentValues valUpd = tabLoc.getValoriLogModifica(db);
                valUpd.put(Locali.ID_LINEA,idLineaNuova);
                valUpd.put(Locali.ID_PLACCA,idPlaccaNuova);

                ContentValues whereLoc = new ContentValues();
                whereLoc.put(Locali.ID_LOCALE,((ContentValues)listaLocali.get(i)).getAsInteger(Locali.ID_LOCALE));
                tabLoc.aggiornaRecord(db,valUpd,whereLoc);
            }

            //aggiorno anche sulle aree e sulle unità
            Aree tabAree = new Aree();
            ArrayList<Object> listaAree = tabAree.getAreeCantiere(db,getIDModifica());
            for (int i=0;i<listaAree.size();i++){
                ContentValues valUpd = tabAree.getValoriLogModifica(db);
                valUpd.put(Aree.ID_LINEA,idLineaNuova);
                valUpd.put(Aree.ID_PLACCA,idPlaccaNuova);

                ContentValues whereLoc = new ContentValues();
                whereLoc.put(Aree.ID_AREA,((ContentValues)listaAree.get(i)).getAsInteger(Aree.ID_AREA));
                tabAree.aggiornaRecord(db,valUpd,whereLoc);
            }

            Unita tabUnita = new Unita();
            ContentValues whereCantiere = new ContentValues();
            whereCantiere.put(Unita.ID_CANTIERE,getIDModifica());
            ArrayList<Object> listaUnita = db.eseguiSelect(tabUnita,whereCantiere,null);
            for (int i=0;i<listaUnita.size();i++){
                ContentValues valUpd = tabUnita.getValoriLogModifica(db);
                valUpd.put(Unita.ID_LINEA,idLineaNuova);
                valUpd.put(Unita.ID_PLACCA,idPlaccaNuova);

                ContentValues whereLoc = new ContentValues();
                whereLoc.put(Unita.ID_UNITA,((ContentValues)listaUnita.get(i)).getAsInteger(Unita.ID_UNITA));
                tabUnita.aggiornaRecord(db,valUpd,whereLoc);
            }

        }


		Cantieri cant = new Cantieri();

		ContentValues val = cant.getValoriLogModifica(db);

		val.put(Cantieri.NOME, getTesto(R.id.editText_cantiere));
		val.put(Cantieri.INDIRIZZO, getTesto(R.id.editText_indirizzo));
		val.put(Cantieri.CIVICO, getTesto(R.id.editText_civico));
		val.put(Cantieri.CITTA, getTesto(R.id.editText_citta));
		val.put(Cantieri.CAP, getTesto(R.id.editText_cap));
		val.put(Cantieri.PROVINCIA, getTesto(R.id.editText_provincia));
		String idLinea = getTesto(R.id.econSpinner_linea);
		if (idLinea.equals("")) {
			idLinea = "0";
		}
		val.put(Cantieri.ID_LINEA, idLinea);
		String idPlacca = getTesto(R.id.econSpinner_placca);
		if (idPlacca.equals("")) {
			idPlacca = "0";
		}
		val.put(Cantieri.ID_PLACCA, idPlacca);
		val.put(Cantieri.NOTE, getTesto(R.id.editText_note));

		ContentValues where = new ContentValues();
		where.put(Cantieri.ID_CANTIERE, getIDModifica());

		cant.aggiornaRecord(db, val, where);
		System.out.println("Econ: CantieriDettaglioModActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

	@Override
	protected void onResume() {
		System.out.println("Econ: CantieriDettaglioModActivity onResume");
		// TODO Auto-generated method stub
		super.onResume();
		if (getModalita() == INSERIMENTO) {
			adapter = Utility.getArrayAdapterTabella(this, "Select * from " + Anagrafica.NOME_TABELLA, Anagrafica.RAGIONE_SOCIALE);
			edit_cliente.setAdapter(adapter);
		}
	}

	/** Extra: id dell'indirizzo del cliente da cui nasce il cantiere (resta collegato). */
	public static final String EXTRA_INDIRIZZO = "id_cliente_indirizzo";
	/** Extra: true = cantiere libero del cliente, senza nome e indirizzo proposti (scheda cliente, tabella Cantieri). */
	public static final String EXTRA_LIBERO = "cantiere_libero";
	private int idIndirizzoCliente = 0;
	/** Nome del cantiere proposto in automatico: se l'utente non l'ha toccato, cambiando cliente si aggiorna. */
	private String nomeProposto = "";

	/**
	 * Indirizzo del cantiere dal cliente: dall'indirizzo indicato (idIndirizzo) oppure dalla sede principale, con via e
	 * civico separati; senza indirizzi (dati non ancora sincronizzati) quello scritto sul cliente.
	 */
	private void impostaIndirizzoCliente(ContentValues cliente, int idIndirizzo) {
		// nome proposto = ragione sociale (cantiere alla sede del cliente), solo se vuoto o ancora quello proposto prima
		String nome = getTesto(R.id.editText_cantiere);
		if (nome.isEmpty() || nome.equals(nomeProposto)) {
			String rs = cliente.getAsString(Anagrafica.RAGIONE_SOCIALE);
			nomeProposto = rs == null ? "" : (rs.length() > 100 ? rs.substring(0, 100) : rs);
			setText(R.id.editText_cantiere, nomeProposto);
		}
		DbInterno db = new DbInterno(this);
		String t = com.ncfsistemi.econ.db.table.ClientiIndirizzi.NOME_TABELLA;
		java.util.ArrayList<Object> r = db.eseguiSelect("SELECT * FROM " + t + " WHERE "
				+ (idIndirizzo != 0 ? "id = " + idIndirizzo : "id_anagrafica = " + cliente.getAsInteger(Anagrafica.ID_ANAGRAFICA) + " AND principale = 1")
				+ " LIMIT 1", null);
		db.close();
		if (!r.isEmpty()) {
			ContentValues i = (ContentValues) r.get(0);
			setText(R.id.editText_indirizzo, i.getAsString(com.ncfsistemi.econ.db.table.ClientiIndirizzi.INDIRIZZO));
			setText(R.id.editText_civico, i.getAsString(com.ncfsistemi.econ.db.table.ClientiIndirizzi.CIVICO));
			setText(R.id.editText_cap, i.getAsString(com.ncfsistemi.econ.db.table.ClientiIndirizzi.CAP));
			setText(R.id.editText_citta, i.getAsString(com.ncfsistemi.econ.db.table.ClientiIndirizzi.CITTA));
			setText(R.id.editText_provincia, i.getAsString(com.ncfsistemi.econ.db.table.ClientiIndirizzi.PROVINCIA));
			String descr = i.getAsString(com.ncfsistemi.econ.db.table.ClientiIndirizzi.DESCRIZIONE);
			// da un indirizzo come Cantieri::daIndirizzo: descrizione dell'indirizzo, altrimenti "cliente — città"
			String citta = i.getAsString(com.ncfsistemi.econ.db.table.ClientiIndirizzi.CITTA);
			if (idIndirizzo != 0 && getTesto(R.id.editText_cantiere).equals(nomeProposto)) {
				String n = descr != null && !descr.isEmpty() ? descr
						: nomeProposto + (citta == null || citta.isEmpty() ? "" : " — " + citta);
				nomeProposto = n.length() > 100 ? n.substring(0, 100) : n;
				setText(R.id.editText_cantiere, nomeProposto);
			}
			return;
		}
		setText(R.id.editText_indirizzo, cliente.getAsString(Anagrafica.INDIRIZZO));
		setText(R.id.editText_civico, "");
		setText(R.id.editText_cap, cliente.getAsString(Anagrafica.CAP));
		setText(R.id.editText_citta, cliente.getAsString(Anagrafica.CITTA));
		setText(R.id.editText_provincia, cliente.getAsString(Anagrafica.PROVINCIA));
	}

	public void nuovoCliente(View v) {
		System.out.println("Econ: CantieriDettaglioModActivity nuovoCliente");
		Intent intent = new Intent(this, ClientiDettaglioModActivity.class);
		apriFinestraInserimento(intent, 1, new Anagrafica());

	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		System.out.println("Econ: CantieriDettaglioModActivity onItemClick ENTER");
		// TODO Auto-generated method stub
		ContentValues val = ((EconAutoCompleteContentValue) arg0.getItemAtPosition(position)).getContentValue();
		codicecliente = val.getAsInteger(Anagrafica.ID_ANAGRAFICA);
		findViewById(R.id.editText_cantiere).requestFocus();
		idIndirizzoCliente = 0; // altro cliente: niente collegamento all'indirizzo di prima
		impostaIndirizzoCliente(val, 0);

		edit_cliente.setTextColor(Color.BLACK);
		System.out.println("Econ: CantieriDettaglioModActivity onItemClick EXIT");
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		codicecliente = 0;
		edit_cliente.setTextColor(Color.GRAY);

	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getMessaggioConfermaSalvataggio() {
		System.out.println("Econ: CantieriDettaglioModActivity getMessaggioConfermaSalvataggio");
		// TODO Auto-generated method stub
		String idLineaNuova = getTesto(R.id.econSpinner_linea);
		if (idLineaNuova.equals("")) {
			idLineaNuova = "0";
		}
		if (getModalita() == MODIFICA && !idLineaNuova.equals(idLineaPrec)) {
			return getString(R.string.messaggio_variazione_linea);
		}
		return super.getMessaggioConfermaSalvataggio();
	}


    @Override
    protected void mostraDialogSalvataggio() {
		System.out.println("Econ: CantieriDettaglioModActivity mostraDialogSalvataggio ENTER");
        String idLineaNuova = getTesto(R.id.econSpinner_linea);
        if (idLineaNuova.equals("")) {
            idLineaNuova = "0";
        }
        if (getModalita()==MODIFICA && !idLineaNuova.equals(idLineaPrec)){
            String[] items = new String[2];
            items[0] = getString(R.string.cambia_linea_locali);
            items[1] = getString(R.string.cambia_linea_solo_sul_cantiere);
            Utility.mostraSelezioneDialog(getString(R.string.attenzione),items,this,new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (i==0){
                        cambiaLineaLocali = true;
                    }
                    mostraDialogSalvataggioSuper();
                }
            });

        }
        else{
            mostraDialogSalvataggioSuper();
        }
		System.out.println("Econ: CantieriDettaglioModActivity mostraDialogSalvataggio EXIT");
    }


    private void mostraDialogSalvataggioSuper(){
        super.mostraDialogSalvataggio();
    }
	@Override
	protected String moduloFunzionalita() {
		return com.ncfsistemi.econ.utils.FunzionalitaApp.CANTIERI;
	}

}
