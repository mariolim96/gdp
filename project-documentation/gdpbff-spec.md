# gdpbff — Backend For Frontend: Complete Technical Specification

## Project Context

**System:** GDP — Giornali del Piemonte (Piedmont Newspapers)  
**Client:** Consiglio Regionale del Piemonte (CRP)  
**Plan Reference:** Piano di Sviluppo 2025 — rif. 2025.CR.CC.01  
**Component:** `gdpbff` (Quarkus 3.x backend service)  
**Role:** Backend For Frontend (BFF) — single HTTP entry point for both Angular SPAs  
**Pattern:** Backend For Frontend (BFF)  
**References:** UC-02 · UC-03 · UC-05 · UC-06 · SFU-01 · VDI-01  
**Version:** 1.0 (March 2026)  
**Status:** Draft — under review

### What gdpbff does

gdpbff is the **sole HTTP gateway** for two Angular applications:
- `gdpbospa` — Back Office SPA (internal CSI operators)
- `gdpfospa` — Front Office SPA (public web users and privileged users)

It has **no business logic of its own**: it aggregates, transforms, and routes requests. Its responsibilities are:
1. Expose a unified REST API contract (contract-first from `openapi.yaml`)
2. Implement authentication and role-based authorization as a cross-cutting concern
3. Orchestrate calls to `gdporch` (monitoring/config APIs), DAM LIBRA (search/assets), and PostgreSQL
4. Apply visibility rules: anonymous web users must NOT see the last 2 editions per active testata
5. Assemble PDF fascicoli from LIBRA pages
6. Manage circuit breakers, retries, and timeouts toward backend systems

---

## System Architecture Overview

```
gdpbospa (Angular 19)          gdpfospa (Angular 19)
         \                           /
          \    HTTPS                /
           ──────────┬─────────────
                     │
              gdpbff (Quarkus 3.x)
              /bo/**      /fo/**
                │
       ┌────────┼────────────────┐
       │        │                │
   gdporch   PostgreSQL 15   DAM LIBRA
   REST API   (direct)       REST API
   F12-F15                   /api/v2/...
   F01,F05
   F18
```

### Authentication Architecture

```
CSI Network (Back Office)
  Shibboleth SSO → JWT → @RolesAllowed(operatore)

INTERNET/RUPAR (Front Office)
  IAM RUPAR → JWT → @RolesAllowed(utentePrivilegiato)
  Anonymous → no JWT → utenteWeb (restricted visibility)
```

> **Important:** Shibboleth/IAM integration is managed by CSI infrastructure at the Kubernetes ingress level. gdpbff **receives already-validated JWTs**. The exact JWT format (claims, issuer, expiry) must be confirmed with the infrastructure team before implementing RF-01.

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Framework | Quarkus 3.x (jakarta.* — NOT javax.*) |
| JVM | Adoptium Temurin 17 |
| Deploy | Kubernetes cluster k8s-ol-prod01 |
| API Pattern | JAX-RS (RESTEasy Reactive) — contract-first from `openapi.yaml` |
| DTO Generation | `openapi-generator-maven-plugin` (jaxrs-spec, interfaceOnly=true) |
| Backend Clients | MicroProfile REST Client (`@RegisterRestClient`) |
| Fault Tolerance | SmallRye Fault Tolerance (`@Retry`, `@CircuitBreaker`, `@Timeout`, `@Fallback`) |
| ORM / DB | Hibernate ORM Panache |
| Auth / JWT | SmallRye JWT + `@RolesAllowed` |
| PDF Assembly | Apache PDFBox 3.x |
| JSON | Jackson (RESTEasy Reactive Jackson) |
| Bean Validation | Hibernate Validator + `@Valid` |
| Logging | JBoss Logging + JSON structured (prod) |
| Health / Metrics | SmallRye Health + Micrometer Prometheus |

**Critical constraint:** `openapi.yaml` MUST be agreed upon and signed off by both frontend teams (`bospa` and `fospa`) BEFORE any endpoint implementation begins. No coding without approved contract.

---

## Actors and Roles

| Actor | Authentication | System Role | Accessible Features |
|-------|---------------|-------------|---------------------|
| **Gestore CSI / Operatore** | CSI credentials — Shibboleth | `operatore` | Full Back Office: monitoring, testata management, SFTP users, suspensions, retry, GDPR oblio |
| **Utente privilegiato** | RUPAR or CRP email credentials | `utentePrivilegiato` | Front Office: search + recent editions + permanent fascicoli + favourites management |
| **Utente web libero** | None (anonymous access) | `utenteWeb` | Front Office: archive search (last 2 editions excluded) + temporary fascicoli |
| **gdpbospa (SPA)** | N/A — client, not a user | Angular client | Consumes `/bo/**` APIs from gdpbff |
| **gdpfospa (SPA)** | N/A — client, not a user | Angular client | Consumes `/fo/**` APIs from gdpbff |

---

## Visibility Rule — Last 2 Editions Filter

This rule is the most critical business rule in gdpbff and must be applied consistently across all Front Office endpoints.

**Rule:** For `utenteWeb` (anonymous or with `utenteWeb` role), exclude from all search results and edition listings any edition whose `DATA_PUBBLICAZIONE` is greater than the current date.

**Technical logic:**
- `DATA_PUBBLICAZIONE` is stored in `GDP_EDIZIONE` and calculated at acquisition time as: `DATA_EDIZIONE + gg(periodicità)` where `gg(periodicità)` represents two publication cycles (see gdporch spec for the complete table)
- "Last 2 editions" = the 2 most recent editions for each active testata, identified by `DATA_PUBBLICAZIONE > CURRENT_DATE`
- The filter is applied **at response level** by gdpbff before returning data to the frontend

**Roles NOT subject to the filter:** `utentePrivilegiato`, `operatore`

**Affected endpoints:**
- `GET /fo/ricerca`
- `GET /fo/ricerca/avanzata`
- `GET /fo/testate/{idTestata}/edizioni`
- `GET /fo/edizioni/{idEdizione}`

---

## API Contract — Back Office (/bo/**)

All Back Office APIs require authentication with role `operatore`. The `/bo/` prefix identifies endpoints reserved for `gdpbospa`.

### 4.1 — Testata Management (UC-02)

#### `GET /bo/testate`
**Description:** Filtered testata list  
**Query params:** `invioEdizione` (boolean), `prov` (string), `idTestata` (integer)  
**Backend:** Calls `F16 — DB.getElencoTestate` on gdporch  
**Implementation detail:** The three filters are **mutually exclusive** — validate that only one is provided at a time  
**UI behavior (UC-02):** The Back Office menu shows two radio buttons: "Tutte" (all) and "Solo invianti" (senders only). This maps to `invioEdizione` filter. The result populates a dropdown ordered by `idTestata`, displayed as `idTestata - nomeTestata`  
**UI behavior (UC-06):** Also used to populate the "Testata" dropdown in the ricerca cruscotto search form. In that context the dropdown shows `nomeTestata` (display) with `idTestata` stored as hidden value.

**Response body (200):**
```json
{
  "testate": [
    {
      "idTestata": 1,
      "nomeTestata": "La Sentinella del Canavese",
      "cartellaTestata": "sentinella",
      "invioEdizione": true,
      "provincia": "TO"
    }
  ]
}
```

---

#### `GET /bo/testate/{idTestata}`
**Description:** Complete anagrafica of a specific testata  
**Backend:** Calls `F17 — DB.getTestata` on gdporch (returns all `GDP_TESTATA` fields)

