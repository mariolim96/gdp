# Contesto del progetto GDP (Giornali del Piemonte)

GDP, progetto/prodotto CSI, è nuova Piattaforma dei materiali editoriali.

<b>Cos'è GDP</b> => <i>
L'obiettivo della proposta è la realizzazione della nuova Piattaforma dei materiali editoriali in cui confluiranno tutti i materiali ad oggi ricompresi nel servizio “Giornali del Piemonte” (di seguito indicato come GdP), il materiale che le singole testate giornalistiche continueranno a mettere a disposizione del sistema ed il materiale derivante da campagne di digitalizzazione relative a testate storiche di interesse.

La proposta, nel dettaglio, prevede:

- la realizzazione di un nuovo servizio di archiviazione a lungo termine delle informazioni digitali, sul quale migrare gli archivi attualmente in uso, con l’obiettivo di ottimizzare le componenti infrastrutturali e i relativi costi di gestione;
- la riprogettazione del processo di acquisizione del materiale, sia attinente al materiale delle testate storiche sia relativo al materiale messo a disposizione periodicamente dalle singole testate;
- la riprogettazione della componente di front end, mediante l’adozione di tecnologie moderne e conformi alle linee guida AGID per la Pubblica Amministrazione, in grado di garantire agli utenti un’esperienza di consultazione e ricerca più efficiente e fruibile da una pluralità di dispositivi.

Beneficiari della proposta sono tutte le Direzioni, i Settori e i dipendenti del Consiglio Regionale del Piemonte, nonché tutti gli utilizzatori dell’attuale servizio.

Il sistema nel suo complesso prevede il coinvolgimento di due fonti di alimentazione esterna:

• gli editori di testate locali, che in base alla periodicità della pubblicazione inviano il materiale dell’edizione via sFTP su area riservata;
• i gestori di archivi storici (perlopiù biblioteche) che trasmettono sempre via sFTP le edizioni storiche relative ad un periodo temporale per una o più testate.

Il materiale consegnato è costituito da file PDF per le edizioni attive e da file PDF, TXT e TIFF per le edizioni storiche.

A livello architetturale GDP si compone logicamente di:


- gdpbospa mini-verticale di BackOffice con ruolo di cruscotto di configurazione e monitoraggio
- gdpfospa applicazione fruibile in diverse modalità (autenticata/non autenticata)
- un database per la persistenza delle configurazioni e tracciamento interazioni con API di Libra
- un orchestratore che integra i flussi di chiamate delle API di Libra per gli scopi di cui sopra
- Un file system di lavoro condiviso in read/write con i utenti sFTP per scambio pacchetti di contenuto input/output (ingestion e info arricchite)

Il prodotto è ad istanza singola, la linea cliente di riferimento è RPCR-01.

Seguono i diagrammi architetturali standard CSI che descrivono la soluzione


### Diagramma delle componenti
![component](./model/images/GdP-1.0.0-Component.png)

### Diagramma di deployment (Farm Nivola-C)
![deployment](./model/images/GdP-1.0.0-Deployment.png)

A seguire ulteriori diagrammi NON standard CSI ma esplicativi della soluzione:

### Deployment view logico - dettaglio (Farm Nivola-C - EcaaS)
![k8s-csi-svil01](./model/images/GDP-k8s-csi-svil01-gdp-rpcr-01-svil-V2.png)

### Container Diagram (Farm Nivola-C - EcaaS)
![k8s-csi-svil01](./model/images/GDP-1.0.0-container.png)

Lettura sintetica:

- gdpbospa è esposto su gdp-csi-svil... con Shibboleth ICSI attivo e fa proxy verso gdpbff sul path /gdpbospaweb/restfacade -> /restfacade.
- gdpfospa-noauth è esposto senza Shibboleth, mentre gdpfospa-rupar e gdpfospa-crp hanno Shibboleth attivo rispettivamente con WRUP e CRP; tutti e tre proxano /gdpfospaweb/restfacade verso gdpbff.
- gdpbff usa configurazione datasource da Vault e punta a dbs-gdp-dev-001p.site05.nivolapiemonte.it:5432/GDPDB; 
- gdporch usa anch’esso Vault, dialoga con PostgreSQL, SFTP e Libra API.


## Informazioni operative
Sono fornite le informazioni tecnico/operative dell'infrastruttura di deployment della soluzione.

La deploy Farm di riferimento è Nivola-C, cioè la versione cloud native ECaaS (K8s, container Kubernetes) della Farm Nivola.

La filiera è costituita come default da due Ambienti: sviluppo e produzione.

I cluster Kubernetes sono due, uno di pre-produzione e l'altro di produzione.

Le risorse esterne al cluster K8s sono:
- il database applicativo, erogato in modalità DBAAS su VM
- la piattaforma LIBRA
- il servizio centralizzato sFtp per l'acquisizione dei materiali
- il servizio Mail Farm

Seguono le coordinate dei vari ambienti che costituiscono la filiera di GDP

### Ambiente di sviluppo

<b>Cluster Kubernetes</b>
- Nome dell'istanza di pre-produzione => k8s-csi-svil01
- Namespace dell'applicativo</b> => gdp-rp-01-svil

