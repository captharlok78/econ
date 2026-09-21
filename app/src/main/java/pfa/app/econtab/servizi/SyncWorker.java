package pfa.app.econtab.servizi;

import pfa.app.econtab.utils.SyncUtil;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import pfa.app.econtab.api.MercuryApiClient;
import pfa.app.econtab.api.MercuryApiService;
import pfa.app.econtab.api.TokenManager;
import pfa.app.econtab.db.DbInterno;
import retrofit2.Response;

/**
 * Worker WorkManager che sostituisce EConTabService + Sincronizzatore.java.
 *
 * WorkManager gestisce automaticamente:
 * - Retry in caso di errore di rete
 * - Rispetto delle Battery Optimization (Doze mode)
 * - Esecuzione anche dopo riavvio del dispositivo
 *
 * Scheduling:
 *   SyncWorker.schedulePeriodicSync(context)   → ogni 15 minuti quando c'è rete
 *   SyncWorker.scheduleImmediateSync(context)  → una tantum adesso
 */
public class SyncWorker extends Worker {

    private static final String TAG          = "SyncWorker";
    private static final String PREFS_NAME   = "ECONTAB";
    private static final String KEY_LAST_SYNC = "DATA_ULTIMA_SINCRONIZZAZIONE";

    /**
     * Tabelle sincronizzabili: {nomeTabella, colonnaPK_o_null_se_PK_composita}.
     * L'ordine rispecchia le dipendenze FK (genitori prima dei figli).
     */
    private static final String[][] SYNC_TABLES = {
        {"anagrafica",             "id_anagrafica"},
        {"cantieri",               "id_cantiere"},
        {"unita",                  "id_unita"},
        {"aree",                   "id_area"},
        {"locali",                 "id_locale"},
        {"elementi_cantiere",      "id_elemento_cant"},
        {"componenti_cantiere",    "id_componente_cant"},
        {"composizioni_cantiere",  null},              // PK composita: id_elemento_cant + id_componente_cant
        {"componenti_cant_composti", null},            // PK composita
        {"collegamenti",           "id_collegamento"},
        {"relazioni",              "id_relazione"},
        {"preventivi",             "id_preventivo"},
        {"preventivi_dettaglio",   "id_preventivo_dettaglio"},
        {"rapportini",             "id_rapportino"},
        {"rapportini_dettaglio",   "id_rapportino_dettaglio"},
        {"foto",                   "id_foto"},
    };

    /**
     * Relazioni FK: {tabellaParent, colPK, tabellaFiglio, colFK}.
     * Usato in applyIdMappings per aggiornare i riferimenti dopo remapping degli id locali.
     */
    private static final String[][] FK_CHILDREN = {
        {"anagrafica",        "id_anagrafica",   "cantieri",              "id_anagrafica"},
        {"cantieri",          "id_cantiere",     "unita",                 "id_cantiere"},
        {"cantieri",          "id_cantiere",     "preventivi",            "id_cantiere"},
        {"cantieri",          "id_cantiere",     "rapportini",            "id_cantiere"},
        {"cantieri",          "id_cantiere",     "elementi_cantiere",     "id_cantiere"},
        {"cantieri",          "id_cantiere",     "componenti_cantiere",   "id_cantiere"},
        {"unita",             "id_unita",        "aree",                  "id_unita"},
        {"unita",             "id_unita",        "componenti_cantiere",   "id_unita"},
        {"aree",              "id_area",         "locali",                "id_area"},
        {"aree",              "id_area",         "componenti_cantiere",   "id_area"},
        {"locali",            "id_locale",       "elementi_cantiere",     "id_locale"},
        {"locali",            "id_locale",       "componenti_cantiere",   "id_locale"},
        {"preventivi",        "id_preventivo",   "preventivi_dettaglio",  "id_preventivo"},
        {"preventivi",        "id_preventivo",   "elementi_cantiere",     "id_preventivo"},
        {"preventivi",        "id_preventivo",   "componenti_cantiere",   "id_preventivo"},
        {"rapportini",        "id_rapportino",   "rapportini_dettaglio",  "id_rapportino"},
        // FK intra-tabella e verso tabelle composite
        {"elementi_cantiere", "id_elemento_cant","componenti_cantiere",   "id_elemento_cavo"},
        {"elementi_cantiere", "id_elemento_cant","componenti_cantiere",   "id_elemento_tubo"},
        {"elementi_cantiere", "id_elemento_cant","elementi_cantiere",     "id_elemento_cant_origine"},
        {"elementi_cantiere", "id_elemento_cant","composizioni_cantiere", "id_elemento_cant"},
        {"elementi_cantiere", "id_elemento_cant","relazioni",             "id_elemento_cant1"},
        {"elementi_cantiere", "id_elemento_cant","relazioni",             "id_elemento_cant2"},
        {"componenti_cantiere","id_componente_cant","composizioni_cantiere","id_componente_cant"},
        {"componenti_cantiere","id_componente_cant","relazioni",          "id_componente_cant1"},
        {"componenti_cantiere","id_componente_cant","relazioni",          "id_componente_cant2"},
        {"componenti_cantiere","id_componente_cant","componenti_cant_composti","id_componente_cant_padre"},
        {"componenti_cantiere","id_componente_cant","componenti_cant_composti","id_componente_cant_figlio"},
    };

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        if (!TokenManager.getInstance(ctx).hasToken()) {
            Log.w(TAG, "Nessun token JWT disponibile — skip sync");
            return Result.failure();
        }

