package pfa.app.econtab;

public class Globals {
    public static final boolean DEBUG_MODE = false;

    public static final boolean INVIA_RICHIESTA_USE_POST = true;
    public static final String INVIA_RICHIESTA_POST_DATA_SITES_QUOTES = "dataSitesQuotes";
    public static final String INVIA_RICHIESTA_POST_LOGIN = "login";
    public static final String INVIA_RICHIESTA_POST_DATA_ULTIMA_SINCRONIZZAZIONE = "dataUltimaSincronizzazione";
    public static final String INVIA_RICHIESTA_POST_PARAMETRI = "parametri";
    // http://10.0.2.2:5178/sincronizzatore
    public static final String DL_WEB_SERVICE = "sincronizzatore";
    public static final boolean FORCE_IMMAGINI_TO_BE_UPLOADED = false;
    public static final boolean SINCRONIZZA_SOLO_IMMAGINI = false;
    public static final boolean SKIP_SYNCING_IMMAGINI = false;
    public static final boolean FORCE_GOOGLE_ACCOUNT = false;
    //=========================================================================================
    public static final String FORCE_GOOGLE_ACCOUNT_EMAIL_ADDRESS = "arvidemoit@arviweb.it";   //Mail che parte perchè è il server delle licenze di Davide
    //public static final String FORCE_GOOGLE_ACCOUNT_EMAIL_ADDRESS = "pfaimpianti@gmail.com";
    //public static final String FORCE_GOOGLE_ACCOUNT_EMAIL_ADDRESS = "pfademo@arviweb.it";
    //public static final String FORCE_GOOGLE_ACCOUNT_EMAIL_ADDRESS = "arvitemplateeng@econtab.cloud";
    //public static final String FORCE_GOOGLE_ACCOUNT_EMAIL_ADDRESS = "fecchio.impiantielettrici@gmail.com";
    // EConTabAPI
    public static final String LICENSE_URL_SERVER = "http://147.93.127.131:5151";
    public static final String CLOUD_URL_SERVER_PORT = "80";
    //=========================================================================================
    //public static final String FORCE_GOOGLE_ACCOUNT_EMAIL_ADDRESS = "apolato19@gmail.com";
    //public static final String LICENSE_URL_SERVER = "http://192.168.1.64:5178";
    //=========================================================================================
    public static final boolean LICENSE_DATA_IS_ENCRYPTED = false;
    public static final boolean THUMB_FOLDER_IS_ICONS_FOLDER = true;
    public static final String PUBLISHER_EMAIL_ADDRESS = "pfaimpianti@gmail.com";

    public static final String TERMS_AND_CONDITIONS_ACCEPTANCE_FILE_PATH = "tac.dat";
    public static final int READ_BLOCK_SIZE = 256;
    public static final boolean EXTRA_CAVI_TUBI_IN_PREVENTIVO = true;
    public static final int EXTRA_CAVI_TUBI_IN_PREVENTIVO_SINCE_DATE = 20240803;

    // Bug Fixes
    public static final boolean BUG_FIX_CLICK_ON_ELEMENTO_IN_PIANTINA_DOPO_SYNCRONIZZAZIONE = true;
}
