## Vista d'insieme

```
GdP--VDI- 01 - V0 3
Pag. 1 di 14
```
# GdP - Giornali del Piemonte

# Vista d’insieme

## Versione 03

```
(Febbraio 2026 )
```
##### VERIFICHE E APPROVAZIONI

##### VERS

##### REDAZIONE

##### CONTROLLO

##### APPROVAZIONE

##### AUTORIZZAZIONE

##### EMISSIONE

##### NOME DATA NOME DATA NOME DATA

##### V

```
U. Mandosio
R.Leccese
26/02/2026 P. Galliano 02/03/2026 P. Galliano 02/03/
```
```
V02 U. Mandosio 16/01/2026 P. Galliano 16/01/2026 E. Fiorio 16/01/
```
```
V
```
```
U. Mandosio
R. Leccese
B. Bono
```
```
19/12/2025 P. Galliano 23/12/2025 E. Fiorio 23/12/
```
##### STATO DELLE VARIAZIONI

##### VERS PARAGRAFO O

##### PAGINA

##### DESCRIZIONE DELLA VARIAZIONE

```
V0 3 Paragrafo 1.
Paragrafo 1. 4
```
```
Inserito riferimento a file esterno
Aggiornati diagrammi delle componenti e di deployment
V02 Paragrafo 1.3 Inserito modello dei dati
V01 Tutto il documento Versione iniziale del documento
```

## Vista d'insieme

         - GdP--VDI- 01 - V0
            - Pag. 2 di
- 1. VISTA D’INSIEME SOMMARIO
   - 1.1 RIFERIMENTI
   - 1.2 SCENARI DI BUSINESS (USER STORIES)
      - 1.2.1 Inquadramento
      - 1.2.2 Scenario 1 – Abilitazione utente
      - 1.2.3 Scenario 2 – Registrazione nuova testata
      - 1.2.4 Scenario 3 – Alimentazione periodica edizioni testata
      - 1.2.5 Scenario 4 – Alimentazione edizioni testata storica
      - 1.2.6 Scenario 5 - Monitoraggio edizioni
      - 1.2.7 Scenario 6 – Ricerca archivio e consultazione edizioni...........................................................................
      - 1.2.8 Scenario 7 – Fascicolazione risultati ricerca
      - 1.2.9 Scenario 8 – Ottemperare ad una richiesta di oblio
   - 1.3 MODELLO DEI DATI
   - 1.4 ARCHITETTURA APPLICATIVA
      - 1.4.1 Funzionalità
      - 1.4.2 Applicativi associati al progetto
      - 1.4.3 Dipendenze e criticità applicative della soluzione
      - 1.4.4 Sintesi della soluzione tecnica
   - 1.5 ARCHITETTURA TECNOLOGICA
      - 1.5.1 Standard da soddisfare
      - 1.5.2 Pila tecnologica
- 2. APPENDICE 1: GLOSSARIO DEI TERMINI DELLA VISTA D’INSIEME


**Vista d'insieme**

```
GdP--VDI- 01 - V0 3
Pag. 3 di 14
```
## 1. VISTA D’INSIEME SOMMARIO

### 1.1 RIFERIMENTI

Il presente documento fa riferimento alla scheda del Piano di Sviluppo 2025 del Consiglio regionale del Piemonte (rif.
2025.CR.CC.01).
L'obiettivo della proposta è la realizzazione della nuova Piattaforma dei materiali editoriali in cui confluiranno tutti i
materiali ad oggi ricompresi nel servizio “Giornali del Piemonte” (di seguito indicato come GdP), il materiale che le
singole testate giornalistiche continueranno a mettere a disposizione del sistema ed il materiale derivante da campagne
di digitalizzazione relative a testate storiche di interesse.

La proposta, nel dettaglio, prevede:

- la realizzazione di un nuovo servizio di archiviazione a lungo termine delle informazioni digitali, sul quale
    migrare gli archivi attualmente in uso, con l’obiettivo di ottimizzare le componenti infrastrutturali e i relativi
    costi di gestione;
