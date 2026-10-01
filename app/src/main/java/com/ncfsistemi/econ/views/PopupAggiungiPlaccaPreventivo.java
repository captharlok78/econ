package com.ncfsistemi.econ.views;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Placche;
import com.ncfsistemi.econ.db.table.PlaccheModuli;
import com.ncfsistemi.econ.db.table.PreventiviDettaglio;
import com.ncfsistemi.econ.fragments.PreventivoDettaglioFragment;
import com.ncfsistemi.econ.utils.Utility;

public class PopupAggiungiPlaccaPreventivo implements OnClickListener, TextWatcher {

	private View vista = null;

	private Context ctx = null;

	private AlertDialog di = null;
	private ContentValues valCantiere = null;
	private int idPreventivo = 0;

	private EconSpinner spinnerPlacca = null;
	private EconSpinner spinnerLinea = null;
	private EditText editModuli = null;

	private PreventivoDettaglioFragment fragment = null;

	public PopupAggiungiPlaccaPreventivo(Context ctx, ContentValues valCantiere, int idPreventivo, PreventivoDettaglioFragment fragment) {
		vista = View.inflate(ctx, R.layout.dialog_aggiungi_placca, null);

		this.ctx = ctx;
		this.valCantiere = valCantiere;
		this.idPreventivo = idPreventivo;
		this.fragment = fragment;

		impostaCampi();

	}

	private void impostaCampi() {
		spinnerPlacca = (EconSpinner) vista.findViewById(R.id.econSpinnerPlacca);
		spinnerLinea = (EconSpinner) vista.findViewById(R.id.econSpinnerLinea);

		int idLineaCantiere = valCantiere.getAsInteger(Cantieri.ID_LINEA);
		int idPlaccaCantiere = valCantiere.getAsInteger(Cantieri.ID_PLACCA);

		spinnerLinea.setValue("" + idLineaCantiere);
		spinnerPlacca.setValue("" + idPlaccaCantiere);

		spinnerLinea.setTabella(new Linee());
		spinnerPlacca.setTabella(new Placche());
		spinnerPlacca.setMessaggioDisabilitato(ctx.getString(R.string.errore_selezione_linea));
		spinnerLinea.setSpinnerCollegato(spinnerPlacca);

		spinnerPlacca.addTextChangeListener(this);

		editModuli = (EditText) vista.findViewById(R.id.editText_moduli);

	}

	public void apriPopup() {

		di = Utility.mostraDialogPersonalizzato(ctx.getString(R.string.aggiungi_placca), ctx, vista, ctx.getString(R.string.conferma),
				ctx.getString(R.string.annulla), this);

		controllaAbilitazioneTastoConferma();

	}

	@Override
	public void onClick(DialogInterface arg0, int which) {
		// TODO Auto-generated method stub
		if (which == DialogInterface.BUTTON_POSITIVE) {
			DbInterno db = new DbInterno(ctx);

			int idPlaccaModuli = 0;
			int numeroModuli = 1;
			String numeroModuliTxt = editModuli.getText().toString();
			if (numeroModuliTxt.trim().length() > 0) {
				numeroModuli = Integer.parseInt(numeroModuliTxt.trim());
			}

			PlaccheModuli tabPlaccheModuli = new PlaccheModuli();

			idPlaccaModuli = tabPlaccheModuli.controllaInserisciRecord(db, Integer.parseInt(spinnerPlacca.getValue()), numeroModuli);

			PreventiviDettaglio pd = new PreventiviDettaglio();
			ContentValues valInsert = new ContentValues();
			valInsert.put(PreventiviDettaglio.ID_LOCALE, 0);
			valInsert.put(PreventiviDettaglio.TIPO, PreventiviDettaglio.PLACCHE_PREVENTIVO);
			valInsert.put(PreventiviDettaglio.DESCRIZIONE, spinnerPlacca.getRecordSelezionato().getAsString(Placche.NOME_PLACCA) + " " + numeroModuli+" Mod.");
			valInsert.put(PreventiviDettaglio.ID_PLACCA_MODULI, idPlaccaModuli);
			valInsert.put(PreventiviDettaglio.QUANTITA, 1);

			pd.aggiornaRigaMaterialePreventivo(db, idPreventivo, valInsert, 1, PreventiviDettaglio.PLACCHE_PREVENTIVO, false);
			db.close();
			fragment.ricerca();
		}

	}

	@Override
	public void afterTextChanged(Editable arg0) {
		// TODO Auto-generated method stub
		controllaAbilitazioneTastoConferma();
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	private void controllaAbilitazioneTastoConferma() {

		if (spinnerPlacca.getValue().equals("")) {
			di.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
		} else {
			di.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
		}

	}

}
