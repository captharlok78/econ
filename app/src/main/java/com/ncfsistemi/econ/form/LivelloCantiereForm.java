package com.ncfsistemi.econ.form;

import android.content.ContentValues;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Placche;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;

/**
 * Padre di unita', area e locale del cantiere (gestione elettrica, GESTIONE_ELETTRICA.md): nome, linea e placca
 * standard (la placca filtrata per linea), note. Nel nuovo il padre arriva negli extra e linea e placca si propongono
 * dal padre; cambiare la linea di un record esistente chiede conferma (i codici dei preventivi/ordini aperti cambiano).
 */
public abstract class LivelloCantiereForm extends DefinizioneForm {

	/** Colonne comuni alle tre tabelle (stessi nomi). */
	private static final String NOME = "nome", ID_LINEA = "id_linea", ID_PLACCA = "id_placca", NOTE = "note";

	/** Tabella e colonna (= extra) del padre: cantiere per l'unita', unita' per l'area, area per il locale. */
	protected abstract AbstractTable tabellaPadre();

	protected abstract String colonnaPadre();

	protected abstract String icona();

	/** Nome del livello con l'articolo: "unità", "area", "locale". */
	protected abstract String etichetta();

	protected abstract boolean femminile();

	@Override
	public String titolo(boolean nuovo) {
		String nome = etichetta();
		if (!nuovo) return "Modifica " + nome;
		return (femminile() ? "Nuova " : "Nuovo ") + nome;
	}

	@Override
	public String sottotitolo(ContentValues record) {
		return record.getAsString(NOME);
	}

	@Override
	public String moduloFunzionalita() {
		return FunzionalitaApp.ELETTRICO;
	}

	@Override
	public void sezioni(Form f) {
		String titolo = etichetta().substring(0, 1).toUpperCase() + etichetta().substring(1);
		f.sezione(icona(), titolo)
				.testo(NOME, titolo).max(50)
				.scelta(ID_LINEA, contesto.getString(R.string.linea_standard), new Linee()).metaRiga().nuovaRiga()
				.scelta(ID_PLACCA, contesto.getString(R.string.placca_standard), new Placche()).collegataA(ID_LINEA).metaRiga()
				.note(NOTE, "Note").max(255);
	}

	@Override
	public void dopoCaricamento(Form f, ContentValues record, boolean nuovo) {
		if (!nuovo) return;
		DbInterno db = new DbInterno(ospite);
		ContentValues where = new ContentValues();
		where.put(colonnaPadre(), ospite.getIntent().getIntExtra(colonnaPadre(), 0));
		ContentValues padre = db.getRecord(tabellaPadre(), where);
		db.close();
		if (padre != null) {
			f.imposta(ID_LINEA, padre.getAsString(ID_LINEA));
			f.imposta(ID_PLACCA, padre.getAsString(ID_PLACCA));
		}
	}

	@Override
	public String messaggioConferma(Form f, boolean nuovo) {
		if (nuovo) return null;
		String prima = ospite.getRecord().getAsString(ID_LINEA);
		String ora = f.valore(ID_LINEA);
		if (prima == null || prima.isEmpty()) prima = "0";
		if (ora.isEmpty()) ora = "0";
		return ora.equals(prima) ? null : contesto.getString(R.string.messaggio_variazione_linea);
	}

	@Override
	public void primaDiSalvare(ContentValues val, boolean nuovo) {
		if (nuovo) val.put(colonnaPadre(), ospite.getIntent().getIntExtra(colonnaPadre(), 0));
	}
}
