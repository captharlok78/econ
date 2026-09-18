package pfa.app.econtab.db;

import android.content.ContentValues;

import java.util.Iterator;

public class Join {

	public static final String LEFT_JOIN = "left join";
	public static final String INNER_JOIN = "inner join";
	public static final String RIGHT_JOIN = "right join";

	String tabella1 = "";
	String tabella2 = "";
	String tipo = "";

	String aliasTabella = null;

	ContentValues campijoin = null;

	public Join(String tabella1, String tabella2) {
		this(tabella1, tabella2, INNER_JOIN);
	}

	public Join(String tabella1, String tabella2, String tipo) {
		this.tabella1 = tabella1;
		this.tabella2 = tabella2;
		this.tipo = tipo;
	}

	public void addCampiDiJoin(String campo1, String campo2) {
		if (campijoin == null) {
			campijoin = new ContentValues();
		}

		campijoin.put(campo1, campo2);

	}

	public String getSQLJoin() {

		String aliasPerQuery = "";
		String aliasTabella2 = tabella2;
		if (aliasTabella != null) {
			aliasTabella2 = aliasTabella;
			aliasPerQuery = " as " + aliasTabella;
		}
		String SQL = "";

		SQL = " " + tipo + " ";
		SQL = SQL + tabella2 + aliasPerQuery + " on ";
		if (campijoin != null) {
			Iterator<String> iter = campijoin.keySet().iterator();
			boolean first = true;
			while (iter.hasNext()) {
				String campo1 = iter.next();
				if (first) {
					first = false;
					SQL = SQL + tabella1 + "." + campo1 + "=" + aliasTabella2 + "." + campijoin.getAsString(campo1);
				} else {
					SQL = SQL + " and " + tabella1 + "." + campo1 + "=" + aliasTabella2 + "." + campijoin.getAsString(campo1);
				}
			}
		}

		return SQL;
	}

	public void setAliasTabella(String alias) {
		aliasTabella = alias;
	}

}
