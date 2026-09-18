package pfa.app.econtab.db.table;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.utils.Sessione;

public class Rapportini extends AbstractTable {
	public static final String NOME_TABELLA = "rapportini";


	public static final String ID_RAPPORTINO = "id_rapportino";
    public static final String ID_DITTA = "id_ditta";
    public static final String ID_ORDINE = "id_ordine";
    public static final String ID_OPERATORE = "id_operatore";
    public static final String DATA_RAPPORTINO = "data_rapportino";
    public static final String NOTE = "note";


    public static final String PATH_EXPORT_RAPPORTINI = "rapportini";



	public Rapportini() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_RAPPORTINO);

		aggiungiCampo(ID_RAPPORTINO, INTEGER);
        aggiungiCampo(ID_DITTA, INTEGER);
        aggiungiCampo(ID_ORDINE, INTEGER);
        aggiungiCampo(ID_OPERATORE, INTEGER);
        aggiungiCampo(DATA_RAPPORTINO, DATE);
        aggiungiCampo(NOTE, TEXT);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_RAPPORTINO);
	}


    public ContentValues getUltimoRapportinoOperatore(DbInterno dbcl, int idOperatore) {
        String SQL = "Select * from " + NOME_TABELLA+" where "+ID_OPERATORE+"="+idOperatore+" and "+ID_DITTA+"="+ Sessione.getDittaSelezionata() +" order by "+DATA_RAPPORTINO+" desc";
        ArrayList<Object> recs = dbcl.eseguiSelect(SQL,null);
        if (recs.size()>0){
            return (ContentValues)recs.get(0);
        }
        return null;

    }


    @Override
    protected void eliminaCorrelati(DbInterno db, ContentValues val) {
        int rapportino = val.getAsInteger(ID_RAPPORTINO);
        RapportiniDettaglio tabRappDett = new RapportiniDettaglio();
        ContentValues where = new ContentValues();
        where.put(RapportiniDettaglio.ID_RAPPORTINO, rapportino);
        ArrayList<Object> dett = db.eseguiSelect(tabRappDett, where, null);
        for (int i = 0; i < dett.size(); i++) {
            ContentValues valDett = (ContentValues) dett.get(i);
            tabRappDett.cancellaRecord(db, valDett);
        }
        super.eliminaCorrelati(db, val);

    }


    @Override
    protected int getMassimoNumeroRecordLicenzaGratis() {
        return 5;
    }
}
