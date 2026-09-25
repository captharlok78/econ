package pfa.app.econtab.db.table;

/** Operatori di una squadra (solo lettura, dal server). Il coordinatore non e' tra i membri: e' su squadre. */
public class SquadreMembri extends AbstractTable {
	public static final String NOME_TABELLA = "squadre_membri";

	public static final String ID = "id";
	public static final String ID_SQUADRA = "id_squadra";
	public static final String ID_UTENTE_DITTA = "id_utente_ditta";

	public SquadreMembri() {
		setNomeTabella(NOME_TABELLA);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(ID_SQUADRA, INTEGER);
		aggiungiCampo(ID_UTENTE_DITTA, INTEGER);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}
}
