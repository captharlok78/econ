package pfa.app.econtab.db.table;

import android.content.ContentValues;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.utils.Utility;

public class PreventiviDettaglio extends AbstractTable {
	public static final String NOME_TABELLA = "preventivi_dettaglio";

	public static final String ID_PREVENTIVO_DETTAGLIO = "id_preventivo_dettaglio";
	public static final String ID_PREVENTIVO = "id_preventivo";
	public static final String TIPO = "tipo";
	public static final String DESCRIZIONE = "descrizione";
	public static final String ID_LOCALE = "id_locale";
	public static final String ID_ELEMENTO = "id_elemento";
	public static final String ID_COMPONENTE = "id_componente";
	public static final String ID_PLACCA_MODULI = "id_placca_moduli";
	public static final String CODICE_ARTICOLO = "codice_articolo";
	public static final String ID_MANODOPERA = "id_manodopera";
	public static final String NUM_OPERATORI = "num_operatori";
	public static final String UNITA_MISURA = "unita_misura";
	public static final String QUANTITA = "quantita";
	public static final String PREZZO_ACQ = "prezzo_acq";
	public static final String RICARICO = "ricarico";
	public static final String PREZZO_VEN = "prezzo_ven";
	public static final String SCONTO = "sconto";
	public static final String PREZZO = "prezzo";
	public static final String CODICE_IVA = "codice_iva";
	public static final String STATO = "stato";
	public static final String NOTE = "note";
    public static final String ID_LINEA = "id_linea";

	public static final String MATERIALE = "MA";
	public static final String MANOPERA = "MD";
	public static final String MATERIALE_PREVENTIVO = "MP";
	public static final String ALTRO = "AL";
	public static final String PLACCHE = "PL";
	public static final String PLACCHE_PREVENTIVO = "PP";
	public static final String COLLEGAMENTI = "CL";
	public static final String EXTRA = "EX";

	public static final String APERTO = "A";
	public static final String CHIUSO = "C";
	public static final String FATTURATO = "F";

	public static final String PATH_EXPORT_PREVENTIVI = "preventivi";
    public static final String PATH_EXPORT_ORDINI = "ordini";
    public static final String PATH_EXPORT_CONSUNTIVI = "consuntivi";

	private boolean isAggiornamentoCaviETubi = false; // questo flag mi dice che sono in aggiornamento di un cavo o di
														// un tubo e quindi per evitare loop non devo eseguire le
														// funioni di aggiornamento dei cavi e dei tubi

	private boolean aggiornaCaviETubi = false; // questo flag mi dice che devo eseguire l'aggiornamento dei cavi e dei
												// tubi quando inserisco un nuvo elemento/componente a preventivo non
												// sul cantiere
	private double quantitaAggiunta = 0.0D;
	public boolean eseguiAggiornamentoTubiECavi = true;

