package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Listini;
import pfa.app.econtab.utils.Utility;

public class CodiciArticoliAdapter extends EConTabListViewAdapter {

	private class CodiciArticoliViewHolder extends EConTabViewHolder {
		TextView codice = null;
		TextView descrzione = null;
		TextView fornitore_linea = null;
		TextView prz_acq = null;
		TextView prz_ven = null;
		TextView sconto = null;
		ImageView imgass = null;

	}

	public CodiciArticoliAdapter(Context context, ArrayList<Object> dati, int layoutid) {
		super(context, dati, layoutid);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
		// TODO Auto-generated method stub
		CodiciArticoliViewHolder holder = new CodiciArticoliViewHolder();
		holder.codice = (TextView) convertView.findViewById(R.id.codice_articolo);
		holder.descrzione = (TextView) convertView.findViewById(R.id.descrizione);
		holder.fornitore_linea = (TextView) convertView.findViewById(R.id.fornitore_linea);
		holder.prz_acq = (TextView) convertView.findViewById(R.id.prz_acq);
		holder.prz_ven = (TextView) convertView.findViewById(R.id.prz_ven);
		holder.sconto = (TextView) convertView.findViewById(R.id.sconto);
		holder.imgass = (ImageView) convertView.findViewById(R.id.imgass);
		return holder;
	}

	@Override
	protected void personalizzaView(int position, EConTabViewHolder viewholder) {
		// TODO Auto-generated method stub
		ContentValues val = (ContentValues) dati.get(position);
		((CodiciArticoliViewHolder) viewholder).codice.setText(val.getAsString(Listini.CODICE_ARTICOLO));
		((CodiciArticoliViewHolder) viewholder).codice.setTag(position);

		((CodiciArticoliViewHolder) viewholder).descrzione.setText(val.getAsString(Listini.DESCRIZIONE));
		((CodiciArticoliViewHolder) viewholder).descrzione.setTag(position);

		String fornitore_linea = "";
		// if (val.containsKey(Costruttori.SIGLA_METEL) && val.getAsString(Costruttori.SIGLA_METEL) != null) {
		// fornitore_linea = val.getAsString(Costruttori.SIGLA_METEL);
		// }
		if (val.containsKey(Linee.NOME_LINEA) && val.getAsString(Linee.NOME_LINEA) != null) {
			// fornitore_linea = fornitore_linea + " - " + val.getAsString(Linee.NOME_LINEA);
			fornitore_linea = val.getAsString(Linee.NOME_LINEA);
		}

		if (val.containsKey("asscodicilinee") && val.getAsInteger("asscodicilinee")>0){
			((CodiciArticoliViewHolder) viewholder).imgass.setVisibility(View.VISIBLE);
		}
		else {
			((CodiciArticoliViewHolder) viewholder).imgass.setVisibility(View.INVISIBLE);
		}

		((CodiciArticoliViewHolder) viewholder).fornitore_linea.setText(fornitore_linea);
		((CodiciArticoliViewHolder) viewholder).fornitore_linea.setTag(position);

		double przAcq = val.getAsDouble(Listini.PRZ_ULTIMO_ACQUISTO);
		double przVen = val.getAsDouble(Listini.PRZ_LISTINO);
		double sconto = val.getAsDouble(Listini.SCONTO);

		((CodiciArticoliViewHolder) viewholder).prz_acq
				.setText(getString(R.string.prezzo_acquisto) + " " + Utility.formatNumero(przAcq, 2));
		((CodiciArticoliViewHolder) viewholder).prz_acq.setTag(position);
		((CodiciArticoliViewHolder) viewholder).prz_ven.setText(getString(R.string.prezzo_listino) + " " + Utility.formatNumero(przVen, 2));
		((CodiciArticoliViewHolder) viewholder).prz_ven.setTag(position);
		((CodiciArticoliViewHolder) viewholder).sconto.setText(getString(R.string.sconto) + " % " + Utility.formatNumero(sconto, 2));
		((CodiciArticoliViewHolder) viewholder).sconto.setTag(position);

		super.personalizzaView(position, viewholder);
	}
}
