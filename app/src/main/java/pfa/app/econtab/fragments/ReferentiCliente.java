package pfa.app.econtab.fragments;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import pfa.app.econtab.ClienteReferenteModActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.AbstractTable;
import pfa.app.econtab.db.table.ClientiReferenti;
import pfa.app.econtab.utils.FaIcone;
import pfa.app.econtab.utils.FunzionalitaApp;
import pfa.app.econtab.utils.Utility;

/**
 * Referenti del cliente nella linguetta "Referenti" della scheda cliente (GESTIONE_CLIENTI.md §6.8): pulsanti per
 * chiamare e scrivere; tocco = modifica, tocco lungo = elimina.
 */
public class ReferentiCliente {

	private final Activity activity;
	private final LinearLayout lista;
	private final int cliente;

	public ReferentiCliente(Activity activity, LinearLayout lista, int cliente) {
		this.activity = activity;
		this.lista = lista;
		this.cliente = cliente;
	}

	/** Apre il nuovo referente del cliente. */
	public void nuovo() {
		Intent intent = new Intent(activity, ClienteReferenteModActivity.class);
		intent.putExtra(ClientiReferenti.ID_ANAGRAFICA, cliente);
		activity.startActivity(intent);
	}

	/** Righe dei referenti attivi del cliente. */
	public void carica() {
		lista.removeAllViews();
		DbInterno db = new DbInterno(activity);
		ArrayList<Object> righe = db.eseguiSelect("SELECT * FROM " + ClientiReferenti.NOME_TABELLA + " WHERE "
				+ ClientiReferenti.ID_ANAGRAFICA + " = " + cliente + " AND coalesce(" + AbstractTable.ATTIVO + ", 1) = 1 ORDER BY "
				+ ClientiReferenti.NOME, null);
		db.close();
		if (righe.isEmpty()) {
			TextView vuoto = new TextView(activity);
			vuoto.setText("Nessun referente.");
			vuoto.setPadding(0, 12, 0, 0);
			lista.addView(vuoto);
		}
		boolean modifica = FunzionalitaApp.puoModificare(activity, FunzionalitaApp.CLIENTI);
		for (Object o : righe) {
			lista.addView(riga((ContentValues) o, modifica));
		}
	}

	private View riga(ContentValues r, boolean modifica) {
		LinearLayout riga = new LinearLayout(activity);
		riga.setOrientation(LinearLayout.HORIZONTAL);
		riga.setPadding(0, 12, 0, 12);
		riga.setGravity(Gravity.CENTER_VERTICAL);

		TextView testo = new TextView(activity);
		String ruolo = valore(r, ClientiReferenti.RUOLO);
		String recapiti = String.join("  ·  ", java.util.stream.Stream.of(valore(r, ClientiReferenti.TELEFONO),
				valore(r, ClientiReferenti.CELLULARE), valore(r, ClientiReferenti.EMAIL)).filter(x -> !x.isEmpty()).toArray(String[]::new));
		testo.setText(Html.fromHtml("<b>" + TextUtils.htmlEncode(valore(r, ClientiReferenti.NOME)) + "</b>"
				+ (ruolo.isEmpty() ? "" : " — " + TextUtils.htmlEncode(ruolo))
				+ (recapiti.isEmpty() ? "" : "<br><small>" + TextUtils.htmlEncode(recapiti) + "</small>"), Html.FROM_HTML_MODE_LEGACY));
		riga.addView(testo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

		String tel = !valore(r, ClientiReferenti.CELLULARE).isEmpty() ? valore(r, ClientiReferenti.CELLULARE) : valore(r, ClientiReferenti.TELEFONO);
		if (!tel.isEmpty()) {
			riga.addView(pulsante(FaIcone.TELEFONO, "Chiama", new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tel))));
		}
		String email = valore(r, ClientiReferenti.EMAIL);
		if (!email.isEmpty()) {
			riga.addView(pulsante(FaIcone.EMAIL, "Scrivi", new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email))));
		}
		int id = r.getAsInteger(ClientiReferenti.ID);
		if (modifica) {
			riga.setOnClickListener(x -> {
				Intent intent = new Intent(activity, ClienteReferenteModActivity.class);
				intent.putExtra("ID", id);
				activity.startActivity(intent);
			});
			riga.setOnLongClickListener(x -> {
				Utility.mostraConfermaDialog(activity.getString(R.string.attenzione), "Eliminare il referente " + valore(r, ClientiReferenti.NOME) + "?",
						activity, "Elimina", activity.getString(R.string.annulla), (d, which) -> {
							if (which == android.content.DialogInterface.BUTTON_POSITIVE) {
								DbInterno db = new DbInterno(activity);
								ContentValues k = new ContentValues();
								k.put(ClientiReferenti.ID, id);
								new ClientiReferenti().cancellaRecord(db, k);
								db.close();
								carica();
							}
						});
				return true;
			});
		}
		return riga;
	}

	/** Azione a icona dello standard grafico che apre un'altra app (telefono, posta). */
	private View pulsante(String glifo, String descrizione, Intent azione) {
		return FaIcone.azione(activity, glifo, descrizione, x -> {
			try {
				activity.startActivity(azione);
			} catch (android.content.ActivityNotFoundException e) {
				Toast.makeText(activity, "Nessuna app per " + descrizione.toLowerCase(), Toast.LENGTH_SHORT).show();
			}
		});
	}

	private static String valore(ContentValues r, String campo) {
		String s = r.getAsString(campo);
		return s == null ? "" : s.trim();
	}
}
