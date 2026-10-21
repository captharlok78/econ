package com.ncfsistemi.econ.form;

import android.content.ContentValues;

import com.ncfsistemi.econ.CantieriDettaglioModActivity;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Aree;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.ClientiIndirizzi;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Locali;
import com.ncfsistemi.econ.db.table.Placche;
import com.ncfsistemi.econ.db.table.Unita;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;

import java.util.ArrayList;

/**
 * Nuovo cantiere / modifica del cantiere (CantieriDettaglioModActivity) e linguetta "Dati cantiere" della scheda: cliente
 * (si cerca scrivendo, fisso in modifica), nome e indirizzo proposti dal cliente o dall'indirizzo da cui nasce, linea e
 * placca standard solo con la gestione elettrica (GESTIONE_ELETTRICA.md). Nel nuovo nascono anche unita', area e locale
 * principali (Cantieri.inserisciNuovoCantiere).
 */
public class CantiereForm extends DefinizioneForm {

	private int idIndirizzo = 0;
	/** Nome proposto in automatico: se l'utente non l'ha toccato, cambiando cliente si aggiorna. */
	private String nomeProposto = "";
	/** Cambio di linea in modifica: anche su unita', aree e locali (scelta prima di salvare, nell'activity). */
	private boolean cambiaLineaLocali = false;

	@Override
	public AbstractTable tabella() {
		return new Cantieri();
	}

	@Override
	public String titolo(boolean nuovo) {
		return nuovo ? "Nuovo cantiere" : "Modifica cantiere";
	}

	@Override
	public String sottotitolo(ContentValues record) {
		return record.getAsString(Cantieri.NOME);
	}

	@Override
	public String moduloFunzionalita() {
		return FunzionalitaApp.CANTIERI;
	}

	@Override
	public void sezioni(Form f) {
		f.sezione(FaIcone.CANTIERE, contesto.getString(R.string.dati_cantiere))
				.ricerca(Cantieri.ID_ANAGRAFICA, contesto.getString(R.string.cliente), new Anagrafica()).obbligatorio().fissoInModifica()
				.icona(FaIcone.CLIENTE)
				.avviso("Il cliente si sceglie quando si crea il cantiere, poi non si cambia: per un altro cliente si crea un nuovo cantiere.")
				.testo(Cantieri.NOME, "Nome cantiere").obbligatorio().max(100)
				.note(Cantieri.NOTE, "Note").max(255);
		f.sezione(FaIcone.INDIRIZZO, contesto.getString(R.string.indirizzo))
				.testo(Cantieri.INDIRIZZO, "Via / piazza").max(150).metaRiga(4)
				.testo(Cantieri.CIVICO, "Civico").max(15).maiuscolo().metaRiga()
				.intero(Cantieri.CAP, "CAP").max(10).metaRiga().nuovaRiga()
				.testo(Cantieri.CITTA, "Città").max(70).metaRiga(3)
				.testo(Cantieri.PROVINCIA, "Prov.").max(2).maiuscolo().metaRiga();
		// linea e placca standard: solo con la gestione elettrica; senza, i valori restano quelli che sono
		if (FunzionalitaApp.haModulo(contesto, FunzionalitaApp.ELETTRICO)) {
			f.sezione(FaIcone.IMPIANTO, "Gestione elettrica")
					.scelta(Cantieri.ID_LINEA, contesto.getString(R.string.linea_standard), new Linee()).metaRiga().nuovaRiga()
					.scelta(Cantieri.ID_PLACCA, contesto.getString(R.string.placca_standard), new Placche()).collegataA(Cantieri.ID_LINEA).metaRiga();
		}
	}

