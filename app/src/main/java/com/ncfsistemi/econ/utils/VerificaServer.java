package com.ncfsistemi.econ.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Il server risponde davvero? (ACCESSO_OFFLINE.md §2.3). Utility.isOnline dice solo se c'e' una rete: con il Wi-Fi
 * acceso ma il server spento o irraggiungibile l'app deve comunque poter entrare offline. Qualsiasi risposta HTTP vale
 * come raggiungibile; nessuna risposta entro TIMEOUT_SECONDI no.
 */
public final class VerificaServer {

    private static final int TIMEOUT_SECONDI = 4;

    public interface Esito {
        void risultato(boolean raggiungibile);
    }

    private VerificaServer() {
    }

    public static boolean configurato(Context ctx) {
        return !ctx.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE).getString("URL", "").isEmpty();
    }

    /** Chiamata bloccante: solo da un thread di lavoro. */
    public static boolean raggiungibile(Context ctx) {
        if (!configurato(ctx) || !Utility.isOnline(ctx)) return false;
        String url = Utility.getURLServer(ctx);
        url = (url.endsWith("/") ? url : url + "/") + "api/auth/ditte";
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDI, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDI, TimeUnit.SECONDS)
                .callTimeout(TIMEOUT_SECONDI + 1, TimeUnit.SECONDS)
                .build();
        try (Response resp = client.newCall(new Request.Builder().url(url).build()).execute()) {
            return resp.code() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Verifica in background, risultato sul thread principale. */
    public static void verifica(Context ctx, Esito esito) {
        Context app = ctx.getApplicationContext();
        new Thread(() -> {
            boolean ok = raggiungibile(app);
            new Handler(Looper.getMainLooper()).post(() -> esito.risultato(ok));
        }, "verifica-server").start();
    }
}
