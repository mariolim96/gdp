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
 * * Questo Job si occupa di presidiare la cartella degli "storici" sul server SFTP.
 * Analizza le consegne caricate, verifica se la testata (cartella) esiste a sistema
 * e smista i file: in 'tmp' se tutto è ok, in 'errata' se ci sono anomalie.
 */
@ApplicationScoped
public class CheckConsegnaStoricoJob {

    // Formati data per log e gestione file
    private static final DateTimeFormatter DF_CARTELLA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PATTERN_CONS = "^CONS_\\d{4}-\\d{2}-\\d{2}$";

    // Codici esito standard richiesti dalle specifiche CSI
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

    /**
     * Punto di ingresso del Job. Esecuzione ogni 15 minuti.
     * Usiamo SKIP per evitare che un'esecuzione lenta si sovrapponga alla successiva.
     */
    @Scheduled(every = "15m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void eseguiCheckConsegnaStorico() {
        Log.infof("Avvio scansione consegne storiche su: %s", saltuarioDir);

        try (SftpSession session = sftpProducer.connect()) {
            ChannelSftp sftp = session.getChannel();

            // Recuperiamo solo le cartelle che rispettano il pattern CONS_YYYY-MM-DD
            List<String> consegne = elencaSottocartelle(sftp, saltuarioDir).stream()
                    .filter(name -> name.matches(PATTERN_CONS))
                    .toList();

            if (consegne.isEmpty()) {
                Log.infof("%s - Nessuna cartella di consegna trovata.", MSG_NESSUNA_CONS);
                return;
            }

            for (String nomeConsegna : consegne) {
                elaboraCartellaConsegna(sftp, nomeConsegna);
            }

            Log.infof("%s - Elaborazione completata con successo.", MSG_OK);

        } catch (Exception e) {
            Log.errorf("Errore critico durante la connessione o l'elaborazione SFTP: %s", e.getMessage(), e);
        }
    }

    /**
     * Analizza il contenuto di una singola cartella di consegna (es. CONS_2024-01-01).
     */
    private void elaboraCartellaConsegna(ChannelSftp sftp, String nomeConsegna) throws Exception {
        String pathConsegna = joinPath(saltuarioDir, nomeConsegna);
        List<ChannelSftp.LsEntry> entries = elencaSottocartelleEntry(sftp, pathConsegna);

        if (entries.isEmpty()) {
            Log.warnf("La consegna [%s] è presente ma vuota.", nomeConsegna);
            return;
        }

        for (ChannelSftp.LsEntry entry : entries) {
            String nomeCartella = entry.getFilename();
            String dataCartella = formattaMTime(entry.getAttrs().getMTime());
            LocalDate oggi = LocalDate.now();

            // Verifichiamo se la cartella dell'editore è censita sul nostro DB
            List<GdpTestata> testate = gdpTestataRepository.findByCartella(nomeCartella);

            if (testate.size() > 1) {
                // Errore: ci sono più testate con la stessa cartella (ambiguità)
                gestisciAnomalia(sftp, nomeConsegna, nomeCartella, oggi, MSG_DUPLICATO);
            } else if (testate.isEmpty()) {
                // Errore: la cartella non corrisponde a nessuna testata nota
                gestisciAnomalia(sftp, nomeConsegna, nomeCartella, oggi, MSG_NON_TROVATO);
            } else {
                // Tutto ok: procediamo con lo spostamento verso l'area temporanea di lavorazione
                procediConSuccesso(sftp, testate.get(0), nomeConsegna, nomeCartella, dataCartella, oggi);
            }
        }

        // Se abbiamo processato tutto con successo, la cartella madre CONS_ deve essere rimossa
        rimuoviConsegnaSeVuota(sftp, pathConsegna, nomeConsegna);
    }

    /**
     * Sposta la cartella nell'area 'errata' e traccia l'anomalia sul database.
     * Usiamo l'ID testata 0 (testata tecnica) come da specifiche per le anomalie.
     */
    private void gestisciAnomalia(ChannelSftp sftp, String consegna, String cartella, LocalDate oggi, String msgErrore) {
        String source = joinPath(saltuarioDir, consegna, cartella);
        String target = joinPath(errataDir, consegna, cartella);

        int fileCount = contaFileInCartella(sftp, source);
        spostaCartella(sftp, source, target);

        inserisciGdpLog(0, oggi, fileCount, msgErrore);
    }

