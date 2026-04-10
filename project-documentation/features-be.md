GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 1 di 37
GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Versione 3.
VERIFICHE E APPROVAZIONI
VERS.
REDAZIONE
CONTROLLO
APPROVAZIONE
AUTORIZZAZIONE
EMISSIONE
NOME DATA NOME DATA NOME DATA
1 U. Mandosio 26/02/2026 P. Galliano 02/03/2026 P. Galliano 02/03/
2 U. Mandosio 13/03/2026 P. Galliano 13/03/2026 P. Galliano 13/03/
3 U. Mandosio 27 /03/2026 P. Galliano 27/03/2026 P. Galliano 27/03/
STATO DELLE VARIAZIONI
VERS. PARAGRAFO O
PAGINA
DESCRIZIONE DELLA VARIAZIONE
1 Tutto il documento Versione iniziale delle specifiche dei servizi
2 Operazione F0 4
Operazione F04, F
Operazione F
Operazioni F06, F07, F
Aggiunto salvataggio campo PATH_EDIZIONE
Rinominato parametro di input/output nomeTestata → cartellaTestata
Modifiche varie evidenziate in azzurro
Nuove
3 Operazioni F12, F13, F14, F15,
F20, F21, F
Nuove
GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi

- Pag. 2 di Servizi_BackEnd
  Introduzione Sommario
  Scopo del documento
  Riferimenti
  Servizio: FTPregolare
  Operazione: F01 - FTPregolare.configDTEdizioneAttesa
  Operazione: F02 - FTPregolare.creaCartellaEdizioneAttesa
  Operazione: F03 - FTPregolare.checkEdizioneAttesa
  Operazione: F04 - FTPregolare.ctrlEdizioneAcquisita
  Operazione: F05 - FTPregolare.sospensioneEdizioneAttesa
  Operazione: F18 - FTPregolare. verifDateAttese
  Servizio: FTPsaltuario
  Operazione: F06 - FTPsaltuario.checkConsegnaStorico
  Operazione: F07 - FTPsaltuario.ctrlEdizioniStoriche
  Servizio: DB
  Operazione: F08 - DB.insEdizione
  Operazione: F16 - DB.getElencoTestate
  Operazione: F17 - DB.getTestata
  Servizio: DAMtrasmissione
  Operazione: F09 - DAMtrasmissione.creaXMLEdizione
  Operazione: F10 - DAMtrasmissione.inviaEdizione
  Operazione: F19 - DAMtrasmissione.pulisciEdizione
  Servizio: MONITOR
  Operazione: F12 - MONITOR.elencoAcquisizioni
  Operazione: F13 - MONITOR.dettaglioAcquisizione
  Operazione: F14 - MONITOR.preparaMAIL
  Operazione: F15 - MONITOR.ricercaAcquisizioni
  Operazione: F20 - MONITOR.statoDAM
  Operazione: F21 - MONITOR.attivaCODA
  Operazione: F22 - MONITOR.invioMAIL
  Problemi aperti
  GDP – GIORNALI DEL PIEMONTE
  Specifica dei servizi
  Servizi_BackEnd
  Pag. 3 di 37
  Introduzione Sommario
  Scopo del documento
  Il documento descrive le operazioni necessarie alla gestione delle azioni relative all’acquisizione di nuove edizioni da
  flusso regolare o storico ed alla loro trasmissione al DAM.

Riferimenti
Num. Riferimento
1 GdP--SRS- 01 - Vnn_Specifica-dei-requisiti-del-sistema [ultima versione]
Servizio: FTPregolare
Obiettivo

Il servizio elenca e descrive tutte le operazioni necessarie a gestire il processo di acquisizione delle edizioni inviate
periodicamente.

Operazione: F01 - FTPregolare.configDTEdizioneAttesa
Scopo del servizio

Calcolare per ogni testata attiva tutte le date delle edizioni attese per il periodo ricevuto in input, eventualmente per la
sola testata specificata, e salvarle sul DB [Tabella DATA_USCITA].
Modalità di richiamo
Il servizio è richiamato in modalità “asincrona” dal BE applicativo.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 dataInizio Formato yyyy-mm-dd Dt Obb
2 dataFine Formato yyyy-mm-dd Dt Obb
3 idTestata Se il parametro non è valorizzato si considera il
vincolo GDP_TESTATA. INVIO_EDIZIONI = 1
I NON Obb
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG 0000 x.
S
1 Anno Parametro di input I
2 Un elenco di “”
GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 4 di 37
(l’elenco contiene un’occorrenza per ogni distinta testata)
2 .1 idTestata Identificativo univoco della testata attiva I
2 .2 nroEdizioniAttese Numero di edizioni attese calcolate N
Dettaglio passi operazione

Il sistema legge la data iniziale e quella finale del periodo per cui deve effettuare e l’eventuale identificativo
della testata per cui procedere.
Il sistema accede al DB e recupera l’identificativo univoco e il nome della testata indicata in input ovvero di tutte
le testate invianti [GDP_TESTATA. INVIO_EDIZIONI = 1];
Il calcolo procede per ciascuna testata individuata, distinguendo due modalità distinte:
A. la periodicità della testata è ‘multipla’ del mese (12, 6, 4, 3, 2, 1, 0.5^1 volte all’anno);
B. la periodicità è inferiore al mese.
Caso A [GDP_PERIODICITA.MENSILITA > 0 ]
Nel campo GDP_PERIODICITA.GG_PERIODICITA è riportato il giorno (i giorni) del mese di uscita del periodico e
può essere definito in due forme: G nn ; G n S m.

Nel primo caso è indicato direttamente il numero del giorno del mese:
NULL → l'editore deve fornire elenco da caricare (vedi Problema 1)
G00 → giorno di uscita non definito (come default primo del mese)
G01 → giorno di uscita: primo del mese
G12 → giorno di uscita: 12 del mese
G01;G15 → giorno di uscita: 1 e 15 del mese (se MENSILITA = 0,5)
Nel secondo caso è espresso come ordinale (Gn) del giorno della settimana (Sm):
G1S6 → giorno di uscita: primo sabato del mese
G3S5 → giorno di uscita: terzo venerdì del mese

Indicazioni di calcolo

trovo il primo giorno di uscita MM 0 a partire dal 1° gennaio in base all’indicazione in
GG_PERIODICITA
ricavo le uscite successive dall’indicazione in GDP_PERIODICITA.MENSILITA ponendo
MMi+1 = MMi + MENSILITA
il calcolo termina quando l’uscita successiva cade oltre l’intervallo di tempo indicato in input.
Caso B [GDP_PERIODICITA.MENSILITA = 0 ]
Nel campo GDP_PERIODICITA.GG_PERIODICITA e riportato il giorno (i giorni) della settimana di uscita del
periodico ed è definito nella forma n WS m: è espresso come numero di settimane al mese (nW) e giorno della settimana

(^1) 0,5 = bimensile o quindicinale

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 5 di 37
(Sm).
1WS0 → giorno di uscita: tutti i giorni
1WS3 → giorno di uscita: mercoledì di ogni settimana
1WS1;1WS4 → giorno di uscita: lunedì e giovedì di ogni settimana
2WS7 → giorno di uscita: domenica ogni due settimane

Indicazioni di calcolo

