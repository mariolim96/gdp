domanda 1
Servizio: FTPregolare
Modalità di richiamo
Il servizio è richiamato in modalità “asincrona” dal BE applicativo.
per be applicativo cosa si intende ? e il bff ? e un comando cli ?
<<<<<<< Updated upstream
=======

domanda 2
Servizio: GdpEdizioneService (F08) / DamTrasmissioneService (F09)
Problema: Flusso F07 / F08 / F09 Ambiguity - The "Shared Log" Issue.
Descrizione:
Esiste una relazione uno-a-molti tra GDP_LOG e GDP_LOG_EDIZIONE, in particolar modo nel flusso storico F06/F07, in cui il log di una singola consegna può racchiudere svariate edizioni. L'utilizzo esplorativo e basico del solo parametro `idLog` per recuperare il record dell'edizione genera ambiguità: identificando solo la sessione complessiva di trasmissione e non la specifica edizione, il risultato al momento dell'estrazione e del salvataggio dati comporterebbe l'aggiornamento parziale, errato o continuo esclusivamente del primo record disponibile legato a quell'idLog.

Proposte / Suggerimenti per la Risoluzione:

1. Passare `idLogEdizione` come parametro (Raccomandata)
La soluzione più performante, coerente e senza ambiguità. Consiste nel modificare preventivamente le signature di F08 (`insEdizione`) e F09 (`creaXMLEdizione`) per accettare direttamente la Primary Key (`Integer idLogEdizione`), chiedendo agli orchestratori chiamanti (F04 e F07) di restituirla in seguito alla persistenza del record e fornirla a valle alla catena.

2. Ricerca univoca tramite Path (Alternativa)
Nel caso non si volessero alterare i parametri d'ingresso, andrebbe forzato l'uso combinato di `fk_gdp_log` unito alla validazione del parametro `pathEdizione` (creato appositamente univoco per edizione a partire dalla release V02 della documentazione). Suggerendo in questo scenario anche la formalizzazione di una costraint di unicità a database per tutelarsi da record duplicati per `fk_gdp_log, path_edizione`.

