package pfa.app.econtab.export;

import android.content.ContentValues;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeMap;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Listini;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.PlaccheModuli;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.utils.Utility;

public class PreventivoXLS {

	private Context ctx = null;
	int rowCount = 0;
	private HSSFWorkbook wb = null;
	private Sheet sheet1 = null;
	private PreventiviDettaglio tabPrev = null;
	private HashMap<String, Double> mappaIva = null;

	public PreventivoXLS(Context ctx) {
		this.ctx = ctx;
		wb = new HSSFWorkbook();

		sheet1 = wb.createSheet("Order");
		tabPrev = new PreventiviDettaglio();
		/*
		 * try { File dirApp = ctx.getDir(Elementi.PATH_ICONE, Context.MODE_PRIVATE); File fileIco = new File(dirApp,
		 * "antenna.png");
		 * 
		 * InputStream inputStream = new FileInputStream(fileIco); // Get the contents of an InputStream as a byte[].
		 * byte[] bytes = IOUtils.toByteArray(inputStream); // Adds a picture to the workbook int pictureIdx =
		 * wb.addPicture(bytes, Workbook.PICTURE_TYPE_PNG); // close the input stream inputStream.close();
		 * 
		 * // Returns an object that handles instantiating concrete classes CreationHelper helper =
		 * wb.getCreationHelper();
		 * 
		 * // Creates the top-level drawing patriarch. Drawing drawing = sheet1.createDrawingPatriarch();
		 * 
		 * // Create an anchor that is attached to the worksheet ClientAnchor anchor = helper.createClientAnchor(); //
		 * set top-left corner for the image anchor.setCol1(1); anchor.setRow1(2);
		 * 
		 * // Creates a picture Picture pict = drawing.createPicture(anchor, pictureIdx); // Reset the image to the
		 * original size
		 * 
		 * } catch (Exception e) {
		 * 
		 * }
		 */

	}

	/**
	 * Crea il file xls del preventivo per la quotazione da parte del fornitore
	 * 
	 * @param idPreventivo
	 * @return
	 */
	public String generaReportMateriale(int idPreventivo) throws Exception {
		// TODO Auto-generated method stub

		_aggiungiRigaIntestazioneMateriale();

		DbInterno db = new DbInterno(ctx);

		PreventiviDettaglio tabPrev = new PreventiviDettaglio();
		ArrayList<Object> materialiLocali = tabPrev.getRighePreventivo(db, idPreventivo, PreventiviDettaglio.MATERIALE);
		ArrayList<Object> placcheLocali = tabPrev.getRighePreventivo(db, idPreventivo, PreventiviDettaglio.PLACCHE);

		ArrayList<Object> materialiPreventivo = tabPrev.getRighePreventivo(db, idPreventivo, PreventiviDettaglio.MATERIALE_PREVENTIVO);
		ArrayList<Object> placchePreventivo = tabPrev.getRighePreventivo(db, idPreventivo, PreventiviDettaglio.PLACCHE_PREVENTIVO);
		ArrayList<Object> collegamenti = tabPrev.getRighePreventivo(db, idPreventivo, PreventiviDettaglio.COLLEGAMENTI);

		ArrayList<Object> materialiRaggruppatiPerLinea = _raggruppaPerLinea(materialiLocali, materialiPreventivo, placcheLocali,
				placchePreventivo, collegamenti, db);

        Preventivi tabPreventivi = new Preventivi();
        ContentValues valClie =  tabPreventivi.getClienteCantierePreventivo(db,idPreventivo);

		ContentValues valPrev = tabPreventivi.getRecordPerChiave(db,idPreventivo);

		db.close();

		String numeroPrev = "";
		String annoPrev = "";

		for (int i = 0; i < materialiRaggruppatiPerLinea.size(); i++) {
			_aggiungiRigaMateriale((ContentValues) materialiRaggruppatiPerLinea.get(i));

			if (numeroPrev.equals("")) {
				numeroPrev = ((ContentValues) materialiRaggruppatiPerLinea.get(i)).getAsString(Preventivi.NUMERO);
				annoPrev = ((ContentValues) materialiRaggruppatiPerLinea.get(i)).getAsString(Preventivi.ANNO);
			}
		}

		// Create a path where we will place our List of objects on external storage

        String ragioneSociale = "";
        if (valClie!=null){
            ragioneSociale = valClie.getAsString(Anagrafica.RAGIONE_SOCIALE);

        }

        ragioneSociale = Utility.formattaStringaPerNomeFile(ragioneSociale);
		File dirExport = new File(Environment.getExternalStorageDirectory() + File.separator + "ECONTAB" + File.separator+ragioneSociale+File.separator
				+ PreventiviDettaglio.PATH_EXPORT_PREVENTIVI);

		if (!dirExport.exists()) {
			dirExport.mkdirs();
		}


		String nomePrev = "mat_prev_" + numeroPrev + "_" + annoPrev;
		if (valPrev.getAsString(Preventivi.TIPO).equals(Preventivi.TIPO_ORDINE)){
			nomePrev = "mat_ord_" + numeroPrev + "_" + annoPrev;
		}
		File fileExport = new File(dirExport, nomePrev + ".xls");
		if (fileExport.exists()) {
			fileExport.delete();
		}
		FileOutputStream os = null;

		try {
			os = new FileOutputStream(fileExport);
			wb.write(os);
			return fileExport.getPath();

		} catch (Exception e) {
			Log.w("FileUtils", "Failed to save file", e);
			throw e;
		} finally {
			try {
				if (os != null)
					os.close();
			} catch (Exception ex) {
			}
		}

	}

