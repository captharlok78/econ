package pfa.app.econtab.utils;

import android.content.Context;

import pfa.app.econtab.api.MercuryApiClient;
import pfa.app.econtab.api.MercuryApiService;
import pfa.app.econtab.api.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Cambio della password dell'account sul server (POST api/auth/change-password): vale per l'app e per il portale.
 * Usato da Account (MenuActivity) e dal cambio obbligatorio a password scaduta (CambioPasswordActivity).
 * Dopo il cambio aggiorna le credenziali di "Ricordami" (servono al login automatico) e toglie l'avviso di scadenza.
 */
public final class CambioPassword {

    /** Lunghezza minima, la stessa del server (PasswordAccount::LUNGHEZZA_MINIMA). */
    public static final int LUNGHEZZA_MINIMA = 8;

    public interface Esito {
        void fatto(MercuryApiService.PasswordProfilo password);

        void errore(String messaggio);
    }

    private CambioPassword() {
    }

    /** Controlli locali prima dell'invio: messaggio di errore, null se va bene. */
    public static String errore(String attuale, String nuova, String conferma) {
        if (attuale.isEmpty()) return "Inserisci la password attuale";
        if (nuova.length() < LUNGHEZZA_MINIMA) return "La password deve essere di almeno " + LUNGHEZZA_MINIMA + " caratteri";
        if (!nuova.equals(conferma)) return "Le password non coincidono";
        return null;
    }

    public static void invia(Context ctx, String attuale, String nuova, Esito esito) {
        Context app = ctx.getApplicationContext();
        MercuryApiClient.getInstance(app).getService()
                .changePassword(new MercuryApiService.ChangePasswordRequest(attuale, nuova))
                .enqueue(new Callback<MercuryApiService.ChangePasswordResponse>() {
                    @Override
                    public void onResponse(Call<MercuryApiService.ChangePasswordResponse> call,
                                           Response<MercuryApiService.ChangePasswordResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            TokenManager tm = TokenManager.getInstance(app);
                            if (tm.hasCredenzialiRicordami()) {
                                tm.saveCredenzialiRicordami(tm.getEmailRicordami(), nuova);
                            }
                            tm.setPasswordScaduta(false);
                            esito.fatto(response.body().password);
                        } else {
                            esito.errore(MercuryApiClient.messaggioErrore(response, "Errore dal server"));
                        }
                    }

                    @Override
                    public void onFailure(Call<MercuryApiService.ChangePasswordResponse> call, Throwable t) {
                        esito.errore("La password si cambia solo con il server raggiungibile.\n" + t.getMessage());
                    }
                });
    }
}
