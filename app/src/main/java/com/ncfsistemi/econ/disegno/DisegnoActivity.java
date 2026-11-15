package com.ncfsistemi.econ.disegno;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ncfsistemi.econ.CollegamentiElementoActivity;
import com.ncfsistemi.econ.PiantinaLocaleModActivity;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.RelazioneActivity;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.Elementi;
import com.ncfsistemi.econ.db.table.Locali;
import com.ncfsistemi.econ.utils.Sessione;
import com.ncfsistemi.econ.utils.SimboliDisegno;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Disegno del locale a tre pannelli (STUDIO_DESIGN_GRAFICA.md §8, proposta B): in alto i modi sempre visibili
 * (Guarda, Posa, Sposta, Collega, Disegno), sulla piantina le etichette Struttura e Componenti, al centro la piantina
 * con lo zoom, a destra il dettaglio dell'elemento scelto. Dal menu ⋮ si torna al disegno classico.
 */
public class DisegnoActivity extends AppCompatActivity implements PiantinaView.Ascoltatore {

    public static final String PREF = "disegno";
    public static final String PREF_CLASSICO = "disegno_classico";

    /** true se l'utente ha scelto di restare sul disegno classico. */
    public static boolean classico(Context ctx) {
        return ctx.getSharedPreferences(PREF, MODE_PRIVATE).getBoolean(PREF_CLASSICO, false);
    }

    public static void setClassico(Context ctx, boolean classico) {
        ctx.getSharedPreferences(PREF, MODE_PRIVATE).edit().putBoolean(PREF_CLASSICO, classico).apply();
    }

    private static final int BLU = 0xFF1565C0;
    private static final int BLU_CHIARO = 0xFFE3F2FD;
    private static final int SFONDO = 0xFFF4F6FA;
    private static final int BORDO = 0xFFDDE3EA;
    private static final int TESTO = 0xFF212121;
    private static final int TESTO2 = 0xFF5F6B7A;
    private static final int ARANCIO = 0xFFFB8C00;
    private static final int ROSSO = 0xFFB3261E;

    private static final String[] NOMI_MODI = {"Guarda", "Posa", "Sposta", "Collega", "Disegno"};
    private static final String[] ICONE_MODI = {"", "", "", "", ""};

    private int cantiere, locale, preventivo;
    private boolean bloccato;
    private int modo = PiantinaView.GUARDA;

    private Typeface fa;
    private float dp;
    private boolean stretto;

    private TextView titolo, sottotitolo, suggerimento;
    private final TextView[] pulsantiModo = new TextView[5];
    private LinearLayout bloccoStruttura, pannelloDestro, contenutoDestro;
    /**
     * Struttura e componenti non occupano più una colonna: sono due etichette in alto a sinistra sulla piantina; la
     * struttura si apre in una finestra, i componenti nella finestra a tutto schermo SceltaComponenti (§16-17).
     */
    private LinearLayout etichette;
    private TextView etichettaStruttura, etichettaTavolozza;
    private android.app.Dialog modale;
    /** Il suggerimento sulla piantina sparisce da solo dopo 5 secondi. */
    private final Runnable nascondiSuggerimento = () -> { if (suggerimento != null) suggerimento.animate().alpha(0f).setDuration(300)
            .withEndAction(() -> suggerimento.setVisibility(View.GONE)).start(); };
    /** Struttura: componente unico con la scheda del cantiere (StrutturaCantiere). */
    private android.widget.ListView listaStruttura;
    private final java.util.ArrayList<Object> righeStruttura = new java.util.ArrayList<>();
    private StrutturaCantiere.Adapter adapterStruttura;
    private PiantinaView piantina;
    /** Schermo intero: pannelli laterali e barre di sistema nascosti, resta solo la piantina con la barra dei modi. */
    private boolean schermoIntero = false;
    private TextView pulsanteSchermoIntero;

    private DisegnoDati.Locale datiLocale;
    private List<DisegnoDati.Posato> posati = new ArrayList<>();
    private DisegnoDati.Posato scelto;
    private DisegnoDati.VoceTavolozza daPosare;
    private int categoriaAperta = -1;
    private final Map<Integer, List<DisegnoDati.VoceTavolozza>> cacheTavolozza = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dp = getResources().getDisplayMetrics().density;
        fa = ResourcesCompat.getFont(this, R.font.fa_solid_900);
        stretto = getResources().getConfiguration().screenWidthDp < 840;

        cantiere = getIntent().getIntExtra(Cantieri.ID_CANTIERE, 0);
        locale = getIntent().getIntExtra(Locali.ID_LOCALE, 0);
        if (savedInstanceState != null) {
            locale = savedInstanceState.getInt("locale", locale);
            modo = savedInstanceState.getInt("modo", modo);
            schermoIntero = savedInstanceState.getBoolean("schermoIntero", false);
        }
        preventivo = Sessione.getIdPreventivoSelezionato();
        bloccato = getIntent().getBooleanExtra("MODIFICHE_BLOCCATE", false) || DisegnoDati.documentoBloccato(this, preventivo);
        if (bloccato) modo = PiantinaView.GUARDA;

