package pfa.app.econtab;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.Rapportini;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.db.table.RapportiniDettaglioOperatori;
import pfa.app.econtab.utils.RegoleRapportino;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabCalendario;
import pfa.app.econtab.views.EConTabSpinner;
import pfa.app.econtab.views.PopupDettaglioRapportino;

/**
 * Testata del rapportino: data, cliente, cantiere e ordine (facoltativi ma almeno uno; il cantiere porta il suo
 * cliente, l'ordine il suo cantiere) e note; sotto, le righe (viaggi e lavoro) con i loro operatori. I cantieri
 * sceglibili sono quelli su cui l'utente e' pianificato nella settimana della data (vedi RegoleRapportino).
 */
public class RapportinoDettaglioModActivity extends EConTabDettaglioActivity implements View.OnLongClickListener, View.OnClickListener {

    private EConTabSpinner spinner_clienti = null;
    private EConTabSpinner spinner_cantieri = null;
    private EConTabSpinner spinner_ordini = null;
    private EConTabCalendario data = null;

    private RegoleRapportino.Utente utente = null;
    /** Riferimenti salvati sul rapportino in modifica: cantiere e ordine restano sceglibili anche se non piu' ammessi. */
    private int idCantiereSalvato = 0;
    private int idOrdineSalvato = 0;
    /** Cantieri ammessi per la data (id_cantiere, nome, id_anagrafica, ragione_sociale), prima del filtro per cliente. */
    private ArrayList<Object> cantieriAmmessi = new ArrayList<Object>();
    /** Ultimi valori per cui sono stati caricati cantieri e ordini (i listener scattano anche sui setText interni). */
    private String dataCaricata = null;
    private String clienteCaricato = null;
    private String cantiereCaricato = null;
    /** true mentre i valori degli spinner si impostano da codice: i listener non devono ricaricare a cascata. */
    private boolean aggiornamentoInCorso = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_rapportino_dettaglio_mod);
        data = (EConTabCalendario) findViewById(R.id.edit_data);
        spinner_clienti = (EConTabSpinner) findViewById(R.id.spinner_clienti);
        spinner_cantieri = (EConTabSpinner) findViewById(R.id.spinner_cantieri);
        spinner_ordini = (EConTabSpinner) findViewById(R.id.spinner_ordini);

        DbInterno db = new DbInterno(this);
        utente = RegoleRapportino.utenteCorrente(db, this);
        db.close();

        super.onCreate(savedInstanceState);

        caricaClienti();
        if (getModalita() == INSERIMENTO) {
            // Autore = utente loggato (etichetta fissa); in modifica resta quello del rapportino (vedi inizializzaModifica)
            impostaOperatore(utente.idUtenteDitta);
            findViewById(R.id.linear_lista_dett).setVisibility(View.GONE);

            // stessi cliente e cantiere dell'ultimo rapportino dell'autore (il cantiere se e' ancora tra quelli ammessi)
            DbInterno dbcl = new DbInterno(this);
            ContentValues lastRapp = new Rapportini().getUltimoRapportinoOperatore(dbcl, utente.idUtenteDitta);
            dbcl.close();
            if (lastRapp != null) {
                spinner_clienti.setValue("" + intero(lastRapp, Rapportini.ID_CLIENTE));
                spinner_cantieri.setValue("" + intero(lastRapp, Rapportini.ID_CANTIERE));
            }
            caricaClienti();
            caricaCantieri();
        }

        // data, cliente e cantiere cambiano gli elenchi successivi: listener impostati dopo i primi valori
        data.addTextChangeListener(new CambioValore() {
            @Override
            public void afterTextChanged(Editable editable) {
                caricaCantieri();
            }
        });
        spinner_clienti.addTextChangeListener(new CambioValore() {
            @Override
            public void afterTextChanged(Editable editable) {
                if (!aggiornamentoInCorso) cambioCliente();
            }
        });
        spinner_cantieri.addTextChangeListener(new CambioValore() {
            @Override
            public void afterTextChanged(Editable editable) {
                if (!aggiornamentoInCorso) cambioCantiere();
            }
        });
    }

    /** Mostra il nome dell'autore nell'etichetta fissa (l'utente loggato, o l'autore del rapportino in modifica). */
    private void impostaOperatore(int idUtenteDitta) {
        String nome = null;
        if (idUtenteDitta == utente.idUtenteDitta) {
            nome = Sessione.getNomeOperatore();
        }
        if (nome == null || nome.trim().isEmpty()) {
            DbInterno db = new DbInterno(this);
            nome = RegoleRapportino.nomeOperatore(db, idUtenteDitta);
            db.close();
        }
        ((TextView) findViewById(R.id.text_operatore)).setText(nome.trim());
    }

    @Override
    protected void inizializzaModifica() {
        super.inizializzaModifica();
        DbInterno db = new DbInterno(this);
        ContentValues where = new ContentValues();
        where.put(Rapportini.ID_RAPPORTINO, getIDModifica());
        ContentValues val = db.getRecord(new Rapportini(), where);
        db.close();

        if (val != null) {
            data.setValue("" + val.getAsLong(Rapportini.DATA_RAPPORTINO));
            setText(R.id.editText_note, val.getAsString(Rapportini.NOTE));
            impostaOperatore(intero(val, Rapportini.ID_UTENTE_DITTA));

            idOrdineSalvato = intero(val, Rapportini.ID_ORDINE);
            idCantiereSalvato = intero(val, Rapportini.ID_CANTIERE);
            spinner_clienti.setValue("" + intero(val, Rapportini.ID_CLIENTE));
            spinner_cantieri.setValue("" + idCantiereSalvato);
            spinner_ordini.setValue("" + idOrdineSalvato);
        }

        caricaClienti();
        caricaCantieri();
        caricaDettaglio();
    }

    private long dataRapportino() {
        try {
            return Long.parseLong(data.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Clienti della ditta (l'app scarica solo quelli), piu' "nessun cliente". */
    private void caricaClienti() {
        ArrayList<Object> valori = new ArrayList<Object>();
        valori.add(voce("0", getString(R.string.nessun_cliente)));
        DbInterno db = new DbInterno(this);
        for (Object o : db.eseguiSelect("Select " + Anagrafica.ID_ANAGRAFICA + ", " + Anagrafica.RAGIONE_SOCIALE + " from "
                + Anagrafica.NOME_TABELLA + " order by " + Anagrafica.RAGIONE_SOCIALE, null)) {
            ContentValues c = (ContentValues) o;
            valori.add(voce("" + c.getAsInteger(Anagrafica.ID_ANAGRAFICA), c.getAsString(Anagrafica.RAGIONE_SOCIALE)));
        }
        db.close();
        aggiornamentoInCorso = true;
        impostaElenco(spinner_clienti, valori);
        aggiornamentoInCorso = false;
        clienteCaricato = spinner_clienti.getValue();
    }

    /** Cantieri ammessi per la data; se quello scelto non lo e' piu' la selezione si svuota. */
    private void caricaCantieri() {
        String d = data.getValue();
        if (!d.equals(dataCaricata)) {
            dataCaricata = d;
            DbInterno db = new DbInterno(this);
            cantieriAmmessi = RegoleRapportino.cantieriDisponibili(db, utente, dataRapportino(), idCantiereSalvato);
            db.close();
            if (cantieriAmmessi.isEmpty()) {
                Toast.makeText(this, getString(!utente.amministratore && utente.idUtenteDitta == 0
                        ? R.string.operatori_non_scaricati : R.string.nessun_cantiere_rapportino), Toast.LENGTH_LONG).show();
            }
        }
        mostraCantieri();
    }

    /** Cantieri ammessi del cliente scelto (tutti se nessun cliente), piu' "nessun cantiere". */
    private void mostraCantieri() {
        int cliente = valore(spinner_clienti);
        ArrayList<Object> valori = new ArrayList<Object>();
        valori.add(voce("0", getString(R.string.nessun_cantiere)));
        for (Object o : cantieriAmmessi) {
            ContentValues c = (ContentValues) o;
            if (cliente > 0 && intero(c, Cantieri.ID_ANAGRAFICA) != cliente) continue;
            String rs = c.getAsString(Anagrafica.RAGIONE_SOCIALE);
            valori.add(voce("" + c.getAsInteger(Cantieri.ID_CANTIERE), c.getAsString(Cantieri.NOME)
                    + (cliente == 0 && rs != null && !rs.trim().isEmpty() ? " - " + rs.trim() : "")));
        }
        aggiornamentoInCorso = true;
        impostaElenco(spinner_cantieri, valori);
        aggiornamentoInCorso = false;
        caricaOrdini();
    }

    /** Cambiando cliente il cantiere di un altro cliente non vale piu'. */
    private void cambioCliente() {
        if (spinner_clienti.getValue().equals(clienteCaricato)) return;
        clienteCaricato = spinner_clienti.getValue();
        mostraCantieri();
    }

    /** Scegliendo un cantiere il cliente diventa il suo (come sul server). */
    private void cambioCantiere() {
        int cantiere = valore(spinner_cantieri);
        if (cantiere > 0) {
            for (Object o : cantieriAmmessi) {
                ContentValues c = (ContentValues) o;
                int cliente = intero(c, Cantieri.ID_ANAGRAFICA);
                if (intero(c, Cantieri.ID_CANTIERE) == cantiere && cliente > 0 && cliente != valore(spinner_clienti)) {
                    aggiornamentoInCorso = true;
                    spinner_clienti.setValue("" + cliente);
                    caricaClienti();
                    aggiornamentoInCorso = false;
                    mostraCantieri();
                    return;
                }
            }
        }
        caricaOrdini();
    }

    /** Ordini aperti del cantiere scelto, piu' "nessun ordine" (l'ordine e' facoltativo e richiede il cantiere). */
    private void caricaOrdini() {
        String cantiere = spinner_cantieri.getValue();
        if (cantiere.equals(cantiereCaricato)) return;
        boolean primoCaricamento = cantiereCaricato == null;
        cantiereCaricato = cantiere;

        ArrayList<Object> valori = new ArrayList<Object>();
        valori.add(voce("0", getString(R.string.nessun_ordine)));

        ArrayList<Object> ordini = new ArrayList<Object>();
        if (valore(spinner_cantieri) > 0) {
            DbInterno db = new DbInterno(this);
            ordini = new Preventivi().getOrdiniCantiere(db, valore(spinner_cantieri), idOrdineSalvato);
            db.close();
        }
        for (Object o : ordini) {
            ContentValues ordine = (ContentValues) o;
            valori.add(voce("" + ordine.getAsInteger(Preventivi.ID_PREVENTIVO), "N. " + ordine.getAsInteger(Preventivi.NUMERO) + " del "
                    + Utility.numberToDataShort(ordine.getAsLong(Preventivi.DATA)) + " - " + ordine.getAsString(Preventivi.TITOLO)));
        }

        // cambiando cantiere l'ordine precedente non vale piu'; in un rapportino nuovo con un solo ordine aperto lo propongo
        if (!primoCaricamento || getModalita() == INSERIMENTO) {
            spinner_ordini.setValue(getModalita() == INSERIMENTO && ordini.size() == 1
                    ? ((ContentValues) valori.get(1)).getAsString(EConTabSpinner.VALORE) : "0");
        }
        spinner_ordini.setValoriSpinnerLibero(valori);
        spinner_ordini.setEnabled(!ordini.isEmpty());
    }

    @Override
    protected String eseguiInserimento(DbInterno db) {
        String errore = controllaTestata();
        if (errore != null) return errore;
        if (utente.idUtenteDitta == 0) return getString(R.string.operatori_non_scaricati);

        Rapportini tabella = new Rapportini();
        ContentValues val = tabella.getValoriLogInserimento(db);
        valoriTestata(val);
        val.put(Rapportini.ID_UTENTE_DITTA, utente.idUtenteDitta);

        tabella.inserisciRecord(db, val);
        getIntent().putExtra("ID", val.getAsInteger(Rapportini.ID_RAPPORTINO));
        setChiudiAlSalvataggio(false);
        return super.eseguiInserimento(db);
    }

    @Override
    protected String eseguiAggiornamento(DbInterno db) {
        String errore = controllaTestata();
        if (errore != null) return errore;

        Rapportini tabella = new Rapportini();
        ContentValues val = tabella.getValoriLogModifica(db);
        valoriTestata(val);
        // l'autore non si modifica: resta chi ha aperto il rapportino

        ContentValues where = new ContentValues();
        where.put(Rapportini.ID_RAPPORTINO, getIDModifica());
        tabella.aggiornaRecord(db, val, where);
        scollegaRigheAltroOrdine(db, val.getAsInteger(Rapportini.ID_ORDINE));
        return super.eseguiAggiornamento(db);
    }

    /** Righe imputate a righe di un ordine diverso da quello del rapportino (ordine tolto o cambiato): tornano extra ordine. */
    private void scollegaRigheAltroOrdine(DbInterno db, int idOrdine) {
        RapportiniDettaglio tab = new RapportiniDettaglio();
        ArrayList<Object> righe = db.eseguiSelect("Select " + RapportiniDettaglio.ID_RAPPORTINO_DETTAGLIO + " from " + RapportiniDettaglio.NOME_TABELLA
                + " where " + RapportiniDettaglio.ID_RAPPORTINO + "=" + getIDModifica() + " and coalesce(" + RapportiniDettaglio.ID_PREVENTIVO_DETTAGLIO + ",0)<>0"
                + " and " + RapportiniDettaglio.ID_PREVENTIVO_DETTAGLIO + " not in (select " + PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO + " from "
                + PreventiviDettaglio.NOME_TABELLA + " where " + PreventiviDettaglio.ID_PREVENTIVO + "=" + idOrdine + ")", null);
        for (Object o : righe) {
            ContentValues val = tab.getValoriLogModifica(db);
            val.put(RapportiniDettaglio.ID_PREVENTIVO_DETTAGLIO, 0);
            ContentValues where = new ContentValues();
            where.put(RapportiniDettaglio.ID_RAPPORTINO_DETTAGLIO, ((ContentValues) o).getAsInteger(RapportiniDettaglio.ID_RAPPORTINO_DETTAGLIO));
            tab.aggiornaRecord(db, val, where);
        }
    }

    private String controllaTestata() {
        if (valore(spinner_clienti) == 0 && valore(spinner_cantieri) == 0 && valore(spinner_ordini) == 0) {
            spinner_clienti.setError(getString(R.string.errore_riferimento_rapportino));
            return getString(R.string.errore_riferimento_rapportino);
        }
        spinner_clienti.setError(null);
        return null;
    }

    private void valoriTestata(ContentValues val) {
        val.put(Rapportini.DATA_RAPPORTINO, dataRapportino());
        val.put(Rapportini.ID_CLIENTE, valore(spinner_clienti));
        val.put(Rapportini.ID_CANTIERE, valore(spinner_cantieri));
        val.put(Rapportini.ID_ORDINE, valore(spinner_ordini));
        val.put(Rapportini.NOTE, getTesto(R.id.editText_note));
    }

    @Override
    protected void aggiornaDopoSalvataggio() {
        super.aggiornaDopoSalvataggio();
        setChiudiAlSalvataggio(true);
        Toast.makeText(this, getString(R.string.messaggio_salvataggio_eseguito), Toast.LENGTH_SHORT).show();
        setModalita(MODIFICA);
        // da qui il cantiere e l'ordine salvati restano sempre sceglibili, come per un rapportino aperto in modifica
        idCantiereSalvato = valore(spinner_cantieri);
        idOrdineSalvato = valore(spinner_ordini);
        findViewById(R.id.linear_lista_dett).setVisibility(View.VISIBLE);
        caricaDettaglio();
    }

    public void nuovoDettaglio(View v) {
        new PopupDettaglioRapportino(this, getIDModifica(), 0).apriPopup();
    }

    public void caricaDettaglio() {
        LinearLayout listaDett = (LinearLayout) findViewById(R.id.linear_lista_dett);
        listaDett.removeViews(1, listaDett.getChildCount() - 1);

        DbInterno db = new DbInterno(this);
        String rd = RapportiniDettaglio.NOME_TABELLA;
        String SQLDETT = "Select " + rd + ".*, pd." + PreventiviDettaglio.DESCRIZIONE + " as descrizione_riga_ordine, "
                + RapportiniDettaglioOperatori.sqlOreUomoRiga() + " as ore_uomo"
                + " from " + rd + " left join " + PreventiviDettaglio.NOME_TABELLA + " pd on pd." + PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO
                + "=" + rd + "." + RapportiniDettaglio.ID_PREVENTIVO_DETTAGLIO
                + " where " + rd + "." + RapportiniDettaglio.ID_RAPPORTINO + "=" + getIDModifica()
                + " order by " + RapportiniDettaglio.sqlOrdinamento(rd);
        ArrayList<Object> dettaglio = db.eseguiSelect(SQLDETT, null);

        for (int i = 0; i < dettaglio.size(); i++) {
            ContentValues curr = (ContentValues) dettaglio.get(i);
            View dett = View.inflate(this, R.layout.list_item_rapportino_dett, null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            ((TextView) dett.findViewById(R.id.tipo_manodopera)).setText(titoloRiga(this, curr));
            ((TextView) dett.findViewById(R.id.ore)).setText(getString(R.string.ore_uomo) + ": " + Utility.formatNumero(curr.getAsDouble("ore_uomo")));
            ((TextView) dett.findViewById(R.id.nota)).setText(sottotitoloRiga(curr));
            ((TextView) dett.findViewById(R.id.operatori)).setText(testoOperatori(db, curr));

            dett.setTag(curr.getAsInteger(RapportiniDettaglio.ID_RAPPORTINO_DETTAGLIO));
            dett.setOnClickListener(this);
            dett.setOnLongClickListener(this);
            listaDett.addView(dett, lp);
        }
        db.close();
    }

    /** "Viaggio andata 08:00-08:30" oppure "Posa canaline 08:30-12:30". */
    public static String titoloRiga(android.content.Context ctx, ContentValues riga) {
        String tipo = riga.getAsString(RapportiniDettaglio.TIPO);
        String titolo;
        if (RapportiniDettaglio.TIPO_VIAGGIO_ANDATA.equals(tipo)) {
            titolo = ctx.getString(R.string.tipo_viaggio_andata);
        } else if (RapportiniDettaglio.TIPO_VIAGGIO_RITORNO.equals(tipo)) {
            titolo = ctx.getString(R.string.tipo_viaggio_ritorno);
        } else {
            titolo = testo(riga, RapportiniDettaglio.DESCRIZIONE);
            if (titolo.isEmpty()) titolo = ctx.getString(R.string.tipo_lavoro);
        }
        String inizio = testo(riga, RapportiniDettaglio.ORA_INIZIO);
        String fine = testo(riga, RapportiniDettaglio.ORA_FINE);
        if (!inizio.isEmpty() || !fine.isEmpty()) {
            titolo += "  " + (inizio.isEmpty() ? "?" : inizio) + "-" + (fine.isEmpty() ? "?" : fine);
        }
        return titolo;
    }

    /** Descrizione del viaggio, riga d'ordine, "non a costo" e nota. */
    private String sottotitoloRiga(ContentValues riga) {
        StringBuilder sb = new StringBuilder();
        if (RapportiniDettaglio.isViaggio(riga.getAsString(RapportiniDettaglio.TIPO))) {
            aggiungi(sb, testo(riga, RapportiniDettaglio.DESCRIZIONE));
        }
        aggiungi(sb, testo(riga, "descrizione_riga_ordine"));
        if (intero(riga, RapportiniDettaglio.A_COSTO) == 0) {
            aggiungi(sb, getString(R.string.non_a_costo));
        }
        aggiungi(sb, testo(riga, RapportiniDettaglio.NOTE));
        return sb.toString();
    }

    private static void aggiungi(StringBuilder sb, String s) {
        if (s.isEmpty()) return;
        if (sb.length() > 0) sb.append(" · ");
        sb.append(s);
    }

    private static String testo(ContentValues cv, String campo) {
        String s = cv.getAsString(campo);
        return s != null ? s.trim() : "";
    }

    /** "Mario Rossi 4 · Luigi Bianchi 3,5", e per l'Amministratore ditta il costo della riga (0 se non a costo). */
    private String testoOperatori(DbInterno db, ContentValues riga) {
        boolean aCosto = intero(riga, RapportiniDettaglio.A_COSTO) != 0;
        double costo = 0;
        StringBuilder sb = new StringBuilder();
        for (Object o : RapportiniDettaglioOperatori.operatoriRiga(db, riga.getAsInteger(RapportiniDettaglio.ID_RAPPORTINO_DETTAGLIO))) {
            ContentValues op = (ContentValues) o;
            double ore = op.getAsDouble(RapportiniDettaglioOperatori.ORE);
            if (sb.length() > 0) sb.append(" · ");
            String nome = ((op.getAsString(RapportiniDettaglioOperatori.NOME_OPERATORE) != null ? op.getAsString(RapportiniDettaglioOperatori.NOME_OPERATORE) : "")
                    + " " + (op.getAsString(RapportiniDettaglioOperatori.COGNOME_OPERATORE) != null ? op.getAsString(RapportiniDettaglioOperatori.COGNOME_OPERATORE) : "")).trim();
            sb.append(nome.isEmpty() ? "#" + op.getAsInteger(RapportiniDettaglioOperatori.ID_UTENTE_DITTA) : nome).append(" ").append(Utility.formatNumero(ore));
            if (aCosto) costo += ore * RapportiniDettaglioOperatori.costoOrario(op);
        }
        if (utente.amministratore && sb.length() > 0) {
            sb.append("  —  ").append(getString(R.string.costo_euro)).append(" ").append(Utility.formatNumero(costo, 2));
        }
        return sb.toString();
    }

    @Override
    public boolean onLongClick(final View view) {
        String[] items = new String[] { getString(R.string.modifica), getString(R.string.elimina) };
        Utility.mostraSelezioneDialog(getString(R.string.dettaglio_rapportino), items, this, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    apriDettaglio(view);
                }
                if (which == 1) {
                    eliminaDettaglio(view);
                }
            }
        });
        return true;
    }

    private void eliminaDettaglio(final View view) {
        Utility.mostraConfermaCancellazioneDialog(this, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    DbInterno db = new DbInterno(RapportinoDettaglioModActivity.this);
                    ContentValues valDel = new ContentValues();
                    valDel.put(RapportiniDettaglio.ID_RAPPORTINO_DETTAGLIO, (Integer) view.getTag());
                    new RapportiniDettaglio().cancellaRecord(db, valDel);
                    db.close();
                    caricaDettaglio();
                }
            }
        });
    }

    private void apriDettaglio(View view) {
        new PopupDettaglioRapportino(this, getIDModifica(), (Integer) view.getTag()).apriPopup();
    }

    @Override
    public void onClick(View view) {
        apriDettaglio(view);
    }

    private static int intero(ContentValues cv, String campo) {
        Integer v = cv.getAsInteger(campo);
        return v != null ? v : 0;
    }

    /** Valore numerico di uno spinner: 0 se vuoto o "nessuno". */
    private static int valore(EConTabSpinner spinner) {
        try {
            return Integer.parseInt(spinner.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Imposta l'elenco; se il valore scelto non c'e' (piu') si passa alla prima voce, "nessuno". */
    private static void impostaElenco(EConTabSpinner spinner, ArrayList<Object> valori) {
        boolean presente = false;
        for (Object v : valori) {
            if (((ContentValues) v).getAsString(EConTabSpinner.VALORE).equals(spinner.getValue())) presente = true;
        }
        if (!presente) {
            spinner.setValue(((ContentValues) valori.get(0)).getAsString(EConTabSpinner.VALORE));
        }
        spinner.setValoriSpinnerLibero(valori);
    }

    private static ContentValues voce(String valore, String descrizione) {
        ContentValues v = new ContentValues();
        v.put(EConTabSpinner.VALORE, valore);
        v.put(EConTabSpinner.DESCRIZIONE, descrizione);
        return v;
    }

    /** TextWatcher con i soli metodi che servono qui. */
    private abstract static class CambioValore implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
