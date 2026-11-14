package com.ncfsistemi.econ.db.table;

import android.content.ContentValues;

import com.ncfsistemi.econ.db.DbInterno;

import java.util.ArrayList;

/**
 * Materiale aggiunto a un elemento del disegno da Componi (schema 47, STUDIO_DESIGN_GRAFICA.md §10): un articolo della
 * tabella listini (del listino generale o della ditta, come le righe di materiale dei rapportini) con quantita' e prezzo
 * a costo. Nasce contabilizzato (non_conteggiare_preventivi = 0): il valore va a costo dell'elemento, si toglie toccando
 * la pillola nella riga. Codice, descrizione e unita' di misura restano sulla riga anche se l'articolo sparisce.
 */
public class ElementiCantiereMateriali extends AbstractTable {
	public static final String NOME_TABELLA = "elementi_cantiere_materiali";

	public static final String ID = "id";
	public static final String ID_ELEMENTO_CANT = "id_elemento_cant";
	public static final String ID_LISTINO = "id_listino";
	public static final String CODICE_ARTICOLO = "codice_articolo";
	public static final String DESCRIZIONE = "descrizione";
	public static final String UM = "um";
	public static final String QUANTITA = "quantita";
	public static final String PREZZO = "prezzo";
	public static final String NON_CONTEGGIARE_PREVENTIVI = "non_conteggiare_preventivi";
	public static final String ORDINE = "ordine";
	public static final String NOTA = "nota";
	public static final String ID_DITTA = "id_ditta";

	public ElementiCantiereMateriali() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(ID_ELEMENTO_CANT, INTEGER);
		aggiungiCampo(ID_LISTINO, INTEGER);
		aggiungiCampo(CODICE_ARTICOLO, TEXT);
		aggiungiCampo(DESCRIZIONE, TEXT);
		aggiungiCampo(UM, TEXT);
		aggiungiCampo(QUANTITA, NUMERIC);
		aggiungiCampo(PREZZO, NUMERIC);
		aggiungiCampo(NON_CONTEGGIARE_PREVENTIVI, INTEGER);
		aggiungiCampo(ORDINE, INTEGER);
		aggiungiCampo(NOTA, TEXT);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}

	/** Materiali dell'elemento nell'ordine in cui sono stati aggiunti. */
	public static ArrayList<ContentValues> diElemento(DbInterno db, int idElementoCant) {
		ArrayList<ContentValues> out = new ArrayList<>();
		for (Object o : db.eseguiSelect("select * from " + NOME_TABELLA + " where " + ID_ELEMENTO_CANT + "=" + idElementoCant
				+ " order by coalesce(" + ORDINE + ",0), " + ID + " desc", null)) {
			out.add((ContentValues) o);
		}
		return out;
	}

	/** Aggiunge l'articolo (riga di listini) all'elemento, gia' contabilizzato. */
	public long aggiungi(DbInterno db, int idElementoCant, ContentValues articolo, double quantita, double prezzo, String nota) {
		ContentValues val = getValoriLogInserimento(db);
		val.put(ID_ELEMENTO_CANT, idElementoCant);
		val.put(ID_LISTINO, articolo.getAsInteger(Listini.ID));
		val.put(CODICE_ARTICOLO, testo(articolo, Listini.CODICE_ARTICOLO));
		val.put(DESCRIZIONE, testo(articolo, Listini.DESCRIZIONE));
		val.put(UM, testo(articolo, "um"));
		val.put(QUANTITA, quantita);
		val.put(PREZZO, prezzo);
		val.put(NON_CONTEGGIARE_PREVENTIVI, 0);
		val.put(NOTA, nota == null ? "" : nota);
		Object max = db.eseguiSelect("select coalesce(max(" + ORDINE + "),0)+1 as n from " + NOME_TABELLA + " where "
				+ ID_ELEMENTO_CANT + "=" + idElementoCant, null).get(0);
		val.put(ORDINE, ((ContentValues) max).getAsInteger("n"));
		return inserisciRecord(db, val);
	}

	/** Cambia quantita', prezzo, nota o l'indicatore "non conteggiare" di una riga. */
	public void aggiorna(DbInterno db, int id, ContentValues campi) {
		ContentValues val = getValoriLogModifica(db);
		val.putAll(campi);
		val.put(IN_SERVER, 0);
		ContentValues where = new ContentValues();
		where.put(ID, id);
		aggiornaRecord(db, val, where);
	}

	/** Cancella tutti i materiali dell'elemento (anche dal server alla prossima sincronizzazione). */
	public void cancellaDiElemento(DbInterno db, int idElementoCant) {
		for (ContentValues m : diElemento(db, idElementoCant)) {
			cancellaRecord(db, m);
		}
	}

	/** Copia i materiali da un elemento a un altro (copia degli elementi tra documenti). */
	public void copia(DbInterno db, int idElementoOrigine, int idElementoDestinazione) {
		for (ContentValues m : diElemento(db, idElementoOrigine)) {
			ContentValues nuovi = new ContentValues();
			nuovi.put(ID_ELEMENTO_CANT, idElementoDestinazione);
			copiaRecord(db, m, nuovi);
		}
	}

	private static String testo(ContentValues v, String campo) {
		String s = v.getAsString(campo);
		return s == null ? "" : s;
	}
}
