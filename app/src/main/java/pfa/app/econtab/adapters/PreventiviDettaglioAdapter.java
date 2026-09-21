package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import pfa.app.econtab.db.table.UnitaMisura;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.fragments.PreventivoDettaglioFragment;
import pfa.app.econtab.utils.FaIcone;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.PopupAssociazioneCodiceListino;
import pfa.app.econtab.views.PopupModificaRigaPreventivo;

/**
 * Righe di preventivo/ordine come tabella standard (DettaglioTabellaAdapter): qui ci sono solo le colonne,
 * il contenuto delle celle e le azioni di riga (opzioni, popup, elimina).
 */
public class PreventiviDettaglioAdapter extends DettaglioTabellaAdapter {

	private static final String COL_TIPO = "tipo";
	private static final String COL_DESCRIZIONE = "descrizione";
	private static final String COL_CODICE = "codice";
	private static final String COL_PRZ_ACQ = "prz_acq";
	private static final String COL_PRZ_LIS = "prz_lis";
	private static final String COL_QTA = "qta";
	private static final String COL_UDM = "udm";
	private static final String COL_PREZZO = "prezzo";
	private static final String COL_IMPORTO = "importo";
	private static final String COL_RAPPORTINI = "rapportini";
	private static final String COL_ELIMINA = "elimina";

	private PreventivoDettaglioFragment fragmnent = null;
	private PreventiviDettaglio tabDett = null;
	private List<ColonnaDettaglio> colonne = null;

	/** Cella della quantita': [-] valore + unita' di misura [+]. */
	private static class QtaCella extends LinearLayout {
		final TextView meno;
		final TextView valore;
		final TextView piu;

		QtaCella(Context context) {
			super(context);
			setOrientation(HORIZONTAL);
			setGravity(Gravity.CENTER);
			meno = new TextView(context);
			piu = new TextView(context);
			valore = new TextView(context);
			LinearLayout centro = new LinearLayout(context);
			centro.setOrientation(VERTICAL);
			centro.setGravity(Gravity.CENTER);
			valore.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			valore.setTextColor(Color.parseColor("#212121"));
			centro.addView(valore);
			float d = context.getResources().getDisplayMetrics().density;
			int lato = (int) (40 * d);
			addView(meno, new LayoutParams(lato, lato));
			addView(centro, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
			addView(piu, new LayoutParams(lato, lato));
			FaIcone.applica(meno, FaIcone.MENO, null);
			FaIcone.applica(piu, FaIcone.PIU, null);
			meno.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			piu.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			meno.setBackgroundResource(android.R.drawable.list_selector_background);
			piu.setBackgroundResource(android.R.drawable.list_selector_background);
		}
	}

	public PreventiviDettaglioAdapter(Context context, ArrayList<Object> dati) {
		super(context, dati);
		tabDett = new PreventiviDettaglio();
	}

	@Override
	protected List<ColonnaDettaglio> getColonne() {
		if (colonne == null) {
			colonne = new ArrayList<ColonnaDettaglio>();
			colonne.add(ColonnaDettaglio.icona(COL_TIPO, "", 44));
			colonne.add(ColonnaDettaglio.testo(COL_DESCRIZIONE, getString(R.string.descrizione), 1f));
			colonne.add(ColonnaDettaglio.testoFisso(COL_CODICE, getString(R.string.codice), 110, Gravity.START));
			colonne.add(ColonnaDettaglio.testoFisso(COL_PRZ_ACQ, senzaEuro(R.string.prezzo_acquisto), 84, Gravity.END));
			colonne.add(ColonnaDettaglio.testoFisso(COL_PRZ_LIS, senzaEuro(R.string.prezzo_listino), 84, Gravity.END));
			colonne.add(ColonnaDettaglio.custom(COL_QTA, getString(R.string.qta), 130));
			colonne.add(ColonnaDettaglio.testoFisso(COL_UDM, getString(R.string.um_breve), 56, Gravity.CENTER));
			colonne.add(ColonnaDettaglio.testoFisso(COL_PREZZO, senzaEuro(R.string.prezzo), 76, Gravity.END));
			colonne.add(ColonnaDettaglio.testoFisso(COL_IMPORTO, senzaEuro(R.string.importo), 84, Gravity.END));
			if (fragmnent != null && Preventivi.TIPO_ORDINE.equals(fragmnent.getTipoPreventivoOrdine())) {
				colonne.add(ColonnaDettaglio.testoFisso(COL_RAPPORTINI, getString(R.string.rapportini), 100, Gravity.END));
			}
			colonne.add(ColonnaDettaglio.icona(COL_ELIMINA, "", 44));
		}
		return colonne;
	}

