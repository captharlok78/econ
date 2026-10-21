package com.ncfsistemi.econ.form;

import android.content.ContentValues;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.AbstractTable;

/**
 * Una form del template (STUDIO_FORM_APP.md §3), una per maschera: tabella, titolo, sezioni e campi. Disegno,
 * caricamento, salvataggio, controlli di base, modifiche non salvate e sola lettura li fa EconFormStandardActivity;
 * qui resta solo quello che e' proprio del modulo, negli agganci (tutti facoltativi).
 */
public abstract class DefinizioneForm {

	/** L'activity che mostra la form: extra dell'intent, record, form (null nella scheda: li' c'e' solo contesto). */
	protected EconFormStandardActivity ospite;
	/** Per stringhe e risorse in sezioni(): c'e' sempre, anche nella scheda. */
	protected android.app.Activity contesto;

	public abstract AbstractTable tabella();

	/** Scheda in sola visualizzazione con queste sezioni (EconSchedaFragment): poi Form.carica(record). */
	public Form disegnaScheda(android.app.Activity activity, android.widget.LinearLayout contenitore) {
		contesto = activity;
		Form f = new Form();
		sezioni(f);
		f.disegnaScheda(activity, contenitore);
		return f;
	}

	/** "Nuovo X" / "Modifica X". */
	public abstract String titolo(boolean nuovo);

	public abstract void sezioni(Form f);

	/** Riga sotto il titolo, dal record caricato (null = niente). */
	public String sottotitolo(ContentValues record) {
		return null;
	}

	/** Funzionalita' CRUD del modulo (FunzionalitaApp.CLIENTI, ...): senza MODIFICA il record si consulta soltanto. */
	public String moduloFunzionalita() {
		return null;
	}

	/**
	 * Dopo il caricamento (record null nel nuovo): valori iniziali presi dagli extra o dal record padre (f.imposta), campi
	 * da bloccare o nascondere secondo il record (f.blocca, f.nascondi). Non conta come modifica.
	 */
	public void dopoCaricamento(Form f, ContentValues record, boolean nuovo) {
	}

	/** L'utente ha cambiato una scelta, una ricerca, un'opzione o un si'/no (es. proporre altri valori dal cliente scelto). */
	public void alCambio(Form f, String colonna) {
	}

	/** Domanda di conferma prima di salvare (null = si salva subito), es. un cambio che si ripercuote altrove. */
	public String messaggioConferma(Form f, boolean nuovo) {
		return null;
	}

	/** Controlli del modulo dopo quelli dei campi: f.errore(colonna, messaggio) sul campo sbagliato. */
	public void controlla(Form f, boolean nuovo) {
	}

	/** Valori in piu' prima della scrittura (es. il padre del record nuovo, preso dagli extra). */
	public void primaDiSalvare(ContentValues val, boolean nuovo) {
	}

	/** Scrittura del record nuovo (-1 = errore): da sovrascrivere quando insieme nascono altri record (es. il cantiere). */
	public long inserisci(DbInterno db, AbstractTable tabella, ContentValues val) {
		return tabella.inserisciRecord(db, val);
	}

	/** Dopo la scrittura, nella stessa transazione (es. record collegati). */
	public void dopoSalvataggio(DbInterno db, ContentValues val, boolean nuovo) {
	}
}
