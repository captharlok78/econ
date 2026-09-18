package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import pfa.app.econtab.EConTabActivity;
import pfa.app.econtab.LineeDettaglioModActivity;
import pfa.app.econtab.PlaccheActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Placche;

public class FornitoriLineeAdapter extends EConTabExpListViewAdapter {
	private ArrayList<Object> sezioniRagioniSociali = null;

	private class FornitoriLineeViewHolder extends EConTabViewHolder {
		TextView linea;
		Button buttonPlacche;
	}

	public FornitoriLineeAdapter(EConTabActivity context, ArrayList<String> listDataHeader, HashMap<String, ArrayList<Object>> listChildData,
			int grouplayoutid, int childlayoutid, ArrayList<Object> sezioniRagioniSociali) {
		super(context, listDataHeader, listChildData, grouplayoutid, childlayoutid);
		this.sezioniRagioniSociali = sezioniRagioniSociali;
	}

	@Override
	protected void personalizzaGroupView(final int groupPosition, boolean isExpanded, View convertView) {
		// TODO Auto-generated method stub
		ContentValues val = (ContentValues) sezioniRagioniSociali.get(groupPosition);
		context.setText(R.id.fornitore, val.getAsString(Costruttori.RAGIONE_SOCIALE), convertView);
		context.setText(R.id.siglametel, context.getString(R.string.siglametel) + ":  " + val.getAsString(Costruttori.SIGLA_METEL),
				convertView);
		Button buttonNuovo = (Button) convertView.findViewById(R.id.buttonNuovo);

		buttonNuovo.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				Intent intent = new Intent(context, LineeDettaglioModActivity.class);
				intent.putExtra(Linee.ID_COSTRUTTORE, Integer.parseInt(listDataHeader.get(groupPosition)));
				context.apriFinestraInserimento(intent, 1, new Linee());

			}
		});
		super.personalizzaGroupView(groupPosition, isExpanded, convertView);
	}

	@Override
	protected void personalizzaChildView(int groupPosition, int childPosition, boolean isLastChild, EConTabViewHolder holder) {
		// TODO Auto-generated method stub
		final ContentValues val = (ContentValues) listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition);
		int[] pos = new int[2];
		pos[0] = childPosition;
		pos[1] = groupPosition;
		int idLinea = val.getAsInteger(Linee.ID_LINEA);
		if (idLinea != 0) {
			((FornitoriLineeViewHolder) holder).linea.setText(context.getResources().getString(R.string.linea) + " "
					+ val.getAsString(Linee.NOME_LINEA));
			((FornitoriLineeViewHolder) holder).buttonPlacche.setVisibility(View.VISIBLE);
		} else {
			((FornitoriLineeViewHolder) holder).linea.setText(val.getAsString(Linee.NOME_LINEA));

			((FornitoriLineeViewHolder) holder).buttonPlacche.setVisibility(View.GONE);
		}
		((FornitoriLineeViewHolder) holder).linea.setTag(pos);
		((FornitoriLineeViewHolder) holder).buttonPlacche.setTag(pos);

		((FornitoriLineeViewHolder) holder).buttonPlacche.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// Toast.makeText(context, "Linea " + val.getAsString(Linee.ID_LINEA), Toast.LENGTH_SHORT).show();
				Intent intent = new Intent(context, PlaccheActivity.class);
				intent.putExtra(Placche.ID_LINEA, Integer.parseInt(val.getAsString(Linee.ID_LINEA)));
				context.startActivity(intent);
				// context.apriFinestraInserimento(intent, 1);

			}
		});
		super.personalizzaChildView(groupPosition, childPosition, isLastChild, holder);
	}

	@Override
	protected EConTabViewHolder impostaViewHolder(View convertView, int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		FornitoriLineeViewHolder holder = new FornitoriLineeViewHolder();
		holder.linea = (TextView) convertView.findViewById(R.id.linea);
		holder.buttonPlacche = (Button) convertView.findViewById(R.id.buttonPlacche);

		return holder;
	}

}
