package com.ncfsistemi.econ.db.table;

/** Operatore escluso da un blocco di squadra (solo lettura, dal server). */
public class PianificazioneEsclusioni extends AbstractTable {
	public static final String NOME_TABELLA = "pianificazione_esclusioni";

	public static final String ID = "id";
	public static final String ID_ASSEGNAZIONE = "id_assegnazione";
	public static final String ID_UTENTE_DITTA = "id_utente_ditta";

	public PianificazioneEsclusioni() {
		setNomeTabella(NOME_TABELLA);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(ID_ASSEGNAZIONE, INTEGER);
		aggiungiCampo(ID_UTENTE_DITTA, INTEGER);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}
}
