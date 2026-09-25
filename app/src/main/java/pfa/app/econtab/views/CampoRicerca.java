package pfa.app.econtab.views;

import android.content.ContentValues;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Campo con ricerca: scrivendo almeno {@link #setMinimoLettere(int) n} lettere (default 3) propone le voci che contengono
 * il testo scritto, in qualsiasi punto e senza badare alle maiuscole. Le voci sono un elenco fisso ({@link #setVoci}) oppure
 * vengono cercate a ogni testo ({@link #setSorgente}, es. una query sul listino). Scegliere una voce la seleziona;
 * modificare il testo dopo la scelta la toglie. {@link Ascoltatore} avvisa di ogni cambio di selezione (null = nessuna).
 */
@SuppressWarnings("AppCompatCustomView") // stile autocomplete del tema dell'app (android:autoCompleteTextViewStyle)
public class CampoRicerca extends AutoCompleteTextView {

    /** Voce proponibile: id, testo mostrato, dati del record. */
    public static class Voce {
        public final int id;
        public final String testo;
        public final ContentValues dati;

        public Voce(int id, String testo, ContentValues dati) {
            this.id = id;
            this.testo = testo;
            this.dati = dati;
        }

        @Override
        public String toString() {
            return testo;
        }
    }

    /** Cerca le voci che corrispondono al testo (chiamata fuori dal thread principale). */
    public interface Sorgente {
        List<Voce> cerca(String testo);
    }

    public interface Ascoltatore {
        void selezioneCambiata(CampoRicerca campo, Voce voce);
    }

    private final List<Voce> voci = new ArrayList<Voce>();
    private Sorgente sorgente = null;
    private Ascoltatore ascoltatore = null;
    private Voce scelta = null;
    private boolean impostazioneInCorso = false;
    private int minimoLettere = 3;
    private Adattatore adattatore;

    public CampoRicerca(Context context) {
        super(context);
        inizializza();
    }

    public CampoRicerca(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.autoCompleteTextViewStyle);
        inizializza();
    }

    private void inizializza() {
        setSingleLine(true);
        setThreshold(minimoLettere);
        adattatore = new Adattatore(getContext());
        setAdapter(adattatore);
        setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Voce v = adattatore.getItem(position);
                scelta = v;
                setSelection(0); // si vede l'inizio (codice) e non la fine del testo
                if (ascoltatore != null) ascoltatore.selezioneCambiata(CampoRicerca.this, v);
            }
        });
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (impostazioneInCorso || scelta == null || s.toString().equals(scelta.testo)) return;
                scelta = null;
                if (ascoltatore != null) ascoltatore.selezioneCambiata(CampoRicerca.this, null);
            }
        });
        // con minimo 0 (elenchi corti, es. gli ordini del cantiere) si apre l'elenco toccando il campo
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (minimoLettere == 0 && isEnabled()) showDropDown();
            }
        });
    }

    public void setMinimoLettere(int minimo) {
        minimoLettere = minimo;
        setThreshold(Math.max(1, minimo));
    }

    @Override
    public boolean enoughToFilter() {
        return minimoLettere == 0 || super.enoughToFilter();
    }

    public void setVoci(List<Voce> nuove) {
        voci.clear();
        voci.addAll(nuove);
        sorgente = null;
    }

    public void setSorgente(Sorgente s) {
        sorgente = s;
    }

    public void setAscoltatore(Ascoltatore a) {
        ascoltatore = a;
    }

    public Voce getScelta() {
        return scelta;
    }

    /** Id della voce scelta, 0 se nessuna. */
    public int getIdScelto() {
        return scelta != null ? scelta.id : 0;
    }

    /** Imposta la selezione da codice (senza avvisare l'ascoltatore); null svuota il campo. */
    public void seleziona(Voce v) {
        impostazioneInCorso = true;
        scelta = v;
        setText(v != null ? v.testo : "", false);
        setSelection(0);
        impostazioneInCorso = false;
        dismissDropDown();
    }

    /** Seleziona la voce dell'elenco con quell'id; se non c'e' svuota il campo. */
    public void selezionaId(int id) {
        seleziona(id != 0 ? cercaId(id) : null);
    }

    public Voce cercaId(int id) {
        for (Voce v : voci) {
            if (v.id == id) return v;
        }
        return null;
    }

    public List<Voce> getVoci() {
        return voci;
    }

    private class Adattatore extends ArrayAdapter<Voce> {
        private final Filter filtro = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence testo) {
                String t = testo != null ? testo.toString().trim().toLowerCase(Locale.getDefault()) : "";
                List<Voce> trovate = new ArrayList<Voce>();
                if (sorgente != null) {
                    if (t.length() >= Math.max(1, minimoLettere)) trovate.addAll(sorgente.cerca(t));
                } else {
                    for (Voce v : new ArrayList<Voce>(voci)) {
                        if (t.isEmpty() || v.testo.toLowerCase(Locale.getDefault()).contains(t)) trovate.add(v);
                    }
                }
                FilterResults r = new FilterResults();
                r.values = trovate;
                r.count = trovate.size();
                return r;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence testo, FilterResults risultati) {
                clear();
                if (risultati.values != null) addAll((List<Voce>) risultati.values);
                notifyDataSetChanged();
            }

            @Override
            public CharSequence convertResultToString(Object risultato) {
                return ((Voce) risultato).testo;
            }
        };

        Adattatore(Context context) {
            super(context, android.R.layout.simple_dropdown_item_1line, new ArrayList<Voce>());
        }

        @Override
        public Filter getFilter() {
            return filtro;
        }
    }
}
