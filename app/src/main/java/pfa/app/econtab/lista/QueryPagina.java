package pfa.app.econtab.lista;

/**
 * Query costruita da un modulo (via EConTabListaStandardDefinition.costruisciQuery): la
 * parte "from ... [join ...]" e la parte where (con i relativi argomenti). L'ordinamento
 * e la paginazione (limit/offset) sono gestiti da EConTabListaStandardController, non fanno
 * parte di questo oggetto.
 *
 * selectSql e' la lista di colonne selezionate (default "*"): quando fromJoinSql e' un JOIN
 * fra tabelle che condividono nomi di campo (es. Cantieri e Anagrafica hanno entrambe
 * "citta"/"provincia" e i campi di audit di AbstractTable), un modulo deve selezionare
 * esplicitamente solo le colonne che gli servono (es. "cantieri.*, anagrafica.ragione_sociale")
 * per evitare che una colonna sovrascriva l'altra nel risultato.
 */
public class QueryPagina {
    public final String selectSql;
    public final String fromJoinSql;
    public final String whereSql;
    public final String[] whereArgs;

    public QueryPagina(String fromJoinSql, String whereSql, String[] whereArgs) {
        this("*", fromJoinSql, whereSql, whereArgs);
    }

    public QueryPagina(String selectSql, String fromJoinSql, String whereSql, String[] whereArgs) {
        this.selectSql = selectSql != null ? selectSql : "*";
        this.fromJoinSql = fromJoinSql;
        this.whereSql = whereSql != null ? whereSql : "";
        this.whereArgs = whereArgs != null ? whereArgs : new String[0];
    }
}
