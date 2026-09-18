package pfa.app.econtab.db.table;

public class LivelliUtente extends AbstractTable {
	public static final String NOME_TABELLA = "livelli_utente";

	public static final String ID_LIVELLO = "id_livello";
	public static final String NOME_LIVELLO = "nome_livello";
	public static final String VISUALIZZAZIONE_PREZZI = "visualizzazione_prezzi";
	public static final String INSERIMENTO_CANCELLAZIONE_PREV = "inserimento_cancellazione_prev";

	public LivelliUtente() {
		setNomeTabella(NOME_TABELLA);
		aggiungiCampo(ID_LIVELLO, INTEGER);
		aggiungiCampo(NOME_LIVELLO, TEXT);
		aggiungiCampo(VISUALIZZAZIONE_PREZZI, INTEGER);
		aggiungiCampo(INSERIMENTO_CANCELLAZIONE_PREV, INTEGER);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_LIVELLO);

	}

}
