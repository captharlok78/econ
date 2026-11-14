package com.ncfsistemi.econ.export;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.disegno.DisegnoDati;
import com.ncfsistemi.econ.disegno.PiantinaView;
import com.ncfsistemi.econ.utils.Utility;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Excel per la dichiarazione di conformità di tutto il cantiere (erede di "Composizione scatole"): un foglio per ogni
 * locale con elementi, a sinistra la piantina (A1:F24) disegnata con il disegno nuovo, a destra la tabella con ogni
 * elemento, i suoi componenti (posizione, codice, sotto-componenti) e i collegamenti (elemento, componente e locale
 * dall'altra parte, cavo/tubo, metri). Stesso formato del portale (ConformitaExcel.php, pagina Disegno).
 *
 * Uso: {@link #piantine(Context, int)} sul thread della UI (la piantina è una View), poi {@link #genera} in background.
 */
public class ConformitaXLS {

    /** prima colonna della tabella (H), dopo la piantina */
    private static final int T = 7;
    private static final String[] TITOLI = {"N. / Pos.", "Elemento / componente", "Codice", "Collegato a", "Componente collegato",
            "Locale", "Collegamento", "Metri", "Note"};
    private static final int[] LARGHEZZE = {8, 38, 16, 28, 22, 16, 28, 8, 30};
    private static final int LATO_PIANTINA = 640;

    private final Context ctx;
    private HSSFWorkbook wb;
    private HSSFCellStyle sTitolo, sNormale, sIntestazione, sElemento, sComponente, sDettaglio, sMetri;

    public ConformitaXLS(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Locali del cantiere che hanno elementi: id → [unità, area, nome, livello dell'area]; in ordine di unità, livello
     * dell'area (dal più basso), area e locale in ordine alfabetico (STUDIO_DESIGN_GRAFICA.md §11).
     */
    public static LinkedHashMap<Integer, String[]> locali(Context ctx, int idCantiere) {
        LinkedHashMap<Integer, String[]> out = new LinkedHashMap<>();
        DbInterno db = new DbInterno(ctx);
        try {
            for (Object o : db.eseguiSelect("select l.id_locale, l.nome, a.nome as area, coalesce(a.livello,0) as livello, u.nome as unita from locali l"
                    + " inner join aree a on a.id_area=l.id_area inner join unita u on u.id_unita=a.id_unita"
                    + " where u.id_cantiere=" + idCantiere
                    + " and exists (select 1 from elementi_cantiere e where e.id_locale=l.id_locale)"
                    + " order by u.id_unita, coalesce(a.livello,0), lower(a.nome), a.id_area, lower(l.nome), l.id_locale", null)) {
                ContentValues v = (ContentValues) o;
                out.put(v.getAsInteger("id_locale"), new String[]{s(v, "unita"), s(v, "area"), s(v, "nome"), s(v, "livello")});
            }
        } finally {
            db.close();
        }
        return out;
    }

    /**
     * Immagini PNG delle piantine (tutta la griglia, elementi col simbolo e il numero), da chiamare sul thread della UI.
     */
    public static Map<Integer, byte[]> piantine(Context ctx, int idCantiere) {
        Map<Integer, byte[]> out = new HashMap<>();
        PiantinaView v = new PiantinaView(ctx);
        v.setModo(PiantinaView.GUARDA);
        int spec = View.MeasureSpec.makeMeasureSpec(LATO_PIANTINA, View.MeasureSpec.EXACTLY);
        v.measure(spec, spec);
        v.layout(0, 0, LATO_PIANTINA, LATO_PIANTINA);
        for (Integer idLocale : locali(ctx, idCantiere).keySet()) {
            DisegnoDati.Locale loc = DisegnoDati.locale(ctx, idLocale);
            if (loc == null) continue;
            v.setDati(loc, posati(ctx, idLocale), new ArrayList<>());
            v.inquadraTutto();
            Bitmap b = Bitmap.createBitmap(LATO_PIANTINA, LATO_PIANTINA, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            c.drawColor(Color.WHITE);
            v.draw(c);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.PNG, 90, bos);
            b.recycle();
            out.put(idLocale, bos.toByteArray());
        }
        return out;
    }

    /** Tutti gli elementi del locale (qualunque documento), per la piantina. */
    private static List<DisegnoDati.Posato> posati(Context ctx, int idLocale) {
        List<DisegnoDati.Posato> out = new ArrayList<>();
        DbInterno db = new DbInterno(ctx);
        try {
            for (Object o : db.eseguiSelect("select e.*, el.icona, el.id_categoria_generale from elementi_cantiere e"
                    + " left join elementi el on el.id_elemento=e.id_elemento where e.id_locale=" + idLocale
                    + " order by e.numero_identificativo", null)) {
                ContentValues v = (ContentValues) o;
                DisegnoDati.Posato p = new DisegnoDati.Posato();
                p.id = i(v, "id_elemento_cant");
                p.idElemento = i(v, "id_elemento");
                p.idCategoria = i(v, "id_categoria_generale");
                p.idPreventivo = i(v, "id_preventivo");
                p.nome = s(v, "nome_elemento_cant");
                p.numero = "" + i(v, "numero_identificativo");
                p.x = Math.max(0, Math.min(200, i(v, "pos_x")));
                p.y = Math.max(0, Math.min(200, i(v, "pos_y")));
                p.icona = v.getAsString("icona");
                out.add(p);
            }
        } finally {
            db.close();
        }
        return out;
    }

    /** Scrive il file e ne restituisce il percorso. */
    public String genera(int idCantiere, Map<Integer, byte[]> piantine) throws Exception {
        wb = new HSSFWorkbook();
        stili();
        DbInterno db = new DbInterno(ctx);
        String nomeCantiere = "cantiere", cliente = "";
        try {
            ContentValues c = db.getRecord("select c.nome, a.ragione_sociale from cantieri c left join anagrafica a"
                    + " on a.id_anagrafica=c.id_anagrafica where c.id_cantiere=" + idCantiere);
            if (c != null) {
                if (!s(c, "nome").isEmpty()) nomeCantiere = s(c, "nome");
                cliente = s(c, "ragione_sociale");
            }
            LinkedHashMap<Integer, String[]> locali = locali(ctx, idCantiere);
            Set<String> usati = new HashSet<>();
            for (Map.Entry<Integer, String[]> l : locali.entrySet()) {
                foglioLocale(db, l.getKey(), l.getValue(), nomeCantiere, piantine.get(l.getKey()), usati);
            }
            if (locali.isEmpty()) {
                HSSFSheet f = wb.createSheet("Conformità");
                f.createRow(0).createCell(0).setCellValue("Dichiarazione di conformità — " + nomeCantiere);
                f.createRow(1).createCell(0).setCellValue("Il cantiere non ha elementi disegnati.");
            }
        } finally {
            db.close();
        }

        File dir = new File(Utility.cartellaApp() + File.separator + Utility.formattaStringaPerNomeFile(cliente.isEmpty() ? "cantieri" : cliente)
                + File.separator + "conformita");
        if (!dir.exists()) dir.mkdirs();
        String nomeFile = "conformita_" + Utility.formattaStringaPerNomeFile(nomeCantiere) + "_"
                + new SimpleDateFormat("yyyy-MM-dd", Locale.ITALY).format(new Date()) + ".xls";
        File file = new File(dir, nomeFile);
        if (file.exists()) file.delete();
        FileOutputStream os = new FileOutputStream(file);
        try {
            wb.write(os);
        } finally {
            os.close();
        }
        return file.getPath();
    }

    // ------------------------------------------------------------------ foglio del locale

    private int r;
    private HSSFSheet f;

    private void foglioLocale(DbInterno db, int idLocale, String[] percorso, String nomeCantiere, byte[] png, Set<String> usati) {
        // etichetta del foglio "area - locale" (STUDIO_DESIGN_GRAFICA.md §11)
        f = wb.createSheet(nomeFoglio(percorso[1].trim().isEmpty() ? percorso[2] : percorso[1].trim() + " - " + percorso[2], usati));
        f.setDisplayGridlines(false);
        f.getPrintSetup().setLandscape(true);
        f.setAutobreaks(true);
        f.getPrintSetup().setFitWidth((short) 1);
        f.getPrintSetup().setFitHeight((short) 0);
        for (int c = 0; c < 6; c++) f.setColumnWidth(c, 11 * 256);
        f.setColumnWidth(6, 2 * 256);
        for (int i = 0; i < LARGHEZZE.length; i++) f.setColumnWidth(T + i, LARGHEZZE[i] * 256);

        if (png != null) {
            int idx = wb.addPicture(png, Workbook.PICTURE_TYPE_PNG);
            HSSFPatriarch disegno = f.createDrawingPatriarch();
            disegno.createPicture(new HSSFClientAnchor(0, 0, 0, 0, (short) 0, 0, (short) 6, 24), idx);
        }

        StringBuilder tit = new StringBuilder();
        int livello = 0;
        try { livello = Integer.parseInt(percorso[3]); } catch (Exception ignored) { }
        String[] parti = {percorso[0], percorso[1].trim().isEmpty() ? "" : percorso[1] + " (" + nomeLivello(livello) + ")", percorso[2]};
        for (String p : parti) {
            if (p == null || p.isEmpty()) continue;
            if (tit.length() > 0) tit.append(" › ");
            tit.append(p);
        }
        List<ContentValues> elementi = elementi(db, idLocale);
        cella(0, T, tit.toString(), sTitolo);
        cella(1, T, "Dichiarazione di conformità · " + nomeCantiere + " · " + elementi.size() + " elementi", sNormale);
        for (int i = 0; i < TITOLI.length; i++) cella(2, T + i, TITOLI[i], sIntestazione);
        r = 3;

        for (ContentValues e : elementi) {
            int id = i(e, "id_elemento_cant");
            List<String> note = new ArrayList<>();
            Double h = e.getAsDouble("altezza_da_terra");
            if (h != null && h > 0) note.add("h " + Math.round(h) + " cm");
            if (!s(e, "nome_linea").isEmpty()) note.add("Linea " + s(e, "nome_linea"));
            if (!s(e, "nome_placca").isEmpty()) note.add("Placca " + s(e, "nome_placca"));
            if (i(e, "prev_numero") > 0) note.add(("O".equals(s(e, "prev_tipo")) ? "Ordine " : "Prev. ") + i(e, "prev_numero") + "/" + i(e, "prev_anno"));
            if (!s(e, "nota").isEmpty()) note.add(s(e, "nota"));
            riga(new Object[]{i(e, "numero_identificativo"), s(e, "nome_elemento_cant"), s(e, "codice_elemento"), "", "", "", "", "",
                    unisci(note)}, sElemento);

            List<ContentValues> componenti = componenti(db, id);
            Set<Integer> idComp = new HashSet<>();
            for (ContentValues c : componenti) idComp.add(i(c, "id_componente_cant"));
            // collegamenti: quelli di un componente vanno sulla sua riga, gli altri sotto l'elemento
            Map<Integer, List<ContentValues>> diComp = new HashMap<>();
            for (ContentValues k : collegamenti(db, id)) {
                int mio = i(k, "id_elemento_cant1") == id ? i(k, "id_componente_cant1") : i(k, "id_componente_cant2");
                if (mio != 0 && idComp.contains(mio)) {
                    List<ContentValues> l = diComp.get(mio);
                    if (l == null) diComp.put(mio, l = new ArrayList<>());
                    l.add(k);
                } else {
                    riga(concat(new Object[]{"", "", ""}, collegamento(k, id), new Object[]{""}), sDettaglio);
                }
            }
            for (ContentValues c : componenti) {
                int idc = i(c, "id_componente_cant");
                String pos = s(c, "moduli_occupati_cant").trim();
                List<ContentValues> kk = diComp.containsKey(idc) ? new ArrayList<>(diComp.get(idc)) : new ArrayList<ContentValues>();
                ContentValues primo = kk.isEmpty() ? null : kk.remove(0);
                riga(concat(new Object[]{pos.isEmpty() ? "" : "Pos. " + pos, s(c, "nome_componente_cant"), s(c, "codice")},
                        primo != null ? collegamento(primo, id) : new Object[]{"", "", "", "", ""}, new Object[]{s(c, "nota")}), sComponente);
                for (ContentValues k : kk) riga(concat(new Object[]{"", "", ""}, collegamento(k, id), new Object[]{""}), sDettaglio);
                for (ContentValues sc : composti(db, idc)) {
                    riga(new Object[]{"", "– " + s(sc, "nome_componente_cant"), s(sc, "codice"), "", "", "", "", "", ""}, sDettaglio);
                }
            }
            // materiale aggiunto all'elemento in Componi (schema 47, STUDIO_DESIGN_GRAFICA.md §10)
            for (ContentValues m : com.ncfsistemi.econ.db.table.ElementiCantiereMateriali.diElemento(db, id)) {
                Double q = m.getAsDouble("quantita");
                String qt = q == null ? "0" : (q == Math.rint(q) ? String.valueOf(q.longValue())
                        : String.format(java.util.Locale.ITALY, "%.3f", q).replaceAll("0+$", ""));
                List<String> nm = new ArrayList<>();
                nm.add("q.tà " + qt + (s(m, "um").isEmpty() ? "" : " " + s(m, "um")));
                if (!s(m, "nota").isEmpty()) nm.add(s(m, "nota"));
                riga(new Object[]{"Mat.", s(m, "descrizione").isEmpty() ? s(m, "codice_articolo") : s(m, "descrizione"), s(m, "codice_articolo"),
                        "", "", "", "", "", unisci(nm)}, sComponente);
            }
            r++; // riga vuota tra un elemento e l'altro
        }
    }

    /** Collegato a, componente, locale, collegamento, metri: visti dall'elemento id. */
    private static Object[] collegamento(ContentValues k, int id) {
        String s = i(k, "id_elemento_cant1") == id ? "2" : "1";
        List<String> tipo = new ArrayList<>();
        if (!s(k, "cavo").isEmpty()) tipo.add("Cavo: " + s(k, "cavo") + (i(k, "qta_cavo") > 1 ? " ×" + i(k, "qta_cavo") : ""));
        if (!s(k, "tubo").isEmpty()) tipo.add("Tubo: " + s(k, "tubo"));
        if (!s(k, "nota").isEmpty()) tipo.add("(" + s(k, "nota") + ")");
        Double metri = k.getAsDouble("metri");
        return new Object[]{i(k, "num" + s) + " - " + s(k, "nome" + s), s(k, "comp" + s), s(k, "locale" + s),
                android.text.TextUtils.join(" ", tipo), metri == null ? 0d : metri};
    }

    // ------------------------------------------------------------------ dati

    private static final String CODICE_COMPONENTE = "(select min(ec.codice_articolo) from elementi_codici ec"
            + " where ec.id_componente=cc.id_componente and ec.id_elemento=0) as codice";

    private List<ContentValues> elementi(DbInterno db, int idLocale) {
        return lista(db.eseguiSelect("select e.*, li.nome_linea, pl.nome_placca, p.numero as prev_numero, p.anno as prev_anno, p.tipo as prev_tipo,"
                + " (select min(ec.codice_articolo) from elementi_codici ec where ec.id_elemento=e.id_elemento and ec.id_componente=0"
                + " and ec.id_placca_moduli=0) as codice_elemento"
                + " from elementi_cantiere e left join linee li on li.id_linea=e.id_linea left join placche pl on pl.id_placca=e.id_placca"
                + " left join preventivi p on p.id_preventivo=e.id_preventivo"
                + " where e.id_locale=" + idLocale + " order by e.numero_identificativo, e.id_elemento_cant", null));
    }

    private List<ContentValues> componenti(DbInterno db, int idElementoCant) {
        return lista(db.eseguiSelect("select x.moduli_occupati_cant, x.posizione_iniziale, cc.id_componente_cant, cc.nome_componente_cant, cc.nota, "
                + CODICE_COMPONENTE + " from composizioni_cantiere x inner join componenti_cantiere cc on cc.id_componente_cant=x.id_componente_cant"
                + " where x.id_elemento_cant=" + idElementoCant + " order by x.posizione_iniziale, cc.id_componente_cant", null));
    }

    private List<ContentValues> composti(DbInterno db, int idComponenteCant) {
        return lista(db.eseguiSelect("select cc.nome_componente_cant, " + CODICE_COMPONENTE + " from componenti_cant_composti p"
                + " inner join componenti_cantiere cc on cc.id_componente_cant=p.id_componente_cant_figlio"
                + " where p.id_componente_cant_padre=" + idComponenteCant + " order by cc.id_componente_cant", null));
    }

    private List<ContentValues> collegamenti(DbInterno db, int idElementoCant) {
        return lista(db.eseguiSelect("select k.*, e1.numero_identificativo as num1, e1.nome_elemento_cant as nome1, l1.nome as locale1,"
                + " c1.nome_componente_cant as comp1, e2.numero_identificativo as num2, e2.nome_elemento_cant as nome2, l2.nome as locale2,"
                + " c2.nome_componente_cant as comp2, cv.nome_elemento as cavo, tb.nome_elemento as tubo from collegamenti k"
                + " inner join elementi_cantiere e1 on e1.id_elemento_cant=k.id_elemento_cant1 left join locali l1 on l1.id_locale=e1.id_locale"
                + " inner join elementi_cantiere e2 on e2.id_elemento_cant=k.id_elemento_cant2 left join locali l2 on l2.id_locale=e2.id_locale"
                + " left join componenti_cantiere c1 on c1.id_componente_cant=k.id_componente_cant1"
                + " left join componenti_cantiere c2 on c2.id_componente_cant=k.id_componente_cant2"
                + " left join elementi cv on cv.id_elemento=k.id_cavo and k.id_cavo<>0"
                + " left join elementi tb on tb.id_elemento=k.id_tubo and k.id_tubo<>0"
                + " where k.id_elemento_cant1=" + idElementoCant + " or k.id_elemento_cant2=" + idElementoCant
                + " order by k.id_collegamento", null));
    }

    // ------------------------------------------------------------------ celle e stili

    private void riga(Object[] valori, HSSFCellStyle stile) {
        for (int i = 0; i < valori.length; i++) {
            Object v = valori[i];
            if (i == 7 && v instanceof Double) {
                HSSFCell c = cella(r, T + i, null, sMetri);
                c.setCellValue((Double) v);
            } else if (v instanceof Integer) {
                HSSFCell c = cella(r, T + i, null, stile);
                c.setCellValue((Integer) v);
            } else {
                cella(r, T + i, v == null ? "" : v.toString(), stile);
            }
        }
        r++;
    }

    private HSSFCell cella(int riga, int col, String testo, HSSFCellStyle stile) {
        HSSFRow row = f.getRow(riga);
        if (row == null) row = f.createRow(riga);
        HSSFCell c = row.createCell(col);
        c.setCellStyle(stile);
        if (testo != null) c.setCellValue(testo);
        return c;
    }

    private void stili() {
        HSSFPalette pal = wb.getCustomPalette();
        // stessi colori del portale: blu elemento, grigio componente, blu scuro intestazione
        pal.setColorAtIndex(HSSFColor.LIGHT_BLUE.index, (byte) 0x5B, (byte) 0x9B, (byte) 0xD5);
        pal.setColorAtIndex(HSSFColor.GREY_25_PERCENT.index, (byte) 0xEC, (byte) 0xEC, (byte) 0xEC);
        pal.setColorAtIndex(HSSFColor.DARK_BLUE.index, (byte) 0x4A, (byte) 0x67, (byte) 0x85);
        pal.setColorAtIndex(HSSFColor.GREY_50_PERCENT.index, (byte) 0x9E, (byte) 0x9E, (byte) 0x9E);

        HSSFFont titolo = wb.createFont();
        titolo.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        titolo.setFontHeightInPoints((short) 13);
        titolo.setColor(HSSFColor.DARK_BLUE.index);
        HSSFFont biancoGrassetto = wb.createFont();
        biancoGrassetto.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        biancoGrassetto.setFontHeightInPoints((short) 10);
        biancoGrassetto.setColor(HSSFColor.WHITE.index);
        HSSFFont corsivo = wb.createFont();
        corsivo.setItalic(true);
        corsivo.setFontHeightInPoints((short) 9);

        sTitolo = wb.createCellStyle();
        sTitolo.setFont(titolo);
        sNormale = wb.createCellStyle();
        sIntestazione = pieno(HSSFColor.DARK_BLUE.index, biancoGrassetto);
        sElemento = pieno(HSSFColor.LIGHT_BLUE.index, biancoGrassetto);
        sComponente = pieno(HSSFColor.GREY_25_PERCENT.index, null);
        sDettaglio = bordato(wb.createCellStyle());
        sDettaglio.setFont(corsivo);
        sMetri = bordato(wb.createCellStyle());
        sMetri.setFont(corsivo);
        sMetri.setDataFormat(wb.createDataFormat().getFormat("0.00"));
    }

    private HSSFCellStyle pieno(short colore, HSSFFont font) {
        HSSFCellStyle s = bordato(wb.createCellStyle());
        s.setFillForegroundColor(colore);
        s.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        if (font != null) s.setFont(font);
        return s;
    }

    private static HSSFCellStyle bordato(HSSFCellStyle s) {
        s.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        s.setBorderRight(HSSFCellStyle.BORDER_THIN);
        s.setBorderTop(HSSFCellStyle.BORDER_THIN);
        s.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        s.setLeftBorderColor(HSSFColor.GREY_50_PERCENT.index);
        s.setRightBorderColor(HSSFColor.GREY_50_PERCENT.index);
        s.setTopBorderColor(HSSFColor.GREY_50_PERCENT.index);
        s.setBottomBorderColor(HSSFColor.GREY_50_PERCENT.index);
        s.setVerticalAlignment(HSSFCellStyle.VERTICAL_TOP);
        s.setWrapText(true);
        return s;
    }

    // ------------------------------------------------------------------ utilità

    /** Nome di foglio valido e unico (max 31 caratteri, senza []:*?/\). */
    /** -1 scantinato, 0 piano terra, 1 piano 1... (come il portale, ConformitaExcel::nomeLivello). */
    public static String nomeLivello(int n) {
        if (n < -1) return "interrato " + n;
        if (n == -1) return "scantinato";
        if (n == 0) return "piano terra";
        return "piano " + n;
    }

    private static String nomeFoglio(String nome, Set<String> usati) {
        String base = (nome == null ? "" : nome).replaceAll("[\\[\\]:*?/\\\\]+", " ").trim();
        if (base.isEmpty()) base = "Locale";
        if (base.length() > 31) base = base.substring(0, 31);
        String n = base;
        for (int i = 2; usati.contains(n.toLowerCase(Locale.ROOT)); i++) {
            String suff = " (" + i + ")";
            n = base.substring(0, Math.min(base.length(), 31 - suff.length())) + suff;
        }
        usati.add(n.toLowerCase(Locale.ROOT));
        return n;
    }

    private static Object[] concat(Object[]... parti) {
        List<Object> out = new ArrayList<>();
        for (Object[] p : parti) for (Object o : p) out.add(o);
        return out.toArray();
    }

    private static String unisci(List<String> parti) {
        return android.text.TextUtils.join(" · ", parti);
    }

    private static List<ContentValues> lista(ArrayList<Object> righe) {
        List<ContentValues> out = new ArrayList<>();
        if (righe != null) for (Object o : righe) out.add((ContentValues) o);
        return out;
    }

    private static String s(ContentValues v, String k) {
        String x = v.getAsString(k);
        return x == null || x.equals("null") ? "" : x;
    }

    private static int i(ContentValues v, String k) {
        try {
            Integer x = v.getAsInteger(k);
            return x == null ? 0 : x;
        } catch (Exception e) {
            return 0;
        }
    }
}
