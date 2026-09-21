package pfa.app.econtab;

import android.accounts.Account;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.util.IabHelper;
import pfa.app.econtab.api.MercuryApiClient;
import pfa.app.econtab.api.TokenManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.AbstractTable;
import pfa.app.econtab.db.table.Ditte;
import pfa.app.econtab.db.table.Utenti;
import pfa.app.econtab.fragments.EConTabFragment;
import pfa.app.econtab.server.ConfigurazioneGenActivity;
import pfa.app.econtab.api.TokenManager;
import pfa.app.econtab.utils.AsyncTaskExecutorService;
import pfa.app.econtab.utils.Licenza;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabSpecialView;

public abstract class EConTabActivity extends FragmentActivity {

    public static final int NO_BACKGROUND = -1;
    public static final int NO_MENU = -1;
    public static String BASE_64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA+aG/tyW2kwzN62qvdaTSIWf4veiQ2l1v/HVTjsdEb4+QMr+lmCX4KUdsEI2VDkxoFZBAnsHEXDaFzjPnlVW4BFVJ6NGukPimRs1AXFR4jAbvV+FkrFyDJbrLOzvt0J7yHoTXUl/8gNO4vrRt3dISYbpill+3BTg7WkSUIw5VrHUkvOW5jvDmsH5PLHvneg4582Q0PsGqilNw07b6vONCDE/HUVDi+l4u1Cthw2zD8hm6b+MNg7KQfqDFKSfYefZy2d9URKJSF4QXuBtZ0Q5sCsCXZjJ6rqIipV2fYa0SDqbiyfV2fESXL8A4jH+dgXEcjlxvLUtsI4nLIT9rvUfQDQIDAQAB";
    public IabHelper mHelper = null;
    private boolean scalingComplete = false;
    private boolean backgroundComplete = false;
    private boolean sceltaAziendaComplete = false;
    private boolean visualizzazionePupup = false;
    private int previousFingerPosition = 0;
    private int baseLayoutPosition = 0;
    private int defaultViewHeight;
    private boolean isClosing = false;
    private boolean isScrollingUp = false;
    private boolean isScrollingDown = false;


    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        if (visualizzazionePupup)
        {
            getWindow().setWindowAnimations(R.style.animationpopup);
            View contenitore = getWindow().getDecorView().findViewById(android.R.id.content);
            if (contenitore != null) {
                animazioneChiusura(contenitore);
            }
        }
        else{
        getWindow().setWindowAnimations(R.style.animationnormal);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub

        System.out.println("EConTab: EConTabActivity onCreate ENTER");

        if (nascondiTastiera()) {
            getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        }

        // getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        // int screenSize = getResources().getConfiguration().screenLayout &
        // Configuration.SCREENLAYOUT_SIZE_MASK;
        // if (screenSize == Configuration.SCREENLAYOUT_SIZE_NORMAL ||
        // screenSize == Configuration.SCREENLAYOUT_SIZE_SMALL ){
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        // WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // }

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable ex) {

                EConTabCrash(Log.getStackTraceString(ex));

            }
        });

        super.onCreate(savedInstanceState);

        if (getActionBar() != null) {
            getActionBar().hide();
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        System.out.println("EConTab: EConTabActivity onCreate EXIT");
    }

    protected void EConTabCrash(String message) {
        System.out.println("EConTab: EConTabActivity EConTabCrash");
        Intent intent = new Intent(getBaseContext(), CrashActivity.class);
        intent.putExtra("ERRORE", message);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        System.exit(0);
    }

    /**
     * Da sovrascrivere per i metodi da eseguire in maniera asincorna dopo l'apertura dell'activity
     */
    protected void esecuzioneAsincrona() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        //System.out.println("EConTab: EConTabActivity onWindowFocusChanged ENTER");
        if (!getLocalClassName().equals("StartActivity")){
            String NomeDittaSelezionata = Sessione.getNomeDittaSelezionata();
            impostaTestoAzienda(NomeDittaSelezionata);
            impostaTestoOperatore(Sessione.getNomeOperatore());
            //System.out.println("EConTab: EConTabActivity onWindowFocusChanged NomeDittaSelezionata " + NomeDittaSelezionata);
            // TODO Auto-generated method stub
            if (!backgroundComplete && getBackgroundID() > 0) {
                View cont = findViewById(R.id.container);
                if (cont != null) {
                    // cont.setBackgroundResource(getBackgroundID());
                }
                backgroundComplete = true;
            }

            //System.out.println("EConTab: EConTabActivity onWindowFocusChanged 1");

            if (!scalingComplete && eseguiRidimensionamento()) // only do this once
            {
                View content = findViewById(R.id.content);
                Utility.scaleContents(content, findViewById(R.id.container), ridimensionaXY());
                scalingComplete = true;
            }

            //System.out.println("EConTab: EConTabActivity onWindowFocusChanged 2");

            if (sceltaAziendaComplete == false) {
                //System.out.println("EConTab: EConTabActivity onWindowFocusChanged 3");
                sceltaAziendaComplete = true;
                DbInterno db = new DbInterno(this);
                if (!Sessione.isLicenzaBusiness(this)) {
                    // Se non � licenza server imposto l'id utente di default
                    Sessione.setIdOperatore(1, this);
                    ContentValues where = new ContentValues();
                    where.put(Utenti.ID_UTENTE, -1);
                    ArrayList<Object> utente = db.eseguiSelect(new Utenti(), where, null);
                    if (utente.size() > 0) {
                        ContentValues ut = (ContentValues) utente.get(0);
                        Sessione.setNomeOperatore(ut.getAsString(Utenti.NOME) + " " + ut.getAsString(Utenti.COGNOME));
                    }

                }

                //System.out.println("EConTab: EConTabActivity onWindowFocusChanged 4");

                // Se Sessione è stata resettata (es. processo riavviato) e c'è un JWT valido,
                // ripristina ditta e operatore da token senza passare per il DB locale
                // (ditte non sincronizzate). Rete di sicurezza: onResume() lo fa già ad
                // ogni ripresa dell'app, ma questo blocco può girare anche prima di esso.
                if (Sessione.getDittaSelezionata() == 0) {
                    Sessione.ripristinaDaToken(this);
                    impostaTestoOperatore(Sessione.getNomeOperatore());
                }

                // Imposto l'azienda
                if (Sessione.getDittaSelezionata() == 0) {
                    //System.out.println("EConTab: EConTabActivity onWindowFocusChanged 5");
                    final ArrayList<Object> aziende = db.eseguiSelect(new Ditte(), null, new String[]{Ditte.ID_DITTA + " DESC"});
                    if (aziende.size() > 1) {
                        final String[] items = new String[aziende.size()];
                        for (int i = 0; i < aziende.size(); i++) {
                            items[i] = ((ContentValues) aziende.get(i)).getAsString(Ditte.RAGIONE_SOCIALE);
                        }
                        // Preimposto la selezione sulla prima ditta
                        // visualizzata
                        // Sessione.setNomeDittaSelezionata(items[0]);
                        // Sessione.setDittaSelezionata(((ContentValues)aziende.get(0)).getAsInteger(Ditte.ID_DITTA));

                        Utility.mostraSelezioneDialog(getString(R.string.selezione_azienda), items, EConTabActivity.this,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // TODO Auto-generated method stub
                                        Toast.makeText(EConTabActivity.this, "Azienda selezionata: " + items[which], Toast.LENGTH_SHORT).show();
                                        Sessione.setNomeDittaSelezionata(items[which]);
                                        Sessione.setDittaSelezionata(((ContentValues) aziende.get(which)).getAsInteger(Ditte.ID_DITTA));
                                        Intent intent = new Intent(EConTabActivity.this, MenuActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                        EConTabActivity.this.finish();
                                    }
                                });

                    } else {
                        //System.out.println("EConTab: EConTabActivity onWindowFocusChanged 6");
                        try {
                            ContentValues az = (ContentValues) aziende.get(0);
                            Sessione.setDittaSelezionata(az.getAsInteger(Ditte.ID_DITTA));
                            Sessione.setNomeDittaSelezionata(az.getAsString(Ditte.RAGIONE_SOCIALE));
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            Sessione.setDittaSelezionata(-1);
                            Sessione.setNomeDittaSelezionata("Nessuna ditta impostata");
                        }
                    }
                }
                db.close();
            }
            else
            {
                //System.out.println("EConTab: EConTabActivity onWindowFocusChanged sceltaAziendaComplete TRUE");
            }
        }

        //System.out.println("EConTab: EConTabActivity onWindowFocusChanged EXIT");
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        System.out.println("EConTab: EConTabActivity onCreateOptionsMenu ENTER");
        // TODO Auto-generated method stub
        if (getMenuID() > 0) {
            getMenuInflater().inflate(getMenuID(), menu);

            boolean hasMercuryToken = TokenManager.getInstance(this).hasToken();
            if (!Sessione.isLicenzaBusiness(this) && !hasMercuryToken) {
                try {
                    menu.findItem(R.id.item_logout).setVisible(false);
                    menu.findItem(R.id.item_sinc).setVisible(false);
                } catch (Exception e) {

                }
            }

            if (this instanceof CantiereSplitActivity ){
                menu.findItem(R.id.item_cerca_codice).setVisible(true);
            }
            else {
                try {
                    menu.findItem(R.id.item_cerca_codice).setVisible(false);
                }
                catch (Exception ex){

                }
            }
        }

        System.out.println("EConTab: EConTabActivity onCreateOptionsMenu EXIT");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        System.out.println("EConTab: EConTabActivity onOptionsItemSelected ENTER");
        // TODO Auto-generated method stub

        if (item.getItemId() == R.id.item_menu) {
            System.out.println("EConTab: EConTabActivity onOptionsItemSelected item_menu");
            Intent intent = new Intent(this, MenuActivity.class);
            //intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.item_sinc) {
            System.out.println("EConTab: EConTabActivity onOptionsItemSelected item_sinc");
            Intent intent = new Intent(this, ConfigurazioneGenActivity.class);
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.item_logout) {
            System.out.println("EConTab: EConTabActivity onOptionsItemSelected item_logout");
            richiediConfermaLogout();
            return true;
        }

        if (item.getItemId() == R.id.item_about) {
            System.out.println("EConTab: EConTabActivity onOptionsItemSelected item_about");
            String versionNumber = "";
            try {
                versionNumber = getPackageManager().getPackageInfo(getPackageName(), 0).versionName + " build:" + getPackageManager().getPackageInfo(getPackageName(), 0).versionCode ;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            Utility.mostraDialog(getString(R.string.versione_installata), versionNumber, this, getString(R.string.chiudi));
        }

        // DL: TODO
        if(false)
        {
            /*
            if (item.getItemId() == R.id.item_sitoweb) {
                System.out.println("EConTab: EConTabActivity onOptionsItemSelected item_sitoweb");
                Utility.apriSito(this, "http://www.econtab.mobi");
            }
            */
            if (item.getItemId() == R.id.item_feedback) {
                System.out.println("EConTab: EConTabActivity onOptionsItemSelected item_feedback");
                Account[] acc = Licenza.getGoogleAccount(this);
                String account = "";
                if (acc.length > 0) {
                    account = acc[0].name;
                }

                String URL = "http://www.econtab.mobi/contatti/?account=" + account + "&model=" + Build.MODEL + "&android=" + Build.VERSION.RELEASE + "(API" + Build.VERSION.SDK_INT + ")";
           /*try {
                URL = URLEncoder.encode(URL,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }*/
                Utility.apriSito(this, URL);
            }
        }
        else{
            /*
            if (item.getItemId() == R.id.item_sitoweb) {
                System.out.println("EConTab: EConTabActivity onOptionsItemSelected item_sitoweb (BY-PASS)");
            }
            */
            if (item.getItemId() == R.id.item_feedback) {
                System.out.println("EConTab: EConTabActivity onOptionsItemSelected item_feedback (BY-PASS)");
            }
        }

        if (item.getItemId()==R.id.item_controlla_aggiornamenti){
            System.out.println("EConTab: EConTabActivity onOptionsItemSelected item_controlla_aggiornamenti");
            if (Utility.isOnline(this)) {
                // new class for asynchronous task
                AsyncTaskExecutorService<Void, Integer, String> task = new AsyncTaskExecutorService<Void, Integer, String>(){

                    @Override
                    protected String doInBackground(Void unused) {
                        String resultws = "NO";
                        try{
                            String url = pfa.app.econtab.Globals.LICENSE_URL_SERVER + "/mobileapp/version?type=Android";
                            resultws =  Utility.getStringaDaPaginaWeb(url).toString();
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                        System.out.println("EConTab: EConTabActivity doInBackground resultws " + resultws);
                        return resultws;
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        SharedPreferences.Editor editor = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE).edit();
                        editor.putString("DATA_VERIFICA_AGGIORNAMERNTI", Utility.dataToString(Calendar.getInstance()));
                        editor.apply();
                        // Guard: non mostrare dialog se l'activity non è più in foreground
                        if (isFinishing() || isDestroyed()) return;
                        if (result != null && !result.trim().equals("NO")){
                            PackageInfo pinfo;
                            try {
                                pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                                int versionCode = pinfo.versionCode;
                                int newVersion = Integer.parseInt(result.trim());
                                if (newVersion > versionCode){
                                    Utility.mostraConfermaDialog("", "E' disponibile una nuova versione dell'app. Aggiorna adesso!", EConTabActivity.this, "Aggiorna", "No, grazie", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (i==DialogInterface.BUTTON_POSITIVE){
                                                Utility.aggiornaApp(EConTabActivity.this);
                                            }
                                        }
                                    });
                                } else {
                                    Utility.mostraDialog("","Nessun aggiornamento disponibile",EConTabActivity.this,"OK");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Utility.mostraDialog("","Nessun aggiornamento disponibile",EConTabActivity.this,"OK");
                            }
                        } else {
                            Utility.mostraDialog("","Nessun aggiornamento disponibile",EConTabActivity.this,"OK");
                        }
                    }
                };
                task.execute();
            }
            else{
                Utility.mostraDialog("",getString(R.string.connessione_non_disponibile),this,"OK");
            }
        }

        if (item.getItemId() == R.id.item_cerca_codice) {
            ((CantiereSplitActivity)this).cercaElemento();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Pu� essere sovrascritto per avere men� diversi nelle varie activity
     *
     * @return l'id del menu da utilizzare
     */
    protected int getMenuID() {
        return R.menu.econtab_menu;
    }

    /**
     * Pu� essere sovrascritto per avere background diversi nelle varie activity
     *
     * @return
     */
    protected int getBackgroundID() {
        return R.drawable.sfondo_app;
    }

    /**
     * Pu� essere sovrascritto
     *
     * @return se la scalatura avviene in entrambe le dimensioni
     */
    protected boolean ridimensionaXY() {
        return true;
    }

    protected boolean eseguiRidimensionamento() {
        return false;
    }

    public void disabilitaCampo(int id) {
        findViewById(id).setEnabled(false);
    }

    /**
     * Imposta il testo della view passata come id
     *
     * @param id   della textview (editText,Button)
     * @param text
     * @param view la view che contiene la textView (se null prende l'activity)
     */
    public void setText(int id, String text, View view) {
        //System.out.println("EConTab: EConTabActivity setText");
        View v = null;
        if (view == null) {
            v = findViewById(id);
        } else {
            v = view.findViewById(id);
        }
        if (v instanceof TextView) {
            ((TextView) v).setText(text);
        }
        if (v instanceof EConTabSpecialView) {
            ((EConTabSpecialView) v).setValue(text);
        }

    }

    /**
     * Imposta il testo della view passata come id
     *
     * @param id   della textview (editText,Button)
     * @param text
     */
    public void setText(int id, String text) {
        setText(id, text, null);

    }

    /**
     * Ritorna il testo del campo editabile
     *
     * @param id
     * @param view
     * @return
     */
    public String getTesto(int id, View view) {
        View v = null;
        if (view == null) {
            v = findViewById(id);
        } else {
            v = view.findViewById(id);
        }
        if (v instanceof TextView) {
            return ((TextView) v).getText().toString();
        }
        if (v instanceof EConTabSpecialView) {
            return ((EConTabSpecialView) v).getValue();
        }
        return "";
    }

    /**
     * Ritorna il testo del campo editabile
     *
     * @param id
     * @return
     */
    public String getTesto(int id) {
        return getTesto(id, null);
    }

    /**
     * @param intent
     * @param resultCode
     */
    public void apriFinestraModifica(Intent intent, int resultCode) {
        System.out.println("EConTab: EConTabActivity apriFinestraModifica");
        if (resultCode > 0) {
            startActivityForResult(intent, resultCode);
        } else {
            startActivity(intent);
        }
    }

    /**
     * Se la licenza � quella gratuita impedisco l'inserimento
     *
     * @param intent
     * @param resultCode
     */
    public void apriFinestraInserimento(Intent intent, int resultCode, AbstractTable tabella) {
        System.out.println("EConTab: EConTabActivity apriFinestraInserimento");
        DbInterno db = new DbInterno(this);
        if (tabella.controllaMaxInserimentiLicenza(db)) {
            if (resultCode > 0) {
                startActivityForResult(intent, resultCode);
            } else {
                startActivity(intent);
            }
        }
        db.close();
    }

    /**
     * Non c'è più una view generica per ditta/operatore nel footer condiviso (rimossa:
     * si vedono solo nella barra in alto della home, vedi MenuActivity). Il metodo resta
     * come hook per le sottoclassi che vogliono mostrarli altrove.
     */
    protected void impostaTestoAzienda(String azienda) {
    }

    protected void impostaTestoOperatore(String operatore) {
    }

    /** Pulsante di logout nella barra in basso. */
    public void logoutFooter(View v) {
        richiediConfermaLogout();
    }

    private void richiediConfermaLogout() {
        Utility.mostraConfermaDialog(getString(R.string.attenzione), getString(R.string.conferma_disconnessione), this, "OK",
                getString(R.string.annulla), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            eseguiLogout();
                        }
                    }
                });
    }

    private void eseguiLogout() {
        // Logout operatore legacy
        Sessione.logout(this);
        // Logout Mercury: cancella JWT, ditte, moduli e flag attivazione
        TokenManager.getInstance(this).clearToken();
        pfa.app.econtab.utils.DittaLocale.cancella(this);
        getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(Sessione.CODICE_ATTIVAZIONE)
                .apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void confermaCancellazione(final AbstractTable tabella, final ContentValues val, final boolean reload) {
        // TODO Auto-generated method stub
        System.out.println("EConTab: EConTabActivity confermaCancellazione ENTER");
        Utility.mostraConfermaCancellazioneDialog(this, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    DbInterno db = new DbInterno(EConTabActivity.this);
                    db.getReadableDatabase().beginTransaction();

                    int deleteresult = -1;
                    try {
                            if (tabella.cancellazionePossibile(db, val, EConTabActivity.this)) {
                                deleteresult = tabella.cancellaRecord(db, val);
                        }
                        db.getReadableDatabase().setTransactionSuccessful();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        Utility.mostraDialog("Errore", Log.getStackTraceString(e), EConTabActivity.this, "OK");
                    } finally {
                        db.getReadableDatabase().endTransaction();
                        db.close();
                    }

                    if (deleteresult >= 0 && reload == true) {
                        aggiornaDopoCancellazione();
                    }
                }
                dialog.cancel();
            }
        });

    }

    protected void aggiornaDopoCancellazione() {
        System.out.println("EConTab: EConTabActivity aggiornaDopoCancellazione");
        // TODO Auto-generated method stub
        Intent intent = new Intent(EConTabActivity.this, EConTabActivity.this.getClass());
        if (EConTabActivity.this.getIntent().getExtras() != null) {
            intent.putExtras(EConTabActivity.this.getIntent().getExtras());
        }
        startActivity(intent);
        EConTabActivity.this.finish();
    }

    @Override
    protected void onResume() {
        System.out.println("EConTab: EConTabActivity onResume ENTER");
        // TODO Auto-generated method stub
        super.onResume();
        if (getLocalClassName().equals("StartActivity")) {
            return;
        }
        // Riallinea sempre ditta/operatore dal JWT: la Sessione è un singleton in-memory
        // che il sistema azzera se ricrea il processo, mentre il token resta valido.
        // Senza questo passaggio si cadeva nel ramo sotto, che si fidava ciecamente
        // dell'unica riga della tabella Ditte locale anche quando non corrispondeva
        // più alla ditta dell'utente realmente autenticato: da qui la perdita di
        // sincronizzazione con la ditta collegata in sessione.
        Sessione.ripristinaDaToken(this);
        DbInterno db = null;
        if (Sessione.getDittaSelezionata() == 0) {
            db = new DbInterno(this);
            final ArrayList<Object> aziende = db.eseguiSelect(new Ditte(), null, new String[]{Ditte.ID_DITTA + " DESC"});
            if (aziende.size() == 1) {
                ContentValues az = (ContentValues) aziende.get(0);
                try {
                    Sessione.setDittaSelezionata(az.getAsInteger(Ditte.ID_DITTA));
                    Sessione.setNomeDittaSelezionata(az.getAsString(Ditte.RAGIONE_SOCIALE));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    Sessione.setDittaSelezionata(-1);
                    Sessione.setNomeDittaSelezionata("Nessuna ditta impostata");
                }
            }
            db.close();
        }
        impostaTestoAzienda(Sessione.getNomeDittaSelezionata());
        impostaTestoOperatore(Sessione.getNomeOperatore());

        if (isControllaRegistrazione()) {
            // Il token Mercury è sufficiente come prova di registrazione
            boolean hasMercuryToken = pfa.app.econtab.api.TokenManager.getInstance(this).hasToken();
            SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, MODE_PRIVATE);
            if (!hasMercuryToken && !pref.contains("ECONTAB_REG")) {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                return;
            } else if (hasMercuryToken) {
                // Autenticato via Mercury: salta i controlli licenza legacy
            } else {
                String dataultimaVerifica = pref.getString("DATA_VERIFICA", "01/01/1970");

                long ultimaVerificaNumber = Utility.dataToNumber(dataultimaVerifica);
                Calendar oggi = Calendar.getInstance();
                oggi.set(Calendar.HOUR_OF_DAY, 0);
                oggi.set(Calendar.MINUTE, 0);
                oggi.set(Calendar.SECOND, 0);
                long oggiNumber = Utility.dataToNumber(oggi);

                if (ultimaVerificaNumber > oggiNumber) {
                    //imposto la data ultima verifica a ieri per intercettare perchè non può essere nel futuro
                    Calendar ieri = Calendar.getInstance();
                    ieri.add(Calendar.DATE, -1);
                    ieri.set(Calendar.HOUR_OF_DAY, 0);
                    ieri.set(Calendar.MINUTE, 0);
                    ieri.set(Calendar.SECOND, 0);

                    ultimaVerificaNumber = Utility.dataToNumber(ieri);
                }

                // faccio il controllo una volta al giorno
                if (oggiNumber > ultimaVerificaNumber) {
                    // intanto imposto il controllo cos� le prossime activity non lo rilanciano
                    // poi al massimo il servizio lo reimposter� al 01/01/1970 se il controllo non va a buon
                    // fine
                    // Toast.makeText(this, "" + oggiNumber + " - " + ultimaVerificaNumber,
                    // Toast.LENGTH_LONG).show();
                    Editor editor = pref.edit();
                    editor.putString("DATA_VERIFICA", Utility.dataToString(Calendar.getInstance()));
                    editor.apply();
                    if (Utility.isOnline(this)) {
                        //  Intent intentService = new Intent(this, EConTabService.class);
                        //  startService(intentService);

                        //qui verifico anche la scadenza dell'abbonamento direttamente online
                        Licenza.controllaAbbonamentoOnLine(this);
                    } else {
                        String dataValiditaAbbonamento = Sessione.getValiditaAbbonamento(this);
                        if (!dataValiditaAbbonamento.equals("")) {
                            Calendar dataValiditaCal = Utility.numberToDataCalendar(Long.parseLong(dataValiditaAbbonamento));
                            if (Calendar.getInstance().after(dataValiditaCal)) {

                                //dataValiditaCal.add(Calendar.DATE,10);
                                String messaggio = getString(R.string.messaggio_abbonamento_scaduto);
                                //   messaggio = messaggio+System.getProperty("line.separator")+ getString(R.string.messaggio_abbonamento_scaduto_2)+" "+Utility.dataToString(dataValiditaCal);

                                String[] opzioni = new String[2];
                                opzioni[0] = getString(R.string.verifica_adesso) + " (" + getString(R.string.connessione_necessaria) + ")";
                                opzioni[1] = getString(R.string.rimanda_verifica) + "" + System.getProperty("line.separator") + "(" + getString(R.string.messaggio_abbonamento_scaduto_2) + " " + Utility.dataToString(dataValiditaCal) + ")";
                                Utility.mostraSelezioneDialog(messaggio, opzioni, this, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if (i == 0) {
                                            Intent intent = new Intent(EConTabActivity.this, GestAbbonamentoActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                            startActivity(intent);
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }

        // Controllo autenticazione: Mercury token oppure utente locale (legacy)
        if (isControllaLogin()) {
            boolean hasMercuryToken = pfa.app.econtab.api.TokenManager.getInstance(this).hasToken();
            if (!hasMercuryToken) {
                if (Sessione.isLicenzaBusiness(this) && Sessione.getIdOperatore(this) == 0) {
                    db = new DbInterno(this);
                    ArrayList<Object> utenti = db.eseguiSelect(
                            "Select * from " + Utenti.NOME_TABELLA + " where " + Utenti.ID_UTENTE + ">0", null);
                    db.close();
                    Intent intent;
                    if (utenti.size() > 0) {
                        intent = new Intent(this, LoginActivity.class);
                    } else {
                        intent = new Intent(this, ConfigurazioneGenActivity.class);
                    }
                    startActivity(intent);
                }
            }
        }

        System.out.println("EConTab: EConTabActivity onResume EXIT");

    }

    protected boolean isControllaRegistrazione() {
        // TODO Auto-generated method stub
        return true;
    }

    protected boolean isControllaLogin() {
        // TODO Auto-generated method stub
        return true;
    }

    public void inviaMail(View v) {
        // DL: TODO
        //Utility.inviaMail(this, getTesto(v.getId()));
    }

    public void chiama(View v) {
        // DL: TODO
        //Utility.chiama(this, getTesto(v.getId()));
    }

    public void indietro(View v) {
        finish();
    }

    /**
     * Alias di indietro(), usato dal pulsante indietro di header_dettaglio_standard (che ha
     * onClick="annulla" per essere compatibile anche con le pagine di modifica, dove
     * EConTabDettaglioActivity sovrascrive questo metodo per impostare RESULT_CANCELED). Nelle
     * pagine di sola visualizzazione (non EConTabDettaglioActivity) equivale a indietro().
     */
    public void annulla(View v) {
        indietro(v);
    }

    /** Apre il dialog per configurare l'URL del server Mercury (icona impostazioni). */
    public void apriImpostazioniServer(View v) {
        android.content.SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        String urlAttuale = pref.getString("URL", "");

        android.widget.EditText editUrl = new android.widget.EditText(this);
        editUrl.setText(urlAttuale);
        editUrl.setHint("http://192.168.x.x:8000/");
        editUrl.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        editUrl.setSingleLine(true);
        editUrl.setPadding(48, 24, 48, 24);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Impostazioni server Mercury");
        builder.setView(editUrl);
        builder.setPositiveButton("Salva", (dialog, which) -> {
            String nuovoUrl = editUrl.getText().toString().trim();
            if (!nuovoUrl.isEmpty() && !nuovoUrl.endsWith("/")) nuovoUrl = nuovoUrl + "/";
            pref.edit().putString("URL", nuovoUrl).apply();
            pfa.app.econtab.api.MercuryApiClient.invalidate();
            Toast.makeText(this, "URL salvato", Toast.LENGTH_SHORT).show();
        });
        builder.setNeutralButton("Test", null);
        builder.setNegativeButton("Annulla", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(btn -> {
            String urlDaTestare = editUrl.getText().toString().trim();
            if (!urlDaTestare.endsWith("/")) urlDaTestare = urlDaTestare + "/";
            final String pingUrl = urlDaTestare + "api/auth/ditte";
            btn.setEnabled(false);
            new Thread(() -> {
                int code = -1;
                String errMsg = null;
                try {
                    okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                            .build();
                    okhttp3.Request req = new okhttp3.Request.Builder().url(pingUrl).build();
                    try (okhttp3.Response resp = client.newCall(req).execute()) {
                        code = resp.code();
                    }
                } catch (Exception e) {
                    errMsg = e.getMessage();
                }
                final int finalCode = code;
                final String finalErr = errMsg;
                runOnUiThread(() -> {
                    btn.setEnabled(true);
                    if (isFinishing() || isDestroyed()) return;
                    if (finalCode > 0) {
                        Toast.makeText(this, "Server raggiungibile (HTTP " + finalCode + ")", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Non raggiungibile: " + finalErr, Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });
    }

    /**
     * Imposta il contenuto del fragment nel container identificato dall'id sull xml
     *
     * @param container
     * @param fragment
     * @param registraIndietro set true allora all apressione del tasto indietro il fragmanet viene tolto
     */
    public void impostaFragment(int container, EConTabFragment fragment, boolean registraIndietro) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(container, fragment);
        if (registraIndietro) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    /**
     * Imposta il contenuto del fragment nel container identificato dall'id sull xml
     *
     * @param container
     * @param fragment
     */
    public void impostaFragment(int container, EConTabFragment fragment) {
        impostaFragment(container, fragment, false);
    }

    public void setVisualizzazionePopup(int margineLarghezzaPerc, int margineAltezzaPerc) {
        // No-op: tutte le schermate sono a pieno schermo
    }

    public void setVisualizzazionePopup() {
        // No-op: tutte le schermate sono a pieno schermo
    }

    protected boolean nascondiTastiera() {
        // TODO Auto-generated method stub
        return true;
    }

    private void animazioneChiusura(final View contenitore) {
        contenitore.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                // Get finger position on screen
                final int Y = (int) event.getRawY();

                // Switch on motion event type
                switch (event.getAction() & MotionEvent.ACTION_MASK) {

                    case MotionEvent.ACTION_DOWN:
                        // save default base layout height
                        defaultViewHeight = contenitore.getHeight();

                        // Init finger and view position
                        previousFingerPosition = Y;
                        baseLayoutPosition = (int) contenitore.getY();
                        break;

                    case MotionEvent.ACTION_UP:
                        // If user was doing a scroll up
                        if (isScrollingUp) {
                            // Reset baselayout position
                            contenitore.setY(0);
                            // We are not in scrolling up mode anymore
                            isScrollingUp = false;
                        }

                        // If user was doing a scroll down
                        if (isScrollingDown) {
                            // Reset baselayout position
                            contenitore.setY(0);
                            // Reset base layout size
                            contenitore.getLayoutParams().height = defaultViewHeight;
                            contenitore.requestLayout();
                            // We are not in scrolling down mode anymore
                            isScrollingDown = false;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (!isClosing) {
                            int currentYPosition = (int) contenitore.getY();

                            // If we scroll up
                            if (previousFingerPosition > Y) {
                                // First time android rise an event for "up" move
                                if (!isScrollingUp) {
                                    isScrollingUp = true;
                                }

                                // Has user scroll down before -> view is smaller than it's default size -> resize it instead of change it position
                                if (contenitore.getHeight() < defaultViewHeight) {
                                    contenitore.getLayoutParams().height = contenitore.getHeight() - (Y - previousFingerPosition);
                                    //contenitore.requestLayout();
                                } else {
                                    // Has user scroll enough to "auto close" popup ?
                                    if ((baseLayoutPosition - currentYPosition) > defaultViewHeight / 4) {
                                        closeUpAndDismissDialog(currentYPosition, contenitore);
                                        return true;
                                    }

                                    //
                                }
                                contenitore.setY(contenitore.getY() + (Y - previousFingerPosition));

                            }
                            // If we scroll down
                            else {

                                // First time android rise an event for "down" move
                                if (!isScrollingDown) {
                                    isScrollingDown = true;
                                }

                                // Has user scroll enough to "auto close" popup ?
                                if (Math.abs(baseLayoutPosition - currentYPosition) > defaultViewHeight / 2) {
                                    closeDownAndDismissDialog(currentYPosition, contenitore);
                                    return true;
                                }

                                // Change base layout size and position (must change position because view anchor is top left corner)
                                contenitore.setY(contenitore.getY() + (Y - previousFingerPosition));
                                contenitore.getLayoutParams().height = contenitore.getHeight() - (Y - previousFingerPosition);
                                //contenitore.requestLayout();
                            }

                            // Update position
                            previousFingerPosition = Y;
                        }
                        break;
                }
                return true;
            }
        });
    }

    public void closeUpAndDismissDialog(int currentPosition, View contenitore) {
        isClosing = true;
        ObjectAnimator positionAnimator = ObjectAnimator.ofFloat(contenitore, "y", currentPosition, -contenitore.getHeight());
        positionAnimator.setDuration(300);
        positionAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                finish();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }

        });
        positionAnimator.start();
    }

    public void closeDownAndDismissDialog(int currentPosition, View contenitore) {
        isClosing = true;
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenHeight = size.y;
        ObjectAnimator positionAnimator = ObjectAnimator.ofFloat(contenitore, "y", currentPosition, screenHeight + contenitore.getHeight());
        positionAnimator.setDuration(300);
        positionAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                finish();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }

        });
        positionAnimator.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHelper != null) {
            mHelper.dispose();
        }
        mHelper = null;

    }

    public class EConTabAsyncTask extends AsyncTask<Void, Integer, String> {
        AlertDialog di = null;

        private boolean mostraAttesa = false;

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            if (isMostraAttesa()) {
                AlertDialog.Builder ab = new AlertDialog.Builder(EConTabActivity.this);
                ab.setMessage(EConTabActivity.this.getResources().getString(R.string.messaggio_caricamento_in_corso));
                di = ab.create();
                di.show();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);

            esecuzioneAsincrona();
            if (isMostraAttesa()) {
                di.cancel();
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            // TODO Auto-generated method stub
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return "";
        }

        public boolean isMostraAttesa() {
            return mostraAttesa;
        }

        public void setMostraAttesa(boolean mostraAttesa) {
            this.mostraAttesa = mostraAttesa;
        }

    }



}
