package pfa.app.econtab.utils;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.AbstractTable;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.ClientiIndirizzi;

/**
 * Cantiere creato in automatico da un indirizzo del cliente (GESTIONE_CLIENTI.md §6, come Cantieri::daIndirizzo sul
 * server): cliente e indirizzo copiati, collegato all'indirizzo, nome = descrizione dell'indirizzo oppure "cliente —
 * città". Se c'e' gia' un cantiere attivo nato da quell'indirizzo non ne crea un altro. Serve CANTIERI.CREA.
 */
public final class CantiereDaIndirizzo {

    private CantiereDaIndirizzo() {
    }

    /** Crea il cantiere e ritorna il messaggio da mostrare. */
    public static String crea(DbInterno db, int idIndirizzo) {
        ContentValues w = new ContentValues();
        w.put(ClientiIndirizzi.ID, idIndirizzo);
        ContentValues ind = db.getRecord(new ClientiIndirizzi(), w);
        if (ind == null) return "Indirizzo non trovato.";

        ArrayList<Object> esistenti = db.eseguiSelect("SELECT " + Cantieri.NOME + " FROM " + Cantieri.NOME_TABELLA + " WHERE "
                + Cantieri.ID_CLIENTE_INDIRIZZO + " = " + idIndirizzo + " AND coalesce(" + AbstractTable.ATTIVO + ", 1) = 1 LIMIT 1", null);
        if (!esistenti.isEmpty()) {
            return "Esiste già il cantiere \"" + ((ContentValues) esistenti.get(0)).getAsString(Cantieri.NOME)
                    + "\" a questo indirizzo: non ne ho creato un altro.";
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
        if (tab.inserisciRecord(db, val) < 0) {
            return "Cantiere non creato (limite della licenza).";
        }
        return "Cantiere \"" + val.getAsString(Cantieri.NOME) + "\" creato a " + ClientiIndirizzi.completo(ind) + ".";
    }

    private static String testo(ContentValues v, String campo) {
        String s = v.getAsString(campo);
        return s == null ? "" : s.trim();
    }
}
