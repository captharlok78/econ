package com.ncfsistemi.econ.db.table;

/**
 * Tipologie di giustificativo attive della ditta (schema 38): di sistema e della ditta, sola lettura, arrivano intere
 * con /api/mobile/giustificativi (utils.GiustificativiLocali).
 */
public class GiustificativiTipologie extends AbstractTable {
	public static final String NOME_TABELLA = "giustificativi_tipologie";

	public static final String ID = "id";
	public static final String CODICE = "codice";
	public static final String DESCRIZIONE = "descrizione";
	public static final String ICONA = "icona";
	public static final String COLORE = "colore";
	public static final String CONTABILE = "contabile";
	public static final String ORDINE = "ordine";

	public GiustificativiTipologie() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(CODICE, TEXT);
		aggiungiCampo(DESCRIZIONE, TEXT);
		aggiungiCampo(ICONA, TEXT);
		aggiungiCampo(COLORE, TEXT);
		aggiungiCampo(CONTABILE, INTEGER);
		aggiungiCampo(ORDINE, INTEGER);

		aggiungiCampoChiave(ID);
	}
}
