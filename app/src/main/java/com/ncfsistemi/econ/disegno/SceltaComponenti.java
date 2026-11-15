package com.ncfsistemi.econ.disegno;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.table.Elementi;
import com.ncfsistemi.econ.utils.SimboliDisegno;

/**
 * Finestra unica a tutto schermo per scegliere il componente da posare nel disegno (STUDIO_DESIGN_GRAFICA.md §17):
 * prende il posto della tavolozza e della finestra "Cerca un elemento".
 * <ul>
 *     <li>in alto la barra blu con chiudi, titolo e la casella di ricerca (tutte le parole, maiuscole indifferenti);</li>
 *     <li>a sinistra il filtro: Tutti, Preferiti e le categorie, sempre visibili (niente menu a tendina, che con la
 *     tastiera aperta in orizzontale restava tagliato);</li>
 *     <li>a destra la griglia dei componenti, che scorre; con la tastiera la finestra si ridimensiona.</li>
 * </ul>
 * Toccando un componente lo si prende per posarlo e la finestra si chiude.
 */
public final class SceltaComponenti {

    /** Categoria "tutte" del filtro (0 = preferiti, >0 = categoria). */
    public static final int TUTTE = -1;

    public interface Scelta {
        /** Componente scelto; categoria = filtro attivo (da ricordare per la prossima apertura). */
        void scelto(DisegnoDati.VoceTavolozza voce, int categoria);
    }

    private static final int BLU = 0xFF1565C0, BLU_CHIARO = 0xFFE3F2FD, SFONDO = 0xFFF4F6FA, BORDO = 0xFFDDE3EA,
            TESTO = 0xFF212121, TESTO2 = 0xFF5F6B7A, ARANCIO = 0xFFFB8C00;

    private final Activity act;
    private final IntFunction<List<DisegnoDati.VoceTavolozza>> voci;
    private final float dp;
    private final Typeface fa;
    private final List<DisegnoDati.Categoria> categorie;
    private final Map<Integer, String> nomeCategoria = new HashMap<>();

    private Dialog dlg;
    private EditText cerca;
    private TextView conteggio;
    private int filtro;
    private final List<Object[]> righeFiltro = new ArrayList<>(); // {id, nome}
    private BaseAdapter adapterFiltro;
    private final List<DisegnoDati.VoceTavolozza> trovati = new ArrayList<>();
    private BaseAdapter adapterGriglia;
    private GridView griglia;

    public SceltaComponenti(Activity act, IntFunction<List<DisegnoDati.VoceTavolozza>> voci) {
        this.act = act;
        this.voci = voci;
        this.dp = act.getResources().getDisplayMetrics().density;
        this.fa = ResourcesCompat.getFont(act, R.font.fa_solid_900);
        this.categorie = DisegnoDati.categorie(act);
        righeFiltro.add(new Object[]{TUTTE, "Tutti"});
        righeFiltro.add(new Object[]{0, "Preferiti"});
        for (DisegnoDati.Categoria c : categorie) {
            righeFiltro.add(new Object[]{c.id, c.nome});
            nomeCategoria.put(c.id, c.nome);
        }
    }

    private int px(float v) {
        return Math.round(v * dp);
    }

