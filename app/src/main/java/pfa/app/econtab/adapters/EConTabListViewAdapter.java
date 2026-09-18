package pfa.app.econtab.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

public class EConTabListViewAdapter extends BaseAdapter {
	protected Context context;
	protected ArrayList<Object> dati;
	protected int layoutId;

	public EConTabListViewAdapter(Context context, ArrayList<Object> dati, int layoutid) {
		this.context = context;
		this.dati = dati;
		this.layoutId = layoutid;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return dati.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return dati.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return arg0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		EConTabViewHolder viewholder = null;

		if (convertView == null) {
			LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = impostaLayout(position, infalInflater);
			viewholder = impostaViewHolder(convertView, position);
			convertView.setTag(viewholder);
		} else {
			viewholder = (EConTabViewHolder) convertView.getTag();
		}
		personalizzaView(position, viewholder);

		return convertView;
	}

	protected void personalizzaView(int position, EConTabViewHolder viewholder) {
		// TODO Auto-generated method stub

	}

	protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Pu� essere sovrascritto ad esempio per impostare layout diversi solo per alcune righe
	 * 
	 * @param infalInflater
	 * @return
	 */
	protected View impostaLayout(int position, LayoutInflater infalInflater) {
		return infalInflater.inflate(layoutId, null);
	}

	protected String getString(int id) {
		return context.getResources().getString(id);
	}
}
