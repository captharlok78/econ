package com.ncfsistemi.econ.disegno;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.android.material.button.MaterialButton;
import com.ncfsistemi.econ.CantiereSplitActivity;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Preventivi;
import com.ncfsistemi.econ.utils.FaIcone;

/**
 * Struttura del cantiere (cantiere › unità › aree › locali): componente unico per la scheda del cantiere
 * (CantiereSplitActivity) e per il pannello Struttura del disegno (DisegnoActivity). Doc STUDIO_DESIGN_GRAFICA.md §15.
 * <ul>
 *     <li>{@link #righe}: legge unità, aree (con livello) e locali (con il numero di elementi) nell'ordine scelto;</li>
 *     <li>{@link #barraOrdine}: i due pulsanti "Per livello" e "Locali A-Z", ricordati e uguali nelle due maschere;</li>
 *     <li>{@link Adapter}: le righe nella grafica del disegno, con il livello del piano a sinistra delle aree.</li>
 * </ul>
 * Le righe sono ContentValues con TIPO (costanti di CantiereSplitActivity), NOME, ID, e LIVELLO (aree) / N_ELEMENTI (locali).
 */
public final class StrutturaCantiere {

    public static final String TIPO = "TIPO", NOME = "NOME", ID = "ID", LIVELLO = "LIVELLO", N_ELEMENTI = "N_ELEMENTI";

    static final int BLU = 0xFF1565C0, BLU_CHIARO = 0xFFE3F2FD, SFONDO = 0xFFF4F6FA, TESTO = 0xFF212121, TESTO2 = 0xFF5F6B7A;

    private static final String PREF = "ECON", PREF_ORDINE_LIVELLI = "STRUTTURA_ORDINE_LIVELLI",
            PREF_LOCALI_ALFABETICI = "STRUTTURA_LOCALI_ALFABETICI";

    private StrutturaCantiere() {
    }

    // ------------------------------------------------------------------ ordine (ricordato, uguale nelle due maschere)

    public static boolean perLivello(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(PREF_ORDINE_LIVELLI, true);
    }

    public static boolean localiAZ(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(PREF_LOCALI_ALFABETICI, true);
    }

    // ------------------------------------------------------------------ dati

    /** Unità (più recenti in alto), aree (per livello se scelto, poi più recenti in alto) e locali di ogni area. */
    public static List<ContentValues> righe(Context ctx, int idCantiere) {
        boolean livelli = perLivello(ctx), az = localiAZ(ctx);
        List<ContentValues> out = new ArrayList<>();
        DbInterno db = new DbInterno(ctx);
        try {
            ArrayList<Object> unita = db.eseguiSelect("select id_unita, nome from unita where id_cantiere=" + idCantiere
                    + " order by id_unita desc", null);
            for (Object ou : unita) {
                ContentValues u = (ContentValues) ou;
                int idUnita = u.getAsInteger("id_unita");
                out.add(riga(CantiereSplitActivity.UNITA, idUnita, u.getAsString("nome")));
                ArrayList<Object> aree = db.eseguiSelect("select id_area, nome, livello from aree where id_unita=" + idUnita
                        + " order by id_area desc", null);
                if (livelli) {
                    // dal livello più basso (scantinato) al più alto; a parità l'ordine di prima (sort stabile)
                    Collections.sort(aree, (x, y) -> Integer.compare(livello((ContentValues) x), livello((ContentValues) y)));
                }
                for (Object oa : aree) {
                    ContentValues a = (ContentValues) oa;
                    int idArea = a.getAsInteger("id_area");
                    ContentValues ra = riga(CantiereSplitActivity.AREA, idArea, a.getAsString("nome"));
                    ra.put(LIVELLO, livello(a));
                    out.add(ra);
                    ArrayList<Object> locali = db.eseguiSelect("select l.id_locale, l.nome, (select count(*) from"
                            + " elementi_cantiere e where e.id_locale=l.id_locale) as n from locali l where l.id_area=" + idArea
                            + (az ? " order by l.nome collate nocase, l.id_locale" : " order by l.id_locale"), null);
                    for (Object ol : locali) {
                        ContentValues l = (ContentValues) ol;
                        ContentValues rl = riga(CantiereSplitActivity.LOCALE, l.getAsInteger("id_locale"), l.getAsString("nome"));
                        rl.put(N_ELEMENTI, l.getAsInteger("n"));
                        out.add(rl);
                    }
                }
            }
        } finally {
            db.close();
        }
        return out;
    }

