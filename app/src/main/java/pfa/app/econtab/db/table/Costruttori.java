package pfa.app.econtab.db.table;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.CostruttoriDettaglioActivity;
import pfa.app.econtab.db.DbInterno;

public class Costruttori extends AbstractTable {
	public static final String NOME_TABELLA = "costruttori";

	public static final String ID_COSTRUTTORE = "id_costruttore";
	public static final String SIGLA_METEL = "sigla_metel";
	public static final String RAGIONE_SOCIALE = "ragione_sociale";

	public Costruttori() {
		System.out.println("EConTab: Costruttori CONSTRUCTOR ENTER");
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_COSTRUTTORE);

		aggiungiCampo(ID_COSTRUTTORE, INTEGER);
		aggiungiCampo(SIGLA_METEL, TEXT);
		aggiungiCampo(RAGIONE_SOCIALE, TEXT);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_COSTRUTTORE);
		System.out.println("EConTab: Costruttori CONSTRUCTOR EXIT");
	}

	/*
	 * public ArrayList<ContentValues> getCostruttoriDefault(){ ArrayList<ContentValues> lista = new
	 * ArrayList<ContentValues>(); ContentValues val = super.getValoriInsertDefault(); val.put(ID_COSTRUTTORE, 1);
	 * val.put(SIGLA_METEL, "BTI"); val.put(RAGIONE_SOCIALE, "Bticino Spa"); lista.add(val);
	 * 
	 * ContentValues val1 = new ContentValues(val); val1.put(ID_COSTRUTTORE, 2); val1.put(SIGLA_METEL, "VIW");
	 * val1.put(RAGIONE_SOCIALE, "Vimar Spa"); lista.add(val1);
	 * 
	 * ContentValues val2 = new ContentValues(val); val2.put(ID_COSTRUTTORE, 3); val2.put(SIGLA_METEL, "DIS");
	 * val2.put(RAGIONE_SOCIALE, "Disano Illuminazione Spa"); lista.add(val2);
	 * 
	 * ContentValues val3 = new ContentValues(val); val3.put(ID_COSTRUTTORE, 4); val3.put(SIGLA_METEL, "BEG");
	 * val3.put(RAGIONE_SOCIALE, "Beghelli Spa"); lista.add(val3);
	 * 
	 * ContentValues val4 = new ContentValues(val); val4.put(ID_COSTRUTTORE, 5); val4.put(SIGLA_METEL, "BEC");
	 * val4.put(RAGIONE_SOCIALE, "Berica Cavi Spa"); lista.add(val4);
	 * 
	 * return lista; }
	 */

	@Override
	public Class getDettaglioActivity() {
		System.out.println("EConTab: Costruttori getDettaglioActivity");
		// TODO Auto-generated method stub
		return CostruttoriDettaglioActivity.class;
	}

	@Override
	public String getCampoCodicePerSpinner() {
		System.out.println("EConTab: Costruttori getCampoCodicePerSpinner");
		// TODO Auto-generated method stub
		return ID_COSTRUTTORE;
	}

	@Override
	public String getCampoDescrizionePerSpinner() {
		System.out.println("EConTab: Costruttori getCampoDescrizionePerSpinner");
		// TODO Auto-generated method stub
		return RAGIONE_SOCIALE;
	}

	public ContentValues getCostruttoreDaSiglaMetel(DbInterno db, String siglaMetel) {
		System.out.println("EConTab: Costruttori getCostruttoreDaSiglaMetel");
		ContentValues where = new ContentValues();
		where.put(SIGLA_METEL, siglaMetel);
		ArrayList<Object> recs = db.eseguiSelect(this, where, null);
		if (recs.size() > 0) {
			return (ContentValues) recs.get(0);
		}
		return null;
	}

	@Override
	protected int getMassimoNumeroRecordLicenzaGratis() {
		// TODO Auto-generated method stub
		return 5;
	}
}
