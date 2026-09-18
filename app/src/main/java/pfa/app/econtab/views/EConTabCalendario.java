package pfa.app.econtab.views;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Calendar;

import pfa.app.econtab.R;
import pfa.app.econtab.utils.Utility;

public class EConTabCalendario extends RelativeLayout implements EConTabSpecialView {

	private static class EConTabCalendarioClickListener implements OnClickListener {
		private EConTabCalendario spinner = null;
		
		public EConTabCalendarioClickListener(EConTabCalendario spinner) {
			this.spinner = spinner;
		}

		@Override
		public void onClick(View v) {
			
			spinner.datapicker.show();
		}

	}


	
	private DatePickerDialog.OnDateSetListener mDateSetListener = null;

	private DatePickerDialog datapicker = null;
	
	private TextView testo = null;
	
	
	private int anno = 0;
	private int mese = 0;
	private int giorno = 0;

	
	public EConTabCalendario(Context context) {
		// TODO Auto-generated constructor stub
		super(context);
		init();
	}

	public EConTabCalendario(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();

	}

	private void init() {

		View v = inflate(getContext(), R.layout.econtab_calendario_layout, null);
		addView(v);
		if (!isInEditMode()) {
			setTesto((TextView) findViewById(R.id.textView1));
			

			EConTabCalendarioClickListener listener = new EConTabCalendarioClickListener(this);
			getTesto().setOnClickListener(listener);
			findViewById(R.id.button1).setOnClickListener(listener);
			
			Calendar cal = Calendar.getInstance();
		    anno = cal.get(Calendar.YEAR);
		    mese = cal.get(Calendar.MONTH);
		    giorno  = cal.get(Calendar.DAY_OF_MONTH);
		    aggiornaCampoData();
		  
			mDateSetListener = new DatePickerDialog.OnDateSetListener() {
				
				@Override
				public void onDateSet(DatePicker view, int year, int monthOfYear,int dayOfMonth) {
					// TODO Auto-generated method stub
					    anno = year;
		                mese = monthOfYear;
		                giorno = dayOfMonth;
		                aggiornaCampoData();
					
				}
			};
			
			  datapicker = new DatePickerDialog(getContext(),
		                mDateSetListener,
		                anno, mese, giorno);
			  
		}
	}

	private void aggiornaCampoData() {
		 String giornoTxt = ""+giorno;
		 String meseTxt = ""+(mese+1);
		 if (giorno<10){
			 giornoTxt = "0"+giornoTxt;
		 }
		 if (mese<9){
			 meseTxt = "0"+meseTxt;
		 }
		 getTesto().setText(new StringBuilder()
	                    .append(giornoTxt).append("/")
	                    .append(meseTxt).append("/")
	                    .append(anno));
	    }

	@Override
	public String getValue() {
		return ""+Utility.dataToNumber(getTesto().getText().toString());
	}

	
	@Override
	public void setValue(String value) {
		// TODO Auto-generated method stub
		setValue(Long.parseLong(value));
	}

	private void setValue(long data){
		Calendar c = Utility.numberToDataCalendar(data);
	
		anno = c.get(Calendar.YEAR);
		mese = c.get(Calendar.MONTH);
	    giorno  = c.get(Calendar.DAY_OF_MONTH);
	    aggiornaCampoData();
	    datapicker.updateDate(anno, mese, giorno);
	}

	public int getAnno() {
		// TODO Auto-generated method stub
		return anno;
	}
	
	public int getGiorno() {
		// TODO Auto-generated method stub
		return giorno;
	}
	
	public int getMese() {
		// TODO Auto-generated method stub
		return mese;
	}
	
	
	public void addTextChangeListener(TextWatcher listener){
		getTesto().addTextChangedListener(listener);
	}

	public TextView getTesto() {
		return testo;
	}

	private void setTesto(TextView testo) {
		this.testo = testo;
	}
	
	
}
