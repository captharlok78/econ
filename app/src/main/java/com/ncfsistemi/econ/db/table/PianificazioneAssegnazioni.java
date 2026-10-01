package com.ncfsistemi.econ.db.table;

/**
 * Blocchi di pianificazione (solo lettura, dal server): un cantiere per N minuti in una fascia di un giorno,
 * assegnato a una squadra (id_squadra, esclusi gli operatori in pianificazione_esclusioni) oppure a un
 * singolo operatore (id_utente_ditta).
 */
public class PianificazioneAssegnazioni extends AbstractTable {
	public static final String NOME_TABELLA = "pianificazione_assegnazioni";

	public static final String ID = "id";
	public static final String ID_DITTA = "id_ditta";
	public static final String ID_CANTIERE = "id_cantiere";
	public static final String DATA = "data";
	public static final String FASCIA = "fascia";
	public static final String MINUTI = "minuti";
	public static final String ID_SQUADRA = "id_squadra";
	public static final String ID_UTENTE_DITTA = "id_utente_ditta";

	public PianificazioneAssegnazioni() {
		setNomeTabella(NOME_TABELLA);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(ID_CANTIERE, INTEGER);
		aggiungiCampo(DATA, DATE);
		aggiungiCampo(FASCIA, TEXT);
		aggiungiCampo(MINUTI, INTEGER);
		aggiungiCampo(ID_SQUADRA, INTEGER);
		aggiungiCampo(ID_UTENTE_DITTA, INTEGER);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}
}
