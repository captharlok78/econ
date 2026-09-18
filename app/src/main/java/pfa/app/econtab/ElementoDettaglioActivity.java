package pfa.app.econtab;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Aree;
import pfa.app.econtab.db.table.CategorieGenerali;
import pfa.app.econtab.db.table.Collegamenti;
import pfa.app.econtab.db.table.ComponentiCantiere;
import pfa.app.econtab.db.table.ComposizioniCantiere;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.Relazioni;
import pfa.app.econtab.utils.Utility;

public class ElementoDettaglioActivity extends EConTabActivity implements OnClickListener, OnLongClickListener {

    private int idElementoCantiere = 0;

    private ImageView iconaElemento = null;
    private int idPreventivo = 0;
    private int idPreventivoSelezionato = 0;
    private int idLocale = 0;
    private boolean modificheBloccate = false;
    private int idCategoria = 0;

    private ImageButton buttonrelazioni = null;
    private ImageButton buttonmodificca = null;
    private ImageButton buttonNonConteggiare = null;

    private ArrayList<Object> relazioni = null;
    private ArrayList<Object> relazioniLogiche = null;

    private LinearLayout lista_relazioni = null;
    private LinearLayout lista_relazioni_logiche = null;
    private TextView nota = null;
    private int nonConteggiarePreventivi;
    private boolean elementoAltroOrdine = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("EConTab: ElementoDettaglioActivity onCreate ENTER");
        super.onCreate(savedInstanceState);
        setVisualizzazionePopup();
        setContentView(R.layout.activity_elemento_dettaglio);

