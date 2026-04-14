package it.csipiemonte.gdp.gdporch.service.impl;

import it.csipiemonte.gdp.gdporch.dto.AcquisizioneList;
import it.csipiemonte.gdp.gdporch.dto.AcquisizioneDetail;
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
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class GdpMonitorServiceImpl implements GdpMonitorService {

    private static final Logger LOG = Logger.getLogger(GdpMonitorServiceImpl.class);

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
            tipo = TipoAcquisizione.valueOf(tipoAcquisizione.toUpperCase());
        } catch (Exception e) {
            throw new GdpException(GdpMessage.F12_MONITOR_ERROR);
        }

        List<GdpLog> logs = logRepository.findByTipoAcquisizioneAndDataAcquisizione(tipo, dataAcquisizione);
        if (logs == null || logs.isEmpty()) {
            throw new GdpException(GdpMessage.F12_MONITOR_ERROR);
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

            // Get dataEdizione and tipoEdizione from first edition linked to this log
            List<GdpLogEdizione> logEdizioni = logEdizioneRepository.findByLog(log.id);
            if (!logEdizioni.isEmpty()) {
                GdpLogEdizione logEdizione = logEdizioni.get(0);

                // Human-readable tipoEdizione
                if (logEdizione.tipoEdizione != null) {
                    summary.setTipoEdizione(logEdizione.tipoEdizione.getDescrizione());
                }

                Integer fkEdizione = logEdizione.fkGdpEdizione;
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

        return new AcquisizioneList()
                .codice(GdpMessage.F_OK.getCodice())
                .messaggio(GdpMessage.F_OK.getDescrizioneDefault())
                .acquisizioni(summaries);
    }

    @Override
    public AcquisizioneDetail dettaglioAcquisizione(Integer idLog) {
        GdpLog log = logRepository.findById(idLog);
        if (log == null) {
            LOG.warnf("F13: GDP_LOG non trovato per idLog=%d", idLog);
            throw new GdpException(
                    GdpMessage.F13_MONITOR_ERROR,
                    GdpMessage.F13_MONITOR_ERROR.getDescrizioneDefault() + " [idLog=" + idLog
                            + ", causa=GDP_LOG non trovato]",
                    Response.Status.NOT_FOUND);
        }

        List<GdpLogEdizione> logEdizioni = logEdizioneRepository.findByLog(idLog);
        if (logEdizioni == null || logEdizioni.isEmpty()) {
            LOG.warnf("F13: GDP_LOG_EDIZIONE non trovato per idLog=%d", idLog);
            throw new GdpException(
                    GdpMessage.F13_MONITOR_ERROR,
                    GdpMessage.F13_MONITOR_ERROR.getDescrizioneDefault() + " [idLog=" + idLog
                            + ", causa=GDP_LOG_EDIZIONE non trovato]",
                    Response.Status.NOT_FOUND);
        }

        GdpLogEdizione logEdizione = logEdizioni.stream()
                .sorted(Comparator.comparing((GdpLogEdizione le) -> le.id, Comparator.nullsLast(Integer::compareTo))
                        .reversed())
                .filter(le -> le.fkGdpEdizione != null)
                .findFirst()
                .orElseGet(() -> logEdizioni.stream()
                        .max(Comparator.comparing((GdpLogEdizione le) -> le.id,
                                Comparator.nullsLast(Integer::compareTo)))
                        .orElse(logEdizioni.get(0)));
        GdpTestata testata = testataRepository.findById(log.fkGdpTestata);
        GdpEdizione edizione = logEdizione.fkGdpEdizione != null
                ? edizioneRepository.findById(logEdizione.fkGdpEdizione)
                : null;
        if (testata == null) {
            LOG.warnf("F13: GDP_TESTATA non trovata per idLog=%d fkGdpTestata=%d", idLog, log.fkGdpTestata);
        }
        if (edizione == null) {
            LOG.warnf("F13: GDP_EDIZIONE non trovata per idLog=%d fkGdpEdizione=%s", idLog,
                    String.valueOf(logEdizione.fkGdpEdizione));
        }

        AcquisizioneDetail detail = new AcquisizioneDetail();
        detail.setIdLog(log.id);
        detail.setIdTestata(log.fkGdpTestata);
        if (testata != null) {
            detail.setNomeTestata(testata.nomeTestata);
        }
        if (edizione != null) {
            detail.setDataEdizione(edizione.dataEdizione);
        }
        if (logEdizione.tipoEdizione != null) {
            detail.setTipoEdizione(logEdizione.tipoEdizione.getDescrizione());
            detail.setTipoEdizioneCode(
                    it.csipiemonte.gdp.gdporch.dto.TipoEdizione.fromValue(logEdizione.tipoEdizione.name()));
        }
        if (log.tipoAcquisizione != null) {
            detail.setTipoAcquisizione(AcquisizioneDetail.TipoAcquisizioneEnum.fromValue(log.tipoAcquisizione.name()));
        }
        if (log.dataAcquisizione != null) {
            detail.setDataAcquisizione(
                    Date.from(log.dataAcquisizione.atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }
        detail.setNroTotFile(log.totaleFileAcquisiti);
        if (log.esito == null || log.esito.isBlank()) {
            detail.setEsito(GdpMessage.Codes.MSG00009);
        } else {
            detail.setEsito(log.esito);
        }
        detail.setIdEdizione(logEdizione.fkGdpEdizione);
        detail.setPrimaPagina(logEdizione.primaPagina);
        detail.setFileXML(logEdizione.fileXml);
        detail.setFileZIP(logEdizione.fileZip);
        detail.setNroPagAcq(logEdizione.nroPagAcquisite);
        detail.setNroPagOK(logEdizione.nroPagValide);
        detail.setNroPagErrate(logEdizione.nroPagErrate);
        detail.setJobId(logEdizione.jobId);
        detail.setDescrizione(logEdizione.descrizione);
        return detail;
    }
}