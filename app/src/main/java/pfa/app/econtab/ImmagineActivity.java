package pfa.app.econtab;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import pfa.app.econtab.utils.Utility;

public class ImmagineActivity extends EConTabActivity {
	private ImageView img = null;

	public static final String IMMAGINE = "IMMAGINE";
	public static final String DIRECTORY = "DIR";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: ImmagineActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_immagine);
		// setVisualizzazionePopup(6, 3);
		img = (ImageView) findViewById(R.id.imageView1);
		String immagine = getIntent().getStringExtra(IMMAGINE);
		String dir = getIntent().getStringExtra(DIRECTORY);
		if (immagine != null && !immagine.equals("")) {

			img.setImageBitmap(Utility.getIcona(this, dir, immagine));
			// img.setImageBitmap(Utility.getIconaThumb(this,
			// Componenti.PATH_ICONE, "commutatore.png"));
		} else {
			img.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.no_image));
		}
		System.out.println("EConTab: ImmagineActivity onCreate EXIT");
	}

	public void chiudi(View v) {
		finish();
	}

}
