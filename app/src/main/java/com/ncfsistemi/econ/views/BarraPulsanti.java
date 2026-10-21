package com.ncfsistemi.econ.views;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Barra dei pulsanti in fondo a una maschera (rapportino), su una o piu' righe secondo la larghezza dello schermo. Ogni
 * pulsante ha il testo su una riga sola (niente a capo): le righe si riempiono con la larghezza vera di ogni pulsante
 * (testo, icona, imbottitura), quindi su tablet e in orizzontale di solito una riga, su smartphone in verticale due.
 * La barra e' un LinearLayout verticale con i pulsanti come figli diretti, nell'ordine in cui si vedono; si richiama
 * disponi() ogni volta che cambia la visibilita' o il testo di un pulsante, o l'orientamento.
 */
public final class BarraPulsanti {

    /** Imbottitura orizzontale (dp) di ogni pulsante: ridotta, il testo resta grande. */
    private static final int IMBOTTITURA_DP = 10;
    /** Spazio (dp) tra due pulsanti e ai lati della barra (padding del contenitore nel layout). */
    private static final int SPAZIO_DP = 6;
    private static final int LATI_BARRA_DP = 20;
    private static final int ALTEZZA_RIGA_DP = 52;

    private BarraPulsanti() {
    }

    public static void disponi(LinearLayout barra) {
        @SuppressWarnings("unchecked")
        List<View> pulsanti = (List<View>) barra.getTag();
        if (pulsanti == null) { // prima volta: i pulsanti sono i figli della barra, nell'ordine dell'XML
            pulsanti = new ArrayList<View>();
            for (int i = 0; i < barra.getChildCount(); i++) pulsanti.add(barra.getChildAt(i));
            barra.setTag(pulsanti);
        }
        for (View p : pulsanti) {
            if (p.getParent() != null) ((ViewGroup) p.getParent()).removeView(p);
        }
        barra.removeAllViews();

        float densita = barra.getResources().getDisplayMetrics().density;
        int spazio = Math.round(SPAZIO_DP * densita);
        int disponibile = Math.round((barra.getResources().getConfiguration().screenWidthDp - LATI_BARRA_DP) * densita);

        // righe riempite in ordine con la larghezza che serve a ogni pulsante
        List<List<View>> righe = new ArrayList<List<View>>();
        List<Integer> larghezze = new ArrayList<Integer>();
        int occupata = 0;
        for (View p : pulsanti) {
            if (p.getVisibility() == View.GONE) continue;
            int serve = larghezza(p, densita) + spazio;
            larghezze.add(serve);
            if (righe.isEmpty() || occupata + serve > disponibile) {
                righe.add(new ArrayList<View>());
                occupata = 0;
            }
            righe.get(righe.size() - 1).add(p);
            occupata += serve;
        }

        int altezza = Math.round(ALTEZZA_RIGA_DP * densita);
        int indice = 0;
        for (int r = 0; r < righe.size(); r++) {
            LinearLayout riga = new LinearLayout(barra.getContext());
            riga.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, altezza);
            if (r > 0) lp.topMargin = Math.round(2 * densita);
            barra.addView(riga, lp);
            for (View p : righe.get(r)) {
                // larghezza in proporzione a quella che serve: nessun testo resta stretto
                LinearLayout.LayoutParams lpp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, larghezze.get(indice++));
                lpp.setMarginStart(spazio / 2);
                lpp.setMarginEnd(spazio / 2);
                riga.addView(p, lpp);
            }
        }
        // i pulsanti nascosti restano nella barra (invisibili), cosi' findViewById li trova ancora
        for (View p : pulsanti) {
            if (p.getVisibility() == View.GONE) barra.addView(p);
        }
    }

    /**
     * Pulsanti affiancati su una riga (es. + Manodopera / + Viaggio / + Articolo): testo su una riga, imbottitura ridotta,
     * larghezza in proporzione a quella che serve a ciascuno, cosi' il testo piu' lungo non va a capo.
     */
    public static void inUnaRiga(LinearLayout riga) {
        float densita = riga.getResources().getDisplayMetrics().density;
        for (int i = 0; i < riga.getChildCount(); i++) {
            View p = riga.getChildAt(i);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) p.getLayoutParams();
            lp.width = 0;
            lp.weight = larghezza(p, densita);
            p.setLayoutParams(lp);
        }
    }

    /** Larghezza (px) del pulsante col testo su una riga: testo + icona + imbottitura ridotta. */
    private static int larghezza(View v, float densita) {
        if (!(v instanceof Button)) return Math.round(96 * densita);
        Button b = (Button) v;
        int imbottitura = Math.round(IMBOTTITURA_DP * densita);
        b.setPadding(imbottitura, b.getPaddingTop(), imbottitura, b.getPaddingBottom());
        b.setMaxLines(1);
        b.setEllipsize(android.text.TextUtils.TruncateAt.END);
        int w = Math.round(b.getPaint().measureText(b.getText().toString())) + 2 * imbottitura;
        if (b instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton m = (com.google.android.material.button.MaterialButton) b;
            m.setInsetTop(Math.round(2 * densita));
            m.setInsetBottom(Math.round(2 * densita));
            if (m.getIcon() != null) {
                w += (m.getIconSize() > 0 ? m.getIconSize() : m.getIcon().getIntrinsicWidth()) + m.getIconPadding();
            }
        }
        // un po' di margine per il bordo e gli arrotondamenti
        return w + Math.round(8 * densita);
    }
}
