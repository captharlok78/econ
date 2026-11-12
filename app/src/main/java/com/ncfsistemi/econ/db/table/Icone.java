package com.ncfsistemi.econ.db.table;

/**
 * Icone di categorie, elementi e componenti del disegno (schema 45, STUDIO_DESIGN.md §5.2): nascono sul server e arrivano
 * con la sync in sola lettura. {@code nome} e' quello scritto in categorie_*.icona, elementi.icona e componenti.icona senza
 * estensione; l'immagine (di solito WebP, 1-2 KB) e' in base64 com'e' arrivata. Le legge utils.IconeDb quando nella
 * cartella dell'app non c'e' il file dell'icona.
 */
public class Icone extends AbstractTable {
	public static final String NOME_TABELLA = "icone";

	public static final String ID = "id";
	public static final String NOME = "nome";
	public static final String TIPO = "tipo";
	public static final String FORMATO = "formato";
	public static final String IMMAGINE = "immagine";
	public static final String IMPRONTA = "impronta";
	public static final String DIMENSIONE = "dimensione";
	public static final String ID_DITTA = "id_ditta";

	public Icone() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(TIPO, TEXT);
		aggiungiCampo(FORMATO, TEXT);
		aggiungiCampo(IMMAGINE, TEXT);
		aggiungiCampo(IMPRONTA, TEXT);
		aggiungiCampo(DIMENSIONE, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}
}
