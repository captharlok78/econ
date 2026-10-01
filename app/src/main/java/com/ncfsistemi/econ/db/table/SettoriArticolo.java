package com.ncfsistemi.econ.db.table;

/**
 * Settori degli articoli (schema 30, GESTIONE_RAPPORTINI.md §14): libreria del server, quelli generali (id_ditta 0) piu'
 * quelli aggiunti dalla ditta. Si scelgono (e si aggiungono) nel nuovo articolo del rapportino.
 */
public class SettoriArticolo extends AbstractTable {
	public static final String NOME_TABELLA = "settori_articolo";

	public static final String ID = "id";
	public static final String NOME = "nome";
	public static final String ICONA = "icona";
	public static final String ORDINE = "ordine";
	public static final String ID_DITTA = "id_ditta";

	public SettoriArticolo() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(ICONA, TEXT);
		aggiungiCampo(ORDINE, INTEGER);
		aggiungiCampo(ATTIVO, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}
}
