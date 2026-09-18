package pfa.app.econtab.export;

import android.content.ContentValues;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFCellUtil;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Listini;
import pfa.app.econtab.db.table.Manodopera;
import pfa.app.econtab.db.table.PlaccheModuli;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.Rapportini;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.db.table.Utenti;
import pfa.app.econtab.utils.Utility;
import pfa.app.general.simplecropimage.Util;

/**
 * Created by daniele on 18/02/2015.
 */
public class ConsuntivoXLS {

    private Context ctx = null;

    private HSSFWorkbook wb = null;
    private HSSFSheet sheet1 = null;
    private HSSFSheet sheet2 = null;
    private CreationHelper helper = null;

    private  Font fTitolo = null;
    private  Font fBluBold = null;
    private Font fBlu = null;
    private Font fPreventivo = null;
    private Font fVerde = null;
    private Font fRosso = null;
    private Font fCodici = null;


    private CellStyle cs = null;
    private CellStyle csTitolo = null;
    private CellStyle csBlu = null;
    private CellStyle csBluBold = null;
    private CellStyle csIntestazione = null;
    private CellStyle currencyCellStyle = null;
    private CellStyle csPrev = null;
    private CellStyle csCodici = null;
    private CellStyle csVerde = null;
    private CellStyle csRosso = null;


    double qtaTotOrd = 0;
    double qtaTotPrev = 0;

    double impTotOrd = 0;
    double impTotPrev = 0;


    double oreTotRApportini = 0;
    double importoTotRapportini = 0;

    private int rowCount = 0;
    private int rowCountRapportini = 0;

    public ConsuntivoXLS(Context ctx) {
        this.ctx = ctx;
        wb = new HSSFWorkbook();
        sheet1 = wb.createSheet("Consuntivo");
        sheet2 = wb.createSheet("Rapportini");
        _preparaStili();
    }

    private void _preparaStili() {
        fTitolo = wb.createFont();
        fTitolo.setFontHeightInPoints((short) 13);
        fTitolo.setBoldweight(Font.BOLDWEIGHT_BOLD);

        fBluBold = wb.createFont();
        fBluBold.setColor(HSSFColor.DARK_BLUE.index);
        fBluBold.setBoldweight(Font.BOLDWEIGHT_BOLD);

        fBlu = wb.createFont();
        fBlu.setColor(HSSFColor.DARK_BLUE.index);

        fVerde = wb.createFont();
        fVerde.setColor(HSSFColor.GREEN.index);

        fRosso = wb.createFont();
        fRosso.setColor(HSSFColor.RED.index);

        fPreventivo = wb.createFont();
        fPreventivo.setColor(HSSFColor.GREY_50_PERCENT.index);

        fCodici = wb.createFont();
        fCodici.setColor(HSSFColor.GREY_40_PERCENT.index);
        fCodici.setItalic(true);

        cs = wb.createCellStyle();
        cs.setWrapText(true);

        csCodici = wb.createCellStyle();
        csCodici.setFont(fCodici);
        csCodici.setWrapText(true);


        csPrev = wb.createCellStyle();
        csPrev.setFont(fPreventivo);
        csPrev.setWrapText(true);


        csVerde= wb.createCellStyle();
       // csVerde.setFont(fVerde);
        csVerde.setFillForegroundColor(HSSFColor.LIGHT_GREEN.index);
        csVerde.setFillPattern(CellStyle.SOLID_FOREGROUND);
        csVerde.setBottomBorderColor(HSSFColor.GREY_50_PERCENT.index);
        csVerde.setTopBorderColor(HSSFColor.GREY_50_PERCENT.index);
        csVerde.setLeftBorderColor(HSSFColor.GREY_50_PERCENT.index);
        csVerde.setRightBorderColor(HSSFColor.GREY_50_PERCENT.index);
        csVerde.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        csVerde.setBorderTop(HSSFCellStyle.BORDER_THIN);
        csVerde.setBorderRight(HSSFCellStyle.BORDER_THIN);
        csVerde.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        csVerde.setWrapText(true);

        csRosso= wb.createCellStyle();
        //csRosso.setFont(fRosso);
        csRosso.setFillForegroundColor(HSSFColor.LIGHT_ORANGE.index);
        csRosso.setFillPattern(CellStyle.SOLID_FOREGROUND);
        csRosso.setBottomBorderColor(HSSFColor.GREY_50_PERCENT.index);
        csRosso.setTopBorderColor(HSSFColor.GREY_50_PERCENT.index);
        csRosso.setLeftBorderColor(HSSFColor.GREY_50_PERCENT.index);
        csRosso.setRightBorderColor(HSSFColor.GREY_50_PERCENT.index);
        csRosso.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        csRosso.setBorderTop(HSSFCellStyle.BORDER_THIN);
        csRosso.setBorderRight(HSSFCellStyle.BORDER_THIN);
        csRosso.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        csRosso.setWrapText(true);

        csTitolo = wb.createCellStyle();
        csTitolo.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
        csTitolo.setFillPattern(CellStyle.SOLID_FOREGROUND);
        csTitolo.setFont(fTitolo);

        csBlu = wb.createCellStyle();
        csBlu.setFont(fBlu);
        csBlu.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        csBlu.setWrapText(true);
        csBlu.setFillForegroundColor(HSSFColor.WHITE.index);
        csBlu.setFillPattern(CellStyle.SOLID_FOREGROUND);

        csBluBold = wb.createCellStyle();
        csBluBold.setFont(fBluBold);
        csBluBold.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        csBluBold.setWrapText(true);
        csBluBold.setFillForegroundColor(HSSFColor.WHITE.index);
        csBluBold.setFillPattern(CellStyle.SOLID_FOREGROUND);

        currencyCellStyle = wb.createCellStyle();
        currencyCellStyle.setWrapText(true);

        currencyCellStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

        csIntestazione = wb.createCellStyle();
        Font fBold = wb.createFont();
        fBold.setBoldweight(Font.BOLDWEIGHT_BOLD);
        wb.getCustomPalette().setColorAtIndex(HSSFColor.GREY_25_PERCENT.index, (byte) 236, (byte) 236, (byte) 236);
        csIntestazione.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
        csIntestazione.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        csIntestazione.setFont(fBold);
    }

