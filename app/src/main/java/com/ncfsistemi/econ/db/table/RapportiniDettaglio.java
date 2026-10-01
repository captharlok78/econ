package com.ncfsistemi.econ.db.table;

/**
 * Riga di un rapportino, schema 26 (nomi del server, GESTIONE_RAPPORTINI.md §1.2): tipo (la categoria del tipo decide il
 * comportamento: LAVORO, VIAGGIO, MATERIALE), utente della manodopera, quantita' con la sua unita' di misura (per lavoro e
 * viaggio un'unita' di tempo), orari facoltativi, articolo del listino (materiale), prezzo e "a costo" (solo per chi li vede),
 * note, riga dell'ordine. Nessuna descrizione salvata: la si ricava dal contenuto.
 */
public class RapportiniDettaglio extends AbstractTable {
	public static final String NOME_TABELLA = "rapportini_dettaglio";

	public static final String ID = "id";
    public static final String ID_DITTA = "id_ditta";
    public static final String ID_RAPPORTINO = "id_rapportino";
    public static final String ID_TIPO = "id_tipo";
    public static final String ID_UTENTE_MANODOPERA = "id_utente_manodopera";
    public static final String QUANTITA = "quantita";
    public static final String ID_UNITA_MISURA = "id_unita_misura";
    public static final String ORA_INIZIO = "ora_inizio";
    public static final String ORA_FINE = "ora_fine";
    public static final String ID_LISTINO = "id_listino";
    public static final String PREZZO = "prezzo";
    public static final String A_COSTO = "a_costo";
    public static final String NOTE = "note";
    public static final String ID_PREVENTIVO_DETTAGLIO = "id_preventivo_dettaglio";

	public RapportiniDettaglio() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID);

		aggiungiCampo(ID, INTEGER);
        aggiungiCampo(ID_DITTA, INTEGER);
        aggiungiCampo(ID_RAPPORTINO, INTEGER);
        aggiungiCampo(ID_TIPO, INTEGER);
        aggiungiCampo(ID_UTENTE_MANODOPERA, INTEGER);
        aggiungiCampo(QUANTITA, NUMERIC);
        aggiungiCampo(ID_UNITA_MISURA, INTEGER);
        aggiungiCampo(ORA_INIZIO, TEXT);
        aggiungiCampo(ORA_FINE, TEXT);
        aggiungiCampo(ID_LISTINO, INTEGER);
        aggiungiCampo(PREZZO, NUMERIC);
        aggiungiCampo(A_COSTO, INTEGER);
        aggiungiCampo(NOTE, TEXT);
        aggiungiCampo(ID_PREVENTIVO_DETTAGLIO, INTEGER);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}

    /** Ore-uomo di una riga come espressione SQL (alias u = unita_misura): quantita' × minuti / 60, 0 per il materiale. */
    public static String sqlOreUomo(String aliasRiga, String aliasTipo, String aliasUnita) {
        return "(case when " + aliasTipo + "." + RapportiniDettaglioTipi.CATEGORIA + " = '" + RapportiniDettaglioTipi.CATEGORIA_MATERIALE
                + "' then 0 else coalesce(" + aliasRiga + "." + QUANTITA + ",0) * coalesce(" + aliasUnita + "." + UnitaMisura.MINUTI_PER_UNITA
                + ",0) / 60.0 end)";
    }

}
