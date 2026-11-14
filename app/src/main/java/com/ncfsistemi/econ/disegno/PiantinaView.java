package com.ncfsistemi.econ.disegno;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;

import com.ncfsistemi.econ.utils.SimboliDisegno;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Piantina del locale disegnata su Canvas (schermata Disegno, STUDIO_DESIGN_GRAFICA.md §8): pavimento a quadretti,
 * pareti sui bordi della stanza, porte e finestre, elementi col simbolo e il numero sotto. Zoom con due dita,
 * trascinamento per spostare la vista; cosa fa un tocco dipende dal modo scelto nella barra in alto.
 * Coordinate degli elementi come nella piantina classica e nel portale: 0..200 = angolo in alto a sinistra
 * dell'elemento, che nella piantina classica è grande lato/16.
 */
public class PiantinaView extends View {

    public static final int GUARDA = 0, POSA = 1, SPOSTA = 2, COLLEGA = 3, PARETI = 4;

    public interface Ascoltatore {
        void toccoElemento(DisegnoDati.Posato p);

        void toccoVuoto(int x, int y);

        void spostato(DisegnoDati.Posato p, int x, int y);

        /** Modo Pareti: tocco su un lato con parete (apertura = quella già presente lì, o null). */
        void toccoParete(int cella, String lato, int perc, DisegnoDati.Apertura apertura);

        /** Modo Pareti: tocco al centro di un quadretto. */
        void toccoCella(int cella);
    }

    private static final int PAVIMENTO = Color.WHITE;
    private static final int FUORI = Color.parseColor("#EEF1F5");
    private static final int GRIGLIA = Color.parseColor("#E3E8EF");
    private static final int PARETE = Color.parseColor("#37474F");
    private static final int FINESTRA = Color.parseColor("#1E88E5");
    private static final int SELEZIONE = Color.parseColor("#FB8C00");
    private static final int TESTO = Color.parseColor("#263238");

    private DisegnoDati.Locale locale;
    private List<DisegnoDati.Posato> elementi = new ArrayList<>();
    private List<int[]> linee = new ArrayList<>();
    private final Map<Integer, SimboliDisegno.Simbolo> simboli = new HashMap<>();
    private int selezionato = 0;
    private int modo = GUARDA;
    private Ascoltatore ascoltatore;

    private float scala = 1f, panX = 0f, panY = 0f;
    /** true finché l'utente non zooma o sposta: la piantina resta adattata alla stanza anche se cambia la misura. */
    private boolean adattata = true;
    private final ScaleGestureDetector scaleDetector;
    private final float slop;
    private final float dp;

    private final Paint pPavimento = new Paint();
    private final Paint pGriglia = new Paint();
    private final Paint pParete = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pPorta = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pFinestra = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSelezione = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pPill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pPillBordo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pNumero = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBadge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBadgeTesto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pCavo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pTubo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pCellaPareti = new Paint(Paint.ANTI_ALIAS_FLAG);

    // tocco
    private float downX, downY;
    private boolean mosso, scalato, multiTocco;
    private DisegnoDati.Posato trascinato;
    private float trascDx, trascDy;
    private float ultimoX, ultimoY;

    public PiantinaView(Context ctx) {
        this(ctx, null);
    }

    public PiantinaView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        slop = ViewConfiguration.get(ctx).getScaledTouchSlop();
        pPavimento.setColor(PAVIMENTO);
        pGriglia.setColor(GRIGLIA);
        pGriglia.setStrokeWidth(Math.max(1f, dp));
        pParete.setColor(PARETE);
        pParete.setStrokeCap(Paint.Cap.SQUARE);
        pPorta.setColor(PARETE);
        pPorta.setStyle(Paint.Style.STROKE);
        pFinestra.setColor(FINESTRA);
        pFinestra.setStyle(Paint.Style.STROKE);
        pSelezione.setColor(SELEZIONE);
        pSelezione.setStyle(Paint.Style.STROKE);
        pPill.setColor(Color.WHITE);
        pPillBordo.setColor(Color.parseColor("#B0BEC5"));
        pPillBordo.setStyle(Paint.Style.STROKE);
        pPillBordo.setStrokeWidth(dp);
        pNumero.setColor(TESTO);
        pNumero.setTextAlign(Paint.Align.CENTER);
        pNumero.setFakeBoldText(true);
        pBadge.setColor(Color.parseColor("#1565C0"));
        pBadgeTesto.setColor(Color.WHITE);
        pBadgeTesto.setTextAlign(Paint.Align.CENTER);
        pBadgeTesto.setFakeBoldText(true);
        pCavo.setColor(Color.parseColor("#1565C0"));
        pCavo.setStyle(Paint.Style.STROKE);
        pCavo.setStrokeWidth(2.5f * dp);
        pTubo.setColor(Color.parseColor("#78909C"));
        pTubo.setStyle(Paint.Style.STROKE);
        pTubo.setStrokeWidth(2.5f * dp);
        pTubo.setPathEffect(new DashPathEffect(new float[]{8 * dp, 6 * dp}, 0));
        pCellaPareti.setColor(Color.parseColor("#331565C0"));

