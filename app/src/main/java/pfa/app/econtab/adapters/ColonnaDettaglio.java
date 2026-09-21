package pfa.app.econtab.adapters;

import android.view.Gravity;

/**
 * Definizione di una colonna della tabella di dettaglio standard (vedi DettaglioTabellaAdapter).
 * Larghezza fissa in dp oppure flessibile (peso): la testata e le righe usano la stessa definizione,
 * quindi sono sempre allineate.
 */
public class ColonnaDettaglio {

	public enum Tipo {
		/** Cella di testo (TextView). */
		TESTO,
		/** Icona Font Awesome (TextView, vedi FaIcone): colonna stretta, con tooltip. */
		ICONA,
		/** Contenuto costruito dal modulo con DettaglioTabellaAdapter.creaCellaCustom. */
		CUSTOM
	}

	public final String id;
	public final String titolo;
	public final Tipo tipo;
	/** Larghezza in dp; 0 = flessibile secondo il peso. */
	public final int larghezzaDp;
	public final float peso;
	public final int gravity;

	private ColonnaDettaglio(String id, String titolo, Tipo tipo, int larghezzaDp, float peso, int gravity) {
		this.id = id;
		this.titolo = titolo;
		this.tipo = tipo;
		this.larghezzaDp = larghezzaDp;
		this.peso = peso;
		this.gravity = gravity;
	}

	/** Colonna di testo flessibile (es. descrizione). */
	public static ColonnaDettaglio testo(String id, String titolo, float peso) {
		return new ColonnaDettaglio(id, titolo, Tipo.TESTO, 0, peso, Gravity.START);
	}

	/** Colonna di testo a larghezza fissa; gravity: Gravity.START per il testo, Gravity.END per i numeri. */
	public static ColonnaDettaglio testoFisso(String id, String titolo, int larghezzaDp, int gravity) {
		return new ColonnaDettaglio(id, titolo, Tipo.TESTO, larghezzaDp, 0, gravity);
	}

	/** Colonna icona, stretta. Il titolo (spesso vuoto) e' anche il tooltip della testata. */
	public static ColonnaDettaglio icona(String id, String titolo, int larghezzaDp) {
		return new ColonnaDettaglio(id, titolo, Tipo.ICONA, larghezzaDp, 0, Gravity.CENTER);
	}

	public static ColonnaDettaglio custom(String id, String titolo, int larghezzaDp) {
		return new ColonnaDettaglio(id, titolo, Tipo.CUSTOM, larghezzaDp, 0, Gravity.CENTER);
	}
}
