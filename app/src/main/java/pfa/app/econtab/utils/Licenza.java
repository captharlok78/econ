package pfa.app.econtab.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.util.IabException;
import com.android.vending.billing.util.IabHelper;
import com.android.vending.billing.util.IabResult;
import com.android.vending.billing.util.Inventory;
import com.android.vending.billing.util.Purchase;

import org.kobjects.base64.Base64;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.transform.sax.TransformerHandler;

import pfa.app.econtab.EConTabActivity;
import pfa.app.econtab.R;

public class Licenza {
    public static String ABBONAMENTO_MENSILE = "abb_mese_2";
    public static String ABBONAMENTO_ANNUALE = "abb_anno";


    public static int ABBONAMENTO_ATTIVO = 0;
    public static int ABBONAMENTO_SCADUTO = 1;
    public static int ABBONAMENTO_RIMBORSATO = 2;

	// variabili per cript/decript
	private static final String password = "apppfa14";
	private static final String salt = "pfasas";
	private static final String iv = "e675f725e675f725";

	public static final int LIGHT = 0;
	public static final int FULL = 1;
	public static final int BUSINESS = 2;// server

	// Il codice usato per le installazioni server
	private String codice = "";
	private int tipoLicenza = 0;



	public Licenza(int tipo) {
		this.tipoLicenza = tipo;
	}

	public String getCodice() {
		return codice;
	}

	public void setCodice(String codice) {
		this.codice = codice;
	}

	public int getTipoLicenza() {
		return tipoLicenza;
	}

	public String getDescrizioneLicenza() {
		if (tipoLicenza == LIGHT) {
			return "LIGHT";
		}
		if (tipoLicenza == FULL) {
			return "STANDARD";
		}
		if (tipoLicenza == BUSINESS) {
			return "BUSINESS";
		}
		return "";
	}

	public static String getIDdispositivo(Context context) {
        System.out.println("EConTab: Licenza getIDdispositivo ENTER");
		String identifier = null;

		if (identifier == null || identifier.length() == 0) {
			identifier = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
		}
        System.out.println("EConTab: Licenza getIDdispositivo EXIT");
		return identifier;
	}

	public static Account[] getGoogleAccount(Context context) {
        System.out.println("EConTab: Licenza getGoogleAccount");
		AccountManager accountManager = AccountManager.get(context);
		return accountManager.getAccountsByType("com.google");

	}

	public static String encrypt(String raw) {
        System.out.println("EConTab: Licenza encrypt");
        if(pfa.app.econtab.Globals.LICENSE_DATA_IS_ENCRYPTED == true) {
            try {
                Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");

                // a random Init. Vector. just for testing
                byte[] iv = "e675f725e675f725".getBytes("UTF-8");

                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                char[] pw = password.toCharArray();
                byte[] s = salt.getBytes("UTF-8");

                KeySpec spec = new PBEKeySpec(pw, s, 65536, 128);
                SecretKey tmp = factory.generateSecret(spec);
                byte[] encoded = tmp.getEncoded();
                SecretKeySpec key = new SecretKeySpec(encoded, "AES");

                c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

                byte[] encryptedVal = c.doFinal(raw.getBytes("UTF-8"));
                return Base64.encode(encryptedVal);
            } catch (Exception ex) {
                return "";
            }
        }
        else{
            try {
                return Base64.encode(raw.getBytes("UTF-8"));
            }catch (Exception ex) {
                return "";
            }
        }
	}

