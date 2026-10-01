package com.ncfsistemi.econ.utils;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.ncfsistemi.econ.SincronizzazioneActivity;
import com.ncfsistemi.econ.api.MercuryApiClient;
import com.ncfsistemi.econ.api.MercuryApiService;
import com.ncfsistemi.econ.api.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Accesso a Mercury all'apertura dell'app, per tenere sempre allineati app e server.
 *
 * - {@link #accediConCredenzialiRicordate}: con "Ricordami" attivo rifà il login con le credenziali
 *   salvate (token nuovo, moduli e pacchetti aggiornati), senza passare dalla schermata di login;
 * - {@link #apriAllineamento}: apre la sincronizzazione di avvio (moduli, invio, scarico) che al termine
 *   porta da sola al menu.
 */
public final class AccessoMercury {

    /** Esito dell'accesso automatico. */
    public interface Esito {
        void accesso();

        /** Credenziali non più valide o licenza non abilitata: serve il login manuale. */
        void rifiutato(String messaggio);

        /** Server non raggiungibile: si può lavorare con i dati del dispositivo se il token è ancora valido. */
        void erroreRete(String messaggio);
    }

    private AccessoMercury() {
    }

    public static void accediConCredenzialiRicordate(Context ctx, Esito esito) {
        Context app = ctx.getApplicationContext();
        TokenManager tm = TokenManager.getInstance(app);
        String email = tm.getEmailRicordami();
        String password = tm.getPasswordRicordami();
        String deviceSerial = Settings.Secure.getString(app.getContentResolver(), Settings.Secure.ANDROID_ID);

        MercuryApiService api = MercuryApiClient.getInstance(app).getService();
        api.login(new MercuryApiService.LoginRequest(email, password, null, deviceSerial))
                .enqueue(new Callback<MercuryApiService.LoginResponse>() {
                    @Override
                    public void onResponse(Call<MercuryApiService.LoginResponse> call,
                                           Response<MercuryApiService.LoginResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            MercuryApiService.LoginResponse body = response.body();
                            tm.saveToken(body);
                            app.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("MERCURY_EMAIL", email)
                                    .apply();
                            Sessione.ripristinaDaToken(app);
                            esito.accesso();
                        } else if (response.code() == 401) {
                            esito.rifiutato("Le credenziali salvate non sono più valide: accedi di nuovo.");
                        } else if (response.code() == 403 || response.code() == 429) {
                            esito.rifiutato(MercuryApiClient.messaggioErrore(response, "Accesso non consentito"));
                        } else {
                            esito.erroreRete("Errore server (HTTP " + response.code() + ").");
                        }
                    }

                    @Override
                    public void onFailure(Call<MercuryApiService.LoginResponse> call, Throwable t) {
                        esito.erroreRete(t.getMessage());
                    }
                });
    }

    /**
     * Apre la sincronizzazione di avvio, che al termine porta al menu (nessun ritorno alla schermata precedente).
     * Con la password scaduta all'ultimo login (e la rete disponibile) prima si deve cambiarla: il server non accetta
     * altre chiamate. Senza rete si prosegue con i dati del dispositivo e il cambio si chiede alla prossima apertura.
     */
    public static void apriAllineamento(Context ctx) {
        if (TokenManager.getInstance(ctx).isPasswordScaduta() && Utility.isOnline(ctx)) {
            Intent cambio = new Intent(ctx, com.ncfsistemi.econ.CambioPasswordActivity.class);
            cambio.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ctx.startActivity(cambio);
            return;
        }
        Intent intent = new Intent(ctx, SincronizzazioneActivity.class);
        intent.putExtra(SincronizzazioneActivity.EXTRA_MODE, SincronizzazioneActivity.MODE_AVVIO);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ctx.startActivity(intent);
    }
}
