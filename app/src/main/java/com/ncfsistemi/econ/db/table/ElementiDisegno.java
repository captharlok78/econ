package com.ncfsistemi.econ.db.table;

/**
 * Elementi di disegno della piantina (schema 49, STUDIO_DESIGN_GRAFICA.md §18): porte, finestre, scale, sanitari, arredi...
 * Catalogo di sistema del server (Sonata), scaricato sempre completo in sola lettura. Posizione PARETE (sul muro, larghezza =
 * lunghezza) o PAVIMENTO (dentro la stanza, larghezza x profondita); misure in centesimi del lato della piantina; la forma
 * dice come si disegna (PiantinaView). Si posano in locali_porte_finestre.
 */
public class ElementiDisegno extends AbstractTable {
	public static final String NOME_TABELLA = "elementi_disegno";

	public static final String ID = "id";
	public static final String CODICE = "codice";
	public static final String NOME = "nome";
	public static final String GRUPPO = "gruppo";
	public static final String POSIZIONE = "posizione";
	public static final String FORMA = "forma";
	public static final String LARGHEZZA = "larghezza";
	public static final String PROFONDITA = "profondita";
	public static final String SIGLA = "sigla";
	public static final String COLORE = "colore";
	public static final String ORDINE = "ordine";
	public static final String ATTIVO = "attivo";

	public static final String PARETE = "PARETE";
	public static final String PAVIMENTO = "PAVIMENTO";

	public ElementiDisegno() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(CODICE, TEXT);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(GRUPPO, TEXT);
		aggiungiCampo(POSIZIONE, TEXT);
		aggiungiCampo(FORMA, TEXT);
		aggiungiCampo(LARGHEZZA, INTEGER);
		aggiungiCampo(PROFONDITA, INTEGER);
		aggiungiCampo(SIGLA, TEXT);
		aggiungiCampo(COLORE, TEXT);
		aggiungiCampo(ORDINE, INTEGER);
		aggiungiCampo(ATTIVO, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}
}
