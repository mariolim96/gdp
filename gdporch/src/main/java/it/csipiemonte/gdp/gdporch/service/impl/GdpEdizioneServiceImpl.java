package it.csipiemonte.gdp.gdporch.service.impl;

import it.csipiemonte.gdp.gdporch.dto.EdizioneInsertResponse;
import it.csipiemonte.gdp.gdporch.model.entity.GdpEdizione;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLogEdizione;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPagina;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import it.csipiemonte.gdp.gdporch.model.repository.GdpEdizioneRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogEdizioneRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpPaginaRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpPeriodicitaRepository;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import it.csipiemonte.gdp.gdporch.service.GdpEdizioneService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ApplicationScoped
public class GdpEdizioneServiceImpl implements GdpEdizioneService {

    private static final Logger LOG = Logger.getLogger(GdpEdizioneServiceImpl.class);

    private final GdpEdizioneRepository edizioneRepository;
    private final GdpPaginaRepository paginaRepository;
    private final GdpPeriodicitaRepository periodicitaRepository;
    private final GdpLogEdizioneRepository logEdizioneRepository;

    @Inject
    public GdpEdizioneServiceImpl(GdpEdizioneRepository edizioneRepository, GdpPaginaRepository paginaRepository,
            GdpPeriodicitaRepository periodicitaRepository, GdpLogEdizioneRepository logEdizioneRepository) {
        this.edizioneRepository = edizioneRepository;
        this.paginaRepository = paginaRepository;
        this.periodicitaRepository = periodicitaRepository;
        this.logEdizioneRepository = logEdizioneRepository;
    }

    @Override
    @Transactional
    public EdizioneInsertResponse insEdizione(Integer idTestata, String path, LocalDate dataEdizione, Integer idLog) {
        LOG.infof("Avvio F08 - DB.insEdizione per testata %d, data %s, path %s", idTestata, dataEdizione, path);
        
        EdizioneInsertResponse response = new EdizioneInsertResponse();
        
        try {
            // Count PDFs in path
            Path editionPath = Paths.get(path);
            long nroPDF;
            List<Path> pdfFiles;
            try (Stream<Path> stream = Files.list(editionPath)) {
                pdfFiles = stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                        .toList();
                nroPDF = pdfFiles.size();
            }

            // Check for existing edition
            Optional<GdpEdizione> existingOpt = edizioneRepository.findByTestataAndData(idTestata, dataEdizione);
            GdpEdizione edizione;
            
            if (existingOpt.isPresent()) {
                edizione = existingOpt.get();
                LOG.infof("Aggiornamento edizione esistente ID: %d", edizione.id);
                if (edizione.totalePagine != (int) nroPDF) {
                    edizione.totalePagine = (int) nroPDF;
                }
            } else {
                edizione = new GdpEdizione();
                edizione.fkGdpTestata = idTestata;
                edizione.dataEdizione = dataEdizione;
                edizione.dataPubblicazione = calculateDataPubblicazione(idTestata, dataEdizione);
                edizione.stato = 0;
                edizione.totalePagine = (int) nroPDF;
                edizioneRepository.persist(edizione);
                LOG.infof("Creata nuova edizione ID: %d", edizione.id);
            }

            // Insert/Update Pages
            Pattern p = Pattern.compile(".*_(\\d{3})\\.pdf");
            for (Path pdf : pdfFiles) {
                String fileName = pdf.getFileName().toString();
                Matcher m = p.matcher(fileName);
                if (m.matches()) {
                    Integer numPagina = Integer.parseInt(m.group(1));
                    Optional<GdpPagina> pagOpt = paginaRepository.find("fkGdpEdizione = ?1 and numPagina = ?2", edizione.id, numPagina).firstResultOptional();
                    GdpPagina pagina = pagOpt.orElse(new GdpPagina());
                    
                    pagina.fkGdpTestata = idTestata;
                    pagina.fkGdpEdizione = edizione.id;
                    pagina.numPagina = numPagina;
                    pagina.filePdf = fileName;
                    pagina.fileTxt = fileName.replace(".pdf", ".txt");
                    pagina.annoEdizione = dataEdizione.getYear();
                    pagina.stato = 0;
                    
                    if (!pagOpt.isPresent()) {
                        paginaRepository.persist(pagina);
                    }
                }
            }

            // Update GDP_LOG_EDIZIONE
            Optional<GdpLogEdizione> logEdOpt = logEdizioneRepository.find("fkGdpLog = ?1", idLog).firstResultOptional();
            if (logEdOpt.isPresent()) {
                GdpLogEdizione logEd = logEdOpt.get();
                logEd.fkGdpEdizione = edizione.id;
            }

            response.setCodice(GdpMessage.OK.getCodice());
            response.setIdEdizione(edizione.id);
            response.setMessaggio(GdpMessage.OK.getDescrizioneDefault());
            
        } catch (Exception e) {
            LOG.error("Errore durante insEdizione", e);
            GdpMessage error = e.getMessage() != null && e.getMessage().contains("GDP_PAGINA") ? GdpMessage.ERRORE_PAGINA : GdpMessage.ERRORE_EDIZIONE;
            response.setCodice(error.getCodice());
            response.setMessaggio(error.getDescrizioneDefault() + ": " + e.getMessage());
        }

        return response;
    }

    private LocalDate calculateDataPubblicazione(Integer idTestata, LocalDate dataEdizione) {
        GdpPeriodicita periodicita = periodicitaRepository.find("fkGdpTestata", idTestata).firstResult();
        int daysToAdd = 1; // Default for daily

        if (periodicita != null) {
            if (periodicita.mensilita > 0) {
                // Monthly or multi-monthly logic
                String ggP = periodicita.ggPeriodicita != null ? periodicita.ggPeriodicita : "";
                if (periodicita.mensilita == 1 && ggP.contains(";")) {
                    daysToAdd = 15; // quindicinale (twice-monthly)
                } else {
                    daysToAdd = periodicita.mensilita * 30;
                }
            } else if (periodicita.ggPeriodicita != null) {
                String ggP = periodicita.ggPeriodicita;
                if (ggP.contains("S0")) {
                    daysToAdd = 1; // daily
                } else if (ggP.contains("2W")) {
                    daysToAdd = 14; // quattordicinale
                } else {
                    // Weekly frequency
                    long count = ggP.chars().filter(ch -> ch == ';').count() + 1;
                    if (count == 1) daysToAdd = 7;
                    else if (count == 2) daysToAdd = 4; // bisettimanale (3.5 ceil)
                    else if (count == 3) daysToAdd = 3; // trisettimanale (2.34 ceil)
                    else if (count == 4) daysToAdd = 2; // quadrisettimanale (1.75 ceil)
                }
            }
        }

        return dataEdizione.plusDays(daysToAdd);
    }
}
