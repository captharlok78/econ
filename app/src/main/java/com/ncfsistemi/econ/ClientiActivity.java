package com.ncfsistemi.econ;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import java.util.Arrays;
import java.util.List;

import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.lista.ColonnaLista;
import com.ncfsistemi.econ.lista.EconListaStandardActivity;
import com.ncfsistemi.econ.lista.EconListaStandardController;
import com.ncfsistemi.econ.lista.EconListaStandardDefinition;
import com.ncfsistemi.econ.lista.FiltriHelper;
import com.ncfsistemi.econ.lista.QueryPagina;

/**
 * Modulo Clienti sullo standard di ricerca/lista condiviso (package com.ncfsistemi.econ.lista):
 * form filtri di default, paginazione, ordinamento per colonna e righe alternate sono
 * gestiti da EconListaStandardActivity/Controller. Qui restano solo la query/colonne/
 * click-riga di Clienti (ClientiDefinition) e le azioni specifiche del modulo (nuovo/
 * modifica/elimina dal menu contestuale).
 */
public class ClientiActivity extends EconListaStandardActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_clienti;
    }

    @Override
    protected EconListaStandardDefinition creaDefinizione() {
        return new ClientiDefinition();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerForContextMenu(getController().getListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        ContentValues item = (ContentValues) getController().getElementoAllaPosizione(info.position);
        menu.setHeaderTitle(item.getAsString(Anagrafica.RAGIONE_SOCIALE));
        menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
        menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        ContentValues cliente = (ContentValues) getController().getElementoAllaPosizione(info.position);
        if (item.getItemId() == 1) {
            Intent intent = new Intent(this, ClientiDettaglioModActivity.class);
            intent.putExtra("ID", cliente.getAsInteger(Anagrafica.ID_ANAGRAFICA));
            apriFinestraModifica(intent, 1);
        }
        if (item.getItemId() == 2) {
            confermaCancellazione(new Anagrafica(), cliente, true);
        }
        return super.onContextItemSelected(item);
    }

    /** Query, colonne e azione di click riga specifiche del modulo Clienti. */
    private static class ClientiDefinition extends EconListaStandardDefinition {

        @Override
        public QueryPagina costruisciQuery(String testoFiltro, boolean ordinaPerRecenti) {
            return FiltriHelper.likeMultiCampo(Anagrafica.NOME_TABELLA, testoFiltro,
                    Anagrafica.RAGIONE_SOCIALE, Anagrafica.INDIRIZZO, Anagrafica.CITTA, Anagrafica.CODICE);
        }

        @Override
        public List<ColonnaLista> getColonne() {
            return Arrays.asList(
                    new ColonnaLista(Anagrafica.CODICE, "Cod.", 14),
                    new ColonnaLista(Anagrafica.RAGIONE_SOCIALE, "Ragione sociale", 32, true),
                    new ColonnaLista(Anagrafica.INDIRIZZO, "Via", 24),
                    new ColonnaLista(Anagrafica.CITTA, "Città", 20),
                    new ColonnaLista(Anagrafica.PROVINCIA, "Prov.", 10));
        }

        @Override
        public void onRigaClick(EconListaStandardController.Host host, ContentValues riga) {
            Intent intent = new Intent(host.getContext(), ClientiDettaglioActivity.class);
            intent.putExtra(Anagrafica.ID_ANAGRAFICA, riga.getAsInteger(Anagrafica.ID_ANAGRAFICA));
            host.startActivity(intent);
        }

        @Override
        public String getTitolo() {
            return "Clienti";
        }

        @Override
        public String funzionalitaNuovo() {
            return com.ncfsistemi.econ.utils.FunzionalitaApp.CLIENTI_CREA;
        }

        @Override
        public void onNuovoClick(EconListaStandardController.Host host) {
            Intent intent = new Intent(host.getContext(), ClientiDettaglioModActivity.class);
            ((EconActivity) host.getContext()).apriFinestraInserimento(intent, 1, new Anagrafica());
        }

        @Override
        public String getOrdineRecenti() {
            return Anagrafica.DATA_MOD + " is null asc, " + Anagrafica.DATA_MOD + " desc, " + Anagrafica.DATA_INS + " desc";
        }

        @Override
        public String getColonnaOrdinamentoDefault() {
            return Anagrafica.RAGIONE_SOCIALE;
        }
    }
}
