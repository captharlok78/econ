package pfa.app.econtab.utils;

import android.content.ContentValues;
import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.PianificazioneAssegnazioni;
import pfa.app.econtab.db.table.PianificazioneEsclusioni;
import pfa.app.econtab.db.table.Squadre;
import pfa.app.econtab.db.table.SquadreMembri;
import pfa.app.econtab.db.table.StatiDocumento;
import pfa.app.econtab.db.table.StatiDocumentoTransizioni;
import pfa.app.econtab.db.table.Utenti;

/**
 * Chi puo' fare un rapportino su quale cantiere e con quali operatori, a partire dalla pianificazione scaricata
 * dal server (squadre, membri, blocchi di pianificazione ed esclusioni). Il periodo e' la settimana (lunedi-domenica)
 * della data del rapportino.
 * <ul>
 * <li>Amministratore ditta: qualsiasi cantiere e qualsiasi operatore della ditta.</li>
 * <li>Gli altri: solo i cantieri su cui sono pianificati nella settimana (blocco personale, oppure blocco di una
 * squadra attiva di cui sono coordinatore o membro, senza esserne esclusi).</li>
 * <li>Nelle righe il coordinatore sceglie se stesso e i membri delle squadre attive che coordina e che sono
 * pianificate su quel cantiere nella settimana; ogni altro operatore mette ore solo su se stesso.</li>
 * <li>Rapportino senza cantiere (solo cliente, §9.6.1 dello studio): il coordinatore sceglie se stesso e i membri di
 * tutte le squadre attive che coordina, gli altri solo se stessi.</li>
 * </ul>
 * Le regole valgono solo nell'app: il server controlla soltanto che cantiere e operatori siano della ditta.
 */
public final class RegoleRapportino {

    /** Colonna aggiunta agli operatori selezionabili: 1 se pianificato sul cantiere nella settimana. */
    public static final String PIANIFICATO = "pianificato";

    private RegoleRapportino() {}

    /** Utente collegato, visto come operatore della ditta. */
    public static final class Utente {
        /** 0 se l'elenco operatori non e' ancora stato scaricato (serve una sincronizzazione). */
        public final int idUtenteDitta;
        public final boolean amministratore;

        Utente(int idUtenteDitta, boolean amministratore) {
            this.idUtenteDitta = idUtenteDitta;
            this.amministratore = amministratore;
        }
    }

    public static Utente utenteCorrente(DbInterno db, Context ctx) {
        ContentValues ut = db.getRecord("Select * from " + Utenti.NOME_TABELLA + " where " + Utenti.ID_UTENTE + "="
                + Sessione.getIdOperatore(ctx));
        if (ut == null) {
            return new Utente(0, false);
        }
        return new Utente(intero(ut, Utenti.ID_UTENTE_DITTA), intero(ut, Utenti.AMMINISTRATORE) == 1);
    }

    /** Primo e ultimo istante (yyyyMMddHHmmss) della settimana, da lunedi a domenica, che contiene la data. */
    public static long[] settimana(long data) {
        Calendar c;
        String s = String.valueOf(data);
        if (s.length() >= 8) {
            c = new GregorianCalendar(Integer.parseInt(s.substring(0, 4)), Integer.parseInt(s.substring(4, 6)) - 1,
                    Integer.parseInt(s.substring(6, 8)));
        } else {
            c = Calendar.getInstance();
        }
        int daLunedi = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7; // lunedi 0 ... domenica 6
        c.add(Calendar.DATE, -daLunedi);
        long inizio = giorno(c) * 1000000L;
        c.add(Calendar.DATE, 6);
        long fine = giorno(c) * 1000000L + 235959L;
        return new long[] { inizio, fine };
    }

