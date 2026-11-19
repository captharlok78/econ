package com.ncfsistemi.econ.disegno;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.ncfsistemi.econ.R;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finestra a tutto schermo del modo Disegno (STUDIO_DESIGN_GRAFICA.md §18), aperta toccando un punto della piantina:
 * <ul>
 *     <li>in alto le azioni sul punto: aggiungi o togli il quadretto (lo spazio della stanza) e, se si è toccato un
 *     elemento di disegno, ruota/ribalta ed elimina;</li>
 *     <li>la ricerca e, a sinistra, il filtro per gruppo (Aperture, Strutture, Bagno, ...);</li>
 *     <li>la griglia degli elementi di disegno del server con l'anteprima: quelli sul muro si posano se si è toccato vicino
 *     a un muro, quelli a pavimento se si è toccato dentro la stanza. Con un elemento toccato, la scelta lo cambia.</li>
 * </ul>
 */
public final class SceltaDisegno {

    public interface Azioni {
        /** Aggiunge (true) o toglie (false) il quadretto toccato. */
        void quadretto(int cella, boolean aggiungi);

        /** Posa un elemento nel punto toccato. */
        void posa(DisegnoDati.ElementoDisegno e, PiantinaView.PuntoDisegno p);

        /** Sostituisce l'elemento toccato con un altro dello stesso tipo (muro o pavimento). */
        void cambia(DisegnoDati.Apertura a, DisegnoDati.ElementoDisegno e);

        /** Gira di 90° (a pavimento) o ribalta (porta). */
        void ruota(DisegnoDati.Apertura a);

        void elimina(DisegnoDati.Apertura a);
    }

    private static final String TUTTI = "";

    private static final int BLU = 0xFF1565C0, BLU_CHIARO = 0xFFE3F2FD, SFONDO = 0xFFF4F6FA, BORDO = 0xFFDDE3EA,
            TESTO = 0xFF212121, TESTO2 = 0xFF5F6B7A, ARANCIO = 0xFFFB8C00, ROSSO = 0xFFC62828;

    private final Activity act;
    private final float dp;
    private final Typeface fa;
    private final List<DisegnoDati.ElementoDisegno> catalogo = new ArrayList<>();
    private final List<String> gruppi = new ArrayList<>();

    private Dialog dlg;
    private EditText cerca;
    private TextView conteggio;
    private String filtro = TUTTI;
    private BaseAdapter adapterFiltro, adapterGriglia;
    private GridView griglia;
    private final List<DisegnoDati.ElementoDisegno> trovati = new ArrayList<>();

    private PiantinaView.PuntoDisegno punto;
    private int nLocale;

    public SceltaDisegno(Activity act) {
        this.act = act;
        this.dp = act.getResources().getDisplayMetrics().density;
        this.fa = ResourcesCompat.getFont(act, R.font.fa_solid_900);
        Set<String> g = new LinkedHashSet<>();
        for (DisegnoDati.ElementoDisegno e : DisegnoDati.catalogoDisegno(act)) {
            if (!e.attivo) continue;
            catalogo.add(e);
            g.add(e.gruppo.isEmpty() ? "Altro" : e.gruppo);
        }
        gruppi.add(TUTTI);
        gruppi.addAll(g);
    }

    private int px(float v) {
        return Math.round(v * dp);
    }

    /** Posabile nel punto toccato: sul muro se c'è un muro vicino, a pavimento se il punto è dentro la stanza. */
    private boolean posabile(DisegnoDati.ElementoDisegno e) {
        if (punto.esistente != null) return punto.esistente.parete() == e.parete();
        return e.parete() ? punto.cellaParete >= 0 : punto.cella >= 0 && punto.cellaPiena;
    }

