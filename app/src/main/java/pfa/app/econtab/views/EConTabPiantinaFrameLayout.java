package pfa.app.econtab.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileOutputStream;

import pfa.app.econtab.utils.Utility;

public class EConTabPiantinaFrameLayout extends FrameLayout {

	private boolean inzoom = false;

	private GestureDetector detector = null;
	private ScaleGestureDetector scaleDetector = null;
	private float xPosition = 0;
	private float yPosition = 0;

	private int offsetX = 0;
	private int offsetY = 0;

	private boolean rimisura = true;

	private int larghezzaOriginale = 0;
	private int altezzaOriginale = 0;

	private int idLocale = 0;
	private float fattoreScala = 1f;
	private float fattoreScalaIniziale = 0f;

	private boolean onScale = false;

	private EConTabLocaleViewPager viewPager;

	private class GestureDoubleClickListener extends SimpleOnGestureListener {

		@Override
		public boolean onDown(MotionEvent e) {
			// TODO Auto-generated method stub

			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			// TODO Auto-generated method stub
			// creaImmaginePiantina();
			// Toast.makeText(getContext(), "Immagine generata " + getIdLocale(), Toast.LENGTH_SHORT).show();
			super.onLongPress(e);
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			// TODO Auto-generated method stub
			xPosition = e.getX();
			yPosition = e.getY();
			setInzoom(!isInzoom());
			if (isInzoom()) {
				fattoreScala = 1.5f;
				fattoreScalaIniziale = 0.5f;
				if (xPosition > getWidth() / 2) {

					offsetX = (int) (xPosition / fattoreScala);
				}

				if (yPosition > getHeight() / 2) {

					offsetY = (int) (yPosition / fattoreScala);
				}

			} else {
				fattoreScala = 1;
				fattoreScalaIniziale = 0f;
				offsetX = 0;
				offsetY = 0;
			}

			invalidate();
			requestLayout();

			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			// TODO Auto-generated method stub


			if (onScale) {
				return super.onScroll(e1, e2, velocityX, velocityY);
			}
			if (isInzoom()) {
				float x1 = e1.getX();
				float x2 = e2.getX();

				float y1 = e1.getY();
				float y2 = e2.getY();

				offsetX = offsetX + (int) (x1 - x2);
				offsetY = offsetY + (int) (y1 - y2);

				invalidate();
				requestLayout();
				return true;
			}

			else {
				scrollBy(0, 0);
				offsetX = 0;
				offsetY = 0;
			}
			return super.onScroll(e1, e2, velocityX, velocityY);
		}



	}

	public EConTabPiantinaFrameLayout(Context context) {
		super(context);
		init();
		// TODO Auto-generated constructor stub
	}

