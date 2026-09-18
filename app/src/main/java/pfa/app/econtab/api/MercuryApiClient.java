package pfa.app.econtab.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton che crea e gestisce il client Retrofit per Mercury.
 * L'URL base viene letto dalle SharedPreferences.
 *
 * Uso:
 *   MercuryApiService api = MercuryApiClient.getInstance(context).getService();
 *   api.syncDownload("2024-01-01T00:00:00").enqueue(...);
 */
public class MercuryApiClient {

    private static final String PREFS_NAME  = "ECONTAB";
    private static final String PREF_URL    = "URL";
    private static final String DEFAULT_URL = "http://127.0.0.1:8000/";

    private static MercuryApiClient instance;
    private Retrofit retrofit;
    private MercuryApiService service;
    private String currentBaseUrl;

    private MercuryApiClient() {}

    public static synchronized MercuryApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new MercuryApiClient();
        }
        instance.initIfNeeded(context);
        return instance;
    }

    private void initIfNeeded(Context context) {
        String baseUrl = getBaseUrl(context);
        if (!baseUrl.equals(currentBaseUrl)) {
            // URL cambiato (es. dopo configurazione) — ricrea il client
            buildClient(context, baseUrl);
        }
    }

    private void buildClient(Context context, String baseUrl) {
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
        // BuildConfig.DEBUG è disponibile solo dopo la prima compilazione.
        // Usiamo un flag statico per evitare problemi di inizializzazione.
        logger.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttp = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(context))
                .addInterceptor(logger)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(ensureTrailingSlash(baseUrl))
                .client(okHttp)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service       = retrofit.create(MercuryApiService.class);
        currentBaseUrl = baseUrl;
    }

    public MercuryApiService getService() {
        return service;
    }

    /** Invalida il client (es. dopo cambio URL in ConfigurazioneGenActivity) */
    public static void invalidate() {
        instance = null;
    }

    private static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String url = prefs.getString(PREF_URL, DEFAULT_URL);
        return ensureTrailingSlash(url);
    }

    private static String ensureTrailingSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }
}
