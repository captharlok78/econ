package pfa.app.econtab.db.table;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.R;

public class CategorieGenerali extends AbstractTable {
	public static final String NOME_TABELLA = "categorie_generali";

	public static final String ID_CATEGORIA_GENERALE = "id_categoria_generale";
	public static final String NOME = "nome";
	public static final String LINEA_SN = "linea_sn";
	public static final String CONFIGURABILE_SN = "configurabile_sn";
	public static final String CAVI_SN = "cavi_sn";
	public static final String TUBO_SN = "tubo_sn";
	public static final String POSIZIONABILE_SN = "posizionabile_sn";

	public static final String ICONA = "icona";

	public static final String PATH_ICONE = "icone_categorie";

	public static final int SCATOLE_PRESE = 1;
	public static final int SCATOLE_INTERRUTTORI = 2;

	public static final int UTILIZZATORI = 3;
	public static final int QUADRI = 4;
	public static final int SCATOLE_COMPONIBILI = 5;

	public static final int CAVI = 7;
	public static final int TUBI = 8;



	public CategorieGenerali() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_CATEGORIA_GENERALE);

		aggiungiCampo(ID_CATEGORIA_GENERALE, INTEGER);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(LINEA_SN, INTEGER);
		aggiungiCampo(CONFIGURABILE_SN, INTEGER);
		aggiungiCampo(CAVI_SN, INTEGER);
		aggiungiCampo(TUBO_SN, INTEGER);
		aggiungiCampo(POSIZIONABILE_SN, INTEGER);
		aggiungiCampo(ICONA, TEXT);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_CATEGORIA_GENERALE);
	}

	public ArrayList<ContentValues> getCategorieDefault() {
		ArrayList<ContentValues> lista = new ArrayList<ContentValues>();
		ContentValues val = super.getValoriInsertDefault();
		val.put(ID_CATEGORIA_GENERALE, 1);
		val.put(NOME, "Prese");
		val.put(LINEA_SN, 1);
		val.put(CONFIGURABILE_SN, 1);
		val.put(POSIZIONABILE_SN, 1);
		val.put(CAVI_SN, 1);
		val.put(TUBO_SN, 1);
		val.put(ICONA, "prese.png");
		lista.add(val);

		ContentValues val1 = new ContentValues(val);
		val1.put(ID_CATEGORIA_GENERALE, 2);
		val1.put(NOME, "Interruttori");
		val1.put(LINEA_SN, 1);
		val1.put(CONFIGURABILE_SN, 1);
		val1.put(CAVI_SN, 1);
		val1.put(TUBO_SN, 1);
		val1.put(POSIZIONABILE_SN, 1);
		val1.put(ICONA, "interruttori.png");
		lista.add(val1);

		ContentValues val2 = new ContentValues(val);
		val2.put(ID_CATEGORIA_GENERALE, 3);
		val2.put(NOME, "Utilizzatori");
		val2.put(LINEA_SN, 1);
		val2.put(CONFIGURABILE_SN, 0);
		val2.put(CAVI_SN, 1);
		val2.put(TUBO_SN, 1);
		val2.put(POSIZIONABILE_SN, 1);
		val2.put(ICONA, "utilizzatori.png");
		lista.add(val2);

		ContentValues val3 = new ContentValues(val);
		val3.put(ID_CATEGORIA_GENERALE, 4);
		val3.put(NOME, "Quadri");
		val3.put(LINEA_SN, 0);
		val3.put(CONFIGURABILE_SN, 1);
		val3.put(CAVI_SN, 1);
		val3.put(TUBO_SN, 1);
		val3.put(POSIZIONABILE_SN, 1);
		val3.put(ICONA, "quadri.png");
		lista.add(val3);

		ContentValues val4 = new ContentValues(val);
		val4.put(ID_CATEGORIA_GENERALE, 5);
		val4.put(NOME, "Scatole");
		val4.put(LINEA_SN, 0);
		val4.put(CONFIGURABILE_SN, 1);
		val4.put(CAVI_SN, 1);
		val4.put(TUBO_SN, 1);
		val4.put(POSIZIONABILE_SN, 1);
		val4.put(ICONA, "scatole.png");
		lista.add(val4);

		ContentValues val5 = new ContentValues(val);
		val5.put(ID_CATEGORIA_GENERALE, 6);
		val5.put(NOME, "Allarme");
		val5.put(LINEA_SN, 1);
		val5.put(CONFIGURABILE_SN, 0);
		val5.put(CAVI_SN, 1);
		val5.put(TUBO_SN, 1);
		val5.put(POSIZIONABILE_SN, 1);
		val5.put(ICONA, "allarme.png");
		lista.add(val5);

		ContentValues val6 = new ContentValues(val);
		val6.put(ID_CATEGORIA_GENERALE, 7);
		val6.put(NOME, "Cavi");
		val6.put(LINEA_SN, 0);
		val6.put(CONFIGURABILE_SN, 0);
		val6.put(CAVI_SN, 0);
		val6.put(TUBO_SN, 0);
		val6.put(POSIZIONABILE_SN, 0);
		val6.put(ICONA, "cavi.png");
		lista.add(val6);

		ContentValues val7 = new ContentValues(val);
		val7.put(ID_CATEGORIA_GENERALE, 8);
		val7.put(NOME, "Tubi");
		val7.put(LINEA_SN, 0);
		val7.put(CONFIGURABILE_SN, 0);
		val7.put(CAVI_SN, 0);
		val7.put(TUBO_SN, 0);
		val7.put(POSIZIONABILE_SN, 0);
		val7.put(ICONA, "tubi.png");
		lista.add(val7);

		return lista;
	}

}
