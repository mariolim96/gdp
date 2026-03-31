# Odoo Tickets for gdporch

Below is a comprehensive list of all potential Odoo tickets required to complete `gdporch`, broken down by Roadmap phases. They are grouped into logical Epics and sized to be actionable tasks.

---

## EPIC 1: Configuration & Validation Services (Phase 3)

### Ticket: Implement Validation Rules for Periodic Flow (F04)
**Type**: Independent
**Description**: Create the core PDF validation logic (F04) including multi-page splitting, format/naming verification (`_numeroPagina.pdf`), and basic TXT extraction. Integrate `pdfbox` or CLI equivalents to check readability. Implement date and front-page heuristics. Handle Anomalous ("AA") classifications.
**Estimate**: 12h

### Ticket: Implement Validation Rules for Historical Flow (F07)
**Type**: Independent
**Description**: Create historical edition validation (F07) to verify the structured folder names (`yyyymmdd`) and check for presence of matching PDF, TXT, and optional TIF files. Handle Anomaly classifications ("AS").
**Estimate**: 10h

### Ticket: Implement Expected Date Calculation Logic (F01)
**Type**: Independent
**Description**: Write the complex `configDTEdizioneAttesa` (F01) service to calculate expected publishing dates based on periodic rules (e.g. `MENSILITA` values, `nGx` formats). Ensure records are idempotently persisted to `GDP_DATA_USCITA` table.
**Estimate**: 18h

### Ticket: Implement Date Suspension & Retrieval Logic (F05 & F18)
**Type**: Dependent
**Depends On**: F01
**Description**: Implement the `sospensioneEdizioneAttesa` (F05) service to suspend automatically expected editions, and `verifDateAttese` (F18) to retrieve dates with suspended statuses.
**Estimate**: 8h

---

## EPIC 2: Database Persistence & Queries (Phase 4)

### Ticket: Implement Edition Data Persistence Service (F08)
**Type**: Dependent
**Depends On**: F04, F07
**Description**: Implement the transactional `insEdizione` (F08) service using Panache Repositories. It must perform `INSERT` for new tracking instances or `UPDATE` for existing ones, while populating related `GDP_PAGINA` entries for all analyzed PDFs. Needs logic for calculating `DATA_PUBBLICAZIONE` depending on periodic rules.
**Estimate**: 16h

### Ticket: Implement Testate Lookup Queries (F16, F17)
**Type**: Independent
**Description**: Implement basic read operations: `getElencoTestate` (F16) for lists with dynamic filtering and `getTestata` (F17) for retrieving full testata details out of the database.
**Estimate**: 6h

---

## EPIC 3: SFTP Integration & Polling Jobs (Phase 5)

### Ticket: Implement SFTP Connections and Directory Scaffolding (Infrastructure)
**Type**: Independent
**Description**: Build a reusable Apache MINA SSHD SFTP client wrapper taking configurations securely via the `application.properties`/env vars. Must support asymmetric key auth and manage connections actively. 
**Estimate**: 12h

### Ticket: Implement Pre-Polling Directory Generation (F02)
**Type**: Dependent
**Depends On**: F01, SFTP Infrastructure
**Description**: Implement the scheduled `@Scheduled` cron job `creaCartellaEdizioneAttesa` (F02) that polls tomorrow's expected dates from the DB and generates empty directories on the Publisher's SFTP.
**Estimate**: 6h

### Ticket: Implement Periodic Publishing Polling Job (F03)
**Type**: Dependent
**Depends On**: SFTP Infrastructure, F04
**Description**: Build the 15-minute polling service `checkEdizioneAttesa` (F03) examining `/_flusso_regolare`. Needs to detect transfer completion stably (waiting for file lengths to stop expanding), parse folder mappings, record to `GDP_LOG`, and submit valid ones to `F04` asynchronously.
**Estimate**: 16h

