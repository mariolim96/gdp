package it.csipiemonte.gdp.gdporch.service;

import com.jcraft.jsch.ChannelSftp;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import it.csipiemonte.gdp.gdporch.dto.AcquisizioneDetail;
import it.csipiemonte.gdp.gdporch.mapper.AcquisizioneMapper;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLog;
import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import it.csipiemonte.gdp.gdporch.model.entity.GdpUtenteSftp;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import it.csipiemonte.gdp.gdporch.utils.FileUtils;
import it.csipiemonte.gdp.gdporch.utils.SftpUtils;
import it.csipiemonte.gdp.sftp.SftpClientProducer;
import it.csipiemonte.gdp.sftp.SftpSession;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * F06 - FTPsaltuario.checkConsegnaStorico
 * * Questo servizio presidia l'area di "deposito storico" sul server SFTP.
 * Il flusso prevede:
 * 1. Identificazione degli utenti e delle relative cartelle di consegna (CONS_yyyy-mm-dd).
 * 2. Verifica dell'esistenza degli editori (testate) su database.
 * 3. Smistamento file: in 'tmp' per la lavorazione o in 'errata' per anomalie,
 * mantenendo la struttura utente/consegna.
 *
 * Gli esiti seguono lo standard di tracciamento CSI (mappati via GdpMessage).
 */
@ApplicationScoped
public class CheckConsegnaStoricoJob {

    private static final DateTimeFormatter DF_CARTELLA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PATTERN_CONS = "^CONS_\\d{4}-\\d{2}-\\d{2}$";

    // Esiti GDP_LOG gestiti direttamente via GdpMessage enum

    @ConfigProperty(name = "sftp.root.prefix.flussoSaltuario")
    String saltuarioDir;

    @ConfigProperty(name = "sftp.root.prefix.errata")
    String errataDir;

    @ConfigProperty(name = "sftp.root.prefix.tmp")
    String tmpDir;

    @ConfigProperty(name = "gdp.sftp.utente.id", defaultValue = "1")
    Integer defaultUtenteFtpId;

    private final SftpClientProducer sftpProducer;
    private final GdpTestataRepository gdpTestataRepository;
    private final AcquisizioneMapper acquisizioneMapper;
    private final GdpCtrlEdizioniStoricheService ctrlEdizioniStoricheService;
    @Inject
    public CheckConsegnaStoricoJob(SftpClientProducer sftpProducer,
                                   GdpTestataRepository gdpTestataRepository,
                                   AcquisizioneMapper acquisizioneMapper,
                                   GdpCtrlEdizioniStoricheService ctrlEdizioniStoricheService) {
        this.sftpProducer = sftpProducer;
        this.gdpTestataRepository = gdpTestataRepository;
        this.acquisizioneMapper = acquisizioneMapper;
        this.ctrlEdizioniStoricheService = ctrlEdizioniStoricheService;
    }

    // -------------------------------------------------------------------------
    // Entry point schedulato
    // -------------------------------------------------------------------------

