package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.form.LineaForm;

/** Nuova / modifica linea di un fornitore: template form (STUDIO_FORM_APP.md), campi in LineaForm. */
public class LineeDettaglioModActivity extends EconFormStandardActivity {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new LineaForm();
	}
}
