package com.ncfsistemi.econ.utils;

import android.app.Activity;
import android.util.Log;

import com.ncfsistemi.econ.api.MercuryApiClient;
import com.ncfsistemi.econ.api.MercuryApiService;
import com.ncfsistemi.econ.api.TokenManager;
import retrofit2.Response;

/**
 * Moduli e funzionalita' dell'utente riletti dal server (GET api/auth/moduli) anche fuori dall'avvio: cosi' una modifica ai
 * pacchetti fatta nel pannello (Sonata) vale subito anche nel tablet, al ritorno sul menu o alla sincronizzazione
 * (PACCHETTI_E_FUNZIONALITA.md). In background, al massimo una volta ogni INTERVALLO_MS; senza rete o con errore non tocca
 * nulla. Il server applica comunque le stesse regole a ogni invio.
 */
public final class PermessiServer {

    private static final String TAG = "PermessiServer";
    private static final long INTERVALLO_MS = 30_000;
    private static long ultimaLettura = 0;

    private PermessiServer() {
    }

    /** Rilegge i permessi; se sono cambiati esegue seCambiati sul thread principale (es. ridisegnare il menu). */
    public static void aggiorna(Activity activity, Runnable seCambiati) {
        long ora = System.currentTimeMillis();
        if (ora - ultimaLettura < INTERVALLO_MS || Sessione.isOffline() || !Utility.isOnline(activity)) return;
        TokenManager tm = TokenManager.getInstance(activity);
        if (!tm.hasToken()) return;
        ultimaLettura = ora;
        new Thread(() -> {
            try {
                if (leggi(activity) && seCambiati != null) activity.runOnUiThread(seCambiati);
            } catch (Exception e) {
                Log.w(TAG, "Permessi non riletti: " + e.getMessage());
            }
        }, "permessi-server").start();
    }

    /** Lettura sincrona (da un thread di lavoro, es. la sincronizzazione); ritorna true se moduli o funzionalita' sono cambiati. */
    public static boolean leggi(android.content.Context ctx) throws Exception {
        MercuryApiService api = MercuryApiClient.getInstance(ctx).getService();
        Response<MercuryApiService.ModuliResponse> resp = api.getModuli().execute();
        Sessione.setOffline(false); // il server ha risposto
        if (!resp.isSuccessful() || resp.body() == null) return false;
        TokenManager tm = TokenManager.getInstance(ctx);
        String prima = tm.improntaPermessi();
        tm.saveModuli(resp.body().moduli);
        tm.saveFunzionalita(resp.body().funzionalita);
        tm.savePianificazione(resp.body().pianificazione);
        tm.savePacchetti(resp.body().pacchetti);
        com.ncfsistemi.econ.api.ProfiloOffline.segnaOnline(ctx, resp.body().offlineGiorni);
        ultimaLettura = System.currentTimeMillis();
        return !prima.equals(tm.improntaPermessi());
    }
}