	public EConTabPiantinaFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
		// TODO Auto-generated constructor stub
	}

	public EConTabPiantinaFrameLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
		// TODO Auto-generated constructor stub

	}

	private void init() {
		// TODO Auto-generated method stub
		detector = new GestureDetector(getContext(), new GestureDoubleClickListener());
		scaleDetector = new ScaleGestureDetector(getContext(), new OnScaleGestureListener() {

			@Override
			public void onScaleEnd(ScaleGestureDetector detector) {
				// TODO Auto-generated method stub
				fattoreScalaIniziale = fattoreScala - 1;
				postDelayed(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						onScale = false;
					}
				}, 200);
			}

			@Override
			public boolean onScaleBegin(ScaleGestureDetector detector) {
				// TODO Auto-generated method stub
				xPosition = detector.getFocusX();
				yPosition = detector.getFocusY();

				onScale = true;
				return true;
			}

			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				// TODO Auto-generated method stub
				fattoreScala = fattoreScalaIniziale + detector.getScaleFactor();

				if (fattoreScala < 1) {
					fattoreScala = 1;
				}

				if (fattoreScala > 1) {

					setInzoom(true);
					if (fattoreScala > 2) {
						fattoreScala = 2;
					}
				} else {
					setInzoom(false);
				}
				invalidate();
				requestLayout();
				return false;
			}
		});

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub

		if (inzoom ) {
			widthMeasureSpec = MeasureSpec.makeMeasureSpec((int) (larghezzaOriginale * fattoreScala), MeasureSpec.EXACTLY);
			heightMeasureSpec = MeasureSpec.makeMeasureSpec((int) (altezzaOriginale * fattoreScala), MeasureSpec.EXACTLY);
			if (rimisura == true) {
				widthMeasureSpec = widthMeasureSpec + 1;
			}
			rimisura = !rimisura;
		} else {
			larghezzaOriginale = MeasureSpec.getSize(widthMeasureSpec);
			altezzaOriginale = MeasureSpec.getSize(heightMeasureSpec);
		}

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		// detector.onTouchEvent(event);
		// if (event.getY() > getResources().getDimensionPixelSize(R.dimen.altezza_campo_doppia)) {
		// return true;
		// }
		// return false;

		detector.onTouchEvent(event);

		scaleDetector.onTouchEvent(event);

		return true;
	}

	public boolean isInzoom() {
		return inzoom;
	}

	public void setInzoom(boolean inzoom) {
		// TODO Auto-generated method stub
		this.inzoom = inzoom;
		if (viewPager != null) {
			if (inzoom) {
				viewPager.setAbilitato(false);
			} else {
				viewPager.setAbilitato(true);
			}
		}

	}

	public int getOffsetX() {
		return offsetX;
	}

	public void setOffsetX(int offsetX) {
		this.offsetX = offsetX;
	}

	public int getOffsetY() {
		return offsetY;
	}

	public void setOffsetY(int offsetY) {
		this.offsetY = offsetY;
	}

	public void creaImmaginePiantina() {
		((ViewGroup) getChildAt(0)).getChildAt(0).setVisibility(View.INVISIBLE);// nascondo il cestion
		Bitmap b = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		draw(c);
		((ViewGroup) getChildAt(0)).getChildAt(0).setVisibility(View.VISIBLE);// rimostro il cestino
		FileOutputStream out = null;
		FileOutputStream outThumb = null;
		try {

			File fileIco = Utility.getFileImmaginePiantina(idLocale);
			File fileIcoThumb = Utility.getFileImmaginePiantinaThumb(idLocale);
			out = new FileOutputStream(fileIco);
			b.compress(Bitmap.CompressFormat.PNG, 90, out);

			outThumb = new FileOutputStream(fileIcoThumb);
			Bitmap scaledThumb = Bitmap.createScaledBitmap(b, 100, 100, true);
			scaledThumb.compress(Bitmap.CompressFormat.PNG, 90, outThumb);

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			try {
				out.close();
				outThumb.close();
			} catch (Throwable ignore) {

			}
		}

	}

	public int getIdLocale() {
		return idLocale;
	}

	public void setIdLocale(int idLocale) {
		this.idLocale = idLocale;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

		super.onLayout(changed, left, top, right, bottom);
		// offsetX = 0;
		// offsetY = 0;

		// TODO Auto-generated method stub

		if (inzoom) {

			for (int i = 0; i < getChildCount(); i++) {
				View child = (View) getChildAt(i);

				LayoutParams lp = (LayoutParams) child.getLayoutParams();
				int l = lp.leftMargin;
				// altezza e larghezza a questo punto sono gi� zoomate e
				// quindi
				// divido per trovare la met� divido per 3 e non per 2 (3 =
				// 2*1,5)

				l = l - offsetX;

				int t = lp.topMargin;

				t = t - (int) offsetY;


				child.layout(l, t, l + child.getWidth(), t + child.getHeight());

			}
		} else {
			offsetX = 0;
			offsetY = 0;
		}
	}

	public void setViewPager(EConTabLocaleViewPager viewPager) {
		// TODO Auto-generated method stub
		this.viewPager = viewPager;
	}

	// VECCHIO METODO ON LAYOUT SOSTITUITO DOPO ZOOM CON DUE DITA SPOSTAMENTO IN ZOOM
	// @Override
	// protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
	//
	// super.onLayout(changed, left, top, right, bottom);
	// offsetX = 0;
	// offsetY = 0;
	//
	// // TODO Auto-generated method stub
	// if (inzoom) {
	//
	// for (int i = 0; i < getChildCount(); i++) {
	// View child = (View) getChildAt(i);
	//
	// LayoutParams lp = (LayoutParams) child.getLayoutParams();
	// int l = lp.leftMargin;
	// // altezza e larghezza a questo punto sono gi� zoomate e
	// // quindi
	// // divido per trovare la met� divido per 3 e non per 2 (3 =
	// // 2*1,5)
	// if (xPosition > getWidth() / 3) {
	// l = l - (int) (xPosition / 1.5f);
	// offsetX = (int) (xPosition / 1.5f);
	// }
	//
	// int t = lp.topMargin;
	// if (yPosition > getHeight() / 3) {
	// t = t - (int) (yPosition / 1.5f);
	// offsetY = (int) (yPosition / 1.5f);
	// }
	//
	// child.layout(l, t, l + child.getWidth(), t + child.getHeight());
	//
	// }
	// }
	//
	// }

}
