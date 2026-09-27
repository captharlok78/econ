package pfa.app.econtab.api;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Streaming;
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

    /** Profilo utente + ditta (sola lettura) + licenza, per il modale "info account" (richiede JWT valido) */
    @GET("api/auth/profilo")
    Call<ProfiloResponse> getProfilo();

    /**
     * Dati ditta (anagrafica, note predefinite, nome file del logo). Se `versione` coincide con quella
     * corrente sul server la risposta e' solo {invariata:true}: i dati si scaricano solo quando cambiano.
     */
    @GET("api/mobile/ditta")
    Call<DittaSyncResponse> getDittaSync(@Query("versione") String versione);

    /**
     * Articoli di listino che spettano alla ditta (configurazione catalogo). Con `firma` uguale a quella del server
     * arrivano solo gli articoli modificati da `since`, altrimenti l'elenco completo da sostituire (vedi CatalogoLocale).
     */
    @GET("api/mobile/catalogo")
    Call<CatalogoResponse> getCatalogo(@Query("firma") String firma, @Query("since") String since);

    /** File del logo della ditta (404 se non presente). Da scaricare solo se il nome file e' cambiato. */
    @Streaming
    @GET("api/mobile/ditta/logo")
    Call<ResponseBody> getLogoDitta();

    /** Moduli (voci di menu) abilitati per l'utente (richiede JWT valido) */
    @GET("api/auth/moduli")
    Call<ModuliResponse> getModuli();

    /** Recupero password step 1: il server invia il codice per email (non lo restituisce mai) */
    @POST("api/auth/forgot-password")
    Call<ForgotPasswordResponse> forgotPassword(@Body ForgotPasswordRequest request);

    /** Recupero password step 2: imposta la nuova password tramite il codice */
    @POST("api/auth/reset-password")
    Call<ResetPasswordResponse> resetPassword(@Body ResetPasswordRequest request);

    /** Cambio password dell'utente collegato: vale per l'account, quindi anche per il portale (richiede JWT) */
    @POST("api/auth/change-password")
    Call<ChangePasswordResponse> changePassword(@Body ChangePasswordRequest request);

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

    /** Versione "umana" di Mercury stesso (nessun parametro: il server risolve il proprio commit). */
    @GET("api/version/mercury")
    Call<VersionResponse> getVersioneMercury();

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

    class LoginResponse {
        public String token;
        public int    userId;
        public int    idDitta;
        public String nome;
        public String cognome;
        public java.util.List<DittaInfo> ditte;
        /** Codici dei moduli app concessi dai pacchetti licenza (più quelli di base), es. ["CLIENTI","PREVENTIVI",...] */
        public java.util.List<String> moduli;
        /** Pacchetti licenza assegnati all'utente nella ditta, con stato e scadenza. */
        public java.util.List<PacchettoInfo> pacchetti;
        /** Funzionalita' dell'app concesse per il ruolo, es. ["CLIENTI.CREA","DASHBOARD.OGGI"] (null da server vecchi) */
        public java.util.List<String> funzionalita;
        /** La ditta usa la pianificazione (null da server vecchi = si') */
        public Boolean pianificazione;
        /** true = password scaduta secondo la ditta: va cambiata prima di continuare (le altre API rispondono 401) */
        public boolean passwordScaduta;
        public PasswordProfilo password;
    }

    /** GET api/auth/moduli: moduli app e pacchetti dell'utente nella ditta del token (chiamato a ogni apertura). */
    class ModuliResponse {
        public java.util.List<String> moduli;
        public java.util.List<String> funzionalita;
        public Boolean pianificazione;
        public java.util.List<PacchettoInfo> pacchetti;
    }

    /** Pacchetto licenza assegnato: tipo "APP" o "WEB" (server), stato leggibile (Attiva, In scadenza, Scaduta, ...). */
    class PacchettoInfo {
        public String  nome;
        public String  tipo;
        public String  stato;
        public boolean valido;
        public String  scadenza;
        public boolean canaleApp;
        public boolean canaleWeb;
    }

    class DittaInfo {
        public int    idDitta;
        public String nome;
    }

    class ProfiloResponse {
        public String nome;
        public String cognome;
        public String email;
        public DittaProfilo ditta;
        public LicenzaProfilo licenza;
        public PasswordProfilo password;
    }

    /** Scadenza della password dell'account secondo la ditta */
    class PasswordProfilo {
        /** Mesi di validità impostati dalla ditta, -1 = non scade */
        public int durataMesi;
        /** Formato "Y-m-d", data dell'ultimo cambio */
        public String cambiataIl;
        /** Formato "Y-m-d", null se non scade */
        public String scadenza;
        /** Negativo se scaduta, null se non scade */
        public Integer giorniRimanenti;
    }

    class CatalogoResponse {
        /** Impronta dell'elenco di articoli della ditta: da rimandare al prossimo allineamento */
        public String  firma;
        /** true = elenco completo, il listino locale va sostituito; false = solo articoli modificati */
        public boolean completo;
        /** Momento dell'allineamento sul server: da rimandare come `since` */
        public String  timestamp;
        public java.util.List<com.google.gson.JsonObject> articoli;
    }

    class DittaSyncResponse {
        public String  versione;
        public boolean invariata;
        /** Null se invariata */
        public DittaDati ditta;
    }

    class DittaDati {
        public int    idDitta;
        public String ragioneSociale;
        public String indirizzo;
        public String citta;
        public String provincia;
        public String cap;
        public String codiceFiscale;
        public String partitaIva;
        public String noteRapportini;
        public String notePreventivi;
        public String noteOrdini;
        /** Nome file del logo sul server (cambia a ogni nuovo upload), null se la ditta non ha logo */
        public String logo;
    }

    class DittaProfilo {
        public int    id;
        public String nome;
    }

    class LicenzaProfilo {
        public boolean illimitata;
        /** Formato "Y-m-d", null se nessuna licenza registrata */
        public String scadenza;
        /** Negativo se scaduta, null se non applicabile (illimitata o nessuna licenza) */
        public Integer giorniRimanenti;
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
        /** Data del rilascio, "Y-m-d" (null con server vecchi). */
        public String data;
    }

    class ForgotPasswordRequest {
        public String email;

        public ForgotPasswordRequest(String email) {
            this.email = email;
        }
    }

    class ForgotPasswordResponse {
        public String message;
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

    class ChangePasswordRequest {
        public String oldPassword;
        public String newPassword;

        public ChangePasswordRequest(String oldPassword, String newPassword) {
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
        }
    }

    class ChangePasswordResponse {
        public String message;
        public String error;
        public PasswordProfilo password;
    }
}
