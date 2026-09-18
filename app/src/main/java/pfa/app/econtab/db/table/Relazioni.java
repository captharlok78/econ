package pfa.app.econtab.db.table;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;

/**
 * Created by daniele on 12/01/2015.
 */
public class Relazioni extends AbstractTable {
    public static final String NOME_TABELLA = "relazioni";

    public static final String ID_RELAZIONE = "id_relazione";
    public static final String ID_ELEMENTO_CANT1 = "id_elemento_cant1";
    public static final String ID_COMPONENTE_CANT1 = "id_componente_cant1";
    public static final String ID_ELEMENTO_CANT2 = "id_elemento_cant2";
    public static final String ID_COMPONENTE_CANT2 = "id_componente_cant2";


    public Relazioni() {
        setNomeTabella(NOME_TABELLA);
        setCampoNumeratore(ID_RELAZIONE);
        aggiungiCampo(ID_RELAZIONE, INTEGER);
        aggiungiCampo(ID_ELEMENTO_CANT1, INTEGER);
        aggiungiCampo(ID_COMPONENTE_CANT1, INTEGER);
        aggiungiCampo(ID_ELEMENTO_CANT2, INTEGER);
        aggiungiCampo(ID_COMPONENTE_CANT2, INTEGER);
        aggiungiCampo(ID_OPERATORE_INS, INTEGER);
        aggiungiCampo(DATA_INS, DATE);
        aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
        aggiungiCampo(DATA_MOD, DATE);
        aggiungiCampo(IN_SERVER, INTEGER);

        aggiungiCampoChiave(ID_RELAZIONE);
    }

    public ArrayList<Integer> getComponentiCollegatiElemento(DbInterno db, int idElemento) {

            String SQL = "Select * from " + Relazioni.NOME_TABELLA + " where (" + Relazioni.ID_ELEMENTO_CANT1 + " = " + idElemento
                    + " or " + Relazioni.ID_ELEMENTO_CANT2 + " = " + idElemento + ") ";
            ArrayList<Object> collegamentiElemento = db.eseguiSelect(SQL, null);
            ArrayList<Integer> componentiCollegati = new ArrayList<Integer>();
            for (int i = 0; i < collegamentiElemento.size(); i++) {

                ContentValues curr = (ContentValues) collegamentiElemento.get(i);
                int comp1 = curr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT1);
                int comp2 = curr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT2);
                if (comp1 != 0 && !componentiCollegati.contains(comp1)) {
                    componentiCollegati.add(comp1);
                }
                if (comp2 != 0 && !componentiCollegati.contains(comp2)) {
                    componentiCollegati.add(comp2);
                }
            }
            return componentiCollegati;

    }


    public ArrayList<Object> getComponentiCollegatiComponente(DbInterno db, int idComponenteCant) {

        String SQL = "Select * from " + Relazioni.NOME_TABELLA + " where (" + Relazioni.ID_COMPONENTE_CANT1 + " = " + idComponenteCant
                + " or " + Relazioni.ID_COMPONENTE_CANT2 + " = " + idComponenteCant + ") ";
        ArrayList<Object> collegamentiComponenti = db.eseguiSelect(SQL, null);

        return collegamentiComponenti;

    }
}
