package com.ncfsistemi.econ.form;

import android.content.ContentValues;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Placche;
import com.ncfsistemi.econ.utils.FaIcone;

/** Placca di una linea (PlaccheDettaglioModActivity). Extra nel nuovo: Linee.ID_LINEA come testo (proposta). */
public class PlaccaForm extends DefinizioneForm {

	@Override
	public AbstractTable tabella() {
		return new Placche();
	}

	@Override
	public String titolo(boolean nuovo) {
		return nuovo ? "Nuova placca" : "Modifica placca";
	}

	@Override
	public void sezioni(Form f) {
		f.sezione(FaIcone.PLACCHE, "Placca")
				.scelta(Placche.ID_LINEA, contesto.getString(R.string.linea), new Linee()).obbligatorio().fissoInModifica()
				.testo(Placche.NOME_PLACCA, contesto.getString(R.string.placca)).max(50);
	}

	@Override
	public void dopoCaricamento(Form f, ContentValues record, boolean nuovo) {
		String linea = ospite.getIntent().getStringExtra(Linee.ID_LINEA);
		if (nuovo && linea != null && !linea.equals("0")) f.imposta(Placche.ID_LINEA, linea);
	}
}
