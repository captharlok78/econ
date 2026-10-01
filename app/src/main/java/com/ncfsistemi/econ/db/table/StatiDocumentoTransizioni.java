package com.ncfsistemi.econ.db.table;

/** Passaggi ammessi tra stati dei documenti (dal server, sola lettura), eventualmente solo per l'amministratore. */
public class StatiDocumentoTransizioni extends AbstractTable {
	public static final String NOME_TABELLA = "stati_documento_transizioni";

	public static final String ID = "id";
	public static final String ID_STATO_DA = "id_stato_da";
	public static final String ID_STATO_A = "id_stato_a";
	public static final String SOLO_AMMINISTRATORE = "solo_amministratore";

	public StatiDocumentoTransizioni() {
		setNomeTabella(NOME_TABELLA);
		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(ID_STATO_DA, INTEGER);
		aggiungiCampo(ID_STATO_A, INTEGER);
		aggiungiCampo(SOLO_AMMINISTRATORE, INTEGER);
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
