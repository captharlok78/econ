package com.ncfsistemi.econ.form;

import android.content.ContentValues;

import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.ClientiReferenti;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;

/**
 * Referente del cliente (ClienteReferenteModActivity, GESTIONE_CLIENTI.md §6). Extra: "ID" (modifica) oppure
 * ClientiReferenti.ID_ANAGRAFICA (nuovo). Il referente e' del cliente, non di un suo indirizzo.
 */
public class ReferenteForm extends DefinizioneForm {

	@Override
	public AbstractTable tabella() {
		return new ClientiReferenti();
	}

	@Override
	public String titolo(boolean nuovo) {
		return nuovo ? "Nuovo referente" : "Modifica referente";
	}

	@Override
	public String sottotitolo(ContentValues record) {
		return record.getAsString(ClientiReferenti.NOME);
	}

	@Override
	public String moduloFunzionalita() {
		return FunzionalitaApp.CLIENTI;
	}

	@Override
	public void sezioni(Form f) {
		f.sezione(FaIcone.REFERENTI, "Referente")
				.testo(ClientiReferenti.NOME, "Nome e cognome").obbligatorio().max(100).icona(FaIcone.CLIENTE)
				.testo(ClientiReferenti.RUOLO, "Ruolo").max(50).aiuto("Es. Amministratore, Custode")
				.telefono(ClientiReferenti.TELEFONO, "Telefono").max(30).metaRiga()
				.telefono(ClientiReferenti.CELLULARE, "Cellulare").max(30).icona(FaIcone.CELLULARE).metaRiga()
				.email(ClientiReferenti.EMAIL, "Email").max(150)
				.note(ClientiReferenti.NOTE, "Note").max(255)
				.siNo(AbstractTable.ATTIVO, "Attivo").predefinito("1");
	}

	@Override
	public void primaDiSalvare(ContentValues val, boolean nuovo) {
		if (nuovo) val.put(ClientiReferenti.ID_ANAGRAFICA, ospite.getIntent().getIntExtra(ClientiReferenti.ID_ANAGRAFICA, 0));
	}
}
