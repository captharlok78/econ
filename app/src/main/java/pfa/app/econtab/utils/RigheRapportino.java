package pfa.app.econtab.utils;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Listini;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.db.table.RapportiniDettaglioTipi;
import pfa.app.econtab.db.table.UnitaMisura;
import pfa.app.econtab.db.table.Utenti;

/**
 * Righe dei rapportini (schema 26, GESTIONE_RAPPORTINI.md §9 e §12) con i dati collegati che servono a maschera ed export:
 * tipo (codice, descrizione, categoria), unita' di misura, utente della manodopera, articolo del listino, riga d'ordine.
 * Come sul server nessuna descrizione e' salvata e i costi si calcolano al momento (costo orario dell'utente, prezzo di
 * acquisto del listino).
 */
public final class RigheRapportino {

    // colonne aggiunte alla riga dalle join (o da completa())
    public static final String CODICE_TIPO = "codice_tipo";
    public static final String DESCRIZIONE_TIPO = "descrizione_tipo";
    public static final String CATEGORIA = "categoria";
    public static final String ORDINE_TIPO = "ordine_tipo";
    public static final String CODICE_UNITA = "codice_unita";
    /** Descrizione dell'unita' di misura (es. "Ore"), per le stampe. */
    public static final String DESCRIZIONE_UNITA = "descrizione_unita";
    public static final String MINUTI_PER_UNITA = "minuti_per_unita";
    public static final String NOME_UTENTE = "nome_utente";
    public static final String COSTO_ORARIO = "costo_orario";
    public static final String CODICE_ARTICOLO = "codice_articolo";
    public static final String DESCRIZIONE_ARTICOLO = "descrizione_articolo";
    public static final String PREZZO_ACQUISTO = "prz_ultimo_acquisto";
    public static final String DESCRIZIONE_RIGA_ORDINE = "descrizione_riga_ordine";

    private RigheRapportino() {}

    /**
     * Select delle righe (alias d) con i dati collegati; where e order by con gli alias d (righe), t (tipi), u (unita'),
     * ut (utenti), l (listini), pd (righe d'ordine); rapportini, se serve, si aggiunge con join extra.
     */
    public static String sqlRighe(String joinExtra, String where, String orderBy) {
        return "select d.*, t." + RapportiniDettaglioTipi.CODICE + " as " + CODICE_TIPO + ", t." + RapportiniDettaglioTipi.DESCRIZIONE
                + " as " + DESCRIZIONE_TIPO + ", t." + RapportiniDettaglioTipi.CATEGORIA + " as " + CATEGORIA + ", t."
                + RapportiniDettaglioTipi.ORDINE + " as " + ORDINE_TIPO + ", u." + UnitaMisura.UNITA_MISURA + " as " + CODICE_UNITA
                + ", u." + UnitaMisura.NOME + " as " + DESCRIZIONE_UNITA
                + ", u." + UnitaMisura.MINUTI_PER_UNITA + " as " + MINUTI_PER_UNITA
                + ", trim(coalesce(ut." + Utenti.NOME + ",'') || ' ' || coalesce(ut." + Utenti.COGNOME + ",'')) as " + NOME_UTENTE
                + ", ut." + Utenti.COSTO_ORARIO + " as " + COSTO_ORARIO
                + ", l." + Listini.CODICE_ARTICOLO + " as " + CODICE_ARTICOLO + ", l." + Listini.DESCRIZIONE + " as " + DESCRIZIONE_ARTICOLO
                + ", l." + Listini.PRZ_ULTIMO_ACQUISTO + " as " + PREZZO_ACQUISTO
                + ", pd." + PreventiviDettaglio.DESCRIZIONE + " as " + DESCRIZIONE_RIGA_ORDINE
                + " from " + RapportiniDettaglio.NOME_TABELLA + " d"
                + " left join " + RapportiniDettaglioTipi.NOME_TABELLA + " t on t." + RapportiniDettaglioTipi.ID + "=d." + RapportiniDettaglio.ID_TIPO
                + " left join " + UnitaMisura.NOME_TABELLA + " u on u." + UnitaMisura.ID + "=d." + RapportiniDettaglio.ID_UNITA_MISURA
                + " left join " + Utenti.NOME_TABELLA + " ut on ut." + Utenti.ID_UTENTE_DITTA + "=d." + RapportiniDettaglio.ID_UTENTE_MANODOPERA
                + " and d." + RapportiniDettaglio.ID_UTENTE_MANODOPERA + ">0"
                + " left join " + Listini.NOME_TABELLA + " l on l." + Listini.ID + "=d." + RapportiniDettaglio.ID_LISTINO
                + " and d." + RapportiniDettaglio.ID_LISTINO + "<>0"
                + " left join " + PreventiviDettaglio.NOME_TABELLA + " pd on pd." + PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO
                + "=d." + RapportiniDettaglio.ID_PREVENTIVO_DETTAGLIO + " and d." + RapportiniDettaglio.ID_PREVENTIVO_DETTAGLIO + "<>0"
                + (joinExtra != null ? " " + joinExtra : "")
                + (where != null ? " where " + where : "")
                + (orderBy != null ? " order by " + orderBy : "");
    }

