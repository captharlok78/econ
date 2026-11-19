package com.ncfsistemi.econ.dialogo;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.utils.FaIcone;

/**
 * Tutti i modali dell'app con un solo stile (STUDIO_MODALI_APP.md §4).
 *
 * <ul>
 *   <li>{@link Builder}: al posto di MaterialAlertDialogBuilder ovunque. Disegna la testata (icona in un cerchio colorato,
 *   titolo, sottotitolo), mette i margini standard alla vista personalizzata e porta i campi vecchi (EditText, Spinner,
 *   etichette azzurre) allo stile dei campi del template form ({@link Stile}).</li>
 *   <li>Varianti pronte: {@link #avviso}, {@link #attenzione}, {@link #errore}, {@link #conferma}, {@link #eliminazione}
 *   (azione rossa), {@link #scelta} (foglio dal basso sullo smartphone), {@link #attesa} (rotellina o avanzamento).</li>
 *   <li>Campi per i modali con dati da scrivere: {@link #corpo}, {@link #campo}, {@link #password}, {@link #etichetta}.</li>
 *   <li>Finestre grandi (tutto schermo): {@link #testataFinestra}.</li>
 * </ul>
 * L'aspetto (colori, angoli, pulsanti) e' in themes_econ.xml (ThemeOverlay.Econ.Dialogo) e nei layout dialogo_*.xml.
 */
public final class EconDialogo {

	/** Colore dell'icona in testata. */
	public enum Tono { INFO, DOMANDA, ATTENZIONE, PERICOLO, OK }

	/** Sotto questa larghezza (dp) e' uno smartphone: scelte con il foglio dal basso, finestre a tutto schermo. */
	private static final int LARGHEZZA_TABLET = 600;

	private EconDialogo() {
	}

	// ── varianti ─────────────────────────────────────────────────────────────────────────────────────────────────────

	public static Builder nuovo(Context ctx) {
		return new Builder(ctx);
	}

	/** Informazione: icona blu, solo OK. */
	public static Builder avviso(Context ctx, CharSequence titolo, CharSequence testo) {
		return testo(new Builder(ctx).icona(FaIcone.INFO, Tono.INFO), titolo, testo).setPositiveButton("OK", null);
	}

	/** Attenzione: icona arancio, solo OK. */
	public static Builder attenzione(Context ctx, CharSequence titolo, CharSequence testo) {
		return testo(new Builder(ctx).icona(FaIcone.ATTENZIONE, Tono.ATTENZIONE), titolo, testo).setPositiveButton("OK", null);
	}

	/** Errore: icona rossa, solo OK. */
	public static Builder errore(Context ctx, CharSequence titolo, CharSequence testo) {
		return testo(new Builder(ctx).icona(FaIcone.ERRORE, Tono.PERICOLO), titolo, testo).setPositiveButton("OK", null);
	}

	/** Esito positivo: icona verde, solo OK. */
	public static Builder fatto(Context ctx, CharSequence titolo, CharSequence testo) {
		return testo(new Builder(ctx).icona(FaIcone.FATTO, Tono.OK), titolo, testo).setPositiveButton("OK", null);
	}

	/** Domanda: icona blu; i pulsanti li mette chi chiama (azione principale + Annulla). */
	public static Builder conferma(Context ctx, CharSequence titolo, CharSequence testo) {
		return testo(new Builder(ctx).icona(FaIcone.DOMANDA, Tono.DOMANDA), titolo, testo);
	}

	/** Eliminazione: cestino rosso, azione principale rossa. */
	public static Builder eliminazione(Context ctx, CharSequence titolo, CharSequence testo) {
		return testo(new Builder(ctx, R.style.ThemeOverlay_Econ_Dialogo_Distruttivo).icona(FaIcone.ELIMINA, Tono.PERICOLO), titolo, testo);
	}

	private static Builder testo(Builder b, CharSequence titolo, CharSequence testo) {
		if (titolo != null) b.setTitle(titolo);
		if (testo != null) b.setMessage(testo);
		return b;
	}

