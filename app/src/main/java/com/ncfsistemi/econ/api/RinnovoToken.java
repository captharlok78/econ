package com.ncfsistemi.econ.api;

import android.content.Context;
import android.provider.Settings;

import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Rinnovo del token di Mercury (dura un'ora): a una risposta 401, se c'e' "Ricordami", rifa' il login in silenzio nella
 * ditta corrente e ripete la chiamata con il token nuovo. Senza credenziali salvate, o se il login non riesce, la 401
 * arriva al chiamante come prima (che porta al login). Una sola ripetizione per chiamata.
 */
class RinnovoToken implements Authenticator {

    private static final Object LOCK = new Object();
    private static final String TAG = "Econ.RinnovoToken";

    private final Context context;
    private final String baseUrl;

    RinnovoToken(Context context, String baseUrl) {
        this.context = context.getApplicationContext();
        this.baseUrl = baseUrl;
    }

    @Override
    public Request authenticate(Route route, Response response) {
        if (response.priorResponse() != null || response.request().url().encodedPath().endsWith("/api/auth/login")) {
            return null;
        }
        TokenManager tm = TokenManager.getInstance(context);
        if (!tm.hasCredenzialiRicordami()) {
            android.util.Log.i(TAG, "401 senza credenziali ricordate: nessun rinnovo");
            return null;
        }
        String usato = response.request().header("Authorization");
        synchronized (LOCK) {
            String attuale = tm.getToken();
            // un'altra chiamata l'ha gia' rinnovato nel frattempo
            if (attuale != null && usato != null && !usato.equals("Bearer " + attuale)) {
                return conToken(response.request(), attuale);
            }
            String nuovo = login(tm);
            return nuovo != null ? conToken(response.request(), nuovo) : null;
        }
    }

    /** Minuti prima della scadenza in cui il token si rinnova. */
    private static final long ANTICIPO_SECONDI = 20 * 60;

    /**
     * Se il token scade entro ANTICIPO_SECONDI (ma e' ancora valido) chiede al server un token nuovo
     * (POST api/auth/rinnova): cosi' la sessione resta viva anche senza "Ricordami". Non fa niente se manca il token,
     * se e' gia' scaduto (ci pensa authenticate con "Ricordami") o se la chiamata non riesce.
     */
    static void rinnovaSeInScadenza(Context context, String baseUrl) {
        TokenManager tm = TokenManager.getInstance(context);
        long scadenza = scadenza(tm.getToken());
        long ora = System.currentTimeMillis() / 1000L;
        if (scadenza <= ora || scadenza - ora > ANTICIPO_SECONDI) {
            return;
        }
        synchronized (LOCK) {
            String token = tm.getToken();
            scadenza = scadenza(token);
            if (scadenza <= ora || scadenza - ora > ANTICIPO_SECONDI) {
                return; // gia' rinnovato da un'altra chiamata
            }
            try {
                OkHttpClient semplice = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                                .header("Authorization", "Bearer " + token).header("Accept", "application/json").build()))
                        .build();
                MercuryApiService api = new Retrofit.Builder().baseUrl(baseUrl).client(semplice)
                        .addConverterFactory(GsonConverterFactory.create()).build().create(MercuryApiService.class);
                java.util.Map<String, Object> dati = new java.util.HashMap<>();
                dati.put("device_serial", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
                retrofit2.Response<MercuryApiService.LoginResponse> r = api.rinnova(dati).execute();
                if (r.isSuccessful() && r.body() != null && r.body().token != null) {
                    tm.saveToken(r.body());
                    android.util.Log.i(TAG, "token rinnovato prima della scadenza");
                } else {
                    android.util.Log.w(TAG, "rinnovo anticipato rifiutato: HTTP " + r.code());
                }
            } catch (Exception e) {
                android.util.Log.w(TAG, "rinnovo anticipato non riuscito", e);
            }
        }
    }

    /** "exp" del JWT in secondi, 0 se manca o non si legge. */
    static long scadenza(String token) {
        if (token == null) return 0;
        try {
            String[] parti = token.split("\\.");
            if (parti.length != 3) return 0;
            byte[] json = android.util.Base64.decode(parti[1], android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING);
            return new org.json.JSONObject(new String(json, "UTF-8")).optLong("exp", 0);
        } catch (Exception e) {
            return 0;
        }
    }

    private Request conToken(Request request, String token) {
        return request.newBuilder().header("Authorization", "Bearer " + token).build();
    }

    /** Login con le credenziali ricordate nella ditta corrente; salva il token nuovo (e moduli, funzionalita'). */
    private String login(TokenManager tm) {
        try {
            OkHttpClient semplice = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
            MercuryApiService api = new Retrofit.Builder().baseUrl(baseUrl).client(semplice)
                    .addConverterFactory(GsonConverterFactory.create()).build().create(MercuryApiService.class);
            String seriale = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            int ditta = tm.getIdDitta();
            retrofit2.Response<MercuryApiService.LoginResponse> r = api.login(new MercuryApiService.LoginRequest(
                    tm.getEmailRicordami(), tm.getPasswordRicordami(), ditta > 0 ? ditta : null, seriale)).execute();
            if (r.isSuccessful() && r.body() != null && r.body().token != null) {
                tm.saveToken(r.body());
                android.util.Log.i(TAG, "token rinnovato");
                return r.body().token;
            }
            android.util.Log.w(TAG, "rinnovo rifiutato: HTTP " + r.code());
        } catch (Exception e) {
            android.util.Log.w(TAG, "rinnovo non riuscito", e); // rete assente o risposta illeggibile: resta la 401
        }
        return null;
    }
}
