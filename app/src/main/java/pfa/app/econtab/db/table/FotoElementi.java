package pfa.app.econtab.db.table;

public class FotoElementi extends AbstractTable {
	public static final String NOME_TABELLA = "foto_elementi";

	public static final String ID_FOTO = "id_foto";
	public static final String ID_ELEMENTO_CANT = "id_elemento_cant";
	public static final String POS_X = "pos_x";
	public static final String POS_Y = "pos_y";

	public FotoElementi() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_FOTO);

		aggiungiCampo(ID_FOTO, INTEGER);
		aggiungiCampo(ID_ELEMENTO_CANT, INTEGER);
		aggiungiCampo(POS_X, INTEGER);
		aggiungiCampo(POS_Y, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_FOTO);
		aggiungiCampoChiave(ID_ELEMENTO_CANT);

	}
}
