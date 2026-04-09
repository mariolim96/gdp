package it.csipiemonte.gdp.gdporch.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.csipiemonte.gdp.gdporch.dto.EdizioneInsertResponse;
import it.csipiemonte.gdp.gdporch.dto.XmlCreationResponse;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLogEdizione;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogEdizioneRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogRepository;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
public class GdpCtrlEdizioniStoricheServiceTest {

    @Inject
    GdpCtrlEdizioniStoricheService service;

    @InjectMock
    GdpEdizioneService edizioneService;

    @InjectMock
    DamTrasmissioneService damTrasmissioneService;

    @InjectMock
    GdpLogRepository logRepository;

    @InjectMock
    GdpLogEdizioneRepository logEdizioneRepository;

    // Iniettiamo i path dal profilo %test (target/test-sftp/...)
    @ConfigProperty(name = "sftp.root.prefix.tmp")
    String tmpPrefix;

    @ConfigProperty(name = "sftp.root.prefix.errata")
    String errataPrefix;

    @BeforeEach
    void cleanup() throws IOException {
        // Pulizia cartelle di test prima di ogni esecuzione
        deleteDirectory(Paths.get(tmpPrefix));
        deleteDirectory(Paths.get(errataPrefix));
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {}
                    });
        }
    }

        @Test
        @Disabled("Rimuovi se hai decommentato F09/F10 nel service")
    void testScenarioSuccesso() throws Exception {
        String dataC = "20260408";
        String testata = "TESTATA";

        // 1. Creazione struttura cartelle basata su ConfigProperty
        Path tempDir = Paths.get(tmpPrefix, "CONS_" + dataC, testata, dataC);
        Files.createDirectories(tempDir);

        // 2. Creazione file validi (PDF, TXT, TIF obbligatorio)
        Files.write(tempDir.resolve(testata + "-TO-" + dataC + "_001.pdf"), new byte[0]);
        Files.write(tempDir.resolve(testata + "-TO-" + dataC + "_001.txt"), new byte[0]);
        Files.write(tempDir.resolve(testata + "-TO-" + dataC + "_001.tif"), new byte[0]);

        // 3. Mock Risposte Servizi
        EdizioneInsertResponse res08 = new EdizioneInsertResponse();
        res08.setIdEdizione(1001);
        when(edizioneService.insEdizione(any(), any(), any(), any())).thenReturn(res08);

        // Mock F09 (Attenzione: il codice deve corrispondere a GdpMessage.F_OK)
        XmlCreationResponse res09 = new XmlCreationResponse();
        res09.setCodice(it.csipiemonte.gdp.gdporch.exception.GdpMessage.F_OK.getCodice()); // Esempio di codice OK
        res09.setNomeFileCompresso("test_storico.zip");
        when(damTrasmissioneService.creaXMLEdizione(any(), any(), any(), any())).thenReturn(res09);

        // Mock Repository (necessari per evitare NullPointerException o errori DB)
        when(logRepository.findById(any())).thenReturn(new it.csipiemonte.gdp.gdporch.model.entity.GdpLog());

        // 4. Esecuzione
        service.ctrlEdizioniStoriche(1, testata, dataC, 123);

        // 5. Verifica: Se F09/F10 sono attivi nel Service, verifica la chiamata
         verify(damTrasmissioneService, times(1)).inviaEdizioneAsync(eq(123), eq(1001), anyString());

        // Verifica che il log dell'edizione sia stato salvato
        verify(logEdizioneRepository, atLeastOnce()).persist((GdpLogEdizione) any());
    }

    @Test
    @Disabled
    void testScenarioTifMancante() throws Exception {
        String dataC = "20260408";
        String testata = "TESTATA";

        Path tempDir = Paths.get(tmpPrefix, "CONS_" + dataC, testata, dataC);
        Files.createDirectories(tempDir);

        // Solo PDF, niente TIF
        Files.write(tempDir.resolve(testata + "-TO-" + dataC + "_001.pdf"), new byte[0]);

        service.ctrlEdizioniStoriche(1, testata, dataC, 123);

        // Verifica che l'edizione sia stata considerata anomala (TipoEdizione.AS)
        verify(logEdizioneRepository).persist(argThat((GdpLogEdizione log) ->
                log.tipoEdizione.name().equals("AS") && log.descrizione.contains("TIF mancante")
        ));
    }
}