    /** Ordine della scheda: ordine del tipo, ora di inizio (senza orario in fondo al gruppo), inserimento. */
    public static final String ORDINAMENTO = "t." + RapportiniDettaglioTipi.ORDINE + ", coalesce(d." + RapportiniDettaglio.ORA_INIZIO
            + ",'')='', d." + RapportiniDettaglio.ORA_INIZIO + ", d." + RapportiniDettaglio.ID + "<0, abs(d." + RapportiniDettaglio.ID + ")";

    /** Righe di un rapportino, nell'ordine della scheda. */
    public static ArrayList<ContentValues> righe(DbInterno db, int idRapportino) {
        ArrayList<ContentValues> ris = new ArrayList<ContentValues>();
        for (Object o : db.eseguiSelect(sqlRighe(null, "d." + RapportiniDettaglio.ID_RAPPORTINO + "=" + idRapportino, ORDINAMENTO), null)) {
            ris.add((ContentValues) o);
        }
        return ris;
    }

    /** Riempie (o aggiorna) i dati collegati di una riga tenuta in memoria, come farebbe sqlRighe(). */
    public static void completa(DbInterno db, ContentValues riga) {
        ContentValues t = db.getRecord("select * from " + RapportiniDettaglioTipi.NOME_TABELLA + " where " + RapportiniDettaglioTipi.ID
                + "=" + intero(riga, RapportiniDettaglio.ID_TIPO));
        riga.put(CODICE_TIPO, t != null ? t.getAsString(RapportiniDettaglioTipi.CODICE) : "");
        riga.put(DESCRIZIONE_TIPO, t != null ? t.getAsString(RapportiniDettaglioTipi.DESCRIZIONE) : "");
        riga.put(CATEGORIA, t != null ? t.getAsString(RapportiniDettaglioTipi.CATEGORIA) : "");
        riga.put(ORDINE_TIPO, t != null ? t.getAsInteger(RapportiniDettaglioTipi.ORDINE) : 0);

        ContentValues u = db.getRecord("select * from " + UnitaMisura.NOME_TABELLA + " where " + UnitaMisura.ID + "="
                + intero(riga, RapportiniDettaglio.ID_UNITA_MISURA));
        riga.put(CODICE_UNITA, u != null ? u.getAsString(UnitaMisura.UNITA_MISURA) : "");
        riga.put(DESCRIZIONE_UNITA, u != null ? u.getAsString(UnitaMisura.NOME) : "");
        riga.put(MINUTI_PER_UNITA, u != null && u.getAsInteger(UnitaMisura.MINUTI_PER_UNITA) != null ? u.getAsInteger(UnitaMisura.MINUTI_PER_UNITA) : 0);

        int idUtente = intero(riga, RapportiniDettaglio.ID_UTENTE_MANODOPERA);
        ContentValues ut = idUtente > 0 ? db.getRecord("select * from " + Utenti.NOME_TABELLA + " where " + Utenti.ID_UTENTE_DITTA + "=" + idUtente) : null;
        riga.put(NOME_UTENTE, ut != null ? RegoleRapportino.nome(ut) : "");
        if (ut != null && ut.getAsDouble(Utenti.COSTO_ORARIO) != null) {
            riga.put(COSTO_ORARIO, ut.getAsDouble(Utenti.COSTO_ORARIO));
        } else {
            riga.putNull(COSTO_ORARIO);
        }

        int idListino = intero(riga, RapportiniDettaglio.ID_LISTINO);
        // anche negativo: articolo nuovo della ditta non ancora inviato al server (§14)
        ContentValues l = idListino != 0 ? db.getRecord("select * from " + Listini.NOME_TABELLA + " where " + Listini.ID + "=" + idListino) : null;
        riga.put(CODICE_ARTICOLO, l != null ? l.getAsString(Listini.CODICE_ARTICOLO) : "");
        riga.put(DESCRIZIONE_ARTICOLO, l != null ? l.getAsString(Listini.DESCRIZIONE) : "");
        if (l != null && l.getAsDouble(Listini.PRZ_ULTIMO_ACQUISTO) != null) {
            riga.put(PREZZO_ACQUISTO, l.getAsDouble(Listini.PRZ_ULTIMO_ACQUISTO));
        } else {
            riga.putNull(PREZZO_ACQUISTO);
        }

        int idRigaOrdine = intero(riga, RapportiniDettaglio.ID_PREVENTIVO_DETTAGLIO);
        ContentValues pd = idRigaOrdine != 0 ? db.getRecord("select * from " + PreventiviDettaglio.NOME_TABELLA + " where "
                + PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO + "=" + idRigaOrdine) : null;
        riga.put(DESCRIZIONE_RIGA_ORDINE, pd != null ? pd.getAsString(PreventiviDettaglio.DESCRIZIONE) : "");
    }

