package pfa.app.econtab.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.TooltipCompat;

import java.util.ArrayList;
import java.util.List;

import pfa.app.econtab.R;
import pfa.app.econtab.utils.FaIcone;

/**
 * Padre astratto delle maschere di dettaglio "a tabella" (righe di preventivo/ordine, e in prospettiva
 * rapportini): una riga per record con i dati in celle bordate, testata fissa sopra la lista.
 * <p>
 * Il modulo dice solo COSA mostrare: le colonne (getColonne) e il contenuto di ogni cella (bindCella).
 * Grafica, allineamento testata/righe, bordi, riga di gruppo e riciclo delle view sono qui.
 * <p>
 * Uso: layout dettaglio_tabella_standard.xml (testata in @+id/testata_dettaglio, lista in @+id/lista_dett),
 * poi {@link #collegaTestata(ViewGroup)} sul contenitore della testata. Per i totali statici in basso
 * si usa barra_totali_standard.xml.
 */
public abstract class DettaglioTabellaAdapter extends EConTabListViewAdapter {

	private static final int VISTA_GRUPPO = 0;
	private static final int VISTA_RIGA = 1;

	/** Le celle di una riga, nell'ordine delle colonne (view di contenuto, gia' dentro il bordo). */
	private static final int TAG_CELLE = R.id.tag_celle_dettaglio;

	public DettaglioTabellaAdapter(Context context, ArrayList<Object> dati) {
		super(context, dati, 0);
	}

	/** Colonne della tabella, nell'ordine di visualizzazione. Devono restare le stesse finche' l'adapter e' in uso. */
	protected abstract List<ColonnaDettaglio> getColonne();

	/** Valorizza il contenuto della cella (TextView per TESTO/ICONA, la view di creaCellaCustom per CUSTOM). Va resettata ogni volta: le view sono riciclate. */
	protected abstract void bindCella(int position, ColonnaDettaglio colonna, View cella);

	/** Righe di raggruppamento a tutta larghezza (es. il locale): default nessuna. */
	protected boolean isRigaGruppo(int position) {
		return false;
	}

	/** Testi della riga di gruppo: sopra (piccolo, puo' essere vuoto), titolo, destra (puo' essere vuoto). */
	protected void bindGruppo(int position, TextView sopra, TextView titolo, TextView destra) {
	}

	/** Contenuto di una colonna CUSTOM. */
	protected View creaCellaCustom(ColonnaDettaglio colonna, ViewGroup parent) {
		return new TextView(context);
	}

	/** Imposta una cella ICONA: glifo Font Awesome nel colore di default + tooltip. */
	protected void impostaIcona(View cella, String glifo, CharSequence tooltip) {
		FaIcone.applica((TextView) cella, glifo, tooltip);
	}

	// ── Testata ──────────────────────────────────────────────────────────────

	/** Riempie il contenitore con la testata (fissa: sta fuori dalla lista). Da richiamare se cambiano le colonne. */
	public void collegaTestata(ViewGroup contenitore) {
		contenitore.removeAllViews();
		LinearLayout testata = nuovaRiga();
		for (ColonnaDettaglio c : getColonne()) {
			TextView tv = new TextView(context);
			tv.setText(c.titolo);
			tv.setAllCaps(true);
			tv.setTypeface(null, Typeface.BOLD);
			tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
			tv.setTextColor(Color.parseColor("#616161"));
			tv.setMaxLines(2);
			tv.setGravity(Gravity.CENTER_VERTICAL | (c.tipo == ColonnaDettaglio.Tipo.TESTO ? c.gravity : Gravity.CENTER_HORIZONTAL));
			int p = dp(6);
			tv.setPadding(p, dp(8), p, dp(8));
			if (c.titolo != null && c.titolo.length() > 0) {
				TooltipCompat.setTooltipText(tv, c.titolo);
			}
			testata.addView(incornicia(tv, c, R.drawable.bg_cella_testata));
		}
		contenitore.addView(testata, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	// ── Righe ────────────────────────────────────────────────────────────────

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		return isRigaGruppo(position) ? VISTA_GRUPPO : VISTA_RIGA;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (isRigaGruppo(position)) {
			if (convertView == null) {
				convertView = android.view.LayoutInflater.from(context).inflate(R.layout.list_item_riga_gruppo, parent, false);
			}
			bindGruppo(position, (TextView) convertView.findViewById(R.id.gruppo_sopra),
					(TextView) convertView.findViewById(R.id.gruppo_titolo), (TextView) convertView.findViewById(R.id.gruppo_destra));
			return convertView;
		}

		List<ColonnaDettaglio> colonne = getColonne();
		if (convertView == null) {
			LinearLayout riga = nuovaRiga();
			View[] celle = new View[colonne.size()];
			for (int i = 0; i < colonne.size(); i++) {
				ColonnaDettaglio c = colonne.get(i);
				celle[i] = creaContenuto(c, riga);
				riga.addView(incornicia(celle[i], c, R.drawable.bg_cella_dettaglio));
			}
			riga.setTag(TAG_CELLE, celle);
			convertView = riga;
		}
		View[] celle = (View[]) convertView.getTag(TAG_CELLE);
		for (int i = 0; i < colonne.size(); i++) {
			bindCella(position, colonne.get(i), celle[i]);
		}
		return convertView;
	}

	private View creaContenuto(ColonnaDettaglio c, ViewGroup parent) {
		if (c.tipo == ColonnaDettaglio.Tipo.CUSTOM) {
			return creaCellaCustom(c, parent);
		}
		TextView tv = new TextView(context);
		if (c.tipo == ColonnaDettaglio.Tipo.TESTO) {
			tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			tv.setTextColor(Color.parseColor("#212121"));
			tv.setGravity(Gravity.CENTER_VERTICAL | c.gravity);
			int p = dp(6);
			tv.setPadding(p, dp(4), p, dp(4));
		}
		return tv;
	}

	/** Cella = contenuto dentro un contenitore col bordo grigio e la larghezza della colonna. */
	private View incornicia(View contenuto, ColonnaDettaglio c, int sfondo) {
		FrameLayout cella = new FrameLayout(context);
		cella.setBackgroundResource(sfondo);
		cella.addView(contenuto, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		LinearLayout.LayoutParams lp = c.larghezzaDp > 0
				? new LinearLayout.LayoutParams(dp(c.larghezzaDp), ViewGroup.LayoutParams.MATCH_PARENT)
				: new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, c.peso);
		cella.setLayoutParams(lp);
		return cella;
	}

	private LinearLayout nuovaRiga() {
		LinearLayout riga = new LinearLayout(context);
		riga.setOrientation(LinearLayout.HORIZONTAL);
		riga.setMinimumHeight(dp(44));
		riga.setLayoutParams(new android.widget.AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		return riga;
	}

	protected int dp(int valore) {
		return (int) (valore * context.getResources().getDisplayMetrics().density + 0.5f);
	}
}
