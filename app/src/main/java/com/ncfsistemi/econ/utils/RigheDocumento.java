package com.ncfsistemi.econ.utils;

import android.content.ContentValues;

import java.util.ArrayList;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Listini;
import com.ncfsistemi.econ.db.table.Manodopera;
import com.ncfsistemi.econ.db.table.Preventivi;
import com.ncfsistemi.econ.db.table.PreventiviDettaglio;
import com.ncfsistemi.econ.db.table.RapportiniDettaglio;
import com.ncfsistemi.econ.db.table.RapportiniDettaglioTipi;
import com.ncfsistemi.econ.db.table.UnitaMisura;
import com.ncfsistemi.econ.views.PopupRigaRapportino;

/**
 * Righe "libere" di preventivo e ordine fatte con la finestra della riga del rapportino in modo documento
 * (STUDIO_PREVENTIVI_ORDINI.md §6.5): articolo del listino (tipo MP senza elemento/componente) e lavoro (MD, voce di
 * manodopera). Traduce la riga di PreventiviDettaglio nella riga della finestra e la riga della finestra in quella del
 * documento. Le righe del cantiere (elementi, componenti, placche, collegamenti, note) restano con le loro finestre.
 */
public final class RigheDocumento {

    private RigheDocumento() {
    }

    /** Riga che si apre con la finestra comune: articolo libero o lavoro. */
    public static boolean conFinestraComune(ContentValues pd) {
        String tipo = RigheRapportino.testo(pd, PreventiviDettaglio.TIPO);
        if (PreventiviDettaglio.MANOPERA.equals(tipo)) return true;
        return PreventiviDettaglio.MATERIALE_PREVENTIVO.equals(tipo) && RigheRapportino.intero(pd, PreventiviDettaglio.ID_ELEMENTO) == 0
                && RigheRapportino.intero(pd, PreventiviDettaglio.ID_COMPONENTE) == 0 && RigheRapportino.intero(pd, PreventiviDettaglio.ID_LOCALE) == 0;
    }

    /** Contesto della finestra per un documento: sempre con i prezzi (il preventivo e' fatto di prezzi). */
    public static PopupRigaRapportino.Contesto contesto(int idCantiere) {
        PopupRigaRapportino.Contesto c = new PopupRigaRapportino.Contesto();
        c.documento = true;
        c.vedePrezzi = true;
        c.idCantiere = idCantiere;
        c.data = Long.parseLong(Utility.dataToString(java.util.Calendar.getInstance(), "yyyyMMddHHmmss"));
        return c;
    }

    /** Riga del documento → riga della finestra (chiavi di RapportiniDettaglio + SCONTO e NUM_OPERATORI). */
    public static ContentValues perFinestra(DbInterno db, ContentValues pd) {
        ContentValues r = new ContentValues();
        boolean lavoro = PreventiviDettaglio.MANOPERA.equals(RigheRapportino.testo(pd, PreventiviDettaglio.TIPO));
        r.put(RigheRapportino.CATEGORIA, lavoro ? RapportiniDettaglioTipi.CATEGORIA_LAVORO : RapportiniDettaglioTipi.CATEGORIA_MATERIALE);
        r.put(RapportiniDettaglio.QUANTITA, RigheRapportino.decimale(pd, PreventiviDettaglio.QUANTITA));
        r.put(RapportiniDettaglio.NOTE, RigheRapportino.testo(pd, PreventiviDettaglio.NOTE));
        r.put(RapportiniDettaglio.ID_UNITA_MISURA, idUnita(db, RigheRapportino.testo(pd, PreventiviDettaglio.UNITA_MISURA), lavoro));
        if (lavoro) {
            r.put(RapportiniDettaglio.ID_TIPO, RigheRapportino.intero(pd, PreventiviDettaglio.ID_MANODOPERA));
            r.put(RapportiniDettaglio.PREZZO, RigheRapportino.decimale(pd, PreventiviDettaglio.PREZZO));
            r.put(PopupRigaRapportino.NUM_OPERATORI, Math.max(1, RigheRapportino.intero(pd, PreventiviDettaglio.NUM_OPERATORI)));
        } else {
            String codice = RigheRapportino.testo(pd, PreventiviDettaglio.CODICE_ARTICOLO);
            ContentValues l = listino(db, codice);
            r.put(RapportiniDettaglio.ID_LISTINO, l != null ? RigheRapportino.intero(l, Listini.ID) : 0);
            r.put(RigheRapportino.CODICE_ARTICOLO, codice);
            r.put(RigheRapportino.DESCRIZIONE_ARTICOLO, RigheRapportino.testo(pd, PreventiviDettaglio.DESCRIZIONE));
            double lordo = RigheRapportino.decimale(pd, PreventiviDettaglio.PREZZO_VEN);
            r.put(RapportiniDettaglio.PREZZO, lordo > 0 ? lordo : RigheRapportino.decimale(pd, PreventiviDettaglio.PREZZO));
            r.put(PopupRigaRapportino.SCONTO, lordo > 0 ? RigheRapportino.decimale(pd, PreventiviDettaglio.SCONTO) : 0);
        }
        return r;
    }

