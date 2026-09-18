package pfa.app.econtab.lista;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import pfa.app.econtab.adapters.EConTabListViewAdapter;
import pfa.app.econtab.adapters.EConTabViewHolder;

/**
 * Adapter generico per i moduli lista standard: costruisce le righe a runtime dalla stessa
 * lista di colonne usata per la testata (EConTabListaStandardController), cosi' non possono
 * disallinearsi, e applica la colorazione alternata pari/dispari una volta sola per tutti
 * i moduli (prima duplicata a mano in ogni adapter).
 */
public class EConTabListaStandardAdapter extends EConTabListViewAdapter {

    private static final int COLORE_RIGA_PARI = 0xFFFFFFFF;
    private static final int COLORE_RIGA_DISPARI = 0xFFF2F2F2;

    private final List<ColonnaLista> colonne;

    private static class RigaViewHolder extends EConTabViewHolder {
        View radice;
        TextView[] celle;
    }

    public EConTabListaStandardAdapter(Context context, ArrayList<Object> dati, List<ColonnaLista> colonne) {
        super(context, dati, 0);
        this.colonne = colonne;
    }

    @Override
    protected View impostaLayout(int position, LayoutInflater inflater) {
        LinearLayout riga = new LinearLayout(context);
        riga.setOrientation(LinearLayout.HORIZONTAL);
        riga.setGravity(Gravity.CENTER_VERTICAL);
        int paddingOrizzontale = dpToPx(8);
        riga.setPadding(paddingOrizzontale, dpToPx(10), paddingOrizzontale, dpToPx(10));

        for (int i = 0; i < colonne.size(); i++) {
            ColonnaLista colonna = colonne.get(i);
            TextView cella = new TextView(context);
            cella.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, colonna.peso));
            cella.setSingleLine(true);
            cella.setEllipsize(TextUtils.TruncateAt.END);
            if (i < colonne.size() - 1) {
                cella.setPadding(0, 0, dpToPx(4), 0);
            }
            if (colonna.grassetto) {
                cella.setTypeface(Typeface.DEFAULT_BOLD);
                cella.setTextSize(14);
                cella.setTextColor(0xFF000000);
            } else {
                cella.setTextSize(13);
                cella.setTextColor(0xFF333333);
            }
            riga.addView(cella);
        }
        return riga;
    }

    @Override
    protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
        RigaViewHolder holder = new RigaViewHolder();
        holder.radice = convertView;
        LinearLayout riga = (LinearLayout) convertView;
        holder.celle = new TextView[colonne.size()];
        for (int i = 0; i < colonne.size(); i++) {
            holder.celle[i] = (TextView) riga.getChildAt(i);
        }
        return holder;
    }

    @Override
    protected void personalizzaView(int position, EConTabViewHolder viewholder) {
        RigaViewHolder holder = (RigaViewHolder) viewholder;
        ContentValues riga = (ContentValues) dati.get(position);
        for (int i = 0; i < colonne.size(); i++) {
            holder.celle[i].setText(colonne.get(i).getTesto(riga));
        }
        holder.radice.setBackgroundColor(position % 2 == 0 ? COLORE_RIGA_PARI : COLORE_RIGA_DISPARI);
        super.personalizzaView(position, viewholder);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