- la riprogettazione del processo di acquisizione del materiale, sia attinente al materiale delle testate storiche
    sia relativo al materiale messo a disposizione periodicamente dalle singole testate;
- la riprogettazione della componente di front end, mediante l’adozione di tecnologie moderne e conformi alle
    linee guida AGID per la Pubblica Amministrazione, in grado di garantire agli utenti un’esperienza di
    consultazione e ricerca più efficiente e fruibile da una pluralità di dispositivi.

Beneficiari della proposta sono tutte le Direzioni, i Settori e i dipendenti del Consiglio Regionale del Piemonte, nonché
tutti gli utilizzatori dell’attuale servizio.

### 1.2 SCENARI DI BUSINESS (USER STORIES)

#### 1.2.1 Inquadramento

Il sistema nel suo complesso prevede il coinvolgimento di due fonti di alimentazione esterna:

- gli editori di testate locali, che in base alla periodicità della pubblicazione inviano il materiale dell’edizione
    via sFTP su area riservata;
- i gestori di archivi storici (perlopiù biblioteche) che trasmettono sempre via sFTP le edizioni storiche relative
    ad un periodo temporale per una o più testate.

Il materiale consegnato è costituito da file PDF per le edizioni attive e da file PDF, TXT e TIFF per le edizioni storiche.

#### 1.2.2 Scenario 1 – Abilitazione utente

```
Il nuovo soggetto da abilitare al sistema GdP può essere:
▪ un editore di testate locali che voglia inviare edizioni di una testata attiva;
▪ un incaricato da parte di un gestore di archivi storici che debba consegnare materiale digitalizzato
di pubblicazioni periodiche o tematiche;
```
```
Esigenza da indirizzare Attivare le credenziali per nuovi soggetti abilitati a GdP
Obiettivi Come operatore del servizio voglio registrare un utente in modo che
possa accedere al sistema ed operare in base al ruolo assegnatogli
Stakeholders coinvolti Consiglio Regionale del Piemonte (CRP), Regione Piemonte (RP)
Scenari di business coinvolti
```

**Vista d'insieme**

```
GdP--VDI- 01 - V0 3
Pag. 4 di 14
```
```
▪ un utente in possesso di credenziali di posta del Consiglio Regionale o RUPAR che abbia richiesto
un accesso privilegiato a Giornali del Piemonte.
```
L’operatore di servizio, dopo aver verificato che il soggetto è in possesso dell’autorizzazione del responsabile CRP,
censisce l’utente nel sistema e gli assegna il ruolo opportuno per consentirgli di accedere alle funzionalità riservate:

```
▪ utenteFTP per poter trasmettere le edizioni periodiche o consegnare materiale storico digitalizzato;
▪ utenteWEB per accedere in consultazione anche alle pubblicazioni recenti di ciascuna testata attiva
che invia periodicamente le edizioni.
```
Il sistema considera di default recenti le ultime due edizioni pubblicate da una testata, in base alla sua periodicità.

#### 1.2.3 Scenario 2 – Registrazione nuova testata

1.2.3.1 Descrizione

L’operatore di servizio, dopo aver verificato l’autorizzazione da parte del responsabile CRP, censisce nel sistema i
dati descrittivi, i dati tematici e di geolocalizzazione ed eventuali informazioni di supporto per la nuova testata storica
o attiva.

Il sistema assegna l’identificativo univoco. Se la testata è attiva ed invia periodicamente le edizioni, imposta i dati di
configurazione necessari per l’area dedicata alla trasmissione del materiale via sFTP e verifica che la testata sia
abbinata ad un soggetto abilitato alla trasmissione del materiale

#### 1.2.4 Scenario 3 – Alimentazione periodica edizioni testata

1.2.4.1 Descrizione

Il sistema esterno predisposto (o l’utente designato dall’editore) si autentica via FTP, viene indirizzato
automaticamente nell’area riservata, predisposta dal sistema GdP con la data di pubblicazione attesa in base alla
periodicità, e deposita il materiale editoriale.

