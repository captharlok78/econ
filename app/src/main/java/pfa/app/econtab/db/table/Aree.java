package pfa.app.econtab.db.table;

import android.content.ContentValues;
import android.content.Context;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.utils.Utility;

public class Aree extends AbstractTable {
	public static final String NOME_TABELLA = "aree";

	public static final String ID_AREA = "id_area";
	public static final String ID_UNITA = "id_unita";
	public static final String ID_DITTA = "id_ditta";
	public static final String NOME = "nome";
	public static final String ID_LINEA = "id_linea";
	public static final String ID_PLACCA = "id_placca";
	public static final String NOTE = "note";

	public Aree() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_AREA);

		aggiungiCampo(ID_AREA, INTEGER);
		aggiungiCampo(ID_UNITA, INTEGER);
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

		aggiungiCampoChiave(ID_AREA);
	}

	public ArrayList<Object> getAreeCantiere(DbInterno db, int cantiere) {
		Join joinAreeUnita = new Join(Aree.NOME_TABELLA, Unita.NOME_TABELLA);
		joinAreeUnita.addCampiDiJoin(Aree.ID_UNITA, Unita.ID_UNITA);

		ArrayList<Object> aree = db.eseguiSelect(
				"Select " + getNomeCampoTabella("*") + " from " + NOME_TABELLA + joinAreeUnita.getSQLJoin() + " where " + Unita.ID_CANTIERE
						+ "=" + cantiere + " order by " + ID_AREA + " desc ", null);
		return aree;
	}

	@Override
	protected void eliminaCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		int area = val.getAsInteger(ID_AREA);

		Locali tablocali = new Locali();
		ContentValues where = new ContentValues();
		where.put(ID_AREA, area);
		ArrayList<Object> locali = db.eseguiSelect(new Locali(), where, null);
		for (int i = 0; i < locali.size(); i++) {
			ContentValues vallocale = (ContentValues) locali.get(i);
			// ContentValues vallocalewhere = tablocali.getFiltroPerChiave(vallocale);
			// tablocali.eliminaCorrelati(db, vallocale);
			// db.delete(Locali.NOME_TABELLA, vallocalewhere);

			tablocali.cancellaRecord(db, vallocale);
		}
		super.eliminaCorrelati(db, val);
	}

	public ContentValues getLineaArea(DbInterno db, int idArea) {
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
				+ jLinea2.getSQLJoin() + " where " + ID_AREA + "= " + idArea;

		ArrayList<Object> recs = db.eseguiSelect(SQL_LINEA, null);
		if (recs.size() > 0) {
			return (ContentValues) recs.get(0);
		}
		return null;
	}

	public ContentValues getCantiereArea(DbInterno db, int idArea) {

		Join j1 = new Join(NOME_TABELLA, Unita.NOME_TABELLA);
		j1.addCampiDiJoin(ID_UNITA, Unita.ID_UNITA);

		Join j2 = new Join(Unita.NOME_TABELLA, Cantieri.NOME_TABELLA);
		j2.addCampiDiJoin(Unita.ID_CANTIERE, Cantieri.ID_CANTIERE);

		Cantieri tabCant = new Cantieri();

		ArrayList<Object> res = db.eseguiSelect("Select " + tabCant.getNomeCampoTabella("*") + "from " + NOME_TABELLA + j1.getSQLJoin()
				+ j2.getSQLJoin() + " where " + ID_AREA + "=" + idArea, null);
		if (res.size() > 0) {
			return (ContentValues) res.get(0);
		}
		return null;
	}

	@Override
	protected void aggiornamentoCorrelati(DbInterno db, ContentValues val, ContentValues where) {
		// TODO Auto-generated method stub
		ContentValues recAggiornato = db.getRecord(this, where);
		int idArea = recAggiornato.getAsInteger(ID_AREA);
		// prendo il record del cantiere
		ContentValues cantiere = getCantiereArea(db, idArea);
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
					tabPrevDett.associazioneAutomaticaCodiciArea(db, idPreventivo, idArea);
				}
			}
		}

		super.aggiornamentoCorrelati(db, val, where);
	}


    @Override
    public boolean cancellazionePossibile(DbInterno db, ContentValues val, Context ctx) {
        // se esistono dei preventivi/ordini non cancello
        int idArea = val.getAsInteger(ID_AREA);
        Locali tabLocali = new Locali();
        ArrayList<Object> locali =  tabLocali.getLocaliArea(db,idArea);
        int numPrevTot = 0;
        for (int i=0;i<locali.size();i++){
            int idLocale = ((ContentValues)locali.get(i)).getAsInteger(Locali.ID_LOCALE);
            ContentValues wherePrev = new ContentValues();
            wherePrev.put(PreventiviDettaglio.ID_LOCALE, idLocale);
            int numeroPreventivi = db.eseguiCount(new PreventiviDettaglio(), wherePrev);
            numPrevTot = numPrevTot+numeroPreventivi;
        }
        if (numPrevTot > 0) {
            String messaggio = ctx.getString(R.string.cancellazione_non_possibile_area, "" + numPrevTot);
            Utility.mostraDialog(ctx.getString(R.string.attenzione), messaggio, ctx, "OK");
            return false;
        }

        return super.cancellazionePossibile(db, val, ctx);
    }

}
