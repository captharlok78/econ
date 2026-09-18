package pfa.app.econtab.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import pfa.app.econtab.R;
import pfa.app.econtab.utils.Utility;

public class EConTabTelefonoLayout extends RelativeLayout implements EConTabSpecialView {

	private static class EConTabTelefonoClickListener implements OnClickListener {
		private EConTabTelefonoLayout spinner = null;
		
		public EConTabTelefonoClickListener(EConTabTelefonoLayout spinner) {
			this.spinner = spinner;
		}

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Utility.chiama(spinner.getContext(), spinner.testo.getText().toString());
		}
	}



	private TextView testo = null;
	private Button button = null;


	public EConTabTelefonoLayout(Context context) {
		// TODO Auto-generated constructor stub
		super(context);
		init();
	}
	
	public EConTabTelefonoLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
		
		
	}
	
	private void init(){
		
			View v = inflate(getContext(), R.layout.econtab_telefono_layout, null);
			addView(v);
			if (!isInEditMode()){
				testo = (TextView) findViewById(R.id.textView1);
				button = (Button) findViewById(R.id.button1);
			
				EConTabTelefonoClickListener listener = new EConTabTelefonoClickListener(this);
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
