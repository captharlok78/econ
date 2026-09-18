package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import pfa.app.econtab.CantiereSplitActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Preventivi;

public class CantiereMenuAdapter extends EConTabListViewAdapter {
	CantiereSplitActivity activity = null;

	private class CantiereMenuViewHolder extends EConTabViewHolder {
		TextView tipo = null;
		TextView nome = null;
		ImageView icona = null;

	}

	public CantiereMenuAdapter(Context context, ArrayList<Object> dati, int layoutid) {
		super(context, dati, layoutid);
		activity = (CantiereSplitActivity) context;
		// TODO Auto-generated constructor stub
	}

	@Override
	protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
		// TODO Auto-generated method stub
		CantiereMenuViewHolder holder = new CantiereMenuViewHolder();
		holder.tipo = (TextView) convertView.findViewById(R.id.tipo);
		holder.nome = (TextView) convertView.findViewById(R.id.nome);
		// holder.nome.setMovementMethod(new ScrollingMovementMethod());
		holder.icona = (ImageView) convertView.findViewById(R.id.icona);

		return holder;
	}

	@Override
	protected void personalizzaView(int position, EConTabViewHolder viewholder) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		ContentValues val = (ContentValues) dati.get(position);
		int tipo = val.getAsInteger("TIPO");
		((CantiereMenuViewHolder) viewholder).tipo.setText(getTipoStringa(val, context));
		((CantiereMenuViewHolder) viewholder).tipo.setTag(position);

		((CantiereMenuViewHolder) viewholder).nome.setText(val.getAsString("NOME"));
		((CantiereMenuViewHolder) viewholder).nome.setTag(position);

		((CantiereMenuViewHolder) viewholder).icona.setImageResource(getIcona(tipo, context));
		((CantiereMenuViewHolder) viewholder).icona.setTag(position);

		super.personalizzaView(position, viewholder);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View v = super.getView(position, convertView, parent);
		if (position == activity.getSelezionato()) {
			v.setBackgroundResource(R.drawable.bg_econtab_sel);
		} else {
			v.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
		}
		ContentValues val = (ContentValues) dati.get(position);
		int tipo = val.getAsInteger("TIPO");
		int paddingunitario = context.getResources().getDimensionPixelSize(R.dimen.margine);
		if (tipo == CantiereSplitActivity.UNITA || tipo == CantiereSplitActivity.CANTIERE || tipo == CantiereSplitActivity.PREVENTIVO) {
			v.setPadding(paddingunitario, v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom());
		}
		if (tipo == CantiereSplitActivity.AREA) {
			v.setPadding(paddingunitario * 2, v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom());
		}
		if (tipo == CantiereSplitActivity.LOCALE) {
			v.setPadding(paddingunitario * 3, v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom());
		}

		return v;

	}

	public static int getIcona(int tipo, Context context) {
		if (tipo == CantiereSplitActivity.PREVENTIVO) {
			return R.drawable.ordini_preventivi_small;
		}
		if (tipo == CantiereSplitActivity.CANTIERE) {
			return R.drawable.icona_cantiere;
		}
		if (tipo == CantiereSplitActivity.UNITA) {
			return R.drawable.icona_unita;
		}
		if (tipo == CantiereSplitActivity.AREA) {
			return R.drawable.icona_area;
		}
		if (tipo == CantiereSplitActivity.LOCALE) {
			return R.drawable.icona_locale;
		}
		return R.drawable.icona_cantiere;
	}

	public static String getTipoStringa(ContentValues item, Context context) {
        int tipo = item.getAsInteger("TIPO");
		if (tipo == CantiereSplitActivity.PREVENTIVO) {
            if (item.containsKey("TIPO_PREV_ORD") && item.getAsString("TIPO_PREV_ORD").equals(Preventivi.TIPO_ORDINE)){
                return context.getResources().getString(R.string.dettaglio_ordine);
            }
            return context.getResources().getString(R.string.dettaglio_preventivo);

		}
		if (tipo == CantiereSplitActivity.CANTIERE) {
			return context.getResources().getString(R.string.cantiere);
		}
		if (tipo == CantiereSplitActivity.UNITA) {
			return context.getResources().getString(R.string.unita);
		}
		if (tipo == CantiereSplitActivity.AREA) {
			return context.getResources().getString(R.string.area);
		}
		if (tipo == CantiereSplitActivity.LOCALE) {
			return context.getResources().getString(R.string.locale);
		}

		return "";
	}

}
