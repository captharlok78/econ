package com.ncfsistemi.econ;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.Preventivi;
import com.ncfsistemi.econ.db.table.PreventiviDettaglio;
import com.ncfsistemi.econ.fragments.PreventivoDettaglioFragment;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;
import com.ncfsistemi.econ.utils.RigheRapportino;
import com.ncfsistemi.econ.utils.SezioneStandard;
import com.ncfsistemi.econ.utils.Utility;
import com.ncfsistemi.econ.views.BarraPulsanti;

/**
 * Scheda di preventivo e ordine (STUDIO_PREVENTIVI_ORDINI.md §6.5) con la grafica del rapportino: testata standard con
 * lo stato, sezione dei dati del documento (matita = PreventiviDettaglioModActivity) e sezione delle righe
 * (PreventivoDettaglioFragment: + Articolo / + Lavoro con la finestra della riga del rapportino e il barcode, tabella e
 * totali) a fisarmonica; in fondo Chiudi, Apri nel cantiere (composizione elettrica) e Converti in ordine.
 * Extra: Preventivi.ID_PREVENTIVO.
 */
public class DocumentoActivity extends EconActivity {

    private static final int RICHIESTA_MODIFICA = 2;
    private static final int RICHIESTA_XLS = 100;

    private int idPreventivo;
    private ContentValues documento;
    private PreventivoDettaglioFragment righe;
    private boolean testataAperta = false, righeAperte = true;
    private TextView frecciaTestata, frecciaRighe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documento);
        idPreventivo = getIntent().getIntExtra(Preventivi.ID_PREVENTIVO, 0);
        if (!carica()) {
            finish();
            return;
        }
        if (savedInstanceState == null) {
            righe = new PreventivoDettaglioFragment();
            Bundle arg = new Bundle();
            arg.putInt(Preventivi.ID_PREVENTIVO, idPreventivo);
            arg.putString("DESC_PREVENTIVO", titoloDocumento());
            righe.setArguments(arg);
            getSupportFragmentManager().beginTransaction().replace(R.id.contenuto_righe, righe).commit();
        } else {
            righe = (PreventivoDettaglioFragment) getSupportFragmentManager().findFragmentById(R.id.contenuto_righe);
        }
        preparaSezioni();
        findViewById(R.id.button_cantiere).setOnClickListener(v -> apriNelCantiere());
        findViewById(R.id.button_crea_ordine).setOnClickListener(v -> com.ncfsistemi.econ.utils.AzioniDocumento.trasformaInOrdine(this, documento, () -> {
            setResult(RESULT_OK);
            ricarica();
        }));
        BarraPulsanti.disponi((LinearLayout) findViewById(R.id.barra_pulsanti));
        // tocco sullo stato: cambio di stato (aperto, accettato, rifiutato, chiuso), come dal menu dell'elenco
        findViewById(R.id.badge_stato).setOnClickListener(v -> {
            if (!FunzionalitaApp.puoModificare(this, FunzionalitaApp.moduloPreventivo(RigheRapportino.testo(documento, Preventivi.TIPO)))) return;
            com.ncfsistemi.econ.utils.AzioniDocumento.cambiaStato(this, documento, () -> {
                setResult(RESULT_OK);
                ricarica();
                if (righe != null) righe.ricerca();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ricarica();
    }

    /** Rilegge il documento (dopo la modifica della testata o la conversione) e ridisegna testata e pulsanti. */
    private void ricarica() {
        if (!carica()) {
            finish();
            return;
        }
        mostraTestata();
    }

    private boolean carica() {
        DbInterno db = new DbInterno(this);
        ContentValues where = new ContentValues();
        where.put(Preventivi.ID_PREVENTIVO, idPreventivo);
        documento = idPreventivo == 0 ? null : db.getRecord(new Preventivi(), where);
        if (documento != null) {
            ContentValues cc = new Preventivi().getClienteCantierePreventivo(db, idPreventivo);
            // solo cliente e cantiere: la join porta anche note e date del cliente, che coprirebbero quelle del documento
            if (cc != null) {
                documento.put(Anagrafica.RAGIONE_SOCIALE, RigheRapportino.testo(cc, Anagrafica.RAGIONE_SOCIALE));
                documento.put(Cantieri.NOME, RigheRapportino.testo(cc, Cantieri.NOME));
            }
        }
        db.close();
        return documento != null;
    }

    private boolean ordine() {
        return Preventivi.TIPO_ORDINE.equals(RigheRapportino.testo(documento, Preventivi.TIPO));
    }

    private String stato() {
        return RigheRapportino.testo(documento, Preventivi.STATO);
    }

    private String titoloDocumento() {
        return (ordine() ? "Ordine" : "Preventivo") + " n. " + RigheRapportino.testo(documento, Preventivi.NUMERO) + "/"
                + RigheRapportino.testo(documento, Preventivi.ANNO);
    }

    // ── Testata ──────────────────────────────────────────────────────────────────────────────────────────────

    private void mostraTestata() {
        ((TextView) findViewById(R.id.headerTitolo)).setText(titoloDocumento());
        TextView sotto = findViewById(R.id.headerSottotitolo);
        String titolo = RigheRapportino.testo(documento, Preventivi.TITOLO);
        sotto.setText(titolo);
        sotto.setVisibility(titolo.isEmpty() ? View.GONE : View.VISIBLE);

        TextView badge = findViewById(R.id.badge_stato);
        badge.setText(new Preventivi().getDescrizioneStatoPreventivo(stato(), this));
        GradientDrawable sfondo = new GradientDrawable();
        sfondo.setCornerRadius(40);
        sfondo.setColor(coloreStato(stato()));
        badge.setBackground(sfondo);
        badge.setVisibility(badge.getText().length() > 0 ? View.VISIBLE : View.GONE);

        LinearLayout campi = findViewById(R.id.campi);
        campi.removeAllViews();
        String cantiere = RigheRapportino.testo(documento, Cantieri.NOME);
        String data = documento.getAsLong(Preventivi.DATA) != null ? Utility.numberToData(documento.getAsLong(Preventivi.DATA)) : "";
        riga(campi, new String[] { getString(R.string.cliente), RigheRapportino.testo(documento, Anagrafica.RAGIONE_SOCIALE) },
                new String[] { getString(R.string.cantiere), cantiere });
        riga(campi, new String[] { getString(R.string.data), data },
                new String[] { getString(R.string.stato), badge.getText().toString() });
        riga(campi, new String[] { getString(R.string.descrizione), titolo }, null);
        String note = RigheRapportino.testo(documento, Preventivi.NOTE);
        if (!note.isEmpty()) riga(campi, new String[] { getString(R.string.note), note }, null);

        // pulsanti in fondo: cantiere solo con la struttura (Elettro/Design); conversione dei preventivi aperti o accettati
        boolean struttura = FunzionalitaApp.haModulo(this, FunzionalitaApp.STRUTTURA)
                && RigheRapportino.intero(documento, Preventivi.ID_CANTIERE) != 0;
        findViewById(R.id.button_cantiere).setVisibility(struttura ? View.VISIBLE : View.GONE);
        boolean converti = !ordine() && (Preventivi.STATO_APERTO.equals(stato()) || Preventivi.STATO_ACCETTATO.equals(stato()))
                && FunzionalitaApp.ha(this, FunzionalitaApp.codice(FunzionalitaApp.ORDINI, FunzionalitaApp.CREA));
        findViewById(R.id.button_crea_ordine).setVisibility(converti ? View.VISIBLE : View.GONE);
        BarraPulsanti.disponi((LinearLayout) findViewById(R.id.barra_pulsanti));
        titoli();
    }

    /** Una riga della testata: uno o due dati (etichetta piccola e valore), affiancati sui tablet. */
    private void riga(LinearLayout campi, String[] a, String[] b) {
        boolean affianca = getResources().getConfiguration().screenWidthDp >= 600;
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(affianca ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        for (String[] d : new String[][] { a, b }) {
            if (d == null) continue;
            View v = View.inflate(this, R.layout.form_campo_dato, null);
            ((TextView) v.findViewById(R.id.form_campo_etichetta)).setText(d[0]);
            ((TextView) v.findViewById(R.id.form_campo_valore)).setText(d[1].isEmpty() ? "—" : d[1]);
            LinearLayout.LayoutParams lp = affianca ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (affianca && r.getChildCount() > 0) lp.setMarginStart(Math.round(16 * getResources().getDisplayMetrics().density));
            r.addView(v, lp);
        }
        if (affianca && b == null) r.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        campi.addView(r);
    }

    private static int coloreStato(String stato) {
        switch (stato) {
            case Preventivi.STATO_APERTO: return 0xFF1565C0;
            case Preventivi.STATO_ACCETTATO: return 0xFF2E7D32;
            case Preventivi.STATO_RIFIUTATO: return 0xFFC62828;
            default: return 0xFF607D8B;
        }
    }

    // ── Sezioni a fisarmonica (come il rapportino) ───────────────────────────────────────────────────────────

    private void preparaSezioni() {
        View sezTestata = findViewById(R.id.sezione_testata), sezRighe = findViewById(R.id.sezione_righe);
        SezioneStandard.imposta(sezTestata, FaIcone.PREVENTIVO, ordine() ? "Ordine" : "Preventivo");
        SezioneStandard.imposta(sezRighe, FaIcone.ELENCO, "Righe");
        float d = getResources().getDisplayMetrics().density;
        ((ViewGroup.MarginLayoutParams) sezTestata.getLayoutParams()).topMargin = Math.round(2 * d);
        ((ViewGroup.MarginLayoutParams) sezRighe.getLayoutParams()).topMargin = Math.round(2 * d);
        // matita: dati del documento (numero, data, descrizione, stato, cantiere) nella form del template
        if (FunzionalitaApp.puoModificare(this, FunzionalitaApp.moduloPreventivo(RigheRapportino.testo(documento, Preventivi.TIPO)))) {
            SezioneStandard.azione(sezTestata, FaIcone.MODIFICA, "Modifica i dati del documento", v -> {
                Intent intent = new Intent(this, PreventiviDettaglioModActivity.class);
                intent.putExtra("ID", idPreventivo);
                apriFinestraModifica(intent, RICHIESTA_MODIFICA);
            });
        }
        frecciaTestata = freccia(sezTestata);
        frecciaRighe = freccia(sezRighe);
        sezTestata.setOnClickListener(v -> {
            testataAperta = !testataAperta;
            if (testataAperta && esclusiva()) righeAperte = false;
            fisarmonica();
        });
        sezRighe.setOnClickListener(v -> {
            righeAperte = !righeAperte;
            if (righeAperte && esclusiva()) testataAperta = false;
            fisarmonica();
        });
        mostraTestata();
        fisarmonica();
    }

    /** In orizzontale e sugli smartphone aprire una sezione chiude l'altra. */
    private boolean esclusiva() {
        android.content.res.Configuration c = getResources().getConfiguration();
        return c.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE || c.smallestScreenWidthDp < 600;
    }

    private TextView freccia(View sezione) {
        TextView t = new TextView(this);
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
        t.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.econ_testo_secondario));
        t.setPadding(Math.round(12 * getResources().getDisplayMetrics().density), 0, 0, 0);
        ((ViewGroup) sezione.findViewById(R.id.sezioneAzioni)).addView(t);
        android.util.TypedValue sfondo = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, sfondo, true);
        sezione.setBackgroundResource(sfondo.resourceId);
        return t;
    }

    private void fisarmonica() {
        if (!testataAperta && !righeAperte) righeAperte = true;
        findViewById(R.id.scroll_testata).setVisibility(testataAperta ? View.VISIBLE : View.GONE);
        findViewById(R.id.contenuto_righe).setVisibility(righeAperte ? View.VISIBLE : View.GONE);
        FaIcone.applica(frecciaTestata, testataAperta ? FaIcone.CHIUDI_SEZIONE : FaIcone.APRI, null);
        FaIcone.applica(frecciaRighe, righeAperte ? FaIcone.CHIUDI_SEZIONE : FaIcone.APRI, null);
        LinearLayout.LayoutParams lpR = (LinearLayout.LayoutParams) findViewById(R.id.card_righe).getLayoutParams();
        lpR.height = righeAperte ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
        lpR.weight = righeAperte ? 1f : 0f;
        findViewById(R.id.card_righe).setLayoutParams(lpR);
        titoli();
    }

    /** Le righe sono cambiate (inserite, modificate, eliminate): si aggiorna il conteggio. */
    public void righeCambiate() {
        if (documento != null) titoli();
    }

    /** Da chiusa la testata riassume cliente · cantiere · data; le righe dicono quante sono. */
    private void titoli() {
        if (frecciaTestata == null) return;
        String base = ordine() ? "Ordine" : "Preventivo";
        String riassunto = "";
        if (!testataAperta) {
            String cliente = RigheRapportino.testo(documento, Anagrafica.RAGIONE_SOCIALE);
            String cantiere = RigheRapportino.testo(documento, Cantieri.NOME);
            String data = documento.getAsLong(Preventivi.DATA) != null ? Utility.numberToData(documento.getAsLong(Preventivi.DATA)) : "";
            StringBuilder sb = new StringBuilder();
            for (String p : new String[] { cliente, cantiere, data }) {
                if (p.isEmpty()) continue;
                sb.append(sb.length() == 0 ? "" : " · ").append(p);
            }
            riassunto = sb.length() == 0 ? "" : " · " + sb;
        }
        ((TextView) findViewById(R.id.sezione_testata).findViewById(R.id.sezioneTitolo)).setText(base + riassunto);
        DbInterno db = new DbInterno(this);
        java.util.ArrayList<Object> n = db.eseguiSelect("select count(*) as n from " + PreventiviDettaglio.NOME_TABELLA + " where "
                + PreventiviDettaglio.ID_PREVENTIVO + "=" + idPreventivo, null);
        db.close();
        int quante = n.isEmpty() ? 0 : RigheRapportino.intero((ContentValues) n.get(0), "n");
        ((TextView) findViewById(R.id.sezione_righe).findViewById(R.id.sezioneTitolo)).setText("Righe" + (quante > 0 ? " · " + quante : ""));
    }

    // ── Azioni ───────────────────────────────────────────────────────────────────────────────────────────────

    /** Composizione elettrica nel cantiere, con questo documento scelto (come prima dall'elenco). */
    private void apriNelCantiere() {
        Intent intent = new Intent(this, CantiereSplitActivity.class);
        intent.putExtra(Cantieri.ID_CANTIERE, RigheRapportino.intero(documento, Preventivi.ID_CANTIERE));
        intent.putExtra(Preventivi.ID_PREVENTIVO, idPreventivo);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (com.ncfsistemi.econ.views.PopupRigaRapportino.risultatoFoto(requestCode, resultCode, data)) return;
        if (requestCode == RICHIESTA_XLS && resultCode == Activity.RESULT_OK && righe != null && data != null) {
            righe.importaDaFileXLS(data.getStringExtra("PATH"));
        }
        if (requestCode == RICHIESTA_MODIFICA && resultCode == Activity.RESULT_OK) setResult(RESULT_OK);
    }
}
