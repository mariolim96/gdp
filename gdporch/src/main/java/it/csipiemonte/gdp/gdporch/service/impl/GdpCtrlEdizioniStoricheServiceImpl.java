package it.csipiemonte.gdp.gdporch.service.impl;

import io.quarkus.logging.Log;
import it.csipiemonte.gdp.gdporch.dto.EdizioneInsertResponse;
import it.csipiemonte.gdp.gdporch.dto.XmlCreationResponse;
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

        // Path da documento: _tmp/CONS_<dataConsegna>/<cartellaTestata>/
        Path rootTmpPath = Paths.get(tmpPrefix, "CONS_" + dataConsegna, cartellaTestata);

        if (!Files.exists(rootTmpPath)) {
            Log.errorf("Path non trovato: %s", rootTmpPath);
            // Gestire MSG00001 se necessario
            return;
        }

        try (Stream<Path> edizioniStream = Files.list(rootTmpPath)) {
            edizioniStream.filter(Files::isDirectory).forEach(pathEdizione -> {
                processaCartellaEdizione(pathEdizione, idTestata, cartellaTestata, dataConsegna, idLog);
            });

            // TODO: Step finale - Aggiornamento GDP_LOG con MSG00009 (Esito OK)
            // aggiornaLogFinale(idLog, "MSG00009");
            Log.info("F07 conclusa. Placeholder per aggiornaLogFinale.");
        } catch (Exception e) {
            Log.error("Errore scansione F07", e);
        }
    }

    /**
     * Metodo per l'analisi della sottocartella (Edizione)
     */
    private void processaCartellaEdizione(Path pathEdizione, Integer idTestata, String cartellaTestata, String dataConsegna, Integer idLog) {
        String nomeCartella = pathEdizione.getFileName().toString();
        LocalDate dataEdizione;

        // 1. Verificare se nomeCartella match con formato yyyyMMdd
        // Se Errato: Spostare in _errata, loggare TIPO_EDIZIONE = 'AS' (Anomala Storica) e passare oltre.
            try{
              dataEdizione = LocalDate.parse(nomeCartella, FOLDER_DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                Log.warnf("Data cartella non valida: %s. Spostamento in _errata.", nomeCartella);
                // registraEdizioneAnomala(idLog, pathEdizione, "Data cartella non valida: " + nomeCartella);
                spostaInErrata(pathEdizione, dataConsegna, cartellaTestata);
                return;
            }

        // 2.Validazione file interni con STORICO_FILE_PATTERN
        // Deve estrarre i gruppi dalla Regex e verificare che la data nel nome file
        // sia uguale alla data del nome cartella.
        ContatoriStorico ct = validaFileInterni(pathEdizione, cartellaTestata ,nomeCartella,dataConsegna );

        // 3. TODO: Check TIF Obbligatori (Nota 3)
        // Se dopo la scansione dei file, il conteggio dei TIF validi è 0,
        // l'edizione va scartata interamente (spostata in _errata).
            if(ct.tifOk == 0){
                Log.warnf("Edizione %s scartata: File TIF mancante.", nomeCartella);
                //registraEdizioneAnomala(idLog,pathEdizione, "NF - File TIF mancante (Obbligatorio per Storico)");
                spostaInErrata(pathEdizione,dataConsegna,cartellaTestata);
                return;
            }
          /* --- LOGICA F08-F09-F10 (Commentata per il Push) ---
        EdizioneInsertResponse res08 = edizioneService.insEdizione(idTestata,pathEdizione.toString(),dataEdizione,idLog);
            if(res08.getIdEdizione() != null) {
                // 5. Se F08 OK: Invocazione F09 (creaXMLEdizione) con PRIORITÀ 100
                XmlCreationResponse res09 = damTrasmissioneService.creaXMLEdizione(idTestata, idLog, res08.getIdEdizione(), 100);
                if("MSG00009".equals(res09.getCodice())) {
                    // ESITO POSITIVO F09 -> Richiamo ASINCRONO F10
                    // Passiamo nomeFileZIP restituito da F09
                    damTrasmissioneService.inviaEdizioneAsync(idLog,res08.getIdEdizione(),res09.getNomeFileCompresso());

                    // 6.  Persistenza record su GDP_LOG_EDIZIONE (TIPO_EDIZIONE = 'ST')
                    salvaLogEdizione(idLog,pathEdizione, TipoEdizione.ST, ct, res08.getIdEdizione(), "");
                }else{
                    //Esito negativo F09, sposto in cartella -errata
                    spostaInErrata(pathEdizione, dataConsegna, cartellaTestata);

                    //Salvataggio log con MSG0004 e massaggio di errore f09
                    String descErrore = "MSG00004 - Errore f09: " + res09.getMessaggio();
                    registraEdizioneAnomala(idLog, pathEdizione, descErrore);
                }
            }else{
            String msgMsg = (res08 != null) ? res08.getMessaggio() : "Risposta nulla";
                registraEdizioneAnomala(idLog,pathEdizione,"Errore f08: "+ msgMsg);
                spostaInErrata(pathEdizione,dataConsegna,cartellaTestata);
            }
            --- FINE LOGICA F08-F10 --- */
        Log.infof("F07: Edizione %s analizzata (Logica di invio commentata)", nomeCartella);

    }

    /**
     *  Metodo per validazione naming e incrocio dati (Regex + Nome Cartella)
     */
    private ContatoriStorico validaFileInterni(Path pathEdizione, String testataAttesa,String dataAttesa, String dataConsegna){
        ContatoriStorico ct = new ContatoriStorico();
        try(Stream<Path> files = Files.list(pathEdizione)){
            files.filter(Files::isRegularFile).forEach(file ->{
                String fileName = file.getFileName().toString();
                Matcher matcher = STORICO_FILE_PATTERN.matcher(fileName);

                if(matcher.matches() && matcher.group(1).equalsIgnoreCase(testataAttesa) && matcher.group(3).equals(dataAttesa)){
                    String ext = matcher.group(5).toLowerCase();
                    switch (ext) {
                        case "pdf" -> ct.pdfOk++;
                        case "txt" -> ct.txtOk++;
                        case "tif" -> ct.tifOk++;
                    }
                }else{
                    ct.errati++;
                    //Spostamento singolo file non conforme
                    spostaFileErrato(file,dataConsegna,testataAttesa,dataAttesa);
                }
            });
        }catch(IOException e){
            Log.error("Errore lettura file edizione", e);
        }
        return ct;
    }

    /**
     *  Metodo per spostamento fisico in _errata
     * Deve mantenere la struttura /_errata/CONS_<dataConsegna>/<cartellaTestata>/<edizione>
     */
    private void spostaInErrata(Path source, String dataConsegna, String cartellaTestata) {
        // Logica Files.move...
        try{
            Path dest = Paths.get(errataPrefix, "CONS_"+ dataConsegna, cartellaTestata, source.getFileName().toString());
            Files.createDirectories(dest.getParent());
            Files.move(source,dest, StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){
            Log.error("Impossibile spostare in errata "+ source ,e );
        }
    }

    private void spostaFileErrato(Path file, String dataConsegna, String testata, String dataEdiz){
        try{
            Path destDir = Paths.get(errataPrefix, "CONS_"+ dataConsegna, testata, dataEdiz);
            Files.createDirectories(destDir);
            Files.move(file,destDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){
            Log.error("Errore spostamento file errato", e);
        }
    }

    /* --- METODI DI PERSISTENZA LOG (Commentati per Push) ---
    private void salvaLogEdizione(Integer idLog, Path path, TipoEdizione tipo, ContatoriStorico ct, Integer idEdizione, String desc) {
        GdpLogEdizione logEdizione = new GdpLogEdizione();
        logEdizione.fkGdpLog = idLog;
        logEdizione.tipoEdizione = tipo;
        logEdizione.pathEdizione = path.toString();
        logEdizione.nroPagAcquisite = ct.pdfOk + ct.errati;
        logEdizione.nroPagValide = ct.pdfOk;
        logEdizione.nroPagErrate = ct.errati;
        logEdizione.descrizione = desc;
        logEdizione.fkGdpEdizione = idEdizione;
        logEdizioneRepository.persist(logEdizione);
    }

    private void registraEdizioneAnomala(Integer idLog, Path path, String errore) {
        ContatoriStorico vuoto = new ContatoriStorico();
        salvaLogEdizione(idLog, path, TipoEdizione.AS, vuoto, 0, errore);
    }

    private void aggiornaLogFinale(Integer idLog, String esito) {
        GdpLog log = logRepository.findById(idLog);
        if (log != null) {
            log.esito = esito;
            logRepository.persist(log);
        }
    }
    */

    //Classe interna per i conteggi
    private static class ContatoriStorico{
        int pdfOk = 0;
        int txtOk = 0;
        int tifOk = 0;
        int errati = 0;
    }
}
