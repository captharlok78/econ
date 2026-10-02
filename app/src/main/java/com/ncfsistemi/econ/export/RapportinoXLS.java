package com.ncfsistemi.econ.export;

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

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.Costruttori;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Preventivi;
import com.ncfsistemi.econ.db.table.PreventiviDettaglio;
import com.ncfsistemi.econ.db.table.Rapportini;
import com.ncfsistemi.econ.db.table.RapportiniDettaglio;
import com.ncfsistemi.econ.utils.RigheRapportino;
import com.ncfsistemi.econ.utils.RegoleRapportino;
import com.ncfsistemi.econ.utils.Utility;
import com.ncfsistemi.general.simplecropimage.Util;

public class RapportinoXLS {
    private Context ctx = null;
    int rowCount = 0;
    private HSSFWorkbook wb = null;
    private Sheet sheet1 = null;
    /** Costi (costo orario e totale per operatore) solo per l'Amministratore ditta. */
    private boolean amministratore = false;
    private double totaleOre = 0;
    private double totaleCosto = 0;

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

        // cliente, cantiere e ordine facoltativi (almeno uno); il cliente e' quello del rapportino o del cantiere
        ArrayList<Object> recs =  db.eseguiSelect("Select rapportini.*,Preventivi.numero as numero_ordine,Preventivi.data as data_ordine,Anagrafica.ragione_sociale,cantieri.nome as nome_cantiere," // alias: "numero" e' quello del rapportino
                + "Utenti.nome as nome_operatore,Utenti.cognome as cognome_operatore,ditte.ragione_sociale as rag_soc_ditta"
                + " from Rapportini left join Utenti on rapportini.id_utente_ditta=Utenti.id_utente_ditta left join Preventivi on rapportini.id_ordine=Preventivi.id_preventivo"
                + " left join cantieri on cantieri.id_cantiere=rapportini.id_cantiere"
                + " left join anagrafica on anagrafica.id_anagrafica=(case when rapportini.id_cliente>0 then rapportini.id_cliente else cantieri.id_anagrafica end)"
                + " left join ditte on rapportini.id_ditta=ditte.id_ditta where rapportini.id=" + idRapportino, null);
        if (recs.size()>0){
            valTestata = (ContentValues)recs.get(0);
            amministratore = RegoleRapportino.vedePrezzi(db, RegoleRapportino.utenteCorrente(db, ctx)); // chi vede i prezzi vede i costi
            dataRapp = Utility.numberToData(valTestata.getAsLong(Rapportini.DATA_RAPPORTINO));
            numeroOrdine = valTestata.getAsString("numero_ordine");

            _aggiungiTitolo(valTestata);
            rowCount = 4;

            //prendo le righe
            _aggiungiRigaIntestazione();

            ArrayList descrizioni = new ArrayList();
            _aggiungiRigheRapportino(db, idRapportino, valTestata, descrizioni);

            if (multiplo){
                //prendo gli altri rapportini nella stessa data e sullo stesso cantiere (senza cantiere: stesso cliente)
                Integer idCantiere = valTestata.getAsInteger(Rapportini.ID_CANTIERE);
                Integer idCliente = valTestata.getAsInteger(Rapportini.ID_CLIENTE);
                String stesso = idCantiere != null && idCantiere > 0 ? "rapportini.id_cantiere=" + idCantiere
                        : "coalesce(rapportini.id_cantiere,0)=0 and rapportini.id_cliente=" + (idCliente != null ? idCliente : 0);
                ArrayList<Object> altriRapportini = db.eseguiSelect("Select rapportini.id,Utenti.nome as nome_operatore,Utenti.cognome as cognome_operatore"
                        + " from rapportini left join Utenti on rapportini.id_utente_ditta=Utenti.id_utente_ditta"
                        + " where rapportini.data_rapportino=" + valTestata.getAsLong(Rapportini.DATA_RAPPORTINO)
                        + " and " + stesso
                        + " and rapportini.id_ditta=" + valTestata.getAsInteger(Rapportini.ID_DITTA)
                        + " and rapportini.id<>" + idRapportino, null);
                for (int i=0;i<altriRapportini.size();i++){
                    ContentValues altro = (ContentValues)altriRapportini.get(i);
                    _aggiungiRigheRapportino(db, altro.getAsInteger(Rapportini.ID), altro, descrizioni);
                }
            }

            _aggiungiRigaTotali();

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


        File dirExport = new File(com.ncfsistemi.econ.utils.Utility.cartellaApp() + File.separator+ragioneSociale+File.separator
                + cartellaRapp );

        if (!dirExport.exists()) {
            dirExport.mkdirs();
        }

        String nomeRapp = "RAPP_" + dataRapp.replaceAll("/","") + (numeroOrdine != null
                ? "_ORD_" + numeroOrdine
                : "_" + Utility.formattaStringaPerNomeFile(valTestata != null && valTestata.getAsString("nome_cantiere") != null ? valTestata.getAsString("nome_cantiere") : ""));


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

    /**
     * Una riga del foglio per ogni riga del rapportino (nell'ordine della scheda): per lavoro e viaggio l'utente, il tipo con
     * gli orari e le ore-uomo; per il materiale l'articolo, il tipo con la quantita' e il costo. Costi calcolati al momento
     * (costo orario dell'utente, prezzo di acquisto del listino). Le note delle righe vanno nell'elenco descrizioni.
     */
    private void _aggiungiRigheRapportino(DbInterno db, int idRapportino, ContentValues valAutore, ArrayList descrizioni) {
        for (ContentValues riga : RigheRapportino.righe(db, idRapportino)) {
            String tipo = RigheRapportino.testo(riga, RigheRapportino.DESCRIZIONE_TIPO);
            if (RigheRapportino.isMateriale(riga)) {
                _aggiungiRigaMateriale(RigheRapportino.articolo(riga), tipo + " · " + Utility.formatNumero(RigheRapportino.decimale(riga,
                        RapportiniDettaglio.QUANTITA)) + " " + RigheRapportino.testo(riga, RigheRapportino.CODICE_UNITA),
                        RigheRapportino.costoUnitario(riga), RigheRapportino.costo(riga));
            } else {
                String orari = RigheRapportino.orari(riga);
                _aggiungiRigaTecnico(RigheRapportino.testo(riga, RigheRapportino.NOME_UTENTE), tipo + (orari.isEmpty() ? "" : " " + orari),
                        RigheRapportino.oreUomo(riga), RigheRapportino.costoUnitario(riga));
            }
            String nota = riga.getAsString(RapportiniDettaglio.NOTE);
            if (nota != null && !nota.trim().isEmpty() && !descrizioni.contains(nota)){
                descrizioni.add(nota);
            }
        }
    }

    /** Riga di materiale: articolo 0-2, tipo e quantita' 3-7 (3-5 per chi vede i prezzi), prezzo di acquisto 6 e costo 7. */
    private void _aggiungiRigaMateriale(String articolo, String descrizione, double prezzo, double costo) {
        Row riga = sheet1.createRow(rowCount);

        CellStyle cs = wb.createCellStyle();
        cs.setWrapText(true);

        Cell c = riga.createCell(0);
        c.setCellValue(articolo);
        c.setCellStyle(cs);
        sheet1.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 0, 2));

