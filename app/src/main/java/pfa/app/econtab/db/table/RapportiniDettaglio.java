package pfa.app.econtab.db.table;

/**
 * Riga di un rapportino: lavoro (cosa si e' fatto) oppure viaggio di andata/ritorno (solo tempo), con orari
 * facoltativi (HH:MM) e, facoltativamente, la riga di manodopera (MD) dell'ordine a cui si imputano le ore.
 * Le ore sono solo sugli operatori della riga (rapportini_dettaglio_operatori). a_costo = 0: le ore restano ma non
 * entrano nel costo (es. viaggio non addebitato). Stessa struttura del server (STUDIO_PIANIFICAZIONE_E_LAVORI §9).
 */
public class RapportiniDettaglio extends AbstractTable {
	public static final String NOME_TABELLA = "rapportini_dettaglio";

	public static final String ID_RAPPORTINO_DETTAGLIO = "id_rapportino_dettaglio";
    public static final String ID_RAPPORTINO = "id_rapportino";
    public static final String TIPO = "tipo";
    public static final String ORA_INIZIO = "ora_inizio";
    public static final String ORA_FINE = "ora_fine";
    public static final String A_COSTO = "a_costo";
    public static final String DESCRIZIONE = "descrizione";
    public static final String NOTE = "note";
    public static final String ID_PREVENTIVO_DETTAGLIO = "id_preventivo_dettaglio";

    public static final String TIPO_VIAGGIO_ANDATA = "VA";
    public static final String TIPO_LAVORO = "LA";
    public static final String TIPO_VIAGGIO_RITORNO = "VR";
    /** Tipi nell'ordine in cui si mostrano le righe: andata, lavoro, ritorno. */
    public static final String[] TIPI = { TIPO_VIAGGIO_ANDATA, TIPO_LAVORO, TIPO_VIAGGIO_RITORNO };

	public RapportiniDettaglio() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_RAPPORTINO_DETTAGLIO);

		aggiungiCampo(ID_RAPPORTINO_DETTAGLIO, INTEGER);
        aggiungiCampo(ID_RAPPORTINO, INTEGER);
        aggiungiCampo(TIPO, TEXT);
        aggiungiCampo(ORA_INIZIO, TEXT);
        aggiungiCampo(ORA_FINE, TEXT);
        aggiungiCampo(A_COSTO, INTEGER);
        aggiungiCampo(DESCRIZIONE, TEXT);
        aggiungiCampo(NOTE, TEXT);
        aggiungiCampo(ID_PREVENTIVO_DETTAGLIO, INTEGER);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_RAPPORTINO_DETTAGLIO);
	}

    public static boolean isViaggio(String tipo) {
        return TIPO_VIAGGIO_ANDATA.equals(tipo) || TIPO_VIAGGIO_RITORNO.equals(tipo);
    }

    /** Espressione SQL per ordinare le righe come nella scheda: andata, lavoro, ritorno, poi per ora di inizio. */
    public static String sqlOrdinamento(String alias) {
        String a = alias != null ? alias + "." : "";
        return "(case " + a + TIPO + " when '" + TIPO_VIAGGIO_ANDATA + "' then 0 when '" + TIPO_VIAGGIO_RITORNO + "' then 2 else 1 end), "
                + "coalesce(" + a + ORA_INIZIO + ",'') = '', " + a + ORA_INIZIO + ", " + a + ID_RAPPORTINO_DETTAGLIO;
    }

    /**
     * Gli operatori della riga si tolgono solo in locale: sul server li cancella la riga stessa (ON DELETE CASCADE),
     * quindi non vanno in record_eliminati (il server risponderebbe "record non trovato").
     */
    @Override
    protected void eliminaCorrelati(pfa.app.econtab.db.DbInterno db, android.content.ContentValues val) {
        Integer idRiga = val.getAsInteger(ID_RAPPORTINO_DETTAGLIO);
        if (idRiga != null) {
            android.content.ContentValues where = new android.content.ContentValues();
            where.put(RapportiniDettaglioOperatori.ID_RAPPORTINO_DETTAGLIO, idRiga);
            db.delete(RapportiniDettaglioOperatori.NOME_TABELLA, where);
        }
        super.eliminaCorrelati(db, val);
    }

    @Override
    protected int getMassimoNumeroRecordLicenzaGratis() {
        return 20;
    }
}