        idElementoCantiere = getIntent().getIntExtra(ElementiCantiere.ID_ELEMENTO_CANT, 0);
        elementoAltroOrdine = getIntent().getBooleanExtra("ELEMENTO_ALTRO_ORDINE",false);
        iconaElemento = (ImageView) findViewById(R.id.imageView_iconaElemento);
        buttonrelazioni = (ImageButton) findViewById(R.id.button_relazioni);
        buttonmodificca = (ImageButton) findViewById(R.id.button_edit_elemento);
        buttonNonConteggiare = (ImageButton) findViewById(R.id.button_non_conteggiare);
        relazioni = new ArrayList<Object>();
        relazioniLogiche = new ArrayList<Object>();
        lista_relazioni_logiche = (LinearLayout) findViewById(R.id.lista_relazioni_logiche);
        lista_relazioni = (LinearLayout) findViewById(R.id.lista_relazioni);
        nota = (TextView) findViewById(R.id.nota);
        idPreventivoSelezionato = getIntent().getIntExtra("ID_PREVENTIVO_SELEZIONATO",0);
        System.out.println("EConTab: ElementoDettaglioActivity onCreate EXIT");
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        ricerca();

    }

    public void ricerca() {
        System.out.println("EConTab: ElementoDettaglioActivity ricerca ENTER");
        // TODO Auto-generated method stub

        if (idElementoCantiere != 0) {
            DbInterno db = new DbInterno(this);
            ContentValues where_elem = new ContentValues();
            where_elem.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
            ContentValues valElem = db.getRecord(new ElementiCantiere(), where_elem);
            if (valElem != null) {
                if (valElem.getAsString(ElementiCantiere.NOTA).equals("")) {
                    nota.setVisibility(View.GONE);
                } else {
                    nota.setVisibility(View.VISIBLE);
                }
                nota.setText(valElem.getAsString(ElementiCantiere.NOTA));
                setText(R.id.editText_altezza, "" + valElem.getAsInteger(ElementiCantiere.ALTEZZA_DA_TERRA));
                setText(R.id.editText_etichetta, "" + valElem.getAsInteger(ElementiCantiere.NUMERO_IDENTIFICATIVO));
                setText(R.id.textView_titolo, "" + valElem.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
                idLocale = valElem.getAsInteger(ElementiCantiere.ID_LOCALE);
                idPreventivo = valElem.getAsInteger(ElementiCantiere.ID_PREVENTIVO);
                if (idPreventivo != 0) {
                    ContentValues wherePrev = new ContentValues();
                    wherePrev.put(Preventivi.ID_PREVENTIVO, idPreventivo);
                    ContentValues recPrev = db.getRecord(new Preventivi(), wherePrev);
                    if (recPrev != null) {
                        if (!recPrev.getAsString(Preventivi.STATO).equals(Preventivi.STATO_APERTO)) {
                            modificheBloccate = true;
                        }
                        if (recPrev.getAsString(Preventivi.TIPO).equals(Preventivi.TIPO_PREVENTIVO)) {
                            buttonrelazioni.setVisibility(View.GONE);
                            findViewById(R.id.linear_collegamenti).setVisibility(View.GONE);
                        }
                    }
                    buttonNonConteggiare.setVisibility(View.VISIBLE);
                    if (valElem.get(ElementiCantiere.NON_CONTEGGIARE_PREVENTIVI)==null || valElem.getAsInteger(ElementiCantiere.NON_CONTEGGIARE_PREVENTIVI)==0){
                        nonConteggiarePreventivi = 0;

                    }
                    else{
                        nonConteggiarePreventivi = 1;

                    }

                }
                else{
                    nonConteggiarePreventivi = 1;
                    if (idPreventivoSelezionato==0){
                        buttonNonConteggiare.setVisibility(View.GONE);
                    }

                }
                _impostaSfondoBottoneNonConteggiare();
                if (elementoAltroOrdine){
                    modificheBloccate = true;
                }

                ContentValues where_elem2 = new ContentValues();
                where_elem2.put(Elementi.ID_ELEMENTO, valElem.getAsInteger(ElementiCantiere.ID_ELEMENTO));
                ContentValues valElem2 = db.getRecord(new Elementi(), where_elem2);
                if (valElem2 != null) {
                    idCategoria = valElem2.getAsInteger(Elementi.ID_CATEGORIA_GENERALE);
                    iconaElemento.setImageBitmap(Utility.getIconaScalata(this, Elementi.PATH_ICONE, valElem2.getAsString(Elementi.ICONA)));

                }
            }

            _caricaRelazioniLogiche(db);
            _caricaRelazioni(db);

            db.close();

            if (modificheBloccate) {
                buttonmodificca.setVisibility(View.GONE);
                buttonrelazioni.setVisibility(View.GONE);
                buttonNonConteggiare.setVisibility(View.GONE);
            }
        }
        System.out.println("EConTab: ElementoDettaglioActivity ricerca EXIT");
    }

    private void _impostaSfondoBottoneNonConteggiare() {
        if (nonConteggiarePreventivi==0){
            buttonNonConteggiare.setBackgroundResource(R.drawable.bg_bottone_rotondo_giallo);
        }
        else{
            buttonNonConteggiare.setBackgroundResource(R.drawable.bg_bottone_rotondo);
        }
    }

    private void _caricaRelazioniLogiche(DbInterno db) {
        System.out.println("EConTab: ElementoDettaglioActivity _caricaRelazioniLogiche ENTER");
        relazioniLogiche.clear();
        if (lista_relazioni_logiche.getChildCount() > 1) {
            lista_relazioni_logiche.removeViews(0, lista_relazioni_logiche.getChildCount() - 1);
        }

        Join j0 = new Join(Relazioni.NOME_TABELLA, ElementiCantiere.NOME_TABELLA);
        j0.addCampiDiJoin(Relazioni.ID_ELEMENTO_CANT2, ElementiCantiere.ID_ELEMENTO_CANT);

        Join j1 = new Join(ElementiCantiere.NOME_TABELLA, Locali.NOME_TABELLA);
        j1.addCampiDiJoin(ElementiCantiere.ID_LOCALE, Locali.ID_LOCALE);

        Join j2 = new Join(Locali.NOME_TABELLA, Aree.NOME_TABELLA);
        j2.addCampiDiJoin(Locali.ID_AREA, Aree.ID_AREA);


        String SQLREL = "Select " + Relazioni.NOME_TABELLA + ".*," + Aree.NOME_TABELLA + "." + Aree.NOME + " as nome_area," + Locali.NOME_TABELLA + "." + Locali.NOME + " as nome_locale," + ElementiCantiere.ID_ELEMENTO + "," + ElementiCantiere.NUMERO_IDENTIFICATIVO + "," + ElementiCantiere.NOME_ELEMENTO_CANT + " " +
                "from " + Relazioni.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + j2.getSQLJoin() + " where " + Relazioni.ID_ELEMENTO_CANT1 + "=" + idElementoCantiere;

        relazioniLogiche = db.eseguiSelect(SQLREL, null);

        ContentValues whereComp = new ContentValues();
        ContentValues recComp = null;
        ComponentiCantiere tabComp = new ComponentiCantiere();
        for (int i = 0; i < relazioniLogiche.size(); i++) {
            ContentValues curr = (ContentValues) relazioniLogiche.get(i);
            curr.put("COMPONENTE_B", "");
            int idComponente2 = 0;

            try {
                idComponente2 = curr.getAsInteger(Relazioni.ID_COMPONENTE_CANT2);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (idComponente2 != 0) {
                whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponente2);
                recComp = db.getRecord(tabComp, whereComp);
                if (recComp != null) {
                    // prendo la composizione
                    String moduliOccupati = "";
                    ContentValues whereCompos = new ContentValues();
                    whereCompos.put(ComposizioniCantiere.ID_ELEMENTO_CANT, curr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT2));
                    whereCompos.put(ComposizioniCantiere.ID_COMPONENTE_CANT, idComponente2);

                    ContentValues valCompos = db.getRecord(new ComposizioniCantiere(), whereCompos);
                    if (valCompos != null) {
                        moduliOccupati = valCompos.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
                    }

                    curr.put("COMPONENTE_B", " - Posto " + moduliOccupati + " (" + recComp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT) + ")");
                }
            }

            View relazione = View.inflate(this, R.layout.list_item_relazione_logica, null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = getResources().getDimensionPixelSize(R.dimen.margine);

            int idElementoDestinazione = curr.getAsInteger(ElementiCantiere.ID_ELEMENTO);


            setText(R.id.textViewLocaleB, curr.getAsString("nome_area") + " - " + curr.getAsString("nome_locale"), relazione);
            setText(R.id.textViewNumElementoB, curr.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO), relazione);


            setText(R.id.textViewElementoB, curr.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT) + curr.getAsString("COMPONENTE_B"), relazione);


            if (idElementoDestinazione != 0) {
                ContentValues whereElem = new ContentValues();
                whereElem.put(Elementi.ID_ELEMENTO, idElementoDestinazione);
                ContentValues valElem = db.getRecord(new Elementi(), whereElem);
                if (valElem != null) {
                    int idCategoria = valElem.getAsInteger(Elementi.ID_CATEGORIA_GENERALE);
                    relazione.findViewById(R.id.colore_elemento).setBackgroundColor(
                            Color.parseColor(Utility.getColoreCategoria(idCategoria)));
                }
            }

            relazione.setTag("REL|" + curr.getAsInteger(Relazioni.ID_RELAZIONE));
            relazione.setOnClickListener(this);
            relazione.setOnLongClickListener(this);
            lista_relazioni_logiche.addView(relazione, i, lp);
        }
        System.out.println("EConTab: ElementoDettaglioActivity _caricaRelazioniLogiche EXIT");
    }

    private void _caricaRelazioni(DbInterno db) {
        System.out.println("EConTab: ElementoDettaglioActivity _caricaRelazioni ENTER");
        // TODO Auto-generated method stub
        relazioni.clear();
        lista_relazioni.removeAllViews();

        ArrayList<View> listaVisteDaOrdinare = new ArrayList<View>();

        Collegamenti tabColl = new Collegamenti();
        ArrayList<Object> locali = tabColl.caricaLocaliCantiere(db, idElementoCantiere);

        ContentValues whereTubi = new ContentValues();
        whereTubi.put(Elementi.ID_CATEGORIA_GENERALE, CategorieGenerali.TUBI);
        ContentValues whereCavi = new ContentValues();
        whereCavi.put(Elementi.ID_CATEGORIA_GENERALE, CategorieGenerali.CAVI);
        ArrayList<Object> tubi = db.eseguiSelect(new Elementi(), whereTubi, null);
        ArrayList<Object> cavi = db.eseguiSelect(new Elementi(), whereCavi, null);

        Join j0 = new Join(Collegamenti.NOME_TABELLA, ElementiCantiere.NOME_TABELLA);
        j0.addCampiDiJoin(Collegamenti.ID_ELEMENTO_CANT1, ElementiCantiere.ID_ELEMENTO_CANT);

        Join j1 = new Join(Collegamenti.NOME_TABELLA, ElementiCantiere.NOME_TABELLA);
        j1.setAliasTabella("ElementiCantiere2");
        j1.addCampiDiJoin(Collegamenti.ID_ELEMENTO_CANT2, ElementiCantiere.ID_ELEMENTO_CANT);

        String filtroTubi = " and " + Collegamenti.ID_TUBO + "<>0";
        String filtroCavi = " and " + Collegamenti.ID_CAVO + "<>0";

        String SQL = "Select " + Collegamenti.NOME_TABELLA + ".*," + ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.ID_ELEMENTO
                + "," + ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.NOME_ELEMENTO_CANT + "," + ElementiCantiere.NOME_TABELLA
                + "." + ElementiCantiere.NUMERO_IDENTIFICATIVO + "," + ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.ID_LOCALE
                + " as ID_LOCALE_A,ElementiCantiere2." + ElementiCantiere.ID_ELEMENTO + " as idElemento_2,ElementiCantiere2."
                + ElementiCantiere.NOME_ELEMENTO_CANT + " as nome_elemento_cant2,ElementiCantiere2."
                + ElementiCantiere.NUMERO_IDENTIFICATIVO + " as numero_identificativo_2,ElementiCantiere2." + ElementiCantiere.ID_LOCALE
                + " as ID_LOCALE_B from " + Collegamenti.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where ("
                + Collegamenti.ID_ELEMENTO_CANT1 + " = " + idElementoCantiere + " or " + Collegamenti.ID_ELEMENTO_CANT2 + " = "
                + idElementoCantiere + ") ";

        relazioni.addAll(db.eseguiSelect(SQL + filtroTubi, null));

        ArrayList<Object> relazioniCavi = db.eseguiSelect(SQL + filtroCavi, null);
        for (int i=0;i<relazioniCavi.size();i++){
            boolean trovato = false;
            ContentValues curr = (ContentValues)relazioniCavi.get(i);
            int idCollTubo = curr.getAsInteger(Collegamenti.ID_COLLEGAMENTO_TUBO);
            if (idCollTubo==0){

                relazioni.add(curr);
                trovato = true;
            }
            else{
                for (int j=0;j<relazioni.size();j++){
                    ContentValues currTubo = (ContentValues)relazioni.get(j);
                    int idTubo = currTubo.getAsInteger(Collegamenti.ID_COLLEGAMENTO);
                    if (idTubo==idCollTubo){
                        currTubo.put("ID_TUBO_PER_ORDINAMENTO",idTubo);
                        curr.put("ID_TUBO_PER_ORDINAMENTO",idTubo);
                        relazioni.add(j+1,curr);
                        trovato = true;
                        break;
                    }
                }
            }
            if (!trovato){

                relazioni.add(curr);
            }
        }

        //relazioni.addAll(db.eseguiSelect(SQL + filtroCavi, null));


        ContentValues whereComp = new ContentValues();
        ComponentiCantiere tabComp = new ComponentiCantiere();
        ContentValues recComp = null;

        for (int i = 0; i < relazioni.size(); i++) {
            ContentValues curr = (ContentValues) relazioni.get(i);
            // prendo i dati accessori (locale, componente ecc)
            tabColl.aggiungiTuboCavo(curr, tubi, cavi, this);
            tabColl.aggiungiLocali(curr, locali);

            curr.put("COMPONENTE_A", "");
            curr.put("COMPONENTE_B", "");
            int idComponente1 = curr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT1);
            if (idComponente1 != 0) {
                whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponente1);
                recComp = db.getRecord(tabComp, whereComp);
                if (recComp != null) {
                    // prendo la composizione
                    String moduliOccupati = "";
                    ContentValues whereCompos = new ContentValues();
                    whereCompos.put(ComposizioniCantiere.ID_ELEMENTO_CANT, curr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT1));
                    whereCompos.put(ComposizioniCantiere.ID_COMPONENTE_CANT, idComponente1);

                    ContentValues valCompos = db.getRecord(new ComposizioniCantiere(), whereCompos);
                    if (valCompos != null) {
                        moduliOccupati = valCompos.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
                        if (!moduliOccupati.trim().equals("")){
                            moduliOccupati = moduliOccupati + " - ";
                        }
                    }

                    curr.put("COMPONENTE_A", moduliOccupati +  recComp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
                }
            }

            int idComponente2 = curr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT2);
            if (idComponente2 != 0) {
                whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponente2);
                recComp = db.getRecord(tabComp, whereComp);
                if (recComp != null) {
                    // prendo la composizione
                    String moduliOccupati = "";
                    ContentValues whereCompos = new ContentValues();
                    whereCompos.put(ComposizioniCantiere.ID_ELEMENTO_CANT, curr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT2));
                    whereCompos.put(ComposizioniCantiere.ID_COMPONENTE_CANT, idComponente2);

                    ContentValues valCompos = db.getRecord(new ComposizioniCantiere(), whereCompos);
                    if (valCompos != null) {
                        moduliOccupati = valCompos.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
                        if (!moduliOccupati.trim().equals("")){
                            moduliOccupati = moduliOccupati + " - ";
                        }
                    }

                    curr.put("COMPONENTE_B", moduliOccupati +  recComp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
                }
            }

            View relazione = null;
            int idTubo = curr.getAsInteger(Collegamenti.ID_TUBO);
            int idCavo = curr.getAsInteger(Collegamenti.ID_CAVO);
            if (idTubo!=0){
                relazione = View.inflate(this, R.layout.list_item_relazione, null);
            }
            if (idCavo!=0){
                relazione = View.inflate(this, R.layout.list_item_relazione_cavo, null);
            }
            if (relazione!=null) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = getResources().getDimensionPixelSize(R.dimen.margine);
                int idElementoPartenza = curr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT1);
                int idElementoDestinazione = curr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT2);
                int idElemententoRel = 0;
                if (idElementoPartenza == idElementoCantiere) {
                    idElemententoRel = curr.getAsInteger("idElemento_2");
                    setText(R.id.textViewLocaleB, curr.getAsString("LOCALE_B"), relazione);
                    setText(R.id.textViewNumElementoB, curr.getAsString("numero_identificativo_2"), relazione);
                    setText(R.id.textViewElementoB, curr.getAsString("nome_elemento_cant2"), relazione);
                    setText(R.id.textViewComponenteB, curr.getAsString("COMPONENTE_B"), relazione);

                }
                if (idElementoDestinazione == idElementoCantiere) {
                    idElemententoRel = curr.getAsInteger(ElementiCantiere.ID_ELEMENTO);
                    setText(R.id.textViewLocaleB, curr.getAsString("LOCALE_A"), relazione);
                    setText(R.id.textViewNumElementoB, curr.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO), relazione);
                    setText(R.id.textViewElementoB, curr.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT), relazione);
                    setText(R.id.textViewComponenteB, curr.getAsString("COMPONENTE_A"), relazione);

                }

                if (getTesto(R.id.textViewComponenteB, relazione).equals("")) {
                    relazione.findViewById(R.id.textViewComponenteB).setVisibility(View.GONE);
                }
                if (idElemententoRel != 0) {
                    ContentValues whereElem = new ContentValues();
                    whereElem.put(Elementi.ID_ELEMENTO, idElemententoRel);
                    ContentValues valElem = db.getRecord(new Elementi(), whereElem);
                    if (valElem != null) {
                        int idCategoria = valElem.getAsInteger(Elementi.ID_CATEGORIA_GENERALE);
                        relazione.findViewById(R.id.colore_elemento).setBackgroundColor(
                                Color.parseColor(Utility.getColoreCategoria(idCategoria)));
                    }
                }

                setText(R.id.tipo, curr.getAsString("NOME")+": ", relazione);
               // setText(R.id.tipo, curr.getAsString("TIPO"), relazione);
                if (curr.containsKey("ID_TUBO_PER_ORDINAMENTO")){
                    setText(R.id.nome, curr.getAsString("ID_TUBO_PER_ORDINAMENTO"), relazione);
                }
                else{
                    setText(R.id.nome, "0", relazione);
                }

                if (idTubo!=0){
                    setText(R.id.metri, getString(R.string.metri_udm) + " " + Utility.formatNumero(curr.getAsDouble(Collegamenti.METRI), 2),
                            relazione);
                }
                if (idCavo!=0){
                    setText(R.id.metri, getString(R.string.qta) + " " + curr.getAsInteger(Collegamenti.QTA_CAVO),
                            relazione);
                }


                relazione.setTag("COLL|" + curr.getAsInteger(Collegamenti.ID_COLLEGAMENTO));

                relazione.setOnClickListener(this);
                relazione.setOnLongClickListener(this);

                listaVisteDaOrdinare.add(relazione);
            }
        }

