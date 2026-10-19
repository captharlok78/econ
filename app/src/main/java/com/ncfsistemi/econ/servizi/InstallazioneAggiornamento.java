package com.ncfsistemi.econ.servizi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

/**
 * Esito dell'installazione dell'aggiornamento (utils/AggiornaApp, AGGIORNAMENTO_APP.md §3): se Android vuole la
 * conferma dell'utente apre la sua finestra "Aggiorna"; in caso di errore lo dice. Se riesce, Android chiude Econ e
 * la sostituisce con la versione nuova (si riapre dall'icona).
 */
public class InstallazioneAggiornamento extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        int stato = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (stato == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @SuppressWarnings("deprecation")
            Intent conferma = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (conferma != null) {
                conferma.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(conferma);
            }
        } else if (stato == PackageInstaller.STATUS_SUCCESS) {
            Toast.makeText(ctx, "iEcon aggiornata", Toast.LENGTH_SHORT).show();
        } else if (stato != PackageInstaller.STATUS_FAILURE_ABORTED) {
            String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            String testo = stato == PackageInstaller.STATUS_FAILURE_CONFLICT || stato == PackageInstaller.STATUS_FAILURE_INCOMPATIBLE
                    ? "Aggiornamento non installato: l'app sul dispositivo è firmata con un'altra chiave. Disinstallala una volta e installa iEcon dal portale."
                    : "Aggiornamento non installato" + (msg != null ? ": " + msg : "");
            Toast.makeText(ctx, testo, Toast.LENGTH_LONG).show();
        }
    }
}
