package com.ncfsistemi.econ;

import android.app.Application;
import android.content.Context;

import com.ncfsistemi.econ.utils.Sessione;
public class EconApplication extends Application {

	private static Context contesto;

	/** Contesto dell'applicazione, per chi non ne ha uno (es. utils.CategorieDisegno); null prima di onCreate. */
	public static Context contesto() {
		return contesto;
	}

	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		contesto = getApplicationContext();

		Sessione.initIstanza();
		// cartella privata dell'app per backup, esportazioni e piantine (SICUREZZA_E_PRIVACY.md, S5)
		com.ncfsistemi.econ.utils.Utility.inizializzaCartella(this);
		// indirizzo del server predefinito della build (release: il cloud), solo se non ne e' gia' impostato uno
		// (STUDIO_PRODUZIONE_E_AGGIORNAMENTI.md §3.2)
		android.content.SharedPreferences pref = getSharedPreferences(com.ncfsistemi.econ.utils.Utility.APP_NAME, MODE_PRIVATE);
		if (!BuildConfig.API_BASE_URL.isEmpty() && pref.getString("URL", "").isEmpty()) {
			pref.edit().putString("URL", BuildConfig.API_BASE_URL).apply();
		}
		
		
		
	}
	
	
}
