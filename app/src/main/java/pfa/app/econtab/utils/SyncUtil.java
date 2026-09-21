package pfa.app.econtab.utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilità condivise dai due percorsi di sincronizzazione (SincronizzazioneActivity e SyncWorker).
 */
public final class SyncUtil {

    /**
     * Tabelle di configurazione gestite dal server che il download invia SEMPRE per intero (sono poche righe):
     * l'app le allinea sostituendo il contenuto locale, cosi' rinomine, cancellazioni fatte da Sonata e valori
     * rimasti da vecchie versioni dell'app non restano in giro.
     */
    private static final String[] TABELLE_ELENCO_COMPLETO = { "unita_misura" };

    /** Tabelle locali con una colonna unita' di misura (chiave esterna verso unita_misura sul server). */
    private static final String[] TABELLE_CON_UNITA_MISURA = { "elementi", "componenti", "elementi_cantiere",
            "componenti_cantiere", "preventivi_dettaglio", "manodopera", "rapportini_dettaglio" };

    /** Valori liberi usati in passato dall'app -> codice attuale (stessa mappatura della migrazione server). */
    private static final Map<String, String> SINONIMI_UNITA_MISURA = new HashMap<String, String>();
    static {
        String[][] s = { { "PZ", "pz,pezzo,pezzi,oggetto,um,nr,n" }, { "CM", "cm" }, { "MT", "m,mt,metro,metri,ml" },
                { "MQ", "mq" }, { "OR", "h,hh,or,ora,ore" } };
        for (String[] riga : s) {
            for (String chiave : riga[1].split(",")) SINONIMI_UNITA_MISURA.put(chiave, riga[0]);
        }
    }

    /**
     * Le unita' di misura possono essere SOLO quelle scaricate dal server (tabella unita_misura). Prima di ogni
     * upload riporta i valori locali non validi (es. la "H" fissa delle vecchie righe di manodopera) al codice
     * corrispondente, oppure a vuoto se non ce n'e' uno: il server rifiuta i codici che non conosce.
     * Se l'elenco non e' ancora stato scaricato non tocca nulla.
     *
     * @return numero di valori corretti
     */
    public static int allineaUnitaMisura(SQLiteDatabase db) {
        Set<String> validi = new HashSet<String>();
        try (Cursor c = db.rawQuery("SELECT unita_misura FROM unita_misura", null)) {
            while (c.moveToNext()) validi.add(c.getString(0));
        }
        if (validi.isEmpty()) return 0;

        int corretti = 0;
        for (String tabella : TABELLE_CON_UNITA_MISURA) {
            Set<String> daCorreggere = new HashSet<String>();
            try (Cursor c = db.rawQuery("SELECT DISTINCT unita_misura FROM " + tabella
                    + " WHERE unita_misura IS NOT NULL AND unita_misura <> ''", null)) {
                while (c.moveToNext()) {
                    if (!validi.contains(c.getString(0))) daCorreggere.add(c.getString(0));
                }
            } catch (Exception e) {
                continue; // tabella senza la colonna (schema non aggiornato)
            }
            for (String valore : daCorreggere) {
                String codice = SINONIMI_UNITA_MISURA.get(valore.trim().toLowerCase(java.util.Locale.ROOT));
                if (codice == null && validi.contains(valore.trim().toUpperCase(java.util.Locale.ROOT))) {
                    codice = valore.trim().toUpperCase(java.util.Locale.ROOT);
                }
                if (codice == null || !validi.contains(codice)) codice = "";
                corretti += db.compileStatement("UPDATE " + tabella + " SET unita_misura = '" + codice.replace("'", "''")
                        + "' WHERE unita_misura = '" + valore.replace("'", "''") + "'").executeUpdateDelete();
            }
        }
        return corretti;
    }

    /** Tabella locale degli operatori (utenti della ditta), alimentata dal download: id_utente = id utente Mercury. */
    public static final String TABELLA_OPERATORI = "utenti";

