package com.ncfsistemi.econ.disegno;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ncfsistemi.econ.RapportinoDettaglioModActivity;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.CollegamentiDisegno;
import com.ncfsistemi.econ.db.table.ElementiCantiere;
import com.ncfsistemi.econ.db.table.ElementiCodici;
import com.ncfsistemi.econ.db.table.Listini;
import com.ncfsistemi.econ.db.table.RapportiniDettaglio;
import com.ncfsistemi.econ.db.table.RapportiniDettaglioTipi;
import com.ncfsistemi.econ.db.table.UnitaMisura;
import com.ncfsistemi.econ.utils.AssistenzeLocali;
import com.ncfsistemi.econ.utils.RigheRapportino;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * "Rapportino dal disegno" (STUDIO_STORICO_DISEGNO.md §8.1): tutto quello che e' posato sul disegno del cantiere,
 * contabilizzato e senza un rapportino, finisce in un rapportino nuovo gia' compilato (cantiere, cliente, data di oggi,
 * righe di materiale articolo × quantita'). Gli oggetti con lo stesso articolo vanno in una riga sola; un articolo in
 * assistenza a tempo porta i suoi mesi (l'assistenza parte alla conferma, dalla data del rapportino). Al salvataggio
 * ogni oggetto ancora coperto dalla sua riga riceve il collegamento al rapportino (ruolo POSATO): solo allora.
 * Gli oggetti senza articolo si segnano posati con il rapportino, senza riga.
 */
public final class RapportinoDalDisegno {

    /** Extra della maschera del rapportino: cantiere e oggetti scelti ("TIPO:id,TIPO:id"). */
    public static final String EXTRA_CANTIERE = "DISEGNO_CANTIERE";
    public static final String EXTRA_OGGETTI = "DISEGNO_OGGETTI";
    /** Chiave nascosta della riga in memoria: oggetti coperti "TIPO:id:quantita;..." (non e' un campo della tabella). */
    public static final String OGGETTI_RIGA = "_oggetti_disegno";

    private RapportinoDalDisegno() {
    }

    /** Oggetto posato da mettere nel rapportino. */
    public static final class Oggetto {
        public String tipo;
        public int id;
        public String nome = "";
        public String locale = "";
        public int idListino;
        public String articolo = "";
        public double quantita = 1;
        public String um = "";

        String chiave() {
            return tipo + ":" + id;
        }
    }

    /** Quanti oggetti aspettano un rapportino (per il pulsante). */
    public static int conta(Context ctx, int idCantiere) {
        return raccogli(ctx, idCantiere).size();
    }

    /**
     * Oggetti del cantiere contabilizzati, non dismessi e senza collegamento a un rapportino: elementi (non le copie dentro
     * i documenti), componenti e materiale degli elementi. In ordine di locale e nome.
     */
    public static List<Oggetto> raccogli(Context ctx, int idCantiere) {
        List<Oggetto> out = new ArrayList<>();
        if (idCantiere == 0) return out;
        DbInterno db = new DbInterno(ctx);
        try {
            String senzaRapportino = " and not exists (select 1 from " + CollegamentiDisegno.NOME_TABELLA + " x where x.oggetto_tipo = '%s'"
                    + " and x.id_oggetto = %s and x.documento_tipo = '" + CollegamentiDisegno.RAPPORTINO + "')";
            String attivo = " and coalesce(%s.non_conteggiare_preventivi, 0) = 0 and coalesce(%s.stato, '') <> '" + CollegamentiDisegno.STATO_DISMESSO + "'";
            String elementi = "select e.id_elemento_cant id, e.nome_elemento_cant nome, e.id_elemento, e.id_articolo_ditta, e.quantita,"
                    + " coalesce((select u.unita_misura from unita_misura u where u.id = e.id_unita_misura), e.unita_misura) um,"
                    + " coalesce(l.nome, '') locale from elementi_cantiere e left join locali l on l.id_locale = e.id_locale"
                    + " where e.id_cantiere = " + idCantiere + " and coalesce(e.id_elemento_cant_origine, 0) = 0"
                    + String.format(attivo, "e", "e") + String.format(senzaRapportino, CollegamentiDisegno.ELEMENTO, "e.id_elemento_cant")
                    + " order by locale, nome";
            for (Object o : db.eseguiSelect(elementi, null)) {
                ContentValues r = (ContentValues) o;
                Oggetto x = base(r, CollegamentiDisegno.ELEMENTO);
                int idArticolo = RigheRapportino.intero(r, ElementiCantiere.ID_ARTICOLO_DITTA);
                x.idListino = idArticolo != 0 ? idArticolo : articoloDaCodici(db, ElementiCodici.ID_ELEMENTO, RigheRapportino.intero(r, ElementiCantiere.ID_ELEMENTO));
                double q = RigheRapportino.decimale(r, ElementiCantiere.QUANTITA);
                if (idArticolo != 0 && q > 0) x.quantita = q;
                out.add(completa(db, x));
            }
            String componenti = "select c.id_componente_cant id, c.nome_componente_cant nome, c.id_componente, coalesce(l.nome, '') locale"
                    + " from componenti_cantiere c left join locali l on l.id_locale = c.id_locale where c.id_cantiere = " + idCantiere
                    + String.format(attivo, "c", "c") + String.format(senzaRapportino, CollegamentiDisegno.COMPONENTE, "c.id_componente_cant")
                    + " order by locale, nome";
            for (Object o : db.eseguiSelect(componenti, null)) {
                ContentValues r = (ContentValues) o;
                Oggetto x = base(r, CollegamentiDisegno.COMPONENTE);
                x.idListino = articoloDaCodici(db, ElementiCodici.ID_COMPONENTE, RigheRapportino.intero(r, "id_componente"));
                out.add(completa(db, x));
            }
            String materiale = "select m.id, m.descrizione nome, m.id_listino, m.quantita, m.um, coalesce(l.nome, '') locale"
                    + " from elementi_cantiere_materiali m join elementi_cantiere e on e.id_elemento_cant = m.id_elemento_cant"
                    + " left join locali l on l.id_locale = e.id_locale where e.id_cantiere = " + idCantiere
                    + " and coalesce(e.id_elemento_cant_origine, 0) = 0"
                    + String.format(attivo, "m", "m") + String.format(senzaRapportino, CollegamentiDisegno.MATERIALE, "m.id")
                    + " order by locale, nome";
            for (Object o : db.eseguiSelect(materiale, null)) {
                ContentValues r = (ContentValues) o;
                Oggetto x = base(r, CollegamentiDisegno.MATERIALE);
                x.idListino = RigheRapportino.intero(r, "id_listino");
                double q = RigheRapportino.decimale(r, "quantita");
                x.quantita = q > 0 ? q : 1;
                x.um = RigheRapportino.testo(r, "um");
                out.add(completa(db, x));
            }
        } finally {
            db.close();
        }
        return out;
    }

    private static Oggetto base(ContentValues r, String tipo) {
        Oggetto x = new Oggetto();
        x.tipo = tipo;
        x.id = RigheRapportino.intero(r, "id");
        x.nome = RigheRapportino.testo(r, "nome");
        x.locale = RigheRapportino.testo(r, "locale");
        return x;
    }

    /** Articolo dal codice associato all'elemento o al componente della libreria (elementi_codici), 0 se non c'e'. */
    private static int articoloDaCodici(DbInterno db, String colonna, int id) {
        if (id == 0) return 0;
        ContentValues c = db.getRecord("select l." + Listini.ID + " from " + ElementiCodici.NOME_TABELLA + " ec join " + Listini.NOME_TABELLA
                + " l on upper(l." + Listini.CODICE_ARTICOLO + ") = upper(ec." + ElementiCodici.CODICE_ARTICOLO + ") where ec." + colonna + " = " + id
                + (ElementiCodici.ID_ELEMENTO.equals(colonna) ? " and coalesce(ec." + ElementiCodici.ID_COMPONENTE + ", 0) = 0" : "")
                + " and coalesce(ec." + ElementiCodici.ID_PLACCA_MODULI + ", 0) = 0 limit 1");
        return c != null ? RigheRapportino.intero(c, Listini.ID) : 0;
    }

    private static Oggetto completa(DbInterno db, Oggetto x) {
        if (x.idListino != 0) {
            ContentValues l = db.getRecord("select * from " + Listini.NOME_TABELLA + " where " + Listini.ID + " = " + x.idListino);
            if (l == null) {
                x.idListino = 0;
            } else {
                String cod = RigheRapportino.testo(l, Listini.CODICE_ARTICOLO);
                x.articolo = (cod.isEmpty() ? "" : cod + " ") + RigheRapportino.testo(l, Listini.DESCRIZIONE);
            }
        }
        if (x.nome.isEmpty()) x.nome = x.articolo.isEmpty() ? (x.tipo.toLowerCase(Locale.ITALIAN) + " " + x.id) : x.articolo;
        return x;
    }

    // ------------------------------------------------------------------ finestra di scelta

    /**
     * Finestra con gli oggetti per locale, tutti spuntati: si toglie quello che non e' ancora montato davvero, poi
     * "Prepara il rapportino" apre la maschera del rapportino gia' compilata (nulla e' salvato finche' non si salva).
     */
    public static void scegli(Activity act, int idCantiere) {
        List<Oggetto> oggetti = raccogli(act, idCantiere);
        if (oggetti.isEmpty()) {
            Toast.makeText(act, "Niente da mettere nel rapportino: tutto quello che e' posato ha gia' un rapportino.", Toast.LENGTH_LONG).show();
            return;
        }
        float d = act.getResources().getDisplayMetrics().density;
        LinearLayout lista = new LinearLayout(act);
        lista.setOrientation(LinearLayout.VERTICAL);
        lista.setPadding((int) (20 * d), (int) (4 * d), (int) (20 * d), (int) (8 * d));
        TextView spiega = new TextView(act);
        spiega.setText("Oggetti posati e contabilizzati senza rapportino. Togli quelli non ancora montati: gli altri finiscono"
                + " nel rapportino di oggi, e si segnano posati quando lo salvi.");
        spiega.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        lista.addView(spiega);

        Map<String, List<CheckBox>> perLocale = new LinkedHashMap<>();
        Map<CheckBox, Oggetto> caselle = new LinkedHashMap<>();
        String localeCorrente = null;
        for (Oggetto x : oggetti) {
            String loc = x.locale.isEmpty() ? "Senza locale" : x.locale;
            if (!loc.equals(localeCorrente)) {
                localeCorrente = loc;
                CheckBox h = new CheckBox(act);
                h.setText(loc);
                h.setChecked(true);
                h.setTypeface(Typeface.DEFAULT_BOLD);
                h.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = (int) (10 * d);
                lista.addView(h, lp);
                List<CheckBox> gruppo = new ArrayList<>();
                perLocale.put(loc, gruppo);
                h.setOnCheckedChangeListener((b, si) -> {
                    for (CheckBox c : gruppo) c.setChecked(si);
                });
            }
            CheckBox c = new CheckBox(act);
            c.setChecked(true);
            c.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            c.setText(x.nome + (x.idListino != 0
                    ? "  —  " + (x.articolo.equals(x.nome) ? "" : x.articolo + " ") + "× " + formatta(x.quantita)
                    : "  —  senza articolo: posato senza riga"));
            c.setPadding((int) (8 * d), 0, 0, 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = (int) (24 * d);
            lista.addView(c, lp);
            perLocale.get(loc).add(c);
            caselle.put(c, x);
        }
        ScrollView sv = new ScrollView(act);
        sv.addView(lista);
        new com.ncfsistemi.econ.dialogo.EconDialogo.Builder(act)
                .setTitle("Rapportino dal disegno (" + oggetti.size() + ")")
                .setView(sv)
                .setPositiveButton("Prepara il rapportino", (dlg, w) -> {
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<CheckBox, Oggetto> e : caselle.entrySet()) {
                        if (!e.getKey().isChecked()) continue;
                        if (sb.length() > 0) sb.append(',');
                        sb.append(e.getValue().chiave());
                    }
                    if (sb.length() == 0) {
                        Toast.makeText(act, "Nessun oggetto scelto", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent i = new Intent(act, RapportinoDettaglioModActivity.class);
                    i.putExtra(EXTRA_CANTIERE, idCantiere);
                    i.putExtra(EXTRA_OGGETTI, sb.toString());
                    act.startActivity(i);
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    // ------------------------------------------------------------------ righe del rapportino

    /** Righe preparate per il rapportino nuovo: righe di materiale e oggetti senza articolo. */
    public static final class Preparato {
        public final List<ContentValues> righe = new ArrayList<>();
        public final List<Oggetto> senzaArticolo = new ArrayList<>();
        /** Ordine comune a tutti gli oggetti scelti che hanno un ordine (0 se nessuno o piu' d'uno). */
        public int idOrdine;
    }

    /**
     * Righe di materiale per gli oggetti scelti (ancora senza rapportino): una per articolo con la quantita' sommata, la
     * chiave OGGETTI_RIGA con gli oggetti coperti, i mesi dell'assistenza a tempo dell'articolo (pezzi interi).
     */
    public static Preparato prepara(Context ctx, int idCantiere, String scelti, int idUtenteDitta) {
        Preparato p = new Preparato();
        Set<String> chiavi = new LinkedHashSet<>();
        for (String k : scelti.split(",")) if (!k.trim().isEmpty()) chiavi.add(k.trim());
        Map<Integer, List<Oggetto>> perArticolo = new LinkedHashMap<>();
        for (Oggetto x : raccogli(ctx, idCantiere)) {
            if (!chiavi.contains(x.chiave())) continue;
            if (x.idListino == 0) {
                p.senzaArticolo.add(x);
            } else {
                List<Oggetto> g = perArticolo.get(x.idListino);
                if (g == null) perArticolo.put(x.idListino, g = new ArrayList<>());
                g.add(x);
            }
        }
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues tipo = db.getRecord("select " + RapportiniDettaglioTipi.ID + " from " + RapportiniDettaglioTipi.NOME_TABELLA
                    + " where " + RapportiniDettaglioTipi.CATEGORIA + " = '" + RapportiniDettaglioTipi.CATEGORIA_MATERIALE + "' order by "
                    + RapportiniDettaglioTipi.ORDINE + " limit 1");
            int idTipo = tipo != null ? RigheRapportino.intero(tipo, RapportiniDettaglioTipi.ID) : 0;
            int pezzo = idUnita(db, UnitaMisura.CODICE_PEZZO);
            for (Map.Entry<Integer, List<Oggetto>> e : perArticolo.entrySet()) {
                double q = 0;
                StringBuilder oggetti = new StringBuilder();
                List<String> nomi = new ArrayList<>();
                String um = "";
                for (Oggetto x : e.getValue()) {
                    q += x.quantita;
                    if (oggetti.length() > 0) oggetti.append(';');
                    oggetti.append(x.tipo).append(':').append(x.id).append(':').append(x.quantita);
                    if (!nomi.contains(x.nome)) nomi.add(x.nome);
                    if (um.isEmpty()) um = x.um;
                }
                ContentValues r = new ContentValues();
                r.put(RapportiniDettaglio.ID_TIPO, idTipo);
                r.put(RapportiniDettaglio.ID_UTENTE_MANODOPERA, idUtenteDitta);
                r.put(RapportiniDettaglio.QUANTITA, q);
                int idUm = um.isEmpty() ? 0 : idUnita(db, um);
                r.put(RapportiniDettaglio.ID_UNITA_MISURA, idUm != 0 ? idUm : pezzo);
                r.put(RapportiniDettaglio.ID_LISTINO, e.getKey());
                int mesi = AssistenzeLocali.mesiArticolo(ctx, db, e.getKey());
                r.put(RapportiniDettaglio.MESI_ASSISTENZA, mesi > 0 && q == Math.floor(q) ? mesi : 0);
                r.put(RapportiniDettaglio.ID_PREVENTIVO_DETTAGLIO, 0);
                String note = "Dal disegno: " + android.text.TextUtils.join(", ", nomi);
                r.put(RapportiniDettaglio.NOTE, note.length() > 250 ? note.substring(0, 247) + "…" : note);
                r.put(OGGETTI_RIGA, oggetti.toString());
                RigheRapportino.completa(db, r);
                p.righe.add(r);
            }
            p.idOrdine = ordineComune(db, chiavi);
        } finally {
            db.close();
        }
        return p;
    }

    private static int idUnita(DbInterno db, String codice) {
        ContentValues u = db.getRecord("select " + UnitaMisura.ID + " from " + UnitaMisura.NOME_TABELLA + " where upper("
                + UnitaMisura.UNITA_MISURA + ") = upper('" + codice.replace("'", "''") + "') limit 1");
        return u != null ? RigheRapportino.intero(u, UnitaMisura.ID) : 0;
    }

    private static int ordineComune(DbInterno db, Set<String> chiavi) {
        Set<Integer> ordini = new LinkedHashSet<>();
        for (String k : chiavi) {
            String[] p = k.split(":");
            for (Object o : db.eseguiSelect("select " + CollegamentiDisegno.ID_DOCUMENTO + " from " + CollegamentiDisegno.NOME_TABELLA
                    + " where " + CollegamentiDisegno.OGGETTO_TIPO + " = '" + p[0] + "' and " + CollegamentiDisegno.ID_OGGETTO + " = " + p[1]
                    + " and " + CollegamentiDisegno.DOCUMENTO_TIPO + " = '" + CollegamentiDisegno.ORDINE + "'", null)) {
                ordini.add(RigheRapportino.intero((ContentValues) o, CollegamentiDisegno.ID_DOCUMENTO));
            }
        }
        return ordini.size() == 1 ? ordini.iterator().next() : 0;
    }

    // ------------------------------------------------------------------ salvataggio

    /**
     * Dopo il salvataggio di una riga: collega al rapportino gli oggetti che la riga copre ancora (in ordine, finche' la
     * quantita' della riga basta: se e' stata abbassata, i restanti aspettano il prossimo rapportino).
     */
    public static void collegaRiga(DbInterno db, int idCantiere, int idRapportino, int idRiga, ContentValues riga) {
        String oggetti = riga.getAsString(OGGETTI_RIGA);
        if (oggetti == null || oggetti.isEmpty()) return;
        double resta = RigheRapportino.decimale(riga, RapportiniDettaglio.QUANTITA);
        for (String o : oggetti.split(";")) {
            String[] p = o.split(":");
            if (p.length < 3) continue;
            double q = Double.parseDouble(p[2]);
            if (resta + 1e-9 < q) break;
            resta -= q;
            int id = Integer.parseInt(p[1]);
            if (CollegamentiDisegno.haRapportino(db, p[0], id)) continue;
            CollegamentiDisegno.inserisci(db, idCantiere, p[0], id, CollegamentiDisegno.RAPPORTINO, idRapportino, idRiga,
                    CollegamentiDisegno.RUOLO_POSATO, q);
            segnaPosato(db, p[0], id);
        }
    }

    /** Oggetti senza articolo: posati con il rapportino, senza riga. */
    public static void collegaSenzaRiga(DbInterno db, int idCantiere, int idRapportino, List<Oggetto> oggetti) {
        for (Oggetto x : oggetti) {
            if (CollegamentiDisegno.haRapportino(db, x.tipo, x.id)) continue;
            CollegamentiDisegno.inserisci(db, idCantiere, x.tipo, x.id, CollegamentiDisegno.RAPPORTINO, idRapportino, 0,
                    CollegamentiDisegno.RUOLO_POSATO, x.quantita);
            segnaPosato(db, x.tipo, x.id);
        }
    }

    /** Stato visto subito sul tablet (il server lo ricalcola e lo rimanda); non segna la riga da inviare. */
    private static void segnaPosato(DbInterno db, String tipo, int id) {
        String tabella = CollegamentiDisegno.tabellaOggetto(tipo);
        if (tabella == null) return;
        db.getWritableDatabase().execSQL("update " + tabella + " set stato = '" + CollegamentiDisegno.STATO_POSATO + "' where "
                + CollegamentiDisegno.pkOggetto(tipo) + " = " + id);
    }

    private static String formatta(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v).replace('.', ',');
    }
}
