package com.ncfsistemi.econ.utils;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.app.AppCompatViewInflater;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.button.MaterialButton;

/**
 * Inflater delle View dell'app (Theme.Econ, themes_econ.xml): i Button dei layout restano AppCompatButton (sfondi e
 * colori di sempre), ma i pulsanti dei dialoghi (android:id/button1..3) diventano MaterialButton, perche' i loro stili
 * Material 3 prendono il colore dal tema del pulsante (colorOnContainer) che solo MaterialButton applica. I dialoghi
 * usano l'inflater dell'activity, per questo la scelta si fa qui.
 */
public class EconViewInflater extends AppCompatViewInflater {

	private static final String ANDROID = "http://schemas.android.com/apk/res/android";

	@Override
	protected AppCompatButton createButton(Context context, AttributeSet attrs) {
		int id = attrs.getAttributeResourceValue(ANDROID, "id", 0);
		if (id == android.R.id.button1 || id == android.R.id.button2 || id == android.R.id.button3) {
			return new MaterialButton(context, attrs);
		}
		return super.createButton(context, attrs);
	}
}
