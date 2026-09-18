package pfa.app.econtab.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Placche;

/**
 * Created by daniele on 15/01/2018.
 */

public class AssCodiciLineeAdapter extends  EConTabListViewAdapter {
    private class AccCodiciLineeViewHolder extends EConTabViewHolder {
        TextView linea = null;


    }

    public AssCodiciLineeAdapter(Context context, ArrayList<Object> dati, int layoutid) {
        super(context, dati, layoutid);
    }

    @Override
    protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
        // TODO Auto-generated method stub
        AssCodiciLineeAdapter.AccCodiciLineeViewHolder holder = new AssCodiciLineeAdapter.AccCodiciLineeViewHolder();
        holder.linea = (TextView) convertView.findViewById(R.id.nome_linea);
        
        return holder;
    }

    @Override
    protected void personalizzaView(int position, EConTabViewHolder viewholder) {
        // TODO Auto-generated method stub
        ContentValues val = (ContentValues) dati.get(position);

        String nomeLinea = val.getAsString(Linee.NOME_LINEA);

        ((AssCodiciLineeAdapter.AccCodiciLineeViewHolder) viewholder).linea.setText(nomeLinea);
        ((AssCodiciLineeAdapter.AccCodiciLineeViewHolder) viewholder).linea.setTag(position);

        super.personalizzaView(position, viewholder);
    }


}
