package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import pfa.app.econtab.R;
import pfa.app.econtab.RapportinoDettaglioModActivity;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.PlaccheModuli;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.Rapportini;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.fragments.PreventivoDettaglioFragment;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.PopupAssociazioneCodiceListino;
import pfa.app.econtab.views.PopupModificaRigaPreventivo;

public class PreventiviDettaglioAdapter extends EConTabListViewAdapter {

	private PreventivoDettaglioFragment fragmnent = null;
	private PreventiviDettaglio tabDett = null;

	private class PreventiviDettaglioViewHolder extends EConTabViewHolder {
		TextView descrizione = null;
		TextView codice = null;
		TextView qta = null;
		TextView udm = null;
		TextView przAcquisto = null;
		TextView ricarico = null;
		TextView przListino = null;
		TextView sconto = null;
		TextView prezzo = null;
		TextView importo = null;
		Button buttonPiu = null;
		Button buttonMeno = null;
		View viewDescrizione = null;
		TextView inModifica = null;
        LinearLayout linearRapportini = null;
        TextView oreRapportini = null;
        TextView importoRapportini = null;

	}

	private class PreventiviDettaglioLocaleViewHolder extends EConTabViewHolder {
		TextView unitaArea = null;
		TextView locale = null;
		TextView linea = null;
		View linearLinea = null;

	}

	public PreventiviDettaglioAdapter(Context context, ArrayList<Object> dati, int layoutid) {
		super(context, dati, layoutid);
		tabDett = new PreventiviDettaglio();
		// TODO Auto-generated constructor stub
	}

	@Override
	protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
		// TODO Auto-generated method stub

