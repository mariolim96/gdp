# GDP — Giornali del Piemonte
## Chiarimenti e segnalazioni sulla Specifica dei Servizi BackEnd (SFU-01 V03)

Data: 10/04/2026

Di seguito riportiamo le domande e le segnalazioni emerse durante l'analisi della specifica dei servizi e dello schema del database, raggruppate per area tematica.

---

### 1 — Modalità di richiamo del servizio FTPregolare
**Riferimento:** Servizio FTPregolare — Modalità di richiamo

La specifica indica che il servizio è richiamato in modalità "asincrona" dal **BE applicativo**. Si chiede di chiarire cosa si intenda per "BE applicativo": è il BFF, un comando CLI, uno scheduler esterno o altro?

---

### 2 — Ambiguità nell'uso di `idLog` nei flussi F08 e F09 (flusso storico)
**Riferimento:** Operazioni F07 / F08 / F09

Esiste una relazione uno-a-molti tra `GDP_LOG` e `GDP_LOG_EDIZIONE`, in particolar modo nel flusso storico F06/F07, in cui il log di una singola consegna può racchiudere svariate edizioni. L'utilizzo del solo parametro `idLog` per recuperare il record dell'edizione genera ambiguità: identificando solo la sessione complessiva di trasmissione e non la specifica edizione, il risultato al momento dell'estrazione e del salvataggio dati comporterebbe l'aggiornamento parziale, errato o continuo del primo record disponibile legato a quell'idLog.

**Proposte:**
1. **Passare `idLogEdizione` come parametro (Raccomandata):** Modificare le signature di F08 e F09 per accettare direttamente la Primary Key `ID_GDP_LOG_EDIZIONE`, chiedendo agli orchestratori chiamanti (F04/F07) di restituirla dopo la persistenza del record.
2. **Ricerca univoca tramite Path (Alternativa):** Usare la combinazione di `FK_GDP_LOG` + `PATH_EDIZIONE` (introdotto nella V02) come chiave univoca, formalizzando anche una constraint di unicità a database per tutela da duplicati.

---

### 3 — Discrepanza sui parametri di F10 e sulle modalità di esecuzione
**Riferimento:** Operazioni F04, F07 e F10

Nelle specifiche operative di F04 e F07 viene indicato di richiamare in modo asincrono l'operazione F10 passando come parametri `idLog`, `idEdizione` e `nomeFileZIP`. Tuttavia, la definizione dell'interfaccia di F10 prevede come parametri di input esclusivi solo `idLog` e `nomeFileEdizione` (omettendo `idEdizione`).

Inoltre, all'interno dei passaggi di F10, il processo viene descritto come uno scheduler/worker batch che interroga la tabella `GDP_CODA_CARICAMENTO` filtrando tutti i record con STATO = "READY", ignorando completamente i parametri di input.

