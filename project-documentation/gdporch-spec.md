# gdporch — Orchestration Engine: Complete Technical Specification

## Project Context

**System:** GDP — Giornali del Piemonte (Piedmont Newspapers)  
**Client:** Consiglio Regionale del Piemonte (CRP)  
**Plan Reference:** Piano di Sviluppo 2025 — rif. 2025.CR.CC.01  
**Component:** `gdporch` (Quarkus 3.x backend service)  
**Role:** Acquisition Orchestrator — invisible backbone that acquires, validates, and transmits editorial editions to the DAM  
**Version:** SFU-01-V03 (March 2026)  
**Status:** Draft — under review

### What gdporch does

gdporch is a **pure backend service with no user interface**. It runs entirely as scheduled jobs and async processes. Its sole responsibility is:

1. Monitoring SFTP server for new editions deposited by publishers (periodic flow) or archivists (historical flow)
2. Validating every PDF file according to strict format and naming rules
3. Inserting edition and page records into the shared PostgreSQL database
4. Generating XML metadata conforming to schema `GdP-STD-04-V01-Validazione_metadati_flussoFTP.xsd`
5. Packaging XML + PDF + TXT into a `.zip` file and transmitting it to DAM LIBRA
6. Exposing REST monitoring APIs consumed by `gdpbff` (the BFF layer)

---

## System Architecture Overview

```
Publishers / Archivists
        │ sFTP deposit
        ▼
  sftp.al01.csipiemonte.it  (port 22, RSA key auth)
        │
        ▼
   gdporch (Quarkus 3.x)
   ┌─────────────────────────────────────┐
   │  Scheduler (Quarkus @Scheduled)     │
   │  ┌──────────┐  ┌─────────────────┐ │
   │  │ F02/F03  │  │ F06             │ │
   │  │ periodic │  │ historical      │ │
   │  │ polling  │  │ polling         │ │
   │  └────┬─────┘  └────────┬────────┘ │
   │       │                 │           │
   │  F04.ctrlEdizione   F07.ctrlStoriche│
   │       │                 │           │
   │  F08.insEdizione ───────┘           │
   │       │                             │
   │  F09.creaXML                        │
   │       │                             │
   │  F10.inviaEdizione (every 30 min)   │
   │                                     │
   │  MONITOR APIs: F12,F13,F14,F15,F18  │
   │               F20,F21,F22           │
   └─────────────────────────────────────┘
        │               │
        ▼               ▼
   PostgreSQL 15    DAM LIBRA (REST)
                    http://ts-libra-sv-exp1.csi.it/rpcr02
                    POST /api/v2/imports
```

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Framework | Quarkus 3.x (jakarta.* — NOT javax.*) |
| JVM | Adoptium Temurin 17 |
| Deploy | Kubernetes cluster k8s-ol-prod01 |
| Database | PostgreSQL 15 Community |
| SFTP Client | Apache MINA SSHD 2.12.x |
| PDF Processing | Apache PDFBox 3.x |
| REST Client (DAM/Mail) | MicroProfile REST Client (`@RegisterRestClient`) |
| Fault Tolerance | SmallRye Fault Tolerance (`@Retry`, `@CircuitBreaker`, `@Timeout`, `@Fallback`) |
| Scheduler | Quarkus Scheduler (`@Scheduled`) |
| ORM / DB | Hibernate ORM Panache |
| Logging | JBoss Logging + Quarkus JSON logging (prod) |
| Metrics | Micrometer + Prometheus registry |
| Health | SmallRye Health (`/q/health`) |
| Tracing | OpenTelemetry (`@WithSpan`) → Jaeger/OTLP |

**Critical constraint:** Use `jakarta.*` packages only. Quarkus 3.x is mandatory for CSI stack compatibility. Java 17 only — no earlier or later versions at release time.

---

## SFTP Server Configuration

| Parameter | Value |
|-----------|-------|
| Host | `sftp.al01.csipiemonte.it` |
| Port | 22 |
| Authentication | RSA private key — path from env var `SFTP_KEY` |
| Library | Apache MINA SSHD 2.12.x |
| Periodic editions area | `/<rootFTP>/flusso_regolare/` |
| Historical editions area | `/<rootFTP>/flusso_saltuario/` |
| Working area | `/<rootFTP>/_tmp/` |
| Error area | `/<rootFTP>/_errata/` |
| DAM staging area | `/<rootFTP>/_dam/` |

---

## Database Schema — Tables Used by gdporch

gdporch reads from and writes to the shared GdP PostgreSQL database. No SUPERUSER — dedicated user with restricted permissions.

### GDP_TESTATA
- **Access:** READ
- **Purpose:** Retrieve active testate, `CARTELLA_TESTATA` field, SFTP config
- **Key fields:** `ID_GDP_TESTATA`, `NOME_TESTATA`, `CARTELLA_TESTATA`, `INVIO_EDIZIONE` (**singular** — `boolean`: True = sends periodic editions), `STATO` (0=attiva, 1=storica), `DATA_STATO`, `CANCELLAZIONE`, `COD_TEMA`, `TEMA`, `SOC_EDITRICE`, `ENTE_PROPONENTE`, `ANNO_FONDAZIONE`, `PERIODO_FREQ`, `PERIODO_GG`, `DESCRIZIONE`, `WWW`, `MAIL`, `PROVINCIA`, `COMUNE`, `INDIRIZZO`, `CAP`, `LONGITUDINE`, `LATITUDINE`

> **Important:** The column is `INVIO_EDIZIONE` (singular), not `INVIO_EDIZIONI`. All queries filtering active senders must use `INVIO_EDIZIONE = true`.

### GDP_PERIODICITA
- **Access:** READ
- **Purpose:** Read `MENSILITA` and `GG_PERIODICITA` for date calculation and edition classification
- **Key fields:** `ID_GDP_PERIODICITA`, `FK_GDP_TESTATA`, `MENSILITA` (**integer** — not double/float), `GG_PERIODICITA` (varchar 128), `DT_FINE_VALIDITA` (constraint: only one NULL record per testata = current config), `INIZIO_SOSPENSIONE`, `FINE_SOSPENSIONE`

> **Important:** `MENSILITA` is `integer` in the DB. Values: 0=sub-monthly, 1=monthly, 2=bimonthly, 3=quarterly, 4=every 4 months, 6=semi-annual, 12=annual. Twice-monthly publications use `MENSILITA=1` with two dates in `GG_PERIODICITA` (e.g. `G01;G15`).

### GDP_DATA_USCITA
- **Access:** READ / WRITE
- **Purpose:** Store expected publication dates per periodicity range
- **Key fields:** `ID_GDP_DATA_USCITA`, `FK_GDP_PERIODICITA`, `DT_INIZIO` (date — range start), `DT_FINE` (date — range end), `DATA_ATTESA` (date — specific expected date), `SOSPESA` (boolean, default False)

> **Important:** The table has `DT_INIZIO` + `DT_FINE` range fields (not `ANNO` as previously documented). F01 inserts records specifying the validity range of the date calculation. F03 queries by `DATA_ATTESA` to check for expected editions.

### GDP_EDIZIONE
- **Access:** READ / WRITE
- **Purpose:** INSERT new edition or UPDATE if existing. Read by F09 for XML generation.
- **Key fields:** `ID_GDP_EDIZIONE`, `FK_GDP_TESTATA`, `DATA_EDIZIONE`, `DATA_PUBBLICAZIONE`, `STATO` (default 0), `NUMERO_PAGINE`

### GDP_PAGINA
- **Access:** READ / WRITE
- **Purpose:** INSERT one record per valid PDF page. Read by F09 for XML metadata.
- **Key fields:** `ID_GDP_PAGINA`, `FK_GDP_TESTATA`, `FK_GDP_EDIZIONE`, `NUM_PAGINA`, `FILE_PDF` (varchar 128), `FILE_TXT` (varchar 128), `FILE_TIF` (varchar 128, NULL for periodic), `ANNO_EDIZIONE` (integer — redundant year), `STATO` (integer, default 0), `OBLIO` (**varchar(256)** — list of items to de-index, NULL if no oblio request), `DATA_OBLIO` (date), `NOTA_OBLIO` (varchar 2048)

> **Important:** `OBLIO` is **varchar(256)**, not a boolean. It stores a descriptive list of content items to obscure. When processing an oblio request, set `OBLIO` to the list of terms/items, not simply `true`.

### GDP_LOG
- **Access:** WRITE
- **Purpose:** One record per SFTP acquisition detected (both periodic and historical)
- **Key fields:** `ID_GDP_LOG`, `FK_GDP_UTENTEFTP` (**column name** — FK to `GDP_UTENTESFTP.ID_GDP_UTENTESFTP`), `FK_GDP_TESTATA` (0 if testata unknown), `TIPO_ACQUISIZIONE` ('G'=daily, 'S'=historical), `DT_ACQUISIZIONE` (date), `TOTALE_FILE_ACQUISITI`, `ESITO` (varchar 1024)