		if (getFragmnent().isRigaLocale(position)) {
			PreventiviDettaglioLocaleViewHolder holder = new PreventiviDettaglioLocaleViewHolder();
			holder.unitaArea = (TextView) convertView.findViewById(R.id.unitaArea);
			holder.locale = (TextView) convertView.findViewById(R.id.locale);
			holder.linea = (TextView) convertView.findViewById(R.id.linea);
			holder.linearLinea = convertView.findViewById(R.id.linear_linea);

			return holder;
		} else {
			PreventiviDettaglioViewHolder holder = new PreventiviDettaglioViewHolder();
			holder.descrizione = (TextView) convertView.findViewById(R.id.descrizione_riga);
			holder.codice = (TextView) convertView.findViewById(R.id.codice_articolo);
			holder.qta = (TextView) convertView.findViewById(R.id.txt_qta);
			holder.udm = (TextView) convertView.findViewById(R.id.udm);
			holder.przAcquisto = (TextView) convertView.findViewById(R.id.text_prxacq);
			holder.ricarico = (TextView) convertView.findViewById(R.id.text_ricarico);
			holder.przListino = (TextView) convertView.findViewById(R.id.text_prxLis);
			holder.sconto = (TextView) convertView.findViewById(R.id.text_sconto);
			holder.prezzo = (TextView) convertView.findViewById(R.id.text_prezzo);
			holder.importo = (TextView) convertView.findViewById(R.id.text_importo);
			holder.buttonPiu = (Button) convertView.findViewById(R.id.buttonPiu);
			holder.buttonMeno = (Button) convertView.findViewById(R.id.buttonMeno);
            holder.linearRapportini = (LinearLayout) convertView.findViewById(R.id.linear_rapportini);
            holder.oreRapportini = (TextView) convertView.findViewById(R.id.oreRapportini);
            holder.importoRapportini = (TextView) convertView.findViewById(R.id.importoRapportini);

			holder.viewDescrizione = convertView.findViewById(R.id.linear_descrizione);
			holder.inModifica = (TextView) convertView.findViewById(R.id.textView_inModifica);
			return holder;
		}

	}

	protected void aggiornaQta(int position, int q) {
		// TODO Auto-generated method stub
		ContentValues val = (ContentValues) dati.get(position);
		float qtaOld = val.getAsFloat(PreventiviDettaglio.QUANTITA);
		float newQta = qtaOld + q;
		if (newQta < 0) {
			newQta = 0;
		}

		val.put(PreventiviDettaglio.QUANTITA, newQta);
		setInModifica(position);
		notifyDataSetChanged();
	}

	@Override
	protected void personalizzaView(final int position, EConTabViewHolder viewholder) {
		System.out.println("EConTab: PreventivoDettaglioAdapter personalizzaView ENTER");
		System.out.println("EConTab: PreventivoDettaglioAdapter personalizzaView position " + position);
		// TODO Auto-generated method stub
		final ContentValues val = (ContentValues) dati.get(position);
		if (getFragmnent().isRigaLocale(position)) {
			System.out.println("EConTab: PreventivoDettaglioAdapter personalizzaView YES RIGA LOCALE");
			((PreventiviDettaglioLocaleViewHolder) viewholder).locale.setText(val.getAsString(Locali.NOME));
			((PreventiviDettaglioLocaleViewHolder) viewholder).locale.setTag(position);
			String tipo = val.getAsString(PreventiviDettaglio.TIPO);
			System.out.println("EConTab: PreventivoDettaglioAdapter personalizzaView tipo " + tipo);
			if (tipo.equals(PreventiviDettaglio.MANOPERA) || tipo.equals(PreventiviDettaglio.COLLEGAMENTI)
					|| tipo.equals(PreventiviDettaglio.ALTRO)) {
				((PreventiviDettaglioLocaleViewHolder) viewholder).unitaArea.setVisibility(View.GONE);
				((PreventiviDettaglioLocaleViewHolder) viewholder).linearLinea.setVisibility(View.GONE);
			} else {
				((PreventiviDettaglioLocaleViewHolder) viewholder).unitaArea.setText(val.getAsString("nomeArea"));
				((PreventiviDettaglioLocaleViewHolder) viewholder).unitaArea.setVisibility(View.VISIBLE);
				((PreventiviDettaglioLocaleViewHolder) viewholder).linearLinea.setVisibility(View.VISIBLE);
				((PreventiviDettaglioLocaleViewHolder) viewholder).linea.setText(val.getAsString(Linee.NOME_LINEA) + " - "
						+ val.getAsString(Costruttori.SIGLA_METEL));
				((PreventiviDettaglioLocaleViewHolder) viewholder).linea.setTag(position);
			}

		} else {
			System.out.println("EConTab: PreventivoDettaglioAdapter personalizzaView NO RIGA LOCALE");
			PreventiviDettaglioViewHolder holder = (PreventiviDettaglioViewHolder) viewholder;
			String tipo = val.getAsString(PreventiviDettaglio.TIPO);
			System.out.println("EConTab: PreventivoDettaglioAdapter personalizzaView tipo " + tipo);

			if (tipo.equals(PreventiviDettaglio.ALTRO)) {
				holder.codice.setVisibility(View.GONE);
			} else {
				holder.codice.setVisibility(View.VISIBLE);

			}

            if (tipo.equals(PreventiviDettaglio.MANOPERA) && fragmnent.getTipoPreventivoOrdine().equals(Preventivi.TIPO_ORDINE) ) {
                holder.linearRapportini.setVisibility(View.VISIBLE);
                holder.linearRapportini.setTag(position);
                holder.linearRapportini.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
_mostraRapportiniRiga(val.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO), val.getAsInteger(PreventiviDettaglio.ID_MANODOPERA));
                    }
                });

                holder.oreRapportini.setText(Utility.formatNumero(val.getAsDouble("ORE_RAPP")));
                holder.oreRapportini.setTag(position);

                holder.importoRapportini.setText(Utility.formatNumero(val.getAsDouble("IMPORTO_RAPP"), 2));
                holder.importoRapportini.setTag(position);
            }
            else{
                holder.linearRapportini.setVisibility(View.GONE);
            }

			System.out.println("EConTab: PreventivoDettaglioAdapter personalizzaView val.getAsString(PreventiviDettaglio.CODICE_ARTICOLO) " + val.getAsString(PreventiviDettaglio.CODICE_ARTICOLO));

			if (tipo.equals(PreventiviDettaglio.MANOPERA) ) {

                holder.codice.setText(getString(R.string.operatori) + " " + val.getAsInteger(PreventiviDettaglio.NUM_OPERATORI));

			} else {
				if (val.getAsString(PreventiviDettaglio.CODICE_ARTICOLO) != null
						&& val.getAsString(PreventiviDettaglio.CODICE_ARTICOLO).trim().length() > 0) {
					holder.codice.setText(val.getAsString(PreventiviDettaglio.CODICE_ARTICOLO));
				} else {
					holder.codice.setText(getString(R.string.codice));
				}

			}

			if (tipo.equals(PreventiviDettaglio.ALTRO)) {
				holder.descrizione.setText(val.getAsString(PreventiviDettaglio.NOTE));
			} else {

				if (val.containsKey("DESCRI_LINEA")){
					holder.descrizione.setText(val.getAsString(PreventiviDettaglio.DESCRIZIONE) + " "+val.getAsString("DESCRI_LINEA"));
				}
				else {
					holder.descrizione.setText(val.getAsString(PreventiviDettaglio.DESCRIZIONE));
				}
				if (tipo.equals(PreventiviDettaglio.PLACCHE) || tipo.equals(PreventiviDettaglio.PLACCHE_PREVENTIVO)) {
					holder.descrizione.setText(getString(R.string.placca) + " " + val.getAsString(PreventiviDettaglio.DESCRIZIONE) + " "
							+ val.getAsString(PlaccheModuli.NUMERO_MODULI) + " " + getString(R.string.moduli));
				}
			}

			if (isInModifica(position)) {
				holder.inModifica.setVisibility(View.VISIBLE);
				holder.viewDescrizione.setBackgroundResource(R.drawable.bg_econtab_titolo_rosso);

			} else {
				holder.inModifica.setVisibility(View.GONE);
				holder.viewDescrizione.setBackgroundResource(R.drawable.bg_econtab_titolo);
			}

			holder.viewDescrizione.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					mostraOpzioniRiga(position);
				}
			});

            holder.codice.setTextColor(Color.parseColor("#b9b9b9"));
            holder.codice.setClickable(false);
            holder.codice.setBackgroundResource(android.R.color.transparent);
			if (tipo.equals(PreventiviDettaglio.MATERIALE) || tipo.equals(PreventiviDettaglio.MATERIALE_PREVENTIVO)
					|| tipo.equals(PreventiviDettaglio.COLLEGAMENTI) || tipo.equals(PreventiviDettaglio.PLACCHE)
					|| tipo.equals(PreventiviDettaglio.PLACCHE_PREVENTIVO)) {
				holder.codice.setBackgroundResource(R.drawable.bg_bottone_grigio_arrotondato);
                holder.codice.setClickable(true);
				if (val.getAsString(PreventiviDettaglio.CODICE_ARTICOLO) != null
						&& val.getAsString(PreventiviDettaglio.CODICE_ARTICOLO).trim().length() > 0) {
					holder.codice.setTextColor(Color.parseColor("#0099cc"));
				}
				holder.codice.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						apriPopupCodice(position);

					}
				});

			}

            if (tipo.equals(PreventiviDettaglio.MANOPERA)){
                holder.codice.setBackgroundResource(R.drawable.bg_bottone_grigio_arrotondato);
                holder.codice.setClickable(true);

                holder.codice.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        apriPopupModifica(position,PreventiviDettaglio.NUM_OPERATORI);

                    }
                });
            }


			holder.prezzo.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					apriPopupModifica(position, PreventiviDettaglio.PREZZO);
				}
			});
			holder.qta.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					apriPopupModifica(position, PreventiviDettaglio.QUANTITA);
				}
			});

			if (tipo.equals(PreventiviDettaglio.MATERIALE) || tipo.equals(PreventiviDettaglio.PLACCHE)
					|| tipo.equals(PreventiviDettaglio.COLLEGAMENTI) || fragmnent.isModificheBloccate()) {
				holder.buttonPiu.setVisibility(View.INVISIBLE);
				holder.buttonMeno.setVisibility(View.INVISIBLE);
				// holder.qta.setBackgroundResource(android.R.color.transparent);

			} else {
				holder.buttonPiu.setVisibility(View.VISIBLE);
				holder.buttonMeno.setVisibility(View.VISIBLE);
				// holder.qta.setBackgroundResource(R.drawable.bg_econtab_edit);

				holder.buttonPiu.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						aggiornaQta(position, 1);
					}
				});

				holder.buttonMeno.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						aggiornaQta(position, -1);
					}
				});
			}

			holder.descrizione.setTag(position);

			holder.qta.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.QUANTITA)));
			holder.qta.setTag(position);

			String qtaUdm = val.getAsString(PreventiviDettaglio.UNITA_MISURA);
			if (qtaUdm != null && qtaUdm.trim().length() > 0) {
				holder.udm.setText(getString(R.string.qta).toUpperCase(Locale.getDefault()) + " " + qtaUdm.trim());
			} else {
				holder.udm.setText(getString(R.string.qta).toUpperCase(Locale.getDefault()));
			}
			holder.udm.setTag(position);

			holder.przAcquisto.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.PREZZO_ACQ), 2));
			holder.przAcquisto.setTag(position);

			holder.ricarico.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.RICARICO)));
			holder.ricarico.setTag(position);

			holder.przListino.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.PREZZO_VEN), 2));
			holder.przListino.setTag(position);

			holder.sconto.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.SCONTO)));
			holder.sconto.setTag(position);

			holder.prezzo.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.PREZZO), 2));
			holder.prezzo.setTag(position);

			float prezzo = val.getAsFloat(PreventiviDettaglio.PREZZO);
			float przAcq = val.getAsFloat(PreventiviDettaglio.PREZZO_ACQ);
			if (prezzo < przAcq) {
				holder.prezzo.setBackgroundColor(Color.parseColor("#ff4444"));
			} else {
				holder.prezzo.setBackgroundColor(Color.TRANSPARENT);
			}

			holder.importo.setText(Utility.formatNumero(tabDett.getImportoRiga(val), 2));
			holder.importo.setTag(position);

		}

		System.out.println("EConTab: PreventivoDettaglioAdapter personalizzaView EXIT");

		super.personalizzaView(position, viewholder);
	}

    private void _mostraRapportiniRiga(int idOrdine, int idManodopera) {
        DbInterno db = new DbInterno(context);
        Join j0 = new Join(Rapportini.NOME_TABELLA,RapportiniDettaglio.NOME_TABELLA);
        j0.addCampiDiJoin(Rapportini.ID_RAPPORTINO,RapportiniDettaglio.ID_RAPPORTINO);

        String SQL = "Select distinct "+Rapportini.NOME_TABELLA+".* from "+ Rapportini.NOME_TABELLA+ j0.getSQLJoin()+" where "+Rapportini.ID_ORDINE+"="+idOrdine+" and "+RapportiniDettaglio.ID_MANODOPERA+"="+idManodopera;
        final ArrayList<Object> rapp = db.eseguiSelect(SQL,null);

        db.close();
        String[] rappSel = new String[rapp.size()];
        for (int i=0;i<rapp.size();i++){
            ContentValues curr = (ContentValues)rapp.get(i);
            rappSel[i] = getString(R.string.rapportino_del)+" "+Utility.numberToData(curr.getAsLong(Rapportini.DATA_RAPPORTINO));
        }

        Utility.mostraSelezioneDialog(getString(R.string.rapportini),rappSel,context,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                Intent intent = new Intent(context,RapportinoDettaglioModActivity.class);
                intent.putExtra("ID",((ContentValues)rapp.get(which)).getAsInteger(Rapportini.ID_RAPPORTINO));
                context.startActivity(intent);
            }
        });
    }

    protected void mostraOpzioniRiga(final int position) {
		// TODO Auto-generated method stub
		ContentValues val = (ContentValues) dati.get(position);
		final String tipo = val.getAsString(PreventiviDettaglio.TIPO);
		final int idRiga = val.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO);
		String[] items = null;
		if (tipo.equals(PreventiviDettaglio.MATERIALE) || tipo.equals(PreventiviDettaglio.PLACCHE)
				|| tipo.equals(PreventiviDettaglio.COLLEGAMENTI)) {
			items = new String[2];
			items[0] = getString(R.string.modifica);
			items[1] = getString(R.string.modifica_codice_articolo);

		} else {
			if (tipo.equals(PreventiviDettaglio.MATERIALE_PREVENTIVO) || tipo.equals(PreventiviDettaglio.PLACCHE_PREVENTIVO)) {
				items = new String[3];
				items[0] = getString(R.string.modifica);
				items[1] = getString(R.string.modifica_codice_articolo);
				items[2] = getString(R.string.elimina);
			} else {
				items = new String[2];
				items[0] = getString(R.string.modifica);
				items[1] = getString(R.string.elimina);
			}

		}

		String titolo = "";
		if (tipo.equals(PreventiviDettaglio.ALTRO)) {
			titolo = val.getAsString(PreventiviDettaglio.NOTE);
		} else {
			titolo = val.getAsString(PreventiviDettaglio.DESCRIZIONE);
		}
		Utility.mostraSelezioneDialog(titolo, items, context, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if (which == 0) {
					apriPopupModifica(position, "");
				}

				if (which == 1) {
					if (tipo.equals(PreventiviDettaglio.MATERIALE_PREVENTIVO) || tipo.equals(PreventiviDettaglio.MATERIALE)
							|| tipo.equals(PreventiviDettaglio.PLACCHE) || tipo.equals(PreventiviDettaglio.PLACCHE_PREVENTIVO)
							|| tipo.equals(PreventiviDettaglio.COLLEGAMENTI)) {
						apriPopupCodice(position);
					} else {
						if (fragmnent.isModificheBloccate()) {
							Utility.mostraDialog(getString(R.string.attenzione), getString(R.string.modifiche_non_permesse), context, "OK");
						} else {
							Utility.mostraConfermaCancellazioneDialog(context, new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									if (which == DialogInterface.BUTTON_POSITIVE) {
										cancellaRiga(idRiga, position);
									}
								}
							});
						}
					}

				}

				if (which == 2) {
					if (fragmnent.isModificheBloccate()) {
						Utility.mostraDialog(getString(R.string.attenzione), getString(R.string.modifiche_non_permesse), context, "OK");
					} else {
						Utility.mostraConfermaCancellazioneDialog(context, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								if (which == DialogInterface.BUTTON_POSITIVE) {
									cancellaRiga(idRiga, position);
								}
							}
						});
					}

				}
			}
		});
	}

	protected void apriPopupModifica(int position, String campo) {
		// TODO Auto-generated method stub
		ContentValues val = (ContentValues) dati.get(position);
		PopupModificaRigaPreventivo popup = new PopupModificaRigaPreventivo(this, context, val, position);
		popup.impostaFocus(campo);
		popup.setModificheBloccate(fragmnent.isModificheBloccate());
		popup.apriPopup();
	}

	protected void apriPopupCodice(int position) {
		// TODO Auto-generated method stub
		ContentValues val = (ContentValues) dati.get(position);
		PopupAssociazioneCodiceListino popup = new PopupAssociazioneCodiceListino(this, context, val, position);
		popup.setModificheBloccate(fragmnent.isModificheBloccate());
		popup.apriPopup();
	}

	@Override
	protected View impostaLayout(int position, LayoutInflater infalInflater) {
		// TODO Auto-generated method stub

		if (getFragmnent().isRigaLocale(position)) {
			return infalInflater.inflate(R.layout.list_item_locale_preventivo_dettaglio, null);
		} else {
			return super.impostaLayout(position, infalInflater);
		}

	}

	@Override
	public int getViewTypeCount() {
		// TODO Auto-generated method stub
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		// TODO Auto-generated method stub
		if (getFragmnent().isRigaLocale(position)) {
			return 0;
		}
		return 1;
	}

	public void setInModifica(int position) {
		ContentValues val = (ContentValues) dati.get(position);
		val.put("MOD", "SI");
		notifyDataSetChanged();
		getFragmnent().setInModifica();
	}

	private boolean isInModifica(int position) {
		ContentValues val = (ContentValues) dati.get(position);
		return val.containsKey("MOD");
	}

	public HashMap<Integer, ContentValues> getRigheInModifica() {
		HashMap<Integer, ContentValues> righeMod = new HashMap<Integer, ContentValues>();
		for (int i = 0; i < dati.size(); i++) {
			if (isInModifica(i)) {
				ContentValues valCurr = (ContentValues) dati.get(i);
				righeMod.put(valCurr.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO), new ContentValues(valCurr));
			}
		}
		return righeMod;
	}

	public void salvaModifiche() {
		// TODO Auto-generated method stub

	}

	public void cancellaRiga(int idRiga, int position) {
		DbInterno db = new DbInterno(context);
		PreventiviDettaglio tabPreventivi = new PreventiviDettaglio();
		ContentValues valWhere = new ContentValues();
		valWhere.put(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO, idRiga);
		tabPreventivi.cancellaRecord(db, valWhere);
		db.close();
		dati.remove(position);
		notifyDataSetChanged();
        getFragmnent().ricalcolaTotali();
	}

	public PreventivoDettaglioFragment getFragmnent() {
		return fragmnent;
	}

	public void setFragmnent(PreventivoDettaglioFragment fragmnent) {
		this.fragmnent = fragmnent;
	}

	@Override
	public boolean isEnabled(int position) {
		// TODO Auto-generated method stub
		if (getFragmnent().isRigaLocale(position)) {
			return false;
		}
		return super.isEnabled(position);
	}

	/**
	 * aggiorno eventuali righe con lo stesso idcomponente e stessa linea
	 * 
	 * @param valModificato
     */
	public void aggiornaCodiciComponentiUguali(ContentValues valModificato) {
		// TODO Auto-generated method stub
		int idElemento = valModificato.getAsInteger(PreventiviDettaglio.ID_ELEMENTO);
		int idComponente = valModificato.getAsInteger(PreventiviDettaglio.ID_COMPONENTE);
		int idPlaccaModuli = valModificato.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI);
		int idLinea = valModificato.getAsInteger(Linee.ID_LINEA);

		if (idLinea != 0) {
			for (int i = 0; i < dati.size(); i++) {
				if (!fragmnent.isRigaLocale(i)) {
					ContentValues curr = (ContentValues) dati.get(i);
					int idElemCurr = curr.getAsInteger(PreventiviDettaglio.ID_ELEMENTO);
					int idCompCurr = curr.getAsInteger(PreventiviDettaglio.ID_COMPONENTE);
					int idPlaccaModuliCurr = curr.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI);
					if (curr.containsKey(Linee.ID_LINEA)) {
						int idLineaCurr = curr.getAsInteger(Linee.ID_LINEA);
						String codiceCurr = curr.getAsString(PreventiviDettaglio.CODICE_ARTICOLO);
						if (idLineaCurr == idLinea && idCompCurr == idComponente && idElemento == idElemCurr
								&& idPlaccaModuli == idPlaccaModuliCurr) {
							if (codiceCurr == null || codiceCurr.equals("")) {
								curr.put(PreventiviDettaglio.CODICE_ARTICOLO,
										valModificato.getAsString(PreventiviDettaglio.CODICE_ARTICOLO));
								curr.put(PreventiviDettaglio.PREZZO_ACQ, valModificato.getAsFloat(PreventiviDettaglio.PREZZO_ACQ));
								curr.put(PreventiviDettaglio.PREZZO_VEN, valModificato.getAsFloat(PreventiviDettaglio.PREZZO_VEN));
								curr.put(PreventiviDettaglio.SCONTO, valModificato.getAsFloat(PreventiviDettaglio.SCONTO));
								curr.put(PreventiviDettaglio.PREZZO, valModificato.getAsFloat(PreventiviDettaglio.PREZZO));
								// imposto in modifica
								curr.put("MOD", "SI");
							}
						}
					}
				}

			}
		}
	}
}
