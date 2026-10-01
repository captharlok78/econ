package com.ncfsistemi.econ.api;

import android.content.Context;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Aggiunge il JWT Bearer token ad ogni richiesta verso Mercury. */
public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;
    private final Context context;
    private final String baseUrl;

    public AuthInterceptor(Context context, String baseUrl) {
        this.tokenManager = TokenManager.getInstance(context);
        this.context = context.getApplicationContext();
        this.baseUrl = baseUrl;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        // token che scade entro poco: si rinnova prima della chiamata (RinnovoToken, STATO_SISTEMA_APP.md)
        String percorso = original.url().encodedPath();
        if (!percorso.endsWith("/api/auth/login") && !percorso.endsWith("/api/auth/rinnova")) {
            RinnovoToken.rinnovaSeInScadenza(context, baseUrl);
        }
        String token = tokenManager.getToken();

        Request.Builder builder = original.newBuilder();

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        // Il server invia le tabelle nuove (es. operatori delle righe dei rapportini) solo agli schemi che le hanno
        builder.header("X-App-Schema", String.valueOf(com.ncfsistemi.econ.db.DbInterno.SCHEMA_VERSION));
        builder.header("Accept", "application/json");
        builder.header("Content-Type", "application/json");

        return chain.proceed(builder.build());
    }
}
