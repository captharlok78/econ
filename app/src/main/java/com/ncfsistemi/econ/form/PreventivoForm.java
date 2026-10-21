package com.ncfsistemi.econ.form;

import android.content.ContentValues;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.Locali;
import com.ncfsistemi.econ.db.table.Preventivi;
import com.ncfsistemi.econ.db.table.PreventiviDettaglio;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;

import java.util.ArrayList;

/**
 * Testata di preventivo e ordine (PreventiviDettaglioModActivity): tipo (dall'extra, fisso in modifica), numero proposto
 * per anno e tipo, data, descrizione, stato (stati diversi per preventivo e ordine; un ordine chiuso non si riapre),
 * cliente (si cerca, serve solo a filtrare i cantieri) e cantiere. Riaprire un preventivo accettato/rifiutato riallinea
 * le placche dai locali; chiudere un ordine aperto sposta gli elementi nel cantiere (Preventivi.chiudiOrdine).
 */
public class PreventivoForm extends DefinizioneForm {

	/** Campo d'appoggio: il cliente filtra i cantieri, sul documento c'e' solo il cantiere. */
	private static final String CLIENTE = "cliente";

	@Override
	public AbstractTable tabella() {
		return new Preventivi();
	}

	/** Tipo del documento: dal record in modifica, dall'extra nel nuovo (preventivo se manca). */
	private String tipo() {
		if (ospite == null) return Preventivi.TIPO_PREVENTIVO;
		if (ospite.getRecord() != null) return ospite.getRecord().getAsString(Preventivi.TIPO);
		String t = ospite.getIntent().getStringExtra(Preventivi.TIPO);
		return t == null ? Preventivi.TIPO_PREVENTIVO : t;
	}

	private boolean ordine(Form f) {
		String t = f == null || f.valore(Preventivi.TIPO).isEmpty() ? tipo() : f.valore(Preventivi.TIPO);
		return Preventivi.TIPO_ORDINE.equals(t);
	}

	@Override
	public String titolo(boolean nuovo) {
		boolean ordine = Preventivi.TIPO_ORDINE.equals(tipo());
		return (nuovo ? "Nuovo " : "Modifica ") + (ordine ? "ordine" : "preventivo");
	}

	@Override
	public String sottotitolo(ContentValues record) {
		String titolo = record.getAsString(Preventivi.TITOLO);
		String numero = record.getAsString(Preventivi.NUMERO) + "/" + record.getAsString(Preventivi.ANNO);
		return titolo == null || titolo.isEmpty() ? "N. " + numero : "N. " + numero + " · " + titolo;
	}

	@Override
	public String moduloFunzionalita() {
		return FunzionalitaApp.moduloPreventivo(tipo());
	}

	@Override
	public void sezioni(Form f) {
		f.sezione(FaIcone.PREVENTIVO, "Documento")
				.opzioni(Preventivi.TIPO, contesto.getString(R.string.tipo), new String[][] {
						{ Preventivi.TIPO_PREVENTIVO, contesto.getString(R.string.preventivo) },
						{ Preventivi.TIPO_ORDINE, contesto.getString(R.string.ordine) } })
				.predefinito(tipo()).fissoInModifica()
				.intero(Preventivi.NUMERO, contesto.getString(R.string.numero)).obbligatorio().max(9).fissoInModifica().metaRiga()
				.aiuto("Proposto: il primo libero dell'anno")
				.data(Preventivi.DATA, contesto.getString(R.string.data)).obbligatorio().predefinito(Campo.OGGI).metaRiga()
				.testo(Preventivi.TITOLO, contesto.getString(R.string.descrizione)).max(255)
				.scelta(Preventivi.STATO, contesto.getString(R.string.stato), stati(tipo())).obbligatorio().predefinito(Preventivi.STATO_APERTO)
				.note(Preventivi.NOTE, "Note").max(255);
		f.sezione(FaIcone.CANTIERE, "Cliente e cantiere")
				.ricerca(CLIENTE, contesto.getString(R.string.cliente), new Anagrafica()).obbligatorio().appoggio().fissoInModifica()
				.icona(FaIcone.CLIENTE)
				.scelta(Preventivi.ID_CANTIERE, contesto.getString(R.string.cantiere), new Cantieri()).obbligatorio().collegataA(CLIENTE)
				.fissoInModifica();
	}

	/** Stati del preventivo o dell'ordine come {valore, descrizione}. */
	private String[][] stati(String tipo) {
		Preventivi p = new Preventivi();
		ArrayList<Object> elenco = Preventivi.TIPO_ORDINE.equals(tipo) ? p.getStatiOrdine(contesto) : p.getStatiPreventivo(contesto);
		String[][] voci = new String[elenco.size()][];
		for (int i = 0; i < elenco.size(); i++) {
			ContentValues v = (ContentValues) elenco.get(i);
			voci[i] = new String[] { v.getAsString("VAL"), v.getAsString("DESC") };
		}
		return voci;
	}