**Richiesta:** Chiarire se F10 debba funzionare come consumer batch di una coda schedulata (e quindi i passi di F04/F07 che gli passano parametri vanno rimossi), oppure come chiamata puntuale in base agli ID ricevuti (nel qual caso l'interfaccia deve includere `idEdizione` o meglio `idLogEdizione`).

---

### 4 — Disallineamento sugli stati in GDP_CODA_CARICAMENTO
**Riferimento:** Operazioni F09 e F10

Al termine dell'operazione F09, viene inserito un record in `GDP_IMPORT_TASK` / `GDP_CODA_CARICAMENTO` con STATO = `'PRO'`.
Tuttavia, il task schedulato F10 processa solo i record con STATO = `'READY'`.

**Richiesta:** Chiarire se `"PRO"` è l'abbreviazione di "PRONTO" e va normalizzato in `'READY'`, oppure se F09 dovrebbe inserire direttamente con stato `'READY'`, oppure ancora se F10 debba cercare anche i record in stato `'PRO'`.

---

### 5 — Ambiguità `idLog` anche in F21 (attivaCODA) e F14 (preparaMAIL)
**Riferimento:** Operazioni F21 e F14

Analogamente alla domanda 2, l'operazione F21 permette la rimessa in coda di una trasmissione fallita partendo da `idLog`. Nel caso del flusso storico, un singolo `idLog` è legato a decine di edizioni (ognuna con il suo `GDP_LOG_EDIZIONE`). Usando l'idLog parentale, F21 riattiverebbe ciecamente tutte le code associate, perdendo granularità.

Lo stesso vale per F14 che usa `idLog` per recuperare `DATA_EDIZIONE` (corrispondenza 1-a-N nel caso storico) da inserire nel template della mail.

**Richiesta:** Modificare le interfacce di F21 e F14 affinché richiedano o preferiscano `idLogEdizione` al posto di `idLog` per garantire selezione univoca dell'anomalia desiderata.

---

### 6 — Contraddizione sul campo ANNO in GDP_DATA_USCITA (F04)
**Riferimento:** Operazione F04 — Classificazione edizione

Nella tabella `GDP_DATA_USCITA` dello schema DB reale sono presenti i campi `DT_INIZIO` e `DT_FINE` (range di validità), ma **non** il campo `ANNO`. Tuttavia, nei passi operativi di F04, il controllo prevede esplicitamente il filtraggio `ANNO = current_year`.

**Richiesta:** Aggiornare la query documentata per F04 in modo che si basi sul filtraggio tramite `DATA_ATTESA` eventualmente contestualizzato con il range `DT_INIZIO`/`DT_FINE`, in sostituzione del campo `ANNO` che non esiste nello schema.

---

### 7 — Campo STATO mancante nello schema DB per GDP_IMPORT_TASK
**Riferimento:** Operazioni F09, F10 e F21

Nelle specifiche operative si fa reiterato riferimento all'impostazione e lettura del campo `STATO` sulla tabella `GDP_IMPORT_TASK` (o `GDP_CODA_CARICAMENTO`) con valori `'PRO'`, `'READY'`, `'SUBMITTED'`, `'FAILED'`. Tuttavia, analizzando lo schema DB fornito (DDL), questo campo **non risulta presente** nella definizione della tabella.

**Richiesta:** Confermare ufficialmente l'aggiunta del campo `STATO` allo schema del database (modificando il DDL) per poter gestire correttamente il ciclo di vita della coda di caricamenti verso il DAM.

---

### 8 — Assenza di Sequence/auto-increment per le Primary Key nel DDL
**Riferimento:** Configurazione DB / DDL Iniziale

Nello schema di database fornito manca completamente l'implementazione delle Sequence necessarie per generare in modo progressivo e automatico gli identificativi di Primary Key (es. `ID_GDP_LOG`, `ID_GDP_EDIZIONE`, `ID_GDP_PAGINA`, ecc.).

Abbiamo provveduto temporaneamente a creare script personalizzati nel nostro ambiente per definire le sequenze (es. `CREATE SEQUENCE IF NOT EXISTS seq_gdp_edizione START WITH 10;`) e a sincronizzarle con il pre-caricamento base tramite `setval`.

**Richiesta:** Ufficializzare l'aggiunta delle sequenze nel DDL nativo, oppure formalizzare un pattern supportato per la generazione degli ID.

---

### 9 — Discrepanza sui codici tipoEdizione in F04
**Riferimento:** Operazioni F04 e F15

Il flusso F04 usa i codici `"AT"` (anticipataria) e `"PT"` (posticipataria) per classificare le edizioni. Tuttavia, la stessa specifica nella sezione F15 (ricercaAcquisizioni) usa `"AN"` (anticipataria) e `"PO"` (posticipataria) nella lista dei valori ammessi. Anche la tabella DB `GDP_TIPO_EDIZIONE` nel file `init.sql` riporta `"AN"` e `"PO"`.

**Richiesta:** Aggiornare la specifica di F04 (e ovunque compaiano `"AT"` e `"PT"`) sostituendoli con `"AN"` e `"PO"` per allinearsi ai dati presenti nello schema DB. Oppure confermare esplicitamente il mapping: AT→AN, PT→PO.

---

### 10 — Tipo errato del parametro `IDEdizione` nell'interfaccia di F09
**Riferimento:** Operazioni F04 e F09

L'interfaccia di richiamo (input) di F09 nella specifica elenca:
- Parametro 3: `IDEdizione` — "Formato yyyy-mm-dd Dt" (tipo data)

Tuttavia, F04 invoca F09 passando `<IDEdizione>` come ID numerico restituito da F08 (FK a `GDP_EDIZIONE`), non una data.

**Richiesta:** Correggere la tabella dei parametri di input di F09: il parametro 3 deve essere `IDEdizione` di tipo `Integer` (FK a `GDP_EDIZIONE`), non una data in formato yyyy-mm-dd.

---

### 11 — F07: Modalità di richiamo autoreferenziale
**Riferimento:** Operazione F07 — Modalità di richiamo

Nella sezione F07, la "Modalità di richiamo" recita:
> "Il servizio è richiamato in modalità asincrona dall'operazione **F07**."

Questo è chiaramente un errore tipografico: F07 non può richiamare sé stesso. Dalla logica del sistema, F07 è richiamato da **F06** (`FTPsaltuario.checkConsegnaStorico`).

**Richiesta:** Correggere indicando "richiamato in modalità asincrona dall'operazione **F06**".

---

### 12 — Nome del campo `INVIO_EDIZIONI` vs `INVIO_EDIZIONE`
**Riferimento:** Operazioni F01 e F18

Le operazioni F01 e F18 filtrano le testate attive usando il vincolo `GDP_TESTATA.INVIO_EDIZIONI = 1` (con la "S" finale, plurale). Tuttavia, nella DDL dello schema DB il campo risulta chiamato `INVIO_EDIZIONE` (singolare, boolean).

**Richiesta:** Chiarire e uniformare il nome del campo: qual è il nome esatto nella DDL del database? Aggiornare tutti i riferimenti nella specifica di conseguenza.

---

### 13 — Campo `ANNO_EDIZIONE` mancante nella specifica di F08 per GDP_PAGINA
**Riferimento:** Operazione F08

La specifica di F08 elenca i campi da inserire nella tabella `GDP_PAGINA`, ma non include il campo `ANNO_EDIZIONE`. Tuttavia, nello schema DB la tabella `GDP_PAGINA` contiene il campo `ANNO_EDIZIONE` (integer).

**Richiesta:** Confermare se `ANNO_EDIZIONE` deve essere popolato al momento dell'insert su `GDP_PAGINA` (con il valore dell'anno estratto da `DATA_EDIZIONE`) e, in caso affermativo, aggiornare la specifica di F08.

