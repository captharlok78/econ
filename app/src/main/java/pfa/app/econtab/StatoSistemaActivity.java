package pfa.app.econtab;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import pfa.app.econtab.api.MercuryApiClient;
import pfa.app.econtab.api.MercuryApiService;
import pfa.app.econtab.api.TokenManager;
import pfa.app.econtab.utils.DatiDispositivo;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.utils.VersioneApp;

/**
 * Stato sistema (STATO_SISTEMA_APP.md): server, sessione, sincronizzazione, terminale come lo vede il server e dati del
 * dispositivo (anche dimensione del database e spazio libero). Ad ogni apertura e "Aggiorna" i dati del dispositivo
 * vanno al server (Sonata › Terminali). Il token si rinnova da solo (api.RinnovoToken) se c'e' "Ricordami".
 */
public class StatoSistemaActivity extends AppCompatActivity {

    private static final int REQUEST_LOGIN = 1001;
    private static final String VERDE = "#4CAF50", GIALLO = "#FFC107", ARANCIO = "#FF9800", ROSSO = "#F44336", GRIGIO = "#9E9E9E";

    private View      dotServer, dotServerUrl, dotAuth, dotSync, dotAttivazione;
    private TextView  tvServerUrl, tvServerStato;
    private TextView  tvAuthStato, tvAuthUtente, tvAuthDitta, tvAuthScadenza;
    private TextView  tvSyncData;
    private TextView  tvAttivazioneStato, tvAttivazioneCodice;
    private LinearLayout boxDispositivo;
    private Button    btnRilogin;

    /** Versioni lette dal server (app di questa build e server), per la riga "Versioni". */
    private String versioneServer = null;

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

        dotAttivazione      = findViewById(R.id.dotAttivazione);
        tvAttivazioneStato  = findViewById(R.id.tvAttivazioneStato);
        tvAttivazioneCodice = findViewById(R.id.tvAttivazioneCodice);

        boxDispositivo    = findViewById(R.id.boxDispositivo);

        versioneServer = VersioneApp.server(this);
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

        String url = pref.getString("URL", "");
        if (url.isEmpty()) {
            tvServerUrl.setText("Non configurato");
            colore(dotServerUrl, GRIGIO);
        } else {
            tvServerUrl.setText(url);
            colore(dotServerUrl, VERDE);
        }
        colore(dotServer, GIALLO);
        tvServerStato.setText("Verifica in corso...");

        String token = tm.getToken();
        if (token == null) {
            colore(dotAuth, ROSSO);
            tvAuthStato.setText("Non autenticato");
            tvAuthUtente.setText("—");
            tvAuthDitta.setText("—");
            tvAuthScadenza.setText("—");
        } else {
            mostraSessione(tm);
            colore(dotAuth, GIALLO);
            tvAuthStato.setText("Verifica in corso...");
        }

        // l'app salva l'ora UTC con il fuso ("2026-10-01T09:20:45+00:00"): si mostra nell'ora del tablet
        String datSync = pref.getString("DATA_ULTIMA_SINCRONIZZAZIONE", "");
        if (datSync.isEmpty() || datSync.equals("19700101000000")) {
            colore(dotSync, GRIGIO);
            tvSyncData.setText("Mai sincronizzato");
        } else {
            colore(dotSync, VERDE);
            tvSyncData.setText(formattaDataSync(datSync));
        }

        colore(dotAttivazione, GIALLO);
        tvAttivazioneStato.setText("Verifica in corso...");
        tvAttivazioneCodice.setText("—");

