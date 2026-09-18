package pfa.app.econtab.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestisce il JWT token con storage cifrato.
 * Usa EncryptedSharedPreferences (Jetpack Security) invece di SharedPreferences in chiaro.
 */
public class TokenManager {

    private static final String PREFS_FILE   = "mercury_secure_prefs";
    private static final String KEY_TOKEN      = "jwt_token";
    private static final String KEY_USER_ID    = "user_id";
    private static final String KEY_ID_DITTA   = "id_ditta";
    private static final String KEY_NOME_DITTA = "nome_ditta";
    private static final String KEY_NOME       = "user_nome";
    private static final String KEY_COGNOME    = "user_cognome";
    private static final String KEY_MODULI     = "moduli_abilitati";
    private static final String KEY_DITTE      = "ditte_abilitate";
    private static final String KEY_RICORDA_EMAIL    = "ricorda_email";
    private static final String KEY_RICORDA_PASSWORD  = "ricorda_password";

    private static TokenManager instance;
    private final SharedPreferences prefs;

    private TokenManager(Context context) {
        SharedPreferences p;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            p = EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Fallback a SharedPreferences in chiaro se EncryptedSharedPreferences non è disponibile
            p = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        }
        this.prefs = p;
    }

    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveToken(MercuryApiService.LoginResponse response) {
        String nomeDitta = "";
        if (response.ditte != null) {
            for (MercuryApiService.DittaInfo d : response.ditte) {
                if (d.idDitta == response.idDitta) {
                    nomeDitta = d.nome != null ? d.nome : "";
                    break;
                }
            }
            if (nomeDitta.isEmpty() && !response.ditte.isEmpty()) {
                nomeDitta = response.ditte.get(0).nome != null ? response.ditte.get(0).nome : "";
            }
        }
        prefs.edit()
                .putString(KEY_TOKEN,      response.token)
                .putInt(KEY_USER_ID,       response.userId)
                .putInt(KEY_ID_DITTA,      response.idDitta)
                .putString(KEY_NOME_DITTA, nomeDitta)
                .putString(KEY_NOME,       response.nome)
                .putString(KEY_COGNOME,    response.cognome)
                .apply();
        saveModuli(response.moduli);
        saveDitte(response.ditte);
    }

    /**
     * Salva le ditte a cui l'utente è abilitato (stessa lista usata per il login),
     * così la barra in basso può proporle senza dover interrogare il server ad ogni tap.
     */
    public void saveDitte(List<MercuryApiService.DittaInfo> ditte) {
        if (ditte == null) {
            return;
        }
        try {
            JSONArray arr = new JSONArray();
            for (MercuryApiService.DittaInfo d : ditte) {
                JSONObject o = new JSONObject();
                o.put("idDitta", d.idDitta);
                o.put("nome", d.nome);
                arr.put(o);
            }
            prefs.edit().putString(KEY_DITTE, arr.toString()).apply();
        } catch (JSONException ignored) {
            // in caso di dato malformato lascio il valore precedente piuttosto che perderlo
        }
    }

    /** Ditte a cui l'utente è abilitato. Lista vuota se non ancora note. */
    public List<MercuryApiService.DittaInfo> getDitte() {
        List<MercuryApiService.DittaInfo> result = new ArrayList<>();
        String json = prefs.getString(KEY_DITTE, "");
        if (json.isEmpty()) {
            return result;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                MercuryApiService.DittaInfo d = new MercuryApiService.DittaInfo();
                d.idDitta = o.getInt("idDitta");
                d.nome = o.getString("nome");
                result.add(d);
            }
        } catch (JSONException ignored) {
            // dato malformato: meglio nessuna ditta proposta che un crash
        }
        return result;
    }

    /**
     * Salva i codici dei moduli abilitati per l'utente, nell'ordine ricevuto dal server
     * (determina l'ordine delle voci nel menu principale). Se null (server vecchio o
     * campo assente) non tocca il valore già salvato, per non svuotare il menu.
     */
    public void saveModuli(java.util.List<String> moduli) {
        if (moduli == null) {
            return;
        }
        prefs.edit().putString(KEY_MODULI, android.text.TextUtils.join(",", moduli)).apply();
    }

    /**
     * Codici dei moduli abilitati, nell'ordine salvato. Lista vuota se non ancora note
     * (mai fatto login con un server che supporta i moduli): il chiamante in quel caso
     * deve mostrare tutte le voci per compatibilità, non nessuna.
     */
    public java.util.List<String> getModuli() {
        String csv = prefs.getString(KEY_MODULI, "");
        if (csv.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return new java.util.ArrayList<>(java.util.Arrays.asList(csv.split(",")));
    }

    /** true se non è mai arrivata dal server una lista moduli (nessuna restrizione nota). */
    public boolean moduliMaiRicevuti() {
        return !prefs.contains(KEY_MODULI);
    }

    public String getNomeDitta() {
        return prefs.getString(KEY_NOME_DITTA, "");
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public int getIdDitta() {
        return prefs.getInt(KEY_ID_DITTA, -1);
    }

    public String getNome() {
        return prefs.getString(KEY_NOME, "");
    }

    public String getCognome() {
        return prefs.getString(KEY_COGNOME, "");
    }

    /** Salva un JWT direttamente decodificandone il payload (userId, idDitta). */
    public void saveRawToken(String token) {
        int userId  = -1;
        int idDitta = -1;
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                byte[] decoded = android.util.Base64.decode(
                        parts[1].replace('-', '+').replace('_', '/'),
                        android.util.Base64.DEFAULT);
                org.json.JSONObject payload = new org.json.JSONObject(new String(decoded, "UTF-8"));
                userId  = payload.optInt("userId",  -1);
                idDitta = payload.optInt("idDitta", -1);
            }
        } catch (Exception ignored) {}
        prefs.edit()
                .putString(KEY_TOKEN,    token)
                .putInt(KEY_USER_ID,     userId)
                .putInt(KEY_ID_DITTA,    idDitta)
                .apply();
    }

    public boolean hasToken() {
        return getToken() != null;
    }

    public void clearToken() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_ID_DITTA)
                .remove(KEY_NOME_DITTA)
                .remove(KEY_NOME)
                .remove(KEY_COGNOME)
                .remove(KEY_MODULI)
                .remove(KEY_DITTE)
                .apply();
        clearCredenzialiRicordami();
    }

    /**
     * "Ricordami" del login Mercury: email+password restano precompilate tra un avvio
     * e l'altro finché non si fa logout (clearToken() le cancella insieme al resto).
     * Storage cifrato (stessa istanza EncryptedSharedPreferences del JWT).
     */
    public void saveCredenzialiRicordami(String email, String password) {
        prefs.edit()
                .putString(KEY_RICORDA_EMAIL, email)
                .putString(KEY_RICORDA_PASSWORD, password)
                .apply();
    }

    public void clearCredenzialiRicordami() {
        prefs.edit()
                .remove(KEY_RICORDA_EMAIL)
                .remove(KEY_RICORDA_PASSWORD)
                .apply();
    }

    public String getEmailRicordami() {
        return prefs.getString(KEY_RICORDA_EMAIL, "");
    }

    public String getPasswordRicordami() {
        return prefs.getString(KEY_RICORDA_PASSWORD, "");
    }

    public boolean hasCredenzialiRicordami() {
        return prefs.contains(KEY_RICORDA_PASSWORD);
    }
}
