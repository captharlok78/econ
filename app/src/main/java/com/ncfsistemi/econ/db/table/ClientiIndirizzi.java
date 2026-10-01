package com.ncfsistemi.econ.db.table;

/**
 * Indirizzi dei clienti (GESTIONE_CLIENTI.md §6, schema 28): n per cliente, via e civico separati, uno principale (la sede,
 * che il server copia sul cliente). Da un indirizzo si crea un cantiere collegato (cantieri.id_cliente_indirizzo).
 */
public class ClientiIndirizzi extends AbstractTable {
	public static final String NOME_TABELLA = "clienti_indirizzi";

	public static final String ID = "id";
	public static final String ID_DITTA = "id_ditta";
	public static final String ID_ANAGRAFICA = "id_anagrafica";
	public static final String TIPO = "tipo";
	public static final String DESCRIZIONE = "descrizione";
	public static final String INDIRIZZO = "indirizzo";
	public static final String CIVICO = "civico";
	public static final String CAP = "cap";
	public static final String CITTA = "citta";
	public static final String PROVINCIA = "provincia";
	public static final String NAZIONE = "nazione";
	public static final String PRINCIPALE = "principale";
	public static final String NOTE = "note";

	/** Tipi come sul server (ClienteIndirizzo::TIPI): codice e descrizione. */
	public static final String[][] TIPI = {
			{"SEDE", "Sede"}, {"OPERATIVA", "Sede operativa"}, {"CANTIERE", "Luogo di lavoro"}, {"ALTRO", "Altro"},
	};

	public ClientiIndirizzi() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID);

		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(ID_ANAGRAFICA, INTEGER);
		aggiungiCampo(TIPO, TEXT);
		aggiungiCampo(DESCRIZIONE, TEXT);
		aggiungiCampo(INDIRIZZO, TEXT);
		aggiungiCampo(CIVICO, TEXT);
		aggiungiCampo(CAP, TEXT);
		aggiungiCampo(CITTA, TEXT);
		aggiungiCampo(PROVINCIA, TEXT);
		aggiungiCampo(NAZIONE, TEXT);
		aggiungiCampo(PRINCIPALE, INTEGER);
		aggiungiCampo(NOTE, TEXT);
		aggiungiCampo(ATTIVO, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID);
	}

	/** Descrizione del tipo ("Sede", ...). */
	public static String tipoLabel(String codice) {
		for (String[] t : TIPI) {
			if (t[0].equals(codice)) return t[1];
		}
		return codice == null ? "" : codice;
	}

	/** "Via Roma 12, 35010 Cadoneghe (PD)" (+ nazione se non Italia) da una riga della tabella. */
	public static String completo(android.content.ContentValues v) {
		String via = (testo(v, INDIRIZZO) + " " + testo(v, CIVICO)).trim();
		String prov = testo(v, PROVINCIA);
		String luogo = (testo(v, CAP) + " " + testo(v, CITTA) + (prov.isEmpty() ? "" : " (" + prov + ")")).trim();
		String naz = testo(v, NAZIONE);
		StringBuilder sb = new StringBuilder(via);
		if (!luogo.isEmpty()) sb.append(sb.length() > 0 ? ", " : "").append(luogo);
		if (!naz.isEmpty() && !"IT".equals(naz)) sb.append(sb.length() > 0 ? ", " : "").append(naz);
		return sb.toString();
	}

	private static String testo(android.content.ContentValues v, String campo) {
		String s = v.getAsString(campo);
		return s == null ? "" : s.trim();
	}
}
