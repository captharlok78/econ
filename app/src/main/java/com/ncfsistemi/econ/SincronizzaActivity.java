package com.ncfsistemi.econ;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.UltimeSync;

/**
 * Sincronizzazione dal menu: due pulsanti a mezza riga, con sopra l'ultimo download e l'ultimo upload riusciti.
 * Il tocco avvia subito l'operazione (SincronizzazioneActivity con avvio automatico): Scarica = download; Carica =
 * invio delle modifiche del tablet e poi download degli aggiornamenti. Le impostazioni (server, immagini, test, reset)
 * sono in Impostazioni › Configurazione generale.
 */
public class SincronizzaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sincronizza);
        icona(findViewById(R.id.iconaScarica), FaIcone.SCARICA);
        icona(findViewById(R.id.iconaCarica), FaIcone.CARICA);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // anche al ritorno dalla sincronizzazione
        data(findViewById(R.id.textUltimoDownload), "Ultimo download", UltimeSync.etichettaDownload(this));
        data(findViewById(R.id.textUltimoUpload), "Ultimo upload", UltimeSync.etichettaUpload(this));
    }

    public void scarica(View v) {
        avvia(SincronizzazioneActivity.MODE_DOWNLOAD);
    }

    public void carica(View v) {
        avvia(SincronizzazioneActivity.MODE_UPLOAD);
    }

    public void chiudi(View v) {
        finish();
    }

    private void avvia(String modo) {
        Intent intent = new Intent(this, SincronizzazioneActivity.class);
        intent.putExtra(SincronizzazioneActivity.EXTRA_MODE, modo);
        intent.putExtra(SincronizzazioneActivity.EXTRA_AUTOMATICO, true);
        startActivity(intent);
    }

    private static void icona(TextView t, String glifo) {
        FaIcone.applica(t, glifo, null);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 56);
    }

    /** "Ultimo download" piccolo sopra, data e ora in grassetto sotto. */
    private static void data(TextView t, String etichetta, String quando) {
        SpannableStringBuilder sb = new SpannableStringBuilder(etichetta + "\n");
        sb.setSpan(new RelativeSizeSpan(0.8f), 0, sb.length(), 0);
        int inizio = sb.length();
        sb.append(quando);
        sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), inizio, sb.length(), 0);
        sb.setSpan(new RelativeSizeSpan(1.25f), inizio, sb.length(), 0);
        t.setText(sb);
    }
}
