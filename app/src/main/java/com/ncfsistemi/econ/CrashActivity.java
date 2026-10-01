package com.ncfsistemi.econ;

import android.app.Activity;
import android.content.ClipboardManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ncfsistemi.econ.utils.Utility;

public class CrashActivity extends Activity {
	TextView testo = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_crash);
		String messaggio = getIntent().getStringExtra("ERRORE");
		testo = (TextView)findViewById(R.id.textView1);
		testo.setText(messaggio);
		registerForContextMenu(testo);
		
	}

	public void chiudi(View v){
		System.exit(0);
	}
	
	public void seleziona(View v){
		((TextView)v).setSelectAllOnFocus(true);
		v.requestFocus();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
	        
	        menu.setHeaderTitle("Crash report").add(0, 0, 0, "Copia testo errore");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
	
	    ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setText(testo.getText());
	    Toast.makeText(this, "Testo copiato negli appunti", Toast.LENGTH_SHORT).show();
	    return true;
	}

    public void invia_mail(View view) {
		// all'assistenza dell'editore (utils.Editore), con i dati del dispositivo e delle versioni
		com.ncfsistemi.econ.utils.Editore.scriviAssistenza(this, "Econ: errore dell'app", testo.getText().toString());
    }
}
