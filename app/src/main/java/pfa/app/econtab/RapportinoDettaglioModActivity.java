package pfa.app.econtab;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Manodopera;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.Rapportini;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.db.table.Utenti;
import pfa.app.econtab.utils.EConTabAutoCompleteContentValue;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabCalendario;
import pfa.app.econtab.views.EConTabSpinner;
import pfa.app.econtab.views.PopupDettaglioRapportino;

public class RapportinoDettaglioModActivity extends EConTabDettaglioActivity implements TextWatcher, AdapterView.OnItemClickListener, View.OnLongClickListener, View.OnClickListener {

    private int codicecliente;
    private ArrayAdapter<Object> adapter = null;
    private EConTabSpinner spinner_ordini = null;
    private EConTabSpinner spinner_operatori = null;

    private AutoCompleteTextView edit_cliente = null;
    private EConTabCalendario data = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("EConTab: RapportinoDettaglioModActivity onCreate ENTER");
        setContentView(R.layout.activity_rapportino_dettaglio_mod);
        edit_cliente = (AutoCompleteTextView) findViewById(R.id.editText_cliente);
        data = (EConTabCalendario) findViewById(R.id.edit_data);

        spinner_ordini = (EConTabSpinner) findViewById(R.id.spinner_ordini);
        spinner_operatori = (EConTabSpinner) findViewById(R.id.spinner_operatori);
        super.onCreate(savedInstanceState);

        if (Sessione.isLicenzaBusiness(this)){
            findViewById(R.id.textView_operatore).setVisibility(View.VISIBLE);
            if (getModalita() == INSERIMENTO) {
                spinner_operatori.setValue(""+Sessione.getIdOperatore(this));
            }
            _caricaOperatori();
            spinner_operatori.setVisibility(View.VISIBLE);
        }
        else{
            findViewById(R.id.textView_operatore).setVisibility(View.GONE);
            spinner_operatori.setVisibility(View.GONE);
        }

