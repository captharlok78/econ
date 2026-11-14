package com.ncfsistemi.econ.disegno;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Costruttori;
import com.ncfsistemi.econ.db.table.ElementiCantiereMateriali;
import com.ncfsistemi.econ.db.table.Listini;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;
import com.ncfsistemi.econ.views.CampoRicerca;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Riquadro "Materiale" di Componi (schema 47, STUDIO_DESIGN_GRAFICA.md §10): articoli della ditta o del listino aggiunti
 * all'elemento, accanto ai componenti. "+ Aggiungi materiale" apre la ricerca dell'articolo (codice, descrizione, barcode)
 * con quantita' e prezzo a costo; ogni riga nasce contabilizzata (pillola arancio, tocca per toglierla), si modifica
 * toccandola e si toglie col cestino. Il prezzo lo vede solo chi ha LISTINI.COSTI. Alto al massimo ~40% dello schermo:
 * le righe scorrono dentro.
 */
public class MaterialiElementoView extends LinearLayout {

    private final float dp;
    private final Typeface fa;
    private final boolean vediCosti;
    private final boolean puoModificare;
    private final TextView titolo;
    private final MaterialButton aggiungi;
    private final LinearLayout righe;
    private final ScrollView scorri;
    private int idElementoCant = 0;
    private boolean bloccato = false;

    public MaterialiElementoView(Context context) {
        this(context, null);
    }

    public MaterialiElementoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        dp = getResources().getDisplayMetrics().density;
        fa = ResourcesCompat.getFont(context, R.font.fa_solid_900);
        vediCosti = FunzionalitaApp.ha(context, FunzionalitaApp.LISTINI_COSTI);
        puoModificare = FunzionalitaApp.puoModificare(context, FunzionalitaApp.DESIGN);

