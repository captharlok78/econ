package com.ncfsistemi.econ;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ncfsistemi.econ.adapters.ColonnaDettaglio;
import com.ncfsistemi.econ.adapters.DettaglioTabellaAdapter;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.ClientiIndirizzi;
import com.ncfsistemi.econ.db.table.Preventivi;
import com.ncfsistemi.econ.db.table.PreventiviDettaglio;
import com.ncfsistemi.econ.db.table.Rapportini;
import com.ncfsistemi.econ.db.table.RapportiniDettaglio;
import com.ncfsistemi.econ.db.table.RapportiniDettaglioTipi;
import com.ncfsistemi.econ.db.table.StatiDocumento;
import com.ncfsistemi.econ.db.table.Utenti;
import com.ncfsistemi.econ.utils.CantiereDaIndirizzo;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.RegoleRapportino;
import com.ncfsistemi.econ.utils.RigheRapportino;
import com.ncfsistemi.econ.utils.Sessione;
import com.ncfsistemi.econ.utils.Utility;
import com.ncfsistemi.econ.views.CampoRicerca;
import com.ncfsistemi.econ.views.EconCalendario;
import com.ncfsistemi.econ.views.PopupRigaRapportino;

/**
 * Rapportino, schema 26 (GESTIONE_RAPPORTINI.md §12): testata (operatore, data, cliente / cantiere / ordine con ricerca,
 * cantiere obbligatorio, STUDIO_ASSISTENZE_TEMPO.md §8; note), righe aggiunte con le finestre Manodopera, Viaggio e Articolo e mostrate in tabella. Le righe restano in
 * memoria fino a Salva (il rapportino resta nello stato attuale, per un rapportino nuovo quello iniziale, Bozza) o Conferma
 * (passa a Confermato). Salva, In verifica e Conferma non chiudono la maschera: la riaprono sul rapportino salvato, con
 * stato e permessi nuovi; si esce solo con Chiudi (o indietro), che con modifiche non salvate chiede conferma. Se lo stato non e' modificabile la maschera e' in sola lettura. I cantieri
 * sceglibili sono quelli su cui l'utente e' pianificato nella settimana della data (vedi RegoleRapportino).
 */
public class RapportinoDettaglioModActivity extends EconDettaglioActivity {

    /** Colonna solo in memoria: la riga e' nuova o cambiata e va scritta al salvataggio. */
    private static final String DA_SALVARE = "_da_salvare";

    private EconCalendario data;
    private CampoRicerca campoCliente, campoCantiere, campoOrdine;
    private EditText editNote;

    private RegoleRapportino.Utente utente;
    private boolean vedePrezzi;

    /** Testata letta dal DB (null per un rapportino nuovo). */
    private ContentValues testata = null;
    /** Stato attuale (null se gli stati non sono ancora scaricati). */
    private ContentValues stato = null;
    private boolean modificabile = true;
    /**
     * Chi puo' fare cosa (STUDIO_VERIFICA_RAPPORTINI.md §2.2): l'autore (o chi l'ha inserito) e l'amministratore cambiano
     * testata e stato; gli operatori coinvolti, In verifica, aggiungono e correggono solo le proprie righe.
     */
    private boolean proprietario = true;
    private boolean testataModificabile = true;
    private boolean aggiungeRighe = true;
    /** Rapportino esistente e ruolo senza RAPPORTINI.MODIFICA: sola lettura qualunque sia lo stato. */
    private boolean senzaLicenzaModifica = false;

    private final ArrayList<Object> righe = new ArrayList<Object>();
    /** Id delle righe gia' salvate e tolte dalla tabella: si cancellano al salvataggio. */
    private final List<Integer> righeEliminate = new ArrayList<Integer>();
    private RigheAdapter adapter;
    private boolean modificato = false;

    /** Cantieri ammessi per la data (id_cantiere, nome, id_anagrafica, ragione_sociale). */
    private ArrayList<Object> cantieriAmmessi = new ArrayList<Object>();
    private String dataCaricata = null;
    /** Cantiere creato da qui per un cliente che non ne aveva (resta sceglibile anche se non e' pianificato). */
    private int idCantiereCreato = 0;
    /** Stato a cui porta il salvataggio in corso (Salva: lo stesso o l'iniziale; Conferma: Confermato). */
    private int idStatoDestino = 0;

    /** Firma fatta in questa sessione e non ancora salvata (null = invariata), GESTIONE_RAPPORTINI.md §13. */
    private String firmaNuova = null;
    private String firmaNuovaNome = null;
    private long firmaNuovaData = 0;
    private static final int RICHIESTA_FIRMA = 77;
    private static final int RICHIESTA_NUOVO_CLIENTE = 78;
    /** Invio in sede del rapportino appena confermato, per avere subito il numero (SincronizzazioneConfermaActivity). */
    private static final int RICHIESTA_INVIO_CONFERMA = 79;

    /** Rapportino nuovo con questa data (yyyyMMddHHmmss), es. dal calendario personale. */
    public static final String EXTRA_DATA = "DATA_RAPPORTINO";
    /** Maschera riaperta dopo un salvataggio: messaggio da mostrare; chi l'ha aperta deve aggiornarsi (RESULT_OK). */
    private static final String EXTRA_SALVATO = "SALVATO";

