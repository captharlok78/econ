package pfa.app.econtab.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;

import pfa.app.econtab.R;

public class EConTabItemMenu extends RelativeLayout {

	ImageView icona = null;
	TextView testo = null;
	private int idCategoria = 0;
	private int tipo = CATEGORIA_ELEMENTI;

	public static final int CATEGORIA_ELEMENTI = 0;
	public static final int CATEGORIA_COMPONENTI = 1;

	public EConTabItemMenu(Context context) {
		super(context);
		init();
		// TODO Auto-generated constructor stub
	}

	public EConTabItemMenu(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
		// TODO Auto-generated constructor stub
	}

	public EConTabItemMenu(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
		// TODO Auto-generated constructor stub
	}

	private void init() {
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.econtab_item_menu, this, true);

		if (!isInEditMode()) {
			testo = (TextView) findViewById(R.id.testo);
			testo.setTextColor(Color.BLACK);
			//testo.setTextSize(18);
			//testo.setPadding(1, 1, 0, 10);
			icona = (ImageView) findViewById(R.id.icona);
			//icona.setPadding(4, 1, 4, 0);
			setClickable(true);
		}
	}

	public void setTesto(String titolo) {
		testo.setText(titolo);
        testo.setSelected(true);//serve per fare in modo che il testo scorra se troppo grande
	}

	public String getTesto() {
		return testo.getText().toString();
	}

	public void setIcona(String dir, String filename) {
		try {
			File dirApp = getContext().getDir(dir, Context.MODE_PRIVATE);

			File fileIco = new File(dirApp, filename);

			if (fileIco.exists()) {
				Bitmap bitmap = BitmapFactory.decodeFile(dirApp.getAbsolutePath() + File.separator + filename);
				icona.setImageBitmap(bitmap);
			} else {
				icona.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
			}

		} catch (Exception e) {
			icona.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
		}

	}

	@Override
	public void setSelected(boolean selected) {
		// TODO Auto-generated method stub
		super.setSelected(selected);
		if (selected) {
			setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
			testo.setTextColor(Color.WHITE);
		} else {
			setBackgroundColor(getResources().getColor(android.R.color.transparent));
			testo.setTextColor(Color.BLACK);

		}
	}

	public int getIdCategoria() {
		return idCategoria;
	}

	public void setIdCategoria(int idCategoria) {
		this.idCategoria = idCategoria;
	}

	public int getTipo() {
		return tipo;
	}

	public void setTipo(int tipo) {
		this.tipo = tipo;
	}

}
