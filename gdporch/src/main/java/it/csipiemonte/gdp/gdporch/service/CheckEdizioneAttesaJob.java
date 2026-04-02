package it.csipiemonte.gdp.gdporch.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLog;
import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import it.csipiemonte.gdp.sftp.SftpClientProducer;
import it.csipiemonte.gdp.sftp.SftpSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

@ApplicationScoped
public class CheckEdizioneAttesaJob {

    @ConfigProperty(name = "sftp.root.prefix")
    String sourceDir;

    @ConfigProperty(name = "sftp.root.prefix.tmp")
    String tmpDir;

    @ConfigProperty(name = "sftp.root.prefix.errata")
    String errataDir;

    @ConfigProperty(name = "sftp.file.stable.seconds", defaultValue = "180")

    int stableSeconds;
    private final SftpClientProducer sftpProducer;
    private final GdpTestataRepository testataRepository;
    private final GdpLogRepository logRepository;

    public CheckEdizioneAttesaJob(SftpClientProducer sftpProducer, GdpTestataRepository testataRepository, GdpLogRepository logRepository) {
        this.sftpProducer = sftpProducer;
        this.testataRepository = testataRepository;
        this.logRepository = logRepository;
    }

    //Metodo principale che richiama tutto il flusso
    @Transactional
    @Scheduled(cron = "0/10 * * * * ?")
    public void monitorAllFile(){
        Log.info("[F03] Avvio processo monitoraggio file");
        try(SftpSession session = sftpProducer.connect()){
            ChannelSftp sftp = session.getChannel();

            ArrayList<ChannelSftp.LsEntry> listaTestate = checkAndGetDirectory(sftp,sourceDir);

            if (!listaTestate.isEmpty()){
                for (ChannelSftp.LsEntry testata : listaTestate) {
                    String nomeTestata = testata.getFilename();
                    processaEdizioniPerTestata(sftp, nomeTestata);

                }
            }
            Log.info("[F03]MSG00009 Processo Completato");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //Metodo che controlla se nel path ci sono delle cartelle
    private ArrayList<ChannelSftp.LsEntry> checkAndGetDirectory(ChannelSftp sftp, String path) {
        ArrayList<ChannelSftp.LsEntry> list = new ArrayList<>();
        try {
            Log.info("[F03] Analizzando cartelle nel path: " + path);
            // 1. Legge tutto quello che c'è nel path
            ArrayList<ChannelSftp.LsEntry> entries = new ArrayList<>(sftp.ls(path));
            list.addAll(entries);

            // 2. Filtra che tiene solo cartelle vere
            list.removeIf(e -> !e.getAttrs().isDir() || isSystemDir(e));

            // 3. Controllo se è vuota logghiamo l'errore della specifica
            if (list.isEmpty()) {
                Log.error("[F03] MSG00001 - Nessuna cartella trovata in: " + path);
            }else{
                Log.info("[F03] Trovati nuovi file in: " + path);
            }

        } catch (SftpException e) {
            // Se la cartella non esiste proprio sul server
            Log.error("[F03] Errore SFTP o percorso inesistente: " + path);
            Log.error("[F03] MSG00001");
        }

        return list; // Ritorna la lista (piena o vuota che sia)
    }

    //Controlla se è una cartella di sistema
    private boolean isSystemDir(ChannelSftp.LsEntry e) {
        String name = e.getFilename();
        return name.equals(".") || name.equals("..");
    }

    // 2. Metodo separato che entra nella testata e cerca le date (edizioni)
    private void processaEdizioniPerTestata(ChannelSftp sftp, String nomeTestata) {
        String pathTestata = sourceDir + "/" + nomeTestata;
        Log.info("[F03] Entro nella testata: " + nomeTestata);

        ArrayList<ChannelSftp.LsEntry> listaEdizioni = checkAndGetDirectory(sftp, pathTestata);
        Log.infof("[F03] Edizioni trovate per %s: %d", nomeTestata, listaEdizioni.size());

        for (ChannelSftp.LsEntry edizione : listaEdizioni) {
            String nomeEdizione = edizione.getFilename();
            String pathEdizione = pathTestata + "/" + nomeEdizione;

            ArrayList<ChannelSftp.LsEntry> files = getFiles(sftp, pathEdizione);

            if (files.isEmpty()) {
                Log.warnf("[F03] Cartella edizione vuota, skip: %s", pathEdizione);
                continue;
            }

            Log.infof("[F03] Trovati %d file in %s — avvio controllo stabilità",
                    files.size(), pathEdizione);

            if (isCartellaStabile(sftp, pathEdizione)) {
                Log.infof("[F03] OK: cartella pronta per essere processata: %s", pathEdizione);
                String dtAcquisizione = recuperoDataAcquisizione(files);
                Log.infof("[F03] DT_ACQUISIZIONE calcolata: %s", dtAcquisizione);

                ricavaIdentificativoEFaiOperazioni(sftp, nomeTestata, dtAcquisizione, pathEdizione, pathTestata);


            } else {
                Log.warnf("[F03] KO: cartella ancora in caricamento: %s", pathEdizione);
            }
        }
    }

    private ArrayList<ChannelSftp.LsEntry> getFiles(ChannelSftp sftp, String path) {
        ArrayList<ChannelSftp.LsEntry> list = new ArrayList<>();
        try {
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(path);
            list.addAll(entries);
            // Tiene solo i file veri (no dir, no ".", no "..")
            // Sposta tutti i file TRANNE quelli .OK
            list.removeIf(e -> e.getAttrs().isDir() || isSystemDir(e) || e.getFilename().toLowerCase().endsWith(".ok"));
        } catch (SftpException e) {
            Log.errorf("[F03] Errore lettura file in %s: %s", path, e.getMessage());
        }
        return list;
    }

    private String recuperoDataAcquisizione(ArrayList<ChannelSftp.LsEntry> files) {
        if (files == null || files.isEmpty()) {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        // Cerchiamo il file con il Time più recente
        ChannelSftp.LsEntry ultimoFile = files.stream()
                .max(Comparator.comparingInt(e -> e.getAttrs().getMTime()))
                .orElse(files.get(0));

        // Il tempo restituito da SFTP è in secondi , lo convertiamo in LocalDateTime
        long secondi = ultimoFile.getAttrs().getMTime();
        LocalDateTime dataUltimoFile = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(secondi),
                ZoneId.systemDefault()
        );

        // Formattazione richiesta: yyyy-mm-dd HH:MM:SS
        return dataUltimoFile.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    //Metodo che controlla se la dimensione è stabile
    private boolean isCartellaStabile(ChannelSftp sftp, String pathEdizione) {
        try {
            long dimensionePrecedente = -1;
            int tentativiStabili = 0;
            int tentativiTotali = 0;
            int maxTentativi = stableSeconds / 15; // es: 60/15 = 4 check

            // while è più naturale qui: "continua finché non hai esaurito i tentativi"
            while (tentativiTotali < maxTentativi) {

                long dimensioneAttuale = getDimensioneTotaleCartella(sftp, pathEdizione);

                if (dimensioneAttuale > 0 && dimensioneAttuale == dimensionePrecedente) {
                    tentativiStabili++;
                    Log.infof("[F03] [%s] stabile %d/%d (size=%d)",
                            pathEdizione, tentativiStabili, 3, dimensioneAttuale);

                    // 3 check consecutivi stabili = trasferimento completato
                    if (tentativiStabili >= 3) {
                        Log.infof("[F03] Cartella STABILE: %s", pathEdizione);
                        return true;
                    }
                } else {
                    // Qualcosa è cambiato → reset del contatore
                    tentativiStabili = 0;
                    Log.infof("[F03] [%s] ancora in trasferimento (size=%d)",
                            pathEdizione, dimensioneAttuale);
                }

                dimensionePrecedente = dimensioneAttuale;
                tentativiTotali++;
                Thread.sleep(15000);
            }

            Log.warnf("[F03] Timeout stabilità per: %s", pathEdizione);

        } catch (Exception e) {
            Log.error("[F03] Errore controllo stabilità: " + e.getMessage());
        }

        return false;
    }

    // Metodo di supporto per sommare il peso di tutti i file nella cartella
    private long getDimensioneTotaleCartella(ChannelSftp sftp, String path) throws SftpException {
        long totale = 0;
        Vector<ChannelSftp.LsEntry> files = sftp.ls(path);
        for (ChannelSftp.LsEntry file : files) {
            if (!file.getAttrs().isDir()) {
                totale += file.getAttrs().getSize();
            }
        }
        return totale;
    }

    private void ricavaIdentificativoEFaiOperazioni(
            ChannelSftp sftp,
            String      nomeCartellaTestata,
            String      dataAcquisizione,
            String      pathEdizione,
            String      pathTestata) {

        List<GdpTestata> testate = testataRepository.findByCartella(nomeCartellaTestata);

        if (testate.size() == 1) {
            processaEdizioneCorretta(sftp, testate.get(0), dataAcquisizione, pathEdizione, pathTestata);
        } else if (testate.size() > 1) {
            processaEdizioneErrata(sftp, nomeCartellaTestata, dataAcquisizione, pathEdizione);
        } else {
            Log.errorf("[F03] Nessuna testata trovata per cartella: %s", nomeCartellaTestata);
        }
    }

    private void processaEdizioneCorretta(
            ChannelSftp sftp,
            GdpTestata  testata,
            String      dataAcquisizione,
            String      pathEdizione,
            String      pathTestata) {

        try {
            String nomeEdizione = pathEdizione.substring(pathEdizione.lastIndexOf("/") + 1);
            String destPath = tmpDir + "/" + testata.cartellaTestata + "/" + nomeEdizione;

            mkdirRecursive(sftp, destPath);

            // Sposta tutti i file in tmp
            ArrayList<ChannelSftp.LsEntry> files = getFiles(sftp, pathEdizione);
            for (ChannelSftp.LsEntry file : files) {
                sftp.rename(
                        pathEdizione + "/" + file.getFilename(),
                        destPath     + "/" + file.getFilename()
                );
            }

            // Unico file marker a livello testata: 21-01-2024.OK
            String pathMarker = pathTestata + "/" + nomeEdizione + ".OK";
            sftp.put(new ByteArrayInputStream(new byte[0]), pathMarker);
            Log.infof("[F03] Marker creato: %s", pathMarker);

            //  CANCELLA LA CARTELLA ORIGINALE (che ora è vuota)
            try {
                sftp.rmdir(pathEdizione);
                Log.infof("[F03] Cartella sorgente eliminata: %s", pathEdizione);
            } catch (SftpException e) {
                Log.warnf("[F03] Impossibile eliminare la cartella %s (forse non è vuota?): %s",
                        pathEdizione, e.getMessage());
            }

            // Conta i file in tmp e salva GDP_LOG
            int totaleFileTmp = getFiles(sftp, destPath).size();
            salvaLog(testata.id, "G", dataAcquisizione, totaleFileTmp, null);

            Log.infof("[F03] Edizione %s spostata in TMP. File acquisiti: %d",
                    nomeEdizione, totaleFileTmp);

        } catch (SftpException e) {
            Log.errorf("[F03] Errore spostamento in TMP: %s", e.getMessage());
        }
    }

    private void processaEdizioneErrata(
            ChannelSftp sftp,
            String      nomeCartellaTestata,
            String      dataAcquisizione,
            String      pathEdizione) {

        try {
            String nomeEdizione = pathEdizione.substring(pathEdizione.lastIndexOf("/") + 1);

            // path: _errata/nomeCartellaTestata/nomeEdizione
            String destPath = errataDir + "/" + nomeCartellaTestata + "/" + nomeEdizione;

            mkdirRecursive(sftp, destPath);

            // Sposta solo i file PDF (come da spec)
            ArrayList<ChannelSftp.LsEntry> files = getFiles(sftp, pathEdizione);
            for (ChannelSftp.LsEntry file : files) {
                if (file.getFilename().toLowerCase().endsWith(".pdf")) {
                    sftp.rename(
                            pathEdizione + "/" + file.getFilename(),
                            destPath     + "/" + file.getFilename()
                    );
                }
            }

            // GDP_LOG con FK_GDP_TESTATA = 0 e ESITO = MSG00002
            salvaLog(0, "G", dataAcquisizione, 0, "MSG00002");

            Log.errorf("[F03] MSG00002 - Anomalia unicità testata, PDF spostati in errata: %s/%s",
                    nomeCartellaTestata, nomeEdizione);

        } catch (SftpException e) {
            Log.errorf("[F03] Errore spostamento in ERRATA: %s", e.getMessage());
        }
    }

    private void salvaLog(Integer testataId, String tipo, String dtAcq, int count, String esito) {
        GdpLog log = new GdpLog();
        log.fkGdpTestata        = testataId;
        log.tipoAcquisizione    = tipo;
        log.dataAcquisizione    = converteDataAcquisizione(dtAcq); // riuso metodo esistente
        log.totaleFileAcquisiti = count;
        log.esito               = esito;
        logRepository.persist(log);
    }


    private LocalDateTime converteDataAcquisizione(String dataAcquisizione) {
        if (dataAcquisizione == null || dataAcquisizione.isBlank()) {
            return LocalDateTime.now();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            return LocalDateTime.parse(dataAcquisizione, formatter);
        } catch (DateTimeParseException e) {
            Log.errorf("[F03] Formato data non valido: %s", dataAcquisizione);
            return LocalDateTime.now();
        }
    }

    private void mkdirRecursive(ChannelSftp sftp, String path) {
        String[] parti = path.split("/");
        StringBuilder corrente = new StringBuilder();
        for (String parte : parti) {
            if (parte.isEmpty()) { corrente.append("/"); continue; }
            corrente.append(parte).append("/");
            try {
                sftp.stat(corrente.toString());
            } catch (SftpException e) {
                try {
                    sftp.mkdir(corrente.toString());
                } catch (SftpException ex) {
                    Log.warnf("[F03] mkdir fallito per %s: %s", corrente, ex.getMessage());
                }
            }
        }
    }
}
