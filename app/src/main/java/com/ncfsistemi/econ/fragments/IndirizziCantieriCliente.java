package com.ncfsistemi.econ.fragments;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import com.ncfsistemi.econ.ClienteIndirizzoModActivity;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.ClientiIndirizzi;
import com.ncfsistemi.econ.utils.CantiereDaIndirizzo;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;
import com.ncfsistemi.econ.utils.Utility;

/**
 * Linguetta "Indirizzi/Cantieri" della scheda cliente (GESTIONE_CLIENTI.md §6.8): due tabelle a colonne con la stessa
 * griglia, sopra gli indirizzi e sotto i cantieri del cliente.
 * <ul>
 * <li>Indirizzi: ★ sede principale, descrizione, indirizzo, civico, CAP, paese, provincia, cantiere, mappa. Casco
 * giallo = il cantiere a questo indirizzo c'e' e si apre; casco verde = lo crea dall'indirizzo (dopo la conferma) e
 * ricarica le tabelle. Tocco = modifica l'indirizzo; tocco lungo = rendi principale, elimina.</li>
 * <li>Cantieri: stesse colonne (nome al posto della descrizione), casco giallo e tocco sulla riga aprono il cantiere.</li>
 * </ul>
 */
public class IndirizziCantieriCliente {

	private final Activity activity;
	private final LinearLayout tabIndirizzi;
	private final LinearLayout tabCantieri;
	private final int cliente;
	private final float dp;

	/** Pesi delle colonne a larghezza variabile: descrizione, indirizzo, civico, CAP, paese, provincia. */
	private static final float[] PESI = {3f, 4f, 1.1f, 1.3f, 2.6f, 1f};
	private static final String[] TITOLI = {"Descrizione", "Indirizzo", "Civico", "CAP", "Paese", "Prov."};
	/** Larghezza delle colonne a icona (dp), tale da leggere per intero l'intestazione. */
	private static final int CANTIERE_DP = 96, MAPPA_DP = 80;

