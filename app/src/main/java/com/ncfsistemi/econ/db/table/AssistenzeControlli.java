package com.ncfsistemi.econ.db.table;

/**
 * Controlli delle assistenze a tempo segnati nel rapportino (STUDIO_ASSISTENZE_TEMPO.md §10): uno per assistenza e
 * rapportino, si inviano al server con il rapportino e contano dalla sua conferma (la scadenza la ricalcola il server).
 * Esito: eseguito, sostituito o rimosso.
 */
public class AssistenzeControlli extends AbstractTable {
	public static final String NOME_TABELLA = "assistenze_controlli";

	public static final String ID = "id";
	public static final String ID_DITTA = "id_ditta";
	public static final String ID_ASSISTENZA = "id_assistenza";
	public static final String ID_RAPPORTINO = "id_rapportino";
	public static final String DATA = "data";
	public static final String ID_UTENTE_DITTA = "id_utente_ditta";
	public static final String ESITO = "esito";
	public static final String NOTE = "note";

	public static final String ESEGUITO = "eseguito";
	public static final String SOSTITUITO = "sostituito";
	public static final String RIMOSSO = "rimosso";
	/** Esiti in ordine, con il nome da mostrare. */
	public static final String[][] ESITI = { { ESEGUITO, "Eseguito" }, { SOSTITUITO, "Sostituito" }, { RIMOSSO, "Rimosso" } };

	public AssistenzeControlli() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(ID_ASSISTENZA, INTEGER);
		aggiungiCampo(ID_RAPPORTINO, INTEGER);
		aggiungiCampo(DATA, DATE);
		aggiungiCampo(ID_UTENTE_DITTA, INTEGER);
		aggiungiCampo(ESITO, TEXT);
		aggiungiCampo(NOTE, TEXT);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}

	public static String nomeEsito(String esito) {
		for (String[] e : ESITI) {
			if (e[0].equals(esito)) return e[1];
		}
		return esito;
	}
}