    public static boolean isMateriale(ContentValues riga) {
        return RapportiniDettaglioTipi.CATEGORIA_MATERIALE.equals(riga.getAsString(CATEGORIA));
    }

    public static boolean isViaggio(ContentValues riga) {
        return RapportiniDettaglioTipi.CATEGORIA_VIAGGIO.equals(riga.getAsString(CATEGORIA));
    }

    /** Ore-uomo: quantita' convertita in ore (0 per il materiale o per unita' che non sono tempo). */
    public static double oreUomo(ContentValues riga) {
        if (isMateriale(riga)) return 0;
        return decimale(riga, RapportiniDettaglio.QUANTITA) * intero(riga, MINUTI_PER_UNITA) / 60.0;
    }

    /** Costo unitario attuale: orario dell'utente, o prezzo di acquisto del listino per il materiale. */
    public static double costoUnitario(ContentValues riga) {
        return isMateriale(riga) ? decimale(riga, PREZZO_ACQUISTO) : decimale(riga, COSTO_ORARIO);
    }

    /** Costo interno calcolato al momento (come RapportiniDettaglio::getCosto() del server). */
    public static double costo(ContentValues riga) {
        return isMateriale(riga) ? decimale(riga, RapportiniDettaglio.QUANTITA) * costoUnitario(riga) : oreUomo(riga) * costoUnitario(riga);
    }

    /** Articolo come "codice descrizione" (vuoto se la riga non ha articolo). */
    public static String articolo(ContentValues riga) {
        return (testo(riga, CODICE_ARTICOLO) + " " + testo(riga, DESCRIZIONE_ARTICOLO)).trim();
    }

    /** Orari "08:00–12:30" (vuoto se la riga non ne ha). */
    public static String orari(ContentValues riga) {
        String inizio = testo(riga, RapportiniDettaglio.ORA_INIZIO);
        String fine = testo(riga, RapportiniDettaglio.ORA_FINE);
        if (inizio.isEmpty() && fine.isEmpty()) return "";
        return (inizio.isEmpty() ? "?" : inizio) + "–" + (fine.isEmpty() ? "?" : fine);
    }

    /** Descrizione ricavata dal contenuto: tipo, articolo o utente, orari (come getDescrizioneRiga() del server). */
    public static String descrizione(ContentValues riga) {
        StringBuilder sb = new StringBuilder();
        aggiungi(sb, testo(riga, DESCRIZIONE_TIPO));
        aggiungi(sb, isMateriale(riga) ? articolo(riga) : testo(riga, NOME_UTENTE));
        aggiungi(sb, orari(riga));
        return sb.toString();
    }

    private static void aggiungi(StringBuilder sb, String s) {
        if (s.isEmpty()) return;
        if (sb.length() > 0) sb.append(" · ");
        sb.append(s);
    }

    public static String testo(ContentValues cv, String campo) {
        String s = cv.getAsString(campo);
        return s != null ? s.trim() : "";
    }

    public static int intero(ContentValues cv, String campo) {
        Integer v = cv.getAsInteger(campo);
        return v != null ? v : 0;
    }

    public static double decimale(ContentValues cv, String campo) {
        Double v = cv.getAsDouble(campo);
        return v != null ? v : 0;
    }
}
