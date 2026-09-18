package pfa.app.econtab.views;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Collegamenti;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.LocaliPorteFinestre;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.Relazioni;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;

public class EConTabPiantinaGridView extends GridView implements OnTouchListener, View.OnClickListener {

	private boolean inModifica = false;

	private ArrayList<EConTabElementoPorta> elementiporte = null;
	private ArrayList<EConTabElementoElettrico> elementi = null;

	private EConTabPiantinaFrameLayout root = null;
	private EConTabLocaleViewPager viewPager = null;

	private ImageView cestino = null;

	long startDragTime = 0;

	private Rect rectCestino = null;
	private Rect rectElemento = null;
	private Rect rectCella = null;
	private Rect rectCellaOriginal = null;

	private int idLocale = 0;

	boolean inpianta = true;
	boolean incestino = false;

	private boolean dragPorteFinestre = false;
	private boolean dragElementi = false;

	Handler hndl = new Handler();

	private boolean postLayout = true;
	private long lastPostLayout = 0;

	private int elementoSelezionato = -1;

	int[] posizionepiantina = new int[2];
	int[] posizionecestino = new int[2];
	int[] xyroot = new int[2];

	boolean vibrazioneEseguita = false;
	private LayoutInflater li = null;
	private View viewToast = null;
	private TextView textViewToast = null;
	private Toast t = null;

	private int idPreventivoSelezionato = 0;

	private boolean refreshElementi = true;

	public EConTabPiantinaGridView(Context context) {
		super(context);
		init();
	}