        if (getModalita() == INSERIMENTO) {
            findViewById(R.id.linear_lista_dett).setVisibility(View.GONE);
            DbInterno dbcl = new DbInterno(this);

            //prendo l'ultimo rapportino inserito per l'operatore e imposto lo stesso ordine
            Rapportini tabRapp = new Rapportini();
            ContentValues lastRapp = tabRapp.getUltimoRapportinoOperatore(dbcl, Sessione.getIdOperatore(this));
            if (lastRapp != null) {
                int idOrdine = lastRapp.getAsInteger(Rapportini.ID_ORDINE);

                Preventivi tabPrev = new Preventivi();
                ContentValues valCli = tabPrev.getClientePreventivo(dbcl, idOrdine);
                if (valCli != null) {
                    codicecliente = valCli.getAsInteger(Anagrafica.ID_ANAGRAFICA);
                    edit_cliente.setText(valCli.getAsString(Anagrafica.RAGIONE_SOCIALE));
                    spinner_ordini.setValue("" + idOrdine);
                    _caricaOrdiniCliente();
                }
            } else {
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
                    _caricaOrdiniCliente();
                }
            }


        }

        // imposto i listener solo in inserimento e dopo aver chiamato il primo settext
        edit_cliente.setOnItemClickListener(this);
        edit_cliente.addTextChangedListener(this);
        System.out.println("EConTab: RapportinoDettaglioModActivity onCreate EXIT");
    }

    private void _caricaOperatori() {
        System.out.println("EConTab: RapportinoDettaglioModActivity _caricaOperatori ENTER");
        ArrayList<Object> valoriSpinner = new ArrayList<Object>();
        ContentValues valOp = new ContentValues();
        valOp.put(EConTabSpinner.VALORE, "" + Sessione.getIdOperatore(this));

        valoriSpinner.add(valOp);

        DbInterno db = new DbInterno(this);
        ArrayList<Object> utenti = db.eseguiSelect(new Utenti(),new ContentValues(),new String[]{Utenti.NOME,Utenti.COGNOME},true);

        for (int i = 0; i < utenti.size(); i++) {
            ContentValues utente = (ContentValues) utenti.get(i);

            if (utente.getAsInteger(Utenti.ID_UTENTE)!=Sessione.getIdOperatore(this)){
                ContentValues valCurr = new ContentValues();
                valCurr.put(EConTabSpinner.VALORE, "" + utente.getAsInteger(Utenti.ID_UTENTE));
                String nomeOperatore = utente.getAsString(Utenti.NOME) + " " + utente.getAsString(Utenti.COGNOME);
                valCurr.put(EConTabSpinner.DESCRIZIONE, nomeOperatore);

                valoriSpinner.add(valCurr);
            }
            else{
                ((ContentValues)valoriSpinner.get(0)).put(EConTabSpinner.DESCRIZIONE,utente.getAsString(Utenti.NOME) + " " + utente.getAsString(Utenti.COGNOME));

            }

        }

        spinner_operatori.setValoriSpinnerLibero(valoriSpinner);

        db.close();
        System.out.println("EConTab: RapportinoDettaglioModActivity _caricaOperatori EXIT");
    }


    @Override
    protected void onResume() {
        System.out.println("EConTab: RapportinoDettaglioModActivity onResume ENTER");
        super.onResume();
        adapter = Utility.getArrayAdapterTabella(this, "Select * from " + Anagrafica.NOME_TABELLA, Anagrafica.RAGIONE_SOCIALE);
        edit_cliente.setAdapter(adapter);
        System.out.println("EConTab: RapportinoDettaglioModActivity onResume EXIT");
    }

    @Override
    protected void inizializzaModifica() {
        System.out.println("EConTab: RapportinoDettaglioModActivity inizializzaModifica ENTER");
        // TODO Auto-generated method stub
        super.inizializzaModifica();
        int id = getIDModifica();
        DbInterno db = new DbInterno(this);
        ContentValues where = new ContentValues();
        where.put(Rapportini.ID_RAPPORTINO, id);
        ContentValues val = db.getRecord(new Rapportini(), where);

        if (val != null) {
            data.setValue("" + val.getAsLong(Rapportini.DATA_RAPPORTINO));
            setText(R.id.editText_note, val.getAsString(Rapportini.NOTE));
            int idOrdine = val.getAsInteger(Rapportini.ID_ORDINE);
            if (Sessione.isLicenzaBusiness(this)){
                int idOperatoreRapp = val.getAsInteger(Rapportini.ID_OPERATORE);
                spinner_operatori.setValue(""+idOperatoreRapp);
                _caricaOperatori();
            }

            Preventivi tabPrev = new Preventivi();
            ContentValues valCli = tabPrev.getClientePreventivo(db, idOrdine);
            if (valCli != null) {
                codicecliente = valCli.getAsInteger(Anagrafica.ID_ANAGRAFICA);
                edit_cliente.setText(valCli.getAsString(Anagrafica.RAGIONE_SOCIALE));
                spinner_ordini.setValue("" + idOrdine);
                _caricaOrdiniCliente();
            }

        }
        db.close();

        caricaDettaglio();

        System.out.println("EConTab: RapportinoDettaglioModActivity inizializzaModifica EXIT");
    }

    @Override
    protected String eseguiInserimento(DbInterno db) {
        System.out.println("EConTab: RapportinoDettaglioModActivity eseguiInserimento ENTER");
        // TODO Auto-generated method stub
        if (getTesto(R.id.spinner_ordini).equals("")) {
            ((EConTabSpinner) findViewById(R.id.spinner_ordini)).setError(getString(R.string.errore_selezione_ordine));
            return getString(R.string.errore_selezione_ordine);
        }
        Rapportini tabella = new Rapportini();
        ContentValues val = tabella.getValoriLogInserimento(db);
        val.put(Rapportini.DATA_RAPPORTINO, Long.parseLong(data.getValue()));
        val.put(Rapportini.ID_ORDINE, getTesto(R.id.spinner_ordini));
        val.put(Rapportini.NOTE, getTesto(R.id.editText_note));
        if (Sessione.isLicenzaBusiness(this)){
            val.put(Rapportini.ID_OPERATORE, getTesto(R.id.spinner_operatori));
        }
        else{
            val.put(Rapportini.ID_OPERATORE, Sessione.getIdOperatore(this));
        }



        tabella.inserisciRecord(db, val);
        getIntent().putExtra("ID",val.getAsInteger(Rapportini.ID_RAPPORTINO));
        setChiudiAlSalvataggio(false);
        System.out.println("EConTab: RapportinoDettaglioModActivity eseguiInserimento EXIT");
        return super.eseguiInserimento(db);
    }

    @Override
    protected String eseguiAggiornamento(DbInterno db) {
        System.out.println("EConTab: RapportinoDettaglioModActivity eseguiAggiornamento ENTER");
        // TODO Auto-generated method stub
        if (getTesto(R.id.spinner_ordini).equals("")) {
            ((EConTabSpinner) findViewById(R.id.spinner_ordini)).setError(getString(R.string.errore_selezione_ordine));
            return getString(R.string.errore_selezione_ordine);
        }

        Rapportini tabella = new Rapportini();
        ContentValues val = tabella.getValoriLogModifica(db);
        val.put(Rapportini.DATA_RAPPORTINO, Long.parseLong(data.getValue()));
        val.put(Rapportini.ID_ORDINE, getTesto(R.id.spinner_ordini));
        val.put(Rapportini.NOTE, getTesto(R.id.editText_note));
        if (Sessione.isLicenzaBusiness(this)){
            val.put(Rapportini.ID_OPERATORE, getTesto(R.id.spinner_operatori));
        }
        else{
            val.put(Rapportini.ID_OPERATORE, Sessione.getIdOperatore(this));
        }


        ContentValues where = new ContentValues();
        where.put(Rapportini.ID_RAPPORTINO, getIDModifica());

        tabella.aggiornaRecord(db, val, where);
        System.out.println("EConTab: RapportinoDettaglioModActivity eseguiAggiornamento EXIT");
        return super.eseguiAggiornamento(db);
    }


    @Override
    protected void aggiornaDopoSalvataggio() {
        System.out.println("EConTab: RapportinoDettaglioModActivity aggiornaDopoSalvataggio ENTER");
        super.aggiornaDopoSalvataggio();
        setChiudiAlSalvataggio(true);
        Toast.makeText(this,getString(R.string.messaggio_salvataggio_eseguito),Toast.LENGTH_SHORT).show();
        setModalita(MODIFICA);
        findViewById(R.id.linear_lista_dett).setVisibility(View.VISIBLE);
        caricaDettaglio();
        System.out.println("EConTab: RapportinoDettaglioModActivity aggiornaDopoSalvataggio EXIT");
    }



    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        codicecliente = 0;
        edit_cliente.setTextColor(Color.GRAY);
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        System.out.println("EConTab: RapportinoDettaglioModActivity onItemClick ENTER");
        ContentValues val = ((EConTabAutoCompleteContentValue) adapterView.getItemAtPosition(position)).getContentValue();
        codicecliente = val.getAsInteger(Anagrafica.ID_ANAGRAFICA);
        edit_cliente.setTextColor(Color.BLACK);
        _caricaOrdiniCliente();
        System.out.println("EConTab: RapportinoDettaglioModActivity onItemClick EXIT");
    }

    private void _caricaOrdiniCliente() {
        System.out.println("EConTab: RapportinoDettaglioModActivity _caricaOrdiniCliente ENTER");
        DbInterno db = new DbInterno(this);
        Preventivi tabPrev = new Preventivi();
        ArrayList<Object> ordini = tabPrev.getOrdiniCliente(db, codicecliente);
        ArrayList<Object> valoriSpinner = new ArrayList<Object>();
        for (int i = 0; i < ordini.size(); i++) {
            ContentValues ordine = (ContentValues) ordini.get(i);

            ContentValues valCurr = new ContentValues();
            valCurr.put(EConTabSpinner.VALORE, "" + ordine.getAsInteger(Preventivi.ID_PREVENTIVO));
            String cantiere = ordine.getAsString("nome_cantiere");
            if (cantiere.trim().length() > 0) {
                cantiere = " (" + cantiere + ")";
            }
            String titoloOrdine = ordine.getAsString(Preventivi.TITOLO);
            valCurr.put(EConTabSpinner.DESCRIZIONE, "N. " + ordine.getAsInteger(Preventivi.NUMERO) + " del " + Utility.numberToDataShort(ordine.getAsLong(Preventivi.DATA)) + " - " + titoloOrdine +  cantiere);
            if (i == 0 && spinner_ordini.getValue().equals("")) {
                spinner_ordini.setValue("" + ordine.getAsInteger(Preventivi.ID_PREVENTIVO));
            }

            valoriSpinner.add(valCurr);
        }

        spinner_ordini.setValoriSpinnerLibero(valoriSpinner);

        db.close();
        System.out.println("EConTab: RapportinoDettaglioModActivity _caricaOrdiniCliente EXIT");
    }

    public void nuovoDettaglio(View v) {
        System.out.println("EConTab: RapportinoDettaglioModActivity nuovoDettaglio ENTER");
        PopupDettaglioRapportino popDett = new PopupDettaglioRapportino(this, getIDModifica(),0);
        popDett.apriPopup();
        System.out.println("EConTab: RapportinoDettaglioModActivity nuovoDettaglio EXIT");
    }

    public void caricaDettaglio() {
        System.out.println("EConTab: RapportinoDettaglioModActivity caricaDettaglio ENTER");
        LinearLayout listaDett = (LinearLayout)findViewById(R.id.linear_lista_dett);

        listaDett.removeViews(1,listaDett.getChildCount()-1);

        DbInterno db = new DbInterno(this);
        RapportiniDettaglio tabDett = new RapportiniDettaglio();

        Join j0 = new Join(RapportiniDettaglio.NOME_TABELLA, Manodopera.NOME_TABELLA);
        j0.addCampiDiJoin(RapportiniDettaglio.ID_MANODOPERA,Manodopera.ID_MANODOPERA);

        String SQLDETT = "Select "+RapportiniDettaglio.NOME_TABELLA+".*,"+Manodopera.NOME+" from "+RapportiniDettaglio.NOME_TABELLA+j0.getSQLJoin()+" where "+RapportiniDettaglio.ID_RAPPORTINO+"="+getIDModifica();

        ArrayList<Object> dettaglio = db.eseguiSelect(SQLDETT,null);

        db.close();

        for (int i=0;i<dettaglio.size();i++){
            ContentValues curr = (ContentValues)dettaglio.get(i);
            View dett = View.inflate(this, R.layout.list_item_rapportino_dett, null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);



            ((TextView)dett.findViewById(R.id.tipo_manodopera)).setText(curr.getAsString(Manodopera.NOME));
            ((TextView)dett.findViewById(R.id.ore)).setText(getString(R.string.ore)+": "+Utility.formatNumero(curr.getAsDouble(RapportiniDettaglio.ORE)));
            ((TextView)dett.findViewById(R.id.nota)).setText(curr.getAsString(RapportiniDettaglio.NOTE));

            dett.setTag(curr.getAsInteger(RapportiniDettaglio.ID_RAPPORTINO_DETTAGLIO));
            dett.setOnClickListener(this);
            dett.setOnLongClickListener(this);
            listaDett.addView(dett, lp);
        }
        System.out.println("EConTab: RapportinoDettaglioModActivity caricaDettaglio EXIT");
    }

    @Override
    public boolean onLongClick(final View view) {
        System.out.println("EConTab: RapportinoDettaglioModActivity onLongClick ENTER");
        String[] items = new String[2];
        items[0] = getString(R.string.modifica);
        items[1] = getString(R.string.elimina);
        Utility.mostraSelezioneDialog(getString(R.string.dettaglio_rapportino), items, this, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (which == 0) {
                    apriDettaglio(view);
                }
                if (which == 1) {
                    eliminaDettaglio(view);
                }
            }
        });
        System.out.println("EConTab: RapportinoDettaglioModActivity onLongClick EXIT");
        return true;
    }

    private void eliminaDettaglio(final View view) {
        System.out.println("EConTab: RapportinoDettaglioModActivity eliminaDettaglio ENTER");
        Utility.mostraConfermaCancellazioneDialog(this, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    int idDett = (Integer)view.getTag();

                    DbInterno db = new DbInterno(RapportinoDettaglioModActivity.this);
                    RapportiniDettaglio tabDett = new RapportiniDettaglio();
                    ContentValues valDel = new ContentValues();
                    valDel.put(RapportiniDettaglio.ID_RAPPORTINO_DETTAGLIO, idDett);
                    tabDett.cancellaRecord(db, valDel);
                    db.close();
                     caricaDettaglio();

                }
            }
        });
        System.out.println("EConTab: RapportinoDettaglioModActivity eliminaDettaglio EXIT");
    }

    private void apriDettaglio(View view) {
        System.out.println("EConTab: RapportinoDettaglioModActivity apriDettaglio ENTER");
        PopupDettaglioRapportino popDett = new PopupDettaglioRapportino(this, getIDModifica(),(Integer)view.getTag());

        popDett.apriPopup();
        System.out.println("EConTab: RapportinoDettaglioModActivity apriDettaglio EXIT");
    }

    @Override
    public void onClick(View view) {
apriDettaglio(view);
    }
}
