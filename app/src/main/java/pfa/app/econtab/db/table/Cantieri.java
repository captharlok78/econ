package pfa.app.econtab.db.table;

import android.content.ContentValues;
import android.content.Context;

import java.util.ArrayList;

import pfa.app.econtab.CantieriDettaglioModActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.utils.Utility;

public class Cantieri extends AbstractTable {
	public static final String NOME_TABELLA = "cantieri";

	public static final String ID_CANTIERE = "id_cantiere";
	public static final String ID_ANAGRAFICA = "id_anagrafica";
	public static final String ID_DITTA = "id_ditta";
	public static final String NOME = "nome";
	public static final String INDIRIZZO = "indirizzo";
	public static final String CITTA = "citta";
	public static final String PROVINCIA = "provincia";
	public static final String CAP = "cap";
	public static final String ID_LINEA = "id_linea";
	public static final String ID_PLACCA = "id_placca";
	public static final String NOTE = "note";

	public Cantieri() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_CANTIERE);

		aggiungiCampo(ID_CANTIERE, INTEGER);
		aggiungiCampo(ID_ANAGRAFICA, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(INDIRIZZO, TEXT);
		aggiungiCampo(CITTA, TEXT);
		aggiungiCampo(PROVINCIA, TEXT);
		aggiungiCampo(CAP, TEXT);
		aggiungiCampo(ID_LINEA, INTEGER);
		aggiungiCampo(ID_PLACCA, INTEGER);
		aggiungiCampo(NOTE, TEXT);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_CANTIERE);
	}

	/**
	 * Inserisc eil cantiere, unit� principale , area e locale
	 * 
	 * @param val
	 * @param db
	 * @return
	 * @throws Exception
	 */
	public long inserisciNuovoCantiere(ContentValues val, DbInterno db, Context ctx) {
		long inserito = -1;

		inserito = inserisciRecord(db, val);

		Unita tabunita = new Unita();
		ContentValues valUnita = tabunita.getValoriLogInserimento(db);
		valUnita.put(Unita.ID_CANTIERE, val.getAsInteger(Cantieri.ID_CANTIERE));
		valUnita.put(Unita.ID_LINEA, val.getAsInteger(Cantieri.ID_LINEA));
		valUnita.put(Unita.ID_PLACCA, val.getAsInteger(Cantieri.ID_PLACCA));
		valUnita.put(Unita.NOME, ctx.getString(R.string.unita_principale));
		tabunita.inserisciRecord(db, valUnita);

		Aree tabAree = new Aree();
		ContentValues valAree = tabAree.getValoriLogInserimento(db);
		valAree.put(Aree.ID_UNITA, valUnita.getAsInteger(Unita.ID_UNITA));
		valAree.put(Aree.ID_LINEA, val.getAsInteger(Cantieri.ID_LINEA));
		valAree.put(Aree.ID_PLACCA, val.getAsInteger(Cantieri.ID_PLACCA));
		valAree.put(Aree.NOME, ctx.getString(R.string.area_principale));
		tabAree.inserisciRecord(db, valAree);

		Locali tabLocale = new Locali();
		ContentValues valLocali = tabLocale.getValoriLogInserimento(db);
		valLocali.put(Locali.ID_AREA, valAree.getAsInteger(Aree.ID_AREA));
		valLocali.put(Locali.ID_LINEA, val.getAsInteger(Cantieri.ID_LINEA));
		valLocali.put(Locali.ID_PLACCA, val.getAsInteger(Cantieri.ID_PLACCA));
		valLocali.put(Locali.NOME, ctx.getString(R.string.locale_principale));

		tabLocale.inserisciRecord(db, valLocali);

		return inserito;
	}

	@Override
	protected void eliminaCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		int cantiere = val.getAsInteger(ID_CANTIERE);

		/**
		 * Locali tablocali = new Locali(); ArrayList<Object> locali = tablocali.getLocaliCantiere(db, cantiere); for
		 * (int i=0;i<locali.size();i++){ ContentValues vallocale = (ContentValues)locali.get(i);
		 * tablocali.eliminaCorrelati(db, vallocale); db.delete(Locali.NOME_TABELLA, vallocale); }
		 * 
		 * Aree tabAree = new Aree(); ArrayList<Object> aree = tabAree.getAreeCantiere(db, cantiere); for (int
		 * i=0;i<aree.size();i++){ ContentValues valarea = (ContentValues)aree.get(i); tabAree.eliminaCorrelati(db,
		 * valarea); db.delete(Aree.NOME_TABELLA, valarea); }
		 */

		Unita tabUnita = new Unita();
		ContentValues whereUnita = new ContentValues();
		whereUnita.put(Unita.ID_CANTIERE, cantiere);
		ArrayList<Object> unita = db.eseguiSelect(new Unita(), whereUnita, null);
		for (int i = 0; i < unita.size(); i++) {
			ContentValues valunita = (ContentValues) unita.get(i);
			// ContentValues valunitawhere = tabUnita.getFiltroPerChiave(valunita);
			// tabUnita.eliminaCorrelati(db, valunita);
			// db.delete(Unita.NOME_TABELLA, valunitawhere);

			tabUnita.cancellaRecord(db, valunita);
		}

		super.eliminaCorrelati(db, val);
	}

	@Override
	public Class getDettaglioActivity() {
		// TODO Auto-generated method stub
		return CantieriDettaglioModActivity.class;
	}

	@Override
	public String getCampoDescrizionePerSpinner() {
		// TODO Auto-generated method stub
		return NOME;
	}

	public ContentValues getLineaCantiere(DbInterno db, int idCantiere) {
		// TODO Auto-generated method stub
		Join jLinea = new Join(Cantieri.NOME_TABELLA, Linee.NOME_TABELLA);
		jLinea.addCampiDiJoin(Cantieri.ID_LINEA, Linee.ID_LINEA);

		Join jLinea2 = new Join(Linee.NOME_TABELLA, Costruttori.NOME_TABELLA);
		jLinea2.addCampiDiJoin(Linee.ID_COSTRUTTORE, Costruttori.ID_COSTRUTTORE);

		Linee tabLinee = new Linee();
		Costruttori tabCostruttori = new Costruttori();

		String SQL_LINEA = "Select " + tabLinee.getNomeCampoTabella(Linee.ID_LINEA) + "," + tabLinee.getNomeCampoTabella(Linee.NOME_LINEA)
				+ "," + tabCostruttori.getNomeCampoTabella(Costruttori.RAGIONE_SOCIALE) + ","
				+ tabCostruttori.getNomeCampoTabella(Costruttori.SIGLA_METEL) + ","
				+ tabCostruttori.getNomeCampoTabella(Costruttori.ID_COSTRUTTORE) + " from " + NOME_TABELLA + jLinea.getSQLJoin()
				+ jLinea2.getSQLJoin() + " where " + ID_CANTIERE + "= " + idCantiere;

		ArrayList<Object> recs = db.eseguiSelect(SQL_LINEA, null);
		if (recs.size() > 0) {
			return (ContentValues) recs.get(0);
		}

		// record vuoto di default
		ContentValues def = new ContentValues();
		def.put(Linee.ID_LINEA, 0);
		def.put(Linee.NOME_LINEA, "");
		def.put(Costruttori.RAGIONE_SOCIALE, "");
		def.put(Costruttori.SIGLA_METEL, "");
		def.put(Costruttori.ID_COSTRUTTORE, 0);
		return def;
	}

	@Override
	protected void aggiornamentoCorrelati(DbInterno db, ContentValues val, ContentValues where) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		int idCantiere = where.getAsInteger(Cantieri.ID_CANTIERE);

		// Prendo tutti i preventivi del cantiere
		ContentValues wherePrev = new ContentValues();
		wherePrev.put(Preventivi.ID_CANTIERE, idCantiere);

		PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();
		ArrayList<Object> preventivi = db.eseguiSelect(new Preventivi(), wherePrev, null);
		for (int i = 0; i < preventivi.size(); i++) {
			ContentValues prevCurr = (ContentValues) preventivi.get(i);
			// Cambio solo i preventivi/ordini aperti
			if (prevCurr.getAsString(Preventivi.STATO).equals(Preventivi.STATO_APERTO)) {
				int idPreventivo = prevCurr.getAsInteger(Preventivi.ID_PREVENTIVO);
				tabPrevDett.associazioneAutomaticaCodiciCantiere(db, idPreventivo, idCantiere);
			}
		}

		super.aggiornamentoCorrelati(db, val, where);
	}

	@Override
	public boolean cancellazionePossibile(DbInterno db, ContentValues val, Context ctx) {
		// se esistono dei preventivi/ordini non cancello
		int idCantiere = val.getAsInteger(ID_CANTIERE);
		ContentValues wherePrev = new ContentValues();
		wherePrev.put(Preventivi.ID_CANTIERE, idCantiere);

		int numeroPreventivi = db.eseguiCount(new Preventivi(), wherePrev);
		if (numeroPreventivi > 0) {
			String messaggio = ctx.getString(R.string.cancellazione_non_possibile_cantiere, "" + numeroPreventivi);
			Utility.mostraDialog(ctx.getString(R.string.attenzione), messaggio, ctx, "OK");
			return false;
		}

		return super.cancellazionePossibile(db, val, ctx);
	}

	@Override
	protected int getMassimoNumeroRecordLicenzaGratis() {
		// TODO Auto-generated method stub
		return 1;
	}
}