	/** Titoli colonne senza il simbolo dell'euro (le colonne sono tutte in euro): la testata resta su una riga. */
	private String senzaEuro(int idStringa) {
		return getString(idStringa).replace("€", "").trim();
	}

	@Override
	protected View creaCellaCustom(ColonnaDettaglio colonna, ViewGroup parent) {
		return new QtaCella(context);
	}

	@Override
	protected boolean isRigaGruppo(int position) {
		return getFragmnent().isRigaLocale(position);
	}

	@Override
	protected void bindGruppo(int position, TextView sopra, TextView titolo, TextView destra) {
		ContentValues val = (ContentValues) dati.get(position);
		titolo.setText(val.getAsString(Locali.NOME));
		String tipo = val.getAsString(PreventiviDettaglio.TIPO);
		boolean conDettagli = !(tipo.equals(PreventiviDettaglio.MANOPERA) || tipo.equals(PreventiviDettaglio.COLLEGAMENTI)
				|| tipo.equals(PreventiviDettaglio.ALTRO));
		String area = conDettagli ? val.getAsString("nomeArea") : null;
		String linea = conDettagli ? val.getAsString(Linee.NOME_LINEA) : null;
		String sigla = conDettagli ? val.getAsString(Costruttori.SIGLA_METEL) : null;
		sopra.setText(area);
		sopra.setVisibility(area != null && area.length() > 0 ? View.VISIBLE : View.GONE);
		boolean haLinea = linea != null && linea.length() > 0;
		destra.setText(haLinea ? getString(R.string.linea) + ": " + linea + (sigla != null && sigla.length() > 0 ? " - " + sigla : "") : "");
		destra.setVisibility(haLinea ? View.VISIBLE : View.GONE);
	}

	@Override
	protected void bindCella(final int position, ColonnaDettaglio colonna, View cella) {
		final ContentValues val = (ContentValues) dati.get(position);
		final String tipo = val.getAsString(PreventiviDettaglio.TIPO);
		boolean codiceModificabile = tipo.equals(PreventiviDettaglio.MATERIALE) || tipo.equals(PreventiviDettaglio.MATERIALE_PREVENTIVO)
				|| tipo.equals(PreventiviDettaglio.COLLEGAMENTI) || tipo.equals(PreventiviDettaglio.PLACCHE)
				|| tipo.equals(PreventiviDettaglio.PLACCHE_PREVENTIVO);

		switch (colonna.id) {
		case COL_TIPO:
			bindTipo((TextView) cella, tipo);
			break;

		case COL_DESCRIZIONE: {
			TextView tv = (TextView) cella;
			tv.setText(descrizioneRiga(val, tipo));
			// riga modificata e non ancora salvata: sfondo evidenziato leggero
			tv.setBackgroundColor(isInModifica(position) ? Color.parseColor("#FFEBEE") : Color.TRANSPARENT);
			tv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mostraOpzioniRiga(position);
				}
			});
			break;
		}

