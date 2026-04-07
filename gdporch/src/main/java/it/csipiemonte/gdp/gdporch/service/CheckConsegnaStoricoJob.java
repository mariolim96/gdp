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
import it.csipiemonte.gdp.sftp.SftpClientProducer;
import it.csipiemonte.gdp.sftp.SftpSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * F06 - FTPsaltuario.checkConsegnaStorico
 * * Questo servizio presidia l'area di "deposito storico" sul server SFTP.
 * Il flusso prevede:
 * 1. Identificazione delle cartelle di consegna (CONS_yyyy-mm-dd).
 * 2. Verifica dell'esistenza degli editori (testate) su database.
 * 3. Smistamento file: in 'tmp' per la lavorazione o in 'errata' per anomalie.
 *
 * I messaggi MSG0000x seguono lo standard di tracciamento CSI.
 */
@ApplicationScoped
public class CheckConsegnaStoricoJob {

    private static final DateTimeFormatter DF_CARTELLA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PATTERN_CONS = "^CONS_\\d{4}-\\d{2}-\\d{2}$";

    // Codici esito per GDP_LOG
    private static final String MSG_OK           = "MSG00009";
    private static final String MSG_NESSUNA_CONS = "MSG00001";
    private static final String MSG_DUPLICATO    = "MSG00002";
    private static final String MSG_NON_TROVATO  = "MSG00003";

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

    public CheckConsegnaStoricoJob(SftpClientProducer sftpProducer,
                                   GdpTestataRepository gdpTestataRepository,
                                   AcquisizioneMapper acquisizioneMapper) {
        this.sftpProducer = sftpProducer;
        this.gdpTestataRepository = gdpTestataRepository;
        this.acquisizioneMapper = acquisizioneMapper;
    }

    // -------------------------------------------------------------------------
    // Entry point schedulato
    // -------------------------------------------------------------------------

