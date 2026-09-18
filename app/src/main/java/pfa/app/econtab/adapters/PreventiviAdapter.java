package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Observable;

import pfa.app.econtab.EConTabActivity;
import pfa.app.econtab.PreventiviActivity;
import pfa.app.econtab.PreventiviDettaglioModActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.fragments.PreventiviListaFragment;
import pfa.app.econtab.utils.Utility;

public class PreventiviAdapter extends EConTabListViewAdapter {

	private PreventiviListaFragment fragmentPreventivi = null;

	private class PreventiviViewHolder extends EConTabViewHolder {
		TextView numero = null;
		TextView data = null;
		TextView cliente = null;
		TextView cantiere = null;
		TextView preventivo = null;
		TextView stato = null;
		Button buttonAzioni = null;

	}

	public PreventiviAdapter(Context context, ArrayList<Object> dati, int layoutid) {
		super(context, dati, layoutid);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
		// TODO Auto-generated method stub
		PreventiviViewHolder holder = new PreventiviViewHolder();
		holder.numero = (TextView) convertView.findViewById(R.id.numero);
		holder.data = (TextView) convertView.findViewById(R.id.data);
		holder.cliente = (TextView) convertView.findViewById(R.id.ragione_sociale);
		holder.cantiere = (TextView) convertView.findViewById(R.id.cantiere);
		holder.preventivo = (TextView) convertView.findViewById(R.id.nome);
		holder.stato = (TextView) convertView.findViewById(R.id.stato);
		holder.buttonAzioni = (Button) convertView.findViewById(R.id.buttonAzioni);

		return holder;
	}

