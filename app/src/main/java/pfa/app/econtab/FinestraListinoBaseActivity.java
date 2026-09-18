package pfa.app.econtab;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.CategorieComponenti;
import pfa.app.econtab.db.table.CategorieGenerali;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.db.table.ComponentiCantiere;
import pfa.app.econtab.db.table.ComponentiComposti;
import pfa.app.econtab.db.table.Composizioni;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabItemMenu;

public class FinestraListinoBaseActivity extends FinestraElementiActivity {

    boolean elemento = false;
    boolean componente = false;


    int elementiOComponenti = EConTabItemMenu.CATEGORIA_ELEMENTI;

    @Override
    protected void creaMenuCategorie() {
        System.out.println("EConTab: FinestraListinoBaseActivity creaMenuCategorie ENTER");
        // TODO Auto-generated method stub
        resetCategorie();
        DbInterno db = new DbInterno(this);


        ArrayList<Object> lista = new ArrayList<Object>();
        ArrayList<Object> listaComp = new ArrayList<Object>();
        if (elementiOComponenti == EConTabItemMenu.CATEGORIA_ELEMENTI) {
            CategorieGenerali tabCat = new CategorieGenerali();
            lista = db.eseguiSelect(tabCat);
        }
        if (elementiOComponenti == EConTabItemMenu.CATEGORIA_COMPONENTI) {
            CategorieComponenti tabCatComp = new CategorieComponenti();
            listaComp = db.eseguiSelect(tabCatComp);
        }


        numeroCategorie = lista.size() + listaComp.size();

        db.close();

        for (int i = 0; i < lista.size(); i++) {
            ContentValues val = (ContentValues) lista.get(i);
            EConTabItemMenu item = new EConTabItemMenu(this);
            item.setTipo(EConTabItemMenu.CATEGORIA_ELEMENTI);
            item.setTesto(val.getAsString(CategorieGenerali.NOME));
            item.setIcona(CategorieGenerali.PATH_ICONE, val.getAsString(CategorieGenerali.ICONA));

            item.setOnClickListener(this);
            item.setTag("" + i);
            item.setIdCategoria(val.getAsInteger(CategorieGenerali.ID_CATEGORIA_GENERALE));
            if (listaCategorie == null) {
                listaCategorie = new ArrayList<EConTabItemMenu>();
            }
            listaCategorie.add(item);

            // seleziono il primo
            if (i == 0) {
                item.setSelected(true);
                titolo.setText(item.getTesto());
            }

            aggiungiCategoria(item);

        }

        for (int i = 0; i < listaComp.size(); i++) {
            ContentValues val = (ContentValues) listaComp.get(i);
            EConTabItemMenu item = new EConTabItemMenu(this);
            item.setTipo(EConTabItemMenu.CATEGORIA_COMPONENTI);
            item.setTesto(val.getAsString(CategorieComponenti.NOME));
            item.setIcona(CategorieComponenti.PATH_ICONE, val.getAsString(CategorieComponenti.ICONA));

            item.setOnClickListener(this);
            item.setTag("" + (i + lista.size()));
            item.setIdCategoria(val.getAsInteger(CategorieComponenti.ID_CATEGORIA_COMPONENTE));
            if (listaCategorie == null) {
                listaCategorie = new ArrayList<EConTabItemMenu>();
            }
            listaCategorie.add(item);

            // seleziono il primo
            if (i + lista.size() == 0) {
                item.setSelected(true);
                titolo.setText(item.getTesto());
            }

            aggiungiCategoria(item);

        }
        selezionato = 0;
        ricaricaPageAdapter();
        notifyPageAdapter();
        System.out.println("EConTab: FinestraListinoBaseActivity creaMenuCategorie EXIT");
    }


    public void aggiungiElemento(final ContentValues val) {
        System.out.println("EConTab: FinestraListinoBaseActivity aggiungiElemento ENTER");

        mostraPopupQuantita(val);
        // Toast t = Toast.makeText(this, "aggiunto", Toast.LENGTH_SHORT);
        // t.setGravity(Gravity.CENTER, 0, 0);
        // t.show();
        System.out.println("EConTab: FinestraListinoBaseActivity aggiungiElemento EXIT");
    }

