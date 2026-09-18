package pfa.app.econtab.lista;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import pfa.app.econtab.fragments.EConTabFragment;

/**
 * Fragment padre per i moduli "lista standard" incorporabili altrove (es. Cantieri dentro
 * ClientiDettaglioActivity), non solo a schermo intero. Stesso comportamento comune di
 * EConTabListaStandardActivity (form filtri di default, paginazione, ordinamento per
 * colonna, righe alternate), condiviso tramite lo stesso EConTabListaStandardController:
 * l'unica differenza e' che qui l'host e' un Fragment, non una Activity.
 */
public abstract class EConTabListaStandardFragment extends EConTabFragment
        implements EConTabListaStandardController.Host {

    private EConTabListaStandardController controller;
    private View radice;

    protected abstract int getLayoutId();

    protected abstract EConTabListaStandardDefinition creaDefinizione();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        radice = inflater.inflate(getLayoutId(), container, false);
        controller = new EConTabListaStandardController(this, creaDefinizione());
        controller.inizializza();
        return radice;
    }

    protected EConTabListaStandardController getController() {
        return controller;
    }

    @Override
    public <T extends View> T findViewById(int id) {
        return radice.findViewById(id);
    }

    @Override
    public void onResume() {
        super.onResume();
        controller.mostraSoloForm();
    }

    /** Da richiamare da aggiornaDopoCancellazione() dell'Activity che ospita il fragment. */
    public void ricaricaSeAttiva() {
        controller.ricaricaSeAttiva();
    }
}
