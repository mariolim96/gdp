package it.csipiemonte.gdp.gdporch.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import it.csipiemonte.gdp.gdporch.dto.AcquisizioneDetail;
import it.csipiemonte.gdp.gdporch.mapper.AcquisizioneMapper;
import it.csipiemonte.gdp.gdporch.model.entity.GdpUtenteSftp;
import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import it.csipiemonte.gdp.sftp.SftpClientProducer;
import it.csipiemonte.gdp.sftp.SftpSession;
import it.csipiemonte.gdp.gdporch.utils.SftpUtils;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Date;

/**
 * F03 - FTPregolare.checkEdizioneAttesa
 *
 * Monitora il "flusso regolare" sul server SFTP.
 * Per ogni testata trovata, verifica la stabilità dei file e smista:
 *   - in '_tmp'    se la testata è censita univocamente su DB
 *   - in '_errata' se ci sono anomalie (duplicato o non trovata)
 *
 * Esiti previsti da specifica:
 *   F03_NO_NEW_EDITION - Nessuna nuova edizione trovata
 *   F03_AMBIGUOUS_TESTATA - Anomalia unicità: più testate trovate per la stessa cartella
 *   F03_OK - Elaborazione completata con successo
 *
 * Nota: F03_TESTATA_NOT_FOUND non è previsto dalla specifica F03 ma viene gestito
 *       per robustezza nel caso in cui la testata non esista su DB.
 */
@ApplicationScoped
public class CheckEdizioneAttesaJob {

    private static final DateTimeFormatter DF_LOG        = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String            PATTERN_EDIZIONE = "^\\d{4}-\\d{2}-\\d{2}$";


    @ConfigProperty(name = "sftp.root.prefix")
    String sourceDir;

    @ConfigProperty(name = "sftp.root.prefix.tmp")
    String tmpDir;

    @ConfigProperty(name = "sftp.root.prefix.errata")
    String errataDir;

    @ConfigProperty(name = "sftp.file.stable.seconds")
    int stableSeconds;

    @ConfigProperty(name = "gdp.sftp.utente.id", defaultValue = "1")
    Integer defaultUtenteFtpId;

    @Inject
    ManagedExecutor managedExecutor;

    private final SftpClientProducer      sftpProducer;
    private final GdpTestataRepository    testataRepository;
    private final AcquisizioneMapper      acquisizioneMapper;
    private final GdpCtrlEdizioneAcquisitaService gdpCtrlEdizioneAcquisitaService;

    public CheckEdizioneAttesaJob(SftpClientProducer sftpProducer,
                                  GdpTestataRepository testataRepository,
                                  AcquisizioneMapper acquisizioneMapper,
                                  GdpCtrlEdizioneAcquisitaService gdpCtrlEdizioneAcquisitaService) {
        this.sftpProducer       = sftpProducer;
        this.testataRepository  = testataRepository;
        this.acquisizioneMapper = acquisizioneMapper;
        this.gdpCtrlEdizioneAcquisitaService = gdpCtrlEdizioneAcquisitaService;
    }

    // -------------------------------------------------------------------------
    // Entry point schedulato
    // -------------------------------------------------------------------------

