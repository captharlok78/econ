package com.ncfsistemi.econ.form;

import android.content.ContentValues;
import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.AbstractTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Un campo del template form (STUDIO_FORM_APP.md §3): colonna della tabella, etichetta, tipo e regole. La definizione lo
 * descrive con i metodi di Sezione; Form lo disegna (crea la View), lo carica dal record e lo rilegge al salvataggio.
 */
public class Campo {

	public enum Tipo { TESTO, NOTE, NUMERO, INTERO, EMAIL, TELEFONO, SCELTA, RICERCA, DATA, SI_NO, OPZIONI }

	/** predefinito(OGGI) di un campo DATA: la data del giorno. */
	public static final String OGGI = "oggi";

	/** Voce di una RICERCA: il menu filtra sulla descrizione, la scelta da' il codice. */
	static class Voce {
		final String codice;
		final String descrizione;

		Voce(String codice, String descrizione) {
			this.codice = codice;
			this.descrizione = descrizione;
		}

		@Override
		public String toString() {
			return descrizione;
		}
	}

	final Tipo tipo;
	final String colonna;
	final String etichetta;

	boolean obbligatorio;
	boolean maiuscolo;
	boolean soloLettura;
	boolean fissoInModifica;
	boolean soloInserimento;
	boolean soloModifica;
	boolean metaRiga;
	float peso = 1f;
	boolean nuovaRiga;
	boolean appoggio;
	/** Bloccato o nascosto dalla definizione (Form.blocca / Form.nascondi). */
	boolean bloccato;
	boolean nascosto;
	int max;
	/** NUMERO: decimali fissi da mostrare (-1 = quanti servono). */
	int decimali = -1;
	/** SCELTA da elenco fisso ({valore, descrizione}) invece che da tabella. */
	String[][] voci;
	/** SCELTA collegata: colonna del campo da cui dipende, e filtro attuale. */
	String collegataA;
	ContentValues filtro;
	String icona;
	String aiuto;
	/** Spiegazione che si apre toccando un campo che non si modifica (es. codice assegnato dal server). */
	String avviso;
	String predefinito;
	AbstractTable tabella;
	String[][] opzioni;
	String visibileSeColonna;
	String visibileSeValore;

	/** View piu' esterna del campo (per nasconderlo), e i suoi pezzi secondo il tipo. */
	View vista;
	TextInputLayout box;
	EditText testo;
	MaterialSwitch interruttore;
	MaterialButtonToggleGroup gruppo;
	TextView erroreOpzioni;
	/** Scheda (sola visualizzazione): il valore come testo, al posto del campo. */
	TextView dato;
	private String valoreScheda = "";
	/** Messo in sola lettura da bloccaInLettura. */
	boolean inLettura;
	/** DATA: yyyyMMdd000000 come Utility.dataToNumber ("" = nessuna). */
	private String dataScelta = "";
	/** Avvisa la form di un cambio fatto dall'utente fuori dai listener dei widget (DATA dal calendario). */
	Runnable cambiato;
	/** RICERCA: testo messo dal programma (non cancella la scelta). */
	private boolean impostando;
	/** SCELTA: codici e descrizioni nell'ordine del menu; "" = nessuna scelta, null = crea nuovo. */
	final List<String> codici = new ArrayList<>();
	final List<String> descrizioni = new ArrayList<>();
	private String codiceScelto = "";

	Campo(Tipo tipo, String colonna, String etichetta) {
		this.tipo = tipo;
		this.colonna = colonna;
		this.etichetta = etichetta;
	}

	public String getColonna() {
		return colonna;
	}

	public Tipo getTipo() {
		return tipo;
	}

