package pfa.app.econtab.db.table;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;

public class ComponentiCantComposti extends AbstractTable {
	public static final String NOME_TABELLA = "componenti_cant_composti";

	public static final String ID_COMPONENTE_CANT_PADRE = "id_componente_cant_padre";
	public static final String ID_COMPONENTE_CANT_FIGLIO = "id_componente_cant_figlio";

	public ComponentiCantComposti() {
		setNomeTabella(NOME_TABELLA);

		aggiungiCampo(ID_COMPONENTE_CANT_PADRE, INTEGER);
		aggiungiCampo(ID_COMPONENTE_CANT_FIGLIO, INTEGER);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_COMPONENTE_CANT_PADRE);
		aggiungiCampoChiave(ID_COMPONENTE_CANT_FIGLIO);
	}

	public ArrayList<Object> getComposizioneComponente(DbInterno db, int idComponentePadre) {
		// TODO Auto-generated method stub
		Join j0 = new Join(NOME_TABELLA, ComponentiCantiere.NOME_TABELLA);
		j0.addCampiDiJoin(ID_COMPONENTE_CANT_FIGLIO, ComponentiCantiere.ID_COMPONENTE_CANT);

		Join j1 = new Join(ComponentiCantiere.NOME_TABELLA, Componenti.NOME_TABELLA);
		j1.addCampiDiJoin(ComponentiCantiere.ID_COMPONENTE, Componenti.ID_COMPONENTE);

		String SQL = "Select " + ID_COMPONENTE_CANT_FIGLIO + "," + ComponentiCantiere.NOME_COMPONENTE_CANT + "," + Componenti.NOME_TABELLA
				+ ".* from " + NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where " + ID_COMPONENTE_CANT_PADRE + "="
				+ idComponentePadre;

		ArrayList<Object> composizione = db.eseguiSelect(SQL, null);
		return composizione;
	}

	@Override
	protected void eliminaCorrelati(DbInterno db, ContentValues val) {
		int idComponentePadre = val.getAsInteger(ID_COMPONENTE_CANT_PADRE);

		// trovo in quale elemento_cantiere � presente il componente padre
	/*	ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
		ContentValues valElem = tabComposizioni.getElementoCantiereDaComponente(db, idComponentePadre);
		if (valElem != null && valElem.getAsInteger(ElementiCantiere.ID_PREVENTIVO) != 0) {
			int idPreventivo = valElem.getAsInteger(ElementiCantiere.ID_PREVENTIVO);
			PreventiviDettaglio tabPRev = new PreventiviDettaglio();
			ContentValues where = new ContentValues();
			where.put(ComponentiCantiere.ID_COMPONENTE_CANT, val.getAsInteger(ID_COMPONENTE_CANT_FIGLIO));
			ContentValues valori = db.getRecord(new ComponentiCantiere(), where);
			valori.put(ElementiCantiere.ID_LOCALE, valElem.getAsInteger(ElementiCantiere.ID_LOCALE));

			tabPRev.aggiornaRigaPreventivo(db, idPreventivo, valori, -1, PreventiviDettaglio.MATERIALE, true);
		}*/

		// Elimino anche il ComponenteCantiere Figlio perch� si troverebbe slegato da qualsiasi relazione
		int idFiglio = val.getAsInteger(ID_COMPONENTE_CANT_FIGLIO);
		ContentValues whereCant = new ContentValues();
		whereCant.put(ComponentiCantiere.ID_COMPONENTE_CANT, idFiglio);

		ComponentiCantiere tabCompCant = new ComponentiCantiere();
		tabCompCant.cancellaRecord(db, whereCant);

		super.eliminaCorrelati(db, val);
	}

	@Override
	protected void inserimentoCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
	/*	int idComponentePadre = val.getAsInteger(ID_COMPONENTE_CANT_PADRE);

		// trovo in quale elemento_cantiere � presente il componente padre
		ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
		ContentValues valElem = tabComposizioni.getElementoCantiereDaComponente(db, idComponentePadre);
		if (valElem != null && valElem.getAsInteger(ElementiCantiere.ID_PREVENTIVO) != 0) {
			int idPreventivo = valElem.getAsInteger(ElementiCantiere.ID_PREVENTIVO);
			PreventiviDettaglio tabPRev = new PreventiviDettaglio();
			ContentValues where = new ContentValues();
			where.put(ComponentiCantiere.ID_COMPONENTE_CANT, val.getAsInteger(ID_COMPONENTE_CANT_FIGLIO));
			ContentValues valori = db.getRecord(new ComponentiCantiere(), where);
			valori.put(ElementiCantiere.ID_LOCALE, valElem.getAsInteger(ElementiCantiere.ID_LOCALE));

			tabPRev.aggiornaRigaPreventivo(db, idPreventivo, valori, 1, PreventiviDettaglio.MATERIALE, true);

		}*/

		super.inserimentoCorrelati(db, val);
	}

}