        setContentView(costruisci());
        getWindow().setStatusBarColor(BLU);
        if (schermoIntero) applicaSchermoIntero();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("locale", locale);
        out.putInt("modo", modo);
        out.putBoolean("schermoIntero", schermoIntero);
    }

    @Override
    public void onBackPressed() {
        // indietro dallo schermo intero torna ai pannelli, non chiude il disegno
        if (schermoIntero) {
            impostaSchermoIntero(false);
            return;
        }
        super.onBackPressed();
    }

    // ------------------------------------------------------------------ schermo intero

    private void impostaSchermoIntero(boolean si) {
        if (schermoIntero == si) return;
        schermoIntero = si;
        applicaSchermoIntero();
    }

    private void applicaSchermoIntero() {
        androidx.core.view.WindowInsetsControllerCompat wc =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (schermoIntero) {
            chiudiModale();
            pannelloDestro.setVisibility(View.GONE);
            wc.setSystemBarsBehavior(androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            wc.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());
        } else {
            wc.show(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            costruisciDestra();
        }
        if (pulsanteSchermoIntero != null) {
            pulsanteSchermoIntero.setText(schermoIntero ? "\uf066" : "\uf065");
            String d = schermoIntero ? "Esci dallo schermo intero" : "Schermo intero";
            pulsanteSchermoIntero.setContentDescription(d);
            androidx.appcompat.widget.TooltipCompat.setTooltipText(pulsanteSchermoIntero, d);
        }
    }

    /** Mostra o nasconde un pannello laterale; a schermo intero restano nascosti. */
    private void vedi(View pannello, boolean si) {
        pannello.setVisibility(si && !schermoIntero ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // si torna dalla composizione, dai collegamenti o dalla forma del locale: si rileggono i dati
        ricarica();
    }

    // ------------------------------------------------------------------ costruzione

    private View costruisci() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(SFONDO);
        root.addView(barra(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout corpo = new LinearLayout(this);
        corpo.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(corpo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // struttura: riquadro che si apre in una finestra dall'etichetta sulla piantina; ha la sua
        // intestazione fissa e scorre dentro il proprio riquadro
        // struttura: stesso componente della scheda del cantiere (ordine Per livello / Locali A-Z, livello a sinistra delle aree)
        LinearLayout struttura = verticale();
        struttura.addView(StrutturaCantiere.barraOrdine(this, this::caricaStruttura));
        View sepStruttura = separatore();
        struttura.addView(sepStruttura, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(1)));
        listaStruttura = new android.widget.ListView(this);
        StrutturaCantiere.stileLista(listaStruttura);
        adapterStruttura = new StrutturaCantiere.Adapter(this, righeStruttura, new StrutturaCantiere.Selezione() {
            @Override
            public boolean selezionata(int posizione, android.content.ContentValues riga) {
                return riga.getAsInteger(StrutturaCantiere.TIPO) == com.ncfsistemi.econ.CantiereSplitActivity.LOCALE
                        && riga.getAsInteger(StrutturaCantiere.ID) == locale;
            }

            @Override
            public boolean attiva(int posizione, android.content.ContentValues riga) {
                // nel disegno si sceglie il locale
                return riga.getAsInteger(StrutturaCantiere.TIPO) == com.ncfsistemi.econ.CantiereSplitActivity.LOCALE;
            }
        });
        listaStruttura.setAdapter(adapterStruttura);
        listaStruttura.setOnItemClickListener((parent, view, pos, id) -> {
            android.content.ContentValues r = (android.content.ContentValues) righeStruttura.get(pos);
            int idLocale = r.getAsInteger(StrutturaCantiere.ID);
            if (idLocale == locale) return;
            locale = idLocale;
            scelto = null;
            piantina.adatta();
            ricarica();
            chiudiModale();
        });
        struttura.addView(listaStruttura, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        bloccoStruttura = blocco("STRUTTURA", struttura);
        // matita in modalità modifica (unità, area, locale): si rilegge struttura, titolo e sottotitolo
        adapterStruttura.setDopoModifica(this::ricarica);

        // centro: piantina
        FrameLayout centro = new FrameLayout(this);
        piantina = new PiantinaView(this);
        piantina.setAscoltatore(this);
        piantina.setModo(modo);
        centro.addView(piantina, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // etichette in alto a sinistra: Struttura (finestra) e Componenti (finestra a tutto schermo)
        etichette = new LinearLayout(this);
        etichette.setOrientation(LinearLayout.HORIZONTAL);
        etichettaStruttura = etichetta("\uf0e8", "Struttura", v -> apriModale(bloccoStruttura));
        etichettaTavolozza = etichetta("\uf1b3", "Componenti", v -> apriComponenti());
        etichette.addView(etichettaStruttura);
        LinearLayout.LayoutParams lpE = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpE.leftMargin = px(8);
        etichette.addView(etichettaTavolozza, lpE);
        FrameLayout.LayoutParams lpEt = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        lpEt.setMargins(px(12), px(12), px(12), px(12));
        centro.addView(etichette, lpEt);

        suggerimento = new TextView(this);
        suggerimento.setTextColor(TESTO);
        suggerimento.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        suggerimento.setPadding(px(12), px(6), px(12), px(6));
        suggerimento.setBackground(arrotondato(Color.WHITE, BORDO, 16));
        suggerimento.setElevation(px(2));
        suggerimento.setOnClickListener(v -> { suggerimento.removeCallbacks(nascondiSuggerimento); nascondiSuggerimento.run(); });
        FrameLayout.LayoutParams lpS = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START);
        lpS.setMargins(px(12), px(12), px(76), px(14));
        centro.addView(suggerimento, lpS);

        LinearLayout zoom = verticale();
        zoom.setBackground(arrotondato(Color.WHITE, BORDO, 12));
        zoom.setElevation(px(3));
        zoom.addView(iconaPulsante("", "Ingrandisci", v -> piantina.zoom(1.25f)));
        zoom.addView(iconaPulsante("", "Riduci", v -> piantina.zoom(0.8f)));
        pulsanteSchermoIntero = iconaPulsante("\uf065", "Schermo intero", v -> impostaSchermoIntero(!schermoIntero));
        zoom.addView(pulsanteSchermoIntero);
        zoom.addView(iconaPulsante("\uf05b", "Adatta alla stanza", v -> piantina.adatta()));
        FrameLayout.LayoutParams lpZ = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        lpZ.setMargins(px(12), px(12), px(12), px(12));
        centro.addView(zoom, lpZ);
        corpo.addView(centro, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        // destra: dettaglio
        pannelloDestro = new LinearLayout(this);
        pannelloDestro.setOrientation(LinearLayout.HORIZONTAL);
        pannelloDestro.addView(separatore(), new LinearLayout.LayoutParams(px(1), ViewGroup.LayoutParams.MATCH_PARENT));
        ScrollView sd = new ScrollView(this);
        sd.setBackgroundColor(Color.WHITE);
        contenutoDestro = verticale();
        contenutoDestro.setPadding(px(16), px(12), px(16), px(16));
        sd.addView(contenutoDestro);
        pannelloDestro.addView(sd, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        corpo.addView(pannelloDestro, new LinearLayout.LayoutParams(px(stretto ? 280 : 320), ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    private View barra() {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setBackgroundColor(BLU);
        b.setPadding(px(4), px(6), px(8), px(6));
        b.setElevation(px(4));

        b.addView(iconaBianca("", "Indietro", v -> finish()));

        LinearLayout testi = verticale();
        titolo = new TextView(this);
        titolo.setTextColor(Color.WHITE);
        titolo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titolo.setTypeface(Typeface.DEFAULT_BOLD);
        titolo.setSingleLine(true);
        titolo.setEllipsize(TextUtils.TruncateAt.END);
        sottotitolo = new TextView(this);
        sottotitolo.setTextColor(0xCCFFFFFF);
        sottotitolo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sottotitolo.setSingleLine(true);
        sottotitolo.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        testi.addView(titolo);
        testi.addView(sottotitolo);
        LinearLayout.LayoutParams lpT = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpT.leftMargin = px(4);
        lpT.rightMargin = px(12);
        b.addView(testi, lpT);

        // modi
        LinearLayout modi = new LinearLayout(this);
        modi.setOrientation(LinearLayout.HORIZONTAL);
        modi.setBackground(arrotondato(0x33FFFFFF, 0, 22));
        modi.setPadding(px(3), px(3), px(3), px(3));
        for (int i = 0; i < 5; i++) {
            final int m = i;
            TextView t = new TextView(this);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            t.setGravity(Gravity.CENTER);
            t.setPadding(px(stretto ? 10 : 14), px(7), px(stretto ? 10 : 14), px(7));
            t.setSingleLine(true);
            t.setOnClickListener(v -> cambiaModo(m));
            pulsantiModo[i] = t;
            modi.addView(t);
        }
        b.addView(modi);
        aggiornaModi();

        // Excel e stampa della conformità solo nel menu dei tre punti
        b.addView(iconaBianca("", "Altro", this::menu));
        return b;
    }

    private void menu(View ancora) {
        PopupMenu pm = new PopupMenu(this, ancora);
        if (!bloccato) {
            pm.getMenu().add(0, 1, 0, "Forma del locale (8×8)");
            pm.getMenu().add(0, 2, 0, "Forma del locale (6×6)");
        }
        pm.getMenu().add(0, 3, 0, "Adatta la piantina");
        pm.getMenu().add(0, 5, 0, "Dichiarazione di conformità (Excel)");
        pm.getMenu().add(0, 6, 0, "Dichiarazione di conformità da stampare (PDF)");
        pm.getMenu().add(0, 4, 0, "Usa il disegno classico");
        pm.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                case 2: {
                    Intent i = new Intent(this, PiantinaLocaleModActivity.class);
                    i.putExtra("ID", locale);
                    if (item.getItemId() == 2) {
                        i.putExtra("NUMRIGHE", 6);
                        i.putExtra("NUMCOLONNE", 6);
                    }
                    startActivity(i);
                    return true;
                }
                case 3:
                    piantina.adatta();
                    return true;
                case 5:
                    esportaConformita();
                    return true;
                case 6:
                    stampaConformita();
                    return true;
                case 4:
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Disegno classico")
                            .setMessage("Da ora i locali si aprono con il disegno classico. Per tornare a questa schermata usa"
                                    + " \"Disegno nuovo\" nel menu del locale.")
                            .setPositiveButton("Usa il classico", (d, w) -> {
                                setClassico(this, true);
                                finish();
                            })
                            .setNegativeButton("Annulla", null)
                            .show();
                    return true;
            }
            return false;
        });
        pm.show();
    }

    /**
     * Dichiarazione di conformità da stampare (STUDIO_DESIGN_GRAFICA.md §12): stesso modello del portale, un blocco A4 per
     * locale; si stampa o si salva in PDF dalla stampa di Android. Le piantine si disegnano sul thread della UI.
     */
    private void stampaConformita() {
        Toast.makeText(this, "Preparo la stampa della conformità…", Toast.LENGTH_SHORT).show();
        piantina.post(() -> com.ncfsistemi.econ.stampa.StampaConformita.stampa(this, cantiere));
    }

    /**
     * Excel per la dichiarazione di conformità di tutto il cantiere (un foglio per locale con piantina, elementi,
     * componenti e collegamenti; stesso file del portale). Le piantine si disegnano qui, il file in background.
     */
    private void esportaConformita() {
        androidx.appcompat.app.AlertDialog attesa = new MaterialAlertDialogBuilder(this)
                .setMessage("Creo l'Excel per la dichiarazione di conformità…").setCancelable(false).create();
        attesa.show();
        // un giro sul thread della UI per far comparire la finestra prima di disegnare le piantine
        piantina.post(() -> {
            final Map<Integer, byte[]> piantine;
            try {
                piantine = com.ncfsistemi.econ.export.ConformitaXLS.piantine(this, cantiere);
            } catch (Exception e) {
                attesa.dismiss();
                Toast.makeText(this, "Errore nel disegno delle piantine: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            new Thread(() -> {
                String percorso = null;
                String errore = null;
                try {
                    percorso = new com.ncfsistemi.econ.export.ConformitaXLS(getApplicationContext()).genera(cantiere, piantine);
                } catch (Exception e) {
                    errore = e.getMessage();
                }
                final String file = percorso, err = errore;
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    attesa.dismiss();
                    if (file == null) {
                        Toast.makeText(this, "Excel non creato: " + err, Toast.LENGTH_LONG).show();
                        return;
                    }
                    apriFile(new java.io.File(file));
                });
            }).start();
        });
    }

    private void apriFile(java.io.File file) {
        android.net.Uri uri = com.ncfsistemi.econ.utils.Utility.uriCondivisibile(this, file);
        Intent apri = new Intent(Intent.ACTION_VIEW);
        apri.setDataAndType(uri, "application/vnd.ms-excel");
        apri.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(apri);
        } catch (android.content.ActivityNotFoundException e) {
            // nessuna app per i fogli di calcolo: si propone di inviarlo
            Intent invia = new Intent(Intent.ACTION_SEND);
            invia.setType("application/vnd.ms-excel");
            invia.putExtra(Intent.EXTRA_STREAM, uri);
            invia.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(Intent.createChooser(invia, "Invia l'Excel"));
            } catch (Exception ex) {
                Toast.makeText(this, "Excel salvato in " + file.getPath(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void cambiaModo(int m) {
        if (bloccato && m != PiantinaView.GUARDA && m != PiantinaView.COLLEGA) {
            Toast.makeText(this, "Il documento scelto non è aperto: il disegno si può solo guardare", Toast.LENGTH_SHORT).show();
            return;
        }
        modo = m;
        piantina.setModo(m);
        if (m == PiantinaView.POSA) {
            // per posare serve un elemento: senza, si apre Componenti
            if (daPosare == null && !bloccato) apriComponenti();
        }
        aggiornaModi();
        aggiornaSuggerimento();
    }

    private void aggiornaModi() {
        for (int i = 0; i < 5; i++) {
            TextView t = pulsantiModo[i];
            if (t == null) continue;
            boolean sel = i == modo;
            boolean off = bloccato && i != PiantinaView.GUARDA && i != PiantinaView.COLLEGA;
            android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
            sb.append(ICONE_MODI[i]);
            sb.setSpan(new CustomTypefaceSpan(fa), 0, sb.length(), 0);
            sb.append("  ").append(NOMI_MODI[i]);
            t.setText(sb);
            t.setTextColor(sel ? BLU : off ? 0x66FFFFFF : Color.WHITE);
            t.setTypeface(sel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            t.setBackground(sel ? arrotondato(Color.WHITE, 0, 19) : null);
        }
    }

    private void aggiornaSuggerimento() {
        String s;
        switch (modo) {
            case PiantinaView.POSA:
                s = daPosare == null ? "Scegli un elemento in Componenti, poi tocca la piantina dove va"
                        : "Tocca la piantina per posare: " + daPosare.nome;
                break;
            case PiantinaView.SPOSTA:
                s = "Trascina gli elementi per spostarli";
                break;
            case PiantinaView.COLLEGA:
                s = "Tocca un elemento per vedere o aggiungere i collegamenti";
                break;
            case PiantinaView.DISEGNO:
                s = "Tocca la piantina: spazio della stanza, porte, finestre e arredi · trascina gli arredi per spostarli";
                break;
            default:
                s = bloccato ? "Sola lettura: il documento scelto non è aperto" : "Tocca un elemento per vederne il dettaglio";
        }
        suggerimento.setText(s);
        // si vede per 5 secondi, poi sparisce (si tocca per chiuderlo prima)
        suggerimento.removeCallbacks(nascondiSuggerimento);
        suggerimento.animate().cancel();
        suggerimento.setAlpha(1f);
        suggerimento.setVisibility(View.VISIBLE);
        suggerimento.postDelayed(nascondiSuggerimento, 5000);
        aggiornaEtichette();
    }

    /** Testi delle etichette: Struttura, Componenti (con l'elemento da posare); con il documento bloccato niente Componenti. */
    private void aggiornaEtichette() {
        if (etichettaStruttura == null) return;
        testoEtichetta(etichettaStruttura, "\uf0e8", "Struttura");
        testoEtichetta(etichettaTavolozza, "\uf1b3", daPosare == null ? "Componenti" : "Componenti · " + daPosare.nome);
        etichettaTavolozza.setVisibility(bloccato ? View.GONE : View.VISIBLE);
    }

    private TextView etichetta(String glifo, String testo, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTextColor(BLU);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setMaxWidth(px(420));
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(px(14), px(9), px(16), px(9));
        t.setBackground(arrotondato(Color.WHITE, BLU, 20));
        t.setElevation(px(3));
        t.setOnClickListener(l);
        testoEtichetta(t, glifo, testo);
        return t;
    }

    private void testoEtichetta(TextView t, String glifo, String testo) {
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder(glifo);
        sb.setSpan(new CustomTypefaceSpan(fa), 0, sb.length(), 0);
        sb.append("  ").append(testo);
        t.setText(sb);
        t.setContentDescription(testo);
    }

    /** Apre la struttura in una finestra a sinistra, sopra la piantina; si chiude toccando fuori. */
    private void apriModale(View blocco) {
        if (blocco == null || isFinishing()) return;
        chiudiModale();
        if (blocco.getParent() != null) ((ViewGroup) blocco.getParent()).removeView(blocco);
        FrameLayout cornice = new FrameLayout(this);
        cornice.addView(blocco, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        modale = new android.app.Dialog(this);
        modale.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        modale.setContentView(cornice);
        modale.setCanceledOnTouchOutside(true);
        modale.setOnDismissListener(d -> {
            if (blocco.getParent() != null) ((ViewGroup) blocco.getParent()).removeView(blocco);
        });
        android.view.Window w = modale.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            android.view.WindowManager.LayoutParams lp = w.getAttributes();
            lp.width = Math.min(px(380), dm.widthPixels - px(24));
            lp.height = Math.round(dm.heightPixels * 0.80f);
            lp.gravity = Gravity.TOP | Gravity.START;
            lp.x = px(12);
            lp.y = px(110);
            lp.dimAmount = 0.2f;
            w.setAttributes(lp);
            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        modale.show();
    }

    private void chiudiModale() {
        if (modale != null && modale.isShowing()) modale.dismiss();
        modale = null;
    }

    @Override
    protected void onDestroy() {
        chiudiModale();
        if (suggerimento != null) suggerimento.removeCallbacks(nascondiSuggerimento);
        super.onDestroy();
    }

    // ------------------------------------------------------------------ dati

    private void ricarica() {
        datiLocale = DisegnoDati.locale(this, locale);
        posati = DisegnoDati.elementi(this, locale, preventivo);
        piantina.setDati(datiLocale, posati, DisegnoDati.linee(this, locale));
        titolo.setText(datiLocale.nome.isEmpty() ? "Locale" : datiLocale.nome);
        String perc = DisegnoDati.percorso(this, locale);
        sottotitolo.setText((perc.isEmpty() ? "" : perc + "  ·  ") + DisegnoDati.descrizioneDocumento(this, preventivo));
        // l'elemento scelto si rilegge (etichetta, collegamenti cambiati)
        DisegnoDati.Posato prima = scelto;
        scelto = null;
        if (prima != null) {
            for (DisegnoDati.Posato p : posati) if (p.id == prima.id) scelto = p;
        }
        piantina.setSelezionato(scelto == null ? 0 : scelto.id);
        costruisciSinistra();
        costruisciDestra();
        aggiornaSuggerimento();
    }

    private void costruisciSinistra() {
        caricaStruttura();
    }

    /**
     * Finestra unica a tutto schermo per scegliere il componente da posare (ricerca, filtro per categoria, griglia):
     * SceltaComponenti, STUDIO_DESIGN_GRAFICA.md §17. Ricorda l'ultimo filtro usato.
     */
    private void apriComponenti() {
        if (bloccato) return;
        new SceltaComponenti(this, this::vociTavolozza).apri(categoriaAperta, daPosare == null ? 0 : daPosare.id, (v, filtro) -> {
            daPosare = v;
            categoriaAperta = filtro;
            if (modo != PiantinaView.POSA) cambiaModo(PiantinaView.POSA);
            aggiornaSuggerimento();
        });
    }

    /** Rilegge la struttura nell'ordine scelto; la lista resta dov'era (stesso adapter). */
    private void caricaStruttura() {
        righeStruttura.clear();
        righeStruttura.addAll(StrutturaCantiere.righe(this, cantiere));
        adapterStruttura.notifyDataSetChanged();
    }

    /** Riquadro bianco con intestazione fissa e contenuto che scorre dentro (pannello di sinistra). */
    private LinearLayout blocco(String titolo, View scorri) {
        LinearLayout b = verticale();
        b.setBackground(arrotondato(Color.WHITE, BORDO, 12));
        b.setClipToOutline(true);
        TextView t = intestazione(titolo, null);
        t.setPadding(px(14), px(10), px(14), px(6));
        b.addView(t);
        View riga = separatore();
        b.addView(riga, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(1)));
        if (scorri instanceof ScrollView) ((ScrollView) scorri).setFillViewport(false);
        b.addView(scorri, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return b;
    }

    private List<DisegnoDati.VoceTavolozza> vociTavolozza(int idCategoria) {
        List<DisegnoDati.VoceTavolozza> voci = cacheTavolozza.get(idCategoria);
        if (voci == null) {
            voci = DisegnoDati.tavolozza(this, idCategoria);
            cacheTavolozza.put(idCategoria, voci);
        }
        return voci;
    }

    // ------------------------------------------------------------------ pannello destro

    private void costruisciDestra() {
        contenutoDestro.removeAllViews();
        if (scelto == null) {
            if (stretto && modo != PiantinaView.GUARDA) {
                vedi(pannelloDestro, false);
                return;
            }
            vedi(pannelloDestro, true);
            riepilogoLocale();
            return;
        }
        vedi(pannelloDestro, true);
        DisegnoDati.Posato p = scelto;
        DisegnoDati.Dettaglio d = DisegnoDati.dettaglio(this, p);

        // intestazione: foto (se c'è) o simbolo, nome, categoria e numero
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        SimboliDisegno.Voce voce = p.idElemento > 0 ? SimboliDisegno.elemento(p.idElemento) : SimboliDisegno.categoriaElementi(p.idCategoria);
        iv.setImageDrawable(SimboliDisegno.fotoOppureSimbolo(this, Elementi.PATH_ICONE, p.icona, voce));
        h.addView(iv, new LinearLayout.LayoutParams(px(52), px(52)));
        LinearLayout ht = verticale();
        ht.setPadding(px(12), 0, 0, 0);
        TextView n = new TextView(this);
        n.setText(p.nome);
        n.setTextColor(TESTO);
        n.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        n.setTypeface(Typeface.DEFAULT_BOLD);
        ht.addView(n);
        TextView sub = new TextView(this);
        sub.setText((d.categoria.isEmpty() ? "" : d.categoria + " · ") + "elemento " + p.numero);
        sub.setTextColor(TESTO2);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        ht.addView(sub);
        h.addView(ht, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        h.addView(iconaPulsante("", "Chiudi", v -> seleziona(null)));
        contenutoDestro.addView(h);

        TextView stato = new TextView(this);
        boolean nelDocumento = p.idPreventivo == preventivo;
        stato.setText(p.idPreventivo == 0 ? "Nel cantiere"
                : (nelDocumento ? "Nel documento scelto: " : "") + DisegnoDati.descrizioneDocumento(this, p.idPreventivo));
        stato.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        stato.setTextColor(p.altroOrdine ? TESTO2 : BLU);
        stato.setPadding(px(10), px(3), px(10), px(3));
        stato.setBackground(arrotondato(p.altroOrdine ? 0xFFECEFF1 : BLU_CHIARO, 0, 12));
        LinearLayout.LayoutParams lpSt = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpSt.topMargin = px(10);
        contenutoDestro.addView(stato, lpSt);

        boolean modificabile = !bloccato && !DisegnoDati.serveCopia(this, p, preventivo);
        EditText etichetta = campo("Etichetta", p.nome, false, modificabile);
        EditText altezza = campo("Altezza da terra (cm)", formatta(p.altezza), true, modificabile);
        if (modificabile) {
            TextView salva = pulsante("Salva", false, v -> {
                Double h2 = null;
                try {
                    String s = altezza.getText().toString().trim().replace(',', '.');
                    if (!s.isEmpty()) h2 = Double.parseDouble(s);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Altezza non valida", Toast.LENGTH_SHORT).show();
                    return;
                }
                String nome = etichetta.getText().toString().trim();
                if (nome.isEmpty()) nome = p.nome;
                DisegnoDati.salvaEtichettaAltezza(this, p.id, nome, h2);
                Toast.makeText(this, "Salvato", Toast.LENGTH_SHORT).show();
                ricarica();
            });
            LinearLayout.LayoutParams lpSalva = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpSalva.gravity = Gravity.END;
            lpSalva.topMargin = px(6);
            contenutoDestro.addView(salva, lpSalva);
        }

        sezione("COMPOSIZIONE", d.composizione, d.configurabile ? "Nessun componente: tocca Componi" : "—");
        sezione("COLLEGAMENTI", d.collegamenti, "Nessun collegamento");
        sezione("RELAZIONI", d.relazioni, "Nessuna relazione");

        // azioni
        LinearLayout az = new LinearLayout(this);
        az.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpAz = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpAz.topMargin = px(16);
        az.addView(pulsante(d.configurabile ? "Componi" : "Dettaglio", true, v -> apri(p)), pesato());
        az.addView(pulsante("Collega", false, v -> collega(p)), pesato());
        contenutoDestro.addView(az, lpAz);
        LinearLayout az2 = new LinearLayout(this);
        az2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpAz2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpAz2.topMargin = px(8);
        az2.addView(pulsante("Relazione", false, v -> relazione(p)), pesato());
        if (modificabile) {
            TextView el = pulsante("Elimina", false, v -> elimina(p));
            el.setTextColor(ROSSO);
            el.setBackground(arrotondato(Color.WHITE, ROSSO, 20));
            az2.addView(el, pesato());
        }
        contenutoDestro.addView(az2, lpAz2);
    }

    private void riepilogoLocale() {
        TextView t = new TextView(this);
        t.setText(datiLocale == null || datiLocale.nome.isEmpty() ? "Locale" : datiLocale.nome);
        t.setTextColor(TESTO);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        contenutoDestro.addView(t);
        TextView s = new TextView(this);
        s.setText(posati.size() == 1 ? "1 elemento" : posati.size() + " elementi");
        s.setTextColor(TESTO2);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        contenutoDestro.addView(s);
        contenutoDestro.addView(intestazione("ELEMENTI", null));
        if (posati.isEmpty()) {
            TextView v = new TextView(this);
            v.setText(bloccato ? "Nessun elemento" : "Nessun elemento: scegli Posa e un elemento in Componenti");
            v.setTextColor(TESTO2);
            contenutoDestro.addView(v);
        }
        for (DisegnoDati.Posato p : posati) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(px(4), px(6), px(4), px(6));
            r.setBackground(selezionabile());
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(SimboliDisegno.drawable(this, p.idElemento > 0 ? SimboliDisegno.elemento(p.idElemento)
                    : SimboliDisegno.categoriaElementi(p.idCategoria)));
            r.addView(iv, new LinearLayout.LayoutParams(px(26), px(26)));
            TextView num = new TextView(this);
            num.setText(p.numero);
            num.setTextColor(TESTO2);
            num.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            num.setTypeface(Typeface.DEFAULT_BOLD);
            num.setPadding(px(10), 0, px(8), 0);
            r.addView(num);
            TextView nome = new TextView(this);
            nome.setText(p.nome);
            nome.setTextColor(TESTO);
            nome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            r.addView(nome, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            r.setOnClickListener(v -> seleziona(p));
            contenutoDestro.addView(r);
        }
    }

    private void sezione(String titoloSez, List<String> righe, String vuoto) {
        contenutoDestro.addView(intestazione(titoloSez + (righe.isEmpty() ? "" : "  (" + righe.size() + ")"), null));
        if (righe.isEmpty()) {
            TextView v = new TextView(this);
            v.setText(vuoto);
            v.setTextColor(TESTO2);
            v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            contenutoDestro.addView(v);
            return;
        }
        for (String s : righe) {
            TextView v = new TextView(this);
            v.setText(s);
            v.setTextColor(TESTO);
            v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            v.setPadding(px(10), px(6), px(8), px(6));
            v.setBackground(arrotondato(SFONDO, 0, 8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = px(4);
            contenutoDestro.addView(v, lp);
        }
    }

    // ------------------------------------------------------------------ azioni

    private void seleziona(DisegnoDati.Posato p) {
        scelto = p;
        piantina.setSelezionato(p == null ? 0 : p.id);
        costruisciDestra();
    }

    private void apri(DisegnoDati.Posato p) {
        if (DisegnoDati.serveCopia(this, p, preventivo)) {
            String[] opzioni = {getString(R.string.copia_elemento_su_preventivo_corrente_senza_conteggiare),
                    getString(R.string.apri_elemento_solo_in_visualizzazione)};
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.modifiche_non_permesse_preventivo)
                    .setItems(opzioni, (d, i) -> {
                        if (i == 0) {
                            DisegnoDati.copiaNelPreventivo(this, p, preventivo);
                            ricarica();
                        } else {
                            DisegnoDati.apri(this, p, preventivo, true);
                        }
                    })
                    .show();
        } else {
            DisegnoDati.apri(this, p, preventivo, bloccato);
        }
    }

    private void collega(DisegnoDati.Posato p) {
        Intent i = new Intent(this, CollegamentiElementoActivity.class);
        i.putExtra("ID_ELEMENTO", p.id);
        startActivity(i);
    }

    private void relazione(DisegnoDati.Posato p) {
        Intent i = new Intent(this, RelazioneActivity.class);
        i.putExtra("ID_ELEMENTO", p.id);
        startActivity(i);
    }

    private void elimina(DisegnoDati.Posato p) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Elimina elemento")
                .setMessage("Eliminare " + p.numero + ". " + p.nome + " con la sua composizione e i suoi collegamenti?")
                .setPositiveButton("Elimina", (d, w) -> {
                    DisegnoDati.elimina(this, p.id);
                    scelto = null;
                    ricarica();
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    // ------------------------------------------------------------------ piantina

    @Override
    public void toccoElemento(DisegnoDati.Posato p) {
        if (p != null && modo == PiantinaView.COLLEGA) {
            seleziona(p);
            collega(p);
            return;
        }
        seleziona(p);
    }

    @Override
    public void toccoVuoto(int x, int y) {
        if (modo == PiantinaView.POSA && !bloccato) {
            if (daPosare == null) {
                Toast.makeText(this, "Scegli prima un elemento in Componenti", Toast.LENGTH_SHORT).show();
                apriComponenti();
                return;
            }
            int id = DisegnoDati.posa(this, cantiere, locale, preventivo, daPosare.id, x, y);
            scelto = null;
            ricarica();
            for (DisegnoDati.Posato p : posati) if (p.id == id) seleziona(p);
            return;
        }
        seleziona(null);
    }

    @Override
    public void spostato(DisegnoDati.Posato p, int x, int y) {
        if (bloccato || DisegnoDati.serveCopia(this, p, preventivo)) {
            Toast.makeText(this, "Questo elemento non si può spostare nel documento scelto", Toast.LENGTH_SHORT).show();
            ricarica();
            return;
        }
        DisegnoDati.sposta(this, p.id, x, y);
        seleziona(p);
    }

    /**
     * Modo Disegno (STUDIO_DESIGN_GRAFICA.md §18): ogni tocco apre la finestra SceltaDisegno con lo spazio della stanza
     * (aggiungi o togli il quadretto) e gli elementi di disegno del server; fuori dalla stanza il quadretto si aggiunge
     * subito, perché lì non c'è altro da fare.
     */
    @Override
    public void toccoDisegno(PiantinaView.PuntoDisegno p) {
        if (bloccato || datiLocale == null) return;
        if (p.esistente == null && p.cellaParete < 0 && p.cella >= 0 && !p.cellaPiena) {
            cambiaQuadretto(p.cella, true);
            return;
        }
        new SceltaDisegno(this).apri(p, datiLocale.n, new SceltaDisegno.Azioni() {
            @Override
            public void quadretto(int cella, boolean aggiungi) {
                cambiaQuadretto(cella, aggiungi);
            }

            @Override
            public void posa(DisegnoDati.ElementoDisegno e, PiantinaView.PuntoDisegno punto) {
                if (e.parete()) {
                    DisegnoDati.aggiungiApertura(DisegnoActivity.this, locale, e, punto.cellaParete, punto.lato,
                            punto.perc(datiLocale.n, e.larghezza), 0, 0);
                } else {
                    DisegnoDati.aggiungiApertura(DisegnoActivity.this, locale, e, 0, null, 0, punto.posX, punto.posY);
                }
                ricarica();
            }

            @Override
            public void cambia(DisegnoDati.Apertura a, DisegnoDati.ElementoDisegno e) {
                a.elemento = e;
                DisegnoDati.spostaApertura(DisegnoActivity.this, a);
                ricarica();
            }

            @Override
            public void ruota(DisegnoDati.Apertura a) {
                a.rotazione = a.parete() ? (a.rotazione == 180 ? 0 : 180) : (a.rotazione + 90) % 360;
                DisegnoDati.spostaApertura(DisegnoActivity.this, a);
                ricarica();
            }

            @Override
            public void elimina(DisegnoDati.Apertura a) {
                DisegnoDati.eliminaApertura(DisegnoActivity.this, a.id);
                ricarica();
            }
        });
    }

    @Override
    public void spostatoDisegno(DisegnoDati.Apertura a) {
        if (bloccato) {
            ricarica();
            return;
        }
        DisegnoDati.spostaApertura(this, a);
        ricarica();
    }

    private void cambiaQuadretto(int cella, boolean aggiungi) {
        if (datiLocale == null || cella < 0 || cella >= datiLocale.celle.length) return;
        boolean[] celle = datiLocale.celle.clone();
        celle[cella] = aggiungi;
        int pieni = 0;
        for (boolean c : celle) if (c) pieni++;
        if (pieni == 0) {
            Toast.makeText(this, "La stanza deve avere almeno un quadretto", Toast.LENGTH_SHORT).show();
            return;
        }
        DisegnoDati.salvaPianta(this, locale, celle);
        ricarica();
    }

    // ------------------------------------------------------------------ aiuti grafici

    private int px(float v) {
        return Math.round(v * dp);
    }

    private LinearLayout verticale() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private View separatore() {
        View v = new View(this);
        v.setBackgroundColor(BORDO);
        return v;
    }

    private GradientDrawable arrotondato(int sfondo, int bordo, int raggioDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(sfondo);
        g.setCornerRadius(px(raggioDp));
        if (bordo != 0) g.setStroke(px(1), bordo);
        return g;
    }

    private Drawable selezionabile() {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        return ResourcesCompat.getDrawable(getResources(), tv.resourceId, getTheme());
    }

    private TextView intestazione(String testo, View destra) {
        TextView t = new TextView(this);
        t.setText(testo);
        t.setTextColor(TESTO2);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.08f);
        t.setPadding(px(4), px(16), px(4), px(6));
        return t;
    }

    private TextView iconaBianca(String glifo, String descr, View.OnClickListener l) {
        TextView t = iconaPulsante(glifo, descr, l);
        t.setTextColor(Color.WHITE);
        return t;
    }

    private TextView iconaPulsante(String glifo, String descr, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setTypeface(fa);
        t.setText(glifo);
        t.setTextColor(TESTO2);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        t.setGravity(Gravity.CENTER);
        t.setContentDescription(descr);
        androidx.appcompat.widget.TooltipCompat.setTooltipText(t, descr);
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true);
        t.setBackgroundResource(tv.resourceId);
        t.setLayoutParams(new LinearLayout.LayoutParams(px(46), px(46)));
        t.setOnClickListener(l);
        return t;
    }

    private TextView pulsante(String testo, boolean primario, View.OnClickListener l) {
        TextView b = new TextView(this);
        b.setText(testo);
        b.setGravity(Gravity.CENTER);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setMinHeight(px(42));
        b.setPadding(px(14), px(8), px(14), px(8));
        b.setTextColor(primario ? Color.WHITE : BLU);
        b.setBackground(primario ? arrotondato(BLU, 0, 20) : arrotondato(Color.WHITE, BLU, 20));
        b.setOnClickListener(l);
        return b;
    }

    private LinearLayout.LayoutParams pesato() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = px(4);
        lp.rightMargin = px(4);
        return lp;
    }

    private EditText campo(String etichetta, String valore, boolean numero, boolean modificabile) {
        TextView t = new TextView(this);
        t.setText(etichetta);
        t.setTextColor(TESTO2);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setPadding(px(2), px(12), 0, px(4));
        contenutoDestro.addView(t);
        EditText e = new EditText(this);
        e.setText(valore);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        e.setTextColor(TESTO);
        e.setSingleLine(true);
        e.setPadding(px(12), px(10), px(12), px(10));
        e.setBackground(arrotondato(modificabile ? Color.WHITE : 0xFFEEF1F5, BORDO, 8));
        e.setBackgroundTintList((ColorStateList) null);
        if (numero) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setEnabled(modificabile);
        contenutoDestro.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    private static String formatta(double d) {
        if (d == Math.rint(d)) return String.valueOf((long) d);
        return String.format(Locale.ITALY, "%.1f", d);
    }

    /** Span con un font diverso (le icone Font Awesome davanti al nome del modo). */
    private static final class CustomTypefaceSpan extends android.text.style.MetricAffectingSpan {
        private final Typeface tf;

        CustomTypefaceSpan(Typeface tf) {
            this.tf = tf;
        }

        @Override
        public void updateDrawState(android.text.TextPaint p) {
            p.setTypeface(tf);
        }

        @Override
        public void updateMeasureState(android.text.TextPaint p) {
            p.setTypeface(tf);
        }
    }
}
