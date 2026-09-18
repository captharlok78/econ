package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;

public class CantieriAdapter extends EConTabListViewAdapter {

	private class CantieriViewHolder extends EConTabViewHolder{
		TextView cliente = null;
		TextView cantiere = null;
		TextView indirizzo = null;
		TextView cap_citta = null;
	}
	
	public CantieriAdapter(Context context, ArrayList<Object> dati, int layoutid) {
		super(context, dati, layoutid);
		// TODO Auto-generated constructor stub
	}
	
	
	@Override
	protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
		// TODO Auto-generated method stub
		CantieriViewHolder holder = new CantieriViewHolder();
		holder.cliente = (TextView)convertView.findViewById(R.id.ragione_sociale);
		holder.cantiere = (TextView)convertView.findViewById(R.id.nome);
		holder.indirizzo = (TextView)convertView.findViewById(R.id.indirizzo);
		holder.cap_citta = (TextView)convertView.findViewById(R.id.cap_citta);
		return holder;
	}
	
	
	@Override
	protected void personalizzaView(int position, EConTabViewHolder viewholder) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		ContentValues val = (ContentValues)dati.get(position);
		((CantieriViewHolder)viewholder).cliente.setText(val.getAsString(Anagrafica.RAGIONE_SOCIALE));
		((CantieriViewHolder)viewholder).cliente.setTag(position);
		
		((CantieriViewHolder)viewholder).cantiere.setText(val.getAsString(Cantieri.NOME));
		((CantieriViewHolder)viewholder).cantiere.setTag(position);
		
		
		((CantieriViewHolder)viewholder).indirizzo.setText(val.getAsString(Cantieri.INDIRIZZO));
		((CantieriViewHolder)viewholder).indirizzo.setTag(position);
		
		
		String cap_citta = val.getAsString(Cantieri.CAP) +" - " + val.getAsString(Cantieri.CITTA);
		String provincia = val.getAsString(Cantieri.PROVINCIA);
		if (provincia!=null && provincia.length()>0){
			cap_citta = cap_citta + " (" + provincia + ")";
		}
		((CantieriViewHolder)viewholder).cap_citta.setText(cap_citta);
		((CantieriViewHolder)viewholder).cap_citta.setTag(position);
		
		
		
		super.personalizzaView(position, viewholder);
	}

}
