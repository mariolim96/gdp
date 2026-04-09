package it.csipiemonte.gdp.gdporch.service.impl;

import io.quarkus.logging.Log;
import it.csipiemonte.gdp.gdporch.dto.EdizioneInsertResponse;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLog;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLogEdizione;
import it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogEdizioneRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogRepository;
import it.csipiemonte.gdp.gdporch.service.DamTrasmissioneService;
import it.csipiemonte.gdp.gdporch.service.GdpCtrlEdizioniStoricheService;
import it.csipiemonte.gdp.gdporch.service.GdpEdizioneService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ApplicationScoped
public class GdpCtrlEdizioniStoricheServiceImpl implements GdpCtrlEdizioniStoricheService {

    // Regex Nota 3: <nomeTestata>-<SiglaPROV>-<dataEdizione>_<nroPag>.<ext>
    private static final Pattern STORICO_FILE_PATTERN =
            Pattern.compile("^(.+)-([A-Z]{2})-(\\d{8})_(\\d{3})\\.(pdf|txt|tif)$", Pattern.CASE_INSENSITIVE);

    private static final DateTimeFormatter FOLDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @ConfigProperty(name = "sftp.root.prefix.tmp", defaultValue = "_tmp/")
    String tmpPrefix;

    @ConfigProperty(name = "sftp.root.prefix.errata", defaultValue = "_errata/")
    String errataPrefix;

    @Inject
    GdpLogRepository logRepository;

    @Inject
    GdpLogEdizioneRepository logEdizioneRepository;

    @Inject
    GdpEdizioneService edizioneService; // F08

    @Inject
    DamTrasmissioneService damTrasmissioneService; // F09

    @Override
    @Transactional
    public void ctrlEdizioniStoriche(Integer idTestata, String cartellaTestata, String dataConsegna, Integer idLog) {
        Log.infof("Avvio F07 per Testata: %d, Consegna: %s", idTestata, dataConsegna);
        //Contatori per MSG00009
        ContatoriGlobali cg = new ContatoriGlobali();

        // Path da documento: _tmp/CONS_<dataConsegna>/<cartellaTestata>/
        Path rootTmpPath = Paths.get(tmpPrefix, "CONS_" + dataConsegna, cartellaTestata);

        if (!Files.exists(rootTmpPath)) {
            Log.errorf("Path non trovato: %s", rootTmpPath);
            // Gestire MSG00001 se necessario
            return;
        }
        try (Stream<Path> edizioniStream = Files.list(rootTmpPath)) {
            edizioniStream.filter(Files::isDirectory).forEach(pathEdizione -> {
                //incremento edizioni esaminate
                cg.nroEdizioni++;
                processaCartellaEdizione(pathEdizione, idTestata, cartellaTestata, dataConsegna, idLog, cg);
            });

            // Formattazione MSG00009
            String riepilogoFinale = String.format(GdpMessage.F07_OK.getDescrizioneDefault(),
                    idTestata, cartellaTestata, cg.nroEdizioni,
                    cg.pdfTot, cg.pdfKo, // <nroPDFOK> e <nroPDFKO>
                    cg.txtTot, cg.txtKo, // <nroTXTOK> e <nroTXTKO>
                    cg.tifTot, cg.tifKo  // <nroTIFOK> e <nroTIFKO>
            );
            aggiornaLogFinale(idLog, GdpMessage.F_OK.getCodice(), riepilogoFinale);
            Log.info("F07 conclusa con successo");
        } catch (Exception e) {
            Log.error("Errore scansione F07", e);
        }
    }

