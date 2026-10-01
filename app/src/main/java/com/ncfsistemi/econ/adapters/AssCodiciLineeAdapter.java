package com.ncfsistemi.econ.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.table.Costruttori;
import com.ncfsistemi.econ.db.table.Linee;
import com.ncfsistemi.econ.db.table.Placche;


public class AssCodiciLineeAdapter extends  EconListViewAdapter {
    private class AccCodiciLineeViewHolder extends EconViewHolder {
        TextView linea = null;


    }

    public AssCodiciLineeAdapter(Context context, ArrayList<Object> dati, int layoutid) {
        super(context, dati, layoutid);
    }

    @Override
    protected EconViewHolder impostaViewHolder(View convertView, int position) {
        // TODO Auto-generated method stub
        AssCodiciLineeAdapter.AccCodiciLineeViewHolder holder = new AssCodiciLineeAdapter.AccCodiciLineeViewHolder();
        holder.linea = (TextView) convertView.findViewById(R.id.nome_linea);
        
        return holder;
    }

    @Override
    protected void personalizzaView(int position, EconViewHolder viewholder) {
        // TODO Auto-generated method stub
        ContentValues val = (ContentValues) dati.get(position);

        String nomeLinea = val.getAsString(Linee.NOME_LINEA);

        ((AssCodiciLineeAdapter.AccCodiciLineeViewHolder) viewholder).linea.setText(nomeLinea);
        ((AssCodiciLineeAdapter.AccCodiciLineeViewHolder) viewholder).linea.setTag(position);

        super.personalizzaView(position, viewholder);
    }


}
