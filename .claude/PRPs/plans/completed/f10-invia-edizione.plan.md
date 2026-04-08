# Feature: F10 DAMtrasmissione.inviaEdizione

## Summary
Implement the F10 automated scheduled job that picks up "READY" items from the import task queue (`GDP_CODA_CARICAMENTO`), downloads the corresponding `.zip` file from the SFTP `_dam` directory, and transmits it via HTTP POST (multipart) to the external DAM LIBRA API. Upon completion, the job updates the internal logging tables and removes the `.zip` file from the staging area if successful.

## User Story
As the gdporch orchestrator
I want to periodically transmit queued edition ZIP files to the DAM LIBRA archive
So that the front-end archive applications can expose the documents and I can track the upload results.

## Problem Statement
After an edition's metadata (F08) and XML/ZIP package (F09) have been successfully generated and staged, there is currently no systematic transmission process that officially uploads the package to DAM LIBRA and manages the external service's response (`jobId`, `status`) to close the internal workflow execution cycle.

## Solution Statement
We will introduce a Quarkus Scheduled Job (`InviaEdizioneJob`) that runs every 30 minutes. It will query the `GdpCodaCaricamento` entity (aka `GDP_IMPORT_TASK`) for records in the `READY` state. For each record, it will use the existing `SftpSession` infrastructure to download the ZIP file locally, transmit it to DAM LIBRA using a new Quarkus Reactive REST Client (`LibraClient`), and update the Database logs depending on whether the response status is `SUBMITTED` or `FAILED`.

## Metadata

| Field            | Value                                             |
| ---------------- | ------------------------------------------------- |
| Type             | NEW_CAPABILITY                                    |
| Complexity       | MEDIUM                                            |
| Systems Affected | gdporch (Scheduler, Rest Client, DB Logs)         |
| Dependencies     | ts-libra-sv-exp1.csi.it (DAM API)                 |
| Estimated Tasks  | 6                                                 |

---

## UX Design

### Before State
```text
╔═══════════════════════════════════════════════════════════════════════════════╗
║                              BEFORE STATE                                     ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║                                                                               ║
║   [GDP_CODA_CARICAMENTO]                                                      ║
║        STATO = 'READY'                                                        ║
║        SFTP_PATH = '/_dam/foo.zip'                                            ║
║                                                                               ║
║   USER_FLOW: Zip packages sit in SFTP `_dam` folder indefinitely.            ║
║   DATA_FLOW: Stopped at F09 (creaXML). No DAM integration exists.             ║
║                                                                               ║
╚═══════════════════════════════════════════════════════════════════════════════╝
```

### After State
```text
╔═══════════════════════════════════════════════════════════════════════════════╗
║                               AFTER STATE                                     ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║                                                                               ║
║   [GDP_CODA_CARICAMENTO] ─► [InviaEdizioneJob] ─► [DAM LIBRA API]            ║
║                                   │                                           ║
║                                   ▼                                           ║
║                           [UPDATE LOGS & CLEANUP]                             ║
║                                                                               ║
║   USER_FLOW: Packages are automatically dispatched every 30m.                 ║
║   VALUE_ADD: Closes the acquisition loop and stores DAM Job IDs.              ║
║   DATA_FLOW: DB -> SFTP download -> HTTP POST upload -> DB Update -> Cleanup  ║
║                                                                               ║
╚═══════════════════════════════════════════════════════════════════════════════╝
```

### Interaction Changes
| Location | Before | After | User Impact |
|----------|--------|-------|-------------|
| `GDP_CODA_CARICAMENTO` | Remains stuck in READY | Moves to SUBMITTED or FAILED | Admin can monitor progress |
| `GDP_LOG_EDIZIONE` | Incomplete state | Populated with `JOB_ID` and new status | Reliable tracking |
| SFTP `_dam` folder | Accumulates files | Cleaned up exactly upon successful DAM upload | Saves storage / Avoids clutter |

---

## Mandatory Reading

| Priority | File | Lines | Why Read This |
|----------|------|-------|---------------|
| P0 | `project-documentation/gdporch-spec.md` | 830-870 | Core specification for F10 behavior and exact DB fields to update |
| P1 | `gdporch/src/main/java/it/csipiemonte/gdp/gdporch/service/CreaCartellaEdizioneAttesaJob.java` | 60-70 | Pattern for SFTP Connection and Channel handling (`sftpClientProducer`) |
| P2 | `gdporch/src/main/resources/application.properties` | 28-30 | Existent properties map to `libra.api.url` |

---

## Patterns to Mirror