domanda 3
Servizio: FTPregolare e DAMtrasmissione (F04, F07 e F10)
Problema: Discrepanza sui parametri di F10 e sulle modalità di esecuzione.
Descrizione:
Nelle specifiche operative di F04 e F07 viene indicato di richiamare in modo asincrono l'operazione F10 passando come parametri 'idLog', 'idEdizione' e 'nomeFileZIP'. Tuttavia, la definizione dell'interfaccia di F10 prevede come parametri di input esclusivi solo 'idLog' e 'nomeFileEdizione' (omettendo 'idEdizione').
Inoltre, all'interno dei passaggi di F10, il processo viene descritto come uno "scheduler" o worker su base batch che interroga la tabella 'GDP_CODA_CARICAMENTO' filtrando tutti i record in attesa ("READY"), ignorando completamente i parametri di input.
Proposta/Richiesta: Si chiede di chiarire se F10 debba funzionare come consumer di una coda su base fissa/schedulata (e quindi i passi che vi passano direttamente parametri vanno eliminati dal diagramma/dal codice chiamante F04/F07), oppure se F10 sia da pensare come una chiamata che agisce puntualmente in base agli id tramandati in input (nel qual caso l'interfaccia deve includere 'idEdizione' o meglio 'idLogEdizione').

domanda 4
Servizio: DAMtrasmissione (F09 e F10)
Problema: Disallineamento sugli stati in GDP_CODA_CARICAMENTO.
Descrizione:
Alla fine dell'operazione F09, viene inserito un record nella coda in GDP_IMPORT_TASK / GDP_CODA_CARICAMENTO associandogli come STATO = 'PRO'. 
Tuttavia, il task schedulato (F10) incaricato di prendere i file da inviare interroga i record e processa solo quelli con STATO = 'READY'. 
Proposta/Richiesta: È necessario chiarire se "PRO" è l'abbreviazione di "PRONTO" e va normalizzato in 'READY'. F09 dovrebbe inserire i record già nello stato 'READY', oppure in alternativa F10 dovrebbe ricercare i record nello stato 'PRO' / convertirli a 'READY'.

domanda 5
Servizio: MONITOR (F21 attivaCODA e F14 preparaMAIL)
Problema: L'uso di idLog come unico parametro crea ambiguità analogamente a quanto riportato nella domanda 2.
Descrizione:
L'operazione F21 permette la rimessa in coda di una trasmissione fallita partendo da 'idLog'. Nel caso di flussi storici, un singolo identificativo è legato a decine di edizioni log (ognuna col suo GdpLogEdizione). Usando l'idLog parentale, F21 andrebbe a riattivare inavvertitamente e ciecamente tutte le code associate al flusso, perdendo la granularità. 
Allo stesso modo, l'operazione F14 usa 'idLog' per recuperare la 'DATA_EDIZIONE' (in corrispondenza 1-a-N nel caso storico) da un'occorrenza legata ai log per inserirla nel template della mail. 
Proposta/Richiesta: Modificare le interfacce affinché richiedano o preferiscano 'idLogEdizione' al posto di 'idLog' in caso di recupero granulare, permettendo al sistema di selezionare univocamente l'anomalia log-edizione desiderata.

domanda 6
Servizio: FTPregolare (F04)
Problema: Contraddizione sul posizionamento e filtraggio di GDP_DATA_USCITA (rimozione campo ANNO).
Descrizione: 
Nella sezione relativa al "DB Schema" di gdporch-spec.md, viene sottolineato come: "Important: The table has DT_INIZIO + DT_FINE range fields (not ANNO as previously documented)". Sostanzialmente, rimuove l'esistenza del campo ANNO.
Tuttavia, all'interno del flow dell'operazione F04, uno dei controlli è di interrogare il Database filtrando per forza per "ANNO = current_year".
Proposta/Richiesta: Aggiornare la query documentata in F04 in modo che si basi sul filtraggio usando DATA_ATTESA, al massimo contestualizzato tramite range (DT_INIZIO e DT_FINE) in luogo del rimosso ANNO.

domanda 7
Servizio: DAMtrasmissione (F09 e F10)
Problema: Il campo STATO in GDP_IMPORT_TASK / GDP_CODA_CARICAMENTO non è presente sul DataBase.
Descrizione: 
Nelle specifiche operative (ad es. per F09, F10 e F21) si fa reiterato riferimento all'impostazione e lettura del campo `STATO` sulla tabella `GDP_IMPORT_TASK` (oppure `GDP_CODA_CARICAMENTO`) con valori come 'PRO', 'READY', 'SUBMITTED', 'FAILED'. Tuttavia, come confermato dalle analisi dello schema DB reale, questo campo non risulta essere presente nel database originario. Purtroppo per via di come è pensata l'architettura, risulta indispensabile poter filtrare i record della coda per il loro stato.
Proposta/Richiesta: Confermare ufficialmente l'aggiunta del campo `STATO` allo schema del Database (modificando il DDL) per poter gestire correttamente il ciclo di vita e la persistenza della coda di caricamenti verso il DAM.

domanda 8
Servizio: Configurazione DB / DDL Iniziale
Problema: Assenza di definizioni (Sequence o auto-increment) per le Primary Key nel DDL fornito.
Descrizione:
Nello schema di database fornito (Init/DDL) manca completamente l'implementazione o la dichiarazione delle _Sequence_ necessarie per generare in modo progressivo ed automatico gli identificativi di Primary Key sui nuovi insert (es. ID_GDP_LOG, ID_GDP_EDIZIONE, ID_GDP_PAGINA, ecc.). Questo ha provocato l'assenza di generazione da parte dell'ORM JPA/Hibernate, che sollevava eccezioni per chiavi duplicate essendo ignaro della modalità di allocazione IDs.
Proposta/Richiesta: Abbiamo provveduto temporaneamente a creare script personalizzati nel nostro env per definire le sequenze (es. `CREATE SEQUENCE IF NOT EXISTS seq_gdp_edizione START WITH 10;`) ed a sincronizzarle col pre-caricamento base (`setval`). Si chiede l'ufficializzazione dell'aggiunta nel DDL nativo oppure di formalizzare un uso tramite pattern Hibernate o identità seriali supportate.

---

domanda 9
Servizio: FTPregolare (F04) / FTPsaltuario (F07)
Problema: Discrepanza sui codici tipoEdizione tra features-be.md e gdporch-spec.md.
Descrizione:
In features-be.md (SFU-01), il flusso F04 usa i codici `"AT"` (anticipataria) e `"PT"` (posticipataria) per classificare le edizioni. La stessa specifica F15 (ricercaAcquisizioni) usa `"AN"` (anticipataria) e `"PO"` (posticipataria) nella lista dei valori ammessi per il parametro `tipoEdizione`.
In gdporch-spec.md, la tabella DB `GDP_TIPO_EDIZIONE` (init.sql) è dichiarata come source of truth e riporta `"AN"` e `"PO"` (non `"AT"` / `"PT"`). La spec interna di gdporch-spec.md adotta correttamente `"AN"` e `"PO"`.
Proposta/Richiesta: Aggiornare **features-be.md** alla descrizione di F04 (e ovunque compaiano `"AT"` e `"PT"`) sostituendoli con `"AN"` e `"PO"` per allinearsi allo schema DB. Oppure confermare esplicitamente che il mapping tra codici vecchi e nuovi è: AT→AN, PT→PO.

domanda 10
Servizio: FTPregolare (F04) / FTPsaltuario (F07)
Problema: F04 in features-be.md richiama F09 con `<IDEdizione>` come parametro, ma in features-be.md la firma di F09 dichiara il terzo parametro come "IDEdizione Formato yyyy-mm-dd Dt" (tipo data, non intero).
Descrizione:
In features-be.md, l'interfaccia di richiamo (input) di F09 elenca:
  - Parametro 3: `IDEdizione` — "Formato yyyy-mm-dd Dt" (tipo data)
Il campo di F04 che invoca F09 passa `<IDEdizione>` (ID numerico restituito da F08, non una data).
Analogamente, gdporch-spec.md (sezione F09) chiarisce che il parametro 3 è `IDEdizione = ID_EDIZIONE` di tipo `Integer`.
Proposta/Richiesta: Correggere la tabella dei parametri di input di F09 in features-be.md: il parametro 3 deve essere `IDEdizione` di tipo `Integer` (FK a GDP_EDIZIONE), non una data in formato yyyy-mm-dd.

domanda 11
Servizio: FTPregolare (F07) — Modalità di richiamo
Problema: F07 in features-be.md dichiara di essere richiamato "dall'operazione F07" (autoreferenziale), non da F06.
Descrizione:
In features-be.md, alla sezione F07, la "Modalità di richiamo" recita:
  "Il servizio è richiamato in modalità "asincrona" dall'operazione F07."
Questo è chiaramente un errore tipografico/copia-incolla: F07 non può richiamare sé stesso. Dalla logica del sistema e da gdporch-spec.md risulta che F07 è richiamato da **F06** (FTPsaltuario.checkConsegnaStorico).
Proposta/Richiesta: Correggere features-be.md: la "Modalità di richiamo" di F07 deve riportare "richiamato in modalità asincrona dall'operazione **F06**".

domanda 12
Servizio: FTPregolare (F01) e FTPregolare (F18)
Problema: Disallineamento nel nome del campo filtro `INVIO_EDIZIONI` vs `INVIO_EDIZIONE`.
Descrizione:
In features-be.md, le operazioni F01 e F18 filtrano le testate attive usando il vincolo `GDP_TESTATA.INVIO_EDIZIONI = 1` (con la S finale — plurale).
In gdporch-spec.md, la tabella `GDP_TESTATA` documenta esplicitamente il campo come `INVIO_EDIZIONE` (singolare, boolean), e segnala:
  "Important: The column is `INVIO_EDIZIONE` (singular), not `INVIO_EDIZIONI`."
Proposta/Richiesta: Chiarire e uniformare il nome del campo: qual è il nome esatto nella DDL del database? Tutti i riferimenti in features-be.md (F01, F18) devono essere aggiornati di conseguenza.

domanda 13
Servizio: DB (F08) — Campo GDP_PAGINA.ANNO_EDIZIONE
Problema: features-be.md non menziona il campo `ANNO_EDIZIONE` in GDP_PAGINA, mentre gdporch-spec.md lo include.
Descrizione:
La specifica del servizio F08 in features-be.md elenca i campi da inserire nella tabella GDP_PAGINA durante l'inserimento di una nuova edizione, ma non include il campo `ANNO_EDIZIONE`.
Tuttavia, gdporch-spec.md elenca `ANNO_EDIZIONE (integer — redundant year)` come campo presente e mappato nella tabella `GDP_PAGINA`.
Proposta/Richiesta: Confermare se `ANNO_EDIZIONE` deve essere popolato al momento dell'insert su GDP_PAGINA (con il valore dell'anno estratto da `DATA_EDIZIONE`) e, in caso affermativo, aggiornare la specifica F08 in features-be.md.

