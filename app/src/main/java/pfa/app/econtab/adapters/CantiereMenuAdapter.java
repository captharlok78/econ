package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;

import java.util.ArrayList;

import pfa.app.econtab.CantiereSplitActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.utils.FaIcone;

/**
 * Menu laterale del cantiere (Preventivo / Cantiere / Unita' / Area / Locale): specializza il pannello
 * standard MenuLateraleAdapter dicendo cosa mostrare per ogni riga. Grafica e selezione sono nel padre.
 */
public class CantiereMenuAdapter extends MenuLateraleAdapter {
	private final CantiereSplitActivity activity;

	public CantiereMenuAdapter(Context context, ArrayList<Object> dati) {
		super(context, dati);
		activity = (CantiereSplitActivity) context;
	}

	private ContentValues riga(int position) {
		return (ContentValues) dati.get(position);
	}

	@Override
	protected String getTipoTesto(int position) {
		return getTipoStringa(riga(position), context);
	}

	@Override
	protected String getNomeTesto(int position) {
		return riga(position).getAsString("NOME");
	}

	@Override
	protected String getIconaGlifo(int position) {
		return getGlifo(riga(position).getAsInteger("TIPO"));
	}

	@Override
	protected int getLivello(int position) {
		int tipo = riga(position).getAsInteger("TIPO");
		if (tipo == CantiereSplitActivity.AREA) {
			return 2;
		}
		if (tipo == CantiereSplitActivity.LOCALE) {
			return 3;
		}
		return 1; // preventivo, cantiere, unita'
	}

	@Override
	protected int getPosizioneSelezionata() {
		return activity.getSelezionato();
	}

	public static String getGlifo(int tipo) {
		if (tipo == CantiereSplitActivity.PREVENTIVO) {
			return FaIcone.PREVENTIVO;
		}
		if (tipo == CantiereSplitActivity.UNITA) {
			return FaIcone.UNITA;
		}
		if (tipo == CantiereSplitActivity.AREA) {
			return FaIcone.AREA;
		}
		if (tipo == CantiereSplitActivity.LOCALE) {
			return FaIcone.LOCALE;
		}
		return FaIcone.CANTIERE;
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
