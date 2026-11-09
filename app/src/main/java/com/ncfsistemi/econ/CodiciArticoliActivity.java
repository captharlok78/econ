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

import java.util.Arrays;
import java.util.List;

import com.ncfsistemi.econ.db.table.AssCodiciLinee;
import com.ncfsistemi.econ.db.table.Costruttori;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Listini;
import com.ncfsistemi.econ.lista.ColonnaLista;
import com.ncfsistemi.econ.lista.EconListaStandardActivity;
import com.ncfsistemi.econ.lista.EconListaStandardController;
import com.ncfsistemi.econ.lista.EconListaStandardDefinition;
import com.ncfsistemi.econ.lista.FiltriHelper;
import com.ncfsistemi.econ.lista.QueryPagina;
import com.ncfsistemi.econ.views.CampoRicerca;

/**
 * Menu Articoli (ex Listini): elenco degli articoli sul dispositivo sullo standard di ricerca/lista condiviso, tabellare
 * come Clienti e Cantieri (testata ordinabile, pagine, esportazione xls). Filtri (layout filtri_articoli: due righe sugli
 * schermi stretti, una sui tablet in orizzontale): testo (codice, descrizione,
 * barcode, costruttore), lettore barcode con la fotocamera (un solo articolo con quel barcode = si apre la scheda) e
 * costruttore (scritto o scelto dall'elenco). "Ultimi 20" = i 20 articoli inseriti o modificati per ultimi; Reset e
 * Ultimi 20 svuotano anche il costruttore.
 */
public class CodiciArticoliActivity extends EconListaStandardActivity {

    /** Dal drill-down "Altro materiale": articoli del costruttore senza linea. */
    public static final String EXTRA_SENZA_LINEA = "senza_linea";
    /** Testo da cercare negli articoli (codice o descrizione), dalla ricerca dei Listini. */
    public static final String EXTRA_TESTO = "testo";

    private boolean senzaLinea = false;

    private CampoRicerca campoFornitore;
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
        idFornitoreIniziale = getIntent().getIntExtra(Costruttori.ID_COSTRUTTORE, 0);
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

