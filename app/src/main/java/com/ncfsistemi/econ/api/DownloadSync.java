package com.ncfsistemi.econ.api;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;

/**
 * Download della sync letto a pezzi. La risposta di /api/mobile/sync/download può essere di decine di MB (lo storico
 * del disegno: decine di migliaia di elementi, componenti e collegamenti) e tenerla tutta in memoria come oggetti
 * faceva cadere l'app. Qui si legge come flusso: ogni tabella finisce in un file temporaneo (una riga JSON per record)
 * nella cache dell'app; poi le tabelle si applicano a blocchi, nell'ordine che serve a chi le usa, e alla fine
 * ({@link #close()}) i file si cancellano.
 *
 * <pre>
 * try (DownloadSync dl = DownloadSync.leggi(ctx, risposta.body())) {
 *     dl.perBlocchi("cantieri", DownloadSync.BLOCCO, (righe, primo) -> ...);
 * }
 * </pre>
 */
public final class DownloadSync implements Closeable {

    /** Righe applicate per volta (una transazione per blocco). */
    public static final int BLOCCO = 500;

    private static final String PREFISSO_CARTELLA = "sync-download-";
    private static final long UN_GIORNO_MS = 24L * 60 * 60 * 1000;

    /** Ricevuto per blocco: {@code primo} vale true per il primo blocco della tabella. */
    public interface Blocco {
        void applica(List<JsonObject> righe, boolean primo) throws IOException;
    }

    public String syncTimestamp;
    public final List<MercuryApiService.SyncDownloadResponse.Fuori> fuori = new ArrayList<>();
    public MercuryApiService.SyncDownloadResponse.Perimetro perimetro;

    /** Tabella → numero di righe, nell'ordine in cui le ha mandate il server. */
    private final Map<String, Integer> conteggi = new LinkedHashMap<>();
    private final Map<String, File> file = new LinkedHashMap<>();
    private final File cartella;
    private final Gson gson = new Gson();

    private DownloadSync(File cartella) {
        this.cartella = cartella;
    }

    /** Legge la risposta e la divide per tabella; chiude il corpo della risposta. Se qualcosa va storto non lascia file. */
    public static DownloadSync leggi(Context ctx, ResponseBody corpo) throws IOException {
        pulisciVecchie(ctx.getCacheDir());
        File cartella = new File(ctx.getCacheDir(), PREFISSO_CARTELLA + System.currentTimeMillis() + "-" + System.nanoTime());
        if (!cartella.mkdirs()) {
            corpo.close();
            throw new IOException("Cartella temporanea della sync non creata: " + cartella);
        }
        DownloadSync dl = new DownloadSync(cartella);
        try (JsonReader r = new JsonReader(new BufferedReader(corpo.charStream(), 1 << 16))) {
            r.beginObject();
            while (r.hasNext()) {
                String chiave = r.nextName();
                if (r.peek() == JsonToken.NULL) {
                    r.nextNull();
                    continue;
                }
                switch (chiave) {
                    case "syncTimestamp":
                        dl.syncTimestamp = r.nextString();
                        break;
                    case "tables":
                        dl.leggiTabelle(r);
                        break;
                    case "fuori":
                        r.beginArray();
                        while (r.hasNext()) {
                            dl.fuori.add(dl.gson.fromJson(r, MercuryApiService.SyncDownloadResponse.Fuori.class));
                        }
                        r.endArray();
                        break;
                    case "perimetro":
                        dl.perimetro = dl.gson.fromJson(r, MercuryApiService.SyncDownloadResponse.Perimetro.class);
                        break;
                    default:
                        r.skipValue();
                }
            }
            r.endObject();
        } catch (IOException | RuntimeException e) {
            dl.close();
            throw e instanceof IOException ? (IOException) e : new IOException("Risposta della sync non leggibile: " + e.getMessage(), e);
        } finally {
            corpo.close();
        }
        return dl;
    }

