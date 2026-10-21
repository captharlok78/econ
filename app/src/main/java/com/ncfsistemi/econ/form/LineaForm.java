package com.ncfsistemi.econ.form;

import android.content.ContentValues;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Costruttori;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.utils.FaIcone;

/** Linea di un fornitore (LineeDettaglioModActivity). Extra nel nuovo: Costruttori.ID_COSTRUTTORE (proposto). */
public class LineaForm extends DefinizioneForm {

	@Override
	public AbstractTable tabella() {
		return new Linee();
	}

	@Override
	public String titolo(boolean nuovo) {
		return nuovo ? "Nuova linea" : "Modifica linea";
	}

	@Override
	public void sezioni(Form f) {
		f.sezione(FaIcone.ELENCO, "Linea")
				.scelta(Linee.ID_COSTRUTTORE, contesto.getString(R.string.fornitore), new Costruttori()).obbligatorio().fissoInModifica()
				.testo(Linee.NOME_LINEA, contesto.getString(R.string.linea)).max(50);
	}

	@Override
	public void dopoCaricamento(Form f, ContentValues record, boolean nuovo) {
		int fornitore = ospite.getIntent().getIntExtra(Costruttori.ID_COSTRUTTORE, 0);
		if (nuovo && fornitore != 0) f.imposta(Linee.ID_COSTRUTTORE, "" + fornitore);
	}
}
