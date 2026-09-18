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

import java.util.ArrayList;

import pfa.app.econtab.adapters.CodiciArticoliAdapter;
import pfa.app.econtab.adapters.EConTabListViewAdapter;
import pfa.app.econtab.db.table.AssCodiciLinee;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Listini;
import pfa.app.econtab.lista.EConTabListaStandardActivity;
import pfa.app.econtab.lista.EConTabListaStandardController;
import pfa.app.econtab.lista.EConTabListaStandardDefinition;
import pfa.app.econtab.lista.FiltriHelper;
import pfa.app.econtab.lista.QueryPagina;
import pfa.app.econtab.views.EConTabSpinner;

/**
 * Modulo Codici articoli (Listini) sullo standard di ricerca/lista condiviso. Le righe
 * (prezzi formattati, icona associazioni con linee alternative) sono troppo custom per il
 * modello a colonne: usa il proprio CodiciArticoliAdapter (via creaAdapterPersonalizzato),
 * come Preventivi/Ordini/Rapportini.
 */
public class CodiciArticoliActivity extends EConTabListaStandardActivity {

    private EConTabSpinner spinnerFornitore;
    private EConTabSpinner spinnerLinea;
    private int idFornitoreIniziale = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_codici_articoli;
    }

    @Override
    protected EConTabListaStandardDefinition creaDefinizione() {
        return new CodiciArticoliDefinition();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registerForContextMenu(getController().getListView());

        spinnerFornitore = findViewById(R.id.spinner_fornitore);
        spinnerLinea = findViewById(R.id.spinner_linea);
        idFornitoreIniziale = getIntent().getIntExtra(Costruttori.ID_COSTRUTTORE, 0);
        int idLinea = getIntent().getIntExtra(Linee.ID_LINEA, 0);
        if (idFornitoreIniziale != 0) {
            spinnerFornitore.setValue("" + idFornitoreIniziale);
        }
        if (idLinea != 0) {
            spinnerLinea.setValue("" + idLinea);
        }
        spinnerFornitore.setTabella(new Costruttori());
        spinnerLinea.setTabella(new Linee());
        spinnerFornitore.setSpinnerCollegato(spinnerLinea);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Se arriviamo da un drill-down (fornitore/linea gia' scelti), la ricerca parte
        // subito; altrimenti si resta sulla sola form come in ogni altro modulo.
        if (idFornitoreIniziale != 0) {
            getController().eseguiRicerca(false);
        }
    }

    /** Compat: chiamato da CodiciArticoliAdapter dopo Elimina. */
    public void ricerca() {
        getController().ricaricaSeAttiva();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        ContentValues item = (ContentValues) getController().getElementoAllaPosizione(info.position);
        menu.setHeaderTitle(item.getAsString(Listini.CODICE_ARTICOLO));
        menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
        menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
        menu.add(Menu.NONE, 3, Menu.NONE, "Visualizza associazioni con linee alternative");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        ContentValues articolo = (ContentValues) getController().getElementoAllaPosizione(info.position);
        if (item.getItemId() == 1) {
            Intent intent = new Intent(this, CodiceArticoloModActivity.class);
            intent.putExtra("ID", articolo.getAsString(Listini.CODICE_ARTICOLO));
            apriFinestraModifica(intent, 1);
        }
        if (item.getItemId() == 2) {
            confermaCancellazione(new Listini(), articolo, true);
        }
        if (item.getItemId() == 3) {
            Intent intent = new Intent(this, AssCodiciLineeActivity.class);
            intent.putExtra(AssCodiciLinee.CODICE_ARTICOLO, articolo.getAsString(Listini.CODICE_ARTICOLO));
            startActivity(intent);
        }
        return super.onContextItemSelected(item);
    }

    /** Query, adapter e azione di click riga specifiche del modulo Codici articoli. */
    private class CodiciArticoliDefinition extends EConTabListaStandardDefinition {

        @Override
        public QueryPagina costruisciQuery(String testoFiltro, boolean ordinaPerRecenti) {
            String fromJoin = Listini.NOME_TABELLA
                    + " left join " + Costruttori.NOME_TABELLA + " on " + Listini.NOME_TABELLA + "." + Listini.ID_COSTRUTTORE
                    + "=" + Costruttori.NOME_TABELLA + "." + Costruttori.ID_COSTRUTTORE
                    + " left join " + Linee.NOME_TABELLA + " on " + Listini.NOME_TABELLA + "." + Listini.ID_LINEA
                    + "=" + Linee.NOME_TABELLA + "." + Linee.ID_LINEA;

            String select = Listini.NOME_TABELLA + ".*, " + Costruttori.NOME_TABELLA + "." + Costruttori.SIGLA_METEL + ", "
                    + Linee.NOME_TABELLA + "." + Linee.NOME_LINEA + ", "
                    + "(select count(*) from " + AssCodiciLinee.NOME_TABELLA + " where " + AssCodiciLinee.CODICE_ARTICOLO
                    + "=" + Listini.NOME_TABELLA + "." + Listini.CODICE_ARTICOLO + ") as asscodicilinee";

            QueryPagina like = FiltriHelper.likeMultiCampo(fromJoin, testoFiltro,
                    Listini.NOME_TABELLA + "." + Listini.CODICE_ARTICOLO, Listini.NOME_TABELLA + "." + Listini.DESCRIZIONE);

            String where = like.whereSql;
            String valForn = spinnerFornitore.getValue();
            String valLinea = spinnerLinea.getValue();
            if (!valForn.equals("")) {
                where += " and " + Listini.NOME_TABELLA + "." + Listini.ID_COSTRUTTORE + "=" + valForn;
                if (valLinea.equals("")) {
                    where += " and " + Listini.NOME_TABELLA + "." + Listini.ID_LINEA + "=0";
                }
            }
            if (!valLinea.equals("")) {
                where += " and " + Listini.NOME_TABELLA + "." + Listini.ID_LINEA + "=" + valLinea;
            }

            return new QueryPagina(select, fromJoin, where, like.whereArgs);
        }

        @Override
        public boolean isTabellare() {
            return false;
        }

        @Override
        public EConTabListViewAdapter creaAdapterPersonalizzato(android.content.Context context, ArrayList<Object> dati) {
            return new CodiciArticoliAdapter(context, dati, R.layout.list_item_codice_articolo);
        }

        @Override
        public void onRigaClick(EConTabListaStandardController.Host host, ContentValues riga) {
            Intent intent = new Intent(host.getContext(), CodiceArticoloModActivity.class);
            intent.putExtra("ID", riga.getAsString(Listini.CODICE_ARTICOLO));
            host.startActivity(intent);
        }

        @Override
        public String getColonnaOrdinamentoDefault() {
            return Listini.CODICE_ARTICOLO;
        }

        @Override
        public String getOrdineRecenti() {
            return Listini.NOME_TABELLA + "." + Listini.DATA_MOD + " is null asc, "
                    + Listini.NOME_TABELLA + "." + Listini.DATA_MOD + " desc, "
                    + Listini.NOME_TABELLA + "." + Listini.DATA_INS + " desc";
        }

        @Override
        public String getTitolo() {
            return "Codici articoli";
        }

        @Override
        public void onNuovoClick(EConTabListaStandardController.Host host) {
            Intent intent = new Intent(host.getContext(), CodiceArticoloModActivity.class);
            if (!spinnerFornitore.getValue().equals("")) {
                intent.putExtra(Listini.ID_COSTRUTTORE, Integer.parseInt(spinnerFornitore.getValue()));
            }
            if (!spinnerLinea.getValue().equals("")) {
                intent.putExtra(Listini.ID_LINEA, Integer.parseInt(spinnerLinea.getValue()));
            }
            ((EConTabActivity) host.getContext()).apriFinestraInserimento(intent, 1, new Listini());
        }
    }
}
