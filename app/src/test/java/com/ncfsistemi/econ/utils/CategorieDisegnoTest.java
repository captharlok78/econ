package com.ncfsistemi.econ.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Stessi nomi di tests/Design/CatalogoDisegnoTest.php del server (categorie di P.F.A. importate da appel). */
public class CategorieDisegnoTest {

    @Test
    public void elementiDiAppelDalNome() {
        String[] nomi = {"Sc. prese", "Sc. interruttori e altro", "Utilizzatori", "Quadri", "Componibili", "Allarme", "Cavi",
                "Tubi", "Autamazione cancelli", "Citofonia TVCC", "Telefonia e reti", "Materiale vario"};
        for (int i = 0; i < nomi.length; i++) {
            assertEquals(nomi[i], i + 1, CategorieDisegno.riconosciElemento(null, nomi[i], null));
        }
        assertEquals(CategorieDisegno.COMPONIBILI, CategorieDisegno.riconosciElemento(null, "Altro", "scatole.png"));
        assertEquals(0, CategorieDisegno.riconosciElemento(null, "Murature", null));
    }

    @Test
    public void componentiDiAppelDalNome() {
        String[] nomi = {"Scatole da incasso", "Portafrutti", "Prese", "Interruttori", "Copriscatola", "Utilizzatori",
                "Centralini", "Componenti quadri", "Comp. interruttori", "Allarmi", "Automazione cancelli", "Citofonia TVCC",
                "Telefonia e reti", "Materiale vario"};
        for (int i = 0; i < nomi.length; i++) {
            assertEquals(nomi[i], i + 1, CategorieDisegno.riconosciComponente(null, nomi[i], null));
        }
    }

    @Test
    public void codiceDelServerVinceSulNome() {
        assertEquals(CategorieDisegno.QUADRI, CategorieDisegno.riconosciElemento("QUADRI", "Centralini e armadi", null));
        assertEquals(CategorieDisegno.TELEFONIA, CategorieDisegno.riconosciElemento("RETI", null, null));
        assertEquals(7, CategorieDisegno.riconosciComponente("CENTRALINI", "Quadretti", null));
        // codice che il disegno classico non conosce (categoria di un altro mestiere): nessun comportamento speciale
        assertEquals(0, CategorieDisegno.riconosciElemento("MURATURE", "Prese finte", null));
    }
}
