package com.ncfsistemi.econ.utils;

import android.content.ContentValues;
import android.content.Context;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.Rapportini;
import com.ncfsistemi.econ.db.table.RapportiniDettaglio;
import com.ncfsistemi.econ.db.table.RapportiniDettaglioTipi;
import com.ncfsistemi.econ.db.table.UnitaMisura;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Dati del calendario personale (CALENDARIO_PERSONALE.md), tutti dal database del tablet: le ore dell'utente nei
 * rapportini (righe di lavoro e viaggio con lui come operatore), la sua pianificazione (solo se la ditta la usa) e i
 * rapportini di un giorno in cui puo' aggiungere ore secondo le regole della scheda del rapportino.
 * Date come nel resto dell'app: long yyyyMMddHHmmss; il giorno e' yyyyMMdd.
 */
public final class CalendarioPersonale {

    private CalendarioPersonale() {
    }

    /** Ore di un rapportino in un giorno: una voce per rapportino, lavoro e viaggio separati. */
    public static final class Voce {
        public int idRapportino;
        public long giorno;
        public String luogo = "";      // cantiere, oppure cliente senza cantiere
        public String cliente = "";
        public int minutiLavoro;
        public int minutiViaggio;
        public String orari = "";      // "08:00-12:00, 13:00-17:00"
        public ContentValues stato;    // stati_documento, null se non scaricato
        public String numero = "";     // "12/2026" se confermato
    }

    /** Un giorno del calendario. */
    public static final class Giorno {
        public final long giorno;
        public final List<Voce> voci = new ArrayList<>();
        public final List<ContentValues> pianificazione = new ArrayList<>();

        Giorno(long giorno) {
            this.giorno = giorno;
        }

        public int minutiLavoro() {
            int m = 0;
            for (Voce v : voci) m += v.minutiLavoro;
            return m;
        }

        public int minutiViaggio() {
            int m = 0;
            for (Voce v : voci) m += v.minutiViaggio;
            return m;
        }
    }

