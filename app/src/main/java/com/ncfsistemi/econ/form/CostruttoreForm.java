package com.ncfsistemi.econ.form;

import android.content.ContentValues;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Costruttori;
import com.ncfsistemi.econ.utils.FaIcone;

/** Fornitore (CostruttoriDettaglioActivity): la sigla Metel di un fornitore gia' sul server non si cambia. */
public class CostruttoreForm extends DefinizioneForm {

	@Override
	public AbstractTable tabella() {
		return new Costruttori();
	}

	@Override
	public String titolo(boolean nuovo) {
		return nuovo ? "Nuovo fornitore" : "Modifica fornitore";
	}

	@Override
	public void sezioni(Form f) {
		f.sezione(FaIcone.DITTA, "Fornitore")
				.testo(Costruttori.RAGIONE_SOCIALE, contesto.getString(R.string.ragione_sociale)).max(100).metaRiga(3)
				.testo(Costruttori.SIGLA_METEL, contesto.getString(R.string.siglametel)).max(3).metaRiga();
	}

	@Override
	public void dopoCaricamento(Form f, ContentValues record, boolean nuovo) {
		Integer id = record == null ? null : record.getAsInteger(Costruttori.ID_COSTRUTTORE);
		if (id != null && id > 0) f.blocca(Costruttori.SIGLA_METEL);
	}
}
