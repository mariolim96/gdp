# SPEC.md — Project Specification

> **Status**: `FINALIZED`

## Vision
gdporch is the Orchestration Engine — a pure backend service without a user interface. Its primary role is to serve as the invisible backbone that polls, acquires, validates, and transmits editorial editions to the DAM via scheduled jobs and async processes, while also providing REST monitoring endpoints for the BFF layer.

## Functional Flow Walkthrough

### 1. Periodic Flow (Daily)
The orchestrator operates via three main automated triggers:
- **Trigger A (Yearly/On-demand - F01):** Calculates all expected release dates for each active testata and saves them to `GDP_DATA_USCITA`. This is the foundation for all subsequent steps.
- **Trigger B (Daily at 20:00 - F02):** Physically creates the directory for tomorrow's edition on the SFTP server (`yyyy-mm-dd`), where publishers must deposit their PDFs.
- **Trigger C (Every 15 minutes - F03):** Scans all SFTP folders. If new files are found, it moves them to a `/_tmp` area and asynchronously triggers the validation process (**F04**).

### 2. The Processing Heart (F04)
Triggered asynchronously by F03, **F04** performs the following sequence:
- **Coherence Check:** verifies the edition date against the DB (Classifies as OK, Early, Late, Suspended, or Anomalous).
- **PDF Pre-processing:** checks for multi-page PDFs and splits them if necessary.
- **Readability:** ensures PDF/A compliance.
- **Naming Standard:** validates and renames files to conform to the system standard.
- **Extraction:** Extracts text via `pdftotext`.
- **Heuristics:** Checks for date presence and front-page signature.
- **Execution Chain:** Calls **F08** (DB Sync), then **F09** (XML Sync), and finally triggers **F10** (DAM Async).

### 3. Persistence & Transmission
- **F08 (Database):** Inserts or updates records in `GDP_EDIZIONE` and `GDP_PAGINA`.
- **F09 (Packaging):** Builds the XML metadata file, compresses it with the PDF/TXT assets into a `.zip`, and inserts a record into the `GDP_IMPORT_TASK` transmission queue.
- **F10 (DAM Courier - every 30 min):** Reads the queue (ordered by priority) and submits the ZIP file to the **DAM LIBRA** API.

### 4. Historical Flow (Bulk Load)
- **Nightly Trigger:** Scans `flusso_saltuario` for `CONS_yyyy-mm-dd` packages.
- **F07:** Validates historical edition dates and files (PDF, TXT, TIF).
- **Completion:** Triggers the same `F08` → `F09` → `F10` chain but with **Priority 100** (historical loads are processed after daily periodic ones).

### 5. BFF Console (Monitoring & Control)
Exposes synchronous operations for the UI:
- **Queries:** `F12`, `F13`, `F15` for acquisition logs and details.
- **Operations:** `F20` (Check DAM Job status), `F21` (Manual retry for failed tasks).
- **Notifications:** `F14` + `F22` for preparing and sending mail notifications.
- **Configuration:** `F05` for manually suspending expected dates.

## Goals
1. Monitor SFTP server for new editions deposited by publishers (periodic flow) and archivists (historical flow).
2. Validate every acquired PDF file according to strict format and naming rules.
3. Manage database persistence for edition and page records within shared PostgreSQL 15.
4. Generate strictly validated XML metadata and package it into a `.zip` with the PDFs and extracted TXTs.
5. Transmit the complete packages to the DAM LIBRA via REST APIs.
6. Expose comprehensive monitoring REST APIs for consumption by `gdpbff`.

## Non-Goals (Out of Scope)
- User, Publisher, or Testata registration functionality.
- UI for monitoring, administration, or configuring periodicity.
- Direct User / SFTP Authentication logic (handled by Shibboleth + IAM/RUPAR).
- Full-text search or document viewing in the archive.
- GDPR oblio processing logic (gdporch only receives commands, while gdpbff coordinates execution).

## Users
- This is a system-to-system application. It integrates with:
  - Publishers / Archivists (via SFTP server).
  - DAM LIBRA System (via REST POST requests).
  - Back-end For Front-end (`gdpbff`) via internal REST monitoring APIs.

## Constraints
- **Framework:** Quarkus 3.x (`jakarta.*` namespace exclusively).
- **Runtime:** JVM Adoptium Temurin Java 17.
- **Data Persistence:** PostgreSQL 15 Community with restricted permissions (No SUPERUSER).
- No embedded UI logic.
- Asynchronous and Scheduled Operations heavily utilized (`@Scheduled`).
- All environment configurations (sftp, dam url, db credentials) must be injected; none hardcoded.

## Success Criteria
- [ ] Successfully acquire, validate, and move periodic flow testate from SFTP (F03, F04).
- [ ] Successfully acquire, validate, and move historical flow testate from SFTP (F06, F07).
- [ ] Insert/update entities into db effectively, avoiding duplicates (F08).
- [ ] Prepare valid XML schemas inside `.zip` packages for DAM (F09).
- [ ] Successfully transmit to DAM and track results idempotently (F10).
- [ ] Functioning set of Monitor APIs matching contract specifications for `gdpbff` integration.