    public String generaReport(ArrayList<Integer> listaOrdini) throws Exception {
        DbInterno db = new DbInterno(ctx);


        int idOrdine = 0;
        ContentValues valClie = null;
        if (listaOrdini.size()>0){
            idOrdine = listaOrdini.get(0);
            Preventivi tabPreventivi = new Preventivi();
            PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();
            valClie =  _aggiungiTitolo(db,idOrdine);

            rowCount = 5;
            _aggiungiRigaIntestazione();
            for (int i=0;i<listaOrdini.size();i++){
                idOrdine = listaOrdini.get(i);
                ContentValues valOrd = tabPreventivi.getClienteCantierePreventivo(db,idOrdine);
                if (i>0){
                    rowCount++;//salto una riga tra un ordine e l'altro
                }
                _aggiungiTitoloOrdine(valOrd);
                ArrayList<Object> materialiLocali = tabPrevDett.getRighePreventivo(db, idOrdine, PreventiviDettaglio.MATERIALE);
                ArrayList<Object> placcheLocali = tabPrevDett.getRighePreventivo(db, idOrdine, PreventiviDettaglio.PLACCHE);
                ArrayList<Object> materialiPreventivo = tabPrevDett.getRighePreventivo(db, idOrdine, PreventiviDettaglio.MATERIALE_PREVENTIVO);
                ArrayList<Object> placchePreventivo = tabPrevDett.getRighePreventivo(db, idOrdine, PreventiviDettaglio.PLACCHE_PREVENTIVO);
                ArrayList<Object> manodopera = tabPrevDett.getRighePreventivo(db, idOrdine, PreventiviDettaglio.MANOPERA);
                ArrayList<Object> altro = tabPrevDett.getRighePreventivo(db, idOrdine, PreventiviDettaglio.ALTRO);
                ArrayList<Object> collegamenti = tabPrevDett.getRighePreventivo(db, idOrdine, PreventiviDettaglio.COLLEGAMENTI);


                ArrayList<Object> materialeOrdine = new ArrayList<Object>();
                materialeOrdine.addAll(materialiLocali);
                materialeOrdine.addAll(materialiPreventivo);
                materialeOrdine.addAll(collegamenti);
                materialeOrdine.addAll(placcheLocali);
                materialeOrdine.addAll(placchePreventivo);

                int idPreventivoOrigine = valOrd.getAsInteger(Preventivi.ID_PREVENTIVO_ORIGINE);
                ArrayList<Object> materialiLocaliPrev = null;
                ArrayList<Object> placcheLocaliPrev = null;
                ArrayList<Object> materialiPreventivoPrev = null;
                ArrayList<Object> placchePreventivoPrev = null;
                ArrayList<Object> manodoperaPrev = null;
                ArrayList<Object> altroPrev = null;
                ArrayList<Object> collegamentiPrev = null;



                ArrayList<Object> materialePreventivo = new ArrayList<Object>();
                if (idPreventivoOrigine!=0){
                    materialiLocaliPrev = tabPrevDett.getRighePreventivo(db, idPreventivoOrigine, PreventiviDettaglio.MATERIALE);
                    placcheLocaliPrev = tabPrevDett.getRighePreventivo(db, idPreventivoOrigine, PreventiviDettaglio.PLACCHE);
                    materialiPreventivoPrev = tabPrevDett.getRighePreventivo(db, idPreventivoOrigine, PreventiviDettaglio.MATERIALE_PREVENTIVO);
                    placchePreventivoPrev = tabPrevDett.getRighePreventivo(db, idPreventivoOrigine, PreventiviDettaglio.PLACCHE_PREVENTIVO);
                    manodoperaPrev = tabPrevDett.getRighePreventivo(db, idPreventivoOrigine, PreventiviDettaglio.MANOPERA);
                    altroPrev = tabPrevDett.getRighePreventivo(db, idPreventivoOrigine, PreventiviDettaglio.ALTRO);
                    collegamentiPrev = tabPrevDett.getRighePreventivo(db, idPreventivoOrigine, PreventiviDettaglio.COLLEGAMENTI);

                    materialePreventivo.addAll(materialiLocaliPrev);
                    materialePreventivo.addAll(materialiPreventivoPrev);
                    materialePreventivo.addAll(collegamentiPrev);
                    materialePreventivo.addAll(placcheLocaliPrev);
                    materialePreventivo.addAll(placchePreventivoPrev);
                }

                //materiali e materiali preventivo raggruppo per (elemento,componente,codice)


                ArrayList<Object> materialiRaggruppati = raggruppaMateriali(db, materialeOrdine, materialePreventivo);
                if (materialiRaggruppati.size()>1) {
                    Collections.sort(materialiRaggruppati, new Comparator<Object>() {
                        @Override
                        public int compare(Object o, Object o2) {
                            String descri = ((ContentValues) o).getAsString(PreventiviDettaglio.DESCRIZIONE);
                            String descri2 = ((ContentValues) o2).getAsString(PreventiviDettaglio.DESCRIZIONE);
                            return descri.compareToIgnoreCase(descri2);
                        }
                    });
                }

                for (int j=0;j<materialiRaggruppati.size();j++){
                    _aggiungiRiga((ContentValues)materialiRaggruppati.get(j));
                }

                //manodopera
                if (manodoperaPrev==null){
                    manodoperaPrev = new ArrayList<Object>();
                }
                ArrayList<Object> manodoperaRaggruppati = raggruppaManodopera(db, manodopera, manodoperaPrev);
                if (manodoperaRaggruppati.size()>1) {
                    Collections.sort(manodoperaRaggruppati, new Comparator<Object>() {
                        @Override
                        public int compare(Object o, Object o2) {
                            String descri = ((ContentValues) o).getAsString(PreventiviDettaglio.DESCRIZIONE);
                            String descri2 = ((ContentValues) o2).getAsString(PreventiviDettaglio.DESCRIZIONE);
                            return descri.compareToIgnoreCase(descri2);
                        }
                    });
                }
                for (int j=0;j<manodoperaRaggruppati.size();j++){
                    _aggiungiRiga((ContentValues)manodoperaRaggruppati.get(j));
                }


                //altro
                if (altroPrev==null){
                    altroPrev = new ArrayList<Object>();
                }
                ArrayList<Object> altroRaggruppato = raggruppaAltro(db,altro,altroPrev);
                for (int j=0;j<altroRaggruppato.size();j++){
                    _aggiungiRiga((ContentValues)altroRaggruppato.get(j));
                }
            }

        }

        _aggiungiRigaTotali();


        //genero il foglio rapportini
        _aggiungiRigaIntestazioneRapportini();
        String ordini = "";
        for (int i=0;i<listaOrdini.size();i++){
            ordini = ordini + listaOrdini.get(i)+",";
        }
        if (ordini.endsWith(",")){
            ordini = ordini.substring(0,ordini.length()-1);
        }
        ArrayList<Object> rapportini =  db.eseguiSelect("Select rapportini_dettaglio.*,rapportini.data_rapportino,utenti.nome as nomeutente,utenti.cognome as cognomeutente,manodopera.nome,manodopera.costo_orario from rapportini inner join rapportini_dettaglio on rapportini.id_rapportino=rapportini_dettaglio.id_rapportino inner join manodopera on rapportini_dettaglio.id_manodopera=manodopera.id_manodopera inner join utenti on rapportini.id_operatore=utenti.id_utente inner join preventivi on rapportini.id_ordine=preventivi.id_preventivo where rapportini.id_ordine in ("+ordini+") order by rapportini.data_rapportino,rapportini.id_operatore,rapportini.id_rapportino", null);
        db.close();

        for (int i=0;i<rapportini.size();i++){
            _aggiungiRigaRapportino((ContentValues)rapportini.get(i));
        }

        //riga totale rapportini
       _aggiungiRigaTotaliRapportini();

        String ragioneSociale = "";
        if (valClie!=null){
            ragioneSociale = valClie.getAsString(Anagrafica.RAGIONE_SOCIALE);
        }

        ragioneSociale = Utility.formattaStringaPerNomeFile(ragioneSociale);
        String cartellaPrev = PreventiviDettaglio.PATH_EXPORT_PREVENTIVI;


        // Create a path where we will place our List of objects on external storage
        File dirExport = new File(Environment.getExternalStorageDirectory() + File.separator + "ECONTAB" + File.separator+ragioneSociale+File.separator
                + PreventiviDettaglio.PATH_EXPORT_CONSUNTIVI);


        if (!dirExport.exists()) {
            dirExport.mkdirs();
        }

        String nomePrev = "cons_" + Utility.dataToString(Calendar.getInstance(),"ddMMyyyy")+"_"+Utility.dataToString(Calendar.getInstance(),"hhmmss");


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

    private ArrayList<Object> raggruppaMateriali(DbInterno db, ArrayList<Object> materialiOrdine, ArrayList<Object> materialiPreventivo) {
        HashMap<String, ContentValues> mappaRaggruppata = new HashMap<String, ContentValues>();
        HashMap<String, ContentValues> mappaRaggruppataPrev = new HashMap<String, ContentValues>();

        HashMap<String,HashMap<String,ContentValues>> mappaCodiciRaggruppata = new HashMap<String, HashMap<String,ContentValues>>();
        HashMap<String,HashMap<String,ContentValues>> mappaCodiciRaggruppataPrev = new HashMap<String, HashMap<String,ContentValues>>();

        ArrayList<Object> materialiRaggruppati = new ArrayList<Object>();



        PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();
        PlaccheModuli tabPlacche = new PlaccheModuli();
        //raggruppo prima i materiali dell ordine



        for (int i=0;i<materialiOrdine.size();i++){
            ContentValues curr = (ContentValues)materialiOrdine.get(i);
            int idPlaccaModuli = curr.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI);
            if (idPlaccaModuli!=0){

                ContentValues valPlaccaLinea = tabPlacche.getRecordPlaccaConLineaFornitore(db, idPlaccaModuli);
                if (valPlaccaLinea != null) {
                    curr.put(PreventiviDettaglio.DESCRIZIONE,ctx.getString(R.string.placca) + " " + curr.getAsString(PreventiviDettaglio.DESCRIZIONE) + " "+ valPlaccaLinea.getAsInteger(PlaccheModuli.NUMERO_MODULI) + " " + ctx.getString(R.string.moduli));
                }
            }

             _aggiungiInformazioniFornitoreLinea(curr, db);


            String chiave = curr.getAsInteger(PreventiviDettaglio.ID_ELEMENTO)+"|"+curr.getAsInteger(PreventiviDettaglio.ID_COMPONENTE)+"|"+curr.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI);
            if (mappaRaggruppata.containsKey(chiave)){
                ContentValues val = mappaRaggruppata.get(chiave);
                double qtaOld = val.getAsDouble(PreventiviDettaglio.QUANTITA);
                double qtaCurr = curr.getAsDouble(PreventiviDettaglio.QUANTITA);
                double importoOld = val.getAsDouble("IMPORTO");
                double importoCurr = tabPrevDett.getImportoRiga(curr);
                val.put(PreventiviDettaglio.QUANTITA, qtaOld + qtaCurr);
                val.put("IMPORTO",importoOld + importoCurr);


                String codiceCurr = curr.getAsString(PreventiviDettaglio.CODICE_ARTICOLO);
                double prezzoCurr = curr.getAsDouble(PreventiviDettaglio.PREZZO);
                String chiaveCodice = codiceCurr+"|"+prezzoCurr;

                HashMap<String,ContentValues> codici = mappaCodiciRaggruppata.get(chiave);
                if (codici.containsKey(chiaveCodice)){
                    ContentValues currCodice = codici.get(chiaveCodice);
                    double oldQtaCod = currCodice.getAsDouble(PreventiviDettaglio.QUANTITA);
                    currCodice.put(PreventiviDettaglio.QUANTITA,oldQtaCod+Utility.arrotonda(curr.getAsDouble(PreventiviDettaglio.QUANTITA), 2));
                }
                else{
                    ContentValues currCodice = new ContentValues();
                    currCodice.put(PreventiviDettaglio.CODICE_ARTICOLO,codiceCurr);
                    currCodice.put(PreventiviDettaglio.QUANTITA,Utility.arrotonda(curr.getAsDouble(PreventiviDettaglio.QUANTITA),2));
                    currCodice.put(PreventiviDettaglio.PREZZO,curr.getAsDouble(PreventiviDettaglio.PREZZO));
                    codici.put(chiaveCodice,currCodice );
                }

               // String codiciRaggruppati = _getStringaCodicePrezzo(db,curr);
              //  val.put("CODICI",val.getAsString("CODICI")+System.getProperty("line.separator")+codiciRaggruppati);
            }
            else{
                curr.put("IMPORTO",tabPrevDett.getImportoRiga(curr));
              //  String codiciRaggruppati = _getStringaCodicePrezzo(db,curr);
               // curr.put("CODICI",codiciRaggruppati);
                mappaRaggruppata.put(chiave, curr);

                String codiceCurr = curr.getAsString(PreventiviDettaglio.CODICE_ARTICOLO);
                double prezzoCurr = curr.getAsDouble(PreventiviDettaglio.PREZZO);
                String chiaveCodice = codiceCurr+"|"+prezzoCurr;
                ContentValues currCodice = new ContentValues();
                currCodice.put(PreventiviDettaglio.CODICE_ARTICOLO, codiceCurr);
                currCodice.put(PreventiviDettaglio.QUANTITA,Utility.arrotonda(curr.getAsDouble(PreventiviDettaglio.QUANTITA),2));
                currCodice.put(PreventiviDettaglio.PREZZO,curr.getAsDouble(PreventiviDettaglio.PREZZO));
                HashMap<String,ContentValues> codici = new HashMap<String, ContentValues>();
                codici.put(chiaveCodice,currCodice );
                mappaCodiciRaggruppata.put(chiave,codici);

            }
        }