        c = riga.createCell(3);
        c.setCellValue(descrizione);
        c.setCellStyle(cs);
        sheet1.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 3, amministratore ? 5 : 7));

        if (amministratore) {
            CellStyle currencyCellStyle = wb.createCellStyle();
            currencyCellStyle.setWrapText(true);
            currencyCellStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            c = riga.createCell(6);
            c.setCellValue(Utility.arrotonda(prezzo, 2));
            c.setCellType(Cell.CELL_TYPE_NUMERIC);
            c.setCellStyle(currencyCellStyle);

            c = riga.createCell(7);
            c.setCellValue(Utility.arrotonda(costo, 2));
            c.setCellType(Cell.CELL_TYPE_NUMERIC);
            c.setCellStyle(currencyCellStyle);
            totaleCosto += costo;
        }

        rowCount++;
    }

    /** Colonne: tecnico 0-2, tipo 3-6 (3-4 per l'amministratore), ore 7 (5), costo orario 6 e totale 7 solo per l'amministratore. */
    private void _aggiungiRigaTecnico(String operatore, String manodopera, double ore, double costoOrario) {
        Row riga = sheet1.createRow(rowCount);

        CellStyle cs = wb.createCellStyle();
        cs.setWrapText(true);

        Cell c = riga.createCell(0);
        c.setCellValue(operatore);
        c.setCellStyle(cs);
        sheet1.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 0, 2));

        c = riga.createCell(3);
        c.setCellValue(manodopera);
        c.setCellStyle(cs);
        sheet1.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 3, amministratore ? 4 : 6));

        c = riga.createCell(amministratore ? 5 : 7);
        c.setCellValue(ore);
        c.setCellType(Cell.CELL_TYPE_NUMERIC);
        c.setCellStyle(stileOre(cs));
        totaleOre += ore;

        if (amministratore) {
            CellStyle currencyCellStyle = wb.createCellStyle();
            currencyCellStyle.setWrapText(true);
            currencyCellStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            c = riga.createCell(6);
            c.setCellValue(Utility.arrotonda(costoOrario, 2));
            c.setCellType(Cell.CELL_TYPE_NUMERIC);
            c.setCellStyle(currencyCellStyle);

            c = riga.createCell(7);
            c.setCellValue(Utility.arrotonda(ore * costoOrario, 2));
            c.setCellType(Cell.CELL_TYPE_NUMERIC);
            c.setCellStyle(currencyCellStyle);
            totaleCosto += ore * costoOrario;
        }

        rowCount++;
    }

    /** Ore con due decimali e separatori della lingua del foglio (0,50 e non 0.5), sullo stile della riga. */
    private CellStyle stileOre(CellStyle base) {
        CellStyle stile = wb.createCellStyle();
        stile.cloneStyleFrom(base);
        stile.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return stile;
    }

    /** Totale ore-uomo (e costo, per l'amministratore) sotto le righe dei tecnici. */
    private void _aggiungiRigaTotali() {
        Row riga = sheet1.createRow(rowCount);

        CellStyle cs = wb.createCellStyle();
        Font fBold = wb.createFont();
        fBold.setBoldweight(Font.BOLDWEIGHT_BOLD);
        cs.setFont(fBold);

        Cell c = riga.createCell(0);
        c.setCellValue(ctx.getResources().getString(R.string.totale).toUpperCase(Locale.getDefault()));
        c.setCellStyle(cs);
        sheet1.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 0, amministratore ? 4 : 6));

        c = riga.createCell(amministratore ? 5 : 7);
        c.setCellValue(Utility.arrotonda(totaleOre, 2));
        c.setCellType(Cell.CELL_TYPE_NUMERIC);
        c.setCellStyle(stileOre(cs));

        if (amministratore) {
            CellStyle csCosto = wb.createCellStyle();
            csCosto.cloneStyleFrom(cs);
            csCosto.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            c = riga.createCell(7);
            c.setCellValue(Utility.arrotonda(totaleCosto, 2));
            c.setCellType(Cell.CELL_TYPE_NUMERIC);
            c.setCellStyle(csCosto);
        }

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
        // numero dato alla conferma (NUMERAZIONE_DOCUMENTI.md); la ditta solo se presente sul dispositivo
        String numero = Rapportini.numeroDocumento(val);
        String ditta = val.getAsString("rag_soc_ditta");
        c.setCellValue(ctx.getResources().getString(R.string.rapportino_del) + " " + Utility.numberToData(val.getAsLong(Rapportini.DATA_RAPPORTINO))
                + (numero.isEmpty() ? "" : " - N. " + numero)
                + (ditta == null || ditta.isEmpty() ? "" : " - " + ditta));


        c.setCellStyle(csTitolo);

        row = sheet1.createRow(1);
        //sheet1.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));

        String testoClienteCantiere = ctx.getResources().getString(R.string.cliente).toUpperCase(Locale.getDefault());

        String cliente = val.getAsString(Anagrafica.RAGIONE_SOCIALE) != null ? val.getAsString(Anagrafica.RAGIONE_SOCIALE) : "";
        String cantiere = val.getAsString("nome_cantiere") != null ? val.getAsString("nome_cantiere") : "";
        testoClienteCantiere = testoClienteCantiere + ": " + cliente + "\n"
                + ctx.getResources().getString(R.string.cantiere).toUpperCase(Locale.getDefault()) + ": " + cantiere;
        if (val.getAsString("numero_ordine") != null && val.getAsLong("data_ordine") != null) {
            testoClienteCantiere = testoClienteCantiere + "\n" + ctx.getResources().getString(R.string.ordine_num_del,val.getAsString("numero_ordine"), Utility.numberToData(val.getAsLong("data_ordine"))).toUpperCase(Locale.getDefault());
        }
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
        sheet1.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 3, amministratore ? 4 : 6));

        c = row.createCell(amministratore ? 5 : 7);
        c.setCellValue(ctx.getResources().getString(R.string.ore).toUpperCase(Locale.getDefault()));
        c.setCellStyle(cs);

        if (amministratore) {
            c = row.createCell(6);
            c.setCellValue(ctx.getResources().getString(R.string.costo_orario).toUpperCase(Locale.getDefault()));
            c.setCellStyle(cs);

            c = row.createCell(7);
            c.setCellValue(ctx.getResources().getString(R.string.totale).toUpperCase(Locale.getDefault()));
            c.setCellStyle(cs);
        }

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
}
