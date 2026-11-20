package com.ncfsistemi.econ.dialogo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.utils.Utility;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Solo debug: tutti i tipi di modale dell'app in una pagina, per controllarne l'aspetto senza login
 * (STUDIO_MODALI_APP.md). Extra "mostra" = nome della voce da aprire subito.
 */
public class GalleriaModaliActivity extends AppCompatActivity {

	private final Map<String, Runnable> voci = new LinkedHashMap<>();

	@Override
	protected void onCreate(Bundle stato) {
		super.onCreate(stato);
		voci.put("avviso", () -> EconDialogo.avviso(this, "Sincronizzazione", "Dati inviati al server.").show());
		voci.put("attenzione", () -> Utility.mostraDialog("Attenzione", "Il rapportino non ha righe di manodopera.", this, "OK"));
		voci.put("errore", () -> EconDialogo.errore(this, "Errore di connessione", "Il server non risponde. Riprova tra poco.").show());
		voci.put("conferma", () -> Utility.mostraConfermaSalvataggioDialog(this, null));
		voci.put("eliminazione", () -> Utility.mostraConfermaCancellazioneDialog(this, null));
		voci.put("scelta", () -> Utility.mostraSelezioneDialog("Modello di stampa", new CharSequence[] { "Rapportino standard",
				"Rapportino con prezzi", "Rapportino senza firme", "Riepilogo ore" }, this, null));
		voci.put("multipla", () -> Utility.mostraSelezioneMultiplaDialog("Operatori", new CharSequence[] { "Mario Rossi", "Luca Bianchi",
				"Anna Verdi" }, new boolean[3], this, "OK", "Annulla", null));
		voci.put("attesa", () -> {
			EconDialogo.Attesa a = EconDialogo.attesa(this, "Creo il report del preventivo…");
			a.avanzamento(40);
			new Handler(Looper.getMainLooper()).postDelayed(a::chiudi, 6000);
		});
		voci.put("attesavecchia", () -> new EconDialogo.Builder(this).setMessage("Creazione report in corso…").show());
		voci.put("campounico", () -> {
			android.widget.EditText e = new android.widget.EditText(this);
			e.setHint("Nome del locale preferito");
			Utility.mostraDialogPersonalizzato("Locale preferito", this, e, "Salva", "Annulla", null);
		});
		voci.put("password", () -> {
			LinearLayout c = EconDialogo.corpo(this);
			c.addView(EconDialogo.nota(this, "La nuova password vale per l'app e per il portale web."));
			c.addView(EconDialogo.password(this, "Password attuale"));
			c.addView(EconDialogo.password(this, "Nuova password (almeno 8 caratteri)"));
			c.addView(EconDialogo.password(this, "Conferma nuova password"));
			EconDialogo.nuovo(this).icona(com.ncfsistemi.econ.utils.FaIcone.CHIAVE, EconDialogo.Tono.INFO).setTitle("Cambia password")
					.setView(c).setPositiveButton("Conferma", null).setNegativeButton("Annulla", null).show();
		});
		for (String[] l : new String[][] { { "qta", "dialog_qtacomponente_layout" }, { "note", "dialog_note_layout" },
				{ "ricarico", "dialog_ricarico_layout" }, { "spunta", "dialog_checkbox_layout" }, { "rigaprev", "dialog_edit_rigaprev_layout" },
				{ "codice", "dialog_edit_codice_articolo" }, { "placca", "dialog_aggiungi_placca" }, { "notaprev", "dialog_nota_preventivo_layout" },
				{ "titoloprev", "dialog_edit_rigaprev_titolo_layout" }, { "riga", "dialog_riga_rapportino" }, { "manodopera", "dialog_sel_manodopera_layout" } }) {
			voci.put(l[0], () -> {
				int id = getResources().getIdentifier(l[1], "layout", getPackageName());
				View v = View.inflate(this, id, null);
				Utility.mostraDialogPersonalizzato(l[1], this, v, "Salva", "Annulla", null);
			});
		}

		for (String cat : new String[] { "rigaman", "rigaart" }) {
			voci.put(cat, () -> {
				View v = View.inflate(this, com.ncfsistemi.econ.R.layout.dialog_riga_rapportino, null);
				boolean art = cat.equals("rigaart");
				v.findViewById(com.ncfsistemi.econ.R.id.sezione_articolo).setVisibility(art ? View.VISIBLE : View.GONE);
				v.findViewById(com.ncfsistemi.econ.R.id.sezione_utente).setVisibility(art ? View.GONE : View.VISIBLE);
				v.findViewById(com.ncfsistemi.econ.R.id.sezione_orari).setVisibility(View.GONE);
				v.findViewById(com.ncfsistemi.econ.R.id.sezione_riga_ordine).setVisibility(View.GONE);
				for (int sp : new int[] { com.ncfsistemi.econ.R.id.spinner_tipo, com.ncfsistemi.econ.R.id.spinner_utente, com.ncfsistemi.econ.R.id.spinner_unita,
						com.ncfsistemi.econ.R.id.spinner_costruttore, com.ncfsistemi.econ.R.id.spinner_linea }) {
					((android.widget.Spinner) v.findViewById(sp)).setAdapter(new android.widget.ArrayAdapter<>(this,
							com.ncfsistemi.econ.R.layout.econ_tendina_valore, new String[] { "Voce di prova" }));
				}
				((com.google.android.material.textfield.TextInputLayout) v.findViewById(com.ncfsistemi.econ.R.id.label_prezzo)).setHint("Costo fisso");
				androidx.appcompat.app.AlertDialog d = new EconDialogo.Builder(this).setTitle(art ? "Articolo" : "Manodopera").setView(v)
						.setPositiveButton("Inserisci", null).setNegativeButton("Annulla", null).create();
				d.show();
				com.ncfsistemi.econ.views.PopupRigaRapportino.adattaAlloSchermo(this, v, d);
			});
		}

		// struttura del cantiere: pannello finto (barra a icone, "+", schermo intero) senza dati veri
		voci.put("struttura", this::strutturaFinta);
		voci.put("strutturanuovo", () -> com.ncfsistemi.econ.disegno.StrutturaCantiere.nuovo(this, 1, null, null));
		voci.put("strutturagrande", () -> {
			java.util.List<Object> d = righeFinte();
			com.ncfsistemi.econ.disegno.StrutturaCantiere.schermoIntero(this, "Struttura del cantiere", "Villa Rossi", d, selezioneFinta(),
					() -> { }, 1, null, null, pos -> { });
		});

		ScrollView sv = new ScrollView(this);
		LinearLayout l = new LinearLayout(this);
		l.setOrientation(LinearLayout.VERTICAL);
		l.setPadding(40, 40, 40, 40);
		sv.addView(l);
		for (Map.Entry<String, Runnable> e : voci.entrySet()) {
			Button b = new com.google.android.material.button.MaterialButton(this);
			b.setText(e.getKey());
			b.setOnClickListener(v -> e.getValue().run());
			l.addView(b);
		}
		setContentView(sv);
		String mostra = getIntent().getStringExtra("mostra");
		if (mostra != null && voci.containsKey(mostra)) sv.post(() -> voci.get(mostra).run());
	}