A cadenza regolare, il sistema GdP verifica la presenza di edizioni trasmesse; se trovate, effettua alcuni controlli,
superati i quali avvia il trasferimento sul DAM e la registrazione dei metadati nella base dati. Qualora venissero invece
rilevati errori e/o anomalie, il sistema ne riporta i dettagli nel registro delle irregolarità, per un eventuale trattamento
successivo.
Al termine del trasferimento sul DAM con esito positivo l’edizione depositata viene eliminata.

```
Esigenza da indirizzare Censire una nuova testata attiva o storica
Obiettivi Come operatore del servizio voglio registrare una nuova testata
attiva o storica in modo da poter acquisire le relative edizioni
Stakeholders coinvolti Editori, Gestori archivi storici
Scenari di business coinvolti
```
```
Esigenza da indirizzare Trasmettere l’edizione di una testata attiva già censita a sistema
Obiettivi Come editore di una testata in attività voglio trasmettere in
autonomia l’edizione in modo da vederla pubblicata su “GdP”
Stakeholders coinvolti Editori, DAM
Scenari di business coinvolti Scenari 1 e 2
```

**Vista d'insieme**

```
GdP--VDI- 01 - V0 3
Pag. 5 di 14
```
#### 1.2.5 Scenario 4 – Alimentazione edizioni testata storica

1.2.5.1 Descrizione

Il gestore dell’archivio storico (o l’utente designato dal gestore) a seguito dell’esito positivo di validazione del
prototipo riceve le credenziali per consegnare il materiale via FTP; al momento della consegna, dopo l’autenticazione
viene indirizzato automaticamente in un’area dedicata, predisposta dal sistema GdP ed univocamente identificata per
depositare il materiale editoriale.

A cadenza giornaliera, il sistema GdP verifica la presenza di materiale trasmesso; se trovato, effettua alcuni controlli,
superati i quali avvia il trasferimento sul DAM della parte di dati pubblicabili (PDF e TXT), archivia i dati da
conservare (TIFF) e registra i metadati nella base dati.
Al termine del trasferimento sul DAM con esito positivo il materiale precedentemente depositato viene eliminato.

#### 1.2.6 Scenario 5 - Monitoraggio edizioni

1.2.6.1 Descrizione

L’operatore di servizio accede al cruscotto nella sezione di verifica delle edizioni attese o nella sezione di controllo
delle consegne di digitalizzazioni di materiale storico; qui può controllare lo stato complessivo dei caricamenti delle
edizioni previste, il numero di pagine trasmesse e acquisite con evidenza di eventuali anomalie, quali: numero file
corrotti, numero file non leggibili, presenza cartelle irregolari.
L’operatore può ripetere tali controlli per singola testata selezionata e per altra data specificata.
Dall’elenco che riporta lo stato complessivo, l’operatore accede al dettaglio della singola edizione per verificare le
eventuali segnalazioni.
Il sistema permette di scaricare e ricaricare file e di riattivare il processo di trasmissione al DAM.

#### 1.2.7 Scenario 6 – Ricerca archivio e consultazione edizioni...........................................................................

```
Esigenza da indirizzare Trasmettere le edizioni di una testata storica
Obiettivi Come gestore di archivio storico voglio trasmettere in autonomia le
edizioni di un periodo di una testata storica in modo da vederle
pubblicate su “GdP”
Stakeholders coinvolti Gestori archivi storici, aziende incaricate della digitalizzazione, DAM
Scenari di business coinvolti Scenari 1 e 2
```
```
Esigenza da indirizzare Controllare il materiale editoriale trasmesso prima del
trasferimento al DAM
Obiettivi Come operatore del servizio voglio verificare il materiale
editoriale trasmesso in modo da valutare eventuali edizioni
scartate o trattare eventuali anomalie/errori
Stakeholders coinvolti Editori, Gestori archivi storici
Scenari di business coinvolti Scenari 3 e 4
```
```
Esigenza da indirizzare Eseguire ricerche sull’archivio, esplorare il catalogo delle testate
o sfogliare un’edizione
Obiettivi Come utente voglio trovare articoli su fatti ed avvenimenti di
interesse, consultare il catalogo delle testate ottenendo l’elenco
delle sue edizioni ed eventualmente consultare un’edizione
specifica
Stakeholders coinvolti Utente web, utente privilegiato, CRP
Scenari di business coinvolti
```

