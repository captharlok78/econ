package pfa.app.econtab.db.table;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.utils.Sessione;

public class ElementiCantiere extends AbstractTable {
	public static final String NOME_TABELLA = "elementi_cantiere";

	public static final String ID_ELEMENTO_CANT = "id_elemento_cant";
	public static final String ID_ELEMENTO = "id_elemento";
	public static final String NOME_ELEMENTO_CANT = "nome_elemento_cant";
	public static final String NUMERO_IDENTIFICATIVO = "numero_identificativo";
	public static final String ALTEZZA_DA_TERRA = "altezza_da_terra";
	public static final String PLACCA_SN = "placca_sn";
	public static final String ID_PREVENTIVO = "id_preventivo";
	public static final String ID_ELEMENTO_CANT_ORIGINE = "id_elemento_cant_origine";
	public static final String PREVENTIVI = "preventivi";
	public static final String ID_CANTIERE = "id_cantiere";
	public static final String ID_UNITA = "id_unita";
	public static final String ID_AREA = "id_area";
	public static final String ID_LOCALE = "id_locale";
	public static final String METRI_CAVO_CANT = "metri_cavo_cant";
	public static final String ID_ELEMENTO_CAVO = "id_elemento_cavo";
	public static final String METRI_TUBO_CANT = "metri_tubo_cant";
	public static final String ID_ELEMENTO_TUBO = "id_elemento_tubo";
	public static final String POS_X = "pos_x";
	public static final String POS_Y = "pos_y";
	public static final String UNITA_MISURA = "unita_misura";
	public static final String NOTA = "nota";
	public static final String NON_CONTEGGIARE_PREVENTIVI = "non_conteggiare_preventivi";
	public static final String ID_LINEA = "id_linea";
	public static final String ID_PLACCA = "id_placca";

	private int idElementoCantierePreferito = 0;

