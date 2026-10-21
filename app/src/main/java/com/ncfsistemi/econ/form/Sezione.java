package com.ncfsistemi.econ.form;

import android.widget.LinearLayout;

import com.ncfsistemi.econ.db.table.AbstractTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Sezione del template form: icona e titolo (testata standard) e i suoi campi. I metodi che aggiungono un campo
 * restituiscono la sezione; quelli che seguono (obbligatorio(), max(...), ...) valgono per l'ultimo campo aggiunto:
 *
 * <pre>
 * f.sezione(FaIcone.CLIENTE, "Dati principali")
 *     .testo(Tabella.NOME, "Nome").obbligatorio().max(100)
 *     .telefono(Tabella.TELEFONO, "Telefono").metaRiga()
 * </pre>
 */
public class Sezione {

	/** Contenuto disegnato dal modulo (righe, tabelle, firme) al posto dei campi. */
	public interface Personalizzata {
		void disegna(LinearLayout contenitore);
	}

	final String icona;
	final String titolo;
	final List<Campo> campi = new ArrayList<>();
	Personalizzata personalizzata;
	/** Testata e card disegnate (per nascondere la sezione). */
	android.view.View vista;
	android.view.View testata;
	android.widget.LinearLayout contenitoreCampi;
	private Campo ultimo;

	Sezione(String icona, String titolo) {
		this.icona = icona;
		this.titolo = titolo;
	}

	private Sezione aggiungi(Campo.Tipo tipo, String colonna, String etichetta) {
		ultimo = new Campo(tipo, colonna, etichetta);
		campi.add(ultimo);
		return this;
	}

	public Sezione testo(String colonna, String etichetta) {
		return aggiungi(Campo.Tipo.TESTO, colonna, etichetta);
	}

	public Sezione note(String colonna, String etichetta) {
		return aggiungi(Campo.Tipo.NOTE, colonna, etichetta);
	}

	public Sezione numero(String colonna, String etichetta) {
		return aggiungi(Campo.Tipo.NUMERO, colonna, etichetta);
	}

	public Sezione intero(String colonna, String etichetta) {
		return aggiungi(Campo.Tipo.INTERO, colonna, etichetta);
	}

	public Sezione email(String colonna, String etichetta) {
		aggiungi(Campo.Tipo.EMAIL, colonna, etichetta);
		ultimo.icona = com.ncfsistemi.econ.utils.FaIcone.EMAIL;
		return this;
	}

	public Sezione telefono(String colonna, String etichetta) {
		aggiungi(Campo.Tipo.TELEFONO, colonna, etichetta);
		ultimo.icona = com.ncfsistemi.econ.utils.FaIcone.TELEFONO;
		return this;
	}

	/** Scelta tra i record di una tabella (codice e descrizione come EconSpinner). */
	public Sezione scelta(String colonna, String etichetta, AbstractTable tabella) {
		aggiungi(Campo.Tipo.SCELTA, colonna, etichetta);
		ultimo.tabella = tabella;
		return this;
	}

	/** Scelta scrivendo: il menu filtra mentre si scrive (molte voci, es. i clienti); + in fondo crea un record nuovo. */
	public Sezione ricerca(String colonna, String etichetta, AbstractTable tabella) {
		aggiungi(Campo.Tipo.RICERCA, colonna, etichetta);
		ultimo.tabella = tabella;
		return this;
	}

	/** Scelta da un elenco fisso: {{valore, descrizione}, ...}. */
	public Sezione scelta(String colonna, String etichetta, String[][] voci) {
		aggiungi(Campo.Tipo.SCELTA, colonna, etichetta);
		ultimo.voci = voci;
		return this;
	}

	/**
	 * Scelta collegata a un'altra (es. la placca alla linea): le voci sono filtrate col valore di quella, come
	 * EconSpinner.setSpinnerCollegato; finche' quella e' vuota il campo non si sceglie.
	 */
	public Sezione collegataA(String colonna) {
		ultimo.collegataA = colonna;
		return this;
	}

