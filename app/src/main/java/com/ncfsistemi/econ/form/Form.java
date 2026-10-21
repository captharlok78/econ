package com.ncfsistemi.econ.form;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.SezioneStandard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Le sezioni e i campi di una form del template (STUDIO_FORM_APP.md §3). La DefinizioneForm li descrive con sezione(...);
 * EconFormStandardActivity chiama disegna(), poi carica() col record e leggi() al salvataggio.
 */
public class Form {

	/** Sopra questa larghezza (dp), e solo in orizzontale, le sezioni vanno su due colonne (tablet). */
	private static final int LARGHEZZA_DUE_COLONNE = 720;
	/** Larghezza massima (dp) di una form a una colonna (una sola sezione su tablet). */
	private static final int LARGHEZZA_UNA_COLONNA = 760;
	/** Sotto questa larghezza di colonna (dp) le righe di campi diversi si spezzano. */
	private static final int LARGHEZZA_STRETTA = 480;

	private final List<Sezione> sezioni = new ArrayList<>();
	private Activity activity;
	private boolean nuovo;
	private Map<String, String> istantanea = new HashMap<>();
	/** Colonna piu' stretta di LARGHEZZA_STRETTA (smartphone, tablet verticale a due colonne). */
	private boolean stretta;
	/** Scheda: stessa definizione in sola visualizzazione (etichetta e valore, niente campi da compilare). */
	private boolean scheda;
	/** Valori messi dal programma (carica, imposta): non sono cambi dell'utente. */
	private boolean caricando;
	/** Campo di cui si sta creando un nuovo record ("+"): al ritorno si sceglie quello appena creato. */
	private String colonnaInCreazione;

	/** Richiesta (startActivityForResult) del "+ nuovo" di una scelta o ricerca. */
	public static final int RICHIESTA_CREA = 7301;

	/** Chi vuole sapere quando l'utente cambia un campo (DefinizioneForm.alCambio). */
	public interface AlCambio {
		void cambiato(String colonna);
	}

	AlCambio alCambio;

	public Sezione sezione(String icona, String titolo) {
		Sezione s = new Sezione(icona, titolo);
		sezioni.add(s);
		return s;
	}

	/** Il campo di quella colonna (null se la form non ce l'ha). */
	public Campo campo(String colonna) {
		for (Campo c : campi()) {
			if (c.colonna.equals(colonna)) return c;
		}
		return null;
	}

	/** Valore attuale del campo di quella colonna ("" se non c'e'). */
	public String valore(String colonna) {
		Campo c = campo(colonna);
		return c == null ? "" : c.valore();
	}

	/** Imposta il valore di un campo (es. da un extra, in DefinizioneForm.dopoCaricamento). */
	public void imposta(String colonna, String valore) {
		Campo c = campo(colonna);
		if (c == null) return;
		caricando = true;
		c.imposta(valore);
		aggiornaCollegate();
		aggiornaVisibilita();
		caricando = false;
	}

	/** Scheda: azione a icona nella testata della sezione (es. la matita della modifica sulla prima). */
	public void azione(int sezione, String icona, String testo, View.OnClickListener azione) {
		Sezione s = sezioni.get(sezione);
		if (s.testata != null) SezioneStandard.azione(s.testata, icona, testo, azione);
	}

	/** Scheda: un dato in piu' in fondo alla sezione, calcolato dal modulo (es. la durata del cantiere). */
	public void aggiungiDato(int sezione, String etichetta, String valore) {
		Sezione s = sezioni.get(sezione);
		if (s.contenitoreCampi == null) return;
		View v = LayoutInflater.from(activity).inflate(R.layout.form_campo_dato, s.contenitoreCampi, false);
		((TextView) v.findViewById(R.id.form_campo_etichetta)).setText(etichetta);
		((TextView) v.findViewById(R.id.form_campo_valore)).setText(valore == null || valore.isEmpty() ? "—" : valore);
		s.contenitoreCampi.addView(v);
	}

