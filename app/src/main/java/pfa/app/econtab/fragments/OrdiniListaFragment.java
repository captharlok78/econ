package pfa.app.econtab.fragments;

import android.content.ContentValues;

import java.util.ArrayList;

import pfa.app.econtab.db.table.Preventivi;

/**
 * Modulo Ordini: stessa tabella/query/adapter di Preventivi (vedi PreventiviListaFragment),
 * cambia solo il tipo di documento e le opzioni/il default della spinner di stato - prima
 * era una classe quasi interamente duplicata (stesso layout, stesso adapter, query quasi
 * identica), con in piu' un bug latente (anno di default hardcoded "2014" invece
 * dell'anno corrente).
 */
public class OrdiniListaFragment extends PreventiviListaFragment {

    @Override
    protected String getTipo() {
        return Preventivi.TIPO_ORDINE;
    }

    @Override
    protected ArrayList<Object> getOpzioniStato() {
        ArrayList<Object> stati = new Preventivi().getStatiOrdine(getActivity());
        ContentValues valTutti = new ContentValues();
        valTutti.put("VAL", "");
        valTutti.put("DESC", "");
        stati.add(0, valTutti);
        return stati;
    }

    @Override
    protected String getStatoDefault() {
        return Preventivi.STATO_APERTO;
    }

    @Override
    protected String getTitoloModulo() {
        return "Ordini";
    }
}
