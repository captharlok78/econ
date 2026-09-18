package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import pfa.app.econtab.EConTabActivity;
import pfa.app.econtab.CollegamentoActivity;
import pfa.app.econtab.ComposizioneActivity;
import pfa.app.econtab.ComposizioneLiberaActivity;
import pfa.app.econtab.ComposizioneQuadroActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.RelazioneActivity;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.CategorieComponenti;
import pfa.app.econtab.db.table.Collegamenti;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.db.table.ComponentiCantComposti;
import pfa.app.econtab.db.table.ComponentiCantiere;
import pfa.app.econtab.db.table.Composizioni;
import pfa.app.econtab.db.table.ComposizioniCantiere;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.Relazioni;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;

public class ComposizioneAdapter extends EConTabListViewAdapter implements OnClickListener, OnLongClickListener {


    private boolean composizioneCantiere = false;
    private boolean modificheBloccate = false;
    private boolean mostraRelazioni = false;
    private boolean composizioneLibera = false;
    private HashMap<Integer, ArrayList<Object>> collegamenti = null;
    private HashMap<Integer, ArrayList<Object>> relazioniLogiche = null;
    private int idpreventivo = 0;
    private int idLocale=0;

    public ComposizioneAdapter(Context context, ArrayList<Object> dati, int layoutid) {
        super(context, dati, layoutid);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
        // TODO Auto-generated method stub
        ComposizioneViewHolder holder = new ComposizioneViewHolder();
        holder.tipo = (TextView) convertView.findViewById(R.id.tipo);
        holder.descrizione = (TextView) convertView.findViewById(R.id.descrizione);
        holder.nota = (TextView) convertView.findViewById(R.id.nota);
        holder.posizione = (TextView) convertView.findViewById(R.id.textView_posizione);
        holder.icona = (ImageView) convertView.findViewById(R.id.imageView_icona);
        holder.buttonRelazioni = (ImageButton) convertView.findViewById(R.id.button_relazioni);
        holder.buttonUp = (ImageButton) convertView.findViewById(R.id.button_up);
        holder.buttonDown = (ImageButton) convertView.findViewById(R.id.button_down);
        holder.linearCollegamenti = (LinearLayout) convertView.findViewById(R.id.linear_collegamenti);
        holder.linearRelazioniLogiche = (LinearLayout) convertView.findViewById(R.id.linear_relazioni);
        holder.listaCollegamenti = (LinearLayout) convertView.findViewById(R.id.lista_relazioni);
        holder.listaRelazioniLibereTubi = (LinearLayout) convertView.findViewById(R.id.lista_relazioni_tubi_libere);
        holder.listaRelazioniLogiche = (LinearLayout) convertView.findViewById(R.id.lista_relazioni_logiche);
        holder.buttonNonConteggiare = (ImageButton) convertView.findViewById(R.id.button_non_conteggiare);
        return holder;
    }