	public EConTabPiantinaGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public EConTabPiantinaGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();

	}

	private void init() {
		setVerticalScrollBarEnabled(false);
		setVerticalSpacing(0);
		setHorizontalSpacing(0);
		setStretchMode(GridView.STRETCH_COLUMN_WIDTH);

		rectCestino = new Rect();
		rectElemento = new Rect();
		rectCella = new Rect();
		rectCellaOriginal = new Rect();
		li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		viewToast = li.inflate(R.layout.econtab_toast_layout, null);
		textViewToast = (TextView) viewToast.findViewById(R.id.textView_toast);
	}

	public boolean isInModifica() {
		return inModifica;
	}

	public void setInModifica(boolean inModifica) {
		this.inModifica = inModifica;
		if (!inModifica) {

			addOnLayoutChangeListener(new OnLayoutChangeListener() {

				@Override
				public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight,
						int oldBottom) {
					if (!postLayout) {
						long now = System.currentTimeMillis();
						if (now - lastPostLayout > 500) {
							postLayout = true;
						}
					}

					if (postLayout) {
						postLayout = false;
						lastPostLayout = System.currentTimeMillis();

						hndl.post(new Runnable() {

							@Override
							public void run() {
								// TODO Auto-generated method stub
								root.invalidate();
								root.requestLayout();
							}
						});
					}

				}
			});
		}

	}

	public void setRoot(FrameLayout root) {

		this.root = (EConTabPiantinaFrameLayout) root;

	}

	public void creaImmagine() {
		if (root != null) {
			root.creaImmaginePiantina();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		if (!isInModifica() && !isInEditMode()) {

			if (widthMeasureSpec > heightMeasureSpec) {
				widthMeasureSpec = heightMeasureSpec;
			}
			if (heightMeasureSpec > widthMeasureSpec) {
				heightMeasureSpec = widthMeasureSpec;
			}

		}

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub

		super.onLayout(changed, l, t, r, b);
		if (!isInModifica() && !isInEditMode()) {
			int[] xy = getPosizionePiantina();
			if (elementiporte != null) {
				for (int i = 0; i < elementiporte.size(); i++) {
					final EConTabElementoPorta elem = elementiporte.get(i);

					elem.setPosXPiantina(xy[0]);
					elem.setPosYPiantina(xy[1]);
					elem.setLarghezzaPiantina(getMeasuredWidth());
					elem.setAltezzaPiantina(getMeasuredHeight());
					if (elem.getParent() == null) {
						root.addView(elem);

					}
					if (!elem.isOnDrag()) {
						// ricalcolaPosizione(elem, rectElemento);
						calcolaPosizioneXYCella(elem);
						elem.ricalcolaPosizione();
					}
					elem.invalidate();

				}

			}

			if (elementi != null ) {
				for (int i = 0; i < elementi.size(); i++) {
					final EConTabElementoElettrico elem = elementi.get(i);
					if (elem.isAbilitaLayout()) {
						elem.setPosXPiantina(xy[0]);
						elem.setPosYPiantina(xy[1]);
                        if (elem.getParent() == null) {
                            root.addView(elem);
                        }
                        //se la largehezza e l'altezza sono uguali per ottimizzare le operazioni grafiche non ridisegno l'elemento
                        if (elem.getLarghezzaPiantina()!=getMeasuredWidth() || elem.getAltezzaPiantina()!=getMeasuredHeight()){
                            elem.setLarghezzaPiantina(getMeasuredWidth());
                            elem.setAltezzaPiantina(getMeasuredHeight());

                            if (!elem.isOnDrag()) {
                                // ricalcolaPosizione(elem, rectElemento);
                                elem.ricalcolaPosizione();
                            }
                            elem.invalidate();
                        }

					}
				}
			}
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if (!isInModifica()) {

			return root.onTouchEvent(event);
		}
		return super.onTouchEvent(event);
	}

	public void aggiungiElementoPorta(EConTabElementoPorta elemento) {
		if (elementiporte == null) {
			elementiporte = new ArrayList<EConTabElementoPorta>();
		}

		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT);

		calcolaPosizioneIniziale(elemento);
		calcolaPosizioneXYCella(elemento);
		elemento.setLayoutParams(lp);

		// root.addView(elemento);

		elemento.setSelected(true);

		elemento.aggiungiInDb(getContext(), idLocale);

		elementiporte.add(elemento);
		requestLayout();
	}

	private void calcolaPosizioneIniziale(EConTabElementoPorta elemento) {
		// TODO Auto-generated method stub
		elemento.setCellaIniziale(0);
		elemento.setPosizioneInCella(EConTabElementoPorta.SOPRA);
		elemento.setPositionInizialePercent(25);
		for (int i = 0; i < getChildCount(); i++) {
			EConTabPiantinaImageView cella = (EConTabPiantinaImageView) getChildAt(i);
			if (cella.bordoSopra) {
				elemento.setCellaIniziale(i);
				elemento.setPosizioneInCella(EConTabElementoPorta.SOPRA);
				elemento.setPositionInizialePercent(25);

				break;
			}
		}
	}

	private void calcolaPosizioneXYCella(EConTabElementoPorta elemento) {
		// TODO Auto-generated method stub
		EConTabPiantinaImageView cella = (EConTabPiantinaImageView) getChildAt(elemento.getCellaIniziale());

		if (cella != null) {
			int[] xypiantina = getPosizionePiantina();

			int[] xy = new int[2];
			xy[0] = cella.getLeft() + xypiantina[0];
			xy[1] = cella.getTop() + xypiantina[1];

			elemento.setPosXCella(xy[0]);
			elemento.setPosYCella(xy[1]);
			elemento.setAltezzaCella(cella.getHeight());
			elemento.setLarghezzaCella(cella.getWidth());
		}
	}

	/**
	 * E' la posizione relativa rispetto alla root. mi serve per posizionare gli elementi in maniera esatta
	 * 
	 * @return
	 */
	private int[] getPosizionePiantina() {
		int[] xy = new int[2];
		getLocationInWindow(xy);

		int[] xy_root = new int[2];
		root.getLocationInWindow(xy_root);

		int[] xy_rel = new int[2];
		xy_rel[0] = xy[0] - xy_root[0];
		xy_rel[1] = xy[1] - xy_root[1];

		return xy_rel;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		// TODO Auto-generated method stub
		float x = event.getRawX();
		float y = event.getRawY();

		// Alcuni dati non variano durante il drag quindi li calcolo solo una
		// volta
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			getLocationInWindow(posizionepiantina);
			cestino.getLocationInWindow(posizionecestino);
			root.getLocationInWindow(xyroot);

			rectCestino.set(posizionecestino[0], posizionecestino[1], posizionecestino[0] + cestino.getWidth(), posizionecestino[1]
					+ cestino.getHeight());
		}

		int xroot = xyroot[0];
		int yroot = xyroot[1];

		float realX = x - xroot;
		float realy = y - yroot;

		if (v instanceof EConTabElementoPorta) {
			return dragELementoPorta(v, event, posizionecestino, realX, realy);
		}

		if (v instanceof EConTabElementoElettrico) {
			/*if (((EConTabElementoElettrico) v).isElementoAltroOrdine()){
				return false;
			}
			else{

			}*/
			return dragELementoElettrico(v, event, posizionecestino, realX, realy);
		}

		return false;
	}

	private boolean dragELementoPorta(View v, MotionEvent event, int posizionecestino[], float realX, float realy) {

		int[] posizioneelemento = new int[2];
		v.getLocationInWindow(posizioneelemento);

		EConTabElementoPorta elem = (EConTabElementoPorta) v;

		rectElemento.set(posizioneelemento[0] + elem.getPosizioneLeftImmagine(), posizioneelemento[1] + elem.getPosizioneTopImmagine(),
				posizioneelemento[0] + elem.getPosizioneLeftImmagine() + elem.getLarghezzaImmagine(),
				posizioneelemento[1] + elem.getPosizioneTopImmagine() + elem.getAltezzaImmagine());

		if (Rect.intersects(rectCestino, rectElemento)) {
			incestino = true;
			cestino.setImageResource(R.drawable.cestino_op);

		} else {
			incestino = false;
			cestino.setImageResource(R.drawable.cestino);
		}

		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) elem.getLayoutParams();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {

			if (elem.isSelected()) {
				elem.setSelected(false);
			} else {
				elem.setSelected(true);
			}

			startDragTime = System.currentTimeMillis();
			// elem.setOnDrag(true);
			getViewPager().requestDisallowInterceptTouchEvent(true);

			break;
		}
		case MotionEvent.ACTION_MOVE: {
			long endDragTime = System.currentTimeMillis();
			if (endDragTime - startDragTime > 100) {
				elem.setSelected(true);
				elem.setOnDrag(true);

				lp.leftMargin = (int) realX + root.getOffsetX() - elem.getWidth() / 2;
				lp.topMargin = (int) realy + root.getOffsetY() - elem.getHeight() - elem.getPaddingTop();
				lp.rightMargin = -(350);
				lp.bottomMargin = -(350);

				if (incestino) {

					elem.setBackgroundResource(R.drawable.bg_econtab_drag_ko);
				} else {
					elem.setBackgroundResource(R.drawable.bg_econtab_drag_ok);
				}

				// invece di ridisegnare tutto l'albero delle view (piantina
				// ecc..) ridisegno solo gli elementi nella nuova posizione
				// cos� � molto pi� veloce
				elem.layout(lp.leftMargin - root.getOffsetX(), lp.topMargin - root.getOffsetY(),
						lp.leftMargin - root.getOffsetX() + elem.getWidth(), lp.topMargin - root.getOffsetY() + elem.getHeight());

			}

			break;
		}
		case MotionEvent.ACTION_UP: {

			getViewPager().requestDisallowInterceptTouchEvent(false);

			if (incestino) {
				eliminaElementoPorta(elem);
				cestino.setImageResource(R.drawable.cestino);
			} else {

				if (elem.isOnDrag()) {
					elem.setOnDrag(false);
					ricalcolaParametriPosizioneElemento(elem, rectElemento);

				}
				elem.invalidate();
				elem.requestLayout();
			}

			break;
		}
		}
		return true;
	}

	private boolean dragELementoElettrico(View v, MotionEvent event, int posizionecestino[], float realX, float realy) {

		int[] posizioneelemento = new int[2];
		v.getLocationInWindow(posizioneelemento);

		EConTabElementoElettrico elem = (EConTabElementoElettrico) v;

		rectElemento.set(posizioneelemento[0], posizioneelemento[1], posizioneelemento[0] + elem.getWidth(),
				posizioneelemento[1] + elem.getHeight());

		if (Rect.intersects(rectCestino, rectElemento)) {
			incestino = true;
			cestino.setImageResource(R.drawable.cestino_op);

		} else {
			incestino = false;
			cestino.setImageResource(R.drawable.cestino);
		}

		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) elem.getLayoutParams();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {

			if (elem.isSelected()) {
				elem.setSelected(false);
			} else {
				elem.setSelected(true);
			}

			elem.setOnDrag(true);
			for (int i = 0; i < elementi.size(); i++) {
				if (elementi.get(i).getIdElemento() != elem.getIdElemento()) {
					elementi.get(i).setAbilitaLayout(false);
				}
			}
			if (vibrazioneEseguita == false) {
				elem.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
				vibrazioneEseguita = true;

				if (t == null) {
					t = Toast.makeText(getContext(), "", Toast.LENGTH_SHORT);
					t.setView(viewToast);
					int screenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
					int offsety = (15 * screenHeight) / 100;
					t.setGravity(Gravity.TOP, 0, offsety);
				}

				textViewToast.setText(elem.getNumero() + " - " + elem.getDescrizione());
				t.show();

			}

			getViewPager().requestDisallowInterceptTouchEvent(true);

			break;
		}
		case MotionEvent.ACTION_MOVE: {
			elem.setInMovimento(true);

            if (!elem.isOnFront()){
                elem.setOnFront(true);
            }


			lp.leftMargin = (int) realX + root.getOffsetX() - elem.getWidth() / 2;
			lp.topMargin = (int) realy + root.getOffsetY() - 2 * elem.getHeight() - elem.getPaddingTop();
			lp.rightMargin = -(350);
			lp.bottomMargin = -(350);

			// invece di ridisegnare tutto l'albero delle view (piantina
			// ecc..) ridisegno solo gli elementi nella nuova posizione
			// cos� � molto pi� veloce
			elem.layout(lp.leftMargin - root.getOffsetX(), lp.topMargin - root.getOffsetY(),
					lp.leftMargin - root.getOffsetX() + elem.getWidth(), lp.topMargin - root.getOffsetY() + elem.getHeight());

			break;
		}
		case MotionEvent.ACTION_UP: {
			for (int i = 0; i < elementi.size(); i++) {

				elementi.get(i).setAbilitaLayout(true);

			}
			elem.setSelected(false);
			vibrazioneEseguita = false;

			getViewPager().requestDisallowInterceptTouchEvent(false);

			if (incestino) {
				eliminaElementoElettrico(elem);
				cestino.setImageResource(R.drawable.cestino);
			} else {

				if (elem.isOnDrag()) {
					elem.setOnDrag(false);
                    elem.setOnFront(false);
					elem.setOffsetRootX(root.getOffsetX());
					elem.setOffsetRootY(root.getOffsetY());

					elem.ricalcolaPosizioneInversa();
					elem.aggiornaDb(getContext());
				}
				elem.invalidate();
				elem.requestLayout();
			}
			if (!elem.isInMovimento()) {
				elem.mostraPopup();
			}
			elem.setInMovimento(false);
			break;
		}
		}
		return true;
	}

	private void eliminaElementoElettrico(final EConTabElementoElettrico elem) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		Utility.mostraConfermaCancellazioneDialog(getContext(), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if (which == Dialog.BUTTON_POSITIVE) {
					elementi.remove(elem);
					root.removeView(elem);
					elem.cancellaDaDb(getContext());
				} else {
					elem.setOnDrag(false);
					elem.setOffsetRootX(root.getOffsetX());
					elem.setOffsetRootY(root.getOffsetY());
					elem.ricalcolaPosizione();
					elem.invalidate();
					elem.requestLayout();
				}
			}
		});

	}

	private void ricalcolaParametriPosizioneElemento(EConTabElementoPorta elemento, Rect rectElemento) {
		// TODO Auto-generated method stub
		int[] xypiantina = new int[2];
		getLocationInWindow(xypiantina);
		int padding = getResources().getDimensionPixelSize(R.dimen.padding_drag);
		for (int i = 0; i < getChildCount(); i++) {
			EConTabPiantinaImageView cella = (EConTabPiantinaImageView) getChildAt(i);

			int[] xycella = new int[2];

			xycella[0] = cella.getLeft() + xypiantina[0];
			xycella[1] = cella.getTop() + xypiantina[1];

			rectCella.set(xycella[0], xycella[1], xycella[0] + cella.getWidth(), xycella[1] + cella.getHeight());

			rectCellaOriginal.set(rectCella);

			if (elemento.getOrientamento() == EConTabElementoPorta.VERTICALE) {
				if (cella.bordoDestra || cella.bordoSinistra) {
					if (rectCella.intersect(rectElemento)) {
						elemento.setCellaIniziale(i);
						int posizionePercentuale = ((rectElemento.top - rectCellaOriginal.top) * 100) / rectCellaOriginal.height();

						if (cella.bordoSinistra && cella.bordoDestra) {
							if ((rectElemento.left - rectCellaOriginal.left) > rectCellaOriginal.width() / 2) {
								elemento.setPosizioneInCella(EConTabElementoPorta.DESTRA);
							} else {
								elemento.setPosizioneInCella(EConTabElementoPorta.SINISTRA);
							}
						} else {
							if (cella.bordoDestra) {
								elemento.setPosizioneInCella(EConTabElementoPorta.DESTRA);
							} else {
								elemento.setPosizioneInCella(EConTabElementoPorta.SINISTRA);
							}
						}

						elemento.setPositionInizialePercent(posizionePercentuale);
						break;
					}

				}
			}

			else {
				if (cella.bordoSopra || cella.bordoSotto) {
					if (rectCella.intersect(rectElemento)) {
						elemento.setCellaIniziale(i);
						int posizionePercentuale = ((rectElemento.left - rectCellaOriginal.left) * 100) / rectCellaOriginal.width();

						// se ha il bordo sia sopra che sotto allora devo
						// guradare la met� cella
						if (cella.bordoSopra && cella.bordoSotto) {
							if ((rectElemento.top - rectCellaOriginal.top) > rectCellaOriginal.height() / 2) {
								elemento.setPosizioneInCella(EConTabElementoPorta.SOTTO);
							} else {
								elemento.setPosizioneInCella(EConTabElementoPorta.SOPRA);
							}
						} else {
							if (cella.bordoSopra) {
								elemento.setPosizioneInCella(EConTabElementoPorta.SOPRA);
							} else {
								elemento.setPosizioneInCella(EConTabElementoPorta.SOTTO);
							}
						}

						elemento.setPositionInizialePercent(posizionePercentuale);
						break;
					}

				}
			}

		}

		elemento.aggiornaDb(getContext());

		calcolaPosizioneXYCella(elemento);
	}

	private void eliminaElementoPorta(EConTabElementoPorta elem) {
		// TODO Auto-generated method stub
		elementiporte.remove(elem);
		root.removeView(elem);
		elem.cancellaDaDb(getContext());
	}

	public EConTabLocaleViewPager getViewPager() {
		return viewPager;
	}

	public void setViewPager(EConTabLocaleViewPager viewPager) {
		this.viewPager = viewPager;
		this.root.setViewPager(viewPager);
	}

	public ImageView getCestino() {
		return cestino;
	}

	public void setCestino(ImageView cestino) {
		this.cestino = cestino;
	}

	public int getIdLocale() {
		return idLocale;
	}

	public void setIdLocale(int idLocale) {
		this.idLocale = idLocale;
		if (root != null) {
			root.setIdLocale(idLocale);
		}
		aggiornaElementiPorte();

	}

	public void aggiornaElementiPorte() {
		DbInterno db = new DbInterno(getContext());
		LocaliPorteFinestre tabporte = new LocaliPorteFinestre();
		ContentValues where = new ContentValues();
		where.put(LocaliPorteFinestre.ID_LOCALE, idLocale);
		ArrayList<Object> porte = db.eseguiSelect(tabporte, where, null);
		db.close();
		if (elementiporte == null) {
			elementiporte = new ArrayList<EConTabElementoPorta>();
		} else {
			for (int i = 0; i < elementiporte.size(); i++) {

				root.removeView(elementiporte.get(i));
			}
			elementiporte.clear();
		}
		for (int i = 0; i < porte.size(); i++) {

			ContentValues val = (ContentValues) porte.get(i);
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
					FrameLayout.LayoutParams.WRAP_CONTENT);

			EConTabElementoPorta elemento = new EConTabElementoPorta(getContext(), val.getAsString(LocaliPorteFinestre.TIPO));
			elemento.setIdElemento(val.getAsInteger(LocaliPorteFinestre.ID_PORTA_FINESTRA));
			elemento.setPositionInizialePercent(val.getAsInteger(LocaliPorteFinestre.POS_INIZIALE_PERC));
			elemento.setPosizioneInCella(val.getAsString(LocaliPorteFinestre.POS_IN_CELLA));
			elemento.setCellaIniziale(val.getAsInteger(LocaliPorteFinestre.CELLA));
			// calcolaPosizioneXYCella(elemento);
			elemento.setLayoutParams(lp);
			// root.addView(elemento);
			elementiporte.add(elemento);
		}
	}

	public void aggiornaElementi(int idPreventivo) {
		boolean aggiornaTutto=false;
		if (idPreventivoSelezionato!=idPreventivo){
			idPreventivoSelezionato = idPreventivo;
			aggiornaTutto = true;
		}

		DbInterno db = new DbInterno(getContext());

		ContentValues where = new ContentValues();
		where.put("id_preventivo",idPreventivo);
		ContentValues recPREV = db.getRecord(new Preventivi(),where);
		String filtro_prev = "";
		boolean preventivoAccettatoORifiutato = false;
		if (recPREV!=null && recPREV.getAsString("tipo").equals(Preventivi.TIPO_PREVENTIVO)){
			filtro_prev = " or elementi_cantiere.id_preventivo=" + idPreventivo;
			preventivoAccettatoORifiutato = recPREV.getAsString(Preventivi.STATO).equals(Preventivi.STATO_ACCETTATO)||recPREV.getAsString(Preventivi.STATO).equals(Preventivi.STATO_RIFIUTATO);
		}

		String SQL = "";
		if (preventivoAccettatoORifiutato){
			SQL = "Select elementi_cantiere.*,(select count(*) from componenti_cantiere inner join composizioni_cantiere on componenti_cantiere.id_componente_cant=composizioni_cantiere.id_componente_cant where composizioni_cantiere.id_elemento_cant=elementi_cantiere.id_elemento_cant and componenti_cantiere.id_preventivo="+ Sessione.getIdPreventivoSelezionato()+") as num_componenti_prev_selezionato,elementi.icona,elementi.id_categoria_generale,(select count(*) from collegamenti where id_elemento_cant1=id_elemento_cant and id_cavo<>0) as collegamenti_daCavi,(select count(*) from collegamenti where id_elemento_cant2=id_elemento_cant and id_cavo<>0) as collegamenti_aCavi,(select count(*) from collegamenti where id_elemento_cant1=id_elemento_cant and id_tubo<>0) as collegamenti_daTubi,(select count(*) from collegamenti where id_elemento_cant2=id_elemento_cant and id_tubo<>0) as collegamenti_aTubi,(select count(*) from relazioni where id_elemento_cant1=id_elemento_cant or id_elemento_cant2=id_elemento_cant) as relazioni from elementi_cantiere left join elementi on elementi_cantiere.id_elemento=elementi.id_elemento left join preventivi on elementi_cantiere.id_preventivo=preventivi.id_preventivo where id_locale = "+ getIdLocale() +" and elementi_cantiere.id_preventivo=" + idPreventivo;
		}
		else {
			 SQL = "Select elementi_cantiere.*,(select count(*) from componenti_cantiere inner join composizioni_cantiere on componenti_cantiere.id_componente_cant=composizioni_cantiere.id_componente_cant where composizioni_cantiere.id_elemento_cant=elementi_cantiere.id_elemento_cant and componenti_cantiere.id_preventivo="+ Sessione.getIdPreventivoSelezionato()+") as num_componenti_prev_selezionato,elementi.icona,elementi.id_categoria_generale,(select count(*) from collegamenti where id_elemento_cant1=id_elemento_cant and id_cavo<>0) as collegamenti_daCavi,(select count(*) from collegamenti where id_elemento_cant2=id_elemento_cant and id_cavo<>0) as collegamenti_aCavi,(select count(*) from collegamenti where id_elemento_cant1=id_elemento_cant and id_tubo<>0) as collegamenti_daTubi,(select count(*) from collegamenti where id_elemento_cant2=id_elemento_cant and id_tubo<>0) as collegamenti_aTubi,(select count(*) from relazioni where id_elemento_cant1=id_elemento_cant or id_elemento_cant2=id_elemento_cant) as relazioni from elementi_cantiere left join elementi on elementi_cantiere.id_elemento=elementi.id_elemento left join preventivi on elementi_cantiere.id_preventivo=preventivi.id_preventivo where id_locale = "+ getIdLocale() +" and (elementi_cantiere.id_preventivo=0 or preventivi.tipo='O' "+filtro_prev+")";
		}

		ArrayList<Object> elementiCantiereALl = db.eseguiSelect(SQL, null);

		db.close();

		//Se ho seleionato un preventivo nascondo tutti gli elementi che sono stati copiati nel preventivo corrente
		ArrayList<Object> elementiCantiere = new ArrayList<Object>();
		if (recPREV!=null && recPREV.getAsString("tipo").equals(Preventivi.TIPO_PREVENTIVO)){
			ArrayList<Integer> idDaEliminare = new ArrayList<Integer>();
			for (int i=0;i<elementiCantiereALl.size();i++){
				ContentValues curr = (ContentValues) elementiCantiereALl.get(i);
				if (curr.get(ElementiCantiere.ID_ELEMENTO_CANT_ORIGINE)!=null){
					int idOrigine = curr.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT_ORIGINE);
					idDaEliminare.add(idOrigine);
				}
			}

			for (int i=0;i<elementiCantiereALl.size();i++){
				ContentValues curr = (ContentValues) elementiCantiereALl.get(i);
				if (!idDaEliminare.contains(curr.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT))){
					elementiCantiere.add(curr);
				}
			}
		}
		else {
			elementiCantiere.addAll(elementiCantiereALl);
		}

		if (elementi == null) {
			elementi = new ArrayList<EConTabElementoElettrico>();
		} else {

			ArrayList<EConTabElementoElettrico> elementiClone = new ArrayList<EConTabElementoElettrico>();
			elementiClone.addAll(elementi);
			for (int i = 0; i < elementiClone.size(); i++) {
				_controllaAggiornaElemento(elementiClone.get(i), elementiCantiere,aggiornaTutto);

			}
			elementiClone.clear();
			elementiClone = null;
		}

		for (int i = 0; i < elementiCantiere.size(); i++) {

			ContentValues val = (ContentValues) elementiCantiere.get(i);
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
					FrameLayout.LayoutParams.WRAP_CONTENT);

			EConTabElementoElettrico elemento = new EConTabElementoElettrico(getContext());
			elemento.setNumeroComponentiPreventivoSelezionato(val.getAsInteger("num_componenti_prev_selezionato"));
			elemento.setPiantina(this);
			elemento.setIdElemento(val.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT));
			elemento.setDescrizione(val.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
			elemento.setPositionPercentX(val.getAsInteger(ElementiCantiere.POS_X));
			elemento.setPositionPercentY(val.getAsInteger(ElementiCantiere.POS_Y));
			elemento.setIdPreventivo(val.getAsInteger(ElementiCantiere.ID_PREVENTIVO));
			elemento.setIdPreventivoSelezionato(getIdPreventivoSelezionato());
			// DL: bad fix
			/*
			try {
				elemento.setIdCategoria(val.getAsInteger(Elementi.ID_CATEGORIA_GENERALE));
			}
			catch(Exception e)
			{
				elemento.setIdCategoria(1);
			}
			*/
			elemento.setIdCategoria(val.getAsInteger(Elementi.ID_CATEGORIA_GENERALE));
			elemento.setIcona(val.getAsString(Elementi.ICONA));
			elemento.setNumero("" + val.getAsInteger(ElementiCantiere.NUMERO_IDENTIFICATIVO));
			int collegamentiDaCavi = val.getAsInteger("collegamenti_daCavi");
			int collegamentiACavi = val.getAsInteger("collegamenti_aCavi");
			int collegamentiTotCavi = collegamentiACavi + collegamentiDaCavi;

			int collegamentiDaTubi = val.getAsInteger("collegamenti_daTubi");
			int collegamentiATubi = val.getAsInteger("collegamenti_aTubi");
			int collegamentiTotTubi = collegamentiATubi + collegamentiDaTubi;

            int relazioni = val.getAsInteger("relazioni");

			elemento.setNumeroCollegamenti(collegamentiTotCavi, collegamentiTotTubi);

            elemento.setNumeroRelazioni(relazioni);
			elemento.setOnClickListener(this);
			// calcolaPosizioneXYCella(elemento);
			elemento.setLayoutParams(lp);

			elementi.add(elemento);
		}

		elementoSelezionato = -1;

	}

	private void _controllaAggiornaElemento(EConTabElementoElettrico elem, ArrayList<Object> elementiCantiere,boolean aggiornaTutto) {
		// TODO Auto-generated method stub
		boolean daRimuovere = true;
		if (!aggiornaTutto){
			boolean trovato = false;
			for (int i = 0; i < elementiCantiere.size(); i++) {
				ContentValues valCurr = (ContentValues) elementiCantiere.get(i);
				int idElementoCurr = valCurr.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT);
				if (elem.getIdElemento() == idElementoCurr) {
					trovato = true;
					String numero = "" + valCurr.getAsInteger(ElementiCantiere.NUMERO_IDENTIFICATIVO);
					int collegamentiDaCavi = valCurr.getAsInteger("collegamenti_daCavi");
					int collegamentiACavi = valCurr.getAsInteger("collegamenti_aCavi");
					int collegamentiTotCavi = collegamentiACavi + collegamentiDaCavi;

					int collegamentiDaTubi = valCurr.getAsInteger("collegamenti_daTubi");
					int collegamentiATubi = valCurr.getAsInteger("collegamenti_aTubi");
					int collegamentiTotTubi = collegamentiATubi + collegamentiDaTubi;

					int relazioni = valCurr.getAsInteger("relazioni");
					if (elem.getNumero().equals(numero) && elem.getNumeroCollegamentiCavi() == collegamentiTotCavi
							&& elem.getNumeroCollegamentiTubi() == collegamentiTotTubi && elem.getNumeroRelazioni()==relazioni ) {
						daRimuovere = false;
						elementiCantiere.remove(i);
					}
				}

				if (trovato) {
					break;
				}
			}
		}



		if (daRimuovere) {
			root.removeView(elem);
			elementi.remove(elem);
		} else {
			elem.setSelected(false);
		}

	}

	public boolean isDragPorteFinestre() {
		return dragPorteFinestre;

	}

	public void setDragPorteFinestre(boolean dragPorteFinestre) {
		this.dragPorteFinestre = dragPorteFinestre;
		if (getViewPager() != null) {
			if (dragPorteFinestre) {
				//getViewPager().setAbilitato(false);
			} else {
				//getViewPager().setAbilitato(true);
			}
		}
		if (elementiporte != null) {
			for (int i = 0; i < elementiporte.size(); i++) {
				if (dragPorteFinestre) {
					elementiporte.get(i).setOnTouchListener(this);
				} else {
					elementiporte.get(i).setOnTouchListener(null);
					elementiporte.get(i).setSelected(false);
				}

			}
		}
	}

	public boolean isDragELementi() {
		return dragElementi;

	}

	public void setDragElementi(boolean dragElementi) {
		this.dragElementi = dragElementi;
		if (getViewPager() != null) {
			if (dragElementi || root.isInzoom()) {
				//getViewPager().setAbilitato(false);
			} else {
				//getViewPager().setAbilitato(true);
			}
		}

		if (elementi != null) {
			for (int i = 0; i < elementi.size(); i++) {
				if (dragElementi) {
					elementi.get(i).setOnTouchListener(this);
				} else {
					elementi.get(i).setOnTouchListener(null);
					elementi.get(i).setSelected(false);
				}

			}
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		v.setSelected(!v.isSelected());
		if (elementoSelezionato >= 0) {
			elementi.get(elementoSelezionato).setSelected(false);
		}

		if (v.isSelected()) {
			elementoSelezionato = elementi.indexOf(v);
			elementi.get(elementoSelezionato).mostraPopup();
		} else {
			elementoSelezionato = -1;
		}

	}

	/**
	 * Non pi� usato (serviva quando mostravo il dialog al posto dell'activity)
	 * 
	 */
	/*
	 * public void mostraPopupElementoSelezionato() { // TODO Auto-generated method stub if (elementoSelezionato >= 0 &&
	 * elementi.size() > elementoSelezionato && elementi.get(elementoSelezionato).isPopupVisibile()) {
	 * elementi.get(elementoSelezionato).mostraPopup(); } }
	 */

	/**
	 * ritorna la lista degli id degli elementi visualizzati nella piantina
	 * 
	 * @return
	 */
	public ArrayList<Integer> getListaIdElementi() {
		ArrayList<Integer> lista = new ArrayList<Integer>();
		for (int i = 0; i < elementi.size(); i++) {
			lista.add(elementi.get(i).getIdElemento());
		}
		return lista;
	}


    public ArrayList<EConTabElementoElettrico> getListaElementi(){
        return  elementi;
    }

	public int getIdPreventivoSelezionato() {
		return idPreventivoSelezionato;
	}

	public boolean isRefreshElementi() {
		return refreshElementi;
	}

	public void setRefreshElementi(boolean refreshElementi) {
		this.refreshElementi = refreshElementi;
	}
}
