package com.ncfsistemi.econ.views;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class EconLocaleViewPager extends ViewPager {

	private boolean abilitato = true;

	public EconLocaleViewPager(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public EconLocaleViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (isAbilitato()) {
			return super.onTouchEvent(event);
		}

		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (isAbilitato()) {
			return super.onInterceptTouchEvent(event);
		}

		return false;
	}

	public boolean isAbilitato() {
		return abilitato;
	}

	public void setAbilitato(boolean abilitato) {
		this.abilitato = abilitato;
	}

}
