package com.ncfsistemi.econ.lista;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ncfsistemi.econ.fragments.EconFragment;

/**
 * Fragment padre per i moduli "lista standard" incorporabili altrove (es. Cantieri dentro
 * ClientiDettaglioActivity), non solo a schermo intero. Stesso comportamento comune di
 * EconListaStandardActivity (form filtri di default, paginazione, ordinamento per
 * colonna, righe alternate), condiviso tramite lo stesso EconListaStandardController:
 * l'unica differenza e' che qui l'host e' un Fragment, non una Activity.
 */
public abstract class EconListaStandardFragment extends EconFragment
        implements EconListaStandardController.Host {

    private EconListaStandardController controller;
    private View radice;

    protected abstract int getLayoutId();

    protected abstract EconListaStandardDefinition creaDefinizione();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        radice = inflater.inflate(getLayoutId(), container, false);
        controller = new EconListaStandardController(this, creaDefinizione());
        controller.inizializza();
        return radice;
    }

    protected EconListaStandardController getController() {
        return controller;
    }

    @Override
    public <T extends View> T findViewById(int id) {
        return radice.findViewById(id);
    }

    @Override
    public void onResume() {
        super.onResume();
        controller.apertura();
    }

    /** Da richiamare da aggiornaDopoCancellazione() dell'Activity che ospita il fragment. */
    public void ricaricaSeAttiva() {
        controller.ricaricaSeAttiva();
    }
}
