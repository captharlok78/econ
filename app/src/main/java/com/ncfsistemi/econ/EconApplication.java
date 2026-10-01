package com.ncfsistemi.econ;

import android.app.Application;

import com.ncfsistemi.econ.utils.Sessione;
public class EconApplication extends Application {

	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();

		Sessione.initIstanza();
		// cartella privata dell'app per backup, esportazioni e piantine (SICUREZZA_E_PRIVACY.md, S5)
		com.ncfsistemi.econ.utils.Utility.inizializzaCartella(this);
		
		
		
	}
	
	
}
