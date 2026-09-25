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
        int idUtente = nuova ? (isMateriale() ? 0 : contesto.utente.idUtenteDitta) : RigheRapportino.intero(riga, RapportiniDettaglio.ID_UTENTE_MANODOPERA);
        if (idUtente > 0 && trova(utenti, idUtente) < 0) { // utente della riga non piu' sceglibile: resta com'e'
            String nome = RegoleRapportino.nomeOperatore(db, idUtente);
            utenti.add(new CampoRicerca.Voce(idUtente, nome.isEmpty() ? "#" + idUtente : nome, new ContentValues()));
        }
        imposta(spinnerUtente, utenti, idUtente, null);
        spinnerUtente.setEnabled(utenti.size() > 1);

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
        vista.findViewById(R.id.button_tab_nuovo_articolo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ctx, R.string.nuovo_articolo_non_disponibile, Toast.LENGTH_LONG).show();
            }
        });

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
                for (Object o : dbc.eseguiSelect("select * from " + Listini.NOME_TABELLA + " where coalesce(" + Listini.ID + ",0)>0"
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
        if (RigheRapportino.intero(riga, RapportiniDettaglio.ID_LISTINO) > 0) {
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

    // ── Controllo e compilazione della riga ────────────────────────────────────────────────────────────────────

    /** Controlla i dati e li scrive nella riga; ritorna il messaggio d'errore, o null se va bene. */
    private String compila() {
        double quantita = numero(editQuantita.getText().toString());
        if (quantita <= 0) return ctx.getString(R.string.quantita_obbligatoria);
        int idUnita = idScelto(spinnerUnita);
        if (idUnita <= 0) return ctx.getString(R.string.unita_obbligatoria);
        int idUtente = idScelto(spinnerUtente);
        if (RapportiniDettaglioTipi.CATEGORIA_LAVORO.equals(categoria) && idUtente <= 0) return ctx.getString(R.string.utente_obbligatorio);
        int idListino = isMateriale() ? campoArticolo.getIdScelto() : RigheRapportino.intero(riga, RapportiniDettaglio.ID_LISTINO);
        if (isMateriale() && idListino <= 0) return ctx.getString(R.string.articolo_obbligatorio);

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
