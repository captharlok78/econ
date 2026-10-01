package com.ncfsistemi.econ.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.table.Costruttori;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Placche;

public class PlaccheAdapter extends EconListViewAdapter {

	private class PlaccheViewHolder extends EconViewHolder {
		TextView linea = null;
		TextView nomePlacca = null;

	}

	public PlaccheAdapter(Context context, ArrayList<Object> dati, int layoutid) {
		super(context, dati, layoutid);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected EconViewHolder impostaViewHolder(View convertView, int position) {
		// TODO Auto-generated method stub
		PlaccheViewHolder holder = new PlaccheViewHolder();
		holder.linea = (TextView) convertView.findViewById(R.id.nome_linea);
		holder.nomePlacca = (TextView) convertView.findViewById(R.id.nome_placca);

		return holder;
	}

	@Override
	protected void personalizzaView(int position, EconViewHolder viewholder) {
		// TODO Auto-generated method stub
		ContentValues val = (ContentValues) dati.get(position);
		String linea = "";
		String siglaMetel = val.getAsString(Costruttori.SIGLA_METEL);
		String nomeLinea = val.getAsString(Linee.NOME_LINEA);
		if (siglaMetel != null) {
			linea = siglaMetel + " - ";
		}
		if (nomeLinea != null) {
			linea = linea + nomeLinea;
		}
		((PlaccheViewHolder) viewholder).linea.setText(linea);
		((PlaccheViewHolder) viewholder).linea.setTag(position);

		((PlaccheViewHolder) viewholder).nomePlacca.setText(val.getAsString(Placche.NOME_PLACCA));
		((PlaccheViewHolder) viewholder).nomePlacca.setTag(position);

		super.personalizzaView(position, viewholder);
	}
}