    /**
     * Allinea gli operatori locali all'elenco della ditta inviato dal server.
     * Non usa REPLACE: i vecchi utenti locali (login legacy) conservano la loro password; gli operatori nuovi
     * arrivano con password vuota, quindi non compaiono mai nell'"Accesso locale". Gli operatori sparsi dalla
     * ditta (senza password, non piu' nell'elenco) vengono rimossi.
     *
     * @return numero di operatori applicati
     */
    public static int applicaOperatori(SQLiteDatabase db, List<com.google.gson.JsonObject> records) {
        if (records == null || records.isEmpty()) return 0;
        StringBuilder ids = new StringBuilder();
        int n = 0;
        for (com.google.gson.JsonObject r : records) {
            if (!r.has("id_utente") || r.get("id_utente").isJsonNull()) continue;
            long id = r.get("id_utente").getAsLong();
            String nome = r.has("nome") && !r.get("nome").isJsonNull() ? r.get("nome").getAsString() : "";
            String cognome = r.has("cognome") && !r.get("cognome").isJsonNull() ? r.get("cognome").getAsString() : "";

            ContentValues aggiorna = new ContentValues();
            aggiorna.put("nome", nome);
            aggiorna.put("cognome", cognome);
            int toccate = db.update(TABELLA_OPERATORI, aggiorna, "id_utente = ?", new String[] { String.valueOf(id) });
            if (toccate == 0) {
                ContentValues nuovo = new ContentValues(aggiorna);
                nuovo.put("id_utente", id);
                nuovo.put("livello", 0);
                nuovo.put("password", "");
                nuovo.put("ricorda_password", 0);
                nuovo.put("in_server", 1);
                db.insert(TABELLA_OPERATORI, null, nuovo);
            }
            if (ids.length() > 0) ids.append(',');
            ids.append(id);
            n++;
        }
        if (n > 0) {
            db.execSQL("DELETE FROM " + TABELLA_OPERATORI + " WHERE (password IS NULL OR password = '') AND id_utente > 0"
                    + " AND id_utente NOT IN (" + ids + ")");
        }
        return n;
    }

    /** Svuota la tabella prima di applicare un elenco completo. Con un elenco vuoto non tocca nulla (errore/assenza dati). */
    public static void svuotaSeElencoCompleto(SQLiteDatabase db, String nomeTabella, List<?> records) {
        if (records == null || records.isEmpty()) return;
        for (String t : TABELLE_ELENCO_COMPLETO) {
            if (t.equals(nomeTabella)) {
                db.delete(nomeTabella, null, null);
                return;
            }
        }
    }


    private static final Map<String, Set<String>> COLONNE_PER_TABELLA = new HashMap<>();

    private SyncUtil() {}

