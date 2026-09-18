package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.db.table.ComponentiCantiere;
import pfa.app.econtab.utils.Utility;

public class ComposizioneComponenteAdapter extends EConTabListViewAdapter {

	private boolean componenteCantiere = false;

	private class ComposizioneComponenteViewHolder extends EConTabViewHolder {
		TextView descrizione = null;
		ImageView icona = null;

	}

	public ComposizioneComponenteAdapter(Context context, ArrayList<Object> dati, int layoutid) {
		super(context, dati, layoutid);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
		// TODO Auto-generated method stub
		ComposizioneComponenteViewHolder holder = new ComposizioneComponenteViewHolder();
		holder.descrizione = (TextView) convertView.findViewById(R.id.descrizione);
		holder.icona = (ImageView) convertView.findViewById(R.id.imageView_icona);

		return holder;
	}

	@Override
	protected void personalizzaView(int position, EConTabViewHolder viewholder) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		ContentValues val = (ContentValues) dati.get(position);
		if (isComponenteCantiere()) {
			((ComposizioneComponenteViewHolder) viewholder).descrizione.setText(val.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));

		} else {
			((ComposizioneComponenteViewHolder) viewholder).descrizione.setText(val.getAsString(Componenti.NOME_COMPONENTE));

		}

		((ComposizioneComponenteViewHolder) viewholder).icona.setImageBitmap(Utility.getIconaThumb(context, Componenti.PATH_ICONE,
				val.getAsString(Componenti.ICONA)));

		((ComposizioneComponenteViewHolder) viewholder).descrizione.setTag(position);
		((ComposizioneComponenteViewHolder) viewholder).icona.setTag(position);

		super.personalizzaView(position, viewholder);
	}

	public boolean isComponenteCantiere() {
		return componenteCantiere;
	}

	public void setComponenteCantiere(boolean componenteCantiere) {
		this.componenteCantiere = componenteCantiere;
	}

}
