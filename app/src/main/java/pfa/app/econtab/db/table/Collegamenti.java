package pfa.app.econtab.db.table;

import android.content.ContentValues;
import android.content.Context;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;

public class Collegamenti extends AbstractTable {
	public static final String NOME_TABELLA = "collegamenti";

	public static final String ID_COLLEGAMENTO = "id_collegamento";
	public static final String ID_ORDINE = "id_ordine";
	public static final String ID_ELEMENTO_CANT1 = "id_elemento_cant1";
	public static final String ID_COMPONENTE_CANT1 = "id_componente_cant1";
	public static final String ID_ELEMENTO_CANT2 = "id_elemento_cant2";
	public static final String ID_COMPONENTE_CANT2 = "id_componente_cant2";
	public static final String ID_TUBO = "id_tubo";
	public static final String ID_CAVO = "id_cavo";
	public static final String METRI = "metri";
	public static final String METRI_CAVO_UNI = "metri_cavo_uni";
	public static final String QTA_CAVO = "qta_cavo";
	public static final String METRI_CODA = "metri_coda";
	public static final String NOTA = "nota";
	public static final String ID_COLLEGAMENTO_TUBO = "id_collegamento_tubo";

    private int idElementoTuboPrec = 0;
    private int idElementoCavoPrec = 0;

	public Collegamenti() {
		// TODO Auto-generated constructor stub
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_COLLEGAMENTO);

