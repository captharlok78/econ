package pfa.app.econtab.db.table;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;

public class Composizioni extends AbstractTable {
	public static final String NOME_TABELLA = "composizioni";

	public static final String ID_COMPOSIZIONE = "id_composizione";
	public static final String ID_ELEMENTO = "id_elemento";
	public static final String ID_COMPONENTE = "id_componente";
	public static final String POSIZIONE_INIZIALE = "posizione_iniziale";
	public static final String MODULI_OCCUPATI = "moduli_occupati";

	public Composizioni() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_COMPOSIZIONE);

		aggiungiCampo(ID_COMPOSIZIONE, INTEGER);
		aggiungiCampo(ID_ELEMENTO, INTEGER);
		aggiungiCampo(ID_COMPONENTE, INTEGER);
		aggiungiCampo(POSIZIONE_INIZIALE, INTEGER);
		aggiungiCampo(MODULI_OCCUPATI, TEXT);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_COMPOSIZIONE);
	}

	/**
	 * Inserisce la scatola identificata dal val nella tabella Composizioni Inserisce il portaFrutti e i tappi se
	 * richiesto, altrimenti inserisce il copriscatola.
	 * 
	 * @param val
	 * @param idElementoIntent
	 * @param db
	 */
	public void componiScatola(ContentValues val, int idElementoIntent, DbInterno db, boolean portafrutti, boolean senzaPortafrutti, boolean copriscatola) {
		// Cancello la vecchia scatola e il vecchio portafrutti se esistono
		Join j0 = new Join(Composizioni.NOME_TABELLA, Componenti.NOME_TABELLA);
		j0.addCampiDiJoin(Composizioni.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		ArrayList<Object> recs = db.eseguiSelect(
				"Select " + Composizioni.ID_COMPOSIZIONE + " from " + Composizioni.NOME_TABELLA + j0.getSQLJoin() + " where "
						+ Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "="
						+ Componenti.SCATOLE, null);
		if (recs != null && recs.size() > 0) {
			ContentValues valScatolaOld = (ContentValues) recs.get(0);
			ContentValues where = new ContentValues();
			where.put(Composizioni.ID_COMPOSIZIONE, valScatolaOld.getAsInteger(Composizioni.ID_COMPOSIZIONE));
			// db.delete(Composizioni.NOME_TABELLA, where);
			cancellaRecord(db, where, false);
		}

		if (portafrutti == true || senzaPortafrutti==true) {
			ArrayList<Object> recs2 = db.eseguiSelect(
					"Select " + Composizioni.ID_COMPOSIZIONE + " from " + Composizioni.NOME_TABELLA + j0.getSQLJoin() + " where "
							+ Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "="
							+ Componenti.PORTAFRUTTI, null);
			if (recs2 != null && recs2.size() > 0) {
				ContentValues valScatolaOld = (ContentValues) recs2.get(0);
				ContentValues where = new ContentValues();
				where.put(Composizioni.ID_COMPOSIZIONE, valScatolaOld.getAsInteger(Composizioni.ID_COMPOSIZIONE));
				// db.delete(Composizioni.NOME_TABELLA, where);
				cancellaRecord(db, where, false);
			}
		}

		if (copriscatola == true) {
			ArrayList<Object> recs3 = db.eseguiSelect(
					"Select " + Composizioni.ID_COMPOSIZIONE + " from " + Composizioni.NOME_TABELLA + j0.getSQLJoin() + " where "
							+ Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "="
							+ Componenti.COPRISCATOLA, null);
			if (recs3 != null && recs3.size() > 0) {
				ContentValues valScatolaOld = (ContentValues) recs3.get(0);
				ContentValues where = new ContentValues();
				where.put(Composizioni.ID_COMPOSIZIONE, valScatolaOld.getAsInteger(Composizioni.ID_COMPOSIZIONE));
				// db.delete(Composizioni.NOME_TABELLA, where);
				cancellaRecord(db, where, false);
			}
		}

		// inserisco la scatola

		ContentValues valInsert = getValoriLogInserimento(db);
		valInsert.put(Composizioni.ID_ELEMENTO, idElementoIntent);
		valInsert.put(Composizioni.ID_COMPONENTE, val.getAsInteger(Componenti.ID_COMPONENTE));
		inserisciRecord(db, valInsert);

		// inserisco il portafrutti e i tappi
		if (portafrutti == true || senzaPortafrutti==true) {
			// Prendo il primo portafrutti che abbia lo stesso numero di
			// moduli ospitati della scatola
			if (portafrutti){
				ContentValues wherePortafrutti = new ContentValues();
				wherePortafrutti.put(Componenti.ID_CATEGORIA_COMPONENTE, Componenti.PORTAFRUTTI);
				wherePortafrutti.put(Componenti.SPAZI_OSPITATI, val.getAsInteger(Componenti.SPAZI_OSPITATI));
				ArrayList<Object> recsPortafrutti = db.eseguiSelect(new Componenti(), wherePortafrutti,
						new String[] { Componenti.ID_COMPONENTE });
				if (recsPortafrutti.size() > 0) {
					ContentValues valPF = (ContentValues) recsPortafrutti.get(0);
					ContentValues valInsertPF = getValoriLogInserimento(db);
					valInsertPF.put(Composizioni.ID_ELEMENTO, idElementoIntent);
					valInsertPF.put(Composizioni.ID_COMPONENTE, valPF.getAsInteger(Componenti.ID_COMPONENTE));
					inserisciRecord(db, valInsertPF);
				}

				// Aggiungo un portafrutti di default con il numero moduli della
				// scatola
				else {
					aggiungiPortafruttiDefault(db, val, idElementoIntent);

				}
			}

			int spaziOspitati = val.getAsInteger(Componenti.SPAZI_OSPITATI);
			// inserisco i tappi
			inserisciTappi(db, spaziOspitati, idElementoIntent, 0);

		}

		// inserisco il copriscatola
		if (copriscatola == true) {
			// Prendo il primo copriscatola che abbia lo stesso numero di
			// moduli ospitati della scatola
			ContentValues whereCopriscatola = new ContentValues();
			whereCopriscatola.put(Componenti.ID_CATEGORIA_COMPONENTE, Componenti.COPRISCATOLA);
			whereCopriscatola.put(Componenti.SPAZI_OCCUPATI, val.getAsInteger(Componenti.SPAZI_OSPITATI));
			ArrayList<Object> recsCopriscatola = db.eseguiSelect(new Componenti(), whereCopriscatola,
					new String[] { Componenti.ID_COMPONENTE });
			if (recsCopriscatola.size() > 0) {
				ContentValues valCS = (ContentValues) recsCopriscatola.get(0);
				ContentValues valInsertCS = getValoriLogInserimento(db);
				valInsertCS.put(Composizioni.ID_ELEMENTO, idElementoIntent);
				valInsertCS.put(Composizioni.ID_COMPONENTE, valCS.getAsInteger(Componenti.ID_COMPONENTE));
				inserisciRecord(db, valInsertCS);
			} else {
				aggiungiCopriscatolaDefault(db, val, idElementoIntent);
			}
		}
	}

	public void sostituisciScatola(ContentValues val, int idElementoIntent, DbInterno db, boolean portafrutti, boolean senzaPortafrutti, boolean copriscatola) {
		// Cancello la vecchia scatola e il vecchio portafrutti se esistono
		Join j0 = new Join(Composizioni.NOME_TABELLA, Componenti.NOME_TABELLA);
		j0.addCampiDiJoin(Composizioni.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j1 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
		j1.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);

		int spaziOspitatiOLD = 0;
		int spaziOspitatiNEW = val.getAsInteger(Componenti.SPAZI_OSPITATI);

		ArrayList<Object> recs = db.eseguiSelect("Select " + Composizioni.ID_COMPOSIZIONE + "," + Componenti.SPAZI_OSPITATI + " from "
				+ Composizioni.NOME_TABELLA + j0.getSQLJoin() + " where " + Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and "
				+ Componenti.ID_CATEGORIA_COMPONENTE + "=" + Componenti.SCATOLE, null);
		if (recs != null && recs.size() > 0) {
			ContentValues valScatolaOld = (ContentValues) recs.get(0);
			spaziOspitatiOLD = valScatolaOld.getAsInteger(Componenti.SPAZI_OSPITATI);
			ContentValues where = new ContentValues();
			where.put(Composizioni.ID_COMPOSIZIONE, valScatolaOld.getAsInteger(Composizioni.ID_COMPOSIZIONE));

			ContentValues valUpdate = getValoriLogModifica(db);
			valUpdate.put(Composizioni.ID_COMPONENTE, val.getAsInteger(Componenti.ID_COMPONENTE));
			aggiornaRecord(db, valUpdate, where);

		}

		ArrayList<Object> frutti = db.eseguiSelect("Select " + Composizioni.ID_COMPOSIZIONE + "," + Componenti.SPAZI_OCCUPATI + ","
				+ Composizioni.POSIZIONE_INIZIALE + " from " + Composizioni.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where "
				+ Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and " + CategorieComponenti.TIPO + "='"
				+ CategorieComponenti.TIPO_FRUTTO + "'", null);

		// Elimino l'eventuale copriscatola
		ArrayList<Object> recsCS = db.eseguiSelect(
				"Select " + Composizioni.ID_COMPOSIZIONE + " from " + Composizioni.NOME_TABELLA + j0.getSQLJoin() + " where "
						+ Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "="
						+ Componenti.COPRISCATOLA, null);
		for (int i = 0; i < recsCS.size(); i++) {
			ContentValues recCS = (ContentValues) recsCS.get(i);
			ContentValues wherecs = new ContentValues();
			wherecs.put(Composizioni.ID_COMPOSIZIONE, recCS.getAsInteger(Composizioni.ID_COMPOSIZIONE));
			// db.delete(Composizioni.NOME_TABELLA, wherecs);
			cancellaRecord(db, wherecs, false);
		}

		// Elimino l'eventuale portafrutti
		ArrayList<Object> recsPF = db.eseguiSelect(
				"Select " + Composizioni.ID_COMPOSIZIONE + " from " + Composizioni.NOME_TABELLA + j0.getSQLJoin() + " where "
						+ Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "="
						+ Componenti.PORTAFRUTTI, null);
		for (int i = 0; i < recsPF.size(); i++) {
			ContentValues recPF = (ContentValues) recsPF.get(i);
			ContentValues wherecs = new ContentValues();
			wherecs.put(Composizioni.ID_COMPOSIZIONE, recPF.getAsInteger(Composizioni.ID_COMPOSIZIONE));
			// db.delete(Composizioni.NOME_TABELLA, wherecs);
			cancellaRecord(db, wherecs, false);
		}

		if (portafrutti == true || senzaPortafrutti==true) {

			// Prendo il primo portafrutti che abbia lo stesso numero di
			// moduli ospitati della scatola
			if (portafrutti){
				ContentValues wherePortafrutti = new ContentValues();
				wherePortafrutti.put(Componenti.ID_CATEGORIA_COMPONENTE, Componenti.PORTAFRUTTI);
				wherePortafrutti.put(Componenti.SPAZI_OSPITATI, spaziOspitatiNEW);
				ArrayList<Object> recsPortafrutti = db.eseguiSelect(new Componenti(), wherePortafrutti,
						new String[] { Componenti.ID_COMPONENTE });

				ArrayList<Object> recs2 = db.eseguiSelect(
						"Select " + Composizioni.ID_COMPOSIZIONE + " from " + Composizioni.NOME_TABELLA + j0.getSQLJoin() + " where "
								+ Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "="
								+ Componenti.PORTAFRUTTI, null);
				if (recs2.size() > 0) {
					ContentValues valScatolaOld = (ContentValues) recs2.get(0);
					ContentValues where = new ContentValues();
					where.put(Composizioni.ID_COMPOSIZIONE, valScatolaOld.getAsInteger(Composizioni.ID_COMPOSIZIONE));
					if (recsPortafrutti.size() > 0) {
						ContentValues valPF = (ContentValues) recsPortafrutti.get(0);
						ContentValues valUpdate = getValoriLogModifica(db);
						valUpdate.put(Composizioni.ID_COMPONENTE, valPF.getAsInteger(Componenti.ID_COMPONENTE));
						aggiornaRecord(db, valUpdate, where);

					} else {
						// db.delete(Composizioni.NOME_TABELLA, where);
						cancellaRecord(db, where, false);
						aggiungiPortafruttiDefault(db, val, idElementoIntent);
					}

				} else {
					if (recsPortafrutti.size() > 0) {

						ContentValues valPF = (ContentValues) recsPortafrutti.get(0);
						ContentValues valInsertPF = getValoriLogInserimento(db);
						valInsertPF.put(Composizioni.ID_ELEMENTO, idElementoIntent);
						valInsertPF.put(Composizioni.ID_COMPONENTE, valPF.getAsInteger(Componenti.ID_COMPONENTE));
						inserisciRecord(db, valInsertPF);
					} else {
						aggiungiPortafruttiDefault(db, val, idElementoIntent);
					}
				}
			}


			// 1)Se Prima era un copriscatola: inserisco tutti i tappi
			if (frutti.size() == 0) {
				inserisciTappi(db, spaziOspitatiNEW, idElementoIntent, 0);
			} else {
				// 2) Prima aveva meno spazi
				if (spaziOspitatiNEW > spaziOspitatiOLD) {
					inserisciTappi(db, spaziOspitatiNEW, idElementoIntent, spaziOspitatiOLD);
				}
				// 3) Prima aveva pi� spazi
				if (spaziOspitatiNEW < spaziOspitatiOLD) {

					for (int i = 0; i < frutti.size(); i++) {
						ContentValues frutto = (ContentValues) frutti.get(i);
						int posizione_iniziale = frutto.getAsInteger(Composizioni.POSIZIONE_INIZIALE);

						int spazi_occupati = frutto.getAsInteger(Componenti.SPAZI_OCCUPATI);
						// tolgo 1 dalla posizione perch� inizio a contare le
						// posizioni da 1
						if (posizione_iniziale - 1 + spazi_occupati > spaziOspitatiNEW) {
							// elimino il record di composizione
							ContentValues where = new ContentValues();
							where.put(Composizioni.ID_COMPOSIZIONE, frutto.getAsInteger(Composizioni.ID_COMPOSIZIONE));
							// db.delete(Composizioni.NOME_TABELLA, where);
							cancellaRecord(db, where, false);
						}
					}
				}
			}
		}

		if (copriscatola == true) {
			// elimino tutti i frutti
			for (int i = 0; i < frutti.size(); i++) {
				ContentValues frutto = (ContentValues) frutti.get(i);
				ContentValues where = new ContentValues();
				where.put(Composizioni.ID_COMPOSIZIONE, frutto.getAsInteger(Composizioni.ID_COMPOSIZIONE));
				// db.delete(Composizioni.NOME_TABELLA, where);
				cancellaRecord(db, where, false);
			}

			// Prendo il primo copriscatola che abbia lo stesso numero di
			// moduli ospitati della scatola
			ContentValues whereCopriscatola = new ContentValues();
			whereCopriscatola.put(Componenti.ID_CATEGORIA_COMPONENTE, Componenti.COPRISCATOLA);
			whereCopriscatola.put(Componenti.SPAZI_OCCUPATI, val.getAsInteger(Componenti.SPAZI_OSPITATI));
			ArrayList<Object> recsCopriscatola = db.eseguiSelect(new Componenti(), whereCopriscatola,
					new String[] { Componenti.ID_COMPONENTE });
			if (recsCopriscatola.size() > 0) {
				ContentValues valCS = (ContentValues) recsCopriscatola.get(0);
				ContentValues valInsertCS = getValoriLogInserimento(db);
				valInsertCS.put(Composizioni.ID_ELEMENTO, idElementoIntent);
				valInsertCS.put(Composizioni.ID_COMPONENTE, valCS.getAsInteger(Componenti.ID_COMPONENTE));
				inserisciRecord(db, valInsertCS);
			} else {
				aggiungiCopriscatolaDefault(db, val, idElementoIntent);
			}
		}

	}

	private void inserisciTappi(DbInterno db, int spaziOspitatiNEW, int idElementoIntent, int posizioneIniziale) {
		// TODO Auto-generated method stub

		ContentValues whereTappo = new ContentValues();
		whereTappo.put(Componenti.TAPPO_SN, 1);
		ArrayList<Object> recsTappo = db.eseguiSelect(new Componenti(), whereTappo, new String[] { Componenti.ID_COMPONENTE });
		if (recsTappo.size() > 0) {
			ContentValues valTAPPO = (ContentValues) recsTappo.get(0);

			for (int i = posizioneIniziale; i < spaziOspitatiNEW; i++) {
				ContentValues valInsertTAPPO = getValoriLogInserimento(db);
				valInsertTAPPO.put(Composizioni.ID_ELEMENTO, idElementoIntent);
				valInsertTAPPO.put(Composizioni.ID_COMPONENTE, valTAPPO.getAsInteger(Componenti.ID_COMPONENTE));
				valInsertTAPPO.put(Composizioni.POSIZIONE_INIZIALE, i + 1);
				valInsertTAPPO.put(Composizioni.MODULI_OCCUPATI, "" + (i + 1));
				inserisciRecord(db, valInsertTAPPO);
			}

		}
	}

	/**
	 * Aggiungo sulla tabella componenti il portafrutto che copre gli spazi necessari e lo utilizzo subito nella
	 * composizione della scatola
	 * 
	 * @param db
	 * @param val
	 * @param idElementoIntent
	 */
	private void aggiungiPortafruttiDefault(DbInterno db, ContentValues val, int idElementoIntent) {
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

		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(Composizioni.ID_ELEMENTO, idElementoIntent);
		valInsertPF.put(Composizioni.ID_COMPONENTE, pfDefault.getAsInteger(Componenti.ID_COMPONENTE));
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

		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(Composizioni.ID_ELEMENTO, idElementoIntent);
		valInsertPF.put(Composizioni.ID_COMPONENTE, pfDefault.getAsInteger(Componenti.ID_COMPONENTE));
		inserisciRecord(db, valInsertPF);
	}

	public void aggiungiFrutto(DbInterno db, ContentValues val, int idElementoIntent, int posizioneIniziale) {
		// TODO Auto-generated method stub
		Join j0 = new Join(Composizioni.NOME_TABELLA, Componenti.NOME_TABELLA);
		j0.addCampiDiJoin(Composizioni.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j1 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
		j1.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);

		ArrayList<Object> recs = db.eseguiSelect("Select " + Composizioni.ID_COMPOSIZIONE + "," + Componenti.SPAZI_OSPITATI + " from "
				+ Composizioni.NOME_TABELLA + j0.getSQLJoin() + " where " + Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and "
				+ Componenti.ID_CATEGORIA_COMPONENTE + "=" + Componenti.SCATOLE, null);
		if (recs.size() > 0) {
			ContentValues valScatolaOld = (ContentValues) recs.get(0);
			int spaziOspitatiScatola = valScatolaOld.getAsInteger(Componenti.SPAZI_OSPITATI);
			// Cancello i tappi e/o gli eventuali componenti che occupavano la posizione
			int spazi_occupati = val.getAsInteger(Componenti.SPAZI_OCCUPATI);
			for (int i = 0; i < spazi_occupati; i++) {
				ContentValues whereDel = new ContentValues();
				whereDel.put(Composizioni.ID_ELEMENTO, idElementoIntent);
				whereDel.put(Composizioni.POSIZIONE_INIZIALE, posizioneIniziale + i);
				// db.delete(Composizioni.NOME_TABELLA, whereDel);
				cancellaRecord(db, whereDel, false);
			}

			ContentValues valInsert = getValoriLogInserimento(db);
			valInsert.put(Composizioni.ID_ELEMENTO, idElementoIntent);
			valInsert.put(Composizioni.ID_COMPONENTE, val.getAsInteger(Componenti.ID_COMPONENTE));
			valInsert.put(Composizioni.POSIZIONE_INIZIALE, posizioneIniziale);

			// Se il componente che sto insrerndo eccede gli spazi della scatola non lo inserisco
			if (posizioneIniziale + spazi_occupati <= spaziOspitatiScatola + 1) {
				String moduli_occupati = getModuliOccupati(posizioneIniziale, spazi_occupati);
				valInsert.put(Composizioni.MODULI_OCCUPATI, moduli_occupati);
				inserisciRecord(db, valInsert);
			}
			// Completo con tappi se ci sono dei buchi
			completaConTappi(db, idElementoIntent);
		}

	}

	private void completaConTappi(DbInterno db, int idElementoIntent) {

		// Completo con tappi se ci sono dei buchi
		Join j0 = new Join(Composizioni.NOME_TABELLA, Componenti.NOME_TABELLA);
		j0.addCampiDiJoin(Composizioni.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j1 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
		j1.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);

		ArrayList<Object> recs = db.eseguiSelect("Select " + Composizioni.ID_COMPOSIZIONE + "," + Componenti.SPAZI_OSPITATI + " from "
				+ Composizioni.NOME_TABELLA + j0.getSQLJoin() + " where " + Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and "
				+ Componenti.ID_CATEGORIA_COMPONENTE + "=" + Componenti.SCATOLE, null);
		if (recs.size() > 0) {
			ContentValues valScatolaOld = (ContentValues) recs.get(0);
			int spaziOspitatiScatola = valScatolaOld.getAsInteger(Componenti.SPAZI_OSPITATI);
			ArrayList<Object> frutti = db.eseguiSelect(
					"Select " + Composizioni.ID_COMPOSIZIONE + "," + Componenti.SPAZI_OCCUPATI + "," + Composizioni.POSIZIONE_INIZIALE
							+ "," + Composizioni.MODULI_OCCUPATI + " from " + Composizioni.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin()
							+ " where " + Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and " + CategorieComponenti.TIPO + "='"
							+ CategorieComponenti.TIPO_FRUTTO + "'", null);
			ArrayList<Integer> postiOccupati = new ArrayList<Integer>();
			for (int i = 0; i < frutti.size(); i++) {
				ContentValues frutto = (ContentValues) frutti.get(i);
				String postiFrutto = frutto.getAsString(Composizioni.MODULI_OCCUPATI);
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

						ContentValues valInsertTAPPO = getValoriLogInserimento(db);
						valInsertTAPPO.put(Composizioni.ID_ELEMENTO, idElementoIntent);
						valInsertTAPPO.put(Composizioni.ID_COMPONENTE, valTAPPO.getAsInteger(Componenti.ID_COMPONENTE));
						valInsertTAPPO.put(Composizioni.POSIZIONE_INIZIALE, i);
						valInsertTAPPO.put(Composizioni.MODULI_OCCUPATI, "" + i);
						inserisciRecord(db, valInsertTAPPO);

					}
				}
			}

		}
	}

	public void eliminaFrutto(DbInterno db, ContentValues val, int idElementoIntent) {
		// TODO Auto-generated method stub
		// Cancello i tappi e/o gli eventuali componenti che occupavano la posizione
		int spazi_occupati = val.getAsInteger(Componenti.SPAZI_OCCUPATI);
		int posizioneIniziale = val.getAsInteger(Composizioni.POSIZIONE_INIZIALE);
		for (int i = 0; i < spazi_occupati; i++) {
			ContentValues whereDel = new ContentValues();
			whereDel.put(Composizioni.ID_ELEMENTO, idElementoIntent);
			whereDel.put(Composizioni.POSIZIONE_INIZIALE, posizioneIniziale + i);
			cancellaRecord(db, whereDel, false);
		}

		completaConTappi(db, idElementoIntent);
	}

	public void eliminaPortafrutti(DbInterno db, ContentValues val, int idElementoIntent) {

		ContentValues whereDel = new ContentValues();
		whereDel.put(Composizioni.ID_ELEMENTO, idElementoIntent);
		whereDel.put(Composizioni.ID_COMPONENTE, val.getAsInteger(Componenti.ID_COMPONENTE));
		cancellaRecord(db, whereDel, true);

	}

	public void sostituisciCopriscatola(DbInterno db, ContentValues val, int idElementoIntent) {
		// TODO Auto-generated method stub
		// Elimino l'eventuale copriscatola

		Join j0 = new Join(Composizioni.NOME_TABELLA, Componenti.NOME_TABELLA);
		j0.addCampiDiJoin(Composizioni.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j1 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
		j1.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);
		ArrayList<Object> recsCS = db.eseguiSelect(
				"Select " + Composizioni.ID_COMPOSIZIONE + " from " + Composizioni.NOME_TABELLA + j0.getSQLJoin() + " where "
						+ Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "="
						+ Componenti.COPRISCATOLA, null);
		for (int i = 0; i < recsCS.size(); i++) {
			ContentValues recCS = (ContentValues) recsCS.get(i);
			ContentValues wherecs = new ContentValues();
			wherecs.put(Composizioni.ID_COMPOSIZIONE, recCS.getAsInteger(Composizioni.ID_COMPOSIZIONE));
			// db.delete(Composizioni.NOME_TABELLA, wherecs);
			cancellaRecord(db, wherecs, false);
		}

		// Elimino l'eventuale portafrutti
		ArrayList<Object> recsPF = db.eseguiSelect(
				"Select " + Composizioni.ID_COMPOSIZIONE + " from " + Composizioni.NOME_TABELLA + j0.getSQLJoin() + " where "
						+ Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "="
						+ Componenti.PORTAFRUTTI, null);
		for (int i = 0; i < recsPF.size(); i++) {
			ContentValues recPF = (ContentValues) recsPF.get(i);
			ContentValues wherecs = new ContentValues();
			wherecs.put(Composizioni.ID_COMPOSIZIONE, recPF.getAsInteger(Composizioni.ID_COMPOSIZIONE));
			// db.delete(Composizioni.NOME_TABELLA, wherecs);
			cancellaRecord(db, wherecs, false);
		}

		ArrayList<Object> frutti = db.eseguiSelect("Select " + Composizioni.ID_COMPOSIZIONE + "," + Componenti.SPAZI_OCCUPATI + ","
				+ Composizioni.POSIZIONE_INIZIALE + " from " + Composizioni.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where "
				+ Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and " + CategorieComponenti.TIPO + "='"
				+ CategorieComponenti.TIPO_FRUTTO + "'", null);

		for (int i = 0; i < frutti.size(); i++) {
			ContentValues frutto = (ContentValues) frutti.get(i);
			ContentValues where = new ContentValues();
			where.put(Composizioni.ID_COMPOSIZIONE, frutto.getAsInteger(Composizioni.ID_COMPOSIZIONE));
			// db.delete(Composizioni.NOME_TABELLA, where);
			cancellaRecord(db, where, false);
		}

		// inserisco il copriscatola

		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(Composizioni.ID_ELEMENTO, idElementoIntent);
		valInsertPF.put(Composizioni.ID_COMPONENTE, val.getAsInteger(Componenti.ID_COMPONENTE));
		inserisciRecord(db, valInsertPF);

	}

	public void sostituisciPortafrutti(DbInterno db, ContentValues val, int idElementoIntent) {
		// TODO Auto-generated method stub
		// Elimino l'eventuale copriscatola

		// Completo con tappi se ci sono dei buchi
		Join j0 = new Join(Composizioni.NOME_TABELLA, Componenti.NOME_TABELLA);
		j0.addCampiDiJoin(Composizioni.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j1 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
		j1.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);
		ArrayList<Object> recsPF = db.eseguiSelect(
				"Select " + Composizioni.ID_COMPOSIZIONE + " from " + Composizioni.NOME_TABELLA + j0.getSQLJoin() + " where "
						+ Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "="
						+ Componenti.PORTAFRUTTI, null);
		for (int i = 0; i < recsPF.size(); i++) {
			ContentValues recCS = (ContentValues) recsPF.get(i);
			ContentValues wherecs = new ContentValues();
			wherecs.put(Composizioni.ID_COMPOSIZIONE, recCS.getAsInteger(Composizioni.ID_COMPOSIZIONE));
			// db.delete(Composizioni.NOME_TABELLA, wherecs);
			cancellaRecord(db, wherecs, false);
		}

		// Elimino l'eventuale copriscatola
		ArrayList<Object> recsCS = db.eseguiSelect(
				"Select " + Composizioni.ID_COMPOSIZIONE + " from " + Composizioni.NOME_TABELLA + j0.getSQLJoin() + " where "
						+ Composizioni.ID_ELEMENTO + "=" + idElementoIntent + " and " + Componenti.ID_CATEGORIA_COMPONENTE + "="
						+ Componenti.COPRISCATOLA, null);
		for (int i = 0; i < recsCS.size(); i++) {
			ContentValues recPF = (ContentValues) recsCS.get(i);
			ContentValues wherecs = new ContentValues();
			wherecs.put(Composizioni.ID_COMPOSIZIONE, recPF.getAsInteger(Composizioni.ID_COMPOSIZIONE));
			// db.delete(Composizioni.NOME_TABELLA, wherecs);
			cancellaRecord(db, wherecs, false);
		}

		// inserisco il portafrutti
		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(Composizioni.ID_ELEMENTO, idElementoIntent);
		valInsertPF.put(Composizioni.ID_COMPONENTE, val.getAsInteger(Componenti.ID_COMPONENTE));
		inserisciRecord(db, valInsertPF);

		completaConTappi(db, idElementoIntent);
	}

	public void aggiungiCentralinoQuadro(DbInterno db, ContentValues val, int idElementoIntent) {

		// Elimino la vecchia composizione
		ContentValues wherecs = new ContentValues();
		wherecs.put(Composizioni.ID_ELEMENTO, idElementoIntent);
		// db.delete(Composizioni.NOME_TABELLA, wherecs);
		cancellaRecord(db, wherecs, false);

		// inserisco il centralino
		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(Composizioni.ID_ELEMENTO, idElementoIntent);
		valInsertPF.put(Composizioni.ID_COMPONENTE, val.getAsInteger(Componenti.ID_COMPONENTE));
		inserisciRecord(db, valInsertPF);
	}

	public void aggiungiComponenteQuadro(DbInterno db, ContentValues val, int idElementoIntent) {
		double spaziOccupati = val.getAsDouble(Componenti.SPAZI_OCCUPATI);
		ContentValues where = new ContentValues();
		where.put(ID_ELEMENTO, idElementoIntent);
		ArrayList<Object> componentiOrdinati = db.eseguiSelect(this, where, new String[] { POSIZIONE_INIZIALE + " desc" });
		int posizioneIniziale = 1;
		if (componentiOrdinati.size() > 0) {
			ContentValues recPosMax = (ContentValues) componentiOrdinati.get(0);
			if (recPosMax.getAsInteger(POSIZIONE_INIZIALE) > 0) {
				ContentValues whereComp = new ContentValues();
				whereComp.put(Componenti.ID_COMPONENTE,recPosMax.getAsInteger(Composizioni.ID_COMPONENTE));
				ContentValues valComp = db.getRecord(new Componenti(),whereComp);
				double spaziOccupatiComp = 0;
				if (valComp!=null){
					 spaziOccupatiComp = valComp.getAsDouble(Componenti.SPAZI_OCCUPATI);
				}
				//posizioneIniziale = recPosMax.getAsInteger(POSIZIONE_INIZIALE) + (int) spaziOccupatiComp;
				posizioneIniziale = recPosMax.getAsInteger(POSIZIONE_INIZIALE) + 1;
			}
		}

		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(Composizioni.ID_ELEMENTO, idElementoIntent);
		valInsertPF.put(Composizioni.ID_COMPONENTE, val.getAsInteger(Componenti.ID_COMPONENTE));
		valInsertPF.put(Composizioni.POSIZIONE_INIZIALE, posizioneIniziale);

		//String moduliOccupati = getModuliOccupati(posizioneIniziale, val.getAsDouble(Componenti.SPAZI_OCCUPATI));
		valInsertPF.put(Composizioni.MODULI_OCCUPATI, posizioneIniziale);

		inserisciRecord(db, valInsertPF);
	}

	public void aggiungiComponenteLibero(DbInterno db, ContentValues val, int idElementoIntent) {
		// double spaziOccupati = val.getAsDouble(Componenti.SPAZI_OCCUPATI);
		double spaziOccupati = 1;
		ContentValues where = new ContentValues();
		where.put(ID_ELEMENTO, idElementoIntent);
		ArrayList<Object> componentiOrdinati = db.eseguiSelect(this, where, new String[] { POSIZIONE_INIZIALE + " desc" });
		int posizioneIniziale = 1;
		if (componentiOrdinati.size() > 0) {
			ContentValues recPosMax = (ContentValues) componentiOrdinati.get(0);
			if (recPosMax.getAsInteger(POSIZIONE_INIZIALE) > 0) {
				posizioneIniziale = recPosMax.getAsInteger(POSIZIONE_INIZIALE) + (int) spaziOccupati;
			}
		}

		ContentValues valInsertPF = getValoriLogInserimento(db);
		valInsertPF.put(Composizioni.ID_ELEMENTO, idElementoIntent);
		valInsertPF.put(Composizioni.ID_COMPONENTE, val.getAsInteger(Componenti.ID_COMPONENTE));
		valInsertPF.put(Composizioni.POSIZIONE_INIZIALE, posizioneIniziale);

		// String moduliOccupati = getModuliOccupati(posizioneIniziale, val.getAsDouble(Componenti.SPAZI_OCCUPATI));
		//valInsertPF.put(Composizioni.MODULI_OCCUPATI, posizioneIniziale);

		inserisciRecord(db, valInsertPF);
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

		Join j1 = new Join(NOME_TABELLA, Componenti.NOME_TABELLA);
		j1.addCampiDiJoin(Composizioni.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j2 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
		j2.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);

		CategorieComponenti tabComp = new CategorieComponenti();

		String sql = "Select " + NOME_TABELLA + ".*," + Componenti.NOME_TABELLA + ".*,"
				+ tabComp.getNomeCampoTabella(CategorieComponenti.TIPO) + " from " + NOME_TABELLA + j1.getSQLJoin() + j2.getSQLJoin()
				+ " where " + ID_ELEMENTO + " = " + idElemento;

		ArrayList<Object> result = db.eseguiSelect(sql, null);
		return result;
	}

	/**
	 * 
	 * Ritorna i record di composizione in join con i componenti per l'elemento passato
	 * 
	 * @param db
	 * @param idElemento
	 * @return
	 */
	public ArrayList<Object> getComponentiQuadroOrdinati(DbInterno db, int idElemento) {

		Join j1 = new Join(NOME_TABELLA, Componenti.NOME_TABELLA);
		j1.addCampiDiJoin(Composizioni.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Join j2 = new Join(Componenti.NOME_TABELLA, CategorieComponenti.NOME_TABELLA);
		j2.addCampiDiJoin(Componenti.ID_CATEGORIA_COMPONENTE, CategorieComponenti.ID_CATEGORIA_COMPONENTE);

		CategorieComponenti tabComp = new CategorieComponenti();

		String sql = "Select " + NOME_TABELLA + ".*," + Componenti.NOME_TABELLA + ".*,"
				+ tabComp.getNomeCampoTabella(CategorieComponenti.TIPO) + " from " + NOME_TABELLA + j1.getSQLJoin() + j2.getSQLJoin()
				+ " where " + ID_ELEMENTO + " = " + idElemento + " and "
				+ tabComp.getNomeCampoTabella(CategorieComponenti.ID_CATEGORIA_COMPONENTE) + "=" + Componenti.COMPONENTI_QUADRI
				+ " order by " + POSIZIONE_INIZIALE;

		ArrayList<Object> result = db.eseguiSelect(sql, null);
		return result;
	}

	public void spostaComponente(DbInterno db, ContentValues val, int pos, boolean composizioneLibera,boolean quadro) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		int posCurr = val.getAsInteger(Composizioni.POSIZIONE_INIZIALE);
		int posNew = posCurr + pos;
		if (posNew > 0) {
			// prendo il componente precednete/successivo

			String SQL = "";
			if (pos == 1) {
				SQL = "Select * from " + NOME_TABELLA + " where " + ID_ELEMENTO + "=" + val.getAsInteger(Composizioni.ID_ELEMENTO)
						+ " and " + POSIZIONE_INIZIALE + ">=" + posNew + " order by " + POSIZIONE_INIZIALE + " asc";
			} else {
				SQL = "Select * from " + NOME_TABELLA + " where " + ID_ELEMENTO + "=" + val.getAsInteger(Composizioni.ID_ELEMENTO)
						+ " and " + POSIZIONE_INIZIALE + "<=" + posNew + " order by " + POSIZIONE_INIZIALE + " desc";
			}

			ArrayList<Object> res = db.eseguiSelect(SQL, null);
			ContentValues valPos = null;
			if (res.size() > 0) {
				valPos = (ContentValues) res.get(0);
			}

			if (valPos != null) {

				ContentValues whereComp1 = new ContentValues();
				whereComp1.put(Componenti.ID_COMPONENTE, valPos.getAsInteger(ID_COMPONENTE));
				ContentValues recComp1 = db.getRecord(new Componenti(), whereComp1);
				double spazi_occupati1 = 1d;
				if (recComp1 != null) {
					spazi_occupati1 = recComp1.getAsDouble(Componenti.SPAZI_OCCUPATI);
				}

				ContentValues whereComp2 = new ContentValues();
				whereComp2.put(Componenti.ID_COMPONENTE, val.getAsInteger(ID_COMPONENTE));
				ContentValues recComp2 = db.getRecord(new Componenti(), whereComp2);
				double spazi_occupati2 = 1d;
				if (recComp2 != null) {
					spazi_occupati2 = recComp2.getAsDouble(Componenti.SPAZI_OCCUPATI);
				}

				if (composizioneLibera == true) {
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

				ContentValues valUpd1 = new ContentValues();
				valUpd1.put(POSIZIONE_INIZIALE, pos1);
				if (!composizioneLibera) {
					valUpd1.put(MODULI_OCCUPATI, getModuliOccupati(pos1, spazi_occupati1));
				}else{
					valUpd1.put(MODULI_OCCUPATI, pos1);
				}

				ContentValues where1 = new ContentValues();
				where1.put(ID_COMPOSIZIONE, valPos.getAsInteger(ID_COMPOSIZIONE));

				aggiornaRecord(db, valUpd1, where1);

				ContentValues valUpd2 = new ContentValues();
				valUpd2.put(POSIZIONE_INIZIALE, pos2);
				if (!composizioneLibera) {
					valUpd2.put(MODULI_OCCUPATI, getModuliOccupati(pos2, spazi_occupati2));
				}
				else{
					if (quadro){
						valUpd2.put(MODULI_OCCUPATI, pos2);
					}

				}
				ContentValues where2 = new ContentValues();
				where2.put(ID_COMPOSIZIONE, val.getAsInteger(ID_COMPOSIZIONE));

				aggiornaRecord(db, valUpd2, where2);
			}

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

	@Override
	protected void aggiornamentoCorrelati(DbInterno db, ContentValues val, ContentValues where) {

		// nelle installazioni server la composizione_cantiere deve viaggiare come un unico record
		// quindi aggiorno il campo in_server a false
		/*if (Sessione.isLicenzaBusiness(db.getContext())) {
			ContentValues elem = db.getRecord(this, where);
			if (elem != null) {
				ContentValues whereElemento = new ContentValues();
				whereElemento.put(ID_ELEMENTO, elem.getAsInteger(ID_ELEMENTO));

				ContentValues valUpd = new ContentValues();
				valUpd.put(IN_SERVER, 0);
				db.update(NOME_TABELLA, valUpd, whereElemento);
			}
		}*/
		super.aggiornamentoCorrelati(db, val, where);
	}

	@Override
	protected void inserimentoCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		// nelle installazioni server la composizione_cantiere deve viaggiare come un unico record
		// quindi aggiorno il campo in_server a false
	/*	if (Sessione.isLicenzaBusiness(db.getContext())) {

			ContentValues whereElemento = new ContentValues();
			whereElemento.put(ID_ELEMENTO, val.getAsInteger(ID_ELEMENTO));

			ContentValues valUpd = new ContentValues();
			valUpd.put(IN_SERVER, 0);
			db.update(NOME_TABELLA, valUpd, whereElemento);

		}*/
		super.inserimentoCorrelati(db, val);
	}

}