        mostraDispositivo();
    }

    /** Utente, azienda (nome) e scadenza del token salvati. */
    private void mostraSessione(TokenManager tm) {
        String nome = (trim(tm.getNome()) + " " + trim(tm.getCognome())).trim();
        tvAuthUtente.setText(nome.isEmpty() ? "—" : nome);
        tvAuthDitta.setText(nomeDitta(tm));
        tvAuthScadenza.setText(decodeTokenExpiry(tm.getToken()).label);
    }

    /** Righe del dispositivo: marca e modello, sistema, versioni (app · server), database, spazio, RAM, ecc. */
    private void mostraDispositivo() {
        boxDispositivo.removeAllViews();
        riga("Dispositivo", (DatiDispositivo.marca() + " " + Build.MODEL).trim());
        riga("Sistema", DatiDispositivo.sistema() + (Build.VERSION.SECURITY_PATCH.isEmpty() ? "" : "  ·  patch " + Build.VERSION.SECURITY_PATCH));
        String app = VersioneApp.salvata(this);
        String c = BuildConfig.GIT_COMMIT;
        riga("Versioni", "App " + (app != null ? app : "build " + (c.length() > 7 ? c.substring(0, 7) : c))
                + "  ·  Server " + (versioneServer != null ? versioneServer : "—"));
        try {
            long[] sp = DatiDispositivo.spazio();
            riga("Database dell'app", DatiDispositivo.byte_(DatiDispositivo.dimensioneDatabase(this))
                    + "  (schema " + pfa.app.econtab.db.DbInterno.SCHEMA_VERSION + ")");
            riga("Spazio libero", DatiDispositivo.byte_(sp[0]) + " su " + DatiDispositivo.byte_(sp[1])
                    + String.format(Locale.ITALY, "  (%d%% libero)", sp[1] > 0 ? Math.round(sp[0] * 100.0 / sp[1]) : 0));
            long[] r = DatiDispositivo.ram(this);
            riga("Memoria (RAM)", DatiDispositivo.byte_(r[0]) + " disponibili su " + DatiDispositivo.byte_(r[1]));
            int b = DatiDispositivo.batteria(this);
            if (b >= 0) riga("Batteria", b + "%" + (DatiDispositivo.inCarica(this) ? " (in carica)" : ""));
        } catch (Exception ignored) {
            // dati facoltativi
        }
        riga("Rete", DatiDispositivo.rete(this));
        riga("Schermo", DatiDispositivo.schermo(this));
        TextView id = riga("ID dispositivo", DatiDispositivo.seriale(this));
        id.setTypeface(android.graphics.Typeface.MONOSPACE);
    }

    private TextView riga(String etichetta, String valore) {
        float dp = getResources().getDisplayMetrics().density;
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(Math.round(22 * dp), 0, 0, Math.round(10 * dp));
        TextView e = new TextView(this);
        e.setText(etichetta);
        e.setTextSize(12);
        e.setTextColor(Color.parseColor("#757575"));
        TextView v = new TextView(this);
        v.setText(valore == null || valore.isEmpty() ? "—" : valore);
        v.setTextSize(14);
        v.setTextColor(Color.parseColor("#212121"));
        v.setTextIsSelectable(true);
        r.addView(e);
        r.addView(v);
        boxDispositivo.addView(r);
        return v;
    }

    // ── Server, sessione e terminale (asincrono) ────────────────────────────

    private void verificaServerAsync() {
        SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        if (pref.getString("URL", "").isEmpty()) {
            colore(dotServer, GRIGIO);
            tvServerStato.setText("Server non configurato");
            return;
        }
        TokenManager tm = TokenManager.getInstance(this);

        new Thread(() -> {
            MercuryApiService api = MercuryApiClient.getInstance(StatoSistemaActivity.this).getService();

            // 1. Raggiungibilita' e versione del server (chiamata pubblica)
            long start = System.currentTimeMillis();
            String vServer = null;
            int code = -1;
            try {
                retrofit2.Response<MercuryApiService.VersionResponse> r = api.getVersioneMercury().execute();
                code = r.code();
                if (r.isSuccessful() && r.body() != null) {
                    vServer = r.body().versione;
                    pfa.app.econtab.utils.Editore.salva(this, r.body());
                }
            } catch (Exception ignored) {
            }
            final long ms = System.currentTimeMillis() - start;
            final boolean online = code > 0;
            final String vs = vServer;
            if (vs != null) VersioneApp.salvaServer(this, vs);
            // versione di questa build (registro dei rilasci)
            if (online) {
                try {
                    retrofit2.Response<MercuryApiService.VersionResponse> r = api.getVersioneApp(BuildConfig.GIT_COMMIT).execute();
                    if (r.isSuccessful() && r.body() != null && r.body().versione != null) VersioneApp.salva(this, r.body().versione);
                } catch (Exception ignored) {
                }
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (online) {
                    colore(dotServer, VERDE);
                    tvServerStato.setText("Online  •  " + ms + " ms" + (vs != null ? "  •  Mercury " + vs : ""));
                } else {
                    colore(dotServer, ROSSO);
                    tvServerStato.setText("Non raggiungibile");
                }
                if (vs != null) versioneServer = vs;
                mostraDispositivo();
            });

            if (tm.getToken() == null) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnRilogin.setVisibility(View.VISIBLE);
                    colore(dotAttivazione, GRIGIO);
                    tvAttivazioneStato.setText("Serve l'accesso a Mercury per verificarlo");
                });
                return;
            }
            if (!online) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    TokenExpiry expiry = decodeTokenExpiry(tm.getToken());
                    colore(dotAuth, expiry.isValid ? VERDE : ARANCIO);
                    tvAuthStato.setText(expiry.isValid ? "Autenticato (server non raggiungibile)" : "Token scaduto");
                    colore(dotAttivazione, GRIGIO);
                    tvAttivazioneStato.setText("Server non raggiungibile");
                });
                return;
            }

            // 2. Sessione e terminale con una chiamata autenticata: manda i dati del dispositivo e riceve lo stato del
            //    terminale. Un token scaduto si rinnova da solo (RinnovoToken) se c'e' "Ricordami".
            String tokenPrima = tm.getToken();
            int authCode;
            MercuryApiService.TerminaleResponse terminale = null;
            try {
                retrofit2.Response<MercuryApiService.TerminaleResponse> r = api.inviaTerminale(DatiDispositivo.raccogli(this)).execute();
                authCode = r.code();
                terminale = r.isSuccessful() ? r.body() : null;
            } catch (Exception e) {
                authCode = -1;
            }
            final int ac = authCode;
            final boolean rinnovato = tokenPrima != null && !tokenPrima.equals(tm.getToken());
            final MercuryApiService.TerminaleResponse t = terminale;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                mostraSessione(tm);
                if (ac >= 200 && ac < 300) {
                    colore(dotAuth, VERDE);
                    tvAuthStato.setText(rinnovato ? "Autenticato  •  sessione rinnovata ora ✓" : "Autenticato  •  verificato con Mercury ✓");
                    btnRilogin.setVisibility(View.GONE);
                } else if (ac == 401 || ac == 403) {
                    colore(dotAuth, ROSSO);
                    tvAuthStato.setText(ac == 403 ? "Accesso non consentito: rifai il login" : "Sessione scaduta: rifai il login");
                    btnRilogin.setVisibility(View.VISIBLE);
                } else {
                    colore(dotAuth, ARANCIO);
                    tvAuthStato.setText("Verifica non riuscita" + (ac > 0 ? " (HTTP " + ac + ")" : ""));
                }
                mostraTerminale(t, ac);
            });
        }).start();
    }

    /** Terminale come lo vede il server (Sonata › Terminali). */
    private void mostraTerminale(MercuryApiService.TerminaleResponse t, int code) {
        if (t == null) {
            colore(dotAttivazione, code == 401 || code == 403 ? ROSSO : GRIGIO);
            tvAttivazioneStato.setText(code == 401 || code == 403 ? "Non verificabile senza accesso" : "Non verificato");
            tvAttivazioneCodice.setText("—");
            return;
        }
        if (t.attivo) {
            colore(dotAttivazione, VERDE);
            tvAttivazioneStato.setText("Registrato e attivo  •  dati del dispositivo inviati ✓");
        } else {
            colore(dotAttivazione, ROSSO);
            tvAttivazioneStato.setText("Disattivato sul server: al prossimo accesso il dispositivo sarà bloccato");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(t.descrizione != null && !t.descrizione.isEmpty() ? t.descrizione : "Terminale n. " + t.id);
        if (t.ditta != null) sb.append("  ·  ").append(t.ditta);
        if (t.registrato != null) sb.append("\nRegistrato il ").append(formattaIso(t.registrato));
        tvAttivazioneCodice.setText(sb.toString());
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private static void colore(View dot, String colore) {
        dot.setBackgroundColor(Color.parseColor(colore));
    }

    /** Nome dell'azienda: dal login (TokenManager), altrimenti dal database locale. */
    private String nomeDitta(TokenManager tm) {
        String nome = trim(tm.getNomeDitta());
        if (!nome.isEmpty()) return nome;
        int idDitta = tm.getIdDitta();
        try {
            pfa.app.econtab.db.DbInterno db = new pfa.app.econtab.db.DbInterno(this);
            android.content.ContentValues where = new android.content.ContentValues();
            where.put(pfa.app.econtab.db.table.Ditte.ID_DITTA, idDitta);
            android.content.ContentValues rec = db.getRecord(new pfa.app.econtab.db.table.Ditte(), where);
            db.close();
            if (rec != null && !trim(rec.getAsString(pfa.app.econtab.db.table.Ditte.RAGIONE_SOCIALE)).isEmpty()) {
                return rec.getAsString(pfa.app.econtab.db.table.Ditte.RAGIONE_SOCIALE).trim();
            }
        } catch (Exception ignored) {
        }
        return "—";
    }

    private static class TokenExpiry {
        boolean isValid;
        String  label = "—";
    }

    private TokenExpiry decodeTokenExpiry(String token) {
        TokenExpiry result = new TokenExpiry();
        if (token == null) return result;
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) { result.label = "Token non valido"; return result; }
            byte[] decoded = Base64.decode(parts[1].replace('-', '+').replace('_', '/'), Base64.DEFAULT);
            JSONObject payload = new JSONObject(new String(decoded, "UTF-8"));
            long exp = payload.optLong("exp", 0);
            result.isValid = exp == 0 || exp > System.currentTimeMillis() / 1000L;
            if (exp > 0) {
                String formatted = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(new Date(exp * 1000L));
                result.label = formatted + (result.isValid ? " (si rinnova da solo)" : " (scaduto: si rinnova alla prossima chiamata)");
            } else {
                result.label = "Nessuna scadenza";
            }
        } catch (Exception e) {
            result.label = "Errore lettura token";
        }
        return result;
    }

    /** Data della sync: ISO con fuso (UTC) o vecchio yyyyMMddHHmmss (locale), mostrata nell'ora del tablet. */
    private String formattaDataSync(String raw) {
        try {
            Date d;
            if (raw.length() == 14 && !raw.contains("T")) {
                d = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ITALY).parse(raw);
            } else {
                d = parseIso(raw);
            }
            if (d == null) return raw;
            long diffMin = (System.currentTimeMillis() - d.getTime()) / 60_000L;
            String label = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(d);
            if (diffMin < 1)        return label + " (adesso)";
            if (diffMin < 60)       return label + " (" + diffMin + " min fa)";
            if (diffMin < 24 * 60)  return label + " (" + (diffMin / 60) + (diffMin / 60 == 1 ? " ora fa)" : " ore fa)");
            return label + " (" + (diffMin / (24 * 60)) + " giorni fa)";
        } catch (Exception e) {
            return raw;
        }
    }

    private String formattaIso(String iso) {
        try {
            Date d = parseIso(iso);
            return d != null ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(d) : iso;
        } catch (Exception e) {
            return iso;
        }
    }

    /** "2026-10-01T09:20:45+00:00" (o senza fuso = ora locale). */
    private static Date parseIso(String iso) throws java.text.ParseException {
        String s = iso.trim();
        if (s.endsWith("Z")) s = s.substring(0, s.length() - 1) + "+00:00";
        if (s.length() > 19 && (s.charAt(19) == '+' || s.charAt(19) == '-')) {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ITALY).parse(s.substring(0, 19) + s.substring(19));
        }
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ITALY).parse(s.length() > 19 ? s.substring(0, 19) : s);
    }

    private String trim(String s) {
        return (s == null) ? "" : s.trim();
    }
}
