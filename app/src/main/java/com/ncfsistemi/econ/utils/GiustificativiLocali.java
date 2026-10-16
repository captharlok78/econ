package com.ncfsistemi.econ.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ncfsistemi.econ.api.MercuryApiService;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Giustificativi;
import com.ncfsistemi.econ.db.table.GiustificativiTipologie;
import com.ncfsistemi.econ.servizi.GiustificativiWorker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

/**
 * Giustificativi della persona sul dispositivo (STUDIO_GIUSTIFICATIVI_APP.md): ferie, permessi, malattia… chiesti dal
 * calendario personale. Si salvano subito sul dispositivo e partono da soli verso il server appena c'e' rete
 * (inviaInBackground, WorkManager); arrivano anche con ogni sincronizzazione (allinea). Sul server nascono "in attesa":
 * li approva o rifiuta il coordinatore o l'amministratore dal portale, e l'esito torna con l'allineamento successivo.
 * Serve la funzionalita' CALENDARIO.GIUSTIFICATIVI.
 */
public final class GiustificativiLocali {

    public static final String FUNZIONALITA = "CALENDARIO.GIUSTIFICATIVI";
    /** Nome del lavoro di invio (WorkManager): CalendarioActivity lo osserva per ridisegnarsi alla fine. */
    public static final String LAVORO = "giustificativi-invio";
    /** Periodo piu' lungo chiedibile in una volta (giorni). */
    public static final int GIORNI_MAX = 62;

    private static final String TAG = "GiustificativiLocali";
    private static final String PREFS = "GIUSTIFICATIVI";
    private static final String K_ORE_GIORNO = "ore_giorno";
    private static final String K_AVVISI = "avvisi";
    private static final String K_ULTIMO = "ultimo";
    /** Nuovo rifiutato dal server (es. doppione): resta sul dispositivo con l'errore, non si rimanda finche' non si cambia. */
    private static final int NON_ACCETTATO = 2;

    /** Un allineamento alla volta (sync, lavoro in background). */
    private static final Object LOCK = new Object();

    private GiustificativiLocali() {
    }

    public static boolean attivo(Context ctx) {
        return FunzionalitaApp.ha(ctx, FUNZIONALITA);
    }

    // ── dati ─────────────────────────────────────────────────────────────────

    public static final class Tipologia {
        public int id;
        public String codice = "";
        public String descrizione = "";
        public String icona = "";
        public String colore = "";
        public boolean contabile;

        @Override
        public String toString() {
            return descrizione;
        }
    }

    public static final class Voce {
        public int id;
        public long giorno;
        public int idTipologia;
        public double ore;
        public String note = "";
        public String stato = "";
        public String gruppo = "";
        public String motivoRifiuto = "";
        public String approvatoDa = "";
        public String errore = "";
        public boolean daInviare;
        public boolean nonAccettato;
        public Tipologia tipologia;

        public boolean isRichiesto() {
            return Giustificativi.STATO_RICHIESTO.equals(stato);
        }

        public boolean isApprovato() {
            return Giustificativi.STATO_APPROVATO.equals(stato);
        }

        public boolean isRifiutato() {
            return Giustificativi.STATO_RIFIUTATO.equals(stato);
        }

        /** Si cambia o si toglie dal dispositivo: non ancora inviato, non accettato o in attesa (non i decisi). */
        public boolean modificabile() {
            return id < 0 || isRichiesto();
        }

        /** Si toglie: anche il rifiutato (sparisce dalla vista), non l'approvato. */
        public boolean eliminabile() {
            return !isApprovato();
        }

        public String etichettaStato() {
            if (nonAccettato) return "Non accettato";
            if (id < 0 || daInviare) return "Da inviare";
            if (isApprovato()) return "Approvato";
            if (isRifiutato()) return "Rifiutato";
            return "In attesa";
        }

        /** Grigio da inviare, rosso rifiutato/non accettato, verde approvato, giallo in attesa. */
        public int coloreStato() {
            if (nonAccettato || isRifiutato()) return 0xFFC62828;
            if (id < 0 || daInviare) return 0xFF757575;
            if (isApprovato()) return 0xFF2E7D32;
            return 0xFFF9A825;
        }
    }

