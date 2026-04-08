# Audit Report: F03 — FTPregolare.checkEdizioneAttesa

**Component:** gdporch  
**Feature:** F03 — Acquisition Orchestration (Periodic Flow)  
**Status:** ✅ Completed (Pipeline Integrated)

## Summary of Analysis

The implementation in `CheckEdizioneAttesaJob.java` now correctly scans the SFTP server, verifies transfer stability, resolves testata unicity, and **triggers the asynchronous validation pipeline (F04)** as required.

## Findings vs. Specification

| Requirement | Spec Compliance | Implementation | Severity |
| :--- | :--- | :--- | :--- |
| **Trigger** | Every 15 minutes | `@Scheduled(every = "15m")` | OK |
| **SFTP Scan** | Scan `flusso_regolare` | Correctly implemented | OK |
| **Stability Check** | Size stability for 3 min | Implemented in `isCartellaStabile` | OK |
| **Testata Lookup** | Match by `CARTELLA_TESTATA` | `testataRepository.findByCartella` | OK |
| **Ambiguity (Dupli.)** | Move to `_errata`, Log `MSG00002` | Correctly implemented | OK |
| **Marker Strategy** | Create `.OK` sentinel | Correctly implemented | OK |
| **Pipeline Trigger** | **Invoke F04 async** | ✅ **IMPLEMENTED** | OK |

## Detected Issues & Risks

### 1. Missing F04 Invocation (Resolved)

The service now correctly invokes **F04 — FTPregolare.ctrlEdizioneAcquisita** asynchronously using a `ManagedExecutor`. This ensures that processing is non-blocking and following the transactional persistence of the log entry.

### 2. Error Code Discrepancy (Resolved)

Standardized on `F03_TESTATA_NOT_FOUND` in `GdpMessage` to handle cases where the folder name does not match any censused testata, improving traceability beyond generic codes.

### 3. Blocking Stability Check (Medium)

`isCartellaStabile` uses `Thread.sleep` within the `ManagedExecutor`. While not blocking the scheduler, it holds a thread for up to 3 minutes per edition.

- **Recommendation:** Ensure the thread pool size is sufficient for the expected number of active publishers.

### 4. Transactional Boundaries (Resolved)

The SFTP `rename` operations are now properly coordinated with DB log persistence. The flow ensures that the `idLog` is generated and available for passing to the F04 pipeline, maintaining a strong link between physical files and logical audit trails.

## Suggested Fixes (Implemented)

1.  **Integrate F04 Call**: Injected `GdpCtrlEdizioneAcquisitaService` and implemented async call with `idLog`.
2.  **Date Type Resolution**: Resolved `OffsetDateTime` vs `Date` mismatch for compatibility with generated DTOs.
3.  **Error Handling**: Added specific message code for testata resolution failures.

---

## F01 Audit Note (configDTEdizioneAttesa)

A quick review of `GdpDataUscitaService.java` shows that the **F01** logic is **Correctly Implemented**:

- It handles both `Gnn` and `GnSm` patterns for monthly editions.
- It correctly implements the `nWSm` logic for sub-monthly editions.
- It enforces idempotency in `salvaBatch`.
- Date range calculations follow the "first occurrence from Jan 1st" rule.

---
**Status:** All critical items for F03 have been implemented and verified to compile.
