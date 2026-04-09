domanda 1
Servizio: FTPregolare
Modalità di richiamo
Il servizio è richiamato in modalità “asincrona” dal BE applicativo.
per be applicativo cosa si intende ? e il bff ? e un comando cli ?

domanda 2
GDP_IMPORT_TASK (= GDP_CODA_CARICAMENTO) ?

domanda 3
- **Schema Discrepancy (Spec vs DB):** The specification (UC F10) mentions updating `GDP_LOG_EDIZIONE.STATO`. However, the current database DDL and JPA entities only provide a `DESCRIZIONE` field.
  - **Action taken:** Status updates ("SUBMITTED", "FAILED") are persisted in the `DESCRIZIONE` field.
  - **Recommendation:** If a structured `STATO` column is required for reporting, a DB migration should be planned.
