package pfa.app.econtab;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pfa.app.econtab.api.MercuryApiClient;
import pfa.app.econtab.api.MercuryApiService;
import pfa.app.econtab.api.TokenManager;
import pfa.app.econtab.utils.Licenza;
import pfa.app.econtab.utils.Utility;

public class StatoSistemaActivity extends AppCompatActivity {

    private static final int REQUEST_LOGIN = 1001;

    private View      dotServer, dotServerUrl, dotAuth, dotSync, dotAttivazione;
    private TextView  tvServerUrl, tvServerStato;
    private TextView  tvAuthStato, tvAuthUtente, tvAuthDitta, tvAuthScadenza;
    private TextView  tvSyncData;
    private TextView  tvAttivazioneStato, tvAttivazioneCodice;
    private TextView  tvDeviceId, tvAppVersion, tvAndroid;
    private Button    btnRilogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stato_sistema);

        dotServerUrl      = findViewById(R.id.dotServerUrl);
        dotServer         = findViewById(R.id.dotServer);
        tvServerUrl       = findViewById(R.id.tvServerUrl);
        tvServerStato     = findViewById(R.id.tvServerStato);

        dotAuth           = findViewById(R.id.dotAuth);
        tvAuthStato       = findViewById(R.id.tvAuthStato);
        tvAuthUtente      = findViewById(R.id.tvAuthUtente);
        tvAuthDitta       = findViewById(R.id.tvAuthDitta);
        tvAuthScadenza    = findViewById(R.id.tvAuthScadenza);
        btnRilogin        = findViewById(R.id.btnRilogin);

        dotSync           = findViewById(R.id.dotSync);
        tvSyncData        = findViewById(R.id.tvSyncData);

        dotAttivazione    = findViewById(R.id.dotAttivazione);
        tvAttivazioneStato   = findViewById(R.id.tvAttivazioneStato);
        tvAttivazioneCodice  = findViewById(R.id.tvAttivazioneCodice);

        tvDeviceId        = findViewById(R.id.tvDeviceId);
        tvAppVersion      = findViewById(R.id.tvAppVersion);
        tvAndroid         = findViewById(R.id.tvAndroid);

        eseguiVerifica(null);
    }

    /** Apre il login Mercury e torna a questa schermata al completamento. */
    public void apriLogin(View v) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.EXTRA_RETURN_AFTER_LOGIN, true);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOGIN && resultCode == Activity.RESULT_OK) {
            btnRilogin.setVisibility(View.GONE);
            eseguiVerifica(null);
        }
    }

    /** Chiamato dal bottone "Aggiorna" e dall'avvio. */
    public void eseguiVerifica(View v) {
        popolaDatiLocali();
        verificaServerAsync();
    }

    public void chiudi(View v) {
        finish();
    }

    // ── Dati locali (sincroni) ───────────────────────────────────────────────

    private void popolaDatiLocali() {
        SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        TokenManager tm = TokenManager.getInstance(this);

        // URL server
        String url = pref.getString("URL", "");
        if (url.isEmpty()) {
            tvServerUrl.setText("Non configurato");
            dotServerUrl.setBackgroundColor(Color.parseColor("#9E9E9E"));
        } else {
            tvServerUrl.setText(url);
            dotServerUrl.setBackgroundColor(Color.parseColor("#4CAF50"));
        }

        // Reset ping indicator mentre aspettiamo
        dotServer.setBackgroundColor(Color.parseColor("#FFC107"));
        tvServerStato.setText("Verifica in corso...");

        // Autenticazione Mercury
        String token = tm.getToken();
        if (token == null) {
            dotAuth.setBackgroundColor(Color.parseColor("#F44336"));
            tvAuthStato.setText("Non autenticato");
            tvAuthUtente.setText("—");
            tvAuthDitta.setText("—");
            tvAuthScadenza.setText("—");
        } else {
            TokenExpiry expiry = decodeTokenExpiry(token);
            if (expiry.isValid) {
                dotAuth.setBackgroundColor(Color.parseColor("#4CAF50"));
                tvAuthStato.setText("Autenticato");
            } else {
                dotAuth.setBackgroundColor(Color.parseColor("#FF9800"));
                tvAuthStato.setText("Token scaduto — effettua nuovamente il login");
            }
            String nome = trim(tm.getNome()) + " " + trim(tm.getCognome());
            tvAuthUtente.setText(nome.trim().isEmpty() ? "—" : nome.trim());
            tvAuthDitta.setText(popolaNomeDitta(pref, tm.getIdDitta()));
            tvAuthScadenza.setText(expiry.label);
        }

        // Ultima sincronizzazione
        String datSync = pref.getString("DATA_ULTIMA_SINCRONIZZAZIONE", "");
        if (datSync.isEmpty() || datSync.equals("19700101000000")) {
            dotSync.setBackgroundColor(Color.parseColor("#9E9E9E"));
            tvSyncData.setText("Mai sincronizzato");
        } else {
            dotSync.setBackgroundColor(Color.parseColor("#4CAF50"));
            tvSyncData.setText(formattaDataSync(datSync));
        }

        // Attivazione terminale (sistema legacy)
        String codReg  = pref.getString("ECONTAB_REG", "");
        String codFull = pref.getString("ECONTAB_CODICE_FULL", "");
        boolean attivato = !codReg.isEmpty() || !codFull.isEmpty();
        // Con Mercury il JWT è l'attivazione principale; il codice legacy è opzionale
        if (token != null && decodeTokenExpiry(token).isValid) {
            dotAttivazione.setBackgroundColor(Color.parseColor("#4CAF50"));
            tvAttivazioneStato.setText("Operativo (Mercury)");
        } else if (attivato) {
            dotAttivazione.setBackgroundColor(Color.parseColor("#4CAF50"));
            tvAttivazioneStato.setText("Attivato (codice locale)");
        } else {
            dotAttivazione.setBackgroundColor(Color.parseColor("#F44336"));
            tvAttivazioneStato.setText("Non attivato — effettua il login Mercury");
        }
        tvAttivazioneCodice.setText(codReg.isEmpty() ? "—" : mascheraCodice(codReg));

        // Dispositivo
        tvDeviceId.setText(Licenza.getIDdispositivo(this));
        tvAndroid.setText("Android " + Build.VERSION.RELEASE + "  (API " + Build.VERSION.SDK_INT + ")  —  " + Build.MODEL);
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvAppVersion.setText("v" + pi.versionName + " (build " + pi.versionCode + ")");
        } catch (Exception e) {
            tvAppVersion.setText("—");
        }
    }

    // ── Ping server + verifica JWT (asincrono) ──────────────────────────────

    private void verificaServerAsync() {
        SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        String url = pref.getString("URL", "");

        if (url.isEmpty()) {
            dotServer.setBackgroundColor(Color.parseColor("#9E9E9E"));
            tvServerStato.setText("Server non configurato");
            return;
        }

        TokenManager tm = TokenManager.getInstance(this);
        String token = tm.getToken();

        String pingUrl = (url.endsWith("/") ? url : url + "/") + "api/auth/ditte";
        new Thread(() -> {
            // ── 1. Ping non autenticato per verificare raggiungibilità ──────
            long start = System.currentTimeMillis();
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
            } catch (Exception ignored) {}
            long ms = System.currentTimeMillis() - start;
            final int finalCode = code;
            final long finalMs = ms;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (finalCode > 0) {
                    dotServer.setBackgroundColor(Color.parseColor("#4CAF50"));
                    tvServerStato.setText("Online  •  HTTP " + finalCode + "  •  " + finalMs + " ms");
                } else {
                    dotServer.setBackgroundColor(Color.parseColor("#F44336"));
                    tvServerStato.setText("Non raggiungibile");
                }
            });

            // ── 2. Verifica JWT con chiamata autenticata a Mercury ──────────
            if (token != null && finalCode > 0) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    dotAuth.setBackgroundColor(Color.parseColor("#FFC107"));
                    tvAuthStato.setText("Verifica token con Mercury...");
                });
                try {
                    MercuryApiService api = MercuryApiClient.getInstance(StatoSistemaActivity.this).getService();
                    retrofit2.Response<java.util.List<MercuryApiService.DittaInfo>> apiResp =
                            api.getDitte().execute();
                    final int apiCode = apiResp.code();
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (apiCode == 200) {
                            dotAuth.setBackgroundColor(Color.parseColor("#4CAF50"));
                            tvAuthStato.setText("Autenticato  •  Token verificato con Mercury ✓");
                            btnRilogin.setVisibility(View.GONE);
                        } else if (apiCode == 401 || apiCode == 403) {
                            dotAuth.setBackgroundColor(Color.parseColor("#F44336"));
                            tvAuthStato.setText("Token non accettato da Mercury (HTTP " + apiCode + ")");
                            btnRilogin.setVisibility(View.VISIBLE);
                        } else {
                            dotAuth.setBackgroundColor(Color.parseColor("#FF9800"));
                            tvAuthStato.setText("Risposta Mercury inattesa: HTTP " + apiCode);
                            btnRilogin.setVisibility(View.VISIBLE);
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        TokenExpiry expiry = decodeTokenExpiry(token);
                        if (expiry.isValid) {
                            dotAuth.setBackgroundColor(Color.parseColor("#4CAF50"));
                            tvAuthStato.setText("Autenticato  (impossibile verificare con Mercury)");
                            btnRilogin.setVisibility(View.GONE);
                        } else {
                            dotAuth.setBackgroundColor(Color.parseColor("#FF9800"));
                            tvAuthStato.setText("Token scaduto");
                            btnRilogin.setVisibility(View.VISIBLE);
                        }
                    });
                }
            } else if (token == null) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnRilogin.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private String popolaNomeDitta(SharedPreferences pref, int idDitta) {
        // Prova dal DB locale (disponibile dopo almeno una sync)
        try {
            pfa.app.econtab.db.DbInterno db = new pfa.app.econtab.db.DbInterno(this);
            android.content.ContentValues where = new android.content.ContentValues();
            where.put(pfa.app.econtab.db.table.Ditte.ID_DITTA, idDitta);
            android.content.ContentValues rec = db.getRecord(new pfa.app.econtab.db.table.Ditte(), where);
            db.close();
            if (rec != null) {
                String nome = rec.getAsString(pfa.app.econtab.db.table.Ditte.RAGIONE_SOCIALE);
                return (nome != null && !nome.isEmpty()) ? nome : "Ditta ID " + idDitta;
            }
        } catch (Exception ignored) {}
        return idDitta > 0 ? "Ditta ID " + idDitta : "—";
    }

    private static class TokenExpiry {
        boolean isValid;
        String  label;
    }

    private TokenExpiry decodeTokenExpiry(String token) {
        TokenExpiry result = new TokenExpiry();
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) { result.label = "Token non valido"; return result; }
            byte[] decoded = Base64.decode(
                    parts[1].replace('-', '+').replace('_', '/'), Base64.DEFAULT);
            JSONObject payload = new JSONObject(new String(decoded, "UTF-8"));
            long exp = payload.optLong("exp", 0);
            long nowSec = System.currentTimeMillis() / 1000L;
            result.isValid = exp > nowSec;
            if (exp > 0) {
                Date expDate = new Date(exp * 1000L);
                String formatted = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(expDate);
                result.label = result.isValid
                        ? formatted + " (valido)"
                        : formatted + " (SCADUTO)";
            } else {
                result.label = "Nessuna scadenza";
                result.isValid = true;
            }
        } catch (Exception e) {
            result.label = "Errore lettura token";
        }
        return result;
    }

    private String formattaDataSync(String raw) {
        // Supporta sia ISO 8601 (2024-05-17T10:30:00) sia vecchio formato yyyyMMddHHmmss
        try {
            if (raw.length() == 14 && !raw.contains("T")) {
                raw = raw.substring(0, 4) + "-" + raw.substring(4, 6) + "-" + raw.substring(6, 8)
                        + "T" + raw.substring(8, 10) + ":" + raw.substring(10, 12) + ":" + raw.substring(12, 14);
            }
            Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ITALY).parse(raw);
            if (d == null) return raw;
            long diffH = (System.currentTimeMillis() - d.getTime()) / 3_600_000L;
            String label = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(d);
            if (diffH < 1)       return label + " (meno di 1 ora fa)";
            if (diffH < 24)      return label + " (" + diffH + " ore fa)";
            return label + " (" + (diffH / 24) + " giorni fa)";
        } catch (Exception e) {
            return raw;
        }
    }

    private String mascheraCodice(String codice) {
        if (codice.length() <= 4) return "****";
        return codice.substring(0, 2) + "****" + codice.substring(codice.length() - 2);
    }

    private String trim(String s) {
        return (s == null) ? "" : s.trim();
    }
}
