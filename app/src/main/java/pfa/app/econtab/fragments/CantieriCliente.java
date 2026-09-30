package pfa.app.econtab.fragments;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import pfa.app.econtab.CantiereSplitActivity;
import pfa.app.econtab.CantieriDettaglioModActivity;
import pfa.app.econtab.EConTabActivity;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.AbstractTable;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.ClientiIndirizzi;
import pfa.app.econtab.utils.FunzionalitaApp;

/**
 * Regole sui cantieri del cliente per la linguetta "Indirizzi/Cantieri" della scheda cliente (GESTIONE_CLIENTI.md §6.8):
 * quali sono, quale sta a un indirizzo, chi puo' crearne, come aprirli e il nuovo cantiere libero.
 */
public class CantieriCliente {

	private CantieriCliente() {
	}

	/** Form del nuovo cantiere "libero" del cliente: cliente scelto, nome e indirizzo a mano. */
	public static void nuovoLibero(Activity activity, int cliente) {
		Intent intent = new Intent(activity, CantieriDettaglioModActivity.class);
		intent.putExtra(Anagrafica.ID_ANAGRAFICA, cliente);
		intent.putExtra(CantieriDettaglioModActivity.EXTRA_LIBERO, true);
		// richiesta 2: salvato, ClientiDettaglioActivity apre il cantiere nuovo
		((EConTabActivity) activity).apriFinestraInserimento(intent, 2, new Cantieri());
	}

	public static void apri(Activity activity, int idCantiere) {
		Intent intent = new Intent(activity, CantiereSplitActivity.class);
		intent.putExtra(Cantieri.ID_CANTIERE, idCantiere);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		activity.startActivity(intent);
	}

	/** Si puo' creare un cantiere per il cliente: funzionalita' CANTIERI.CREA e cliente attivo. */
	public static boolean puoCreare(Activity activity, int cliente) {
		if (!FunzionalitaApp.ha(activity, FunzionalitaApp.CANTIERI_CREA)) return false;
		DbInterno db = new DbInterno(activity);
		ContentValues w = new ContentValues();
		w.put(Anagrafica.ID_ANAGRAFICA, cliente);
		ContentValues v = db.getRecord(new Anagrafica(), w);
		db.close();
		Integer attivo = v != null ? v.getAsInteger(AbstractTable.ATTIVO) : null;
		return v != null && (attivo == null || attivo == 1);
	}

	/** Cantieri attivi del cliente, per nome. */
	public static List<ContentValues> attivi(DbInterno db, int cliente) {
		ArrayList<Object> r = db.eseguiSelect("SELECT * FROM " + Cantieri.NOME_TABELLA + " WHERE " + Cantieri.ID_ANAGRAFICA + " = " + cliente
				+ " AND coalesce(" + AbstractTable.ATTIVO + ", 1) = 1 ORDER BY " + Cantieri.NOME, null);
		List<ContentValues> l = new ArrayList<>();
		for (Object o : r) l.add((ContentValues) o);
		return l;
	}

	/**
	 * Il cantiere che sta a un indirizzo del cliente: quello nato dall'indirizzo (id_cliente_indirizzo), altrimenti uno
	 * con la stessa via, civico e citta' (cantieri creati a mano o prima degli indirizzi). Null se non c'e'.
	 */
	public static ContentValues aIndirizzo(List<ContentValues> cantieri, ContentValues ind) {
		Integer idInd = ind.getAsInteger(ClientiIndirizzi.ID);
		for (ContentValues c : cantieri) {
			Integer k = c.getAsInteger(Cantieri.ID_CLIENTE_INDIRIZZO);
			if (k != null && k.equals(idInd)) return c;
		}
		String via = chiave(testo(ind, ClientiIndirizzi.INDIRIZZO) + testo(ind, ClientiIndirizzi.CIVICO));
		String citta = chiave(testo(ind, ClientiIndirizzi.CITTA));
		if (via.isEmpty()) return null;
		for (ContentValues c : cantieri) {
			if (via.equals(chiave(testo(c, Cantieri.INDIRIZZO) + testo(c, Cantieri.CIVICO)))
					&& (citta.isEmpty() || chiave(testo(c, Cantieri.CITTA)).isEmpty() || citta.equals(chiave(testo(c, Cantieri.CITTA))))) {
				return c;
			}
		}
		return null;
	}

	/** Solo lettere e numeri, minuscole: "Via Roma, 12" = "via roma 12". */
	private static String chiave(String s) {
		return s.toLowerCase().replaceAll("[^\\p{L}\\p{N}]", "");
	}

	private static String testo(ContentValues v, String campo) {
		String s = v.getAsString(campo);
		return s == null ? "" : s.trim();
	}
}
