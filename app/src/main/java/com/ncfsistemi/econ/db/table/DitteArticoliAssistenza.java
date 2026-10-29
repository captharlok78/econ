package com.ncfsistemi.econ.db.table;

/**
 * Articoli in assistenza a tempo della ditta (solo lettura, dal server, STUDIO_ASSISTENZE_TEMPO.md §10): la riga c'e' =
 * l'articolo richiede assistenza; mesi e giorni di preavviso vuoti = quelli della ditta.
 */
public class DitteArticoliAssistenza extends AbstractTable {
	public static final String NOME_TABELLA = "ditte_articoli_assistenza";

	public static final String ID = "id";
	public static final String ID_DITTA = "id_ditta";
	public static final String ID_LISTINO = "id_listino";
	public static final String MESI = "mesi";
	public static final String GIORNI_CHECK = "giorni_check";

	public DitteArticoliAssistenza() {
		setNomeTabella(NOME_TABELLA);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(ID_LISTINO, INTEGER);
		aggiungiCampo(MESI, INTEGER);
		aggiungiCampo(GIORNI_CHECK, INTEGER);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}
}
