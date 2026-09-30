package pfa.app.econtab.db.table;

/** Referenti dei clienti (GESTIONE_CLIENTI.md §6, schema 28): n per cliente con i loro recapiti (del cliente, non di un indirizzo). */
public class ClientiReferenti extends AbstractTable {
	public static final String NOME_TABELLA = "clienti_referenti";

	public static final String ID = "id";
	public static final String ID_DITTA = "id_ditta";
	public static final String ID_ANAGRAFICA = "id_anagrafica";
	public static final String NOME = "nome";
	public static final String RUOLO = "ruolo";
	public static final String TELEFONO = "telefono";
	public static final String CELLULARE = "cellulare";
	public static final String EMAIL = "email";
	public static final String NOTE = "note";

	public ClientiReferenti() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(ID_ANAGRAFICA, INTEGER);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(RUOLO, TEXT);
		aggiungiCampo(TELEFONO, TEXT);
		aggiungiCampo(CELLULARE, TEXT);
		aggiungiCampo(EMAIL, TEXT);
		aggiungiCampo(NOTE, TEXT);
		aggiungiCampo(ATTIVO, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}
}
