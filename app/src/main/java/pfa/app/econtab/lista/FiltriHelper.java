package pfa.app.econtab.lista;

/**
 * Pattern di filtro ricorrenti tra i moduli lista standard, per evitare di riscrivere la
 * stessa costruzione di WHERE/argomenti in ogni EConTabListaStandardDefinition.
 */
public class FiltriHelper {

    private FiltriHelper() {
    }

    /** Ricerca libera "or"-ata su piu' colonne (like), es. ragione sociale/indirizzo/citta'. */
    public static QueryPagina likeMultiCampo(String fromJoinSql, String testo, String... campi) {
        String like = (testo == null || testo.trim().isEmpty()) ? "%" : "%" + testo.trim() + "%";
        StringBuilder where = new StringBuilder();
        String[] args = new String[campi.length];
        for (int i = 0; i < campi.length; i++) {
            if (i > 0) {
                where.append(" or ");
            }
            where.append(campi[i]).append(" like ?");
            args[i] = like;
        }
        return new QueryPagina(fromJoinSql, where.toString(), args);
    }
}
