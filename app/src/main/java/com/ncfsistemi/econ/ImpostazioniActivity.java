package com.ncfsistemi.econ;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.PreferenzeDispositivo;
import com.ncfsistemi.econ.utils.SezioneStandard;

/**
 * Impostazioni (STATO_SISTEMA_APP.md §5): "Preferenze del dispositivo" (salvate solo sul dispositivo, utils.PreferenzeDispositivo)
 * e "Dati della ditta", che per ora apre le voci di prima (Aziende, Codici IVA, Unita' di misura, Manodopera).
 * Ogni preferenza e' una riga: titolo, descrizione e valore attuale; il tocco apre la scelta.
 */
public class ImpostazioniActivity extends AppCompatActivity {

    private LinearLayout righePreferenze;
    private float dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_impostazioni);
        dp = getResources().getDisplayMetrics().density;
        SezioneStandard.imposta(findViewById(R.id.sezione_preferenze), FaIcone.IMPOSTAZIONI, "Preferenze del dispositivo");
        SezioneStandard.imposta(findViewById(R.id.sezione_ditta), FaIcone.DITTA, "Dati della ditta");
        righePreferenze = findViewById(R.id.righePreferenze);
        LinearLayout righeDitta = findViewById(R.id.righeDitta);
        righeDitta.addView(riga("Aziende, codici IVA, unità di misura, manodopera",
                "Arrivano dal server con la sincronizzazione: si cambiano dal portale",
                "Apri", x -> startActivity(new Intent(this, ConfigurazioneActivity.class))));
        mostraPreferenze();
    }

    public void chiudi(View v) {
        finish();
    }

    private void mostraPreferenze() {
        righePreferenze.removeAllViews();
        preferenza("Sincronizzazione automatica",
                "Invia e scarica i dati in background quando c'è rete; tiene anche viva la sessione",
                PreferenzeDispositivo.SYNC_AUTOMATICA, "0", PreferenzeDispositivo.VALORI_SYNC, PreferenzeDispositivo.ETICHETTE_SYNC);
        preferenza("Apertura delle liste",
                "Clienti, cantieri, rapportini…: mostrare solo i filtri o subito i risultati",
                PreferenzeDispositivo.LISTE_SUBITO, "0", PreferenzeDispositivo.VALORI_LISTE, PreferenzeDispositivo.ETICHETTE_LISTE);
        preferenza("Righe per pagina",
                "Quante righe mostrano le liste per ogni pagina",
                PreferenzeDispositivo.RIGHE_PAGINA, "20", PreferenzeDispositivo.VALORI_RIGHE, PreferenzeDispositivo.ETICHETTE_RIGHE);
        preferenza("Periodo dei rapportini",
                "Periodo proposto all'apertura dell'elenco dei rapportini",
                PreferenzeDispositivo.PERIODO_RAPPORTINI, "S", PreferenzeDispositivo.VALORI_PERIODO, PreferenzeDispositivo.ETICHETTE_PERIODO);
    }

    private void preferenza(String titolo, String descrizione, String chiave, String predefinito, String[] valori, String[] etichette) {
        String attuale = PreferenzeDispositivo.etichetta(this, chiave, predefinito, valori, etichette);
        righePreferenze.addView(riga(titolo, descrizione, attuale, x -> {
            String v = PreferenzeDispositivo.leggi(this, chiave, predefinito);
            int scelto = 0;
            for (int i = 0; i < valori.length; i++) if (valori[i].equals(v)) scelto = i;
            new AlertDialog.Builder(this)
                    .setTitle(titolo)
                    .setSingleChoiceItems(etichette, scelto, (d, i) -> {
                        PreferenzeDispositivo.salva(this, chiave, valori[i]);
                        d.dismiss();
                        mostraPreferenze();
                    })
                    .setNegativeButton(R.string.annulla, null)
                    .show();
        }));
    }

    /** Riga: titolo in grassetto e descrizione a sinistra, valore (blu) a destra; tutta toccabile. */
    private View riga(String titolo, String descrizione, String valore, View.OnClickListener azione) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(Math.round(64 * dp));
        r.setPadding(0, Math.round(8 * dp), 0, Math.round(8 * dp));
        TypedValue sfondo = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, sfondo, true);
        r.setBackgroundResource(sfondo.resourceId);
        r.setOnClickListener(azione);

        LinearLayout testi = new LinearLayout(this);
        testi.setOrientation(LinearLayout.VERTICAL);
        testi.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView t = new TextView(this);
        t.setText(titolo);
        t.setTextSize(17);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(Color.parseColor("#212121"));
        TextView d = new TextView(this);
        d.setText(descrizione);
        d.setTextSize(13);
        d.setTextColor(Color.parseColor("#757575"));
        testi.addView(t);
        testi.addView(d);

        TextView v = new TextView(this);
        v.setText(valore);
        v.setTextSize(16);
        v.setTextColor(Color.parseColor("#1565C0"));
        v.setGravity(Gravity.END);
        v.setPadding(Math.round(12 * dp), 0, 0, 0);

        r.addView(testi);
        r.addView(v);

        LinearLayout contenitore = new LinearLayout(this);
        contenitore.setOrientation(LinearLayout.VERTICAL);
        contenitore.addView(r);
        View linea = new View(this);
        linea.setBackgroundColor(Color.parseColor("#E0E0E0"));
        contenitore.addView(linea, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Math.round(dp))));
        return contenitore;
    }
}