    private static long giorno(Calendar c) {
        return c.get(Calendar.YEAR) * 10000L + (c.get(Calendar.MONTH) + 1) * 100L + c.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Coppie (id_cantiere, id_utente_ditta) pianificate nella settimana: blocchi personali e, per i blocchi di
     * squadra attiva, coordinatore e membri non esclusi da quel blocco.
     */
    private static String sqlPianificati(long[] settimana) {
        String pa = PianificazioneAssegnazioni.NOME_TABELLA;
        String filtro = " where pa." + PianificazioneAssegnazioni.ID_DITTA + "=" + Sessione.getDittaSelezionata()
                + " and pa." + PianificazioneAssegnazioni.DATA + " between " + settimana[0] + " and " + settimana[1];
        String squadra = " inner join " + Squadre.NOME_TABELLA + " s on s." + Squadre.ID_SQUADRA + "=pa."
                + PianificazioneAssegnazioni.ID_SQUADRA;
        String nonEscluso = " and not exists (select 1 from " + PianificazioneEsclusioni.NOME_TABELLA + " e where e."
                + PianificazioneEsclusioni.ID_ASSEGNAZIONE + "=pa." + PianificazioneAssegnazioni.ID + " and e."
                + PianificazioneEsclusioni.ID_UTENTE_DITTA + "=%s)";
        return "select pa." + PianificazioneAssegnazioni.ID_CANTIERE + " as id_cantiere, pa."
                + PianificazioneAssegnazioni.ID_UTENTE_DITTA + " as id_utente_ditta from " + pa + " pa" + filtro
                + " and pa." + PianificazioneAssegnazioni.ID_UTENTE_DITTA + ">0"
                + " union select pa." + PianificazioneAssegnazioni.ID_CANTIERE + ", s." + Squadre.ID_COORDINATORE
                + " from " + pa + " pa" + squadra + filtro + " and s." + Squadre.ATTIVA + "=1"
                + String.format(nonEscluso, "s." + Squadre.ID_COORDINATORE)
                + " union select pa." + PianificazioneAssegnazioni.ID_CANTIERE + ", m." + SquadreMembri.ID_UTENTE_DITTA
                + " from " + pa + " pa" + squadra + " inner join " + SquadreMembri.NOME_TABELLA + " m on m."
                + SquadreMembri.ID_SQUADRA + "=s." + Squadre.ID_SQUADRA + filtro + " and s." + Squadre.ATTIVA + "=1"
                + String.format(nonEscluso, "m." + SquadreMembri.ID_UTENTE_DITTA);
    }

    /**
     * Cantieri su cui l'utente puo' fare un rapportino in quella data (id_cantiere, nome, id_anagrafica e
     * ragione_sociale del cliente). idCantiereSempreIncluso (0 = nessuno) resta in elenco comunque, per non perdere il cantiere di un
     * rapportino gia' esistente.
     */
    public static ArrayList<Object> cantieriDisponibili(DbInterno db, Utente utente, long data, int idCantiereSempreIncluso) {
        String filtro = "";
        if (!utente.amministratore) {
            filtro = " and (" + Cantieri.NOME_TABELLA + "." + Cantieri.ID_CANTIERE + " in (select id_cantiere from ("
                    + sqlPianificati(settimana(data)) + ") where id_utente_ditta=" + utente.idUtenteDitta + ")"
                    + " or " + Cantieri.NOME_TABELLA + "." + Cantieri.ID_CANTIERE + "=" + idCantiereSempreIncluso + ")";
        }
        String sql = "Select " + Cantieri.NOME_TABELLA + "." + Cantieri.ID_CANTIERE + ", " + Cantieri.NOME_TABELLA + "."
                + Cantieri.NOME + ", " + Cantieri.NOME_TABELLA + "." + Cantieri.ID_ANAGRAFICA + ", "
                + Anagrafica.NOME_TABELLA + "." + Anagrafica.RAGIONE_SOCIALE
                + " from " + Cantieri.NOME_TABELLA + " left join " + Anagrafica.NOME_TABELLA + " on "
                + Anagrafica.NOME_TABELLA + "." + Anagrafica.ID_ANAGRAFICA + "=" + Cantieri.NOME_TABELLA + "." + Cantieri.ID_ANAGRAFICA
                + " where " + Cantieri.NOME_TABELLA + "." + Cantieri.ID_DITTA + "=" + Sessione.getDittaSelezionata() + filtro
                + " order by " + Cantieri.NOME_TABELLA + "." + Cantieri.NOME;
        return db.eseguiSelect(sql, null);
    }

    /**
     * Operatori che l'utente puo' mettere su una riga di un rapportino di quel cantiere e di quella data (colonne di
     * utenti piu' PIANIFICATO): prima i pianificati sul cantiere nella settimana, poi gli altri, per cognome e nome.
     */
    public static ArrayList<ContentValues> operatoriSelezionabili(DbInterno db, Utente utente, int idCantiere, long data) {
        long[] sett = settimana(data);
        Set<Integer> pianificati = interi(db, "select id_utente_ditta from (" + sqlPianificati(sett) + ") where id_cantiere=" + idCantiere);

        Set<Integer> ammessi = null; // null = tutti (amministratore)
        if (!utente.amministratore) {
            ammessi = new HashSet<Integer>();
            ammessi.add(utente.idUtenteDitta);
            ammessi.addAll(interi(db, "select m." + SquadreMembri.ID_UTENTE_DITTA + " from " + SquadreMembri.NOME_TABELLA + " m"
                    + " inner join " + Squadre.NOME_TABELLA + " s on s." + Squadre.ID_SQUADRA + "=m." + SquadreMembri.ID_SQUADRA
                    + " where s." + Squadre.ID_COORDINATORE + "=" + utente.idUtenteDitta + " and s." + Squadre.ATTIVA + "=1"
                    + " and s." + Squadre.ID_DITTA + "=" + Sessione.getDittaSelezionata()
                    + (idCantiere <= 0 ? "" // senza cantiere: tutte le squadre attive che coordina
                    : " and exists (select 1 from " + PianificazioneAssegnazioni.NOME_TABELLA + " pa where pa."
                    + PianificazioneAssegnazioni.ID_SQUADRA + "=s." + Squadre.ID_SQUADRA + " and pa."
                    + PianificazioneAssegnazioni.ID_CANTIERE + "=" + idCantiere + " and pa." + PianificazioneAssegnazioni.DATA
                    + " between " + sett[0] + " and " + sett[1] + ")")));
        }

        ArrayList<ContentValues> primi = new ArrayList<ContentValues>();
        ArrayList<ContentValues> altri = new ArrayList<ContentValues>();
        for (Object o : db.eseguiSelect("Select * from " + Utenti.NOME_TABELLA + " where " + Utenti.ID_UTENTE_DITTA + ">0"
                + " order by " + Utenti.COGNOME + ", " + Utenti.NOME, null)) {
            ContentValues ut = (ContentValues) o;
            int id = intero(ut, Utenti.ID_UTENTE_DITTA);
            if (ammessi != null && !ammessi.contains(id)) {
                continue;
            }
            boolean pianificato = pianificati.contains(id);
            ut.put(PIANIFICATO, pianificato ? 1 : 0);
            (pianificato ? primi : altri).add(ut);
        }
        primi.addAll(altri);
        return primi;
    }

    /**
     * Blocchi di pianificazione dell'utente in un giorno (dashboard della prima schermata): personali, o di una squadra
     * attiva di cui e' coordinatore o membro, se non ne e' escluso. Colonne: fascia (M/P), minuti, nome_cantiere,
     * cliente, squadra (vuota per i blocchi personali), indirizzo, cap, citta, provincia del cantiere; mattina prima
     * del pomeriggio.
     */
    public static ArrayList<Object> pianificazioneGiorno(DbInterno db, Utente utente, long data) {
        if (utente.idUtenteDitta == 0) {
            return new ArrayList<Object>();
        }
        long giorno = data / 1000000L;
        int u = utente.idUtenteDitta;
        String sql = "select pa." + PianificazioneAssegnazioni.FASCIA + " as fascia, pa." + PianificazioneAssegnazioni.MINUTI + " as minuti,"
                + " c." + Cantieri.NOME + " as nome_cantiere, a." + Anagrafica.RAGIONE_SOCIALE + " as cliente, s." + Squadre.NOME + " as squadra,"
                + " c." + Cantieri.INDIRIZZO + " as indirizzo, c." + Cantieri.CAP + " as cap, c." + Cantieri.CITTA + " as citta, c." + Cantieri.PROVINCIA + " as provincia"
                + " from " + PianificazioneAssegnazioni.NOME_TABELLA + " pa"
                + " inner join " + Cantieri.NOME_TABELLA + " c on c." + Cantieri.ID_CANTIERE + "=pa." + PianificazioneAssegnazioni.ID_CANTIERE
                + " left join " + Anagrafica.NOME_TABELLA + " a on a." + Anagrafica.ID_ANAGRAFICA + "=c." + Cantieri.ID_ANAGRAFICA
                + " left join " + Squadre.NOME_TABELLA + " s on s." + Squadre.ID_SQUADRA + "=pa." + PianificazioneAssegnazioni.ID_SQUADRA
                + " where pa." + PianificazioneAssegnazioni.ID_DITTA + "=" + Sessione.getDittaSelezionata()
                + " and pa." + PianificazioneAssegnazioni.DATA + " between " + (giorno * 1000000L) + " and " + (giorno * 1000000L + 235959L)
                + " and (pa." + PianificazioneAssegnazioni.ID_UTENTE_DITTA + "=" + u
                + " or (s." + Squadre.ATTIVA + "=1 and (s." + Squadre.ID_COORDINATORE + "=" + u
                + " or exists (select 1 from " + SquadreMembri.NOME_TABELLA + " m where m." + SquadreMembri.ID_SQUADRA + "=s." + Squadre.ID_SQUADRA
                + " and m." + SquadreMembri.ID_UTENTE_DITTA + "=" + u + "))"
                + " and not exists (select 1 from " + PianificazioneEsclusioni.NOME_TABELLA + " e where e." + PianificazioneEsclusioni.ID_ASSEGNAZIONE
                + "=pa." + PianificazioneAssegnazioni.ID + " and e." + PianificazioneEsclusioni.ID_UTENTE_DITTA + "=" + u + ")))"
                + " order by pa." + PianificazioneAssegnazioni.FASCIA + ", c." + Cantieri.NOME;
        return db.eseguiSelect(sql, null);
    }

    private static Set<Integer> interi(DbInterno db, String sql) {
        Set<Integer> ris = new HashSet<Integer>();
        for (Object o : db.eseguiSelect(sql, null)) {
            ContentValues cv = (ContentValues) o;
            for (String k : cv.keySet()) {
                Integer v = cv.getAsInteger(k);
                if (v != null) ris.add(v);
            }
        }
        return ris;
    }

    private static int intero(ContentValues cv, String campo) {
        Integer v = cv.getAsInteger(campo);
        return v != null ? v : 0;
    }

    // ── Prezzi e stati (GESTIONE_RAPPORTINI.md §9 e §10) ────────────────────────────────────────────────────────

    /**
     * Prezzo e "a costo" delle righe: li vedono e li impostano solo l'amministratore, i coordinatori di una squadra attiva
     * e l'unico operatore della ditta (come PermessiRapportini::vedePrezzi() del server, che scarta i valori degli altri).
     */
    public static boolean vedePrezzi(DbInterno db, Utente utente) {
        if (utente.amministratore) return true;
        if (utente.idUtenteDitta == 0) return false;
        if (!interi(db, "select 1 from " + Squadre.NOME_TABELLA + " where " + Squadre.ID_COORDINATORE + "=" + utente.idUtenteDitta
                + " and " + Squadre.ATTIVA + "=1 and " + Squadre.ID_DITTA + "=" + Sessione.getDittaSelezionata()).isEmpty()) {
            return true;
        }
        return interi(db, "select " + Utenti.ID_UTENTE_DITTA + " from " + Utenti.NOME_TABELLA + " where " + Utenti.ID_UTENTE_DITTA + ">0").size() == 1;
    }

    /**
     * Colore del badge di uno stato: sul server e' un colore Bootstrap (primary, success...) oppure un codice #rrggbb;
     * vuoto o sconosciuto = grigio.
     */
    public static int coloreStato(String colore) {
        String c = colore != null ? colore.trim().toLowerCase(java.util.Locale.ROOT) : "";
        switch (c) {
            case "primary": return android.graphics.Color.parseColor("#0D6EFD");
            case "secondary": return android.graphics.Color.parseColor("#6C757D");
            case "success": return android.graphics.Color.parseColor("#198754");
            case "info": return android.graphics.Color.parseColor("#0AA2C0");
            case "warning": return android.graphics.Color.parseColor("#E0A800");
            case "danger": return android.graphics.Color.parseColor("#DC3545");
            case "dark": return android.graphics.Color.parseColor("#212529");
        }
        try {
            return android.graphics.Color.parseColor(c.startsWith("#") ? c : "#" + c);
        } catch (IllegalArgumentException e) {
            return android.graphics.Color.parseColor("#6C757D");
        }
    }

    /** Stato con quell'id (null se non c'e': stati non ancora scaricati, o documento vecchio senza stato). */
    public static ContentValues stato(DbInterno db, int idStato) {
        return idStato > 0 ? db.getRecord("select * from " + StatiDocumento.NOME_TABELLA + " where " + StatiDocumento.ID + "=" + idStato) : null;
    }

    /** Stato iniziale dei rapportini (null se gli stati non sono ancora scaricati: il server lo assegna all'invio). */
    public static ContentValues statoIniziale(DbInterno db) {
        return db.getRecord("select * from " + StatiDocumento.NOME_TABELLA + " where " + StatiDocumento.AMBITO + "='"
                + StatiDocumento.AMBITO_RAPPORTINI + "' and " + StatiDocumento.INIZIALE + "=1");
    }

    public static ContentValues statoPerCodice(DbInterno db, String codice) {
        return db.getRecord("select * from " + StatiDocumento.NOME_TABELLA + " where " + StatiDocumento.AMBITO + "='"
                + StatiDocumento.AMBITO_RAPPORTINI + "' and " + StatiDocumento.CODICE + "='" + codice + "'");
    }

    /** Senza stato (non ancora scaricato) il rapportino si tratta come in bozza. */
    public static boolean isModificabile(ContentValues stato) {
        return stato == null || intero(stato, StatiDocumento.MODIFICABILE) == 1;
    }

    public static boolean isCancellabile(ContentValues stato) {
        return stato == null || intero(stato, StatiDocumento.CANCELLABILE) == 1;
    }

    /** Passaggio ammesso tra due stati (quelli riservati all'amministratore solo per lui). */
    public static boolean passaggioAmmesso(DbInterno db, int idStatoDa, int idStatoA, Utente utente) {
        if (idStatoDa == idStatoA) return true;
        if (idStatoDa <= 0) { // documento nuovo o senza stato: dall'iniziale
            ContentValues iniziale = statoIniziale(db);
            if (iniziale == null) return false;
            idStatoDa = intero(iniziale, StatiDocumento.ID);
            if (idStatoDa == idStatoA) return true;
        }
        return !interi(db, "select 1 from " + StatiDocumentoTransizioni.NOME_TABELLA + " where " + StatiDocumentoTransizioni.ID_STATO_DA
                + "=" + idStatoDa + " and " + StatiDocumentoTransizioni.ID_STATO_A + "=" + idStatoA
                + (utente.amministratore ? "" : " and coalesce(" + StatiDocumentoTransizioni.SOLO_AMMINISTRATORE + ",0)=0")).isEmpty();
    }

    /** Nome dell'operatore con quell'id_utente_ditta, "" se non e' tra gli operatori scaricati. */
    public static String nomeOperatore(DbInterno db, int idUtenteDitta) {
        ContentValues ut = db.getRecord("Select * from " + Utenti.NOME_TABELLA + " where " + Utenti.ID_UTENTE_DITTA + "=" + idUtenteDitta);
        return ut != null ? nome(ut) : "";
    }

    /** Nome da mostrare per un operatore (riga di utenti). */
    public static String nome(ContentValues ut) {
        String n = (ut.getAsString(Utenti.NOME) != null ? ut.getAsString(Utenti.NOME) : "") + " "
                + (ut.getAsString(Utenti.COGNOME) != null ? ut.getAsString(Utenti.COGNOME) : "");
        return n.trim();
    }
}
