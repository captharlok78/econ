package com.ncfsistemi.econ.lista;

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

    /** Colore dell'icona di una colonna a icona (es. lo stato del rapportino), riga per riga. */
    public interface ColoreIcona {
        int colore(ContentValues riga);
    }

    public final String campo;
    public final String label;
    public final int peso;
    public final boolean grassetto;
    public final Formatter formatter;
    /** Riferimento SQL da usare nell'ORDER BY quando "campo" da solo e' ambiguo (es. in un
     * JOIN dove piu' tabelle hanno un campo con lo stesso nome): default = campo stesso. */
    public final String ordinamentoSql;
    /** Glifo FaIcone: se presente la cella mostra l'icona colorata con coloreIcona al posto del testo. */
    public String glifo;
    public ColoreIcona coloreIcona;
    /** Colonna numerica (es. ore): due decimali fissi, cosi' i valori restano allineati; nell'xls e' un numero vero. */
    public boolean numerica;

    public ColonnaLista(String campo, String label, int peso) {
        this(campo, label, peso, false, null);
    }

    public ColonnaLista(String campo, String label, int peso, boolean grassetto) {
        this(campo, label, peso, grassetto, null);
    }

    public ColonnaLista(String campo, String label, int peso, boolean grassetto, Formatter formatter) {
        this(campo, label, peso, grassetto, formatter, campo);
    }

    public ColonnaLista(String campo, String label, int peso, boolean grassetto, Formatter formatter, String ordinamentoSql) {
        this.campo = campo;
        this.label = label;
        this.peso = peso;
        this.grassetto = grassetto;
        this.formatter = formatter;
        this.ordinamentoSql = ordinamentoSql != null ? ordinamentoSql : campo;
    }

    /** Colonna a icona (FaIcone) colorata riga per riga; si ordina per campo/ordinamentoSql come le altre. */
    public static ColonnaLista icona(String campo, String label, int peso, String glifo, ColoreIcona coloreIcona, String ordinamentoSql) {
        ColonnaLista colonna = new ColonnaLista(campo, label, peso, false, null, ordinamentoSql);
        colonna.glifo = glifo;
        colonna.coloreIcona = coloreIcona;
        return colonna;
    }

    /** Colonna di ore: sempre con due decimali (2,00 - 0,50), allineata a sinistra come le altre. */
    public static ColonnaLista ore(String campo, String label, int peso, String ordinamentoSql) {
        ColonnaLista colonna = new ColonnaLista(campo, label, peso, true,
                riga -> formattaNumero(riga.getAsDouble(campo)), ordinamentoSql);
        colonna.numerica = true;
        return colonna;
    }

    public static String formattaNumero(Double valore) {
        return String.format(java.util.Locale.ITALY, "%,.2f", valore != null ? valore : 0d);
    }

    /** Valore numerico della cella (solo colonne numeriche), per l'esportazione in xls. */
    public double getNumero(ContentValues riga) {
        Double valore = riga.getAsDouble(campo);
        return valore != null ? valore : 0d;
    }

    public String getTesto(ContentValues riga) {
        if (formatter != null) {
            return formatter.formatta(riga);
        }
        String valore = riga.getAsString(campo);
        return valore != null ? valore : "";
    }
}
