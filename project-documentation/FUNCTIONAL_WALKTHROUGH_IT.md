# Walkthrough Funzionale — gdporch

Questo documento descrive il funzionamento passo-passo dell'orchestratore `gdporch` per la gestione dei flussi giornalieri e storici dei Giornali del Piemonte.

---

## 1. Flusso Regolare (Giornaliero)

L'orchestratore opera attraverso tre trigger principali pianificati:

1.  **F01 — Calcolo Date Attese (Annuale / On-demand):**
    *   Calcola tutte le date di uscita attese per ogni testata attiva in base alla propria periodicità.
    *   Salva i risultati nella tabella `GDP_DATA_USCITA`.
    *   È la base informativa fondamentale per l'intero ciclo di vita dell'edizione.

2.  **F02 — Creazione Cartelle SFTP (Ogni sera alle 20:00):**
    *   Crea fisicamente sul server sFTP la cartella con la data di domani (`yyyy-mm-dd`) sotto la root della testata.
    *   Prepara l'ambiente dove l'editore dovrà depositare i PDF.

3.  **F03 — Scansione e Acquisizione (Ogni 15 minuti):**
    *   Scansiona tutte le cartelle sotto `/_flusso_regolare`.
    *   Se trova nuovi file, verifica la stabilità del trasferimento (attesa fine scrittura).
    *   Sposta i file in una zona temporanea locale `/_tmp` e attiva in modo asincrono il processo di controllo (**F04**).
    *   Registra l'evento in `GDP_LOG`.

---

## 2. Il Cuore del Processo: F04 (Validazione)

Attivato in modo asincrono da F03 per ogni edizione acquisita, il servizio **F04** esegue:

*   **Verifica Coerenza Data:** Confronta la data dell'edizione con il calendario atteso sul DB (Classificazione: **OK**, Anticipata, Posticipata, Sospesa o Anomala).
*   **Gestione PDF:** Controlla se il PDF è multi-pagina e lo splitta in file singoli se necessario.
*   **Leggibilità:** Verifica che il file sia un PDF/A leggibile tramite librerie di sistema.
*   **Standard Naming:** Verifica il rispetto dello standard di denominazione e rinomina i file secondo il pattern ufficiale.
*   **Estrazione Testo:** Estrae il contenuto testuale tramite `pdftotext` (per l'indicizzazione).
*   **Controlli Euristici:** Analisi del testo della prima pagina per verificare la data e la presenza degli elementi minimi (direttore, redazione, ecc.).
*   **Chiamata a Catena:** Al termine, invoca in sequenza sincrona **F08** e **F09**, e lancia il caricamento **F10** in modo asincrono.

---

## 3. Persistenza e Trasmissione al DAM

*   **F08 (Database):** Inserisce o aggiorna i record nelle tabelle `GDP_EDIZIONE` e `GDP_PAGINA` (un record per ogni pagina validata).
*   **F09 (Packaging):**
    *   Costruisce il file XML con i metadati (XSD `GdP-STD-04-V01`).
    *   Comprime XML, PDF e TXT in un unico archivio ZIP.
    *   Inserisce un record nella coda di invio `GDP_IMPORT_TASK` (stato `PRO`).
*   **F10 (Invio DAM Libra — ogni 30 minuti):** Legge la coda ordinata per priorità e invoca l'API REST di Libra per il caricamento dello ZIP.

---

## 4. Flusso Saltuario (Storico)

*   **Trigger Serale:** Scansiona la cartella `/_flusso_saltuario`.
*   **F07 (Validazione Storica):**
    *   Individua i pacchetti di consegna (`CONS_yyyy-mm-dd`).
    *   Verifica le date delle edizioni storiche e valida ogni file (PDF, TXT, TIF).
    *   Attiva la stessa catena di persistenza e packaging (**F08** → **F09** → **F10**) ma con **Priorità 100**.
    *   Le edizioni storiche vengono elaborate in coda dopo quelle giornaliere.

---

## 5. BFF Console (Back-end for Frontend)

Espone API sincrone per la UI di monitoraggio (`gdpbff`):

*   **Query Acquisizioni:** `F12`, `F13`, `F15` per consultare i log (per data, per ID, con filtri dinamici).
*   **Stato DAM:** `F20` interroga il sistema Libra per conoscere lo stato di elaborazione di un job.
*   **Gestione Errori:** `F21` permette di resettare un tentativo di invio fallito e riaccodarlo.
*   **Notifiche:** `F14` e `F22` gestiscono la composizione e l'invio delle mail di report/errore.
*   **Sospensioni:** `F05` consente di sospendere manualmente una o più date nel calendario delle attese.
