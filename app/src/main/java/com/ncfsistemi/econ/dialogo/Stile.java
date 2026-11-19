package com.ncfsistemi.econ.dialogo;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.textfield.TextInputLayout;
import com.ncfsistemi.econ.R;

/**
 * Porta il contenuto di un modale allo stile unico (STUDIO_MODALI_APP.md §4): margini laterali 24dp, EditText e Spinner
 * vecchi bordati e arrotondati come i campi del template form, etichette azzurre (TestoIntestazione) grigie e in
 * grassetto. I campi gia' Material (TextInputLayout) non si toccano. Lo chiama EconDialogo.Builder.create(); si puo'
 * chiamare anche sulle finestre grandi.
 */
public final class Stile {

	/** Colori delle etichette vecchie (TestoIntestazione = holo_blue_dark, e holo_blue_light). */
	private static final int AZZURRO_SCURO = 0xFF0099CC;
	private static final int AZZURRO_CHIARO = 0xFF33B5E5;

	private Stile() {
	}

	public static void applica(View radice) {
		margini(radice);
		percorri(radice);
	}

	/** Solo i campi (senza toccare i margini): per le finestre a tutta pagina (Finestra*Activity). */
	public static void contenuto(View radice) {
		percorri(radice);
	}

	/** Contenuto senza margini propri (o stretti): 24dp ai lati, come testata e pulsanti. */
	private static void margini(View radice) {
		View v = radice;
		if ((v instanceof ScrollView || v instanceof NestedScrollView) && ((ViewGroup) v).getChildCount() == 1) {
			v = ((ViewGroup) v).getChildAt(0);
		}
		if (!(v instanceof ViewGroup)) return;
		Context ctx = v.getContext();
		int lato = EconDialogo.dp(ctx, 24);
		if (v.getPaddingLeft() < EconDialogo.dp(ctx, 20) || v.getPaddingRight() < EconDialogo.dp(ctx, 20)) {
			v.setPadding(lato, Math.max(v.getPaddingTop(), EconDialogo.dp(ctx, 4)), lato, Math.max(v.getPaddingBottom(), EconDialogo.dp(ctx, 8)));
		}
	}

	private static void percorri(View v) {
		if (v instanceof TextInputLayout) return;
		if (v instanceof EditText) {
			campo((EditText) v);
		} else if (v instanceof Spinner) {
			tendina((Spinner) v);
		} else if (v instanceof Button && !(v instanceof CompoundButton) && !(v instanceof com.google.android.material.button.MaterialButton)
				&& ((Button) v).getText().length() > 0) {
			// solo i pulsanti con un testo: quelli vuoti hanno un'icona come sfondo
			pulsante((Button) v);
		} else if (v instanceof TextView && !(v instanceof Button) && !(v instanceof CompoundButton)) {
			int c = ((TextView) v).getCurrentTextColor();
			if (c == AZZURRO_SCURO || c == AZZURRO_CHIARO) {
				etichetta((TextView) v);
				// spazio dal campo sopra, come fra i campi del template
				v.setPadding(v.getPaddingLeft(), Math.max(v.getPaddingTop(), EconDialogo.dp(v.getContext(), 10)), v.getPaddingRight(),
						Math.max(v.getPaddingBottom(), EconDialogo.dp(v.getContext(), 4)));
			}
		}
		if (v instanceof ViewGroup) {
			ViewGroup g = (ViewGroup) v;
			for (int i = 0; i < g.getChildCount(); i++) percorri(g.getChildAt(i));
		}
	}

	/** EditText fuori da un TextInputLayout: bordo arrotondato, testo 16sp, altezza minima da dito. */
	public static void campo(EditText e) {
		Context ctx = e.getContext();
		e.setBackgroundResource(R.drawable.econ_campo_bordato);
		e.setPadding(EconDialogo.dp(ctx, 12), EconDialogo.dp(ctx, 10), EconDialogo.dp(ctx, 12), EconDialogo.dp(ctx, 10));
		e.setMinHeight(EconDialogo.dp(ctx, 46));
		float sp = e.getTextSize() / ctx.getResources().getDisplayMetrics().scaledDensity;
		if (sp < 15f) e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		e.setTextColor(ContextCompat.getColor(ctx, R.color.econ_testo));
		e.setHintTextColor(ContextCompat.getColor(ctx, R.color.econ_testo_secondario));
	}

	/** Spinner: stesso bordo dei campi, freccia a destra. */
	public static void tendina(Spinner s) {
		Context ctx = s.getContext();
		s.setBackgroundResource(R.drawable.econ_scelta_bordata);
		s.setPadding(EconDialogo.dp(ctx, 4), 0, EconDialogo.dp(ctx, 36), 0);
		s.setMinimumHeight(EconDialogo.dp(ctx, 46));
	}

	/** Button vecchio (AppCompat con sfondo button_econ): pillola bordata, testo blu, altezza da dito. */
	public static void pulsante(Button b) {
		Context ctx = b.getContext();
		b.setBackgroundResource(R.drawable.econ_pulsante_bordato);
		b.setTextColor(ContextCompat.getColor(ctx, R.color.econ_primario));
		b.setAllCaps(false);
		b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		b.setMinHeight(EconDialogo.dp(ctx, 40));
		b.setMinimumHeight(EconDialogo.dp(ctx, 40));
		b.setPadding(EconDialogo.dp(ctx, 16), 0, EconDialogo.dp(ctx, 16), 0);
		b.setStateListAnimator(null);
	}

	/** Etichetta di un campo o di un gruppo: grigia, grassetto, 13sp. */
	public static void etichetta(TextView t) {
		Context ctx = t.getContext();
		t.setTextColor(ContextCompat.getColor(ctx, R.color.econ_testo_secondario));
		t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
		t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
		t.setAllCaps(false);
	}
}