	/** Valore come lo scrive il DB ("" se vuoto): per SI_NO "1"/"0", per SCELTA il codice. */
	public String valore() {
		if (dato != null) return valoreScheda;
		switch (tipo) {
			case SI_NO:
				return interruttore.isChecked() ? "1" : "0";
			case OPZIONI: {
				int scelto = gruppo.getCheckedButtonId();
				for (int i = 0; i < gruppo.getChildCount(); i++) {
					if (gruppo.getChildAt(i).getId() == scelto) return opzioni[i][0];
				}
				return "";
			}
			case SCELTA:
			case RICERCA:
				return codiceScelto;
			case DATA:
				return dataScelta;
			case NUMERO:
				return testo.getText().toString().trim().replace(',', '.');
			default: {
				String s = testo.getText().toString().trim();
				return maiuscolo ? s.toUpperCase() : s;
			}
		}
	}

	public void imposta(String valore) {
		if (valore == null) valore = "";
		if (tipo == Tipo.DATA) valore = normalizzaData(valore);
		if (dato != null) {
			valoreScheda = valore;
			dato.setText(testoScheda(valore));
			return;
		}
		switch (tipo) {
			case SI_NO:
				interruttore.setChecked(!valore.isEmpty() && !"0".equals(valore));
				break;
			case OPZIONI:
				for (int i = 0; i < opzioni.length; i++) {
					if (opzioni[i][0].equals(valore)) gruppo.check(gruppo.getChildAt(i).getId());
				}
				break;
			case SCELTA:
			case RICERCA: {
				codiceScelto = valore;
				int i = codici.indexOf(valore);
				impostando = true;
				((MaterialAutoCompleteTextView) testo).setText(i >= 0 ? descrizioni.get(i) : "", false);
				impostando = false;
				break;
			}
			case NUMERO:
				testo.setText(numeroDaMostrare(valore, decimali));
				break;
			case DATA:
				dataScelta = valore;
				testo.setText(dataDaMostrare(valore));
				break;
			default:
				testo.setText(valore);
		}
	}

	/** Il valore come si legge nella scheda: Si'/No, descrizione della scelta, numero con la virgola; "—" se vuoto. */
	String testoScheda(String valore) {
		String s;
		switch (tipo) {
			case SI_NO:
				return !valore.isEmpty() && !"0".equals(valore) ? "Sì" : "No";
			case OPZIONI:
				s = valore;
				for (String[] o : opzioni) {
					if (o[0].equals(valore)) s = o[1];
				}
				break;
			case SCELTA:
			case RICERCA: {
				int i = codici.indexOf(valore);
				s = i >= 0 ? descrizioni.get(i) : ("0".equals(valore) ? "" : valore);
				break;
			}
			case NUMERO:
				s = numeroDaMostrare(valore, decimali);
				break;
			case DATA:
				s = dataDaMostrare(valore);
				break;
			default:
				s = valore.trim();
		}
		return s.isEmpty() ? "—" : s;
	}

	/** Data del DB (yyyyMMdd o yyyyMMddHHmmss, OGGI) come yyyyMMdd000000; "" se vuota o non valida. */
	static String normalizzaData(String valore) {
		if (OGGI.equals(valore)) return String.valueOf(com.ncfsistemi.econ.utils.Utility.dataToNumber(java.util.Calendar.getInstance()));
		String v = valore.trim();
		if (v.length() == 8 && !v.equals("00000000")) return v + "000000";
		if (v.length() == 14) return v.substring(0, 8) + "000000";
		return "";
	}

	/** yyyyMMdd000000 → gg/mm/aaaa. */
	static String dataDaMostrare(String valore) {
		return valore.length() < 8 ? "" : valore.substring(6, 8) + "/" + valore.substring(4, 6) + "/" + valore.substring(0, 4);
	}

