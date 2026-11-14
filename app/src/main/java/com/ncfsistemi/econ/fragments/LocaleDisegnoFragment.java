package com.ncfsistemi.econ.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.ncfsistemi.econ.CantiereSplitActivity;
import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.table.Locali;
import com.ncfsistemi.econ.disegno.DisegnoDati;
import com.ncfsistemi.econ.disegno.PiantinaView;
import com.ncfsistemi.econ.utils.Sessione;

import java.util.ArrayList;
import java.util.List;

/**
 * Linguetta "Disegno" del locale nella scheda cantiere, al posto della vecchia piantina quando si usa il disegno nuovo
 * (DisegnoActivity): anteprima della piantina, elementi del locale e "Apri il disegno", nella grafica del disegno.
 * È la pagina su cui si torna uscendo dal disegno; si aggiorna a ogni ritorno.
 */
public class LocaleDisegnoFragment extends Fragment {

    private static final int BLU = 0xFF1565C0;
    private static final int SFONDO = 0xFFF4F6FA;
    private static final int BORDO = 0xFFDDE3EA;
    private static final int TESTO = 0xFF212121;
    private static final int TESTO2 = 0xFF5F6B7A;

    private int idLocale;
    private float dp;
    private Typeface fa;
    private PiantinaView anteprima;
    private TextView titolo, sottotitolo, titoloElementi;
    private LinearLayout elenco;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        idLocale = getArguments() != null ? getArguments().getInt(Locali.ID_LOCALE) : 0;
        dp = getResources().getDisplayMetrics().density;
        fa = ResourcesCompat.getFont(requireContext(), R.font.fa_solid_900);

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setBackgroundColor(SFONDO);
        scroll.setFillViewport(true);
        LinearLayout corpo = verticale();
        corpo.setPadding(px(12), px(12), px(12), px(12));
        scroll.addView(corpo);

