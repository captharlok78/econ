package com.ncfsistemi.econ.stampa;

import android.app.Activity;
import android.content.ContentValues;
import android.util.Base64;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.ElementiCantiereMateriali;
import com.ncfsistemi.econ.db.table.ModelliStampa;
import com.ncfsistemi.econ.export.ConformitaXLS;
import com.ncfsistemi.econ.utils.RigheRapportino;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Dichiarazione di conformità stampabile dal tablet (STUDIO_DESIGN_GRAFICA.md §12), anche offline: stesse variabili e stesso
 * modello del server (App\Stampa\StampaConformita, documento "conformita"). Un blocco A4 per locale con elementi
 * (titolo "area - locale", pagina N di M, piantina, elementi, componenti, materiale, collegamenti) e il riepilogo degli
 * articoli di ogni area. L'HTML va alla stampa di Android: stampante o "Salva come PDF".
 */
public final class StampaConformita {

    private StampaConformita() {
    }

    /** Da chiamare sul thread della UI (le piantine si disegnano con la PiantinaView). */
    public static void stampa(Activity activity, int idCantiere) {
        DbInterno db = new DbInterno(activity);
        List<ContentValues> modelli = StampaRapportino.modelli(db, ModelliStampa.DOC_CONFORMITA);
        db.close();
        if (modelli.isEmpty()) {
            Toast.makeText(activity, "Nessun modello di stampa della conformità: sincronizza il tablet.", Toast.LENGTH_LONG).show();
            return;
        }
        if (modelli.size() == 1) {
            stampa(activity, idCantiere, modelli.get(0));
            return;
        }
        String[] nomi = new String[modelli.size()];
        for (int i = 0; i < nomi.length; i++) {
            nomi[i] = RigheRapportino.testo(modelli.get(i), ModelliStampa.NOME) + (i == 0 ? " (proposto)" : "");
        }
        new MaterialAlertDialogBuilder(activity).setTitle("Modello di stampa")
                .setItems(nomi, (d, i) -> stampa(activity, idCantiere, modelli.get(i)))
                .setNegativeButton("Annulla", null).show();
    }