**SFTP_CONNECTION_PATTERN:**
```java
try (SftpSession sftpSession = sftpClientProducer.connect()) {
    ChannelSftp channel = sftpSession.getChannel();
    // Do SFTP operations
} catch (Exception e) {
    LOG.error("Errore SFTP", e);
}
```

**SCHEDULER_PATTERN:**
```java
@ApplicationScoped
public class InviaEdizioneJob {
    @Scheduled(every = "30m", identity = "F10-inviaEdizione")
    @Transactional
    public void execute() { ... }
}
```

---

## Files to Change

| File                             | Action | Justification                            |
| -------------------------------- | ------ | ---------------------------------------- |
| `src/main/java/.../client/LibraClient.java` | CREATE | Quarkus Rest Client interface for DAM API  |
| `src/main/java/.../dto/LibraImportResponse.java` | CREATE | DTO for `jobId` and `status` response mapping|
| `src/main/java/.../service/InviaEdizioneJob.java` | CREATE | Scheduled business logic orchestrating F10 |
| `src/main/resources/application.properties` | UPDATE | Map the REST client to `libra-api` and fix typo |

---

## NOT Building (Scope Limits)

- **F19 (pulisciEdizione)**: The plan only covers successful cleanup from the `_dam` folder at the exact time of successful upload. Generic 24-hour delayed cleanup logic (F19) is out of scope per documentation "To BE DO".
- **DAM Authentication Token Gen (UC-01)**: Marked as out of scope for F10 in spec lines 1273. For testing, an API token injected from `application.properties` or Mock will be used.

---

## Step-by-Step Tasks

### Task 1: CREATE LibraImportResponse DTO
- **ACTION**: Define the expected JSON response from DAM
- **IMPLEMENT**: Class with `String jobId` and `String status`.
- **VALIDATE**: `mvn compile`

### Task 2: CREATE LibraClient Interface
- **ACTION**: Define the Quarkus Reactive REST client 
- **IMPLEMENT**: Interface `LibraClient` with `@RegisterRestClient(configKey = "libra-api")`
- **METHOD**: `POST /api/v2/imports` accepting a Multipart form (e.g. `java.io.File` using `@RestForm "file"`) and returning `LibraImportResponse`.
- **VALIDATE**: `mvn compile`

### Task 3: UPDATE application.properties
- **ACTION**: Fix existing REST client config keys
- **IMPLEMENT**: Change `quarkus.rest-client."it.csipiemonte.gdp.gdporch.client.LibreClient".url` to `quarkus.rest-client.libra-api.url`. Add `libra.api.token=${LIBRA_TOKEN}` if needed for headers.
- **VALIDATE**: `mvn compile`

### Task 4: CREATE InviaEdizioneJob Logic Skeleton
- **ACTION**: Define `InviaEdizioneJob` class 
- **IMPLEMENT**: Inject repositories (`GdpCodaCaricamentoRepository`, `GdpLogEdizioneRepository`, `GdpLogRepository`). `@Scheduled` for every 30m. Read `GdpCodaCaricamento` where `STATO='READY'` ordered by `priorita` and `dt_inserim_in_coda`.
- **VALIDATE**: logic bounds checked

### Task 5: CREATE SFTP Download and API Upload Loop
- **ACTION**: Add the detailed file-handling process mapping to job logic
- **IMPLEMENT**: 
    1. Iterate active queues.
    2. Using `SftpSession`, retrieve file from `task.sftpPath` to a temporary `java.io.File`.
    3. Call `libraClient.uploadZip(tempFile)`.
    4. Parse response.
- **GOTCHA**: Ensure the temporary file is always deleted in a `finally` block to prevent disk space leaks in the orchestration container.

### Task 6: CREATE Database Update & SFTP Cleanup
- **ACTION**: Finalize state mutations based on API response
- **IMPLEMENT**:
    - If `SUBMITTED`: Delete from `sftpSession` channel using `.rm()`. Update `GdpLogEdizione` and `GdpLog` with `MSG00009`. Set task to `SUBMITTED`.
    - If `FAILED`: Keep file. Update logs with `MSG00001`. Set task to `FAILED`.
- **VALIDATE**: Code compiles perfectly `mvn compile`.

---

## Testing Strategy
| Test File                                | Test Cases                 | Validates      |
| ---------------------------------------- | -------------------------- | -------------- |
| `InviaEdizioneJobTest.java`              | No READY tasks             | Returns early  |
| `InviaEdizioneJobTest.java`              | Mocked FAILED response     | DB state FAILED|
| `InviaEdizioneJobTest.java`              | Mocked SUBMITTED response  | DB OK, SFTP rm |