	@Override
	protected void personalizzaView(int position, EConTabViewHolder viewholder) {

		// TODO Auto-generated method stub
		final ContentValues val = (ContentValues) dati.get(position);
		((PreventiviViewHolder) viewholder).numero.setText("N. " + val.getAsString(Preventivi.NUMERO) + "/"
				+ val.getAsString(Preventivi.ANNO));
		((PreventiviViewHolder) viewholder).numero.setTag(position);

		((PreventiviViewHolder) viewholder).data.setText(Utility.numberToData(val.getAsLong(Preventivi.DATA)));
		((PreventiviViewHolder) viewholder).data.setTag(position);

		((PreventiviViewHolder) viewholder).cliente.setText(val.getAsString(Anagrafica.RAGIONE_SOCIALE));
		((PreventiviViewHolder) viewholder).cliente.setTag(position);

		((PreventiviViewHolder) viewholder).cantiere.setText(val.getAsString(Cantieri.NOME));
		((PreventiviViewHolder) viewholder).cantiere.setTag(position);

		((PreventiviViewHolder) viewholder).preventivo.setText(val.getAsString(Preventivi.TITOLO));
		((PreventiviViewHolder) viewholder).preventivo.setTag(position);

		((PreventiviViewHolder) viewholder).stato.setText(new Preventivi().getDescrizioneStatoPreventivo(val.getAsString(Preventivi.STATO),
				context));
		((PreventiviViewHolder) viewholder).stato.setTag(position);

		final String tipo = val.getAsString(Preventivi.TIPO);
		((PreventiviViewHolder) viewholder).buttonAzioni.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String[] opzioni = null;
				if (tipo.equals(Preventivi.TIPO_ORDINE)){
					opzioni = new String[2];
					opzioni[0] = context.getString(R.string.modifica);
					opzioni[1] = context.getString(R.string.cambia_stato);
				}
				if (tipo.equals(Preventivi.TIPO_PREVENTIVO)){
					String stato = val.getAsString(Preventivi.STATO);
					if (stato.equals(Preventivi.STATO_APERTO)) {
						opzioni = new String[3];
						opzioni[0] = context.getString(R.string.modifica);
						opzioni[1] = context.getString(R.string.cambia_stato);
						opzioni[2] = context.getString(R.string.trasforma_in_ordine);
					}
					else{
						opzioni = new String[2];
						opzioni[0] = context.getString(R.string.modifica);
						opzioni[1] = context.getString(R.string.cambia_stato);
					}

				}
				Utility.mostraSelezioneDialog("", opzioni, context, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						if (i==0){
							modifica(val);
						}
						if (i==1){
							cambiaStato(val);
						}
						if (i==2){
							trasformaInOrdine(val);
						}
					}
				});
			}
		});

		/*if (tipo.equals(Preventivi.TIPO_ORDINE)) {
			((PreventiviViewHolder) viewholder).buttonAzioni.setVisibility(View.GONE);
		} else {
			String stato = val.getAsString(Preventivi.STATO);
			if (stato.equals(Preventivi.STATO_APERTO)) {
				((PreventiviViewHolder) viewholder).buttonAzioni.setVisibility(View.VISIBLE);
				((PreventiviViewHolder) viewholder).buttonAzioni.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						trasformaInOrdine(val);
					}
				});
			} else {
				((PreventiviViewHolder) viewholder).buttonAzioni.setVisibility(View.GONE);
			}
		}*/

		((PreventiviViewHolder) viewholder).buttonAzioni.setTag(position);

		super.personalizzaView(position, viewholder);
	}

	private void cambiaStato(final ContentValues valPrev) {
		final String tipo = valPrev.getAsString(Preventivi.TIPO);
		ArrayList<Object> stati = null;
		if (tipo.equals(Preventivi.TIPO_PREVENTIVO)){
			stati = new Preventivi().getStatiPreventivo(context);
		}
		else{
			if (valPrev.getAsString(Preventivi.STATO).equals(Preventivi.STATO_CHIUSO)){
				Utility.mostraDialog(getString(R.string.attenzione),getString(R.string.messaggio_ordine_chiuso),context,"OK");
				return;
			}
			stati = new Preventivi().getStatiOrdine(context);
		}

		String[] opzioni = new String[stati.size()];
		for (int i=0;i<stati.size();i++){
			opzioni[i] = ((ContentValues)stati.get(i)).getAsString("DESC");
		}
		final ArrayList<Object> statiFinal = new ArrayList<Object>();
		statiFinal.addAll(stati);
		Utility.mostraSelezioneDialog(getString(R.string.cambia_stato), opzioni, context, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {

				final ContentValues val = (ContentValues)statiFinal.get(i);
				if (tipo.equals(Preventivi.TIPO_ORDINE) && val.getAsString("VAL").equals(Preventivi.STATO_CHIUSO)){
					Utility.mostraConfermaDialog(getString(R.string.attenzione), getString(R.string.messaggio_chiusura_ordine), context, "OK", getString(R.string.annulla), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							if (i==DialogInterface.BUTTON_POSITIVE){
								_eseguiCambioStato(val,valPrev);
								Preventivi tabPrev = new Preventivi();
								DbInterno db = new DbInterno(context);
								tabPrev.chiudiOrdine(db,valPrev.getAsInteger(Preventivi.ID_PREVENTIVO));
								db.close();

							}
						}
					});
				}else{
					_eseguiCambioStato(val,valPrev);
				}
			}
		});
	}

	private void _eseguiCambioStato(ContentValues val,ContentValues valPrev){
		DbInterno db = new DbInterno(context);
		ContentValues upd = new ContentValues();
		upd.put(Preventivi.STATO,val.getAsString("VAL"));
		ContentValues where = new ContentValues();
		where.put(Preventivi.ID_PREVENTIVO,valPrev.getAsInteger(Preventivi.ID_PREVENTIVO));
		Preventivi tabPrev = new Preventivi();
		tabPrev.aggiornaRecord(db, upd, where);
		db.close();
		if (context instanceof PreventiviActivity) {
			((PreventiviActivity) context).refresh();
		}
	}

	private void modifica(ContentValues val) {
		Intent intent = new Intent(context, PreventiviDettaglioModActivity.class);
		intent.putExtra("ID", val.getAsInteger(Preventivi.ID_PREVENTIVO));
		((EConTabActivity)context).apriFinestraModifica(intent, 2);

	}

	protected void trasformaInOrdine(ContentValues val) {
		// TODO Auto-generated method stub
		if (fragmentPreventivi != null) {
			fragmentPreventivi.trasformaInOrdine(val);
		}

	}

	public void setPreventiviFragment(PreventiviListaFragment preventiviListaFragment) {
		// TODO Auto-generated method stub
		this.fragmentPreventivi = preventiviListaFragment;
	}

}
