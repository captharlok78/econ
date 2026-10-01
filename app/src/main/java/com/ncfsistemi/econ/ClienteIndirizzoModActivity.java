package com.ncfsistemi.econ;

import android.content.ContentValues;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.ClientiIndirizzi;

/**
 * Nuovo indirizzo / modifica di un indirizzo del cliente (GESTIONE_CLIENTI.md §6). Extra: "ID" (modifica) oppure
 * ClientiIndirizzi.ID_ANAGRAFICA (nuovo). Se diventa principale, gli altri indirizzi del cliente non lo sono piu' (il
 * server fa lo stesso e copia la sede sul cliente).
 */
public class ClienteIndirizzoModActivity extends EconDettaglioActivity {

	private int idCliente;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_cliente_indirizzo_mod);
		String[] tipi = new String[ClientiIndirizzi.TIPI.length];
		for (int i = 0; i < tipi.length; i++) tipi[i] = ClientiIndirizzi.TIPI[i][1];
		ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tipi);
		ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		((Spinner) findViewById(R.id.spinner_tipo)).setAdapter(ad);
		idCliente = getIntent().getIntExtra(ClientiIndirizzi.ID_ANAGRAFICA, 0);
		super.onCreate(savedInstanceState);
		if (getModalita() == INSERIMENTO) {
			setText(R.id.editText_nazione, "IT");
		}
		// "crea anche il cantiere": solo per un indirizzo nuovo e con la funzionalita' del pacchetto
		boolean creaCantiere = getModalita() == INSERIMENTO
				&& com.ncfsistemi.econ.utils.FunzionalitaApp.ha(this, com.ncfsistemi.econ.utils.FunzionalitaApp.CANTIERI_CREA);
		findViewById(R.id.checkBox_crea_cantiere).setVisibility(creaCantiere ? android.view.View.VISIBLE : android.view.View.GONE);
	}

	@Override
	protected String getTitoloDettaglio() {
		return getModalita() == INSERIMENTO ? "Nuovo indirizzo" : "Modifica indirizzo";
	}

	@Override
	protected void inizializzaModifica() {
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(ClientiIndirizzi.ID, getIDModifica());
		ContentValues v = db.getRecord(new ClientiIndirizzi(), where);
		db.close();
		if (v == null) return;
		idCliente = v.getAsInteger(ClientiIndirizzi.ID_ANAGRAFICA);
		for (int i = 0; i < ClientiIndirizzi.TIPI.length; i++) {
			if (ClientiIndirizzi.TIPI[i][0].equals(v.getAsString(ClientiIndirizzi.TIPO))) {
				((Spinner) findViewById(R.id.spinner_tipo)).setSelection(i);
			}
		}
		setText(R.id.editText_descrizione, v.getAsString(ClientiIndirizzi.DESCRIZIONE));
		setText(R.id.editText_indirizzo, v.getAsString(ClientiIndirizzi.INDIRIZZO));
		setText(R.id.editText_civico, v.getAsString(ClientiIndirizzi.CIVICO));
		setText(R.id.editText_cap, v.getAsString(ClientiIndirizzi.CAP));
		setText(R.id.editText_citta, v.getAsString(ClientiIndirizzi.CITTA));
		setText(R.id.editText_provincia, v.getAsString(ClientiIndirizzi.PROVINCIA));
		setText(R.id.editText_nazione, v.getAsString(ClientiIndirizzi.NAZIONE));
		setText(R.id.editText_note, v.getAsString(ClientiIndirizzi.NOTE));
		((CheckBox) findViewById(R.id.checkBox_principale)).setChecked(intero(v, ClientiIndirizzi.PRINCIPALE) == 1);
		((CheckBox) findViewById(R.id.checkBox_attivo)).setChecked(intero(v, AbstractTable.ATTIVO) != 0);
	}

	private static int intero(ContentValues v, String campo) {
		Integer i = v.getAsInteger(campo);
		return i == null ? 0 : i;
	}

	private void valori(ContentValues val) {
		val.put(ClientiIndirizzi.TIPO, ClientiIndirizzi.TIPI[((Spinner) findViewById(R.id.spinner_tipo)).getSelectedItemPosition()][0]);
		val.put(ClientiIndirizzi.DESCRIZIONE, getTesto(R.id.editText_descrizione));
		val.put(ClientiIndirizzi.INDIRIZZO, getTesto(R.id.editText_indirizzo));
		val.put(ClientiIndirizzi.CIVICO, getTesto(R.id.editText_civico));
		val.put(ClientiIndirizzi.CAP, getTesto(R.id.editText_cap));
		val.put(ClientiIndirizzi.CITTA, getTesto(R.id.editText_citta));
		val.put(ClientiIndirizzi.PROVINCIA, getTesto(R.id.editText_provincia).toUpperCase());
		String naz = getTesto(R.id.editText_nazione).toUpperCase();
		val.put(ClientiIndirizzi.NAZIONE, naz.isEmpty() ? "IT" : naz);
		val.put(ClientiIndirizzi.NOTE, getTesto(R.id.editText_note));
		val.put(ClientiIndirizzi.PRINCIPALE, ((CheckBox) findViewById(R.id.checkBox_principale)).isChecked() ? 1 : 0);
		val.put(AbstractTable.ATTIVO, ((CheckBox) findViewById(R.id.checkBox_attivo)).isChecked() ? 1 : 0);
	}

	/** Italia: CAP di 5 cifre. */
	private String controlla() {
		String naz = getTesto(R.id.editText_nazione).toUpperCase();
		String cap = getTesto(R.id.editText_cap);
		if ((naz.isEmpty() || naz.equals("IT")) && !cap.isEmpty() && !cap.matches("\\d{5}")) {
			return "Il CAP italiano ha 5 cifre.";
		}
		return null;
	}

	/** Un solo principale per cliente: gli altri indirizzi smettono di esserlo (da inviare). */
	private void togliAltriPrincipali(DbInterno db, ContentValues val, long idQuesto) {
		if (intero(val, ClientiIndirizzi.PRINCIPALE) != 1) return;
		db.getWritableDatabase().execSQL("UPDATE " + ClientiIndirizzi.NOME_TABELLA + " SET " + ClientiIndirizzi.PRINCIPALE + " = 0, "
				+ AbstractTable.IN_SERVER + " = 0 WHERE " + ClientiIndirizzi.ID_ANAGRAFICA + " = ? AND " + ClientiIndirizzi.ID + " <> ? AND "
				+ ClientiIndirizzi.PRINCIPALE + " = 1", new Object[]{idCliente, idQuesto});
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		String errore = controlla();
		if (errore != null) return errore;
		ClientiIndirizzi tab = new ClientiIndirizzi();
		ContentValues val = tab.getValoriLogInserimento(db);
		valori(val);
		val.put(ClientiIndirizzi.ID_ANAGRAFICA, idCliente);
		tab.inserisciRecord(db, val);
		togliAltriPrincipali(db, val, val.getAsLong(ClientiIndirizzi.ID));
		CheckBox crea = findViewById(R.id.checkBox_crea_cantiere);
		if (crea.getVisibility() == android.view.View.VISIBLE && crea.isChecked()) {
			String esito = com.ncfsistemi.econ.utils.CantiereDaIndirizzo.crea(db, val.getAsInteger(ClientiIndirizzi.ID));
			android.widget.Toast.makeText(this, esito, android.widget.Toast.LENGTH_LONG).show();
		}
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		String errore = controlla();
		if (errore != null) return errore;
		ClientiIndirizzi tab = new ClientiIndirizzi();
		ContentValues val = tab.getValoriLogModifica(db);
		valori(val);
		ContentValues where = new ContentValues();
		where.put(ClientiIndirizzi.ID, getIDModifica());
		tab.aggiornaRecord(db, val, where);
		togliAltriPrincipali(db, val, getIDModifica());
		return super.eseguiAggiornamento(db);
	}

	@Override
	protected String moduloFunzionalita() {
		return com.ncfsistemi.econ.utils.FunzionalitaApp.CLIENTI;
	}
}
