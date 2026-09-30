package pfa.app.econtab;

import android.content.ContentValues;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.AbstractTable;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.ClientiIndirizzi;
import pfa.app.econtab.db.table.Iva;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.views.EConTabSpinner;

/**
 * Nuovo cliente / modifica del cliente (GESTIONE_CLIENTI.md §6), in sezioni: Dati principali (tipo azienda o persona
 * fisica, attivo), Sede principale (solo nel nuovo cliente: il server ne fa il suo primo indirizzo), Dati fatturazione
 * (P.IVA, CF, IVA, PEC, SDI). Gli altri indirizzi e i referenti si gestiscono dalla scheda del cliente.
 */
public class ClientiDettaglioModActivity extends EConTabDettaglioActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_clienti_dettaglio_mod);
		super.onCreate(savedInstanceState);
		// testate di sezione dello standard grafico
		pfa.app.econtab.utils.SezioneStandard.imposta(findViewById(R.id.sezione_principali), pfa.app.econtab.utils.FaIcone.CLIENTE, "Dati principali");
		pfa.app.econtab.utils.SezioneStandard.imposta(findViewById(R.id.sezione_sede_titolo), pfa.app.econtab.utils.FaIcone.INDIRIZZO, "Sede principale");
		pfa.app.econtab.utils.SezioneStandard.imposta(findViewById(R.id.sezione_fatturazione), pfa.app.econtab.utils.FaIcone.FATTURAZIONE, "Dati fatturazione");

		if (getModalita() == INSERIMENTO) {
			((EConTabSpinner) findViewById(R.id.spinner_iva)).setTabella(new Iva());
		} else {
			// in modifica l'indirizzo si cambia dalla linguetta Indirizzi della scheda
			findViewById(R.id.sezione_sede).setVisibility(View.GONE);
		}
		// per le licenze server non si modifica il codice che arriva dal server (modifica per PFA)
		if (Sessione.isLicenzaBusiness(this)) {
			findViewById(R.id.editText_codice).setEnabled(false);
		}
		((RadioButton) findViewById(R.id.radio_tipo_persona)).setOnCheckedChangeListener((b, persona) -> mostraPartitaIva(!persona));
	}

	/** Persona fisica: niente partita IVA. */
	private void mostraPartitaIva(boolean mostra) {
		View piva = findViewById(R.id.editText_partitaiva);
		((View) piva.getParent()).setVisibility(mostra ? View.VISIBLE : View.GONE);
	}

	@Override
	protected String getTitoloDettaglio() {
		return getModalita() == INSERIMENTO ? "Nuovo cliente" : "Modifica cliente";
	}

	@Override
	protected void inizializzaModifica() {
		super.inizializzaModifica();
		int id = getIntent().getIntExtra("ID", 0);
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(Anagrafica.ID_ANAGRAFICA, id);
		ContentValues val = db.getRecord(new Anagrafica(), where);
		db.close();
		if (val != null) {
			setText(R.id.editText_ragionesociale, val.getAsString(Anagrafica.RAGIONE_SOCIALE));
			setText(R.id.editText_codice, val.getAsString(Anagrafica.CODICE));
			setText(R.id.editText_cf, val.getAsString(Anagrafica.CODICE_FISCALE));
			setText(R.id.editText_partitaiva, val.getAsString(Anagrafica.PARTITA_IVA));
			setText(R.id.editText_cellulare1, val.getAsString(Anagrafica.CELLULARE));
			setText(R.id.editText_telefono, val.getAsString(Anagrafica.TELEFONO));
			setText(R.id.spinner_iva, val.getAsString(Anagrafica.CODICE_IVA));
			setText(R.id.editText_mail, val.getAsString(Anagrafica.MAIL));
			setText(R.id.editText_note, val.getAsString(Anagrafica.NOTE));
			setText(R.id.editText_pec, val.getAsString(Anagrafica.PEC));
			setText(R.id.editText_sdi, val.getAsString(Anagrafica.CODICE_SDI));
			boolean persona = Anagrafica.TIPO_PERSONA_FISICA.equals(val.getAsString(Anagrafica.TIPO_CF));
			((RadioButton) findViewById(persona ? R.id.radio_tipo_persona : R.id.radio_tipo_azienda)).setChecked(true);
			mostraPartitaIva(!persona);
			Integer attivo = val.getAsInteger(AbstractTable.ATTIVO);
			((CheckBox) findViewById(R.id.checkBox_attivo)).setChecked(attivo == null || attivo != 0);
		}
		((EConTabSpinner) findViewById(R.id.spinner_iva)).setTabella(new Iva());
	}

	/** Campi comuni a nuovo e modifica. */
	private void valoriComuni(ContentValues val) {
		boolean persona = ((RadioButton) findViewById(R.id.radio_tipo_persona)).isChecked();
		val.put(Anagrafica.TIPO_CF, persona ? Anagrafica.TIPO_PERSONA_FISICA : Anagrafica.TIPO_AZIENDA);
		val.put(Anagrafica.RAGIONE_SOCIALE, getTesto(R.id.editText_ragionesociale));
		val.put(Anagrafica.CODICE, getTesto(R.id.editText_codice));
		val.put(Anagrafica.CODICE_FISCALE, getTesto(R.id.editText_cf).toUpperCase());
		val.put(Anagrafica.PARTITA_IVA, persona ? "" : getTesto(R.id.editText_partitaiva));
		val.put(Anagrafica.CELLULARE, getTesto(R.id.editText_cellulare1));
		val.put(Anagrafica.TELEFONO, getTesto(R.id.editText_telefono));
		val.put(Anagrafica.MAIL, getTesto(R.id.editText_mail));
		val.put(Anagrafica.CODICE_IVA, getTesto(R.id.spinner_iva));
		val.put(Anagrafica.NOTE, getTesto(R.id.editText_note));
		val.put(Anagrafica.PEC, getTesto(R.id.editText_pec));
		val.put(Anagrafica.CODICE_SDI, getTesto(R.id.editText_sdi).toUpperCase());
		val.put(AbstractTable.ATTIVO, ((CheckBox) findViewById(R.id.checkBox_attivo)).isChecked() ? 1 : 0);
	}

	private String controllaIva() {
		if (getTesto(R.id.spinner_iva).equals("")) {
			((EConTabSpinner) findViewById(R.id.spinner_iva)).setError(getString(R.string.errore_selezione_iva));
			return getString(R.string.errore_selezione_iva);
		}
		return null;
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		String errore = controllaIva();
		if (errore != null) return errore;
		Anagrafica tabella = new Anagrafica();
		ContentValues val = tabella.getValoriLogInserimento(db);
		valoriComuni(val);
		// sede principale con via e civico separati: diventa il primo indirizzo del cliente (va al server nello stesso invio;
		// SedePrincipaleListener allinea poi il cliente alla sede). Sul cliente resta anche la copia "via civico".
		String via = getTesto(R.id.editText_indirizzo).trim(), civico = getTesto(R.id.editText_civico).trim();
		String cap = getTesto(R.id.editText_cap), citta = getTesto(R.id.editText_citta), prov = getTesto(R.id.editText_provincia).toUpperCase();
		val.put(Anagrafica.INDIRIZZO, (via + " " + civico).trim());
		val.put(Anagrafica.CAP, cap);
		val.put(Anagrafica.CITTA, citta);
		val.put(Anagrafica.PROVINCIA, prov);
		tabella.inserisciRecord(db, val);
		if (!(via + civico + cap + citta + prov).trim().isEmpty()) {
			ClientiIndirizzi tabInd = new ClientiIndirizzi();
			ContentValues sede = tabInd.getValoriLogInserimento(db);
			sede.put(ClientiIndirizzi.ID_ANAGRAFICA, val.getAsInteger(Anagrafica.ID_ANAGRAFICA));
			sede.put(ClientiIndirizzi.TIPO, ClientiIndirizzi.TIPI[0][0]);
			sede.put(ClientiIndirizzi.INDIRIZZO, via);
			sede.put(ClientiIndirizzi.CIVICO, civico);
			sede.put(ClientiIndirizzi.CAP, cap);
			sede.put(ClientiIndirizzi.CITTA, citta);
			sede.put(ClientiIndirizzi.PROVINCIA, prov);
			sede.put(ClientiIndirizzi.NAZIONE, "IT");
			sede.put(ClientiIndirizzi.PRINCIPALE, 1);
			sede.put(AbstractTable.ATTIVO, 1);
			tabInd.inserisciRecord(db, sede);
		}
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		String errore = controllaIva();
		if (errore != null) return errore;
		Anagrafica tabella = new Anagrafica();
		ContentValues val = tabella.getValoriLogModifica(db);
		valoriComuni(val);
		ContentValues where = new ContentValues();
		where.put(Anagrafica.ID_ANAGRAFICA, getIDModifica());
		tabella.aggiornaRecord(db, val, where);
		return super.eseguiAggiornamento(db);
	}

	@Override
	protected String moduloFunzionalita() {
		return pfa.app.econtab.utils.FunzionalitaApp.CLIENTI;
	}
}
