package com.ncfsistemi.econ.db.table;

public class LocaliPorteFinestre extends AbstractTable {
	public static final String NOME_TABELLA = "locali_porte_finestre";

	public static final String ID_PORTA_FINESTRA = "id_porta_finestra";
	public static final String ID_LOCALE = "id_locale";
	public static final String TIPO = "tipo";
	public static final String POS_INIZIALE_PERC = "pos_iniziale_perc";
	public static final String CELLA = "cella";
	public static final String POS_IN_CELLA = "pos_in_cella";
	/** Elemento di disegno (schema 49, STUDIO_DESIGN_GRAFICA.md §18); 0 = porta o finestra da TIPO. */
	public static final String ID_ELEMENTO_DISEGNO = "id_elemento_disegno";
	/** Centro degli elementi a pavimento (TIPO E), 0..200 come gli elementi del cantiere. */
	public static final String POS_X = "pos_x";
	public static final String POS_Y = "pos_y";
	/** 0, 90, 180, 270: elementi a pavimento girati; porte con 180 = cardine dall'altra parte. */
	public static final String ROTAZIONE = "rotazione";

	public LocaliPorteFinestre() {
		// TODO Auto-generated constructor stub
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_PORTA_FINESTRA);

		aggiungiCampo(ID_PORTA_FINESTRA, INTEGER);
		aggiungiCampo(ID_LOCALE, INTEGER);
		aggiungiCampo(TIPO, TEXT);
		aggiungiCampo(POS_INIZIALE_PERC, INTEGER);
		aggiungiCampo(CELLA, INTEGER);
		aggiungiCampo(POS_IN_CELLA, TEXT);
		aggiungiCampo(ID_ELEMENTO_DISEGNO, INTEGER);
		aggiungiCampo(POS_X, INTEGER);
		aggiungiCampo(POS_Y, INTEGER);
		aggiungiCampo(ROTAZIONE, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_PORTA_FINESTRA);
	}

}