    public void apri(PiantinaView.PuntoDisegno p, int n, Azioni azioni) {
        punto = p;
        nLocale = n;
        DisegnoDati.Apertura es = p.esistente;

        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(SFONDO);

        // barra: chiudi, titolo, ricerca, conteggio
        LinearLayout barra = new LinearLayout(act);
        barra.setOrientation(LinearLayout.HORIZONTAL);
        barra.setGravity(Gravity.CENTER_VERTICAL);
        barra.setBackgroundColor(BLU);
        barra.setPadding(px(4), px(6), px(12), px(6));
        barra.setElevation(px(4));
        TextView chiudi = glifo("", Color.WHITE, 20);
        chiudi.setContentDescription("Chiudi");
        chiudi.setOnClickListener(v -> chiudi());
        barra.addView(chiudi, new LinearLayout.LayoutParams(px(48), px(48)));
        TextView titolo = new TextView(act);
        titolo.setText(es != null ? es.elemento.nome : "Disegno");
        titolo.setTextColor(Color.WHITE);
        titolo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        titolo.setTypeface(Typeface.DEFAULT_BOLD);
        titolo.setSingleLine(true);
        titolo.setEllipsize(TextUtils.TruncateAt.END);
        titolo.setMaxWidth(px(260));
        titolo.setPadding(px(4), 0, px(16), 0);
        barra.addView(titolo);

        LinearLayout casella = new LinearLayout(act);
        casella.setOrientation(LinearLayout.HORIZONTAL);
        casella.setGravity(Gravity.CENTER_VERTICAL);
        casella.setBackground(arrotondato(Color.WHITE, 0, 22));
        casella.setPadding(px(14), 0, px(6), 0);
        casella.addView(glifo("", TESTO2, 15), new LinearLayout.LayoutParams(px(22), px(40)));
        cerca = new EditText(act);
        cerca.setHint("Cerca un elemento di disegno");
        cerca.setSingleLine(true);
        cerca.setBackground(null);
        cerca.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        cerca.setTextColor(TESTO);
        cerca.setPadding(px(8), px(6), px(4), px(6));
        cerca.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        cerca.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        casella.addView(cerca, new LinearLayout.LayoutParams(0, px(44), 1f));
        TextView svuota = glifo("", 0xFF90A4AE, 17);
        svuota.setContentDescription("Svuota la ricerca");
        svuota.setVisibility(View.GONE);
        svuota.setOnClickListener(v -> cerca.setText(""));
        casella.addView(svuota, new LinearLayout.LayoutParams(px(36), px(40)));
        barra.addView(casella, new LinearLayout.LayoutParams(0, px(44), 1f));
        conteggio = new TextView(act);
        conteggio.setTextColor(0xDDFFFFFF);
        conteggio.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        conteggio.setPadding(px(14), 0, px(4), 0);
        conteggio.setSingleLine(true);
        barra.addView(conteggio);
        root.addView(barra, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // azioni sul punto toccato: spazio della stanza ed elemento già presente
        LinearLayout azioniRiga = new LinearLayout(act);
        azioniRiga.setOrientation(LinearLayout.HORIZONTAL);
        azioniRiga.setGravity(Gravity.CENTER_VERTICAL);
        azioniRiga.setPadding(px(12), px(8), px(12), px(8));
        if (p.cella >= 0) {
            boolean aggiungi = !p.cellaPiena;
            azioniRiga.addView(pulsante(aggiungi ? "" : "", aggiungi ? "Aggiungi spazio (quadretto)" : "Togli spazio (quadretto)", BLU, v -> {
                chiudi();
                azioni.quadretto(p.cella, aggiungi);
            }));
        }
        if (es != null) {
            boolean porta = es.parete() && "PORTA".equalsIgnoreCase(es.elemento.forma);
            if (!es.parete() || porta) {
                azioniRiga.addView(pulsante(porta ? "" : "", porta ? "Ribalta l'apertura" : "Ruota di 90°", BLU, v -> {
                    chiudi();
                    azioni.ruota(es);
                }));
            }
            azioniRiga.addView(pulsante("", "Elimina " + es.elemento.nome.toLowerCase(), ROSSO, v -> {
                chiudi();
                azioni.elimina(es);
            }));
        }
        TextView spiega = new TextView(act);
        spiega.setTextColor(TESTO2);
        spiega.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        spiega.setPadding(px(12), 0, 0, 0);
        spiega.setText(es != null ? "Tocca un elemento qui sotto per cambiarlo"
                : p.cellaParete >= 0 && p.cellaPiena ? "Vicino al muro: puoi posare elementi sul muro e nella stanza"
                : p.cellaParete >= 0 ? "Vicino al muro: puoi posare elementi sul muro"
                : p.cellaPiena ? "Dentro la stanza: per porte e finestre tocca vicino a un muro"
                : "Fuori dalla stanza: aggiungi lo spazio per usarlo");
        azioniRiga.addView(spiega, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        HorizontalScrollView scorri = new HorizontalScrollView(act);
        scorri.setBackgroundColor(Color.WHITE);
        scorri.setFillViewport(true);
        scorri.addView(azioniRiga);
        root.addView(scorri, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View sepA = new View(act);
        sepA.setBackgroundColor(BORDO);
        root.addView(sepA, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(1)));

        // corpo: gruppi a sinistra, elementi a destra
        LinearLayout corpo = new LinearLayout(act);
        corpo.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(corpo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        ListView listaFiltro = new ListView(act);
        listaFiltro.setDivider(null);
        listaFiltro.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        listaFiltro.setBackgroundColor(Color.WHITE);
        listaFiltro.setPadding(px(8), px(8), px(8), px(8));
        listaFiltro.setClipToPadding(false);
        adapterFiltro = new BaseAdapter() {
            @Override public int getCount() { return gruppi.size(); }
            @Override public Object getItem(int i) { return gruppi.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View cv, ViewGroup parent) { return rigaFiltro(gruppi.get(i), cv); }
        };
        listaFiltro.setAdapter(adapterFiltro);
        listaFiltro.setOnItemClickListener((parent, view, i, id) -> {
            filtro = gruppi.get(i);
            adapterFiltro.notifyDataSetChanged();
            aggiorna();
        });
        int largFiltro = act.getResources().getConfiguration().screenWidthDp < 700 ? 170 : 220;
        corpo.addView(listaFiltro, new LinearLayout.LayoutParams(px(largFiltro), ViewGroup.LayoutParams.MATCH_PARENT));
        View sep = new View(act);
        sep.setBackgroundColor(BORDO);
        corpo.addView(sep, new LinearLayout.LayoutParams(px(1), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout destra = new LinearLayout(act);
        destra.setOrientation(LinearLayout.VERTICAL);
        griglia = new GridView(act);
        griglia.setNumColumns(GridView.AUTO_FIT);
        griglia.setColumnWidth(px(140));
        griglia.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        griglia.setHorizontalSpacing(px(10));
        griglia.setVerticalSpacing(px(10));
        griglia.setPadding(px(12), px(12), px(12), px(12));
        griglia.setClipToPadding(false);
        griglia.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        adapterGriglia = new BaseAdapter() {
            @Override public int getCount() { return trovati.size(); }
            @Override public Object getItem(int i) { return trovati.get(i); }
            @Override public long getItemId(int i) { return trovati.get(i).id; }
            @Override public View getView(int i, View cv, ViewGroup parent) { return casella(trovati.get(i), cv); }
        };
        griglia.setAdapter(adapterGriglia);
        griglia.setOnItemClickListener((parent, view, i, id) -> {
            DisegnoDati.ElementoDisegno e = trovati.get(i);
            if (!posabile(e)) {
                Toast.makeText(act, es != null ? "Si cambia solo con un elemento dello stesso tipo"
                        : e.parete() ? "Per gli elementi sul muro tocca la piantina vicino a un muro"
                        : "Per gli elementi nella stanza tocca dentro la stanza", Toast.LENGTH_SHORT).show();
                return;
            }
            nascondiTastiera();
            chiudi();
            if (es != null) azioni.cambia(es, e);
            else azioni.posa(e, p);
        });
        griglia.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(android.widget.AbsListView v, int stato) {
                if (stato == SCROLL_STATE_TOUCH_SCROLL) nascondiTastiera();
            }
            @Override public void onScroll(android.widget.AbsListView v, int a, int b, int c) { }
        });
        destra.addView(griglia, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        TextView vuoto = new TextView(act);
        vuoto.setTextColor(TESTO2);
        vuoto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        vuoto.setGravity(Gravity.CENTER);
        vuoto.setPadding(px(16), px(32), px(16), px(16));
        vuoto.setText("Nessun elemento trovato");
        destra.addView(vuoto, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        griglia.setEmptyView(vuoto);
        corpo.addView(destra, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        cerca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) { }
            @Override public void afterTextChanged(Editable e) {
                svuota.setVisibility(e.length() > 0 ? View.VISIBLE : View.GONE);
                aggiorna();
            }
        });
        cerca.setOnEditorActionListener((v, azione, ev) -> {
            nascondiTastiera();
            return true;
        });

        dlg = new Dialog(act, R.style.AppTheme); // tema dell'app: campi e pulsanti come nel resto (STUDIO_MODALI_APP.md)
        dlg.setContentView(root);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dlg.getWindow().setStatusBarColor(BLU);
            dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
        aggiorna();
        dlg.show();
    }

    public void chiudi() {
        if (dlg != null && dlg.isShowing()) dlg.dismiss();
        dlg = null;
    }

    /** Elementi del filtro e della ricerca: prima quelli posabili nel punto toccato, nell'ordine del catalogo. */
    private void aggiorna() {
        String testo = cerca == null ? "" : cerca.getText().toString();
        List<DisegnoDati.ElementoDisegno> si = new ArrayList<>(), no = new ArrayList<>();
        for (DisegnoDati.ElementoDisegno e : catalogo) {
            String g = e.gruppo.isEmpty() ? "Altro" : e.gruppo;
            if (!filtro.equals(TUTTI) && !filtro.equals(g)) continue;
            if (!SceltaComponenti.corrisponde(e.nome + " " + g + " " + e.sigla, testo)) continue;
            (posabile(e) ? si : no).add(e);
        }
        trovati.clear();
        trovati.addAll(si);
        trovati.addAll(no);
        conteggio.setText(trovati.size() == 1 ? "1 elemento" : trovati.size() + " elementi");
        adapterGriglia.notifyDataSetChanged();
        if (griglia != null) griglia.setSelection(0);
    }

    private View rigaFiltro(String gruppo, View cv) {
        TextView t = cv instanceof TextView ? (TextView) cv : new TextView(act);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setPadding(px(12), px(12), px(8), px(12));
        boolean sel = gruppo.equals(filtro);
        t.setText(gruppo.equals(TUTTI) ? "Tutti" : gruppo);
        t.setTextColor(sel ? BLU : TESTO);
        t.setTypeface(sel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        t.setBackground(sel ? arrotondato(BLU_CHIARO, 0, 10) : null);
        return t;
    }

    private View casella(DisegnoDati.ElementoDisegno e, View cv) {
        LinearLayout c;
        Anteprima an;
        TextView nome, dove;
        if (cv == null) {
            c = new LinearLayout(act);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setGravity(Gravity.CENTER_HORIZONTAL);
            c.setPadding(px(8), px(10), px(8), px(10));
            an = new Anteprima(act);
            c.addView(an, new LinearLayout.LayoutParams(px(90), px(64)));
            nome = new TextView(act);
            nome.setTextColor(TESTO);
            nome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            nome.setGravity(Gravity.CENTER);
            nome.setMaxLines(2);
            nome.setMinLines(2);
            nome.setEllipsize(TextUtils.TruncateAt.END);
            nome.setPadding(0, px(6), 0, 0);
            c.addView(nome, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            dove = new TextView(act);
            dove.setTextColor(TESTO2);
            dove.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            dove.setGravity(Gravity.CENTER);
            dove.setSingleLine(true);
            c.addView(dove, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            c.setTag(new View[]{an, nome, dove});
        } else {
            c = (LinearLayout) cv;
            View[] w = (View[]) c.getTag();
            an = (Anteprima) w[0];
            nome = (TextView) w[1];
            dove = (TextView) w[2];
        }
        boolean attuale = punto.esistente != null && punto.esistente.elemento != null && punto.esistente.elemento.codice.equals(e.codice);
        c.setBackground(attuale ? arrotondato(0xFFFFF3E0, ARANCIO, 12) : arrotondato(Color.WHITE, BORDO, 12));
        c.setAlpha(posabile(e) ? 1f : 0.38f);
        an.imposta(e);
        nome.setText(e.nome);
        dove.setText(e.parete() ? "sul muro" : "nella stanza");
        return c;
    }

    private View pulsante(String glifo, String testo, int colore, View.OnClickListener click) {
        LinearLayout b = new LinearLayout(act);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setPadding(px(12), px(8), px(14), px(8));
        b.setBackground(arrotondato(Color.WHITE, colore, 20));
        TextView g = glifo(glifo, colore, 15);
        g.setBackground(null);
        b.addView(g, new LinearLayout.LayoutParams(px(24), px(24)));
        TextView t = new TextView(act);
        t.setText(testo);
        t.setTextColor(colore);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(px(6), 0, 0, 0);
        b.addView(t);
        b.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(44));
        lp.setMargins(0, 0, px(10), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private TextView glifo(String g, int colore, int sp) {
        TextView t = new TextView(act);
        t.setTypeface(fa);
        t.setText(g);
        t.setTextColor(colore);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setGravity(Gravity.CENTER);
        TypedValue tv = new TypedValue();
        act.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true);
        t.setBackgroundResource(tv.resourceId);
        return t;
    }

    private GradientDrawable arrotondato(int colore, int bordo, int raggioDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(colore);
        g.setCornerRadius(px(raggioDp));
        if (bordo != 0) g.setStroke(Math.max(1, px(1)), bordo);
        return g;
    }

    private void nascondiTastiera() {
        if (cerca == null) return;
        cerca.clearFocus();
        InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(cerca.getWindowToken(), 0);
    }

    /** Anteprima dell'elemento con le stesse forme della piantina: sul muro un tratto di parete, a pavimento la forma. */
    private final class Anteprima extends View {
        private final DisegnoDati.Apertura a = new DisegnoDati.Apertura();
        private final Paint muro = new Paint(Paint.ANTI_ALIAS_FLAG);

        Anteprima(Context ctx) {
            super(ctx);
            muro.setColor(FormeDisegno.PARETE);
            muro.setStrokeCap(Paint.Cap.SQUARE);
        }

        void imposta(DisegnoDati.ElementoDisegno e) {
            a.elemento = e;
            a.tipo = e.tipo();
            a.rotazione = 0;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (a.elemento == null) return;
            float W = getWidth(), H = getHeight();
            DisegnoDati.ElementoDisegno e = a.elemento;
            if (e.parete()) {
                // il muro in alto, l'elemento lungo 3/4 della casella, aperto verso il basso (l'interno)
                float lung = Math.min(W * 0.62f, H * 0.72f);
                float L = lung * 100f / Math.max(2, e.larghezza);
                float spessore = Math.max(3 * dp, Math.min(5 * dp, L / 60f));
                float y = H * 0.2f, ax = (W - lung) / 2f;
                muro.setStrokeWidth(spessore);
                canvas.drawLine(2 * dp, y, W - 2 * dp, y, muro);
                FormeDisegno.parete(canvas, a, ax, y, ax + lung, y, 0, 1, L, spessore, dp);
            } else {
                float lato = Math.min(W, H) * 0.9f;
                float L = lato * 100f / Math.max(e.larghezza, e.profondita);
                float w = L * e.larghezza / 100f, h = L * e.profondita / 100f;
                FormeDisegno.pavimento(canvas, a, W / 2f, H / 2f, w, h, Math.max(L, 600f), Math.max(3 * dp, 4 * dp), dp);
            }
        }
    }
}
