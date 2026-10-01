package com.ncfsistemi.econ;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ncfsistemi.econ.api.MercuryApiService;
import com.ncfsistemi.econ.api.TokenManager;
import com.ncfsistemi.econ.utils.AccessoMercury;
import com.ncfsistemi.econ.utils.CambioPassword;
import com.ncfsistemi.econ.utils.Utility;

/**
 * Cambio password obbligatorio (ACCESSI_E_LICENZE.md §7): la password è scaduta secondo la ditta e il server non
 * accetta altre chiamate finché non la si cambia. Si apre al posto dell'allineamento (AccessoMercury.apriAllineamento);
 * dopo il cambio prosegue con l'allineamento, "Esci" torna al login.
 */
public class CambioPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cambio_password);

        EditText etAttuale = findViewById(R.id.etPasswordAttuale);
        EditText etNuova = findViewById(R.id.etPasswordNuova);
        EditText etConferma = findViewById(R.id.etPasswordConferma);
        Button btnConferma = findViewById(R.id.btnCambiaPassword);
        TextView tvEmail = findViewById(R.id.tvCambioPasswordEmail);

        String email = TokenManager.getInstance(this).getEmailRicordami();
        if (email.isEmpty()) {
            email = getSharedPreferences(Utility.APP_NAME, MODE_PRIVATE).getString("MERCURY_EMAIL", "");
        }
        tvEmail.setText(email);
        etNuova.setHint("Nuova password (almeno " + CambioPassword.LUNGHEZZA_MINIMA + " caratteri)");

        btnConferma.setOnClickListener(v -> {
            String attuale = etAttuale.getText().toString();
            String nuova = etNuova.getText().toString();
            String errore = CambioPassword.errore(attuale, nuova, etConferma.getText().toString());
            if (errore != null) {
                Utility.mostraDialog("Cambia password", errore, this, "OK");
                return;
            }
            btnConferma.setEnabled(false);
            CambioPassword.invia(this, attuale, nuova, new CambioPassword.Esito() {
                @Override
                public void fatto(MercuryApiService.PasswordProfilo password) {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(CambioPasswordActivity.this, "Password aggiornata.", Toast.LENGTH_LONG).show();
                    AccessoMercury.apriAllineamento(CambioPasswordActivity.this);
                    finish();
                }

                @Override
                public void errore(String messaggio) {
                    if (isFinishing() || isDestroyed()) return;
                    btnConferma.setEnabled(true);
                    Utility.mostraDialog("Cambia password", messaggio, CambioPasswordActivity.this, "OK");
                }
            });
        });
    }

    /** "Esci": senza cambio password non si entra; torna al login (i dati sul dispositivo restano). */
    public void esci(View v) {
        TokenManager.getInstance(this).clearToken();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        esci(null);
    }
}
