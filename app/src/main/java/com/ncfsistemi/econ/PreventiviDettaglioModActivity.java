package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.form.PreventivoForm;

/** Testata di preventivo e ordine sul template form: definizione in PreventivoForm. */
public class PreventiviDettaglioModActivity extends EconFormStandardActivity {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new PreventivoForm();
	}
}
