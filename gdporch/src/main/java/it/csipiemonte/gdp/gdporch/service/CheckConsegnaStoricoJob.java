package it.csipiemonte.gdp.gdporch.service;

import com.jcraft.jsch.ChannelSftp;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import it.csipiemonte.gdp.sftp.SftpClientProducer;
import it.csipiemonte.gdp.sftp.SftpSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class CheckConsegnaStoricoJob {

    @ConfigProperty(name = "sftp.root.prefix.flussoSaltuario")
    String saltuarioDir;

    @ConfigProperty(name = "sftp.root.prefix.errata")
    String errataDir;

    private final SftpClientProducer sftpProducer;
    private final GdpTestataRepository gdpTestataRepository;

    public CheckConsegnaStoricoJob(SftpClientProducer sftpProducer, GdpTestataRepository gdpTestataRepository) {
        this.sftpProducer = sftpProducer;
        this.gdpTestataRepository = gdpTestataRepository;
    }

    @Scheduled(every = "15m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void entryPointCheckStorico(){
        boolean eseguitoConSuccesso = false;
        try(SftpSession session = sftpProducer.connect()) {
            ChannelSftp sftp = session.getChannel();
            Log.info("Avvio check storico su path: " + saltuarioDir);

            checkNuovaConsegna(sftp, saltuarioDir);

            eseguitoConSuccesso = true;
        } catch (Exception e) {
            Log.error("Errore connessione o esecuzione Job SFTP: " + e.getMessage());
        } finally {
            if (eseguitoConSuccesso) {
                Log.info("MSG00009 - Operazione conclusa correttamente.");
            }
        }
    }

    private void checkNuovaConsegna(ChannelSftp sftp, String path){
        try {
            List<ChannelSftp.LsEntry> entries = new ArrayList<>(sftp.ls(path));

            List<String> utenteStoricoFolderName = entries.stream()
                    .filter(entry -> entry.getAttrs().isDir())
                    .map(ChannelSftp.LsEntry::getFilename)
                    .filter(name -> !name.equals(".") && !name.equals(".."))
                    .toList();

            if (utenteStoricoFolderName.isEmpty()){
                Log.info("MSG00001 - Nessuna cartella di consegna trovata in " + path + ". Fine operazione.");
            } else {
                processaSottocartelle(sftp, path, utenteStoricoFolderName);
            }
        } catch (Exception e) {
            Log.error("Errore durante la scansione della root: " + path, e);
        }
    }

    private void processaSottocartelle(ChannelSftp sftp, String basePath, List<String> folderNames) {
        String regexPattern = "^CONS_\\d{4}-\\d{2}-\\d{2}$";

        for (String folderName : folderNames) {
            if (folderName.matches(regexPattern)) {
                String fullPath = basePath.endsWith("/") ? basePath + folderName : basePath + "/" + folderName;

                try {
                    List<ChannelSftp.LsEntry> entriesTecniche = new ArrayList<>(sftp.ls(fullPath))
                            .stream()
                            .filter(e -> e.getAttrs().isDir() && !e.getFilename().equals(".") && !e.getFilename().equals(".."))
                            .toList();

                    if (entriesTecniche.isEmpty()) {
                        Log.warn("Consegna " + folderName + " trovata, ma è vuota..");
                        continue;
                    }

                    for (ChannelSftp.LsEntry entrySub : entriesTecniche) {
                        elaboraFolder(sftp, entrySub.getFilename(), entrySub, fullPath, folderName);
                    }

                    // --- AGGIUNTA LOGICA DI PULIZIA ---
                    // Verifichiamo se dopo l'elaborazione la CONS_ è rimasta vuota
                    List<ChannelSftp.LsEntry> restanti = new ArrayList<>(sftp.ls(fullPath));
                    boolean ancoraFile = restanti.stream()
                            .anyMatch(e -> !e.getFilename().equals(".") && !e.getFilename().equals(".."));

                    if (!ancoraFile) {
                        sftp.rmdir(fullPath);
                        Log.infof("Cartella di consegna %s processata completamente e rimossa dal saltuario.", folderName);
                    } else {
                        Log.infof("Cartella di consegna %s non ancora vuota (alcune testate potrebbero essere rimaste in attesa).", folderName);
                    }
                    // ----------------------------------

                } catch (Exception e) {
                    Log.error("Errore nell'accesso o pulizia della consegna " + fullPath, e);
                }
            }
        }
    }

    private void elaboraFolder(ChannelSftp sftp, String nomeCartella, ChannelSftp.LsEntry entry, String fullPathConsegna, String folderName) {
        // Specifica: Recupero timestamp della cartella (MTime)
        // entry.getAttrs().getMTimeString() restituisce una stringa,
        // ma per precisione usiamo i secondi epoch:
        long mTimeLong = (long) entry.getAttrs().getMTime() * 1000L;
        java.util.Date dataModifica = new java.util.Date(mTimeLong);
        String dataCartella = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(dataModifica);

        String dataAcquisizione = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        List<GdpTestata> testate = gdpTestataRepository.findByCartella(nomeCartella);

        // Gestione path più sicura per evitare doppi slash //
        String cleanFullPath = fullPathConsegna.endsWith("/") ? fullPathConsegna : fullPathConsegna + "/";
        String sourcePath = cleanFullPath + nomeCartella;

        String cleanErrataPath = errataDir.endsWith("/") ? errataDir : errataDir + "/";
        String targetPath = cleanErrataPath + folderName + "/" + nomeCartella;

        if (testate.size() > 1) {
            processaErrore(sftp, sourcePath, targetPath, dataAcquisizione, "MSG00002");
            return;
        }

        if (testate.isEmpty()) {
            processaErrore(sftp, sourcePath, targetPath, dataAcquisizione, "MSG00003");
            return;
        }

        // Se arrivi qui, testate.size() == 1
        Log.infof("Testata %s OK (ID: %d). Data Cartella: %s", nomeCartella, testate.get(0).id, dataCartella);
    }

    private void processaErrore(ChannelSftp sftp, String source, String target, String dataAcq, String codiceEsito) {
        Log.errorf("%s - Problema testata su %s. Spostamento in errata.", codiceEsito, source);


        int numeroFile = contaFileInCartella(sftp, source);


        spostaInErrata(sftp, source, target);


        inserisciGdpLogErrore(0L, dataAcq, numeroFile, codiceEsito);
    }

    private void spostaInErrata(ChannelSftp sftp, String source, String target) {
        try {
            String targetDirOnly = target.substring(0, target.lastIndexOf("/"));
            ensureDirectoryExists(sftp, targetDirOnly);

            // Rimuovi target se esiste (opzionale, per evitare errori rename)
            try { sftp.rmdir(target); } catch (Exception e) { /* non esiste, ok */ }

            sftp.rename(source, target);
            Log.infof("Spostamento completato: %s -> %s", source, target);
        } catch (Exception e) {
            Log.error("Errore SFTP durante lo spostamento: " + source, e);
        }
    }

    private void ensureDirectoryExists(ChannelSftp sftp, String path) {
        String[] folders = path.split("/");
        StringBuilder currentPath = new StringBuilder();
        for (String f : folders) {
            if (f.isEmpty()) continue;
            currentPath.append("/").append(f);
            try {
                sftp.mkdir(currentPath.toString());
            } catch (Exception e) {
                // Esiste già, ignoriamo
            }
        }
    }

    private int contaFileInCartella(ChannelSftp sftp, String path) {
        try {
            // ls(path) elenca il contenuto della sottocartella tecnica
            List<ChannelSftp.LsEntry> files = new ArrayList<>(sftp.ls(path));
            // Contiamo solo gli elementi che non sono directory e non sono i puntatori . e ..
            return (int) files.stream()
                    .filter(e -> !e.getAttrs().isDir())
                    .count();
        } catch (Exception e) {
            Log.error("Impossibile contare i file nel percorso: " + path);
            return 0;
        }
    }

    @Transactional
    public void inserisciGdpLogErrore(Long fkTestata, String dataAcq, int totFile, String esito) {
        // Implementazione persistenza (esempio commentato)
        /*
        GdpLog log = new GdpLog();
        log.fkGdpTestata = fkTestata; // Sarà 0 come da specifica
        log.dtAcquisizione = dataAcq;
        log.totaleFileAcquisiti = totFile;
        log.esito = esito;
        gdpLogRepository.persist(log);
        */
        Log.infof("Persistenza DB -> GDP_LOG: FK=%d, Data=%s, Files=%d, Esito=%s",
                fkTestata, dataAcq, totFile, esito);
    }
}
