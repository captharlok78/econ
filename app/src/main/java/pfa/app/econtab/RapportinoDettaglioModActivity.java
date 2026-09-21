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

    private AutoCompleteTextView edit_cliente = null;
    private EConTabCalendario data = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("EConTab: RapportinoDettaglioModActivity onCreate ENTER");
        setContentView(R.layout.activity_rapportino_dettaglio_mod);
        edit_cliente = (AutoCompleteTextView) findViewById(R.id.editText_cliente);
        data = (EConTabCalendario) findViewById(R.id.edit_data);

        spinner_ordini = (EConTabSpinner) findViewById(R.id.spinner_ordini);
        super.onCreate(savedInstanceState);

        // Operatore = utente loggato (etichetta fissa); in modifica resta quello del rapportino (vedi inizializzaModifica)
        if (getModalita() == INSERIMENTO) {
            impostaOperatore(Sessione.getIdOperatore(this));
        }

        if (getModalita() == INSERIMENTO) {
            findViewById(R.id.linear_lista_dett).setVisibility(View.GONE);
            DbInterno dbcl = new DbInterno(this);

            //prendo l'ultimo rapportino inserito per l'operatore e imposto lo stesso ordine
            Rapportini tabRapp = new Rapportini();
            Preventivi tabPrev0 = new Preventivi();
            ContentValues lastRapp = tabRapp.getUltimoRapportinoOperatore(dbcl, Sessione.getIdOperatore(this));
            boolean preselezionato = false;
            if (lastRapp != null) {
                int idOrdine = lastRapp.getAsInteger(Rapportini.ID_ORDINE);

                // stesso ordine dell'ultimo rapportino, ma solo se e' ancora aperto
                if (tabPrev0.isOrdineAperto(dbcl, idOrdine)) {
                    ContentValues valCli = tabPrev0.getClientePreventivo(dbcl, idOrdine);
                    if (valCli != null) {
                        codicecliente = valCli.getAsInteger(Anagrafica.ID_ANAGRAFICA);
                        edit_cliente.setText(valCli.getAsString(Anagrafica.RAGIONE_SOCIALE));
                        spinner_ordini.setValue("" + idOrdine);
                        _caricaOrdiniCliente();
                        preselezionato = true;
                    }
                }
            }
            if (!preselezionato) {
                // un solo cliente con ordini aperti: lo propongo gia' selezionato
                ArrayList<Object> clientiAperti = dbcl.eseguiSelect(Preventivi.getSqlClientiConOrdiniAperti(), null);
                if (clientiAperti.size() == 1) {
                    ContentValues valCl = (ContentValues) clientiAperti.get(0);
                    codicecliente = valCl.getAsInteger(Anagrafica.ID_ANAGRAFICA);
                    setText(R.id.editText_cliente, valCl.getAsString(Anagrafica.RAGIONE_SOCIALE));
                    edit_cliente.setTextColor(Color.BLACK);
                    _caricaOrdiniCliente();
                }
            }
            dbcl.close();
        }

        // imposto i listener solo in inserimento e dopo aver chiamato il primo settext
        edit_cliente.setOnItemClickListener(this);
        edit_cliente.addTextChangedListener(this);
        System.out.println("EConTab: RapportinoDettaglioModActivity onCreate EXIT");
    }

    /** Mostra il nome dell'operatore nell'etichetta fissa (l'utente loggato, o l'autore del rapportino in modifica). */
    private void impostaOperatore(int idOperatore) {
        String nome = null;
        if (idOperatore == Sessione.getIdOperatore(this)) {
            nome = Sessione.getNomeOperatore();
        }
        if (nome == null || nome.trim().isEmpty()) {
            DbInterno db = new DbInterno(this);
            ContentValues where = new ContentValues();
            where.put(Utenti.ID_UTENTE, idOperatore);
            ContentValues ut = db.getRecord(new Utenti(), where);
            db.close();
            nome = ut != null ? ut.getAsString(Utenti.NOME) + " " + ut.getAsString(Utenti.COGNOME) : "";
        }
        ((TextView) findViewById(R.id.text_operatore)).setText(nome.trim());
    }

    @Override
    protected void onResume() {
        System.out.println("EConTab: RapportinoDettaglioModActivity onResume ENTER");
        super.onResume();
        // solo i clienti della ditta con ordini aperti
        adapter = Utility.getArrayAdapterTabella(this, Preventivi.getSqlClientiConOrdiniAperti(), Anagrafica.RAGIONE_SOCIALE);
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
            impostaOperatore(val.getAsInteger(Rapportini.ID_OPERATORE));

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
        val.put(Rapportini.ID_OPERATORE, Sessione.getIdOperatore(this));

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
        // l'operatore non si modifica: resta quello che ha aperto il rapportino

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
        int ordineCorrente = 0;
        if (getModalita() != INSERIMENTO && !spinner_ordini.getValue().equals("")) {
            ordineCorrente = Integer.parseInt(spinner_ordini.getValue());
        }
        ArrayList<Object> ordini = tabPrev.getOrdiniCliente(db, codicecliente, true, ordineCorrente);
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
            ((TextView)dett.findViewById(R.id.ore)).setText(getString(R.string.ore)+": "+Utility.formatNumero(curr.getAsDouble(RapportiniDettaglio.ORE))
                    + (curr.getAsString(RapportiniDettaglio.UNITA_MISURA) != null && !curr.getAsString(RapportiniDettaglio.UNITA_MISURA).trim().isEmpty()
                        ? " " + curr.getAsString(RapportiniDettaglio.UNITA_MISURA).trim() : ""));
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
