package pfa.app.econtab.db.table;

/** Tipologie di modello di stampa (schema 33, GESTIONE_RAPPORTINI.md §16): solo lettura dal server. */
public class TipologieModello extends AbstractTable {
	public static final String NOME_TABELLA = "tipologie_modello";

	public static final String ID = "id";
	public static final String CODICE = "codice";
	public static final String DESCRIZIONE = "descrizione";
	public static final String DOCUMENTO = "documento";
	public static final String ORDINE = "ordine";

	public TipologieModello() {
		setNomeTabella(NOME_TABELLA);
		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(CODICE, TEXT);
		aggiungiCampo(DESCRIZIONE, TEXT);
		aggiungiCampo(DOCUMENTO, TEXT);
		aggiungiCampo(ORDINE, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);
		aggiungiCampoChiave(ID);
	}
}
