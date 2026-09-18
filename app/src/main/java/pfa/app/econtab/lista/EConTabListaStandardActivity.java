package pfa.app.econtab.lista;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import pfa.app.econtab.EConTabActivity;

/**
 * Activity padre per i moduli "lista standard" (form filtri di default, paginazione,
 * ordinamento per colonna, righe alternate). Ogni modulo estende questa classe e fornisce
 * una EConTabListaStandardDefinition con query/colonne/azione di click riga: una modifica
 * qui si propaga a tutti i moduli senza duplicare codice.
 */
public abstract class EConTabListaStandardActivity extends EConTabActivity
        implements EConTabListaStandardController.Host {

    private EConTabListaStandardController controller;

    protected abstract int getLayoutId();

    protected abstract EConTabListaStandardDefinition creaDefinizione();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        controller = new EConTabListaStandardController(this, creaDefinizione());
        controller.inizializza();
    }

    protected EConTabListaStandardController getController() {
        return controller;
    }

    @Override
    public Context getContext() {
        return this;
    }

    /** Icona filtro nella barra in alto: riapre la form se era stata nascosta dopo una ricerca. */
    public void toggleFiltri(View v) {
        controller.toggleFiltri();
    }

    public void paginaPrecedente(View v) {
        controller.paginaPrecedente();
    }

    public void paginaSuccessiva(View v) {
        controller.paginaSuccessiva();
    }

    @Override
    protected void aggiornaDopoCancellazione() {
        controller.ricaricaSeAttiva();
    }

    @Override
    protected void onResume() {
        super.onResume();
        controller.mostraSoloForm();
    }
}