	public boolean isScheda() {
		return scheda;
	}

	/** Nuove voci di una scelta da elenco fisso (es. gli stati, diversi per preventivo e ordine). */
	public void voci(String colonna, String[][] voci) {
		Campo c = campo(colonna);
		if (c == null || c.tipo != Campo.Tipo.SCELTA) return;
		c.voci = voci;
		c.caricaScelte(activity);
	}

	/** Il campo non si modifica piu' (resta visibile, col lucchetto). */
	public void blocca(String colonna) {
		Campo c = campo(colonna);
		if (c == null) return;
		c.bloccaInLettura(activity);
		c.bloccato = true;
		if (c.box != null) c.box.setStartIconDrawable(FaIcone.drawable(activity, FaIcone.LUCCHETTO, 18));
	}

	/** Il campo non si vede (e non si controlla). */
	public void nascondi(String colonna) {
		Campo c = campo(colonna);
		if (c == null) return;
		c.nascosto = true;
		c.vista.setVisibility(View.GONE);
	}

	/** Errore sotto il campo di quella colonna, da DefinizioneForm.controlla(). */
	public void errore(String colonna, String messaggio) {
		Campo c = campo(colonna);
		if (c != null) c.errore(messaggio);
	}

	List<Campo> campi() {
		List<Campo> tutti = new ArrayList<>();
		for (Sezione s : sezioni) tutti.addAll(s.campi);
		return tutti;
	}

	// ── disegno ──────────────────────────────────────────────────────────────────────────────────────────────────────

	/**
	 * Scheda in sola visualizzazione dalla stessa definizione (STUDIO_FORM_APP.md §7.6): sezioni e righe come nella form,
	 * ogni campo come etichetta e valore; si riempie con carica(record). Niente campi d'appoggio ne' del solo nuovo.
	 */
	public void disegnaScheda(Activity activity, LinearLayout contenitore) {
		scheda = true;
		contenitore.removeAllViews();
		disegna(activity, contenitore, false);
	}