    /**
     * Esecuzione giornaliera (orario serale) o intervallata.
     * SKIP evita sovrapposizioni se l'SFTP è lento.
     */
    @Scheduled(every = "15m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void eseguiCheckConsegnaStorico() {
        Log.infof("Avvio scansione consegne storiche su: %s", saltuarioDir);

        try (SftpSession session = sftpProducer.connect()) {
            ChannelSftp sftp = session.getChannel();

            // Recupero delle cartelle utente (es. /flusso_saltuario/utente1)
            List<String> utenti = SftpUtils.leggiNomiCartelle(sftp, saltuarioDir);

            if (utenti.isEmpty()) {
                Log.infof("%s - Nessuna cartella utente trovata.", GdpMessage.F06_NO_HISTORICAL.getCodice());
                return;
            }

            boolean trovataAlmenoUnaConsegna = false;

            for (String utente : utenti) {
                String pathUtente = FileUtils.joinPath(saltuarioDir, utente);

                List<String> consegne = SftpUtils.leggiNomiCartelle(sftp, pathUtente).stream()
                        .filter(name -> name.matches(PATTERN_CONS))
                        .toList();

                if (!consegne.isEmpty()) {
                    trovataAlmenoUnaConsegna = true;
                }

                for (String nomeConsegna : consegne) {
                    elaboraCartellaConsegna(sftp, utente, nomeConsegna);
                }
            }

            if (!trovataAlmenoUnaConsegna) {
                Log.infof("%s - %s", GdpMessage.F06_NO_HISTORICAL.getCodice(), GdpMessage.F06_NO_HISTORICAL.getDescrizioneDefault());
                return;
            }

            Log.infof("%s - Operazione conclusa correttamente.", GdpMessage.F06_OK.getCodice());

        } catch (Exception e) {
            Log.errorf("Errore critico durante la connessione SFTP: %s", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Step 1 - Elaborazione singola cartella CONS_yyyy-mm-dd di un utente
    // -------------------------------------------------------------------------

    private void elaboraCartellaConsegna(ChannelSftp sftp, String utente, String nomeConsegna) {
        String pathConsegna = FileUtils.joinPath(saltuarioDir, utente, nomeConsegna);
        try {
            List<ChannelSftp.LsEntry> entries = SftpUtils.leggiCartelle(sftp, pathConsegna);

            if (entries.isEmpty()) {
                Log.warnf("La consegna [%s/%s] è presente ma vuota.", utente, nomeConsegna);
                return;
            }

            // Ogni entry qui rappresenta una cartella editore/testata
            for (ChannelSftp.LsEntry entry : entries) {
                elaboraTestata(sftp, entry, utente, nomeConsegna);
            }

            // Una volta svuotata dai file validi/errati, proviamo a rimuovere la CONS_
            rimuoviConsegnaSeVuota(sftp, pathConsegna, nomeConsegna);

        } catch (Exception e) {
            Log.errorf("Impossibile processare la consegna [%s] per l'utente [%s]: %s", nomeConsegna, utente, e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Step 2 - Elaborazione singola testata (editore)
    // -------------------------------------------------------------------------

    private void elaboraTestata(ChannelSftp sftp, ChannelSftp.LsEntry entry, String utente, String nomeConsegna) {
        String nomeCartella = entry.getFilename();
        // Timestamp reale della cartella SFTP (yyyy-MM-dd HH:mm:ss)
        String dataCartella = formattaMTime(entry.getAttrs().getMTime());

        // Verifichiamo se la cartella editore è censita nel nostro sistema
        List<GdpTestata> testate = gdpTestataRepository.findByCartella(nomeCartella);

        if (testate.size() > 1) {
            String ids = testate.stream()
                    .map(t -> String.valueOf(t.id))
                    .collect(Collectors.joining(","));
            Log.errorf("%s - Trovate più testate per la cartella [%s]: %s", GdpMessage.F06_AMBIGUOUS_TESTATA.getCodice(), nomeCartella, ids);
            gestisciAnomalia(sftp, utente, nomeConsegna, nomeCartella, dataCartella, GdpMessage.F06_AMBIGUOUS_TESTATA);

        } else if (testate.isEmpty()) {
            Log.errorf("%s - Nessuna testata trovata per la cartella [%s].", GdpMessage.F06_TESTATA_NOT_FOUND.getCodice(), nomeCartella);
            gestisciAnomalia(sftp, utente, nomeConsegna, nomeCartella, dataCartella, GdpMessage.F06_TESTATA_NOT_FOUND);

        } else {
            // Caso OK: testata trovata univocamente
            procediConSuccesso(sftp, testate.get(0), utente, nomeConsegna, nomeCartella, dataCartella);
        }
    }

    // -------------------------------------------------------------------------
    // Gestione anomalie e Successi
    // -------------------------------------------------------------------------

    /**
     * Sposta la cartella in 'errata'. In caso di testata mancante o duplicata,
     * usiamo l'ID 0 (testata tecnica) per poter salvare il log rispettando i vincoli DB.
     */
    private void gestisciAnomalia(ChannelSftp sftp, String utente, String consegna, String cartella,
                                  String dataCartella, GdpMessage message) {
        String cartellaSanificata = FileUtils.sanitizePathComponent(cartella);
        String source = FileUtils.joinPath(saltuarioDir, utente, consegna, cartella);
        String target = FileUtils.joinPath(errataDir, utente, consegna, cartellaSanificata);

        int fileCount = SftpUtils.contaFileInCartella(sftp, source);
        spostaCartella(sftp, source, target);
        inserisciGdpLog(0, dataCartella, fileCount, message);

        Log.infof("<%s> Anomalia rilevata per [%s]. Cartella spostata in ERRATA.", message.getCodice(), cartella);
    }

    /**
     * Sposta la cartella in 'tmp' per la futura elaborazione F07.
     */
    private void procediConSuccesso(ChannelSftp sftp, GdpTestata testata, String utente, String consegna,
                                    String cartella, String dataCartella) {
        String cartellaSanificata = FileUtils.sanitizePathComponent(cartella);
        String source = FileUtils.joinPath(saltuarioDir, utente, consegna, cartella);
        String target = FileUtils.joinPath(tmpDir, utente, consegna, cartellaSanificata);

        int fileCount = SftpUtils.contaFileInCartella(sftp, source);
        spostaCartella(sftp, source, target);
        Integer idLog = inserisciGdpLog(testata.id, dataCartella, fileCount, GdpMessage.F06_OK);

        Log.infof("Cartella [%s] (Log ID: %d, Testata ID: %d) dell'utente [%s] smistata correttamente in TMP.", cartella, idLog, testata.id, utente);

        //  Invocazione asincrona del servizio di controllo edizioni storiche.
        // Il servizio (da implementare in branch/f07) riceverà:
        // - idLog: l'ID del log appena creato
        // - idTestata: l'ID della testata individuata
        // - cartella: il nome della cartella SFTP
        // - dataConsegna: la data ricavata da nomeConsegna (CONS_yyyy-mm-dd)
        ctrlEdizioniStoricheService.ctrlEdizioniStoriche(
                testata.id,
                FileUtils.sanitizePathComponent(cartella),
                consegna,
                idLog
        );
    }

    // -------------------------------------------------------------------------
    // Persistenza
    // -------------------------------------------------------------------------

    /**
     * Registra l'esito dell'operazione su GDP_LOG.
     * Converte la stringa timestamp dell'SFTP in OffsetDateTime per il database.
     */
    @Transactional
    public Integer inserisciGdpLog(Integer fkTestata, String dataCartella, Integer totaleFile, GdpMessage message) {
        try {
            LocalDateTime ldt = LocalDateTime.parse(dataCartella, DF_CARTELLA);

            AcquisizioneDetail dto = new AcquisizioneDetail();
            dto.setIdTestata(fkTestata);
            dto.setNroTotFile(totaleFile);
            dto.setEsito(message.getCodice());
            dto.setTipoAcquisizione(AcquisizioneDetail.TipoAcquisizioneEnum.S);
            dto.setDataAcquisizione(java.util.Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));

            // Prepariamo l'utente SFTP per il log (usiamo l'ID predefinito da config)
            GdpUtenteSftp utente = new GdpUtenteSftp();
            utente.id = defaultUtenteFtpId;

            GdpLog log = acquisizioneMapper.toEntity(dto, utente);
            log.persist();

            Log.debugf("GDP_LOG registrato per testata %d con ID %d", fkTestata, log.id);
            return log.id;

        } catch (Exception e) {
            Log.errorf("Errore persistenza log (ID Testata: %d): %s", fkTestata, e.getMessage());
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // Sezione Utility SFTP
    // -------------------------------------------------------------------------



    /**
     * Sposta una cartella rinominandola. Assicura che la struttura target esista.
     */
    private void spostaCartella(ChannelSftp sftp, String src, String dst) {
        try {
            SftpUtils.creaDirectoryRicorsiva(sftp, dst.substring(0, dst.lastIndexOf("/")));
            sftp.rename(src, dst);
            Log.infof("Spostamento SFTP: %s -> %s", src, dst);
        } catch (Exception e) {
            Log.errorf("Errore spostamento cartella: %s", e.getMessage());
        }
    }



    /**
     * Tenta di rimuovere la cartella di consegna madre.
     * Se è rimasto qualcosa dentro (es. file non previsti), lo segnala nel log.
     */
    private void rimuoviConsegnaSeVuota(ChannelSftp sftp, String pathConsegna, String nomeConsegna) {
        try {
            boolean ancoraPiena = new ArrayList<>(sftp.ls(pathConsegna)).stream()
                    .anyMatch(e -> !e.getFilename().equals(".") && !e.getFilename().equals(".."));

            if (!ancoraPiena) {
                sftp.rmdir(pathConsegna);
                Log.infof("Consegna [%s] rimossa con successo.", nomeConsegna);
            } else {
                Log.infof("Consegna [%s] non vuota, rimozione saltata.", nomeConsegna);
            }
        } catch (Exception e) {
            Log.errorf("Errore pulizia cartella %s: %s", nomeConsegna, e.getMessage());
        }
    }



    /**
     * Trasforma il timestamp SFTP (mTime) in data leggibile per l'elaborazione.
     */
    private String formattaMTime(int mTime) {
        return LocalDateTime
                .ofInstant(Instant.ofEpochSecond(mTime), ZoneId.systemDefault())
                .format(DF_CARTELLA);
    }


}