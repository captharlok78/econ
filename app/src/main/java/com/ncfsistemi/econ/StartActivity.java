package com.ncfsistemi.econ;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.ncfsistemi.econ.api.TokenManager;
import com.ncfsistemi.econ.utils.AccessoMercury;
import com.ncfsistemi.econ.utils.AccessoOffline;
import com.ncfsistemi.econ.utils.Sessione;
import com.ncfsistemi.econ.utils.Utility;

public class StartActivity extends EconActivity {

    private static final int REQUEST_PERMISSIONS = 1234;
    private static Handler splashHandler;
    /** inizia() può arrivare sia dal timer dello splash sia dai tocchi: l'accesso va fatto una volta sola. */
    private boolean avviato = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mostraVersioneReale();

        // nessun permesso sulla memoria condivisa all'avvio: i file dell'app sono nell'area privata
        // (SICUREZZA_E_PRIVACY.md, S5); la lettura si chiede solo per importare un file (EconFileChooserActivity)
        scheduleStart();
    }

    /**
     * Versione assegnata dal server a questa build (VersioneApp), non il versionName del manifest: quella salvata
     * subito, poi quella aggiornata appena il server risponde (se la schermata e' ancora sulla versione).
     */
    private void mostraVersioneReale() {
        TextView tv = findViewById(R.id.textViewVersione);
        if (tv == null) return;
        String iniziale = com.ncfsistemi.econ.utils.VersioneApp.etichetta(this);
        tv.setText(iniziale);
        com.ncfsistemi.econ.utils.VersioneApp.aggiorna(this, versione -> {
            if (!isFinishing() && iniziale.contentEquals(tv.getText())) {
                tv.setText("v " + versione);
            }
        });
    }

    private void scheduleStart() {
        if (splashHandler == null) {
            splashHandler = new Handler(Looper.getMainLooper());
            splashHandler.postDelayed(() -> {
                splashHandler = null;
                if (!isFinishing() && !isDestroyed()) inizia();
            }, 2500);
        } else {
            inizia();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code == REQUEST_PERMISSIONS
                && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            scheduleStart();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (splashHandler != null) {
                splashHandler.removeCallbacksAndMessages(null);
                splashHandler = null;
            }
            inizia();
        }
        return super.onTouchEvent(event);
    }

    private void inizia() {
        if (avviato) return;
        avviato = true;
        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
            splashHandler = null;
        }

        // Ad ogni apertura l'app si riallinea al server (moduli, pacchetti, dati) prima del menu: con "Ricordami" rifà
        // l'accesso con le credenziali salvate, altrimenti usa il token ancora valido. Se il server non risponde si
        // entra offline quando e' possibile (ACCESSO_OFFLINE.md §2.3).
        TokenManager tm = TokenManager.getInstance(this);
        if (!tm.hasToken() && !tm.hasCredenzialiRicordami()) {
            apriLogin();
            return;
        }
        ((TextView) findViewById(R.id.textViewVersione)).setText("Verifica del server...");
        com.ncfsistemi.econ.utils.VerificaServer.verifica(this, raggiungibile -> {
            if (isFinishing() || isDestroyed()) return;
            if (raggiungibile) {
                accediOnline();
            } else {
                senzaServer();
            }
        });
    }

    private void accediOnline() {
        TokenManager tm = TokenManager.getInstance(this);
        if (tm.hasCredenzialiRicordami()) {
            ((TextView) findViewById(R.id.textViewVersione)).setText("Accesso in corso...");
            AccessoMercury.accediConCredenzialiRicordate(this, new AccessoMercury.Esito() {
                @Override
                public void accesso() {
                    AccessoMercury.apriAllineamento(StartActivity.this);
                    finish();
                }

                @Override
                public void rifiutato(String messaggio) {
                    android.widget.Toast.makeText(StartActivity.this, messaggio, android.widget.Toast.LENGTH_LONG).show();
                    apriLogin();
                }

                @Override
                public void erroreRete(String messaggio) {
                    senzaServer();
                }
            });
            return;
        }
        if (tm.isTokenValido() && tm.getIdDitta() > 0) {
            Sessione.ripristinaDaToken(this);
            AccessoMercury.apriAllineamento(this);
            finish();
        } else {
            apriLogin();
        }
    }

    /** Server non raggiungibile: con "Ricordami" (o token ancora valido) si entra offline, altrimenti il login offline. */
    private void senzaServer() {
        TokenManager tm = TokenManager.getInstance(this);
        if (AccessoOffline.possibile(this) && (tm.hasCredenzialiRicordami() || tm.isTokenValido())) {
            AccessoOffline.entra(this);
            finish();
        } else {
            apriLogin();
        }
    }

    private void apriLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected int getBackgroundID() {
        return NO_BACKGROUND;
    }

    @Override
    protected int getMenuID() {
        return NO_MENU;
    }

    @Override
    protected boolean isControllaRegistrazione() {
        return false;
    }

    @Override
    protected boolean isControllaLogin() {
        return false;
    }
}
