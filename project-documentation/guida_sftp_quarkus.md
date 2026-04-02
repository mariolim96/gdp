# Tutorial SFTP — RebexTinySftpServer + MobaXterm + Java Quarkus

> **Obiettivo:** Configurare un server SFTP locale con **RebexTinySftpServer**, connettersi tramite **MobaXterm** e integrare la connessione nel progetto **Java Quarkus** tramite le classi `SftpClientProducer` e `SftpSession`.

---

## Prerequisiti

| Software | Download | Note |
|---|---|---|
| Docker Desktop | docker.com | Alternativa portabile (consigliata) |
| RebexTinySftpServer | rebex.net/tiny-sftp-server/ | Versione Windows |
| MobaXterm | mobaxterm.mobatek.net | Home Edition gratuita |
| Java 17+ / Maven | openjdk.org | Per il progetto Quarkus |
| JSch (dipendenza Maven) | — | Aggiunta al `pom.xml` |

---

## Parte 1 — Configurare RebexTinySftpServer

### STEP 1 — Creare la cartella SFTP

Crea la directory che sarà la root del server SFTP. Questa cartella conterrà tutti i file accessibili via SFTP dal tuo progetto Quarkus e da MobaXterm.

```bat
:: Apri il prompt dei comandi (CMD) come Amministratore
mkdir C:\SFTP_TEST

:: Verifica che la cartella esista
dir C:\SFTP_TEST
```

> 📥 Scarica RebexTinySftpServer dal sito ufficiale ed estrailo in una cartella a tua scelta, ad esempio `C:\Tools\RebexSFTP\`.

---

### STEP 2 — Modificare il file `App.config`

Apri il file **`RebexTinySftpServer.exe.config`** con un editor di testo (es. Notepad++ o VS Code).  
Modifica le seguenti chiavi nella sezione `<appSettings>`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<configuration>
  <appSettings>
    <!-- Credenziali utente e cartella root -->
    <add key="userName"     value="tester" />
    <add key="userPassword" value="password" />
    <add key="userRootDir"  value="C:\SFTP_TEST" />

    <!-- Porta SSH (2222 evita conflitti con la porta 22) -->
    <add key="sshPort"      value="2222" />

    <!-- Avvia automaticamente all'apertura -->
    <add key="autoStart"    value="true" />

    <!-- Chiavi host del server (lascia i valori di default) -->
    <add key="rsaPrivateKeyFile"     value="server-private-key-rsa.ppk" />
    <add key="rsaPrivateKeyPassword" value="my-super-secure-password" />
    <add key="dssPrivateKeyFile"     value="server-private-key-dss.ppk" />
    <add key="dssPrivateKeyPassword" value="my-super-secure-password" />
  </appSettings>
</configuration>
```

| Chiave | Valore esempio | Descrizione |
|---|---|---|
| `userName` | `tester` | Nome utente per il login SFTP |
| `userPassword` | `password` | Password dell'utente |
| `userRootDir` | `C:\SFTP_TEST` | Cartella radice esposta via SFTP |
| `sshPort` | `2222` | Porta SSH |
| `autoStart` | `true` | Avvia il server all'apertura dell'app |

> ⚠️ I campi `rsaPrivateKeyFile` e `dssPrivateKeyFile` devono puntare ai file `.ppk` inclusi nella cartella di RebexTinySftpServer. Non modificarli se non stai usando chiavi personalizzate.

---

### STEP 3 — Avviare il server

Fai doppio clic su **`RebexTinySftpServer.exe`**. Se `autoStart` è `true`, il server parte automaticamente. Nella finestra principale vedrai:

```
Host:     127.0.0.1
Port:     2222
User:     tester
Password: password
Root:     C:\SFTP_TEST
```

> ℹ️ Se il firewall di Windows mostra un avviso, clicca su **Consenti accesso**.

---

## Parte 1.2 — Alternativa Docker (Portabile e Veloce)

Se hai Docker installato, puoi evitare di scaricare software Windows usando un'immagine leggera. Questo garantisce che tutto il team abbia lo stesso ambiente.

### STEP 3.1 — Configura `docker-compose.yml`

Nel root del tuo progetto, assicurati di avere il servizio SFTP:

```yaml
services:
  sftp:
    image: atmoz/sftp:alpine
    container_name: gdp-sftp
    environment:
      # Formato: user:password:uid:gid:root_folder
      - SFTP_USERS=tester:password:1001:1001:upload
    ports:
      - "2222:22"
    volumes:
      - ./sftp-data/tester:/home/tester/upload
```

### STEP 3.2 — Avvia il container

```bash
docker compose up -d sftp
```

> [!NOTE]
> I file creati dal codice appariranno nella cartella locale `./sftp-data/tester/upload`.

---

## Parte 2 — Connettersi con MobaXterm

### STEP 4 — Creare una nuova sessione SFTP

