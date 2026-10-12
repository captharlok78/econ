package com.ncfsistemi.econ;

import android.app.AlertDialog;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Rapportini;
import com.ncfsistemi.econ.db.table.StatiDocumento;
import com.ncfsistemi.econ.utils.CalendarioPersonale;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;
import com.ncfsistemi.econ.utils.RegoleRapportino;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Calendario personale (CALENDARIO_PERSONALE.md): settimana (predefinita) o mese con le ore dell'utente nei rapportini
 * (lavoro e viaggio, per rapportino, con il colore dello stato) e, se la ditta usa la pianificazione, i suoi cantieri
 * pianificati. Il tocco su un giorno apre il dettaglio: ore, pianificazione, rapportini aperti in cui aggiungere le ore
 * (suoi o della sua squadra, secondo le regole della scheda del rapportino) e un nuovo rapportino con quella data.
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
        disegna();
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
        if (g.voci.isEmpty() && g.pianificazione.isEmpty()) {
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
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            riga.addView(cella, lp);
        }
        contenuto.addView(nota(utente.pianificazione ? "Ore di lavoro in blu, viaggio in grigio; l'icona verde segna i giorni pianificati. Tocca un giorno per i dettagli."
                : "Ore di lavoro in blu, viaggio in grigio. Tocca un giorno per i dettagli."));
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
        AlertDialog.Builder b = new AlertDialog.Builder(this)
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
