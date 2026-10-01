package com.ncfsistemi.econ.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.utils.Utility;

public class EconMailLayout extends RelativeLayout implements EconSpecialView {

	private static class EconMailClickListener implements OnClickListener {
		private EconMailLayout spinner = null;
		
		public EconMailClickListener(EconMailLayout spinner) {
			this.spinner = spinner;
		}

		@Override
		public void onClick(View v) {
			// DL: TODO
			//Utility.inviaMail(spinner.getContext(), spinner.testo.getText().toString());
		}
	}

	private TextView testo = null;
	private Button button = null;

	public EconMailLayout(Context context) {
		// TODO Auto-generated constructor stub
		super(context);
		init();
	}
	
	public EconMailLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	private void init(){
		
			View v = inflate(getContext(), R.layout.econ_mail_layout, null);
			addView(v);
			if (!isInEditMode()){
				testo = (TextView) findViewById(R.id.textView1);
				button = (Button) findViewById(R.id.button1);
			
				EconMailClickListener listener = new EconMailClickListener(this);
				button.setOnClickListener(listener);
				testo.setOnClickListener(listener);
			}
	}

	

	

	
	
	
	
	public void setValue(String value){
		testo.setText(value);
		if (value != null && value.length()>0){
			button.setVisibility(View.VISIBLE);
		}
		else{
			button.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public String getValue() {
		// TODO Auto-generated method stub
		return testo.getText().toString();
	}

}
