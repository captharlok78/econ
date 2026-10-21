package com.ncfsistemi.econ.form;

import android.content.ContentValues;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Costruttori;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Listini;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;

/**
 * Codice articolo del listino (CodiceArticoloModActivity): il codice e' la chiave (unico, non si cambia), la linea e'
 * filtrata per fornitore. Extra nel nuovo: Listini.ID_COSTRUTTORE e/o Listini.ID_LINEA (proposti).
 */
public class ArticoloForm extends DefinizioneForm {

	@Override
	public AbstractTable tabella() {
		return new Listini();
	}

	@Override
	public String titolo(boolean nuovo) {
		return nuovo ? "Nuovo articolo" : "Modifica articolo";
	}

	@Override
	public String sottotitolo(ContentValues record) {
		return record.getAsString(Listini.CODICE_ARTICOLO);
	}

	@Override
	public String moduloFunzionalita() {
		return FunzionalitaApp.LISTINI;
	}

	@Override
	public void sezioni(Form f) {
		f.sezione(FaIcone.MATERIALE, "Articolo")
				.testo(Listini.CODICE_ARTICOLO, contesto.getString(R.string.codice_articolo)).obbligatorio().max(50).fissoInModifica()
				.icona(FaIcone.CODICE)
				.testo(Listini.DESCRIZIONE, contesto.getString(R.string.descrizione)).max(255)
				.scelta(Listini.ID_COSTRUTTORE, contesto.getString(R.string.fornitore), new Costruttori()).obbligatorio().metaRiga()
				.scelta(Listini.ID_LINEA, contesto.getString(R.string.linea), new Linee()).collegataA(Listini.ID_COSTRUTTORE).metaRiga();
		f.sezione(FaIcone.FATTURAZIONE, "Prezzi")
				.numero(Listini.PRZ_ULTIMO_ACQUISTO, contesto.getString(R.string.prezzo_acquisto)).max(8).decimali(2).metaRiga()
				.numero(Listini.PRZ_LISTINO, contesto.getString(R.string.prezzo_listino)).max(8).decimali(2).metaRiga()
				.numero(Listini.SCONTO, contesto.getString(R.string.sconto)).max(6).decimali(2).icona(FaIcone.PERCENTUALE).metaRiga();
	}

	@Override
	public void dopoCaricamento(Form f, ContentValues record, boolean nuovo) {
		if (!nuovo) return;
		int fornitore = ospite.getIntent().getIntExtra(Listini.ID_COSTRUTTORE, 0);
		int linea = ospite.getIntent().getIntExtra(Listini.ID_LINEA, 0);
		if (linea != 0) {
			// il fornitore e' quello della linea
			DbInterno db = new DbInterno(ospite);
			ContentValues where = new ContentValues();
			where.put(Linee.ID_LINEA, linea);
			ContentValues valLinea = db.getRecord(new Linee(), where);
			db.close();
			if (valLinea != null) fornitore = valLinea.getAsInteger(Linee.ID_COSTRUTTORE);
		}
		if (fornitore != 0) f.imposta(Listini.ID_COSTRUTTORE, "" + fornitore);
		if (linea != 0) f.imposta(Listini.ID_LINEA, "" + linea);
	}

	@Override
	public void controlla(Form f, boolean nuovo) {
		if (!nuovo) return;
		DbInterno db = new DbInterno(ospite);
		ContentValues where = new ContentValues();
		where.put(Listini.CODICE_ARTICOLO, f.valore(Listini.CODICE_ARTICOLO));
		ContentValues esiste = db.getRecord(new Listini(), where);
		db.close();
		if (esiste != null) f.errore(Listini.CODICE_ARTICOLO, contesto.getString(R.string.errore_codice_articolo_esistente));
	}

	@Override
	public void primaDiSalvare(ContentValues val, boolean nuovo) {
		// senza linea: nel nuovo non si scrive, in modifica diventa 0 (come prima)
		if (val.getAsString(Listini.ID_LINEA).isEmpty()) {
			if (nuovo) val.remove(Listini.ID_LINEA);
			else val.put(Listini.ID_LINEA, 0);
		}
	}
}
