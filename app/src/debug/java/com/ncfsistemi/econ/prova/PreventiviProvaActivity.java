package com.ncfsistemi.econ.prova;

/** Solo debug: l'elenco dei preventivi senza login, sui dati finti dell'emulatore (ditta 2). */
public class PreventiviProvaActivity extends com.ncfsistemi.econ.PreventiviActivity {

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
