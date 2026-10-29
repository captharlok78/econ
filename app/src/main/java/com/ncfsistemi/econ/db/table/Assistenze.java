package com.ncfsistemi.econ.db.table;

/**
 * Assistenze a tempo dei pezzi montati (solo lettura, dal server, STUDIO_ASSISTENZE_TEMPO.md §10): una per pezzo, nasce
 * alla conferma del rapportino; prossima scadenza = (ultimo controllo, altrimenti installazione) + mesi. Pianificata =
 * collegata a un blocco della pianificazione (id_pianificazione).
 */
public class Assistenze extends AbstractTable {
	public static final String NOME_TABELLA = "assistenze";

	public static final String ID = "id";
	public static final String ID_DITTA = "id_ditta";
	public static final String ID_RIGA_RAPPORTINO = "id_riga_rapportino";
	public static final String ID_LISTINO = "id_listino";
	public static final String ID_CANTIERE = "id_cantiere";
	public static final String ID_CLIENTE = "id_cliente";
	public static final String MATRICOLA = "matricola";
	public static final String UBICAZIONE = "ubicazione";
	public static final String DATA_INSTALLAZIONE = "data_installazione";
	public static final String ULTIMO_CONTROLLO = "ultimo_controllo";
	public static final String PROSSIMA_SCADENZA = "prossima_scadenza";
	public static final String MESI = "mesi";
	public static final String GIORNI_CHECK = "giorni_check";
	public static final String STATO = "stato";
	public static final String ID_PIANIFICAZIONE = "id_pianificazione";
	public static final String ID_SOSTITUISCE = "id_sostituisce";
	public static final String NOTE = "note";

	public static final String ATTIVA = "attiva";

	public Assistenze() {
		setNomeTabella(NOME_TABELLA);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(ID_RIGA_RAPPORTINO, INTEGER);
		aggiungiCampo(ID_LISTINO, INTEGER);
		aggiungiCampo(ID_CANTIERE, INTEGER);
		aggiungiCampo(ID_CLIENTE, INTEGER);
		aggiungiCampo(MATRICOLA, TEXT);
		aggiungiCampo(UBICAZIONE, TEXT);
		aggiungiCampo(DATA_INSTALLAZIONE, DATE);
		aggiungiCampo(ULTIMO_CONTROLLO, DATE);
		aggiungiCampo(PROSSIMA_SCADENZA, DATE);
		aggiungiCampo(MESI, INTEGER);
		aggiungiCampo(GIORNI_CHECK, INTEGER);
		aggiungiCampo(STATO, TEXT);
		aggiungiCampo(ID_PIANIFICAZIONE, INTEGER);
		aggiungiCampo(ID_SOSTITUISCE, INTEGER);
		aggiungiCampo(NOTE, TEXT);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}
}
