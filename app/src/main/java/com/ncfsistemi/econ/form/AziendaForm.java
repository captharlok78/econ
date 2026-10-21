package com.ncfsistemi.econ.form;

import android.content.ContentValues;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Ditte;
import com.ncfsistemi.econ.utils.FaIcone;

/** Dati della ditta (AziendaDettaglioActivity, da Configurazione): le code si scrivono in cm, nel DB sono in metri. */
public class AziendaForm extends DefinizioneForm {

	private static final String CM_SCATOLE = "cm_coda_scatole", CM_UTILIZZATORI = "cm_coda_utilizzatori";

	@Override
	public AbstractTable tabella() {
		return new Ditte();
	}

	@Override
	public String titolo(boolean nuovo) {
		return nuovo ? "Nuova ditta" : "Dati della ditta";
	}

	@Override
	public void sezioni(Form f) {
		f.sezione(FaIcone.DITTA, "Ditta")
				.testo(Ditte.RAGIONE_SOCIALE, contesto.getString(R.string.ragione_sociale)).max(100)
				.testo(Ditte.INDIRIZZO, contesto.getString(R.string.indirizzo)).max(255)
				.intero(Ditte.CAP, "CAP").max(5).metaRiga()
				.testo(Ditte.CITTA, "Città").max(70).metaRiga(3)
				.testo(Ditte.PROVINCIA, "Prov.").max(2).maiuscolo().metaRiga()
				.testo(Ditte.CODICE_FISCALE, contesto.getString(R.string.codice_fiscale)).max(16).maiuscolo().metaRiga().nuovaRiga()
				.testo(Ditte.PARTITA_IVA, contesto.getString(R.string.partita_iva)).max(50).metaRiga();
		f.sezione(FaIcone.IMPIANTO, "Gestione elettrica")
				.intero(CM_SCATOLE, contesto.getString(R.string.centimetri_coda_scatole)).appoggio().metaRiga()
				.intero(CM_UTILIZZATORI, contesto.getString(R.string.centimetri_coda_utilizzatori)).appoggio().metaRiga();
	}

	private static String cm(ContentValues record, String metri) {
		Double m = record.getAsDouble(metri);
		return m == null ? "" : "" + (int) Math.round(m * 100);
	}

	@Override
	public void dopoCaricamento(Form f, ContentValues record, boolean nuovo) {
		if (record == null) return;
		f.imposta(CM_SCATOLE, cm(record, Ditte.METRI_CODA_SCATOLE));
		f.imposta(CM_UTILIZZATORI, cm(record, Ditte.METRI_CODA_UTILIZZATORI));
	}

	private static double metri(String cm) {
		return cm.trim().isEmpty() ? 0 : Double.parseDouble(cm.trim()) / 100;
	}

	@Override
	public void primaDiSalvare(ContentValues val, boolean nuovo) {
		val.put(Ditte.METRI_CODA_SCATOLE, metri(ospite.getForm().valore(CM_SCATOLE)));
		val.put(Ditte.METRI_CODA_UTILIZZATORI, metri(ospite.getForm().valore(CM_UTILIZZATORI)));
	}
}