### Ticket: Implement Historical Delivery Polling Job (F06)
**Type**: Dependent
**Depends On**: SFTP Infrastructure, F07
**Description**: Implement the `@Scheduled` nightly `checkConsegnaStorico` job (F06) traversing the `/_flusso_saltuario` trees. Handle multi-layered folder resolution, map against testate and handle duplicates explicitly. Forward to validation `F07`.
**Estimate**: 12h

---

## EPIC 4: DAM Transmission Pipeline (Phase 6)

### Ticket: Implement XML Schema Generation (F09)
**Type**: Dependent
**Depends On**: F08 (DB State)
**Description**: Build the `creaXMLEdizione` service. Transform DB metadata into XML aligning strictly with XSD `GdP-STD-04-V01-Validazione_metadati_flussoFTP.xsd`. Zip all generated assets (`.xml`, `.pdf`, `.txt`) into `/_dam` target folders. Track state inside `GDP_IMPORT_TASK`.
**Estimate**: 18h

### Ticket: Implement DAM Courier Service and Uploads (F10)
**Type**: Dependent
**Depends On**: F09
**Description**: Implement the `inviaEdizione` (F10) recurring daemon. Interrogate `GDP_CODA_CARICAMENTO` prioritizing historic loads vs periodic ones, send ZIP payloads to the RestClient integrated DAM endpoints (`POST /api/v2/imports`). Handle REST faults elegantly via the queue system records `STATO`.
**Estimate**: 14h

### Ticket: Implement Dam Cleanup Job (F19)
**Type**: Dependent
**Depends On**: F10
**Description**: Implement the `pulisciEdizione` job to cleanup `/_tmp` and `/_dam` footprints on local disks and remote SFTP when conditions match confirmed DAM loads.
**Estimate**: 8h

---

## EPIC 5: Monitoring API endpoints (Phase 7)

### Ticket: Build Acquisition Query APIs (F12, F13, F15)
**Type**: Independent (DB Read)
**Description**: Implement REST services for dashboard SPA data pulls. Develop `elencoAcquisizioni` (F12) for top-level logs, `ricercaAcquisizioni` (F15) for deeply filtered metrics, and `dettaglioAcquisizione` (F13) exposing raw validation results for single payloads.
**Estimate**: 12h

### Ticket: Build Active DAM Integration Check (F20)
**Type**: Independent (External API Read)
**Description**: Expose the endpoint `statoDAM` (F20) directly bridging to the DAM GET queue status RestClient to live-check processing conditions on the remote end.
**Estimate**: 6h

### Ticket: Build Mail Generation & Transmission Operations (F14, F22)
**Type**: Independent (Infrastructure dependencies)
**Description**: Implement the email template assembly logic `preparaMAIL` (F14) replacing wildcard string values with DB lookups matching `GDP_MAIL`. Follow up by creating the actual SMTP transaction endpoint `invioMAIL` (F22).
**Estimate**: 10h

### Ticket: Build Quarantine Retry Enabler (F21)
**Type**: Dependent
**Depends On**: F10
**Description**: Write the manual resubmission backend api `attivaCODA` (F21) to reset DAM load failures inside `GDP_IMPORT_TASK`, adhering strictly to max-attempts restrictions (`NRO_MAX_TENTATIVI`).
**Estimate**: 4h

---

## EPIC 6: Testing & Observability Deployment

### Ticket: Implement System Observability Stack 
**Type**: Independent
**Description**: Expose logging correlation IDs traversing threads when processing an edition. Provide Quarkus `/q/health` probes (SFTP connectivity livechecks) and `/q/metrics` mapping success volumes (Prometheus).
**Estimate**: 10h

### Ticket: K8s Infrastructure Configurations
**Type**: Independent
**Description**: Document all env vars (`LIBRA_URL`, `DB_URL`, etc). Write local DEV `docker-compose.yml` configs validating Quarkus configurations. 
**Estimate**: 8h