    public void mostraPopupQuantita(final ContentValues val) {
        System.out.println("EConTab: FinestraListinoBaseActivity mostraPopupQuantita ENTER");
        final ArrayList<Object> composizioni = new ArrayList<Object>();
        elemento = false;
        componente = false;
        boolean erroreComposizioniMancanti = false;
        if (val.containsKey(Elementi.ID_CATEGORIA_GENERALE)) {
            elemento = true;
            DbInterno db = new DbInterno(this);
            int idCategoriaGenerale = val.getAsInteger(Elementi.ID_CATEGORIA_GENERALE);
            ContentValues where = new ContentValues();
            where.put(CategorieGenerali.ID_CATEGORIA_GENERALE, idCategoriaGenerale);
            ContentValues valCat = db.getRecord(new CategorieGenerali(), where);
            if (valCat != null && valCat.getAsInteger(CategorieGenerali.CONFIGURABILE_SN) == 1) {
                Composizioni tabComposizioni = new Composizioni();
                composizioni.addAll(tabComposizioni.getComposizioneElemento(db, val.getAsInteger(Elementi.ID_ELEMENTO)));
                if (composizioni.size() == 0) {
                    erroreComposizioniMancanti = true;
                }
            }

            db.close();
        }
        if (val.containsKey(Componenti.ID_CATEGORIA_COMPONENTE)) {
            componente = true;
        }
        if (erroreComposizioniMancanti) {
            Utility.mostraDialog(getString(R.string.attenzione),getString(R.string.messaggio_composizoni_mancanti),this,"OK");
        } else {
            View campi = View.inflate(this, R.layout.dialog_qtaelemento_layout, null);
            final TextView etichetta = (TextView) campi.findViewById(R.id.textView_qta);
            final EditText qta = (EditText) campi.findViewById(R.id.editText_qta);
            final TextView composizioneTxt = (TextView) campi.findViewById(R.id.textView_composizione);
            if (composizioni.size() == 0) {
                composizioneTxt.setVisibility(View.GONE);
            } else {
                String testoComposizione = getString(R.string.messaggio_componenti).toUpperCase(Locale.getDefault()) + ":";
                for (int i = 0; i < composizioni.size(); i++) {
                    ContentValues valComp = (ContentValues) composizioni.get(i);
                    testoComposizione = testoComposizione + "\n- " + valComp.getAsString(Componenti.NOME_COMPONENTE);
                }
                composizioneTxt.setText(testoComposizione);
            }

            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    if (which == Dialog.BUTTON_POSITIVE) {
                        DbInterno db = new DbInterno(FinestraListinoBaseActivity.this);

                        db.getReadableDatabase().beginTransaction();
                        try {
                            // per le righe esterne ai loclai non aggiungo nella tabella
                            // elementi_cantiere,componenti_cantiere
                            // ma solo nella tabella preventivi_dettaglio.
                            PreventiviDettaglio tabPrev = new PreventiviDettaglio();
                            tabPrev.setAggiornaCaviETubi(true);
                            if (componente == true) {
                                ComponentiComposti tabComposti = new ComponentiComposti();
                                ArrayList<Object> componentiComp = tabComposti.getComposizioneComponente(db,
                                        val.getAsInteger(Componenti.ID_COMPONENTE));
                                if (componentiComp.size() == 0) {
                                    tabPrev.aggiornaRigaMaterialePreventivo(db, getIdPreventivoSelezionato(), val,
                                            Utility.formatNumeroDB(qta.getText().toString()),PreventiviDettaglio.MATERIALE_PREVENTIVO, true);
                                } else {
                                    for (int i = 0; i < componentiComp.size(); i++) {
                                        ContentValues currComp = (ContentValues) componentiComp.get(i);
                                        tabPrev.aggiornaRigaMaterialePreventivo(db, getIdPreventivoSelezionato(), currComp,
                                                Utility.formatNumeroDB(qta.getText().toString()),PreventiviDettaglio.MATERIALE_PREVENTIVO,
                                                true);
                                    }
                                    val.put(PreventiviDettaglio.QUANTITA, qta.getText().toString());
                                    val.put(PreventiviDettaglio.ID_PREVENTIVO, getIdPreventivoSelezionato());
                                    tabPrev.aggiornaTubiECaviMaterialePreventivo(db, val);
                                }
                            }

                            if (elemento == true) {
                                // se componibile allora inserisco i componenti , altrimneti inserisco direttamente
                                // l'elemento
                                if (composizioni.size() > 0) {
                                    ComponentiComposti tabComposti = new ComponentiComposti();
                                    for (int i = 0; i < composizioni.size(); i++) {
                                        ContentValues valCompos = (ContentValues) composizioni.get(i);
                                        ArrayList<Object> componentiComp = tabComposti.getComposizioneComponente(db,
                                                valCompos.getAsInteger(Composizioni.ID_COMPONENTE));
                                        if (componentiComp.size() == 0) {
                                            // per compatibilit� con l'aggiunta su locali copio il campo
                                            // NOME_COMPONENTE_CANT
                                            // con il NOME_COMPONENTE
                                            valCompos.put(ComponentiCantiere.NOME_COMPONENTE_CANT,
                                                    valCompos.getAsString(Componenti.NOME_COMPONENTE));
                                            tabPrev.aggiornaRigaMaterialePreventivo(db, getIdPreventivoSelezionato(), valCompos,
                                                    Utility.formatNumeroDB(qta.getText().toString()),PreventiviDettaglio.MATERIALE_PREVENTIVO,
                                                    true);
                                        } else {
                                            for (int j = 0; j < componentiComp.size(); j++) {
                                                ContentValues currComp = (ContentValues) componentiComp.get(j);
                                                currComp.put(ComponentiCantiere.NOME_COMPONENTE_CANT,
                                                        currComp.getAsString(Componenti.NOME_COMPONENTE));
                                                tabPrev.aggiornaRigaMaterialePreventivo(db, getIdPreventivoSelezionato(), currComp,
                                                        Utility.formatNumeroDB(qta.getText().toString())
                                                        ,PreventiviDettaglio.MATERIALE_PREVENTIVO, true);
                                            }
                                            valCompos.put(PreventiviDettaglio.QUANTITA, qta.getText().toString());
                                            valCompos.put(PreventiviDettaglio.ID_PREVENTIVO, getIdPreventivoSelezionato());
                                            tabPrev.aggiornaTubiECaviMaterialePreventivo(db, valCompos);

                                        }
                                        // aggiorno le placche
                                     /*   if (valCompos.getAsString(CategorieComponenti.TIPO).equals(CategorieComponenti.TIPO_PORTAFRUTTI)) {
                                            tabPrev.aggiornaPlacche(db, val, valCompos, getIdPreventivoSelezionato(), 0,
                                                    Utility.formatNumeroDB(qta.getText().toString()));
                                        }*/

                                    }

                                    // aggiorno le placche

                                } else

                                {
                                    tabPrev.aggiornaRigaMaterialePreventivo(db, getIdPreventivoSelezionato(), val,
                                            Utility.formatNumeroDB(qta.getText().toString()),PreventiviDettaglio.MATERIALE_PREVENTIVO, false);
                                }

                            }
                            db.getReadableDatabase().setTransactionSuccessful();
                        } catch (Exception e) {
                            Utility.mostraDialog("Errore", Log.getStackTraceString(e), FinestraListinoBaseActivity.this, "OK");
                        } finally {
                            db.getReadableDatabase().endTransaction();
                            db.close();
                        }
                    }
                }
            };