    /** Giorni da dal ad al compresi (yyyyMMdd), con ore e pianificazione. */
    public static List<Giorno> periodo(DbInterno db, Context ctx, RegoleRapportino.Utente utente, long dal, long al) {
        Map<Long, Giorno> giorni = new LinkedHashMap<>();
        Calendar c = calendario(dal);
        while (giornoDi(c) <= al) {
            giorni.put(giornoDi(c), new Giorno(giornoDi(c)));
            c.add(Calendar.DATE, 1);
        }
        if (utente.idUtenteDitta <= 0) return new ArrayList<>(giorni.values());

        // righe di lavoro e viaggio dell'utente, con testata, cantiere e cliente
        String sql = "select r." + Rapportini.ID + " as id_rapportino, r." + Rapportini.DATA_RAPPORTINO + " as data, r." + Rapportini.ID_STATO + " as id_stato,"
                + " r." + Rapportini.NUMERO + " as numero, r." + Rapportini.ANNO + " as anno,"
                + " c." + Cantieri.NOME + " as cantiere, a." + Anagrafica.RAGIONE_SOCIALE + " as cliente,"
                + " t." + RapportiniDettaglioTipi.CATEGORIA + " as categoria, d." + RapportiniDettaglio.QUANTITA + " as quantita,"
                + " coalesce(u." + UnitaMisura.MINUTI_PER_UNITA + ", 60) as minuti_per_unita,"
                + " d." + RapportiniDettaglio.ORA_INIZIO + " as ora_inizio, d." + RapportiniDettaglio.ORA_FINE + " as ora_fine"
                + " from " + RapportiniDettaglio.NOME_TABELLA + " d"
                + " inner join " + Rapportini.NOME_TABELLA + " r on r." + Rapportini.ID + "=d." + RapportiniDettaglio.ID_RAPPORTINO
                + " inner join " + RapportiniDettaglioTipi.NOME_TABELLA + " t on t." + RapportiniDettaglioTipi.ID + "=d." + RapportiniDettaglio.ID_TIPO
                + " left join " + UnitaMisura.NOME_TABELLA + " u on u." + UnitaMisura.ID + "=d." + RapportiniDettaglio.ID_UNITA_MISURA
                + " left join " + Cantieri.NOME_TABELLA + " c on c." + Cantieri.ID_CANTIERE + "=r." + Rapportini.ID_CANTIERE
                + " left join " + Anagrafica.NOME_TABELLA + " a on a." + Anagrafica.ID_ANAGRAFICA + "=r." + Rapportini.ID_CLIENTE
                + " where r." + Rapportini.ID_DITTA + "=" + Sessione.getDittaSelezionata()
                + " and d." + RapportiniDettaglio.ID_UTENTE_MANODOPERA + "=" + utente.idUtenteDitta
                + " and t." + RapportiniDettaglioTipi.CATEGORIA + " in ('" + RapportiniDettaglioTipi.CATEGORIA_LAVORO + "','" + RapportiniDettaglioTipi.CATEGORIA_VIAGGIO + "')"
                + " and r." + Rapportini.DATA_RAPPORTINO + " between " + (dal * 1000000L) + " and " + (al * 1000000L + 235959L)
                + " order by r." + Rapportini.DATA_RAPPORTINO + ", coalesce(d." + RapportiniDettaglio.ORA_INIZIO + ", ''), r." + Rapportini.ID;
        Map<Integer, Voce> perRapportino = new TreeMap<>();
        for (Object o : db.eseguiSelect(sql, null)) {
            ContentValues r = (ContentValues) o;
            int id = intero(r, "id_rapportino");
            Voce v = perRapportino.get(id);
            if (v == null) {
                v = new Voce();
                v.idRapportino = id;
                v.giorno = lungo(r, "data") / 1000000L;
                String cantiere = testo(r, "cantiere");
                v.cliente = testo(r, "cliente");
                v.luogo = !cantiere.isEmpty() ? cantiere : (!v.cliente.isEmpty() ? v.cliente : "Rapportino");
                v.stato = RegoleRapportino.stato(db, intero(r, "id_stato"));
                int numero = intero(r, "numero");
                v.numero = numero > 0 ? numero + "/" + intero(r, "anno") : "";
                perRapportino.put(id, v);
                Giorno g = giorni.get(v.giorno);
                if (g != null) g.voci.add(v);
            }
            Double q = r.getAsDouble("quantita");
            Double mpu = r.getAsDouble("minuti_per_unita");
            int minuti = (int) Math.round((q != null ? q : 0) * (mpu != null && mpu > 0 ? mpu : 60));
            if (RapportiniDettaglioTipi.CATEGORIA_VIAGGIO.equals(testo(r, "categoria"))) {
                v.minutiViaggio += minuti;
            } else {
                v.minutiLavoro += minuti;
            }
            String orario = orario(testo(r, "ora_inizio"), testo(r, "ora_fine"));
            if (!orario.isEmpty() && !v.orari.contains(orario)) {
                v.orari = v.orari.isEmpty() ? orario : v.orari + ", " + orario;
            }
        }

        if (utente.pianificazione) {
            for (Object o : RegoleRapportino.pianificazionePeriodo(db, utente, dal * 1000000L, al * 1000000L + 235959L)) {
                ContentValues p = (ContentValues) o;
                Giorno g = giorni.get(lungo(p, "data") / 1000000L);
                if (g != null) g.pianificazione.add(p);
            }
        }
        return new ArrayList<>(giorni.values());
    }

