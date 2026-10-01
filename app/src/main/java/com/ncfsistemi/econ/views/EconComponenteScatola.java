package com.ncfsistemi.econ.views;

import android.content.ContentValues;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.table.Componenti;
import com.ncfsistemi.econ.utils.Utility;

public class EconComponenteScatola extends RelativeLayout {

	TextView testo = null;
	ImageView icona = null;

	private ContentValues item = null;

	public EconComponenteScatola(Context context) {
		super(context);
		init();
		// TODO Auto-generated constructor stub
	}

	public EconComponenteScatola(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
		// TODO Auto-generated constructor stub
	}

	public EconComponenteScatola(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
		// TODO Auto-generated constructor stub
	}

	private void init() {
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.econ_componente_scatola, this, true);

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
