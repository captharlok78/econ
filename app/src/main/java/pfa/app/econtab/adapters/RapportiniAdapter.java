package pfa.app.econtab.adapters;

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
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.Rapportini;
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
        holder.stato = (TextView) convertView.findViewById(R.id.stato);

        return holder;
    }

    @Override
    protected void personalizzaView(final int position, EConTabViewHolder viewholder) {

        final ContentValues val = (ContentValues) dati.get(position);
        ((RapportiniViewHolder) viewholder).data_rapportino.setText(Utility.numberToData(val.getAsLong(Rapportini.DATA_RAPPORTINO)));
        ((RapportiniViewHolder) viewholder).data_rapportino.setTag(position);

        // badge dello stato (nome e colore da stati_documento; assente se gli stati non sono ancora scaricati)
        TextView stato = ((RapportiniViewHolder) viewholder).stato;
        String nomeStato = val.getAsString("nome_stato");
        if (nomeStato != null && !nomeStato.isEmpty()) {
            stato.setVisibility(View.VISIBLE);
            stato.setText(nomeStato);
            android.graphics.drawable.GradientDrawable sfondo = new android.graphics.drawable.GradientDrawable();
            sfondo.setCornerRadius(24);
            sfondo.setColor(pfa.app.econtab.utils.RegoleRapportino.coloreStato(val.getAsString("colore_stato")));
            stato.setBackground(sfondo);
        } else {
            stato.setVisibility(View.GONE);
        }

        if (val.getAsDouble("tot_ore") != null) {
            ((RapportiniViewHolder) viewholder).ore.setText(getString(R.string.ore_uomo) + ": " + Utility.formatNumero(val.getAsDouble("tot_ore")));

        } else {
            ((RapportiniViewHolder) viewholder).ore.setText(getString(R.string.ore_uomo) + ": 0");
        }
        ((RapportiniViewHolder) viewholder).ore.setTag(position);


        ((RapportiniViewHolder) viewholder).ragione_sociale.setText(val.getAsString(Anagrafica.RAGIONE_SOCIALE));
        ((RapportiniViewHolder) viewholder).ragione_sociale.setTag(position);

        // cantiere e ordine facoltativi (il rapportino puo' essere solo per il cliente)
        String nomeCantiere = val.getAsString("nome_cantiere") != null ? val.getAsString("nome_cantiere") : "";
        String testoOrdine = nomeCantiere;
        if (val.getAsString(Preventivi.NUMERO) != null && val.getAsLong("data_ordine") != null) {
            testoOrdine = context.getResources().getString(R.string.ordine_num_del, val.getAsString(Preventivi.NUMERO), Utility.numberToDataShort(val.getAsLong("data_ordine")))
                    + " - " + val.getAsString(Preventivi.TITOLO) + (nomeCantiere.isEmpty() ? "" : " (" + nomeCantiere + ")");
        }
        ((RapportiniViewHolder) viewholder).ordine.setText(testoOrdine);
        ((RapportiniViewHolder) viewholder).ordine.setTag(position);

        ((RapportiniViewHolder) viewholder).note.setText(val.getAsString(Rapportini.NOTE));
        ((RapportiniViewHolder) viewholder).note.setTag(position);

        if (Sessione.isLicenzaBusiness(context)){
            ((RapportiniViewHolder) viewholder).operatore.setVisibility(View.VISIBLE);
            ((RapportiniViewHolder) viewholder).operatore.setText(" - "+val.getAsString("nome_operatore")+" "+val.getAsString("cognome_operatore"));
            ((RapportiniViewHolder) viewholder).operatore.setTag(position);
        }
        else{
            ((RapportiniViewHolder) viewholder).operatore.setVisibility(View.GONE);
        }
        // "Duplica per altri utenti" non c'e' piu': chi ha lavorato si indica con gli operatori di ogni riga, e
        // duplicare il rapportino per altri conterebbe due volte le stesse ore
        ((RapportiniViewHolder) viewholder).buttonDuplica.setVisibility(View.GONE);


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
                            ((RapportiniActivity) context).esportaRapportino(val.getAsInteger(Rapportini.ID),false);
                        }
                        if (i == 3) {
                            ((RapportiniActivity) context).esportaRapportino(val.getAsInteger(Rapportini.ID),true);
                        }
                    }
                });
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
        TextView stato = null;

    }

}
