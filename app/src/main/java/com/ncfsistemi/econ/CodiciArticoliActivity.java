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

import java.util.ArrayList;

import com.ncfsistemi.econ.adapters.CodiciArticoliAdapter;
import com.ncfsistemi.econ.adapters.EconListViewAdapter;
import com.ncfsistemi.econ.db.table.AssCodiciLinee;
import com.ncfsistemi.econ.db.table.Costruttori;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Listini;
import com.ncfsistemi.econ.lista.EconListaStandardActivity;
import com.ncfsistemi.econ.lista.EconListaStandardController;
import com.ncfsistemi.econ.lista.EconListaStandardDefinition;
import com.ncfsistemi.econ.lista.FiltriHelper;
import com.ncfsistemi.econ.lista.QueryPagina;
import com.ncfsistemi.econ.views.CampoRicerca;
import com.ncfsistemi.econ.views.EconSpinner;

/**
 * Modulo Codici articoli (Listini) sullo standard di ricerca/lista condiviso. Le righe
 * (prezzi formattati, icona associazioni con linee alternative) sono troppo custom per il
 * modello a colonne: usa il proprio CodiciArticoliAdapter (via creaAdapterPersonalizzato),
 * come Preventivi/Ordini/Rapportini.
 */
public class CodiciArticoliActivity extends EconListaStandardActivity {

    /** Dal drill-down "Altro materiale": articoli del costruttore senza linea. */
    public static final String EXTRA_SENZA_LINEA = "senza_linea";
    /** Testo da cercare negli articoli (codice o descrizione), dalla ricerca dei Listini. */
    public static final String EXTRA_TESTO = "testo";

    private boolean senzaLinea = false;

