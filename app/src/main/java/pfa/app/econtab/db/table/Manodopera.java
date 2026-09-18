package pfa.app.econtab.db.table;

import android.content.ContentValues;

import pfa.app.econtab.ManodoperaDettaglioActivity;
import pfa.app.econtab.utils.Utility;

public class Manodopera extends AbstractTable {
	public static final String NOME_TABELLA = "manodopera";

	public static final String ID_MANODOPERA = "id_manodopera";
	public static final String NOME = "nome";
	public static final String NUM_OPERATORI_DEFAULT = "num_operatori_default";
	public static final String COSTO_ORARIO = "costo_orario";

	public Manodopera() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_MANODOPERA);

		aggiungiCampo(ID_MANODOPERA, INTEGER);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(NUM_OPERATORI_DEFAULT, INTEGER);
		aggiungiCampo(COSTO_ORARIO, NUMERIC);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_MANODOPERA);
	}

	@Override
	public String getCampoDescrizionePerSpinner() {
		// TODO Auto-generated method stub
		return NOME;
	}

	@Override
	public Class getDettaglioActivity() {
		// TODO Auto-generated method stub
		return ManodoperaDettaglioActivity.class;
	}

	@Override
	public String formattaDescrizioneSpinner(ContentValues val) {
		// TODO Auto-generated method stub
		return super.formattaDescrizioneSpinner(val) + " (" + Utility.formatPrezzo(val.getAsFloat(COSTO_ORARIO)) + ")";
	}

	@Override
	public String getSQLPerSpinner(ContentValues filtro) {

		// TODO Auto-generated method stub
		String SQL = "Select " + getCampoCodicePerSpinner() + "," + getCampoDescrizionePerSpinner() + "," + COSTO_ORARIO + " from "
				+ NOME_TABELLA;
		String filtroWhere = getFiltro(filtro);
		if (!filtroWhere.equals("")) {
			filtroWhere = " where " + filtroWhere;
		}
		SQL = SQL + filtroWhere + " order by " + getCampoDescrizionePerSpinner();
		return SQL;
	}

	@Override
	protected int getMassimoNumeroRecordLicenzaGratis() {
		// TODO Auto-generated method stub
		return 5;
	}

}
