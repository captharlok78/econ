package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.form.UnitaForm;

/** Nuova / modifica unita' del cantiere: template form (STUDIO_FORM_APP.md), campi in UnitaForm. */
public class UnitaDettaglioModActivity extends EconFormStandardActivity {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new UnitaForm();
	}
}