### GDP_LOG_EDIZIONE
- **Access:** WRITE
- **Purpose:** Detail of validation per edition: pages OK/KO, edition type, anomaly description, XML/ZIP flags
- **Key fields:** `ID_GDP_LOG_EDIZIONE`, `FK_GDP_LOG`, `FK_GDP_EDIZIONE`, `TIPO_EDIZIONE` (FK to `GDP_TIPO_EDIZIONE.COD_TIPO`), `PATH_EDIZIONE` (varchar 256 — full staging path), `NRO_PAG_ACQUISITE`, `NRO_PAG_VALIDE`, `NRO_PAG_ERRATE`, `PRIMA_PAGINA` (boolean), `FILE_XML` (boolean), `FILE_ZIP` (boolean), `JOB_ID` (varchar 128 — DAM job ID), `DESCRIZIONE` (varchar 8192)

> **Note (v2 SFU-01):** `PATH_EDIZIONE` was added in version 2. Must be populated with the full staging path: `/<rootFTP>/_tmp/<cartellaTestata>/<dataEdizione>` for periodic, or `/<rootFTP>/_tmp/<utenteStorico>/CONS_<dataConsegna>/<nomeTestata>/<dataEdizione>` for historical.

### GDP_IMPORT_TASK (= GDP_CODA_CARICAMENTO)
- **Access:** READ / WRITE
- **Purpose:** DAM transmission queue. INSERT when .zip is created; UPDATE after DAM response.
- **Key fields:** `ID_GDP_CODA_CARICAMENTO`, `FK_GDP_LOG_EDIZIONE`, `DT_INSERIM_IN_CODA` (**date** — field name confirmed from DB schema), `NRO_TENTATIVO` (integer, default 0), `DT_TENTATIVO` (timestamp), `SFTP_PATH` (varchar 256), `PRIORITA` (integer: 0=daily, 100=historical), `STATO` (varchar 64: 'PRO', 'READY', 'SUBMITTED', 'FAILED'), `NRO_MAX_TENTATIVI` (integer, **default 10** — F21 checks this before allowing retry)

### GDP_UTENTSFTP
- **Access:** READ
- **Purpose:** Verify `HOME_SFTP` for constructing SFTP paths; source of email for F14
- **Key fields:** `ID_GDP_UTENTESFTP`, `USERNAME`, `PASSWORD`, `RIF_TESTATA`, `HOME_SFTP`, `DIRETTORE`, `REFERENTE_SFTP`, `EMAIL`, `WWW`, `EDITORE`, `INDIRIZZO`, `TELEFONO_FISSO`, `TELEFONO_CELLULARE`, `DT_CREAZIONE`, `DT_FINE_VALIDITA`, `STATO`

### GDP_TIPO_FILE
- **Access:** READ
- **Purpose:** Lookup table for file anomaly codes used in `GDP_LOG_EDIZIONE.DESCRIZIONE` and F04/F07 validation. Codes are prefixed to file names in the description string (e.g. `"NL – page001.pdf"`).
- **Key fields:** `COD_TIPO` (varchar 8), `TIPO_FILE` (varchar 128)
- **Seeded data:**

| COD_TIPO | TIPO_FILE |
|----------|-----------|
| `NP` | PDF multi-pagina |
| `NL` | file non leggibile |
| `NF` | file formato errato |
| `DA` | data attesa |
| `PP` | prima pagina |

### GDP_MAIL
- **Access:** READ
- **Purpose:** Email templates used by F14 (`preparaMAIL`). Keyed by `COD_MAIL` (varchar 5). Placeholders `<[dataED]>` and `<[nomeTestata]>` are substituted at runtime.
- **Key fields:** `COD_MAIL` (varchar 5 — PK), `SMTP_MAIL_HOST` (varchar 128), `SMTP_MAIL_PORTA` (integer), `MITTENTE` (varchar 128), `TESTO_OGGETTO` (varchar 1024), `TESTO_MAIL` (varchar 4096)
- **Seeded data from init.sql:**

| COD_MAIL | Host | Port | Mittente | Subject |
|----------|------|------|----------|---------|
| `GW001` | mailfarm-app.csi.it | 25 | assistenza.bdp@csi.it | Giornali del Piemonte - Edizione da integrare |
| `GE001` | mailfarm-app.csi.it | 25 | assistenza.bdp@csi.it | Giornali del Piemonte - Edizione non caricata |
| `SW001` | mailfarm-app.csi.it | 25 | assistenza.bdp@csi.it | Giornali del Piemonte - Edizione da integrare (storica) |
| `SE001` | mailfarm-app.csi.it | 25 | assistenza.bdp@csi.it | Giornali del Piemonte - Edizione non caricata (storica) |
| `ST001` | mailfarm-app.csi.it | 25 | assistenza.bdp@csi.it | Giornali del Piemonte - Edizioni testata non caricate |

> **Important:** Codes are **5 characters** (`GW001` not `GW`). F14 receives and uses these exact `COD_MAIL` values. The openapi.yaml tipoMail enum has been updated accordingly.

### GDP_TIPO_EDIZIONE
- **Access:** READ
- **Purpose:** Lookup table for decoding `TIPO_EDIZIONE` codes into human-readable descriptions. Used by F13 and F15.
- **Key fields:** `COD_TIPO` (varchar 8 — PK), `TIPO_EDIZIONE` (varchar 128)
- **Seeded data (from init.sql — source of truth):**

| COD_TIPO | TIPO_EDIZIONE |
|----------|---------------|
| `OK` | corrispondente |
| `SO` | sospesa |
| `AN` | anticipataria |
| `PO` | posticipataria |
| `AA` | anomalia edizione attesa |
| `ST` | edizione storica |
| `AS` | edizione storica con anomalia |

> **⚠️ Discrepancy:** SFU-01 specification uses codes `AT` (anticipataria) and `PT` (posticipataria) in the F04 process description, but the actual DB lookup table uses `AN` and `PO`. The DB init.sql is the **source of truth** — use `AN` and `PO` in all code.

---

## Scheduled Jobs

| Job | Frequency | Service | Description |
|-----|-----------|---------|-------------|
| F03 checkEdizioneAttesa | Every 15 minutes | FTPregolare.checkEdizioneAttesa | Poll periodic SFTP flow |
| F02 creaCartellaDomani | Daily at 20:00 | FTPregolare.creaCartellaEdizioneAttesa | Create folders for next day's expected editions |
| F06 checkConsegnaStorico | Daily at 01:00 | FTPsaltuario.checkConsegnaStorico | Poll historical SFTP flow |
| F01 configDTEdizioneAttesa | On-demand (async from BFF) | FTPregolare.configDTEdizioneAttesa | Calculate expected dates for a period |
| F10 inviaEdizione | Every 30 minutes | DAMtrasmissione.inviaEdizione | Send queued .zip files to DAM LIBRA |
| F19 pulisciEdizione | Every 24 hours | DAMtrasmissione.pulisciEdizione | Cleanup staged files (detail TBD) |
| Cleanup staging | Daily at 03:30 | Internal | Remove files in `/_tmp` and `/_dam` older than 4 hours |

---

## Service: FTPregolare — Periodic Flow

### F01 — FTPregolare.configDTEdizioneAttesa

**Trigger:** Async, invoked by BFF (gdpbff) on-demand  
**Purpose:** Calculate expected edition dates for a given period and persist them to `GDP_DATA_USCITA`

**Input:**
| # | Parameter | Rules | Type | Required |
|---|-----------|-------|------|----------|
| 1 | dataInizio | yyyy-mm-dd | Date | Yes |
| 2 | dataFine | yyyy-mm-dd | Date | Yes |
| 3 | idTestata | If empty, process all testate with `INVIO_EDIZIONI=1` | Integer | No |

**Output:**
| # | Field | Content | Type |
|---|-------|---------|------|
| 0 | esito | MSG00009 (OK) or MSG0000x (error) | String |
| 1 | anno | Input year parameter | Integer |
| 2 | elenco | List of `{idTestata, nroEdizioniAttese}` per testata | Array |

**Algorithm — Case A: MENSILITA > 0 (monthly or multi-monthly periodicity)**

`GG_PERIODICITA` field in `GDP_PERIODICITA` can be in two forms:

Form 1 — Direct day of month:

