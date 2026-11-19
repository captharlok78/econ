package com.ncfsistemi.econ;

/**
 * Conferma del rapportino (NUMERAZIONE_DOCUMENTI.md §6): la sincronizzazione in modalita' conferma in una finestra sopra il
 * rapportino (tema EconFinestra nel manifest). Tutta la logica e' in SincronizzazioneActivity (MODE_CONFERMA).
 * Angoli arrotondati come i modali (STUDIO_MODALI_APP.md).
 */
public class SincronizzazioneConfermaActivity extends SincronizzazioneActivity {

    @Override
    protected void onPostCreate(android.os.Bundle stato) {
        super.onPostCreate(stato);
        com.ncfsistemi.econ.dialogo.EconDialogo.arrotonda(this);
    }
}
