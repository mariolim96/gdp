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
import java.util.stream.Collectors;

/**
 * F03 - CheckEdizioneAttesa
 *
 * Servizio schedulato ogni 15 minuti che scansiona la cartella SFTP [flusso_regolare]
 * alla ricerca di nuove edizioni da acquisire.
 *
 * Struttura cartelle SFTP attesa:
 *   flusso_regolare/
 *     <cartellaTestata>/          ← nome cartella = CARTELLA_TESTATA su DB
 *       <nomeEdizione>/           ← es. 2024-12-02
 *         pag1.pdf
 *         pag2.pdf
 *         pag1.pdf.OK             ← marker: file già spostato in tmp
 *
 * Flusso principale:
 *   1. Scansiona tutte le testate in [flusso_regolare]
 *   2. Per ogni testata scansiona le edizioni
 *   3. Per ogni edizione:
 *      a. Verifica se ci sono file senza marker .OK (da processare)
 *      b. Controlla la stabilità della cartella (dimensione invariata per 3 check da 15")
 *      c. Ricava la testata dal DB tramite CARTELLA_TESTATA
 *      d. Se testata univoca → sposta in _tmp, crea marker .OK, salva GDP_LOG, attiva F05
 *      e. Se testata ambigua → sposta PDF in _errata, salva GDP_LOG con MSG00002
 */
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

    private final SftpClientProducer  sftpProducer;
    private final GdpTestataRepository testataRepository;
    private final GdpLogRepository     logRepository;

    // TODO: iniettare F05Service quando disponibile
    // @Inject F05Service f05Service;

    public CheckEdizioneAttesaJob(SftpClientProducer sftpProducer,
                                  GdpTestataRepository testataRepository,
                                  GdpLogRepository logRepository) {
        this.sftpProducer      = sftpProducer;
        this.testataRepository = testataRepository;
        this.logRepository     = logRepository;
    }

    // =========================================================================
    // ENTRY POINT — schedulato ogni 15 minuti, esecuzione singola (SKIP se già in corso)
    // =========================================================================

    @Scheduled(every = "15m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void monitorAllFile() {
        Log.info("[F03] Avvio processo monitoraggio file");

        try (SftpSession session = sftpProducer.connect()) {
            ChannelSftp sftp = session.getChannel();

            // Legge le cartelle testata presenti in [flusso_regolare]
            ArrayList<ChannelSftp.LsEntry> listaTestate = leggiCartelle(sftp, sourceDir);

            // Se non trova nessuna cartella → MSG00001 e termina
            if (listaTestate.isEmpty()) {
                Log.info("[F03] MSG00001 - Nessuna nuova edizione trovata in: " + sourceDir);
                return;
            }

            // Itera su ogni cartella testata trovata
            for (ChannelSftp.LsEntry testata : listaTestate) {
                processaEdizioniPerTestata(sftp, testata.getFilename());
            }

            Log.info("[F03] MSG00009 - Processo completato");

        } catch (Exception e) {
            Log.errorf("[F03] Errore imprevisto nel processo: %s", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // =========================================================================
    // SCANSIONE EDIZIONI DI UNA TESTATA
    // =========================================================================

    /**
     * Entra nella cartella della testata e itera sulle sotto-cartelle edizione.
     * Per ogni edizione verifica se ci sono file da processare e ne controlla la stabilità.
     */
    private void processaEdizioniPerTestata(ChannelSftp sftp, String nomeTestata) {
        String pathTestata = sourceDir + "/" + nomeTestata;
        Log.infof("[F03] Analisi testata: %s", nomeTestata);

        ArrayList<ChannelSftp.LsEntry> listaEdizioni = leggiCartelle(sftp, pathTestata);
        Log.infof("[F03] Edizioni trovate per '%s': %d", nomeTestata, listaEdizioni.size());

        for (ChannelSftp.LsEntry edizione : listaEdizioni) {
            String nomeEdizione = edizione.getFilename();
            String pathEdizione = pathTestata + "/" + nomeEdizione;

            // Legge solo i file senza marker .OK (quelli da processare)
            ArrayList<ChannelSftp.LsEntry> fileDaProcessare = getFileSenzaMarker(sftp, pathEdizione);

            if (fileDaProcessare.isEmpty()) {
                // Tutti i file hanno già il .OK → edizione già acquisita completamente
                Log.infof("[F03] Edizione già processata, skip: %s", pathEdizione);
                continue;
            }

            Log.infof("[F03] Trovati %d file da processare in %s — avvio controllo stabilità",
                    fileDaProcessare.size(), pathEdizione);

            // Controlla che il trasferimento FTP sia completato (dimensione stabile per 3 check)
            if (!isCartellaStabile(sftp, pathEdizione)) {
                Log.warnf("[F03] Cartella ancora in trasferimento, skip: %s", pathEdizione);
                continue;
            }

            Log.infof("[F03] Cartella stabile, avvio acquisizione: %s", pathEdizione);

            // Calcola DT_ACQUISIZIONE = timestamp del file più recente
            String dtAcquisizione = recuperoDataAcquisizione(fileDaProcessare);
            Log.infof("[F03] DT_ACQUISIZIONE calcolata: %s", dtAcquisizione);

            // Ricava la testata dal DB e smista il flusso (corretto/errato)
            ricavaIdentificativoEFaiOperazioni(sftp, nomeTestata, dtAcquisizione, pathEdizione, fileDaProcessare);
        }
    }

    // =========================================================================
    // RICAVA TESTATA DAL DB E SMISTA IL FLUSSO
    // =========================================================================

    /**
     * Cerca la testata nel DB tramite CARTELLA_TESTATA = nomeCartellaTestata.
     *   - 1 risultato  → flusso corretto  → processaEdizioneCorretta
     *   - >1 risultati → anomalia unicità → processaEdizioneErrata
     *   - 0 risultati  → errore config    → log e skip
     */
    @Transactional
    void ricavaIdentificativoEFaiOperazioni(
            ChannelSftp sftp,
            String      nomeCartellaTestata,
            String      dataAcquisizione,
            String      pathEdizione,
            ArrayList<ChannelSftp.LsEntry> fileDaProcessare) {

        List<GdpTestata> testate = testataRepository.findByCartella(nomeCartellaTestata);

        if (testate.size() == 1) {
            // Caso OK: testata univoca trovata
            processaEdizioneCorretta(sftp, testate.get(0), dataAcquisizione, pathEdizione, fileDaProcessare);

        } else if (testate.size() > 1) {
            // Caso KO: più testate con la stessa cartella → anomalia unicità
            String idTestate = testate.stream()
                    .map(t -> String.valueOf(t.id))
                    .collect(Collectors.joining(", "));
            Log.errorf("[F03] MSG00002 - Anomalia unicità testata per cartella '%s'. ID trovati: [%s]",
                    nomeCartellaTestata, idTestate);
            processaEdizioneErrata(sftp, nomeCartellaTestata, dataAcquisizione, pathEdizione, fileDaProcessare);

        } else {
            // Caso KO: nessuna testata trovata → errore configurazione
            Log.errorf("[F03] Nessuna testata trovata per cartella: '%s' — verifica configurazione DB",
                    nomeCartellaTestata);
        }
    }

    // =========================================================================
    // FLUSSO CORRETTO — sposta in _tmp, crea marker .OK, salva log, attiva F05
    // =========================================================================

    /**
     * Elaborazione edizione con testata univoca:
     *   1. Crea cartella destinazione in _tmp (se non esiste)
     *   2. Per ogni file senza .OK: sposta in _tmp e crea marker .OK al suo posto
     *   3. Conta i file totali in _tmp
     *   4. Salva record GDP_LOG
     *   5. Attiva F05 in modalità asincrona
     */
    private void processaEdizioneCorretta(
            ChannelSftp sftp,
            GdpTestata  testata,
            String      dataAcquisizione,
            String      pathEdizione,
            ArrayList<ChannelSftp.LsEntry> fileDaProcessare) {

        try {
            String nomeEdizione = estraiNomeCartella(pathEdizione);

            // Percorso destinazione: _tmp/<cartellaTestata>/<nomeEdizione>
            String destPath = tmpDir + "/" + testata.cartellaTestata + "/" + nomeEdizione;
            mkdirRecursive(sftp, destPath);

            // Sposta ogni file in _tmp e lascia il marker .OK al suo posto
            for (ChannelSftp.LsEntry file : fileDaProcessare) {
                String srcFile  = pathEdizione + "/" + file.getFilename();
                String destFile = destPath     + "/" + file.getFilename();

                // Sposta il file in _tmp
                sftp.rename(srcFile, destFile);

                // Lascia "pag1.pdf.OK" al posto di "pag1.pdf" in flusso_regolare
                sftp.put(new ByteArrayInputStream(new byte[0]), srcFile + ".OK");

                Log.infof("[F03] Spostato: %s → %s", srcFile, destFile);
            }

            // Conta i file presenti in _tmp dopo lo spostamento (esclusi i .OK)
            int totaleFileTmp = getFileSenzaMarker(sftp, destPath).size();

            // Salva GDP_LOG con i dati dell'acquisizione
            salvaLog(testata.id, "G", dataAcquisizione, totaleFileTmp, null);

            Log.infof("[F03] Edizione '%s' spostata in TMP. File acquisiti: %d", nomeEdizione, totaleFileTmp);

            // TODO: attivare F05 in modalità asincrona
            // Parametri: idTestata, nomeEdizione, destPath, dataAcquisizione
            // f05Service.elaboraEdizione(testata.id, nomeEdizione, destPath, dataAcquisizione);
            Log.infof("[F03] TODO: attivare F05 per testata=%d, edizione=%s, path=%s",
                    testata.id, nomeEdizione, destPath);

        } catch (SftpException e) {
            Log.errorf("[F03] Errore spostamento in TMP per edizione '%s': %s",
                    estraiNomeCartella(pathEdizione), e.getMessage());
        }
    }

    // =========================================================================
    // FLUSSO ERRATO — sposta PDF in _errata, salva log con MSG00002
    // =========================================================================

    /**
     * Elaborazione edizione con anomalia unicità testata:
     *   1. Crea cartella destinazione in _errata (se non esiste)
     *   2. Sposta solo i file PDF (non i marker .OK) in _errata
     *   3. Salva record GDP_LOG con FK_TESTATA=0 e ESITO=MSG00002
     *   Nessun record GDP_LOG_EDIZIONE — nessuna attivazione F05
     */
    private void processaEdizioneErrata(
            ChannelSftp sftp,
            String      nomeCartellaTestata,
            String      dataAcquisizione,
            String      pathEdizione,
            ArrayList<ChannelSftp.LsEntry> fileDaProcessare) {

        try {
            String nomeEdizione = estraiNomeCartella(pathEdizione);

            // Percorso destinazione: _errata/<cartellaTestata>/<nomeEdizione>
            String destPath = errataDir + "/" + nomeCartellaTestata + "/" + nomeEdizione;
            mkdirRecursive(sftp, destPath);

            // Sposta solo i PDF (la spec esclude altri tipi di file)
            List<String> pdfSpostati = new ArrayList<>();
            for (ChannelSftp.LsEntry file : fileDaProcessare) {
                if (file.getFilename().toLowerCase().endsWith(".pdf")) {
                    String srcFile  = pathEdizione + "/" + file.getFilename();
                    String destFile = destPath     + "/" + file.getFilename();
                    sftp.rename(srcFile, destFile);
                    pdfSpostati.add(file.getFilename());
                    Log.infof("[F03] PDF spostato in errata: %s", file.getFilename());
                }
            }

            // Salva GDP_LOG con FK_TESTATA=0 e ESITO=MSG00002
            salvaLog(0, "G", dataAcquisizione, 0, "MSG00002");

            Log.errorf("[F03] MSG00002 - PDF spostati in errata [%s/%s]: %s",
                    nomeCartellaTestata, nomeEdizione, pdfSpostati);

        } catch (SftpException e) {
            Log.errorf("[F03] Errore spostamento in ERRATA per edizione '%s': %s",
                    estraiNomeCartella(pathEdizione), e.getMessage());
        }
    }

    // =========================================================================
    // PERSISTENZA — salva record GDP_LOG
    // =========================================================================

    /**
     * Inserisce un record in GDP_LOG.
     * Chiamato sia per il flusso corretto (esito null, valorizzato da F05)
     * sia per il flusso errato (esito MSG00002, FK_TESTATA=0).
     */
    @Transactional
    void salvaLog(Integer testataId, String tipo, String dtAcq, int count, String esito) {
        GdpLog log = new GdpLog();
        log.fkGdpTestata        = testataId;
        log.tipoAcquisizione    = tipo;
        log.dataAcquisizione    = converteDataAcquisizione(dtAcq);
        log.totaleFileAcquisiti = count;
        log.esito               = esito;
        logRepository.persist(log);
        Log.infof("[F03] GDP_LOG salvato — testata=%d, tipo=%s, file=%d, esito=%s",
                testataId, tipo, count, esito);
    }

    // =========================================================================
    // SFTP HELPERS
    // =========================================================================

    /**
     * Legge le sotto-cartelle di un path SFTP (esclude file e directory di sistema . e ..).
     * Logga MSG00001 se il path è vuoto o inesistente.
     */
    private ArrayList<ChannelSftp.LsEntry> leggiCartelle(ChannelSftp sftp, String path) {
        ArrayList<ChannelSftp.LsEntry> list = new ArrayList<>();
        try {
            Log.infof("[F03] Scansione cartelle in: %s", path);
            ArrayList<ChannelSftp.LsEntry> entries = new ArrayList<>(sftp.ls(path));
            entries.removeIf(e -> !e.getAttrs().isDir() || isSystemDir(e));
            list.addAll(entries);

            if (list.isEmpty()) {
                Log.errorf("[F03] MSG00001 - Nessuna cartella trovata in: %s", path);
            } else {
                Log.infof("[F03] Trovate %d cartelle in: %s", list.size(), path);
            }
        } catch (SftpException e) {
            Log.errorf("[F03] MSG00001 - Errore SFTP o percorso inesistente: %s", path);
        }
        return list;
    }

    /**
     * Restituisce i file presenti nel path che NON hanno ancora il corrispondente marker .OK.
     * Un file "pag1.pdf" è considerato già processato se esiste "pag1.pdf.OK" nella stessa cartella.
     * Esclude directory, file di sistema e file .OK.
     */
    private ArrayList<ChannelSftp.LsEntry> getFileSenzaMarker(ChannelSftp sftp, String path) {
        ArrayList<ChannelSftp.LsEntry> list = new ArrayList<>();
        try {
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(path);

            // Raccoglie tutti i marker .OK presenti nella cartella
            List<String> markers = entries.stream()
                    .map(ChannelSftp.LsEntry::getFilename)
                    .filter(n -> n.toLowerCase().endsWith(".ok"))
                    .toList();

            for (ChannelSftp.LsEntry entry : entries) {
                if (entry.getAttrs().isDir() || isSystemDir(entry)) continue;
                if (entry.getFilename().toLowerCase().endsWith(".ok")) continue;

                // Include il file solo se non esiste ancora il suo marker .OK
                if (!markers.contains(entry.getFilename() + ".OK")) {
                    list.add(entry);
                }
            }
        } catch (SftpException e) {
            Log.errorf("[F03] Errore lettura file in %s: %s", path, e.getMessage());
        }
        return list;
    }

    /**
     * Controlla se il trasferimento FTP è completato verificando che la dimensione
     * totale della cartella (esclusi i marker .OK) rimanga invariata per 3 check
     * consecutivi ogni 15 secondi.
     * Timeout configurabile tramite sftp.file.stable.seconds (default 180s = 3 minuti).
     */
    private boolean isCartellaStabile(ChannelSftp sftp, String pathEdizione) {
        try {
            long dimensionePrecedente = -1;
            int  tentativiStabili    = 0;
            int  tentativiTotali     = 0;
            int  maxTentativi        = stableSeconds / 15;

            while (tentativiTotali < maxTentativi) {
                long dimensioneAttuale = getDimensioneTotaleCartella(sftp, pathEdizione);

                if (dimensioneAttuale == dimensionePrecedente) {
                    tentativiStabili++;
                    Log.infof("[F03] [%s] stabile %d/3 (size=%d)",
                            pathEdizione, tentativiStabili, dimensioneAttuale);

                    if (tentativiStabili >= 3) {
                        Log.infof("[F03] Cartella STABILE: %s", pathEdizione);
                        return true;
                    }
                } else {
                    tentativiStabili = 0;
                    Log.infof("[F03] [%s] ancora in trasferimento (size=%d)",
                            pathEdizione, dimensioneAttuale);
                }

                dimensionePrecedente = dimensioneAttuale;
                tentativiTotali++;
                Thread.sleep(15_000);
            }

            Log.warnf("[F03] Timeout stabilità per: %s", pathEdizione);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.errorf("[F03] Thread interrotto durante controllo stabilità: %s", e.getMessage());
        } catch (Exception e) {
            Log.errorf("[F03] Errore controllo stabilità: %s", e.getMessage());
        }

        return false;
    }

    /**
     * Somma la dimensione di tutti i file nella cartella escludendo i marker .OK.
     * Usato da isCartellaStabile per verificare che il trasferimento sia completato.
     */
    private long getDimensioneTotaleCartella(ChannelSftp sftp, String path) throws SftpException {
        long totale = 0;
        for (ChannelSftp.LsEntry file : sftp.ls(path)) {
            if (!file.getAttrs().isDir()
                    && !file.getFilename().toLowerCase().endsWith(".ok")) {
                totale += file.getAttrs().getSize();
            }
        }
        return totale;
    }

    /**
     * Calcola DT_ACQUISIZIONE come timestamp del file più recente (per mTime).
     * Formato: yyyy-MM-dd HH:mm:ss come da spec.
     */
    private String recuperoDataAcquisizione(ArrayList<ChannelSftp.LsEntry> files) {
        if (files == null || files.isEmpty()) {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        ChannelSftp.LsEntry ultimoFile = files.stream()
                .max(Comparator.comparingInt(e -> e.getAttrs().getMTime()))
                .orElse(files.get(0));

        long secondi = ultimoFile.getAttrs().getMTime();
        LocalDateTime dataUltimoFile = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(secondi), ZoneId.systemDefault());

        return dataUltimoFile.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Crea ricorsivamente le directory sul server SFTP se non esistono già.
     * Necessario perché sftp.mkdir() crea solo un livello alla volta.
     */
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
                    Log.infof("[F03] Cartella creata: %s", corrente);
                } catch (SftpException ex) {
                    Log.warnf("[F03] mkdir fallito per %s: %s", corrente, ex.getMessage());
                }
            }
        }
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    /** Converte la stringa data acquisizione in LocalDateTime. Fallback su now() se null o malformata. */
    private LocalDateTime converteDataAcquisizione(String dataAcquisizione) {
        if (dataAcquisizione == null || dataAcquisizione.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(dataAcquisizione,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            Log.errorf("[F03] Formato data non valido '%s', uso now()", dataAcquisizione);
            return LocalDateTime.now();
        }
    }

    /** Estrae il nome dell'ultima cartella da un path. Es: "/a/b/c" → "c" */
    private String estraiNomeCartella(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    /** Restituisce true se la entry è una directory di sistema (. o ..) */
    private boolean isSystemDir(ChannelSftp.LsEntry e) {
        return e.getFilename().equals(".") || e.getFilename().equals("..");
    }
}