	private java.util.List<Object> righeFinte() {
		java.util.List<Object> d = new java.util.ArrayList<>();
		d.add(com.ncfsistemi.econ.disegno.StrutturaCantiere.riga(com.ncfsistemi.econ.CantiereSplitActivity.CANTIERE, 1, "Villa Rossi"));
		d.add(com.ncfsistemi.econ.disegno.StrutturaCantiere.riga(com.ncfsistemi.econ.CantiereSplitActivity.UNITA, 1, "Abitazione"));
		String[][] aree = { { "Piano terra", "0" }, { "Primo piano", "1" } };
		int id = 1;
		for (String[] a : aree) {
			android.content.ContentValues r = com.ncfsistemi.econ.disegno.StrutturaCantiere.riga(com.ncfsistemi.econ.CantiereSplitActivity.AREA, id, a[0]);
			r.put(com.ncfsistemi.econ.disegno.StrutturaCantiere.LIVELLO, Integer.parseInt(a[1]));
			d.add(r);
			for (String loc : new String[] { "Cucina", "Soggiorno", "Bagno", "Camera" }) {
				android.content.ContentValues l = com.ncfsistemi.econ.disegno.StrutturaCantiere.riga(com.ncfsistemi.econ.CantiereSplitActivity.LOCALE, id * 10 + d.size(), loc);
				l.put(com.ncfsistemi.econ.disegno.StrutturaCantiere.N_ELEMENTI, d.size() % 4);
				d.add(l);
			}
			id++;
		}
		return d;
	}

	private com.ncfsistemi.econ.disegno.StrutturaCantiere.Selezione selezioneFinta() {
		return new com.ncfsistemi.econ.disegno.StrutturaCantiere.Selezione() {
			@Override
			public boolean selezionata(int posizione, android.content.ContentValues riga) {
				return posizione == 4;
			}

			@Override
			public boolean attiva(int posizione, android.content.ContentValues riga) {
				return true;
			}
		};
	}

	/** Pannello della struttura largo come nella scheda cantiere, con righe di esempio. */
	private void strutturaFinta() {
		java.util.List<Object> d = righeFinte();
		LinearLayout pannello = new LinearLayout(this);
		pannello.setOrientation(LinearLayout.VERTICAL);
		pannello.setBackgroundColor(0xFFF4F6FA);
		android.widget.ListView lista = new android.widget.ListView(this);
		com.ncfsistemi.econ.disegno.StrutturaCantiere.Adapter a = new com.ncfsistemi.econ.disegno.StrutturaCantiere.Adapter(this, d, selezioneFinta());
		pannello.addView(com.ncfsistemi.econ.disegno.StrutturaCantiere.barra(this, a::notifyDataSetChanged, 1, null,
				() -> com.ncfsistemi.econ.disegno.StrutturaCantiere.schermoIntero(this, "Struttura del cantiere", "Villa Rossi", d, selezioneFinta(),
						a::notifyDataSetChanged, 1, null, null, pos -> { })));
		com.ncfsistemi.econ.disegno.StrutturaCantiere.stileLista(lista);
		lista.setAdapter(a);
		pannello.addView(lista, new LinearLayout.LayoutParams(-1, 0, 1f));
		LinearLayout riga = new LinearLayout(this);
		riga.addView(pannello, new LinearLayout.LayoutParams(Math.round(300 * getResources().getDisplayMetrics().density), -1));
		setContentView(riga);
	}
}
