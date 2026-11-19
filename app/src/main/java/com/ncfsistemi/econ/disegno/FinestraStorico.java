package com.ncfsistemi.econ.disegno;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ncfsistemi.econ.api.MercuryApiClient;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.CollegamentiDisegno;
import com.ncfsistemi.econ.utils.RigheRapportino;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Finestra "Storico" del disegno (STUDIO_STORICO_DISEGNO.md §8.2, S9): lo storico non sta sul tablet, si chiede al server
 * per periodo (ultima settimana, mese, 3 mesi, tutto, oppure dal… al…). Per un oggetto mostra anche, dai dati del
 * tablet, stato, data di posa e di assistenza e documenti collegati, che si vedono anche senza rete.
 */
public final class FinestraStorico {

    private static final int BLU = 0xFF1565C0;
    private static final String[][] PERIODI = {{"7", "Ultima settimana"}, {"30", "Ultimo mese"}, {"90", "3 mesi"}, {"tutto", "Tutto"}};

    private final Activity act;
    private final int idCantiere;
    private final String oggettoTipo;
    private final int idOggetto;
    private final String titolo;
    private final float d;
    private LinearLayout elenco;
    private final List<TextView> scelte = new ArrayList<>();
    private String periodo = "30";
    private String dal = null, al = null;
    private TextView pulsanteDate;

    private FinestraStorico(Activity act, int idCantiere, String oggettoTipo, int idOggetto, String titolo) {
        this.act = act;
        this.idCantiere = idCantiere;
        this.oggettoTipo = oggettoTipo;
        this.idOggetto = idOggetto;
        this.titolo = titolo;
        this.d = act.getResources().getDisplayMetrics().density;
    }

    /** Storico di tutto il cantiere. */
    public static void cantiere(Activity act, int idCantiere) {
        new FinestraStorico(act, idCantiere, null, 0, "Storico del disegno").apri();
    }

    /** Storico di un oggetto (ELEMENTO, COMPONENTE, MATERIALE). */
    public static void oggetto(Activity act, int idCantiere, String tipo, int id, String nome) {
        new FinestraStorico(act, idCantiere, tipo, id, "Storico: " + nome).apri();
    }

    private void apri() {
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(px(20), px(4), px(20), px(8));

        if (oggettoTipo != null) root.addView(statoOggetto());

        HorizontalScrollView hs = new HorizontalScrollView(act);
        LinearLayout riga = new LinearLayout(act);
        riga.setOrientation(LinearLayout.HORIZONTAL);
        for (String[] p : PERIODI) {
            TextView t = chip(p[1]);
            t.setOnClickListener(v -> {
                periodo = p[0];
                dal = al = null;
                pulsanteDate.setText("Dal… al…");
                carica();
            });
            t.setTag(p[0]);
            scelte.add(t);
            riga.addView(t);
        }
        pulsanteDate = chip("Dal… al…");
        pulsanteDate.setOnClickListener(v -> scegliDate());
        riga.addView(pulsanteDate);
        hs.addView(riga);
        root.addView(hs);

        elenco = new LinearLayout(act);
        elenco.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(act);
        sv.addView(elenco);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(420));
        lp.topMargin = px(8);
        root.addView(sv, lp);

