package com.ncfsistemi.econ;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Preventivi;
import com.ncfsistemi.econ.db.table.Rapportini;
import com.ncfsistemi.econ.db.table.RapportiniDettaglio;
import com.ncfsistemi.econ.db.table.RapportiniDettaglioTipi;
import com.ncfsistemi.econ.db.table.StatiDocumento;
import com.ncfsistemi.econ.db.table.UnitaMisura;
import com.ncfsistemi.econ.db.table.Utenti;
import com.ncfsistemi.econ.export.RapportinoXLS;
import com.ncfsistemi.econ.lista.ColonnaLista;
import com.ncfsistemi.econ.lista.EconListaStandardActivity;
import com.ncfsistemi.econ.lista.EconListaStandardController;
import com.ncfsistemi.econ.lista.EconListaStandardDefinition;
import com.ncfsistemi.econ.lista.FiltriHelper;
import com.ncfsistemi.econ.lista.QueryPagina;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.RegoleRapportino;
import com.ncfsistemi.econ.utils.Sessione;
import com.ncfsistemi.econ.utils.Utility;
import com.ncfsistemi.econ.views.EconSpinner;

/**
 * Modulo Rapportini sullo standard di ricerca/lista condiviso (vedi com.ncfsistemi.econ.lista).
 * Il filtro "periodo" e' un range di date calcolato (non un like/OR su colonne), diverso
 * dagli altri moduli: resta logica interamente custom in RapportiniDefinition. Le righe sono
 * tabellari come Clienti e Cantieri (una riga, testata ordinabile, stato a icona colorata) con
 * le azioni del rapportino nel pulsante a icona in fondo alla riga.
 */
public class RapportiniActivity extends EconListaStandardActivity {