    /** Colonne data dell'app: numeri yyyyMMddHHmmss (14 cifre), non testo. */
    private static final String[] COLONNE_DATA = {"data", "data_ins", "data_mod", "data_rapportino"};
    private static final Pattern DATA_TESTO = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})[ T](\\d{2}):(\\d{2}):(\\d{2}).*");

    /**
     * Converte nelle colonne data di {@code cv} il testo "yyyy-MM-dd HH:mm:ss" nel numero yyyyMMddHHmmss
     * che l'app si aspetta (Utility.numberToData e getAsLong falliscono sul testo). Rete di sicurezza per i
     * dati scaricati: il server invia gia' il formato numerico.
     */
    public static void normalizzaDate(ContentValues cv) {
        for (String col : COLONNE_DATA) {
            Object v = cv.get(col);
            if (v instanceof String) {
                Matcher m = DATA_TESTO.matcher((String) v);
                if (m.matches()) {
                    cv.put(col, Long.parseLong(m.group(1) + m.group(2) + m.group(3) + m.group(4) + m.group(5) + m.group(6)));
                }
            }
        }
    }

    /**
     * L'app non gestisce i NULL: legge i campi come valori non nulli (es. icona null → crash nella griglia
     * dei componenti, id_locale null → crash nel dettaglio preventivo). Nei record scaricati sostituisce i
     * NULL con '' (colonne testo) o 0 (colonne numeriche). Le colonne data restano NULL: l'app le usa per
     * "mai modificato".
     */
    public static void normalizzaNulli(SQLiteDatabase db, String tabella, ContentValues cv) {
        Map<String, String> tipi = getTipiColonne(db, tabella);
        for (String chiave : new HashSet<>(cv.keySet())) {
            if (cv.get(chiave) != null || isColonnaData(chiave)) {
                continue;
            }
            String tipo = tipi.get(chiave);
            if (tipo == null) {
                continue; // colonna sconosciuta: verra' scartata da rimuoviColonneSconosciute
            }
            if (tipo.startsWith("TEXT") || tipo.startsWith("VARCHAR") || tipo.startsWith("CHAR")) {
                cv.put(chiave, "");
            } else {
                cv.put(chiave, 0);
            }
        }
    }

    /**
     * Ripara nel database locale i NULL rimasti da download precedenti (colonne testo → '', numeriche → 0,
     * escluse le date e le chiavi primarie). Idempotente.
     */
    public static void normalizzaNulliLocali(SQLiteDatabase db) {
        List<String> tabelle = new java.util.ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'", null)) {
            while (c.moveToNext()) {
                tabelle.add(c.getString(0));
            }
        }
        for (String tabella : tabelle) {
            try (Cursor c = db.rawQuery("PRAGMA table_info(" + tabella + ")", null)) {
                List<String> sql = new java.util.ArrayList<>();
                while (c.moveToNext()) {
                    String nome = c.getString(c.getColumnIndexOrThrow("name"));
                    String tipo = c.getString(c.getColumnIndexOrThrow("type"));
                    int pk = c.getInt(c.getColumnIndexOrThrow("pk"));
                    if (pk != 0 || tipo == null || isColonnaData(nome)) {
                        continue;
                    }
                    tipo = tipo.toUpperCase();
                    String valore = (tipo.startsWith("TEXT") || tipo.startsWith("VARCHAR") || tipo.startsWith("CHAR")) ? "''" : "0";
                    sql.add("UPDATE " + tabella + " SET " + nome + " = " + valore + " WHERE " + nome + " IS NULL");
                }
                c.close();
                for (String q : sql) {
                    db.execSQL(q);
                }
            }
        }
    }

    private static boolean isColonnaData(String nome) {
        for (String d : COLONNE_DATA) {
            if (d.equals(nome)) {
                return true;
            }
        }
        return false;
    }

    private static final Map<String, Map<String, String>> TIPI_PER_TABELLA = new HashMap<>();

    private static synchronized Map<String, String> getTipiColonne(SQLiteDatabase db, String tabella) {
        Map<String, String> tipi = TIPI_PER_TABELLA.get(tabella);
        if (tipi != null) {
            return tipi;
        }
        tipi = new HashMap<>();
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + tabella + ")", null)) {
            while (c.moveToNext()) {
                String tipo = c.getString(c.getColumnIndexOrThrow("type"));
                tipi.put(c.getString(c.getColumnIndexOrThrow("name")), tipo == null ? "" : tipo.toUpperCase());
            }
        }
        if (!tipi.isEmpty()) {
            TIPI_PER_TABELLA.put(tabella, tipi);
        }
        return tipi;
    }

    /**
     * Ripara le date gia' salvate come testo nel database locale (scaricate quando il server usava il formato
     * "yyyy-MM-dd HH:mm:ss"). Idempotente: tocca solo i valori testuali in quel formato.
     *
     * @return numero di tabelle in cui e' stata riparata almeno una colonna
     */
    public static int normalizzaDateLocali(SQLiteDatabase db) {
        int riparate = 0;
        List<String> tabelle = new java.util.ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'", null)) {
            while (c.moveToNext()) {
                tabelle.add(c.getString(0));
            }
        }
        for (String tabella : tabelle) {
            Set<String> colonne = new HashSet<>();
            try (Cursor c = db.rawQuery("PRAGMA table_info(" + tabella + ")", null)) {
                while (c.moveToNext()) {
                    colonne.add(c.getString(c.getColumnIndexOrThrow("name")));
                }
            }
            boolean toccata = false;
            for (String col : COLONNE_DATA) {
                if (!colonne.contains(col)) {
                    continue;
                }
                db.execSQL("UPDATE " + tabella + " SET " + col + " = CAST(strftime('%Y%m%d%H%M%S', " + col + ") AS INTEGER)"
                        + " WHERE typeof(" + col + ") = 'text' AND strftime('%Y%m%d%H%M%S', " + col + ") IS NOT NULL");
                Cursor ch = db.rawQuery("SELECT changes()", null);
                try {
                    if (ch.moveToFirst() && ch.getInt(0) > 0) {
                        toccata = true;
                    }
                } finally {
                    ch.close();
                }
            }
            if (toccata) {
                riparate++;
            }
        }
        return riparate;
    }

    // Formati degli errori restituiti dal server nell'array "errors" della risposta di upload
    private static final Pattern[] ERR_PER_RECORD = {
            Pattern.compile("^(\\w+) insert error \\(localId=([^)]*)\\)"),
            Pattern.compile("^(\\w+) update error \\(id=([^)]*)\\)"),
            Pattern.compile("^(\\w+) #(-?\\d+):"),
            Pattern.compile("^(\\w+): record (-?\\d+) non trovato"),
            Pattern.compile("^(\\w+): id non valido per update: (-?\\d+)"),
    };
    private static final Pattern ERR_TABELLA = Pattern.compile("^Tabella sconosciuta: (\\w+)");
    private static final Pattern ID_NUMERICO = Pattern.compile("-?\\d+");

    /** Esito di un upload: quali record il server ha rifiutato, per tabella. */
    public static final class EsitoUpload {
        /** tabella → id (come stringa) dei record rifiutati */
        public final Map<String, Set<String>> falliti = new HashMap<>();
        /** tabella → primo messaggio di errore, da mostrare all'utente */
        public final Map<String, String> primoErrore = new HashMap<>();
        /** true se almeno un errore non e' riconducibile a una tabella: nel dubbio non si marca nulla */
        public boolean nonAttribuibile = false;
    }

    /** Interpreta l'array "errors" del server (null o vuoto = tutto accettato). */
    public static EsitoUpload analizzaErrori(List<String> errori) {
        EsitoUpload esito = new EsitoUpload();
        if (errori == null) {
            return esito;
        }
        for (String err : errori) {
            String tabella = null;
            String id = "*";
            Matcher m = ERR_TABELLA.matcher(err);
            if (m.find()) {
                tabella = m.group(1);
            } else {
                for (Pattern p : ERR_PER_RECORD) {
                    m = p.matcher(err);
                    if (m.find()) {
                        tabella = m.group(1);
                        id = m.group(2);
                        break;
                    }
                }
            }
            if (tabella == null) {
                esito.nonAttribuibile = true;
                continue;
            }
            Set<String> ids = esito.falliti.get(tabella);
            if (ids == null) {
                ids = new HashSet<>();
                esito.falliti.put(tabella, ids);
            }
            ids.add(id);
            if (!esito.primoErrore.containsKey(tabella)) {
                esito.primoErrore.put(tabella, err);
            }
        }
        return esito;
    }

    /**
     * Segna in_server=1 i record della tabella che il server ha accettato, lasciando a 0 quelli rifiutati
     * (verranno reinviati alla sync successiva). Per le tabelle senza PK numerica singola (chiavi composte
     * o stringa) basta un errore per non segnare nessun record: il server le tratta come upsert, quindi
     * reinviarle e' sicuro. Se un errore non e' attribuibile a una tabella non si segna nulla.
     *
     * @return numero di record marcati
     */
    public static int marcaInviati(SQLiteDatabase db, String tabella, String pkCol, EsitoUpload esito) {
        if (esito.nonAttribuibile) {
            return 0;
        }
        String where = "in_server = 0";
        List<String> args = new java.util.ArrayList<>();
        Set<String> falliti = esito.falliti.get(tabella);
        if (falliti != null) {
            if (pkCol == null) {
                return 0;
            }
            for (String id : falliti) {
                if (!ID_NUMERICO.matcher(id).matches()) {
                    return 0; // id non numerico o tabella intera: non so quali record siano stati rifiutati
                }
                where += " AND " + pkCol + " <> ?";
                args.add(id);
            }
        }
        ContentValues cv = new ContentValues();
        cv.put("in_server", 1);
        return db.update(tabella, cv, where, args.isEmpty() ? null : args.toArray(new String[0]));
    }

    /**
     * Toglie da {@code cv} le colonne che la tabella locale non ha (es. {@code stato} sulle tabelle
     * catalogo, che esiste solo su Mercury). Senza questo filtro l'insert fallisce con
     * "table X has no column named Y" e, nel SyncWorker, annulla l'intera transazione di download.
     */
    public static void rimuoviColonneSconosciute(SQLiteDatabase db, String tabella, ContentValues cv) {
        Set<String> colonne = getColonne(db, tabella);
        if (colonne.isEmpty()) {
            return; // tabella non leggibile: lascio a insert() il compito di segnalare l'errore vero
        }
        for (String chiave : new HashSet<>(cv.keySet())) {
            if (!colonne.contains(chiave)) {
                cv.remove(chiave);
            }
        }
    }

    private static synchronized Set<String> getColonne(SQLiteDatabase db, String tabella) {
        Set<String> colonne = COLONNE_PER_TABELLA.get(tabella);
        if (colonne != null) {
            return colonne;
        }
        colonne = new HashSet<>();
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + tabella + ")", null)) {
            while (c.moveToNext()) {
                colonne.add(c.getString(c.getColumnIndexOrThrow("name")));
            }
        }
        if (!colonne.isEmpty()) {
            COLONNE_PER_TABELLA.put(tabella, colonne);
        }
        return colonne;
    }
}
