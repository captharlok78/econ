package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.form.ArticoloForm;

/** Nuovo / modifica codice articolo del listino: template form (STUDIO_FORM_APP.md), campi in ArticoloForm. */
public class CodiceArticoloModActivity extends EconFormStandardActivity {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new ArticoloForm();
	}
}
