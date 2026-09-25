package pfa.app.econtab.db.table;

/**
 * Operatori di una riga di rapportino, con le proprie ore (ore-uomo della riga = somma delle loro ore).
 * id_utente_ditta e' l'appartenenza alla ditta dell'operatore (utenti.id_utente_ditta). Il costo orario lo
 * calcola e lo copia il server: l'app lo riceve solo se l'utente e' Amministratore ditta e non lo invia mai.
 */
public class RapportiniDettaglioOperatori extends AbstractTable {
	public static final String NOME_TABELLA = "rapportini_dettaglio_operatori";

	public static final String ID_RAPPORTINO_DETTAGLIO_OPERATORE = "id_rapportino_dettaglio_operatore";
	public static final String ID_RAPPORTINO_DETTAGLIO = "id_rapportino_dettaglio";
	public static final String ID_UTENTE_DITTA = "id_utente_ditta";
	public static final String ORE = "ore";
	public static final String COSTO_ORARIO = "costo_orario";

	public RapportiniDettaglioOperatori() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_RAPPORTINO_DETTAGLIO_OPERATORE);

		aggiungiCampo(ID_RAPPORTINO_DETTAGLIO_OPERATORE, INTEGER);
		aggiungiCampo(ID_RAPPORTINO_DETTAGLIO, INTEGER);
		aggiungiCampo(ID_UTENTE_DITTA, INTEGER);
		aggiungiCampo(ORE, NUMERIC);
		aggiungiCampo(COSTO_ORARIO, NUMERIC);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_RAPPORTINO_DETTAGLIO_OPERATORE);
	}

	/** Alias delle colonne di utenti aggiunte da operatoriRiga(). */
	public static final String NOME_OPERATORE = "nome_operatore";
	public static final String COGNOME_OPERATORE = "cognome_operatore";
	public static final String COSTO_ORARIO_UTENTE = "costo_orario_utente";

	/** Operatori di una riga con nome, cognome e costo orario attuale dell'operatore (quest'ultimo solo per l'amministratore). */
	public static java.util.ArrayList<Object> operatoriRiga(pfa.app.econtab.db.DbInterno db, int idRapportinoDettaglio) {
		return db.eseguiSelect("Select o.*, u." + Utenti.NOME + " as " + NOME_OPERATORE + ", u." + Utenti.COGNOME + " as " + COGNOME_OPERATORE
				+ ", u." + Utenti.COSTO_ORARIO + " as " + COSTO_ORARIO_UTENTE + " from " + NOME_TABELLA + " o left join " + Utenti.NOME_TABELLA
				+ " u on u." + Utenti.ID_UTENTE_DITTA + "=o." + ID_UTENTE_DITTA + " where o." + ID_RAPPORTINO_DETTAGLIO + "=" + idRapportinoDettaglio
				+ " order by u." + Utenti.COGNOME + ", u." + Utenti.NOME, null);
	}

	/**
	 * Costo orario di un operatore di riga (record di operatoriRiga()): quello copiato dal server, altrimenti quello
	 * attuale dell'operatore (per il viaggio il server usa il costo viaggio della ditta, che l'app non conosce: fino alla
	 * sincronizzazione e' una stima).
	 */
	public static double costoOrario(android.content.ContentValues operatore) {
		Double copiato = operatore.getAsDouble(COSTO_ORARIO);
		if (copiato != null && copiato > 0) return copiato;
		Double utente = operatore.getAsDouble(COSTO_ORARIO_UTENTE);
		return utente != null ? utente : 0;
	}

	/** Ore-uomo di una riga come espressione SQL sulla tabella rapportini_dettaglio: somma delle ore dei suoi operatori. */
	public static String sqlOreUomoRiga() {
		return "coalesce((select sum(o." + ORE + ") from " + NOME_TABELLA + " o where o." + ID_RAPPORTINO_DETTAGLIO + "="
				+ RapportiniDettaglio.NOME_TABELLA + "." + RapportiniDettaglio.ID_RAPPORTINO_DETTAGLIO + "), 0)";
	}
}