    /**
     * Rapportini del giorno in cui l'utente puo' aggiungere le proprie ore, con le regole della scheda
     * (RapportinoDettaglioModActivity): stato modificabile e funzionalita' MODIFICA, e poi suo (autore o inserito da lui),
     * oppure In verifica con lui coinvolto. Tra questi solo quelli "suoi o della sua squadra": autore lui o una persona
     * delle sue squadre, gia' in una riga, o pianificato sul cantiere nella settimana. Colonne: id, cantiere, cliente,
     * autore (nome), id_stato.
     */
    public static List<ContentValues> rapportiniAperti(DbInterno db, Context ctx, RegoleRapportino.Utente utente, long giorno) {
        List<ContentValues> ris = new ArrayList<>();
        if (!FunzionalitaApp.puoModificare(ctx, FunzionalitaApp.RAPPORTINI)) return ris;
        Set<Integer> squadra = RegoleRapportino.personeDelleSquadre(db, RegoleRapportino.squadreDi(db, utente));
        String sql = "select r.*, c." + Cantieri.NOME + " as cantiere, a." + Anagrafica.RAGIONE_SOCIALE + " as cliente"
                + " from " + Rapportini.NOME_TABELLA + " r"
                + " left join " + Cantieri.NOME_TABELLA + " c on c." + Cantieri.ID_CANTIERE + "=r." + Rapportini.ID_CANTIERE
                + " left join " + Anagrafica.NOME_TABELLA + " a on a." + Anagrafica.ID_ANAGRAFICA + "=r." + Rapportini.ID_CLIENTE
                + " where r." + Rapportini.ID_DITTA + "=" + Sessione.getDittaSelezionata()
                + " and r." + Rapportini.DATA_RAPPORTINO + " between " + (giorno * 1000000L) + " and " + (giorno * 1000000L + 235959L)
                + " order by c." + Cantieri.NOME + ", r." + Rapportini.ID;
        for (Object o : db.eseguiSelect(sql, null)) {
            ContentValues t = (ContentValues) o;
            ContentValues stato = RegoleRapportino.stato(db, intero(t, Rapportini.ID_STATO));
            if (!RegoleRapportino.isModificabile(stato)) continue;
            if (RegoleRapportino.bloccatoInApp(stato)) continue;
            int idCantiere = intero(t, Rapportini.ID_CANTIERE);
            long data = lungo(t, Rapportini.DATA_RAPPORTINO);
            List<Object> righe = new ArrayList<Object>(RigheRapportino.righe(db, intero(t, Rapportini.ID)));
            boolean proprietario = RegoleRapportino.isProprietario(t, utente);
            boolean coinvolto = RegoleRapportino.coinvolto(db, utente, idCantiere, data, righe);
            boolean aggiunge = utente.amministratore || proprietario || (RegoleRapportino.isRigheOperatori(stato) && coinvolto);
            if (!aggiunge) continue;
            // suo o della sua squadra (l'amministratore vedrebbe tutti quelli della ditta)
            boolean pertinente = proprietario || squadra.contains(intero(t, Rapportini.ID_UTENTE_DITTA))
                    || (utente.pianificazione ? coinvolto && idCantiere > 0 : giaInRiga(righe, utente));
            if (!pertinente) continue;
            t.put("autore", RegoleRapportino.nomeOperatore(db, intero(t, Rapportini.ID_UTENTE_DITTA)));
            ris.add(t);
        }
        return ris;
    }

    private static boolean giaInRiga(List<Object> righe, RegoleRapportino.Utente utente) {
        for (Object o : righe) {
            if (intero((ContentValues) o, RapportiniDettaglio.ID_UTENTE_MANODOPERA) == utente.idUtenteDitta) return true;
        }
        return false;
    }

    // ── date ─────────────────────────────────────────────────────────────────

    /** Calendario sul giorno yyyyMMdd. */
    public static Calendar calendario(long giorno) {
        int g = (int) giorno;
        return new GregorianCalendar(g / 10000, (g / 100) % 100 - 1, g % 100);
    }

    public static long giornoDi(Calendar c) {
        return c.get(Calendar.YEAR) * 10000L + (c.get(Calendar.MONTH) + 1) * 100L + c.get(Calendar.DAY_OF_MONTH);
    }

    public static long oggi() {
        return giornoDi(Calendar.getInstance());
    }

    /** Lunedi' della settimana del giorno. */
    public static long lunedi(long giorno) {
        Calendar c = calendario(giorno);
        c.add(Calendar.DATE, -((c.get(Calendar.DAY_OF_WEEK) + 5) % 7));
        return giornoDi(c);
    }

    public static long piuGiorni(long giorno, int n) {
        Calendar c = calendario(giorno);
        c.add(Calendar.DATE, n);
        return giornoDi(c);
    }

    /** "7 h 30" / "45 min" / "" se zero. */
    public static String ore(int minuti) {
        if (minuti <= 0) return "";
        int h = minuti / 60, m = minuti % 60;
        if (h == 0) return m + " min";
        return m == 0 ? h + " h" : h + " h " + (m < 10 ? "0" + m : String.valueOf(m));
    }

    private static String orario(String inizio, String fine) {
        String i = hhmm(inizio), f = hhmm(fine);
        if (i.isEmpty() && f.isEmpty()) return "";
        return i + "-" + f;
    }

    /** "08:00" da "08:00:00", "0800", "20261012080000"... */
    private static String hhmm(String s) {
        if (s == null) return "";
        String cifre = s.replaceAll("[^0-9]", "");
        if (cifre.length() >= 14) cifre = cifre.substring(8, 12);
        else if (cifre.length() == 3) cifre = "0" + cifre; // "8:00"
        else if (cifre.length() >= 4) cifre = cifre.substring(0, 4);
        else return "";
        return cifre.substring(0, 2) + ":" + cifre.substring(2, 4);
    }

    private static int intero(ContentValues cv, String k) {
        Integer v = cv.getAsInteger(k);
        return v != null ? v : 0;
    }

    private static long lungo(ContentValues cv, String k) {
        Long v = cv.getAsLong(k);
        return v != null ? v : 0;
    }

    private static String testo(ContentValues cv, String k) {
        String v = cv.getAsString(k);
        return v != null ? v.trim() : "";
    }
}
