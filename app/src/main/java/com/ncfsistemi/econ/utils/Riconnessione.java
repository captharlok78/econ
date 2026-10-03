package com.ncfsistemi.econ.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;

import com.ncfsistemi.econ.EconActivity;

/**
 * Ritorno del server mentre si lavora offline (ACCESSO_OFFLINE.md §2.4): ascolta la rete e riprova ogni INTERVALLO_MS.
 * Quando il server risponde toglie la modalita' offline e avvisa la schermata aperta (EconActivity.notificaRicollegamento),
 * che propone l'invio delle modifiche; se nessuna schermata e' in primo piano l'avviso aspetta la prossima.
 */
public final class Riconnessione {

    private static final long INTERVALLO_MS = 60_000;
    private static final long MINIMO_MS = 10_000;
    private static long ultimaVerifica = 0;

    private static boolean attiva = false;
    private static Handler handler;
    private static ConnectivityManager.NetworkCallback callback;
    private static volatile boolean daNotificare = false;

    private static final Runnable PERIODICA = new Runnable() {
        @Override
        public void run() {
            if (contesto != null) verifica(contesto);
            if (attiva && handler != null) handler.postDelayed(this, INTERVALLO_MS);
        }
    };
    private static Context contesto;

    private Riconnessione() {
    }

    /** Un controllo del server non ha avuto risposta: si lavora offline finche' non torna. */
    public static void serverNonRaggiungibile(Context ctx) {
        Sessione.setOffline(true);
        avvia(ctx);
    }

    public static synchronized void avvia(Context ctx) {
        if (attiva) return;
        attiva = true;
        contesto = ctx.getApplicationContext();
        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(PERIODICA, INTERVALLO_MS);
        ConnectivityManager cm = (ConnectivityManager) contesto.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            callback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    // la rete appena tornata puo' non essere ancora pronta: si prova dopo qualche secondo
                    if (handler != null) handler.postDelayed(() -> verifica(contesto), 3000);
                }
            };
            try {
                cm.registerDefaultNetworkCallback(callback);
            } catch (Exception e) {
                callback = null; // resta il controllo periodico
            }
        }
    }

    private static synchronized void ferma() {
        attiva = false;
        if (handler != null) handler.removeCallbacks(PERIODICA);
        if (callback != null && contesto != null) {
            ConnectivityManager cm = (ConnectivityManager) contesto.getSystemService(Context.CONNECTIVITY_SERVICE);
            try {
                if (cm != null) cm.unregisterNetworkCallback(callback);
            } catch (Exception ignored) {
                // gia' tolto
            }
        }
        callback = null;
    }

    /** Prova subito (es. al ritorno su una schermata), non piu' di una volta ogni MINIMO_MS. */
    public static void verifica(Context ctx) {
        if (!Sessione.isOffline()) {
            ferma();
            return;
        }
        long ora = System.currentTimeMillis();
        if (ora - ultimaVerifica < MINIMO_MS) return;
        ultimaVerifica = ora;
        VerificaServer.verifica(ctx, ok -> {
            if (ok && Sessione.isOffline()) ricollegato();
        });
    }

    /** Un controllo del server (es. il LED del menu) ha avuto risposta: se si era offline, ricollegato. */
    public static void serverRaggiungibile() {
        if (Sessione.isOffline()) ricollegato();
    }

    private static void ricollegato() {
        Sessione.setOffline(false);
        ferma();
        daNotificare = true;
        EconActivity attuale = EconActivity.inPrimoPiano();
        if (attuale != null) attuale.notificaRicollegamento();
    }

    /** true una sola volta dopo il ricollegamento: chi la legge mostra l'avviso. */
    public static boolean consumaNotifica() {
        boolean d = daNotificare;
        daNotificare = false;
        return d;
    }

}
