package pfa.app.econtab.lista;

/**
 * Query costruita da un modulo (via EConTabListaStandardDefinition.costruisciQuery): la
 * parte "from ... [join ...]" e la parte where (con i relativi argomenti). L'ordinamento
 * e la paginazione (limit/offset) sono gestiti da EConTabListaStandardController, non fanno
 * parte di questo oggetto.
 */
public class QueryPagina {
    public final String fromJoinSql;
    public final String whereSql;
    public final String[] whereArgs;

    public QueryPagina(String fromJoinSql, String whereSql, String[] whereArgs) {
        this.fromJoinSql = fromJoinSql;
        this.whereSql = whereSql != null ? whereSql : "";
        this.whereArgs = whereArgs != null ? whereArgs : new String[0];
    }
}
