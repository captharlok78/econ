package pfa.app.econtab.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;

import java.io.ByteArrayOutputStream;

/**
 * Area in cui il cliente firma con il dito (FirmaActivity, GESTIONE_RAPPORTINI.md §13): tratto nero morbido su fondo
 * bianco. {@link #png()} ritorna la firma ritagliata sul disegno, rimpicciolita se serve, in PNG base64.
 */
public class FirmaView extends View {

	/** Larghezza massima dell'immagine salvata: basta per la scheda e il PDF e tiene piccolo il dato da sincronizzare. */
	private static final int LARGHEZZA_MAX = 900;

	private final Path tratto = new Path();
	private final Paint penna = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final RectF limiti = new RectF();
	private float ultimoX, ultimoY;
	private boolean vuota = true;
	private Runnable alPrimoTratto;

	public FirmaView(Context context, AttributeSet attrs) {
		super(context, attrs);
		penna.setColor(Color.BLACK);
		penna.setStyle(Paint.Style.STROKE);
		penna.setStrokeJoin(Paint.Join.ROUND);
		penna.setStrokeCap(Paint.Cap.ROUND);
		penna.setStrokeWidth(3.5f * getResources().getDisplayMetrics().density);
		setBackgroundColor(Color.WHITE);
	}

	/** Chiamato al primo tratto (per nascondere il suggerimento e abilitare Salva). */
	public void setAlPrimoTratto(Runnable r) {
		alPrimoTratto = r;
	}

	public boolean isVuota() {
		return vuota;
	}

	public void pulisci() {
		tratto.reset();
		vuota = true;
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawPath(tratto, penna);
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		float x = e.getX(), y = e.getY();
		switch (e.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				getParent().requestDisallowInterceptTouchEvent(true);
				tratto.moveTo(x, y);
				tratto.lineTo(x + 0.1f, y + 0.1f); // un tocco lascia un punto
				ultimoX = x;
				ultimoY = y;
				if (vuota) {
					vuota = false;
					if (alPrimoTratto != null) alPrimoTratto.run();
				}
				break;
			case MotionEvent.ACTION_MOVE:
				// i punti intermedi (storici) rendono il tratto continuo anche con movimenti veloci
				for (int i = 0; i < e.getHistorySize(); i++) {
					linea(e.getHistoricalX(i), e.getHistoricalY(i));
				}
				linea(x, y);
				break;
			default:
				return true;
		}
		invalidate();
		return true;
	}

	private void linea(float x, float y) {
		tratto.quadTo(ultimoX, ultimoY, (x + ultimoX) / 2, (y + ultimoY) / 2);
		ultimoX = x;
		ultimoY = y;
	}

	/** La firma in PNG base64 (senza prefisso "data:"), null se non c'e' niente di disegnato. */
	public String png() {
		if (vuota || getWidth() == 0 || getHeight() == 0) return null;
		tratto.computeBounds(limiti, true);
		float margine = penna.getStrokeWidth() * 3;
		int sx = Math.max(0, (int) (limiti.left - margine));
		int sy = Math.max(0, (int) (limiti.top - margine));
		int dx = Math.min(getWidth(), (int) (limiti.right + margine));
		int dy = Math.min(getHeight(), (int) (limiti.bottom + margine));
		if (dx <= sx || dy <= sy) return null;

		Bitmap tutta = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(tutta);
		c.drawColor(Color.WHITE);
		c.drawPath(tratto, penna);
		Bitmap ritaglio = Bitmap.createBitmap(tutta, sx, sy, dx - sx, dy - sy);
		if (ritaglio.getWidth() > LARGHEZZA_MAX) {
			int altezza = Math.max(1, Math.round(ritaglio.getHeight() * (LARGHEZZA_MAX / (float) ritaglio.getWidth())));
			ritaglio = Bitmap.createScaledBitmap(ritaglio, LARGHEZZA_MAX, altezza, true);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ritaglio.compress(Bitmap.CompressFormat.PNG, 100, out);
		return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
	}
}