**Vista d'insieme**

```
GdP--VDI- 01 - V0 3
Pag. 6 di 14
```
1.2.7.1 Descrizione

L’utente esegue una ricerca diretta per trovare articoli su fatti ed avvenimenti di interesse, sfruttando eventualmente i
filtri a diposizione per circoscrivere il contesto di ricerca su base temporale, territoriale (province), per testata specifica
o area tematica, ed ottenere il risultato sotto forma di elenco di pagine di edizione, ricavate da tutte le testate presenti
in archivio.
L’elenco di pagine di edizioni, ordinato temporalmente o con altri criteri, può essere raffinato agendo su opportuni
filtri.
La ricerca non tiene conto delle ultime due edizioni di ciascuna testata attiva per il generico utente Web.

L’utente consulta il catalogo delle testate, sfruttando eventualmente i filtri a diposizione per delimitare l’elenco, ed
ottiene l’elenco delle edizioni di quelle rispondono ai criteri impostati, eventualmente consulta un’edizione specifica.

#### 1.2.8 Scenario 7 – Fascicolazione risultati ricerca

1.2.8.1 Descrizione

L’utente sceglie una o più voci di suo interesse dall’elenco di pagine risultato della ricerca e richiede di scaricarle in
locale in formato PDF. Il sistema assembla le pagine selezionate in un unico fascicolo pronto per essere scaricato ed
attiva la funzionalità di salvataggio in locale.
Il PDF generato non permane nel sistema e viene cancellato appena l’utente esegue altre operazioni o termina la
navigazione.
Solo se l’utente è privilegiato, può richiedere al sistema il salvataggio del fascicolo (inteso come salvataggio dei
contenuti atti a generare il PDF scaricabile) per futuri ampliamenti.
Il fascicolo viene mantenuto dal sistema finché l’utente non decide di eliminarlo esplicitamente e può essere recuperato
successivamente per essere stampato o scaricato in locale.

#### 1.2.9 Scenario 8 – Ottemperare ad una richiesta di oblio

1.2.9.1 Descrizione

Il sistema, avuto riscontro da CRP che la richiesta di diritto all’oblio è stata accolta, procede ad espungere le
informazioni personali obsolete in riferimento a fatti od a eventi passati che possono recare danno alla reputazione del
soggetto richiedente. Nei casi in cui il diritto alla privacy abbia prevalenza sulla libertà di espressione e informazione,
si procede anche ad oscurare adeguatamente tali informazioni personali in modo che non siano recuperabili
indirettamente.

```
Esigenza da indirizzare Collegare in un documento più notizie di interesse
Obiettivi Come utente voglio mettere insieme più informazioni frutto di
ricerche sul sistema per poterle scaricare in locale
Stakeholders coinvolti Utente web, utente privilegiato
Scenari di business coinvolti Scenario 6
```
```
Esigenza da indirizzare Adempiere alla richiesta di oblio ai sensi del GDPR
Obiettivi Come cittadino voglio far valere il diritto all’oblio in modo che il mio
nome non compaia a seguito di specifiche ricerche.
Stakeholders coinvolti Utente richiedente, CRP
Scenari di business coinvolti
```

**Vista d'insieme**

```
GdP--VDI- 01 - V0 3
Pag. 7 di 14
```
### 1.3 MODELLO DEI DATI

La descrizione completa del modello dei dati è riportata nel file _GdP-STD- 01 - Vnn-ModelloDati_ ultima versione
disponibile.

### 1.4 ARCHITETTURA APPLICATIVA

Il diagramma di deploy di fig.1 esemplifica il dispiegamento della nuova piattaforma dei materiali editoriali, basata
su un nuovo servizio di archiviazione a lungo termine delle informazioni digitali.


**Vista d'insieme**

```
GdP--VDI- 01 - V0 3
Pag. 8 di 14
```
```
Figura 1
```

**Vista d'insieme**

```
GdP--VDI- 01 - V0 3
Pag. 9 di 14
```
La soluzione è costituita da:

- un sistema di archiviazione del materiale digitalizzato, “LIBRA” (DAM);
- un sottosistema di orchestrazione (“Orchestratore GDP”) che si interfaccia con il server sFTP, il Database e
    i servizi di “LIBRA”;
- un modulo “batch” (Batch Scheduler) che si occupa di attivare l’acquisizione delle edizioni trasmesse via
    FTP, della creazione dell’XML per ogni edizione;
- un sottosistema di back-office (SPA BACK OFFICE) per il monitoraggio delle acquisizioni del materiale
    digitale trasmesso via FTP;
- un sottosistema di front-office (SPA FRONT OFFICE) che permette all’utente internet la ricerca in archivio.

Nella figura 2 è rappresentato il diagramma delle componenti.

```
Figura 2
```
I volumi e il livello di servizio indicati nelle tabelle seguenti si riferiscono alla componente di front end.


**Vista d'insieme**

```
GdP--VDI- 01 - V0 3
Pag. 10 di 14
```
```
Tipo volume Cardinalità
Massimo numero di utenti del sistema (utenti potenziali) N.A.
Numero di utenti giornalieri che accedono al sistema 500
Massimo numero di utenti contemporanei previsti dal sistema 30
Numero Enti/strutture organizzative interessate 5 (+ utenza internet)
```
```
Ente H24 per 7 giorni Stagionalità Livello di Servizio
Consiglio regionale del
Piemonte
```
```
NO n.a n.a
```
#### 1.4.1 Funzionalità

```
Id Nome funzionalità Descrizione funzionalità Riferimento
requisito
```
```
Riferimento
scenario di
business (se
applicabile)
FunA 01 Autenticazione Per accedere al sistema, l’attore deve
fornire le credenziali che possono
essere:
```
- Credenziali RUPAR o di posta
    CRP per l’utente privilegiato
    (Front-End)
- Credenziali CSI per l’operatore
    di servizio (Back-Office)
- Credenziali sFTP, fornite da CSI
    per il fornitore di edizioni attive
- Credenziali sFTP, fornite da CSI,
    per fornitore di pubblicazioni
    storiche

##### 1 , 3, 4, 5, 6

```
FunA02 Nuova testata Censire nel sistema una nuova testata,
registrandone le informazioni
anagrafiche e di configurazione
```
##### 2

```
FunA03 Aggiorna testata Modificare alcune informazioni della
testata (descrizione, indirizzo, stato,...)
```
##### 2

```
FunA04 Sospendi testata Indicare il periodo di sospensione delle
pubblicazioni periodiche per le testate
“attive”
```
##### 5

```
FunA05 Modifica Periodo Cambiare la periodicità di uscita per le
testate “attive”
```
##### 3, 5

```
FunB01 Nuovo utente Censire un nuovo soggetto abilitato ad
accedere al sistema in base al ruolo
(utente privilegiato, utente sFTP)
```
##### 1, 3, 4, 5, 6

```
FunB02 Aggiorna utente Modifica di alcune informazioni
dell’utente (ruolo, stato)
```
##### 1

FunC01 Ricerca semplice (^) Ricercare uno o più lemmi su tutto
l’archivio

##### 6, 7


**Vista d'insieme**

```
GdP--VDI- 01 - V0 3
Pag. 11 di 14
```
```
Id Nome funzionalità Descrizione funzionalità Riferimento
requisito
```
```
Riferimento
scenario di
business (se
applicabile)
FunC02 Ricerca avanzata Ricercare uno o più lemmi con specifica
delle modalità (AND, OR, EXACT)
limitazione di ambito (filtri luogo, tempo,
tematismo,...)
```
##### 6, 7

```
FunC03 Esplora catalogo Ottenere l’elenco testate in archivio in
base a:
```
- Provincia
- Attive/non attive

```
Seleziona una testata specifica in base al
nome
```
##### 6

```
FunC0 4 Sfoglia edizione Rica vare l’elenco delle edizioni di una
testata, eventualmente per intervallo
temporale
```
##### 6, 7

```
FunD01 Gestione fascicolo Creare un fascicolo con una o più pagine
della ricerca, modificarlo, scaricarlo in
locale, eliminarlo e, se il ruolo lo
consente, salvarlo.
```
##### 7