**Response body (200):** All fields from `GDP_TESTATA`:
```json
{
  "idTestata": 1,
  "nomeTestata": "string",
  "cartellaTestata": "string",
  "stato": "string",
  "cancellazione": "string",
  "codToma": "string",
  "codEditrice": "string",
  "annoFondazione": 0,
  "periodoDal": "string",
  "periodoAl": "string",
  "sitoWeb": "string",
  "entePropietario": "string",
  "invioEdizioni": true,
  "comune": "string",
  "indirizzo": "string",
  "latitudine": 0.0,
  "longitudine": 0.0,
  "provincia": "string"
}
```

---

#### `PUT /bo/testate/{idTestata}`
**Description:** Update testata anagrafica data (description, state, address, etc.)  
**Backend:** Direct DB write via Panache  
**Returns:** 200 with updated testata

---

#### `GET /bo/testate/{idTestata}/periodicita`
**Description:** Read testata periodicity configuration  
**Backend:** Direct DB read from `GDP_PERIODICITA`  
**Returns (200):**
```json
{
  "idPeriodicita": 1,
  "idTestata": 1,
  "mensilita": 0.5,
  "ggPeriodicita": "G01;G15",
  "inizioSospensione": "2026-01-01",
  "fineSospensione": "2026-01-31"
}
```

---

#### `PUT /bo/testate/{idTestata}/periodicita`
**Description:** Update periodicity — triggers async date recalculation  
**Backend:** 
1. Update `GDP_PERIODICITA` in DB
2. Invoke `F01 — FTPregolare.configDTEdizioneAttesa` on gdporch **asynchronously**  
**Returns:** `202 Accepted` with `jobId` for polling

**Response (202):**
```json
{
  "jobId": "uuid-or-id",
  "message": "Periodicity updated. Date recalculation started.",
  "status": "PROCESSING"
}
```

---

#### `GET /bo/testate/{idTestata}/date-attese`
**Description:** List expected dates for a period  
**Query params:** `dataInizio` (yyyy-mm-dd, required), `dataFine` (yyyy-mm-dd, required)  
**Backend:** Calls `F18 — FTPregolare.verifDateAttese` on gdporch  
**Implementation detail (v2 SFU-01):** Response from F18 now includes `cartellaTestata` field — must be forwarded in DTO

**Response (200):**
```json
{
  "testate": [
    {
      "idTestata": 1,
      "cartellaTestata": "sentinella",
      "dateAttese": [
        { "data": "2026-03-01", "sospesa": false },
        { "data": "2026-03-15", "sospesa": true }
      ],
      "nroEdizioniAttese": 2
    }
  ]
}
```

---

#### `POST /bo/testate/{idTestata}/date-attese`
**Description:** Generate expected dates for a period  
**Body:** `{ "dataInizio": "yyyy-mm-dd", "dataFine": "yyyy-mm-dd" }`  
**Backend:** Calls `F01 — configDTEdizioneAttesa` on gdporch **asynchronously**  
**UI flow (UC-02):** User selects testata from dropdown, defines period, presses "Genera". Result verified afterwards by pressing "Cerca" (triggers F18).  
**Returns:** `202 Accepted` with `jobId`

---

#### `GET /bo/testate/{idTestata}/sospensioni`
**Description:** Expected dates with suspension status for a date range  
**Query params:** `dataInizio`, `dataFine`  
**Backend:** Calls `F18 — verifDateAttese` on gdporch  
**UI flow (UC-02):** The Back Office sospensione screen shows: date range inputs, a table with `{idTestata, nomeTestata, dataEdizioneAttesa, sospesa}`, a "Numero edizioni sospese" counter, CERCA + SOSPENDI buttons.

---

#### `POST /bo/testate/{idTestata}/sospensioni`
**Description:** Suspend editions for a period  
**Body:** `{ "dataInizio": "yyyy-mm-dd", "dataFine": "yyyy-mm-dd" }`  
**Backend:** Calls `F05 — sospensioneEdizioneAttesa` on gdporch **synchronously**  
**Implementation detail:** F05 returns `giorniEdizione` (number of suspended days) — must be included in response DTO  
**Returns:**
```json
{
  "giorniSospesi": 5,
  "message": "Editions suspended successfully"
}
```

---

### 4.2 — SFTP User Management (UC-01)

#### `GET /bo/utenti-sftp`
**Description:** List SFTP users with role and status  
**Query params:** `stato` (string, optional)  
**Backend:** Direct DB read from `GDP_UTENTSFTP`

---

#### `POST /bo/utenti-sftp`
**Description:** Register new SFTP user (`utenteFTP` or `utenteWEB`)  
**Backend:** Write to `GDP_UTENTSFTP`  
**Returns:** `201 Created`

**Request body:**
```json
{
  "idTestata": 1,
  "username": "string",
  "password": "string",
  "homeFtp": "string",
  "referenteFtp": "string",
  "email": "string",
  "ruolo": "utenteFTP | utenteWEB",
  "stato": "attivo | sospeso"
}
```

---

#### `GET /bo/utenti-sftp/{id}`
**Description:** SFTP user detail  
**Backend:** Direct DB read from `GDP_UTENTSFTP`

---

#### `PUT /bo/utenti-sftp/{id}`
**Description:** Update user (role, status, SFTP password)  
**Backend:** Direct DB write to `GDP_UTENTSFTP`

---

### 4.3 — Acquisition Monitoring Dashboard (UC-05 — Scenario 5)

> **UC-05 reference:** This section maps directly to UC-05 (Monitoraggio cruscotto, V01 27/03/2026). The Back Office SPA `gdpbospa` uses these endpoints to drive the monitoring dashboard. The UI rendering rules (colors, button states, tipoMail values) are documented here because they tightly constrain the response contract that gdpbff must expose.

#### Back Office Menu Entry Points (from UC-05 Schema 1)

The BO main menu exposes three monitoring access points:
1. **"Accedi per verificare i caricamenti GIORNALIERI"** → calls `GET /bo/acquisizioni?tipo=G&data=<today>`
2. **"Accedi per verificare le acquisizioni STORICHE"** → calls `GET /bo/acquisizioni?tipo=S&data=<today>`
3. **"Ricerca i caricamenti pregressi per testata e/o edizione"** → calls `GET /bo/acquisizioni/ricerca`

---

#### `GET /bo/acquisizioni`
**Description:** Acquisition list for daily or historical monitoring (Algoritmo 1 — UC-05)  
**Query params:** `data` (yyyy-mm-dd, required), `tipo` ('G'=daily, 'S'=historical, required)  
**Backend:** Calls `F12 — MONITOR.elencoAcquisizioni` on gdporch

**UI rendering rules for the `esito` field (Algoritmo 1 — UC-05):**

The `esito` string from gdporch contains a tag in format `<xyz>text</xyz>`. The BFF must include the raw `esito` string in the response so that `gdpbospa` can apply these rendering rules:

| Tag in `esito` | Text color | Cell background | Meaning |
|----------------|-----------|-----------------|---------|
| `<MSG>` | white `#ffffff` | green `#408040` | OK |
| `<WRN>` | blue `#0000ff` | yellow `#ffff00` | Warning (non-blocking anomalies) |
| `<Ennn>` | yellow `#ffff00` | red `#ff0000` | Error (blocking) |

> The BFF should expose `esito` as-is (raw string with tags). The Angular SPA extracts the tag and applies styling client-side.

