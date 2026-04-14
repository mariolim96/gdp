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
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import it.csipiemonte.gdp.gdporch.mapper.AcquisizioneRicercaMapper;
import it.csipiemonte.gdp.gdporch.model.projection.AcquisizioneRicercaProjection;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogRepository;
import it.csipiemonte.gdp.gdporch.service.impl.GdpMonitorAcquisizioniServiceImpl;
import java.time.LocalDate;
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
                // Obiettivo (TB-F15-I01): verificare che F15 applichi i filtri e
                // ritorni il mapping completo verso il DTO di risposta.
                // Dipendenze usate: logRepository.searchAcquisizioni +
                // acquisizioneRicercaMapper.toDto.
                LocalDate dataDa = LocalDate.of(2026, 4, 1);
                LocalDate dataA = LocalDate.of(2026, 4, 13);
                AcquisizioneRicercaProjection projection = new AcquisizioneRicercaProjection(
                                10,
                                22,
                                "Gazzetta Test",
                                it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione.OK,
                                LocalDate.of(2026, 4, 12),
                                LocalDate.of(2026, 4, 12),
                                12,
                                11);
                AcquisizioneRicercaSummary dto = new AcquisizioneRicercaSummary()
                                .idLog(10)
                                .idTestata(22)
                                .nomeTestata("Gazzetta Test")
                                .tipoEdizione(TipoEdizione.OK)
                                .dataEdizione(LocalDate.of(2026, 4, 12))
                                .dataAcquisizione(Date.from(LocalDate.of(2026, 4, 12)
                                                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()))
                                .nroTotFileAcq(12)
                                .nroTotFileVal(11);

                when(logRepository.searchAcquisizioni(
                                eq(it.csipiemonte.gdp.gdporch.model.enums.TipoAcquisizione.G),
                                eq(22),
                                eq(dataDa),
                                eq(dataA),
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
                                eq(dataDa),
                                eq(dataA),
                                eq(it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione.OK));
        }

        @Test
        void ricercaAcquisizioniUsaDataDaDefaultQuandoAssente() {
                // Obiettivo: allinearsi all'implementazione che usa 1900-01-01 quando
                // dataDA non e valorizzata.
                // Dipendenze usate: logRepository.searchAcquisizioni con verifica
                // parametro dataDa default.
                LocalDate dataA = LocalDate.of(2026, 4, 13);

                AcquisizioneRicercaProjection projection = new AcquisizioneRicercaProjection(
                                11,
                                22,
                                "Gazzetta Test",
                                it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione.OK,
                                LocalDate.of(2026, 4, 12),
                                LocalDate.of(2026, 4, 12),
                                5,
                                5);
                AcquisizioneRicercaSummary dto = new AcquisizioneRicercaSummary().idLog(11);

                when(logRepository.searchAcquisizioni(
                                eq(it.csipiemonte.gdp.gdporch.model.enums.TipoAcquisizione.G),
                                eq(22),
                                eq(LocalDate.of(1900, 1, 1)),
                                eq(dataA),
                                eq(it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione.OK)))
                                .thenReturn(List.of(projection));
                when(acquisizioneRicercaMapper.toDto(projection)).thenReturn(dto);

                AcquisizioneRicercaList response = service.ricercaAcquisizioni("G", 22, dataA, TipoEdizione.OK, null);

                assertNotNull(response);
                assertEquals(1, response.getAcquisizioni().size());
                verify(logRepository).searchAcquisizioni(
                                eq(it.csipiemonte.gdp.gdporch.model.enums.TipoAcquisizione.G),
                                eq(22),
                                eq(LocalDate.of(1900, 1, 1)),
                                eq(dataA),
                                eq(it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione.OK));
        }

        @Test
        void ricercaAcquisizioniLanciaEccezioneSeNessunDato() {
                // Obiettivo: coprire il ramo F15_NO_RESULTS quando il repository non
                // restituisce righe.
                // Dipendenze usate: logRepository.searchAcquisizioni.
                LocalDate dataA = LocalDate.of(2026, 4, 13);
                when(logRepository.searchAcquisizioni(
                                eq(it.csipiemonte.gdp.gdporch.model.enums.TipoAcquisizione.S),
                                eq(99),
                                eq(LocalDate.of(1900, 1, 1)),
                                eq(dataA),
                                eq(it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione.ST)))
                                .thenReturn(Collections.emptyList());

                GdpException exception = assertThrows(GdpException.class,
                                () -> service.ricercaAcquisizioni("S", 99, dataA, TipoEdizione.ST, null));

                assertEquals("MSG00001", exception.getCodice());
                assertEquals(GdpMessage.F15_NO_RESULTS.getDescrizioneDefault(), exception.getMessage());
        }

        @Test
        void ricercaAcquisizioniValidaRangeDate() {
                // Obiettivo: garantire che dataDA > dataA produca errore di validazione
                // coerente con la business rule.
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

        @Test
        void ricercaAcquisizioniValidaParametriObbligatori() {
                // Obiettivo: verificare i tre controlli obbligatori previsti da F15
                // (idTestata, dataA, tipoEdizione).
                // Dipendenze usate: nessuna, i controlli avvengono prima di chiamare il
                // repository.
                GdpException idTestataMissing = assertThrows(GdpException.class,
                                () -> service.ricercaAcquisizioni("G", null, LocalDate.of(2026, 4, 13), TipoEdizione.OK,
                                                null));
                assertEquals("MSG00002", idTestataMissing.getCodice());
                assertEquals(GdpMessage.F15_ID_TESTATA_REQUIRED.getDescrizioneDefault(), idTestataMissing.getMessage());

                GdpException dataAMissing = assertThrows(GdpException.class,
                                () -> service.ricercaAcquisizioni("G", 1, null, TipoEdizione.OK, null));
                assertEquals("MSG00002", dataAMissing.getCodice());
                assertEquals(GdpMessage.F15_DATA_A_REQUIRED.getDescrizioneDefault(), dataAMissing.getMessage());

                GdpException tipoEdizioneMissing = assertThrows(GdpException.class,
                                () -> service.ricercaAcquisizioni("G", 1, LocalDate.of(2026, 4, 13), null, null));
                assertEquals("MSG00002", tipoEdizioneMissing.getCodice());
                assertEquals(GdpMessage.F15_TIPO_EDIZIONE_REQUIRED.getDescrizioneDefault(),
                                tipoEdizioneMissing.getMessage());
        }

        @Test
        void ricercaAcquisizioniValidaTipoAcquisizione() {
                // Obiettivo: verificare il controllo su tipoAcquisizione diverso da G/S.
                // Dipendenze usate: nessuna chiamata repository attesa.
                GdpException exception = assertThrows(GdpException.class,
                                () -> service.ricercaAcquisizioni(
                                                "X",
                                                22,
                                                LocalDate.of(2026, 4, 13),
                                                TipoEdizione.OK,
                                                LocalDate.of(2026, 4, 1)));

                assertEquals("MSG00002", exception.getCodice());
                assertEquals(GdpMessage.F15_INVALID_TIPO_ACQUISIZIONE.getDescrizioneDefault(), exception.getMessage());
        }
}