package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.form.IndirizzoForm;

/** Nuovo / modifica indirizzo del cliente: template form (STUDIO_FORM_APP.md), campi in IndirizzoForm. */
public class ClienteIndirizzoModActivity extends EconFormStandardActivity {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new IndirizzoForm();
	}
}
