package com.ncfsistemi.econ.fragments;

import android.content.ContentValues;
import android.content.Intent;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import java.util.Arrays;
import java.util.List;

import com.ncfsistemi.econ.CantiereSplitActivity;
import com.ncfsistemi.econ.CantieriDettaglioModActivity;
import com.ncfsistemi.econ.EconActivity;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.lista.ColonnaLista;
import com.ncfsistemi.econ.lista.EconListaStandardController;
import com.ncfsistemi.econ.lista.EconListaStandardDefinition;
import com.ncfsistemi.econ.lista.EconListaStandardFragment;
import com.ncfsistemi.econ.lista.FiltriHelper;
import com.ncfsistemi.econ.lista.QueryPagina;
import com.ncfsistemi.econ.utils.Sessione;

/**
 * Modulo Cantieri sullo standard di ricerca/lista condiviso (vedi com.ncfsistemi.econ.lista).
 * E' un Fragment (non una Activity diretta come Clienti) perche' viene incorporato sia a
 * schermo intero (CantieriActivity) sia come tab dentro ClientiDettaglioActivity, filtrato
 * per cliente.
 */
public class CantieriListaFragment extends EconListaStandardFragment {

    private int cliente = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_cantieri_lista;
    }

    @Override
    protected EconListaStandardDefinition creaDefinizione() {
        if (getArguments() != null) {
            cliente = getArguments().getInt(Anagrafica.ID_ANAGRAFICA);
        } else if (getActivity() != null && getActivity().getIntent() != null) {
            // lista aperta dalla scheda cliente ("Cantieri del cliente"): il cliente arriva nell'intent
            cliente = getActivity().getIntent().getIntExtra(Anagrafica.ID_ANAGRAFICA, 0);
        }
        return new CantieriDefinition();
    }

    /** Compat: chiamato da ClientiDettaglioActivity.aggiornaDopoCancellazione(). */
    public void ricerca() {
        ricaricaSeAttiva();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        ContentValues item = (ContentValues) getController().getElementoAllaPosizione(info.position);
        menu.setHeaderTitle(item.getAsString(Cantieri.NOME));
        menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.visualizza));
        menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.modifica));
        menu.add(Menu.NONE, 3, Menu.NONE, getString(R.string.elimina));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        ContentValues cantiere = (ContentValues) getController().getElementoAllaPosizione(info.position);
        if (item.getItemId() == 1) {
            apriCantiere(cantiere);
        }
        if (item.getItemId() == 2) {
            Intent intent = new Intent(getActivity(), CantieriDettaglioModActivity.class);
            intent.putExtra("ID", cantiere.getAsInteger(Cantieri.ID_CANTIERE));
            getEconActivity().apriFinestraModifica(intent, 2);
        }
        if (item.getItemId() == 3) {
            getEconActivity().confermaCancellazione(new Cantieri(), cantiere, true);
        }
        return super.onContextItemSelected(item);
    }

    private void apriCantiere(ContentValues cantiere) {
        Intent intent = new Intent(getActivity(), CantiereSplitActivity.class);
        intent.putExtra(Cantieri.ID_CANTIERE, cantiere.getAsInteger(Cantieri.ID_CANTIERE));
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    /** Query, colonne e azione di click riga specifiche del modulo Cantieri. */
    private class CantieriDefinition extends EconListaStandardDefinition {

        // Anagrafica e Cantieri condividono i nomi citta'/provincia/campi di audit: la
        // select va qualificata esplicitamente (solo le colonne di cantieri + il nome
        // cliente), altrimenti "select *" farebbe vincere il campo dell'ultima tabella.
        private static final String SELECT = Cantieri.NOME_TABELLA + ".*, " + Anagrafica.NOME_TABELLA + "." + Anagrafica.RAGIONE_SOCIALE;

        @Override
        public QueryPagina costruisciQuery(String testoFiltro, boolean ordinaPerRecenti) {
            String fromJoin = Cantieri.NOME_TABELLA + " inner join " + Anagrafica.NOME_TABELLA
                    + " on " + Cantieri.NOME_TABELLA + "." + Cantieri.ID_ANAGRAFICA
                    + "=" + Anagrafica.NOME_TABELLA + "." + Anagrafica.ID_ANAGRAFICA;

            QueryPagina base = FiltriHelper.likeMultiCampo(fromJoin, testoFiltro,
                    Cantieri.NOME_TABELLA + "." + Cantieri.NOME,
                    Cantieri.NOME_TABELLA + "." + Cantieri.INDIRIZZO,
                    Cantieri.NOME_TABELLA + "." + Cantieri.CITTA);

            String where = "(" + base.whereSql + ") and " + Cantieri.NOME_TABELLA + "." + Cantieri.ID_DITTA
                    + "=" + Sessione.getDittaSelezionata();
            if (cliente != 0) {
                where += " and " + Cantieri.NOME_TABELLA + "." + Cantieri.ID_ANAGRAFICA + "=" + cliente;
            }
            return new QueryPagina(SELECT, fromJoin, where, base.whereArgs);
        }

        @Override
        public List<ColonnaLista> getColonne() {
            String cantieriTab = Cantieri.NOME_TABELLA + ".";
            return Arrays.asList(
                    new ColonnaLista(Anagrafica.RAGIONE_SOCIALE, "Cliente", 26),
                    new ColonnaLista(Cantieri.NOME, "Cantiere", 26, true),
                    new ColonnaLista(Cantieri.INDIRIZZO, "Via", 24),
                    new ColonnaLista(Cantieri.CITTA, "Città", 16, false, null, cantieriTab + Cantieri.CITTA),
                    new ColonnaLista(Cantieri.PROVINCIA, "Prov.", 8, false, null, cantieriTab + Cantieri.PROVINCIA));
        }

        @Override
        public void onRigaClick(EconListaStandardController.Host host, ContentValues riga) {
            apriCantiere(riga);
        }

        @Override
        public String getOrdineRecenti() {
            return Cantieri.NOME_TABELLA + "." + Cantieri.DATA_INS + " desc";
        }

        @Override
        public String getColonnaOrdinamentoDefault() {
            return Cantieri.NOME;
        }

        @Override
        public String getTitolo() {
            return "Cantieri";
        }

        @Override
        public String funzionalitaNuovo() {
            return com.ncfsistemi.econ.utils.FunzionalitaApp.CANTIERI_CREA;
        }

        @Override
        public void onNuovoClick(EconListaStandardController.Host host) {
            Intent intent = new Intent(host.getContext(), CantieriDettaglioModActivity.class);
            if (cliente != 0) {
                // Incorporato come tab di un cliente: porta con se' tutti gli extra
                // dell'Activity ospitante (es. l'id del cliente stesso).
                intent.putExtras(getActivity().getIntent().getExtras());
            }
            ((EconActivity) host.getContext()).apriFinestraInserimento(intent, 2, new Cantieri());
        }

        @Override
        public boolean mostraBarraTitolo() {
            return cliente == 0;
        }
    }
}
