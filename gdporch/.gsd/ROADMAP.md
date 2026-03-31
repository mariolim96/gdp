# ROADMAP.md

> **Current Phase**: Phase 2
> **Milestone**: v1.0

## Must-Haves (from SPEC)
- [x] Base Database Panache Entities (Brownfield baseline)
- [ ] SFTP Period Flow Jobs (F01, F02, F03, F04)
- [ ] SFTP Historical Flow Jobs (F06, F07)
- [ ] Database Persistence Service (F08)
- [ ] DAM Transmission Service (F09, F10)
- [ ] Monitoring REST API (F12, F13, F14, F15, F16, F17, F18, F20, F21, F22)

## Phases

### Phase 1: Foundation & Codebase Consolidation
**Status**: 🟢 Completed (Prior Context)
**Objective**: Establish repository, OpenAPI contracts, baseline Quarkus configuration, and DB Panache entities architecture.
**Requirements**: REQ-01

### Phase 2: Odoo Tickets Generation
**Status**: 🟢 Completed
**Objective**: Generate and formalize a comprehensive list of all Odoo tickets required to complete the project phases based on the SPEC and ROADMAP.
**Requirements**: None

### Phase 3: Configuration & Validation Services
**Status**: ⬜ Not Started
**Objective**: Establish core validation rules (F04, F07 requirements), parsing strategies (PDFBox, PDF naming rules), and expected date configuration calculators (F01, F05).
**Requirements**: REQ-02, REQ-03

### Phase 4: DB Integration & State Management
**Status**: ⬜ Not Started
**Objective**: Fully implement the F08 database persistence service for inserting/updating edition and page records based on validated models, along with base data lookup queries.
**Requirements**: REQ-04

### Phase 5: SFTP Integration & Polling Jobs
**Status**: ⬜ Not Started
**Objective**: Implement SFTP folder creation, periodic polling (F02, F03), historical polling (F06), and the orchestration linking them to validation and DB routines.
**Requirements**: REQ-05, REQ-06

### Phase 6: DAM Transmission Pipeline
**Status**: ⬜ Not Started
**Objective**: Create XML (F09) generation conforming to the schema, package it alongside the PDF and TXT into a ZIP, and implement the cronjob to transmit to DAM LIBRA (F10).
**Requirements**: REQ-07

### Phase 7: Monitoring REST API Endpoints
**Status**: ⬜ Not Started
**Objective**: Expose all `/bo/**` operations for BFF consumption including acquisition listings, mail preparation/sending (F14, F22), and DAM status queue active integrations (F20, F21).
**Requirements**: REQ-08
