package pfa.app.econtab.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.utils.FaIcone;

/**
 * Adapter astratto del pannello laterale standard (layout pannello_menu_laterale.xml): righe bianche con
 * icona, tipo e nome, indentate per livello. La riga selezionata ha sfondo azzurrino e testi in grassetto.
 * <p>
 * Ogni maschera con un menu laterale estende questa classe e dice solo COSA mostrare (i quattro metodi
 * astratti); grafica, indentazione e evidenziazione della selezione sono qui e valgono per tutte.
 */
public abstract class MenuLateraleAdapter extends EConTabListViewAdapter {

	private static class Holder extends EConTabViewHolder {
		TextView tipo;
		TextView nome;
		TextView icona;
	}

	public MenuLateraleAdapter(Context context, ArrayList<Object> dati) {
		super(context, dati, R.layout.list_item_menu_laterale);
	}

	/** Etichetta piccola sopra il nome (es. "Cantiere", "Area"). */
	protected abstract String getTipoTesto(int position);

	protected abstract String getNomeTesto(int position);

	/** Glifo Font Awesome (costante di FaIcone): l'icona e' sempre nel colore di default dell'app. */
	protected abstract String getIconaGlifo(int position);

	/** Livello di indentazione: 1 = primo livello, 2 = annidato, ... */
	protected abstract int getLivello(int position);

	/** Posizione della riga da evidenziare, -1 se nessuna. */
	protected abstract int getPosizioneSelezionata();

	@Override
	protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
		Holder holder = new Holder();
		holder.tipo = (TextView) convertView.findViewById(R.id.tipo);
		holder.nome = (TextView) convertView.findViewById(R.id.nome);
		holder.icona = (TextView) convertView.findViewById(R.id.icona);
		return holder;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = super.getView(position, convertView, parent);
		Holder holder = (Holder) v.getTag();

		holder.tipo.setText(getTipoTesto(position));
		holder.nome.setText(getNomeTesto(position));
		FaIcone.applica(holder.icona, getIconaGlifo(position), getTipoTesto(position));

		// Le view vengono riciclate: sfondo e grassetto vanno sempre impostati in entrambi i sensi
		boolean selezionata = position == getPosizioneSelezionata();
		if (selezionata) {
			v.setBackgroundResource(R.drawable.bg_menu_laterale_sel);
		} else {
			v.setBackgroundColor(android.graphics.Color.TRANSPARENT);
		}
		int stile = selezionata ? Typeface.BOLD : Typeface.NORMAL;
		holder.tipo.setTypeface(null, stile);
		holder.nome.setTypeface(null, stile);

		int unitario = context.getResources().getDimensionPixelSize(R.dimen.margine);
		v.setPadding(unitario * getLivello(position), v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom());
		return v;
	}
}
