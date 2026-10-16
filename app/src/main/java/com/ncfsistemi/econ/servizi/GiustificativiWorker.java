package com.ncfsistemi.econ.servizi;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ncfsistemi.econ.api.MercuryApiClient;
import com.ncfsistemi.econ.api.TokenManager;
import com.ncfsistemi.econ.utils.GiustificativiLocali;

/**
 * Invio dei giustificativi appena salvati nel calendario (STUDIO_GIUSTIFICATIVI_APP.md §2.4): parte con la rete
 * (GiustificativiLocali.inviaInBackground) e riprende lo stato dal server. Senza rete WorkManager aspetta; con un errore
 * di rete riprova piu' tardi.
 */
public class GiustificativiWorker extends Worker {

    private static final String TAG = "GiustificativiWorker";

    public GiustificativiWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        if (!TokenManager.getInstance(ctx).hasToken()) {
            return Result.failure(); // senza accesso partono con la prossima sincronizzazione
        }
        try {
            GiustificativiLocali.allinea(ctx, MercuryApiClient.getInstance(ctx).getService());
            return Result.success();
        } catch (java.io.IOException e) {
            Log.w(TAG, "Rete non disponibile, si riprova: " + e.getMessage());
            return getRunAttemptCount() < 10 ? Result.retry() : Result.failure();
        } catch (Exception e) {
            Log.w(TAG, "Invio giustificativi non riuscito: " + e.getMessage());
            return getRunAttemptCount() < 3 ? Result.retry() : Result.failure();
        }
    }
}