	public PreventiviDettaglio() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_PREVENTIVO_DETTAGLIO);

		aggiungiCampo(ID_PREVENTIVO_DETTAGLIO, INTEGER);
		aggiungiCampo(ID_PREVENTIVO, INTEGER);
		aggiungiCampo(TIPO, TEXT);
		aggiungiCampo(DESCRIZIONE, TEXT);
		aggiungiCampo(ID_LOCALE, INTEGER);
		aggiungiCampo(ID_ELEMENTO, INTEGER);
		aggiungiCampo(ID_COMPONENTE, INTEGER);
		aggiungiCampo(ID_PLACCA_MODULI, INTEGER);
		aggiungiCampo(CODICE_ARTICOLO, TEXT);
		aggiungiCampo(ID_MANODOPERA, INTEGER);
		aggiungiCampo(NUM_OPERATORI, INTEGER);
		aggiungiCampo(UNITA_MISURA, TEXT);
		aggiungiCampo(QUANTITA, NUMERIC);
		aggiungiCampo(PREZZO_ACQ, NUMERIC);
		aggiungiCampo(RICARICO, NUMERIC);
		aggiungiCampo(PREZZO_VEN, NUMERIC);
		aggiungiCampo(SCONTO, NUMERIC);
		aggiungiCampo(PREZZO, NUMERIC);
		aggiungiCampo(CODICE_IVA, TEXT);
		aggiungiCampo(STATO, TEXT);
		aggiungiCampo(NOTE, TEXT);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);
        aggiungiCampo(ID_LINEA,INTEGER);

		aggiungiCampoChiave(ID_PREVENTIVO_DETTAGLIO);
	}

	@Override
	protected void inserimentoCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		if (!isAggiornamentoCaviETubi) {
			String tipo = val.getAsString(TIPO);
			/*if (tipo.equals(MATERIALE)) {
				boolean aggiornamentoDaEseguire = true;// non � da eseguire solo quando � una riga di composizione di un
				// componente
				int idComponente = val.getAsInteger(ID_COMPONENTE);
				if (idComponente != 0) {
					Componenti tabComp = new Componenti();
					ContentValues whereComp = new ContentValues();
					whereComp.put(Componenti.ID_COMPONENTE, idComponente);
					ContentValues valComp = db.getRecord(tabComp, whereComp);
					if (valComp != null && valComp.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.COMPONENTI_INTERRUTTORI) {
						aggiornamentoDaEseguire = false;
					}
				}
				if (aggiornamentoDaEseguire) {
					aggiornaTubiECaviLocale(db, val.getAsInteger(ID_PREVENTIVO), val.getAsInteger(ID_LOCALE));
				}
			}*/

			if (tipo.equals(MATERIALE_PREVENTIVO) && isAggiornaCaviETubi()) {
				aggiornaTubiECaviMaterialePreventivo(db, val);

			}
		}
		super.inserimentoCorrelati(db, val);
	}

	@Override
	protected void aggiornamentoCorrelati(DbInterno db, ContentValues val, ContentValues where) {
		// TODO Auto-generated method stub
		if (!isAggiornamentoCaviETubi) {
			ContentValues valRec = db.getRecord(this, where);
			String tipo = valRec.getAsString(TIPO);
			/*if (tipo.equals(MATERIALE)) {
				boolean aggiornamentoDaEseguire = true;// non � da eseguire solo quando � una riga di composizione di un
														// componente
				int idComponente = valRec.getAsInteger(ID_COMPONENTE);
				if (idComponente != 0) {
					Componenti tabComp = new Componenti();
					ContentValues whereComp = new ContentValues();
					whereComp.put(Componenti.ID_COMPONENTE, idComponente);
					ContentValues valComp = db.getRecord(tabComp, whereComp);
					if (valComp != null && valComp.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.COMPONENTI_INTERRUTTORI) {
						aggiornamentoDaEseguire = false;
					}
				}
				if (aggiornamentoDaEseguire) {
					aggiornaTubiECaviLocale(db, valRec.getAsInteger(ID_PREVENTIVO), valRec.getAsInteger(ID_LOCALE));
				}

			}*/

			if (tipo.equals(MATERIALE_PREVENTIVO) && isAggiornaCaviETubi()) {
				ContentValues valUpd = valRec;
				valUpd.put(QUANTITA, getQuantitaAggiunta());
				aggiornaTubiECaviMaterialePreventivo(db, valUpd);

			}

		}
		super.aggiornamentoCorrelati(db, val, where);
	}

	/*
	 * @Override
	 * 
	 * public int cancellaRecord(DbInterno db, ContentValues val, boolean perchiave) { // TODO Auto-generated method
	 * stub ContentValues valRec = db.getRecord(this, getFiltroPerChiave(val)); int result = super.cancellaRecord(db,
	 * val, perchiave); if (!isAggiornamentoCaviETubi) { String tipo = valRec.getAsString(TIPO); if
	 * (tipo.equals(MATERIALE)) {
	 * 
	 * aggiornaTubiECaviLocale(db, valRec.getAsInteger(ID_PREVENTIVO), valRec.getAsInteger(ID_LOCALE)); }
	 * 
	 * } return result; }
	 */
	public void aggiornaTubiECaviMaterialePreventivo(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		isAggiornamentoCaviETubi = true;
		int idElemento = 0;
		int idComponente = 0;

		if (val.containsKey(ID_ELEMENTO)) {
			idElemento = val.getAsInteger(ID_ELEMENTO);
		}

		if (val.containsKey(ID_COMPONENTE)) {
			idComponente = val.getAsInteger(ID_COMPONENTE);
		}

		int idCavo = 0;
		int idTubo = 0;
		double metriCavo = 0.0D;
		double metriTubo = 0.0D;
		ContentValues recCavo = null;
		ContentValues recTubo = null;

		if (idElemento != 0) {
			ContentValues where = new ContentValues();
			where.put(Elementi.ID_ELEMENTO, idElemento);

			ContentValues valElem = db.getRecord(new Elementi(), where);
			if (valElem != null) {
				idCavo = valElem.getAsInteger(Elementi.ID_ELEMENTO_CAVO);
				metriCavo = valElem.getAsDouble(Elementi.METRI_CAVO);
				idTubo = valElem.getAsInteger(Elementi.ID_ELEMENTO_TUBO);
				metriTubo = valElem.getAsDouble(Elementi.METRI_TUBO);

			}
		}

		if (idComponente != 0) {
			ContentValues where = new ContentValues();
			where.put(Componenti.ID_COMPONENTE, idComponente);

			ContentValues valElem = db.getRecord(new Componenti(), where);
			if (valElem != null) {
				idCavo = valElem.getAsInteger(Componenti.ID_ELEMENTO_CAVO);
				metriCavo = valElem.getAsDouble(Componenti.METRI_CAVO);
				idTubo = valElem.getAsInteger(Componenti.ID_ELEMENTO_TUBO);
				metriTubo = valElem.getAsDouble(Componenti.METRI_TUBO);

			}
		}

		if (idCavo != 0) {
			ContentValues whereCavo = new ContentValues();
			whereCavo.put(Elementi.ID_ELEMENTO, idCavo);
			recCavo = db.getRecord(new Elementi(), whereCavo);

		}
		if (idTubo != 0) {
			ContentValues whereTubo = new ContentValues();
			whereTubo.put(Elementi.ID_ELEMENTO, idTubo);
			recTubo = db.getRecord(new Elementi(), whereTubo);
		}

		// 2) Inserisco la nuova riga

		if (recTubo != null) {
			double qta = metriTubo * val.getAsDouble(QUANTITA);
			ContentValues whereChk = new ContentValues();
			whereChk.put(ID_PREVENTIVO, val.getAsInteger(ID_PREVENTIVO));
			whereChk.put(ID_ELEMENTO, recTubo.getAsInteger(Elementi.ID_ELEMENTO));
			whereChk.put(TIPO, MATERIALE_PREVENTIVO);
			ContentValues recordCheck = db.getRecord(this, whereChk);
			if (recordCheck != null) {
				ContentValues valUPD = getValoriLogModifica(db);
				ContentValues whereUpd = new ContentValues();
				whereUpd.put(ID_PREVENTIVO_DETTAGLIO, recordCheck.getAsInteger(ID_PREVENTIVO_DETTAGLIO));
				valUPD.put(QUANTITA, recordCheck.getAsDouble(QUANTITA) + qta);
				aggiornaRecord(db, valUPD, whereUpd);
			} else {
				ContentValues valINS = getValoriLogInserimento(db);
				valINS.put(ID_PREVENTIVO, val.getAsInteger(ID_PREVENTIVO));
				valINS.put(TIPO, MATERIALE_PREVENTIVO);
				valINS.put(ID_LOCALE, 0);
				valINS.put(ID_ELEMENTO, recTubo.getAsInteger(Elementi.ID_ELEMENTO));
				valINS.put(UNITA_MISURA, recTubo.getAsString(Elementi.UNITA_MISURA));
				valINS.put(DESCRIZIONE, recTubo.getAsString(Elementi.NOME_ELEMENTO));
				valINS.put(QUANTITA, qta);
				valINS.put(CODICE_IVA, val.getAsString(CODICE_IVA));
				valINS.put(STATO, APERTO);
				inserisciRecord(db, valINS);
			}
		}

		if (recCavo != null) {
			double qta = metriCavo * val.getAsDouble(QUANTITA);
			ContentValues whereChk = new ContentValues();
			whereChk.put(ID_PREVENTIVO, val.getAsInteger(ID_PREVENTIVO));
			whereChk.put(ID_ELEMENTO, recCavo.getAsInteger(Elementi.ID_ELEMENTO));
			whereChk.put(TIPO, MATERIALE_PREVENTIVO);
			ContentValues recordCheck = db.getRecord(this, whereChk);
			if (recordCheck != null) {
				ContentValues valUPD = getValoriLogModifica(db);
				ContentValues whereUpd = new ContentValues();
				whereUpd.put(ID_PREVENTIVO_DETTAGLIO, recordCheck.getAsInteger(ID_PREVENTIVO_DETTAGLIO));
				valUPD.put(QUANTITA, recordCheck.getAsDouble(QUANTITA) + qta);
				aggiornaRecord(db, valUPD, whereUpd);
			} else {
				ContentValues valINS = getValoriLogInserimento(db);
				valINS.put(ID_PREVENTIVO, val.getAsInteger(ID_PREVENTIVO));
				valINS.put(TIPO, MATERIALE_PREVENTIVO);
				valINS.put(ID_LOCALE, 0);
				valINS.put(ID_ELEMENTO, recCavo.getAsInteger(Elementi.ID_ELEMENTO));
				valINS.put(UNITA_MISURA, recCavo.getAsString(Elementi.UNITA_MISURA));
				valINS.put(DESCRIZIONE, recCavo.getAsString(Elementi.NOME_ELEMENTO));
				valINS.put(QUANTITA, qta);
				valINS.put(CODICE_IVA, val.getAsString(CODICE_IVA));
				valINS.put(STATO, APERTO);
				inserisciRecord(db, valINS);
			}

		}
		isAggiornamentoCaviETubi = false;
	}

	public void aggiornaTubiECaviLocale(DbInterno db, int idPreventivo, int idLocale) {
		System.out.println("EConTab: PreventiviDettaglio aggiornaTubiECaviLocale ENTER");
		if (eseguiAggiornamentoTubiECavi){
			isAggiornamentoCaviETubi = true;

			ContentValues wherePrev = new ContentValues();
			wherePrev.put(Preventivi.ID_PREVENTIVO, idPreventivo);
			ContentValues valPrev = db.getRecord(new Preventivi(), wherePrev);
			// solo in caso di preventivo aggiorno i tubi e i cavi del locale
			// nel caso di ordine i tubi e i cavi vengono aggiornati dai collegamenti
			if (valPrev != null && valPrev.getAsString(Preventivi.TIPO).equals(Preventivi.TIPO_PREVENTIVO)) {
				HashMap<Integer, Double> mappaTubi = new HashMap<Integer, Double>();
				HashMap<Integer, Double> mappaCavi = new HashMap<Integer, Double>();

				// 1) Prendo tutti gli elementi/componenti del preventivo
				ElementiCantiere tabElemCant = new ElementiCantiere();
				ComponentiCantiere tabCompCant = new ComponentiCantiere();

				Join j0 = new Join(ElementiCantiere.NOME_TABELLA, ComposizioniCantiere.NOME_TABELLA, Join.LEFT_JOIN);
				j0.addCampiDiJoin(ElementiCantiere.ID_ELEMENTO_CANT, ComposizioniCantiere.ID_ELEMENTO_CANT);

				Join j1 = new Join(ComposizioniCantiere.NOME_TABELLA, ComponentiCantiere.NOME_TABELLA, Join.LEFT_JOIN);
				j1.addCampiDiJoin(ComposizioniCantiere.ID_COMPONENTE_CANT, ComponentiCantiere.ID_COMPONENTE_CANT);

				String SQL = "Select " + tabElemCant.getNomeCampoTabella(ElementiCantiere.ID_ELEMENTO_TUBO) + ","
						+ tabElemCant.getNomeCampoTabella(ElementiCantiere.ID_ELEMENTO_CAVO) + ","
						+ tabElemCant.getNomeCampoTabella(ElementiCantiere.METRI_TUBO_CANT) + ","
						+ tabElemCant.getNomeCampoTabella(ElementiCantiere.METRI_CAVO_CANT) + ","
						+ tabCompCant.getNomeCampoTabella(ComponentiCantiere.ID_ELEMENTO_TUBO) + " as "
						+ tabCompCant.getAlias(ComponentiCantiere.ID_ELEMENTO_TUBO) + ","
						+ tabCompCant.getNomeCampoTabella(ComponentiCantiere.ID_ELEMENTO_CAVO) + " as "
						+ tabCompCant.getAlias(ComponentiCantiere.ID_ELEMENTO_CAVO) + ","
						+ tabCompCant.getNomeCampoTabella(ComponentiCantiere.METRI_TUBO_CANT) + " as "
						+ tabCompCant.getAlias(ComponentiCantiere.METRI_TUBO_CANT) + ","
						+ tabCompCant.getNomeCampoTabella(ComponentiCantiere.METRI_CAVO_CANT) + " as "
						+ tabCompCant.getAlias(ComponentiCantiere.METRI_CAVO_CANT) + ","
						+ tabElemCant.getNomeCampoTabella(ElementiCantiere.NON_CONTEGGIARE_PREVENTIVI) + " as "
						+ tabElemCant.getAlias(ElementiCantiere.NON_CONTEGGIARE_PREVENTIVI) + ","
						+ tabCompCant.getNomeCampoTabella(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI) + " as "
						+ tabCompCant.getAlias(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI) + ","
						+ tabCompCant.getNomeCampoTabella(ComponentiCantiere.NOME_COMPONENTE_CANT) + " from " + ElementiCantiere.NOME_TABELLA
						+ j0.getSQLJoin() + j1.getSQLJoin() + " where ((" + tabElemCant.getNomeCampoTabella(ElementiCantiere.ID_PREVENTIVO) + "="
						+ idPreventivo + " and " + tabCompCant.getNomeCampoTabella(ComponentiCantiere.NOME_COMPONENTE_CANT) + " is null) or ("+ tabCompCant.getNomeCampoTabella(ComponentiCantiere.ID_PREVENTIVO) +"="+idPreventivo+")) and " + tabElemCant.getNomeCampoTabella(ElementiCantiere.ID_LOCALE) + "=" + idLocale;

				ArrayList<Object> elementi = db.eseguiSelect(SQL, null);

				// 2) verifico se � un componente o se � un elemento (E' un elemento se idComponente � null)
				// e prendo l'id del tubo e del cavo e lo metto da parte nella mappa
				for (int i = 0; i < elementi.size(); i++) {
					ContentValues curr = (ContentValues) elementi.get(i);
					int idElementoTubo = 0;
					int idElementoCavo = 0;
					double metriTubo = 0.0D;
					double metriCavo = 0.0D;

					if (curr.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT) == null) {
						idElementoTubo = curr.getAsInteger(ElementiCantiere.ID_ELEMENTO_TUBO);
						idElementoCavo = curr.getAsInteger(ElementiCantiere.ID_ELEMENTO_CAVO);
						metriTubo = curr.getAsDouble(ElementiCantiere.METRI_TUBO_CANT);
						metriCavo = curr.getAsDouble(ElementiCantiere.METRI_CAVO_CANT);
					} else {
						idElementoTubo = curr.getAsInteger(tabCompCant.getAlias(ComponentiCantiere.ID_ELEMENTO_TUBO));
						idElementoCavo = curr.getAsInteger(tabCompCant.getAlias(ComponentiCantiere.ID_ELEMENTO_CAVO));
						metriTubo = curr.getAsDouble(tabCompCant.getAlias(ComponentiCantiere.METRI_TUBO_CANT));
						metriCavo = curr.getAsDouble(tabCompCant.getAlias(ComponentiCantiere.METRI_CAVO_CANT));
					}

					System.out.println("EConTab: PreventiviDettaglio aggiornaTubiECaviLocale idElementoTubo " + idElementoTubo);
					System.out.println("EConTab: PreventiviDettaglio aggiornaTubiECaviLocale idElementoCavo " + idElementoCavo);
					System.out.println("EConTab: PreventiviDettaglio aggiornaTubiECaviLocale metriTubo      " + metriTubo);
					System.out.println("EConTab: PreventiviDettaglio aggiornaTubiECaviLocale metriCavo      " + metriCavo);

					if (idElementoTubo != 0) {
						double newMetritubo = metriTubo;
						if (mappaTubi.containsKey(idElementoTubo)) {

							newMetritubo = metriTubo + mappaTubi.get(idElementoTubo);

						}
						mappaTubi.put(idElementoTubo, newMetritubo);
					}

					if (idElementoCavo != 0) {
						double newMetriCavo = metriCavo;
						if (mappaCavi.containsKey(idElementoCavo)) {
							newMetriCavo = metriCavo + mappaCavi.get(idElementoCavo);
						}
						mappaCavi.put(idElementoCavo, newMetriCavo);
					}
				}

				// 3) Alla fine aggiorno/inserisco le righe di preventivo con i cavi e i tubi (eliminando le righe che non
				// ci
				// sono
				// pi� se per
				// esempio cambia l'id del tubo di un componente)
				Join jElem = new Join(NOME_TABELLA, Elementi.NOME_TABELLA);
				jElem.addCampiDiJoin(ID_ELEMENTO, Elementi.ID_ELEMENTO);

				String SQLTUBI = "Select " + getNomeCampoTabella("*") + " from " + NOME_TABELLA + jElem.getSQLJoin() + " where "
						+ ID_PREVENTIVO + "=" + idPreventivo + " and " + ID_LOCALE + "=" + idLocale + " and " + Elementi.ID_CATEGORIA_GENERALE
						+ "=" + CategorieGenerali.TUBI;

				ArrayList<Object> tubiPrev = db.eseguiSelect(SQLTUBI, null);
				for (int i = 0; i < tubiPrev.size(); i++) {
					ContentValues valTubo = (ContentValues) tubiPrev.get(i);
					ContentValues where = new ContentValues();
					where.put(ID_PREVENTIVO_DETTAGLIO, valTubo.getAsInteger(ID_PREVENTIVO_DETTAGLIO));
					int idTuboCurr = valTubo.getAsInteger(ID_ELEMENTO);
					if (mappaTubi.containsKey(idTuboCurr)) {
						double metriNew = mappaTubi.get(idTuboCurr);
						ContentValues valUPD = getValoriLogModifica(db);
						valUPD.put(QUANTITA, metriNew);
						aggiornaRecord(db, valUPD, where);
						mappaTubi.remove(idTuboCurr);
					} else {
						cancellaRecord(db, where);
					}
				}

				String SQLCAVI = "Select " + getNomeCampoTabella("*") + " from " + NOME_TABELLA + jElem.getSQLJoin() + " where "
						+ ID_PREVENTIVO + "=" + idPreventivo + " and " + ID_LOCALE + "=" + idLocale + " and " + Elementi.ID_CATEGORIA_GENERALE
						+ "=" + CategorieGenerali.CAVI;
				ArrayList<Object> caviPrev = db.eseguiSelect(SQLCAVI, null);
				for (int i = 0; i < caviPrev.size(); i++) {
					ContentValues valCavo = (ContentValues) caviPrev.get(i);
					ContentValues where = new ContentValues();
					where.put(ID_PREVENTIVO_DETTAGLIO, valCavo.getAsInteger(ID_PREVENTIVO_DETTAGLIO));
					int idCavoCurr = valCavo.getAsInteger(ID_ELEMENTO);
					if (mappaCavi.containsKey(idCavoCurr)) {
						double metriNew = mappaCavi.get(idCavoCurr);
						ContentValues valUPD = getValoriLogModifica(db);
						valUPD.put(QUANTITA, metriNew);
						aggiornaRecord(db, valUPD, where);
						mappaCavi.remove(idCavoCurr);
					} else {
						cancellaRecord(db, where);
					}
				}

				HashMap<Integer, Double> mappaDaInserire = new HashMap<Integer, Double>();
				mappaDaInserire.putAll(mappaCavi);
				mappaDaInserire.putAll(mappaTubi);
				String elementiDaInserire = "";
				// Prendo i dati degli elementi cavi e tubi da inserire
				Iterator<Integer> iter = mappaDaInserire.keySet().iterator();
				while (iter.hasNext()) {
					String currEl = "" + iter.next();
					elementiDaInserire = elementiDaInserire + currEl + ",";
				}

				if (elementiDaInserire.length() > 0) {
					// 1) Prendo l'iva del cliente del preventivo
					Preventivi tabPrev = new Preventivi();
					ContentValues recCliente = tabPrev.getClientePreventivo(db, idPreventivo);
					String codiceIVA = "";
					if (recCliente != null) {
						codiceIVA = recCliente.getAsString(Anagrafica.CODICE_IVA);
					}
					elementiDaInserire = elementiDaInserire.substring(0, elementiDaInserire.length() - 1);
					elementiDaInserire = " (" + elementiDaInserire + ")";
					String SQLELEM = "Select * from " + Elementi.NOME_TABELLA + " where " + Elementi.ID_ELEMENTO + " in " + elementiDaInserire;
					ArrayList<Object> recs = db.eseguiSelect(SQLELEM, null);
					// aggiungo i tubi nuovi e i cavi nuovi (rimasti nella mappa)
					for (int i = 0; i < recs.size(); i++) {
						ContentValues valCurr = (ContentValues) recs.get(i);

						// 2) Inserisco la nuova riga
						ContentValues valINS = getValoriLogInserimento(db);
						valINS.put(ID_PREVENTIVO, idPreventivo);
						if (idLocale != 0) {
							valINS.put(TIPO, MATERIALE);
						} else {
							valINS.put(TIPO, MATERIALE_PREVENTIVO);
						}

						valINS.put(ID_LOCALE, idLocale);

						valINS.put(ID_ELEMENTO, valCurr.getAsInteger(Elementi.ID_ELEMENTO));
						valINS.put(UNITA_MISURA, valCurr.getAsString(Elementi.UNITA_MISURA));
						valINS.put(DESCRIZIONE, valCurr.getAsString(Elementi.NOME_ELEMENTO));

						valINS.put(QUANTITA, mappaDaInserire.get(valCurr.getAsInteger(Elementi.ID_ELEMENTO)));
						valINS.put(CODICE_IVA, codiceIVA);
						valINS.put(STATO, APERTO);

						inserisciRecord(db, valINS);
					}

				}
			}
			isAggiornamentoCaviETubi = false;
		}
		System.out.println("EConTab: PreventiviDettaglio aggiornaTubiECaviLocale EXIT");
	}

	/**
	 * Aggiunge 1 unit� alla riga dell'elemento o inserisce una nuova riga
	 * 
	 * @param db
	 * @param idPreventivo
	 * @param valori
	 * @return eventuale errore
	 */