    /** In questa maschera qualcosa e' stato salvato (rapportino, firma, richiesta di invio): alla chiusura RESULT_OK. */
    private boolean salvato = false;
    /** Id del rapportino appena salvato (anche quello nuovo), per riaprirlo. */
    private int idSalvato = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_rapportino_dettaglio_mod);
        com.ncfsistemi.econ.utils.SezioneStandard.imposta(findViewById(R.id.sezione_testata), com.ncfsistemi.econ.utils.FaIcone.RAPPORTINO, "Rapportino");
        data = (EconCalendario) findViewById(R.id.edit_data);
        campoCliente = (CampoRicerca) findViewById(R.id.campo_cliente);
        campoCantiere = (CampoRicerca) findViewById(R.id.campo_cantiere);
        campoOrdine = (CampoRicerca) findViewById(R.id.campo_ordine);
        campoOrdine.setMinimoLettere(0);
        editNote = (EditText) findViewById(R.id.editText_note);

        DbInterno db = new DbInterno(this);
        utente = RegoleRapportino.utenteCorrente(db, this);
        vedePrezzi = RegoleRapportino.vedePrezzi(db, utente);
        db.close();

        adapter = new RigheAdapter();
        adapter.collegaTestata((ViewGroup) findViewById(R.id.testata_dettaglio));
        ((ListView) findViewById(R.id.lista_dett)).setAdapter(adapter);

        super.onCreate(savedInstanceState); // in modifica chiama inizializzaModifica()

        db = new DbInterno(this);
        int idCliente = 0, idCantiere = 0, idOrdine = 0;
        if (testata != null) {
            idCliente = RigheRapportino.intero(testata, Rapportini.ID_CLIENTE);
            idCantiere = RigheRapportino.intero(testata, Rapportini.ID_CANTIERE);
            idOrdine = RigheRapportino.intero(testata, Rapportini.ID_ORDINE);
        } else {
            // data scelta da chi apre il nuovo rapportino (calendario personale), altrimenti quella proposta
            long dataProposta = getIntent().getLongExtra(EXTRA_DATA, 0);
            if (dataProposta > 0) data.setValue(String.valueOf(dataProposta));
            stato = RegoleRapportino.statoIniziale(db);
            impostaOperatore(utente.idUtenteDitta);
            // nuovo rapportino pulito (21/10): cliente, cantiere e ordine si scelgono, niente ripresi dall'ultimo
        }
        // stato modificabile e, per un rapportino esistente, la funzionalita' MODIFICA del pacchetto per il ruolo
        senzaLicenzaModifica = testata != null
                && !com.ncfsistemi.econ.utils.FunzionalitaApp.puoModificare(this, com.ncfsistemi.econ.utils.FunzionalitaApp.RAPPORTINI);
        modificabile = RegoleRapportino.isModificabile(stato) && !senzaLicenzaModifica
                && !RegoleRapportino.bloccatoInApp(stato);
        proprietario = RegoleRapportino.isProprietario(testata, utente);
        testataModificabile = modificabile && (utente.amministratore || proprietario);
        aggiungeRighe = modificabile && (utente.amministratore || proprietario
                || (RegoleRapportino.isRigheOperatori(stato) && RegoleRapportino.coinvolto(db, utente, idCantiere, dataRapportino(), righe)));
        caricaClienti(db);
        db.close();

        caricaCantieriAmmessi();
        campoCliente.selezionaId(idCliente);
        aggiornaElencoCantieri();
        campoCantiere.selezionaId(idCantiere);
        aggiornaElencoOrdini(idOrdine);
        campoOrdine.selezionaId(idOrdine);
        if (testata != null) {
            // riferimenti che non sono sul tablet (es. cantiere disattivato, fuori dal perimetro dell'app): si mostrano come
            // tali e il salvataggio li lascia com'erano, invece di azzerarli anche sul server
            tieniSeAssente(campoCliente, idCliente, "Cliente");
            tieniSeAssente(campoCantiere, idCantiere, "Cantiere");
            tieniSeAssente(campoOrdine, idOrdine, "Ordine");
        }

        impostaAscoltatori();
        applicaStato();
        aggiornaInviaMail();
        mostraFirma();
        disponiPulsanti();
        disponiSoggetti();
        com.ncfsistemi.econ.views.BarraPulsanti.inUnaRiga((android.widget.LinearLayout) findViewById(R.id.pulsanti_righe));
        preparaFisarmonica();

        String messaggio = getIntent().getStringExtra(EXTRA_SALVATO);
        if (messaggio != null) {
            segnaSalvato();
            Toast.makeText(this, messaggio, Toast.LENGTH_SHORT).show();
        }
    }

    private void segnaSalvato() {
        salvato = true;
        setResult(RESULT_OK);
    }

    /** Pulsanti in fondo su una o piu' righe (smartphone in verticale: 2-3 righe), di nuovo a ogni rotazione. */
    private void disponiPulsanti() {
        com.ncfsistemi.econ.views.BarraPulsanti.disponi((android.widget.LinearLayout) findViewById(R.id.barra_pulsanti));
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration nuova) {
        super.onConfigurationChanged(nuova);
        disponiPulsanti();
        disponiSoggetti();
        fisarmonica();
    }

    /** Se il riferimento salvato non e' tra le voci del tablet, lo seleziona con un testo che lo spiega (l'id resta). */
    private static void tieniSeAssente(CampoRicerca campo, int id, String cosa) {
        if (id != 0 && campo.getIdScelto() == 0) {
            campo.seleziona(new CampoRicerca.Voce(id, cosa + " n. " + id + " (non sul tablet)", new ContentValues()));
        }
    }

    @Override
    protected String getTitoloDettaglio() {
        if (getModalita() == INSERIMENTO) return getString(R.string.nuovo_rapportino);
        // "Rapportino 12/2026" (numero dato dal server alla conferma, NUMERAZIONE_DOCUMENTI.md)
        DbInterno db = new DbInterno(this);
        ContentValues r = db.getRecord("select " + Rapportini.NUMERO + ", " + Rapportini.ANNO + " from " + Rapportini.NOME_TABELLA
                + " where " + Rapportini.ID + "=" + getIDModifica());
        db.close();
        String numero = Rapportini.numeroDocumento(r);
        return getString(R.string.rapportino) + (numero.isEmpty() ? "" : " " + numero);
    }

    @Override
    protected void inizializzaModifica() {
        super.inizializzaModifica();
        DbInterno db = new DbInterno(this);
        ContentValues where = new ContentValues();
        where.put(Rapportini.ID, getIDModifica());
        testata = db.getRecord(new Rapportini(), where);
        if (testata != null) {
            data.setValue("" + testata.getAsLong(Rapportini.DATA_RAPPORTINO));
            editNote.setText(testata.getAsString(Rapportini.NOTE));
            impostaOperatore(RigheRapportino.intero(testata, Rapportini.ID_UTENTE_DITTA));
            stato = RegoleRapportino.stato(db, RigheRapportino.intero(testata, Rapportini.ID_STATO));
            righe.addAll(RigheRapportino.righe(db, getIDModifica()));
        }
        db.close();
        adapter.notifyDataSetChanged();
    }

    /** Nome dell'autore (l'utente collegato, o l'autore del rapportino in modifica). */
    private void impostaOperatore(int idUtenteDitta) {
        String nome = idUtenteDitta == utente.idUtenteDitta ? Sessione.getNomeOperatore() : null;
        if (nome == null || nome.trim().isEmpty()) {
            DbInterno db = new DbInterno(this);
            nome = RegoleRapportino.nomeOperatore(db, idUtenteDitta);
            db.close();
        }
        ((TextView) findViewById(R.id.text_operatore)).setText(nome.trim());
    }

    // ── Stato ────────────────────────────────────────────────────────────────────────────────────────────────

    /** Badge dello stato; in sola lettura se lo stato non e' modificabile; Conferma solo se il passaggio e' ammesso. */
    private void applicaStato() {
        TextView badge = (TextView) findViewById(R.id.badge_stato);
        if (stato != null) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(RigheRapportino.testo(stato, StatiDocumento.NOME));
            GradientDrawable sfondo = new GradientDrawable();
            sfondo.setCornerRadius(40);
            sfondo.setColor(RegoleRapportino.coloreStato(RigheRapportino.testo(stato, StatiDocumento.COLORE)));
            badge.setBackground(sfondo);
        } else {
            badge.setVisibility(View.GONE);
        }

        data.setEnabled(testataModificabile);
        campoCliente.setEnabled(testataModificabile);
        findViewById(R.id.button_nuovo_cliente).setVisibility(testataModificabile ? View.VISIBLE : View.GONE);
        campoCantiere.setEnabled(testataModificabile);
        campoOrdine.setEnabled(testataModificabile);
        editNote.setEnabled(testataModificabile);
        aggiornaPulsantiRighe();
        findViewById(R.id.button_salva).setVisibility(testataModificabile || aggiungeRighe ? View.VISIBLE : View.GONE);

        // Conferma e In verifica: solo l'autore o l'amministratore, se il passaggio e' ammesso
        DbInterno db = new DbInterno(this);
        int idAttuale = stato != null ? RigheRapportino.intero(stato, StatiDocumento.ID) : 0;
        ContentValues confermato = RegoleRapportino.statoPerCodice(db, StatiDocumento.CODICE_CONFERMATO);
        boolean confermabile = testataModificabile && confermato != null && idAttuale != RigheRapportino.intero(confermato, StatiDocumento.ID)
                && RegoleRapportino.passaggioAmmesso(db, idAttuale, RigheRapportino.intero(confermato, StatiDocumento.ID), utente);
        ContentValues verifica = RegoleRapportino.statoPerCodice(db, StatiDocumento.CODICE_VERIFICA);
        boolean verificabile = testataModificabile && verifica != null && idAttuale != RigheRapportino.intero(verifica, StatiDocumento.ID)
                && RegoleRapportino.passaggioAmmesso(db, idAttuale, RigheRapportino.intero(verifica, StatiDocumento.ID), utente);
        // riga informativa: In verifica, chi ha confermato
        String info = "";
        if (RegoleRapportino.isRigheOperatori(stato)) {
            info = proprietario || utente.amministratore
                    ? "In verifica: gli operatori coinvolti aggiungono le proprie ore, poi conferma."
                    : (aggiungeRighe ? "In verifica: aggiungi e correggi le tue ore." : "In verifica: non sei tra gli operatori coinvolti.");
        }
        if (RegoleRapportino.bloccatoInApp(stato)) {
            info += (info.isEmpty() ? "" : "\n") + "Confermato: dall'app non si modifica più (si può riaprire dal portale).";
        }
        if (testata != null && RigheRapportino.intero(testata, Rapportini.ID_CONFERMATO_DA) > 0) {
            String chi = RegoleRapportino.nomeOperatore(db, RigheRapportino.intero(testata, Rapportini.ID_CONFERMATO_DA));
            long quando = testata.getAsLong(Rapportini.DATA_CONFERMA) != null ? testata.getAsLong(Rapportini.DATA_CONFERMA) : 0;
            info += (info.isEmpty() ? "" : "\n") + "Confermato da " + (chi.isEmpty() ? "—" : chi) + (quando > 0 ? " il " + dataOra(quando) : "");
        }
        String mail = testoMail();
        if (!mail.isEmpty()) info += (info.isEmpty() ? "" : "\n") + mail;
        db.close();
        TextView testoInfo = (TextView) findViewById(R.id.text_info_stato);
        testoInfo.setText(info);
        testoInfo.setVisibility(info.isEmpty() ? View.GONE : View.VISIBLE);
        findViewById(R.id.button_conferma).setVisibility(confermabile ? View.VISIBLE : View.GONE);
        findViewById(R.id.button_verifica).setVisibility(verificabile ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
        disponiPulsanti();
    }

    /** gg/mm/aaaa hh:mm da yyyyMMddHHmmss */
    private static String dataOra(long v) {
        String s = String.valueOf(v);
        return s.length() < 12 ? "" : s.substring(6, 8) + "/" + s.substring(4, 6) + "/" + s.substring(0, 4) + " " + s.substring(8, 10) + ":" + s.substring(10, 12);
    }

    // ── Cliente, cantiere, ordine ─────────────────────────────────────────────────────────────────────────────

    private long dataRapportino() {
        try {
            return Long.parseLong(data.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void caricaClienti(DbInterno db) {
        List<CampoRicerca.Voce> voci = new ArrayList<CampoRicerca.Voce>();
        for (Object o : db.eseguiSelect("select * from " + Anagrafica.NOME_TABELLA + " order by " + Anagrafica.RAGIONE_SOCIALE, null)) {
            ContentValues c = (ContentValues) o;
            voci.add(new CampoRicerca.Voce(RigheRapportino.intero(c, Anagrafica.ID_ANAGRAFICA), RigheRapportino.testo(c, Anagrafica.RAGIONE_SOCIALE), c));
        }
        campoCliente.setVoci(voci);
    }

    /** Avviso (non blocco) per un rapportino prima dell'apertura del cantiere (STUDIO_DATE_CANTIERI.md D3). */
    private void avvisoApertura(ContentValues cantiere) {
        Long apertura = cantiere.getAsLong(Cantieri.DATA_APERTURA);
        long giorno = Cantieri.giorno(dataRapportino());
        if (apertura == null || apertura <= 0 || giorno == 0 || giorno >= Cantieri.giorno(apertura)) return;
        String s = String.valueOf(Cantieri.giorno(apertura));
        Toast.makeText(this, "Il cantiere apre il " + s.substring(6, 8) + "/" + s.substring(4, 6) + "/" + s.substring(0, 4)
                + ": il rapportino e' prima dell'apertura.", Toast.LENGTH_LONG).show();
    }

    /** Cantieri ammessi per la data (quello del rapportino salvato resta sempre sceglibile). */
    private void caricaCantieriAmmessi() {
        dataCaricata = data.getValue();
        DbInterno db = new DbInterno(this);
        cantieriAmmessi = RegoleRapportino.cantieriDisponibili(db, utente, dataRapportino(),
                testata != null ? RigheRapportino.intero(testata, Rapportini.ID_CANTIERE) : idCantiereCreato);
        db.close();
        if (cantieriAmmessi.isEmpty() && modificabile) {
            Toast.makeText(this, getString(!utente.amministratore && utente.idUtenteDitta == 0
                    ? R.string.operatori_non_scaricati : R.string.nessun_cantiere_rapportino), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Cantieri ammessi del cliente scelto (tutti se nessun cliente). Con il cliente scelto l'elenco e' corto: si apre
     * toccando il campo, senza scrivere; senza cliente si cerca scrivendo almeno 3 lettere.
     */
    private void aggiornaElencoCantieri() {
        int cliente = campoCliente.getIdScelto();
        List<CampoRicerca.Voce> voci = new ArrayList<CampoRicerca.Voce>();
        for (Object o : cantieriAmmessi) {
            ContentValues c = (ContentValues) o;
            // i clienti creati sul dispositivo hanno l'id negativo: il filtro vale per ogni id diverso da 0
            if (cliente != 0 && RigheRapportino.intero(c, Cantieri.ID_ANAGRAFICA) != cliente) continue;
            String rs = RigheRapportino.testo(c, Anagrafica.RAGIONE_SOCIALE);
            voci.add(new CampoRicerca.Voce(RigheRapportino.intero(c, Cantieri.ID_CANTIERE),
                    RigheRapportino.testo(c, Cantieri.NOME) + (cliente == 0 && !rs.isEmpty() ? " - " + rs : ""), c));
        }
        campoCantiere.setVoci(voci);
        campoCantiere.setMinimoLettere(cliente != 0 ? 0 : 3);
        campoCantiere.setHint(cliente == 0 ? getString(R.string.cerca_3_lettere)
                : voci.isEmpty() ? "Nessun cantiere per questo cliente" : "Tocca per scegliere (" + voci.size() + ")");
    }

    /**
     * Cliente scelto (o appena creato col "+") senza cantieri: si propone di creare il cantiere al suo indirizzo predefinito (il
     * principale, altrimenti il primo attivo) e, alla conferma, lo si crea e lo si sceglie subito. Niente domanda se il
     * cliente ha gia' cantieri (anche se non sceglibili per la data o la pianificazione), se non ha indirizzi o se manca
     * CANTIERI.CREA.
     */
    private void proponiCantiere(CampoRicerca.Voce cliente) {
        if (!testataModificabile) return;
        // mai in silenzio: se il cantiere non si puo' creare si dice perche'
        if (!com.ncfsistemi.econ.utils.FunzionalitaApp.ha(this, com.ncfsistemi.econ.utils.FunzionalitaApp.CANTIERI_CREA)) {
            Utility.mostraDialog("Nessun cantiere", cliente.testo + " non ha cantieri e la tua licenza non consente di crearne"
                    + " (funzionalita' Cantieri › Crea). Chiedi all'amministratore della ditta.", this, "OK");
            return;
        }
        DbInterno db = new DbInterno(this);
        ArrayList<Object> esistenti = db.eseguiSelect("select " + Cantieri.NOME + " from " + Cantieri.NOME_TABELLA + " where "
                + Cantieri.ID_ANAGRAFICA + "=" + cliente.id + " and coalesce(" + AbstractTable.ATTIVO + ", 1) = 1 limit 1", null);
        boolean haCantieri = !esistenti.isEmpty();
        ArrayList<Object> indirizzi = haCantieri ? new ArrayList<Object>() : db.eseguiSelect("select * from " + ClientiIndirizzi.NOME_TABELLA
                + " where " + ClientiIndirizzi.ID_ANAGRAFICA + "=" + cliente.id + " and coalesce(" + AbstractTable.ATTIVO + ", 1) = 1"
                + " order by coalesce(" + ClientiIndirizzi.PRINCIPALE + ", 0) desc, " + ClientiIndirizzi.ID + " limit 1", null);
        db.close();
        if (haCantieri) {
            Utility.mostraDialog("Cantiere non sceglibile", cliente.testo + " ha gia' il cantiere \""
                    + RigheRapportino.testo((ContentValues) esistenti.get(0), Cantieri.NOME) + "\", ma non e' sceglibile per il "
                    + data.getTesto().getText() + ": e' chiuso a quella data" + (utente.amministratore ? "" : " o non sei pianificato li'")
                    + ". Controlla le date del cantiere.", this, "OK");
            return;
        }
        // senza indirizzi (cliente con l'indirizzo solo sulla scheda): la sede principale nasce da quello, alla conferma
        ContentValues indirizzo = indirizzi.isEmpty() ? sedeDalCliente(cliente.dati) : (ContentValues) indirizzi.get(0);
        final boolean sedeDaCreare = indirizzi.isEmpty();
        String dove = indirizzo == null ? "Creo il cantiere \"" + cliente.testo + "\" (senza indirizzo, si completa dopo)"
                : "Creo il cantiere all'indirizzo " + ClientiIndirizzi.completo(indirizzo);
        Utility.mostraConfermaDialog("Nessun cantiere", cliente.testo + " non ha cantieri.\n\n" + dove
                        + " e lo scelgo per questo rapportino?", this, "Crea cantiere",
                getString(R.string.annulla), (dialog, quale) -> {
                    if (quale != DialogInterface.BUTTON_POSITIVE) return;
                    DbInterno dbc = new DbInterno(this);
                    CantiereDaIndirizzo.Esito esito;
                    if (indirizzo == null) {
                        esito = CantiereDaIndirizzo.creaCantiereSenzaIndirizzo(dbc, cliente.id);
                    } else {
                        int idIndirizzo = sedeDaCreare ? creaSede(dbc, indirizzo) : RigheRapportino.intero(indirizzo, ClientiIndirizzi.ID);
                        esito = CantiereDaIndirizzo.creaCantiere(dbc, idIndirizzo);
                    }
                    dbc.close();
                    Toast.makeText(this, esito.messaggio, Toast.LENGTH_LONG).show();
                    if (esito.idCantiere == 0) return;
                    idCantiereCreato = esito.idCantiere;
                    caricaCantieriAmmessi();
                    aggiornaElencoCantieri();
                    campoCantiere.selezionaId(idCantiereCreato);
                    cantiereScelto(campoCantiere.getScelta());
                });
    }

    /** Sede principale (non ancora scritta) dall'indirizzo sulla scheda del cliente; null se il cliente non ha indirizzo. */
    private static ContentValues sedeDalCliente(ContentValues cli) {
        String via = RigheRapportino.testo(cli, Anagrafica.INDIRIZZO), citta = RigheRapportino.testo(cli, Anagrafica.CITTA);
        if ((via + citta).trim().isEmpty()) return null;
        ContentValues sede = new ContentValues();
        sede.put(ClientiIndirizzi.ID_ANAGRAFICA, RigheRapportino.intero(cli, Anagrafica.ID_ANAGRAFICA));
        sede.put(ClientiIndirizzi.TIPO, ClientiIndirizzi.TIPI[0][0]);
        sede.put(ClientiIndirizzi.INDIRIZZO, via);
        sede.put(ClientiIndirizzi.CIVICO, "");
        sede.put(ClientiIndirizzi.CAP, RigheRapportino.testo(cli, Anagrafica.CAP));
        sede.put(ClientiIndirizzi.CITTA, citta);
        sede.put(ClientiIndirizzi.PROVINCIA, RigheRapportino.testo(cli, Anagrafica.PROVINCIA));
        sede.put(ClientiIndirizzi.NAZIONE, "IT");
        sede.put(ClientiIndirizzi.PRINCIPALE, 1);
        sede.put(AbstractTable.ATTIVO, 1);
        return sede;
    }

    /** Scrive la sede principale proposta (va al server con il cantiere); il suo id. */
    private static int creaSede(DbInterno db, ContentValues sede) {
        ClientiIndirizzi tab = new ClientiIndirizzi();
        ContentValues val = tab.getValoriLogInserimento(db);
        val.putAll(sede);
        tab.inserisciRecord(db, val);
        return RigheRapportino.intero(val, ClientiIndirizzi.ID);
    }

    /** "+" accanto al cliente: inserimento di un cliente nuovo (se la licenza lo consente); al ritorno e' gia' scelto. */
    public void nuovoCliente(View v) {
        apriFinestraInserimento(new Intent(this, ClientiDettaglioModActivity.class), RICHIESTA_NUOVO_CLIENTE, new Anagrafica());
    }

    /** Apre l'elenco del campo (dopo la scelta del cliente o del cantiere), se c'e' qualcosa da scegliere. */
    private void apriElenco(CampoRicerca campo) {
        if (!modificabile || campo.getScelta() != null || campo.getVoci().isEmpty()) return;
        campo.post(() -> {
            if (isFinishing() || isDestroyed() || !campo.isShown()) return;
            campo.requestFocus();
            campo.mostraElenco();
        });
    }

    /** Ordini aperti del cantiere scelto, o di tutti i cantieri ammessi (del cliente scelto); il salvato resta sceglibile. */
    private void aggiornaElencoOrdini(int idOrdineSempreIncluso) {
        Set<Integer> cantieri = new HashSet<Integer>();
        if (campoCantiere.getIdScelto() > 0) {
            cantieri.add(campoCantiere.getIdScelto());
        } else {
            for (CampoRicerca.Voce v : campoCantiere.getVoci()) cantieri.add(v.id);
        }
        List<CampoRicerca.Voce> voci = new ArrayList<CampoRicerca.Voce>();
        if (!cantieri.isEmpty()) {
            String elenco = cantieri.toString().replace("[", "").replace("]", "");
            DbInterno db = new DbInterno(this);
            for (Object o : db.eseguiSelect("select * from " + Preventivi.NOME_TABELLA + " where " + Preventivi.ID_CANTIERE + " in (" + elenco
                    + ") and " + Preventivi.TIPO + "='" + Preventivi.TIPO_ORDINE + "' and (" + Preventivi.STATO + "='" + Preventivi.STATO_APERTO + "'"
                    + (idOrdineSempreIncluso != 0 ? " or " + Preventivi.ID_PREVENTIVO + "=" + idOrdineSempreIncluso : "")
                    + ") order by " + Preventivi.DATA + " desc", null)) {
                ContentValues ordine = (ContentValues) o;
                voci.add(new CampoRicerca.Voce(RigheRapportino.intero(ordine, Preventivi.ID_PREVENTIVO), "N. " + ordine.getAsInteger(Preventivi.NUMERO)
                        + " del " + Utility.numberToDataShort(ordine.getAsLong(Preventivi.DATA)) + " - "
                        + RigheRapportino.testo(ordine, Preventivi.TITOLO), ordine));
            }
            db.close();
        }
        campoOrdine.setVoci(voci);
        campoOrdine.setHint(voci.isEmpty() ? "Nessun ordine aperto" : "Tocca per scegliere (" + voci.size() + ")");
    }

    /** Cliente scelto dall'elenco o appena creato col "+": cantieri del cliente, oppure la domanda "Crea cantiere". */
    private void clienteScelto(CampoRicerca.Voce voce) {
        modificato = true;
        // il cantiere di un altro cliente non vale piu' (e con lui l'ordine)
        CampoRicerca.Voce cantiere = campoCantiere.getScelta();
        if (voce != null && cantiere != null && RigheRapportino.intero(cantiere.dati, Cantieri.ID_ANAGRAFICA) != voce.id) {
            campoCantiere.seleziona(null);
            campoOrdine.seleziona(null);
        }
        aggiornaElencoCantieri();
        aggiornaElencoOrdini(0);
        aggiornaInviaMail();
        if (voce != null && campoCantiere.getVoci().isEmpty()) {
            proponiCantiere(voce);
        } else if (voce != null && campoCantiere.getScelta() == null && campoCantiere.getVoci().size() == 1) {
            // un solo cantiere sceglibile: e' quello, senza aprire l'elenco
            campoCantiere.seleziona(campoCantiere.getVoci().get(0));
            cantiereScelto(campoCantiere.getScelta());
        } else if (voce != null) {
            apriElenco(campoCantiere); // i cantieri del cliente appena scelto
        }
    }

    /** Cantiere scelto dall'elenco o impostato da solo (unico del cliente): cliente del cantiere, ordini aperti. */
    private void cantiereScelto(CampoRicerca.Voce voce) {
        modificato = true;
        if (voce != null) { // il cliente diventa quello del cantiere (come sul server)
            avvisoApertura(voce.dati);
            int cliente = RigheRapportino.intero(voce.dati, Cantieri.ID_ANAGRAFICA);
            if (cliente != 0 && cliente != campoCliente.getIdScelto()) {
                campoCliente.selezionaId(cliente);
                aggiornaInviaMail();
            }
        }
        CampoRicerca.Voce ordine = campoOrdine.getScelta();
        if (ordine != null && (voce == null || RigheRapportino.intero(ordine.dati, Preventivi.ID_CANTIERE) != voce.id)) {
            campoOrdine.seleziona(null);
        }
        aggiornaElencoOrdini(0);
        if (voce != null) apriElenco(campoOrdine); // gli ordini aperti del cantiere, se ce ne sono
    }

    private void impostaAscoltatori() {
        campoCliente.setAscoltatore(new CampoRicerca.Ascoltatore() {
            @Override
            public void selezioneCambiata(CampoRicerca campo, CampoRicerca.Voce voce) {
                clienteScelto(voce);
            }
        });
        campoCantiere.setAscoltatore(new CampoRicerca.Ascoltatore() {
            @Override
            public void selezioneCambiata(CampoRicerca campo, CampoRicerca.Voce voce) {
                cantiereScelto(voce);
            }
        });
        campoOrdine.setAscoltatore(new CampoRicerca.Ascoltatore() {
            @Override
            public void selezioneCambiata(CampoRicerca campo, CampoRicerca.Voce voce) {
                modificato = true;
                if (voce == null) return;
                // l'ordine porta il suo cantiere, il cantiere il suo cliente
                int idCantiere = RigheRapportino.intero(voce.dati, Preventivi.ID_CANTIERE);
                if (idCantiere != campoCantiere.getIdScelto()) {
                    for (Object o : cantieriAmmessi) {
                        ContentValues c = (ContentValues) o;
                        if (RigheRapportino.intero(c, Cantieri.ID_CANTIERE) != idCantiere) continue;
                        int cliente = RigheRapportino.intero(c, Cantieri.ID_ANAGRAFICA);
                        if (cliente != 0) campoCliente.selezionaId(cliente);
                        aggiornaElencoCantieri();
                        campoCantiere.selezionaId(idCantiere);
                        aggiornaInviaMail();
                    }
                }
            }
        });
        data.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                cambioData();
            }
        });
        editNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                modificato = true;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * Cambio data (§12.4): cantieri ammessi diversi, quindi cliente, cantiere e ordine si scelgono di nuovo; le righe restano,
     * con un avviso se il loro utente non e' piu' sceglibile.
     */
    private void cambioData() {
        if (data.getValue().equals(dataCaricata)) return;
        modificato = true;
        caricaCantieriAmmessi();
        campoCliente.seleziona(null);
        campoCantiere.seleziona(null);
        campoOrdine.seleziona(null);
        aggiornaElencoCantieri();
        aggiornaElencoOrdini(0);
        aggiornaInviaMail();

        DbInterno db = new DbInterno(this);
        Set<Integer> sceglibili = new HashSet<Integer>();
        for (ContentValues ut : RegoleRapportino.operatoriSelezionabili(db, utente, 0, dataRapportino())) {
            sceglibili.add(RigheRapportino.intero(ut, Utenti.ID_UTENTE_DITTA));
        }
        db.close();
        Set<String> nomi = new LinkedHashSet<String>();
        for (Object o : righe) {
            ContentValues r = (ContentValues) o;
            int id = RigheRapportino.intero(r, RapportiniDettaglio.ID_UTENTE_MANODOPERA);
            if (id > 0 && !sceglibili.contains(id)) nomi.add(RigheRapportino.testo(r, RigheRapportino.NOME_UTENTE));
        }
        String avviso = getString(R.string.avviso_cambio_data);
        if (!nomi.isEmpty()) {
            avviso += "\n\n" + getString(R.string.avviso_utenti_non_sceglibili, android.text.TextUtils.join(", ", nomi));
        }
        Utility.mostraDialog(getString(R.string.attenzione), avviso, this, "OK");
    }

    // ── Invio al cliente per mail (INVIO_RAPPORTINI_MAIL.md) ─────────────────────────────────────────────────

    /**
     * "Invia mail" attivo solo se il rapportino e' confermato (firmato e' meglio, ma non serve) e il cliente ha l'email; senza la licenza di modifica non c'e' (il server non accetterebbe la
     * richiesta). Dopo l'invio dice "Inviata ✓".
     */
    private void aggiornaInviaMail() {
        android.widget.Button pulsante = findViewById(R.id.button_invia_mail);
        pulsante.setVisibility(senzaLicenzaModifica ? View.GONE : View.VISIBLE);
        pulsante.setEnabled(confermato() && !emailCliente().isEmpty());
        boolean inviata = testata != null && Rapportini.MAIL_INVIATA.equals(RigheRapportino.testo(testata, Rapportini.MAIL_STATO));
        pulsante.setText(inviata ? "Inviata ✓" : getString(R.string.invia_mail));
    }

    /** Email del cliente scelto ("" se non c'e'). */
    private String emailCliente() {
        CampoRicerca.Voce cliente = campoCliente.getScelta();
        return cliente != null ? RigheRapportino.testo(cliente.dati, Anagrafica.MAIL) : "";
    }

    /**
     * Invia mail: chiede conferma con i destinatari, poi segna la richiesta sul rapportino. La mail con il PDF la manda il
     * server alla sincronizzazione (anche se ora il telefono e' senza rete), all'email del cliente e in copia a chi la chiede.
     */
    public void inviaMail(View v) {
        String email = emailCliente();
        if (email.isEmpty()) {
            Toast.makeText(this, "Il cliente non ha l'email: aggiungila nella sua scheda.", Toast.LENGTH_LONG).show();
            return;
        }
        if (testata == null || modificato) {
            Toast.makeText(this, "Salva il rapportino prima di inviarlo.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!confermato()) {
            Toast.makeText(this, "Si invia al cliente solo un rapportino confermato.", Toast.LENGTH_LONG).show();
            return;
        }
        String gia = "";
        if (Rapportini.MAIL_INVIATA.equals(RigheRapportino.testo(testata, Rapportini.MAIL_STATO))) {
            gia = testoMail() + ".\n\n";
        }
        Utility.mostraConfermaDialog(getString(R.string.invia_mail), gia + "Inviare il rapportino (PDF) a " + email
                        + "?\n\nLa mail parte dal server alla sincronizzazione, con una copia a te.", this, "Invia",
                getString(R.string.annulla), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) richiediInvioMail();
                    }
                });
    }

    private void richiediInvioMail() {
        // sempre una data piu' recente della richiesta precedente: per il server e' quella che fa partire un nuovo invio
        long ora = Utility.dataToNumber(java.util.Calendar.getInstance());
        Long prima = testata.getAsLong(Rapportini.MAIL_RICHIESTA_IL);
        if (prima != null && ora <= prima) ora = prima + 1;
        DbInterno db = new DbInterno(this);
        Rapportini tabella = new Rapportini();
        ContentValues val = tabella.getValoriLogModifica(db);
        val.put(Rapportini.MAIL_RICHIESTA_IL, ora);
        val.put(Rapportini.MAIL_STATO, Rapportini.MAIL_IN_ATTESA); // solo per mostrarlo: lo stato vero lo rimanda il server
        ContentValues where = new ContentValues();
        where.put(Rapportini.ID, getIDModifica());
        tabella.aggiornaRecord(db, val, where);
        db.close();
        testata.putAll(val);
        segnaSalvato();
        com.ncfsistemi.econ.servizi.SyncWorker.scheduleImmediateSync(this);
        Toast.makeText(this, "Invio chiesto: la mail parte alla sincronizzazione.", Toast.LENGTH_LONG).show();
        aggiornaInviaMail();
        applicaStato();
    }

    /** Esito dell'invio al cliente, per la riga informativa ("" se non e' mai stato chiesto). */
    private String testoMail() {
        if (testata == null) return "";
        String stato = RigheRapportino.testo(testata, Rapportini.MAIL_STATO);
        long quando = testata.getAsLong(Rapportini.MAIL_INVIATA_IL) != null ? testata.getAsLong(Rapportini.MAIL_INVIATA_IL) : 0;
        String destinatari = RigheRapportino.testo(testata, Rapportini.MAIL_DESTINATARI);
        if (Rapportini.MAIL_INVIATA.equals(stato)) {
            return "Mail inviata al cliente" + (quando > 0 ? " il " + dataOra(quando) : "") + (destinatari.isEmpty() ? "" : " a " + destinatari);
        }
        if (Rapportini.MAIL_IN_ATTESA.equals(stato)) {
            return "Mail al cliente: in attesa di invio (parte alla sincronizzazione)";
        }
        if (Rapportini.MAIL_NON_INVIATA.equals(stato)) {
            return "Mail al cliente non inviata: " + RigheRapportino.testo(testata, Rapportini.MAIL_ERRORE);
        }
        return "";
    }

    // ── Righe ────────────────────────────────────────────────────────────────────────────────────────────────

    public void nuovaManodopera(View v) {
        apriRiga(RapportiniDettaglioTipi.CATEGORIA_LAVORO, -1);
    }

    public void nuovoViaggio(View v) {
        apriRiga(RapportiniDettaglioTipi.CATEGORIA_VIAGGIO, -1);
    }

    public void nuovoArticolo(View v) {
        apriRiga(RapportiniDettaglioTipi.CATEGORIA_MATERIALE, -1);
    }

    /** Apre la finestra della riga: nuova (posizione -1) o esistente. */
    private void apriRiga(String categoria, final int posizione) {
        PopupRigaRapportino.Contesto c = new PopupRigaRapportino.Contesto();
        c.utente = utente;
        c.soloSeStesso = !proprietario && !utente.amministratore;
        c.vedePrezzi = vedePrezzi;
        c.idCantiere = campoCantiere.getIdScelto();
        c.idOrdine = campoOrdine.getIdScelto();
        c.data = dataRapportino();
        ContentValues riga = posizione >= 0 ? (ContentValues) righe.get(posizione) : null;
        new PopupRigaRapportino(this, categoria, riga, c, new PopupRigaRapportino.Esito() {
            @Override
            public void rigaConfermata(ContentValues nuova) {
                nuova.put(DA_SALVARE, 1);
                if (posizione >= 0) {
                    righe.set(posizione, nuova);
                } else {
                    righe.add(nuova);
                }
                ordinaRighe();
                modificato = true;
                adapter.notifyDataSetChanged();
            }
        }).apri();
    }

    /** Ordine della scheda: ordine del tipo, poi ora di inizio (senza orario in fondo al gruppo); a parita', come inserite. */
    private void ordinaRighe() {
        Collections.sort(righe, new Comparator<Object>() {
            @Override
            public int compare(Object a, Object b) {
                ContentValues ra = (ContentValues) a, rb = (ContentValues) b;
                int c = RigheRapportino.intero(ra, RigheRapportino.ORDINE_TIPO) - RigheRapportino.intero(rb, RigheRapportino.ORDINE_TIPO);
                if (c != 0) return c;
                String oa = RigheRapportino.testo(ra, RapportiniDettaglio.ORA_INIZIO), ob = RigheRapportino.testo(rb, RapportiniDettaglio.ORA_INIZIO);
                if (oa.isEmpty() != ob.isEmpty()) return oa.isEmpty() ? 1 : -1;
                return oa.compareTo(ob);
            }
        });
    }

    private void eliminaRiga(final int posizione) {
        Utility.mostraConfermaCancellazioneDialog(this, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which != DialogInterface.BUTTON_POSITIVE) return;
                ContentValues r = (ContentValues) righe.remove(posizione);
                if (r.getAsInteger(RapportiniDettaglio.ID) != null) righeEliminate.add(r.getAsInteger(RapportiniDettaglio.ID));
                modificato = true;
                adapter.notifyDataSetChanged();
            }
        });
    }

    /** Tabella delle righe: modifica | elimina | tipo | descrizione tipo | utente | articolo | UM | quantita'. */
    private class RigheAdapter extends DettaglioTabellaAdapter {
        private final List<ColonnaDettaglio> colonne = new ArrayList<ColonnaDettaglio>();

        RigheAdapter() {
            super(RapportinoDettaglioModActivity.this, righe);
            colonne.add(ColonnaDettaglio.icona("modifica", "", 40));
            colonne.add(ColonnaDettaglio.icona("elimina", "", 40));
            colonne.add(ColonnaDettaglio.testoFisso("tipo", getString(R.string.tipo), 56, Gravity.START));
            colonne.add(ColonnaDettaglio.testo("descrizione_tipo", getString(R.string.descrizione_tipo), 1.2f));
            colonne.add(ColonnaDettaglio.testo("utente", getString(R.string.utente), 1f));
            colonne.add(ColonnaDettaglio.testo("articolo", getString(R.string.articolo), 1.5f));
            colonne.add(ColonnaDettaglio.testoFisso("um", getString(R.string.um), 50, Gravity.START));
            colonne.add(ColonnaDettaglio.testoFisso("quantita", getString(R.string.qta), 70, Gravity.END));
            colonne.add(ColonnaDettaglio.testo("inserita", "Inserita da", 0.9f));
        }

        @Override
        protected List<ColonnaDettaglio> getColonne() {
            return colonne;
        }

        @Override
        protected void bindCella(final int position, ColonnaDettaglio colonna, View cella) {
            ContentValues r = (ContentValues) righe.get(position);
            cella.setOnClickListener(null);
            cella.setClickable(false);
            switch (colonna.id) {
                case "modifica":
                case "elimina":
                    boolean modifica = colonna.id.equals("modifica");
                    if (!modificabile || !RegoleRapportino.rigaModificabile(r, utente, proprietario)) {
                        ((TextView) cella).setText("");
                        break;
                    }
                    impostaIcona(cella, modifica ? FaIcone.MODIFICA : FaIcone.ELIMINA, getString(modifica ? R.string.modifica : R.string.elimina));
                    final String categoria = RigheRapportino.testo(r, RigheRapportino.CATEGORIA);
                    cella.setOnClickListener(modifica ? new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            apriRiga(categoria, position);
                        }
                    } : new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            eliminaRiga(position);
                        }
                    });
                    break;
                case "tipo":
                    ((TextView) cella).setText(RigheRapportino.testo(r, RigheRapportino.CODICE_TIPO));
                    break;
                case "descrizione_tipo":
                    String orari = RigheRapportino.orari(r);
                    String note = RigheRapportino.testo(r, RapportiniDettaglio.NOTE);
                    ((TextView) cella).setText(RigheRapportino.testo(r, RigheRapportino.DESCRIZIONE_TIPO)
                            + (orari.isEmpty() ? "" : "  " + orari) + (note.isEmpty() ? "" : "\n" + note));
                    break;
                case "utente":
                    ((TextView) cella).setText(RigheRapportino.testo(r, RigheRapportino.NOME_UTENTE));
                    break;
                case "articolo":
                    ((TextView) cella).setText(RigheRapportino.articolo(r));
                    break;
                case "um":
                    ((TextView) cella).setText(RigheRapportino.testo(r, RigheRapportino.CODICE_UNITA));
                    break;
                case "quantita":
                    ((TextView) cella).setText(Utility.formatNumero(RigheRapportino.decimale(r, RapportiniDettaglio.QUANTITA)));
                    break;
                case "inserita":
                    ((TextView) cella).setText(nomeInserita(r));
                    break;
            }
        }
    }

    /** Chi ha inserito la riga (non si cambia): dagli operatori scaricati; le righe nuove sono di chi compila. */
    private final java.util.Map<Integer, String> nomiAccount = new java.util.HashMap<Integer, String>();

    private String nomeInserita(ContentValues r) {
        int id = r.getAsInteger(RapportiniDettaglio.ID) == null ? utente.idAccount : RigheRapportino.intero(r, RapportiniDettaglio.ID_OPERATORE_INS);
        if (id <= 0) return "—";
        if (!nomiAccount.containsKey(id)) {
            DbInterno db = new DbInterno(RapportinoDettaglioModActivity.this);
            String n = RegoleRapportino.nomeAccount(db, id);
            db.close();
            nomiAccount.put(id, n.isEmpty() ? "—" : n);
        }
        return nomiAccount.get(id);
    }

    // ── Salvataggio ───────────────────────────────────────────────────────────────────────────────────────────

    /** Salva: il rapportino resta nello stato attuale (nuovo: quello iniziale, Bozza). */
    @Override
    public void salva(View v) {
        idStatoDestino = stato != null ? RigheRapportino.intero(stato, StatiDocumento.ID) : 0;
        eseguiSalvataggio("Rapportino salvato.");
    }

    /**
     * Testata e righe in un'unica transazione, confermata solo se tutto va bene (la classe base la conferma anche quando
     * il salvataggio ritorna un messaggio d'errore: qui un errore non deve lasciare il rapportino salvato a meta').
     */
    private void eseguiSalvataggio(String messaggio) {
        eseguiSalvataggio(messaggio, false);
    }

    /** @param inviaInSede dopo il salvataggio si invia subito al server, che da' il numero (Conferma), poi si riapre */
    private void eseguiSalvataggio(String messaggio, boolean inviaInSede) {
        DbInterno db = new DbInterno(this);
        android.database.sqlite.SQLiteDatabase sqlite = db.getReadableDatabase();
        String esito;
        sqlite.beginTransaction();
        try {
            esito = getModalita() == INSERIMENTO ? eseguiInserimento(db) : eseguiAggiornamento(db);
            if (SALVATAGGIO_OK.equals(esito)) sqlite.setTransactionSuccessful();
        } catch (Exception e) {
            esito = "ERR_" + (e.getMessage() != null ? e.getMessage() : android.util.Log.getStackTraceString(e));
        } finally {
            sqlite.endTransaction();
            db.close();
        }
        if (SALVATAGGIO_OK.equals(esito) && inviaInSede) {
            salvato = true;
            setResult(RESULT_OK);
            Intent intent = new Intent(this, SincronizzazioneConfermaActivity.class);
            intent.putExtra(SincronizzazioneActivity.EXTRA_MODE, SincronizzazioneActivity.MODE_CONFERMA);
            intent.putExtra(SincronizzazioneActivity.EXTRA_ID_RAPPORTINO, idSalvato);
            startActivityForResult(intent, RICHIESTA_INVIO_CONFERMA);
        } else if (SALVATAGGIO_OK.equals(esito)) {
            riapri(messaggio);
        } else if (esito.startsWith("ERR")) {
            EconCrash(esito);
        } else {
            Toast.makeText(this, esito, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Dopo il salvataggio la maschera resta aperta: si riapre sul rapportino salvato (un nuovo passa in modifica), cosi'
     * stato, permessi, numero e pulsanti sono quelli veri. Il risultato va a chi aveva aperto questa (FORWARD_RESULT).
     */
    private void riapri(String messaggio) {
        Intent intent = new Intent(getIntent());
        intent.removeExtra(EXTRA_DATA);
        intent.putExtra("ID", idSalvato);
        intent.putExtra(EXTRA_SALVATO, messaggio);
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        setResult(RESULT_OK);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }

    /** In verifica: salva e passa allo stato In verifica, in cui gli operatori coinvolti aggiungono le proprie ore. */
    public void mettiInVerifica(final View v) {
        Utility.mostraConfermaDialog("In verifica", "Mettere il rapportino in verifica? Gli operatori coinvolti potranno aggiungere e correggere le proprie ore; poi lo confermi.",
                this, "In verifica", getString(R.string.annulla), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which != DialogInterface.BUTTON_POSITIVE) return;
                        DbInterno db = new DbInterno(RapportinoDettaglioModActivity.this);
                        ContentValues verifica = RegoleRapportino.statoPerCodice(db, StatiDocumento.CODICE_VERIFICA);
                        db.close();
                        if (verifica == null) return;
                        idStatoDestino = RigheRapportino.intero(verifica, StatiDocumento.ID);
                        eseguiSalvataggio("Rapportino in verifica.");
                    }
                });
    }

    /**
     * Conferma: l'avviso spiega che il rapportino va in sede, riceve il numero progressivo e (regole dello stato Confermato)
     * non si modifica piu'; con "Conferma e invia" si salva come Confermato e si invia subito, con la finestra del
     * collegamento e della sincronizzazione (NUMERAZIONE_DOCUMENTI.md §6). Senza rete resta confermato sul dispositivo e il
     * numero arriva alla prossima sincronizzazione.
     */
    public void conferma(final View v) {
        DbInterno dbs = new DbInterno(this);
        ContentValues arrivo = RegoleRapportino.statoPerCodice(dbs, StatiDocumento.CODICE_CONFERMATO);
        dbs.close();
        String messaggio = getString(R.string.conferma_rapportino);
        if (!RegoleRapportino.isModificabile(arrivo) || RegoleRapportino.bloccatoInApp(arrivo)) {
            messaggio += " " + getString(R.string.conferma_non_modificabile);
        } else if (!RegoleRapportino.isCancellabile(arrivo)) {
            messaggio += " " + getString(R.string.conferma_non_cancellabile);
        }
        messaggio += "\n\n" + getString(R.string.conferma_dopo);
        Utility.mostraConfermaDialog(getString(R.string.conferma_titolo), messaggio, this, getString(R.string.conferma_e_invia),
                getString(R.string.annulla), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which != DialogInterface.BUTTON_POSITIVE) return;
                        DbInterno db = new DbInterno(RapportinoDettaglioModActivity.this);
                        ContentValues confermato = RegoleRapportino.statoPerCodice(db, StatiDocumento.CODICE_CONFERMATO);
                        db.close();
                        if (confermato == null) return;
                        idStatoDestino = RigheRapportino.intero(confermato, StatiDocumento.ID);
                        eseguiSalvataggio("Rapportino confermato.", true);
                    }
                });
    }

    @Override
    protected String eseguiInserimento(DbInterno db) {
        String errore = controlla(db);
        if (errore != null) return errore;
        if (utente.idUtenteDitta == 0) return getString(R.string.operatori_non_scaricati);

        Rapportini tabella = new Rapportini();
        ContentValues val = tabella.getValoriLogInserimento(db);
        valoriTestata(val);
        val.put(Rapportini.ID_UTENTE_DITTA, utente.idUtenteDitta);
        tabella.inserisciRecord(db, val);
        idSalvato = val.getAsInteger(Rapportini.ID);
        return salvaRighe(db, idSalvato, val.getAsInteger(Rapportini.ID_ORDINE));
    }

    @Override
    protected String eseguiAggiornamento(DbInterno db) {
        String errore = controlla(db);
        if (errore != null) return errore;

        Rapportini tabella = new Rapportini();
        ContentValues val = tabella.getValoriLogModifica(db);
        valoriTestata(val); // l'autore non cambia
        ContentValues where = new ContentValues();
        where.put(Rapportini.ID, getIDModifica());
        tabella.aggiornaRecord(db, val, where);

        RapportiniDettaglio tabRighe = new RapportiniDettaglio();
        for (Integer id : righeEliminate) {
            ContentValues del = new ContentValues();
            del.put(RapportiniDettaglio.ID, id);
            tabRighe.cancellaRecord(db, del);
        }
        idSalvato = getIDModifica();
        return salvaRighe(db, idSalvato, val.getAsInteger(Rapportini.ID_ORDINE));
    }

    private String controlla(DbInterno db) {
        if (senzaLicenzaModifica) return getString(R.string.sola_lettura_licenza);
        if (!modificabile) return getString(R.string.rapportino_non_modificabile, RigheRapportino.testo(stato, StatiDocumento.NOME));
        if (!testataModificabile && !aggiungeRighe) return "Le righe di questo rapportino le modifica l'autore o l'amministratore.";
        // cantiere obbligatorio (STUDIO_ASSISTENZE_TEMPO.md §8, come Rapportini::valida() del server); i vecchi senza restano modificabili
        boolean vecchioSenzaCantiere = testata != null && RigheRapportino.intero(testata, Rapportini.ID_CANTIERE) == 0;
        if (campoCantiere.getIdScelto() == 0 && campoOrdine.getIdScelto() == 0 && !vecchioSenzaCantiere) {
            return getString(R.string.errore_cantiere_rapportino);
        }
        if (campoCliente.getIdScelto() == 0 && campoCantiere.getIdScelto() == 0 && campoOrdine.getIdScelto() == 0) {
            return getString(R.string.errore_riferimento_rapportino);
        }
        int idStatoAttuale = stato != null ? RigheRapportino.intero(stato, StatiDocumento.ID) : 0;
        if (idStatoDestino != idStatoAttuale && !RegoleRapportino.passaggioAmmesso(db, idStatoAttuale, idStatoDestino, utente)) {
            return getString(R.string.passaggio_stato_non_ammesso);
        }
        return null;
    }

    /** Testata: l'ordine porta il suo cantiere, il cantiere il suo cliente (come Rapportini::allineaRiferimenti() del server). */
    private void valoriTestata(ContentValues val) {
        int idCliente = campoCliente.getIdScelto();
        int idCantiere = campoCantiere.getIdScelto();
        CampoRicerca.Voce ordine = campoOrdine.getScelta();
        if (ordine != null && RigheRapportino.intero(ordine.dati, Preventivi.ID_CANTIERE) != 0) {
            idCantiere = RigheRapportino.intero(ordine.dati, Preventivi.ID_CANTIERE);
        }
        CampoRicerca.Voce cantiere = campoCantiere.cercaId(idCantiere);
        if (cantiere != null && RigheRapportino.intero(cantiere.dati, Cantieri.ID_ANAGRAFICA) != 0) {
            idCliente = RigheRapportino.intero(cantiere.dati, Cantieri.ID_ANAGRAFICA);
        }
        val.put(Rapportini.DATA_RAPPORTINO, dataRapportino());
        val.put(Rapportini.ID_CLIENTE, idCliente);
        val.put(Rapportini.ID_CANTIERE, idCantiere);
        val.put(Rapportini.ID_ORDINE, ordine != null ? ordine.id : 0);
        val.put(Rapportini.ID_STATO, idStatoDestino);
        val.put(Rapportini.NOTE, editNote.getText().toString().trim());
        if (firmaNuova != null) {
            val.put(Rapportini.FIRMA, firmaNuova);
            val.put(Rapportini.FIRMA_NOME, firmaNuovaNome);
            val.put(Rapportini.FIRMA_DATA, firmaNuovaData);
        }
    }

    // ── Stampa (GESTIONE_RAPPORTINI.md §15) ─────────────────────────────────────────────────────────────────────

    /** Stampa il rapportino salvato con il modello della ditta; con modifiche non salvate chiede di salvare prima. */
    public void stampa(View v) {
        if (testata == null || modificato) {
            Toast.makeText(this, "Salva il rapportino prima di stamparlo.", Toast.LENGTH_LONG).show();
            return;
        }
        com.ncfsistemi.econ.stampa.StampaRapportino.stampa(this, getIDModifica());
    }

    // ── Firma del cliente (GESTIONE_RAPPORTINI.md §13) ──────────────────────────────────────────────────────────

    /** Firma attuale: quella appena fatta o quella salvata; stringa vuota se il rapportino non e' firmato. */
    private String firmaAttuale() {
        if (firmaNuova != null) return firmaNuova;
        return testata != null ? RigheRapportino.testo(testata, Rapportini.FIRMA) : "";
    }

    /** Il rapportino e' confermato: solo allora il cliente firma e si invia per mail (STUDIO_FLUSSO_DOCUMENTI_E_MAIL.md). */
    private boolean confermato() {
        return testata != null && stato != null && StatiDocumento.CODICE_CONFERMATO.equals(RigheRapportino.testo(stato, StatiDocumento.CODICE));
    }

    /**
     * Si firma solo un rapportino confermato; la firma non cambia lo stato, e' un indicatore con la sua data. La prima
     * volta chiunque abbia la licenza di modifica, di nuovo solo chi puo' modificare il confermato (l'amministratore),
     * come sul server. Senza RAPPORTINI.MODIFICA la firma solo si guarda.
     */
    private boolean puoFirmare() {
        return !senzaLicenzaModifica && confermato() && (modificabile || firmaAttuale().isEmpty());
    }

    /** Pulsante Firma: se c'e' gia' la firma la mostra (con "Firma di nuovo" se si puo'), altrimenti apre la firma. */
    public void firma(View v) {
        if (firmaAttuale().isEmpty()) {
            if (puoFirmare()) apriFirma();
            return;
        }
        android.widget.ImageView img = new android.widget.ImageView(this);
        img.setImageBitmap(bitmapFirma(firmaAttuale()));
        img.setAdjustViewBounds(true);
        img.setBackgroundColor(android.graphics.Color.WHITE);
        int p = Math.round(16 * getResources().getDisplayMetrics().density);
        img.setPadding(p, p, p, p);
        androidx.appcompat.app.AlertDialog.Builder b = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(testoFirma())
                .setView(img)
                .setNegativeButton("Chiudi", null);
        if (puoFirmare()) b.setPositiveButton("Firma di nuovo", (d, w) -> apriFirma());
        b.show();
    }

    private void apriFirma() {
        Intent intent = new Intent(this, FirmaActivity.class);
        String nome = firmaNuova != null ? firmaNuovaNome : (testata != null ? RigheRapportino.testo(testata, Rapportini.FIRMA_NOME) : "");
        intent.putExtra(FirmaActivity.EXTRA_NOME, nome);
        CampoRicerca.Voce cliente = campoCliente.getScelta();
        intent.putExtra(FirmaActivity.EXTRA_TITOLO, "Firma del cliente" + (cliente != null ? " — " + RigheRapportino.testo(cliente.dati, Anagrafica.RAGIONE_SOCIALE) : ""));
        startActivityForResult(intent, RICHIESTA_FIRMA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // foto del nuovo articolo chiesta dalla finestra della riga (§14)
        if (com.ncfsistemi.econ.views.PopupRigaRapportino.risultatoFoto(requestCode, resultCode, data)) return;
        if (requestCode == RICHIESTA_INVIO_CONFERMA) {
            // l'id cambia se il rapportino era nuovo (quello del server); comunque si riapre sul rapportino confermato
            String messaggio = "Rapportino confermato.";
            if (data != null) {
                idSalvato = data.getIntExtra(SincronizzazioneActivity.EXTRA_ID_RAPPORTINO, idSalvato);
                if (data.getStringExtra(SincronizzazioneActivity.EXTRA_MESSAGGIO) != null) {
                    messaggio = data.getStringExtra(SincronizzazioneActivity.EXTRA_MESSAGGIO);
                }
            }
            riapri(messaggio);
            return;
        }
        if (requestCode == RICHIESTA_NUOVO_CLIENTE) {
            if (resultCode != RESULT_OK) return;
            // i clienti nuovi del dispositivo hanno l'id negativo: l'ultimo creato e' il piu' basso
            DbInterno db = new DbInterno(this);
            int nuovo = db.getUltimoId(new Anagrafica());
            caricaClienti(db);
            db.close();
            CampoRicerca.Voce voce = campoCliente.cercaId(nuovo);
            if (voce == null) Toast.makeText(this, "Cliente salvato: sceglilo dall'elenco.", Toast.LENGTH_LONG).show();
            if (voce != null) {
                campoCliente.seleziona(voce);
                clienteScelto(voce); // propone il cantiere alla sua sede
            }
            return;
        }
        if (requestCode != RICHIESTA_FIRMA || resultCode != RESULT_OK || data == null) return;
        firmaNuova = data.getStringExtra(FirmaActivity.EXTRA_FIRMA);
        firmaNuovaNome = data.getStringExtra(FirmaActivity.EXTRA_NOME);
        firmaNuovaData = Utility.dataToNumber(java.util.Calendar.getInstance());
        if (testata != null) {
            // il rapportino e' confermato: si salva subito solo la firma, come accetta il server; lo stato non cambia
            DbInterno db = new DbInterno(this);
            Rapportini tabella = new Rapportini();
            ContentValues val = tabella.getValoriLogModifica(db);
            val.put(Rapportini.FIRMA, firmaNuova);
            val.put(Rapportini.FIRMA_NOME, firmaNuovaNome);
            val.put(Rapportini.FIRMA_DATA, firmaNuovaData);
            ContentValues where = new ContentValues();
            where.put(Rapportini.ID, getIDModifica());
            tabella.aggiornaRecord(db, val, where);
            testata.putAll(val);
            db.close();
            firmaNuova = null;
            segnaSalvato();
            Toast.makeText(this, "Firma salvata.", Toast.LENGTH_SHORT).show();
        }
        mostraFirma();
    }

    /** Tutte e due aperte in orizzontale (dopo una rotazione): la testata occupa al massimo questa parte dell'altezza (poi scorre). */
    private static final float TESTATA_MAX_ORIZZONTALE = 0.42f;

    /** Sezioni a fisarmonica (21/10): testata e righe si aprono/chiudono toccando il titolo. */
    private boolean testataAperta = true, righeAperte = true;
    private TextView frecciaTestata, frecciaRighe;
    /** Icone delle nuove righe nel titolo "Righe" (solo in compatto). */
    private android.widget.LinearLayout azioniRighe;

    /** Smartphone in orizzontale: pochissima altezza, la pagina si stringe (margini, nuove righe come icone). */
    private boolean compatto() {
        android.content.res.Configuration c = getResources().getConfiguration();
        return c.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE && c.smallestScreenWidthDp < 600;
    }

    /** Nuove righe: pulsanti sopra la tabella, o icone nel titolo in compatto; solo per chi puo' aggiungere righe. */
    private void aggiornaPulsantiRighe() {
        boolean compatto = compatto();
        findViewById(R.id.pulsanti_righe).setVisibility(aggiungeRighe && !compatto ? View.VISIBLE : View.GONE);
        if (azioniRighe != null) azioniRighe.setVisibility(aggiungeRighe && compatto ? View.VISIBLE : View.GONE);
    }

    /**
     * Rapportino esistente: testata chiusa (cliente / cantiere / data nel titolo) e righe aperte, per controllare e
     * aggiungere righe; nuovo: tutte e due aperte, per scegliere cliente e cantiere.
     */
    private void preparaFisarmonica() {
        testataAperta = testata == null;
        // nuovo rapportino in orizzontale: prima la testata (cliente e cantiere), le righe si aprono dopo
        righeAperte = !(testataAperta && esclusiva());
        View sezTestata = findViewById(R.id.sezione_testata), sezRighe = findViewById(R.id.sezione_righe);
        com.ncfsistemi.econ.utils.SezioneStandard.imposta(sezRighe, com.ncfsistemi.econ.utils.FaIcone.ELENCO, "Righe");
        ((ViewGroup.MarginLayoutParams) sezRighe.getLayoutParams()).topMargin = Math.round(2 * getResources().getDisplayMetrics().density);
        frecciaTestata = freccia(sezTestata);
        // nuove righe come icone nel titolo "Righe": al posto della riga di pulsanti dove l'altezza e' poca (compatto)
        azioniRighe = new android.widget.LinearLayout(this);
        azioniRighe.addView(com.ncfsistemi.econ.utils.FaIcone.azione(this, com.ncfsistemi.econ.utils.FaIcone.MANODOPERA,
                getString(R.string.aggiungi_manodopera), this::nuovaManodopera));
        azioniRighe.addView(com.ncfsistemi.econ.utils.FaIcone.azione(this, com.ncfsistemi.econ.utils.FaIcone.VIAGGIO,
                getString(R.string.aggiungi_viaggio), this::nuovoViaggio));
        azioniRighe.addView(com.ncfsistemi.econ.utils.FaIcone.azione(this, com.ncfsistemi.econ.utils.FaIcone.MATERIALE,
                getString(R.string.aggiungi_articolo), this::nuovoArticolo));
        ((ViewGroup) sezRighe.findViewById(R.id.sezioneAzioni)).addView(azioniRighe);
        frecciaRighe = freccia(sezRighe);
        aggiornaPulsantiRighe();
        ((ViewGroup.MarginLayoutParams) sezTestata.getLayoutParams()).topMargin = Math.round(2 * getResources().getDisplayMetrics().density);
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
        // il numero di righe nel titolo resta giusto a ogni riga aggiunta, modificata o tolta
        adapter.registerDataSetObserver(new android.database.DataSetObserver() {
            @Override
            public void onChanged() {
                titoliFisarmonica();
            }
        });
        fisarmonica();
    }

    /**
     * Fisarmonica vera (aprire una sezione chiude l'altra e quella aperta prende tutto lo schermo): in orizzontale, dove
     * l'altezza e' poca, e sempre sugli smartphone; sui tablet in verticale si possono tenere aperte tutte e due.
     */
    private boolean esclusiva() {
        android.content.res.Configuration c = getResources().getConfiguration();
        return c.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE || c.smallestScreenWidthDp < 600;
    }

    /** Smartphone in verticale: cliente, cantiere e ordine uno sotto l'altro a tutta larghezza (in tre colonne si tagliano). */
    private void disponiSoggetti() {
        float densita = getResources().getDisplayMetrics().density;
        boolean incolonna = getResources().getConfiguration().screenWidthDp < 600;
        android.widget.LinearLayout soggetti = findViewById(R.id.riga_soggetti);
        soggetti.setOrientation(incolonna ? android.widget.LinearLayout.VERTICAL : android.widget.LinearLayout.HORIZONTAL);
        for (int i = 0; i < soggetti.getChildCount(); i++) {
            android.widget.LinearLayout.LayoutParams lp = incolonna
                    ? new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    : new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0 && incolonna) lp.topMargin = Math.round(12 * densita);
            if (i > 0 && !incolonna) lp.setMarginStart(Math.round(10 * densita));
            soggetti.getChildAt(i).setLayoutParams(lp);
        }
    }

    /** Freccia della sezione (non cliccabile da sola: il tocco va a tutto il titolo). */
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

    /**
     * Aperto/chiuso, frecce, titoli e spazio: la sezione aperta prende l'altezza libera; con le righe aperte, in
     * orizzontale, la testata non supera TESTATA_MAX_ORIZZONTALE (scorre).
     */
    private void fisarmonica() {
        // dopo una rotazione: dove la fisarmonica e' esclusiva resta aperta una sola sezione
        if (esclusiva() && testataAperta && righeAperte) righeAperte = false;
        View scroll = findViewById(R.id.scroll_testata);
        scroll.setVisibility(testataAperta ? View.VISIBLE : View.GONE);
        findViewById(R.id.contenuto_righe).setVisibility(righeAperte ? View.VISIBLE : View.GONE);
        com.ncfsistemi.econ.utils.FaIcone.applica(frecciaTestata, testataAperta
                ? com.ncfsistemi.econ.utils.FaIcone.CHIUDI_SEZIONE : com.ncfsistemi.econ.utils.FaIcone.APRI, null);
        com.ncfsistemi.econ.utils.FaIcone.applica(frecciaRighe, righeAperte
                ? com.ncfsistemi.econ.utils.FaIcone.CHIUDI_SEZIONE : com.ncfsistemi.econ.utils.FaIcone.APRI, null);
        titoliFisarmonica();

        boolean testataRiempie = testataAperta && !righeAperte;
        float densita = getResources().getDisplayMetrics().density;
        // compatto: margini delle card e della barra in fondo ridotti, tutto lo spazio alla sezione aperta
        int margine = Math.round((compatto() ? 6 : 12) * densita);
        aggiornaPulsantiRighe();
        View barra = (View) findViewById(R.id.barra_pulsanti).getParent();
        barra.setPadding(barra.getPaddingLeft(), Math.round((compatto() ? 2 : 6) * densita), barra.getPaddingRight(),
                Math.round((compatto() ? 2 : 6) * densita));
        android.widget.LinearLayout.LayoutParams lpT = (android.widget.LinearLayout.LayoutParams) findViewById(R.id.card_testata).getLayoutParams();
        lpT.height = testataRiempie ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
        lpT.weight = testataRiempie ? 1f : 0f;
        lpT.setMargins(margine, margine, margine, righeAperte ? 0 : margine);
        findViewById(R.id.card_testata).setLayoutParams(lpT);
        android.widget.LinearLayout.LayoutParams lpR = (android.widget.LinearLayout.LayoutParams) findViewById(R.id.card_righe).getLayoutParams();
        lpR.height = righeAperte ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
        lpR.weight = righeAperte ? 1f : 0f;
        lpR.setMargins(margine, margine, margine, margine);
        findViewById(R.id.card_righe).setLayoutParams(lpR);
        android.widget.LinearLayout.LayoutParams lpS = (android.widget.LinearLayout.LayoutParams) scroll.getLayoutParams();
        lpS.height = testataRiempie ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
        lpS.weight = testataRiempie ? 1f : 0f;
        scroll.setLayoutParams(lpS);

        android.content.res.Configuration c = getResources().getConfiguration();
        if (testataAperta && righeAperte && c.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            int massimo = Math.round(c.screenHeightDp * densita * TESTATA_MAX_ORIZZONTALE);
            scroll.post(() -> {
                // nel frattempo una sezione puo' essere stata chiusa: allora la testata resta a tutta altezza
                if (testataAperta && righeAperte && scroll.getHeight() > massimo) {
                    lpS.height = massimo;
                    scroll.setLayoutParams(lpS);
                }
            });
        }
    }

    /** Titoli: da chiusa la testata riassume cliente, cantiere e data; le righe dicono quante sono. */
    private void titoliFisarmonica() {
        String titolo = "Rapportino";
        if (!testataAperta) {
            StringBuilder r = new StringBuilder();
            CampoRicerca.Voce cli = campoCliente.getScelta(), can = campoCantiere.getScelta();
            if (cli != null) r.append(" · ").append(cli.testo);
            if (can != null) r.append(" · ").append(can.testo);
            r.append(" · ").append(data.getTesto().getText());
            titolo += r;
        }
        ((TextView) findViewById(R.id.sezione_testata).findViewById(R.id.sezioneTitolo)).setText(titolo);
        ((TextView) findViewById(R.id.sezione_righe).findViewById(R.id.sezioneTitolo))
                .setText(righe.isEmpty() ? "Righe" : "Righe (" + righe.size() + ")");
    }

    /** Anteprima della firma sotto le note e testo del pulsante; il pulsante c'e' se si puo' firmare o guardare la firma. */
    private void mostraFirma() {
        String firma = firmaAttuale();
        View riga = findViewById(R.id.riga_firma);
        android.widget.Button pulsante = findViewById(R.id.button_firma);
        pulsante.setVisibility(puoFirmare() || !firma.isEmpty() ? View.VISIBLE : View.GONE);
        if (firma.isEmpty()) {
            riga.setVisibility(View.GONE);
            pulsante.setText(R.string.firma);
            disponiPulsanti();
            return;
        }
        riga.setVisibility(View.VISIBLE);
        ((android.widget.ImageView) findViewById(R.id.img_firma)).setImageBitmap(bitmapFirma(firma));
        ((TextView) findViewById(R.id.text_firma)).setText(testoFirma());
        pulsante.setText("Firmato ✓");
        disponiPulsanti();
    }

    /** "Firmato da Mario Rossi il 30/09/2026 10:40". */
    private String testoFirma() {
        String nome = firmaNuova != null ? firmaNuovaNome : RigheRapportino.testo(testata, Rapportini.FIRMA_NOME);
        Long quando = firmaNuova != null ? Long.valueOf(firmaNuovaData) : (testata != null ? testata.getAsLong(Rapportini.FIRMA_DATA) : null);
        String q = "" + (quando != null ? quando : "");
        String data = q.length() == 14 ? Utility.numberToData(quando) + " " + q.substring(8, 10) + ":" + q.substring(10, 12) : "";
        return "Firmato" + (nome == null || nome.isEmpty() ? "" : " da " + nome) + (data.isEmpty() ? "" : " il " + data);
    }

    private static android.graphics.Bitmap bitmapFirma(String base64) {
        try {
            byte[] png = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            return android.graphics.BitmapFactory.decodeByteArray(png, 0, png.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Scrive le righe nuove o cambiate; quelle imputate a righe di un ordine diverso da quello del rapportino (ordine tolto o
     * cambiato) tornano extra ordine.
     */
    private String salvaRighe(DbInterno db, int idRapportino, int idOrdine) {
        Set<Integer> righeOrdine = new HashSet<Integer>();
        for (Object o : db.eseguiSelect("select " + PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO + " from " + PreventiviDettaglio.NOME_TABELLA
                + " where " + PreventiviDettaglio.ID_PREVENTIVO + "=" + idOrdine, null)) {
            righeOrdine.add(((ContentValues) o).getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO));
        }
        RapportiniDettaglio tab = new RapportiniDettaglio();
        for (Object o : righe) {
            ContentValues r = (ContentValues) o;
            int rigaOrdine = RigheRapportino.intero(r, RapportiniDettaglio.ID_PREVENTIVO_DETTAGLIO);
            if (rigaOrdine != 0 && (idOrdine == 0 || !righeOrdine.contains(rigaOrdine))) {
                r.put(RapportiniDettaglio.ID_PREVENTIVO_DETTAGLIO, 0);
                r.put(DA_SALVARE, 1);
            }
            if (RigheRapportino.intero(r, DA_SALVARE) != 1) continue;

            Integer id = r.getAsInteger(RapportiniDettaglio.ID);
            ContentValues val = id == null ? tab.getValoriLogInserimento(db) : tab.getValoriLogModifica(db);
            for (String campo : tab.getNomiCampi()) {
                if (campo.equals(RapportiniDettaglio.ID) || campo.equals(RapportiniDettaglio.ID_DITTA) || campo.startsWith("id_operatore_")
                        || campo.startsWith("data_") || campo.equals(RapportiniDettaglio.IN_SERVER) || !r.containsKey(campo)) {
                    continue;
                }
                Object v = r.get(campo);
                if (v == null) {
                    val.putNull(campo);
                } else {
                    val.put(campo, v.toString());
                }
            }
            val.put(RapportiniDettaglio.ID_RAPPORTINO, idRapportino);
            if (id == null) {
                tab.inserisciRecord(db, val);
            } else {
                ContentValues where = new ContentValues();
                where.put(RapportiniDettaglio.ID, id);
                tab.aggiornaRecord(db, val, where);
            }
        }
        return SALVATAGGIO_OK;
    }

    // ── Uscita ────────────────────────────────────────────────────────────────────────────────────────────────

    /** Chiudi (e indietro): l'unico modo di uscire; con modifiche non salvate chiede conferma. */
    @Override
    public void annulla(View v) {
        if (!modificato || !modificabile) {
            chiudi();
            return;
        }
        Utility.mostraConfermaDialog(getString(R.string.attenzione), getString(R.string.modifiche_non_salvate), this, "Esci",
                getString(R.string.annulla), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) chiudi();
                    }
                });
    }

    /** Chi ha aperto la maschera si aggiorna se qui si e' salvato qualcosa. */
    private void chiudi() {
        setResult(salvato ? RESULT_OK : RESULT_CANCELED);
        finish();
    }

    @Override
    public void onBackPressed() {
        annulla(null);
    }
}
