package pfa.app.econtab.views;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.BitmapDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import pfa.app.econtab.R;
import pfa.app.econtab.adapters.PreventiviDettaglioAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.AssCodiciLinee;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCodici;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Listini;
import pfa.app.econtab.db.table.Placche;
import pfa.app.econtab.db.table.PlaccheModuli;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.utils.EConTabAutoCompleteContentValue;
import pfa.app.econtab.utils.AsyncTaskExecutorService;
import pfa.app.econtab.utils.Utility;

public class PopupAssociazioneCodiceListino implements DialogInterface.OnClickListener, TextWatcher, OnItemClickListener {
	View vista = null;

	Context ctx = null;
	ContentValues val = null;
	BaseAdapter adapter = null;
	int position = 0;

	AutoCompleteTextView editCodice = null;
	EditText editDescrizione = null;
	EditText editPrzAcq = null;
	EditText editPrzLis = null;
	EditText editSconto = null;
	CheckBox checkAggiorna = null;
	EConTabSpinner spinnerFornitore = null;
	EConTabSpinner spinnerLinea = null;

	ArrayAdapter<Object> adapterCodici = null;

	ContentValues valComponente = null;
	ContentValues valElemento = null;
	ContentValues valPlacca = null;

	AlertDialog di = null;

	int gestitoALinea = 0;

	private boolean pulisciCodice = true;

	private boolean modificheBloccate = false;
	private int idLineaArticoloSeleizonato = 0;
	private String idLineaOriginale = "";

	public PopupAssociazioneCodiceListino(BaseAdapter adapter, Context ctx, ContentValues val, int position) {
		vista = View.inflate(ctx, R.layout.dialog_edit_codice_articolo, null);

		this.ctx = ctx;
		this.val = val;
		this.adapter = adapter;
		this.position = position;

		impostaCampi();

	}

	private void impostaCampi() {
		vista.findViewById(R.id.textView_linea_originale).setVisibility(View.GONE);
		// TODO Auto-generated method stub
		DbInterno db = new DbInterno(ctx);

		int idElemento = val.getAsInteger(PreventiviDettaglio.ID_ELEMENTO);
		int idComponente = val.getAsInteger(PreventiviDettaglio.ID_COMPONENTE);
		int idPlaccaModuli = val.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI);

		// nei materiali o � diverso da zero id_elemnto (elementi) o id_componente (componenti)
		if (idElemento != 0) {
			Elementi tabElem = new Elementi();
			ContentValues where = new ContentValues();
			where.put(Elementi.ID_ELEMENTO, val.getAsString(PreventiviDettaglio.ID_ELEMENTO));
			valElemento = db.getRecord(tabElem, where);
		}

		if (idComponente != 0) {
			Componenti tabComp = new Componenti();
			ContentValues where = new ContentValues();
			where.put(Componenti.ID_COMPONENTE, val.getAsString(PreventiviDettaglio.ID_COMPONENTE));
			valComponente = db.getRecord(tabComp, where);
		}

		if (idPlaccaModuli != 0) {
			PlaccheModuli tabPlacche = new PlaccheModuli();

			valPlacca = tabPlacche.getRecordPlacca(db, idPlaccaModuli);
			if (valPlacca!=null){
				val.put(PreventiviDettaglio.ID_LINEA,valPlacca.getAsInteger(Placche.ID_LINEA));
			}
		}

