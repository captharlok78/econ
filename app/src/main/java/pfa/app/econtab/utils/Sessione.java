package pfa.app.econtab.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.util.LruCache;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import pfa.app.econtab.api.TokenManager;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Utenti;
import pfa.app.econtab.server.RecordEliminati;

/**
 * Classe utilizzata per i dati comuni dell'app. Esiste un'unica istanza di questa classe
 * 
 * @author daniele
 */
public class Sessione {
	private static Sessione istanza = null;

	private SimpleDateFormat dateFormat = null;
	private Calendar calendar = null;
	private Licenza licenza = null;
	private int dittaSelezionata = 0;
	private String nomeDittaSelezionata = null;

	private String nomeOperatore = null;
	private ContentValues recordOperatore = null;
	private String dataUltimaSincronizzazione = null;

    private String dataValiditaAbbonamento = null;

    private LruCache<String,Bitmap> cacheIcone = null;

    public static final String CHIAVE_ABBONAMENTO = "VLFA"; //Validità licenza full contab
    public static final String CODICE_ATTIVAZIONE = "CAS"; //Codice attivazione server
	private int idPreventivoSelezionato=0;


	private Sessione() {

	}

	public static void initIstanza() {
		if (istanza == null) {
			istanza = new Sessione();
		}
	}

	private static Sessione getIstanza() {
		if (istanza == null) {
			istanza = new Sessione();

		}
		return istanza;
	}

	private SimpleDateFormat _getDateFormat() {
		if (dateFormat == null) {
			dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		}
		return dateFormat;
	}

	public static SimpleDateFormat getDateFormat() {
		return getIstanza()._getDateFormat();
	}

	private Calendar _getCalendar() {
		if (calendar == null) {
			calendar = Calendar.getInstance();
		} else {
			calendar.clear();
		}
		return calendar;
	}

	public static Calendar getCalendar() {
		return getIstanza()._getCalendar();
	}

	private Licenza _getLicenza(Context ctx) {
		//System.out.println("EConTab: Sessione _getLicenza ENTER");
        // licenza = new Licenza(Licenza.LIGHT);
        //return licenza;
      	if (licenza == null) {
			try {
                SharedPreferences pref = ctx.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
                String lic = pref.getString(CODICE_ATTIVAZIONE, "");
				//System.out.println("EConTab: Sessione _getLicenza lic " + lic);
                if (!lic.equals("")){
					//System.out.println("EConTab: Sessione _getLicenza licenza BUSINESS");
                    licenza = new Licenza(Licenza.BUSINESS);
                }
                else{
                    //controllo chiave gratis (fino al 31/12/2015 se qualcuno ha il pacchetto econtabkey.app.pfa.econtabkey può usare la versione full)
                    Calendar today = Calendar.getInstance();
                    Calendar dataScadenzaGratis = Calendar.getInstance();
                    dataScadenzaGratis.set(2016,1,1);
                    if (today.before(dataScadenzaGratis)) {
						System.out.println("EConTab: Sessione _getLicenza today.before(dataScadenzaGratis)");
                        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                        List<ResolveInfo> pkgAppsList = ctx.getPackageManager().queryIntentActivities(mainIntent, 0);
                        for (int i = 0; i < pkgAppsList.size(); i++) {
                            ResolveInfo r = pkgAppsList.get(i);
                            if (r.activityInfo.packageName.equals("econtabkey.app.pfa.econtabkey")) {
								//System.out.println("EConTab: Sessione _getLicenza licenza FULL");
                                licenza = new Licenza(Licenza.FULL);
                            }
                        }
                    }
                    //Per i tablet di PFA è sempre FULL
                    if (Licenza.getIDdispositivo(ctx).equals("351885062665889") || Licenza.getIDdispositivo(ctx).equals("fc68d750b0c5050e")){
                       // licenza = new Licenza(Licenza.FULL);
                    }

                    //se non ho nessuna promozione e non sono PFA
                    if (licenza==null){
						//System.out.println("EConTab: Sessione _getLicenza licenza null");
                        licenza = new Licenza(Licenza.FULL);
                        //se non ha l'abbonamento attivo allora la licenza è la light
                        String validitaAbbonamento = _getValiditaAbbonamento(ctx);
                        if (validitaAbbonamento.equals("")){
							//System.out.println("EConTab: Sessione _getLicenza licenza LIGHT 1");
                            licenza = new Licenza((Licenza.LIGHT));
                        }
                        else{
                            long dataValiditaLong = Long.parseLong(validitaAbbonamento);
                            Calendar dataValiditaCal = Utility.numberToDataCalendar(dataValiditaLong);
                            Calendar oggi = Calendar.getInstance();
                            oggi.add(Calendar.DATE,-10);

                            if (oggi.after(dataValiditaCal)){//10 giorni di estensione dell'abbonamento prima di tornare alla licenza Light
								//System.out.println("EConTab: Sessione _getLicenza licenza LIGHT 2");
								licenza = new Licenza((Licenza.LIGHT));
                            }
                        }
                    }
                }
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				licenza = new Licenza(Licenza.LIGHT);
			}

		}
		//System.out.println("EConTab: Sessione _getLicenza EXIT");
		return licenza;

	}