		aggiungiCampo(ID_COLLEGAMENTO, INTEGER);
		aggiungiCampo(ID_ORDINE, INTEGER);
		aggiungiCampo(ID_ELEMENTO_CANT1, INTEGER);
		aggiungiCampo(ID_COMPONENTE_CANT1, INTEGER);
		aggiungiCampo(ID_ELEMENTO_CANT2, INTEGER);
		aggiungiCampo(ID_COMPONENTE_CANT2, INTEGER);
		aggiungiCampo(ID_TUBO, INTEGER);
		aggiungiCampo(ID_CAVO, INTEGER);
		aggiungiCampo(METRI, NUMERIC);
		aggiungiCampo(METRI_CAVO_UNI, NUMERIC);
		aggiungiCampo(QTA_CAVO, INTEGER);
		aggiungiCampo(METRI_CODA, NUMERIC);
		aggiungiCampo(NOTA, TEXT);
		aggiungiCampo(ID_COLLEGAMENTO_TUBO, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_COLLEGAMENTO);
	}

	public ArrayList<Integer> getComponentiCollegatiElemento(DbInterno db, int idElemento) {
		String SQL = "Select * from " + Collegamenti.NOME_TABELLA + " where (" + Collegamenti.ID_ELEMENTO_CANT1 + " = " + idElemento
				+ " or " + Collegamenti.ID_ELEMENTO_CANT2 + " = " + idElemento + ") ";
		ArrayList<Object> collegamentiElemento = db.eseguiSelect(SQL, null);
		ArrayList<Integer> componentiCollegati = new ArrayList<Integer>();
		for (int i = 0; i < collegamentiElemento.size(); i++) {
			ContentValues curr = (ContentValues) collegamentiElemento.get(i);
			int comp1 = curr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT1);
			int comp2 = curr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT2);
			if (comp1 != 0 && !componentiCollegati.contains(comp1)) {
				componentiCollegati.add(comp1);
			}
			if (comp2 != 0 && !componentiCollegati.contains(comp2)) {
				componentiCollegati.add(comp2);
			}
		}
		return componentiCollegati;
	}

	public ArrayList<Object> getCollegamentiComponente(DbInterno db, int idComponente) {
		String SQL = "Select * from " + Collegamenti.NOME_TABELLA + " where (" + Collegamenti.ID_COMPONENTE_CANT1 + " = " + idComponente
				+ " or " + Collegamenti.ID_COMPONENTE_CANT2 + " = " + idComponente + ") ";
		ArrayList<Object> collegamenti = db.eseguiSelect(SQL, null);
		return collegamenti;
	}

	@Override
	protected void inserimentoCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		_aggiornaCollegamentiOrdine(db, val);
		super.inserimentoCorrelati(db, val);
	}

	@Override
	protected void aggiornamentoCorrelati(DbInterno db, ContentValues val, ContentValues where) {
		// TODO Auto-generated method stub
		ContentValues valRec = db.getRecord(this, where);
		_aggiornaCollegamentiOrdine(db, valRec);
		super.aggiornamentoCorrelati(db, val, where);
	}


    @Override
    protected void eliminaCorrelati(DbInterno db, ContentValues val) {
        super.eliminaCorrelati(db, val);
        //prendo i collegamenti cavi passanti per il tubo e li elimino
        ContentValues whereCavo = new ContentValues();
        whereCavo.put(ID_COLLEGAMENTO_TUBO,val.getAsInteger(ID_COLLEGAMENTO));

        ArrayList<Object> caviPassanti = db.eseguiSelect(this,whereCavo,null);
        for (int i=0;i<caviPassanti.size();i++){
            cancellaRecord(db,(ContentValues)caviPassanti.get(i));
        }

    }

    @Override
	public int cancellaRecord(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		ContentValues where = new ContentValues();
		where.put(ID_COLLEGAMENTO, val.getAsInteger(ID_COLLEGAMENTO));
		ContentValues valRec = db.getRecord(this, where);
		int ret = super.cancellaRecord(db, val);
		_aggiornaCollegamentiOrdine(db, valRec);
		return ret;
	}


    @Override
    public int aggiornaRecord(DbInterno db, ContentValues val, ContentValues where) {

        ContentValues valRecPrec = db.getRecord(this, where);
        //imposto il cavo e il tubo precedente all'aggiornamento per togliere dal preventivo
        idElementoTuboPrec = valRecPrec.getAsInteger(ID_TUBO);
        idElementoCavoPrec = valRecPrec.getAsInteger(ID_CAVO);
        int result =  super.aggiornaRecord(db, val, where);
        idElementoCavoPrec = 0;
        idElementoTuboPrec = 0;
        return result;
    }

    private void _aggiornaCollegamentiOrdine(DbInterno db, ContentValues valRec) {
		// TODO Auto-generated method stub
		if (valRec!=null &&  valRec.getAsInteger(ID_ORDINE) != 0) {
			int idOrdine = valRec.getAsInteger(ID_ORDINE);

            for (int i=0;i<2;i++){
                //primo giro id nuovi, secondo giro idVecchi se diversi
                int idTubo = valRec.getAsInteger(ID_TUBO);
                int idCavo = valRec.getAsInteger(ID_CAVO);

                if (i==1 ){
                    if ((idElementoCavoPrec==idCavo && idElementoTuboPrec==idTubo) || (idElementoCavoPrec==0 && idElementoTuboPrec==0) ){
                        idTubo = 0;//mi fermo se gli id sono uguali o se non sono in aggiornamento
                        idCavo = 0;
                    }
                    else{
                        idTubo = idElementoTuboPrec;
                        idCavo = idElementoCavoPrec;
                    }

                }

                String SQL_SUM = "";
                String SQL_ORD = "";

                int idElemento = 0;
                if (idTubo != 0) {
                    idElemento = idTubo;
                    SQL_SUM = "Select Sum(" + METRI + ") as TOT from " + NOME_TABELLA + " where " + ID_TUBO + "=" + idTubo + " and "
                            + ID_ORDINE + "=" + idOrdine + " group by " + ID_TUBO;

                    SQL_ORD = "Select * from " + PreventiviDettaglio.NOME_TABELLA + " where " + PreventiviDettaglio.TIPO + "='"
                            + PreventiviDettaglio.COLLEGAMENTI + "' and " + PreventiviDettaglio.ID_ELEMENTO + "=" + idTubo + " and "
                            + PreventiviDettaglio.ID_PREVENTIVO + "=" + idOrdine;
                }
                if (idCavo != 0) {
                    idElemento = idCavo;
                    SQL_SUM = "Select (Sum(" + METRI + ")+Sum(" + METRI_CODA + "*" + QTA_CAVO + ")) as TOT from " + NOME_TABELLA + " where " + ID_CAVO + "=" + idCavo + " and "
                            + ID_ORDINE + "=" + idOrdine + " group by " + ID_CAVO;
                    SQL_ORD = "Select * from " + PreventiviDettaglio.NOME_TABELLA + " where " + PreventiviDettaglio.TIPO + "='"
                            + PreventiviDettaglio.COLLEGAMENTI + "' and " + PreventiviDettaglio.ID_ELEMENTO + "=" + idCavo + " and "
                            + PreventiviDettaglio.ID_PREVENTIVO + "=" + idOrdine;

                }
                // prendo la riga dall'ordine se esiste
                double metriTotCollegamenti = 0d;
                double metriTotOrdine = 0d;
                ContentValues recPrev = null;

                if (!SQL_ORD.equals("")) {
                    ArrayList<Object> tuboPrev = db.eseguiSelect(SQL_ORD, null);
                    if (tuboPrev.size() > 0) {
                        recPrev = (ContentValues) tuboPrev.get(0);
                        metriTotOrdine = recPrev.getAsDouble(PreventiviDettaglio.QUANTITA);
                    }

                    ArrayList<Object> tuboTot = db.eseguiSelect(SQL_SUM, null);
                    if (tuboTot.size() > 0) {
                        ContentValues tuboTotRec = (ContentValues) tuboTot.get(0);
                        metriTotCollegamenti = tuboTotRec.getAsDouble("TOT");
                        if (recPrev != null) {
                            // aggiorno
                            if (metriTotCollegamenti != metriTotOrdine) {
                                PreventiviDettaglio tabPrev = new PreventiviDettaglio();
                                ContentValues valUPD = tabPrev.getValoriLogModifica(db);
                                valUPD.put(PreventiviDettaglio.QUANTITA, metriTotCollegamenti);
                                ContentValues whereUPD = new ContentValues();
                                whereUPD.put(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO,
                                        recPrev.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO));
                                tabPrev.aggiornaRecord(db, valUPD, whereUPD);
                            }
                        } else {
                            // inserisco
                            Preventivi tabPrev = new Preventivi();
                            ContentValues recCliente = tabPrev.getClientePreventivo(db, idOrdine);
                            String codiceIVA = "";
                            if (recCliente != null) {
                                codiceIVA = recCliente.getAsString(Anagrafica.CODICE_IVA);
                            }

                            ContentValues whereElem = new ContentValues();
                            whereElem.put(Elementi.ID_ELEMENTO, idElemento);
                            ContentValues recElem = db.getRecord(new Elementi(), whereElem);

                            PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();

                            ContentValues valINS = tabPrevDett.getValoriLogInserimento(db);
                            valINS.put(PreventiviDettaglio.ID_PREVENTIVO, idOrdine);
                            valINS.put(PreventiviDettaglio.TIPO, PreventiviDettaglio.COLLEGAMENTI);
                            valINS.put(PreventiviDettaglio.ID_ELEMENTO, idElemento);
                            if (recElem != null) {

                                valINS.put(PreventiviDettaglio.UNITA_MISURA, recElem.getAsString(Elementi.UNITA_MISURA));
                                valINS.put(PreventiviDettaglio.DESCRIZIONE, recElem.getAsString(Elementi.NOME_ELEMENTO));
                            }
                            valINS.put(PreventiviDettaglio.QUANTITA, metriTotCollegamenti);
                            valINS.put(PreventiviDettaglio.CODICE_IVA, codiceIVA);
                            valINS.put(PreventiviDettaglio.STATO, PreventiviDettaglio.APERTO);
                            tabPrevDett.inserisciRecord(db, valINS);

                        }

                    } else {
                        // elimino il record dal preventivo
                        if (recPrev != null) {
                            PreventiviDettaglio tabPrev = new PreventiviDettaglio();
                            tabPrev.cancellaRecord(db, recPrev);
                        }

                    }

                }
            }


		}
	}

	// METODI PER LA VISUALIZZAZIONE DEI COLLEGAMENTI

	public void aggiungiTuboCavo(ContentValues curr, ArrayList<Object> tubi, ArrayList<Object> cavi, Context ctx) {
		// TODO Auto-generated method stub
		curr.put("NOME", "");
		if (curr.getAsInteger(Collegamenti.ID_TUBO) != null) {
			int idtubo = curr.getAsInteger(Collegamenti.ID_TUBO);
			if (idtubo != 0) {
				curr.put("TIPO", ctx.getString(R.string.tubo));
				for (int i = 0; i < tubi.size(); i++) {
					ContentValues tubo = (ContentValues) tubi.get(i);
					if (tubo.getAsInteger(Elementi.ID_ELEMENTO) == idtubo) {
						curr.put("NOME", tubo.getAsString(Elementi.NOME_ELEMENTO));
						break;
					}
				}
			}
		}
		if (curr.getAsInteger(Collegamenti.ID_CAVO) != null) {
			int idcavo = curr.getAsInteger(Collegamenti.ID_CAVO);
			if (idcavo != 0) {
				curr.put("TIPO", ctx.getString(R.string.cavo));
				for (int i = 0; i < cavi.size(); i++) {
					ContentValues cavo = (ContentValues) cavi.get(i);
					if (cavo.getAsInteger(Elementi.ID_ELEMENTO) == idcavo) {
						curr.put("NOME", cavo.getAsString(Elementi.NOME_ELEMENTO));
						break;
					}
				}
			}
		}
	}

	/**
	 * Ritorna i locali del cantiere a partire dall'elemento
	 * 
	 * @param db
	 * @param idElemento
	 * @return
	 */
	public ArrayList<Object> caricaLocaliCantiere(DbInterno db, int idElemento) {
		// TODO Auto-generated method stub
		ArrayList<Object> locali = new ArrayList<Object>();
		ElementiCantiere tabElem = new ElementiCantiere();
		ContentValues where = new ContentValues();
		where.put(ElementiCantiere.ID_ELEMENTO_CANT, idElemento);
		ContentValues valElem = db.getRecord(tabElem, where);
		if (valElem != null) {
			int idCantiere = valElem.getAsInteger(ElementiCantiere.ID_CANTIERE);

			if (idCantiere == 0) {
				// prendo il cantiere dal preventivo
				int idOrdine = valElem.getAsInteger(ElementiCantiere.ID_PREVENTIVO);
				Preventivi tabPrev = new Preventivi();
				ContentValues whereOrdine = new ContentValues();
				whereOrdine.put(Preventivi.ID_PREVENTIVO, idOrdine);
				ContentValues valOrdine = db.getRecord(tabPrev, whereOrdine);
				if (valOrdine != null) {
					idCantiere = valOrdine.getAsInteger(Preventivi.ID_CANTIERE);
				}
			}
			Locali tabLocali = new Locali();
			Aree tabAree = new Aree();
			locali = tabLocali.getLocaliCantiereConArea(db, idCantiere);

		}

		return locali;
	}

	/**
	 * Aggiunge ai dati l'informazione del nome locale Utilizzato nella visualizzazione dei collegamenti
	 * 
	 * @param curr
	 * @param locali
	 */
	public void aggiungiLocali(ContentValues curr, ArrayList<Object> locali) {
		// TODO Auto-generated method stub
		Aree tabAree = new Aree();
		int idLocaleA = curr.getAsInteger("ID_LOCALE_A");
		if (idLocaleA != 0) {
			for (int i = 0; i < locali.size(); i++) {
				ContentValues locale = (ContentValues) locali.get(i);
				if (locale.getAsInteger(Locali.ID_LOCALE) == idLocaleA) {
					String area = "";
					if (locale.containsKey(tabAree.getAlias(Aree.NOME))) {
						area = locale.getAsString(tabAree.getAlias(Aree.NOME)) + " - ";
					}
					curr.put("LOCALE_A", area + locale.getAsString(Locali.NOME));
					break;
				}
			}
		}

		int idLocaleB = curr.getAsInteger("ID_LOCALE_B");
		if (idLocaleB != 0) {
			for (int i = 0; i < locali.size(); i++) {
				ContentValues locale = (ContentValues) locali.get(i);
				if (locale.getAsInteger(Locali.ID_LOCALE) == idLocaleB) {
					String area = "";
					if (locale.containsKey(tabAree.getAlias(Aree.NOME))) {
						area = locale.getAsString(tabAree.getAlias(Aree.NOME)) + " - ";
					}
					curr.put("LOCALE_B", area + locale.getAsString(Locali.NOME));
					break;
				}
			}
		}
	}

}
