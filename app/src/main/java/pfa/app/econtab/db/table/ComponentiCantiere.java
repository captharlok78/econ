package pfa.app.econtab.db.table;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.utils.Sessione;

public class ComponentiCantiere extends AbstractTable {
	public static final String NOME_TABELLA = "componenti_cantiere";

	public static final String ID_COMPONENTE_CANT = "id_componente_cant";
	public static final String ID_COMPONENTE = "id_componente";
	public static final String NOME_COMPONENTE_CANT = "nome_componente_cant";
	public static final String SPAZI_OCCUPATI_CANT = "spazi_occupati_cant";
	public static final String SPAZI_OSPITATI_CANT = "spazi_ospitati_cant";
	public static final String TAPPO_SN_CANT = "tappo_sn_cant";
	public static final String METRI_CAVO_CANT = "metri_cavo_cant";
	public static final String ID_ELEMENTO_CAVO = "id_elemento_cavo";
	public static final String METRI_TUBO_CANT = "metri_tubo_cant";
	public static final String ID_ELEMENTO_TUBO = "id_elemento_tubo";
	public static final String ID_CANTIERE = "id_cantiere";
	public static final String ID_UNITA = "id_unita";
	public static final String ID_AREA = "id_area";
	public static final String ID_LOCALE = "id_locale";
	public static final String POS_X = "pos_x";
	public static final String POS_Y = "pos_y";
	public static final String UNITA_MISURA = "unita_misura";
	public static final String NOTA = "nota";
	public static final String NON_CONTEGGIARE_PREVENTIVI = "non_conteggiare_preventivi";
	public static final String ID_PREVENTIVO = "id_preventivo";

