package com.ncfsistemi.econ.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Log delle chiamate al server (SICUREZZA_E_PRIVACY.md, S2): solo nelle build di debug, mai nella release. Anche in
 * debug le chiamate di autenticazione (login, rinnovo, password) non registrano il corpo, che contiene password e
 * token, e l'intestazione Authorization e' sempre oscurata.
 */
final class LogChiamate implements Interceptor {

    private final HttpLoggingInterceptor completo = new HttpLoggingInterceptor();
    private final HttpLoggingInterceptor soloTestate = new HttpLoggingInterceptor();

    LogChiamate() {
        completo.setLevel(HttpLoggingInterceptor.Level.BODY);
        completo.redactHeader("Authorization");
        soloTestate.setLevel(HttpLoggingInterceptor.Level.BASIC);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (!com.ncfsistemi.econ.BuildConfig.DEBUG) {
            return chain.proceed(chain.request());
        }
        String percorso = chain.request().url().encodedPath();
        return percorso.contains("/api/auth/") ? soloTestate.intercept(chain) : completo.intercept(chain);
    }
}
