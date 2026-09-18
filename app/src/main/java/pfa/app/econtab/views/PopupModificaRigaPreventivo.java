package pfa.app.econtab.views;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;

import pfa.app.econtab.R;
import pfa.app.econtab.adapters.PreventiviDettaglioAdapter;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.utils.Utility;

public class PopupModificaRigaPreventivo implements DialogInterface.OnClickListener, OnClickListener {
	View vista = null;
	View vistaTitolo = null;
	Context ctx = null;
	ContentValues val = null;
	BaseAdapter adapter = null;
	int position = 0;

	EditText editNumOperatori = null;
	EditText editQta = null;
	EditText editPrezzo = null;
	EditText editPrezzoAcq = null;
	EditText editPrezzoLis = null;
	EditText editRicarico = null;
	EditText editSconto = null;

	EditText editTitolo = null;
	private boolean modificheBloccate = false;

	public PopupModificaRigaPreventivo(BaseAdapter adapter, Context ctx, ContentValues val, int position) {
		vista = View.inflate(ctx, R.layout.dialog_edit_rigaprev_layout, null);
		vistaTitolo = View.inflate(ctx, R.layout.dialog_edit_rigaprev_titolo_layout, null);
		this.ctx = ctx;
		this.val = val;
		this.adapter = adapter;
		this.position = position;

		impostaCampi();

	}

	private void impostaCampi() {
		// TODO Auto-generated method stub
		editTitolo = (EditText) vistaTitolo.findViewById(R.id.editText_titolo);

		editNumOperatori = (EditText) vista.findViewById(R.id.editText_numop);
		editQta = (EditText) vista.findViewById(R.id.editText_qta);
		editPrezzo = (EditText) vista.findViewById(R.id.editText_prezzo);
		editPrezzoAcq = (EditText) vista.findViewById(R.id.editText_przAcq);
		editPrezzoLis = (EditText) vista.findViewById(R.id.editText_przlis);
		editRicarico = (EditText) vista.findViewById(R.id.editText_ric);
		editSconto = (EditText) vista.findViewById(R.id.editText_sco);

		editNumOperatori.setSelectAllOnFocus(true);
		if (!getTipoRiga().equals(PreventiviDettaglio.MATERIALE)) {
			editQta.setSelectAllOnFocus(true);
		}

		editPrezzo.setSelectAllOnFocus(true);
		editPrezzoAcq.setSelectAllOnFocus(true);
		editPrezzoLis.setSelectAllOnFocus(true);
		editRicarico.setSelectAllOnFocus(true);
		editSconto.setSelectAllOnFocus(true);

		editSconto.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				double sconto = Utility.formatNumeroDB(editSconto.getText().toString());
				if (sconto > 100) {
					editSconto.setText("100");
				}
			}
		});

		vista.findViewById(R.id.buttonRicalcola).setOnClickListener(this);
		vista.findViewById(R.id.buttonRicalcola1).setOnClickListener(this);

		if (getTipoRiga().equals(PreventiviDettaglio.ALTRO)) {
			editTitolo.setText(val.getAsString(PreventiviDettaglio.NOTE));
		} else {
			editTitolo.setText(val.getAsString(PreventiviDettaglio.DESCRIZIONE));
		}

		editNumOperatori.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.NUM_OPERATORI)));
		editQta.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.QUANTITA)));
		editPrezzo.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.PREZZO), 2));
		editPrezzoAcq.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.PREZZO_ACQ), 2));
		editPrezzoLis.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.PREZZO_VEN), 2));
		editRicarico.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.RICARICO)));
		editSconto.setText(Utility.formatNumero(val.getAsFloat(PreventiviDettaglio.SCONTO)));

		if (getTipoRiga().equals(PreventiviDettaglio.MATERIALE) || getTipoRiga().equals(PreventiviDettaglio.COLLEGAMENTI)) {
			editQta.setEnabled(false);
		}

		if (getTipoRiga().equals(PreventiviDettaglio.MANOPERA)) {
			vista.findViewById(R.id.linearCampi0).setVisibility(View.VISIBLE);
		} else {
			vista.findViewById(R.id.linearCampi0).setVisibility(View.GONE);
		}
	}

	public String getTipoRiga() {
		return val.getAsString(PreventiviDettaglio.TIPO);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		// TODO Auto-generated method stub
		if (which == DialogInterface.BUTTON_POSITIVE) {
			if (getTipoRiga().equals(PreventiviDettaglio.ALTRO)) {
				val.put(PreventiviDettaglio.NOTE, editTitolo.getText().toString());
			} else {
				val.put(PreventiviDettaglio.DESCRIZIONE, editTitolo.getText().toString());
			}

			val.put(PreventiviDettaglio.NUM_OPERATORI, Utility.formatNumeroDB(editNumOperatori.getText().toString()));
			val.put(PreventiviDettaglio.QUANTITA, Utility.formatNumeroDB(editQta.getText().toString()));
			val.put(PreventiviDettaglio.PREZZO, Utility.formatNumeroDB(editPrezzo.getText().toString()));
			val.put(PreventiviDettaglio.PREZZO_ACQ, Utility.formatNumeroDB(editPrezzoAcq.getText().toString()));
			val.put(PreventiviDettaglio.PREZZO_VEN, Utility.formatNumeroDB(editPrezzoLis.getText().toString()));
			val.put(PreventiviDettaglio.RICARICO, Utility.formatNumeroDB(editRicarico.getText().toString()));
			val.put(PreventiviDettaglio.SCONTO, Utility.formatNumeroDB(editSconto.getText().toString()));
			((PreventiviDettaglioAdapter) adapter).setInModifica(position);
			adapter.notifyDataSetChanged();
		}
	}

	public void apriPopup() {
		AlertDialog di = Utility.mostraDialogPersonalizzato("", null, ctx, vista, vistaTitolo, ctx.getString(R.string.conferma),
				ctx.getString(R.string.annulla), this);
		if (modificheBloccate) {
			di.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.buttonRicalcola) {
			double przAcq = Utility.formatNumeroDB(editPrezzoAcq.getText().toString());
			double ricarico = Utility.formatNumeroDB(editRicarico.getText().toString());
			double przNew = przAcq + (przAcq * ricarico) / 100;
			editPrezzo.setText(Utility.formatNumero(przNew, 2));
		}

		if (v.getId() == R.id.buttonRicalcola1) {
			double przLis = Utility.formatNumeroDB(editPrezzoLis.getText().toString());
			double sconto = Utility.formatNumeroDB(editSconto.getText().toString());
			double przNew = przLis - (przLis * sconto) / 100;
			editPrezzo.setText(Utility.formatNumero(przNew, 2));
		}
	}

	public void impostaFocus(String campo) {
		// TODO Auto-generated method stub
		if (campo.equals(PreventiviDettaglio.PREZZO)) {
			editPrezzo.requestFocus();
		}
        if (campo.equals(PreventiviDettaglio.NUM_OPERATORI)) {
            editNumOperatori.requestFocus();
        }
	}

	public void setModificheBloccate(boolean modificheBloccate) {
		// TODO Auto-generated method stub
		this.modificheBloccate = modificheBloccate;
	}

}
