package com.ncfsistemi.econ;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ncfsistemi.econ.api.MercuryApiClient;
import com.ncfsistemi.econ.api.MercuryApiService;
import com.ncfsistemi.econ.api.TokenManager;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.utils.Sessione;
import com.ncfsistemi.econ.utils.Utility;
import retrofit2.Call;
import retrofit2.Callback;

public class LoginActivity extends EconActivity {

    private View indicatoreConnessione = null;
    private TextView testoStatoConnessione = null;
    private TextView testoVersioneApp = null;
    private CheckBox checkBoxRicordamiMercury = null;
    private String versioneAppTesto = "";
    private String versioneMercuryTesto = "";
    /** Server non raggiungibile e profilo offline disponibile: la password si controlla sul tablet (ACCESSO_OFFLINE.md). */
    private boolean modoOffline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("Econ: LoginActivity onCreate ENTER");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        indicatoreConnessione = findViewById(R.id.indicatoreConnessione);
        testoStatoConnessione = (TextView) findViewById(R.id.testoStatoConnessione);
        testoVersioneApp = (TextView) findViewById(R.id.testoVersioneApp);
        checkBoxRicordamiMercury = (CheckBox) findViewById(R.id.checkBoxRicordamiMercury);

        caricaVersioni();

        // "Ricordami": se presenti, precompila email+password cifrate (hanno priorità
        // sul solo-email storico sotto, che resta per chi non ha mai spuntato "Ricordami").
        TokenManager tm = TokenManager.getInstance(this);
        if (tm.hasCredenzialiRicordami()) {
            ((EditText) findViewById(R.id.editTextEmail)).setText(tm.getEmailRicordami());
            ((EditText) findViewById(R.id.editTextPasswordMercury)).setText(tm.getPasswordRicordami());
            checkBoxRicordamiMercury.setChecked(true);
        } else {
            // Ripristina l'email usata nell'ultimo login Mercury
            String emailSalvata = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE)
                    .getString("MERCURY_EMAIL", "");
            if (!emailSalvata.isEmpty()) {
                ((EditText) findViewById(R.id.editTextEmail)).setText(emailSalvata);
            }
        }

        System.out.println("Econ: LoginActivity onCreate EXIT");
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("Econ: LoginActivity onResume ENTER");

        // Auto-login solo se NON siamo stati aperti per fare un nuovo login esplicito
        boolean returnAfterLogin = getIntent().getBooleanExtra(EXTRA_RETURN_AFTER_LOGIN, false);
        if (!returnAfterLogin && TokenManager.getInstance(this).isTokenValido()) {
            setupSessioneFromToken();
            avanzaAlMenu();
            return;
        }

        // Verifica connessione Mercury in background
        verificaConnessione();

        System.out.println("Econ: LoginActivity onResume EXIT");
    }

    /**
     * Recupera la versione "umana" dell'app (risolta da Mercury a partire dal commit
     * HEAD della build, BuildConfig.GIT_COMMIT) e quella di Mercury stesso (che risolve
     * da solo il proprio commit in esecuzione), e le mostra insieme, centrate, sotto la
     * card di login. Se una chiamata fallisce (server non configurato/raggiungibile) quel
     * pezzo ricade sul versionName locale del pacchetto (per l'app) o viene omesso (per
     * Mercury, che non è determinabile offline), così il testo non resta mai vuoto.
     */
    private void caricaVersioni() {
        // versione dal registro dei rilasci (salvata), altrimenti l'identificativo della build
        versioneAppTesto = "App " + com.ncfsistemi.econ.utils.VersioneApp.etichetta(this);
        versioneMercuryTesto = "";
        aggiornaTestoVersioni();

        SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        if (pref.getString("URL", "").isEmpty()) {
            return;
        }

        MercuryApiService api = MercuryApiClient.getInstance(this).getService();

        api.getVersioneApp(com.ncfsistemi.econ.BuildConfig.GIT_COMMIT).enqueue(new Callback<MercuryApiService.VersionResponse>() {
            @Override
            public void onResponse(Call<MercuryApiService.VersionResponse> call,
                                   retrofit2.Response<MercuryApiService.VersionResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && response.body().versione != null) {
                    com.ncfsistemi.econ.utils.VersioneApp.salva(LoginActivity.this, response.body().versione);
                    versioneAppTesto = "App " + com.ncfsistemi.econ.utils.VersioneApp.etichetta(LoginActivity.this);
                    aggiornaTestoVersioni();
                }
            }

            @Override
            public void onFailure(Call<MercuryApiService.VersionResponse> call, Throwable t) {
                // Nessuna azione: resta il fallback locale già impostato
            }
        });

        api.getVersioneMercury().enqueue(new Callback<MercuryApiService.VersionResponse>() {
            @Override
            public void onResponse(Call<MercuryApiService.VersionResponse> call,
                                   retrofit2.Response<MercuryApiService.VersionResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && response.body().versione != null) {
                    versioneMercuryTesto = "Mercury v" + response.body().versione;
                    com.ncfsistemi.econ.utils.VersioneApp.salvaServer(LoginActivity.this, response.body().versione);
                    com.ncfsistemi.econ.utils.Editore.salva(LoginActivity.this, response.body());
                    aggiornaTestoVersioni();
                }
            }

            @Override
            public void onFailure(Call<MercuryApiService.VersionResponse> call, Throwable t) {
                // Nessuna azione: Mercury non raggiungibile, si mostra solo la versione app
            }
        });
    }

    private void aggiornaTestoVersioni() {
        String testo = versioneMercuryTesto.isEmpty()
                ? versioneAppTesto
                : versioneAppTesto + "  ·  " + versioneMercuryTesto;
        testoVersioneApp.setText(testo);
    }

    private void verificaConnessione() {
        SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        String url = pref.getString("URL", "");
        if (url.isEmpty()) {
            indicatoreConnessione.setBackgroundColor(Color.parseColor("#9E9E9E"));
            testoStatoConnessione.setText("Server non configurato");
            return;
        }

        indicatoreConnessione.setBackgroundColor(Color.parseColor("#FFC107"));
        testoStatoConnessione.setText("Verifica connessione...");

        new Thread(() -> {
            final boolean raggiungibile = com.ncfsistemi.econ.utils.VerificaServer.raggiungibile(this);
            final String urlDisplay = url;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (raggiungibile) {
                    indicatoreConnessione.setBackgroundColor(Color.parseColor("#4CAF50"));
                    testoStatoConnessione.setText("Connesso: " + urlDisplay);
                    impostaModoOffline(false);
                } else {
                    indicatoreConnessione.setBackgroundColor(Color.parseColor("#F44336"));
                    testoStatoConnessione.setText("Server non raggiungibile");
                    impostaModoOffline(offlineConsentito());
                }
            });
        }).start();
    }

    /** Login con credenziali Mercury (JWT). */
    public void accediMercury(View v) {
        System.out.println("Econ: LoginActivity accediMercury ENTER");

        EditText editEmail = findViewById(R.id.editTextEmail);
        EditText editPasswordMercury = findViewById(R.id.editTextPasswordMercury);

        String email = editEmail.getText().toString().trim();
        String password = editPasswordMercury.getText().toString();

        if (email.isEmpty()) {
            editEmail.setError("Inserisci l'email");
            return;
        }
        if (password.isEmpty()) {
            editPasswordMercury.setError("Inserisci la password");
            return;
        }

        if (modoOffline) {
            accediOffline(v, email, password);
            return;
        }

        SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        if (pref.getString("URL", "").isEmpty()) {
            Utility.mostraDialog("Server non configurato",
                    "Tocca l'icona delle impostazioni in alto a destra per configurare l'indirizzo del server Mercury.",
                    this, "OK");
            return;
        }

        v.setEnabled(false);
        Toast.makeText(this, "Accesso in corso...", Toast.LENGTH_SHORT).show();

        String deviceSerial = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        MercuryApiService api = MercuryApiClient.getInstance(this).getService();
        api.login(new MercuryApiService.LoginRequest(email, password, null, deviceSerial))
                .enqueue(new Callback<MercuryApiService.LoginResponse>() {
                    @Override
                    public void onResponse(Call<MercuryApiService.LoginResponse> call,
                                           retrofit2.Response<MercuryApiService.LoginResponse> response) {
                        v.setEnabled(true);
                        if (isFinishing() || isDestroyed()) return;

                        if (response.isSuccessful() && response.body() != null) {
                            MercuryApiService.LoginResponse body = response.body();
                            TokenManager tm = TokenManager.getInstance(LoginActivity.this);
                            tm.saveToken(body);
                            com.ncfsistemi.econ.api.ProfiloOffline.registraPassword(LoginActivity.this, email, password);
                            Sessione.setOffline(false);

                            // "Ricordami": salva o cancella email+password cifrate a seconda della
                            // checkbox. Restano finché non si fa logout (TokenManager.clearToken()).
                            if (checkBoxRicordamiMercury.isChecked()) {
                                tm.saveCredenzialiRicordami(email, password);
                            } else {
                                tm.clearCredenzialiRicordami();
                            }

                            // Salva l'email (per il campo del login)
                            getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("MERCURY_EMAIL", email)
                                    .apply();

                            // Imposta la sessione in-memory usata dal resto dell'app
                            setupSessioneFromToken();

                            avanzaAlMenu();
                        } else {
                            String msg;
                            if (response.code() == 403) com.ncfsistemi.econ.api.ProfiloOffline.blocca(LoginActivity.this);
                            if (response.code() == 401) msg = "Email o password errata.";
                            else if (response.code() == 403 || response.code() == 429) msg = MercuryApiClient.messaggioErrore(response, "Accesso non consentito");
                            else if (response.code() >= 500) msg = "Errore server (HTTP " + response.code() + ").";
                            else msg = "Accesso fallito (HTTP " + response.code() + ").";
                            Utility.mostraDialog("Accesso Mercury", msg, LoginActivity.this, "OK");
                        }
                    }

                    @Override
                    public void onFailure(Call<MercuryApiService.LoginResponse> call, Throwable t) {
                        v.setEnabled(true);
                        if (isFinishing() || isDestroyed()) return;
                        // server non raggiungibile: se si puo', si entra offline con la stessa password
                        if (offlineConsentito() && email.equalsIgnoreCase(com.ncfsistemi.econ.api.ProfiloOffline.email(LoginActivity.this))) {
                            impostaModoOffline(true);
                            accediOffline(v, email, password);
                            return;
                        }
                        String motivo = com.ncfsistemi.econ.api.ProfiloOffline.motivoNonDisponibile(LoginActivity.this);
                        Utility.mostraDialog("Server non raggiungibile",
                                "Non è possibile collegarsi al server (" + t.getMessage() + ")."
                                        + (motivo != null ? "\n\n" + motivo : ""), LoginActivity.this, "OK");
                    }
                });
        System.out.println("Econ: LoginActivity accediMercury EXIT");
    }

    @Override
    protected boolean isControllaRegistrazione() {
        return false;
    }

    @Override
    protected boolean isControllaLogin() {
        return false;
    }

    /** Apre la schermata di configurazione del server Mercury */
    public void apriImpostazioniServer(View v) {
        startActivity(new Intent(this, com.ncfsistemi.econ.server.ConfigurazioneGenActivity.class));
    }

    /**
     * Non c'e' una registrazione self-service: l'account lo crea l'amministratore della ditta oppure l'assistenza
     * dell'editore (utils.Editore), che si puo' contattare da qui.
     */
    public void registrati(View v) {
        String telefono = com.ncfsistemi.econ.utils.Editore.telefonoAssistenza(this);
        String testo = "L'account lo crea l'amministratore della tua ditta. Per un nuovo accesso o per attivare Econ "
                + "contatta l'assistenza:\n\n" + com.ncfsistemi.econ.utils.Editore.nome(this)
                + "\n" + com.ncfsistemi.econ.utils.Editore.emailAssistenza(this)
                + (telefono.isEmpty() ? "" : "\n" + telefono);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Registrati")
                .setMessage(testo)
                .setPositiveButton("Scrivi", (d, w) -> com.ncfsistemi.econ.utils.Editore.scriviAssistenza(this, "Richiesta di accesso a Econ", ""))
                .setNegativeButton(R.string.chiudi, null)
                .show();
    }

    /** Avvia il flusso di recupero password Mercury (step 1: inserisci email). */
    public void recuperoPasswordMercury(View v) {
        SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        if (pref.getString("URL", "").isEmpty()) {
            Utility.mostraDialog("Server non configurato",
                    "Configura l'indirizzo del server Mercury prima di procedere.",
                    this, "OK");
            return;
        }

        EditText etEmail = new EditText(this);
        etEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setHint("Email");
        String emailSalvata = pref.getString("MERCURY_EMAIL", "");
        if (!emailSalvata.isEmpty()) {
            etEmail.setText(emailSalvata);
        }

        new AlertDialog.Builder(this)
                .setTitle("Recupero password Mercury")
                .setMessage("Inserisci la tua email: riceverai per email un codice valido un'ora.")
                .setView(etEmail)
                .setPositiveButton("Richiedi codice", (dialog, which) -> {
                    String email = etEmail.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Inserisci un'email valida", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    inviaRichiestaResetPassword(email);
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void inviaRichiestaResetPassword(String email) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Richiesta in corso...");
        progress.setCancelable(false);
        progress.show();

        MercuryApiService api = MercuryApiClient.getInstance(this).getService();
        api.forgotPassword(new MercuryApiService.ForgotPasswordRequest(email))
                .enqueue(new retrofit2.Callback<MercuryApiService.ForgotPasswordResponse>() {
                    @Override
                    public void onResponse(Call<MercuryApiService.ForgotPasswordResponse> call,
                                           retrofit2.Response<MercuryApiService.ForgotPasswordResponse> response) {
                        progress.dismiss();
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            // Il server non dice se l'email esiste e non restituisce il codice: arriva per email
                            mostraDialogNuovaPassword(email);
                        } else {
                            Utility.mostraDialog("Recupero password",
                                    "Errore dal server (HTTP " + response.code() + ").", LoginActivity.this, "OK");
                        }
                    }

                    @Override
                    public void onFailure(Call<MercuryApiService.ForgotPasswordResponse> call, Throwable t) {
                        progress.dismiss();
                        if (isFinishing() || isDestroyed()) return;
                        Utility.mostraDialog("Errore di rete", t.getMessage(), LoginActivity.this, "OK");
                    }
                });
    }

    private void mostraDialogNuovaPassword(String email) {
        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp16, dp16, dp16, dp16);

        TextView tvCodice = new TextView(this);
        tvCodice.setText("Se " + email + " è l'email di un account, ti abbiamo inviato un codice (valido 1 ora). "
                + "Controlla anche la posta indesiderata.");
        tvCodice.setPadding(0, 0, 0, dp16);
        layout.addView(tvCodice);

        EditText etCodice = new EditText(this);
        etCodice.setHint("Codice");
        etCodice.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        layout.addView(etCodice);

        EditText etNuovaPassword = new EditText(this);
        etNuovaPassword.setHint("Nuova password");
        etNuovaPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNuovaPassword);

        EditText etConfermaPassword = new EditText(this);
        etConfermaPassword.setHint("Conferma password");
        etConfermaPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfermaPassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Imposta nuova password")
                .setView(layout)
                .setPositiveButton("Conferma", null)
                .setNegativeButton("Annulla", null)
                .create();

        dialog.show();

        // Override sul button positivo per evitare chiusura automatica
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btnView -> {
            String codice = etCodice.getText().toString().trim();
            String nuovaPassword = etNuovaPassword.getText().toString();
            String confermaPassword = etConfermaPassword.getText().toString();

            if (codice.isEmpty()) {
                etCodice.setError("Inserisci il codice");
                return;
            }
            if (nuovaPassword.length() < com.ncfsistemi.econ.utils.CambioPassword.LUNGHEZZA_MINIMA) {
                etNuovaPassword.setError("La password deve essere di almeno "
                        + com.ncfsistemi.econ.utils.CambioPassword.LUNGHEZZA_MINIMA + " caratteri");
                return;
            }
            if (!nuovaPassword.equals(confermaPassword)) {
                etConfermaPassword.setError("Le password non coincidono");
                return;
            }

            btnView.setEnabled(false);
            MercuryApiService api = MercuryApiClient.getInstance(this).getService();
            api.resetPassword(new MercuryApiService.ResetPasswordRequest(email, codice, nuovaPassword))
                    .enqueue(new retrofit2.Callback<MercuryApiService.ResetPasswordResponse>() {
                        @Override
                        public void onResponse(Call<MercuryApiService.ResetPasswordResponse> call,
                                               retrofit2.Response<MercuryApiService.ResetPasswordResponse> response) {
                            if (isFinishing() || isDestroyed()) return;
                            if (response.isSuccessful() && response.body() != null) {
                                dialog.dismiss();
                                Toast.makeText(LoginActivity.this,
                                        "Password aggiornata! Ora effettua il login.",
                                        Toast.LENGTH_LONG).show();
                                EditText editPasswordMercury = findViewById(R.id.editTextPasswordMercury);
                                if (editPasswordMercury != null) {
                                    editPasswordMercury.setText("");
                                }
                            } else {
                                btnView.setEnabled(true);
                                String errMsg = MercuryApiClient.messaggioErrore(response, "Errore dal server");
                                Utility.mostraDialog("Reset password", errMsg, LoginActivity.this, "OK");
                            }
                        }

                        @Override
                        public void onFailure(Call<MercuryApiService.ResetPasswordResponse> call, Throwable t) {
                            if (isFinishing() || isDestroyed()) return;
                            btnView.setEnabled(true);
                            Utility.mostraDialog("Errore di rete", t.getMessage(), LoginActivity.this, "OK");
                        }
                    });
        });
    }

    /**
     * Imposta la sessione in-memory (Sessione.*) a partire dai dati salvati nel TokenManager.
     * Deve essere chiamato dopo ogni login Mercury riuscito e all'avvio con token valido.
     */
    private void setupSessioneFromToken() {
        TokenManager tm = TokenManager.getInstance(this);
        int userId  = tm.getUserId();
        int idDitta = tm.getIdDitta();

        Sessione.setIdOperatore(userId, this);
        Sessione.setDittaSelezionata(idDitta);

        // Preferisce il nome dal DB locale (dopo sync), altrimenti usa quello salvato al login
        String nomeDitta = getNomeDittaFromDb(idDitta);
        if (nomeDitta.isEmpty()) {
            nomeDitta = tm.getNomeDitta();
        }
        Sessione.setNomeDittaSelezionata(nomeDitta);

        String nomeCompleto = ((tm.getNome() == null ? "" : tm.getNome()) + " "
                + (tm.getCognome() == null ? "" : tm.getCognome())).trim();
        if (!nomeCompleto.isEmpty()) {
            Sessione.setNomeOperatore(nomeCompleto);
        }
    }

    /** Legge il nome della ditta dal SQLite locale. Restituisce "" se non ancora sincronizzato. */
    private String getNomeDittaFromDb(int idDitta) {
        try {
            DbInterno db = new DbInterno(this);
            ContentValues where = new ContentValues();
            where.put(com.ncfsistemi.econ.db.table.Ditte.ID_DITTA, idDitta);
            ContentValues rec = db.getRecord(new com.ncfsistemi.econ.db.table.Ditte(), where);
            db.close();
            if (rec != null) {
                return rec.getAsString(com.ncfsistemi.econ.db.table.Ditte.RAGIONE_SOCIALE);
            }
        } catch (Exception ignored) {}
        return "";
    }

    /** Secondi dopo i quali la password mostrata torna nascosta. */
    private static final long PASSWORD_VISIBILE_MS = 10_000;
    private final android.os.Handler handlerPassword = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable nascondiPassword = () -> impostaPasswordVisibile(false);

    /** Occhio accanto alla password: la mostra (per PASSWORD_VISIBILE_MS) o la nasconde. */
    public void mostraNascondiPassword(View v) {
        EditText et = findViewById(R.id.editTextPasswordMercury);
        boolean visibile = (et.getInputType() & InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        impostaPasswordVisibile(!visibile);
    }

    private void impostaPasswordVisibile(boolean visibile) {
        EditText et = findViewById(R.id.editTextPasswordMercury);
        android.widget.ImageButton occhio = findViewById(R.id.buttonMostraPassword);
        handlerPassword.removeCallbacks(nascondiPassword);
        int cursore = et.getSelectionEnd();
        et.setInputType(InputType.TYPE_CLASS_TEXT
                | (visibile ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
        et.setSelection(Math.max(0, Math.min(cursore, et.length())));
        occhio.setImageResource(visibile ? R.drawable.ic_password_nascondi : R.drawable.ic_password_mostra);
        occhio.setContentDescription(visibile ? "Nascondi la password" : "Mostra la password");
        if (visibile) handlerPassword.postDelayed(nascondiPassword, PASSWORD_VISIBILE_MS);
    }

    @Override
    protected void onPause() {
        impostaPasswordVisibile(false);
        super.onPause();
    }

    /** Login offline consentito: non per un nuovo accesso richiesto da un'altra schermata (serve il server). */
    private boolean offlineConsentito() {
        return !getIntent().getBooleanExtra(EXTRA_RETURN_AFTER_LOGIN, false)
                && com.ncfsistemi.econ.api.ProfiloOffline.disponibile(this);
    }

    /** Server non raggiungibile con profilo offline: email fissa, "Accedi offline", niente "Ricordami". */
    private void impostaModoOffline(boolean attivo) {
        modoOffline = attivo;
        EditText editEmail = findViewById(R.id.editTextEmail);
        android.widget.Button pulsante = findViewById(R.id.button_login_mercury);
        if (attivo) {
            editEmail.setText(com.ncfsistemi.econ.api.ProfiloOffline.email(this));
            editEmail.setEnabled(false);
            pulsante.setText("Accedi offline");
            checkBoxRicordamiMercury.setVisibility(View.GONE);
            findViewById(R.id.testoRicordamiOffline).setVisibility(View.GONE);
            testoStatoConnessione.setText("Server non raggiungibile: puoi accedere offline");
        } else {
            editEmail.setEnabled(true);
            pulsante.setText("Accedi");
            checkBoxRicordamiMercury.setVisibility(View.VISIBLE);
            findViewById(R.id.testoRicordamiOffline).setVisibility(View.VISIBLE);
        }
    }

    /** Password controllata sul tablet (ProfiloOffline); se giusta si entra nel menu offline. */
    private void accediOffline(View v, String email, String password) {
        v.setEnabled(false);
        new Thread(() -> {
            boolean ok = com.ncfsistemi.econ.api.ProfiloOffline.verificaPassword(this, email, password);
            runOnUiThread(() -> {
                v.setEnabled(true);
                if (isFinishing() || isDestroyed()) return;
                if (ok) {
                    com.ncfsistemi.econ.utils.AccessoOffline.entra(this);
                    finish();
                    return;
                }
                int rimasti = com.ncfsistemi.econ.api.ProfiloOffline.tentativiRimasti(this);
                if (rimasti > 0) {
                    ((EditText) findViewById(R.id.editTextPasswordMercury)).setError("Password errata (tentativi rimasti: " + rimasti + ")");
                } else {
                    impostaModoOffline(false);
                    Utility.mostraDialog("Accesso offline", com.ncfsistemi.econ.api.ProfiloOffline.motivoNonDisponibile(this), this, "OK");
                }
            });
        }, "accesso-offline").start();
    }

    /** Costante per indicare che dopo il login si deve tornare all'activity chiamante. */
    public static final String EXTRA_RETURN_AFTER_LOGIN = "return_after_login";

    /**
     * Dopo l'accesso: allineamento con il server (moduli, pacchetti, invio e scarico dati) che poi porta
     * al menu; oppure torna all'activity chiamante se richiesto.
     */
    private void avanzaAlMenu() {
        if (getIntent().getBooleanExtra(EXTRA_RETURN_AFTER_LOGIN, false)) {
            setResult(android.app.Activity.RESULT_OK);
            finish();
            return;
        }
        com.ncfsistemi.econ.utils.AccessoMercury.apriAllineamento(this);
    }
}