    public static void controllaAbbonamentoOnLine(final EConTabActivity act){
        System.out.println("EConTab: Licenza controllaAbbonamentoOnLine ENTER");
        SharedPreferences pref = act.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        String id = Utility.APP_NAME + "|" + pref.getString("ECONTAB_REG", "") + "|" + Licenza.getIDdispositivo(act)+"|" + pref.getString("ECONTAB_CODICE_FULL", "");
        System.out.println("EConTab: Licenza controllaAbbonamentoOnLine id " + id);
        try {
            id = URLEncoder.encode(Licenza.encrypt(id), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String url = pfa.app.econtab.Globals.LICENSE_URL_SERVER + "/license/attivazioneFull?action=C&ID=" + id;

        System.out.println("EConTab: Licenza controllaAbbonamentoOnLine url " + url);

        // new class for asynchronous task
        final AsyncTaskExecutorService task = new AsyncTaskExecutorService() {
            //ProgressBar di = null;
            AlertDialog dialog = null;

            @Override
            protected void onPreExecute() {
                int llPadding = 30;
                LinearLayout ll = new LinearLayout(act);
                ll.setOrientation(LinearLayout.HORIZONTAL);
                ll.setPadding(llPadding, llPadding, llPadding, llPadding);
                ll.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                llParam.gravity = Gravity.CENTER;
                ll.setLayoutParams(llParam);

                ProgressBar di = new ProgressBar(act);
                di.setPadding(0, 0, llPadding, 0);
                di.setLayoutParams(llParam);
                di.setIndeterminate(true);

                llParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                llParam.gravity = Gravity.CENTER;
                TextView tvText = new TextView(act);
                tvText.setText(act.getString(R.string.messaggio_solo_un_attimo));
                tvText.setTextColor(Color.parseColor("#000000"));
                tvText.setTextSize(20);
                tvText.setLayoutParams(llParam);

                ll.addView(di);
                ll.addView(tvText);

                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                builder.setCancelable(true);
                builder.setView(ll);

                dialog = builder.create();
                dialog.show();
            }

            @Override
            protected Object doInBackground(Object o) {
                String result = "";
                if (Utility.isOnline(act)){
                    try {
                        // Inventory inv =  act.mHelper.queryInventory(true,new ArrayList<String>(),new ArrayList<String>());
                        String resultControllo = Utility.getStringaDaPaginaWeb(url);
                        System.out.println("EConTab: Licenza doInBackground resultControllo " + resultControllo);
                        int giorniRimanenti = 365;
                        if (resultControllo.contains("|") && resultControllo.startsWith("ATTIVATO")) {
                            try {
                                giorniRimanenti = Integer.parseInt(resultControllo.substring(resultControllo.indexOf("|") + 1));
                            } catch (Exception e) {
                                giorniRimanenti = 180;
                            }
                            resultControllo = "ATTIVATO";
                            System.out.println("EConTab: Licenza doInBackground (1) giorniRimanenti " + giorniRimanenti);
                        }
                        else
                        {
                            System.out.println("EConTab: Licenza doInBackground (2) giorniRimanenti " + giorniRimanenti);
                        }
                        aggiornaStatoAbbonamennto(act,resultControllo);

                        //verifico anche lo stato di attivazione di ECONTAB sul dispoitivo
                        SharedPreferences pref = act.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
                        String id = Utility.APP_NAME + "|" + pref.getString("ECONTAB_REG", "") + "|" + Licenza.getIDdispositivo(act);
                        System.out.println("EConTab: Licenza doInBackground id " + id);

                        try {
                            id = URLEncoder.encode(Licenza.encrypt(id), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        //System.out.println("EConTab: Licenza doInBackground id " + id);
                        String url = pfa.app.econtab.Globals.LICENSE_URL_SERVER + "/license/controllaAttivazione?ID=" + id;
                        //System.out.println("EConTab: Licenza doInBackground url " + url);
                        String controllo = Utility.getStringaDaPaginaWeb(url);
                        System.out.println("EConTab: Licenza doInBackground controllo " + controllo);
                        SharedPreferences.Editor editor = pref.edit();
                        if (controllo != null && controllo.equals("KO")) {
                            editor.remove("ECONTAB_REG");
                        }
                        editor.putString("DATA_VERIFICA", Utility.dataToString(Calendar.getInstance()));
                        editor.putInt("GIORNI_RIMANENTI", giorniRimanenti);
                        editor.apply();

                    } catch (Exception e) {
                        result = "KO";
                        e.printStackTrace();
                    }
                }
                else{
                    result = "KO";
                }
                System.out.println("EConTab: Licenza doInBackground result " + result);
                return  result;
            }

            @Override
            protected void onPostExecute(Object o) {
                //di.dismiss();
                dialog.dismiss();
                dialog = null;

                System.out.println("EConTab: Licenza onPostExecute o.toString() " + o.toString());
                if (o.toString().equals("KO")){
                    Toast.makeText(act,"Errore nella connessione",Toast.LENGTH_SHORT).show();
                }
                else{
                    SharedPreferences pref = act.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
                    System.out.println("EConTab: Licenza onPostExecute GIORNI_RIMANENTI " + pref.getInt("GIORNI_RIMANENTI", 365));
                    if (pref.getInt("GIORNI_RIMANENTI", 365)<=30){
                        String publisher_email_address = pfa.app.econtab.Globals.PUBLISHER_EMAIL_ADDRESS;
                        Utility.mostraDialog(act.getString(R.string.attenzione),"La tua licenza scade tra " + pref.getInt("GIORNI_RIMANENTI", 365) + " giorni. Contatta " + publisher_email_address + " per rinnovarla",act,"OK");
                    }
                    Sessione.resettaLicenza(act);
                }
            }
        };
        task.execute();

        /*
        final AsyncTask task = new AsyncTask() {
            ProgressDialog di = null;

            @Override
            protected void onPreExecute() {
                ProgressDialog.Builder ab = new ProgressDialog.Builder(act);
               // ab.setMessage(act.getResources().getString(R.string.messaggio_solo_un_attimo));
                di = new ProgressDialog(act);

                di.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                di.setMessage(act.getString(R.string.messaggio_solo_un_attimo));
                di.setCancelable(false);
                di.setIndeterminate(true);

               di.show();


            }

            @Override
            protected Object doInBackground(Object[] objects) {
                String result = "";
                if (Utility.isOnline(act)){
                    try {
                       // Inventory inv =  act.mHelper.queryInventory(true,new ArrayList<String>(),new ArrayList<String>());
                        String resultControllo = Utility.getStringaDaPaginaWeb(url);
                        System.out.println("EConTab: Licenza doInBackground resultControllo " + resultControllo);
                        int giorniRimanenti = 365;
                        if (resultControllo.contains("|") && resultControllo.startsWith("ATTIVATO")) {
                            try {
                                giorniRimanenti = Integer.parseInt(resultControllo.substring(resultControllo.indexOf("|") + 1));
                            } catch (Exception e) {
                                giorniRimanenti = 180;
                            }
                            resultControllo = "ATTIVATO";
                            System.out.println("EConTab: Licenza doInBackground (1) giorniRimanenti " + giorniRimanenti);
                        }
                        else
                        {
                            System.out.println("EConTab: Licenza doInBackground (2) giorniRimanenti " + giorniRimanenti);
                        }
                        aggiornaStatoAbbonamennto(act,resultControllo);

                        //verifico anche lo stato di attivazione di ECONTAB sul dispoitivo
                        SharedPreferences pref = act.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
                        String id = Utility.APP_NAME + "|" + pref.getString("ECONTAB_REG", "") + "|" + Licenza.getIDdispositivo(act);
                        System.out.println("EConTab: Licenza doInBackground id " + id);

                        String url = "";
                        try {
                            id = URLEncoder.encode(Licenza.encrypt(id), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        //System.out.println("EConTab: Licenza doInBackground id " + id);
                        url = pfa.app.econtab.Globals.LICENSE_URL_SERVER + "/license/controllaAttivazione?ID=" + id;
                        //System.out.println("EConTab: Licenza doInBackground url " + url);
                        String controllo = Utility.getStringaDaPaginaWeb(url);
                        System.out.println("EConTab: Licenza doInBackground controllo " + controllo);
                        SharedPreferences.Editor editor = pref.edit();
                        if (controllo != null && controllo.equals("KO")) {
                            editor.remove("ECONTAB_REG");
                        }
                        editor.putString("DATA_VERIFICA", Utility.dataToString(Calendar.getInstance()));
                        editor.putInt("GIORNI_RIMANENTI", giorniRimanenti);
                        editor.apply();

                    } catch (Exception e) {
                        result = "KO";
                        e.printStackTrace();
                    }
                }
                else{
                    result = "KO";
                }
                System.out.println("EConTab: Licenza doInBackground result " + result);
                return  result;
            }


            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);

                di.dismiss();

                System.out.println("EConTab: Licenza onPostExecute o.toString() " + o.toString());
                if (o.toString().equals("KO")){
                    Toast.makeText(act,"Errore nella connessione",Toast.LENGTH_SHORT).show();
                }
                else{
                    SharedPreferences pref = act.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
                    System.out.println("EConTab: Licenza onPostExecute GIORNI_RIMANENTI " + pref.getInt("GIORNI_RIMANENTI", 365));
                    if (pref.getInt("GIORNI_RIMANENTI", 365)<=30){
                        String publisher_email_address = pfa.app.econtab.Globals.PUBLISHER_EMAIL_ADDRESS;
                        Utility.mostraDialog(act.getString(R.string.attenzione),"La tua licenza scade tra " + pref.getInt("GIORNI_RIMANENTI", 365) + " giorni. Contatta " + publisher_email_address + " per rinnovarla",act,"OK");
                    }
                    Sessione.resettaLicenza(act);
                }
            }
        };
            task.execute();
       */

        System.out.println("EConTab: Licenza controllaAbbonamentoOnLine EXIT");
    }

    public static void aggiornaStatoAbbonamennto(Activity act,String result){
        System.out.println("EConTab: Licenza aggiornaStatoAbbonamennto ENTER");
        System.out.println("EConTab: Licenza aggiornaStatoAbbonamennto result " + result);
        SharedPreferences pref = act.getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if (result.equals("ATTIVATO")){
            System.out.println("EConTab: Licenza aggiornaStatoAbbonamennto OK");
            Calendar oggi = Calendar.getInstance();
            oggi.add(Calendar.DATE,30);
            long prossimoControllo = Utility.dataToNumber(oggi);
            String chiave_abbonamento = getIDdispositivo(act)+"[ECONTAB]"+Licenza.ABBONAMENTO_ANNUALE+"[ECONTAB]"+Licenza.ABBONAMENTO_ATTIVO+"[ECONTAB]"+prossimoControllo;
            System.out.println("EConTab: Licenza aggiornaStatoAbbonamennto chiave_abbonamento " + chiave_abbonamento);
            editor.putString(Sessione.CHIAVE_ABBONAMENTO, chiave_abbonamento);
        }
        else{
            System.out.println("EConTab: Licenza aggiornaStatoAbbonamennto ERROR");
            //Toast.makeText(act,result,Toast.LENGTH_LONG).show();
            editor.remove(Sessione.CHIAVE_ABBONAMENTO);
        }
        editor.apply();
        System.out.println("EConTab: Licenza aggiornaStatoAbbonamennto EXIT");
    }
}
