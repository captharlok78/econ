package pfa.app.econtab.db.table;

import android.content.ContentValues;

public class Utenti extends AbstractTable {
	public static final String NOME_TABELLA = "utenti";

	public static final String ID_UTENTE = "id_utente";
	public static final String NOME = "nome";
	public static final String COGNOME = "cognome";
	public static final String LIVELLO = "livello";
	public static final String PASSWORD = "password";
	public static final String E_MAIL = "e_mail";
	public static final String TELEFONO = "telefono";
	public static final String RICORDA_PASSWORD = "ricorda_password";

	public static final int LIVELLO_AMMINISTRATORE = 1;

	public Utenti() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_UTENTE);

		aggiungiCampo(ID_UTENTE, INTEGER);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(COGNOME, TEXT);
		aggiungiCampo(LIVELLO, INTEGER);
		aggiungiCampo(PASSWORD, TEXT);
		aggiungiCampo(E_MAIL, TEXT);
		aggiungiCampo(TELEFONO, TEXT);
		aggiungiCampo(RICORDA_PASSWORD, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_UTENTE);
	}

	@Override
	public ContentValues getValoriInsertDefault() {
		// TODO Auto-generated method stub
		ContentValues val = super.getValoriInsertDefault();
		val.put(ID_UTENTE, VALORE_PRIMO_ID);
		val.put(NOME, "Admin");
		val.put(LIVELLO, LIVELLO_AMMINISTRATORE);
		return val;
	}
}
