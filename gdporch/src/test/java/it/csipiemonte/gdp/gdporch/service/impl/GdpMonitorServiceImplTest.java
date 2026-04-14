package it.csipiemonte.gdp.gdporch.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import it.csipiemonte.gdp.gdporch.dto.AcquisizioneDetail;
import it.csipiemonte.gdp.gdporch.exception.GdpException;
import it.csipiemonte.gdp.gdporch.model.entity.GdpEdizione;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLog;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLogEdizione;
import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import it.csipiemonte.gdp.gdporch.model.enums.TipoAcquisizione;
import it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione;
import it.csipiemonte.gdp.gdporch.model.repository.GdpEdizioneRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogEdizioneRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GdpMonitorServiceImplTest {

    @Mock
    private GdpLogRepository logRepository;

    @Mock
    private GdpTestataRepository testataRepository;

    @Mock
    private GdpEdizioneRepository edizioneRepository;

    @Mock
    private GdpLogEdizioneRepository logEdizioneRepository;

    private GdpMonitorServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new GdpMonitorServiceImpl();
        service.logRepository = logRepository;
        service.testataRepository = testataRepository;
        service.edizioneRepository = edizioneRepository;
        service.logEdizioneRepository = logEdizioneRepository;
    }

    @Test
    void dettaglioAcquisizioneUsaLogEdizioneConFkEdizioneValorizzata() {
        GdpLog log = new GdpLog();
        log.id = 77;
        log.fkGdpTestata = 9;
        log.tipoAcquisizione = TipoAcquisizione.S;
        log.dataAcquisizione = LocalDateTime.of(2026, 4, 12, 9, 45);
        log.totaleFileAcquisiti = 12;
        log.esito = null;

        GdpLogEdizione incompleta = new GdpLogEdizione();
        incompleta.fkGdpLog = 77;
        incompleta.fkGdpEdizione = null;
        incompleta.tipoEdizione = TipoEdizione.ST;

        GdpLogEdizione completa = new GdpLogEdizione();
        completa.fkGdpLog = 77;
        completa.fkGdpEdizione = 1001;
        completa.tipoEdizione = TipoEdizione.ST;
        completa.primaPagina = Boolean.TRUE;
        completa.fileXml = Boolean.TRUE;
        completa.fileZip = Boolean.FALSE;
        completa.nroPagAcquisite = 12;
        completa.nroPagValide = 11;
        completa.nroPagErrate = 1;
        completa.jobId = "999";
        completa.descrizione = "Acquisizione storica";

        GdpTestata testata = new GdpTestata();
        testata.id = 9;
        testata.nomeTestata = "Gazzetta Test";

        GdpEdizione edizione = new GdpEdizione();
        edizione.id = 1001;
        edizione.dataEdizione = LocalDate.of(2026, 4, 10);

        when(logRepository.findById(77)).thenReturn(log);
        when(logEdizioneRepository.findByLog(77)).thenReturn(List.of(incompleta, completa));
        when(testataRepository.findById(9)).thenReturn(testata);
        when(edizioneRepository.findById(1001)).thenReturn(edizione);

        AcquisizioneDetail result = service.dettaglioAcquisizione(77);

        assertEquals(1001, result.getIdEdizione());
        assertEquals("MSG00009", result.getEsito());
    }

    @Test
    void dettaglioAcquisizioneLanciaEccezioneSeLogInesistente() {
        when(logRepository.findById(123)).thenReturn(null);

        GdpException exception = assertThrows(GdpException.class, () -> service.dettaglioAcquisizione(123));

        assertEquals("MSG00001", exception.getCodice());
        assertEquals("Problema nel recupero dei dati di monitoraggio [idLog=123, causa=GDP_LOG non trovato]",
                exception.getMessage());
    }

    @Test
    void dettaglioAcquisizioneNonFallisceSeEdizioneMancante() {
        GdpLog log = new GdpLog();
        log.id = 90;
        log.fkGdpTestata = 3;
        log.tipoAcquisizione = TipoAcquisizione.G;
        log.dataAcquisizione = LocalDateTime.of(2026, 4, 14, 8, 0);
        log.totaleFileAcquisiti = 5;
        log.esito = "MSG00009";

        GdpLogEdizione logEdizione = new GdpLogEdizione();
        logEdizione.id = 901;
        logEdizione.fkGdpLog = 90;
        logEdizione.fkGdpEdizione = 3003;
        logEdizione.tipoEdizione = TipoEdizione.OK;

        GdpTestata testata = new GdpTestata();
        testata.id = 3;
        testata.nomeTestata = "Testata senza edizione";

        when(logRepository.findById(90)).thenReturn(log);
        when(logEdizioneRepository.findByLog(90)).thenReturn(List.of(logEdizione));
        when(testataRepository.findById(3)).thenReturn(testata);
        when(edizioneRepository.findById(3003)).thenReturn(null);

        AcquisizioneDetail result = service.dettaglioAcquisizione(90);

        assertEquals(90, result.getIdLog());
        assertEquals(3003, result.getIdEdizione());
        assertEquals("Testata senza edizione", result.getNomeTestata());
    }
}
