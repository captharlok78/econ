package pfa.app.econtab;

import pfa.app.econtab.utils.SyncUtil;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pfa.app.econtab.api.MercuryApiClient;
import pfa.app.econtab.api.MercuryApiService;
import pfa.app.econtab.api.TokenManager;
import pfa.app.econtab.db.DbInterno;
import retrofit2.Response;

public class SincronizzazioneActivity extends AppCompatActivity {

    private static final String TAG              = "SincronizzazioneAct";
    private static final String PREFS_NAME       = "ECONTAB";
    private static final String KEY_SYNC         = "DATA_ULTIMA_SINCRONIZZAZIONE";
    private static final int    REQUEST_RELOGIN  = 9001;

    public static final String EXTRA_MODE    = "SYNC_MODE";
    public static final String MODE_DOWNLOAD = "download";
    public static final String MODE_UPLOAD   = "upload";

    // ── Colori stato ──────────────────────────────────────────────────────────
    private static final int COLOR_PENDING  = Color.parseColor("#BDBDBD");
    private static final int COLOR_APPLYING = Color.parseColor("#FF9800");
    private static final int COLOR_DONE     = Color.parseColor("#43A047");
    private static final int COLOR_SKIP     = Color.parseColor("#90CAF9");
    private static final int COLOR_ERROR    = Color.parseColor("#E53935");
    private static final int COLOR_UPLOAD   = Color.parseColor("#FF7043"); // arancio upload

    private static final String TABELLA_DITTA = "ditta";

    // ── Tabelle DOWNLOAD (da Mercury → app) ──────────────────────────────────
    private static final String[][] TABELLE = {
        { TABELLA_DITTA,              "Dati ditta e logo" },   // non e' una tabella: vedi sincronizzaDitta()
        { SyncUtil.TABELLA_OPERATORI, "Operatori" },
        { "anagrafica",               "Clienti" },
        { "cantieri",                 "Cantieri" },
        { "aree",                     "Aree" },
        { "unita",                    "Unità" },
        { "locali",                   "Locali" },
        { "locali_porte_finestre",    "Porte / Finestre" },
        { "preventivi",               "Preventivi" },
        { "preventivi_dettaglio",     "Righe Preventivo" },
        { "rapportini",               "Rapportini" },
        { "rapportini_dettaglio",     "Righe Rapportino" },
        { "elementi",                 "Elementi" },
        { "elementi_cantiere",        "Elementi Cantiere" },
        { "elementi_codici",          "Codici Elementi" },
        { "componenti",               "Componenti" },
        { "componenti_cantiere",      "Componenti Cantiere" },
        { "componenti_composti",      "Componenti Composti" },
        { "componenti_cant_composti", "Comp. Composti Cantiere" },
        { "composizioni",             "Composizioni" },
        { "composizioni_cantiere",    "Composizioni Cantiere" },
        { "listini",                  "Listini" },
        { "costruttori",              "Costruttori" },
        { "manodopera",               "Manodopera" },
        { "categorie_generali",       "Categorie Generali" },
        { "categorie_componenti",     "Categorie Componenti" },
        { "linee",                    "Linee" },
        { "placche",                  "Placche" },
        { "placche_moduli",           "Moduli Placche" },
        { "foto",                     "Foto" },
        { "foto_elementi",            "Foto Elementi" },
        { "iva",                      "IVA" },
        { "unita_misura",             "Unità di misura" },
        { "relazioni",                "Relazioni" },
        { "collegamenti",             "Collegamenti" },
    };

    // ── Tabelle UPLOAD (app → Mercury): solo quelle con in_server e PK ────────
    private static final String[][] TABELLE_UPLOAD = {
        { "anagrafica",               "Clienti" },
        { "cantieri",                 "Cantieri" },
        { "unita",                    "Unità" },
        { "aree",                     "Aree" },
        { "locali",                   "Locali" },
        { "preventivi",               "Preventivi" },
        { "preventivi_dettaglio",     "Righe Preventivo" },
        { "rapportini",               "Rapportini" },
        { "rapportini_dettaglio",     "Righe Rapportino" },
        { "elementi_cantiere",        "Elementi Cantiere" },
        { "componenti_cantiere",      "Componenti Cantiere" },
        { "composizioni_cantiere",    "Composizioni Cantiere" },
        { "componenti_cant_composti", "Comp. Composti Cantiere" },
        { "collegamenti",             "Collegamenti" },
        { "relazioni",                "Relazioni" },
        { "foto",                     "Foto" },
        // Catalogo: ordine rispetta i vincoli FK (costruttori → linee → listini/placche → placche_moduli)
        { "costruttori",              "Costruttori" },
        { "linee",                    "Linee" },
        { "listini",                  "Listini" },
        { "placche",                  "Placche" },
        { "placche_moduli",           "Moduli Placche" },
    };

    // PK per tabella (null = PK composita, va sempre in INSERT)
    private static String pkCol(String tabella) {
        switch (tabella) {
            case "anagrafica":               return "id_anagrafica";
            case "cantieri":                 return "id_cantiere";
            case "unita":                    return "id_unita";
            case "aree":                     return "id_area";
            case "locali":                   return "id_locale";
            case "preventivi":               return "id_preventivo";
            case "preventivi_dettaglio":     return "id_preventivo_dettaglio";
            case "rapportini":               return "id_rapportino";
            case "rapportini_dettaglio":     return "id_rapportino_dettaglio";
            case "elementi_cantiere":        return "id_elemento_cant";
            case "componenti_cantiere":      return "id_componente_cant";
            case "collegamenti":             return "id_collegamento";
            case "relazioni":                return "id_relazione";
            case "foto":                     return "id_foto";
            case "costruttori":              return "id_costruttore";
            case "linee":                    return "id_linea";
            case "listini":                  return null; // PK stringa: codice_articolo → sempre INSERT
            case "placche":                  return "id_placca";
            case "placche_moduli":           return "id_placca_modulo";
            default:                         return null; // PK composita
        }
    }

