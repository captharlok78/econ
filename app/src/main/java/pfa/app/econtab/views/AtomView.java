package pfa.app.econtab.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class AtomView extends View {

    private final Paint orbitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nucleusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float animAngle = 0f;
    private ValueAnimator animator;

    // Two orbital planes at 0° and 60°
    private static final double THETA1 = 0.0;
    private static final double THETA2 = Math.PI / 3.0;

    public AtomView(Context context) {
        super(context);
        init();
    }

    public AtomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AtomView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        orbitPaint.setStyle(Paint.Style.STROKE);
        orbitPaint.setColor(Color.argb(110, 80, 170, 255));
        orbitPaint.setStrokeWidth(2f);

        nucleusPaint.setStyle(Paint.Style.FILL);
        nucleusPaint.setColor(Color.argb(255, 120, 220, 255));

        glowPaint.setStyle(Paint.Style.FILL);

        animator = ValueAnimator.ofFloat(0f, (float) (2 * Math.PI));
        animator.setDuration(3800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            animAngle = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        float cx = w / 2f;
        float cy = h * 0.42f;
        float unit = Math.min(w, h);

        float a = unit * 0.30f;
        float b = unit * 0.10f;
        float nr = unit * 0.048f;
        float er = unit * 0.020f;

        // Orbits
        drawOrbit(canvas, cx, cy, a, b, THETA1);
        drawOrbit(canvas, cx, cy, a, b, THETA2);

        // Nucleus glow
        for (int i = 5; i >= 1; i--) {
            glowPaint.setColor(Color.argb(15 * i, 80, 190, 255));
            canvas.drawCircle(cx, cy, nr * (1f + i * 0.55f), glowPaint);
        }
        canvas.drawCircle(cx, cy, nr, nucleusPaint);

        // Electron 1 on orbit 1
        drawElectron(canvas, cx, cy, a, b, THETA1, animAngle, er);
        // Electron 2 on orbit 2, offset by ~140°
        drawElectron(canvas, cx, cy, a, b, THETA2, animAngle + (float) (Math.PI * 0.78), er);
    }

    private void drawOrbit(Canvas canvas, float cx, float cy,
                           float a, float b, double theta) {
        Path path = new Path();
        int steps = 120;
        for (int i = 0; i <= steps; i++) {
            double t = 2 * Math.PI * i / steps;
            float x = cx + (float) (a * Math.cos(t) * Math.cos(theta) - b * Math.sin(t) * Math.sin(theta));
            float y = cy + (float) (a * Math.cos(t) * Math.sin(theta) + b * Math.sin(t) * Math.cos(theta));
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.close();
        canvas.drawPath(path, orbitPaint);
    }

    private void drawElectron(Canvas canvas, float cx, float cy,
                              float a, float b, double theta, float t, float er) {
        float x = cx + (float) (a * Math.cos(t) * Math.cos(theta) - b * Math.sin(t) * Math.sin(theta));
        float y = cy + (float) (a * Math.cos(t) * Math.sin(theta) + b * Math.sin(t) * Math.cos(theta));

        // Glow layers
        for (int i = 4; i >= 1; i--) {
            glowPaint.setColor(Color.argb(22 * i, 255, 235, 80));
            canvas.drawCircle(x, y, er * (1f + i * 0.7f), glowPaint);
        }

        // Electron core
        Paint ep = new Paint(Paint.ANTI_ALIAS_FLAG);
        ep.setStyle(Paint.Style.FILL);
        ep.setColor(Color.argb(255, 255, 240, 100));
        canvas.drawCircle(x, y, er, ep);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }
}
