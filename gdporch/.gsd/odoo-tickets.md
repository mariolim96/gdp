# Odoo Tickets for gdporch

Below is a comprehensive list of all potential Odoo tickets required to complete `gdporch`, broken down by functional flow. They are grouped into logical Epics following the data journey from scheduling to transmission.

---

## EPIC 1: Expected Scheduling & Acquisition (Phase 3)

_Focus: Calculating when to expect editions and capturing them from SFTP._

### Ticket: Implement Expected Date Calculation Logic (F01) [COMPLETED]

**Type**: Independent
**Description**: Write the complex `configDTEdizioneAttesa` (F01) service to calculate expected publishing dates based on periodic rules (e.g. `MENSILITA` values, `nGx` formats). Ensure records are idempotently persisted to `GDP_DATA_USCITA` table.
**Estimate**: 18h

### Ticket: Implement Date Suspension & Retrieval Logic (F05 & F18)[COMPLETED]

**Type**: Dependent
**Depends On**: F01
**Description**: Implement the `sospensioneEdizioneAttesa` (F05) service to allow manual suspension of expected editions, and `verifDateAttese` (F18) for retrieving the calendar of expected/suspended dates.
**Estimate**: 8h

### Ticket: Implement SFTP Infrastructure & Directory Generation (F02) [COMPLETED]

**Type**: Independent
**Description**: Build a reusable Apache MINA SSHD SFTP client wrapper. Implement the scheduled job `creaCartellaEdizioneAttesa` (F02) that polls tomorrow's expected dates and generates empty directories on the Publisher's SFTP.
**Implementation Note**: Switched from MINA to JSch and added `atmoz/sftp` Docker container for local testing.
**Estimate**: 18h

### Ticket: Implement Periodic Publishing Polling Job (F03) [COMPLETED]

**Type**: Dependent
**Depends On**: F01, F02
**Description**: Build the 15-minute polling service `checkEdizioneAttesa` (F03) scanning `/_flusso_regolare`. Detect transfer completion (size stability), record acquisition to `GDP_LOG`, and prepare the staging area in `/_tmp`.
**Estimate**: 16h

---

## EPIC 2: Validation & State Persistence (Phase 4)

_Focus: Processing acquired files and recording the permanent state in the database._

### Ticket: Implement Validation Rules for Periodic Flow (F04) [COMPLETED]

**Type**: Dependent
**Depends On**: F03
**Description**: Create the core PDF validation logic (F04) including multi-page splitting, format/naming verification, and basic TXT extraction. Implement date and front-page heuristics from PDF contents.
**Estimate**: 12h

### Ticket: Implement Edition Data Persistence Service (F08) [COMPLETED]

**Type**: Dependent
**Depends On**: F04
**Description**: Implement the transactional `insEdizione` (F08) service. It must create or update `GDP_EDIZIONE` and `GDP_PAGINA` records based on validated metadata. Calculate `DATA_PUBBLICAZIONE` depending on periodic rules.
**Estimate**: 16h

### Ticket: Implement Testate Lookup Queries (F16, F17) [COMPLETED]

**Type**: Independent
**Description**: Implement basic read operations: `getElencoTestate` (F16) for lists with filtering and `getTestata` (F17) for full details.
**Estimate**: 6h

---

## EPIC 3: Historical Delivery (Phase 5)

_Focus: Handling bulk historical loads from dedicated archivist folders._

### Ticket: Implement Historical Delivery Polling Job (F06) [COMPLETED]

**Type**: Independent
**Description**: Implement the nightly `checkConsegnaStorico` job (F06) traversing `/_flusso_saltuario`. Handle folder resolution, map against known testata names, and handle duplicate/unknown testata cases according to spec.
**Estimate**: 12h

### Ticket: Implement Validation Rules for Historical Flow (F07) [COMPLETED]

**Type**: Dependent
**Depends On**: F06
**Description**: Create historical edition validation (F07) to verify the structured folder names (`yyyymmdd`) and check for the presence of matching PDF, TXT, and optional TIF files.
**Estimate**: 10h

---

## EPIC 4: DAM Transmission Pipeline (Phase 6)

_Focus: Packaging assets and transmitting to the CSI DAM system._

### Ticket: Implement XML Schema Generation (F09) [DEVELOPING]

**Type**: Dependent
**Depends On**: F08
**Description**: Build the `creaXMLEdizione` service. Transform DB metadata into XML aligning with XSD `GdP-STD-04-V01-Validazione_metadati_flussoFTP.xsd`. Zip all generated assets (`.xml`, `.pdf`, `.txt`) into `/_dam` target folders.
**Estimate**: 18h

### Ticket: Implement DAM Courier Service and Uploads (F10) [DEVELOPING]

**Type**: Dependent
**Depends On**: F09
**Description**: Implement the `inviaEdizione` (F10) recurring daemon. Interrogate `GDP_CODA_CARICAMENTO`, send ZIP payloads to the RestClient integrated DAM endpoints (`POST /api/v2/imports`). Handle REST faults via the queue system.
**Estimate**: 14h

### Ticket: Implement Dam Cleanup Job (F19) [w8DOC]

