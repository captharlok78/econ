package pfa.app.econtab.db.table;

import android.content.ContentValues;
import android.content.Context;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.utils.Utility;

public class Unita extends AbstractTable {
	public static final String NOME_TABELLA = "unita";

	public static final String ID_UNITA = "id_unita";
	public static final String ID_CANTIERE = "id_cantiere";
	public static final String ID_DITTA = "id_ditta";
	public static final String NOME = "nome";
	public static final String ID_LINEA = "id_linea";
	public static final String ID_PLACCA = "id_placca";
	public static final String NOTE = "note";

	public Unita() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_UNITA);

		aggiungiCampo(ID_UNITA, INTEGER);
		aggiungiCampo(ID_CANTIERE, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(ID_LINEA, INTEGER);
		aggiungiCampo(ID_PLACCA, INTEGER);
		aggiungiCampo(NOTE, TEXT);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_UNITA);
	}

	@Override
	protected void eliminaCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		int unita = val.getAsInteger(ID_UNITA);

		Aree tabAree = new Aree();
		ContentValues where = new ContentValues();
		where.put(Aree.ID_UNITA, unita);
		ArrayList<Object> aree = db.eseguiSelect(new Aree(), where, null);
		for (int i = 0; i < aree.size(); i++) {
			ContentValues vallocale = (ContentValues) aree.get(i);
			// ContentValues vallocalewhere = tabAree.getFiltroPerChiave(vallocale);
			// tabAree.eliminaCorrelati(db, vallocale);
			// db.delete(Aree.NOME_TABELLA, vallocalewhere);

			tabAree.cancellaRecord(db, vallocale);
		}
		super.eliminaCorrelati(db, val);
	}

	public ContentValues getLineaUnita(DbInterno db, int idUnita) {
		Join jLinea = new Join(NOME_TABELLA, Linee.NOME_TABELLA);
		jLinea.addCampiDiJoin(ID_LINEA, Linee.ID_LINEA);

		Join jLinea2 = new Join(Linee.NOME_TABELLA, Costruttori.NOME_TABELLA);
		jLinea2.addCampiDiJoin(Linee.ID_COSTRUTTORE, Costruttori.ID_COSTRUTTORE);

		Linee tabLinee = new Linee();
		Costruttori tabCostruttori = new Costruttori();

		String SQL_LINEA = "Select " + tabLinee.getNomeCampoTabella(Linee.ID_LINEA) + "," + tabLinee.getNomeCampoTabella(Linee.NOME_LINEA)
				+ "," + tabCostruttori.getNomeCampoTabella(Costruttori.RAGIONE_SOCIALE) + ","
				+ tabCostruttori.getNomeCampoTabella(Costruttori.SIGLA_METEL) + ","
				+ tabCostruttori.getNomeCampoTabella(Costruttori.ID_COSTRUTTORE) + " from " + NOME_TABELLA + jLinea.getSQLJoin()
				+ jLinea2.getSQLJoin() + " where " + ID_UNITA + "= " + idUnita;

		ArrayList<Object> recs = db.eseguiSelect(SQL_LINEA, null);
		if (recs.size() > 0) {
			return (ContentValues) recs.get(0);
		}
		return null;
	}

	public ContentValues getCantiereUnita(DbInterno db, int idUnita) {

		Join j2 = new Join(NOME_TABELLA, Cantieri.NOME_TABELLA);
		j2.addCampiDiJoin(ID_CANTIERE, Cantieri.ID_CANTIERE);

		Cantieri tabCant = new Cantieri();

		ArrayList<Object> res = db.eseguiSelect("Select " + tabCant.getNomeCampoTabella("*") + "from " + NOME_TABELLA + j2.getSQLJoin()
				+ " where " + ID_UNITA + "=" + idUnita, null);
		if (res.size() > 0) {
			return (ContentValues) res.get(0);
		}
		return null;
	}

	@Override
	protected void aggiornamentoCorrelati(DbInterno db, ContentValues val, ContentValues where) {
		// TODO Auto-generated method stub
		ContentValues recAggiornato = db.getRecord(this, where);
		int idUnita = recAggiornato.getAsInteger(ID_UNITA);
		// prendo il record del cantiere
		ContentValues cantiere = getCantiereUnita(db, idUnita);
		if (cantiere != null) {
			// Prendo tutti i preventivi del cantiere
			ContentValues wherePrev = new ContentValues();
			wherePrev.put(Preventivi.ID_CANTIERE, cantiere.getAsInteger(Cantieri.ID_CANTIERE));

			PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();
			ArrayList<Object> preventivi = db.eseguiSelect(new Preventivi(), wherePrev, null);
			for (int i = 0; i < preventivi.size(); i++) {
				ContentValues prevCurr = (ContentValues) preventivi.get(i);
				// Cambio solo i preventivi/ordini aperti
				if (prevCurr.getAsString(Preventivi.STATO).equals(Preventivi.STATO_APERTO)) {
					int idPreventivo = prevCurr.getAsInteger(Preventivi.ID_PREVENTIVO);
					tabPrevDett.associazioneAutomaticaCodiciUnita(db, idPreventivo, idUnita);
				}
			}
		}

		super.aggiornamentoCorrelati(db, val, where);
	}

	@Override
	protected int getMassimoNumeroRecordLicenzaGratis() {
		// TODO Auto-generated method stub
		return 4;
	}

    @Override
    public boolean cancellazionePossibile(DbInterno db, ContentValues val, Context ctx) {
        // se esistono dei preventivi/ordini non cancello
        int idUnita = val.getAsInteger(ID_UNITA);
        Locali tabLocali = new Locali();
        ArrayList<Object> locali =  tabLocali.getLocaliUnita(db,idUnita);
        int numPrevTot = 0;
        for (int i=0;i<locali.size();i++){
            int idLocale = ((ContentValues)locali.get(i)).getAsInteger(Locali.ID_LOCALE);
            ContentValues wherePrev = new ContentValues();
            wherePrev.put(PreventiviDettaglio.ID_LOCALE, idLocale);
            int numeroPreventivi = db.eseguiCount(new PreventiviDettaglio(), wherePrev);
            numPrevTot = numPrevTot+numeroPreventivi;
        }
        if (numPrevTot > 0) {
            String messaggio = ctx.getString(R.string.cancellazione_non_possibile_unita, "" + numPrevTot);
            Utility.mostraDialog(ctx.getString(R.string.attenzione), messaggio, ctx, "OK");
            return false;
        }

        return super.cancellazionePossibile(db, val, ctx);
    }
}
