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
import it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione;
import it.csipiemonte.gdp.gdporch.service.DamTrasmissioneService;
import it.csipiemonte.gdp.gdporch.service.GdpCtrlEdizioneAcquisitaService;
import it.csipiemonte.gdp.gdporch.service.GdpEdizioneService;
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
import java.util.stream.Stream;

@ApplicationScoped
public class GdpCtrlEdizioneAcquisitaServiceImpl implements GdpCtrlEdizioneAcquisitaService {

    private static final Logger LOG = Logger.getLogger(GdpCtrlEdizioneAcquisitaServiceImpl.class);

    private static final String DATE_REGEX = "((^|[\\s])(?<day>\\d{1,2})°?(?<sep>[\\s\\-\\.\\/])(?<month>\\d{1,2}|gen(naio)?|feb(braio)?|mar(zo)?|apr(ile)?|mag(gio)?|giu(gno)?|lug(lio)?|ago(sto)?|set(tembre)?|ott(obre)?|nov(embre)?|dic(embre)?)\\k<sep>(?<year>18\\d{2}|19\\d{2}|20\\d{2}))";

    private final String tmpPrefix;
    private final String errataPrefix;
    private final GdpPeriodicitaRepository periodicitaRepository;
    private final GdpDataUscitaRepository dataUscitaRepository;
    private final GdpLogRepository logRepository;
    private final GdpLogEdizioneRepository logEdizioneRepository;
    private final GdpEdizioneService edizioneService;
    private final DamTrasmissioneService damTrasmissioneService;

