package pfa.app.econtab.db.table;

public class IconeModificate extends AbstractTable {
    public static final String NOME_TABELLA = "icone_modificate";

    public static final String PERCORSO_FILE = "percorso_file";
    public static final String ID_ICONA_MODIFICATA = "id_icona_modificata";


    public IconeModificate(){
        setNomeTabella(NOME_TABELLA);

        setCampoNumeratore(ID_ICONA_MODIFICATA);
        aggiungiCampo(ID_ICONA_MODIFICATA,INTEGER);
        aggiungiCampo(PERCORSO_FILE,TEXT);
        aggiungiCampo(ID_OPERATORE_INS, INTEGER);
        aggiungiCampo(DATA_INS, DATE);
        aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
        aggiungiCampo(DATA_MOD, DATE);

        aggiungiCampoChiave(ID_ICONA_MODIFICATA);
    }
}
