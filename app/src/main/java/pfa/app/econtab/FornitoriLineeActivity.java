package pfa.app.econtab;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import pfa.app.econtab.adapters.FornitoriLineeAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;

/**
 * Modulo Fornitori/Linee (Listini, livello 1): resta su ExpandableListView/adapter
 * raggruppato proprio (i gruppi sono Costruttori, non righe piatte), quindi non usa
 * EConTabListaStandardController/Definition come gli altri moduli - adotta comunque la
 * stessa testata standard e lo stesso stile di form filtri/paginazione, paginando per
 * Costruttore (gruppo, come deciso in precedenza), non per riga.
 */
public class FornitoriLineeActivity extends EConTabActivity implements OnChildClickListener {

    private static final int RISULTATI_PER_PAGINA_DEFAULT = 20;

    private ExpandableListView lista = null;
    private View cardFiltri = null;
    private View barraPaginazione = null;
    private TextView textViewPaginaInfo = null;
    private TextView textViewPaginaInfo2 = null;
    private TextView textViewNessunDato = null;
    private EditText editTextRisultatiPerPagina = null;
    private EditText filtro = null;

    private HashMap<String, ArrayList<Object>> listChildData = null;
    private ArrayList<Object> sezioni = null;
    private ArrayList<String> sezioniCodici = null;

    private FornitoriLineeAdapter adapter = null;

