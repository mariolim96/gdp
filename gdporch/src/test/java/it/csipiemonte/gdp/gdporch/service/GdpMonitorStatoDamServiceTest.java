package it.csipiemonte.gdp.gdporch.service;

import it.csipiemonte.gdp.gdporch.dto.DamStatusResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class GdpMonitorStatoDamServiceTest {

    @Inject
    GdpMonitorStatoDamService service;

    @InjectMock
    @RestClient
    GdporchRestClient mockClient;

    @Test
    public void testRecuperaStatoDam_Success() {
        // Setup del mock
        DamStatusResponse mockResponse = new DamStatusResponse();
        mockResponse.setJobId("123");
        mockResponse.setStatus(DamStatusResponse.StatusEnum.COMPLETED);
        Mockito.when(mockClient.getStatoLibra("123")).thenReturn(mockResponse);

        // Esecuzione
        String risultato = service.recuperaStatoDam(123L);

        // Verifica
        Assertions.assertEquals("MSG00009 Stato edizione COMPLETED", risultato);
    }

    @Test
    public void testRecuperaStatoDam_Error() {
        // Simulo un errore del client
        Mockito.when(mockClient.getStatoLibra("999")).thenThrow(new RuntimeException("Connection error"));

        String risultato = service.recuperaStatoDam(999L);

        Assertions.assertEquals("MSG00001 Dato non trovato", risultato);
    }

    @Test
    public void testRecuperaStatoDam_NullResponse() {
        // Caso limite: il server risponde 200 ma il body è vuoto
        Mockito.when(mockClient.getStatoLibra("000")).thenReturn(null);

        String risultato = service.recuperaStatoDam(0L);

        // Deve gestire il null restituendo il messaggio di errore
        Assertions.assertEquals("MSG00001 Dato non trovato", risultato);
    }
}