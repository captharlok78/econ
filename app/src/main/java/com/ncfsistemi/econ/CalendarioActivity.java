package com.ncfsistemi.econ;

import androidx.appcompat.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Rapportini;
import com.ncfsistemi.econ.db.table.StatiDocumento;
import com.ncfsistemi.econ.utils.CalendarioPersonale;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;
import com.ncfsistemi.econ.utils.GiustificativiLocali;
import com.ncfsistemi.econ.utils.RegoleRapportino;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Calendario personale (CALENDARIO_PERSONALE.md): settimana (predefinita) o mese con le ore dell'utente nei rapportini
 * (lavoro e viaggio, per rapportino, con il colore dello stato) e, se la ditta usa la pianificazione, i suoi cantieri
 * pianificati. Il tocco su un giorno apre il dettaglio: ore, pianificazione, rapportini aperti in cui aggiungere le ore
 * (suoi o della sua squadra, secondo le regole della scheda del rapportino) e un nuovo rapportino con quella data.
 * Con la funzionalita' CALENDARIO.GIUSTIFICATIVI anche i giustificativi della persona (STUDIO_GIUSTIFICATIVI_APP.md):
 * si chiedono da qui (un giorno o un periodo), partono da soli verso il server e tornano con l'esito dell'approvazione.
 * Tutto dai dati del tablet.
 */
public class CalendarioActivity extends AppCompatActivity {

    private static final String STATO_VISTA = "vista";
    private static final String STATO_GIORNO = "giorno";
    private static final int BLU = 0xFF1565C0;
    private static final int VERDE = 0xFF2E7D32;
    private static final int GRIGIO = 0xFF757575;
    private static final int TESTO = 0xFF212121;

