package pfa.app.econtab.lista;

import android.content.ContentValues;

/**
 * Descrive una colonna della tabella (testata + celle) di un modulo lista standard.
 * Testata e righe vengono costruite a runtime dalla stessa lista di colonne, cosi'
 * non possono disallinearsi come poteva succedere con XML separati per header/riga.
 */
public class ColonnaLista {

    public interface Formatter {
        String formatta(ContentValues riga);
    }

    public final String campo;
    public final String label;
    public final int peso;
    public final boolean grassetto;
    public final Formatter formatter;

    public ColonnaLista(String campo, String label, int peso) {
        this(campo, label, peso, false, null);
    }

    public ColonnaLista(String campo, String label, int peso, boolean grassetto) {
        this(campo, label, peso, grassetto, null);
    }

    public ColonnaLista(String campo, String label, int peso, boolean grassetto, Formatter formatter) {
        this.campo = campo;
        this.label = label;
        this.peso = peso;
        this.grassetto = grassetto;
        this.formatter = formatter;
    }

    public String getTesto(ContentValues riga) {
        if (formatter != null) {
            return formatter.formatta(riga);
        }
        String valore = riga.getAsString(campo);
        return valore != null ? valore : "";
    }
}
