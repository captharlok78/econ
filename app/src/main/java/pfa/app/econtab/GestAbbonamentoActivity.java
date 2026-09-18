package pfa.app.econtab;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.vending.billing.util.IabHelper;
import com.android.vending.billing.util.IabResult;
import com.android.vending.billing.util.Inventory;
import com.android.vending.billing.util.Purchase;
import com.android.vending.billing.util.SkuDetails;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Ditte;
import pfa.app.econtab.server.ConfigurazioneGenActivity;
import pfa.app.econtab.utils.AsyncTaskExecutorService;
import pfa.app.econtab.utils.Licenza;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;

public class GestAbbonamentoActivity extends EConTabActivity
{
	private TextView textViewMessaggio = null;
    private TextView textViewMessaggioProva = null;
    private ProgressBar progressBar = null;

    private String prezzoMese = "";
    private String prezzoAnno = "";

    private ArrayList<String> abbonamenti = new ArrayList<String>();
    private Button buttonAbbona = null;
    private Button buttonServer = null;

    private boolean attivazioneBusiness = false;

    private IabResult myResult = null;
    private EditText editCodice = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
        System.out.println("EConTab: GestAbbonamentoActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestabbonamento);
        buttonAbbona = (Button)findViewById(R.id.button_abbona);

        buttonServer = (Button)findViewById(R.id.button_server);
        //buttonServer.setVisibility(View.VISIBLE);

        textViewMessaggio = (TextView)findViewById(R.id.textViewMessaggio);
        textViewMessaggioProva = (TextView)findViewById(R.id.textViewMessaggioProva);
        editCodice = (EditText)findViewById(R.id.editTextCodiceAttivazione);

        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        //imposto il codice salvato
        SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        editCodice.setText(pref.getString("ECONTAB_CODICE_FULL",""));
        System.out.println("EConTab: GestAbbonamentoActivity onCreate EXIT");
	}

    @Override
    protected void onResume() {
        System.out.println("EConTab: GestAbbonamentoActivity onResume ENTER");
        super.onResume();
       // buttonAbbona.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        buttonServer.setVisibility(View.GONE);
        textViewMessaggio.setText("");
        textViewMessaggioProva.setText("");

        //dal 22/05/2015 nascondo la possibilità di fare l'abbonamento
        textViewMessaggio.setVisibility(View.GONE);
        textViewMessaggioProva.setVisibility(View.GONE);

        //if (mHelper==null){
        //    mHelper = new IabHelper(GestAbbonamentoActivity.this, BASE_64_PUBLIC_KEY);
        //}

        if (Utility.isOnline(GestAbbonamentoActivity.this)){
            aggiorna();
           /* if (myResult==null) {
                mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                    public void onIabSetupFinished(IabResult result) {
                        myResult = result;
                        processResult();


                    }
                });
            }
            else{
                processResult();
            }*/
        }
        else{
            progressBar.setVisibility(View.GONE);

            textViewMessaggio.setText(getString(R.string.connessione_non_disponibile));
            // Utility.mostraDialog(getString(R.string.attenzione), getString(R.string.connessione_non_disponibile), GestAbbonamentoActivity.this, "OK");
        }
        System.out.println("EConTab: GestAbbonamentoActivity onResume EXIT");
    }


