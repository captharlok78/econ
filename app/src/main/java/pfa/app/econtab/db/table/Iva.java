package pfa.app.econtab.db.table;

import pfa.app.econtab.IvaDettaglioActivity;

public class Iva extends AbstractTable {
	public static final String NOME_TABELLA = "iva";

	public static final String CODICE_IVA = "codice_iva";
	public static final String ALIQUOTA = "aliquota";

	public Iva() {
		setNomeTabella(NOME_TABELLA);

		aggiungiCampo(CODICE_IVA, TEXT);
		aggiungiCampo(ALIQUOTA, NUMERIC);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(CODICE_IVA);
	}

	@Override
	public Class getDettaglioActivity() {
		// TODO Auto-generated method stub
		return IvaDettaglioActivity.class;
	}

	@Override
	protected int getMassimoNumeroRecordLicenzaGratis() {
		// TODO Auto-generated method stub
		return 2;
	}
}