    private boolean ricercaAttiva = false;
    private boolean ordinaPerRecenti = false;
    private int risultatiPerPagina = RISULTATI_PER_PAGINA_DEFAULT;
    private int paginaCorrente = 0;
    private int totalePagine = 1;
    private int totaleCostruttori = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fornitori_linee);

        lista = findViewById(R.id.lista);
        lista.setOnChildClickListener(this);
        textViewNessunDato = findViewById(R.id.textViewNessunDato);
        cardFiltri = findViewById(R.id.cardFiltri);
        barraPaginazione = findViewById(R.id.barraPaginazione);
        textViewPaginaInfo = findViewById(R.id.textViewPaginaInfo);
        textViewPaginaInfo2 = findViewById(R.id.textViewPaginaInfo2);
        registerForContextMenu(lista);

        ((TextView) findViewById(R.id.headerTitolo)).setText("Listini");
        findViewById(R.id.buttonToggleFiltri).setOnClickListener(v ->
                cardFiltri.setVisibility(cardFiltri.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        findViewById(R.id.buttonnuovo).setOnClickListener(v -> {
            Intent intent = new Intent(this, CostruttoriDettaglioActivity.class);
            apriFinestraInserimento(intent, 1, new Costruttori());
        });

        editTextRisultatiPerPagina = findViewById(R.id.editTextRisultatiPerPagina);
        editTextRisultatiPerPagina.setText(String.valueOf(risultatiPerPagina));
        editTextRisultatiPerPagina.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                applicaRisultatiPerPagina();
            }
        });
        editTextRisultatiPerPagina.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                nascondiTastiera(editTextRisultatiPerPagina);
                applicaRisultatiPerPagina();
                return true;
            }
            return false;
        });

        filtro = findViewById(R.id.editText_filtra);
        filtro.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                eseguiRicerca(false);
                return true;
            }
            return false;
        });
        findViewById(R.id.buttonCerca).setOnClickListener(v -> eseguiRicerca(false));
        findViewById(R.id.buttonResetFiltri).setOnClickListener(v -> {
            filtro.setText("");
            eseguiRicerca(false);
        });
        findViewById(R.id.buttonUltimiGestiti).setOnClickListener(v -> {
            filtro.setText("");
            eseguiRicerca(true);
        });
        findViewById(R.id.buttonPaginaPrec).setOnClickListener(v -> paginaPrecedente());
        findViewById(R.id.buttonPaginaSucc).setOnClickListener(v -> paginaSuccessiva());
    }

    /**
     * sezioni/sezioniCodici/listChildData vengono creati una sola volta (qui) e poi solo
     * svuotati/ripopolati in place: l'adapter (FornitoriLineeAdapter/EConTabExpListViewAdapter)
     * riceve questi riferimenti UNA volta nel costruttore e non li rilegge piu' - se venissero
     * sostituiti con nuovi oggetti l'adapter continuerebbe a mostrare i vecchi (vuoti).
     */
    private void inizializzaStruttureSeServe() {
        if (sezioni == null) {
            sezioni = new ArrayList<>();
            sezioniCodici = new ArrayList<>();
            listChildData = new HashMap<>();
        }
    }

    /** Stato di apertura del modulo: solo la form filtri, nessun caricamento dati. */
    private void mostraSoloForm() {
        ricercaAttiva = false;
        cardFiltri.setVisibility(View.VISIBLE);
        barraPaginazione.setVisibility(View.GONE);
        textViewNessunDato.setVisibility(View.GONE);
        inizializzaStruttureSeServe();
        sezioni.clear();
        sezioniCodici.clear();
        listChildData.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void eseguiRicerca(boolean ordinaPerRecenti) {
        nascondiTastiera(filtro);
        ricercaAttiva = true;
        this.ordinaPerRecenti = ordinaPerRecenti;
        paginaCorrente = 0;
        cardFiltri.setVisibility(View.GONE);
        ricerca();
    }

    private void applicaRisultatiPerPagina() {
        String testo = editTextRisultatiPerPagina.getText().toString().trim();
        int nuovoValore;
        try {
            nuovoValore = Integer.parseInt(testo);
        } catch (NumberFormatException e) {
            nuovoValore = 0;
        }
        if (nuovoValore <= 0) {
            editTextRisultatiPerPagina.setText(String.valueOf(risultatiPerPagina));
            return;
        }
        if (nuovoValore == risultatiPerPagina) {
            return;
        }
        risultatiPerPagina = nuovoValore;
        paginaCorrente = 0;
        if (ricercaAttiva) {
            ricerca();
        }
    }

    private void nascondiTastiera(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mostraSoloForm();
    }

    @Override
    protected void aggiornaDopoCancellazione() {
        if (ricercaAttiva) {
            ricerca();
        }
    }

    /** Query paginata per Costruttore (gruppo): la pagina scorre i fornitori, non le righe. */
    private void ricerca() {
        inizializzaStruttureSeServe();
        sezioni.clear();
        sezioniCodici.clear();
        listChildData.clear();

        DbInterno db = new DbInterno(this);
        String testo = getTesto(R.id.editText_filtra);
        String like = testo.isEmpty() ? "%" : "%" + testo + "%";
        String whereSql = Costruttori.RAGIONE_SOCIALE + " like ?";
        String orderBySql = ordinaPerRecenti
                ? Costruttori.NOME_TABELLA + "." + Costruttori.DATA_MOD + " is null asc, "
                        + Costruttori.NOME_TABELLA + "." + Costruttori.DATA_MOD + " desc, "
                        + Costruttori.NOME_TABELLA + "." + Costruttori.DATA_INS + " desc"
                : Costruttori.NOME_TABELLA + "." + Costruttori.RAGIONE_SOCIALE;

        ArrayList<Object> conteggio = db.eseguiSelect(
                "Select count(*) as n from " + Costruttori.NOME_TABELLA + " where " + whereSql, new String[]{like});
        totaleCostruttori = conteggio.isEmpty() ? 0 : ((ContentValues) conteggio.get(0)).getAsInteger("n");
        totalePagine = Math.max(1, (int) Math.ceil(totaleCostruttori / (double) risultatiPerPagina));
        if (paginaCorrente >= totalePagine) {
            paginaCorrente = totalePagine - 1;
        }

        ArrayList<Object> paginaCostruttori = db.eseguiSelect(
                "Select " + Costruttori.ID_COSTRUTTORE + " from " + Costruttori.NOME_TABELLA + " where " + whereSql
                        + " order by " + orderBySql
                        + " limit " + risultatiPerPagina + " offset " + (paginaCorrente * risultatiPerPagina),
                new String[]{like});

        if (!paginaCostruttori.isEmpty()) {
            StringBuilder idsIn = new StringBuilder();
            for (Object o : paginaCostruttori) {
                if (idsIn.length() > 0) {
                    idsIn.append(",");
                }
                idsIn.append(((ContentValues) o).getAsInteger(Costruttori.ID_COSTRUTTORE));
            }

            String SQL = "Select " + Costruttori.NOME_TABELLA + ".*," + Linee.ID_LINEA + "," + Linee.NOME_LINEA + " from "
                    + Costruttori.NOME_TABELLA + " left join " + Linee.NOME_TABELLA + " on " + Costruttori.NOME_TABELLA + "."
                    + Costruttori.ID_COSTRUTTORE + "=" + Linee.NOME_TABELLA + "." + Linee.ID_COSTRUTTORE
                    + " where " + Costruttori.NOME_TABELLA + "." + Costruttori.ID_COSTRUTTORE + " in (" + idsIn + ")"
                    + " order by " + orderBySql;
            ArrayList<Object> listaFornitori = db.eseguiSelect(SQL, null);

            for (Object o : listaFornitori) {
                ContentValues forni = (ContentValues) o;
                String key = forni.getAsString(Costruttori.ID_COSTRUTTORE);
                if (sezioniCodici.contains(key)) {
                    if (forni.getAsString(Linee.ID_LINEA) != null) {
                        listChildData.get(key).add(forni);
                    }
                } else {
                    sezioni.add(forni);
                    sezioniCodici.add(key);
                    ArrayList<Object> linee = new ArrayList<>();
                    if (forni.getAsString(Linee.ID_LINEA) != null) {
                        linee.add(forni);
                    }
                    listChildData.put(key, linee);
                }
            }

            Iterator<String> iterChild = listChildData.keySet().iterator();
            while (iterChild.hasNext()) {
                ArrayList<Object> linee = listChildData.get(iterChild.next());
                ContentValues noLinea = new ContentValues();
                noLinea.put(Linee.ID_LINEA, 0);
                noLinea.put(Linee.NOME_LINEA, getString(R.string.altro_materiale));
                linee.add(noLinea);
            }
        }
        db.close();

        if (adapter == null) {
            adapter = new FornitoriLineeAdapter(this, sezioniCodici, listChildData, R.layout.list_group_fornitore,
                    R.layout.list_item_linea, sezioni);
            lista.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }

        textViewNessunDato.setVisibility(sezioni.isEmpty() ? View.VISIBLE : View.GONE);
        aggiornaBarraPaginazione();
    }

    private void aggiornaBarraPaginazione() {
        barraPaginazione.setVisibility(View.VISIBLE);
        textViewPaginaInfo.setText("Pagina " + (paginaCorrente + 1) + " di " + totalePagine + "  ·  ");
        textViewPaginaInfo2.setText("  fornitori per pagina  ·  " + totaleCostruttori + " fornitori trovati");
        if (!editTextRisultatiPerPagina.isFocused()) {
            editTextRisultatiPerPagina.setText(String.valueOf(risultatiPerPagina));
        }
        View buttonPrec = findViewById(R.id.buttonPaginaPrec);
        buttonPrec.setEnabled(paginaCorrente > 0);
        buttonPrec.setAlpha(paginaCorrente > 0 ? 1f : 0.3f);
        View buttonSucc = findViewById(R.id.buttonPaginaSucc);
        buttonSucc.setEnabled(paginaCorrente < totalePagine - 1);
        buttonSucc.setAlpha(paginaCorrente < totalePagine - 1 ? 1f : 0.3f);
    }

    private void paginaPrecedente() {
        if (paginaCorrente > 0) {
            paginaCorrente--;
            ricerca();
        }
    }

    private void paginaSuccessiva() {
        if (paginaCorrente < totalePagine - 1) {
            paginaCorrente++;
            ricerca();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);

        if (type == 0) {
            int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            ContentValues val = (ContentValues) sezioni.get(group);
            menu.setHeaderTitle(val.getAsString(Costruttori.RAGIONE_SOCIALE));
            menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
            menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
        }

        if (type == 1) {
            int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
            ContentValues val = (ContentValues) listChildData.get(sezioniCodici.get(group)).get(child);
            if (val.getAsInteger(Linee.ID_LINEA) != 0) {
                menu.setHeaderTitle(val.getAsString(Linee.NOME_LINEA));
                menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
                menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        if (type == 0) {
            int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            if (item.getItemId() == 1) {
                Intent intent = new Intent(this, CostruttoriDettaglioActivity.class);
                intent.putExtra("ID", Integer.parseInt(sezioniCodici.get(group)));
                apriFinestraModifica(intent, 1);
            }
            if (item.getItemId() == 2) {
                ContentValues val = new ContentValues();
                val.put(Costruttori.ID_COSTRUTTORE, sezioniCodici.get(group));
                confermaCancellazione(new Costruttori(), val, true);
            }
        }
        if (type == 1) {
            int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
            ContentValues val = (ContentValues) listChildData.get(sezioniCodici.get(group)).get(child);

            if (item.getItemId() == 1) {
                Intent intent = new Intent(this, LineeDettaglioModActivity.class);
                intent.putExtra("ID", val.getAsInteger(Linee.ID_LINEA));
                apriFinestraModifica(intent, 1);
            }
            if (item.getItemId() == 2) {
                confermaCancellazione(new Linee(), val, true);
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onChildClick(ExpandableListView arg0, View arg1, int groupPosition, int childPosition, long arg4) {
        Intent intent = new Intent(this, CodiciArticoliActivity.class);
        ContentValues val = (ContentValues) listChildData.get(sezioniCodici.get(groupPosition)).get(childPosition);
        int idLinea = val.getAsInteger(Linee.ID_LINEA);
        int idCostruttore = Integer.parseInt(sezioniCodici.get(groupPosition));

        intent.putExtra(Linee.ID_LINEA, idLinea);
        intent.putExtra(Costruttori.ID_COSTRUTTORE, idCostruttore);

        startActivity(intent);
        return true;
    }
}