        LinearLayout testa = new LinearLayout(context);
        testa.setOrientation(HORIZONTAL);
        testa.setGravity(Gravity.CENTER_VERTICAL);
        titolo = new TextView(context);
        titolo.setTextColor(colore(R.color.disegno_testo2));
        titolo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        titolo.setTypeface(Typeface.DEFAULT_BOLD);
        titolo.setAllCaps(true);
        testa.addView(titolo, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        aggiungi = new MaterialButton(context);
        aggiungi.setText("+ Aggiungi materiale");
        aggiungi.setAllCaps(false);
        aggiungi.setBackgroundTintList(ColorStateList.valueOf(colore(R.color.disegno_blu)));
        aggiungi.setOnClickListener(v -> finestraAggiungi());
        testa.addView(aggiungi);
        addView(testa);

        scorri = new ScrollView(context);
        righe = new LinearLayout(context);
        righe.setOrientation(VERTICAL);
        scorri.addView(righe);
        addView(scorri, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /** Alto al massimo il 40% dello schermo: il resto lo tiene la composizione. */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int max = Math.round(getResources().getDisplayMetrics().heightPixels * 0.4f);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED || h > max) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /** Elemento da mostrare; bloccato = documento non aperto o elemento di un altro ordine (sola lettura). */
    public void carica(int idElementoCant, boolean bloccato) {
        this.idElementoCant = idElementoCant;
        this.bloccato = bloccato || !puoModificare;
        aggiorna();
    }

    private void aggiorna() {
        aggiungi.setVisibility(bloccato || idElementoCant == 0 ? GONE : VISIBLE);
        righe.removeAllViews();
        DbInterno db = new DbInterno(getContext());
        List<ContentValues> lista = idElementoCant == 0 ? new ArrayList<>() : ElementiCantiereMateriali.diElemento(db, idElementoCant);
        db.close();
        double totale = 0;
        for (ContentValues m : lista) {
            if (intero(m, ElementiCantiereMateriali.NON_CONTEGGIARE_PREVENTIVI) == 0) {
                totale += numero(m, ElementiCantiereMateriali.QUANTITA) * numero(m, ElementiCantiereMateriali.PREZZO);
            }
            righe.addView(riga(m));
        }
        titolo.setText("Materiale (" + lista.size() + ")" + (vediCosti && totale > 0 ? "  ·  a costo " + euro(totale) : ""));
        if (lista.isEmpty()) {
            TextView vuoto = testo(13, R.color.disegno_testo2, false);
            vuoto.setText(bloccato ? "Nessun materiale" : "Nessun materiale: aggiungi articoli con il pulsante qui sopra");
            vuoto.setPadding(0, px(6), 0, 0);
            righe.addView(vuoto);
        }
    }

    private View riga(ContentValues m) {
        int id = intero(m, ElementiCantiereMateriali.ID);
        LinearLayout r = new LinearLayout(getContext());
        r.setOrientation(HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackgroundResource(R.drawable.bg_disegno_riga);
        r.setPadding(px(8), px(6), px(6), px(6));
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(6);
        r.setLayoutParams(lp);

        TextView icona = new TextView(getContext());
        icona.setTypeface(fa);
        icona.setText(FaIcone.MATERIALE);
        icona.setTextColor(colore(R.color.disegno_blu));
        icona.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        icona.setGravity(Gravity.CENTER);
        r.addView(icona, new LayoutParams(px(36), px(36)));

        LinearLayout testi = new LinearLayout(getContext());
        testi.setOrientation(VERTICAL);
        testi.setPadding(px(8), 0, px(8), 0);
        TextView nome = testo(15, R.color.disegno_testo, false);
        nome.setText(testo(m, ElementiCantiereMateriali.DESCRIZIONE).isEmpty() ? testo(m, ElementiCantiereMateriali.CODICE_ARTICOLO)
                : testo(m, ElementiCantiereMateriali.DESCRIZIONE));
        nome.setMaxLines(2);
        nome.setEllipsize(TextUtils.TruncateAt.END);
        testi.addView(nome);
        double q = numero(m, ElementiCantiereMateriali.QUANTITA);
        double p = numero(m, ElementiCantiereMateriali.PREZZO);
        List<String> info = new ArrayList<>();
        if (!testo(m, ElementiCantiereMateriali.CODICE_ARTICOLO).isEmpty()) info.add(testo(m, ElementiCantiereMateriali.CODICE_ARTICOLO));
        String um = testo(m, ElementiCantiereMateriali.UM);
        info.add("q.tà " + quantita(q) + (um.isEmpty() ? "" : " " + um) + (vediCosti && p > 0 ? " × " + euro(p) + " = " + euro(q * p) : ""));
        if (!testo(m, ElementiCantiereMateriali.NOTA).isEmpty()) info.add(testo(m, ElementiCantiereMateriali.NOTA));
        TextView dett = testo(12, R.color.disegno_testo2, false);
        dett.setText(TextUtils.join("  ·  ", info));
        testi.addView(dett);
        r.addView(testi, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // pillola "a costo", come quella dei componenti (ComposizioneAdapter)
        boolean contabilizzato = intero(m, ElementiCantiereMateriali.NON_CONTEGGIARE_PREVENTIVI) == 0;
        TextView pill = new TextView(getContext());
        pill.setTypeface(fa);
        pill.setText("");
        pill.setGravity(Gravity.CENTER);
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        pill.setTextColor(colore(contabilizzato ? R.color.disegno_arancio : R.color.colore_icona));
        pill.setBackgroundResource(contabilizzato ? R.drawable.bg_disegno_conteggiato : R.drawable.bg_disegno_non_conteggiato);
        String suggerimento = contabilizzato ? "Contabilizzato (va a costo): tocca per toglierlo" : "Non contabilizzato: tocca per metterlo a costo";
        pill.setContentDescription(suggerimento);
        TooltipCompat.setTooltipText(pill, suggerimento);
        r.addView(pill, new LayoutParams(px(52), px(36)));

        if (!bloccato) {
            pill.setOnClickListener(v -> {
                ContentValues c = new ContentValues();
                c.put(ElementiCantiereMateriali.NON_CONTEGGIARE_PREVENTIVI, contabilizzato ? 1 : 0);
                salva(id, c);
            });
            TextView cestino = new TextView(getContext());
            cestino.setTypeface(fa);
            cestino.setText(FaIcone.ELIMINA);
            cestino.setGravity(Gravity.CENTER);
            cestino.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            cestino.setTextColor(colore(R.color.disegno_testo2));
            cestino.setContentDescription("Togli il materiale");
            cestino.setOnClickListener(v -> new MaterialAlertDialogBuilder(getContext())
                    .setTitle("Togli il materiale")
                    .setMessage(nome.getText())
                    .setPositiveButton("Togli", (d, w) -> {
                        DbInterno db = new DbInterno(getContext());
                        new ElementiCantiereMateriali().cancellaRecord(db, m);
                        db.close();
                        aggiorna();
                    })
                    .setNegativeButton(R.string.annulla, null)
                    .show());
            LayoutParams lc = new LayoutParams(px(40), px(36));
            lc.leftMargin = px(4);
            r.addView(cestino, lc);
            r.setOnClickListener(v -> finestraModifica(m));
        }
        return r;
    }

    // ------------------------------------------------------------------ finestre

    private void finestraAggiungi() {
        Context ctx = getContext();
        LinearLayout corpo = new LinearLayout(ctx);
        corpo.setOrientation(VERTICAL);
        corpo.setPadding(px(20), px(8), px(20), 0);

        corpo.addView(etichetta("Articolo (codice, descrizione o barcode)"));
        CampoRicerca campo = new CampoRicerca(ctx);
        campo.setMinimoLettere(2);
        campo.setSingleLine(true);
        campo.setSorgente(t -> cercaArticoli(ctx, t));
        corpo.addView(campo, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout numeri = new LinearLayout(ctx);
        numeri.setOrientation(HORIZONTAL);
        EditText qta = numerico("1");
        EditText prezzo = numerico("");
        LinearLayout cq = colonna("Quantità", qta);
        numeri.addView(cq, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (vediCosti) {
            LinearLayout cp = colonna("Prezzo a costo €", prezzo);
            cp.setPadding(px(12), 0, 0, 0);
            numeri.addView(cp, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        corpo.addView(numeri);
        corpo.addView(etichetta("Nota"));
        EditText nota = new EditText(ctx);
        nota.setSingleLine(true);
        corpo.addView(nota);

        campo.setAscoltatore((c, voce) -> {
            if (voce == null) return;
            if (vediCosti && prezzo.getText().toString().trim().isEmpty()) {
                double costo = costo(voce.dati);
                if (costo > 0) prezzo.setText(formatta(costo));
            }
            qta.requestFocus();
            qta.selectAll();
        });

        AlertDialog d = new MaterialAlertDialogBuilder(ctx)
                .setTitle("Aggiungi materiale")
                .setView(corpo)
                .setPositiveButton("Aggiungi", null)
                .setNegativeButton(R.string.annulla, null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            CampoRicerca.Voce voce = campo.getScelta();
            if (voce == null || voce.dati == null) {
                campo.setError("Scegli un articolo dall'elenco");
                return;
            }
            double q = numero(qta.getText().toString());
            if (q <= 0) {
                qta.setError("Quantità maggiore di zero");
                return;
            }
            double p = vediCosti ? numero(prezzo.getText().toString()) : costo(voce.dati);
            DbInterno db = new DbInterno(ctx);
            new ElementiCantiereMateriali().aggiungi(db, idElementoCant, voce.dati, q, p, nota.getText().toString().trim());
            db.close();
            d.dismiss();
            aggiorna();
        }));
        d.show();
        campo.requestFocus();
    }

    private void finestraModifica(ContentValues m) {
        Context ctx = getContext();
        LinearLayout corpo = new LinearLayout(ctx);
        corpo.setOrientation(VERTICAL);
        corpo.setPadding(px(20), px(8), px(20), 0);
        LinearLayout numeri = new LinearLayout(ctx);
        numeri.setOrientation(HORIZONTAL);
        EditText qta = numerico(formatta(numero(m, ElementiCantiereMateriali.QUANTITA)));
        EditText prezzo = numerico(formatta(numero(m, ElementiCantiereMateriali.PREZZO)));
        numeri.addView(colonna("Quantità", qta), new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (vediCosti) {
            LinearLayout cp = colonna("Prezzo a costo €", prezzo);
            cp.setPadding(px(12), 0, 0, 0);
            numeri.addView(cp, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        corpo.addView(numeri);
        corpo.addView(etichetta("Nota"));
        EditText nota = new EditText(ctx);
        nota.setSingleLine(true);
        nota.setText(testo(m, ElementiCantiereMateriali.NOTA));
        corpo.addView(nota);

        String nome = testo(m, ElementiCantiereMateriali.CODICE_ARTICOLO) + " " + testo(m, ElementiCantiereMateriali.DESCRIZIONE);
        AlertDialog d = new MaterialAlertDialogBuilder(ctx)
                .setTitle(nome.trim())
                .setView(corpo)
                .setPositiveButton(R.string.salva, null)
                .setNegativeButton(R.string.annulla, null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double q = numero(qta.getText().toString());
            if (q <= 0) {
                qta.setError("Quantità maggiore di zero");
                return;
            }
            ContentValues c = new ContentValues();
            c.put(ElementiCantiereMateriali.QUANTITA, q);
            if (vediCosti) c.put(ElementiCantiereMateriali.PREZZO, numero(prezzo.getText().toString()));
            c.put(ElementiCantiereMateriali.NOTA, nota.getText().toString().trim());
            d.dismiss();
            salva(intero(m, ElementiCantiereMateriali.ID), c);
        }));
        d.show();
    }

    private void salva(int id, ContentValues campi) {
        DbInterno db = new DbInterno(getContext());
        new ElementiCantiereMateriali().aggiorna(db, id, campi);
        db.close();
        aggiorna();
    }

    /** Articoli del tablet (della ditta, anche nuovi non ancora inviati, e del listino), come la riga materiale del rapportino. */
    private static List<CampoRicerca.Voce> cercaArticoli(Context ctx, String testo) {
        String like = "'%" + testo.replace("'", "''") + "%'";
        List<CampoRicerca.Voce> ris = new ArrayList<>();
        DbInterno db = new DbInterno(ctx);
        try {
            for (Object o : db.eseguiSelect("select l.*, k." + Costruttori.SIGLA_METEL + " as sigla_costruttore from "
                    + Listini.NOME_TABELLA + " l left join " + Costruttori.NOME_TABELLA + " k on k." + Costruttori.ID_COSTRUTTORE
                    + "=l." + Listini.ID_COSTRUTTORE + " where coalesce(l." + Listini.ID + ",0)<>0"
                    + " and coalesce(l." + Listini.NEL_LISTINO + ",1)=1"
                    + " and (l." + Listini.CODICE_ARTICOLO + " like " + like + " or l." + Listini.DESCRIZIONE + " like " + like
                    + " or l." + Listini.CODICE_DITTA + " like " + like + " or l." + Listini.BARCODE + " like " + like
                    + " or l." + Listini.BARCODE_FORNITORE + " like " + like + " or l." + Listini.BARCODE_COSTRUTTORE + " like " + like + ")"
                    + " order by l." + Listini.CODICE_ARTICOLO + ", k." + Costruttori.SIGLA_METEL + " limit 50", null)) {
                ContentValues l = (ContentValues) o;
                String sigla = testo(l, "sigla_costruttore");
                ris.add(new CampoRicerca.Voce(intero(l, Listini.ID), testo(l, Listini.CODICE_ARTICOLO)
                        + (sigla.isEmpty() ? "" : " (" + sigla + ")") + " " + testo(l, Listini.DESCRIZIONE), l));
            }
        } finally {
            db.close();
        }
        return ris;
    }

    // ------------------------------------------------------------------ utilità

    /** Costo unitario proposto: ultimo acquisto, altrimenti listino meno sconto. */
    private static double costo(ContentValues a) {
        double acquisto = numero(a, Listini.PRZ_ULTIMO_ACQUISTO);
        if (acquisto > 0) return acquisto;
        double listino = numero(a, Listini.PRZ_LISTINO);
        double sconto = numero(a, Listini.SCONTO);
        return listino * (1 - sconto / 100.0);
    }

    private TextView etichetta(String s) {
        TextView t = testo(12, R.color.disegno_testo2, true);
        t.setText(s);
        t.setPadding(0, px(10), 0, 0);
        return t;
    }

    private LinearLayout colonna(String etichetta, View campo) {
        LinearLayout l = new LinearLayout(getContext());
        l.setOrientation(VERTICAL);
        l.addView(etichetta(etichetta));
        l.addView(campo, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return l;
    }

    private EditText numerico(String valore) {
        EditText e = new EditText(getContext());
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setSingleLine(true);
        e.setText(valore);
        e.setSelectAllOnFocus(true);
        return e;
    }

    private TextView testo(int sp, int coloreRes, boolean grassetto) {
        TextView t = new TextView(getContext());
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(colore(coloreRes));
        if (grassetto) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private int colore(int res) {
        return ContextCompat.getColor(getContext(), res);
    }

    private int px(float v) {
        return Math.round(v * dp);
    }

    private static String testo(ContentValues v, String campo) {
        String s = v.getAsString(campo);
        return s == null ? "" : s;
    }

    private static int intero(ContentValues v, String campo) {
        Integer i = null;
        try {
            i = v.getAsInteger(campo);
        } catch (Exception ignored) {
        }
        return i == null ? 0 : i;
    }

    private static double numero(ContentValues v, String campo) {
        return numero(v.getAsString(campo));
    }

    private static double numero(String s) {
        if (s == null) return 0;
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String formatta(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.format(Locale.US, "%.4f", v).replaceAll("0+$", "").replace('.', ',');
    }

    private static String quantita(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.format(Locale.ITALY, "%.3f", v).replaceAll("0+$", "");
    }

    private static String euro(double v) {
        return NumberFormat.getCurrencyInstance(Locale.ITALY).format(v);
    }
}
