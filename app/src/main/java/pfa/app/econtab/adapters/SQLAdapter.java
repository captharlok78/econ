package pfa.app.econtab.adapters;


import android.content.ContentValues;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;

import pfa.app.econtab.R;


/**
 * Created by matteo on 09/10/2017.
 */

public class SQLAdapter extends BaseAdapter {
    private ArrayList<Object> dati = null;
    private Context ctx = null;
    private LayoutInflater myLayoutInflater;


    public SQLAdapter(Context ctx, ArrayList<Object> dati) {
        this.ctx = ctx;
        this.dati = dati;
        myLayoutInflater = LayoutInflater.from(ctx);
    }

    private class ViewHolder {
        TextView textSQL = null;
        int position = -1;
    }

    public int getPosition(View v) {
        SQLAdapter.ViewHolder holder = (SQLAdapter.ViewHolder) v.getTag();
        return holder.position;
    }

    @Override
    public int getCount() {
        return dati.size();
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }


    @Override
    public View getView(final int position, View view, ViewGroup viewGroup) {

        final SQLAdapter.ViewHolder myHolder;
        if (view == null) {
            view = myLayoutInflater.inflate(R.layout.singola_sql, null);
            myHolder = new SQLAdapter.ViewHolder();
            myHolder.textSQL = (TextView) view.findViewById(R.id.textSQL);

            view.setTag(myHolder);
        } else {
            myHolder = (SQLAdapter.ViewHolder) view.getTag();
        }
        myHolder.position = position;
        final ContentValues val = (ContentValues) dati.get(position);
        Iterator<String> iter =  val.keySet().iterator();
        String record = "";
        while(iter.hasNext()){
            String chiave = iter.next();
            String valore = val.getAsString(chiave);
            record = record+chiave + ": " + valore+" , ";

        }
        if (record.length()>0){
            record = record.substring(0,record.length()-2);
        }
        myHolder.textSQL.setText(record);

        return view;
    }
}
