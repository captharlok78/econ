package pfa.app.econtab.lista;

import android.content.ContentValues;
import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pfa.app.econtab.adapters.EConTabListViewAdapter;

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

    public abstract void onRigaClick(EConTabListaStandardController.Host host, ContentValues riga);

    /** Titolo mostrato nella testata standard (uguale per struttura/stile in tutti i moduli). */
    public abstract String getTitolo();

    /** Pulsante "+" nella testata standard: apre la finestra di inserimento del modulo. */
    public abstract void onNuovoClick(EConTabListaStandardController.Host host);

    /**
     * false nasconde indietro/titolo nella testata standard, mostrando solo filtro/nuovo:
     * usato dai moduli incorporati altrove (es. Cantieri come tab di un cliente), dove
     * indietro/titolo sarebbero ridondanti/sbagliati rispetto al contenitore.
     */
    public boolean mostraBarraTitolo() {
        return true;
    }

    /**
     * Colonne della testata/righe generiche (EConTabListaStandardAdapter). Non serve
     * sovrascriverlo per un modulo che fornisce un adapter personalizzato tramite
     * creaAdapterPersonalizzato() e ha isTabellare()=false (righe non semplici colonne
     * di testo, es. Preventivi/Ordini/Rapportini con azioni inline per riga).
     */
    public List<ColonnaLista> getColonne() {
        return Collections.emptyList();
    }

    /**
     * false per i moduli le cui righe non sono semplici colonne di testo (contenuti multi
     * riga, pulsanti di azione inline): la testata ordinabile non viene costruita/mostrata,
     * ma paginazione/form filtri/righe alternate restano comunque gestite dal controller.
     */
    public boolean isTabellare() {
        return true;
    }

    /**
     * Adapter da usare al posto di quello generico a colonne (EConTabListaStandardAdapter),
     * per i moduli con righe troppo custom per il modello dichiarativo. Default null = usa
     * quello generico.
     */
    public EConTabListViewAdapter creaAdapterPersonalizzato(Context context, ArrayList<Object> dati) {
        return null;
    }

    /** Order by SQL usato quando e' attivo "ultimi N". Null = disabilita quel pulsante. */
    public String getOrdineRecenti() {
        return null;
    }

    public String getColonnaOrdinamentoDefault() {
        List<ColonnaLista> colonne = getColonne();
        return colonne.isEmpty() ? null : colonne.get(0).campo;
    }

    public boolean isOrdinamentoDefaultDiscendente() {
        return false;
    }

    public int getRisultatiPerPaginaDefault() {
        return 20;
    }
}