    public static ContentValues riga(int tipo, int id, String nome) {
        ContentValues r = new ContentValues();
        r.put(TIPO, tipo);
        r.put(ID, id);
        r.put(NOME, nome == null ? "" : nome.trim());
        return r;
    }

    private static int livello(ContentValues area) {
        Integer l = area.getAsInteger("livello");
        return l == null ? 0 : l;
    }

    /** Livello per la pastiglia a sinistra dell'area: il numero (-1, 0, 1, 2...). */
    public static String siglaLivello(int livello) {
        return String.valueOf(livello);
    }

    /**
     * Cambia nome e livello di un'area (matita sulla riga). Salva con il log di modifica, così va al server con la sync;
     * poi chiama {@code dopo}.
     */
    public static void modificaArea(Context ctx, int idArea, Runnable dopo) {
        DbInterno db = new DbInterno(ctx);
        ContentValues area;
        try {
            area = db.getRecord("select nome, livello from aree where id_area=" + idArea);
        } finally {
            db.close();
        }
        if (area == null) return;
        float dp = ctx.getResources().getDisplayMetrics().density;
        LinearLayout campi = new LinearLayout(ctx);
        campi.setOrientation(LinearLayout.VERTICAL);
        campi.setPadding(Math.round(24 * dp), Math.round(8 * dp), Math.round(24 * dp), 0);
        com.google.android.material.textfield.TextInputLayout tilNome = new com.google.android.material.textfield.TextInputLayout(ctx, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        tilNome.setHint("Nome dell'area");
        com.google.android.material.textfield.TextInputEditText nome = new com.google.android.material.textfield.TextInputEditText(tilNome.getContext());
        nome.setSingleLine(true);
        nome.setText(area.getAsString("nome"));
        tilNome.addView(nome);
        campi.addView(tilNome);
        TextView et = new TextView(ctx);
        et.setText("Livello (piano)");
        et.setTextColor(TESTO2);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        et.setPadding(0, Math.round(14 * dp), 0, Math.round(4 * dp));
        campi.addView(et);
        String[][] voci = com.ncfsistemi.econ.form.AreaForm.vociLivello();
        String[] testi = new String[voci.length];
        for (int i = 0; i < voci.length; i++) testi[i] = voci[i][1];
        android.widget.Spinner livello = new android.widget.Spinner(ctx);
        livello.setAdapter(new android.widget.ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, testi));
        Integer l = area.getAsInteger("livello");
        int attuale = l == null ? 0 : l;
        for (int i = 0; i < voci.length; i++) if (voci[i][0].equals(String.valueOf(attuale))) livello.setSelection(i);
        campi.addView(livello, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(48 * dp)));
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
                .setTitle("Area")
                .setView(campi)
                .setNegativeButton(R.string.annulla, null)
                .setPositiveButton(R.string.conferma, (d, w) -> {
                    String n = nome.getText() == null ? "" : nome.getText().toString().trim();
                    if (n.isEmpty()) {
                        android.widget.Toast.makeText(ctx, "Il nome dell'area non può essere vuoto", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int liv = Integer.parseInt(voci[livello.getSelectedItemPosition()][0]);
                    DbInterno dbs = new DbInterno(ctx);
                    try {
                        com.ncfsistemi.econ.db.table.Aree tab = new com.ncfsistemi.econ.db.table.Aree();
                        ContentValues val = tab.getValoriLogModifica(dbs);
                        val.put(com.ncfsistemi.econ.db.table.Aree.NOME, n);
                        val.put(com.ncfsistemi.econ.db.table.Aree.LIVELLO, liv);
                        ContentValues where = new ContentValues();
                        where.put(com.ncfsistemi.econ.db.table.Aree.ID_AREA, idArea);
                        tab.aggiornaRecord(dbs, val, where);
                    } finally {
                        dbs.close();
                    }
                    if (dopo != null) dopo.run();
                })
                .show();
    }

    /** Etichetta del tipo di riga (menu contestuale, accessibilità): "Area · Piano terra". */
    public static String tipoTesto(ContentValues r, Context ctx) {
        int tipo = r.getAsInteger(TIPO);
        if (tipo == CantiereSplitActivity.PREVENTIVO) {
            if (Preventivi.TIPO_ORDINE.equals(r.getAsString("TIPO_PREV_ORD"))) return ctx.getString(R.string.dettaglio_ordine);
            return ctx.getString(R.string.dettaglio_preventivo);
        }
        if (tipo == CantiereSplitActivity.CANTIERE) return ctx.getString(R.string.cantiere);
        if (tipo == CantiereSplitActivity.UNITA) return ctx.getString(R.string.unita);
        if (tipo == CantiereSplitActivity.AREA) {
            Integer l = r.getAsInteger(LIVELLO);
            return ctx.getString(R.string.area) + (l == null ? "" : " · " + com.ncfsistemi.econ.form.AreaForm.nomeLivello(l));
        }
        if (tipo == CantiereSplitActivity.LOCALE) return ctx.getString(R.string.locale);
        return "";
    }

    // ------------------------------------------------------------------ barra dell'ordine

    /**
     * I due pulsanti in cima alla struttura: "Per livello" (aree dal piano più basso) e "Locali A-Z". Si ricordano e
     * valgono per scheda cantiere e disegno; al cambio chiama {@code cambiato} (che rilegge le righe).
     */
    public static LinearLayout barraOrdine(Context ctx, Runnable cambiato) {
        float dp = ctx.getResources().getDisplayMetrics().density;
        LinearLayout barra = new LinearLayout(ctx);
        barra.setOrientation(LinearLayout.HORIZONTAL);
        barra.setGravity(Gravity.CENTER_VERTICAL);
        barra.setPadding(Math.round(8 * dp), Math.round(6 * dp), Math.round(8 * dp), Math.round(6 * dp));
        MaterialButton livelli = bottone(ctx, "Per livello", FaIcone.AREA, "Aree dal piano più basso (scantinato) al più alto");
        MaterialButton az = bottone(ctx, "Locali A-Z", "", "Locali di ogni area in ordine alfabetico");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = Math.round(6 * dp);
        barra.addView(livelli, lp);
        barra.addView(az, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        SharedPreferences pref = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        stato(livelli, perLivello(ctx));
        stato(az, localiAZ(ctx));
        livelli.setOnClickListener(v -> {
            boolean nuovo = !perLivello(ctx);
            pref.edit().putBoolean(PREF_ORDINE_LIVELLI, nuovo).apply();
            stato(livelli, nuovo);
            cambiato.run();
        });
        az.setOnClickListener(v -> {
            boolean nuovo = !localiAZ(ctx);
            pref.edit().putBoolean(PREF_LOCALI_ALFABETICI, nuovo).apply();
            stato(az, nuovo);
            cambiato.run();
        });
        return barra;
    }

    private static MaterialButton bottone(Context ctx, String testo, String glifo, String suggerimento) {
        float dp = ctx.getResources().getDisplayMetrics().density;
        MaterialButton b = new MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(testo);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setSingleLine(true);
        b.setEllipsize(TextUtils.TruncateAt.END);
        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setMinHeight(Math.round(36 * dp));
        b.setMinimumHeight(Math.round(36 * dp));
        b.setPadding(Math.round(8 * dp), 0, Math.round(8 * dp), 0);
        b.setCornerRadius(Math.round(18 * dp));
        b.setIcon(FaIcone.drawable(ctx, glifo, 14));
        b.setIconPadding(Math.round(6 * dp));
        b.setContentDescription(suggerimento);
        androidx.appcompat.widget.TooltipCompat.setTooltipText(b, suggerimento);
        return b;
    }

    private static void stato(MaterialButton b, boolean attivo) {
        b.setBackgroundTintList(ColorStateList.valueOf(attivo ? BLU : Color.WHITE));
        b.setTextColor(attivo ? Color.WHITE : BLU);
        b.setIconTint(ColorStateList.valueOf(attivo ? Color.WHITE : BLU));
        b.setStrokeColor(ColorStateList.valueOf(BLU));
    }

    /** ListView della struttura, senza divisori (le righe hanno già i loro spazi). */
    public static void stileLista(ListView lista) {
        lista.setDivider(null);
        lista.setDividerHeight(0);
        lista.setBackgroundColor(Color.TRANSPARENT);
        lista.setCacheColorHint(Color.TRANSPARENT);
        lista.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        float dp = lista.getResources().getDisplayMetrics().density;
        lista.setPadding(Math.round(6 * dp), Math.round(2 * dp), Math.round(6 * dp), Math.round(8 * dp));
        lista.setClipToPadding(false);
    }

    // ------------------------------------------------------------------ righe

    /** Cosa è selezionato e cosa si può toccare: lo decide la maschera. */
    public interface Selezione {
        boolean selezionata(int posizione, ContentValues riga);

        boolean attiva(int posizione, ContentValues riga);
    }

    /** Righe della struttura nella grafica del disegno. */
    public static class Adapter extends BaseAdapter {
        private final Context ctx;
        private final List<Object> dati;
        private final Selezione selezione;
        private final float dp;
        /** Dopo la modifica di un'area (nome, livello): la maschera rilegge. Null = niente matita. */
        private Runnable dopoModificaArea;

        public void setDopoModificaArea(Runnable r) {
            this.dopoModificaArea = r;
        }

        public Adapter(Context ctx, List<Object> dati, Selezione selezione) {
            this.ctx = ctx;
            this.dati = dati;
            this.selezione = selezione;
            this.dp = ctx.getResources().getDisplayMetrics().density;
        }

        @Override
        public int getCount() {
            return dati.size();
        }

        @Override
        public Object getItem(int position) {
            return dati.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean isEnabled(int position) {
            return selezione.attiva(position, (ContentValues) dati.get(position));
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        private int px(float v) {
            return Math.round(v * dp);
        }

        private static final class Holder {
            LinearLayout fondo;
            TextView sinistra, tipo, nome, conta, matita;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder h;
            if (convertView == null) {
                h = new Holder();
                LinearLayout esterno = new LinearLayout(ctx);
                esterno.setPadding(0, px(1), 0, px(1));
                h.fondo = new LinearLayout(ctx);
                h.fondo.setOrientation(LinearLayout.HORIZONTAL);
                h.fondo.setGravity(Gravity.CENTER_VERTICAL);
                esterno.addView(h.fondo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                h.sinistra = new TextView(ctx);
                h.sinistra.setGravity(Gravity.CENTER);
                h.sinistra.setIncludeFontPadding(false);
                h.fondo.addView(h.sinistra, new LinearLayout.LayoutParams(px(32), px(24)));
                LinearLayout testi = new LinearLayout(ctx);
                testi.setOrientation(LinearLayout.VERTICAL);
                h.tipo = new TextView(ctx);
                h.tipo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                h.tipo.setAllCaps(true);
                h.tipo.setTypeface(Typeface.DEFAULT_BOLD);
                h.tipo.setTextColor(TESTO2);
                h.tipo.setLetterSpacing(0.05f);
                h.nome = new TextView(ctx);
                h.nome.setSingleLine(true);
                h.nome.setEllipsize(TextUtils.TruncateAt.END);
                testi.addView(h.tipo);
                testi.addView(h.nome);
                LinearLayout.LayoutParams lpT = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lpT.leftMargin = px(8);
                h.fondo.addView(testi, lpT);
                h.conta = new TextView(ctx);
                h.conta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                h.conta.setTextColor(TESTO2);
                h.conta.setPadding(px(8), px(1), px(8), px(1));
                h.conta.setBackground(arrotondato(0xFFECEFF1, 0, 10));
                h.fondo.addView(h.conta);
                h.matita = new TextView(ctx);
                FaIcone.applica(h.matita, FaIcone.MODIFICA, "Cambia nome e livello dell'area");
                h.matita.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                h.matita.setTextColor(TESTO2);
                h.matita.setContentDescription("Cambia nome e livello dell'area");
                TypedValue tvm = new TypedValue();
                ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tvm, true);
                h.matita.setBackgroundResource(tvm.resourceId);
                h.fondo.addView(h.matita, new LinearLayout.LayoutParams(px(36), px(32)));
                esterno.setTag(h);
                convertView = esterno;
            } else {
                h = (Holder) convertView.getTag();
            }
            ContentValues r = (ContentValues) dati.get(position);
            int tipo = r.getAsInteger(TIPO);
            boolean sel = selezione.selezionata(position, r);
            boolean attiva = selezione.attiva(position, r);
            String nome = r.getAsString(NOME);
            h.nome.setText(nome == null || nome.isEmpty() ? "—" : nome);
            h.conta.setVisibility(View.GONE);
            h.tipo.setVisibility(View.GONE);
            h.matita.setVisibility(View.GONE);
            h.matita.setOnClickListener(null);
            h.nome.setTypeface(sel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            h.nome.setTextColor(sel ? BLU : TESTO);
            h.sinistra.setBackground(null);

            if (tipo == CantiereSplitActivity.PREVENTIVO || tipo == CantiereSplitActivity.CANTIERE) {
                // testa: icona, tipo piccolo e nome
                icona(h.sinistra, tipo == CantiereSplitActivity.PREVENTIVO ? FaIcone.PREVENTIVO : FaIcone.CANTIERE, sel ? BLU : TESTO2, 16);
                h.tipo.setVisibility(View.VISIBLE);
                h.tipo.setText(tipoTesto(r, ctx));
                h.nome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                h.nome.setTypeface(Typeface.DEFAULT_BOLD);
                h.fondo.setPadding(px(6), px(8), px(8), px(8));
            } else if (tipo == CantiereSplitActivity.UNITA) {
                // intestazione dell'unità: piccola, maiuscola, grigia
                icona(h.sinistra, FaIcone.UNITA, sel ? BLU : TESTO2, 13);
                h.nome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                h.nome.setAllCaps(true);
                h.nome.setTypeface(Typeface.DEFAULT_BOLD);
                h.nome.setTextColor(sel ? BLU : TESTO2);
                h.fondo.setPadding(px(6), px(10), px(8), px(4));
            } else if (tipo == CantiereSplitActivity.AREA) {
                // a sinistra la pastiglia del livello (PT, -1, P1...)
                Integer l = r.getAsInteger(LIVELLO);
                int liv = l == null ? 0 : l;
                h.sinistra.setTypeface(Typeface.DEFAULT_BOLD);
                h.sinistra.setText(siglaLivello(liv));
                h.sinistra.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                int colore = liv < 0 ? 0xFF6D4C41 : liv == 0 ? 0xFF2E7D32 : BLU;
                int fondo = liv < 0 ? 0xFFEFEBE9 : liv == 0 ? 0xFFE8F5E9 : BLU_CHIARO;
                h.sinistra.setTextColor(colore);
                h.sinistra.setBackground(arrotondato(fondo, colore, 12));
                h.sinistra.setContentDescription(com.ncfsistemi.econ.form.AreaForm.nomeLivello(liv));
                h.nome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                h.nome.setAllCaps(false);
                if (!sel) h.nome.setTextColor(0xFF37474F);
                h.nome.setTypeface(Typeface.DEFAULT_BOLD);
                h.fondo.setPadding(px(14), px(6), px(4), px(4));
                if (dopoModificaArea != null && com.ncfsistemi.econ.utils.FunzionalitaApp.puoModificare(ctx,
                        com.ncfsistemi.econ.utils.FunzionalitaApp.STRUTTURA)) {
                    int idArea = r.getAsInteger(ID);
                    h.matita.setVisibility(View.VISIBLE);
                    h.matita.setOnClickListener(v -> modificaArea(ctx, idArea, dopoModificaArea));
                }
            } else {
                // locale: rientrato, con il numero di elementi a destra
                icona(h.sinistra, FaIcone.LOCALE, sel ? BLU : 0xFF90A4AE, 13);
                h.nome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                h.nome.setAllCaps(false);
                Integer n = r.getAsInteger(N_ELEMENTI);
                if (n != null && n > 0) {
                    h.conta.setVisibility(View.VISIBLE);
                    h.conta.setText(String.valueOf(n));
                }
                h.fondo.setPadding(px(26), px(8), px(8), px(8));
            }
            if (tipo != CantiereSplitActivity.UNITA) h.nome.setAllCaps(false);
            if (sel) {
                h.fondo.setBackground(arrotondato(BLU_CHIARO, 0, 8));
            } else if (attiva) {
                TypedValue tv = new TypedValue();
                ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
                h.fondo.setBackgroundResource(tv.resourceId);
            } else {
                h.fondo.setBackground(null);
            }
            return convertView;
        }

        private void icona(TextView tv, String glifo, int colore, int sp) {
            FaIcone.applica(tv, glifo, null);
            tv.setTextColor(colore);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        }

        private GradientDrawable arrotondato(int colore, int bordo, int raggioDp) {
            GradientDrawable g = new GradientDrawable();
            g.setColor(colore);
            g.setCornerRadius(px(raggioDp));
            if (bordo != 0) g.setStroke(Math.max(1, px(1)), bordo);
            return g;
        }
    }
}
