package pfa.app.econtab;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import pfa.app.econtab.api.MercuryApiClient;
import pfa.app.econtab.api.MercuryApiService;
import pfa.app.econtab.api.TokenManager;
import pfa.app.econtab.server.ConfigurazioneGenActivity;
import pfa.app.econtab.utils.AsyncTaskExecutorService;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.SyncUtil;
import pfa.app.econtab.utils.Utility;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuActivity extends EConTabActivity {

    // ── Definizione voci di menu ──────────────────────────────────────────────
    // Il "codice" deve combaciare con quello dei moduli di tipo app del catalogo in Sonata
    // (Amministrazione → Moduli): il server concede le voci in base ai pacchetti licenza app
    // assegnati all'utente, e l'app le riceve a ogni apertura (SincronizzazioneActivity, avvio).

    private static class MenuItemDef {
        final int buttonId;
        final int drawableRes;
        final int labelRes;
        final String codice;

        MenuItemDef(int buttonId, int drawableRes, int labelRes, String codice) {
            this.buttonId    = buttonId;
            this.drawableRes = drawableRes;
            this.labelRes    = labelRes;
            this.codice      = codice;
        }
    }

    private List<MenuItemDef> getTutteLeVociMenu() {
        List<MenuItemDef> items = new ArrayList<>();
        items.add(new MenuItemDef(R.id.buttonClienti,      R.drawable.button_clienti,       R.string.clienti,        "CLIENTI"));
        items.add(new MenuItemDef(R.id.buttonCantieri,     R.drawable.button_cantieri,      R.string.cantieri,       "CANTIERI"));
        items.add(new MenuItemDef(R.id.buttonListini,      R.drawable.button_listini,       R.string.listini,        "LISTINI"));
        items.add(new MenuItemDef(R.id.buttonPreventivi,   R.drawable.button_preventivi,    R.string.preventivi,     "PREVENTIVI"));
        items.add(new MenuItemDef(R.id.buttonOrdini,       R.drawable.button_ordini,        R.string.ordini,         "ORDINI"));
        items.add(new MenuItemDef(R.id.buttonRapportini,   R.drawable.button_rapportini,    R.string.rapportini,     "RAPPORTINI"));
        items.add(new MenuItemDef(R.id.buttonStatoSistema, R.drawable.button_stato_sistema, R.string.stato_sistema,  "STATO_SISTEMA"));
        items.add(new MenuItemDef(R.id.buttonImpostazioni, R.drawable.button_impostazioni,  R.string.impostazioni,   "IMPOSTAZIONI"));
        items.add(new MenuItemDef(R.id.buttonGuida,        R.drawable.button_guida,         R.string.guida,          "GUIDA"));
        items.add(new MenuItemDef(R.id.buttonSync,         R.drawable.button_sincronizza,   R.string.sincronizza,    "SINCRONIZZA"));
        return items;
    }

    /**
     * Voci di menu effettivamente da mostrare: filtrate sui moduli concessi all'utente
     * dai pacchetti licenza app (Sonata → Licenze → Pacchetti ditte). Se il
     * server non ha mai inviato la lista (login legacy, o server non aggiornato)
     * si mostra tutto per compatibilità, invece di un menu vuoto.
     */
    private List<MenuItemDef> getMenuItems() {
        List<MenuItemDef> tutte = getTutteLeVociMenu();

        TokenManager tm = TokenManager.getInstance(this);
        if (tm.moduliMaiRicevuti()) {
            return tutte;
        }

        List<String> abilitati = tm.getModuli();
        List<MenuItemDef> visibili = new ArrayList<>();
        for (MenuItemDef item : tutte) {
            if (abilitati.contains(item.codice)) {
                visibili.add(item);
            }
        }
        return visibili;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Ripara i dati scaricati da versioni precedenti del server: date in testo ("yyyy-MM-dd HH:mm:ss" invece
     * di yyyyMMddHHmmss) e valori NULL (l'app vuole '' o 0). In entrambi i casi l'app andava in crash.
     * Idempotente e veloce.
     */
    private void riparaDateLocali() {
        try {
            DbInterno db = new DbInterno(this);
            try {
                android.database.sqlite.SQLiteDatabase sqlite = db.getWritableDatabase();
                SyncUtil.normalizzaNulliLocali(sqlite);
                int tabelle = SyncUtil.normalizzaDateLocali(sqlite);
                if (tabelle > 0) {
                    System.out.println("EConTab: MenuActivity riparate date testuali in " + tabelle + " tabelle");
                }
            } finally {
                db.close();
            }
        } catch (Exception e) {
            System.out.println("EConTab: MenuActivity riparaDateLocali errore: " + e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("EConTab: MenuActivity onCreate ENTER");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        costruisciGriglia();
        riparaDateLocali();
        // a ogni login/apertura il listino si riallinea agli articoli della ditta (in background, senza avvisi)
        pfa.app.econtab.utils.CatalogoLocale.allineaInBackground(this);

        SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        if (!pref.contains("PERINIZIARE") && pref.contains("ECONTAB_REG")) {
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("PERINIZIARE", "1");
            editor.apply();

            findViewById(R.id.footer).postDelayed(() -> {
                AlertDialog.Builder ab = new AlertDialog.Builder(MenuActivity.this);
                ab.setTitle(getString(R.string.periniziare));
                String[] opzioni = {
                    getString(R.string.videoguide),
                    getString(R.string.faq),
                    getString(R.string.guidapdf)
                };
                ab.setItems(opzioni, (d, i) -> selezioneGuida(i));
                ab.setIcon(android.R.drawable.ic_dialog_info);
                ab.setNeutralButton(getString(R.string.chiudi),
                    (d, i) -> Utility.mostraDialog("", getString(R.string.messaggio_guida), MenuActivity.this, "OK"));
                AlertDialog di = ab.create();
                di.show();
                di.setCancelable(true);
                di.setCanceledOnTouchOutside(true);
                di.setOnCancelListener(
                    d -> Utility.mostraDialog("", getString(R.string.messaggio_guida), MenuActivity.this, "OK"));
            }, 2000);
        }

        System.out.println("EConTab: MenuActivity onCreate EXIT");
    }

    // ── Costruzione griglia adattiva ─────────────────────────────────────────

    private void costruisciGriglia() {
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        int numColonne    = screenWidthDp >= 480 ? 3 : 2;

        LinearLayout content  = findViewById(R.id.content);
        content.removeAllViews();

        List<MenuItemDef> items     = getMenuItems();
        int               numRighe  = (int) Math.ceil((double) items.size() / numColonne);
        LayoutInflater    inflater  = LayoutInflater.from(this);

        for (int r = 0; r < numRighe; r++) {
            LinearLayout riga       = new LinearLayout(this);
            LinearLayout.LayoutParams rigaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
            riga.setLayoutParams(rigaParams);
            riga.setOrientation(LinearLayout.HORIZONTAL);

            for (int c = 0; c < numColonne; c++) {
                int idx = r * numColonne + c;
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);

                if (idx < items.size()) {
                    View cell = inflater.inflate(R.layout.item_menu_principale, null, false);
                    cell.setLayoutParams(cellParams);
                    impostaVoceMenu(cell, items.get(idx));
                    riga.addView(cell);
                } else {
                    View spacer = new View(this);
                    spacer.setLayoutParams(cellParams);
                    riga.addView(spacer);
                }
            }
            content.addView(riga);
        }
    }

    private void impostaVoceMenu(View cell, MenuItemDef item) {
        Button   btn   = cell.findViewById(R.id.menuItemButton);
        TextView label = cell.findViewById(R.id.menuItemLabel);
        btn.setBackgroundResource(item.drawableRes);
        btn.setId(item.buttonId);
        btn.setOnClickListener(this::apriFunzione);
        label.setText(item.labelRes);
    }

    // ── Navigazione ───────────────────────────────────────────────────────────

    public void apriFunzione(View v) {
        System.out.println("EConTab: MenuActivity apriFunzione ENTER");
        int id = v.getId();

        if (id == R.id.buttonClienti) {
            startActivity(new Intent(this, ClientiActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        } else if (id == R.id.buttonCantieri) {
            startActivity(new Intent(this, CantieriActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        } else if (id == R.id.buttonListini) {
            startActivity(new Intent(this, FornitoriLineeActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        } else if (id == R.id.buttonImpostazioni) {
            startActivity(new Intent(this, ConfigurazioneActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        } else if (id == R.id.buttonPreventivi) {
            startActivity(new Intent(this, PreventiviActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        } else if (id == R.id.buttonOrdini) {
            startActivity(new Intent(this, OrdiniActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        } else if (id == R.id.buttonRapportini) {
            startActivity(new Intent(this, RapportiniActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        } else if (id == R.id.buttonStatoSistema) {
            startActivity(new Intent(this, StatoSistemaActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        } else if (id == R.id.buttonGuida) {
            String[] opzioni = {
                getString(R.string.videoguide),
                getString(R.string.faq),
                getString(R.string.guidapdf),
                getString(R.string.richiedi_assistenza)
            };
            Utility.mostraSelezioneDialog(getString(R.string.serve_aiuto), opzioni, this,
                (dialog, i) -> selezioneGuida(i));
        } else if (id == R.id.buttonSync) {
            startActivity(new Intent(this, ConfigurazioneGenActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        }

        System.out.println("EConTab: MenuActivity apriFunzione EXIT");
    }

    // ── Guida ─────────────────────────────────────────────────────────────────

    private void selezioneGuida(int i) {
        // TODO: implementare navigazione guida
        System.out.println("EConTab: MenuActivity selezioneGuida " + i);
    }

    // ── Scaling (non necessario: layout responsivo) ───────────────────────────

    @Override
    protected boolean eseguiRidimensionamento() { return false; }

    @Override
    protected boolean ridimensionaXY() { return false; }

    // ── onResume: LED server + check aggiornamenti ────────────────────────────

    @Override
    protected void onResume() {
        System.out.println("EConTab: MenuActivity onResume ENTER");
        super.onResume();

        aggiornaOggi();
        verificaConnessioneServer();

        SharedPreferences pref = getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE);
        String dataultimaVerifica = pref.getString("DATA_VERIFICA_AGGIORNAMERNTI", "01/01/1970");

        long ultimaVerificaNumber = Utility.dataToNumber(dataultimaVerifica);
        Calendar oggi = Calendar.getInstance();
        oggi.set(Calendar.HOUR_OF_DAY, 0);
        oggi.set(Calendar.MINUTE, 0);
        oggi.set(Calendar.SECOND, 0);
        long oggiNumber = Utility.dataToNumber(oggi);

        if (ultimaVerificaNumber > oggiNumber) {
            Calendar ieri = Calendar.getInstance();
            ieri.add(Calendar.DATE, -1);
            ieri.set(Calendar.HOUR_OF_DAY, 0);
            ieri.set(Calendar.MINUTE, 0);
            ieri.set(Calendar.SECOND, 0);
            ultimaVerificaNumber = Utility.dataToNumber(ieri);
        }

        if (oggiNumber > ultimaVerificaNumber && Utility.isOnline(this)) {
            AsyncTaskExecutorService<Void, Integer, String> task =
                new AsyncTaskExecutorService<Void, Integer, String>() {
                    @Override
                    protected String doInBackground(Void unused) {
                        try {
                            String url = pfa.app.econtab.Globals.LICENSE_URL_SERVER + "/mobileapp/version?type=Android";
                            return Utility.getStringaDaPaginaWeb(url);
                        } catch (Exception e) {
                            return "NO";
                        }
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        SharedPreferences.Editor editor =
                            getSharedPreferences(Utility.APP_NAME, Context.MODE_PRIVATE).edit();
                        editor.putString("DATA_VERIFICA_AGGIORNAMERNTI",
                            Utility.dataToString(Calendar.getInstance()));
                        editor.apply();
                        if (result != null && !result.trim().equals("NO")) {
                            try {
                                PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                                int newVersion = Integer.parseInt(result.trim());
                                if (newVersion > pinfo.versionCode) {
                                    Utility.mostraConfermaDialog("",
                                        "E' disponibile una nuova versione dell'app. Aggiorna adesso!",
                                        MenuActivity.this, "Aggiorna", "No, grazie",
                                        (dialog, which) -> {
                                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                                Utility.aggiornaApp(MenuActivity.this);
                                            }
                                        });
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                };
            task.execute();
        }

        System.out.println("EConTab: MenuActivity onResume EXIT");
    }

    // ── LED connessione server ────────────────────────────────────────────────

    private void verificaConnessioneServer() {
        View led = findViewById(R.id.led_server);
        if (led != null) led.setBackgroundResource(R.drawable.ic_led_gray);
        impostaTestoLed("Verifica connessione...");

        String url = Utility.getURLServer(this);
        new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()
            .newCall(new okhttp3.Request.Builder().url(url).build())
            .enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    runOnUiThread(() -> {
                        View l = findViewById(R.id.led_server);
                        if (l != null) l.setBackgroundResource(R.drawable.ic_led_red);
                        impostaTestoLed("Disconnesso");
                    });
                }
                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                    response.close();
                    runOnUiThread(() -> {
                        View l = findViewById(R.id.led_server);
                        if (l != null) l.setBackgroundResource(R.drawable.ic_led_green);
                        impostaTestoLed("Connesso");
                    });
                }
            });
    }

    private void impostaTestoLed(String testo) {
        TextView tv = findViewById(R.id.testoLedServer);
        if (tv != null) tv.setText(testo);
    }

    /**
     * L'operatore nel menu principale si mostra nella barra in alto (accanto
     * all'iconcina account), non più nel footer: qui non c'è più spazio dopo
     * aver tolto nome utente e ditta dal footer.
     */
    @Override
    protected void impostaTestoOperatore(String operatore) {
        super.impostaTestoOperatore(operatore);
        TextView tv = findViewById(R.id.textViewUtenteTopBar);
        if (tv != null) tv.setText(operatore == null ? "" : operatore);
    }

    // ── Micro dashboard: cantieri di oggi ─────────────────────────────────────

    /** Blocchi di oggi dell'ultima lettura, per il riquadro che si apre toccando la riga. */
    private ArrayList<Object> blocchiOggi = new ArrayList<>();

    /**
     * Una riga sotto la barra in alto con i cantieri pianificati oggi per l'utente collegato (dalla pianificazione
     * scaricata in sync), e la data di oggi nella barra blu. Riga nascosta senza la funzionalita' DASHBOARD.OGGI o se
     * l'elenco operatori non e' ancora stato scaricato.
     */
    private void aggiornaOggi() {
        Calendar oggi = Calendar.getInstance();
        TextView tvData = findViewById(R.id.textDataOggi);
        if (tvData != null) {
            String data = String.format(java.util.Locale.ITALY, "%1$tA %1$td/%1$tm", oggi);
            tvData.setText(data.substring(0, 1).toUpperCase(java.util.Locale.ITALY) + data.substring(1));
        }

        View riga = findViewById(R.id.rigaOggi);
        TextView tv = findViewById(R.id.textOggi);
        if (riga == null || tv == null) return;
        // pannello "Cantieri di oggi": funzionalita' DASHBOARD.OGGI del pacchetto per il ruolo
        if (!pfa.app.econtab.utils.FunzionalitaApp.ha(this, pfa.app.econtab.utils.FunzionalitaApp.DASHBOARD_OGGI)) {
            riga.setVisibility(View.GONE);
            return;
        }
        pfa.app.econtab.utils.RegoleRapportino.Utente utente = utenteCorrente();
        if (utente == null || utente.idUtenteDitta == 0) {
            riga.setVisibility(View.GONE);
            return;
        }
        ArrayList<Object> blocchi = pianificazione(utente, Utility.dataToNumber(oggi));

        StringBuilder testo = new StringBuilder("Pianificato per oggi: ");
        blocchiOggi = blocchi;
        for (int i = 0; i < blocchi.size(); i++) {
            android.content.ContentValues b = (android.content.ContentValues) blocchi.get(i);
            if (i > 0) testo.append(" · ");
            testo.append(fascia(b)).append(": ").append(b.getAsString("nome_cantiere")).append(" ").append(durata(b.getAsInteger("minuti")));
        }
        if (blocchi.isEmpty()) {
            testo.append("nessun cantiere");
        }
        tv.setText(testo.toString());
        riga.setVisibility(View.VISIBLE);
    }

    private pfa.app.econtab.utils.RegoleRapportino.Utente utenteCorrente() {
        DbInterno db = new DbInterno(this);
        try {
            return pfa.app.econtab.utils.RegoleRapportino.utenteCorrente(db, this);
        } catch (Exception e) {
            return null; // tabelle non ancora presenti
        } finally {
            db.close();
        }
    }

    private ArrayList<Object> pianificazione(pfa.app.econtab.utils.RegoleRapportino.Utente utente, long giorno) {
        DbInterno db = new DbInterno(this);
        try {
            return pfa.app.econtab.utils.RegoleRapportino.pianificazioneGiorno(db, utente, giorno);
        } catch (Exception e) {
            return new ArrayList<>(); // pianificazione non ancora scaricata
        } finally {
            db.close();
        }
    }

    private static String fascia(android.content.ContentValues b) {
        return "M".equals(b.getAsString("fascia")) ? "Mattina" : "Pomeriggio";
    }

    /** "Cantiere — Cliente", piu' "Squadra: X" per i blocchi di squadra. */
    private static String descrizioneBlocco(android.content.ContentValues b) {
        String cliente = b.getAsString("cliente");
        String squadra = b.getAsString("squadra");
        return b.getAsString("nome_cantiere")
                + (cliente != null && !cliente.trim().isEmpty() ? " — " + cliente.trim() : "")
                + (squadra != null && !squadra.trim().isEmpty() ? "\nSquadra: " + squadra.trim() : "");
    }

    // ── Calendario della settimana ────────────────────────────────────────────

    /** Icona calendario: pianificazione dell'utente collegato, settimana per settimana (lunedi-domenica). */
    public void apriCalendarioSettimana(View v) {
        pfa.app.econtab.utils.RegoleRapportino.Utente utente = utenteCorrente();
        if (utente == null || utente.idUtenteDitta == 0) {
            Toast.makeText(this, "Elenco operatori non ancora scaricato: esegui una sincronizzazione.", Toast.LENGTH_LONG).show();
            return;
        }
        float dp = getResources().getDisplayMetrics().density;

        LinearLayout contenuto = new LinearLayout(this);
        contenuto.setOrientation(LinearLayout.VERTICAL);

        LinearLayout testata = new LinearLayout(this);
        testata.setOrientation(LinearLayout.HORIZONTAL);
        testata.setGravity(android.view.Gravity.CENTER_VERTICAL);
        testata.setPadding((int) (8 * dp), (int) (8 * dp), (int) (8 * dp), (int) (4 * dp));
        Button prec = new Button(this, null, android.R.attr.borderlessButtonStyle);
        prec.setText("‹");
        prec.setTextSize(22);
        Button succ = new Button(this, null, android.R.attr.borderlessButtonStyle);
        succ.setText("›");
        succ.setTextSize(22);
        TextView titolo = new TextView(this);
        titolo.setGravity(android.view.Gravity.CENTER);
        titolo.setTextSize(17);
        titolo.setTypeface(null, android.graphics.Typeface.BOLD);
        titolo.setTextColor(0xFF0D47A1);
        testata.addView(prec);
        testata.addView(titolo, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        testata.addView(succ);
        contenuto.addView(testata);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout giorni = new LinearLayout(this);
        giorni.setOrientation(LinearLayout.VERTICAL);
        giorni.setPadding((int) (16 * dp), 0, (int) (16 * dp), (int) (8 * dp));
        scroll.addView(giorni);
        contenuto.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        final int[] spostamento = { 0 }; // settimane rispetto a quella corrente
        Runnable disegna = () -> disegnaSettimana(utente, spostamento[0], titolo, giorni, dp);
        prec.setOnClickListener(x -> { spostamento[0]--; disegna.run(); });
        succ.setOnClickListener(x -> { spostamento[0]++; disegna.run(); });
        disegna.run();

        new AlertDialog.Builder(this)
                .setView(contenuto)
                .setPositiveButton("Chiudi", null)
                .show();
    }

    private void disegnaSettimana(pfa.app.econtab.utils.RegoleRapportino.Utente utente, int spostamento,
                                  TextView titolo, LinearLayout giorni, float dp) {
        Calendar oggi = Calendar.getInstance();
        Calendar giorno = Calendar.getInstance();
        giorno.add(Calendar.DATE, -((giorno.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 7 * spostamento); // lunedi
        Calendar domenica = (Calendar) giorno.clone();
        domenica.add(Calendar.DATE, 6);
        titolo.setText(String.format(java.util.Locale.ITALY, "Settimana %1$td/%1$tm – %2$td/%2$tm", giorno, domenica));

        giorni.removeAllViews();
        for (int i = 0; i < 7; i++, giorno.add(Calendar.DATE, 1)) {
            boolean eOggi = giorno.get(Calendar.YEAR) == oggi.get(Calendar.YEAR) && giorno.get(Calendar.DAY_OF_YEAR) == oggi.get(Calendar.DAY_OF_YEAR);
            ArrayList<Object> blocchi = pianificazione(utente, Utility.dataToNumber(giorno));

            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding((int) (10 * dp), (int) (6 * dp), (int) (10 * dp), (int) (6 * dp));
            if (eOggi) box.setBackgroundColor(0xFFE3F2FD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (4 * dp);

            TextView nome = new TextView(this);
            String etichetta = String.format(java.util.Locale.ITALY, "%1$tA %1$td/%1$tm", giorno);
            nome.setText(etichetta.substring(0, 1).toUpperCase(java.util.Locale.ITALY) + etichetta.substring(1) + (eOggi ? "  (oggi)" : ""));
            nome.setTypeface(null, android.graphics.Typeface.BOLD);
            nome.setTextColor(blocchi.isEmpty() && i >= 5 ? 0xFF9E9E9E : 0xFF212121);
            box.addView(nome);

            if (blocchi.isEmpty()) {
                TextView vuoto = new TextView(this);
                vuoto.setText("—");
                vuoto.setTextColor(0xFF9E9E9E);
                box.addView(vuoto);
            }
            for (Object o : blocchi) {
                android.content.ContentValues b = (android.content.ContentValues) o;
                box.addView(vistaBlocco(b, fascia(b) + " · " + durata(b.getAsInteger("minuti")) + "  " + descrizioneBlocco(b).replace("\n", " · "), dp));
            }
            giorni.addView(box, lp);
        }
    }

    /** 240 → "4h", 90 → "1h30", 30 → "30m". */
    private static String durata(Integer minuti) {
        int m = minuti != null ? minuti : 0;
        if (m < 60) return m + "m";
        return (m / 60) + "h" + (m % 60 > 0 ? String.format(java.util.Locale.ITALY, "%02d", m % 60) : "");
    }

    /** Tocco sulla riga di oggi: dettaglio con cliente e squadra di ogni blocco; tocco sul cantiere = mappa. */
    public void apriDettaglioOggi(View v) {
        float dp = getResources().getDisplayMetrics().density;
        LinearLayout elenco = new LinearLayout(this);
        elenco.setOrientation(LinearLayout.VERTICAL);
        elenco.setPadding((int) (16 * dp), (int) (8 * dp), (int) (16 * dp), 0);
        if (blocchiOggi.isEmpty()) {
            TextView vuoto = new TextView(this);
            vuoto.setText("Nessun cantiere pianificato per oggi.\nLa pianificazione arriva con la sincronizzazione.");
            elenco.addView(vuoto);
        }
        for (Object o : blocchiOggi) {
            android.content.ContentValues b = (android.content.ContentValues) o;
            elenco.addView(vistaBlocco(b, fascia(b) + " · " + durata(b.getAsInteger("minuti")) + "\n" + descrizioneBlocco(b), dp));
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(elenco);
        new AlertDialog.Builder(this)
                .setTitle("I tuoi cantieri di oggi")
                .setView(scroll)
                .setPositiveButton("Chiudi", null)
                .show();
    }

    /**
     * Riga di un blocco di pianificazione: testo e, se il cantiere ha un indirizzo, icona mappa; il tocco apre
     * Google Maps (o un'altra app di mappe) sull'indirizzo del cantiere.
     */
    private View vistaBlocco(android.content.ContentValues b, String testo, float dp) {
        LinearLayout riga = new LinearLayout(this);
        riga.setOrientation(LinearLayout.HORIZONTAL);
        riga.setGravity(android.view.Gravity.CENTER_VERTICAL);
        riga.setPadding((int) (8 * dp), (int) (6 * dp), (int) (4 * dp), (int) (6 * dp));

        TextView t = new TextView(this);
        t.setText(testo);
        t.setTextColor(0xFF0D47A1);
        t.setTextSize(15);
        riga.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final String indirizzo = indirizzoCantiere(b);
        if (indirizzo != null) {
            ImageView mappa = new ImageView(this);
            mappa.setImageResource(R.drawable.ic_mappa);
            mappa.setContentDescription("Apri nella mappa");
            riga.addView(mappa, new LinearLayout.LayoutParams((int) (28 * dp), (int) (28 * dp)));
            android.util.TypedValue sfondo = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, sfondo, true);
            riga.setBackgroundResource(sfondo.resourceId);
            riga.setOnClickListener(x -> apriMappa(indirizzo));
        }
        return riga;
    }

    /** "Via, CAP Citta (PR)" dai campi del cantiere, null se manca l'indirizzo. */
    private static String indirizzoCantiere(android.content.ContentValues b) {
        String via = b.getAsString("indirizzo");
        if (via == null || via.trim().isEmpty()) return null;
        StringBuilder sb = new StringBuilder(via.trim());
        String cap = b.getAsString("cap"), citta = b.getAsString("citta"), prov = b.getAsString("provincia");
        String luogo = ((cap != null ? cap.trim() : "") + " " + (citta != null ? citta.trim() : "")).trim();
        if (!luogo.isEmpty()) sb.append(", ").append(luogo);
        if (prov != null && !prov.trim().isEmpty()) sb.append(" (").append(prov.trim()).append(")");
        return sb.toString();
    }

    /** Apre l'indirizzo in Google Maps, o in un'altra app di mappe se Google Maps non c'e'. */
    private void apriMappa(String indirizzo) {
        android.net.Uri uri = android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(indirizzo));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (android.content.ActivityNotFoundException e2) {
                Toast.makeText(this, "Nessuna app di mappe installata", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ── Modale info account ───────────────────────────────────────────────────

    /** Icona utente nella barra in basso: mostra utente/ditta/versioni/licenza in sola lettura. */
    public void apriInfoUtente(View v) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View contenuto = inflater.inflate(R.layout.dialog_profilo, null, false);

        TextView tvUtente    = contenuto.findViewById(R.id.tvProfiloUtente);
        TextView tvDitta     = contenuto.findViewById(R.id.tvProfiloDitta);
        TextView tvVersioni  = contenuto.findViewById(R.id.tvProfiloVersioni);
        TextView tvLicenza   = contenuto.findViewById(R.id.tvProfiloLicenza);
        TextView tvPassword  = contenuto.findViewById(R.id.tvProfiloPassword);
        TextView tvScadPw    = contenuto.findViewById(R.id.tvProfiloScadenzaPassword);
        TextView tvPacchetto = contenuto.findViewById(R.id.tvProfiloPacchetto);
        tvPacchetto.setText(nomiPacchetti());
        ImageView ivLogo     = contenuto.findViewById(R.id.ivProfiloLogo);
        contenuto.findViewById(R.id.btnProfiloCambiaPassword).setOnClickListener(b -> apriCambioPassword(tvPassword, tvScadPw));

        // Logo e dati ditta arrivano dal sync di download e sono in locale: il modale li mostra anche offline
        android.graphics.Bitmap logo = pfa.app.econtab.utils.DittaLocale.getLogo(this);
        if (logo != null) {
            ivLogo.setImageBitmap(logo);
            ivLogo.setVisibility(View.VISIBLE);
        }
        final MercuryApiService.DittaDati dittaLocale = pfa.app.econtab.utils.DittaLocale.getDati(this);

        tvUtente.setText("Caricamento...");
        tvDitta.setText(dittaLocale != null ? "Ditta: " + dittaLocale.ragioneSociale + dettagliDitta(dittaLocale) : "");
        tvVersioni.setText("Versioni: verifica in corso...");
        tvLicenza.setText("Verifica in corso...");
        tvPassword.setText("Verifica in corso...");

        new AlertDialog.Builder(this)
                .setTitle("Account")
                .setView(contenuto)
                .setPositiveButton("Chiudi", null)
                .show();

        MercuryApiService api = MercuryApiClient.getInstance(this).getService();

        api.getProfilo().enqueue(new Callback<MercuryApiService.ProfiloResponse>() {
            @Override
            public void onResponse(Call<MercuryApiService.ProfiloResponse> call,
                                   Response<MercuryApiService.ProfiloResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    tvUtente.setText("Impossibile recuperare i dati account.");
                    tvLicenza.setText("");
                    tvPassword.setText("");
                    return;
                }
                MercuryApiService.ProfiloResponse p = response.body();
                String nomeCompleto = ((p.nome == null ? "" : p.nome) + " " + (p.cognome == null ? "" : p.cognome)).trim();
                tvUtente.setText(nomeCompleto.isEmpty() ? p.email : nomeCompleto + "\n" + p.email);
                tvDitta.setText(p.ditta != null ? "Ditta: " + p.ditta.nome + dettagliDitta(dittaLocale) : "Nessuna ditta");
                tvLicenza.setText(formattaLicenza(p.licenza));
                tvPassword.setText(formattaPassword(p.password));
                mostraScadenzaPassword(tvScadPw, p.password);
            }

            @Override
            public void onFailure(Call<MercuryApiService.ProfiloResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                tvUtente.setText("Errore di rete: impossibile recuperare i dati account.");
                tvLicenza.setText("");
                tvPassword.setText("");
            }
        });

        final String[] versioneApp = {null};
        final String[] versioneMercury = {null};
        Runnable aggiornaVersioni = () -> {
            String app = versioneApp[0] != null ? versioneApp[0] : "n.d.";
            String srv = versioneMercury[0] != null ? versioneMercury[0] : "n.d.";
            tvVersioni.setText("Versione app: " + app + "\nVersione server: " + srv);
        };

        api.getVersioneApp(pfa.app.econtab.BuildConfig.GIT_COMMIT).enqueue(new Callback<MercuryApiService.VersionResponse>() {
            @Override
            public void onResponse(Call<MercuryApiService.VersionResponse> call,
                                   Response<MercuryApiService.VersionResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && response.body().versione != null) {
                    versioneApp[0] = response.body().versione;
                    aggiornaVersioni.run();
                }
            }

            @Override
            public void onFailure(Call<MercuryApiService.VersionResponse> call, Throwable t) {
                // resta "n.d."
            }
        });

        api.getVersioneMercury().enqueue(new Callback<MercuryApiService.VersionResponse>() {
            @Override
            public void onResponse(Call<MercuryApiService.VersionResponse> call,
                                   Response<MercuryApiService.VersionResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && response.body().versione != null) {
                    versioneMercury[0] = response.body().versione;
                    aggiornaVersioni.run();
                }
            }

            @Override
            public void onFailure(Call<MercuryApiService.VersionResponse> call, Throwable t) {
                // resta "n.d."
            }
        });
    }

    /** Indirizzo e dati fiscali della ditta (da sync locale), a capo dopo il nome; vuoto se non ancora scaricati. */
    private String dettagliDitta(MercuryApiService.DittaDati d) {
        if (d == null) return "";
        StringBuilder sb = new StringBuilder();
        String localita = ((d.cap == null ? "" : d.cap + " ") + (d.citta == null ? "" : d.citta)
                + (d.provincia == null || d.provincia.isEmpty() ? "" : " (" + d.provincia + ")")).trim();
        if (d.indirizzo != null && !d.indirizzo.isEmpty()) sb.append("\n").append(d.indirizzo);
        if (!localita.isEmpty()) sb.append("\n").append(localita);
        if (d.partitaIva != null && !d.partitaIva.isEmpty()) sb.append("\nP.IVA ").append(d.partitaIva);
        if (d.codiceFiscale != null && !d.codiceFiscale.isEmpty()) sb.append("\nC.F. ").append(d.codiceFiscale);
        return sb.toString();
    }

    private String formattaLicenza(MercuryApiService.LicenzaProfilo licenza) {
        if (licenza == null) {
            return "Nessuna licenza attiva.";
        }
        if (licenza.illimitata) {
            return "Illimitata (nessuna scadenza).";
        }
        if (licenza.scadenza == null) {
            return "Nessuna licenza attiva.";
        }
        int giorni = licenza.giorniRimanenti != null ? licenza.giorniRimanenti : 0;
        if (giorni < 0) {
            return "Scaduta il " + dataIt(licenza.scadenza) + " (" + (-giorni) + " giorni fa).";
        }
        return "Scadenza: " + dataIt(licenza.scadenza) + " (" + giorni + " giorni rimanenti).";
    }

    /**
     * Pacchetti di licenza assegnati all'utente nella ditta (salvati al login e a ogni apertura), es. "Econ Mini": prima
     * quelli validi; se nessuno e' valido, i nomi con il loro stato (es. "Cantieri (Scaduta)").
     */
    private String nomiPacchetti() {
        java.util.List<MercuryApiService.PacchettoInfo> pacchetti = TokenManager.getInstance(this).getPacchetti();
        java.util.List<String> validi = new ArrayList<>();
        java.util.List<String> altri = new ArrayList<>();
        for (MercuryApiService.PacchettoInfo p : pacchetti) {
            if (p == null || p.nome == null) continue;
            if (p.valido) {
                if (!validi.contains(p.nome)) validi.add(p.nome);
            } else {
                altri.add(p.nome + (p.stato != null ? " (" + p.stato + ")" : ""));
            }
        }
        return android.text.TextUtils.join(", ", validi.isEmpty() ? altri : validi);
    }

    /** Data dal server ("2026-12-31") in formato italiano ("31/12/2026"); altrimenti il testo com'e'. */
    private static String dataIt(String ymd) {
        if (ymd == null || !ymd.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            return ymd;
        }
        return ymd.substring(8, 10) + "/" + ymd.substring(5, 7) + "/" + ymd.substring(0, 4);
    }

    /**
     * Riga sotto la scadenza della licenza: "Password: da cambiare entro il 27/12/2026 (91 giorni)", in rosso se
     * scaduta o in scadenza entro 15 giorni.
     */
    private void mostraScadenzaPassword(TextView tv, MercuryApiService.PasswordProfilo pw) {
        if (pw == null) {
            tv.setText("");
            return;
        }
        String testo;
        boolean avviso = false;
        if (pw.giorniRimanenti == null) {
            testo = "Password: non scade.";
        } else if (pw.giorniRimanenti < 0) {
            testo = "Password: scaduta il " + dataIt(pw.scadenza) + ".";
            avviso = true;
        } else if (pw.giorniRimanenti == 0) {
            testo = "Password: scade oggi.";
            avviso = true;
        } else {
            testo = "Password: da cambiare entro il " + dataIt(pw.scadenza) + " (" + pw.giorniRimanenti
                    + (pw.giorniRimanenti == 1 ? " giorno)." : " giorni).");
            avviso = pw.giorniRimanenti <= 15;
        }
        tv.setText(testo);
        tv.setTextColor(avviso ? 0xFFC62828 : 0xFF444444);
    }

    /** Giorni al cambio della password dell'account, secondo la durata impostata dalla ditta. */
    private String formattaPassword(MercuryApiService.PasswordProfilo pw) {
        if (pw == null) {
            return "";
        }
        String cambiata = pw.cambiataIl != null ? "Ultimo cambio: " + dataIt(pw.cambiataIl) + "\n" : "";
        if (pw.giorniRimanenti == null) {
            return cambiata + "Non scade.";
        }
        int giorni = pw.giorniRimanenti;
        if (giorni < 0) {
            return cambiata + "Scaduta il " + dataIt(pw.scadenza) + ": cambiala adesso.";
        }
        if (giorni == 0) {
            return cambiata + "Scade oggi: cambiala adesso.";
        }
        return cambiata + "Da cambiare entro il " + dataIt(pw.scadenza) + " (" + giorni + (giorni == 1 ? " giorno)." : " giorni).");
    }

    /**
     * Cambio password dell'account: la nuova password vale subito anche per il portale web e per le altre ditte.
     * Se l'utente ha "Ricordami", aggiorna anche le credenziali salvate (servono al login automatico).
     */
    private void apriCambioPassword(TextView tvPassword, TextView tvScadPw) {
        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp16, dp16, dp16, 0);

        android.widget.EditText etAttuale = campoPassword("Password attuale");
        android.widget.EditText etNuova = campoPassword("Nuova password (almeno " + pfa.app.econtab.utils.CambioPassword.LUNGHEZZA_MINIMA + " caratteri)");
        android.widget.EditText etConferma = campoPassword("Conferma nuova password");
        layout.addView(etAttuale);
        layout.addView(etNuova);
        layout.addView(etConferma);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Cambia password")
                .setMessage("La nuova password vale per l'app e per il portale web.")
                .setView(layout)
                .setPositiveButton("Conferma", null)
                .setNegativeButton("Annulla", null)
                .create();
        dialog.show();

        // Override sul pulsante positivo per non chiudere il dialog sugli errori
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String attuale = etAttuale.getText().toString();
            String nuova = etNuova.getText().toString();
            String errore = pfa.app.econtab.utils.CambioPassword.errore(attuale, nuova, etConferma.getText().toString());
            if (errore != null) {
                Utility.mostraDialog("Cambia password", errore, this, "OK");
                return;
            }
            btn.setEnabled(false);
            pfa.app.econtab.utils.CambioPassword.invia(this, attuale, nuova, new pfa.app.econtab.utils.CambioPassword.Esito() {
                @Override
                public void fatto(MercuryApiService.PasswordProfilo password) {
                    if (isFinishing() || isDestroyed()) return;
                    tvPassword.setText(formattaPassword(password));
                    mostraScadenzaPassword(tvScadPw, password);
                    dialog.dismiss();
                    Toast.makeText(MenuActivity.this, "Password aggiornata.", Toast.LENGTH_LONG).show();
                }

                @Override
                public void errore(String messaggio) {
                    if (isFinishing() || isDestroyed()) return;
                    btn.setEnabled(true);
                    Utility.mostraDialog("Cambia password", messaggio, MenuActivity.this, "OK");
                }
            });
        });
    }

    private android.widget.EditText campoPassword(String hint) {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint(hint);
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return et;
    }
}