trovo il primo giorno di uscita a partire dal 1° gennaio in base all’indicazione in GG_PERIODICITA
ricavo le uscite successive scomponendo l’indicazione in GG_PERIODICITA
a. “ ; ” → separa il numero di uscite a settimana
b. nW → intervallo in settimana delle uscite
il calcolo termina quando l’uscita successiva cade oltre l’intervallo di tempo indicato in configurazione.
Terminato il calcolo (caso A o caso B), il sistema inserisce nella tabella GDP_DATA_USCITA un’occorrenza per ogni
uscita determinata di ciascuna testata:

➢ FK_TESTATA → identificativo della testata
➢ ANNO → parametro di input
➢ DATA_ATTESA → data uscita calcolata
➢ SOSPESA → “NULL”
Nota
Per i giorni della settimana assumiamo:
S0 = quotidiano
S1 = lunedì
S2 = martedì
S3 = mercoledì
S4 = giovedì
S5 = venerdì
S6 = sabato
S7 = domenica

Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione OK
MSG00001 Il servizio non ha trovato occorrenze per i parametri di input
MSG00002 La testata attiva {ID} non ha MENSILITA definita
MSG00003 La testata attiva {ID} non ha GG_PERIODICITA definita

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 6 di 37
Operazione: F02 - FTPregolare.creaCartellaEdizioneAttesa
Scopo del servizio

Creare la cartella destinata a ricevere l’edizione attesa il giorno successivo, per le testate che lo prevedono.
Modalità di richiamo
Il servizio è richiamato in modalità “asincrona” ogni giorno dopo le 20:00.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 NA
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
1 NA (^)
Dettaglio passi operazione
Il processo accede alla tabella GDP_DATA_USCITA e seleziona le occorrenze non sospese per l’anno corrente e
DATA_ATTESA uguale alla data successiva alla data corrente di elaborazione [sysdate+1].
Se non vi sono occorrenze, l’elaborazione termina con MSG
Da ciascuna occorrenza ricava l’identificativo della testata e, accedendo alla tabella GDP_TESTATA, legge il contenuto
del campo CARTELLA_TESTATA corrispondente, con cui identificare la “home” nell’area sFTP.
Infine, nell’area sFTP, sotto la home per la testata (definita in GDP_UTENTESFTP.HOME_FTP ) crea la cartella nel
formato “ yyyy-mm-dd ”.
Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione OK
MSG00001 Il servizio non ha trovato occorrenze per i parametri di input

Operazione: F03 - FTPregolare.checkEdizioneAttesa
Scopo del servizio

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 7 di 37
Verificare la presenza di un’edizione attesa consegnata.

