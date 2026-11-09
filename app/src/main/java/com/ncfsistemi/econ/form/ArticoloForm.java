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
 * Codice articolo del listino (CodiceArticoloModActivity): la chiave e' l'id (extra "ID", schema 40), il codice e' unico
 * per costruttore e non si cambia, la linea e' filtrata per costruttore. Extra nel nuovo: Listini.ID_COSTRUTTORE e/o Listini.ID_LINEA (proposti).
 * Costruttore facoltativo (STUDIO_IMPORT_APPEL.md §11.8): si cerca scrivendo tra quelli collegati alla ditta (schema 41,
 * piu' quelli dei suoi articoli), vuoto = Generico; senza linea va la Generica del costruttore.
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
				.ricerca(Listini.ID_COSTRUTTORE, contesto.getString(R.string.costruttore), new CostruttoriCollegati()).metaRiga()
				.scelta(Listini.ID_LINEA, contesto.getString(R.string.linea), new Linee()).collegataA(Listini.ID_COSTRUTTORE).metaRiga()
				// barcode: si scrive o si legge con la fotocamera (icona a destra del campo)
				.testo(Listini.BARCODE, "Barcode").max(50).icona(FaIcone.BARCODE);
		f.sezione(FaIcone.FATTURAZIONE, "Prezzi")
				.numero(Listini.PRZ_ULTIMO_ACQUISTO, contesto.getString(R.string.prezzo_acquisto)).max(8).decimali(2).metaRiga()
				.numero(Listini.PRZ_LISTINO, contesto.getString(R.string.prezzo_listino)).max(8).decimali(2).metaRiga()
				.numero(Listini.SCONTO, contesto.getString(R.string.sconto)).max(6).decimali(2).icona(FaIcone.PERCENTUALE).metaRiga();
		// foto: solo per gli articoli che ci sono sul server (id positivo), scaricata aprendo la scheda (FotoArticolo)
		int id = ospite != null ? ospite.getIntent().getIntExtra("ID", 0) : 0;
		if (id > 0) {
			f.sezione(FaIcone.FOTO, "Foto").personalizzata(contenitore -> disegnaFoto(contenitore, id));
		}
	}

	private void disegnaFoto(android.widget.LinearLayout contenitore, int id) {
		android.widget.TextView stato = new android.widget.TextView(contesto);
		stato.setText("Caricamento della foto…");
		stato.setTextColor(0xFF888888);
		stato.setPadding(0, 8, 0, 8);
		android.widget.ImageView immagine = new android.widget.ImageView(contesto);
		immagine.setAdjustViewBounds(true);
		immagine.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
		int lato = Math.round(260 * contesto.getResources().getDisplayMetrics().density);
		immagine.setMaxHeight(lato);
		immagine.setVisibility(android.view.View.GONE);
		contenitore.addView(stato);
		contenitore.addView(immagine, new android.widget.LinearLayout.LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
		com.ncfsistemi.econ.utils.FotoArticolo.carica(contesto, id, foto -> {
			if (foto == null) {
				stato.setText("Nessuna foto per questo articolo.");
				return;
			}
			stato.setVisibility(android.view.View.GONE);
			immagine.setImageBitmap(foto);
			immagine.setVisibility(android.view.View.VISIBLE);
		});
	}

	@Override
	public void dopoCaricamento(Form f, ContentValues record, boolean nuovo) {
		lettoreBarcode(f);
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
		if (fornitore == 0) fornitore = generico();
		if (fornitore != 0) f.imposta(Listini.ID_COSTRUTTORE, "" + fornitore);
		if (linea != 0) f.imposta(Listini.ID_LINEA, "" + linea);
	}

	@Override
	public void controlla(Form f, boolean nuovo) {
		if (!nuovo) return;
		// stesso codice dello stesso fornitore: c'e' gia' (di un altro fornitore va bene, L8)
		DbInterno db = new DbInterno(ospite);
		ContentValues where = new ContentValues();
		where.put(Listini.CODICE_ARTICOLO, f.valore(Listini.CODICE_ARTICOLO));
		String costruttore = f.valore(Listini.ID_COSTRUTTORE);
		where.put(Listini.ID_COSTRUTTORE, costruttore.isEmpty() ? "" + generico() : costruttore);
		ContentValues esiste = db.getRecord(new Listini(), where);
		db.close();
		if (esiste != null) f.errore(Listini.CODICE_ARTICOLO, contesto.getString(R.string.errore_codice_articolo_esistente));
	}

	@Override
	public void primaDiSalvare(ContentValues val, boolean nuovo) {
		// senza costruttore: Generico; senza linea: la Generica del costruttore se c'e'
		String costruttore = val.getAsString(Listini.ID_COSTRUTTORE);
		if (costruttore == null || costruttore.isEmpty() || "0".equals(costruttore)) {
			int g = generico();
			if (g != 0) val.put(Listini.ID_COSTRUTTORE, g);
			else val.remove(Listini.ID_COSTRUTTORE);
			costruttore = val.getAsString(Listini.ID_COSTRUTTORE);
		}
		if (val.getAsString(Listini.ID_LINEA) == null || val.getAsString(Listini.ID_LINEA).isEmpty()) {
			int generica = costruttore != null ? genericaDi(costruttore) : 0;
			if (generica != 0) val.put(Listini.ID_LINEA, "" + generica);
		}
		// senza linea: nel nuovo non si scrive, in modifica diventa 0 (come prima)
		if (val.getAsString(Listini.ID_LINEA) == null || val.getAsString(Listini.ID_LINEA).isEmpty()) {
			if (nuovo) val.remove(Listini.ID_LINEA);
			else val.put(Listini.ID_LINEA, 0);
		}
	}

	/** Icona a destra del campo Barcode: legge il codice con la fotocamera e lo scrive nel campo. */
	private void lettoreBarcode(Form f) {
		Campo c = f.campo(Listini.BARCODE);
		if (c == null || c.box == null || c.bloccato) return;
		c.box.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM);
		c.box.setEndIconDrawable(FaIcone.drawable(contesto, FaIcone.BARCODE, 22));
		c.box.setEndIconContentDescription("Leggi il barcode con la fotocamera");
		c.box.setEndIconOnClickListener(v -> com.ncfsistemi.econ.utils.LettoreBarcode.leggi(ospite,
				codice -> f.imposta(Listini.BARCODE, codice)));
	}

	/** Id del costruttore Generico del catalogo generale, 0 se non c'e'. */
	private int generico() {
		DbInterno db = new DbInterno(ospite);
		ContentValues r = db.getRecord("select " + Costruttori.ID_COSTRUTTORE + " from " + Costruttori.NOME_TABELLA + " where "
				+ Costruttori.SIGLA_METEL + "='" + Costruttori.SIGLA_GENERICO + "' and coalesce(" + Costruttori.ID_DITTA + ",0)=0 limit 1");
		db.close();
		return r != null && r.getAsInteger(Costruttori.ID_COSTRUTTORE) != null ? r.getAsInteger(Costruttori.ID_COSTRUTTORE) : 0;
	}

	/** Id della linea "Generica" del costruttore, 0 se non c'e'. */
	private int genericaDi(String costruttore) {
		DbInterno db = new DbInterno(ospite);
		ContentValues r = db.getRecord("select " + Linee.ID_LINEA + " from " + Linee.NOME_TABELLA + " where " + Linee.ID_COSTRUTTORE + "="
				+ Integer.parseInt(costruttore) + " and lower(" + Linee.NOME_LINEA + ")='" + Linee.NOME_GENERICA.toLowerCase() + "' limit 1");
		db.close();
		return r != null && r.getAsInteger(Linee.ID_LINEA) != null ? r.getAsInteger(Linee.ID_LINEA) : 0;
	}

	/** Costruttori da proporre: collegati alla ditta (schema 41) e quelli che hanno gia' articoli sul dispositivo. */
	static class CostruttoriCollegati extends Costruttori {
		@Override
		public String getSQLPerSpinner(ContentValues filtro) {
			return "select * from " + NOME_TABELLA + " where " + CONDIZIONE_COLLEGATI + " or " + ID_COSTRUTTORE + " in (select "
					+ Listini.ID_COSTRUTTORE + " from " + Listini.NOME_TABELLA + ") order by " + RAGIONE_SOCIALE;
		}
	}
}
