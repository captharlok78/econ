package com.ncfsistemi.econ.db.table;

/**
 * Livelli (piani) delle aree (schema 50, STUDIO_DESIGN_GRAFICA.md §20): catalogo del server (Sonata › Disegno), scaricato
 * sempre completo in sola lettura. aree.livello contiene il valore; il nome si legge qui (utils.LivelliAree).
 */
public class LivelliArea extends AbstractTable {
	public static final String NOME_TABELLA = "livelli_area";

	public static final String ID = "id";
	public static final String VALORE = "valore";
	public static final String NOME = "nome";
	public static final String ATTIVO = "attivo";

	public LivelliArea() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(VALORE, INTEGER);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(ATTIVO, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}
}
