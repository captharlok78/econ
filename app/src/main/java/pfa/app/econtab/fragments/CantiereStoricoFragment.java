package pfa.app.econtab.fragments;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pfa.app.econtab.R;
import pfa.app.econtab.RapportinoDettaglioModActivity;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.AbstractTable;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Listini;
import pfa.app.econtab.db.table.Rapportini;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.db.table.RapportiniDettaglioTipi;
import pfa.app.econtab.db.table.StatiDocumento;
import pfa.app.econtab.db.table.UnitaMisura;
import pfa.app.econtab.db.table.Utenti;
import pfa.app.econtab.utils.FaIcone;
import pfa.app.econtab.utils.RegoleRapportino;
import pfa.app.econtab.utils.SezioneStandard;
import pfa.app.econtab.utils.SyncUtil;
import pfa.app.econtab.utils.Utility;

/**
 * Linguette "Rapportini" e "Materiali" della scheda del cantiere (STORICO_CANTIERE.md): lo storico che c'e' sul tablet,
 * cioe' i rapportini dentro il perimetro dell'app (ultimi N giorni della ditta e quelli ancora in bozza). Lo storico
 * completo e' nella scheda del cantiere sul portale.
 * <ul>
 * <li>Rapportini: numero, data, stato, autore, note, ore; tocco = apre il rapportino.</li>
 * <li>Materiali: articoli usati raggruppati per articolo e unita' di misura (dal piu' usato); tocco = movimenti,
 * e dai movimenti si apre il rapportino.</li>
 * </ul>
 * I rapportini disattivati non contano, come sul portale.
 */
public class CantiereStoricoFragment extends EConTabFragment {

	public static final String MODO = "MODO_STORICO";
	public static final String RAPPORTINI = "RAPPORTINI";
	public static final String MATERIALI = "MATERIALI";

