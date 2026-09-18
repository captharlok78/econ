package pfa.app.econtab.db.table;

import android.content.ContentValues;
import android.content.Context;

import java.io.File;
import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.utils.Utility;

public class Locali extends AbstractTable {
	public static final String NOME_TABELLA = "locali";

	public static final String ID_LOCALE = "id_locale";
	public static final String ID_AREA = "id_area";
	public static final String ID_DITTA = "id_ditta";
	public static final String NOME = "nome";
	public static final String ID_LINEA = "id_linea";
	public static final String ID_PLACCA = "id_placca";
	public static final String PIANTA_LOGICA = "pianta_logica";
	public static final String PIANTA_IMMAGINE = "pianta_immagine";
	public static final String NOTE = "note";
	public static final String PREFERITO_SN = "preferito_sn";

	public Locali() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_LOCALE);

		aggiungiCampo(ID_LOCALE, INTEGER);
		aggiungiCampo(ID_AREA, INTEGER);
		aggiungiCampo(ID_DITTA, INTEGER);
		aggiungiCampo(NOME, TEXT);
		aggiungiCampo(ID_LINEA, INTEGER);
		aggiungiCampo(ID_PLACCA, INTEGER);
		aggiungiCampo(PIANTA_LOGICA, TEXT);
		aggiungiCampo(PIANTA_IMMAGINE, TEXT);
		aggiungiCampo(NOTE, TEXT);
		aggiungiCampo(PREFERITO_SN, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_LOCALE);
	}

	public ArrayList<Object> getLocaliCantiere(DbInterno db, int cantiere) {
		Join joinAreeUnita = new Join(Aree.NOME_TABELLA, Unita.NOME_TABELLA);
		joinAreeUnita.addCampiDiJoin(Aree.ID_UNITA, Unita.ID_UNITA);

		Join joinLocaliAree = new Join(Locali.NOME_TABELLA, Aree.NOME_TABELLA);
		joinLocaliAree.addCampiDiJoin(Locali.ID_AREA, Aree.ID_AREA);
		ArrayList<Object> locali = db.eseguiSelect(
				"Select " + getNomeCampoTabella("*") + " from " + NOME_TABELLA + joinLocaliAree.getSQLJoin() + joinAreeUnita.getSQLJoin()
						+ " where " + Unita.ID_CANTIERE + "=" + cantiere + " order by " + ID_LOCALE + " desc", null);
		return locali;
	}

	public ArrayList<Object> getLocaliCantiereOrdineAlfabetico(DbInterno db, int cantiere) {
		Join joinAreeUnita = new Join(Aree.NOME_TABELLA, Unita.NOME_TABELLA);
		joinAreeUnita.addCampiDiJoin(Aree.ID_UNITA, Unita.ID_UNITA);

		Join joinLocaliAree = new Join(Locali.NOME_TABELLA, Aree.NOME_TABELLA);
		joinLocaliAree.addCampiDiJoin(Locali.ID_AREA, Aree.ID_AREA);
		ArrayList<Object> locali = db.eseguiSelect(
				"Select " + getNomeCampoTabella("*") + " from " + NOME_TABELLA + joinLocaliAree.getSQLJoin() + joinAreeUnita.getSQLJoin()
						+ " where " + Unita.ID_CANTIERE + "=" + cantiere + " order by " + getNomeCampoTabella(NOME), null);
		return locali;
	}

	public ArrayList<Object> getLocaliCantiereConArea(DbInterno db, int cantiere) {
		Join joinAreeUnita = new Join(Aree.NOME_TABELLA, Unita.NOME_TABELLA);
		joinAreeUnita.addCampiDiJoin(Aree.ID_UNITA, Unita.ID_UNITA);

		Join joinLocaliAree = new Join(Locali.NOME_TABELLA, Aree.NOME_TABELLA);
		joinLocaliAree.addCampiDiJoin(Locali.ID_AREA, Aree.ID_AREA);

		Aree tabAree = new Aree();

		ArrayList<Object> locali = db.eseguiSelect("Select " + getNomeCampoTabella("*") + "," + tabAree.getNomeCampoTabella(Aree.NOME)
				+ " as " + tabAree.getAlias(Aree.NOME) + " from " + NOME_TABELLA + joinLocaliAree.getSQLJoin() + joinAreeUnita.getSQLJoin()
				+ " where " + Unita.ID_CANTIERE + "=" + cantiere + " order by " + Aree.NOME_TABELLA + "." + ID_AREA + " desc," + ID_LOCALE
				+ " desc", null);
		return locali;
	}

	public ArrayList<Object> getLocaliUnita(DbInterno db, int unita) {

		Join joinLocaliAree = new Join(Locali.NOME_TABELLA, Aree.NOME_TABELLA);
		joinLocaliAree.addCampiDiJoin(Locali.ID_AREA, Aree.ID_AREA);
		ArrayList<Object> locali = db.eseguiSelect(
				"Select " + getNomeCampoTabella("*") + " from " + NOME_TABELLA + joinLocaliAree.getSQLJoin() + " where " + Aree.ID_UNITA
						+ "=" + unita, null);
		return locali;
	}

	public ArrayList<Object> getLocaliArea(DbInterno db, int area) {

		ContentValues where = new ContentValues();
		where.put(Locali.ID_AREA, area);
		ArrayList<Object> locali = db.eseguiSelect(this, where, null);

		return locali;
	}

	public static String getJoinPerIDCantiere() {
		Join j1 = new Join(NOME_TABELLA, Aree.NOME_TABELLA);
		j1.addCampiDiJoin(ID_AREA, Aree.ID_AREA);

		Join j2 = new Join(Aree.NOME_TABELLA, Unita.NOME_TABELLA);
		j2.addCampiDiJoin(Aree.ID_UNITA, Unita.ID_UNITA);

		return j1.getSQLJoin() + j2.getSQLJoin();
	}

	@Override
	protected void eliminaCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		int locale = val.getAsInteger(ID_LOCALE);
		LocaliPorteFinestre tabporte = new LocaliPorteFinestre();
		ContentValues where = new ContentValues();
		where.put(ID_LOCALE, locale);
		ArrayList<Object> porte = db.eseguiSelect(new LocaliPorteFinestre(), where, null);
		for (int i = 0; i < porte.size(); i++) {
			ContentValues vallocale = (ContentValues) porte.get(i);
			// ContentValues vallocalewhere = tabporte.getFiltroPerChiave(vallocale);
			// tabporte.eliminaCorrelati(db, vallocale);
			// db.delete(LocaliPorteFinestre.NOME_TABELLA, vallocalewhere);

			tabporte.cancellaRecord(db, vallocale);
		}

		ContentValues where1 = new ContentValues();
		ElementiCantiere tabelementi = new ElementiCantiere();
		where1.put(ID_LOCALE, locale);
		ArrayList<Object> elementi = db.eseguiSelect(new ElementiCantiere(), where1, null);
		for (int i = 0; i < elementi.size(); i++) {
			ContentValues vallocale = (ContentValues) elementi.get(i);
			// ContentValues vallocalewhere = tabelementi.getFiltroPerChiave(vallocale);
			// tabelementi.eliminaCorrelati(db, vallocale);
			// db.delete(ElementiCantiere.NOME_TABELLA, vallocalewhere);

			tabelementi.cancellaRecord(db, vallocale);
		}



        //prendo tutte le righe di preventivi dettaglio ed elimino
        PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();
        ContentValues wherePrev = new ContentValues();
        wherePrev.put(PreventiviDettaglio.ID_LOCALE,locale);
        ArrayList<Object> recsPrev = db.eseguiSelect(tabPrevDett,wherePrev,null);
        for (int i=0;i<recsPrev.size();i++){
             ContentValues currPrev = (ContentValues)recsPrev.get(i);
             tabPrevDett.cancellaRecord(db,currPrev);
        }


		super.eliminaCorrelati(db, val);
	}

	/**
	 * Prende la struttura del locale (area,unit�, cantiere)
	 * 
	 * @param db
	 * @param idLocale
	 * @return
	 */
	public ContentValues getAreaUnitaCantiere(DbInterno db, int idLocale) {
		Join jA = new Join(NOME_TABELLA, Aree.NOME_TABELLA);
		jA.addCampiDiJoin(ID_AREA, Aree.ID_AREA);

		Join jU = new Join(Aree.NOME_TABELLA, Unita.NOME_TABELLA);
		jU.addCampiDiJoin(Aree.ID_UNITA, Unita.ID_UNITA);

		Join jC = new Join(Unita.NOME_TABELLA, Cantieri.NOME_TABELLA);
		jC.addCampiDiJoin(Unita.ID_CANTIERE, Cantieri.ID_CANTIERE);

		Aree tabAree = new Aree();
		Unita tabUnita = new Unita();
		Cantieri tabCantieri = new Cantieri();

		String SQL = "Select " + getNomeCampoTabella(NOME) + "," + tabAree.getNomeCampoTabella(Aree.ID_AREA) + ","
				+ tabAree.getNomeCampoTabella(Aree.NOME) + "," + tabUnita.getNomeCampoTabella(Unita.ID_UNITA) + ","
				+ tabUnita.getNomeCampoTabella(Unita.NOME) + "," + tabCantieri.getNomeCampoTabella(Unita.ID_CANTIERE) + ","
				+ tabCantieri.getNomeCampoTabella(Unita.NOME) + " from " + NOME_TABELLA + jA.getSQLJoin() + jU.getSQLJoin()
				+ jC.getSQLJoin() + " where " + ID_LOCALE + " = " + idLocale;

		ArrayList<Object> recs = db.eseguiSelect(SQL, null);
		if (recs.size() > 0) {
			return (ContentValues) recs.get(0);
		}
		return null;
	}

	/**
	 * Prende la linea del locale: 1) dal locale se specificata 2) altrimenti dall'area se specificata 3) altrimenti
	 * dall'unit� se specificata 4) altrimenti dal cantiere
	 * 
	 * @param db
	 * @param idLocale
	 * @return
	 */
	public ContentValues getLineaLocale(DbInterno db, int idLocale) {
		// TODO Auto-generated method stub
		ContentValues recAUC = getAreaUnitaCantiere(db, idLocale);

		Join jLinea = new Join(Locali.NOME_TABELLA, Linee.NOME_TABELLA);
		jLinea.addCampiDiJoin(Locali.ID_LINEA, Linee.ID_LINEA);

		Join jLinea2 = new Join(Linee.NOME_TABELLA, Costruttori.NOME_TABELLA);
		jLinea2.addCampiDiJoin(Linee.ID_COSTRUTTORE, Costruttori.ID_COSTRUTTORE);

		Linee tabLinee = new Linee();
		Costruttori tabCostruttori = new Costruttori();

		String SQL_LINEA = "Select " + tabLinee.getNomeCampoTabella(Linee.ID_LINEA) + "," + tabLinee.getNomeCampoTabella(Linee.NOME_LINEA)
				+ "," + tabCostruttori.getNomeCampoTabella(Costruttori.RAGIONE_SOCIALE) + ","
				+ tabCostruttori.getNomeCampoTabella(Costruttori.SIGLA_METEL) + ","
				+ tabCostruttori.getNomeCampoTabella(Costruttori.ID_COSTRUTTORE) + " from " + NOME_TABELLA + jLinea.getSQLJoin()
				+ jLinea2.getSQLJoin() + " where " + ID_LOCALE + "= " + idLocale;

		ArrayList<Object> recs = db.eseguiSelect(SQL_LINEA, null);
		if (recs.size() > 0) {
			return (ContentValues) recs.get(0);
		}

		if (recAUC != null) {
			// se non trovata la linea nel locale allora guardo nell'area
			Aree tabAree = new Aree();
			ContentValues lineaArea = tabAree.getLineaArea(db, recAUC.getAsInteger(Aree.ID_AREA));
			if (lineaArea != null) {
				return lineaArea;

			}
			// se non trovata la linea nell'area allora guardo nell'unit�
			Unita tabUnita = new Unita();
			ContentValues lineaUnita = tabUnita.getLineaUnita(db, recAUC.getAsInteger(Unita.ID_UNITA));
			if (lineaUnita != null) {
				return lineaUnita;

			}
			// se non trovata la linea nell'unit� allora guardo nel cantiere
			Cantieri tabCantieri = new Cantieri();
			ContentValues lineaCantiere = tabCantieri.getLineaCantiere(db, recAUC.getAsInteger(Cantieri.ID_CANTIERE));
			if (lineaCantiere != null) {
				return lineaCantiere;

			}
		}

		return null;
	}

	public ContentValues getCantiereLocale(DbInterno db, int idLocale) {
		Join j0 = new Join(NOME_TABELLA, Aree.NOME_TABELLA);
		j0.addCampiDiJoin(ID_AREA, Aree.ID_AREA);

		Join j1 = new Join(Aree.NOME_TABELLA, Unita.NOME_TABELLA);
		j1.addCampiDiJoin(Aree.ID_UNITA, Unita.ID_UNITA);

		Join j2 = new Join(Unita.NOME_TABELLA, Cantieri.NOME_TABELLA);
		j2.addCampiDiJoin(Unita.ID_CANTIERE, Cantieri.ID_CANTIERE);

		Cantieri tabCant = new Cantieri();

		ArrayList<Object> res = db.eseguiSelect("Select " + tabCant.getNomeCampoTabella("*") + "from " + NOME_TABELLA + j0.getSQLJoin()
				+ j1.getSQLJoin() + j2.getSQLJoin() + " where " + ID_LOCALE + "=" + idLocale, null);
		if (res.size() > 0) {
			return (ContentValues) res.get(0);
		}
		return null;
	}

	@Override
	protected void aggiornamentoCorrelati(DbInterno db, ContentValues val, ContentValues where) {
		// TODO Auto-generated method stub
		ContentValues recAggiornato = db.getRecord(this, where);
		int idLocale = recAggiornato.getAsInteger(ID_LOCALE);
		// prendo il record del cantiere
		ContentValues cantiere = getCantiereLocale(db, idLocale);
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

					//ArrayList<Object> righePlacche = tabPrevDett.getRighePreventivo(db, idPreventivo, PreventiviDettaglio.PLACCHE);

                    //aggiornaPlaccheDaLocale(righePlacche, val, idLocale, idPreventivo, db);
					tabPrevDett.aggiornaRighePreventivoLocale(db,idPreventivo,idLocale,false);


					tabPrevDett.associazioneAutomaticaCodiciLocale(db, idPreventivo, idLocale);

				}
			}
		}

		super.aggiornamentoCorrelati(db, val, where);
	}

  /*  public void aggiornaPlaccheDaLocale(ArrayList<Object> righePlacche, ContentValues val, int idLocale, int idPreventivo, DbInterno db) {
        PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();
        PlaccheModuli tabPlaccheModuli = new PlaccheModuli();
        ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
        ArrayList<Object> placcheLocale = new ArrayList<Object>();
        for (int j = 0; j < righePlacche.size(); j++) {
            ContentValues valPlacca = (ContentValues) righePlacche.get(j);
            if (valPlacca.getAsInteger(PreventiviDettaglio.ID_LOCALE) == idLocale) {
                placcheLocale.add(valPlacca);
                // se non c'è la placca selezionata sul locale elimino tutte le placche dal preventivo
                if (val.getAsString(ID_PLACCA) == null || val.getAsString(ID_PLACCA).equals("") || val.getAsString(ID_PLACCA).equals("0")) {
                    tabPrevDett.cancellaRecord(db, valPlacca);
                }
                // altrimenti aggiorno le righe di placche con il nuovo id di placche_moduli
                else {
                    ContentValues wherePlaccheModuli = new ContentValues();
                    wherePlaccheModuli.put(PlaccheModuli.ID_PLACCA_MODULI,
                            valPlacca.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI));
                    ContentValues recPlaccaModuli = db.getRecord(new PlaccheModuli(), wherePlaccheModuli);
                    if (recPlaccaModuli != null) {
                        int numeroModuli = recPlaccaModuli.getAsInteger(PlaccheModuli.NUMERO_MODULI);
                        int idPlaccaModuliNew = tabPlaccheModuli.controllaInserisciRecord(db, val.getAsInteger(ID_PLACCA),
                                numeroModuli);
                        ContentValues wherePlacca = new ContentValues();
                        wherePlacca.put(Placche.ID_PLACCA, val.getAsString(ID_PLACCA));
                        ContentValues valPlaccaNome = db.getRecord(new Placche(), wherePlacca);

                        ContentValues upd = new ContentValues();
                        upd.put(PreventiviDettaglio.ID_PLACCA_MODULI, idPlaccaModuliNew);
                        upd.put(PreventiviDettaglio.DESCRIZIONE, valPlaccaNome.getAsString(Placche.NOME_PLACCA));
                        tabPrevDett.aggiornaRecord(db, upd, tabPrevDett.getFiltroPerChiave(valPlacca));
                    }
                }
            }
        }

        // se non ho le placche a preventivo probabilmente significa che prima il locale non aveva
        // l'id_placca e quindi vanno inserite da zero


        if (placcheLocale.size() == 0 && val.getAsString(ID_PLACCA) != null && !val.getAsString(ID_PLACCA).equals("")) {
            // prendo tutti gli elementi del locale
            ContentValues whereLoc = new ContentValues();
            whereLoc.put(ElementiCantiere.ID_PREVENTIVO, idPreventivo);
            whereLoc.put(ElementiCantiere.ID_LOCALE, idLocale);
            ArrayList<Object> elementi = db.eseguiSelect(new ElementiCantiere(), whereLoc, null);

            for (int j = 0; j < elementi.size(); j++) {
                ContentValues valEmen = (ContentValues) elementi.get(j);
                ArrayList<Object> composizioni = tabComposizioni.getComposizioneElemento(db,
                        valEmen.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT));
                valEmen.put(Elementi.PLACCA_SN, valEmen.getAsInteger(ElementiCantiere.PLACCA_SN));
                for (int k = 0; k < composizioni.size(); k++) {
                    ContentValues valComp = (ContentValues) composizioni.get(k);
                    if (valComp.getAsString(CategorieComponenti.TIPO).equals(CategorieComponenti.TIPO_PORTAFRUTTI)) {
						try{
							if (valComp.get(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI)==null || valComp.getAsInteger(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI)==0 ){
								tabPrevDett.aggiornaPlacche(db, valEmen, valComp, idPreventivo, idLocale, 1);
							}
						}
						catch (Exception ex){
							tabPrevDett.aggiornaPlacche(db, valEmen, valComp, idPreventivo, idLocale, 1);
						}


                    }
                }

            }

        }
    }*/

    /**
	 * Effettua le operazioni per il salvataggio del locale e degli elementi visualizzati come preferito
	 * 
	 * @param db
	 * @param listaIdElementi
	 * @throws Exception
	 */
	public void salvaLocalePreferito(DbInterno db, int idLocale, String nomePreferito, ArrayList<Integer> listaIdElementi) throws Exception {
		// 1) prendo il locale di partenza
		ContentValues where = new ContentValues();
		where.put(ID_LOCALE, idLocale);
		ContentValues recOrigine = db.getRecord(this, where);

		if (recOrigine != null) {
			ContentValues valoriDaSovrascrivere = new ContentValues();
			valoriDaSovrascrivere.put(ID_AREA, 0);
			valoriDaSovrascrivere.put(NOME, nomePreferito);
			valoriDaSovrascrivere.put(PREFERITO_SN, 1);
			ContentValues recordCopia = copiaRecord(db, recOrigine, valoriDaSovrascrivere);
			int idLocaleCopia = recordCopia.getAsInteger(ID_LOCALE);
			File immagineOriginale = Utility.getFileImmaginePiantina(idLocale);
			File immagineDestinazione = Utility.getFileImmaginePiantina(idLocaleCopia);
			Utility.copiaFile(immagineOriginale, immagineDestinazione);

			File immagineOriginaleThumb = Utility.getFileImmaginePiantinaThumb(idLocale);
			File immagineDestinazioneThumb = Utility.getFileImmaginePiantinaThumb(idLocaleCopia);

			Utility.copiaFile(immagineOriginaleThumb, immagineDestinazioneThumb);

			// 2) Copio le porte e le finestre
			_salvaPorteFinestreLocalePreferito(db, idLocale, idLocaleCopia);

			// 3) prendo tutti gli elementi visualizzati e li copio sull nuova stanza prefrita
			_salvaElementiLocalePreferito(db, idLocale, idLocaleCopia, listaIdElementi);

		}

	}

	private void _salvaPorteFinestreLocalePreferito(DbInterno db, int idLocaleOrigine, int idLocaleCopia) {
		// TODO Auto-generated method stub
		LocaliPorteFinestre tabPorte = new LocaliPorteFinestre();
		ContentValues where = new ContentValues();
		where.put(ID_LOCALE, idLocaleOrigine);
		ArrayList<Object> porte = db.eseguiSelect(tabPorte, where, null);

		for (int i = 0; i < porte.size(); i++) {
			ContentValues recPorta = (ContentValues) porte.get(i);
			ContentValues valoriDaSovrascrivere = new ContentValues();
			valoriDaSovrascrivere.put(LocaliPorteFinestre.ID_LOCALE, idLocaleCopia);
			tabPorte.copiaRecord(db, recPorta, valoriDaSovrascrivere);
		}
	}

	private void _salvaElementiLocalePreferito(DbInterno db, int idLocaleOrigine, int idLocaleCopia, ArrayList<Integer> listaIdElementi) {
		// TODO Auto-generated method stub
		if (listaIdElementi.size() > 0) {
			String filtroElementi = "";
			for (int i = 0; i < listaIdElementi.size(); i++) {
				filtroElementi = filtroElementi + listaIdElementi.get(i) + ",";
			}
			filtroElementi = filtroElementi.substring(0, filtroElementi.length() - 1);

			String SQLELEM = "Select * from " + ElementiCantiere.NOME_TABELLA + " where " + ElementiCantiere.ID_ELEMENTO_CANT + " in ("
					+ filtroElementi + ")";
			ArrayList<Object> elementi = db.eseguiSelect(SQLELEM, null);
			ContentValues nuoviValori = new ContentValues();
			ContentValues where = new ContentValues();
			ElementiCantiere tabElementi = new ElementiCantiere();
			ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
			ComponentiCantiere tabComponenti = new ComponentiCantiere();
			for (int i = 0; i < elementi.size(); i++) {
				ContentValues curr = (ContentValues) elementi.get(i);
				nuoviValori.clear();
				nuoviValori.put(ElementiCantiere.ID_PREVENTIVO, 0);
				nuoviValori.put(ElementiCantiere.ID_CANTIERE, 0);
				nuoviValori.put(ElementiCantiere.ID_LOCALE, idLocaleCopia);
				nuoviValori.put(ElementiCantiere.PREVENTIVI, "");
				nuoviValori.put(ElementiCantiere.NUMERO_IDENTIFICATIVO, (i + 1));
				ContentValues recCopia = tabElementi.copiaRecord(db, curr, nuoviValori);

				// ATTENZIONE CHE IN FASE DI COPIA DEVO COPIARE LE COMPOSIZIONI NON DALLA TABELLA COMPOSIZIONI MA DA
				// COMPOSIZIONI_CANTIERE
				int idElementoCantiereOrigine = curr.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT);
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
						nuoviValori.clear();
						nuoviValori.put(ComponentiCantiere.ID_LOCALE, 0);
						nuoviValori.put(ComponentiCantiere.ID_CANTIERE, 0);
						nuoviValori.put(ComponentiCantiere.ID_PREVENTIVO, 0);

						ContentValues recCopiaComp = tabComponenti.copiaRecord(db, recComp, nuoviValori);
						nuoviValori.clear();
						nuoviValori.put(ComposizioniCantiere.ID_ELEMENTO_CANT, recCopia.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT));
						nuoviValori.put(ComposizioniCantiere.ID_COMPONENTE_CANT,
								recCopiaComp.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
						// copi la composizione
						tabComposizioni.copiaRecord(db, currCompos, nuoviValori);

					}
				}
			}

		}
	}

	public void importaDaPreferito(DbInterno db, int idLocale, int idPreventivo, int idLocalePreferito, boolean conElementi, int[] elementi) {
		System.out.println("EConTab: Locali importaDaPreferito ENTER");
		// 1) importa i dati del locale (pianta logica per adesso)
		ContentValues whereLocale = new ContentValues();
		whereLocale.put(ID_LOCALE, idLocale);
		ContentValues valLocale = db.getRecord(this, whereLocale);

		ContentValues wherePref = new ContentValues();
		wherePref.put(ID_LOCALE, idLocalePreferito);
		ContentValues valPref = db.getRecord(this, wherePref);

		ContentValues whereUpd = new ContentValues();
		whereUpd.put(ID_LOCALE, idLocale);

		ContentValues valUpd = new ContentValues();
		valUpd.put(PIANTA_LOGICA, valPref.getAsString(PIANTA_LOGICA));
		// se la stanza non ha nome aggiorno anche il nome
		if (valLocale.getAsString(NOME).equals("")) {
			valUpd.put(NOME, valPref.getAsString(NOME));
		}
		aggiornaRecord(db, valUpd, whereUpd);

		// 2) aggiorno le porte e le finestre: cancello le vecchie e copio le nuove
		LocaliPorteFinestre tabporte = new LocaliPorteFinestre();
		ContentValues where = new ContentValues();
		where.put(LocaliPorteFinestre.ID_LOCALE, idLocale);
		ArrayList<Object> porte = db.eseguiSelect(tabporte, where, null);
		for (int i = 0; i < porte.size(); i++) {
			ContentValues vallocale = (ContentValues) porte.get(i);
			tabporte.cancellaRecord(db, vallocale);
		}

		where.put(LocaliPorteFinestre.ID_LOCALE, idLocalePreferito);
		ArrayList<Object> portePref = db.eseguiSelect(tabporte, where, null);
		for (int i = 0; i < portePref.size(); i++) {
			ContentValues recOrigine = (ContentValues) portePref.get(i);
			ContentValues valoriDaSovrascrivere = new ContentValues();
			valoriDaSovrascrivere.put(LocaliPorteFinestre.ID_LOCALE, idLocale);
			tabporte.copiaRecord(db, recOrigine, valoriDaSovrascrivere);
		}

		// 3) se devo copiare anche gli elementi elimino quelli passati e copio i nuovi
		if (conElementi) {

			// prendo il cantiere dal locale

			ContentValues cantiere = getCantiereLocale(db, idLocale);
			int idCantiere = cantiere.getAsInteger(Cantieri.ID_CANTIERE);

			ContentValues whereElem = new ContentValues();
			ElementiCantiere tabElem = new ElementiCantiere();
			for (int i = 0; i < elementi.length; i++) {
				whereElem.put(ElementiCantiere.ID_ELEMENTO_CANT, elementi[i]);
				tabElem.cancellaRecord(db, whereElem);
			}

			whereElem.clear();
			whereElem.put(ElementiCantiere.ID_LOCALE, idLocalePreferito);
			ArrayList<Object> elementiPref = db.eseguiSelect(tabElem, whereElem, null);
			for (int i = 0; i < elementiPref.size(); i++) {
				ContentValues val = (ContentValues) elementiPref.get(i);

				ContentValues valInsert = tabElem.getValoriLogInserimento(db);
				valInsert.put(ElementiCantiere.ID_LOCALE, idLocale);
				valInsert.put(ElementiCantiere.ID_ELEMENTO, val.getAsInteger(ElementiCantiere.ID_ELEMENTO));
				valInsert.put(ElementiCantiere.NOME_ELEMENTO_CANT, val.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
				valInsert.put(ElementiCantiere.UNITA_MISURA, val.getAsString(ElementiCantiere.UNITA_MISURA));
				valInsert.put(ElementiCantiere.ALTEZZA_DA_TERRA, val.getAsDouble(ElementiCantiere.ALTEZZA_DA_TERRA));
				valInsert.put(ElementiCantiere.PLACCA_SN, val.getAsInteger(ElementiCantiere.PLACCA_SN));
				valInsert.put(ElementiCantiere.ID_PREVENTIVO, idPreventivo);
				valInsert.put(ElementiCantiere.METRI_CAVO_CANT, val.getAsDouble(ElementiCantiere.METRI_CAVO_CANT));
				valInsert.put(ElementiCantiere.METRI_TUBO_CANT, val.getAsDouble(ElementiCantiere.METRI_TUBO_CANT));
				valInsert.put(ElementiCantiere.ID_ELEMENTO_CAVO, val.getAsInteger(ElementiCantiere.ID_ELEMENTO_CAVO));
				valInsert.put(ElementiCantiere.ID_ELEMENTO_TUBO, val.getAsInteger(ElementiCantiere.ID_ELEMENTO_TUBO));
				// se sto aggiungendo un elemento ad un preventivo non scrivo subito l'id cantiere (verr� scritto quando
				// il
				// preventivo sar� consolidato)
				if (idPreventivo == 0) {
					valInsert.put(ElementiCantiere.ID_CANTIERE, idCantiere);
				}
				valInsert.put(ElementiCantiere.POS_X, val.getAsInteger(ElementiCantiere.POS_X));
				valInsert.put(ElementiCantiere.POS_Y, val.getAsInteger(ElementiCantiere.POS_Y));
				valInsert.put(ElementiCantiere.NUMERO_IDENTIFICATIVO, tabElem.getProssimoNumero(db, idCantiere));

				// imposto l'id dell'elemento preferito nella tabella per differenziare il compartamento di
				// inserimentoCorrelati
				// che � la funzione che si occupa di inserire la composizione e aggiornare il preventivo
				tabElem.setIdElementoCantierePreferito(val.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT));
				tabElem.inserisciRecord(db, valInsert);
				tabElem.setIdElementoCantierePreferito(0);

			}
		}
		System.out.println("EConTab: Locali importaDaPreferito EXIT");
	}

    @Override
    public boolean cancellazionePossibile(DbInterno db, ContentValues val, Context ctx) {
        // se esistono dei preventivi/ordini non cancello
        int idLocale = val.getAsInteger(ID_LOCALE);
        ContentValues wherePrev = new ContentValues();
        wherePrev.put(PreventiviDettaglio.ID_LOCALE, idLocale);

        int numeroPreventivi = db.eseguiCount(new PreventiviDettaglio(), wherePrev);
        if (numeroPreventivi > 0) {
            String messaggio = ctx.getString(R.string.cancellazione_non_possibile_locale, "" + numeroPreventivi);
            Utility.mostraDialog(ctx.getString(R.string.attenzione), messaggio, ctx, "OK");
            return false;
        }

        return super.cancellazionePossibile(db, val, ctx);
    }



}
