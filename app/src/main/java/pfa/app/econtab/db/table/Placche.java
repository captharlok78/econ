package pfa.app.econtab.db.table;

import pfa.app.econtab.PlaccheDettaglioModActivity;

public class Placche extends AbstractTable {
	public static final String NOME_TABELLA = "placche";

	public static final String ID_PLACCA = "id_placca";
	public static final String ID_LINEA = "id_linea";
	public static final String NOME_PLACCA = "nome_placca";

	public Placche() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_PLACCA);

		aggiungiCampo(ID_PLACCA, INTEGER);
		aggiungiCampo(ID_LINEA, INTEGER);
		aggiungiCampo(NOME_PLACCA, TEXT);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_PLACCA);
	}

	@Override
	public String getCampoDescrizionePerSpinner() {
		// TODO Auto-generated method stub
		return NOME_PLACCA;
	}

	@Override
	public Class getDettaglioActivity() {
		// TODO Auto-generated method stub
		return PlaccheDettaglioModActivity.class;
	}

}
