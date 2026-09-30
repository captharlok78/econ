package pfa.app.econtab.views;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Listini;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.db.table.RapportiniDettaglioTipi;
import pfa.app.econtab.db.table.SettoriArticolo;
import pfa.app.econtab.db.table.UnitaMisura;
import pfa.app.econtab.db.table.Utenti;
import pfa.app.econtab.utils.RegoleRapportino;
import pfa.app.econtab.utils.RigheRapportino;
import pfa.app.econtab.utils.Utility;

/**
 * Finestra di una riga del rapportino (GESTIONE_RAPPORTINI.md §12.2), una per categoria del tipo:
 * <ul>
 * <li>LAVORO (Manodopera): tipo (se ce n'e' piu' d'uno), utente obbligatorio, quantita' in unita' di tempo (default ore);</li>
 * <li>VIAGGIO: tipo (andata, ritorno, generico), utente, partenza e arrivo che calcolano la durata, oppure la durata a mano
 * (default minuti);</li>
 * <li>MATERIALE (Articolo): articolo dal listino (filtro costruttore e linea, ricerca da 3 lettere), utente facoltativo,
 * quantita' in qualsiasi unita' (default pezzo).</li>
 * </ul>
 * Per tutte: note, riga dell'ordine (se il rapportino ha un ordine) e, per chi li vede, prezzo e "a costo". La riga non si
 * salva qui: torna alla maschera, che la tiene in memoria fino a Salva o Conferma.
 */
public class PopupRigaRapportino {

    /** Riceve la riga compilata (gia' completata con i dati collegati, vedi RigheRapportino.completa()). */
    public interface Esito {
        void rigaConfermata(ContentValues riga);
    }

    /** Quello che la finestra deve sapere del rapportino. */
    public static class Contesto {
        public RegoleRapportino.Utente utente;
        public boolean vedePrezzi;
        public int idCantiere;
        public int idOrdine;
        public long data;
    }

    private final Context ctx;
    private final String categoria;
    private final ContentValues riga;
    private final boolean nuova;
    private final Contesto contesto;
    private final Esito esito;
    private final View vista;

    private Spinner spinnerTipo, spinnerUtente, spinnerUnita, spinnerRigaOrdine, spinnerCostruttore, spinnerLinea;
    private EditText editOraInizio, editOraFine, editQuantita, editNote, editPrezzo;
    private CheckBox checkACosto;
    private CampoRicerca campoArticolo;
    /** true mentre la quantita' si scrive da codice (calcolo dagli orari): non deve svuotare gli orari. */
    private boolean calcoloInCorso = false;

    /** riga null = riga nuova della categoria indicata. */
    public PopupRigaRapportino(Context ctx, String categoria, ContentValues riga, Contesto contesto, Esito esito) {
        this.ctx = ctx;
        this.nuova = riga == null;
        this.riga = riga != null ? new ContentValues(riga) : new ContentValues();
        this.categoria = riga != null ? RigheRapportino.testo(riga, RigheRapportino.CATEGORIA) : categoria;
        this.contesto = contesto;
        this.esito = esito;
        this.vista = View.inflate(ctx, R.layout.dialog_riga_rapportino, null);
    }

    private boolean isMateriale() {
        return RapportiniDettaglioTipi.CATEGORIA_MATERIALE.equals(categoria);
    }

    private boolean isViaggio() {
        return RapportiniDettaglioTipi.CATEGORIA_VIAGGIO.equals(categoria);
    }

