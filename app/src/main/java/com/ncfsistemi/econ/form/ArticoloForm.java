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
 * Schema 43 (STUDIO_CODICE_ARTICOLO_DITTA.md §9): nel nuovo si scrive solo il codice, il prefisso "<ditta>." e' fisso; vuoto =
 * automatico (lo da' il server). Schema 42 (STUDIO_BARCODE_ARTICOLI.md): tre barcode; di un articolo del listino si cambiano
 * solo i barcode (il server li mette nella personalizzazione della ditta).
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
				.testo(Listini.CODICE_ARTICOLO, contesto.getString(R.string.codice_articolo)).max(50).fissoInModifica()
				.icona(FaIcone.CODICE).aiuto("Vuoto = codice automatico")
				.testo(Listini.CODICE_DITTA, "Codice aziendale").soloLettura().soloModifica().icona(FaIcone.CODICE)
				.testo(Listini.DESCRIZIONE, contesto.getString(R.string.descrizione)).max(255)
				.ricerca(Listini.ID_COSTRUTTORE, contesto.getString(R.string.costruttore), new CostruttoriCollegati()).metaRiga()
				.scelta(Listini.ID_LINEA, contesto.getString(R.string.linea), new Linee()).collegataA(Listini.ID_COSTRUTTORE).metaRiga();
		// barcode: si scrivono o si leggono con la fotocamera (icona a destra del campo)
		f.sezione(FaIcone.BARCODE, "Barcode")
				.testo(Listini.BARCODE, "Barcode della tua azienda").max(50).icona(FaIcone.BARCODE)
				.testo(Listini.BARCODE_FORNITORE, "Barcode del fornitore").max(50).icona(FaIcone.BARCODE).metaRiga()
				.testo(Listini.BARCODE_COSTRUTTORE, "Barcode del costruttore (EAN)").max(50).icona(FaIcone.BARCODE).metaRiga();
		Sezione prezzi = f.sezione(FaIcone.FATTURAZIONE, "Prezzi");
		// netto e sconto solo a chi vede i costi (LISTINI.COSTI): agli altri il server li manda a zero
		if (FunzionalitaApp.ha(contesto, FunzionalitaApp.LISTINI_COSTI)) {
			prezzi.numero(Listini.PRZ_ULTIMO_ACQUISTO, contesto.getString(R.string.prezzo_acquisto)).max(8).decimali(2).metaRiga();
		}
		prezzi.numero(Listini.PRZ_LISTINO, contesto.getString(R.string.prezzo_listino)).max(8).decimali(2).metaRiga();
		if (FunzionalitaApp.ha(contesto, FunzionalitaApp.LISTINI_COSTI)) {
			prezzi.numero(Listini.SCONTO, contesto.getString(R.string.sconto)).max(6).decimali(2).icona(FaIcone.PERCENTUALE).metaRiga();
		}
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
		for (String colonna : new String[]{Listini.BARCODE, Listini.BARCODE_FORNITORE, Listini.BARCODE_COSTRUTTORE}) {
			lettoreBarcode(f, colonna);
		}
		if (!nuovo && Listini.isDelListino(record)) {
			// articolo del listino: si cambiano solo i barcode; quello del costruttore solo se il listino non ce l'ha
			for (String colonna : new String[]{Listini.DESCRIZIONE, Listini.ID_COSTRUTTORE, Listini.ID_LINEA,
					Listini.PRZ_ULTIMO_ACQUISTO, Listini.PRZ_LISTINO, Listini.SCONTO}) {
				f.blocca(colonna);
			}
			String ean = record.getAsString(Listini.BARCODE_COSTRUTTORE);
			if (ean != null && !ean.trim().isEmpty()) f.blocca(Listini.BARCODE_COSTRUTTORE);
		}
		if (!nuovo) return;
		// codice aziendale: il prefisso della ditta e' fisso, si scrive il resto
		Campo codice = f.campo(Listini.CODICE_ARTICOLO);
		if (codice != null && codice.box != null) codice.box.setPrefixText(Listini.prefissoDitta());
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
		String parte = parteCodice(f.valore(Listini.CODICE_ARTICOLO));
		if (parte.isEmpty()) return; // automatico
		// stesso codice dello stesso costruttore: c'e' gia' (di un altro costruttore va bene, L8); con o senza prefisso
		String costruttore = f.valore(Listini.ID_COSTRUTTORE);
		DbInterno db = new DbInterno(ospite);
		boolean esiste = !db.eseguiSelect("select " + Listini.ID + " from " + Listini.NOME_TABELLA + " where upper(" + Listini.CODICE_ARTICOLO
				+ ") in (?, ?) and " + Listini.ID_COSTRUTTORE + "=? limit 1",
				new String[]{parte, Listini.prefissoDitta() + parte, costruttore.isEmpty() ? "" + generico() : costruttore}).isEmpty();
		db.close();
		if (esiste) f.errore(Listini.CODICE_ARTICOLO, contesto.getString(R.string.errore_codice_articolo_esistente));
	}

	/** Il codice scritto senza il prefisso della ditta, in maiuscolo. */
	static String parteCodice(String codice) {
		String c = codice == null ? "" : codice.trim().toUpperCase(java.util.Locale.ITALY);
		String prefisso = Listini.prefissoDitta();
		return c.startsWith(prefisso) ? c.substring(prefisso.length()) : c;
	}

	@Override
	public void primaDiSalvare(ContentValues val, boolean nuovo) {
		if (nuovo) {
			// con il prefisso, come lo salva il server (vuoto: automatico, lo da' il server alla sincronizzazione)
			String parte = parteCodice(val.getAsString(Listini.CODICE_ARTICOLO));
			val.put(Listini.CODICE_ARTICOLO, parte.isEmpty() ? "" : Listini.prefissoDitta() + parte);
			val.put(Listini.ID_DITTA, com.ncfsistemi.econ.utils.Sessione.getDittaSelezionata());
		}
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

	/** Icona a destra del campo barcode: legge il codice con la fotocamera e lo scrive nel campo. */
	private void lettoreBarcode(Form f, String colonna) {
		Campo c = f.campo(colonna);
		if (c == null || c.box == null || c.bloccato) return;
		c.box.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM);
		c.box.setEndIconDrawable(FaIcone.drawable(contesto, FaIcone.BARCODE, 22));
		c.box.setEndIconContentDescription("Leggi il barcode con la fotocamera");
		c.box.setEndIconOnClickListener(v -> com.ncfsistemi.econ.utils.LettoreBarcode.leggi(ospite,
				codice -> f.imposta(colonna, codice)));
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