        new com.ncfsistemi.econ.dialogo.EconDialogo.Builder(act).setTitle(titolo).setView(root).setPositiveButton("Chiudi", null).show();
        carica();
    }

    /** Dati dell'oggetto sul tablet: stato, date, documenti collegati (anche senza rete). */
    private TextView statoOggetto() {
        StringBuilder sb = new StringBuilder();
        DbInterno db = new DbInterno(act);
        try {
            String tab = CollegamentiDisegno.tabellaOggetto(oggettoTipo);
            ContentValues o = tab == null ? null : db.getRecord("select * from " + tab + " where " + CollegamentiDisegno.pkOggetto(oggettoTipo) + "=" + idOggetto);
            if (o != null) {
                String stato = RigheRapportino.testo(o, "stato");
                sb.append("Stato: ").append(stato.isEmpty() ? "previsto" : stato.toLowerCase(Locale.ITALIAN));
                String posa = RigheRapportino.testo(o, "data_posa"), ass = RigheRapportino.testo(o, "data_assistenza");
                if (!posa.isEmpty()) sb.append(" · posato il ").append(data(posa));
                if (!ass.isEmpty()) sb.append(" · ultima assistenza ").append(data(ass));
            }
            List<String> docs = new ArrayList<>();
            for (Object x : db.eseguiSelect("select * from " + CollegamentiDisegno.NOME_TABELLA + " where " + CollegamentiDisegno.OGGETTO_TIPO
                    + "='" + oggettoTipo + "' and " + CollegamentiDisegno.ID_OGGETTO + "=" + idOggetto, null)) {
                ContentValues c = (ContentValues) x;
                docs.add(documento(db, RigheRapportino.testo(c, CollegamentiDisegno.DOCUMENTO_TIPO), RigheRapportino.intero(c, CollegamentiDisegno.ID_DOCUMENTO))
                        + " (" + RigheRapportino.testo(c, CollegamentiDisegno.RUOLO).toLowerCase(Locale.ITALIAN) + ")");
            }
            sb.append(sb.length() > 0 ? "\n" : "").append(docs.isEmpty() ? "Nessun documento collegato" : "Documenti: " + android.text.TextUtils.join(", ", docs));
        } finally {
            db.close();
        }
        TextView t = new TextView(act);
        t.setText(sb.toString());
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setTextColor(0xFF37474F);
        t.setPadding(px(10), px(8), px(10), px(8));
        t.setBackground(sfondo(0xFFE3F2FD, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = px(8);
        t.setLayoutParams(lp);
        return t;
    }

    private static String documento(DbInterno db, String tipo, int id) {
        String etichetta = tipo.isEmpty() ? "Documento" : tipo.charAt(0) + tipo.substring(1).toLowerCase(Locale.ITALIAN);
        ContentValues r = null;
        if (CollegamentiDisegno.RAPPORTINO.equals(tipo)) {
            r = db.getRecord("select numero, anno from rapportini where id=" + id);
        } else if (CollegamentiDisegno.ORDINE.equals(tipo) || CollegamentiDisegno.PREVENTIVO.equals(tipo)) {
            r = db.getRecord("select numero, anno from preventivi where id_preventivo=" + id);
        }
        int numero = r != null ? RigheRapportino.intero(r, "numero") : 0;
        return etichetta + (numero > 0 ? " " + numero + "/" + RigheRapportino.intero(r, "anno") : id < 0 ? " (da inviare)" : " #" + id);
    }

    private void scegliDate() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(act, (v1, y1, m1, g1) -> {
            String primo = String.format(Locale.ITALIAN, "%04d-%02d-%02d", y1, m1 + 1, g1);
            new DatePickerDialog(act, (v2, y2, m2, g2) -> {
                dal = primo;
                al = String.format(Locale.ITALIAN, "%04d-%02d-%02d", y2, m2 + 1, g2);
                if (al.compareTo(dal) < 0) {
                    String t = dal;
                    dal = al;
                    al = t;
                }
                periodo = null;
                pulsanteDate.setText(data(dal) + " – " + data(al));
                carica();
            }, y1, m1, g1).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void carica() {
        for (TextView t : scelte) evidenzia(t, t.getTag().equals(periodo));
        evidenzia(pulsanteDate, periodo == null);
        elenco.removeAllViews();
        ProgressBar pb = new ProgressBar(act);
        elenco.addView(pb);
        Map<String, String> q = new HashMap<>();
        q.put("cantiere", String.valueOf(idCantiere));
        if (oggettoTipo != null) {
            q.put("oggetto_tipo", oggettoTipo);
            q.put("id_oggetto", String.valueOf(idOggetto));
        }
        if (periodo != null) q.put("periodo", periodo);
        if (dal != null) q.put("dal", dal);
        if (al != null) q.put("al", al);
        if (idCantiere <= 0 || idOggetto < 0) {
            messaggio("Il cantiere o l'oggetto non e' ancora stato inviato in sede: lo storico c'e' dopo la sincronizzazione.");
            return;
        }
        new Thread(() -> {
            String errore = null;
            JsonObject corpo = null;
            try {
                retrofit2.Response<JsonObject> r = MercuryApiClient.getInstance(act).getService().getStoricoDisegno(q).execute();
                if (r.isSuccessful()) corpo = r.body();
                else errore = r.code() == 403 ? "Storico non concesso dalla licenza." : "Il server non risponde (" + r.code() + ").";
            } catch (Exception e) {
                errore = "Lo storico si vede con la connessione: ora non c'e' rete.";
            }
            final String err = errore;
            final JsonObject ok = corpo;
            act.runOnUiThread(() -> {
                if (act.isFinishing()) return;
                if (ok == null) messaggio(err);
                else mostra(ok);
            });
        }).start();
    }

    private void mostra(JsonObject corpo) {
        elenco.removeAllViews();
        JsonArray eventi = corpo.has("eventi") ? corpo.getAsJsonArray("eventi") : new JsonArray();
        if (eventi.size() == 0) {
            messaggio("Nessun evento nel periodo scelto.");
            return;
        }
        String giorno = "";
        for (JsonElement el : eventi) {
            JsonObject e = el.getAsJsonObject();
            String quando = testo(e, "data");
            String g = quando.length() >= 10 ? quando.substring(0, 10) : quando;
            if (!g.equals(giorno)) {
                giorno = g;
                TextView h = new TextView(act);
                h.setText(data(g));
                h.setTypeface(Typeface.DEFAULT_BOLD);
                h.setTextColor(BLU);
                h.setPadding(0, px(10), 0, px(2));
                elenco.addView(h);
            }
            TextView t = new TextView(act);
            String ora = quando.length() >= 16 ? quando.substring(11, 16) : "";
            String chi = testo(e, "utente");
            t.setText(ora + "  " + testo(e, "descrizione") + (chi.isEmpty() ? "" : "  ·  " + chi));
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            t.setTextColor(0xFF263238);
            t.setPadding(px(8), px(4), 0, px(4));
            elenco.addView(t);
        }
        if (corpo.has("troppi") && corpo.get("troppi").getAsBoolean()) {
            messaggioAggiunto("Mostrati gli ultimi 500 eventi: restringi il periodo per vedere gli altri.");
        }
    }

    private void messaggio(String m) {
        elenco.removeAllViews();
        messaggioAggiunto(m);
    }

    private void messaggioAggiunto(String m) {
        TextView t = new TextView(act);
        t.setText(m);
        t.setTextColor(0xFF607D8B);
        t.setPadding(0, px(12), 0, px(12));
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        elenco.addView(t);
    }

    private TextView chip(String testo) {
        TextView t = new TextView(act);
        t.setText(testo);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setPadding(px(12), px(6), px(12), px(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = px(6);
        t.setLayoutParams(lp);
        evidenzia(t, false);
        return t;
    }

    private void evidenzia(TextView t, boolean si) {
        t.setTextColor(si ? Color.WHITE : BLU);
        t.setBackground(sfondo(si ? BLU : 0xFFE3F2FD, 16));
    }

    private GradientDrawable sfondo(int colore, int raggio) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(colore);
        g.setCornerRadius(px(raggio));
        return g;
    }

    private static String testo(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    /** 2026-11-15 → 15/11/2026 */
    private static String data(String iso) {
        return iso.length() >= 10 ? iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4) : iso;
    }

    private int px(int dp) {
        return (int) (dp * d);
    }
}
