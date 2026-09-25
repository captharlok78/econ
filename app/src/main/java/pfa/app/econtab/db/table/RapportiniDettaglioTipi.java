package pfa.app.econtab.db.table;

/** Tipi di riga dei rapportini (dal server, sola lettura): codice, descrizione, ordine nella scheda, categoria. */
public class RapportiniDettaglioTipi extends AbstractTable {
	public static final String NOME_TABELLA = "rapportini_dettaglio_tipi";

	public static final String ID = "id";
	public static final String CODICE = "codice";
	public static final String DESCRIZIONE = "descrizione";
	public static final String ORDINE = "ordine";
	public static final String CATEGORIA = "categoria";

	public static final String CATEGORIA_LAVORO = "LAVORO";
	public static final String CATEGORIA_VIAGGIO = "VIAGGIO";
	public static final String CATEGORIA_MATERIALE = "MATERIALE";

	public RapportiniDettaglioTipi() {
		setNomeTabella(NOME_TABELLA);
		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(CODICE, TEXT);
		aggiungiCampo(DESCRIZIONE, TEXT);
		aggiungiCampo(ORDINE, INTEGER);
		aggiungiCampo(CATEGORIA, TEXT);
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
