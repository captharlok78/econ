package com.ncfsistemi.econ.disegno;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Forme degli elementi di disegno della piantina (STUDIO_DESIGN_GRAFICA.md §18), uguali a quelle del portale e delle
 * stampe del server (FormePiantina.php): le usano la piantina (PiantinaView, anche per PDF ed Excel) e le anteprime della
 * finestra Disegno (SceltaDisegno). L = lato della piantina in pixel, misure dell'elemento in centesimi di L.
 */
final class FormeDisegno {

    static final int PAVIMENTO = Color.WHITE;
    static final int PARETE = Color.parseColor("#37474F");
    static final int FINESTRA = Color.parseColor("#1E88E5");

    private FormeDisegno() {
    }

    static int larghezza(DisegnoDati.Apertura a) {
        if (a.elemento != null) return a.elemento.larghezza;
        return "F".equalsIgnoreCase(a.tipo) ? 17 : 14;
    }

    static int colore(String hex, int predefinito) {
        try {
            if (hex != null && hex.matches("#[0-9a-fA-F]{6}")) return Color.parseColor(hex);
        } catch (IllegalArgumentException ignorata) {
            // colore non valido
        }
        return predefinito;
    }

    /**
     * Elemento sul muro, stesse forme del portale e delle stampe (FormePiantina.php): porta, porta a due ante, scorrevole,
     * finestra, portafinestra, varco, basculante.
     */
    static void parete(Canvas canvas, DisegnoDati.Apertura a, float ax, float ay, float bx, float by, float nx, float ny,
                       float L, float spessore, float dp) {
        float lung = (float) Math.hypot(bx - ax, by - ay);
        String forma = a.elemento == null ? ("F".equalsIgnoreCase(a.tipo) ? "FINESTRA" : "PORTA") : a.elemento.forma.toUpperCase();
        boolean vetro = forma.equals("FINESTRA") || forma.equals("PORTAFINESTRA");
        int col = colore(a.elemento == null ? null : a.elemento.colore, vetro ? FINESTRA : PARETE);
        // varco nella parete
        Paint varco = new Paint(Paint.ANTI_ALIAS_FLAG);
        varco.setColor(PAVIMENTO);
        varco.setStrokeWidth(spessore + 2 * dp);
        canvas.drawLine(ax, ay, bx, by, varco);
        Paint anta = new Paint(Paint.ANTI_ALIAS_FLAG);
        anta.setStyle(Paint.Style.STROKE);
        anta.setColor(col);
        anta.setStrokeWidth(Math.max(1.5f * dp, spessore / 2.5f));
        Paint arco = new Paint(anta);
        arco.setStrokeWidth(Math.max(1f, dp));
        arco.setPathEffect(new DashPathEffect(new float[]{4 * dp, 3 * dp}, 0));
        Paint tratteggio = new Paint(arco);
        switch (forma) {
            case "PORTA":
                if (a.rotazione == 180) anta(canvas, bx, by, ax, ay, lung, nx, ny, anta, arco);
                else anta(canvas, ax, ay, bx, by, lung, nx, ny, anta, arco);
                break;
            case "PORTA_DOPPIA": {
                float mx = (ax + bx) / 2f, my = (ay + by) / 2f;
                anta(canvas, ax, ay, mx, my, lung / 2f, nx, ny, anta, arco);
                anta(canvas, bx, by, mx, my, lung / 2f, nx, ny, anta, arco);
                break;
            }
            case "PORTAFINESTRA": {
                vetro(canvas, ax, ay, bx, by, nx, ny, spessore, col, dp);
                float mx = (ax + bx) / 2f, my = (ay + by) / 2f;
                Paint antaVetro = new Paint(anta);
                antaVetro.setPathEffect(new DashPathEffect(new float[]{4 * dp, 3 * dp}, 0));
                anta(canvas, ax, ay, mx, my, lung / 2f, nx, ny, antaVetro, arco);
                anta(canvas, bx, by, mx, my, lung / 2f, nx, ny, antaVetro, arco);
                break;
            }
            case "PORTA_SCORREVOLE": {
                float d = spessore * 1.1f;
                Paint pannello = new Paint(anta);
                pannello.setStrokeWidth(Math.max(2 * dp, spessore / 2f));
                canvas.drawLine(ax + nx * d, ay + ny * d, bx + nx * d, by + ny * d, pannello);
                canvas.drawLine(ax, ay, bx, by, tratteggio);
                break;
            }
            case "FINESTRA":
                vetro(canvas, ax, ay, bx, by, nx, ny, spessore, col, dp);
                break;
            case "BASCULANTE": {
                float d = L * Math.max(1, a.elemento == null ? 6 : a.elemento.profondita) / 100f;
                Paint p = new Paint(anta);
                p.setStrokeWidth(Math.max(2 * dp, spessore / 2f));
                canvas.drawLine(ax, ay, bx, by, p);
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(ax, ay);
                path.lineTo(ax + nx * d, ay + ny * d);
                path.lineTo(bx + nx * d, by + ny * d);
                path.lineTo(bx, by);
                canvas.drawPath(path, tratteggio);
                break;
            }
            default: { // VARCO: solo l'apertura con i due stipiti
                Paint stipite = new Paint(Paint.ANTI_ALIAS_FLAG);
                stipite.setColor(PARETE);
                stipite.setStrokeWidth(Math.max(1.5f * dp, spessore / 3f));
                canvas.drawLine(ax - nx * spessore, ay - ny * spessore, ax + nx * spessore, ay + ny * spessore, stipite);
                canvas.drawLine(bx - nx * spessore, by - ny * spessore, bx + nx * spessore, by + ny * spessore, stipite);
            }
        }
    }

