package pfa.app.econtab.db.table;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.LineeDettaglioModActivity;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;

public class Linee extends AbstractTable {
	public static final String NOME_TABELLA = "linee";

	public static final String ID_LINEA = "id_linea";
	public static final String ID_COSTRUTTORE = "id_costruttore";
	public static final String NOME_LINEA = "nome_linea";

	public Linee() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_LINEA);

		aggiungiCampo(ID_LINEA, INTEGER);
		aggiungiCampo(ID_COSTRUTTORE, INTEGER);
		aggiungiCampo(NOME_LINEA, TEXT);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_LINEA);
	}

	@Override
	public Class getDettaglioActivity() {
		// TODO Auto-generated method stub
		return LineeDettaglioModActivity.class;
	}

	@Override
	public String getSQLPerSpinner(ContentValues filtro) {

		// TODO Auto-generated method stub
		String SQL = "Select " + getCampoCodicePerSpinner() + "," + Costruttori.RAGIONE_SOCIALE + " ||' - '|| " + NOME_LINEA + " as "
				+ NOME_LINEA + " from " + NOME_TABELLA + " inner join " + Costruttori.NOME_TABELLA + " on " + NOME_TABELLA + "."
				+ ID_COSTRUTTORE + "=" + Costruttori.NOME_TABELLA + "." + Costruttori.ID_COSTRUTTORE;
		String filtroWhere = getFiltro(filtro);
		if (!filtroWhere.equals("")) {
			filtroWhere = " where " + filtroWhere;
		}
		SQL = SQL + filtroWhere + "  order by " + Costruttori.RAGIONE_SOCIALE + "," + NOME_LINEA;

		return SQL;
	}

	@Override
	public String getCampoCodicePerSpinner() {
		// TODO Auto-generated method stub
		return ID_LINEA;
	}

	@Override
	public String getCampoDescrizionePerSpinner() {
		// TODO Auto-generated method stub
		return NOME_LINEA;
	}

	public ContentValues getRecordConFornitore(int idLinea, DbInterno db) {
		// TODO Auto-generated method stub
		Join j0 = new Join(NOME_TABELLA, Costruttori.NOME_TABELLA);
		j0.addCampiDiJoin(ID_COSTRUTTORE, Costruttori.ID_COSTRUTTORE);
		ArrayList<Object> res = db.eseguiSelect("Select " + NOME_TABELLA + ".*," + Costruttori.RAGIONE_SOCIALE + ","
				+ Costruttori.SIGLA_METEL + " from " + NOME_TABELLA + j0.getSQLJoin() + " where " + ID_LINEA + "=" + idLinea, null);
		if (res.size() > 0) {
			return (ContentValues) res.get(0);
		}
		return null;
	}

	public ContentValues getLinea(DbInterno db, int idLinea) {
		// TODO Auto-generated method stub



		Join jLinea2 = new Join(Linee.NOME_TABELLA, Costruttori.NOME_TABELLA);
		jLinea2.addCampiDiJoin(Linee.ID_COSTRUTTORE, Costruttori.ID_COSTRUTTORE);

		Linee tabLinee = new Linee();
		Costruttori tabCostruttori = new Costruttori();

		String SQL_LINEA = "Select " + tabLinee.getNomeCampoTabella(Linee.ID_LINEA) + "," + tabLinee.getNomeCampoTabella(Linee.NOME_LINEA)
				+ "," + tabCostruttori.getNomeCampoTabella(Costruttori.RAGIONE_SOCIALE) + ","
				+ tabCostruttori.getNomeCampoTabella(Costruttori.SIGLA_METEL) + ","
				+ tabCostruttori.getNomeCampoTabella(Costruttori.ID_COSTRUTTORE) + " from " + NOME_TABELLA
				+ jLinea2.getSQLJoin() + " where " + ID_LINEA + "= " + idLinea;

		ArrayList<Object> recs = db.eseguiSelect(SQL_LINEA, null);
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