	/** Scelta di una voce: sullo smartphone foglio dal basso, sul tablet dialogo al centro. */
	public static DialogInterface scelta(Context ctx, CharSequence titolo, CharSequence[] voci, DialogInterface.OnClickListener scelto) {
		return scelta(ctx, titolo, voci, scelto, true);
	}

	public static DialogInterface scelta(Context ctx, CharSequence titolo, CharSequence[] voci, DialogInterface.OnClickListener scelto,
			boolean annullabile) {
		if (!smartphone(ctx)) {
			AlertDialog d = new Builder(ctx).setTitle(titolo).setItems(voci, scelto).setNegativeButton("Annulla", null).create();
			d.setCancelable(annullabile);
			d.setCanceledOnTouchOutside(annullabile);
			d.show();
			return d;
		}
		BottomSheetDialog foglio = new BottomSheetDialog(ctx);
		View v = LayoutInflater.from(foglio.getContext()).inflate(R.layout.dialogo_foglio, null, false);
		if (titolo != null && titolo.length() > 0) {
			View t = testata(foglio.getContext(), titolo, null, null, null, null);
			t.setPadding(t.getPaddingLeft(), dp(ctx, 12), t.getPaddingRight(), t.getPaddingBottom());
			((ViewGroup) v.findViewById(R.id.foglio_testata)).addView(t);
		}
		ListView elenco = v.findViewById(R.id.foglio_voci);
		elenco.setAdapter(new ArrayAdapter<>(foglio.getContext(), R.layout.dialogo_voce, voci));
		elenco.setOnItemClickListener((l, riga, i, id) -> {
			foglio.dismiss();
			if (scelto != null) scelto.onClick(foglio, i);
		});
		foglio.setContentView(v);
		foglio.setCancelable(annullabile);
		foglio.setCanceledOnTouchOutside(annullabile);
		foglio.getBehavior().setSkipCollapsed(true);
		foglio.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
		foglio.show();
		return foglio;
	}

	/** Attesa non annullabile, gia' aperta: rotellina e testo; avanzamento() mostra la barra. */
	public static Attesa attesa(Context ctx, CharSequence testo) {
		return new Attesa(ctx, testo);
	}

	// ── campi ────────────────────────────────────────────────────────────────────────────────────────────────────────

	/** Contenitore verticale con i margini dei modali, per mettere campi ed etichette. */
	public static LinearLayout corpo(Context ctx) {
		LinearLayout l = new LinearLayout(ctx);
		l.setOrientation(LinearLayout.VERTICAL);
		l.setPadding(dp(ctx, 24), dp(ctx, 4), dp(ctx, 24), dp(ctx, 8));
		return l;
	}