		case COL_CODICE: {
			TextView tv = (TextView) cella;
			tv.setOnClickListener(null);
			tv.setClickable(false);
			boolean haCodice = val.getAsString(PreventiviDettaglio.CODICE_ARTICOLO) != null
					&& val.getAsString(PreventiviDettaglio.CODICE_ARTICOLO).trim().length() > 0;
			if (tipo.equals(PreventiviDettaglio.ALTRO)) {
				tv.setText("");
			} else if (tipo.equals(PreventiviDettaglio.MANOPERA)) {
				tv.setText(getString(R.string.operatori) + " " + val.getAsInteger(PreventiviDettaglio.NUM_OPERATORI));
				tv.setTextColor(Color.parseColor("#212121"));
				tv.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						apriPopupModifica(position, PreventiviDettaglio.NUM_OPERATORI);
					}
				});
			} else {
				tv.setText(haCodice ? val.getAsString(PreventiviDettaglio.CODICE_ARTICOLO) : getString(R.string.codice));
				// placeholder in grigio (non e' un dato), codice valorizzato nel colore testo standard
				tv.setTextColor(Color.parseColor(haCodice ? "#212121" : "#B9B9B9"));
				if (codiceModificabile) {
					tv.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							apriPopupCodice(position);
						}
					});
				}
			}
			break;
		}

		case COL_PRZ_ACQ:
			((TextView) cella).setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.PREZZO_ACQ), 2));
			break;

		case COL_PRZ_LIS:
			((TextView) cella).setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.PREZZO_VEN), 2));
			break;

		case COL_QTA:
			bindQta((QtaCella) cella, position, val, tipo);
			break;

		case COL_UDM: {
			TextView tv = (TextView) cella;
			String codice = val.getAsString(PreventiviDettaglio.UNITA_MISURA);
			tv.setText(codice == null ? "" : codice.trim());
			// tooltip con la descrizione dell'unita' (dalla tabella scaricata dal server)
			String nome = nomiUnitaMisura().get(codice == null ? "" : codice.trim());
			androidx.appcompat.widget.TooltipCompat.setTooltipText(tv, nome != null ? nome : getString(R.string.unita_misura));
			tv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					scegliUnitaMisura(position);
				}
			});
			break;
		}

		case COL_PREZZO: {
			TextView tv = (TextView) cella;
			tv.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.PREZZO), 2));
			// prezzo sotto il costo di acquisto: evidenziato
			tv.setBackgroundColor(val.getAsFloat(PreventiviDettaglio.PREZZO) < val.getAsFloat(PreventiviDettaglio.PREZZO_ACQ)
					? Color.parseColor("#ff4444") : Color.TRANSPARENT);
			tv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					apriPopupModifica(position, PreventiviDettaglio.PREZZO);
				}
			});
			break;
		}

		case COL_IMPORTO:
			((TextView) cella).setText(Utility.formatNumero(tabDett.getImportoRiga(val), 2));
			break;

		case COL_RAPPORTINI: {
			TextView tv = (TextView) cella;
			tv.setOnClickListener(null);
			tv.setClickable(false);
			if (tipo.equals(PreventiviDettaglio.MANOPERA)) {
				tv.setText(Utility.formatNumero(val.getAsDouble("ORE_RAPP")) + " h  ·  " + Utility.formatNumero(val.getAsDouble("IMPORTO_RAPP"), 2));
				tv.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						_mostraRapportiniRiga(val.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO), val.getAsInteger(PreventiviDettaglio.ID_MANODOPERA));
					}
				});
			} else {
				tv.setText("");
			}
			break;
		}

		case COL_ELIMINA:
			impostaIcona(cella, FaIcone.ELIMINA, getString(R.string.elimina));
			cella.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					confermaEliminaRiga(position);
				}
			});
			break;
		}
	}

	private HashMap<String, String> nomiUm = null;

	/** Codice -> descrizione delle unita' di misura scaricate dal server (caricata una volta sola). */
	private HashMap<String, String> nomiUnitaMisura() {
		if (nomiUm == null) {
			nomiUm = new HashMap<String, String>();
			DbInterno db = new DbInterno(context);
			ArrayList<Object> udm = db.eseguiSelect(new UnitaMisura(), null, new String[] { UnitaMisura.UNITA_MISURA });
			db.close();
			for (Object o : udm) {
				ContentValues v = (ContentValues) o;
				nomiUm.put(v.getAsString(UnitaMisura.UNITA_MISURA), v.getAsString(UnitaMisura.NOME));
			}
		}
		return nomiUm;
	}

	/** Cambio dell'unita' di misura della riga: si sceglie tra quelle gestite dal server, poi la riga risulta "in modifica". */
	private void scegliUnitaMisura(final int position) {
		if (fragmnent.isModificheBloccate()) {
			Utility.mostraDialog(getString(R.string.attenzione), getString(R.string.modifiche_non_permesse), context, "OK");
			return;
		}
		DbInterno db = new DbInterno(context);
		final ArrayList<Object> udm = db.eseguiSelect(new UnitaMisura(), null, new String[] { UnitaMisura.UNITA_MISURA });
		db.close();
		if (udm.isEmpty()) {
			Utility.mostraDialog(getString(R.string.attenzione), getString(R.string.unita_misura) + ": nessuna unita' scaricata dal server", context, "OK");
			return;
		}
		String[] voci = new String[udm.size()];
		for (int i = 0; i < udm.size(); i++) {
			ContentValues v = (ContentValues) udm.get(i);
			voci[i] = v.getAsString(UnitaMisura.UNITA_MISURA) + " - " + v.getAsString(UnitaMisura.NOME);
		}
		Utility.mostraSelezioneDialog(getString(R.string.unita_misura), voci, context, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ContentValues val = (ContentValues) dati.get(position);
				val.put(PreventiviDettaglio.UNITA_MISURA, ((ContentValues) udm.get(which)).getAsString(UnitaMisura.UNITA_MISURA));
				setInModifica(position);
			}
		});
	}

	/** Icona della tipologia riga (Font Awesome, colore di default) con tooltip. */
	private void bindTipo(TextView cella, String tipo) {
		if (tipo.equals(PreventiviDettaglio.MANOPERA)) {
			impostaIcona(cella, FaIcone.MANODOPERA, getString(R.string.manodopera));
		} else if (tipo.equals(PreventiviDettaglio.COLLEGAMENTI)) {
			impostaIcona(cella, FaIcone.COLLEGAMENTI, getString(R.string.collegamenti));
		} else if (tipo.equals(PreventiviDettaglio.PLACCHE) || tipo.equals(PreventiviDettaglio.PLACCHE_PREVENTIVO)) {
			impostaIcona(cella, FaIcone.PLACCHE, getString(R.string.placche));
		} else if (tipo.equals(PreventiviDettaglio.ALTRO)) {
			impostaIcona(cella, FaIcone.NOTE, getString(R.string.note));
		} else {
			impostaIcona(cella, FaIcone.MATERIALE, getString(R.string.materiale));
		}
	}

	private String descrizioneRiga(ContentValues val, String tipo) {
		if (tipo.equals(PreventiviDettaglio.ALTRO)) {
			return val.getAsString(PreventiviDettaglio.NOTE);
		}
		if (tipo.equals(PreventiviDettaglio.PLACCHE) || tipo.equals(PreventiviDettaglio.PLACCHE_PREVENTIVO)) {
			return getString(R.string.placca) + " " + val.getAsString(PreventiviDettaglio.DESCRIZIONE) + " "
					+ val.getAsString(PlaccheModuli.NUMERO_MODULI) + " " + getString(R.string.moduli);
		}
		if (val.containsKey("DESCRI_LINEA")) {
			return val.getAsString(PreventiviDettaglio.DESCRIZIONE) + " " + val.getAsString("DESCRI_LINEA");
		}
		return val.getAsString(PreventiviDettaglio.DESCRIZIONE);
	}

	private void bindQta(QtaCella cella, final int position, ContentValues val, String tipo) {
		cella.valore.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.QUANTITA)));
		cella.valore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				apriPopupModifica(position, PreventiviDettaglio.QUANTITA);
			}
		});

		// quantita' delle righe derivate dal cantiere: non si incrementa a mano
		boolean stepper = !(tipo.equals(PreventiviDettaglio.MATERIALE) || tipo.equals(PreventiviDettaglio.PLACCHE)
				|| tipo.equals(PreventiviDettaglio.COLLEGAMENTI) || fragmnent.isModificheBloccate());
		cella.meno.setVisibility(stepper ? View.VISIBLE : View.INVISIBLE);
		cella.piu.setVisibility(stepper ? View.VISIBLE : View.INVISIBLE);
		if (stepper) {
			cella.piu.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					aggiornaQta(position, 1);
				}
			});
			cella.meno.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					aggiornaQta(position, -1);
				}
			});
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

	/**
	 * Cancellazione dal pulsante a fine riga. Vale per tutte le righe, a differenza del menu opzioni
	 * che non propone "Elimina" per le righe generate dal cantiere (materiale, placche, collegamenti):
	 * per queste chiede una conferma con un avviso, perche' il ricalcolo del preventivo le puo' rigenerare.
	 */
	protected void confermaEliminaRiga(final int position) {
		if (fragmnent.isModificheBloccate()) {
			Utility.mostraDialog(getString(R.string.attenzione), getString(R.string.modifiche_non_permesse), context, "OK");
			return;
		}

		ContentValues val = (ContentValues) dati.get(position);
		final int idRiga = val.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO);
		String tipo = val.getAsString(PreventiviDettaglio.TIPO);

		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					cancellaRiga(idRiga, position);
				}
			}
		};

		if (tipo.equals(PreventiviDettaglio.MATERIALE) || tipo.equals(PreventiviDettaglio.PLACCHE)
				|| tipo.equals(PreventiviDettaglio.COLLEGAMENTI)) {
			Utility.mostraConfermaDialog(getString(R.string.attenzione), getString(R.string.conferma_cancellazione_riga_derivata), context,
					getString(R.string.conferma), getString(R.string.annulla), listener);
		} else {
			Utility.mostraConfermaCancellazioneDialog(context, listener);
		}
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
