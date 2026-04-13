# Gdporch Testing Guide

This document describes how to use the shared test infrastructure for the `gdporch` service. Our testing strategy combines high-performance unit tests with resilient, hybrid integration tests.

## 1. Core Testing Classes

All testing utilities are located in `src/test/java/it/csipiemonte/gdp/gdporch/`.

### 🧪 `GdporchIntegrationTest`
An abstract base class for all integration tests. It handles the lifecycle of the database, SFTP mock, and external API mocks (WireMock).

**Hybrid Mode Features:**
- **Auto-Detection**: It pings ports `5432` (Postgres) and `2222` (SFTP). If your local Docker containers (from `docker-compose`) are running, it connects to them directly.
- **Testcontainers Fallback**: If local services are not found (e.g., in CI/CD), it automatically starts isolated Docker containers.
- **WireMock**: Always starts an internal WireMock server for mocking the DAM LIBRA API.

### 📁 `SftpTestServer`
A wrapper around the SFTP mock. 
- In **Local Mode** (connected to your running Docker), it writes files directly to the host's `./sftp-data` folder.
- In **Container Mode**, it uses standard Testcontainers commands.
- **Usage**: `sftpServer.depositaPdf(cartella, data, filename)` will place a valid PDF in the correct path for the acquisition flow.

### 📄 `TestPdfFactory`
A utility powered by **PDFBox 3.x** to generate various types of PDF/A files for testing:
- `singlePage(filename)`: Standard valid PDF.
- `multiPage(filename, pages)`: For testing bulk acquisitions.
- `corrupted()`: Returns a byte array of a broken PDF file.
- `passwordProtected()`: For testing validation failures.

---

## 2. Writing an Integration Test

To write a new integration test, extend `GdporchIntegrationTest`. You will have access to the `entityManager`, `sftpServer`, and `libraMock`.

```java
@QuarkusTest
class MyFeatureIntegrationTest extends GdporchIntegrationTest {

    @Test
    void testFullFlow() throws IOException {
        // 1. Mock external API response
        libraMock.stubFor(post(urlEqualTo("/dam/upload"))
                .willReturn(okJson("{\"status\":\"SUCCESS\"}")));

        // 2. Deposit a test file in SFTP
        sftpServer.depositaPdf("mytestata", "2026-04-13", "edition.pdf");

        // 3. Trigger your service/endpoint
        // ... (e.g., call a REST endpoint or a Job)

        // 4. Verify results in DB
        awaitDbRecord("GDP_LOG_EDIZIONE", "NRO_PAG_ACQUISITE = 1", Duration.ofSeconds(10));
    }
}
```

---

## 3. Writing a Unit Test

For unit tests, use standard Quarkus `@QuarkusTest` with `@InjectMock` to isolate the business logic from the database and external services.

```java
@QuarkusTest
class MyServiceUnitTest {

    @InjectMock
    MyRepository repository;

    @Inject
    MyService service;

    @Test
    void testLogic() {
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(new MyEntity()));
        // ... verify logic
    }
}
```

---

## 4. Local Environment Setup

To get the most out of the **Hybrid Mode** (recommended for speed), follow these steps:

1.  **Start Docker**:
    ```bash
    docker-compose up -d
    ```
2.  **Enable TCP (Windows only)**: 
    In Docker Desktop Settings -> General, ensure **"Expose daemon on tcp://localhost:2375 without TLS"** is checked. This ensures Testcontainers behaves correctly even if we don't use it directly.
3.  **Run Tests**:
    ```bash
    mvn test -Dtest=InfrastructureSmokeTest
    ```

## 5. Summary Checklist

- [ ] Does your test extend `GdporchIntegrationTest`?
- [ ] Are you using `sftpServer.depositaPdf()` to simulate incoming files?
- [ ] Are you using `libraMock.stubFor()` to simulate DAM API responses?
- [ ] Are you checking the database state using `awaitDbRecord()`?