    /** Tipologie attive della ditta, nell'ordine del portale. */
    public static List<Tipologia> tipologie(Context ctx) {
        List<Tipologia> ris = new ArrayList<>();
        DbInterno dbi = new DbInterno(ctx);
        try (Cursor c = dbi.getReadableDatabase().rawQuery("select * from " + GiustificativiTipologie.NOME_TABELLA
                + " order by coalesce(" + GiustificativiTipologie.ORDINE + ", 0), " + GiustificativiTipologie.DESCRIZIONE, null)) {
            while (c.moveToNext()) ris.add(tipologia(c));
        } catch (Exception e) {
            Log.w(TAG, "Tipologie non lette: " + e.getMessage());
        } finally {
            dbi.close();
        }
        return ris;
    }

    /** Giustificativi da dal ad al (yyyyMMdd) per giorno, senza quelli tolti in attesa di conferma dal server. */
    public static Map<Long, List<Voce>> periodo(Context ctx, long dal, long al) {
        Map<Long, List<Voce>> ris = new LinkedHashMap<>();
        Map<Integer, Tipologia> tipi = new HashMap<>();
        for (Tipologia t : tipologie(ctx)) tipi.put(t.id, t);
        DbInterno dbi = new DbInterno(ctx);
        try (Cursor c = dbi.getReadableDatabase().rawQuery("select * from " + Giustificativi.NOME_TABELLA
                + " where " + Giustificativi.DATA + " between ? and ? and coalesce(" + Giustificativi.ELIMINATO + ", 0) = 0"
                + " order by " + Giustificativi.DATA + ", " + Giustificativi.ID, new String[]{data(dal), data(al)})) {
            while (c.moveToNext()) {
                Voce v = voce(c);
                v.tipologia = tipi.get(v.idTipologia);
                if (v.tipologia == null) {
                    v.tipologia = new Tipologia();
                    v.tipologia.id = v.idTipologia;
                    v.tipologia.descrizione = "Giustificativo";
                }
                List<Voce> l = ris.get(v.giorno);
                if (l == null) ris.put(v.giorno, l = new ArrayList<>());
                l.add(v);
            }
        } catch (Exception e) {
            Log.w(TAG, "Giustificativi non letti: " + e.getMessage());
        } finally {
            dbi.close();
        }
        return ris;
    }

