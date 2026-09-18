package pfa.app.econtab.servizi;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

public class EConTabService extends Service {
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			/*if (Utility.isOnline(getBaseContext())) {
				SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, MODE_PRIVATE);
				String id = Utility.APP_NAME + "|" + pref.getString("ECONTAB_REG", "") + "|" + Licenza.getIDdispositivo(getBaseContext());

				try {
					id = URLEncoder.encode(Licenza.encrypt(id), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String url = "http://app.pfa.it/controllaAttivazione.aspx?ID=" + id;
				String controllo = Utility.getStringaDaPaginaWeb(url);
				Editor editor = pref.edit();

				if (controllo.equals("KO")) {
					editor.remove("ECONTAB_REG");
				}
				editor.putString("DATA_VERIFICA", Utility.dataToString(Calendar.getInstance()));
				editor.apply();
			}*/
			// Stop the service using the startId, so that we don't stop
			// the service in the middle of handling another job
			stopSelf(msg.arg1);
		}
	}

	public EConTabService() {
	}

	@Override
	public void onCreate() {
		Log.i("Service ECONTAB", "created");
		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block. We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;

		mServiceHandler.sendMessage(msg);

		// If we get killed, after returning from here, restart
		return START_STICKY;

	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		return null;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.i("Service ECONTAB", "destroy");
		super.onDestroy();
	}
}