    // Metodo per l'analisi della sottocartella (Edizione)
    private void processaCartellaEdizione(Path pathEdizione, Integer idTestata, String cartellaTestata, String dataConsegna, Integer idLog, ContatoriGlobali cg) {
        String nomeCartella = pathEdizione.getFileName().toString();
        LocalDate dataEdizione;

        // 1. Verificare se nomeCartella match con formato yyyyMMdd
        // Se Errato: Spostare in _errata, loggare TIPO_EDIZIONE = 'AS' (Anomala Storica) e passare oltre.
        try {
            dataEdizione = LocalDate.parse(nomeCartella, FOLDER_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            cg.nroEdizioniERR++;
            String descERR = String.format(GdpMessage.F07_WRONG_DATE_FORMAT.getDescrizioneDefault(),
                    cg.nroEdizioniERR, nomeCartella, "sistema", dataConsegna, cartellaTestata, nomeCartella);
            Log.warnf("Data cartella non valida: %s. Spostamento in _errata.", nomeCartella);
            registraEdizioneAnomala(idLog, pathEdizione, descERR);
            spostaInErrata(pathEdizione, dataConsegna, cartellaTestata);
            return;
        }

        // 2.Validazione file interni con STORICO_FILE_PATTERN
        // Deve estrarre i gruppi dalla Regex e verificare che la data nel nome file
        // sia uguale alla data del nome cartella.
        ContatoriStorico ct = validaFileInterni(pathEdizione, cartellaTestata, nomeCartella, dataConsegna);
        //AGGIORNAMENTO CONTATORI GLOBALI
        cg.pdfTot += ct.pdfOk;
        cg.pdfKo += ct.pdfKo;

        cg.txtTot += ct.txtOk;
        cg.txtKo += ct.txtKo;

        cg.tifTot += ct.tifOk;
        cg.tifKo += ct.tifKo;

        // 3. TODO: Check TIF Obbligatori (Nota 3)
        // Se dopo la scansione dei file, il conteggio dei TIF validi è 0,
        // l'edizione va scartata interamente (spostata in _errata).
        if (ct.tifOk == 0) {
            Log.warnf("Edizione %s scartata: File TIF mancante.", nomeCartella);
            String msgMancante = "NF - file TIF mancante";
            registraEdizioneAnomala(idLog, pathEdizione, msgMancante);
            spostaInErrata(pathEdizione, dataConsegna, cartellaTestata);
            return;
        }

        EdizioneInsertResponse res08 = edizioneService.insEdizione(idTestata, pathEdizione.toAbsolutePath().toString(), dataEdizione, idLog);
        if (res08 != null && res08.getIdEdizione() != null) {
            // 6.  Persistenza record su GDP_LOG_EDIZIONE (TIPO_EDIZIONE = 'ST')
            salvaLogEdizione(idLog, pathEdizione, TipoEdizione.ST, ct, res08.getIdEdizione(), GdpMessage.F_OK.getDescrizioneDefault());
                /* TODO: Da implementare quando F09 e F10 saranno pronte
                // 5. Se F08 OK: Invocazione F09 (creaXMLEdizione) con PRIORITÀ 100
                XmlCreationResponse res09 = damTrasmissioneService.creaXMLEdizione(idTestata, idLog, res08.getIdEdizione(), 100);
                if(res09 !=null && GdpMessage.F_OK.getCodice().equals(res09.getCodice())) {
                    // ESITO POSITIVO F09 -> Richiamo ASINCRONO F10
                    // Passiamo nomeFileZIP restituito da F09
                    damTrasmissioneService.inviaEdizioneAsync(idLog,res08.getIdEdizione(),res09.getNomeFileCompresso());
                }else{
                    //Esito negativo F09, sposto in cartella -errata
                    String msg04 = String.format("MSG00004 - Errore in F09: %s", (res09 != null ? res09.getMessaggio() : "Risposta nulla"));
                    registraEdizioneAnomala(idLog, pathEdizione, msg04);
                    spostaInErrata(pathEdizione, dataConsegna, cartellaTestata);
                }
                  --- FINE LOGICA F09-F10 --- */
        } else {
            String erroreTecnico = (res08 != null) ? res08.getMessaggio() : "Risposta nulla F08";
            String descErr = String.format(GdpMessage.F07_DB_ERROR.getDescrizioneDefault(), erroreTecnico,
                    dataEdizione.toString(), idTestata, cartellaTestata);
            registraEdizioneAnomala(idLog, pathEdizione, descErr);
            spostaInErrata(pathEdizione, dataConsegna, cartellaTestata);
        }
        Log.infof("F07: Edizione %s analizzata (Logica di invio commentata)", nomeCartella);
    }

     // Metodo per validazione naming e incrocio dati (Regex + Nome Cartella)
    private ContatoriStorico validaFileInterni(Path pathEdizione, String testataAttesa, String dataAttesa, String dataConsegna) {
        ContatoriStorico ct = new ContatoriStorico();
        try (Stream<Path> files = Files.list(pathEdizione)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                String fileName = file.getFileName().toString();
                String ext = getExtension(fileName).toLowerCase();
                Matcher matcher = STORICO_FILE_PATTERN.matcher(fileName);

                if (matcher.matches() && matcher.group(1).equalsIgnoreCase(testataAttesa) && matcher.group(3).equals(dataAttesa)) {

                    switch (ext) {
                        case "pdf" -> ct.pdfOk++;
                        case "txt" -> ct.txtOk++;
                        case "tif" -> ct.tifOk++;
                    }
                } else {
                    ct.erratiTotali++;
                    switch (ext) {
                        case "pdf" -> ct.pdfKo++;
                        case "txt" -> ct.txtKo++;
                        case "tif" -> ct.tifKo++;
                    }
                    if (ct.dettagliNF.length() > 0) ct.dettagliNF.append("; ");
                    ct.dettagliNF.append("NF - ").append(fileName);

                    //Spostamento singolo file non conforme
                    spostaFileErrato(file, dataConsegna, testataAttesa, dataAttesa);
                }
            });
        } catch (IOException e) {
            Log.error("Errore lettura file edizione", e);
        }
        return ct;
    }

     // Metodo per spostamento fisico in _errata
     //Deve mantenere la struttura /_errata/CONS_<dataConsegna>/<cartellaTestata>/<edizione>
    private void spostaInErrata(Path source, String dataConsegna, String cartellaTestata) {
        // Logica Files.move...
        try {
            Path dest = Paths.get(errataPrefix, "CONS_" + dataConsegna, cartellaTestata, source.getFileName().toString());
            Files.createDirectories(dest.getParent());
            Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Log.error("Impossibile spostare in errata " + source, e);
        }
    }

    private void spostaFileErrato(Path file, String dataConsegna, String testata, String dataEdiz) {
        try {
            Path destDir = Paths.get(errataPrefix, "CONS_" + dataConsegna, testata, dataEdiz);
            Files.createDirectories(destDir);
            Files.move(file, destDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Log.error("Errore spostamento file errato", e);
        }
    }

    //--- METODI DI PERSISTENZA LOG (Commentati per Push) ---
    private void salvaLogEdizione(Integer idLog, Path path, TipoEdizione tipo, ContatoriStorico ct, Integer idEdizione, String desc) {
        GdpLogEdizione logEdizione = new GdpLogEdizione();
        logEdizione.fkGdpLog = idLog;
        logEdizione.tipoEdizione = tipo;
        logEdizione.pathEdizione = path.toString();

        //NRO_PAG_ACQUISITE: 0 per edizioni errate
        logEdizione.nroPagAcquisite = (tipo == TipoEdizione.AS) ? 0 : (ct.pdfOk + ct.erratiTotali);

        //NRO_PAG_VALIDE: 0 per edizioni errate
        logEdizione.nroPagValide = (tipo == TipoEdizione.AS) ? 0 : ct.pdfOk;

        //NRO_PAG_ERRATE: [vuoto] per scecifica
        //Se tipo è AS o se non ci sono errori, mettiamo null
        logEdizione.nroPagErrate = (tipo == TipoEdizione.AS || ct.erratiTotali == 0) ? null : ct.erratiTotali;

        // Se abbiamo dettagli NF accumulati, li aggiungiamo alla descrizione
        if (ct.dettagliNF.length() > 0) {
            logEdizione.descrizione = desc + " | " + ct.dettagliNF.toString();
        } else {
            logEdizione.descrizione = desc;
        }

        logEdizione.fkGdpEdizione = idEdizione;
        logEdizioneRepository.persist(logEdizione);
    }

    private void registraEdizioneAnomala(Integer idLog, Path path, String errore) {
        ContatoriStorico vuoto = new ContatoriStorico();
        salvaLogEdizione(idLog, path, TipoEdizione.AS, vuoto, 0, errore);
    }

    private void aggiornaLogFinale(Integer idLog, String esito, String descrizioneFormattata) {
        GdpLog log = logRepository.findById(idLog);
        if (log != null) {
            log.esito = esito;
            logRepository.persist(log);
        }
        // Stampiamo il riepilogo richiesto dal documento nel log applicativo
        Log.infof("RIEPILOGO FINALE LOG %d: %s", idLog, descrizioneFormattata);
    }

    //Classe interna per i conteggi
    private static class ContatoriStorico {
        int pdfOk = 0, pdfKo = 0;
        int txtOk = 0, txtKo = 0;
        int tifOk = 0, tifKo = 0;
        int erratiTotali = 0; // Somma di tutti i KO per nroPagErrate
        StringBuilder dettagliNF = new StringBuilder();
    }

    private static class ContatoriGlobali {
        int nroEdizioni = 0;
        int nroEdizioniERR = 0;
        int pdfTot = 0, pdfKo = 0;
        int txtTot = 0, txtKo = 0;
        int tifTot = 0, tifKo = 0;
    }

    // Utility semplice
    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1);
    }
}
