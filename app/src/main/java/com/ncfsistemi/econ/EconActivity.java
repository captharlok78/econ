package com.ncfsistemi.econ;

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

import com.ncfsistemi.econ.api.MercuryApiClient;
import com.ncfsistemi.econ.api.TokenManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Ditte;
import com.ncfsistemi.econ.db.table.Utenti;
import com.ncfsistemi.econ.fragments.EconFragment;
import com.ncfsistemi.econ.server.ConfigurazioneGenActivity;
import com.ncfsistemi.econ.api.TokenManager;
import com.ncfsistemi.econ.utils.AsyncTaskExecutorService;
import com.ncfsistemi.econ.utils.Sessione;
import com.ncfsistemi.econ.utils.Utility;
import com.ncfsistemi.econ.views.EconSpecialView;

public abstract class EconActivity extends FragmentActivity {

    public static final int NO_BACKGROUND = -1;
    public static final int NO_MENU = -1;
    public static String BASE_64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA+aG/tyW2kwzN62qvdaTSIWf4veiQ2l1v/HVTjsdEb4+QMr+lmCX4KUdsEI2VDkxoFZBAnsHEXDaFzjPnlVW4BFVJ6NGukPimRs1AXFR4jAbvV+FkrFyDJbrLOzvt0J7yHoTXUl/8gNO4vrRt3dISYbpill+3BTg7WkSUIw5VrHUkvOW5jvDmsH5PLHvneg4582Q0PsGqilNw07b6vONCDE/HUVDi+l4u1Cthw2zD8hm6b+MNg7KQfqDFKSfYefZy2d9URKJSF4QXuBtZ0Q5sCsCXZjJ6rqIipV2fYa0SDqbiyfV2fESXL8A4jH+dgXEcjlxvLUtsI4nLIT9rvUfQDQIDAQAB";
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

        System.out.println("Econ: EconActivity onCreate ENTER");

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

