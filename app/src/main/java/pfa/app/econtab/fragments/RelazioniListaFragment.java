package pfa.app.econtab.fragments;


import android.app.Fragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.RelazioneActivity;
import pfa.app.econtab.adapters.RelazioniListaAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Aree;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.ComponentiCantiere;
import pfa.app.econtab.db.table.ComposizioniCantiere;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Relazioni;
import pfa.app.econtab.utils.Utility;

/**
 * A simple {@link Fragment} subclass.
 */
public class RelazioniListaFragment extends EConTabFragment implements OnItemClickListener {


    private boolean daCantiere = false;

    private ListView lista = null;
    private RelazioniListaAdapter adapter = null;

    private ArrayList<Object> datiAll = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = super.onCreateView(inflater, container, savedInstanceState);
        v = inflater.inflate(R.layout.fragment_relazioni_lista, container, false);


        if (getArguments().containsKey("DA_CANTIERE")) {
            daCantiere = true;
        }


        lista = (ListView) v.findViewById(R.id.lista);
        lista.setOnItemClickListener(this);

        return v;
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        refresh();
    }


    public void refresh() {
        lista.invalidate();
        if (datiAll == null) {
            datiAll = new ArrayList<Object>();
        }
        datiAll.clear();
        DbInterno db = new DbInterno(getActivity());

        ArrayList<Integer> ordineLocali = new ArrayList();
        ArrayList<String> locali = new ArrayList();
        if (daCantiere) {
            int idCantiere = getArguments().getInt(Cantieri.ID_CANTIERE);
            Locali tabLocali = new Locali();
            Aree tabAree = new Aree();
            ArrayList localiCantiere = tabLocali.getLocaliCantiereConArea(db, idCantiere);
            for (int i=0;i<localiCantiere.size();i++){
                ContentValues currLocale = (ContentValues)localiCantiere.get(i);
                ordineLocali.add(currLocale.getAsInteger(Locali.ID_LOCALE));
                locali.add(currLocale.getAsString(tabAree.getAlias(Aree.NOME))+" - " + currLocale.getAsString(Locali.NOME));
            }


        } else {
            int idLocale = getArguments().getInt(Locali.ID_LOCALE);
            ordineLocali.add(idLocale);
            locali.add("");
        }

        for (int l=0;l<ordineLocali.size();l++){
            int idLocale = ordineLocali.get(l);
            String nomeLocale = locali.get(l);

            Join j0 = new Join(Relazioni.NOME_TABELLA, ElementiCantiere.NOME_TABELLA);
            j0.addCampiDiJoin(Relazioni.ID_ELEMENTO_CANT1, ElementiCantiere.ID_ELEMENTO_CANT);// join tra elemento comandato e elementocantiere

            Join j1 = new Join(ElementiCantiere.NOME_TABELLA, Locali.NOME_TABELLA);
            j1.addCampiDiJoin(ElementiCantiere.ID_LOCALE, Locali.ID_LOCALE);

            Join j2 = new Join(Locali.NOME_TABELLA, Aree.NOME_TABELLA);
            j2.addCampiDiJoin(Locali.ID_AREA, Aree.ID_AREA);

            Join j3 = new Join(ElementiCantiere.NOME_TABELLA, Elementi.NOME_TABELLA);
            j3.addCampiDiJoin(ElementiCantiere.ID_ELEMENTO, Elementi.ID_ELEMENTO);// join tra elemento comandato e elementocantiere


            String SQLREL = "Select " + Relazioni.NOME_TABELLA + ".*," + Aree.NOME_TABELLA + "." + Aree.NOME + " as nome_area," + Locali.NOME_TABELLA + "." + Locali.ID_LOCALE + " as id_locale," + Locali.NOME_TABELLA + "." + Locali.NOME + " as nome_locale," +
                    ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.ID_ELEMENTO + "," + ElementiCantiere.NUMERO_IDENTIFICATIVO + "," + ElementiCantiere.NOME_ELEMENTO_CANT + "," + Elementi.ID_CATEGORIA_GENERALE + " " +
                    "from " + Relazioni.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + j2.getSQLJoin() + j3.getSQLJoin() + " where " + ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.ID_LOCALE + "=" + idLocale + " order by " + ElementiCantiere.NUMERO_IDENTIFICATIVO;


            ArrayList datiLocale = db.eseguiSelect(SQLREL, null);
            String filtro_id_relazioni = "";
            for (int i = 0; i < datiLocale.size(); i++) {
                ContentValues curr = (ContentValues) datiLocale.get(i);
                int idRelazioneCurr = curr.getAsInteger(Relazioni.ID_RELAZIONE);
                filtro_id_relazioni = filtro_id_relazioni + idRelazioneCurr + ",";
            }

            ArrayList<Object> elementiComando = new ArrayList<Object>();
            if (!filtro_id_relazioni.equals("")) {
                filtro_id_relazioni = filtro_id_relazioni.substring(0, filtro_id_relazioni.length() - 1);
                filtro_id_relazioni = "(" + filtro_id_relazioni + ")";
                //faccio praticamente la stessa query ma in join con idElemento2 per prendere i dati dell'elemnto di comando
                Join j0C = new Join(Relazioni.NOME_TABELLA, ElementiCantiere.NOME_TABELLA);
                j0C.addCampiDiJoin(Relazioni.ID_ELEMENTO_CANT2, ElementiCantiere.ID_ELEMENTO_CANT);

                Join j1C = new Join(ElementiCantiere.NOME_TABELLA, Locali.NOME_TABELLA);
                j1C.addCampiDiJoin(ElementiCantiere.ID_LOCALE, Locali.ID_LOCALE);

                Join j2C = new Join(Locali.NOME_TABELLA, Aree.NOME_TABELLA);
                j2C.addCampiDiJoin(Locali.ID_AREA, Aree.ID_AREA);

                Join j3C = new Join(ElementiCantiere.NOME_TABELLA, Elementi.NOME_TABELLA);
                j3C.addCampiDiJoin(ElementiCantiere.ID_ELEMENTO, Elementi.ID_ELEMENTO);


                String SQLREL_C = "Select " + Relazioni.NOME_TABELLA + ".*," + Aree.NOME_TABELLA + "." + Aree.NOME + " as nome_area," + Locali.NOME_TABELLA + "." + Locali.ID_LOCALE + " as id_locale," + Locali.NOME_TABELLA + "." + Locali.NOME + " as nome_locale," +
                        ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.ID_ELEMENTO + "," + ElementiCantiere.NUMERO_IDENTIFICATIVO + "," + ElementiCantiere.NOME_ELEMENTO_CANT + "," + Elementi.ID_CATEGORIA_GENERALE + " " +
                        "from " + Relazioni.NOME_TABELLA + j0C.getSQLJoin() + j1C.getSQLJoin() + j2C.getSQLJoin() + j3C.getSQLJoin() + " where " + Relazioni.ID_RELAZIONE + " in " + filtro_id_relazioni + " order by " + ElementiCantiere.NUMERO_IDENTIFICATIVO;

                elementiComando = db.eseguiSelect(SQLREL_C, null);
            }

            for (int i = 0; i < elementiComando.size(); i++) {
                _aggiungiDatiComponente((ContentValues) elementiComando.get(i), db, true);
            }


            for (int i = 0; i < datiLocale.size(); i++) {
                ContentValues curr = (ContentValues) datiLocale.get(i);

                _aggiungiDatiComponente(curr, db, false);

                int idRelazioneCurr = curr.getAsInteger(Relazioni.ID_RELAZIONE);
                //aggiungo i dati sull'elemento di comando
                for (int j = 0; j < elementiComando.size(); j++) {
                    ContentValues currComando = (ContentValues) elementiComando.get(j);
                    if (idRelazioneCurr == currComando.getAsInteger(Relazioni.ID_RELAZIONE)) {
                        curr.put("ID_LOCALE_COMANDO", currComando.getAsInteger("id_locale"));
                        curr.put("AREA_COMANDO", currComando.getAsString("nome_area"));
                        curr.put("LOCALE_COMANDO", currComando.getAsString("nome_locale"));
                        curr.put("NUM_ELEMENTO_COMANDO", currComando.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO));
                        curr.put("ID_CATEGORIA_COMANDO", currComando.getAsInteger(Elementi.ID_CATEGORIA_GENERALE));
                        curr.put("NOME_ELEMENTO_COMANDO", currComando.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
                        curr.put("COMPONENTE_COMANDO", currComando.getAsString("COMPONENTE_COMANDATO"));
                    }

                }

            }
            
            //a questo punto prendo anche eventuali relazioni che appartengono a questo locale ma solo come punto di comando
            Join j0_2 = new Join(Relazioni.NOME_TABELLA, ElementiCantiere.NOME_TABELLA);
            j0_2.addCampiDiJoin(Relazioni.ID_ELEMENTO_CANT2, ElementiCantiere.ID_ELEMENTO_CANT);// join tra elemento comandato e elementocantiere

            Join j1_2 = new Join(ElementiCantiere.NOME_TABELLA, Locali.NOME_TABELLA);
            j1_2.addCampiDiJoin(ElementiCantiere.ID_LOCALE, Locali.ID_LOCALE);

            Join j2_2 = new Join(Locali.NOME_TABELLA, Aree.NOME_TABELLA);
            j2_2.addCampiDiJoin(Locali.ID_AREA, Aree.ID_AREA);

            Join j3_2 = new Join(ElementiCantiere.NOME_TABELLA, Elementi.NOME_TABELLA);
            j3_2.addCampiDiJoin(ElementiCantiere.ID_ELEMENTO, Elementi.ID_ELEMENTO);// join tra elemento comandato e elementocantiere

            String filtroEscludiRelazioniGiaProcessate = "";
            if (!filtro_id_relazioni.equals("")) {
                filtroEscludiRelazioniGiaProcessate = " and " + Relazioni.ID_RELAZIONE + " not in " + filtro_id_relazioni;
            }

            String SQLREL_2 = "Select " + Relazioni.NOME_TABELLA + ".*," + Aree.NOME_TABELLA + "." + Aree.NOME + " as nome_area," + Locali.NOME_TABELLA + "." + Locali.ID_LOCALE + " as id_locale," + Locali.NOME_TABELLA + "." + Locali.NOME + " as nome_locale," +
                    ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.ID_ELEMENTO + "," + ElementiCantiere.NUMERO_IDENTIFICATIVO + "," + ElementiCantiere.NOME_ELEMENTO_CANT + "," + Elementi.ID_CATEGORIA_GENERALE + " " +
                    "from " + Relazioni.NOME_TABELLA + j0_2.getSQLJoin() + j1_2.getSQLJoin() + j2_2.getSQLJoin() + j3_2.getSQLJoin() + " where " + ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.ID_LOCALE + "=" + idLocale + filtroEscludiRelazioniGiaProcessate + " order by " + ElementiCantiere.NUMERO_IDENTIFICATIVO;
            ArrayList<Object> dati2 = db.eseguiSelect(SQLREL_2, null);
            String filtro_id_relazioni_2 = "";
            for (int i = 0; i < dati2.size(); i++) {
                ContentValues curr = (ContentValues) dati2.get(i);
                int idRelazioneCurr = curr.getAsInteger(Relazioni.ID_RELAZIONE);
                filtro_id_relazioni_2 = filtro_id_relazioni_2 + idRelazioneCurr + ",";
            }

            ArrayList<Object> elementiComando2 = new ArrayList<Object>();
            if (!filtro_id_relazioni_2.equals("")) {
                filtro_id_relazioni_2 = filtro_id_relazioni_2.substring(0, filtro_id_relazioni_2.length() - 1);
                filtro_id_relazioni_2 = "(" + filtro_id_relazioni_2 + ")";
                //faccio praticamente la stessa query ma in join con idElemento2 per prendere i dati dell'elemnto di comando
                Join j0C = new Join(Relazioni.NOME_TABELLA, ElementiCantiere.NOME_TABELLA);
                j0C.addCampiDiJoin(Relazioni.ID_ELEMENTO_CANT1, ElementiCantiere.ID_ELEMENTO_CANT);

                Join j1C = new Join(ElementiCantiere.NOME_TABELLA, Locali.NOME_TABELLA);
                j1C.addCampiDiJoin(ElementiCantiere.ID_LOCALE, Locali.ID_LOCALE);

                Join j2C = new Join(Locali.NOME_TABELLA, Aree.NOME_TABELLA);
                j2C.addCampiDiJoin(Locali.ID_AREA, Aree.ID_AREA);

                Join j3C = new Join(ElementiCantiere.NOME_TABELLA, Elementi.NOME_TABELLA);
                j3C.addCampiDiJoin(ElementiCantiere.ID_ELEMENTO, Elementi.ID_ELEMENTO);


                String SQLREL_C = "Select " + Relazioni.NOME_TABELLA + ".*," + Aree.NOME_TABELLA + "." + Aree.NOME + " as nome_area," + Locali.NOME_TABELLA + "." + Locali.ID_LOCALE + " as id_locale," + Locali.NOME_TABELLA + "." + Locali.NOME + " as nome_locale," +
                        ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.ID_ELEMENTO + "," + ElementiCantiere.NUMERO_IDENTIFICATIVO + "," + ElementiCantiere.NOME_ELEMENTO_CANT + "," + Elementi.ID_CATEGORIA_GENERALE + " " +
                        "from " + Relazioni.NOME_TABELLA + j0C.getSQLJoin() + j1C.getSQLJoin() + j2C.getSQLJoin() + j3C.getSQLJoin() + " where " + Relazioni.ID_RELAZIONE + " in " + filtro_id_relazioni_2 + " order by " + ElementiCantiere.NUMERO_IDENTIFICATIVO;

                elementiComando2 = db.eseguiSelect(SQLREL_C, null);
            }

            for (int i = 0; i < elementiComando2.size(); i++) {
                _aggiungiDatiComponente((ContentValues) elementiComando2.get(i), db, false);
            }


            for (int i = 0; i < dati2.size(); i++) {
                ContentValues curr = (ContentValues) dati2.get(i);


                _aggiungiDatiComponente(curr, db, true);

                //inverto le posizioni dei campi da mostrare sull'adapter perchè dalla query sarebbero sbagliati (id_elemento_cantiere 2 invece che id_elemento_cantiere_1)
                curr.put("ID_LOCALE_COMANDO", curr.getAsInteger("id_locale"));
                curr.put("AREA_COMANDO", curr.getAsString("nome_area"));
                curr.put("LOCALE_COMANDO", curr.getAsString("nome_locale"));
                curr.put("NUM_ELEMENTO_COMANDO", curr.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO));
                curr.put("ID_CATEGORIA_COMANDO", curr.getAsInteger(Elementi.ID_CATEGORIA_GENERALE));
                curr.put("NOME_ELEMENTO_COMANDO", curr.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
                curr.put("COMPONENTE_COMANDO", curr.getAsString("COMPONENTE_COMANDATO"));

                int idRelazioneCurr = curr.getAsInteger(Relazioni.ID_RELAZIONE);
                //aggiungo i dati sull'elemento di comando
                for (int j = 0; j < elementiComando2.size(); j++) {
                    ContentValues currComando = (ContentValues) elementiComando2.get(j);
                    if (idRelazioneCurr == currComando.getAsInteger(Relazioni.ID_RELAZIONE)) {
                        curr.put("id_locale", currComando.getAsInteger("id_locale"));
                        curr.put("nome_area", currComando.getAsString("nome_area"));
                        curr.put("nome_locale", currComando.getAsString("nome_locale"));
                        curr.put(ElementiCantiere.NUMERO_IDENTIFICATIVO, currComando.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO));
                        curr.put(Elementi.ID_CATEGORIA_GENERALE, currComando.getAsInteger(Elementi.ID_CATEGORIA_GENERALE));
                        curr.put(ElementiCantiere.NOME_ELEMENTO_CANT, currComando.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
                        curr.put("COMPONENTE_COMANDATO", currComando.getAsString("COMPONENTE_COMANDATO"));
                    }
                }
            }

            datiLocale.addAll(dati2);
            for (int i = 0; i < datiLocale.size(); i++) {
                ContentValues curr = (ContentValues) datiLocale.get(i);
                curr.put("id_locale_corrente", idLocale);
            }

            //aggiungo ai dati totali i dati del locale
            if (daCantiere && datiLocale.size()>0){
                ContentValues valLocale = new ContentValues();
                valLocale.put("TITOLO_LOCALE",nomeLocale);
                datiAll.add(valLocale);
            }
            datiAll.addAll(datiLocale);

        }


        db.close();
        if (adapter == null) {
            adapter = new RelazioniListaAdapter(getActivity(), datiAll, R.layout.list_item_relazione_lista);


            lista.setAdapter(adapter);
            registerForContextMenu(lista);
        } else {
            adapter.notifyDataSetChanged();

        }
    }

    private void _aggiungiDatiComponente(ContentValues curr, DbInterno db, boolean comando) {
        ContentValues whereComp = new ContentValues();
        ContentValues recComp = null;
        ComponentiCantiere tabComp = new ComponentiCantiere();
        curr.put("COMPONENTE_COMANDATO", "");
        int idComponente = 0;
        int idElemento = 0;

        try {
            if (comando) {
                idComponente = curr.getAsInteger(Relazioni.ID_COMPONENTE_CANT2);
                idElemento = curr.getAsInteger(Relazioni.ID_ELEMENTO_CANT2);
            } else {
                idComponente = curr.getAsInteger(Relazioni.ID_COMPONENTE_CANT1);
                idElemento = curr.getAsInteger(Relazioni.ID_ELEMENTO_CANT1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (idComponente != 0) {
            whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponente);
            recComp = db.getRecord(tabComp, whereComp);
            if (recComp != null) {
                // prendo la composizione
                String moduliOccupati = "";
                ContentValues whereCompos = new ContentValues();
                whereCompos.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElemento);
                whereCompos.put(ComposizioniCantiere.ID_COMPONENTE_CANT, idComponente);
                ContentValues valCompos = db.getRecord(new ComposizioniCantiere(), whereCompos);
                if (valCompos != null) {
                    moduliOccupati = valCompos.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
                }
                curr.put("COMPONENTE_COMANDATO", recComp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT) + " (Posto " + moduliOccupati + ")");
            }
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        ContentValues item = (ContentValues) lista.getItemAtPosition(i);
        apriDettaglioRelazione(item);
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        //super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        ContentValues item = (ContentValues) lista.getItemAtPosition(info.position);
        menu.setHeaderTitle(getString(R.string.relazione));
        menu.add(Menu.NONE, 100, Menu.NONE, getString(R.string.modifica));
        menu.add(Menu.NONE, 200, Menu.NONE, getString(R.string.elimina));

        //devo fare così perchè altrimenti usa il listener di default dell'activity e potrebbe non chiamare onContextItemSelected
        MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onContextItemSelected(item);
                return true;
            }
        };

        for (int i = 0; i <  menu.size(); i++){
            menu.getItem(i).setOnMenuItemClickListener(listener);
        }



    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (getUserVisibleHint()) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            ContentValues val = (ContentValues) lista.getItemAtPosition(info.position);
            if (item.getItemId() == 100) {
                apriDettaglioRelazione(val);
            }
            if (item.getItemId() == 200) {
                eliminaRelazione(val);
            }
            return true;
        }

        return false;
    }

    private void apriDettaglioRelazione(ContentValues val) {

        Intent intent = new Intent(getActivity(), RelazioneActivity.class);
        int idRelazione = val.getAsInteger(Relazioni.ID_RELAZIONE);
        intent.putExtra(Relazioni.ID_RELAZIONE, idRelazione);
        startActivity(intent);
    }

    private void eliminaRelazione(final ContentValues val) {
        // TODO Auto-generated method stub
        Utility.mostraConfermaCancellazioneDialog(getActivity(), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    int idRelazione = val.getAsInteger(Relazioni.ID_RELAZIONE);
                    DbInterno db = new DbInterno(getActivity());
                    Relazioni tabCollegamenti = new Relazioni();
                    ContentValues valDel = new ContentValues();
                    valDel.put(Relazioni.ID_RELAZIONE, idRelazione);
                    tabCollegamenti.cancellaRecord(db, valDel);
                    db.close();
                    refresh();
                }
            }
        });

    }
}
