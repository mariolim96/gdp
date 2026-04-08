package it.csipiemonte.gdp.gdporch.service;


import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock; // <--- CORRETTO
import it.csipiemonte.gdp.gdporch.dto.EdizioneInsertResponse;
import it.csipiemonte.gdp.gdporch.dto.XmlCreationResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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


    @BeforeEach
    void cleanup() throws IOException {
        Path tmpDir = Paths.get("_tmp");
        if (Files.exists(tmpDir)) {
            // Cammina nella cartella e cancella tutto ciò che trova
            Files.walk(tmpDir)
                    .sorted((a, b) -> b.compareTo(a)) // Ordina al contrario per cancellare prima i file e poi le cartelle
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            // Ignora se non riesce a cancellare qualcosa (es. file bloccati)
                        }
                    });
        }
    }

    @Test
    @Disabled("Disabilitato in attesa di riattivazione logica F08-F10 nel Service")
    void testScenarioSuccesso() throws Exception {
        // 1. Definiamo il path
        Path tempDir = Paths.get("_tmp", "CONS_20260408", "TESTATA", "20260408");

        // 2. CREIAMO LE CARTELLE (Mancava questa riga!)
        Files.createDirectories(tempDir);

        // 3. CREIAMO IL FILE (Usiamo write che sovrascrive senza lanciare eccezioni)
        Path fileTif = tempDir.resolve("TESTATA-TO-20260408_001.tif");
        Files.write(fileTif, new byte[0]);

        // --- DA QUI IN POI IL CODICE È CORRETTO ---

        // 1. Mock F08
        EdizioneInsertResponse res08 = new EdizioneInsertResponse();
        res08.setIdEdizione(1001);
        when(edizioneService.insEdizione(any(), any(), any(), any())).thenReturn(res08);

        // 2. Mock F09
        XmlCreationResponse res09 = new XmlCreationResponse();
        res09.setCodice("MSG00009");
        res09.setNomeFileCompresso("test_storico.zip");
        when(damTrasmissioneService.creaXMLEdizione(any(), any(), any(), any())).thenReturn(res09);

        // 3. Esecuzione
        service.ctrlEdizioniStoriche(1, "TESTATA", "20260408", 123);

        // 4. Verifica
        verify(damTrasmissioneService, times(1)).inviaEdizioneAsync(eq(123), eq(1001), eq("test_storico.zip"));
    }

    @Test
    @Disabled("Disabilitato in attesa di riattivazione logica F08-F10 nel Service")
    void testScenarioErroreF09() {
        // 1. Mock F08: OK
        EdizioneInsertResponse res08 = new EdizioneInsertResponse();
        res08.setIdEdizione(1001);
        when(edizioneService.insEdizione(any(), any(), any(), any())).thenReturn(res08);

        // 2. Mock F09: Errore (es. MSG00004)
        XmlCreationResponse res09KO = new XmlCreationResponse();
        res09KO.setCodice("MSG00004");
        res09KO.setMessaggio("Errore validazione DAM");
        when(damTrasmissioneService.creaXMLEdizione(any(), any(), any(), any())).thenReturn(res09KO);

        // 3. Esecuzione
        service.ctrlEdizioniStoriche(1, "TESTATA", "20260408", 123);

        // 4. Verifica: NON deve aver chiamato l'invio asincrono (F10)
        verify(damTrasmissioneService, never()).inviaEdizioneAsync(any(), any(), any());

    }
}