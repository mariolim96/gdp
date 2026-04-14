package it.csipiemonte.gdp.gdporch;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@QuarkusTest
class InfrastructureSmokeTest extends GdporchIntegrationTest {

    @Test
    void testDatabaseConnectivity() {
        // Init.sql contains 8 provinces
        Object result = entityManager.createNativeQuery("SELECT COUNT(*) FROM GDP_PROVINCE").getSingleResult();
        Assertions.assertTrue(((Number) result).longValue() >= 8);
    }

    @Test
    @Transactional
    void testTestDataSeeding() throws IOException {
        entityManager.createNativeQuery("INSERT INTO GDP_TESTATA (ID_GDP_TESTATA, NOME_TESTATA, CARTELLA_TESTATA, INVIO_EDIZIONE, STATO, COD_TEMA, PROVINCIA) " +
                "VALUES (1001, 'Testata Smoke', 'smoke', true, 0, 1, 'TO') ON CONFLICT DO NOTHING").executeUpdate();
        
        Object count = entityManager.createNativeQuery("SELECT COUNT(*) FROM GDP_TESTATA WHERE ID_GDP_TESTATA = 1001").getSingleResult();
        Assertions.assertEquals(1L, ((Number) count).longValue());
    }

    @Test
    void testSftpMock() throws IOException, InterruptedException {
        String testFile = "smoke_test.pdf";
        sftpServer.depositaPdf("smoke", "2026-04-13", testFile);
        
        if (sftpServer.isLocalMode()) {
            // Verify on host filesystem
            Path path = Path.of("../sftp-data/flusso_regolare/smoke/2026-04-13", testFile);
            Assertions.assertTrue(Files.exists(path), "File should exist in local sftp-data folder");
        } else {
            // Verify in container
            var execResult = sftpServer.getContainer().execInContainer("ls", "/home/tester/flusso_regolare/smoke/2026-04-13/" + testFile);
            Assertions.assertEquals(0, execResult.getExitCode());
        }
    }

    @Test
    void testWireMock() {
        Assertions.assertNotNull(libraMock);
        Assertions.assertTrue(libraMock.isRunning());
    }
}
