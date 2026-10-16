package com.ncfsistemi.econ.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.ncfsistemi.econ.R;

/**
 * Occhio accanto a un campo password, come nel login: il tocco la mostra (per VISIBILE_MS, poi si nasconde da sola) o la
 * nasconde. Usato nel cambio password (pannello account del menu e password scaduta).
 */
public final class OcchioPassword {

    private static final long VISIBILE_MS = 10_000;

    private OcchioPassword() {
    }

    /** Mette il campo, già nel suo contenitore, in una riga con l'occhio a destra (stesso posto e stessi margini). */
    public static void aggiungi(EditText et) {
        ViewGroup padre = (ViewGroup) et.getParent();
        Context ctx = et.getContext();
        float dp = ctx.getResources().getDisplayMetrics().density;
        int indice = padre.indexOfChild(et);
        ViewGroup.LayoutParams lp = et.getLayoutParams();
        padre.removeView(et);

        LinearLayout riga = new LinearLayout(ctx);
        riga.setOrientation(LinearLayout.HORIZONTAL);
        riga.setGravity(Gravity.CENTER_VERTICAL);
        riga.addView(et, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        ImageButton occhio = new ImageButton(ctx);
        TypedValue sfondo = new TypedValue();
        ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, sfondo, true);
        occhio.setBackgroundResource(sfondo.resourceId);
        int lato = Math.round(44 * dp);
        riga.addView(occhio, new LinearLayout.LayoutParams(lato, lato));

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable[] nascondi = new Runnable[1];
        nascondi[0] = () -> imposta(et, occhio, false, handler, nascondi[0]);
        imposta(et, occhio, false, handler, nascondi[0]);
        occhio.setOnClickListener(v -> imposta(et, occhio, !visibile(et), handler, nascondi[0]));

        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) lp;
            ViewGroup.MarginLayoutParams nuovo = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nuovo.setMargins(m.leftMargin, m.topMargin, m.rightMargin, m.bottomMargin);
            padre.addView(riga, indice, padre instanceof LinearLayout ? new LinearLayout.LayoutParams(nuovo) : nuovo);
        } else {
            padre.addView(riga, indice, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private static boolean visibile(EditText et) {
        return (et.getInputType() & InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
    }

    private static void imposta(EditText et, ImageButton occhio, boolean visibile, Handler handler, Runnable nascondi) {
        handler.removeCallbacks(nascondi);
        int cursore = et.getSelectionEnd();
        et.setInputType(InputType.TYPE_CLASS_TEXT
                | (visibile ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
        et.setSelection(Math.max(0, Math.min(cursore, et.length())));
        occhio.setImageResource(visibile ? R.drawable.ic_password_nascondi : R.drawable.ic_password_mostra);
        occhio.setContentDescription(visibile ? "Nascondi la password" : "Mostra la password");
        if (visibile) handler.postDelayed(nascondi, VISIBILE_MS);
    }
}
