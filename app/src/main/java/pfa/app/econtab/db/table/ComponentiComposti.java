package pfa.app.econtab.db.table;

import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;

public class ComponentiComposti extends AbstractTable {

	public static final String NOME_TABELLA = "componenti_composti";

	public static final String ID_COMPONENTE_COMPOSTO = "id_componente_composto";
	public static final String ID_COMPONENTE_PADRE = "id_componente_padre";
	public static final String ID_COMPONENTE_FIGLIO = "id_componente_figlio";

	public ComponentiComposti() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_COMPONENTE_COMPOSTO);

		aggiungiCampo(ID_COMPONENTE_COMPOSTO, INTEGER);
		aggiungiCampo(ID_COMPONENTE_PADRE, INTEGER);
		aggiungiCampo(ID_COMPONENTE_FIGLIO, INTEGER);

		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_COMPONENTE_COMPOSTO);

	}

	public ArrayList<Object> getComposizioneComponente(DbInterno db, int idComponentePadre) {

		Join j0 = new Join(NOME_TABELLA, Componenti.NOME_TABELLA);
		j0.addCampiDiJoin(ID_COMPONENTE_FIGLIO, Componenti.ID_COMPONENTE);

		String SQL = "Select " + Componenti.NOME_TABELLA + ".* from " + NOME_TABELLA + j0.getSQLJoin() + " where " + ID_COMPONENTE_PADRE
				+ "=" + idComponentePadre;

		ArrayList<Object> composizione = db.eseguiSelect(SQL, null);
		return composizione;
	}

}
