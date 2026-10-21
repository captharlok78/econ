package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.form.PlaccaForm;

/** Nuova / modifica placca di una linea: template form (STUDIO_FORM_APP.md), campi in PlaccaForm. */
public class PlaccheDettaglioModActivity extends EconFormStandardActivity {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new PlaccaForm();
	}
}