	/** DATA: calendario Material al tocco (anche sull'icona); la data scelta si scrive come yyyyMMdd000000. */
	void configuraData(androidx.fragment.app.FragmentActivity activity) {
		testo.setKeyListener(null);
		testo.setFocusable(false);
		testo.setCursorVisible(false);
		View.OnClickListener apri = v -> {
			if (inLettura || !box.isEnabled()) return;
			com.google.android.material.datepicker.MaterialDatePicker.Builder<Long> b =
					com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker().setTitleText(etichetta);
			if (!dataScelta.isEmpty()) {
				java.util.Calendar utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
				utc.clear();
				utc.set(Integer.parseInt(dataScelta.substring(0, 4)), Integer.parseInt(dataScelta.substring(4, 6)) - 1,
						Integer.parseInt(dataScelta.substring(6, 8)));
				b.setSelection(utc.getTimeInMillis());
			}
			com.google.android.material.datepicker.MaterialDatePicker<Long> picker = b.build();
			picker.addOnPositiveButtonClickListener(millis -> {
				java.util.Calendar utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
				utc.setTimeInMillis(millis);
				imposta(String.format(java.util.Locale.ROOT, "%04d%02d%02d", utc.get(java.util.Calendar.YEAR),
						utc.get(java.util.Calendar.MONTH) + 1, utc.get(java.util.Calendar.DAY_OF_MONTH)));
				errore(null);
				if (cambiato != null) cambiato.run();
			});
			picker.show(activity.getSupportFragmentManager(), "data_" + colonna);
		};
		testo.setOnClickListener(apri);
	}

	/** "15.0" del DB → "15", "22.50" → "22,5": virgola italiana, niente zeri in coda. */
	static String numeroDaMostrare(String valore, int decimali) {
		if (valore.isEmpty()) return valore;
		try {
			java.math.BigDecimal n = new java.math.BigDecimal(valore.replace(',', '.'));
			String s = decimali >= 0 ? n.setScale(decimali, java.math.RoundingMode.HALF_UP).toPlainString()
					: n.stripTrailingZeros().toPlainString();
			return s.replace('.', ',');
		} catch (NumberFormatException e) {
			return valore;
		}
	}

	/** Errore sotto il campo (null lo toglie). */
	public void errore(String messaggio) {
		if (dato != null) return;
		if (tipo == Tipo.OPZIONI) {
			erroreOpzioni.setText(messaggio);
			erroreOpzioni.setVisibility(messaggio == null ? View.GONE : View.VISIBLE);
		} else if (box != null) {
			box.setError(messaggio);
		}
	}

	boolean haErrore() {
		if (dato != null) return false;
		if (tipo == Tipo.OPZIONI) return erroreOpzioni.getVisibility() == View.VISIBLE;
		return box != null && box.getError() != null;
	}

	boolean isVisibile() {
		return vista != null && vista.getVisibility() == View.VISIBLE;
	}

	/** Controlli di base del tipo; il messaggio d'errore o null. */
	String controlla() {
		String v = valore();
		if (obbligatorio && v.isEmpty()) {
			if (tipo == Tipo.RICERCA && !testo.getText().toString().trim().isEmpty()) return "Scegli una voce dell'elenco";
			return tipo == Tipo.SCELTA || tipo == Tipo.RICERCA || tipo == Tipo.OPZIONI ? "Scegli: " + etichetta : "Campo obbligatorio";
		}
		if (v.isEmpty()) {
			// ricerca scritta ma non scelta dall'elenco
			if (tipo == Tipo.RICERCA && !testo.getText().toString().trim().isEmpty()) return "Scegli una voce dell'elenco";
			return null;
		}
		switch (tipo) {
			case EMAIL:
				return Patterns.EMAIL_ADDRESS.matcher(v).matches() ? null : "Email non valida";
			case NUMERO:
				try {
					Double.parseDouble(v);
					return null;
				} catch (NumberFormatException e) {
					return "Numero non valido";
				}
			case INTERO:
				try {
					Long.parseLong(v);
					return null;
				} catch (NumberFormatException e) {
					return "Numero intero non valido";
				}
			default:
				return null;
		}
	}

