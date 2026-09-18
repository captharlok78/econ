package pfa.app.econtab.server;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

import pfa.app.econtab.EConTabActivity;
import pfa.app.econtab.SincronizzazioneActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.api.MercuryApiClient;
import pfa.app.econtab.api.MercuryApiService;
import pfa.app.econtab.api.TokenManager;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Ditte;
import pfa.app.econtab.utils.AsyncTaskExecutorService;
import pfa.app.econtab.utils.Licenza;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfigurazioneGenActivity extends EConTabActivity {
	private ProgressBar progress = null;
	private Button buttonAttiva = null;
	private Button buttonSincronizza = null;
	private View linearSincMercury = null;
	private EditText attivazioneEmail = null;
	private EditText attivazionePassword = null;

	private NsdManager.DiscoveryListener nsdDiscoveryListener = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: ConfigurazioneGenActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setVisualizzazionePopup(0, 3);

		System.out.println("EConTab: ConfigurazioneGenActivity onCreate !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		setContentView(R.layout.activity_configurazione_generale);

		setText(R.id.editTextUrlServer, Utility.getURLServer(this));
		progress = (ProgressBar) findViewById(R.id.progressBar1);
		buttonAttiva = (Button) findViewById(R.id.button_attiva);
		buttonSincronizza = (Button) findViewById(R.id.button_sincronizza);
		linearSincMercury = findViewById(R.id.linear_sinc_mercury);
		attivazioneEmail    = (EditText) findViewById(R.id.editTextAttivazioneEmail);
		attivazionePassword = (EditText) findViewById(R.id.editTextAttivazionePassword);

		SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, MODE_PRIVATE);

		if (pref.contains(Sessione.CODICE_ATTIVAZIONE)) {
			findViewById(R.id.linear_attivazione).setVisibility(View.GONE);
			linearSincMercury.setVisibility(View.VISIBLE);
			findViewById(R.id.footer_dett).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.linear_attivazione).setVisibility(View.VISIBLE);
			linearSincMercury.setVisibility(View.GONE);
			findViewById(R.id.footer_dett).setVisibility(View.GONE);
		}

		System.out.println("EConTab: ConfigurazioneGenActivity onCreate EXIT");
	}

	public void chiudi(View v) {
		if (getIntent().getBooleanExtra("FIRST_RUN", false)
				&& TokenManager.getInstance(this).hasToken()) {
			Intent intent = new Intent(this, SincronizzazioneActivity.class);
			intent.putExtra(SincronizzazioneActivity.EXTRA_MODE, SincronizzazioneActivity.MODE_DOWNLOAD);
			intent.putExtra("FIRST_RUN", true);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(intent);
		}
		finish();
	}

	public void apriDownload(View v) {
		android.content.Intent i = new android.content.Intent(this, pfa.app.econtab.SincronizzazioneActivity.class);
		i.putExtra(pfa.app.econtab.SincronizzazioneActivity.EXTRA_MODE,
				pfa.app.econtab.SincronizzazioneActivity.MODE_DOWNLOAD);
		startActivity(i);
	}

	public void apriUpload(View v) {
		android.content.Intent i = new android.content.Intent(this, pfa.app.econtab.SincronizzazioneActivity.class);
		i.putExtra(pfa.app.econtab.SincronizzazioneActivity.EXTRA_MODE,
				pfa.app.econtab.SincronizzazioneActivity.MODE_UPLOAD);
		startActivity(i);
	}

	/** Compat: usato dal button_sincronizza nascosto e dalla prima sincronizzazione */
	public void apriSincronizzazione(View v) {
		apriDownload(v);
	}

	public void salvaConfigurazioneGenerale(View v) {
		System.out.println("EConTab: ConfigurazioneGenActivity salvaConfigurazioneGenerale ENTER");
		SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString("URL", getTesto(R.id.editTextUrlServer));
		editor.apply();

		// Invalida il client Retrofit così usa il nuovo URL
		MercuryApiClient.invalidate();

		finish();
		System.out.println("EConTab: ConfigurazioneGenActivity salvaConfigurazioneGenerale EXIT");
	}

	/**
	 * Test connessione Mercury API.
	 * Chiama login + sync download e mostra il risultato in un dialog.
	 * Email e password vengono chieste con un dialog semplice.
	 */
	public void testMercury(View v) {
		// Chiede email e password in un dialog
		android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
		builder.setTitle("Test Mercury API");

		android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
		layout.setOrientation(android.widget.LinearLayout.VERTICAL);
		int pad = (int)(16 * getResources().getDisplayMetrics().density);
		layout.setPadding(pad, pad, pad, pad);

		final EditText inputEmail = new EditText(this);
		inputEmail.setHint("Email");
		inputEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		layout.addView(inputEmail);

		final EditText inputPassword = new EditText(this);
		inputPassword.setHint("Password");
		inputPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
		layout.addView(inputPassword);

		builder.setView(layout);
		builder.setPositiveButton("Test", (dialog, which) -> {
			String email    = inputEmail.getText().toString().trim();
			String password = inputPassword.getText().toString().trim();
			eseguiTestMercury(email, password);
		});
		builder.setNegativeButton("Annulla", null);
		builder.show();
	}

	private void eseguiTestMercury(String email, String password) {
		android.widget.Toast.makeText(this, "Test Mercury in corso...", android.widget.Toast.LENGTH_SHORT).show();

		MercuryApiService api = MercuryApiClient.getInstance(this).getService();
		MercuryApiService.LoginRequest req = new MercuryApiService.LoginRequest(email, password, null, null);

		api.login(req).enqueue(new Callback<MercuryApiService.LoginResponse>() {
			@Override
			public void onResponse(Call<MercuryApiService.LoginResponse> call,
								   Response<MercuryApiService.LoginResponse> response) {
				if (response.isSuccessful() && response.body() != null) {
					MercuryApiService.LoginResponse body = response.body();
					TokenManager.getInstance(ConfigurazioneGenActivity.this).saveToken(body);

					if (getIntent().getBooleanExtra("FIRST_RUN", false)) {
						runOnUiThread(() -> {
							Intent intent = new Intent(ConfigurazioneGenActivity.this, SincronizzazioneActivity.class);
							intent.putExtra(SincronizzazioneActivity.EXTRA_MODE, SincronizzazioneActivity.MODE_DOWNLOAD);
							intent.putExtra("FIRST_RUN", true);
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
							startActivity(intent);
							finish();
						});
						return;
					}

					String msg = "✓ Login OK\n"
							+ "Utente: " + body.nome + " " + body.cognome + "\n"
							+ "idDitta: " + body.idDitta + "\n"
							+ "Token: " + body.token.substring(0, 20) + "...";

					runOnUiThread(() ->
						Utility.mostraDialog("Mercury — Login OK", msg, ConfigurazioneGenActivity.this, "OK")
					);

					// Tenta anche il download sync (dalla data 1970 per testare)
					api.syncDownload("1970-01-01T00:00:00").enqueue(new Callback<MercuryApiService.SyncDownloadResponse>() {
						@Override
						public void onResponse(Call<MercuryApiService.SyncDownloadResponse> call2,
											   Response<MercuryApiService.SyncDownloadResponse> r2) {
							String syncMsg;
							if (r2.isSuccessful() && r2.body() != null) {
								int totTabelle = r2.body().tables != null ? r2.body().tables.size() : 0;
								syncMsg = "✓ Sync download OK\nTimestamp: " + r2.body().syncTimestamp
										+ "\nTabelle ricevute: " + totTabelle;
							} else {
								syncMsg = "✗ Sync download fallita: HTTP " + r2.code();
							}
							runOnUiThread(() ->
								Utility.mostraDialog("Mercury — Sync", syncMsg, ConfigurazioneGenActivity.this, "OK")
							);
						}

						@Override
						public void onFailure(Call<MercuryApiService.SyncDownloadResponse> call2, Throwable t) {
							runOnUiThread(() ->
								Utility.mostraDialog("Mercury — Sync fallita", t.getMessage(), ConfigurazioneGenActivity.this, "OK")
							);
						}
					});

				} else {
					String errMsg = "HTTP " + response.code();
					try {
						if (response.errorBody() != null) errMsg += "\n" + response.errorBody().string();
					} catch (IOException ignored) {}
					final String finalErrMsg = errMsg;
					runOnUiThread(() ->
						Utility.mostraDialog("Mercury — Login fallito", finalErrMsg, ConfigurazioneGenActivity.this, "OK")
					);
				}
			}

			@Override
			public void onFailure(Call<MercuryApiService.LoginResponse> call, Throwable t) {
				Log.e("TestMercury", "Errore connessione", t);
				runOnUiThread(() ->
					Utility.mostraDialog("Mercury — Errore connessione", t.getMessage(), ConfigurazioneGenActivity.this, "OK")
				);
			}
		});
	}

	public void richiediCodice(View v) {
		System.out.println("EConTab: ConfigurazioneGenActivity richiediCodice ENTER");
        String[] opzioni = new String[2];
        opzioni[0] = getString(R.string.richiedi_per_telefono);
        opzioni[1] = getString(R.string.richiedi_via_mail);
        Utility.mostraSelezioneDialog(getString(R.string.codice_attivazione), opzioni, this, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    Utility.chiama(ConfigurazioneGenActivity.this, "+390499832182",false);
                }
                if (i == 1) {
                    String partitaIva = "P.I.";
                    ContentValues where = new ContentValues();
                    where.put(Ditte.ID_DITTA, Sessione.getDittaSelezionata());
                    DbInterno db = new DbInterno(ConfigurazioneGenActivity.this);
                    ContentValues recDitta = db.getRecord(new Ditte(), where);
                    if (recDitta != null) {
                        partitaIva = partitaIva + " " + recDitta.getAsString(Ditte.PARTITA_IVA);
                    }
                    db.close();
					String publisher_email_address = "";
					publisher_email_address = pfa.app.econtab.Globals.PUBLISHER_EMAIL_ADDRESS;
                    Utility.inviaMail(ConfigurazioneGenActivity.this, publisher_email_address, "Richiesta codice attivazione App.el business", "Ragione Sociale: " + Sessione.getNomeDittaSelezionata() + "\n" + partitaIva);

                }
            }
        });
		System.out.println("EConTab: ConfigurazioneGenActivity richiediCodice EXIT");
	}

	public void attiva(View v) {
		String email    = attivazioneEmail.getText().toString().trim();
		String password = attivazionePassword.getText().toString().trim();

		if (email.isEmpty() || password.isEmpty()) {
			Utility.mostraDialog(getString(R.string.attenzione), "Inserisci email e password.", this, "OK");
			return;
		}

		if (!Utility.isOnline(this)) {
			Utility.mostraDialog(getString(R.string.attenzione), getString(R.string.connessione_non_disponibile), this, "OK");
			return;
		}

		// Persiste l'URL dal campo prima di costruire il client Retrofit
		getSharedPreferences(Utility.APP_NAME, MODE_PRIVATE)
			.edit().putString("URL", getTesto(R.id.editTextUrlServer)).apply();
		MercuryApiClient.invalidate();

		buttonAttiva.setVisibility(View.GONE);
		progress.setVisibility(View.VISIBLE);

		MercuryApiService api = MercuryApiClient.getInstance(this).getService();
		MercuryApiService.LoginRequest req = new MercuryApiService.LoginRequest(email, password, null, null);

		api.login(req).enqueue(new retrofit2.Callback<MercuryApiService.LoginResponse>() {
			@Override
			public void onResponse(retrofit2.Call<MercuryApiService.LoginResponse> call,
								   retrofit2.Response<MercuryApiService.LoginResponse> response) {
				runOnUiThread(() -> {
					progress.setVisibility(View.GONE);
					buttonAttiva.setVisibility(View.VISIBLE);
				});

				if (response.isSuccessful() && response.body() != null) {
					MercuryApiService.LoginResponse body = response.body();
					TokenManager.getInstance(ConfigurazioneGenActivity.this).saveToken(body);

					SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, MODE_PRIVATE);
					pref.edit()
						.putString(Sessione.CODICE_ATTIVAZIONE, body.token)
						.putString("URL", getTesto(R.id.editTextUrlServer))
						.apply();

					Sessione.resettaLicenza(ConfigurazioneGenActivity.this);

					// Pre-imposta la ditta: se l'utente ne ha una sola, selezionala subito
					// senza aspettare il primo download, così il menu mostra già il nome
					if (body.ditte != null && body.ditte.size() == 1) {
						Sessione.setDittaSelezionata(body.ditte.get(0).idDitta);
						Sessione.setNomeDittaSelezionata(body.ditte.get(0).nome);
					} else if (body.ditte != null && body.ditte.size() > 1) {
						// Più ditte: preseleziona quella del token, l'utente può cambiarla dal menu
						Sessione.setDittaSelezionata(body.idDitta);
						for (MercuryApiService.DittaInfo d : body.ditte) {
							if (d.idDitta == body.idDitta) {
								Sessione.setNomeDittaSelezionata(d.nome);
								break;
							}
						}
					}

					runOnUiThread(() -> {
						findViewById(R.id.linear_attivazione).setVisibility(View.GONE);
						linearSincMercury.setVisibility(View.VISIBLE);
						findViewById(R.id.footer_dett).setVisibility(View.VISIBLE);

						if (getIntent().getBooleanExtra("FIRST_RUN", false)) {
							primaSincronizzazione();
						} else {
							Utility.mostraDialog("Accesso completato",
								"Benvenuto, " + body.nome + " " + body.cognome + ".\nProcedere con la sincronizzazione.",
								ConfigurazioneGenActivity.this, "OK");
						}
					});
				} else {
					String errMsg = "HTTP " + response.code();
					try {
						if (response.errorBody() != null) errMsg += "\n" + response.errorBody().string();
					} catch (IOException ignored) {}
					final String msg = errMsg;
					runOnUiThread(() ->
						Utility.mostraDialog("Accesso fallito", msg, ConfigurazioneGenActivity.this, "OK")
					);
				}
			}

			@Override
			public void onFailure(retrofit2.Call<MercuryApiService.LoginResponse> call, Throwable t) {
				runOnUiThread(() -> {
					progress.setVisibility(View.GONE);
					buttonAttiva.setVisibility(View.VISIBLE);
					Utility.mostraDialog("Errore connessione", t.getMessage(), ConfigurazioneGenActivity.this, "OK");
				});
			}
		});
	}

	public void pingServer(View v) {
		String url = getTesto(R.id.editTextUrlServer).trim();
		if (url.isEmpty()) {
			Utility.mostraDialog("Ping", "Inserisci prima l'indirizzo del server.", this, "OK");
			return;
		}
		if (!url.endsWith("/")) url = url + "/";
		final String finalUrl = url;

		final android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
		pd.setMessage("Connessione a " + finalUrl + "...");
		pd.setCancelable(false);
		pd.show();

		OkHttpClient client = new OkHttpClient.Builder()
			.connectTimeout(5, TimeUnit.SECONDS)
			.readTimeout(5, TimeUnit.SECONDS)
			.followRedirects(false)
			.build();

		okhttp3.Request request = new okhttp3.Request.Builder().url(finalUrl).build();

		client.newCall(request).enqueue(new okhttp3.Callback() {
			@Override
			public void onFailure(okhttp3.Call call, IOException e) {
				runOnUiThread(() -> {
					pd.dismiss();
					Utility.mostraDialog("Server non raggiungibile",
						"URL: " + finalUrl +
						"\n\nErrore: " + e.getMessage() +
						"\n\nVerifica:\n• Il server è avviato?\n• L'indirizzo è corretto?\n• Sei sulla stessa rete Wi-Fi?",
						ConfigurazioneGenActivity.this, "OK");
				});
			}

			@Override
			public void onResponse(okhttp3.Call call, okhttp3.Response response) {
				int code = response.code();
				response.close();
				String esito = (code >= 200 && code < 500)
					? "✓ Server raggiungibile (HTTP " + code + ")"
					: "⚠ Risposta inattesa (HTTP " + code + ")";
				runOnUiThread(() -> {
					pd.dismiss();
					Utility.mostraDialog("Ping", esito + "\nURL: " + finalUrl, ConfigurazioneGenActivity.this, "OK");
				});
			}
		});
	}

	@Override
	protected boolean isControllaRegistrazione() {
		return false;
	}

	@Override
	protected boolean isControllaLogin() {
		return false;
	}


    private void primaSincronizzazione() {
		DbInterno db = new DbInterno(ConfigurazioneGenActivity.this);
		db.reset();
		db.close();
		// FLAG_ACTIVITY_CLEAR_TASK elimina ConfigurazioneGenActivity dal back stack,
		// impedendo il secondo lancio della schermata download al ritorno indietro.
		Intent intent = new Intent(this, pfa.app.econtab.SincronizzazioneActivity.class);
		intent.putExtra(pfa.app.econtab.SincronizzazioneActivity.EXTRA_MODE,
			pfa.app.econtab.SincronizzazioneActivity.MODE_DOWNLOAD);
		intent.putExtra("FIRST_RUN", true);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		finish();
    }

	public void sincronizza(View v) {
		apriSincronizzazione(v);
	}

	public void cercaServerMercury(View v) {
		final List<String> trovati = Collections.synchronizedList(new ArrayList<>());
		final Queue<NsdServiceInfo> daRisolvere = new ConcurrentLinkedQueue<>();
		final boolean[] risolvizioneInCorso = {false};

		final android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
		progressDialog.setMessage("Ricerca server Mercury sulla rete...");
		progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(true);
		progressDialog.show();

		final NsdManager nsdMgr = (NsdManager) getSystemService(NSD_SERVICE);
		final Handler uiHandler = new Handler(Looper.getMainLooper());
		final NsdManager.DiscoveryListener[] listenerHolder = new NsdManager.DiscoveryListener[1];

		Runnable mostraRisultati = () -> {
			if (listenerHolder[0] != null) {
				try { nsdMgr.stopServiceDiscovery(listenerHolder[0]); } catch (Exception ignored) {}
				nsdDiscoveryListener = null;
				listenerHolder[0] = null;
			}
			if (!progressDialog.isShowing()) return;
			progressDialog.dismiss();
			List<String> urls = new ArrayList<>(trovati);
			if (urls.isEmpty()) {
				Utility.mostraDialog("Ricerca server", "Nessun server Mercury trovato sulla rete locale.", ConfigurazioneGenActivity.this, "OK");
			} else if (urls.size() == 1) {
				setText(R.id.editTextUrlServer, urls.get(0));
				android.widget.Toast.makeText(ConfigurazioneGenActivity.this, "Server trovato: " + urls.get(0), android.widget.Toast.LENGTH_SHORT).show();
			} else {
				String[] urlsArr = urls.toArray(new String[0]);
				new android.app.AlertDialog.Builder(ConfigurazioneGenActivity.this)
					.setTitle("Seleziona server Mercury")
					.setItems(urlsArr, (d, which) -> setText(R.id.editTextUrlServer, urlsArr[which]))
					.show();
			}
		};

		uiHandler.postDelayed(mostraRisultati, 6000);

		final Runnable[] risolviProssimo = new Runnable[1];
		risolviProssimo[0] = () -> {
			NsdServiceInfo prossimo = daRisolvere.poll();
			if (prossimo == null) {
				risolvizioneInCorso[0] = false;
				return;
			}
			risolvizioneInCorso[0] = true;
			nsdMgr.resolveService(prossimo, new NsdManager.ResolveListener() {
				@Override
				public void onResolveFailed(NsdServiceInfo info, int errorCode) {
					Log.w("NSD", "Resolve fallito per " + info.getServiceName() + " codice " + errorCode);
					uiHandler.post(risolviProssimo[0]);
				}
				@Override
				public void onServiceResolved(NsdServiceInfo info) {
					java.net.InetAddress addr = info.getHost();
					int port = info.getPort();
					if (addr != null && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
						String url = "http://" + addr.getHostAddress() + ":" + port + "/";
						if (!trovati.contains(url)) trovati.add(url);
					}
					uiHandler.post(risolviProssimo[0]);
				}
			});
		};

		listenerHolder[0] = new NsdManager.DiscoveryListener() {
			@Override public void onStartDiscoveryFailed(String type, int errorCode) {
				uiHandler.removeCallbacksAndMessages(null);
				uiHandler.post(() -> {
					progressDialog.dismiss();
					Utility.mostraDialog("Errore ricerca", "Impossibile avviare la ricerca (codice " + errorCode + ").", ConfigurazioneGenActivity.this, "OK");
				});
			}
			@Override public void onStopDiscoveryFailed(String type, int errorCode) {}
			@Override public void onDiscoveryStarted(String type) {}
			@Override public void onDiscoveryStopped(String type) {}
			@Override
			public void onServiceFound(NsdServiceInfo serviceInfo) {
				if (!serviceInfo.getServiceType().contains("_mercury._tcp")) return;
				daRisolvere.add(serviceInfo);
				if (!risolvizioneInCorso[0]) {
					uiHandler.post(risolviProssimo[0]);
				}
			}
			@Override public void onServiceLost(NsdServiceInfo serviceInfo) {}
		};

		nsdDiscoveryListener = listenerHolder[0];
		nsdMgr.discoverServices("_mercury._tcp", NsdManager.PROTOCOL_DNS_SD, listenerHolder[0]);
	}

	@Override
	protected void onDestroy() {
		if (nsdDiscoveryListener != null) {
			try {
				((NsdManager) getSystemService(NSD_SERVICE)).stopServiceDiscovery(nsdDiscoveryListener);
			} catch (Exception ignored) {}
			nsdDiscoveryListener = null;
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		System.out.println("EConTab: ConfigurazioneGenActivity onResume ENTER");
		super.onResume();
		String dataUltimaSync = Sessione.getDataUltimaSincronizzazione(this);
		try{
			setText(R.id.editTextDataAnno,dataUltimaSync.substring(0,4));
			setText(R.id.editTextDataMese,dataUltimaSync.substring(4,6));
			setText(R.id.editTextDataGiorno,dataUltimaSync.substring(6,8));
			setText(R.id.editTextDataOra,dataUltimaSync.substring(8,10));
			setText(R.id.editTextDataMinuti,dataUltimaSync.substring(10,12));
		}
		catch (Exception ex){

		}
		System.out.println("EConTab: ConfigurazioneGenActivity onResume EXIT");
	}

	public void reset(View v) {
		System.out.println("EConTab: ConfigurazioneGenActivity reset ENTER");
		Utility.mostraConfermaDialog(getString(R.string.attenzione), getString(R.string.messaggio_conferma_reset), this, "OK",
				getString(R.string.annulla), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int witch) {
						// TODO Auto-generated method stub
						if (witch == DialogInterface.BUTTON_POSITIVE) {
							//elimino il backup automatico
                           /* try {
                                //per sicurezza faccio un backup del db prima del reset
                                Utility.backupDatabase(ConfigurazioneGenActivity.this,"");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }*/
                            DbInterno db = new DbInterno(ConfigurazioneGenActivity.this);
							db.reset();
							db.close();
						}
					}
				});
		System.out.println("EConTab: ConfigurazioneGenActivity reset EXIT");
	}

}
