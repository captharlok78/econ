package pfa.app.econtab.db.table;

/**
 * Modelli di stampa (schema 31, GESTIONE_RAPPORTINI.md §15): solo lettura dal server, lo standard (id_ditta 0) e quelli
 * della ditta. HTML con i segnaposti di stampa.MiniMustache, gli stessi del server.
 */
public class ModelliStampa extends AbstractTable {
	public static final String NOME_TABELLA = "modelli_stampa";

	public static final String ID = "id";
	public static final String ID_DITTA = "id_ditta";
	public static final String DOCUMENTO = "documento";
	public static final String NOME = "nome";
	public static final String CONTENUTO = "contenuto";
	public static final String PREDEFINITO = "predefinito";
	public static final String ORDINE = "ordine";

	public static final String DOC_RAPPORTINO = "rapportino";

	public ModelliStampa() {
		setNomeTabella(NOME_TABELLA);
		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(DOCUMENTO, TEXT);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(CONTENUTO, TEXT);
		aggiungiCampo(PREDEFINITO, INTEGER);
		aggiungiCampo(ATTIVO, INTEGER);
		aggiungiCampo(ORDINE, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);
		aggiungiCampoChiave(ID);
	}
}