if (listaVisteDaOrdinare.size()>1) {
    Collections.sort(listaVisteDaOrdinare, new Comparator<View>() {
        @Override
        public int compare(View view, View view2) {
            try {
                String num1 = ((TextView) view.findViewById(R.id.textViewNumElementoB)).getText().toString();
                String num2 = ((TextView) view2.findViewById(R.id.textViewNumElementoB)).getText().toString();
                if (Integer.parseInt(num1) > Integer.parseInt(num2)) {
                    return 1;
                }
                if (Integer.parseInt(num1) < Integer.parseInt(num2)) {
                    return -1;
                }


                String idTuboPerOrdine1 = ((TextView) view.findViewById(R.id.nome)).getText().toString();
                String idTuboPerOrdine2 = ((TextView) view2.findViewById(R.id.nome)).getText().toString();


                if (Integer.parseInt(idTuboPerOrdine1) > Integer.parseInt(idTuboPerOrdine2)) {
                    return 1;
                }
                if (Integer.parseInt(idTuboPerOrdine1) < Integer.parseInt(idTuboPerOrdine2)) {
                    return -1;
                }

                return 0;
            } catch (Exception e) {

            }
            return 0;

        }
    });
}

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = getResources().getDimensionPixelSize(R.dimen.padding_bottone);
        for (int i = 0; i < listaVisteDaOrdinare.size(); i++) {
            lista_relazioni.addView(listaVisteDaOrdinare.get(i), lp);
        }

        System.out.println("EConTab: ElementoDettaglioActivity _caricaRelazioni EXIT");

    }

    public void chiudi(View v) {
        finish();
    }

    public void salvaDatiGenerali(View v) {
        System.out.println("EConTab: ElementoDettaglioActivity salvaDatiGenerali ENTER");
        DbInterno db = new DbInterno(this);

        ContentValues val = new ContentValues();
        val.put(ElementiCantiere.NUMERO_IDENTIFICATIVO, getTesto(R.id.editText_etichetta));

        String altezzaTxt = getTesto(R.id.editText_altezza);
        if (altezzaTxt.equals("")) {
            altezzaTxt = "0";
        }

        val.put(ElementiCantiere.ALTEZZA_DA_TERRA, Double.parseDouble(altezzaTxt));
        ContentValues where = new ContentValues();
        where.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
        ElementiCantiere tabElementi = new ElementiCantiere();
        tabElementi.aggiornaRecord(db, val, where);

        db.close();

        Toast.makeText(this, getString(R.string.messaggio_salvataggio_eseguito), Toast.LENGTH_SHORT).show();
        System.out.println("EConTab: ElementoDettaglioActivity salvaDatiGenerali EXIT");
    }

    public void apriModifica(View v) {
        System.out.println("EConTab: ElementoDettaglioActivity apriModifica");
        Intent intent = new Intent(this, ElementoModActivity.class);
        intent.putExtra("ID", idElementoCantiere);
        intent.putExtra("CANTIERE", "SI");
        intent.putExtra(CategorieGenerali.ID_CATEGORIA_GENERALE, idCategoria);

        startActivity(intent);
    }

    public void nuovoCollegamento(View v) {
        System.out.println("EConTab: ElementoDettaglioActivity nuovoCollegamento ENTER");
        String[] tipiCollegamento = new String[3];
        tipiCollegamento[0] = getString(R.string.tubo);
        tipiCollegamento[1] = getString(R.string.cavo);
        tipiCollegamento[2] = getString(R.string.tubo_cavo);

        Utility.mostraSelezioneDialog(getString(R.string.nuovo_collegamento), tipiCollegamento, this, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                Intent intent = new Intent(ElementoDettaglioActivity.this, CollegamentoActivity.class);
                intent.putExtra(Collegamenti.ID_ORDINE, idPreventivo);
                intent.putExtra("ID_ELEMENTO", idElementoCantiere);
                if (which == 0) {
                    intent.putExtra("TIPO", CollegamentoActivity.TUBO);
                }
                if (which == 1) {
                    intent.putExtra("TIPO", CollegamentoActivity.CAVO);
                }
                if (which == 2) {
                    intent.putExtra("TIPO", CollegamentoActivity.TUBO_CAVO);
                }
                startActivity(intent);
            }
        });

        System.out.println("EConTab: ElementoDettaglioActivity nuovoCollegamento EXIT");
    }

    @Override
    public void onClick(View v) {
        System.out.println("EConTab: ElementoDettaglioActivity onClick");
        if (v.getTag().toString().startsWith("COLL|")) {
            apriDettaglio(v);
        }
        if (v.getTag().toString().startsWith("REL|")) {
            apriDettaglioRelazione(v);
        }

    }

    @Override
    public boolean onLongClick(final View v) {
        System.out.println("EConTab: ElementoDettaglioActivity onLongClick");
        // TODO Auto-generated method stub
        if (v.getTag().toString().startsWith("COLL|")) {
            String[] items = new String[2];
            items[0] = getString(R.string.modifica);
            items[1] = getString(R.string.elimina);
            Utility.mostraSelezioneDialog(getString(R.string.collegamento), items, this, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    if (which == 0) {
                        apriDettaglio(v);
                    }
                    if (which == 1) {
                        eliminaCollegamento(v);
                    }
                }
            });

            return true;

        }
        if (v.getTag().toString().startsWith("REL|")) {
            String[] items = new String[2];
            items[0] = getString(R.string.modifica);
            items[1] = getString(R.string.elimina);
            Utility.mostraSelezioneDialog(getString(R.string.relazioni), items, this, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    if (which == 0) {
                        apriDettaglioRelazione(v);
                    }
                    if (which == 1) {
                        eliminaRelazione(v);
                    }
                }
            });

            return true;

        }
        return false;

    }

    private void apriDettaglio(View v) {
        System.out.println("EConTab: ElementoDettaglioActivity apriDettaglio");
        String tag = v.getTag().toString();
        Intent intent = new Intent(this, CollegamentoActivity.class);
        String idCollegamento = tag.substring(tag.indexOf("|") + 1);
        intent.putExtra(Collegamenti.ID_COLLEGAMENTO, Integer.parseInt(idCollegamento));

        // intent.putExtra(Collegamenti.ID_ORDINE, idPreventivo);
        startActivity(intent);
    }

    private void apriDettaglioRelazione(View v) {
        System.out.println("EConTab: ElementoDettaglioActivity apriDettaglioRelazione");
        String tag = v.getTag().toString();
        Intent intent = new Intent(this, RelazioneActivity.class);
        String idRelazione = tag.substring(tag.indexOf("|") + 1);
        intent.putExtra(Relazioni.ID_RELAZIONE, Integer.parseInt(idRelazione));

        // intent.putExtra(Collegamenti.ID_ORDINE, idPreventivo);
        startActivity(intent);
    }

    private void eliminaCollegamento(final View v) {
        // TODO Auto-generated method stub
        Utility.mostraConfermaCancellazioneDialog(this, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    String tag = v.getTag().toString();
                    String idCollegamento = tag.substring(tag.indexOf("|") + 1);
                    DbInterno db = new DbInterno(ElementoDettaglioActivity.this);
                    Collegamenti tabCollegamenti = new Collegamenti();
                    ContentValues valDel = new ContentValues();
                    valDel.put(Collegamenti.ID_COLLEGAMENTO, Integer.parseInt(idCollegamento));
                    tabCollegamenti.cancellaRecord(db, valDel);
                    db.close();
                    ricerca();
                }
            }
        });

    }

    private void eliminaRelazione(final View v) {
        // TODO Auto-generated method stub
        Utility.mostraConfermaCancellazioneDialog(this, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    String tag = v.getTag().toString();
                    String idRelazione = tag.substring(tag.indexOf("|") + 1);
                    DbInterno db = new DbInterno(ElementoDettaglioActivity.this);
                    Relazioni tabCollegamenti = new Relazioni();
                    ContentValues valDel = new ContentValues();
                    valDel.put(Relazioni.ID_RELAZIONE, Integer.parseInt(idRelazione));
                    tabCollegamenti.cancellaRecord(db, valDel);
                    db.close();
                    ricerca();
                }
            }
        });

    }

    public void apriNota(View v) {
        final View campi = View.inflate(this, R.layout.dialog_note_layout, null);
        final EditText campoNota = (EditText) campi.findViewById(R.id.editText_nota);
        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    DbInterno db = new DbInterno(ElementoDettaglioActivity.this);
                    ElementiCantiere tabComp = new ElementiCantiere();
                    ContentValues where = new ContentValues();
                    where.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
                    ContentValues valUpd = tabComp.getValoriLogModifica(db);
                    valUpd.put(ElementiCantiere.NOTA, campoNota.getText().toString());
                    tabComp.aggiornaRecord(db, valUpd, where);
                    db.close();
                    ricerca();

                }

            }
        };
        campoNota.setText(nota.getText().toString());
        Utility.mostraDialogPersonalizzato(getTesto(R.id.textView_titolo), this, campi, getString(R.string.conferma),
                getString(R.string.annulla), listener);
    }


    public void nuovaRelazione(View v) {
        Intent intent = new Intent(this, RelazioneActivity.class);
        intent.putExtra("ID_ELEMENTO", idElementoCantiere);
        startActivity(intent);
    }



    public void nonConteggiare(View view) {
        System.out.println("EConTab: ElementoDettaglioActivity nonConteggiare ENTER");
        if (!modificheBloccate){
            DbInterno db = new DbInterno(this);
            ContentValues valUpd = new ContentValues();
            int idPrevNew = 0;
            if (nonConteggiarePreventivi==0){
                nonConteggiarePreventivi = 1;
                idPrevNew = 0;
            }
            else {
                nonConteggiarePreventivi = 0;
                idPrevNew = idPreventivoSelezionato;
            }
            valUpd.put(ElementiCantiere.NON_CONTEGGIARE_PREVENTIVI, nonConteggiarePreventivi);
            valUpd.put(ElementiCantiere.ID_PREVENTIVO, idPrevNew);
            ContentValues where = new ContentValues();
            where.put(ElementiCantiere.ID_ELEMENTO_CANT, idElementoCantiere);
            ElementiCantiere tab = new ElementiCantiere();
            tab.aggiornaRecord(db, valUpd, where);

            ContentValues elemento = db.getRecord(tab,where);
            PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();
          /*  if (nonConteggiarePreventivi==1){
                tabPrevDett.togliDaPreventivo(db, idPreventivo, elemento);
            }
            else{
                elemento.put(Elementi.NOME_ELEMENTO, elemento.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
                tabPrevDett.aggiungiAPreventivo(db, idPreventivo, elemento);

            }*/

            if (idPrevNew!=0){
                tabPrevDett.aggiornaRighePreventivoLocale(db,idPrevNew,idLocale);
            }
            else {
                tabPrevDett.aggiornaRighePreventivoLocale(db,idPreventivo,idLocale);
            }

            db.close();
            _impostaSfondoBottoneNonConteggiare();
            if (nonConteggiarePreventivi==1){
                Toast.makeText(this,getString(R.string.elemento_non_conteggiato),Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this,getString(R.string.elemento_conteggiato),Toast.LENGTH_SHORT).show();
            }

        }
        System.out.println("EConTab: ElementoDettaglioActivity nonConteggiare EXIT");
    }
}
