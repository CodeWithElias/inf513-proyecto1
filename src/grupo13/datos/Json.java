package grupo13.datos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector JSON mínimo (el JDK no trae uno). Devuelve Map, List, String, Double, Boolean o null.
 * Se usa para leer las respuestas de YouTube en la fuente de scraping.
 */
final class Json {

    private final String s;
    private int i;

    private Json(String s) {
        this.s = s;
    }

    static Object leer(String texto) {
        Json j = new Json(texto);
        j.espacios();
        Object v = j.valor();
        j.espacios();
        return v;
    }

    /** Busca, en todo el árbol, los objetos guardados bajo la clave 'clave' (p. ej. "channelRenderer"). */
    @SuppressWarnings("unchecked")
    static void buscar(Object nodo, String clave, List<Map<String, Object>> encontrados) {
        if (nodo instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) nodo).entrySet()) {
                if (e.getKey().equals(clave) && e.getValue() instanceof Map) {
                    encontrados.add((Map<String, Object>) e.getValue());
                } else {
                    buscar(e.getValue(), clave, encontrados);
                }
            }
        } else if (nodo instanceof List) {
            for (Object o : (List<Object>) nodo) {
                buscar(o, clave, encontrados);
            }
        }
    }

    /** Navega por una ruta de claves (y posiciones numéricas en listas); null si algo no existe. */
    @SuppressWarnings("unchecked")
    static Object ruta(Object nodo, Object... claves) {
        Object actual = nodo;
        for (Object c : claves) {
            if (c instanceof String && actual instanceof Map) {
                actual = ((Map<String, Object>) actual).get(c);
            } else if (c instanceof Integer && actual instanceof List && ((List<Object>) actual).size() > (Integer) c) {
                actual = ((List<Object>) actual).get((Integer) c);
            } else {
                return null;
            }
        }
        return actual;
    }

    /** Texto de un nodo YouTube: {"simpleText": ...} o {"runs": [{"text": ...}, ...]}. */
    @SuppressWarnings("unchecked")
    static String texto(Object nodo) {
        if (!(nodo instanceof Map)) {
            return null;
        }
        Map<String, Object> m = (Map<String, Object>) nodo;
        if (m.get("simpleText") instanceof String) {
            return (String) m.get("simpleText");
        }
        if (m.get("runs") instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Object r : (List<Object>) m.get("runs")) {
                Object t = ruta(r, "text");
                if (t instanceof String) {
                    sb.append(t);
                }
            }
            return sb.toString();
        }
        return null;
    }

    private Object valor() {
        char c = s.charAt(i);
        switch (c) {
            case '{':
                return objeto();
            case '[':
                return lista();
            case '"':
                return cadena();
            case 't':
                i += 4;
                return Boolean.TRUE;
            case 'f':
                i += 5;
                return Boolean.FALSE;
            case 'n':
                i += 4;
                return null;
            default:
                return numero();
        }
    }

    private Map<String, Object> objeto() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++;
        espacios();
        if (s.charAt(i) == '}') {
            i++;
            return m;
        }
        while (true) {
            espacios();
            String clave = cadena();
            espacios();
            i++; // ':'
            espacios();
            m.put(clave, valor());
            espacios();
            if (s.charAt(i++) == '}') {
                return m;
            }
        }
    }

    private List<Object> lista() {
        List<Object> l = new ArrayList<>();
        i++;
        espacios();
        if (s.charAt(i) == ']') {
            i++;
            return l;
        }
        while (true) {
            espacios();
            l.add(valor());
            espacios();
            if (s.charAt(i++) == ']') {
                return l;
            }
        }
    }

    private String cadena() {
        StringBuilder sb = new StringBuilder();
        i++; // comilla inicial
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = s.charAt(i++);
            switch (e) {
                case 'n': sb.append('\n'); break;
                case 't': sb.append('\t'); break;
                case 'r': sb.append('\r'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'u':
                    sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                    break;
                default: sb.append(e);
            }
        }
    }

    private Double numero() {
        int inicio = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
            i++;
        }
        return Double.valueOf(s.substring(inicio, i));
    }

    private void espacios() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
    }
}