    /**
     * Funzione per ricaricare la licenza (utile dopo un controllo su App Store)
     * @param ctx
     */
    public static void resettaLicenza(Context ctx){
		System.out.println("EConTab: Sessione resettaLicenza ENTER");
        getIstanza().licenza = null;
        getIstanza().dataValiditaAbbonamento=null;
        getLicenza(ctx);
		System.out.println("EConTab: Sessione resettaLicenza EXIT");
    }


	public static Licenza getLicenza(Context ctx) {
		return getIstanza()._getLicenza(ctx);
	}

	/**
	 * Ritorna true se la versione installata � la demo
	 * 
	 * @param ctx
	 * @return
	 */
	public static boolean isLicenzaGratis(Context ctx) {
		//System.out.println("EConTab: Sessione isLicenzaGratis");
		boolean ret = getIstanza()._getLicenza(ctx).getTipoLicenza() == Licenza.LIGHT;
		//System.out.println("EConTab: Sessione isLicenzaGratis " + ret);
		return ret;
	}

    /**
     * Ritorna true se la versione installata � la demo
     *
     * @param ctx
     * @return
     */
    public static boolean isLicenzaFull(Context ctx) {
		//System.out.println("EConTab: Sessione isLicenzaFull");
		boolean ret = getIstanza()._getLicenza(ctx).getTipoLicenza() == Licenza.FULL;
		//System.out.println("EConTab: Sessione isLicenzaFull " + ret);
		return ret;
    }

	/**
	 * Ritorna true se la versione installata � la server
	 * 
	 * @param ctx
	 * @return
	 */
	public static boolean isLicenzaBusiness(Context ctx) {
		return true;
	}

	private int _getDittaSelezionata() {
		return dittaSelezionata;
	}

	private void _setDittaSelezionata(int dittaSelezionata) {
		this.dittaSelezionata = dittaSelezionata;
	}

	public static int getDittaSelezionata() {
		return getIstanza()._getDittaSelezionata();
	}

	public static void setDittaSelezionata(int dittaSelezionata) {
		getIstanza()._setDittaSelezionata(dittaSelezionata);
	}

	private String _getNomeDittaSelezionata() {
		return nomeDittaSelezionata;
	}

	private void _setNomeDittaSelezionata(String nomeDittaSelezionata) {
		this.nomeDittaSelezionata = nomeDittaSelezionata;
	}

	public static String getNomeDittaSelezionata() {
		return getIstanza()._getNomeDittaSelezionata();
	}

	public static void setNomeDittaSelezionata(String nomeDittaSelezionata) {
		getIstanza()._setNomeDittaSelezionata(nomeDittaSelezionata);
	}

	/**
	 * Riallinea ditta e operatore in sessione con i dati del JWT (TokenManager), che è
	 * persistito su storage cifrato ed è quindi l'unica fonte affidabile dopo che il
	 * processo Android è stato ricreato (la Sessione, essendo un singleton in-memory,
	 * torna a dittaSelezionata=0/nomeOperatore=null anche se l'utente resta autenticato).
	 * Da chiamare ad ogni ripresa dell'app (es. in onResume), non solo dopo il login:
	 * senza questo riallineamento periodico l'app può perdere il collegamento con la
	 * ditta dell'utente loggato e mostrare/filtrare dati di una ditta non più valida.
	 */
	public static void ripristinaDaToken(Context ctx) {
		getIstanza()._ripristinaDaToken(ctx);
	}

