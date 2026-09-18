package pfa.app.econtab.api;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Interfaccia Retrofit che mappa gli endpoint REST di Mercury.
 *
 * URL base configurata in MercuryApiClient (presa da SharedPreferences).
 *
 * Autenticazione: JWT Bearer token aggiunto automaticamente da AuthInterceptor.
 */
public interface MercuryApiService {

    // ── Autenticazione ────────────────────────────────────────────────────

    /** Login: ottiene JWT token */
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    /** Lista ditte associate all'utente (richiede JWT valido) */
    @GET("api/auth/ditte")
    Call<java.util.List<DittaInfo>> getDitte();

    /** Moduli (voci di menu) abilitati per l'utente (richiede JWT valido) */
    @GET("api/auth/moduli")
    Call<ModuliResponse> getModuli();

    /**
     * Cambia la ditta attiva: riemette un JWT per la ditta scelta (deve essere tra
     * quelle abilitate per l'utente) senza richiedere di nuovo la password.
     */
    @POST("api/auth/switch-ditta")
    Call<LoginResponse> switchDitta(@Body SwitchDittaRequest request);

    /** Recupero password step 1: genera e restituisce il codice temporaneo */
    @POST("api/auth/forgot-password")
    Call<ForgotPasswordResponse> forgotPassword(@Body ForgotPasswordRequest request);

    /** Recupero password step 2: imposta la nuova password tramite il codice */
    @POST("api/auth/reset-password")
    Call<ResetPasswordResponse> resetPassword(@Body ResetPasswordRequest request);

    // ── Sincronizzazione ──────────────────────────────────────────────────

    /**
     * Download delta: tutti i record modificati dopo `since`.
     * @param since ISO 8601 (es. "2024-01-15T10:30:00")
     */
    @GET("api/mobile/sync/download")
    Call<SyncDownloadResponse> syncDownload(@Query("since") String since);

    /**
     * Record eliminati sul server dopo `since`.
     * @param since ISO 8601
     */
    @GET("api/mobile/sync/deleted")
    Call<DeletedResponse> getDeleted(@Query("since") String since);

    /**
     * Upload batch delle modifiche locali (INSERT/UPDATE/DELETE).
     * Risponde con i mapping id-locali → id-server per i nuovi record.
     */
    @POST("api/mobile/sync/upload")
    Call<SyncUploadResponse> syncUpload(@Body SyncUploadRequest request);

    // ── Immagini ──────────────────────────────────────────────────────────

    /** Upload immagini in batch (base64) */
    @POST("api/mobile/images/upload")
    Call<ImageUploadResponse> uploadImages(@Body ImageUploadRequest request);

    /** Lista immagini modificate dopo `since` da scaricare */
    @GET("api/mobile/images/download")
    Call<ImageDownloadResponse> getImagesToDownload(@Query("since") String since);

    // ── Versione app ──────────────────────────────────────────────────────

    /**
     * Risolve il commit HEAD della build (BuildConfig.GIT_COMMIT) nella versione
     * "umana" registrata su Mercury (tabella Rilasci). Pubblico, nessun JWT richiesto:
     * va chiamato anche dalla schermata di login, prima di autenticarsi.
     */
    @GET("api/version/app")
    Call<VersionResponse> getVersioneApp(@Query("commit") String commit);

    // ── DTO inline ────────────────────────────────────────────────────────

    class LoginRequest {
        public String email;
        public String password;
        public Integer idDitta;   // null = usa la prima disponibile
        @SerializedName("device_serial")
        public String deviceSerial;
        /** Commit HEAD della build (BuildConfig.GIT_COMMIT): Mercury lo risolve in versione umana su Terminale. */
        public String appCommit;

        public LoginRequest(String email, String password, Integer idDitta, String deviceSerial) {
            this.email        = email;
            this.password     = password;
            this.idDitta      = idDitta;
            this.deviceSerial = deviceSerial;
            this.appCommit    = pfa.app.econtab.BuildConfig.GIT_COMMIT;
        }
    }

    class SwitchDittaRequest {
        public int idDitta;
        @SerializedName("device_serial")
        public String deviceSerial;
        public String appCommit;

        public SwitchDittaRequest(int idDitta, String deviceSerial) {
            this.idDitta      = idDitta;
            this.deviceSerial = deviceSerial;
            this.appCommit    = pfa.app.econtab.BuildConfig.GIT_COMMIT;
        }
    }

    class LoginResponse {
        public String token;
        public int    userId;
        public int    idDitta;
        public String nome;
        public String cognome;
        public java.util.List<DittaInfo> ditte;
        /** Codici dei moduli (voci di menu) abilitati per l'utente, es. ["CLIENTI","PREVENTIVI",...] */
        public java.util.List<String> moduli;
    }

    class ModuliResponse {
        public java.util.List<String> moduli;
    }

    class DittaInfo {
        public int    idDitta;
        public String nome;
    }

    class SyncDownloadResponse {
        /** Timestamp del server al momento della sync — da salvare come nuova dataUltimaSincronizzazione */
        public String syncTimestamp;
        /**
         * Mappa tabella → lista record (JsonObject grezzo per deserializzazione flessibile).
         * Struttura risposta Mercury: { "syncTimestamp": "...", "tables": { "cantieri": [...] } }
         */
        public Map<String, List<JsonObject>> tables;
    }

    class DeletedResponse {
        public java.util.List<DeletedItem> deleted;

        public static class DeletedItem {
            public String tabella;
            public String chiaveRecord;
        }
    }

    class SyncUploadRequest {
        /** Mappa tabella → lista record da inserire (id < 0 lato app) */
        public Map<String, java.util.List<JsonObject>> insert;
        /** Mappa tabella → lista record da aggiornare (id > 0) */
        public Map<String, java.util.List<JsonObject>> update;
        /** Mappa tabella → lista di id stringa da eliminare */
        public Map<String, java.util.List<String>> delete;
    }

    class SyncUploadResponse {
        public boolean success;
        /** Mappa tabella → {"-1": 42, "-2": 43} per rimappare gli id locali */
        public Map<String, Map<String, Integer>> idMappings;
        public java.util.List<String> errors;
    }

    class ImageUploadRequest {
        public java.util.List<ImageItem> images;

        public static class ImageItem {
            public String fileName;     // "immagini/categorie/icon.png"
            public String fileContent;  // base64
        }
    }

    class ImageUploadResponse {
        public String syncTimestamp;
        public java.util.List<String> toDownload;  // file da riscaricare
    }

    class ImageDownloadResponse {
        public java.util.List<String> categorie_generali;
        public java.util.List<String> elementi;
        public java.util.List<String> componenti;
        public String syncTimestamp;
    }

    class VersionResponse {
        public String progetto;
        /** Versione "umana" (es. "1.0.2"), null se nessun rilascio è ancora registrato per questo progetto. */
        public String versione;
        public String commit;
        /** true se il commit inviato corrisponde esattamente a un rilascio registrato. */
        public boolean esatta;
    }

    class ForgotPasswordRequest {
        public String email;

        public ForgotPasswordRequest(String email) {
            this.email = email;
        }
    }

    class ForgotPasswordResponse {
        public String message;
        public String resetCode;
        public int    expiresIn;
    }

    class ResetPasswordRequest {
        public String email;
        public String code;
        public String newPassword;

        public ResetPasswordRequest(String email, String code, String newPassword) {
            this.email       = email;
            this.code        = code;
            this.newPassword = newPassword;
        }
    }

    class ResetPasswordResponse {
        public String message;
        public String error;
    }
}
