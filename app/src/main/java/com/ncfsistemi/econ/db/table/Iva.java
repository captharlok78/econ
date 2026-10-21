package com.ncfsistemi.econ.db.table;

import com.ncfsistemi.econ.IvaDettaglioActivity;

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

	/** Scelte e schede: il codice con l'aliquota ("022 · 22%"), che da solo non dice niente. */
	@Override
	public String getSQLPerSpinner(android.content.ContentValues filtro) {
		String where = getFiltro(filtro);
		return "SELECT " + CODICE_IVA + ", " + ALIQUOTA + " FROM " + NOME_TABELLA + (where.isEmpty() ? "" : " WHERE " + where)
				+ " ORDER BY " + CODICE_IVA;
	}

	@Override
	public String formattaDescrizioneSpinner(android.content.ContentValues val) {
		String codice = val.getAsString(CODICE_IVA);
		Double aliquota = val.getAsDouble(ALIQUOTA);
		if (aliquota == null) return codice;
		return codice + " · " + java.math.BigDecimal.valueOf(aliquota).stripTrailingZeros().toPlainString().replace('.', ',') + "%";
	}
}