1. Avvia **MobaXterm**
2. Clicca su **Session** nella barra in alto
3. Nella finestra che si apre, seleziona **SFTP** (l'icona della cartella blu, **non** SSH)
4. Compila i campi come segue:

| Campo | Valore da inserire | Note |
|---|---|---|
| Remote host | `127.0.0.1` | Localhost (stesso PC) |
| Username | `tester` | Come in `App.config` |
| Port | `2222` | Come in `App.config` |
| Authentication | `Password` | Seleziona "Password" |

5. Clicca **OK** per salvare e avviare la sessione
6. Inserisci la password: **`password`**

---

### STEP 5 — Navigare nella cartella SFTP (cartella blu)

Una volta connesso, nel pannello sinistro di MobaXterm vedrai il **file browser SFTP** — la cartella blu con la struttura di `C:\SFTP_TEST`. Da qui puoi:

- **Caricare file**: drag & drop dal tuo PC
- **Scaricare file**: clic destro → Download
- **Creare cartelle**: clic destro → Create directory
- **Eliminare file**: clic destro → Delete

> ℹ️ Sul lato destro hai anche un terminale SSH per eseguire comandi se necessario.

---

### STEP 6 — Salvare la sessione per usi futuri

MobaXterm salva automaticamente le sessioni nel pannello **Sessions** a sinistra.  
Puoi rinominarla con clic destro → **Rename** (es. `SFTP Locale Quarkus`) per ritrovarla facilmente.

---

## Parte 3 — Integrazione con Java Quarkus

### STEP 7 — Aggiungere la dipendenza JSch al `pom.xml`

Nel tuo progetto Quarkus aggiungi la dipendenza per **JSch**:

```xml
<dependency>
    <groupId>com.jcraft</groupId>
    <artifactId>jsch</artifactId>
    <version>0.1.55</version>
</dependency>
```

---

### STEP 8 — Configurare `application.properties`

Aggiungi le proprietà di connessione SFTP nel file `src/main/resources/application.properties`:

```properties
# SFTP Configuration (Local Docker o Rebex)
sftp.host=localhost
sftp.port=2222
sftp.username=${SFTP_USER:tester}
sftp.password=${SFTP_PASS:password}
sftp.key.path=${SFTP_KEY}
```

### STEP 8.1 — Gestione env vars con file `.env` (Best Practice)

Per non scrivere password nel codice, crea un file `.env` nella root di Quarkus (`gdporch/.env`):

```env
GDP_DB_USER=gdp_user
GDP_DB_PASSWORD=gdp_password
SFTP_KEY=C:/Users/tuo_nome/.ssh/id_rsa
```

> Questi valori corrispondono esattamente a quelli configurati nel file `App.config` di RebexTinySftpServer.

---

### STEP 9 — La classe `SftpSession`

Questa classe rappresenta il "pacchetto" connessione, contenente sia la sessione SSH generale che il canale SFTP specifico per i file. Implementa `AutoCloseable` così Java la chiude automaticamente al termine dell'uso (ideale con il costrutto `try-with-resources`).

```java
package it.csipiemonte.gdp.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

// "AutoCloseable" significa: "Caro Java, quando finisco di usarmi, chiudimi da solo"
public class SftpSession implements AutoCloseable {

    private final Session session;      // La connessione SSH generale
    private final ChannelSftp channel;  // Il canale specifico per spostare file

    public SftpSession(Session session, ChannelSftp channel) {
        this.session = session;
        this.channel = channel;
    }

    // Restituisce il canale al Service quando deve lavorare
    public ChannelSftp getChannel() {
        return channel;
    }

    // Questo metodo viene chiamato AUTOMATICAMENTE da Java alla fine del blocco try
    @Override
    public void close() {
        if (channel != null && channel.isConnected()) {
            channel.disconnect(); // Chiude il canale SFTP
        }
        if (session != null && session.isConnected()) {
            session.disconnect(); // Chiude la sessione SSH
        }
    }
}
```

**Punti chiave:**
- `AutoCloseable` garantisce che la connessione venga sempre chiusa, anche in caso di eccezione
- `getChannel()` espone solo il canale SFTP, nascondendo i dettagli della sessione SSH
- `close()` segue l'ordine corretto: prima il canale, poi la sessione

---

### STEP 10 — La classe `SftpClientProducer`

Questa classe `@ApplicationScoped` legge la configurazione da `application.properties` e crea la connessione SFTP restituendo un oggetto `SftpSession` pronto all'uso.

```java
package it.csipiemonte.gdp.sftp;

import com.jcraft.jsch.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import java.util.Properties;

@ApplicationScoped
public class SftpClientProducer {

    private static final Logger LOG = Logger.getLogger(SftpClientProducer.class);

    // Leggiamo i dati dal file application.properties
    @ConfigProperty(name = "sftp.host")     String host;
    @ConfigProperty(name = "sftp.port")     int    port;
    @ConfigProperty(name = "sftp.username") String user;
    @ConfigProperty(name = "sftp.password") String password;

    public SftpSession connect() throws JSchException {
        JSch jsch = new JSch(); // La libreria che "parla" SFTP
        LOG.infof("Connessione a %s:%d...", host, port);

        // Prepariamo la sessione (il tunnel SSH)
        Session session = jsch.getSession(user, host, port);
        session.setPassword(password);

        Properties config = new Properties();
        // In sviluppo: non verificare la chiave host del server
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.connect(10000); // Timeout di 10 secondi

        // Apriamo il canale specifico per i file (SFTP)
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();

        // Restituiamo il "pacchetto" sessione+canale
        return new SftpSession(session, channel);
    }
}
```

**Punti chiave:**
- `@ApplicationScoped` significa che Quarkus crea una sola istanza per tutta l'applicazione
- `@ConfigProperty` inietta automaticamente i valori da `application.properties`
- Il timeout `connect(10000)` evita attese infinite se il server non risponde
- `StrictHostKeyChecking=no` va usato **solo in sviluppo locale**

---

### STEP 11 — Usare `SftpSession` nell'orchestratore

Ecco come usare le due classi nell'orchestratore con il pattern `try-with-resources`, che garantisce la chiusura automatica della connessione:

```java
@ApplicationScoped
public class SftpOrchestrator {

    @Inject
    SftpClientProducer sftpProducer;

    public void uploadFile(String localPath, String remotePath) throws Exception {
        // try-with-resources: SftpSession.close() viene chiamato in automatico
        try (SftpSession sftpSession = sftpProducer.connect()) {
            ChannelSftp channel = sftpSession.getChannel();
            channel.put(localPath, remotePath);
            LOG.infof("File caricato: %s -> %s", localPath, remotePath);
        }
        // Qui la connessione è già chiusa automaticamente
    }

    public void downloadFile(String remotePath, String localPath) throws Exception {
        try (SftpSession sftpSession = sftpProducer.connect()) {
            ChannelSftp channel = sftpSession.getChannel();
            channel.get(remotePath, localPath);
            LOG.infof("File scaricato: %s -> %s", remotePath, localPath);
        }
    }

    public Vector<ChannelSftp.LsEntry> listFiles(String remoteDir) throws Exception {
        try (SftpSession sftpSession = sftpProducer.connect()) {
            return sftpSession.getChannel().ls(remoteDir);
        }
    }
}
```

---

### STEP 12 — Testare con un endpoint REST

Crea un endpoint di test per verificare che la connessione funzioni end-to-end:

```java
@Path("/sftp")
public class SftpResource {

    @Inject
    SftpOrchestrator orchestrator;

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public Response testConnection() {
        try {
            var files = orchestrator.listFiles("/");
            return Response.ok("Connessione OK! File trovati: " + files.size()).build();
        } catch (Exception e) {
            return Response.serverError().entity("Errore: " + e.getMessage()).build();
        }
    }
}
```

Avvia Quarkus in dev mode e testa:

```bash
mvn quarkus:dev

# In un altro terminale:
curl http://localhost:8080/sftp/test
# Output atteso: Connessione OK! File trovati: 0
```

---

## Flusso completo — Riepilogo

```
application.properties
        │
        ▼
SftpClientProducer.connect()
        │  legge host/port/user/pass
        │  crea JSch Session + ChannelSftp
        │
        ▼
SftpSession (AutoCloseable)
        │  contiene Session + ChannelSftp
        │
        ▼
SftpOrchestrator (try-with-resources)
        │  usa getChannel() per upload/download/list
        │  chiude automaticamente a fine blocco
        │
        ▼
RebexTinySftpServer (localhost:2222)
        │
        ▼
C:\SFTP_TEST  ←→  MobaXterm (verifica manuale)
```

---

## Risoluzione Problemi Comuni

| Problema | Causa probabile | Soluzione |
|---|---|---|
| `Connection refused` porta 2222 | Server non avviato o porta bloccata | Verifica che RebexTinySftpServer sia in esecuzione. Controlla il firewall Windows. |
| `Auth failed` (wrong credentials) | Username/password errati | Controlla `userName` e `userPassword` in `App.config`. Riavvia il server dopo ogni modifica. |
| La cartella SFTP è vuota | `userRootDir` non esiste | Crea manualmente `C:\SFTP_TEST` prima di avviare il server. |
| `Host key verification failed` | JSch rifiuta la chiave server | In dev: imposta `StrictHostKeyChecking=no`. In prod: aggiungi la chiave al `known_hosts`. |
| MobaXterm: cartella blu non appare | Sessione creata come SSH invece di SFTP | Crea una nuova sessione scegliendo esplicitamente **SFTP** (non SSH). |
| `NullPointerException` su `getChannel()` | `connect()` non chiamato o fallito | Controlla i log per l'errore di connessione. Verifica che il server sia attivo. |

---

## Note di Sicurezza

> ⚠️ La configurazione descritta in questo tutorial è pensata **esclusivamente per sviluppo locale**.

Per un ambiente di produzione:

- Usa **autenticazione con chiave pubblica/privata** invece della password
- Abilita **`StrictHostKeyChecking=yes`** e configura il `known_hosts`
- Non salvare credenziali in chiaro in `application.properties` — usa **Vault** o variabili d'ambiente
- Cambia la porta da `2222` a una non standard e protetta da firewall
- Usa connessioni **TLS/SSL** dove possibile

---

*Tutorial per progetto Java Quarkus — Integrazione SFTP con RebexTinySftpServer*