    // FK da aggiornare dopo rimappatura ID (tabella_padre, pk_padre, tabella_figlio, fk_figlio)
    private static final String[][] FK_CHILDREN = {
        {"anagrafica",        "id_anagrafica",    "cantieri",               "id_anagrafica"},
        {"cantieri",          "id_cantiere",      "unita",                  "id_cantiere"},
        {"cantieri",          "id_cantiere",      "preventivi",             "id_cantiere"},
        {"cantieri",          "id_cantiere",      "elementi_cantiere",      "id_cantiere"},
        {"cantieri",          "id_cantiere",      "componenti_cantiere",    "id_cantiere"},
        {"unita",             "id_unita",         "aree",                   "id_unita"},
        {"unita",             "id_unita",         "componenti_cantiere",    "id_unita"},
        {"aree",              "id_area",          "locali",                 "id_area"},
        {"aree",              "id_area",          "componenti_cantiere",    "id_area"},
        {"locali",            "id_locale",        "elementi_cantiere",      "id_locale"},
        {"locali",            "id_locale",        "componenti_cantiere",    "id_locale"},
        {"preventivi",        "id_preventivo",    "preventivi_dettaglio",   "id_preventivo"},
        {"preventivi",        "id_preventivo",    "rapportini",             "id_ordine"},
        {"preventivi",        "id_preventivo",    "elementi_cantiere",      "id_preventivo"},
        {"preventivi",        "id_preventivo",    "componenti_cantiere",    "id_preventivo"},
        {"rapportini",        "id_rapportino",    "rapportini_dettaglio",   "id_rapportino"},
        {"elementi_cantiere", "id_elemento_cant", "componenti_cantiere",    "id_elemento_cavo"},
        {"elementi_cantiere", "id_elemento_cant", "componenti_cantiere",    "id_elemento_tubo"},
        {"elementi_cantiere", "id_elemento_cant", "elementi_cantiere",      "id_elemento_cant_origine"},
        {"elementi_cantiere", "id_elemento_cant", "composizioni_cantiere",  "id_elemento_cant"},
        {"elementi_cantiere", "id_elemento_cant", "relazioni",              "id_elemento_cant1"},
        {"elementi_cantiere", "id_elemento_cant", "relazioni",              "id_elemento_cant2"},
        {"componenti_cantiere","id_componente_cant","composizioni_cantiere","id_componente_cant"},
        {"componenti_cantiere","id_componente_cant","relazioni",            "id_componente_cant1"},
        {"componenti_cantiere","id_componente_cant","relazioni",            "id_componente_cant2"},
        {"componenti_cantiere","id_componente_cant","componenti_cant_composti","id_componente_cant_padre"},
        {"componenti_cantiere","id_componente_cant","componenti_cant_composti","id_componente_cant_figlio"},
        // Catalogo
        {"costruttori", "id_costruttore", "linee",         "id_costruttore"},
        {"costruttori", "id_costruttore", "listini",       "id_costruttore"},
        {"linee",       "id_linea",       "listini",       "id_linea"},
        {"linee",       "id_linea",       "placche",       "id_linea"},
        {"placche",     "id_placca",      "placche_moduli","id_placca"},
    };

    // ── Campi UI ──────────────────────────────────────────────────────────────
    private TextView     textSyncTitle;
    private TextView     textSyncStatus;
    private TextView     textLastSync;
    private TextView     textTabelleProgress;
    private ProgressBar  progressGlobale;
    private Button       btnSincronizza;
    private LinearLayout containerTabelle;

    private String syncMode = MODE_DOWNLOAD;

    private final Map<String, RigaTabella> righe       = new LinkedHashMap<>();
    private final Map<String, RigaTabella> righeUpload = new LinkedHashMap<>();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Set<String> tabelleConInServer = new HashSet<>();
    private int     tabelleCompletate  = 0;
    private boolean redirectingToLogin = false;