```
FunD02 Gestione preferiti Creare e modificare un elenco di testate
o edizioni preferite. Funzionalità
riservata ad utente privilegiato.
```
##### 7

```
FunE01 Oblio Intervenire su specifiche pagine per
modifiche in ottemperanza a richieste
riconosciute di tutela della privacy.
Funzionalità riservata ad utente di
servizio.
```
##### 8

#### 1.4.2 Applicativi associati al progetto

```
Applicativo Componente applicativa Tipologia (Esistente/nuovo)
GdP gdpfospa nuovo
GdP gdpbospa nuovo
GdP gdporch nuovo
GdP gdpbff nuovo
```
#### 1.4.3 Dipendenze e criticità applicative della soluzione

Il sistema GdP si basa in modo sostanziale sull’accesso e la consultazione dei servizi Libra (DAM). Eventuali
problematiche temporanee su questa piattaforma potrebbero avere impatto sull’utilizzo del sistema GdP.


**Vista d'insieme**

```
GdP--VDI- 01 - V0 3
Pag. 12 di 14
```
#### 1.4.4 Sintesi della soluzione tecnica

**Soluzione Tecnica Sì/No**^ **Motivazioni**^

**Arricchimento
funzionale del
software
applicativo
esistente**

```
Sviluppo interno No^
```
```
Nuovo
oggetto che si
integra con
applicativo
esistente
```
**Riuso** No (^)
**Acquisto
SW di
mercato**
No
**Sviluppo di
nuovo prodotto
o sostituzione
completa di un
prodotto già
esistente
Sviluppo Interno** SI
Superamento del lock-in tecnologico e adozione
di nuova piattaforma opensource per
l’archiviazione di materiale multimediale digitale
**Riuso** No
**Acquisto SW di mercato**^ No^

### 1.5 ARCHITETTURA TECNOLOGICA

#### 1.5.1 Standard da soddisfare

```
Standard Contesto di utilizzo
Framework Quarkus 3.x Accesso a DB relazionale, accesso ai dati
Business Logic
Gestione delle transazioni
Implementazione servizi Rest
IoC
Angular 19.x Implementazione Rich Internet Application
```
#### 1.5.2 Pila tecnologica

```
Nome System
Software
```
```
Versione Facente parte di pila
tecnologica CSI di
riferimento (SI/NO)
```
```
Imposto da riuso
/ acquisto (SI/NO)
```
```
(opzionale)
numero server
coinvolti(*)
quarkus 3.x +
Adoptium Temurin
17 su K8s
```
##### 3 SI NO

```
Apache WS su K8s 2.4 SI NO
PostgreSQL
community
```
##### 15 SI NO

(*) utile per un dimensionamento dell’infrastruttura e relativi costi coinvolgendo Piattaforme/Datacenter


**Vista d'insieme**

```
GdP--VDI- 01 - V0 3
Pag. 13 di 14
```
## 2. APPENDICE 1: GLOSSARIO DEI TERMINI DELLA VISTA D’INSIEME