---

### 14 — Campo `FILE_TIF = NULL` in F08 anche per il flusso storico
**Riferimento:** Operazione F08 — inserimento GDP_PAGINA

La specifica di F08 (sia in inserimento che in aggiornamento) imposta `FILE_TIF = NULL` per tutti i record su `GDP_PAGINA` senza distinzione tra flusso regolare e storico. Tuttavia, l'operazione F07 (storico) verifica e conta esplicitamente i file TIF, e nel flusso storico ci si aspetta che i TIF esistano e siano associati alle pagine.

**Richiesta:** Chiarire se F08 deve popolare `FILE_TIF` con il nome del file .tif corrispondente quando invocato dal flusso storico F07, oppure se i file TIF hanno un percorso di archiviazione separato e `GDP_PAGINA.FILE_TIF` rimane sempre NULL.

---

### 15 — Campo `idUtenteSFTP` nell'output di F12 senza sorgente DB esplicitata
**Riferimento:** Operazione F12 — elencoAcquisizioni

L'output di F12 elenca il campo `idUtenteSFTP` (campo 2), ma la sezione "Dettaglio passi operazione" non specifica da quale tabella/campo DB esso venga recuperato. La logica descrive solo il join con GDP_TESTATA, GDP_EDIZIONE e GDP_LOG_EDIZIONE.

