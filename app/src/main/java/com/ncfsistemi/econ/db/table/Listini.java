package com.ncfsistemi.econ.db.table;

import android.content.ContentValues;

import java.util.ArrayList;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.Join;

public class Listini extends AbstractTable {
	public static final String NOME_TABELLA = "listini";

	public static final String CODICE_ARTICOLO = "codice_articolo";
	/** Id sul server: le righe di materiale dei rapportini (schema 26) vi fanno riferimento con id_listino. */
	public static final String ID = "id";
	public static final String ID_COSTRUTTORE = "id_costruttore";
	public static final String ID_LINEA = "id_linea";
	public static final String DESCRIZIONE = "descrizione";
	public static final String PRZ_ULTIMO_ACQUISTO = "prz_ultimo_acquisto";
	public static final String PRZ_LISTINO = "prz_listino";
	public static final String SCONTO = "sconto";
	/** Schema 30 (GESTIONE_RAPPORTINI.md §14): articolo della ditta (0 = catalogo generale), barcode, foto base64, settore. */
	public static final String ID_DITTA = "id_ditta";
	public static final String BARCODE = "barcode";
	public static final String FOTO = "foto";
	/** 1 = proposto nella ricerca del listino; 0 = articolo usato solo nella riga in cui e' nato. */
	public static final String NEL_LISTINO = "nel_listino";
	public static final String ID_SETTORE = "id_settore";
	/**
	 * Schema 42 (STUDIO_BARCODE_ARTICOLI.md §4.2): BARCODE e' il barcode della ditta; quello del fornitore e quello del
	 * costruttore (dal listino, o scritto dalla ditta se il listino non ce l'ha) nelle loro colonne.
	 */
	public static final String BARCODE_FORNITORE = "barcode_fornitore";
	public static final String BARCODE_COSTRUTTORE = "barcode_costruttore";
	/** Schema 43 (STUDIO_CODICE_ARTICOLO_DITTA.md §9): codice aziendale, lo calcola il server (anche per gli articoli del listino). */
	public static final String CODICE_DITTA = "codice_ditta";
	/** Schema 44 (STUDIO_LISTINO_E_ARTICOLI.md §6.3, D4): prezzo di vendita proposto, lo calcola il server (fisso, costo + ricarico, listino). */
	public static final String PRZ_VENDITA = "prz_vendita";
	/** Schema 44 (D8): famiglia dell'articolo, per proporre i componenti della categoria nella gestione elettrica. */
	public static final String ID_FAMIGLIA = "id_famiglia";

	/**
	 * Prezzo di vendita proposto dal server (schema 44, D4), 0 se non c'è (catalogo di un server precedente): allora vale
	 * la regola di prima (listino meno lo sconto).
	 */
	public static double prezzoVendita(android.content.ContentValues l) {
		if (l == null) return 0;
		Double v = l.getAsDouble(PRZ_VENDITA);
		return v != null && v > 0 ? v : 0;
	}