domanda 14
Servizio: DB (F08) — Campo GDP_PAGINA.FILE_TIF nelle edizioni storiche
Problema: features-be.md imposta sempre `FILE_TIF = NULL` su GDP_PAGINA, ma le edizioni storiche prevedono file TIF.
Descrizione:
In features-be.md, la specifica F08 (sia in Inserimento che in Aggiornamento) imposta `FILE_TIF = NULL` per tutti i record su GDP_PAGINA senza distinzione tra flusso regolare e storico.
Tuttavia, gdporch-spec.md documenta `FILE_TIF (varchar 128)` come campo nullable, e F07 (storico) verifica e conta esplicitamente i file TIF. Nel flusso storico ci si aspetta che i TIF esistano e siano associati alle pagine.
Proposta/Richiesta: Chiarire se F08 deve popolare `FILE_TIF` con il nome del file .tif corrispondente quando invocato dal flusso storico F07, oppure se i file TIF hanno un percorso di archiviazione separato (es. conservazione) e GDP_PAGINA.FILE_TIF rimane sempre NULL.

domanda 15
Servizio: MONITOR (F12 — elencoAcquisizioni)
Problema: Il campo `idUtenteSFTP` è nell'output di F12 in features-be.md ma non ha una sorgente DB esplicitata.
Descrizione:
In features-be.md, l'output di F12 elenca il campo `idUtenteSFTP` (campo 2), ma la sezione "Dettaglio passi operazione" non specifica da quale tabella/campo DB esso venga recuperato. La logica descrive solo il join con GDP_TESTATA, GDP_EDIZIONE e GDP_LOG_EDIZIONE.
Anche in gdporch-spec.md, il campo `1.2 idUtenteSFTP` è elencato nell'output senza un mapping DB esplicito nella tabella di output.
Proposta/Richiesta: Specificare esplicitamente da dove viene recuperato `idUtenteSFTP`: si presume da `GDP_LOG.FK_GDP_UTENTEFTP` oppure direttamente da `GDP_UTENTSFTP` tramite join. Aggiornare la specifica del mapping dell'output di F12.

