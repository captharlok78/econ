package com.ncfsistemi.econ.disegno;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Componenti;
import com.ncfsistemi.econ.db.table.ComponentiCantComposti;
import com.ncfsistemi.econ.db.table.ComponentiCantiere;
import com.ncfsistemi.econ.db.table.Composizioni;
import com.ncfsistemi.econ.db.table.ComposizioniCantiere;
import com.ncfsistemi.econ.utils.Utility;

/**
 * Componi: i componenti liberi uguali (morsetti, cavo, ...) si vedono in una riga sola con la quantità.
 * <p>
 * Solo a video: nel DB restano N righe (una per pezzo), così sync, preventivo/ordine, Excel e PDF di conformità non
 * cambiano. La riga del gruppo è la copia della prima con {@link #QTA} (pezzi) e {@link #IDS} (id delle righe, in ordine
 * di posizione). Si raggruppano le righe con lo stesso componente, nome, nota, unità di misura, metri, contabilizzazione,
 * documento e sotto-componenti; quelle con collegamenti o relazioni restano da sole (sono pezzi "veri" del disegno).
 * Doc: STUDIO_DESIGN_GRAFICA.md §14.
 */
public final class QuantitaComponenti {

    /** Pezzi nella riga (presente solo sulle righe di componenti liberi, anche con 1). */
    public static final String QTA = "qta_gruppo";
    /** Id delle righe del gruppo separati da virgola: id_componente_cant (cantiere) o id_composizione (catalogo). */
    public static final String IDS = "ids_gruppo";

    /** L'activity di Componi che ospita la lista. */
    public interface Ospite {
        /** Tocco sulla pillola della quantità. */
        void chiediQuantita(ContentValues riga);
    }

    /** Quali righe sono componenti liberi (dipende dalla maschera: libera, scatola, quadro). */
    public interface Filtro {
        boolean libero(ContentValues riga);
    }

    private QuantitaComponenti() {
    }

    public static int quantita(ContentValues riga) {
        if (riga == null || riga.get(QTA) == null) return 0;
        return riga.getAsInteger(QTA);
    }

    public static boolean raggruppabile(ContentValues riga) {
        return riga != null && riga.get(QTA) != null;
    }