                EconCrash(Log.getStackTraceString(ex));

            }
        });

        super.onCreate(savedInstanceState);

        if (getActionBar() != null) {
            getActionBar().hide();
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        System.out.println("Econ: EconActivity onCreate EXIT");
    }

    protected void EconCrash(String message) {
        System.out.println("Econ: EconActivity EconCrash");
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
        //System.out.println("Econ: EconActivity onWindowFocusChanged ENTER");
        if (!getLocalClassName().equals("StartActivity")){
            String NomeDittaSelezionata = Sessione.getNomeDittaSelezionata();
            impostaTestoAzienda(NomeDittaSelezionata);
            impostaTestoOperatore(Sessione.getNomeOperatore());
            //System.out.println("Econ: EconActivity onWindowFocusChanged NomeDittaSelezionata " + NomeDittaSelezionata);
            // TODO Auto-generated method stub
            if (!backgroundComplete && getBackgroundID() > 0) {
                View cont = findViewById(R.id.container);
                if (cont != null) {
                    // cont.setBackgroundResource(getBackgroundID());
                }
                backgroundComplete = true;
            }

            //System.out.println("Econ: EconActivity onWindowFocusChanged 1");

            if (!scalingComplete && eseguiRidimensionamento()) // only do this once
            {
                View content = findViewById(R.id.content);
                Utility.scaleContents(content, findViewById(R.id.container), ridimensionaXY());
                scalingComplete = true;
            }

            //System.out.println("Econ: EconActivity onWindowFocusChanged 2");

            if (sceltaAziendaComplete == false) {
                //System.out.println("Econ: EconActivity onWindowFocusChanged 3");
                sceltaAziendaComplete = true;
                DbInterno db = new DbInterno(this);

                //System.out.println("Econ: EconActivity onWindowFocusChanged 4");

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
                    //System.out.println("Econ: EconActivity onWindowFocusChanged 5");
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

                        Utility.mostraSelezioneDialog(getString(R.string.selezione_azienda), items, EconActivity.this,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // TODO Auto-generated method stub
                                        Toast.makeText(EconActivity.this, "Azienda selezionata: " + items[which], Toast.LENGTH_SHORT).show();
                                        Sessione.setNomeDittaSelezionata(items[which]);
                                        Sessione.setDittaSelezionata(((ContentValues) aziende.get(which)).getAsInteger(Ditte.ID_DITTA));
                                        Intent intent = new Intent(EconActivity.this, MenuActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                        EconActivity.this.finish();
                                    }
                                });

                    } else {
                        //System.out.println("Econ: EconActivity onWindowFocusChanged 6");
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
                //System.out.println("Econ: EconActivity onWindowFocusChanged sceltaAziendaComplete TRUE");
            }
        }

        //System.out.println("Econ: EconActivity onWindowFocusChanged EXIT");
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        System.out.println("Econ: EconActivity onCreateOptionsMenu ENTER");
        // TODO Auto-generated method stub
        if (getMenuID() > 0) {
            getMenuInflater().inflate(getMenuID(), menu);

            boolean hasMercuryToken = TokenManager.getInstance(this).hasToken();
            if (!hasMercuryToken) {
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

        System.out.println("Econ: EconActivity onCreateOptionsMenu EXIT");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        System.out.println("Econ: EconActivity onOptionsItemSelected ENTER");
        // TODO Auto-generated method stub

        if (item.getItemId() == R.id.item_menu) {
            System.out.println("Econ: EconActivity onOptionsItemSelected item_menu");
            Intent intent = new Intent(this, MenuActivity.class);
            //intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.item_sinc) {
            System.out.println("Econ: EconActivity onOptionsItemSelected item_sinc");
            Intent intent = new Intent(this, ConfigurazioneGenActivity.class);
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.item_logout) {
            System.out.println("Econ: EconActivity onOptionsItemSelected item_logout");
            richiediConfermaLogout();
            return true;
        }

        if (item.getItemId() == R.id.item_about) {
            System.out.println("Econ: EconActivity onOptionsItemSelected item_about");
            Utility.mostraDialog(getString(R.string.versione_installata),
                    com.ncfsistemi.econ.utils.VersioneApp.etichetta(this) + "\n" + com.ncfsistemi.econ.utils.Editore.nome(this),
                    this, getString(R.string.chiudi));
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
        return R.menu.econ_menu;
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
        //System.out.println("Econ: EconActivity setText");
        View v = null;
        if (view == null) {
            v = findViewById(id);
        } else {
            v = view.findViewById(id);
        }
        if (v instanceof TextView) {
            ((TextView) v).setText(text);
        }
        if (v instanceof EconSpecialView) {
            ((EconSpecialView) v).setValue(text);
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
        if (v instanceof EconSpecialView) {
            return ((EconSpecialView) v).getValue();
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
        System.out.println("Econ: EconActivity apriFinestraModifica");
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
        System.out.println("Econ: EconActivity apriFinestraInserimento");
        // Nuovi clienti, cantieri, listini, preventivi, ordini e rapportini solo con la funzionalita' CREA del pacchetto
        // (il server li rifiuterebbe); per i preventivi il tipo (ordine o preventivo) arriva nell'intent
        if (!com.ncfsistemi.econ.utils.FunzionalitaApp.puoInserire(this, tabella,
                intent != null ? intent.getStringExtra(com.ncfsistemi.econ.db.table.Preventivi.TIPO) : null)) {
            Utility.mostraDialog("Non consentito",
                    "La tua licenza non consente questo inserimento per il tuo ruolo. Chiedi all'amministratore della ditta.",
                    this, "OK");
            return;
        }
        if (resultCode > 0) {
            startActivityForResult(intent, resultCode);
        } else {
            startActivity(intent);
        }
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
        // modifiche fatte offline e non ancora inviate: uscendo non si potra' piu' lavorare offline (ACCESSO_OFFLINE.md §2.5)
        int nonInviate = com.ncfsistemi.econ.utils.DatiLocali.modificheNonInviate(this);
        String messaggio = nonInviate == 0 ? getString(R.string.conferma_disconnessione)
                : "Sul tablet ci sono " + nonInviate + " modifiche non ancora inviate al server. Restano sul tablet, ma "
                + "per inviarle dovrai accedere di nuovo con questo account e, finché non ti ricolleghi, non potrai "
                + "lavorare offline.\n\nUscire comunque?";
        Utility.mostraConfermaDialog(getString(R.string.attenzione), messaggio, this, "OK",
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
        com.ncfsistemi.econ.utils.DittaLocale.cancella(this);
        com.ncfsistemi.econ.utils.CatalogoLocale.cancella(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void confermaCancellazione(final AbstractTable tabella, final ContentValues val, final boolean reload) {
        // TODO Auto-generated method stub
        System.out.println("Econ: EconActivity confermaCancellazione ENTER");
        // Clienti, cantieri, listini, preventivi, ordini e rapportini: solo con la funzionalita' ELIMINA del pacchetto
        if (!com.ncfsistemi.econ.utils.FunzionalitaApp.puoEliminare(this, tabella, val)) {
            Utility.mostraDialog("Non consentito", getString(R.string.eliminazione_non_consentita), this, "OK");
            return;
        }
        Utility.mostraConfermaCancellazioneDialog(this, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    DbInterno db = new DbInterno(EconActivity.this);
                    db.getReadableDatabase().beginTransaction();

                    int deleteresult = -1;
                    try {
                            if (tabella.cancellazionePossibile(db, val, EconActivity.this)) {
                                deleteresult = tabella.cancellaRecord(db, val);
                        }
                        db.getReadableDatabase().setTransactionSuccessful();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        Utility.mostraDialog("Errore", Log.getStackTraceString(e), EconActivity.this, "OK");
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
        System.out.println("Econ: EconActivity aggiornaDopoCancellazione");
        // TODO Auto-generated method stub
        Intent intent = new Intent(EconActivity.this, EconActivity.this.getClass());
        if (EconActivity.this.getIntent().getExtras() != null) {
            intent.putExtras(EconActivity.this.getIntent().getExtras());
        }
        startActivity(intent);
        EconActivity.this.finish();
    }

    /** Schermata in primo piano, per l'avviso di ricollegamento al server (utils.Riconnessione). */
    private static java.lang.ref.WeakReference<EconActivity> inPrimoPiano = new java.lang.ref.WeakReference<>(null);

    public static EconActivity inPrimoPiano() {
        return inPrimoPiano.get();
    }

    @Override
    protected void onPause() {
        if (inPrimoPiano.get() == this) inPrimoPiano.clear();
        super.onPause();
    }

    /** Le schermate di accesso gestiscono da sole il collegamento: niente avvisi li' (sincronizzazione e cambio password non sono EconActivity). */
    private boolean gestisceConnessione() {
        return this instanceof StartActivity || this instanceof LoginActivity;
    }

    /**
     * Il server e' tornato raggiungibile dopo un periodo offline (ACCESSO_OFFLINE.md §2.4): con modifiche da inviare si
     * propone l'invio (che poi scarica anche gli aggiornamenti), altrimenti basta un avviso.
     */
    public void notificaRicollegamento() {
        runOnUiThread(() -> {
            // non adatta: l'avviso resta in attesa della prossima schermata
            if (isFinishing() || isDestroyed() || gestisceConnessione()) return;
            if (!com.ncfsistemi.econ.utils.Riconnessione.consumaNotifica()) return;
            int nonInviate = com.ncfsistemi.econ.utils.DatiLocali.modificheNonInviate(this);
            if (nonInviate == 0) {
                Toast.makeText(this, "Di nuovo collegato al server.", Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Di nuovo collegato al server")
                    .setMessage("Hai " + nonInviate + (nonInviate == 1 ? " modifica fatta" : " modifiche fatte")
                            + " senza collegamento. Inviarle adesso? Dopo l'invio si scaricano anche gli aggiornamenti.")
                    .setPositiveButton("Invia ora", (d, w) -> {
                        Intent sync = new Intent(this, SincronizzazioneActivity.class);
                        sync.putExtra(SincronizzazioneActivity.EXTRA_MODE, SincronizzazioneActivity.MODE_UPLOAD);
                        sync.putExtra(SincronizzazioneActivity.EXTRA_AUTOMATICO, true);
                        startActivity(sync);
                    })
                    .setNegativeButton("Più tardi", (d, w) -> Toast.makeText(this,
                            "Le modifiche restano sul tablet: inviale da Sincronizza.", Toast.LENGTH_LONG).show())
                    .show();
        });
    }

    @Override
    protected void onResume() {
        System.out.println("Econ: EconActivity onResume ENTER");
        // TODO Auto-generated method stub
        super.onResume();
        if (getLocalClassName().equals("StartActivity")) {
            return;
        }
        inPrimoPiano = new java.lang.ref.WeakReference<>(this);
        if (!gestisceConnessione()) {
            if (com.ncfsistemi.econ.utils.Sessione.isOffline()) {
                com.ncfsistemi.econ.utils.Riconnessione.avvia(this);
                com.ncfsistemi.econ.utils.Riconnessione.verifica(this);
            } else {
                // ricollegamento avvenuto mentre nessuna schermata adatta era in primo piano
                notificaRicollegamento();
            }
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

        // Accesso: serve il token di Mercury (le licenze sono sul server, ACCESSI_E_LICENZE.md); senza si va al login
        if ((isControllaRegistrazione() || isControllaLogin())
                && !com.ncfsistemi.econ.api.TokenManager.getInstance(this).hasToken()) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        System.out.println("Econ: EconActivity onResume EXIT");

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
     * EconDettaglioActivity sovrascrive questo metodo per impostare RESULT_CANCELED). Nelle
     * pagine di sola visualizzazione (non EconDettaglioActivity) equivale a indietro().
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
            com.ncfsistemi.econ.api.MercuryApiClient.invalidate();
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
    public void impostaFragment(int container, EconFragment fragment, boolean registraIndietro) {
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
    public void impostaFragment(int container, EconFragment fragment) {
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

    public class EconAsyncTask extends AsyncTask<Void, Integer, String> {
        AlertDialog di = null;

        private boolean mostraAttesa = false;

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            if (isMostraAttesa()) {
                AlertDialog.Builder ab = new AlertDialog.Builder(EconActivity.this);
                ab.setMessage(EconActivity.this.getResources().getString(R.string.messaggio_caricamento_in_corso));
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
