package com.ncfsistemi.econ.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.ncfsistemi.econ.BuildConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Controlli all'apertura dell'app, prima dell'accesso (AGGIORNAMENTO_APP.md §2): il dispositivo puo' usare l'app?
 * Controlli locali (spazio libero) e, se il server risponde, POST api/version/avvio: terminale non disattivato in
 * Sonata › Terminali, ora del dispositivo uguale a quella del server, aggiornamento dell'app disponibile.
 * Prende il posto di VerificaServer all'avvio: una risposta qualsiasi del server vale come "raggiungibile" (anche il 404
 * di un server che non ha ancora api/version/avvio: allora i controlli del server si saltano).
 */
public final class ControlloAvvio {

    private static final int TIMEOUT_SECONDI = 5;
    /** Sotto questo spazio libero si avvisa: database, immagini e aggiornamenti non ci starebbero. */
    private static final long SPAZIO_MINIMO = 200L * 1024 * 1024;
    /** Oltre questa differenza con l'ora del server sessione e lavoro offline non sono affidabili. */
    private static final long SCARTO_ORA_MASSIMO_MS = 5L * 60 * 1000;

    public static final class Problema {
        public final String testo;
        /** Bloccante: l'app non puo' andare avanti (es. terminale disattivato). */
        public final boolean bloccante;

        Problema(String testo, boolean bloccante) {
            this.testo = testo;
            this.bloccante = bloccante;
        }
    }

    public static final class Esito {
        public boolean serverConfigurato;
        public boolean serverRaggiungibile;
        public String versioneServer;
        public final List<Problema> problemi = new ArrayList<>();
        /** null se il server non lo ha detto (non raggiungibile o senza api/version/avvio). */
        public Aggiornamento aggiornamento;

        public boolean bloccato() {
            for (Problema p : problemi) if (p.bloccante) return true;
            return false;
        }
    }

    /** Risposta di api/version/app/ultima (e parte di api/version/avvio). */
    public static final class Aggiornamento {
        public boolean disponibile;
        public boolean aggiornamento;
        public boolean obbligatorio;
        public String versione;
        public int versionCode;
        public String url;
        public String sha256;
        public long dimensione;
        public String note;
        public String pubblicataIl;
    }

    private static final class RispostaAvvio {
        Terminale terminale;
        long oraServer;
        String server;
        Aggiornamento aggiornamento;
    }

    private static final class Terminale {
        String stato;
        String messaggio;
    }

    public interface Dopo {
        void esito(Esito esito);
    }

    private ControlloAvvio() {
    }

    /** Controlli in background, esito sul thread principale. */
    public static void esegui(Context ctx, Dopo dopo) {
        Context app = ctx.getApplicationContext();
        new Thread(() -> {
            Esito e = controlla(app);
            new Handler(Looper.getMainLooper()).post(() -> dopo.esito(e));
        }, "controllo-avvio").start();
    }

    /** Chiamata bloccante: solo da un thread di lavoro. */
    public static Esito controlla(Context ctx) {
        Esito e = new Esito();

        long[] spazio = DatiDispositivo.spazio();
        if (spazio != null && spazio.length > 0 && spazio[0] >= 0 && spazio[0] < SPAZIO_MINIMO) {
            e.problemi.add(new Problema("Poco spazio libero sul dispositivo (" + (spazio[0] / (1024 * 1024))
                    + " MB): liberane un po' per dati, immagini e aggiornamenti.", false));
        }

        e.serverConfigurato = VerificaServer.configurato(ctx);
        if (!e.serverConfigurato || !Utility.isOnline(ctx)) return e;

        String url = Utility.getURLServer(ctx);
        url = (url.endsWith("/") ? url : url + "/") + "api/version/avvio";
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("device_serial", DatiDispositivo.seriale(ctx));
        corpo.put("versionCode", BuildConfig.VERSION_CODE);
        corpo.put("appCommit", BuildConfig.GIT_COMMIT);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDI, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDI, TimeUnit.SECONDS)
                .callTimeout(TIMEOUT_SECONDI + 1, TimeUnit.SECONDS)
                .build();
        Request req = new Request.Builder().url(url)
                .post(RequestBody.create(new Gson().toJson(corpo), MediaType.get("application/json")))
                .build();
        long inizio = System.currentTimeMillis();
        try (Response resp = client.newCall(req).execute()) {
            e.serverRaggiungibile = resp.code() > 0;
            if (!resp.isSuccessful() || resp.body() == null) return e;
            RispostaAvvio r = new Gson().fromJson(resp.body().string(), RispostaAvvio.class);
            if (r == null) return e;
            long fine = System.currentTimeMillis();
            e.versioneServer = r.server;
            if (r.server != null) VersioneApp.salvaServer(ctx, r.server);
            e.aggiornamento = r.aggiornamento;

            if (r.terminale != null && "disattivato".equals(r.terminale.stato)) {
                e.problemi.add(new Problema(r.terminale.messaggio != null ? r.terminale.messaggio
                        : "Questo dispositivo è stato disattivato.", true));
            }
            if (r.oraServer > 0) {
                // ora del server a meta' della chiamata
                long scarto = (inizio + fine) / 2 - r.oraServer;
                if (Math.abs(scarto) > SCARTO_ORA_MASSIMO_MS) {
                    e.problemi.add(new Problema("La data e l'ora del dispositivo sono sbagliate di "
                            + descriviScarto(Math.abs(scarto)) + " (" + (scarto > 0 ? "avanti" : "indietro")
                            + "): correggile nelle impostazioni di Android (data e ora automatiche), altrimenti "
                            + "l'accesso e la sincronizzazione possono non funzionare.", false));
                }
            }
        } catch (Exception ex) {
            e.serverRaggiungibile = false;
        }
        return e;
    }

    private static String descriviScarto(long ms) {
        long minuti = ms / 60000;
        if (minuti < 120) return minuti + " minuti";
        long ore = minuti / 60;
        if (ore < 48) return ore + " ore";
        return (ore / 24) + " giorni";
    }
}