    @Inject
    public GdpCtrlEdizioneAcquisitaServiceImpl(
            @ConfigProperty(name = "sftp.root.prefix.tmp", defaultValue = "_tmp/") String tmpPrefix,
            @ConfigProperty(name = "sftp.root.prefix.errata", defaultValue = "_errata/") String errataPrefix,
            GdpPeriodicitaRepository periodicitaRepository,
            GdpDataUscitaRepository dataUscitaRepository,
            GdpLogRepository logRepository,
            GdpLogEdizioneRepository logEdizioneRepository,
            GdpEdizioneService edizioneService,
            DamTrasmissioneService damTrasmissioneService) {
        this.tmpPrefix = tmpPrefix;
        this.errataPrefix = errataPrefix;
        this.periodicitaRepository = periodicitaRepository;
        this.dataUscitaRepository = dataUscitaRepository;
        this.logRepository = logRepository;
        this.logEdizioneRepository = logEdizioneRepository;
        this.edizioneService = edizioneService;
        this.damTrasmissioneService = damTrasmissioneService;
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
        TipoEdizione tipoEdizione = classifyEditionType(dataEdizione, dataUscita);

        if (TipoEdizione.AA.equals(tipoEdizione)) {
            handleBlockingError(cartellaTestata, dataEdizione, idLog);
            throw new GdpException(GdpMessage.ANOMALIA_DATA_EDIZIONE);
        }

        // Step 2 - Per-PDF processing loop
        String safeCartella = sanitizePathComponent(cartellaTestata);
        Path editionPath = Paths.get(tmpPrefix, safeCartella, dataEdizione.toString());
        Path errorPath = Paths.get(errataPrefix, safeCartella, dataEdizione.toString());

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
                        errorPath, idLog));

        // Step 3 - Update Logs
        updateLogs(idLog, tipoEdizione, editionPath.toString(), validFiles.size(), invalidFiles.size(),
                descriptionErrors);

        // Step 4 - Invoke F08 (synchronous)
        var f08Response = edizioneService.insEdizione(idTestata, editionPath.toString(), dataEdizione, idLog);
        if (!GdpMessage.OK.getCodice().equals(f08Response.getCodice())) {
            handleProcessingError(editionPath, idLog, GdpMessage.ERRORE_IO.getCodice(), f08Response.getMessaggio());
            response.setCodice(f08Response.getCodice());
            response.setMessaggio(f08Response.getMessaggio());
            return response;
        }

        // Step 5 - Invoke F09 (synchronous)
        var f09Response = damTrasmissioneService.creaXMLEdizione(idTestata, idLog, f08Response.getIdEdizione(), 0);
        if (!GdpMessage.OK.getCodice().equals(f09Response.getCodice())) {
            handleProcessingError(editionPath, idLog, GdpMessage.ERRORE_CODA_DAM.getCodice(), f09Response.getMessaggio());
            response.setCodice(f09Response.getCodice());
            response.setMessaggio(f09Response.getMessaggio());
            return response;
        }

        // TODO: invoke F10 asynchronously - will be implemented in branch feat/f10
        // it.csipiemonte.gdp.gdporch.service.DamTrasmissioneService.inviaEdizioneAsync(idLog, f08Response.getIdEdizione());

        response.setCodice(GdpMessage.OK.getCodice());
        return response;
    }

    private TipoEdizione classifyEditionType(LocalDate dataEdizione, GdpDataUscita dataUscita) {
        LocalDate today = LocalDate.now();
        if (dataUscita == null)
            return TipoEdizione.AA;
        if (Boolean.TRUE.equals(dataUscita.sospesa))
            return TipoEdizione.SO;
        if (dataEdizione.equals(today))
            return TipoEdizione.OK;
        if (dataEdizione.isAfter(today))
            return TipoEdizione.AN;
        return TipoEdizione.PO;
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
                log.esito = GdpMessage.ERROR_GENERICO.getCodice();
            }
        } catch (IOException e) {
            LOG.error("Errore spostamento edizione AA", e);
        }
    }

    private boolean processPdfFile(File pdfFile, String cartellaTestata, LocalDate dataEdizione,
            List<String> errors, List<File> valid, List<File> invalid, Path errorPath, Integer idLog) {

        String originalName = pdfFile.getName();

        // 2a. Multi-page check
        try {
            List<PDDocument> pages = PdfUtils.splitIfMultiPage(pdfFile);
            if (pages != null) {
                errors.add("NP - " + originalName);
                int i = 1;
                for (PDDocument pageDoc : pages) {
                    try {
                        String partName = String.format("%s_%03d.part.pdf", originalName.replace(".pdf", ""), i++);
                        File partFile = new File(pdfFile.getParentFile(), partName);
                        pageDoc.save(partFile);
                        processSinglePdf(partFile, cartellaTestata, dataEdizione, errors, valid, invalid, errorPath, true, idLog);
                    } finally {
                        try { pageDoc.close(); } catch (Exception ignored) {}
                    }
                }
                Files.deleteIfExists(pdfFile.toPath());
                return true;
            }
        } catch (Exception e) {
            LOG.errorf("Errore split PDF %s: %s", originalName, e.getMessage());
        }

        return processSinglePdf(pdfFile, cartellaTestata, dataEdizione, errors, valid, invalid, errorPath, false, idLog);
    }

    private boolean processSinglePdf(File pdfFile, String cartellaTestata, LocalDate dataEdizione,
            List<String> errors, List<File> valid, List<File> invalid, Path errorPath, boolean wasSplit, Integer idLog) {

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
        if (!renamedFile.exists() && !pdfFile.renameTo(renamedFile)) {
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

            // 2e. Heuristic Checks (Page 1)
            if ("001".equals(pageNum)) {
                checkHeuristicDate(text, dataEdizione, originalName, errors);
                checkFirstPageKeywords(text, originalName, errors, idLog);
            }

        } catch (IOException e) {
            LOG.error("Errore estrazione testo", e);
        }

        valid.add(renamedFile);
        return true;
    }

    private void checkHeuristicDate(String text, LocalDate expectedDate, String originalName, List<String> errors) {
        Pattern p = Pattern.compile(DATE_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        boolean matched = false;

        while (m.find()) {
            String dayStr = m.group("day");
            String monthStr = m.group("month").toLowerCase();
            String yearStr = m.group("year");

            int day = Integer.parseInt(dayStr);
            int year = Integer.parseInt(yearStr);
            int month = parseMonth(monthStr);

            try {
                LocalDate foundDate = LocalDate.of(year, month, day);
                if (foundDate.equals(expectedDate)) {
                    matched = true;
                    break;
                }
            } catch (Exception e) {
                // Invalid date found, ignore
            }
        }

        if (!matched) {
            errors.add("DA - " + originalName);
        }
    }

    private void checkFirstPageKeywords(String text, String originalName, List<String> errors, Integer idLog) {
        String lowerText = text.toLowerCase();
        List<String> keywords = List.of("abbonamento", "direzione", "direttore", "redazione", "amministrazione");
        boolean found = keywords.stream().anyMatch(lowerText::contains);

        if (!found) {
            errors.add("PP - " + originalName);
        } else {
            // If keywords found, update log to indicate prima pagina found
            // This is a bit tricky since we are in a parallel stream.
            // But we can update the log later or use an atomic flag.
            // For now, the spec says "PRIMA_PAGINA = True/False" in GDP_LOG_EDIZIONE.
        }
    }

    private int parseMonth(String monthStr) {
        if (monthStr.matches("\\d{1,2}")) return Integer.parseInt(monthStr);
        if (monthStr.startsWith("gen")) return 1;
        if (monthStr.startsWith("feb")) return 2;
        if (monthStr.startsWith("mar")) return 3;
        if (monthStr.startsWith("apr")) return 4;
        if (monthStr.startsWith("mag")) return 5;
        if (monthStr.startsWith("giu")) return 6;
        if (monthStr.startsWith("lug")) return 7;
        if (monthStr.startsWith("ago")) return 8;
        if (monthStr.startsWith("set")) return 9;
        if (monthStr.startsWith("ott")) return 10;
        if (monthStr.startsWith("nov")) return 11;
        if (monthStr.startsWith("dic")) return 12;
        return 0;
    }

    private void moveFile(File file, Path destDir) {
        try {
            Files.move(file.toPath(), destDir.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.errorf("Errore spostamento file %s", file.getName(), e);
        }
    }

    private String sanitizePathComponent(String part) {
        if (part == null) return "unknown";
        // Prevent path traversal and illegal characters
        return part.replaceAll("[\\\\/:*?\"<>|]", "_").replace("..", "_");
    }

    private void handleProcessingError(Path src, Integer idLog, String code, String message) {
        // Find folder index in the prefix to replicate it in errata
        String relativeSource = src.toString();
        if (relativeSource.startsWith(tmpPrefix)) {
            relativeSource = relativeSource.substring(tmpPrefix.length());
        }
        Path dest = Paths.get(errataPrefix, relativeSource);
        
        try {
            if (Files.exists(src)) {
                if (!Files.exists(dest.getParent())) {
                    Files.createDirectories(dest.getParent());
                }
                Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            GdpLog log = logRepository.findById(idLog);
            if (log != null) {
                log.esito = code;
            }
        } catch (IOException e) {
            LOG.error("Errore spostamento edizione fallita", e);
        }
    }

    private void updateLogs(Integer idLog, TipoEdizione tipoEdizione, String path, int ok, int ko, List<String> errorCodes) {
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
        le.fileXml = false;
        le.fileZip = false;
        le.primaPagina = errorCodes.stream().noneMatch(e -> e.startsWith("PP"));

        logEdizioneRepository.persist(le);

        if (log != null && !errorCodes.isEmpty()) {
            if (log.esito != null) {
                log.esito = log.esito.replace("MSG", "WRN");
            }
        }
    }
}