    private void processResult(){
        System.out.println("EConTab: GestAbbonamentoActivity processResult ENTER");
        if (myResult.isSuccess()) {
            aggiorna();
        }
        else
        {
            progressBar.setVisibility(View.GONE);
            Utility.mostraDialog(getString(R.string.attenzione), getString(R.string.errore_connessione_play_store), GestAbbonamentoActivity.this, "OK");
        }
        System.out.println("EConTab: GestAbbonamentoActivity processResult EXIT");
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

    public void abbona(View v){
        System.out.println("EConTab: GestAbbonamentoActivity abbona ENTER");
        if (Utility.isOnline(this)){
            eseguiAbbonamento(Licenza.ABBONAMENTO_ANNUALE);
        }
        else{
            Utility.mostraDialog(getString(R.string.attenzione), getString(R.string.connessione_non_disponibile), this, "OK");
        }
        System.out.println("EConTab: GestAbbonamentoActivity abbona EXIT");
    }

    private void eseguiAbbonamento(String tipo) {
        System.out.println("EConTab: GestAbbonamentoActivity eseguiAbbonamento ENTER");
        SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        String id = Utility.APP_NAME + "|" + pref.getString("ECONTAB_REG", "") + "|" + Licenza.getIDdispositivo(this)+"|" + editCodice.getText().toString().trim();
        try {
            id = URLEncoder.encode(Licenza.encrypt(id), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        final String url = pfa.app.econtab.Globals.LICENSE_URL_SERVER + "/license/attivazioneFull?action=A&ID=" + id;

        // new class for asynchronous task
        AsyncTaskExecutorService task = new AsyncTaskExecutorService() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected Object doInBackground(Object o) {
                return  Utility.getStringaDaPaginaWeb(url);
            }

            @Override
            protected void onPostExecute(Object o) {
                progressBar.setVisibility(View.GONE);
                String resultWeb = "";
                if(o != null)
                {
                    resultWeb = o.toString();
                } else {
                    resultWeb = "ERROR";
                }
                Licenza.aggiornaStatoAbbonamennto(GestAbbonamentoActivity.this,resultWeb);
                System.out.println("EConTab: GestAbbonamentoActivity onPostExecute resultWeb " + resultWeb);
                Sessione.resettaLicenza(GestAbbonamentoActivity.this);

                progressBar.setVisibility(View.GONE);
            }
        };
        task.execute();

       /* mHelper.launchSubscriptionPurchaseFlow(this,tipo,1,new IabHelper.OnIabPurchaseFinishedListener() {
            @Override
            public void onIabPurchaseFinished(IabResult result, Purchase info) {
                if (result.isSuccess()){
                   aggiorna();
                }
              //  else{
              //     Utility.mostraDialog(getString(R.string.attenzione),result.getMessage(),GestAbbonamentoActivity.this,"OK");
              //  }
            }
        });*/

        System.out.println("EConTab: GestAbbonamentoActivity eseguiAbbonamento EXIT");
    }

    private void aggiorna() {
        System.out.println("EConTab: GestAbbonamentoActivity aggiorna ENTER");
        progressBar.setVisibility(View.VISIBLE);

        // new class for asynchronous task
        AsyncTaskExecutorService task = new AsyncTaskExecutorService() {

            @Override
            protected Object doInBackground(Object o) {
                System.out.println("EConTab: GestAbbonamentoActivity aggiorna doInBackground");
                try {
                    String partitaIvaDitta = "";

                    DbInterno db = new DbInterno(GestAbbonamentoActivity.this);
                    ContentValues whereDitta = new ContentValues();
                    whereDitta.put(Ditte.ID_DITTA,Sessione.getDittaSelezionata());
                    ContentValues recDitta = db.getRecord(new Ditte(),whereDitta);
                    if (recDitta!=null){
                        partitaIvaDitta = recDitta.getAsString(Ditte.PARTITA_IVA);
                    }
                    System.out.println("EConTab: GestAbbonamentoActivity aggiorna partitaIvaDitta " + partitaIvaDitta);
                    db.close();

                    try {
                        if (partitaIvaDitta!=null && !partitaIvaDitta.equals(""))
                        {
                            partitaIvaDitta = URLEncoder.encode(Licenza.encrypt(Utility.APP_NAME + "|" + partitaIvaDitta), "UTF-8");
                            String url = pfa.app.econtab.Globals.LICENSE_URL_SERVER + "/license/controllaPIServer?ID=" + partitaIvaDitta;
                            String resultServer = Utility.getStringaDaPaginaWeb(url);
                            if (resultServer != null && resultServer.equals("OK")) {
                                attivazioneBusiness = true;
                            }
                        }
                    } catch (UnsupportedEncodingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } catch (Exception e) {}
                return "";
            }

            @Override
            protected void onPostExecute(Object o) {
                progressBar.setVisibility(View.GONE);
                if (attivazioneBusiness==true){
                    buttonServer.setVisibility(View.VISIBLE);
                }
            }
        };
        task.execute();

        System.out.println("EConTab: GestAbbonamentoActivity aggiorna EXIT");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

       // mHelper.handleActivityResult(requestCode,resultCode,data);

    }

    public void apriSitoConTab(View v){
        // DL: TODO
        //Utility.apriSito(this, "http://www.econtab.mobi");
    }

    public void attivaServer(View view) {
        Intent intent = new Intent(this, ConfigurazioneGenActivity.class);
        startActivity(intent);
    }

    public void richiediCodice(View view){
        System.out.println("EConTab: GestAbbonamentoActivity richiediCodice ENTER");
        Account[] acc = Licenza.getGoogleAccount(this);
        String account = "";
        if (acc.length > 0) {
            account = acc[0].name;
        }
        System.out.println("EConTab: GestAbbonamentoActivity richiediCodice account " + account);
        // DL: TODO
        //String URL = "http://www.econtab.mobi/contatti/?account=" + account + "&model=" + Build.MODEL + "&android=" + Build.VERSION.RELEASE + "(API" + Build.VERSION.SDK_INT + ")";
        //Utility.apriSito(this,URL);
        System.out.println("EConTab: GestAbbonamentoActivity richiediCodice EXIT");
    }
}
