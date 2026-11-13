package com.ncfsistemi.econ.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.core.content.res.ResourcesCompat;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.CategorieComponenti;
import com.ncfsistemi.econ.db.table.CategorieGenerali;
import com.ncfsistemi.econ.db.table.Componenti;
import com.ncfsistemi.econ.db.table.Elementi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Simboli del disegno (STUDIO_DESIGN_GRAFICA.md §3.2, G1): un cerchio del colore della categoria con il simbolo bianco
 * (Material Symbols ridotto, res/font/simboli_disegno.ttf; nomi e codici in assets/simboli_disegno.json), al posto delle
 * foto nella piantina, nelle tavolozze e nelle composizioni. La foto resta facoltativa e si vede solo nelle schede.
 * <p>
 * Simbolo di un elemento o componente: il suo, se no quello della categoria; colore: quello della categoria. Le categorie
 * arrivano dal server con simbolo e colore (schema 46); se mancano (DB vecchi) valgono quelli di sistema del numero
 * classico (stessi valori di categorie_disegno.json del server), se no "category" grigio.
 */
public final class SimboliDisegno {

    public static final String SIMBOLO_PREDEFINITO = "category";
    public static final int COLORE_NEUTRO = Color.parseColor("#9e9e9e");

    /** Simbolo e colore di sistema per numero classico (CategorieDisegno): elementi 1-12. */
    private static final String[][] SISTEMA_ELEMENTI = {
            {"outlet", "#ee6e6e"}, {"toggle_on", "#6ed1c5"}, {"lightbulb", "#eca86f"}, {"electrical_services", "#b870ea"},
            {"check_box_outline_blank", "#71e88a"}, {"sensors", "#7388e4"}, {"cable", "#888888"}, {"conversion_path", "#888888"},
            {"gate", "#31436a"}, {"doorbell", "#518f40"}, {"router", "#e8d000"}, {"category", "#e855b8"},
    };
    /** Componenti 1-14 (scatole, portafrutti, prese, interruttori, copriscatole, ...). */
    private static final String[][] SISTEMA_COMPONENTI = {
            {"check_box_outline_blank", "#9e9e9e"}, {"view_column", "#9e9e9e"}, {"outlet", "#ee6e6e"}, {"toggle_on", "#6ed1c5"},
            {"crop_square", "#9e9e9e"}, {"lightbulb", "#eca86f"}, {"electrical_services", "#b870ea"}, {"memory", "#b870ea"},
            {"toggle_on", "#b870ea"}, {"sensors", "#7388e4"}, {"gate", "#31436a"}, {"doorbell", "#518f40"},
            {"router", "#e8d000"}, {"category", "#e855b8"},
    };

    /** Simbolo e colore pronti per il disegno. */
    public static final class Voce {
        public final String simbolo;
        public final int colore;

        Voce(String simbolo, int colore) {
            this.simbolo = simbolo;
            this.colore = colore;
        }
    }

    private static Typeface font;
    private static Map<String, String> caratteri;
    /** id → voce: categorie risolte, elementi e componenti (simbolo proprio o della categoria). */
    private static Map<Integer, Voce> catElementi, catComponenti, elementi, componenti;

    private SimboliDisegno() {}

    // ------------------------------------------------------------------ letture

    public static synchronized Voce elemento(Integer idElemento) {
        carica();
        Voce v = idElemento != null ? elementi.get(idElemento) : null;
        return v != null ? v : new Voce(SIMBOLO_PREDEFINITO, COLORE_NEUTRO);
    }

    public static synchronized Voce componente(Integer idComponente) {
        carica();
        Voce v = idComponente != null ? componenti.get(idComponente) : null;
        return v != null ? v : new Voce(SIMBOLO_PREDEFINITO, COLORE_NEUTRO);
    }

    public static synchronized Voce categoriaElementi(Integer idCategoria) {
        carica();
        Voce v = idCategoria != null ? catElementi.get(idCategoria) : null;
        return v != null ? v : sistema(SISTEMA_ELEMENTI, CategorieDisegno.classica(idCategoria));
    }

    public static synchronized Voce categoriaComponenti(Integer idCategoria) {
        carica();
        Voce v = idCategoria != null ? catComponenti.get(idCategoria) : null;
        return v != null ? v : sistema(SISTEMA_COMPONENTI, CategorieDisegno.componente(idCategoria));
    }

    /** Il carattere del font per quel simbolo (quello di "category" se non c'è). */
    public static synchronized String carattere(Context ctx, String simbolo) {
        if (caratteri == null) caricaCatalogo(ctx.getApplicationContext());
        String c = simbolo != null ? caratteri.get(simbolo) : null;
        if (c == null) c = caratteri.get(SIMBOLO_PREDEFINITO);
        return c != null ? c : "?";
    }

