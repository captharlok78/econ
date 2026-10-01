package com.ncfsistemi.econ.db.table;

/**
 * Modello di stampa scelto dalla ditta per ogni documento (schema 33, GESTIONE_RAPPORTINI.md §16): solo lettura dal server.
 * Senza riga per il documento si usa il modello predefinito.
 */
public class DitteModelliStampa extends AbstractTable {
	public static final String NOME_TABELLA = "ditte_modelli_stampa";

	public static final String ID = "id";
	public static final String ID_DITTA = "id_ditta";
	public static final String DOCUMENTO = "documento";
	public static final String ID_MODELLO = "id_modello";

	public DitteModelliStampa() {
		setNomeTabella(NOME_TABELLA);
		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(DOCUMENTO, TEXT);
		aggiungiCampo(ID_MODELLO, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);
		aggiungiCampoChiave(ID);
	}
}
