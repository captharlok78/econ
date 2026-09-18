package pfa.app.econtab.lista;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;

/**
 * Comportamento comune ai moduli "lista standard" (Clienti e, in seguito, gli altri):
 * form filtri visibile di default (nessun caricamento finche' non si preme Cerca/Reset/
 * Ultimi N), paginazione SQL (LIMIT/OFFSET), testata tabellare ordinabile per colonna,
 * righe alternate. Un modulo fornisce solo una EConTabListaStandardDefinition (query,
 * colonne, azione di click riga); questa classe fa tutto il resto, cosi' una modifica qui
 * si propaga a tutti i moduli senza duplicare codice.
 *
 * Non e' una Activity/Fragment: comunica col suo contenitore solo tramite Host, il che
 * permette di condividere la stessa logica sia da una Activity diretta (Clienti) sia da un
 * Fragment incorporato altrove (es. Cantieri dentro ClientiDettaglioActivity).
 */
public class EConTabListaStandardController {

    public interface Host {
        Context getContext();
        <T extends View> T findViewById(int id);
        void startActivity(Intent intent);
    }

    private final Host host;
    private final EConTabListaStandardDefinition definizione;

    private View cardFiltri;
    private LinearLayout headerTabella;
    private View headerBorder;
    private View barraPaginazione;
    private TextView textViewPaginaInfo;
    private TextView textViewPaginaInfo2;
    private TextView textViewNessunDato;
    private EditText editTextRisultatiPerPagina;
    private EditText filtro;
    private ListView lista;

    private final Map<String, TextView[]> frecceOrdinamento = new HashMap<>();

    private final ArrayList<Object> dati = new ArrayList<>();
    private EConTabListaStandardAdapter adapter;

    private boolean ricercaAttiva = false;
    private boolean ordinaPerRecenti = false;
    private String colonnaOrdinamento;
    private boolean ordineDiscendente = false;
    private int risultatiPerPagina;
    private int paginaCorrente = 0;
    private int totalePagine = 1;
    private int totaleRisultati = 0;

    public EConTabListaStandardController(Host host, EConTabListaStandardDefinition definizione) {
        this.host = host;
        this.definizione = definizione;
        this.risultatiPerPagina = definizione.getRisultatiPerPaginaDefault();
        this.colonnaOrdinamento = definizione.getColonnaOrdinamentoDefault();
    }