    public static synchronized Typeface font(Context ctx) {
        if (font == null) {
            try {
                font = ResourcesCompat.getFont(ctx.getApplicationContext(), R.font.simboli_disegno);
            } catch (Exception e) {
                font = Typeface.DEFAULT;
            }
        }
        return font;
    }

    // ------------------------------------------------------------------ disegno

    /** Cerchio pieno col simbolo, per ImageView e sfondi. */
    public static Drawable drawable(Context ctx, Voce v) {
        return new Simbolo(ctx, v);
    }

    public static Drawable drawableElemento(Context ctx, Integer idElemento) {
        return drawable(ctx, elemento(idElemento));
    }

    public static Drawable drawableComponente(Context ctx, Integer idComponente) {
        return drawable(ctx, componente(idComponente));
    }

    /** Il simbolo come immagine (per le finestre che vogliono una BitmapDrawable), lato in pixel. */
    public static android.graphics.drawable.BitmapDrawable bitmap(Context ctx, Voce v, int lato) {
        android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(lato, lato, android.graphics.Bitmap.Config.ARGB_8888);
        Drawable d = drawable(ctx, v);
        d.setBounds(0, 0, lato, lato);
        d.draw(new Canvas(b));
        return new android.graphics.drawable.BitmapDrawable(ctx.getResources(), b);
    }

    /** Riga di composizione o di libreria con "id_componente": il suo simbolo; null se la riga non ce l'ha. */
    public static Drawable drawableComponente(Context ctx, android.content.ContentValues riga) {
        Integer id = riga != null ? riga.getAsInteger(Componenti.ID_COMPONENTE) : null;
        return id != null && id > 0 ? drawableComponente(ctx, id) : null;
    }

    /**
     * Il simbolo nel disegno: cerchio del colore, simbolo bianco al centro. {@link #setStile} cambia l'aspetto per lo
     * stato dell'elemento nella piantina (nel documento, solo nel cantiere, di un altro ordine, selezionato).
     */
    public static final class Simbolo extends Drawable {
        public static final int NEL_DOCUMENTO = 0, SOLO_CANTIERE = 1, ALTRO_ORDINE = 2;

        private final Paint cerchio = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bordo = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint testo = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String carattere;
        private final int colore;
        private int stile = NEL_DOCUMENTO;
        /** Parte alta del riquadro occupata dal cerchio (1 = tutto; nella piantina sotto c'è il numero). */
        private float altezzaCerchio = 1f;

        public Simbolo(Context ctx, Voce v) {
            colore = v.colore;
            carattere = carattere(ctx, v.simbolo);
            cerchio.setStyle(Paint.Style.FILL);
            bordo.setStyle(Paint.Style.STROKE);
            testo.setTypeface(font(ctx));
            testo.setTextAlign(Paint.Align.CENTER);
            testo.setColor(Color.WHITE);
            applicaStile();
        }

        public Simbolo setStile(int stile) {
            this.stile = stile;
            applicaStile();
            invalidateSelf();
            return this;
        }

        public Simbolo setAltezzaCerchio(float frazione) {
            altezzaCerchio = Math.max(0.3f, Math.min(1f, frazione));
            invalidateSelf();
            return this;
        }

        private void applicaStile() {
            if (stile == ALTRO_ORDINE) {
                cerchio.setColor(Color.parseColor("#e0e0e0"));
                bordo.setColor(colore);
                testo.setColor(colore);
            } else {
                cerchio.setColor(colore);
                cerchio.setAlpha(stile == SOLO_CANTIERE ? 150 : 255);
                bordo.setColor(Color.parseColor("#263238"));
                testo.setColor(Color.WHITE);
            }
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float lato = Math.min(b.width(), b.height() * altezzaCerchio);
            if (lato <= 0) return;
            float cx = b.exactCenterX();
            float cy = b.top + lato / 2f;
            if (altezzaCerchio >= 1f) cy = b.exactCenterY();
            float raggio = lato / 2f;
            float tratto = Math.max(1.5f, lato / 18f);
            canvas.drawCircle(cx, cy, raggio - tratto / 2f, cerchio);
            if (stile != SOLO_CANTIERE) {
                bordo.setStrokeWidth(tratto);
                canvas.drawCircle(cx, cy, raggio - tratto / 2f, bordo);
            }
            testo.setTextSize(lato * 0.6f);
            Paint.FontMetrics fm = testo.getFontMetrics();
            canvas.drawText(carattere, cx, cy - (fm.ascent + fm.descent) / 2f, testo);
        }

        @Override
        public void setAlpha(int alpha) {
            cerchio.setAlpha(alpha);
            testo.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            cerchio.setColorFilter(cf);
            invalidateSelf();
        }

        @Override
        public int getIntrinsicWidth() {
            return 96;
        }