	public ComponentiCantiere() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_COMPONENTE_CANT);

		aggiungiCampo(ID_COMPONENTE_CANT, INTEGER);
		aggiungiCampo(ID_COMPONENTE, INTEGER);
		aggiungiCampo(NOME_COMPONENTE_CANT, TEXT);
		aggiungiCampo(SPAZI_OCCUPATI_CANT, NUMERIC);
		aggiungiCampo(SPAZI_OSPITATI_CANT, INTEGER);
		aggiungiCampo(TAPPO_SN_CANT, INTEGER);
		aggiungiCampo(METRI_CAVO_CANT, NUMERIC);
		aggiungiCampo(ID_ELEMENTO_CAVO, INTEGER);
		aggiungiCampo(METRI_TUBO_CANT, NUMERIC);
		aggiungiCampo(ID_ELEMENTO_TUBO, INTEGER);
		aggiungiCampo(ID_CANTIERE, INTEGER);
		aggiungiCampo(ID_UNITA, INTEGER);
		aggiungiCampo(ID_AREA, INTEGER);
		aggiungiCampo(ID_LOCALE, INTEGER);
		aggiungiCampo(POS_X, INTEGER);
		aggiungiCampo(POS_Y, INTEGER);
		aggiungiCampo(UNITA_MISURA, TEXT);
		aggiungiCampo(NOTA, TEXT);
		aggiungiCampo(NON_CONTEGGIARE_PREVENTIVI,INTEGER);
		aggiungiCampo(ID_PREVENTIVO,INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_COMPONENTE_CANT);
	}

	@Override
	protected void aggiornamentoCorrelati(DbInterno db, ContentValues val, ContentValues where) {
		// TODO Auto-generated method stub
	/*	int idComponente = where.getAsInteger(ID_COMPONENTE_CANT);
		Join j0 = new Join(ComposizioniCantiere.NOME_TABELLA, ElementiCantiere.NOME_TABELLA);
		j0.addCampiDiJoin(ComposizioniCantiere.ID_ELEMENTO_CANT, ElementiCantiere.ID_ELEMENTO_CANT);

		String SQL = "Select " + ElementiCantiere.NOME_TABELLA + ".* from " + ComposizioniCantiere.NOME_TABELLA + j0.getSQLJoin()
				+ " where " + ComposizioniCantiere.ID_COMPONENTE_CANT + "=" + idComponente;

		ArrayList<Object> recs = db.eseguiSelect(SQL, null);
		ContentValues recordCheck = null;
		if (recs.size() > 0) {
			recordCheck = (ContentValues) recs.get(0);
		}
		int idPreventivo = 0;
		int idLocale = 0;
		if (recordCheck != null) {

			idPreventivo = recordCheck.getAsInteger(ElementiCantiere.ID_PREVENTIVO);
			idLocale = recordCheck.getAsInteger(ElementiCantiere.ID_LOCALE);
			if (idLocale != 0) {
				PreventiviDettaglio tabPrev = new PreventiviDettaglio();
				tabPrev.aggiornaTubiECaviLocale(db, idPreventivo, idLocale);
			}
		}*/
		super.aggiornamentoCorrelati(db, val, where);
	}

	public ContentValues inserisciComponenteCantiere(DbInterno db, ContentValues val) {
		ContentValues valComp = getValoriLogInserimento(db);

		valComp.put(ComponentiCantiere.ID_COMPONENTE, val.getAsInteger(Componenti.ID_COMPONENTE));
		valComp.put(ComponentiCantiere.NOME_COMPONENTE_CANT, val.getAsString(Componenti.NOME_COMPONENTE));
		valComp.put(ComponentiCantiere.SPAZI_OCCUPATI_CANT, val.getAsFloat(Componenti.SPAZI_OCCUPATI));
		valComp.put(ComponentiCantiere.SPAZI_OSPITATI_CANT, val.getAsInteger(Componenti.SPAZI_OSPITATI));
		valComp.put(ComponentiCantiere.TAPPO_SN_CANT, val.getAsInteger(Componenti.TAPPO_SN));
		valComp.put(ComponentiCantiere.METRI_CAVO_CANT, val.getAsFloat(Componenti.METRI_CAVO));
		valComp.put(ComponentiCantiere.ID_ELEMENTO_CAVO, val.getAsInteger(Componenti.ID_ELEMENTO_CAVO));
		valComp.put(ComponentiCantiere.METRI_TUBO_CANT, val.getAsFloat(Componenti.METRI_TUBO));
		valComp.put(ComponentiCantiere.ID_ELEMENTO_TUBO, val.getAsInteger(Componenti.ID_ELEMENTO_TUBO));
		valComp.put(ComponentiCantiere.UNITA_MISURA, val.getAsString(Componenti.UNITA_MISURA));


		inserisciRecord(db, valComp);

		return valComp;
	}

	public ContentValues getRecordComponente(DbInterno db, int idComponenteCantiere) {
		// TODO Auto-generated method stub
		ContentValues result = null;

		Join j0 = new Join(NOME_TABELLA, Componenti.NOME_TABELLA);
		j0.addCampiDiJoin(ID_COMPONENTE, Componenti.ID_COMPONENTE);

		Componenti tabComp = new Componenti();

		String SQL = "select " + getNomeCampoTabella("*") + "," + tabComp.getNomeCampoTabella("*") + " from " + NOME_TABELLA
				+ j0.getSQLJoin() + " where " + getNomeCampoTabella(ID_COMPONENTE_CANT) + "=" + idComponenteCantiere;

		ArrayList<Object> recs = db.eseguiSelect(SQL, null);
		if (recs.size() > 0) {
			result = (ContentValues) recs.get(0);
		}

		return result;
	}

	@Override
	protected void inserimentoCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub

		// verifico se c'� la composizione del componente
		int idComponentePadre = val.getAsInteger(ID_COMPONENTE);
		ComponentiComposti tabComp = new ComponentiComposti();
		ComponentiCantComposti tabCompCantComposti = new ComponentiCantComposti();

		ArrayList<Object> componenti = tabComp.getComposizioneComponente(db, idComponentePadre);
		for (int i = 0; i < componenti.size(); i++) {
			// inserisco il componente
			ContentValues figlioCant = inserisciComponenteCantiere(db, (ContentValues) componenti.get(i));
			int idFiglioCant = figlioCant.getAsInteger(ID_COMPONENTE_CANT);

			// inserisco la composizione
			ContentValues valINS = tabCompCantComposti.getValoriLogInserimento(db);
			valINS.put(ComponentiCantComposti.ID_COMPONENTE_CANT_PADRE, val.getAsInteger(ID_COMPONENTE_CANT));
			valINS.put(ComponentiCantComposti.ID_COMPONENTE_CANT_FIGLIO, idFiglioCant);
			tabCompCantComposti.inserisciRecord(db, valINS);

		}

		super.inserimentoCorrelati(db, val);
	}

	@Override
	protected void eliminaCorrelati(DbInterno db, ContentValues val) {
		// elimno gli eventuali componenti di questo componente
		int idComponentePadre = val.getAsInteger(ID_COMPONENTE_CANT);
		ComponentiCantComposti tabCompCantComposti = new ComponentiCantComposti();
		ArrayList<Object> componenti = tabCompCantComposti.getComposizioneComponente(db, idComponentePadre);
		for (int i = 0; i < componenti.size(); i++) {
			// cancello il record da ComponentiCantComposti: in automatico verr� cancellato anche da ComponenteCantiere
			// il figlio
			// con il metodo eliminaCorrelati
			ContentValues where = new ContentValues();
			where.put(ComponentiCantComposti.ID_COMPONENTE_CANT_PADRE, idComponentePadre);
			where.put(ComponentiCantComposti.ID_COMPONENTE_CANT_FIGLIO,
					((ContentValues) componenti.get(i)).getAsInteger(ComponentiCantComposti.ID_COMPONENTE_CANT_FIGLIO));
			tabCompCantComposti.cancellaRecord(db, where);
		}
		super.eliminaCorrelati(db, val);
	}

}