	private void _ripristinaDaToken(Context ctx) {
		TokenManager tm = TokenManager.getInstance(ctx);
		if (!tm.hasToken()) {
			return;
		}

		int idDitta = tm.getIdDitta();
		if (idDitta > 0) {
			_setDittaSelezionata(idDitta);
			String nomeDitta = tm.getNomeDitta();
			_setNomeDittaSelezionata((nomeDitta == null || nomeDitta.isEmpty()) ? ("Ditta " + idDitta) : nomeDitta);
		}

		int userId = tm.getUserId();
		if (userId > 0) {
			_setIdOperatore(userId, ctx);
		}

		String nome = tm.getNome() == null ? "" : tm.getNome();
		String cognome = tm.getCognome() == null ? "" : tm.getCognome();
		String nomeCompleto = (nome + " " + cognome).trim();
		if (!nomeCompleto.isEmpty()) {
			_setNomeOperatore(nomeCompleto);
		}
	}

	private int _getIdOperatore(Context ctx) {
		SharedPreferences pref = ctx.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
		if (pref.contains("ID_OPERATORE")) {
			return pref.getInt("ID_OPERATORE", 0);
		}
		return 0;
	}

	private void _setIdOperatore(int idOperatore, Context ctx) {
		System.out.println("EConTab: Sessione _setIdOperatore");
		SharedPreferences pref = ctx.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putInt("ID_OPERATORE", idOperatore);
		editor.apply();

	}

	public static int getIdOperatore(Context ctx) {
		return getIstanza()._getIdOperatore(ctx);
	}

	public static void setIdOperatore(int idOperatore, Context ctx) {
		getIstanza()._setIdOperatore(idOperatore, ctx);
	}

	public static ContentValues getRecordOperatore(DbInterno db) {
		return getIstanza()._getRecordOperatore(db);
	}

	private ContentValues _getRecordOperatore(DbInterno db) {
		System.out.println("EConTab: Sessione _getRecordOperatore");
		if (recordOperatore == null) {
			int idOperatoreSel = _getIdOperatore(db.getContext());
			if (idOperatoreSel != 0) {
				ContentValues where = new ContentValues();
				where.put(Utenti.ID_UTENTE, idOperatoreSel);
				try {
					recordOperatore = db.getRecord(new Utenti(), where);
				} catch (Exception e) {
					System.out.println("EConTab: Sessione recordOperatore NULL");
					recordOperatore = null;
				}
			}
		}
		return recordOperatore;
	}

	private String _getNomeOperatore() {
		return nomeOperatore;
	}

	private void _setNomeOperatore(String nomeOperatore) {
		this.nomeOperatore = nomeOperatore;
	}

	public static String getNomeOperatore() {
		return getIstanza()._getNomeOperatore();
	}

	public static void setNomeOperatore(String nomeOperatore) {
		getIstanza()._setNomeOperatore(nomeOperatore);
	}

	public static String getCodiceCliente(Context ctx) {
		// TODO Auto-generated method stub
		return getIstanza()._getCodiceCliente(ctx);
	}

	private String _getCodiceCliente(Context ctx) {
		System.out.println("EConTab: Sessione _getCodiceCliente");
		// TODO Auto-generated method stub
		SharedPreferences pref = ctx.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
		if (pref.contains("CODICE_CLIENTE")) {
			return pref.getString("CODICE_CLIENTE", "");
		}
		return "";
	}

	public static void logout(Context ctx) {
		getIstanza()._logout(ctx);
	}

	private void _logout(Context ctx) {
		recordOperatore = null;
		setIdOperatore(0, ctx);
	}

	public static String getDataUltimaSincronizzazione(Context context) {
		//System.out.println("EConTab: Sessione getDataUltimaSincronizzazione");
		// TODO Auto-generated method stub
		String ret = getIstanza()._getDataUltimaSincronizzazione(context);
		if(ret.length() < 2)
		{
			System.out.println("EConTab: Sessione getDataUltimaSincronizzazione ERROR!!!!!!!!!!!");
			ret = "19700101000000";
		}
		return ret;
	}

	private String _getDataUltimaSincronizzazione(Context context) {
		//System.out.println("EConTab: Sessione _getDataUltimaSincronizzazione");
		// TODO Auto-generated method stub
		if (dataUltimaSincronizzazione == null) {
			SharedPreferences pref = context.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
			dataUltimaSincronizzazione = pref.getString("DATA_ULTIMA_SINCRONIZZAZIONE", "19700101000000");
		}
		return dataUltimaSincronizzazione;

	}