        for (int i=0;i<materialiPreventivo.size();i++){
            ContentValues curr = (ContentValues)materialiPreventivo.get(i);
            int idPlaccaModuli = curr.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI);
            if (idPlaccaModuli!=0){
                ContentValues valPlaccaLinea = tabPlacche.getRecordPlaccaConLineaFornitore(db, idPlaccaModuli);
                if (valPlaccaLinea != null) {
                    curr.put(PreventiviDettaglio.DESCRIZIONE,ctx.getString(R.string.placca) + " " + curr.getAsString(PreventiviDettaglio.DESCRIZIONE) + " "+ valPlaccaLinea.getAsInteger(PlaccheModuli.NUMERO_MODULI) + " " + ctx.getString(R.string.moduli));
                }
            }
            _aggiungiInformazioniFornitoreLinea(curr, db);
            String chiave = curr.getAsInteger(PreventiviDettaglio.ID_ELEMENTO)+"|"+curr.getAsInteger(PreventiviDettaglio.ID_COMPONENTE)+"|"+curr.getAsInteger(PreventiviDettaglio.ID_PLACCA_MODULI);
            if (mappaRaggruppataPrev.containsKey(chiave)){
                ContentValues val = mappaRaggruppataPrev.get(chiave);
                double qtaOld = val.getAsDouble(PreventiviDettaglio.QUANTITA);
                double qtaCurr = curr.getAsDouble(PreventiviDettaglio.QUANTITA);
                double importoOld = val.getAsDouble("IMPORTO");
                double importoCurr = tabPrevDett.getImportoRiga(curr);
                val.put(PreventiviDettaglio.QUANTITA, qtaOld + qtaCurr);
                val.put("IMPORTO",importoOld + importoCurr);
               // String codiciRaggruppati = _getStringaCodicePrezzo(db,curr);
               // val.put("CODICI",val.getAsString("CODICI")+System.getProperty("line.separator")+codiciRaggruppati);

                String codiceCurr = curr.getAsString(PreventiviDettaglio.CODICE_ARTICOLO);
                double prezzoCurr = curr.getAsDouble(PreventiviDettaglio.PREZZO);
                String chiaveCodice = codiceCurr+"|"+prezzoCurr;

                HashMap<String,ContentValues> codici = mappaCodiciRaggruppataPrev.get(chiave);
                if (codici.containsKey(chiaveCodice)){
                    ContentValues currCodice = codici.get(chiaveCodice);
                    double oldQtaCod = currCodice.getAsDouble(PreventiviDettaglio.QUANTITA);
                    currCodice.put(PreventiviDettaglio.QUANTITA,oldQtaCod+Utility.arrotonda(curr.getAsDouble(PreventiviDettaglio.QUANTITA),2));
                }
                else{
                    ContentValues currCodice = new ContentValues();
                    currCodice.put(PreventiviDettaglio.CODICE_ARTICOLO,codiceCurr);
                    currCodice.put(PreventiviDettaglio.QUANTITA,Utility.arrotonda(curr.getAsDouble(PreventiviDettaglio.QUANTITA),2));
                    currCodice.put(PreventiviDettaglio.PREZZO,curr.getAsDouble(PreventiviDettaglio.PREZZO));
                    codici.put(chiaveCodice,currCodice );
                }

            }
            else{
                curr.put("IMPORTO",tabPrevDett.getImportoRiga(curr));
               // String codiciRaggruppati = _getStringaCodicePrezzo(db,curr);
               // curr.put("CODICI",codiciRaggruppati);
                mappaRaggruppataPrev.put(chiave, curr);


                String codiceCurr = curr.getAsString(PreventiviDettaglio.CODICE_ARTICOLO);
                double prezzoCurr = curr.getAsDouble(PreventiviDettaglio.PREZZO);
                String chiaveCodice = codiceCurr+"|"+prezzoCurr;
                ContentValues currCodice = new ContentValues();
                currCodice.put(PreventiviDettaglio.CODICE_ARTICOLO,codiceCurr);
                currCodice.put(PreventiviDettaglio.QUANTITA,Utility.arrotonda(curr.getAsDouble(PreventiviDettaglio.QUANTITA),2));
                currCodice.put(PreventiviDettaglio.PREZZO,curr.getAsDouble(PreventiviDettaglio.PREZZO));
                HashMap<String,ContentValues> codici = new HashMap<String, ContentValues>();
                codici.put(chiaveCodice,currCodice );
                mappaCodiciRaggruppataPrev.put(chiave,codici);
            }
        }

        //aggiungo le info sui codici
        Iterator<String> iterPerCodici = mappaRaggruppata.keySet().iterator();
        while (iterPerCodici.hasNext()){
            String chiave = iterPerCodici.next();
            ContentValues val = mappaRaggruppata.get(chiave);
            HashMap<String,ContentValues> mappaCodici = mappaCodiciRaggruppata.get(chiave);
            String codici = "";
            Iterator<String> iter2 = mappaCodici.keySet().iterator();
            while (iter2.hasNext()){
                String chiaveCod = iter2.next();
                ContentValues valCod = mappaCodici.get(chiaveCod);
                String testoCodice = _getStringaCodicePrezzo(db,valCod);
                codici = codici+testoCodice+System.getProperty("line.separator");
            }
            val.put("CODICI",codici);

        }


        Iterator<String> iterPerCodiciPrev = mappaRaggruppataPrev.keySet().iterator();
        while (iterPerCodiciPrev.hasNext()){
            String chiave = iterPerCodiciPrev.next();
            ContentValues val = mappaRaggruppataPrev.get(chiave);
            HashMap<String,ContentValues> mappaCodici = mappaCodiciRaggruppataPrev.get(chiave);
            String codici = "";
            Iterator<String> iter2 = mappaCodici.keySet().iterator();
            while (iter2.hasNext()){
                String chiaveCod = iter2.next();
                ContentValues valCod = mappaCodici.get(chiaveCod);
                String testoCodice = _getStringaCodicePrezzo(db,valCod);
                codici = codici+testoCodice+System.getProperty("line.separator");
            }
            val.put("CODICI",codici);

        }


        //associo alle righe di ordine le righe di preventivo
        Iterator<String> iter = mappaRaggruppata.keySet().iterator();
        while (iter.hasNext()){
            String chiave = iter.next();
            ContentValues val = mappaRaggruppata.get(chiave);
            if (mappaRaggruppataPrev.containsKey(chiave)){
                ContentValues prev =  mappaRaggruppataPrev.remove(chiave);
                val.put("CODICI_PREV",prev.getAsString("CODICI"));
                val.put("QTA_PREV",prev.getAsDouble(PreventiviDettaglio.QUANTITA));
                val.put("IMPORTO_PREV",prev.getAsDouble("IMPORTO"));
            }
            else{
                val.put("CODICI_PREV","");
                val.put("QTA_PREV",0);
                val.put("IMPORTO_PREV",0);
            }
        }

        //Infine aggiungo eventuali righe che erano solo sul preventivo
        Iterator<String> iterPrev = mappaRaggruppataPrev.keySet().iterator();
        while (iterPrev.hasNext()){
            String chiave = iterPrev.next();
            ContentValues val = mappaRaggruppataPrev.get(chiave);
            val.put("CODICI_PREV",val.getAsString("CODICI"));
            val.put("QTA_PREV",val.getAsDouble(PreventiviDettaglio.QUANTITA));
            val.put("IMPORTO_PREV",val.getAsDouble("IMPORTO"));

            val.put("CODICI","");
            val.put(PreventiviDettaglio.QUANTITA,0);
            val.put("IMPORTO",0);
            mappaRaggruppata.put(chiave,val);
        }

        materialiRaggruppati.addAll(mappaRaggruppata.values());

        return materialiRaggruppati;

    }



    private ArrayList<Object> raggruppaManodopera(DbInterno db, ArrayList<Object> manodopera, ArrayList<Object> manodoperaPrev) {
        HashMap<String, ContentValues> mappaRaggruppata = new HashMap<String, ContentValues>();
        HashMap<String, ContentValues> mappaRaggruppataPrev = new HashMap<String, ContentValues>();

        PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();

        ArrayList<Object> manodoperaRaggruppati = new ArrayList<Object>();

        for (int i=0;i<manodopera.size();i++){
            ContentValues curr = (ContentValues)manodopera.get(i);

            String chiave = ""+curr.getAsInteger(PreventiviDettaglio.ID_MANODOPERA);
            if (mappaRaggruppata.containsKey(chiave)){
                ContentValues val = mappaRaggruppata.get(chiave);
                double qtaOld = val.getAsDouble(PreventiviDettaglio.QUANTITA);
                double qtaCurr = curr.getAsDouble(PreventiviDettaglio.QUANTITA);
                double importoOld = val.getAsDouble("IMPORTO");
                double importoCurr = tabPrevDett.getImportoRiga(curr);
                int numOperatorei = curr.getAsInteger(PreventiviDettaglio.NUM_OPERATORI);
                if (numOperatorei==0){
                    numOperatorei = 1;
                }
                val.put(PreventiviDettaglio.QUANTITA, qtaOld + qtaCurr*numOperatorei);
                val.put("IMPORTO",importoOld + importoCurr);
            }
            else{
                curr.put("IMPORTO",tabPrevDett.getImportoRiga(curr));
                double qtaCurr = curr.getAsDouble(PreventiviDettaglio.QUANTITA);
                int numOperatorei = curr.getAsInteger(PreventiviDettaglio.NUM_OPERATORI);
                if (numOperatorei==0){
                    numOperatorei = 1;
                }
                curr.put(PreventiviDettaglio.QUANTITA,qtaCurr*numOperatorei);
                mappaRaggruppata.put(chiave, curr);
            }
        }


        for (int i=0;i<manodoperaPrev.size();i++){
            ContentValues curr = (ContentValues)manodoperaPrev.get(i);

            String chiave = ""+curr.getAsInteger(PreventiviDettaglio.ID_MANODOPERA);
            if (mappaRaggruppataPrev.containsKey(chiave)){
                ContentValues val = mappaRaggruppataPrev.get(chiave);
                double qtaOld = val.getAsDouble(PreventiviDettaglio.QUANTITA);
                double qtaCurr = curr.getAsDouble(PreventiviDettaglio.QUANTITA);
                double importoOld = val.getAsDouble("IMPORTO");
                double importoCurr = tabPrevDett.getImportoRiga(curr);
                int numOperatorei = curr.getAsInteger(PreventiviDettaglio.NUM_OPERATORI);
                if (numOperatorei==0){
                    numOperatorei = 1;
                }
                val.put(PreventiviDettaglio.QUANTITA, qtaOld + qtaCurr*numOperatorei);
                val.put("IMPORTO",importoOld + importoCurr);
            }
            else{
                curr.put("IMPORTO",tabPrevDett.getImportoRiga(curr));
                double qtaCurr = curr.getAsDouble(PreventiviDettaglio.QUANTITA);
                int numOperatorei = curr.getAsInteger(PreventiviDettaglio.NUM_OPERATORI);
                if (numOperatorei==0){
                    numOperatorei = 1;
                }
                curr.put(PreventiviDettaglio.QUANTITA,qtaCurr*numOperatorei);
                mappaRaggruppataPrev.put(chiave, curr);
            }
        }

        //associo alle righe di ordine le righe di preventivo
        Iterator<String> iter = mappaRaggruppata.keySet().iterator();
        while (iter.hasNext()){
            String chiave = iter.next();
            ContentValues val = mappaRaggruppata.get(chiave);
            if (mappaRaggruppataPrev.containsKey(chiave)){
                ContentValues prev =  mappaRaggruppataPrev.remove(chiave);

                val.put("QTA_PREV",prev.getAsDouble(PreventiviDettaglio.QUANTITA));
                val.put("IMPORTO_PREV",prev.getAsDouble("IMPORTO"));
            }
            else{

                val.put("QTA_PREV",0);
                val.put("IMPORTO_PREV",0);
            }
        }

        //Infine aggiungo eventuali righe che erano solo sul preventivo
        Iterator<String> iterPrev = mappaRaggruppataPrev.keySet().iterator();
        while (iterPrev.hasNext()){
            String chiave = iterPrev.next();
            ContentValues val = mappaRaggruppataPrev.get(chiave);

            val.put("QTA_PREV",val.getAsDouble(PreventiviDettaglio.QUANTITA));
            val.put("IMPORTO_PREV",val.getAsDouble("IMPORTO"));

            val.put(PreventiviDettaglio.QUANTITA,0);
            val.put("IMPORTO",0);
            mappaRaggruppata.put(chiave,val);
        }


        manodoperaRaggruppati.addAll(mappaRaggruppata.values());

        return manodoperaRaggruppati;
    }


    private ArrayList<Object> raggruppaAltro(DbInterno db, ArrayList<Object> altro, ArrayList<Object> altroPrev) {
        ArrayList<Object> altroRaggruppato = new ArrayList<Object>();
        PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();

        ContentValues valMaster = null;
        ContentValues valMasterPrev = null;

        if (altro.size()>0){
            valMaster = (ContentValues)altro.get(0);
            valMaster.put("IMPORTO",tabPrevDett.getImportoRiga(valMaster));
            for (int i=1;i<altro.size();i++){
                ContentValues curr = (ContentValues)altro.get(i);
              //  String descrizioneOld = valMaster.getAsString(PreventiviDettaglio.DESCRIZIONE);
                double qtaOld = valMaster.getAsDouble(PreventiviDettaglio.QUANTITA);
                double qtaCurr = curr.getAsDouble(PreventiviDettaglio.QUANTITA);
                double importoOld = valMaster.getAsDouble("IMPORTO");
                double importoCurr = tabPrevDett.getImportoRiga(curr);
                valMaster.put(PreventiviDettaglio.QUANTITA, qtaOld + qtaCurr);
                valMaster.put("IMPORTO",importoOld + importoCurr);
               // valMaster.put(PreventiviDettaglio.DESCRIZIONE,descrizioneOld + System.getProperty("line.separator") +curr.getAsString(PreventiviDettaglio.DESCRIZIONE));
            }
        }


        if (altroPrev.size()>0){
            valMasterPrev = (ContentValues)altroPrev.get(0);
            valMasterPrev.put("IMPORTO",tabPrevDett.getImportoRiga(valMasterPrev));

            for (int i=1;i<altroPrev.size();i++){
                ContentValues curr = (ContentValues)altroPrev.get(i);
              //  String descrizioneOld = valMasterPrev.getAsString(PreventiviDettaglio.DESCRIZIONE);
                double qtaOld = valMasterPrev.getAsDouble(PreventiviDettaglio.QUANTITA);
                double qtaCurr = curr.getAsDouble(PreventiviDettaglio.QUANTITA);
                double importoOld = valMasterPrev.getAsDouble("IMPORTO");
                double importoCurr = tabPrevDett.getImportoRiga(curr);
                valMasterPrev.put(PreventiviDettaglio.QUANTITA, qtaOld + qtaCurr);
                valMasterPrev.put("IMPORTO",importoOld + importoCurr);
               // valMasterPrev.put(PreventiviDettaglio.DESCRIZIONE,descrizioneOld + System.getProperty("line.separator") +curr.getAsString(PreventiviDettaglio.DESCRIZIONE));
            }
        }

        if (valMaster!=null){
            if (valMasterPrev!=null){
                valMaster.put("QTA_PREV",valMasterPrev.getAsDouble(PreventiviDettaglio.QUANTITA));
                valMaster.put("IMPORTO_PREV",valMasterPrev.getAsDouble("IMPORTO"));
            }
            else{
                valMaster.put("QTA_PREV",0);
                valMaster.put("IMPORTO_PREV",0);
            }
            valMaster.put(PreventiviDettaglio.DESCRIZIONE,"Altro");
            altroRaggruppato.add(valMaster);
        }
        else{
            if (valMasterPrev!=null){
                valMasterPrev.put("QTA_PREV",valMasterPrev.getAsDouble(PreventiviDettaglio.QUANTITA));
                valMasterPrev.put("IMPORTO_PREV",valMasterPrev.getAsDouble("IMPORTO"));

                valMasterPrev.put(PreventiviDettaglio.QUANTITA,0);
                valMasterPrev.put("IMPORTO",0);

                valMasterPrev.put(PreventiviDettaglio.DESCRIZIONE,"Altro");
                altroRaggruppato.add(valMasterPrev);
            }

        }




        return altroRaggruppato;
    }



    private String _getStringaCodicePrezzo(DbInterno db, ContentValues curr) {
       return curr.getAsString(PreventiviDettaglio.CODICE_ARTICOLO)+" ("+ctx.getString(R.string.qta)+" "+Utility.formatNumero(curr.getAsDouble(PreventiviDettaglio.QUANTITA))+","+ctx.getString(R.string.prezzo)+" "+Utility.formatNumero(curr.getAsDouble(PreventiviDettaglio.PREZZO), 2)+")";

    }

    private void _aggiungiTitoloOrdine(ContentValues valOrd) {
        Row row = sheet1.createRow(rowCount);
        sheet1.addMergedRegion(new CellRangeAddress(rowCount, rowCount, 0, 8));
        Cell c = row.createCell(0);
        c.setCellValue(ctx.getResources().getString(R.string.ordine_num_del, valOrd.getAsString(Preventivi.NUMERO),
                Utility.numberToData(valOrd.getAsLong(Preventivi.DATA)))+" "+ valOrd.getAsString(Preventivi.TITOLO));
        c.setCellStyle(csBlu);
        rowCount++;
    }


    private ContentValues _aggiungiTitolo(DbInterno db, int idPreventivo) {
        // TODO Auto-generated method stub

        Preventivi tabPrev = new Preventivi();
        ContentValues val = tabPrev.getClienteCantierePreventivo(db, idPreventivo);

        Row row = sheet1.createRow(0);
        sheet1.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

        String testoClienteCantiere = ctx.getResources().getString(R.string.cliente).toUpperCase(Locale.getDefault());

        testoClienteCantiere = testoClienteCantiere + ": " + val.getAsString(Anagrafica.RAGIONE_SOCIALE);
        Cell c = row.createCell(0);
        c.setCellValue(testoClienteCantiere);
        c.setCellStyle(csBluBold);

        row = sheet1.createRow(1);
        sheet1.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));
        testoClienteCantiere = val.getAsString(Anagrafica.INDIRIZZO);
        testoClienteCantiere = testoClienteCantiere + ", " + val.getAsString(Anagrafica.CITTA);
        testoClienteCantiere = testoClienteCantiere + " (" + val.getAsString(Anagrafica.PROVINCIA) + ")";
        if (testoClienteCantiere.equals(",  ()")) {
            testoClienteCantiere = "";
        }
        c = row.createCell(0);
        c.setCellValue(testoClienteCantiere);
        c.setCellStyle(csBlu);

        row = sheet1.createRow(2);
        sheet1.addMergedRegion(new CellRangeAddress(2, 2, 0, 8));
        testoClienteCantiere = ctx.getResources().getString(R.string.cantiere).toUpperCase(Locale.getDefault());
        testoClienteCantiere = testoClienteCantiere + ": " + val.getAsString(Cantieri.NOME);
        c = row.createCell(0);
        c.setCellValue(testoClienteCantiere);
        c.setCellStyle(csBluBold);

        row = sheet1.createRow(3);
        sheet1.addMergedRegion(new CellRangeAddress(3, 3, 0, 8));

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


    private void _aggiungiRigaIntestazione() {
        // TODO Auto-generated method stub
        // Generate column headings
        Cell c = null;


        Row row = sheet1.createRow(rowCount);



        c = row.createCell(0);
        c.setCellValue(ctx.getResources().getString(R.string.descrizione).toUpperCase(Locale.getDefault()));
        c.setCellStyle(csIntestazione);





        c = row.createCell(1);
        c.setCellValue(ctx.getResources().getString(R.string.unita_misura).toUpperCase(Locale.getDefault()));
        c.setCellStyle(csIntestazione);

        c = row.createCell(2);
        c.setCellValue(ctx.getResources().getString(R.string.quantita).toUpperCase(Locale.getDefault()));
        c.setCellStyle(csIntestazione);

        c = row.createCell(3);
        c.setCellValue(ctx.getResources().getString(R.string.quantita_preventivo).toUpperCase(Locale.getDefault()));
        c.setCellStyle(csIntestazione);

        c = row.createCell(4);
        c.setCellValue(ctx.getResources().getString(R.string.differenza_qta).toUpperCase(Locale.getDefault()));
        c.setCellStyle(csIntestazione);


        c = row.createCell(5);
        c.setCellValue(ctx.getResources().getString(R.string.importo).toUpperCase(Locale.getDefault()));
        c.setCellStyle(csIntestazione);


        c = row.createCell(6);
        c.setCellValue(ctx.getResources().getString(R.string.importo_preventivo).toUpperCase(Locale.getDefault()));
        c.setCellStyle(csIntestazione);

        c = row.createCell(7);
        c.setCellValue(ctx.getResources().getString(R.string.differenza_importo).toUpperCase(Locale.getDefault()));
        c.setCellStyle(csIntestazione);

        c = row.createCell(8);
        c.setCellValue(ctx.getResources().getString(R.string.codici_ordine).toUpperCase(Locale.getDefault()));
        c.setCellStyle(csIntestazione);

        c = row.createCell(9);
        c.setCellValue(ctx.getResources().getString(R.string.codici_preventivo).toUpperCase(Locale.getDefault()));
        c.setCellStyle(csIntestazione);


        sheet1.setColumnWidth(0, (15 * 500));
        sheet1.setColumnWidth(1, (15 * 200));
        sheet1.setColumnWidth(2, (15 * 300));
        sheet1.setColumnWidth(3, (15 * 300));
        sheet1.setColumnWidth(4, (15 * 300));
        sheet1.setColumnWidth(5, (15 * 300));
        sheet1.setColumnWidth(6, (15 * 300));
        sheet1.setColumnWidth(7, (15 * 300));
        sheet1.setColumnWidth(8, (15 * 600));
        sheet1.setColumnWidth(9, (15 * 600));




        rowCount++;
    }


    private void _aggiungiRiga(ContentValues val) {
        // TODO Auto-generated method stub
        Row riga = sheet1.createRow(rowCount);



        Cell c = riga.createCell(0);
        c.setCellValue(val.getAsString(PreventiviDettaglio.DESCRIZIONE));

        c = riga.createCell(1);
        c.setCellValue(val.getAsString(PreventiviDettaglio.UNITA_MISURA));// UDM

        c = riga.createCell(2);

        c.setCellValue(Utility.arrotonda(val.getAsDouble(PreventiviDettaglio.QUANTITA), 2));// QTA
        c.setCellType(Cell.CELL_TYPE_NUMERIC);
        qtaTotOrd = qtaTotOrd+Utility.arrotonda(val.getAsDouble(PreventiviDettaglio.QUANTITA), 2);


        c = riga.createCell(3);
        c.setCellValue(Utility.arrotonda(val.getAsDouble("QTA_PREV"), 2));
        c.setCellType(Cell.CELL_TYPE_NUMERIC);
        c.setCellStyle(csPrev);
        c = riga.createCell(4);
        qtaTotPrev = qtaTotPrev+Utility.arrotonda(val.getAsDouble("QTA_PREV"), 2);

        String cellaQta = "C" + (rowCount+1);
        String cellaQtaPrev = "D" + (rowCount+1);
        double diffQta = Utility.arrotonda(val.getAsDouble(PreventiviDettaglio.QUANTITA),2) - Utility.arrotonda(val.getAsDouble("QTA_PREV"), 2);
        c.setCellFormula("SUM(" + cellaQta + ",-" + cellaQtaPrev + ")");
        c.setCellType(Cell.CELL_TYPE_FORMULA);
        c.setCellValue(diffQta);
        if (diffQta>0){
            c.setCellStyle(csRosso);
        }
        if (diffQta<0){
            c.setCellStyle(csVerde);
        }


        c = riga.createCell(5);
        c.setCellValue(Utility.arrotonda(val.getAsDouble("IMPORTO"),2));// PREZZO
        c.setCellType(Cell.CELL_TYPE_NUMERIC);
        impTotOrd = impTotOrd+Utility.arrotonda(val.getAsDouble("IMPORTO"),2);


        c = riga.createCell(6);
        c.setCellValue(Utility.arrotonda(val.getAsDouble("IMPORTO_PREV"), 2));
        c.setCellType(Cell.CELL_TYPE_NUMERIC);
        c.setCellStyle(csPrev);
        c = riga.createCell(7);
        impTotPrev = impTotPrev+Utility.arrotonda(val.getAsDouble("IMPORTO_PREV"),2);

        String cellaImporto = "F" + (rowCount+1);
        String cellaImportoPrev = "G" + (rowCount+1);
        double diffImporto = Utility.arrotonda(val.getAsDouble("IMPORTO"), 2) - Utility.arrotonda(val.getAsDouble("IMPORTO_PREV"), 2);
        c.setCellFormula("SUM(" + cellaImporto + ",-" + cellaImportoPrev + ")");
        c.setCellType(Cell.CELL_TYPE_FORMULA);
        c.setCellValue(diffImporto);
        if (diffImporto>0){
            c.setCellStyle(csRosso);
        }
        if (diffImporto<0){
            c.setCellStyle(csVerde);
        }


        c = riga.createCell(8);
        c.setCellStyle(csCodici);
        if (val.containsKey("CODICI")){
            c.setCellValue(new HSSFRichTextString(val.getAsString("CODICI")));
        }


        c = riga.createCell(9);
        c.setCellStyle(csCodici);
        if (val.containsKey("CODICI_PREV")){
            c.setCellValue(new HSSFRichTextString(val.getAsString("CODICI_PREV")));
        }


        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(2), wb, CellUtil.BORDER_LEFT, HSSFCellStyle.BORDER_MEDIUM);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(2), wb, CellUtil.LEFT_BORDER_COLOR, HSSFColor.BLACK.index);

        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(5), wb, CellUtil.BORDER_LEFT, HSSFCellStyle.BORDER_MEDIUM);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(5), wb, CellUtil.LEFT_BORDER_COLOR, HSSFColor.BLACK.index);

        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(8), wb, CellUtil.BORDER_LEFT, HSSFCellStyle.BORDER_MEDIUM);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(8), wb, CellUtil.LEFT_BORDER_COLOR, HSSFColor.BLACK.index);

        rowCount++;
    }



    private void _aggiungiRigaTotali() {
        rowCount++;
        Row riga = sheet1.createRow(rowCount);

        Cell c = riga.createCell(2);
        String cQta_0  = "C8";
        String cQta_1  = "C"+(rowCount-1);
        c.setCellFormula("SUM(" + cQta_0 + ":" + cQta_1 + ")");
        c.setCellType(Cell.CELL_TYPE_FORMULA);

        c.setCellValue(qtaTotOrd);


        c = riga.createCell(3);
        String cQtaPrev_0  = "D8";
        String cQtaPrev_1  = "D"+(rowCount-1);
        c.setCellFormula("SUM(" + cQtaPrev_0 + ":" + cQtaPrev_1 + ")");
        c.setCellType(Cell.CELL_TYPE_FORMULA);

        c.setCellValue(qtaTotPrev);

        c = riga.createCell(4);
        String cQtaDif_0  = "C"+(rowCount+1);
        String cQtaDif_1  = "D"+(rowCount+1);
        c.setCellFormula("SUM(" + cQtaDif_0 + ",-" + cQtaDif_1 + ")");
        c.setCellType(Cell.CELL_TYPE_FORMULA);

        c.setCellValue(qtaTotOrd-qtaTotPrev);
        if (qtaTotOrd-qtaTotPrev>0){
            c.setCellStyle(csRosso);
        }
        if (qtaTotOrd-qtaTotPrev<0){
            c.setCellStyle(csVerde);
        }


        c = riga.createCell(5);
        String cImp_0  = "F8";
        String cImp_1  = "F"+(rowCount-1);
        c.setCellFormula("SUM(" + cImp_0 + ":" + cImp_1 + ")");
        c.setCellType(Cell.CELL_TYPE_FORMULA);
        c.setCellValue(impTotOrd);


        c = riga.createCell(6);
        String cImpPrev_0  = "G8";
        String cImpPrev_1  = "G"+(rowCount-1);
        c.setCellFormula("SUM(" + cImpPrev_0 + ":" + cImpPrev_1 + ")");
        c.setCellType(Cell.CELL_TYPE_FORMULA);
        c.setCellValue(impTotPrev);


        c = riga.createCell(7);
        String cImpDif_0  = "F"+(rowCount+1);
        String cImpDif_1  = "G"+(rowCount+1);
        c.setCellFormula("SUM(" + cImpDif_0 + ",-" + cImpDif_1 + ")");
        c.setCellType(Cell.CELL_TYPE_FORMULA);

        c.setCellValue(impTotOrd-impTotPrev);
        if (impTotOrd-impTotPrev>0){
            c.setCellStyle(csRosso);
        }
        if (impTotOrd-impTotPrev<0){
            c.setCellStyle(csVerde);
        }

        HSSFCellUtil.setCellStyleProperty(HSSFCellUtil.getCell(sheet1.createRow(rowCount - 1),2),wb,CellUtil.LEFT_BORDER_COLOR,HSSFColor.BLACK.index);
        HSSFCellUtil.setCellStyleProperty(HSSFCellUtil.getCell(sheet1.getRow(rowCount - 1),2),wb,CellUtil.BORDER_LEFT,HSSFCellStyle.BORDER_MEDIUM);

        HSSFCellUtil.setCellStyleProperty(HSSFCellUtil.getCell(sheet1.getRow(rowCount - 1),5),wb,CellUtil.LEFT_BORDER_COLOR,HSSFColor.BLACK.index);
        HSSFCellUtil.setCellStyleProperty(HSSFCellUtil.getCell(sheet1.getRow(rowCount - 1),5),wb,CellUtil.BORDER_LEFT,HSSFCellStyle.BORDER_MEDIUM);

        HSSFCellUtil.setCellStyleProperty(HSSFCellUtil.getCell(sheet1.getRow(rowCount-1),8),wb,CellUtil.LEFT_BORDER_COLOR,HSSFColor.BLACK.index);
        HSSFCellUtil.setCellStyleProperty(HSSFCellUtil.getCell(sheet1.getRow(rowCount-1),8),wb,CellUtil.BORDER_LEFT,HSSFCellStyle.BORDER_MEDIUM);

        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(2),wb,CellUtil.TOP_BORDER_COLOR,HSSFColor.BLACK.index);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(2),wb,CellUtil.BORDER_TOP,HSSFCellStyle.BORDER_THIN);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(2),wb,CellUtil.LEFT_BORDER_COLOR,HSSFColor.BLACK.index);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(2),wb,CellUtil.BORDER_LEFT,HSSFCellStyle.BORDER_MEDIUM);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(3),wb,CellUtil.TOP_BORDER_COLOR,HSSFColor.BLACK.index);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(3),wb,CellUtil.BORDER_TOP,HSSFCellStyle.BORDER_THIN);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(4),wb,CellUtil.TOP_BORDER_COLOR,HSSFColor.BLACK.index);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(4),wb,CellUtil.BORDER_TOP,HSSFCellStyle.BORDER_THIN);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(5),wb,CellUtil.TOP_BORDER_COLOR,HSSFColor.BLACK.index);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(5),wb,CellUtil.BORDER_TOP,HSSFCellStyle.BORDER_THIN);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(5),wb,CellUtil.LEFT_BORDER_COLOR,HSSFColor.BLACK.index);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(5),wb,CellUtil.BORDER_LEFT,HSSFCellStyle.BORDER_MEDIUM);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(6),wb,CellUtil.TOP_BORDER_COLOR,HSSFColor.BLACK.index);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(6),wb,CellUtil.BORDER_TOP,HSSFCellStyle.BORDER_THIN);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(7),wb,CellUtil.TOP_BORDER_COLOR,HSSFColor.BLACK.index);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(7),wb,CellUtil.BORDER_TOP,HSSFCellStyle.BORDER_THIN);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(7),wb,CellUtil.RIGHT_BORDER_COLOR,HSSFColor.BLACK.index);
        HSSFCellUtil.setCellStyleProperty((HSSFCell) riga.getCell(7),wb,CellUtil.BORDER_RIGHT,HSSFCellStyle.BORDER_MEDIUM);

        HSSFCellUtil.setFont((HSSFCell) riga.getCell(2), wb, (HSSFFont)fTitolo);
        HSSFCellUtil.setFont((HSSFCell) riga.getCell(3), wb, (HSSFFont)fTitolo);
        HSSFCellUtil.setFont((HSSFCell) riga.getCell(4), wb, (HSSFFont)fTitolo);
        HSSFCellUtil.setFont((HSSFCell) riga.getCell(5), wb, (HSSFFont)fTitolo);
        HSSFCellUtil.setFont((HSSFCell) riga.getCell(6), wb, (HSSFFont)fTitolo);
        HSSFCellUtil.setFont((HSSFCell) riga.getCell(7), wb, (HSSFFont)fTitolo);

        riga.setHeightInPoints(20);

    }




    private void _aggiungiRigaIntestazioneRapportini() {
        // TODO Auto-generated method stub
        // Generate column headings
        Cell c = null;

        // Cell style for header row
        CellStyle cs = wb.createCellStyle();
        Font fBold = wb.createFont();
        fBold.setBoldweight(Font.BOLDWEIGHT_BOLD);

        cs.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
        cs.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        cs.setFont(fBold);

        Row row = sheet2.createRow(0);


        c = row.createCell(0);
        c.setCellValue(ctx.getResources().getString(R.string.data).toUpperCase(Locale.getDefault()));
        c.setCellStyle(cs);

        c = row.createCell(1);
        c.setCellValue(ctx.getResources().getString(R.string.operatore).toUpperCase(Locale.getDefault()));
        c.setCellStyle(cs);

        c = row.createCell(2);
        c.setCellValue(ctx.getResources().getString(R.string.tipo).toUpperCase(Locale.getDefault()));
        c.setCellStyle(cs);

        c = row.createCell(3);
        c.setCellValue(ctx.getResources().getString(R.string.descrizione).toUpperCase(Locale.getDefault()));
        c.setCellStyle(cs);

        c = row.createCell(4);
        c.setCellValue(ctx.getResources().getString(R.string.ore).toUpperCase(Locale.getDefault()));
        c.setCellStyle(cs);

        c = row.createCell(5);
        c.setCellValue(ctx.getResources().getString(R.string.costo_orario).toUpperCase(Locale.getDefault()));
        c.setCellStyle(cs);

        c = row.createCell(6);
        c.setCellValue(ctx.getResources().getString(R.string.totale).toUpperCase(Locale.getDefault()));
        c.setCellStyle(cs);


        sheet2.setColumnWidth(0, (15 * 200));
        sheet2.setColumnWidth(1, (15 * 300));
        sheet2.setColumnWidth(2, (15 * 200));
        sheet2.setColumnWidth(3, (15 * 500));
        sheet2.setColumnWidth(4, (15 * 200));
        sheet2.setColumnWidth(5, (15 * 200));
        sheet2.setColumnWidth(6, (15 * 200));


        rowCountRapportini++;
    }


    private void _aggiungiRigaRapportino(ContentValues val) {
        // TODO Auto-generated method stub



        Row riga = sheet2.createRow(rowCountRapportini);

        CellStyle cs = wb.createCellStyle();
        cs.setWrapText(true);

        CellStyle currencyCellStyle = wb.createCellStyle();
        currencyCellStyle.setWrapText(true);

        currencyCellStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));


        Cell c = riga.createCell(0);
        c.setCellValue(Utility.numberToData(val.getAsLong(Rapportini.DATA_RAPPORTINO)));// CODICE ARTICOLO
        c.setCellStyle(cs);

        c = riga.createCell(1);
        c.setCellValue(val.getAsString("nomeutente") + " " + val.getAsString("cognomeutente"));// CODICE ARTICOLO
        c.setCellStyle(cs);

        c = riga.createCell(2);
        c.setCellValue(val.getAsString(Manodopera.NOME));// CODICE ARTICOLO
        c.setCellStyle(cs);

        c = riga.createCell(3);
        c.setCellValue(val.getAsString(RapportiniDettaglio.NOTE));
        c.setCellStyle(cs);


        c = riga.createCell(4);
        oreTotRApportini = oreTotRApportini+ val.getAsDouble(RapportiniDettaglio.ORE);
        c.setCellValue(val.getAsDouble(RapportiniDettaglio.ORE));
        c.setCellType(Cell.CELL_TYPE_NUMERIC);
        c.setCellStyle(cs);

        c = riga.createCell(5);
        c.setCellValue(Utility.arrotonda(val.getAsDouble(Manodopera.COSTO_ORARIO), 2));
        c.setCellType(Cell.CELL_TYPE_NUMERIC);
        c.setCellStyle(currencyCellStyle);

        c = riga.createCell(6);
        double tot = val.getAsDouble(Manodopera.COSTO_ORARIO)  * val.getAsDouble(RapportiniDettaglio.ORE);
        String cellaQta = "E" + (rowCountRapportini + 1);
        String cellaPrezzo = "F" + (rowCountRapportini + 1);
        c.setCellFormula("PRODUCT(" + cellaQta + ":" + cellaPrezzo + ")");
        c.setCellValue(Utility.arrotonda(tot, 2));
        importoTotRapportini = importoTotRapportini + tot;
        c.setCellType(Cell.CELL_TYPE_FORMULA);
        c.setCellStyle(currencyCellStyle);



        rowCountRapportini++;
    }


    private void _aggiungiRigaTotaliRapportini() {
        CellStyle cs = wb.createCellStyle();
        Font fBold = wb.createFont();
        fBold.setBoldweight(Font.BOLDWEIGHT_BOLD);

        CellStyle csBold = wb.createCellStyle();
        csBold.setFont(fBold);
        Row riga = sheet2.createRow(rowCountRapportini);

        Cell c = riga.createCell(4);
        String cQta_0  = "E2";
        String cQta_1  = "E"+(rowCountRapportini);
        c.setCellFormula("SUM(" + cQta_0 + ":" + cQta_1 + ")");
        c.setCellType(Cell.CELL_TYPE_FORMULA);
        c.setCellStyle(csBold);
        c.setCellValue(Utility.arrotonda(oreTotRApportini,2));


        c = riga.createCell(6);
        String cImp_0  = "G2";
        String cImp_1  = "G"+(rowCountRapportini);
        c.setCellFormula("SUM(" + cImp_0 + ":" + cImp_1 + ")");
        c.setCellType(Cell.CELL_TYPE_FORMULA);
        c.setCellStyle(csBold);
        c.setCellValue(Utility.arrotonda(importoTotRapportini,2));



    }

}
