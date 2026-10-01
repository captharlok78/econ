package com.ncfsistemi.econ;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import com.ncfsistemi.econ.api.TokenManager;
import com.ncfsistemi.econ.utils.AccessoMercury;
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

        // Ad ogni apertura l'app si riallinea al server (moduli, pacchetti, dati) prima del menu:
        // con "Ricordami" rifà l'accesso con le credenziali salvate, altrimenti usa il token ancora valido.
        TokenManager tm = TokenManager.getInstance(this);
        boolean tokenValido = tm.hasToken() && isTokenValid() && tm.getIdDitta() > 0;

        if (tm.hasCredenzialiRicordami() && Utility.isOnline(this)) {
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
                    // Server non raggiungibile: si prosegue con i dati del dispositivo se il token vale ancora.
                    if (tokenValido) {
                        Sessione.ripristinaDaToken(StartActivity.this);
                        AccessoMercury.apriAllineamento(StartActivity.this);
                        finish();
                    } else {
                        apriLogin();
                    }
                }
            });
            return;
        }

        if (tokenValido) {
            Sessione.ripristinaDaToken(this);
            AccessoMercury.apriAllineamento(this);
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }

    private void apriLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private boolean isTokenValid() {
        String token = TokenManager.getInstance(this).getToken();
        if (token == null) return false;
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;
            byte[] decoded = Base64.decode(
                    parts[1].replace('-', '+').replace('_', '/'),
                    Base64.DEFAULT);
            JSONObject payload = new JSONObject(new String(decoded, "UTF-8"));
            long exp = payload.optLong("exp", 0);
            return exp > System.currentTimeMillis() / 1000L;
        } catch (Exception e) {
            return false;
        }
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