	/**
	 * Campo bordato come quelli del template form (etichetta nel bordo); il testo e' {@code campo.getEditText()}.
	 * Pronto da aggiungere a un {@link #corpo}.
	 */
	public static TextInputLayout campo(Context ctx, CharSequence etichetta, int tipo) {
		TextInputLayout til = (TextInputLayout) LayoutInflater.from(ctx).inflate(R.layout.form_campo_testo, null, false);
		til.setHint(etichetta);
		EditText e = til.getEditText();
		if (e != null) {
			e.setInputType(tipo);
			if ((tipo & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0) {
				e.setMinLines(3);
				e.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
			} else {
				e.setSingleLine(true);
			}
		}
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.topMargin = dp(ctx, 12);
		til.setLayoutParams(lp);
		return til;
	}

	public static TextInputLayout campo(Context ctx, CharSequence etichetta) {
		return campo(ctx, etichetta, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
	}

	/** Campo password con l'occhio per vedere quello che si scrive. */
	public static TextInputLayout password(Context ctx, CharSequence etichetta) {
		TextInputLayout til = campo(ctx, etichetta, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		til.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
		return til;
	}

	/** Testo semplice dentro un modale (spiegazione sopra o sotto i campi). */
	public static TextView nota(Context ctx, CharSequence testo) {
		TextView t = new TextView(ctx);
		t.setText(testo);
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		t.setTextColor(ContextCompat.getColor(ctx, R.color.econ_testo_secondario));
		t.setLineSpacing(dp(ctx, 2), 1f);
		t.setPadding(0, dp(ctx, 4), 0, dp(ctx, 4));
		return t;
	}

	/** Etichetta di un gruppo di campi (grigia, grassetto). */
	public static TextView etichetta(Context ctx, CharSequence testo) {
		TextView t = new TextView(ctx);
		t.setText(testo);
		Stile.etichetta(t);
		t.setPadding(0, dp(ctx, 14), 0, dp(ctx, 2));
		return t;
	}

	/** Testo del campo ("" se manca). */
	public static String valore(TextInputLayout campo) {
		return campo.getEditText() == null ? "" : campo.getEditText().getText().toString();
	}

	// ── data e ora (selettori Material 3, al posto di DatePickerDialog/TimePickerDialog) ───────────────────────────────

	public interface DataScelta {
		/** mese da 0 (come Calendar). */
		void scelta(int anno, int mese0, int giorno);
	}

	public interface PeriodoScelto {
		void scelto(int[] dal, int[] al);
	}

	public interface OraScelta {
		void scelta(int ore, int minuti);
	}

	private static androidx.fragment.app.FragmentActivity activity(Context ctx) {
		while (ctx instanceof android.content.ContextWrapper) {
			if (ctx instanceof androidx.fragment.app.FragmentActivity) return (androidx.fragment.app.FragmentActivity) ctx;
			ctx = ((android.content.ContextWrapper) ctx).getBaseContext();
		}
		return null;
	}

	private static long utc(int anno, int mese0, int giorno) {
		java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
		c.clear();
		c.set(anno, mese0, giorno);
		return c.getTimeInMillis();
	}

	private static int[] daUtc(long millis) {
		java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
		c.setTimeInMillis(millis);
		return new int[] { c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH) };
	}

	/** Calendario per scegliere un giorno (come i campi data del template form). */
	public static void data(Context ctx, CharSequence titolo, int anno, int mese0, int giorno, DataScelta fatto) {
		androidx.fragment.app.FragmentActivity a = activity(ctx);
		if (a == null) {
			new android.app.DatePickerDialog(ctx, (v, y, m, g) -> fatto.scelta(y, m, g), anno, mese0, giorno).show();
			return;
		}
		com.google.android.material.datepicker.MaterialDatePicker<Long> p = com.google.android.material.datepicker.MaterialDatePicker.Builder
				.datePicker().setTitleText(titolo == null ? "Scegli il giorno" : titolo).setSelection(utc(anno, mese0, giorno)).build();
		p.addOnPositiveButtonClickListener(millis -> {
			int[] d = daUtc(millis);
			fatto.scelta(d[0], d[1], d[2]);
		});
		p.show(a.getSupportFragmentManager(), "econ_data");
	}

	/** Calendario per scegliere un periodo (dal … al). */
	public static void periodo(Context ctx, CharSequence titolo, PeriodoScelto fatto) {
		androidx.fragment.app.FragmentActivity a = activity(ctx);
		if (a == null) return;
		com.google.android.material.datepicker.MaterialDatePicker<androidx.core.util.Pair<Long, Long>> p =
				com.google.android.material.datepicker.MaterialDatePicker.Builder.dateRangePicker()
						.setTitleText(titolo == null ? "Scegli il periodo" : titolo).build();
		p.addOnPositiveButtonClickListener(sel -> {
			if (sel == null || sel.first == null) return;
			fatto.scelto(daUtc(sel.first), daUtc(sel.second == null ? sel.first : sel.second));
		});
		p.show(a.getSupportFragmentManager(), "econ_periodo");
	}

	/** Orologio a 24 ore per scegliere un orario. */
	public static void ora(Context ctx, CharSequence titolo, int ore, int minuti, OraScelta fatto) {
		androidx.fragment.app.FragmentActivity a = activity(ctx);
		if (a == null) {
			new android.app.TimePickerDialog(ctx, (v, h, m) -> fatto.scelta(h, m), ore, minuti, true).show();
			return;
		}
		com.google.android.material.timepicker.MaterialTimePicker p = new com.google.android.material.timepicker.MaterialTimePicker.Builder()
				.setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H).setHour(ore).setMinute(minuti)
				.setTitleText(titolo == null ? "Scegli l'ora" : titolo).build();
		p.addOnPositiveButtonClickListener(v -> fatto.scelta(p.getHour(), p.getMinute()));
		p.show(a.getSupportFragmentManager(), "econ_ora");
	}

	// ── finestre grandi ──────────────────────────────────────────────────────────────────────────────────────────────

	/**
	 * Testata delle finestre grandi (finestra_testata.xml): X per chiudere, titolo, sottotitolo; le azioni si aggiungono
	 * in {@code R.id.finestra_azioni}. La stessa testata e' nelle activity-finestra (Finestra*Activity).
	 */
	public static View testataFinestra(Context ctx, CharSequence titolo, CharSequence sottotitolo, Runnable chiudi) {
		View v = LayoutInflater.from(ctx).inflate(R.layout.finestra_testata, null, false);
		impostaTestataFinestra(v, titolo, sottotitolo, chiudi);
		return v;
	}

	public static void impostaTestataFinestra(View v, CharSequence titolo, CharSequence sottotitolo, Runnable chiudi) {
		Context ctx = v.getContext();
		((TextView) v.findViewById(R.id.finestra_titolo)).setText(titolo);
		TextView sotto = v.findViewById(R.id.finestra_sottotitolo);
		sotto.setText(sottotitolo);
		sotto.setVisibility(sottotitolo == null || sottotitolo.length() == 0 ? View.GONE : View.VISIBLE);
		TextView x = v.findViewById(R.id.finestra_chiudi);
		FaIcone.applica(x, FaIcone.CHIUDI, "Chiudi");
		x.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
		x.setTextColor(0xFFFFFFFF);
		if (chiudi == null && ctx instanceof Activity) chiudi = ((Activity) ctx)::finish;
		final Runnable azione = chiudi;
		x.setOnClickListener(b -> {
			if (azione != null) azione.run();
		});
	}

	/** Activity-finestra flottante (tema EconFinestra): angoli arrotondati come i modali. */
	public static void arrotonda(Activity a) {
		ViewGroup contenuto = a.findViewById(android.R.id.content);
		if (contenuto == null || contenuto.getChildCount() == 0) return;
		View radice = contenuto.getChildAt(0);
		final float raggio = dp(a, 20);
		radice.setOutlineProvider(new android.view.ViewOutlineProvider() {
			@Override
			public void getOutline(View v, android.graphics.Outline o) {
				o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), raggio);
			}
		});
		radice.setClipToOutline(true);
	}

	// ── testata ──────────────────────────────────────────────────────────────────────────────────────────────────────

	static View testata(Context ctx, CharSequence titolo, CharSequence sottotitolo, String glifo, Tono tono, Drawable immagine) {
		View v = LayoutInflater.from(ctx).inflate(R.layout.dialogo_testata, null, false);
		((TextView) v.findViewById(R.id.dialogo_titolo)).setText(titolo);
		v.findViewById(R.id.dialogo_titolo).setVisibility(titolo == null ? View.GONE : View.VISIBLE);
		TextView sotto = v.findViewById(R.id.dialogo_sottotitolo);
		if (sottotitolo != null && sottotitolo.length() > 0) {
			sotto.setText(sottotitolo);
			sotto.setVisibility(View.VISIBLE);
		}
		if (glifo != null) {
			TextView icona = v.findViewById(R.id.dialogo_icona);
			int colore = colore(ctx, tono == null ? Tono.INFO : tono);
			icona.setTypeface(ResourcesCompat.getFont(ctx, R.font.fa_solid_900), Typeface.NORMAL);
			icona.setText(glifo);
			icona.setTextColor(colore);
			GradientDrawable cerchio = new GradientDrawable();
			cerchio.setShape(GradientDrawable.OVAL);
			cerchio.setColor((colore & 0x00FFFFFF) | 0x1F000000);
			icona.setBackground(cerchio);
			icona.setVisibility(View.VISIBLE);
		} else if (immagine != null) {
			android.widget.ImageView img = v.findViewById(R.id.dialogo_immagine);
			img.setImageDrawable(immagine);
			img.setVisibility(View.VISIBLE);
		}
		return v;
	}

	static int colore(Context ctx, Tono tono) {
		switch (tono) {
			case ATTENZIONE: return ContextCompat.getColor(ctx, R.color.econ_attenzione);
			case PERICOLO: return ContextCompat.getColor(ctx, R.color.econ_errore);
			case OK: return ContextCompat.getColor(ctx, R.color.econ_ok);
			default: return ContextCompat.getColor(ctx, R.color.econ_primario);
		}
	}

	public static boolean smartphone(Context ctx) {
		return ctx.getResources().getConfiguration().smallestScreenWidthDp < LARGHEZZA_TABLET;
	}

	static int dp(Context ctx, float v) {
		return Math.round(v * ctx.getResources().getDisplayMetrics().density);
	}

	// ── builder ──────────────────────────────────────────────────────────────────────────────────────────────────────

	/**
	 * Il builder di tutti i dialoghi dell'app. Si usa come MaterialAlertDialogBuilder; in piu': titolo nella testata
	 * standard, icona di sistema (ic_dialog_alert/info) tradotta nell'icona del tono, {@link #icona}, {@link #sottotitolo},
	 * vista personalizzata con margini e campi standard.
	 */
	public static class Builder extends MaterialAlertDialogBuilder {

		private CharSequence titolo;
		private CharSequence sottotitolo;
		private String glifo;
		private Tono tono;
		private Drawable immagine;
		private View vista;
		private boolean testataPropria;
		private CharSequence messaggio;
		/** Pulsanti, voci o vista: se non c'e' niente di questo e c'e' solo un testo, e' un'attesa (rotellina). */
		private boolean contenuto;

		public Builder(@NonNull Context ctx) {
			super(ctx);
		}

		public Builder(@NonNull Context ctx, int tema) {
			super(ctx, tema);
		}

		/** Icona Font Awesome in testata, nel colore del tono. */
		public Builder icona(String glifo, Tono tono) {
			this.glifo = glifo;
			this.tono = tono;
			return this;
		}

		public Builder sottotitolo(CharSequence testo) {
			sottotitolo = testo;
			return this;
		}

		@NonNull
		@Override
		public Builder setTitle(CharSequence t) {
			titolo = t == null || t.length() == 0 ? null : t;
			return this;
		}

		@NonNull
		@Override
		public Builder setTitle(int id) {
			titolo = id == 0 ? null : getContext().getText(id);
			return this;
		}

		@NonNull
		@Override
		public Builder setIcon(int id) {
			if (id == android.R.drawable.ic_dialog_alert) {
				icona(FaIcone.ATTENZIONE, Tono.ATTENZIONE);
			} else if (id == android.R.drawable.ic_dialog_info) {
				icona(FaIcone.INFO, Tono.INFO);
			} else if (id != 0) {
				immagine = AppCompatResources.getDrawable(getContext(), id);
			}
			return this;
		}

		@NonNull
		@Override
		public Builder setIcon(Drawable d) {
			immagine = d;
			return this;
		}

		@NonNull
		@Override
		public Builder setCustomTitle(View v) {
			testataPropria = true;
			super.setCustomTitle(v);
			return this;
		}

		@NonNull
		@Override
		public Builder setView(View v) {
			contenuto = true;
			// un EditText da solo: campo bordato con l'etichetta presa dal suggerimento, nei margini dei modali
			if (v instanceof EditText && v.getParent() == null && !(v instanceof com.google.android.material.textfield.TextInputEditText)) {
				v = incornicia(getContext(), (EditText) v);
			}
			vista = v;
			// contenuto senza scorrimento proprio: scorre, cosi' con la tastiera aperta i campi non si schiacciano
			if (v != null && v.getParent() == null && !scorrevole(v)) {
				androidx.core.widget.NestedScrollView sv = new androidx.core.widget.NestedScrollView(v.getContext());
				sv.addView(v, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				super.setView(sv);
			} else {
				super.setView(v);
			}
			return this;
		}

		/** EditText gia' creato dentro un TextInputLayout del template, in un corpo coi margini dei modali. */
		private static View incornicia(Context ctx, EditText e) {
			LinearLayout corpo = corpo(ctx);
			TextInputLayout til = (TextInputLayout) LayoutInflater.from(ctx).inflate(R.layout.dialogo_campo_vuoto, corpo, false);
			e.setBackground(null);
			int p = dp(ctx, 16);
			e.setPadding(p, p, p, p);
			e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
			til.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			corpo.addView(til);
			return corpo;
		}

		/** Vista che scorre gia' da se' (o contiene liste/griglie, che dentro uno ScrollView non funzionano). */
		private static boolean scorrevole(View v) {
			if (v instanceof android.widget.ScrollView || v instanceof androidx.core.widget.NestedScrollView
					|| v instanceof android.widget.AbsListView || v instanceof androidx.recyclerview.widget.RecyclerView
					|| v instanceof android.webkit.WebView) {
				return true;
			}
			if (v instanceof ViewGroup) {
				ViewGroup g = (ViewGroup) v;
				for (int i = 0; i < g.getChildCount(); i++) {
					if (scorrevole(g.getChildAt(i))) return true;
				}
			}
			return false;
		}

		@NonNull
		@Override
		public Builder setMessage(CharSequence m) {
			messaggio = m;
			super.setMessage(m);
			return this;
		}

		@NonNull
		@Override
		public Builder setMessage(int id) {
			messaggio = id == 0 ? null : getContext().getText(id);
			super.setMessage(id);
			return this;
		}

		@NonNull
		@Override
		public Builder setPositiveButton(CharSequence t, DialogInterface.OnClickListener l) {
			contenuto = true;
			super.setPositiveButton(t, l);
			return this;
		}

		@NonNull
		@Override
		public Builder setPositiveButton(int t, DialogInterface.OnClickListener l) {
			contenuto = true;
			super.setPositiveButton(t, l);
			return this;
		}

		@NonNull
		@Override
		public Builder setNegativeButton(CharSequence t, DialogInterface.OnClickListener l) {
			contenuto = true;
			super.setNegativeButton(t, l);
			return this;
		}

		@NonNull
		@Override
		public Builder setNegativeButton(int t, DialogInterface.OnClickListener l) {
			contenuto = true;
			super.setNegativeButton(t, l);
			return this;
		}

		@NonNull
		@Override
		public Builder setNeutralButton(CharSequence t, DialogInterface.OnClickListener l) {
			contenuto = true;
			super.setNeutralButton(t, l);
			return this;
		}

		@NonNull
		@Override
		public Builder setNeutralButton(int t, DialogInterface.OnClickListener l) {
			contenuto = true;
			super.setNeutralButton(t, l);
			return this;
		}

		@NonNull
		@Override
		public Builder setCancelable(boolean c) {
			super.setCancelable(c);
			return this;
		}

		@NonNull
		@Override
		public Builder setItems(CharSequence[] voci, DialogInterface.OnClickListener l) {
			contenuto = true;
			super.setItems(voci, l);
			return this;
		}

		@NonNull
		@Override
		public Builder setItems(int voci, DialogInterface.OnClickListener l) {
			contenuto = true;
			super.setItems(voci, l);
			return this;
		}

		@NonNull
		@Override
		public Builder setAdapter(android.widget.ListAdapter a, DialogInterface.OnClickListener l) {
			contenuto = true;
			super.setAdapter(a, l);
			return this;
		}

		@NonNull
		@Override
		public Builder setSingleChoiceItems(CharSequence[] voci, int scelta, DialogInterface.OnClickListener l) {
			contenuto = true;
			super.setSingleChoiceItems(voci, scelta, l);
			return this;
		}

		@NonNull
		@Override
		public Builder setMultiChoiceItems(CharSequence[] voci, boolean[] scelte, DialogInterface.OnMultiChoiceClickListener l) {
			contenuto = true;
			super.setMultiChoiceItems(voci, scelte, l);
			return this;
		}

		@NonNull
		@Override
		public AlertDialog create() {
			if (!contenuto && messaggio != null) {
				// solo un testo, niente pulsanti: e' un "Attendere..." (codice vecchio) → rotellina e testo
				View v = LayoutInflater.from(getContext()).inflate(R.layout.dialogo_attesa, null, false);
				((TextView) v.findViewById(R.id.attesa_testo)).setText(messaggio);
				super.setMessage(null);
				super.setView(v);
			}
			if (!testataPropria && (titolo != null || glifo != null || immagine != null)) {
				super.setCustomTitle(testata(getContext(), titolo, sottotitolo, glifo, tono, immagine));
			}
			if (vista != null) Stile.applica(vista);
			return super.create();
		}
	}

	// ── attesa ───────────────────────────────────────────────────────────────────────────────────────────────────────

	/** Dialogo d'attesa (al posto di ProgressDialog e dei dialoghi di solo testo "Attendere..."). */
	public static class Attesa {

		private final AlertDialog dialogo;
		private final TextView testo;
		private final LinearProgressIndicator barra;
		private final CircularProgressIndicator rotellina;

		Attesa(Context ctx, CharSequence messaggio) {
			View v = LayoutInflater.from(ctx).inflate(R.layout.dialogo_attesa, null, false);
			testo = v.findViewById(R.id.attesa_testo);
			barra = v.findViewById(R.id.attesa_barra);
			rotellina = v.findViewById(R.id.attesa_rotellina);
			testo.setText(messaggio);
			dialogo = new Builder(ctx).setView(v).setCancelable(false).create();
			dialogo.setCanceledOnTouchOutside(false);
			dialogo.show();
		}

		public void testo(CharSequence t) {
			testo.setText(t);
		}

		/** Avanzamento 0-100 (barra sotto il testo); -1 = di nuovo indeterminato. */
		public void avanzamento(int percentuale) {
			if (percentuale < 0) {
				barra.setVisibility(View.GONE);
				return;
			}
			barra.setVisibility(View.VISIBLE);
			barra.setProgressCompat(Math.min(100, percentuale), true);
		}

		public boolean aperta() {
			return dialogo.isShowing();
		}

		public AlertDialog dialogo() {
			return dialogo;
		}

		public void chiudi() {
			try {
				if (dialogo.isShowing()) dialogo.dismiss();
			} catch (IllegalArgumentException e) {
				// activity gia' chiusa
			}
		}

		/** Si puo' chiudere col tasto indietro o toccando fuori; alla chiusura esegue {@code annullato}. */
		public Attesa annullabile(Runnable annullato) {
			dialogo.setCancelable(true);
			dialogo.setCanceledOnTouchOutside(true);
			if (annullato != null) dialogo.setOnCancelListener(d -> annullato.run());
			return this;
		}

		// stessi nomi di ProgressDialog (codice vecchio)
		public void dismiss() { chiudi(); }
		public boolean isShowing() { return aperta(); }
		public void setMessage(CharSequence t) { testo(t); }
		public void setProgress(int p) { avanzamento(p); }
	}

	/** Tinta di un colore, per chi disegna a mano (es. pulsanti) nello stile dei modali. */
	public static ColorStateList tinta(Context ctx, Tono tono) {
		return ColorStateList.valueOf(colore(ctx, tono));
	}
}
