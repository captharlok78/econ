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
 *     <li>{@link #barra}: barra a icone (Per livello, Locali A-Z, "+" nuovo, matita, schermo intero), uguale nelle due maschere;</li>
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

    // ------------------------------------------------------------------ modifica (matita)

    /**
     * Modalità modifica della struttura (matita accanto all'ordine): sulle righe di unità, aree e locali compare la matita
     * per cambiarne nome e collegamenti. Vale per scheda cantiere e disegno finché l'app resta aperta.
     */
    private static boolean inModifica = false;

    public static boolean inModifica() {
        return inModifica;
    }

    public static boolean puoModificare(Context ctx) {
        return com.ncfsistemi.econ.utils.FunzionalitaApp.puoModificare(ctx, com.ncfsistemi.econ.utils.FunzionalitaApp.STRUTTURA);
    }

    /** Apre la modifica della riga (unità, area o locale); salva con il log di modifica (va al server con la sync). */
    public static void modifica(Context ctx, int tipo, int id, Runnable dopo) {
        if (tipo == CantiereSplitActivity.UNITA) modificaUnita(ctx, id, dopo);
        else if (tipo == CantiereSplitActivity.AREA) modificaArea(ctx, id, dopo);
        else if (tipo == CantiereSplitActivity.LOCALE) modificaLocale(ctx, id, dopo);
    }

    /** Unità: il nome (il cantiere si cambia dalla scheda del cantiere). */
    public static void modificaUnita(Context ctx, int idUnita, Runnable dopo) {
        ContentValues u = record(ctx, "select nome from unita where id_unita=" + idUnita);
        if (u == null) return;
        Campi c = new Campi(ctx);
        com.google.android.material.textfield.TextInputEditText nome = c.testo("Nome dell'unità", u.getAsString("nome"));
        dialogo(ctx, "Unità", c, () -> {
            String n = testo(nome);
            if (n.isEmpty()) return "Il nome dell'unità non può essere vuoto";
            com.ncfsistemi.econ.db.table.Unita tab = new com.ncfsistemi.econ.db.table.Unita();
            salva(ctx, tab, com.ncfsistemi.econ.db.table.Unita.ID_UNITA, idUnita, val -> val.put(com.ncfsistemi.econ.db.table.Unita.NOME, n));
            return null;
        }, dopo);
    }

    /** Area: nome, livello (piano) e unità a cui appartiene (fra quelle dello stesso cantiere). */
    public static void modificaArea(Context ctx, int idArea, Runnable dopo) {
        ContentValues a = record(ctx, "select a.nome, a.livello, a.id_unita, u.id_cantiere from aree a"
                + " left join unita u on u.id_unita=a.id_unita where a.id_area=" + idArea);
        if (a == null) return;
        Campi c = new Campi(ctx);
        com.google.android.material.textfield.TextInputEditText nome = c.testo("Nome dell'area", a.getAsString("nome"));
        Integer l = a.getAsInteger("livello");
        String[][] voci = com.ncfsistemi.econ.form.AreaForm.vociLivello(l == null ? 0 : l);
        List<String> idLiv = new ArrayList<>(), testiLiv = new ArrayList<>();
        for (String[] v : voci) {
            idLiv.add(v[0]);
            testiLiv.add(v[1]);
        }
        android.widget.Spinner livello = c.scelta("Livello (piano)", testiLiv, idLiv.indexOf(String.valueOf(l == null ? 0 : l)));
        List<Integer> idUnita = new ArrayList<>();
        List<String> nomiUnita = new ArrayList<>();
        for (Object o : elenco(ctx, "select id_unita, nome from unita where id_cantiere=" + intero(a, "id_cantiere") + " order by nome collate nocase")) {
            ContentValues u = (ContentValues) o;
            idUnita.add(intero(u, "id_unita"));
            nomiUnita.add(vuoto(u.getAsString("nome")));
        }
        android.widget.Spinner unita = c.scelta("Unità", nomiUnita, idUnita.indexOf(intero(a, "id_unita")));
        dialogo(ctx, "Area", c, () -> {
            String n = testo(nome);
            if (n.isEmpty()) return "Il nome dell'area non può essere vuoto";
            int liv = Integer.parseInt(idLiv.get(Math.max(0, livello.getSelectedItemPosition())));
            int u = unita.getSelectedItemPosition() >= 0 && !idUnita.isEmpty() ? idUnita.get(unita.getSelectedItemPosition()) : intero(a, "id_unita");
            com.ncfsistemi.econ.db.table.Aree tab = new com.ncfsistemi.econ.db.table.Aree();
            salva(ctx, tab, com.ncfsistemi.econ.db.table.Aree.ID_AREA, idArea, val -> {
                val.put(com.ncfsistemi.econ.db.table.Aree.NOME, n);
                val.put(com.ncfsistemi.econ.db.table.Aree.LIVELLO, liv);
                val.put(com.ncfsistemi.econ.db.table.Aree.ID_UNITA, u);
            });
            return null;
        }, dopo);
    }

    /** Locale: nome e area a cui appartiene (fra tutte le aree del cantiere, "Unità › Area"). */
    public static void modificaLocale(Context ctx, int idLocale, Runnable dopo) {
        ContentValues r = record(ctx, "select l.nome, l.id_area, u.id_cantiere from locali l left join aree a on a.id_area=l.id_area"
                + " left join unita u on u.id_unita=a.id_unita where l.id_locale=" + idLocale);
        if (r == null) return;
        Campi c = new Campi(ctx);
        com.google.android.material.textfield.TextInputEditText nome = c.testo("Nome del locale", r.getAsString("nome"));
        List<Integer> idAree = new ArrayList<>();
        List<String> nomiAree = new ArrayList<>();
        for (Object o : elenco(ctx, "select a.id_area, a.nome, a.livello, u.nome as unita from aree a join unita u on u.id_unita=a.id_unita"
                + " where u.id_cantiere=" + intero(r, "id_cantiere") + " order by u.nome collate nocase, a.livello, a.nome collate nocase")) {
            ContentValues a = (ContentValues) o;
            idAree.add(intero(a, "id_area"));
            nomiAree.add(vuoto(a.getAsString("unita")) + " › " + vuoto(a.getAsString("nome")) + "  (" + intero(a, "livello") + ")");
        }
        android.widget.Spinner area = c.scelta("Area (unità › area, livello)", nomiAree, idAree.indexOf(intero(r, "id_area")));
        dialogo(ctx, "Locale", c, () -> {
            String n = testo(nome);
            if (n.isEmpty()) return "Il nome del locale non può essere vuoto";
            int idArea = area.getSelectedItemPosition() >= 0 && !idAree.isEmpty() ? idAree.get(area.getSelectedItemPosition()) : intero(r, "id_area");
            com.ncfsistemi.econ.db.table.Locali tab = new com.ncfsistemi.econ.db.table.Locali();
            salva(ctx, tab, com.ncfsistemi.econ.db.table.Locali.ID_LOCALE, idLocale, val -> {
                val.put(com.ncfsistemi.econ.db.table.Locali.NOME, n);
                val.put(com.ncfsistemi.econ.db.table.Locali.ID_AREA, idArea);
            });
            return null;
        }, dopo);
    }

    // ------------------------------------------------------------------ nuovi: un solo "+" che apre la scelta

    /** Proposta del padre per i nuovi: {id unità, id area} della riga scelta (0 = nessuna). */
    public interface Proposta {
        int[] padri();
    }

    /** Il ruolo può inserire unità, aree e locali. */
    public static boolean puoInserire(Context ctx) {
        return com.ncfsistemi.econ.utils.FunzionalitaApp.puoInserire(ctx, new com.ncfsistemi.econ.db.table.Unita(), null);
    }

    /**
     * Finestra del "+": tre riquadri grandi con l'icona (Unità, Area, Locale); toccandone uno si apre l'inserimento,
     * con unità/area proposte dalla riga scelta.
     */
    public static void nuovo(Context ctx, int idCantiere, Proposta proposta, Runnable dopo) {
        float dp = ctx.getResources().getDisplayMetrics().density;
        LinearLayout riga = new LinearLayout(ctx);
        riga.setOrientation(LinearLayout.HORIZONTAL);
        riga.setPadding(Math.round(16 * dp), Math.round(8 * dp), Math.round(16 * dp), Math.round(4 * dp));
        String[][] voci = { { FaIcone.UNITA, "Unità", "del cantiere" }, { FaIcone.AREA, "Area", "di un'unità" },
                { FaIcone.LOCALE, "Locale", "di un'area" } };
        androidx.appcompat.app.AlertDialog[] d = new androidx.appcompat.app.AlertDialog[1];
        for (int i = 0; i < voci.length; i++) {
            LinearLayout t = new LinearLayout(ctx);
            t.setOrientation(LinearLayout.VERTICAL);
            t.setGravity(Gravity.CENTER);
            t.setPadding(Math.round(6 * dp), Math.round(14 * dp), Math.round(6 * dp), Math.round(12 * dp));
            GradientDrawable fondo = new GradientDrawable();
            fondo.setColor(Color.WHITE);
            fondo.setStroke(Math.round(1.5f * dp), 0xFFBBDEFB);
            fondo.setCornerRadius(14 * dp);
            t.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(BLU_CHIARO), fondo, null));
            TextView ic = new TextView(ctx);
            FaIcone.applica(ic, voci[i][0], null);
            ic.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            ic.setTextColor(BLU);
            ic.setGravity(Gravity.CENTER);
            GradientDrawable cerchio = new GradientDrawable();
            cerchio.setShape(GradientDrawable.OVAL);
            cerchio.setColor(BLU_CHIARO);
            ic.setBackground(cerchio);
            t.addView(ic, new LinearLayout.LayoutParams(Math.round(56 * dp), Math.round(56 * dp)));
            TextView nome = new TextView(ctx);
            nome.setText(voci[i][1]);
            nome.setTextColor(TESTO);
            nome.setTypeface(Typeface.DEFAULT_BOLD);
            nome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            nome.setGravity(Gravity.CENTER);
            nome.setPadding(0, Math.round(8 * dp), 0, 0);
            t.addView(nome);
            TextView sotto = new TextView(ctx);
            sotto.setText(voci[i][2]);
            sotto.setTextColor(TESTO2);
            sotto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            sotto.setGravity(Gravity.CENTER);
            t.addView(sotto);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = Math.round(10 * dp);
            riga.addView(t, lp);
            final int quale = i;
            t.setOnClickListener(v -> {
                if (d[0] != null) d[0].dismiss();
                int[] p = proposta == null ? new int[] { 0, 0 } : proposta.padri();
                if (quale == 0) nuovaUnita(ctx, idCantiere, dopo);
                else if (quale == 1) nuovaArea(ctx, idCantiere, p[0], dopo);
                else nuovoLocale(ctx, idCantiere, p[1], dopo);
            });
        }
        d[0] = new com.ncfsistemi.econ.dialogo.EconDialogo.Builder(ctx)
                .setTitle("Aggiungi alla struttura")
                .setView(riga)
                .setNegativeButton(R.string.annulla, null)
                .create();
        d[0].show();
    }

    /** Padri da proporre per la riga scelta (tipo e id di CantiereSplitActivity): {id unità, id area}. */
    public static int[] padriDi(Context ctx, int tipo, int id) {
        if (tipo == CantiereSplitActivity.UNITA) return new int[] { id, 0 };
        if (tipo == CantiereSplitActivity.AREA) {
            ContentValues a = record(ctx, "select id_unita from aree where id_area=" + id);
            return new int[] { a == null ? 0 : intero(a, "id_unita"), id };
        }
        if (tipo == CantiereSplitActivity.LOCALE) {
            ContentValues l = record(ctx, "select a.id_unita, l.id_area from locali l left join aree a on a.id_area=l.id_area where l.id_locale=" + id);
            return l == null ? new int[] { 0, 0 } : new int[] { intero(l, "id_unita"), intero(l, "id_area") };
        }
        return new int[] { 0, 0 };
    }

    public static void nuovaUnita(Context ctx, int idCantiere, Runnable dopo) {
        Campi c = new Campi(ctx);
        com.google.android.material.textfield.TextInputEditText nome = c.testo("Nome dell'unità", "");
        dialogo(ctx, "Nuova unità", c, () -> {
            String n = testo(nome);
            if (n.isEmpty()) return "Scrivi il nome dell'unità";
            return inserisci(ctx, new com.ncfsistemi.econ.db.table.Unita(), new com.ncfsistemi.econ.db.table.Cantieri(),
                    com.ncfsistemi.econ.db.table.Cantieri.ID_CANTIERE, idCantiere, val -> val.put(com.ncfsistemi.econ.db.table.Unita.NOME, n));
        }, dopo);
    }

    public static void nuovaArea(Context ctx, int idCantiere, int idUnitaProposta, Runnable dopo) {
        List<Integer> idUnita = new ArrayList<>();
        List<String> nomiUnita = new ArrayList<>();
        for (Object o : elenco(ctx, "select id_unita, nome from unita where id_cantiere=" + idCantiere + " order by nome collate nocase")) {
            ContentValues u = (ContentValues) o;
            idUnita.add(intero(u, "id_unita"));
            nomiUnita.add(vuoto(u.getAsString("nome")));
        }
        if (idUnita.isEmpty()) {
            com.ncfsistemi.econ.dialogo.EconDialogo.attenzione(ctx, "Nuova area", "Crea prima un'unità del cantiere: l'area sta dentro un'unità.").show();
            return;
        }
        Campi c = new Campi(ctx);
        com.google.android.material.textfield.TextInputEditText nome = c.testo("Nome dell'area", "");
        String[][] voci = com.ncfsistemi.econ.form.AreaForm.vociLivello();
        List<String> idLiv = new ArrayList<>(), testiLiv = new ArrayList<>();
        for (String[] v : voci) {
            idLiv.add(v[0]);
            testiLiv.add(v[1]);
        }
        android.widget.Spinner livello = c.scelta("Livello (piano)", testiLiv, Math.max(0, idLiv.indexOf("0")));
        android.widget.Spinner unita = c.scelta("Unità", nomiUnita, Math.max(0, idUnita.indexOf(idUnitaProposta)));
        dialogo(ctx, "Nuova area", c, () -> {
            String n = testo(nome);
            if (n.isEmpty()) return "Scrivi il nome dell'area";
            int liv = idLiv.isEmpty() ? 0 : Integer.parseInt(idLiv.get(Math.max(0, livello.getSelectedItemPosition())));
            int u = idUnita.get(Math.max(0, unita.getSelectedItemPosition()));
            return inserisci(ctx, new com.ncfsistemi.econ.db.table.Aree(), new com.ncfsistemi.econ.db.table.Unita(),
                    com.ncfsistemi.econ.db.table.Unita.ID_UNITA, u, val -> {
                        val.put(com.ncfsistemi.econ.db.table.Aree.NOME, n);
                        val.put(com.ncfsistemi.econ.db.table.Aree.LIVELLO, liv);
                    });
        }, dopo);
    }

    public static void nuovoLocale(Context ctx, int idCantiere, int idAreaProposta, Runnable dopo) {
        List<Integer> idAree = new ArrayList<>();
        List<String> nomiAree = new ArrayList<>();
        for (Object o : elenco(ctx, "select a.id_area, a.nome, a.livello, u.nome as unita from aree a join unita u on u.id_unita=a.id_unita"
                + " where u.id_cantiere=" + idCantiere + " order by u.nome collate nocase, a.livello, a.nome collate nocase")) {
            ContentValues a = (ContentValues) o;
            idAree.add(intero(a, "id_area"));
            nomiAree.add(vuoto(a.getAsString("unita")) + " › " + vuoto(a.getAsString("nome")) + "  (" + intero(a, "livello") + ")");
        }
        if (idAree.isEmpty()) {
            com.ncfsistemi.econ.dialogo.EconDialogo.attenzione(ctx, "Nuovo locale", "Crea prima un'area: il locale sta dentro un'area.").show();
            return;
        }
        Campi c = new Campi(ctx);
        com.google.android.material.textfield.TextInputEditText nome = c.testo("Nome del locale", "");
        android.widget.Spinner area = c.scelta("Area (unità › area, livello)", nomiAree, Math.max(0, idAree.indexOf(idAreaProposta)));
        dialogo(ctx, "Nuovo locale", c, () -> {
            String n = testo(nome);
            if (n.isEmpty()) return "Scrivi il nome del locale";
            int a = idAree.get(Math.max(0, area.getSelectedItemPosition()));
            return inserisci(ctx, new com.ncfsistemi.econ.db.table.Locali(), new com.ncfsistemi.econ.db.table.Aree(),
                    com.ncfsistemi.econ.db.table.Aree.ID_AREA, a, val -> val.put(com.ncfsistemi.econ.db.table.Locali.NOME, n));
        }, dopo);
    }

    /**
     * Nuovo record (id negativo, log di inserimento, va al server con la sync) sotto il padre: linea e placca proposte
     * dal padre, come nella maschera. Null se tutto bene, altrimenti il motivo (con il messaggio del database).
     */
    private static String inserisci(Context ctx, com.ncfsistemi.econ.db.table.AbstractTable tab, com.ncfsistemi.econ.db.table.AbstractTable padre,
                                    String colonnaPadre, int idPadre, java.util.function.Consumer<ContentValues> valori) {
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues val = tab.getValoriLogInserimento(db);
            val.put(colonnaPadre, idPadre);
            ContentValues where = new ContentValues();
            where.put(colonnaPadre, idPadre);
            ContentValues p = db.getRecord(padre, where);
            val.put("id_linea", p != null && p.getAsInteger("id_linea") != null ? p.getAsInteger("id_linea") : 0);
            val.put("id_placca", p != null && p.getAsInteger("id_placca") != null ? p.getAsInteger("id_placca") : 0);
            valori.accept(val);
            if (tab.inserisciRecord(db, val) == -1) return "Non salvato: il database ha rifiutato il record.";
            return null;
        } catch (Exception e) {
            android.util.Log.e("Econ", "StrutturaCantiere inserisci " + tab.getNomeTabella(), e);
            return "Non salvato (" + tab.getNomeTabella() + "): " + e.getMessage();
        } finally {
            db.close();
        }
    }

    /** Campi della finestra di modifica, in colonna. */
    private static final class Campi {
        final Context ctx;
        final float dp;
        final LinearLayout v;

        Campi(Context ctx) {
            this.ctx = ctx;
            this.dp = ctx.getResources().getDisplayMetrics().density;
            v = new LinearLayout(ctx);
            v.setOrientation(LinearLayout.VERTICAL);
            v.setPadding(Math.round(24 * dp), Math.round(8 * dp), Math.round(24 * dp), 0);
        }

        com.google.android.material.textfield.TextInputEditText testo(String etichetta, String valore) {
            com.google.android.material.textfield.TextInputLayout til = new com.google.android.material.textfield.TextInputLayout(ctx, null,
                    com.google.android.material.R.attr.textInputOutlinedStyle);
            til.setHint(etichetta);
            com.google.android.material.textfield.TextInputEditText e = new com.google.android.material.textfield.TextInputEditText(til.getContext());
            e.setSingleLine(true);
            e.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            e.setText(valore == null ? "" : valore.trim());
            til.addView(e);
            v.addView(til);
            return e;
        }

        android.widget.Spinner scelta(String etichetta, List<String> voci, int selezione) {
            TextView et = new TextView(ctx);
            et.setText(etichetta);
            et.setTextColor(TESTO2);
            et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            et.setPadding(0, Math.round(14 * dp), 0, Math.round(4 * dp));
            v.addView(et);
            android.widget.Spinner sp = new android.widget.Spinner(ctx);
            sp.setAdapter(new android.widget.ArrayAdapter<>(ctx, R.layout.econ_tendina_valore, voci));
            if (selezione >= 0) sp.setSelection(selezione);
            v.addView(sp, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(48 * dp)));
            return sp;
        }
    }

    /** Conferma: salva() torna null se tutto bene, altrimenti il messaggio (e la finestra resta aperta). */
    private static void dialogo(Context ctx, String titolo, Campi c, java.util.function.Supplier<String> salva, Runnable dopo) {
        androidx.appcompat.app.AlertDialog d = new com.ncfsistemi.econ.dialogo.EconDialogo.Builder(ctx)
                .setTitle(titolo)
                .setView(c.v)
                .setNegativeButton(R.string.annulla, null)
                .setPositiveButton(R.string.conferma, null)
                .create();
        d.setOnShowListener(x -> d.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(b -> {
            String errore = salva.get();
            if (errore != null) {
                com.ncfsistemi.econ.dialogo.EconDialogo.attenzione(ctx, titolo, errore).show();
                return;
            }
            d.dismiss();
            if (dopo != null) dopo.run();
        }));
        d.show();
    }

    private static void salva(Context ctx, com.ncfsistemi.econ.db.table.AbstractTable tab, String chiave, int id,
                              java.util.function.Consumer<ContentValues> valori) {
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues val = tab.getValoriLogModifica(db);
            valori.accept(val);
            ContentValues where = new ContentValues();
            where.put(chiave, id);
            tab.aggiornaRecord(db, val, where);
        } finally {
            db.close();
        }
    }

    private static ContentValues record(Context ctx, String sql) {
        DbInterno db = new DbInterno(ctx);
        try {
            return db.getRecord(sql);
        } finally {
            db.close();
        }
    }

    private static List<Object> elenco(Context ctx, String sql) {
        DbInterno db = new DbInterno(ctx);
        try {
            return db.eseguiSelect(sql, null);
        } finally {
            db.close();
        }
    }

    private static int intero(ContentValues v, String k) {
        Integer i = v.getAsInteger(k);
        return i == null ? 0 : i;
    }

    private static String vuoto(String s) {
        return s == null || s.trim().isEmpty() ? "—" : s.trim();
    }

    private static String testo(com.google.android.material.textfield.TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
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

    // ------------------------------------------------------------------ barra dei comandi

    /**
     * Barra in cima alla struttura (scheda cantiere, disegno, schermo intero), solo icone: a sinistra l'ordine "Per
     * livello" e "Locali A-Z" (si ricordano, valgono ovunque), a destra "+" (nuova unità/area/locale, se il ruolo può
     * inserire e {@code idCantiere} > 0), matita (modalità modifica) e "schermo intero" (se {@code ingrandisci} non è
     * null). {@code cambiato} rilegge le righe dopo ogni cambio.
     */
    public static LinearLayout barra(Context ctx, Runnable cambiato, int idCantiere, Proposta proposta, Runnable ingrandisci) {
        float dp = ctx.getResources().getDisplayMetrics().density;
        LinearLayout barra = new LinearLayout(ctx);
        barra.setOrientation(LinearLayout.HORIZONTAL);
        barra.setGravity(Gravity.CENTER_VERTICAL);
        barra.setPadding(Math.round(8 * dp), Math.round(6 * dp), Math.round(8 * dp), Math.round(6 * dp));
        MaterialButton livelli = icona(ctx, FaIcone.AREA, "Aree per livello, dal piano più basso (scantinato) al più alto");
        MaterialButton az = icona(ctx, ORDINE_AZ, "Locali di ogni area in ordine alfabetico");
        barra.addView(livelli, quadrato(ctx, false));
        barra.addView(az, quadrato(ctx, true));
        barra.addView(new View(ctx), new LinearLayout.LayoutParams(0, 1, 1f));
        if (idCantiere > 0 && puoInserire(ctx)) {
            MaterialButton piu = icona(ctx, FaIcone.PIU, "Aggiungi unità, area o locale");
            barra.addView(piu, quadrato(ctx, true));
            stato(piu, true);
            piu.setOnClickListener(v -> nuovo(ctx, idCantiere, proposta, cambiato));
        }
        MaterialButton matita = null;
        if (puoModificare(ctx)) {
            // matita: modalità modifica (nomi, livelli, unità e aree collegate) su tutte le righe
            MaterialButton m = icona(ctx, FaIcone.MODIFICA, "Modifica nomi, livelli e collegamenti della struttura");
            matita = m;
            barra.addView(m, quadrato(ctx, true));
            m.setOnClickListener(v -> {
                inModifica = !inModifica;
                stato(m, inModifica);
                if (inModifica) {
                    android.widget.Toast.makeText(ctx, "Tocca la matita su unità, area o locale per modificarli", android.widget.Toast.LENGTH_SHORT).show();
                }
                cambiato.run();
            });
        }
        if (ingrandisci != null) {
            MaterialButton grande = icona(ctx, SCHERMO_INTERO, "Struttura a tutto schermo");
            stato(grande, false);
            barra.addView(grande, quadrato(ctx, true));
            grande.setOnClickListener(v -> ingrandisci.run());
        }
        SharedPreferences pref = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        final MaterialButton mat = matita;
        Runnable stati = () -> {
            stato(livelli, perLivello(ctx));
            stato(az, localiAZ(ctx));
            if (mat != null) stato(mat, inModifica);
        };
        stati.run();
        // tornando dallo schermo intero (o da un'altra barra) i pulsanti riprendono lo stato comune
        barra.getViewTreeObserver().addOnWindowFocusChangeListener(focus -> {
            if (focus) stati.run();
        });
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

    private static final String ORDINE_AZ = "\uf15d";      // sort-alpha-down
    private static final String SCHERMO_INTERO = "\uf065"; // expand

    private static MaterialButton icona(Context ctx, String glifo, String suggerimento) {
        MaterialButton b = bottone(ctx, "", glifo, suggerimento);
        float dp = ctx.getResources().getDisplayMetrics().density;
        b.setIcon(FaIcone.drawable(ctx, glifo, 18));
        b.setIconPadding(0);
        b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        b.setPadding(0, 0, 0, 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(Math.round(40 * dp));
        b.setMinimumHeight(Math.round(40 * dp));
        b.setCornerRadius(Math.round(12 * dp));
        return b;
    }

    private static LinearLayout.LayoutParams quadrato(Context ctx, boolean spazioPrima) {
        float dp = ctx.getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.round(44 * dp), Math.round(40 * dp));
        if (spazioPrima) lp.leftMargin = Math.round(6 * dp);
        return lp;
    }

    // ------------------------------------------------------------------ schermo intero

    /**
     * La struttura in una finestra a tutto schermo (barra blu con la X, stessa barra dei comandi senza "schermo
     * intero", stesse righe): toccando una riga attiva si chiude e la maschera fa quello che fa nel pannello
     * ({@code scelta} con la posizione). {@code dati} è la stessa lista del pannello (riletta da {@code cambiato}).
     */
    public static void schermoIntero(Context ctx, CharSequence titolo, CharSequence sottotitolo, List<Object> dati, Selezione selezione,
            Runnable cambiato, int idCantiere, Proposta proposta, Runnable dopoModifica, java.util.function.IntConsumer scelta) {
        float dp = ctx.getResources().getDisplayMetrics().density;
        android.app.Dialog dlg = new android.app.Dialog(ctx, R.style.AppTheme);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(SFONDO);
        root.addView(com.ncfsistemi.econ.dialogo.EconDialogo.testataFinestra(ctx, titolo, sottotitolo, dlg::dismiss));
        ListView lista = new ListView(ctx);
        Adapter adapter = new Adapter(ctx, dati, selezione);
        Runnable rilegge = () -> {
            cambiato.run();
            adapter.notifyDataSetChanged();
        };
        LinearLayout barra = barra(ctx, rilegge, idCantiere, proposta, null);
        barra.setBackgroundColor(Color.WHITE);
        barra.setPadding(Math.round(12 * dp), Math.round(8 * dp), Math.round(12 * dp), Math.round(8 * dp));
        root.addView(barra);
        View sep = new View(ctx);
        sep.setBackgroundColor(0xFFE0E4EA);
        root.addView(sep, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(dp)));
        stileLista(lista);
        lista.setPadding(Math.round(12 * dp), Math.round(6 * dp), Math.round(12 * dp), Math.round(16 * dp));
        if (dopoModifica != null) {
            adapter.setDopoModifica(() -> {
                dopoModifica.run();
                adapter.notifyDataSetChanged();
            });
        }
        lista.setAdapter(adapter);
        lista.setOnItemClickListener((parent, view, pos, id) -> {
            if (!selezione.attiva(pos, (ContentValues) dati.get(pos))) return;
            dlg.dismiss();
            scelta.accept(pos);
        });
        root.addView(lista, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        dlg.setContentView(root);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dlg.getWindow().setStatusBarColor(BLU);
        }
        dlg.show();
        // la riga scelta si vede subito
        for (int i = 0; i < dati.size(); i++) {
            if (selezione.selezionata(i, (ContentValues) dati.get(i))) {
                final int pos = i;
                lista.post(() -> lista.setSelection(Math.max(0, pos - 3)));
                break;
            }
        }
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
        /** Dopo la modifica di unità, area o locale (matita in modalità modifica): la maschera rilegge. Null = niente matita. */
        private Runnable dopoModifica;

        public void setDopoModifica(Runnable r) {
            this.dopoModifica = r;
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
                FaIcone.applica(h.matita, FaIcone.MODIFICA, "Modifica");
                h.matita.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                h.matita.setTextColor(BLU);
                h.matita.setContentDescription("Modifica");
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
            if (inModifica && dopoModifica != null && (tipo == CantiereSplitActivity.UNITA || tipo == CantiereSplitActivity.AREA
                    || tipo == CantiereSplitActivity.LOCALE) && puoModificare(ctx)) {
                int id = r.getAsInteger(ID);
                h.matita.setVisibility(View.VISIBLE);
                h.matita.setOnClickListener(v -> modifica(ctx, tipo, id, dopoModifica));
            }
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