        com.ncfsistemi.econ.utils.LettoreBarcode.collega(this, findViewById(R.id.buttonLeggiBarcode), this::cercaBarcode);
    }

    /**
     * Barcode letto: se un solo articolo ha proprio quel barcode si apre la sua scheda, altrimenti si cerca il codice
     * (anche dentro codice e descrizione) e si mostra l'elenco.
     */
    private void cercaBarcode(String codice) {
        com.ncfsistemi.econ.db.DbInterno db = new com.ncfsistemi.econ.db.DbInterno(this);
        ArrayList<Object> trovati = db.eseguiSelect("select " + Listini.ID + " from " + Listini.NOME_TABELLA + " where "
                + Listini.BARCODE + " = ? limit 2", new String[]{codice});
        db.close();
        campoFornitore.setText("");
        ((android.widget.EditText) findViewById(R.id.editText_filtra)).setText(codice);
        getController().eseguiRicerca(false);
        if (trovati.size() == 1) {
            Intent intent = new Intent(this, CodiceArticoloModActivity.class);
            intent.putExtra("ID", ((ContentValues) trovati.get(0)).getAsInteger(Listini.ID));
            startActivity(intent);
        } else if (trovati.isEmpty()) {
            android.widget.Toast.makeText(this, "Nessun articolo con il barcode " + codice, android.widget.Toast.LENGTH_LONG).show();
        }
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

    /** Testo scritto nel campo costruttore senza sceglierlo dall'elenco: si cerca su sigla e ragione sociale. */
    private String testoFornitore() {
        return campoFornitore.getIdScelto() > 0 ? "" : campoFornitore.getText().toString().trim();
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

    /** Ricarica la pagina corrente (dopo una modifica o un'eliminazione). */
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

    /** Query, colonne e azione di click riga del modulo Articoli. */
    private class CodiciArticoliDefinition extends EconListaStandardDefinition {

        private static final int ULTIMI = 20;

        private static final String L = Listini.NOME_TABELLA + ".";
        private static final String C = Costruttori.NOME_TABELLA + ".";

        @Override
        public QueryPagina costruisciQuery(String testoFiltro, boolean ordinaPerRecenti) {
            String fromJoin = Listini.NOME_TABELLA
                    + " left join " + Costruttori.NOME_TABELLA + " on " + L + Listini.ID_COSTRUTTORE + "=" + C + Costruttori.ID_COSTRUTTORE
                    + " left join " + Linee.NOME_TABELLA + " on " + L + Listini.ID_LINEA + "=" + Linee.NOME_TABELLA + "." + Linee.ID_LINEA;

            String select = Listini.NOME_TABELLA + ".*, " + C + Costruttori.SIGLA_METEL + ", " + C + Costruttori.RAGIONE_SOCIALE + ", "
                    + Linee.NOME_TABELLA + "." + Linee.NOME_LINEA;

            QueryPagina like = FiltriHelper.likeMultiCampo(fromJoin, testoFiltro, L + Listini.CODICE_ARTICOLO, L + Listini.DESCRIZIONE,
                    L + Listini.BARCODE, C + Costruttori.SIGLA_METEL, C + Costruttori.RAGIONE_SOCIALE);
            List<String> args = new ArrayList<>(Arrays.asList(like.whereArgs));
            String where = "(" + like.whereSql + ")";

            if (ordinaPerRecenti) {
                // gli ultimi 20 inseriti o modificati, tutti in una pagina, dal più recente
                where += " and " + L + Listini.ID + " in (select " + Listini.ID + " from " + Listini.NOME_TABELLA
                        + " order by " + getOrdineRecenti().replace(L, "") + " limit " + ULTIMI + ")";
                return new QueryPagina(select, fromJoin, where, args.toArray(new String[0]));
            }

            String valForn = valoreFornitore();
            if (!valForn.equals("")) {
                where += " and " + L + Listini.ID_COSTRUTTORE + "=" + valForn;
                // "Altro materiale" dall'elenco dei costruttori: solo quelli senza una linea
                if (senzaLinea && valForn.equals("" + idFornitoreIniziale)) {
                    where += " and (" + L + Listini.ID_LINEA + " is null or " + L + Listini.ID_LINEA + "=0 or " + L
                            + Listini.ID_LINEA + " not in (select " + Linee.ID_LINEA + " from " + Linee.NOME_TABELLA + "))";
                }
            } else if (!testoFornitore().isEmpty()) {
                where += " and (" + C + Costruttori.SIGLA_METEL + " like ? or " + C + Costruttori.RAGIONE_SOCIALE + " like ?)";
                args.add(testoFornitore() + "%");
                args.add("%" + testoFornitore() + "%");
            }

            return new QueryPagina(select, fromJoin, where, args.toArray(new String[0]));
        }

        @Override
        public List<ColonnaLista> getColonne() {
            return Arrays.asList(
                    new ColonnaLista(Listini.CODICE_ARTICOLO, "Codice", 16, true, null, L + Listini.CODICE_ARTICOLO),
                    new ColonnaLista(Listini.DESCRIZIONE, "Descrizione", 34, false, null, L + Listini.DESCRIZIONE),
                    new ColonnaLista(Costruttori.RAGIONE_SOCIALE, "Costruttore", 18, false, riga -> {
                        String rs = riga.getAsString(Costruttori.RAGIONE_SOCIALE);
                        return rs != null ? rs : "";
                    }, C + Costruttori.RAGIONE_SOCIALE),
                    new ColonnaLista(Linee.NOME_LINEA, "Linea", 12, false, null, Linee.NOME_TABELLA + "." + Linee.NOME_LINEA),
                    new ColonnaLista(Listini.BARCODE, "Barcode", 14, false, null, L + Listini.BARCODE),
                    ColonnaLista.importo(Listini.PRZ_LISTINO, "Listino €", 12, L + Listini.PRZ_LISTINO));
        }

        @Override
        public void azzeraFiltri() {
            campoFornitore.setText("");
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
        public int getRisultatiPerPaginaDefault() {
            return ULTIMI;
        }

        @Override
        public String getOrdineRecenti() {
            // ultima scrittura: modifica, o inserimento se mai modificato (con "data_mod is null" in fondo un articolo
            // appena inserito finirebbe dopo tutti quelli modificati)
            return "coalesce(" + L + Listini.DATA_MOD + ", " + L + Listini.DATA_INS + ") desc, " + L + Listini.ID + " desc";
        }

        @Override
        public String getTitolo() {
            return "Articoli";
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
}
