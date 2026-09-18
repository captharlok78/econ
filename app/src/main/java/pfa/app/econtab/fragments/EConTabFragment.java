package pfa.app.econtab.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import pfa.app.econtab.EConTabActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.views.EConTabSpecialView;


public abstract class EConTabFragment extends Fragment {

	boolean sfondoImpostato = false;
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onViewCreated(view, savedInstanceState);
		if (!sfondoImpostato){
			sfondoImpostato = true;
			View scroll = view.findViewById(R.id.scroll);
			if (scroll!=null){
				scroll.setBackgroundResource(android.R.drawable.editbox_background_normal);
			}
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		if (sfondoImpostato){
			sfondoImpostato = false;
		}
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	/**
	 * Imposta il testo della view passata come id
	 * 
	 * @param id
	 *            della textview (editText,Button)
	 * @param text
	 * @param view
	 *            la view che contiene la textView (se null prende l'activity)
	 */
	public void setText(int id, String text, View view) {
		View v = null;
		v =  view.findViewById(id);
		
		if (v instanceof TextView){
			((TextView)v).setText(text);
		}
		if (v instanceof EConTabSpecialView){
			((EConTabSpecialView)v).setValue(text);
		}
	}
	
	/**
	 * Ritorna il testo del campo editabile dell'activity
	 * 
	 * @param id
	 * @return
	 */
	public String getTesto(int id){
		return getTesto(id,null);
	}
	
	/**
	 * Ritorna il testo del campo editabile del fragment
	 * 
	 * @param id
	 * @return
	 */
	public String getTesto(int id,View viewFragmemt){
		return ((EConTabActivity)getActivity()).getTesto(id,viewFragmemt);
		
	}
	
	public EConTabActivity getEConTabActivity(){
		try {
			return (EConTabActivity)getActivity();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
	
	
	
	
	
	
}
