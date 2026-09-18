package pfa.app.econtab.server;

import pfa.app.econtab.db.table.AbstractTable;

public class RecordEliminati extends AbstractTable {

	public static final String NOME_TABELLA = "record_eliminati";

	public static final String TABELLA = "tabella";
	public static final String CHIAVE_RECORD = "chiave_record";

	public RecordEliminati() {
		setNomeTabella(NOME_TABELLA);

		aggiungiCampo(TABELLA, TEXT);
		aggiungiCampo(CHIAVE_RECORD, TEXT);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);

		aggiungiCampoChiave(TABELLA);
		aggiungiCampoChiave(CHIAVE_RECORD);
	}

}