	void leggi(ContentValues val) {
		String v = valore();
		if (tipo == Tipo.SI_NO) {
			val.put(colonna, "1".equals(v) ? 1 : 0);
		} else if (tipo == Tipo.NUMERO) {
			// come Utility.formatNumeroDB: vuoto = 0
			val.put(colonna, v.isEmpty() ? 0d : Double.parseDouble(v));
		} else if (tipo == Tipo.DATA) {
			val.put(colonna, v.isEmpty() ? 0L : Long.parseLong(v));
		} else {
			val.put(colonna, v);
		}
	}

	/** Tastiera, filtri e lunghezza secondo il tipo. */
	void configuraTesto() {
		int input;
		switch (tipo) {
			case NOTE:
				input = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
				testo.setMinLines(3);
				testo.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
				break;
			case NUMERO:
				input = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED;
				break;
			case INTERO:
				input = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
				break;
			case EMAIL:
				input = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
				break;
			case TELEFONO:
				input = InputType.TYPE_CLASS_PHONE;
				break;
			default:
				input = InputType.TYPE_CLASS_TEXT | (maiuscolo ? InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS : InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		}
		testo.setInputType(input);
		if (tipo == Tipo.NUMERO) {
			// virgola e punto (la tastiera italiana ha la virgola); valore() la porta a punto per il DB
			testo.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789-.,"));
			testo.setRawInputType(input);
		}
		if (tipo != Tipo.NOTE) testo.setMaxLines(1);
		List<InputFilter> filtri = new ArrayList<>();
		if (max > 0) {
			filtri.add(new InputFilter.LengthFilter(max));
			// contatore solo sulle note: sugli altri campi il limite basta (troppi numeri distraggono)
			if (tipo == Tipo.NOTE) {
				box.setCounterEnabled(true);
				box.setCounterMaxLength(max);
			}
		}
		if (maiuscolo) filtri.add(new InputFilter.AllCaps());
		testo.setFilters(filtri.toArray(new InputFilter[0]));
		// l'errore sparisce appena si corregge
		testo.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (box.getError() != null) box.setError(null);
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}

	/** SCELTA: voci dalla tabella, come EconSpinner (getSQLPerSpinner / formattaDescrizioneSpinner). */
	void caricaScelte(Context ctx) {
		codici.clear();
		descrizioni.clear();
		if (!obbligatorio && tipo == Tipo.SCELTA) {
			codici.add("");
			descrizioni.add("—");
		}
		if (voci != null) {
			for (String[] v : voci) {
				codici.add(v[0]);
				descrizioni.add(v[1]);
			}
		} else if (collegataA == null || filtro != null || dato != null) {
			DbInterno db = new DbInterno(ctx);
			ArrayList<Object> dati;
			if (tabella.getSQLPerSpinner(filtro).equals("")) {
				dati = db.eseguiSelect(tabella, filtro, new String[] { tabella.getCampoDescrizionePerSpinner() });
			} else {
				dati = db.eseguiSelect(tabella.getSQLPerSpinner(filtro), null);
			}
			db.close();
			for (Object o : dati) {
				ContentValues r = (ContentValues) o;
				codici.add(r.getAsString(tabella.getCampoCodicePerSpinner()));
				descrizioni.add(tabella.formattaDescrizioneSpinner(r));
			}
			// la ricerca crea il nuovo record con l'icona + in fondo al campo
			if (tipo == Tipo.SCELTA && dato == null && tabella.isCreabileDaApp() && tabella.getDettaglioActivity() != null) {
				codici.add(null);
				descrizioni.add("+ Crea nuovo");
			}
		}
		if (dato != null) {
			imposta(valoreScheda);
			return;
		}
		MaterialAutoCompleteTextView menu = (MaterialAutoCompleteTextView) testo;
		if (tipo == Tipo.RICERCA) {
			List<Voce> elenco = new ArrayList<>();
			for (int i = 0; i < codici.size(); i++) elenco.add(new Voce(codici.get(i), descrizioni.get(i)));
			menu.setAdapter(new ArrayAdapter<>(ctx, com.google.android.material.R.layout.m3_auto_complete_simple_item, elenco));
		} else {
			menu.setAdapter(new ArrayAdapter<>(ctx, com.google.android.material.R.layout.m3_auto_complete_simple_item, descrizioni));
		}
		// un valore che non c'e' piu' tra le voci (es. filtro cambiato) si toglie
		if (!codiceScelto.isEmpty() && !codici.contains(codiceScelto)) codiceScelto = "";
		imposta(codiceScelto);
	}

	void scegli(int posizione) {
		codiceScelto = codici.get(posizione) == null ? codiceScelto : codici.get(posizione);
		if (box.getError() != null) box.setError(null);
	}

	/** RICERCA: voce scelta dal menu filtrato. */
	void scegli(Voce voce) {
		codiceScelto = voce.codice;
		if (box.getError() != null) box.setError(null);
	}

	/** RICERCA: tastiera di testo, menu dal primo carattere; scrivere dopo una scelta la toglie. */
	void configuraRicerca() {
		MaterialAutoCompleteTextView menu = (MaterialAutoCompleteTextView) testo;
		menu.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		menu.setThreshold(1);
		menu.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (!impostando) codiceScelto = "";
				if (box.getError() != null) box.setError(null);
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}

	/** OPZIONI: un pulsante a segmenti per ogni opzione. */
	void creaOpzioni(Context ctx) {
		for (String[] o : opzioni) {
			MaterialButton b = new MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
			b.setId(View.generateViewId());
			b.setText(o[1]);
			b.setAllCaps(false);
			b.setCheckable(true);
			gruppo.addView(b, new MaterialButtonToggleGroup.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		}
		gruppo.addOnButtonCheckedListener((g, id, scelto) -> {
			if (scelto) errore(null);
		});
	}

	/**
	 * Campo che non si modifica ma si legge bene (soloLettura, fissoInModifica, Form.blocca): testo grigio su fondo grigio
	 * chiaro invece del campo spento; con avviso(...) toccandolo si apre la spiegazione. Le scelte e gli interruttori si
	 * spengono e basta.
	 */
	void bloccaInLettura(Context ctx) {
		if (testo == null || tipo == Tipo.SCELTA) {
			disattiva();
			return;
		}
		inLettura = true;
		testo.setKeyListener(null);
		testo.setFocusable(false);
		testo.setCursorVisible(false);
		if (tipo == Tipo.DATA) testo.setOnClickListener(null);
		if (tipo == Tipo.RICERCA) ((MaterialAutoCompleteTextView) testo).setAdapter(null);
		box.setEndIconMode(TextInputLayout.END_ICON_NONE);
		testo.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, com.ncfsistemi.econ.R.color.econ_testo_secondario));
		box.setBoxBackgroundColor(androidx.core.content.ContextCompat.getColor(ctx, com.ncfsistemi.econ.R.color.econ_campo_bloccato));
		if (avviso == null) return;
		View.OnClickListener mostra = v -> new com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
				.setTitle(etichetta).setMessage(avviso).setPositiveButton(android.R.string.ok, null).show();
		box.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
		box.setEndIconDrawable(com.ncfsistemi.econ.utils.FaIcone.drawable(ctx, com.ncfsistemi.econ.utils.FaIcone.INFO, 18));
		box.setEndIconContentDescription("Informazioni");
		box.setEndIconOnClickListener(mostra);
		testo.setOnClickListener(mostra);
	}

	void disattiva() {
		if (dato != null) return;
		switch (tipo) {
			case SI_NO:
				interruttore.setEnabled(false);
				break;
			case OPZIONI:
				for (int i = 0; i < gruppo.getChildCount(); i++) gruppo.getChildAt(i).setEnabled(false);
				break;
			default:
				box.setEnabled(false);
		}
	}
}
