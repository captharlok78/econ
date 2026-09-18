package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.utils.Sessione;

public class ClientiAdapter extends EConTabListViewAdapter {

	private class ClientiViewHolder extends EConTabViewHolder{
		TextView ragionesociale = null;
		TextView indirizzo = null;
		TextView cap_citta = null;
		TextView codice_esterno = null;
	}
	
	public ClientiAdapter(Context context,ArrayList<Object> dati, int layoutid) {
		super(context, dati, layoutid);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
		// TODO Auto-generated method stub
		ClientiViewHolder holder = new ClientiViewHolder();
		holder.ragionesociale = (TextView)convertView.findViewById(R.id.ragione_sociale);
		holder.indirizzo = (TextView)convertView.findViewById(R.id.indirizzo);
		holder.cap_citta = (TextView)convertView.findViewById(R.id.cap_citta);
		holder.codice_esterno = (TextView)convertView.findViewById(R.id.codice_esterno);
		return holder;
	}
	
	
	@Override
	protected void personalizzaView(int position, EConTabViewHolder viewholder) {
		// TODO Auto-generated method stub
		ContentValues val = (ContentValues)dati.get(position);
		((ClientiViewHolder)viewholder).ragionesociale.setText(val.getAsString(Anagrafica.RAGIONE_SOCIALE));
		((ClientiViewHolder)viewholder).ragionesociale.setTag(position);
		
		String indirizzo = val.getAsString(Anagrafica.INDIRIZZO);
		((ClientiViewHolder)viewholder).indirizzo.setText(indirizzo);
		((ClientiViewHolder)viewholder).indirizzo.setTag(position);
		//if (indirizzo.length()==0){
		//	((ClientiViewHolder)viewholder).indirizzo.setVisibility(View.GONE);
		//}
		//else{
		//	((ClientiViewHolder)viewholder).indirizzo.setVisibility(View.VISIBLE);
		//}
		
		String cap_citta = val.getAsString(Anagrafica.CAP) +" - " + val.getAsString(Anagrafica.CITTA);
		String provincia = val.getAsString(Anagrafica.PROVINCIA);
		if (provincia!=null && provincia.length()>0){
			cap_citta = cap_citta + " (" + provincia + ")";
		}
		((ClientiViewHolder)viewholder).cap_citta.setText(cap_citta);
		((ClientiViewHolder)viewholder).cap_citta.setTag(position);

		((ClientiViewHolder)viewholder).codice_esterno.setText("Cod. " + val.getAsString(Anagrafica.CODICE_ESTERNO));
		((ClientiViewHolder)viewholder).codice_esterno.setTag(position);
		
		//if (cap_citta.equals(" - ")){
		//	((ClientiViewHolder)viewholder).cap_citta.setVisibility(View.GONE);
		//}
		//else{
		//	((ClientiViewHolder)viewholder).cap_citta.setVisibility(View.VISIBLE);
		//}
		
		super.personalizzaView(position, viewholder);
	}
	
	
	
}