	/**
	 * Genera il preventivo in formato XLS
	 * 
	 * @param idPreventivo
	 * @return
	 * @throws Exception
	 */
	public String generaReportPreventivo(int idPreventivo) throws Exception {
		DbInterno db = new DbInterno(ctx);
        Preventivi tabPreventivi = new Preventivi();


        ContentValues valTestata = _aggiungiTitolo(db, idPreventivo);
		String numeroPrev = valTestata.getAsString(Preventivi.NUMERO);
		String annoPrev = valTestata.getAsString(Preventivi.ANNO);
        String tipo = valTestata.getAsString(Preventivi.TIPO);

		mappaIva = Utility.getMappaCodiciIva(db);

		rowCount = 5;

		_aggiungiRigaIntestazione();

		PreventiviDettaglio tabPrev = new PreventiviDettaglio();
		ArrayList<Object> materialiLocali = tabPrev.getRighePreventivo(db, idPreventivo, PreventiviDettaglio.MATERIALE,true);
		ArrayList<Object> placcheLocali = tabPrev.getRighePreventivo(db, idPreventivo, PreventiviDettaglio.PLACCHE);
		ArrayList<Object> materialiPreventivo = tabPrev.getRighePreventivo(db, idPreventivo, PreventiviDettaglio.MATERIALE_PREVENTIVO);
		ArrayList<Object> placchePreventivo = tabPrev.getRighePreventivo(db, idPreventivo, PreventiviDettaglio.PLACCHE_PREVENTIVO);
		ArrayList<Object> manodopera = tabPrev.getRighePreventivo(db, idPreventivo, PreventiviDettaglio.MANOPERA);
		ArrayList<Object> altro = tabPrev.getRighePreventivo(db, idPreventivo, PreventiviDettaglio.ALTRO);
		ArrayList<Object> collegamenti = tabPrev.getRighePreventivo(db, idPreventivo, PreventiviDettaglio.COLLEGAMENTI);

		int localePrecedente = 0;
		for (int i = 0; i < materialiLocali.size(); i++) {
			int localeCorrente = ((ContentValues) materialiLocali.get(i)).getAsInteger(PreventiviDettaglio.ID_LOCALE);
			if (localePrecedente != localeCorrente) {
				localePrecedente = localeCorrente;
				_aggiungiRigaTitoloLocale((ContentValues) materialiLocali.get(i));
				// aggiungo le placche
				for (int j = 0; j < placcheLocali.size(); j++) {
					ContentValues valPlacche = (ContentValues) placcheLocali.get(j);
					if (localeCorrente == valPlacche.getAsInteger(PreventiviDettaglio.ID_LOCALE)) {
						int idPlaccaModuli = valPlacche.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI);
						PlaccheModuli tabPlacche = new PlaccheModuli();
						ContentValues valPlaccaLinea = tabPlacche.getRecordPlaccaConLineaFornitore(db, idPlaccaModuli);
						if (valPlaccaLinea != null) {
							valPlacche.put(
									PreventiviDettaglio.DESCRIZIONE,
									ctx.getString(R.string.placca) + " " + valPlacche.getAsString(PreventiviDettaglio.DESCRIZIONE) + " "
											+ valPlaccaLinea.getAsInteger(PlaccheModuli.NUMERO_MODULI) + " "
											+ ctx.getString(R.string.moduli));
						}
						_aggiungiInformazioniFornitoreLinea(valPlacche, db);
						_aggiungiRigaPreventivo(valPlacche);

					}
				}
			}
			ContentValues valCurr = (ContentValues) materialiLocali.get(i);
			_aggiungiInformazioniFornitoreLinea(valCurr, db);
			_aggiungiRigaPreventivo(valCurr);

		}

		if (materialiPreventivo.size() > 0 || placchePreventivo.size() > 0) {
			ContentValues valAltroMateriale = new ContentValues();
			valAltroMateriale.put(Locali.NOME, ctx.getString(R.string.altro_materiale));
			_aggiungiRigaTitoloLocale(valAltroMateriale);
		}
		for (int i = 0; i < materialiPreventivo.size(); i++) {
			ContentValues valCurr = (ContentValues) materialiPreventivo.get(i);
			_aggiungiInformazioniFornitoreLinea(valCurr, db);
			_aggiungiRigaPreventivo(valCurr);
		}

