package pfa.app.econtab.db.table;

public class RapportiniDettaglio extends AbstractTable {
	public static final String NOME_TABELLA = "rapportini_dettaglio";

	public static final String ID_RAPPORTINO_DETTAGLIO = "id_rapportino_dettaglio";
    public static final String ID_RAPPORTINO = "id_rapportino";
    public static final String ID_MANODOPERA = "id_manodopera";
    public static final String ORE = "ore";
    public static final String NOTE = "note";
    public static final String UNITA_MISURA = "unita_misura";

	
	public RapportiniDettaglio() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_RAPPORTINO_DETTAGLIO);

		aggiungiCampo(ID_RAPPORTINO_DETTAGLIO, INTEGER);
        aggiungiCampo(ID_RAPPORTINO, INTEGER);
        aggiungiCampo(ID_MANODOPERA, INTEGER);
        aggiungiCampo(ORE,NUMERIC);
        aggiungiCampo(NOTE, TEXT);
        aggiungiCampo(UNITA_MISURA, TEXT);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_RAPPORTINO_DETTAGLIO);
	}


    /** L'unita' di misura di una riga di rapportino e' quella della sua manodopera (default ore), se non indicata. */
    @Override
    public long inserisciRecord(pfa.app.econtab.db.DbInterno db, android.content.ContentValues val) {
        String udm = val.getAsString(UNITA_MISURA);
        if ((udm == null || udm.trim().isEmpty()) && val.containsKey(ID_MANODOPERA)) {
            android.content.ContentValues where = new android.content.ContentValues();
            where.put(Manodopera.ID_MANODOPERA, val.getAsInteger(ID_MANODOPERA));
            android.content.ContentValues mano = db.getRecord(new Manodopera(), where);
            String daManodopera = mano != null ? mano.getAsString(Manodopera.UNITA_MISURA) : null;
            val.put(UNITA_MISURA, daManodopera != null && !daManodopera.trim().isEmpty() ? daManodopera : Manodopera.UNITA_MISURA_DEFAULT);
        }
        return super.inserisciRecord(db, val);
    }

    @Override
    protected int getMassimoNumeroRecordLicenzaGratis() {
        return 20;
    }
}
