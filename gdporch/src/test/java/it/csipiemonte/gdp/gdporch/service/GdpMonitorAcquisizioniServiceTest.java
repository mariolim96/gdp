package it.csipiemonte.gdp.gdporch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.csipiemonte.gdp.gdporch.dto.AcquisizioneRicercaList;
import it.csipiemonte.gdp.gdporch.dto.AcquisizioneRicercaSummary;
import it.csipiemonte.gdp.gdporch.dto.TipoEdizione;
import it.csipiemonte.gdp.gdporch.exception.GdpException;
import it.csipiemonte.gdp.gdporch.mapper.AcquisizioneRicercaMapper;
import it.csipiemonte.gdp.gdporch.model.projection.AcquisizioneRicercaProjection;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogRepository;
import it.csipiemonte.gdp.gdporch.service.impl.GdpMonitorAcquisizioniServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GdpMonitorAcquisizioniServiceTest {

    @Mock
    private GdpLogRepository logRepository;

    @Mock
    private AcquisizioneRicercaMapper acquisizioneRicercaMapper;

    private GdpMonitorAcquisizioniServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new GdpMonitorAcquisizioniServiceImpl(logRepository, acquisizioneRicercaMapper);
    }

    @Test
    void ricercaAcquisizioniRestituisceRisultatiFiltrati() {
        LocalDate dataDa = LocalDate.of(2026, 4, 1);
        LocalDate dataA = LocalDate.of(2026, 4, 13);
        AcquisizioneRicercaProjection projection = new AcquisizioneRicercaProjection(
                10,
                22,
                "Gazzetta Test",
                it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione.OK,
                LocalDate.of(2026, 4, 12),
                LocalDateTime.of(2026, 4, 12, 9, 45),
                12,
                11);
        AcquisizioneRicercaSummary dto = new AcquisizioneRicercaSummary()
                .idLog(10)
                .idTestata(22)
                .nomeTestata("Gazzetta Test")
                .tipoEdizione(TipoEdizione.OK)
                .dataEdizione(LocalDate.of(2026, 4, 12))
                .dataAcquisizione(Date.from(LocalDateTime.of(2026, 4, 12, 9, 45)
                        .atZone(java.time.ZoneId.systemDefault()).toInstant()))
                .nroTotFileAcq(12)
                .nroTotFileVal(11);

        when(logRepository.searchAcquisizioni(
                eq(it.csipiemonte.gdp.gdporch.model.enums.TipoAcquisizione.G),
                eq(22),
                eq(dataDa.atStartOfDay()),
                eq(dataA.plusDays(1).atStartOfDay()),
                eq(it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione.OK)))
                .thenReturn(List.of(projection));
        when(acquisizioneRicercaMapper.toDto(projection)).thenReturn(dto);

        AcquisizioneRicercaList response = service.ricercaAcquisizioni("G", 22, dataA, TipoEdizione.OK, dataDa);

        assertNotNull(response);
        assertEquals(1, response.getAcquisizioni().size());
        assertEquals(10, response.getAcquisizioni().get(0).getIdLog());
        verify(logRepository).searchAcquisizioni(
                eq(it.csipiemonte.gdp.gdporch.model.enums.TipoAcquisizione.G),
                eq(22),
                eq(dataDa.atStartOfDay()),
                eq(dataA.plusDays(1).atStartOfDay()),
                eq(it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione.OK));
    }

    @Test
    void ricercaAcquisizioniLanciaEccezioneSeNessunDato() {
        LocalDate dataA = LocalDate.of(2026, 4, 13);
        when(logRepository.searchAcquisizioni(
                eq(it.csipiemonte.gdp.gdporch.model.enums.TipoAcquisizione.S),
                eq(99),
                eq(LocalDate.of(1900, 1, 1).atStartOfDay()),
                eq(dataA.plusDays(1).atStartOfDay()),
                eq(it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione.ST)))
                .thenReturn(Collections.emptyList());

        GdpException exception = assertThrows(GdpException.class,
                () -> service.ricercaAcquisizioni("S", 99, dataA, TipoEdizione.ST, null));

        assertEquals("MSG00001", exception.getCodice());
    }

    @Test
    void ricercaAcquisizioniValidaRangeDate() {
        GdpException exception = assertThrows(GdpException.class,
                () -> service.ricercaAcquisizioni(
                        "G",
                        22,
                        LocalDate.of(2026, 4, 13),
                        TipoEdizione.OK,
                        LocalDate.of(2026, 4, 14)));

        assertEquals("MSG00002", exception.getCodice());
        assertEquals("Il parametro dataDA non può essere successivo a dataA", exception.getMessage());
    }
}