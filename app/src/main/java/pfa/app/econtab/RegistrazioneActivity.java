package pfa.app.econtab;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.accounts.AccountManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import pfa.app.econtab.utils.AsyncTaskExecutorService;
import pfa.app.econtab.utils.Licenza;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabSpinner;

public class RegistrazioneActivity extends EConTabActivity  {
	private Button buttonregistra = null;
	private ProgressBar progress = null;
	private LinearLayout linearRegistrazione = null;

	private int REQUEST_CODE_PICK_ACCOUNT = 1234;
	private EConTabSpinner spinnerAccounts;
	private ArrayList<Object> valori;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: RegistrazioneActivity onCreate ENTER");
		super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_registrazione);

		this.spinnerAccounts = (EConTabSpinner) findViewById(R.id.econtabSpinnerAccount);
		buttonregistra = (Button) findViewById(R.id.button_registra);
		linearRegistrazione = (LinearLayout) findViewById(R.id.linear_registrazione);

		progress = (ProgressBar) findViewById(R.id.progressBar1);
		//Account[] acc = Licenza.getGoogleAccount(this);
		//System.out.println("EConTab: RegistrazioneActivity onCreate acc.length " + acc.length);

		this.valori = new ArrayList<Object>();

		if(pfa.app.econtab.Globals.FORCE_GOOGLE_ACCOUNT == false)
		{
			pickUserAccount();
			/*
			for (int i = 0; i < acc.length; i++) {

				ContentValues val = new ContentValues();
				val.put(EConTabSpinner.VALORE, acc[i].name);
				val.put(EConTabSpinner.DESCRIZIONE, acc[i].name);
				valori.add(val);
			}

			if (acc.length > 0) {
				spinnerAccounts.setValue(acc[0].name);
			}
			*/
		}
		else
		{
			ContentValues val = new ContentValues();
			val.put(EConTabSpinner.VALORE, pfa.app.econtab.Globals.FORCE_GOOGLE_ACCOUNT_EMAIL_ADDRESS);
			val.put(EConTabSpinner.DESCRIZIONE, pfa.app.econtab.Globals.FORCE_GOOGLE_ACCOUNT_EMAIL_ADDRESS);
			valori.add(val);
			this.spinnerAccounts.setValue(pfa.app.econtab.Globals.FORCE_GOOGLE_ACCOUNT_EMAIL_ADDRESS);

			this.spinnerAccounts.setValoriSpinnerLibero(valori);

			if (Utility.isOnline(this))
			{
				registra(buttonregistra);
			}
		}

		System.out.println("EConTab: RegistrazioneActivity onCreate EXIT");
	}

	@Override
	protected boolean isControllaRegistrazione() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean isControllaLogin() {
		// TODO Auto-generated method stub
		return false;
	}

	public void pickUserAccount() {
		/*This will list all available accounts on device without any filtering*/
		Intent intent = AccountManager.newChooseAccountIntent(null, null,
				null, false, null, null, null, null);
		startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
	}
	/*After manually selecting every app related account, I got its Account type using the code below*/
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
			// Receiving a result from the AccountPicker
			if (resultCode == RESULT_OK) {
				System.out.println("EConTab: Account Type: " + data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
				System.out.println("EConTab: Account Name: " + data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
				ContentValues val = new ContentValues();
				val.put(EConTabSpinner.VALORE, data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
				val.put(EConTabSpinner.DESCRIZIONE, data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
				valori.add(val);
				spinnerAccounts.setValue(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
				spinnerAccounts.setValoriSpinnerLibero(valori);
				if (Utility.isOnline(this))
				{
					registra(buttonregistra);
				}
			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, R.string.seleziona_account, Toast.LENGTH_LONG).show();
			}
		}
	}

	public void registra(View v) {
		System.out.println("EConTab: RegistrazioneActivity registra ENTER");
		EConTabSpinner spinnerAccounts = (EConTabSpinner) findViewById(R.id.econtabSpinnerAccount);
		String account = spinnerAccounts.getValue();
		if (account.equals("")) {
			Utility.mostraDialog(getString(R.string.attenzione), getString(R.string.messaggio_selezione_account), this, "OK");
		} else {
			if (Utility.isOnline(this)) {
					// new class for asynchronous task
					AsyncTaskExecutorService<Void, Integer, String> task = new AsyncTaskExecutorService<Void, Integer, String>() {

						@Override
						protected void onPreExecute() {
							// TODO Auto-generated method stub
							super.onPreExecute();
							linearRegistrazione.setVisibility(View.GONE);

							progress.setVisibility(View.VISIBLE);
						}

						@Override
						protected String doInBackground(Void unused) {
							// TODO Auto-generated method stub
							EConTabSpinner spinnerAccounts = (EConTabSpinner) findViewById(R.id.econtabSpinnerAccount);
							String account = spinnerAccounts.getValue();
							String id = Utility.APP_NAME + "|" + account + "|" + Licenza.getIDdispositivo(RegistrazioneActivity.this);
							try {
								id = URLEncoder.encode(Licenza.encrypt(id), "UTF-8");
							} catch (UnsupportedEncodingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							String url = pfa.app.econtab.Globals.LICENSE_URL_SERVER + "/license/registrazione?ID=" + id;
							return Utility.getStringaDaPaginaWeb(url);
						}

						@Override
						protected void onPostExecute(String result) {
							if (result != null && result.trim().equals("REGISTRATO")) {
								EConTabSpinner spinnerAccounts = (EConTabSpinner) findViewById(R.id.econtabSpinnerAccount);
								String account = spinnerAccounts.getValue();
								SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, MODE_PRIVATE);
								Editor editor = pref.edit();
								editor.putString("ECONTAB_REG", account);
								editor.apply();

								Toast.makeText(RegistrazioneActivity.this, getString(R.string.registrazione_effettuata), Toast.LENGTH_SHORT)
										.show();
								Intent mainIntent = new Intent(RegistrazioneActivity.this, MenuActivity.class);
								startActivity(mainIntent);
								finish();
							} else {
								if (result == null) {
									Utility.mostraDialog(getString(R.string.attenzione), getString(R.string.connessione_non_disponibile),
											RegistrazioneActivity.this, "OK");
								} else {
									Utility.mostraDialog(getString(R.string.attenzione), result, RegistrazioneActivity.this, "OK");
								}

								linearRegistrazione.setVisibility(View.VISIBLE);
								progress.setVisibility(View.GONE);
							}
						}
					};
					task.execute();
			} else {
				Utility.mostraDialog(getString(R.string.attenzione), getString(R.string.connessione_non_disponibile), this, "OK");

			}
		}
		System.out.println("EConTab: RegistrazioneActivity registra EXIT");
	}

}