domanda 16
Servizio: DAMtrasmissione (F09) — Aggiornamento GDP_LOG_EDIZIONE tramite idLog ambiguo
Problema: F09 in features-be.md usa `idLog` per aggiornare `GDP_LOG_EDIZIONE.FILE_XML` in caso di errore, ma la relazione è 1-a-N nel flusso storico.
Descrizione:
Nella sezione finale di F09 (features-be.md), in caso di errore di creazione XML o ZIP, il sistema imposta rispettivamente `GDP_LOG_EDIZIONE.FILE_XML=False` o `GDP_LOG_EDIZIONE.FILE_ZIP=False` "per l'occorrenza relativa a idLog ricevuto in input". Nel flusso storico un `idLog` è associato a molteplici record `GDP_LOG_EDIZIONE`.
Questo è analogo al problema già descritto nella domanda 2, ma qui si manifesta nel percorso di errore di F09, non nel percorso nominale.
Proposta/Richiesta: Allineare F09 alla soluzione proposta per la domanda 2 (passare `idLogEdizione` o usare path+idLog come chiave composta univoca) anche per la gestione degli errori, in modo che `FILE_XML` e `FILE_ZIP` vengano aggiornati sul record corretto.

---

domanda 17
Servizio: MONITOR (F12 — elencoAcquisizioni)
Problema: Il campo `NomeTestata` in features-be.md è mappato su `GDP_TESTATA.ID_GDP_TESTATA` (un ID numerico) anziché su `GDP_TESTATA.NOME_TESTATA`.
Descrizione:
In features-be.md, nella sezione "Dettaglio passi operazione" di F12, il mapping è:
  `NomeTestata = GDP_TESTATA.ID_GDP_TESTATA`
Questo è chiaramente errato: l'ID numerico della testata non è il nome. In gdporch-spec.md il mapping corretto è:
  `nomeTestata → GDP_TESTATA.NOME_TESTATA` (campo 1.4).
Proposta/Richiesta: Correggere features-be.md per F12: il campo `NomeTestata` deve mapparsi su `GDP_TESTATA.NOME_TESTATA`.

