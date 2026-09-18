package pfa.app.econtab.lista;

import android.content.ContentValues;

import java.util.List;

/**
 * Personalizzazione di un modulo lista standard: query (filtri/join), colonne visualizzate
 * e azione di click riga. E' l'unica cosa che un modulo deve scrivere: tutto il resto (form
 * filtri visibile di default, paginazione, ordinamento per colonna, righe alternate) e'
 * gestito da EConTabListaStandardController, cosi' una modifica al comportamento comune si
 * propaga a tutti i moduli senza duplicare codice.
 */
public abstract class EConTabListaStandardDefinition {

    /**
     * Costruisce la query per la ricerca corrente (testoFiltro e' il testo libero della
     * form filtri standard; i moduli con filtri aggiuntivi li leggono da riferimenti propri
     * tenuti nella sottoclasse, impostati dall'Activity/Fragment che la crea).
     */
    public abstract QueryPagina costruisciQuery(String testoFiltro, boolean ordinaPerRecenti);

    public abstract List<ColonnaLista> getColonne();

    public abstract void onRigaClick(EConTabListaStandardController.Host host, ContentValues riga);

    /** Order by SQL usato quando e' attivo "ultimi N". Null = disabilita quel pulsante. */
    public String getOrdineRecenti() {
        return null;
    }

    public String getColonnaOrdinamentoDefault() {
        List<ColonnaLista> colonne = getColonne();
        return colonne.isEmpty() ? null : colonne.get(0).campo;
    }

    public int getRisultatiPerPaginaDefault() {
        return 20;
    }
}
