package pfa.app.econtab.utils;

import android.content.Context;
import android.content.SharedPreferences;

import pfa.app.econtab.BuildConfig;
import pfa.app.econtab.api.MercuryApiClient;
import pfa.app.econtab.api.MercuryApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Versione "umana" dell'app (es. 1.0.35): la assegna il server al commit (registro dei rilasci, GET api/version/app),
 * non e' il versionName del manifest. Si salva l'ultima ricevuta per il commit di questa build, cosi' la schermata di
 * avvio la mostra anche senza rete.
 */
public final class VersioneApp {

    private static final String PREF_VERSIONE = "VERSIONE_APP";
    private static final String PREF_COMMIT = "VERSIONE_APP_COMMIT";

    public interface Dopo {
        void versione(String versione);
    }

    private VersioneApp() {
    }

    /** Versione salvata per questa build, null se il server non l'ha ancora comunicata. */
    public static String salvata(Context ctx) {
        SharedPreferences p = prefs(ctx);
        return BuildConfig.GIT_COMMIT.equals(p.getString(PREF_COMMIT, "")) ? p.getString(PREF_VERSIONE, null) : null;
    }

    /** Versione del server (Mercury) vista l'ultima volta, per il piede delle stampe; null se mai letta. */
    public static String server(Context ctx) {
        return prefs(ctx).getString("VERSIONE_SERVER", null);
    }

    public static void salvaServer(Context ctx, String versione) {
        if (versione == null || versione.isEmpty()) return;
        prefs(ctx).edit().putString("VERSIONE_SERVER", versione).apply();
    }

    public static void salva(Context ctx, String versione) {
        if (versione == null || versione.isEmpty()) return;
        prefs(ctx).edit().putString(PREF_VERSIONE, versione).putString(PREF_COMMIT, BuildConfig.GIT_COMMIT).apply();
    }

    /** "v 1.0.35", oppure "build 7a4bd34" finche' il server non ha comunicato la versione. */
    public static String etichetta(Context ctx) {
        String v = salvata(ctx);
        if (v != null) return "v " + v;
        String c = BuildConfig.GIT_COMMIT;
        return "build " + (c.length() > 7 ? c.substring(0, 7) : c);
    }

    /** Chiede al server la versione di questa build, la salva e la passa a dopo (solo se nota). */
    public static void aggiorna(Context ctx, Dopo dopo) {
        Context app = ctx.getApplicationContext();
        try {
            MercuryApiClient.getInstance(app).getService().getVersioneApp(BuildConfig.GIT_COMMIT)
                    .enqueue(new Callback<MercuryApiService.VersionResponse>() {
                        @Override
                        public void onResponse(Call<MercuryApiService.VersionResponse> call,
                                               Response<MercuryApiService.VersionResponse> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().versione != null) {
                                salva(app, response.body().versione);
                                if (dopo != null) dopo.versione(response.body().versione);
                            }
                        }

                        @Override
                        public void onFailure(Call<MercuryApiService.VersionResponse> call, Throwable t) {
                            // senza rete resta quella salvata
                        }
                    });
        } catch (Exception ignored) {
            // server non configurato
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
    }
}
