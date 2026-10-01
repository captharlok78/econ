package com.ncfsistemi.econ.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.table.Collegamenti;
import com.ncfsistemi.econ.db.table.Elementi;
import com.ncfsistemi.econ.utils.Utility;

public class TubiPassantiAdapter extends EconListViewAdapter {

	private class TubiPassantiViewHolder extends EconViewHolder {

		TextView nome = null;
		TextView metri = null;
		TextView cavi = null;
		ImageView seleziona = null;
	}

	public TubiPassantiAdapter(Context context, ArrayList<Object> dati, int layoutid) {
		super(context, dati, layoutid);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected EconViewHolder impostaViewHolder(View convertView, int position) {
		// TODO Auto-generated method stub
		TubiPassantiViewHolder holder = new TubiPassantiViewHolder();

		holder.nome = (TextView) convertView.findViewById(R.id.nome);
		holder.metri = (TextView) convertView.findViewById(R.id.metri);
		holder.cavi = (TextView) convertView.findViewById(R.id.textViewCavi);
		holder.seleziona = (ImageView) convertView.findViewById(R.id.imgSeleziona);

		return holder;
	}

	@Override
	protected void personalizzaView(int position, EconViewHolder viewholder) {
		// TODO Auto-generated method stub
		TubiPassantiViewHolder myHolder = (TubiPassantiViewHolder) viewholder;
		ContentValues val = (ContentValues) dati.get(position);

		myHolder.nome.setText(val.getAsString(Elementi.NOME_ELEMENTO));
		myHolder.nome.setTag(position);

		myHolder.metri.setText(getString(R.string.metri) + " " + Utility.formatNumero(val.getAsDouble(Collegamenti.METRI), 2));
		myHolder.metri.setTag(position);

		int num_cavi = 0;
		int num_collegamenti = 0;
		try {
			num_cavi = val.getAsInteger("NUM_CAVI");
			num_collegamenti = val.getAsInteger("COUNT_CAVI");
		} catch (Exception e) {

		}

		myHolder.cavi.setText(getString(R.string.numero_cavi_passati) + ": " + num_cavi + " (" + num_collegamenti + " "
				+ getString(R.string.collegamenti) + ")");
		myHolder.cavi.setTag(position);

		if (val.getAsBoolean("SELEZIONATO") == true) {
			myHolder.seleziona.setImageResource(android.R.drawable.checkbox_on_background);
		} else {
			myHolder.seleziona.setImageResource(android.R.drawable.checkbox_off_background);
		}

		myHolder.seleziona.setTag(position);

		// myHolder.seleziona.setVisibility(View.GONE);

		super.personalizzaView(position, viewholder);
	}

}
