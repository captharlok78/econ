package pfa.app.econtab.views;


import android.content.Context;
import android.graphics.Color;
import androidx.viewpager.widget.PagerTabStrip;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import pfa.app.econtab.R;

public class EConTabPagerTabStrip extends PagerTabStrip {

	public EConTabPagerTabStrip(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		personalizzaStile();
	}

	public EConTabPagerTabStrip(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		personalizzaStile();
	}
	
	private void personalizzaStile(){
		// Stile uniformato al blu usato in header/footer (#1565C0) in tutta l'app
		setTabIndicatorColor(Color.WHITE);
		setBackgroundColor(Color.parseColor("#1565C0"));
		setTextColor(Color.WHITE);
		setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.testo_immagine_grande));

		try {
			((TextView)getChildAt(1)).setTextAppearance(getContext(), R.style.TitoloBold);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setPadding(getResources().getDimensionPixelSize(R.dimen.margine), 0, getResources().getDimensionPixelSize(R.dimen.margine), 0);
	}

	/**
	 * Lascio vuoto cos� ke tab sono una  afianco l'altro senza spazi
	 * senn� la superclasse mette sempre un minimo spazio
	 */
	@Override
	public void setTextSpacing(int textSpacing) {
		// TODO Auto-generated method stub
		
	}
}
