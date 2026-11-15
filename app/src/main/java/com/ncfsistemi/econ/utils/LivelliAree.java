package com.ncfsistemi.econ.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ncfsistemi.econ.EconApplication;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.LivelliArea;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Livelli (piani) delle aree dal catalogo del server livelli_area (schema 50, STUDIO_DESIGN_GRAFICA.md §20). Prima della
 * sincronizzazione (o senza la riga di un valore) il nome si calcola come prima: -1 Scantinato, 0 Piano terra, 1 Piano 1...
 * Letti al più ogni 30 secondi (le righe della struttura chiedono il nome a ogni disegno).
 */
public final class LivelliAree {

    private static final long VALIDITA_MS = 30_000;
    /** valore -> {nome, attivo "1"/"0"} */
    private static Map<Integer, String[]> cache;
    private static long letti;

    private LivelliAree() {
    }

    private static synchronized Map<Integer, String[]> tutti() {
        if (cache != null && System.currentTimeMillis() - letti < VALIDITA_MS) return cache;
        Map<Integer, String[]> m = new TreeMap<>();
        Context ctx = EconApplication.contesto();
        if (ctx != null) {
            DbInterno db = new DbInterno(ctx);
            try {
                SQLiteDatabase sql = db.getReadableDatabase();
                try (Cursor c = sql.rawQuery("select " + LivelliArea.VALORE + ", " + LivelliArea.NOME + ", " + LivelliArea.ATTIVO
                        + " from " + LivelliArea.NOME_TABELLA, null)) {
                    while (c.moveToNext()) {
                        String nome = c.isNull(1) ? "" : c.getString(1).trim();
                        if (nome.isEmpty()) continue;
                        m.put(c.getInt(0), new String[]{nome, c.isNull(2) || c.getInt(2) != 0 ? "1" : "0"});
                    }
                }
            } catch (Exception ignorata) {
                // tabella non ancora creata o scaricata: nomi calcolati
            } finally {
                db.close();
            }
        }
        cache = m;
        letti = System.currentTimeMillis();
        return m;
    }

    /** "Piano terra", "Scantinato", "Sottotetto"... */
    public static String nome(int valore) {
        String[] r = tutti().get(valore);
        if (r != null) return r[0];
        if (valore < -1) return "Interrato " + valore;
        if (valore == -1) return "Scantinato";
        if (valore == 0) return "Piano terra";
        return "Piano " + valore;
    }

    /** Scelte {valore, "valore · nome"}: i livelli attivi più quello attuale; senza catalogo da -3 a 10. */
    public static String[][] voci(int attuale) {
        Map<Integer, String> scelte = new TreeMap<>();
        for (Map.Entry<Integer, String[]> e : tutti().entrySet()) {
            if ("1".equals(e.getValue()[1]) || e.getKey() == attuale) scelte.put(e.getKey(), e.getValue()[0]);
        }
        if (scelte.isEmpty()) {
            for (int n = -3; n <= 10; n++) scelte.put(n, nome(n));
        }
        if (!scelte.containsKey(attuale)) scelte.put(attuale, nome(attuale));
        List<String[]> out = new ArrayList<>();
        for (Map.Entry<Integer, String> e : scelte.entrySet()) {
            out.add(new String[]{String.valueOf(e.getKey()), e.getKey() + " · " + e.getValue()});
        }
        return out.toArray(new String[0][]);
    }
}