	public ElementiCantiere() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_ELEMENTO_CANT);

		aggiungiCampo(ID_ELEMENTO_CANT, INTEGER);
		aggiungiCampo(ID_ELEMENTO, INTEGER);
		aggiungiCampo(NOME_ELEMENTO_CANT, TEXT);
		aggiungiCampo(NUMERO_IDENTIFICATIVO, INTEGER);
		aggiungiCampo(ALTEZZA_DA_TERRA, INTEGER);
		aggiungiCampo(PLACCA_SN, INTEGER);
		aggiungiCampo(ID_PREVENTIVO, INTEGER);
		aggiungiCampo(ID_ELEMENTO_CANT_ORIGINE, INTEGER);
		aggiungiCampo(PREVENTIVI, TEXT);
		aggiungiCampo(ID_CANTIERE, INTEGER);
		aggiungiCampo(ID_UNITA, INTEGER);
		aggiungiCampo(ID_AREA, INTEGER);
		aggiungiCampo(ID_LOCALE, INTEGER);
		aggiungiCampo(METRI_CAVO_CANT, NUMERIC);
		aggiungiCampo(ID_ELEMENTO_CAVO, INTEGER);
		aggiungiCampo(METRI_TUBO_CANT, NUMERIC);
		aggiungiCampo(ID_ELEMENTO_TUBO, INTEGER);
		aggiungiCampo(POS_X, INTEGER);
		aggiungiCampo(POS_Y, INTEGER);
		aggiungiCampo(UNITA_MISURA, TEXT);
		aggiungiCampo(NOTA, TEXT);
		aggiungiCampo(NON_CONTEGGIARE_PREVENTIVI,INTEGER);
		aggiungiCampo(ID_LINEA, INTEGER);
		aggiungiCampo(ID_PLACCA, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_ELEMENTO_CANT);
	}

	public int getProssimoNumero(DbInterno db, int cantiere) {
		int prossimo = 1;
		/**
		 * String SQL = "Select max(" + NUMERO_IDENTIFICATIVO + ") as numero from " + NOME_TABELLA + " where " +
		 * ID_CANTIERE + " = " + cantiere + " order by " + NUMERO_IDENTIFICATIVO + " desc ";
		 */

		// Prendo il prossimo numero guardando l'id cantiere dal locale perch� gli elementi dei preventivi non
		// consolidati
		// non hanno ancora scritto l'id cantiere

		String SQL = "Select max(" + NUMERO_IDENTIFICATIVO + ") as numero from " + NOME_TABELLA + " where " + ID_LOCALE + " in ( select  "
				+ Locali.ID_LOCALE + " from " + Locali.NOME_TABELLA + Locali.getJoinPerIDCantiere() + " where " + Unita.ID_CANTIERE + " = "
				+ cantiere + ") order by " + NUMERO_IDENTIFICATIVO + " desc ";
		/*String SQL = "Select max(" + NUMERO_IDENTIFICATIVO + ") as numero from " + NOME_TABELLA + " where " + ID_LOCALE + " in ( select  "
				+ Locali.ID_LOCALE + " from " + Locali.NOME_TABELLA + Locali.getJoinPerIDCantiere() + " where " + Unita.ID_CANTIERE + " = "
				+ cantiere + ") and (id_preventivo not in (select id_preventivo from preventivi where id_cantiere="+cantiere+" and tipo='P') or id_preventivo = "+ Sessione.getIdPreventivoSelezionato()+") order by " + NUMERO_IDENTIFICATIVO + " desc ";*/
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

	/**
	 * 
	 * - copio l'eventuale composizione <br>
	 * - copio gli eventuali componenti <br>
	 * - aggiorno il dettaglio preventivo
	 * 
	 * @param db
	 * @param val
	 */
	@Override
	protected void inserimentoCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		ComponentiCantiere tabCompCant = new ComponentiCantiere();
		ComposizioniCantiere tabComposCant = new ComposizioniCantiere();
		if (idElementoCantierePreferito != 0) {
			ArrayList<Object> composizioni = tabComposCant.getComposizioneElementoCantiere(db, idElementoCantierePreferito);
			for (int i = 0; i < composizioni.size(); i++) {
				ContentValues composizione = (ContentValues) composizioni.get(i);

				// INSERISCO IL COMPONENTE
				ContentValues valComp = tabCompCant.getValoriLogInserimento(db);
				valComp.put(ComponentiCantiere.ID_COMPONENTE, composizione.getAsInteger(ComponentiCantiere.ID_COMPONENTE));
				valComp.put(ComponentiCantiere.NOME_COMPONENTE_CANT, composizione.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
				valComp.put(ComponentiCantiere.SPAZI_OCCUPATI_CANT, composizione.getAsFloat(ComponentiCantiere.SPAZI_OCCUPATI_CANT));
				valComp.put(ComponentiCantiere.SPAZI_OSPITATI_CANT, composizione.getAsInteger(ComponentiCantiere.SPAZI_OSPITATI_CANT));
				valComp.put(ComponentiCantiere.TAPPO_SN_CANT, composizione.getAsInteger(ComponentiCantiere.TAPPO_SN_CANT));
				valComp.put(ComponentiCantiere.METRI_CAVO_CANT, composizione.getAsFloat(ComponentiCantiere.METRI_CAVO_CANT));
				valComp.put(ComponentiCantiere.ID_ELEMENTO_CAVO, composizione.getAsInteger(ComponentiCantiere.ID_ELEMENTO_CAVO));
				valComp.put(ComponentiCantiere.ID_ELEMENTO_TUBO, composizione.getAsInteger(ComponentiCantiere.ID_ELEMENTO_TUBO));
				valComp.put(ComponentiCantiere.METRI_TUBO_CANT, composizione.getAsFloat(ComponentiCantiere.METRI_TUBO_CANT));
				valComp.put(ComponentiCantiere.UNITA_MISURA, composizione.getAsString(ComponentiCantiere.UNITA_MISURA));
				valComp.put(ComponentiCantiere.ID_PREVENTIVO,val.getAsInteger(ID_PREVENTIVO));

				tabCompCant.inserisciRecord(db, valComp);

				// INSERISCO LA COMPOSIZIONE
				ContentValues valComposizione = tabComposCant.getValoriLogInserimento(db);
				valComposizione.put(ComposizioniCantiere.ID_ELEMENTO_CANT, val.getAsInteger(ID_ELEMENTO_CANT));
				valComposizione.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
				valComposizione.put(ComposizioniCantiere.POSIZIONE_INIZIALE,
						composizione.getAsInteger(ComposizioniCantiere.POSIZIONE_INIZIALE));
				valComposizione.put(ComposizioniCantiere.MODULI_OCCUPATI_CANT,
						composizione.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT));

				tabComposCant.inserisciRecord(db, valComposizione);
			}

			// AGGIUNGO L'ELEMENTO O I COMPONENTI DELL'ELEMENTO AL DETTAGLIO PREVENTIVO (SE SELEZIONATO)
			int idPreventivo = val.getAsInteger(ID_PREVENTIVO);
			if (idPreventivo != 0) {
				PreventiviDettaglio tabPrev = new PreventiviDettaglio();

				/*if (composizioni.size() > 0) {

					// aggiorno le placche
					for (int i = 0; i < composizioni.size(); i++) {
						ContentValues valComp = (ContentValues) composizioni.get(i);
						if (valComp.getAsString(CategorieComponenti.TIPO).equals(CategorieComponenti.TIPO_PORTAFRUTTI)) {
							valComp.put(Componenti.SPAZI_OSPITATI, valComp.getAsInteger(ComponentiCantiere.SPAZI_OSPITATI_CANT));
							tabPrev.aggiornaPlacche(db, val, valComp, idPreventivo, val.getAsInteger(ID_LOCALE), 1);
						}
					}

				} else {
					// AGGIUNGO Il NOME DELL'ELEMENTO
					val.put(Elementi.NOME_ELEMENTO, val.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
					tabPrev.aggiungiAPreventivo(db, idPreventivo, val);
				}*/
			}

		} else {

			int idElemento = val.getAsInteger(ElementiCantiere.ID_ELEMENTO);
			Composizioni tabComp = new Composizioni();
			ArrayList<Object> composizioni = tabComp.getComposizioneElemento(db, idElemento);

			for (int i = 0; i < composizioni.size(); i++) {
				ContentValues composizione = (ContentValues) composizioni.get(i);

				// INSERISCO IL COMPONENTE
				ContentValues valComp = tabCompCant.getValoriLogInserimento(db);

				valComp.put(ComponentiCantiere.ID_COMPONENTE, composizione.getAsInteger(Composizioni.ID_COMPONENTE));
				valComp.put(ComponentiCantiere.NOME_COMPONENTE_CANT, composizione.getAsString(Componenti.NOME_COMPONENTE));
				valComp.put(ComponentiCantiere.SPAZI_OCCUPATI_CANT, composizione.getAsFloat(Componenti.SPAZI_OCCUPATI));
				valComp.put(ComponentiCantiere.SPAZI_OSPITATI_CANT, composizione.getAsInteger(Componenti.SPAZI_OSPITATI));
				valComp.put(ComponentiCantiere.TAPPO_SN_CANT, composizione.getAsInteger(Componenti.TAPPO_SN));
				valComp.put(ComponentiCantiere.METRI_CAVO_CANT, composizione.getAsFloat(Componenti.METRI_CAVO));
				valComp.put(ComponentiCantiere.ID_ELEMENTO_CAVO, composizione.getAsInteger(Componenti.ID_ELEMENTO_CAVO));
				valComp.put(ComponentiCantiere.ID_ELEMENTO_TUBO, composizione.getAsInteger(Componenti.ID_ELEMENTO_TUBO));
				valComp.put(ComponentiCantiere.METRI_TUBO_CANT, composizione.getAsFloat(Componenti.METRI_TUBO));
				valComp.put(ComponentiCantiere.UNITA_MISURA, composizione.getAsString(Componenti.UNITA_MISURA));
				valComp.put(ComponentiCantiere.ID_PREVENTIVO, val.getAsInteger(ID_PREVENTIVO));

				tabCompCant.inserisciRecord(db, valComp);

				// INSERISCO LA COMPOSIZIONE
				ContentValues valComposizione = tabComposCant.getValoriLogInserimento(db);
				valComposizione.put(ComposizioniCantiere.ID_ELEMENTO_CANT, val.getAsInteger(ID_ELEMENTO_CANT));
				valComposizione.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
				valComposizione.put(ComposizioniCantiere.POSIZIONE_INIZIALE, composizione.getAsInteger(Composizioni.POSIZIONE_INIZIALE));
				valComposizione.put(ComposizioniCantiere.MODULI_OCCUPATI_CANT, composizione.getAsString(Composizioni.MODULI_OCCUPATI));

				//System.out.println("EConTab: ElementiCantiere inserimentoCorrelati valComposizione " + valComposizione);

				tabComposCant.inserisciRecord(db, valComposizione);

			}

			// AGGIUNGO L'ELEMENTO O I COMPONENTI DELL'ELEMENTO AL DETTAGLIO PREVENTIVO (SE SELEZIONATO)
			int idPreventivo = val.getAsInteger(ID_PREVENTIVO);
			if (idPreventivo != 0) {
				PreventiviDettaglio tabPrev = new PreventiviDettaglio();

				/*PreventiviDettaglio tabPrev = new PreventiviDettaglio();
				if (composizioni.size() > 0) {

					// aggiorno le placche
					for (int i = 0; i < composizioni.size(); i++) {
						ContentValues valComp = (ContentValues) composizioni.get(i);
						if (valComp.getAsString(CategorieComponenti.TIPO).equals(CategorieComponenti.TIPO_PORTAFRUTTI)) {
							tabPrev.aggiornaPlacche(db, val, valComp, idPreventivo, val.getAsInteger(ID_LOCALE), 1);
						}
					}

				} else {
					// AGGIUNGO L'ELEMENTO
					// prendo l'elemento per la descrizione
					ContentValues whereElem = new ContentValues();
					whereElem.put(Elementi.ID_ELEMENTO, val.getAsInteger(Elementi.ID_ELEMENTO));
					ContentValues valElem = db.getRecord(new Elementi(), whereElem);
					if (valElem != null) {
						val.put(Elementi.NOME_ELEMENTO, valElem.getAsString(Elementi.NOME_ELEMENTO));
					}
					tabPrev.aggiungiAPreventivo(db, idPreventivo, val);
				}*/
			}
		}
		super.inserimentoCorrelati(db, val);
	}

	/*@Override
	public int aggiornaRecord(DbInterno db, ContentValues val, ContentValues where) {
		// TODO Auto-generated method stub

		// prima di aggiornare il record controllo se � cambiata la gestione placche
		// Se prima era si e adesso � no allora tolgo una placca al preventivo
		// se prima era no eadesso � si allora aggiungo una placca al preventivo
		// Se nopn � cambiata non faccio niente
		if (val.containsKey(PLACCA_SN)) {
			ContentValues recordCheck = db.getRecord(this, where);

			if (recordCheck != null && recordCheck.getAsInteger(ID_PREVENTIVO) != 0) {
				int idPreventivo = recordCheck.getAsInteger(ID_PREVENTIVO);
				int idLocale = recordCheck.getAsInteger(ID_LOCALE);

				int placcaSN_old = recordCheck.getAsInteger(PLACCA_SN);
				int placcaSN_new = val.getAsInteger(PLACCA_SN);

				if (placcaSN_new != placcaSN_old) {
					ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
					ArrayList<Object> composizioni = tabComposizioni.getComposizioneElemento(db, where.getAsInteger(ID_ELEMENTO_CANT));
					if (composizioni.size() > 0) {
						PreventiviDettaglio tabPrev = new PreventiviDettaglio();

						for (int i = 0; i < composizioni.size(); i++) {
							ContentValues valComp = (ContentValues) composizioni.get(i);
							if (valComp.getAsString(CategorieComponenti.TIPO).equals(CategorieComponenti.TIPO_PORTAFRUTTI)) {
								// forzo a 1 per fare l'aggiornamento delle placche
								val.put(PLACCA_SN, 1);
								int qtaAgg = 1;
								if (placcaSN_old == 1 && placcaSN_new == 0) {
									qtaAgg = -1;
								}
								tabPrev.aggiornaPlacche(db, val, valComp, idPreventivo, idLocale, qtaAgg);
								// rimetto il valore corretto
								val.put(PLACCA_SN, placcaSN_new);
							}
						}

					}
				}
			}
		}
		return super.aggiornaRecord(db, val, where);
	}*/

	@Override
	public int cancellaRecord(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		ContentValues where = new ContentValues();
		where.put(ID_ELEMENTO_CANT, val.getAsInteger(ID_ELEMENTO_CANT));
		ContentValues recordCheck = db.getRecord(this, where);
		int result = super.cancellaRecord(db, val);
	/*	int idPreventivo = 0;
		int idLocale = 0;
		if (recordCheck != null) {
			idPreventivo = recordCheck.getAsInteger(ID_PREVENTIVO);
			idLocale = recordCheck.getAsInteger(ID_LOCALE);
			if (idLocale != 0 && idPreventivo != 0) {
				PreventiviDettaglio tabPrev = new PreventiviDettaglio();
				tabPrev.aggiornaTubiECaviLocale(db, idPreventivo, idLocale);
			}
		}*/
		return result;
	}

	@Override
	protected void aggiornamentoCorrelati(DbInterno db, ContentValues val, ContentValues where) {
		// TODO Auto-generated method stub

	/*	ContentValues recordCheck = db.getRecord(this, where);
		int idPreventivo = 0;
		int idLocale = 0;
		if (recordCheck != null) {
			idPreventivo = recordCheck.getAsInteger(ID_PREVENTIVO);
			idLocale = recordCheck.getAsInteger(ID_LOCALE);
			if (idLocale != 0 && idPreventivo != 0) {
				PreventiviDettaglio tabPrev = new PreventiviDettaglio();
				tabPrev.aggiornaTubiECaviLocale(db, idPreventivo, idLocale);
			}
		}*/
		super.aggiornamentoCorrelati(db, val, where);
	}

	@Override
	protected void eliminaCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		int idElementoCantiere = val.getAsInteger(ID_ELEMENTO_CANT);
		ContentValues where = new ContentValues();
		where.put(ID_ELEMENTO_CANT, idElementoCantiere);
		ContentValues recordCheck = db.getRecord(this, where);
		int idPreventivo = 0;
		int idLocale = 0;
		if (recordCheck != null) {
			idPreventivo = recordCheck.getAsInteger(ID_PREVENTIVO);
			idLocale = recordCheck.getAsInteger(ID_LOCALE);
		}

		ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
		ComponentiCantiere tabComponente = new ComponentiCantiere();
		PreventiviDettaglio tabPreventivoDett = new PreventiviDettaglio();

		ArrayList<Object> composizioni = tabComposizioni.getComposizioneElemento(db, idElementoCantiere);
		/*if (composizioni.size() > 0) {
			PreventiviDettaglio tabPrev = new PreventiviDettaglio();
			// aggiorno le placche
			if (recordCheck != null) {
				val.put(Elementi.PLACCA_SN, recordCheck.getAsInteger(PLACCA_SN));
				for (int i = 0; i < composizioni.size(); i++) {
					ContentValues valComp = (ContentValues) composizioni.get(i);
					if (valComp.getAsString(CategorieComponenti.TIPO).equals(CategorieComponenti.TIPO_PORTAFRUTTI)) {
						tabPrev.aggiornaPlacche(db, val, valComp, idPreventivo, idLocale, -1);
					}
				}
			}

		}*/
		for (int i = 0; i < composizioni.size(); i++) {
			ContentValues valcomposizione = (ContentValues) composizioni.get(i);

			// elimino la composizione
			tabComposizioni.cancellaRecord(db, valcomposizione);

			// elimino anche il componente
			tabComponente.cancellaRecord(db, valcomposizione);

			// Aggiorno il preventivo per ogni componente
			/*
			 * if (idPreventivo != 0) { if (recComponente != null) { recComponente.put(ID_LOCALE, idLocale);
			 * tabPreventivoDett.togliDaPreventivo(db, idPreventivo, recComponente, true); }
			 * 
			 * }
			 */

		}

		if (composizioni.size() == 0 && idPreventivo != 0) {
			// aggiorno il preventivo per l'elemento se non ci sono composizioni
			tabPreventivoDett.togliDaPreventivo(db, idPreventivo, recordCheck);

		}

		Collegamenti tabCollegamenti = new Collegamenti();
		String SQL = "Select * from " + Collegamenti.NOME_TABELLA + " where (" + Collegamenti.ID_ELEMENTO_CANT1 + " = "
				+ idElementoCantiere + " or " + Collegamenti.ID_ELEMENTO_CANT2 + " = " + idElementoCantiere + ") ";
		ArrayList<Object> collegamenti = db.eseguiSelect(SQL, null);
		for (int i = 0; i < collegamenti.size(); i++) {
			ContentValues valcoll = (ContentValues) collegamenti.get(i);
			tabCollegamenti.cancellaRecord(db, valcoll);
		}


        //elimino anche le relazioni
        Relazioni tabRelazioni = new Relazioni();
        String SQLREL = "Select * from " + Relazioni.NOME_TABELLA + " where (" + Relazioni.ID_ELEMENTO_CANT1 + " = "
                + idElementoCantiere + " or " + Relazioni.ID_ELEMENTO_CANT2 + " = " + idElementoCantiere + ") ";
        ArrayList<Object> relazioni = db.eseguiSelect(SQLREL, null);
        for (int i = 0; i < relazioni.size(); i++) {
            ContentValues valcoll = (ContentValues) relazioni.get(i);
            tabRelazioni.cancellaRecord(db, valcoll);
        }

		super.eliminaCorrelati(db, val);
	}


	/**
	 *
	 * @param db
	 * @param idPreventivoOrigine
	 * @param idPreventivoDestinazione
	 */
	public void copiaElementiPreventivo(DbInterno db, int idPreventivoOrigine, int idPreventivoDestinazione) {
		ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
		ComponentiCantiere tabComponenti = new ComponentiCantiere();
		ComponentiCantComposti tabComponentiComposti = new ComponentiCantComposti();

		ContentValues nuoviValori = new ContentValues();
		ContentValues where = new ContentValues();
		where.put(ID_PREVENTIVO, idPreventivoOrigine);


		ArrayList<Object> righe = db.eseguiSelect(this, where, null);
		for (int i = 0; i < righe.size(); i++) {
			ContentValues curr = (ContentValues) righe.get(i);
			nuoviValori.clear();
			nuoviValori.put(ID_PREVENTIVO, idPreventivoDestinazione);


			ContentValues recCopia = copiaRecord(db, curr, nuoviValori);

			// ATTENZIONE CHE IN FASE DI COPIA DEVO COPIARE LE COMPOSIZIONI NON DALLA TABELLA COMPOSIZIONI MA DA
			// COMPOSIZIONI_CANTIERE
			int idElementoCantiereOrigine = curr.getAsInteger(ID_ELEMENTO_CANT);
			where.clear();
			where.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantiereOrigine);
			ArrayList<Object> composizioni = db.eseguiSelect(tabComposizioni, where, null);

			for (int c = 0; c < composizioni.size(); c++) {
				ContentValues currCompos = (ContentValues) composizioni.get(c);
				int idComponenteCant = currCompos.getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT);
				where.clear();
				where.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponenteCant);
				ContentValues recComp = db.getRecord(tabComponenti, where);
				if (recComp != null) {
					// copio il componente
					ContentValues nuoviValoriComp = new ContentValues();

					if (recComp.getAsInteger(ComponentiCantiere.ID_PREVENTIVO)==0){
						nuoviValoriComp.put(ComponentiCantiere.ID_PREVENTIVO,0);
					}
					else{
						nuoviValoriComp.put(ComponentiCantiere.ID_PREVENTIVO,idPreventivoDestinazione);

					}
					ContentValues recCopiaComp = tabComponenti.copiaRecord(db, recComp, nuoviValoriComp);

					// se ci sono componenti composti copio anche quelli
					ArrayList<Object> composti = tabComponentiComposti.getComposizioneComponente(db, idComponenteCant);
					for (int cc = 0; cc < composti.size(); cc++) {
						ContentValues recFiglioCurr = (ContentValues) composti.get(cc);
						ContentValues whereFilglioCurr = new ContentValues();
						whereFilglioCurr.put(ComponentiCantiere.ID_COMPONENTE_CANT,
								recFiglioCurr.getAsInteger(ComponentiCantComposti.ID_COMPONENTE_CANT_FIGLIO));
						ContentValues recCompCurr = db.getRecord(tabComponenti, whereFilglioCurr);
						if (recCompCurr != null) {
							ContentValues recFiglioCurrCopia = tabComponenti.copiaRecord(db, recCompCurr, null);

							nuoviValori.clear();
							nuoviValori.put(ComponentiCantComposti.ID_COMPONENTE_CANT_PADRE,
									recCopiaComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
							nuoviValori.put(ComponentiCantComposti.ID_COMPONENTE_CANT_FIGLIO,
									recFiglioCurrCopia.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
							// copio la composizione del componente
							tabComponentiComposti.copiaRecord(db, recFiglioCurr, nuoviValori);
						}
					}

					nuoviValori.clear();
					nuoviValori.put(ComposizioniCantiere.ID_ELEMENTO_CANT, recCopia.getAsInteger(ID_ELEMENTO_CANT));
					nuoviValori.put(ComposizioniCantiere.ID_COMPONENTE_CANT,
							recCopiaComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
					// copio la composizione
					tabComposizioni.copiaRecord(db, currCompos, nuoviValori);

				}
			}

		}
	}
	/**
	 * 
	 * @param db
	 * @param idPreventivoOrigine
	 * @param idPreventivoDestinazione
	 * @param idElementoCantiere l'elemento da copiare (se zero copia tutti gli elementi del preventivo di partenza)
	 * @param conteggia se l'elemento copiato è da conteggiare o no nel preventivo/ordine
	 */
	public void copiaElementiPreventivo(DbInterno db, int idPreventivoOrigine, int idPreventivoDestinazione,int idElementoCantiere,boolean conteggia) {
		ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
		ComponentiCantiere tabComponenti = new ComponentiCantiere();
		ComponentiCantComposti tabComponentiComposti = new ComponentiCantComposti();

		ContentValues nuoviValori = new ContentValues();
		ContentValues where = new ContentValues();
		where.put(ID_PREVENTIVO, idPreventivoOrigine);
		if (idElementoCantiere!=0){
			where.put(ID_ELEMENTO_CANT,idElementoCantiere);
		}

		ArrayList<Object> righe = db.eseguiSelect(this, where, null);
		for (int i = 0; i < righe.size(); i++) {
			ContentValues curr = (ContentValues) righe.get(i);
			nuoviValori.clear();
			nuoviValori.put(ID_PREVENTIVO, idPreventivoDestinazione);
			if (idElementoCantiere!=0){
				nuoviValori.put(ID_ELEMENTO_CANT_ORIGINE,curr.getAsInteger(ID_ELEMENTO_CANT));
				nuoviValori.put(POS_X,curr.getAsInteger(POS_X)+5);
			}
			/*if (!conteggia){
				nuoviValori.put(NON_CONTEGGIARE_PREVENTIVI,1);
			}
			else{
				nuoviValori.put(NON_CONTEGGIARE_PREVENTIVI,0);
			}*/
			nuoviValori.put(NON_CONTEGGIARE_PREVENTIVI,0);
			ContentValues recCopia = copiaRecord(db, curr, nuoviValori);

			// ATTENZIONE CHE IN FASE DI COPIA DEVO COPIARE LE COMPOSIZIONI NON DALLA TABELLA COMPOSIZIONI MA DA
			// COMPOSIZIONI_CANTIERE
			int idElementoCantiereOrigine = curr.getAsInteger(ID_ELEMENTO_CANT);
			where.clear();
			where.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantiereOrigine);
			ArrayList<Object> composizioni = db.eseguiSelect(tabComposizioni, where, null);

			for (int c = 0; c < composizioni.size(); c++) {
				ContentValues currCompos = (ContentValues) composizioni.get(c);
				int idComponenteCant = currCompos.getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT);
				where.clear();
				where.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponenteCant);
				ContentValues recComp = db.getRecord(tabComponenti, where);
				if (recComp != null) {
					// copio il componente
					ContentValues nuoviValoriComp = new ContentValues();

					if (!conteggia){
						nuoviValoriComp.put(ComponentiCantiere.ID_PREVENTIVO,0);
						nuoviValoriComp.put(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI,1);
					}
					else{
						nuoviValoriComp.put(ComponentiCantiere.ID_PREVENTIVO,idPreventivoDestinazione);
						nuoviValoriComp.put(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI,0);
					}
					ContentValues recCopiaComp = tabComponenti.copiaRecord(db, recComp, nuoviValoriComp);

					// se ci sono componenti composti copio anche quelli
					ArrayList<Object> composti = tabComponentiComposti.getComposizioneComponente(db, idComponenteCant);
					for (int cc = 0; cc < composti.size(); cc++) {
						ContentValues recFiglioCurr = (ContentValues) composti.get(cc);
						ContentValues whereFilglioCurr = new ContentValues();
						whereFilglioCurr.put(ComponentiCantiere.ID_COMPONENTE_CANT,
								recFiglioCurr.getAsInteger(ComponentiCantComposti.ID_COMPONENTE_CANT_FIGLIO));
						ContentValues recCompCurr = db.getRecord(tabComponenti, whereFilglioCurr);
						if (recCompCurr != null) {
							ContentValues recFiglioCurrCopia = tabComponenti.copiaRecord(db, recCompCurr, null);

							nuoviValori.clear();
							nuoviValori.put(ComponentiCantComposti.ID_COMPONENTE_CANT_PADRE,
									recCopiaComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
							nuoviValori.put(ComponentiCantComposti.ID_COMPONENTE_CANT_FIGLIO,
									recFiglioCurrCopia.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
							// copio la composizione del componente
							tabComponentiComposti.copiaRecord(db, recFiglioCurr, nuoviValori);
						}
					}

					nuoviValori.clear();
					nuoviValori.put(ComposizioniCantiere.ID_ELEMENTO_CANT, recCopia.getAsInteger(ID_ELEMENTO_CANT));
					nuoviValori.put(ComposizioniCantiere.ID_COMPONENTE_CANT,
							recCopiaComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
					// copio la composizione
					tabComposizioni.copiaRecord(db, currCompos, nuoviValori);

				}
			}

		}

	}

	/**
	 * Prendo tutti gli elementi a cantiere o su ordine (esclusi gli elementi su preventivi)
	 * 
	 * @param db
	 * @param idLocale
	 * @return
	 */
	public ArrayList<Object> getElementiLocaleNoPreventivi(DbInterno db, int idLocale,boolean escludiUtilizzatori) {
		// TODO Auto-generated method stub
		Join j0 = new Join(NOME_TABELLA, Preventivi.NOME_TABELLA, Join.LEFT_JOIN);
		j0.addCampiDiJoin(ID_PREVENTIVO, Preventivi.ID_PREVENTIVO);

        Join j1 = new Join(NOME_TABELLA,Elementi.NOME_TABELLA);
        j1.addCampiDiJoin(ID_ELEMENTO,Elementi.ID_ELEMENTO);

        String SQL = "";

		if (escludiUtilizzatori){
            SQL = "Select " + getNomeCampoTabella("*") + " from " + NOME_TABELLA + j1.getSQLJoin() + j0.getSQLJoin()+ " where " + ID_LOCALE + "="
                    + idLocale + " and (" + Preventivi.TIPO + " is null or " + Preventivi.TIPO + "='" + Preventivi.TIPO_ORDINE + "') and " + Elementi.ID_CATEGORIA_GENERALE+"<>"+CategorieGenerali.UTILIZZATORI + " order by "
                    + ElementiCantiere.NUMERO_IDENTIFICATIVO;
        }
        else{
            SQL = "Select " + getNomeCampoTabella("*") + " from " + NOME_TABELLA + j0.getSQLJoin() + " where " + ID_LOCALE + "="
                    + idLocale + " and (" + Preventivi.TIPO + " is null or " + Preventivi.TIPO + "='" + Preventivi.TIPO_ORDINE + "') order by "
                    + ElementiCantiere.NUMERO_IDENTIFICATIVO;
        }



		ArrayList<Object> elementi = db.eseguiSelect(SQL, null);

		return elementi;
	}



	public void setIdElementoCantierePreferito(int idElementoCantierePreferito) {
		// TODO Auto-generated method stub
		this.idElementoCantierePreferito = idElementoCantierePreferito;
	}

	@Override
	protected int getMassimoNumeroRecordLicenzaGratis() {
		// TODO Auto-generated method stub
		return 140;
	}
}
