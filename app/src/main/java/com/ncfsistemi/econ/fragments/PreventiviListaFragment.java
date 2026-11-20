package com.ncfsistemi.econ.fragments;

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

import com.ncfsistemi.econ.CantiereSplitActivity;
import com.ncfsistemi.econ.EconActivity;
import com.ncfsistemi.econ.PreventiviDettaglioModActivity;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.adapters.EconListViewAdapter;
import com.ncfsistemi.econ.adapters.PreventiviAdapter;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.ElementiCantiere;
import com.ncfsistemi.econ.db.table.Preventivi;
import com.ncfsistemi.econ.db.table.PreventiviDettaglio;
import com.ncfsistemi.econ.lista.EconListaStandardController;
import com.ncfsistemi.econ.lista.EconListaStandardDefinition;
import com.ncfsistemi.econ.lista.EconListaStandardFragment;
import com.ncfsistemi.econ.lista.FiltriHelper;
import com.ncfsistemi.econ.lista.QueryPagina;
import com.ncfsistemi.econ.utils.EconAutoCompleteContentValue;
import com.ncfsistemi.econ.utils.Sessione;
import com.ncfsistemi.econ.utils.Utility;
import com.ncfsistemi.econ.views.EconSpinner;

/**
 * Modulo Preventivi sullo standard di ricerca/lista condiviso (vedi com.ncfsistemi.econ.lista).
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
public class PreventiviListaFragment extends EconListaStandardFragment {

    private int cantiere = 0;
    private int codiceClienteFiltro = 0;
    private EconSpinner spinnerFiltroAnno;
    private EconSpinner spinnerFiltroStato;
    private ArrayAdapter<Object> adapterFiltroCliente;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_preventivi_lista;
    }

    @Override
    protected EconListaStandardDefinition creaDefinizione() {
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
            ContentValues val = ((EconAutoCompleteContentValue) parent.getItemAtPosition(position)).getContentValue();
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

        spinnerFiltroAnno = findViewById(R.id.econSpinner_filtroAnno);
        spinnerFiltroStato = findViewById(R.id.econSpinner_filtroStato);
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
                annoCorrente.put(EconSpinner.VALORE, valCurr.getAsString(Preventivi.ANNO));
                annoCorrente.put(EconSpinner.DESCRIZIONE, valCurr.getAsString(Preventivi.ANNO));
                anni.add(annoCorrente);
                if (spinnerFiltroAnno.getValue().equals("")) {
                    spinnerFiltroAnno.setValue(valCurr.getAsString(Preventivi.ANNO));
                }
            }
        } else {
            String annoCorrenteTesto = "" + Calendar.getInstance().get(Calendar.YEAR);
            ContentValues annoCorrente = new ContentValues();
            annoCorrente.put(EconSpinner.VALORE, annoCorrenteTesto);
            annoCorrente.put(EconSpinner.DESCRIZIONE, annoCorrenteTesto);
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
        getEconActivity().apriFinestraModifica(intent, 2);
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
            getEconActivity().confermaCancellazione(new Preventivi(), preventivo, true);
        }
        return super.onContextItemSelected(item);
    }

    public void trasformaInOrdine(final ContentValues val) {
        com.ncfsistemi.econ.utils.AzioniDocumento.trasformaInOrdine(getActivity(), val, this::ricaricaSeAttiva);
    }

    /** Query, adapter e azione di click riga specifiche del modulo Preventivi/Ordini. */
    private class PreventiviDefinition extends EconListaStandardDefinition {

        // Preventivi/Cantieri/Anagrafica condividono campi di audit e alcuni nomi (es.
        // Cantieri.NOME e' selezionato esplicitamente come colonna propria per evitare
        // ambiguita' col resto della select se in futuro servisse ordinare per nome cantiere).
        private static final String SELECT = Preventivi.NOME_TABELLA + ".*, "
                + Anagrafica.NOME_TABELLA + "." + Anagrafica.RAGIONE_SOCIALE + ", "
                + Cantieri.NOME_TABELLA + "." + Cantieri.NOME + ", "
                // totale imponibile del documento (qta x prezzo netto, x operatori per la manodopera), come getImportoRiga()
                + "(select round(coalesce(sum(d." + PreventiviDettaglio.QUANTITA + " * d." + PreventiviDettaglio.PREZZO
                + " * (case when d." + PreventiviDettaglio.TIPO + "='" + PreventiviDettaglio.MANOPERA + "' and d." + PreventiviDettaglio.NUM_OPERATORI
                + " > 0 then d." + PreventiviDettaglio.NUM_OPERATORI + " else 1 end)), 0), 2) from " + PreventiviDettaglio.NOME_TABELLA
                + " d where d." + PreventiviDettaglio.ID_PREVENTIVO + "=" + Preventivi.NOME_TABELLA + "." + Preventivi.ID_PREVENTIVO + ") as totale_documento";

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

        /** Tabella come i rapportini (STUDIO_PREVENTIVI_ORDINI.md §6.5): stato a icona, numero, data, cliente, cantiere, titolo, totale. */
        @Override
        public java.util.List<com.ncfsistemi.econ.lista.ColonnaLista> getColonne() {
            String prev = Preventivi.NOME_TABELLA + ".";
            return java.util.Arrays.asList(
                    com.ncfsistemi.econ.lista.ColonnaLista.icona(Preventivi.STATO, "Stato", 7, com.ncfsistemi.econ.utils.FaIcone.STATO,
                            riga -> coloreStato(riga.getAsString(Preventivi.STATO)), prev + Preventivi.STATO),
                    new com.ncfsistemi.econ.lista.ColonnaLista(Preventivi.NUMERO, "N.", 10, false,
                            riga -> riga.getAsString(Preventivi.NUMERO) + "/" + riga.getAsString(Preventivi.ANNO),
                            "(" + prev + Preventivi.ANNO + "*1000000+" + prev + Preventivi.NUMERO + ")"),
                    new com.ncfsistemi.econ.lista.ColonnaLista(Preventivi.DATA, "Data", 12, false,
                            riga -> riga.getAsLong(Preventivi.DATA) != null ? Utility.numberToDataShort(riga.getAsLong(Preventivi.DATA)) : "", prev + Preventivi.DATA),
                    new com.ncfsistemi.econ.lista.ColonnaLista(Anagrafica.RAGIONE_SOCIALE, "Cliente", 20, true, null,
                            Anagrafica.NOME_TABELLA + "." + Anagrafica.RAGIONE_SOCIALE),
                    new com.ncfsistemi.econ.lista.ColonnaLista(Cantieri.NOME, "Cantiere", 18, false, null, Cantieri.NOME_TABELLA + "." + Cantieri.NOME),
                    new com.ncfsistemi.econ.lista.ColonnaLista(Preventivi.TITOLO, "Descrizione", 20, false, null, prev + Preventivi.TITOLO),
                    com.ncfsistemi.econ.lista.ColonnaLista.importo("totale_documento", "Imponibile", 11, "totale_documento"));
        }

        @Override
        public AzioneRiga getAzioneRiga(EconListaStandardController.Host host) {
            return new AzioneRiga() {
                @Override
                public String glifo() {
                    return com.ncfsistemi.econ.utils.FaIcone.AZIONI;
                }

                @Override
                public String tooltip() {
                    return getString(R.string.opzioni);
                }

                @Override
                public void onClick(ContentValues riga) {
                    com.ncfsistemi.econ.utils.AzioniDocumento.menu((EconActivity) getActivity(), riga, PreventiviListaFragment.this::ricerca);
                }
            };
        }

        /** Scheda del documento (testata e righe con la grafica del rapportino). */
        @Override
        public void onRigaClick(EconListaStandardController.Host host, ContentValues riga) {
            Intent intent = new Intent(host.getContext(), com.ncfsistemi.econ.DocumentoActivity.class);
            intent.putExtra(Preventivi.ID_PREVENTIVO, riga.getAsInteger(Preventivi.ID_PREVENTIVO));
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
        public String funzionalitaNuovo() {
            return com.ncfsistemi.econ.utils.FunzionalitaApp.codice(
                    com.ncfsistemi.econ.utils.FunzionalitaApp.moduloPreventivo(getTipo()), com.ncfsistemi.econ.utils.FunzionalitaApp.CREA);
        }

        @Override
        public void onNuovoClick(EconListaStandardController.Host host) {
            Intent intent = new Intent(host.getContext(), PreventiviDettaglioModActivity.class);
            intent.putExtra(Preventivi.TIPO, getTipo());
            ((EconActivity) host.getContext()).apriFinestraInserimento(intent, 1, new Preventivi());
        }
    }

    /** Colore dello stato del documento (come il badge della scheda). */
    static int coloreStato(String stato) {
        if (Preventivi.STATO_APERTO.equals(stato)) return 0xFF1565C0;
        if (Preventivi.STATO_ACCETTATO.equals(stato)) return 0xFF2E7D32;
        if (Preventivi.STATO_RIFIUTATO.equals(stato)) return 0xFFC62828;
        return 0xFF607D8B;
    }
}