    private static void stampa(Activity activity, int idCantiere, ContentValues modello) {
        String html;
        try {
            Map<Integer, byte[]> piantine = ConformitaXLS.piantine(activity, idCantiere);
            DbInterno db = new DbInterno(activity);
            Map<String, Object> contesto;
            try {
                contesto = contesto(activity, db, idCantiere, piantine);
            } finally {
                db.close();
            }
            html = MiniMustache.render(RigheRapportino.testo(modello, ModelliStampa.CONTENUTO), contesto);
        } catch (Exception e) {
            Toast.makeText(activity, "Stampa non riuscita: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        StampaRapportino.stampaHtml(activity, "Conformità cantiere " + idCantiere, html);
    }

    /** Variabili del modello, come il server: ditta, cliente, cantiere, totali, aree › locali › elementi, articoli, stampa. */
    static Map<String, Object> contesto(Activity ctx, DbInterno db, int idCantiere, Map<Integer, byte[]> piantine) throws Exception {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("ditta", StampaRapportino.ditta(ctx));
        ContentValues ca = db.getRecord("select c.*, a.ragione_sociale, a.indirizzo as cl_indirizzo, a.cap as cl_cap, a.citta as cl_citta,"
                + " a.provincia as cl_provincia from cantieri c left join anagrafica a on a.id_anagrafica=c.id_anagrafica where c.id_cantiere=" + idCantiere);
        Map<String, Object> cant = new LinkedHashMap<>();
        cant.put("nome", ca == null ? "" : s(ca, "nome"));
        cant.put("indirizzo", ca == null ? "" : StampaRapportino.indirizzo(s(ca, "indirizzo"), s(ca, "civico"), s(ca, "cap"), s(ca, "citta"), s(ca, "provincia")));
        c.put("cantiere", cant);
        if (ca != null && !s(ca, "ragione_sociale").isEmpty()) {
            Map<String, Object> cl = new LinkedHashMap<>();
            cl.put("ragione_sociale", s(ca, "ragione_sociale"));
            cl.put("indirizzo", StampaRapportino.indirizzo(s(ca, "cl_indirizzo"), "", s(ca, "cl_cap"), s(ca, "cl_citta"), s(ca, "cl_provincia")));
            c.put("cliente", cl);
        }

        // locali con elementi in ordine di unità, livello dell'area, area e locale: raggruppati per area
        LinkedHashMap<Integer, String[]> locali = ConformitaXLS.locali(ctx, idCantiere);
        LinkedHashMap<String, List<Map.Entry<Integer, String[]>>> perArea = new LinkedHashMap<>();
        for (Map.Entry<Integer, String[]> l : locali.entrySet()) {
            String chiave = l.getValue()[4];
            List<Map.Entry<Integer, String[]>> lista = perArea.get(chiave);
            if (lista == null) perArea.put(chiave, lista = new ArrayList<>());
            lista.add(l);
        }
        int pagine = locali.size(), pagina = 0;
        int[] tot = new int[2]; // componenti, materiali
        int nElementi = 0;
        List<Object> aree = new ArrayList<>();
        for (List<Map.Entry<Integer, String[]>> lista : perArea.values()) {
            String[] p0 = lista.get(0).getValue();
            int livello = intero(p0[3]);
            String livelloNome = maiuscola(ConformitaXLS.nomeLivello(livello));
            TreeMap<String, Map<String, Object>> articoli = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            List<Object> loc = new ArrayList<>();
            for (Map.Entry<Integer, String[]> l : lista) {
                String[] p = l.getValue();
                List<ContentValues> elementi = ConformitaXLS.elementi(db, l.getKey());
                nElementi += elementi.size();
                List<Object> righe = new ArrayList<>();
                for (ContentValues e : elementi) righe.add(elemento(db, e, articoli, tot));
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("primo", pagina == 0);
                m.put("pagina", ++pagina);
                m.put("pagine", pagine);
                m.put("area", p[1]);
                m.put("unita", p[0]);
                m.put("livello_nome", livelloNome);
                m.put("nome", p[2]);
                m.put("titolo", p[1].trim().isEmpty() ? p[2] : p[1].trim() + " - " + p[2]);
                m.put("note", p.length > 5 ? p[5] : "");
                byte[] png = piantine.get(l.getKey());
                m.put("piantina", png == null ? "" : "data:image/png;base64," + Base64.encodeToString(png, Base64.NO_WRAP));
                m.put("n_elementi", elementi.size());
                m.put("elementi", righe);
                loc.add(m);
            }
            List<Object> art = new ArrayList<>();
            for (Map<String, Object> a : articoli.values()) {
                Map<String, Object> r = new LinkedHashMap<>(a);
                r.put("quantita", StampaRapportino.numero((Double) a.get("q")));
                art.add(r);
            }
            Map<String, Object> area = new LinkedHashMap<>();
            area.put("nome", p0[1]);
            area.put("unita", p0[0]);
            area.put("livello", livello);
            area.put("livello_nome", livelloNome);
            area.put("locali", loc);
            area.put("articoli", art);
            area.put("ha_articoli", !art.isEmpty());
            aree.add(area);
        }
        Map<String, Object> totali = new LinkedHashMap<>();
        totali.put("aree", perArea.size());
        totali.put("locali", pagine);
        totali.put("elementi", nElementi);
        totali.put("componenti", tot[0]);
        totali.put("materiali", tot[1]);
        c.put("totali", totali);
        c.put("aree", aree);
        c.put("vuoto", aree.isEmpty());
        c.put("stampa", StampaRapportino.infoStampa(ctx));
        return c;
    }

    private static Map<String, Object> elemento(DbInterno db, ContentValues e, TreeMap<String, Map<String, Object>> articoli, int[] tot) {
        int id = i(e, "id_elemento_cant");
        String codice = s(e, "codice_elemento");
        somma(articoli, "Elemento", codice, s(e, "nome_elemento_cant"), 1, "");
        List<String> note = new ArrayList<>();
        Double h = e.getAsDouble("altezza_da_terra");
        if (h != null && h > 0) note.add("h " + Math.round(h) + " cm");
        if (!s(e, "nome_linea").isEmpty()) note.add("Linea " + s(e, "nome_linea"));
        if (!s(e, "nome_placca").isEmpty()) note.add("Placca " + s(e, "nome_placca"));
        if (i(e, "prev_numero") > 0) note.add(("O".equals(s(e, "prev_tipo")) ? "Ordine " : "Prev. ") + i(e, "prev_numero") + "/" + i(e, "prev_anno"));
        if (!s(e, "nota").isEmpty()) note.add(s(e, "nota"));

        List<ContentValues> componenti = ConformitaXLS.componenti(db, id);
        Set<Integer> idComp = new HashSet<>();
        for (ContentValues cc : componenti) idComp.add(i(cc, "id_componente_cant"));
        Map<Integer, List<String>> diComp = new HashMap<>();
        List<Object> suElemento = new ArrayList<>();
        for (ContentValues k : ConformitaXLS.collegamenti(db, id)) {
            int mio = i(k, "id_elemento_cant1") == id ? i(k, "id_componente_cant1") : i(k, "id_componente_cant2");
            if (mio != 0 && idComp.contains(mio)) {
                List<String> l = diComp.get(mio);
                if (l == null) diComp.put(mio, l = new ArrayList<>());
                l.add(testoCollegamento(k, id));
            } else {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("testo", testoCollegamento(k, id));
                suElemento.add(m);
            }
        }
        List<Object> comp = new ArrayList<>();
        for (ContentValues cc : componenti) {
            int idc = i(cc, "id_componente_cant");
            String pos = s(cc, "moduli_occupati_cant").trim();
            List<String> kk = diComp.get(idc);
            comp.add(riga(pos.isEmpty() ? "" : "Pos. " + pos, s(cc, "nome_componente_cant"), s(cc, "codice"), s(cc, "nota"),
                    kk == null ? "" : android.text.TextUtils.join(" · ", kk), false));
            somma(articoli, "Componente", s(cc, "codice"), s(cc, "nome_componente_cant"), 1, "");
            tot[0]++;
            for (ContentValues sc : ConformitaXLS.composti(db, idc)) {
                comp.add(riga("", s(sc, "nome_componente_cant"), s(sc, "codice"), "", "", true));
                somma(articoli, "Componente", s(sc, "codice"), s(sc, "nome_componente_cant"), 1, "");
                tot[0]++;
            }
        }
        List<Object> mat = new ArrayList<>();
        for (ContentValues m : ElementiCantiereMateriali.diElemento(db, id)) {
            double q = m.getAsDouble("quantita") == null ? 0 : m.getAsDouble("quantita");
            String descr = s(m, "descrizione").isEmpty() ? s(m, "codice_articolo") : s(m, "descrizione");
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("codice", s(m, "codice_articolo"));
            r.put("descrizione", descr);
            r.put("quantita", StampaRapportino.numero(q));
            r.put("um", s(m, "um"));
            r.put("nota", s(m, "nota"));
            mat.add(r);
            somma(articoli, "Materiale", s(m, "codice_articolo"), descr, q, s(m, "um"));
            tot[1]++;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("numero", i(e, "numero_identificativo"));
        out.put("nome", s(e, "nome_elemento_cant"));
        out.put("codice", codice);
        out.put("note", android.text.TextUtils.join(" · ", note));
        out.put("componenti", comp);
        out.put("materiali", mat);
        out.put("collegamenti", suElemento);
        out.put("ha_dettaglio", !comp.isEmpty() || !mat.isEmpty() || !suElemento.isEmpty());
        return out;
    }

    private static Map<String, Object> riga(String pos, String nome, String codice, String nota, String collegamenti, boolean sotto) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pos", pos);
        m.put("nome", nome);
        m.put("codice", codice);
        m.put("nota", nota);
        m.put("collegamenti", collegamenti);
        m.put("sotto", sotto);
        return m;
    }

    /** "→ 12 - Presa (componente) · Cucina · Cavo: FG16 · 5 m", come il server. */
    private static String testoCollegamento(ContentValues k, int id) {
        Object[] c = ConformitaXLS.collegamento(k, id);
        double metri = (Double) c[4];
        return "→ " + c[0] + (((String) c[1]).isEmpty() ? "" : " (" + c[1] + ")") + " · " + c[2]
                + (((String) c[3]).isEmpty() ? "" : " · " + c[3]) + (metri > 0 ? " · " + StampaRapportino.numero(metri) + " m" : "");
    }

    private static void somma(TreeMap<String, Map<String, Object>> articoli, String tipo, String codice, String descrizione, double q, String um) {
        String chiave = tipo + "|" + (codice.isEmpty() ? descrizione.toLowerCase(Locale.ITALY) : codice) + "|" + um;
        Map<String, Object> a = articoli.get(chiave);
        if (a == null) {
            a = new LinkedHashMap<>();
            a.put("tipo", tipo);
            a.put("codice", codice);
            a.put("descrizione", descrizione);
            a.put("um", um);
            a.put("q", 0d);
            articoli.put(chiave, a);
        }
        a.put("q", (Double) a.get("q") + q);
    }

    private static String maiuscola(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ITALY) + s.substring(1);
    }

    private static int intero(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String s(ContentValues v, String k) {
        String x = v.getAsString(k);
        return x == null ? "" : x;
    }

    private static int i(ContentValues v, String k) {
        Integer x = null;
        try {
            x = v.getAsInteger(k);
        } catch (Exception ignored) {
        }
        return x == null ? 0 : x;
    }
}