	public IndirizziCantieriCliente(Activity activity, LinearLayout tabIndirizzi, LinearLayout tabCantieri, int cliente) {
		this.activity = activity;
		this.tabIndirizzi = tabIndirizzi;
		this.tabCantieri = tabCantieri;
		this.cliente = cliente;
		this.dp = activity.getResources().getDisplayMetrics().density;
		for (LinearLayout t : new LinearLayout[]{tabIndirizzi, tabCantieri}) {
			// linea grigia tra le righe
			android.graphics.drawable.GradientDrawable linea = new android.graphics.drawable.GradientDrawable();
			linea.setColor(Color.parseColor("#E0E0E0"));
			linea.setSize(1, Math.max(1, Math.round(dp)));
			t.setDividerDrawable(linea);
			t.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE | LinearLayout.SHOW_DIVIDER_END);
		}
	}

	/** Nuovo indirizzo del cliente. */
	public void nuovoIndirizzo() {
		Intent intent = new Intent(activity, ClienteIndirizzoModActivity.class);
		intent.putExtra(ClientiIndirizzi.ID_ANAGRAFICA, cliente);
		activity.startActivity(intent);
	}

	public void carica() {
		DbInterno db = new DbInterno(activity);
		ArrayList<Object> indirizzi = db.eseguiSelect("SELECT * FROM " + ClientiIndirizzi.NOME_TABELLA + " WHERE "
				+ ClientiIndirizzi.ID_ANAGRAFICA + " = " + cliente + " AND coalesce(" + AbstractTable.ATTIVO + ", 1) = 1 ORDER BY "
				+ ClientiIndirizzi.PRINCIPALE + " DESC, " + ClientiIndirizzi.ID, null);
		List<ContentValues> cantieri = CantieriCliente.attivi(db, cliente);
		db.close();
		boolean creaCantieri = CantieriCliente.puoCreare(activity, cliente);
		boolean modifica = FunzionalitaApp.puoModificare(activity, FunzionalitaApp.CLIENTI);

		tabIndirizzi.removeAllViews();
		tabIndirizzi.addView(intestazione("★"));
		if (indirizzi.isEmpty()) tabIndirizzi.addView(vuoto("Nessun indirizzo."));
		for (Object o : indirizzi) {
			tabIndirizzi.addView(rigaIndirizzo((ContentValues) o, cantieri, creaCantieri, modifica));
		}

		tabCantieri.removeAllViews();
		tabCantieri.addView(intestazione(""));
		if (cantieri.isEmpty()) tabCantieri.addView(vuoto("Nessun cantiere."));
		for (ContentValues c : cantieri) {
			tabCantieri.addView(rigaCantiere(c));
		}
	}

	private View rigaIndirizzo(ContentValues v, List<ContentValues> cantieri, boolean creaCantieri, boolean modifica) {
		int id = intero(v, ClientiIndirizzi.ID);
		boolean principale = intero(v, ClientiIndirizzi.PRINCIPALE) == 1;
		String descr = testo(v, ClientiIndirizzi.DESCRIZIONE);
		if (descr.isEmpty()) descr = ClientiIndirizzi.tipoLabel(testo(v, ClientiIndirizzi.TIPO));
		LinearLayout riga = riga(principale ? "★" : "", descr, testo(v, ClientiIndirizzi.NOTE),
				testo(v, ClientiIndirizzi.INDIRIZZO), testo(v, ClientiIndirizzi.CIVICO), testo(v, ClientiIndirizzi.CAP),
				testo(v, ClientiIndirizzi.CITTA), testo(v, ClientiIndirizzi.PROVINCIA));

		ContentValues cantiere = CantieriCliente.aIndirizzo(cantieri, v);
		if (cantiere != null) {
			int idCantiere = intero(cantiere, Cantieri.ID_CANTIERE);
			riga.addView(casco(R.color.casco_esiste, "Apri il cantiere " + testo(cantiere, Cantieri.NOME),
					x -> CantieriCliente.apri(activity, idCantiere)));
		} else if (creaCantieri) {
			riga.addView(casco(R.color.casco_crea, "Crea il cantiere a questo indirizzo", x -> Utility.mostraConfermaDialog(
					"Nuovo cantiere", "Creare il cantiere a " + ClientiIndirizzi.completo(v) + "?", activity, "Crea",
					activity.getString(R.string.annulla), (d, which) -> {
						if (which != android.content.DialogInterface.BUTTON_POSITIVE) return;
						DbInterno db = new DbInterno(activity);
						String esito = CantiereDaIndirizzo.crea(db, id);
						db.close();
						Toast.makeText(activity, esito, Toast.LENGTH_LONG).show();
						carica();
					})));
		} else {
			riga.addView(cellaFissa(CANTIERE_DP));
		}
		riga.addView(mappa(ClientiIndirizzi.completo(v)));

		if (modifica) {
			riga.setOnClickListener(x -> {
				Intent intent = new Intent(activity, ClienteIndirizzoModActivity.class);
				intent.putExtra("ID", id);
				activity.startActivity(intent);
			});
			riga.setOnLongClickListener(x -> {
				azioni(v, id, principale);
				return true;
			});
		}
		return riga;
	}

	private View rigaCantiere(ContentValues c) {
		int id = intero(c, Cantieri.ID_CANTIERE);
		LinearLayout riga = riga("", testo(c, Cantieri.NOME), "", testo(c, Cantieri.INDIRIZZO), testo(c, Cantieri.CIVICO),
				testo(c, Cantieri.CAP), testo(c, Cantieri.CITTA), testo(c, Cantieri.PROVINCIA));
		riga.addView(casco(R.color.casco_esiste, "Apri il cantiere", x -> CantieriCliente.apri(activity, id)));
		String luogo = (testo(c, Cantieri.INDIRIZZO) + " " + testo(c, Cantieri.CIVICO) + ", " + testo(c, Cantieri.CAP) + " "
				+ testo(c, Cantieri.CITTA) + " " + testo(c, Cantieri.PROVINCIA)).trim();
		riga.addView(mappa(luogo));
		riga.setOnClickListener(x -> CantieriCliente.apri(activity, id));
		return riga;
	}

	/** Tocco lungo su un indirizzo: rendi principale, elimina. */
	private void azioni(ContentValues v, int id, boolean principale) {
		List<String> voci = new ArrayList<>();
		List<Runnable> azioni = new ArrayList<>();
		if (!principale) {
			voci.add("Rendi sede principale");
			azioni.add(() -> {
				DbInterno db = new DbInterno(activity);
				db.getWritableDatabase().execSQL("UPDATE " + ClientiIndirizzi.NOME_TABELLA + " SET " + ClientiIndirizzi.PRINCIPALE
						+ " = (" + ClientiIndirizzi.ID + " = ?), " + AbstractTable.IN_SERVER + " = 0 WHERE " + ClientiIndirizzi.ID_ANAGRAFICA
						+ " = ? AND (" + ClientiIndirizzi.PRINCIPALE + " = 1 OR " + ClientiIndirizzi.ID + " = ?)", new Object[]{id, cliente, id});
				db.close();
				carica();
			});
		}
		voci.add("Elimina");
		azioni.add(() -> Utility.mostraConfermaDialog(activity.getString(R.string.attenzione),
				"Eliminare l'indirizzo? I cantieri nati da qui restano.", activity, "Elimina", activity.getString(R.string.annulla), (d, which) -> {
					if (which == android.content.DialogInterface.BUTTON_POSITIVE) {
						DbInterno db = new DbInterno(activity);
						ContentValues k = new ContentValues();
						k.put(ClientiIndirizzi.ID, id);
						new ClientiIndirizzi().cancellaRecord(db, k);
						db.close();
						carica();
					}
				}));
		new AlertDialog.Builder(activity)
				.setTitle(ClientiIndirizzi.completo(v))
				.setItems(voci.toArray(new String[0]), (d, i) -> azioni.get(i).run())
				.show();
	}

	// ── griglia comune alle due tabelle ──

	private LinearLayout intestazione(String prima) {
		LinearLayout r = contenitore();
		r.setBackgroundColor(Color.parseColor("#ECEFF1"));
		r.addView(cella(prima, 0, true, 36));
		for (int i = 0; i < TITOLI.length; i++) r.addView(cella(TITOLI[i], PESI[i], true, 0));
		r.addView(cella("Cantiere", 0, true, CANTIERE_DP));
		r.addView(cella("Mappa", 0, true, MAPPA_DP));
		return r;
	}

	/** Riga con le colonne di testo; poi il chiamante aggiunge casco e mappa. La nota va sotto la descrizione. */
	private LinearLayout riga(String prima, String descrizione, String nota, String... valori) {
		LinearLayout r = contenitore();
		android.util.TypedValue sfondo = new android.util.TypedValue();
		activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, sfondo, true);
		r.setBackgroundResource(sfondo.resourceId);
		r.addView(cella(prima, 0, false, 36));
		// descrizione in grassetto, la nota sotto piu' piccola e normale
		android.text.SpannableStringBuilder testo = new android.text.SpannableStringBuilder(descrizione);
		testo.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, testo.length(), 0);
		if (!nota.isEmpty()) {
			int inizio = testo.length() + 1;
			testo.append("\n").append(nota);
			testo.setSpan(new android.text.style.RelativeSizeSpan(0.8f), inizio, testo.length(), 0);
		}
		TextView d = cella("", PESI[0], false, 0);
		d.setText(testo);
		r.addView(d);
		for (int i = 0; i < valori.length; i++) r.addView(cella(valori[i], PESI[i + 1], false, 0));
		return r;
	}

	private LinearLayout contenitore() {
		LinearLayout r = new LinearLayout(activity);
		r.setOrientation(LinearLayout.HORIZONTAL);
		r.setGravity(Gravity.CENTER_VERTICAL);
		r.setMinimumHeight(Math.round(48 * dp));
		r.setPadding(0, Math.round(4 * dp), 0, Math.round(4 * dp));
		r.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		return r;
	}

	/** Cella di testo: a peso (peso > 0) oppure larga fissa in dp. */
	private TextView cella(String testo, float peso, boolean intestazione, int larghezzaDp) {
		TextView t = new TextView(activity);
		t.setText(testo == null || testo.isEmpty() ? "" : testo);
		t.setPadding(Math.round(6 * dp), 0, Math.round(6 * dp), 0);
		t.setEllipsize(TextUtils.TruncateAt.END);
		if (intestazione) {
			t.setTextSize(13);
			t.setTypeface(null, Typeface.BOLD);
			t.setTextColor(Color.parseColor("#546E7A"));
			t.setMaxLines(1);
		} else {
			t.setTextSize(15);
			t.setTextColor(Color.parseColor("#212121"));
		}
		if (larghezzaDp > 0) {
			t.setGravity(Gravity.CENTER);
			t.setLayoutParams(new LinearLayout.LayoutParams(Math.round(larghezzaDp * dp), ViewGroup.LayoutParams.WRAP_CONTENT));
		} else {
			t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, peso));
		}
		return t;
	}

	private View cellaFissa(int larghezzaDp) {
		View v = new View(activity);
		v.setLayoutParams(new LinearLayout.LayoutParams(Math.round(larghezzaDp * dp), 1));
		return v;
	}

	/** Casco del cantiere, colorato (giallo = apre, verde = crea). */
	private View casco(int colore, String descrizione, View.OnClickListener azione) {
		TextView t = FaIcone.azione(activity, FaIcone.CASCO, descrizione, azione);
		t.setTextColor(ContextCompat.getColor(activity, colore));
		t.setTextSize(22);
		t.setLayoutParams(new LinearLayout.LayoutParams(Math.round(CANTIERE_DP * dp), Math.round(44 * dp)));
		return t;
	}

	private View mappa(String indirizzo) {
		TextView t = FaIcone.azione(activity, FaIcone.MAPPA, "Mappa", x -> Utility.mostraMappa(activity, indirizzo));
		t.setLayoutParams(new LinearLayout.LayoutParams(Math.round(MAPPA_DP * dp), Math.round(44 * dp)));
		return t;
	}

	private TextView vuoto(String testo) {
		TextView t = new TextView(activity);
		t.setText(testo);
		t.setPadding(Math.round(6 * dp), Math.round(12 * dp), 0, Math.round(12 * dp));
		return t;
	}

	private static int intero(ContentValues v, String campo) {
		Integer i = v.getAsInteger(campo);
		return i == null ? 0 : i;
	}

	private static String testo(ContentValues v, String campo) {
		String s = v.getAsString(campo);
		return s == null ? "" : s.trim();
	}
}
