package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.form.PreventivoForm;

/** Testata di preventivo e ordine sul template form: definizione in PreventivoForm. */
public class PreventiviDettaglioModActivity extends EconFormStandardActivity {

	private PreventivoForm definizione;

	@Override
	protected DefinizioneForm creaDefinizione() {
		definizione = new PreventivoForm();
		return definizione;
	}

	/** Documento nuovo salvato: si apre subito la sua scheda per aggiungere le righe (STUDIO_PREVENTIVI_ORDINI.md §6.5). */
	@Override
	public void finish() {
		if (definizione != null && definizione.idCreato > 0 && esiste((int) definizione.idCreato)) {
			android.content.Intent intent = new android.content.Intent(this, DocumentoActivity.class);
			intent.putExtra(com.ncfsistemi.econ.db.table.Preventivi.ID_PREVENTIVO, (int) definizione.idCreato);
			startActivity(intent);
			definizione.idCreato = 0;
		}
		super.finish();
	}

	/** Il documento e' davvero scritto (la transazione del salvataggio e' andata a buon fine). */
	private boolean esiste(int id) {
		com.ncfsistemi.econ.db.DbInterno db = new com.ncfsistemi.econ.db.DbInterno(this);
		android.content.ContentValues w = new android.content.ContentValues();
		w.put(com.ncfsistemi.econ.db.table.Preventivi.ID_PREVENTIVO, id);
		boolean c = db.getRecord(new com.ncfsistemi.econ.db.table.Preventivi(), w) != null;
		db.close();
		return c;
	}
}