    private EconSpinner spinnerPeriodo;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_rapportini;
    }

    @Override
    protected EconListaStandardDefinition creaDefinizione() {
        return new RapportiniDefinition();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registerForContextMenu(getController().getListView());

        spinnerPeriodo = findViewById(R.id.spinner_periodo);
        ArrayList<Object> valoriPeriodo = new ArrayList<>();
        valoriPeriodo.add(vocePeriodo("S", getString(R.string.ultima_settimana)));
        valoriPeriodo.add(vocePeriodo("M", getString(R.string.ultimo_mese)));
        valoriPeriodo.add(vocePeriodo("3M", getString(R.string.ultimi_3_mesi)));
        valoriPeriodo.add(vocePeriodo("6M", getString(R.string.ultimi_6_mesi)));
        valoriPeriodo.add(vocePeriodo("A", getString(R.string.ultimo_anno)));
        valoriPeriodo.add(vocePeriodo("", getString(R.string.sempre)));
        spinnerPeriodo.setValue(com.ncfsistemi.econ.utils.PreferenzeDispositivo.periodoRapportini(this));
        spinnerPeriodo.setValoriSpinnerLibero(valoriPeriodo);
    }

    private ContentValues vocePeriodo(String valore, String descrizione) {
        ContentValues val = new ContentValues();
        val.put(EconSpinner.VALORE, valore);
        val.put(EconSpinner.DESCRIZIONE, descrizione);
        return val;
    }

    /** Ricarica la lista dopo un'eliminazione. */
    public void ricerca() {
        getController().ricaricaSeAttiva();
    }

    public void modifica(ContentValues rapportino) {
        Intent intent = new Intent(this, RapportinoDettaglioModActivity.class);
        intent.putExtra("ID", rapportino.getAsInteger(Rapportini.ID));
        apriFinestraModifica(intent, 1);
    }

    public void esportaRapportino(final int idRapportino, final boolean multiplo) {
        AsyncTask<Object, Object, Object> task = new AsyncTask<Object, Object, Object>() {
            AlertDialog di = null;

            @Override
            protected void onPreExecute() {
                AlertDialog.Builder ab = new AlertDialog.Builder(RapportiniActivity.this);
                ab.setMessage(getString(R.string.messaggio_creazione_report));
                di = ab.create();
                di.show();
            }

            @Override
            protected Object doInBackground(Object... objects) {
                RapportinoXLS exp = new RapportinoXLS(RapportiniActivity.this);
                try {
                    return exp.generaReportRapportino(idRapportino, multiplo);
                } catch (Exception e) {
                    return "Errore:" + Log.getStackTraceString(e);
                }
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                di.cancel();
                if (!o.toString().startsWith("Errore")) {
                    File fileExport = new File(o.toString());
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(com.ncfsistemi.econ.utils.Utility.uriCondivisibile(RapportiniActivity.this, fileExport), Utility.XLS); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } else {
                    Toast.makeText(RapportiniActivity.this, o.toString(), Toast.LENGTH_LONG).show();
                }
            }
        };
        task.execute();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        ContentValues item = (ContentValues) getController().getElementoAllaPosizione(info.position);
        menu.setHeaderTitle(getString(R.string.rapportino_del) + " " + Utility.numberToData(item.getAsLong(Rapportini.DATA_RAPPORTINO)));
        menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
        menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
        menu.add(Menu.NONE, 3, Menu.NONE, getString(R.string.esporta_rapportino_xls));
        menu.add(Menu.NONE, 4, Menu.NONE, getString(R.string.esporta_rapportino_multiplo_xls));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        ContentValues rapportino = (ContentValues) getController().getElementoAllaPosizione(info.position);
        if (item.getItemId() == 1) {
            modifica(rapportino);
        }
        if (item.getItemId() == 2) {
            confermaCancellazione(new Rapportini(), rapportino, true);
        }
        if (item.getItemId() == 3) {
            esportaRapportino(rapportino.getAsInteger(Rapportini.ID), false);
        }
        if (item.getItemId() == 4) {
            esportaRapportino(rapportino.getAsInteger(Rapportini.ID), true);
        }
        return super.onContextItemSelected(item);
    }

    /** Azioni del rapportino (pulsante a icona in fondo alla riga; le stesse del menu a pressione lunga). */
    private void mostraAzioni(final ContentValues rapportino) {
        String[] azioni = {getString(R.string.modifica), getString(R.string.elimina),
                getString(R.string.esporta_rapportino_xls), getString(R.string.esporta_rapportino_multiplo_xls)};
        Utility.mostraSelezioneDialog(getString(R.string.rapportino_del) + " " + Utility.numberToData(rapportino.getAsLong(Rapportini.DATA_RAPPORTINO)),
                azioni, this, (dialog, i) -> {
                    if (i == 0) modifica(rapportino);
                    if (i == 1) confermaCancellazione(new Rapportini(), rapportino, true);
                    if (i == 2) esportaRapportino(rapportino.getAsInteger(Rapportini.ID), false);
                    if (i == 3) esportaRapportino(rapportino.getAsInteger(Rapportini.ID), true);
                });
    }

    /** Cantiere del rapportino; senza cantiere l'ordine (cantiere e ordine sono facoltativi). */
    private static String cantiereOOrdine(ContentValues riga) {
        String cantiere = testo(riga, "nome_cantiere");
        if (!cantiere.isEmpty() || riga.getAsString("numero_ordine") == null) {
            return cantiere;
        }
        return "Ord. " + riga.getAsString("numero_ordine") + " " + testo(riga, Preventivi.TITOLO);
    }

    private static String testo(ContentValues riga, String campo) {
        String valore = riga.getAsString(campo);
        return valore != null ? valore : "";
    }

    /** Query, colonne e azione di click riga specifiche del modulo Rapportini. */
    private class RapportiniDefinition extends EconListaStandardDefinition {

        @Override
        public QueryPagina costruisciQuery(String testoFiltro, boolean ordinaPerRecenti) {
            // cliente, cantiere e ordine del rapportino sono facoltativi (almeno uno): tutti in left join
            String fromJoin = Rapportini.NOME_TABELLA
                    + " left join " + Preventivi.NOME_TABELLA + " on " + Rapportini.NOME_TABELLA + "." + Rapportini.ID_ORDINE
                    + "=" + Preventivi.NOME_TABELLA + "." + Preventivi.ID_PREVENTIVO
                    + " left join " + Utenti.NOME_TABELLA + " on " + Rapportini.NOME_TABELLA + "." + Rapportini.ID_UTENTE_DITTA
                    + "=" + Utenti.NOME_TABELLA + "." + Utenti.ID_UTENTE_DITTA
                    + " left join " + Cantieri.NOME_TABELLA + " on " + Cantieri.NOME_TABELLA + "." + Cantieri.ID_CANTIERE
                    + "=" + Rapportini.NOME_TABELLA + "." + Rapportini.ID_CANTIERE
                    + " left join " + Anagrafica.NOME_TABELLA + " on " + Anagrafica.NOME_TABELLA + "." + Anagrafica.ID_ANAGRAFICA
                    + "=(case when " + Rapportini.NOME_TABELLA + "." + Rapportini.ID_CLIENTE + ">0 then " + Rapportini.NOME_TABELLA + "."
                    + Rapportini.ID_CLIENTE + " else " + Cantieri.NOME_TABELLA + "." + Cantieri.ID_ANAGRAFICA + " end)";

            String select = Rapportini.NOME_TABELLA + ".*, " + Anagrafica.NOME_TABELLA + "." + Anagrafica.RAGIONE_SOCIALE + ", "
                    // alias: "numero" e' quello del rapportino (NUMERAZIONE_DOCUMENTI.md), non dell'ordine
                    + Preventivi.NOME_TABELLA + "." + Preventivi.NUMERO + " as numero_ordine, "
                    + Preventivi.NOME_TABELLA + "." + Preventivi.TITOLO + ", "
                    + Preventivi.NOME_TABELLA + "." + Preventivi.DATA + " as data_ordine, "
                    + Cantieri.NOME_TABELLA + "." + Cantieri.NOME + " as nome_cantiere, "
                    + "(select sum(" + RapportiniDettaglio.sqlOreUomo("d", "t", "u") + ") from " + RapportiniDettaglio.NOME_TABELLA + " d"
                    + " inner join " + RapportiniDettaglioTipi.NOME_TABELLA + " t on t." + RapportiniDettaglioTipi.ID + "=d." + RapportiniDettaglio.ID_TIPO
                    + " left join " + UnitaMisura.NOME_TABELLA + " u on u." + UnitaMisura.ID + "=d." + RapportiniDettaglio.ID_UNITA_MISURA
                    + " where d." + RapportiniDettaglio.ID_RAPPORTINO + "=" + Rapportini.NOME_TABELLA + "." + Rapportini.ID + ") as tot_ore, "
                    + "(select " + StatiDocumento.NOME + " from " + StatiDocumento.NOME_TABELLA + " sd where sd." + StatiDocumento.ID + "="
                    + Rapportini.NOME_TABELLA + "." + Rapportini.ID_STATO + ") as nome_stato, "
                    + "(select " + StatiDocumento.COLORE + " from " + StatiDocumento.NOME_TABELLA + " sd where sd." + StatiDocumento.ID + "="
                    + Rapportini.NOME_TABELLA + "." + Rapportini.ID_STATO + ") as colore_stato, "
                    + Utenti.NOME_TABELLA + "." + Utenti.NOME + " as nome_operatore, "
                    + Utenti.NOME_TABELLA + "." + Utenti.COGNOME + " as cognome_operatore";

            // coalesce: con i left join cliente o cantiere possono mancare, e NULL like '%' nasconderebbe il rapportino
            QueryPagina like = FiltriHelper.likeMultiCampo(fromJoin, testoFiltro,
                    "coalesce(" + Anagrafica.NOME_TABELLA + "." + Anagrafica.RAGIONE_SOCIALE + ",'')",
                    "coalesce(" + Cantieri.NOME_TABELLA + "." + Cantieri.NOME + ",'')");

            String periodo = spinnerPeriodo.getValue();
            String where = "(" + like.whereSql + ")";
            if (!periodo.equals("")) {
                Calendar cal = Calendar.getInstance();
                int giorni = 0;
                if (periodo.equals("S")) giorni = -7;
                if (periodo.equals("M")) giorni = -30;
                if (periodo.equals("3M")) giorni = -90;
                if (periodo.equals("6M")) giorni = -180;
                if (periodo.equals("A")) giorni = -365;
                cal.add(Calendar.DATE, giorni);
                long dataNumber = Utility.dataToNumber(cal);
                where += " and " + Rapportini.NOME_TABELLA + "." + Rapportini.DATA_RAPPORTINO + ">=" + dataNumber;
            }
            where += " and " + Rapportini.NOME_TABELLA + "." + Rapportini.ID_DITTA + "=" + Sessione.getDittaSelezionata();

            return new QueryPagina(select, fromJoin, where, like.whereArgs);
        }

        @Override
        public List<ColonnaLista> getColonne() {
            String rapp = Rapportini.NOME_TABELLA + ".";
            // una riga sola come Clienti e Cantieri, leggibile anche dal telefono: lo stato e' un'icona nel suo colore
            return Arrays.asList(
                    ColonnaLista.icona("nome_stato", "Stato", 7, FaIcone.STATO,
                            riga -> RegoleRapportino.coloreStato(riga.getAsString("colore_stato")), rapp + Rapportini.ID_STATO),
                    new ColonnaLista(Rapportini.NUMERO, "N.", 11, false, Rapportini::numeroDocumento,
                            "(" + rapp + Rapportini.ANNO + "*1000000+" + rapp + Rapportini.NUMERO + ")"),
                    new ColonnaLista(Rapportini.DATA_RAPPORTINO, "Data", 13, false,
                            riga -> Utility.numberToDataShort(riga.getAsLong(Rapportini.DATA_RAPPORTINO)), rapp + Rapportini.DATA_RAPPORTINO),
                    new ColonnaLista(Anagrafica.RAGIONE_SOCIALE, "Cliente", 23, true, null,
                            Anagrafica.NOME_TABELLA + "." + Anagrafica.RAGIONE_SOCIALE),
                    new ColonnaLista("nome_cantiere", "Cantiere", 21, false, RapportiniActivity::cantiereOOrdine, "nome_cantiere"),
                    new ColonnaLista("cognome_operatore", "Operatore", 16, false,
                            riga -> testo(riga, "nome_operatore") + " " + testo(riga, "cognome_operatore"), "cognome_operatore"),
                    ColonnaLista.ore("tot_ore", "Ore", 9, "tot_ore"));
        }

        @Override
        public AzioneRiga getAzioneRiga(EconListaStandardController.Host host) {
            return new AzioneRiga() {
                @Override
                public String glifo() {
                    return FaIcone.AZIONI;
                }

                @Override
                public String tooltip() {
                    return getString(R.string.opzioni);
                }

                @Override
                public void onClick(ContentValues riga) {
                    mostraAzioni(riga);
                }
            };
        }

        @Override
        public void onRigaClick(EconListaStandardController.Host host, ContentValues riga) {
            Intent intent = new Intent(host.getContext(), RapportinoDettaglioModActivity.class);
            intent.putExtra("ID", riga.getAsInteger(Rapportini.ID));
            ((EconActivity) host.getContext()).apriFinestraModifica(intent, 1);
        }

        @Override
        public String getColonnaOrdinamentoDefault() {
            return Rapportini.DATA_RAPPORTINO;
        }

        @Override
        public boolean isOrdinamentoDefaultDiscendente() {
            return true;
        }

        @Override
        public String getOrdineRecenti() {
            return Rapportini.NOME_TABELLA + "." + Rapportini.DATA_MOD + " is null asc, "
                    + Rapportini.NOME_TABELLA + "." + Rapportini.DATA_MOD + " desc, "
                    + Rapportini.NOME_TABELLA + "." + Rapportini.DATA_INS + " desc";
        }

        @Override
        public String getTitolo() {
            return "Rapportini";
        }

        @Override
        public String funzionalitaNuovo() {
            return com.ncfsistemi.econ.utils.FunzionalitaApp.RAPPORTINI_CREA;
        }

        @Override
        public void onNuovoClick(EconListaStandardController.Host host) {
            // I cantieri ammessi dipendono dalla data scelta nel rapportino: il controllo e' nella testata
            Intent intent = new Intent(host.getContext(), RapportinoDettaglioModActivity.class);
            ((EconActivity) host.getContext()).apriFinestraInserimento(intent, 1, new Rapportini());
        }
    }
}
