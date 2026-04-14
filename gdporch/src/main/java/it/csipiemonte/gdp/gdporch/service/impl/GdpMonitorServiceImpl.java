package it.csipiemonte.gdp.gdporch.service.impl;

import it.csipiemonte.gdp.gdporch.dto.AcquisizioneList;
import it.csipiemonte.gdp.gdporch.dto.AcquisizioneSummary;
import it.csipiemonte.gdp.gdporch.exception.GdpException;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import it.csipiemonte.gdp.gdporch.model.entity.*;
import it.csipiemonte.gdp.gdporch.model.enums.StatoCodaCaricamento;
import it.csipiemonte.gdp.gdporch.model.enums.TipoAcquisizione;
import it.csipiemonte.gdp.gdporch.model.repository.*;
import it.csipiemonte.gdp.gdporch.service.GdpMonitorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Inject
    GdpCodaCaricamentoRepository  caricamentoRepository;

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
    @Transactional
    public String attivaCoda(Integer idLog, Integer idGdpEdizione) {
        GdpCodaCaricamento  coda = caricamentoRepository.find("From GdpCodaCaricamento c Where c.fkGdpLogEdizione IN "+
                "(SELECT le.id FROM GdpLogEdizione le WHERE le.fkGdpLog = ?1 and le.fkGdpEdizione = ?2)",idLog,idGdpEdizione).firstResult();

        if(coda==null){
            return "MSG0000x";
        }

        //Logica di incremento
        int nuovoTentativo = coda.nroTentativo+1;
        int maxTentativi = (coda.nroMaxTentativi != null) ? coda.nroMaxTentativi : 10;

        if(nuovoTentativo <= maxTentativi){
            coda.nroTentativo = nuovoTentativo;
            coda.dataInserimento = LocalDate.now();
            coda.dataTentativo = LocalDateTime.now();
            coda.stato = StatoCodaCaricamento.READY;
            return "MSG00009"; //SUCCESSO
        }else {
            return "MSG00001"; //TROPPI TENTATIVI
        }
    }
}