    private CampoRicerca campoFornitore;
    private EconSpinner spinnerLinea;
    private int idFornitoreIniziale = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_codici_articoli;
    }

    @Override
    protected EconListaStandardDefinition creaDefinizione() {
        return new CodiciArticoliDefinition();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registerForContextMenu(getController().getListView());

        campoFornitore = findViewById(R.id.campo_fornitore);
        spinnerLinea = findViewById(R.id.spinner_linea);
        idFornitoreIniziale = getIntent().getIntExtra(Costruttori.ID_COSTRUTTORE, 0);
        int idLinea = getIntent().getIntExtra(Linee.ID_LINEA, 0);
        senzaLinea = idFornitoreIniziale != 0 && getIntent().getBooleanExtra(EXTRA_SENZA_LINEA, false);
        String testo = getIntent().getStringExtra(EXTRA_TESTO);
        if (testo != null && !testo.isEmpty()) {
            ((android.widget.EditText) findViewById(R.id.editText_filtra)).setText(testo);
        }
        // costruttore: si scrivono 3 lettere del nome o la sigla (sono centinaia, una tendina non serve)
        campoFornitore.setVoci(costruttoriConArticoli());
        if (idFornitoreIniziale != 0) {
            campoFornitore.selezionaId(idFornitoreIniziale);
        }
        if (idLinea != 0) {
            spinnerLinea.setValue("" + idLinea);
        }
        spinnerLinea.setTabella(new LineeConArticoli());
        aggiornaLinee(idFornitoreIniziale);
        campoFornitore.setAscoltatore((campo, voce) -> {
            spinnerLinea.setValue("");
            aggiornaLinee(voce != null ? voce.id : 0);
        });
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

    /** Id del costruttore scelto, "" se nessuno. */
    private String valoreFornitore() {
        return campoFornitore.getIdScelto() > 0 ? "" + campoFornitore.getIdScelto() : "";
    }

    /** Linee del costruttore scelto (solo quelle con articoli); senza costruttore la tendina è spenta. */
    private void aggiornaLinee(int idCostruttore) {
        spinnerLinea.setEnabled(idCostruttore > 0);
        ContentValues filtro = new ContentValues();
        if (idCostruttore > 0) {
            filtro.put(Linee.ID_COSTRUTTORE, idCostruttore);
        }
        spinnerLinea.setFiltro(filtro);
        spinnerLinea.setTabella(spinnerLinea.getTabella(), spinnerLinea.getCampoCodice(), spinnerLinea.getCampoDescrizione());
    }

    /** I costruttori con articoli sul dispositivo, "SIGLA - Ragione sociale": la ricerca trova anche la sigla. */
    private java.util.List<CampoRicerca.Voce> costruttoriConArticoli() {
        java.util.List<CampoRicerca.Voce> voci = new ArrayList<>();
        com.ncfsistemi.econ.db.DbInterno db = new com.ncfsistemi.econ.db.DbInterno(this);
        for (Object o : db.eseguiSelect(new CostruttoriConArticoli().getSQLPerSpinner(null), null)) {
            ContentValues c = (ContentValues) o;
            String sigla = c.getAsString(Costruttori.SIGLA_METEL);
            voci.add(new CampoRicerca.Voce(c.getAsInteger(Costruttori.ID_COSTRUTTORE),
                    (sigla != null && !sigla.isEmpty() ? sigla + " - " : "") + c.getAsString(Costruttori.RAGIONE_SOCIALE), c));
        }
        db.close();
        return voci;
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
            intent.putExtra("ID", articolo.getAsInteger(Listini.ID));
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
    private class CodiciArticoliDefinition extends EconListaStandardDefinition {

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
            String valForn = valoreFornitore();
            String valLinea = spinnerLinea.getValue();
            if (!valForn.equals("")) {
                where += " and " + Listini.NOME_TABELLA + "." + Listini.ID_COSTRUTTORE + "=" + valForn;
                // senza linea scelta: tutti gli articoli del costruttore (prima solo quelli con linea 0, che dalla linea
                // "Generica" non ci sono quasi più: il filtro per costruttore non trovava niente); "Altro materiale" del
                // drill-down: solo quelli senza una linea
                if (valLinea.equals("") && senzaLinea && valForn.equals("" + idFornitoreIniziale)) {
                    where += " and (" + Listini.NOME_TABELLA + "." + Listini.ID_LINEA + " is null or " + Listini.NOME_TABELLA + "."
                            + Listini.ID_LINEA + "=0 or " + Listini.NOME_TABELLA + "." + Listini.ID_LINEA + " not in (select "
                            + Linee.ID_LINEA + " from " + Linee.NOME_TABELLA + "))";
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
        public EconListViewAdapter creaAdapterPersonalizzato(android.content.Context context, ArrayList<Object> dati) {
            return new CodiciArticoliAdapter(context, dati, R.layout.list_item_codice_articolo);
        }

        @Override
        public void onRigaClick(EconListaStandardController.Host host, ContentValues riga) {
            Intent intent = new Intent(host.getContext(), CodiceArticoloModActivity.class);
            intent.putExtra("ID", riga.getAsInteger(Listini.ID));
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
        public String funzionalitaNuovo() {
            return com.ncfsistemi.econ.utils.FunzionalitaApp.codice(com.ncfsistemi.econ.utils.FunzionalitaApp.LISTINI,
                    com.ncfsistemi.econ.utils.FunzionalitaApp.CREA);
        }

        @Override
        public void onNuovoClick(EconListaStandardController.Host host) {
            Intent intent = new Intent(host.getContext(), CodiceArticoloModActivity.class);
            if (!valoreFornitore().equals("")) {
                intent.putExtra(Listini.ID_COSTRUTTORE, Integer.parseInt(valoreFornitore()));
            }
            if (!spinnerLinea.getValue().equals("")) {
                intent.putExtra(Listini.ID_LINEA, Integer.parseInt(spinnerLinea.getValue()));
            }
            ((EconActivity) host.getContext()).apriFinestraInserimento(intent, 1, new Listini());
        }
    }

    /** Nel filtro solo i costruttori con articoli sul dispositivo (non tutti i generali). */
    static class CostruttoriConArticoli extends Costruttori {
        @Override
        public String getSQLPerSpinner(ContentValues filtro) {
            return "select * from " + NOME_TABELLA + " where " + ID_COSTRUTTORE + " in (select " + Listini.ID_COSTRUTTORE + " from "
                    + Listini.NOME_TABELLA + ") order by " + RAGIONE_SOCIALE;
        }
    }

    /** Nel filtro solo le linee con articoli sul dispositivo (ogni costruttore ha la sua "Generica", spesso vuota). */
    static class LineeConArticoli extends Linee {
        @Override
        public String getSQLPerSpinner(ContentValues filtro) {
            String sql = super.getSQLPerSpinner(filtro);
            int i = sql.indexOf(" order by ");
            String prima = sql.substring(0, i);
            return prima + (prima.contains(" where ") ? " and " : " where ") + NOME_TABELLA + "." + ID_LINEA + " in (select "
                    + Listini.ID_LINEA + " from " + Listini.NOME_TABELLA + ")" + sql.substring(i);
        }
    }
}