    /**
     * Esecuzione ogni 15 minuti. SKIP evita sovrapposizioni se l'SFTP è lento.
     */
    @Scheduled(every = "15m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void eseguiCheckConsegnaStorico() {
        Log.infof("Avvio scansione consegne storiche su: %s", saltuarioDir);

        try (SftpSession session = sftpProducer.connect()) {
            ChannelSftp sftp = session.getChannel();

            // Filtriamo solo le cartelle che seguono il pattern CONS_...
            List<String> consegne = elencaSottocartelle(sftp, saltuarioDir).stream()
                    .filter(name -> name.matches(PATTERN_CONS))
                    .toList();

            if (consegne.isEmpty()) {
                Log.infof("%s - Nessuna cartella di consegna trovata.", MSG_NESSUNA_CONS);
                return;
            }

            // Cicliamo sulle giornate di consegna trovate.
            // Se una cartella fallisce, il try-catch interno protegge le altre.
            for (String nomeConsegna : consegne) {
                elaboraCartellaConsegna(sftp, nomeConsegna);
            }

            Log.infof("%s - Operazione conclusa correttamente.", MSG_OK);

        } catch (Exception e) {
            Log.errorf("Errore critico durante la connessione SFTP: %s", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Step 1 - Elaborazione singola cartella CONS_yyyy-mm-dd
    // -------------------------------------------------------------------------

    private void elaboraCartellaConsegna(ChannelSftp sftp, String nomeConsegna) {
        String pathConsegna = joinPath(saltuarioDir, nomeConsegna);
        try {
            List<ChannelSftp.LsEntry> entries = elencaSottocartelleEntry(sftp, pathConsegna);

            if (entries.isEmpty()) {
                Log.warnf("La consegna [%s] è presente ma vuota.", nomeConsegna);
                return;
            }

            // Ogni entry qui rappresenta una cartella editore
            for (ChannelSftp.LsEntry entry : entries) {
                elaboraTestata(sftp, entry, nomeConsegna);
            }

            // Una volta svuotata dai file validi/errati, proviamo a rimuovere la CONS_
            rimuoviConsegnaSeVuota(sftp, pathConsegna, nomeConsegna);

        } catch (Exception e) {
            Log.errorf("Impossibile processare la consegna [%s]: %s", nomeConsegna, e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Step 2 - Elaborazione singola testata (editore)
    // -------------------------------------------------------------------------

    private void elaboraTestata(ChannelSftp sftp, ChannelSftp.LsEntry entry, String nomeConsegna) {
        String nomeCartella = entry.getFilename();
        String dataCartella = formattaMTime(entry.getAttrs().getMTime());
        LocalDate oggi      = LocalDate.now();

        // Verifichiamo se la cartella editore è censita nel nostro sistema
        List<GdpTestata> testate = gdpTestataRepository.findByCartella(nomeCartella);

        if (testate.size() > 1) {
            Log.errorf("%s - Trovate più testate per la cartella [%s].", MSG_DUPLICATO, nomeCartella);
            gestisciAnomalia(sftp, nomeConsegna, nomeCartella, oggi, MSG_DUPLICATO);

        } else if (testate.isEmpty()) {
            Log.errorf("%s - Nessuna testata trovata per la cartella [%s].", MSG_NON_TROVATO, nomeCartella);
            gestisciAnomalia(sftp, nomeConsegna, nomeCartella, oggi, MSG_NON_TROVATO);

        } else {
            // Caso OK: testata trovata univocamente
            procediConSuccesso(sftp, testate.get(0), nomeConsegna, nomeCartella, dataCartella, oggi);
        }
    }

    // -------------------------------------------------------------------------
    // Gestione anomalie e Successi
    // -------------------------------------------------------------------------

    /**
     * Sposta la cartella in 'errata'. In caso di testata mancante o duplicata,
     * usiamo l'ID 0 (testata tecnica) per poter salvare il log rispettando i vincoli DB.
     */
    private void gestisciAnomalia(ChannelSftp sftp, String consegna, String cartella,
                                  LocalDate oggi, String msgErrore) {
        String source = joinPath(saltuarioDir, consegna, cartella);
        String target = joinPath(errataDir, consegna, cartella);

        int fileCount = contaFileInCartella(sftp, source);
        spostaCartella(sftp, source, target);
        inserisciGdpLog(0, oggi, fileCount, msgErrore);
    }

    /**
     * Sposta la cartella in 'tmp' per la futura elaborazione F07.
     */
    private void procediConSuccesso(ChannelSftp sftp, GdpTestata testata, String consegna,
                                    String cartella, String dataCartella, LocalDate oggi) {
        String source = joinPath(saltuarioDir, consegna, cartella);
        String target = joinPath(tmpDir, consegna, cartella);

        int fileCount = contaFileInCartella(sftp, source);
        spostaCartella(sftp, source, target);
        inserisciGdpLog(testata.id, oggi, fileCount, MSG_OK);

        Log.infof("Cartella [%s] (ID: %d) smistata correttamente in TMP.", cartella, testata.id);

        // TODO F07: Invocare qui il servizio di elaborazione una volta che sarà disponibile nel progetto.
    }

    // -------------------------------------------------------------------------
    // Persistenza
    // -------------------------------------------------------------------------

    @Transactional
    public void inserisciGdpLog(Integer fkTestata, LocalDate dataAcq, Integer totaleFile, String esito) {
        try {
            AcquisizioneDetail dto = new AcquisizioneDetail();
            dto.setIdTestata(fkTestata);
            dto.setNroTotFile(totaleFile);
            dto.setEsito(esito);
            dto.setTipoAcquisizione(AcquisizioneDetail.TipoAcquisizioneEnum.S);
            dto.setDataAcquisizione(dataAcq.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());

            // Prepariamo l'utente SFTP per il log (usiamo l'ID predefinito)
            GdpUtenteSftp utente = new GdpUtenteSftp();
            utente.id = defaultUtenteFtpId;

            GdpLog log = acquisizioneMapper.toEntity(dto, utente);
            log.persist();

            Log.debugf("GDP_LOG registrato per testata %d", fkTestata);

        } catch (Exception e) {
            Log.errorf("Errore persistenza log (ID Testata: %d): %s", fkTestata, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Sezione Utility SFTP
    // -------------------------------------------------------------------------

    private List<String> elencaSottocartelle(ChannelSftp sftp, String path) throws Exception {
        return new ArrayList<>(sftp.ls(path)).stream()
                .filter(e -> e.getAttrs().isDir() && !e.getFilename().equals(".") && !e.getFilename().equals(".."))
                .map(ChannelSftp.LsEntry::getFilename)
                .toList();
    }

    private List<ChannelSftp.LsEntry> elencaSottocartelleEntry(ChannelSftp sftp, String path) throws Exception {
        return new ArrayList<>(sftp.ls(path)).stream()
                .filter(e -> e.getAttrs().isDir() && !e.getFilename().equals(".") && !e.getFilename().equals(".."))
                .toList();
    }

    /**
     * Sposta una cartella rinominandola. Assicura che la struttura target esista.
     */
    private void spostaCartella(ChannelSftp sftp, String src, String dst) {
        try {
            creaDirectoryRicorsiva(sftp, dst.substring(0, dst.lastIndexOf("/")));
            sftp.rename(src, dst);
            Log.infof("Spostamento SFTP: %s -> %s", src, dst);
        } catch (Exception e) {
            Log.errorf("Errore spostamento cartella: %s", e.getMessage());
        }
    }

    /**
     * Crea le cartelle nel path se mancanti (mkdir -p).
     */
    private void creaDirectoryRicorsiva(ChannelSftp sftp, String path) {
        StringBuilder currentPath = new StringBuilder();
        for (String segmento : path.split("/")) {
            if (segmento.isEmpty()) continue;
            currentPath.append("/").append(segmento);
            try {
                sftp.mkdir(currentPath.toString());
            } catch (Exception ignored) {
                // Se esiste già, JSch lancia eccezione: la ignoriamo.
            }
        }
    }

    private int contaFileInCartella(ChannelSftp sftp, String path) {
        try {
            return (int) new ArrayList<>(sftp.ls(path)).stream()
                    .filter(e -> !e.getAttrs().isDir())
                    .count();
        } catch (Exception e) {
            return 0;
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
     * Unisce segmenti di path garantendo la presenza di slash singoli.
     */
    private String joinPath(String... segmenti) {
        StringBuilder sb = new StringBuilder();
        for (String s : segmenti) {
            if (s == null || s.isEmpty()) continue;
            if (!sb.isEmpty() && !sb.toString().endsWith("/")) sb.append("/");
            sb.append(s.startsWith("/") && !sb.isEmpty() ? s.substring(1) : s);
        }
        return sb.toString();
    }

    /**
     * Trasforma il timestamp SFTP in data leggibile per l'elaborazione.
     */
    private String formattaMTime(int mTime) {
        return LocalDateTime
                .ofInstant(Instant.ofEpochSecond(mTime), ZoneId.systemDefault())
                .format(DF_CARTELLA);
    }
}