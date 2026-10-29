package com.ncfsistemi.econ.utils;

import android.content.ContentValues;
import android.content.Context;

import com.ncfsistemi.econ.api.TokenManager;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Assistenze;
import com.ncfsistemi.econ.db.table.AssistenzeControlli;
import com.ncfsistemi.econ.db.table.DitteArticoliAssistenza;
import com.ncfsistemi.econ.db.table.Listini;
import com.ncfsistemi.econ.db.table.RapportiniDettaglio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assistenza a tempo sul dispositivo (schema 39, STUDIO_ASSISTENZE_TEMPO.md §10). Le tabelle arrivano solo alle ditte con
 * il pacchetto, quindi senza pacchetto qui non c'e' niente e la maschera del rapportino resta com'era.
 * - articoli in assistenza: mesi proposti sulla riga (dell'articolo o della ditta), quantita' in pezzi interi;
 * - testo della riga: quello fissato dal server alla conferma, prima l'anteprima con il testo della ditta;
 * - controlli: le assistenze del cantiere segnate nel rapportino, inviate con lui; la scadenza la ricalcola il server.
 */
public final class AssistenzeLocali {

    /** Campi aggiunti alle assistenze controllabili: codice e descrizione dell'articolo. */
    public static final String CODICE_ARTICOLO = "codice_articolo";
    public static final String DESCRIZIONE_ARTICOLO = "descrizione_articolo";

    private AssistenzeLocali() {}

    /** Mesi proposti per l'articolo se e' in assistenza nella ditta (quelli dell'articolo o della ditta), altrimenti 0. */
    public static int mesiArticolo(Context ctx, DbInterno db, int idListino) {
        if (idListino == 0 || !esisteTabella(db, DitteArticoliAssistenza.NOME_TABELLA)) return 0;
        ArrayList<Object> r = db.eseguiSelect("select " + DitteArticoliAssistenza.MESI + " from " + DitteArticoliAssistenza.NOME_TABELLA
                + " where " + DitteArticoliAssistenza.ID_LISTINO + "=" + idListino + " and " + DitteArticoliAssistenza.ID_DITTA + "="
                + Sessione.getDittaSelezionata(), null);
        if (r.isEmpty()) return 0;
        int mesi = RigheRapportino.intero((ContentValues) r.get(0), DitteArticoliAssistenza.MESI);
        return mesi > 0 ? mesi : TokenManager.getInstance(ctx).getMesiAssistenza();
    }

    /**
     * Testo dell'assistenza della riga: quello fissato dal server alla conferma, altrimenti l'anteprima con il testo della
     * ditta ({mesi}, {data} = data del rapportino + mesi). Vuoto se la riga non e' in assistenza.
     */
    public static String testoRiga(Context ctx, ContentValues riga, long dataRapportino) {
        String fissato = RigheRapportino.testo(riga, RapportiniDettaglio.TESTO_ASSISTENZA);
        if (!fissato.isEmpty()) return fissato;
        int mesi = RigheRapportino.intero(riga, RapportiniDettaglio.MESI_ASSISTENZA);
        if (mesi <= 0) return "";
        return TokenManager.getInstance(ctx).getTestoAssistenza().replace("{mesi}", String.valueOf(mesi))
                .replace("{data}", dataTra(dataRapportino, mesi));
    }

    /** Data del rapportino (yyyyMMdd...) piu' i mesi, come gg/mm/aaaa; vuota senza data. */
    static String dataTra(long data, int mesi) {
        if (data <= 0) return "";
        String s = String.valueOf(data);
        if (s.length() < 8) return "";
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(Integer.parseInt(s.substring(0, 4)), Integer.parseInt(s.substring(4, 6)) - 1, Integer.parseInt(s.substring(6, 8)));
        c.add(java.util.Calendar.MONTH, mesi);
        return String.format(java.util.Locale.ITALY, "%02d/%02d/%04d", c.get(java.util.Calendar.DAY_OF_MONTH),
                c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.YEAR));
    }

    /** Data yyyyMMdd... come gg/mm/aaaa (vuota se manca). */
    public static String data(long data) {
        return dataTra(data, 0);
    }

    /**
     * Assistenze che si possono controllare con il rapportino, come sul server: le attive del cantiere installate entro la
     * data del rapportino e non montate da lui, piu' quelle che il rapportino ha gia' controllato. Per scadenza.
     */
    public static List<ContentValues> controllabili(DbInterno db, int idCantiere, long dataRapportino, int idRapportino) {
        List<ContentValues> out = new ArrayList<ContentValues>();
        if (idCantiere == 0 || !esisteTabella(db, Assistenze.NOME_TABELLA)) return out;
        long giorno = dataRapportino > 0 ? (dataRapportino / 1000000L) * 1000000L + 235959L : Long.MAX_VALUE;
        String sql = "select a.*, l." + Listini.CODICE_ARTICOLO + " as " + CODICE_ARTICOLO + ", l." + Listini.DESCRIZIONE + " as "
                + DESCRIZIONE_ARTICOLO + " from " + Assistenze.NOME_TABELLA + " a left join " + Listini.NOME_TABELLA + " l on l."
                + Listini.ID + "=a." + Assistenze.ID_LISTINO + " where a." + Assistenze.ID_CANTIERE + "=" + idCantiere
                + " and ((a." + Assistenze.STATO + "='" + Assistenze.ATTIVA + "' and coalesce(a." + Assistenze.DATA_INSTALLAZIONE + ",0)<=" + giorno
                + " and not exists (select 1 from " + RapportiniDettaglio.NOME_TABELLA + " d where d." + RapportiniDettaglio.ID
                + "=a." + Assistenze.ID_RIGA_RAPPORTINO + " and d." + RapportiniDettaglio.ID_RAPPORTINO + "=" + idRapportino + "))"
                + " or exists (select 1 from " + AssistenzeControlli.NOME_TABELLA + " c where c." + AssistenzeControlli.ID_ASSISTENZA
                + "=a." + Assistenze.ID + " and c." + AssistenzeControlli.ID_RAPPORTINO + "=" + idRapportino + "))"
                + " order by a." + Assistenze.PROSSIMA_SCADENZA + ", a." + Assistenze.ID;
        for (Object o : db.eseguiSelect(sql, null)) out.add((ContentValues) o);
        return out;
    }

    /** Controlli segnati nel rapportino, per assistenza. */
    public static Map<Integer, ContentValues> controlli(DbInterno db, int idRapportino) {
        Map<Integer, ContentValues> out = new HashMap<Integer, ContentValues>();
        if (idRapportino == 0 || !esisteTabella(db, AssistenzeControlli.NOME_TABELLA)) return out;
        for (Object o : db.eseguiSelect("select * from " + AssistenzeControlli.NOME_TABELLA + " where "
                + AssistenzeControlli.ID_RAPPORTINO + "=" + idRapportino, null)) {
            ContentValues c = (ContentValues) o;
            out.put(RigheRapportino.intero(c, AssistenzeControlli.ID_ASSISTENZA), c);
        }
        return out;
    }

    /**
     * Salva i controlli del rapportino: scelte = assistenza → {esito, nota}; esito vuoto = non controllata (il controllo
     * che c'era si toglie, anche dal server). Solo le righe cambiate tornano da inviare.
     */
    public static void salvaControlli(DbInterno db, int idRapportino, long dataRapportino, int idUtenteDitta,
                                      Map<Integer, String[]> scelte) {
        if (!esisteTabella(db, AssistenzeControlli.NOME_TABELLA)) return;
        AssistenzeControlli tab = new AssistenzeControlli();
        Map<Integer, ContentValues> esistenti = controlli(db, idRapportino);
        for (Map.Entry<Integer, String[]> e : scelte.entrySet()) {
            String esito = e.getValue()[0];
            String nota = e.getValue()[1] != null ? e.getValue()[1].trim() : "";
            ContentValues c = esistenti.get(e.getKey());
            if (esito == null || esito.isEmpty()) {
                if (c != null) tab.cancellaRecord(db, c);
                continue;
            }
            if (c != null && esito.equals(RigheRapportino.testo(c, AssistenzeControlli.ESITO))
                    && nota.equals(RigheRapportino.testo(c, AssistenzeControlli.NOTE))
                    && RigheRapportino.intero(c, AssistenzeControlli.ID_RAPPORTINO) == idRapportino) {
                continue;
            }
            ContentValues val = c == null ? tab.getValoriLogInserimento(db) : tab.getValoriLogModifica(db);
            val.put(AssistenzeControlli.ID_ASSISTENZA, e.getKey());
            val.put(AssistenzeControlli.ID_RAPPORTINO, idRapportino);
            val.put(AssistenzeControlli.DATA, (dataRapportino / 1000000L) * 1000000L);
            val.put(AssistenzeControlli.ESITO, esito);
            val.put(AssistenzeControlli.NOTE, nota);
            if (c == null) {
                val.put(AssistenzeControlli.ID_UTENTE_DITTA, idUtenteDitta);
                tab.inserisciRecord(db, val);
            } else {
                ContentValues where = new ContentValues();
                where.put(AssistenzeControlli.ID, c.getAsInteger(AssistenzeControlli.ID));
                tab.aggiornaRecord(db, val, where);
            }
        }
    }

    /**
     * Articoli in assistenza collegati a un blocco della pianificazione (calendario dell'app): "codice descrizione ·
     * matricola"; vuoto se il blocco e' un lavoro ordinario.
     */
    public static List<String> articoliPianificati(DbInterno db, int idPianificazione) {
        List<String> out = new ArrayList<String>();
        if (idPianificazione == 0 || !esisteTabella(db, Assistenze.NOME_TABELLA)) return out;
        for (Object o : db.eseguiSelect("select l." + Listini.CODICE_ARTICOLO + " as c, l." + Listini.DESCRIZIONE + " as d, a."
                + Assistenze.MATRICOLA + " as m from " + Assistenze.NOME_TABELLA + " a left join " + Listini.NOME_TABELLA + " l on l."
                + Listini.ID + "=a." + Assistenze.ID_LISTINO + " where a." + Assistenze.ID_PIANIFICAZIONE + "=" + idPianificazione
                + " order by a." + Assistenze.PROSSIMA_SCADENZA, null)) {
            ContentValues r = (ContentValues) o;
            String m = RigheRapportino.testo(r, "m");
            out.add((RigheRapportino.testo(r, "c") + " " + RigheRapportino.testo(r, "d")).trim() + (m.isEmpty() ? "" : " · " + m));
        }
        return out;
    }

    private static boolean esisteTabella(DbInterno db, String tabella) {
        return !db.eseguiSelect("select name from sqlite_master where type='table' and name='" + tabella + "'", null).isEmpty();
    }
}
