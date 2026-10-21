package com.ncfsistemi.econ.form;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Iva;
import com.ncfsistemi.econ.utils.FaIcone;

/** Codice IVA (IvaDettaglioActivity): il codice e' la chiave, quindi si scrive solo nel nuovo. */
public class IvaForm extends DefinizioneForm {

	@Override
	public AbstractTable tabella() {
		return new Iva();
	}

	@Override
	public String titolo(boolean nuovo) {
		return nuovo ? "Nuovo codice IVA" : "Modifica codice IVA";
	}

	@Override
	public void sezioni(Form f) {
		f.sezione(FaIcone.PERCENTUALE, "Codice IVA")
				.testo(Iva.CODICE_IVA, contesto.getString(R.string.codice_iva)).obbligatorio().max(10).fissoInModifica().metaRiga()
				.numero(Iva.ALIQUOTA, contesto.getString(R.string.aliquota)).obbligatorio().icona(FaIcone.PERCENTUALE).metaRiga();
	}
}