**Response (200):**
```json
{
  "acquisizioni": [
    {
      "idLog": 1,
      "idUtenteSFTP": 2,
      "idTestata": 3,
      "nomeTestata": "La Sentinella del Canavese",
      "nroEdizioni": 1,
      "dataEdizione": "2026-03-01",
      "dataAcquisizione": "2026-03-01T08:15:00",
      "nroTotFile": 24,
      "esito": "<MSG>Elaborazione OK</MSG>"
    }
  ]
}
```

> **Note:** `idLog` and `idTestata` are returned in the response but the SPA stores them as session variables — they are NOT displayed in the table. They are needed for subsequent detail/retry/mail calls.

---

#### `GET /bo/acquisizioni/{idLog}`
**Description:** Full acquisition detail — daily or historical (Algoritmo 2 and 6 — UC-05)  
**Backend:** Calls `F13 — MONITOR.dettaglioAcquisizione` on gdporch

**UI rendering rules for the detail page (Algoritmo 2 for daily, Algoritmo 6 for historical):**

Based on the `esito` tag, the SPA applies these behaviors:

| Tag in `esito` | Color | "in CODA" button | "MAIL" button | `tipoMail` set to |
|----------------|-------|-----------------|---------------|-------------------|
| `<MSG>` (daily) | green | **disabled** | **disabled** | — |
| `<WRN>` (daily) | yellow | active | active | `"GW"` |
| `<Ennn>` (daily) | red | active | active | `"GE"` |
| `<MSG>` (storico) | green | **disabled** | **disabled** | — |
| `<WRN>` (storico) | yellow | active | active | `"SW"` |
| `<Ennn>` (storico) | red | active | active | `"SE"` |

> **`tipoMail` values** — this string is passed as parameter to `F14 — preparaMAIL` to select the correct email template from `GDP_MAIL`. The BFF must derive `tipoMail` from the `tipoAcquisizione` and `esito` tag, and include it in the response or compute it client-side. Recommended approach: expose `tipoAcquisizione` and raw `esito` tag, let gdpbospa compute `tipoMail`.

**Field display mapping (both daily and historical detail, Schema 3 — UC-05):**

| UI Label | API Field | Format |
|----------|-----------|--------|
| Identificativo | `idTestata` | As-is (stored but displayed) |
| Nome Testata | `nomeTestata` | As-is |
| Data Edizione | `dataEdizione` | `gg/mm/aaaa` |
| Tipo Edizione | `tipoEdizione` | Decoded via `GDP_TIPO_EDIZIONE.DESCRIZIONE` |
| Tipo Acquisizione | `tipoAcquisizione` | Giornaliera / Storica |
| Data Acquisizione | `dataAcquisizione` | `yyyy-mm-dd HH:MM:SS` |
| Totale file Acquisiti | `nroTotFile` | As-is |
| Esito | `esito` | Tag-based coloring (see above) |
| id edizione | `idEdizione` | As-is |
| Prima pagina | `primaPagina` | As-is |
| File XML | `fileXML` | As-is |
| File ZIP | `fileZIP` | As-is |
| Pagine trasmesse | `nroPagOK` | As-is |
| Pagine scartate | `nroPagErrate` | As-is |
| job ID DAM | `jobId` | As-is (used for stato-dam call) |

**Response (200):**
```json
{
  "idLog": 1,
  "idTestata": 3,
  "nomeTestata": "La Sentinella del Canavese",
  "dataEdizione": "2026-03-01",
  "tipoEdizione": "Regolare",
  "tipoAcquisizione": "G",
  "dataAcquisizione": "2026-03-01T08:15:00",
  "nroTotFile": 24,
  "esito": "<WRN>Avviso: NL – page003.pdf</WRN>",
  "idEdizione": 100,
  "primaPagina": "SI",
  "fileXML": "true",
  "fileZIP": "true",
  "nroPagAcq": 24,
  "nroPagOK": 22,
  "nroPagErrate": 2,
  "jobId": "libra-job-id-xyz",
  "descrizione": "NL – page003.pdf"
}
```

---

#### `GET /bo/acquisizioni/{idLog}/edizioni-storiche`
**Description:** Edition list for a historical delivery (Algoritmo 5 — UC-05)  
**Use case:** In the historical flow, the user first sees a list of testate in a delivery (from F12). On selecting a testata row, the SPA navigates to an edition list for that testata (Schema 5 — UC-05). This is served by calling F13 with the selected `idLog` and mapping the results to a table.  
**Backend:** Calls `F13 — MONITOR.dettaglioAcquisizione` on gdporch with `idLog`  
**Note:** The SPA **keeps all F13 response data in memory** — when the user clicks on a specific edition row, the SPA uses cached data (Algoritmo 6) rather than making a second API call.

**UI table columns for historical edition list (Schema 5 — UC-05):**

| UI Label | API Field | Format |
|----------|-----------|--------|
| Nome Testata | `nomeTestata` | As-is |
| Data Edizione | `dataEdizione` | `gg/mm/aaaa` |
| Tipo Edizione | `tipoEdizione` | As-is |
| Totale pag Acquisite | `nroPagAcq` | As-is |
| Totale pag Valide | `nroPagOK` | As-is |
| Totale pag Errate | `nroPagErrate` | As-is |
| Esito | `esito` | Tag-based coloring; `<Ennn>` sets `tipoMail="ST"` |

**Additional `tipoMail` rule for historical editions (Algoritmo 5):**

| Tag | `tipoMail` |
|-----|-----------|
| `<MSG>` | — (no mail) |
| `<WRN>` | — (no override from list; set at detail level) |
| `<Ennn>` | `"ST"` |

---

#### `GET /bo/acquisizioni/ricerca`
**Description:** Historical search of acquisitions — multi-criteria filtering (UC-06, Scenario 5 — Monitoraggio edizioni)  
**Backend:** Calls `F15 — MONITOR.ricercaAcquisizioni` on gdporch  
**Priority:** SHOULD *(fully specified in SFU-01 V03 and UC-06 V01 27/03/2026)*

**Filter rules (Algoritmo 1 — UC-06):**

At least one of the three main filters (`tipoAcquisizione`, `testata`, `tipoEdizione`) must be set.