	@Override
	public void dopoCaricamento(Form f, ContentValues record, boolean nuovo) {
		if (!nuovo) return;
		int cliente = ospite.getIntent().getIntExtra(Anagrafica.ID_ANAGRAFICA, 0);
		if (cliente == 0) {
			try {
				cliente = Integer.parseInt(ospite.getIntent().getStringExtra(Anagrafica.ID_ANAGRAFICA));
			} catch (Exception e) {
				cliente = 0;
			}
		}
		DbInterno db = new DbInterno(ospite);
		// un solo cliente: e' quello
		if (cliente == 0 && db.eseguiCount(new Anagrafica(), new ContentValues()) == 1) {
			cliente = db.getRecord(new Anagrafica(), new ContentValues()).getAsInteger(Anagrafica.ID_ANAGRAFICA);
		}
		// cantiere da un indirizzo del cliente (GESTIONE_CLIENTI.md §6): resta collegato a quell'indirizzo
		idIndirizzo = ospite.getIntent().getIntExtra(CantieriDettaglioModActivity.EXTRA_INDIRIZZO, 0);
		if (idIndirizzo != 0) {
			ContentValues w = new ContentValues();
			w.put(ClientiIndirizzi.ID, idIndirizzo);
			ContentValues ind = db.getRecord(new ClientiIndirizzi(), w);
			if (ind != null) cliente = ind.getAsInteger(ClientiIndirizzi.ID_ANAGRAFICA);
		}
		db.close();
		if (cliente == 0) return;
		f.imposta(Cantieri.ID_ANAGRAFICA, String.valueOf(cliente));
		// cantiere "libero" dalla scheda cliente: solo il cliente, nome e indirizzo si scrivono a mano
		if (!ospite.getIntent().getBooleanExtra(CantieriDettaglioModActivity.EXTRA_LIBERO, false)) proponiDalCliente(f, cliente);
	}

	@Override
	public void alCambio(Form f, String colonna) {
		if (!Cantieri.ID_ANAGRAFICA.equals(colonna) || f.valore(colonna).isEmpty()) return;
		idIndirizzo = 0; // altro cliente: niente collegamento all'indirizzo di prima
		proponiDalCliente(f, Integer.parseInt(f.valore(colonna)));
	}

	/**
	 * Nome e indirizzo dal cliente: dall'indirizzo da cui nasce il cantiere oppure dalla sede principale; senza indirizzi
	 * (dati non ancora sincronizzati) quello scritto sul cliente. Il nome si propone solo se vuoto o ancora quello proposto.
	 */
	private void proponiDalCliente(Form f, int cliente) {
		DbInterno db = new DbInterno(ospite);
		ContentValues w = new ContentValues();
		w.put(Anagrafica.ID_ANAGRAFICA, cliente);
		ContentValues cli = db.getRecord(new Anagrafica(), w);
		ArrayList<Object> r = db.eseguiSelect("SELECT * FROM " + ClientiIndirizzi.NOME_TABELLA + " WHERE "
				+ (idIndirizzo != 0 ? "id = " + idIndirizzo : "id_anagrafica = " + cliente + " AND principale = 1") + " LIMIT 1", null);
		db.close();
		if (cli == null) return;
		String nome = f.valore(Cantieri.NOME);
		boolean proponiNome = nome.isEmpty() || nome.equals(nomeProposto);
		String rs = cli.getAsString(Anagrafica.RAGIONE_SOCIALE) == null ? "" : cli.getAsString(Anagrafica.RAGIONE_SOCIALE);
		String proposto = rs;
		if (!r.isEmpty()) {
			ContentValues i = (ContentValues) r.get(0);
			f.imposta(Cantieri.INDIRIZZO, i.getAsString(ClientiIndirizzi.INDIRIZZO));
			f.imposta(Cantieri.CIVICO, i.getAsString(ClientiIndirizzi.CIVICO));
			f.imposta(Cantieri.CAP, i.getAsString(ClientiIndirizzi.CAP));
			f.imposta(Cantieri.CITTA, i.getAsString(ClientiIndirizzi.CITTA));
			f.imposta(Cantieri.PROVINCIA, i.getAsString(ClientiIndirizzi.PROVINCIA));
			// da un indirizzo come Cantieri::daIndirizzo: descrizione dell'indirizzo, altrimenti "cliente — città"
			if (idIndirizzo != 0) {
				String descr = i.getAsString(ClientiIndirizzi.DESCRIZIONE), citta = i.getAsString(ClientiIndirizzi.CITTA);
				proposto = descr != null && !descr.isEmpty() ? descr : rs + (citta == null || citta.isEmpty() ? "" : " — " + citta);
			}
		} else {
			f.imposta(Cantieri.INDIRIZZO, cli.getAsString(Anagrafica.INDIRIZZO));
			f.imposta(Cantieri.CIVICO, "");
			f.imposta(Cantieri.CAP, cli.getAsString(Anagrafica.CAP));
			f.imposta(Cantieri.CITTA, cli.getAsString(Anagrafica.CITTA));
			f.imposta(Cantieri.PROVINCIA, cli.getAsString(Anagrafica.PROVINCIA));
		}
		if (proponiNome) {
			nomeProposto = proposto.length() > 100 ? proposto.substring(0, 100) : proposto;
			f.imposta(Cantieri.NOME, nomeProposto);
		}
	}

