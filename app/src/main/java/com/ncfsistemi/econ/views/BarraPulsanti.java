package com.ncfsistemi.econ.views;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Barra dei pulsanti in fondo a una maschera, su una o piu' righe secondo la larghezza dello schermo: su uno smartphone in
 * verticale i pulsanti visibili vanno su 2-3 righe (al massimo 3 per riga), su tablet e in orizzontale restano su una.
 * La barra e' un LinearLayout verticale con i pulsanti come figli diretti, nell'ordine in cui si vedono; si richiama
 * disponi() ogni volta che cambia la visibilita' di un pulsante o l'orientamento.
 */
public final class BarraPulsanti {

    /** Larghezza minima di un pulsante (dp) per decidere quanti ne stanno in una riga. */
    private static final int LARGHEZZA_PULSANTE_DP = 110;
    private static final int MINIMO_PER_RIGA = 3;

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

        List<View> visibili = new ArrayList<View>();
        for (View p : pulsanti) {
            if (p.getVisibility() != View.GONE) visibili.add(p);
        }
        int larghezzaDp = barra.getResources().getConfiguration().screenWidthDp;
        int perRiga = Math.max(MINIMO_PER_RIGA, larghezzaDp / LARGHEZZA_PULSANTE_DP);
        int righe = Math.max(1, (visibili.size() + perRiga - 1) / perRiga);
        float densita = barra.getResources().getDisplayMetrics().density;
        int altezza = Math.round(48 * densita);

        // stesse quantita' per riga (7 pulsanti in 3 righe: 3 + 2 + 2), i primi pulsanti in alto
        int indice = 0;
        for (int r = 0; r < righe; r++) {
            int quanti = visibili.size() / righe + (r < visibili.size() % righe ? 1 : 0);
            LinearLayout riga = new LinearLayout(barra.getContext());
            riga.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, altezza);
            if (r > 0) lp.topMargin = Math.round(2 * densita);
            barra.addView(riga, lp);
            for (int i = 0; i < quanti; i++) {
                LinearLayout.LayoutParams lpp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                lpp.setMarginStart(Math.round(densita));
                lpp.setMarginEnd(Math.round(densita));
                riga.addView(visibili.get(indice++), lpp);
            }
        }
        // i pulsanti nascosti restano nella barra (invisibili), cosi' findViewById li trova ancora
        for (View p : pulsanti) {
            if (p.getVisibility() == View.GONE) barra.addView(p);
        }
    }
}
