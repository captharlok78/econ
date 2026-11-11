package com.ncfsistemi.econ.db.table;

import android.content.res.Resources;

import com.ncfsistemi.econ.R;

public class CategorieComponenti extends AbstractTable {
	public static final String NOME_TABELLA = "categorie_componenti";

	public static final String ID_CATEGORIA_COMPONENTE = "id_categoria_componente";
	public static final String NOME = "nome";
	public static final String ICONA = "icona";
	public static final String TIPO = "tipo";
	/** Schema 44 (STUDIO_LISTINO_E_ARTICOLI.md §6.5, D8): famiglia degli articoli da proporre per i componenti della categoria. */
	public static final String ID_FAMIGLIA = "id_famiglia";

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
		aggiungiCampo(ID_FAMIGLIA, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_CATEGORIA_COMPONENTE);
	}

	public static String getDescrizioneTipo(Resources res, String tipo) {
		//System.out.println("Econ: CategorieComponenti getDescrizioneTipo tipo " + tipo);
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
