package pfa.app.econtab.db.table;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.utils.Sessione;

public class ComposizioniCantiere extends AbstractTable {
	public static final String NOME_TABELLA = "composizioni_cantiere";

	public static final String ID_ELEMENTO_CANT = "id_elemento_cant";
	public static final String ID_COMPONENTE_CANT = "id_componente_cant";
	public static final String POSIZIONE_INIZIALE = "posizione_iniziale";
	public static final String MODULI_OCCUPATI_CANT = "moduli_occupati_cant";

	public ComposizioniCantiere() {
		setNomeTabella(NOME_TABELLA);

		aggiungiCampo(ID_ELEMENTO_CANT, INTEGER);
		aggiungiCampo(ID_COMPONENTE_CANT, INTEGER);
		aggiungiCampo(POSIZIONE_INIZIALE, INTEGER);
		aggiungiCampo(MODULI_OCCUPATI_CANT, TEXT);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_ELEMENTO_CANT);
		aggiungiCampoChiave(ID_COMPONENTE_CANT);
	}

	@Override
	protected void inserimentoCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		ElementiCantiere tabElemCant = new ElementiCantiere();
		ContentValues where = new ContentValues();
		where.put(ElementiCantiere.ID_ELEMENTO_CANT, val.getAsInteger(ID_ELEMENTO_CANT));
		ContentValues recElemCant = db.getRecord(tabElemCant, where);
		if (recElemCant != null) {
			int idPreventivo = recElemCant.getAsInteger(ElementiCantiere.ID_PREVENTIVO);
			if (idPreventivo != 0) {
				PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();
				ContentValues whereComp = new ContentValues();
				whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, val.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
				ContentValues componente = db.getRecord(new ComponentiCantiere(), whereComp);
				componente.put(ElementiCantiere.ID_LOCALE, recElemCant.getAsInteger(ElementiCantiere.ID_LOCALE));
				tabPrevDett.aggiungiAPreventivo(db, idPreventivo, componente, true);


                //se è il primo componente allora verifico che non ci fosse l'elemento aggiunto al preventivo e in caso lo sostituisco
                ArrayList<Object> composizioni =  getComposizioneElemento(db,val.getAsInteger(ID_ELEMENTO_CANT));
                if (composizioni.size()==1){
                    tabPrevDett.togliDaPreventivo(db,idPreventivo,recElemCant);
                }

			}
		}

		// nelle installazioni server la composizione_cantiere deve viaggiare come un unico record
		// quindi aggiorno il campo in_server a false
		if (Sessione.isLicenzaBusiness(db.getContext())) {
			ContentValues whereElemento = new ContentValues();
			whereElemento.put(ID_ELEMENTO_CANT, val.getAsInteger(ID_ELEMENTO_CANT));

			ContentValues valUpd = new ContentValues();
			valUpd.put(IN_SERVER, 0);
			db.update(NOME_TABELLA, valUpd, whereElemento);
		}

		super.inserimentoCorrelati(db, val);
	}

	@Override
	protected void aggiornamentoCorrelati(DbInterno db, ContentValues val, ContentValues where) {

		// nelle installazioni server la composizione_cantiere deve viaggiare come un unico record
		// quindi aggiorno il campo in_server a false
		if (Sessione.isLicenzaBusiness(db.getContext())) {
			ContentValues whereElemento = new ContentValues();
			whereElemento.put(ID_ELEMENTO_CANT, where.getAsInteger(ID_ELEMENTO_CANT));

			ContentValues valUpd = new ContentValues();
			valUpd.put(IN_SERVER, 0);
			db.update(NOME_TABELLA, valUpd, whereElemento);
		}
		super.aggiornamentoCorrelati(db, val, where);
	}

	@Override
	protected void eliminaCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		int idPreventivo = 0;
		ElementiCantiere tabElemCant = new ElementiCantiere();
		ContentValues where = new ContentValues();
		where.put(ElementiCantiere.ID_ELEMENTO_CANT, val.getAsInteger(ID_ELEMENTO_CANT));
		ContentValues recElemCant = db.getRecord(tabElemCant, where);
		if (recElemCant != null) {
			idPreventivo = recElemCant.getAsInteger(ElementiCantiere.ID_PREVENTIVO);
		}

		if (val.containsKey(ID_COMPONENTE_CANT)) {
			int idComponenteCantiere = val.getAsInteger(ID_COMPONENTE_CANT);
			PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();
			if (idPreventivo != 0) {

				ContentValues whereComp = new ContentValues();
				whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponenteCantiere);
				ContentValues componente = db.getRecord(new ComponentiCantiere(), whereComp);
				componente.put(ElementiCantiere.ID_LOCALE, recElemCant.getAsInteger(ElementiCantiere.ID_LOCALE));
				if (componente != null) {

					componente.put(ElementiCantiere.ID_LOCALE, recElemCant.getAsInteger(ElementiCantiere.ID_LOCALE));
					tabPrevDett.eseguiAggiornamentoTubiECavi = false;
					tabPrevDett.togliDaPreventivo(db, idPreventivo, componente, true);
					tabPrevDett.eseguiAggiornamentoTubiECavi = true;
				}
			}

			ContentValues whereComp = new ContentValues();
			whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponenteCantiere);
			ComponentiCantiere tabComp = new ComponentiCantiere();
			tabComp.cancellaRecord(db, whereComp);

		/*	if (idPreventivo!=0){
				tabPrevDett.aggiornaTubiECaviLocale(db,idPreventivo,recElemCant.getAsInteger(ElementiCantiere.ID_LOCALE));
			}*/



		} else {
			ArrayList<Object> recs = db.eseguiSelect(this, val, null);
			ComponentiCantiere tabComp = new ComponentiCantiere();
			PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();
			for (int i = 0; i < recs.size(); i++) {
				if (idPreventivo != 0) {

					ContentValues whereComp = new ContentValues();
					whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, ((ContentValues) recs.get(i)).getAsInteger(ID_COMPONENTE_CANT));
					ContentValues componente = db.getRecord(new ComponentiCantiere(), whereComp);
					componente.put(ElementiCantiere.ID_LOCALE, recElemCant.getAsInteger(ElementiCantiere.ID_LOCALE));
					if (componente != null) {
						componente.put(ElementiCantiere.ID_LOCALE, recElemCant.getAsInteger(ElementiCantiere.ID_LOCALE));
						tabPrevDett.eseguiAggiornamentoTubiECavi = false;
						tabPrevDett.togliDaPreventivo(db, idPreventivo, componente, true);
						tabPrevDett.eseguiAggiornamentoTubiECavi = true;
					}
				}
				ContentValues whereComp = (ContentValues) recs.get(i);
				tabComp.cancellaRecord(db, whereComp);

			}
			/*if (idPreventivo != 0) {
				tabPrevDett.aggiornaTubiECaviLocale(db,idPreventivo,recElemCant.getAsInteger(ElementiCantiere.ID_LOCALE));
			}*/

		}
		super.eliminaCorrelati(db, val);
	}

	/**
	 * 
	 * Ritorna i record di composizione in join con i componenti per l'elemento passato
	 * 
	 * @param db
	 * @param idElemento
	 * @return
	 */
	public ArrayList<Object> getComposizioneElemento(DbInterno db, int idElemento) {

		Join j0 = new Join(NOME_TABELLA, ComponentiCantiere.NOME_TABELLA);
		j0.addCampiDiJoin(ID_COMPONENTE_CANT, ComponentiCantiere.ID_COMPONENTE_CANT);

		Join j1 = new Join(ComponentiCantiere.NOME_TABELLA, Componenti.NOME_TABELLA);
		j1.addCampiDiJoin(ComponentiCantiere.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j2 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
		j2.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);

		CategorieComponenti tabComp = new CategorieComponenti();

		String sql = "Select " + NOME_TABELLA + ".*," + Componenti.NOME_TABELLA + ".*," + ComponentiCantiere.NOME_COMPONENTE_CANT + ","
				+ ComponentiCantiere.NOTA + "," + tabComp.getNomeCampoTabella(CategorieComponenti.TIPO) + ","+ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI+"  from " + NOME_TABELLA
				+ j0.getSQLJoin() + j1.getSQLJoin() + j2.getSQLJoin() + " where " + ID_ELEMENTO_CANT + " = " + idElemento + " order by "
				+ POSIZIONE_INIZIALE+","+Componenti.ID_CATEGORIA_COMPONENTE;

		ArrayList<Object> result = db.eseguiSelect(sql, null);
		return result;
	}

	/**
	 * 
	 * Ritorna i record di composizione in join con i componentiCantiere per l'elemento passato
	 * 
	 * @param db
	 * @param idElemento
	 * @return
	 */
	public ArrayList<Object> getComposizioneElementoCantiere(DbInterno db, int idElemento) {

		Join j0 = new Join(NOME_TABELLA, ComponentiCantiere.NOME_TABELLA);
		j0.addCampiDiJoin(ID_COMPONENTE_CANT, ComponentiCantiere.ID_COMPONENTE_CANT);

		Join j1 = new Join(ComponentiCantiere.NOME_TABELLA, Componenti.NOME_TABELLA);
		j1.addCampiDiJoin(ComponentiCantiere.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j2 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
		j2.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);

		CategorieComponenti tabComp = new CategorieComponenti();

		String sql = "Select " + NOME_TABELLA + ".*," + ComponentiCantiere.NOME_TABELLA + ".*,"
				+ tabComp.getNomeCampoTabella(CategorieComponenti.TIPO) + " from " + NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin()
				+ j2.getSQLJoin() + " where " + ID_ELEMENTO_CANT + " = " + idElemento + " order by " + POSIZIONE_INIZIALE+","+Componenti.NOME_TABELLA+"."+Componenti.ID_CATEGORIA_COMPONENTE;

		ArrayList<Object> result = db.eseguiSelect(sql, null);
		return result;
	}

	public void eliminaFrutto(DbInterno db, ContentValues val, int idElementoIntent) {
		// TODO Auto-generated method stub
		// Cancello i tappi e/o gli eventuali componenti che occupavano la posizione
		int spazi_occupati = val.getAsInteger(ComponentiCantiere.SPAZI_OCCUPATI_CANT);
		int posizioneIniziale = val.getAsInteger(ComposizioniCantiere.POSIZIONE_INIZIALE);
		for (int i = 0; i < spazi_occupati; i++) {
			ContentValues whereDel = new ContentValues();
			whereDel.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoIntent);
			whereDel.put(ComposizioniCantiere.POSIZIONE_INIZIALE, posizioneIniziale + i);
			cancellaRecord(db, whereDel, false);
		}

		completaConTappi(db, idElementoIntent);
	}

	public void eliminaPortafrutti(DbInterno db, ContentValues val, int idElementoIntent) {
		// TODO Auto-generated method stub
		// Cancello i tappi e/o gli eventuali componenti che occupavano la posizione

			ContentValues whereDel = new ContentValues();
			whereDel.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoIntent);
			whereDel.put(ComposizioniCantiere.ID_COMPONENTE_CANT, val.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
			cancellaRecord(db, whereDel, true);


	}

	private void completaConTappi(DbInterno db, int idElementoIntent) {

		// Completo con tappi se ci sono dei buchi
		Join j0 = new Join(ComposizioniCantiere.NOME_TABELLA, ComponentiCantiere.NOME_TABELLA);
		j0.addCampiDiJoin(ComposizioniCantiere.ID_COMPONENTE_CANT, ComponentiCantiere.ID_COMPONENTE_CANT);

		Join j1 = new Join(ComponentiCantiere.NOME_TABELLA, Componenti.NOME_TABELLA);
		j1.addCampiDiJoin(ComponentiCantiere.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j2 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
		j2.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);

		ArrayList<Object> recs = db.eseguiSelect("Select " + ComponentiCantiere.SPAZI_OSPITATI_CANT + " from "
				+ ComposizioniCantiere.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where " + ComposizioniCantiere.ID_ELEMENTO_CANT
				+ "=" + idElementoIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "=" + Componenti.SCATOLE + " and " + ComposizioniCantiere.POSIZIONE_INIZIALE +"<100", null);
		if (recs.size() > 0) {
			ContentValues valScatolaOld = (ContentValues) recs.get(0);
			int spaziOspitatiScatola = valScatolaOld.getAsInteger(ComponentiCantiere.SPAZI_OSPITATI_CANT);
			//prendo solo quelli con posizione<100 (quelli con 100 sono i componenti aggiuntivi delle scatole)
			ArrayList<Object> frutti = db.eseguiSelect("Select " + ComponentiCantiere.SPAZI_OCCUPATI_CANT + ","
					+ ComposizioniCantiere.POSIZIONE_INIZIALE + "," + ComposizioniCantiere.MODULI_OCCUPATI_CANT + " from "
					+ ComposizioniCantiere.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + j2.getSQLJoin() + " where "
					+ ComposizioniCantiere.ID_ELEMENTO_CANT + "=" + idElementoIntent + " and " + CategorieComponenti.TIPO + "='"
					+ CategorieComponenti.TIPO_FRUTTO + "' and " + ComposizioniCantiere.POSIZIONE_INIZIALE +"<100", null);
			ArrayList<Integer> postiOccupati = new ArrayList<Integer>();
			for (int i = 0; i < frutti.size(); i++) {
				ContentValues frutto = (ContentValues) frutti.get(i);
				String postiFrutto = frutto.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
				String[] postiSplit = postiFrutto.split("/");
				for (int p = 0; p < postiSplit.length; p++) {
					postiOccupati.add(Integer.parseInt(postiSplit[p]));
				}

			}

			ContentValues whereTappo = new ContentValues();
			whereTappo.put(Componenti.TAPPO_SN, 1);
			ArrayList<Object> recsTappo = db.eseguiSelect(new Componenti(), whereTappo, new String[] { Componenti.ID_COMPONENTE });

			for (int i = 1; i <= spaziOspitatiScatola; i++) {
				if (!postiOccupati.contains(i)) {
					// inserisco il tappo
					if (recsTappo.size() > 0) {
						ContentValues valTAPPO = (ContentValues) recsTappo.get(0);

						// INSERISCO IL COMPONENTE
						ComponentiCantiere tabCompCant = new ComponentiCantiere();
						ContentValues valComp = tabCompCant.inserisciComponenteCantiere(db, valTAPPO);

						ContentValues valInsertTAPPO = getValoriLogInserimento(db);
						valInsertTAPPO.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoIntent);
						valInsertTAPPO.put(ComposizioniCantiere.ID_COMPONENTE_CANT,
								valComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
						valInsertTAPPO.put(ComposizioniCantiere.POSIZIONE_INIZIALE, i);
						valInsertTAPPO.put(ComposizioniCantiere.MODULI_OCCUPATI_CANT, "" + i);
						inserisciRecord(db, valInsertTAPPO);

					}
				}
			}

		}
	}

	private void inserisciTappi(DbInterno db, int spaziOspitatiNEW, int idElementoCantIntent, int posizioneIniziale) {
		// TODO Auto-generated method stub

		ContentValues whereTappo = new ContentValues();
		whereTappo.put(Componenti.TAPPO_SN, 1);
		ArrayList<Object> recsTappo = db.eseguiSelect(new Componenti(), whereTappo, new String[] { Componenti.ID_COMPONENTE });
		if (recsTappo.size() > 0) {
			ContentValues valTAPPO = (ContentValues) recsTappo.get(0);
			ComponentiCantiere tabCompCant = new ComponentiCantiere();
			for (int i = posizioneIniziale; i < spaziOspitatiNEW; i++) {

				ContentValues valComp = tabCompCant.inserisciComponenteCantiere(db, valTAPPO);

				ContentValues valInsertTAPPO = getValoriLogInserimento(db);
				valInsertTAPPO.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
				valInsertTAPPO.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
				valInsertTAPPO.put(ComposizioniCantiere.POSIZIONE_INIZIALE, i + 1);
				valInsertTAPPO.put(ComposizioniCantiere.MODULI_OCCUPATI_CANT, "" + (i + 1));
				inserisciRecord(db, valInsertTAPPO);
			}

		}
	}

	public void sostituisciScatola(ContentValues val, int idElementoCantIntent, DbInterno db, boolean portafrutti,boolean senzaPortafrutti, boolean copriscatola) {
		// TODO Auto-generated method stub
		ContentValues whereElem = new ContentValues();
		whereElem.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);

		int spaziOspitatiNEW = val.getAsInteger(Componenti.SPAZI_OSPITATI);


		// Elimino le vecchie composizioni se devo sostituire il portafrutti o il copriscatola
		if (portafrutti || copriscatola || senzaPortafrutti){
			ContentValues where = new ContentValues();
			where.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);

			cancellaRecord(db, where, false);

			// inserisco la scatola
			ComponentiCantiere tabCompCant = new ComponentiCantiere();
			ContentValues valComp = tabCompCant.inserisciComponenteCantiere(db, val);

			ContentValues valInsert = getValoriLogInserimento(db);
			valInsert.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
			valInsert.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
			inserisciRecord(db, valInsert);

			if (portafrutti == true || senzaPortafrutti==true) {

				// Prendo il primo portafrutti che abbia lo stesso numero di
				// moduli ospitati della scatola
				if (portafrutti){
					ContentValues wherePortafrutti = new ContentValues();
					wherePortafrutti.put(Componenti.ID_CATEGORIA_COMPONENTE, Componenti.PORTAFRUTTI);
					wherePortafrutti.put(Componenti.SPAZI_OSPITATI, spaziOspitatiNEW);
					ArrayList<Object> recsPortafrutti = db.eseguiSelect(new Componenti(), wherePortafrutti,
							new String[] { Componenti.ID_COMPONENTE });

					if (recsPortafrutti.size() > 0) {

						ContentValues valPF = (ContentValues) recsPortafrutti.get(0);

						ContentValues valCompPF = tabCompCant.inserisciComponenteCantiere(db, valPF);

						ContentValues valInsertPF = getValoriLogInserimento(db);
						valInsertPF.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
						valInsertPF.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valCompPF.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
						inserisciRecord(db, valInsertPF);
					} else {
						aggiungiPortafruttiDefault(db, val, idElementoCantIntent);
					}
				}


				inserisciTappi(db, spaziOspitatiNEW, idElementoCantIntent, 0);



			}

			if (copriscatola == true) {

				// Prendo il primo copriscatola che abbia lo stesso numero di
				// moduli ospitati della scatola
				ContentValues whereCopriscatola = new ContentValues();
				whereCopriscatola.put(Componenti.ID_CATEGORIA_COMPONENTE, Componenti.COPRISCATOLA);
				whereCopriscatola.put(Componenti.SPAZI_OCCUPATI, spaziOspitatiNEW);
				ArrayList<Object> recsCopriscatola = db.eseguiSelect(new Componenti(), whereCopriscatola,
						new String[] { Componenti.ID_COMPONENTE });
				if (recsCopriscatola.size() > 0) {
					ContentValues valCS = (ContentValues) recsCopriscatola.get(0);

					ContentValues valCompCS = tabCompCant.inserisciComponenteCantiere(db, valCS);

					ContentValues valInsertCS = getValoriLogInserimento(db);
					valInsertCS.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
					valInsertCS.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valCompCS.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
					inserisciRecord(db, valInsertCS);
				} else {
					aggiungiCopriscatolaDefault(db, val, idElementoCantIntent);
				}
			}
		}

		else{
			//sostituisco solamente il componente scatola
			//prendo il componente di tipo scatola
			ArrayList<Object> componenti =  getComposizioneElementoCantiere(db,idElementoCantIntent);
			int idCompScatola = 0;
			for (int i=0;i<componenti.size();i++){
				ContentValues curr = (ContentValues)componenti.get(i);
				if (curr.getAsString(CategorieComponenti.TIPO).equals(CategorieComponenti.TIPO_SCATOLA) && curr.getAsInteger(ComposizioniCantiere.POSIZIONE_INIZIALE)<100){
					idCompScatola = curr.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT);
					break;
				}
			}

			Relazioni tabRelazioni = new Relazioni();
			Collegamenti tabCollegamenti = new Collegamenti();
			ArrayList<Object> collegamentiEsistenti = tabCollegamenti.getCollegamentiComponente(db, idCompScatola);
			ArrayList<Object> relazioniEsistenti = tabRelazioni.getComponentiCollegatiComponente(db,idCompScatola);



			ContentValues where = new ContentValues();
			where.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
			where.put(ComposizioniCantiere.ID_COMPONENTE_CANT,idCompScatola);

			cancellaRecord(db, where, false);

			// inserisco la scatola
			ComponentiCantiere tabCompCant = new ComponentiCantiere();
			ContentValues valComp = tabCompCant.inserisciComponenteCantiere(db, val);

			ContentValues valInsert = getValoriLogInserimento(db);
			valInsert.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
			valInsert.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
			inserisciRecord(db, valInsert);

			// aggiorno i collegamenti
			for (int i = 0; i < collegamentiEsistenti.size(); i++) {
				ContentValues valRel = (ContentValues) collegamentiEsistenti.get(i);
				ContentValues valUPD = tabCollegamenti.getValoriLogModifica(db);
				if (idCompScatola == valRel.getAsInteger(Collegamenti.ID_COMPONENTE_CANT1)) {
					valUPD.put(Collegamenti.ID_COMPONENTE_CANT1, valComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
				}
				if (idCompScatola == valRel.getAsInteger(Collegamenti.ID_COMPONENTE_CANT2)) {
					valUPD.put(Collegamenti.ID_COMPONENTE_CANT2, valComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
				}
				ContentValues whereRel = new ContentValues();
				whereRel.put(Collegamenti.ID_COLLEGAMENTO, valRel.getAsInteger(Collegamenti.ID_COLLEGAMENTO));
				db.update(Collegamenti.NOME_TABELLA, valUPD, whereRel);

			}

			// aggiorno le relazioni
			for (int i = 0; i < relazioniEsistenti.size(); i++) {
				ContentValues valRel = (ContentValues) relazioniEsistenti.get(i);
				ContentValues valUPD = tabRelazioni.getValoriLogModifica(db);
				if (idCompScatola ==  valRel.getAsInteger(Relazioni.ID_COMPONENTE_CANT1)) {
					valUPD.put(Relazioni.ID_COMPONENTE_CANT1, valComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
				}
				if (idCompScatola == valRel.getAsInteger(Relazioni.ID_COMPONENTE_CANT2)) {
					valUPD.put(Relazioni.ID_COMPONENTE_CANT2, valComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
				}
				ContentValues whereRel = new ContentValues();
				whereRel.put(Relazioni.ID_RELAZIONE, valRel.getAsInteger(Relazioni.ID_RELAZIONE));

				db.update(Relazioni.NOME_TABELLA, valUPD, whereRel);

			}
		}





	}

	public void sostituisciCopriscatola(DbInterno db, ContentValues val, int idElementoCantIntent) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		// Elimino l'eventuale copriscatola

		ContentValues whereElem = new ContentValues();
		whereElem.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
		ContentValues recordCheck = db.getRecord(new ElementiCantiere(), whereElem);
		int idPreventivo = 0;
		int idLocale = 0;
		if (recordCheck != null) {
			idPreventivo = recordCheck.getAsInteger(ElementiCantiere.ID_PREVENTIVO);
			idLocale = recordCheck.getAsInteger(ElementiCantiere.ID_LOCALE);
		}

		Join j0 = new Join(ComposizioniCantiere.NOME_TABELLA, ComponentiCantiere.NOME_TABELLA);
		j0.addCampiDiJoin(ComposizioniCantiere.ID_COMPONENTE_CANT, ComponentiCantiere.ID_COMPONENTE_CANT);

		Join j1 = new Join(ComponentiCantiere.NOME_TABELLA, Componenti.NOME_TABELLA);
		j1.addCampiDiJoin(ComponentiCantiere.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j2 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
		j2.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);
		ArrayList<Object> recsCS = db.eseguiSelect("Select " + getNomeCampoTabella(ID_COMPONENTE_CANT) + " from "
				+ ComposizioniCantiere.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where " + ComposizioniCantiere.ID_ELEMENTO_CANT
				+ "=" + idElementoCantIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "=" + Componenti.COPRISCATOLA+" and "+ComposizioniCantiere.POSIZIONE_INIZIALE+"<100", null);
		for (int i = 0; i < recsCS.size(); i++) {
			ContentValues recCS = (ContentValues) recsCS.get(i);
			ContentValues wherecs = new ContentValues();
			wherecs.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
			wherecs.put(ComposizioniCantiere.ID_COMPONENTE_CANT, recCS.getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT));
			cancellaRecord(db, wherecs);
		}

		// Elimino l'eventuale portafrutti
		ArrayList<Object> recsPF = db.eseguiSelect("Select " + getNomeCampoTabella(ID_COMPONENTE_CANT) + "," + Componenti.SPAZI_OSPITATI
				+ " from " + ComposizioniCantiere.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where "
				+ ComposizioniCantiere.ID_ELEMENTO_CANT + "=" + idElementoCantIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "="
				+ Componenti.PORTAFRUTTI+" and "+ComposizioniCantiere.POSIZIONE_INIZIALE+"<100", null);
		for (int i = 0; i < recsPF.size(); i++) {
			ContentValues recPF = (ContentValues) recsPF.get(i);
			// se sto modificando un elemento a preventivo allora ricalcolo le placche
		/*	PreventiviDettaglio tabPrev = new PreventiviDettaglio();
			val.put(Elementi.PLACCA_SN, recordCheck.getAsInteger(ElementiCantiere.PLACCA_SN));
			tabPrev.aggiornaPlacche(db, val, recPF, idPreventivo, idLocale, -1);
*/
			ContentValues wherecs = new ContentValues();
			wherecs.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
			wherecs.put(ComposizioniCantiere.ID_COMPONENTE_CANT, recPF.getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT));
			cancellaRecord(db, wherecs);

		}

		ArrayList<Object> frutti = db.eseguiSelect("Select " + getNomeCampoTabella(ID_COMPONENTE_CANT) + " from "
				+ ComposizioniCantiere.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + j2.getSQLJoin() + " where "
				+ ComposizioniCantiere.ID_ELEMENTO_CANT + "=" + idElementoCantIntent + " and " + CategorieComponenti.TIPO + "='"
				+ CategorieComponenti.TIPO_FRUTTO + "' and " + ComposizioniCantiere.POSIZIONE_INIZIALE+"<100", null);

		for (int i = 0; i < frutti.size(); i++) {
			ContentValues frutto = (ContentValues) frutti.get(i);
			ContentValues wherecs = new ContentValues();
			wherecs.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
			wherecs.put(ComposizioniCantiere.ID_COMPONENTE_CANT, frutto.getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT));

			cancellaRecord(db, wherecs);
		}

		// inserisco il copriscatola
		ComponentiCantiere tabCompCant = new ComponentiCantiere();
		ContentValues valCompCS = tabCompCant.inserisciComponenteCantiere(db, val);

		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
		valInsertPF.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valCompCS.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
		inserisciRecord(db, valInsertPF);

	}

	public void sostituisciPortafrutti(DbInterno db, ContentValues val, int idElementoCantIntent) {

		ContentValues whereElem = new ContentValues();
		whereElem.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
		ContentValues recordCheck = db.getRecord(new ElementiCantiere(), whereElem);
		int idPreventivo = 0;
		int idLocale = 0;
		if (recordCheck != null) {
			idPreventivo = recordCheck.getAsInteger(ElementiCantiere.ID_PREVENTIVO);
			idLocale = recordCheck.getAsInteger(ElementiCantiere.ID_LOCALE);
		}

		// Elimino l'eventuale copriscatola
		Join j0 = new Join(ComposizioniCantiere.NOME_TABELLA, ComponentiCantiere.NOME_TABELLA);
		j0.addCampiDiJoin(ComposizioniCantiere.ID_COMPONENTE_CANT, ComponentiCantiere.ID_COMPONENTE_CANT);

		Join j1 = new Join(ComponentiCantiere.NOME_TABELLA, Componenti.NOME_TABELLA);
		j1.addCampiDiJoin(ComponentiCantiere.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j2 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
		j2.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);
		ArrayList<Object> recsCS = db.eseguiSelect("Select " + getNomeCampoTabella(ID_COMPONENTE_CANT) + " from "
				+ ComposizioniCantiere.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where " + ComposizioniCantiere.ID_ELEMENTO_CANT
				+ "=" + idElementoCantIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "=" + Componenti.COPRISCATOLA+" and " + ComposizioniCantiere.POSIZIONE_INIZIALE+"<100", null);
		for (int i = 0; i < recsCS.size(); i++) {
			ContentValues recCS = (ContentValues) recsCS.get(i);
			ContentValues wherecs = new ContentValues();
			wherecs.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
			wherecs.put(ComposizioniCantiere.ID_COMPONENTE_CANT, recCS.getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT));
			cancellaRecord(db, wherecs);
		}

		// Elimino l'eventuale portafrutti
		ArrayList<Object> recsPF = db.eseguiSelect("Select " + getNomeCampoTabella(ID_COMPONENTE_CANT) + "," + Componenti.SPAZI_OSPITATI
				+ " from " + ComposizioniCantiere.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where "
				+ ComposizioniCantiere.ID_ELEMENTO_CANT + "=" + idElementoCantIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "="
				+ Componenti.PORTAFRUTTI+" and "+ComposizioniCantiere.POSIZIONE_INIZIALE+"<100", null);
		for (int i = 0; i < recsPF.size(); i++) {
			ContentValues recPF = (ContentValues) recsPF.get(i);

			// se sto modificando un elemento a preventivo allora ricalcolo le placche
		/*	PreventiviDettaglio tabPrev = new PreventiviDettaglio();
			val.put(Elementi.PLACCA_SN, recordCheck.getAsInteger(ElementiCantiere.PLACCA_SN));
			tabPrev.aggiornaPlacche(db, val, recPF, idPreventivo, idLocale, -1);
*/
			ContentValues wherecs = new ContentValues();
			wherecs.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
			wherecs.put(ComposizioniCantiere.ID_COMPONENTE_CANT, recPF.getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT));
			cancellaRecord(db, wherecs);
		}

		// inserisco il portafrutti
		ComponentiCantiere tabCompCant = new ComponentiCantiere();
		ContentValues valCompCS = tabCompCant.inserisciComponenteCantiere(db, val);

		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
		valInsertPF.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valCompCS.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
		inserisciRecord(db, valInsertPF);

		// se sto modificando un elemento a preventivo allora ricalcolo le placche
	/*	PreventiviDettaglio tabPrev = new PreventiviDettaglio();
		val.put(Elementi.PLACCA_SN, recordCheck.getAsInteger(ElementiCantiere.PLACCA_SN));
		valCompCS.put(Componenti.SPAZI_OSPITATI, valCompCS.getAsInteger(ComponentiCantiere.SPAZI_OSPITATI_CANT));
		tabPrev.aggiornaPlacche(db, val, valCompCS, idPreventivo, idLocale, 1);
*/
		completaConTappi(db, idElementoCantIntent);
	}

	public void aggiungiFrutto(DbInterno db, ContentValues val, int idElementoCantIntent, int posizioneIniziale) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		Join j0 = new Join(ComposizioniCantiere.NOME_TABELLA, ComponentiCantiere.NOME_TABELLA);
		j0.addCampiDiJoin(ComposizioniCantiere.ID_COMPONENTE_CANT, ComponentiCantiere.ID_COMPONENTE_CANT);

		Join j1 = new Join(ComponentiCantiere.NOME_TABELLA, Componenti.NOME_TABELLA);
		j1.addCampiDiJoin(ComponentiCantiere.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j2 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
		j2.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);

		ArrayList<Object> recs = db.eseguiSelect("Select " + getNomeCampoTabella(ComposizioniCantiere.ID_COMPONENTE_CANT) + ","
				+ ComponentiCantiere.SPAZI_OSPITATI_CANT + " from " + ComposizioniCantiere.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin()
				+ " where " + ComposizioniCantiere.ID_ELEMENTO_CANT + "=" + idElementoCantIntent + " and "
				+ Componenti.ID_CATEGORIA_COMPONENTE + "=" + Componenti.SCATOLE, null);
		if (recs.size() > 0) {
			ContentValues valScatolaOld = (ContentValues) recs.get(0);
			int spaziOspitatiScatola = valScatolaOld.getAsInteger(ComponentiCantiere.SPAZI_OSPITATI_CANT);
			// Cancello i tappi e/o gli eventuali componenti che occupavano la posizione
			int spazi_occupati = val.getAsInteger(Componenti.SPAZI_OCCUPATI);
			ArrayList<Object> collegamentiEsistenti = new ArrayList<Object>();
			ArrayList<Object> relazioniEsistenti = new ArrayList<Object>();
			ArrayList<Integer> componentiCancellati = new ArrayList<Integer>();
			Collegamenti tabCollegamenti = new Collegamenti();
			Relazioni tabRelazioni = new Relazioni();
			for (int i = 0; i < spazi_occupati; i++) {
				ContentValues whereDel = new ContentValues();
				whereDel.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
				whereDel.put(ComposizioniCantiere.POSIZIONE_INIZIALE, posizioneIniziale + i);

				ArrayList<Object> vals = db.eseguiSelect(this, whereDel, null);
				for (int j = 0; j < vals.size(); j++) {
					int idComponenteCant = ((ContentValues) vals.get(j)).getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT);
					collegamentiEsistenti.addAll(tabCollegamenti.getCollegamentiComponente(db, idComponenteCant));
					relazioniEsistenti.addAll(tabRelazioni.getComponentiCollegatiComponente(db,idComponenteCant));
					componentiCancellati.add(idComponenteCant);
					cancellaRecord(db, (ContentValues) vals.get(j));
				}

			}
			// Se il componente che sto insrerndo eccede gli spazi della scatola non lo inserisco
			if (posizioneIniziale + spazi_occupati <= spaziOspitatiScatola + 1) {
				ComponentiCantiere tabCompCant = new ComponentiCantiere();
				ContentValues valCompCS = tabCompCant.inserisciComponenteCantiere(db, val);

				ContentValues valInsert = getValoriLogInserimento(db);
				valInsert.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
				valInsert.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valCompCS.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
				valInsert.put(ComposizioniCantiere.POSIZIONE_INIZIALE, posizioneIniziale);

				String moduli_occupati = getModuliOccupati(posizioneIniziale, spazi_occupati);
				valInsert.put(ComposizioniCantiere.MODULI_OCCUPATI_CANT, moduli_occupati);
				inserisciRecord(db, valInsert);

				// aggiorno i collegamenti
				for (int i = 0; i < collegamentiEsistenti.size(); i++) {
					ContentValues valRel = (ContentValues) collegamentiEsistenti.get(i);
					ContentValues valUPD = tabCollegamenti.getValoriLogModifica(db);
					if (componentiCancellati.contains(valRel.getAsInteger(Collegamenti.ID_COMPONENTE_CANT1))) {
						valUPD.put(Collegamenti.ID_COMPONENTE_CANT1, valCompCS.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
					}
					if (componentiCancellati.contains(valRel.getAsInteger(Collegamenti.ID_COMPONENTE_CANT2))) {
						valUPD.put(Collegamenti.ID_COMPONENTE_CANT2, valCompCS.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
					}
					ContentValues whereRel = new ContentValues();
					whereRel.put(Collegamenti.ID_COLLEGAMENTO, valRel.getAsInteger(Collegamenti.ID_COLLEGAMENTO));
					// Non � necessario eseguire l'aggiornamento del preventivo perch� modifico solamente i componenti
					// collegati quindi eseguo il metodo update anzich� aggiornaRecord standard
					db.update(Collegamenti.NOME_TABELLA, valUPD, whereRel);

				}

				// aggiorno le relazioni
				for (int i = 0; i < relazioniEsistenti.size(); i++) {
					ContentValues valRel = (ContentValues) relazioniEsistenti.get(i);
					ContentValues valUPD = tabRelazioni.getValoriLogModifica(db);
					if (componentiCancellati.contains(valRel.getAsInteger(Relazioni.ID_COMPONENTE_CANT1))) {
						valUPD.put(Relazioni.ID_COMPONENTE_CANT1, valCompCS.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
					}
					if (componentiCancellati.contains(valRel.getAsInteger(Relazioni.ID_COMPONENTE_CANT2))) {
						valUPD.put(Relazioni.ID_COMPONENTE_CANT2, valCompCS.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
					}
					ContentValues whereRel = new ContentValues();
					whereRel.put(Relazioni.ID_RELAZIONE, valRel.getAsInteger(Relazioni.ID_RELAZIONE));

					db.update(Relazioni.NOME_TABELLA, valUPD, whereRel);

				}

			}
			// Completo con tappi se ci sono dei buchi
			completaConTappi(db, idElementoCantIntent);
		}
	}



	public static String getModuliOccupati(int posizioneIniziale, double spazi_occupati) {
		String moduli_occupati = "";

		for (int i = 0; i < spazi_occupati; i++) {
			if (i == 0) {
				moduli_occupati = "" + posizioneIniziale;
			} else {
				moduli_occupati = moduli_occupati + "/" + (posizioneIniziale + i);
			}
		}
		return moduli_occupati;
	}

	public void aggiungiCentralinoQuadro(DbInterno db, ContentValues val, int idElementoCantIntent) {
		// TODO Auto-generated method stub
		// Elimino la vecchia composizione

		ContentValues wherecs = new ContentValues();
		wherecs.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
		cancellaRecord(db, wherecs, false);

		ComponentiCantiere tabCompCant = new ComponentiCantiere();
		ContentValues valCompCS = tabCompCant.inserisciComponenteCantiere(db, val);

		// inserisco il centralino
		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
		valInsertPF.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valCompCS.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
		inserisciRecord(db, valInsertPF);

	}

	public void sostituisciCentralinoQuadro(DbInterno db, ContentValues val, int idElementoCantIntent) {
		// TODO Auto-generated method stub

		//sostituisco solamente il componente centralino
		//prendo il componente di tipo centralino
		ArrayList<Object> componenti =  getComposizioneElementoCantiere(db,idElementoCantIntent);
		int idCompCentralino = 0;
		for (int i=0;i<componenti.size();i++){
			ContentValues curr = (ContentValues)componenti.get(i);
			if (curr.getAsString(CategorieComponenti.TIPO).equals(CategorieComponenti.TIPO_CENTRALINO)){
				idCompCentralino = curr.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT);
				break;
			}
		}

		ContentValues wherecs = new ContentValues();
		wherecs.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
		wherecs.put(ComposizioniCantiere.ID_COMPONENTE_CANT,idCompCentralino);
		cancellaRecord(db, wherecs, false);

		ComponentiCantiere tabCompCant = new ComponentiCantiere();
		ContentValues valCompCS = tabCompCant.inserisciComponenteCantiere(db, val);

		// inserisco il centralino
		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
		valInsertPF.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valCompCS.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
		inserisciRecord(db, valInsertPF);

	}

	public void aggiungiComponenteQuadro(DbInterno db, ContentValues val, int idElementoCantIntent) {
		// TODO Auto-generated method stub
		ComponentiCantiere tabCompCant = new ComponentiCantiere();
		ContentValues valCompCS = tabCompCant.inserisciComponenteCantiere(db, val);

		double spaziOccupatiCant = valCompCS.getAsDouble(ComponentiCantiere.SPAZI_OCCUPATI_CANT);
		ContentValues where = new ContentValues();
		where.put(ID_ELEMENTO_CANT, idElementoCantIntent);
		ArrayList<Object> componentiOrdinati = db.eseguiSelect(this, where, new String[] { POSIZIONE_INIZIALE + " desc" });
		int posizioneIniziale = 1;
		if (componentiOrdinati.size() > 0) {
			ContentValues recPosMax = (ContentValues) componentiOrdinati.get(0);
			if (recPosMax.getAsInteger(POSIZIONE_INIZIALE) > 0) {
				ContentValues whereComp = new ContentValues();
				whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT,recPosMax.getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT));
				ContentValues valComp = db.getRecord(new ComponentiCantiere(),whereComp);
				double spaziOccupatiComp = 0;
				if (valComp!=null){
					spaziOccupatiComp = valComp.getAsDouble(ComponentiCantiere.SPAZI_OCCUPATI_CANT);
				}
				//posizioneIniziale = recPosMax.getAsInteger(POSIZIONE_INIZIALE) + (int) (spaziOccupatiComp);
				posizioneIniziale = recPosMax.getAsInteger(POSIZIONE_INIZIALE) + 1;

			}
		}

		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
		valInsertPF.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valCompCS.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
		valInsertPF.put(ComposizioniCantiere.POSIZIONE_INIZIALE, posizioneIniziale);
		//String moduliOccupati = getModuliOccupati(posizioneIniziale, valCompCS.getAsDouble(ComponentiCantiere.SPAZI_OCCUPATI_CANT));
		valInsertPF.put(ComposizioniCantiere.MODULI_OCCUPATI_CANT, posizioneIniziale);
		inserisciRecord(db, valInsertPF);
	}

	public void aggiungiComponenteLibero(DbInterno db, ContentValues val, int idElementoCantIntent,boolean componente_aggiuntivo_scatola) {
		// TODO Auto-generated method stub

		ComponentiCantiere tabCompCant = new ComponentiCantiere();
		ContentValues valCompCS = tabCompCant.inserisciComponenteCantiere(db, val);

		// double spaziOccupatiCant = valCompCS.getAsDouble(ComponentiCantiere.SPAZI_OCCUPATI_CANT);
		int posizioneIniziale = 1;
		if (componente_aggiuntivo_scatola){
			 posizioneIniziale = 100;
		}
		else{
			double spaziOccupatiCant = 1;
			ContentValues where = new ContentValues();
			where.put(ID_ELEMENTO_CANT, idElementoCantIntent);
			ArrayList<Object> componentiOrdinati = db.eseguiSelect(this, where, new String[] { POSIZIONE_INIZIALE + " desc" });

			if (componentiOrdinati.size() > 0) {
				ContentValues recPosMax = (ContentValues) componentiOrdinati.get(0);
				if (recPosMax.getAsInteger(POSIZIONE_INIZIALE) > 0) {
					posizioneIniziale = recPosMax.getAsInteger(POSIZIONE_INIZIALE) + (int) spaziOccupatiCant;
				}
			}
		}


		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
		valInsertPF.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valCompCS.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
		valInsertPF.put(ComposizioniCantiere.POSIZIONE_INIZIALE, posizioneIniziale);
		// String moduliOccupati = getModuliOccupati(posizioneIniziale,
		// valCompCS.getAsDouble(ComponentiCantiere.SPAZI_OCCUPATI_CANT));
		//valInsertPF.put(ComposizioniCantiere.MODULI_OCCUPATI_CANT, posizioneIniziale);
		inserisciRecord(db, valInsertPF);




	}

	/**
	 * Aggiungo sulla tabella componenti il portafrutto che copre gli spazi necessari e lo utilizzo subito nella
	 * composizione della scatola
	 * 
	 * @param db
	 * @param val
	 * @param idElementoIntent
	 */
	private void aggiungiCopriscatolaDefault(DbInterno db, ContentValues val, int idElementoIntent) {
		Componenti tabComponenti = new Componenti();

		ContentValues pfDefault = tabComponenti.getValoriLogInserimento(db);
		pfDefault.put(Componenti.ID_CATEGORIA_COMPONENTE, Componenti.COPRISCATOLA);
		pfDefault.put(Componenti.NOME_COMPONENTE,
				db.getResources().getString(R.string.coperchio) + " " + val.getAsInteger(Componenti.SPAZI_OSPITATI) + "M");
		pfDefault.put(Componenti.SPAZI_OCCUPATI, val.getAsInteger(Componenti.SPAZI_OSPITATI));
		pfDefault.put(Componenti.ICONA, "coperchio_scatole.png");
		pfDefault.put(Componenti.PREFERITO_SN, 1);
		pfDefault.put(Componenti.UNITA_MISURA, "PZ");

		tabComponenti.inserisciRecord(db, pfDefault);

		ComponentiCantiere tabCompCant = new ComponentiCantiere();
		ContentValues valComp = tabCompCant.inserisciComponenteCantiere(db, pfDefault);

		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoIntent);
		valInsertPF.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
		inserisciRecord(db, valInsertPF);
	}

	/**
	 * Aggiungo sulla tabella componenti il portafrutto che copre gli spazi necessari e lo utilizzo subito nella
	 * composizione della scatola
	 * 
	 * @param db
	 * @param val
     */
	private void aggiungiPortafruttiDefault(DbInterno db, ContentValues val, int idElementoCantIntent) {
		Componenti tabComponenti = new Componenti();

		ContentValues pfDefault = tabComponenti.getValoriLogInserimento(db);
		pfDefault.put(Componenti.ID_CATEGORIA_COMPONENTE, Componenti.PORTAFRUTTI);
		pfDefault.put(Componenti.NOME_COMPONENTE,
				db.getResources().getString(R.string.portaelementi) + " " + val.getAsInteger(Componenti.SPAZI_OSPITATI) + "M");
		pfDefault.put(Componenti.SPAZI_OSPITATI, val.getAsInteger(Componenti.SPAZI_OSPITATI));
		pfDefault.put(Componenti.ICONA, "portaelementi.png");
		pfDefault.put(Componenti.PREFERITO_SN, 1);
		pfDefault.put(Componenti.UNITA_MISURA, "PZ");

		tabComponenti.inserisciRecord(db, pfDefault);

		ComponentiCantiere tabCompCant = new ComponentiCantiere();
		ContentValues valComp = tabCompCant.inserisciComponenteCantiere(db, pfDefault);

		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCantIntent);
		valInsertPF.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
		inserisciRecord(db, valInsertPF);
	}



	public void spostaComponente(DbInterno db, ContentValues val, int pos, boolean composizionelibera,boolean quadro) {
		// TODO Auto-generated method stub
		int posCurr = val.getAsInteger(ComposizioniCantiere.POSIZIONE_INIZIALE);
		int posNew = posCurr + pos;
		if (posNew > 0) {
			// prendo il componente precednete/successivo

			String SQL = "";
			if (pos == 1) {
				SQL = "Select * from " + NOME_TABELLA + " where " + ID_ELEMENTO_CANT + "="
						+ val.getAsInteger(ComposizioniCantiere.ID_ELEMENTO_CANT) + " and " + POSIZIONE_INIZIALE + ">=" + posNew
						+ " order by " + POSIZIONE_INIZIALE + " asc";
			} else {
				SQL = "Select * from " + NOME_TABELLA + " where " + ID_ELEMENTO_CANT + "="
						+ val.getAsInteger(ComposizioniCantiere.ID_ELEMENTO_CANT) + " and " + POSIZIONE_INIZIALE + "<=" + posNew
						+ " order by " + POSIZIONE_INIZIALE + " desc";
			}

			ArrayList<Object> res = db.eseguiSelect(SQL, null);
			ContentValues valPos = null;
			if (res.size() > 0) {
				valPos = (ContentValues) res.get(0);
			}

			if (valPos != null) {

				ContentValues whereComp1 = new ContentValues();
				whereComp1.put(ComponentiCantiere.ID_COMPONENTE_CANT, valPos.getAsInteger(ID_COMPONENTE_CANT));
				ContentValues recComp1 = db.getRecord(new ComponentiCantiere(), whereComp1);
				double spazi_occupati1 = 1d;
				if (recComp1 != null) {
					spazi_occupati1 = recComp1.getAsDouble(ComponentiCantiere.SPAZI_OCCUPATI_CANT);
				}

				ContentValues whereComp2 = new ContentValues();
				whereComp2.put(ComponentiCantiere.ID_COMPONENTE_CANT, val.getAsInteger(ID_COMPONENTE_CANT));
				ContentValues recComp2 = db.getRecord(new ComponentiCantiere(), whereComp2);
				double spazi_occupati2 = 1d;
				if (recComp2 != null) {
					spazi_occupati2 = recComp2.getAsDouble(ComponentiCantiere.SPAZI_OCCUPATI_CANT);
				}

				if (composizionelibera == true) {
					spazi_occupati1 = 1;
					spazi_occupati2 = 1;
				}

				int pos1 = posCurr;
				int pos2 = posNew;

				int posIni1 = valPos.getAsInteger(POSIZIONE_INIZIALE);
				if (spazi_occupati1 == spazi_occupati2) {

					pos1 = posCurr;
					pos2 = posIni1;

				} else {
					if (pos == 1) {
						pos1 = posCurr;
						pos2 = posCurr + (int) spazi_occupati1;
					} else {
						pos1 = posIni1 + (int) spazi_occupati2;
						pos2 = posIni1;

					}
				}

				ContentValues valUpd1 = getValoriLogModifica(db);
				valUpd1.put(ComposizioniCantiere.POSIZIONE_INIZIALE, pos1);
				if (!composizionelibera) {
					valUpd1.put(ComposizioniCantiere.MODULI_OCCUPATI_CANT, getModuliOccupati(pos1, spazi_occupati1));
				}
				else{
					valUpd1.put(ComposizioniCantiere.MODULI_OCCUPATI_CANT, pos1);
				}

				ContentValues where1 = new ContentValues();
				where1.put(ComposizioniCantiere.ID_ELEMENTO_CANT, valPos.getAsInteger(ComposizioniCantiere.ID_ELEMENTO_CANT));
				where1.put(ComposizioniCantiere.ID_COMPONENTE_CANT, valPos.getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT));
				aggiornaRecord(db, valUpd1, where1);

				ContentValues valUpd2 = getValoriLogModifica(db);
				valUpd2.put(ComposizioniCantiere.POSIZIONE_INIZIALE, pos2);
				if (!composizionelibera) {
					valUpd2.put(ComposizioniCantiere.MODULI_OCCUPATI_CANT, getModuliOccupati(pos2, spazi_occupati2));
				}
				else{
					if (quadro){
						valUpd2.put(ComposizioniCantiere.MODULI_OCCUPATI_CANT, pos2);
					}

				}
				ContentValues where2 = new ContentValues();
				where2.put(ComposizioniCantiere.ID_ELEMENTO_CANT, val.getAsInteger(ComposizioniCantiere.ID_ELEMENTO_CANT));
				where2.put(ComposizioniCantiere.ID_COMPONENTE_CANT, val.getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT));
				aggiornaRecord(db, valUpd2, where2);
			}

		}
	}

	public ArrayList<Object> getComponentiQuadroOrdinati(DbInterno db, int idElemento) {
		// TODO Auto-generated method stub
		Join j0 = new Join(NOME_TABELLA, ComponentiCantiere.NOME_TABELLA);
		j0.addCampiDiJoin(ID_COMPONENTE_CANT, ComponentiCantiere.ID_COMPONENTE_CANT);

		Join j1 = new Join(ComponentiCantiere.NOME_TABELLA, Componenti.NOME_TABELLA);
		j1.addCampiDiJoin(ComponentiCantiere.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j2 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
		j2.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);

		CategorieComponenti tabComp = new CategorieComponenti();

		String sql = "Select " + NOME_TABELLA + ".*," + ComponentiCantiere.NOME_TABELLA + ".*,"
				+ tabComp.getNomeCampoTabella(CategorieComponenti.TIPO) + " from " + NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin()
				+ j2.getSQLJoin() + " where " + ID_ELEMENTO_CANT + " = " + idElemento + " and "
				+ tabComp.getNomeCampoTabella(CategorieComponenti.ID_CATEGORIA_COMPONENTE) + "=" + Componenti.COMPONENTI_QUADRI
				+ " order by " + POSIZIONE_INIZIALE;

		ArrayList<Object> result = db.eseguiSelect(sql, null);
		return result;
	}

	public ContentValues getElementoCantiereDaComponente(DbInterno db, int idComponenteCantiere) {
		Join j0 = new Join(NOME_TABELLA, ElementiCantiere.NOME_TABELLA);
		j0.addCampiDiJoin(ID_ELEMENTO_CANT, ElementiCantiere.ID_ELEMENTO_CANT);

		String SQL = "Select " + ElementiCantiere.NOME_TABELLA + ".* from " + NOME_TABELLA + j0.getSQLJoin() + " where "
				+ ID_COMPONENTE_CANT + "=" + idComponenteCantiere;
		ArrayList<Object> lista = db.eseguiSelect(SQL, null);
		if (lista != null && lista.size() > 0) {
			return (ContentValues) lista.get(0);
		}

		return null;
	}

}
