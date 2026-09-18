package pfa.app.econtab.db.table;

import android.content.res.Resources;

import pfa.app.econtab.R;

public class CategorieComponenti extends AbstractTable {
	public static final String NOME_TABELLA = "categorie_componenti";

	public static final String ID_CATEGORIA_COMPONENTE = "id_categoria_componente";
	public static final String NOME = "nome";
	public static final String ICONA = "icona";
	public static final String TIPO = "tipo";

	public static final String PATH_ICONE = "icone_categorie_comp";

	public static final String TIPO_FRUTTO = "F";
	public static final String TIPO_SCATOLA = "S";
	public static final String TIPO_PORTAFRUTTI = "P";
	public static final String TIPO_COPRISCATOLA = "C";
	public static final String TIPO_CENTRALINO = "Q";
	public static final String TIPO_COMPONENTE_QUADRO = "I";

	public CategorieComponenti() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_CATEGORIA_COMPONENTE);

		aggiungiCampo(ID_CATEGORIA_COMPONENTE, INTEGER);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(ICONA, TEXT);
		aggiungiCampo(TIPO, TEXT);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_CATEGORIA_COMPONENTE);
	}

	public static String getDescrizioneTipo(Resources res, String tipo) {
		//System.out.println("EConTab: CategorieComponenti getDescrizioneTipo tipo " + tipo);
		if (tipo.equals(TIPO_SCATOLA)) {
			return res.getString(R.string.scatola);
		}
		if (tipo.equals(TIPO_FRUTTO)) {
			return res.getString(R.string.frutto);
		}
		if (tipo.equals(TIPO_PORTAFRUTTI)) {
			return res.getString(R.string.portafrutti);
		}
		if (tipo.equals(TIPO_CENTRALINO)) {
			return res.getString(R.string.centralino);
		}
		if (tipo.equals(TIPO_COMPONENTE_QUADRO)) {
			return res.getString(R.string.componente);
		}
		return tipo;
	}

}