        scaleDetector = new ScaleGestureDetector(ctx, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                float nuova = Math.max(0.6f, Math.min(4f, scala * d.getScaleFactor()));
                float f = nuova / scala;
                // zoom attorno al punto tra le dita
                float cx = d.getFocusX() - getWidth() / 2f, cy = d.getFocusY() - getHeight() / 2f;
                panX = cx - (cx - panX) * f;
                panY = cy - (cy - panY) * f;
                scala = nuova;
                scalato = true;
                adattata = false;
                invalidate();
                return true;
            }
        });
    }

    // ------------------------------------------------------------------ dati

    public void setDati(DisegnoDati.Locale locale, List<DisegnoDati.Posato> elementi, List<int[]> linee) {
        boolean nuovoLocale = locale != null && (this.locale == null || this.locale.id != locale.id);
        this.locale = locale;
        this.elementi = elementi == null ? new ArrayList<>() : elementi;
        this.linee = linee == null ? new ArrayList<>() : linee;
        simboli.clear();
        if (nuovoLocale || adattata) adatta();
        else invalidate();
    }

    public void setAscoltatore(Ascoltatore a) {
        ascoltatore = a;
    }

    public void setModo(int modo) {
        this.modo = modo;
        invalidate();
    }

    public void setSelezionato(int idElementoCant) {
        selezionato = idElementoCant;
        invalidate();
    }

    public void zoom(float fattore) {
        float nuova = Math.max(0.6f, Math.min(4f, scala * fattore));
        float f = nuova / scala;
        scala = nuova;
        panX *= f;
        panY *= f;
        adattata = false;
        invalidate();
    }

    /**
     * Inquadra la stanza (quadretti pieni ed elementi posati), non tutta la griglia: zoom e centro calcolati sulla misura
     * attuale della vista. Se la vista non è ancora misurata si rifà in onSizeChanged.
     */
    public void adatta() {
        adattata = true;
        scala = 1f;
        panX = panY = 0f;
        if (locale == null || getWidth() == 0 || getHeight() == 0) {
            invalidate();
            return;
        }
        int n = locale.n;
        float minX = 1f, minY = 1f, maxX = 0f, maxY = 0f;
        boolean trovato = false;
        for (int r = 0; r < n; r++) {
            for (int k = 0; k < n; k++) {
                if (!pieno(r, k)) continue;
                trovato = true;
                minX = Math.min(minX, k / (float) n);
                maxX = Math.max(maxX, (k + 1) / (float) n);
                minY = Math.min(minY, r / (float) n);
                maxY = Math.max(maxY, (r + 1) / (float) n);
            }
        }
        for (DisegnoDati.Posato p : elementi) {
            // centro dell'elemento in frazione del lato, con un margine di mezzo cerchio
            float cx = p.x / 200f + 1f / 32f, cy = p.y / 200f + 1f / 32f, m = 1f / 24f;
            trovato = true;
            minX = Math.min(minX, cx - m);
            maxX = Math.max(maxX, cx + m);
            minY = Math.min(minY, cy - m);
            maxY = Math.max(maxY, cy + m);
        }
        if (!trovato) {
            invalidate();
            return;
        }
        float base = lato(); // con scala 1
        float spazioW = getWidth() - 32 * dp, spazioH = getHeight() - 32 * dp;
        float s = Math.min(spazioW / Math.max(0.05f, (maxX - minX) * base), spazioH / Math.max(0.05f, (maxY - minY) * base));
        scala = Math.max(0.6f, Math.min(4f, s));
        float L = lato();
        panX = L * (0.5f - (minX + maxX) / 2f);
        panY = L * (0.5f - (minY + maxY) / 2f);
        invalidate();
    }

    /** Tutta la griglia del locale, senza zoom (per le immagini dell'Excel di conformità, come sul portale). */
    public void inquadraTutto() {
        adattata = false;
        scala = 1f;
        panX = panY = 0f;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // pannelli aperti/chiusi o schermo intero: se l'utente non ha zoomato, si reinquadra la stanza
        if (adattata) adatta();
    }

    // ------------------------------------------------------------------ geometria

    private float lato() {
        float base = Math.min(getWidth(), getHeight()) - 32 * dp;
        return Math.max(50f, base) * scala;
    }

    private float sinistra() {
        return (getWidth() - lato()) / 2f + panX;
    }

    private float alto() {
        return (getHeight() - lato()) / 2f + panY;
    }

    /** Lato dell'elemento nella piantina classica: serve per tenere le stesse posizioni. */
    private float latoClassico() {
        return lato() / 16f;
    }

    /** Diametro del cerchio disegnato: più grande della piantina classica, mai sotto i 30dp. */
    private float diametro() {
        return Math.max(30 * dp * Math.min(1f, scala), lato() / 12f);
    }

    private float centroX(DisegnoDati.Posato p) {
        return sinistra() + p.x / 200f * lato() + latoClassico() / 2f;
    }

    private float centroY(DisegnoDati.Posato p) {
        return alto() + p.y / 200f * lato() + latoClassico() / 2f;
    }

    private int aPosX(float cx) {
        return Math.round(Math.max(0, Math.min(200 - 200f / 16f, (cx - sinistra() - latoClassico() / 2f) / lato() * 200f)));
    }

    private int aPosY(float cy) {
        return Math.round(Math.max(0, Math.min(200 - 200f / 16f, (cy - alto() - latoClassico() / 2f) / lato() * 200f)));
    }

    private boolean pieno(int r, int c) {
        if (locale == null || r < 0 || c < 0 || r >= locale.n || c >= locale.n) return false;
        return locale.celle[r * locale.n + c];
    }

    // ------------------------------------------------------------------ disegno

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(FUORI);
        if (locale == null) return;
        int n = locale.n;
        float L = lato(), x0 = sinistra(), y0 = alto(), c = L / n;

        // pavimento e quadretti
        for (int r = 0; r < n; r++) {
            for (int k = 0; k < n; k++) {
                if (!pieno(r, k)) continue;
                canvas.drawRect(x0 + k * c, y0 + r * c, x0 + (k + 1) * c, y0 + (r + 1) * c, pPavimento);
            }
        }
        for (int r = 0; r < n; r++) {
            for (int k = 0; k < n; k++) {
                if (!pieno(r, k)) {
                    if (modo == PARETI) {
                        // quadretti fuori dalla stanza tratteggiati: si toccano per aggiungerli
                        Paint pt = new Paint(pGriglia);
                        pt.setStyle(Paint.Style.STROKE);
                        pt.setPathEffect(new DashPathEffect(new float[]{4 * dp, 4 * dp}, 0));
                        canvas.drawRect(x0 + k * c + 2 * dp, y0 + r * c + 2 * dp, x0 + (k + 1) * c - 2 * dp, y0 + (r + 1) * c - 2 * dp, pt);
                    }
                    continue;
                }
                if (pieno(r, k + 1)) canvas.drawLine(x0 + (k + 1) * c, y0 + r * c, x0 + (k + 1) * c, y0 + (r + 1) * c, pGriglia);
                if (pieno(r + 1, k)) canvas.drawLine(x0 + k * c, y0 + (r + 1) * c, x0 + (k + 1) * c, y0 + (r + 1) * c, pGriglia);
            }
        }

        // pareti
        float spessore = Math.max(3 * dp, L / 60f);
        pParete.setStrokeWidth(spessore);
        for (int r = 0; r < n; r++) {
            for (int k = 0; k < n; k++) {
                if (!pieno(r, k)) continue;
                float l = x0 + k * c, t = y0 + r * c;
                if (!pieno(r - 1, k)) canvas.drawLine(l, t, l + c, t, pParete);
                if (!pieno(r + 1, k)) canvas.drawLine(l, t + c, l + c, t + c, pParete);
                if (!pieno(r, k - 1)) canvas.drawLine(l, t, l, t + c, pParete);
                if (!pieno(r, k + 1)) canvas.drawLine(l + c, t, l + c, t + c, pParete);
            }
        }

        // porte e finestre
        for (DisegnoDati.Apertura a : locale.aperture) disegnaApertura(canvas, a, x0, y0, c, L, spessore);

        // collegamenti (modo Collega)
        if (modo == COLLEGA) {
            Map<Integer, DisegnoDati.Posato> perId = new HashMap<>();
            for (DisegnoDati.Posato p : elementi) perId.put(p.id, p);
            for (int[] li : linee) {
                DisegnoDati.Posato a = perId.get(li[0]), b = perId.get(li[1]);
                if (a == null || b == null) continue;
                canvas.drawLine(centroX(a), centroY(a), centroX(b), centroY(b), li[2] == 1 ? pCavo : pTubo);
            }
        }

        // elementi
        float D = diametro();
        pNumero.setTextSize(Math.max(10 * dp, D * 0.30f));
        pBadgeTesto.setTextSize(Math.max(8 * dp, D * 0.24f));
        for (DisegnoDati.Posato p : elementi) {
            if (p == trascinato) continue;
            disegnaElemento(canvas, p, centroX(p), centroY(p), D);
        }
        if (trascinato != null) disegnaElemento(canvas, trascinato, ultimoX + trascDx, ultimoY + trascDy, D * 1.15f);
    }

    private void disegnaApertura(Canvas canvas, DisegnoDati.Apertura a, float x0, float y0, float c, float L, float spessore) {
        int r = a.cella / locale.n, k = a.cella % locale.n;
        float l = x0 + k * c, t = y0 + r * c;
        boolean porta = "P".equalsIgnoreCase(a.tipo);
        float lung = porta ? L / 7f : L / 6f;
        float inizio = c * Math.max(0, a.perc) / 100f;
        float ax, ay, bx, by;         // estremi sull'asse della parete
        float nx = 0, ny = 0;         // verso l'interno della stanza
        switch (a.lato == null ? "T" : a.lato.toUpperCase()) {
            case "B":
                ax = l + inizio; ay = t + c; bx = ax + lung; by = ay; ny = -1;
                break;
            case "L":
                ax = l; ay = t + inizio; bx = ax; by = ay + lung; nx = 1;
                break;
            case "R":
                ax = l + c; ay = t + inizio; bx = ax; by = ay + lung; nx = -1;
                break;
            default:
                ax = l + inizio; ay = t; bx = ax + lung; by = ay; ny = 1;
        }
        // varco nella parete
        Paint varco = new Paint(Paint.ANTI_ALIAS_FLAG);
        varco.setColor(PAVIMENTO);
        varco.setStrokeWidth(spessore + 2 * dp);
        canvas.drawLine(ax, ay, bx, by, varco);
        if (porta) {
            pPorta.setStrokeWidth(Math.max(1.5f * dp, spessore / 2.5f));
            // anta aperta verso l'interno e arco di apertura
            float px = ax + nx * lung, py = ay + ny * lung;
            canvas.drawLine(ax, ay, px, py, pPorta);
            RectF ov = new RectF(ax - lung, ay - lung, ax + lung, ay + lung);
            float da, sweep;
            if (ny == 1) { da = 0; sweep = 90; }
            else if (ny == -1) { da = 270; sweep = 90; }
            else if (nx == 1) { da = 0; sweep = 90; }
            else { da = 90; sweep = 90; }
            Paint arco = new Paint(pPorta);
            arco.setStrokeWidth(Math.max(1f, dp));
            arco.setPathEffect(new DashPathEffect(new float[]{4 * dp, 3 * dp}, 0));
            canvas.drawArc(ov, da, sweep, false, arco);
        } else {
            pFinestra.setStrokeWidth(Math.max(1.5f * dp, spessore / 3f));
            float o = spessore / 2.2f;
            float ox = ny != 0 ? 0 : o, oy = ny != 0 ? o : 0;
            canvas.drawLine(ax - ox, ay - oy, bx - ox, by - oy, pFinestra);
            canvas.drawLine(ax + ox, ay + oy, bx + ox, by + oy, pFinestra);
            canvas.drawLine(ax, ay, bx, by, pFinestra);
        }
    }

    private void disegnaElemento(Canvas canvas, DisegnoDati.Posato p, float cx, float cy, float D) {
        SimboliDisegno.Simbolo s = simboli.get(p.id);
        if (s == null) {
            SimboliDisegno.Voce v = p.idElemento > 0 ? SimboliDisegno.elemento(p.idElemento)
                    : SimboliDisegno.categoriaElementi(p.idCategoria);
            s = new SimboliDisegno.Simbolo(getContext(), v);
            s.setStile(p.altroOrdine ? SimboliDisegno.Simbolo.ALTRO_ORDINE
                    : p.idPreventivo == 0 ? SimboliDisegno.Simbolo.SOLO_CANTIERE : SimboliDisegno.Simbolo.NEL_DOCUMENTO);
            simboli.put(p.id, s);
        }
        float r = D / 2f;
        if (p.id == selezionato) {
            Paint alone = new Paint(Paint.ANTI_ALIAS_FLAG);
            alone.setColor(Color.argb(60, 251, 140, 0));
            canvas.drawCircle(cx, cy, r + 7 * dp, alone);
            pSelezione.setStrokeWidth(3 * dp);
            canvas.drawCircle(cx, cy, r + 4 * dp, pSelezione);
        }
        s.setBounds(Math.round(cx - r), Math.round(cy - r), Math.round(cx + r), Math.round(cy + r));
        s.draw(canvas);

        // numero in una pillola sotto il cerchio
        String num = p.numero;
        float tw = pNumero.measureText(num);
        Paint.FontMetrics fm = pNumero.getFontMetrics();
        float th = fm.descent - fm.ascent;
        float pw = tw + 10 * dp, ph = th + 2 * dp;
        RectF pill = new RectF(cx - pw / 2f, cy + r + 2 * dp, cx + pw / 2f, cy + r + 2 * dp + ph);
        canvas.drawRoundRect(pill, ph / 2f, ph / 2f, pPill);
        canvas.drawRoundRect(pill, ph / 2f, ph / 2f, pPillBordo);
        canvas.drawText(num, cx, pill.centerY() - (fm.ascent + fm.descent) / 2f, pNumero);

        // collegamenti: pallino col numero in alto a destra
        int coll = p.cavi + p.tubi;
        if (coll > 0) {
            float br = Math.max(7 * dp, D * 0.2f);
            float bx = cx + r * 0.78f, by = cy - r * 0.78f;
            canvas.drawCircle(bx, by, br, pBadge);
            Paint.FontMetrics bf = pBadgeTesto.getFontMetrics();
            canvas.drawText(String.valueOf(coll), bx, by - (bf.ascent + bf.descent) / 2f, pBadgeTesto);
        }
    }

    // ------------------------------------------------------------------ tocco

    private DisegnoDati.Posato elementoIn(float x, float y) {
        float raggio = diametro() / 2f + 10 * dp;
        DisegnoDati.Posato migliore = null;
        float dmin = Float.MAX_VALUE;
        for (DisegnoDati.Posato p : elementi) {
            float dx = centroX(p) - x, dy = centroY(p) - y;
            float d = dx * dx + dy * dy;
            if (d <= raggio * raggio && d < dmin) {
                dmin = d;
                migliore = p;
            }
        }
        return migliore;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ultimoX = e.getX();
                downY = ultimoY = e.getY();
                mosso = scalato = multiTocco = false;
                trascinato = null;
                if (modo == SPOSTA) {
                    DisegnoDati.Posato p = elementoIn(downX, downY);
                    if (p != null) {
                        trascinato = p;
                        trascDx = centroX(p) - downX;
                        trascDy = centroY(p) - downY;
                        selezionato = p.id;
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                multiTocco = true;
                trascinato = null;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE: {
                float x = e.getX(), y = e.getY();
                if (!mosso && (Math.abs(x - downX) > slop || Math.abs(y - downY) > slop)) mosso = true;
                if (trascinato != null && !multiTocco) {
                    ultimoX = x;
                    ultimoY = y;
                    invalidate();
                } else if (mosso && !scaleDetector.isInProgress() && e.getPointerCount() == 1 && !multiTocco) {
                    panX += x - ultimoX;
                    panY += y - ultimoY;
                    adattata = false;
                    ultimoX = x;
                    ultimoY = y;
                    invalidate();
                } else {
                    ultimoX = x;
                    ultimoY = y;
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                DisegnoDati.Posato t = trascinato;
                trascinato = null;
                if (t != null) {
                    if (mosso) {
                        int nx = aPosX(e.getX() + trascDx), ny = aPosY(e.getY() + trascDy);
                        t.x = nx;
                        t.y = ny;
                        invalidate();
                        if (ascoltatore != null) ascoltatore.spostato(t, nx, ny);
                    } else if (ascoltatore != null) {
                        invalidate();
                        ascoltatore.toccoElemento(t);
                    }
                    return true;
                }
                if (!mosso && !scalato && !multiTocco) tocco(e.getX(), e.getY());
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                trascinato = null;
                invalidate();
                return true;
        }
        return true;
    }

    private void tocco(float x, float y) {
        if (ascoltatore == null || locale == null) return;
        if (modo == PARETI) {
            toccoPareti(x, y);
            return;
        }
        DisegnoDati.Posato p = elementoIn(x, y);
        if (p != null) {
            ascoltatore.toccoElemento(p);
            return;
        }
        float L = lato();
        if (x < sinistra() || y < alto() || x > sinistra() + L || y > alto() + L) {
            ascoltatore.toccoElemento(null);
            return;
        }
        ascoltatore.toccoVuoto(aPosX(x), aPosY(y));
    }

    private void toccoPareti(float x, float y) {
        int n = locale.n;
        float c = lato() / n;
        float fx = (x - sinistra()) / c, fy = (y - alto()) / c;
        if (fx < -0.3f || fy < -0.3f || fx > n + 0.3f || fy > n + 0.3f) return;
        // lato più vicino che ha una parete, entro un terzo di quadretto
        float soglia = 0.28f;
        int migliorCella = -1;
        String migliorLato = null;
        float migliorD = soglia, migliorPos = 0;
        for (int r = Math.max(0, (int) fy - 1); r <= Math.min(n - 1, (int) fy + 1); r++) {
            for (int k = Math.max(0, (int) fx - 1); k <= Math.min(n - 1, (int) fx + 1); k++) {
                if (!pieno(r, k)) continue;
                float lx = fx - k, ly = fy - r;
                if (lx < -soglia || lx > 1 + soglia || ly < -soglia || ly > 1 + soglia) continue;
                float[][] lati = {
                        {pieno(r - 1, k) ? -1 : Math.abs(ly), lx},       // T
                        {pieno(r + 1, k) ? -1 : Math.abs(ly - 1), lx},   // B
                        {pieno(r, k - 1) ? -1 : Math.abs(lx), ly},       // L
                        {pieno(r, k + 1) ? -1 : Math.abs(lx - 1), ly}};  // R
                String[] nomi = {"T", "B", "L", "R"};
                for (int i = 0; i < 4; i++) {
                    float d = lati[i][0];
                    float lungo = lati[i][1];
                    if (d < 0 || lungo < -0.05f || lungo > 1.05f) continue;
                    if (d < migliorD) {
                        migliorD = d;
                        migliorCella = r * n + k;
                        migliorLato = nomi[i];
                        migliorPos = lungo;
                    }
                }
            }
        }
        if (migliorCella >= 0) {
            DisegnoDati.Apertura esistente = aperturaIn(x, y);
            // il tocco cade al centro dell'apertura: l'inizio è mezza apertura prima
            int perc = Math.round(Math.max(0f, Math.min(100f, migliorPos * 100f - 100f * n / 7f / 2f)));
            ascoltatore.toccoParete(migliorCella, migliorLato, perc, esistente);
            return;
        }
        DisegnoDati.Apertura esistente = aperturaIn(x, y);
        if (esistente != null) {
            ascoltatore.toccoParete(esistente.cella, esistente.lato, esistente.perc, esistente);
            return;
        }
        int k = (int) Math.floor(fx), r = (int) Math.floor(fy);
        if (k >= 0 && r >= 0 && k < n && r < n) ascoltatore.toccoCella(r * n + k);
    }

    private DisegnoDati.Apertura aperturaIn(float x, float y) {
        if (locale == null) return null;
        float L = lato(), c = L / locale.n, x0 = sinistra(), y0 = alto();
        float tolleranza = Math.max(14 * dp, c * 0.2f);
        for (DisegnoDati.Apertura a : locale.aperture) {
            int r = a.cella / locale.n, k = a.cella % locale.n;
            float l = x0 + k * c, t = y0 + r * c;
            float lung = "P".equalsIgnoreCase(a.tipo) ? L / 7f : L / 6f;
            float inizio = c * Math.max(0, a.perc) / 100f;
            RectF box;
            switch (a.lato == null ? "T" : a.lato.toUpperCase()) {
                case "B": box = new RectF(l + inizio, t + c - tolleranza, l + inizio + lung, t + c + tolleranza); break;
                case "L": box = new RectF(l - tolleranza, t + inizio, l + tolleranza, t + inizio + lung); break;
                case "R": box = new RectF(l + c - tolleranza, t + inizio, l + c + tolleranza, t + inizio + lung); break;
                default: box = new RectF(l + inizio, t - tolleranza, l + inizio + lung, t + tolleranza);
            }
            if (box.contains(x, y)) return a;
        }
        return null;
    }
}