		for (int i = 0; i < placchePreventivo.size(); i++) {
			ContentValues valCurr = (ContentValues) placchePreventivo.get(i);
			int idPlaccaModuli = valCurr.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI);
			PlaccheModuli tabPlacche = new PlaccheModuli();
			ContentValues valPlaccaLinea = tabPlacche.getRecordPlaccaConLineaFornitore(db, idPlaccaModuli);
			if (valPlaccaLinea != null) {
				valCurr.put(
						PreventiviDettaglio.DESCRIZIONE,
						ctx.getString(R.string.placca) + " " + valCurr.getAsString(PreventiviDettaglio.DESCRIZIONE) + " "
								+ valPlaccaLinea.getAsInteger(PlaccheModuli.NUMERO_MODULI) + " " + ctx.getString(R.string.moduli));
			}
			_aggiungiInformazioniFornitoreLinea(valCurr, db);
			_aggiungiRigaPreventivo(valCurr);
		}

		if (collegamenti.size() > 0) {
			ContentValues valAltroMateriale = new ContentValues();
			valAltroMateriale.put(Locali.NOME, ctx.getString(R.string.collegamenti));
			_aggiungiRigaTitoloLocale(valAltroMateriale);
		}
		for (int i = 0; i < collegamenti.size(); i++) {
			ContentValues valCurr = (ContentValues) collegamenti.get(i);
			_aggiungiInformazioniFornitoreLinea(valCurr, db);
			_aggiungiRigaPreventivo((ContentValues) collegamenti.get(i));
		}

		if (manodopera.size() > 0) {
			ContentValues valAltroMateriale = new ContentValues();
			valAltroMateriale.put(Locali.NOME, ctx.getString(R.string.manodopera));
			_aggiungiRigaTitoloLocale(valAltroMateriale);
		}
		for (int i = 0; i < manodopera.size(); i++) {
			_aggiungiRigaPreventivo((ContentValues) manodopera.get(i));
		}

		if (altro.size() > 0) {
			ContentValues valAltroMateriale = new ContentValues();
			valAltroMateriale.put(Locali.NOME, ctx.getString(R.string.note));
			_aggiungiRigaTitoloLocale(valAltroMateriale);
		}
		for (int i = 0; i < altro.size(); i++) {
			_aggiungiRigaPreventivo((ContentValues) altro.get(i));
		}

		ArrayList<Object> tutteLeRighe = new ArrayList<Object>();
		tutteLeRighe.addAll(materialiLocali);
		tutteLeRighe.addAll(materialiPreventivo);
        tutteLeRighe.addAll(placcheLocali);
        tutteLeRighe.addAll(placchePreventivo);
		tutteLeRighe.addAll(collegamenti);
		tutteLeRighe.addAll(manodopera);
		tutteLeRighe.addAll(altro);

		_aggiungiRigaTotali(tutteLeRighe, mappaIva);

		sheet1.setColumnHidden(9, true);



		db.close();


        String ragioneSociale = "";
        if (valTestata!=null){
            ragioneSociale = valTestata.getAsString(Anagrafica.RAGIONE_SOCIALE);
        }

        ragioneSociale = Utility.formattaStringaPerNomeFile(ragioneSociale);
        String cartellaPrev = PreventiviDettaglio.PATH_EXPORT_PREVENTIVI;
        if (tipo.equals(Preventivi.TIPO_ORDINE)){
            cartellaPrev = PreventiviDettaglio.PATH_EXPORT_ORDINI;
        }


		File dirExport = new File(Environment.getExternalStorageDirectory() + File.separator + "ECONTAB" + File.separator+ragioneSociale+File.separator
				+ cartellaPrev);

		if (!dirExport.exists()) {
			dirExport.mkdirs();
		}

		String nomePrev = "prev_" + numeroPrev + "_" + annoPrev;
        if (tipo.equals(Preventivi.TIPO_ORDINE)){
             nomePrev = "ord_" + numeroPrev + "_" + annoPrev;
        }

		File fileExport = new File(dirExport, nomePrev + ".xls");
		if (fileExport.exists()) {
			fileExport.delete();
		}
		FileOutputStream os = null;

		try {
			os = new FileOutputStream(fileExport);
			wb.write(os);
			return fileExport.getPath();

		} catch (Exception e) {
			Log.w("FileUtils", "Failed to save file", e);
			throw e;
		} finally {
			try {
				if (os != null)
					os.close();
			} catch (Exception ex) {
			}
		}
	}

	private void _aggiungiInformazioniFornitoreLinea(ContentValues valCurr, DbInterno db) {
		// TODO Auto-generated method stub
		String codiceArticolo = valCurr.getAsString(PreventiviDettaglio.CODICE_ARTICOLO);
		if (codiceArticolo.trim().length() > 0) {
			// int idElemento = valCurr.getAsInteger(PreventiviDettaglio.ID_ELEMENTO);
			// int idComponente = valCurr.getAsInteger(PreventiviDettaglio.ID_COMPONENTE);
			ContentValues whereLis = new ContentValues();
			whereLis.put(Listini.CODICE_ARTICOLO, codiceArticolo);

			Listini tabListino = new Listini();
			ContentValues valListino = tabListino.getRecordConFornitoreELinea(whereLis, db);
			if (valListino != null && valListino.getAsString(Costruttori.SIGLA_METEL) != null) {

				valCurr.put(Costruttori.SIGLA_METEL, valListino.getAsString(Costruttori.SIGLA_METEL));
				valCurr.put(Linee.NOME_LINEA, valListino.getAsString(Linee.NOME_LINEA));
			}
		}
	}

	private void _aggiungiRigaTotali(ArrayList<Object> tutteLeRighe, HashMap<String, Double> mappaCodiciIva) {
		// TODO Auto-generated method stub

		int primaRigaDati = 8;
		int ultimaRigaDati = rowCount;

		CellStyle cs = wb.createCellStyle();
		cs.setWrapText(true);

		CellStyle csBold = wb.createCellStyle();
		csBold.setWrapText(true);
		Font f = wb.createFont();
		f.setBoldweight(Font.BOLDWEIGHT_BOLD);
		csBold.setFont(f);

		CellStyle currencyCellStyle = wb.createCellStyle();
		currencyCellStyle.setWrapText(true);
		currencyCellStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

		CellStyle currencyCellStyleBold = wb.createCellStyle();
		currencyCellStyleBold.setWrapText(true);
		currencyCellStyleBold.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
		currencyCellStyleBold.setFont(f);

		Row riga = sheet1.createRow(rowCount + 1);
		Preventivi tabPreventivi = new Preventivi();
		HashMap<String, Double> mappaTotali = tabPreventivi.calcolaTotaliPreventivo(tutteLeRighe, mappaCodiciIva, ctx);

		Cell c = riga.createCell(6);
		c.setCellValue(ctx.getString(R.string.tot_imponibile));//
		c.setCellStyle(cs);

		c = riga.createCell(7);
		String primaCellaSomma = "H" + primaRigaDati;
		String ultimaCellaSomma = "H" + ultimaRigaDati;
		c.setCellFormula("SUM(" + primaCellaSomma + ":" + ultimaCellaSomma + ")");// formula imponibile
		c.setCellValue(mappaTotali.get("IMPONIBILE"));// imponibile
		c.setCellType(Cell.CELL_TYPE_FORMULA);

		c.setCellStyle(currencyCellStyle);

		riga = sheet1.createRow(rowCount + 2);
		c = riga.createCell(6);
		c.setCellValue(ctx.getString(R.string.tot_iva));//
		c.setCellStyle(cs);

		c = riga.createCell(7);
		String primaCellaSommaIva = "J" + primaRigaDati;
		String ultimaCellaSommaIva = "J" + ultimaRigaDati;

		c.setCellFormula("SUM(" + primaCellaSommaIva + ":" + ultimaCellaSommaIva + ")");// formula iva totale
		c.setCellValue(mappaTotali.get("IVA"));// iva
		c.setCellType(Cell.CELL_TYPE_FORMULA);
		c.setCellStyle(currencyCellStyle);

		riga = sheet1.createRow(rowCount + 3);
		c = riga.createCell(6);
		c.setCellValue(ctx.getString(R.string.tot_importo));//
		c.setCellStyle(csBold);

		c = riga.createCell(7);

		String cellaImponibile = "H" + (rowCount + 2); // il foglio excel numera le righe a partire da 1 quindi devo //
														// sommare 1 all'indice della riga
		String cellaIva = "H" + (rowCount + 3); // il foglio excel numera le righe a partire da 1 quindi devo
		// sommare 1 all'indice della riga
		c.setCellFormula("SUM(" + cellaImponibile + ":" + cellaIva + ")");// formula importo totale
		c.setCellValue(mappaTotali.get("IMPORTO"));// importo totale

		c.setCellType(Cell.CELL_TYPE_FORMULA);
		c.setCellStyle(currencyCellStyleBold);

	}

	private void _aggiungiRigaTitoloLocale(ContentValues val) {
		// TODO Auto-generated method stub

		Row riga = sheet1.createRow(rowCount);

		CellStyle cs = wb.createCellStyle();
		cs.setWrapText(true);
		Font f = wb.createFont();
		f.setBoldweight(Font.BOLDWEIGHT_BOLD);
		cs.setFont(f);

		sheet1.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 0, 1));

		Cell c = riga.createCell(0);
		c.setCellValue(val.getAsString(Locali.NOME));// LOCALE
		c.setCellStyle(cs);

		rowCount++;
	}

	private ContentValues _aggiungiTitolo(DbInterno db, int idPreventivo) {
		// TODO Auto-generated method stub

		Preventivi tabPrev = new Preventivi();
		ContentValues val = tabPrev.getClienteCantierePreventivo(db, idPreventivo);
String tipo = val.getAsString(Preventivi.TIPO);
		Font fTitolo = wb.createFont();
		fTitolo.setFontHeightInPoints((short) 14);
		fTitolo.setItalic(true);

		Font fBluBold = wb.createFont();
		fBluBold.setColor(HSSFColor.DARK_BLUE.index);
		fBluBold.setBoldweight(Font.BOLDWEIGHT_BOLD);

		Font fBlu = wb.createFont();
		fBlu.setColor(HSSFColor.DARK_BLUE.index);

		CellStyle csTitolo = wb.createCellStyle();
		csTitolo.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
		csTitolo.setFillPattern(CellStyle.SOLID_FOREGROUND);
		csTitolo.setFont(fTitolo);

		CellStyle csBlu = wb.createCellStyle();
		csBlu.setFont(fBlu);
		csBlu.setVerticalAlignment(CellStyle.VERTICAL_TOP);
		csBlu.setWrapText(true);
		csBlu.setFillForegroundColor(HSSFColor.WHITE.index);
		csBlu.setFillPattern(CellStyle.SOLID_FOREGROUND);

		CellStyle csBluBold = wb.createCellStyle();
		csBluBold.setFont(fBluBold);
		csBluBold.setVerticalAlignment(CellStyle.VERTICAL_TOP);
		csBluBold.setWrapText(true);
		csBluBold.setFillForegroundColor(HSSFColor.WHITE.index);
		csBluBold.setFillPattern(CellStyle.SOLID_FOREGROUND);

		Row row = sheet1.createRow(0);
		sheet1.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
		Cell c = row.createCell(0);
        if (tipo.equals(Preventivi.TIPO_ORDINE)){
            c.setCellValue(ctx.getResources().getString(R.string.ordine_num_del, val.getAsString(Preventivi.NUMERO),
                    Utility.numberToData(val.getAsLong(Preventivi.DATA))));
        }
        else{
            c.setCellValue(ctx.getResources().getString(R.string.preventivo_num_del, val.getAsString(Preventivi.NUMERO),
                    Utility.numberToData(val.getAsLong(Preventivi.DATA))));
        }

		c.setCellStyle(csTitolo);

		row = sheet1.createRow(1);
		sheet1.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));

		String testoClienteCantiere = ctx.getResources().getString(R.string.cliente).toUpperCase(Locale.getDefault());

		testoClienteCantiere = testoClienteCantiere + ": " + val.getAsString(Anagrafica.RAGIONE_SOCIALE);
		c = row.createCell(0);
		c.setCellValue(testoClienteCantiere);
		c.setCellStyle(csBluBold);

		row = sheet1.createRow(2);
		sheet1.addMergedRegion(new CellRangeAddress(2, 2, 0, 8));
		testoClienteCantiere = val.getAsString(Anagrafica.INDIRIZZO);
		testoClienteCantiere = testoClienteCantiere + ", " + val.getAsString(Anagrafica.CITTA);
		testoClienteCantiere = testoClienteCantiere + " (" + val.getAsString(Anagrafica.PROVINCIA) + ")";
		if (testoClienteCantiere.equals(",  ()")) {
			testoClienteCantiere = "";
		}
		c = row.createCell(0);
		c.setCellValue(testoClienteCantiere);
		c.setCellStyle(csBlu);

		row = sheet1.createRow(3);
		sheet1.addMergedRegion(new CellRangeAddress(3, 3, 0, 8));
		testoClienteCantiere = ctx.getResources().getString(R.string.cantiere).toUpperCase(Locale.getDefault());
		testoClienteCantiere = testoClienteCantiere + ": " + val.getAsString(Cantieri.NOME);
		c = row.createCell(0);
		c.setCellValue(testoClienteCantiere);
		c.setCellStyle(csBluBold);

		row = sheet1.createRow(4);
		sheet1.addMergedRegion(new CellRangeAddress(4, 4, 0, 8));

		testoClienteCantiere = val.getAsString(Cantieri.INDIRIZZO);
		testoClienteCantiere = testoClienteCantiere + ", " + val.getAsString(Cantieri.CITTA);
		testoClienteCantiere = testoClienteCantiere + " (" + val.getAsString(Cantieri.PROVINCIA) + ")";
		if (testoClienteCantiere.equals(",  ()")) {
			testoClienteCantiere = "";
		}

		c = row.createCell(0);
		c.setCellValue(testoClienteCantiere);
		c.setCellStyle(csBlu);

		return val;

	}

	/**
	 * Raggruppo assieme gli elementi per linea per evitare doppioni nel xls Per gli elementi aggiunti sui locali guardo
	 * la linea del locale (poi area,poi unit� e infine cantiere) Per gli elementi aggiunti sul cantiere guardo la linea
	 * sul cantiere
	 * 
	 * @param materialiLocali
	 * @param materialiPreventivo
	 * @param db
	 * @return
	 */
	private ArrayList<Object> _raggruppaPerLinea(ArrayList<Object> materialiLocali, ArrayList<Object> materialiPreventivo,
			ArrayList<Object> placcheLocali, ArrayList<Object> placchePreventivo, ArrayList<Object> collegamenti, DbInterno db) {
		// TODO Auto-generated method stub
		ArrayList<Object> listaRaggruppata = new ArrayList<Object>();

		TreeMap<String, ContentValues> mappaLinee = new TreeMap<String, ContentValues>();

		Locali tabLoc = new Locali();
		Cantieri tabCant = new Cantieri();

		boolean gestitoALinea = false;

		for (int i = 0; i < materialiLocali.size(); i++) {
			ContentValues curr = (ContentValues) materialiLocali.get(i);

			gestitoALinea = false;
			int idComponente = curr.getAsInteger(PreventiviDettaglio.ID_COMPONENTE);
			if (idComponente != 0) {
				ContentValues whereComp = new ContentValues();
				whereComp.put(Componenti.ID_COMPONENTE, idComponente);
				ContentValues valComp = db.getRecord(new Componenti(), whereComp);
				if (valComp != null) {
					if (valComp.getAsInteger(Componenti.LINEA_SN) == 1) {
						gestitoALinea = true;
					}

				}
			}

			String elemento = curr.getAsString(PreventiviDettaglio.DESCRIZIONE).trim();

			int idlinea = 0;
			String nomeLInea = "";
			String siglaMetel = "";
			if (gestitoALinea == true) {

				ContentValues lineaCv = tabLoc.getLineaLocale(db, curr.getAsInteger(PreventiviDettaglio.ID_LOCALE));
				idlinea = lineaCv.getAsInteger(Linee.ID_LINEA);

				nomeLInea = lineaCv.getAsString(Linee.NOME_LINEA);
				siglaMetel = lineaCv.getAsString(Costruttori.SIGLA_METEL);

				//controllo se c'è la linea sulla riga dell'ordine ed è diversa da quella del locale
				if (curr.containsKey(PreventiviDettaglio.ID_LINEA) && curr.get(PreventiviDettaglio.ID_LINEA)!=null && !curr.getAsString(PreventiviDettaglio.ID_LINEA).equals("null")){
					int idLineaRiga = curr.getAsInteger(PreventiviDettaglio.ID_LINEA);
					if (idLineaRiga!=0 && idLineaRiga!=idlinea){
						idlinea = idLineaRiga;
						nomeLInea = curr.getAsString(Linee.NOME_LINEA);
						siglaMetel = curr.getAsString(Costruttori.SIGLA_METEL);
					}
				}
			}

			String chiaveElemento = elemento + "|" + idlinea;

			if (mappaLinee.containsKey(chiaveElemento)) {
				ContentValues val = mappaLinee.get(chiaveElemento);
				double qtaOld = val.getAsDouble(PreventiviDettaglio.QUANTITA);
				double qtaCurr = curr.getAsDouble(PreventiviDettaglio.QUANTITA);
				val.put(PreventiviDettaglio.QUANTITA, qtaOld + qtaCurr);

			} else {
				curr.put(Linee.NOME_LINEA, nomeLInea);
				curr.put(Costruttori.SIGLA_METEL, siglaMetel);
				mappaLinee.put(chiaveElemento, curr);
			}
		}

		for (int i = 0; i < materialiPreventivo.size(); i++) {
			ContentValues curr = (ContentValues) materialiPreventivo.get(i);
			gestitoALinea = false;

			String elemento = curr.getAsString(PreventiviDettaglio.DESCRIZIONE).trim();
			int idComponente = curr.getAsInteger(PreventiviDettaglio.ID_COMPONENTE);
			if (idComponente != 0) {
				ContentValues whereComp = new ContentValues();
				whereComp.put(Componenti.ID_COMPONENTE, idComponente);
				ContentValues valComp = db.getRecord(new Componenti(), whereComp);
				if (valComp != null) {
					if (valComp.getAsInteger(Componenti.LINEA_SN) == 1) {
						gestitoALinea = true;
					}
				}
			}
			int idlinea = 0;
			String nomeLInea = "";
			String siglaMetel = "";
			if (gestitoALinea == true) {
				ContentValues lineaCv = tabCant.getLineaCantiere(db, curr.getAsInteger(Preventivi.ID_CANTIERE));
				idlinea = lineaCv.getAsInteger(Linee.ID_LINEA);
				nomeLInea = lineaCv.getAsString(Linee.NOME_LINEA);
				siglaMetel = lineaCv.getAsString(Costruttori.SIGLA_METEL);
			}

			String chiaveElemento = elemento + "|" + idlinea;

			if (mappaLinee.containsKey(chiaveElemento)) {
				ContentValues val = mappaLinee.get(chiaveElemento);
				double qtaOld = val.getAsDouble(PreventiviDettaglio.QUANTITA);
				double qtaCurr = curr.getAsDouble(PreventiviDettaglio.QUANTITA);
				val.put(PreventiviDettaglio.QUANTITA, qtaOld + qtaCurr);

			} else {
				curr.put(Linee.NOME_LINEA, nomeLInea);
				curr.put(Costruttori.SIGLA_METEL, siglaMetel);
				mappaLinee.put(chiaveElemento, curr);
			}
		}

		ArrayList<Object> placche = new ArrayList<Object>();
		placche.addAll(placcheLocali);
		placche.addAll(placchePreventivo);

		// placche
		for (int i = 0; i < placche.size(); i++) {
			ContentValues curr = (ContentValues) placche.get(i);
			int idPlaccaModuli = curr.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI);
			PlaccheModuli tabPlacche = new PlaccheModuli();
			ContentValues valPlaccaLinea = tabPlacche.getRecordPlaccaConLineaFornitore(db, idPlaccaModuli);
			if (valPlaccaLinea != null) {
				curr.put(
						PreventiviDettaglio.DESCRIZIONE,
						ctx.getString(R.string.placca) + " " + curr.getAsString(PreventiviDettaglio.DESCRIZIONE) + " "
								+ valPlaccaLinea.getAsInteger(PlaccheModuli.NUMERO_MODULI) + " " + ctx.getString(R.string.moduli));
				String chiaveElemento = idPlaccaModuli + "|" + valPlaccaLinea.getAsInteger(Linee.ID_LINEA);

				if (mappaLinee.containsKey(chiaveElemento)) {
					ContentValues val = mappaLinee.get(chiaveElemento);
					double qtaOld = val.getAsDouble(PreventiviDettaglio.QUANTITA);
					double qtaCurr = curr.getAsDouble(PreventiviDettaglio.QUANTITA);
					val.put(PreventiviDettaglio.QUANTITA, qtaOld + qtaCurr);

				} else {
					curr.put(Linee.NOME_LINEA, valPlaccaLinea.getAsString(Linee.NOME_LINEA));
					curr.put(Costruttori.SIGLA_METEL, valPlaccaLinea.getAsString(Costruttori.SIGLA_METEL));
					mappaLinee.put(chiaveElemento, curr);
				}
			}

		}

		// collegamenti
		for (int i = 0; i < collegamenti.size(); i++) {
			ContentValues curr = (ContentValues) collegamenti.get(i);
			gestitoALinea = false;

			String elemento = curr.getAsString(PreventiviDettaglio.DESCRIZIONE).trim();

			int idlinea = 0;
			String nomeLInea = "";
			String siglaMetel = "";

			String chiaveElemento = elemento + "|" + idlinea;

			if (mappaLinee.containsKey(chiaveElemento)) {
				ContentValues val = mappaLinee.get(chiaveElemento);
				double qtaOld = val.getAsDouble(PreventiviDettaglio.QUANTITA);
				double qtaCurr = curr.getAsDouble(PreventiviDettaglio.QUANTITA);
				val.put(PreventiviDettaglio.QUANTITA, qtaOld + qtaCurr);

			} else {
				curr.put(Linee.NOME_LINEA, nomeLInea);
				curr.put(Costruttori.SIGLA_METEL, siglaMetel);
				mappaLinee.put(chiaveElemento, curr);
			}
		}

		listaRaggruppata.addAll(mappaLinee.values());

		// prendo i fornitori degli eventuali codici articolo
		for (int i = 0; i < listaRaggruppata.size(); i++) {
			ContentValues valCurr = (ContentValues) listaRaggruppata.get(i);
			_aggiungiInformazioniFornitoreLinea(valCurr, db);
		}

		return listaRaggruppata;
	}

	private void _aggiungiRigaIntestazioneMateriale() {
		// TODO Auto-generated method stub
		// Generate column headings
		Cell c = null;

		// Cell style for header row
		CellStyle cs = wb.createCellStyle();
		cs.setFillForegroundColor(HSSFColor.LIME.index);
		cs.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

		Row row = sheet1.createRow(rowCount);

		c = row.createCell(0);
		c.setCellValue(ctx.getResources().getString(R.string.codice_articolo));
		c.setCellStyle(cs);

		c = row.createCell(1);
		c.setCellValue(ctx.getResources().getString(R.string.descrizione));
		c.setCellStyle(cs);

		c = row.createCell(2);
		c.setCellValue(ctx.getResources().getString(R.string.siglametel));
		c.setCellStyle(cs);

		c = row.createCell(3);
		c.setCellValue(ctx.getResources().getString(R.string.linea));
		c.setCellStyle(cs);

		c = row.createCell(4);
		c.setCellValue(ctx.getResources().getString(R.string.quantita));
		c.setCellStyle(cs);

		c = row.createCell(5);
		c.setCellValue(ctx.getResources().getString(R.string.unita_misura));
		c.setCellStyle(cs);

		c = row.createCell(6);
		c.setCellValue(ctx.getResources().getString(R.string.prezzo));
		c.setCellStyle(cs);

		// colonna descrizione ripetuta per il confronto nell'importazione dal fornitore
		c = row.createCell(10);
		c.setCellValue("");
		c.setCellStyle(cs);

		c = row.createCell(11);
		c.setCellValue("");
		c.setCellStyle(cs);

		sheet1.setColumnWidth(0, (15 * 200));
		sheet1.setColumnWidth(1, (15 * 500));
		sheet1.setColumnWidth(2, (15 * 150));
		sheet1.setColumnWidth(3, (15 * 300));
		sheet1.setColumnWidth(4, (15 * 200));
		sheet1.setColumnWidth(5, (15 * 200));
		sheet1.setColumnWidth(6, (15 * 200));

		sheet1.setColumnHidden(10, true);
		sheet1.setColumnHidden(11, true);

		rowCount++;
	}

	private void _aggiungiRigaMateriale(ContentValues val) {
		// TODO Auto-generated method stub
		Row riga = sheet1.createRow(rowCount);

		Cell c = riga.createCell(0);
		c.setCellValue(val.getAsString(PreventiviDettaglio.CODICE_ARTICOLO));// CODICE ARTICOLO

		c = riga.createCell(1);
		c.setCellValue(val.getAsString(PreventiviDettaglio.DESCRIZIONE));

		// colonna descrizione ripetuta per il confronto in fase di importazione prezzi
		c = riga.createCell(10);
		c.setCellValue(val.getAsString(PreventiviDettaglio.DESCRIZIONE));

		c = riga.createCell(2);
		c.setCellValue(val.getAsString(Costruttori.SIGLA_METEL));// SIGLA_METEL

		c = riga.createCell(3);
		c.setCellValue(val.getAsString(Linee.NOME_LINEA));// LINEA

		// colonna linea ripetuta per il confronto in fase di importazione prezzi
		c = riga.createCell(11);
		c.setCellValue(val.getAsString(Linee.NOME_LINEA));

		c = riga.createCell(4);
		c.setCellValue(val.getAsDouble(PreventiviDettaglio.QUANTITA));// QTA

		c = riga.createCell(5);
		c.setCellValue(val.getAsString(PreventiviDettaglio.UNITA_MISURA));// UDM

		c = riga.createCell(6);
		c.setCellValue(0.0f);// PREZZO
		c.setCellType(Cell.CELL_TYPE_NUMERIC);

		rowCount++;
	}

	private void _aggiungiRigaPreventivo(ContentValues val) {
		// TODO Auto-generated method stub

		String tipo = val.getAsString(PreventiviDettaglio.TIPO);

		Row riga = sheet1.createRow(rowCount);

		CellStyle cs = wb.createCellStyle();
		cs.setWrapText(true);

		CellStyle currencyCellStyle = wb.createCellStyle();
		currencyCellStyle.setWrapText(true);

		currencyCellStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

		Cell c = riga.createCell(0);
		c.setCellValue(val.getAsString(PreventiviDettaglio.CODICE_ARTICOLO));// CODICE ARTICOLO
		c.setCellStyle(cs);

		c = riga.createCell(1);
		if (tipo.equals(PreventiviDettaglio.ALTRO)) {
			c.setCellValue(val.getAsString(PreventiviDettaglio.NOTE));
		} else {
			c.setCellValue(val.getAsString(PreventiviDettaglio.DESCRIZIONE));
		}

		c.setCellStyle(cs);

		c = riga.createCell(2);
		if (tipo.equals(PreventiviDettaglio.MATERIALE) || tipo.equals(PreventiviDettaglio.MATERIALE_PREVENTIVO)
				|| tipo.equals(PreventiviDettaglio.PLACCHE) || tipo.equals(PreventiviDettaglio.PLACCHE_PREVENTIVO)
				|| tipo.equals(PreventiviDettaglio.COLLEGAMENTI)) {
			c.setCellValue(val.getAsString(Costruttori.SIGLA_METEL));// SIGLA METEL
		} else {
			c.setCellValue("");

		}

		c.setCellStyle(cs);

		c = riga.createCell(3);
		if (tipo.equals(PreventiviDettaglio.MATERIALE) || tipo.equals(PreventiviDettaglio.MATERIALE_PREVENTIVO)
				|| tipo.equals(PreventiviDettaglio.PLACCHE) || tipo.equals(PreventiviDettaglio.PLACCHE_PREVENTIVO)) {
			c.setCellValue(val.getAsString(Linee.NOME_LINEA));// LINEA
		} else {
			c.setCellValue("");

		}

		c.setCellStyle(cs);

		c = riga.createCell(4);
		int numeroOperatori = val.getAsInteger(PreventiviDettaglio.NUM_OPERATORI);
		double numeroOre = val.getAsDouble(PreventiviDettaglio.QUANTITA);
		if (tipo.equals(PreventiviDettaglio.MANOPERA)) {

			if (numeroOperatori >= 1) {
				numeroOre = numeroOre * numeroOperatori;
			}
			c.setCellValue(numeroOre);// QTA
		} else {
			c.setCellValue(val.getAsDouble(PreventiviDettaglio.QUANTITA));// QTA
		}

		c.setCellStyle(cs);

		c = riga.createCell(5);
		if (tipo.equals(PreventiviDettaglio.MANOPERA)) {
			if (numeroOperatori > 1) {
				c.setCellValue(val.getAsString(PreventiviDettaglio.UNITA_MISURA) + " (x " + numeroOperatori + " "
						+ ctx.getResources().getString(R.string.operatori).toLowerCase(Locale.getDefault()) + ")");// UDM
			} else {
				c.setCellValue(val.getAsString(PreventiviDettaglio.UNITA_MISURA));// UDM
			}

		} else {
			c.setCellValue(val.getAsString(PreventiviDettaglio.UNITA_MISURA));// UDM
		}

		c.setCellStyle(cs);

		c = riga.createCell(6);
		c.setCellValue(Utility.arrotonda(val.getAsDouble(PreventiviDettaglio.PREZZO), 2));// PREZZO
		c.setCellType(Cell.CELL_TYPE_NUMERIC);

		c.setCellStyle(currencyCellStyle);

		c = riga.createCell(7);

		String cellaQta = "E" + (rowCount + 1);
		String cellaPrezzo = "G" + (rowCount + 1);

		c.setCellFormula("PRODUCT(" + cellaQta + ":" + cellaPrezzo + ")");
		c.setCellValue(tabPrev.getImportoRiga(val));// importo
		c.setCellType(Cell.CELL_TYPE_FORMULA);
		c.setCellStyle(currencyCellStyle);

		c = riga.createCell(8);

		try {
			c.setCellValue(mappaIva.get(val.getAsString(PreventiviDettaglio.CODICE_IVA)));// aliquota
		} catch (Exception e) {
			c.setCellValue(0);
		}

		c.setCellType(Cell.CELL_TYPE_NUMERIC);
		c.setCellStyle(cs);

		c = riga.createCell(9);
		try {
			c.setCellFormula("H" + (rowCount + 1) + "*I" + (rowCount + 1) + "/100");
			c.setCellValue(Utility.arrotonda(tabPrev.getImportoRiga(val) * mappaIva.get(val.getAsString(PreventiviDettaglio.CODICE_IVA))
					/ 100, 2));// importo
			// iva
		} catch (Exception e) {

		}

		c.setCellType(Cell.CELL_TYPE_FORMULA);
		c.setCellStyle(cs);

		rowCount++;
	}

	private void _aggiungiRigaIntestazione() {
		// TODO Auto-generated method stub
		// Generate column headings
		Cell c = null;

		// Cell style for header row
		CellStyle cs = wb.createCellStyle();
		Font fBold = wb.createFont();
		fBold.setBoldweight(Font.BOLDWEIGHT_BOLD);
		wb.getCustomPalette().setColorAtIndex(HSSFColor.GREY_25_PERCENT.index, (byte) 236, (byte) 236, (byte) 236);
		cs.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
		cs.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		cs.setFont(fBold);

		Row row = sheet1.createRow(rowCount);

		c = row.createCell(0);
		c.setCellValue(ctx.getResources().getString(R.string.codice).toUpperCase(Locale.getDefault()));
		c.setCellStyle(cs);

		c = row.createCell(1);
		c.setCellValue(ctx.getResources().getString(R.string.descrizione).toUpperCase(Locale.getDefault()));
		c.setCellStyle(cs);

		c = row.createCell(2);
		c.setCellValue(ctx.getResources().getString(R.string.siglametel).toUpperCase(Locale.getDefault()));
		c.setCellStyle(cs);

		c = row.createCell(3);
		c.setCellValue(ctx.getResources().getString(R.string.linea).toUpperCase(Locale.getDefault()));
		c.setCellStyle(cs);

		c = row.createCell(4);
		c.setCellValue(ctx.getResources().getString(R.string.quantita).toUpperCase(Locale.getDefault()));
		c.setCellStyle(cs);

		c = row.createCell(5);
		c.setCellValue(ctx.getResources().getString(R.string.unita_misura).toUpperCase(Locale.getDefault()));
		c.setCellStyle(cs);

		c = row.createCell(6);
		c.setCellValue(ctx.getResources().getString(R.string.prezzo).toUpperCase(Locale.getDefault()));
		c.setCellStyle(cs);

		c = row.createCell(7);
		c.setCellValue(ctx.getResources().getString(R.string.importo).toUpperCase(Locale.getDefault()));
		c.setCellStyle(cs);

		c = row.createCell(8);
		c.setCellValue(ctx.getResources().getString(R.string.aliquota).toUpperCase(Locale.getDefault()));
		c.setCellStyle(cs);

		sheet1.setColumnWidth(0, (15 * 200));
		sheet1.setColumnWidth(1, (15 * 500));
		sheet1.setColumnWidth(2, (15 * 170));
		sheet1.setColumnWidth(3, (15 * 300));
		sheet1.setColumnWidth(4, (15 * 200));
		sheet1.setColumnWidth(5, (15 * 200));
		sheet1.setColumnWidth(6, (15 * 200));
		sheet1.setColumnWidth(7, (15 * 200));
		sheet1.setColumnWidth(8, (15 * 200));

		rowCount++;
	}
}
