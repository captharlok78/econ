package pfa.app.econtab.db.table;

import android.content.ContentValues;
import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.fragments.PreventivoDettaglioFragment;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;

public class Preventivi extends AbstractTable {
	public static final String NOME_TABELLA = "preventivi";

	public static final String ID_PREVENTIVO = "id_preventivo";
	public static final String TIPO = "tipo";
	public static final String ID_PREVENTIVO_ORIGINE = "id_preventivo_origine";
	public static final String NUMERO = "numero";
	public static final String ANNO = "anno";

	public static final String ID_CANTIERE = "id_cantiere";

	public static final String STATO = "stato";
	public static final String DATA = "data";
	public static final String TITOLO = "titolo";
	public static final String NOTE = "note";

	public static final String TIPO_ORDINE = "O";
	public static final String TIPO_PREVENTIVO = "P";

	public static final String STATO_APERTO = "A";
	public static final String STATO_ACCETTATO = "O";
	public static final String STATO_RIFIUTATO = "R";

	public static final String STATO_CHIUSO = "C";
	public static final String STATO_FATTURATO = "F";
	public static final String STATO_PARZIALMENTE_FATTURATO = "P";

	public Preventivi() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_PREVENTIVO);

		aggiungiCampo(ID_PREVENTIVO, INTEGER);
		aggiungiCampo(TIPO, TEXT);
		aggiungiCampo(ID_PREVENTIVO_ORIGINE, INTEGER);
		aggiungiCampo(NUMERO, INTEGER);
		aggiungiCampo(ANNO, INTEGER);

		aggiungiCampo(ID_CANTIERE, INTEGER);
		aggiungiCampo(STATO, TEXT);
		aggiungiCampo(DATA, DATE);
		aggiungiCampo(TITOLO, TEXT);
		aggiungiCampo(NOTE, TEXT);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_PREVENTIVO);
	}

	public int getProssimoNumeroPreventivo(DbInterno db, int anno) {
		int prossimo = 1;
        Join j1 = new Join(NOME_TABELLA,Cantieri.NOME_TABELLA);
        j1.addCampiDiJoin(ID_CANTIERE,Cantieri.ID_CANTIERE);
		String SQL = "Select max(" + NUMERO + ") as numero from " + NOME_TABELLA + j1.getSQLJoin()+ " where " + ANNO + " = " + anno + " and " + TIPO + "='"
				+ TIPO_PREVENTIVO + "' and "+ Cantieri.ID_DITTA+"="+Sessione.getDittaSelezionata() +" order by " + NUMERO + " desc ";
		ArrayList<Object> records = db.eseguiSelect(SQL, null);
		if (records.size() > 0) {
			ContentValues val = (ContentValues) records.get(0);
			if (val.get("numero") != null) {
				int num = val.getAsInteger("numero");
				prossimo = num + 1;
			}

		}
		return prossimo;
	}

	public int getProssimoNumeroOrdine(DbInterno db, int anno) {
		int prossimo = 1;
        Join j1 = new Join(NOME_TABELLA,Cantieri.NOME_TABELLA);
        j1.addCampiDiJoin(ID_CANTIERE,Cantieri.ID_CANTIERE);
		String SQL = "Select max(" + NUMERO + ") as numero from " + NOME_TABELLA + j1.getSQLJoin()+ " where " + ANNO + " = " + anno + " and " + TIPO + "='"
				+ TIPO_ORDINE + "' and "+ Cantieri.ID_DITTA+"="+Sessione.getDittaSelezionata() +" order by " + NUMERO + " desc ";
		ArrayList<Object> records = db.eseguiSelect(SQL, null);
		if (records.size() > 0) {
			ContentValues val = (ContentValues) records.get(0);
			if (val.get("numero") != null) {
				int num = val.getAsInteger("numero");
				prossimo = num + 1;
			}
		}
		return prossimo;
	}

	public ArrayList<Object> getStatiPreventivo(Context cont) {
		// TODO Auto-generated method stub
		ArrayList<Object> stati = new ArrayList<Object>();
		ContentValues val = new ContentValues();
		val.put("VAL", STATO_APERTO);
		val.put("DESC", cont.getResources().getString(R.string.stato_aperto));
		stati.add(val);

		ContentValues val1 = new ContentValues();
		val1.put("VAL", STATO_ACCETTATO);
		val1.put("DESC", cont.getResources().getString(R.string.stato_accettato));
		stati.add(val1);



		ContentValues val2 = new ContentValues();
		val2.put("VAL", STATO_RIFIUTATO);
		val2.put("DESC", cont.getResources().getString(R.string.stato_rifiutato));
		stati.add(val2);

		ContentValues val3 = new ContentValues();
		val3.put("VAL", STATO_CHIUSO);
		val3.put("DESC", cont.getResources().getString(R.string.stato_chiuso));
		stati.add(val3);

		// ContentValues val3 = new ContentValues();
		// val3.put("VAL", STATO_FATTURATO);
		// val3.put("DESC", cont.getResources().getString(R.string.stato_fatturato));
		// stati.add(val3);

		// ContentValues val4 = new ContentValues();
		// val4.put("VAL", STATO_PARZIALMENTE_FATTURATO);
		// val4.put("DESC", cont.getResources().getString(R.string.stato_parzialmente_fatturato));
		// stati.add(val4);

		return stati;
	}


	public ArrayList<Object> getStatiOrdine(Context cont) {
		// TODO Auto-generated method stub
		ArrayList<Object> stati = new ArrayList<Object>();
		ContentValues val = new ContentValues();
		val.put("VAL", STATO_APERTO);
		val.put("DESC", cont.getResources().getString(R.string.stato_aperto));
		stati.add(val);



		ContentValues val3 = new ContentValues();
		val3.put("VAL", STATO_CHIUSO);
		val3.put("DESC", cont.getResources().getString(R.string.stato_chiuso));
		stati.add(val3);

		// ContentValues val3 = new ContentValues();
		// val3.put("VAL", STATO_FATTURATO);
		// val3.put("DESC", cont.getResources().getString(R.string.stato_fatturato));
		// stati.add(val3);

		// ContentValues val4 = new ContentValues();
		// val4.put("VAL", STATO_PARZIALMENTE_FATTURATO);
		// val4.put("DESC", cont.getResources().getString(R.string.stato_parzialmente_fatturato));
		// stati.add(val4);

		return stati;
	}

	public ArrayList<Object> getTipiDocumento(Context cont) {
		// TODO Auto-generated method stub
		ArrayList<Object> stati = new ArrayList<Object>();
		ContentValues val = new ContentValues();
		val.put("VAL", TIPO_PREVENTIVO);
		val.put("DESC", cont.getResources().getString(R.string.preventivo));
		stati.add(val);

		ContentValues val1 = new ContentValues();
		val1.put("VAL", TIPO_ORDINE);
		val1.put("DESC", cont.getResources().getString(R.string.ordine));
		stati.add(val1);

		return stati;
	}

	public CharSequence getDescrizioneStatoPreventivo(String stato, Context cont) {

		// TODO Auto-generated method stub
		if (stato.equals(STATO_APERTO)) {
			return cont.getResources().getString(R.string.stato_aperto);
		}

		if (stato.equals(STATO_ACCETTATO)) {
			return cont.getResources().getString(R.string.stato_accettato);
		}
		if (stato.equals(STATO_RIFIUTATO)) {
			return cont.getResources().getString(R.string.stato_rifiutato);
		}

		if (stato.equals(STATO_CHIUSO)){
			return  cont.getResources().getString(R.string.stato_chiuso);
		}

		return "";
	}

	public ContentValues getClientePreventivo(DbInterno db, int idPreventivo) {
		// TODO Auto-generated method stub
		Join j1 = new Join(NOME_TABELLA, Cantieri.NOME_TABELLA);
		j1.addCampiDiJoin(ID_CANTIERE, Cantieri.ID_CANTIERE);

		Join j2 = new Join(Cantieri.NOME_TABELLA, Anagrafica.NOME_TABELLA);
		j2.addCampiDiJoin(Cantieri.ID_ANAGRAFICA, Anagrafica.ID_ANAGRAFICA);

		Anagrafica tabCliente = new Anagrafica();
		String SQL = "Select " + tabCliente.getNomeCampoTabella("*") + " from " + NOME_TABELLA + j1.getSQLJoin() + j2.getSQLJoin()
				+ " where " + ID_PREVENTIVO + " = " + idPreventivo;
		ArrayList<Object> recs = db.eseguiSelect(SQL, null);

		if (recs.size() > 0) {
			return (ContentValues) recs.get(0);
		}
		return null;
	}

	public ContentValues getClienteCantierePreventivo(DbInterno db, int idPreventivo) {
		// TODO Auto-generated method stub
		Join j1 = new Join(NOME_TABELLA, Cantieri.NOME_TABELLA);
		j1.addCampiDiJoin(ID_CANTIERE, Cantieri.ID_CANTIERE);

		Join j2 = new Join(Cantieri.NOME_TABELLA, Anagrafica.NOME_TABELLA);
		j2.addCampiDiJoin(Cantieri.ID_ANAGRAFICA, Anagrafica.ID_ANAGRAFICA);

		Anagrafica tabCliente = new Anagrafica();
		Cantieri tabCantiere = new Cantieri();
		String SQL = "Select " + getNomeCampoTabella(ID_PREVENTIVO_ORIGINE)+","+ getNomeCampoTabella(TITOLO)+","+ getNomeCampoTabella(TIPO)+"," + getNomeCampoTabella(NUMERO) + "," + getNomeCampoTabella(ANNO) + "," + getNomeCampoTabella(DATA) + ","
				+ tabCliente.getNomeCampoTabella("*") + "," + tabCantiere.getNomeCampoTabella(Cantieri.NOME) + ","
				+ tabCantiere.getNomeCampoTabella(Cantieri.INDIRIZZO) + "," + tabCantiere.getNomeCampoTabella(Cantieri.CITTA) + ","
				+ tabCantiere.getNomeCampoTabella(Cantieri.PROVINCIA) + " from " + NOME_TABELLA + j1.getSQLJoin() + j2.getSQLJoin()
				+ " where " + ID_PREVENTIVO + " = " + idPreventivo;
		ArrayList<Object> recs = db.eseguiSelect(SQL, null);

		if (recs.size() > 0) {
			return (ContentValues) recs.get(0);
		}
		return null;
	}

	public ContentValues getCantierePreventivo(DbInterno db, int idPreventivo) {
		// TODO Auto-generated method stub
		Join j1 = new Join(NOME_TABELLA, Cantieri.NOME_TABELLA);
		j1.addCampiDiJoin(ID_CANTIERE, Cantieri.ID_CANTIERE);

		Cantieri tabCantiere = new Cantieri();
		String SQL = "Select " + tabCantiere.getNomeCampoTabella("*") + " from " + NOME_TABELLA + j1.getSQLJoin() + " where "
				+ ID_PREVENTIVO + " = " + idPreventivo;
		ArrayList<Object> recs = db.eseguiSelect(SQL, null);

		if (recs.size() > 0) {
			return (ContentValues) recs.get(0);
		}
		return null;
	}

	public HashMap<String, Double> calcolaTotaliPreventivo(ArrayList<Object> dati, HashMap<String, Double> mappa_iva, Context ctx) {
		// TODO Auto-generated method stub
		double totaleImponibile = 0.0d;
		double totaleIva = 0.0d;
		double totaleImporto = 0.0d;

		PreventiviDettaglio tabDett = new PreventiviDettaglio();

		for (int i = 0; i < dati.size(); i++) {
			ContentValues curr = (ContentValues) dati.get(i);
			if (!curr.containsKey(PreventivoDettaglioFragment.RIGA_LOCALE)) {
				try {
					double imponibileRiga = tabDett.getImportoRiga(curr);
					totaleImponibile = totaleImponibile + imponibileRiga;
					double aliquotaIva = 0.0d;
					if (mappa_iva.containsKey(curr.getAsString(PreventiviDettaglio.CODICE_IVA))) {
						aliquotaIva = mappa_iva.get(curr.getAsString(PreventiviDettaglio.CODICE_IVA));
						totaleIva = totaleIva + (imponibileRiga * aliquotaIva) / 100;
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
			}
		}

		totaleImponibile = Utility.arrotonda(totaleImponibile, 2);
		totaleIva = Utility.arrotonda(totaleIva, 2);
		totaleImporto = totaleImponibile + totaleIva;
		totaleImporto = Utility.arrotonda(totaleImporto, 2);

		HashMap<String, Double> mappa = new HashMap<String, Double>();
		mappa.put("IMPONIBILE", totaleImponibile);
		mappa.put("IVA", totaleIva);
		mappa.put("IMPORTO", totaleImporto);
		return mappa;
	}

	@Override
	protected void eliminaCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		super.eliminaCorrelati(db, val);
		ContentValues whereDel = new ContentValues();
		whereDel.put(ID_PREVENTIVO, val.getAsInteger(ID_PREVENTIVO));
        ContentValues valDel = db.getRecord(this,whereDel);
		if (valDel!=null && valDel.getAsString(TIPO).equals(TIPO_ORDINE)){
			ElementiCantiere tabElem = new ElementiCantiere();
			ContentValues whereELeme = new ContentValues();
			whereELeme.put(ElementiCantiere.ID_PREVENTIVO, val.getAsInteger(ID_PREVENTIVO));
			//whereELeme.put(ElementiCantiere.ID_CANTIERE, 0);
			ContentValues valUpd = new ContentValues();
			valUpd.put(ElementiCantiere.ID_PREVENTIVO,0);
			valUpd.put(ElementiCantiere.DATA_MOD, Utility.dataToNumber(Calendar.getInstance()));
			db.update(tabElem.getNomeTabella(),valUpd,whereELeme);


			ComponentiCantiere tabComp = new ComponentiCantiere();
			ContentValues whereComp = new ContentValues();
			whereComp.put(ComponentiCantiere.ID_PREVENTIVO, val.getAsInteger(ID_PREVENTIVO));

			ContentValues valUpdComp = new ContentValues();
			valUpdComp.put(ComponentiCantiere.ID_PREVENTIVO,0);
			valUpdComp.put(ComponentiCantiere.DATA_MOD, Utility.dataToNumber(Calendar.getInstance()));
			db.update(tabComp.getNomeTabella(),valUpdComp,whereComp);
		}
		else{
			ElementiCantiere tabElem = new ElementiCantiere();
			ContentValues whereELeme = new ContentValues();
			whereELeme.put(ElementiCantiere.ID_PREVENTIVO, val.getAsInteger(ID_PREVENTIVO));
			//whereELeme.put(ElementiCantiere.ID_CANTIERE, 0);
			ArrayList<Object> elementiPreventivo = db.eseguiSelect(tabElem, whereELeme, null);
			for (int i = 0; i < elementiPreventivo.size(); i++) {
				tabElem.cancellaRecord(db, (ContentValues) elementiPreventivo.get(i));
			}
		}



		PreventiviDettaglio tabDett = new PreventiviDettaglio();
		ArrayList<Object> righe = tabDett.getRighePreventivo(db, val.getAsInteger(ID_PREVENTIVO), null);
		for (int i = 0; i < righe.size(); i++) {
			tabDett.cancellaRecord(db, (ContentValues) righe.get(i));
		}


        Rapportini tabRapp = new Rapportini();
        ContentValues whereRapp = new ContentValues();
        whereRapp.put(Rapportini.ID_ORDINE,val.getAsInteger(ID_PREVENTIVO));
        ArrayList<Object> righeRapp = db.eseguiSelect(tabRapp,whereRapp,null);
        for (int i = 0; i < righeRapp.size(); i++) {
            tabRapp.cancellaRecord(db, (ContentValues) righeRapp.get(i));
        }

	}

	/**
	 * 
	 * @param db
	 * @param idPreventivo
	 */
	public void trasformaPreventivoInOrdine(DbInterno db, int idPreventivo) {

		ContentValues where = new ContentValues();
		where.put(ID_PREVENTIVO, idPreventivo);
		ContentValues valPrev = db.getRecord(this, where);
		if (valPrev != null) {
			// 1) Copio il preventivo e le righe del preventivo
			int anno = Calendar.getInstance().get(Calendar.YEAR);
			ContentValues valNuovi = new ContentValues();

			valNuovi.put(TIPO, TIPO_ORDINE);
			valNuovi.put(DATA, Utility.dataToNumber(Calendar.getInstance()));
			valNuovi.put(ANNO, anno);
			valNuovi.put(NUMERO, getProssimoNumeroOrdine(db, anno));
			valNuovi.put(ID_PREVENTIVO_ORIGINE, idPreventivo);

			ContentValues valCopia = copiaRecord(db, valPrev, valNuovi);
			PreventiviDettaglio tabDett = new PreventiviDettaglio();
			// ContentValues whereDett = new ContentValues();
			// whereDett.put(PreventiviDettaglio.ID_PREVENTIVO, idPreventivo);

			Join jElem = new Join(PreventiviDettaglio.NOME_TABELLA, Elementi.NOME_TABELLA, Join.LEFT_JOIN);
			jElem.addCampiDiJoin(PreventiviDettaglio.ID_ELEMENTO, Elementi.ID_ELEMENTO);

			String SQLRIGHE = "Select " + PreventiviDettaglio.NOME_TABELLA + ".*," + Elementi.ID_CATEGORIA_GENERALE + " from "
					+ PreventiviDettaglio.NOME_TABELLA + jElem.getSQLJoin() + " where " + ID_PREVENTIVO + "=" + idPreventivo;

			ArrayList<Object> righe = db.eseguiSelect(SQLRIGHE, null);

			for (int i = 0; i < righe.size(); i++) {
				ContentValues rigaCurr = (ContentValues) righe.get(i);
				// se sono su una riga di locale di tipo cavo o tubo non la copio:
				// saranno i collegamenti nell'ordine a conteggiare tubi e cavi
				String idCatGen = rigaCurr.getAsString(Elementi.ID_CATEGORIA_GENERALE);
				if (rigaCurr.getAsString(PreventiviDettaglio.TIPO).equals(PreventiviDettaglio.MATERIALE) && idCatGen != null) {
					if (idCatGen.equals("" + CategorieGenerali.TUBI) || idCatGen.equals("" + CategorieGenerali.CAVI)) {
						continue;
					}
				}
				valNuovi.clear();
				valNuovi.put(PreventiviDettaglio.ID_PREVENTIVO, valCopia.getAsInteger(ID_PREVENTIVO));
				if (rigaCurr.containsKey(Elementi.ID_CATEGORIA_GENERALE)) {
					rigaCurr.remove(Elementi.ID_CATEGORIA_GENERALE);
				}

				tabDett.copiaRecord(db, rigaCurr, valNuovi);
			}

			// 2) Imposto lo stato accettato sul preventivo
			ContentValues valUp = getValoriLogModifica(db);
			valUp.put(STATO, STATO_ACCETTATO);
			aggiornaRecord(db, valUp, where);




			// 3 aggiorno l'id preventivo sugli elementi e sui componenti del preventivo con l'id dell'ordine appena creato
			// elimino gli eventuali elementi sostituiti e

			ContentValues wherePrev = new ContentValues();
			where.put(ID_PREVENTIVO, idPreventivo);
			ArrayList<Object> righeDaCopiare = db.eseguiSelect(new ElementiCantiere(), wherePrev, null);
			for (int i=0;i<righeDaCopiare.size();i++){
				ContentValues curr = (ContentValues)righeDaCopiare.get(i);
				if (curr.get(ElementiCantiere.ID_ELEMENTO_CANT_ORIGINE)!=null){
					int idElementoCantOrigine = curr.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT_ORIGINE);
					if (idElementoCantOrigine!=0){
						//elimino l'elemento e aggiorno l'eventuale ordine collegato
						ArrayList<Object> preventiviDaAggiornare = db.eseguiSelect("Select distinct componenti_cantiere.id_preventivo  from componenti_cantiere inner join composizioni_cantiere on componenti_cantiere.id_componente_cant=composizioni_cantiere.id_componente_cant inner join elementi_cantiere on composizioni_cantiere.id_elemento_cant=elementi_cantiere.id_elemento_cant where elementi_cantiere.id_elemento_cant="+idElementoCantOrigine+" and  componenti_cantiere.id_preventivo<>0",null);
						ContentValues val = db.getRecord("Select * from elementi_cantiere where id_elemento_cant="+idElementoCantOrigine);
						ElementiCantiere tabElemCant = new ElementiCantiere();
						ContentValues whereElemeCant = new ContentValues();
						whereElemeCant.put(ElementiCantiere.ID_ELEMENTO_CANT,idElementoCantOrigine);
						tabElemCant.cancellaRecord(db, whereElemeCant);

						PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();

						//poichè alcuni elementi potrebbero essere composti da componenti appartenenti ad ordini diversi allora estraggo prima tutti i preventivi interessati e per ognuno chiamo la funzione di aggiornamento
						if (preventiviDaAggiornare.size()==0){
							tabPrevDett.aggiornaRighePreventivoLocale(db,val.getAsInteger(ElementiCantiere.ID_PREVENTIVO),val.getAsInteger(ElementiCantiere.ID_LOCALE));
						}
						else{
							for (int j=0;j<preventiviDaAggiornare.size();j++){
								tabPrevDett.aggiornaRighePreventivoLocale(db,((ContentValues)preventiviDaAggiornare.get(j)).getAsInteger(ComponentiCantiere.ID_PREVENTIVO),val.getAsInteger(ElementiCantiere.ID_LOCALE));
							}
						}
					}
				}
				//aggiorno l'id preventivo sull'elemento e sui componenti
			/*	ElementiCantiere tabElemCant = new ElementiCantiere();
				ContentValues valUPD = new ContentValues();
				ContentValues whereElemeCant = new ContentValues();
				whereElemeCant.put(ElementiCantiere.ID_ELEMENTO_CANT,curr.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT));
				valUPD.put(ElementiCantiere.ID_PREVENTIVO,valCopia.getAsInteger(ID_PREVENTIVO));
				tabElemCant.aggiornaRecord(db,valUPD,where);
				//prendo tutti i componenti dell'elemento
				ComposizioniCantiere tabComp = new ComposizioniCantiere();
				ArrayList<Object> componenti =  tabComp.getComposizioneElementoCantiere(db,curr.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT));*/



			}

			// 4 Copio le tabelle elementi_cantiere, composizioni_cantiere e componenti_cantiere
			ElementiCantiere tabElementi = new ElementiCantiere();
			tabElementi.copiaElementiPreventivo(db, idPreventivo, valCopia.getAsInteger(ID_PREVENTIVO));
		}

	}

	@Override
	protected int getMassimoNumeroRecordLicenzaGratis() {
		// TODO Auto-generated method stub
		return 2;
	}

    /**
     * SQL dei clienti della ditta selezionata che hanno almeno un ordine APERTO: sono i soli per cui si puo'
     * aprire un rapportino.
     */
    public static String getSqlClientiConOrdiniAperti() {
        return "Select distinct " + Anagrafica.NOME_TABELLA + ".* from " + Anagrafica.NOME_TABELLA
                + " inner join " + Cantieri.NOME_TABELLA + " on " + Cantieri.NOME_TABELLA + "." + Cantieri.ID_ANAGRAFICA
                + "=" + Anagrafica.NOME_TABELLA + "." + Anagrafica.ID_ANAGRAFICA
                + " inner join " + NOME_TABELLA + " on " + NOME_TABELLA + "." + ID_CANTIERE + "=" + Cantieri.NOME_TABELLA + "." + Cantieri.ID_CANTIERE
                + " where " + NOME_TABELLA + "." + TIPO + "='" + TIPO_ORDINE + "' and " + NOME_TABELLA + "." + STATO + "='" + STATO_APERTO + "'"
                + " and " + Cantieri.NOME_TABELLA + "." + Cantieri.ID_DITTA + "=" + Sessione.getDittaSelezionata()
                + " order by " + Anagrafica.NOME_TABELLA + "." + Anagrafica.RAGIONE_SOCIALE;
    }

    /** True se nella ditta selezionata c'e' almeno un ordine aperto. */
    public boolean esistonoOrdiniAperti(DbInterno db) {
        return !db.eseguiSelect(getSqlClientiConOrdiniAperti(), null).isEmpty();
    }

    /** True se l'ordine esiste, e' un ordine ed e' aperto. */
    public boolean isOrdineAperto(DbInterno db, int idOrdine) {
        ContentValues where = new ContentValues();
        where.put(ID_PREVENTIVO, idOrdine);
        ContentValues rec = db.getRecord(this, where);
        return rec != null && TIPO_ORDINE.equals(rec.getAsString(TIPO)) && STATO_APERTO.equals(rec.getAsString(STATO));
    }

    /**
     * Ordini del cliente nella ditta selezionata. Con soloAperti restano i soli ordini aperti; idOrdineSempreIncluso
     * (0 = nessuno) resta in elenco anche se non piu' aperto, per non perdere l'ordine di un rapportino gia' esistente.
     */
    public ArrayList<Object> getOrdiniCliente(DbInterno db, int codicecliente, boolean soloAperti, int idOrdineSempreIncluso) {
        Join j1 = new Join(NOME_TABELLA, Cantieri.NOME_TABELLA);
        j1.addCampiDiJoin(ID_CANTIERE, Cantieri.ID_CANTIERE);

        Join j2 = new Join(Cantieri.NOME_TABELLA, Anagrafica.NOME_TABELLA);
        j2.addCampiDiJoin(Cantieri.ID_ANAGRAFICA, Anagrafica.ID_ANAGRAFICA);

        String filtroStato = "";
        if (soloAperti) {
            filtroStato = " and (" + NOME_TABELLA + "." + STATO + "='" + STATO_APERTO + "'"
                    + (idOrdineSempreIncluso > 0 ? " or " + NOME_TABELLA + "." + ID_PREVENTIVO + "=" + idOrdineSempreIncluso : "") + ")";
        }
        String SQL = "Select " + NOME_TABELLA+".*," + Cantieri.NOME +" as nome_cantiere from " + NOME_TABELLA + j1.getSQLJoin() + j2.getSQLJoin()
                + " where " + Anagrafica.NOME_TABELLA+"."+Anagrafica.ID_ANAGRAFICA + " = " + codicecliente + " and "+TIPO+"='"+TIPO_ORDINE+"' and "+Cantieri.ID_DITTA+"="+ Sessione.getDittaSelezionata()
                + filtroStato + " order by " + DATA + " desc";
        return db.eseguiSelect(SQL, null);
    }

	/**
	 * spsota tutti gli elementi_cantiere ,i componenti_cantiere e i collegamenti dall'ordine al cantiere
	 * @param db
	 * @param idOrdine
     */
	public void chiudiOrdine(DbInterno db, int idOrdine) {

		ContentValues valUpd = new ElementiCantiere().getValoriLogModifica(db);
		valUpd.put(ElementiCantiere.ID_PREVENTIVO,0);
		ContentValues where = new ContentValues();
		where.put(ElementiCantiere.ID_PREVENTIVO,idOrdine);
		db.update(ElementiCantiere.NOME_TABELLA,valUpd,where);

		ContentValues valUpd2 = new ComposizioniCantiere().getValoriLogModifica(db);
		valUpd2.put(ComponentiCantiere.ID_PREVENTIVO,0);
		ContentValues where2 = new ContentValues();
		where2.put(ComponentiCantiere.ID_PREVENTIVO,idOrdine);
		db.update(ComponentiCantiere.NOME_TABELLA,valUpd2,where2);

		ContentValues valUpd3 = new Collegamenti().getValoriLogModifica(db);
		valUpd3.put(Collegamenti.ID_ORDINE,0);
		ContentValues where3 = new ContentValues();
		where3.put(Collegamenti.ID_ORDINE,idOrdine);
		db.update(Collegamenti.NOME_TABELLA,valUpd3,where3);
	}
}
