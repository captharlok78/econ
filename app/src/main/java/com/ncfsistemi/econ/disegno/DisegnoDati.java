package com.ncfsistemi.econ.disegno;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.ncfsistemi.econ.ComposizioneActivity;
import com.ncfsistemi.econ.ComposizioneLiberaActivity;
import com.ncfsistemi.econ.ComposizioneQuadroActivity;
import com.ncfsistemi.econ.ElementoDettaglioActivity;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.CategorieGenerali;
import com.ncfsistemi.econ.db.table.ComponentiCantiere;
import com.ncfsistemi.econ.db.table.ComposizioniCantiere;
import com.ncfsistemi.econ.db.table.Elementi;
import com.ncfsistemi.econ.db.table.ElementiCantiere;
import com.ncfsistemi.econ.db.table.ElementiDisegno;
import com.ncfsistemi.econ.db.table.Locali;
import com.ncfsistemi.econ.db.table.LocaliPorteFinestre;
import com.ncfsistemi.econ.db.table.Preventivi;
import com.ncfsistemi.econ.utils.CategorieDisegno;
import com.ncfsistemi.econ.views.EconElementoElettrico;

import java.util.ArrayList;
import java.util.List;

/**
 * Dati della schermata Disegno a tre pannelli (STUDIO_DESIGN_GRAFICA.md §8): struttura del cantiere, piantina del
 * locale, elementi posati (stessa scelta per documento della piantina classica), tavolozza e dettaglio dell'elemento.
 * Le scritture passano dagli stessi metodi della piantina classica (EconElementoElettrico / LocaliPorteFinestre), così
 * restano uguali le righe dei preventivi e la sincronizzazione.
 */
public final class DisegnoDati {

    private DisegnoDati() {
    }

    // ------------------------------------------------------------------ struttura: vedi StrutturaCantiere (componente unico)

