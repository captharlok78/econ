package pfa.app.econtab;

import android.app.Activity;
import android.content.ClipboardManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import pfa.app.econtab.utils.Utility;

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
		String publisher_email_address = "";
		publisher_email_address = pfa.app.econtab.Globals.PUBLISHER_EMAIL_ADDRESS;
		Utility.inviaMail(this,publisher_email_address,"ECONTAB CRASH (API"+ Build.VERSION.SDK_INT+ ",MODELLO "+ Build.MODEL+")",testo.getText().toString());
    }
}
