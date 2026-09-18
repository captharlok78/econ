package pfa.app.econtab.adapters;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.RapportiniActivity;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.Rapportini;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.db.table.Utenti;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;

public class RapportiniAdapter extends EConTabListViewAdapter  {

    public RapportiniAdapter(Context context, ArrayList<Object> dati, int layoutid) {
        super(context, dati, layoutid);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
        // TODO Auto-generated method stub
        RapportiniViewHolder holder = new RapportiniViewHolder();
        holder.data_rapportino = (TextView) convertView.findViewById(R.id.data_rapportino);
        holder.operatore = (TextView) convertView.findViewById(R.id.operatore);
        holder.ore = (TextView) convertView.findViewById(R.id.ore);
        holder.ragione_sociale = (TextView) convertView.findViewById(R.id.ragione_sociale);
        holder.ordine = (TextView) convertView.findViewById(R.id.ordine);
        holder.note = (TextView) convertView.findViewById(R.id.note);
        holder.buttonAzioni = (Button) convertView.findViewById(R.id.buttonAzioni);
        holder.buttonDuplica = (Button) convertView.findViewById(R.id.buttonDuplica);
        holder.operatore = (TextView) convertView.findViewById(R.id.operatore);

        return holder;
    }

    @Override
    protected void personalizzaView(final int position, EConTabViewHolder viewholder) {

        final ContentValues val = (ContentValues) dati.get(position);
        ((RapportiniViewHolder) viewholder).data_rapportino.setText(Utility.numberToData(val.getAsLong(Rapportini.DATA_RAPPORTINO)));
        ((RapportiniViewHolder) viewholder).data_rapportino.setTag(position);

        if (val.getAsDouble("tot_ore") != null) {
            ((RapportiniViewHolder) viewholder).ore.setText(getString(R.string.ore) + ": " + Utility.formatNumero(val.getAsDouble("tot_ore")));

        } else {
            ((RapportiniViewHolder) viewholder).ore.setText(getString(R.string.ore) + ": 0");
        }
        ((RapportiniViewHolder) viewholder).ore.setTag(position);


        ((RapportiniViewHolder) viewholder).ragione_sociale.setText(val.getAsString(Anagrafica.RAGIONE_SOCIALE));
        ((RapportiniViewHolder) viewholder).ragione_sociale.setTag(position);

        String nomeCantiere = val.getAsString("nome_cantiere");
        String testoOrdine = context.getResources().getString(R.string.ordine_num_del, val.getAsString(Preventivi.NUMERO), Utility.numberToDataShort(val.getAsLong("data_ordine")));
        testoOrdine = testoOrdine + " - " + val.getAsString(Preventivi.TITOLO);
        testoOrdine = testoOrdine + " (" + nomeCantiere + ")";
        ((RapportiniViewHolder) viewholder).ordine.setText(testoOrdine);
        ((RapportiniViewHolder) viewholder).ordine.setTag(position);

        ((RapportiniViewHolder) viewholder).note.setText(val.getAsString(Rapportini.NOTE));
        ((RapportiniViewHolder) viewholder).note.setTag(position);

        if (Sessione.isLicenzaBusiness(context)){
            ((RapportiniViewHolder) viewholder).operatore.setVisibility(View.VISIBLE);
            ((RapportiniViewHolder) viewholder).operatore.setText(" - "+val.getAsString("nome_operatore")+" "+val.getAsString("cognome_operatore"));
            ((RapportiniViewHolder) viewholder).operatore.setTag(position);
            ((RapportiniViewHolder) viewholder).buttonDuplica.setVisibility(View.VISIBLE);
            ((RapportiniViewHolder) viewholder).buttonDuplica.setTag(position);
        }
        else{
            ((RapportiniViewHolder) viewholder).operatore.setVisibility(View.GONE);
            ((RapportiniViewHolder) viewholder).buttonDuplica.setVisibility(View.GONE);
        }


        ((RapportiniViewHolder) viewholder).buttonAzioni.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] azioni = new String[4];
                azioni[0] = getString(R.string.modifica);
                azioni[1] = getString(R.string.elimina);
                azioni[2] = getString(R.string.esporta_rapportino_xls);
                azioni[3] = getString(R.string.esporta_rapportino_multiplo_xls);
                Utility.mostraSelezioneDialog(getString(R.string.rapportino_del) + " " + Utility.numberToData(val.getAsLong(Rapportini.DATA_RAPPORTINO)), azioni, context, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0) {
                            ((RapportiniActivity) context).modifica(val);
                        }
                        if (i == 1) {
                            ((RapportiniActivity) context).confermaCancellazione(new Rapportini(), val, true);
                        }
                        if (i == 2) {
                            ((RapportiniActivity) context).esportaRapportino(val.getAsInteger(Rapportini.ID_RAPPORTINO),false);
                        }
                        if (i == 3) {
                            ((RapportiniActivity) context).esportaRapportino(val.getAsInteger(Rapportini.ID_RAPPORTINO),true);
                        }
                    }
                });
            }
        });

        ((RapportiniViewHolder) viewholder).buttonDuplica.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.duplica_rapportino_per);
                DbInterno db = new DbInterno(context);
                final ArrayList<Object> listaUtenti = db.eseguiSelect("Select * from " + Utenti.NOME_TABELLA + " where " + Utenti.ID_UTENTE + "<>" + val.getAsInteger(Rapportini.ID_OPERATORE) ,null);
                String[] utenti = new String[listaUtenti.size()];
                for (int i=0;i<listaUtenti.size();i++){
                    utenti[i] = ((ContentValues)listaUtenti.get(i)).getAsString(Utenti.NOME)+" "+((ContentValues)listaUtenti.get(i)).getAsString(Utenti.COGNOME);
                }
                final boolean[] checkedItems = new boolean[listaUtenti.size()];
                builder.setMultiChoiceItems(utenti, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {


                    }
                });

                builder.setPositiveButton(getString(R.string.duplica), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        DbInterno db = new DbInterno(context);
                        Rapportini tabella = new Rapportini();
                        RapportiniDettaglio tabellaRighe = new RapportiniDettaglio();
                        for (int n=0;n<checkedItems.length;n++){
                            if (checkedItems[n]){
                                ContentValues valCopia = (ContentValues)listaUtenti.get(n);
                                ContentValues valRappCopia = tabella.getValoriLogInserimento(db);
                                valRappCopia.put(Rapportini.DATA_RAPPORTINO, val.getAsLong(Rapportini.DATA_RAPPORTINO));
                                valRappCopia.put(Rapportini.ID_ORDINE, val.getAsInteger(Rapportini.ID_ORDINE));
                                valRappCopia.put(Rapportini.NOTE, val.getAsString(Rapportini.NOTE));
                                valRappCopia.put(Rapportini.ID_OPERATORE, valCopia.getAsInteger(Utenti.ID_UTENTE));

                                tabella.inserisciRecord(db, valRappCopia);

                                //inserisco le righe
                                ArrayList<Object> listaRighe = db.eseguiSelect("Select * from " + RapportiniDettaglio.NOME_TABELLA + " where " + RapportiniDettaglio.ID_RAPPORTINO + "=" + val.getAsInteger(Rapportini.ID_RAPPORTINO),null);
                                for (int r = 0;r<listaRighe.size();r++){
                                    ContentValues valCopiaDett = (ContentValues)listaRighe.get(r);
                                    ContentValues valRappCopiaDett = tabellaRighe.getValoriLogInserimento(db);
                                    valRappCopiaDett.put(RapportiniDettaglio.ID_RAPPORTINO,valRappCopia.getAsInteger(Rapportini.ID_RAPPORTINO));
                                    valRappCopiaDett.put(RapportiniDettaglio.ID_MANODOPERA,valCopiaDett.getAsInteger(RapportiniDettaglio.ID_MANODOPERA));
                                    valRappCopiaDett.put(RapportiniDettaglio.ORE,valCopiaDett.getAsDouble(RapportiniDettaglio.ORE));
                                    valRappCopiaDett.put(RapportiniDettaglio.NOTE, valCopiaDett.getAsString(RapportiniDettaglio.NOTE));
                                    tabellaRighe.inserisciRecord(db,valRappCopiaDett);
                                }
                            }
                        }
                        db.close();
                        dialogInterface.cancel();
                        ((RapportiniActivity)context).ricerca();
                    }
                });
                builder.setNegativeButton(getString(R.string.annulla), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        super.personalizzaView(position, viewholder);
    }



    private class RapportiniViewHolder extends EConTabViewHolder {
        TextView data_rapportino = null;
        TextView operatore = null;
        TextView ore = null;
        TextView ragione_sociale = null;
        TextView ordine = null;
        TextView note = null;
        Button buttonAzioni = null;
        Button buttonDuplica = null;

    }

}
