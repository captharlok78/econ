package com.ncfsistemi.econ.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import java.util.ArrayList;
import java.util.HashMap;

import com.ncfsistemi.econ.EconActivity;

public abstract class EconExpListViewAdapter extends BaseExpandableListAdapter {

	protected EconActivity context;
    protected ArrayList<String> listDataHeader; 
    protected HashMap<String, ArrayList<Object>> listDataChild;
    protected int grouplayoutid;
    protected int childlayoutid;

	
	public EconExpListViewAdapter(EconActivity context, ArrayList<String> listDataHeader,HashMap<String, ArrayList<Object>> listChildData,int grouplayoutid,int childlayoutid) {
		// TODO Auto-generated constructor stub
		this.context = context;
        this.listDataHeader = listDataHeader;
        this.listDataChild = listChildData;
		this.grouplayoutid = grouplayoutid;
		this.childlayoutid = childlayoutid;
	}

	@Override
	public Object getChild(int groupPosition, int childPosititon) {
		return listDataChild.get(listDataHeader.get(groupPosition)).get(childPosititon);

	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, final int childPosition,boolean isLastChild, View convertView, ViewGroup parent) {
		 EconViewHolder viewholder = null;
		 
	        if (convertView == null) {
	            LayoutInflater infalInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            convertView = infalInflater.inflate(childlayoutid, null);
	            viewholder = impostaViewHolder(convertView,groupPosition,childPosition);
	            convertView.setTag(viewholder);
	        }
	        else{
	        	viewholder = (EconViewHolder)convertView.getTag();
	        }
	        
	        personalizzaChildView(groupPosition, childPosition, isLastChild, viewholder);
	        
	        return convertView;

	}

	@Override
	public int getChildrenCount(int groupPosition) {
		// TODO Auto-generated method stub
			if (!listDataChild.containsKey(listDataHeader.get(groupPosition))){
				return 0;
			}
		  return this.listDataChild.get(this.listDataHeader.get(groupPosition)).size();

	}

	@Override
	public Object getGroup(int groupPosition) {
		// TODO Auto-generated method stub
		return listDataHeader.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		// TODO Auto-generated method stub
		return listDataHeader.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		// TODO Auto-generated method stub
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		
		if (convertView == null) {
			LayoutInflater infalInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = infalInflater.inflate(grouplayoutid, null);
		}

		personalizzaGroupView(groupPosition, isExpanded, convertView);

		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return true;
	}
	
	
	/**
	 * Aggiunge le personalizzazioni di visualizzazione per i figli della lista corrente
	 * Da sovrascrivere
     */
	protected void personalizzaChildView(int groupPosition, final int childPosition,boolean isLastChild, EconViewHolder holder){
		
	}
	
	/**
	 * Aggiunge le personalizzazioni di visualizzazione per i gruppi della lista corrente
	 * Da sovrascrivere 
	 * @param convertView
	 */
	protected void personalizzaGroupView(int groupPosition, boolean isExpanded,View convertView){
		
	}
	
	/**
	 * 
	 * Da sovrascrivere per aggiungere le view del layout corrente
	 * @param convertView
	 */
	protected EconViewHolder impostaViewHolder(View convertView,final int groupPosition, final int childPosition){
		return null;
	}

}
