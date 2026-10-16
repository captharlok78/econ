package com.ncfsistemi.econ.db.table;

/**
 * Giustificativi della persona (schema 38, STUDIO_GIUSTIFICATIVI_APP.md): ferie, permessi, malattia… chiesti dal
 * calendario e approvati dal portale. Fuori dalla sync generale: li allinea utils.GiustificativiLocali con
 * /api/mobile/giustificativi. Id negativo = nuovo non ancora arrivato al server; DA_INVIARE = nuovo o cambiato da
 * inviare; ELIMINATO = da togliere sul server (resta finché il server non conferma).
 */
public class Giustificativi extends AbstractTable {
	public static final String NOME_TABELLA = "giustificativi";

	public static final String ID = "id";
	public static final String DATA = "data";
	public static final String ID_TIPOLOGIA = "id_tipologia";
	public static final String ORE = "ore";
	public static final String NOTE = "note";
	public static final String STATO = "stato";
	public static final String GRUPPO = "gruppo";
	public static final String MOTIVO_RIFIUTO = "motivo_rifiuto";
	public static final String APPROVATO_DA = "approvato_da";
	public static final String APPROVATO_IL = "approvato_il";
	public static final String ORIGINE = "origine";
	public static final String DA_INVIARE = "da_inviare";
	public static final String ELIMINATO = "eliminato";
	public static final String ERRORE = "errore";

	public static final String STATO_RICHIESTO = "RICHIESTO";
	public static final String STATO_APPROVATO = "APPROVATO";
	public static final String STATO_RIFIUTATO = "RIFIUTATO";

	public Giustificativi() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(DATA, TEXT);
		aggiungiCampo(ID_TIPOLOGIA, INTEGER);
		aggiungiCampo(ORE, NUMERIC);
		aggiungiCampo(NOTE, TEXT);
		aggiungiCampo(STATO, TEXT);
		aggiungiCampo(GRUPPO, TEXT);
		aggiungiCampo(MOTIVO_RIFIUTO, TEXT);
		aggiungiCampo(APPROVATO_DA, TEXT);
		aggiungiCampo(APPROVATO_IL, TEXT);
		aggiungiCampo(ORIGINE, TEXT);
		aggiungiCampo(DA_INVIARE, INTEGER);
		aggiungiCampo(ELIMINATO, INTEGER);
		aggiungiCampo(ERRORE, TEXT);

		aggiungiCampoChiave(ID);
	}
}
