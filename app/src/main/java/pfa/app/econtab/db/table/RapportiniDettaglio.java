package pfa.app.econtab.db.table;

public class RapportiniDettaglio extends AbstractTable {
	public static final String NOME_TABELLA = "rapportini_dettaglio";

	public static final String ID_RAPPORTINO_DETTAGLIO = "id_rapportino_dettaglio";
    public static final String ID_RAPPORTINO = "id_rapportino";
    public static final String ID_MANODOPERA = "id_manodopera";
    public static final String ORE = "ore";
    public static final String NOTE = "note";

	
	public RapportiniDettaglio() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_RAPPORTINO_DETTAGLIO);

		aggiungiCampo(ID_RAPPORTINO_DETTAGLIO, INTEGER);
        aggiungiCampo(ID_RAPPORTINO, INTEGER);
        aggiungiCampo(ID_MANODOPERA, INTEGER);
        aggiungiCampo(ORE,NUMERIC);
        aggiungiCampo(NOTE, TEXT);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_RAPPORTINO_DETTAGLIO);
	}


    @Override
    protected int getMassimoNumeroRecordLicenzaGratis() {
        return 20;
    }
}
