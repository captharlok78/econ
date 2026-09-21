package pfa.app.econtab;

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
import java.util.Calendar;

import pfa.app.econtab.adapters.EConTabListViewAdapter;
import pfa.app.econtab.adapters.RapportiniAdapter;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.Rapportini;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.db.table.Utenti;
import pfa.app.econtab.export.RapportinoXLS;
import pfa.app.econtab.lista.EConTabListaStandardActivity;
import pfa.app.econtab.lista.EConTabListaStandardController;
import pfa.app.econtab.lista.EConTabListaStandardDefinition;
import pfa.app.econtab.lista.FiltriHelper;
import pfa.app.econtab.lista.QueryPagina;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabSpinner;

/**
 * Modulo Rapportini sullo standard di ricerca/lista condiviso (vedi pfa.app.econtab.lista).
 * Il filtro "periodo" e' un range di date calcolato (non un like/OR su colonne), diverso
 * dagli altri moduli: resta logica interamente custom in RapportiniDefinition. Le righe
 * hanno pulsanti di azione con logica di business (cambia stato non qui, ma azioni/
 * duplica) troppo custom per il modello dichiarativo: usa il proprio RapportiniAdapter
 * (via creaAdapterPersonalizzato), come Preventivi/Ordini.
 */
public class RapportiniActivity extends EConTabListaStandardActivity {

    private EConTabSpinner spinnerPeriodo;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_rapportini;
    }

    @Override
    protected EConTabListaStandardDefinition creaDefinizione() {
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
        spinnerPeriodo.setValue("S");
        spinnerPeriodo.setValoriSpinnerLibero(valoriPeriodo);
    }

    private ContentValues vocePeriodo(String valore, String descrizione) {
        ContentValues val = new ContentValues();
        val.put(EConTabSpinner.VALORE, valore);
        val.put(EConTabSpinner.DESCRIZIONE, descrizione);
        return val;
    }

    /** Compat: chiamato da RapportiniAdapter dopo Elimina/duplica. */
    public void ricerca() {
        getController().ricaricaSeAttiva();
    }

    public void modifica(ContentValues rapportino) {
        Intent intent = new Intent(this, RapportinoDettaglioModActivity.class);
        intent.putExtra("ID", rapportino.getAsInteger(Rapportini.ID_RAPPORTINO));
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
                    intent.setDataAndType(Uri.fromFile(fileExport), Utility.XLS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
            esportaRapportino(rapportino.getAsInteger(Rapportini.ID_RAPPORTINO), false);
        }
        if (item.getItemId() == 4) {
            esportaRapportino(rapportino.getAsInteger(Rapportini.ID_RAPPORTINO), true);
        }
        return super.onContextItemSelected(item);
    }

    /** Query, adapter e azione di click riga specifiche del modulo Rapportini. */
    private class RapportiniDefinition extends EConTabListaStandardDefinition {

        @Override
        public QueryPagina costruisciQuery(String testoFiltro, boolean ordinaPerRecenti) {
            String fromJoin = Rapportini.NOME_TABELLA
                    + " inner join " + Preventivi.NOME_TABELLA + " on " + Rapportini.NOME_TABELLA + "." + Rapportini.ID_ORDINE
                    + "=" + Preventivi.NOME_TABELLA + "." + Preventivi.ID_PREVENTIVO
                    + " inner join " + Utenti.NOME_TABELLA + " on " + Rapportini.NOME_TABELLA + "." + Rapportini.ID_OPERATORE
                    + "=" + Utenti.NOME_TABELLA + "." + Utenti.ID_UTENTE
                    + " inner join " + Cantieri.NOME_TABELLA + " on " + Preventivi.NOME_TABELLA + "." + Preventivi.ID_CANTIERE
                    + "=" + Cantieri.NOME_TABELLA + "." + Cantieri.ID_CANTIERE
                    + " inner join " + Anagrafica.NOME_TABELLA + " on " + Cantieri.NOME_TABELLA + "." + Cantieri.ID_ANAGRAFICA
                    + "=" + Anagrafica.NOME_TABELLA + "." + Anagrafica.ID_ANAGRAFICA;

            String select = Rapportini.NOME_TABELLA + ".*, " + Anagrafica.NOME_TABELLA + "." + Anagrafica.RAGIONE_SOCIALE + ", "
                    + Preventivi.NOME_TABELLA + "." + Preventivi.NUMERO + ", "
                    + Preventivi.NOME_TABELLA + "." + Preventivi.TITOLO + ", "
                    + Preventivi.NOME_TABELLA + "." + Preventivi.DATA + " as data_ordine, "
                    + Cantieri.NOME_TABELLA + "." + Cantieri.NOME + " as nome_cantiere, "
                    + "(select sum(" + RapportiniDettaglio.ORE + ") from " + RapportiniDettaglio.NOME_TABELLA + " where "
                    + RapportiniDettaglio.NOME_TABELLA + "." + RapportiniDettaglio.ID_RAPPORTINO + "=" + Rapportini.NOME_TABELLA + "."
                    + Rapportini.ID_RAPPORTINO + ") as tot_ore, "
                    + Utenti.NOME_TABELLA + "." + Utenti.NOME + " as nome_operatore, "
                    + Utenti.NOME_TABELLA + "." + Utenti.COGNOME + " as cognome_operatore";

            QueryPagina like = FiltriHelper.likeMultiCampo(fromJoin, testoFiltro, Anagrafica.NOME_TABELLA + "." + Anagrafica.RAGIONE_SOCIALE);

            String periodo = spinnerPeriodo.getValue();
            String where = like.whereSql;
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
        public boolean isTabellare() {
            return false;
        }

        @Override
        public EConTabListViewAdapter creaAdapterPersonalizzato(android.content.Context context, ArrayList<Object> dati) {
            return new RapportiniAdapter(context, dati, R.layout.list_item_rapportino);
        }

        @Override
        public void onRigaClick(EConTabListaStandardController.Host host, ContentValues riga) {
            Intent intent = new Intent(host.getContext(), RapportinoDettaglioModActivity.class);
            intent.putExtra("ID", riga.getAsInteger(Rapportini.ID_RAPPORTINO));
            ((EConTabActivity) host.getContext()).apriFinestraModifica(intent, 1);
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
        public void onNuovoClick(EConTabListaStandardController.Host host) {
            // Un rapportino si apre su un ordine aperto: senza, non si puo' aprirne nessuno
            DbInterno db = new DbInterno(host.getContext());
            boolean ordiniAperti = new Preventivi().esistonoOrdiniAperti(db);
            db.close();
            if (!ordiniAperti) {
                Utility.mostraDialog(host.getContext().getString(R.string.attenzione),
                        host.getContext().getString(R.string.nessun_ordine_aperto_rapportino), host.getContext(), "OK");
                return;
            }
            Intent intent = new Intent(host.getContext(), RapportinoDettaglioModActivity.class);
            ((EConTabActivity) host.getContext()).apriFinestraInserimento(intent, 1, new Rapportini());
        }
    }
}
