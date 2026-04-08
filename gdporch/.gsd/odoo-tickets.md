# Odoo Tickets for gdporch

Below is a comprehensive list of all potential Odoo tickets required to complete `gdporch`, broken down by functional flow. They are grouped into logical Epics following the data journey from scheduling to transmission.

---

## EPIC 1: Expected Scheduling & Acquisition (Phase 3)
*Focus: Calculating when to expect editions and capturing them from SFTP.*

### Ticket: Implement Expected Date Calculation Logic (F01) [COMPLETED]
**Type**: Independent
**Description**: Write the complex `configDTEdizioneAttesa` (F01) service to calculate expected publishing dates based on periodic rules (e.g. `MENSILITA` values, `nGx` formats). Ensure records are idempotently persisted to `GDP_DATA_USCITA` table.
**Estimate**: 18h

### Ticket: Implement Date Suspension & Retrieval Logic (F05 & F18)[DEVELOPING]
**Type**: Dependent
**Depends On**: F01
**Description**: Implement the `sospensioneEdizioneAttesa` (F05) service to allow manual suspension of expected editions, and `verifDateAttese` (F18) for retrieving the calendar of expected/suspended dates.
**Estimate**: 8h

### Ticket: Implement SFTP Infrastructure & Directory Generation (F02) [COMPLETED]
**Type**: Independent
**Description**: Build a reusable Apache MINA SSHD SFTP client wrapper. Implement the scheduled job `creaCartellaEdizioneAttesa` (F02) that polls tomorrow's expected dates and generates empty directories on the Publisher's SFTP.
**Implementation Note**: Switched from MINA to JSch and added `atmoz/sftp` Docker container for local testing.
**Estimate**: 18h

### Ticket: Implement Periodic Publishing Polling Job (F03) [DEVELOPING]
**Type**: Dependent
**Depends On**: F01, F02
**Description**: Build the 15-minute polling service `checkEdizioneAttesa` (F03) scanning `/_flusso_regolare`. Detect transfer completion (size stability), record acquisition to `GDP_LOG`, and prepare the staging area in `/_tmp`.
**Estimate**: 16h

---

## EPIC 2: Validation & State Persistence (Phase 4)
*Focus: Processing acquired files and recording the permanent state in the database.*

### Ticket: Implement Validation Rules for Periodic Flow (F04) [COMPLETED]
**Type**: Dependent
**Depends On**: F03
**Description**: Create the core PDF validation logic (F04) including multi-page splitting, format/naming verification, and basic TXT extraction. Implement date and front-page heuristics from PDF contents.
**Estimate**: 12h

### Ticket: Implement Edition Data Persistence Service (F08) [DEVELOPING]
**Type**: Dependent
**Depends On**: F04
**Description**: Implement the transactional `insEdizione` (F08) service. It must create or update `GDP_EDIZIONE` and `GDP_PAGINA` records based on validated metadata. Calculate `DATA_PUBBLICAZIONE` depending on periodic rules.
**Estimate**: 16h

### Ticket: Implement Testate Lookup Queries (F16, F17) 
**Type**: Independent
**Description**: Implement basic read operations: `getElencoTestate` (F16) for lists with filtering and `getTestata` (F17) for full details.
**Estimate**: 6h

---

## EPIC 3: Historical Delivery (Phase 5)
*Focus: Handling bulk historical loads from dedicated archivist folders.*

### Ticket: Implement Historical Delivery Polling Job (F06) [DEVELOPING]
**Type**: Independent
**Description**: Implement the nightly `checkConsegnaStorico` job (F06) traversing `/_flusso_saltuario`. Handle folder resolution, map against known testata names, and handle duplicate/unknown testata cases according to spec.
**Estimate**: 12h

### Ticket: Implement Validation Rules for Historical Flow (F07) [DEVELOPING]
**Type**: Dependent
**Depends On**: F06
**Description**: Create historical edition validation (F07) to verify the structured folder names (`yyyymmdd`) and check for the presence of matching PDF, TXT, and optional TIF files.
**Estimate**: 10h

---

## EPIC 4: DAM Transmission Pipeline (Phase 6)
*Focus: Packaging assets and transmitting to the CSI DAM system.*

### Ticket: Implement XML Schema Generation (F09)
**Type**: Dependent
**Depends On**: F08
**Description**: Build the `creaXMLEdizione` service. Transform DB metadata into XML aligning with XSD `GdP-STD-04-V01-Validazione_metadati_flussoFTP.xsd`. Zip all generated assets (`.xml`, `.pdf`, `.txt`) into `/_dam` target folders. 
**Estimate**: 18h

### Ticket: Implement DAM Courier Service and Uploads (F10)
**Type**: Dependent
**Depends On**: F09
**Description**: Implement the `inviaEdizione` (F10) recurring daemon. Interrogate `GDP_CODA_CARICAMENTO`, send ZIP payloads to the RestClient integrated DAM endpoints (`POST /api/v2/imports`). Handle REST faults via the queue system.
**Estimate**: 14h

### Ticket: Implement Dam Cleanup Job (F19)
**Type**: Dependent
**Depends On**: F10
**Description**: Implement the `pulisciEdizione` job to cleanup `/_tmp` and `/_dam` footprints on local disks and remote SFTP when conditions match confirmed DAM loads.
**Estimate**: 8h

---

## EPIC 5: Monitoring & Mail Endpoints (Phase 7)
*Focus: Exposing status and handling communication failures via BFF.*

### Ticket: Build Acquisition Query APIs (F12, F13, F15)
**Type**: Independent
**Description**: Implement REST services for dashboard data: `elencoAcquisizioni` (F12), `ricercaAcquisizioni` (F15), and `dettaglioAcquisizione` (F13).
**Estimate**: 12h

### Ticket: Build Active DAM Integration Check (F20)
**Type**: Independent
**Description**: Expose `statoDAM` (F20) bridging to the DAM GET queue status RestClient to live-check processing conditions.
**Estimate**: 6h

### Ticket: Build Mail Generation & Transmission Operations (F14, F22)
**Type**: Independent
**Description**: Implement `preparaMAIL` (F14) for template assembly and `invioMAIL` (F22) for the actual SMTP transaction.
**Estimate**: 10h

### Ticket: Build Quarantine Retry Enabler (F21)
**Type**: Dependent
**Depends On**: F10
**Description**: Write the manual resubmission backend api `attivaCODA` (F21) to reset DAM load failures inside the upload queue.
**Estimate**: 4h

---

## EPIC 6: Observability & Infra
*Focus: Deployment and monitoring readiness.*

### Ticket: Implement System Observability Stack 
**Type**: Independent
**Description**: Quarkus `/q/health` probes (SFTP connectivity) and `/q/metrics` mapping success volumes (Micrometer).
**Estimate**: 10h

### Ticket: K8s Infrastructure Configurations
**Type**: Independent
**Description**: Document and write `docker-compose.yml` and K8s ConfigMaps validating Quarkus configurations. 
**Estimate**: 8h
