package it.csipiemonte.gdp.gdporch.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.dto.SospensioneResponse;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import it.csipiemonte.gdp.gdporch.model.repository.GdpDataUscitaRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpPeriodicitaRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GdpSospensioneServiceImplTest {

    @Mock
    private GdpDataUscitaRepository dataUscitaRepository;

    @Mock
    private GdpPeriodicitaRepository periodicitaRepository;

    private GdpSospensioneServiceImpl service;

    @BeforeEach
    void setUp() {
        // Inizializza i mock dei repository e crea il service sotto test con dependency
        // injection manuale.
        MockitoAnnotations.openMocks(this);
        service = new GdpSospensioneServiceImpl();
        service.dataUscitaRepository = dataUscitaRepository;
        service.periodicitaRepository = periodicitaRepository;
    }

    @Test
    void sospendiAggiornaDateNelRangeEPeriodicita() {
        // Caso F05-I01 (successo):
        // - Usa periodicitaRepository.findActiveByTestata per recuperare la periodicita
        // attiva.
        // - Usa dataUscitaRepository.findByPeriodicitaAndRange per recuperare le date
        // nel range.
        // Verifica che il servizio:
        // - aggiorni inizio/fine sospensione sulla periodicita,
        // - imposti sospesa=true su tutte le date trovate,
        // - ritorni MSG00009 e il numero corretto di giorni sospesi.
        Integer idTestata = 1;
        DateRangeRequest request = request(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 22));

        GdpPeriodicita periodicita = new GdpPeriodicita();
        periodicita.id = 10;

        GdpDataUscita d1 = new GdpDataUscita();
        d1.sospesa = false;
        GdpDataUscita d2 = new GdpDataUscita();
        d2.sospesa = false;

        when(periodicitaRepository.findActiveByTestata(idTestata)).thenReturn(periodicita);
        when(dataUscitaRepository.findByPeriodicitaAndRange(10, request.getDataInizio(), request.getDataFine()))
                .thenReturn(List.of(d1, d2));

        SospensioneResponse response = service.sospendi(idTestata, request);

        assertEquals(GdpMessage.F_OK.getCodice(), response.getMessage());
        assertEquals(2, response.getGiorniSospesi());
        assertEquals(LocalDate.of(2026, 3, 8), periodicita.inizioSospensione);
        assertEquals(LocalDate.of(2026, 3, 22), periodicita.fineSospensione);
        assertTrue(d1.sospesa);
        assertTrue(d2.sospesa);
    }

    @Test
    void sospendiRitornaNoResultsQuandoPeriodicitaAssente() {
        // Caso no-results: periodicita assente per la testata.
        // Usa solo periodicitaRepository.findActiveByTestata (ritorna null).
        // Verifica che il servizio ritorni MSG00001 con giorniSospesi=0
        // e che NON interroghi dataUscitaRepository.
        Integer idTestata = 1;
        DateRangeRequest request = request(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 22));

        when(periodicitaRepository.findActiveByTestata(idTestata)).thenReturn(null);

        SospensioneResponse response = service.sospendi(idTestata, request);

        assertEquals(GdpMessage.F05_NO_RESULTS.getCodice(), response.getMessage());
        assertEquals(0, response.getGiorniSospesi());
        verify(dataUscitaRepository, never()).findByPeriodicitaAndRange(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sospendiRitornaNoResultsQuandoNessunaDataNelRange() {
        // Caso F05-I02: periodicita presente ma nessuna data attesa nel range.
        // Usa:
        // - periodicitaRepository.findActiveByTestata -> periodicita valida
        // - dataUscitaRepository.findByPeriodicitaAndRange -> lista vuota
        // Verifica risposta MSG00001 e giorniSospesi=0.
        Integer idTestata = 1;
        DateRangeRequest request = request(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3));

        GdpPeriodicita periodicita = new GdpPeriodicita();
        periodicita.id = 10;

        when(periodicitaRepository.findActiveByTestata(idTestata)).thenReturn(periodicita);
        when(dataUscitaRepository.findByPeriodicitaAndRange(10, request.getDataInizio(), request.getDataFine()))
                .thenReturn(List.of());

        SospensioneResponse response = service.sospendi(idTestata, request);

        assertEquals(GdpMessage.F05_NO_RESULTS.getCodice(), response.getMessage());
        assertEquals(0, response.getGiorniSospesi());
    }

    @Test
    void sospendiLanciaErroreSeParametriMancanti() {
        // Caso validazione input: request incompleta (dataFine mancante).
        // Non usa repository: il controllo avviene all'ingresso del service.
        // Verifica IllegalArgumentException con messaggio atteso.
        DateRangeRequest request = new DateRangeRequest();
        request.setDataInizio(LocalDate.of(2026, 3, 8));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.sospendi(1, request));

        assertEquals("Missing required parameters", exception.getMessage());
    }

    @Test
    void sospendiLanciaErroreSeRangeNonValido() {
        // Caso validazione input: dataInizio successiva a dataFine.
        // Non usa repository: il controllo avviene prima delle query DB.
        // Verifica IllegalArgumentException con messaggio atteso.
        DateRangeRequest request = request(LocalDate.of(2026, 3, 22), LocalDate.of(2026, 3, 8));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.sospendi(1, request));

        assertEquals("Data inizio > data fine", exception.getMessage());
    }

    private static DateRangeRequest request(LocalDate dataInizio, LocalDate dataFine) {
        // Helper di test: crea una request minimale con intervallo date.
        DateRangeRequest request = new DateRangeRequest();
        request.setDataInizio(dataInizio);
        request.setDataFine(dataFine);
        return request;
    }
}
