package it.csipiemonte.gdp.gdporch.utils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import io.quarkus.logging.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for SFTP operations using JSch.
 */
public class SftpUtils {

    private SftpUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Elenca le sottocartelle di un path (esclude . e ..).
     * 
     * @param sftp Il canale SFTP attivo.
     * @param path Il percorso della directory da esplorare.
     * @return Lista di nomi di sottocartelle.
     */
    public static List<String> leggiNomiCartelle(ChannelSftp sftp, String path) {
        return leggiCartelle(sftp, path).stream()
                .map(ChannelSftp.LsEntry::getFilename)
                .collect(Collectors.toList());
    }

    /**
     * Elenca le voci (LsEntry) delle sottocartelle di un path (esclude . e ..).
     * 
     * @param sftp Il canale SFTP attivo.
     * @param path Il percorso della directory da esplorare.
     * @return Lista di LsEntry delle sottocartelle.
     */
    public static List<ChannelSftp.LsEntry> leggiCartelle(ChannelSftp sftp, String path) {
        try {
            java.util.Vector<ChannelSftp.LsEntry> vector = sftp.ls(path);
            return new ArrayList<>(vector).stream()
                    .filter(e -> e.getAttrs().isDir()
                            && !e.getFilename().equals(".")
                            && !e.getFilename().equals(".."))
                    .collect(Collectors.toList());
        } catch (SftpException e) {
            Log.warnf("Errore lettura cartelle in [%s]: %s", path, e.getMessage());
            return List.of();
        }
    }

    /**
     * Conta i file (non directory) presenti nel path specificato.
     * 
     * @param sftp Il canale SFTP attivo.
     * @param path Il percorso della directory.
     * @return Numero di file trovati.
     */
    public static int contaFileInCartella(ChannelSftp sftp, String path) {
        try {
            java.util.Vector<ChannelSftp.LsEntry> vector = sftp.ls(path);
            return (int) new ArrayList<>(vector).stream()
                    .filter(e -> !e.getAttrs().isDir())
                    .count();
        } catch (SftpException e) {
            Log.warnf("Impossibile contare i file in [%s]: %s", path, e.getMessage());
            return 0;
        }
    }

    /**
     * Crea ricorsivamente le directory se non presenti (equivalente a mkdir -p).
     * 
     * @param sftp Il canale SFTP attivo.
     * @param path Il percorso completo della directory da creare.
     */
    public static void creaDirectoryRicorsiva(ChannelSftp sftp, String path) {
        StringBuilder currentPath = new StringBuilder();
        if (path.startsWith("/")) {
            currentPath.append("/");
        }
        for (String segmento : path.split("/")) {
            if (segmento.isEmpty()) continue;
            
            if (currentPath.length() > 0 && !currentPath.toString().endsWith("/")) {
                currentPath.append("/");
            }
            currentPath.append(segmento);
            
            try {
                sftp.mkdir(currentPath.toString());
            } catch (SftpException ignored) {
                // Directory esistente
            }
        }
    }
}