		db.close();
		if (valComponente != null || valElemento != null || valPlacca != null) {
			editCodice = (AutoCompleteTextView) vista.findViewById(R.id.editText_codice);
			editDescrizione = (EditText) vista.findViewById(R.id.editText_descrizione);
			editPrzAcq = (EditText) vista.findViewById(R.id.editText_przAcq);
			editPrzLis = (EditText) vista.findViewById(R.id.editText_przLis);
			editSconto = (EditText) vista.findViewById(R.id.editText_sconto);

			editCodice.setSelectAllOnFocus(true);
			editDescrizione.setSelectAllOnFocus(true);
			editPrzAcq.setSelectAllOnFocus(true);
			editPrzLis.setSelectAllOnFocus(true);
			editSconto.setSelectAllOnFocus(true);

			checkAggiorna = (CheckBox) vista.findViewById(R.id.checkBoxAggiorna);
			spinnerFornitore = (EConTabSpinner) vista.findViewById(R.id.econtabSpinnerFornitore);
			spinnerLinea = (EConTabSpinner) vista.findViewById(R.id.econtabSpinnerLinea);

			// Se non ci sono i prezzi nella riga di preventivo imposto selezionato il checkbox di aggiornamento
			if (val.getAsFloat(PreventiviDettaglio.PREZZO) == 0) {
				checkAggiorna.setChecked(true);
			}

			if (valPlacca!=null){
				spinnerLinea.setValue("" + valPlacca.getAsInteger(Placche.ID_LINEA));
				idLineaOriginale = "" + valPlacca.getAsInteger(Linee.ID_LINEA);
			}
			else{
				spinnerLinea.setValue("" + val.getAsInteger(Linee.ID_LINEA));
				//modifica del 13/01/2018 : si può cambiare linea sempre però do un avviso quando la cambio . Se cambio linea inserisco un record in ass_codici_linea con la linea passata
				idLineaOriginale = "" + val.getAsInteger(Linee.ID_LINEA);

			}
			if (idLineaOriginale.equals("0")){
				idLineaOriginale = "";
			}
			// verifico se c'� gi� il codice associato alla riga di preventivo
			String codiceAssociato = val.getAsString(PreventiviDettaglio.CODICE_ARTICOLO);

			if (codiceAssociato != null && codiceAssociato.trim().length() > 0) {
				db = new DbInterno(ctx);
				ContentValues whereLis = new ContentValues();

				whereLis.put(Listini.CODICE_ARTICOLO, codiceAssociato);
				ContentValues valLis = db.getRecord(new Listini(), whereLis);

				db.close();

				if (valLis != null) {
					editCodice.setText(codiceAssociato);
					editDescrizione.setText(valLis.getAsString(Listini.DESCRIZIONE));
					editPrzAcq.setText(Utility.formatNumero(valLis.getAsFloat(Listini.PRZ_ULTIMO_ACQUISTO), 2));
					editPrzLis.setText(Utility.formatNumero(valLis.getAsFloat(Listini.PRZ_LISTINO), 2));
					editSconto.setText(Utility.formatNumero(valLis.getAsFloat(Listini.SCONTO), 2));
					spinnerLinea.setValue("" + valLis.getAsInteger(Listini.ID_LINEA));
					spinnerFornitore.setValue("" + valLis.getAsInteger(Listini.ID_COSTRUTTORE));
					if (!(""+valLis.getAsInteger(Listini.ID_LINEA)).equals(idLineaOriginale)){
						ContentValues whereLinea = new ContentValues();
						whereLinea.put(Linee.ID_LINEA,idLineaOriginale);
						ContentValues recLineaOrig = db.getRecord(new Linee(),whereLinea);
						vista.findViewById(R.id.textView_linea_originale).setVisibility(View.VISIBLE);
						if (recLineaOrig!=null){
							((TextView)vista.findViewById(R.id.textView_linea_originale)).setText("Linea ordine: " + recLineaOrig.getAsString(Linee.NOME_LINEA));
						}
						else {
							((TextView)vista.findViewById(R.id.textView_linea_originale)).setText("Linea ordine: " + idLineaOriginale);
						}
					}
				}
			}

			spinnerFornitore.setTabella(new Costruttori());
			spinnerFornitore.addTextChangeListener(this);

			spinnerLinea.setTabella(new Linee());
			spinnerLinea.addTextChangeListener(this);

			if (valComponente != null) {
				gestitoALinea = valComponente.getAsInteger(Componenti.LINEA_SN);
			}

			// Le placche sono sempre gestite a linea
			if (valPlacca != null) {
				gestitoALinea = 1;
			}

			if (gestitoALinea == 1) {
				vista.findViewById(R.id.linearFornitore).setVisibility(View.GONE);
				// disbailito lo spinner della linea se impostata sul locale
				/*if (!spinnerLinea.getValue().equals("")) {
					spinnerLinea.setEnabled(false);
					spinnerLinea.setMessaggioDisabilitato(ctx.getString(R.string.messaggio_linea_non_modificabile));
				}*/
			} else {
				vista.findViewById(R.id.linearLinea).setVisibility(View.GONE);
			}

			caricaCodiciArticoli();

			editCodice.setOnItemClickListener(this);
		}

	}

	private void caricaCodiciArticoli() {
		// new class for asynchronous task
		AsyncTaskExecutorService task = new AsyncTaskExecutorService() {
			private String spinnerLineaValue = "";
			private String spinnerFornitoreValue = "";
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				editCodice.setEnabled(false);
				spinnerLineaValue = spinnerLinea.getValue();
				spinnerFornitoreValue = spinnerFornitore.getValue();
			}

			@Override
			protected Object doInBackground(Object o) {
				String SQL = "";
				String SQL_NO_LINEA = "";
				DbInterno db = new DbInterno(ctx);
				if (gestitoALinea == 1) {
					if (!spinnerLineaValue.equals("")) {
						SQL = "Select " + Listini.NOME_TABELLA + ".*," + Listini.CODICE_ARTICOLO + " ||' - '|| " + Listini.DESCRIZIONE
								+ " as codicedescri  from " + Listini.NOME_TABELLA + " where " + Listini.ID_LINEA + "=" + spinnerLineaValue;
					} else {
						SQL = "Select " + Listini.NOME_TABELLA + ".*," + Listini.CODICE_ARTICOLO + " ||' - '|| " + Listini.DESCRIZIONE
								+ " as codicedescri from " + Listini.NOME_TABELLA + " where " + Listini.ID_LINEA + "<>0";
					}
					int idFornitoreLinea = 0;
					Linee tabLinee = new Linee();
					ContentValues whereLinea = new ContentValues();
					whereLinea.put(Linee.ID_LINEA,spinnerLineaValue);

					try{
						ContentValues valLinea = db.getRecord(tabLinee,whereLinea);
						if (valLinea!=null){
							idFornitoreLinea = valLinea.getAsInteger(Linee.ID_COSTRUTTORE);
						}
					}
					catch (Exception e){

					}

					SQL_NO_LINEA = "Select " + Listini.NOME_TABELLA + ".*," + Listini.CODICE_ARTICOLO + " ||' - '|| " + Listini.DESCRIZIONE
							+ " as codicedescri from " + Listini.NOME_TABELLA + " where " + Listini.ID_LINEA + "=0 and "+Listini.ID_COSTRUTTORE+"="+idFornitoreLinea;
				} else {
					if (!spinnerFornitoreValue.equals("")) {
						SQL = "Select " + Listini.NOME_TABELLA + ".*," + Listini.CODICE_ARTICOLO + " ||' - '|| " + Listini.DESCRIZIONE
								+ " as codicedescri from " + Listini.NOME_TABELLA + " where " + Listini.ID_LINEA + "=0" + " and "
								+ Listini.ID_COSTRUTTORE + "=" + spinnerFornitoreValue;
					} else {
						SQL = "Select " + Listini.NOME_TABELLA + ".*," + Listini.CODICE_ARTICOLO + " ||' - '|| " + Listini.DESCRIZIONE
								+ " as codicedescri from " + Listini.NOME_TABELLA + " where " + Listini.ID_LINEA + "=0";

					}
				}

				ArrayList<Object> lista = db.eseguiSelect(SQL, null);
				if (!SQL_NO_LINEA.equals("")) {
					ArrayList<Object> listaNoLinea = db.eseguiSelect(SQL_NO_LINEA, null);
					if (listaNoLinea.size() > 0) {
						for (int i = 0; i < listaNoLinea.size(); i++) {
							ContentValues curr = (ContentValues) listaNoLinea.get(i);
							String codicedescri = curr.getAsString("codicedescri");
							codicedescri = "- " + ctx.getString(R.string.nessuna_linea) + " -\r\n" + codicedescri;
							curr.put("codicedescri", codicedescri);

						}
						lista.addAll(listaNoLinea);
					}
				}
				db.close();
				return lista;
			}

			@Override
			protected void onPostExecute(Object o) {
				adapterCodici = Utility.getArrayAdapterTabella(ctx, (ArrayList)o, "codicedescri");
				editCodice.setAdapter(adapterCodici);
				editCodice.setEnabled(true);
			}
		};
		task.execute();
		/*
        AsyncTask task = new AsyncTask() {

			private String spinnerLineaValue = "";
			private String spinnerFornitoreValue = "";
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                editCodice.setEnabled(false);
				spinnerLineaValue = spinnerLinea.getValue();
				spinnerFornitoreValue = spinnerFornitore.getValue();
            }

            @Override
            protected Object doInBackground(Object[] objects) {
                String SQL = "";
                String SQL_NO_LINEA = "";
                DbInterno db = new DbInterno(ctx);
                if (gestitoALinea == 1) {
                    if (!spinnerLineaValue.equals("")) {
                        SQL = "Select " + Listini.NOME_TABELLA + ".*," + Listini.CODICE_ARTICOLO + " ||' - '|| " + Listini.DESCRIZIONE
                                + " as codicedescri  from " + Listini.NOME_TABELLA + " where " + Listini.ID_LINEA + "=" + spinnerLineaValue;
                    } else {
                        SQL = "Select " + Listini.NOME_TABELLA + ".*," + Listini.CODICE_ARTICOLO + " ||' - '|| " + Listini.DESCRIZIONE
                                + " as codicedescri from " + Listini.NOME_TABELLA + " where " + Listini.ID_LINEA + "<>0";
                    }
                    int idFornitoreLinea = 0;
                    Linee tabLinee = new Linee();
                    ContentValues whereLinea = new ContentValues();
                    whereLinea.put(Linee.ID_LINEA,spinnerLineaValue);

                    try{
                        ContentValues valLinea = db.getRecord(tabLinee,whereLinea);
                        if (valLinea!=null){
                            idFornitoreLinea = valLinea.getAsInteger(Linee.ID_COSTRUTTORE);
                        }
                    }
                    catch (Exception e){

                    }


                    SQL_NO_LINEA = "Select " + Listini.NOME_TABELLA + ".*," + Listini.CODICE_ARTICOLO + " ||' - '|| " + Listini.DESCRIZIONE
                            + " as codicedescri from " + Listini.NOME_TABELLA + " where " + Listini.ID_LINEA + "=0 and "+Listini.ID_COSTRUTTORE+"="+idFornitoreLinea;
                } else {
                    if (!spinnerFornitoreValue.equals("")) {
                        SQL = "Select " + Listini.NOME_TABELLA + ".*," + Listini.CODICE_ARTICOLO + " ||' - '|| " + Listini.DESCRIZIONE
                                + " as codicedescri from " + Listini.NOME_TABELLA + " where " + Listini.ID_LINEA + "=0" + " and "
                                + Listini.ID_COSTRUTTORE + "=" + spinnerFornitoreValue;
                    } else {
                        SQL = "Select " + Listini.NOME_TABELLA + ".*," + Listini.CODICE_ARTICOLO + " ||' - '|| " + Listini.DESCRIZIONE
                                + " as codicedescri from " + Listini.NOME_TABELLA + " where " + Listini.ID_LINEA + "=0";

                    }

                }

                ArrayList<Object> lista = db.eseguiSelect(SQL, null);
                if (!SQL_NO_LINEA.equals("")) {
                    ArrayList<Object> listaNoLinea = db.eseguiSelect(SQL_NO_LINEA, null);
                    if (listaNoLinea.size() > 0) {
                        for (int i = 0; i < listaNoLinea.size(); i++) {
                            ContentValues curr = (ContentValues) listaNoLinea.get(i);
                            String codicedescri = curr.getAsString("codicedescri");
                            codicedescri = "- " + ctx.getString(R.string.nessuna_linea) + " -\r\n" + codicedescri;
                            curr.put("codicedescri", codicedescri);

                        }
                        lista.addAll(listaNoLinea);
                    }
                }
                db.close();
                return lista;
            }


            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                adapterCodici = Utility.getArrayAdapterTabella(ctx, (ArrayList)o, "codicedescri");
                editCodice.setAdapter(adapterCodici);
                editCodice.setEnabled(true);
            }
        };
        task.execute();
		*/
	}

	public String getTipoRiga() {
		return val.getAsString(PreventiviDettaglio.TIPO);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		// TODO Auto-generated method stub
		if (which == DialogInterface.BUTTON_POSITIVE) {

			// inserisco il codice articolo se non esiste nella tabella listini
			DbInterno db = new DbInterno(ctx);
			ContentValues where = new ContentValues();

            String codiceArticolo =editCodice.getText().toString().toUpperCase(Locale.getDefault());

			where.put(Listini.CODICE_ARTICOLO, codiceArticolo.trim());

			ContentValues recordCheck = db.getRecord(new Listini(), where);
			if (recordCheck == null && codiceArticolo.trim().length() > 0) {
				// inserisco il record
				Listini tabListini = new Listini();
				ContentValues valInsert = tabListini.getValoriLogInserimento(db);

				valInsert.put(Listini.CODICE_ARTICOLO, codiceArticolo.trim());
				valInsert.put(Listini.DESCRIZIONE, editDescrizione.getText().toString().trim());
				valInsert.put(Listini.PRZ_ULTIMO_ACQUISTO, Utility.formatNumeroDB(editPrzAcq.getText().toString()));
				valInsert.put(Listini.PRZ_LISTINO, Utility.formatNumeroDB(editPrzLis.getText().toString()));
				valInsert.put(Listini.SCONTO, Utility.formatNumeroDB(editSconto.getText().toString()));
				if (gestitoALinea == 1) {
					valInsert.put(Listini.ID_LINEA, spinnerLinea.getValue());
					valInsert.put(Listini.ID_COSTRUTTORE, spinnerLinea.getRecordSelezionato().getAsInteger(Linee.ID_COSTRUTTORE));
				} else {
					valInsert.put(Listini.ID_COSTRUTTORE, spinnerFornitore.getValue());
				}
				tabListini.inserisciRecord(db, valInsert);

				Toast.makeText(ctx, "Aggiunto il codice " + codiceArticolo + " al listino", Toast.LENGTH_SHORT).show();
			}

			// aggiorno la descrizione se cambiata
			if (recordCheck != null && codiceArticolo.trim().length() > 0) {
				Listini tabListini = new Listini();
				ContentValues valUpdate = tabListini.getValoriLogModifica(db);
				valUpdate.put(Listini.DESCRIZIONE, editDescrizione.getText().toString().trim());
				valUpdate.put(Listini.PRZ_ULTIMO_ACQUISTO, Utility.formatNumeroDB(editPrzAcq.getText().toString()));
				valUpdate.put(Listini.PRZ_LISTINO, Utility.formatNumeroDB(editPrzLis.getText().toString()));
				valUpdate.put(Listini.SCONTO, Utility.formatNumeroDB(editSconto.getText().toString()));
				if (gestitoALinea == 1) {
					valUpdate.put(Listini.ID_LINEA, spinnerLinea.getValue());
					valUpdate.put(Listini.ID_COSTRUTTORE, spinnerLinea.getRecordSelezionato().getAsInteger(Linee.ID_COSTRUTTORE));
				}

				ContentValues whereUpd = new ContentValues();

				whereUpd.put(Listini.CODICE_ARTICOLO, codiceArticolo.trim());

				tabListini.aggiornaRecord(db, valUpdate, whereUpd);

				Toast.makeText(ctx, "Aggiornato il codice " + codiceArticolo + " sul listino", Toast.LENGTH_SHORT).show();

			}

			// inserisco l'associazione nella tabella Elementi_codici se non esiste
			if (codiceArticolo.trim().length() > 0) {
				ContentValues whereAss = new ContentValues();
				whereAss.put(ElementiCodici.ID_ELEMENTO, val.getAsInteger(PreventiviDettaglio.ID_ELEMENTO));
				whereAss.put(ElementiCodici.ID_COMPONENTE, val.getAsInteger(PreventiviDettaglio.ID_COMPONENTE));
				whereAss.put(ElementiCodici.ID_PLACCA_MODULI, val.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI));
				whereAss.put(ElementiCodici.CODICE_ARTICOLO, codiceArticolo.trim());
				ElementiCodici tabAss = new ElementiCodici();
				ContentValues recCheckAss = db.getRecord(tabAss, whereAss);
				if (recCheckAss == null) {
					ContentValues recValAss = tabAss.getValoriLogInserimento(db);
					recValAss.putAll(whereAss);
					tabAss.inserisciRecord(db, recValAss);
				}

			}


			//se la linea selezionata è diversa da quella di partenza inserisco un record in ass_codici_linee per futuri utilizzi dello stesso elemento
			if (gestitoALinea == 1) {
				if (idLineaOriginale!=spinnerLinea.getValue()){
					ContentValues whereChk = new ContentValues();
					whereChk.put(AssCodiciLinee.CODICE_ARTICOLO, codiceArticolo.trim());
					whereChk.put(AssCodiciLinee.ID_LINEA, idLineaOriginale);

					AssCodiciLinee tabAss = new AssCodiciLinee();
					ContentValues recCheck = db.getRecord(tabAss, whereChk);
					if (recCheck==null){
						ContentValues recValAss = tabAss.getValoriLogInserimento(db);
						recValAss.putAll(whereChk);
						tabAss.inserisciRecord(db, recValAss);
					}
				}
			}
			db.close();

			val.put(PreventiviDettaglio.CODICE_ARTICOLO, codiceArticolo.trim());
			if (checkAggiorna.isChecked()) {
				// calcolo il prezzo di vendita da prezzo listino con sconto
				double przLis = Utility.formatNumeroDB(editPrzLis.getText().toString());
				double sconto = Utility.formatNumeroDB(editSconto.getText().toString());
				double przVen = przLis - (przLis * sconto) / 100;
				// aggiorno i prezzi sul preventivo
				val.put(PreventiviDettaglio.PREZZO_ACQ, Utility.formatNumeroDB(editPrzAcq.getText().toString()));
				val.put(PreventiviDettaglio.PREZZO_VEN, przLis);
				val.put(PreventiviDettaglio.SCONTO, sconto);
				val.put(PreventiviDettaglio.PREZZO, przVen);

			}
			((PreventiviDettaglioAdapter) adapter).setInModifica(position);

			// aggiorno eventuali righe con lo stesso idcomponente e stessa linea
			((PreventiviDettaglioAdapter) adapter).aggiornaCodiciComponentiUguali(val);

			adapter.notifyDataSetChanged();
			/*
			 * val.put(PreventiviDettaglio.DESCRIZIONE, editTitolo.getText().toString());
			 * 
			 * val.put(PreventiviDettaglio.NUM_OPERATORI,
			 * Utility.formatNumeroDB(editNumOperatori.getText().toString())); val.put(PreventiviDettaglio.QUANTITA,
			 * Utility.formatNumeroDB(editQta.getText().toString())); val.put(PreventiviDettaglio.PREZZO,
			 * Utility.formatNumeroDB(editPrezzo.getText().toString())); val.put(PreventiviDettaglio.PREZZO_ACQ,
			 * Utility.formatNumeroDB(editPrezzoAcq.getText().toString())); val.put(PreventiviDettaglio.PREZZO_VEN,
			 * Utility.formatNumeroDB(editPrezzoLis.getText().toString())); val.put(PreventiviDettaglio.RICARICO,
			 * Utility.formatNumeroDB(editRicarico.getText().toString())); val.put(PreventiviDettaglio.SCONTO,
			 * Utility.formatNumeroDB(editSconto.getText().toString())); ((PreventiviDettaglioAdapter)
			 * adapter).setInModifica(position); adapter.notifyDataSetChanged();
			 */
		}
	}

	public void apriPopup() {
		if (valComponente != null || valElemento != null || valPlacca != null) {
			if (valComponente != null) {
				di = Utility.mostraDialogPersonalizzato(
						val.getAsString(PreventiviDettaglio.DESCRIZIONE),
						new BitmapDrawable(ctx.getResources(), Utility.getIconaThumb(ctx, Componenti.PATH_ICONE,
								valComponente.getAsString(Componenti.ICONA))), ctx, vista, null, ctx.getString(R.string.conferma),
						ctx.getString(R.string.annulla), this);
			}
			if (valElemento != null) {
				di = Utility.mostraDialogPersonalizzato(
						val.getAsString(PreventiviDettaglio.DESCRIZIONE),
						new BitmapDrawable(ctx.getResources(), Utility.getIconaThumb(ctx, Elementi.PATH_ICONE,
								valElemento.getAsString(Elementi.ICONA))), ctx, vista, null, ctx.getString(R.string.conferma),
						ctx.getString(R.string.annulla), this);
			}
			if (valPlacca != null) {
				di = Utility.mostraDialogPersonalizzato(
						ctx.getString(R.string.placca) + " " + val.getAsString(PreventiviDettaglio.DESCRIZIONE) + " "
								+ val.getAsString(PlaccheModuli.NUMERO_MODULI) + " " + ctx.getString(R.string.moduli), null, ctx, vista,
						null, ctx.getString(R.string.conferma), ctx.getString(R.string.annulla), this);
			}

			controllaAbilitazioneTastoConferma();
		} else {
			Toast.makeText(ctx, ctx.getString(R.string.errore_elemento_non_trovato), Toast.LENGTH_SHORT).show();
		}

	}

	@Override
	public void afterTextChanged(Editable arg0) {
		// TODO Auto-generated method stub
		controllaAbilitazioneTastoConferma();
        caricaCodiciArticoli();
        if (pulisciCodice == true && idLineaArticoloSeleizonato != 0) {
            editCodice.setText("");
            editDescrizione.setText("");
        } else {
            pulisciCodice = true;
        }

        if (!idLineaOriginale.equals("") && !spinnerLinea.getValue().equals(idLineaOriginale)){
			Utility.mostraConfermaDialog(ctx.getString(R.string.attenzione), "Hai cambiato la linea per la ricerca del codice: se confermi, l'articolo che scegli verrà associato anche alla tua linea di partenza, in modo che nei prossimi ordini venga associato in automatico. Continuare?", ctx, ctx.getString(R.string.si), ctx.getString(R.string.no), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					if (i==DialogInterface.BUTTON_NEGATIVE){
						spinnerLinea.setValue(idLineaOriginale);
						spinnerLinea.setTabella(new Linee());
					}
				}
			});
		}



	}

	private void controllaAbilitazioneTastoConferma() {
		if (gestitoALinea == 1) {
			if (spinnerLinea.getValue().equals("")) {
				di.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
			} else {
				di.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
			}
		} else {
			if (spinnerFornitore.getValue().equals("")) {
				di.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
			} else {
				di.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
			}
		}

		if (modificheBloccate) {
			di.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
		}
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		// TODO Auto-generated method stub
		idLineaArticoloSeleizonato = 0;
		ContentValues val = ((EConTabAutoCompleteContentValue) arg0.getItemAtPosition(position)).getContentValue();

		editCodice.setText(val.getAsString(Listini.CODICE_ARTICOLO));
		editDescrizione.setText(val.getAsString(Listini.DESCRIZIONE));
		editPrzAcq.setText(Utility.formatNumero(val.getAsFloat(Listini.PRZ_ULTIMO_ACQUISTO), 2));
		editPrzLis.setText(Utility.formatNumero(val.getAsFloat(Listini.PRZ_LISTINO), 2));
		editSconto.setText(Utility.formatNumero(val.getAsFloat(Listini.SCONTO), 2));
		if (gestitoALinea == 1) {
			idLineaArticoloSeleizonato = val.getAsInteger(Listini.ID_LINEA);
			if (idLineaArticoloSeleizonato != 0 && !spinnerLinea.getValue().equals(val.getAsString(Listini.ID_LINEA))) {
				pulisciCodice = false;
				spinnerLinea.setValue(val.getAsString(Listini.ID_LINEA));

				spinnerLinea.setTabella(new Linee());
			}

			if (idLineaArticoloSeleizonato == 0) {
				Utility.mostraDialog(ctx.getString(R.string.attenzione),
						ctx.getString(R.string.messaggio_associazione_linea, val.getAsString(Listini.CODICE_ARTICOLO)), ctx, "OK");
			}

		} else {
			if (!spinnerFornitore.getValue().equals(val.getAsString(Listini.ID_COSTRUTTORE))) {
				pulisciCodice = false;
				spinnerFornitore.setValue(val.getAsString(Listini.ID_COSTRUTTORE));

				spinnerFornitore.setTabella(new Costruttori());
			}
		}
	}

	public void setModificheBloccate(boolean modificheBloccate) {
		// TODO Auto-generated method stub
		this.modificheBloccate = modificheBloccate;
	}

}
