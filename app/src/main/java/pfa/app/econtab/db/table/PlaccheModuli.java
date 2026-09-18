package pfa.app.econtab.db.table;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;

public class PlaccheModuli extends AbstractTable {
	public static final String NOME_TABELLA = "placche_moduli";

	public static final String ID_PLACCA_MODULI = "id_placca_moduli";
	public static final String ID_PLACCA = "id_placca";
	public static final String NUMERO_MODULI = "numero_moduli";

	public PlaccheModuli() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_PLACCA_MODULI);

		aggiungiCampo(ID_PLACCA_MODULI, INTEGER);
		aggiungiCampo(ID_PLACCA, INTEGER);
		aggiungiCampo(NUMERO_MODULI, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_PLACCA_MODULI);
	}

	/**
	 * Se non esiste gi� il record con il numero di moduli per la placca, lo inserisco
	 * 
	 * @param db
	 * @param idPlacca
	 * @param numeroModuli
	 */
	public int controllaInserisciRecord(DbInterno db, int idPlacca, int numeroModuli) {
		// TODO Auto-generated method stub
		ContentValues where = new ContentValues();
		where.put(ID_PLACCA, idPlacca);
		where.put(NUMERO_MODULI, numeroModuli);

		ContentValues valChk = db.getRecord(this, where);
		if (valChk == null) {
			ContentValues valInsert = getValoriLogInserimento(db);
			valInsert.put(ID_PLACCA, idPlacca);
			valInsert.put(NUMERO_MODULI, numeroModuli);

			inserisciRecord(db, valInsert);

			return valInsert.getAsInteger(ID_PLACCA_MODULI);
		} else {
			return valChk.getAsInteger(ID_PLACCA_MODULI);
		}
	}

	/**
	 * ritorna il record in join con la tabella Placche
	 * 
	 * @param db
	 * @param idPlaccaModuli
	 * @return
	 */
	public ContentValues getRecordPlacca(DbInterno db, int idPlaccaModuli) {
		Join j0 = new Join(NOME_TABELLA, Placche.NOME_TABELLA);
		j0.addCampiDiJoin(ID_PLACCA, Placche.ID_PLACCA);
		String SQL = "Select * from " + NOME_TABELLA + j0.getSQLJoin() + " where " + ID_PLACCA_MODULI + "=" + idPlaccaModuli;
		ArrayList<Object> recs = db.eseguiSelect(SQL, null);
		if (recs.size() > 0) {
			return (ContentValues) recs.get(0);
		}
		return null;

	}

	/**
	 * ritorna il record in join con la tabella Placche
	 * 
	 * @param db
	 * @param idPlaccaModuli
	 * @return
	 */
	public ContentValues getRecordPlaccaConLineaFornitore(DbInterno db, int idPlaccaModuli) {
		Join j0 = new Join(NOME_TABELLA, Placche.NOME_TABELLA);
		j0.addCampiDiJoin(ID_PLACCA, Placche.ID_PLACCA);

		Join j1 = new Join(Placche.NOME_TABELLA, Linee.NOME_TABELLA);
		j1.addCampiDiJoin(Placche.ID_LINEA, Linee.ID_LINEA);

		Join j2 = new Join(Linee.NOME_TABELLA, Costruttori.NOME_TABELLA);
		j2.addCampiDiJoin(Linee.ID_COSTRUTTORE, Costruttori.ID_COSTRUTTORE);

		Linee tablinea = new Linee();
		Costruttori tabCostruttori = new Costruttori();

		String SQL = "Select " + NOME_TABELLA + ".*," + tablinea.getNomeCampoTabella(Linee.ID_LINEA) + ","
				+ tablinea.getNomeCampoTabella(Linee.NOME_LINEA) + "," + tabCostruttori.getNomeCampoTabella(Costruttori.SIGLA_METEL)
				+ " from " + NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + j2.getSQLJoin() + " where " + ID_PLACCA_MODULI + "="
				+ idPlaccaModuli;
		ArrayList<Object> recs = db.eseguiSelect(SQL, null);
		if (recs.size() > 0) {
			return (ContentValues) recs.get(0);
		}
		return null;

	}

}
