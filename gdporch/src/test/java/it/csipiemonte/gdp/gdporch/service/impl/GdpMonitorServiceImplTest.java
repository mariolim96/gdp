package it.csipiemonte.gdp.gdporch.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.csipiemonte.gdp.gdporch.dto.AcquisizioneList;
import it.csipiemonte.gdp.gdporch.dto.AcquisizioneDetail;
import it.csipiemonte.gdp.gdporch.exception.GdpException;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
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
import java.time.ZoneId;
import java.util.Collections;
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
        // Inizializza i mock repository e inietta le dipendenze nel service sotto test.
        MockitoAnnotations.openMocks(this);
        service = new GdpMonitorServiceImpl();
        service.logRepository = logRepository;
        service.testataRepository = testataRepository;
        service.edizioneRepository = edizioneRepository;
        service.logEdizioneRepository = logEdizioneRepository;
    }

    @Test
    void elencoAcquisizioniRestituisceRisultatiPerDataETipo() {
        // Obiettivo (TB-F12-I01):
        // - verificare che F12 ritorni acquisizioni per data/tipo con mapping campi
        // principali.
        // Dipendenze usate:
        // - logRepository.findByTipoAcquisizioneAndDataAcquisizione
        // - testataRepository.findById
        // - logEdizioneRepository.findByLog
        // - edizioneRepository.findById
        LocalDate dataAcquisizione = LocalDate.of(2026, 3, 20);

        GdpLog log = new GdpLog();
        log.id = 101;
        log.fkGdpUtenteFtp = 7;
        log.fkGdpTestata = 1;
        log.tipoAcquisizione = TipoAcquisizione.G;
        log.dataAcquisizione = LocalDateTime.of(2026, 3, 20, 8, 30);
        log.totaleFileAcquisiti = 12;
        log.esito = "MSG00009";

        GdpTestata testata = new GdpTestata();
        testata.id = 1;
        testata.nomeTestata = "La Sentinella";

        GdpLogEdizione logEdizione = new GdpLogEdizione();
        logEdizione.fkGdpLog = 101;
        logEdizione.fkGdpEdizione = 501;
        logEdizione.tipoEdizione = TipoEdizione.OK;

        GdpEdizione edizione = new GdpEdizione();
        edizione.id = 501;
        edizione.dataEdizione = LocalDate.of(2026, 3, 19);

        when(logRepository.findByTipoAcquisizioneAndDataAcquisizione(TipoAcquisizione.G, dataAcquisizione))
                .thenReturn(List.of(log));
        when(testataRepository.findById(1)).thenReturn(testata);
        when(logEdizioneRepository.findByLog(101)).thenReturn(List.of(logEdizione));
        when(edizioneRepository.findById(501)).thenReturn(edizione);

        AcquisizioneList response = service.elencoAcquisizioni("G", dataAcquisizione);

        assertNotNull(response);
        assertEquals(GdpMessage.F_OK.getCodice(), response.getCodice());
        assertEquals(1, response.getAcquisizioni().size());
        assertEquals(101, response.getAcquisizioni().get(0).getIdLog());
        assertEquals("La Sentinella", response.getAcquisizioni().get(0).getNomeTestata());
        assertEquals("Regolare (Corrispondente)", response.getAcquisizioni().get(0).getTipoEdizione());
        assertEquals(1, response.getAcquisizioni().get(0).getNroEdizioni());
        assertEquals(
                java.util.Date.from(log.dataAcquisizione.atZone(ZoneId.systemDefault()).toInstant()),
                response.getAcquisizioni().get(0).getDataAcquisizione());
    }

    @Test
    void elencoAcquisizioniStoricaCalcolaNroEdizioniDaCountByLog() {
        // Obiettivo (TB-F12-I02):
        // - per tipo storico (S), verificare che nroEdizioni usi countByLog invece del
        // valore fisso 1.
        // Dipendenze usate:
        // - logRepository.findByTipoAcquisizioneAndDataAcquisizione
        // - logEdizioneRepository.countByLog
        LocalDate dataAcquisizione = LocalDate.of(2026, 3, 21);

        GdpLog log = new GdpLog();
        log.id = 202;
        log.fkGdpUtenteFtp = 9;
        log.fkGdpTestata = 2;
        log.tipoAcquisizione = TipoAcquisizione.S;
        log.dataAcquisizione = LocalDateTime.of(2026, 3, 21, 9, 0);
        log.totaleFileAcquisiti = 5;
        log.esito = "MSG00009";

        GdpTestata testata = new GdpTestata();
        testata.id = 2;
        testata.nomeTestata = "Storico Test";

        GdpLogEdizione logEdizione = new GdpLogEdizione();
        logEdizione.fkGdpLog = 202;
        logEdizione.fkGdpEdizione = 601;
        logEdizione.tipoEdizione = TipoEdizione.ST;

        GdpEdizione edizione = new GdpEdizione();
        edizione.id = 601;
        edizione.dataEdizione = LocalDate.of(2026, 3, 20);

        when(logRepository.findByTipoAcquisizioneAndDataAcquisizione(TipoAcquisizione.S, dataAcquisizione))
                .thenReturn(List.of(log));
        when(testataRepository.findById(2)).thenReturn(testata);
        when(logEdizioneRepository.findByLog(202)).thenReturn(List.of(logEdizione));
        when(logEdizioneRepository.countByLog(202)).thenReturn(3L);
        when(edizioneRepository.findById(601)).thenReturn(edizione);

        AcquisizioneList response = service.elencoAcquisizioni("S", dataAcquisizione);

        assertEquals(1, response.getAcquisizioni().size());
        assertEquals(3, response.getAcquisizioni().get(0).getNroEdizioni());
        verify(logEdizioneRepository).countByLog(202);
    }

    @Test
    void elencoAcquisizioniLanciaEccezioneSeTipoNonValidoONessunDato() {
        // Obiettivo:
        // - validare i due error path F12: tipo non valido e nessun risultato.
        // Dipendenze usate:
        // - logRepository.findByTipoAcquisizioneAndDataAcquisizione solo nel secondo
        // scenario.
        LocalDate dataAcquisizione = LocalDate.of(2026, 3, 22);

        GdpException invalidType = assertThrows(GdpException.class,
                () -> service.elencoAcquisizioni("X", dataAcquisizione));
        assertEquals("MSG00001", invalidType.getCodice());

        when(logRepository.findByTipoAcquisizioneAndDataAcquisizione(TipoAcquisizione.G, dataAcquisizione))
                .thenReturn(Collections.emptyList());

        GdpException noData = assertThrows(GdpException.class,
                () -> service.elencoAcquisizioni("G", dataAcquisizione));
        assertEquals("MSG00001", noData.getCodice());
        assertEquals(GdpMessage.F12_MONITOR_ERROR.getDescrizioneDefault(), noData.getMessage());
    }

    @Test
    void dettaglioAcquisizioneUsaLogEdizioneConFkEdizioneValorizzata() {
        // Obiettivo (F13):
        // - scegliere una log-edizione con fkGdpEdizione valorizzato anche se la prima
        // riga non lo ha.
        // Dipendenze usate:
        // - logRepository.findById
        // - logEdizioneRepository.findByLog
        // - testataRepository.findById
        // - edizioneRepository.findById
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
        // Obiettivo (F13):
        // - verificare errore business con dettaglio causa quando GDP_LOG non esiste.
        // Dipendenze usate:
        // - logRepository.findById mockato con null.
        when(logRepository.findById(123)).thenReturn(null);

        GdpException exception = assertThrows(GdpException.class, () -> service.dettaglioAcquisizione(123));

        assertEquals("MSG00001", exception.getCodice());
        assertEquals("Problema nel recupero dei dati di monitoraggio [idLog=123, causa=GDP_LOG non trovato]",
                exception.getMessage());
    }

    @Test
    void dettaglioAcquisizioneNonFallisceSeEdizioneMancante() {
        // Obiettivo (F13):
        // - il servizio non deve fallire se GDP_EDIZIONE non è trovata, ma restituire i
        // dati disponibili.
        // Dipendenze usate:
        // - logRepository.findById
        // - logEdizioneRepository.findByLog
        // - testataRepository.findById
        // - edizioneRepository.findById mockato con null.
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