    public void apri() {
        DbInterno db = new DbInterno(ctx);
        List<CampoRicerca.Voce> tipi = voci(db, "select * from " + RapportiniDettaglioTipi.NOME_TABELLA + " where "
                + RapportiniDettaglioTipi.CATEGORIA + "='" + categoria + "' order by " + RapportiniDettaglioTipi.ORDINE,
                RapportiniDettaglioTipi.ID, RapportiniDettaglioTipi.DESCRIZIONE);
        List<CampoRicerca.Voce> unita = voci(db, "select * from " + UnitaMisura.NOME_TABELLA + " where coalesce(" + UnitaMisura.ID + ",0)>0"
                + (isMateriale() ? "" : " and coalesce(" + UnitaMisura.MINUTI_PER_UNITA + ",0)>0") + " order by " + UnitaMisura.UNITA_MISURA,
                UnitaMisura.ID, null);
        if (tipi.isEmpty() || unita.isEmpty()) {
            db.close();
            Toast.makeText(ctx, R.string.tipi_riga_non_scaricati, Toast.LENGTH_LONG).show();
            return;
        }
        for (CampoRicerca.Voce u : unita) { // "OR - Ore"
            String nome = RigheRapportino.testo(u.dati, UnitaMisura.NOME);
            u.dati.put("etichetta", RigheRapportino.testo(u.dati, UnitaMisura.UNITA_MISURA) + (nome.isEmpty() ? "" : " - " + nome));
        }

        spinnerTipo = (Spinner) vista.findViewById(R.id.spinner_tipo);
        spinnerUtente = (Spinner) vista.findViewById(R.id.spinner_utente);
        spinnerUnita = (Spinner) vista.findViewById(R.id.spinner_unita);
        spinnerRigaOrdine = (Spinner) vista.findViewById(R.id.spinner_riga_ordine);
        spinnerCostruttore = (Spinner) vista.findViewById(R.id.spinner_costruttore);
        spinnerLinea = (Spinner) vista.findViewById(R.id.spinner_linea);
        editOraInizio = (EditText) vista.findViewById(R.id.edit_ora_inizio);
        editOraFine = (EditText) vista.findViewById(R.id.edit_ora_fine);
        editQuantita = (EditText) vista.findViewById(R.id.edit_quantita);
        editNote = (EditText) vista.findViewById(R.id.edit_note_riga);
        editPrezzo = (EditText) vista.findViewById(R.id.edit_prezzo);
        checkACosto = (CheckBox) vista.findViewById(R.id.check_a_costo);
        campoArticolo = (CampoRicerca) vista.findViewById(R.id.campo_articolo);

        // tipo: tendina solo se ce n'e' piu' d'uno (il materiale ne ha uno solo, MA)
        imposta(spinnerTipo, tipi, RigheRapportino.intero(riga, RapportiniDettaglio.ID_TIPO), null);
        vista.findViewById(R.id.sezione_tipo).setVisibility(tipi.size() > 1 ? View.VISIBLE : View.GONE);

        // utente: chi puo' scegliere l'utente lo sceglie, gli altri sono fissi su se stessi; per il materiale e' facoltativo
        List<CampoRicerca.Voce> utenti = new ArrayList<CampoRicerca.Voce>();
        if (isMateriale()) utenti.add(new CampoRicerca.Voce(0, ctx.getString(R.string.nessuno), new ContentValues()));
        for (ContentValues ut : RegoleRapportino.operatoriSelezionabili(db, contesto.utente, contesto.idCantiere, contesto.data)) {
            utenti.add(new CampoRicerca.Voce(RigheRapportino.intero(ut, Utenti.ID_UTENTE_DITTA), RegoleRapportino.nome(ut)
                    + (RigheRapportino.intero(ut, RegoleRapportino.PIANIFICATO) == 1 ? " (" + ctx.getString(R.string.pianificato) + ")" : ""), ut));
        }
        // articolo: l'utente e' chi compila (nascosto, §14); una riga esistente tiene il suo
        int idUtente = nuova ? contesto.utente.idUtenteDitta : RigheRapportino.intero(riga, RapportiniDettaglio.ID_UTENTE_MANODOPERA);
        if (idUtente > 0 && trova(utenti, idUtente) < 0) { // utente della riga non piu' sceglibile: resta com'e'
            String nome = RegoleRapportino.nomeOperatore(db, idUtente);
            utenti.add(new CampoRicerca.Voce(idUtente, nome.isEmpty() ? "#" + idUtente : nome, new ContentValues()));
        }
        imposta(spinnerUtente, utenti, idUtente, null);
        spinnerUtente.setEnabled(utenti.size() > 1);
        if (isMateriale()) vista.findViewById(R.id.sezione_utente).setVisibility(View.GONE);

        // unita' di misura: di tempo per lavoro e viaggio (default ore / minuti), tutte per il materiale (default pezzo)
        String codiceDefault = isMateriale() ? UnitaMisura.CODICE_PEZZO : isViaggio() ? UnitaMisura.CODICE_MINUTI : UnitaMisura.CODICE_ORE;
        imposta(spinnerUnita, unita, RigheRapportino.intero(riga, RapportiniDettaglio.ID_UNITA_MISURA), codiceDefault);

        if (!nuova) {
            double q = RigheRapportino.decimale(riga, RapportiniDettaglio.QUANTITA);
            editQuantita.setText(q > 0 ? formatta(q) : "");
            editOraInizio.setText(RigheRapportino.testo(riga, RapportiniDettaglio.ORA_INIZIO));
            editOraFine.setText(RigheRapportino.testo(riga, RapportiniDettaglio.ORA_FINE));
            editNote.setText(RigheRapportino.testo(riga, RapportiniDettaglio.NOTE));
            Double prezzo = riga.getAsDouble(RapportiniDettaglio.PREZZO);
            editPrezzo.setText(prezzo != null && prezzo != 0 ? formatta(prezzo) : "");
            checkACosto.setChecked(RigheRapportino.intero(riga, RapportiniDettaglio.A_COSTO) == 1);
        }

        vista.findViewById(R.id.sezione_orari).setVisibility(isViaggio() ? View.VISIBLE : View.GONE);
        if (isViaggio()) preparaOrari();

        vista.findViewById(R.id.sezione_articolo).setVisibility(isMateriale() ? View.VISIBLE : View.GONE);
        if (isMateriale()) preparaArticolo(db);

        preparaRigaOrdine(db);

        vista.findViewById(R.id.sezione_prezzo).setVisibility(contesto.vedePrezzi ? View.VISIBLE : View.GONE);
        ((TextView) vista.findViewById(R.id.label_prezzo)).setText(isMateriale() ? R.string.costo_acquisto
                : isViaggio() ? R.string.costo_trasferta : R.string.costo_fisso);
        db.close();

        int titolo = isMateriale() ? R.string.articolo : isViaggio() ? R.string.tipo_viaggio : R.string.manodopera;
        final AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle(titolo)
                .setView(vista)
                .setPositiveButton(nuova ? R.string.inserisci : R.string.salva, null)
                .setNegativeButton(R.string.annulla, null)
                .create();
        // con la tastiera aperta la finestra si restringe e i pulsanti restano raggiungibili
        if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
        // il pulsante non chiude la finestra se i dati non sono validi
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String errore = compila();
                if (errore != null) {
                    Toast.makeText(ctx, errore, Toast.LENGTH_LONG).show();
                    return;
                }
                dialog.dismiss();
                esito.rigaConfermata(riga);
            }
        });
    }

    // ── Viaggio: partenza e arrivo → durata ─────────────────────────────────────────────────────────────────────

    private void preparaOrari() {
        View.OnClickListener sceltaOra = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText campo = (EditText) v;
                int[] hm = oraMinuti(campo.getText().toString());
                if (hm == null) {
                    java.util.Calendar c = java.util.Calendar.getInstance();
                    hm = new int[] { c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE) };
                }
                new TimePickerDialog(ctx, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int ore, int minuti) {
                        campo.setText(String.format(Locale.ITALY, "%02d:%02d", ore, minuti));
                        calcolaDurata();
                    }
                }, hm[0], hm[1], true).show();
            }
        };
        View.OnLongClickListener svuota = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ((EditText) v).setText("");
                return true;
            }
        };
        editOraInizio.setOnClickListener(sceltaOra);
        editOraFine.setOnClickListener(sceltaOra);
        editOraInizio.setOnLongClickListener(svuota);
        editOraFine.setOnLongClickListener(svuota);
        // durata scritta a mano: gli orari non valgono piu'
        editQuantita.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (calcoloInCorso || !editQuantita.hasFocus()) return;
                editOraInizio.setText("");
                editOraFine.setText("");
            }
        });
        spinnerUnita.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calcolaDurata();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /** Con partenza e arrivo la quantita' e' la durata nell'unita' scelta (arrivo prima della partenza = giorno dopo). */
    private void calcolaDurata() {
        int[] da = oraMinuti(editOraInizio.getText().toString());
        int[] a = oraMinuti(editOraFine.getText().toString());
        CampoRicerca.Voce u = (CampoRicerca.Voce) spinnerUnita.getSelectedItem();
        if (da == null || a == null || u == null) return;
        int minuti = (a[0] * 60 + a[1]) - (da[0] * 60 + da[1]);
        if (minuti < 0) minuti += 24 * 60;
        int perUnita = RigheRapportino.intero(u.dati, UnitaMisura.MINUTI_PER_UNITA);
        if (perUnita <= 0) return;
        calcoloInCorso = true;
        editQuantita.setText(formatta(Math.round(minuti * 1000.0 / perUnita) / 1000.0));
        calcoloInCorso = false;
    }

    private static int[] oraMinuti(String testo) {
        String[] p = testo.trim().split(":");
        if (p.length < 2) return null;
        try {
            return new int[] { Integer.parseInt(p[0]), Integer.parseInt(p[1]) };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── Articolo: listino filtrato per costruttore e linea ─────────────────────────────────────────────────────

    private void preparaArticolo(DbInterno db) {
        vista.findViewById(R.id.button_tab_listino).setOnClickListener(v -> mostraTab(false));
        vista.findViewById(R.id.button_tab_nuovo_articolo).setOnClickListener(v -> mostraTab(true));
        // la riga di un articolo gia' scelto si modifica dal listino: la tab del nuovo articolo e' per le righe nuove
        vista.findViewById(R.id.button_tab_nuovo_articolo).setVisibility(nuova ? View.VISIBLE : View.GONE);
        if (nuova) preparaNuovoArticolo(db);

        List<CampoRicerca.Voce> costruttori = new ArrayList<CampoRicerca.Voce>();
        costruttori.add(new CampoRicerca.Voce(0, ctx.getString(R.string.tutti), new ContentValues()));
        costruttori.addAll(voci(db, "select * from " + Costruttori.NOME_TABELLA + " order by " + Costruttori.RAGIONE_SOCIALE,
                Costruttori.ID_COSTRUTTORE, Costruttori.RAGIONE_SOCIALE));
        imposta(spinnerCostruttore, costruttori, 0, null);
        caricaLinee();
        spinnerCostruttore.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                caricaLinee();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        campoArticolo.setSorgente(new CampoRicerca.Sorgente() {
            @Override
            public List<CampoRicerca.Voce> cerca(String testo) {
                String like = "'%" + testo.replace("'", "''") + "%'";
                int costruttore = idScelto(spinnerCostruttore);
                int linea = idScelto(spinnerLinea);
                DbInterno dbc = new DbInterno(ctx);
                List<CampoRicerca.Voce> ris = new ArrayList<CampoRicerca.Voce>();
                // anche gli articoli della ditta non ancora inviati (id negativo); non quelli usati solo nella loro riga (§14)
                for (Object o : dbc.eseguiSelect("select * from " + Listini.NOME_TABELLA + " where coalesce(" + Listini.ID + ",0)<>0"
                        + " and coalesce(" + Listini.NEL_LISTINO + ",1)=1"
                        + (costruttore > 0 ? " and " + Listini.ID_COSTRUTTORE + "=" + costruttore : "")
                        + (linea > 0 ? " and " + Listini.ID_LINEA + "=" + linea : "")
                        + " and (" + Listini.CODICE_ARTICOLO + " like " + like + " or " + Listini.DESCRIZIONE + " like " + like + ")"
                        + " order by " + Listini.CODICE_ARTICOLO + " limit 50", null)) {
                    ContentValues l = (ContentValues) o;
                    ris.add(new CampoRicerca.Voce(RigheRapportino.intero(l, Listini.ID), RigheRapportino.testo(l, Listini.CODICE_ARTICOLO)
                            + " " + RigheRapportino.testo(l, Listini.DESCRIZIONE), l));
                }
                dbc.close();
                return ris;
            }
        });
        campoArticolo.setAscoltatore(new CampoRicerca.Ascoltatore() {
            @Override
            public void selezioneCambiata(CampoRicerca campo, CampoRicerca.Voce voce) {
                if (voce == null) return;
                proponiRigaOrdine(RigheRapportino.testo(voce.dati, Listini.CODICE_ARTICOLO));
                editQuantita.requestFocus(); // scelto l'articolo si passa alla quantita'
            }
        });
        if (RigheRapportino.intero(riga, RapportiniDettaglio.ID_LISTINO) != 0) {
            ContentValues l = new ContentValues();
            l.put(Listini.CODICE_ARTICOLO, RigheRapportino.testo(riga, RigheRapportino.CODICE_ARTICOLO));
            campoArticolo.seleziona(new CampoRicerca.Voce(RigheRapportino.intero(riga, RapportiniDettaglio.ID_LISTINO),
                    RigheRapportino.articolo(riga), l));
        }
    }

    private void caricaLinee() {
        int costruttore = idScelto(spinnerCostruttore);
        List<CampoRicerca.Voce> linee = new ArrayList<CampoRicerca.Voce>();
        linee.add(new CampoRicerca.Voce(0, ctx.getString(R.string.tutti), new ContentValues()));
        if (costruttore > 0) {
            DbInterno db = new DbInterno(ctx);
            linee.addAll(voci(db, "select * from " + Linee.NOME_TABELLA + " where " + Linee.ID_COSTRUTTORE + "=" + costruttore
                    + " order by " + Linee.NOME_LINEA, Linee.ID_LINEA, Linee.NOME_LINEA));
            db.close();
        }
        imposta(spinnerLinea, linee, 0, null);
        spinnerLinea.setEnabled(costruttore > 0);
    }

    // ── Riga dell'ordine ──────────────────────────────────────────────────────────────────────────────────────

    private List<CampoRicerca.Voce> righeOrdine = new ArrayList<CampoRicerca.Voce>();

    /** Righe dell'ordine del rapportino a cui imputare: manodopera (MD) per il tempo, materiale (MA/MP) per gli articoli. */
    private void preparaRigaOrdine(DbInterno db) {
        vista.findViewById(R.id.sezione_riga_ordine).setVisibility(contesto.idOrdine != 0 ? View.VISIBLE : View.GONE);
        righeOrdine.add(new CampoRicerca.Voce(0, ctx.getString(R.string.extra_ordine), new ContentValues()));
        if (contesto.idOrdine != 0) {
            String tipi = isMateriale() ? "'" + PreventiviDettaglio.MATERIALE + "','" + PreventiviDettaglio.MATERIALE_PREVENTIVO + "'"
                    : "'" + PreventiviDettaglio.MANOPERA + "'";
            for (Object o : db.eseguiSelect("select * from " + PreventiviDettaglio.NOME_TABELLA + " where " + PreventiviDettaglio.ID_PREVENTIVO
                    + "=" + contesto.idOrdine + " and " + PreventiviDettaglio.TIPO + " in (" + tipi + ") order by "
                    + PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO, null)) {
                ContentValues pd = (ContentValues) o;
                String codice = RigheRapportino.testo(pd, PreventiviDettaglio.CODICE_ARTICOLO);
                righeOrdine.add(new CampoRicerca.Voce(RigheRapportino.intero(pd, PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO),
                        (codice.isEmpty() ? "" : codice + " ") + RigheRapportino.testo(pd, PreventiviDettaglio.DESCRIZIONE), pd));
            }
        }
        imposta(spinnerRigaOrdine, righeOrdine, RigheRapportino.intero(riga, RapportiniDettaglio.ID_PREVENTIVO_DETTAGLIO), null);
    }

    /** Scelto un articolo, se l'ordine ha una riga con lo stesso codice la propone (se non ne era gia' scelta una). */
    private void proponiRigaOrdine(String codiceArticolo) {
        if (idScelto(spinnerRigaOrdine) != 0 || codiceArticolo.isEmpty()) return;
        for (int i = 0; i < righeOrdine.size(); i++) {
            if (codiceArticolo.equals(RigheRapportino.testo(righeOrdine.get(i).dati, PreventiviDettaglio.CODICE_ARTICOLO))) {
                spinnerRigaOrdine.setSelection(i);
                return;
            }
        }
    }

    /** Tab dell'articolo: Listino (catalogo) o Nuovo articolo (articolo libero della ditta, esempio §14). */
    private boolean tabNuovoArticolo = false;

    private void mostraTab(boolean nuovo) {
        tabNuovoArticolo = nuovo;
        vista.findViewById(R.id.pannello_listino).setVisibility(nuovo ? View.GONE : View.VISIBLE);
        vista.findViewById(R.id.pannello_nuovo_articolo).setVisibility(nuovo ? View.VISIBLE : View.GONE);
        android.widget.Button listino = vista.findViewById(R.id.button_tab_listino);
        android.widget.Button nuovoArt = vista.findViewById(R.id.button_tab_nuovo_articolo);
        int blu = android.graphics.Color.parseColor("#1565C0"), grigio = android.graphics.Color.parseColor("#E0E0E0");
        listino.setBackgroundTintList(android.content.res.ColorStateList.valueOf(nuovo ? grigio : blu));
        listino.setTextColor(nuovo ? blu : android.graphics.Color.WHITE);
        nuovoArt.setBackgroundTintList(android.content.res.ColorStateList.valueOf(nuovo ? blu : grigio));
        nuovoArt.setTextColor(nuovo ? android.graphics.Color.WHITE : blu);
        if (nuovo) vista.findViewById(R.id.edit_nuovo_descrizione).requestFocus();
    }

    // ── Nuovo articolo della ditta (GESTIONE_RAPPORTINI.md §14) ────────────────────────────────────────────────

    private Spinner spinnerNuovoSettore, spinnerNuovoCostruttore, spinnerNuovaLinea;
    /** Foto dell'articolo in JPEG base64 (null = nessuna). */
    private String fotoBase64 = null;
    public static final int RICHIESTA_FOTO = 78;
    /** Immagine scelta dal tablet (galleria, download): JPEG, PNG, WebP. */
    public static final int RICHIESTA_IMMAGINE = 79;
    /** Finestra che ha chiesto la foto alla fotocamera: l'activity le passa il risultato (risultatoFoto). */
    private static PopupRigaRapportino inAttesaFoto = null;

    private void preparaNuovoArticolo(DbInterno db) {
        spinnerNuovoSettore = vista.findViewById(R.id.spinner_nuovo_settore);
        spinnerNuovoCostruttore = vista.findViewById(R.id.spinner_nuovo_costruttore);
        spinnerNuovaLinea = vista.findViewById(R.id.spinner_nuovo_linea);
        vista.findViewById(R.id.sezione_nuovo_prezzo_vendita).setVisibility(contesto.vedePrezzi ? View.VISIBLE : View.INVISIBLE);

        // settore proposto: quello con piu' articoli nel listino della ditta (i suoi settori), altrimenti il primo
        ContentValues piuUsato = db.getRecord("select " + Listini.ID_SETTORE + " from " + Listini.NOME_TABELLA + " where coalesce("
                + Listini.ID_SETTORE + ",0)<>0 group by " + Listini.ID_SETTORE + " order by count(*) desc limit 1");
        caricaSettori(piuUsato != null ? RigheRapportino.intero(piuUsato, Listini.ID_SETTORE) : 0);
        // costruttore proposto: "Generico" (catalogo generale), con la sua linea "Generica"
        ContentValues generico = db.getRecord("select " + Costruttori.ID_COSTRUTTORE + " from " + Costruttori.NOME_TABELLA
                + " where " + Costruttori.SIGLA_METEL + "='" + Costruttori.SIGLA_GENERICO + "' and coalesce(" + Costruttori.ID_DITTA + ",0)=0");
        caricaCostruttori(generico != null ? RigheRapportino.intero(generico, Costruttori.ID_COSTRUTTORE) : 0);
        spinnerNuovoCostruttore.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                caricaLineeNuovo(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        caricaLineeNuovo(0);

        piu(R.id.button_piu_settore, "Nuovo settore", nome -> {
            DbInterno d = new DbInterno(ctx);
            int id = voceEsistente(d, SettoriArticolo.NOME_TABELLA, SettoriArticolo.ID, SettoriArticolo.NOME, nome, null);
            if (id == 0) {
                SettoriArticolo tab = new SettoriArticolo();
                ContentValues v = tab.getValoriLogInserimento(d);
                v.put(SettoriArticolo.NOME, nome);
                v.put(SettoriArticolo.ORDINE, 999);
                v.put(SettoriArticolo.ATTIVO, 1);
                tab.inserisciRecord(d, v);
                id = v.getAsInteger(SettoriArticolo.ID);
            }
            d.close();
            caricaSettori(id);
        });
        piu(R.id.button_piu_costruttore, "Nuovo costruttore", nome -> {
            DbInterno d = new DbInterno(ctx);
            int id = voceEsistente(d, Costruttori.NOME_TABELLA, Costruttori.ID_COSTRUTTORE, Costruttori.RAGIONE_SOCIALE, nome, null);
            if (id == 0) {
                Costruttori tab = new Costruttori();
                ContentValues v = tab.getValoriLogInserimento(d);
                v.put(Costruttori.RAGIONE_SOCIALE, nome);
                String sigla = nome.toUpperCase(Locale.ITALY).replaceAll("[^A-Z0-9]", "");
                v.put(Costruttori.SIGLA_METEL, (sigla + "XXX").substring(0, 3));
                tab.inserisciRecord(d, v);
                id = v.getAsInteger(Costruttori.ID_COSTRUTTORE);
                // ogni costruttore nuovo nasce con la sua linea "Generica"
                nuovaLinea(d, id, Linee.NOME_GENERICA);
            }
            d.close();
            caricaCostruttori(id);
            caricaLineeNuovo(0);
        });
        piu(R.id.button_piu_linea, "Nuova linea", nome -> {
            int costruttore = idScelto(spinnerNuovoCostruttore);
            if (costruttore == 0) {
                Toast.makeText(ctx, "Scegli prima il costruttore.", Toast.LENGTH_SHORT).show();
                return;
            }
            DbInterno d = new DbInterno(ctx);
            int id = voceEsistente(d, Linee.NOME_TABELLA, Linee.ID_LINEA, Linee.NOME_LINEA, nome, Linee.ID_COSTRUTTORE + "=" + costruttore);
            if (id == 0) id = nuovaLinea(d, costruttore, nome);
            d.close();
            caricaLineeNuovo(id);
        });

        vista.findViewById(R.id.button_nuovo_foto).setOnClickListener(v -> scattaFoto());
        vista.findViewById(R.id.button_scegli_immagine).setOnClickListener(v -> scegliImmagine());
        vista.findViewById(R.id.button_togli_foto).setOnClickListener(v -> {
            fotoBase64 = null;
            ((android.widget.ImageView) vista.findViewById(R.id.img_nuovo_foto)).setImageDrawable(null);
            v.setVisibility(View.GONE);
        });
    }

    private void caricaSettori(int idScelto) {
        DbInterno db = new DbInterno(ctx);
        List<CampoRicerca.Voce> settori = voci(db, "select * from " + SettoriArticolo.NOME_TABELLA + " where coalesce(" + SettoriArticolo.ATTIVO
                + ",1)=1 order by " + SettoriArticolo.ORDINE + ", " + SettoriArticolo.NOME, SettoriArticolo.ID, SettoriArticolo.NOME);
        db.close();
        imposta(spinnerNuovoSettore, settori, idScelto, null);
    }

    private void caricaCostruttori(int idScelto) {
        DbInterno db = new DbInterno(ctx);
        List<CampoRicerca.Voce> costruttori = voci(db, "select * from " + Costruttori.NOME_TABELLA + " order by " + Costruttori.RAGIONE_SOCIALE,
                Costruttori.ID_COSTRUTTORE, Costruttori.RAGIONE_SOCIALE);
        db.close();
        imposta(spinnerNuovoCostruttore, costruttori, idScelto, null);
    }

    /** Linee del costruttore scelto: quella indicata, altrimenti "Generica" se c'e', altrimenti la prima. */
    private void caricaLineeNuovo(int idScelto) {
        int costruttore = idScelto(spinnerNuovoCostruttore);
        DbInterno db = new DbInterno(ctx);
        List<CampoRicerca.Voce> linee = voci(db, "select * from " + Linee.NOME_TABELLA + " where " + Linee.ID_COSTRUTTORE + "=" + costruttore
                + " order by " + Linee.NOME_LINEA, Linee.ID_LINEA, Linee.NOME_LINEA);
        db.close();
        if (idScelto == 0) {
            for (CampoRicerca.Voce v : linee) {
                if (Linee.NOME_GENERICA.equalsIgnoreCase(v.testo)) idScelto = v.id;
            }
        }
        imposta(spinnerNuovaLinea, linee, idScelto, null);
    }

    private static int nuovaLinea(DbInterno db, int idCostruttore, String nome) {
        Linee tab = new Linee();
        ContentValues v = tab.getValoriLogInserimento(db);
        v.put(Linee.ID_COSTRUTTORE, idCostruttore);
        v.put(Linee.NOME_LINEA, nome);
        tab.inserisciRecord(db, v);
        return v.getAsInteger(Linee.ID_LINEA);
    }

    /** Id di una voce con lo stesso nome (senza badare a maiuscole), 0 se non c'e': non si creano doppioni. */
    private static int voceEsistente(DbInterno db, String tabella, String campoId, String campoNome, String nome, String filtro) {
        ContentValues v = db.getRecord("select " + campoId + " from " + tabella + " where lower(trim(" + campoNome + "))='"
                + nome.trim().toLowerCase(Locale.ITALY).replace("'", "''") + "'" + (filtro != null ? " and " + filtro : "") + " limit 1");
        return v != null ? RigheRapportino.intero(v, campoId) : 0;
    }

    /** Icona "+" accanto a una tendina: chiede il nome e passa la voce da creare (o da riusare) a chi la gestisce. */
    private void piu(int idIcona, String titolo, java.util.function.Consumer<String> crea) {
        TextView icona = vista.findViewById(idIcona);
        pfa.app.econtab.utils.FaIcone.applica(icona, pfa.app.econtab.utils.FaIcone.PIU, titolo);
        android.util.TypedValue sfondo = new android.util.TypedValue();
        ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, sfondo, true);
        icona.setBackgroundResource(sfondo.resourceId);
        icona.setOnClickListener(v -> {
            final EditText nome = new EditText(ctx);
            nome.setHint("Nome");
            nome.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            nome.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(50) });
            int p = Math.round(20 * ctx.getResources().getDisplayMetrics().density);
            android.widget.FrameLayout cornice = new android.widget.FrameLayout(ctx);
            cornice.setPadding(p, p / 2, p, 0);
            cornice.addView(nome);
            new AlertDialog.Builder(ctx).setTitle(titolo).setView(cornice)
                    .setPositiveButton(R.string.inserisci, (d, w) -> {
                        String n = nome.getText().toString().trim();
                        if (!n.isEmpty()) crea.accept(n);
                    })
                    .setNegativeButton(R.string.annulla, null)
                    .show();
        });
    }

    private void scattaFoto() {
        try {
            java.io.File file = fileFoto();
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".provider", file);
            android.content.Intent intent = new android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri);
            intent.addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION | android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            inAttesaFoto = this;
            ((android.app.Activity) ctx).startActivityForResult(intent, RICHIESTA_FOTO);
        } catch (Exception e) {
            Toast.makeText(ctx, "Fotocamera non disponibile: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void scegliImmagine() {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(android.content.Intent.EXTRA_MIME_TYPES, new String[] { "image/jpeg", "image/png", "image/webp" });
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            inAttesaFoto = this;
            ((android.app.Activity) ctx).startActivityForResult(android.content.Intent.createChooser(intent, "Scegli immagine"), RICHIESTA_IMMAGINE);
        } catch (Exception e) {
            Toast.makeText(ctx, "Nessuna app per scegliere le immagini: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private java.io.File fileFoto() {
        return new java.io.File(ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "foto_articolo.jpg");
    }

    /**
     * Da onActivityResult dell'activity: se era la foto (fotocamera) o l'immagine scelta per l'articolo la prepara (ridotta,
     * JPEG base64) e ritorna true.
     */
    public static boolean risultatoFoto(int requestCode, int resultCode, android.content.Intent data) {
        if ((requestCode != RICHIESTA_FOTO && requestCode != RICHIESTA_IMMAGINE) || inAttesaFoto == null) return false;
        PopupRigaRapportino p = inAttesaFoto;
        inAttesaFoto = null;
        if (resultCode != android.app.Activity.RESULT_OK) return true;
        if (requestCode == RICHIESTA_FOTO) {
            java.io.File file = p.fileFoto();
            p.caricaFoto(android.net.Uri.fromFile(file));
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        } else if (data != null && data.getData() != null) {
            p.caricaFoto(data.getData());
        }
        return true;
    }

    /**
     * Foto scattata o immagine scelta (JPEG, PNG, WebP: le legge Android), girata come indica l'EXIF e ridotta a 1024 px di
     * lato, in JPEG base64 (il server accetta anche PNG e WebP, ma cosi' le foto hanno tutte lo stesso formato).
     */
    private void caricaFoto(android.net.Uri uri) {
        android.content.ContentResolver cr = ctx.getContentResolver();
        try {
            android.graphics.BitmapFactory.Options dim = new android.graphics.BitmapFactory.Options();
            dim.inJustDecodeBounds = true;
            try (java.io.InputStream in = cr.openInputStream(uri)) {
                android.graphics.BitmapFactory.decodeStream(in, null, dim);
            }
            int scala = 1;
            while (Math.max(dim.outWidth, dim.outHeight) / (scala * 2) >= 1024) scala *= 2;
            android.graphics.BitmapFactory.Options opz = new android.graphics.BitmapFactory.Options();
            opz.inSampleSize = scala;
            android.graphics.Bitmap bmp;
            try (java.io.InputStream in = cr.openInputStream(uri)) {
                bmp = android.graphics.BitmapFactory.decodeStream(in, null, opz);
            }
            if (bmp == null) throw new java.io.IOException("immagine non leggibile (servono JPEG, PNG o WebP)");
            int orientamento = android.media.ExifInterface.ORIENTATION_NORMAL;
            try (java.io.InputStream in = cr.openInputStream(uri)) {
                orientamento = new android.media.ExifInterface(in)
                        .getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL);
            } catch (Exception ignored) {
                // PNG e WebP senza EXIF: restano come sono
            }
            int gradi = orientamento == android.media.ExifInterface.ORIENTATION_ROTATE_90 ? 90
                    : orientamento == android.media.ExifInterface.ORIENTATION_ROTATE_180 ? 180
                    : orientamento == android.media.ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
            float riduzione = Math.min(1f, 1024f / Math.max(bmp.getWidth(), bmp.getHeight()));
            android.graphics.Matrix m = new android.graphics.Matrix();
            m.postScale(riduzione, riduzione);
            m.postRotate(gradi);
            bmp = android.graphics.Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out);
            fotoBase64 = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP);
            ((android.widget.ImageView) vista.findViewById(R.id.img_nuovo_foto)).setImageBitmap(bmp);
            vista.findViewById(R.id.button_togli_foto).setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Toast.makeText(ctx, "Foto non caricata: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String testoCampo(int id) {
        return ((EditText) vista.findViewById(id)).getText().toString().trim();
    }

    /** Controlli del nuovo articolo prima di crearlo; null se va bene. */
    private String controllaNuovoArticolo() {
        if (testoCampo(R.id.edit_nuovo_descrizione).isEmpty()) return "Scrivi la descrizione dell'articolo.";
        if (idScelto(spinnerNuovoSettore) == 0) return "Scegli il settore (o aggiungilo con +).";
        if (idScelto(spinnerNuovoCostruttore) == 0) return "Scegli il costruttore (o aggiungilo con +).";
        if (idScelto(spinnerNuovaLinea) == 0) return "Scegli la linea (o aggiungila con +).";
        String codice = testoCampo(R.id.edit_nuovo_codice).toUpperCase(Locale.ITALY);
        if (!codice.isEmpty()) {
            DbInterno db = new DbInterno(ctx);
            ContentValues c = db.getRecord("select " + Listini.DESCRIZIONE + " from " + Listini.NOME_TABELLA + " where "
                    + Listini.CODICE_ARTICOLO + "='" + codice.replace("'", "''") + "'");
            db.close();
            if (c != null) return "Il codice " + codice + " c'è già nel listino (" + RigheRapportino.testo(c, Listini.DESCRIZIONE)
                    + "): sceglilo dalla tab Listino.";
        }
        return null;
    }

    /**
     * Crea l'articolo della ditta nel listino locale (id negativo, da inviare: in_server = 0) e ne ritorna l'id. Senza codice
     * gliene da' uno "D<ditta>-<data e ora>". Si crea subito, anche se il rapportino poi non si salva.
     */
    private int creaNuovoArticolo() {
        DbInterno db = new DbInterno(ctx);
        ContentValues minimo = db.getRecord("select min(coalesce(" + Listini.ID + ",0)) as m from " + Listini.NOME_TABELLA);
        int id = Math.min(minimo != null ? RigheRapportino.intero(minimo, "m") : 0, -1) - 1;
        String codice = testoCampo(R.id.edit_nuovo_codice).toUpperCase(Locale.ITALY);
        if (codice.isEmpty()) {
            codice = "D" + pfa.app.econtab.utils.Sessione.getDittaSelezionata() + "-"
                    + new java.text.SimpleDateFormat("yyMMddHHmmss", Locale.ITALY).format(new java.util.Date());
        }
        Listini tab = new Listini();
        ContentValues v = tab.getValoriLogInserimento(db);
        v.put(Listini.ID, id);
        v.put(Listini.CODICE_ARTICOLO, codice);
        v.put(Listini.DESCRIZIONE, testoCampo(R.id.edit_nuovo_descrizione));
        v.put(Listini.ID_SETTORE, idScelto(spinnerNuovoSettore));
        v.put(Listini.ID_COSTRUTTORE, idScelto(spinnerNuovoCostruttore));
        v.put(Listini.ID_LINEA, idScelto(spinnerNuovaLinea));
        v.put(Listini.BARCODE, testoCampo(R.id.edit_nuovo_barcode));
        v.put(Listini.ID_DITTA, pfa.app.econtab.utils.Sessione.getDittaSelezionata());
        v.put(Listini.NEL_LISTINO, ((CheckBox) vista.findViewById(R.id.check_nuovo_listino_ditta)).isChecked() ? 1 : 0);
        v.put(Listini.FOTO, fotoBase64 != null ? fotoBase64 : "");
        if (contesto.vedePrezzi) {
            v.put(Listini.PRZ_LISTINO, numero(testoCampo(R.id.edit_nuovo_prezzo_vendita)));
            v.put(Listini.PRZ_ULTIMO_ACQUISTO, numero(editPrezzo.getText().toString()));
        }
        v.put(Listini.IN_SERVER, 0);
        db.insert(Listini.NOME_TABELLA, v);
        db.close();
        return id;
    }

    // ── Controllo e compilazione della riga ────────────────────────────────────────────────────────────────────

    /** Controlla i dati e li scrive nella riga; ritorna il messaggio d'errore, o null se va bene. */
    private String compila() {
        double quantita = numero(editQuantita.getText().toString());
        if (quantita <= 0) return ctx.getString(R.string.quantita_obbligatoria);
        int idUnita = idScelto(spinnerUnita);
        if (idUnita <= 0) return ctx.getString(R.string.unita_obbligatoria);
        int idUtente = idScelto(spinnerUtente);
        if (RapportiniDettaglioTipi.CATEGORIA_LAVORO.equals(categoria) && idUtente <= 0) return ctx.getString(R.string.utente_obbligatorio);
        int idListino;
        if (isMateriale() && tabNuovoArticolo) {
            String errore = controllaNuovoArticolo();
            if (errore != null) return errore;
            idListino = creaNuovoArticolo();
        } else {
            idListino = isMateriale() ? campoArticolo.getIdScelto() : RigheRapportino.intero(riga, RapportiniDettaglio.ID_LISTINO);
        }
        if (isMateriale() && idListino == 0) return ctx.getString(R.string.articolo_obbligatorio);

        riga.put(RapportiniDettaglio.ID_TIPO, idScelto(spinnerTipo));
        riga.put(RapportiniDettaglio.ID_UTENTE_MANODOPERA, idUtente);
        riga.put(RapportiniDettaglio.QUANTITA, quantita);
        riga.put(RapportiniDettaglio.ID_UNITA_MISURA, idUnita);
        if (isViaggio()) {
            riga.put(RapportiniDettaglio.ORA_INIZIO, editOraInizio.getText().toString().trim());
            riga.put(RapportiniDettaglio.ORA_FINE, editOraFine.getText().toString().trim());
        }
        riga.put(RapportiniDettaglio.ID_LISTINO, isMateriale() ? idListino : 0);
        riga.put(RapportiniDettaglio.ID_PREVENTIVO_DETTAGLIO, contesto.idOrdine != 0 ? idScelto(spinnerRigaOrdine) : 0);
        riga.put(RapportiniDettaglio.NOTE, editNote.getText().toString().trim());
        if (contesto.vedePrezzi) {
            String prezzo = editPrezzo.getText().toString().trim();
            if (prezzo.isEmpty()) {
                riga.putNull(RapportiniDettaglio.PREZZO);
            } else {
                riga.put(RapportiniDettaglio.PREZZO, numero(prezzo));
            }
            riga.put(RapportiniDettaglio.A_COSTO, checkACosto.isChecked() ? 1 : 0);
        }
        DbInterno db = new DbInterno(ctx);
        RigheRapportino.completa(db, riga);
        db.close();
        return null;
    }

    // ── Utilita' ──────────────────────────────────────────────────────────────────────────────────────────────

    /** Voci da una query: id dal campo indicato, testo dal campo descrizione (o "etichetta" se descrizione null). */
    private static List<CampoRicerca.Voce> voci(DbInterno db, String sql, String campoId, String campoTesto) {
        List<CampoRicerca.Voce> ris = new ArrayList<CampoRicerca.Voce>();
        for (Object o : db.eseguiSelect(sql, null)) {
            ContentValues cv = (ContentValues) o;
            ris.add(new CampoRicerca.Voce(RigheRapportino.intero(cv, campoId), campoTesto != null ? RigheRapportino.testo(cv, campoTesto) : "", cv));
        }
        return ris;
    }

    /** Riempie lo spinner e seleziona l'id indicato, altrimenti la voce con quel codice di unita' di misura, altrimenti la prima. */
    private void imposta(Spinner spinner, final List<CampoRicerca.Voce> voci, int id, String codiceUnita) {
        ArrayAdapter<CampoRicerca.Voce> adapter = new ArrayAdapter<CampoRicerca.Voce>(ctx, android.R.layout.simple_spinner_item, voci) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                return etichetta(super.getView(position, convertView, parent), position);
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                return etichetta(super.getDropDownView(position, convertView, parent), position);
            }

            private View etichetta(View v, int position) {
                String e = voci.get(position).dati.getAsString("etichetta");
                if (e != null) ((TextView) v).setText(e);
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int pos = trova(voci, id);
        if (pos < 0 && codiceUnita != null) {
            for (int i = 0; i < voci.size(); i++) {
                if (codiceUnita.equals(RigheRapportino.testo(voci.get(i).dati, UnitaMisura.UNITA_MISURA))) pos = i;
            }
        }
        spinner.setSelection(Math.max(pos, 0));
    }

    private static int trova(List<CampoRicerca.Voce> voci, int id) {
        if (id == 0) return -1;
        for (int i = 0; i < voci.size(); i++) {
            if (voci.get(i).id == id) return i;
        }
        return -1;
    }

    private static int idScelto(Spinner spinner) {
        Object v = spinner.getSelectedItem();
        return v != null ? ((CampoRicerca.Voce) v).id : 0;
    }

    private static double numero(String testo) {
        try {
            return Double.parseDouble(testo.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String formatta(double valore) {
        if (valore == Math.rint(valore)) return String.valueOf((long) valore);
        return String.valueOf(valore);
    }
}