    // ── Costanti stato ────────────────────────────────────────────────────────
    private static final int STATO_PENDING   = 0;
    private static final int STATO_IN_CORSO  = 1;
    private static final int STATO_FATTO     = 2;
    private static final int STATO_SALTO     = 3;
    private static final int STATO_ERRORE    = 4;
    private static final int STATO_DA_INVIARE = 5; // upload: ci sono record

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sincronizzazione);

        String mode = getIntent().getStringExtra(EXTRA_MODE);
        syncMode = (MODE_UPLOAD.equals(mode)) ? MODE_UPLOAD : MODE_DOWNLOAD;

        textSyncTitle       = findViewById(R.id.textSyncTitle);
        textSyncStatus      = findViewById(R.id.textSyncStatus);
        textLastSync        = findViewById(R.id.textLastSync);
        textTabelleProgress = findViewById(R.id.textTabelleProgress);
        progressGlobale     = findViewById(R.id.progressGlobale);
        btnSincronizza      = findViewById(R.id.btnSincronizza);
        containerTabelle    = findViewById(R.id.containerTabelle);

        if (MODE_UPLOAD.equals(syncMode)) {
            textSyncTitle.setText("Carica su server");
            btnSincronizza.setText("CARICA SU SERVER");
            textLastSync.setVisibility(android.view.View.GONE);
        } else {
            textSyncTitle.setText("Scarica da server");
            btnSincronizza.setText("SCARICA DA SERVER");
            aggiornaEtichettaUltimaSync();
        }

        costruisciRighe();

        if (!TokenManager.getInstance(this).hasToken()) {
            btnSincronizza.setEnabled(false);
            if (getIntent().getBooleanExtra("FIRST_RUN", false)) {
                setStatus("Nessun account Mercury configurato. Configura il server dal menu (pulsante Sincronizza), poi torna qui per il download.", true);
            } else {
                setStatus("Accedi prima all'account Mercury", true);
            }
        } else if (getIntent().getBooleanExtra("FIRST_RUN", false)) {
            // Auto-avvio solo al primo lancio dopo attivazione: nessun click richiesto
            btnSincronizza.setVisibility(android.view.View.GONE);
            uiHandler.postDelayed(() -> avviaSincronizzazione(null), 300);
        }
    }

    public void chiudi(View v) {
        if (getIntent().getBooleanExtra("FIRST_RUN", false)) {
            Intent intent = new Intent(this, MenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RELOGIN && resultCode == android.app.Activity.RESULT_OK) {
            redirectingToLogin = false;
            uiHandler.postDelayed(() -> avviaSincronizzazione(null), 300);
        } else if (requestCode == REQUEST_RELOGIN) {
            // Login annullato dall'utente
            redirectingToLogin = false;
            uiHandler.post(() -> btnSincronizza.setEnabled(true));
        }
    }

    /**
     * Gestisce una risposta HTTP 401 (token scaduto o non valido):
     * cancella il token, mostra un messaggio chiaro e porta al login.
     */
    private void gestisci401() {
        redirectingToLogin = true;
        pfa.app.econtab.api.TokenManager.getInstance(this).clearToken();
        setStatus("Sessione scaduta — effettua nuovamente il login a Mercury.", true);
        uiHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            Intent intent = new Intent(SincronizzazioneActivity.this, LoginActivity.class);
            intent.putExtra(LoginActivity.EXTRA_RETURN_AFTER_LOGIN, true);
            startActivityForResult(intent, REQUEST_RELOGIN);
        }, 800);
    }

    public void avviaSincronizzazione(View v) {
        if (!TokenManager.getInstance(this).hasToken()) {
            setStatus("Nessun token JWT — esegui prima il login", true);
            return;
        }
        btnSincronizza.setEnabled(false);
        resetUI();
        new Thread(this::eseguiSync).start();
    }

    // ── Logica di sync ────────────────────────────────────────────────────────

    private void eseguiSync() {
        try {
            MercuryApiService api = MercuryApiClient.getInstance(this).getService();
            if (MODE_UPLOAD.equals(syncMode)) {
                runUploadSync(api);
            } else {
                runDownloadSync(api);
            }
        } catch (Exception e) {
            Log.e(TAG, "Errore sync", e);
            setStatus("Errore: " + e.getMessage(), true);
        } finally {
            if (!redirectingToLogin) {
                uiHandler.post(() -> btnSincronizza.setEnabled(true));
            }
        }
    }

    private void runDownloadSync(MercuryApiService api) throws Exception {
        String since = getLastSyncTimestamp();
        setStatus("Download da Mercury...", false);

        Response<MercuryApiService.SyncDownloadResponse> resp =
                api.syncDownload(since).execute();

        if (!resp.isSuccessful() || resp.body() == null) {
            if (resp.code() == 401) {
                gestisci401();
                return;
            }
            String err = "";
            if (resp.errorBody() != null) {
                try { err = resp.errorBody().string(); } catch (Exception ignored) {}
            }
            setStatus("Errore download: HTTP " + resp.code() + " " + err, true);
            return;
        }

        MercuryApiService.SyncDownloadResponse dl = resp.body();
        Map<String, List<JsonObject>> tables = dl.tables;

        int totRec = 0;
        if (tables != null) {
            for (Map.Entry<String, List<JsonObject>> e : tables.entrySet()) {
                RigaTabella riga = righe.get(e.getKey());
                if (riga != null) {
                    riga.total = e.getValue() != null ? e.getValue().size() : 0;
                    totRec    += riga.total;
                    aggiornaRiga(riga, riga.total > 0 ? STATO_PENDING : STATO_SALTO, 0, false);
                }
            }
        }
        final int totRecFin = totRec;
        uiHandler.post(() -> {
            progressGlobale.setMax(100);
            textTabelleProgress.setText("0 / " + TABELLE.length + " tabelle  ·  " + totRecFin + " record");
        });

        setStatus("Applicazione dati...", false);
        DbInterno db = new DbInterno(this);
        SQLiteDatabase sqliteDb = db.getWritableDatabase();
        rilevaTabelleConInServer(sqliteDb);
        tabelleCompletate = 0;

        if (tables != null) {
            for (String[] entry : TABELLE) {
                String           nome    = entry[0];
                if (TABELLA_DITTA.equals(nome)) continue;
                List<JsonObject> records = tables.get(nome);
                RigaTabella      riga    = righe.get(nome);
                if (records == null || records.isEmpty()) {
                    if (riga != null) aggiornaRiga(riga, STATO_SALTO, 0, false);
                    incrementaTabelleCompletate();
                    continue;
                }
                applicaRecordTabella(sqliteDb, nome, records);
            }
        }
        db.close();

        sincronizzaDitta(api);

        setStatus("Eliminazioni...", false);
        try {
            Response<MercuryApiService.DeletedResponse> delResp =
                    api.getDeleted(since).execute();
            if (delResp.isSuccessful() && delResp.body() != null) {
                applicaEliminazioni(delResp.body());
            }
        } catch (Exception e) {
            Log.w(TAG, "Errore eliminazioni", e);
        }

        if (dl.syncTimestamp != null) salvaUltimaSync(dl.syncTimestamp);
        uiHandler.post(this::aggiornaEtichettaUltimaSync);
        setStatus("Download completato ✓", false);

        if (getIntent().getBooleanExtra("FIRST_RUN", false)) {
            uiHandler.postDelayed(() -> {
                Intent intent = new Intent(SincronizzazioneActivity.this, MenuActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }, 2000);
        }
    }

    /**
     * Dati ditta + logo. La riga mostra la ragione sociale della ditta (dai dati locali, aggiornati o gia' presenti):
     * ✓ verde se e' stato scaricato qualcosa, ✓ azzurro se era gia' allineata.
     */
    private void sincronizzaDitta(MercuryApiService api) {
        RigaTabella riga = righe.get(TABELLA_DITTA);
        setStatus("Dati ditta...", false);
        aggiornaRiga(riga, STATO_IN_CORSO, 0, false);
        try {
            boolean aggiornata = pfa.app.econtab.utils.DittaLocale.sincronizza(this, api)
                    == pfa.app.econtab.utils.DittaLocale.Esito.AGGIORNATA;
            if (riga != null) riga.total = aggiornata ? 1 : 0;
            aggiornaRiga(riga, aggiornata ? STATO_FATTO : STATO_SALTO, aggiornata ? 1 : 0, false);

            MercuryApiService.DittaDati d = pfa.app.econtab.utils.DittaLocale.getDati(this);
            if (riga != null && d != null) {
                final String testo = d.ragioneSociale + (d.logo != null ? "  ·  logo" : "");
                // dopo aggiornaRiga (che accoda sul main thread la propria etichetta): la sovrascrive
                uiHandler.post(() -> {
                    riga.tvCount.setSingleLine(true);
                    riga.tvCount.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    riga.tvCount.setMaxWidth((int) (340 * getResources().getDisplayMetrics().density));
                    riga.tvCount.setText(testo);
                });
            }
        } catch (Exception e) {
            // non blocca il resto del download: al prossimo sync si riprova
            Log.w(TAG, "Errore dati ditta", e);
            if (riga != null) riga.errorMsg = e.getMessage();
            aggiornaRiga(riga, STATO_ERRORE, 0, false);
        }
        incrementaTabelleCompletate();
    }

    private void runUploadSync(MercuryApiService api) throws Exception {
        setStatus("Scansione dati locali...", false);
        // solo unita' di misura del server: i valori non validi vengono corretti prima dell'invio
        DbInterno dbUm = new DbInterno(this);
        SyncUtil.allineaUnitaMisura(dbUm.getWritableDatabase());
        dbUm.close();
        scansionaRecordLocali();
        setStatus("Upload a Mercury...", false);
        eseguiUploadLocale(api); // imposta il proprio status finale
        uiHandler.post(() -> progressGlobale.setProgress(100));
    }

    // ── Scansione pre-upload ──────────────────────────────────────────────────

    /** Legge il DB locale e popola i contatori delle righe upload. */
    private void scansionaRecordLocali() {
        DbInterno      db      = new DbInterno(this);
        SQLiteDatabase sqlite  = db.getReadableDatabase();
        try {
            for (String[] entry : TABELLE_UPLOAD) {
                String      nome = entry[0];
                RigaTabella riga = righeUpload.get(nome);
                if (riga == null) continue;

                if (!hasColumn(sqlite, nome, "in_server")) {
                    aggiornaRiga(riga, STATO_SALTO, 0, true);
                    continue;
                }
                try (Cursor c = sqlite.rawQuery(
                        "SELECT COUNT(*) FROM " + nome + " WHERE in_server = 0", null)) {
                    int count = c.moveToFirst() ? c.getInt(0) : 0;
                    riga.total = count;
                    aggiornaRiga(riga, count > 0 ? STATO_DA_INVIARE : STATO_SALTO, 0, true);
                } catch (Exception e) {
                    aggiornaRiga(riga, STATO_SALTO, 0, true);
                }
            }
            // Conta anche i record eliminati
            try (Cursor c = sqlite.rawQuery("SELECT COUNT(*) FROM record_eliminati", null)) {
                if (c.moveToFirst() && c.getInt(0) > 0) {
                    Log.d(TAG, "Record eliminati da inviare: " + c.getInt(0));
                }
            } catch (Exception ignored) {}
        } finally {
            sqlite.close();
            db.close();
        }
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    private void eseguiUploadLocale(MercuryApiService api) {
        MercuryApiService.SyncUploadRequest req = new MercuryApiService.SyncUploadRequest();
        // LinkedHashMap preserva l'ordine di inserimento (= ordine TABELLE_UPLOAD),
        // così il server riceve le tabelle padre prima delle figlie e può risolvere FK intra-batch.
        req.insert = new LinkedHashMap<>();
        req.update = new LinkedHashMap<>();
        req.delete = new LinkedHashMap<>();

        DbInterno      db     = new DbInterno(this);
        SQLiteDatabase sqlite = db.getReadableDatabase();

        try {
            // Costruisci insert/update per tabelle con in_server=0
            for (String[] entry : TABELLE_UPLOAD) {
                String      nome = entry[0];
                RigaTabella riga = righeUpload.get(nome);
                String      pk   = pkCol(nome);

                if (!hasColumn(sqlite, nome, "in_server")) continue;

                aggiornaRiga(riga, STATO_IN_CORSO, 0, true);

                try (Cursor c = sqlite.rawQuery(
                        "SELECT * FROM " + nome + " WHERE in_server = 0", null)) {

                    int count = 0;
                    while (c.moveToNext()) {
                        JsonObject json = cursorToJson(c);
                        if (pk == null) {
                            // PK composita → sempre insert (upsert lato server)
                            req.insert.computeIfAbsent(nome, k -> new ArrayList<>()).add(json);
                        } else {
                            int pkIdx = c.getColumnIndex(pk);
                            long pkVal = pkIdx >= 0 ? c.getLong(pkIdx) : 0;
                            if (pkVal < 0) {
                                req.insert.computeIfAbsent(nome, k -> new ArrayList<>()).add(json);
                            } else {
                                req.update.computeIfAbsent(nome, k -> new ArrayList<>()).add(json);
                            }
                        }
                        count++;
                    }
                    if (count == 0) aggiornaRiga(riga, STATO_SALTO, 0, true);
                } catch (Exception e) {
                    Log.w(TAG, "Errore lettura upload " + nome, e);
                    riga.errorMsg = e.getMessage();
                    aggiornaRiga(riga, STATO_ERRORE, 0, true);
                }
            }

            // Aggiungi eliminazioni locali
            try (Cursor c = sqlite.rawQuery(
                    "SELECT tabella, chiave_record FROM record_eliminati", null)) {
                while (c.moveToNext()) {
                    String tbl = c.getString(0);
                    String key = c.getString(1);
                    req.delete.computeIfAbsent(tbl, k -> new ArrayList<>()).add(key);
                }
            } catch (Exception e) {
                Log.w(TAG, "Errore lettura record_eliminati", e);
            }
        } finally {
            sqlite.close();
            db.close();
        }

        boolean haInsert = !req.insert.isEmpty();
        boolean haUpdate = !req.update.isEmpty();
        boolean haDelete = !req.delete.isEmpty();
        if (!haInsert && !haUpdate && !haDelete) {
            for (RigaTabella riga : righeUpload.values()) {
                if (riga.total == 0) aggiornaRiga(riga, STATO_SALTO, 0, true);
            }
            setStatus("Nessun dato locale da inviare", false);
            return;
        }

        // Log riepilogo di quello che stiamo per inviare
        int totInsert = req.insert.values().stream().mapToInt(List::size).sum();
        int totUpdate = req.update.values().stream().mapToInt(List::size).sum();
        int totDelete = req.delete.values().stream().mapToInt(List::size).sum();
        Log.d(TAG, "Upload: " + totInsert + " insert, " + totUpdate + " update, " + totDelete + " delete");
        setStatus("Invio " + (totInsert + totUpdate) + " record a Mercury...", false);

        try {
            Response<MercuryApiService.SyncUploadResponse> upResp =
                    api.syncUpload(req).execute();

            if (upResp.isSuccessful() && upResp.body() != null) {
                MercuryApiService.SyncUploadResponse body = upResp.body();

                // Applica mapping ID e aggiorna DB locale solo se il server conferma il successo
                applicaIdMappings(body);
                // Segna come inviati solo i record che il server ha accettato: quelli rifiutati restano
                // in_server=0 e verranno reinviati (prima venivano segnati tutti, perdendo i rifiutati)
                SyncUtil.EsitoUpload esito = SyncUtil.analizzaErrori(body.errors);
                marcaInServer1(esito);
                svuotaRecordEliminati();

                for (String[] entry : TABELLE_UPLOAD) {
                    String      nome = entry[0];
                    RigaTabella riga = righeUpload.get(nome);
                    if (riga != null && riga.total > 0) {
                        java.util.Set<String> rifiutati = esito.falliti.get(nome);
                        if (rifiutati != null) {
                            riga.errori   = rifiutati.size();
                            riga.errorMsg = esito.primoErrore.get(nome);
                            aggiornaRiga(riga, STATO_ERRORE, Math.max(0, riga.total - rifiutati.size()), true);
                        } else {
                            aggiornaRiga(riga, STATO_FATTO, riga.total, true);
                        }
                    }
                }

                if (body.errors != null && !body.errors.isEmpty()) {
                    String primo = body.errors.get(0);
                    setStatus("Upload parziale (" + body.errors.size() + " err): " + primo, true);
                    for (String err : body.errors) Log.w(TAG, "Upload server error: " + err);
                } else {
                    int inviati = totInsert + totUpdate + totDelete;
                    setStatus("Upload completato ✓  (" + inviati + " record)", false);
                }

            } else {
                if (upResp.code() == 401) {
                    for (RigaTabella riga : righeUpload.values()) {
                        if (riga.total > 0) {
                            riga.errorMsg = "Token scaduto o non valido (HTTP 401). Rieffettua il login.";
                            aggiornaRiga(riga, STATO_ERRORE, 0, true);
                        }
                    }
                    gestisci401();
                    return;
                }
                String errBody = "";
                if (upResp.errorBody() != null) {
                    try { errBody = upResp.errorBody().string(); } catch (Exception ignored) {}
                }
                // Prova a estrarre il campo "error" dal JSON
                String errMsg = "HTTP " + upResp.code();
                try {
                    org.json.JSONObject j = new org.json.JSONObject(errBody);
                    if (j.has("error"))   errMsg += ": " + j.getString("error");
                    if (j.has("message")) errMsg += " — " + j.getString("message");
                } catch (Exception ignored) {
                    if (!errBody.isEmpty()) errMsg += ": " + errBody;
                }
                Log.e(TAG, "Upload fallito: " + errMsg);
                setStatus("Errore upload: " + errMsg, true);
                for (RigaTabella riga : righeUpload.values()) {
                    if (riga.total > 0) {
                        riga.errorMsg = errMsg;
                        aggiornaRiga(riga, STATO_ERRORE, 0, true);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Errore upload", e);
            String exMsg = e.getMessage();
            setStatus("Errore upload: " + exMsg, true);
            for (RigaTabella riga : righeUpload.values()) {
                if (riga.total > 0) {
                    riga.errorMsg = exMsg;
                    aggiornaRiga(riga, STATO_ERRORE, 0, true);
                }
            }
        }
    }

    private void applicaIdMappings(MercuryApiService.SyncUploadResponse resp) {
        if (resp.idMappings == null || resp.idMappings.isEmpty()) return;
        DbInterno      db     = new DbInterno(this);
        SQLiteDatabase sqlite = db.getWritableDatabase();
        try {
            sqlite.beginTransaction();
            try {
                for (Map.Entry<String, Map<String, Integer>> tableEntry
                        : resp.idMappings.entrySet()) {
                    String tableName = tableEntry.getKey();
                    String pk        = pkCol(tableName);
                    if (pk == null) continue;

                    for (Map.Entry<String, Integer> mapping : tableEntry.getValue().entrySet()) {
                        int oldId = Integer.parseInt(mapping.getKey());
                        int newId = mapping.getValue();

                        // Se il nuovo ID assegnato dal server esiste già in locale
                        // (da un download precedente), eliminalo: è lo stesso record,
                        // ora sostituito dalla versione appena caricata.
                        sqlite.delete(tableName, pk + " = ?",
                                new String[]{String.valueOf(newId)});

                        android.content.ContentValues cv = new android.content.ContentValues();
                        cv.put(pk, newId);
                        cv.put("in_server", 1);
                        sqlite.update(tableName, cv, pk + " = ?",
                                new String[]{String.valueOf(oldId)});

                        // Aggiorna FK nei figli
                        for (String[] fk : FK_CHILDREN) {
                            if (fk[0].equals(tableName) && fk[1].equals(pk)) {
                                android.content.ContentValues fkCv = new android.content.ContentValues();
                                fkCv.put(fk[3], newId);
                                sqlite.update(fk[2], fkCv, fk[3] + " = ?",
                                        new String[]{String.valueOf(oldId)});
                            }
                        }
                        Log.d(TAG, "Rimappato " + tableName + " " + oldId + "→" + newId);
                    }
                }
                sqlite.setTransactionSuccessful();
            } finally {
                sqlite.endTransaction();
            }
        } finally {
            sqlite.close();
            db.close();
        }
    }

    private void marcaInServer1(SyncUtil.EsitoUpload esito) {
        DbInterno      db     = new DbInterno(this);
        SQLiteDatabase sqlite = db.getWritableDatabase();
        try {
            for (String[] entry : TABELLE_UPLOAD) {
                String nome = entry[0];
                if (!hasColumn(sqlite, nome, "in_server")) continue;
                SyncUtil.marcaInviati(sqlite, nome, pkCol(nome), esito);
            }
        } finally {
            sqlite.close();
            db.close();
        }
    }

    private void svuotaRecordEliminati() {
        DbInterno      db     = new DbInterno(this);
        SQLiteDatabase sqlite = db.getWritableDatabase();
        try {
            sqlite.delete("record_eliminati", null, null);
        } finally {
            sqlite.close();
            db.close();
        }
    }

    // ── Download helpers ──────────────────────────────────────────────────────

    private void applicaRecordTabella(SQLiteDatabase db, String nomeTabella, List<JsonObject> records) {
        RigaTabella riga = righe.get(nomeTabella);
        if (riga == null) return;
        aggiornaRiga(riga, STATO_IN_CORSO, 0, false);
        SyncUtil.svuotaSeElencoCompleto(db, nomeTabella, records);
        if (SyncUtil.TABELLA_OPERATORI.equals(nomeTabella)) {
            // operatori della ditta: aggiornamento dedicato (non sovrascrive le password dei vecchi utenti locali)
            int applicati = SyncUtil.applicaOperatori(db, records);
            aggiornaRiga(riga, STATO_FATTO, applicati, false);
            incrementaTabelleCompletate();
            return;
        }
        boolean haInServer = tabelleConInServer.contains(nomeTabella);

        for (int i = 0; i < records.size(); i++) {
            try {
                JsonObject record = records.get(i);
                android.content.ContentValues cv = jsonToContentValues(record);

                // Catalogo eliminato da Mercury: cancella localmente invece di aggiornare
                JsonElement statoEl = record.get("stato");
                if (statoEl != null && !statoEl.isJsonNull() && "eliminato".equals(statoEl.getAsString())) {
                    String pkColName = pkCol(nomeTabella);
                    if (pkColName != null && cv.containsKey(pkColName)) {
                        db.delete(nomeTabella, pkColName + " = ?",
                            new String[]{cv.getAsString(pkColName)});
                    } else if ("listini".equals(nomeTabella) && cv.containsKey("codice_articolo")) {
                        db.delete("listini", "codice_articolo = ?",
                            new String[]{cv.getAsString("codice_articolo")});
                    }
                    incrementaTabelleCompletate();
                    continue;
                }

                if (haInServer) {
                    // Non sovrascrivere record modificati localmente (in_server=0): verranno caricati nell'upload
                    String pkCol = pkCol(nomeTabella);
                    if (pkCol != null && cv.containsKey(pkCol)) {
                        long pkVal = cv.getAsLong(pkCol);
                        boolean isDirty = false;
                        try (Cursor ck = db.rawQuery(
                                "SELECT in_server FROM " + nomeTabella + " WHERE " + pkCol + "=?",
                                new String[]{String.valueOf(pkVal)})) {
                            if (ck.moveToFirst() && ck.getInt(0) == 0) isDirty = true;
                        }
                        if (isDirty) {
                            continue; // salta: il record locale ha modifiche non ancora uploadate
                        }
                    }
                    cv.put("in_server", 1);
                }
                SyncUtil.normalizzaDate(cv);
                SyncUtil.rimuoviColonneSconosciute(db, nomeTabella, cv);
                SyncUtil.normalizzaNulli(db, nomeTabella, cv);
                db.insertWithOnConflict(nomeTabella, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            } catch (Exception e) {
                Log.w(TAG, "Errore inserimento " + nomeTabella + ": " + e.getMessage());
                riga.errori++;
                if (riga.errorMsg == null) riga.errorMsg = e.getMessage();
            }
            final int app = i + 1, tot = records.size();
            uiHandler.post(() -> {
                if (riga.tvCount    != null) riga.tvCount.setText(app + " / " + tot);
                if (riga.progressBarra != null) {
                    riga.progressBarra.setMax(tot);
                    riga.progressBarra.setProgress(app);
                }
            });
        }
        int ok = records.size() - riga.errori;
        aggiornaRiga(riga, riga.errori > 0 ? STATO_ERRORE : STATO_FATTO, ok, false);
        if (riga.errori > 0) {
            uiHandler.post(() -> riga.tvCount.setText(ok + " ✓  " + riga.errori + " ✗"));
        }
        incrementaTabelleCompletate();
    }

    private void applicaEliminazioni(MercuryApiService.DeletedResponse resp) {
        if (resp.deleted == null) return;
        DbInterno db = new DbInterno(this);
        for (MercuryApiService.DeletedResponse.DeletedItem item : resp.deleted) {
            try {
                db.delete("DELETE FROM " + item.tabella
                        + " WHERE " + getPkCondition(item.tabella, item.chiaveRecord), (String[]) null);
            } catch (Exception e) {
                Log.w(TAG, "Errore eliminazione " + item.tabella + " #" + item.chiaveRecord, e);
            }
        }
        db.close();
    }

    // ── Costruzione UI ────────────────────────────────────────────────────────

    private void costruisciRighe() {
        LayoutInflater inflater = LayoutInflater.from(this);

        if (MODE_UPLOAD.equals(syncMode)) {
            aggiungiIntestazione("↑  Dati da inviare a Mercury", Color.parseColor("#BF360C"));
            for (String[] entry : TABELLE_UPLOAD) {
                RigaTabella r = creaRiga(inflater, entry[0], entry[1]);
                r.isUpload = true;
                righeUpload.put(entry[0], r);
            }
        } else {
            aggiungiIntestazione("↓  Dati da scaricare da Mercury", Color.parseColor("#1565C0"));
            for (String[] entry : TABELLE) {
                RigaTabella r = creaRiga(inflater, entry[0], entry[1]);
                righe.put(entry[0], r);
            }
        }
    }

    private RigaTabella creaRiga(LayoutInflater inflater, String nomeTecnico, String nomeUmano) {
        View rigaView = inflater.inflate(R.layout.item_sync_table, containerTabelle, false);
        ((TextView) rigaView.findViewById(R.id.textNome)).setText(nomeUmano);

        RigaTabella r   = new RigaTabella();
        r.nomeTecnico   = nomeTecnico;
        r.nomeUmano     = nomeUmano;
        r.tvCount       = rigaView.findViewById(R.id.textCount);
        r.tvStato       = rigaView.findViewById(R.id.textStato);
        r.progressBarra = rigaView.findViewById(R.id.progressBarra);
        containerTabelle.addView(rigaView);
        return r;
    }

    private void aggiungiIntestazione(String titolo, int colore) {
        int dp8  = (int)(8  * getResources().getDisplayMetrics().density);
        int dp12 = (int)(12 * getResources().getDisplayMetrics().density);
        int dp6  = (int)(6  * getResources().getDisplayMetrics().density);

        TextView tv = new TextView(this);
        tv.setText(titolo);
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundColor(colore);
        tv.setPadding(dp12, dp6, dp12, dp6);
        tv.setTextSize(12f);
        tv.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp8, 0, 0);
        tv.setLayoutParams(lp);
        containerTabelle.addView(tv);
    }

    // ── Aggiornamento UI ──────────────────────────────────────────────────────

    private void aggiornaRiga(RigaTabella riga, int stato, int applicati, boolean isUpload) {
        if (riga == null) return;
        int coloreBarra = isUpload ? COLOR_UPLOAD : Color.parseColor("#1565C0");
        uiHandler.post(() -> {
            switch (stato) {
                case STATO_DA_INVIARE:
                    riga.tvStato.setText("↑");
                    riga.tvStato.setTextColor(COLOR_UPLOAD);
                    riga.tvCount.setText(riga.total + " da inviare");
                    riga.progressBarra.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(COLOR_UPLOAD));
                    riga.progressBarra.setMax(Math.max(riga.total, 1));
                    riga.progressBarra.setProgress(0);
                    break;
                case STATO_PENDING:
                    riga.tvStato.setText("↓");
                    riga.tvStato.setTextColor(COLOR_APPLYING);
                    riga.tvCount.setText("0 / " + riga.total);
                    riga.progressBarra.setMax(Math.max(riga.total, 1));
                    riga.progressBarra.setProgress(0);
                    break;
                case STATO_IN_CORSO:
                    riga.tvStato.setText(isUpload ? "↑" : "↓");
                    riga.tvStato.setTextColor(isUpload ? COLOR_UPLOAD : COLOR_APPLYING);
                    riga.progressBarra.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(coloreBarra));
                    break;
                case STATO_FATTO:
                    riga.tvStato.setText("✓");
                    riga.tvStato.setTextColor(COLOR_DONE);
                    riga.tvCount.setText(String.valueOf(riga.total));
                    riga.progressBarra.setMax(Math.max(riga.total, 1));
                    riga.progressBarra.setProgress(riga.total);
                    riga.progressBarra.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(COLOR_DONE));
                    break;
                case STATO_SALTO:
                    riga.tvStato.setText("✓");
                    riga.tvStato.setTextColor(COLOR_SKIP);
                    riga.tvCount.setText("0");
                    riga.progressBarra.setMax(1);
                    riga.progressBarra.setProgress(1);
                    riga.progressBarra.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(COLOR_SKIP));
                    break;
                case STATO_ERRORE:
                    riga.tvStato.setText("✗");
                    riga.tvStato.setTextColor(COLOR_ERROR);
                    if (riga.errorMsg != null && !riga.errorMsg.isEmpty()) {
                        final String dettaglio = riga.errorMsg;
                        final String etichetta = riga.nomeUmano != null ? riga.nomeUmano : riga.nomeTecnico;
                        riga.tvStato.setOnClickListener(v ->
                            new android.app.AlertDialog.Builder(SincronizzazioneActivity.this)
                                .setTitle("Errore — " + etichetta)
                                .setMessage(dettaglio)
                                .setPositiveButton("Chiudi", null)
                                .show());
                    }
                    break;
            }
        });
    }

    private void resetUI() {
        tabelleCompletate = 0;
        uiHandler.post(() -> {
            progressGlobale.setProgress(0);
            textTabelleProgress.setText("");
            textSyncStatus.setText("Preparazione...");
            textSyncStatus.setTextColor(Color.parseColor("#333333"));
        });
        for (RigaTabella r : righe.values())       resetRiga(r);
        for (RigaTabella r : righeUpload.values()) resetRiga(r);
    }

    private void resetRiga(RigaTabella r) {
        r.total    = 0;
        r.applicati = 0;
        r.errori   = 0;
        r.errorMsg = null;
        uiHandler.post(() -> {
            r.tvStato.setOnClickListener(null);
            r.tvStato.setText("●");
            r.tvStato.setTextColor(COLOR_PENDING);
            r.tvCount.setText("—");
            r.progressBarra.setProgress(0);
            r.progressBarra.setProgressTintList(
                android.content.res.ColorStateList.valueOf(
                    r.isUpload ? COLOR_UPLOAD : Color.parseColor("#1565C0")));
        });
    }

    private void incrementaTabelleCompletate() {
        tabelleCompletate++;
        final int completate = tabelleCompletate;
        final int totale     = MODE_UPLOAD.equals(syncMode) ? TABELLE_UPLOAD.length : TABELLE.length;
        uiHandler.post(() -> {
            progressGlobale.setProgress((int)(completate * 100.0 / totale));
            String attuale = textTabelleProgress.getText().toString();
            if (!attuale.startsWith(completate + " / ")) {
                int punto   = attuale.indexOf("·");
                String suff = punto >= 0 ? "  " + attuale.substring(punto) : "";
                textTabelleProgress.setText(completate + " / " + totale + " tabelle" + suff);
            }
        });
    }

    private void setStatus(String msg, boolean isError) {
        uiHandler.post(() -> {
            textSyncStatus.setText(msg);
            textSyncStatus.setTextColor(isError ? COLOR_ERROR : Color.parseColor("#333333"));
        });
    }

    // ── Helpers generici ──────────────────────────────────────────────────────

    private android.content.ContentValues jsonToContentValues(JsonObject record) {
        android.content.ContentValues cv = new android.content.ContentValues();
        for (Map.Entry<String, JsonElement> entry : record.entrySet()) {
            String key = entry.getKey();
            JsonElement val = entry.getValue();
            if (val == null || val.isJsonNull()) {
                cv.putNull(key);
            } else if (val.isJsonPrimitive()) {
                JsonPrimitive p = val.getAsJsonPrimitive();
                if (p.isBoolean()) {
                    cv.put(key, p.getAsBoolean() ? 1 : 0);
                } else if (p.isNumber()) {
                    String s = p.getAsString();
                    if (s.contains(".")) {
                        cv.put(key, p.getAsDouble());
                    } else {
                        try { cv.put(key, p.getAsLong()); }
                        catch (NumberFormatException e) { cv.put(key, s); }
                    }
                } else {
                    cv.put(key, p.getAsString());
                }
            }
        }
        return cv;
    }

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

    private boolean hasColumn(SQLiteDatabase db, String table, String column) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (c.moveToNext()) {
                if (column.equals(c.getString(1))) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void rilevaTabelleConInServer(SQLiteDatabase db) {
        tabelleConInServer.clear();
        for (String[] entry : TABELLE) {
            if (hasColumn(db, entry[0], "in_server")) tabelleConInServer.add(entry[0]);
        }
    }

    private String getPkCondition(String tabella, String chiaveRecord) {
        Map<String, String> pk = new HashMap<>();
        pk.put("cantieri",             "id_cantiere = "             + chiaveRecord);
        pk.put("preventivi",           "id_preventivo = "           + chiaveRecord);
        pk.put("rapportini",           "id_rapportino = "           + chiaveRecord);
        pk.put("elementi_cantiere",    "id_elemento_cant = "        + chiaveRecord);
        pk.put("preventivi_dettaglio", "id_preventivo_dettaglio = " + chiaveRecord);
        pk.put("rapportini_dettaglio", "id_rapportino_dettaglio = " + chiaveRecord);
        pk.put("anagrafica",           "id_anagrafica = "           + chiaveRecord);
        pk.put("aree",                 "id_area = "                 + chiaveRecord);
        pk.put("componenti",           "id_componente = "           + chiaveRecord);
        pk.put("componenti_cantiere",  "id_componente_cant = "      + chiaveRecord);
        pk.put("costruttori",          "id_costruttore = "          + chiaveRecord);
        pk.put("elementi",             "id_elemento = "             + chiaveRecord);
        pk.put("foto",                 "id_foto = "                 + chiaveRecord);
        pk.put("foto_elementi",        "id_foto_elemento = "        + chiaveRecord);
        pk.put("linee",                "id_linea = "                + chiaveRecord);
        pk.put("listini",              "id_listino = "              + chiaveRecord);
        pk.put("locali",               "id_locale = "               + chiaveRecord);
        pk.put("manodopera",           "id_manodopera = "           + chiaveRecord);
        pk.put("placche",              "id_placca = "               + chiaveRecord);
        pk.put("placche_moduli",       "id_placca_modulo = "        + chiaveRecord);
        pk.put("unita",                "id_unita = "                + chiaveRecord);
        return pk.getOrDefault(tabella, "id = " + chiaveRecord);
    }

    // ── Timestamp ─────────────────────────────────────────────────────────────

    private String getLastSyncTimestamp() {
        SharedPreferences prefs  = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String            stored = prefs.getString(KEY_SYNC, null);
        if (stored == null || stored.length() < 8) return "1970-01-01T00:00:00";
        if (stored.length() == 14 && !stored.contains("T")) {
            return stored.substring(0, 4) + "-" + stored.substring(4, 6) + "-"
                 + stored.substring(6, 8) + "T" + stored.substring(8, 10) + ":"
                 + stored.substring(10, 12) + ":" + stored.substring(12, 14);
        }
        return stored;
    }

    private void salvaUltimaSync(String isoTimestamp) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_SYNC, isoTimestamp).apply();
    }

    private void aggiornaEtichettaUltimaSync() {
        String ts    = getLastSyncTimestamp();
        String label = "1970-01-01T00:00:00".equals(ts)
                ? "Nessuna sincronizzazione precedente"
                : "Ultima sync: " + ts;
        textLastSync.setText(label);
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    private static class RigaTabella {
        String      nomeTecnico;
        String      nomeUmano;
        int         total    = 0;
        int         applicati = 0;
        int         errori    = 0;
        boolean     isUpload  = false;
        String      errorMsg  = null;
        TextView    tvCount;
        TextView    tvStato;
        ProgressBar progressBarra;
    }
}
