# SPEC.md — Project Specification

> **Status**: `FINALIZED`

## Vision
gdporch is the Orchestration Engine — a pure backend service without a user interface. Its primary role is to serve as the invisible backbone that polls, acquires, validates, and transmits editorial editions to the DAM via scheduled jobs and async processes, while also providing REST monitoring endpoints for the BFF layer.

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