    public static List<Integer> ids(ContentValues riga, boolean cantiere) {
        ArrayList<Integer> out = new ArrayList<Integer>();
        if (riga.get(IDS) != null) {
            for (String s : riga.getAsString(IDS).split(",")) {
                if (s.length() > 0) out.add(Integer.parseInt(s));
            }
        } else {
            Integer id = cantiere ? riga.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT) : riga.getAsInteger(Composizioni.ID_COMPOSIZIONE);
            if (id != null) out.add(id);
        }
        return out;
    }

    public static int[] idsArray(ContentValues riga, boolean cantiere) {
        List<Integer> l = ids(riga, cantiere);
        int[] a = new int[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        return a;
    }

    /** Come {@link #raggruppa(DbInterno, ArrayList, boolean, Filtro, HashMap, HashMap)} aprendo il DB. */
    public static ArrayList<Object> raggruppa(android.content.Context ctx, ArrayList<Object> righe, boolean cantiere, Filtro filtro,
                                              HashMap<Integer, ArrayList<Object>> collegamenti,
                                              HashMap<Integer, ArrayList<Object>> relazioni) {
        DbInterno db = new DbInterno(ctx);
        try {
            return raggruppa(db, righe, cantiere, filtro, collegamenti, relazioni);
        } finally {
            db.close();
        }
    }

    /**
     * Raggruppa le righe libere uguali. Le altre restano dove sono; il gruppo prende il posto della sua prima riga.
     */
    public static ArrayList<Object> raggruppa(DbInterno db, ArrayList<Object> righe, boolean cantiere, Filtro filtro,
                                              HashMap<Integer, ArrayList<Object>> collegamenti,
                                              HashMap<Integer, ArrayList<Object>> relazioni) {
        HashMap<Integer, String> figli = cantiere ? firmeFigli(db, righe) : new HashMap<Integer, String>();
        LinkedHashMap<String, ContentValues> gruppi = new LinkedHashMap<String, ContentValues>();
        ArrayList<Object> out = new ArrayList<Object>();
        for (Object o : righe) {
            ContentValues r = (ContentValues) o;
            Integer idCat = r.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE);
            if (idCat == null || idCat == 0 || !filtro.libero(r)) {
                out.add(r);
                continue;
            }
            Integer id = cantiere ? r.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT) : r.getAsInteger(Composizioni.ID_COMPOSIZIONE);
            if (id == null) {
                out.add(r);
                continue;
            }
            boolean legato = cantiere && ((collegamenti != null && collegamenti.containsKey(id)) || (relazioni != null && relazioni.containsKey(id)));
            if (legato) {
                // pezzo con collegamenti o relazioni: da solo, senza pillola della quantità
                out.add(r);
                continue;
            }
            String chiave = chiave(r, cantiere, figli.get(id));
            ContentValues g = gruppi.get(chiave);
            if (g == null) {
                g = new ContentValues(r);
                g.put(QTA, 1);
                g.put(IDS, String.valueOf(id));
                gruppi.put(chiave, g);
                out.add(g);
            } else {
                g.put(QTA, g.getAsInteger(QTA) + 1);
                g.put(IDS, g.getAsString(IDS) + "," + id);
            }
        }
        return out;
    }

    private static String chiave(ContentValues r, boolean cantiere, String figli) {
        StringBuilder k = new StringBuilder();
        k.append(r.getAsString(Componenti.ID_COMPONENTE));
        if (cantiere) {
            k.append('|').append(r.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
            k.append('|').append(r.getAsString(ComponentiCantiere.NOTA));
            k.append('|').append(r.getAsString(ComponentiCantiere.UNITA_MISURA));
            k.append('|').append(r.getAsString(ComponentiCantiere.METRI_CAVO_CANT));
            k.append('|').append(r.getAsString(ComponentiCantiere.ID_ELEMENTO_CAVO));
            k.append('|').append(r.getAsString(ComponentiCantiere.METRI_TUBO_CANT));
            k.append('|').append(r.getAsString(ComponentiCantiere.ID_ELEMENTO_TUBO));
            k.append('|').append(r.getAsString(ComponentiCantiere.SPAZI_OCCUPATI_CANT));
            Integer nc = r.getAsInteger(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI);
            k.append('|').append(nc == null ? 0 : nc);
            Integer prev = r.getAsInteger(ComponentiCantiere.ID_PREVENTIVO);
            k.append('|').append(prev == null ? 0 : prev);
            Integer pos = r.getAsInteger(ComposizioniCantiere.POSIZIONE_INIZIALE);
            k.append('|').append(pos != null && pos == 100 ? "S" : "-");
            k.append('|').append(figli == null ? "" : figli);
        }
        return k.toString();
    }

    /** Per ogni componente del cantiere la lista ordinata dei componenti di catalogo dei suoi figli (composti). */
    private static HashMap<Integer, String> firmeFigli(DbInterno db, ArrayList<Object> righe) {
        HashMap<Integer, String> out = new HashMap<Integer, String>();
        StringBuilder in = new StringBuilder();
        for (Object o : righe) {
            Integer id = ((ContentValues) o).getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT);
            if (id == null) continue;
            if (in.length() > 0) in.append(',');
            in.append(id);
        }
        if (in.length() == 0) return out;
        String sql = "select cc." + ComponentiCantComposti.ID_COMPONENTE_CANT_PADRE + " as padre, f." + ComponentiCantiere.ID_COMPONENTE
                + " as comp, f." + ComponentiCantiere.NOME_COMPONENTE_CANT + " as nome from " + ComponentiCantComposti.NOME_TABELLA
                + " cc inner join " + ComponentiCantiere.NOME_TABELLA + " f on f." + ComponentiCantiere.ID_COMPONENTE_CANT + "=cc."
                + ComponentiCantComposti.ID_COMPONENTE_CANT_FIGLIO + " where cc." + ComponentiCantComposti.ID_COMPONENTE_CANT_PADRE
                + " in (" + in + ") order by cc." + ComponentiCantComposti.ID_COMPONENTE_CANT_PADRE + ", f." + ComponentiCantiere.ID_COMPONENTE
                + ", f." + ComponentiCantiere.NOME_COMPONENTE_CANT;
        ArrayList<Object> recs = db.eseguiSelect(sql, null);
        if (recs == null) return out;
        for (Object o : recs) {
            ContentValues r = (ContentValues) o;
            int padre = r.getAsInteger("padre");
            String prima = out.get(padre);
            String pezzo = r.getAsString("comp") + ":" + r.getAsString("nome");
            out.put(padre, prima == null ? pezzo : prima + ";" + pezzo);
        }
        return out;
    }

    /** Testo della pillola: "× 10" e l'unità di misura se non è il pezzo. */
    public static String etichetta(ContentValues riga) {
        String um = riga.getAsString(ComponentiCantiere.UNITA_MISURA);
        um = um == null ? "" : um.trim();
        String s = "× " + quantita(riga);
        if (um.length() > 0 && !um.equalsIgnoreCase("pz") && !um.equalsIgnoreCase("n")) s += " " + um.toLowerCase(Locale.getDefault());
        return s;
    }

    /**
     * Finestra della quantità: Conferma porta il gruppo al numero scritto (aggiunge in fondo o toglie le ultime righe).
     * Con minimo 0 si può svuotare il gruppo; dopo il cambio chiama {@code dopo}.
     */
    public static void chiediQuantita(final Activity act, final ContentValues riga, final int idElemento, final int idElementoCant,
                                      final int minimo, final Runnable dopo) {
        final boolean cantiere = idElementoCant != 0;
        View campi = View.inflate(act, R.layout.dialog_qtacomponente_layout, null);
        com.google.android.material.textfield.TextInputLayout etichetta = campi.findViewById(R.id.campo_qta);
        final EditText qta = (EditText) campi.findViewById(R.id.editText_qta);
        TextView info = (TextView) campi.findViewById(R.id.textView_componenti);
        String um = riga.getAsString(ComponentiCantiere.UNITA_MISURA);
        etichetta.setHint(act.getString(R.string.quantita) + " " + (um == null ? "" : um.trim()));
        qta.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        final int attuale = quantita(riga);
        qta.setText(String.valueOf(attuale));
        info.setText("Ogni pezzo resta una riga nel documento: qui li vedi insieme. Pezzi adesso: " + attuale);
        String nome = cantiere ? riga.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT) : riga.getAsString(Componenti.NOME_COMPONENTE);
        Utility.mostraDialogPersonalizzato(nome == null ? "" : nome, act, campi,
                act.getString(R.string.conferma), act.getString(R.string.annulla), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which != DialogInterface.BUTTON_POSITIVE) return;
                        int nuova;
                        try {
                            nuova = Integer.parseInt(qta.getText().toString().trim());
                        } catch (Exception e) {
                            return;
                        }
                        if (nuova < minimo) {
                            Toast.makeText(act, "La quantità minima è " + minimo, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (nuova > 999) {
                            Toast.makeText(act, "La quantità massima è 999", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (nuova == attuale) return;
                        DbInterno db = new DbInterno(act);
                        try {
                            portaA(db, riga, idElemento, idElementoCant, nuova);
                        } finally {
                            db.close();
                        }
                        dopo.run();
                    }
                });
    }

    /** Elimina tutti i pezzi del gruppo dopo la conferma. */
    public static void eliminaGruppo(final Activity act, final ContentValues riga, final int idElemento, final int idElementoCant,
                                     final Runnable dopo) {
        Utility.mostraConfermaDialog(act.getString(R.string.attenzione), "Eliminare tutti i " + quantita(riga) + " pezzi di questa riga?",
                act, act.getString(R.string.si), act.getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which != DialogInterface.BUTTON_POSITIVE) return;
                        DbInterno db = new DbInterno(act);
                        try {
                            portaA(db, riga, idElemento, idElementoCant, 0);
                        } finally {
                            db.close();
                        }
                        dopo.run();
                    }
                });
    }

    /** Porta il gruppo a {@code nuova} pezzi. */
    public static void portaA(DbInterno db, ContentValues riga, int idElemento, int idElementoCant, int nuova) {
        boolean cantiere = idElementoCant != 0;
        List<Integer> ids = ids(riga, cantiere);
        int diff = nuova - ids.size();
        if (diff > 0) {
            for (int n = 0; n < diff; n++) aggiungiUno(db, riga, idElemento, idElementoCant);
        } else if (diff < 0) {
            // tolgo dal fondo: le ultime aggiunte
            for (int n = 0; n < -diff; n++) togliUno(db, ids.get(ids.size() - 1 - n), idElemento, idElementoCant);
        }
    }

    private static void aggiungiUno(DbInterno db, ContentValues riga, int idElemento, int idElementoCant) {
        if (idElementoCant != 0) {
            ContentValues where = new ContentValues();
            where.put(Componenti.ID_COMPONENTE, riga.getAsInteger(ComponentiCantiere.ID_COMPONENTE));
            ContentValues comp = db.getRecord(new Componenti(), where);
            if (comp == null) return;
            Integer pos = riga.getAsInteger(ComposizioniCantiere.POSIZIONE_INIZIALE);
            ContentValues nuovo = new ComposizioniCantiere().aggiungiComponenteLibero(db, comp, idElementoCant, pos != null && pos == 100);
            // il pezzo nuovo è uguale agli altri del gruppo (nome, nota, metri, contabilizzazione, documento)
            ComponentiCantiere tab = new ComponentiCantiere();
            ContentValues upd = tab.getValoriLogModifica(db);
            String[] campi = {ComponentiCantiere.NOME_COMPONENTE_CANT, ComponentiCantiere.NOTA, ComponentiCantiere.UNITA_MISURA,
                    ComponentiCantiere.METRI_CAVO_CANT, ComponentiCantiere.ID_ELEMENTO_CAVO, ComponentiCantiere.METRI_TUBO_CANT,
                    ComponentiCantiere.ID_ELEMENTO_TUBO, ComponentiCantiere.SPAZI_OCCUPATI_CANT,
                    ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI, ComponentiCantiere.ID_PREVENTIVO};
            for (String c : campi) {
                if (riga.containsKey(c)) upd.put(c, riga.getAsString(c));
            }
            ContentValues whereUpd = new ContentValues();
            whereUpd.put(ComponentiCantiere.ID_COMPONENTE_CANT, nuovo.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
            tab.aggiornaRecord(db, upd, whereUpd);
        } else if (idElemento != 0) {
            new Composizioni().aggiungiComponenteLibero(db, riga, idElemento);
        }
    }

    /**
     * Cancella una riga del gruppo. Le posizioni degli altri non si ricompattano: per i componenti liberi contano solo
     * come ordine (spostaComponente cerca il vicino con >= / <=) e l'inserimento usa la massima + 1.
     */
    private static void togliUno(DbInterno db, int id, int idElemento, int idElementoCant) {
        if (idElementoCant != 0) {
            ContentValues where = new ContentValues();
            where.put(ComposizioniCantiere.ID_ELEMENTO_CANT, idElementoCant);
            where.put(ComposizioniCantiere.ID_COMPONENTE_CANT, id);
            // cancella anche il componente del cantiere e lo toglie dal documento (eliminaCorrelati)
            new ComposizioniCantiere().cancellaRecord(db, where);
        } else if (idElemento != 0) {
            ContentValues where = new ContentValues();
            where.put(Composizioni.ID_COMPOSIZIONE, id);
            new Composizioni().cancellaRecord(db, where);
        }
    }

    /** Applica gli stessi valori a tutte le righe del gruppo (nota, contabilizzazione). Solo cantiere. */
    public static void aggiornaTutti(DbInterno db, ContentValues riga, ContentValues valori) {
        ComponentiCantiere tab = new ComponentiCantiere();
        for (int id : ids(riga, true)) {
            ContentValues where = new ContentValues();
            where.put(ComponentiCantiere.ID_COMPONENTE_CANT, id);
            tab.aggiornaRecord(db, new ContentValues(valori), where);
        }
    }
}