    /**
     * Scrive la riga della finestra nel documento: nuova (idRiga 0) o al posto di quella indicata. Articolo: codice e
     * descrizione dal listino, costo dal listino, prezzo netto = prezzo di vendita meno lo sconto. Lavoro: voce di
     * manodopera, operatori e tariffa. IVA del cliente del documento.
     */
    public static void salva(DbInterno db, int idPreventivo, int idRiga, ContentValues f) {
        PreventiviDettaglio tab = new PreventiviDettaglio();
        boolean lavoro = RigheRapportino.intero(f, RapportiniDettaglio.ID_LISTINO) == 0 && RigheRapportino.intero(f, RapportiniDettaglio.ID_TIPO) > 0;
        ContentValues v = idRiga == 0 ? tab.getValoriLogInserimento(db) : tab.getValoriLogModifica(db);
        double quantita = RigheRapportino.decimale(f, RapportiniDettaglio.QUANTITA);
        double prezzo = RigheRapportino.decimale(f, RapportiniDettaglio.PREZZO);
        v.put(PreventiviDettaglio.QUANTITA, quantita);
        v.put(PreventiviDettaglio.UNITA_MISURA, codiceUnita(db, RigheRapportino.intero(f, RapportiniDettaglio.ID_UNITA_MISURA)));
        v.put(PreventiviDettaglio.NOTE, RigheRapportino.testo(f, RapportiniDettaglio.NOTE));
        if (lavoro) {
            int idManodopera = RigheRapportino.intero(f, RapportiniDettaglio.ID_TIPO);
            ContentValues where = new ContentValues();
            where.put(Manodopera.ID_MANODOPERA, idManodopera);
            ContentValues m = db.getRecord(new Manodopera(), where);
            v.put(PreventiviDettaglio.TIPO, PreventiviDettaglio.MANOPERA);
            v.put(PreventiviDettaglio.ID_MANODOPERA, idManodopera);
            v.put(PreventiviDettaglio.DESCRIZIONE, m != null ? RigheRapportino.testo(m, Manodopera.NOME) : "");
            v.put(PreventiviDettaglio.NUM_OPERATORI, Math.max(1, RigheRapportino.intero(f, PopupRigaRapportino.NUM_OPERATORI)));
            v.put(PreventiviDettaglio.PREZZO_VEN, prezzo);
            v.put(PreventiviDettaglio.SCONTO, 0);
            v.put(PreventiviDettaglio.PREZZO, prezzo);
            v.put(PreventiviDettaglio.CODICE_ARTICOLO, "");
        } else {
            ContentValues where = new ContentValues();
            where.put(Listini.ID, RigheRapportino.intero(f, RapportiniDettaglio.ID_LISTINO));
            ContentValues l = db.getRecord(new Listini(), where);
            double sconto = RigheRapportino.decimale(f, PopupRigaRapportino.SCONTO);
            v.put(PreventiviDettaglio.TIPO, PreventiviDettaglio.MATERIALE_PREVENTIVO);
            v.put(PreventiviDettaglio.CODICE_ARTICOLO, l != null ? RigheRapportino.testo(l, Listini.CODICE_ARTICOLO) : "");
            v.put(PreventiviDettaglio.DESCRIZIONE, l != null ? RigheRapportino.testo(l, Listini.DESCRIZIONE) : "");
            if (l != null && l.get(Listini.ID_LINEA) != null) v.put(PreventiviDettaglio.ID_LINEA, RigheRapportino.intero(l, Listini.ID_LINEA));
            v.put(PreventiviDettaglio.PREZZO_ACQ, l != null ? costo(l) : 0);
            v.put(PreventiviDettaglio.PREZZO_VEN, prezzo);
            v.put(PreventiviDettaglio.SCONTO, sconto);
            v.put(PreventiviDettaglio.PREZZO, Utility.arrotonda(prezzo * (1 - sconto / 100), 4));
            v.put(PreventiviDettaglio.ID_MANODOPERA, 0);
            v.put(PreventiviDettaglio.NUM_OPERATORI, 0);
        }
        if (idRiga == 0) {
            v.put(PreventiviDettaglio.ID_PREVENTIVO, idPreventivo);
            v.put(PreventiviDettaglio.ID_LOCALE, 0);
            v.put(PreventiviDettaglio.ID_ELEMENTO, 0);
            v.put(PreventiviDettaglio.ID_COMPONENTE, 0);
            v.put(PreventiviDettaglio.ID_PLACCA_MODULI, 0);
            v.put(PreventiviDettaglio.RICARICO, 0);
            ContentValues cliente = new Preventivi().getClientePreventivo(db, idPreventivo);
            v.put(PreventiviDettaglio.CODICE_IVA, cliente != null ? RigheRapportino.testo(cliente, com.ncfsistemi.econ.db.table.Anagrafica.CODICE_IVA) : "");
            v.put(PreventiviDettaglio.STATO, PreventiviDettaglio.APERTO);
            tab.inserisciRecord(db, v);
        } else {
            ContentValues where = new ContentValues();
            where.put(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO, idRiga);
            tab.aggiornaRecord(db, v, where);
        }
    }

