package pfa.app.econtab.views;

import android.content.ContentValues;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import pfa.app.econtab.R;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.utils.Utility;

public class EConTabComponenteScatola extends RelativeLayout {

	TextView testo = null;
	ImageView icona = null;

	private ContentValues item = null;

	public EConTabComponenteScatola(Context context) {
		super(context);
		init();
		// TODO Auto-generated constructor stub
	}

	public EConTabComponenteScatola(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
		// TODO Auto-generated constructor stub
	}

	public EConTabComponenteScatola(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
		// TODO Auto-generated constructor stub
	}

	private void init() {
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.econtab_componente_scatola, this, true);

		if (!isInEditMode()) {
			testo = (TextView) findViewById(R.id.testo);
			icona = (ImageView) findViewById(R.id.imageView_icona);
			setClickable(true);
		}
	}

	public void setWrapContentWidth() {
		RelativeLayout root = (RelativeLayout) findViewById(R.id.root_view);
		root.getLayoutParams().width = RelativeLayout.LayoutParams.WRAP_CONTENT;
	}

	public void setTesto(String titolo) {
		testo.setText(titolo);
	}

	public String getTesto() {
		return testo.getText().toString();
	}

	public void setIcona(String ico) {

		icona.setImageBitmap(Utility.getIcona(getContext(), Componenti.PATH_ICONE, ico));
	}

	public ContentValues getItem() {
		return item;
	}

	public void setItem(ContentValues item) {
		this.item = item;
	}

}
