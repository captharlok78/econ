package pfa.app.econtab;

import android.app.Application;

import pfa.app.econtab.utils.Sessione;
public class EConTabApplication extends Application {

	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();

		Sessione.initIstanza();
		
		
		
	}
	
	
}