    /** "Cantiere › Unità › Area" del locale, per il sottotitolo. */
    public static String percorso(Context ctx, int idLocale) {
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues r = db.getRecord("select c.nome as c, u.nome as u, a.nome as a from locali l"
                    + " left join aree a on a.id_area=l.id_area left join unita u on u.id_unita=a.id_unita"
                    + " left join cantieri c on c.id_cantiere=u.id_cantiere where l.id_locale=" + idLocale);
            if (r == null) return "";
            StringBuilder sb = new StringBuilder();
            for (String k : new String[]{"c", "u", "a"}) {
                String s = r.getAsString(k);
                if (s == null || s.trim().isEmpty()) continue;
                if (sb.length() > 0) sb.append(" › ");
                sb.append(s.trim());
            }
            return sb.toString();
        } finally {
            db.close();
        }
    }

    // ------------------------------------------------------------------ locale

    /**
     * Elemento di disegno del catalogo del server (schema 49, STUDIO_DESIGN_GRAFICA.md §18): sul muro (PARETE) o dentro la
     * stanza (PAVIMENTO), misure in centesimi del lato della piantina, forma = come si disegna (PiantinaView).
     */
    public static final class ElementoDisegno {
        public int id;
        public String codice = "";
        public String nome = "";
        public String gruppo = "";
        public String posizione = ElementiDisegno.PAVIMENTO;
        public String forma = "RETTANGOLO";
        public int larghezza = 12;
        public int profondita = 8;
        public String sigla = "";
        public String colore = "";
        public boolean attivo = true;

        public boolean parete() {
            return ElementiDisegno.PARETE.equalsIgnoreCase(posizione);
        }

        /** Tipo della riga in locali_porte_finestre: P/F per le app vecchie, E a pavimento. */
        String tipo() {
            if (!parete()) return "E";
            return "FINESTRA".equalsIgnoreCase(forma) || "PORTAFINESTRA".equalsIgnoreCase(forma) ? "F" : "P";
        }

        static ElementoDisegno predefinito(String tipo) {
            ElementoDisegno e = new ElementoDisegno();
            boolean finestra = "F".equalsIgnoreCase(tipo);
            e.codice = finestra ? "FINESTRA" : "PORTA";
            e.nome = finestra ? "Finestra" : "Porta";
            e.gruppo = "Aperture";
            e.posizione = ElementiDisegno.PARETE;
            e.forma = e.codice;
            e.larghezza = finestra ? 17 : 14;
            e.profondita = finestra ? 3 : 14;
            e.colore = finestra ? "#4aa3df" : "#8a6d3b";
            return e;
        }
    }

    /**
     * Catalogo degli elementi di disegno (anche i non attivi, per riconoscere quelli già posati), per ordine. Prima della
     * prima sincronizzazione con lo schema 49 è vuoto: restano porta e finestra.
     */
    public static List<ElementoDisegno> catalogoDisegno(Context ctx) {
        List<ElementoDisegno> out = new ArrayList<>();
        DbInterno db = new DbInterno(ctx);
        try {
            for (Object o : db.eseguiSelect("select * from " + ElementiDisegno.NOME_TABELLA + " order by "
                    + ElementiDisegno.ORDINE + ", " + ElementiDisegno.NOME, null)) {
                ContentValues r = (ContentValues) o;
                ElementoDisegno e = new ElementoDisegno();
                e.id = intero(r, ElementiDisegno.ID);
                e.codice = testo(r, ElementiDisegno.CODICE);
                e.nome = testo(r, ElementiDisegno.NOME);
                e.gruppo = testo(r, ElementiDisegno.GRUPPO);
                e.posizione = testo(r, ElementiDisegno.POSIZIONE);
                e.forma = testo(r, ElementiDisegno.FORMA).toUpperCase();
                e.larghezza = Math.max(2, intero(r, ElementiDisegno.LARGHEZZA));
                e.profondita = Math.max(1, intero(r, ElementiDisegno.PROFONDITA));
                e.sigla = testo(r, ElementiDisegno.SIGLA);
                e.colore = testo(r, ElementiDisegno.COLORE);
                e.attivo = r.get(ElementiDisegno.ATTIVO) == null || intero(r, ElementiDisegno.ATTIVO) != 0;
                out.add(e);
            }
        } catch (Exception ignorata) {
            // tabella non ancora creata/scaricata: si usano porta e finestra
        } finally {
            db.close();
        }
        if (out.isEmpty()) {
            out.add(ElementoDisegno.predefinito("P"));
            out.add(ElementoDisegno.predefinito("F"));
        }
        return out;
    }

    private static String testo(ContentValues v, String k) {
        String s = v.getAsString(k);
        return s == null ? "" : s.trim();
    }

    /** Porta, finestra o altro elemento di disegno posato nel locale (riga di locali_porte_finestre). */
    public static final class Apertura {
        public int id;
        public String tipo;       // P porta, F finestra (anche per le app vecchie), E a pavimento
        public int cella;
        public String lato;       // T, B, L, R
        public int perc;          // inizio dell'apertura sul lato della cella, in % del lato
        public int posX, posY;    // centro degli elementi a pavimento, 0..200
        public int rotazione;     // 0, 90, 180, 270
        public ElementoDisegno elemento;

        public boolean parete() {
            return elemento == null || elemento.parete();
        }
    }

    public static final class Locale {
        public int id;
        public String nome = "";
        public int n = 8;
        /** true = quadretto della stanza. */
        public boolean[] celle;
        public final List<Apertura> aperture = new ArrayList<>();
    }

    public static Locale locale(Context ctx, int idLocale) {
        Locale l = new Locale();
        l.id = idLocale;
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues where = new ContentValues();
            where.put(Locali.ID_LOCALE, idLocale);
            ContentValues r = db.getRecord(new Locali(), where);
            String pianta = "";
            if (r != null) {
                l.nome = r.getAsString(Locali.NOME) == null ? "" : r.getAsString(Locali.NOME);
                pianta = r.getAsString(Locali.PIANTA_LOGICA);
            }
            if (pianta == null) pianta = "";
            if (pianta.length() == 36) {
                l.n = 6;
            } else {
                l.n = 8;
                StringBuilder sb = new StringBuilder(pianta);
                while (sb.length() < 64) sb.append('1');
                pianta = sb.substring(0, 64);
            }
            l.celle = new boolean[l.n * l.n];
            for (int i = 0; i < l.celle.length; i++) l.celle[i] = pianta.charAt(i) == '1';

        } finally {
            db.close();
        }
        // elementi di disegno: porte e finestre senza elemento (app vecchie) prendono quelli del catalogo con quel codice
        java.util.Map<Integer, ElementoDisegno> perId = new java.util.HashMap<>();
        java.util.Map<String, ElementoDisegno> perCodice = new java.util.HashMap<>();
        for (ElementoDisegno e : catalogoDisegno(ctx)) {
            if (e.id > 0) perId.put(e.id, e);
            perCodice.put(e.codice, e);
        }
        db = new DbInterno(ctx);
        try {
            ContentValues wp = new ContentValues();
            wp.put(LocaliPorteFinestre.ID_LOCALE, idLocale);
            for (Object o : db.eseguiSelect(new LocaliPorteFinestre(), wp, null)) {
                ContentValues p = (ContentValues) o;
                Apertura a = new Apertura();
                a.id = intero(p, LocaliPorteFinestre.ID_PORTA_FINESTRA);
                a.tipo = p.getAsString(LocaliPorteFinestre.TIPO);
                a.cella = intero(p, LocaliPorteFinestre.CELLA);
                a.lato = p.getAsString(LocaliPorteFinestre.POS_IN_CELLA);
                a.perc = intero(p, LocaliPorteFinestre.POS_INIZIALE_PERC);
                a.posX = intero(p, LocaliPorteFinestre.POS_X);
                a.posY = intero(p, LocaliPorteFinestre.POS_Y);
                a.rotazione = ((intero(p, LocaliPorteFinestre.ROTAZIONE) % 360) + 360) % 360;
                a.elemento = perId.get(intero(p, LocaliPorteFinestre.ID_ELEMENTO_DISEGNO));
                boolean pavimento = "E".equalsIgnoreCase(a.tipo);
                if (a.elemento == null && !pavimento) {
                    a.elemento = perCodice.get("F".equalsIgnoreCase(a.tipo) ? "FINESTRA" : "PORTA");
                    if (a.elemento == null) a.elemento = ElementoDisegno.predefinito(a.tipo);
                }
                if (a.elemento == null || a.elemento.parete() == pavimento) continue; // catalogo cambiato: non si sa disegnare
                if (pavimento || (a.lato != null && a.cella >= 0 && a.cella < l.celle.length)) l.aperture.add(a);
            }
        } finally {
            db.close();
        }
        return l;
    }

    /** Posa un elemento di disegno: sul muro (cella, lato, perc) o dentro la stanza (posX, posY 0..200). */
    public static void aggiungiApertura(Context ctx, int idLocale, ElementoDisegno e, int cella, String lato, int perc, int posX, int posY) {
        DbInterno db = new DbInterno(ctx);
        try {
            LocaliPorteFinestre tab = new LocaliPorteFinestre();
            ContentValues val = tab.getValoriLogInserimento(db);
            val.put(LocaliPorteFinestre.ID_LOCALE, idLocale);
            val.put(LocaliPorteFinestre.TIPO, e.tipo());
            val.put(LocaliPorteFinestre.ID_ELEMENTO_DISEGNO, e.id);
            val.put(LocaliPorteFinestre.CELLA, e.parete() ? cella : 0);
            val.put(LocaliPorteFinestre.POS_IN_CELLA, e.parete() ? lato : "T");
            val.put(LocaliPorteFinestre.POS_INIZIALE_PERC, e.parete() ? perc : 0);
            val.put(LocaliPorteFinestre.POS_X, e.parete() ? 0 : posX);
            val.put(LocaliPorteFinestre.POS_Y, e.parete() ? 0 : posY);
            val.put(LocaliPorteFinestre.ROTAZIONE, 0);
            tab.inserisciRecord(db, val);
        } finally {
            db.close();
        }
    }

    /** Salva posizione, rotazione ed elemento di una riga già posata. */
    public static void spostaApertura(Context ctx, Apertura a) {
        DbInterno db = new DbInterno(ctx);
        try {
            LocaliPorteFinestre tab = new LocaliPorteFinestre();
            ContentValues val = tab.getValoriLogModifica(db);
            if (a.elemento != null) {
                val.put(LocaliPorteFinestre.TIPO, a.elemento.tipo());
                val.put(LocaliPorteFinestre.ID_ELEMENTO_DISEGNO, a.elemento.id);
            }
            val.put(LocaliPorteFinestre.CELLA, a.cella);
            val.put(LocaliPorteFinestre.POS_IN_CELLA, a.lato == null ? "T" : a.lato);
            val.put(LocaliPorteFinestre.POS_INIZIALE_PERC, a.perc);
            val.put(LocaliPorteFinestre.POS_X, a.posX);
            val.put(LocaliPorteFinestre.POS_Y, a.posY);
            val.put(LocaliPorteFinestre.ROTAZIONE, a.rotazione);
            ContentValues where = new ContentValues();
            where.put(LocaliPorteFinestre.ID_PORTA_FINESTRA, a.id);
            tab.aggiornaRecord(db, val, where);
        } finally {
            db.close();
        }
    }

    public static void eliminaApertura(Context ctx, int idApertura) {
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues where = new ContentValues();
            where.put(LocaliPorteFinestre.ID_PORTA_FINESTRA, idApertura);
            new LocaliPorteFinestre().cancellaRecord(db, where);
        } finally {
            db.close();
        }
    }

    // ------------------------------------------------------------------ elementi posati

    public static final class Posato {
        public int id;                 // id_elemento_cant
        public int idElemento;         // elemento del listino
        public int idCategoria;
        public int idPreventivo;
        public String nome = "";
        public String numero = "";
        public int x, y;               // 0..200, angolo in alto a sinistra
        public double altezza;
        public String icona;
        public int cavi, tubi, relazioni, componentiDocumento;
        /** Articolo della ditta posato direttamente (§3.5 A): id, quantità, unità, settore (simbolo e colore). */
        public int idArticolo, idUnita, idSettore;
        public double quantita;
        public String um = "";
        /** Elemento di un altro documento (stile grigio, come nella piantina classica). */
        public boolean altroOrdine;
    }

    /** Elementi del locale per il documento scelto: stessa query e stesso filtro della piantina classica. */
    public static List<Posato> elementi(Context ctx, int idLocale, int idPreventivo) {
        List<Posato> out = new ArrayList<>();
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues where = new ContentValues();
            where.put("id_preventivo", idPreventivo);
            ContentValues recPREV = db.getRecord(new Preventivi(), where);
            String filtroPrev = "";
            boolean chiuso = false;
            boolean preventivo = recPREV != null && Preventivi.TIPO_PREVENTIVO.equals(recPREV.getAsString("tipo"));
            if (preventivo) {
                filtroPrev = " or elementi_cantiere.id_preventivo=" + idPreventivo;
                String stato = recPREV.getAsString(Preventivi.STATO);
                chiuso = Preventivi.STATO_ACCETTATO.equals(stato) || Preventivi.STATO_RIFIUTATO.equals(stato);
            }
            String campi = "Select elementi_cantiere.*,(select count(*) from componenti_cantiere inner join composizioni_cantiere"
                    + " on componenti_cantiere.id_componente_cant=composizioni_cantiere.id_componente_cant where"
                    + " composizioni_cantiere.id_elemento_cant=elementi_cantiere.id_elemento_cant and componenti_cantiere.id_preventivo="
                    + idPreventivo + ") as num_componenti_prev_selezionato,elementi.icona,elementi.id_categoria_generale,"
                    + "(select count(*) from collegamenti where (id_elemento_cant1=elementi_cantiere.id_elemento_cant or"
                    + " id_elemento_cant2=elementi_cantiere.id_elemento_cant) and id_cavo<>0) as n_cavi,"
                    + "(select count(*) from collegamenti where (id_elemento_cant1=elementi_cantiere.id_elemento_cant or"
                    + " id_elemento_cant2=elementi_cantiere.id_elemento_cant) and id_tubo<>0) as n_tubi,"
                    + "(select count(*) from relazioni where id_elemento_cant1=elementi_cantiere.id_elemento_cant or"
                    + " id_elemento_cant2=elementi_cantiere.id_elemento_cant) as n_relazioni,"
                    + "(select id_settore from listini where listini.id=elementi_cantiere.id_articolo_ditta) as id_settore_art,"
                    + "(select unita_misura from unita_misura where unita_misura.id=elementi_cantiere.id_unita_misura) as um_art"
                    + " from elementi_cantiere left join elementi on elementi_cantiere.id_elemento=elementi.id_elemento"
                    + " left join preventivi on elementi_cantiere.id_preventivo=preventivi.id_preventivo where id_locale = " + idLocale;
            String sql = chiuso ? campi + " and elementi_cantiere.id_preventivo=" + idPreventivo
                    : campi + " and (elementi_cantiere.id_preventivo=0 or preventivi.tipo='O' " + filtroPrev + ")";
            ArrayList<Object> tutti = db.eseguiSelect(sql + " order by elementi_cantiere.numero_identificativo", null);

            // col preventivo scelto si nascondono gli elementi copiati nel preventivo (resta la copia)
            List<Integer> copiati = new ArrayList<>();
            if (preventivo) {
                for (Object o : tutti) {
                    Integer orig = ((ContentValues) o).getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT_ORIGINE);
                    if (orig != null) copiati.add(orig);
                }
            }
            for (Object o : tutti) {
                ContentValues v = (ContentValues) o;
                Posato p = new Posato();
                p.id = intero(v, ElementiCantiere.ID_ELEMENTO_CANT);
                if (copiati.contains(p.id)) continue;
                p.idElemento = intero(v, ElementiCantiere.ID_ELEMENTO);
                p.idCategoria = intero(v, Elementi.ID_CATEGORIA_GENERALE);
                p.idPreventivo = intero(v, ElementiCantiere.ID_PREVENTIVO);
                p.nome = v.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT) == null ? "" : v.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT);
                p.numero = "" + intero(v, ElementiCantiere.NUMERO_IDENTIFICATIVO);
                p.x = Math.max(0, Math.min(200, intero(v, ElementiCantiere.POS_X)));
                p.y = Math.max(0, Math.min(200, intero(v, ElementiCantiere.POS_Y)));
                Double h = v.getAsDouble(ElementiCantiere.ALTEZZA_DA_TERRA);
                p.altezza = h == null ? 0 : h;
                p.icona = v.getAsString(Elementi.ICONA);
                p.cavi = intero(v, "n_cavi");
                p.tubi = intero(v, "n_tubi");
                p.relazioni = intero(v, "n_relazioni");
                p.componentiDocumento = intero(v, "num_componenti_prev_selezionato");
                p.idArticolo = intero(v, ElementiCantiere.ID_ARTICOLO_DITTA);
                if (p.idElemento <= 0 && p.idArticolo != 0) {
                    p.idUnita = intero(v, ElementiCantiere.ID_UNITA_MISURA);
                    p.idSettore = intero(v, "id_settore_art");
                    Double q = v.getAsDouble(ElementiCantiere.QUANTITA);
                    p.quantita = q == null || q <= 0 ? 1 : q;
                    String um = v.getAsString("um_art");
                    if (um == null || um.isEmpty()) um = v.getAsString(ElementiCantiere.UNITA_MISURA);
                    p.um = um == null ? "" : um;
                } else {
                    p.idArticolo = 0;
                }
                p.altroOrdine = p.idPreventivo != idPreventivo && p.componentiDocumento == 0;
                // dismesso (sostituito o tolto con un rapportino): resta sulla piantina, in grigio (STUDIO_STORICO_DISEGNO.md S6)
                if ("DISMESSO".equals(v.getAsString(ElementiCantiere.STATO))) p.altroOrdine = true;
                out.add(p);
            }
        } finally {
            db.close();
        }
        return out;
    }

    /** Coppie di elementi collegati (cavo o tubo) dentro al locale, per le linee del modo Collega. */
    public static List<int[]> linee(Context ctx, int idLocale) {
        List<int[]> out = new ArrayList<>();
        DbInterno db = new DbInterno(ctx);
        try {
            ArrayList<Object> r = db.eseguiSelect("select c.id_elemento_cant1 as a, c.id_elemento_cant2 as b,"
                    + " case when c.id_cavo<>0 then 1 else 0 end as cavo from collegamenti c"
                    + " inner join elementi_cantiere e1 on e1.id_elemento_cant=c.id_elemento_cant1"
                    + " inner join elementi_cantiere e2 on e2.id_elemento_cant=c.id_elemento_cant2"
                    + " where e1.id_locale=" + idLocale + " and e2.id_locale=" + idLocale, null);
            for (Object o : r) {
                ContentValues v = (ContentValues) o;
                out.add(new int[]{intero(v, "a"), intero(v, "b"), intero(v, "cavo")});
            }
        } finally {
            db.close();
        }
        return out;
    }

    // ------------------------------------------------------------------ dettaglio

    public static final class Dettaglio {
        public final List<String> composizione = new ArrayList<>();
        public final List<String> collegamenti = new ArrayList<>();
        public final List<String> relazioni = new ArrayList<>();
        public boolean configurabile;
        public String categoria = "";
    }

    public static Dettaglio dettaglio(Context ctx, Posato p) {
        Dettaglio d = new Dettaglio();
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues cat = db.getRecord("select nome, configurabile_sn from categorie_generali where id_categoria_generale=" + p.idCategoria);
            if (cat != null) {
                d.categoria = cat.getAsString("nome") == null ? "" : cat.getAsString("nome");
                d.configurabile = intero(cat, "configurabile_sn") == 1;
            }
            for (Object o : new ComposizioniCantiere().getComposizioneElemento(db, p.id)) {
                ContentValues v = (ContentValues) o;
                String moduli = v.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
                String nota = v.getAsString(ComponentiCantiere.NOTA);
                StringBuilder sb = new StringBuilder();
                if (moduli != null && !moduli.trim().isEmpty()) sb.append(moduli.trim()).append(" · ");
                sb.append(v.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
                if (nota != null && !nota.trim().isEmpty()) sb.append(" (").append(nota.trim()).append(")");
                d.composizione.add(sb.toString());
            }
            ArrayList<Object> coll = db.eseguiSelect("select c.metri, c.id_cavo, e.numero_identificativo as num,"
                    + " e.nome_elemento_cant as nome from collegamenti c left join elementi_cantiere e on"
                    + " e.id_elemento_cant = case when c.id_elemento_cant1=" + p.id + " then c.id_elemento_cant2 else c.id_elemento_cant1 end"
                    + " where c.id_elemento_cant1=" + p.id + " or c.id_elemento_cant2=" + p.id, null);
            for (Object o : coll) {
                ContentValues v = (ContentValues) o;
                String verso = intero(v, "id_cavo") != 0 ? "Cavo" : "Tubo";
                String metri = v.getAsString("metri");
                String altro = v.getAsString("nome") == null ? "?" : v.getAsInteger("num") + ". " + v.getAsString("nome");
                d.collegamenti.add(verso + " → " + altro + (metri != null && !metri.equals("0") && !metri.equals("0.0") ? " · " + metri + " m" : ""));
            }
            ArrayList<Object> rel = db.eseguiSelect("select e.numero_identificativo as num, e.nome_elemento_cant as nome"
                    + " from relazioni r left join elementi_cantiere e on e.id_elemento_cant = case when r.id_elemento_cant1="
                    + p.id + " then r.id_elemento_cant2 else r.id_elemento_cant1 end where r.id_elemento_cant1=" + p.id
                    + " or r.id_elemento_cant2=" + p.id, null);
            for (Object o : rel) {
                ContentValues v = (ContentValues) o;
                d.relazioni.add(v.getAsString("nome") == null ? "?" : v.getAsInteger("num") + ". " + v.getAsString("nome"));
            }
        } finally {
            db.close();
        }
        return d;
    }

    // ------------------------------------------------------------------ tavolozza

    public static final class Categoria {
        public int id;
        public String nome = "";
    }

    public static final class VoceTavolozza {
        public int id;
        public int idCategoria;
        public String nome = "";
        public String icona;
        public boolean preferito;
        /** Articolo della ditta (id di listini = articoli_ditta sul server), 0 per gli elementi della libreria. */
        public int idArticolo;
        public int idSettore;
        public String codice = "";
        /** Foto dell'articolo in base64 (listini.foto), null se non c'è. */
        public String foto;
    }

    /** Filtro della finestra di scelta per gli articoli della ditta (STUDIO_DESIGN_GRAFICA.md §22). */
    public static final int ARTICOLI = -2;

    /**
     * Elementi e componenti della libreria (categorie, Componi, collegamenti, conformità) solo con il modulo Elettro: con il
     * solo Design sul disegno si posano gli articoli della ditta (STUDIO_DESIGN_GRAFICA.md §22).
     */
    public static boolean conElettro(Context ctx) {
        return com.ncfsistemi.econ.utils.FunzionalitaApp.haModulo(ctx, com.ncfsistemi.econ.utils.FunzionalitaApp.ELETTRICO);
    }

    /** Articoli della ditta proposti per il disegno (quelli del listino della ditta, non quelli nati in una riga). */
    public static List<VoceTavolozza> articoli(Context ctx) {
        List<VoceTavolozza> out = new ArrayList<>();
        DbInterno db = new DbInterno(ctx);
        try {
            for (Object o : db.eseguiSelect("select id, codice_articolo, codice_ditta, descrizione, id_settore, foto from listini"
                    + " where coalesce(id, 0) <> 0 and coalesce(nel_listino, 1) = 1 order by descrizione collate nocase limit 2000", null)) {
                ContentValues v = (ContentValues) o;
                VoceTavolozza t = new VoceTavolozza();
                t.idArticolo = intero(v, "id");
                t.id = -t.idArticolo; // non si confonde con gli elementi della libreria nella stessa griglia
                t.idCategoria = ARTICOLI;
                String cod = v.getAsString("codice_ditta");
                if (cod == null || cod.trim().isEmpty()) cod = v.getAsString("codice_articolo");
                t.codice = cod == null ? "" : cod.trim();
                t.nome = v.getAsString("descrizione") == null ? t.codice : v.getAsString("descrizione").trim();
                t.idSettore = intero(v, "id_settore");
                String foto = v.getAsString("foto");
                t.foto = foto == null || foto.trim().isEmpty() ? null : foto;
                out.add(t);
            }
        } finally {
            db.close();
        }
        return out;
    }

    /** Unità di misura proponibili per un articolo posato: {id, codice, nome}, senza ore e minuti. */
    public static List<String[]> unitaMisura(Context ctx) {
        List<String[]> out = new ArrayList<>();
        DbInterno db = new DbInterno(ctx);
        try {
            for (Object o : db.eseguiSelect("select id, unita_misura, nome from unita_misura where coalesce(minuti_per_unita, 0) = 0"
                    + " order by id", null)) {
                ContentValues v = (ContentValues) o;
                out.add(new String[]{String.valueOf(intero(v, "id")), v.getAsString("unita_misura"), v.getAsString("nome")});
            }
        } finally {
            db.close();
        }
        return out;
    }

    /** Quantità e unità dell'ultima posa di questo articolo sul tablet (per proporle), {quantita, id_unita} o null. */
    public static double[] ultimaPosa(Context ctx, int idArticolo) {
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues r = db.getRecord("select quantita, id_unita_misura from elementi_cantiere where id_articolo_ditta=" + idArticolo
                    + " and coalesce(id_elemento, 0) = 0 order by data_ins desc, id_elemento_cant desc limit 1");
            if (r == null) return null;
            Double q = r.getAsDouble("quantita");
            return new double[]{q == null ? 1 : q, intero(r, "id_unita_misura")};
        } catch (Exception e) {
            return null;
        } finally {
            db.close();
        }
    }

    /**
     * Posa un articolo della ditta nel locale (STUDIO_DESIGN_GRAFICA.md §3.5 A): riga di elementi_cantiere senza elemento
     * della libreria, con articolo, quantità e unità di misura. Torna l'id_elemento_cant.
     */
    public static int posaArticolo(Context ctx, int idCantiere, int idLocale, int idPreventivo, VoceTavolozza a,
                                   double quantita, int idUnita, int x, int y) {
        DbInterno db = new DbInterno(ctx);
        try {
            ElementiCantiere tab = new ElementiCantiere();
            ContentValues val = tab.getValoriLogInserimento(db);
            val.put(ElementiCantiere.ID_LOCALE, idLocale);
            val.put(ElementiCantiere.ID_ELEMENTO, 0);
            val.put(ElementiCantiere.ID_ARTICOLO_DITTA, a.idArticolo);
            val.put(ElementiCantiere.NOME_ELEMENTO_CANT, a.nome);
            val.put(ElementiCantiere.QUANTITA, quantita);
            if (idUnita > 0) {
                val.put(ElementiCantiere.ID_UNITA_MISURA, idUnita);
                ContentValues u = db.getRecord("select unita_misura from unita_misura where id=" + idUnita);
                if (u != null) val.put(ElementiCantiere.UNITA_MISURA, u.getAsString("unita_misura"));
            }
            val.put(ElementiCantiere.ID_PREVENTIVO, idPreventivo);
            if (idPreventivo == 0) val.put(ElementiCantiere.ID_CANTIERE, idCantiere);
            val.put(ElementiCantiere.POS_X, x);
            val.put(ElementiCantiere.POS_Y, y);
            val.put(ElementiCantiere.NUMERO_IDENTIFICATIVO, tab.getProssimoNumero(db, idCantiere));
            tab.inserisciRecord(db, val);
            Integer id = val.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT);
            return id == null ? 0 : id;
        } finally {
            db.close();
        }
    }

    /** Articolo della ditta (listini): codice (aziendale se c'è), descrizione, foto; null se non è sul tablet. */
    public static ContentValues articolo(Context ctx, int idArticolo) {
        DbInterno db = new DbInterno(ctx);
        try {
            return db.getRecord("select coalesce(nullif(codice_ditta, ''), codice_articolo) as codice, descrizione, foto from listini where id=" + idArticolo);
        } catch (Exception e) {
            return null;
        } finally {
            db.close();
        }
    }

    /** Nuova quantità (e unità) di un articolo posato. */
    public static void salvaQuantita(Context ctx, int idElementoCant, double quantita, int idUnita) {
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues c = new ElementiCantiere().getValoriLogModifica(db);
            c.put(ElementiCantiere.QUANTITA, quantita);
            if (idUnita > 0) {
                c.put(ElementiCantiere.ID_UNITA_MISURA, idUnita);
                ContentValues u = db.getRecord("select unita_misura from unita_misura where id=" + idUnita);
                if (u != null) c.put(ElementiCantiere.UNITA_MISURA, u.getAsString("unita_misura"));
            }
            ContentValues where = new ContentValues();
            where.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCant);
            new ElementiCantiere().aggiornaRecord(db, c, where);
        } finally {
            db.close();
        }
    }

    public static List<Categoria> categorie(Context ctx) {
        List<Categoria> out = new ArrayList<>();
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues where = new ContentValues();
            where.put(CategorieGenerali.POSIZIONABILE_SN, 1);
            for (Object o : db.eseguiSelect(new CategorieGenerali(), where, null)) {
                ContentValues v = (ContentValues) o;
                Categoria c = new Categoria();
                c.id = intero(v, CategorieGenerali.ID_CATEGORIA_GENERALE);
                c.nome = v.getAsString(CategorieGenerali.NOME) == null ? "" : v.getAsString(CategorieGenerali.NOME);
                out.add(c);
            }
        } finally {
            db.close();
        }
        return out;
    }

    /** Elementi di una categoria (idCategoria 0 = solo i preferiti di tutte le categorie posizionabili). */
    public static List<VoceTavolozza> tavolozza(Context ctx, int idCategoria) {
        List<VoceTavolozza> out = new ArrayList<>();
        DbInterno db = new DbInterno(ctx);
        try {
            String sql = idCategoria > 0
                    ? "select * from elementi where id_categoria_generale=" + idCategoria + " order by nome_elemento collate nocase"
                    : "select e.* from elementi e inner join categorie_generali c on c.id_categoria_generale=e.id_categoria_generale"
                    + " where c.posizionabile_sn=1 and e.preferito_sn=1 order by e.nome_elemento collate nocase";
            for (Object o : db.eseguiSelect(sql, null)) {
                ContentValues v = (ContentValues) o;
                VoceTavolozza t = new VoceTavolozza();
                t.id = intero(v, Elementi.ID_ELEMENTO);
                t.idCategoria = intero(v, Elementi.ID_CATEGORIA_GENERALE);
                t.nome = v.getAsString(Elementi.NOME_ELEMENTO) == null ? "" : v.getAsString(Elementi.NOME_ELEMENTO);
                t.icona = v.getAsString(Elementi.ICONA);
                t.preferito = intero(v, Elementi.PREFERITO_SN) == 1;
                out.add(t);
            }
        } finally {
            db.close();
        }
        return out;
    }

    // ------------------------------------------------------------------ scritture

    /** Posa un elemento del listino nel locale, alla posizione x,y (0..200). Torna l'id_elemento_cant. */
    public static int posa(Activity ctx, int idCantiere, int idLocale, int idPreventivo, int idElemento, int x, int y) {
        EconElementoElettrico e = new EconElementoElettrico(ctx);
        e.setIdCantiere(idCantiere);
        e.setIdElementoListino(idElemento);
        e.setIdPreventivo(idPreventivo);
        e.aggiungiInDb(ctx, idLocale);
        if (e.getIdElemento() > 0) {
            e.setPositionPercentX(x);
            e.setPositionPercentY(y);
            e.aggiornaDb(ctx);
        }
        return e.getIdElemento();
    }

    public static void sposta(Activity ctx, int idElementoCant, int x, int y) {
        EconElementoElettrico e = new EconElementoElettrico(ctx);
        e.setIdElemento(idElementoCant);
        e.setPositionPercentX(x);
        e.setPositionPercentY(y);
        e.aggiornaDb(ctx);
    }

    public static void elimina(Activity ctx, int idElementoCant) {
        EconElementoElettrico e = new EconElementoElettrico(ctx);
        e.setIdElemento(idElementoCant);
        e.cancellaDaDb(ctx);
    }

    public static void salvaEtichettaAltezza(Context ctx, int idElementoCant, String nome, Double altezza) {
        DbInterno db = new DbInterno(ctx);
        try {
            ElementiCantiere tab = new ElementiCantiere();
            ContentValues val = tab.getValoriLogModifica(db);
            val.put(ElementiCantiere.NOME_ELEMENTO_CANT, nome);
            if (altezza != null) val.put(ElementiCantiere.ALTEZZA_DA_TERRA, altezza);
            ContentValues where = new ContentValues();
            where.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCant);
            tab.aggiornaRecord(db, val, where);
        } finally {
            db.close();
        }
    }

    public static void salvaPianta(Context ctx, int idLocale, boolean[] celle) {
        StringBuilder sb = new StringBuilder();
        for (boolean c : celle) sb.append(c ? '1' : '0');
        DbInterno db = new DbInterno(ctx);
        try {
            Locali tab = new Locali();
            ContentValues val = tab.getValoriLogModifica(db);
            val.put(Locali.PIANTA_LOGICA, sb.toString());
            ContentValues where = new ContentValues();
            where.put(Locali.ID_LOCALE, idLocale);
            tab.aggiornaRecord(db, val, where);
        } finally {
            db.close();
        }
    }

    /**
     * Elemento posato in un ordine/cantiere mentre è scelto un preventivo: come nella piantina classica si può solo
     * copiarlo nel preventivo o aprirlo in sola lettura.
     */
    public static boolean serveCopia(Context ctx, Posato p, int idPreventivoSelezionato) {
        if (idPreventivoSelezionato == 0 || p.idPreventivo == idPreventivoSelezionato) return false;
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues r = db.getRecord("select tipo from preventivi where id_preventivo=" + idPreventivoSelezionato);
            return r != null && Preventivi.TIPO_PREVENTIVO.equals(r.getAsString("tipo"));
        } finally {
            db.close();
        }
    }

    public static void copiaNelPreventivo(Context ctx, Posato p, int idPreventivoSelezionato) {
        DbInterno db = new DbInterno(ctx);
        try {
            new ElementiCantiere().copiaElementiPreventivo(db, p.idPreventivo, idPreventivoSelezionato, p.id, false);
        } finally {
            db.close();
        }
    }

    /** Apre la composizione (categorie configurabili) o il dettaglio dell'elemento, come la piantina classica. */
    public static void apri(Activity ctx, Posato p, int idPreventivoSelezionato, boolean soloVisualizzazione) {
        DbInterno db = new DbInterno(ctx);
        ContentValues rec;
        ContentValues cat;
        try {
            cat = db.getRecord("select configurabile_sn from categorie_generali where id_categoria_generale=" + p.idCategoria);
            rec = db.getRecord("select e.nome_elemento_cant, o.id_preventivo as id_preventivo_origine from elementi_cantiere e"
                    + " left join elementi_cantiere o on o.id_elemento_cant=e.id_elemento_cant_origine where e.id_elemento_cant=" + p.id);
        } finally {
            db.close();
        }
        Intent intent;
        if (cat != null && intero(cat, "configurabile_sn") == 1) {
            Class<?> cls = ComposizioneLiberaActivity.class;
            int classica = CategorieDisegno.classica(p.idCategoria);
            if (classica == CategorieGenerali.QUADRI) cls = ComposizioneQuadroActivity.class;
            if (classica == CategorieGenerali.SCATOLE_PRESE || classica == CategorieGenerali.SCATOLE_INTERRUTTORI) {
                cls = ComposizioneActivity.class;
            }
            intent = new Intent(ctx, cls);
            intent.putExtra(ElementiCantiere.ID_ELEMENTO_CANT, p.id);
            intent.putExtra(Elementi.NOME_ELEMENTO, rec != null ? rec.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT) : p.nome);
            intent.putExtra("ID_PREVENTIVO_SELEZIONATO", idPreventivoSelezionato);
            intent.putExtra("ELEMENTO_ALTRO_ORDINE", soloVisualizzazione);
            if (rec != null && rec.get("id_preventivo_origine") != null) {
                intent.putExtra("id_preventivo_origine", rec.getAsInteger("id_preventivo_origine"));
            }
        } else {
            intent = new Intent(ctx, ElementoDettaglioActivity.class);
            intent.putExtra(ElementiCantiere.ID_ELEMENTO_CANT, p.id);
            intent.putExtra("ELEMENTO_ALTRO_ORDINE", soloVisualizzazione);
            intent.putExtra("ID_PREVENTIVO_SELEZIONATO", idPreventivoSelezionato);
        }
        ctx.startActivity(intent);
    }

    /** Documento scelto aperto? Se no la piantina è in sola lettura (come CantiereSplitActivity). */
    public static boolean documentoBloccato(Context ctx, int idPreventivo) {
        if (idPreventivo == 0) return false;
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues r = db.getRecord("select stato from preventivi where id_preventivo=" + idPreventivo);
            return r != null && !Preventivi.STATO_APERTO.equals(r.getAsString("stato"));
        } finally {
            db.close();
        }
    }

    public static String descrizioneDocumento(Context ctx, int idPreventivo) {
        if (idPreventivo == 0) return "Cantiere";
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues r = db.getRecord("select tipo, numero from preventivi where id_preventivo=" + idPreventivo);
            if (r == null) return "Cantiere";
            return (Preventivi.TIPO_ORDINE.equals(r.getAsString("tipo")) ? "Ordine " : "Preventivo ") + r.getAsString("numero");
        } finally {
            db.close();
        }
    }

    /**
     * Stato di posa dell'elemento (STUDIO_STORICO_DISEGNO.md): "Posato il 15/11/2026", "Ordinato", "Dismesso", oppure ""
     * se e' solo previsto. Stato e date li calcola il server e arrivano con la sincronizzazione.
     */
    public static String statoPosa(Context ctx, int idElementoCant) {
        DbInterno db = new DbInterno(ctx);
        try {
            ContentValues r = db.getRecord("select stato, data_posa, data_assistenza from elementi_cantiere where id_elemento_cant=" + idElementoCant);
            if (r == null) return "";
            String stato = r.getAsString("stato") == null ? "" : r.getAsString("stato");
            String posa = r.getAsString("data_posa"), ass = r.getAsString("data_assistenza");
            String testo;
            switch (stato) {
                case "POSATO":
                    testo = "Posato" + (posa != null && posa.length() >= 10 ? " il " + dataIt(posa) : "");
                    break;
                case "ORDINATO":
                    testo = "Ordinato, da posare";
                    break;
                case "DISMESSO":
                    testo = "Dismesso";
                    break;
                default:
                    return "";
            }
            if (ass != null && ass.length() >= 10) testo += " · assistenza " + dataIt(ass);
            return testo;
        } catch (Exception e) {
            return "";
        } finally {
            db.close();
        }
    }

    private static String dataIt(String iso) {
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

    static int intero(ContentValues v, String k) {
        try {
            Integer i = v.getAsInteger(k);
            return i == null ? 0 : i;
        } catch (Exception e) {
            return 0;
        }
    }
}