            qta.setText("1");
            if (elemento) {

                etichetta.setText(getString(R.string.quantita) + " " + val.getAsString(Elementi.UNITA_MISURA).trim());

                Utility.mostraDialogPersonalizzato(
                        val.getAsString(Elementi.NOME_ELEMENTO).toUpperCase(Locale.getDefault()),
                        new BitmapDrawable(getResources(), Utility.getIconaScalata(this, Elementi.PATH_ICONE, val.getAsString(Elementi.ICONA))),
                        this, campi, this.getString(R.string.conferma), this.getString(R.string.annulla), listener);
            }

            if (componente) {

                etichetta.setText(getString(R.string.quantita) + " " + val.getAsString(Componenti.UNITA_MISURA).trim());

                Utility.mostraDialogPersonalizzato(
                        val.getAsString(Componenti.NOME_COMPONENTE).toUpperCase(Locale.getDefault()),
                        new BitmapDrawable(getResources(), Utility.getIconaScalata(this, Componenti.PATH_ICONE,
                                val.getAsString(Componenti.ICONA))), this, campi, this.getString(R.string.conferma),
                        this.getString(R.string.annulla), listener);

            }
        }

        System.out.println("EConTab: FinestraListinoBaseActivity mostraPopupQuantita EXIT");
    }

    protected void setVisualizzazioneCategorie() {
        System.out.println("EConTab: FinestraListinoBaseActivity setVisualizzazioneCategorie ENTER");
        final TextView catComp = (TextView) findViewById(R.id.textView_categorie_comp);
        final TextView catGen = (TextView) findViewById(R.id.textView_categorie_gen);
        catComp.setVisibility(View.VISIBLE);
        catComp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                catComp.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                catGen.setTextColor(getResources().getColor(android.R.color.darker_gray));
                elementiOComponenti = EConTabItemMenu.CATEGORIA_COMPONENTI;
                creaMenuCategorie();
            }
        });

        catGen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                catComp.setTextColor(getResources().getColor(android.R.color.darker_gray));
                catGen.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                elementiOComponenti = EConTabItemMenu.CATEGORIA_ELEMENTI;
                creaMenuCategorie();
            }
        });
        System.out.println("EConTab: FinestraListinoBaseActivity setVisualizzazioneCategorie EXIT");
    }
}