	@Override
	public void dopoCaricamento(Form f, ContentValues record, boolean nuovo) {
		DbInterno db = new DbInterno(ospite);
		int cantiere = nuovo ? ospite.getIntent().getIntExtra(Cantieri.ID_CANTIERE, 0) : record.getAsInteger(Preventivi.ID_CANTIERE);
		Integer cliente = null;
		if (cantiere != 0) {
			ContentValues w = new ContentValues();
			w.put(Cantieri.ID_CANTIERE, cantiere);
			ContentValues c = db.getRecord(new Cantieri(), w);
			if (c != null) cliente = c.getAsInteger(Cantieri.ID_ANAGRAFICA);
		} else if (db.eseguiCount(new Anagrafica(), new ContentValues()) == 1) {
			// un solo cliente: e' quello
			cliente = db.getRecord(new Anagrafica(), new ContentValues()).getAsInteger(Anagrafica.ID_ANAGRAFICA);
		}
		db.close();
		if (cliente != null) f.imposta(CLIENTE, String.valueOf(cliente));
		if (cantiere != 0) f.imposta(Preventivi.ID_CANTIERE, String.valueOf(cantiere));
		if (nuovo) {
			proponiNumero(f);
			return;
		}
		// le voci sono state disegnate prima di leggere il record: ora si sa se e' un ordine
		f.voci(Preventivi.STATO, stati(tipo()));
		f.imposta(Preventivi.STATO, record.getAsString(Preventivi.STATO));
		if (Preventivi.TIPO_ORDINE.equals(tipo()) && Preventivi.STATO_CHIUSO.equals(record.getAsString(Preventivi.STATO))) {
			// un ordine chiuso ha gia' spostato gli elementi nel cantiere: non si riapre
			f.blocca(Preventivi.STATO);
		}
	}

	@Override
	public void alCambio(Form f, String colonna) {
		if (!ospite.isNuovo()) return;
		if (Preventivi.TIPO.equals(colonna)) {
			f.voci(Preventivi.STATO, stati(f.valore(Preventivi.TIPO)));
			f.imposta(Preventivi.STATO, Preventivi.STATO_APERTO);
			proponiNumero(f);
		} else if (Preventivi.DATA.equals(colonna)) {
			proponiNumero(f);
		}
	}

	private static int anno(Form f) {
		String d = f.valore(Preventivi.DATA);
		return d.length() >= 4 ? Integer.parseInt(d.substring(0, 4)) : java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
	}

	/** Primo numero libero dell'anno per il tipo scelto. */
	private void proponiNumero(Form f) {
		DbInterno db = new DbInterno(ospite);
		Preventivi p = new Preventivi();
		int numero = ordine(f) ? p.getProssimoNumeroOrdine(db, anno(f)) : p.getProssimoNumeroPreventivo(db, anno(f));
		db.close();
		f.imposta(Preventivi.NUMERO, String.valueOf(numero));
	}

	private String statoPrima() {
		return ospite.getRecord() == null ? "" : ospite.getRecord().getAsString(Preventivi.STATO);
	}

	private boolean riapreAccettatoRifiutato(Form f) {
		String prima = statoPrima();
		return Preventivi.STATO_APERTO.equals(f.valore(Preventivi.STATO))
				&& (Preventivi.STATO_ACCETTATO.equals(prima) || Preventivi.STATO_RIFIUTATO.equals(prima));
	}

	private boolean chiudeOrdine(Form f) {
		return ordine(f) && Preventivi.STATO_CHIUSO.equals(f.valore(Preventivi.STATO)) && Preventivi.STATO_APERTO.equals(statoPrima());
	}

	@Override
	public String messaggioConferma(Form f, boolean nuovo) {
		if (nuovo) return null;
		if (riapreAccettatoRifiutato(f)) return contesto.getString(R.string.messaggio_variazione_placche);
		if (chiudeOrdine(f)) return contesto.getString(R.string.messaggio_chiusura_ordine);
		return null;
	}

	@Override
	public void primaDiSalvare(ContentValues val, boolean nuovo) {
		if (nuovo) val.put(Preventivi.ANNO, anno(ospite.getForm()));
	}

	@Override
	public void dopoSalvataggio(DbInterno db, ContentValues val, boolean nuovo) {
		if (nuovo) return;
		Form f = ospite.getForm();
		int id = ospite.getIDModifica();
		if (riapreAccettatoRifiutato(f)) {
			// placche del preventivo riallineate dai locali
			PreventiviDettaglio righe = new PreventiviDettaglio();
			for (Object o : new Locali().getLocaliCantiere(db, Integer.parseInt(f.valore(Preventivi.ID_CANTIERE)))) {
				righe.aggiornaPlaccheLocalePreventivo(db, id, ((ContentValues) o).getAsInteger(Locali.ID_LOCALE));
			}
		}
		if (chiudeOrdine(f)) new Preventivi().chiudiOrdine(db, id);
	}
}
