package it.csipiemonte.gdp.gdporch.service.impl;

import it.csipiemonte.gdp.gdporch.dto.AcquisizioneList;
import it.csipiemonte.gdp.gdporch.dto.AcquisizioneSummary;
import it.csipiemonte.gdp.gdporch.exception.GdpException;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import it.csipiemonte.gdp.gdporch.model.entity.GdpEdizione;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLog;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLogEdizione;
import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import it.csipiemonte.gdp.gdporch.model.enums.TipoAcquisizione;
import it.csipiemonte.gdp.gdporch.model.repository.GdpEdizioneRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogEdizioneRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import it.csipiemonte.gdp.gdporch.service.GdpMonitorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GdpMonitorServiceImpl implements GdpMonitorService {

    @Inject
    GdpLogRepository logRepository;

    @Inject
    GdpTestataRepository testataRepository;

    @Inject
    GdpEdizioneRepository edizioneRepository;

    @Inject
    GdpLogEdizioneRepository logEdizioneRepository;

    @Override
    public AcquisizioneList elencoAcquisizioni(String tipoAcquisizione, LocalDate dataAcquisizione) {
        TipoAcquisizione tipo;
        try {
            tipo = TipoAcquisizione.valueOf(tipoAcquisizione);
        } catch (IllegalArgumentException e) {
            throw new GdpException(GdpMessage.F12_MONITOR_ERROR);
        }

        List<GdpLog> logs = logRepository.findByTipoAcquisizioneAndDataAcquisizione(tipo, dataAcquisizione);
        if (logs == null || logs.isEmpty()) {
            return new AcquisizioneList().acquisizioni(new ArrayList<>());
        }

        List<AcquisizioneSummary> summaries = new ArrayList<>();
        for (GdpLog log : logs) {
            AcquisizioneSummary summary = new AcquisizioneSummary();
            summary.setIdLog(log.id);
            summary.setIdUtenteSFTP(log.fkGdpUtenteFtp);
            summary.setIdTestata(log.fkGdpTestata);
            summary.setDataAcquisizione(
                    Date.from(log.dataAcquisizione.atZone(java.time.ZoneId.systemDefault()).toInstant()));
            summary.setNroTotFile(log.totaleFileAcquisiti);
            summary.setEsito(log.esito);

            // Get testata name
            GdpTestata testata = testataRepository.findById(log.fkGdpTestata);
            if (testata != null) {
                summary.setNomeTestata(testata.nomeTestata);
            }

            // Get dataEdizione from first edition linked to this log
            List<GdpLogEdizione> logEdizioni = logEdizioneRepository.findByLog(log.id);
            if (!logEdizioni.isEmpty()) {
                Integer fkEdizione = logEdizioni.get(0).fkGdpEdizione;
                GdpEdizione edizione = edizioneRepository.findById(fkEdizione);
                if (edizione != null) {
                    summary.setDataEdizione(edizione.dataEdizione);
                }
            }

            // nroEdizioni
            if (TipoAcquisizione.G.equals(tipo)) {
                summary.setNroEdizioni(1);
            } else {
                summary.setNroEdizioni((int) logEdizioneRepository.countByLog(log.id));
            }

            summaries.add(summary);
        }

        return new AcquisizioneList().acquisizioni(summaries);
    }
}