    /** Ore di lavoro della ditta nel giorno della settimana (dall'ultimo allineamento); 8 se non ancora note. */
    public static double oreGiorno(Context ctx, long giorno) {
        String json = prefs(ctx).getString(K_ORE_GIORNO, null);
        int n = (CalendarioPersonale.calendario(giorno).get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1; // 1 = lunedi'
        if (json == null) return n <= 5 ? 8 : 0;
        try {
            return new org.json.JSONObject(json).optDouble(String.valueOf(n), 0);
        } catch (Exception e) {
            return 8;
        }
    }

    // ── modifiche dal calendario ─────────────────────────────────────────────

    /**
     * Nuova richiesta dal al (yyyyMMdd). Per un giorno solo si crea sempre; per un periodo un giorno per ogni giorno con
     * orario della ditta, con le ore dell'orario (ore null) o quelle indicate. Salta i giorni che hanno gia' un
     * giustificativo di quel tipo non rifiutato. Lancia IllegalArgumentException con il motivo se non crea nulla.
     *
     * @return giorni creati
     */
    public static int richiedi(Context ctx, int idTipologia, long dal, long al, Double ore, String note) {
        if (idTipologia <= 0) throw new IllegalArgumentException("Scegli la tipologia.");
        if (al < dal) throw new IllegalArgumentException("Il giorno di fine è prima di quello di inizio.");
        if (ore != null && (ore <= 0 || ore > 24)) throw new IllegalArgumentException("Le ore vanno da 0,25 a 24.");
        List<Long> giorni = new ArrayList<>();
        for (long g = dal; g <= al; g = CalendarioPersonale.piuGiorni(g, 1)) {
            giorni.add(g);
            if (giorni.size() > GIORNI_MAX) throw new IllegalArgumentException("Al massimo " + GIORNI_MAX + " giorni alla volta.");
        }
        boolean periodo = giorni.size() > 1;
        String gruppo = periodo ? UUID.randomUUID().toString() : null;
        int creati = 0, gia = 0;
        DbInterno dbi = new DbInterno(ctx);
        SQLiteDatabase db = dbi.getWritableDatabase();
        try {
            db.beginTransaction();
            int id = Math.min(minimoId(db), 0);
            for (long g : giorni) {
                double oreDelGiorno = ore != null ? ore : oreGiorno(ctx, g);
                if (periodo && oreGiorno(ctx, g) <= 0) continue; // giorno senza orario (sabato, domenica…)
                if (oreDelGiorno <= 0) oreDelGiorno = 8;
                if (esiste(db, g, idTipologia)) {
                    gia++;
                    continue;
                }
                ContentValues cv = new ContentValues();
                cv.put(Giustificativi.ID, --id);
                cv.put(Giustificativi.DATA, data(g));
                cv.put(Giustificativi.ID_TIPOLOGIA, idTipologia);
                cv.put(Giustificativi.ORE, Math.min(24, oreDelGiorno));
                cv.put(Giustificativi.NOTE, note != null ? note.trim() : "");
                cv.put(Giustificativi.STATO, Giustificativi.STATO_RICHIESTO);
                cv.put(Giustificativi.ORIGINE, "APP");
                cv.put(Giustificativi.GRUPPO, gruppo);
                cv.put(Giustificativi.DA_INVIARE, 1);
                cv.put(Giustificativi.ELIMINATO, 0);
                db.insert(Giustificativi.NOME_TABELLA, null, cv);
                creati++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            dbi.close();
        }
        if (creati == 0) {
            throw new IllegalArgumentException(gia > 0 ? "C'è già un giustificativo di questo tipo in quei giorni."
                    : "Nessun giorno lavorativo nel periodo scelto.");
        }
        inviaInBackground(ctx);
        return creati;
    }

    /** Cambia tipologia, ore e note di un giustificativo non ancora deciso; riparte l'invio. */
    public static void modifica(Context ctx, int id, int idTipologia, double ore, String note) {
        if (ore <= 0 || ore > 24) throw new IllegalArgumentException("Le ore vanno da 0,25 a 24.");
        ContentValues cv = new ContentValues();
        cv.put(Giustificativi.ID_TIPOLOGIA, idTipologia);
        cv.put(Giustificativi.ORE, ore);
        cv.put(Giustificativi.NOTE, note != null ? note.trim() : "");
        cv.put(Giustificativi.DA_INVIARE, 1);
        cv.putNull(Giustificativi.ERRORE);
        aggiorna(ctx, id, cv);
        inviaInBackground(ctx);
    }

    /** Toglie un giustificativo: subito se non era ancora sul server, altrimenti alla conferma del server. */
    public static void elimina(Context ctx, int id) {
        DbInterno dbi = new DbInterno(ctx);
        try {
            SQLiteDatabase db = dbi.getWritableDatabase();
            if (id < 0) {
                db.delete(Giustificativi.NOME_TABELLA, Giustificativi.ID + "=?", new String[]{String.valueOf(id)});
                return;
            }
            ContentValues cv = new ContentValues();
            cv.put(Giustificativi.ELIMINATO, 1);
            db.update(Giustificativi.NOME_TABELLA, cv, Giustificativi.ID + "=?", new String[]{String.valueOf(id)});
        } finally {
            dbi.close();
        }
        if (id > 0) inviaInBackground(ctx);
    }

    // ── server ───────────────────────────────────────────────────────────────

    /** Invio appena c'e' rete, in background; si sostituisce a un invio gia' in coda. */
    public static void inviaInBackground(Context ctx) {
        if (!attivo(ctx)) return;
        OneTimeWorkRequest lavoro = new OneTimeWorkRequest.Builder(GiustificativiWorker.class)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(ctx.getApplicationContext()).enqueueUniqueWork(LAVORO, ExistingWorkPolicy.REPLACE, lavoro);
    }

    /**
     * Manda le modifiche del dispositivo e riprende dal server tipologie, giustificativi (con gli esiti) e orario della
     * ditta. Blocca sulla rete: da un thread di background. Gli avvisi (es. "gia' approvato") restano per il calendario.
     *
     * @return avvisi del server, vuoto se tutto bene
     */
    public static List<String> allinea(Context ctx, MercuryApiService api) throws Exception {
        if (!attivo(ctx)) return new ArrayList<>();
        synchronized (LOCK) {
            DbInterno dbi = new DbInterno(ctx);
            SQLiteDatabase db = dbi.getWritableDatabase();
            try {
                db.delete(Giustificativi.NOME_TABELLA, Giustificativi.ID + "<0 and coalesce(" + Giustificativi.ELIMINATO + ",0)=1", null);
                JsonArray nuovi = new JsonArray(), modificati = new JsonArray(), eliminati = new JsonArray();
                Map<Integer, String> dataLocale = new HashMap<>();
                try (Cursor c = db.rawQuery("select * from " + Giustificativi.NOME_TABELLA + " where coalesce(" + Giustificativi.DA_INVIARE
                        + ",0)=1 or coalesce(" + Giustificativi.ELIMINATO + ",0)=1", null)) {
                    while (c.moveToNext()) {
                        Voce v = voce(c);
                        boolean eliminato = c.getInt(c.getColumnIndexOrThrow(Giustificativi.ELIMINATO)) == 1;
                        if (eliminato) {
                            eliminati.add(v.id);
                            continue;
                        }
                        JsonObject r = new JsonObject();
                        r.addProperty("data", data(v.giorno));
                        r.addProperty("id_tipologia", v.idTipologia);
                        r.addProperty("ore", v.ore);
                        r.addProperty("note", v.note);
                        if (v.id < 0) {
                            r.addProperty("locale", v.id);
                            if (!v.gruppo.isEmpty()) r.addProperty("gruppo", v.gruppo);
                            nuovi.add(r);
                            dataLocale.put(v.id, giornoLeggibile(v.giorno));
                        } else {
                            r.addProperty("id", v.id);
                            modificati.add(r);
                        }
                    }
                }

                Response<JsonObject> resp;
                if (nuovi.size() == 0 && modificati.size() == 0 && eliminati.size() == 0) {
                    resp = api.getGiustificativi().execute();
                } else {
                    JsonObject richieste = new JsonObject();
                    richieste.add("nuovi", nuovi);
                    richieste.add("modificati", modificati);
                    richieste.add("eliminati", eliminati);
                    resp = api.inviaGiustificativi(richieste).execute();
                }
                if (!resp.isSuccessful() || resp.body() == null) throw new Exception("HTTP " + resp.code());
                JsonObject b = resp.body();
                List<String> avvisi = new ArrayList<>();

                db.beginTransaction();
                try {
                    for (JsonElement e : array(b, "nuovi")) {
                        JsonObject o = e.getAsJsonObject();
                        int locale = o.get("locale").getAsInt();
                        if (o.get("ok").getAsBoolean()) {
                            db.delete(Giustificativi.NOME_TABELLA, Giustificativi.ID + "=?", new String[]{String.valueOf(locale)});
                        } else {
                            String errore = testo(o, "errore");
                            ContentValues cv = new ContentValues();
                            cv.put(Giustificativi.DA_INVIARE, NON_ACCETTATO);
                            cv.put(Giustificativi.ERRORE, errore);
                            db.update(Giustificativi.NOME_TABELLA, cv, Giustificativi.ID + "=?", new String[]{String.valueOf(locale)});
                            avvisi.add(dataLocale.getOrDefault(locale, "") + ": " + errore);
                        }
                    }
                    // modifiche: accettate o no, vale la versione del server (che arriva sotto)
                    for (JsonElement e : array(b, "modificati")) {
                        JsonObject o = e.getAsJsonObject();
                        ContentValues cv = new ContentValues();
                        cv.put(Giustificativi.DA_INVIARE, 0);
                        db.update(Giustificativi.NOME_TABELLA, cv, Giustificativi.ID + "=?", new String[]{o.get("id").getAsString()});
                        if (!o.get("ok").getAsBoolean()) avvisi.add(testo(o, "errore"));
                    }
                    for (JsonElement e : array(b, "eliminati")) {
                        JsonObject o = e.getAsJsonObject();
                        if (o.get("ok").getAsBoolean()) {
                            db.delete(Giustificativi.NOME_TABELLA, Giustificativi.ID + "=?", new String[]{o.get("id").getAsString()});
                        } else {
                            ContentValues cv = new ContentValues();
                            cv.put(Giustificativi.ELIMINATO, 0);
                            db.update(Giustificativi.NOME_TABELLA, cv, Giustificativi.ID + "=?", new String[]{o.get("id").getAsString()});
                            avvisi.add(testo(o, "errore"));
                        }
                    }

                    // quelli del server sostituiscono i suoi sul dispositivo; restano i nuovi non accettati e le
                    // modifiche fatte nel frattempo
                    db.delete(Giustificativi.NOME_TABELLA, Giustificativi.ID + ">0 and coalesce(" + Giustificativi.DA_INVIARE + ",0)=0 and coalesce("
                            + Giustificativi.ELIMINATO + ",0)=0", null);
                    for (JsonElement e : array(b, "giustificativi")) {
                        JsonObject o = e.getAsJsonObject();
                        int id = o.get("id").getAsInt();
                        if (cambiatoNelFrattempo(db, id)) continue;
                        ContentValues cv = new ContentValues();
                        cv.put(Giustificativi.ID, id);
                        cv.put(Giustificativi.DATA, testo(o, "data"));
                        cv.put(Giustificativi.ID_TIPOLOGIA, o.get("id_tipologia").getAsInt());
                        cv.put(Giustificativi.ORE, o.get("ore").getAsDouble());
                        cv.put(Giustificativi.NOTE, testo(o, "note"));
                        cv.put(Giustificativi.STATO, testo(o, "stato"));
                        cv.put(Giustificativi.ORIGINE, testo(o, "origine"));
                        cv.put(Giustificativi.GRUPPO, testo(o, "gruppo"));
                        cv.put(Giustificativi.MOTIVO_RIFIUTO, testo(o, "motivo_rifiuto"));
                        cv.put(Giustificativi.APPROVATO_DA, testo(o, "approvato_da"));
                        cv.put(Giustificativi.APPROVATO_IL, testo(o, "approvato_il"));
                        cv.put(Giustificativi.DA_INVIARE, 0);
                        cv.put(Giustificativi.ELIMINATO, 0);
                        cv.putNull(Giustificativi.ERRORE);
                        db.insertWithOnConflict(Giustificativi.NOME_TABELLA, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                    }

                    db.delete(GiustificativiTipologie.NOME_TABELLA, null, null);
                    for (JsonElement e : array(b, "tipologie")) {
                        JsonObject o = e.getAsJsonObject();
                        ContentValues cv = new ContentValues();
                        cv.put(GiustificativiTipologie.ID, o.get("id").getAsInt());
                        cv.put(GiustificativiTipologie.CODICE, testo(o, "codice"));
                        cv.put(GiustificativiTipologie.DESCRIZIONE, testo(o, "descrizione"));
                        cv.put(GiustificativiTipologie.ICONA, testo(o, "icona"));
                        cv.put(GiustificativiTipologie.COLORE, testo(o, "colore"));
                        cv.put(GiustificativiTipologie.CONTABILE, o.has("contabile") && o.get("contabile").getAsBoolean() ? 1 : 0);
                        cv.put(GiustificativiTipologie.ORDINE, o.has("ordine") && !o.get("ordine").isJsonNull() ? o.get("ordine").getAsInt() : 0);
                        db.insertWithOnConflict(GiustificativiTipologie.NOME_TABELLA, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                SharedPreferences.Editor ed = prefs(ctx).edit().putLong(K_ULTIMO, System.currentTimeMillis());
                if (b.has("oreGiorno") && b.get("oreGiorno").isJsonObject()) ed.putString(K_ORE_GIORNO, b.get("oreGiorno").toString());
                if (!avvisi.isEmpty()) {
                    String prima = prefs(ctx).getString(K_AVVISI, "");
                    ed.putString(K_AVVISI, (prima.isEmpty() ? "" : prima + "\n") + String.join("\n", avvisi));
                }
                ed.apply();
                return avvisi;
            } finally {
                dbi.close();
            }
        }
    }

    /** Avvisi del server non ancora mostrati (es. modifica rifiutata perche' gia' approvato); li consuma. */
    public static String prendiAvvisi(Context ctx) {
        String a = prefs(ctx).getString(K_AVVISI, "");
        if (!a.isEmpty()) prefs(ctx).edit().remove(K_AVVISI).apply();
        return a;
    }

    /** Ci sono modifiche del dispositivo non ancora arrivate al server. */
    public static boolean inSospeso(Context ctx) {
        DbInterno dbi = new DbInterno(ctx);
        try (Cursor c = dbi.getReadableDatabase().rawQuery("select count(*) from " + Giustificativi.NOME_TABELLA + " where coalesce("
                + Giustificativi.DA_INVIARE + ",0)=1 or coalesce(" + Giustificativi.ELIMINATO + ",0)=1", null)) {
            return c.moveToFirst() && c.getInt(0) > 0;
        } catch (Exception e) {
            return false;
        } finally {
            dbi.close();
        }
    }

    // ── interni ──────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void aggiorna(Context ctx, int id, ContentValues cv) {
        DbInterno dbi = new DbInterno(ctx);
        try {
            dbi.getWritableDatabase().update(Giustificativi.NOME_TABELLA, cv, Giustificativi.ID + "=?", new String[]{String.valueOf(id)});
        } finally {
            dbi.close();
        }
    }

    private static boolean esiste(SQLiteDatabase db, long giorno, int idTipologia) {
        try (Cursor c = db.rawQuery("select count(*) from " + Giustificativi.NOME_TABELLA + " where " + Giustificativi.DATA + "=? and "
                + Giustificativi.ID_TIPOLOGIA + "=? and coalesce(" + Giustificativi.STATO + ",'') <> ? and coalesce(" + Giustificativi.ELIMINATO + ",0)=0",
                new String[]{data(giorno), String.valueOf(idTipologia), Giustificativi.STATO_RIFIUTATO})) {
            return c.moveToFirst() && c.getInt(0) > 0;
        }
    }

    private static int minimoId(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("select min(" + Giustificativi.ID + ") from " + Giustificativi.NOME_TABELLA, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private static boolean cambiatoNelFrattempo(SQLiteDatabase db, int id) {
        try (Cursor c = db.rawQuery("select count(*) from " + Giustificativi.NOME_TABELLA + " where " + Giustificativi.ID + "=? and (coalesce("
                + Giustificativi.DA_INVIARE + ",0)=1 or coalesce(" + Giustificativi.ELIMINATO + ",0)=1)", new String[]{String.valueOf(id)})) {
            return c.moveToFirst() && c.getInt(0) > 0;
        }
    }

    private static Voce voce(Cursor c) {
        Voce v = new Voce();
        v.id = c.getInt(c.getColumnIndexOrThrow(Giustificativi.ID));
        v.giorno = giorno(stringa(c, Giustificativi.DATA));
        v.idTipologia = c.getInt(c.getColumnIndexOrThrow(Giustificativi.ID_TIPOLOGIA));
        v.ore = c.getDouble(c.getColumnIndexOrThrow(Giustificativi.ORE));
        v.note = stringa(c, Giustificativi.NOTE);
        v.stato = stringa(c, Giustificativi.STATO);
        v.gruppo = stringa(c, Giustificativi.GRUPPO);
        v.motivoRifiuto = stringa(c, Giustificativi.MOTIVO_RIFIUTO);
        v.approvatoDa = stringa(c, Giustificativi.APPROVATO_DA);
        v.errore = stringa(c, Giustificativi.ERRORE);
        int daInviare = c.getInt(c.getColumnIndexOrThrow(Giustificativi.DA_INVIARE));
        v.daInviare = daInviare == 1;
        v.nonAccettato = daInviare == NON_ACCETTATO;
        return v;
    }

    private static Tipologia tipologia(Cursor c) {
        Tipologia t = new Tipologia();
        t.id = c.getInt(c.getColumnIndexOrThrow(GiustificativiTipologie.ID));
        t.codice = stringa(c, GiustificativiTipologie.CODICE);
        t.descrizione = stringa(c, GiustificativiTipologie.DESCRIZIONE);
        t.icona = stringa(c, GiustificativiTipologie.ICONA);
        t.colore = stringa(c, GiustificativiTipologie.COLORE);
        t.contabile = c.getInt(c.getColumnIndexOrThrow(GiustificativiTipologie.CONTABILE)) == 1;
        return t;
    }

    private static String stringa(Cursor c, String colonna) {
        int i = c.getColumnIndex(colonna);
        String s = i >= 0 ? c.getString(i) : null;
        return s != null ? s : "";
    }

    private static String testo(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    private static JsonArray array(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonArray() ? o.getAsJsonArray(k) : new JsonArray();
    }

    /** yyyyMMdd → "yyyy-MM-dd". */
    public static String data(long giorno) {
        return String.format(Locale.ROOT, "%04d-%02d-%02d", giorno / 10000, giorno / 100 % 100, giorno % 100);
    }

    /** "yyyy-MM-dd" → yyyyMMdd (0 se non valida). */
    public static long giorno(String data) {
        try {
            return Long.parseLong(data.replace("-", "").substring(0, 8));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String giornoLeggibile(long giorno) {
        return String.format(Locale.ROOT, "%02d/%02d/%04d", giorno % 100, giorno / 100 % 100, giorno / 10000);
    }
}
