package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Anagrafica;

public class ClientiAdapter extends EConTabListViewAdapter {

	private class ClientiViewHolder extends EConTabViewHolder{
		TextView ragionesociale = null;
		TextView indirizzo = null;
		TextView citta = null;
		TextView provincia = null;
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
		holder.citta = (TextView)convertView.findViewById(R.id.citta);
		holder.provincia = (TextView)convertView.findViewById(R.id.provincia);
		holder.codice_esterno = (TextView)convertView.findViewById(R.id.codice_esterno);
		return holder;
	}


	@Override
	protected void personalizzaView(int position, EConTabViewHolder viewholder) {
		// TODO Auto-generated method stub
		ContentValues val = (ContentValues)dati.get(position);
		((ClientiViewHolder)viewholder).ragionesociale.setText(val.getAsString(Anagrafica.RAGIONE_SOCIALE));
		((ClientiViewHolder)viewholder).ragionesociale.setTag(position);

		((ClientiViewHolder)viewholder).indirizzo.setText(val.getAsString(Anagrafica.INDIRIZZO));
		((ClientiViewHolder)viewholder).indirizzo.setTag(position);

		((ClientiViewHolder)viewholder).citta.setText(val.getAsString(Anagrafica.CITTA));
		((ClientiViewHolder)viewholder).citta.setTag(position);

		((ClientiViewHolder)viewholder).provincia.setText(val.getAsString(Anagrafica.PROVINCIA));
		((ClientiViewHolder)viewholder).provincia.setTag(position);

		((ClientiViewHolder)viewholder).codice_esterno.setText(val.getAsString(Anagrafica.CODICE_ESTERNO));
		((ClientiViewHolder)viewholder).codice_esterno.setTag(position);

		super.personalizzaView(position, viewholder);
	}



}