domanda 18
Servizio: MONITOR (F13 — dettaglioAcquisizione)
Problema: Numerazione duplicata dei campi di output in features-be.md (campo 1.12 usato per `fileXML` e `fileZIP`).
Descrizione:
In features-be.md (e specularmente in gdporch-spec.md), la tabella di output di F13 assegna il numero `1.12` sia a `fileXML` sia a `fileZIP`. Dovrebbero avere numerazione distinta (es. 1.12 per fileXML e 1.13 per fileZIP, con rinumerazione dei successivi).
Proposta/Richiesta: Correggere la numerazione dei campi di output di F13 in entrambi i documenti per evitare ambiguità.

domanda 19
Servizio: MONITOR (F13 — dettaglioAcquisizione)
Problema: Il campo `NomeTestata` in features-be.md è mappato su `GDP_TESTATA.ID_GDP_TESTATA` anziché su `GDP_TESTATA.NOME_TESTATA` (stessa anomalia del F12).
Descrizione:
In features-be.md, nella sezione "Dettaglio passi operazione" di F13, il mapping riporta:
  `NomeTestata = GDP_TESTATA.ID_GDP_TESTATA`
Anche qui si tratta di un errore: dovrebbe essere `GDP_TESTATA.NOME_TESTATA`.
In gdporch-spec.md il campo 1.3 è correttamente mappato come `GDP_TESTATA.NOME_TESTATA`.
Proposta/Richiesta: Allineare features-be.md per F13 al mapping corretto.

domanda 20
Servizio: FTPsaltuario (F06 — checkConsegnaStorico)
Problema: features-be.md non valorizza `TIPO_ACQUISIZIONE` nel record GDP_LOG per il flusso storico.
Descrizione:
In features-be.md, nella descrizione operativa di F06, quando viene inserito il record GDP_LOG per il caso di match singolo (testata trovata), il campo `TIPO_ACQUISIZIONE` non viene citato tra i campi valorizzati. Vengono indicati solo `FK_TESTATA` e `DT_ACQUISIZIONE`.
In gdporch-spec.md (sezione F06), il record GDP_LOG viene inserito con gli stessi campi ma anch'esso non specifica esplicitamente `TIPO_ACQUISIZIONE='S'`. Tuttavia per coerenza col flusso regolare F03 (che imposta `TIPO_ACQUISIZIONE='G'`), F06 dovrebbe impostare esplicitamente `TIPO_ACQUISIZIONE='S'`.
Proposta/Richiesta: Aggiornare entrambi i documenti per rendere esplicito che F06 deve impostare `GDP_LOG.TIPO_ACQUISIZIONE = 'S'` nel record inserito per il caso di match singolo.

domanda 21
Servizio: DAMtrasmissione (F10 — inviaEdizione)
Problema: In features-be.md, F10 aggiorna `STATO` e `JOB_ID` su `GDP_LOG_EDIZIONE`, ma questi campi nella spec DB appartengono a `GDP_IMPORT_TASK` (`GDP_CODA_CARICAMENTO`).
Descrizione:
In features-be.md, per F10 in caso di "status = FAILED" viene indicato:
  `GDP_LOG_EDIZIONE.JOB_ID = jobId`
  `GDP_LOG_EDIZIONE.STATO = "FAILED"`
E per "status = SUBMITTED":
  `GDP_LOG_EDIZIONE.FILE_ZIP = True`
  `GDP_LOG_EDIZIONE.JOB_ID = jobId`
  `GDP_LOG_EDIZIONE.STATO = "SUBMITTED"`
Il campo `JOB_ID` esiste effettivamente su `GDP_LOG_EDIZIONE`, ma il campo `STATO` con valori 'FAILED'/'SUBMITTED'/'READY' è definito su `GDP_IMPORT_TASK` (= `GDP_CODA_CARICAMENTO`), non su `GDP_LOG_EDIZIONE`. In gdporch-spec.md la situazione è la stessa.
Proposta/Richiesta: Chiarire se F10 deve aggiornare lo `STATO` su `GDP_IMPORT_TASK.STATO` (come sembra logico, dato che è la coda di caricamento che gestisce il ciclo di vita) e il `JOB_ID` su `GDP_LOG_EDIZIONE.JOB_ID` (per tracciabilità). In features-be.md e gdporch-spec.md allineare i riferimenti alle tabelle corrette.

