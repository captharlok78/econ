package pfa.app.econtab.stampa;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motore dei modelli di stampa (GESTIONE_RAPPORTINI.md §15), identico al server (App\Stampa\MiniMustache):
 * {{nome}} / {{a.b}} protetto, {{{nome}}} cosi' com'e', {{#x}}…{{/x}} elenco/oggetto/vero, {{^x}}…{{/x}} se vuoto,
 * {{! commento}}, {{.}} elemento corrente. I nomi si cercano dal contesto piu' interno verso l'esterno; un segmento
 * numerico indica un elemento di un elenco ({{#righe.0}}: c'e' almeno una riga). Contesti: Map, List, String, Number, Boolean.
 */
public final class MiniMustache {

    private static final Pattern TAG = Pattern.compile("\\{\\{(\\{)?\\s*([#^/!]?)\\s*([^}]*?)\\s*\\}?\\}\\}", Pattern.DOTALL);

    private MiniMustache() {
    }

    public static String render(String modello, Map<String, Object> contesto) {
        int[] pos = { 0 };
        List<Object> albero = analizza(modello, pos, null);
        List<Object> pila = new ArrayList<>();
        pila.add(contesto);
        StringBuilder out = new StringBuilder();
        scrivi(albero, pila, out);
        return out.toString();
    }

    /** Nodo: String = testo; Valore; Sezione. */
    private static final class Valore {
        final String nome;
        final boolean raw;
        Valore(String nome, boolean raw) { this.nome = nome; this.raw = raw; }
    }

    private static final class Sezione {
        final String nome;
        final boolean inverso;
        final List<Object> figli;
        Sezione(String nome, boolean inverso, List<Object> figli) { this.nome = nome; this.inverso = inverso; this.figli = figli; }
    }

    private static List<Object> analizza(String t, int[] pos, String chiusura) {
        List<Object> nodi = new ArrayList<>();
        Matcher m = TAG.matcher(t);
        while (m.find(pos[0])) {
            if (m.start() > pos[0]) nodi.add(t.substring(pos[0], m.start()));
            pos[0] = m.end();
            boolean raw = m.group(1) != null;
            String tipo = m.group(2);
            String nome = m.group(3).trim();
            if ("!".equals(tipo)) continue;
            if ("/".equals(tipo)) {
                if (!nome.equals(chiusura)) throw new IllegalArgumentException("Modello di stampa: {{/" + nome + "}} senza {{#" + nome + "}}");
                return nodi;
            }
            if ("#".equals(tipo) || "^".equals(tipo)) {
                nodi.add(new Sezione(nome, "^".equals(tipo), analizza(t, pos, nome)));
                continue;
            }
            nodi.add(new Valore(nome, raw));
        }
        if (chiusura != null) throw new IllegalArgumentException("Modello di stampa: manca {{/" + chiusura + "}}");
        if (pos[0] < t.length()) nodi.add(t.substring(pos[0]));
        pos[0] = t.length();
        return nodi;
    }

    private static void scrivi(List<Object> nodi, List<Object> pila, StringBuilder out) {
        for (Object n : nodi) {
            if (n instanceof String) {
                out.append((String) n);
            } else if (n instanceof Valore) {
                Valore v = (Valore) n;
                String testo = testo(cerca(v.nome, pila));
                out.append(v.raw ? testo : TextUtils.htmlEncode(testo));
            } else {
                Sezione s = (Sezione) n;
                Object valore = cerca(s.nome, pila);
                boolean vuoto = vuoto(valore);
                if (s.inverso) {
                    if (vuoto) scrivi(s.figli, pila, out);
                    continue;
                }
                if (vuoto) continue;
                if (valore instanceof List) {
                    for (Object elemento : (List<?>) valore) {
                        pila.add(elemento);
                        scrivi(s.figli, pila, out);
                        pila.remove(pila.size() - 1);
                    }
                } else if (valore instanceof Map) {
                    pila.add(valore);
                    scrivi(s.figli, pila, out);
                    pila.remove(pila.size() - 1);
                } else {
                    scrivi(s.figli, pila, out);
                }
            }
        }
    }

    private static boolean vuoto(Object v) {
        if (v == null || Boolean.FALSE.equals(v)) return true;
        if (v instanceof String) return ((String) v).isEmpty() || "0".equals(v);
        if (v instanceof Number) return ((Number) v).doubleValue() == 0;
        if (v instanceof List) return ((List<?>) v).isEmpty();
        if (v instanceof Map) return ((Map<?, ?>) v).isEmpty();
        return false;
    }

    private static Object cerca(String nome, List<Object> pila) {
        if (".".equals(nome)) return pila.get(pila.size() - 1);
        String[] parti = nome.split("\\.");
        for (int i = pila.size() - 1; i >= 0; i--) {
            Object c = pila.get(i);
            if (c instanceof Map && ((Map<?, ?>) c).containsKey(parti[0])) {
                Object v = ((Map<?, ?>) c).get(parti[0]);
                for (int k = 1; k < parti.length; k++) v = figlio(v, parti[k]);
                return v;
            }
        }
        return null;
    }

    private static Object figlio(Object v, String parte) {
        if (v instanceof Map) return ((Map<?, ?>) v).get(parte);
        if (v instanceof List && parte.matches("\\d+")) {
            int i = Integer.parseInt(parte);
            List<?> l = (List<?>) v;
            return i < l.size() ? l.get(i) : null;
        }
        return null;
    }

    private static String testo(Object v) {
        if (v == null || Boolean.FALSE.equals(v) || v instanceof List || v instanceof Map) return "";
        if (Boolean.TRUE.equals(v)) return "1";
        return v.toString();
    }
}