- `NULL` → publisher must supply date list manually (Open Problem #1)
- `G00` → publication day undefined (default: 1st of month)
- `G01` → 1st of month
- `G12` → 12th of month
- `G01;G15` → 1st and 15th of month (when `MENSILITA=0.5`, i.e., bimonthly)

Form 2 — Ordinal weekday of month (`GnSm`):

- `G1S6` → first Saturday of month
- `G3S5` → third Friday of month

Calculation steps:
1. Find first publication date `MM0` starting from January 1st based on `GG_PERIODICITA`
2. Derive subsequent dates: `MMi+1 = MMi + MENSILITA` (in months)
3. Stop when next date falls beyond the input period

**Day codes for `Sm` patterns:**
- `S0` = daily (quotidiano — in form `GnS0` indicates the whole n-th week)
- `S1` = Monday
- `S2` = Tuesday
- `S3` = Wednesday
- `S4` = Thursday
- `S5` = Friday
- `S6` = Saturday
- `S7` = Sunday

**Extension Pattern (Case A):**
- `GnS0` → returns all publication dates (Monday–Sunday) belonging to the **n-th complete week** of the month.

**Algorithm — Case B: MENSILITA = 0 (sub-monthly periodicity)**

`GG_PERIODICITA` is in format `nWSm` (number of weeks × day of week):

Day codes:
- `S0` = daily (quotidiano)
- `S1` = Monday, `S2` = Tuesday, `S3` = Wednesday, `S4` = Thursday, `S5` = Friday, `S6` = Saturday, `S7` = Sunday

Examples:
- `1WS0` → every day
- `1WS3` → every Wednesday
- `1WS1;1WS4` → every Monday and Thursday
- `2WS7` → every other Sunday

Calculation steps:
1. Find first date from January 1st based on `GG_PERIODICITA`
2. `;` separates multiple weekly publication days
3. `nW` = interval in weeks between occurrences
4. Stop when next date falls beyond configured interval

**After calculation (both cases):**  
Insert one record per calculated date into `GDP_DATA_USCITA`:
- `FK_TESTATA` = testata ID
- `ANNO` = input year
- `DATA_ATTESA` = calculated date
- `SOSPESA` = NULL

Operation is **idempotent** — no duplicates.

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | OK |
| MSG00001 | No occurrences found for input parameters |
| MSG00002 | Active testata {ID} has no MENSILITA defined |
| MSG00003 | Active testata {ID} has no GG_PERIODICITA defined |

---

### F02 — FTPregolare.creaCartellaEdizioneAttesa

**Trigger:** Async, daily after 20:00  
**Purpose:** Create SFTP folder for next day's expected editions

**Steps:**
1. Query `GDP_DATA_USCITA` for non-suspended records where `DATA_ATTESA = sysdate+1`
2. For each: retrieve `CARTELLA_TESTATA` from `GDP_TESTATA`
3. Get `HOME_FTP` from `GDP_UTENTESFTP`
4. Create folder `yyyy-mm-dd` under the testata home on SFTP

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | OK |
| MSG00001 | No occurrences found |

---

### F03 — FTPregolare.checkEdizioneAttesa

**Trigger:** Async, every 15 minutes  
**Purpose:** Scan SFTP for newly deposited periodic editions

**Steps:**
1. Scan all folders under `/<rootFTP>/flusso_regolare/`
2. If nothing found → MSG00001, terminate
3. For each found edition in `/<rootFTP>/flusso_regolare/<cartellaTestata>/<dataEdizione>/`:
   a. **Verify transfer completion:** Check file size stability every 15 seconds for 3 minutes. Alternative (Linux): use `rsync -av -e ssh` during move.
   b. Set `<DataAcquisizione>` = timestamp of last file in folder (format `yyyy-mm-dd HH:MM:SS`)
   c. Look up `<IDTestata>` by querying `GDP_TESTATA` where `CARTELLA_TESTATA = <cartellaTestata>`
   
   **If multiple matches (ambiguity):**
   - Move PDFs to `/<rootFTP>/_errata/<cartellaTestata>/<dataEdizione>/`
   - Insert `GDP_LOG` record: `FK_GDP_TESTATA=0`, `TIPO_ACQUISIZIONE='G'`, `DT_ACQUISIZIONE=<DataAcquisizione>`, `ESITO=MSG00002`
   - No `GDP_LOG_EDIZIONE` record
   - Skip this edition, continue scanning
   
   **If single match:**
   - Set `GDP_LOG.FK_GDP_TESTATA = <IDTestata>`
   - Move entire edition to `/<rootFTP>/_tmp/<cartellaTestata>/<dataEdizione>/`
   - Count files → `<nroFILE>`
   - Place sentinel file with same name and `.OK` extension in original location
   - Insert `GDP_LOG` record: `FK_TESTATA=<IDTestata>`, `TIPO_ACQUISIZIONE='G'`, `DT_ACQUISIZIONE=<DataAcquisizione>`, `TOTALE_FILE_ACQUISITI=<nroFILE>`
   - Invoke **F04** asynchronously with params: `<IDTestata>`, `<cartellaTestata>`, `<dataEdizione>`, `<IDLog>`
   - Continue scanning

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | OK |
| MSG00001 | No new edition found |
| MSG00002 | `<E001>` Anomalia UNICITA' TESTATA — multiple IDs found for `<cartellaTestata>` |

---

### F04 — FTPregolare.ctrlEdizioneAcquisita

**Trigger:** Async, invoked by F03  
**Purpose:** Validate acquired periodic edition — full PDF-level control pipeline

**Input:**
| # | Parameter | Rules | Type | Required |
|---|-----------|-------|------|----------|
| 1 | testata | ID_TESTATA from DB | Integer | Yes |
| 2 | cartellaTestata | CARTELLA_TESTATA from DB | String | Yes |
| 3 | dataEdizione | yyyy-mm-dd | Date | Yes |
| 4 | log | ID_LOG from DB | Integer | Yes |

**Step 1 — Edition date validation:**
1. Query `GDP_PERIODICITA` where `FK_GDP_TESTATA = <IDTestata>` → get `ID_GDP_PERIODICITA`
2. Query `GDP_DATA_USCITA` where `FK_GDP_PERIODICITA = <ID>`, `ANNO = current_year`, `DATA_ATTESA = <dataEdizione>`

**Classify edition type (`<tipoEdizione>`):**
- `<dataEdizione>` == processing date → `"OK"`
- `<dataEdizione>` > processing date → `"AN"` (anticipataria — early) ⚠️ SFU-01 uses `"AT"` but DB lookup table `GDP_TIPO_EDIZIONE` has `"AN"` — use `"AN"`
- `<dataEdizione>` < processing date → `"PO"` (posticipataria — late) ⚠️ SFU-01 uses `"PT"` but DB lookup table has `"PO"` — use `"PO"`
- `<dataEdizione>` in DB but `SOSPESA=True` → `"SO"` (suspended)
- `<dataEdizione>` NOT in DB → `"AA"` (anomala — blocking)

**If `tipoEdizione == "AA"` (blocking):**
- Move edition to `/<rootFTP>/_errata/<cartellaTestata>/<dataEdizione>/`
- Set `GDP_LOG.ESITO = MSG00001`
- Terminate with MSG00001

**Step 2 — Per-PDF processing loop:**

For each PDF file in `/<rootFTP>/_tmp/<cartellaTestata>/<dataEdizione>/`:

**2a. Multi-page check:**
- If PDF has multiple pages → split into single-page files
- Add `"NP – <nomedelfile.pdf>"` to `<Descrizione>`
- Do NOT update file count

**2b. Readability check** (PDF/A validation using one of):
- `pdfinfo fileedizione.pdf`
- `pdfcpu validate fileedizione.pdf`
- `gs -dNOPAUSE -dBATCH -sDEVICE=nullpage fileedizione.pdf`
- Apache PDFBox

If NOT readable:
- Move file to `/<rootFTP>/_errata/<cartellaTestata>/<dataEdizione>/`
- Add `"NL – <nomedelfile.pdf>"` to `<Descrizione>`

**2c. Naming validation:**
Standard: `nomefile_numpagina.pdf`  
Rules:
- `nomefile` = any alphanumeric sequence, underscore `_` is NOT allowed
- `numpagina` = exactly 3 digits (e.g., `001`, `012`, `123`)
- Extension: `.pdf`

If VALID: rename to `cartellaTestata-dataEdizione_numeroPagina.pdf`  
If INVALID:
- Move to `/<rootFTP>/_errata/<cartellaTestata>/<dataEdizione>/`
- Add `"NF – <nomedelfile.pdf>"` to `<Descrizione>`

**2d. Text extraction:**
- Extract text using `pdftotext` (recommended) or Apache PDFBox
- Create `.txt` file with same base name for each valid PDF

**2e. Heuristic checks (non-blocking, add codes to description):**

*Date check (DATA ATTESA):*
Apply this regex to page 1 TXT:
```
DATE_REGEX = r"((^|[\s])(\d{1,2})°?(?P<sep>[\s\-\/\.])(\d{1,2}|gen(naio)?|feb(braio)?|mar(zo)?|apr(ile)?|mag(gio)?|giu(gno)?|lug(lio)?|ago(sto)?|set(tembre)?|ott(obre)?|nov(embre)?|dic(embre)?)(?P=sep)(18\d{2}|19\d{2}|20\d{2}))"
```
- Extract all dates matching this pattern
- Check if `DATA_ATTESA` matches any extracted date
- If NO match: add `"DA – <nomedelfile.pdf>"` to `<Descrizione>`

*Front page check (PRIMA PAGINA):*
Search page 1 TXT for any keyword in: `["abbonamento", "direzione", "direttore", "redazione", "amministrazione"]`
- If NO keyword found: add `"PP – <nomedelfile.pdf>"` to `<Descrizione>`

**Step 3 — Save monitoring data:**
1. Count PDFs in `/_tmp/...` → `<nroFileOK>`
2. Count PDFs in `/_errata/...` → `<nroFileKO>`
3. `<nroFileED>` = `<nroFileOK>` + `<nroFileKO>`

Insert `GDP_LOG_EDIZIONE`:
- `TIPO_EDIZIONE = <tipoEdizione>`
- `PATH_EDIZIONE = "/<rootFTP>/_tmp/<cartellaTestata>/<dataEdizione>"` ← **required (v2)**
- `NRO_PAG_ACQUISITE = <nroFileED>`
- `NRO_PAG_VALIDE = <nroFileOK>`
- `NRO_PAG_ERRATE = <nroFileKO>`
- `PRIMA_PAGINA = True/False`
- If `<Descrizione>` not empty: set `DESCRIZIONE = <Descrizione>` and change `GDP_LOG.ESITO` prefix from `<MSG>` to `<WRN>`

**Step 4 — Invoke F08 (synchronous):**
Pass: `<IDTestata>`, `<path>`, `<dataEdizione>`, `<IDLog>`
- On success: update `GDP_LOG_EDIZIONE.FK_GDP_EDIZIONE = <IDEdizione>`
- On failure: move folder to `/_errata/`, set `GDP_LOG.ESITO = MSG00002`, terminate MSG00002

**Step 5 — Invoke F09 (synchronous):**
Pass: `<IDTestata>`, `<IDLog>`, `<IDEdizione>`, `<Priorità>=0`
- On success: invoke **F10 asynchronously** with `<IDLog>`, `<IDEdizione>`, `<nomeFileZIP>` → terminate MSG00009
- On failure: move folder to `/_errata/`, set `GDP_LOG.ESITO = MSG00003`, terminate MSG00003

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | OK |
| MSG00001 | `<E002>` Anomalia DATA EDIZIONE — blocking date anomaly |
| MSG00002 | `<E003>` Anomalia EDIZIONE — DB insert failed (F08 message embedded) |
| MSG00003 | `<E004>` Anomalia EDIZIONE — DAM package creation failed (F09 message embedded) |

---

### F05 — FTPregolare.sospensioneEdizioneAttesa

**Trigger:** Synchronous, invoked by BFF (monitoring console)  
**Purpose:** Suspend automatic acquisition for a defined period

**Input:**
| # | Parameter | Rules | Type | Required |
|---|-----------|-------|------|----------|
| 1 | testata | ID_TESTATA | Integer | Yes |
| 2 | dataInizio | yyyy-mm-dd | Date | Yes |
| 3 | dataFine | yyyy-mm-dd | Date | Yes |

**Output:**
| # | Field | Content | Type |
|---|-------|---------|------|
| 0 | esito | MSG00009 or MSG0000x | String |
| 1 | giorniEdizione | Number of suspension days inserted | Integer |

**Steps:**
1. Read testata ID, date range from input
2. Query `GDP_DATA_USCITA` for expected dates in range
3. If empty → MSG00001
4. In `GDP_PERIODICITA`: set `INIZIO_SOSPENSIONE = dataInizio`, `FINE_SOSPENSIONE = dataFine`
5. In `GDP_DATA_USCITA`: set `SOSPESA = True` for all dates found in step 2

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | OK |
| MSG00001 | No occurrences found for input parameters |

---

### F18 — FTPregolare.verifDateAttese

**Trigger:** Synchronous, invoked by BFF  
**Purpose:** Retrieve expected dates with suspension status for a period

> **Note (v2 SFU-01):** Output field `cartellaTestata` was added in version 2. Must be included.

**Input:**
| # | Parameter | Rules | Type | Required |
|---|-----------|-------|------|----------|
| 1 | dataInizio | yyyy-mm-dd | Date | Yes |
| 2 | dataFine | yyyy-mm-dd | Date | Yes |
| 3 | idTestata | If empty, all testate with `INVIO_EDIZIONI=1` | Integer | No |

**Output:**
| # | Field | Content | Type |
|---|-------|---------|------|
| 0 | esito | MSG00009 or MSG0000x | String |
| 2 | elenco | Array, one entry per testata | Array |
| 2.1 | idTestata | | Integer |
| 2.2 | cartellaTestata | `GDP_TESTATA.CARTELLA_TESTATA` — **added v2** | String |
| 2.3 | DataEdizioneAttesa | | Date |
| 2.4 | Sospesa | | Boolean |

**Steps:**
1. Retrieve `idTestata` and `nomeTestata` from `GDP_TESTATA` according to input params
2. For each testata: select from `GDP_PERIODICITA` where `ID_GDP_PERIODICITA = ID_GDP_TESTATA`
3. Select `DATA_ATTESA` and `SOSPESA` from `GDP_DATA_USCITA` in date range
4. If no results → MSG00001

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | OK |
| MSG00001 | No results found |

---

## Service: FTPsaltuario — Historical Flow

### F06 — FTPsaltuario.checkConsegnaStorico

**Trigger:** Scheduled daily, evening hours (01:00)  
**Purpose:** Scan SFTP for new historical edition deliveries; verify each folder maps to a unique known testata

**Steps:**
1. Scan `/<rootFTP>/flusso_saltuario/` for new deliveries
2. If nothing found → MSG00001
3. Found a delivery `"/CONS_yyyy-mm-dd"` in `/<rootFTP>/flusso_saltuario/<utenteStorico>/`
4. For each subfolder (= one testata):
   - Set `<cartellaTestata>` = folder name
   - Set `<dataAcquisizione>` = folder timestamp (format `yyyy-mm-dd HH:MM:SS`)
   - Set `<dataConsegna>` = acquisition date (format `yyyy-mm-dd`)
   - Query `GDP_TESTATA` where `CARTELLA_TESTATA = <cartellaTestata>`
   
   **If multiple matches:**
   - Move folder content to `/<rootFTP>/_errata/<utenteStorico>/CONS_<dataConsegna>/<cartellaTestata>/`
   - Count files → `<nroTotFile>`
   - Insert `GDP_LOG`: `FK_GDP_TESTATA=0`, `DT_ACQUISIZIONE=<dataAcquisizione>`, `TOTALE_FILE_ACQUISITI=<nroTotFile>`, `ESITO=MSG00002`
   - No `GDP_LOG_EDIZIONE`; skip to next folder
   
   **If NO match (testata not found):**
   - Move folder content to `/<rootFTP>/_errata/<utenteStorico>/CONS_<dataConsegna>/<cartellaTestata>/`
   - Count files → `<nroTotFile>`
   - Insert `GDP_LOG`: `FK_GDP_TESTATA=0`, `DT_ACQUISIZIONE=<dataAcquisizione>`, `TOTALE_FILE_ACQUISITI=<nroTotFile>`, `ESITO=MSG00003`
   - No `GDP_LOG_EDIZIONE`; skip to next folder
   
   **If single match:**
   - Move all editions to `/<rootFTP>/_tmp/<utenteStorico>/CONS_<dataConsegna>/<cartellaTestata>/`
   - Insert `GDP_LOG`: `FK_TESTATA=<IDTestata>`, `DT_ACQUISIZIONE=<dataAcquisizione>`
   - Invoke **F07 asynchronously**: `<IDTestata>`, `<cartellaTestata>`, `<dataConsegna>`, `<IDLog>`
   - Continue to next folder

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | OK |
| MSG00001 | No new historical delivery found |
| MSG00002 | `<E101>` Anomalia UNICITA' — multiple testata IDs found |
| MSG00003 | `<E102>` Anomalia ESISTENZA — testata ID not found |

---

### F07 — FTPsaltuario.ctrlEdizioniStoriche

**Trigger:** Async, invoked by F06  
**Purpose:** Control all editions of a single historical testata delivery

**Input:**
| # | Parameter | Rules | Type | Required |
|---|-----------|-------|------|----------|
| 1 | testata | ID_TESTATA from DB | Integer | Yes |
| 2 | cartellaTestata | CARTELLA_TESTATA from DB | String | Yes |
| 3 | dataConsegna | yyyy-mm-dd | Date | Yes |
| 4 | log | ID_LOG from DB | Integer | Yes |

**Processing path:** `/<rootFTP>/_tmp/<utenteStorico>/CONS_<dataConsegna>/<nomeTestata>/`

For each edition subfolder found:

**Date format validation:**
- Valid format: `yyyymmdd` with coherent mm and dd values
- If VALID: `<tipoEdizione> = "ST"` (storica), increment `<nroEdizioni>`
- If INVALID: `<tipoEdizione> = "AS"` (anomala storica), increment `<nroEdizioniERR>`, move to `/_errata/`, record `GDP_LOG_EDIZIONE` with `NRO_PAG_ACQUISITE=0`, terminate this edition

**File naming validation (for each file in edition folder):**  
Required pattern: `<nomeTestata>-<SiglaPROVINCIA>-<dataEdizione>_<nro_pag>.<estensione>`  
where `<nro_pag>` is 3 digits.

- PDF non-conforming → move to `/_errata/`, add `"NF – <nomedelfile.pdf>"` to description
- TXT non-conforming → move to `/_errata/`, add `"NF – <nomedelfile.txt>"` to description
- TIF non-conforming → move to `/_errata/`, add `"NF – <nomedelfile.tif>"` to description
- TIF missing → add `"NF – file TIF mancante"` to description

**Counts per edition:**
- `<nroPDFOKedizione>`, `<nroTXTok>`, `<nroTIFok>` — valid files
- `<nroPDFko>`, `<nroTXTko>`, `<nroTIFko>` — invalid files

**Insert `GDP_LOG_EDIZIONE`:**
- `TIPO_EDIZIONE = <tipoEdizione>`
- `PATH_EDIZIONE = /<rootFTP>/_tmp/<utenteStorico>/CONS_<dataConsegna>/<nomeTestata>/<dataEdizione>` ← **required (v2)**
- `NRO_PAG_ACQUISITE = <nroPDFedizione>`
- `NRO_PAG_VALIDE = <nroPDFOKedizione>`
- `NRO_PAG_ERRATE = <nroPDFKOedizione>`
- `DESCRIZIONE = <Descrizione>` if not empty

**Invoke F08 (synchronous):**
- On success: update `GDP_LOG_EDIZIONE.FK_GDP_EDIZIONE = <IDEdizione>`
- On failure: move folder to `/_errata/`, add MSG00003 to esito, proceed to next edition

**Invoke F09 (synchronous) with `<Priorità>=100`:**
- On success: invoke **F10 asynchronously**, proceed to next edition
- On failure: move folder to `/_errata/`, add MSG00004 to esito, proceed to next edition

**After all editions processed:**
- Calculate totals: `<nroPDF>`, `<nroTXT>`, `<nroTIF>`
- Set `esito = MSG00009`
- Update `GDP_LOG.ESITO = <esito>`

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | Elaboration completed for testata |
| MSG00001 | `<E103>` Edition with wrong date format |
| MSG00002 | Edition moved (format error) |
| MSG00003 | `<E104>` DB insert failed (F08 error) |
| MSG00004 | `<E105>` DAM package failed (F09 error) |

---

## Service: DB

### F08 — DB.insEdizione

**Trigger:** Synchronous, invoked by F04 (periodic) or F07 (historical)  
**Purpose:** Insert or update edition and page records in DB

**Input:**
| # | Parameter | Rules | Type | Required |
|---|-----------|-------|------|----------|
| 1 | idTestata | ID_TESTATA from DB | Integer | Yes |
| 2 | path | Staging path (SFTP) | String | Yes |
| 3 | dataEdizione | yyyy-mm-dd | Date | Yes |
| 4 | log | ID_GDP_LOG from DB | Integer | Yes |

**Output:**
| # | Field | Content | Type |
|---|-------|---------|------|
| 0 | esito | MSG00009 or MSG0000x | String |
| 1 | edizione | ID_EDIZIONE | Integer |

**Check for existing edition:**  
Query `GDP_EDIZIONE` where `FK_GDP_EDIZIONE = <idTestata>` AND `DATA_EDIZIONE = <dataEdizione>`

**INSERT (new edition):**

Count PDFs in `<path>` → `<nroPDF>`

Insert `GDP_EDIZIONE`:
- `FK_TESTATA = ID_TESTATA`
- `DATA_EDIZIONE = <dataEdizione>`
- `DATA_PUBBLICAZIONE = <dataEdizione> + gg(periodicità)` — see table below
- `STATO = 0`
- `NUMERO_PAGINE = <nroPDF>`

**DATA_PUBBLICAZIONE calculation table:**

| Periodicity | Days to add (× 2 occurrences, ceiling) |
|-------------|----------------------------------------|
| quotidiano (daily) | 1 |
| bisettimanale | 3.5 → ceil = 4 |
| trisettimanale | 2.34 → ceil = 3 |
| quadrisettimanale | 1.75 → ceil = 2 |
| settimanale (weekly) | 7 |
| quattordicinale | 14 |
| quindicinale | 15 |
| mensile (monthly) | 30 |
| bimestrale | 60 |
| trimestrale | 90 |
| quadrimestrale | 120 |

Insert one `GDP_PAGINA` record per PDF file:
- `FK_TESTATA = ID_TESTATA`
- `FK_EDIZIONE = ID_EDIZIONE`
- `NUM_PAGINA = numeroPagina` (extracted from filename)
- `FILE_PDF = nome file pdf (with extension)`
- `FILE_TXT = nome file txt (with extension)`
- `FILE_TIF = NULL`
- `DATA_EDIZIONE = EDIZIONE.DATA_EDIZIONE`
- `STATO = 0`
- `DATA_OBLIO = NULL`, `OBLIO = NULL`, `NOTA_OBLIO = NULL`

**UPDATE (existing edition):**
- Count PDFs → `<nroPDF>`
- Update `GDP_EDIZIONE.NUMERO_PAGINE = <nroPDF>` if different
- For each PDF: INSERT new page if `NUM_PAGINA` not existing, else UPDATE existing record (same fields as above)

**Atomicity:** The INSERT of `GDP_EDIZIONE` and all `GDP_PAGINA` records MUST occur in a single transaction. Partial failure triggers full rollback.

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | OK |
| MSG00001 | Error inserting GDP_EDIZIONE |
| MSG00002 | Error inserting GDP_PAGINA |

---

### F16 — DB.getElencoTestate

**Trigger:** Synchronous, invoked by BFF  
**Purpose:** Return list of testate with optional filters

**Input (filters are mutually exclusive):**
| # | Parameter | Type |
|---|-----------|------|
| 1 | idTestata | Integer |
| 2 | invioEdizione | String |
| 3 | prov | String |

**Output:** List of testate with: `idTestata`, `NomeTestata`, `CartellaTestata`, `invioEdizione`, `Prov`

---

### F17 — DB.getTestata

**Trigger:** Synchronous, invoked by BFF  
**Purpose:** Return all fields of a single testata

**Input:** `idTestata` (Integer, required)  
**Output:** All fields from `GDP_TESTATA`

---

## Service: DAMtrasmissione

### F09 — DAMtrasmissione.creaXMLEdizione

**Trigger:** Synchronous, invoked by F04 or F07  
**Purpose:** Generate XML metadata + .zip package for DAM transmission

**Input:**
| # | Parameter | Rules | Type | Required |
|---|-----------|-------|------|----------|
| 1 | testata | ID_TESTATA | Integer | Yes |
| 2 | idLog | ID_LOG | Integer | Yes |
| 3 | IDEdizione | ID_EDIZIONE | Integer | Yes |
| 4 | priorità | 0 = daily editions, 100 = historical editions | Integer | Yes |

**Output:**
| # | Field | Content | Type |
|---|-------|---------|------|
| 0 | esito | MSG00009 or MSG0000x | String |
| 1 | nomeFileCompresso | Generated zip filename | String |

**Steps:**
1. Read `GDP_TESTATA` by `ID_TESTATA` → compile `<xs:element name="testata">` metadata
2. Read `GDP_EDIZIONE` by `ID_EDIZIONE` → compile `<xs:element name="edizione">` metadata
3. Read `GDP_PAGINA` where `FK_GDP_EDIZIONE = idEdizione` → compile `<xs:element name="pagina">` for each page
4. Create XML file: `<NomeTestata>.<dataEdizione>.xml` (date in `yyyy-mm-dd` format)
   - Schema: `GdP-STD-04-V01-Validazione_metadati_flussoFTP.xsd` ← **must be available before F09 implementation**
5. Compress XML + all PDFs + TXTs into `<NomeTestata>.<dataEdizione>.zip`
6. Update `GDP_LOG_EDIZIONE`: set `FILE_XML = True`
7. Insert `GDP_IMPORT_TASK` record:
   - `DATA_INSERIM_IN_CODA = sysdate (yyyy-mm-dd HH:MM:SS,CC)`
   - `FK_GDP_LOG_EDIZIONE = ID_GDP_LOG_EDIZIONE` (retrieved via `<idLog>`)
   - `NRO_TENTATIVO = 0`
   - `DATA_TENTATIVO = NULL`
   - `SFTP_PATH = "/<rootFTP>/_dam/<NomeTestata>.<dataEdizione>.zip"`
   - `PRIORITA = <Priorità>` (0 or 100)
   - `STATO = 'PRO'`
8. Deposit .zip in `/<rootFTP>/_dam/`
9. Return MSG00009 with zip filename

**Error handling:**
- XML creation error → set `GDP_LOG_EDIZIONE.FILE_XML = False`, return MSG00002
- ZIP creation error → set `GDP_LOG_EDIZIONE.FILE_ZIP = False`, return MSG00003

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | OK |
| MSG00002 | Error creating XML file |
| MSG00003 | Error creating ZIP file |

---

### F10 — DAMtrasmissione.inviaEdizione

**Trigger:** Scheduled every 30 minutes  
**Purpose:** Send queued .zip files to DAM LIBRA

**Prerequisite:** DAM authentication token must be pre-generated from public/private key pair (see UC-01). For testing, a token will be provided externally.

**Input:**
| # | Parameter | Rules | Type | Required |
|---|-----------|-------|------|----------|
| 1 | idLog | ID_LOG | Integer | Yes |
| 2 | nomeFileEdizione | .zip filename | String | Yes |

**Steps:**
1. Query `GDP_CODA_CARICAMENTO` (= `GDP_IMPORT_TASK`) for records where `STATO = 'READY'`
2. Order by: `PRIORITA ASC`, `DATA_INSERIM_IN_CODA ASC` (daily editions before historical)
3. For each record: read `SFTP_PATH`, retrieve `<nomeFileZIP>`
4. Call DAM LIBRA:
   - **Base URL (TEST):** `http://ts-libra-sv-exp1.csi.it/rpcr02`
   - **Endpoint:** `POST /api/v2/imports`
   - **Payload:** multipart containing .zip file (XML metadata + PDFs + TXTs)
   - **Auth:** Bearer token from `LIBRA_API_KEY` (injected via `ClientRequestFilter`)
5. Wait for response, extract `"jobId"` and `"status"` fields

**If `status == "FAILED"`:**
- Update `GDP_LOG_EDIZIONE`: `JOB_ID = jobId`, `STATO = 'FAILED'`
- Update `GDP_LOG.ESITO = MSG00001`
- Continue to next record

**If `status == "SUBMITTED"`:**
- Update `GDP_LOG_EDIZIONE`: `FILE_ZIP = True`, `JOB_ID = jobId`, `STATO = 'SUBMITTED'`
- Update `GDP_LOG.ESITO = MSG00009`
- Delete .zip from `/<rootFTP>/_dam/`
- Continue to next record

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | `<MSG>` DAM upload executed |
| MSG00001 | `<E005>` Anomalia EDIZIONE DAM — transmission failed |

---

### F19 — DAMtrasmissione.pulisciEdizione

**Trigger:** Scheduled every 24 hours  
**Purpose:** Clean up staged edition files after confirmed DAM transmission

**Status:** Detail TBD (marked "To BE DO" in SFU-01 v2)  
**Priority:** SHOULD — implement as placeholder, detail to be defined with analysis team

---

## Service: MONITOR — REST APIs for BFF

These REST endpoints are consumed exclusively by `gdpbff` to feed the Back Office SPA.  
**Version note:** SFU-01-V03 significantly expands this service. F14 is now split into `preparaMAIL` (F14) and `invioMAIL` (F22). F15 is now fully specified. Three new operations added: F20 `statoDAM`, F21 `attivaCODA`, F22 `invioMAIL`.

> **Important:** The `tipoEdizione` field in F13/F15 is decoded via the lookup table `GDP_TIPO_EDIZIONE.DESCRIZIONE`, not hardcoded in application logic.

---

### F12 — MONITOR.elencoAcquisizioni

**Trigger:** Synchronous, invoked by BFF back-end  
**Purpose:** Return list of acquisitions filtered by type and date

**Input:**
| # | Parameter | Rules | Type | Required |
|---|-----------|-------|------|----------|
| 1 | tipoAcquisizione | 'G'=daily, 'S'=historical | String | Yes |
| 2 | dataAcquisizione | yyyy-mm-dd | Date | Yes |

**Output:**
| # | Field | Description | Type |
|---|-------|-------------|------|
| 0 | esito | MSG00009 or MSG0000x | String |
| 1 | elenco | List of "edizioni monitorate" | Array |
| 1.1 | idLog | `GDP_LOG.ID_GDP_LOG` | Integer |
| 1.2 | idUtenteSFTP | | Integer |
| 1.3 | idTestata | `GDP_LOG.FK_GDP_TESTATA` | Integer |
| 1.4 | nomeTestata | `GDP_TESTATA.NOME_TESTATA` | String |
| 1.5 | nroEdizioni | If 'G': always 1. If 'S': count of `GDP_LOG_EDIZIONE` records for idLog | Integer |
| 1.6 | dataEdizione | `GDP_EDIZIONE.DATA_EDIZIONE` | Date |
| 1.7 | dataAcquisizione | `GDP_LOG.DT_ACQUISIZIONE` | Date |
| 1.8 | nroTotFile | `GDP_LOG.TOTALE_FILE_ACQUISITI` | Integer |
| 1.9 | esito | `GDP_LOG.ESITO` | String |

**Implementation steps:**
1. Query `GDP_LOG` where `TIPO_ACQUISIZIONE = <tipoAcquisizione>` AND `DT_ACQUISIZIONE = <dataAcquisizione>`
2. For each record found, join: `GDP_TESTATA`, `GDP_EDIZIONE`, `GDP_LOG_EDIZIONE`
3. Populate output list with field mapping above
4. If no records found → MSG00001

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | OK |
| MSG00001 | Error retrieving data |

---

### F13 — MONITOR.dettaglioAcquisizione

**Trigger:** Synchronous, invoked by BFF  
**Purpose:** Return full detail of a single acquisition with all validation fields

**Input:**
| # | Parameter | Type | Required |
|---|-----------|------|----------|
| 1 | idLog | Integer | Yes |

**Output:**
| # | Field | DB Mapping | Type |
|---|-------|-----------|------|
| 0 | esito | MSG00009 or MSG0000x | String |
| 1 | elenco | List of "Edizioni" | Array |
| 1.1 | idLog | `GDP_LOG.ID_GDP_LOG` | Integer |
| 1.2 | IDTestata | `GDP_LOG.FK_GDP_TESTATA` | Integer |
| 1.3 | nomeTestata | `GDP_TESTATA.NOME_TESTATA` | String |
| 1.4 | dataEdizione | `GDP_EDIZIONE.DATA_EDIZIONE` | Date |
| 1.5 | tipoEdizione | Decoded via `GDP_TIPO_EDIZIONE.DESCRIZIONE` | String |
| 1.6 | tipoAcquisizione | `GDP_LOG.TIPO_ACQUISIZIONE` → G=Giornaliera, S=Storica | String |
| 1.7 | dataAcquisizione | `GDP_LOG.DT_ACQUISIZIONE` | Date |
| 1.8 | NroTotFile | `GDP_LOG.TOTALE_FILE_ACQUISITI` | Integer |
| 1.9 | Esito | `GDP_LOG.ESITO` | String |
| 1.10 | idEdizione | `GDP_LOG_EDIZIONE.FK_GDP_EDIZIONE` | Integer |
| 1.11 | primaPagina | `GDP_LOG_EDIZIONE.PRIMA_PAGINA` → True=SI, False=NO | String |
| 1.12 | fileXML | `GDP_LOG_EDIZIONE.FILE_XML` → True=SI, False=NO | String |
| 1.12 | fileZIP | `GDP_LOG_EDIZIONE.FILE_ZIP` → True=SI, False=NO | String |
| 1.13 | nroPagAcq | `GDP_LOG_EDIZIONE.NRO_PAG_ACQUISITE` | Integer |
| 1.14 | nroPagOK | `GDP_LOG_EDIZIONE.NRO_PAG_VALIDE` | Integer |
| 1.15 | nroPagErrate | `GDP_LOG_EDIZIONE.NRO_PAG_ERRATE` | Integer |
| 1.16 | jobID | `GDP_LOG_EDIZIONE.JOB_ID` | Integer |
| 1.17 | descrizione | `GDP_LOG_EDIZIONE.DESCRIZIONE` | String |

**Implementation steps:**
1. Query `GDP_LOG` where `ID_GDP_LOG = <idLog>`
2. Query `GDP_LOG_EDIZIONE` for corresponding record
3. Join `GDP_TESTATA` and `GDP_EDIZIONE`
4. Map all output fields as specified above
5. Decode `tipoEdizione` via `GDP_TIPO_EDIZIONE.DESCRIZIONE` lookup (NOT hardcoded)
6. If not found → MSG00001

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | OK |
| MSG00001 | Error retrieving monitoring data |

---

### F14 — MONITOR.preparaMAIL

> **Breaking change in V03:** F14 has been renamed from `invioMAIL` to `preparaMAIL`. It now only **prepares** the mail payload (from, to, host, port, subject, body) and returns it as output. The actual **sending** is delegated to the new **F22 — MONITOR.invioMAIL**. The BFF must call F14 first, then pass the result to F22.

**Trigger:** Synchronous, invoked by BFF  
**Purpose:** Compose email notification data for an acquisition anomaly — does NOT send the email

**Input:**
| # | Parameter | Type | Required |
|---|-----------|------|----------|
| 1 | idLog | Integer | Yes |
| 2 | tipoMail | String | Yes |

**Output:**
| # | Field | DB Source | Type |
|---|-------|-----------|------|
| 0 | esito | MSG00009 or MSG0000x | String |
| 1 | from | `GDP_MAIL.MITTENTE` | String |
| 2 | to | `GDP_UTENTE_FTP.EMAIL` | String |
| 3 | host | | String |
| 4 | porta | | Integer |
| 5 | oggetto | Composed from `GDP_MAIL` template with placeholders replaced | String |
| 6 | testo | Composed from `GDP_MAIL` template with placeholders replaced | String |

**Implementation steps:**
1. Query `GDP_LOG` where `ID_GDP_LOG = <idLog>`
2. Via `FK_GDP_UTENTE_FTP` → query `GDP_UTENTE_FTP`, retrieve `EMAIL` → save as `<to>`. If not defined → MSG00001
3. Via `FK_GDP_TESTATA` → query `GDP_TESTATA`, retrieve `NOME_TESTATA` → save as `<nomeTestata>`
4. If `<tipoMail>` ≠ `"STnnn"` → via `GDP_LOG_EDIZIONE.FK_GDP_EDIZIONE` → query `GDP_EDIZIONE`, retrieve `DATA_EDIZIONE` → save as `<dataEdizione>`
5. Query `GDP_MAIL` where `COD_MAIL = <tipoMail>`
6. Replace placeholders `<[dataED]>` and `<[nomeTestata]>` in the template with respective values
7. Return composed mail fields (does NOT send)

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | OK |
| MSG00001 | Email address not found |
| MSG00002 | Mail send failed (legacy — now raised by F22) |

---

### F15 — MONITOR.ricercaAcquisizioni

**Trigger:** Synchronous, invoked by BFF  
**Purpose:** Historical search of acquisitions with multi-criteria filtering  
**Priority:** SHOULD (now fully specified in V03)

**Input:**
| # | Parameter | Values / Rules | Type | Required |
|---|-----------|---------------|------|----------|
| 1 | tipoAcquisizione | `"G"` = daily, `"S"` = historical | Integer | Yes |
| 2 | testata | Unique testata identifier | String | Yes |
| 3 | dataDA | Start date | Date | Optional |
| 4 | dataA | End date | Date | Yes |
| 5 | tipoEdizione | `"OK"` = corrispondente, `"SO"` = sospesa, `"AN"` = anticipataria, `"PO"` = posticipataria, `"AA"` = anomalia edizione attesa, `"ST"` = edizione storica, `"AS"` = anomalia edizione storica | String | Yes |

**Output:**
| # | Field | DB Mapping | Type |
|---|-------|-----------|------|
| 0 | esito | MSG00009 or MSG0000x | String |
| 1 | elenco | List of "edizioni monitorate" | Array |
| 1.1 | idLog | `GDP_LOG.ID_GDP_LOG` | Integer |
| 1.2 | idTestata | `GDP_TESTATA.ID_TESTATA` (via `GDP_LOG.FK_GDP_TESTATA`) | Integer |
| 1.3 | nomeTestata | `GDP_TESTATA.NOME_TESTATA` | String |
| 1.4 | tipoEdizione | `GDP_LOG_EDIZIONE.TIPO_EDIZIONE` | String |
| 1.5 | dataEdizione | `GDP_EDIZIONE.DATA_EDIZIONE` (via `GDP_LOG_EDIZIONE.FK_GDP_EDIZIONE`) | Date |
| 1.6 | dataAcquisizione | `GDP_LOG.DT_ACQUISIZIONE` | Date |
| 1.7 | nroTotFileAcq | `GDP_LOG_EDIZIONE.NRO_PAG_ACQUISITE` | Integer |
| 1.8 | nroTotFileVal | `GDP_LOG_EDIZIONE.NRO_PAG_VALIDE` | Integer |

**Implementation steps:**

Query `GDP_LOG` with these filters:
- `GDP_LOG.TIPO_ACQUISIZIONE = <tipoAcquisizione>`
- `GDP_LOG.FK_GDP_TESTATA = <idTestata>`
- `GDP_LOG.DT_ACQUISIZIONE >= dataDA` (if provided)
- `GDP_LOG.DT_ACQUISIZIONE <= dataA`
- `GDP_LOG_EDIZIONE.TIPO_EDIZIONE = <tipoEdizione>`

Compose output list from joins with `GDP_TESTATA`, `GDP_EDIZIONE`, `GDP_LOG_EDIZIONE`.  
If no records found → MSG00001

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | OK |
| MSG00001 | No data found |

---

### F20 — MONITOR.statoDAM *(new in V03)*

**Trigger:** Synchronous, invoked by BFF  
**Purpose:** Check the current processing status of a DAM import job by querying LIBRA directly

**Input:**
| # | Parameter | Type | Required |
|---|-----------|------|----------|
| 1 | jobID | Integer | Yes |

**Output:**
| # | Field | Content | Type |
|---|-------|---------|------|
| 0 | esito | MSG00009 (with status value) or MSG00001 | String |

**Implementation steps:**
1. Call DAM LIBRA:
   - **Base URL (TEST):** `http://ts-libra-sv-exp1.csi.it/rpcr02`
   - **Endpoint:** `GET /api/v2/success` with `jobID` as parameter
2. Extract `"status"` field from response
3. Return MSG00009 with `"Stato edizione <status>"` on success
4. Return MSG00001 `"Dato non trovato"` on failure

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | `Stato edizione <status>` |
| MSG00001 | Data not found |

---

### F21 — MONITOR.attivaCODA *(new in V03)*

**Trigger:** Synchronous, invoked by BFF (Back Office retry action)  
**Purpose:** Re-enqueue a failed DAM transmission task for retry — replaces the direct DB write previously documented as the "retry" endpoint

> **Important:** This is the correct implementation of the retry mechanism. The BFF `POST /bo/acquisizioni/{idLog}/retry` endpoint must call **F21**, not write directly to `GDP_IMPORT_TASK`. F21 enforces the maximum retry limit via `NRO_MAX_TENTATIVI`.

**Input:**
| # | Parameter | Type | Required |
|---|-----------|------|----------|
| 1 | idLog | Integer | Yes |

**Output:**
| # | Field | Content | Type |
|---|-------|---------|------|
| 0 | esito | MSG00009 or MSG00001 | String |

**Implementation steps:**
1. Via `GDP_LOG` where `ID_GDP_LOG = <idLog>`
2. Via `GDP_LOG_EDIZIONE` where `FK_GDP_LOG = ID_GDP_LOG`
3. Access `GDP_CODA_CARICAMENTO` where `FK_GDP_LOG_EDIZIONE = ID_GDP_LOG_EDIZIONE`
4. Update the record:
   - `DATA_INSERIMENTO_IN_CODA = current timestamp`
   - `STATO = "READY"`
   - `NRO_TENTATIVO = NRO_TENTATIVO + 1`
5. If updated `NRO_TENTATIVO <= NRO_MAX_TENTATIVI` → MSG00009
6. If updated `NRO_TENTATIVO > NRO_MAX_TENTATIVI` → MSG00001 (max retries exceeded — alert operator)

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | `Tentativo di invio numero <NRO_TENTATIVO> attivato` |
| MSG00001 | `ATTENZIONE! Superato il numero massimo di tentativi ammessi` |

---

### F22 — MONITOR.invioMAIL *(new in V03)*

**Trigger:** Synchronous, invoked by BFF  
**Purpose:** Actually send the email whose payload was prepared by F14. F14 and F22 together replace the old single-step mail sending.

> **Calling sequence:** BFF calls F14 → receives `{from, to, host, porta, oggetto, testo}` → passes these directly as input to F22.

**Input:**
| # | Parameter | Type | Required |
|---|-----------|------|----------|
| 1 | from | String | Yes |
| 2 | to | String | Yes |
| 3 | host | String | Yes |
| 4 | porta | Integer | Yes |
| 5 | oggetto | String | Yes |
| 6 | testo | String | Yes |

**Output:**
| # | Field | Content | Type |
|---|-------|---------|------|
| 0 | esito | MSG00009 or MSG00001 | String |

**Implementation steps:**
1. Use the input parameters to send the email via the configured mail server
2. On success → MSG00009
3. On failure → MSG00001

**Error codes:**
| Code | Description |
|------|-------------|
| MSG00009 | OK |
| MSG00001 | Mail send failed |

---

## Non-Functional Requirements

### Performance
| ID | Requirement | Priority |
|----|-------------|----------|
| RNF-01 | Polling-to-processing latency ≤ 15 minutes under normal conditions | MUST |
| RNF-02 | Throughput: ≥ 50 editions/day in steady state, peaks up to 200/day for historical bulk loads | MUST |
| RNF-03 | DAM call timeout: configurable, default 120 seconds. Exceeded = task remains queued | MUST |
| RNF-04 | Service operational within 10 seconds from deploy (JVM mode). First polling cycle within 1 minute | SHOULD |

### Reliability and Idempotency
| ID | Requirement | Priority |
|----|-------------|----------|
| RNF-05 | Polling idempotency: double file detection (e.g., restart) must NOT create duplicates in GDP_EDIZIONE | MUST |
| RNF-06 | No data loss: file MUST NOT be deleted from SFTP without DAM confirmation | MUST |
| RNF-07 | Auto-recovery: on restart, automatically resume tasks in 'PRO' state from GDP_IMPORT_TASK | MUST |
| RNF-08 | Non-blocking errors: failure on one edition MUST NOT interrupt polling for others | MUST |

### Security
| ID | Requirement | Priority |
|----|-------------|----------|
| RNF-09 | SFTP auth via RSA/ED25519 private key. No plaintext passwords in code or repository | MUST |
| RNF-10 | All credentials (SFTP key path, DAM URL, DB password) from env vars or Kubernetes Secrets. Never hardcoded | MUST |
| RNF-11 | DAM REST calls must include auth header (API Key or Bearer token) injected via ClientRequestFilter | MUST |
| RNF-12 | DB connection with dedicated PostgreSQL user — restricted permissions, no SUPERUSER | MUST |

### Observability
| ID | Requirement | Priority |
|----|-------------|----------|
| RNF-13 | Structured JSON logging in production (ELK/Loki integration). Human-readable in dev | MUST |
| RNF-14 | Correlation ID: each edition elaboration traced with a unique ID across all logs | SHOULD |
| RNF-15 | Custom Micrometer metrics: `edizioni_elaborate_total`, `edizioni_errore_total`, `elaborazione_durata_secondi` (histogram). Exposed at `/q/metrics` in Prometheus format | SHOULD |
| RNF-16 | OpenTelemetry tracing via `@WithSpan` for polling, validation, DAM send operations. Export to Jaeger/OTLP | COULD |
| RNF-17 | Health checks: `/q/health/live` (liveness) and `/q/health/ready` (readiness with SFTP check). Used by Kubernetes probes | MUST |

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `SFTP_HOST` | Yes | SFTP server hostname (e.g., `sftp.al01.csipiemonte.it`) |
| `SFTP_USER` | Yes | SFTP username |
| `SFTP_KEY` | Yes | Path to RSA private key mounted in container |
| `LIBRA_URL` | Yes | DAM LIBRA base URL |
| `LIBRA_API_KEY` | Yes | API Key for DAM LIBRA authentication |
| `DB_URL` | Yes | JDBC URL for PostgreSQL |
| `DB_USER` | Yes | Database username |
| `DB_PASSWORD` | Yes | Database password |
| `MAIL_URL` | Yes | Mail server URL for anomaly notifications |
| `GDP_POLLING_ENABLED` | No (default: true) | Enable/disable SFTP polling (useful for maintenance) |
| `GDP_POLLING_INTERVAL` | No (default: 15m) | Periodic flow polling interval |
| `GDP_STAGING_BASE_DIR` | No (default: /tmp/gdporch) | Local directory for temporary files |

---

## Acceptance Criteria

### AC-1: Nominal periodic flow
1. A single-page PDF with correct naming deposited on testata SFTP folder is detected within 15 minutes
2. File moved to `/_tmp/`, renamed to standard format, text extracted to `.txt`
3. One record inserted in `GDP_EDIZIONE` and one in `GDP_PAGINA` with `STATO=0`
4. `.zip` generated in `/_dam/` containing XML and PDF+TXT
5. `.zip` sent to DAM; `GDP_IMPORT_TASK` updated to `STATO='OK'`
6. Original SFTP file removed ONLY AFTER `STATO='OK'` in `GDP_IMPORT_TASK`

### AC-2: Multi-page PDF
7. PDF with 3 pages → split into 3 single-page files
8. `GDP_LOG_EDIZIONE.DESCRIZIONE` contains `'NP – <original_filename>'`
9. 3 separate files processed normally, 3 records in `GDP_PAGINA`

### AC-3: Anomalous edition (AA)
10. File with date not in `GDP_DATA_USCITA` (type AA) → all files moved to `/_errata/`
11. `GDP_LOG.ESITO` contains `MSG00001`
12. No records in `GDP_EDIZIONE` or `GDP_PAGINA`
13. Polling continues normally for other editions

### AC-4: DAM unavailable
14. When LIBRA unreachable: `.zip` remains in `/_dam/`, `GDP_IMPORT_TASK.STATO = 'ERR'`, `NRO_TENTATIVO` incremented
15. Original SFTP files NOT deleted
16. `/q/health/ready` returns DOWN when circuit breaker to LIBRA is open
17. On LIBRA restore: system automatically retries at next cycle

### AC-5: Metrics and observability
18. After 10 elaborations: `edizioni_elaborate_total` reflects correct count at `/q/metrics`
19. All logs for one elaboration contain consistent unique correlation ID
20. Dev UI (`/q/dev`) shows all registered CDI beans and active `@Scheduled` jobs

---

## Open Issues

| ID | Description | Impact |
|----|-------------|--------|
| OPEN-1 | `GG_PERIODICITA = NULL`: publisher must supply explicit date list. Loading mechanism to DB not defined. Clarify with analysis team before implementing F01. | RF-01 (F01) |
| OPEN-2 | XSD schema `GdP-STD-04-V01-Validazione_metadati_flussoFTP.xsd` availability. Any mismatch causes LIBRA rejection. Must be obtained before implementing F09. | F09 (DAM XML) |
| OPEN-3 | TIFF file handling in historical flow: routing to conservation storage vs DAM not fully specified. "Still to be defined" per UC-04. | F07 (historical) |
| OPEN-4 | F19 `pulisciEdizione` detail is marked "To BE DO" in SFU-01 v2. Clarify cleanup logic with analysis team. | F19 |
| OPEN-5 | DAM authentication token generation procedure described in UC-01 (Autenticazione). Ensure UC-01 is available before F10 implementation. | F10 |

---

## Out of Scope

| Functionality | Responsible Component |
|--------------|-----------------------|
| Monitoring UI | gdpbospa (Back Office SPA — Angular 19) |
| User and SFTP authentication | Shibboleth + IAM/RUPAR — CSI infrastructure |
| Testata and user registration | gdpbff + gdpbospa (Back Office) |
| Periodicity configuration from UI | gdpbospa — calls F01/F05 exposed by gdporch |
| Full-text search in archive | gdpfospa (Front Office SPA) via gdpbff → DAM LIBRA |
| PDF fasciculation and download | gdpbff — assembles PDF from LIBRA pages |
| GDPR right-to-oblio management | gdpbff — calls LIBRA for obscuring, updates GDP_PAGINA.OBLIO |
| DAM LIBRA content management | LIBRA native interface — accessible directly to CSI Manager |
| Kubernetes infrastructure provisioning | CSI Platforms/Datacenter team |

---

## Technical Constraints

- Quarkus 3.x mandatory — CSI stack compatibility
- Use `jakarta.*` namespace only (NOT `javax.*`)
- Java 17 on Adoptium Temurin — no other versions supported
- PostgreSQL 15 — do not use features specific to later versions
- Code generated by `openapi-generator` must NOT be versioned. Only `openapi.yaml` goes in the repository
- `target/generated-sources/` must be in `.gitignore`
- No SSH keys or credentials committed to git repository
- No SUPERUSER for DB connection