package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.form.LocaleForm;

/** Nuovo / modifica locale di un'area: template form (STUDIO_FORM_APP.md), campi in LocaleForm. */
public class LocaleDettaglioModActivity extends EconFormStandardActivity {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new LocaleForm();
	}
}
