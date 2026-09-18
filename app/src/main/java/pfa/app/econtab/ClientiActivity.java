package pfa.app.econtab;

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

import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.lista.ColonnaLista;
import pfa.app.econtab.lista.EConTabListaStandardActivity;
import pfa.app.econtab.lista.EConTabListaStandardController;
import pfa.app.econtab.lista.EConTabListaStandardDefinition;
import pfa.app.econtab.lista.FiltriHelper;
import pfa.app.econtab.lista.QueryPagina;

/**
 * Modulo Clienti sullo standard di ricerca/lista condiviso (package pfa.app.econtab.lista):
 * form filtri di default, paginazione, ordinamento per colonna e righe alternate sono
 * gestiti da EConTabListaStandardActivity/Controller. Qui restano solo la query/colonne/
 * click-riga di Clienti (ClientiDefinition) e le azioni specifiche del modulo (nuovo/
 * modifica/elimina dal menu contestuale).
 */
public class ClientiActivity extends EConTabListaStandardActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_clienti;
    }

    @Override
    protected EConTabListaStandardDefinition creaDefinizione() {
        return new ClientiDefinition();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerForContextMenu(getController().getListView());
    }

    public void nuovoCliente(View v) {
        Intent intent = new Intent(this, ClientiDettaglioModActivity.class);
        apriFinestraInserimento(intent, 1, new Anagrafica());
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
    private static class ClientiDefinition extends EConTabListaStandardDefinition {

        @Override
        public QueryPagina costruisciQuery(String testoFiltro, boolean ordinaPerRecenti) {
            return FiltriHelper.likeMultiCampo(Anagrafica.NOME_TABELLA, testoFiltro,
                    Anagrafica.RAGIONE_SOCIALE, Anagrafica.INDIRIZZO, Anagrafica.CITTA, Anagrafica.CODICE_ESTERNO);
        }

        @Override
        public List<ColonnaLista> getColonne() {
            return Arrays.asList(
                    new ColonnaLista(Anagrafica.CODICE_ESTERNO, "Cod.", 14),
                    new ColonnaLista(Anagrafica.RAGIONE_SOCIALE, "Ragione sociale", 32, true),
                    new ColonnaLista(Anagrafica.INDIRIZZO, "Via", 24),
                    new ColonnaLista(Anagrafica.CITTA, "Città", 20),
                    new ColonnaLista(Anagrafica.PROVINCIA, "Prov.", 10));
        }

        @Override
        public void onRigaClick(EConTabListaStandardController.Host host, ContentValues riga) {
            Intent intent = new Intent(host.getContext(), ClientiDettaglioActivity.class);
            intent.putExtra(Anagrafica.ID_ANAGRAFICA, riga.getAsInteger(Anagrafica.ID_ANAGRAFICA));
            host.startActivity(intent);
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
