package com.ncfsistemi.econ.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Map;

import com.ncfsistemi.econ.api.MercuryApiService;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Listini;
import retrofit2.Response;

/**
 * Listino locale = solo gli articoli che spettano alla ditta (configurazione catalogo settori → linee decisa dal Super
 * Amministratore). Si allinea a ogni login/apertura (MenuActivity) e a ogni sincronizzazione: l'app manda l'impronta
 * dell'elenco che possiede e l'ultimo allineamento; il server risponde con l'elenco completo se l'impronta e' cambiata
 * (l'app sostituisce il listino), altrimenti con i soli articoli modificati. Dallo schema 24 il listino non arriva
 * piu' dalla sync generale.
 */
public final class CatalogoLocale {

    private static final String TAG     = "CatalogoLocale";
    private static final String PREFS   = "CATALOGO_LOCALE";
    private static final String K_FIRMA = "firma";
    private static final String K_SINCE = "since";

    /** Login, apertura e sync possono partire insieme: un allineamento alla volta. */
    private static final Object LOCK = new Object();

    private CatalogoLocale() {}

    /**
     * Allinea il listino locale. Blocca sulla rete: chiamare da un thread di background. In caso di errore lancia
     * l'eccezione e lascia invariati listino e impronta (al prossimo allineamento si riprova).
     *
     * @return numero di articoli ricevuti (0 se nulla e' cambiato)
     */
    public static int allinea(Context ctx, MercuryApiService api) throws Exception {
        synchronized (LOCK) {
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Response<MercuryApiService.CatalogoResponse> resp =
                    api.getCatalogo(prefs.getString(K_FIRMA, ""), prefs.getString(K_SINCE, "1970-01-01T00:00:00")).execute();
            if (!resp.isSuccessful() || resp.body() == null) {
                throw new Exception("HTTP " + resp.code());
            }
            MercuryApiService.CatalogoResponse body = resp.body();
            int ricevuti = body.articoli != null ? body.articoli.size() : 0;

            DbInterno dbInterno = new DbInterno(ctx);
            SQLiteDatabase db = dbInterno.getWritableDatabase();
            try {
                db.beginTransaction();
                try {
                    if (body.completo) {
                        // restano solo le proposte di articoli fatte in locale e non ancora inviate al server
                        db.delete(Listini.NOME_TABELLA, "in_server IS NULL OR in_server <> 0", null);
                    }
                    if (body.articoli != null) {
                        for (JsonObject record : body.articoli) {
                            ContentValues cv = aContentValues(record);
                            if (!body.completo && SyncUtil.isModificatoInLocale(db, Listini.NOME_TABELLA, Listini.CODICE_ARTICOLO, cv)) {
                                continue;
                            }
                            cv.put("in_server", 1);
                            SyncUtil.normalizzaDate(cv);
                            SyncUtil.rimuoviColonneSconosciute(db, Listini.NOME_TABELLA, cv);
                            SyncUtil.normalizzaNulli(db, Listini.NOME_TABELLA, cv);
                            db.insertWithOnConflict(Listini.NOME_TABELLA, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            } finally {
                dbInterno.close();
            }

            prefs.edit().putString(K_FIRMA, body.firma).putString(K_SINCE, body.timestamp).apply();
            Log.i(TAG, (body.completo ? "Listino sostituito: " : "Articoli aggiornati: ") + ricevuti);
            return ricevuti;
        }
    }

    /** Allineamento in background, senza avvisi: se fallisce (es. offline) riprova al prossimo login/apertura/sync. */
    public static void allineaInBackground(Context ctx) {
        final Context app = ctx.getApplicationContext();
        if (!com.ncfsistemi.econ.api.TokenManager.getInstance(app).hasToken()) return;
        new Thread(() -> {
            try {
                allinea(app, com.ncfsistemi.econ.api.MercuryApiClient.getInstance(app).getService());
            } catch (Exception e) {
                Log.w(TAG, "Allineamento catalogo non riuscito: " + e.getMessage());
            }
        }, "catalogo-allinea").start();
    }

    /** Da chiamare quando la ditta cambia o si fa logout: il prossimo allineamento riscarica tutto. */
    public static void cancella(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    private static ContentValues aContentValues(JsonObject json) {
        ContentValues cv = new ContentValues();
        for (Map.Entry<String, JsonElement> e : json.entrySet()) {
            JsonElement el = e.getValue();
            if (el == null || el.isJsonNull()) {
                cv.putNull(e.getKey());
            } else if (el.isJsonPrimitive()) {
                JsonPrimitive p = el.getAsJsonPrimitive();
                if (p.isNumber()) {
                    String s = p.getAsString();
                    if (s.contains(".") || s.contains("E") || s.contains("e")) cv.put(e.getKey(), p.getAsDouble());
                    else cv.put(e.getKey(), p.getAsLong());
                } else if (p.isBoolean()) {
                    cv.put(e.getKey(), p.getAsBoolean() ? 1 : 0);
                } else {
                    cv.put(e.getKey(), p.getAsString());
                }
            }
        }
        return cv;
    }
}
