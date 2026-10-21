package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.form.ReferenteForm;

/** Nuovo / modifica referente del cliente: template form (STUDIO_FORM_APP.md), campi in ReferenteForm. */
public class ClienteReferenteModActivity extends EconFormStandardActivity {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new ReferenteForm();
	}
}
