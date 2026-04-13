package it.csipiemonte.gdp.gdporch;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class GdporchIntegrationTest {

    protected static PostgreSQLContainer<?> postgres;
    protected static SftpTestServer sftpServer;
    protected static WireMockServer libraMock;

    @Inject
    protected EntityManager entityManager;

    @BeforeAll
    static void setupInfra() {
        // --- 1. Detect Existing Infrastructure ---
        boolean dbRunning = isPortOccupied(5432);
        boolean sftpRunning = isPortOccupied(2222);

        System.out.println(">>> Infrastructure Check: DB Running=" + dbRunning + ", SFTP Running=" + sftpRunning);

        // --- 2. Database Setup ---
        if (dbRunning) {
            System.out.println(">>> Using existing PostgreSQL infrastructure on localhost:5432");
            System.setProperty("quarkus.datasource.jdbc.url", "jdbc:postgresql://localhost:5432/gdp_db");
            System.setProperty("quarkus.datasource.username", "gdp_user");
            System.setProperty("quarkus.datasource.password", "gdp_password");
        } else {
            System.out.println(">>> No local DB found. Starting Testcontainers PostgreSQL...");
            // Pinning API version to 1.44 for better Windows compatibility
            System.setProperty("DOCKER_API_VERSION", "1.44");
            postgres = new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("gdp_db")
                    .withUsername("gdp_user")
                    .withPassword("gdp_password")
                    .withInitScript("db/init.sql");
            postgres.start();
            System.setProperty("quarkus.datasource.jdbc.url", postgres.getJdbcUrl());
            System.setProperty("quarkus.datasource.username", postgres.getUsername());
            System.setProperty("quarkus.datasource.password", postgres.getPassword());
        }

        // --- 3. SFTP Setup ---
        if (sftpRunning) {
            System.out.println(">>> Using existing SFTP infrastructure on localhost:2222");
            sftpServer = new SftpTestServer(true); // Enable Local Mode
        } else {
            System.out.println(">>> No local SFTP found. Starting Testcontainers SFTP...");
            sftpServer = new SftpTestServer(false);
            sftpServer.start();
        }
        
        System.setProperty("sftp.host", sftpServer.getHost());
        System.setProperty("sftp.port", String.valueOf(sftpServer.getPort()));
        System.setProperty("sftp.username", "tester");
        System.setProperty("sftp.password", "password");

        // --- 4. WireMock Setup (Always Inline) ---
        libraMock = new WireMockServer(wireMockConfig().dynamicPort());
        libraMock.start();
        System.setProperty("libra.api.url", libraMock.baseUrl());
        System.setProperty("quarkus.rest-client.\"it.csipiemonte.gdp.gdporch.client.LibreClient\".url", libraMock.baseUrl());
    }

    @AfterAll
    static void tearDownInfra() {
        if (postgres != null) {
            postgres.stop();
        }
        if (sftpServer != null) {
            sftpServer.stop();
        }
        if (libraMock != null) {
            libraMock.stop();
        }
    }

    private static boolean isPortOccupied(int port) {
        try (Socket socket = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @BeforeEach
    void resetWireMock() {
        if (libraMock != null) {
            libraMock.resetAll();
        }
    }

    protected void awaitDbRecord(String table, String condition, Duration timeout) {
        await().atMost(timeout).until(() -> {
            Object result = entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM " + table + " WHERE " + condition
            ).getSingleResult();
            return ((Number) result).longValue() >= 1L;
        });
    }
}
