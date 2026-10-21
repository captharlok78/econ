package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.form.IvaForm;

/** Nuovo / modifica codice IVA: template form (STUDIO_FORM_APP.md), campi in IvaForm. */
public class IvaDettaglioActivity extends EconFormStandardActivity {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new IvaForm();
	}
}
