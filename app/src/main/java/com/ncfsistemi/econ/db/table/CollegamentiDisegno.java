package com.ncfsistemi.econ.db.table;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.ncfsistemi.econ.db.DbInterno;

/**
 * Collegamento "oggetto del disegno ↔ documento" (schema 51, STUDIO_STORICO_DISEGNO.md §2.2): oggetto e documento sono
 * tipo (testo) + id, senza chiave esterna. Dal tablet nascono con "Rapportino dal disegno" (§8.1); il server li controlla,
 * aggiorna stato e date degli oggetti e scrive lo storico. Gli id nati sul tablet si rimappano con rimappa().
 */
public class CollegamentiDisegno extends AbstractTable {
	public static final String NOME_TABELLA = "collegamenti_disegno";

	public static final String ID = "id";
	public static final String ID_DITTA = "id_ditta";
	public static final String ID_CANTIERE = "id_cantiere";
	public static final String OGGETTO_TIPO = "oggetto_tipo";
	public static final String ID_OGGETTO = "id_oggetto";
	public static final String DOCUMENTO_TIPO = "documento_tipo";
	public static final String ID_DOCUMENTO = "id_documento";
	public static final String ID_RIGA_DOCUMENTO = "id_riga_documento";
	public static final String RUOLO = "ruolo";
	public static final String QUANTITA = "quantita";
	public static final String NOTA = "nota";

	public static final String ELEMENTO = "ELEMENTO";
	public static final String COMPONENTE = "COMPONENTE";
	public static final String MATERIALE = "MATERIALE";
	public static final String RAPPORTINO = "RAPPORTINO";
	public static final String PREVENTIVO = "PREVENTIVO";
	public static final String ORDINE = "ORDINE";
	public static final String ASSISTENZA = "ASSISTENZA";
	public static final String RUOLO_POSATO = "POSATO";

	/** Stato degli oggetti (colonna stato di elementi, componenti e materiale, la calcola il server). */
	public static final String STATO_PREVISTO = "PREVISTO";
	public static final String STATO_ORDINATO = "ORDINATO";
	public static final String STATO_POSATO = "POSATO";
	public static final String STATO_DISMESSO = "DISMESSO";

	public CollegamentiDisegno() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(ID_CANTIERE, INTEGER);
		aggiungiCampo(OGGETTO_TIPO, TEXT);
		aggiungiCampo(ID_OGGETTO, INTEGER);
		aggiungiCampo(DOCUMENTO_TIPO, TEXT);
		aggiungiCampo(ID_DOCUMENTO, INTEGER);
		aggiungiCampo(ID_RIGA_DOCUMENTO, INTEGER);
		aggiungiCampo(RUOLO, TEXT);
		aggiungiCampo(QUANTITA, NUMERIC);
		aggiungiCampo(NOTA, TEXT);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}

	/** Tabella locale dell'oggetto, null se il tipo non e' conosciuto. */
	public static String tabellaOggetto(String tipo) {
		if (ELEMENTO.equals(tipo)) return ElementiCantiere.NOME_TABELLA;
		if (COMPONENTE.equals(tipo)) return ComponentiCantiere.NOME_TABELLA;
		if (MATERIALE.equals(tipo)) return ElementiCantiereMateriali.NOME_TABELLA;
		return null;
	}

	/** Chiave primaria locale della tabella dell'oggetto. */
	public static String pkOggetto(String tipo) {
		if (ELEMENTO.equals(tipo)) return ElementiCantiere.ID_ELEMENTO_CANT;
		if (COMPONENTE.equals(tipo)) return ComponentiCantiere.ID_COMPONENTE_CANT;
		return ElementiCantiereMateriali.ID;
	}

	/** L'oggetto e' gia' collegato a un rapportino (posato con un rapportino). */
	public static boolean haRapportino(DbInterno db, String tipo, int idOggetto) {
		return db.getRecord("select " + ID + " from " + NOME_TABELLA + " where " + OGGETTO_TIPO + "='" + tipo + "' and "
				+ ID_OGGETTO + "=" + idOggetto + " and " + DOCUMENTO_TIPO + "='" + RAPPORTINO + "'") != null;
	}

	/** Nuovo collegamento scritto sul tablet (in_server = 0, si invia alla prossima sincronizzazione). */
	public static void inserisci(DbInterno db, int idCantiere, String tipo, int idOggetto, String tipoDocumento, int idDocumento,
								 int idRiga, String ruolo, double quantita) {
		CollegamentiDisegno t = new CollegamentiDisegno();
		ContentValues v = t.getValoriLogInserimento(db);
		v.put(ID_CANTIERE, idCantiere);
		v.put(OGGETTO_TIPO, tipo);
		v.put(ID_OGGETTO, idOggetto);
		v.put(DOCUMENTO_TIPO, tipoDocumento);
		v.put(ID_DOCUMENTO, idDocumento);
		v.put(ID_RIGA_DOCUMENTO, idRiga);
		v.put(RUOLO, ruolo);
		v.put(QUANTITA, quantita);
		v.put(IN_SERVER, 0);
		t.inserisciRecord(db, v);
	}

	/**
	 * Dopo l'invio: un id nato sul tablet (negativo) diventa quello del server anche nei collegamenti, dove oggetto,
	 * documento e riga non sono chiavi esterne (la colonna dipende dal tipo). Da chiamare per ogni id rimappato.
	 */
	public static void rimappa(SQLiteDatabase db, String tabella, int vecchio, int nuovo) {
		String[] tipiOggetto = {ELEMENTO, COMPONENTE, MATERIALE};
		for (String tipo : tipiOggetto) {
			if (tabella.equals(tabellaOggetto(tipo))) {
				aggiorna(db, ID_OGGETTO, nuovo, OGGETTO_TIPO + " = '" + tipo + "' and " + ID_OGGETTO + " = " + vecchio);
			}
		}
		String documenti = null, righe = null;
		switch (tabella) {
			case "rapportini": documenti = "'" + RAPPORTINO + "'"; break;
			case "preventivi": documenti = "'" + PREVENTIVO + "','" + ORDINE + "'"; break;
			case "rapportini_dettaglio": righe = "'" + RAPPORTINO + "'"; break;
			case "preventivi_dettaglio": righe = "'" + PREVENTIVO + "','" + ORDINE + "'"; break;
			default: break;
		}
		if (documenti != null) {
			aggiorna(db, ID_DOCUMENTO, nuovo, DOCUMENTO_TIPO + " in (" + documenti + ") and " + ID_DOCUMENTO + " = " + vecchio);
		}
		if (righe != null) {
			aggiorna(db, ID_RIGA_DOCUMENTO, nuovo, DOCUMENTO_TIPO + " in (" + righe + ") and " + ID_RIGA_DOCUMENTO + " = " + vecchio);
		}
	}

	private static void aggiorna(SQLiteDatabase db, String colonna, int valore, String dove) {
		try {
			ContentValues cv = new ContentValues();
			cv.put(colonna, valore);
			db.update(NOME_TABELLA, cv, dove, null);
		} catch (Exception ignored) {
			// tabella non ancora creata (schema < 51): niente da rimappare
		}
	}
}