        try {
            MercuryApiService api   = MercuryApiClient.getInstance(ctx).getService();
            String            since = getLastSyncTimestamp(ctx);

            Log.i(TAG, "Avvio sync delta, since=" + since);

            // 1. Scarica modifiche dal server
            Response<MercuryApiService.SyncDownloadResponse> dlResp =
                    api.syncDownload(since).execute();

            if (!dlResp.isSuccessful() || dlResp.body() == null) {
                Log.e(TAG, "Download fallito: " + dlResp.code());
                return Result.retry();
            }

            MercuryApiService.SyncDownloadResponse dl = dlResp.body();
            applyDownloadedData(ctx, dl);

            // 1b. Dati ditta e logo: scaricati solo se diversi da quelli locali. Un errore qui non ferma la sync.
            try {
                pfa.app.econtab.utils.DittaLocale.sincronizza(ctx, api);
            } catch (Exception e) {
                Log.w(TAG, "Sync dati ditta fallita: " + e.getMessage());
            }

            // 2. Scarica record eliminati
            Response<MercuryApiService.DeletedResponse> delResp =
                    api.getDeleted(since).execute();

            if (delResp.isSuccessful() && delResp.body() != null) {
                applyDeletions(ctx, delResp.body());
            }

            // 3. Carica modifiche locali (INSERT/UPDATE/DELETE). Unita' di misura: solo quelle del server.
            DbInterno dbUm = new DbInterno(ctx);
            SyncUtil.allineaUnitaMisura(dbUm.getWritableDatabase());
            dbUm.close();
            MercuryApiService.SyncUploadRequest uploadReq = buildUploadRequest(ctx, since);
            if (hasLocalChanges(uploadReq)) {
                Response<MercuryApiService.SyncUploadResponse> upResp =
                        api.syncUpload(uploadReq).execute();

                if (upResp.isSuccessful() && upResp.body() != null) {
                    applyIdMappings(ctx, upResp.body());
                }
            }

            // 4. Aggiorna timestamp ultima sincronizzazione
            saveLastSyncTimestamp(ctx, dl.syncTimestamp);

            Log.i(TAG, "Sync completata. Nuovo timestamp: " + dl.syncTimestamp);
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Errore sync: " + e.getMessage(), e);
            return Result.retry();
        }
    }

    // ── Download ────────────────────────────────────────────────────────────

    private void applyDownloadedData(Context ctx, MercuryApiService.SyncDownloadResponse dl) {
        if (dl.tables == null || dl.tables.isEmpty()) return;

        SQLiteDatabase db = new DbInterno(ctx).getWritableDatabase();
        try {
            db.beginTransaction();
            try {
                for (Map.Entry<String, List<JsonObject>> entry : dl.tables.entrySet()) {
                    String nomeTabella = entry.getKey();
                    List<JsonObject> records = entry.getValue();
                    if (records == null || records.isEmpty()) continue;
                    SyncUtil.svuotaSeElencoCompleto(db, nomeTabella, records);
                    if (SyncUtil.TABELLA_OPERATORI.equals(nomeTabella)) {
                        SyncUtil.applicaOperatori(db, records);
                        continue;
                    }

                    for (JsonObject record : records) {
                        ContentValues cv = jsonToContentValues(record);
                        cv.put("in_server", 1);
                        SyncUtil.normalizzaDate(cv);
                        SyncUtil.rimuoviColonneSconosciute(db, nomeTabella, cv);
                        SyncUtil.normalizzaNulli(db, nomeTabella, cv);
                        db.insertWithOnConflict(nomeTabella, null, cv,
                                SQLiteDatabase.CONFLICT_REPLACE);
                    }
                    Log.d(TAG, "Applicati " + records.size() + " record in " + nomeTabella);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } finally {
            db.close();
        }
    }

    private void applyDeletions(Context ctx, MercuryApiService.DeletedResponse resp) {
        if (resp.deleted == null || resp.deleted.isEmpty()) return;
        SQLiteDatabase db = new DbInterno(ctx).getWritableDatabase();
        try {
            for (MercuryApiService.DeletedResponse.DeletedItem item : resp.deleted) {
                String pkCond = buildPkWhereClause(item.tabella, item.chiaveRecord);
                db.execSQL("DELETE FROM " + item.tabella + " WHERE " + pkCond);
                Log.d(TAG, "Eliminato: " + item.tabella + " #" + item.chiaveRecord);
            }
        } finally {
            db.close();
        }
    }

    // ── Upload ──────────────────────────────────────────────────────────────

    /**
     * Raccoglie dal DB locale tutti i record da caricare sul server:
     * - in_server=0, pk<0 → nuovi creati localmente (INSERT)
     * - in_server=0, pk>0 → modificati localmente (UPDATE)
     * - tabella record_eliminati → eliminati localmente (DELETE)
     * Per le tabelle con PK composita, tutti i record in_server=0 vanno in INSERT.
     */
    private MercuryApiService.SyncUploadRequest buildUploadRequest(Context ctx, String since) {
        MercuryApiService.SyncUploadRequest req = new MercuryApiService.SyncUploadRequest();
        req.insert = new HashMap<>();
        req.update = new HashMap<>();
        req.delete = new HashMap<>();

        SQLiteDatabase db = new DbInterno(ctx).getReadableDatabase();
        try {
            for (String[] tableInfo : SYNC_TABLES) {
                String tableName = tableInfo[0];
                String pkCol     = tableInfo[1];   // null per PK composita

                if (!hasColumn(db, tableName, "in_server")) continue;

                Cursor c = db.rawQuery(
                        "SELECT * FROM " + tableName + " WHERE in_server = 0", null);
                try {
                    while (c.moveToNext()) {
                        JsonObject json = cursorToJson(c);

                        if (pkCol == null) {
                            // PK composita: inviato sempre come insert (server usa upsert)
                            req.insert.computeIfAbsent(tableName, k -> new ArrayList<>()).add(json);
                        } else {
                            int pkIdx = c.getColumnIndex(pkCol);
                            long pkVal = (pkIdx >= 0) ? c.getLong(pkIdx) : 0;
                            if (pkVal < 0) {
                                req.insert.computeIfAbsent(tableName, k -> new ArrayList<>()).add(json);
                            } else {
                                req.update.computeIfAbsent(tableName, k -> new ArrayList<>()).add(json);
                            }
                        }
                    }
                } finally {
                    c.close();
                }
            }

            // Record eliminati localmente
            Cursor dc = db.rawQuery(
                    "SELECT tabella, chiave_record FROM record_eliminati", null);
            try {
                while (dc.moveToNext()) {
                    String tbl = dc.getString(0);
                    String key = dc.getString(1);
                    req.delete.computeIfAbsent(tbl, k -> new ArrayList<>()).add(key);
                }
            } finally {
                dc.close();
            }

        } finally {
            db.close();
        }

        Log.i(TAG, "Upload: insert=" + totalRecords(req.insert)
                + " update=" + totalRecords(req.update)
                + " delete=" + totalStrings(req.delete));
        return req;
    }

    private boolean hasLocalChanges(MercuryApiService.SyncUploadRequest req) {
        return totalRecords(req.insert) > 0
                || totalRecords(req.update) > 0
                || totalStrings(req.delete) > 0;
    }

    /**
     * Dopo un upload riuscito:
     * 1. Aggiorna le PK negative con gli id server restituiti in idMappings.
     * 2. Aggiorna a cascata le FK nei record figli (anche se hanno id negativi non ancora rimappati).
     * 3. Marca tutti i record rimanenti in_server=0 → in_server=1.
     * 4. Svuota la tabella record_eliminati (tutti caricati con successo).
     */
    private void applyIdMappings(Context ctx, MercuryApiService.SyncUploadResponse resp) {
        SQLiteDatabase db = new DbInterno(ctx).getWritableDatabase();
        try {
            db.beginTransaction();
            try {
                // 1+2: rimappa PK e FK per i nuovi record
                if (resp.idMappings != null) {
                    for (Map.Entry<String, Map<String, Integer>> tableEntry
                            : resp.idMappings.entrySet()) {
                        String tableName = tableEntry.getKey();
                        String pkCol     = getPkCol(tableName);
                        if (pkCol == null) continue;

                        for (Map.Entry<String, Integer> mapping : tableEntry.getValue().entrySet()) {
                            int oldId = Integer.parseInt(mapping.getKey());
                            int newId = mapping.getValue();

                            // Aggiorna la PK nella tabella padre e segna come sincronizzato
                            ContentValues cv = new ContentValues();
                            cv.put(pkCol, newId);
                            cv.put("in_server", 1);
                            db.update(tableName, cv, pkCol + " = ?",
                                    new String[]{String.valueOf(oldId)});

                            // Aggiorna le FK nei figli
                            for (String[] fk : FK_CHILDREN) {
                                if (fk[0].equals(tableName) && fk[1].equals(pkCol)) {
                                    ContentValues fkCv = new ContentValues();
                                    fkCv.put(fk[3], newId);
                                    db.update(fk[2], fkCv, fk[3] + " = ?",
                                            new String[]{String.valueOf(oldId)});
                                }
                            }

                            Log.d(TAG, "Rimappato " + tableName + " " + oldId + "→" + newId);
                        }
                    }
                }

                // 3: marca come sincronizzati i record rimasti con in_server=0 (aggiornati e PK-composite),
                //    tranne quelli che il server ha rifiutato: restano in_server=0 e vengono reinviati
                SyncUtil.EsitoUpload esito = SyncUtil.analizzaErrori(resp.errors);
                for (String err : (resp.errors != null ? resp.errors : new ArrayList<String>())) {
                    Log.w(TAG, "Upload server error: " + err);
                }
                for (String[] tableInfo : SYNC_TABLES) {
                    String tableName = tableInfo[0];
                    if (!hasColumn(db, tableName, "in_server")) continue;
                    int updated = SyncUtil.marcaInviati(db, tableName, tableInfo[1], esito);
                    if (updated > 0) Log.d(TAG, "Marcati sync: " + updated + " record in " + tableName);
                }

                // 4: svuota record_eliminati (caricati con successo)
                db.delete("record_eliminati", null, null);
                Log.d(TAG, "Tabella record_eliminati svuotata");

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } finally {
            db.close();
        }
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    /** Converte una riga di Cursor in JsonObject. */
    private JsonObject cursorToJson(Cursor c) {
        JsonObject obj = new JsonObject();
        for (int i = 0; i < c.getColumnCount(); i++) {
            String col = c.getColumnName(i);
            switch (c.getType(i)) {
                case Cursor.FIELD_TYPE_NULL:
                    obj.add(col, JsonNull.INSTANCE);
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    obj.addProperty(col, c.getLong(i));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    obj.addProperty(col, c.getDouble(i));
                    break;
                default:
                    obj.addProperty(col, c.getString(i));
                    break;
            }
        }
        return obj;
    }

    /** Converte un JsonObject dal server in ContentValues per SQLite. */
    private ContentValues jsonToContentValues(JsonObject json) {
        ContentValues cv = new ContentValues();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            com.google.gson.JsonElement el = entry.getValue();
            if (el.isJsonNull()) {
                cv.putNull(key);
            } else if (el.isJsonPrimitive()) {
                com.google.gson.JsonPrimitive p = el.getAsJsonPrimitive();
                if (p.isBoolean()) {
                    cv.put(key, p.getAsBoolean() ? 1 : 0);
                } else if (p.isNumber()) {
                    String s = p.getAsString();
                    if (s.contains(".")) {
                        cv.put(key, p.getAsDouble());
                    } else {
                        cv.put(key, p.getAsLong());
                    }
                } else {
                    cv.put(key, p.getAsString());
                }
            }
        }
        return cv;
    }

    /** Verifica se una colonna esiste nella tabella SQLite. */
    private boolean hasColumn(SQLiteDatabase db, String tableName, String colName) {
        Cursor c = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
        try {
            int nameIdx = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (colName.equals(c.getString(nameIdx))) return true;
            }
        } finally {
            c.close();
        }
        return false;
    }

    /** Ritorna la colonna PK per una tabella (null se non definita o PK composita). */
    private String getPkCol(String tableName) {
        for (String[] tableInfo : SYNC_TABLES) {
            if (tableInfo[0].equals(tableName)) return tableInfo[1];
        }
        return null;
    }

    private String buildPkWhereClause(String tabella, String chiaveRecord) {
        String pkCol = getPkCol(tabella);
        if (pkCol != null) return pkCol + " = " + chiaveRecord;
        // Fallback per tabelle sconosciute
        return "id = " + chiaveRecord;
    }

    private int totalRecords(Map<String, List<JsonObject>> map) {
        if (map == null) return 0;
        int n = 0;
        for (List<JsonObject> l : map.values()) n += l.size();
        return n;
    }

    private int totalStrings(Map<String, List<String>> map) {
        if (map == null) return 0;
        int n = 0;
        for (List<String> l : map.values()) n += l.size();
        return n;
    }

    private String getLastSyncTimestamp(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String stored = prefs.getString(KEY_LAST_SYNC, null);
        if (stored == null || stored.length() < 8) {
            return "1970-01-01T00:00:00";
        }
        // Converte da formato vecchio yyyyMMddHHmmss → ISO 8601
        if (stored.length() == 14 && !stored.contains("T")) {
            return stored.substring(0, 4) + "-" +
                   stored.substring(4, 6) + "-" +
                   stored.substring(6, 8) + "T" +
                   stored.substring(8, 10) + ":" +
                   stored.substring(10, 12) + ":" +
                   stored.substring(12, 14);
        }
        return stored;
    }

    private void saveLastSyncTimestamp(Context ctx, String isoTimestamp) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_SYNC, isoTimestamp)
                .apply();
    }

    // ── Scheduling ─────────────────────────────────────────────────────────

    /** Avvia la sync periodica ogni 15 minuti (solo con rete disponibile) */
    public static void schedulePeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        "mercury_sync",
                        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                        request
                );
    }

    /** Avvia una sync immediata una tantum */
    public static void scheduleImmediateSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueue(request);
    }

    /** Cancella tutte le sync programmate */
    public static void cancelSync(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork("mercury_sync");
    }
}