	void disegna(Activity activity, LinearLayout contenitore, boolean nuovo) {
		this.activity = activity;
		this.nuovo = nuovo;
		LayoutInflater inflater = LayoutInflater.from(activity);
		List<LinearLayout> colonne = new ArrayList<>();
		// due colonne solo in orizzontale (21/10: tablet in verticale a una colonna, piu' comodo)
		android.content.res.Configuration conf = activity.getResources().getConfiguration();
		if (conf.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE && conf.screenWidthDp >= LARGHEZZA_DUE_COLONNE
				&& sezioni.size() > 1) {
			contenitore.setOrientation(LinearLayout.HORIZONTAL);
			for (int i = 0; i < 2; i++) {
				LinearLayout colonna = new LinearLayout(activity);
				colonna.setOrientation(LinearLayout.VERTICAL);
				contenitore.addView(colonna, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
				colonne.add(colonna);
			}
		} else {
			colonne.add(contenitore);
			// una colonna su uno schermo largo: la form resta larga al massimo LARGHEZZA_UNA_COLONNA, al centro
			int larghezza = activity.getResources().getConfiguration().screenWidthDp;
			if (larghezza > LARGHEZZA_UNA_COLONNA) {
				int margine = Math.round((larghezza - LARGHEZZA_UNA_COLONNA) / 2f * activity.getResources().getDisplayMetrics().density);
				contenitore.setPadding(margine, contenitore.getPaddingTop(), margine, contenitore.getPaddingBottom());
			}
		}
		stretta = activity.getResources().getConfiguration().screenWidthDp / colonne.size() < LARGHEZZA_STRETTA;
		int[] peso = new int[colonne.size()];
		for (Sezione s : sezioni) {
			// la sezione va nella colonna meno piena
			int dove = 0;
			for (int i = 1; i < peso.length; i++) {
				if (peso[i] < peso[dove]) dove = i;
			}
			peso[dove] += s.personalizzata != null ? 6 : s.campi.size() + 1;
			disegnaSezione(inflater, colonne.get(dove), s);
		}
		aggiornaCollegate();
		impostaTastiera();
		aggiornaVisibilita();
	}

	private void disegnaSezione(LayoutInflater inflater, LinearLayout colonna, Sezione s) {
		View card = inflater.inflate(R.layout.form_sezione, colonna, false);
		colonna.addView(card);
		s.vista = card;
		View testata = card.findViewById(R.id.form_sezione_testata);
		SezioneStandard.imposta(testata, s.icona, s.titolo);
		s.testata = testata;
		((ViewGroup.MarginLayoutParams) testata.getLayoutParams()).topMargin = 0;
		LinearLayout campi = card.findViewById(R.id.form_sezione_campi);
		s.contenitoreCampi = campi;
		if (s.personalizzata != null) {
			s.personalizzata.disegna(campi);
			return;
		}
		boolean qualcuno = false;
		for (int i = 0; i < s.campi.size(); i++) {
			Campo c = s.campi.get(i);
			qualcuno |= !nascostoQui(c);
			// campi metaRiga() consecutivi (al massimo 3) sulla stessa riga
			int fine = i;
			while (c.metaRiga && fine + 1 < s.campi.size() && s.campi.get(fine + 1).metaRiga && !s.campi.get(fine + 1).nuovaRiga
					&& fine - i < 2) {
				fine++;
			}
			if (fine > i && stretta) {
				// colonna stretta (smartphone): affiancati solo due campi dello stesso peso, gli altri uno per riga
				boolean uguali = fine == i + 1 && s.campi.get(i).peso == s.campi.get(fine).peso;
				if (!uguali) fine = i;
			}
			if (fine > i) {
				LinearLayout riga = new LinearLayout(activity);
				riga.setOrientation(LinearLayout.HORIZONTAL);
				campi.addView(riga, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				int spazio = Math.round(10 * activity.getResources().getDisplayMetrics().density);
				for (int j = i; j <= fine; j++) {
					aggiungiInRiga(inflater, riga, s.campi.get(j), j == i ? 0 : spazio / 2, j == fine ? 0 : spazio / 2);
				}
				i = fine;
			} else {
				campi.addView(creaVista(inflater, campi, c));
			}
		}
		// sezione fatta solo di campi del nuovo record: in modifica non si vede
		if (!qualcuno) card.setVisibility(View.GONE);
	}

	private void aggiungiInRiga(LayoutInflater inflater, LinearLayout riga, Campo c, int sinistra, int destra) {
		View v = creaVista(inflater, riga, c);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, c.peso);
		lp.topMargin = ((ViewGroup.MarginLayoutParams) v.getLayoutParams()).topMargin;
		lp.setMarginStart(sinistra);
		lp.setMarginEnd(destra);
		riga.addView(v, lp);
	}

	private View creaVista(LayoutInflater inflater, ViewGroup padre, Campo c) {
		if (scheda) return creaDato(inflater, padre, c);
		String etichetta = c.etichetta + (c.obbligatorio ? " *" : "");
		switch (c.tipo) {
			case SI_NO:
				c.vista = inflater.inflate(R.layout.form_campo_sino, padre, false);
				c.interruttore = (com.google.android.material.materialswitch.MaterialSwitch) c.vista;
				c.interruttore.setOnCheckedChangeListener((b, si) -> cambiato(c));
				if (c.aiuto == null) {
					c.interruttore.setText(etichetta);
				} else {
					// aiuto sotto l'etichetta, piccolo e grigio
					android.text.SpannableStringBuilder t = new android.text.SpannableStringBuilder(etichetta + "\n" + c.aiuto);
					int da = etichetta.length() + 1;
					t.setSpan(new android.text.style.RelativeSizeSpan(0.8f), da, t.length(), 0);
					t.setSpan(new android.text.style.ForegroundColorSpan(androidx.core.content.ContextCompat.getColor(activity, R.color.econ_testo_secondario)), da, t.length(), 0);
					c.interruttore.setText(t);
				}
				break;
			case OPZIONI:
				c.vista = inflater.inflate(R.layout.form_campo_opzioni, padre, false);
				((TextView) c.vista.findViewById(R.id.form_campo_etichetta)).setText(etichetta);
				c.gruppo = c.vista.findViewById(R.id.form_campo_gruppo);
				c.erroreOpzioni = c.vista.findViewById(R.id.form_campo_errore);
				c.creaOpzioni(activity);
				c.gruppo.addOnButtonCheckedListener((g, id, scelto) -> {
					if (scelto) cambiato(c);
				});
				break;
			case SCELTA:
				c.vista = inflater.inflate(R.layout.form_campo_scelta, padre, false);
				c.box = (TextInputLayout) c.vista;
				c.testo = c.vista.findViewById(R.id.form_campo_testo);
				c.box.setHint(etichetta);
				c.caricaScelte(activity);
				((MaterialAutoCompleteTextView) c.testo).setOnItemClickListener((lista, v, posizione, id) -> {
					if (c.codici.get(posizione) == null) {
						c.imposta(c.valore());
						creaNuovo(c);
						return;
					}
					c.scegli(posizione);
					cambiato(c);
				});
				break;
			case RICERCA:
				c.vista = inflater.inflate(R.layout.form_campo_ricerca, padre, false);
				c.box = (TextInputLayout) c.vista;
				c.testo = c.vista.findViewById(R.id.form_campo_testo);
				c.box.setHint(etichetta);
				c.configuraRicerca();
				c.caricaScelte(activity);
				((MaterialAutoCompleteTextView) c.testo).setOnItemClickListener((lista, v, posizione, id) -> {
					c.scegli((Campo.Voce) lista.getItemAtPosition(posizione));
					cambiato(c);
				});
				if (c.tabella != null && c.tabella.isCreabileDaApp() && c.tabella.getDettaglioActivity() != null) {
					c.box.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
					c.box.setEndIconDrawable(FaIcone.drawable(activity, FaIcone.PIU, 18));
					c.box.setEndIconContentDescription("Nuovo");
					c.box.setEndIconOnClickListener(v -> creaNuovo(c));
				}
				break;
			case DATA:
				c.vista = inflater.inflate(R.layout.form_campo_testo, padre, false);
				c.box = (TextInputLayout) c.vista;
				c.testo = c.vista.findViewById(R.id.form_campo_testo);
				c.box.setHint(etichetta);
				c.cambiato = () -> cambiato(c);
				c.configuraData((androidx.fragment.app.FragmentActivity) activity);
				break;
			default:
				c.vista = inflater.inflate(R.layout.form_campo_testo, padre, false);
				c.box = (TextInputLayout) c.vista;
				c.testo = c.vista.findViewById(R.id.form_campo_testo);
				c.box.setHint(etichetta);
				c.configuraTesto();
		}
		if (c.box != null) {
			if (c.aiuto != null) c.box.setHelperText(c.aiuto);
			boolean bloccato = c.soloLettura || (c.fissoInModifica && !nuovo);
			String icona = bloccato ? FaIcone.LUCCHETTO : c.icona;
			if (icona != null) c.box.setStartIconDrawable(FaIcone.drawable(activity, icona, 18));
		}
		if (c.soloLettura || (c.fissoInModifica && !nuovo)) c.bloccaInLettura(activity);
		if (nascostoQui(c)) c.vista.setVisibility(View.GONE);
		return c.vista;
	}

	/** Campo che in questa modalita' non c'e' (soloInserimento in modifica, soloModifica nel nuovo). */
	private boolean nascostoQui(Campo c) {
		return (c.soloInserimento && !nuovo) || (c.soloModifica && nuovo);
	}

	/** Scheda: etichetta piccola e valore (telefono ed email si toccano per chiamare o scrivere). */
	private View creaDato(LayoutInflater inflater, ViewGroup padre, Campo c) {
		c.vista = inflater.inflate(R.layout.form_campo_dato, padre, false);
		((TextView) c.vista.findViewById(R.id.form_campo_etichetta)).setText(c.etichetta);
		c.dato = c.vista.findViewById(R.id.form_campo_valore);
		if (c.tipo == Campo.Tipo.TELEFONO) c.dato.setAutoLinkMask(android.text.util.Linkify.PHONE_NUMBERS);
		if (c.tipo == Campo.Tipo.EMAIL) c.dato.setAutoLinkMask(android.text.util.Linkify.EMAIL_ADDRESSES);
		if (c.tipo == Campo.Tipo.SCELTA || c.tipo == Campo.Tipo.RICERCA) c.caricaScelte(activity);
		if (c.appoggio || c.soloInserimento) c.vista.setVisibility(View.GONE);
		return c.vista;
	}

	/** L'utente ha cambiato un campo: scelte collegate, campi visibileSe e l'aggancio della definizione. */
	private void cambiato(Campo c) {
		if (caricando) return;
		aggiornaCollegate();
		aggiornaVisibilita();
		if (alCambio != null) alCambio.cambiato(c.colonna);
	}

	/** "+ nuovo" di una scelta o ricerca: la pagina di inserimento di quella tabella (se la licenza lo consente). */
	private void creaNuovo(Campo c) {
		colonnaInCreazione = c.colonna;
		Intent intent = new Intent(activity, c.tabella.getDettaglioActivity());
		if (activity instanceof com.ncfsistemi.econ.EconActivity) {
			((com.ncfsistemi.econ.EconActivity) activity).apriFinestraInserimento(intent, RICHIESTA_CREA, c.tabella);
		} else {
			activity.startActivityForResult(intent, RICHIESTA_CREA);
		}
	}

	String getColonnaInCreazione() {
		return colonnaInCreazione;
	}

	void setColonnaInCreazione(String colonna) {
		colonnaInCreazione = colonna;
	}

	/** Ritorno dal "+ nuovo": voci ricaricate e, se e' stato salvato, scelto il record appena creato. */
	void creato(boolean salvato) {
		Campo c = colonnaInCreazione == null ? null : campo(colonnaInCreazione);
		colonnaInCreazione = null;
		if (c == null) return;
		c.caricaScelte(activity);
		if (!salvato || !c.tabella.getCampoCodicePerSpinner().equals(c.tabella.getCampoNumeratore())) return;
		// i record nuovi del dispositivo hanno l'id negativo: l'ultimo creato e' il piu' basso
		com.ncfsistemi.econ.db.DbInterno db = new com.ncfsistemi.econ.db.DbInterno(activity);
		int ultimo = db.getUltimoId(c.tabella);
		db.close();
		if (!c.codici.contains(String.valueOf(ultimo))) return;
		c.imposta(String.valueOf(ultimo));
		c.errore(null);
		cambiato(c);
	}

	/** Le scelte collegate (collegataA) si filtrano col valore attuale del campo da cui dipendono. */
	void aggiornaCollegate() {
		if (scheda) return;
		for (Campo c : campi()) {
			if (c.collegataA == null) continue;
			Campo origine = campo(c.collegataA);
			if (origine == null) continue;
			String valore = origine.valore();
			ContentValues filtro = null;
			if (!valore.isEmpty()) {
				filtro = new ContentValues();
				filtro.put(origine.tabella != null ? origine.tabella.getCampoCodicePerSpinner() : origine.colonna, valore);
			}
			if (filtro == null ? c.filtro == null : filtro.equals(c.filtro)) continue;
			c.filtro = filtro;
			c.caricaScelte(activity);
			if (!c.bloccato && !c.soloLettura) c.box.setEnabled(filtro != null);
		}
	}

	void aggiornaVisibilita() {
		for (Campo c : campi()) {
			if (c.visibileSeColonna == null || c.nascosto || nascostoQui(c) || (scheda && (c.appoggio || c.soloInserimento))) continue;
			Campo origine = campo(c.visibileSeColonna);
			boolean vedi = origine == null || c.visibileSeValore.equals(origine.valore());
			c.vista.setVisibility(vedi ? View.VISIBLE : View.GONE);
		}
	}

	/** "Avanti" da un campo all'altro, "Fine" sull'ultimo. */
	private void impostaTastiera() {
		TextView ultimo = null;
		for (Campo c : campi()) {
			if (c.testo == null || c.tipo == Campo.Tipo.SCELTA || c.tipo == Campo.Tipo.DATA || c.tipo == Campo.Tipo.NOTE) continue;
			c.testo.setImeOptions(EditorInfo.IME_ACTION_NEXT);
			ultimo = c.testo;
		}
		if (ultimo != null) ultimo.setImeOptions(EditorInfo.IME_ACTION_DONE);
	}

	// ── dati ─────────────────────────────────────────────────────────────────────────────────────────────────────────

	/** Riempie i campi dal record (null = nuovo record: valori predefiniti). */
	public void carica(ContentValues record) {
		caricando = true;
		for (Campo c : campi()) {
			c.imposta(record == null ? c.predefinito : record.getAsString(c.colonna));
		}
		aggiornaCollegate();
		aggiornaVisibilita();
		caricando = false;
	}

	void carica(Map<String, String> valori) {
		caricando = true;
		for (Campo c : campi()) {
			if (valori.containsKey(c.colonna)) c.imposta(valori.get(c.colonna));
			aggiornaCollegate();
		}
		aggiornaVisibilita();
		caricando = false;
	}

	/** Valori dei campi da scrivere: niente campi in sola lettura; un campo nascosto da visibileSe vale vuoto. */
	void leggi(ContentValues val) {
		for (Campo c : campi()) {
			if (c.appoggio || c.bloccato || (c.soloLettura && !nuovo) || (c.fissoInModifica && !nuovo) || nascostoQui(c)) continue;
			if (c.visibileSeColonna != null && !c.isVisibile()) {
				if (c.tipo == Campo.Tipo.SI_NO) val.put(c.colonna, 0);
				else val.put(c.colonna, "");
				continue;
			}
			c.leggi(val);
		}
	}

	/** Controlli dei campi visibili; il primo campo sbagliato o null. */
	Campo controlla() {
		Campo primo = null;
		for (Campo c : campi()) {
			if (!c.isVisibile() || c.soloLettura || c.bloccato || (c.fissoInModifica && !nuovo)) continue;
			String errore = c.controlla();
			c.errore(errore);
			if (errore != null && primo == null) primo = c;
		}
		return primo;
	}

	/** Il primo campo con un errore (anche messo dalla definizione) o null. */
	Campo primoErrore() {
		for (Campo c : campi()) {
			if (c.isVisibile() && c.haErrore()) return c;
		}
		return null;
	}

	Map<String, String> valori() {
		Map<String, String> m = new HashMap<>();
		for (Campo c : campi()) m.put(c.colonna, c.valore());
		return m;
	}

	/** Da qui si misurano le modifiche non salvate. */
	void fotografa() {
		istantanea = valori();
	}

	boolean modificato() {
		return !istantanea.equals(valori());
	}

	void ricaricaScelte() {
		for (Campo c : campi()) {
			if (c.tipo == Campo.Tipo.SCELTA || c.tipo == Campo.Tipo.RICERCA) c.caricaScelte(activity);
		}
	}

	void disattivaTutto() {
		for (Campo c : campi()) c.disattiva();
	}
}
