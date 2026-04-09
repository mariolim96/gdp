package it.csipiemonte.gdp.gdporch.utils;

/**
 * Utility class for file and path operations.
 */
public class FileUtils {

    private FileUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Sanifica un componente di un path (es. nome cartella) rimuovendo caratteri pericolosi.
     * Impedisce path traversal e caratteri illegali per il file system.
     * 
     * @param part Il componente del path da sanificare.
     * @return Il componente sanificato, o "unknown" se nullo.
     */
    public static String sanitizePathComponent(String part) {
        if (part == null) return "unknown";
        return part.replaceAll("[\\\\/:*?\"<>|]", "_").replace("..", "_");
    }

    /**
     * Unisce segmenti di path garantendo la presenza di slash singoli tra di essi.
     * 
     * @param segmenti I segmenti di path da unire.
     * @return Il path completo risultante.
     */
    public static String joinPath(String... segmenti) {
        StringBuilder sb = new StringBuilder();
        for (String s : segmenti) {
            if (s == null || s.isEmpty()) continue;
            
            if (sb.length() > 0 && !sb.toString().endsWith("/")) {
                sb.append("/");
            }
            
            // Se sb non è vuoto e s inizia con /, rimuoviamo lo slash iniziale di s
            if (sb.length() > 0 && s.startsWith("/")) {
                sb.append(s.substring(1));
            } else {
                sb.append(s);
            }
        }
        return sb.toString();
    }
}