	/** La linea di un cantiere esistente e' stata cambiata. */
	public boolean lineaCambiata(Form f) {
		if (ospite.getRecord() == null || f.campo(Cantieri.ID_LINEA) == null) return false;
		return !zero(f.valore(Cantieri.ID_LINEA)).equals(zero(ospite.getRecord().getAsString(Cantieri.ID_LINEA)));
	}

	public void setCambiaLineaLocali(boolean si) {
		cambiaLineaLocali = si;
	}

	@Override
	public String messaggioConferma(Form f, boolean nuovo) {
		return !nuovo && lineaCambiata(f) ? contesto.getString(R.string.messaggio_variazione_linea) : null;
	}

	private static String zero(String s) {
		return s == null || s.isEmpty() ? "0" : s;
	}

	@Override
	public void primaDiSalvare(ContentValues val, boolean nuovo) {
		if (val.containsKey(Cantieri.ID_LINEA)) val.put(Cantieri.ID_LINEA, zero(val.getAsString(Cantieri.ID_LINEA)));
		if (val.containsKey(Cantieri.ID_PLACCA)) val.put(Cantieri.ID_PLACCA, zero(val.getAsString(Cantieri.ID_PLACCA)));
		if (nuovo) {
			val.put(Cantieri.ID_CLIENTE_INDIRIZZO, idIndirizzo);
			if (!val.containsKey(Cantieri.ID_LINEA)) val.put(Cantieri.ID_LINEA, 0);
			if (!val.containsKey(Cantieri.ID_PLACCA)) val.put(Cantieri.ID_PLACCA, 0);
		}
	}

	@Override
	public long inserisci(DbInterno db, AbstractTable tabella, ContentValues val) {
		return ((Cantieri) tabella).inserisciNuovoCantiere(val, db, ospite);
	}

	/** Linea cambiata e scelto "su tutti i locali": la nuova linea e placca anche su locali, aree e unita'. */
	@Override
	public void dopoSalvataggio(DbInterno db, ContentValues val, boolean nuovo) {
		if (nuovo || !cambiaLineaLocali) return;
		int cantiere = ospite.getIDModifica();
		String linea = val.getAsString(Cantieri.ID_LINEA), placca = val.getAsString(Cantieri.ID_PLACCA);
		Locali tabLoc = new Locali();
		for (Object o : tabLoc.getLocaliCantiere(db, cantiere)) {
			ContentValues upd = tabLoc.getValoriLogModifica(db);
			upd.put(Locali.ID_LINEA, linea);
			upd.put(Locali.ID_PLACCA, placca);
			ContentValues w = new ContentValues();
			w.put(Locali.ID_LOCALE, ((ContentValues) o).getAsInteger(Locali.ID_LOCALE));
			tabLoc.aggiornaRecord(db, upd, w);
		}
		Aree tabAree = new Aree();
		for (Object o : tabAree.getAreeCantiere(db, cantiere)) {
			ContentValues upd = tabAree.getValoriLogModifica(db);
			upd.put(Aree.ID_LINEA, linea);
			upd.put(Aree.ID_PLACCA, placca);
			ContentValues w = new ContentValues();
			w.put(Aree.ID_AREA, ((ContentValues) o).getAsInteger(Aree.ID_AREA));
			tabAree.aggiornaRecord(db, upd, w);
		}
		Unita tabUnita = new Unita();
		ContentValues wc = new ContentValues();
		wc.put(Unita.ID_CANTIERE, cantiere);
		for (Object o : db.eseguiSelect(tabUnita, wc, null)) {
			ContentValues upd = tabUnita.getValoriLogModifica(db);
			upd.put(Unita.ID_LINEA, linea);
			upd.put(Unita.ID_PLACCA, placca);
			ContentValues w = new ContentValues();
			w.put(Unita.ID_UNITA, ((ContentValues) o).getAsInteger(Unita.ID_UNITA));
			tabUnita.aggiornaRecord(db, upd, w);
		}
	}
}