**Richiesta:** Specificare esplicitamente la sorgente di `idUtenteSFTP`: si presume da `GDP_LOG.FK_GDP_UTENTEFTP` oppure da `GDP_UTENTESFTP` tramite join. Aggiornare il mapping dell'output di F12.

---

### 16 — Aggiornamento GDP_LOG_EDIZIONE tramite `idLog` ambiguo in F09 (percorso di errore)
**Riferimento:** Operazione F09

Nella sezione finale di F09, in caso di errore nella creazione del file XML o ZIP, il sistema imposta rispettivamente `GDP_LOG_EDIZIONE.FILE_XML=False` o `GDP_LOG_EDIZIONE.FILE_ZIP=False` "per l'occorrenza relativa a idLog ricevuto in input". Nel flusso storico un `idLog` è associato a molteplici record `GDP_LOG_EDIZIONE`.

**Richiesta:** Analogamente alla domanda 2, allineare F09 passando `idLogEdizione` (o usando `path + idLog` come chiave composta) anche per la gestione degli errori, in modo che `FILE_XML` e `FILE_ZIP` vengano aggiornati sul record corretto.

---

### 17 — Mapping errato di `NomeTestata` in F12
**Riferimento:** Operazione F12 — elencoAcquisizioni (Dettaglio passi operazione)

Nella sezione "Dettaglio passi operazione" di F12, il mapping indica:
> `NomeTestata = GDP_TESTATA.ID_GDP_TESTATA`

Questo è errato: `ID_GDP_TESTATA` è l'identificativo numerico, non il nome della testata.

**Richiesta:** Correggere il mapping: il campo `NomeTestata` deve essere recuperato da `GDP_TESTATA.NOME_TESTATA`.

---

### 18 — Numerazione duplicata dei campi di output in F13
**Riferimento:** Operazione F13 — dettaglioAcquisizione

La tabella di output di F13 assegna il numero `1.12` sia al campo `fileXML` sia al campo `fileZIP`. La numerazione dovrebbe essere distinta (es. 1.12 per fileXML, 1.13 per fileZIP, con rinumerazione dei campi successivi).

**Richiesta:** Correggere la numerazione dei campi di output di F13.

---

### 19 — Mapping errato di `NomeTestata` in F13
**Riferimento:** Operazione F13 — dettaglioAcquisizione (Dettaglio passi operazione)

Stessa anomalia della domanda 17. In F13 il mapping riporta:
> `NomeTestata = GDP_TESTATA.ID_GDP_TESTATA`

Dovrebbe essere `GDP_TESTATA.NOME_TESTATA`.

**Richiesta:** Correggere il mapping di F13 per il campo `NomeTestata`.

---

### 20 — Campo `TIPO_ACQUISIZIONE` mancante nel GDP_LOG di F06
**Riferimento:** Operazione F06 — checkConsegnaStorico

Nella descrizione operativa di F06, quando viene inserito il record `GDP_LOG` nel caso di match singolo (testata trovata), i campi valorizzati sono solo `FK_TESTATA` e `DT_ACQUISIZIONE`. Il campo `TIPO_ACQUISIZIONE` non viene citato.

Per coerenza col flusso regolare F03 (che imposta esplicitamente `TIPO_ACQUISIZIONE='G'`), si presume che F06 debba impostare `TIPO_ACQUISIZIONE='S'`.

**Richiesta:** Aggiornare la specifica di F06 rendendo esplicito che `GDP_LOG.TIPO_ACQUISIZIONE = 'S'` per il flusso storico.

---

### 21 — F10: aggiornamento `STATO` sulla tabella errata
**Riferimento:** Operazione F10 — inviaEdizione

In F10, in risposta al DAM, viene indicato di aggiornare:
- `GDP_LOG_EDIZIONE.STATO = "FAILED"` o `"SUBMITTED"`

Tuttavia, il campo `STATO` con valori `'FAILED'`/`'SUBMITTED'`/`'READY'` è definito sulla tabella `GDP_IMPORT_TASK` (= `GDP_CODA_CARICAMENTO`), non su `GDP_LOG_EDIZIONE`. Il campo `JOB_ID` invece esiste correttamente su `GDP_LOG_EDIZIONE` per tracciabilità.

