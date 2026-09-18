package pfa.app.econtab.fragments;

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

import pfa.app.econtab.CantiereSplitActivity;
import pfa.app.econtab.CantieriDettaglioModActivity;
import pfa.app.econtab.EConTabActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.lista.ColonnaLista;
import pfa.app.econtab.lista.EConTabListaStandardController;
import pfa.app.econtab.lista.EConTabListaStandardDefinition;
import pfa.app.econtab.lista.EConTabListaStandardFragment;
import pfa.app.econtab.lista.FiltriHelper;
import pfa.app.econtab.lista.QueryPagina;
import pfa.app.econtab.utils.Sessione;

/**
 * Modulo Cantieri sullo standard di ricerca/lista condiviso (vedi pfa.app.econtab.lista).
 * E' un Fragment (non una Activity diretta come Clienti) perche' viene incorporato sia a
 * schermo intero (CantieriActivity) sia come tab dentro ClientiDettaglioActivity, filtrato
 * per cliente.
 */
public class CantieriListaFragment extends EConTabListaStandardFragment {

    private int cliente = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_cantieri_lista;
    }

    @Override
    protected EConTabListaStandardDefinition creaDefinizione() {
        if (getArguments() != null) {
            cliente = getArguments().getInt(Anagrafica.ID_ANAGRAFICA);
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
            getEConTabActivity().apriFinestraModifica(intent, 2);
        }
        if (item.getItemId() == 3) {
            getEConTabActivity().confermaCancellazione(new Cantieri(), cantiere, true);
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
    private class CantieriDefinition extends EConTabListaStandardDefinition {

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
        public void onRigaClick(EConTabListaStandardController.Host host, ContentValues riga) {
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
        public void onNuovoClick(EConTabListaStandardController.Host host) {
            Intent intent = new Intent(host.getContext(), CantieriDettaglioModActivity.class);
            if (cliente != 0) {
                // Incorporato come tab di un cliente: porta con se' tutti gli extra
                // dell'Activity ospitante (es. l'id del cliente stesso).
                intent.putExtras(getActivity().getIntent().getExtras());
            }
            ((EConTabActivity) host.getContext()).apriFinestraInserimento(intent, 2, new Cantieri());
        }

        @Override
        public boolean mostraBarraTitolo() {
            return cliente == 0;
        }
    }
}
