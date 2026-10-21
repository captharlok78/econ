package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.form.AziendaForm;

/** Dati della ditta: template form (STUDIO_FORM_APP.md), campi in AziendaForm. */
public class AziendaDettaglioActivity extends EconFormStandardActivity {

	@Override
	protected DefinizioneForm creaDefinizione() {
		return new AziendaForm();
	}
}
