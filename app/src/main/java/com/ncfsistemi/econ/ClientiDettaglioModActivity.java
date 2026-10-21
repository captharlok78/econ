package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.ClienteForm;
import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;

/** Nuovo cliente / modifica del cliente: template form (STUDIO_FORM_APP.md), campi in ClienteForm. */
public class ClientiDettaglioModActivity extends EconFormStandardActivity {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new ClienteForm();
	}
}
