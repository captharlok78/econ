package pfa.app.econtab.export;

import android.content.ContentValues;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFCellUtil;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.CategorieGenerali;
import pfa.app.econtab.db.table.Collegamenti;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.db.table.ComponentiCantComposti;
import pfa.app.econtab.db.table.ComponentiCantiere;
import pfa.app.econtab.db.table.ComposizioniCantiere;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.utils.Utility;

public class ComposizioneScatoleXLS {

	private Context ctx = null;
	int rowCount = 1;
	private HSSFWorkbook wb = null;
	private CreationHelper helper = null;

	private Preventivi tabPreventivi = null;
	private PreventiviDettaglio tabPreventiviDettaglio = null;
	private Locali tabLocali = null;
	private ElementiCantiere tabElementi = null;
	private ComposizioniCantiere tabComposizioniCantiere = null;
	private ComponentiCantComposti tabComponentiCantComposti = null;
	private Collegamenti tabColl = null;
	private ComponentiCantiere tabComponenti = null;

	private int colonnaTesti = 0;
	private HSSFCellStyle stileElemento = null;
	private HSSFCellStyle stileComposizione = null;
	private HSSFCellStyle stileComposti = null;

	// private HSSFCellStyle stileComposizioneBottom = null;
	// private HSSFCellStyle stileCompostiBottom = null;

	private int idCantiere = 0;
	private ArrayList<Object> tubi = null;
	private ArrayList<Object> cavi = null;

	private ArrayList<Object> relazioni = null;
	private ArrayList<Object> locali = null;

	public ComposizioneScatoleXLS(Context ctx) {
		this.ctx = ctx;
		wb = new HSSFWorkbook();
		helper = wb.getCreationHelper();

		tabPreventivi = new Preventivi();
		tabPreventiviDettaglio = new PreventiviDettaglio();
		tabLocali = new Locali();
		tabElementi = new ElementiCantiere();
		tabComposizioniCantiere = new ComposizioniCantiere();
		tabComponentiCantComposti = new ComponentiCantComposti();
		tabColl = new Collegamenti();
		tabComponenti = new ComponentiCantiere();
	}

