package pfa.app.econtab.views;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import pfa.app.econtab.R;

public abstract class AbstractEConTabElemento extends RelativeLayout {
	public AbstractEConTabElemento(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	private int posXPiantina = 0;
	private int posYPiantina = 0;
	private int larghezzaPiantina = 0;
	private int altezzaPiantina = 0;

	private int positionPercentX = 0;
	private int positionPercentY = 0;

	private int idElemento = 0;

	private int offsetRootX = 0;
	private int offsetRootY = 0;

	private boolean onDrag = false;
    private boolean onFront = false;

	public int getPositionPercentX() {
		return positionPercentX;
	}

	public void setPositionPercentX(int positionPercentX) {
		this.positionPercentX = positionPercentX;
	}

	public int getPositionPercentY() {
		return positionPercentY;
	}

	public void setPositionPercentY(int positionPercentY) {
		this.positionPercentY = positionPercentY;
	}

	public int getPosXPiantina() {
		return posXPiantina;
	}

	public void setPosXPiantina(int posXPiantina) {
		this.posXPiantina = posXPiantina;
	}

	public int getPosYPiantina() {
		return posYPiantina;
	}

	public void setPosYPiantina(int posYPiantina) {
		this.posYPiantina = posYPiantina;
	}

	public int getLarghezzaPiantina() {
		return larghezzaPiantina;
	}

	public void setLarghezzaPiantina(int larghezzaPiantina) {
		this.larghezzaPiantina = larghezzaPiantina;
	}

	public int getAltezzaPiantina() {
		return altezzaPiantina;
	}

	public void setAltezzaPiantina(int altezzaPiantina) {
		this.altezzaPiantina = altezzaPiantina;
	}

	public void ricalcolaPosizione() {
		int offsetX = (getLarghezzaPiantina() * getPositionPercentX()) / 200;
		int offsetY = (getAltezzaPiantina() * getPositionPercentY()) / 200;
		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
		lp.leftMargin = getPosXPiantina() + offsetX;
		lp.topMargin = getPosYPiantina() + offsetY;
	}

	public void ricalcolaPosizioneInversa() {
		int offsetX = getLeft() - getPosXPiantina() + getOffsetRootX();
		int offsetY = getTop() - getPosYPiantina() + getOffsetRootY();

		int posXPerc = (200 * offsetX) / getLarghezzaPiantina();
		int posYPerc = (200 * offsetY) / getAltezzaPiantina();

		if (posXPerc % 2 > 0) {
			posXPerc = posXPerc + (2 - posXPerc % 2);
		}

		if (posYPerc % 2 > 0) {
			posYPerc = posYPerc + (2 - posYPerc % 2);
		}

		setPositionPercentX(posXPerc);
		setPositionPercentY(posYPerc);
	}

	@Override
	public void setSelected(boolean selected) {
		// TODO Auto-generated method stub
		super.setSelected(selected);
		impostaBordoSelezione();
	}

	public void impostaBordoSelezione() {
		if (isSelected()) {
			setBackgroundResource(R.drawable.bg_econtab_drag_ok);
		} else {
			setBackgroundResource(0);
		}
	}

	public boolean isOnDrag() {
		return onDrag;
	}

	public void setOnDrag(boolean onDrag) {
		this.onDrag = onDrag;
	}

    public void setOnFront(boolean onFront){
        this.onFront = onFront;
        bringToFront();
    }

    public boolean isOnFront() {
        return onFront;
    }

	public int getIdElemento() {
		return idElemento;
	}

	public void setIdElemento(int idElemento) {
		this.idElemento = idElemento;
	}

	public void cancellaDaDb(Context context) {
		// TODO Auto-generated method stub

	}

	public void aggiungiInDb(Context context, int idLocale) {
		// TODO Auto-generated method stub

	}

	public void aggiornaDb(Context context) {
		// TODO Auto-generated method stub

	}

	public int getOffsetRootX() {
		return offsetRootX;
	}

	public void setOffsetRootX(int offsetRootX) {
		this.offsetRootX = offsetRootX;
	}

	public int getOffsetRootY() {
		return offsetRootY;
	}

	public void setOffsetRootY(int offsetRootY) {
		this.offsetRootY = offsetRootY;
	}

}
