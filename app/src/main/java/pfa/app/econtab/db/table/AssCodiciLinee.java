package pfa.app.econtab.db.table;

/**
 * Created by daniele on 13/01/2018.
 */

public class AssCodiciLinee extends AbstractTable {
    public static final String NOME_TABELLA = "ass_codici_linee";

    public static final String CODICE_ARTICOLO = "codice_articolo";
    public static final String ID_LINEA = "id_linea";


    public AssCodiciLinee(){
        setNomeTabella(NOME_TABELLA);


        aggiungiCampo(CODICE_ARTICOLO, TEXT);
        aggiungiCampo(ID_LINEA, INTEGER);
        aggiungiCampo(ID_OPERATORE_INS, INTEGER);
        aggiungiCampo(DATA_INS, DATE);
        aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
        aggiungiCampo(DATA_MOD, DATE);
        aggiungiCampo(IN_SERVER, INTEGER);


        aggiungiCampoChiave(CODICE_ARTICOLO);
        aggiungiCampoChiave(ID_LINEA);
    }
}
