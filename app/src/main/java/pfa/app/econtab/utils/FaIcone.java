package pfa.app.econtab.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import pfa.app.econtab.R;

/**
 * Icone Font Awesome (Free, solid, licenza SIL OFL 1.1) per tutta l'app: un glifo in una TextView, sempre
 * nel colore di default delle icone (R.color.colore_icona), con tooltip opzionale (pressione lunga).
 * Nessuna immagine colorata per icona: per una nuova icona si aggiunge qui la costante del glifo.
 */
public final class FaIcone {

	// Tipologie righe di preventivo/ordine
	public static final String MATERIALE = "";   // cubes
	public static final String MANODOPERA = "";  // hard-hat
	public static final String COLLEGAMENTI = ""; // link
	public static final String PLACCHE = "";     // th-large
	public static final String NOTE = "";        // sticky-note
	// Azioni
	public static final String ELIMINA = "";     // trash-alt
	public static final String PIU = "";         // plus
	public static final String MENO = "";        // minus
	// Livelli del menu del cantiere
	public static final String PREVENTIVO = "";  // file-invoice
	public static final String CANTIERE = "";    // building
	public static final String UNITA = "";       // home
	public static final String AREA = "";        // layer-group
	public static final String LOCALE = "";      // door-open

	private static Typeface typeface;

	private FaIcone() {
	}

	private static synchronized Typeface getTypeface(Context context) {
		if (typeface == null) {
			typeface = ResourcesCompat.getFont(context.getApplicationContext(), R.font.fa_solid_900);
		}
		return typeface;
	}

	/** Trasforma la TextView in un'icona: glifo, font, colore di default, centrata; tooltip se non nullo. */
	public static void applica(TextView tv, String glifo, CharSequence tooltip) {
		Context ctx = tv.getContext();
		tv.setTypeface(getTypeface(ctx), Typeface.NORMAL);
		tv.setText(glifo);
		tv.setTextColor(ContextCompat.getColor(ctx, R.color.colore_icona));
		tv.setGravity(Gravity.CENTER);
		tv.setIncludeFontPadding(false);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		TooltipCompat.setTooltipText(tv, tooltip);
	}
}
