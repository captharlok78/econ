package pfa.app.econtab.db.table;

import android.content.ContentValues;


public class UnitaMisura extends AbstractTable {
	public static final String NOME_TABELLA = "unita_misura";

	public static final String UNITA_MISURA = "unita_misura";
	public static final String NOME = "nome";
	/** Id sul server: le righe dei rapportini (schema 26) vi fanno riferimento con id_unita_misura. */
	public static final String ID = "id";
	/** Unita' di tempo: minuti per unita' (Ore = 60, Minuti = 1); vuoto per le altre. */
	public static final String MINUTI_PER_UNITA = "minuti_per_unita";
	public static final String CODICE_ORE = "OR";
	public static final String CODICE_MINUTI = "MI";
	public static final String CODICE_PEZZO = "PZ";

	public UnitaMisura() {
		setNomeTabella(NOME_TABELLA);

		aggiungiCampo(UNITA_MISURA, TEXT);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(ID, INTEGER);
		aggiungiCampo(MINUTI_PER_UNITA, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(UNITA_MISURA);
	}

	/*
	 * public ArrayList<ContentValues> getUdmDefault() { ArrayList<ContentValues> lista = new
	 * ArrayList<ContentValues>(); ContentValues val = super.getValoriInsertDefault(); val.put(UNITA_MISURA, "pz");
	 * val.put(NOME, "Pezzi");
	 * 
	 * lista.add(val);
	 * 
	 * ContentValues val1 = new ContentValues(val); val1.put(UNITA_MISURA, "mt"); val1.put(NOME, "Metri");
	 * lista.add(val1);
	 * 
	 * ContentValues val2 = new ContentValues(val); val2.put(UNITA_MISURA, "h"); val2.put(NOME, "Ore"); lista.add(val2);
	 * 
	 * return lista; }
	 */

	/** Le unita' di misura arrivano dal server (Sonata, Configurazione): l'app le scarica ma non le crea. */
	@Override
	public boolean isCreabileDaApp() {
		return false;
	}

	@Override
	public String getCampoDescrizionePerSpinner() {
		// TODO Auto-generated method stub
		return NOME;
	}

	@Override
	public String getSQLPerSpinner(ContentValues filtro) {

		// TODO Auto-generated method stub
		String SQL = "Select " + getCampoCodicePerSpinner() + "," + getCampoCodicePerSpinner() + " ||' - '|| " + NOME + " as " + NOME
				+ " from " + NOME_TABELLA;
		String filtroWhere = getFiltro(filtro);
		if (!filtroWhere.equals("")) {
			filtroWhere = " where " + filtroWhere;
		}
		SQL = SQL + filtroWhere;
		return SQL;
	}

	@Override
	protected int getMassimoNumeroRecordLicenzaGratis() {
		// TODO Auto-generated method stub
		return 5;
	}
}
