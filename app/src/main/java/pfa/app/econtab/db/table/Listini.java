package pfa.app.econtab.db.table;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;

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
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(CODICE_ARTICOLO);
	}

	public ContentValues getRecordConFornitoreELinea(ContentValues where, DbInterno db) {
		Join j0 = new Join(NOME_TABELLA, Costruttori.NOME_TABELLA, Join.LEFT_JOIN);
		j0.addCampiDiJoin(ID_COSTRUTTORE, Costruttori.ID_COSTRUTTORE);

		ArrayList<Object> res = db.eseguiSelect("Select " + NOME_TABELLA + ".*," + Costruttori.SIGLA_METEL + ","
				+ Costruttori.RAGIONE_SOCIALE + " from " + NOME_TABELLA + j0.getSQLJoin() + " where " + CODICE_ARTICOLO + "=?",
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
