package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.form.AreaForm;

/** Nuova / modifica area di un'unita': template form (STUDIO_FORM_APP.md), campi in AreaForm. */
public class AreaDettaglioModActivity extends EconFormStandardActivity {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new AreaForm();
	}
}