        @Override
        public int getIntrinsicHeight() {
            return 96;
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /** Da chiamare dopo una sync: categorie, elementi e componenti possono essere cambiati. */
    public static synchronized void azzera() {
        catElementi = catComponenti = elementi = componenti = null;
    }

    // ------------------------------------------------------------------ caricamento

    private static Voce sistema(String[][] tabella, int classica) {
        if (classica >= 1 && classica <= tabella.length) {
            return new Voce(tabella[classica - 1][0], Color.parseColor(tabella[classica - 1][1]));
        }
        return new Voce(SIMBOLO_PREDEFINITO, COLORE_NEUTRO);
    }

    private static int colore(String testo, int predefinito) {
        if (testo == null || testo.trim().isEmpty()) return predefinito;
        try {
            return Color.parseColor(testo.trim());
        } catch (IllegalArgumentException e) {
            return predefinito;
        }
    }

    private static boolean vuoto(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static void carica() {
        if (elementi != null) return;
        catElementi = new HashMap<>();
        catComponenti = new HashMap<>();
        elementi = new HashMap<>();
        componenti = new HashMap<>();
        Context ctx = com.ncfsistemi.econ.EconApplication.contesto();
        if (ctx == null) return;
        DbInterno helper = new DbInterno(ctx);
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            leggiCategorie(db, CategorieGenerali.NOME_TABELLA, CategorieGenerali.ID_CATEGORIA_GENERALE, catElementi, true);
            leggiCategorie(db, CategorieComponenti.NOME_TABELLA, CategorieComponenti.ID_CATEGORIA_COMPONENTE, catComponenti, false);
            leggiVoci(db, Elementi.NOME_TABELLA, Elementi.ID_ELEMENTO, Elementi.ID_CATEGORIA_GENERALE, catElementi, elementi, true);
            leggiVoci(db, Componenti.NOME_TABELLA, Componenti.ID_COMPONENTE, Componenti.ID_CATEGORIA_COMPONENTE, catComponenti, componenti, false);
        } catch (Exception e) {
            // DB non pronto: simboli predefiniti, si riprova dopo la prossima sync
        } finally {
            helper.close();
        }
    }

    private static void leggiCategorie(SQLiteDatabase db, String tabella, String colId, Map<Integer, Voce> out, boolean diElementi) {
        try (Cursor c = db.rawQuery("SELECT * FROM " + tabella, null)) {
            int iId = c.getColumnIndex(colId), iSim = c.getColumnIndex("simbolo"), iCol = c.getColumnIndex("colore");
            while (c.moveToNext()) {
                int id = c.getInt(iId);
                Voce sis = sistema(diElementi ? SISTEMA_ELEMENTI : SISTEMA_COMPONENTI,
                        diElementi ? CategorieDisegno.classica(id) : CategorieDisegno.componente(id));
                String simbolo = iSim >= 0 ? c.getString(iSim) : null;
                String colore = iCol >= 0 ? c.getString(iCol) : null;
                out.put(id, new Voce(vuoto(simbolo) ? sis.simbolo : simbolo.trim(), colore(colore, sis.colore)));
            }
        }
    }

    private static void leggiVoci(SQLiteDatabase db, String tabella, String colId, String colCat, Map<Integer, Voce> categorie,
                                  Map<Integer, Voce> out, boolean diElementi) {
        try (Cursor c = db.rawQuery("SELECT * FROM " + tabella, null)) {
            int iId = c.getColumnIndex(colId), iCat = c.getColumnIndex(colCat), iSim = c.getColumnIndex("simbolo");
            while (c.moveToNext()) {
                int idCat = iCat >= 0 ? c.getInt(iCat) : 0;
                Voce cat = categorie.get(idCat);
                if (cat == null) {
                    cat = sistema(diElementi ? SISTEMA_ELEMENTI : SISTEMA_COMPONENTI,
                            diElementi ? CategorieDisegno.classica(idCat) : CategorieDisegno.componente(idCat));
                }
                String simbolo = iSim >= 0 ? c.getString(iSim) : null;
                out.put(c.getInt(iId), vuoto(simbolo) ? cat : new Voce(simbolo.trim(), cat.colore));
            }
        }
    }

    private static void caricaCatalogo(Context ctx) {
        caratteri = new HashMap<>();
        try (InputStream in = ctx.getAssets().open("simboli_disegno.json")) {
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) buf.write(b, 0, n);
            JSONArray gruppi = new JSONObject(new String(buf.toByteArray(), StandardCharsets.UTF_8)).getJSONArray("gruppi");
            for (int i = 0; i < gruppi.length(); i++) {
                JSONArray simboli = gruppi.getJSONObject(i).getJSONArray("simboli");
                for (int j = 0; j < simboli.length(); j++) {
                    JSONObject s = simboli.getJSONObject(j);
                    caratteri.put(s.getString("nome"), new String(Character.toChars(Integer.parseInt(s.getString("codice"), 16))));
                }
            }
        } catch (Exception e) {
            // catalogo assente: si vedrà "?"
        }
    }
}
