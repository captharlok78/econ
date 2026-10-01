package com.ncfsistemi.econ.utils;

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

import com.ncfsistemi.econ.api.TokenManager;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Utenti;
import com.ncfsistemi.econ.server.RecordEliminati;

/**
 * Classe utilizzata per i dati comuni dell'app. Esiste un'unica istanza di questa classe
 * 
 */
public class Sessione {
	private static Sessione istanza = null;

	private SimpleDateFormat dateFormat = null;
	private Calendar calendar = null;
	private int dittaSelezionata = 0;
	private String nomeDittaSelezionata = null;

	private String nomeOperatore = null;
	private ContentValues recordOperatore = null;
	private String dataUltimaSincronizzazione = null;


    private LruCache<String,Bitmap> cacheIcone = null;

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
		System.out.println("Econ: Sessione _setIdOperatore");
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
		System.out.println("Econ: Sessione _getRecordOperatore");
		if (recordOperatore == null) {
			int idOperatoreSel = _getIdOperatore(db.getContext());
			if (idOperatoreSel != 0) {
				ContentValues where = new ContentValues();
				where.put(Utenti.ID_UTENTE, idOperatoreSel);
				try {
					recordOperatore = db.getRecord(new Utenti(), where);
				} catch (Exception e) {
					System.out.println("Econ: Sessione recordOperatore NULL");
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
		System.out.println("Econ: Sessione _getCodiceCliente");
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
		//System.out.println("Econ: Sessione getDataUltimaSincronizzazione");
		// TODO Auto-generated method stub
		String ret = getIstanza()._getDataUltimaSincronizzazione(context);
		if(ret.length() < 2)
		{
			System.out.println("Econ: Sessione getDataUltimaSincronizzazione ERROR!!!!!!!!!!!");
			ret = "19700101000000";
		}
		return ret;
	}

	private String _getDataUltimaSincronizzazione(Context context) {
		//System.out.println("Econ: Sessione _getDataUltimaSincronizzazione");
		// TODO Auto-generated method stub
		if (dataUltimaSincronizzazione == null) {
			SharedPreferences pref = context.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
			dataUltimaSincronizzazione = pref.getString("DATA_ULTIMA_SINCRONIZZAZIONE", "19700101000000");
		}
		return dataUltimaSincronizzazione;

	}

	public static void setDataUltimaSincronizzazione(String data, Context context) {
		System.out.println("Econ: Sessione setDataUltimaSincronizzazione");
		// TODO Auto-generated method stub
		getIstanza()._setDataUltimaSincronizzazione(data, context);
	}

	private void _setDataUltimaSincronizzazione(String data, Context context) {
		System.out.println("Econ: Sessione _setDataUltimaSincronizzazione");
		// TODO Auto-generated method stub
		SharedPreferences pref = context.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
		pref.edit().putString("DATA_ULTIMA_SINCRONIZZAZIONE", data).apply();
		dataUltimaSincronizzazione = data;
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
