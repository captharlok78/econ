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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import pfa.app.econtab.api.TokenManager;
import pfa.app.econtab.server.ConfigurazioneGenActivity;
import pfa.app.econtab.utils.AsyncTaskExecutorService;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;

public class MenuActivity extends EConTabActivity {

    // ── Definizione voci di menu ──────────────────────────────────────────────
    // Il "codice" deve combaciare con quello del catalogo Moduli in Sonata
    // (Admin → Amministrazione → Moduli App): è così che il server sa quale
    // voce abilitare o nascondere per ciascun utente.

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
     * Voci di menu effettivamente da mostrare: filtrate sui moduli abilitati per
     * l'utente lato server (Sonata → scheda Utente → "Moduli visibili"). Se il
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("EConTab: MenuActivity onCreate ENTER");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        costruisciGriglia();

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
                    });
                }
                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                    response.close();
                    runOnUiThread(() -> {
                        View l = findViewById(R.id.led_server);
                        if (l != null) l.setBackgroundResource(R.drawable.ic_led_green);
                    });
                }
            });
    }
}