    /** Costo dell'articolo: l'ultimo acquisto, altrimenti il listino meno lo sconto. */
    private static double costo(ContentValues l) {
        double acquisto = RigheRapportino.decimale(l, Listini.PRZ_ULTIMO_ACQUISTO);
        if (acquisto > 0) return acquisto;
        return RigheRapportino.decimale(l, Listini.PRZ_LISTINO) * (1 - RigheRapportino.decimale(l, Listini.SCONTO) / 100);
    }

    private static ContentValues listino(DbInterno db, String codice) {
        if (codice.isEmpty()) return null;
        ArrayList<Object> l = db.eseguiSelect("select * from " + Listini.NOME_TABELLA + " where " + Listini.CODICE_ARTICOLO + "=? or "
                + Listini.CODICE_DITTA + "=? order by " + Listini.ID + " desc limit 1", new String[] { codice, codice });
        return l.isEmpty() ? null : (ContentValues) l.get(0);
    }

    /** Id dell'unita' dal codice della riga ("H" delle righe vecchie = ore); 0 = la finestra propone la sua. */
    private static int idUnita(DbInterno db, String codice, boolean lavoro) {
        String c = codice.trim();
        if (c.equalsIgnoreCase("H")) c = UnitaMisura.CODICE_ORE;
        if (c.isEmpty()) c = lavoro ? UnitaMisura.CODICE_ORE : UnitaMisura.CODICE_PEZZO;
        ArrayList<Object> u = db.eseguiSelect("select * from " + UnitaMisura.NOME_TABELLA + " where " + UnitaMisura.UNITA_MISURA + "=?", new String[] { c });
        return u.isEmpty() ? 0 : RigheRapportino.intero((ContentValues) u.get(0), UnitaMisura.ID);
    }

    private static String codiceUnita(DbInterno db, int id) {
        ArrayList<Object> u = db.eseguiSelect("select * from " + UnitaMisura.NOME_TABELLA + " where " + UnitaMisura.ID + "=" + id, null);
        return u.isEmpty() ? "" : RigheRapportino.testo((ContentValues) u.get(0), UnitaMisura.UNITA_MISURA);
    }
}