Modalità di richiamo
Il servizio è richiamato in modalità “asincrona” ogni 15 minuti.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 NA
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
1 NA
Dettaglio passi operazione
Il processo scansiona tutte le cartelle sotto la home applicativa sFTP [//flusso_regolare] per controllare l’arrivo
di una nuova edizione.
Se non trova nulla termina senza ulteriori operazioni con MSG
Altrimenti è stata trovata un’edizione in [//flusso_regolare//] e il processo
per ogni file presente:
➢ verifica che il trasferimento del file sia completato
→ Non ci sono metodi che assicurino con certezza il completamento della scrittura di un file da parte del
processo FTP, quindi si può:
✓ verificare ogni 15” la dimensione del file per 3 minuti → se costante il trasferimento è completato!
✓ (in Linux) adottare la soluzione col comando “rsybc” in sede di spostamento:
rsync - av -e ssh original/ username@remote_host:/path/to/destination/
➢ imposta con il timestamp dell’ultimo (in ordine cronologico) file presente nella cartella
in formato yyyy-mm-dd HH:MM:SS
➢ ricava da DB l’identificativo univoco della testata .
A tal fine accede a GDP_TESTATA e seleziona l’occorrenza con CARTELLA_TESTATA =
.
Se trova più di un’occorrenza:
✓ sposta i file pdf in [//_errata//]
✓ inserisce un record nella tabella GDP_LOG, valorizzando:
▪ FK_GDP_TESTATA = 0
▪ TIPO_ACQUISIZIONE = “G”

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 8 di 37
▪ DT_ACQUISIZIONE =
▪ GDP_LOG.ESITO = MSG0000 2
✓ nessun record nella tabella GDP_LOG_EDIZIONE
✓ interrompe l’elaborazione dell’edizione corrente
✓ prosegue la scansione delle cartelle
➢ imposta GDP_LOG.FK_GDP_TESTATA =
➢ sposta l’intera edizione in //\_tmp//
➢ conta in i file in [//_tmp//]
➢ inserisce al posto del file spostato un file con lo stesso nome ed estensione “OK”
➢ inserisce un record nella tabella GDP_LOG, valorizzando:
▪ FK_TESTATA con ID testata in elaborazione
▪ TIPO_ACQUISIZIONE = “G”
▪ DT_ACQUISIZIONE =
▪ TOTALE_FILE_ACQUISITI =
➢ attiva in modalità asincrona il processo F05, passando come parametri , ,
,
➢ prosegue nella scansione di //flusso_regolare
Al completamento della scansione della cartella //flusso_regolare il processo termina con MSG

Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione OK
MSG00001 Nessuna nuova edizione trovata
MSG00002 Anomalia UNICITA’ TESTATA
Per dell’edizione sono stati trovati: {elenco ID}
I file
{elenco completo dei nomi file PDF}
sono stati spostati in //\_errata//

Operazione: F04 - FTPregolare.ctrlEdizioneAcquisita
Modalità di richiamo
Il servizio è richiamato in modalità “asincrona” da altro processo.
Descrizione dell’operazione

Interfaccia di richiamo (input)

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 9 di 37

# Parametro Regole di composizione Tipo Obbl

1 Testata Corrisponde a ID_TESTATA sul DB I Obb
2 cartellaTestata Corrisponde a CARTELLA_TESTATA su DB Stringa Obb
3 dataEdizione Formato yyyy-mm-dd Dt Obb
4 log Corrisponde a ID_LOG su DB I Obb
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
1 NA
Dettaglio passi operazione
Il processo si posiziona in [//_tmp//]
➢ verifica rispetto ai valori definiti sul DB per la testata.
A tal fine accede a GDP_PERIODICITA e seleziona l’occorrenza con FK_GDP_TESTATA = ,
ricava ID_GDP_PERIODICITA e su GDP_DATA_USCITA verifica che esista e non sia sospesa l’occorrenza
con:
▪ FK_GDP_PERIODICITA = <ID_GDP_PERIODICITA>
▪ ANNO =
▪ DATA_ATTESA =
➢ imposta come segue:
✓ se = data di elaborazione → = “OK”;
✓ se > data di elaborazione → = “AT” [edizione “anticipataria”];
✓ se < data di elaborazione → = “PT” [edizione “posticipataria”];
✓ se presente su DB ma SOSPESA=True → = “SO”
✓ se non presente su DB → = “AA” [edizione “anomala”]
Se = “AA”:
▪ sposta l’edizione ed i file in essa contenuti in
[//_errata//]
▪ GDP_LOG.ESITO = MSG0000 1
▪ termina l’elaborazione con MSG0000 1
per ogni PDF:
➢ accerta che il file non sia un PDF multi-pagina.
Altrimenti, provvede alla separazione in file con pagina singola (split) e riporta “NP – <nomedelfile.pdf>” in
, senza aggiornare il conteggio dei file

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi*BackEnd
Pag. 10 di 37
➢ controlla che il file sia un PDF/A leggibile, ad esempio utilizzando uno dei seguenti strumenti:
utilizzo “pdfinfo fileedizione.pdf”
utilizzo “pdfcpu validate fileedizione.pdf”
utilizzo “gs -dNOPAUSE -dBATCH -sDEVICE=nullpage fileedizione.pdf”
✓ file non leggibile
▪ sposta il file in [/<rootFTP>/_errata/<cartellaTestata>/<dataEdizione>]
▪ riporta “NL – <nomedelfile.pdf>” in <Descrizione>
➢ verifica che il file abbia denominazione standard:
nomefile_numpagina.pdf
dove:
▪ nomefile è qualsiasi sequenza di caratteri alfanumerici escluso '*'
▪ numpagina deve essere scritto con 3 cifre 001
▪ .pdf estensione del file
✓ formato corretto
▪ rinomina il file in “ cartellaTestata-dataEdizione_numeroPagina.pdf ”
✓ formato errato
▪ sposta il file in [/<rootFTP>/_errata/<cartellaTestata>/<dataEdizione>]
▪ riporta “NF – <nomedelfile.pdf>” in <Descrizione>
➢ estrae il testo e crea un file TXT per ogni PDF con lo stesso nome [suggerimento: ottimi risultato con tool
pdftotext ]
➢ applica i seguenti controlli euristici di corrispondenza [i controlli euristici non assicurano che l’edizione
corrisponda alla data attesa o la prima pagina sia correttamente indicata, fornendo solo un ragionevole
riscontro]:
DATA ATTESA → Estrarre dal TXT di pag1 tutte le date con una RegEx^2 che individui:
▪ Il giorno (una o due cifre), con eventuale carattere “°”
▪ Il separatore compreso in “-“, “/”, “.”, “[[spazio]]”
▪ Il mese in cifre, in parola italiana completa o abbreviata con 3 lettere
▪ L’anno su quattro cifre con le due inziali ristrette a “18,19,20”
confrontare la DATA ATTESA con l’elenco estratto, per verificare se presente
✓ se DATA non corrisponde riporta “DA – <nomedelfile.pdf>” in <Descrizione>
PRIMA PAGINA → Individuare nel TXT di pag1 una parola chiave tra quelle nell’elenco ["abbonamento”,
“direzione”, “direttore”, “redazione”, amministrazione"] che compaiono di regola sulla prima pagina di un
(^2) Ad esempio con la regola
DATE_REGEX=r"((^|[\s])(\d{1,2})°?(?P[\s-
/.])(\d{1,2}|gen(naio)?|feb(braio)?|mar(zo)?|apr(ile)?|mag(gio)?|giu(gno)?|lug(lio)?|ago(sto)?|set(tembre)?|ott(obre)?|no
v(embre)?|dic(embre)?)(?P=sep)(18\d{2}|19\d{2}|20\d{2}))"

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 11 di 37
giornale
✓ se PRIMA PAGINA non confermata riporta “PP – <nomedelfile.pdf>” in <Descrizione>
Al termine dei controlli vengono salvate sul DB le informazioni utili ad alimentare il sistema di monitoraggio
applicativo; a tal fine il sistema:

➢ conta il numero di file PDF sotto//\_tmp// e ricava
➢ conta il numero di file PDF sotto//\_errata// e ricava
➢ calcola = < nroFileOK > + < nroFileKO>
➢ inserisce un record su GDP_LOG_EDIZIONE impostando:
▪ TIPO_EDIZIONE =
▪ PATH_EDIZIONE = //\_tmp//
▪ NRO_PAG_ACQUISITE = < nroFileED>
▪ NRO_PAG_VALIDE = < nroFileOK>
▪ NRO_PAG_ERRATE = < nroFileKO>
▪ PRIMA_PAGINA = True o False in base al controllo effettuato
▪ se non vuoto
o DESCRIZIONE =
o Modificare GDP_LOG.ESITO sostituendo a
➢ imposta = “//\_tmp//”
➢ attiva in modalità sincrona il processo F08, passando come parametri , , ,
e attende l’esito.
✓ esito positivo → aggiorna il record su GDP_LOG_EDIZIONE impostando:
▪ FK_GDP_EDIZIONE = < IDEdizione > restituito da F
✓ esito negativo →
▪ sposta la cartella e il suo contenuto sotto

“//\_errata//”
▪ salva GDP_LOG.ESITO = MSG0000 2 sostituendo a [MSG_F08] il messaggio restituito da F
▪ conclude l’elaborazione con MSG0000 2
➢ attiva in modalità sincrona il processo F09, passando come parametri , , ,
<Priorità>= 0 e attende l’esito.
✓ esito positivo → richiama in modalità asincrona il processo F10 passando come parametri ,
e restituito da F09 e conclude l’elaborazione con MSG
✓ esito negativo →
▪ sposta la cartella e il suo contenuto sotto

“//\_errata//”
▪ salva GDP_LOG.ESITO = MSG0000 3 sostituendo a [MSG_F0 9 ] il messaggio restituito da F0 9
▪ conclude l’elaborazione con MSG0000 3
GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 12 di 37
Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione OK
MSG00001 Anomalia DATA EDIZIONE
L’edizione della testata - ha
un’anomalia bloccante sulla data
MSG00002 <E00 3 >Anomalia EDIZIONE - [MSG_F08]<E00 3 >
Non è stato possibile inserire sul DB l’edizione <data_edizione> della testata

- MSG0000 3 Anomalia EDIZIONE - [MSG_F09]<E00 4 >
  Si è verificato un errore nella creazione del file per la trasmissione al DAM
  dell’edizione della testata < idTestata > -

Operazione: F05 - FTPregolare.sospensioneEdizioneAttesa
Scopo del servizio

Sospendere l’acquisizione automatica delle edizioni per un periodo definito.
Modalità di richiamo
Il servizio è richiamato in modalità “sincrona” dalla console di monitoraggio.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 Testata ID_TESTATA I Obb
2 Data inizio Formato yyyy-mm-dd Dt Obb
3 Data fine Formato yyyy-mm-dd Dt Obb
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
1 giorniEdizione Numero di giorni di sospensione inseriti I
Dettaglio passi operazione

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 13 di 37
Il sistema legge dai parametri di input l’identificativo univoco della testata, l’intervallo di date e ricava dalla
tabella GDP_DATA_USCITA l’elenco delle date attese comprese.
Se l’elenco è vuoto restituisce MSG
▪ Altrimenti, sulla tabella GDP_PERIODICITA il sistema imposta:
➢ INIZIO_SOSPENSIONE = Data inizio da input
➢ FINE_SOSPENSIONE = Data fine da input
Per ID_TESTATA ricevuto in input e sulla tabella GDP_DATA_USCITA imposta
➢ SOSPESA = True, per l’elenco di date individuate al passo 1
Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione OK
MSG00001 Il servizio non ha trovato occorrenze per i parametri di input

Operazione: F18 - FTPregolare. verifDateAttese
Modalità di richiamo
Il servizio è richiamato in modalità “sincrona” da BE applicativo.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 dataInizio Formato yyyy-mm-dd Dt Obb
2 dataFine Formato yyyy-mm-dd Dt Obb
3 idTestata Se il parametro non è valorizzato si considera il
vincolo GDP_TESTATA. INVIO_EDIZIONI = 1
I Non Obb
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
2 Un elenco di “”
(l’elenco contiene un’occorrenza per ogni distinta testata)
1 idTestata I
2 cartellaTestata S
2.3 DataEdizioneAttesa Dt
GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 14 di 37
2.4 Sospesa B
Dettaglio passi operazione
Il servizio recupera “idTestata” e “nomeTestata dalla tabella GDP_TESTATA in coerenza con i parametri di input; per
ciascuna testata da considerare:
➢ seleziona l’occorrenza corrispondente su GDP_PERIODICITA [vincolo
ID_GDP_PERIODICITA=ID_GDP_TESTATA];
➢ seleziona le occorrenze di DATA_ATTESA e SOSPESA su GDP_DATA_USCITA [vincolo
ID_GDP_PERIODICITA=ID_GDP_TESTATA] comprese nell’intervallo DT_INIZIO= e
DT_FINE=
Se con i parametri di input non vengono rinvenute occorrenze il servizio restituisce MSG00001 ; altrimenti viene
restituito il risultato trovato.

Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione completata correttamente
MSG00001 Nessun risultato trovato

Servizio: FTPsaltuario
Obiettivo

Il servizio elenca e descrive tutte le operazioni necessarie a gestire il processo di acquisizione di edizioni storiche, inviate
ad intervalli indeterminati.

Operazione: F06 - FTPsaltuario.checkConsegnaStorico
Scopo del servizio

Verificare la presenza di una consegna di materiale storico e controlla che ciascuna cartella presente corrisponda ad
un’unica testata censita.

Modalità di richiamo
Il servizio è schedulato giornalmente, in orario serale.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 NA
GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 15 di 37
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
1 NA
Dettaglio passi operazione
Il processo scansiona tutte le cartelle sotto la home applicativa sFTP [//flusso_saltuario] per controllare l’arrivo
di una nuova consegna.
Se non trova nulla termina senza ulteriori operazioni con MSG
Altrimenti è stata trovata una consegna “ /CONS_yyyy-mm-dd ” in [//flusso_saltuario/< utenteStorico >] e il
processo:

➢ verifica che al suo interno vi sia un elenco di cartelle
➢ per ciascuna cartella presente:
✓ valorizza <cartellaTestata> = nome cartella corrente
✓ valorizza <dataAcquisizione> con il timestamp della cartella in formato yyyy-mm-dd HH:MM:SS
✓ valorizza <dataConsegna> con la data acquisizione in formato yyyy-mm-dd
✓ ricava da DB l’identificativo univoco della testata <IDTestata>.
A tal fine accede a GDP*TESTATA e seleziona l’occorrenza con CARTELLA_TESTATA =
<cartellaTestata>.
Se trova più di un’occorrenza:
▪ sposta il contenuto di <cartellaTestata> in
[/<rootFTP>/\_errata/< utenteStorico >/CONS*<dataConsegna>/<cartellaTestata>]
▪ conta il numero di file presenti nella cartella in <nroTotFile>
▪ inserisce un record nella tabella GDP*LOG con:
o FK_GDP_TESTATA = 0
o DT_ACQUISIZIONE = < dataAcquisizione >
o TOTALE_FILE_ACQUISITI = <nroTotFile>
o GDP_LOG.ESITO = MSG
▪ nessun record nella tabella GDP_LOG_EDIZIONE
▪ interrompe l’elaborazione della cartella corrente
▪ prosegue l’elaborazione con la cartella successiva
Se NON trova l’occorrenza:
▪ sposta il contenuto di <cartellaTestata> in
[/<rootFTP>/\_errata/< utenteStorico >/CONS*<dataConsegna>/<cartellaTestata>]
▪ conta il numero di file presenti nella cartella in <nroTotFile>
GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 16 di 37
▪ inserisce un record nella tabella GDP_LOG con:
o FK_GDP_TESTATA = 0
o DT_ACQUISIZIONE = < dataAcquisizione >
o TOTALE_FILE_ACQUISITI =
o GDP_LOG.ESITO = MSG
▪ nessun record nella tabella GDP_LOG_EDIZIONE
▪ interrompe l’elaborazione della cartella corrente
▪ prosegue l’elaborazione con la cartella successiva
✓ valorizza con il valore trovato
✓ sposta tutte le edizioni della testata in
//tmp/< utenteStorico >/CONS/
✓ inserisce un record nella tabella GDP_LOG, valorizzando:
▪ FK_TESTATA =
▪ DT_ACQUISIZIONE =
✓ attiva in modalità asincrona il processo F0 7 , passando come parametri , ,
,
➢ prosegue nella scansione di //<flusso_saltuario>
Al completamento della scansione della cartella //<flusso_saltuario>il processo termina con MSG

Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione OK
MSG00001 Nessuna nuova consegna di materiale storico
MSG0000 2 Anomalia UNICITA’
Per sono stati trovati: {elenco }
I file sono stati spostati in
[//errata/< utenteStorico >/CONS/]
MSG0000 3 Anomalia ESISTENZA
Per non è stato trovato ID_TESTATA
I file sono stati spostati in
[//errata/< utenteStorico >/CONS/]

Operazione: F07 - FTPsaltuario.ctrlEdizioniStoriche
Scopo del servizio

Controlla tutte le edizioni della singola testata storica.

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 17 di 37
Modalità di richiamo
Il servizio è richiamato in modalità “asincrona” dall’operazione F07.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 Testata Corrisponde a ID_TESTATA sul DB I Obb
2 cartellaTestata Corrisponde a CARTELLA_TESTATA su DB Stringa Obb
3 dataConsegna Formato yyyy-mm-dd Dt Obb
4 log Corrisponde a ID_LOG su DB I Obb
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
1 NA
Dettaglio passi operazione
Il processo si posiziona in //tmp/< utenteStorico >/CONS/ e per ogni
cartella di data edizione trovata:
➢ verifica che la data edizione sia nel formato previsto (cioè yyyymmdd con mm e dd valorizzati in coerenza):
✓ se formato corretto
▪ incrementa
▪ imposta = edizione corrente in formato “yyyy-mm-dd”
▪ imposta = “ST”
✓ se formato errato
▪ incrementa
▪ imposta = “AS”
▪ sposta la cartella e il suo contenuto sotto
/errata>/< utenteStorico >/CONS/
▪ salva (in aggiunta) = MSG0000 1
▪ inserisce un record nella tabella GDP_LOG_EDIZIONE con
o TIPO_EDIZIONE =
o PATH_EDIZIONE =
//tmp/< utenteStorico >/CONS//
o NRO_PAG_ACQUISITE = 0

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi*BackEnd
Pag. 18 di 37
o NRO_PAG_VALIDE = 0
o NRO_PAG_ERRATE = [vuoto]
o PRIMA_PAGINA = [vuoto]
o DESCRIZIONE = MSG0000 2
▪ passa all’edizione successiva
➢ verifica ciascun file contenuto nella cartella dell’edizione:
✓ denominazione PDF non conforme 3
▪ spostare il file sotto
<rootFTP>/\_errata>/< utenteStorico >/CONS*<dataConsegna>/<cartellaTestata>/<dataEdizio
ne>
▪ riporta (in aggiunta) <Descrizione> = “NF – <nomedelfile.pdf>”
✓ denominazione TXT non conforme 3
▪ spostare il file sotto
<rootFTP>/_errata>/< utenteStorico >/CONS_<dataConsegna>/<cartellaTestata>/<dataEdizio
ne>
▪ riporta (in aggiunta) <Descrizione> = “NF – <nomedelfile.txt>”
✓ denominazione TIF non conforme 3
▪ spostare il file sotto
<rootFTP>/_errata>/< utenteStorico >/CONS_<dataConsegna>/<cartellaTestata>/<dataEdizio
ne>
▪ riporta (in aggiunta) <Descrizione> = “NF – <nomedelfile.tif>”
✓ TIF non presente
▪ riporta (in aggiunta) <Descrizione> = “NF – file TIF mancante”
➢ conta il numero di file validi per tipologia [<nroPDFOKedizione >, <nroTXTok>, <nroTIFok>] sotto
▪ <rootFTP>/_tmp>/< utenteStorico >/CONS_<dataConsegna>/<cartellaTestata>/<dataEdizion
e>
➢ conta il numero di file non conformi per tipologia [<nroPDFko>, <nroTXTko>, <nroTIFko>] sotto
▪ <rootFTP>/_errata>/< utenteStorico >/CONS_<dataConsegna>/<cartellaTestata>/<dataEdizio
ne>
➢ calcola
▪ <nroPDFedizione> = <nroPDFOKedizione> + <nroPDFKOedizione>
▪ <nroTXTedizione> = <nroTXTOKedizione> + <nroTXTKOedizione>
▪ <nroTIFedizione> = <nroTIFOKedizione> + <nroTIFKOedizione>
➢ inserisce un record su GDP*LOG_EDIZIONE impostando:
▪ TIPO_EDIZIONE = <tipoEdizione>
▪ PATH_EDIZIONE =
/<rootFTP>/\_tmp/< utenteStorico >/CONS*<dataConsegna>/<nomeTestata>/<dataEdizione>
(^3) Il file deve avere denominazione coerente con la struttura

- -\_<nro_pag>.

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi*BackEnd
Pag. 19 di 37
▪ NRO_PAG_ACQUISITE = < nroPDFedizione >
▪ NRO_PAG_VALIDE = < nroPDFOKedizione >
▪ NRO_PAG_ERRATE = < nroPDFKOedizione >
▪ se <Descrizione> non vuoto
o DESCRIZIONE = <Descrizione>
➢ aggiorna
▪ <nroPDFOK> += <nroPDFOKedizione>
▪ <nroPDFKO> += <nroPDFKOedizione>
▪ <nroTXTOK> += <nroTXTOKedizione>
▪ <nroTXTKO> += <nroTXTKOedizione>
▪ <nroTIFOK> += <nroTIFOKedizione>
▪ <nroTIFKO> += <nroTIFKOedizione>
➢ imposta
<path> = “/<rootFTP>/\_tmp/< utenteStorico >/CONS*<dataConsegna>/<cartellaTestata>/<dataEdizione>”
➢ attiva in modalità sincrona il processo F08, passando come parametri <IDTestata>, <path>, <dataEdizione>,
<IDLog> e attende l’esito.
✓ esito positivo → aggiorna il record su GDP_LOG_EDIZIONE impostando:
▪ FK_GDP_EDIZIONE = < IDEdizione > restituito da F
✓ esito negativo →
▪ sposta la cartella e il suo contenuto sotto
“//\_errata//”
▪ salva (in aggiunta) <esito> = MSG0000 3 sostituendo a [MSG_F08] il messaggio restituito
da F
▪ passa all’edizione successiva
➢ attiva in modalità sincrona il processo F09, passando come parametri <IDTestata>, <IDLog>, <IDEdizione>,
<Priorità>= 10 0 e attende l’esito.
✓ esito positivo →
▪ richiama in modalità asincrona il processo F10 passando come parametri <IDLog>,
<IDEdizione>, e <nomeFileZIP> restituito da F
▪ procede con la successiva edizione
✓ esito negativo →
▪ sposta la cartella e il suo contenuto sotto
“//\_errata//”
▪ salva (in aggiunta) = MSG0000 4 sostituendo a [MSG_F0 9 ] il messaggio restituito
da F0 9
▪ procede con la successiva edizione
Terminata la verifica dell’ultima edizione della testata il processo:
➢ calcola

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 20 di 37
✓ <nroPDF> = <nroPDFOK> + <nroPDFKO>
✓ <nroTXT> = <nroTXTOK> + <nroTXTKO>
✓ <nroTIF> = <nroTIFOK> + <nroTIFKO>
➢ salva (in aggiunta) <esito> = MSG
➢ aggiorna GDP_LOG con ESITO = <esito>
➢ termina l’elaborazione
Codici di errore / messaggi restituiti
Codice Descrizione

MSG00009 (^) Elaborazione completata per la Testata -
Edizioni esaminate
file PDF di cui < nroPDFKO> scartati
file TXT di cui < nroTXTKO> scartati
file TIF di cui < nroTIFKO> scartati
MSG00001 - Edizione <data*edizione> con formato errato
spostata in < utenteStorico >/CONS*//
MSG0000 2 Edizione spostata in
< utenteStorico >/CONS\_//
MSG0000 3 Anomalia EDIZIONE - [MSG_F08]
Non è stato possibile inserire sul DB l’edizione <data_edizione> della testata -

MSG0000 4 Anomalia EDIZIONE - [MSG_F09]
Si è verificato un errore nella creazione del file per la trasmissione al DAM dell’edizione
della testata < idTestata > -

Servizio: DB
Obiettivo

Il servizio elenca e descrive tutte le operazioni necessarie a mantenere allineata la base dati applicativa con il sistema
esterno DAM.

Operazione: F08 - DB.insEdizione
Modalità di richiamo

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 21 di 37
Il servizio è richiamato in modalità “sincrona” da altro processo.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 idTestata Corrisponde a ID_TESTATA sul DB I Obb
2 Path Stringa Obb
3 dataEdizione Formato yyyy-mm-dd Dt Obb
4 log Corrisponde a ID_GDP_LOG su DB I Obb
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
1 Edizione ID_EDIZIONE I
Dettaglio passi operazione
Prima di procedere all’inserimento, il processo verifica se l’edizione è già esistente. A tal fine, accede alla tabella
GDP_EDIZIONE e ricerca l’occorrenza con FK_GDP_EDIZIONE = e DATA_EDIZIONE =
.
Se non viene rinvenuta alcuna occorrenza si procede con l’ “ inserimento ” di una nuova edizione per la testata; altrimenti
si procede con l’ ” aggiornamento ” dell’edizione di cui si mantiene in memoria l’identificativo.
Inserimento
Il processo si posiziona in , conta il numero di file “pdf” e lo salva in , quindi
➢ inserisce un record nella tabella GDP_EDIZIONE del DB:
▪ FK_TESTATA = ID_TESTATA
▪ DATA_EDIZIONE = ricevuta in input
▪ DATA_PUBBLICAZIONE = + gg(periodicità)^4

(^4) Alla data edizione va aggiunto un numero di giorni (arrotondato all’intero superiore) pari a due uscite in base alla
periodicità così definita:
'settimanale': 7
'bisettimanale': 3.5
'trisettimanale': 2.34
‘quadrisettimanale’: 1.75
'quotidiano': 1
'quattordicinale': 14

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi*BackEnd
Pag. 22 di 37
▪ STATO = 0
▪ NUMERO_PAGINE =
➢ inserisce per ciascun file PDF dell’edizione un record sulla tabella PAGINA del DB:
▪ FK_TESTATA = ID_TESTATA
▪ FK* EDIZIONE = ID*EDIZIONE
▪ NUM_PAGINA = numeroPagina del nome file
▪ FILE_PDF = nome file pdf (con estensione)
▪ FILE_TXT = nome file txt (con estensione)
▪ FILE_TIF = NULL
▪ DATA_EDIZIONE = EDIZIONE. DATA_EDIZIONE
▪ STATO = 0
▪ DATA_OBLIO = NULL
▪ OBLIO = NULL
▪ NOTA_OBLIO = NULL
Aggiornamento
Il processo si posiziona in , conta il numero di file “pdf”, salva il valore in e mantiene in memoria il
numeroPagina, quindi:
➢ aggiorna il record nella tabella GDP_EDIZIONE del DB se è diverso dal valore sul DB:
▪ NUMERO_PAGINE =
➢ inserisce un record sulla tabella PAGINA del DB per ciascun file PDF dell’edizione corrispondente ad una pagina
aggiunta, altrimenti aggiorna il record esistente (la distinzione viene operata in base al campo NUM_PAGINA):
▪ FK_TESTATA = ID_TESTATA
▪ FK* EDIZIONE = ID_EDIZIONE
▪ NUM_PAGINA = numeroPagina del nome file
▪ FILE_PDF = nome file pdf (con estensione)
▪ FILE_TXT = nome file txt (con estensione)
▪ FILE_TIF = NULL
▪ DATA_EDIZIONE = EDIZIONE. DATA_EDIZIONE
▪ STATO = 0
▪ DATA_OBLIO = NULL
▪ OBLIO = NULL

'quindicinale': 15
'mensile': 30
'bimestrale': 60
'trimestrale': 90
'quadrimestrale': 120

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 23 di 37
▪ NOTA_OBLIO = NULL
➢ termina l’elaborazione con l’output definito

Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione completata correttamente

MSG00001 (^) Errore inserimento su GDP_EDIZIONE
MSG0000 (^2) Errore inserimento su GDP_PAGINA

Operazione: F16 - DB.getElencoTestate
Modalità di richiamo
Il servizio è richiamato in modalità “sincrona” da BE applicativo.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 idTestata I parametri, se valorizzati, devono essere
alternativi e mutuamente esclusivi
I
2 invioEdizione S
3 Prov S
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
2 Un elenco di “Testata”
(l’elenco contiene un’occorrenza per ogni distinta testata)
1 idTestata Tabella GDP_TESTATA
2 NomeTestata
3 CartellaTestata
4 invioEdizione
5 Prov
Dettaglio passi operazione

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 24 di 37
Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione completata correttamente
MSG00001
MSG0000 2

Operazione: F17 - DB.getTestata
Modalità di richiamo
Il servizio è richiamato in modalità “sincrona” da BE applicativo.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 idTestata I Obb
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
2.x TUTTI Tabella GDP_TESTATA
Dettaglio passi operazione

Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione completata correttamente
MSG00001
MSG0000 2

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 25 di 37
Servizio: DAMtrasmissione
Obiettivo

Il servizio elenca e descrive tutte le operazioni necessarie a trasferire i file dell’edizione e i relativi metadati al sistema di
mantenimento e consultazione DAM.

Operazione: F09 - DAMtrasmissione.creaXMLEdizione
Modalità di richiamo
Il servizio è richiamato in modalità “sincrona” da altro processo.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 Testata Corrisponde a ID_TESTATA sul DB I Obb
2 idLog Corrisponde a ID_LOG su DB I Obb
3 IDEdizione Formato yyyy-mm-dd Dt Obb
4 Priorità 0 per edizioni giornaliere
100 per edizioni storiche
I Obb
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
1 Nome file compresso File compresso generato per l’edizione S
Dettaglio passi operazione
Per generare il contenuto del file XML per l’edizione il processo:
➢ accede alla tabella GDP_TESTATA del DB col parametro di input ID_TESTATA e recupera i dati anagrafici relativi
alla testata per compilare i metadati di <xs:element name="testata">
➢ accede alla tabella GDP_EDIZIONE e recupera l’occorrenza con ID_EDIZIONE = ed utilizza i dati
dell’edizione per compilare i metadati di <xs:element name="edizione">
➢ accede alla tabella GDP_PAGINA e recupera l’occorrenza con FK_GDP_EDIZIONE = idEdizione individuata al
passo precedente con le informazioni per compilare i metadati di <xs:element name="pagina"> di ciascuna pagina
➢ crea il file XML edizione.xml ..xml [data in formato yyyy-mm-dd] secondo lo
schema descritto da “ GdP-STD- 04 - V01-Validazione_metadati_flussoFTP.xsd ”
➢ comprime il file XML e tutti i file PDF e TXT dell’edizione nel file ..zip
➢ aggiorna in GDP_LOG_EDIZIONE l’occorrenza individuata con e , impostando:
✓ FILE_XML = True

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 26 di 37
➢ inserisce un record su GDP_IMPORT_TASK impostando:
✓ DATA_INSERIM_IN_CODA = data di sistema nel formato yyyy-mm-dd HH:MM:SS,CC
✓ FK_GDP_LOG_EDIZIONE = ID_GDP_LOG_EDIZIONE di GDP_LOG_EDIZIONE ricavato a partire da
passato come parametro
✓ NRO_TENTATIVO = 0
✓ DATA_TENTATIVO = NULL
✓ SFTP_PATH = “//\_dam/..zip”
✓ PRIORITA = <Priorità> passato come parametro
✓ STATO = “PRO”
➢ deposita il file compresso creato in “//\_dam” e restituisce con MSG00009 il nome del file creato
..zip
Se si verifica un errore nella creazione del file xml imposta GDP_LOG_EDIZIONE.FILE_XML=False per
l’occorrenza relativa a idLog ricevuto in input e restituisce MSG00002
Se si verifica un errore nella creazione del file compresso imposta GDP_LOG_EDIZIONE.FILE_ZIP=False per
l’occorrenza relativa a idLog ricevuto in input e restituisce MSG0000 3

Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione OK
MSG0000 2 ERRORE nella creazione del file XML
MSG0000 3 ERRORE nella creazione del file ZIP

Operazione: F10 - DAMtrasmissione.inviaEdizione
Pre-requisito

Per l’utilizzo dei servizi Libra occorre disporre di un token di autenticazione che va preventivamente generato.

La procedura di generazione del token a partire dalle chiavi pubblica e privata sarà descritta nel caso d’uso GDP-UC- 01 -
Autenticazione. Per le esigenze di test verrà fornito un token

Modalità di richiamo
Il servizio è schedulato ogni 30 minuti
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 idLog ID_LOG I Obb
2 Nome file edizione compresso File zip in formato Stringa Obb
GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 27 di 37
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
NA
Dettaglio passi operazione
Il processo accede alla tabella GDP_CODA_CARICAMENTO e seleziona tutte le occorrenze con STATO = “READY”
ordinandole per PRIORITA, DATA_INSERIM_IN_CODA.
Per ciascuna occorrenza trovata, legge il contenuto del campo SFTP_PATH e recupera il file compresso
con la sua collocazione; quindi, con la BASE URL http://ts-libra-sv-exp1.csi.it/rpcr02 [per ora si fa riferimento
all’ambiente di TEST] richiama il servizio “ /api/v2/imports ” per caricare il file sul DAM.
Il processo attende la risposta del servizio da cui preleva il valore dei seguenti campi:

"jobId”
"status”
Quindi
➢ Se “status” = “FAILED” →
✓ aggiorna il record su GDP_LOG_EDIZIONE impostando:
▪ JOB_ID = valore di "jobId”
▪ STATO = “FAILED”
✓ aggiorna il record su GDP_LOG impostando:
▪ GDP_LOG.ESITO = MSG0000 1
✓ conclude l’elaborazione con MSG00001
➢ Se “status” = “SUBMITTED” →
✓ aggiorna il record su GDP_LOG_EDIZIONE impostando:
▪ FILE_ZIP = True
▪ JOB_ID = valore di "jobId”
▪ STATO = “SUBMITTED”
✓ aggiorna il record su GDP_LOG impostando:
▪ GDP_LOG.ESITO = MSG00009
✓ cancella il file zip nella cartella //\_dam
✓ conclude l’elaborazione con MSG00009
Codici di errore / messaggi restituiti
Codice Descrizione

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 28 di 37
MSG00009 <MSG>Caricamento DAM eseguito<MSG>
MSG00001 <E00 5 >Anomalia EDIZIONE DAM<E00 5 >
La trasmissione del file <nomeFileZIP> al DAM per l’edizione <data_edizione> della
testata <IDTestata> - <cartellaTestata> è fallita
Operazione: F19 - DAMtrasmissione.pulisciEdizione
Modalità di richiamo
Il servizio è schedulato ogni 24 ore
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 idLog ID_LOG I Obb
2
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
NA
Dettaglio passi operazione
To BE DO

Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009
MSG00001

Servizio: MONITOR
Obiettivo

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 29 di 37
Il servizio elenca e descrive tutte le operazioni necessarie a verificare i caricamenti giornalieri e le consegne di materiale
storico.

Operazione: F12 - MONITOR.elencoAcquisizioni
Modalità di richiamo
Il servizio è richiamato in modalità “sincrona” dal back-end applicativo.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 TipoAcquisizione S Obb
2 dataAcquisizione Formato yyyy-mm-dd Dt Obb
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.

S
1 Un elenco di “ edizioni monitorate ”
1 idLog I
2 idUtenteSFTP I
3 idTestata I
4 nomeTestata S
5 nroEdizioni se tipoAcquisizione = ‘G’ è sempre 1
se tipoAcquisizione = ‘S’ è il conteggio delle occorrenze di
GDP_LOG_EDIZIONE per idLog
I
6 dataEdizione Dt
7 dataAcquisizione Dt
8 nroTotFile I
9 esito S
Dettaglio passi operazione
Il processo accede alla tabella GDP_LOG e seleziona le occorrenze con TIPO_ACQUISIZIONE = e
DT_ACQUISIZIONE = .
Per ciascuna occorrenza trovata recupera le informazioni collegate su GDP_TESTATA, GDP_EDIZIONE,

GDP*LOG_EDIZIONE e valorizza un elenco con:
✓ idLog = GDP_LOG.ID_GDP_LOG
✓ idTestata = GDP_LOG. FK_GDP_TESTATA
✓ NomeTestata = GDP_TESTATA.ID_GDP_TESTATA
GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 30 di 37
✓ dataEdizione = GDP* EDIZIONE.DATA_EDIZIONE
✓ dataAcquisizione = GDP_LOG.DT_ACQUISIZIONE
✓ NroTotFile = GDP_LOG.TOTALE_FILE_ACQUISITI
✓ Esito = GDP_LOG.ESITO
L’elaborazione termina con l’output definito, altrimenti restituisce MSG00001

Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione OK
MSG00001 Problema nel recupero dei dati

Operazione: F13 - MONITOR.dettaglioAcquisizione
Modalità di richiamo
Il servizio è richiamato in modalità “sincrona” dal back-end applicativo.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 idLog I Obb
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
1 Un elenco di “Edizioni”
(^1) idLog I

(^2) IDTestata I

3 nomeTestata S

4 dataEdizione Dt

5 tipoEdizione Decodificato con GDP_TIPO_EDIZIONE.DESCRIZIONE S

(^6) tipoAcquisizione G→Giornaliera, S→Storica S
1.7 dataAcquisizione Dt
1.8 NroTotFile I
1.9 Esito S

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 31 di 37
1.10 (^) idEdizione I
1.11 (^) primaPagina True=SI, False=NO S
1.12 fileXML True=SI, False=NO S
1.12 fileZIP True=SI, False=NO S
1.13 nroPagAcq I
1.14 (^) nroPagOK I
1.15 nroPagErrate I
1.16 jobID I
1.17 descrizione S
Dettaglio passi operazione
Il processo accede alla tabella GDP_LOG e seleziona l’occorrenza con ID_GDP_LOG = , quindi accede a
GDP_LOG_EDIZIONE e seleziona l’occorrenza corrispondente.

Recupera, inoltre, le informazioni collegate su GDP*TESTATA e GDP_EDIZIONE e valorizza l’output con:
✓ idLog = GDP_LOG.ID_GDP_LOG
✓ IDTestata = GDP_LOG. FK_GDP_TESTATA
✓ NomeTestata = GDP_TESTATA.ID_GDP_TESTATA
✓ dataEdizione = GDP* EDIZIONE.DATA_EDIZIONE
✓ tipoEdizione = TIPO_EDIZIONE.DESCRIZIONE
✓ tipoAcquisizione = GDP_LOG.TIPO_ACQUISIZIONE
✓ dataAcquisizione = GDP_LOG.DT_ACQUISIZIONE
✓ NroTotFile = GDP_LOG.TOTALE_FILE_ACQUISITI
✓ Esito = GDP_LOG.ESITO
✓ idEdizione = GDP_LOG_EDIZIONE.FK_GDP_EDIZIONE
✓ primaPagina = GDP_LOG_EDIZIONE.PRIMA_PAGINA
✓ fileXML = GDP_LOG_EDIZIONE.FILE_XML
✓ fileZIP = GDP_LOG_EDIZIONE.FILE_ZIP
✓ nroPagAcq = GDP_LOG_EDIZIONE.NRO_PAG_ACQUISITE
✓ nroPagOK = GDP_LOG_EDIZIONE.NRO_PAG_VALIDE
✓ nroPagErrate = GDP_LOG_EDIZIONE.NRO_PAG_ERRATE
✓ idOperazione = GDP_LOG_EDIZIONE.JOB_ID
✓ descrizione = GDP_LOG_EDIZIONE.DESCRIZIONE
L’elaborazione termina con l’output definito, altrimenti restituisce MSG00001

Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione OK
MSG00001 Problema nel recupero dei dati di monitoraggio

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 32 di 37
Operazione: F14 - MONITOR.preparaMAIL
Modalità di richiamo
Il servizio è richiamato in modalità “sincrona” dal back-end applicativo.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 idLog I Obb
2 tipoMail S Obbl
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
1 from GDP_MAIL.MITTENTE S
2 to S
3 host S
4 porta^^ I^
5 oggetto S
6 testo S
Dettaglio passi operazione
Con il parametro di input il processo seleziona su GDP_LOG l’occorrenza con ID_GDP_LOG = ;
quindi con FK_GDP_UTENTE_FTP accede a GDP_UTENTE_FTP e recupera l’indirizzo mail in EMAIL che salva in
. Se l’indirizzo non è definito l’elaborazione termina e restituisce MSG00001
Dall’occorrenza su GDP_LOG con FK_GDP_TESTATA accede a GDP_TESTATA e recupera NOME_TESTATA che
salva in .
Se il parametro di input è diverso da “STnnn”, dall’occorrenza su GDP_LOG_EDIZIONE con
FK_GDP_EDIZIONE accede a GDP_EDIZIONE e recupera DATA_EDIZIONE che salva in .
Con il parametro di input il processo seleziona su GDP_MAIL l’occorrenza con COD_MAIL = < tipoMail>
in cui procede a rimpiazzare i segnaposti <[dataED]> e <[nomeTestata]> con i rispettivi valori ricavati e
.
L’elaborazione termina dopo aver composto l’output definito.

Codici di errore / messaggi restituiti

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 33 di 37
Codice Descrizione
MSG00009 Elaborazione OK
MSG0000 1 Indirizzo mail non trovato
MSG0000 2 Invio mail fallito
Operazione: F15 - MONITOR.ricercaAcquisizioni
Modalità di richiamo
Il servizio è richiamato in modalità “sincrona” dal back-end applicativo.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 tipoAcquisizione (^) • giornaliera=“G”

storica=“S”
I Obbl
2 Testata Identificativo univoco S Obbl
3 dataDA Dt opz
dataA Dt Obbl
4 tipoEdizione (^) • corrispondente="OK"

sospesa="SO"
anticipataria="AN"
posticipataria="PO"
anomalia edizione attesa="AA"
edizione storica="ST"
anomalia edizione storica="AS"
S Obbl
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
1 Un elenco di “ edizioni monitorate ”
1 idLog I

2 idTestata I

3 nomeTestata S

4 tipoEdizione (^) • OK = corrispondente S

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 34 di 37
SO = sospesa
AN = anticipataria
PO = posticipataria
AA = anomalia edizione attesa
ST = edizione storica
AS = anomalia edizione storica
5 dataEdizione Dt
6 dataAcquisizione Dt
7 nroTotFileAcq I
8 nroTotFileVal I
Dettaglio passi operazione
Il servizio accede alla tabella GDP_LOG e ricava le occorrenze in base ai parametri valorizzati, tenendo conto che:
✓ GDP_LOG.TIPO_ACQUISIZIONE =
✓ GDP_LOG.FK_GDP_TESTATA =
✓ GDP_LOG.DT_ACQUISIZIONE >= dataDA
✓ GDP_LOG.DT_ACQUISIZIONE <= dataA
✓ GDP_LOG_EDIZIONE.TIPO_EDIZIONE =
compone l’elenco delle occorrenze da restituire in output:
✓ idLog = GDP_LOG.ID_GDP_LOG
✓ idTestata = GDP_TESTATA.ID_TESTATA (ID_TESTATA= GDP_LOG.FK_GDP_TESTATA)
✓ nomeTestata = GDP_TESTATA.NOME_TESTATA
✓ tipoEdizione = GDP_LOG_EDIZIONE.TIPO_EDIZIONE
✓ dataEdizione = GDP_EDIZIONE.DATA_EDIZIONE (ID_EDIZIONE=GDP_LOG_EDIZIONE
.FK_GDP_EDIZIONE)
✓ dataAcquisizione = GDP_LOG.DT_ACQUISIZIONE
✓ nroTotFileAcq = GDP_LOG_EDIZIONE. NRO_PAG_ACQUISITE
✓ nroTotFileVal = GDP_LOG_EDIZIONE. NRO_PAG_VALIDE

Se non viene trovata alcuna occorrenza il processo restituisce MSG0000 1

Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione OK
MSG0000 1 Nessun dato trovato

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 35 di 37
Operazione: F20 - MONITOR.statoDAM
Modalità di richiamo
Il servizio è richiamato in modalità “sincrona” dal back-end applicativo.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 jobID I Obb
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
Dettaglio passi operazione
Il processo legge il contenuto del parametro di input e con la BASE URL http://ts-libra-sv-exp1.csi.it/rpcr02 [per ora si
fa riferimento all’ambiente di TEST] richiama il servizio “ /api/v2/success ” con il parametro ricevuto in input.
Il processo attende la risposta del servizio da cui preleva il valore del campo:

"status”
L’elaborazione termina restituendo il dato ricevuto MSG0000 9 , altrimenti restituisce MSG0000 1
Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Stato edizione
MSG0000 1 Dato non trovato

Operazione: F21 - MONITOR.attivaCODA
Modalità di richiamo
Il servizio è richiamato in modalità “sincrona” dal back-end applicativo.
Descrizione dell’operazione

Interfaccia di richiamo (input)

1 idLog (^) Regole di composizione Tipo Obbl
1 idLog I Obb

GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 36 di 37
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
Dettaglio passi operazione
Il processo legge il contenuto del parametro di input e tramite GDP_LOG (ID_GDP_LOG = ) e
GDP_LOG_EDIZIONE (FK_GDP_LOG = ID_GDP_LOG) accede all’occorrenza dell’edizione su
GDP_CODA_CARICAMENTO (FK_GDP_LOG_EDIZIONE = ID_GDP_LOG_EDIZIONE) ed imposta:
▪ DATA_INSERIMENTO_IN_CODA = Timestamp (nel formato opportuno)
▪ STATO = “READY”
▪ NRO_TENTATIVO incrementato di 1
Se NRO_TENTATIVO aggiornato è minore o uguale a NRO_MAX_TENTATIVI l’operazione termina con
MSG0000 9 , altrimenti restituisce MSG0000 1

Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Tentativo di invio numero <NRO_TENTATIVO> attivato
MSG0000 1 ATTENZIONE! Superato il numero massimo di tentativi ammessi

Operazione: F22 - MONITOR.invioMAIL
Modalità di richiamo
Il servizio è richiamato in modalità “sincrona” dal back-end applicativo.
Descrizione dell’operazione

Interfaccia di richiamo (input)

# Parametro Regole di composizione Tipo Obbl

1 from S Obbl
2 to S Obbl
3 Host S Obbl
4 porta I Obb
5 Oggetto S Obbl
6 testo S Obbl
GDP – GIORNALI DEL PIEMONTE
Specifica dei servizi
Servizi_BackEnd
Pag. 37 di 37
Interfaccia di output

# Nome campo Contenuto e regole Tipo

0 Esito Sempre valorizzato.
Per elaborazione correttamente eseguita contiene
MSG00009 , altrimenti MSG0000x.
S
Dettaglio passi operazione
Con i parametri di input il processo procede all’invio della mail.
Se l’operazione fallisce il sistema restituisce MSG00001

Codici di errore / messaggi restituiti
Codice Descrizione
MSG00009 Elaborazione OK
MSG0000 1 Invio mail fallito

Problemi aperti
ID Problema Descrizione
1 Consentire all’editore di fornire esplicitamente l’elenco delle date delle edizioni attese?
Nel caso, come si procede al caricamento sul DB? A mano...
This is a offline tool, your data stays locally and is not send to any server!