	/** NUMERO mostrato sempre con questi decimali (es. 2 per i prezzi). */
	public Sezione decimali(int cifre) {
		ultimo.decimali = cifre;
		return this;
	}

	/** Data dal calendario (nel DB yyyyMMdd000000); predefinito(Campo.OGGI) = il giorno stesso. */
	public Sezione data(String colonna, String etichetta) {
		aggiungi(Campo.Tipo.DATA, colonna, etichetta);
		ultimo.icona = com.ncfsistemi.econ.utils.FaIcone.CALENDARIO;
		return this;
	}

	public Sezione siNo(String colonna, String etichetta) {
		return aggiungi(Campo.Tipo.SI_NO, colonna, etichetta);
	}

	/** Poche opzioni fisse a segmenti: {{valore, etichetta}, ...}. */
	public Sezione opzioni(String colonna, String etichetta, String[][] opzioni) {
		aggiungi(Campo.Tipo.OPZIONI, colonna, etichetta);
		ultimo.opzioni = opzioni;
		return this;
	}

	/** Contenuto della sezione disegnato dal modulo. */
	public Sezione personalizzata(Personalizzata disegno) {
		personalizzata = disegno;
		return this;
	}

	public Sezione obbligatorio() {
		ultimo.obbligatorio = true;
		return this;
	}

	public Sezione maiuscolo() {
		ultimo.maiuscolo = true;
		return this;
	}

	/** Non si modifica mai dal dispositivo (es. codice assegnato dal server). */
	public Sezione soloLettura() {
		ultimo.soloLettura = true;
		ultimo.icona = com.ncfsistemi.econ.utils.FaIcone.LUCCHETTO;
		return this;
	}

	/** Si scrive nel nuovo record, poi non si cambia piu' (es. una chiave). */
	public Sezione fissoInModifica() {
		ultimo.fissoInModifica = true;
		return this;
	}

	/** Il campo c'e' solo nel nuovo record. */
	public Sezione soloInserimento() {
		ultimo.soloInserimento = true;
		return this;
	}

	/** Il campo c'e' solo in modifica (es. un dato che arriva dal server dopo il primo invio). */
	public Sezione soloModifica() {
		ultimo.soloModifica = true;
		return this;
	}

	/** Spiegazione che si apre toccando il campo quando non si modifica (icona i in fondo al campo). */
	public Sezione avviso(String testo) {
		ultimo.avviso = testo;
		return this;
	}

	/** In riga: i campi metaRiga() consecutivi (al massimo 3) stanno sulla stessa riga, larghi in proporzione al peso. */
	public Sezione metaRiga() {
		ultimo.metaRiga = true;
		return this;
	}

	/** metaRiga() con un peso (default 1): es. CAP 1, Citta' 3, Prov. 1. */
	public Sezione metaRiga(float peso) {
		ultimo.metaRiga = true;
		ultimo.peso = peso;
		return this;
	}

	/** Questo campo comincia una riga nuova anche se il precedente e' metaRiga(). */
	public Sezione nuovaRiga() {
		ultimo.nuovaRiga = true;
		return this;
	}

	/** Campo d'appoggio: non e' una colonna da scrivere, la definizione lo legge con f.valore(...) (es. via e civico). */
	public Sezione appoggio() {
		ultimo.appoggio = true;
		return this;
	}

	public Sezione max(int caratteri) {
		ultimo.max = caratteri;
		return this;
	}

	public Sezione icona(String glifo) {
		ultimo.icona = glifo;
		return this;
	}

	/** Testo d'aiuto sotto il campo. */
	public Sezione aiuto(String testo) {
		ultimo.aiuto = testo;
		return this;
	}

	/** Valore del nuovo record. */
	public Sezione predefinito(String valore) {
		ultimo.predefinito = valore;
		return this;
	}

	/** Il campo si vede solo quando un altro campo ha quel valore. */
	public Sezione visibileSe(String colonna, String valore) {
		ultimo.visibileSeColonna = colonna;
		ultimo.visibileSeValore = valore;
		return this;
	}
}
