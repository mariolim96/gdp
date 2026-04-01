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

---

## Phases

### Phase 1: Foundation & Codebase Consolidation
**Status**: 🟢 Completed (Prior Context)
**Objective**: Establish repository, OpenAPI contracts, baseline Quarkus configuration, and DB Panache entities architecture.

### Phase 2: Odoo Tickets Generation
**Status**: 🟢 Completed
**Objective**: Generate and formalize a comprehensive list of all Odoo tickets required to complete the project phases based on the SPEC and ROADMAP.

### Phase 3: Expected Scheduling & Acquisition
**Status**: ⬜ Not Started
**Objective**: Establish the core acquisition engine. Calculate expected dates (F01), manage suspensions (F05/F18), create SFTP folder structures (F02), and poll for incoming periodic editions (F03).

### Phase 4: Validation & Persistence
**Status**: ⬜ Not Started
**Objective**: Implement the validation "Brain" and the permanent state recorder. Validating PDF contents (F04), performing database persistence (F08), and exposing testata lookups (F16/17).

### Phase 5: Historical Delivery
**Status**: ⬜ Not Started
**Objective**: Integrate archivist-driven historical loads. Implement historical polling (F06) and specific historical validation rules (F07).

### Phase 6: DAM Transmission Pipeline
**Status**: ⬜ Not Started
**Objective**: Create the automated export flow. XML metadata generation (F09), ZIP packaging, and transmitting to DAM LIBRA (F10) with cleanup routines (F19).

### Phase 7: Monitoring REST API Endpoints
**Status**: ⬜ Not Started
**Objective**: Expose all `/bo/**` status and control operations. Monitoring queries (F12/13/15), mail handling (F14/22), and DAM queue retry logic (F20/21).

---

## Final Review & Handover
**Objective**: Full end-to-end integration testing in staging environment, observability tuning (Prometheus/Grafana), and handover to CSI ops Team for K8s deployment.