    /** Da chiamare in onCreate/onCreateView, dopo che il layout e' stato impostato. */
    public void inizializza() {
        lista = host.findViewById(R.id.lista);
        textViewNessunDato = host.findViewById(R.id.textViewNessunDato);

        cardFiltri = host.findViewById(R.id.cardFiltri);
        headerTabella = host.findViewById(R.id.headerTabella);
        headerBorder = host.findViewById(R.id.headerBorder);
        barraPaginazione = host.findViewById(R.id.barraPaginazione);
        textViewPaginaInfo = host.findViewById(R.id.textViewPaginaInfo);
        textViewPaginaInfo2 = host.findViewById(R.id.textViewPaginaInfo2);

        editTextRisultatiPerPagina = host.findViewById(R.id.editTextRisultatiPerPagina);
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

        filtro = host.findViewById(R.id.editText_filtra);
        filtro.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                eseguiRicerca(false);
                return true;
            }
            return false;
        });

        View buttonCerca = host.findViewById(R.id.buttonCerca);
        if (buttonCerca != null) {
            buttonCerca.setOnClickListener(v -> eseguiRicerca(false));
        }
        View buttonResetFiltri = host.findViewById(R.id.buttonResetFiltri);
        if (buttonResetFiltri != null) {
            buttonResetFiltri.setOnClickListener(v -> {
                filtro.setText("");
                eseguiRicerca(false);
            });
        }
        View buttonUltimiGestiti = host.findViewById(R.id.buttonUltimiGestiti);
        if (buttonUltimiGestiti != null) {
            buttonUltimiGestiti.setVisibility(definizione.getOrdineRecenti() != null ? View.VISIBLE : View.GONE);
            buttonUltimiGestiti.setOnClickListener(v -> {
                filtro.setText("");
                eseguiRicerca(true);
            });
        }

        costruisciTestata();
        mostraSoloForm();
    }

    private void costruisciTestata() {
        headerTabella.removeAllViews();
        for (ColonnaLista colonna : definizione.getColonne()) {
            LinearLayout cellaHeader = new LinearLayout(host.getContext());
            cellaHeader.setOrientation(LinearLayout.HORIZONTAL);
            cellaHeader.setGravity(Gravity.CENTER_VERTICAL);
            cellaHeader.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, colonna.peso));
            cellaHeader.setClickable(true);
            cellaHeader.setFocusable(true);
            TypedValue sfondoSelezionabile = new TypedValue();
            host.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, sfondoSelezionabile, true);
            cellaHeader.setBackgroundResource(sfondoSelezionabile.resourceId);

            LinearLayout frecce = new LinearLayout(host.getContext());
            frecce.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams paramsFrecce = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            paramsFrecce.setMarginEnd(dpToPx(6));
            frecce.setLayoutParams(paramsFrecce);

            TextView frecciaSu = creaFreccia("▲");
            TextView frecciaGiu = creaFreccia("▼");
            frecce.addView(frecciaSu);
            frecce.addView(frecciaGiu);
            cellaHeader.addView(frecce);

            TextView label = new TextView(host.getContext());
            label.setText(colonna.label);
            label.setTextSize(12);
            label.setTypeface(Typeface.DEFAULT_BOLD);
            label.setTextColor(Color.parseColor("#444444"));
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            cellaHeader.addView(label);

            cellaHeader.setOnClickListener(v -> ordinaPerColonna(colonna.campo));

            headerTabella.addView(cellaHeader);
            frecceOrdinamento.put(colonna.campo, new TextView[]{frecciaSu, frecciaGiu});
        }
    }

    private TextView creaFreccia(String testo) {
        TextView freccia = new TextView(host.getContext());
        freccia.setText(testo);
        freccia.setTextSize(7);
        freccia.setTextColor(0xFFBBBBBB);
        return freccia;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * host.getContext().getResources().getDisplayMetrics().density);
    }

    /** Pulsante filtro nella barra in alto: apre/richiude la form filtri. */
    public void toggleFiltri() {
        cardFiltri.setVisibility(cardFiltri.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    /** Stato di apertura del modulo: solo la form filtri, nessun caricamento dati. */
    public void mostraSoloForm() {
        ricercaAttiva = false;
        cardFiltri.setVisibility(View.VISIBLE);
        headerTabella.setVisibility(View.GONE);
        headerBorder.setVisibility(View.GONE);
        barraPaginazione.setVisibility(View.GONE);
        textViewNessunDato.setVisibility(View.GONE);
        popolaLista(new ArrayList<>());
    }

    /**
     * Cerca/Reset filtri/Ultimi N/invio: nasconde la form e mostra i risultati paginati.
     * Il caricamento dati avviene solo qui.
     */
    public void eseguiRicerca(boolean ordinaPerRecenti) {
        nascondiTastiera(filtro);
        this.ordinaPerRecenti = ordinaPerRecenti;
        ricercaAttiva = true;
        paginaCorrente = 0;
        cardFiltri.setVisibility(View.GONE);
        caricaPagina();
    }

    /** Cambio del numero di risultati per pagina: ricalcola pagine e ricarica mantenendo il filtro. */
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
            caricaPagina();
        }
    }

    private void nascondiTastiera(View v) {
        InputMethodManager imm = (InputMethodManager) host.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    /** Click su una colonna della testata: ordina per quel campo (ASC), o inverte la direzione se e' gia' attiva. */
    public void ordinaPerColonna(String campo) {
        if (!ricercaAttiva) {
            return;
        }
        if (campo.equals(colonnaOrdinamento)) {
            ordineDiscendente = !ordineDiscendente;
        } else {
            colonnaOrdinamento = campo;
            ordineDiscendente = false;
        }
        ordinaPerRecenti = false;
        paginaCorrente = 0;
        caricaPagina();
    }

    /** Query paginata (risultatiPerPagina) con il filtro e l'ordinamento correnti. */
    public void caricaPagina() {
        String testoFiltro = filtro.getText().toString().trim();
        QueryPagina query = definizione.costruisciQuery(testoFiltro, ordinaPerRecenti);

        String ordineSql = (ordinaPerRecenti && definizione.getOrdineRecenti() != null)
                ? definizione.getOrdineRecenti()
                : colonnaOrdinamento + (ordineDiscendente ? " desc" : " asc");

        DbInterno db = new DbInterno(host.getContext());
        DbInterno.PaginaRisultati paginaRisultati = db.eseguiSelectPaginato(
                query.fromJoinSql, query.whereSql, query.whereArgs, ordineSql,
                risultatiPerPagina, paginaCorrente * risultatiPerPagina);
        db.close();

        totaleRisultati = paginaRisultati.totaleRisultati;
        totalePagine = Math.max(1, (int) Math.ceil(totaleRisultati / (double) risultatiPerPagina));
        if (paginaCorrente >= totalePagine) {
            paginaCorrente = totalePagine - 1;
            caricaPagina();
            return;
        }

        popolaLista(paginaRisultati.righe);
        textViewNessunDato.setVisibility(paginaRisultati.righe.isEmpty() ? View.VISIBLE : View.GONE);
        headerTabella.setVisibility(View.VISIBLE);
        headerBorder.setVisibility(View.VISIBLE);
        aggiornaFrecceOrdinamento();
        aggiornaBarraPaginazione();
    }

    private void popolaLista(ArrayList<Object> elems) {
        dati.clear();
        dati.addAll(elems);

        if (adapter == null) {
            adapter = new EConTabListaStandardAdapter(host.getContext(), dati, definizione.getColonne());
            lista.setAdapter(adapter);
            lista.setOnItemClickListener((parent, view, position, id) ->
                    definizione.onRigaClick(host, (ContentValues) dati.get(position)));
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    /** Colora le frecce ▲▼ della testata in base alla colonna/direzione di ordinamento attiva. */
    private void aggiornaFrecceOrdinamento() {
        final int coloreAttivo = 0xFF1565C0;
        final int coloreInattivo = 0xFFBBBBBB;
        for (Map.Entry<String, TextView[]> voce : frecceOrdinamento.entrySet()) {
            boolean colonnaAttiva = !ordinaPerRecenti && voce.getKey().equals(colonnaOrdinamento);
            voce.getValue()[0].setTextColor(colonnaAttiva && !ordineDiscendente ? coloreAttivo : coloreInattivo);
            voce.getValue()[1].setTextColor(colonnaAttiva && ordineDiscendente ? coloreAttivo : coloreInattivo);
        }
    }

    private void aggiornaBarraPaginazione() {
        barraPaginazione.setVisibility(View.VISIBLE);
        textViewPaginaInfo.setText("Pagina " + (paginaCorrente + 1) + " di " + totalePagine + "  ·  ");
        textViewPaginaInfo2.setText("  per pagina  ·  " + totaleRisultati + " risultati trovati");
        if (!editTextRisultatiPerPagina.isFocused()) {
            editTextRisultatiPerPagina.setText(String.valueOf(risultatiPerPagina));
        }
        View buttonPrec = host.findViewById(R.id.buttonPaginaPrec);
        buttonPrec.setEnabled(paginaCorrente > 0);
        buttonPrec.setAlpha(paginaCorrente > 0 ? 1f : 0.3f);
        View buttonSucc = host.findViewById(R.id.buttonPaginaSucc);
        buttonSucc.setEnabled(paginaCorrente < totalePagine - 1);
        buttonSucc.setAlpha(paginaCorrente < totalePagine - 1 ? 1f : 0.3f);
    }

    public void paginaPrecedente() {
        if (paginaCorrente > 0) {
            paginaCorrente--;
            caricaPagina();
        }
    }

    public void paginaSuccessiva() {
        if (paginaCorrente < totalePagine - 1) {
            paginaCorrente++;
            caricaPagina();
        }
    }

    /** Da chiamare da aggiornaDopoCancellazione(): ricarica la pagina corrente se una ricerca e' attiva. */
    public void ricaricaSeAttiva() {
        if (ricercaAttiva) {
            caricaPagina();
        }
    }

    public ListView getListView() {
        return lista;
    }

    public Object getElementoAllaPosizione(int position) {
        return dati.get(position);
    }
}