	private int cantiere = 0;
	private String modo = RAPPORTINI;
	private float dp;
	/** Larghezze in px delle colonne fisse della tabella in costruzione. */
	private int[] larghezze;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.fragment_cantiere_storico, container, false);
		cantiere = getArguments().getInt(Cantieri.ID_CANTIERE);
		modo = getArguments().getString(MODO, RAPPORTINI);
		dp = getResources().getDisplayMetrics().density;
		SezioneStandard.imposta(v.findViewById(R.id.sezione_storico), RAPPORTINI.equals(modo) ? FaIcone.ELENCO : FaIcone.MATERIALE,
				RAPPORTINI.equals(modo) ? "Rapportini del cantiere" : "Materiali usati");
		LinearLayout tabella = v.findViewById(R.id.tabellaStorico);
		GradientDrawable linea = new GradientDrawable();
		linea.setColor(Color.parseColor("#E0E0E0"));
		linea.setSize(1, Math.max(1, Math.round(dp)));
		tabella.setDividerDrawable(linea);
		tabella.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE | LinearLayout.SHOW_DIVIDER_END);
		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		// anche al ritorno da un rapportino modificato
		carica();
	}

	private void carica() {
		View v = getView();
		if (v == null) return;
		LinearLayout tabella = v.findViewById(R.id.tabellaStorico);
		tabella.removeAllViews();
		DbInterno db = new DbInterno(getActivity());
		try {
			if (RAPPORTINI.equals(modo)) {
				caricaRapportini(db, tabella, v.findViewById(R.id.textRiepilogo));
			} else {
				caricaMateriali(db, tabella, v.findViewById(R.id.textRiepilogo));
			}
		} finally {
			db.close();
		}
		int giorni = SyncUtil.giorniStorico(getActivity());
		((TextView) v.findViewById(R.id.textNotaStorico)).setText((giorni > 0
				? "Sul tablet ci sono i rapportini degli ultimi " + giorni + " giorni e quelli ancora in bozza."
				: "Sul tablet ci sono solo i rapportini recenti e quelli ancora in bozza.")
				+ " Lo storico completo del cantiere e' sul portale, nella scheda del cantiere.");
	}

	/** Condizione comune: rapportini attivi di questo cantiere (alias r). */
	private String whereRapportini() {
		return "r." + Rapportini.ID_CANTIERE + " = " + cantiere + " AND coalesce(r." + AbstractTable.ATTIVO + ", 1) = 1";
	}

	// ── Rapportini ──

	/**
	 * Colonne: peso (> 0) per i testi; 0 = larghezza fissa su una riga sola, misurata sul campione (CAMPIONI_*) e
	 * sull'intestazione con il carattere reale (il tablet puo' avere i caratteri ingranditi).
	 */
	private static final float[] PESI_RAP = {0, 0, 2.2f, 2f, 2.4f, 0};
	private static final String[] CAMPIONI_RAP = {"999/2026", "30/09/26", null, null, null, "999:59"};
	private static final String[] TITOLI_RAP = {"N.", "Data", "Stato", "Autore", "Note", "Ore"};

	private void caricaRapportini(DbInterno db, LinearLayout tabella, TextView riepilogo) {
		String sql = "SELECT r." + Rapportini.ID + ", r." + Rapportini.NUMERO + ", r." + Rapportini.ANNO + ", r." + Rapportini.DATA_RAPPORTINO
				+ ", r." + Rapportini.NOTE + ", s." + StatiDocumento.NOME + " AS nome_stato, s." + StatiDocumento.COLORE + " AS colore_stato"
				+ ", trim(coalesce(o." + Utenti.NOME + ", '') || ' ' || coalesce(o." + Utenti.COGNOME + ", '')) AS autore"
				+ ", (SELECT sum(" + RapportiniDettaglio.sqlOreUomo("d", "t", "u") + ") FROM " + RapportiniDettaglio.NOME_TABELLA + " d"
				+ " INNER JOIN " + RapportiniDettaglioTipi.NOME_TABELLA + " t ON t." + RapportiniDettaglioTipi.ID + " = d." + RapportiniDettaglio.ID_TIPO
				+ " LEFT JOIN " + UnitaMisura.NOME_TABELLA + " u ON u." + UnitaMisura.ID + " = d." + RapportiniDettaglio.ID_UNITA_MISURA
				+ " WHERE d." + RapportiniDettaglio.ID_RAPPORTINO + " = r." + Rapportini.ID + ") AS tot_ore"
				+ " FROM " + Rapportini.NOME_TABELLA + " r"
				+ " LEFT JOIN " + StatiDocumento.NOME_TABELLA + " s ON s." + StatiDocumento.ID + " = r." + Rapportini.ID_STATO
				+ " LEFT JOIN " + Utenti.NOME_TABELLA + " o ON o." + Utenti.ID_UTENTE_DITTA + " = r." + Rapportini.ID_UTENTE_DITTA
				+ " WHERE " + whereRapportini()
				+ " ORDER BY r." + Rapportini.DATA_RAPPORTINO + " DESC, r." + Rapportini.ID + " DESC";
		ArrayList<Object> righe = db.eseguiSelect(sql, null);

		double totOre = 0;
		larghezze = larghezze(TITOLI_RAP, CAMPIONI_RAP);
		tabella.addView(intestazione(TITOLI_RAP, PESI_RAP));
		if (righe.isEmpty()) tabella.addView(vuoto("Nessun rapportino di questo cantiere sul tablet."));
		for (Object o : righe) {
			ContentValues r = (ContentValues) o;
			double ore = decimale(r, "tot_ore");
			totOre += ore;
			LinearLayout riga = contenitore(true);
			riga.addView(cella(numero(r), PESI_RAP, 0, false, true));
			riga.addView(cella(data(r.getAsLong(Rapportini.DATA_RAPPORTINO)), PESI_RAP, 1, false, false));
			TextView stato = cella(testo(r, "nome_stato"), PESI_RAP, 2, false, true);
			stato.setTextColor(RegoleRapportino.coloreStato(r.getAsString("colore_stato")));
			riga.addView(stato);
			riga.addView(cella(testo(r, "autore"), PESI_RAP, 3, false, false));
			riga.addView(cella(testo(r, Rapportini.NOTE), PESI_RAP, 4, false, false));
			TextView cOre = cella(oreMinuti(ore), PESI_RAP, 5, false, false);
			cOre.setGravity(Gravity.END);
			riga.addView(cOre);
			int id = r.getAsInteger(Rapportini.ID);
			riga.setOnClickListener(x -> apriRapportino(id));
			tabella.addView(riga);
		}
		riepilogo.setText(righe.size() + (righe.size() == 1 ? " rapportino" : " rapportini") + " · " + oreMinuti(totOre) + " ore");
	}

	// ── Materiali ──

	private static final float[] PESI_MAT = {1.6f, 4f, 0, 0, 0, 0};
	private static final String[] CAMPIONI_MAT = {null, null, "PZ", "9999", "999", "30/09/26"};
	private static final String[] TITOLI_MAT = {"Codice", "Articolo", "U.M.", "Q.tà", "Rapp.", "Ultimo"};

	/** Un articolo (con unita' di misura) e i suoi movimenti, dal piu' recente. */
	private static class Materiale {
		String codice, descrizione, um;
		double quantita;
		long ultimo;
		final List<ContentValues> movimenti = new ArrayList<>();
		final java.util.Set<Integer> rapportini = new java.util.HashSet<>();
	}

	private void caricaMateriali(DbInterno db, LinearLayout tabella, TextView riepilogo) {
		String sql = "SELECT d." + RapportiniDettaglio.ID_LISTINO + ", l." + Listini.CODICE_ARTICOLO + ", l." + Listini.DESCRIZIONE
				+ ", u." + UnitaMisura.UNITA_MISURA + " AS um, d." + RapportiniDettaglio.QUANTITA + ", d." + RapportiniDettaglio.NOTE + " AS nota_riga"
				+ ", r." + Rapportini.ID + " AS id_rapportino, r." + Rapportini.NUMERO + ", r." + Rapportini.ANNO + ", r." + Rapportini.DATA_RAPPORTINO
				+ " FROM " + RapportiniDettaglio.NOME_TABELLA + " d"
				+ " INNER JOIN " + RapportiniDettaglioTipi.NOME_TABELLA + " t ON t." + RapportiniDettaglioTipi.ID + " = d." + RapportiniDettaglio.ID_TIPO
				+ " AND t." + RapportiniDettaglioTipi.CATEGORIA + " = '" + RapportiniDettaglioTipi.CATEGORIA_MATERIALE + "'"
				+ " INNER JOIN " + Rapportini.NOME_TABELLA + " r ON r." + Rapportini.ID + " = d." + RapportiniDettaglio.ID_RAPPORTINO
				+ " LEFT JOIN " + Listini.NOME_TABELLA + " l ON l." + Listini.ID + " = d." + RapportiniDettaglio.ID_LISTINO
				+ " LEFT JOIN " + UnitaMisura.NOME_TABELLA + " u ON u." + UnitaMisura.ID + " = d." + RapportiniDettaglio.ID_UNITA_MISURA
				+ " WHERE " + whereRapportini()
				+ " ORDER BY r." + Rapportini.DATA_RAPPORTINO + " DESC, r." + Rapportini.ID + " DESC, d." + RapportiniDettaglio.ID;
		ArrayList<Object> righe = db.eseguiSelect(sql, null);

		Map<String, Materiale> gruppi = new LinkedHashMap<>();
		for (Object o : righe) {
			ContentValues m = (ContentValues) o;
			String chiave = testo(m, RapportiniDettaglio.ID_LISTINO) + "-" + testo(m, "um");
			Materiale g = gruppi.get(chiave);
			if (g == null) {
				g = new Materiale();
				g.codice = testo(m, Listini.CODICE_ARTICOLO);
				g.descrizione = testo(m, Listini.DESCRIZIONE);
				if (g.descrizione.isEmpty()) g.descrizione = "Articolo non presente sul tablet";
				g.um = testo(m, "um");
				g.ultimo = m.getAsLong(Rapportini.DATA_RAPPORTINO);
				gruppi.put(chiave, g);
			}
			g.quantita += decimale(m, RapportiniDettaglio.QUANTITA);
			g.rapportini.add(m.getAsInteger("id_rapportino"));
			g.movimenti.add(m);
		}
		List<Materiale> materiali = new ArrayList<>(gruppi.values());
		java.util.Collections.sort(materiali, (a, b) -> a.rapportini.size() != b.rapportini.size()
				? b.rapportini.size() - a.rapportini.size() : a.descrizione.compareToIgnoreCase(b.descrizione));

		larghezze = larghezze(TITOLI_MAT, CAMPIONI_MAT);
		tabella.addView(intestazione(TITOLI_MAT, PESI_MAT));
		if (materiali.isEmpty()) tabella.addView(vuoto("Nessun materiale nei rapportini di questo cantiere sul tablet."));
		for (Materiale g : materiali) {
			LinearLayout riga = contenitore(true);
			riga.addView(cella(g.codice, PESI_MAT, 0, false, false));
			riga.addView(cella(g.descrizione, PESI_MAT, 1, false, true));
			riga.addView(cella(g.um, PESI_MAT, 2, false, false));
			TextView q = cella(quantita(g.quantita), PESI_MAT, 3, false, true);
			q.setGravity(Gravity.END);
			riga.addView(q);
			TextView n = cella(String.valueOf(g.rapportini.size()), PESI_MAT, 4, false, false);
			n.setGravity(Gravity.END);
			riga.addView(n);
			riga.addView(cella(data(g.ultimo), PESI_MAT, 5, false, false));
			riga.setOnClickListener(x -> movimenti(g));
			tabella.addView(riga);
		}
		riepilogo.setText(materiali.size() + (materiali.size() == 1 ? " articolo" : " articoli") + " · "
				+ righe.size() + (righe.size() == 1 ? " movimento" : " movimenti") + (materiali.isEmpty() ? "" : " · tocca un articolo per i movimenti"));
	}

	/** Movimenti di un articolo: data, rapportino, quantita', nota; tocco = apre il rapportino. */
	private void movimenti(Materiale g) {
		String[] voci = new String[g.movimenti.size()];
		for (int i = 0; i < voci.length; i++) {
			ContentValues m = g.movimenti.get(i);
			String nota = testo(m, "nota_riga");
			voci[i] = data(m.getAsLong(Rapportini.DATA_RAPPORTINO)) + "  ·  " + numero(m) + "  ·  " + quantita(decimale(m, RapportiniDettaglio.QUANTITA))
					+ " " + g.um + (nota.isEmpty() ? "" : "  ·  " + nota);
		}
		new AlertDialog.Builder(getActivity())
				.setTitle((g.codice.isEmpty() ? "" : g.codice + " ") + g.descrizione)
				.setItems(voci, (d, i) -> apriRapportino(g.movimenti.get(i).getAsInteger("id_rapportino")))
				.setNegativeButton(R.string.annulla, null)
				.show();
	}

	private void apriRapportino(int id) {
		Intent intent = new Intent(getActivity(), RapportinoDettaglioModActivity.class);
		intent.putExtra("ID", id);
		startActivity(intent);
	}

	// ── griglia ──

	private LinearLayout intestazione(String[] titoli, float[] pesi) {
		LinearLayout r = contenitore(false);
		r.setBackgroundColor(Color.parseColor("#ECEFF1"));
		for (int i = 0; i < titoli.length; i++) r.addView(cella(titoli[i], pesi, i, true, false));
		return r;
	}

	private LinearLayout contenitore(boolean toccabile) {
		LinearLayout r = new LinearLayout(getActivity());
		r.setOrientation(LinearLayout.HORIZONTAL);
		r.setGravity(Gravity.CENTER_VERTICAL);
		r.setMinimumHeight(Math.round(44 * dp));
		r.setPadding(0, Math.round(4 * dp), 0, Math.round(4 * dp));
		r.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		if (toccabile) {
			android.util.TypedValue sfondo = new android.util.TypedValue();
			getActivity().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, sfondo, true);
			r.setBackgroundResource(sfondo.resourceId);
		}
		return r;
	}

	/** Larghezza (px) di ogni colonna fissa: il piu' largo tra intestazione e campione, in grassetto, piu' il margine. */
	private int[] larghezze(String[] titoli, String[] campioni) {
		int[] l = new int[titoli.length];
		for (int i = 0; i < titoli.length; i++) {
			if (campioni[i] == null) continue;
			TextView misura = cella("", 1, false, true);
			float testo = misura.getPaint().measureText(campioni[i]);
			TextView misuraTitolo = cella("", 1, true, false);
			float titolo = misuraTitolo.getPaint().measureText(titoli[i]);
			l[i] = (int) Math.ceil(Math.max(testo, titolo)) + Math.round(16 * dp);
		}
		return l;
	}

	/** Cella della colonna i: a peso, oppure (peso 0) larga quanto calcolato in larghezze. */
	private TextView cella(String testo, float[] pesi, int i, boolean intestazione, boolean grassetto) {
		TextView t = cella(testo, pesi[i], intestazione, grassetto);
		if (pesi[i] == 0) {
			t.setMaxLines(1);
			t.setLayoutParams(new LinearLayout.LayoutParams(larghezze[i], ViewGroup.LayoutParams.WRAP_CONTENT));
		}
		return t;
	}

	private TextView cella(String testo, float peso, boolean intestazione, boolean grassetto) {
		TextView t = new TextView(getActivity());
		t.setText(testo == null ? "" : testo);
		t.setPadding(Math.round(6 * dp), 0, Math.round(6 * dp), 0);
		t.setEllipsize(TextUtils.TruncateAt.END);
		t.setMaxLines(intestazione ? 1 : 2);
		if (intestazione) {
			t.setTextSize(13);
			t.setTypeface(null, Typeface.BOLD);
			t.setTextColor(Color.parseColor("#546E7A"));
		} else {
			t.setTextSize(15);
			t.setTextColor(Color.parseColor("#212121"));
			if (grassetto) t.setTypeface(null, Typeface.BOLD);
		}
		t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, peso));
		return t;
	}

	private TextView vuoto(String testo) {
		TextView t = new TextView(getActivity());
		t.setText(testo);
		t.setPadding(Math.round(6 * dp), Math.round(12 * dp), 0, Math.round(12 * dp));
		return t;
	}

	// ── formati ──

	/** "12/2026", "bozza" senza numero (NUMERAZIONE_DOCUMENTI.md). */
	private static String numero(ContentValues r) {
		Integer n = r.getAsInteger(Rapportini.NUMERO);
		return n != null && n > 0 ? n + "/" + r.getAsInteger(Rapportini.ANNO) : "bozza";
	}

	private static String data(Long data) {
		return data == null || data == 0 ? "" : Utility.numberToDataShort(data);
	}

	/** Ore come h:mm. */
	private static String oreMinuti(double ore) {
		long minuti = Math.round(ore * 60);
		return (minuti / 60) + ":" + String.format(java.util.Locale.ITALY, "%02d", minuti % 60);
	}

	/** Quantita' senza decimali inutili (3, 2,5). */
	private static String quantita(double q) {
		return q == Math.rint(q) ? String.valueOf((long) q) : Utility.formatNumero(q);
	}

	private static double decimale(ContentValues v, String campo) {
		Double d = v.getAsDouble(campo);
		return d == null ? 0 : d;
	}

	private static String testo(ContentValues v, String campo) {
		String s = v.getAsString(campo);
		return s == null ? "" : s.trim();
	}
}