domanda 22
Servizio: FTPregolare (F04) — Codici tipoEdizione ancora AT/PT in features-be.md
Problema: Nonostante la domanda 9 già documenti la discrepanza, in features-be.md (F04 "Dettaglio passi operazione") i codici `"AT"` e `"PT"` rimangono in uso anche nella logica decisionale (classificazione edizione).
Descrizione:
In features-be.md (righe ~358-363) il flusso F04 classifica come segue:
  `dataEdizione > data elaborazione → tipoEdizione = "AT"` [anticipataria]
  `dataEdizione < data elaborazione → tipoEdizione = "PT"` [posticipataria]
Questo genera non solo un disallineamento sui codici (AT vs AN, PT vs PO) ma anche un'inversione logica rispetto a gdporch-spec.md:
  - gdporch-spec.md: `dataEdizione > processing date → "AN"` (early = anticipataria) ✓
  - features-be.md: `dataEdizione > data elaborazione → "AT"` — stessa logica, codice diverso.
In aggiunta features-be.md non è stato aggiornato a V03 mentre gdporch-spec.md sì.
Proposta/Richiesta: (Ribadisce domanda 9) Confermare ufficialmente il mapping AT→AN e PT→PO e aggiornare features-be.md.

domanda 23
Servizio: MONITOR (F15 — ricercaAcquisizioni)
Problema: Il tipo del parametro `tipoAcquisizione` in gdporch-spec.md è dichiarato `Integer` invece di `String`.
Descrizione:
In features-be.md (F15), il parametro `tipoAcquisizione` ha tipo `I` (Integer) con valori `"G"` e `"S"`, il che è già incoerente (valori stringa con tipo intero).
In gdporch-spec.md, la tabella dei parametri di input F15 dichiara anch'essa `tipoAcquisizione` di tipo `Integer`, ma i valori ammessi sono stringhe (`"G"`, `"S"`).
Proposta/Richiesta: Correggere il tipo del parametro `tipoAcquisizione` di F15 in `String` in entrambi i documenti.

domanda 24
Servizio: FTPregolare (F01 e F18) — INVIO_EDIZIONI ancora presente in gdporch-spec.md
Problema: Nonostante gdporch-spec.md corregga il nome del campo a `INVIO_EDIZIONE` (singolare) nella sezione DB Schema, nelle sezioni operative F01 e F18 scrive ancora `INVIO_EDIZIONI` (plurale).
Descrizione:
In gdporch-spec.md:
  - Sezione DB Schema (GDP_TESTATA, riga ~110-112): il campo è correttamente documentato come `INVIO_EDIZIONE` (singolare).
  - Sezione F01, tabella input parametro 3 (riga ~238): `If empty, process all testate with INVIO_EDIZIONI=1` — usa il plurale.
  - Sezione F18, tabella input parametro 3 (riga ~535): `If empty, all testate with INVIO_EDIZIONI=1` — usa il plurale.
Questo crea autocontraddizione interna a gdporch-spec.md stesso.
Proposta/Richiesta: Correggere in gdporch-spec.md anche le sezioni F01 e F18 per usare `INVIO_EDIZIONE` (singolare) nei parametri di input, allineandole alla documentazione dello schema DB.

domanda 25
Servizio: DB (F08) — Campo GDP_PAGINA.ANNO_EDIZIONE assente sia in features-be.md che in gdporch-spec.md operativo
Problema: `ANNO_EDIZIONE` è documentato nello schema DB di gdporch-spec.md ma non è presente nei passi operativi di F08 né in features-be.md né in gdporch-spec.md.
Descrizione:
Nella sezione DB Schema di gdporch-spec.md, `GDP_PAGINA` include `ANNO_EDIZIONE (integer — redundant year)`. Tuttavia, nella sezione operativa F08 di gdporch-spec.md (righe ~724-733), i campi inseriti per GDP_PAGINA non includono `ANNO_EDIZIONE`. Analogamente, features-be.md non lo menziona.
(Rinforza domanda 13) La mancanza è presente in entrambi i documenti, non solo in features-be.md.
Proposta/Richiesta: Se `ANNO_EDIZIONE` deve essere valorizzato (come suggerito dalla sua esistenza nello schema), aggiornare i passi operativi di F08 in **entrambi** i documenti.