    @Override
    protected void personalizzaView(int position, EConTabViewHolder viewholder) {
        System.out.println("EConTab: ComposizioneAdapter personalizzaView ENTER");
        // TODO Auto-generated method stub
        final ContentValues val = (ContentValues) dati.get(position);
        String tipo = val.getAsString(CategorieComponenti.TIPO);
        int idCategoriaComponente = val.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE);
        if (tipo == null) {
            tipo = "";
        }
        String posizione = "";
        ((ComposizioneViewHolder) viewholder).linearCollegamenti.setVisibility(View.GONE);
        ((ComposizioneViewHolder) viewholder).linearRelazioniLogiche.setVisibility(View.GONE);
        if (isComposizioneCantiere()) {

            posizione = val.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
            ((ComposizioneViewHolder) viewholder).nota.setText(val.getAsString(ComponentiCantiere.NOTA));
            if (((ComposizioneViewHolder) viewholder).nota.getText().toString().length() > 0) {
                ((ComposizioneViewHolder) viewholder).nota.setVisibility(View.VISIBLE);
            } else {
                ((ComposizioneViewHolder) viewholder).nota.setVisibility(View.GONE);
            }


            if (mostraRelazioni
                    && !isModificheBloccate()
                    && idCategoriaComponente != 0
                    && !isComponenteAggiuntivoScatola(val)
                    && ((idCategoriaComponente != Componenti.PORTAFRUTTI && idCategoriaComponente != Componenti.COPRISCATOLA) || isComposizioneLibera())) {
                ((ComposizioneViewHolder) viewholder).buttonRelazioni.setVisibility(View.VISIBLE);
            } else {
                ((ComposizioneViewHolder) viewholder).buttonRelazioni.setVisibility(View.GONE);
            }
            ((ComposizioneViewHolder) viewholder).buttonNonConteggiare.setVisibility(View.VISIBLE);
            ((ComposizioneViewHolder) viewholder).buttonNonConteggiare.setBackgroundResource(R.drawable.bg_bottone_rotondo);
            if ( idCategoriaComponente != 0){

                if (val.get(ComponentiCantiere.ID_PREVENTIVO)!=null && val.getAsInteger(ComponentiCantiere.ID_PREVENTIVO)!=0 && (val.get(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI)==null || val.getAsInteger(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI)==0)){
                    ((ComposizioneViewHolder) viewholder).buttonNonConteggiare.setBackgroundResource(R.drawable.bg_bottone_rotondo_giallo);
                }
                else{

                    ((ComposizioneViewHolder) viewholder).buttonNonConteggiare.setBackgroundResource(R.drawable.bg_bottone_rotondo);


                }
                if (!isModificheBloccate()){
                    ((ComposizioneViewHolder) viewholder).buttonNonConteggiare.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final ContentValues riga = (ContentValues)dati.get((Integer)view.getTag());
                            int idPrevRiga = riga.getAsInteger(ComponentiCantiere.ID_PREVENTIVO);
                            if (idPrevRiga!=0 && idPrevRiga!= Sessione.getIdPreventivoSelezionato()){
                                Utility.mostraConfermaDialog(getString(R.string.attenzione), "Il componenete non appartiene all'ordine/preventivo selezionato. Confermare comunque l'azione?", context, getString(R.string.si), getString(R.string.no), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if (i==DialogInterface.BUTTON_POSITIVE){
                                            conteggiaSN(riga);
                                        }
                                    }
                                });
                            }
                            else {
                                conteggiaSN(riga);
                            }

                        }
                    });
                }
            }
            else {
                ((ComposizioneViewHolder) viewholder).buttonNonConteggiare.setVisibility(View.GONE);
            }
            ((ComposizioneViewHolder) viewholder).buttonRelazioni.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    String[] tipiCollegamento = new String[3];
                    tipiCollegamento[0] = getString(R.string.tubo);
                    tipiCollegamento[1] = getString(R.string.cavo);
                    tipiCollegamento[2] = getString(R.string.tubo_cavo);

                    Utility.mostraSelezioneDialog(getString(R.string.nuovo_collegamento), tipiCollegamento, context, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            Intent intent = new Intent(context, CollegamentoActivity.class);
                            intent.putExtra(Collegamenti.ID_ORDINE, idpreventivo);
                            intent.putExtra("ID_ELEMENTO", val.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT));
                            intent.putExtra("ID_COMPONENTE", val.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
                            if (which == 0) {
                                intent.putExtra("TIPO", CollegamentoActivity.TUBO);
                            }
                            if (which == 1) {
                                intent.putExtra("TIPO", CollegamentoActivity.CAVO);
                            }
                            if (which == 2) {
                                intent.putExtra("TIPO", CollegamentoActivity.TUBO_CAVO);
                            }


					/*if (val.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.SCATOLE
							|| val.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.CENTRALINI) {
						intent.putExtra("TIPO_FISSO", CollegamentoActivity.TUBO);
					}*/
                            context.startActivity(intent);
                        }
                    });


                }
            });

            if (mostraRelazioni) {
                _mostraCollegamentiComponente(((ComposizioneViewHolder) viewholder), val, position);
            }


            //le relazioni logiche mostro solo per i frutti (no scatole o portafrutti)
            if (isMostraRelazioniLogiche(val)) {
                val.put("MOSTRA_OPZIONI_RELAZIONI", "SI");
                _mostraRelazioniLogicheComponente(((ComposizioneViewHolder) viewholder), val, position);
            }


        } else {
            posizione = val.getAsString(Composizioni.MODULI_OCCUPATI);
            ((ComposizioneViewHolder) viewholder).nota.setVisibility(View.GONE);
            ((ComposizioneViewHolder) viewholder).buttonRelazioni.setVisibility(View.GONE);
            ((ComposizioneViewHolder) viewholder).buttonNonConteggiare.setVisibility(View.GONE);

        }
        ((ComposizioneViewHolder) viewholder).nota.setTag(position);
        ((ComposizioneViewHolder) viewholder).buttonRelazioni.setTag(position);
        if (posizione == null) {

            posizione = "";
        }

        if (idCategoriaComponente == Componenti.CENTRALINI || idCategoriaComponente == 0 || isComposizioneLibera()) {
            ((ComposizioneViewHolder) viewholder).posizione.setVisibility(View.INVISIBLE);
        } else {
            ((ComposizioneViewHolder) viewholder).posizione.setVisibility(View.VISIBLE);
        }

        if (!isComposizioneLibera()
                && (idCategoriaComponente == Componenti.SCATOLE || idCategoriaComponente == Componenti.PORTAFRUTTI
                || idCategoriaComponente == Componenti.COPRISCATOLA || idCategoriaComponente == Componenti.CENTRALINI)
                || idCategoriaComponente == 0 || isComponenteAggiuntivoScatola(val)  ) {
            ((ComposizioneViewHolder) viewholder).buttonDown.setVisibility(View.GONE);
            ((ComposizioneViewHolder) viewholder).buttonUp.setVisibility(View.GONE);
        } else {
            ((ComposizioneViewHolder) viewholder).buttonDown.setVisibility(View.VISIBLE);
            ((ComposizioneViewHolder) viewholder).buttonUp.setVisibility(View.VISIBLE);
            ((ComposizioneViewHolder) viewholder).buttonUp.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    spostaComponente(val, -1);
                }
            });
            ((ComposizioneViewHolder) viewholder).buttonDown.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    spostaComponente(val, 1);
                }
            });

            if (isComposizioneCantiere()) {
                if (val.getAsInteger(ComposizioniCantiere.POSIZIONE_INIZIALE) == 1) {
                    ((ComposizioneViewHolder) viewholder).buttonUp.setVisibility(View.INVISIBLE);
                }
            } else {
                if (val.getAsInteger(Composizioni.POSIZIONE_INIZIALE) == 1) {
                    ((ComposizioneViewHolder) viewholder).buttonUp.setVisibility(View.INVISIBLE);
                }
            }

            if ((idCategoriaComponente == Componenti.COMPONENTI_QUADRI || isComposizioneLibera()) && position == getCount() - 2) {
                ((ComposizioneViewHolder) viewholder).buttonDown.setVisibility(View.INVISIBLE);
            } else {
                if (position == getCount() - 1) {
                    ((ComposizioneViewHolder) viewholder).buttonDown.setVisibility(View.INVISIBLE);
                }
            }

        }
        ((ComposizioneViewHolder) viewholder).buttonDown.setTag(position);
        ((ComposizioneViewHolder) viewholder).buttonUp.setTag(position);
        ((ComposizioneViewHolder) viewholder).posizione.setText(posizione);
        ((ComposizioneViewHolder) viewholder).posizione.setTag(position);
        ((ComposizioneViewHolder) viewholder).buttonNonConteggiare.setTag(position);

        ((ComposizioneViewHolder) viewholder).tipo.setText(CategorieComponenti.getDescrizioneTipo(context.getResources(), tipo));
        if (tipo.equals(CategorieComponenti.TIPO_FRUTTO) || isComponenteAggiuntivoScatola(val)) {
            ((ComposizioneViewHolder) viewholder).tipo.setVisibility(View.GONE);
        } else {
            ((ComposizioneViewHolder) viewholder).tipo.setVisibility(View.VISIBLE);
        }

        if (isComponenteAggiuntivQuadro(val)){
            ((ComposizioneViewHolder) viewholder).tipo.setVisibility(View.VISIBLE);
            ((ComposizioneViewHolder) viewholder).tipo.setText("COMPONENTE EXTRA");
        }
        ((ComposizioneViewHolder) viewholder).tipo.setTag(position);

        String descrizione = "";
        if (isComposizioneCantiere()) {
            descrizione = val.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT);
        } else {
            descrizione = val.getAsString(Componenti.NOME_COMPONENTE);
        }
        ((ComposizioneViewHolder) viewholder).descrizione.setText(descrizione);
        ((ComposizioneViewHolder) viewholder).descrizione.setTag(position);

        if (descrizione.endsWith("0)")){
            ((ComposizioneViewHolder) viewholder).descrizione.setTextColor(Color.RED);
        }
        else{
            ((ComposizioneViewHolder) viewholder).descrizione.setTextColor(Color.BLACK);
        }

        if (val.getAsString(Componenti.ICONA).equals("")) {
            ((ComposizioneViewHolder) viewholder).icona.setVisibility(View.INVISIBLE);
        } else {
            ((ComposizioneViewHolder) viewholder).icona.setVisibility(View.VISIBLE);
        }
        ((ComposizioneViewHolder) viewholder).icona.setImageBitmap(Utility.getIconaThumb(context, Componenti.PATH_ICONE,
                val.getAsString(Componenti.ICONA)));
        ((ComposizioneViewHolder) viewholder).icona.setTag(position);
        System.out.println("EConTab: ComposizioneAdapter personalizzaView EXIT");
        super.personalizzaView(position, viewholder);
    }



    private boolean isComponenteAggiuntivoScatola(ContentValues val) {
        if (isComposizioneCantiere() && val.get(ComposizioniCantiere.POSIZIONE_INIZIALE)!=null){
            return val.getAsInteger(ComposizioniCantiere.POSIZIONE_INIZIALE)==100;
        }
        return false;

    }

    private boolean isComponenteAggiuntivQuadro(ContentValues val) {
        if (context instanceof ComposizioneQuadroActivity && val.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE)!=Componenti.COMPONENTI_QUADRI && val.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE)!=Componenti.CENTRALINI && val.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE)!=0){
            return true;
        }
        return false;

    }

    private boolean isMostraRelazioniLogiche(ContentValues val) {
        if (!mostraRelazioni) {
            return false;
        }

        if (isComposizioneLibera()) {
            return true;
        }
        int idCatComp = val.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE);
        if (idCatComp == 0) {
            return false;
        }
        if (idCatComp == Componenti.SCATOLE || idCatComp == Componenti.COPRISCATOLA || idCatComp == Componenti.PORTAFRUTTI || idCatComp == Componenti.CENTRALINI) {
            return false;
        }
        return true;
    }

    private void _mostraRelazioniLogicheComponente(ComposizioneViewHolder viewholder, ContentValues val, int position) {
        DbInterno db = new DbInterno(context);
        viewholder.listaRelazioniLogiche.removeAllViews();
        if (val.containsKey(ComponentiCantiere.ID_COMPONENTE_CANT)) {
            int idComponente = val.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT);
            if (relazioniLogiche.containsKey(idComponente)) {
                viewholder.linearRelazioniLogiche.setVisibility(View.VISIBLE);
                ArrayList<Object> relazioni = relazioniLogiche.get(idComponente);
                for (int i = 0; i < relazioni.size(); i++) {
                    ContentValues curr = (ContentValues) relazioni.get(i);
                    View relazione = View.inflate(context, R.layout.list_item_relazione_logica, null);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                    lp.bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.padding_bottone);

                    int idElementoDestinazione = curr.getAsInteger(ElementiCantiere.ID_ELEMENTO);


                    ((EConTabActivity) context).setText(R.id.textViewLocaleB, curr.getAsString("nome_area") + " - " + curr.getAsString("nome_locale"), relazione);
                    ((EConTabActivity) context).setText(R.id.textViewNumElementoB, curr.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO), relazione);
                    ((EConTabActivity) context).setText(R.id.textViewElementoB, curr.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT) + curr.getAsString("COMPONENTE_B"), relazione);


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

                    viewholder.listaRelazioniLogiche.addView(relazione, lp);
                }
            } else {
                viewholder.linearRelazioniLogiche.setVisibility(View.GONE);
            }
        } else {
            viewholder.linearRelazioniLogiche.setVisibility(View.GONE);
        }
        db.close();
    }

    private void _mostraCollegamentiComponente(ComposizioneViewHolder holder, ContentValues val, int position) {
        // TODO Auto-generated method stub
        holder.listaCollegamenti.removeAllViews();
        holder.listaRelazioniLibereTubi.removeAllViews();
        if (val.containsKey(ComponentiCantiere.ID_COMPONENTE_CANT)) {

            int idComponente = val.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT);
            if (collegamenti.containsKey(idComponente)) {
                holder.linearCollegamenti.setVisibility(View.VISIBLE);
                ArrayList<Object> relazioni = collegamenti.get(idComponente);
                for (int i = 0; i < relazioni.size(); i++) {
                    ContentValues curr = (ContentValues) relazioni.get(i);

                    int idTubo = curr.getAsInteger(Collegamenti.ID_TUBO);
                    int idCavo = curr.getAsInteger(Collegamenti.ID_CAVO);
                    View relazione = null;

                    if (idTubo != 0) {
                        relazione = View.inflate(context, R.layout.list_item_relazione, null);
                    }
                    if (idCavo != 0) {
                        relazione = View.inflate(context, R.layout.list_item_relazione_cavo, null);
                    }
                    if (relazione != null) {
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                        lp.bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.padding_bottone);

                        ((TextView) relazione.findViewById(R.id.tipo)).setText(curr.getAsString("NOME") + ": ");
                        //((TextView) relazione.findViewById(R.id.tipo)).setText(curr.getAsString("TIPO")+" "+curr.getAsString("NOME")+": ");
                        //((TextView) relazione.findViewById(R.id.nome)).setText(curr.getAsString("NOME"));
                        if (idTubo != 0 || (idCavo != 0 && !curr.containsKey("TUBO_PRESENTE"))) {
                            if (idTubo!=0){
                                ((TextView) relazione.findViewById(R.id.metri)).setText(getString(R.string.metri_udm) + " " + Utility.formatNumero(curr.getAsDouble(Collegamenti.METRI), 2));
                            }
                           else{
                                ((TextView) relazione.findViewById(R.id.metri)).setText(getString(R.string.qta) + " " + curr.getAsInteger(Collegamenti.QTA_CAVO));
                            }

                            if (idCavo != 0) {
                                    relazione.findViewById(R.id.colore_elemento).setVisibility(View.VISIBLE);
                                    relazione.findViewById(R.id.textViewLocaleB).setVisibility(View.VISIBLE);
                                relazione.findViewById(R.id.textViewElementoB).setVisibility(View.VISIBLE);
                            }
                        }
                        if (idCavo != 0 && curr.containsKey("TUBO_PRESENTE")) {
                            ((TextView) relazione.findViewById(R.id.metri)).setText(getString(R.string.qta) + " " + curr.getAsInteger(Collegamenti.QTA_CAVO));
                            relazione.findViewById(R.id.colore_elemento).setVisibility(View.GONE);
                            relazione.findViewById(R.id.textViewLocaleB).setVisibility(View.GONE);
                            relazione.findViewById(R.id.textViewElementoB).setVisibility(View.GONE);
                        }


                        ((TextView) relazione.findViewById(R.id.textViewLocaleB)).setText(curr.getAsString("LOCALE_B"));
                        ((TextView) relazione.findViewById(R.id.textViewNumElementoB)).setText(curr.getAsString("numero_identificativo_2"));


                        ((TextView) relazione.findViewById(R.id.textViewElementoB)).setText(curr.getAsString("nome_elemento_cant2"));
                        ((TextView) relazione.findViewById(R.id.textViewComponenteB)).setText(curr.getAsString("COMPONENTE_B"));
                        if (curr.getAsString("COMPONENTE_B").equals("")) {
                            relazione.findViewById(R.id.textViewComponenteB).setVisibility(View.GONE);
                        }

                        try {
                            int idCategoria = curr.getAsInteger("ID_CATEGORIA");
                            relazione.findViewById(R.id.colore_elemento).setBackgroundColor(
                                    Color.parseColor(Utility.getColoreCategoria(idCategoria)));
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        relazione.setTag("COLL|" + curr.getAsInteger(Collegamenti.ID_COLLEGAMENTO));
                        relazione.setOnClickListener(this);
                        relazione.setOnLongClickListener(this);

                        holder.listaCollegamenti.addView(relazione, lp);
                    }

                }

            } else {
                holder.linearCollegamenti.setVisibility(View.GONE);
            }

            // caso composizione libera senza scatole con tubi: aggiungo i tubi sopra al primo componente
            if (position == 0 && collegamenti.containsKey(0)) {
                ArrayList<Object> relazioni = collegamenti.get(0);
                for (int i = 0; i < relazioni.size(); i++) {
                    ContentValues curr = (ContentValues) relazioni.get(i);
                    int idTubo = curr.getAsInteger(Collegamenti.ID_TUBO);
                    int idCavo = curr.getAsInteger(Collegamenti.ID_CAVO);
                    View relazione = null;

                    if (idTubo != 0) {
                        relazione = View.inflate(context, R.layout.list_item_relazione, null);
                    }
                    if (idCavo != 0) {
                        relazione = View.inflate(context, R.layout.list_item_relazione_cavo, null);
                    }

                    if (relazione != null) {
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                        lp.bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.padding_bottone);

                        ((TextView) relazione.findViewById(R.id.tipo)).setText(curr.getAsString("NOME") + ": ");
                        //((TextView) relazione.findViewById(R.id.tipo)).setText(curr.getAsString("TIPO") + " " + curr.getAsString("NOME")+": ");
                        //((TextView) relazione.findViewById(R.id.nome)).setText(curr.getAsString("NOME"));
                        if (idTubo != 0 || (idCavo != 0 && !curr.containsKey("TUBO_PRESENTE"))) {
                            if (idTubo!=0){
                                ((TextView) relazione.findViewById(R.id.metri)).setText(getString(R.string.metri_udm) + " " + Utility.formatNumero(curr.getAsDouble(Collegamenti.METRI), 2));
                            }
                            else {
                                ((TextView) relazione.findViewById(R.id.metri)).setText(getString(R.string.qta) + " " + curr.getAsInteger(Collegamenti.QTA_CAVO));
                            }
                            if (idCavo != 0) {
                                relazione.findViewById(R.id.colore_elemento).setVisibility(View.VISIBLE);
                                relazione.findViewById(R.id.textViewLocaleB).setVisibility(View.VISIBLE);
                                relazione.findViewById(R.id.textViewElementoB).setVisibility(View.VISIBLE);
                            }
                        }
                        if (idCavo != 0 && curr.containsKey("TUBO_PRESENTE")) {
                            ((TextView) relazione.findViewById(R.id.metri)).setText(getString(R.string.qta) + " " + curr.getAsInteger(Collegamenti.QTA_CAVO));
                            relazione.findViewById(R.id.colore_elemento).setVisibility(View.GONE);
                            relazione.findViewById(R.id.textViewLocaleB).setVisibility(View.GONE);
                            relazione.findViewById(R.id.textViewElementoB).setVisibility(View.GONE);

                        }


                        ((TextView) relazione.findViewById(R.id.textViewLocaleB)).setText(curr.getAsString("LOCALE_B"));
                        ((TextView) relazione.findViewById(R.id.textViewNumElementoB)).setText(curr.getAsString("numero_identificativo_2"));
                        ((TextView) relazione.findViewById(R.id.textViewElementoB)).setText(curr.getAsString("nome_elemento_cant2"));
                        ((TextView) relazione.findViewById(R.id.textViewComponenteB)).setText(curr.getAsString("COMPONENTE_B"));
                        if (curr.getAsString("COMPONENTE_B").equals("")) {
                            relazione.findViewById(R.id.textViewComponenteB).setVisibility(View.GONE);
                        }

                        try {
                            int idCategoria = curr.getAsInteger("ID_CATEGORIA");
                            relazione.findViewById(R.id.colore_elemento).setBackgroundColor(
                                    Color.parseColor(Utility.getColoreCategoria(idCategoria)));

                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        relazione.setTag("COLL|" + curr.getAsInteger(Collegamenti.ID_COLLEGAMENTO));
                        relazione.setOnClickListener(this);
                        relazione.setOnLongClickListener(this);

                        holder.listaRelazioniLibereTubi.addView(relazione, lp);
                    }
                }
            }

        } else {
            holder.linearCollegamenti.setVisibility(View.GONE);
        }
    }

    protected void spostaComponente(ContentValues val, int pos) {
        // TODO Auto-generated method stub
        DbInterno db = new DbInterno(context);
        if (isComposizioneCantiere()) {
            ComposizioniCantiere tabComp = new ComposizioniCantiere();
            if (context instanceof  ComposizioneQuadroActivity){
                tabComp.spostaComponente(db, val, pos, true ,true);
            }
            else{
                tabComp.spostaComponente(db, val, pos, isComposizioneLibera() ,false);
            }

        } else {
            Composizioni tabComp = new Composizioni();
            if (context instanceof  ComposizioneQuadroActivity){
                tabComp.spostaComponente(db, val, pos, true ,true);
            }
            else{
                tabComp.spostaComponente(db, val, pos, isComposizioneLibera(),false);
            }

        }
        db.close();

        if (context instanceof ComposizioneActivity) {
            ((ComposizioneActivity) context).ricerca();

        }
        if (context instanceof ComposizioneQuadroActivity) {
            ((ComposizioneQuadroActivity) context).ricerca();
        }
        if (context instanceof ComposizioneLiberaActivity) {
            ((ComposizioneLiberaActivity) context).ricerca();
        }

        // Toast.makeText(context, "" + val.getAsInteger(ComposizioniCantiere.POSIZIONE_INIZIALE) + " : " + pos,
        // Toast.LENGTH_SHORT).show();
    }

    public boolean isComposizioneCantiere() {
        return composizioneCantiere;
    }

    public void setComposizioneCantiere(boolean composizioneCantiere) {
        this.composizioneCantiere = composizioneCantiere;
    }

    public boolean isModificheBloccate() {
        return modificheBloccate;
    }

    public void setModificheBloccate(boolean modificheBloccate) {
        this.modificheBloccate = modificheBloccate;
    }

    public void setMostraRelazioni(boolean mostraRelazioni) {
        // TODO Auto-generated method stub
        this.mostraRelazioni = mostraRelazioni;
    }

    public HashMap<Integer, ArrayList<Object>> getCollegamenti() {
        return collegamenti;
    }

    public void setCollegamenti(HashMap<Integer, ArrayList<Object>> collegamenti) {
        this.collegamenti = collegamenti;
    }

    public int getIdpreventivo() {
        return idpreventivo;
    }

    public void setIdpreventivo(int idpreventivo) {
        this.idpreventivo = idpreventivo;
    }

    @Override
    public void onClick(View v) {
        if (v.getTag().toString().startsWith("COLL|")) {
            apriDettaglio(v);
        }
        if (v.getTag().toString().startsWith("REL|")) {
            apriDettaglioRelazione(v);
        }

    }

    @Override
    public boolean onLongClick(final View v) {
        // TODO Auto-generated method stub
        if (v.getTag().toString().startsWith("COLL|")) {
            String[] items = new String[2];
            items[0] = getString(R.string.modifica);
            items[1] = getString(R.string.elimina);
            Utility.mostraSelezioneDialog(getString(R.string.collegamento), items, context, new DialogInterface.OnClickListener() {

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
            Utility.mostraSelezioneDialog(getString(R.string.relazione), items, context, new DialogInterface.OnClickListener() {

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
        String tag = v.getTag().toString();
        Intent intent = new Intent(context, CollegamentoActivity.class);
        String idCollegamento = tag.substring(tag.indexOf("|") + 1);
        intent.putExtra(Collegamenti.ID_COLLEGAMENTO, Integer.parseInt(idCollegamento));

        // intent.putExtra(Collegamenti.ID_ORDINE, idPreventivo);
        context.startActivity(intent);
    }

    private void apriDettaglioRelazione(View v) {
        String tag = v.getTag().toString();
        Intent intent = new Intent(context, RelazioneActivity.class);
        String idRelazione = tag.substring(tag.indexOf("|") + 1);
        intent.putExtra(Relazioni.ID_RELAZIONE, Integer.parseInt(idRelazione));

        // intent.putExtra(Collegamenti.ID_ORDINE, idPreventivo);
        context.startActivity(intent);
    }

    private void eliminaCollegamento(final View v) {
        // TODO Auto-generated method stub
        Utility.mostraConfermaCancellazioneDialog(context, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    String tag = v.getTag().toString();
                    String idCollegamento = tag.substring(tag.indexOf("|") + 1);
                    DbInterno db = new DbInterno(context);
                    Collegamenti tabCollegamenti = new Collegamenti();
                    ContentValues valDel = new ContentValues();
                    valDel.put(Collegamenti.ID_COLLEGAMENTO, Integer.parseInt(idCollegamento));
                    tabCollegamenti.cancellaRecord(db, valDel);
                    db.close();
                    if (context instanceof ComposizioneActivity) {
                        ((ComposizioneActivity) context).ricerca();
                    }
                    if (context instanceof ComposizioneQuadroActivity) {
                        ((ComposizioneQuadroActivity) context).ricerca();
                    }
                    if (context instanceof ComposizioneLiberaActivity) {
                        ((ComposizioneLiberaActivity) context).ricerca();
                    }

                }
            }
        });

    }

    private void eliminaRelazione(final View v) {
        // TODO Auto-generated method stub
        Utility.mostraConfermaCancellazioneDialog(context, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    String tag = v.getTag().toString();
                    String idRelazione = tag.substring(tag.indexOf("|") + 1);
                    DbInterno db = new DbInterno(context);
                    Relazioni tabCollegamenti = new Relazioni();
                    ContentValues valDel = new ContentValues();
                    valDel.put(Relazioni.ID_RELAZIONE, Integer.parseInt(idRelazione));
                    tabCollegamenti.cancellaRecord(db, valDel);
                    db.close();
                    if (context instanceof ComposizioneActivity) {
                        ((ComposizioneActivity) context).ricerca();
                    }
                    if (context instanceof ComposizioneQuadroActivity) {
                        ((ComposizioneQuadroActivity) context).ricerca();
                    }
                    if (context instanceof ComposizioneLiberaActivity) {
                        ((ComposizioneLiberaActivity) context).ricerca();
                    }
                }
            }
        });

    }

    public boolean isComposizioneLibera() {
        return composizioneLibera;
    }

    public void setComposizioneLibera(boolean composizioneLibera) {
        this.composizioneLibera = composizioneLibera;
    }

    public HashMap<Integer, ArrayList<Object>> getRelazioni() {
        return relazioniLogiche;
    }

    public void setRelazioni(HashMap<Integer, ArrayList<Object>> relazioni) {
        this.relazioniLogiche = relazioni;
    }

    public void setIdLocale(int idLocale) {
        this.idLocale = idLocale;
    }

    public int getIdLocale() {
       return  idLocale;
    }

    private class ComposizioneViewHolder extends EConTabViewHolder {
        TextView tipo = null;
        TextView descrizione = null;
        TextView nota = null;
        TextView posizione = null;
        ImageView icona = null;
        ImageButton buttonNonConteggiare = null;
        ImageButton buttonRelazioni = null;
        ImageButton buttonUp = null;
        ImageButton buttonDown = null;


        LinearLayout listaRelazioniLibereTubi = null;

        LinearLayout linearCollegamenti = null;
        LinearLayout listaCollegamenti = null;

        LinearLayout linearRelazioniLogiche = null;
        LinearLayout listaRelazioniLogiche = null;
    }

    private void conteggiaSN(ContentValues riga){
        DbInterno db = new DbInterno(context);
        ContentValues valUpd = new ContentValues();
        int nonConteggiarePreventivi = 0;
        int idPreventivo = 0;
        if (riga.get(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI)!=null && riga.getAsInteger(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI)==1){
            nonConteggiarePreventivi = 0;
            idPreventivo = getIdpreventivo();
        }
        else {
            nonConteggiarePreventivi = 1;
            idPreventivo = 0;
        }
        valUpd.put(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI,nonConteggiarePreventivi);
        valUpd.put(ComponentiCantiere.ID_PREVENTIVO,idPreventivo);
        riga.put(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI, nonConteggiarePreventivi);
        riga.put(ComponentiCantiere.ID_PREVENTIVO, idPreventivo);
        ContentValues where = new ContentValues();
        where.put(ComposizioniCantiere.ID_COMPONENTE_CANT, riga.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
        ComponentiCantiere tab = new ComponentiCantiere();

        tab.aggiornaRecord(db, valUpd, where);

        //aggiorno il preventivo/ordine
        PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();

        tabPrevDett.aggiornaRighePreventivoLocale(db,getIdpreventivo(),getIdLocale());

        db.close();

        notifyDataSetChanged();
        if (nonConteggiarePreventivi==1){
            Toast.makeText(context, getString(R.string.elemento_non_conteggiato), Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(context,getString(R.string.elemento_conteggiato),Toast.LENGTH_SHORT).show();
        }
    }
}
