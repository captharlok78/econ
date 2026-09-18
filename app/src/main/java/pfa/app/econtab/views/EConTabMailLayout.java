package pfa.app.econtab.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import pfa.app.econtab.R;
import pfa.app.econtab.utils.Utility;

public class EConTabMailLayout extends RelativeLayout implements EConTabSpecialView {

	private static class EConTabMailClickListener implements OnClickListener {
		private EConTabMailLayout spinner = null;
		
		public EConTabMailClickListener(EConTabMailLayout spinner) {
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

	public EConTabMailLayout(Context context) {
		// TODO Auto-generated constructor stub
		super(context);
		init();
	}
	
	public EConTabMailLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	private void init(){
		
			View v = inflate(getContext(), R.layout.econtab_mail_layout, null);
			addView(v);
			if (!isInEditMode()){
				testo = (TextView) findViewById(R.id.textView1);
				button = (Button) findViewById(R.id.button1);
			
				EConTabMailClickListener listener = new EConTabMailClickListener(this);
				button.setOnClickListener(listener);
				testo.setOnClickListener(listener);
			}
	}

	

	

	
	
	
	
	public void setValue(String value){
		testo.setText(value);
		if (value.length()>0){
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