domanda 26
Servizio: FTPsaltuario (F07 — ctrlEdizioniStoriche)
Problema: features-be.md non menziona il campo `PRIMA_PAGINA` nel record `GDP_LOG_EDIZIONE` per il flusso storico, ma per le edizioni anomale lo imposta a `[vuoto]`.
Descrizione:
In features-be.md (F07), quando un'edizione ha data non conforme (tipoEdizione = "AS"), il record GDP_LOG_EDIZIONE include `PRIMA_PAGINA = [vuoto]`. Ma per le edizioni valide (tipoEdizione = "ST"), il campo `PRIMA_PAGINA` non viene mai menzionato tra i campi del record GDP_LOG_EDIZIONE inserito.
In gdporch-spec.md (F07), il campo `PRIMA_PAGINA` non è elencato tra i campi inseriti per GDP_LOG_EDIZIONE nella sezione "Insert GDP_LOG_EDIZIONE" (~righe 641-647).
A differenza di F04 (flusso regolare), F07 non esegue il controllo euristico della prima pagina. Non è chiaro se `PRIMA_PAGINA` debba restare NULL per le edizioni storiche "ST" o se debba essere esplicitamente impostato a False.
Proposta/Richiesta: Chiarire il valore atteso di `GDP_LOG_EDIZIONE.PRIMA_PAGINA` per le edizioni del flusso storico: NULL, False, o se va implementato un controllo euristico analogo a F04.

domanda 27
Servizio: DAMtrasmissione (F19 — pulisciEdizione)
Problema: F19 è contrassegnato "To BE DO" in features-be.md, ma gdporch-spec.md aggiunge un "Cleanup staging" job schedulato senza coordinamento.
Descrizione:
In features-be.md, F19 è lasciato vuoto con la nota "To BE DO" (non implementato/specificato). La sua interfaccia di input dichiara `idLog` come parametro obbligatorio.
In gdporch-spec.md, F19 è documentato come "Status: Detail TBD". Tuttavia, la tabella dei Scheduled Jobs include anche un job separato "Cleanup staging – Daily at 03:30 – Remove files in `/_tmp` and `/_dam` older than 4 hours" che sembra sovrapporre o prevenire F19.
Proposta/Richiesta: Chiarire la relazione tra F19 e il job "Cleanup staging" in gdporch-spec.md. Sono la stessa cosa? Il job cleanup è la specifica attesa di F19 oppure è un processo indipendente? Se sono separati, definire cosa fa ciascuno.

domanda 28
Servizio: FTPregolare (F01) — Tipo di MENSILITA
Problema: In features-be.md `MENSILITA` deve supportare valori decimali (es. 0.5 = bimensile/quindicinale), ma gdporch-spec.md lo dichiara come `integer`.
Descrizione:
In features-be.md (F01), la descrizione operativa usa:
  - `MENSILITA > 0` per il caso A (periodicità mensile o multi-mensile)
  - `MENSILITA = 0` per il caso B (periodicità inferiore al mese)
  - Nella nota 1 viene specificato: "0,5 = bimensile o quindicinale" come valore ammesso
Tuttavia in gdporch-spec.md, la sezione DB Schema dichiara:
  `MENSILITA (integer — not double/float)`
Con un integer non è possibile rappresentare il valore 0.5 (bimensile).
gdporch-spec.md stesso risolve parzialmente questo dicendo: "Twice-monthly publications use MENSILITA=1 with two dates in GG_PERIODICITA (e.g. G01;G15)" — ma features-be.md non documenta questa convenzione e usa esplicitamente 0.5.
Proposta/Richiesta: Allineare i documenti: confermare che `MENSILITA` è integer e che le pubblicazioni bimensili usano `MENSILITA=1` con due date in `GG_PERIODICITA`, aggiornando features-be.md per eliminare il riferimento a 0.5. Oppure rendere `MENSILITA` di tipo numerico (decimal/float) nello schema DB.

domanda 29
Servizio:F21 - MONITOR.attivaCODA
Problema: Impossibilità di identificare univocamente il record in GDP_CODA_CARICAMENTO tramite il solo idLog.
Descrizione:
L'analisi dello schema ER (Entity-Relationship) evidenzia che la relazione tra GDP_LOG e GDP_LOG_EDIZIONE è di tipo 1-a-N (uno-a-molti). In contesti di caricamento massivo (flussi storici), un singolo idLog può generare decine di record in GDP_LOG_EDIZIONE.
Poiché la tabella GDP_CODA_CARICAMENTO punta a GDP_LOG_EDIZIONE (tramite FK_GDP_LOG_EDIZIONE), l'invio del solo idLog rende il sistema incapace di determinare quale specifica edizione debba essere portata in stato "READY".
>>>>>>> Stashed changes
