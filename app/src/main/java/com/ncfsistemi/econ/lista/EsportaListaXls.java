package com.ncfsistemi.econ.lista;

import android.content.ContentValues;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.ncfsistemi.econ.utils.Utility;

/**
 * Esporta in xls i risultati di una lista standard (pulsante nella barra in fondo): una colonna per ogni colonna della
 * tabella, con la stessa testata e gli stessi testi a video; le colonne numeriche (ore) restano numeri con due decimali.
 */
public final class EsportaListaXls {

    public static final String CARTELLA = "esportazioni";

    private EsportaListaXls() {
    }

    /** Scrive il file e lo restituisce (cartella dell'app/esportazioni, nome = titolo + data e ora). */
    public static File esporta(String titolo, List<ColonnaLista> colonne, List<Object> righe) throws Exception {
        HSSFWorkbook wb = new HSSFWorkbook();
        Sheet foglio = wb.createSheet(titolo);

        CellStyle stileTestata = wb.createCellStyle();
        Font grassetto = wb.createFont();
        grassetto.setBoldweight(Font.BOLDWEIGHT_BOLD);
        stileTestata.setFont(grassetto);
        CellStyle stileNumero = wb.createCellStyle();
        stileNumero.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

        // larghezze dal testo piu' lungo: autoSizeColumn usa java.awt, che su Android non c'e'
        int[] caratteri = new int[colonne.size()];
        Row testata = foglio.createRow(0);
        for (int c = 0; c < colonne.size(); c++) {
            caratteri[c] = colonne.get(c).label.length();
            Cell cella = testata.createCell(c);
            cella.setCellValue(colonne.get(c).label);
            cella.setCellStyle(stileTestata);
        }
        for (int r = 0; r < righe.size(); r++) {
            ContentValues riga = (ContentValues) righe.get(r);
            Row row = foglio.createRow(r + 1);
            for (int c = 0; c < colonne.size(); c++) {
                ColonnaLista colonna = colonne.get(c);
                Cell cella = row.createCell(c);
                if (colonna.numerica) {
                    cella.setCellValue(colonna.getNumero(riga));
                    cella.setCellStyle(stileNumero);
                } else {
                    cella.setCellValue(colonna.getTesto(riga));
                }
                caratteri[c] = Math.max(caratteri[c], colonna.getTesto(riga).length());
            }
        }
        for (int c = 0; c < colonne.size(); c++) {
            foglio.setColumnWidth(c, (Math.min(caratteri[c], 60) + 2) * 256);
        }

        File cartella = new File(Utility.cartellaApp(), CARTELLA);
        if (!cartella.exists()) {
            cartella.mkdirs();
        }
        String data = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.ITALY).format(new Date());
        File file = new File(cartella, Utility.formattaStringaPerNomeFile(titolo) + "_" + data + ".xls");
        try (FileOutputStream os = new FileOutputStream(file)) {
            wb.write(os);
        }
        return file;
    }
}