    /** Apre la finestra sul filtro dato; {@code idScelto} è il componente già scelto (evidenziato), 0 se nessuno. */
    public void apri(int categoriaIniziale, int idScelto, Scelta scelta) {
        filtro = categoriaIniziale;
        boolean esiste = false;
        for (Object[] r : righeFiltro) if ((int) r[0] == filtro) esiste = true;
        if (!esiste) filtro = TUTTE;

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
        titolo.setText("Componenti");
        titolo.setTextColor(Color.WHITE);
        titolo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        titolo.setTypeface(Typeface.DEFAULT_BOLD);
        titolo.setPadding(px(4), 0, px(16), 0);
        barra.addView(titolo);

        LinearLayout casella = new LinearLayout(act);
        casella.setOrientation(LinearLayout.HORIZONTAL);
        casella.setGravity(Gravity.CENTER_VERTICAL);
        casella.setBackground(arrotondato(Color.WHITE, 0, 22));
        casella.setPadding(px(14), 0, px(6), 0);
        TextView lente = glifo("", TESTO2, 15);
        casella.addView(lente, new LinearLayout.LayoutParams(px(22), px(40)));
        cerca = new EditText(act);
        cerca.setHint("Cerca un componente (tutte le parole)");
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

        // corpo: filtro a sinistra, griglia a destra
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
            @Override public int getCount() { return righeFiltro.size(); }
            @Override public Object getItem(int p) { return righeFiltro.get(p); }
            @Override public long getItemId(int p) { return p; }
            @Override public View getView(int p, View cv, ViewGroup parent) { return rigaFiltro(p, cv); }
        };
        listaFiltro.setAdapter(adapterFiltro);
        listaFiltro.setOnItemClickListener((parent, view, p, id) -> {
            filtro = (int) righeFiltro.get(p)[0];
            adapterFiltro.notifyDataSetChanged();
            aggiorna();
        });
        int largFiltro = act.getResources().getConfiguration().screenWidthDp < 700 ? 190 : 250;
        corpo.addView(listaFiltro, new LinearLayout.LayoutParams(px(largFiltro), ViewGroup.LayoutParams.MATCH_PARENT));
        View sep = new View(act);
        sep.setBackgroundColor(BORDO);
        corpo.addView(sep, new LinearLayout.LayoutParams(px(1), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout destra = new LinearLayout(act);
        destra.setOrientation(LinearLayout.VERTICAL);
        griglia = new GridView(act);
        griglia.setNumColumns(GridView.AUTO_FIT);
        griglia.setColumnWidth(px(150));
        griglia.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        griglia.setHorizontalSpacing(px(10));
        griglia.setVerticalSpacing(px(10));
        griglia.setPadding(px(12), px(12), px(12), px(12));
        griglia.setClipToPadding(false);
        griglia.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        adapterGriglia = new BaseAdapter() {
            @Override public int getCount() { return trovati.size(); }
            @Override public Object getItem(int p) { return trovati.get(p); }
            @Override public long getItemId(int p) { return trovati.get(p).id; }
            @Override public View getView(int p, View cv, ViewGroup parent) { return casellaComponente(trovati.get(p), idScelto, cv); }
        };
        griglia.setAdapter(adapterGriglia);
        griglia.setOnItemClickListener((parent, view, p, id) -> {
            DisegnoDati.VoceTavolozza v = trovati.get(p);
            nascondiTastiera();
            chiudi();
            scelta.scelto(v, filtro);
        });
        // toccando la griglia mentre si scrive si chiude la tastiera, così si vede tutto
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
        vuoto.setText("Nessun componente trovato");
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

        dlg = new Dialog(act, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
        dlg.setContentView(root);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dlg.getWindow().setStatusBarColor(BLU);
            // con la tastiera la finestra si accorcia: barra e filtro restano, la griglia scorre
            dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
        aggiorna();
        dlg.show();
        // il filtro scelto si vede anche se è in fondo
        for (int i = 0; i < righeFiltro.size(); i++) {
            if ((int) righeFiltro.get(i)[0] == filtro) {
                final int pos = i;
                listaFiltro.post(() -> listaFiltro.setSelection(Math.max(0, pos - 2)));
            }
        }
    }

    public void chiudi() {
        if (dlg != null && dlg.isShowing()) dlg.dismiss();
        dlg = null;
    }

    private void aggiorna() {
        String testo = cerca == null ? "" : cerca.getText().toString();
        trovati.clear();
        if (filtro >= 0) {
            for (DisegnoDati.VoceTavolozza v : voci.apply(filtro)) if (corrisponde(v.nome, testo)) trovati.add(v);
        } else {
            for (DisegnoDati.Categoria c : categorie) {
                for (DisegnoDati.VoceTavolozza v : voci.apply(c.id)) if (corrisponde(v.nome, testo)) trovati.add(v);
            }
            Collections.sort(trovati, (x, y) -> x.nome.compareToIgnoreCase(y.nome));
        }
        conteggio.setText(trovati.size() == 1 ? "1 componente" : trovati.size() + " componenti");
        adapterGriglia.notifyDataSetChanged();
        if (griglia != null) griglia.setSelection(0);
    }

    static boolean corrisponde(String nome, String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) return true;
        String n = nome == null ? "" : nome.toLowerCase(Locale.ROOT);
        for (String parola : filtro.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (!n.contains(parola)) return false;
        }
        return true;
    }

    private View rigaFiltro(int p, View cv) {
        LinearLayout r;
        ImageView iv;
        TextView st, t;
        if (cv == null) {
            r = new LinearLayout(act);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(px(8), px(8), px(8), px(8));
            iv = new ImageView(act);
            r.addView(iv, new LinearLayout.LayoutParams(px(30), px(30)));
            st = glifo("", BLU, 16);
            r.addView(st, new LinearLayout.LayoutParams(px(30), px(30)));
            t = new TextView(act);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            t.setPadding(px(10), 0, px(4), 0);
            r.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            r.setTag(new View[]{iv, st, t});
        } else {
            r = (LinearLayout) cv;
            View[] v = (View[]) r.getTag();
            iv = (ImageView) v[0];
            st = (TextView) v[1];
            t = (TextView) v[2];
        }
        int id = (int) righeFiltro.get(p)[0];
        boolean sel = id == filtro;
        t.setText((String) righeFiltro.get(p)[1]);
        t.setTextColor(sel ? BLU : TESTO);
        t.setTypeface(sel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        r.setBackground(sel ? arrotondato(BLU_CHIARO, 0, 10) : null);
        if (id > 0) {
            iv.setVisibility(View.VISIBLE);
            st.setVisibility(View.GONE);
            iv.setImageDrawable(SimboliDisegno.drawable(act, SimboliDisegno.categoriaElementi(id)));
        } else {
            iv.setVisibility(View.GONE);
            st.setVisibility(View.VISIBLE);
            st.setText(id == 0 ? "" : "");
            st.setTextColor(id == 0 ? ARANCIO : BLU);
        }
        return r;
    }

    private View casellaComponente(DisegnoDati.VoceTavolozza v, int idScelto, View cv) {
        LinearLayout c;
        ImageView iv;
        TextView nome, cat;
        if (cv == null) {
            c = new LinearLayout(act);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setGravity(Gravity.CENTER_HORIZONTAL);
            c.setPadding(px(8), px(10), px(8), px(10));
            iv = new ImageView(act);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            c.addView(iv, new LinearLayout.LayoutParams(px(64), px(64)));
            nome = new TextView(act);
            nome.setTextColor(TESTO);
            nome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            nome.setGravity(Gravity.CENTER);
            nome.setMaxLines(2);
            nome.setMinLines(2);
            nome.setEllipsize(TextUtils.TruncateAt.END);
            nome.setPadding(0, px(6), 0, 0);
            c.addView(nome, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            cat = new TextView(act);
            cat.setTextColor(TESTO2);
            cat.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            cat.setGravity(Gravity.CENTER);
            cat.setSingleLine(true);
            cat.setEllipsize(TextUtils.TruncateAt.END);
            c.addView(cat, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            c.setTag(new View[]{iv, nome, cat});
        } else {
            c = (LinearLayout) cv;
            View[] w = (View[]) c.getTag();
            iv = (ImageView) w[0];
            nome = (TextView) w[1];
            cat = (TextView) w[2];
        }
        boolean sel = v.id == idScelto;
        c.setBackground(sel ? arrotondato(0xFFFFF3E0, ARANCIO, 12) : arrotondato(Color.WHITE, BORDO, 12));
        iv.setImageDrawable(SimboliDisegno.fotoOppureSimbolo(act, Elementi.PATH_ICONE, v.icona, SimboliDisegno.elemento(v.id)));
        nome.setText(v.nome);
        String nc = nomeCategoria.get(v.idCategoria);
        // la categoria serve quando si guarda fra tutti
        cat.setText(filtro == TUTTE || filtro == 0 ? (nc == null ? "" : nc) + (v.preferito ? " ★" : "") : (v.preferito ? "★ preferito" : ""));
        cat.setVisibility(cat.getText().length() > 0 ? View.VISIBLE : View.GONE);
        return c;
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
}
