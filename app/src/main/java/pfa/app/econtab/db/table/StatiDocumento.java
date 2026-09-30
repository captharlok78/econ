package pfa.app.econtab.db.table;

/**
 * Stati dei documenti (dal server, sola lettura; GESTIONE_RAPPORTINI.md §10): per ogni ambito (es. "rapportini") uno stato
 * iniziale; uno stato non modificabile blocca testata e righe, uno non cancellabile impedisce l'eliminazione.
 */
public class StatiDocumento extends AbstractTable {
	public static final String NOME_TABELLA = "stati_documento";

	public static final String ID = "id";
	public static final String AMBITO = "ambito";
	public static final String CODICE = "codice";
	public static final String NOME = "nome";
	public static final String DESCRIZIONE = "descrizione";
	public static final String ORDINE = "ordine";
	public static final String INIZIALE = "iniziale";
	public static final String MODIFICABILE = "modificabile";
	public static final String CANCELLABILE = "cancellabile";
	public static final String COLORE = "colore";
	public static final String ATTIVO = "attivo";

	public static final String AMBITO_RAPPORTINI = "rapportini";
	public static final String CODICE_CONFERMATO = "CONFERMATO";
	/** Stato in cui va il rapportino quando il cliente firma, se il passaggio e' ammesso (GESTIONE_RAPPORTINI.md §13.6). */
	public static final String CODICE_FIRMATO = "FIRMATO";

	public StatiDocumento() {
		setNomeTabella(NOME_TABELLA);
		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(AMBITO, TEXT);
		aggiungiCampo(CODICE, TEXT);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(DESCRIZIONE, TEXT);
		aggiungiCampo(ORDINE, INTEGER);
		aggiungiCampo(INIZIALE, INTEGER);
		aggiungiCampo(MODIFICABILE, INTEGER);
		aggiungiCampo(CANCELLABILE, INTEGER);
		aggiungiCampo(COLORE, TEXT);
		aggiungiCampo(ATTIVO, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);
		aggiungiCampoChiave(ID);
	}

	@Override
	public boolean isCreabileDaApp() {
		return false;
	}
}
