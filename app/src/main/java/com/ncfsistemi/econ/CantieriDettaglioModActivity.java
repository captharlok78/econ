package com.ncfsistemi.econ;

import com.ncfsistemi.econ.form.CantiereForm;
import com.ncfsistemi.econ.form.DefinizioneForm;
import com.ncfsistemi.econ.form.EconFormStandardActivity;
import com.ncfsistemi.econ.utils.Utility;

/** Nuovo cantiere / modifica del cantiere sul template form: definizione in CantiereForm. */
public class CantieriDettaglioModActivity extends EconFormStandardActivity {

	/** Extra: id dell'indirizzo del cliente da cui nasce il cantiere (resta collegato). */
	public static final String EXTRA_INDIRIZZO = "id_cliente_indirizzo";
	/** Extra: true = cantiere libero del cliente, senza nome e indirizzo proposti (scheda cliente, tabella Cantieri). */
	public static final String EXTRA_LIBERO = "cantiere_libero";

	private CantiereForm cantiere;

	@Override
	protected DefinizioneForm creaDefinizione() {
		cantiere = new CantiereForm();
		return cantiere;
	}

	/** Linea cambiata: prima si sceglie se cambiarla anche su unita', aree e locali, poi la conferma solita. */
	@Override
	protected void mostraDialogSalvataggio() {
		if (getModalita() != MODIFICA || !cantiere.lineaCambiata(getForm())) {
			super.mostraDialogSalvataggio();
			return;
		}
		String[] voci = { getString(R.string.cambia_linea_locali), getString(R.string.cambia_linea_solo_sul_cantiere) };
		Utility.mostraSelezioneDialog(getString(R.string.attenzione), voci, this, (dialog, quale) -> {
			cantiere.setCambiaLineaLocali(quale == 0);
			super.mostraDialogSalvataggio();
		});
	}
}
