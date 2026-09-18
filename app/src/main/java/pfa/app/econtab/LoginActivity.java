package pfa.app.econtab;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pfa.app.econtab.api.MercuryApiClient;
import pfa.app.econtab.api.MercuryApiService;
import pfa.app.econtab.api.TokenManager;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Utenti;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabSpinner;
import retrofit2.Call;
import retrofit2.Callback;

public class LoginActivity extends EConTabActivity implements TextWatcher {

    private EConTabSpinner spinnerutenti = null;
    private CheckBox ricordaPassword = null;
    private EditText editPassword = null;
    private View indicatoreConnessione = null;
    private TextView testoStatoConnessione = null;
    private TextView testoVersioneApp = null;
    private CheckBox checkBoxRicordamiMercury = null;
    private String versioneAppTesto = "";
    private String versioneMercuryTesto = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("EConTab: LoginActivity onCreate ENTER");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ricordaPassword = (CheckBox) findViewById(R.id.checkBoxRicordaPassword);
        editPassword = (EditText) findViewById(R.id.editTextPassword);
        spinnerutenti = (EConTabSpinner) findViewById(R.id.econtabSpinnerUtenti);
        indicatoreConnessione = findViewById(R.id.indicatoreConnessione);
        testoStatoConnessione = (TextView) findViewById(R.id.testoStatoConnessione);
        testoVersioneApp = (TextView) findViewById(R.id.testoVersioneApp);
        checkBoxRicordamiMercury = (CheckBox) findViewById(R.id.checkBoxRicordamiMercury);

        spinnerutenti.addTextChangeListener(this);

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

        System.out.println("EConTab: LoginActivity onCreate EXIT");
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("EConTab: LoginActivity onResume ENTER");

        // Auto-login solo se NON siamo stati aperti per fare un nuovo login esplicito
        boolean returnAfterLogin = getIntent().getBooleanExtra(EXTRA_RETURN_AFTER_LOGIN, false);
        if (!returnAfterLogin && isTokenValid()) {
            setupSessioneFromToken();
            avanzaAlMenu();
            return;
        }

        DbInterno db = new DbInterno(this);
        ContentValues where = new ContentValues();
        ArrayList<Object> utenti = db.eseguiSelect(new Utenti(), where, new String[]{Utenti.NOME, Utenti.COGNOME});
        db.close();

        ArrayList<Object> valori = new ArrayList<>();
        for (int i = 0; i < utenti.size(); i++) {
            ContentValues curr = (ContentValues) utenti.get(i);
            String pwd = curr.getAsString(Utenti.PASSWORD);
            if (pwd != null && !pwd.isEmpty()) {
                ContentValues val = new ContentValues();
                val.put(EConTabSpinner.VALORE, curr.getAsInteger(Utenti.ID_UTENTE));
                val.put(EConTabSpinner.DESCRIZIONE, curr.getAsString(Utenti.NOME) + " " + curr.getAsString(Utenti.COGNOME));
                valori.add(val);
            }
        }

        // Mostra la sezione login locale solo se ci sono utenti locali con password
        boolean hasLocalUsers = !valori.isEmpty();
        int localVisibility = hasLocalUsers ? View.VISIBLE : View.GONE;
        findViewById(R.id.separatoreLogin).setVisibility(localVisibility);
        findViewById(R.id.textViewAccount).setVisibility(localVisibility);
        findViewById(R.id.econtabSpinnerUtenti).setVisibility(localVisibility);
        findViewById(R.id.editTextPassword).setVisibility(localVisibility);
        findViewById(R.id.checkBoxRicordaPassword).setVisibility(localVisibility);
        findViewById(R.id.button_login).setVisibility(localVisibility);

        if (hasLocalUsers) {
            ContentValues valVuoto = new ContentValues();
            valVuoto.put(EConTabSpinner.VALORE, 0);
            valVuoto.put(EConTabSpinner.DESCRIZIONE, "");
            valori.add(0, valVuoto);
            spinnerutenti.setValue("0");
            spinnerutenti.setValoriSpinnerLibero(valori);
        }

        // Verifica connessione Mercury in background
        verificaConnessione();

        System.out.println("EConTab: LoginActivity onResume EXIT");
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
        try {
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versioneAppTesto = "App v" + pinfo.versionName;
        } catch (Exception ignored) {
            versioneAppTesto = "";
        }
        versioneMercuryTesto = "";
        aggiornaTestoVersioni();

        SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        if (pref.getString("URL", "").isEmpty()) {
            return;
        }

