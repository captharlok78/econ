package com.ncfsistemi.econ.utils;

import android.content.Context;
import android.database.Cursor;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.CategorieComponenti;
import com.ncfsistemi.econ.db.table.CategorieGenerali;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Categorie "classiche" di appel del disegno (STUDIO_DESIGN.md §8.5 A3, STUDIO_DESIGN_GRAFICA.md §3.1).
 * <p>
 * Il codice del disegno ragiona sugli id fissi di appel: elementi 1 prese ... 12 vario ({@link CategorieGenerali#QUADRI},
 * {@link CategorieGenerali#TUBI}...), componenti 1 scatole ... 14 vario ({@link com.ncfsistemi.econ.db.table.Componenti#SCATOLE},
 * {@code CENTRALINI}...). Le categorie che arrivano dal server hanno id qualsiasi (importate, di sistema, della ditta):
 * qui si traducono nei due sensi.
 * <ul>
 * <li>{@link #classica(Integer)} / {@link #componente(Integer)}: id del DB → numero classico, per i confronti
 * ({@code CategorieDisegno.classica(idCategoria) == CategorieGenerali.QUADRI});</li>
 * <li>{@link #idElementi(int)} / {@link #idComponenti(int)}: numero classico → id del DB, per i filtri e per le schermate
 * che ricevono la categoria ({@code put(ID_CATEGORIA_GENERALE, CategorieDisegno.idElementi(CategorieGenerali.TUBI))}).</li>
 * </ul>
 * Si riconoscono dal {@code codice} della categoria (sync schema &gt;= 46), se no dal nome e poi dall'icona, con le stesse
 * parole del server (categorie_disegno.json). Una categoria non riconosciuta resta col suo id (DB vecchi di appel, dove
 * id e numero classico coincidono); 0 se l'id è nullo.
 */
public final class CategorieDisegno {

    // Elementi (categorie_generali)
    public static final int PRESE = 1, INTERRUTTORI = 2, UTILIZZATORI = 3, QUADRI = 4, COMPONIBILI = 5, ALLARMI = 6,
            CAVI = 7, TUBI = 8, AUTOMAZIONI = 9, CITOFONIA = 10, TELEFONIA = 11, VARIO = 12;

    /** Codice di sistema (server) → numero classico degli elementi. */
    private static final Object[][] CODICI_ELEMENTI = {
            {"PRESE", PRESE}, {"INTERRUTTORI", INTERRUTTORI}, {"UTILIZZATORI", UTILIZZATORI}, {"QUADRI", QUADRI},
            {"COMPONIBILI", COMPONIBILI}, {"ALLARMI", ALLARMI}, {"CAVI", CAVI}, {"TUBI", TUBI},
            {"AUTOMAZIONI", AUTOMAZIONI}, {"CITOFONIA", CITOFONIA}, {"RETI", TELEFONIA}, {"VARIO", VARIO},
    };
    /** Parole del nome o dell'icona → numero classico; l'ordine conta (la prima che c'è). */
    private static final Object[][] PAROLE_ELEMENTI = {
            {"pres", PRESE}, {"interrut", INTERRUTTORI}, {"utilizzat", UTILIZZATORI}, {"quadr", QUADRI},
            {"componib", COMPONIBILI}, {"scatol", COMPONIBILI}, {"allarm", ALLARMI}, {"cav", CAVI}, {"tub", TUBI},
            {"automa", AUTOMAZIONI}, {"autama", AUTOMAZIONI}, {"cancell", AUTOMAZIONI}, {"citofon", CITOFONIA},
            {"tvcc", CITOFONIA}, {"telefon", TELEFONIA}, {"rete", TELEFONIA}, {"reti", TELEFONIA}, {"vari", VARIO},
    };

    /** Codice di sistema → numero classico dei componenti (come Componenti.SCATOLE ... COMPONENTI_INTERRUTTORI). */
    private static final Object[][] CODICI_COMPONENTI = {
            {"SCATOLE", 1}, {"PORTAFRUTTI", 2}, {"PRESE", 3}, {"INTERRUTTORI", 4}, {"COPRISCATOLE", 5},
            {"UTILIZZATORI", 6}, {"CENTRALINI", 7}, {"COMP_QUADRI", 8}, {"COMP_INTERRUTTORI", 9}, {"ALLARMI", 10},
            {"AUTOMAZIONI", 11}, {"CITOFONIA", 12}, {"RETI", 13}, {"VARIO", 14},
    };
    private static final Object[][] PAROLE_COMPONENTI = {
            {"copriscat", 5}, {"portafrutt", 2}, {"scatol", 1}, {"comp. interrut", 9}, {"componenti interrut", 9},
            {"interrut", 4}, {"pres", 3}, {"utilizzat", 6}, {"centralin", 7}, {"quadr", 8}, {"allarm", 10},
            {"automa", 11}, {"autama", 11}, {"cancell", 11}, {"citofon", 12}, {"tvcc", 12}, {"telefon", 13},
            {"rete", 13}, {"reti", 13}, {"vari", 14},
    };

    /** id del DB → numero classico, e il contrario (il primo id trovato per quel numero). */
    private static Map<Integer, Integer> elementi, componenti;
    private static Map<Integer, Integer> idPerElementi, idPerComponenti;

    private CategorieDisegno() {}

    /** Numero classico della categoria degli elementi (es. {@link CategorieGenerali#QUADRI}); 0 se l'id è nullo. */
    public static synchronized int classica(Integer idCategoria) {
        if (idCategoria == null || idCategoria <= 0) return 0;
        if (elementi == null) carica();
        Integer c = elementi.get(idCategoria);
        return c != null ? c : idCategoria;
    }

    /** Numero classico della categoria dei componenti (es. Componenti.SCATOLE, Componenti.CENTRALINI); 0 se nullo. */
    public static synchronized int componente(Integer idCategoria) {
        if (idCategoria == null || idCategoria <= 0) return 0;
        if (componenti == null) carica();
        Integer c = componenti.get(idCategoria);
        return c != null ? c : idCategoria;
    }

    /** id nel DB della categoria degli elementi con quel numero classico (es. TUBI); il numero stesso se non c'è. */
    public static synchronized int idElementi(int classica) {
        if (idPerElementi == null) carica();
        Integer id = idPerElementi.get(classica);
        return id != null ? id : classica;
    }

    /** id nel DB della categoria dei componenti con quel numero classico (es. Componenti.COPRISCATOLA). */
    public static synchronized int idComponenti(int classica) {
        if (idPerComponenti == null) carica();
        Integer id = idPerComponenti.get(classica);
        return id != null ? id : classica;
    }

    /** Da chiamare dopo una sync: le categorie possono essere cambiate. */
    public static synchronized void azzera() {
        elementi = componenti = idPerElementi = idPerComponenti = null;
    }

    static int riconosciElemento(String codice, String nome, String icona) {
        return riconosci(CODICI_ELEMENTI, PAROLE_ELEMENTI, codice, nome, icona);
    }

    static int riconosciComponente(String codice, String nome, String icona) {
        return riconosci(CODICI_COMPONENTI, PAROLE_COMPONENTI, codice, nome, icona);
    }

    /** Dal codice di sistema; se manca dal nome, poi dall'icona (es. "Componibili" con icona "scatole.png"). */
    private static int riconosci(Object[][] codici, Object[][] parole, String codice, String nome, String icona) {
        if (codice != null && !codice.trim().isEmpty()) {
            for (Object[] c : codici) {
                if (c[0].equals(codice.trim().toUpperCase(Locale.ROOT))) return (Integer) c[1];
            }
            return 0; // categoria con un codice che il disegno classico non conosce
        }
        int c = perParole(parole, nome);
        return c != 0 ? c : perParole(parole, icona);
    }

    private static int perParole(Object[][] parole, String testo) {
        if (testo == null || testo.trim().isEmpty()) return 0;
        String t = testo.toLowerCase(Locale.ITALIAN);
        for (Object[] p : parole) {
            if (t.contains((String) p[0])) return (Integer) p[1];
        }
        return 0;
    }

    private static void carica() {
        elementi = new HashMap<>();
        componenti = new HashMap<>();
        idPerElementi = new HashMap<>();
        idPerComponenti = new HashMap<>();
        Context ctx = com.ncfsistemi.econ.EconApplication.contesto();
        if (ctx == null) return;
        DbInterno helper = new DbInterno(ctx);
        try {
            leggi(helper, CategorieGenerali.NOME_TABELLA, CategorieGenerali.ID_CATEGORIA_GENERALE, CategorieGenerali.NOME,
                    CategorieGenerali.ICONA, true);
            leggi(helper, CategorieComponenti.NOME_TABELLA, CategorieComponenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.NOME,
                    CategorieComponenti.ICONA, false);
        } catch (Exception e) {
            // DB non pronto: si riprova alla prossima sync
        } finally {
            helper.close();
        }
    }

    private static void leggi(DbInterno helper, String tabella, String colId, String colNome, String colIcona, boolean diElementi) {
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT * FROM " + tabella + " ORDER BY " + colId, null)) {
            int iId = c.getColumnIndex(colId), iNome = c.getColumnIndex(colNome), iIcona = c.getColumnIndex(colIcona);
            int iCodice = c.getColumnIndex("codice"); // dallo schema 46
            while (c.moveToNext()) {
                int id = c.getInt(iId);
                String codice = iCodice >= 0 ? c.getString(iCodice) : null;
                String nome = iNome >= 0 ? c.getString(iNome) : null;
                String icona = iIcona >= 0 ? c.getString(iIcona) : null;
                int classica = diElementi ? riconosciElemento(codice, nome, icona) : riconosciComponente(codice, nome, icona);
                (diElementi ? elementi : componenti).put(id, classica);
                Map<Integer, Integer> inverso = diElementi ? idPerElementi : idPerComponenti;
                if (classica != 0 && !inverso.containsKey(classica)) inverso.put(classica, id);
            }
        }
    }
}
