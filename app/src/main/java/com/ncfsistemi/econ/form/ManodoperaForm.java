package com.ncfsistemi.econ.form;

import android.content.ContentValues;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Manodopera;
import com.ncfsistemi.econ.utils.FaIcone;

/** Tipo di manodopera (ManodoperaDettaglioActivity): operatori proposti (almeno 1) e costo orario. */
public class ManodoperaForm extends DefinizioneForm {

	@Override
	public AbstractTable tabella() {
		return new Manodopera();
	}

	@Override
	public String titolo(boolean nuovo) {
		return nuovo ? "Nuova manodopera" : "Modifica manodopera";
	}

	@Override
	public void sezioni(Form f) {
		f.sezione(FaIcone.MANODOPERA, "Manodopera")
				.testo(Manodopera.NOME, contesto.getString(R.string.manodopera)).max(100)
				.intero(Manodopera.NUM_OPERATORI_DEFAULT, contesto.getString(R.string.numero_operatori)).max(3).predefinito("1").metaRiga()
				.numero(Manodopera.COSTO_ORARIO, contesto.getString(R.string.costo_orario)).max(8).decimali(2).predefinito("30").metaRiga();
	}

	@Override
	public void primaDiSalvare(ContentValues val, boolean nuovo) {
		String operatori = val.getAsString(Manodopera.NUM_OPERATORI_DEFAULT);
		if (operatori == null || operatori.trim().isEmpty() || operatori.trim().equals("0")) {
			val.put(Manodopera.NUM_OPERATORI_DEFAULT, 1);
		} else {
			val.put(Manodopera.NUM_OPERATORI_DEFAULT, Integer.parseInt(operatori.trim()));
		}
	}
}