    /**
     * Sposta la cartella nell'area 'tmp' e prepara il terreno per il Job F07.
     */
    private void procediConSuccesso(ChannelSftp sftp, GdpTestata t, String consegna, String cartella, String dataCartella, LocalDate oggi) {
        String source = joinPath(saltuarioDir, consegna, cartella);
        String target = joinPath(tmpDir, consegna, cartella);

        int fileCount = contaFileInCartella(sftp, source);
        spostaCartella(sftp, source, target);

        // Logghiamo l'esito positivo per la testata specifica
        inserisciGdpLog(t.id, oggi, fileCount, MSG_OK);

        Log.infof("[F07] Segnale di testata pronta per elaborazione: %s", cartella);
        // Nota: Qui andrebbe chiamata la logica del servizio F07
    }

    /**
     * Registra l'operazione nella tabella GDP_LOG.
     * Utilizza il mapper per trasformare il DTO nell'entity finale.
     */
    @Transactional
    public void inserisciGdpLog(Integer fkTestata, LocalDate dataAcq, Integer totaleFile, String esito) {
        try {
            // Costruiamo il DTO per il passaggio al mapper
            AcquisizioneDetail dto = new AcquisizioneDetail();
            dto.setIdTestata(fkTestata);
            dto.setNroTotFile(totaleFile);
            dto.setEsito(esito);
            dto.setTipoAcquisizione(it.csipiemonte.gdp.gdporch.dto.AcquisizioneDetail.TipoAcquisizioneEnum.S);
            dto.setDataAcquisizione(dataAcq.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());

            // L'utente FTP è necessario per il vincolo di integrità del log
            GdpUtenteSftp utente = new GdpUtenteSftp();
            utente.id = defaultUtenteFtpId;

            GdpLog log = acquisizioneMapper.toEntity(dto, utente);
            log.persist();

            Log.infof("Log DB registrato - Testata: %d, Esito: %s", fkTestata, esito);
        } catch (Exception e) {
            Log.errorf("Impossibile salvare il log su DB: %s", e.getMessage());
        }
    }

    // --- SEZIONE UTILITY SFTP ---

    /**
     * Recupera i nomi delle sottocartelle escludendo i puntatori di sistema '.' e '..'
     */
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
     * Esegue il rename (spostamento) sul server SFTP garantendo la creazione dei folder di destinazione.
     */
    private void spostaCartella(ChannelSftp sftp, String src, String dst) {
        try {
            // Ci assicuriamo che il path di destinazione esista prima del rename
            creaDirectoryRicorsiva(sftp, dst.substring(0, dst.lastIndexOf("/")));
            sftp.rename(src, dst);
            Log.infof("SFTP Move: %s -> %s", src, dst);
        } catch (Exception e) {
            Log.errorf("Errore durante lo spostamento del file su SFTP: %s", e.getMessage());
        }
    }

    /**
     * Crea ricorsivamente le directory se non presenti (simile a mkdir -p).
     */
    private void creaDirectoryRicorsiva(ChannelSftp sftp, String path) {
        StringBuilder currentPath = new StringBuilder();
        for (String segmento : path.split("/")) {
            if (segmento.isEmpty()) continue;
            currentPath.append("/").append(segmento);
            try {
                sftp.mkdir(currentPath.toString());
            } catch (Exception ignored) {
                // Se la cartella esiste già, JSch lancia eccezione: la ignoriamo e procediamo
            }
        }
    }

    private int contaFileInCartella(ChannelSftp sftp, String path) {
        try {
            return (int) new ArrayList<>(sftp.ls(path)).stream().filter(e -> !e.getAttrs().isDir()).count();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Rimuove la cartella CONS_ solo se è rimasta completamente vuota dopo lo smistamento.
     */
    private void rimuoviConsegnaSeVuota(ChannelSftp sftp, String pathConsegna, String nomeConsegna) {
        try {
            boolean haContenuto = new ArrayList<>(sftp.ls(pathConsegna)).stream()
                    .anyMatch(e -> !e.getFilename().equals(".") && !e.getFilename().equals(".."));

            if (!haContenuto) {
                sftp.rmdir(pathConsegna);
                Log.infof("Cartella di consegna %s rimossa (svuotata).", nomeConsegna);
            }
        } catch (Exception e) {
            Log.errorf("Errore pulizia cartella %s: %s", nomeConsegna, e.getMessage());
        }
    }

    /**
     * Utility per concatenare i segmenti di un path senza preoccuparsi degli slash duplicati.
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
     * Converte l'mTime (secondi da epoca) di JSch in una stringa leggibile.
     */
    private String formattaMTime(int mTime) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(mTime), ZoneId.systemDefault()).format(DF_CARTELLA);
    }
}