package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.utils.BitmapWorkerTask;

public class GrigliaElementiAdapter extends EConTabListViewAdapter {

	private String tabella = Elementi.NOME_TABELLA;

	private class ElementoViewHolder extends EConTabViewHolder {
		TextView nome = null;
		ImageView icona = null;

		Button modifica = null;
	}

	public GrigliaElementiAdapter(Context context, ArrayList<Object> dati, int layoutid) {
		// TODO Auto-generated constructor stub
		super(context, dati, layoutid);

	}

	public void setDati(ArrayList<Object> newDati) {
		this.dati = newDati;
	}

	@Override
	protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
		// TODO Auto-generated method stub
		ElementoViewHolder holder = new ElementoViewHolder();
		holder.nome = (TextView) convertView.findViewById(R.id.nome);
		holder.icona = (ImageView) convertView.findViewById(R.id.icona);
		// per adesso nascondo la modifica diretta (se servir� in futuro la riattivo)
		holder.modifica = (Button) convertView.findViewById(R.id.button_modifica_item);
		holder.modifica.setVisibility(View.GONE);

		return holder;
	}

	@Override
	protected void personalizzaView(int position, EConTabViewHolder viewholder) {
		// TODO Auto-generated method stub
		final ContentValues val = (ContentValues) dati.get(position);
		// per adesso nascondo la modifica diretta (se servir� in futuro la riattivo)

		/*
		 * ((ElementoViewHolder) viewholder).modifica.setOnClickListener(null); ((ElementoViewHolder)
		 * viewholder).modifica.setOnClickListener(new View.OnClickListener() {
		 * 
		 * @Override public void onClick(View arg0) { // TODO Auto-generated method stub
		 * 
		 * if (getTabella().equals(Componenti.NOME_TABELLA)) { Intent intent = new Intent(context,
		 * ComponenteModActivity.class); intent.putExtra("ID", val.getAsInteger(Componenti.ID_COMPONENTE));
		 * intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE,
		 * val.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE)); ((EConTabActivity) context).apriFinestraModifica(intent,
		 * 2); } if (getTabella().equals(Elementi.NOME_TABELLA)) { Intent intent = new Intent(context,
		 * ElementoModActivity.class); intent.putExtra("ID", val.getAsInteger(Elementi.ID_ELEMENTO));
		 * intent.putExtra(CategorieGenerali.ID_CATEGORIA_GENERALE, val.getAsInteger(Elementi.ID_CATEGORIA_GENERALE));
		 * ((EConTabActivity) context).apriFinestraModifica(intent, 2); }
		 * 
		 * }
		 * 
		 * });
		 */

		if (getTabella().equals(Elementi.NOME_TABELLA)) {
			((ElementoViewHolder) viewholder).nome.setText(val.getAsString(Elementi.NOME_ELEMENTO));
			((ElementoViewHolder) viewholder).nome.setTag(position);

			String icona = val.getAsString(Elementi.ICONA);
			//((ElementoViewHolder)viewholder).icona.setVisibility(View.INVISIBLE);
            BitmapWorkerTask task = new BitmapWorkerTask(context,Elementi.PATH_ICONE,((ElementoViewHolder) viewholder).icona,false);
            int altezza = context.getResources().getDimensionPixelSize(R.dimen.larghezza_icona);
           // task.setDimensioni(altezza,altezza);
            task.caricaIcona(icona);
			//((ElementoViewHolder) viewholder).icona.setImageBitmap(Utility.getIconaThumb(context, Elementi.PATH_ICONE, icona));
			((ElementoViewHolder) viewholder).icona.setTag(position);
		}

		if (getTabella().equals(Componenti.NOME_TABELLA)) {
			((ElementoViewHolder) viewholder).nome.setText(val.getAsString(Componenti.NOME_COMPONENTE));
			((ElementoViewHolder) viewholder).nome.setTag(position);

			String icona = val.getAsString(Componenti.ICONA);
			//((ElementoViewHolder)viewholder).icona.setVisibility(View.INVISIBLE);
            BitmapWorkerTask task = new BitmapWorkerTask(context,Componenti.PATH_ICONE,((ElementoViewHolder) viewholder).icona,false);
            int altezza = context.getResources().getDimensionPixelSize(R.dimen.larghezza_icona);
           // task.setDimensioni(altezza,altezza);
            task.caricaIcona(icona);
			//((ElementoViewHolder) viewholder).icona.setImageBitmap(Utility.getIconaThumb(context, Componenti.PATH_ICONE, icona));
			((ElementoViewHolder) viewholder).icona.setTag(position);
		}

		super.personalizzaView(position, viewholder);
	}
	
	public String getTabella() {
		return tabella;
	}

	public void setTabella(String tabella) {
		this.tabella = tabella;
	}




}