	public String generaReport(int idPreventivo, boolean conPiantine) throws Exception {

		_preparaStili();

		DbInterno db = new DbInterno(ctx);
		ContentValues where = new ContentValues();
		where.put(Preventivi.ID_PREVENTIVO, idPreventivo);

		ContentValues recPrev = db.getRecord(tabPreventivi, where);
        String tipo = Preventivi.TIPO_PREVENTIVO;
		if (recPrev != null) {
			// preparo la mappa dei codici associati al preventivo
            tipo = recPrev.getAsString(Preventivi.TIPO);
			idCantiere = recPrev.getAsInteger(Preventivi.ID_CANTIERE);
			locali = tabLocali.getLocaliCantiereOrdineAlfabetico(db, idCantiere);

			ContentValues whereTubi = new ContentValues();
			whereTubi.put(Elementi.ID_CATEGORIA_GENERALE, CategorieGenerali.TUBI);
			ContentValues whereCavi = new ContentValues();
			whereCavi.put(Elementi.ID_CATEGORIA_GENERALE, CategorieGenerali.CAVI);
			tubi = db.eseguiSelect(new Elementi(), whereTubi, null);
			cavi = db.eseguiSelect(new Elementi(), whereCavi, null);

			_caricaRelazioniOrdine(db, idPreventivo);

			for (int i = 0; i < locali.size(); i++) {
				_generaFoglioLocale(db, (ContentValues) locali.get(i), idPreventivo, conPiantine);
			}

		}

        Preventivi tabPreventivi = new Preventivi();
        ContentValues valClie =  tabPreventivi.getClienteCantierePreventivo(db,idPreventivo);
        String ragioneSociale = "";
        if (valClie!=null){
            ragioneSociale = valClie.getAsString(Anagrafica.RAGIONE_SOCIALE);
        }

        ragioneSociale = Utility.formattaStringaPerNomeFile(ragioneSociale);
        String cartellaPrev = PreventiviDettaglio.PATH_EXPORT_PREVENTIVI;
        if (tipo.equals(Preventivi.TIPO_ORDINE)){
            cartellaPrev = PreventiviDettaglio.PATH_EXPORT_ORDINI;
        }

		// Create a path where we will place our List of objects on external storage
		File dirExport = new File(Environment.getExternalStorageDirectory() + File.separator + "ECONTAB" + File.separator+ragioneSociale+File.separator
				+ cartellaPrev);

		if (!dirExport.exists()) {
			dirExport.mkdirs();
		}

		String nomePrev = "scatole_" + recPrev.getAsString(Preventivi.TIPO) + recPrev.getAsInteger(Preventivi.NUMERO) + "_"
				+ recPrev.getAsInteger(Preventivi.ANNO);

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
			db.close();
			try {

				if (os != null)
					os.close();
			} catch (Exception ex) {
			}
		}
	}

	private void _preparaStili() {
		// TODO Auto-generated method stub
		stileElemento = wb.createCellStyle();

		Font fBold = wb.createFont();
		fBold.setBoldweight(Font.BOLDWEIGHT_BOLD);

		Font fBoldBianco = wb.createFont();
		fBoldBianco.setBoldweight(Font.BOLDWEIGHT_BOLD);
		fBoldBianco.setColor(HSSFColor.WHITE.index);

		Font fComp = wb.createFont();
		fComp.setItalic(true);
		fComp.setFontHeightInPoints((short) 9);

		wb.getCustomPalette().setColorAtIndex(HSSFColor.GREY_25_PERCENT.index, (byte) 236, (byte) 236, (byte) 236);

		stileElemento.setFillForegroundColor(HSSFColor.LIGHT_BLUE.index);
		stileElemento.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		stileElemento.setFont(fBoldBianco);
		stileElemento.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		stileElemento.setBorderTop(HSSFCellStyle.BORDER_THIN);
		stileElemento.setBorderRight(HSSFCellStyle.BORDER_THIN);
		stileElemento.setBorderBottom(HSSFCellStyle.BORDER_THIN);

		stileComposizione = wb.createCellStyle();
		stileComposizione.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
		stileComposizione.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		stileComposizione.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		stileComposizione.setBorderRight(HSSFCellStyle.BORDER_THIN);
		stileComposizione.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		stileComposizione.setBorderTop(HSSFCellStyle.BORDER_THIN);
		stileComposizione.setBottomBorderColor(HSSFColor.GREY_50_PERCENT.index);
		stileComposizione.setTopBorderColor(HSSFColor.GREY_50_PERCENT.index);
		stileComposizione.setLeftBorderColor(HSSFColor.GREY_50_PERCENT.index);
		stileComposizione.setRightBorderColor(HSSFColor.GREY_50_PERCENT.index);

		// stileComposizioneBottom = wb.createCellStyle();
		// stileComposizioneBottom.setFillForegroundColor(HSSFColor.WHITE.index);
		// stileComposizioneBottom.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		// stileComposizioneBottom.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		// stileComposizioneBottom.setBorderRight(HSSFCellStyle.BORDER_THIN);
		// stileComposizioneBottom.setBorderBottom(HSSFCellStyle.BORDER_THIN);

		stileComposti = wb.createCellStyle();
		stileComposti.setFillForegroundColor(HSSFColor.WHITE.index);
		stileComposti.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		stileComposti.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		stileComposti.setBorderRight(HSSFCellStyle.BORDER_THIN);
		// stileComposti.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		// stileComposti.setBorderTop(HSSFCellStyle.BORDER_THIN);
		stileComposti.setBottomBorderColor(HSSFColor.GREY_50_PERCENT.index);
		stileComposti.setTopBorderColor(HSSFColor.GREY_50_PERCENT.index);
		stileComposti.setLeftBorderColor(HSSFColor.GREY_50_PERCENT.index);
		stileComposti.setRightBorderColor(HSSFColor.GREY_50_PERCENT.index);
		stileComposti.setFont(fComp);

		// stileCompostiBottom = wb.createCellStyle();
		// stileCompostiBottom.setFillForegroundColor(HSSFColor.WHITE.index);
		// stileCompostiBottom.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		// stileCompostiBottom.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		// stileCompostiBottom.setBorderRight(HSSFCellStyle.BORDER_THIN);
		// stileCompostiBottom.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		// stileCompostiBottom.setFont(fComp);
	}

	private void _generaFoglioLocale(DbInterno db, ContentValues valLocale, int idPreventivo, boolean conPiantine) {
		// TODO Auto-generated method stub
        HSSFSheet foglioLocale = null;
		String nomeLocale = valLocale.getAsString(Locali.NOME);
		if (nomeLocale!=null){
			nomeLocale = nomeLocale.replace("/","_");
		}
		else {
			nomeLocale = "";
		}
        try{
            foglioLocale = wb.createSheet(nomeLocale);
        }
		catch (Exception e) {
			nomeLocale = "Loc. ";
            foglioLocale = wb.createSheet(nomeLocale+""+valLocale.getAsInteger(Locali.ID_LOCALE));
        }
		rowCount = 1;
		colonnaTesti = 0;
		try {
			if (conPiantine) {
				File fileIco = Utility.getFileImmaginePiantina(valLocale.getAsInteger(Locali.ID_LOCALE));
				if (fileIco.exists()) {

					InputStream inputStream = new FileInputStream(fileIco);
					byte[] bytes = IOUtils.toByteArray(inputStream);
					int pictureIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
					inputStream.close();

					Drawing drawing = foglioLocale.createDrawingPatriarch();

					ClientAnchor anchor = helper.createClientAnchor(); //

					anchor.setCol1(0);
					anchor.setRow1(0);

					anchor.setCol2(6);
					anchor.setRow2(24);

					// Creates a picture
					drawing.createPicture(anchor, pictureIdx);

					colonnaTesti = 7;

				}
			}

			foglioLocale.setColumnWidth(colonnaTesti, (15 * 150));
			foglioLocale.setColumnWidth(colonnaTesti + 1, (15 * 800));
			foglioLocale.setColumnWidth(colonnaTesti + 2, (15 * 500));
			foglioLocale.setColumnWidth(colonnaTesti + 3, (15 * 500));
			foglioLocale.setColumnWidth(colonnaTesti + 4, (15 * 500));
			foglioLocale.setColumnWidth(colonnaTesti + 5, (15 * 500));
			foglioLocale.setColumnWidth(colonnaTesti + 6, (15 * 200));
			foglioLocale.setColumnWidth(colonnaTesti + 7, (15 * 500));

			ContentValues whereElementi = new ContentValues();
			whereElementi.put(ElementiCantiere.ID_PREVENTIVO, idPreventivo);
			whereElementi.put(ElementiCantiere.ID_LOCALE, valLocale.getAsInteger(Locali.ID_LOCALE));
			whereElementi.put(ElementiCantiere.ID_CANTIERE, 0);

			ArrayList<Object> righe = db.eseguiSelect(tabElementi, whereElementi, new String[] { ElementiCantiere.NUMERO_IDENTIFICATIVO });

			whereElementi.clear();
			whereElementi.put(ElementiCantiere.ID_LOCALE, valLocale.getAsInteger(Locali.ID_LOCALE));
			whereElementi.put(ElementiCantiere.ID_CANTIERE, idCantiere);
			ArrayList<Object> righeCantiere = db.eseguiSelect(tabElementi, whereElementi,
					new String[] { ElementiCantiere.NUMERO_IDENTIFICATIVO });

			Locali tabLoc = new Locali();
			ContentValues lineaLocale =  tabLoc.getLineaLocale(db,valLocale.getAsInteger(Locali.ID_LOCALE));
			for (int i = 0; i < righe.size(); i++) {
				int rigaIniziale = rowCount;
				_generaTabellaComposizione(db, foglioLocale, (ContentValues) righe.get(i), idPreventivo,lineaLocale);
				int rigaFinale = rowCount - 1;
				_impostaBordoElemento(rigaIniziale, rigaFinale, foglioLocale);
			}

			for (int i = 0; i < righeCantiere.size(); i++) {
				int rigaIniziale = rowCount;
				_generaTabellaComposizione(db, foglioLocale, (ContentValues) righeCantiere.get(i), 0,null);
				int rigaFinale = rowCount - 1;
				_impostaBordoElemento(rigaIniziale, rigaFinale, foglioLocale);
			}

		} catch (Exception e) {
			Toast.makeText(ctx, Log.getStackTraceString(e), Toast.LENGTH_LONG).show();
		}

	}

	private void _impostaBordoElemento(int rigaIniziale, int rigaFinale, HSSFSheet foglioLocale) {

		HSSFRow riga = HSSFCellUtil.getRow(rigaIniziale, foglioLocale);
		HSSFCell cella = HSSFCellUtil.getCell(riga, colonnaTesti);

		HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.BORDER_LEFT, HSSFCellStyle.BORDER_MEDIUM);
		HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.LEFT_BORDER_COLOR, HSSFColor.LIGHT_BLUE.index);
		HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.BORDER_TOP, HSSFCellStyle.BORDER_MEDIUM);
		HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.TOP_BORDER_COLOR, HSSFColor.LIGHT_BLUE.index);

		for (int i = 1; i < 7; i++) {
			cella = HSSFCellUtil.getCell(riga, colonnaTesti + i);
			HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.BORDER_TOP, HSSFCellStyle.BORDER_MEDIUM);
			HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.TOP_BORDER_COLOR, HSSFColor.LIGHT_BLUE.index);
		}

		cella = HSSFCellUtil.getCell(riga, colonnaTesti + 7);
		HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.BORDER_TOP, HSSFCellStyle.BORDER_MEDIUM);
		HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.BORDER_RIGHT, HSSFCellStyle.BORDER_MEDIUM);
		HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.TOP_BORDER_COLOR, HSSFColor.LIGHT_BLUE.index);
		HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.RIGHT_BORDER_COLOR, HSSFColor.LIGHT_BLUE.index);

		for (int i = rigaIniziale + 1; i < rigaFinale; i++) {
			riga = HSSFCellUtil.getRow(i, foglioLocale);
			cella = HSSFCellUtil.getCell(riga, colonnaTesti);
			HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.BORDER_LEFT, HSSFCellStyle.BORDER_MEDIUM);
			HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.LEFT_BORDER_COLOR, HSSFColor.LIGHT_BLUE.index);

			cella = HSSFCellUtil.getCell(riga, colonnaTesti + 7);
			HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.BORDER_RIGHT, HSSFCellStyle.BORDER_MEDIUM);
			HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.RIGHT_BORDER_COLOR, HSSFColor.LIGHT_BLUE.index);
		}

		riga = HSSFCellUtil.getRow(rigaFinale, foglioLocale);
		for (int i = 0; i <= 7; i++) {
			cella = HSSFCellUtil.getCell(riga, colonnaTesti + i);
			HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.BORDER_TOP, HSSFCellStyle.BORDER_MEDIUM);
			HSSFCellUtil.setCellStyleProperty(cella, wb, CellUtil.TOP_BORDER_COLOR, HSSFColor.LIGHT_BLUE.index);
		}

	}

	private void _generaTabellaComposizione(DbInterno db, HSSFSheet foglioLocale, ContentValues valElemento, int idPreventivo,ContentValues lineaLocale) {
		// TODO Auto-generated method stub

		HSSFRow riga = HSSFCellUtil.getRow(rowCount, foglioLocale);
		rowCount++;

		HSSFCell cella = riga.createCell(colonnaTesti);
		cella.setCellStyle(stileElemento);

		cella = riga.createCell(colonnaTesti + 1);
		cella.setCellStyle(stileElemento);

		// vedo le composizioni
		int idElementoCantiere = valElemento.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT);

		ArrayList<Object> composizioni = tabComposizioniCantiere.getComposizioneElemento(db, idElementoCantiere);

		String codiceElemento = "";
		if (composizioni.size() == 0) {
			ContentValues whereRigaPrev = new ContentValues();
			whereRigaPrev.put(PreventiviDettaglio.ID_LOCALE, valElemento.getAsInteger(ElementiCantiere.ID_LOCALE));
			whereRigaPrev.put(PreventiviDettaglio.ID_ELEMENTO, valElemento.getAsInteger(ElementiCantiere.ID_ELEMENTO));
			whereRigaPrev.put(PreventiviDettaglio.ID_PREVENTIVO, idPreventivo);

			ContentValues recPrevDett = null;
			ArrayList<Object> recs = db.eseguiSelect(tabPreventiviDettaglio,whereRigaPrev,null);
			if (recs!=null && recs.size()>0){
				if (lineaLocale==null ){
					recPrevDett = (ContentValues)recs.get(0);
				}
				else{
					int idLineaLocale = lineaLocale.getAsInteger(Linee.ID_LINEA);
					if (!valElemento.containsKey(ElementiCantiere.ID_LINEA) || valElemento.get(ElementiCantiere.ID_LINEA)==null || valElemento.getAsString(ElementiCantiere.ID_LINEA).equals("null") || valElemento.getAsInteger(ElementiCantiere.ID_LINEA)==0){
						for (int r = 0;r<recs.size();r++){
							recPrevDett = (ContentValues)recs.get(r);
							if (recPrevDett.containsKey(PreventiviDettaglio.ID_LINEA) && recPrevDett.get(PreventiviDettaglio.ID_LINEA)!=null && !recPrevDett.getAsString(PreventiviDettaglio.ID_LINEA).equals("")){
								if (idLineaLocale==recPrevDett.getAsInteger(PreventiviDettaglio.ID_LINEA) || recPrevDett.getAsInteger(PreventiviDettaglio.ID_LINEA)==0){
									break;
								}
							}
						}
					}
					else {
						//guardo la linea dell'elemento e se è diversa da quella del locale prendo la riga giusta
						int idLineaElemento = valElemento.getAsInteger(ElementiCantiere.ID_LINEA);
						for (int r = 0;r<recs.size();r++){
							recPrevDett = (ContentValues)recs.get(r);
							if (recPrevDett.containsKey(PreventiviDettaglio.ID_LINEA) && recPrevDett.get(PreventiviDettaglio.ID_LINEA)!=null && !recPrevDett.getAsString(PreventiviDettaglio.ID_LINEA).equals("")){
								if (idLineaElemento==recPrevDett.getAsInteger(PreventiviDettaglio.ID_LINEA) ){
									break;
								}
							}
						}

					}


				}
			}

			//ereRigaPrev);
			if (recPrevDett != null && recPrevDett.getAsString(PreventiviDettaglio.CODICE_ARTICOLO) != null
					&& recPrevDett.getAsString(PreventiviDettaglio.CODICE_ARTICOLO).length() > 0) {
				codiceElemento = " (" + ctx.getString(R.string.codice) + " " + recPrevDett.getAsString(PreventiviDettaglio.CODICE_ARTICOLO)
						+ ")";
			}

			int righeCollegamenti = _creaRigheCollegamenti(db, idElementoCantiere, false, false, foglioLocale);
			rowCount = rowCount + righeCollegamenti;
		}

		cella.setCellValue(valElemento.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO) + " - "
				+ valElemento.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT) + codiceElemento);

		cella = riga.createCell(colonnaTesti + 2);
		cella.setCellStyle(stileElemento);
		cella.setCellValue(ctx.getString(R.string.elemento_collegato));

		cella = riga.createCell(colonnaTesti + 3);
		cella.setCellStyle(stileElemento);
		cella.setCellValue(ctx.getString(R.string.componente_collegato));

		cella = riga.createCell(colonnaTesti + 4);
		cella.setCellStyle(stileElemento);
		cella.setCellValue(ctx.getString(R.string.locale_elemento_collegato));

		cella = riga.createCell(colonnaTesti + 5);
		cella.setCellStyle(stileElemento);
		cella.setCellValue(ctx.getString(R.string.collegamento));

		cella = riga.createCell(colonnaTesti + 6);
		cella.setCellStyle(stileElemento);
		cella.setCellValue(ctx.getString(R.string.metri));

		cella = riga.createCell(colonnaTesti + 7);
		cella.setCellStyle(stileElemento);
		cella.setCellValue(ctx.getString(R.string.note));

		/*
		 * if (composizioni.size() > 0) { riga = Utility.getRigaFoglioXLS(rowCount); rowCount++; cella =
		 * riga.createCell(colonnaTesti); cella.setCellStyle(stileComposizione);
		 * cella.setCellValue(ctx.getResources().getString(R.string.composizione));
		 * 
		 * // aggiungo una cella vuota riga = Utility.getRigaFoglioXLS(rowCount); rowCount++; cella =
		 * riga.createCell(colonnaTesti); cella.setCellStyle(stileComposizione); }
		 */

		int totalerighe = composizioni.size();
		int righeInserite = 0;
		for (int i = 0; i < composizioni.size(); i++) {
			ContentValues comp = (ContentValues) composizioni.get(i);
			righeInserite = righeInserite + 1;

			int righeCollegamenti = 0;
			if (i == 0) {
				// se sono nella scatola (i==0) verifico se ci sono i collegamenti tubo
				righeCollegamenti = _creaRigheCollegamenti(db, idElementoCantiere, false, true, foglioLocale);
			} else {
				righeCollegamenti = _creaRigheCollegamenti(db, comp.getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT), true, false,
						foglioLocale);
			}

			ArrayList<Object> composti = tabComponentiCantComposti.getComposizioneComponente(db,
					comp.getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT));

			riga = HSSFCellUtil.getRow(rowCount, foglioLocale);

			cella = riga.createCell(colonnaTesti);
			// if (righeInserite == totalerighe) {
			// cella.setCellStyle(stileComposizioneBottom);
			// } else {
			cella.setCellStyle(stileComposizione);
			// }

			String moduliOccupati = comp.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
			if (!moduliOccupati.trim().equals("")) {
				moduliOccupati = "Pos. " + moduliOccupati;
			}
			cella.setCellValue(moduliOccupati);

			if (comp.getAsString(ComponentiCantiere.NOTA) != null && !comp.getAsString(ComponentiCantiere.NOTA).equals("")) {
				cella = riga.createCell(colonnaTesti + 7);// note
				cella.setCellStyle(stileComposizione);
				cella.setCellValue(comp.getAsString(ComponentiCantiere.NOTA));
			}

			cella = riga.createCell(colonnaTesti + 1);
			// if (righeInserite == totalerighe) {
			// cella.setCellStyle(stileComposizioneBottom);
			// } else {
			cella.setCellStyle(stileComposizione);
			// }
			if (composti.size() == 0) {
				ContentValues whereRigaPrev = new ContentValues();
				whereRigaPrev.put(PreventiviDettaglio.ID_LOCALE, valElemento.getAsInteger(ElementiCantiere.ID_LOCALE));
				whereRigaPrev.put(PreventiviDettaglio.ID_COMPONENTE, comp.getAsInteger(Componenti.ID_COMPONENTE));
				whereRigaPrev.put(PreventiviDettaglio.ID_PREVENTIVO, idPreventivo);



				String codiceArticolo = "";

				ContentValues recPrevDett = null;
				ArrayList<Object> recs = db.eseguiSelect(tabPreventiviDettaglio,whereRigaPrev,null);
				if (recs!=null && recs.size()>0){
					if (lineaLocale==null ){
						recPrevDett = (ContentValues)recs.get(0);
					}
					else{
						int idLineaLocale = lineaLocale.getAsInteger(Linee.ID_LINEA);
						if (!valElemento.containsKey(ElementiCantiere.ID_LINEA) || valElemento.get(ElementiCantiere.ID_LINEA)==null || valElemento.getAsString(ElementiCantiere.ID_LINEA).equals("null") || valElemento.getAsInteger(ElementiCantiere.ID_LINEA)==0){
							for (int r = 0;r<recs.size();r++){
								recPrevDett = (ContentValues)recs.get(r);
								if (recPrevDett.containsKey(PreventiviDettaglio.ID_LINEA) && recPrevDett.get(PreventiviDettaglio.ID_LINEA)!=null && !recPrevDett.getAsString(PreventiviDettaglio.ID_LINEA).equals("")){
									if (idLineaLocale==recPrevDett.getAsInteger(PreventiviDettaglio.ID_LINEA) || recPrevDett.getAsInteger(PreventiviDettaglio.ID_LINEA)==0){
										break;
									}
								}
							}
						}
						else {
							//guardo la linea dell'elemento e se è diversa da quella del locale prendo la riga giusta
							int idLineaElemento = valElemento.getAsInteger(ElementiCantiere.ID_LINEA);
							for (int r = 0;r<recs.size();r++){
								recPrevDett = (ContentValues)recs.get(r);
								if (recPrevDett.containsKey(PreventiviDettaglio.ID_LINEA) && recPrevDett.get(PreventiviDettaglio.ID_LINEA)!=null && !recPrevDett.getAsString(PreventiviDettaglio.ID_LINEA).equals("")){
									if (idLineaElemento==recPrevDett.getAsInteger(PreventiviDettaglio.ID_LINEA) ){
										break;
									}
								}
							}

						}
					}
				}


				//ContentValues recPrevDett = db.getRecord(tabPreventiviDettaglio, whereRigaPrev);
				if (recPrevDett != null && recPrevDett.getAsString(PreventiviDettaglio.CODICE_ARTICOLO) != null
						&& recPrevDett.getAsString(PreventiviDettaglio.CODICE_ARTICOLO).length() > 0) {
					codiceArticolo = " (" + ctx.getString(R.string.cod) + " "
							+ recPrevDett.getAsString(PreventiviDettaglio.CODICE_ARTICOLO) + ")";
				}
				cella.setCellValue(comp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT) + codiceArticolo);

				if (righeCollegamenti > 1) {
					for (int r = 1; r < righeCollegamenti; r++) {
						HSSFCellUtil.getRow(rowCount + r, foglioLocale).createCell(colonnaTesti).setCellStyle(stileComposti);
						HSSFCellUtil.getRow(rowCount + r, foglioLocale).createCell(colonnaTesti + 1).setCellStyle(stileComposti);
					}

					// rowCount = rowCount + righeCollegamenti - 1;
				}

			} else {
				cella.setCellValue(comp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));

				for (int c = 0; c < composti.size(); c++) {
					ContentValues composto = (ContentValues) composti.get(c);
					righeInserite = righeInserite + 1;
					riga = HSSFCellUtil.getRow(rowCount + c + 1, foglioLocale);

					cella = riga.createCell(colonnaTesti);
					// if (righeInserite == totalerighe) {
					// cella.setCellStyle(stileCompostiBottom);
					// } else {
					cella.setCellStyle(stileComposti);
					// }
					cella.setCellValue("");

					cella = riga.createCell(colonnaTesti + 1);
					// if (righeInserite == totalerighe) {
					// cella.setCellStyle(stileCompostiBottom);
					// } else {
					cella.setCellStyle(stileComposti);
					// }

					ContentValues whereRigaPrev = new ContentValues();
					whereRigaPrev.put(PreventiviDettaglio.ID_LOCALE, valElemento.getAsInteger(ElementiCantiere.ID_LOCALE));
					whereRigaPrev.put(PreventiviDettaglio.ID_COMPONENTE, composto.getAsInteger(Componenti.ID_COMPONENTE));
					whereRigaPrev.put(PreventiviDettaglio.ID_PREVENTIVO, idPreventivo);

					String codiceArticolo = "";

					ContentValues recPrevDett = null;
					ArrayList<Object> recs = db.eseguiSelect(tabPreventiviDettaglio,whereRigaPrev,null);
					if (recs!=null && recs.size()>0){
						if (lineaLocale==null ){
							recPrevDett = (ContentValues)recs.get(0);
						}
						else{
							int idLineaLocale = lineaLocale.getAsInteger(Linee.ID_LINEA);
							if (!valElemento.containsKey(ElementiCantiere.ID_LINEA) || valElemento.get(ElementiCantiere.ID_LINEA)==null || valElemento.getAsString(ElementiCantiere.ID_LINEA).equals("null") || valElemento.getAsInteger(ElementiCantiere.ID_LINEA)==0){
								for (int r = 0;r<recs.size();r++){
									recPrevDett = (ContentValues)recs.get(r);
									if (recPrevDett.containsKey(PreventiviDettaglio.ID_LINEA) && recPrevDett.get(PreventiviDettaglio.ID_LINEA)!=null && !recPrevDett.getAsString(PreventiviDettaglio.ID_LINEA).equals("")){
										if (idLineaLocale==recPrevDett.getAsInteger(PreventiviDettaglio.ID_LINEA) || recPrevDett.getAsInteger(PreventiviDettaglio.ID_LINEA)==0){
											break;
										}
									}
								}
							}
							else {
								//guardo la linea dell'elemento e se è diversa da quella del locale prendo la riga giusta
								int idLineaElemento = valElemento.getAsInteger(ElementiCantiere.ID_LINEA);
								for (int r = 0;r<recs.size();r++){
									recPrevDett = (ContentValues)recs.get(r);
									if (recPrevDett.containsKey(PreventiviDettaglio.ID_LINEA) && recPrevDett.get(PreventiviDettaglio.ID_LINEA)!=null && !recPrevDett.getAsString(PreventiviDettaglio.ID_LINEA).equals("")){
										if (idLineaElemento==recPrevDett.getAsInteger(PreventiviDettaglio.ID_LINEA) ){
											break;
										}
									}
								}

							}
						}
					}

					//ContentValues recPrevDett = db.getRecord(tabPreventiviDettaglio, whereRigaPrev);
					if (recPrevDett != null && recPrevDett.getAsString(PreventiviDettaglio.CODICE_ARTICOLO) != null
							&& recPrevDett.getAsString(PreventiviDettaglio.CODICE_ARTICOLO).length() > 0) {
						codiceArticolo = " (" + ctx.getString(R.string.cod) + " "
								+ recPrevDett.getAsString(PreventiviDettaglio.CODICE_ARTICOLO) + ")";
					}
					cella.setCellValue("- " + composto.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT) + codiceArticolo);
				}

			}
			if (righeCollegamenti > 1 + composti.size()) {
				for (int r = 1 + composti.size(); r < righeCollegamenti; r++) {
					HSSFCellUtil.getRow(rowCount + r, foglioLocale).createCell(colonnaTesti).setCellStyle(stileComposti);
					HSSFCellUtil.getRow(rowCount + r, foglioLocale).createCell(colonnaTesti + 1).setCellStyle(stileComposti);
				}

				rowCount = rowCount + righeCollegamenti;
			} else {
				rowCount = rowCount + composti.size() + 1;
			}
		}

		rowCount++;

	}

	private int _creaRigheCollegamenti(DbInterno db, int idElementoCantiere, boolean componente, boolean soloTubi, HSSFSheet foglioLocale) {
		// TODO Auto-generated method stub
		int righeCollegamenti = 0;

		for (int i = 0; i < relazioni.size(); i++) {
			ContentValues relCurr = (ContentValues) relazioni.get(i);

			int idElemento_1 = 0;
			int idElemento_2 = 0;
			if (componente == false) {
				idElemento_1 = relCurr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT1);
				idElemento_2 = relCurr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT2);
			} else {
				idElemento_1 = relCurr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT1);
				idElemento_2 = relCurr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT2);
			}

			if (idElemento_1 == idElementoCantiere || idElemento_2 == idElementoCantiere) {
				_caricaInformazioniAggiuntiveCollegamento(db, relCurr);
				if (soloTubi && relCurr.getAsString("TIPO").equalsIgnoreCase(ctx.getResources().getString(R.string.cavo))) {
					continue;
				}
				HSSFRow riga = HSSFCellUtil.getRow(rowCount + righeCollegamenti, foglioLocale);

				HSSFCell cella = riga.createCell(colonnaTesti + 2);
				cella.setCellStyle(stileComposizione);
				if (idElemento_1 == idElementoCantiere) {
					cella.setCellValue(relCurr.getAsString("numero_identificativo_2") + " - " + relCurr.getAsString("nome_elemento_cant2"));
				} else {
					cella.setCellValue(relCurr.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO) + " - "
							+ relCurr.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
				}

				cella = riga.createCell(colonnaTesti + 3);
				cella.setCellStyle(stileComposizione);
				if (idElemento_1 == idElementoCantiere) {
					cella.setCellValue(relCurr.getAsString("COMPONENTE_B"));
				} else {
					cella.setCellValue(relCurr.getAsString("COMPONENTE_A"));
				}

				cella = riga.createCell(colonnaTesti + 4);
				cella.setCellStyle(stileComposizione);
				if (idElemento_1 == idElementoCantiere) {
					cella.setCellValue(relCurr.getAsString("LOCALE_B"));
				} else {
					cella.setCellValue(relCurr.getAsString("LOCALE_A"));
				}

				cella = riga.createCell(colonnaTesti + 5);
				cella.setCellStyle(stileComposizione);
				cella.setCellValue(relCurr.getAsString("TIPO") + ": " + relCurr.getAsString("NOME"));

				cella = riga.createCell(colonnaTesti + 6);
				cella.setCellStyle(stileComposizione);
				cella.setCellValue(Utility.formatNumero(relCurr.getAsDouble(Collegamenti.METRI), 2));

				righeCollegamenti++;
			}

		}
		return righeCollegamenti;
	}

	private void _caricaInformazioniAggiuntiveCollegamento(DbInterno db, ContentValues curr) {
		// TODO Auto-generated method stub
		tabColl.aggiungiTuboCavo(curr, tubi, cavi, ctx);
		tabColl.aggiungiLocali(curr, locali);

		curr.put("COMPONENTE_A", "");
		curr.put("COMPONENTE_B", "");
		int idComponente1 = curr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT1);
		ContentValues whereComp = new ContentValues();
		if (idComponente1 != 0) {
			whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponente1);
			ContentValues recComp = db.getRecord(tabComponenti, whereComp);
			if (recComp != null) {
				// prendo la composizione
				String moduliOccupati = "";
				ContentValues whereCompos = new ContentValues();
				whereCompos.put(ComposizioniCantiere.ID_ELEMENTO_CANT, curr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT1));
				whereCompos.put(ComposizioniCantiere.ID_COMPONENTE_CANT, idComponente1);

				ContentValues valCompos = db.getRecord(new ComposizioniCantiere(), whereCompos);
				if (valCompos != null) {
					moduliOccupati = valCompos.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
				}

				curr.put("COMPONENTE_A", moduliOccupati + " - " + recComp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
			}
		}

		int idComponente2 = curr.getAsInteger(Collegamenti.ID_COMPONENTE_CANT2);
		if (idComponente2 != 0) {
			whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponente2);
			ContentValues recComp = db.getRecord(tabComponenti, whereComp);
			if (recComp != null) {
				// prendo la composizione
				String moduliOccupati = "";
				ContentValues whereCompos = new ContentValues();
				whereCompos.put(ComposizioniCantiere.ID_ELEMENTO_CANT, curr.getAsInteger(Collegamenti.ID_ELEMENTO_CANT2));
				whereCompos.put(ComposizioniCantiere.ID_COMPONENTE_CANT, idComponente2);

				ContentValues valCompos = db.getRecord(new ComposizioniCantiere(), whereCompos);
				if (valCompos != null) {
					moduliOccupati = valCompos.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
				}

				curr.put("COMPONENTE_B", moduliOccupati + " - " + recComp.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
			}
		}

	}

	private void _caricaRelazioniOrdine(DbInterno db, int idOrdine) {
		// TODO Auto-generated method stub

		relazioni = new ArrayList<Object>();

		Join j0 = new Join(Collegamenti.NOME_TABELLA, ElementiCantiere.NOME_TABELLA);
		j0.addCampiDiJoin(Collegamenti.ID_ELEMENTO_CANT1, ElementiCantiere.ID_ELEMENTO_CANT);

		Join j1 = new Join(Collegamenti.NOME_TABELLA, ElementiCantiere.NOME_TABELLA);
		j1.setAliasTabella("ElementiCantiere2");
		j1.addCampiDiJoin(Collegamenti.ID_ELEMENTO_CANT2, ElementiCantiere.ID_ELEMENTO_CANT);

		// String SQL = "Select " + Collegamenti.NOME_TABELLA + ".*," + ElementiCantiere.NOME_TABELLA + "." +
		// ElementiCantiere.ID_ELEMENTO
		// + "," + ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.NOME_ELEMENTO_CANT + "," +
		// ElementiCantiere.NOME_TABELLA
		// + "." + ElementiCantiere.NUMERO_IDENTIFICATIVO + "," + ElementiCantiere.NOME_TABELLA + "." +
		// ElementiCantiere.ID_LOCALE
		// + " as ID_LOCALE_A,ElementiCantiere2." + ElementiCantiere.ID_ELEMENTO + " as idElemento_2,ElementiCantiere2."
		// + ElementiCantiere.NOME_ELEMENTO_CANT + " as nome_elemento_cant2,ElementiCantiere2."
		// + ElementiCantiere.NUMERO_IDENTIFICATIVO + " as numero_identificativo_2,ElementiCantiere2." +
		// ElementiCantiere.ID_LOCALE
		// + " as ID_LOCALE_B from " + Collegamenti.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where "
		// + Collegamenti.ID_ORDINE + " in (Select  " + Preventivi.ID_PREVENTIVO + " from " + Preventivi.NOME_TABELLA +
		// " where "
		// + Preventivi.TIPO + "='" + Preventivi.TIPO_ORDINE + "' and " + Preventivi.ID_CANTIERE + "=" + idCantiere +
		// ")";

		String SQL = "Select " + Collegamenti.NOME_TABELLA + ".*," + ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.ID_ELEMENTO
				+ "," + ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.NOME_ELEMENTO_CANT + "," + ElementiCantiere.NOME_TABELLA
				+ "." + ElementiCantiere.NUMERO_IDENTIFICATIVO + "," + ElementiCantiere.NOME_TABELLA + "." + ElementiCantiere.ID_LOCALE
				+ " as ID_LOCALE_A,ElementiCantiere2." + ElementiCantiere.ID_ELEMENTO + " as idElemento_2,ElementiCantiere2."
				+ ElementiCantiere.NOME_ELEMENTO_CANT + " as nome_elemento_cant2,ElementiCantiere2."
				+ ElementiCantiere.NUMERO_IDENTIFICATIVO + " as numero_identificativo_2,ElementiCantiere2." + ElementiCantiere.ID_LOCALE
				+ " as ID_LOCALE_B from " + Collegamenti.NOME_TABELLA + j0.getSQLJoin() + j1.getSQLJoin() + " where "
				+ Collegamenti.ID_ORDINE + "=" + idOrdine;

		relazioni.addAll(db.eseguiSelect(SQL, null));

	}

}
