package pfa.app.econtab.db.table;

import android.content.ContentValues;

public class UtentiDitta extends AbstractTable {
	public static final String NOME_TABELLA = "utenti_ditta";

	public static final String ID_UTENTE = "id_utente";
	public static final String ID_DITTA = "id_ditta";

	public UtentiDitta() {
		setNomeTabella(NOME_TABELLA);

		aggiungiCampo(ID_UTENTE, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_UTENTE);
		aggiungiCampoChiave(ID_DITTA);
	}

	@Override
	public ContentValues getValoriInsertDefault() {
		// TODO Auto-generated method stub
		ContentValues val = super.getValoriInsertDefault();
		val.put(ID_UTENTE, VALORE_PRIMO_ID);
		val.put(ID_DITTA, VALORE_PRIMO_ID);

		return val;
	}
}
