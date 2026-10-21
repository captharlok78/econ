package com.ncfsistemi.econ.form;

import android.content.ContentValues;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import com.ncfsistemi.econ.EconDettaglioActivity;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.utils.Utility;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Pagina di una form del template (STUDIO_FORM_APP.md §3): il modulo da' solo la sua DefinizioneForm, qui si disegna
 * (layout form_standard), si carica il record (extra "ID", numero o testo, sulla prima chiave della tabella), si
 * controlla con l'errore sul campo, si salva (transazione di EconDettaglioActivity) e si chiede conferma prima di
 * uscire con modifiche non salvate. Le maschere non ancora rifatte restano su EconDettaglioActivity.
 */
public abstract class EconFormStandardActivity extends EconDettaglioActivity {

	private static final String STATO_VALORI = "form_valori";
	private static final String STATO_CREAZIONE = "form_creazione";

	private DefinizioneForm definizione;
	private Form form;
	private ContentValues record;
	private boolean ricaricaScelte = false;

	protected abstract DefinizioneForm creaDefinizione();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		definizione = creaDefinizione();
		definizione.ospite = this;
		definizione.contesto = this;
		form = new Form();
		form.alCambio = colonna -> definizione.alCambio(form, colonna);
		definizione.sezioni(form);
		setContentView(R.layout.form_standard);
		boolean nuovo = getIDModifica() == 0 && getIDModificaStringa() == null;
		form.disegna(this, findViewById(R.id.form_contenitore), nuovo);
		// modalita' e inizializzaModifica() (che legge il record) li gestisce il padre
		super.onCreate(savedInstanceState);
		if (getModalita() == INSERIMENTO) form.carica((ContentValues) null);
		definizione.dopoCaricamento(form, record, getModalita() == INSERIMENTO);
		impostaSottotitolo();
		if (savedInstanceState != null) form.setColonnaInCreazione(savedInstanceState.getString(STATO_CREAZIONE));
		if (savedInstanceState != null && savedInstanceState.getSerializable(STATO_VALORI) != null) {
			@SuppressWarnings("unchecked")
			Map<String, String> valori = (Map<String, String>) savedInstanceState.getSerializable(STATO_VALORI);
			form.fotografa();
			form.carica(valori);
		} else {
			form.fotografa();
		}
		getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				annulla(null);
			}
		});
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (isSolaLettura()) form.disattivaTutto();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(STATO_VALORI, (Serializable) new HashMap<>(form.valori()));
		outState.putString(STATO_CREAZIONE, form.getColonnaInCreazione());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == Form.RICHIESTA_CREA) form.creato(resultCode == RESULT_OK);
	}

	@Override
	protected void onPause() {
		super.onPause();
		ricaricaScelte = true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		// di ritorno da "+ Crea nuovo" di una scelta: le voci possono essere cambiate
		if (ricaricaScelte) {
			ricaricaScelte = false;
			form.ricaricaScelte();
		}
	}

	@Override
	protected String getTitoloDettaglio() {
		return definizione.titolo(getModalita() == INSERIMENTO);
	}

	@Override
	public String getMessaggioConfermaSalvataggio() {
		return definizione.messaggioConferma(form, getModalita() == INSERIMENTO);
	}

	@Override
	protected String moduloFunzionalita() {
		return definizione.moduloFunzionalita();
	}

	private void impostaSottotitolo() {
		TextView sotto = findViewById(R.id.headerSottotitolo);
		String testo = record == null ? null : definizione.sottotitolo(record);
		if (sotto != null && testo != null) {
			sotto.setText(testo);
			sotto.setVisibility(View.VISIBLE);
		}
	}

	/** Filtro sulla prima chiave della tabella con l'extra "ID" (numero o testo). */
	private ContentValues chiave(AbstractTable tabella) {
		ContentValues where = new ContentValues();
		String colonna = tabella.getCampiChiave().get(0);
		if (getIDModifica() != 0) {
			where.put(colonna, getIDModifica());
		} else {
			where.put(colonna, getIDModificaStringa());
		}
		return where;
	}

	@Override
	protected void inizializzaModifica() {
		DbInterno db = new DbInterno(this);
		record = db.getRecord(definizione.tabella(), chiave(definizione.tabella()));
		db.close();
		form.carica(record);
	}

	/** Nuovo record (non modifica). */
	public boolean isNuovo() {
		return getModalita() == INSERIMENTO;
	}

	public Form getForm() {
		return form;
	}

	/** Record letto in modifica (null nel nuovo). */
	public ContentValues getRecord() {
		return record;
	}

	@Override
	public void salva(View v) {
		boolean nuovo = getModalita() == INSERIMENTO;
		Campo sbagliato = form.controlla();
		if (sbagliato == null) {
			definizione.controlla(form, nuovo);
			sbagliato = form.primoErrore();
		}
		if (sbagliato != null) {
			mostra(sbagliato);
			return;
		}
		super.salva(v);
	}

	/** Porta il campo sbagliato in vista. */
	private void mostra(Campo c) {
		ScrollView scroll = findViewById(R.id.scroll);
		int[] posCampo = new int[2], posScroll = new int[2];
		c.vista.getLocationInWindow(posCampo);
		scroll.getLocationInWindow(posScroll);
		scroll.smoothScrollBy(0, posCampo[1] - posScroll[1] - 24);
		if (c.testo != null && c.tipo != Campo.Tipo.SCELTA) c.testo.requestFocus();
	}

	@Override
	public void annulla(View v) {
		if (!isSolaLettura() && form.modificato()) {
			Utility.mostraConfermaDialog(getString(R.string.attenzione), "Uscire senza salvare le modifiche?", this, "Esci",
					getString(R.string.annulla), (dialog, quale) -> {
						if (quale == android.content.DialogInterface.BUTTON_POSITIVE) EconFormStandardActivity.super.annulla(null);
					});
			return;
		}
		super.annulla(v);
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		AbstractTable tabella = definizione.tabella();
		ContentValues val = tabella.getValoriLogInserimento(db);
		form.leggi(val);
		definizione.primaDiSalvare(val, true);
		if (definizione.inserisci(db, tabella, val) == -1) {
			return getString(R.string.errore_inserimento);
		}
		definizione.dopoSalvataggio(db, val, true);
		return SALVATAGGIO_OK;
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		AbstractTable tabella = definizione.tabella();
		ContentValues val = tabella.getValoriLogModifica(db);
		form.leggi(val);
		definizione.primaDiSalvare(val, false);
		tabella.aggiornaRecord(db, val, chiave(tabella));
		definizione.dopoSalvataggio(db, val, false);
		return SALVATAGGIO_OK;
	}
}
