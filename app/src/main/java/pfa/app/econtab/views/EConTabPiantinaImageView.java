package pfa.app.econtab.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.ImageView;

import pfa.app.econtab.R;

public class EConTabPiantinaImageView extends ImageView{
	
	public boolean bordoSopra = false;
	public boolean bordoSotto = false;
	public boolean bordoDestra = false;
	public boolean bordoSinistra = false;
	public boolean bordoPuntoSopraSinistra = false;
	public boolean bordoPuntoSopraDestra = false;
	public boolean bordoPuntoSottoSinistra = false;
	public boolean bordoPuntoSottoDestra = false;
	
	int padding = 0;
	
	Paint linePaint = null;
	
	public EConTabPiantinaImageView(Context context) {
		super(context);
		 padding = (int)context.getResources().getDimensionPixelSize(R.dimen.bordo_piantina);
		 linePaint = new Paint();
			linePaint.setStyle(Paint.Style.STROKE);
			linePaint.setStrokeWidth(padding);
			linePaint.setColor(Color.GRAY);
			
			
		// TODO Auto-generated constructor stub
	}
	
	
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		
		super.onDraw(canvas);
		
		
		if (bordoSinistra){
			
			canvas.drawLine(0, 0, 0, getHeight(), linePaint);
		}
		
		if (bordoDestra){
			
			canvas.drawLine(getWidth(), 0, getWidth(), getHeight(), linePaint);
		}
		
		if (bordoSopra){
			
			canvas.drawLine(0, 0, getWidth(), 0, linePaint);
		}
		if (bordoSotto){
			
			canvas.drawLine(0, getHeight(), getWidth(), getHeight(), linePaint);
		}
		
		if (bordoPuntoSopraSinistra){
			
			canvas.drawPoint(0, 0, linePaint);
		}
		if (bordoPuntoSopraDestra){
			
			canvas.drawPoint(getWidth(), 0, linePaint);
		}
		
		if (bordoPuntoSottoSinistra){
			
			canvas.drawPoint(0, getHeight(), linePaint);
		}
		
		if (bordoPuntoSottoDestra){
			
			canvas.drawPoint(getWidth(), getHeight(), linePaint);
		}
	}
	
}

