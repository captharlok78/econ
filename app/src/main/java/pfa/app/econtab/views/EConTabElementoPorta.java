package pfa.app.econtab.views;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.LocaliPorteFinestre;

public class EConTabElementoPorta extends AbstractEConTabElemento implements OnClickListener {

	public static final String PORTA = "P";
	public static final String FINESTRA = "F";

	private String tipo = PORTA;
	private int currentImg = 0;

	private ImageView immagine = null;
	private Button button_ruota = null;

	public static final int VERTICALE = 1;
	public static final int ORIZZONTALE = 0;

	public static final String SOPRA = "T";
	public static final String SOTTO = "B";
	public static final String SINISTRA = "L";
	public static final String DESTRA = "R";

	private int orientamento = ORIZZONTALE;

	private int cellaIniziale = 0;
	private int positionInizialePercent = 0;
	private String posizioneInCella = SOPRA;

	private int posXCella = 0;
	private int posYCella = 0;
	private int larghezzaCella = 0;
	private int altezzaCella = 0;

	private int padding = 0;
	private int bordoPiantina = 0;

	public EConTabElementoPorta(Context context, String tipo) {
		super(context);
		init();
		this.setTipo(tipo);
	}

	private void init() {
		// TODO Auto-generated method stub
		View v = inflate(getContext(), R.layout.econtab_elemento_porta_layout, null);
		addView(v);
		if (!isInEditMode()) {

			immagine = (ImageView) findViewById(R.id.immagine);

			button_ruota = (Button) findViewById(R.id.button_ruota);
			button_ruota.setVisibility(View.INVISIBLE);
			button_ruota.setOnClickListener(this);
			padding = getResources().getDimensionPixelSize(R.dimen.padding_drag);
			bordoPiantina = getResources().getDimensionPixelSize(R.dimen.bordo_piantina);
			setPadding(padding, padding, padding, padding);
		}
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
		if (tipo.equals(PORTA)) {
			immagine.setImageResource(R.drawable.porta_orizzontale);
			currentImg = R.drawable.porta_orizzontale;
		}
		if (tipo.equals(FINESTRA)) {
			immagine.setImageResource(R.drawable.finestra_orizzontale);
			currentImg = R.drawable.finestra_orizzontale;
		}

	}

