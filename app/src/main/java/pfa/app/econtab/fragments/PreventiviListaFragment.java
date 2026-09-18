package pfa.app.econtab.fragments;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

import pfa.app.econtab.CantiereSplitActivity;
import pfa.app.econtab.EConTabActivity;
import pfa.app.econtab.PreventiviDettaglioModActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.adapters.EConTabListViewAdapter;
import pfa.app.econtab.adapters.PreventiviAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.lista.EConTabListaStandardController;
import pfa.app.econtab.lista.EConTabListaStandardDefinition;
import pfa.app.econtab.lista.EConTabListaStandardFragment;
import pfa.app.econtab.lista.FiltriHelper;
import pfa.app.econtab.lista.QueryPagina;
import pfa.app.econtab.utils.EConTabAutoCompleteContentValue;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabSpinner;

/**
 * Modulo Preventivi sullo standard di ricerca/lista condiviso (vedi pfa.app.econtab.lista).
 * Le righe hanno un pulsante "Azioni" con logica di business per riga (cambia stato,
 * trasforma in ordine), troppo custom per il modello dichiarativo a colonne: usa il proprio
 * PreventiviAdapter (via creaAdapterPersonalizzato) e non mostra la testata ordinabile
 * (isTabellare()=false), pur restando sullo standard comune per form filtri/paginazione/
 * righe alternate.
 *
 * OrdiniListaFragment estende questa classe sovrascrivendo solo cio' che cambia fra
 * preventivo e ordine (tipo, opzioni stato, valore di default) - stessa tabella, query e
 * adapter, per non duplicare la stessa logica due volte come accadeva prima.
 */
public class PreventiviListaFragment extends EConTabListaStandardFragment {