**Richiesta:** Chiarire quali aggiornamenti devono avvenire su `GDP_IMPORT_TASK.STATO` e quali su `GDP_LOG_EDIZIONE.JOB_ID`. Correggere i riferimenti alle tabelle nella specifica di F10.

---

### 22 — Codici `"AT"` / `"PT"` ancora presenti nella logica di F04
**Riferimento:** Operazione F04 — Dettaglio passi operazione (classificazione edizione)

Nella logica decisionale di F04 i codici restano `"AT"` (anticipataria) e `"PT"` (posticipataria), in conflitto con i codici `"AN"` e `"PO"` presenti nella tabella `GDP_TIPO_EDIZIONE` e nell'operazione F15.

**Richiesta:** (Rif. anche domanda 9) Confermare ufficialmente il mapping AT→AN e PT→PO e aggiornare la specifica.

---

### 23 — Tipo errato del parametro `tipoAcquisizione` in F15
**Riferimento:** Operazione F15 — ricercaAcquisizioni

Il parametro `tipoAcquisizione` è dichiarato di tipo `I` (Integer) con valori ammessi `"G"` e `"S"`, che sono stringhe.

**Richiesta:** Correggere il tipo del parametro `tipoAcquisizione` di F15 in `String` (S).

---

### 24 — Valore del campo `PRIMA_PAGINA` nel flusso storico (F07)
**Riferimento:** Operazione F07 — ctrlEdizioniStoriche

Per le edizioni con data non conforme (tipoEdizione = "AS"), il record `GDP_LOG_EDIZIONE` include `PRIMA_PAGINA = [vuoto]`. Ma per le edizioni valide (tipoEdizione = "ST"), il campo `PRIMA_PAGINA` non viene mai menzionato nei passi di inserimento.

A differenza di F04 (flusso regolare), F07 non esegue il controllo euristico della prima pagina.

**Richiesta:** Chiarire il valore atteso di `GDP_LOG_EDIZIONE.PRIMA_PAGINA` per le edizioni del flusso storico: NULL, False, oppure va implementato un controllo euristico analogo a quello di F04?

---

### 25 — F19 (pulisciEdizione): logica operativa non specificata
**Riferimento:** Operazione F19 — DAMtrasmissione.pulisciEdizione

L'operazione F19 è contrassegnata "To BE DO" nella specifica e la sezione "Dettaglio passi operazione" è vuota. L'interfaccia di input dichiara `idLog` come parametro obbligatorio, ma non è chiaro se F19 debba agire puntualmente su una singola edizione o funzionare come job di pulizia batch.

**Richiesta:** Fornire la specifica operativa completa di F19 oppure indicare la logica di cleanup attesa (es. rimozione file in `/_tmp` e `/_dam` dopo un tempo configurabile, o dopo conferma DAM).

---

### 26 — Tipo di `MENSILITA`: intero o decimale?
**Riferimento:** Operazione F01 — configDTEdizioneAttesa

Nella descrizione operativa di F01 viene indicato il valore `0,5` per i periodici bimensili/quindicinali, il che richiederebbe un tipo numerico decimale. Tuttavia, nella DDL dello schema DB il campo `GDP_PERIODICITA.MENSILITA` è definito come `integer`, rendendo impossibile rappresentare il valore 0.5.

La convenzione adottata nello schema è che le pubblicazioni bimensili usino `MENSILITA = 1` con due date in `GG_PERIODICITA` (es. `G01;G15`), ma la specifica F01 non documenta questa convenzione e usa esplicitamente 0.5.

**Richiesta:** Allineare la specifica al tipo reale del campo:
- Confermare che `MENSILITA` è integer e che i periodici bimensili/quindicinali usano `MENSILITA=1` con due date in `GG_PERIODICITA` (aggiornando la specifica per eliminare il riferimento a 0.5)
- Oppure modificare il tipo DB in decimale/float.
