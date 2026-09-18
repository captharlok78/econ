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
import java.util.Locale;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Manodopera;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.Rapportini;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.utils.Utility;
import pfa.app.general.simplecropimage.Util;

/**
 * Created by daniele on 15/02/2016.
 */
public class RapportinoXLS {
    private Context ctx = null;
    int rowCount = 0;
    private HSSFWorkbook wb = null;
    private Sheet sheet1 = null;

    public RapportinoXLS(Context ctx){
        this.ctx = ctx;
        wb = new HSSFWorkbook();
        sheet1 = wb.createSheet("Rapportino");
    }

    public String generaReportRapportino(int idRapportino,boolean multiplo) throws Exception  {
        ContentValues valTestata = null;
        String dataRapp = "";
        String numeroOrdine = "";
        DbInterno db = new DbInterno(ctx);

        ArrayList<Object> recs =  db.eseguiSelect("Select rapportini.*,Preventivi.numero,Preventivi.data,Anagrafica.ragione_sociale,Utenti.nome as nome_operatore,Utenti.cognome as cognome_operatore,ditte.ragione_sociale as rag_soc_ditta from Rapportini inner join Utenti on rapportini.id_operatore=Utenti.id_utente inner join  Preventivi on rapportini.id_ordine=Preventivi.id_preventivo inner join  cantieri on preventivi.id_cantiere=cantieri.id_cantiere inner join anagrafica on  cantieri.id_anagrafica=anagrafica.id_anagrafica inner join ditte on rapportini.id_ditta=ditte.id_ditta where id_rapportino=" + idRapportino, null);
        if (recs.size()>0){
            valTestata = (ContentValues)recs.get(0);
            dataRapp = Utility.numberToData(valTestata.getAsLong(Rapportini.DATA_RAPPORTINO));
            numeroOrdine = valTestata.getAsString(Preventivi.NUMERO);

            _aggiungiTitolo(valTestata);
            rowCount = 4;

            //prendo le righe
            _aggiungiRigaIntestazione();


            ArrayList<Object> righe = db.eseguiSelect("Select rapportini_dettaglio.*,manodopera.nome,manodopera.costo_orario from rapportini_dettaglio inner join manodopera on rapportini_dettaglio.id_manodopera=manodopera.id_manodopera where id_rapportino=" + idRapportino,null);


            ArrayList descrizioni = new ArrayList();

            for (int i=0;i<righe.size();i++){
                ContentValues riga = (ContentValues)righe.get(i);
                _aggiungiRigaTecnico(valTestata,riga);
                String descrizione = riga.getAsString(RapportiniDettaglio.NOTE);
                if (!descrizioni.contains(descrizione)){
                    descrizioni.add(descrizione);
                }

            }

            if (multiplo){
                //prendo gli altri rapportini nella stessa data /ordine
                long dataRapportino = valTestata.getAsLong(Rapportini.DATA_RAPPORTINO);
                int idOrdine = valTestata.getAsInteger(Rapportini.ID_ORDINE);
                ArrayList<Object> righeAltriRapportini = db.eseguiSelect("Select Utenti.nome as nome_operatore,Utenti.cognome as cognome_operatore, rapportini_dettaglio.*,manodopera.nome,manodopera.costo_orario from rapportini_dettaglio inner join rapportini on rapportini_dettaglio.id_rapportino=rapportini.id_rapportino inner join Utenti on rapportini.id_operatore=Utenti.id_utente  inner join manodopera on rapportini_dettaglio.id_manodopera=manodopera.id_manodopera where rapportini_dettaglio.id_rapportino in (select id_rapportino from rapportini where data_rapportino="+dataRapportino+" and id_ordine="+idOrdine+" and id_rapportino<>"+idRapportino+")",null);
                for (int i=0;i<righeAltriRapportini.size();i++){
                    ContentValues riga = (ContentValues)righeAltriRapportini.get(i);
                    ContentValues valUtente = new ContentValues();
                    valUtente.put("nome_operatore",riga.getAsString("nome_operatore"));
                    valUtente.put("cognome_operatore",riga.getAsString("cognome_operatore"));
                    _aggiungiRigaTecnico(valUtente,riga);
                    String descrizione = riga.getAsString(RapportiniDettaglio.NOTE);
                    if (!descrizioni.contains(descrizione)){
                        descrizioni.add(descrizione);
                    }
                }
            }


            _aggiungiRigaIntestazioneDescrizioni();

            for (int i=0;i<descrizioni.size();i++){
                _aggiungiRigaDescrizione((String)descrizioni.get(i));

            }



            for (int i=0;i<8;i++){
                Cell c = sheet1.getRow(rowCount-1).getCell(i);
                if (c==null){
                    c = sheet1.getRow(rowCount-1).createCell(i);
                }
                //SOTTO
                CellStyle cs = wb.createCellStyle();
                cs.cloneStyleFrom(c.getCellStyle());

                cs.setBorderBottom(CellStyle.BORDER_THIN);
                c.setCellStyle(cs);

                //SOPRA
                Cell ct = sheet1.getRow(0).getCell(i);
                if (ct==null){
                    ct = sheet1.getRow(0).createCell(i);
                }
                CellStyle cst = wb.createCellStyle();
                cst.cloneStyleFrom(ct.getCellStyle());

                cst.setBorderTop(CellStyle.BORDER_THIN);
                ct.setCellStyle(cst);
            }

            for (int i=0;i<rowCount;i++){
                Row r = sheet1.getRow(i);
                if (r==null){
                    r = sheet1.createRow(i);
                }
                Cell c = r.getCell(7);
                if (c==null){
                    c = r.createCell(7);
                }
                //DESTRA
                CellStyle cs = wb.createCellStyle();
                cs.cloneStyleFrom(c.getCellStyle());

                cs.setBorderRight(CellStyle.BORDER_THIN);
                c.setCellStyle(cs);

                //SINISTRA
                Cell cl = r.getCell(0);
                if (cl==null){
                    cl = r.createCell(0);
                }
                CellStyle csl = wb.createCellStyle();
                csl.cloneStyleFrom(cl.getCellStyle());

                csl.setBorderLeft(CellStyle.BORDER_THIN);
                cl.setCellStyle(csl);

            }
        }

        db.close();

        String ragioneSociale = "";
        if (valTestata!=null){
            ragioneSociale = valTestata.getAsString(Anagrafica.RAGIONE_SOCIALE);
        }

        ragioneSociale = Utility.formattaStringaPerNomeFile(ragioneSociale);
        String cartellaRapp = Rapportini.PATH_EXPORT_RAPPORTINI;


        File dirExport = new File(Environment.getExternalStorageDirectory() + File.separator + "ECONTAB" + File.separator+ragioneSociale+File.separator
                + cartellaRapp );

        if (!dirExport.exists()) {
            dirExport.mkdirs();
        }

        String nomeRapp = "RAPP_" + dataRapp.replaceAll("/","") + "_ORD_" + numeroOrdine;


        File fileExport = new File(dirExport, nomeRapp + ".xls");
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

    private void _aggiungiRigaDescrizione(String descrizione) {
        Row riga = sheet1.createRow(rowCount);

        CellStyle cs = wb.createCellStyle();
        cs.setWrapText(true);



        Cell c = riga.createCell(0);
        c.setCellValue(descrizione);
        c.setCellStyle(cs);

        int numeroRighe = 1+descrizione.length()/85;
        riga.setHeight((short)(300*numeroRighe));



        sheet1.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 0, 7));
        rowCount++;
    }

    private void _aggiungiRigaIntestazioneDescrizioni() {

        // Cell style for header row
        CellStyle cs = wb.createCellStyle();
        Font fBold = wb.createFont();
        fBold.setBoldweight(Font.BOLDWEIGHT_BOLD);
        wb.getCustomPalette().setColorAtIndex(HSSFColor.GREY_25_PERCENT.index, (byte) 236, (byte) 236, (byte) 236);
        cs.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
        cs.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        cs.setFont(fBold);
        cs.setAlignment(CellStyle.ALIGN_CENTER);

        Row row = sheet1.createRow(rowCount);

        Cell c = row.createCell(0);
        c.setCellValue(ctx.getResources().getString(R.string.descrizione_lavorazione).toUpperCase(Locale.getDefault()));
        c.setCellStyle(cs);

        sheet1.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 0, 7));
        rowCount++;
    }

    private void _aggiungiRigaTecnico(ContentValues val, ContentValues val_riga) {
        Row riga = sheet1.createRow(rowCount);

        CellStyle cs = wb.createCellStyle();
        cs.setWrapText(true);

        CellStyle currencyCellStyle = wb.createCellStyle();
        currencyCellStyle.setWrapText(true);

        currencyCellStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

        Cell c = riga.createCell(0);
        c.setCellValue(val.getAsString("nome_operatore") + " "+val.getAsString("cognome_operatore"));
        c.setCellStyle(cs);

        sheet1.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 0, 2));

        c = riga.createCell(3);
        c.setCellValue(val_riga.getAsString(Manodopera.NOME));// CODICE ARTICOLO
        c.setCellStyle(cs);

        sheet1.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 3, 6));


        c = riga.createCell(7);
        c.setCellValue(val_riga.getAsDouble(RapportiniDettaglio.ORE));
        c.setCellType(Cell.CELL_TYPE_NUMERIC);
        c.setCellStyle(cs);

       /* c = riga.createCell(3);
        c.setCellValue(Utility.arrotonda(val.getAsDouble(Manodopera.COSTO_ORARIO), 2));
        c.setCellType(Cell.CELL_TYPE_NUMERIC);
        c.setCellStyle(currencyCellStyle);

        c = riga.createCell(4);
        double tot = val.getAsDouble(Manodopera.COSTO_ORARIO)  * val.getAsDouble(RapportiniDettaglio.ORE);
        String cellaQta = "C" + (rowCount + 1);
        String cellaPrezzo = "D" + (rowCount + 1);
        c.setCellFormula("PRODUCT(" + cellaQta + ":" + cellaPrezzo + ")");
        c.setCellValue(Utility.arrotonda(tot, 2));
        c.setCellType(Cell.CELL_TYPE_FORMULA);
        c.setCellStyle(currencyCellStyle);*/



        rowCount++;
    }

    private void _aggiungiTitolo(ContentValues val) {
        // TODO Auto-generated method stub


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
        sheet1.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        Cell c = row.createCell(0);

       // c.setCellValue(ctx.getResources().getString(R.string.rapportino_del) + " " + Utility.numberToData(val.getAsLong(Rapportini.DATA_RAPPORTINO)) + " - " + val.getAsString("nome_operatore")+" "+val.getAsString("cognome_operatore"));
        c.setCellValue(ctx.getResources().getString(R.string.rapportino_del) + " " + Utility.numberToData(val.getAsLong(Rapportini.DATA_RAPPORTINO)) + " - " + val.getAsString("rag_soc_ditta"));


        c.setCellStyle(csTitolo);

        row = sheet1.createRow(1);
        //sheet1.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));

        String testoClienteCantiere = ctx.getResources().getString(R.string.cliente).toUpperCase(Locale.getDefault());

        testoClienteCantiere = testoClienteCantiere + ": " + val.getAsString(Anagrafica.RAGIONE_SOCIALE) + "\n" + ctx.getResources().getString(R.string.ordine_num_del,val.getAsString(Preventivi.NUMERO), Utility.numberToData(val.getAsLong(Preventivi.DATA))).toUpperCase(Locale.getDefault());
        c = row.createCell(0);
        c.setCellValue(testoClienteCantiere);
        c.setCellStyle(csBluBold);



        //row = sheet1.createRow(2);
        sheet1.addMergedRegion(new CellRangeAddress(1, 3, 0, 7));
        //testoClienteCantiere = ctx.getResources().getString(R.string.ordine_num_del,val.getAsString(Preventivi.NUMERO), Utility.numberToData(val.getAsLong(Preventivi.DATA))).toUpperCase(Locale.getDefault());

       // c = row.createCell(0);
       // c.setCellValue(testoClienteCantiere);
       // c.setCellStyle(csBluBold);





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
        c.setCellValue(ctx.getResources().getString(R.string.tecnico).toUpperCase(Locale.getDefault()));
        c.setCellStyle(cs);

        sheet1.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 0, 2));
     /*   c = row.createCell(1);
        c.setCellValue(ctx.getResources().getString(R.string.descrizione).toUpperCase(Locale.getDefault()));
        c.setCellStyle(cs);*/

        c = row.createCell(3);
        c.setCellValue(ctx.getResources().getString(R.string.tipo).toUpperCase(Locale.getDefault()));
        c.setCellStyle(cs);
        sheet1.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 3, 6));

        c = row.createCell(7);
        c.setCellValue(ctx.getResources().getString(R.string.ore).toUpperCase(Locale.getDefault()));
        c.setCellStyle(cs);

       // c = row.createCell(3);
       // c.setCellValue(ctx.getResources().getString(R.string.costo_orario).toUpperCase(Locale.getDefault()));
       // c.setCellStyle(cs);

       // c = row.createCell(4);
      //  c.setCellValue(ctx.getResources().getString(R.string.totale).toUpperCase(Locale.getDefault()));
      //  c.setCellStyle(cs);


        //sheet1.setColumnWidth(0, (15 * 200));
        //sheet1.setColumnWidth(1, (15 * 500));
        //sheet1.setColumnWidth(2, (15 * 200));
        //sheet1.setColumnWidth(3, (15 * 200));
        //sheet1.setColumnWidth(4, (15 * 200));


        rowCount++;
    }


    private void _aggiungiRiga(ContentValues val) {
        // TODO Auto-generated method stub



        Row riga = sheet1.createRow(rowCount);

        CellStyle cs = wb.createCellStyle();
        cs.setWrapText(true);

        CellStyle currencyCellStyle = wb.createCellStyle();
        currencyCellStyle.setWrapText(true);

        currencyCellStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

        Cell c = riga.createCell(0);
        c.setCellValue(val.getAsString(Manodopera.NOME));// CODICE ARTICOLO
        c.setCellStyle(cs);

        c = riga.createCell(1);
        c.setCellValue(val.getAsString(RapportiniDettaglio.NOTE));
        c.setCellStyle(cs);


        c = riga.createCell(2);
        c.setCellValue(val.getAsDouble(RapportiniDettaglio.ORE));
        c.setCellType(Cell.CELL_TYPE_NUMERIC);
        c.setCellStyle(cs);

        c = riga.createCell(3);
        c.setCellValue(Utility.arrotonda(val.getAsDouble(Manodopera.COSTO_ORARIO), 2));
        c.setCellType(Cell.CELL_TYPE_NUMERIC);
        c.setCellStyle(currencyCellStyle);

        c = riga.createCell(4);
        double tot = val.getAsDouble(Manodopera.COSTO_ORARIO)  * val.getAsDouble(RapportiniDettaglio.ORE);
        String cellaQta = "C" + (rowCount + 1);
        String cellaPrezzo = "D" + (rowCount + 1);
        c.setCellFormula("PRODUCT(" + cellaQta + ":" + cellaPrezzo + ")");
        c.setCellValue(Utility.arrotonda(tot, 2));
        c.setCellType(Cell.CELL_TYPE_FORMULA);
        c.setCellStyle(currencyCellStyle);



        rowCount++;
    }
}
