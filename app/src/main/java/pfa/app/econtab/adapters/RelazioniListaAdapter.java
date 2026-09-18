package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.utils.Utility;

public class RelazioniListaAdapter extends EConTabListViewAdapter {

    private class RelazioniListaViewHolder extends EConTabViewHolder{
        TextView titoloLocale = null;
        LinearLayout linearRelazione = null;

        FrameLayout coloreElementoComandato = null;
        TextView numElementoComandato = null;
        TextView localeElementoComandato = null;
        TextView elementoComandato = null;
        TextView componenteComandato = null;

        FrameLayout coloreElementoDiComando = null;
        TextView numElementoDiComando = null;
        TextView localeElementoDiComando = null;
        TextView elementoDiComando = null;
        TextView componenteDiComando = null;
	}

	public RelazioniListaAdapter(Context context, ArrayList<Object> dati, int layoutid) {
		super(context, dati, layoutid);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
		// TODO Auto-generated method stub
        RelazioniListaViewHolder holder = new RelazioniListaViewHolder();

        holder.titoloLocale = (TextView)convertView.findViewById(R.id.titolo_locale);
        holder.linearRelazione = (LinearLayout)convertView.findViewById(R.id.linear_relazione);

        holder.coloreElementoComandato = (FrameLayout)convertView.findViewById(R.id.coloreElementoComandato);
        holder.numElementoComandato = (TextView)convertView.findViewById(R.id.numElementoComandato);
        holder.localeElementoComandato = (TextView)convertView.findViewById(R.id.localeElemComandato);
        holder.elementoComandato = (TextView)convertView.findViewById(R.id.elementoComandato);
        holder.componenteComandato = (TextView)convertView.findViewById(R.id.componenteComandato);

        holder.coloreElementoDiComando = (FrameLayout)convertView.findViewById(R.id.coloreElementoDiComando);
        holder.numElementoDiComando = (TextView)convertView.findViewById(R.id.numElementoDiComando);
        holder.localeElementoDiComando = (TextView)convertView.findViewById(R.id.localeElemDiComando);
        holder.elementoDiComando = (TextView)convertView.findViewById(R.id.elementoDiComando);
        holder.componenteDiComando = (TextView)convertView.findViewById(R.id.componenteDiComando);

		return holder;
	}
	
	
	@Override
	protected void personalizzaView(int position, EConTabViewHolder viewholder) {

        RelazioniListaViewHolder holder = (RelazioniListaViewHolder)viewholder;
		ContentValues val = (ContentValues)dati.get(position);
        if (val.containsKey("TITOLO_LOCALE")){
            holder.linearRelazione.setVisibility(View.GONE);
            holder.titoloLocale.setVisibility(View.VISIBLE);
            holder.titoloLocale.setText(val.getAsString("TITOLO_LOCALE"));
        }
        else{
            holder.linearRelazione.setVisibility(View.VISIBLE);
            holder.titoloLocale.setVisibility(View.GONE);

            holder.coloreElementoComandato.setBackgroundColor(Color.parseColor(Utility.getColoreCategoria(val.getAsInteger(Elementi.ID_CATEGORIA_GENERALE))));
            holder.coloreElementoComandato.setTag(position);
            holder.numElementoComandato.setText(val.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO));
            holder.numElementoComandato.setTag(position);
            holder.elementoComandato.setText(val.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
            holder.elementoComandato.setTag(position);
            holder.componenteComandato.setText(val.getAsString("COMPONENTE_COMANDATO"));
            holder.componenteComandato.setTag(position);
            if (!val.getAsString("COMPONENTE_COMANDATO").equals("")){
                holder.elementoComandato.setVisibility(View.GONE);
                holder.componenteComandato.setVisibility(View.VISIBLE);
            }
            else{
                holder.elementoComandato.setVisibility(View.VISIBLE);
                holder.componenteComandato.setVisibility(View.GONE);
            }
            holder.localeElementoComandato.setText(val.getAsString("nome_area") + " - " +  val.getAsString("nome_locale"));
            holder.localeElementoComandato.setTag(position);


            if (val.getAsInteger("id_locale")==val.getAsInteger("id_locale_corrente")){
                holder.localeElementoComandato.setVisibility(View.GONE);
            }
            else{
                holder.localeElementoComandato.setVisibility(View.VISIBLE);
            }

            holder.coloreElementoDiComando.setBackgroundColor(Color.parseColor(Utility.getColoreCategoria(val.getAsInteger("ID_CATEGORIA_COMANDO"))));
            holder.coloreElementoDiComando.setTag(position);
            holder.numElementoDiComando.setText(val.getAsString("NUM_ELEMENTO_COMANDO"));
            holder.numElementoDiComando.setTag(position);
            holder.elementoDiComando.setText(val.getAsString("NOME_ELEMENTO_COMANDO"));
            holder.elementoDiComando.setTag(position);
            holder.componenteDiComando.setText(val.getAsString("COMPONENTE_COMANDO"));
            holder.componenteDiComando.setTag(position);
            if (!val.getAsString("COMPONENTE_COMANDO").equals("")) {
                holder.elementoDiComando.setVisibility(View.GONE);
                holder.componenteDiComando.setVisibility(View.VISIBLE);
            } else {
                holder.elementoDiComando.setVisibility(View.VISIBLE);
                holder.componenteDiComando.setVisibility(View.GONE);
            }
            holder.localeElementoDiComando.setText(val.getAsString("AREA_COMANDO") + " - " + val.getAsString("LOCALE_COMANDO"));
            holder.localeElementoDiComando.setTag(position);


            if (val.getAsInteger("ID_LOCALE_COMANDO") == val.getAsInteger("id_locale_corrente")) {
                holder.localeElementoDiComando.setVisibility(View.GONE);
            } else {
                holder.localeElementoDiComando.setVisibility(View.VISIBLE);
            }
        }
        
		super.personalizzaView(position, viewholder);
	}




    @Override
    public boolean isEnabled(int position) {
        ContentValues val = (ContentValues)dati.get(position);
        if (val.containsKey("TITOLO_LOCALE")){
            return false;
        }
        return super.isEnabled(position);
    }



}