	public static void setDataUltimaSincronizzazione(String data, Context context) {
		System.out.println("EConTab: Sessione setDataUltimaSincronizzazione");
		// TODO Auto-generated method stub
		getIstanza()._setDataUltimaSincronizzazione(data, context);
	}

	private void _setDataUltimaSincronizzazione(String data, Context context) {
		System.out.println("EConTab: Sessione _setDataUltimaSincronizzazione");
		// TODO Auto-generated method stub
		SharedPreferences pref = context.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
		pref.edit().putString("DATA_ULTIMA_SINCRONIZZAZIONE", data).apply();
		dataUltimaSincronizzazione = data;
	}



    public static String getValiditaAbbonamento(Context context) {
		System.out.println("EConTab: Sessione getValiditaAbbonamento");
        // TODO Auto-generated method stub
        return getIstanza()._getValiditaAbbonamento(context);
    }

    private String _getValiditaAbbonamento(Context context) {
		System.out.println("EConTab: Sessione _getValiditaAbbonamento ENTER");
        // TODO Auto-generated method stub
        if (dataValiditaAbbonamento == null) {
			System.out.println("EConTab: Sessione _getValiditaAbbonamento NULL");
            SharedPreferences pref = context.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
            String chiaveAbbonamento = pref.getString(CHIAVE_ABBONAMENTO, "");
			System.out.println("EConTab: Sessione _getValiditaAbbonamento chiaveAbbonamento " + chiaveAbbonamento);
            if (chiaveAbbonamento.equals("")){
                dataValiditaAbbonamento = "";
            }
            else{
                //decripto la stringa
                try{
                    String[] chiaveSplit = chiaveAbbonamento.split("\\[ECONTAB\\]");
                    String idDispositivoCript = chiaveSplit[0];
					System.out.println("EConTab: Sessione _getValiditaAbbonamento idDispositivoCript " + idDispositivoCript);
                    //se sono sullo stesso dispositivo verifico la data di validità dell'abbonamento
                    if (Licenza.getIDdispositivo(context).equals(idDispositivoCript)){
                        String id_abb = chiaveSplit[1];
                        int stato = Integer.parseInt(chiaveSplit[2]);
                        long dataProssimoControllo = Long.parseLong(chiaveSplit[3]);
                        if (id_abb.equals(Licenza.ABBONAMENTO_ANNUALE) && stato==Licenza.ABBONAMENTO_ATTIVO){
                           dataValiditaAbbonamento = ""+dataProssimoControllo;
						   System.out.println("EConTab: Sessione _getValiditaAbbonamento dataValiditaAbbonamento " + dataValiditaAbbonamento);
                        }
						else
						{
							System.out.println("EConTab: Sessione _getValiditaAbbonamento dataValiditaAbbonamento ERROR");
						}
                    }
                 }
                catch (Exception e){
					System.out.println("EConTab: Sessione _getValiditaAbbonamento dataValiditaAbbonamento EXCEPTION");
                }
            }

            //se qualche controllo non è stato superato imposto a blank (=Licenza Light)
            if (dataValiditaAbbonamento==null){
                dataValiditaAbbonamento = "";
            }
        }
		System.out.println("EConTab: Sessione _getValiditaAbbonamento dataValiditaAbbonamento " + dataValiditaAbbonamento);
		System.out.println("EConTab: Sessione _getValiditaAbbonamento EXIT");
        return dataValiditaAbbonamento;
    }

    private LruCache<String, Bitmap> _getCacheIcone() {
        if (cacheIcone==null){
            int maxSize = (int)Runtime.getRuntime().maxMemory()/1024;
            maxSize = maxSize/8;
            cacheIcone = new LruCache<String, Bitmap>(maxSize){
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount()/1024;
                }
            };
        }
        return cacheIcone;
    }

    public static LruCache<String, Bitmap> getCacheIcone() {
		return getIstanza()._getCacheIcone();
	}


	public static int getIdPreventivoSelezionato() {
		return getIstanza()._getIdPreventivoSelezionato();
	}

	private int _getIdPreventivoSelezionato() {
		return idPreventivoSelezionato;
	}

	private void _setIdPreventivoSelezionato(int idPreventivoSelezionato){
		this.idPreventivoSelezionato = idPreventivoSelezionato;
	}

	public static void setIdPreventivoSelezionato(int idPreventivoSelezionato){
		getIstanza()._setIdPreventivoSelezionato(idPreventivoSelezionato);
	}
}
