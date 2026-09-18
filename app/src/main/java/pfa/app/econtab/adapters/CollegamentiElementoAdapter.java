package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Collegamenti;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.utils.Utility;

public class CollegamentiElementoAdapter extends EConTabListViewAdapter {

	private class CollegamentiElementoViewHolder extends EConTabViewHolder {
		TextView tipo = null;
		TextView nome = null;
		TextView metri = null;
		TextView localeA = null;
		TextView localeB = null;
		TextView elementoA = null;
		TextView elementoB = null;
		TextView componenteA = null;
		TextView componenteB = null;
	}

	public CollegamentiElementoAdapter(Context context, ArrayList<Object> dati, int layoutid) {
		super(context, dati, layoutid);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
		// TODO Auto-generated method stub
		CollegamentiElementoViewHolder holder = new CollegamentiElementoViewHolder();
		holder.tipo = (TextView) convertView.findViewById(R.id.tipo);
		holder.nome = (TextView) convertView.findViewById(R.id.nome);
		holder.metri = (TextView) convertView.findViewById(R.id.metri);
		holder.localeA = (TextView) convertView.findViewById(R.id.textViewLocaleA);
		holder.localeB = (TextView) convertView.findViewById(R.id.textViewLocaleB);
		holder.elementoA = (TextView) convertView.findViewById(R.id.textViewElementoA);
		holder.elementoB = (TextView) convertView.findViewById(R.id.textViewElementoB);
		holder.componenteA = (TextView) convertView.findViewById(R.id.textViewComponenteA);
		holder.componenteB = (TextView) convertView.findViewById(R.id.textViewComponenteB);
		return holder;
	}

	@Override
	protected void personalizzaView(int position, EConTabViewHolder viewholder) {
		// TODO Auto-generated method stub
		CollegamentiElementoViewHolder myHolder = (CollegamentiElementoViewHolder) viewholder;
		ContentValues val = (ContentValues) dati.get(position);

		if (val.getAsInteger(Collegamenti.ID_TUBO) != 0) {
			myHolder.tipo.setText(getString(R.string.tubo));
		}
		if (val.getAsInteger(Collegamenti.ID_CAVO) != 0) {
			myHolder.tipo.setText(getString(R.string.cavo));
		}
		myHolder.tipo.setTag(position);

		myHolder.nome.setText(val.getAsString("NOME"));
		myHolder.nome.setTag(position);

		myHolder.metri.setText(getString(R.string.metri) + " " + Utility.formatNumero(val.getAsDouble(Collegamenti.METRI), 2));
		myHolder.metri.setTag(position);

		myHolder.localeA.setText(val.getAsString("LOCALE_A"));
		myHolder.localeA.setTag(position);

		myHolder.localeB.setText(val.getAsString("LOCALE_B"));
		myHolder.localeB.setTag(position);

		myHolder.elementoA.setText(val.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO) + " - "
				+ val.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
		myHolder.elementoA.setTag(position);

		myHolder.elementoB.setText(val.getAsString("numero_identificativo_2") + " - " + val.getAsString("nome_elemento_cant2"));
		myHolder.elementoB.setTag(position);

		myHolder.componenteA.setText(val.getAsString("COMPONENTE_A"));
		myHolder.componenteA.setTag(position);

		myHolder.componenteB.setText(val.getAsString("COMPONENTE_B"));
		myHolder.componenteB.setTag(position);

		super.personalizzaView(position, viewholder);
	}

}