    /** Anta aperta verso l'interno dal cardine (h) e arco tratteggiato fino all'altro estremo (o). */
    private static void anta(Canvas canvas, float hx, float hy, float ox, float oy, float r, float nx, float ny, Paint anta, Paint arco) {
        float px = hx + nx * r, py = hy + ny * r;
        canvas.drawLine(hx, hy, px, py, anta);
        double a1 = Math.atan2(py - hy, px - hx), a2 = Math.atan2(oy - hy, ox - hx);
        double d = a2 - a1;
        while (d > Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        RectF ov = new RectF(hx - r, hy - r, hx + r, hy + r);
        canvas.drawArc(ov, (float) Math.toDegrees(a1), (float) Math.toDegrees(d), false, arco);
    }

    private static void vetro(Canvas canvas, float ax, float ay, float bx, float by, float nx, float ny, float spessore, int col, float dp) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(col);
        p.setStrokeWidth(Math.max(1.5f * dp, spessore / 3f));
        float o = spessore / 2.2f;
        for (float k : new float[]{-o, 0, o}) canvas.drawLine(ax + nx * k, ay + ny * k, bx + nx * k, by + ny * k, p);
    }

    /** Elemento dentro la stanza (rettangolo, cerchio, scala, pilastro) con la sigla, come FormePiantina.php. */
    static void pavimento(Canvas canvas, DisegnoDati.Apertura a, float cx, float cy, float w, float h, float L, float spessore, float dp) {
        String forma = a.elemento.forma.toUpperCase();
        RectF r = new RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
        Paint riempi = new Paint(Paint.ANTI_ALIAS_FLAG);
        riempi.setColor(colore(a.elemento.colore, forma.equals("PILASTRO") ? Color.parseColor("#4B5563") : Color.parseColor("#E5E7EB")));
        Paint bordo = new Paint(Paint.ANTI_ALIAS_FLAG);
        bordo.setStyle(Paint.Style.STROKE);
        bordo.setColor(forma.equals("PILASTRO") ? Color.parseColor("#1F2937") : Color.parseColor("#6B7280"));
        bordo.setStrokeWidth(Math.max(1f, spessore / 4f));
        if (forma.equals("CERCHIO")) {
            float rr = Math.min(w, h) / 2f;
            canvas.drawCircle(cx, cy, rr, riempi);
            canvas.drawCircle(cx, cy, rr, bordo);
        } else {
            canvas.drawRect(r, riempi);
            canvas.drawRect(r, bordo);
        }
        if (forma.equals("SCALA")) {
            Paint gradino = new Paint(bordo);
            gradino.setStrokeWidth(Math.max(0.8f, spessore / 6f));
            boolean lungoY = h >= w;
            float lungo = lungoY ? h : w;
            int gradini = Math.max(3, Math.round(lungo / (L / 50f)));
            for (int i = 1; i < gradini; i++) {
                float t = lungo * i / gradini;
                if (lungoY) canvas.drawLine(r.left, r.top + t, r.right, r.top + t, gradino);
                else canvas.drawLine(r.left + t, r.top, r.left + t, r.bottom, gradino);
            }
            int verso = a.rotazione == 180 || a.rotazione == 270 ? -1 : 1;
            float f = lungo * 0.4f * verso, p = Math.min(w, h) * 0.18f;
            Paint freccia = new Paint(bordo);
            freccia.setColor(Color.parseColor("#374151"));
            if (lungoY) {
                canvas.drawLine(cx, cy + f, cx, cy - f, freccia);
                canvas.drawLine(cx, cy - f, cx - p, cy - f + p * verso, freccia);
                canvas.drawLine(cx, cy - f, cx + p, cy - f + p * verso, freccia);
            } else {
                canvas.drawLine(cx - f, cy, cx + f, cy, freccia);
                canvas.drawLine(cx + f, cy, cx + f - p * verso, cy - p, freccia);
                canvas.drawLine(cx + f, cy, cx + f - p * verso, cy + p, freccia);
            }
            return;
        }
        String sigla = a.elemento.sigla == null ? "" : a.elemento.sigla.trim();
        if (sigla.isEmpty() || forma.equals("PILASTRO")) return;
        Paint testo = new Paint(Paint.ANTI_ALIAS_FLAG);
        testo.setColor(Color.parseColor("#374151"));
        testo.setFakeBoldText(true);
        testo.setTextAlign(Paint.Align.CENTER);
        float dim = Math.min(h * 0.45f, Math.min(w * 0.85f / (0.62f * sigla.length()), L / 45f));
        if (dim < 5 * dp) return;
        testo.setTextSize(dim);
        Paint.FontMetrics fm = testo.getFontMetrics();
        canvas.drawText(sigla, cx, cy - (fm.ascent + fm.descent) / 2f, testo);
    }
}
