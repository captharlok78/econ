package pfa.app.econtab;

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

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Aree;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Placche;
import pfa.app.econtab.db.table.Unita;
import pfa.app.econtab.utils.EConTabAutoCompleteContentValue;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabSpinner;

public class CantieriDettaglioModActivity extends EConTabDettaglioActivity implements AdapterView.OnItemClickListener, TextWatcher {

	private int codicecliente;
	private ArrayAdapter<Object> adapter = null;
	private EConTabSpinner spinner_linea = null;
	private EConTabSpinner spinner_placca = null;
	private AutoCompleteTextView edit_cliente = null;
	private String idLineaPrec = "0";
    private boolean cambiaLineaLocali = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: CantieriDettaglioModActivity onCreate ENTER");
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

		spinner_linea = (EConTabSpinner) findViewById(R.id.econtabSpinner_linea);
		spinner_placca = (EConTabSpinner) findViewById(R.id.econtabSpinner_placca);

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

			if (codicecliente != 0) {
				DbInterno db = new DbInterno(this);
				ContentValues where = new ContentValues();
				where.put(Anagrafica.ID_ANAGRAFICA, codicecliente);
				ContentValues val = db.getRecord(new Anagrafica(), where);
				db.close();
				setText(R.id.editText_cliente, val.getAsString(Anagrafica.RAGIONE_SOCIALE));
				edit_cliente.setTextColor(Color.BLACK);
				setText(R.id.editText_indirizzo, val.getAsString(Anagrafica.INDIRIZZO));
				setText(R.id.editText_cap, val.getAsString(Anagrafica.CAP));
				setText(R.id.editText_citta, val.getAsString(Anagrafica.CITTA));
				setText(R.id.editText_provincia, val.getAsString(Anagrafica.PROVINCIA));

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
		System.out.println("EConTab: CantieriDettaglioModActivity onCreate EXIT");
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("EConTab: CantieriDettaglioModActivity inizializzaModifica ENTER");
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
			setText(R.id.editText_cap, val.getAsString(Cantieri.CAP));
			setText(R.id.editText_citta, val.getAsString(Cantieri.CITTA));
			setText(R.id.editText_provincia, val.getAsString(Cantieri.PROVINCIA));
			setText(R.id.econtabSpinner_linea, val.getAsString(Cantieri.ID_LINEA));
			idLineaPrec = val.getAsString(Cantieri.ID_LINEA);
			setText(R.id.econtabSpinner_placca, val.getAsString(Cantieri.ID_PLACCA));
			setText(R.id.editText_note, val.getAsString(Anagrafica.NOTE));
		}
		System.out.println("EConTab: CantieriDettaglioModActivity inizializzaModifica EXIT");
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("EConTab: CantieriDettaglioModActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		AutoCompleteTextView edit_cliente = (AutoCompleteTextView) findViewById(R.id.editText_cliente);

		if (codicecliente == 0) {
			edit_cliente.setError(getString(R.string.errore_selezione_cliente));
			return getString(R.string.errore_selezione_cliente);
		}
		Cantieri cant = new Cantieri();
		ContentValues val = cant.getValoriLogInserimento(db);
		val.put(Cantieri.ID_ANAGRAFICA, codicecliente);
		val.put(Cantieri.NOME, getTesto(R.id.editText_cantiere));
		val.put(Cantieri.INDIRIZZO, getTesto(R.id.editText_indirizzo));
		val.put(Cantieri.CITTA, getTesto(R.id.editText_citta));
		val.put(Cantieri.CAP, getTesto(R.id.editText_cap));
		val.put(Cantieri.PROVINCIA, getTesto(R.id.editText_provincia));
		String idLinea = getTesto(R.id.econtabSpinner_linea);
		if (idLinea.equals("")) {
			idLinea = "0";
		}
		val.put(Cantieri.ID_LINEA, idLinea);
		String idPlacca = getTesto(R.id.econtabSpinner_placca);
		if (idPlacca.equals("")) {
			idPlacca = "0";
		}
		val.put(Cantieri.ID_PLACCA, idPlacca);
		val.put(Cantieri.NOTE, getTesto(R.id.editText_note));

		cant.inserisciNuovoCantiere(val, db, this);
		System.out.println("EConTab: CantieriDettaglioModActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("EConTab: CantieriDettaglioModActivity eseguiAggiornamento ENTER");
        //prima cambio la linea su tutti i locali
        String idLineaNuova = getTesto(R.id.econtabSpinner_linea);
        if (idLineaNuova.equals("")) {
            idLineaNuova = "0";
        }
        String idPlaccaNuova = getTesto(R.id.econtabSpinner_placca);
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
		val.put(Cantieri.CITTA, getTesto(R.id.editText_citta));
		val.put(Cantieri.CAP, getTesto(R.id.editText_cap));
		val.put(Cantieri.PROVINCIA, getTesto(R.id.editText_provincia));
		String idLinea = getTesto(R.id.econtabSpinner_linea);
		if (idLinea.equals("")) {
			idLinea = "0";
		}
		val.put(Cantieri.ID_LINEA, idLinea);
		String idPlacca = getTesto(R.id.econtabSpinner_placca);
		if (idPlacca.equals("")) {
			idPlacca = "0";
		}
		val.put(Cantieri.ID_PLACCA, idPlacca);
		val.put(Cantieri.NOTE, getTesto(R.id.editText_note));

		ContentValues where = new ContentValues();
		where.put(Cantieri.ID_CANTIERE, getIDModifica());

		cant.aggiornaRecord(db, val, where);
		System.out.println("EConTab: CantieriDettaglioModActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

	@Override
	protected void onResume() {
		System.out.println("EConTab: CantieriDettaglioModActivity onResume");
		// TODO Auto-generated method stub
		super.onResume();
		if (getModalita() == INSERIMENTO) {
			adapter = Utility.getArrayAdapterTabella(this, "Select * from " + Anagrafica.NOME_TABELLA, Anagrafica.RAGIONE_SOCIALE);
			edit_cliente.setAdapter(adapter);
		}
	}

	public void nuovoCliente(View v) {
		System.out.println("EConTab: CantieriDettaglioModActivity nuovoCliente");
		Intent intent = new Intent(this, ClientiDettaglioModActivity.class);
		apriFinestraInserimento(intent, 1, new Anagrafica());

	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		System.out.println("EConTab: CantieriDettaglioModActivity onItemClick ENTER");
		// TODO Auto-generated method stub
		ContentValues val = ((EConTabAutoCompleteContentValue) arg0.getItemAtPosition(position)).getContentValue();
		codicecliente = val.getAsInteger(Anagrafica.ID_ANAGRAFICA);
		findViewById(R.id.editText_cantiere).requestFocus();
		setText(R.id.editText_indirizzo, val.getAsString(Anagrafica.INDIRIZZO));
		setText(R.id.editText_cap, val.getAsString(Anagrafica.CAP));
		setText(R.id.editText_citta, val.getAsString(Anagrafica.CITTA));
		setText(R.id.editText_provincia, val.getAsString(Anagrafica.PROVINCIA));

		edit_cliente.setTextColor(Color.BLACK);
		System.out.println("EConTab: CantieriDettaglioModActivity onItemClick EXIT");
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
		System.out.println("EConTab: CantieriDettaglioModActivity getMessaggioConfermaSalvataggio");
		// TODO Auto-generated method stub
		String idLineaNuova = getTesto(R.id.econtabSpinner_linea);
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
		System.out.println("EConTab: CantieriDettaglioModActivity mostraDialogSalvataggio ENTER");
        String idLineaNuova = getTesto(R.id.econtabSpinner_linea);
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
		System.out.println("EConTab: CantieriDettaglioModActivity mostraDialogSalvataggio EXIT");
    }


    private void mostraDialogSalvataggioSuper(){
        super.mostraDialogSalvataggio();
    }
}
