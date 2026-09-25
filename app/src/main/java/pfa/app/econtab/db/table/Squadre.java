package pfa.app.econtab.db.table;

/** Squadre della ditta (solo lettura, dal server): un coordinatore (id_utente_ditta) e i membri in squadre_membri. */
public class Squadre extends AbstractTable {
	public static final String NOME_TABELLA = "squadre";

	public static final String ID_SQUADRA = "id_squadra";
	public static final String ID_DITTA = "id_ditta";
	public static final String NOME = "nome";
	public static final String ID_COORDINATORE = "id_coordinatore";
	public static final String ATTIVA = "attiva";
	public static final String NOTE = "note";

	public Squadre() {
		setNomeTabella(NOME_TABELLA);

		aggiungiCampo(ID_SQUADRA, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(ID_COORDINATORE, INTEGER);
		aggiungiCampo(ATTIVA, INTEGER);
		aggiungiCampo(NOTE, TEXT);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_SQUADRA);
	}
}
