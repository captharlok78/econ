package com.ncfsistemi.econ.utils;

import android.content.Context;
import android.database.Cursor;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.CategorieGenerali;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Categoria "classica" di appel (1 prese ... 12 vario) di una categoria degli elementi (STUDIO_DESIGN.md §8.5 A3): colori
 * e sfondi del disegno dipendevano dagli id fissi 1-12, ma le categorie importate dal server hanno id nuovi. Si riconoscono
 * dal nome (e dall'icona), come fa il portale (DisegnoCantiere::COLORI). 0 = non riconosciuta (colore del cantiere).
 */
public final class CategorieDisegno {

    public static final int PRESE = 1, INTERRUTTORI = 2, UTILIZZATORI = 3, QUADRI = 4, COMPONIBILI = 5, ALLARMI = 6,
            CAVI = 7, TUBI = 8, AUTOMAZIONI = 9, CITOFONIA = 10, TELEFONIA = 11, VARIO = 12;

    /** Parole del nome o dell'icona → categoria classica; l'ordine conta (la prima che c'e'). */
    private static final Object[][] PAROLE = {
            {"pres", PRESE}, {"interrut", INTERRUTTORI}, {"utilizzat", UTILIZZATORI}, {"quadr", QUADRI},
            {"componib", COMPONIBILI}, {"scatol", COMPONIBILI}, {"allarm", ALLARMI}, {"cav", CAVI}, {"tub", TUBI},
            {"automa", AUTOMAZIONI}, {"autama", AUTOMAZIONI}, {"cancell", AUTOMAZIONI}, {"citofon", CITOFONIA},
            {"telefon", TELEFONIA}, {"rete", TELEFONIA}, {"reti", TELEFONIA}, {"vari", VARIO},
    };

    private static Map<Integer, Integer> classiche;

    private CategorieDisegno() {}

    /** Categoria classica di quella dell'elemento; se la categoria non e' nel DB resta il suo id (DB vecchi di appel). */
    public static synchronized int classica(int idCategoria) {
        if (idCategoria <= 0) return 0;
        if (classiche == null) carica();
        Integer c = classiche.get(idCategoria);
        return c != null ? c : idCategoria;
    }

    /** Da chiamare dopo una sync: le categorie possono essere cambiate. */
    public static synchronized void azzera() {
        classiche = null;
    }

    /** Riconoscimento dal nome, poi dall'icona (es. "Componibili" con icona "scatole.png"). */
    static int riconosci(String nome, String icona) {
        int c = perParole(nome);
        return c != 0 ? c : perParole(icona);
    }

    private static int perParole(String testo) {
        if (testo == null || testo.trim().isEmpty()) return 0;
        String t = testo.toLowerCase(Locale.ITALIAN);
        for (Object[] p : PAROLE) {
            if (t.contains((String) p[0])) return (Integer) p[1];
        }
        return 0;
    }

    private static void carica() {
        classiche = new HashMap<>();
        Context ctx = com.ncfsistemi.econ.EconApplication.contesto();
        if (ctx == null) return;
        DbInterno helper = new DbInterno(ctx);
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT " + CategorieGenerali.ID_CATEGORIA_GENERALE + ", "
                + CategorieGenerali.NOME + ", " + CategorieGenerali.ICONA + " FROM " + CategorieGenerali.NOME_TABELLA, null)) {
            while (c.moveToNext()) {
                classiche.put(c.getInt(0), riconosci(c.getString(1), c.getString(2)));
            }
        } catch (Exception e) {
            // DB non pronto: si riprova alla prossima sync
        } finally {
            helper.close();
        }
    }
}