    private boolean mese = false;
    /** Giorno di riferimento (yyyyMMdd): la settimana o il mese che lo contiene. */
    private long riferimento;
    private RegoleRapportino.Utente utente;
    private final Locale it = Locale.ITALY;
    private final String[] nomiMesi = new DateFormatSymbols(Locale.ITALY).getMonths();
    /** Giustificativi del periodo disegnato, per giorno (yyyyMMdd). */
    private Map<Long, List<GiustificativiLocali.Voce>> giust = new HashMap<>();
    private boolean giustAttivi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);
        FaIcone.applica(findViewById(R.id.periodoPrima), FaIcone.INDIETRO, "Periodo precedente");
        FaIcone.applica(findViewById(R.id.periodoDopo), FaIcone.AVANTI, "Periodo successivo");
        ((TextView) findViewById(R.id.periodoPrima)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        ((TextView) findViewById(R.id.periodoDopo)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        riferimento = CalendarioPersonale.oggi();
        if (savedInstanceState != null) {
            mese = savedInstanceState.getBoolean(STATO_VISTA, false);
            riferimento = savedInstanceState.getLong(STATO_GIORNO, riferimento);
        }
        // invio dei giustificativi finito (anche partito da qui): si ridisegna con gli esiti
        WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(GiustificativiLocali.LAVORO).observe(this, infos -> {
            for (WorkInfo i : infos) {
                if (i.getState() == WorkInfo.State.SUCCEEDED && utente != null && !isFinishing()) {
                    disegna();
                    mostraAvvisi();
                    return;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // anche al ritorno da un rapportino: le ore possono essere cambiate
        DbInterno db = new DbInterno(this);
        try {
            utente = RegoleRapportino.utenteCorrente(db, this);
        } finally {
            db.close();
        }
        giustAttivi = GiustificativiLocali.attivo(this);
        disegna();
        if (giustAttivi) {
            mostraAvvisi();
            // invia quelli in sospeso e riprende gli esiti delle approvazioni
            GiustificativiLocali.inviaInBackground(this);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean(STATO_VISTA, mese);
        out.putLong(STATO_GIORNO, riferimento);
    }

    // ── comandi ──────────────────────────────────────────────────────────────

    public void chiudi(View v) {
        finish();
    }

    public void vistaSettimana(View v) {
        mese = false;
        disegna();
    }

    public void vistaMese(View v) {
        mese = true;
        disegna();
    }

    public void prima(View v) {
        sposta(-1);
    }

    public void dopo(View v) {
        sposta(1);
    }

    public void oggi(View v) {
        riferimento = CalendarioPersonale.oggi();
        disegna();
    }

    private void sposta(int verso) {
        if (mese) {
            Calendar c = CalendarioPersonale.calendario(riferimento);
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.add(Calendar.MONTH, verso);
            riferimento = CalendarioPersonale.giornoDi(c);
        } else {
            riferimento = CalendarioPersonale.piuGiorni(riferimento, 7 * verso);
        }
        disegna();
    }

    // ── disegno ──────────────────────────────────────────────────────────────

    private void disegna() {
        evidenziaVista((TextView) findViewById(R.id.vistaSettimana), !mese);
        evidenziaVista((TextView) findViewById(R.id.vistaMese), mese);
        LinearLayout contenuto = findViewById(R.id.contenuto);
        contenuto.removeAllViews();

        if (utente == null || utente.idUtenteDitta <= 0) {
            ((TextView) findViewById(R.id.periodoTitolo)).setText("");
            ((TextView) findViewById(R.id.periodoTotali)).setText("");
            contenuto.addView(nota("Il tuo profilo di operatore non è ancora sul dispositivo: fai una sincronizzazione."));
            return;
        }

        if (giustAttivi) contenuto.addView(pulsanteGiustificativo());

        long dal, al;
        Calendar c = CalendarioPersonale.calendario(riferimento);
        if (mese) {
            c.set(Calendar.DAY_OF_MONTH, 1);
            long primo = CalendarioPersonale.giornoDi(c);
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            long ultimo = CalendarioPersonale.giornoDi(c);
            dal = CalendarioPersonale.lunedi(primo);
            al = CalendarioPersonale.piuGiorni(CalendarioPersonale.lunedi(ultimo), 6);
            ((TextView) findViewById(R.id.periodoTitolo)).setText(maiuscola(nomiMesi[(int) (primo / 100 % 100) - 1]) + " " + primo / 10000);
            List<CalendarioPersonale.Giorno> giorni = carica(dal, al);
            totali(giorni, primo, ultimo);
            disegnaMese(contenuto, giorni, primo / 100);
        } else {
            dal = CalendarioPersonale.lunedi(riferimento);
            al = CalendarioPersonale.piuGiorni(dal, 6);
            ((TextView) findViewById(R.id.periodoTitolo)).setText(titoloSettimana(dal, al));
            List<CalendarioPersonale.Giorno> giorni = carica(dal, al);
            totali(giorni, dal, al);
            disegnaSettimana(contenuto, giorni);
        }
    }

    private List<CalendarioPersonale.Giorno> carica(long dal, long al) {
        giust = giustAttivi ? GiustificativiLocali.periodo(this, dal, al) : new HashMap<>();
        DbInterno db = new DbInterno(this);
        try {
            return CalendarioPersonale.periodo(db, this, utente, dal, al);
        } finally {
            db.close();
        }
    }

    private void totali(List<CalendarioPersonale.Giorno> giorni, long dal, long al) {
        int lavoro = 0, viaggio = 0, giorniLavorati = 0;
        for (CalendarioPersonale.Giorno g : giorni) {
            if (g.giorno < dal || g.giorno > al) continue;
            lavoro += g.minutiLavoro();
            viaggio += g.minutiViaggio();
            if (g.minutiLavoro() > 0) giorniLavorati++;
        }
        String testo = lavoro == 0 && viaggio == 0 ? "Nessuna ora nei rapportini"
                : "Lavoro " + CalendarioPersonale.ore(lavoro) + (viaggio > 0 ? " · viaggio " + CalendarioPersonale.ore(viaggio) : "")
                + " · " + giorniLavorati + (giorniLavorati == 1 ? " giorno" : " giorni");
        int minutiGiust = 0;
        for (Map.Entry<Long, List<GiustificativiLocali.Voce>> e : giust.entrySet()) {
            if (e.getKey() < dal || e.getKey() > al) continue;
            for (GiustificativiLocali.Voce v : e.getValue()) if (!v.isRifiutato() && !v.nonAccettato) minutiGiust += minuti(v.ore);
        }
        if (minutiGiust > 0) testo += " · giustificativi " + CalendarioPersonale.ore(minutiGiust);
        ((TextView) findViewById(R.id.periodoTotali)).setText(testo);
    }

    /** Tablet largo: sette colonne affiancate; altrimenti un giorno sotto l'altro. */
    private void disegnaSettimana(LinearLayout contenuto, List<CalendarioPersonale.Giorno> giorni) {
        boolean colonne = getResources().getConfiguration().screenWidthDp >= 720;
        LinearLayout riga = null;
        if (colonne) {
            riga = new LinearLayout(this);
            riga.setOrientation(LinearLayout.HORIZONTAL);
            contenuto.addView(riga, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        for (CalendarioPersonale.Giorno g : giorni) {
            View cella = cellaSettimana(g, colonne);
            if (colonne) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                lp.setMargins(dp(3), 0, dp(3), 0);
                riga.addView(cella, lp);
            } else {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, dp(4), 0, dp(4));
                contenuto.addView(cella, lp);
            }
        }
        if (utente.pianificazione) {
            contenuto.addView(nota("Tocca un giorno per i dettagli e i rapportini in cui aggiungere le ore. In verde i cantieri pianificati."));
        } else {
            contenuto.addView(nota("Tocca un giorno per i dettagli e i rapportini in cui aggiungere le ore."));
        }
    }

    private View cellaSettimana(CalendarioPersonale.Giorno g, boolean colonna) {
        LinearLayout cella = new LinearLayout(this);
        cella.setOrientation(LinearLayout.VERTICAL);
        cella.setPadding(dp(8), dp(8), dp(8), dp(10));
        cella.setBackground(sfondo(Color.WHITE, g.giorno == CalendarioPersonale.oggi() ? BLU : 0xFFE0E0E0, 8));
        cella.setMinimumHeight(colonna ? dp(260) : 0);
        cella.setOnClickListener(v -> dettaglioGiorno(g.giorno));

        // testata: "Lun 12" e totale
        LinearLayout testa = new LinearLayout(this);
        testa.setOrientation(colonna ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        testa.setGravity(colonna ? Gravity.START : Gravity.CENTER_VERTICAL);
        TextView nome = testo(nomeGiorno(g.giorno), 15, g.giorno == CalendarioPersonale.oggi() ? BLU : TESTO, true);
        testa.addView(nome, new LinearLayout.LayoutParams(colonna ? LinearLayout.LayoutParams.WRAP_CONTENT : 0, LinearLayout.LayoutParams.WRAP_CONTENT, colonna ? 0f : 1f));
        String tot = CalendarioPersonale.ore(g.minutiLavoro());
        if (g.minutiViaggio() > 0) tot += (tot.isEmpty() ? "" : " + ") + CalendarioPersonale.ore(g.minutiViaggio()) + " viaggio";
        testa.addView(testo(tot, 13, BLU, true));
        cella.addView(testa);

        for (CalendarioPersonale.Voce v : g.voci) {
            cella.addView(voce(v, colonna));
        }
        for (ContentValues p : g.pianificazione) {
            cella.addView(pianificato(p));
        }
        List<GiustificativiLocali.Voce> delGiorno = giustDi(g.giorno);
        for (GiustificativiLocali.Voce v : delGiorno) {
            cella.addView(giustificativo(v, colonna));
        }
        if (g.voci.isEmpty() && g.pianificazione.isEmpty() && delGiorno.isEmpty()) {
            TextView vuoto = testo("—", 13, 0xFFBDBDBD, false);
            vuoto.setPadding(0, dp(6), 0, 0);
            cella.addView(vuoto);
        }
        return cella;
    }

    /** Una voce di ore: barra del colore dello stato, luogo, ore e orari; il tocco apre il rapportino. */
    private View voce(CalendarioPersonale.Voce v, boolean stretta) {
        LinearLayout riga = new LinearLayout(this);
        riga.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpr = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpr.setMargins(0, dp(6), 0, 0);
        riga.setLayoutParams(lpr);
        riga.setBackground(sfondo(0xFFF5F8FD, 0, 6));
        riga.setOnClickListener(x -> apriRapportino(v.idRapportino));

        View barra = new View(this);
        barra.setBackgroundColor(coloreStato(v.stato));
        riga.addView(barra, new LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT));

        LinearLayout testi = new LinearLayout(this);
        testi.setOrientation(LinearLayout.VERTICAL);
        testi.setPadding(dp(6), dp(4), dp(6), dp(4));
        TextView luogo = testo(v.luogo, 13, TESTO, true);
        luogo.setMaxLines(stretta ? 2 : 1);
        luogo.setEllipsize(TextUtils.TruncateAt.END);
        testi.addView(luogo);
        StringBuilder det = new StringBuilder(CalendarioPersonale.ore(v.minutiLavoro));
        if (v.minutiViaggio > 0) det.append(det.length() > 0 ? " + " : "").append(CalendarioPersonale.ore(v.minutiViaggio)).append(" viaggio");
        if (!v.orari.isEmpty()) det.append(stretta ? "\n" : " · ").append(v.orari);
        testi.addView(testo(det.toString(), 12, GRIGIO, false));
        if (!stretta && v.stato != null) {
            testi.addView(testo(statoNome(v), 11, coloreStato(v.stato), false));
        }
        riga.addView(testi, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return riga;
    }

    /** Blocco di pianificazione: cantiere, mattina/pomeriggio, ore, squadra. */
    private View pianificato(ContentValues p) {
        LinearLayout riga = new LinearLayout(this);
        riga.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpr = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpr.setMargins(0, dp(6), 0, 0);
        riga.setLayoutParams(lpr);
        riga.setBackground(sfondo(0xFFE8F5E9, 0, 6));
        riga.setPadding(dp(6), dp(4), dp(6), dp(4));
        TextView icona = new TextView(this);
        FaIcone.applica(icona, FaIcone.PIANIFICATO, null);
        icona.setTextColor(VERDE);
        icona.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        riga.addView(icona, new LinearLayout.LayoutParams(dp(18), LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout testi = new LinearLayout(this);
        testi.setOrientation(LinearLayout.VERTICAL);
        TextView nome = testo(val(p, "nome_cantiere"), 13, VERDE, true);
        nome.setMaxLines(2);
        nome.setEllipsize(TextUtils.TruncateAt.END);
        testi.addView(nome);
        testi.addView(testo(descrizionePianificazione(p), 12, 0xFF558B2F, false));
        riga.addView(testi, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return riga;
    }

    private String descrizionePianificazione(ContentValues p) {
        String fascia = "M".equalsIgnoreCase(val(p, "fascia")) ? "Mattina" : "P".equalsIgnoreCase(val(p, "fascia")) ? "Pomeriggio" : val(p, "fascia");
        Integer minuti = p.getAsInteger("minuti");
        String s = fascia + (minuti != null && minuti > 0 ? " · " + CalendarioPersonale.ore(minuti) : "");
        String squadra = val(p, "squadra");
        return squadra.isEmpty() ? s : s + " · " + squadra;
    }

    /** Mese: griglia lunedi'-domenica con le ore del giorno e un segno per la pianificazione. */
    private void disegnaMese(LinearLayout contenuto, List<CalendarioPersonale.Giorno> giorni, long annoMese) {
        LinearLayout intestazione = new LinearLayout(this);
        for (String n : new String[]{"Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom"}) {
            TextView t = testo(n, 12, GRIGIO, true);
            t.setGravity(Gravity.CENTER);
            intestazione.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
        contenuto.addView(intestazione);
        LinearLayout riga = null;
        for (int i = 0; i < giorni.size(); i++) {
            if (i % 7 == 0) {
                riga = new LinearLayout(this);
                contenuto.addView(riga, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            CalendarioPersonale.Giorno g = giorni.get(i);
            boolean delMese = g.giorno / 100 == annoMese;
            LinearLayout cella = new LinearLayout(this);
            cella.setOrientation(LinearLayout.VERTICAL);
            cella.setPadding(dp(6), dp(4), dp(6), dp(6));
            cella.setMinimumHeight(dp(72));
            cella.setBackground(sfondo(delMese ? Color.WHITE : 0xFFFAFAFA, g.giorno == CalendarioPersonale.oggi() ? BLU : 0xFFE0E0E0, 4));
            cella.setOnClickListener(v -> dettaglioGiorno(g.giorno));
            cella.addView(testo(String.valueOf(g.giorno % 100), 13, delMese ? (g.giorno == CalendarioPersonale.oggi() ? BLU : TESTO) : 0xFFBDBDBD, true));
            if (g.minutiLavoro() > 0) cella.addView(testo(CalendarioPersonale.ore(g.minutiLavoro()), 12, delMese ? BLU : 0xFF90A4AE, true));
            if (g.minutiViaggio() > 0) cella.addView(testo("+" + CalendarioPersonale.ore(g.minutiViaggio()), 11, GRIGIO, false));
            if (!g.pianificazione.isEmpty()) {
                TextView p = new TextView(this);
                FaIcone.applica(p, FaIcone.PIANIFICATO, null);
                p.setTextColor(delMese ? VERDE : 0xFFA5D6A7);
                p.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                p.setGravity(Gravity.START);
                cella.addView(p);
            }
            List<GiustificativiLocali.Voce> giustMese = giustDi(g.giorno);
            if (!giustMese.isEmpty()) {
                LinearLayout icone = new LinearLayout(this);
                for (GiustificativiLocali.Voce v : giustMese) {
                    TextView ic = iconaGiustificativo(v, 11);
                    ic.setPadding(0, 0, dp(3), 0);
                    if (!delMese) ic.setAlpha(0.4f);
                    icone.addView(ic);
                }
                cella.addView(icone);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            riga.addView(cella, lp);
        }
        contenuto.addView(nota((utente.pianificazione ? "Ore di lavoro in blu, viaggio in grigio; l'icona verde segna i giorni pianificati."
                : "Ore di lavoro in blu, viaggio in grigio.") + (giustAttivi ? " Le icone colorate sono i giustificativi." : "") + " Tocca un giorno per i dettagli."));
    }

    // ── dettaglio del giorno ─────────────────────────────────────────────────

    private void dettaglioGiorno(long giorno) {
        List<CalendarioPersonale.Giorno> giorni = carica(giorno, giorno);
        CalendarioPersonale.Giorno g = giorni.get(0);
        List<ContentValues> aperti;
        DbInterno db = new DbInterno(this);
        try {
            aperti = CalendarioPersonale.rapportiniAperti(db, this, utente, giorno);
        } finally {
            db.close();
        }

        LinearLayout corpo = new LinearLayout(this);
        corpo.setOrientation(LinearLayout.VERTICAL);
        corpo.setPadding(dp(20), dp(4), dp(20), dp(8));
        AlertDialog[] dialogo = new AlertDialog[1];

        String tot = CalendarioPersonale.ore(g.minutiLavoro());
        corpo.addView(sezione(FaIcone.ORE, "Le tue ore" + (tot.isEmpty() ? "" : " · " + tot
                + (g.minutiViaggio() > 0 ? " + " + CalendarioPersonale.ore(g.minutiViaggio()) + " viaggio" : ""))));
        if (g.voci.isEmpty()) corpo.addView(nota("Nessuna ora nei rapportini di questo giorno."));
        for (CalendarioPersonale.Voce v : g.voci) {
            View r = voce(v, false);
            r.setOnClickListener(x -> {
                dialogo[0].dismiss();
                apriRapportino(v.idRapportino);
            });
            corpo.addView(r);
        }

        if (utente.pianificazione) {
            corpo.addView(sezione(FaIcone.PIANIFICATO, "La tua pianificazione"));
            if (g.pianificazione.isEmpty()) corpo.addView(nota("Nessun cantiere pianificato."));
            for (ContentValues p : g.pianificazione) corpo.addView(pianificato(p));
        }

        if (giustAttivi) {
            corpo.addView(sezione(FaIcone.GIUSTIFICATIVO, "Giustificativi"));
            List<GiustificativiLocali.Voce> delGiorno = giustDi(giorno);
            if (delGiorno.isEmpty()) corpo.addView(nota("Nessun giustificativo in questo giorno."));
            for (GiustificativiLocali.Voce v : delGiorno) {
                View r = giustificativo(v, false);
                r.setOnClickListener(x -> {
                    dialogo[0].dismiss();
                    apriGiustificativo(v);
                });
                corpo.addView(r);
            }
            TextView chiedi = testo("+ Chiedi un giustificativo per questo giorno", 14, BLU, true);
            chiedi.setPadding(dp(4), dp(10), dp(4), dp(6));
            chiedi.setOnClickListener(x -> {
                dialogo[0].dismiss();
                nuovoGiustificativo(giorno);
            });
            corpo.addView(chiedi);
        }

        corpo.addView(sezione(FaIcone.PIU, "Aggiungi le ore a un rapportino"));
        if (aperti.isEmpty()) {
            corpo.addView(nota("Nessun rapportino aperto tuo o della tua squadra in questo giorno."));
        }
        for (ContentValues t : aperti) {
            int id = t.getAsInteger(Rapportini.ID);
            corpo.addView(rapportinoAperto(t, () -> {
                dialogo[0].dismiss();
                apriRapportino(id);
            }));
        }

        ScrollView scorri = new ScrollView(this);
        scorri.addView(corpo);
        AlertDialog.Builder b = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(maiuscola(nomeGiornoEsteso(giorno)))
                .setView(scorri)
                .setNegativeButton("Chiudi", null);
        if (FunzionalitaApp.puoInserire(this, new Rapportini(), null)) {
            b.setPositiveButton("Nuovo rapportino", (d, w) -> nuovoRapportino(giorno));
        }
        dialogo[0] = b.show();
    }

    private View rapportinoAperto(ContentValues t, Runnable apri) {
        LinearLayout riga = new LinearLayout(this);
        riga.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpr = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpr.setMargins(0, dp(6), 0, 0);
        riga.setLayoutParams(lpr);
        riga.setBackground(sfondo(Color.WHITE, 0xFFBBDEFB, 6));
        riga.setPadding(dp(10), dp(6), dp(10), dp(6));
        riga.setOnClickListener(v -> apri.run());
        String cantiere = val(t, "cantiere"), cliente = val(t, "cliente");
        riga.addView(testo(!cantiere.isEmpty() ? cantiere : (!cliente.isEmpty() ? cliente : "Rapportino"), 14, TESTO, true));
        DbInterno db = new DbInterno(this);
        String stato;
        try {
            ContentValues s = RegoleRapportino.stato(db, t.getAsInteger(Rapportini.ID_STATO) != null ? t.getAsInteger(Rapportini.ID_STATO) : 0);
            stato = s != null ? val(s, StatiDocumento.NOME) : "Bozza";
        } finally {
            db.close();
        }
        String autore = val(t, "autore");
        riga.addView(testo((autore.isEmpty() ? "" : "di " + autore + " · ") + stato + " · tocca per aprirlo e aggiungere le tue ore", 12, GRIGIO, false));
        return riga;
    }

    private void apriRapportino(int id) {
        Intent intent = new Intent(this, RapportinoDettaglioModActivity.class);
        intent.putExtra("ID", id);
        startActivity(intent);
    }

    private void nuovoRapportino(long giorno) {
        Intent intent = new Intent(this, RapportinoDettaglioModActivity.class);
        // data del rapportino nuovo (RapportinoDettaglioModActivity.EXTRA_DATA), alle 8 del giorno scelto
        intent.putExtra(RapportinoDettaglioModActivity.EXTRA_DATA, giorno * 1000000L + 80000L);
        startActivity(intent);
    }

    // ── giustificativi ───────────────────────────────────────────────────────

    private List<GiustificativiLocali.Voce> giustDi(long giorno) {
        List<GiustificativiLocali.Voce> l = giust.get(giorno);
        return l != null ? l : new ArrayList<>();
    }

    private static int minuti(double ore) {
        return (int) Math.round(ore * 60);
    }

    /** Pulsante in cima: un giustificativo da oggi (o dal primo giorno del periodo se oggi non c'e'). */
    private View pulsanteGiustificativo() {
        TextView b = testo("+ Chiedi un giustificativo (ferie, permessi…)", 14, BLU, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(10), dp(12), dp(10));
        b.setBackground(sfondo(Color.WHITE, 0xFFBBDEFB, 8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> nuovoGiustificativo(CalendarioPersonale.oggi()));
        return b;
    }

    private TextView iconaGiustificativo(GiustificativiLocali.Voce v, int sp) {
        TextView ic = new TextView(this);
        FaIcone.applica(ic, FaIcone.daNome(this, v.tipologia.icona), v.tipologia.descrizione);
        ic.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        ic.setTextColor(coloreTipologia(v.tipologia));
        if (v.isRifiutato() || v.nonAccettato) ic.setAlpha(0.35f);
        else if (!v.isApprovato()) ic.setAlpha(0.6f);
        return ic;
    }

    private int coloreTipologia(GiustificativiLocali.Tipologia t) {
        try {
            return t.colore != null && !t.colore.isEmpty() ? Color.parseColor(t.colore) : BLU;
        } catch (IllegalArgumentException e) {
            return BLU;
        }
    }

    /** Riga di un giustificativo: icona e tipologia, ore e pastiglia dello stato; il tocco la apre. */
    private View giustificativo(GiustificativiLocali.Voce v, boolean stretta) {
        LinearLayout riga = new LinearLayout(this);
        riga.setOrientation(LinearLayout.HORIZONTAL);
        riga.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lpr = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpr.setMargins(0, dp(6), 0, 0);
        riga.setLayoutParams(lpr);
        riga.setBackground(sfondo(0xFFFFF8E1, 0, 6));
        riga.setPadding(dp(6), dp(4), dp(6), dp(4));
        riga.setOnClickListener(x -> apriGiustificativo(v));
        riga.setGravity(Gravity.TOP);
        TextView ic = iconaGiustificativo(v, 13);
        ic.setPadding(0, dp(2), 0, 0);
        riga.addView(ic, new LinearLayout.LayoutParams(dp(20), LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout testi = new LinearLayout(this);
        testi.setOrientation(LinearLayout.VERTICAL);
        TextView nome = testo(v.tipologia.descrizione, 13, TESTO, true);
        nome.setMaxLines(stretta ? 2 : 1);
        nome.setEllipsize(TextUtils.TruncateAt.END);
        testi.addView(nome);
        String det = CalendarioPersonale.ore(minuti(v.ore));
        if (!stretta && !v.note.isEmpty()) det += " · " + v.note;
        TextView dettaglio = testo(det, 12, GRIGIO, false);
        dettaglio.setMaxLines(1);
        dettaglio.setEllipsize(TextUtils.TruncateAt.END);
        testi.addView(dettaglio);
        TextView stato = testo(v.etichettaStato(), 11, Color.WHITE, true);
        stato.setBackground(sfondo(v.coloreStato(), 0, 8));
        stato.setPadding(dp(6), dp(1), dp(6), dp(1));
        LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lps.setMargins(0, dp(2), 0, 0);
        testi.addView(stato, lps);
        riga.addView(testi, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return riga;
    }

    /** Avvisi del server (es. modifica non accettata perché già approvata). */
    private void mostraAvvisi() {
        String avvisi = GiustificativiLocali.prendiAvvisi(this);
        if (avvisi.isEmpty()) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Giustificativi").setMessage(avvisi).setPositiveButton("OK", null).show();
    }

    /** Nuova richiesta: tipologia, dal / al, ore al giorno (vuoto = orario della ditta), note. */
    private void nuovoGiustificativo(long giorno) {
        List<GiustificativiLocali.Tipologia> tipi = GiustificativiLocali.tipologie(this);
        if (tipi.isEmpty()) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Giustificativi")
                    .setMessage("Le tipologie (ferie, permessi…) non sono ancora sul dispositivo: fai una sincronizzazione con la rete attiva.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        long[] periodo = {giorno, giorno};
        LinearLayout corpo = moduloCorpo();
        Spinner tipologia = spinnerTipologie(tipi, 0);
        corpo.addView(etichetta("Tipologia"));
        corpo.addView(tipologia);

        TextView dal = campoData(), al = campoData();
        Runnable aggiornaDate = () -> {
            dal.setText(dataLeggibile(periodo[0]));
            al.setText(dataLeggibile(periodo[1]));
        };
        aggiornaDate.run();
        dal.setOnClickListener(x -> scegliData(periodo[0], g -> {
            periodo[0] = g;
            if (periodo[1] < g) periodo[1] = g;
            aggiornaDate.run();
        }));
        al.setOnClickListener(x -> scegliData(periodo[1], g -> {
            periodo[1] = g;
            if (periodo[0] > g) periodo[0] = g;
            aggiornaDate.run();
        }));
        LinearLayout date = new LinearLayout(this);
        LinearLayout colDal = new LinearLayout(this), colAl = new LinearLayout(this);
        colDal.setOrientation(LinearLayout.VERTICAL);
        colAl.setOrientation(LinearLayout.VERTICAL);
        colDal.addView(etichetta("Dal"));
        colDal.addView(dal);
        colAl.addView(etichetta("Al"));
        colAl.addView(al);
        LinearLayout.LayoutParams metà = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        metà.setMargins(0, 0, dp(8), 0);
        date.addView(colDal, metà);
        date.addView(colAl, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        corpo.addView(date);

        EditText ore = campoOre(null);
        ore.setHint("come l'orario della ditta");
        corpo.addView(etichetta("Ore al giorno"));
        corpo.addView(ore);
        EditText note = campoNote("");
        corpo.addView(etichetta("Note"));
        corpo.addView(note);
        corpo.addView(nota("Più giorni: se ne crea uno per ogni giorno lavorativo. La richiesta parte da sola appena c'è rete; il coordinatore o l'amministratore la approva dal portale."));

        AlertDialog d = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Chiedi un giustificativo")
                .setView(scorri(corpo))
                .setPositiveButton("Chiedi", null)
                .setNegativeButton("Annulla", null)
                .show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            try {
                Double h = ore.getText().toString().trim().isEmpty() ? null : numero(ore);
                int n = GiustificativiLocali.richiedi(this, ((GiustificativiLocali.Tipologia) tipologia.getSelectedItem()).id,
                        periodo[0], periodo[1], h, note.getText().toString());
                d.dismiss();
                Toast.makeText(this, (n == 1 ? "Richiesta salvata" : "Richiesta salvata: " + n + " giorni") + ", parte appena c'è rete.", Toast.LENGTH_LONG).show();
                riferimento = periodo[0];
                disegna();
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Un giustificativo: in attesa o da inviare si cambia o si toglie; deciso si consulta (il rifiutato si toglie). */
    private void apriGiustificativo(GiustificativiLocali.Voce v) {
        LinearLayout corpo = moduloCorpo();
        StringBuilder info = new StringBuilder(maiuscola(nomeGiornoEsteso(v.giorno))).append("\nStato: ").append(v.etichettaStato());
        if (v.isApprovato() && !v.approvatoDa.isEmpty()) info.append(" da ").append(v.approvatoDa);
        if (v.isRifiutato()) {
            info.append(v.approvatoDa.isEmpty() ? "" : " da " + v.approvatoDa);
            if (!v.motivoRifiuto.isEmpty()) info.append("\nMotivo: ").append(v.motivoRifiuto);
        }
        if (v.nonAccettato && !v.errore.isEmpty()) info.append("\n").append(v.errore);
        TextView t = testo(info.toString(), 14, TESTO, false);
        t.setPadding(0, 0, 0, dp(8));
        corpo.addView(t);

        AlertDialog.Builder b = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(v.tipologia.descrizione).setNegativeButton("Chiudi", null);
        if (!v.modificabile()) {
            corpo.addView(testo(CalendarioPersonale.ore(minuti(v.ore)) + (v.note.isEmpty() ? "" : " · " + v.note), 14, GRIGIO, false));
            if (v.eliminabile()) b.setNeutralButton("Togli", (d, w) -> togliGiustificativo(v));
            b.setView(scorri(corpo)).show();
            return;
        }
        List<GiustificativiLocali.Tipologia> tipi = GiustificativiLocali.tipologie(this);
        int scelta = 0;
        for (int i = 0; i < tipi.size(); i++) if (tipi.get(i).id == v.idTipologia) scelta = i;
        Spinner tipologia = spinnerTipologie(tipi, scelta);
        corpo.addView(etichetta("Tipologia"));
        corpo.addView(tipologia);
        EditText ore = campoOre(v.ore);
        corpo.addView(etichetta("Ore"));
        corpo.addView(ore);
        EditText note = campoNote(v.note);
        corpo.addView(etichetta("Note"));
        corpo.addView(note);
        AlertDialog d = b.setView(scorri(corpo))
                .setPositiveButton("Salva", null)
                .setNeutralButton("Togli", (x, w) -> togliGiustificativo(v))
                .show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            try {
                Double h = numero(ore);
                if (h == null || tipi.isEmpty()) throw new IllegalArgumentException("Indica le ore.");
                GiustificativiLocali.modifica(this, v.id, ((GiustificativiLocali.Tipologia) tipologia.getSelectedItem()).id, h, note.getText().toString());
                d.dismiss();
                disegna();
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void togliGiustificativo(GiustificativiLocali.Voce v) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Togli il giustificativo")
                .setMessage(v.tipologia.descrizione + " di " + nomeGiornoEsteso(v.giorno) + "?")
                .setPositiveButton("Togli", (d, w) -> {
                    GiustificativiLocali.elimina(this, v.id);
                    disegna();
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    private LinearLayout moduloCorpo() {
        LinearLayout corpo = new LinearLayout(this);
        corpo.setOrientation(LinearLayout.VERTICAL);
        corpo.setPadding(dp(20), dp(8), dp(20), dp(4));
        return corpo;
    }

    private ScrollView scorri(View v) {
        ScrollView s = new ScrollView(this);
        s.addView(v);
        return s;
    }

    private TextView etichetta(String t) {
        TextView tv = testo(t, 12, GRIGIO, true);
        tv.setPadding(0, dp(10), 0, dp(2));
        return tv;
    }

    private Spinner spinnerTipologie(List<GiustificativiLocali.Tipologia> tipi, int scelta) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<GiustificativiLocali.Tipologia> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tipi);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(a);
        if (scelta < tipi.size()) sp.setSelection(scelta);
        return sp;
    }

    private TextView campoData() {
        TextView tv = testo("", 15, TESTO, false);
        tv.setBackgroundResource(R.drawable.bg_input_outline);
        tv.setPadding(dp(10), dp(10), dp(10), dp(10));
        return tv;
    }

    private EditText campoOre(Double valore) {
        EditText e = new EditText(this);
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789.,"));
        if (valore != null) e.setText(numeroLeggibile(valore));
        return e;
    }

    private EditText campoNote(String valore) {
        EditText e = new EditText(this);
        e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(255)});
        e.setText(valore);
        return e;
    }

    private interface SceltaGiorno {
        void scelto(long giorno);
    }

    private void scegliData(long giorno, SceltaGiorno dopo) {
        int g = (int) giorno;
        new android.app.DatePickerDialog(this, (view, anno, mese0, giornoMese) -> dopo.scelto(anno * 10000L + (mese0 + 1) * 100L + giornoMese),
                g / 10000, (g / 100) % 100 - 1, g % 100).show();
    }

    private static Double numero(EditText e) {
        String t = e.getText().toString().trim().replace(',', '.');
        if (t.isEmpty()) return null;
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Ore non valide.");
        }
    }

    private static String numeroLeggibile(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v).replace('.', ',');
    }

    private String dataLeggibile(long giorno) {
        Calendar c = CalendarioPersonale.calendario(giorno);
        String n = new DateFormatSymbols(it).getShortWeekdays()[c.get(Calendar.DAY_OF_WEEK)].replace(".", "");
        return maiuscola(n) + " " + String.format(Locale.ITALY, "%02d/%02d/%04d", giorno % 100, giorno / 100 % 100, giorno / 10000);
    }

    // ── piccoli componenti ───────────────────────────────────────────────────

    private View sezione(String glifo, String titolo) {
        LinearLayout s = new LinearLayout(this);
        s.setGravity(Gravity.CENTER_VERTICAL);
        s.setPadding(0, dp(14), 0, dp(2));
        TextView icona = new TextView(this);
        FaIcone.applica(icona, glifo, null);
        icona.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        s.addView(icona, new LinearLayout.LayoutParams(dp(24), LinearLayout.LayoutParams.WRAP_CONTENT));
        s.addView(testo(titolo, 15, TESTO, true));
        return s;
    }

    private TextView nota(String t) {
        TextView tv = testo(t, 13, GRIGIO, false);
        tv.setPadding(dp(4), dp(8), dp(4), dp(4));
        return tv;
    }

    private TextView testo(String t, int sp, int colore, boolean grassetto) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTextColor(colore);
        if (grassetto) tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        return tv;
    }

    private void evidenziaVista(TextView tv, boolean attiva) {
        tv.setBackground(attiva ? sfondo(0x33FFFFFF, 0, 18) : null);
        tv.setTypeface(null, attiva ? Typeface.BOLD : Typeface.NORMAL);
    }

    private GradientDrawable sfondo(int colore, int bordo, int raggioDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(colore);
        d.setCornerRadius(dp(raggioDp));
        if (bordo != 0) d.setStroke(dp(1), bordo);
        return d;
    }

    private int coloreStato(ContentValues stato) {
        return stato != null ? RegoleRapportino.coloreStato(val(stato, StatiDocumento.COLORE)) : 0xFF9E9E9E;
    }

    private String statoNome(CalendarioPersonale.Voce v) {
        String n = v.stato != null ? val(v.stato, StatiDocumento.NOME) : "";
        return v.numero.isEmpty() ? n : n + " · n. " + v.numero;
    }

    private String titoloSettimana(long dal, long al) {
        int gDal = (int) (dal % 100), gAl = (int) (al % 100);
        String mDal = nomiMesi[(int) (dal / 100 % 100) - 1], mAl = nomiMesi[(int) (al / 100 % 100) - 1];
        if (dal / 100 == al / 100) return gDal + " – " + gAl + " " + mAl + " " + al / 10000;
        if (dal / 10000 == al / 10000) return gDal + " " + mDal + " – " + gAl + " " + mAl + " " + al / 10000;
        return gDal + " " + mDal + " " + dal / 10000 + " – " + gAl + " " + mAl + " " + al / 10000;
    }

    private String nomeGiorno(long giorno) {
        Calendar c = CalendarioPersonale.calendario(giorno);
        String n = new DateFormatSymbols(it).getShortWeekdays()[c.get(Calendar.DAY_OF_WEEK)];
        return maiuscola(n.replace(".", "")) + " " + giorno % 100;
    }

    private String nomeGiornoEsteso(long giorno) {
        Calendar c = CalendarioPersonale.calendario(giorno);
        return new DateFormatSymbols(it).getWeekdays()[c.get(Calendar.DAY_OF_WEEK)] + " " + giorno % 100 + " "
                + nomiMesi[(int) (giorno / 100 % 100) - 1] + " " + giorno / 10000;
    }

    private static String maiuscola(String s) {
        return s == null || s.isEmpty() ? "" : s.substring(0, 1).toUpperCase(Locale.ITALY) + s.substring(1);
    }

    private static String val(ContentValues cv, String k) {
        String v = cv.getAsString(k);
        return v != null ? v.trim() : "";
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
