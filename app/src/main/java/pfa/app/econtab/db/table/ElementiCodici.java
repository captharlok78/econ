package pfa.app.econtab.db.table;

public class ElementiCodici extends AbstractTable {

	public static final String NOME_TABELLA = "elementi_codici";

	public static final String ID_ELEMENTO = "id_elemento";
	public static final String ID_COMPONENTE = "id_componente";
	public static final String ID_PLACCA_MODULI = "id_placca_moduli";
	public static final String CODICE_ARTICOLO = "codice_articolo";

	public ElementiCodici() {
		// TODO Auto-generated constructor stub
		setNomeTabella(NOME_TABELLA);

		aggiungiCampo(ID_ELEMENTO, INTEGER);
		aggiungiCampo(ID_COMPONENTE, INTEGER);
		aggiungiCampo(ID_PLACCA_MODULI, INTEGER);
		aggiungiCampo(CODICE_ARTICOLO, TEXT);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_ELEMENTO);
		aggiungiCampoChiave(ID_COMPONENTE);
		aggiungiCampoChiave(ID_PLACCA_MODULI);
		aggiungiCampoChiave(CODICE_ARTICOLO);
	}

}