    private int cantiere = 0;
    private int codiceClienteFiltro = 0;
    private EConTabSpinner spinnerFiltroAnno;
    private EConTabSpinner spinnerFiltroStato;
    private ArrayAdapter<Object> adapterFiltroCliente;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_preventivi_lista;
    }

    @Override
    protected EConTabListaStandardDefinition creaDefinizione() {
        if (getArguments() != null) {
            cantiere = getArguments().getInt(Cantieri.ID_CANTIERE);
        }
        return new PreventiviDefinition();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AutoCompleteTextView filtro = findViewById(R.id.editText_filtra);
        filtro.setOnItemClickListener((parent, v, position, id) -> {
            ContentValues val = ((EConTabAutoCompleteContentValue) parent.getItemAtPosition(position)).getContentValue();
            codiceClienteFiltro = val.getAsInteger(Anagrafica.ID_ANAGRAFICA);
        });
        filtro.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                codiceClienteFiltro = 0;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        spinnerFiltroAnno = findViewById(R.id.econtabSpinner_filtroAnno);
        spinnerFiltroStato = findViewById(R.id.econtabSpinner_filtroStato);
    }

    /** Tipo di documento gestito da questo modulo (sovrascritto da OrdiniListaFragment). */
    protected String getTipo() {
        return Preventivi.TIPO_PREVENTIVO;
    }

    /** Titolo mostrato nella testata standard (sovrascritto da OrdiniListaFragment). */
    protected String getTitoloModulo() {
        return "Preventivi";
    }

    /** Opzioni della spinner di stato, con l'eventuale valore "tutti" in testa (sovrascritto da OrdiniListaFragment). */
    protected ArrayList<Object> getOpzioniStato() {
        ArrayList<Object> stati = new Preventivi().getStatiPreventivo(getActivity());
        ContentValues valTutti = new ContentValues();
        valTutti.put("VAL", "");
        valTutti.put("DESC", "");
        stati.add(0, valTutti);

        ContentValues valApertiAccettati = new ContentValues();
        valApertiAccettati.put("VAL", "AO");
        valApertiAccettati.put("DESC", getString(R.string.stato_aperto_accettato));
        stati.add(1, valApertiAccettati);
        return stati;
    }

    /** Valore di default della spinner stato all'apertura del modulo (sovrascritto da OrdiniListaFragment). */
    protected String getStatoDefault() {
        return "AO";
    }

    @Override
    public void onResume() {
        super.onResume();

        DbInterno db = new DbInterno(getActivity());
        ArrayList<Object> valori = db.eseguiSelect("Select distinct " + Preventivi.ANNO + " from " + Preventivi.NOME_TABELLA
                + " order by " + Preventivi.ANNO + " desc", null);
        db.close();

        ArrayList<Object> anni = new ArrayList<>();
        if (valori.size() > 0) {
            for (Object o : valori) {
                ContentValues valCurr = (ContentValues) o;
                ContentValues annoCorrente = new ContentValues();
                annoCorrente.put(EConTabSpinner.VALORE, valCurr.getAsString(Preventivi.ANNO));
                annoCorrente.put(EConTabSpinner.DESCRIZIONE, valCurr.getAsString(Preventivi.ANNO));
                anni.add(annoCorrente);
                if (spinnerFiltroAnno.getValue().equals("")) {
                    spinnerFiltroAnno.setValue(valCurr.getAsString(Preventivi.ANNO));
                }
            }
        } else {
            String annoCorrenteTesto = "" + Calendar.getInstance().get(Calendar.YEAR);
            ContentValues annoCorrente = new ContentValues();
            annoCorrente.put(EConTabSpinner.VALORE, annoCorrenteTesto);
            annoCorrente.put(EConTabSpinner.DESCRIZIONE, annoCorrenteTesto);
            anni.add(annoCorrente);
            spinnerFiltroAnno.setValue(annoCorrenteTesto);
        }
        spinnerFiltroAnno.setValoriSpinnerLibero(anni);

        if (spinnerFiltroStato.getValue().equals("")) {
            spinnerFiltroStato.setValue(getStatoDefault());
        }
        spinnerFiltroStato.setValoriSpinnerLibero(getOpzioniStato());

        adapterFiltroCliente = Utility.getArrayAdapterTabella(getActivity(), "Select * from " + Anagrafica.NOME_TABELLA,
                Anagrafica.RAGIONE_SOCIALE);
        ((AutoCompleteTextView) findViewById(R.id.editText_filtra)).setAdapter(adapterFiltroCliente);
    }

    /** Compat: chiamato da PreventiviActivity.refresh() e da PreventiviAdapter dopo cambio stato/trasformazione. */
    public void ricerca() {
        ricaricaSeAttiva();
    }

    public void apriModifica(ContentValues val) {
        Intent intent = new Intent(getActivity(), PreventiviDettaglioModActivity.class);
        intent.putExtra("ID", val.getAsInteger(Preventivi.ID_PREVENTIVO));
        getEConTabActivity().apriFinestraModifica(intent, 2);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        ContentValues item = (ContentValues) getController().getElementoAllaPosizione(info.position);
        menu.setHeaderTitle(item.getAsString(Preventivi.NUMERO) + " - " + Utility.numberToData(item.getAsLong(Preventivi.DATA)));
        menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
        if (item.getAsString(Preventivi.STATO).equals(Preventivi.STATO_APERTO)) {
            menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.trasforma_in_ordine));
        }
        menu.add(Menu.NONE, 3, Menu.NONE, getString(R.string.elimina));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        ContentValues preventivo = (ContentValues) getController().getElementoAllaPosizione(info.position);
        if (item.getItemId() == 1) {
            apriModifica(preventivo);
        }
        if (item.getItemId() == 2) {
            trasformaInOrdine(preventivo);
        }
        if (item.getItemId() == 3) {
            getEConTabActivity().confermaCancellazione(new Preventivi(), preventivo, true);
        }
        return super.onContextItemSelected(item);
    }

    public void trasformaInOrdine(final ContentValues val) {
        Utility.mostraConfermaDialog(getString(R.string.attenzione), getString(R.string.messaggio_trasformazione_preventivi),
                getActivity(), getString(R.string.conferma), getString(R.string.annulla), (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        ArrayList<Object> elementiModificatiCantiere = _getElementiModificatiCantiere(val.getAsInteger(Preventivi.ID_PREVENTIVO));
                        if (elementiModificatiCantiere.size() > 0) {
                            StringBuilder messaggio = new StringBuilder("I seguenti elementi del cantiere verranno sostituiti se confermi la "
                                    + "trasformazione del preventivo in ordine. Se gli elementi appartenevano ad un ordine aperto, saranno "
                                    + "tolti dall'ordine e conteggiati nel nuovo ordine derivante dal preventivo trasformato:\n");
                            for (Object o : elementiModificatiCantiere) {
                                ContentValues valMod = (ContentValues) o;
                                messaggio.append(valMod.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO)).append(" - ")
                                        .append(valMod.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT)).append("\n");
                            }
                            Utility.mostraConfermaDialog(getString(R.string.attenzione), messaggio.toString(), getActivity(), "Ok",
                                    getString(R.string.annulla), (dialogInterface, i) -> {
                                        if (i == DialogInterface.BUTTON_POSITIVE) {
                                            _eseguiTrasformazione(val);
                                        }
                                    });
                        } else {
                            _eseguiTrasformazione(val);
                        }
                    }
                });
    }

    private void _eseguiTrasformazione(ContentValues val) {
        DbInterno db = new DbInterno(getActivity());
        try {
            db.getReadableDatabase().beginTransaction();
            new Preventivi().trasformaPreventivoInOrdine(db, val.getAsInteger(Preventivi.ID_PREVENTIVO));
            db.getReadableDatabase().setTransactionSuccessful();
        } catch (Exception e) {
            Toast.makeText(getActivity(), Log.getStackTraceString(e), Toast.LENGTH_LONG).show();
        } finally {
            db.getReadableDatabase().endTransaction();
            db.close();
            ricaricaSeAttiva();
        }
    }

    private ArrayList<Object> _getElementiModificatiCantiere(int idPreventivo) {
        DbInterno db = new DbInterno(getActivity());
        ArrayList<Object> modificati = db.eseguiSelect(
                "Select * from elementi_cantiere where id_preventivo=" + idPreventivo
                        + " and id_elemento_cant_origine is not null and id_elemento_cant_origine<>0 order by numero_identificativo", null);
        db.close();
        return modificati;
    }

    /** Query, adapter e azione di click riga specifiche del modulo Preventivi/Ordini. */
    private class PreventiviDefinition extends EConTabListaStandardDefinition {

        // Preventivi/Cantieri/Anagrafica condividono campi di audit e alcuni nomi (es.
        // Cantieri.NOME e' selezionato esplicitamente come colonna propria per evitare
        // ambiguita' col resto della select se in futuro servisse ordinare per nome cantiere).
        private static final String SELECT = Preventivi.NOME_TABELLA + ".*, "
                + Anagrafica.NOME_TABELLA + "." + Anagrafica.RAGIONE_SOCIALE + ", "
                + Cantieri.NOME_TABELLA + "." + Cantieri.NOME;

        @Override
        public QueryPagina costruisciQuery(String testoFiltroNonUsato, boolean ordinaPerRecenti) {
            String fromJoin = Preventivi.NOME_TABELLA
                    + " inner join " + Cantieri.NOME_TABELLA + " on " + Preventivi.NOME_TABELLA + "." + Preventivi.ID_CANTIERE
                    + "=" + Cantieri.NOME_TABELLA + "." + Cantieri.ID_CANTIERE
                    + " inner join " + Anagrafica.NOME_TABELLA + " on " + Cantieri.NOME_TABELLA + "." + Cantieri.ID_ANAGRAFICA
                    + "=" + Anagrafica.NOME_TABELLA + "." + Anagrafica.ID_ANAGRAFICA;

            String anno = spinnerFiltroAnno.getValue();
            if (anno.equals("")) {
                anno = "" + Calendar.getInstance().get(Calendar.YEAR);
            }

            String where = Preventivi.NOME_TABELLA + "." + Preventivi.ANNO + "=" + anno;
            String[] args = new String[0];

            if (codiceClienteFiltro != 0) {
                where += " and " + Anagrafica.NOME_TABELLA + "." + Anagrafica.ID_ANAGRAFICA + "=" + codiceClienteFiltro;
            } else {
                AutoCompleteTextView filtro = findViewById(R.id.editText_filtra);
                if (filtro.getText().length() > 0) {
                    QueryPagina like = FiltriHelper.likeMultiCampo(fromJoin, filtro.getText().toString(),
                            Anagrafica.NOME_TABELLA + "." + Anagrafica.RAGIONE_SOCIALE);
                    where += " and (" + like.whereSql + ")";
                    args = like.whereArgs;
                }
            }

            String stato = spinnerFiltroStato.getValue();
            if (!stato.equals("")) {
                if (stato.equals("AO")) {
                    where += " and " + Preventivi.STATO + " in ('" + Preventivi.STATO_APERTO + "','" + Preventivi.STATO_ACCETTATO + "')";
                } else {
                    where += " and " + Preventivi.STATO + "='" + stato + "'";
                }
            }

            where += " and " + Preventivi.TIPO + "='" + getTipo() + "'";
            where += " and " + Cantieri.NOME_TABELLA + "." + Cantieri.ID_DITTA + "=" + Sessione.getDittaSelezionata();
            if (cantiere != 0) {
                where += " and " + Preventivi.NOME_TABELLA + "." + Preventivi.ID_CANTIERE + "=" + cantiere;
            }

            return new QueryPagina(SELECT, fromJoin, where, args);
        }

        @Override
        public boolean isTabellare() {
            return false;
        }

        @Override
        public EConTabListViewAdapter creaAdapterPersonalizzato(android.content.Context context, ArrayList<Object> dati) {
            PreventiviAdapter adapter = new PreventiviAdapter(context, dati, R.layout.list_item_preventivo);
            adapter.setPreventiviFragment(PreventiviListaFragment.this);
            return adapter;
        }

        @Override
        public void onRigaClick(EConTabListaStandardController.Host host, ContentValues riga) {
            Intent intent = new Intent(host.getContext(), CantiereSplitActivity.class);
            intent.putExtra(Cantieri.ID_CANTIERE, riga.getAsInteger(Preventivi.ID_CANTIERE));
            intent.putExtra(Preventivi.ID_PREVENTIVO, riga.getAsInteger(Preventivi.ID_PREVENTIVO));
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            host.startActivity(intent);
        }

        @Override
        public String getColonnaOrdinamentoDefault() {
            return Preventivi.NUMERO;
        }

        @Override
        public boolean isOrdinamentoDefaultDiscendente() {
            return true;
        }

        @Override
        public String getOrdineRecenti() {
            return Preventivi.NOME_TABELLA + "." + Preventivi.DATA_MOD + " is null asc, "
                    + Preventivi.NOME_TABELLA + "." + Preventivi.DATA_MOD + " desc, "
                    + Preventivi.NOME_TABELLA + "." + Preventivi.DATA_INS + " desc";
        }

        @Override
        public String getTitolo() {
            return getTitoloModulo();
        }

        @Override
        public void onNuovoClick(EConTabListaStandardController.Host host) {
            Intent intent = new Intent(host.getContext(), PreventiviDettaglioModActivity.class);
            intent.putExtra(Preventivi.TIPO, getTipo());
            ((EConTabActivity) host.getContext()).apriFinestraInserimento(intent, 1, new Preventivi());
        }
    }
}