        MercuryApiService api = MercuryApiClient.getInstance(this).getService();

        api.getVersioneApp(pfa.app.econtab.BuildConfig.GIT_COMMIT).enqueue(new Callback<MercuryApiService.VersionResponse>() {
            @Override
            public void onResponse(Call<MercuryApiService.VersionResponse> call,
                                   retrofit2.Response<MercuryApiService.VersionResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && response.body().versione != null) {
                    versioneAppTesto = "App v" + response.body().versione;
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

        String pingUrl = (url.endsWith("/") ? url : url + "/") + "api/auth/ditte";
        new Thread(() -> {
            int code = -1;
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS)
                        .build();
                Request req = new Request.Builder().url(pingUrl).build();
                try (Response resp = client.newCall(req).execute()) {
                    code = resp.code();
                }
            } catch (Exception ignored) {
            }

            final boolean raggiungibile = code > 0;
            final String urlDisplay = url;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (raggiungibile) {
                    indicatoreConnessione.setBackgroundColor(Color.parseColor("#4CAF50"));
                    testoStatoConnessione.setText("Connesso: " + urlDisplay);
                } else {
                    indicatoreConnessione.setBackgroundColor(Color.parseColor("#F44336"));
                    testoStatoConnessione.setText("Server non raggiungibile");
                }
            });
        }).start();
    }

    /** Login con credenziali Mercury (JWT). */
    public void accediMercury(View v) {
        System.out.println("EConTab: LoginActivity accediMercury ENTER");

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

                            // "Ricordami": salva o cancella email+password cifrate a seconda della
                            // checkbox. Restano finché non si fa logout (TokenManager.clearToken()).
                            if (checkBoxRicordamiMercury.isChecked()) {
                                tm.saveCredenzialiRicordami(email, password);
                            } else {
                                tm.clearCredenzialiRicordami();
                            }

                            // Salva email + segna terminale come attivato (il JWT Mercury è l'attivazione)
                            getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("MERCURY_EMAIL", email)
                                    .putString(pfa.app.econtab.utils.Sessione.CODICE_ATTIVAZIONE, body.token)
                                    .apply();

                            // Imposta la sessione in-memory usata dal resto dell'app
                            setupSessioneFromToken();

                            avanzaAlMenu();
                        } else {
                            String msg;
                            if (response.code() == 401) msg = "Email o password errata.";
                            else if (response.code() == 403) msg = "Licenza scaduta o non abilitata.";
                            else if (response.code() >= 500) msg = "Errore server (HTTP " + response.code() + ").";
                            else msg = "Accesso fallito (HTTP " + response.code() + ").";
                            Utility.mostraDialog("Accesso Mercury", msg, LoginActivity.this, "OK");
                        }
                    }

                    @Override
                    public void onFailure(Call<MercuryApiService.LoginResponse> call, Throwable t) {
                        v.setEnabled(true);
                        if (isFinishing() || isDestroyed()) return;
                        Utility.mostraDialog("Errore di rete", t.getMessage(), LoginActivity.this, "OK");
                    }
                });
        System.out.println("EConTab: LoginActivity accediMercury EXIT");
    }

    @Override
    protected boolean isControllaRegistrazione() {
        return false;
    }

    @Override
    protected boolean isControllaLogin() {
        return false;
    }

    public void accedi(View v) {
        System.out.println("EConTab: LoginActivity accedi ENTER");
        int idUtenteSel = Integer.parseInt(spinnerutenti.getValue());
        if (idUtenteSel == 0) {
            spinnerutenti.setError(getString(R.string.seleziona_utente));
        } else {
            String password = getTesto(R.id.editTextPassword);
            DbInterno db = new DbInterno(this);
            ContentValues where = new ContentValues();
            where.put(Utenti.ID_UTENTE, idUtenteSel);
            ContentValues recUt = db.getRecord(new Utenti(), where);
            db.close();

            if (recUt != null && recUt.getAsString(Utenti.PASSWORD).equals(password)) {
                db = new DbInterno(this);
                Utenti tabUt = new Utenti();
                ContentValues valUpd = tabUt.getValoriLogModifica(db);
                valUpd.put(Utenti.RICORDA_PASSWORD, ricordaPassword.isChecked() ? 1 : 0);
                tabUt.aggiornaRecord(db, valUpd, where);
                db.close();

                Sessione.setIdOperatore(idUtenteSel, this);

                Intent intent = new Intent(this, MenuActivity.class);
                startActivity(intent);
            } else {
                editPassword.setError(getString(R.string.password_errata));
            }
        }
        System.out.println("EConTab: LoginActivity accedi EXIT");
    }

    @Override
    public void afterTextChanged(Editable arg0) {
        System.out.println("EConTab: LoginActivity afterTextChanged ENTER");
        setText(R.id.editTextPassword, "");
        ricordaPassword.setChecked(false);
        int idUtenteSel = Integer.parseInt(spinnerutenti.getValue());
        if (idUtenteSel > 0) {
            DbInterno db = new DbInterno(this);
            ContentValues where = new ContentValues();
            where.put(Utenti.ID_UTENTE, idUtenteSel);
            ContentValues recUt = db.getRecord(new Utenti(), where);
            db.close();

            if (recUt != null && recUt.getAsInteger(Utenti.RICORDA_PASSWORD) == 1) {
                setText(R.id.editTextPassword, recUt.getAsString(Utenti.PASSWORD));
                ricordaPassword.setChecked(true);
            }
        }
        System.out.println("EConTab: LoginActivity afterTextChanged EXIT");
    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

    /** Apre la schermata di configurazione del server Mercury */
    public void apriImpostazioniServer(View v) {
        startActivity(new Intent(this, pfa.app.econtab.server.ConfigurazioneGenActivity.class));
    }

    /**
     * Placeholder: non esiste ancora un flusso di registrazione self-service lato
     * Mercury. Per ora indirizza l'utente a chi gestisce gli accessi.
     */
    public void registrati(View v) {
        Utility.mostraDialog("Registrati",
                "La registrazione non è ancora disponibile da qui: contatta l'amministratore di Mercury per farti creare un account.",
                this, "OK");
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
                .setMessage("Inserisci la tua email per ricevere il codice temporaneo.")
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
                            MercuryApiService.ForgotPasswordResponse body = response.body();
                            mostraDialogNuovaPassword(email, body.resetCode);
                        } else if (response.code() == 404) {
                            Utility.mostraDialog("Recupero password", "Email non trovata.", LoginActivity.this, "OK");
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

    private void mostraDialogNuovaPassword(String email, String codiceRicevuto) {
        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp16, dp16, dp16, dp16);

        TextView tvCodice = new TextView(this);
        tvCodice.setText("Il tuo codice temporaneo è: " + codiceRicevuto + "\n(valido 1 ora)");
        tvCodice.setTypeface(tvCodice.getTypeface(), android.graphics.Typeface.BOLD);
        tvCodice.setPadding(0, 0, 0, dp16);
        layout.addView(tvCodice);

        EditText etCodice = new EditText(this);
        etCodice.setHint("Codice");
        etCodice.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        etCodice.setText(codiceRicevuto);
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
            if (nuovaPassword.length() < 6) {
                etNuovaPassword.setError("La password deve essere di almeno 6 caratteri");
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
                                String errMsg = "Errore dal server (HTTP " + response.code() + ").";
                                if (response.body() != null && response.body().error != null) {
                                    errMsg = response.body().error;
                                }
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
            where.put(pfa.app.econtab.db.table.Ditte.ID_DITTA, idDitta);
            ContentValues rec = db.getRecord(new pfa.app.econtab.db.table.Ditte(), where);
            db.close();
            if (rec != null) {
                return rec.getAsString(pfa.app.econtab.db.table.Ditte.RAGIONE_SOCIALE);
            }
        } catch (Exception ignored) {}
        return "";
    }

    /**
     * Verifica se il JWT token presente nel TokenManager è ancora valido (non scaduto).
     * Decodifica il payload senza network call.
     */
    private boolean isTokenValid() {
        String token = TokenManager.getInstance(this).getToken();
        if (token == null) return false;
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;
            byte[] decoded = Base64.decode(
                    parts[1].replace('-', '+').replace('_', '/'),
                    Base64.DEFAULT
            );
            JSONObject payload = new JSONObject(new String(decoded, "UTF-8"));
            long exp = payload.optLong("exp", 0);
            return exp > System.currentTimeMillis() / 1000L;
        } catch (Exception e) {
            return false;
        }
    }

    /** Costante per indicare che dopo il login si deve tornare all'activity chiamante. */
    public static final String EXTRA_RETURN_AFTER_LOGIN = "return_after_login";

    /** Naviga al menu principale, oppure torna all'activity chiamante se richiesto. */
    private void avanzaAlMenu() {
        if (getIntent().getBooleanExtra(EXTRA_RETURN_AFTER_LOGIN, false)) {
            setResult(android.app.Activity.RESULT_OK);
            finish();
            return;
        }
        Intent intent = new Intent(this, MenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
