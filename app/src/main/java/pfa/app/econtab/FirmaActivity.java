package pfa.app.econtab;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pfa.app.econtab.views.FirmaView;

/**
 * Firma del cliente a schermo intero (GESTIONE_RAPPORTINI.md §13): nome di chi firma e firma con il dito. Salva ritorna
 * EXTRA_FIRMA (PNG base64) ed EXTRA_NOME; Annulla (e indietro) non cambia niente. Extra in ingresso: EXTRA_NOME per
 * proporre il nome, EXTRA_TITOLO per la testata.
 */
public class FirmaActivity extends AppCompatActivity {

	public static final String EXTRA_FIRMA = "firma";
	public static final String EXTRA_NOME = "firma_nome";
	public static final String EXTRA_TITOLO = "titolo";

	private FirmaView firma;
	private EditText nome;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_firma);
		String titolo = getIntent().getStringExtra(EXTRA_TITOLO);
		((TextView) findViewById(R.id.headerTitolo)).setText(titolo != null ? titolo : "Firma del cliente");
		nome = findViewById(R.id.edit_firma_nome);
		String proposto = getIntent().getStringExtra(EXTRA_NOME);
		if (proposto != null) nome.setText(proposto);
		firma = findViewById(R.id.firma_view);
		firma.setAlPrimoTratto(() -> {
			findViewById(R.id.text_firma_suggerimento).setVisibility(View.GONE);
			findViewById(R.id.button_salva_firma).setEnabled(true);
		});
	}

	public void pulisci(View v) {
		firma.pulisci();
		findViewById(R.id.text_firma_suggerimento).setVisibility(View.VISIBLE);
		findViewById(R.id.button_salva_firma).setEnabled(false);
	}

	public void salva(View v) {
		String png = firma.png();
		if (png == null) {
			Toast.makeText(this, "Manca la firma.", Toast.LENGTH_SHORT).show();
			return;
		}
		Intent risultato = new Intent();
		risultato.putExtra(EXTRA_FIRMA, png);
		risultato.putExtra(EXTRA_NOME, nome.getText().toString().trim());
		setResult(RESULT_OK, risultato);
		finish();
	}

	/** Annulla e freccia indietro della testata. */
	public void annulla(View v) {
		setResult(RESULT_CANCELED);
		finish();
	}
}
