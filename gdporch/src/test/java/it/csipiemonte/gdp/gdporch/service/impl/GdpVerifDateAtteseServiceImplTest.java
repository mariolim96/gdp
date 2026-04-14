package it.csipiemonte.gdp.gdporch.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.csipiemonte.gdp.gdporch.dto.DateAtteseList;
import it.csipiemonte.gdp.gdporch.dto.DateAttesePerTestata;
import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GdpVerifDateAtteseServiceImplTest {

    @Mock
    private GdpTestataRepository gdpTestataRepository;

    private GdpVerifDateAtteseServiceImpl service;

    @BeforeEach
    void setUp() {
        // Inizializza i mock e inietta il repository nel service sotto test.
        MockitoAnnotations.openMocks(this);
        service = new GdpVerifDateAtteseServiceImpl();
        service.gdpTestataRepository = gdpTestataRepository;
    }

    @Test
    void executeRestituisceDateAtteseConMixSospeseENonSospeseEConCartellaTestata() {
        // Obiettivo (TB-F18-I01 + I04):
        // - verificare il mapping delle date attese (mix sospesa=true/false)
        // - verificare presenza di cartellaTestata e conteggio nroEdizioniAttese
        // Dipendenze usate:
        // - gdpTestataRepository.findDateAttese(...) mockato con 3 righe per la stessa
        // testata.
        DateRangeRequest request = new DateRangeRequest()
                .dataInizio(LocalDate.of(2026, 3, 1))
                .dataFine(LocalDate.of(2026, 3, 31));

        List<Object[]> rows = List.of(
                new Object[] { 1, "sentinella", LocalDate.of(2026, 3, 8), Boolean.FALSE },
                new Object[] { 1, "sentinella", LocalDate.of(2026, 3, 15), Boolean.TRUE },
                new Object[] { 1, "sentinella", LocalDate.of(2026, 3, 22), Boolean.FALSE });

        when(gdpTestataRepository.findDateAttese(request.getDataInizio(), request.getDataFine(), 1))
                .thenReturn(rows);

        DateAtteseList response = service.execute(request, 1);

        assertNotNull(response);
        assertEquals(1, response.getTestate().size());

        DateAttesePerTestata testata = response.getTestate().get(0);
        assertEquals(1, testata.getIdTestata());
        assertEquals("sentinella", testata.getCartellaTestata());
        assertEquals(3, testata.getNroEdizioniAttese());
        assertEquals(3, testata.getDateAttese().size());
        assertEquals(LocalDate.of(2026, 3, 8), testata.getDateAttese().get(0).getData());
        assertEquals(false, testata.getDateAttese().get(0).getSospesa());
        assertEquals(LocalDate.of(2026, 3, 15), testata.getDateAttese().get(1).getData());
        assertEquals(true, testata.getDateAttese().get(1).getSospesa());
    }

    @Test
    void executeRitornaListaVuotaQuandoNessunaDataNelRange() {
        // Obiettivo (TB-F18-I02, coerente con implementazione attuale):
        // - quando il repository non trova righe nel range, il servizio ritorna
        // DateAtteseList vuota.
        // Dipendenze usate:
        // - gdpTestataRepository.findDateAttese(...) mockato con lista vuota.
        DateRangeRequest request = new DateRangeRequest()
                .dataInizio(LocalDate.of(2026, 5, 1))
                .dataFine(LocalDate.of(2026, 5, 31));

        when(gdpTestataRepository.findDateAttese(request.getDataInizio(), request.getDataFine(), 1))
                .thenReturn(List.of());

        DateAtteseList response = service.execute(request, 1);

        assertNotNull(response);
        assertNotNull(response.getTestate());
        assertEquals(0, response.getTestate().size());
    }

    @Test
    void executeSenzaIdTestataInterrogaRepositoryConNullERaggruppaPiuTestate() {
        // Obiettivo (TB-F18-I03):
        // - verificare che senza idTestata il servizio passi null al repository
        // - verificare il raggruppamento in output su piu testate invianti.
        // Dipendenze usate:
        // - gdpTestataRepository.findDateAttese(..., null) mockato con righe di due
        // testate.
        DateRangeRequest request = new DateRangeRequest()
                .dataInizio(LocalDate.of(2026, 3, 1))
                .dataFine(LocalDate.of(2026, 3, 31));

        List<Object[]> rows = List.of(
                new Object[] { 1, "sentinella", LocalDate.of(2026, 3, 8), Boolean.FALSE },
                new Object[] { 2, "cuneonews", LocalDate.of(2026, 3, 9), Boolean.FALSE },
                new Object[] { 2, "cuneonews", LocalDate.of(2026, 3, 16), Boolean.TRUE });

        when(gdpTestataRepository.findDateAttese(request.getDataInizio(), request.getDataFine(), null))
                .thenReturn(rows);

        DateAtteseList response = service.execute(request, null);

        verify(gdpTestataRepository).findDateAttese(request.getDataInizio(), request.getDataFine(), null);
        assertEquals(2, response.getTestate().size());
        assertEquals(1, response.getTestate().get(0).getIdTestata());
        assertEquals(1, response.getTestate().get(0).getNroEdizioniAttese());
        assertEquals(2, response.getTestate().get(1).getIdTestata());
        assertEquals(2, response.getTestate().get(1).getNroEdizioniAttese());
    }

    @Test
    void executeLanciaErroreSeParametriMancanti() {
        // Obiettivo:
        // - validare i controlli input all'ingresso del service.
        // Dipendenze usate:
        // - nessuna query DB: il metodo termina prima di chiamare il repository.
        DateRangeRequest request = new DateRangeRequest().dataInizio(LocalDate.of(2026, 3, 1));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.execute(request, 1));

        assertEquals("Missing required parameters", exception.getMessage());
    }

    @Test
    void executeLanciaErroreSeIntervalloDateNonValido() {
        // Obiettivo:
        // - validare il vincolo dataInizio <= dataFine.
        // Dipendenze usate:
        // - nessuna query DB: il metodo termina in validazione.
        DateRangeRequest request = new DateRangeRequest()
                .dataInizio(LocalDate.of(2026, 4, 1))
                .dataFine(LocalDate.of(2026, 3, 1));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.execute(request, 1));

        assertEquals("dataInizio > dataFine", exception.getMessage());
    }
}