**Type**: Dependent
**Depends On**: F10
**Description**: Implement the `pulisciEdizione` job to cleanup `/_tmp` and `/_dam` footprints on local disks and remote SFTP when conditions match confirmed DAM loads.
**Estimate**: 8h

---

## EPIC 5: Monitoring & Mail Endpoints (Phase 7)

_Focus: Exposing status and handling communication failures via BFF._

### Ticket: Build Acquisition Query APIs (F12, F13, F15) [DEVELOPING]

**Type**: Independent
**Description**: Implement REST services for dashboard data: `elencoAcquisizioni` (F12), `ricercaAcquisizioni` (F15), and `dettaglioAcquisizione` (F13).
**Estimate**: 12h

### Ticket: Build Active DAM Integration Check (F20) [DEVELOPING]

**Type**: Independent
**Description**: Expose `statoDAM` (F20) bridging to the DAM GET queue status RestClient to live-check processing conditions.
**Estimate**: 6h

### Ticket: Build Mail Generation & Transmission Operations (F14, F22) [DEVELOPING]

**Type**: Independent
**Description**: Implement `preparaMAIL` (F14) for template assembly and `invioMAIL` (F22) for the actual SMTP transaction.
**Estimate**: 10h

### Ticket: Build Quarantine Retry Enabler (F21) [DEVELOPING]

**Type**: Dependent
**Depends On**: F10
**Description**: Write the manual resubmission backend api `attivaCODA` (F21) to reset DAM load failures inside the upload queue.
**Estimate**: 4h

---

## EPIC 6: Observability & Infra

_Focus: Deployment and monitoring readiness._

### Ticket: Implement System Observability Stack

**Type**: Independent
**Description**: Quarkus `/q/health` probes (SFTP connectivity) and `/q/metrics` mapping success volumes (Micrometer).
**Estimate**: 10h

### Ticket: K8s Infrastructure Configurations

**Type**: Independent
**Description**: Document and write `docker-compose.yml` and K8s ConfigMaps validating Quarkus configurations.
**Estimate**: 8h

---

## EPIC 7: Quality Assurance & Automated Testing (Phase 8)


### Ticket: Implement Shared Test Infrastructure & Mocking Suite

**Type**: Independent
**Description**: Set up the core test environment including Testcontainers for PostgreSQL, WireMock for DAM LIBRA API, and the Testcontainers `atmoz/sftp` Docker container for SFTP mocking. Implement `TestPdfFactory` for generating valid/corrupt PDF/A files for validation testing.
**Estimate**: 12h

### Ticket: Implement Unit Test Suite for Core Algorithms (L1)

**Type**: Independent
**Description**: Implement pure unit tests (L1) without external dependencies for core algorithms. Focus on date calculation logic (F01 configDTEdizioneAttesa), naming convention checks, regex validations, and basic data transformations.
**Estimate**: 16h

### Ticket: Implement Integration Test Suite for Periodic Flow (L2)

**Type**: Dependent
**Depends On**: EPIC 1, EPIC 2, EPIC 4, EPIC 7 (Shared Infra)
**Description**: Implement integration tests for the primary periodic pipeline (F01-F04, F08-F10). Cover nominal scenarios, date heuristics, multipage splits, and DAM upload queueing as defined in the testbook.
**Estimate**: 24h

### Ticket: Implement Integration Test Suite for Historical Flow (L2)

**Type**: Dependent
**Depends On**: EPIC 3, EPIC 7 (Shared Infra)
**Description**: Implement integration tests for the historical delivery process (F06, F07). Verify directory traversal, metadata extraction from structured folders, and priority handling for historical vs periodic editions.
**Estimate**: 12h

### Ticket: Implement Integration Test Suite for Monitoring & Management APIs (L2)

**Type**: Dependent
**Depends On**: EPIC 5, EPIC 7 (Shared Infra)
**Description**: Implement integration tests for REST APIs (F12-F15, F16-F17, F18, F19, F20-F22) using RestAssured. Verify status lookups, queue reactivation (F21), date suspensions (F05), and mail assembly (F14/F22).
**Estimate**: 16h

### Ticket: Implement Orchestration and Scheduling Tests (L2)

**Type**: Dependent
**Depends On**: EPIC 7 (Shared Infra)
**Description**: Implement integration tests for Quartz schedulers (TB-SCHEDULING) and workflow orchestration (TB-ORCHESTRATORE, TB-MONITOR-WORKFLOW). Verify execution sequences, state propagation across components, and scheduler firing metrics.
**Estimate**: 16h

### Ticket: Implement End-to-End (E2E) Regression Suite (L3)

**Type**: Dependent
**Depends On**: EPIC 7 (Shared Infra)
**Description**: Implement the L3 E2E tests for the full data lifecycle: from SFTP deposit to DAM submission. Cover nominal paths for both Periodic and Historical flows, ensuring cross-component state consistency.
**Estimate**: 16h

### Ticket: Implement Resilience, Concurrency & Observability Tests

**Type**: Dependent
**Depends On**: EPIC 6 (Observability)
**Description**: Implement tests for critical edge cases: race conditions during polling (TB-CONCORRENZA), circuit breaker behavior (SFTP/DAM downtime), recovery after crash (TB-RESILIENZA), and health/metrics endpoint accuracy (TB-OSSERVABILITA).
**Estimate**: 14h