```
Termine Definizione
Program Management Gestione coordinata di un portafoglio di progetti per raggiungere un insieme di
obiettivi. Questa definizione non si applica solo in senso aziendale, dove gli
obiettivi sono tipicamente di business, ma anche in altri contesti. Si pensi alla
ricostruzione di una città dopo una guerra, programma che comprende diversi
progetti quali ad esempio il progetto per la ricostruzione della rete idrica, il
progetto per la costruzione delle strade, il progetto per la costruzione delle
scuole: ogni progetto porta il suo contributo al raggiungimento degli obiettivi del
programma. Il program management è quindi una visione simultanea dei progetti
in corso. [Wikipedia]
Programma Un programma è l’insieme di più progetti, la cui responsabilità è potenzialmente
assegnata a Project Manager diversi. I progetti devono essere convergenti rispetto
agli obiettivi del programma. Se un programma ha l’obiettivo di mettere a
disposizione uno strumento informatico a supporto di uno o più servizi di
business del cliente è necessario suddividere il programma in più progetti, uno
per la realizzazione del prodotto software, uno per la progettazione del servizio a
seguito della messa a disposizione del prodotto, e se necessario, più progetti
collegati o correlabili ai primi due (es. progetto di evoluzione di una piattaforma
tecnologica o di introduzione di una nuova tecnologia).
Program
Manager/Program Owner
```
```
Collabora attivamente con i project manager ed è responsabile della
pianificazione complessiva e del controllo di uno o più progetti. L'attività di cui è
titolare, come si può intuire dal nome, si chiama programma. Il programma è una
struttura definita al fine di gestire una serie di progetti. Il ruolo del Program
Manager non richiede una profonda conoscenza dei dettagli tecnici del progetto
di cui è responsabile; egli deve piuttosto essere in grado garantire il corretto
allineamento tra le aspettative di business ed i requisiti tecnici di
implementazione. I principali compiti svolti dal Program Manager sono: a)
controllare l'andamento dell'intero progetto, b) collaborare con i responsabili dei
filoni progettuali nella preparazione e presentazione dello Stato Avanzamento
Lavori (SAL), c) presiedere le riunioni con i responsabili degli stream di progetto
e deliberare le azioni da intraprendere. Tra le altre attività svolte tipiche del
program management possiamo citare: 1) La gestione centralizzata della
documentazione di progetto, 2) La preparazione degli incontri e delle
presentazioni per comunicare i risultati allo Steering committee, 3) La
predisposizione e pianificazione delle stime per le fasi progettuali successive.
[Wikipedia]
Ente Il nome e la descrizione dell’Ente di cui si descrive l’Enterprise Architecture
Principio È la regola di carattere generale e stabile nel tempo che indirizza il modo di
operare dell’Ente e le conseguenti scelte architetturali
Stakeholder Sono le persone o le organizzazioni direttamente interessate o coinvolte dai
risultati
Capability Sono le competenze che l’ente deve avere per soddisfare le deleghe
amministrative attribuitegli. Possono essere semplici o gerarchicamente
articolate.
Unità Organizzativa Sono gli elementi utilizzati per descrivere gli organigrammi degli enti articolati
secondo una alberatura gerarchica.
```

**Vista d'insieme**

```
GdP--VDI- 01 - V0 3
Pag. 14 di 14
```
**Obiettivo** Sono il risultato organizzativo o amministrativo che l’ente si prefigge di
raggiungere

**Strategia** Sono le azioni finalizzate al raggiungimento degli obiettivi.

**Servizio di business** Sono l’insieme delle attività svolte dall’Ente al fine di dare attuazione ad una
propria capability e rispondere ad una esigenza interna o esterna all’ente secondo
quanto previsto amministrativamente per l’ente stesso. Possono essere semplici o
gerarchicamente articolati.

**Processo di business** Espressi secondo la notazione BPMN 2.0, descrivono le modalità di attuazione di
un determinato servizio di business come una successione articolata di attività,
dati e documenti di dettaglio.

**Entità Concettuale** Definiscono le entità espresse con l’obiettivo di descrivere la realtà in modo
esaustivo sebbene semplificato. Sono utilizzare per rappresentare i cosiddetti
modelli concettuali dei dati.

**Riferimento entità fisica** Si intende il nome e le informazioni utili a reperire i dati direttamente su DB
relazionali e non relazionali.

**Applicativo** È l’accezione con cui l’ente qualifica il software utilizzato per assolvere ai propri
compiti

**Componente di
Applicativo**

```
È la scomposizione logica dell’applicativo finalizzata alla suddivisione
funzionale o tecnologica dell’applicativo stesso nel caso fosse necessario per
applicativi particolarmente complessi
```
**Prodotto** Prodotto, ovvero l’accezione software dell’applicativo, o meglio ancora della
componente di applicativo cui verrà relazionata all’atto della messa in esercizio

**Componente di Prodotto** Sono la scomposizione logica di un prodotto software in componenti software di
dettaglio

**Unità di Installazione** Sono la rappresentazione dei pacchetti software di installazione sui vari server

**System Software** I software che compongono la pila tecnologica su cui il nodo è definito