| Filter | UI Type | Rules | Maps to |
|--------|---------|-------|---------|
| `tipoAcquisizione` | Dropdown | **Required**. Values: `"G"` (giornaliera), `"S"` (storica) | F15 `tipoAcquisizione` |
| `idTestata` | Dropdown | Optional. Populated from DB testata list (name shown, ID hidden) | F15 `testata` |
| `dataDA` | Date input | Optional. Format `dd-mm-yyyy` | F15 `dataDA` |
| `dataA` | Date input | **Required** (pre-filled with today's date). Format `dd-mm-yyyy` | F15 `dataA` |
| `tipoEdizione` | Dropdown | **Required**. See values below | F15 `tipoEdizione` |

**`tipoEdizione` dropdown values (Algoritmo 1 — UC-06):**

| Display label | Code sent to F15 |
|---------------|-----------------|
| corrispondente | `OK` |
| sospesa | `SO` |
| anticipataria | `AN` |
| posticipataria | `PO` |
| anomalia edizione attesa | `AA` |
| edizione storica | `ST` |
| anomalia edizione storica | `AS` |

**Query params:**
| Param | Values | Required |
|-------|--------|----------|
| `tipoAcquisizione` | `G` or `S` | Yes |
| `idTestata` | Integer | Optional |
| `dataDA` | yyyy-mm-dd | Optional |
| `dataA` | yyyy-mm-dd | Yes (default today) |
| `tipoEdizione` | `OK`, `SO`, `AN`, `PO`, `AA`, `ST`, `AS` | Yes |

**Response (200):**
```json
{
  "acquisizioni": [
    {
      "idLog": 1,
      "idTestata": 3,
      "nomeTestata": "La Sentinella del Canavese",
      "tipoEdizione": "OK",
      "dataEdizione": "2026-03-01",
      "dataAcquisizione": "2026-03-01T08:15:00",
      "nroTotFileAcq": 24,
      "nroTotFileVal": 22
    }
  ]
}
```

**UI rendering rules for `tipoEdizione` in result table (Algoritmo 2 — UC-06):**

| `tipoEdizione` value | Text color | Cell background |
|----------------------|-----------|-----------------|
| `OK` (corrispondente), `AN` (anticipataria), `PO` (posticipataria), `ST` (storica) | white `#ffffff` | green `#408040` |
| `SO` (sospesa) | blue `#0000ff` | yellow `#ffff00` |
| `AA` (anomalia edizione attesa), `AS` (anomalia edizione storica) | yellow `#ffff00` | red `#ff0000` |

**UI field mapping for result table (Algoritmo 2 — UC-06):**

| UI Column | API Field | Format |
|-----------|-----------|--------|
| Nome Testata | `nomeTestata` | As-is |
| Data Edizione | `dataEdizione` | `gg/mm/aaaa` |
| Data Acquisizione | `dataAcquisizione` | `yyyy-mm-dd HH:MM:SS` |
| Totale file acquisiti | `nroTotFileAcq` | As-is |
| Totale file trasmessi | `nroTotFileVal` | As-is |
| Tipo Edizione | `tipoEdizione` | Color-coded cell (see above) |

**Detail navigation (UC-06 Scenario — Dettaglio acquisizione):**
When the user selects a row, the SPA navigates to the detail view following the same flow as UC-05:
- If `tipoAcquisizione = 'G'` → UC-05 "Edizioni giornaliere — Passo 4" (calls `GET /bo/acquisizioni/{idLog}`)
- If `tipoAcquisizione = 'S'` → UC-05 "Edizioni storiche — Passo 5" (calls `GET /bo/acquisizioni/{idLog}/edizioni-storiche`)

---

#### `GET /bo/acquisizioni/{idLog}/stato-dam`
**Description:** Check real-time DAM processing status of an acquisition's import job (Algoritmo 3 — UC-05)  
**Trigger (UC-05):** Operator clicks "stato DAM" button on the detail page (Schema 3.A or 3.B). Button is visible when `esito` tag is `<MSG>` or `<WRN>`.  
**Backend:** Calls `F20 — MONITOR.statoDAM` on gdporch, passing `jobId` from the detail response  
**UI behavior:** Result displayed inline on current page — no page navigation (UC-05 notes: "non ha senso caricare una nuova pagina")

**Returns (200):**
```json
{
  "jobId": "libra-job-id",
  "status": "SUBMITTED | PROCESSING | COMPLETED | FAILED"
}
```

---

#### `POST /bo/acquisizioni/{idLog}/retry`
**Description:** Re-enqueue edition for DAM retransmission (Algoritmo 4 — UC-05 "Riponi in CODA")  
**Trigger (UC-05):** Operator clicks "in CODA" button on the detail page (Schema 3.B or 3.C). Button is active when `esito` tag is `<WRN>` or `<Ennn>`. Button is **disabled** when tag is `<MSG>`.  
**Prerequisite (UC-05):** Acquisition esito is "Elaborazione OK" or "Errore xxx". Operator has performed any necessary extra-procedure actions.  
**Backend:** Calls `F21 — MONITOR.attivaCODA` on gdporch  
**Implementation detail:** F21 sets `GDP_CODA_CARICAMENTO.STATO = 'READY'`, increments `NRO_TENTATIVO`, checks `NRO_MAX_TENTATIVI`. Max retries exceeded → MSG00001.  
**UI behavior:** Result displayed inline on current page — no page navigation  
**Returns:** `202 Accepted` on success, `409 Conflict` if max retries exceeded

---

#### `POST /bo/acquisizioni/{idLog}/mail/prepara`
**Description:** Prepare mail notification for an anomalous acquisition (Algoritmo 7 — UC-05)  
**Trigger (UC-05):** Operator clicks "MAIL" button on detail page (Schema 3.B or 3.C). Button is active when `esito` tag is `<WRN>` or `<Ennn>`. Button is **disabled** when tag is `<MSG>`.  
**Prerequisite (UC-05):** Acquisition esito is "Elaborazione OK" or "Errore xxx".  
**Backend:** Calls `F14 — MONITOR.preparaMAIL` on gdporch with `idLog` and `tipoMail`

**Request body:**
```json
{
  "tipoMail": "GW | GE | SW | SE | ST"
}
```

**`tipoMail` values by context:**
| Scenario | Esito tag | `tipoMail` |
|----------|-----------|-----------|
| Daily (Giornaliero) | `<WRN>` | `"GW"` — "edizione da integrare" |
| Daily (Giornaliero) | `<Ennn>` | `"GE"` — "edizione errata" |
| Historical (Storico) detail | `<WRN>` | `"SW"` — "edizione storica da integrare" |
| Historical (Storico) detail | `<Ennn>` | `"SE"` — "edizione storica errata" |
| Historical edition list row | `<Ennn>` | `"ST"` — all editions in delivery |

**UC-05 mail flow:**
1. BFF calls F14 → receives `{from, to, host, porta, oggetto, testo}`
2. BFF returns all fields to `gdpbospa`
3. SPA displays Schema 6 (mail preview page) with pre-filled subject and body
4. **Operator can edit `oggetto` and `testo` before sending**
5. BFF stores `{from, to, host, porta}` in session (not editable by operator)
6. Operator presses send button → calls `POST /bo/acquisizioni/{idLog}/mail/invia`

**Response (200):**
```json
{
  "from": "gdp@csipiemonte.it",
  "to": "editore@testata.it",
  "host": "mail.csipiemonte.it",
  "porta": 587,
  "oggetto": "Edizione del 01/03/2026 - La Sentinella del Canavese",
  "testo": "Gentile editore, si segnala un'anomalia nell'edizione del..."
}
```

---

#### `POST /bo/acquisizioni/{idLog}/mail/invia`
**Description:** Actually send the prepared mail (Algoritmo 8 — UC-05)  
**Trigger (UC-05):** Operator presses send button on Schema 6 mail preview page, after optionally editing subject and body.  
**Backend:** Calls `F22 — MONITOR.invioMAIL` on gdporch  
**UI behavior:** After send, the SPA reloads the calling page and **disables the "MAIL" button** for that edition.

**Request body** (operator may have modified `oggetto` and `testo`):
```json
{
  "from": "gdp@csipiemonte.it",
  "to": "editore@testata.it",
  "host": "mail.csipiemonte.it",
  "porta": 587,
  "oggetto": "Edizione del 01/03/2026 - [edited by operator]",
  "testo": "Testo della mail [edited by operator]"
}
```

**Returns:** `200 OK` on success, `500` on send failure

---

#### `GET /bo/acquisizioni/{idLog}/file`
**Description:** Download or re-upload acquisition files for operator  
**Backend:** Access SFTP directly via gdporch  
**Priority:** SHOULD

---

### 4.4 — GDPR Right to Oblio (UC — Scenario 8)

#### `POST /bo/pagine/{idPagina}/oblio`
**Description:** Initiate obscuring process on LIBRA and update DB flag  
**Access:** `operatore` role only (403 for `utentePrivilegiato` or `utenteWeb`)  
**Backend:**
1. Call DAM LIBRA API to obscure page content
2. Update `GDP_PAGINA`: set `OBLIO = true`, `DATA_OBLIO = NOW()`, `NOTA_OBLIO = <details>`  
**Corresponds to:** FunE01  
**Returns:** `202 Accepted`

---

#### `GET /bo/pagine/{idPagina}/oblio`
**Description:** Check oblio status of a page  
**Access:** `operatore` role only  
**Returns (200):**
```json
{
  "idPagina": 1,
  "oblio": true,
  "dataOblio": "2026-03-01T10:00:00",
  "notaOblio": "Requested by CRP on 2026-02-15"
}
```

---

## API Contract — Front Office (/fo/**)

Front Office APIs have variable visibility rules based on the user's authenticated role.

### 5.1 — Archive Search (UC — Scenario 6)

#### `GET /fo/ricerca`
**Description:** Full-text search in the archive  
**Backend:** Delegates to DAM LIBRA search API  
**Visibility rule applied before response:** Filter out editions with `DATA_PUBBLICAZIONE > CURRENT_DATE` for `utenteWeb`

**Query params:**
| Param | Description | Required |
|-------|-------------|----------|
| `q` | Search text | Yes |
| `dataInizio` | Date filter start (yyyy-mm-dd) | No |
| `dataFine` | Date filter end (yyyy-mm-dd) | No |
| `provincia` | Province filter | No |
| `idTestata` | Testata filter | No |
| `areaTematica` | Thematic area filter | No |
| `page` | Page number (default 0) | No |
| `size` | Page size (default 20) | No |

**Performance requirement:** 95th percentile latency < 2 seconds at nominal load (30 concurrent users)

**Response (200):**
```json
{
  "risultati": [
    {
      "idPagina": 1,
      "idEdizione": 10,
      "idTestata": 3,
      "nomeTestata": "string",
      "dataEdizione": "2026-03-01",
      "numPagina": 5,
      "excerpt": "...matched text snippet...",
      "thumbnail": "url-to-thumbnail"
    }
  ],
  "totale": 150,
  "pagina": 0,
  "dimensione": 20
}
```

> **Open Issue:** LIBRA full-text search API specification (endpoint, parameters, response format, pagination) is not yet available. This endpoint CANNOT be implemented until LIBRA API documentation is obtained.

---

#### `GET /fo/ricerca/avanzata`
**Description:** Advanced search with AND/OR/EXACT operators  
**Backend:** Delegates to DAM LIBRA with additional operator parameter  
**Visibility rule:** Same as `/fo/ricerca`

**Additional query params:**
| Param | Values | Description |
|-------|--------|-------------|
| `modalita` | `AND \| OR \| EXACT` | Search mode |

All other params same as `/fo/ricerca`.

---

### 5.2 — Testata Catalogue and Editions (UC — Scenario 6)

#### `GET /fo/testate`
**Description:** List consultable testate  
**Backend:** Direct DB read from `GDP_TESTATA`  

**Query params:**
| Param | Description |
|-------|-------------|
| `provincia` | Filter by province |
| `attive` | Filter active/historical (`true/false`) |
| `areaTematica` | Filter by thematic area |

---

#### `GET /fo/testate/{idTestata}`
**Description:** Public data of a testata (name, description, province, periodicity)  
**Backend:** Direct DB read  
**Note:** Returns only public fields — NOT SFTP or internal configuration data

---

#### `GET /fo/testate/{idTestata}/edizioni`
**Description:** List editions of a testata  
**Backend:** Direct DB read from `GDP_EDIZIONE`  
**Visibility rule applied:** Filter `DATA_PUBBLICAZIONE > CURRENT_DATE` for `utenteWeb`

**Query params:** `dataInizio`, `dataFine`, `page`, `size`

**Response (200):**
```json
{
  "edizioni": [
    {
      "idEdizione": 10,
      "dataEdizione": "2026-03-01",
      "dataPubblicazione": "2026-03-08",
      "numeroPagine": 24,
      "stato": 0
    }
  ],
  "totale": 52,
  "pagina": 0,
  "dimensione": 20
}
```

---

#### `GET /fo/edizioni/{idEdizione}`
**Description:** Edition detail with page list and metadata  
**Backend:** Direct DB read from `GDP_EDIZIONE` + `GDP_PAGINA`  
**Visibility rule:** If `utenteWeb` and edition is within last 2 → return 403

---

#### `GET /fo/edizioni/{idEdizione}/pagine/{numPagina}`
**Description:** PDF stream of a specific page  
**Backend:** Retrieve PDF from DAM LIBRA and stream to client  
**Implementation:** Use `transferTo()` — NO full in-memory buffering  
**Performance requirement:** Must use chunked transfer encoding; must not load entire file into heap

---

### 5.3 — Fascicoli (UC — Scenario 7)

A **fascicolo** is a user-selected set of pages assembled into a single downloadable PDF.

**Fascicolo behavior by role:**
- `utenteWeb`: temporary — NOT persisted to DB. PDF generated on-the-fly, deleted after download or session end. No record in `GDP_FASCICOLO`.
- `utentePrivilegiato`: permanent — saved to `GDP_FASCICOLO` and `GDP_FASCICOLO_PAG`. Retrievable in future sessions until explicitly deleted.

> **Open Issue:** Cleanup strategy for temporary `utenteWeb` fascicoli depends on Angular session management. Must be clarified with frontend team before implementing RF-22.

#### `POST /fo/fascicoli`
**Description:** Create a new fascicolo with a list of page IDs  
**Backend:** Assemble PDF from individual LIBRA pages using PDFBox; persist to DB for `utentePrivilegiato`  
**Implementation:** Retrieve each page PDF from LIBRA, merge using Apache PDFBox 3.x

**Request body:**
```json
{
  "pagine": [1, 2, 3, 15, 16]
}
```

**Response for `utenteWeb` (201):**
```json
{
  "pdf": "<base64-encoded-pdf-or-direct-stream>",
  "temporaneo": true
}
```

**Response for `utentePrivilegiato` (201):**
```json
{
  "idFascicolo": 42,
  "temporaneo": false,
  "creazione": "2026-03-01T10:00:00"
}
```

**Performance:** Fascicolo with up to 50 pages must complete within 10 seconds. For larger fascicoli: return `202 Accepted` with `jobId` for status polling.

---

#### `GET /fo/fascicoli/{idFascicolo}`
**Description:** Retrieve an existing fascicolo  
**Access:** `utentePrivilegiato` only — return 404 for `utenteWeb`  
**Backend:** Read from `GDP_FASCICOLO` + `GDP_FASCICOLO_PAG`

---

#### `GET /fo/fascicoli/{idFascicolo}/pdf`
**Description:** Download fascicolo PDF  
- For `utentePrivilegiato`: serve saved PDF
- For `utenteWeb`: generate PDF on-the-fly (page assembly)

---

#### `PUT /fo/fascicoli/{idFascicolo}`
**Description:** Add/remove pages from existing fascicolo  
**Access:** `utentePrivilegiato` only  
**Note:** PDF will be regenerated at next download  
**Priority:** SHOULD

**Request body:**
```json
{
  "aggiungi": [17, 18],
  "rimuovi": [3]
}
```

---

#### `DELETE /fo/fascicoli/{idFascicolo}`
**Description:** Delete a fascicolo  
**Access:** `utentePrivilegiato` only  
**Returns:** `204 No Content`

---

#### `GET /fo/fascicoli`
**Description:** List authenticated user's fascicoli  
**Access:** `utentePrivilegiato` only  
**Backend:** Read from `GDP_FASCICOLO` filtered by user identity

---

### 5.4 — Preferiti / Favourites (UC — Scenario 7)

Accessible only to `utentePrivilegiato`. Allows saving favourite testate or editions.

#### `GET /fo/preferiti`
**Description:** List user's favourite testate and editions

---

#### `POST /fo/preferiti`
**Description:** Add a testata or edition to favourites  
**Backend:** Write to dedicated favourites table (`GDP_PREF_TESTATA`)  
**Priority:** SHOULD

**Request body:**
```json
{
  "tipo": "testata | edizione",
  "idRiferimento": 42
}
```

---

#### `DELETE /fo/preferiti/{id}`
**Description:** Remove item from favourites  
**Priority:** SHOULD

---

## Database — Tables Used by gdpbff

gdpbff accesses PostgreSQL directly for features that do not route through gdporch.

| Table | Access | Usage |
|-------|--------|-------|
| `GDP_TESTATA` | READ / WRITE | FO catalogue, BO management. Write for testata registration/update (if not delegated to gdporch). |
| `GDP_PERIODICITA` | READ / WRITE | Read and update testata periodicity. Write triggers async F01 call to gdporch. |
| `GDP_DATA_USCITA` | READ | Read expected dates for last-2-editions filter calculation and BO display (UC-02). |
| `GDP_UTENTEWEB` | READ / WRITE | Manage privileged users and their roles for FO access. |
| `GDP_UTENTSFTP` | READ / WRITE | SFTP user management from Back Office (UC-01). |
| `GDP_EDIZIONE` | READ | FO: edition listing per testata, detail, last-2 calculation from `DATA_PUBBLICAZIONE`. |
| `GDP_PAGINA` | READ / WRITE | Read: page listing per edition. Write: update `OBLIO` flag after GDPR request. |
| `GDP_FASCICOLO` | READ / WRITE | Create, read, update, delete fascicoli for `utentePrivilegiato`. |
| `GDP_FASCICOLO_PAG` | READ / WRITE | Manage pages associated with each fascicolo. |
| `GDP_IMPORT_TASK` | WRITE | Update `STATO='PRO'` for retry of a failed task from Back Office. |
| `GDP_PREF_TESTATA` | READ / WRITE | Favourites management for `utentePrivilegiato`. |
| `GDP_MAIL` | READ | Email templates for `F14 — preparaMAIL`. Keyed by `COD_MAIL` (GW, GE, SW, SE, ST). Contains `MITTENTE`, `OGGETTO`, `TESTO` with placeholders. |
| `GDP_TIPO_EDIZIONE` | READ | Lookup table for decoding edition type codes (OK, AT, PT, SO, AA, ST, AS) into human-readable descriptions. Used in F13/F15 responses. |

---

## External Integrations

### gdporch — Monitoring and Configuration APIs

| Parameter | Value |
|-----------|-------|
| Integration type | `@RegisterRestClient` (MicroProfile REST Client) |
| URL | From env var `GDPORCH_URL` |
| Fault tolerance | `@Retry(maxRetries=2)` + `@Timeout(5s)` + `@CircuitBreaker` |
| Authentication | Internal service token (header `X-Service-Token`) |
| APIs consumed | F12 `elencoAcquisizioni`, F13 `dettaglioAcquisizione`, F14 `preparaMAIL`, F15 `ricercaAcquisizioni`, F20 `statoDAM`, F21 `attivaCODA`, F22 `invioMAIL`, F01 `configDTEdizioneAttesa`, F05 `sospensioneEdizioneAttesa`, F18 `verifDateAttese` |

**Circuit breaker behavior:** If gdporch unavailable, monitoring endpoints return 503. BO testata/user management endpoints that read directly from DB continue to work.

---

### DAM LIBRA — Search and Asset Management

| Parameter | Value |
|-----------|-------|
| Integration type | `@RegisterRestClient` (MicroProfile REST Client) |
| URL | From env var `LIBRA_URL` |
| Fault tolerance | `@CircuitBreaker` + `@Timeout(10s)` + `@Fallback` |
| Authentication | API Key via `ClientRequestFilter` — from env var `LIBRA_API_KEY` |
| APIs consumed | Full-text search, asset listing per edition, PDF asset download (streaming), page obscuring (oblio) |
| Streaming | PDF download uses `transferTo()` — no complete in-memory buffering |

**Circuit breaker behavior:** If LIBRA unavailable, search endpoints return 503 instead of making users wait. `/bo/testate` continues to work (reads from DB).

> **Open Issue:** LIBRA search API specifications (endpoint, parameters, response format, pagination) are not documented in available materials. Must be obtained before implementing RF-16, RF-17.

---

## Package Structure

```
com.csi.gdpbff/
├── api/
│   ├── bo/                     # Back Office endpoint implementations
│   │   ├── TestataBOResource.java
│   │   ├── AcquisizioneBOResource.java
│   │   ├── UtenteSftpBOResource.java
│   │   └── PaginaBOResource.java
│   └── fo/                     # Front Office endpoint implementations
│       ├── RicercaFOResource.java
│       ├── TestateFOResource.java
│       ├── EdizioniFOResource.java
│       ├── FascicoliFOResource.java
│       └── PreferitivFOResource.java
├── service/
│   ├── bo/
│   │   ├── TestataService.java
│   │   ├── AcquisizioneService.java
│   │   └── UtenteService.java
│   └── fo/
│       ├── RicercaService.java
│       ├── FascicoloService.java
│       └── CatalogoService.java
├── client/
│   ├── GdporchClient.java      # REST Client for gdporch APIs
│   └── LibraClient.java        # REST Client for DAM LIBRA
├── entity/
│   ├── Testata.java
│   ├── Edizione.java
│   ├── Pagina.java
│   ├── Fascicolo.java
│   └── Utente.java
├── repository/
│   └── (Panache repositories for DB queries)
├── mapper/
│   └── (domain ↔ generated DTOs conversion)
├── exception/
│   ├── ValidationExceptionMapper.java
│   ├── BackendUnavailableExceptionMapper.java
│   └── GlobalExceptionMapper.java
└── filter/
    ├── LibraAuthFilter.java        # ClientRequestFilter for LIBRA API Key
    └── CorrelationIdFilter.java    # Propagate correlation ID
```

---

## Functional Requirements

### Authentication and Authorization

| ID | Requirement | Description | Priority |
|----|-------------|-------------|----------|
| RF-01 | Shibboleth/IAM integration | BFF must integrate SSO via Shibboleth for CSI users (BO) and IAM RUPAR for privileged users. Identity propagated as JWT. **Must confirm JWT format (claims, issuer, expiry) with CSI infrastructure team before implementation.** | MUST |
| RF-02 | Open Front Office access | FO must be accessible without authentication for anonymous users, with reduced visibility (no last 2 editions per active testata) | MUST |
| RF-03 | Role-based control `@RolesAllowed` | Every endpoint must apply role check. `/bo/**` reserved for `operatore`. Some `/fo/**` reserved for `utentePrivilegiato`. | MUST |
| RF-04 | Last 2 editions filter | For `utenteWeb`: exclude from search and edition listing all pages/editions whose `DATA_PUBBLICAZIONE > CURRENT_DATE`. Calculated from `GDP_EDIZIONE.DATA_PUBBLICAZIONE`. No restriction for `utentePrivilegiato` and `operatore`. | MUST |
| RF-05 | Identity propagation to backends | Calls to gdporch and LIBRA must propagate user identity or a configurable service token for traceability | SHOULD |

### Back Office — Testata and Periodicity Management

| ID | Requirement | Description | Priority |
|----|-------------|-------------|----------|
| RF-06 | Testata list with filters | Expose testata list with filters for status (all/senders only/by province) delegating to `F16-DB.getElencoTestate`. Response optimized for BO selection dropdown. | MUST |
| RF-07 | Periodicity update | On periodicity change, invoke F01 on gdporch asynchronously for date regeneration. Return 202 Accepted + jobId. | MUST |
| RF-08 | Expected date generation | Expose POST endpoint to start expected date generation for a period, delegating to F01 (async). Verification via GET. | MUST |
| RF-09 | Edition suspension | Expose endpoint to suspend testata editions for a period, delegating to F05 on gdporch (synchronous). Return `giorniEdizione` (number of days suspended) in response DTO. | MUST |
| RF-10 | Expected date verification | Expose GET endpoint to consult expected dates with suspension status, delegating to F18 on gdporch. Include `cartellaTestata` field in response (added in SFU-01 v2). | MUST |

### Back Office — Acquisition Monitoring

| ID | Requirement | Description | Priority |
|----|-------------|-------------|----------|
| RF-11 | Daily acquisition list | Expose list of day's acquisitions by type (G/S), aggregating F12 data from gdporch with testata info from `GDP_TESTATA`. | MUST |
| RF-12 | Acquisition detail | Expose single acquisition detail with: pages OK/KO, decoded edition type (OK/AT/PT/SO/AA/ST/AS), anomalies, XML/ZIP flags. Delegate to F13. | MUST |
| RF-13 | Acquisition retry | Expose `POST /retry` that resets `GDP_IMPORT_TASK.STATO='PRO'` for a failed task, allowing gdporch to retry DAM send. | MUST |
| RF-14 | Download/upload files for operator | Allow operator to download and re-upload files from an anomalous acquisition, interacting with SFTP via gdporch. | SHOULD |
| RF-15 | Historical acquisition search | Expose acquisition search endpoint by testata and/or date range. Delegate to F15 on gdporch. | SHOULD |

### Front Office — Search and Consultation

| ID | Requirement | Description | Priority |
|----|-------------|-------------|----------|
| RF-16 | Full-text search | Expose full-text search endpoint delegating to LIBRA. Apply last-2-editions filter for `utenteWeb`. | MUST |
| RF-17 | Advanced search | Support advanced search with AND/OR/EXACT operators and filters for province, time period, specific testata, and thematic area. | MUST |
| RF-18 | Testata catalogue | Expose testata catalogue with filters by province, active/inactive, reading from `GDP_TESTATA`. | MUST |
| RF-19 | Edition listing per testata | Return editions for a testata by date range, applying role-based visibility filter. | MUST |
| RF-20 | PDF page stream | Expose endpoint for single PDF page download, retrieving from DAM LIBRA in streaming mode without full in-memory load. | MUST |

### Front Office — Fascicoli

| ID | Requirement | Description | Priority |
|----|-------------|-------------|----------|
| RF-21 | PDF fascicolo assembly | Assemble selected pages into single PDF, retrieving individual PDFs from LIBRA and merging with PDFBox or equivalent. | MUST |
| RF-22 | Temporary fascicolo | For `utenteWeb`: fascicolo NOT persisted to DB. PDF generated on-the-fly, deleted after download or session end. **Cleanup strategy must be agreed with frontend team.** | MUST |
| RF-23 | Permanent fascicolo | For `utentePrivilegiato`: fascicolo saved to `GDP_FASCICOLO` and `GDP_FASCICOLO_PAG`. Retrievable in future sessions until explicitly deleted. | MUST |
| RF-24 | Fascicolo modification | `utentePrivilegiato` must be able to add/remove pages from existing fascicolo. PDF regenerated at next download. | SHOULD |
| RF-25 | Favourites management | BFF must manage list of favourite testate/editions for `utentePrivilegiato`, reading and writing to dedicated table. | SHOULD |

### GDPR Right to Oblio

| ID | Requirement | Description | Priority |
|----|-------------|-------------|----------|
| RF-26 | Page obscuring on LIBRA | Invoke LIBRA API to obscure a specific page's content following an accepted oblio request from CRP. | MUST |
| RF-27 | Update oblio flag on DB | Simultaneously with LIBRA obscuring: update `GDP_PAGINA` setting `OBLIO=true`, `DATA_OBLIO=now()`, `NOTA_OBLIO` with details. | MUST |
| RF-28 | Operator-only access | Oblio endpoint accessible only with `operatore` role. `utentePrivilegiato` or `utenteWeb` must receive 403. | MUST |

---

## Non-Functional Requirements

### API Contract and Compatibility

| ID | Requirement | Description | Priority |
|----|-------------|-------------|----------|
| RNF-01 | Contract-first with openapi.yaml | API contract must be defined in `openapi.yaml` agreed with frontend team before implementation. DTOs auto-generated with `openapi-generator-maven-plugin`. **No handwritten DTOs.** | MUST |
| RNF-02 | Contract stability | Breaking API changes (field removal, type change) require versioning or explicit agreement with frontend teams. Adding optional fields is non-breaking. | MUST |
| RNF-03 | Swagger UI in dev | In dev environment, Swagger UI available at `/q/swagger-ui` for frontend team during integrated development. | MUST |

### Performance

| ID | Requirement | Description | Priority |
|----|-------------|-------------|----------|
| RNF-04 | Search latency | 95th percentile latency for `/fo/ricerca` must be < 2 seconds at nominal load (30 concurrent users) | MUST |
| RNF-05 | PDF streaming | Single PDF page download must use chunked transfer, no full heap buffering | MUST |
| RNF-06 | Fascicolo assembly | Assembly of up to 50 pages must complete within 10 seconds. Larger fascicoli: return 202 Accepted + jobId for polling. | SHOULD |
| RNF-07 | Parallel backend calls | When response requires data from multiple sources (e.g., acquisition list + testata data), BFF must execute calls in parallel using `Uni.combine()`, not sequentially. | MUST |

### Security

| ID | Requirement | Description | Priority |
|----|-------------|-------------|----------|
| RNF-08 | Input validation `@Valid` | All endpoints with request body must apply `@Valid` for Bean Validation. Errors must return 400 with structured `ErrorResponse`. | MUST |
| RNF-09 | CORS configured | BFF must have CORS configured to accept requests only from `gdpbospa` and `gdpfospa` origins. No wildcard (`*`) in production. | MUST |
| RNF-10 | No internal detail exposure | 500 Internal Server Errors must NOT expose stack traces or system details in JSON response. Only error code and generic message. | MUST |
| RNF-11 | Rate limiting | BFF should apply rate limiting on public FO endpoints to prevent abuse. Configurable via Quarkus. | SHOULD |

### Resilience

| ID | Requirement | Description | Priority |
|----|-------------|-------------|----------|
| RNF-12 | Fault tolerance toward backends | Calls to gdporch and LIBRA must be protected with `@Retry` and `@Timeout`. On timeout or repeated error: return 503 with clear message. | MUST |
| RNF-13 | Circuit breaker LIBRA | Client toward DAM LIBRA must have a circuit breaker. If LIBRA unreachable, search endpoints must return 503 instead of making users wait. | MUST |
| RNF-14 | Graceful BO degradation | If gdporch unavailable, monitoring endpoints return 503. BO testata and user management endpoints reading directly from DB must continue working. | SHOULD |

---

## Error Response Contract

All error responses must follow this structure (to be defined in `openapi.yaml`):

```json
{
  "codice": "BACKEND_UNAVAILABLE | VALIDATION_ERROR | NOT_FOUND | FORBIDDEN | INTERNAL_ERROR",
  "messaggio": "Human-readable error description",
  "dettagli": [
    {
      "campo": "fieldName",
      "errore": "Field-specific error description"
    }
  ],
  "timestamp": "2026-03-01T10:00:00Z"
}
```

HTTP status codes:
- `400 Bad Request` — validation errors (`@Valid` failures)
- `401 Unauthorized` — missing or invalid JWT
- `403 Forbidden` — authenticated but insufficient role
- `404 Not Found` — resource not found
- `503 Service Unavailable` — backend system unavailable (gdporch or LIBRA)
- `500 Internal Server Error` — unexpected errors (no stack trace in response)

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GDPORCH_URL` | Yes | gdporch base URL (e.g., `http://gdporch:8082`) |
| `LIBRA_URL` | Yes | DAM LIBRA base URL |
| `LIBRA_API_KEY` | Yes | API Key for LIBRA authentication |
| `DB_URL` | Yes | JDBC URL for PostgreSQL |
| `DB_USER` | Yes | Database username |
| `DB_PASSWORD` | Yes | Database password |
| `JWT_ISSUER` | Yes | Expected issuer for JWT validation (e.g., Shibboleth/IAM URL) |
| `JWT_PUBLIC_KEY_LOCATION` | Yes | URL or path of public certificate for JWT verification |
| `CORS_ORIGINS` | Yes | Allowed CORS origins (e.g., `https://gdp.cr.piemonte.it`) |
| `SERVICE_TOKEN` | No | Token for service-to-service calls to gdporch |

---

## Acceptance Criteria

### AC-1: Access control and roles
1. `GET /bo/acquisizioni` without token → 401 Unauthorized
2. `GET /bo/acquisizioni` with `utenteWeb` token → 403 Forbidden
3. `GET /bo/acquisizioni` with `operatore` token → 200 with data
4. `POST /bo/pagine/{id}/oblio` with `utentePrivilegiato` token → 403

### AC-2: Last 2 editions filter
5. Given a weekly testata with last edition `DATA_PUBBLICAZIONE = today+7`, `GET /fo/ricerca` with anonymous token must NOT include that edition in results
6. Same call with `utentePrivilegiato` token must include all editions without filter
7. Same call with `operatore` token must include all editions

### AC-3: Fascicoli
8. `POST /fo/fascicoli` with `utenteWeb` token → 201 with direct PDF response (not persisted to DB). `GDP_FASCICOLO` must have no new records.
9. Same call with `utentePrivilegiato` token → creates `GDP_FASCICOLO` record and returns 201 with `idFascicolo`
10. `GET /fo/fascicoli/{idFascicolo}` with `utenteWeb` token → 404

### AC-4: Backend resilience
11. With gdporch unreachable: `GET /bo/acquisizioni` → 503 with `ErrorResponse.codice='BACKEND_UNAVAILABLE'` within 5 seconds (configured timeout)
12. With LIBRA unreachable: `GET /fo/ricerca` → 503. `GET /bo/testate` → 200 (reads from DB, independent of LIBRA)

### AC-5: API contract
13. `openapi.yaml` agreed with frontend team compiles without errors with `openapi-generator-maven-plugin` version 7.4.x
14. Generated DTOs have `@NotNull` and `@Size` on mandatory fields defined in yaml
15. Request body with missing mandatory field → 400 with missing field detail in `ErrorResponse.dettagli`

---

## Open Issues

| ID | Description | Impact |
|----|-------------|--------|
| OPEN-1 | LIBRA API for full-text search: specifications (endpoint, parameters, response format, pagination) not available. **Cannot implement RF-16 and RF-17 until LIBRA API documentation is obtained.** | RF-16, RF-17 |
| OPEN-2 | Temporary fascicolo cleanup (Scenario 7): UC description says PDF "is deleted as soon as user performs other operations or ends navigation". Cleanup depends on Angular session management. **Must be clarified with frontend team before implementing RF-22.** | RF-22 |
| OPEN-3 | JWT format: exact format (claims, issuer, expiry) of tokens produced by Shibboleth/IAM must be confirmed with CSI infrastructure team before implementing RF-01. | RF-01 |
| OPEN-4 | DAM authentication: UC-01 (Autenticazione) describes token generation from public/private key pair. Must ensure UC-01 documentation is available before implementing F10 integration. | LIBRA calls |

---

## Out of Scope

| Functionality | Responsible Component |
|--------------|-----------------------|
| UI rendering and Angular components | gdpbospa and gdpfospa (frontend SPAs) |
| Shibboleth/RUPAR user provisioning | CSI infrastructure — BFF consumes tokens, does NOT issue them |
| Full-text indexing in DAM | LIBRA — BFF queries LIBRA, does not manage indexing |
| SFTP file acquisition and polling | gdporch — sole responsibility for acquisition flow |
| TIFF archiving of historical editions | Managed by gdporch + dedicated conservation storage |
| XML metadata generation for DAM | gdporch (F09) — BFF does not produce XML metadata |
| Kubernetes infrastructure management | CSI Platforms/Datacenter team |

---

## Technical Constraints

- Quarkus 3.x mandatory — CSI stack compatibility
- Use `jakarta.*` namespace only (NOT `javax.*`)
- Java 17 on Adoptium Temurin — no other versions supported
- PostgreSQL 15 — do not use features specific to later versions
- `openapi.yaml` MUST precede implementation — no coding without approved contract
- Code generated by `openapi-generator` must NOT be versioned. Only `openapi.yaml` in repository
- `target/generated-sources/` in `.gitignore`
- No credentials committed to git
- Angular 19.x for frontend SPAs (for context — out of BFF scope)
- fixed database schema

---

## System-Level User Stories (from VDI-01)

| ID | Name | Description |
|----|------|-------------|
| FunA01 | Authentication | User provides credentials: RUPAR/CRP (privileged FO), CSI (BO operator), SFTP (publisher/archivist) |
| FunA02 | New testata | Register new testata with anagrafica, thematic data, geolocation |
| FunA03 | Update testata | Modify testata info (description, address, state) |
| FunA04 | Suspend testata | Define suspension period for periodic publications |
| FunA05 | Modify periodicity | Change publication periodicity for active testata |
| FunB01 | New user | Register new subject: privileged user or SFTP user |
| FunB02 | Update user | Modify user info (role, state) |
| FunC01 | Simple search | Search one or more terms across entire archive |
| FunC02 | Advanced search | Search with AND/OR/EXACT, filters for place/time/theme |
| FunC03 | Explore catalogue | List testate by province, active/non-active, select by name |
| FunC04 | Browse edition | List editions of a testata, optionally by time range |
| FunD01 | Fascicolo management | Create, modify, download, delete, save fascicolo |
| FunD02 | Favourites management | Create/modify list of favourite testate or editions (privileged only) |
| FunE01 | Oblio | Intervene on specific pages for GDPR-compliant privacy changes (operator only) |

---

## MoSCoW Priority Summary

| Priority | Requirements | Count |
|----------|-------------|-------|
| MUST | RF-01÷05, RF-06÷10, RF-11÷13, RF-16÷21, RF-26÷28, RNF-01÷05, RNF-07÷10, RNF-12÷13 | 30 |
| SHOULD | RF-14÷15, RF-22÷25, RNF-06, RNF-11, RNF-14 | 8 |
| COULD | RF-05 (identity propagation), RNF-11 (rate limiting) | 2 |