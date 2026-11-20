package com.ncfsistemi.econ.prova;

/**
 * Solo debug: la scheda del documento senza login, sui dati finti dell'emulatore (ditta 2), per controllarne l'aspetto.
 * adb shell am start -n com.ncfsistemi.econ/.prova.DocumentoProvaActivity --ei id_preventivo 1
 */
public class DocumentoProvaActivity extends com.ncfsistemi.econ.DocumentoActivity {

	@Override
	protected void onCreate(android.os.Bundle stato) {
		com.ncfsistemi.econ.utils.Sessione.setDittaSelezionata(2);
		super.onCreate(stato);
	}

	@Override
	protected boolean isControllaLogin() {
		return false;
	}

	@Override
	protected boolean isControllaRegistrazione() {
		return false;
	}
}
