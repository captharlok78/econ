package pfa.app.econtab.utils;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import pfa.app.econtab.R;

/**
 * Testata di sezione dello standard grafico (layout sezione_standard.xml, STANDARD_GRAFICO.md): icona, titolo e azioni a
 * icona (FaIcone.azione) a destra, senza pulsanti con testo.
 */
public final class SezioneStandard {

	private SezioneStandard() {
	}

	/** Imposta icona e titolo della sezione (la View e' l'include di sezione_standard). */
	public static void imposta(View sezione, String glifo, String titolo) {
		FaIcone.applica(sezione.findViewById(R.id.sezioneIcona), glifo, null);
		((TextView) sezione.findViewById(R.id.sezioneIcona)).setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
		((TextView) sezione.findViewById(R.id.sezioneTitolo)).setText(titolo);
	}

	/** Aggiunge un'azione a icona a destra (es. FaIcone.PIU per "nuovo"). */
	public static void azione(View sezione, String glifo, String tooltip, View.OnClickListener listener) {
		LinearLayout azioni = sezione.findViewById(R.id.sezioneAzioni);
		azioni.addView(FaIcone.azione(sezione.getContext(), glifo, tooltip, listener));
	}
}