    /**
     * Scansione ogni 15 minuti. SKIP evita sovrapposizioni.
     * Ogni testata viene elaborata in un thread separato tramite ManagedExecutor,
     * così lo scheduler non rimane bloccato durante la verifica di stabilità dei file.
     */
    @Scheduled(every = "15m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void monitorAllFile() {
        Log.infof("Avvio scansione flusso regolare su: %s", sourceDir);

        try (SftpSession session = sftpProducer.connect()) {
            ChannelSftp sftp = session.getChannel();
            List<ChannelSftp.LsEntry> testateSftp = SftpUtils.leggiCartelle(sftp, sourceDir);

            if (testateSftp.isEmpty()) {
                Log.infof("%s - Nessuna nuova edizione trovata.", GdpMessage.F03_NO_NEW_EDITION.getCodice());
                return;
            }

            for (ChannelSftp.LsEntry cartellaEntry : testateSftp) {
                String nomeTestata = cartellaEntry.getFilename();
                managedExecutor.runAsync(() -> processaTestataAsincrona(nomeTestata));
            }

            Log.infof("%s - Task di scansione lanciati per %d testate.", GdpMessage.F03_OK.getCodice(), testateSftp.size());

        } catch (Exception e) {
            Log.errorf("Errore critico durante il monitoraggio SFTP: %s", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Step 1 - Worker asincrono per singola testata
    // -------------------------------------------------------------------------

    /**
     * Eseguito in un thread separato del ManagedExecutor.
     * Apre una propria sessione SFTP per thread-safety.
     */
    @ActivateRequestContext
     void processaTestataAsincrona(String nomeTestata) {
        try (SftpSession session = sftpProducer.connect()) {
            ChannelSftp sftp = session.getChannel();
            processaEdizioniPerTestata(sftp, nomeTestata);
        } catch (Exception e) {
            Log.errorf("Errore nel worker asincrono per testata [%s]: %s", nomeTestata, e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Step 2 - Elaborazione edizioni per singola testata
    // -------------------------------------------------------------------------

    void processaEdizioniPerTestata(ChannelSftp sftp, String nomeTestata) {
        String pathTestata = sourceDir + "/" + nomeTestata;
        List<ChannelSftp.LsEntry> edizioniSftp = SftpUtils.leggiCartelle(sftp, pathTestata);

        for (ChannelSftp.LsEntry edizione : edizioniSftp) {
            // FIX: ogni edizione è elaborata in modo indipendente —
            // un errore su una non blocca le successive della stessa testata
            try {
                String nomeEdizione = edizione.getFilename();

                if (!nomeEdizione.matches(PATTERN_EDIZIONE)) {
                    Log.warnf("Cartella ignorata (formato non conforme): %s", nomeEdizione);
                    continue;
                }

                String pathEdizione = pathTestata + "/" + nomeEdizione;

                List<ChannelSftp.LsEntry> pdfsDaLavorare = getFileSenzaMarker(sftp, pathEdizione)
                        .stream()
                        .filter(e -> e.getFilename().toLowerCase().endsWith(".pdf"))
                        .toList();

                if (pdfsDaLavorare.isEmpty()) {
                    Log.debugf("Nessun PDF da lavorare in [%s], skip.", pathEdizione);
                    continue;
                }

                if (!isCartellaStabile(sftp, pathEdizione)) {
                    Log.warnf("Edizione [%s/%s] non stabile. Riprovo al prossimo ciclo.", nomeTestata, nomeEdizione);
                    continue;
                }

                String dtAcquisizione = calcolaDataAcquisizione(pdfsDaLavorare);
                smistaEdizione(sftp, nomeTestata, nomeEdizione, dtAcquisizione, pathEdizione, pdfsDaLavorare);

            } catch (Exception e) {
                Log.errorf("Errore durante l'elaborazione dell'edizione [%s] per testata [%s]: %s",
                        edizione.getFilename(), nomeTestata, e.getMessage(), e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Step 3 - Validazione testata e smistamento
    // -------------------------------------------------------------------------

    @Transactional
     void smistaEdizione(ChannelSftp sftp, String nomeCartella, String nomeEdizione, String dtAcq,
                                String pathEdizione, List<ChannelSftp.LsEntry> files) {

        List<GdpTestata> testateDb = testataRepository.findByCartella(nomeCartella);

        if (testateDb.size() > 1) {
            String ids = testateDb.stream().map(t -> String.valueOf(t.id)).collect(Collectors.joining(", "));
            Log.errorf("%s - Trovate più testate per [%s]: [%s]. Spostamento in errata.",
                    GdpMessage.F03_AMBIGUOUS_TESTATA.getCodice(), nomeCartella, ids);
            procediErrore(sftp, nomeCartella, nomeEdizione, dtAcq, pathEdizione, files, GdpMessage.F03_AMBIGUOUS_TESTATA);

        } else if (testateDb.isEmpty()) {
            Log.errorf("%s - Nessuna testata trovata per [%s]. Spostamento in errata.",
                    GdpMessage.F03_TESTATA_NOT_FOUND.getCodice(), nomeCartella);
            procediErrore(sftp, nomeCartella, nomeEdizione, dtAcq, pathEdizione, files, GdpMessage.F03_TESTATA_NOT_FOUND);

        } else {
            procediSuccesso(sftp, testateDb.get(0), nomeEdizione, dtAcq, pathEdizione, files);
        }
    }

    // -------------------------------------------------------------------------
    // Happy path - testata trovata univocamente
    // -------------------------------------------------------------------------

    /**
     * Sposta i PDF in '_tmp', crea i marker .OK in sorgente,
     * conta i file effettivamente presenti in tmp dopo lo spostamento
     * e registra il log su GDP_LOG.
     */
    private void procediSuccesso(ChannelSftp sftp, GdpTestata testata, String nomeEdizione,
                                 String dtAcq, String pathEdizione, List<ChannelSftp.LsEntry> files) {
        try {
            String targetPath = tmpDir + "/" + testata.cartellaTestata + "/" + nomeEdizione;
            SftpUtils.creaDirectoryRicorsiva(sftp, targetPath);

            for (ChannelSftp.LsEntry file : files) {
                String nomeFile          = file.getFilename();
                String sorgenteFile      = pathEdizione + "/" + nomeFile;
                String destinazioneFile  = targetPath + "/" + nomeFile;

                // 1. Sposto il PDF in _tmp
                sftp.rename(sorgenteFile, destinazioneFile);

                // 2. Creo il marker .OK nella posizione originale (ricevuta come da specifica)
                sftp.put(new ByteArrayInputStream(new byte[0]), sorgenteFile + ".OK");
            }

            int totaleFileInTmp = SftpUtils.contaFileInCartella(sftp, targetPath);
            Integer idLog = inserisciGdpLog(testata.id, dtAcq, totaleFileInTmp, GdpMessage.F03_OK);

            Log.infof("Edizione [%s/%s] acquisita correttamente (Log ID: %d). File in TMP: %d",
                    testata.cartellaTestata, nomeEdizione, idLog, totaleFileInTmp);

            // Trigger F04 pipeline asynchronously
            if (idLog != null) {
                managedExecutor.runAsync(() -> {
                    try {
                        gdpCtrlEdizioneAcquisitaService.ctrlEdizioneAcquisita(idLog, testata.cartellaTestata, nomeEdizione, dtAcq, totaleFileInTmp);
                    } catch (Exception e) {
                        Log.errorf("Errore durante l'avvio della pipeline F04 per log %d: %s", idLog, e.getMessage());
                    }
                });
            }

        } catch (SftpException e) {
            Log.errorf("Errore I/O SFTP in procediSuccesso per edizione [%s]: %s", nomeEdizione, e.getMessage(), e);
        }
    }
    
    /**
     * Sposta i PDF in '_errata' e registra l'anomalia su GDP_LOG.
     * FK_GDP_TESTATA = 0 come da specifica per i casi di anomalia.
     */
    private void procediErrore(ChannelSftp sftp, String nomeCartella, String nomeEdizione,
                               String dtAcq, String pathEdizione, List<ChannelSftp.LsEntry> files, GdpMessage errore) {
        try {
            String targetPath = errataDir + "/" + nomeCartella + "/" + nomeEdizione;
            SftpUtils.creaDirectoryRicorsiva(sftp, targetPath);

            for (ChannelSftp.LsEntry file : files) {
                sftp.rename(
                        pathEdizione + "/" + file.getFilename(),
                        targetPath + "/" + file.getFilename()
                );
            }
            String elencoFile = files.stream()
                    .map(ChannelSftp.LsEntry::getFilename)
                    .collect(Collectors.joining("\n"));

            // FIX: passato files.size() invece di 0 hardcoded
            inserisciGdpLog(0, dtAcq, files.size(), errore);

            String ids = ""; // Se hai gli ID delle testate, puoi mapparli qui, altrimenti resta vuoto
            if (errore.equals(GdpMessage.F03_AMBIGUOUS_TESTATA)) {
                // esempio: ids = "12, 15, 18"; // recuperabili da testataRepository se vuoi
            }

            String msgDettagliato = String.format(
                    "%s <E001>Anomalia UNICITA’ TESTATA<E001>%n" +
                            "Per %s dell’edizione %s sono stati trovati: %s%n" +
                            "I file%n%s%n" +
                            "sono stati spostati in %s",
                    errore.getCodice(),
                    nomeCartella,
                    nomeEdizione,
                    ids,
                    elencoFile,
                    targetPath
            );

            // Log del messaggio dettagliato
            Log.info(msgDettagliato);

        } catch (SftpException e) {
            Log.errorf("Errore I/O SFTP in procediErrore per edizione [%s]: %s", nomeEdizione, e.getMessage(), e);
        }
    }

    @Transactional
    public Integer inserisciGdpLog(Integer fkTestata, String dataAcquisizione, Integer totaleFile, GdpMessage message) {
        try {
            LocalDateTime ldt = LocalDateTime.parse(dataAcquisizione, DF_LOG);

            AcquisizioneDetail dto = new AcquisizioneDetail();
            dto.setIdTestata(fkTestata);
            dto.setNroTotFile(totaleFile);
            dto.setEsito(message.getCodice());
            dto.setTipoAcquisizione(AcquisizioneDetail.TipoAcquisizioneEnum.G);
            dto.setDataAcquisizione(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));

            GdpUtenteSftp utente = new GdpUtenteSftp();
            utente.id = defaultUtenteFtpId;

            it.csipiemonte.gdp.gdporch.model.entity.GdpLog logEntity = acquisizioneMapper.toEntity(dto, utente);
            logEntity.persist();

            Log.infof("GDP_LOG registrato - ID: %d | FK_TESTATA: %d | Esito: %s | File: %d",
                    logEntity.id, fkTestata, message.getCodice(), totaleFile);

            return logEntity.id;

        } catch (Exception e) {
            Log.errorf("Errore persistenza GDP_LOG - FK: %d | Esito: %s | Errore: %s",
                    fkTestata, message.getCodice(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Verifica la stabilità dei file controllando che la dimensione
     * non cambi per un intero intervallo — come da specifica F03.
     * Ogni 15 secondi per un massimo di 3 minuti (stableSeconds).
     * Sicuro perché eseguito nel ManagedExecutor, non nello scheduler.
     */
    private boolean isCartellaStabile(ChannelSftp sftp, String path) {
        final int intervalSec = 15;
        final int maxChecks   = stableSeconds / intervalSec;

        try {
            Map<String, Long> lastSizes = new HashMap<>();
            for (int i = 0; i < maxChecks; i++) {
                java.util.Vector<ChannelSftp.LsEntry> vector = sftp.ls(path);
                Map<String, Long> currentSizes = new ArrayList<>(vector).stream()
                        .filter(e -> !e.getAttrs().isDir())
                        .collect(Collectors.toMap(
                                ChannelSftp.LsEntry::getFilename,
                                e -> (long) e.getAttrs().getSize()
                        ));

                if (i > 0 && !currentSizes.isEmpty() && currentSizes.equals(lastSizes)) {
                    Log.debugf("Cartella [%s] stabile dopo %d secondi.", path, i * intervalSec);
                    return true;
                }

                lastSizes = currentSizes;
                Thread.sleep(intervalSec * 1000L);
            }
        } catch (SftpException e) {
            Log.warnf("Errore SFTP durante verifica stabilità [%s]: %s", path, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.warnf("Thread interrotto durante verifica stabilità [%s].", path);
        }
        return false;
    }

    /**
     * Calcola la data di acquisizione dal timestamp dell'ultimo file
     * in ordine cronologico — come da specifica F03.
     */
    private String calcolaDataAcquisizione(List<ChannelSftp.LsEntry> files) {
        return files.stream()
                .max(Comparator.comparingInt(e -> e.getAttrs().getMTime()))
                .map(e -> LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(e.getAttrs().getMTime()), ZoneId.systemDefault()))
                .orElse(LocalDateTime.now())
                .format(DF_LOG);
    }

    /**
     * Restituisce i file che non hanno ancora un marker .OK associato.
     * Usato per evitare di riprocessare edizioni già elaborate.
     */
    private List<ChannelSftp.LsEntry> getFileSenzaMarker(ChannelSftp sftp, String path) {
        try {
            // 1. Leggiamo tutto il contenuto della cartella
            List<ChannelSftp.LsEntry> entries = new ArrayList<>(sftp.ls(path));

            // 2. Creiamo un set con i nomi di tutti i marker presenti (es. "test.pdf.OK")
            Set<String> markers = entries.stream()
                    .map(ChannelSftp.LsEntry::getFilename)
                    .filter(nome -> nome.endsWith(".OK"))
                    .collect(Collectors.toSet());

            // 3. Filtriamo i file: prendi il file SOLO SE non esiste il suo marker corrispondente
            return entries.stream()
                    .filter(e -> !e.getAttrs().isDir())
                    .filter(e -> !e.getFilename().endsWith(".OK"))
                    // Se esiste "articolo.pdf.OK", scarta "articolo.pdf"
                    .filter(e -> !markers.contains(e.getFilename() + ".OK"))
                    .toList();

        } catch (SftpException e) {
            Log.warnf("Errore lettura file senza marker in [%s]: %s", path, e.getMessage());
            return List.of();
        }
    }
}