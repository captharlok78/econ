package com.ncfsistemi.econ.utils;

import android.content.ContentValues;

import java.util.ArrayList;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.ClientiIndirizzi;

/**
 * Cantiere creato in automatico da un indirizzo del cliente (GESTIONE_CLIENTI.md §6, come Cantieri::daIndirizzo sul
 * server): cliente e indirizzo copiati, collegato all'indirizzo, nome = descrizione dell'indirizzo oppure "cliente —
 * città". Se c'e' gia' un cantiere attivo nato da quell'indirizzo non ne crea un altro. Serve CANTIERI.CREA.
 */
public final class CantiereDaIndirizzo {

    private CantiereDaIndirizzo() {
    }

    /** Esito della creazione: id del cantiere creato (0 = non creato) e messaggio da mostrare. */
    public static final class Esito {
        public final int idCantiere;
        public final String messaggio;

        Esito(int idCantiere, String messaggio) {
            this.idCantiere = idCantiere;
            this.messaggio = messaggio;
        }
    }

    /** Crea il cantiere e ritorna il messaggio da mostrare. */
    public static String crea(DbInterno db, int idIndirizzo) {
        return creaCantiere(db, idIndirizzo).messaggio;
    }

    /** Crea il cantiere e ritorna id e messaggio (es. il rapportino lo sceglie subito). */
    public static Esito creaCantiere(DbInterno db, int idIndirizzo) {
        ContentValues w = new ContentValues();
        w.put(ClientiIndirizzi.ID, idIndirizzo);
        ContentValues ind = db.getRecord(new ClientiIndirizzi(), w);
        if (ind == null) return new Esito(0, "Indirizzo non trovato.");

        ArrayList<Object> esistenti = db.eseguiSelect("SELECT " + Cantieri.NOME + " FROM " + Cantieri.NOME_TABELLA + " WHERE "
                + Cantieri.ID_CLIENTE_INDIRIZZO + " = " + idIndirizzo + " AND coalesce(" + AbstractTable.ATTIVO + ", 1) = 1 LIMIT 1", null);
        if (!esistenti.isEmpty()) {
            return new Esito(0, "Esiste già il cantiere \"" + ((ContentValues) esistenti.get(0)).getAsString(Cantieri.NOME)
                    + "\" a questo indirizzo: non ne ho creato un altro.");
        }

        int idCliente = ind.getAsInteger(ClientiIndirizzi.ID_ANAGRAFICA);
        ContentValues wc = new ContentValues();
        wc.put(Anagrafica.ID_ANAGRAFICA, idCliente);
        ContentValues cliente = db.getRecord(new Anagrafica(), wc);
        String descrizione = testo(ind, ClientiIndirizzi.DESCRIZIONE);
        String citta = testo(ind, ClientiIndirizzi.CITTA);
        String nome = !descrizione.isEmpty() ? descrizione
                : (cliente != null ? testo(cliente, Anagrafica.RAGIONE_SOCIALE) : "") + (citta.isEmpty() ? "" : " — " + citta);

        Cantieri tab = new Cantieri();
        ContentValues val = tab.getValoriLogInserimento(db);
        val.put(Cantieri.ID_ANAGRAFICA, idCliente);
        val.put(Cantieri.ID_CLIENTE_INDIRIZZO, idIndirizzo);
        val.put(Cantieri.NOME, nome.length() > 100 ? nome.substring(0, 100) : nome);
        val.put(Cantieri.INDIRIZZO, testo(ind, ClientiIndirizzi.INDIRIZZO));
        val.put(Cantieri.CIVICO, testo(ind, ClientiIndirizzi.CIVICO));
        val.put(Cantieri.CAP, testo(ind, ClientiIndirizzi.CAP));
        val.put(Cantieri.CITTA, citta);
        val.put(Cantieri.PROVINCIA, testo(ind, ClientiIndirizzi.PROVINCIA));
        // inserisciRecord da' il rowid, che qui e' l'id del cantiere (negativo sul dispositivo): un errore e' un'eccezione
        try {
            tab.inserisciRecord(db, val);
        } catch (android.database.SQLException e) {
            return new Esito(0, "Cantiere non creato: " + e.getMessage());
        }
        Integer id = val.getAsInteger(Cantieri.ID_CANTIERE);
        return new Esito(id == null ? 0 : id, "Cantiere \"" + val.getAsString(Cantieri.NOME) + "\" creato a " + ClientiIndirizzi.completo(ind) + ".");
    }

    /** Cliente senza nessun indirizzo (es. appena creato col "+" del rapportino): cantiere col nome del cliente, indirizzo da completare. */
    public static Esito creaCantiereSenzaIndirizzo(DbInterno db, int idCliente) {
        ContentValues wc = new ContentValues();
        wc.put(Anagrafica.ID_ANAGRAFICA, idCliente);
        ContentValues cliente = db.getRecord(new Anagrafica(), wc);
        if (cliente == null) return new Esito(0, "Cliente non trovato.");
        String nome = testo(cliente, Anagrafica.RAGIONE_SOCIALE);
        Cantieri tab = new Cantieri();
        ContentValues val = tab.getValoriLogInserimento(db);
        val.put(Cantieri.ID_ANAGRAFICA, idCliente);
        val.put(Cantieri.ID_CLIENTE_INDIRIZZO, 0);
        val.put(Cantieri.NOME, nome.length() > 100 ? nome.substring(0, 100) : nome);
        // inserisciRecord da' il rowid, che qui e' l'id del cantiere (negativo sul dispositivo): un errore e' un'eccezione
        try {
            tab.inserisciRecord(db, val);
        } catch (android.database.SQLException e) {
            return new Esito(0, "Cantiere non creato: " + e.getMessage());
        }
        Integer id = val.getAsInteger(Cantieri.ID_CANTIERE);
        return new Esito(id == null ? 0 : id, "Cantiere \"" + nome + "\" creato: l'indirizzo si completa dalla scheda del cantiere.");
    }

    private static String testo(ContentValues v, String campo) {
        String s = v.getAsString(campo);
        return s == null ? "" : s.trim();
    }
}