	public Listini() {
		setNomeTabella(NOME_TABELLA);

		aggiungiCampo(CODICE_ARTICOLO, TEXT);
		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(ID_COSTRUTTORE, INTEGER);
		aggiungiCampo(ID_LINEA, INTEGER);
		aggiungiCampo(DESCRIZIONE, TEXT);
		aggiungiCampo(PRZ_ULTIMO_ACQUISTO, NUMERIC);
		aggiungiCampo(PRZ_LISTINO, NUMERIC);
		aggiungiCampo(SCONTO, NUMERIC);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(BARCODE, TEXT);
		aggiungiCampo(FOTO, TEXT);
		aggiungiCampo(NEL_LISTINO, INTEGER);
		aggiungiCampo(ID_SETTORE, INTEGER);
		aggiungiCampo(BARCODE_FORNITORE, TEXT);
		aggiungiCampo(BARCODE_COSTRUTTORE, TEXT);
		aggiungiCampo(CODICE_DITTA, TEXT);
		aggiungiCampo(PRZ_VENDITA, NUMERIC);
		aggiungiCampo(ID_FAMIGLIA, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		// Schema 40 (STUDIO_PACCHETTI_LISTINI.md, L8): la chiave e' l'id del server, perche' lo stesso codice puo' essere di
		// piu' costruttori. Gli articoli nati sul dispositivo hanno id negativo fino all'invio (come le altre tabelle).
		aggiungiCampoChiave(ID);
	}

	/** Condizione "uno dei tre barcode e' ?" (con l'alias della tabella, es. "l."): tre parametri uguali. */
	public static String condizioneBarcode(String alias) {
		return "(" + alias + BARCODE + "=? or " + alias + BARCODE_FORNITORE + "=? or " + alias + BARCODE_COSTRUTTORE + "=?)";
	}

	/** Prefisso del codice aziendale della ditta ("2."), come sul server. */
	public static String prefissoDitta() {
		return com.ncfsistemi.econ.utils.Sessione.getDittaSelezionata() + ".";
	}

	/** Articolo del listino generale (non della ditta): sul dispositivo si cambiano solo i barcode. */
	public static boolean isDelListino(ContentValues articolo) {
		Integer id = articolo != null ? articolo.getAsInteger(ID) : null;
		Integer ditta = articolo != null ? articolo.getAsInteger(ID_DITTA) : null;
		return id != null && id > 0 && (ditta == null || ditta == 0);
	}

	/** Id per un articolo nuovo nato sul dispositivo: negativo, sotto il minimo gia' usato. */
	public static int nuovoIdLocale(DbInterno db) {
		ContentValues m = db.getRecord("select min(coalesce(" + ID + ", 0)) as m from " + NOME_TABELLA);
		Integer minimo = m != null ? m.getAsInteger("m") : null;
		return Math.min(minimo != null ? minimo : 0, -1) - 1;
	}

	/** Chi inserisce senza id (maschere dei preventivi, associazioni codici) riceve un id locale negativo. */
	@Override
	public long inserisciRecord(DbInterno db, ContentValues val) {
		Integer id = val.containsKey(ID) && val.get(ID) != null && !"".equals(val.getAsString(ID)) ? val.getAsInteger(ID) : null;
		if (id == null || id == 0) {
			val.put(ID, nuovoIdLocale(db));
			if (!val.containsKey(IN_SERVER)) val.put(IN_SERVER, 0);
		}
		return super.inserisciRecord(db, val);
	}

	/**
	 * Articolo per codice, preferendo il costruttore indicato (0 = qualsiasi): con lo schema 40 lo stesso codice puo' essere
	 * di piu' costruttori. Null se non c'e'.
	 */
	public static ContentValues perCodice(DbInterno db, String codice, int idCostruttore) {
		if (codice == null || codice.trim().isEmpty()) return null;
		String c = codice.trim().replace("'", "''");
		String ordine = idCostruttore != 0 ? " order by case when " + ID_COSTRUTTORE + "=" + idCostruttore + " then 0 else 1 end, " + ID : " order by " + ID;
		return db.getRecord("select * from " + NOME_TABELLA + " where " + CODICE_ARTICOLO + "='" + c + "'" + ordine + " limit 1");
	}

	public ContentValues getRecordConFornitoreELinea(ContentValues where, DbInterno db) {
		Join j0 = new Join(NOME_TABELLA, Costruttori.NOME_TABELLA, Join.LEFT_JOIN);
		j0.addCampiDiJoin(ID_COSTRUTTORE, Costruttori.ID_COSTRUTTORE);

		ArrayList<Object> res = db.eseguiSelect("Select " + NOME_TABELLA + ".*," + Costruttori.SIGLA_METEL + ","
				+ Costruttori.RAGIONE_SOCIALE + " from " + NOME_TABELLA + j0.getSQLJoin() + " where " + CODICE_ARTICOLO + "=? order by " + NOME_TABELLA + "." + ID,
				new String[] { where.getAsString(CODICE_ARTICOLO) });

		if (res.size() > 0) {
			ContentValues rec = (ContentValues) res.get(0);
			int idLinea = rec.getAsInteger(ID_LINEA);
			// Se ho la linea allora prenndo il fornitore dalla linea
			rec.put(Linee.NOME_LINEA, "");
			if (idLinea != 0) {
				Linee tabLinee = new Linee();
				ContentValues valLinea = tabLinee.getRecordConFornitore(idLinea, db);
				if (valLinea != null) {
					rec.put(Costruttori.SIGLA_METEL, valLinea.getAsString(Costruttori.SIGLA_METEL));
					rec.put(Costruttori.RAGIONE_SOCIALE, valLinea.getAsString(Costruttori.RAGIONE_SOCIALE));
					rec.put(Linee.NOME_LINEA, valLinea.getAsString(Linee.NOME_LINEA));
				}
			}
			return rec;

		}
		return null;

	}

}
