package pfa.app.econtab;

import android.content.ContentValues;
import android.os.Bundle;
import android.widget.CheckBox;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.AbstractTable;
import pfa.app.econtab.db.table.ClientiReferenti;

/**
 * Nuovo referente / modifica di un referente del cliente (GESTIONE_CLIENTI.md §6). Extra: "ID" (modifica) oppure
 * ClientiReferenti.ID_ANAGRAFICA (nuovo). Il referente e' del cliente, non di un suo indirizzo.
 */
public class ClienteReferenteModActivity extends EConTabDettaglioActivity {

	private int idCliente;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_cliente_referente_mod);
		idCliente = getIntent().getIntExtra(ClientiReferenti.ID_ANAGRAFICA, 0);
		super.onCreate(savedInstanceState);
	}

	@Override
	protected String getTitoloDettaglio() {
		return getModalita() == INSERIMENTO ? "Nuovo referente" : "Modifica referente";
	}

	@Override
	protected void inizializzaModifica() {
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(ClientiReferenti.ID, getIDModifica());
		ContentValues v = db.getRecord(new ClientiReferenti(), where);
		db.close();
		if (v == null) return;
		setText(R.id.editText_nome, v.getAsString(ClientiReferenti.NOME));
		setText(R.id.editText_ruolo, v.getAsString(ClientiReferenti.RUOLO));
		setText(R.id.editText_telefono, v.getAsString(ClientiReferenti.TELEFONO));
		setText(R.id.editText_cellulare, v.getAsString(ClientiReferenti.CELLULARE));
		setText(R.id.editText_email, v.getAsString(ClientiReferenti.EMAIL));
		setText(R.id.editText_note, v.getAsString(ClientiReferenti.NOTE));
		Integer attivo = v.getAsInteger(AbstractTable.ATTIVO);
		((CheckBox) findViewById(R.id.checkBox_attivo)).setChecked(attivo == null || attivo != 0);
	}

	private void valori(ContentValues val) {
		val.put(ClientiReferenti.NOME, getTesto(R.id.editText_nome));
		val.put(ClientiReferenti.RUOLO, getTesto(R.id.editText_ruolo));
		val.put(ClientiReferenti.TELEFONO, getTesto(R.id.editText_telefono));
		val.put(ClientiReferenti.CELLULARE, getTesto(R.id.editText_cellulare));
		val.put(ClientiReferenti.EMAIL, getTesto(R.id.editText_email));
		val.put(ClientiReferenti.NOTE, getTesto(R.id.editText_note));
		val.put(AbstractTable.ATTIVO, ((CheckBox) findViewById(R.id.checkBox_attivo)).isChecked() ? 1 : 0);
	}

	private String controlla() {
		if (getTesto(R.id.editText_nome).isEmpty()) return "Il nome del referente è obbligatorio.";
		String email = getTesto(R.id.editText_email);
		if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Email non valida.";
		return null;
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		String errore = controlla();
		if (errore != null) return errore;
		ClientiReferenti tab = new ClientiReferenti();
		ContentValues val = tab.getValoriLogInserimento(db);
		valori(val);
		val.put(ClientiReferenti.ID_ANAGRAFICA, idCliente);
		tab.inserisciRecord(db, val);
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		String errore = controlla();
		if (errore != null) return errore;
		ClientiReferenti tab = new ClientiReferenti();
		ContentValues val = tab.getValoriLogModifica(db);
		valori(val);
		ContentValues where = new ContentValues();
		where.put(ClientiReferenti.ID, getIDModifica());
		tab.aggiornaRecord(db, val, where);
		return super.eseguiAggiornamento(db);
	}

	@Override
	protected String moduloFunzionalita() {
		return pfa.app.econtab.utils.FunzionalitaApp.CLIENTI;
	}
}