    private void leggiTabelle(JsonReader r) throws IOException {
        r.beginObject();
        while (r.hasNext()) {
            String tabella = r.nextName();
            if (r.peek() != JsonToken.BEGIN_ARRAY) {
                r.skipValue();
                continue;
            }
            File f = new File(cartella, tabella.replaceAll("[^A-Za-z0-9_]", "_") + ".jsonl");
            int n = 0;
            r.beginArray();
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8), 1 << 16)) {
                while (r.hasNext()) {
                    if (r.peek() != JsonToken.BEGIN_OBJECT) {
                        r.skipValue();
                        continue;
                    }
                    JsonObject riga = gson.fromJson(r, JsonObject.class);
                    w.write(riga.toString()); // Gson: una riga sola, gli a capo nei testi restano \n
                    w.write('\n');
                    n++;
                }
            }
            r.endArray();
            conteggi.put(tabella, n);
            file.put(tabella, f);
        }
        r.endObject();
    }

    /** Tabelle ricevute, nell'ordine del server. */
    public List<String> tabelle() {
        return new ArrayList<>(conteggi.keySet());
    }

    /** Righe ricevute per la tabella (0 se non c'è). */
    public int conta(String tabella) {
        Integer n = conteggi.get(tabella);
        return n != null ? n : 0;
    }

    /** Totale delle righe ricevute. */
    public int totale() {
        int t = 0;
        for (int n : conteggi.values()) t += n;
        return t;
    }

    /** Le righe della tabella a blocchi di {@code dimensione}; niente se la tabella è vuota o assente. */
    public void perBlocchi(String tabella, int dimensione, Blocco blocco) throws IOException {
        File f = file.get(tabella);
        if (f == null || conta(tabella) == 0) return;
        List<JsonObject> righe = new ArrayList<>(Math.min(dimensione, conta(tabella)));
        boolean primo = true;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8), 1 << 16)) {
            String riga;
            while ((riga = br.readLine()) != null) {
                if (riga.isEmpty()) continue;
                righe.add(gson.fromJson(riga, JsonObject.class));
                if (righe.size() >= dimensione) {
                    blocco.applica(righe, primo);
                    primo = false;
                    righe = new ArrayList<>(dimensione);
                }
            }
        }
        if (!righe.isEmpty()) blocco.applica(righe, primo);
    }

    /** Tutte le righe della tabella in memoria: solo per tabelle piccole che vanno trattate intere (es. operatori). */
    public List<JsonObject> tutte(String tabella) throws IOException {
        if (conta(tabella) == 0) return Collections.emptyList();
        List<JsonObject> tutte = new ArrayList<>(conta(tabella));
        perBlocchi(tabella, Integer.MAX_VALUE, (righe, primo) -> tutte.addAll(righe));
        return tutte;
    }

    /** Timestamp, perimetro e righe fuori come nella risposta intera (senza le tabelle), per SyncUtil.applicaPerimetro. */
    public MercuryApiService.SyncDownloadResponse comeRisposta() {
        MercuryApiService.SyncDownloadResponse r = new MercuryApiService.SyncDownloadResponse();
        r.syncTimestamp = syncTimestamp;
        r.fuori = fuori;
        r.perimetro = perimetro;
        return r;
    }

    /** Cancella i file temporanei. */
    @Override
    public void close() {
        cancella(cartella);
    }

    /** Cartelle rimaste da sync interrotte (app chiusa a metà): via quelle di più di un giorno. */
    private static void pulisciVecchie(File cache) {
        File[] vecchie = cache.listFiles((d, nome) -> nome.startsWith(PREFISSO_CARTELLA));
        if (vecchie == null) return;
        long limite = System.currentTimeMillis() - UN_GIORNO_MS;
        for (File c : vecchie) {
            if (c.lastModified() < limite) cancella(c);
        }
    }

    private static void cancella(File f) {
        File[] figli = f.listFiles();
        if (figli != null) {
            for (File x : figli) cancella(x);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
