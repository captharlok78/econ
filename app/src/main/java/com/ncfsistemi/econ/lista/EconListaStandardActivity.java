package com.ncfsistemi.econ.lista;

import android.content.Context;
import android.os.Bundle;

import com.ncfsistemi.econ.EconActivity;

/**
 * Activity padre per i moduli "lista standard" (form filtri di default, paginazione,
 * ordinamento per colonna, righe alternate). Ogni modulo estende questa classe e fornisce
 * una EconListaStandardDefinition con query/colonne/azione di click riga: una modifica
 * qui si propaga a tutti i moduli senza duplicare codice.
 */
public abstract class EconListaStandardActivity extends EconActivity
        implements EconListaStandardController.Host {

    private EconListaStandardController controller;

    protected abstract int getLayoutId();

    protected abstract EconListaStandardDefinition creaDefinizione();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        controller = new EconListaStandardController(this, creaDefinizione());
        controller.inizializza();
    }

    protected EconListaStandardController getController() {
        return controller;
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    protected void aggiornaDopoCancellazione() {
        controller.ricaricaSeAttiva();
    }

    @Override
    protected void onResume() {
        super.onResume();
        controller.apertura();
    }
}
