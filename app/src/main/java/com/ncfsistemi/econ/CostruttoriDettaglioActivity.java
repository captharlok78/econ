package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.form.CostruttoreForm;

/** Nuovo / modifica fornitore (costruttore): template form (STUDIO_FORM_APP.md), campi in CostruttoreForm. */
public class CostruttoriDettaglioActivity extends EconFormStandardActivity {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new CostruttoreForm();
	}
}
