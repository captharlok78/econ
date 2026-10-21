package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.form.ManodoperaForm;

/** Nuovo / modifica tipo di manodopera: template form (STUDIO_FORM_APP.md), campi in ManodoperaForm. */
public class ManodoperaDettaglioActivity extends EconFormStandardActivity {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new ManodoperaForm();
	}
}