/*	public String aggiungiAPreventivo(DbInterno db, int idPreventivo, ContentValues valori) {
		// TODO Auto-generated method stub
		return aggiornaRigaPreventivo(db, idPreventivo, valori, 1, MATERIALE, false);
	}*/

	/**
	 * Aggiunge 1 unit� alla riga dell'elemento o inserisce una nuova riga
	 * 
	 * @param db
	 * @param idPreventivo
	 * @param valori
	 * @param componente
	 *            componente o elemento
	 * @return eventuale errore
	 */
	public String aggiungiAPreventivo(DbInterno db, int idPreventivo, ContentValues valori, boolean componente) {
		// Se sono in un componente verifico che non sia composto: se � composto allora inserisco i suoi componenti nel
		// preventivo al posto del componente
		ComponentiCantComposti tabCompCantComposti = new ComponentiCantComposti();
		if (componente == true) {
			int idComponentePadre = valori.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT);
			ArrayList<Object> componenti = tabCompCantComposti.getComposizioneComponente(db, idComponentePadre);
			if (componenti.size() == 0) {
				//return aggiornaRigaPreventivo(db, idPreventivo, valori, 1, MATERIALE, componente);
				return "";
			} else {
				String result = "";
				for (int i = 0; i < componenti.size(); i++) {
					/*int idCompFiglio = ((ContentValues) componenti.get(i)).getAsInteger(ComponentiCantComposti.ID_COMPONENTE_CANT_FIGLIO);
					ContentValues where = new ContentValues();
					where.put(ComponentiCantiere.ID_COMPONENTE_CANT, idCompFiglio);
					ContentValues valCurr = db.getRecord(new ComponentiCantiere(), where);
					if (valCurr != null) {
						valCurr.put(ElementiCantiere.ID_LOCALE, valori.getAsInteger(ElementiCantiere.ID_LOCALE));
						result = aggiornaRigaPreventivo(db, idPreventivo, valCurr, 1, MATERIALE, componente);
					}*/

				}
				//aggiornaTubiECaviLocale(db, idPreventivo, valori.getAsInteger(ElementiCantiere.ID_LOCALE));
				return result;
			}
		} else {
			//return aggiornaRigaPreventivo(db, idPreventivo, valori, 1, MATERIALE, componente);
			return "";
		}

	}

	/**
	 * Toglie 1 unit� alla riga dell'elemento o elimina la riga se <=0
	 * 
	 * @param db
	 * @param idPreventivo
	 * @param valori
	 * @return eventuale errore
	 */
	public String togliDaPreventivo(DbInterno db, int idPreventivo, ContentValues valori) {
		// TODO Auto-generated method stub
		//return aggiornaRigaPreventivo(db, idPreventivo, valori, -1, MATERIALE, false);
		return "";
	}

	/**
	 * Toglie 1 unit� alla riga dell'elemento o elimina la riga se <=0
	 * 
	 * @param db
	 * @param idPreventivo
	 * @param valori
	 * @param componente
	 *            componente o elemento
	 * @return eventuale errore
	 */
	public String togliDaPreventivo(DbInterno db, int idPreventivo, ContentValues valori, boolean componente) {
		// TODO Auto-generated method stub
		ComponentiCantComposti tabCompCantComposti = new ComponentiCantComposti();
		if (componente == true) {
			int idComponentePadre = valori.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT);
			ArrayList<Object> componenti = tabCompCantComposti.getComposizioneComponente(db, idComponentePadre);
			if (componenti.size() == 0) {
				//return aggiornaRigaPreventivo(db, idPreventivo, valori, -1, MATERIALE, componente);
				return "";
			} else {
				// String result = aggiornaRigaPreventivo(db, idPreventivo, valori, -1, MATERIALE, componente);

				/*
				 * for (int i = 0; i < componenti.size(); i++) { int idCompFiglio = ((ContentValues)
				 * componenti.get(i)).getAsInteger(ComponentiCantComposti.ID_COMPONENTE_CANT_FIGLIO); ContentValues
				 * where = new ContentValues(); where.put(ComponentiCantiere.ID_COMPONENTE_CANT, idCompFiglio);
				 * ContentValues valCurr = db.getRecord(new ComponentiCantiere(), where); if (valCurr != null) {
				 * valCurr.put(ElementiCantiere.ID_LOCALE, valori.getAsInteger(ElementiCantiere.ID_LOCALE)); result =
				 * aggiornaRigaPreventivo(db, idPreventivo, valCurr, -1, MATERIALE, componente); }
				 * 
				 * }
				 */
				//aggiornaTubiECaviLocale(db, idPreventivo, valori.getAsInteger(ElementiCantiere.ID_LOCALE));
				return "";
			}
		} else {
			//return aggiornaRigaPreventivo(db, idPreventivo, valori, -1, MATERIALE, componente);
			return "";
		}
	}

	/**
	 * Modifica o inserisce la riga:<br>
	 * Se qta >o inserisce o aggiunge qta alla riga<br>
	 * Se qta <0 sottrae o elimina la riga<br>
	 * se componente >=true � un componente altrimenti � un elemento<br>
	 * se tipo riga e Manopera allora componente � ininfluente
	 * 
	 * @param db
	 * @param idPreventivo
	 * @param valori
	 * @param qta
	 * @param tipoRiga
	 * @param componente
	 * @return eventuale errore
	 */
	/*public String aggiornaRigaPreventivo(DbInterno db, int idPreventivo, ContentValues valori, double qta, String tipoRiga,
			boolean componente) {
		return aggiornaRigaPreventivo(db,idPreventivo,valori,qta,tipoRiga,componente,false);
	}*/



    /**
     * Modifica o inserisce la riga:<br>
     * Se qta >o inserisce o aggiunge qta alla riga<br>
     * Se qta <0 sottrae o elimina la riga<br>
     * se componente >=true � un componente altrimenti � un elemento<br>
     * se tipo riga e Manopera allora componente � ininfluente
     *
     * @param db
     * @param idPreventivo
     * @param valori
     * @param qta
     * @param tipoRiga
     * @param componente
     * @return eventuale errore
     */
   /* public String aggiornaRigaPreventivo(DbInterno db, int idPreventivo, ContentValues valori, double qta, String tipoRiga,
                                         boolean componente,boolean permettiQtaZero) {
        // TODO Auto-generated method stub
        // 1) Cerco se c'� gi� la riga nel preventivo

        ContentValues where = new ContentValues();

        ContentValues recordCheck = null;

        // Verifico se sono in inserimento o in aggiornamento
        if (valori.containsKey(ID_PREVENTIVO_DETTAGLIO)) {
            // aggiornamento
            where.put(ID_PREVENTIVO_DETTAGLIO, valori.getAsInteger(ID_PREVENTIVO_DETTAGLIO));
            recordCheck = db.getRecord(this, where);
        } else {
            // inserimento
            where.put(ID_PREVENTIVO, idPreventivo);
            where.put(TIPO, tipoRiga);
            if (valori.containsKey(ElementiCantiere.ID_LOCALE)) {
                where.put(ID_LOCALE, valori.getAsInteger(ElementiCantiere.ID_LOCALE));
            } else {
                where.put(ID_LOCALE, 0);
            }
            if (tipoRiga.equals(MATERIALE) || tipoRiga.equals(MATERIALE_PREVENTIVO)) {

                if (componente) {
                    where.put(ID_COMPONENTE, valori.getAsInteger(ComponentiCantiere.ID_COMPONENTE));

                } else {
                    where.put(ID_ELEMENTO, valori.getAsInteger(ElementiCantiere.ID_ELEMENTO));
                }

            }

            if (tipoRiga.equals(PLACCHE) || tipoRiga.equals(PLACCHE_PREVENTIVO)) {
                where.put(ID_PLACCA_MODULI, valori.getAsInteger(ID_PLACCA_MODULI));
            }

            recordCheck = db.getRecord(this, where);
            // per la manodopera inserisco sempre una nuova riga
            if (tipoRiga.equals(MANOPERA) || tipoRiga.equals(ALTRO)) {
                recordCheck = null;
            }
        }

        if (recordCheck == null && (qta > 0||permettiQtaZero)) {
            // inserisco nuova riga
            // 1) Prendo l'iva del cliente del preventivo
            Preventivi tabPrev = new Preventivi();
            ContentValues recCliente = tabPrev.getClientePreventivo(db, idPreventivo);
            String codiceIVA = "";
            if (recCliente != null) {
                codiceIVA = recCliente.getAsString(Anagrafica.CODICE_IVA);
            }

            // 2) Inserisco la nuova riga
            ContentValues valINS = getValoriLogInserimento(db);
            valINS.put(ID_PREVENTIVO, idPreventivo);
            valINS.put(TIPO, tipoRiga);

            if (valori.containsKey(ElementiCantiere.ID_LOCALE)) {
                valINS.put(ID_LOCALE, valori.getAsInteger(ElementiCantiere.ID_LOCALE));
            }
            if (tipoRiga.equals(MATERIALE) || tipoRiga.equals(MATERIALE_PREVENTIVO)) {
                if (componente) {

                    if (tipoRiga.equals(MATERIALE)) {
                        valINS.put(ID_COMPONENTE, valori.getAsInteger(ComponentiCantiere.ID_COMPONENTE));
                        // valINS.put(CODICE_ARTICOLO, valori.getAsString(CODICE_ARTICOLO));
                        valINS.put(UNITA_MISURA, valori.getAsString(ComponentiCantiere.UNITA_MISURA));
                        valINS.put(DESCRIZIONE, valori.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
                    } else {
                        valINS.put(ID_COMPONENTE, valori.getAsInteger(Componenti.ID_COMPONENTE));
                        // valINS.put(CODICE_ARTICOLO, valori.getAsString(CODICE_ARTICOLO));
                        valINS.put(UNITA_MISURA, valori.getAsString(Componenti.UNITA_MISURA));
                        valINS.put(DESCRIZIONE, valori.getAsString(Componenti.NOME_COMPONENTE));
                    }

                } else {
                    valINS.put(ID_ELEMENTO, valori.getAsInteger(ElementiCantiere.ID_ELEMENTO));
                    valINS.put(UNITA_MISURA, valori.getAsString(ElementiCantiere.UNITA_MISURA));
                    valINS.put(DESCRIZIONE, valori.getAsString(Elementi.NOME_ELEMENTO));

                }

            }

            if (tipoRiga.equals(PLACCHE) || tipoRiga.equals(PLACCHE_PREVENTIVO)) {
                valINS.put(DESCRIZIONE, valori.getAsString(DESCRIZIONE));
                valINS.put(ID_PLACCA_MODULI, valori.getAsInteger(ID_PLACCA_MODULI));
                valINS.put(UNITA_MISURA, "PZ");
            }

            if (tipoRiga.equals(MANOPERA)) {
                valINS.put(ID_MANODOPERA, valori.getAsInteger(Manodopera.ID_MANODOPERA));
                valINS.put(NUM_OPERATORI, valori.getAsInteger(Manodopera.NUM_OPERATORI_DEFAULT));
                valINS.put(DESCRIZIONE, valori.getAsString(Manodopera.NOME));
                valINS.put(PREZZO, valori.getAsFloat(Manodopera.COSTO_ORARIO));
                String udmMano = valori.getAsString(Manodopera.UNITA_MISURA);
                valINS.put(UNITA_MISURA, udmMano != null && !udmMano.trim().isEmpty() ? udmMano : Manodopera.UNITA_MISURA_DEFAULT);
            }

            if (tipoRiga.equals(ALTRO)) {
                valINS.put(NOTE, valori.getAsString(PreventiviDettaglio.NOTE));
                valINS.put(PREZZO, valori.getAsFloat(PreventiviDettaglio.PREZZO));
            }

            valINS.put(QUANTITA, qta);
            valINS.put(CODICE_IVA, codiceIVA);
            valINS.put(STATO, APERTO);

            inserisciRecord(db, valINS);

        } else {
            // aggiorno la riga trovata
            if (valori.containsKey(ID_PREVENTIVO_DETTAGLIO)) {
                // Se sono in aggiornamento allora aggiorno tutti i valori
                ContentValues valUpd = getValoriLogModifica(db);

                for (int i = 0; i < getNomiCampi().size(); i++) {
                    String nomeCampo = getNomiCampi().get(i);
                    if (valori.containsKey(nomeCampo) && !isChiave(nomeCampo)) {
                        if (getTipoCampo(nomeCampo).equals(INTEGER)) {
                            valUpd.put(nomeCampo, valori.getAsInteger(nomeCampo));
                        }
                        if (getTipoCampo(nomeCampo).equals(TEXT)) {
                            valUpd.put(nomeCampo, valori.getAsString(nomeCampo));
                        }
                        if (getTipoCampo(nomeCampo).equals(NUMERIC)) {
                            valUpd.put(nomeCampo, valori.getAsFloat(nomeCampo));
                        }
                    }
                }
                ContentValues whereUpd = new ContentValues();
                whereUpd.put(ID_PREVENTIVO_DETTAGLIO, valori.getAsInteger(ID_PREVENTIVO_DETTAGLIO));
                aggiornaRecord(db, valUpd, whereUpd);

            } else {
                // se sono in inserimento allora aggiorno solo la qta
                if (recordCheck != null) {
                    double old_qta = recordCheck.getAsDouble(QUANTITA);
                    double new_qta = old_qta + qta;
                    ContentValues whereUPD = new ContentValues();
                    whereUPD.put(ID_PREVENTIVO_DETTAGLIO, recordCheck.getAsInteger(ID_PREVENTIVO_DETTAGLIO));
                    if (new_qta > 0) {
                        // aggiorno la riga
                        ContentValues valUPD = getValoriLogModifica(db);
                        valUPD.put(QUANTITA, new_qta);
                        setQuantitaAggiunta(qta);
                        aggiornaRecord(db, valUPD, whereUPD);

                    } else {
                        // cancello la riga
                        cancellaRecord(db, whereUPD);
                    }
                }
            }

        }

        return "";
    }*/

	/**
	 * Restituisce le righe del preventivo passato (con descrizione anagrafica e cantiere) filtrate per tipo
	 *
	 * @param db
	 * @param idPreventivo
	 * @param tipo
	 *            se null prende tutte le righe
	 * @return
	 */
	public ArrayList<Object> getRighePreventivo(DbInterno db, int idPreventivo, String tipo,boolean ordinatoPerLocaleAsc){
		Join j0 = new Join(NOME_TABELLA, Preventivi.NOME_TABELLA);
		j0.addCampiDiJoin(ID_PREVENTIVO, Preventivi.ID_PREVENTIVO);

		Join j1 = new Join(NOME_TABELLA, Componenti.NOME_TABELLA, Join.LEFT_JOIN);
		j1.addCampiDiJoin(ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j2 = new Join(NOME_TABELLA, Elementi.NOME_TABELLA, Join.LEFT_JOIN);
		j2.addCampiDiJoin(ID_ELEMENTO, Elementi.ID_ELEMENTO);

		Join j3 = new Join(NOME_TABELLA, Locali.NOME_TABELLA, Join.LEFT_JOIN);
		j3.addCampiDiJoin(ID_LOCALE, Locali.ID_LOCALE);

		Join j4 = new Join(Locali.NOME_TABELLA, Aree.NOME_TABELLA, Join.LEFT_JOIN);
		j4.addCampiDiJoin(Locali.ID_AREA, Aree.ID_AREA);

		/*
		 * Join j1 = new Join(Preventivi.NOME_TABELLA,Cantieri.NOME_TABELLA); j1.addCampiDiJoin(Preventivi.ID_CANTIERE,
		 * Cantieri.ID_CANTIERE);
		 *
		 * Join j2 = new Join(Cantieri.NOME_TABELLA,Anagrafica.NOME_TABELLA); j2.addCampiDiJoin(Cantieri.ID_ANAGRAFICA,
		 * Anagrafica.ID_ANAGRAFICA);
		 *
		 * Cantieri tabCant = new Cantieri();
		 */
		Locali tabLocali = new Locali();
		PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();
		String filtroTipo = "";
		if (tipo != null) {
			filtroTipo = " and " + getNomeCampoTabella(TIPO) + "='" + tipo + "'";
		}

		String SQL = "Select " + getNomeCampoTabella("*") + "," + Preventivi.NUMERO + "," + Preventivi.ANNO + "," + Preventivi.ID_CANTIERE
				+ " from " + NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + j2.getSQLJoin() + j3.getSQLJoin() + j4.getSQLJoin()
				+ " where " + getNomeCampoTabella(ID_PREVENTIVO) + "=" + idPreventivo + filtroTipo;

		if (tipo != null && tipo.equals(MATERIALE)) {
			if (ordinatoPerLocaleAsc){
				SQL = SQL + " order by " + Aree.ID_UNITA + " desc," + tabLocali.getNomeCampoTabella(Locali.ID_AREA) + " desc,"
						+ tabLocali.getNomeCampoTabella(Locali.NOME) + "," + tabPrevDett.getNomeCampoTabella(PreventiviDettaglio.ID_LOCALE) + " desc," + CategorieGenerali.ID_CATEGORIA_GENERALE
						+ "," + CategorieComponenti.ID_CATEGORIA_COMPONENTE + "," + DESCRIZIONE;
			}
			else {
				SQL = SQL + " order by " + Aree.ID_UNITA + " desc," + tabLocali.getNomeCampoTabella(Locali.ID_AREA) + " desc,"
						+ tabPrevDett.getNomeCampoTabella(PreventiviDettaglio.ID_LOCALE) + " desc," + CategorieGenerali.ID_CATEGORIA_GENERALE
						+ "," + CategorieComponenti.ID_CATEGORIA_COMPONENTE + "," + DESCRIZIONE;
			}

		}

		ArrayList<Object> righe = db.eseguiSelect(SQL, null);

		if (tipo != null && (tipo.equals(MATERIALE) || tipo.equals(PLACCHE))) {
			Locali tabLoc = new Locali();
			// Aggiungo informazioni sulla linea e sul locale
			HashMap<Integer, ContentValues> mappaLocali = new HashMap<Integer, ContentValues>();
			for (int i = 0; i < righe.size(); i++) {
				ContentValues valCurr = (ContentValues) righe.get(i);
				int idLocale = valCurr.getAsInteger(PreventiviDettaglio.ID_LOCALE);
				int idlineaRiga = 0;
				if (valCurr.containsKey(PreventiviDettaglio.ID_LINEA)&& valCurr.get(PreventiviDettaglio.ID_LINEA)!=null && !valCurr.getAsString(PreventiviDettaglio.ID_LINEA).equals("null")){
					idlineaRiga = valCurr.getAsInteger(PreventiviDettaglio.ID_LINEA);
				}
				if (mappaLocali.containsKey(idLocale)) {
					ContentValues valLoc = mappaLocali.get(idLocale);
					//modifica del 03/01/2018: prendo la linea dalla riga preventivo se diversa da quella del locale
					if (idlineaRiga!=0 &&  valLoc.getAsInteger(Linee.ID_LINEA)!=idlineaRiga){
						Linee tabLinea = new Linee();
						ContentValues valLocLinea = new ContentValues();
						valLocLinea.putAll(valLoc);
						ContentValues vallinea = tabLinea.getLinea(db,idlineaRiga);

						valLocLinea.putAll(vallinea);
						valCurr.putAll(valLocLinea);
					}
					else {
						valCurr.putAll(valLoc);
					}

				} else {
					ContentValues valLoc = new ContentValues();
					ContentValues lineaLocale = tabLoc.getLineaLocale(db, idLocale);

					if (lineaLocale!=null){
						valLoc.putAll(lineaLocale);
						ContentValues where = new ContentValues();
						where.put(Locali.ID_LOCALE, idLocale);
						ContentValues recordLocale = db.getRecord(tabLoc, where);
						if (recordLocale != null) {
							valLoc.put(Locali.NOME, recordLocale.getAsString(Locali.NOME));
						} else {
							valLoc.put(Locali.NOME, "");
						}

						mappaLocali.put(idLocale, valLoc);

						//modifica del 03/01/2018: prendo la linea dalla riga preventivo se diversa da quella del locale
						if (idlineaRiga!=0 &&  valLoc.getAsInteger(Linee.ID_LINEA)!=idlineaRiga){
							Linee tabLinea = new Linee();
							ContentValues valLocLinea = new ContentValues();
							valLocLinea.putAll(valLoc);
							ContentValues vallinea = tabLinea.getLinea(db,idlineaRiga);

							valLocLinea.putAll(vallinea);
							valCurr.putAll(valLocLinea);
						}
						else {
							valCurr.putAll(valLoc);
						}


					}
				}

			}
		}

		if (tipo != null && (tipo.equals(MATERIALE_PREVENTIVO) || tipo.equals(PLACCHE_PREVENTIVO))) {
			if (righe.size() > 0) {
				ContentValues valCurr = (ContentValues) righe.get(0);
				int idCantiere = valCurr.getAsInteger(Preventivi.ID_CANTIERE);
				Cantieri tabCant = new Cantieri();
				ContentValues valLineaCant = tabCant.getLineaCantiere(db, idCantiere);

				for (int i = 0; i < righe.size(); i++) {
					valCurr = (ContentValues) righe.get(i);
					valCurr.putAll(valLineaCant);
				}
			}

		}

		return righe;
	}

	/**
	 * Restituisce le righe del preventivo passato (con descrizione anagrafica e cantiere) filtrate per tipo
	 * 
	 * @param db
	 * @param idPreventivo
	 * @param tipo
	 *            se null prende tutte le righe
	 * @return
	 */
	public ArrayList<Object> getRighePreventivo(DbInterno db, int idPreventivo, String tipo) {
		return getRighePreventivo(db,idPreventivo,tipo,false);
	}

	public double getImportoRiga(ContentValues val) {
		double qta = val.getAsDouble(PreventiviDettaglio.QUANTITA);
		double prezzo = val.getAsDouble(PreventiviDettaglio.PREZZO);
		String tipo = val.getAsString(PreventiviDettaglio.TIPO);
		int numeroOperatori = 1;
		if (tipo.equals(PreventiviDettaglio.MANOPERA)) {
			numeroOperatori = val.getAsInteger(PreventiviDettaglio.NUM_OPERATORI);
			if (numeroOperatori == 0) {
				numeroOperatori = 1;
			}
		}
		double importo = qta * prezzo * numeroOperatori;

		return Utility.arrotonda(importo, 2);
	}

	/**
	 * Per ogni elemento gestito a linea verifico: <br>
	 * 1) Se esiste un codice associato <br>
	 * 2) Se il codice associato ha la stessa linea del locale (area,unit�, cantiere) <br>
	 * 3) Se no cancello l'associazione e imposto la nuova associazione per la nuova linea se trovo un codice
	 * corrispondente
	 * 
	 * @param db
	 * @param idPreventivo
	 * @param idLocale
	 */
	public void associazioneAutomaticaCodiciLocale(DbInterno db, int idPreventivo, int idLocale) {
		associazioneAutomaticaCodiciLinea(db, idPreventivo, 0, 0, 0, idLocale);
	}

	/**
	 * Per ogni elemento gestito a linea verifico: <br>
	 * 1) Se esiste un codice associato <br>
	 * 2) Se il codice associato ha la stessa linea del locale (area,unit�, cantiere) <br>
	 * 3) Se no cancello l'associazione e imposto la nuova associazione per la nuova linea se trovo un codice
	 * corrispondente
	 * 
	 * @param db
	 * @param idPreventivo
	 * @param idArea
	 */
	public void associazioneAutomaticaCodiciArea(DbInterno db, int idPreventivo, int idArea) {
		associazioneAutomaticaCodiciLinea(db, idPreventivo, 0, 0, idArea, 0);
	}

	/**
	 * Per ogni elemento gestito a linea verifico: <br>
	 * 1) Se esiste un codice associato <br>
	 * 2) Se il codice associato ha la stessa linea del locale (area,unit�, cantiere) <br>
	 * 3) Se no cancello l'associazione e imposto la nuova associazione per la nuova linea se trovo un codice
	 * corrispondente
	 * 
	 * @param db
	 * @param idPreventivo
	 * @param idUnita
	 */
	public void associazioneAutomaticaCodiciUnita(DbInterno db, int idPreventivo, int idUnita) {
		associazioneAutomaticaCodiciLinea(db, idPreventivo, 0, idUnita, 0, 0);
	}

	/**
	 * Per ogni elemento gestito a linea verifico: <br>
	 * 1) Se esiste un codice associato <br>
	 * 2) Se il codice associato ha la stessa linea del locale (area,unit�, cantiere) <br>
	 * 3) Se no cancello l'associazione e imposto la nuova associazione per la nuova linea se trovo un codice
	 * corrispondente
	 * 
	 * @param db
	 * @param idPreventivo
	 * @param idCantiere
	 */
	public void associazioneAutomaticaCodiciCantiere(DbInterno db, int idPreventivo, int idCantiere) {
		associazioneAutomaticaCodiciLinea(db, idPreventivo, idCantiere, 0, 0, 0);
	}

	private void associazioneAutomaticaCodiciLinea(DbInterno db, int idPreventivo, int idCantiere, int idUnita, int idArea, int idLocale) {
		associazioneAutomaticaCodici(db, idPreventivo, idCantiere, idUnita, idArea, idLocale, true);
	}

	/**
	 * Per ogni elemento gestito a linea verifico: <br>
	 * 1) Se esiste un codice associato <br>
	 * 2) Se il codice associato ha la stessa linea del locale (area,unit�, cantiere) <br>
	 * 3) Se no cancello l'associazione e imposto la nuova associazione per la nuova linea se trovo un codice
	 * corrispondente
	 * 
	 * @param db
	 * @param idPreventivo
	 * @param idCantiere
	 * @param idUnita
	 * @param idArea
	 * @param idLocale
	 * @param soloLinea
	 *            se false imposto anche i codici non gestiti a linea
	 */
	private void associazioneAutomaticaCodici(DbInterno db, int idPreventivo, int idCantiere, int idUnita, int idArea, int idLocale,
			boolean soloLinea) {
		ArrayList<Integer> locali = new ArrayList<Integer>();
		if (idCantiere != 0) {
			Locali tabLocali = new Locali();
			ArrayList<Object> recLocali = tabLocali.getLocaliCantiere(db, idCantiere);
			for (int i = 0; i < recLocali.size(); i++) {
				locali.add(((ContentValues) recLocali.get(i)).getAsInteger(Locali.ID_LOCALE));
			}
			locali.add(0);// per i componenti aggiunti solo sul preventivo e non sulle stanze
		}

		if (idUnita != 0) {
			Locali tabLocali = new Locali();
			ArrayList<Object> recLocali = tabLocali.getLocaliUnita(db, idUnita);
			for (int i = 0; i < recLocali.size(); i++) {
				locali.add(((ContentValues) recLocali.get(i)).getAsInteger(Locali.ID_LOCALE));
			}
		}

		if (idArea != 0) {
			Locali tabLocali = new Locali();
			ArrayList<Object> recLocali = tabLocali.getLocaliArea(db, idArea);
			for (int i = 0; i < recLocali.size(); i++) {
				locali.add(((ContentValues) recLocali.get(i)).getAsInteger(Locali.ID_LOCALE));
			}
		}

		if (idLocale != 0) {
			locali.add(idLocale);
		}

		ArrayList<Object> righeMaterialiLocali = getRighePreventivo(db, idPreventivo, MATERIALE);
		ArrayList<Object> righeMaterialiPreventivo = getRighePreventivo(db, idPreventivo, MATERIALE_PREVENTIVO);
		ArrayList<Object> righeMaterialiPlacche = getRighePreventivo(db, idPreventivo, PLACCHE);
		ArrayList<Object> righeMaterialiPlacchePreventivo = getRighePreventivo(db, idPreventivo, PLACCHE_PREVENTIVO);
        ArrayList<Object> righeCollegamenti = getRighePreventivo(db, idPreventivo, COLLEGAMENTI);
       // ArrayList<Object> righeMaterialiPlacchePreventivo = getRighePreventivo(db, idPreventivo, PLACCHE_PREVENTIVO);

		ArrayList<Object> righeMateriali = new ArrayList<Object>();
		righeMateriali.addAll(righeMaterialiLocali);
		righeMateriali.addAll(righeMaterialiPreventivo);
		righeMateriali.addAll(righeMaterialiPlacche);
		righeMateriali.addAll(righeMaterialiPlacchePreventivo);
        righeMateriali.addAll(righeCollegamenti);

		Listini tabListini = new Listini();
		boolean gestitoALinea = false;
		Componenti tabComponenti = new Componenti();
		for (int i = 0; i < righeMateriali.size(); i++) {
			ContentValues valCurr = (ContentValues) righeMateriali.get(i);
			int idElemento = valCurr.getAsInteger(ID_ELEMENTO);
			int idComponente = valCurr.getAsInteger(ID_COMPONENTE);
			int idPlaccaModuli = valCurr.getAsInteger(ID_PLACCA_MODULI);
			int idLocaleCurr = valCurr.getAsInteger(ID_LOCALE);

			if ((idElemento != 0 || idComponente != 0 || idPlaccaModuli != 0) && locali.contains(idLocaleCurr)) {
				gestitoALinea = false;
				if (idComponente != 0) {
					ContentValues whereComp = new ContentValues();
					whereComp.put(Componenti.ID_COMPONENTE, idComponente);
					ContentValues valComp = db.getRecord(tabComponenti, whereComp);
					if (valComp != null && valComp.getAsInteger(Componenti.LINEA_SN) == 1) {
						gestitoALinea = true;
					}
				}
				if (idPlaccaModuli != 0) {
					gestitoALinea = true;
				}
				String codiceCorrente = valCurr.getAsString(CODICE_ARTICOLO);
				// procedo solo se il componente � gestito a Linea
				if (gestitoALinea) {
					int idLineaLocale = valCurr.getAsInteger(Linee.ID_LINEA);

					if (idLineaLocale != 0) {
						// cambio il codice solo se il locale ha la linea altrimenti tengo il codice impostato perch�
						// potrei averle inserite a mano
						System.out.println("EConTab: PreventivoDettaglio associazioneAutomaticaCodici codiceCorrente " + codiceCorrente);
						if (codiceCorrente == null || codiceCorrente.equals("")) {
							// associo direttamente il codice se esiste
							_cambiaCodice(valCurr, idLineaLocale, db);
						} else {
							// verifico che la linea del codice associato corrisponda a quella del locale
							// se diversa sostituisco il codice se ne esiste uno per quel componente
							ContentValues whereLis = new ContentValues();
							whereLis.put(Listini.CODICE_ARTICOLO, codiceCorrente);
							ContentValues valLis = db.getRecord(tabListini, whereLis);
							if (valLis != null) {
								int idLineaListino = valLis.getAsInteger(Listini.ID_LINEA);
								if (idLineaListino != idLineaLocale) {
									//15/01/2018 - verifico che non esista in tabella Ass_codici_linee: se esiste non faccio nulla altrimenti cambio codice
									ContentValues whereAss = new ContentValues();
									whereAss.put(AssCodiciLinee.CODICE_ARTICOLO,codiceCorrente);
									whereAss.put(AssCodiciLinee.ID_LINEA,idLineaLocale);
									AssCodiciLinee tabAss = new AssCodiciLinee();
									ContentValues recordChk = db.getRecord(tabAss,whereAss);
									if (recordChk==null){
										System.out.println("EConTab: PreventivoDettaglio associazioneAutomaticaCodici recordChk NULL");
										// cerco un codice con stessa linea e stesso id_componente
										_cambiaCodice(valCurr, idLineaLocale, db);
									}
									else
									{
										System.out.println("EConTab: PreventivoDettaglio associazioneAutomaticaCodici recordChk OK");
									}
								}
							}
						}
					}
				} else {
					if (soloLinea == false && (codiceCorrente == null || codiceCorrente.equals(""))) {
						// prendo l'ultimo codice associato all'elemento non gestito a linea
						_cambiaCodiceNoLinea(valCurr, db);
					}
				}
			}
		}
	}

	/**
	 * Effettua l'aggiornamento del codice sulla riga di preventivo
	 * 
	 * @param valCurr
	 * @param idLineaLocale
	 * @param db
	 */
	private void _cambiaCodice(ContentValues valCurr, int idLineaLocale, DbInterno db) {
		// TODO Auto-generated method stub
		System.out.println("EConTab: PreventivoDettaglio _cambiaCodice ENTER");
		Join j0 = new Join(ElementiCodici.NOME_TABELLA, Listini.NOME_TABELLA);
		j0.addCampiDiJoin(ElementiCodici.CODICE_ARTICOLO, Listini.CODICE_ARTICOLO);

		String SQL = "Select " + Listini.NOME_TABELLA + ".* from " + ElementiCodici.NOME_TABELLA + j0.getSQLJoin() + " where "
				+ ElementiCodici.ID_ELEMENTO + "=0 and " + ElementiCodici.ID_COMPONENTE + "=" + valCurr.getAsInteger(ID_COMPONENTE)
				+ " and " + ElementiCodici.ID_PLACCA_MODULI + "=" + valCurr.getAsInteger(ID_PLACCA_MODULI) + " and " + Listini.ID_LINEA
				+ "=" + idLineaLocale;

		ArrayList<Object> codici = db.eseguiSelect(SQL, null);
		ContentValues valLis = null;
		if (codici.size() > 0) {
			// prendo il primo codice che trovo
			valLis = (ContentValues) codici.get(0);
		}

		//se non trovo codice nella tabella ElementiCodici cerco nella tabella Ass_codici_linee
		if (valLis==null){
			String SQLASS = "Select listini.* from elementi_codici inner join ass_codici_linee on elementi_codici.codice_articolo=ass_codici_linee.codice_articolo and ass_codici_linee.id_linea="+idLineaLocale+" inner join listini on elementi_codici.codice_articolo=listini.codice_articolo where elementi_codici.id_elemento=0 and elementi_codici.id_componente="+valCurr.getAsInteger(ID_COMPONENTE)+" and elementi_codici.id_placca_moduli="+valCurr.getAsInteger(ID_PLACCA_MODULI);
			ArrayList<Object> codiciAss = db.eseguiSelect(SQLASS, null);
			if (codiciAss.size() > 0) {
				// prendo il primo codice che trovo
				valLis = (ContentValues) codiciAss.get(0);
			}
		}


		ContentValues valUPd = getValoriLogModifica(db);
		if (valLis != null) {

			// calcolo il prezzo di vendita da prezzo listino con sconto
			double przLis = valLis.getAsDouble(Listini.PRZ_LISTINO);
			double sconto = valLis.getAsDouble(Listini.SCONTO);
			double przVen = Utility.arrotonda(przLis - (przLis * sconto) / 100, 2);

			valUPd.put(PreventiviDettaglio.CODICE_ARTICOLO, valLis.getAsString(Listini.CODICE_ARTICOLO));
			valUPd.put(PreventiviDettaglio.PREZZO_ACQ, valLis.getAsDouble(Listini.PRZ_ULTIMO_ACQUISTO));
			valUPd.put(PreventiviDettaglio.PREZZO_VEN, przLis);
			valUPd.put(PreventiviDettaglio.SCONTO, sconto);
			valUPd.put(PreventiviDettaglio.PREZZO, przVen);

		} else {
			valUPd.put(PreventiviDettaglio.CODICE_ARTICOLO, "");
			valUPd.put(PreventiviDettaglio.PREZZO_ACQ, 0);
			valUPd.put(PreventiviDettaglio.PREZZO_VEN, 0);
			valUPd.put(PreventiviDettaglio.SCONTO, 0);
			valUPd.put(PreventiviDettaglio.PREZZO, 0);
		}
		// aggiorno la riga di preventivo
		ContentValues whereUpd = new ContentValues();
		whereUpd.put(ID_PREVENTIVO_DETTAGLIO, valCurr.getAsInteger(ID_PREVENTIVO_DETTAGLIO));
		aggiornaRecord(db, valUPd, whereUpd);
		System.out.println("EConTab: PreventivoDettaglio _cambiaCodice EXIT");
	}

	/**
	 * Effettua l'aggiornamento del codice sulla riga di preventivo
	 * 
	 * @param valCurr
	 * @param db
	 */
	private void _cambiaCodiceNoLinea(ContentValues valCurr, DbInterno db) {
		// TODO Auto-generated method stub

		Join j0 = new Join(ElementiCodici.NOME_TABELLA, Listini.NOME_TABELLA);
		j0.addCampiDiJoin(ElementiCodici.CODICE_ARTICOLO, Listini.CODICE_ARTICOLO);

		String SQL = "Select " + Listini.NOME_TABELLA + ".* from " + ElementiCodici.NOME_TABELLA + j0.getSQLJoin() + " where "
				+ ElementiCodici.ID_ELEMENTO + "=" + valCurr.getAsInteger(ID_ELEMENTO) + " and " + ElementiCodici.ID_COMPONENTE + "="
				+ valCurr.getAsInteger(ID_COMPONENTE) + " and " + ElementiCodici.ID_PLACCA_MODULI + "="
				+ valCurr.getAsInteger(ID_PLACCA_MODULI) + " order by " + ElementiCodici.DATA_INS + " desc";

		ArrayList<Object> codici = db.eseguiSelect(SQL, null);
		ContentValues valLis = null;
		if (codici.size() > 0) {
			// prendo il primo codice che trovo
			valLis = (ContentValues) codici.get(0);
		}

		ContentValues valUPd = getValoriLogModifica(db);
		if (valLis != null) {

			// calcolo il prezzo di vendita da prezzo listino con sconto
			double przLis = valLis.getAsDouble(Listini.PRZ_LISTINO);
			double sconto = valLis.getAsDouble(Listini.SCONTO);
			double przVen = Utility.arrotonda(przLis - (przLis * sconto) / 100, 2);

			valUPd.put(PreventiviDettaglio.CODICE_ARTICOLO, valLis.getAsString(Listini.CODICE_ARTICOLO));
			valUPd.put(PreventiviDettaglio.PREZZO_ACQ, valLis.getAsDouble(Listini.PRZ_ULTIMO_ACQUISTO));
			valUPd.put(PreventiviDettaglio.PREZZO_VEN, przLis);
			valUPd.put(PreventiviDettaglio.SCONTO, sconto);
			valUPd.put(PreventiviDettaglio.PREZZO, przVen);
			// aggiorno la riga di preventivo
			ContentValues whereUpd = new ContentValues();
			whereUpd.put(ID_PREVENTIVO_DETTAGLIO, valCurr.getAsInteger(ID_PREVENTIVO_DETTAGLIO));
			aggiornaRecord(db, valUPd, whereUpd);
		}
	}

	/**
	 * 
	 * @param db
	 * @param val
	 *            l'elemento
	 * @param idPreventivoSelezionato
	 * @param idLocale
	 */
	/*public void aggiornaPlacche(DbInterno db, ContentValues val, ContentValues valComp, int idPreventivoSelezionato, int idLocale,
			double quantita) {
		// 1) Verifico che il locale o il cantiere abbia la placca (se l'elemento gestisce la placca)
		if (val.getAsInteger(Elementi.PLACCA_SN) == 1) {
			int idPlacca = 0;
			if (idLocale != 0) {
				ContentValues locale = new Locali().getRecordPerChiave(db, idLocale);
				if (locale != null && locale.containsKey(Locali.ID_PLACCA)) {
					idPlacca = locale.getAsInteger(Locali.ID_PLACCA);
				}
			} else {
				ContentValues cantiere = new Preventivi().getCantierePreventivo(db, idPreventivoSelezionato);
				if (cantiere != null && cantiere.getAsString(Cantieri.ID_PLACCA) != null
						&& !cantiere.getAsString(Cantieri.ID_PLACCA).equals("")) {
					idPlacca = cantiere.getAsInteger(Cantieri.ID_PLACCA);
				}
			}

			if (idPlacca != 0) {
				// 2)Prendo il numero di moduli del portafrutti e verifco che esista il record in placche_moduli
				int idPlaccaModuli = 0;

				int numeroModuli = valComp.getAsInteger(Componenti.SPAZI_OSPITATI);
				PlaccheModuli tabPlaccheModuli = new PlaccheModuli();
				idPlaccaModuli = tabPlaccheModuli.controllaInserisciRecord(db, idPlacca, numeroModuli);

				// 3)Aggiorno/inserisco la riga del preventivo
				if (idPlaccaModuli != 0) {
					ContentValues wherePrevDett = new ContentValues();
					wherePrevDett.put(ID_LOCALE, idLocale);
					wherePrevDett.put(ID_PLACCA_MODULI, idPlaccaModuli);
					wherePrevDett.put(ID_PREVENTIVO, idPreventivoSelezionato);

					ContentValues riga = db.getRecord(this, wherePrevDett);
					if (riga != null) {
						// aggiorno la quantita delle placche

						double old_qta = riga.getAsFloat(QUANTITA);
						double new_qta = old_qta + quantita;
						ContentValues whereUPD = new ContentValues();
						whereUPD.put(ID_PREVENTIVO_DETTAGLIO, riga.getAsInteger(ID_PREVENTIVO_DETTAGLIO));
						if (new_qta > 0) {
							// aggiorno la riga
							ContentValues valUPD = getValoriLogModifica(db);
							valUPD.put(QUANTITA, new_qta);
							aggiornaRecord(db, valUPD, whereUPD);

						} else {
							// cancello la riga
							cancellaRecord(db, whereUPD);
						}
					} else {
						// inserisco la placca nel preventivo
						if (quantita > 0) {
							ContentValues valPlacca = new Placche().getRecordPerChiave(db, idPlacca);
							String descri = "";
							if (valPlacca != null) {
								descri = valPlacca.getAsString(Placche.NOME_PLACCA);
							}
							ContentValues valInsert = getValoriLogInserimento(db);
							valInsert.put(ID_LOCALE, idLocale);
							if (idLocale != 0) {
								valInsert.put(TIPO, PLACCHE);
							} else {
								valInsert.put(TIPO, PLACCHE_PREVENTIVO);
							}

							valInsert.put(DESCRIZIONE, descri);
							valInsert.put(ID_PLACCA_MODULI, idPlaccaModuli);
							valInsert.put(QUANTITA, quantita);

							if (idLocale != 0) {
								aggiornaRigaPreventivo(db, idPreventivoSelezionato, valInsert, quantita, PLACCHE, false);
							} else {
								aggiornaRigaPreventivo(db, idPreventivoSelezionato, valInsert, quantita, PLACCHE_PREVENTIVO, false);
							}

						}
					}
				}
			}
		}

	}*/

	/**
	 * 
	 * @param db
	 * @param idPreventivo
	 */
	public void riassociaCodiciPreventivo(DbInterno db, int idPreventivo) {
		Preventivi tabPrev = new Preventivi();
		ContentValues where = new ContentValues();
		where.put(Preventivi.ID_PREVENTIVO, idPreventivo);
		ContentValues valPrev = db.getRecord(tabPrev, where);
		if (valPrev != null) {
			associazioneAutomaticaCodici(db, idPreventivo, valPrev.getAsInteger(Preventivi.ID_CANTIERE), 0, 0, 0, false);
		}

	}

	public boolean isAggiornaCaviETubi() {
		return aggiornaCaviETubi;
	}

	public void setAggiornaCaviETubi(boolean aggiornaCaviETubi) {
		this.aggiornaCaviETubi = aggiornaCaviETubi;
	}

	public double getQuantitaAggiunta() {
		return quantitaAggiunta;
	}

	public void setQuantitaAggiunta(double quantitaAggiunta) {
		this.quantitaAggiunta = quantitaAggiunta;
	}

	public void aggiornaRighePreventivoLocale(DbInterno db,int idPreventivo,int idLocale){
		aggiornaRighePreventivoLocale(db,idPreventivo,idLocale,true);
	}


	public void aggiornaRighePreventivoLocale(DbInterno db,int idPreventivo,int idLocale,boolean aggiornacollegamenti){
		Locali tabLoc = new Locali();
		int idLineaLocale = 0;
		ContentValues recLineaLocale = tabLoc.getLineaLocale(db,idLocale);
		if (recLineaLocale!=null){
			idLineaLocale = recLineaLocale.getAsInteger(Linee.ID_LINEA);
		}
		// 1) Prendo l'iva del cliente del preventivo
		Preventivi tabPrev = new Preventivi();
		ContentValues recCliente = tabPrev.getClientePreventivo(db, idPreventivo);
		String codiceIVA = "";
		if (recCliente != null) {
			codiceIVA = recCliente.getAsString(Anagrafica.CODICE_IVA);
		}

		//1) Elementi
		HashMap<String,Double> mappaNuoviElementi = new HashMap<String, Double>();
		ArrayList<Object> elementiPreventivo = db.eseguiSelect("Select elementi_cantiere.* from elementi_cantiere left join composizioni_cantiere on elementi_cantiere.id_elemento_cant=composizioni_cantiere.id_elemento_cant where elementi_cantiere.id_preventivo="+idPreventivo+" and elementi_cantiere.id_locale="+idLocale+" and composizioni_cantiere.id_elemento_cant is null",null);
		for (int i=0;i<elementiPreventivo.size();i++){
			ContentValues curr = (ContentValues) elementiPreventivo.get(i);
			String idElemento = curr.getAsString(ElementiCantiere.ID_ELEMENTO);
			String idLineaElemento = ""+idLineaLocale;
			if (curr.get(ElementiCantiere.ID_LINEA)!=null){
				idLineaElemento = curr.getAsString(ElementiCantiere.ID_LINEA);
				if (idLineaElemento.equals("0")|| idLineaElemento.equals("null")){
					idLineaElemento = ""+idLineaLocale;
				}
			}

			String chiave = idElemento+"|"+idLineaElemento;

			if (mappaNuoviElementi.containsKey(chiave)){
				double qta = mappaNuoviElementi.get(idElemento);
				mappaNuoviElementi.put(chiave,qta+1);
			}
			else {
				mappaNuoviElementi.put(chiave,1d);
			}
		}
		//trovo le righe del preventivo
		ArrayList<String> elementiValutati = new ArrayList<String>();
		ArrayList<Object> righePreventivoElem = db.eseguiSelect("Select * from preventivi_dettaglio where id_preventivo="+idPreventivo+" and preventivi_dettaglio.id_locale="+idLocale+" and id_elemento<>0 and tipo in ('MA','MP')",null);
		for (int i=0;i<righePreventivoElem.size();i++){
			ContentValues rigaCurr = (ContentValues) righePreventivoElem.get(i);
			String idElemento = rigaCurr.getAsString(PreventiviDettaglio.ID_ELEMENTO);
			String idLineaRiga = ""+idLineaLocale;
			if (rigaCurr.get(PreventiviDettaglio.ID_LINEA)!=null){
				idLineaRiga = rigaCurr.getAsString(PreventiviDettaglio.ID_LINEA);
				if (idLineaRiga.equals("0")|| idLineaRiga.equals("null")){
					idLineaRiga = ""+idLineaLocale;
				}
			}
			String chiaveRiga = idElemento+"|"+idLineaRiga;
			int id_preventivo_dettaglio = rigaCurr.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO);
			if (mappaNuoviElementi.containsKey(chiaveRiga)){
				elementiValutati.add(chiaveRiga);
				//se la quantità è diversa aggiorno
				double qtaNew = mappaNuoviElementi.get(chiaveRiga);
				double qtaOld = rigaCurr.getAsDouble(PreventiviDettaglio.QUANTITA);
				if (qtaNew!=qtaOld){
					ContentValues where = new ContentValues();
					where.put(ID_PREVENTIVO_DETTAGLIO,id_preventivo_dettaglio);
					ContentValues valUpd = getValoriLogModifica(db);
					valUpd.put(QUANTITA,qtaNew);
					db.update(getNomeTabella(),valUpd,where);
				}
			}
			else {
				//se i nuovi elementi non contengono l'elemento allora elimino dal preventivo
				ContentValues where = new ContentValues();
				where.put(ID_PREVENTIVO_DETTAGLIO,id_preventivo_dettaglio);
				//db.delete(getNomeTabella(),where);
				cancellaRecord(db,where);
			}
		}
		for (int i=0;i<elementiValutati.size();i++){
			mappaNuoviElementi.remove(elementiValutati.get(i));
		}
		//se sono rimasti degli elementi da aggiungere allora eseguo l'inserimento nel preventivo
		Iterator<String> iterNuovi = mappaNuoviElementi.keySet().iterator();
		while (iterNuovi.hasNext()){
			String chiaveElementoNuovo = iterNuovi.next();
			String idElemetoNuovo = chiaveElementoNuovo.split("\\|")[0];
			String idLineaRigaNuova = chiaveElementoNuovo.split("\\|")[1];
			ContentValues valEleme = db.getRecord("Select * from elementi where id_elemento="+idElemetoNuovo);
			double qta = mappaNuoviElementi.get(chiaveElementoNuovo);
			// inserisco nuova riga

			ContentValues valINS = getValoriLogInserimento(db);
			valINS.put(ID_PREVENTIVO, idPreventivo);
			if (idLocale==0){
				valINS.put(TIPO, MATERIALE_PREVENTIVO);
			}
			else {
				valINS.put(TIPO, MATERIALE);
			}
			valINS.put(ID_LOCALE, idLocale);
	        valINS.put(ID_ELEMENTO, idElemetoNuovo);
			valINS.put(UNITA_MISURA, valEleme.getAsString(Elementi.UNITA_MISURA));
			if (idLineaRigaNuova.equals(""+idLineaLocale)){
				valINS.put(ID_LINEA,idLineaLocale);
			}
			else {
				valINS.put(ID_LINEA, Integer.parseInt(idLineaRigaNuova));
			}
			valINS.put(DESCRIZIONE, valEleme.getAsString(Elementi.NOME_ELEMENTO));
			valINS.put(QUANTITA, qta);
			valINS.put(CODICE_IVA, codiceIVA);
			valINS.put(STATO, APERTO);
			inserisciRecord(db, valINS);
		}



		//2) componenti
		HashMap<String,Double> mappaNuoviComponenti = new HashMap<String, Double>();
		//prendo siaa i componenti finiti che i componenti composti

		ArrayList<Object> componentiPreventivo = db.eseguiSelect("Select componenti_cantiere.*,componenti.linea_sn,elementi_cantiere.id_linea as id_linea_componente from componenti_cantiere inner join composizioni_cantiere on componenti_cantiere.id_componente_cant=composizioni_cantiere.id_componente_cant inner join elementi_cantiere on composizioni_cantiere.id_elemento_cant=elementi_cantiere.id_elemento_cant inner join componenti on componenti_cantiere.id_componente=componenti.id_componente left join componenti_cant_composti on componenti_cantiere.id_componente_cant=componenti_cant_composti.id_componente_cant_padre where componenti_cant_composti.id_componente_cant_padre is null and componenti_cantiere.id_preventivo="+idPreventivo+" and elementi_cantiere.id_locale="+idLocale,null);
		componentiPreventivo.addAll(db.eseguiSelect("Select componenti_cantiere_figlio.*,componenti.linea_sn,elementi_cantiere.id_linea as id_linea_componente from componenti_cantiere inner join composizioni_cantiere on componenti_cantiere.id_componente_cant=composizioni_cantiere.id_componente_cant inner join elementi_cantiere on composizioni_cantiere.id_elemento_cant=elementi_cantiere.id_elemento_cant inner join componenti_cant_composti on componenti_cantiere.id_componente_cant=componenti_cant_composti.id_componente_cant_padre inner join componenti_cantiere as componenti_cantiere_figlio on componenti_cant_composti.id_componente_cant_figlio=componenti_cantiere_figlio.id_componente_cant inner join componenti on componenti_cantiere_figlio.id_componente=componenti.id_componente  where componenti_cantiere.id_preventivo="+idPreventivo+" and elementi_cantiere.id_locale="+idLocale,null));
		for (int i=0;i<componentiPreventivo.size();i++){
			ContentValues curr = (ContentValues) componentiPreventivo.get(i);
			String idComponente = curr.getAsString(ComponentiCantiere.ID_COMPONENTE);
			String idLineaComponente = ""+idLineaLocale;
			int gestitoALinea = curr.getAsInteger(Componenti.LINEA_SN);
			if (gestitoALinea==1 && curr.get("id_linea_componente")!=null){
				idLineaComponente = curr.getAsString("id_linea_componente");
				if (idLineaComponente.equals("0") || idLineaComponente.equals("null")){
					idLineaComponente = ""+idLineaLocale;
				}
			}
			String chiaveComponente = idComponente+"|"+idLineaComponente;
			if (mappaNuoviComponenti.containsKey(chiaveComponente)){
				double qta = mappaNuoviComponenti.get(chiaveComponente);
				mappaNuoviComponenti.put(chiaveComponente,qta+1);
			}
			else {
				mappaNuoviComponenti.put(chiaveComponente,1d);
			}
		}
		//trovo le righe del preventivo
		ArrayList<String> componentiValutati = new ArrayList<String>();
		ArrayList<Object> righePreventivoComp = db.eseguiSelect("Select * from preventivi_dettaglio where id_preventivo="+idPreventivo+" and preventivi_dettaglio.id_locale="+idLocale+" and id_elemento=0 and id_componente<>0 and tipo in ('MA','MP')",null);
		for (int i=0;i<righePreventivoComp.size();i++){
			ContentValues rigaCurr = (ContentValues) righePreventivoComp.get(i);
			String idComp = rigaCurr.getAsString(PreventiviDettaglio.ID_COMPONENTE);
			String idLineaRiga = ""+idLineaLocale;
			if (rigaCurr.get(PreventiviDettaglio.ID_LINEA)!=null){
				idLineaRiga = rigaCurr.getAsString(PreventiviDettaglio.ID_LINEA);
				if (idLineaRiga.equals("0")|| idLineaRiga.equals("null")){
					idLineaRiga = ""+idLineaLocale;
				}
			}
			String chiaveRiga = idComp+"|"+idLineaRiga;
			int id_preventivo_dettaglio = rigaCurr.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO);
			if (mappaNuoviComponenti.containsKey(chiaveRiga)){
				componentiValutati.add(chiaveRiga);
				//se la quantità è diversa aggiorno
				double qtaNew = mappaNuoviComponenti.get(chiaveRiga);
				double qtaOld = rigaCurr.getAsDouble(PreventiviDettaglio.QUANTITA);
				if (qtaNew!=qtaOld){
					ContentValues where = new ContentValues();
					where.put(ID_PREVENTIVO_DETTAGLIO,id_preventivo_dettaglio);
					ContentValues valUpd =getValoriLogModifica(db);
					valUpd.put(QUANTITA,qtaNew);
					db.update(getNomeTabella(),valUpd,where);
				}
			}
			else {
				//se i nuovi elementi non contengono l'elemento allora elimino dal preventivo
				ContentValues where = new ContentValues();
				where.put(ID_PREVENTIVO_DETTAGLIO,id_preventivo_dettaglio);
				//db.delete(getNomeTabella(),where);
				cancellaRecord(db,where);
			}
		}
		for (int i=0;i<componentiValutati.size();i++){
			mappaNuoviComponenti.remove(componentiValutati.get(i));
		}
		//se sono rimasti degli elementi da aggiungere allora eseguo l'inserimento nel preventivo
		Iterator<String> iterNuoviComp = mappaNuoviComponenti.keySet().iterator();
		while (iterNuoviComp.hasNext()){
			String chiaveComponenteNuovo = iterNuoviComp.next();
			String idComponenteNuovo = chiaveComponenteNuovo.split("\\|")[0];
			String idLineaRigaNuova = chiaveComponenteNuovo.split("\\|")[1];
			ContentValues valComp = db.getRecord("Select * from componenti where id_componente="+idComponenteNuovo);
			double qta = mappaNuoviComponenti.get(chiaveComponenteNuovo);
			// inserisco nuova riga

			ContentValues valINS = getValoriLogInserimento(db);
			valINS.put(ID_PREVENTIVO, idPreventivo);
			if (idLocale==0){
				valINS.put(TIPO, MATERIALE_PREVENTIVO);
			}
			else {
				valINS.put(TIPO, MATERIALE);
			}
			valINS.put(ID_LOCALE, idLocale);
			valINS.put(ID_COMPONENTE, idComponenteNuovo);
			valINS.put(UNITA_MISURA, valComp.getAsString(Componenti.UNITA_MISURA));
			if (idLineaRigaNuova.equals(""+idLineaLocale)){
				valINS.put(ID_LINEA, idLineaLocale);
			}
			else {
				valINS.put(ID_LINEA, Integer.parseInt(idLineaRigaNuova));
			}
			valINS.put(DESCRIZIONE, valComp.getAsString(Componenti.NOME_COMPONENTE));
			valINS.put(QUANTITA, qta);
			valINS.put(CODICE_IVA, codiceIVA);
			valINS.put(STATO, APERTO);
			inserisciRecord(db, valINS);
		}

		aggiornaPlaccheLocalePreventivo(db,idPreventivo,idLocale);

		if (aggiornacollegamenti){
			aggiornaTubiECaviLocale(db,idPreventivo,idLocale);
		}

	}

	public void aggiornaPlaccheLocalePreventivo(DbInterno db, int idPreventivo, int idLocale) {
		Preventivi tabPrev = new Preventivi();
		ContentValues recCliente = tabPrev.getClientePreventivo(db, idPreventivo);
		String codiceIVA = "";

		if (recCliente != null) {
			codiceIVA = recCliente.getAsString(Anagrafica.CODICE_IVA);
		}

		HashMap<String,Integer> mappaNuovePlacche = new HashMap<String, Integer>();
		int idPlaccaLocale = 0;
		ContentValues locale = new Locali().getRecordPerChiave(db, idLocale);
		if (locale != null && locale.containsKey(Locali.ID_PLACCA)) {
			idPlaccaLocale = locale.getAsInteger(Locali.ID_PLACCA);
		}

		//prendo tutti i componenti di tipo portafrutti caricati in questo preventivo per associarlo all'id_elemento
		ArrayList<Object> portafruttiPreventivo = db.eseguiSelect("Select elementi_cantiere.id_elemento_cant,componenti_cantiere.* from componenti_cantiere inner join composizioni_cantiere on componenti_cantiere.id_componente_cant=composizioni_cantiere.id_componente_cant inner join elementi_cantiere on composizioni_cantiere.id_elemento_cant=elementi_cantiere.id_elemento_cant inner join componenti on componenti_cantiere.id_componente=componenti.id_componente inner join categorie_componenti on componenti.id_categoria_componente=categorie_componenti.id_categoria_componente where categorie_componenti.tipo='"+CategorieComponenti.TIPO_PORTAFRUTTI+"' and componenti_cantiere.id_preventivo="+idPreventivo+" and elementi_cantiere.id_locale="+idLocale,null);
		HashMap<String,ArrayList<ContentValues>> mappaPortaFrutti = new HashMap<String, ArrayList<ContentValues>>();
		for (int i=0;i<portafruttiPreventivo.size();i++){
			ContentValues valPortafrutti = (ContentValues)portafruttiPreventivo.get(i);
			String idElemento = valPortafrutti.getAsString(ElementiCantiere.ID_ELEMENTO_CANT);
			if (!mappaPortaFrutti.containsKey(idElemento)){
				ArrayList<ContentValues> portafrutti = new ArrayList<ContentValues>();
				portafrutti.add(valPortafrutti);
				mappaPortaFrutti.put(idElemento,portafrutti);
			}
			else {
				ArrayList<ContentValues> portafrutti = mappaPortaFrutti.get(idElemento);
				portafrutti.add(valPortafrutti);
			}
		}

		//prendo tutti gli elementi con placca si nel locale
		ArrayList<Object> elementiCantiereConPlacca = db.eseguiSelect("Select * from elementi_cantiere where  id_locale="+idLocale+" and placca_sn=1",null);
		for (int i=0;i<elementiCantiereConPlacca.size();i++){
			ContentValues valCurr = (ContentValues) elementiCantiereConPlacca.get(i);
			String idElementoCantCurr = valCurr.getAsString(ElementiCantiere.ID_ELEMENTO_CANT);
			int idPlaccaElemento = idPlaccaLocale;
			if (valCurr.get(ElementiCantiere.ID_PLACCA)!=null && !valCurr.getAsString(ElementiCantiere.ID_PLACCA).equals("")){
				if (valCurr.getAsInteger(ElementiCantiere.ID_PLACCA)!=0){
					idPlaccaElemento = valCurr.getAsInteger(ElementiCantiere.ID_PLACCA);
				}
			}

			if (idPlaccaElemento!=0){
				//prendo la placca moduli
				if (mappaPortaFrutti.containsKey(idElementoCantCurr)){
					ArrayList<ContentValues> portafrutti = mappaPortaFrutti.get(idElementoCantCurr);
					for (int p=0;p<portafrutti.size();p++){
						// 2)Prendo il numero di moduli del portafrutti e verifco che esista il record in placche_moduli
						ContentValues valComp = portafrutti.get(p);
						int numeroModuli = valComp.getAsInteger(ComponentiCantiere.SPAZI_OSPITATI_CANT);
						PlaccheModuli tabPlaccheModuli = new PlaccheModuli();
						int idPlaccaModuli = tabPlaccheModuli.controllaInserisciRecord(db, idPlaccaElemento, numeroModuli);
						int qta = 0;
						if (mappaNuovePlacche.containsKey(""+idPlaccaModuli)){
							qta = mappaNuovePlacche.get(""+idPlaccaModuli);
						}
						mappaNuovePlacche.put(""+idPlaccaModuli,qta+1);
					}
				}
			}
		}

		//ora verifico se devo aggiornare le righe del preventivo
		ArrayList<String> componentiValutati = new ArrayList<String>();
		ArrayList<Object> righePreventivoComp = db.eseguiSelect("Select * from preventivi_dettaglio where id_preventivo="+idPreventivo+" and preventivi_dettaglio.id_locale="+idLocale+" and id_elemento=0 and id_componente=0 and id_placca_moduli<>0 and tipo in ('PL','PP')",null);
		for (int i=0;i<righePreventivoComp.size();i++){
			ContentValues rigaCurr = (ContentValues) righePreventivoComp.get(i);
			String idPlaccaCurr = rigaCurr.getAsString(PreventiviDettaglio.ID_PLACCA_MODULI);
			int id_preventivo_dettaglio = rigaCurr.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO);
			if (mappaNuovePlacche.containsKey(idPlaccaCurr)){
				componentiValutati.add(idPlaccaCurr);
				//se la quantità è diversa aggiorno
				double qtaNew = mappaNuovePlacche.get(idPlaccaCurr);
				double qtaOld = rigaCurr.getAsDouble(PreventiviDettaglio.QUANTITA);
				if (qtaNew!=qtaOld){
					ContentValues where = new ContentValues();
					where.put(ID_PREVENTIVO_DETTAGLIO,id_preventivo_dettaglio);
					ContentValues valUpd = getValoriLogModifica(db);

					valUpd.put(QUANTITA,qtaNew);
					db.update(getNomeTabella(),valUpd,where);
				}
			}
			else {
				//se i nuovi elementi non contengono l'elemento allora elimino dal preventivo
				ContentValues where = new ContentValues();
				where.put(ID_PREVENTIVO_DETTAGLIO,id_preventivo_dettaglio);
				//db.delete(getNomeTabella(),where);
				cancellaRecord(db,where);
			}
		}
		for (int i=0;i<componentiValutati.size();i++){
			mappaNuovePlacche.remove(componentiValutati.get(i));
		}
		//se sono rimasti degli elementi da aggiungere allora eseguo l'inserimento nel preventivo
		Iterator<String> iterNuoviComp = mappaNuovePlacche.keySet().iterator();
		while (iterNuoviComp.hasNext()){
			String idPlaccaModuliNuovo = iterNuoviComp.next();

			ContentValues valPlacca = db.getRecord("Select placche.* from placche_moduli inner join placche on placche_moduli.id_placca=placche.id_placca where placche_moduli.id_placca_moduli=" + idPlaccaModuliNuovo);
			double qta = mappaNuovePlacche.get(idPlaccaModuliNuovo);
			// inserisco nuova riga

			ContentValues valINS = getValoriLogInserimento(db);
			valINS.put(ID_PREVENTIVO, idPreventivo);
			if (idLocale==0){
				valINS.put(TIPO, PLACCHE_PREVENTIVO);
			}
			else {
				valINS.put(TIPO, PLACCHE);
			}
			valINS.put(ID_LOCALE, idLocale);
			valINS.put(ID_LINEA,valPlacca.getAsInteger(Placche.ID_LINEA));
			valINS.put(ID_COMPONENTE, 0);
			valINS.put(ID_PLACCA_MODULI, idPlaccaModuliNuovo);
			valINS.put(UNITA_MISURA, "PZ");
			valINS.put(DESCRIZIONE, valPlacca.getAsString(Placche.NOME_PLACCA));
			valINS.put(QUANTITA, qta);
			valINS.put(CODICE_IVA, codiceIVA);
			valINS.put(STATO, APERTO);
			inserisciRecord(db, valINS);
		}
	}


	public String aggiornaRigaMaterialePreventivo(DbInterno db, int idPreventivo, ContentValues valori, double qta,String tipo,
										 boolean componente) {
		// 1) Cerco se c'� gi� la riga nel preventivo

		ContentValues where = new ContentValues();

		ContentValues recordCheck = null;

		// Verifico se sono in inserimento o in aggiornamento
		if (valori.containsKey(ID_PREVENTIVO_DETTAGLIO)) {
			// aggiornamento
			where.put(ID_PREVENTIVO_DETTAGLIO, valori.getAsInteger(ID_PREVENTIVO_DETTAGLIO));
			recordCheck = db.getRecord(this, where);
		} else {
			// inserimento
			where.put(ID_PREVENTIVO, idPreventivo);
			where.put(TIPO, tipo);
			where.put(ID_LOCALE, 0);
			if (tipo.equals(MATERIALE_PREVENTIVO)){
				if (componente) {
					where.put(ID_COMPONENTE, valori.getAsInteger(ComponentiCantiere.ID_COMPONENTE));
				} else {
					where.put(ID_ELEMENTO, valori.getAsInteger(ElementiCantiere.ID_ELEMENTO));
				}
			}
			if (tipo.equals(PLACCHE_PREVENTIVO)){
				where.put(ID_PLACCA_MODULI, valori.getAsInteger(ID_PLACCA_MODULI));
			}

			//per la manodopera inserisco sempre una nuova riga
			if (!tipo.equals(MANOPERA) && !tipo.equals(ALTRO)){
				recordCheck = db.getRecord(this, where);
			}


		}

		//per la manodopera accetto anche qta = 0 che potrebbe arrivare dal rapportino inserito a fronte di un tipo manodopera non presente nell'ordine
		if (recordCheck == null && (qta > 0 || (qta==0 && tipo==MANOPERA))) {
			// inserisco nuova riga
			// 1) Prendo l'iva del cliente del preventivo
			Preventivi tabPrev = new Preventivi();
			ContentValues recCliente = tabPrev.getClientePreventivo(db, idPreventivo);
			String codiceIVA = "";
			if (recCliente != null) {
				codiceIVA = recCliente.getAsString(Anagrafica.CODICE_IVA);
			}

			// 2) Inserisco la nuova riga
			ContentValues valINS = getValoriLogInserimento(db);
			valINS.put(ID_PREVENTIVO, idPreventivo);
			valINS.put(TIPO, tipo);
			valINS.put(ID_LOCALE, 0);
			if (tipo.equals(MATERIALE_PREVENTIVO)){
				if (componente) {
					valINS.put(ID_COMPONENTE, valori.getAsInteger(Componenti.ID_COMPONENTE));
					valINS.put(UNITA_MISURA, valori.getAsString(Componenti.UNITA_MISURA));
					valINS.put(DESCRIZIONE, valori.getAsString(Componenti.NOME_COMPONENTE));

				} else {
					valINS.put(ID_ELEMENTO, valori.getAsInteger(ElementiCantiere.ID_ELEMENTO));
					valINS.put(UNITA_MISURA, valori.getAsString(ElementiCantiere.UNITA_MISURA));
					valINS.put(DESCRIZIONE, valori.getAsString(Elementi.NOME_ELEMENTO));
				}
			}
			if (tipo.equals(PLACCHE_PREVENTIVO)) {
				valINS.put(DESCRIZIONE, valori.getAsString(DESCRIZIONE));
				valINS.put(ID_PLACCA_MODULI, valori.getAsInteger(ID_PLACCA_MODULI));
				valINS.put(UNITA_MISURA, "PZ");
			}

			if (tipo.equals(MANOPERA)) {
				valINS.put(ID_MANODOPERA, valori.getAsInteger(Manodopera.ID_MANODOPERA));
				valINS.put(NUM_OPERATORI, valori.getAsInteger(Manodopera.NUM_OPERATORI_DEFAULT));
				valINS.put(DESCRIZIONE, valori.getAsString(Manodopera.NOME));
				valINS.put(PREZZO, valori.getAsFloat(Manodopera.COSTO_ORARIO));
				valINS.put(UNITA_MISURA, "H");
			}

			if (tipo.equals(ALTRO)) {
				valINS.put(NOTE, valori.getAsString(PreventiviDettaglio.NOTE));
				valINS.put(PREZZO, valori.getAsFloat(PreventiviDettaglio.PREZZO));
			}


			valINS.put(QUANTITA, qta);
			valINS.put(CODICE_IVA, codiceIVA);
			valINS.put(STATO, APERTO);

			inserisciRecord(db, valINS);

		} else {
			// aggiorno la riga trovata
			if (valori.containsKey(ID_PREVENTIVO_DETTAGLIO)) {
				// Se sono in aggiornamento allora aggiorno tutti i valori
				ContentValues valUpd = getValoriLogModifica(db);

				for (int i = 0; i < getNomiCampi().size(); i++) {
					String nomeCampo = getNomiCampi().get(i);
					if (valori.containsKey(nomeCampo) && !isChiave(nomeCampo)) {
						if (getTipoCampo(nomeCampo).equals(INTEGER)) {
							valUpd.put(nomeCampo, valori.getAsInteger(nomeCampo));
						}
						if (getTipoCampo(nomeCampo).equals(TEXT)) {
							valUpd.put(nomeCampo, valori.getAsString(nomeCampo));
						}
						if (getTipoCampo(nomeCampo).equals(NUMERIC)) {
							valUpd.put(nomeCampo, valori.getAsFloat(nomeCampo));
						}
					}
				}
				ContentValues whereUpd = new ContentValues();
				whereUpd.put(ID_PREVENTIVO_DETTAGLIO, valori.getAsInteger(ID_PREVENTIVO_DETTAGLIO));
				aggiornaRecord(db, valUpd, whereUpd);

			} else {
				// se sono in inserimento allora aggiorno solo la qta
				if (recordCheck != null) {
					double old_qta = recordCheck.getAsDouble(QUANTITA);
					double new_qta = old_qta + qta;
					ContentValues whereUPD = new ContentValues();
					whereUPD.put(ID_PREVENTIVO_DETTAGLIO, recordCheck.getAsInteger(ID_PREVENTIVO_DETTAGLIO));
					if (new_qta > 0) {
						// aggiorno la riga
						ContentValues valUPD = getValoriLogModifica(db);
						valUPD.put(QUANTITA, new_qta);
						setQuantitaAggiunta(qta);
						aggiornaRecord(db, valUPD, whereUPD);

					} else {
						// cancello la riga
						cancellaRecord(db, whereUPD);
					}
				}
			}

		}

		return "";
	}


}
