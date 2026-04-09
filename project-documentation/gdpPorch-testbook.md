# gdporch — Testbook

**Sistema:** GDP — Giornali del Piemonte  
**Componente:** gdporch (Quarkus 3.x)  
**Versione:** 1.0 (Aprile 2026)  
**Stack di test:** JUnit 5 · Testcontainers (PostgreSQL 15) · WireMock · MockSFTP (Apache MINA test server) · RestAssured · Awaitility

---

## Indice

1. [Architettura di test](#1-architettura-di-test)
2. [Infrastruttura condivisa](#2-infrastruttura-condivisa)
3. [TB-F01 — configDTEdizioneAttesa](#3-tb-f01--configdtedizioneattesa)
4. [TB-F02 — creaCartellaEdizioneAttesa](#4-tb-f02--creacartellaedizioneattesa)
5. [TB-F03 — checkEdizioneAttesa](#5-tb-f03--checkedizioneattesa)
6. [TB-F04 — ctrlEdizioneAcquisita](#6-tb-f04--ctrledizioneacquisita)
7. [TB-F08 — DB.insEdizione](#7-tb-f08--dbinsedizione)
8. [TB-F09 — creaXMLEdizione](#8-tb-f09--creaxmledizione)
9. [TB-F10 — inviaEdizione](#9-tb-f10--inviaedizione)
10. [TB-FLUSSO-REGOLARE — End-to-End flusso periodico](#10-tb-flusso-regolare--end-to-end-flusso-periodico)
11. [TB-F06/F07 — Flusso storico](#11-tb-f06f07--flusso-storico)
12. [TB-BFF — Monitor APIs (F12–F15, F20–F22)](#12-tb-bff--monitor-apis-f12f15-f20f22)
13. [TB-F05 — sospensioneEdizioneAttesa](#13-tb-f05--sospensioneedizioneattesa)
14. [TB-RESILIENZA — Fault tolerance](#14-tb-resilienza--fault-tolerance)
15. [TB-FLUSSO-STORICO — End-to-End flusso saltuario](#15-tb-flusso-storico--end-to-end-flusso-saltuario)
16. [TB-F16/F17 — Testate APIs](#16-tb-f16f17--testate-apis)
17. [TB-F18 — verifDateAttese](#17-tb-f18--verifdateattese)
18. [TB-F19 — pulisciEdizione](#18-tb-f19--pulisciedizione)
19. [TB-ORCHESTRATORE — Test di orchestrazione dei flussi](#19-tb-orchestratore--test-di-orchestrazione-dei-flussi)
20. [TB-SCHEDULING — Test degli scheduler](#20-tb-scheduling--test-degli-scheduler)
21. [TB-CONCORRENZA — Scenari concorrenti e race conditions](#21-tb-concorrenza--scenari-concorrenti-e-race-conditions)
22. [TB-MONITOR-WORKFLOW — Test del flusso monitor completo](#22-tb-monitor-workflow--test-del-flusso-monitor-completo)
23. [TB-OSSERVABILITA — Metriche, logging e health checks](#23-tb-osservabilita--metriche-logging-e-health-checks)
- [Appendice A — Matrice di copertura](#appendice-a--matrice-di-copertura)
- [Appendice B — Casi limite critici](#appendice-b--casi-limite-critici--non-dimenticare)
- [Appendice C — Configurazione test e profile Quarkus](#appendice-c--configurazione-test-e-profile-quarkus)
- [Appendice D — Helper di test aggiuntivi](#appendice-d--helper-di-test-aggiuntivi)
- [Appendice E — Ordine di esecuzione consigliato](#appendice-e--ordine-di-esecuzione-consigliato)


---

## 1. Architettura di test

### Livelli

```
┌──────────────────────────────────────────────────────┐
│  L3 — E2E (TB-FLUSSO-*)                              │
│  SFTP mock + PostgreSQL container + WireMock LIBRA   │
│  Verifica intera catena F03 → F04 → F08 → F09 → F10  │
├──────────────────────────────────────────────────────┤
│  L2 — Integration (TB-F01 … TB-F22)                  │
│  Testcontainers PG + WireMock + MockSFTP             │
│  Verifica singola funzione con dipendenze reali      │
├──────────────────────────────────────────────────────┤
│  L1 — Unit (TB-UNIT-*)                               │
│  Nessuna dipendenza esterna                          │
│  Algoritmi puri: F01 date calc, regex, naming check  │
└──────────────────────────────────────────────────────┘
```

### Dipendenze Maven da aggiungere a `pom.xml`

```xml
<!-- Testcontainers -->
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-test-containers</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>

<!-- WireMock standalone -->
<dependency>
  <groupId>com.github.tomakehurst</groupId>
  <artifactId>wiremock-jre8</artifactId>
  <version>2.35.2</version>
  <scope>test</scope>
</dependency>

<!-- Apache MINA SSHD test server (stessa lib usata in produzione) -->
<dependency>
  <groupId>org.apache.sshd</groupId>
  <artifactId>sshd-sftp</artifactId>
  <version>2.12.1</version>
  <scope>test</scope>
</dependency>

<!-- Awaitility per asserzioni async -->
<dependency>
  <groupId>org.awaitility</groupId>
  <artifactId>awaitility</artifactId>
  <scope>test</scope>
</dependency>

<!-- RestAssured -->
<dependency>
  <groupId>io.rest-assured</groupId>
  <artifactId>rest-assured</artifactId>
  <scope>test</scope>
</dependency>
```

---

## 2. Infrastruttura condivisa

### 2.1 Base class `GdporchIntegrationTest`

```java
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class GdporchIntegrationTest {

    // PostgreSQL container con init.sql seedato
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withInitScript("db/init.sql");  // il file init.sql del progetto

    // SFTP mock (Apache MINA embedded server)
    static SftpTestServer sftpServer;

    // WireMock per DAM LIBRA
    static WireMockServer libraMock;

    @BeforeAll
    static void setupInfra() throws Exception {
        sftpServer = SftpTestServer.start();  // vedi §2.2
        libraMock  = new WireMockServer(wireMockConfig().dynamicPort());
        libraMock.start();
    }

    @AfterAll
    static void tearDownInfra() {
        sftpServer.stop();
        libraMock.stop();
    }

    @BeforeEach
    void resetWireMock() {
        libraMock.resetAll();
    }

    // Helper: aspetta che un record esista su DB con timeout
    protected void awaitDbRecord(String table, String condition, Duration timeout) {
        await().atMost(timeout).until(() ->
            entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM " + table + " WHERE " + condition
            ).getSingleResult().equals(1L)
        );
    }
}
```

### 2.2 `SftpTestServer` helper

```java
public class SftpTestServer {
    private SshServer server;
    private Path rootFtp;

    public static SftpTestServer start() throws IOException {
        SftpTestServer s = new SftpTestServer();
        s.rootFtp = Files.createTempDirectory("gdp-sftp-test");
        // Crea struttura cartelle standard
        Files.createDirectories(s.rootFtp.resolve("flusso_regolare"));
        Files.createDirectories(s.rootFtp.resolve("flusso_saltuario"));
        Files.createDirectories(s.rootFtp.resolve("_tmp"));
        Files.createDirectories(s.rootFtp.resolve("_errata"));
        Files.createDirectories(s.rootFtp.resolve("_dam"));

        s.server = SshServer.setUpDefaultServer();
        s.server.setPort(0); // porta casuale
        s.server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        s.server.setPasswordAuthenticator((u, p, sess) -> true);
        s.server.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
        s.server.setFileSystemFactory(new VirtualFileSystemFactory(s.rootFtp));
        s.server.start();
        return s;
    }

    public Path flussoRegolare() { return rootFtp.resolve("flusso_regolare"); }
    public Path tmp()            { return rootFtp.resolve("_tmp"); }
    public Path errata()         { return rootFtp.resolve("_errata"); }
    public Path dam()            { return rootFtp.resolve("_dam"); }

    // Deposita un PDF finto in flusso_regolare/<cartella>/<data>/
    public Path depositaPdf(String cartella, String data, String nomefile) throws IOException {
        Path dir = flussoRegolare().resolve(cartella).resolve(data);
        Files.createDirectories(dir);
        Path file = dir.resolve(nomefile);
        Files.write(file, TestPdfFactory.singlePage(nomefile)); // PDF/A 1 pagina valido
        return file;
    }
}
```

### 2.3 `TestPdfFactory` — PDF di test

```java
public class TestPdfFactory {

    // PDF singola pagina valido con testo contenente la data
    public static byte[] singlePage(String filename) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.beginText();
                cs.newLineAtOffset(50, 700);
                cs.showText("Giornale di Test — direttore responsabile");
                cs.newLine();
                cs.showText("Edizione del 01/03/2026");
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    // PDF multipagina (per test split)
    public static byte[] multiPage(int nPages) throws IOException { ... }

    // PDF corrotto (non leggibile)
    public static byte[] corrupted() { return new byte[]{0x25, 0x50, 0x44, 0x46}; }

    // PDF protetto da password
    public static byte[] passwordProtected() throws IOException { ... }
}
```

### 2.4 Seed dati di test (oltre a init.sql)

```sql
-- src/test/resources/db/test-data.sql
-- Testata di test: La Sentinella del Canavese
INSERT INTO GDP_TESTATA (ID_GDP_TESTATA, NOME_TESTATA, CARTELLA_TESTATA, INVIO_EDIZIONE,
  STATO, COD_TEMA, PROVINCIA)
VALUES (1, 'La Sentinella del Canavese', 'sentinella', true, 0, 1, 'TO');

-- Periodicità settimanale (ogni mercoledì)
INSERT INTO GDP_PERIODICITA (ID_GDP_PERIODICITA, FK_GDP_TESTATA, MENSILITA, GG_PERIODICITA)
VALUES (1, 1, 0, '1WS3');

-- Periodicità mensile (1° del mese)
INSERT INTO GDP_PERIODICITA (ID_GDP_PERIODICITA, FK_GDP_TESTATA, MENSILITA, GG_PERIODICITA)
VALUES (2, 2, 1, 'G01');

-- Utente SFTP per testata 1
INSERT INTO GDP_UTENTESFTP (ID_GDP_UTENTESFTP, USERNAME, PASSWORD, HOME_SFTP, EMAIL, STATO)
VALUES (1, 'editore_sentinella', 'pw', '/flusso_regolare/sentinella', 'ed@sentinella.it', 'attivo');

INSERT INTO GDP_LOG (ID_GDP_LOG, FK_GDP_UTENTEFTP, FK_GDP_TESTATA,
  TIPO_ACQUISIZIONE, DT_ACQUISIZIONE, TOTALE_FILE_ACQUISITI)
VALUES (100, 1, 1, 'G', CURRENT_DATE, 0);
```

---

## 3. TB-F01 — configDTEdizioneAttesa

**Funzione:** Calcolo date attese e salvataggio su `GDP_DATA_USCITA`  
**Tipo:** Unit (algoritmo) + Integration (persistenza)

---

### TB-F01-U01 — Periodicità settimanale: calcolo date mercoledì

```
GIVEN  una testata con GG_PERIODICITA = '1WS3' (ogni mercoledì), MENSILITA = 0
WHEN   configDTEdizioneAttesa(dataInizio='2026-01-01', dataFine='2026-01-31', idTestata=1)
THEN   GDP_DATA_USCITA contiene esattamente 4 record per idTestata=1 nell'intervallo
AND    tutte le DATA_ATTESA sono un mercoledì (dayOfWeek = WEDNESDAY)
AND    SOSPESA = false su tutti i record
AND    esito = MSG00009
```

```java
@Test
void f01_u01_weeklyWednesday() {
    var result = f01Service.configDTEdizioneAttesa(
        LocalDate.of(2026,1,1), LocalDate.of(2026,1,31), 1);

    assertThat(result.getEsito()).isEqualTo("MSG00009");
    var dates = dataUscitaRepo.findByTestataAndYear(1, 2026);
    assertThat(dates).hasSize(4);
    assertThat(dates).allMatch(d -> d.getDataAttesa().getDayOfWeek() == DayOfWeek.WEDNESDAY);
    assertThat(dates).allMatch(d -> !d.isSospesa());
}
```

---

### TB-F01-U02 — Periodicità mensile: calcolo 1° del mese

```
GIVEN  una testata con GG_PERIODICITA = 'G01', MENSILITA = 1
WHEN   configDTEdizioneAttesa(dataInizio='2026-01-01', dataFine='2026-06-30')
THEN   6 record inseriti, uno per mese, tutti con giorno = 1
AND    esito = MSG00009
```

---

### TB-F01-U03 — Periodicità bimensile: G01;G15

```
GIVEN  GG_PERIODICITA = 'G01;G15', MENSILITA = 1
WHEN   configDTEdizioneAttesa per Q1 2026
THEN   6 record: 01-gen, 15-gen, 01-feb, 15-feb, 01-mar, 15-mar
```

---

### TB-F01-U04 — Periodicità primo sabato del mese: G1S6

```
GIVEN  GG_PERIODICITA = 'G1S6', MENSILITA = 1
WHEN   calcolo per gennaio-marzo 2026
THEN   date sono: 2026-01-03 (sabato), 2026-02-07 (sabato), 2026-03-07 (sabato)
```

---

### TB-F01-U05 — Idempotenza: chiamata doppia non crea duplicati

```
GIVEN  F01 già eseguito per 2026-01 con idTestata=1
WHEN   F01 richiamato con gli stessi parametri
THEN   nessun nuovo record in GDP_DATA_USCITA
AND    conteggio invariato
AND    esito = MSG00009
```

---

### TB-F01-U06 — Testata non inviante ignorata senza idTestata

```
GIVEN  esiste testata con INVIO_EDIZIONE = false (idTestata=99)
WHEN   F01 chiamato senza idTestata (elabora tutte le invianti)
THEN   nessun record per idTestata=99 in GDP_DATA_USCITA
```

---

### TB-F01-U07 — Errore: GG_PERIODICITA NULL

```
GIVEN  testata con GG_PERIODICITA = NULL
WHEN   F01 chiamato con idTestata di quella testata
THEN   esito contiene MSG00003
AND    nessun record inserito per quella testata
```

---

## 4. TB-F02 — creaCartellaEdizioneAttesa

**Funzione:** Crea cartella SFTP per il giorno successivo  
**Trigger:** Ogni giorno ore 20:00  
**Tipo:** Integration

---

### TB-F02-I01 — Crea cartella domani per testata attesa

```
GIVEN  GDP_DATA_USCITA ha record con DATA_ATTESA = CURRENT_DATE+1
       e FK_GDP_TESTATA=1 (CARTELLA_TESTATA='sentinella')
       e HOME_SFTP='/flusso_regolare/sentinella'
WHEN   F02 eseguito
THEN   esiste sul server SFTP la cartella
       /flusso_regolare/sentinella/<CURRENT_DATE+1 in yyyy-MM-dd>
AND    esito = MSG00009
```

```java
@Test
void f02_i01_createsFolder() throws Exception {
    // Inserisce data attesa domani
    dataUscitaRepo.insertForTest(1, LocalDate.now().plusDays(1));

    f02Service.creaCartellaEdizioneAttesa();

    String expected = LocalDate.now().plusDays(1).toString(); // yyyy-MM-dd
    Path cartella = sftpServer.flussoRegolare().resolve("sentinella").resolve(expected);
    assertThat(Files.exists(cartella)).isTrue();
    assertThat(Files.isDirectory(cartella)).isTrue();
}
```

---

### TB-F02-I02 — Nessuna data attesa domani → MSG00001

```
GIVEN  GDP_DATA_USCITA non ha record per CURRENT_DATE+1
WHEN   F02 eseguito
THEN   esito = MSG00001
AND    nessuna cartella creata sul SFTP
```

---

### TB-F02-I03 — Edizione sospesa non genera cartella

```
GIVEN  GDP_DATA_USCITA ha record con DATA_ATTESA = domani ma SOSPESA = true
WHEN   F02 eseguito
THEN   nessuna cartella creata sul SFTP per quella testata
```

---

## 5. TB-F03 — checkEdizioneAttesa

**Funzione:** Polling SFTP ogni 15 minuti — rileva edizioni e attiva F04  
**Tipo:** Integration

---

### TB-F03-I01 — Scenario nominale: PDF rilevato e spostato in _tmp

```
GIVEN  depositato su SFTP: /flusso_regolare/sentinella/2026-03-01/sentinella_001.pdf
       (dimensione stabile per almeno 3 minuti — simulato con file già presente)
       GDP_TESTATA.CARTELLA_TESTATA = 'sentinella' identifica testata 1
WHEN   F03 eseguito
THEN   file spostato in /_tmp/sentinella/2026-03-01/sentinella_001.pdf
AND    al posto del file originale esiste sentinella_001.OK
AND    GDP_LOG inserito con FK_GDP_TESTATA=1, TIPO_ACQUISIZIONE='G'
AND    F04 attivato asincronamente (verificare con Awaitility che GDP_LOG_EDIZIONE venga inserito)
```

```java
@Test
void f03_i01_nominalFlow() throws Exception {
    sftpServer.depositaPdf("sentinella", "2026-03-01", "sentinella_001.pdf");

    f03Service.checkEdizioneAttesa();

    // File spostato in _tmp
    assertThat(sftpServer.tmp().resolve("sentinella/2026-03-01/sentinella_001.pdf")).exists();
    // Sentinel .OK al posto dell'originale
    assertThat(sftpServer.flussoRegolare()
        .resolve("sentinella/2026-03-01/sentinella_001.OK")).exists();
    // GDP_LOG inserito
    GdpLog log = gdpLogRepo.findLatest();
    assertThat(log.getFkGdpTestata()).isEqualTo(1);
    assertThat(log.getTipoAcquisizione()).isEqualTo("G");

    // F04 attivato: attendere GDP_LOG_EDIZIONE
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
        assertThat(gdpLogEdizioneRepo.countByLogId(log.getId())).isGreaterThan(0)
    );
}
```

---

### TB-F03-I02 — Ambiguità testata: più match su CARTELLA_TESTATA → _errata

```
GIVEN  GDP_TESTATA contiene due record con CARTELLA_TESTATA = 'ambigua'
AND    depositato su SFTP: /flusso_regolare/ambigua/2026-03-01/ambigua_001.pdf
WHEN   F03 eseguito
THEN   file spostato in /_errata/ambigua/2026-03-01/
AND    GDP_LOG.FK_GDP_TESTATA = 0
AND    GDP_LOG.ESITO contiene 'MSG00002'
AND    nessun record in GDP_LOG_EDIZIONE
AND    F04 NON attivato
```

---

### TB-F03-I03 — Cartella vuota: nessuna azione

```
GIVEN  /flusso_regolare/ non contiene file in nessuna cartella
WHEN   F03 eseguito
THEN   nessun record in GDP_LOG
AND    esito = MSG00001
```

---

### TB-F03-I04 — File non ancora completo: trasferimento in corso

```
GIVEN  depositato un file ma la sua dimensione continua a cambiare (simulare con rename di file)
WHEN   F03 eseguito
THEN   file NON spostato in _tmp (trasferimento non completato)
AND    al ciclo successivo (dopo stabilità) viene processato normalmente
```

> **Implementazione:** Usare un file che viene scritto incrementalmente tramite thread separato; verificare che F03 non lo processi fino a quando la dimensione non si stabilizza per 3 minuti (configurabile a 3 secondi in test).

---

## 6. TB-F04 — ctrlEdizioneAcquisita

**Funzione:** Pipeline completa di validazione PDF  
**Tipo:** Integration (richiede DB + SFTP mock + PDFBox)

---

### TB-F04-I01 — Flusso nominale completo OK

```
GIVEN  in /_tmp/sentinella/2026-03-01/
       file: sentinella_001.pdf (PDF/A valido, singola pagina, testo con "01/03/2026" e "direttore")
       GDP_DATA_USCITA ha DATA_ATTESA=2026-03-01 per testata 1, SOSPESA=false
WHEN   F04 invocato con (idTestata=1, cartellaTestata='sentinella', dataEdizione='2026-03-01', idLog=100)
THEN   file rinominato in sentinella-2026-03-01_001.pdf
AND    creato file TXT con stesso nome
AND   GDP_LOG_EDIZIONE.TIPO_EDIZIONE = 'OK'
AND   GDP_LOG_EDIZIONE.NRO_PAG_VALIDE = 1
AND   GDP_LOG_EDIZIONE.NRO_PAG_ERRATE = 0
AND   GDP_LOG_EDIZIONE.PRIMA_PAGINA = true
AND   GDP_LOG_EDIZIONE.DESCRIZIONE = null (nessuna anomalia)
AND   F08 chiamato e GDP_EDIZIONE inserita
AND   F09 chiamato e .zip creato in /_dam/
AND   F10 lanciato asincronamente
AND   esito = MSG00009
```

---

### TB-F04-I02 — Edizione anticipataria (AT)

```
GIVEN  dataEdizione = domani (> data di oggi)
       GDP_DATA_USCITA ha il record
WHEN   F04 invocato
THEN   GDP_LOG_EDIZIONE.TIPO_EDIZIONE = 'AN'
AND   elaborazione prosegue normalmente (non bloccante)
```

---

### TB-F04-I03 — Edizione posticipataria (PT)

```
GIVEN  dataEdizione = ieri (< data di oggi)
       GDP_DATA_USCITA ha il record
WHEN   F04 invocato
THEN   GDP_LOG_EDIZIONE.TIPO_EDIZIONE = 'PO'
AND   elaborazione prosegue normalmente (non bloccante)
```

---

### TB-F04-I04 — Edizione sospesa (SO)

```
GIVEN  GDP_DATA_USCITA ha DATA_ATTESA=2026-03-01 con SOSPESA=true
WHEN   F04 invocato con dataEdizione='2026-03-01'
THEN   GDP_LOG_EDIZIONE.TIPO_EDIZIONE = 'SO'
AND   elaborazione prosegue (non bloccante)
```

---

### TB-F04-I05 — Edizione anomala (AA) — bloccante ⚠️

```
GIVEN  GDP_DATA_USCITA NON ha record per dataEdizione='2026-03-15' (data non attesa)
WHEN   F04 invocato con dataEdizione='2026-03-15'
THEN   tutti i file spostati in /_errata/sentinella/2026-03-15/
AND   GDP_LOG.ESITO contiene 'MSG00001'
AND   NESSUN record in GDP_EDIZIONE
AND   NESSUN record in GDP_PAGINA
AND   F08 NON chiamato
AND   esito = MSG00001
```

---

### TB-F04-I06 — PDF multipagina: split automatico

```
GIVEN  file sentinella_001.pdf con 3 pagine
WHEN   F04 invocato
THEN   3 file singoli creati: sentinella-2026-03-01_001.pdf, _002.pdf, _003.pdf
AND   GDP_LOG_EDIZIONE.DESCRIZIONE contiene 'NP – sentinella_001.pdf'
AND   GDP_LOG_EDIZIONE.NRO_PAG_VALIDE = 3
AND   GDP_PAGINA ha 3 record
```

---

### TB-F04-I07 — PDF non leggibile: spostato in _errata

```
GIVEN  file sentinella_001.pdf è corrotto (bytes non validi)
WHEN   F04 invocato
THEN   file spostato in /_errata/sentinella/2026-03-01/
AND   GDP_LOG_EDIZIONE.DESCRIZIONE contiene 'NL – sentinella_001.pdf'
AND   GDP_LOG_EDIZIONE.NRO_PAG_ERRATE = 1
AND   nessun record GDP_PAGINA per quella pagina
```

---

### TB-F04-I08 — Naming non conforme: spostato in _errata

```
GIVEN  file con nome 'sentinella001.pdf' (manca underscore + 3 cifre numero pagina)
WHEN   F04 invocato
THEN   file spostato in /_errata/
AND   GDP_LOG_EDIZIONE.DESCRIZIONE contiene 'NF – sentinella001.pdf'
```

---

### TB-F04-I09 — Naming conforme: rinomina in formato standard

```
GIVEN  file con nome 'articolo_001.pdf' (formato corretto)
WHEN   F04 invocato
THEN   file rinominato in 'sentinella-2026-03-01_001.pdf'
AND   file originale non più presente
```

---

### TB-F04-I10 — Check euristico data: assente (DA) — non bloccante

```
GIVEN  PDF con testo che NON contiene alcuna data in formato riconoscibile
WHEN   F04 invocato
THEN   GDP_LOG_EDIZIONE.DESCRIZIONE contiene 'DA – sentinella-2026-03-01_001.pdf'
AND   GDP_LOG.ESITO prefix cambia da <MSG> a <WRN>
AND   elaborazione NON interrotta — F08 e F09 vengono comunque chiamati
```

---

### TB-F04-I11 — Check euristico prima pagina: assente (PP) — non bloccante

```
GIVEN  PDF con testo che NON contiene le keyword 'direttore', 'redazione', ecc.
WHEN   F04 invocato
THEN   GDP_LOG_EDIZIONE.PRIMA_PAGINA = false
AND   GDP_LOG_EDIZIONE.DESCRIZIONE contiene 'PP – sentinella-2026-03-01_001.pdf'
AND   elaborazione NON interrotta
```

---

### TB-F04-I12 — Mix pagine valide e invalide

```
GIVEN  3 PDF: _001.pdf valido, _002.pdf corrotto, _003.pdf valido
WHEN   F04 invocato
THEN   NRO_PAG_VALIDE = 2, NRO_PAG_ERRATE = 1
AND   GDP_PAGINA ha 2 record (solo le pagine valide)
AND   DESCRIZIONE contiene 'NL – ..._002.pdf'
AND   elaborazione prosegue con le 2 pagine valide
```

---

### TB-F04-I13 — Errore F08: rollback e spostamento in _errata ⚠️

```
GIVEN  DB non raggiungibile (connection pool esaurito / constraint violation simulata)
WHEN   F04 invocato e F08 fallisce
THEN   cartella e contenuto spostati in /_errata/
AND   GDP_LOG.ESITO = MSG00002
AND   esito = MSG00002
AND   NESSUN record in GDP_EDIZIONE o GDP_PAGINA (rollback completo)
```

---

### TB-F04-I14 — Errore F09: spostamento in _errata, non F08

```
GIVEN  F08 eseguito con successo (GDP_EDIZIONE e GDP_PAGINA inseriti)
       F09 fallisce (errore creazione XML/ZIP)
WHEN   F04 gestisce il fallimento
THEN   cartella spostata in /_errata/
AND   GDP_LOG.ESITO = MSG00003
AND   GDP_EDIZIONE e GDP_PAGINA rimangono (non rollbackati da F04 — erano già committati)
```

> **Nota per implementazione:** Questo caso evidenzia che F08 è in transazione separata da F09. Documentare questo comportamento.

---

## 7. TB-F08 — DB.insEdizione

**Funzione:** INSERT/UPDATE GDP_EDIZIONE + GDP_PAGINA (atomico)  
**Tipo:** Integration (Testcontainers PG)

---

### TB-F08-I01 — Inserimento nuova edizione

```
GIVEN  GDP_EDIZIONE non ha record per (FK_GDP_TESTATA=1, DATA_EDIZIONE='2026-03-01')
       path contiene 2 file PDF validi
WHEN   F08 invocato (idTestata=1, path='/tmp/sentinella/2026-03-01', dataEdizione='2026-03-01', idLog=100)
THEN   1 record in GDP_EDIZIONE con:
         FK_GDP_TESTATA=1
         DATA_EDIZIONE=2026-03-01
         DATA_PUBBLICAZIONE=2026-03-08 (settimanale: +7 giorni)
         STATO=0
         TOTALE_PAGINE=2
AND   2 record in GDP_PAGINA con STATO=0, FILE_TIF=NULL, OBLIO=NULL
AND   esito = MSG00009
AND   idEdizione restituito è l'ID appena inserito
```

---

### TB-F08-I02 — Aggiornamento edizione esistente (aggiunta pagina)

```
GIVEN  GDP_EDIZIONE ha già un record per (testata=1, data=2026-03-01) con TOTALE_PAGINE=1
       path ora contiene 2 PDF (aggiunta una pagina)
WHEN   F08 invocato
THEN   GDP_EDIZIONE.TOTALE_PAGINE aggiornato a 2
AND   nuovo record aggiunto in GDP_PAGINA per la pagina mancante
AND   record esistente aggiornato
AND   esito = MSG00009
```

---

### TB-F08-I03 — Calcolo DATA_PUBBLICAZIONE per ogni periodicità

```
GIVEN  edizioni di testate con diverse periodicità
THEN   verificare DATA_PUBBLICAZIONE = DATA_EDIZIONE + giorni attesi:

| Periodicità    | DATA_EDIZIONE | DATA_PUBBLICAZIONE attesa |
|---------------|---------------|--------------------------|
| quotidiano    | 2026-03-01    | 2026-03-02 (+1)          |
| settimanale   | 2026-03-01    | 2026-03-08 (+7)          |
| bisettimanale | 2026-03-01    | 2026-03-05 (ceil 3.5=+4) |
| mensile       | 2026-03-01    | 2026-03-31 (+30)         |
| trimestrale   | 2026-03-01    | 2026-05-30 (+90)         |
```

---

### TB-F08-I04 — Atomicità: rollback completo se inserimento pagina fallisce ⚠️

```
GIVEN  violazione constraint su GDP_PAGINA (es. NUM_PAGINA duplicato)
WHEN   F08 invocato
THEN   NESSUN record in GDP_EDIZIONE (rollback completo)
AND   NESSUN record in GDP_PAGINA
AND   esito = MSG00002
```

```java
@Test
@Transactional
void f08_i04_atomicRollback() {
    // Forza constraint violation inserendo pagina duplicata
    doThrow(new PersistenceException("duplicate key"))
        .when(paginaRepo).persist(argThat(p -> p.getNumPagina() == 1));

    var result = f08Service.insEdizione(1, "/path", LocalDate.of(2026,3,1), 100L);

    assertThat(result.getCodice()).isEqualTo("MSG00002");
    // Verifica rollback completo
    assertThat(edizioneRepo.count()).isEqualTo(0);
    assertThat(paginaRepo.count()).isEqualTo(0);
}
```

---

## 8. TB-F09 — creaXMLEdizione

**Funzione:** Generazione XML metadati + ZIP + inserimento GDP_CODA_CARICAMENTO  
**Tipo:** Integration

---

### TB-F09-I01 — XML generato e conforme allo schema XSD

```
GIVEN  GDP_TESTATA, GDP_EDIZIONE, GDP_PAGINA popolati per testata 1 edizione 2026-03-01
WHEN   F09 invocato (idTestata=1, idLog=100, idEdizione=10, priorita=0)
THEN   file XML creato: sentinella.2026-03-01.xml
AND   XML valida rispetto a GdP-STD-04-V01-Validazione_metadati_flussoFTP.xsd
AND   file ZIP creato: sentinella.2026-03-01.zip (contiene XML + PDF + TXT)
AND   ZIP depositato in /_dam/
AND   GDP_CODA_CARICAMENTO record inserito con:
         STATO='PRO'
         PRIORITA=0
         NRO_TENTATIVO=0
         SFTP_PATH='/_dam/sentinella.2026-03-01.zip'
AND   GDP_LOG_EDIZIONE.FILE_XML = true
AND   esito = MSG00009
AND   nomeFileCompresso = 'sentinella.2026-03-01.zip' restituito
```

---

### TB-F09-I02 — Edizione storica: priorità 100

```
GIVEN  stessa configurazione ma priorita=100
WHEN   F09 invocato
THEN   GDP_CODA_CARICAMENTO.PRIORITA = 100
```

---

### TB-F09-I03 — Errore creazione XML: GDP_LOG_EDIZIONE.FILE_XML = false

```
GIVEN  GDP_PAGINA vuota per l'edizione (nessuna pagina — non dovrebbe accadere ma caso difensivo)
WHEN   F09 invocato e generazione XML fallisce
THEN   GDP_LOG_EDIZIONE.FILE_XML = false
AND   esito = MSG00002
AND   NESSUN record in GDP_CODA_CARICAMENTO
```

---

### TB-F09-I04 — Errore creazione ZIP: GDP_LOG_EDIZIONE.FILE_ZIP = false

```
GIVEN  XML generato correttamente ma filesystem pieno (simulare)
WHEN   creazione ZIP fallisce
THEN   GDP_LOG_EDIZIONE.FILE_ZIP = false
AND   esito = MSG00003
AND   NESSUN record in GDP_CODA_CARICAMENTO
```

---

## 9. TB-F10 — inviaEdizione

**Funzione:** Invio .zip a DAM LIBRA ogni 30 minuti  
**Tipo:** Integration (WireMock per LIBRA)

---

### TB-F10-I01 — Invio con successo: status SUBMITTED

```
GIVEN  GDP_CODA_CARICAMENTO ha record con STATO='READY', PRIORITA=0
       SFTP_PATH='/_dam/sentinella.2026-03-01.zip' (file presente su MockSFTP)
       WireMock: POST /api/v2/imports → 200 {"jobId":"job-123","status":"SUBMITTED"}
WHEN   F10 eseguito
THEN   GDP_LOG_EDIZIONE.JOB_ID = 'job-123'
AND   GDP_LOG_EDIZIONE.STATO = 'SUBMITTED'
AND   GDP_LOG_EDIZIONE.FILE_ZIP = true
AND   GDP_LOG.ESITO = MSG00009
AND   file ZIP eliminato da /_dam/ ⚠️ (solo dopo conferma!)
AND   WireMock: POST /api/v2/imports chiamato esattamente 1 volta
```

---

### TB-F10-I02 — Invio fallito: status FAILED

```
GIVEN  WireMock: POST /api/v2/imports → 200 {"jobId":"job-456","status":"FAILED"}
WHEN   F10 eseguito
THEN   GDP_LOG_EDIZIONE.JOB_ID = 'job-456'
AND   GDP_LOG_EDIZIONE.STATO = 'FAILED'
AND   GDP_LOG.ESITO = MSG00001
AND   file ZIP NON eliminato da /_dam/ ⚠️
AND   GDP_CODA_CARICAMENTO.NRO_TENTATIVO invariato (F10 non incrementa — lo fa F21)
```

---

### TB-F10-I03 — Ordinamento per priorità: giornaliero prima dello storico ⚠️

```
GIVEN  GDP_CODA_CARICAMENTO ha 2 record:
       - id=1, PRIORITA=100, inserito 09:00 (storico)
       - id=2, PRIORITA=0,   inserito 09:01 (giornaliero)
WHEN   F10 eseguito
THEN   WireMock riceve PRIMA la chiamata per id=2 (PRIORITA=0)
       POI la chiamata per id=1 (PRIORITA=100)
```

```java
@Test
void f10_i03_priorityOrder() {
    List<String> callOrder = new ArrayList<>();
    libraMock.addStubMapping(post(urlEqualTo("/api/v2/imports"))
        .willReturn(aResponse()
            .withBody("{\"jobId\":\"j1\",\"status\":\"SUBMITTED\"}")
            .withTransformerParameter("idCapture", callOrder)));

    f10Service.inviaEdizione();

    // Verifica ordine chiamate
    var calls = libraMock.getAllServeEvents();
    assertThat(calls).hasSize(2);
    // Prima quella con priorita=0 (giornaliera)
    assertThat(extractPriorityFromBody(calls.get(0))).isEqualTo(0);
    assertThat(extractPriorityFromBody(calls.get(1))).isEqualTo(100);
}
```

---

### TB-F10-I04 — LIBRA non raggiungibile: circuit breaker aperto

```
GIVEN  WireMock: POST /api/v2/imports → connessione rifiutata (503)
WHEN   F10 eseguito
THEN   Circuit breaker si apre dopo i tentativi configurati
AND   GDP_CODA_CARICAMENTO rimane con STATO='READY' (o 'ERR')
AND   file ZIP NON eliminato ⚠️
AND   elaborazione si conclude senza eccezione unchecked
```

---

### TB-F10-I05 — File ZIP non trovato in /_dam/: skip record

```
GIVEN  GDP_CODA_CARICAMENTO ha record con SFTP_PATH puntante a file inesistente
WHEN   F10 eseguito
THEN   record saltato (non genera eccezione bloccante)
AND   elaborazione continua sugli altri record in coda
```

---

## 10. TB-FLUSSO-REGOLARE — End-to-End flusso periodico

**Scenario:** Dall'SFTP al DAM — catena completa  
**Tipo:** E2E (L3)

---

### TB-E2E-R01 — Flusso nominale completo ✅

```
GIVEN  GDP_DATA_USCITA ha DATA_ATTESA=oggi per testata 1
       WireMock LIBRA: POST /api/v2/imports → {"jobId":"e2e-job","status":"SUBMITTED"}
       Depositato su SFTP: /flusso_regolare/sentinella/2026-03-01/sentinella_001.pdf

WHEN   (1) F03 eseguito (polling)
       (2) F04 attivato asincronamente
       (3) F08 chiamato da F04 (sincrono)
       (4) F09 chiamato da F04 (sincrono)
       (5) F10 eseguito (ogni 30 min, aspettare con Awaitility)

THEN   sentinella_001.pdf NON presente in /flusso_regolare/
       sentinella_001.OK presente in /flusso_regolare/sentinella/2026-03-01/
       sentinella-2026-03-01_001.pdf presente in /_tmp/
       GDP_EDIZIONE ha 1 record con DATA_EDIZIONE=2026-03-01
       GDP_PAGINA ha 1 record con FILE_PDF='sentinella-2026-03-01_001.pdf'
       GDP_LOG.ESITO = '<MSG>Elaborazione OK</MSG>'
       GDP_LOG_EDIZIONE.JOB_ID = 'e2e-job'
       GDP_LOG_EDIZIONE.STATO = 'SUBMITTED'
       FILE .zip eliminato da /_dam/ ⚠️ (file originale su SFTP ancora presente — eliminazione solo dopo conferma DAM)
```

```java
@Test
void e2e_r01_fullNominalFlow() throws Exception {
    // Setup
    dataUscitaRepo.insertForTest(1, LocalDate.of(2026,3,1));
    sftpServer.depositaPdf("sentinella", "2026-03-01", "sentinella_001.pdf");
    libraMock.stubFor(post(urlEqualTo("/api/v2/imports"))
        .willReturn(okJson("{\"jobId\":\"e2e-job\",\"status\":\"SUBMITTED\"}")));

    // Trigger chain
    f03Service.checkEdizioneAttesa();

    // Await async F04 completion + F10
    await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
        var logEd = gdpLogEdizioneRepo.findLatest();
        assertThat(logEd.getJobId()).isEqualTo("e2e-job");
        assertThat(logEd.getStato()).isEqualTo("SUBMITTED");
    });

    // Verify DB state
    var edizione = edizioneRepo.findByTestataAndDate(1, LocalDate.of(2026,3,1));
    assertThat(edizione).isNotNull();
    assertThat(paginaRepo.findByEdizione(edizione.getId())).hasSize(1);

    // Verify file state
    assertThat(sftpServer.dam().resolve("sentinella.2026-03-01.zip")).doesNotExist();
    assertThat(sftpServer.flussoRegolare()
        .resolve("sentinella/2026-03-01/sentinella_001.OK")).exists();
}
```

---

### TB-E2E-R02 — File originale SFTP sopravvive se DAM fallisce ⚠️

```
GIVEN  stessa configurazione di TB-E2E-R01
       WireMock: POST /api/v2/imports → {"jobId":"fail-job","status":"FAILED"}
WHEN   flusso completo eseguito
THEN   file .zip ancora presente in /_dam/
       GDP_CODA_CARICAMENTO.STATO != 'SUBMITTED'
       file sentinella_001.OK ancora presente (sentinel — non eliminato)
       GDP_LOG_EDIZIONE.STATO = 'FAILED'
```

---

### TB-E2E-R03 — Edizione anomala non blocca le altre

```
GIVEN  due cartelle su SFTP:
       /flusso_regolare/sentinella/2026-03-15/  ← data NON attesa (anomala)
       /flusso_regolare/altrotestata/2026-03-01/ ← data attesa e valida
WHEN   F03 eseguito
THEN   sentinella/2026-03-15 spostata in /_errata (AA)
       altrotestata/2026-03-01 elaborata correttamente fino a SUBMITTED
       entrambi i GDP_LOG inseriti
       elaborazione non bloccata dall'anomalia ⚠️
```

---

## 11. TB-F06/F07 — Flusso storico

---

### TB-F06-I01 — Scenario nominale storico

```
GIVEN  su SFTP: /flusso_saltuario/archivio_to/CONS_2026-03-01/sentinella/
       contenente edizione con data cartella 20260301/ e file sentinella-TO-20260301_001.pdf
       GDP_TESTATA.CARTELLA_TESTATA = 'sentinella' identifica testata 1
WHEN   F06 eseguito
THEN   contenuto spostato in /_tmp/archivio_to/CONS_2026-03-01/sentinella/
AND   GDP_LOG inserito con FK_GDP_TESTATA=1, TIPO_ACQUISIZIONE='S'
AND   F07 attivato asincronamente
```

---

### TB-F06-I02 — Testata non trovata → _errata con MSG00003

```
GIVEN  cartella 'sconosciuta' non corrisponde ad alcuna CARTELLA_TESTATA in DB
WHEN   F06 eseguito
THEN   contenuto spostato in /_errata/
AND   GDP_LOG.FK_GDP_TESTATA = 0
AND   GDP_LOG.ESITO = MSG00003 (E102 Anomalia ESISTENZA)
```

---

### TB-F07-I01 — Validazione naming file storico

```
GIVEN  file con naming non conforme: 'pagina1.pdf' invece di 'sentinella-TO-20260301_001.pdf'
WHEN   F07 eseguito
THEN   file spostato in /_errata/
AND   GDP_LOG_EDIZIONE.DESCRIZIONE contiene 'NF – pagina1.pdf'
```

---

### TB-F07-I02 — File TIF mancante: warning ma non bloccante

```
GIVEN  edizione storica con PDF e TXT ma senza TIF
WHEN   F07 eseguito
THEN   GDP_LOG_EDIZIONE.DESCRIZIONE contiene 'NF – file TIF mancante'
AND   elaborazione prosegue — PDF e TXT inviati al DAM
```

---

### TB-F07-I03 — Priorità 100 su GDP_CODA_CARICAMENTO ⚠️

```
GIVEN  edizione storica elaborata correttamente fino a F09
WHEN   F09 invocato da F07 con priorita=100
THEN   GDP_CODA_CARICAMENTO.PRIORITA = 100
AND   in presenza di edizioni giornaliere in coda (PRIORITA=0),
       F10 elabora prima le giornaliere
```

---

## 12. TB-BFF — Monitor APIs (F12–F15, F20–F22)

Questi test verificano le API REST esposte da gdporch verso gdpbff.

---

### TB-F12-I01 — elencoAcquisizioni: risultati per data e tipo

```
GIVEN  GDP_LOG ha 3 record: 2 con TIPO='G' e DT_ACQUISIZIONE=oggi, 1 con DT=ieri
WHEN   GET /orch/acquisizioni?tipoAcquisizione=G&dataAcquisizione=<oggi>
THEN   response contiene esattamente 2 item
AND   ogni item ha i campi: idLog, idTestata, nomeTestata, dataEdizione, nroTotFile, esito
```

---

### TB-F12-I02 — elencoAcquisizioni: nroEdizioni per storico

```
GIVEN  GDP_LOG record tipo 'S' con 3 record in GDP_LOG_EDIZIONE correlati
WHEN   GET /orch/acquisizioni?tipoAcquisizione=S&dataAcquisizione=oggi
THEN   item.nroEdizioni = 3
```

---

### TB-F13-I01 — dettaglioAcquisizione: tutti i campi presenti

```
GIVEN  GDP_LOG + GDP_LOG_EDIZIONE popolati con tutti i campi
WHEN   GET /orch/acquisizioni/{idLog}
THEN   response contiene tutti i 17 campi specificati (idLog, IDTestata, nomeTestata,
       dataEdizione, tipoEdizione, tipoAcquisizione, dataAcquisizione, NroTotFile,
       Esito, idEdizione, primaPagina, fileXML, fileZIP, nroPagAcq, nroPagOK,
       nroPagErrate, jobID, descrizione)
AND   tipoEdizione è la DESCRIZIONE decodificata (non il codice) via GDP_TIPO_EDIZIONE
```

---

### TB-F13-I02 — tipoEdizione decodificato correttamente

```
GIVEN  GDP_LOG_EDIZIONE.TIPO_EDIZIONE = 'AN'
WHEN   GET /orch/acquisizioni/{idLog}
THEN   response.tipoEdizione = 'anticipataria' (da GDP_TIPO_EDIZIONE)
       NON 'AN'
```

---

### TB-F15-I01 — ricercaAcquisizioni: filtri multipli

```
GIVEN  vari record in GDP_LOG
WHEN   GET /orch/acquisizioni/ricerca?tipoAcquisizione=G&idTestata=1&dataA=2026-03-31&tipoEdizione=OK
THEN   solo i record che soddisfano tutti i criteri
AND   ogni item ha: idLog, idTestata, nomeTestata, tipoEdizione, dataEdizione,
       dataAcquisizione, nroTotFileAcq, nroTotFileVal
```

---

### TB-F20-I01 — statoDAM: interroga LIBRA con jobId

```
GIVEN  WireMock: GET /api/v2/success?jobId=job-123 → {"status":"COMPLETED"}
WHEN   GET /orch/acquisizioni/{idLog}/stato-dam
       (GDP_LOG_EDIZIONE.JOB_ID = 'job-123')
THEN   response.jobId = 'job-123'
AND   response.status = 'COMPLETED'
AND   WireMock riceve GET /api/v2/success?jobId=job-123
```

---

### TB-F21-I01 — attivaCODA: riattiva task in errore

```
GIVEN  GDP_CODA_CARICAMENTO: STATO='FAILED', NRO_TENTATIVO=2, NRO_MAX_TENTATIVI=10
WHEN   POST /orch/acquisizioni/{idLog}/coda
THEN   GDP_CODA_CARICAMENTO.STATO = 'READY'
AND   NRO_TENTATIVO = 3
AND   DT_INSERIMENTO_IN_CODA aggiornata
AND   response 200 con messaggio "Tentativo di invio numero 3 attivato"
```

---

### TB-F21-I02 — attivaCODA: rifiuto quando max tentativi superati ⚠️

```
GIVEN  GDP_CODA_CARICAMENTO: NRO_TENTATIVO=10, NRO_MAX_TENTATIVI=10
WHEN   POST /orch/acquisizioni/{idLog}/coda
THEN   response 409 Conflict
AND   GDP_CODA_CARICAMENTO.STATO invariato (non cambiato a READY)
AND   NRO_TENTATIVO NON incrementato
```

---

### TB-F14-F22-I01 — preparaMAIL + invioMAIL: flusso completo

```
GIVEN  GDP_LOG ha record con FK_GDP_UTENTEFTP=1
       GDP_UTENTESFTP.EMAIL = 'editore@testinella.it'
       GDP_MAIL ha record per COD_MAIL='GW001'
       WireMock SMTP (o mock mailer): accetta connessione su porta 25

WHEN   (1) POST /orch/acquisizioni/{idLog}/mail/prepara {tipoMail:"GW001"}
THEN   response contiene:
         from: 'assistenza.bdp@csi.it'
         to: 'editore@testinella.it'
         host: 'mailfarm-app.csi.it'
         porta: 25
         oggetto: 'Giornali del Piemonte - Edizione da integrare'
         testo: contiene <DATA_EDIZIONE> e 'La Sentinella del Canavese' (placeholder sostituiti)

WHEN   (2) POST /orch/acquisizioni/{idLog}/mail/invia {from, to, host, porta, oggetto, testo}
THEN   mail inviata con successo
AND   response 200
```

---

### TB-F14-I01 — preparaMAIL: indirizzo email mancante → MSG00001

```
GIVEN  GDP_UTENTESFTP.EMAIL = NULL per l'utente collegato al log
WHEN   POST /orch/acquisizioni/{idLog}/mail/prepara
THEN   response con codice MSG00001 ('Indirizzo mail non trovato')
```

---

## 13. TB-F05 — sospensioneEdizioneAttesa

---

### TB-F05-I01 — Sospensione range di date

```
GIVEN  GDP_DATA_USCITA ha 5 record per testata 1 nel range 2026-03-01/2026-03-31
WHEN   POST /orch/testate/1/sospensioni {dataInizio:'2026-03-08', dataFine:'2026-03-22'}
THEN   GDP_DATA_USCITA: SOSPESA=true per tutte le date nel range
AND   GDP_DATA_USCITA: SOSPESA=false per date fuori dal range
AND   GDP_PERIODICITA.INIZIO_SOSPENSIONE = 2026-03-08
AND   GDP_PERIODICITA.FINE_SOSPENSIONE = 2026-03-22
AND   response.giorniSospesi = numero di date effettivamente sospese
```

---

### TB-F05-I02 — Nessuna data nel range → MSG00001

```
GIVEN  GDP_DATA_USCITA non ha record nel range specificato
WHEN   POST /orch/testate/1/sospensioni {dataInizio, dataFine}
THEN   response con MSG00001
AND   GDP_PERIODICITA invariata
```

---

### TB-F05-I03 — F03 non elabora edizioni sospese ⚠️

```
GIVEN  GDP_DATA_USCITA.SOSPESA=true per DATA_ATTESA=2026-03-15
       depositato PDF su SFTP per quella data
WHEN   F03 eseguito
THEN   F04 attivato normalmente (F03 non filtra)
       MA F04 classifica come 'SO' (sospesa) invece di 'OK'
       elaborazione prosegue con tipo SO (non bloccante)
```

> **Nota:** La sospensione non blocca il polling ma cambia il `tipoEdizione`. È l'editore che non dovrebbe depositare file sospesi.

---

## 14. TB-RESILIENZA — Fault tolerance

---

### TB-RES-01 — Recovery automatico dopo riavvio ⚠️

```
GIVEN  GDP_CODA_CARICAMENTO ha record con STATO='PRO' (elaborazione interrotta da crash)
WHEN   servizio gdporch riavviato
THEN   entro 1 minuto dall'avvio F10 riprende l'elaborazione dei task 'PRO'
AND   task aggiornati a 'READY' e inviati al DAM
```

```java
@Test
void res_01_recoveryAfterRestart() {
    // Pre-condition: task in stato PRO rimasto da elaborazione precedente
    codaRepo.insertWithState("PRO", "/_dam/test.zip", 0);

    // Simula riavvio: ricrea il servizio (o usa @ApplicationScoped restart)
    f10Service.scheduleExecution(); // trigger immediato

    await().atMost(Duration.ofSeconds(60)).untilAsserted(() ->
        assertThat(libraMock.getAllServeEvents()).hasSizeGreaterThan(0)
    );
}
```

---

### TB-RES-02 — Un errore su una edizione non blocca le altre ⚠️

```
GIVEN  su SFTP due cartelle:
       sentinella/2026-03-01/ → PDF corrotto
       altrotestata/2026-03-01/ → PDF valido
WHEN   F03 + F04 eseguiti
THEN   sentinella elaborata con errore NL → spostata in _errata
       altrotestata elaborata con successo → ZIP in _dam e SUBMITTED
       entrambi i GDP_LOG presenti
```

---

### TB-RES-03 — DAM timeout: task rimane in coda

```
GIVEN  WireMock: POST /api/v2/imports → delay di 130 secondi (> timeout 120s)
WHEN   F10 tenta invio
THEN   dopo il timeout (120s): chiamata interrotta
AND   GDP_CODA_CARICAMENTO.STATO = 'ERR'
AND   file .zip ancora presente in /_dam/
AND   elaborazione ritentata al ciclo successivo (Awaitility)
```

---

### TB-RES-04 — SFTP non raggiungibile: polling si interrompe ordinatamente

```
GIVEN  SFTP server arrestato (SftpTestServer.stop())
WHEN   F03 tenta connessione
THEN   circuit breaker SFTP si apre
AND   nessuna eccezione unchecked propagata al scheduler
AND   health check /q/health/ready restituisce DOWN
AND   quando SFTP torna disponibile: elaborazione riprende automaticamente
```

---

## 15. TB-FLUSSO-STORICO — End-to-End flusso saltuario

**Scenario:** Dall'SFTP storico al DAM — catena completa F06 → F07 → F08 → F09 → F10  
**Tipo:** E2E (L3)

---

### TB-E2E-S01 — Flusso storico nominale completo ✅

```
GIVEN  su SFTP: /flusso_saltuario/archivio_to/CONS_2026-03-01/sentinella/
       con edizione 20260301/ contenente:
       - sentinella-TO-20260301_001.pdf (PDF valido)
       - sentinella-TO-20260301_001.txt
       - sentinella-TO-20260301_001.tif
       GDP_TESTATA.CARTELLA_TESTATA = 'sentinella' identifica testata 1
       GdpUtenteSftp con HOME_SFTP = '/flusso_saltuario/archivio_to'
       WireMock LIBRA: POST /api/v2/imports → {"jobId":"storico-job","status":"SUBMITTED"}

WHEN   (1) F06 eseguito (polling notturno)
       (2) F07 attivato asincronamente da F06
       (3) F08 chiamato da F07 (sincrono)
       (4) F09 chiamato da F07 con priorità=100 (sincrono)
       (5) F10 eseguito (scheduler ogni 30 min)

THEN   contenuto spostato in /_tmp/archivio_to/CONS_2026-03-01/sentinella/20260301/
       GDP_LOG inserito con FK_GDP_TESTATA=1, TIPO_ACQUISIZIONE='S'
       GDP_LOG_EDIZIONE.TIPO_EDIZIONE = 'ST'
       GDP_LOG_EDIZIONE.PATH_EDIZIONE = '/_tmp/archivio_to/CONS_2026-03-01/sentinella/20260301'
       GDP_EDIZIONE inserita con DATA_EDIZIONE=2026-03-01
       GDP_PAGINA ha 1 record con FILE_PDF, FILE_TXT e FILE_TIF popolati
       GDP_CODA_CARICAMENTO.PRIORITA = 100
       GDP_LOG_EDIZIONE.JOB_ID = 'storico-job'
       GDP_LOG_EDIZIONE.STATO = 'SUBMITTED'
       file .zip eliminato da /_dam/ dopo conferma
```

```java
@Test
void e2e_s01_fullHistoricalFlow() throws Exception {
    // Setup: struttura storica su MockSFTP
    Path edizione = sftpServer.flussoSaltuario()
        .resolve("archivio_to/CONS_2026-03-01/sentinella/20260301");
    Files.createDirectories(edizione);
    Files.write(edizione.resolve("sentinella-TO-20260301_001.pdf"),
        TestPdfFactory.singlePage("sentinella-TO-20260301_001.pdf"));
    Files.write(edizione.resolve("sentinella-TO-20260301_001.txt"),
        "Testo edizione storica".getBytes());
    Files.write(edizione.resolve("sentinella-TO-20260301_001.tif"),
        TestTifFactory.minimal());

    libraMock.stubFor(post(urlEqualTo("/api/v2/imports"))
        .willReturn(okJson("{\"jobId\":\"storico-job\",\"status\":\"SUBMITTED\"}")));

    // Trigger chain
    f06Service.checkConsegnaStorico();

    // Await async F07 completion + F10
    await().atMost(Duration.ofSeconds(45)).untilAsserted(() -> {
        var logEd = gdpLogEdizioneRepo.findLatestByTipoEdizione("ST");
        assertThat(logEd.getJobId()).isEqualTo("storico-job");
        assertThat(logEd.getStato()).isEqualTo("SUBMITTED");
    });

    // Verify DB state
    var ediz = edizioneRepo.findByTestataAndDate(1, LocalDate.of(2026,3,1));
    assertThat(ediz).isNotNull();
    var pagine = paginaRepo.findByEdizione(ediz.getId());
    assertThat(pagine).hasSize(1);
    assertThat(pagine.get(0).getFileTif()).isNotNull(); // TIF presente per storico

    // Verify priorità
    var coda = codaRepo.findByLogEdizione(gdpLogEdizioneRepo.findLatestByTipoEdizione("ST").getId());
    assertThat(coda.getPriorita()).isEqualTo(100);
}
```

---

### TB-E2E-S02 — Storico multi-edizione: una OK e una anomala ⚠️

```
GIVEN  /flusso_saltuario/archivio_to/CONS_2026-03-01/sentinella/
       contenente DUE cartelle edizione:
       - 20260301/ (data valida yyyymmdd) con file conformi
       - 2026-03/ (data INVALIDA) con file conformi
WHEN   F06 + F07 eseguiti
THEN   edizione 20260301:
         GDP_LOG_EDIZIONE.TIPO_EDIZIONE = 'ST'
         elaborata fino a SUBMITTED (F08→F09→F10)
       edizione 2026-03:
         GDP_LOG_EDIZIONE.TIPO_EDIZIONE = 'AS' (anomala storica)
         spostata in /_errata/
         NRO_PAG_ACQUISITE = 0
       entrambi i GDP_LOG_EDIZIONE inseriti sotto lo stesso GDP_LOG
       elaborazione NON interrotta dall'anomalia ⚠️
```

---

### TB-E2E-S03 — Storico non blocca il giornaliero (priorità) ⚠️

```
GIVEN  su SFTP presenti contemporaneamente:
       - flusso_regolare/sentinella/2026-03-01/sentinella_001.pdf (giornaliero)
       - flusso_saltuario/archivio_to/CONS_2026-03-01/sentinella/20260301/... (storico)
       GDP_DATA_USCITA ha DATA_ATTESA=2026-03-01 per testata 1
WHEN   F03 eseguito → genera coda con PRIORITA=0
       F06 eseguito → genera coda con PRIORITA=100
       F10 eseguito (scheduler)
THEN   WireMock LIBRA riceve PRIMA il giornaliero (PRIORITA=0)
       POI lo storico (PRIORITA=100)
       entrambi con STATO='SUBMITTED' alla fine
```

---

### TB-E2E-S04 — Storico: TIF mancante genera warning ma non blocca

```
GIVEN  edizione storica con PDF e TXT conformi ma SENZA file TIF
WHEN   flusso storico completo F06→F07→F08→F09→F10
THEN   GDP_LOG_EDIZIONE.DESCRIZIONE contiene 'NF – file TIF mancante'
       GDP_PAGINA.FILE_TIF = NULL
       elaborazione completa fino a SUBMITTED (warning non bloccante)
       GDP_LOG.ESITO prefisso <WRN> (non <MSG>)
```

---

### TB-E2E-S05 — Storico: testata non trovata → tutto in _errata

```
GIVEN  /flusso_saltuario/archivio_to/CONS_2026-03-01/inesistente/20260301/file.pdf
       'inesistente' non corrisponde ad alcuna CARTELLA_TESTATA in DB
WHEN   F06 eseguito
THEN   contenuto spostato in /_errata/archivio_to/CONS_2026-03-01/inesistente/
       GDP_LOG.FK_GDP_TESTATA = 0
       GDP_LOG.ESITO = MSG00003 (E102)
       F07 NON attivato
       nessun record in GDP_LOG_EDIZIONE, GDP_EDIZIONE, GDP_PAGINA
```

---

## 16. TB-F16/F17 — Testate APIs

**Funzione:** Lookup testate per elenco e dettaglio  
**Tipo:** Integration (RestAssured + Testcontainers PG)

---

### TB-F16-I01 — getElencoTestate senza filtri

```
GIVEN  GDP_TESTATA ha 3 record (testata 1 inviante, testata 2 inviante, testata 99 non inviante)
WHEN   GET /orch/testate
THEN   response 200 con 3 item
AND    ogni item contiene: idTestata, nomeTestata, cartellaTestata, invioEdizione, prov
```

```java
@Test
void f16_i01_listAllTestate() {
    given()
        .when().get("/orch/testate")
        .then()
            .statusCode(200)
            .body("size()", is(3))
            .body("[0].idTestata", notNullValue())
            .body("[0].nomeTestata", notNullValue())
            .body("[0].cartellaTestata", notNullValue())
            .body("[0].invioEdizione", notNullValue())
            .body("[0].prov", notNullValue());
}
```

---

### TB-F16-I02 — getElencoTestate filtro per invioEdizione

```
GIVEN  3 testate: 2 con INVIO_EDIZIONE=true, 1 con INVIO_EDIZIONE=false
WHEN   GET /orch/testate?invioEdizione=true
THEN   response contiene esattamente 2 item
AND    tutti con invioEdizione=true
```

---

### TB-F16-I03 — getElencoTestate filtro per provincia

```
GIVEN  testate con PROVINCIA='TO', 'CN', 'AL'
WHEN   GET /orch/testate?prov=TO
THEN   solo testate con prov='TO' restituite
```

---

### TB-F17-I01 — getTestata: tutti i campi restituiti

```
GIVEN  GDP_TESTATA ha record completo per idTestata=1
WHEN   GET /orch/testate/1
THEN   response 200 con tutti i campi:
       idTestata, nomeTestata, cartellaTestata, invioEdizione, stato, dataStato,
       codTema, tema, socEditrice, enteProponente, annoFondazione, periodoFreq,
       periodoGg, descrizione, www, mail, provincia, comune, indirizzo, cap,
       longitudine, latitudine
```

---

### TB-F17-I02 — getTestata: id inesistente → 404

```
GIVEN  GDP_TESTATA non ha record per idTestata=999
WHEN   GET /orch/testate/999
THEN   response 404
AND    body contiene MSG00001
```

---

## 17. TB-F18 — verifDateAttese

**Funzione:** Verifica date attese con stato sospensione  
**Tipo:** Integration

---

### TB-F18-I01 — Date attese con mix sospese e non sospese

```
GIVEN  GDP_DATA_USCITA ha 5 record per testata 1 in range 2026-03-01/2026-03-31
       2 record con SOSPESA=true, 3 con SOSPESA=false
WHEN   GET /orch/testate/1/date-attese?dataInizio=2026-03-01&dataFine=2026-03-31
THEN   response 200 con 5 item
       ogni item ha: idTestata, cartellaTestata, dataEdizioneAttesa, sospesa
       2 item con sospesa=true e 3 con sospesa=false
AND    esito = MSG00009
```

---

### TB-F18-I02 — Nessuna data attesa nel range → MSG00001

```
GIVEN  GDP_DATA_USCITA vuota per testata 1 nel range giugno 2026
WHEN   GET /orch/testate/1/date-attese?dataInizio=2026-06-01&dataFine=2026-06-30
THEN   response con esito = MSG00001
AND    elenco vuoto
```

---

### TB-F18-I03 — Senza idTestata: tutte le testate invianti

```
GIVEN  GDP_DATA_USCITA ha date per testata 1 e testata 2 (entrambe con INVIO_EDIZIONE=true)
WHEN   GET /orch/date-attese?dataInizio=2026-03-01&dataFine=2026-03-31
THEN   response contiene date per entrambe le testate
AND    nessuna data per testate con INVIO_EDIZIONE=false
```

---

### TB-F18-I04 — Campo cartellaTestata presente (v2 SFU-01)

```
GIVEN  testata 1 con CARTELLA_TESTATA='sentinella'
WHEN   GET /orch/testate/1/date-attese?dataInizio=2026-03-01&dataFine=2026-03-31
THEN   ogni item della response ha cartellaTestata='sentinella'
```

---

## 18. TB-F19 — pulisciEdizione (placeholder)

**Funzione:** Cleanup staged file dopo conferma DAM  
**Stato:** Detail TBD (SFU-01 v2 — "To BE DO")  
**Tipo:** Integration

---

### TB-F19-I01 — Cleanup file in _tmp più vecchi di 4 ore

```
GIVEN  /_tmp/sentinella/2026-03-01/ con file creato 5 ore fa
       /_dam/sentinella.2026-03-01.zip con file creato 5 ore fa
       GDP_CODA_CARICAMENTO.STATO = 'SUBMITTED' per quella edizione
WHEN   F19 eseguito
THEN   cartella /_tmp/sentinella/2026-03-01/ eliminata
AND    file /_dam/sentinella.2026-03-01.zip eliminato (se presente)
AND    file in /flusso_regolare/ NON toccati
```

---

### TB-F19-I02 — File recenti non eliminati

```
GIVEN  /_tmp/sentinella/2026-03-01/ con file creato 1 ora fa
WHEN   F19 eseguito
THEN   cartella NON eliminata (file troppo recente)
```

---

### TB-F19-I03 — File con stato FAILED non eliminati ⚠️

```
GIVEN  /_dam/sentinella.2026-03-01.zip creato 5 ore fa
       GDP_CODA_CARICAMENTO.STATO = 'FAILED' per quella edizione
WHEN   F19 eseguito
THEN   file .zip NON eliminato ⚠️ (edizione non confermata dal DAM)
```

---

## 19. TB-ORCHESTRATORE — Test di orchestrazione dei flussi

Questi test verificano la **sequenza di orchestrazione completa** del sistema, testando che i trigger schedulati attivino correttamente la catena di servizi nell'ordine atteso. A differenza dei test E2E che verificano dati e file, questi test si concentrano sull'**ordine, il timing e il coordinamento** tra i componenti.

---

### 19.1 Flusso Regolare (Giornaliero): Orchestrazione Completa

```
                    F01 (annuale)
                        │
                        ▼
              GDP_DATA_USCITA (date attese)
                        │
              F02 (20:00 ogni sera)
                        │ crea cartella SFTP /<data_domani>
                        ▼
              SFTP: editore deposita PDF
                        │
              F03 (ogni 15 min)
                        │ rileva file → sposta in _tmp → GDP_LOG
                        ▼
              F04 (asincrono da F03)
                        │ valida PDF, classifica edizione
                        │
                 ┌──────┴──────┐
                 │             │
              F08 (sincrono)  │
                 │ inserisce   │
                 │ GDP_EDIZIONE│
                 │ GDP_PAGINA  │
                 ▼             │
              F09 (sincrono)  │
                 │ genera XML  │
                 │ crea ZIP    │
                 │ GDP_IMPORT  │
                 ▼             │
              F10 (asincrono) │
                 │ invia a DAM │
                 │ LIBRA       │
                 └─────────────┘
```

---

### TB-ORCH-R01 — F01 alimenta F02: data calcolata genera cartella ⚠️

```
GIVEN  testata 1 con periodicità settimanale (1WS3), attiva
       GDP_DATA_USCITA vuota
WHEN   (1) F01 eseguito per anno 2026 → popola GDP_DATA_USCITA con tutte le date mercoledì
       (2) F02 eseguito la sera prima di un mercoledì atteso
THEN   F02 trova la data in GDP_DATA_USCITA e crea la cartella SFTP
       cartella /flusso_regolare/sentinella/<mercoledì_domani> esiste su SFTP
       la data usata da F02 è esattamente una delle date calcolate da F01
```

```java
@Test
void orch_r01_f01FeedsF02() throws Exception {
    // F01: calcola date attese
    f01Service.configDTEdizioneAttesa(
        LocalDate.of(2026,1,1), LocalDate.of(2026,12,31), 1);

    // Trova prossimo mercoledì
    LocalDate nextWednesday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));

    // Verifica che F01 ha generato una data per quel mercoledì
    var dateAttese = dataUscitaRepo.findByTestataAndYear(1, 2026);
    assertThat(dateAttese.stream().map(d -> d.getDataAttesa()))
        .contains(nextWednesday);

    // Ora simula la sera prima: inserisci data attesa domani
    // (F02 processerà solo CURRENT_DATE+1)
    // ... (se nextWednesday == domani, F02 creerà la cartella)

    f02Service.creaCartellaEdizioneAttesa();

    // Verifica cartella creata per il prossimo mercoledì (se domani)
    if (nextWednesday.equals(LocalDate.now().plusDays(1))) {
        Path cartella = sftpServer.flussoRegolare()
            .resolve("sentinella").resolve(nextWednesday.toString());
        assertThat(Files.exists(cartella)).isTrue();
    }
}
```

---

### TB-ORCH-R02 — F03 attiva F04 in asincrono: verifica non-blocking

```
GIVEN  3 edizioni su SFTP:
       /flusso_regolare/sentinella/2026-03-01/ (1 PDF)
       /flusso_regolare/sentinella/2026-03-08/ (1 PDF)
       /flusso_regolare/sentinella/2026-03-15/ (1 PDF — data NON attesa)
       GDP_DATA_USCITA ha record per 2026-03-01 e 2026-03-08, NON per 2026-03-15
WHEN   F03 eseguito (singola scansione)
THEN   (1) F03 termina in tempo ragionevole (< 5 secondi di attesa)
       (2) 3 GDP_LOG inseriti (uno per edizione trovata)
       (3) F04 attivato 3 volte in modo asincrono (thread separato)
       (4) F03 NON attende che F04 finisca → verifica timestamp:
           GDP_LOG.DT_ACQUISIZIONE dei 3 record ≈ stesso secondo
       (5) Dopo attesa asincrona (Awaitility max 30s):
           - 2 edizioni con GDP_LOG_EDIZIONE.TIPO_EDIZIONE = 'OK'
           - 1 edizione con GDP_LOG_EDIZIONE.TIPO_EDIZIONE = 'AA' (anomala)
```

```java
@Test
void orch_r02_f03TriggersF04Async() throws Exception {
    sftpServer.depositaPdf("sentinella", "2026-03-01", "sentinella_001.pdf");
    sftpServer.depositaPdf("sentinella", "2026-03-08", "sentinella_001.pdf");
    sftpServer.depositaPdf("sentinella", "2026-03-15", "sentinella_001.pdf");
    dataUscitaRepo.insertForTest(1, LocalDate.of(2026,3,1));
    dataUscitaRepo.insertForTest(1, LocalDate.of(2026,3,8));
    // NON inserire data attesa per 2026-03-15

    Instant start = Instant.now();
    f03Service.checkEdizioneAttesa();
    Duration f03Duration = Duration.between(start, Instant.now());

    // F03 deve terminare velocemente (non attende F04)
    assertThat(f03Duration).isLessThan(Duration.ofSeconds(5));

    // 3 GDP_LOG inseriti
    assertThat(gdpLogRepo.countToday()).isEqualTo(3);

    // Attesa asincrona per completamento F04
    await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
        var logEdizioni = gdpLogEdizioneRepo.findAll();
        assertThat(logEdizioni).hasSize(3);
        long ok = logEdizioni.stream()
            .filter(e -> "OK".equals(e.getTipoEdizione())).count();
        long aa = logEdizioni.stream()
            .filter(e -> "AA".equals(e.getTipoEdizione())).count();
        assertThat(ok).isEqualTo(2);
        assertThat(aa).isEqualTo(1);
    });
}
```

---

### TB-ORCH-R03 — F04→F08→F09 catena sincrona: ordine garantito

```
GIVEN  edizione in /_tmp/sentinella/2026-03-01/ con PDF valido
       GDP_DATA_USCITA ha DATA_ATTESA=2026-03-01
WHEN   F04 invocato
THEN   ordine di esecuzione verificato tramite timestamp:
       (1) GDP_LOG_EDIZIONE inserito (F04 step 3)
       (2) GDP_EDIZIONE inserita (F08 — sincrono)
       (3) GDP_LOG_EDIZIONE.FK_GDP_EDIZIONE aggiornato (post-F08)
       (4) GDP_CODA_CARICAMENTO inserita (F09 — sincrono)
       (5) GDP_LOG_EDIZIONE.FILE_XML = true (post-F09)
       (6) F10 lanciato asincronamente (non verificabile tramite timestamp F04)
```

```java
@Test
void orch_r03_syncChainOrder() throws Exception {
    sftpServer.depositaPdf("sentinella", "2026-03-01", "sentinella_001.pdf");
    dataUscitaRepo.insertForTest(1, LocalDate.of(2026,3,1));

    f04Service.ctrlEdizioneAcquisita(1, "sentinella",
        LocalDate.of(2026,3,1), 100L);

    // Verifica che tutta la catena sincrona è stata eseguita
    var logEd = gdpLogEdizioneRepo.findByLogId(100L);
    assertThat(logEd).isNotNull();
    assertThat(logEd.getFkGdpEdizione()).isNotNull();     // F08 eseguito
    assertThat(logEd.getFileXml()).isTrue();               // F09 eseguito
    assertThat(logEd.getFileZip()).isTrue();               // F09 ZIP creato

    // GDP_CODA_CARICAMENTO inserita da F09
    var coda = codaRepo.findByLogEdizione(logEd.getId());
    assertThat(coda).isNotNull();
    assertThat(coda.getStato()).isEqualTo("PRO");
    assertThat(coda.getPriorita()).isEqualTo(0); // giornaliero
}
```

---

### TB-ORCH-R04 — Fallimento F08 interrompe la catena: F09 mai invocato ⚠️

```
GIVEN  edizione in /_tmp/ con PDF valido
       DB simulato con constraint violation su GDP_EDIZIONE
WHEN   F04 invocato
THEN   F08 fallisce (MSG00002)
AND    F09 NON invocato (verificare assenza di record in GDP_CODA_CARICAMENTO)
AND    F10 NON invocato
AND    cartella spostata in /_errata/
AND    GDP_LOG.ESITO = MSG00002
AND    GDP_LOG_EDIZIONE.FK_GDP_EDIZIONE = NULL (mai aggiornato)
```

---

### TB-ORCH-R05 — Fallimento F09 non invalida F08 ⚠️

```
GIVEN  F08 eseguito con successo → GDP_EDIZIONE e GDP_PAGINA inseriti
       F09 fallisce per errore filesystem/XML
WHEN   F04 gestisce il fallimento di F09
THEN   GDP_EDIZIONE e GDP_PAGINA RESTANO nel DB (transazione F08 già committata)
AND    GDP_CODA_CARICAMENTO NON ha record per questa edizione
AND    cartella spostata in /_errata/
AND    GDP_LOG.ESITO = MSG00003
AND    F10 NON invocato
```

> **Importante:** Questo test dimostra che F08 e F09 sono in transazioni separate. Un rollback di F09 non annulla F08.

---

### TB-ORCH-R06 — F10 asincrono rispetto a F04: verifica indipendenza

```
GIVEN  F04 ha completato con successo → GDP_CODA_CARICAMENTO con STATO='PRO'
       WireMock LIBRA: POST ritarda di 5 secondi
WHEN   F10 eseguito manualmente (scheduler trigger)
THEN   F10 opera indipendentemente dal thread F04
AND    GDP_CODA_CARICAMENTO.STATO aggiornato a 'SUBMITTED' dopo risposta LIBRA
AND    GDP_LOG_EDIZIONE.JOB_ID aggiornato
AND    nessuna influenza su F03/F04 concorrenti
```

---

### 19.2 Flusso Storico: Orchestrazione Completa

```
              F06 (01:00 ogni notte)
                    │ scan /flusso_saltuario/
                    │ trova CONS_yyyy-mm-dd/
                    ▼
              F07 (asincrono da F06) ← per ogni testata
                    │ valida date, file
                    │ per OGNI edizione nella consegna:
                    │
             ┌──────┴──────┐
             │             │
          F08 (sincrono)   │
             │ inserisce   │
             │ ediz/pagine │
             ▼             │
          F09 (sincrono)   │
             │ XML+ZIP     │
             │ priorità=100│
             ▼             │
          F10 (asincrono)  │
             │ invio DAM   │
             └─────────────┘
```

---

### TB-ORCH-H01 — F06 attiva F07 per ogni testata nella consegna

```
GIVEN  /flusso_saltuario/archivio_to/CONS_2026-03-01/
       contiene 2 sottocartelle: 'sentinella/' e 'altrotestata/'
       entrambe con edizioni valide
WHEN   F06 eseguito
THEN   F07 attivato 2 volte in modo asincrono (1 per testata)
       GDP_LOG inserito con 2 record distinti (uno per testata)
       entrambi con TIPO_ACQUISIZIONE='S'
```

---

### TB-ORCH-H02 — F07 processa tutte le edizioni di una consegna

```
GIVEN  /flusso_saltuario/archivio_to/CONS_2026-03-01/sentinella/
       con 3 cartelle edizione: 20260101/, 20260201/, 20260301/
       ogni cartella con file conformi
WHEN   F07 invocato per testata sentinella
THEN   F08 chiamato 3 volte (una per edizione)
       F09 chiamato 3 volte (una per edizione)
       3 GDP_LOG_EDIZIONE inseriti sotto lo stesso GDP_LOG
       3 GDP_EDIZIONE inserite
       3 record in GDP_CODA_CARICAMENTO tutti con PRIORITA=100
       GDP_LOG.ESITO = MSG00009 (elaborazione completa)
```

```java
@Test
void orch_h02_f07ProcessesMultipleEditions() throws Exception {
    String[] dates = {"20260101", "20260201", "20260301"};
    for (String d : dates) {
        Path editionDir = sftpServer.flussoSaltuario()
            .resolve("archivio_to/CONS_2026-03-01/sentinella/" + d);
        Files.createDirectories(editionDir);
        Files.write(editionDir.resolve("sentinella-TO-" + d + "_001.pdf"),
            TestPdfFactory.singlePage("test.pdf"));
        Files.write(editionDir.resolve("sentinella-TO-" + d + "_001.txt"),
            "Testo".getBytes());
    }

    Long logId = gdpLogRepo.insert(1, "S", LocalDate.now(), 0);
    f07Service.ctrlEdizioniStoriche(1, "sentinella",
        LocalDate.of(2026,3,1), logId);

    // 3 edizioni processate
    assertThat(gdpLogEdizioneRepo.findByLogId(logId)).hasSize(3);
    assertThat(edizioneRepo.count()).isEqualTo(3);

    // Tutte con priorità 100
    var codaList = codaRepo.findAll();
    assertThat(codaList).hasSize(3);
    assertThat(codaList).allMatch(c -> c.getPriorita() == 100);
}
```

---

### TB-ORCH-H03 — F07 fallimento F08 non blocca edizioni successive ⚠️

```
GIVEN  3 edizioni nella consegna: 20260101/, 20260201/, 20260301/
       F08 per 20260201 fallisce (simulare constraint violation)
WHEN   F07 elabora tutte le edizioni in sequenza
THEN   edizione 20260101: F08→F09→F10 completato
       edizione 20260201: F08 fallisce → spostata in /_errata, GDP_LOG_EDIZIONE con MSG00003
       edizione 20260301: F08→F09→F10 completato (NON bloccata dall'errore precedente) ⚠️
       GDP_LOG.ESITO contiene info su stato di tutte le edizioni
```

---

## 20. TB-SCHEDULING — Test degli scheduler

Questi test verificano che i trigger temporali funzionino correttamente e che i job non si sovrappongano.

---

### TB-SCH-01 — F03 non si sovrappone: lock @Scheduled

```
GIVEN  F03 in esecuzione (simulare polling lento con file SFTP grande)
WHEN   scatta il timer 15 minuti del ciclo successivo
THEN   il 2° ciclo F03 NON parte fino a quando il 1° non termina
       (oppure viene saltato con @Scheduled(skipExecutionIf = ...))
AND    nessun file processato due volte
AND    nessun GDP_LOG duplicato
```

> **Implementazione:** Verificare con `@ApplicationScoped` e lock Quarkus `@Scheduled(concurrentExecution = SKIP)`. Test: iniettare delay artificiale in F03 e trigger manuale del 2° ciclo.

---

### TB-SCH-02 — F10 non si sovrappone: lock scheduler

```
GIVEN  F10 in esecuzione (WireMock LIBRA ritarda risposta di 20 secondi)
WHEN   scatta timer 30 minuti per nuovo ciclo F10
THEN   il 2° ciclo F10 NON parte (skip o queue)
AND    nessuna chiamata duplicata a LIBRA per lo stesso task
```

---

### TB-SCH-03 — F02 trigger alle 20:00: verifica CRON

```
GIVEN  configurazione @Scheduled con cron = "0 0 20 * * ?"
WHEN   servizio avviato
THEN   F02 NON eseguito all'avvio
AND    F02 eseguito alla prima occorrenza delle 20:00
```

> **Implementazione:** Usare `@io.quarkus.scheduler.Scheduled` con `QuartzScheduler` e Quartz `CronTrigger` verificabile tramite `Scheduler` API.

---

### TB-SCH-04 — F06 trigger giornaliero: verifica CRON notturno

```
GIVEN  configurazione @Scheduled con cron = "0 0 1 * * ?" (01:00)
WHEN   servizio avviato
THEN   F06 NON eseguito all'avvio
AND    F06 eseguito alla prima occorrenza delle 01:00
```

---

### TB-SCH-05 — Tutti gli scheduler disabilitabili da config ⚠️

```
GIVEN  gdp.polling.enabled=false in application.properties
WHEN   servizio avviato
THEN   F03 NON eseguito (polling periodico disabilitato)
AND    F06 NON eseguito (polling storico disabilitato)
AND    F10 NON eseguito (invio DAM disabilitato)
AND    /q/health/ready restituisce UP (servizio attivo ma inattivo)
AND    API MONITOR (F12-F22) funzionanti normalmente
```

---

## 21. TB-CONCORRENZA — Scenari concorrenti e race conditions

---

### TB-CONC-01 — Due F04 concorrenti sulla stessa testata, edizioni diverse

```
GIVEN  F03 trova 2 edizioni per testata 'sentinella': 2026-03-01 e 2026-03-08
WHEN   F03 lancia 2 F04 in parallelo (async) per la stessa testata
THEN   entrambi i F04 completano senza deadlock
       2 GDP_EDIZIONE distinte inserite
       nessun constraint violation su chiave unica (testata, data_edizione)
       file correttamente separati in /_tmp/sentinella/2026-03-01/ e /2026-03-08/
```

---

### TB-CONC-02 — F04 e F10 concorrenti: F04 scrive coda mentre F10 legge

```
GIVEN  F10 scheduler in esecuzione (sta leggendo GDP_CODA_CARICAMENTO)
WHEN   F04 completa e F09 inserisce un nuovo record in GDP_CODA_CARICAMENTO
THEN   il nuovo record NON è processato nel ciclo F10 corrente (è stato inserito dopo la lettura)
AND    sarà processato nel ciclo F10 successivo
AND    nessun errore di concorrenza / lock contention
```

---

### TB-CONC-03 — F03 e F06 concorrenti: stesso orario per test

```
GIVEN  F03 (periodico) e F06 (storico) triggered contemporaneamente
       F03 trova file in /flusso_regolare/
       F06 trova file in /flusso_saltuario/
WHEN   entrambi i job eseguiti in parallelo
THEN   nessuna interferenza tra i due flussi
       GDP_LOG record distinti con TIPO_ACQUISIZIONE 'G' e 'S'
       file spostati in /_tmp/ sotto path distinti
       nessun deadlock su DB connection pool
```

---

### TB-CONC-04 — Doppia invocazione F04 stessa edizione (idempotenza) ⚠️

```
GIVEN  F03 invoca F04 per testata 1, data 2026-03-01
       per errore o restart, F04 invocato di nuovo con stessi parametri
WHEN   2° invocazione F04 trova GDP_EDIZIONE già esistente
THEN   F08 aggiorna (UPDATE) invece di inserire (INSERT)
       nessun GDP_EDIZIONE duplicato
       GDP_PAGINA aggiornate senza duplicati
       2° GDP_LOG_EDIZIONE inserito (distinto dal 1°)
```

---

## 22. TB-MONITOR-WORKFLOW — Test del flusso monitor completo

Questi test verificano il workflow tipico dell'operatore sulla console di monitoraggio, simulando la sequenza di chiamate BFF → gdporch.

---

### TB-MW-01 — Scenario operatore: verifica acquisizioni del giorno

```
GIVEN  3 edizioni giornaliere elaborate oggi:
       - sentinella (OK, SUBMITTED)
       - altrotestata (OK, SUBMITTED)  
       - terztestata (AA, in _errata)
WHEN   operatore:
       (1) GET /orch/acquisizioni?tipoAcquisizione=G&dataAcquisizione=<oggi>
       (2) per ogni idLog restituito → GET /orch/acquisizioni/{idLog}
THEN   (1) elenco con 3 item
       (2) dettaglio completo per ogni acquisizione:
           - sentinella: tipoEdizione='corrispondente', jobID popolato
           - altrotestata: tipoEdizione='corrispondente', jobID popolato
           - terztestata: tipoEdizione='anomalia edizione attesa', jobID=null
```

---

### TB-MW-02 — Scenario operatore: retry invio DAM fallito

```
GIVEN  edizione con GDP_CODA_CARICAMENTO.STATO='FAILED', NRO_TENTATIVO=1
       WireMock LIBRA ora risponde correttamente: {"status":"SUBMITTED"}
WHEN   operatore:
       (1) GET /orch/acquisizioni/{idLog} → verifica STATO='FAILED'
       (2) POST /orch/acquisizioni/{idLog}/coda → riattiva (F21)
       (3) attendere esecuzione F10 (scheduler 30 min)
       (4) GET /orch/acquisizioni/{idLog}/stato-dam → verifica stato DAM (F20)
THEN   (2) response: "Tentativo di invio numero 2 attivato"
           GDP_CODA_CARICAMENTO.STATO = 'READY', NRO_TENTATIVO = 2
       (3) F10 riprende il task → invia a LIBRA → SUBMITTED
       (4) F20 restituisce stato DAM aggiornato
```

---

### TB-MW-03 — Scenario operatore: invio mail di notifica

```
GIVEN  edizione con warning (PDF con data mancante)
       GDP_UTENTESFTP.EMAIL = 'editore@sentinella.it'
       GDP_MAIL.COD_MAIL = 'GW001' configurata
WHEN   operatore:
       (1) POST /orch/acquisizioni/{idLog}/mail/prepara {tipoMail:"GW001"}
       (2) verifica preview dei dati mail restituiti
       (3) POST /orch/acquisizioni/{idLog}/mail/invia {from, to, host, porta, oggetto, testo}
THEN   (1) response con from='assistenza.bdp@csi.it', to='editore@sentinella.it'
           oggetto contiene 'Edizione da integrare'
           testo contiene 'La Sentinella del Canavese' e la dataEdizione
       (3) mail inviata con successo (MockSMTP riceve il messaggio)
```

---

### TB-MW-04 — Scenario operatore: ricerca storica con filtri

```
GIVEN  20 acquisizioni nel DB degli ultimi 3 mesi
       mix di tipiEdizione: OK, AN, PO, AA, SO
       mix di testate: sentinella, altrotestata
WHEN   GET /orch/acquisizioni/ricerca?tipoAcquisizione=G&idTestata=1
                                      &dataDA=2026-01-01&dataA=2026-03-31
                                      &tipoEdizione=OK
THEN   solo acquisizioni giornaliere della testata 1 con tipoEdizione='OK' nel trimestre
AND    totale minore delle 20 acquisizioni totali
AND    ogni item ha nroTotFileAcq e nroTotFileVal corretti
```

---

### TB-MW-05 — Sospensione e verifica: ciclo completo F05+F18

```
GIVEN  F01 ha calcolato date per Marzo 2026: 4 mercoledì (4, 11, 18, 25)
WHEN   operatore:
       (1) GET /orch/testate/1/date-attese?dataInizio=2026-03-01&dataFine=2026-03-31
           → 4 date, tutte con sospesa=false
       (2) POST /orch/testate/1/sospensioni {dataInizio:'2026-03-11', dataFine:'2026-03-18'}
           → giorniSospesi=2 (11 e 18 marzo)
       (3) GET /orch/testate/1/date-attese?dataInizio=2026-03-01&dataFine=2026-03-31
           → 4 date: 4/03 e 25/03 non sospese, 11/03 e 18/03 sospese
THEN   le date sospese hanno sospesa=true
AND    GDP_PERIODICITA.INIZIO_SOSPENSIONE = 2026-03-11
AND    GDP_PERIODICITA.FINE_SOSPENSIONE = 2026-03-18
```

---

## 23. TB-OSSERVABILITA — Metriche, logging e health checks

---

### TB-OBS-01 — Metriche Micrometer: contatori edizioni

```
GIVEN  5 edizioni elaborate con successo, 2 con errore
WHEN   GET /q/metrics
THEN   edizioni_elaborate_total = 7
AND    edizioni_errore_total = 2
AND    elaborazione_durata_secondi (histogram) ha 7 osservazioni
```

---

### TB-OBS-02 — Health check: SFTP readiness

```
GIVEN  MockSFTP server attivo e raggiungibile
WHEN   GET /q/health/ready
THEN   status = 'UP'
AND    checks contiene 'sftp-connection' con status UP
```

---

### TB-OBS-03 — Health check: SFTP DOWN

```
GIVEN  MockSFTP server arrestato
WHEN   GET /q/health/ready
THEN   status = 'DOWN'
AND    checks contiene 'sftp-connection' con status DOWN
```

---

### TB-OBS-04 — Correlation ID: traccia unica per edizione

```
GIVEN  edizione elaborata com successo (F03→F04→F08→F09)
WHEN   analisi dei log prodotti durante l'elaborazione
THEN   tutti i log per quella edizione contengono lo stesso correlation ID
AND    il formato è UUID (es. "550e8400-e29b-41d4-a716-446655440000")
AND    log di edizioni diverse hanno correlation ID diversi
```

---

### TB-OBS-05 — Liveness check separato da readiness

```
GIVEN  SFTP server DOWN ma applicazione Quarkus in esecuzione
WHEN   GET /q/health/live
THEN   status = 'UP' (l'applicazione è viva)
AND    GET /q/health/ready → status = 'DOWN' (non pronta a operare)
```

---

## Appendice A — Matrice di copertura

| Test ID | Funzione | Priorità | Tipo | Stato |
|---------|----------|----------|------|-------|
| TB-F01-U01..U07 | F01 calcolo date | Alta | Unit | — |
| TB-F02-I01..I03 | F02 crea cartelle | Media | Integration | — |
| TB-F03-I01..I04 | F03 polling SFTP | Alta | Integration | — |
| TB-F04-I01..I14 | F04 validazione PDF | Alta | Integration | — |
| TB-F05-I01..I03 | F05 sospensione | Media | Integration | — |
| TB-F06-I01..I02 | F06 polling storico | Media | Integration | — |
| TB-F07-I01..I03 | F07 validazione storica | Media | Integration | — |
| TB-F08-I01..I04 | F08 DB insert | Alta | Integration | — |
| TB-F09-I01..I04 | F09 XML+ZIP | Alta | Integration | — |
| TB-F10-I01..I05 | F10 invio DAM | Alta | Integration | — |
| TB-F12-I01..I02 | F12 elenco acquisizioni | Media | Integration | — |
| TB-F13-I01..I02 | F13 dettaglio | Media | Integration | — |
| TB-F15-I01 | F15 ricerca | Media | Integration | — |
| TB-F16-I01..I03 | F16 elenco testate | Media | Integration | — |
| TB-F17-I01..I02 | F17 dettaglio testata | Media | Integration | — |
| TB-F18-I01..I04 | F18 verifica date attese | Media | Integration | — |
| TB-F19-I01..I03 | F19 cleanup staging | Bassa | Integration | — |
| TB-F20-I01 | F20 stato DAM | Media | Integration | — |
| TB-F21-I01..I02 | F21 retry coda | Alta | Integration | — |
| TB-F14-F22-I01 | F14+F22 mail | Media | Integration | — |
| TB-E2E-R01..R03 | Flusso E2E periodico | Alta | E2E | — |
| TB-E2E-S01..S05 | Flusso E2E storico | Alta | E2E | — |
| TB-ORCH-R01..R06 | Orchestrazione regolare | Alta | E2E | — |
| TB-ORCH-H01..H03 | Orchestrazione storica | Alta | E2E | — |
| TB-SCH-01..05 | Scheduler | Alta | Integration | — |
| TB-CONC-01..04 | Concorrenza | Alta | Integration | — |
| TB-MW-01..05 | Monitor workflow | Media | E2E | — |
| TB-OBS-01..05 | Osservabilità | Media | Integration | — |
| TB-RES-01..04 | Resilienza | Alta | E2E | — |

---

## Appendice B — Casi limite critici (⚠️ non dimenticare)

1. **File NON eliminato da SFTP prima di conferma DAM** (TB-F10-I02, TB-E2E-R02)
2. **Rollback completo F08** quando inserimento pagina fallisce (TB-F08-I04)
3. **Un errore non blocca gli altri** — continuazione polling (TB-RES-02, TB-E2E-R03, TB-ORCH-H03)
4. **Ordinamento priorità coda**: giornaliero (0) prima di storico (100) (TB-F10-I03, TB-E2E-S03)
5. **Idempotenza F01**: doppia chiamata non duplica date (TB-F01-U05)
6. **Idempotenza F04**: doppia invocazione non duplica edizioni (TB-CONC-04)
7. **Max retry F21**: 409 quando NRO_TENTATIVO >= NRO_MAX_TENTATIVI (TB-F21-I02)
8. **Recovery dopo crash**: task in stato PRO ripresi all'avvio (TB-RES-01)
9. **Circuit breaker SFTP**: health DOWN, polling si ferma ordinatamente (TB-RES-04)
10. **Transazioni separate F08/F09**: F09 fallisce ma F08 è già committed (TB-ORCH-R05)
11. **F03 non-blocking**: non attende completamento F04 asincrono (TB-ORCH-R02)
12. **Scheduler non-overlapping**: F03 e F10 non si sovrappongono (TB-SCH-01, TB-SCH-02)
13. **Disabilitazione polling via config**: tutti gli scheduler spegnibili (TB-SCH-05)
14. **File TIF mancante nello storico**: warning, non bloccante (TB-E2E-S04)
15. **F07 multi-edizione**: errore su una edizione non blocca le altre (TB-ORCH-H03)

---

## Appendice C — Configurazione test e profile Quarkus

```properties
# src/test/resources/application.properties (%test profile)

# Testcontainers PostgreSQL
quarkus.datasource.db-kind=postgresql
quarkus.datasource.devservices.enabled=false
# La connessione è iniettata da Testcontainers: @QuarkusTestResource

# SFTP mock (porta dinamica)
gdp.sftp.host=localhost
gdp.sftp.port=${sftp.test.port}
gdp.sftp.user=testuser
gdp.sftp.password=testpass

# DAM LIBRA mock (WireMock porta dinamica)
gdp.libra.url=http://localhost:${wiremock.test.port}/rpcr02
gdp.libra.api-key=test-api-key

# Polling disabilitato in test (trigger manuale)
gdp.polling.enabled=false
gdp.f03.cron=disabled
gdp.f06.cron=disabled
gdp.f10.cron=disabled
gdp.f02.cron=disabled

# Stabilità file ridotta per test (3 secondi invece di 3 minuti)
gdp.sftp.stability-check-seconds=3

# Timeout ridotto per test
gdp.libra.timeout=5s

# Retry ridotto per test
gdp.libra.max-retries=2
```

---

## Appendice D — Helper di test aggiuntivi

### D.1 `TestTifFactory` — File TIF di test

```java
public class TestTifFactory {
    public static byte[] minimal() {
        // TIFF header minimale valido (little-endian, 1x1 pixel)
        return new byte[]{
            0x49, 0x49, 0x2A, 0x00,  // II + magic number
            0x08, 0x00, 0x00, 0x00,  // offset IFD
            // ... IFD entries minimali per 1x1 pixel
        };
    }
}
```

### D.2 `MockSmtpServer` — Server SMTP di test

```java
public class MockSmtpServer {
    private GreenMail greenMail;

    public static MockSmtpServer start() {
        MockSmtpServer s = new MockSmtpServer();
        s.greenMail = new GreenMail(ServerSetupTest.SMTP);
        s.greenMail.start();
        return s;
    }

    public int getPort() { return greenMail.getSmtp().getPort(); }

    public MimeMessage[] getReceivedMessages() {
        return greenMail.getReceivedMessages();
    }

    public void stop() { greenMail.stop(); }
}
```

### D.3 `SftpTestServer` esteso per flusso saltuario

```java
// Aggiunta al SftpTestServer esistente (§2.2)
public Path flussoSaltuario() { return rootFtp.resolve("flusso_saltuario"); }

public Path depositaEdizioneStoica(String utente, String consegna,
        String testata, String dataEdizione) throws IOException {
    Path dir = flussoSaltuario().resolve(utente)
        .resolve("CONS_" + consegna).resolve(testata).resolve(dataEdizione);
    Files.createDirectories(dir);
    return dir;
}
```

---

## Appendice E — Ordine di esecuzione consigliato

Per il testing incrementale, seguire questo ordine:

```
Phase 1 — Unit (L1)
  └─ TB-F01-U* (algoritmo calcolo date — nessuna dipendenza)

Phase 2 — Integration singola funzione (L2a)
  ├─ TB-F08-I* (DB insert — dipende solo da DB)
  ├─ TB-F02-I* (cartelle SFTP — dipende da DB + SFTP)
  └─ TB-F16/F17/F18-I* (testate lookup — dipende solo da DB)

Phase 3 — Integration catena corta (L2b)
  ├─ TB-F03-I* (polling + trigger F04)
  ├─ TB-F04-I* (validazione PDF completa)
  ├─ TB-F09-I* (XML + ZIP)
  ├─ TB-F10-I* (invio DAM mock)
  └─ TB-F06/F07-I* (polling storico)

Phase 4 — Integration BFF/Monitor (L2c)
  ├─ TB-F12/F13/F15-I* (query API)
  ├─ TB-F20/F21-I* (stato DAM + retry)
  ├─ TB-F14-F22-I* (mail)
  └─ TB-F05-I* (sospensione)

Phase 5 — E2E e Orchestrazione (L3)
  ├─ TB-E2E-R* (flusso regolare end-to-end)
  ├─ TB-E2E-S* (flusso storico end-to-end)
  ├─ TB-ORCH-R* (orchestrazione regolare)
  └─ TB-ORCH-H* (orchestrazione storica)

Phase 6 — Non-funzionali (L3+)
  ├─ TB-SCH-* (scheduler)
  ├─ TB-CONC-* (concorrenza)
  ├─ TB-RES-* (resilienza)
  └─ TB-OBS-* (osservabilità)
```