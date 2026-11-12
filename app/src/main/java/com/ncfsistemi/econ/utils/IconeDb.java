package com.ncfsistemi.econ.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.LruCache;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Icone;

import java.util.HashSet;
import java.util.Set;

/**
 * Icone del disegno lette dalla tabella {@code icone} (schema 45, STUDIO_DESIGN.md §8.5 A2): il server le manda con la sync
 * e qui diventano bitmap, tenute in memoria per non rileggerle a ogni riga di elenco. Le usa Utility.getIcona quando nella
 * cartella dell'app non c'e' il file (le icone fatte sul tablet restano file). Dopo una sync si azzera ({@link #azzera()}).
 */
public final class IconeDb {

    /** Bitmap in memoria, misurate in KB (128 px: circa 64 KB l'una, quindi qualche centinaio). */
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(16 * 1024) {
        @Override
        protected int sizeOf(String chiave, Bitmap b) {
            return Math.max(1, b.getByteCount() / 1024);
        }
    };
    /** Nomi cercati e non trovati: niente query ripetute per le icone che non ci sono. */
    private static final Set<String> ASSENTI = new HashSet<>();

    private IconeDb() {}

    /** L'icona con quel nome ("prese.png" e "prese" sono la stessa), null se non c'e' o non si legge. */
    public static synchronized Bitmap bitmap(Context ctx, String icona) {
        String nome = nome(icona);
        if (nome.isEmpty() || ASSENTI.contains(nome)) return null;
        Bitmap b = CACHE.get(nome);
        if (b != null) return b;
        b = leggi(ctx.getApplicationContext(), nome);
        if (b == null) {
            ASSENTI.add(nome);
            return null;
        }
        CACHE.put(nome, b);
        return b;
    }

    /** Da chiamare dopo aver scaricato dati: le icone possono essere cambiate o arrivate. */
    public static synchronized void azzera() {
        CACHE.evictAll();
        ASSENTI.clear();
    }

    /** Come Icona::nomeSenzaEstensione del server. */
    static String nome(String icona) {
        if (icona == null) return "";
        String n = icona.trim();
        int barra = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
        if (barra >= 0) n = n.substring(barra + 1);
        return n.replaceFirst("(?i)\\.(png|jpe?g|webp|gif|svg)$", "");
    }

    private static Bitmap leggi(Context ctx, String nome) {
        DbInterno helper = new DbInterno(ctx);
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            // prima quella della ditta, poi quella di sistema (id_ditta 0)
            try (Cursor c = db.rawQuery("SELECT " + Icone.IMMAGINE + ", " + Icone.FORMATO + " FROM " + Icone.NOME_TABELLA
                    + " WHERE " + Icone.NOME + " = ? ORDER BY " + Icone.ID_DITTA + " DESC LIMIT 1", new String[]{nome})) {
                if (!c.moveToFirst() || c.isNull(0)) return null;
                if ("svg".equalsIgnoreCase(c.getString(1))) return null; // BitmapFactory non legge gli svg
                byte[] dati = Base64.decode(c.getString(0), Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(dati, 0, dati.length);
            }
        } catch (Exception e) {
            return null; // tabella assente (DB vecchio) o immagine rovinata: icona predefinita
        } finally {
            helper.close();
        }
    }
}
