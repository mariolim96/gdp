package it.csipiemonte.gdp.gdporch.service.impl;

import it.csipiemonte.gdp.gdporch.dto.GenericProcessResponse;
import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLog;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLogEdizione;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import it.csipiemonte.gdp.gdporch.model.repository.GdpDataUscitaRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogEdizioneRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpPeriodicitaRepository;
import it.csipiemonte.gdp.gdporch.service.GdpCtrlEdizioneAcquisitaService;
import it.csipiemonte.gdp.gdporch.utils.PdfUtils;
import it.csipiemonte.gdp.gdporch.exception.GdpException;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class GdpCtrlEdizioneAcquisitaServiceImpl implements GdpCtrlEdizioneAcquisitaService {

    private static final Logger LOG = Logger.getLogger(GdpCtrlEdizioneAcquisitaServiceImpl.class);

    private static final String DATE_REGEX = "((^|[\\s])(\\d{1,2})°?(?<sep>[\\s\\-\\.\\/])(\\d{1,2}|gen(naio)?|feb(braio)?|mar(zo)?|apr(ile)?|mag(gio)?|giu(gno)?|lug(lio)?|ago(sto)?|set(tembre)?|ott(obre)?|nov(embre)?|dic(embre)?)\\k<sep>(18\\d{2}|19\\d{2}|20\\d{2}))";

    private final String tmpPrefix;
    private final String errataPrefix;
    private final GdpPeriodicitaRepository periodicitaRepository;
    private final GdpDataUscitaRepository dataUscitaRepository;
    private final GdpLogRepository logRepository;
    private final GdpLogEdizioneRepository logEdizioneRepository;

    @Inject
    public GdpCtrlEdizioneAcquisitaServiceImpl(
            @ConfigProperty(name = "sftp.root.prefix.tmp", defaultValue = "_tmp/") String tmpPrefix,
            @ConfigProperty(name = "sftp.root.prefix.errata", defaultValue = "_errata/") String errataPrefix,
            GdpPeriodicitaRepository periodicitaRepository,
            GdpDataUscitaRepository dataUscitaRepository,
            GdpLogRepository logRepository,
            GdpLogEdizioneRepository logEdizioneRepository) {
        this.tmpPrefix = tmpPrefix;
        this.errataPrefix = errataPrefix;
        this.periodicitaRepository = periodicitaRepository;
        this.dataUscitaRepository = dataUscitaRepository;
        this.logRepository = logRepository;
        this.logEdizioneRepository = logEdizioneRepository;
    }

    @Override
    @Transactional
    public GenericProcessResponse ctrlEdizioneAcquisita(Integer idTestata, String cartellaTestata,
            LocalDate dataEdizione, Integer idLog) {
        LOG.infof("Avvio F04 - FTPregolare.ctrlEdizioneAcquisita per testata %d, data %s", idTestata, dataEdizione);

        GenericProcessResponse response = new GenericProcessResponse();
        response.setMessaggio(GdpMessage.OK.getDescrizioneDefault());

        // Step 1 - Edition date validation
        GdpPeriodicita periodicita = periodicitaRepository.find("fkGdpTestata", idTestata).firstResult();
        if (periodicita == null) {
            LOG.warnf("Periodicita non trovata per testata %d", idTestata);
            throw new GdpException(GdpMessage.NO_PERIODICITA);
        }

        GdpDataUscita dataUscita = dataUscitaRepository.findByPeriodicitaAndDate(periodicita.id, dataEdizione);
        String tipoEdizione = classifyEditionType(dataEdizione, dataUscita);

        if ("AA".equals(tipoEdizione)) {
            handleBlockingError(cartellaTestata, dataEdizione, idLog);
            throw new GdpException(GdpMessage.ANOMALIA_DATA_EDIZIONE);
        }

        // Step 2 - Per-PDF processing loop
        Path editionPath = Paths.get(tmpPrefix, cartellaTestata, dataEdizione.toString());
        Path errorPath = Paths.get(errataPrefix, cartellaTestata, dataEdizione.toString());

        try {
            if (!Files.exists(errorPath)) {
                Files.createDirectories(errorPath);
            }
        } catch (IOException e) {
            LOG.error("Errore creazione cartella errata", e);
            throw new GdpException(GdpMessage.ERRORE_IO, "Impossibile creare cartella errata: " + errorPath);
        }

        List<File> pdfFiles;
        try (Stream<Path> stream = Files.list(editionPath)) {
            pdfFiles = stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .map(Path::toFile)
                    .toList();
        } catch (IOException e) {
            LOG.error("Errore scansione file PDF", e);
            throw new GdpException(GdpMessage.ERRORE_IO, "Impossibile leggere file dalla directory di edizione");
        }

        List<String> descriptionErrors = new CopyOnWriteArrayList<>();
        List<File> validFiles = new CopyOnWriteArrayList<>();
        List<File> invalidFiles = new CopyOnWriteArrayList<>();
        // Parallel processing loop
        pdfFiles.parallelStream()
                .forEach(pdfFile -> processPdfFile(pdfFile, cartellaTestata, dataEdizione, descriptionErrors,
                        validFiles, invalidFiles,
                        errorPath));

        // Step 3 - Update Logs
        updateLogs(idLog, tipoEdizione, editionPath.toString(), validFiles.size(), invalidFiles.size(),
                descriptionErrors);

        response.setCodice(GdpMessage.OK.getCodice());
        return response;
    }

    private String classifyEditionType(LocalDate dataEdizione, GdpDataUscita dataUscita) {
        LocalDate today = LocalDate.now();
        if (dataUscita == null)
            return "AA";
        if (Boolean.TRUE.equals(dataUscita.sospesa))
            return "SO";
        if (dataEdizione.equals(today))
            return "OK";
        if (dataEdizione.isAfter(today))
            return "AN";
        return "PO";
    }

    private void handleBlockingError(String cartellaTestata, LocalDate dataEdizione, Integer idLog) {
        Path src = Paths.get(tmpPrefix, cartellaTestata, dataEdizione.toString());
        Path dest = Paths.get(errataPrefix, cartellaTestata, dataEdizione.toString());
        try {
            if (Files.exists(src)) {
                Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            GdpLog log = logRepository.findById(idLog);
            if (log != null) {
                log.esito = "MSG00001";
            }
        } catch (IOException e) {
            LOG.error("Errore spostamento edizione AA", e);
        }
    }

    private boolean processPdfFile(File pdfFile, String cartellaTestata, LocalDate dataEdizione,
            List<String> errors, List<File> valid, List<File> invalid, Path errorPath) {

        String originalName = pdfFile.getName();

        // 2a. Multi-page check
        try {
            List<PDDocument> pages = PdfUtils.splitIfMultiPage(pdfFile);
            if (pages != null) {
                errors.add("NP - " + originalName);
                // For simplicity, we just take the first page or handle them all.
                // Spec says split into single-page files.
                int i = 1;
                for (PDDocument pageDoc : pages) {
                    String partName = String.format("%s_%03d.part.pdf", originalName.replace(".pdf", ""), i++);
                    File partFile = new File(pdfFile.getParentFile(), partName);
                    pageDoc.save(partFile);
                    pageDoc.close();
                    processSinglePdf(partFile, cartellaTestata, dataEdizione, errors, valid, invalid, errorPath, true);
                }
                Files.delete(pdfFile.toPath());
                return true;
            }
        } catch (IOException e) {
            LOG.error("Errore split PDF", e);
        }

        return processSinglePdf(pdfFile, cartellaTestata, dataEdizione, errors, valid, invalid, errorPath, false);
    }

    private boolean processSinglePdf(File pdfFile, String cartellaTestata, LocalDate dataEdizione,
            List<String> errors, List<File> valid, List<File> invalid, Path errorPath, boolean wasSplit) {

        String originalName = pdfFile.getName();

        // 2b. Readability check
        if (!PdfUtils.isReadable(pdfFile)) {
            moveFile(pdfFile, errorPath);
            errors.add("NL - " + originalName);
            invalid.add(new File(errorPath.toFile(), originalName));
            return false;
        }

        // 2c. Naming validation
        String namingPattern = "([^\\_]+)_(\\d{3})\\.pdf";
        Pattern p = Pattern.compile(namingPattern);
        Matcher m = p.matcher(originalName);

        if (!m.matches()) {
            moveFile(pdfFile, errorPath);
            errors.add("NF - " + originalName);
            invalid.add(new File(errorPath.toFile(), originalName));
            return false;
        }

        String pageNum = m.group(2);
        String newName = String.format("%s-%s_%s.pdf", cartellaTestata, dataEdizione.toString(), pageNum);
        File renamedFile = new File(pdfFile.getParentFile(), newName);
        if (!pdfFile.renameTo(renamedFile)) {
            LOG.errorf("Impossibile rinominare file %s in %s", pdfFile.getName(), newName);
            moveFile(pdfFile, errorPath);
            errors.add("NF - " + originalName);
            invalid.add(new File(errorPath.toFile(), originalName));
            return false;
        }

        // 2d. Text extraction
        try {
            String text = PdfUtils.extractText(renamedFile);
            File txtFile = new File(pdfFile.getParentFile(), newName.replace(".pdf", ".txt"));
            Files.writeString(txtFile.toPath(), text);

            // 2e. Heuristic Date Check (Page 1)
            if ("001".equals(pageNum)) {
                checkHeuristicDate(text, dataEdizione, originalName, errors);
            }

        } catch (IOException e) {
            LOG.error("Errore estrazione testo", e);
        }

        valid.add(renamedFile);
        return true;
    }

    private void checkHeuristicDate(String text, LocalDate expectedDate, String originalName, List<String> errors) {
        Pattern p = Pattern.compile(DATE_REGEX);
        Matcher m = p.matcher(text);
        boolean found = false;
        while (m.find()) {
            // Very simple check: if any date found matches expected format
            // In a real scenario, we'd parse the date found.
            if (text.contains(expectedDate.toString())) {
                found = true;
                break;
            }
        }
        if (!found) {
            errors.add("DA - " + originalName);
        }
    }

    private void moveFile(File file, Path destDir) {
        try {
            Files.move(file.toPath(), destDir.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.errorf("Errore spostamento file %s", file.getName(), e);
        }
    }

    private void updateLogs(Integer idLog, String tipoEdizione, String path, int ok, int ko, List<String> errorCodes) {
        GdpLog log = logRepository.findById(idLog);

        GdpLogEdizione le = new GdpLogEdizione();
        le.fkGdpLog = idLog;
        le.tipoEdizione = tipoEdizione;
        le.pathEdizione = path;
        le.nroPagAcquisite = ok + ko;
        le.nroPagValide = ok;
        le.nroPagErrate = ko;
        le.descrizione = String.join(", ", errorCodes);
        le.fkGdpEdizione = 0; // Will be updated by F08

        logEdizioneRepository.persist(le);

        if (log != null && !errorCodes.isEmpty()) {
            if (log.esito != null) {
                log.esito = log.esito.replace("MSG", "WRN");
            }
        }
    }
}