	public void setSelected(boolean selected) {
		super.setSelected(selected);
		if (selected) {
			// immagine.setBackgroundResource(R.drawable.bg_econtab_drag_ok);
			button_ruota.setVisibility(View.VISIBLE);
		} else {
			// immagine.setBackgroundResource(0);
			button_ruota.setVisibility(View.INVISIBLE);

		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

		if (tipo.equals(PORTA)) {
			if (currentImg == R.drawable.porta_orizzontale) {

				setOrientamento(VERTICALE);

			} else {

				setOrientamento(ORIZZONTALE);
			}
		}

		if (tipo.equals(FINESTRA)) {
			if (currentImg == R.drawable.finestra_orizzontale) {

				setOrientamento(VERTICALE);

			} else {

				setOrientamento(ORIZZONTALE);

			}
		}

		ricalcolaPosizione();

	}

	@Override
	public void ricalcolaPosizione() {
		// TODO Auto-generated method stub
		// super.ricalcolaPosizione();

		if (tipo.equals(PORTA)) {
			if (currentImg == R.drawable.porta_orizzontale) {
				immagine.getLayoutParams().width = getLarghezzaPiantina() / 7;
				immagine.getLayoutParams().height = getAltezzaPiantina() / 40;

			} else {
				immagine.getLayoutParams().height = getAltezzaPiantina() / 7;
				immagine.getLayoutParams().width = getLarghezzaPiantina() / 40;

			}

		}

		if (tipo.equals(FINESTRA)) {
			if (currentImg == R.drawable.finestra_orizzontale) {
				immagine.getLayoutParams().width = getLarghezzaPiantina() / 6;
				immagine.getLayoutParams().height = getAltezzaPiantina() / 40;

			} else {
				immagine.getLayoutParams().height = getAltezzaPiantina() / 6;
				immagine.getLayoutParams().width = getLarghezzaPiantina() / 40;

			}

		}

		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();

		// imposto la posizione in base alla posizione della cella
		if (getPosizioneInCella().equals(SOPRA)) {
			int paddingLeft = getLarghezzaCella() * getPositionInizialePercent() / 100;
			if (paddingLeft < 0) {
				paddingLeft = 0;
			}
			lp.topMargin = getPosYCella() - padding - immagine.getLayoutParams().height / 2;
			lp.leftMargin = getPosXCella() - padding + paddingLeft;
		}

		if (getPosizioneInCella().equals(SOTTO)) {
			int paddingLeft = getLarghezzaCella() * getPositionInizialePercent() / 100;
			if (paddingLeft < 0) {
				paddingLeft = 0;
			}
			lp.topMargin = getPosYCella() - padding + getAltezzaCella() - immagine.getLayoutParams().height / 2 - bordoPiantina / 2;
			lp.leftMargin = getPosXCella() - padding + paddingLeft;
		}

		if (getPosizioneInCella().equals(DESTRA)) {
			int paddingLeft = getAltezzaCella() * getPositionInizialePercent() / 100;
			if (paddingLeft < 0) {
				paddingLeft = 0;
			}
			lp.topMargin = getPosYCella() - padding + paddingLeft;
			lp.leftMargin = getPosXCella() + getLarghezzaCella() - padding - immagine.getLayoutParams().width / 2 - bordoPiantina / 2;

		}

		if (getPosizioneInCella().equals(SINISTRA)) {
			int paddingLeft = getAltezzaCella() * getPositionInizialePercent() / 100;
			if (paddingLeft < 0) {
				paddingLeft = 0;
			}
			lp.topMargin = getPosYCella() - padding + paddingLeft;
			lp.leftMargin = getPosXCella() - padding - immagine.getLayoutParams().width / 2;

		}

	}

	public int getOrientamento() {
		return orientamento;
	}

	public void setOrientamento(int orientamento) {
		this.orientamento = orientamento;
		if (tipo.equals(PORTA)) {
			if (orientamento == VERTICALE) {
				immagine.setImageResource(R.drawable.porta_verticale);
				currentImg = R.drawable.porta_verticale;
			} else {
				immagine.setImageResource(R.drawable.porta_orizzontale);
				currentImg = R.drawable.porta_orizzontale;
			}
		}

		if (tipo.equals(FINESTRA)) {
			if (orientamento == VERTICALE) {
				immagine.setImageResource(R.drawable.finestra_verticale);
				currentImg = R.drawable.finestra_verticale;
			} else {
				immagine.setImageResource(R.drawable.finestra_orizzontale);
				currentImg = R.drawable.finestra_orizzontale;
			}
		}
	}

	public int getCellaIniziale() {
		return cellaIniziale;
	}

	public void setCellaIniziale(int cellaIniziale) {
		this.cellaIniziale = cellaIniziale;
	}

	public int getPositionInizialePercent() {
		return positionInizialePercent;
	}

	public void setPositionInizialePercent(int positionInizialePercent) {
		this.positionInizialePercent = positionInizialePercent;
	}

	public String getPosizioneInCella() {
		return posizioneInCella;
	}

	public void setPosizioneInCella(String posizioneInCella) {
		this.posizioneInCella = posizioneInCella;
		if (posizioneInCella.equals(SOPRA) || posizioneInCella.equals(SOTTO)) {
			setOrientamento(ORIZZONTALE);
		} else {
			setOrientamento(VERTICALE);
		}
	}

	public int getPosXCella() {
		return posXCella;
	}

	public void setPosXCella(int posXCella) {
		this.posXCella = posXCella;
	}

	public int getPosYCella() {
		return posYCella;
	}

	public void setPosYCella(int posYCella) {
		this.posYCella = posYCella;
	}

	public int getLarghezzaCella() {
		return larghezzaCella;
	}

	public void setLarghezzaCella(int larghezzaCella) {
		this.larghezzaCella = larghezzaCella;
	}

	public int getAltezzaCella() {
		return altezzaCella;
	}

	public void setAltezzaCella(int altezzaCella) {
		this.altezzaCella = altezzaCella;
	}

	public int getPosizioneLeftImmagine() {
		return immagine.getLeft() + padding;
	}

	public int getPosizioneTopImmagine() {
		return immagine.getTop() + padding;
	}

	public int getLarghezzaImmagine() {
		return immagine.getWidth();
	}

	public int getAltezzaImmagine() {
		return immagine.getHeight();
	}

	@Override
	public void cancellaDaDb(Context context) {
		// TODO Auto-generated method stub
		super.cancellaDaDb(context);
		DbInterno db = new DbInterno(context);

		ContentValues where = new ContentValues();
		where.put(LocaliPorteFinestre.ID_PORTA_FINESTRA, getIdElemento());

		LocaliPorteFinestre tabLocali = new LocaliPorteFinestre();
		tabLocali.cancellaRecord(db, where);
		// db.delete(LocaliPorteFinestre.NOME_TABELLA, where);
		db.close();
	}

	@Override
	public void aggiungiInDb(Context context, int idLocale) {
		// TODO Auto-generated method stub
		super.aggiungiInDb(context, idLocale);

		DbInterno db = new DbInterno(context);
		LocaliPorteFinestre porte = new LocaliPorteFinestre();
		ContentValues val = porte.getValoriLogInserimento(db);
		val.put(LocaliPorteFinestre.ID_LOCALE, idLocale);
		val.put(LocaliPorteFinestre.TIPO, tipo);
		val.put(LocaliPorteFinestre.CELLA, getCellaIniziale());
		val.put(LocaliPorteFinestre.POS_IN_CELLA, getPosizioneInCella());
		val.put(LocaliPorteFinestre.POS_INIZIALE_PERC, getPositionInizialePercent());

		porte.inserisciRecord(db, val);

		db.close();

		setIdElemento(val.getAsInteger(LocaliPorteFinestre.ID_PORTA_FINESTRA));

	}

	@Override
	public void aggiornaDb(Context context) {
		// TODO Auto-generated method stub
		super.aggiornaDb(context);
		DbInterno db = new DbInterno(context);
		LocaliPorteFinestre porte = new LocaliPorteFinestre();
		ContentValues val = porte.getValoriLogModifica(db);

		val.put(LocaliPorteFinestre.CELLA, getCellaIniziale());
		val.put(LocaliPorteFinestre.POS_IN_CELLA, getPosizioneInCella());
		val.put(LocaliPorteFinestre.POS_INIZIALE_PERC, getPositionInizialePercent());

		ContentValues where = new ContentValues();
		where.put(LocaliPorteFinestre.ID_PORTA_FINESTRA, getIdElemento());

		porte.aggiornaRecord(db, val, where);

		db.close();
	}

}