Accessibilità da locale: garantita da rete VPN con opportuno profilo assegnato alle utenze abilitate

L'accesso da locale è utile nel caso in cui lo sviluppatore abbia necessità di procedere ad un deploy dell'applicazione nel cluster.
Questa possibilità è concessa soltanto per l'ambiente di pre-produzione.

<b>Database applicativo</b>:
- host => dbs-gdp-dev-001p.site05.nivolapiemonte.it:5432/
- porta => 5432
- database name => GDPDB
- schema => gdp
- ruoli => gdp, gdp_rw

Accessibilità: sono state applicate le policy di rete affinchè il DB sia raggiungibile dai nodi del cluster K8s di sviluppo.

Per l'accesso da locale occorre configurare un tunnel SSH passando da un gateway CSI, raggiungibile via VPN:
- host gateway SSH => cmpto2-gwcl1.site02.nivolapiemonte.it
- porta: 22
- credenziali di accesso: utenza di dominio DOMNT fornita nel provisioning

Questa configurazione è tipicamente supportata all'interno dei tool client DB (as esempio DBeaver).
Nel caso si usasse un tool che non lo supporta, si può configurare lo strumento Putty in locale come gateway SSH usando i paramentri di connessione suindicati.

<b>Piattaforma LIBRA</b>:
- VH endpoint => http://ts-libra-sv-exp1.csi.it/rpcr02/api/v2/
- porte supportate => 80
- autenticazione alle API => Le API REST v2 supportano autenticazione per mezzo di token JWT firmato, rilasciato dal fruitore mediante una propria chiave privata.
Per poter accedere con tale token il fruitore dovrà aver registrato la chiave pubblica su LIBRA (opportuna richiesta al gruppo di gestione INDEX/LIBRA).
L'attività di registrazione per l'accesso a Libra è già stata svolta (le chiavi pubblica/privata per GDP sono disponibili in uno share apposito).


Accessibilità: sono state applicate le policy di rete affinchè il DB sia raggiungibile dai nodi del cluster K8s di sviluppo.

L'accesso da locale è garantito via VPN, tramite profili trasversali assegnati a fornitori CSI che operano esternamente.

Per conferma della raggiungibilità da locale è possibile usare l'utility telnet da linea di comando (da una power shell o da shell Dos):

<i>telnet ts-libra-sv-exp1.csi.it \<porta\></i> porta 80

In caso affermativo, la connessione viene stabilita.

<b>Servizio trasversale sFtp</b>:

NOTA: Il servizio è unico ed è esposto sia per accesso da ambienti di pre-produzione che di produzione

- host => podto1-sftpsrv.site01.nivolapiemonte.it
- porta => 2222
- cartella di lavoro => <i>/data</i> (su tale cartella sono già state create manualmente due sottocartelle per differenziare l'uso in base all'ambiente di provenienza; <i>/dev</i> per accesso da sviluppo e <i>/prod</i> per accesso da produzione)
- credenziale di accesso utenza GDPSFTP
    - user => gdpsftp
    - password => occorre usare una chiave privata RSA in fomrato PEM da copiare lato client

Accessibilità: sono state applicate le policy di rete affinchè il server sia raggiungibile dai nodi del cluster K8s di sviluppo.

L'accesso da locale è garantito via VPN, tramite un profilo specifico assegnato all'utenza DOMNT (2091_custom_ftp_nivola) ed il possesso della chiave privata copiata in locale.

Per conferma della raggiungibilità da locale è possibile usare l'utility telnet da linea di comando (da una power shell o da shell Dos):

<i>telnet podto1-sftpsrv.site01.nivolapiemonte.it 2222</i>

In caso affermativo, la connessione viene stabilita.

### Ambiente di collaudo
Al momento non è previsto l'uso

### Ambiente di produzione
Ambiente non disponibile; sarà oggetto di provisioning futuro, ancora da pianificare.

<b>Cluster K8s</b> => k8s-csi-prod01

<b>Namespace Nivola-C</b> => gdp-rpcr-01-prod

<b>Database applicativo</b>:
- host => TBD
- porta => 5432
- database name => DBGDP
- schema => gdp
- ruoli => gdp, gdp_rw

<b>Piattaforma LIBRA</b>:
- VH endpoint => TBD
- porte supportate => 80
- autenticazione alle API => basic auth (user,password) fornite dal gruppo di supporto CSI della piattaforma

Per l'accessibilità valgono le stesse considerazioni di Sviluppo; saranno applicate in fase di allestimento dell'ambiente

<b>Servizio trasversale sFtp</b>: la stessa configurazione di Sviluppo

## Help

Any advise for common problems or issues.
```
command to run if program contains helper info
```

## Authors

Gruppo di progetto CSI
ALO: Cultura
Soluzione applicativa: Ecosistema Beni Culturali Regione Piemonte

## Version History

* x.x.x
    * Various bug fixes and optimizations
    * See [commit change]() or See [release history]()
* 1.0.0
    * Initial Release

## License

This project is licensed under the [NAME HERE] License - see the LICENSE.md file for details

## Acknowledgments

Inspiration, code snippets, etc.
