package pfa.app.econtab.db.table;

public class Foto extends AbstractTable {
	public static final String NOME_TABELLA = "foto";

	public static final String ID_FOTO = "id_foto";
	public static final String FOTO = "foto";
	public static final String NOTE = "note";
	public static final String ID_CANTIERE = "id_cantiere";
	public static final String ID_UNITA = "id_unita";
	public static final String ID_AREA = "id_area";
	public static final String ID_LOCALE = "id_locale";
	public static final String POS_X = "pos_x";
	public static final String POS_Y = "pos_y";

	public Foto() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_FOTO);

		aggiungiCampo(ID_FOTO, INTEGER);
		aggiungiCampo(FOTO, TEXT);
		aggiungiCampo(NOTE, TEXT);
		aggiungiCampo(ID_CANTIERE, INTEGER);
		aggiungiCampo(ID_UNITA, INTEGER);
		aggiungiCampo(ID_AREA, INTEGER);
		aggiungiCampo(ID_LOCALE, INTEGER);
		aggiungiCampo(POS_X, INTEGER);
		aggiungiCampo(POS_Y, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_FOTO);
	}
}