        // testata: locale e Apri il disegno
        LinearLayout testa = new LinearLayout(requireContext());
        testa.setOrientation(LinearLayout.HORIZONTAL);
        testa.setGravity(Gravity.CENTER_VERTICAL);
        card(testa);
        TextView icona = new TextView(requireContext());
        icona.setTypeface(fa);
        icona.setText("");
        icona.setTextColor(BLU);
        icona.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        icona.setGravity(Gravity.CENTER);
        testa.addView(icona, new LinearLayout.LayoutParams(px(40), px(40)));
        LinearLayout testi = verticale();
        testi.setPadding(px(10), 0, px(10), 0);
        titolo = testo(18, TESTO, true);
        titolo.setSingleLine(true);
        titolo.setEllipsize(TextUtils.TruncateAt.END);
        sottotitolo = testo(13, TESTO2, false);
        sottotitolo.setSingleLine(true);
        sottotitolo.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        testi.addView(titolo);
        testi.addView(sottotitolo);
        testa.addView(testi, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        MaterialButton apri = new MaterialButton(requireContext());
        apri.setText("Apri il disegno");
        apri.setAllCaps(false);
        apri.setBackgroundTintList(ColorStateList.valueOf(BLU));
        apri.setOnClickListener(v -> apriDisegno());
        testa.addView(apri);
        corpo.addView(testa, lpCard());

        // anteprima della piantina: toccandola si apre il disegno
        FrameLayout riquadro = new FrameLayout(requireContext());
        card(riquadro);
        riquadro.setPadding(px(6), px(6), px(6), px(6));
        anteprima = new PiantinaView(requireContext());
        anteprima.setModo(PiantinaView.GUARDA);
        riquadro.addView(anteprima, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(380)));
        View tocco = new View(requireContext());
        tocco.setOnClickListener(v -> apriDisegno());
        riquadro.addView(tocco, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(380)));
        corpo.addView(riquadro, lpCard());

        // elementi del locale
        LinearLayout cardElementi = verticale();
        card(cardElementi);
        titoloElementi = testo(12, TESTO2, true);
        titoloElementi.setAllCaps(true);
        titoloElementi.setPadding(0, 0, 0, px(8));
        cardElementi.addView(titoloElementi);
        elenco = verticale();
        cardElementi.addView(elenco);
        corpo.addView(cardElementi, lpCard());
        return scroll;
    }

    @Override
    public void onResume() {
        super.onResume();
        aggiorna();
    }

    /** Rilegge locale ed elementi (al ritorno dal disegno). */
    public void aggiorna() {
        if (getView() == null || idLocale == 0) return;
        int preventivo = Sessione.getIdPreventivoSelezionato();
        DisegnoDati.Locale loc = DisegnoDati.locale(requireContext(), idLocale);
        List<DisegnoDati.Posato> posati = loc == null ? new ArrayList<>() : DisegnoDati.elementi(requireContext(), idLocale, preventivo);
        titolo.setText(loc == null ? "" : loc.nome);
        String perc = DisegnoDati.percorso(requireContext(), idLocale);
        sottotitolo.setText((perc.isEmpty() ? "" : perc + "  ·  ") + DisegnoDati.descrizioneDocumento(requireContext(), preventivo));
        if (loc != null) anteprima.setDati(loc, posati, DisegnoDati.linee(requireContext(), idLocale));

        titoloElementi.setText("Elementi (" + posati.size() + ")");
        elenco.removeAllViews();
        if (posati.isEmpty()) {
            TextView v = testo(14, TESTO2, false);
            v.setText("Nessun elemento: apri il disegno e scegli Posa");
            elenco.addView(v);
        }
        for (DisegnoDati.Posato p : posati) {
            LinearLayout r = new LinearLayout(requireContext());
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(px(4), px(8), px(4), px(8));
            TypedValue tv = new TypedValue();
            requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
            r.setBackgroundResource(tv.resourceId);
            TextView num = testo(12, p.altroOrdine ? TESTO2 : BLU, true);
            num.setText(p.numero);
            num.setGravity(Gravity.CENTER);
            GradientDrawable g = new GradientDrawable();
            g.setColor(p.altroOrdine ? 0xFFECEFF1 : 0xFFE3F2FD);
            g.setCornerRadius(px(12));
            num.setBackground(g);
            r.addView(num, new LinearLayout.LayoutParams(px(40), px(24)));
            TextView nome = testo(15, p.altroOrdine ? TESTO2 : TESTO, false);
            nome.setText(p.nome);
            nome.setPadding(px(12), 0, px(8), 0);
            nome.setSingleLine(true);
            nome.setEllipsize(TextUtils.TruncateAt.END);
            r.addView(nome, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            List<String> info = new ArrayList<>();
            if (p.cavi + p.tubi > 0) info.add((p.cavi + p.tubi) + " coll.");
            if (p.altezza > 0) info.add("h " + Math.round(p.altezza) + " cm");
            if (!info.isEmpty()) {
                TextView i = testo(12, TESTO2, false);
                i.setText(TextUtils.join(" · ", info));
                r.addView(i);
            }
            r.setOnClickListener(v -> apriDisegno());
            elenco.addView(r);
        }
    }

    private void apriDisegno() {
        if (getActivity() instanceof CantiereSplitActivity) {
            ((CantiereSplitActivity) getActivity()).apriDisegno(idLocale);
        }
    }

    // ------------------------------------------------------------------ utilità

    private void card(View v) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.WHITE);
        g.setStroke(px(1), BORDO);
        g.setCornerRadius(px(12));
        v.setBackground(g);
        v.setPadding(px(12), px(12), px(12), px(12));
    }

    private LinearLayout.LayoutParams lpCard() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = px(12);
        return lp;
    }

    private LinearLayout verticale() {
        LinearLayout l = new LinearLayout(requireContext());
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private TextView testo(int sp, int colore, boolean grassetto) {
        TextView t = new TextView(requireContext());
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(colore);
        if (grassetto) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private int px(float v) {
        return Math.round(v * dp);
    }
}
