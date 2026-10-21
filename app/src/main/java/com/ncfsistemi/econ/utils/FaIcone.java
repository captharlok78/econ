package com.ncfsistemi.econ.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.ncfsistemi.econ.R;

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
	public static final String MODIFICA = "\uf304";     // pen
	public static final String ELIMINA = "";     // trash-alt
	public static final String PIU = "";         // plus
	public static final String MENO = "";        // minus
	public static final String MAPPA = "\uf5a0";        // map-marked-alt
	public static final String TELEFONO = "\uf095";     // phone
	public static final String EMAIL = "\uf0e0";        // envelope
	public static final String FIRMA = "\uf5b7";        // signature: firmato dal cliente
	public static final String PRINCIPALE = "\uf005";   // star
	public static final String AUTOMATICO = "\uf0e7";   // bolt
	public static final String ELENCO = "\uf03a";       // list
	public static final String AZIONI = "\uf142";       // ellipsis-v: menu delle azioni della riga
	public static final String XLS = "\uf1c3";          // file-excel: esporta in xls
	public static final String STATO = "\uf111";        // circle: stato del documento, nel colore dello stato
	// Sezioni delle schede (STANDARD_GRAFICO.md)
	public static final String CLIENTE = "\uf007";      // user
	public static final String INDIRIZZO = "\uf3c5";    // map-marker-alt
	public static final String REFERENTI = "\uf2b9";    // address-book
	public static final String FATTURAZIONE = "\uf570"; // file-invoice-dollar
	// Livelli del menu del cantiere
	public static final String PREVENTIVO = "";  // file-invoice
	public static final String CASCO = "\uf807";       // hard-hat: cantiere di un indirizzo (giallo = c'e', verde = da creare)
	public static final String CANTIERE = "";    // building
	public static final String UNITA = "";       // home
	public static final String AREA = "";        // layer-group
	public static final String LOCALE = "";      // door-open
	public static final String IMPIANTO = "\uf1e6";    // plug: gestione elettrica del cantiere
	public static final String IMPOSTAZIONI = "\uf013"; // cog: preferenze
	public static final String DITTA = "\uf1ad";        // building: dati della ditta
	public static final String SCARICA = "\uf063";     // arrow-down: scarica da server
	public static final String CARICA = "\uf062";      // arrow-up: carica su server
	public static final String CALENDARIO = "\uf073";  // calendar-alt: calendario personale
	public static final String ORE = "\uf017";         // clock: ore lavorate
	public static final String PIANIFICATO = "\uf274"; // calendar-check: pianificazione
	public static final String INDIETRO = "\uf053";    // chevron-left
	public static final String AVANTI = "\uf054";      // chevron-right
	public static final String LUCCHETTO = "\uf023";    // lock: campo che non si modifica (template form)
	public static final String CELLULARE = "\uf3cd";    // mobile-alt
	public static final String PERCENTUALE = "\uf295";  // percent
	public static final String CODICE = "\uf02a";       // barcode
	public static final String CONFERMA = "\uf00c";     // check: pulsante Salva
	public static final String RAPPORTINO = "\uf46d";  // clipboard-list: rapportino
	public static final String INFO = "\uf05a";
	public static final String VIAGGIO = "\uf1b9";    // car: riga di viaggio del rapportino
	public static final String APRI = "\uf078";       // chevron-down: sezione a fisarmonica chiusa
	public static final String CHIUDI_SEZIONE = "\uf077"; // chevron-up: sezione a fisarmonica aperta        // info-circle: avviso su un campo che non si modifica (template form)
	public static final String GIUSTIFICATIVO = "\uf573"; // file-signature: giustificativo (ferie, permessi…) senza icona nota

	/** Nome Font Awesome → glifo (assets/fa_solid_nomi.txt), per le icone che arrivano dal server come classe ("fa-…"). */
	private static java.util.Map<String, String> perNome;

	private static Typeface typeface;

	private FaIcone() {
	}

	private static synchronized Typeface getTypeface(Context context) {
		if (typeface == null) {
			typeface = ResourcesCompat.getFont(context.getApplicationContext(), R.font.fa_solid_900);
		}
		return typeface;
	}

	/**
	 * Icona azione dello standard grafico (STANDARD_GRAFICO.md): TextView con il glifo, grigia, senza cerchio, area di tocco
	 * 44dp; tooltip e descrizione per l'accessibilita'.
	 */
	public static TextView azione(Context ctx, String glifo, String tooltip, android.view.View.OnClickListener azione) {
		TextView tv = new TextView(ctx);
		applica(tv, glifo, tooltip);
		tv.setContentDescription(tooltip);
		int lato = Math.round(44 * ctx.getResources().getDisplayMetrics().density);
		tv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(lato, lato));
		tv.setOnClickListener(azione);
		android.util.TypedValue sfondo = new android.util.TypedValue();
		ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, sfondo, true);
		tv.setBackgroundResource(sfondo.resourceId);
		return tv;
	}

	/**
	 * Glifo di una classe Font Awesome del portale ("fa-umbrella-beach", "fa fa-broom"…), anche con i nomi di FA6 piu'
	 * comuni; GIUSTIFICATIVO se il nome non c'e' nel font dell'app.
	 */
	public static String daNome(Context ctx, String classe) {
		synchronized (FaIcone.class) {
			if (perNome == null) {
				perNome = new java.util.HashMap<>();
				try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(
						ctx.getApplicationContext().getAssets().open("fa_solid_nomi.txt"), java.nio.charset.StandardCharsets.UTF_8))) {
					String riga;
					while ((riga = r.readLine()) != null) {
						int uguale = riga.indexOf('=');
						if (riga.startsWith("#") || uguale <= 0) continue;
						perNome.put(riga.substring(0, uguale), new String(Character.toChars(Integer.parseInt(riga.substring(uguale + 1).trim(), 16))));
					}
				} catch (Exception e) {
					android.util.Log.w("FaIcone", "Nomi delle icone non letti: " + e.getMessage());
				}
			}
		}
		if (classe == null) return GIUSTIFICATIVO;
		for (String parte : classe.trim().split("\\s+")) {
			String nome = parte.startsWith("fa-") ? parte.substring(3) : parte;
			String glifo = perNome.get(nome);
			if (glifo != null) return glifo;
		}
		return GIUSTIFICATIVO;
	}

	/**
	 * Glifo come Drawable (lato in dp), per le icone dentro i campi del template form (TextInputLayout.setStartIconDrawable)
	 * e nei pulsanti Material; colore di default, il campo lo puo' ritingere.
	 */
	public static android.graphics.drawable.Drawable drawable(Context ctx, String glifo, int latoDp) {
		final android.graphics.Paint pennello = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
		final float densita = ctx.getResources().getDisplayMetrics().density;
		final int lato = Math.round(latoDp * densita);
		pennello.setTypeface(getTypeface(ctx));
		pennello.setTextAlign(android.graphics.Paint.Align.CENTER);
		pennello.setTextSize(lato * 0.85f);
		pennello.setColor(ContextCompat.getColor(ctx, R.color.colore_icona));
		return new android.graphics.drawable.Drawable() {
			@Override
			public void draw(android.graphics.Canvas canvas) {
				android.graphics.Rect r = getBounds();
				float y = r.exactCenterY() - (pennello.descent() + pennello.ascent()) / 2f;
				canvas.drawText(glifo, r.exactCenterX(), y, pennello);
			}

			@Override
			public void setAlpha(int alpha) {
				pennello.setAlpha(alpha);
			}

			@Override
			public void setColorFilter(android.graphics.ColorFilter filtro) {
				pennello.setColorFilter(filtro);
			}

			@Override
			public void setTintList(android.content.res.ColorStateList tinta) {
				if (tinta != null) pennello.setColor(tinta.getColorForState(getState(), tinta.getDefaultColor()));
				invalidateSelf();
			}

			@Override
			public int getOpacity() {
				return android.graphics.PixelFormat.TRANSLUCENT;
			}

			@Override
			public int getIntrinsicWidth() {
				return lato;
			}

			@Override
			public int getIntrinsicHeight() {
				return lato;
			}
		};
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
