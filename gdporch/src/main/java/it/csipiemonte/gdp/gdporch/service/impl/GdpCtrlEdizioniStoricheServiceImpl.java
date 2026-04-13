package it.csipiemonte.gdp.gdporch.service.impl;

import io.quarkus.logging.Log;
import it.csipiemonte.gdp.gdporch.dto.EdizioneInsertResponse;
import it.csipiemonte.gdp.gdporch.dto.XmlCreationResponse;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import it.csipiemonte.gdp.gdporch.model.entity.*;
import it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione;
import it.csipiemonte.gdp.gdporch.model.repository.*;
import it.csipiemonte.gdp.gdporch.service.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


 //Servizio per il controllo e lo smistamento delle edizioni storiche (F07).
 //Valida la struttura delle cartelle, il naming dei file e inserisce i dati nel sistema.
@ApplicationScoped
public class GdpCtrlEdizioniStoricheServiceImpl implements GdpCtrlEdizioniStoricheService {

    // Regex per validare il naming file secondo Nota 3: <testata>-<sigla>-<data>_<pag>.<ext>
    private static final Pattern STORICO_FILE_PATTERN =
            Pattern.compile("^(.+?)-([A-Z]{2})-(\\d{8})_(\\d{3})\\.(pdf|txt|tif)$", Pattern.CASE_INSENSITIVE);

    // Formato atteso per le cartelle delle edizioni (yyyyMMdd)
    private static final DateTimeFormatter FOLDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @ConfigProperty(name = "sftp.root.prefix.tmp") String tmpPrefix;
    @ConfigProperty(name = "sftp.root.prefix.errata") String errataPrefix;

    @Inject GdpLogRepository logRepository;
    @Inject GdpLogEdizioneRepository logEdizioneRepository;
    @Inject GdpUtenteSftpRepository utenteSftpRepository;
    @Inject GdpEdizioneService edizioneService;
    @Inject DamTrasmissioneService damTrasmissioneService;

    // Self-injection necessaria per invocare metodi @Transactional nello stesso bean
    @Inject GdpCtrlEdizioniStoricheServiceImpl self;

     // DTO interno per trasportare i metadati della consegna senza appesantire le firme dei metodi.
    private record Context(Integer idTestata, String cartellaTestata, String dataConsegna, Integer idLog, String utenteSftp) {}

     // Entry point principale del servizio F07.
    @Override
    public void ctrlEdizioniStoriche(Integer idTestata, String cartellaTestata, String dataConsegna, Integer idLog) {
        // 1. Identifica l'utente proprietario per gestire correttamente i percorsi SFTP
        String utenteSftp = recuperaUsernameDaLog(idLog);
        if (utenteSftp == null) return;

        // 2. Definisce il path di partenza nella TMP
        Path rootTmpPath = Paths.get(tmpPrefix, utenteSftp, dataConsegna, cartellaTestata);
        if (!Files.exists(rootTmpPath)) return;

        Context ctx = new Context(idTestata, cartellaTestata, dataConsegna, idLog, utenteSftp);
        ContatoriGlobali cg = new ContatoriGlobali();

        // 3. Scansiona le sottocartelle (ogni cartella è un'edizione)
        try (Stream<Path> edizioniStream = Files.list(rootTmpPath)) {
            edizioniStream.filter(Files::isDirectory).forEach(path -> {
                cg.nroEdizioni++;
                // Chiamata al metodo transazionale per isolare l'elaborazione della singola edizione
                self.processaCartellaEdizione(path, ctx, cg);
            });
            // 4. Aggiorna il log principale con il riepilogo finale (MSG00009)
            concludiElaborazione(ctx, cg);
        } catch (IOException e) {
            Log.error("Errore critico durante la scansione della TMP", e);
        }
    }

     //Elabora la singola cartella edizione. Ogni edizione è gestita in una transazione separata.
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void processaCartellaEdizione(Path pathEdiz, Context ctx, ContatoriGlobali cg) {
        String nomeCartella = pathEdiz.getFileName().toString();
        LocalDate dataEdiz;

        // --- STEP 1: VALIDAZIONE DATA CARTELLA ---
        try {
            dataEdiz = LocalDate.parse(nomeCartella, FOLDER_DATE_FORMATTER);
        } catch (Exception e) {
            cg.nroEdizioniERR++;
            // Se la data è errata, l'edizione è "Anomala Storica" (AS) e viene spostata in errata
            registraESposta(pathEdiz, ctx, TipoEdizione.AS, null,
                    buildMsg(GdpMessage.F07_WRONG_DATE_FORMAT, cg.nroEdizioniERR, nomeCartella, errataPrefix, ctx.dataConsegna, ctx.cartellaTestata, nomeCartella));
            return;
        }

        // --- STEP 2: VALIDAZIONE FILE INTERNI ---
        ContatoriStorico ct = validaFileInterni(pathEdiz, ctx, nomeCartella);
        cg.aggiorna(ct); // Somma i contatori dell'edizione a quelli globali della testata

        // --- STEP 3: VERIFICA OBBLIGATORIETÀ TIF ---
        if (ct.tifOk == 0) {
            // Se mancano i TIF validi, l'edizione viene scartata totalmente (Pag. 19)
            String msg = buildMsg(GdpMessage.F07_MOVE_ERRATA, ctx.idLog, errataPrefix, ctx.dataConsegna, ctx.cartellaTestata, nomeCartella) + " | NF - file TIF mancante";
            registraESposta(pathEdiz, ctx, TipoEdizione.AS, ct, msg);
            return;
        }

        // --- STEP 4: PERSISTENZA E LOGICA F08 ---
        EdizioneInsertResponse res08 = edizioneService.insEdizione(ctx.idTestata, pathEdiz.toAbsolutePath().toString(), dataEdiz, ctx.idLog);

        if (res08 != null && res08.getIdEdizione() != null) {
            // Edizione corretta: TIPO_EDIZIONE = ST
            salvaLogEdizione(ctx.idLog, pathEdiz, TipoEdizione.ST, ct, res08.getIdEdizione(), GdpMessage.F_OK.getDescrizioneDefault());
           //---STEP 5:F09/F10
            try {

                // Invocazione sincrona F09 con Priorità 100
                XmlCreationResponse res09 = damTrasmissioneService.creaXMLEdizione(ctx.idTestata, ctx.idLog, res08.getIdEdizione(), 100);

                if (res09 != null && GdpMessage.F_OK.getCodice().equals(res09.getCodice())) {
                    // Esito Positivo F09 -> Invocazione Asincrona F10
                    damTrasmissioneService.inviaEdizioneAsync(ctx.idLog, res08.getIdEdizione(), res09.getNomeFileCompresso());
                } else {
                    // Esito Negativo F09
                    String erroreF09 = (res09 != null) ? res09.getMessaggio() : "Risposta nulla da F09";
                    String msgErrF09 = "MSG00004 - Errore in F09: " + erroreF09;
                    registraESposta(pathEdiz, ctx, TipoEdizione.AS, ct, msgErrF09);
                }
            } catch (Exception e) {
                Log.error("Errore tecnico durante invocazione F09/F10 per edizione: " + nomeCartella, e);
                registraESposta(pathEdiz, ctx, TipoEdizione.AS, ct, "Errore tecnico integrazione flussi downstream");
            }
        } else {
            // Errore durante l'inserimento DB (F08 fallito)
            String err = (res08 != null) ? res08.getMessaggio() : "Risposta nulla F08";
            registraESposta(pathEdiz, ctx, TipoEdizione.AS, ct, buildMsg(GdpMessage.F07_DB_ERROR, err, dataEdiz, ctx.idTestata, ctx.cartellaTestata));
        }
    }

     // Analizza i singoli file PDF, TXT, TIF verificando regex e coerenza dati.
    private ContatoriStorico validaFileInterni(Path pathEdiz, Context ctx, String dataAttesa) {
        ContatoriStorico ct = new ContatoriStorico();
        try (Stream<Path> files = Files.list(pathEdiz)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                String fileName = file.getFileName().toString();
                Matcher m = STORICO_FILE_PATTERN.matcher(fileName);

                // Verifica: Regex corretta + Testata corretta + Data file coerente con cartella
                if (m.matches() && m.group(1).equalsIgnoreCase(ctx.cartellaTestata) && m.group(3).equals(dataAttesa)) {
                    ct.incrementaOk(fileName.substring(fileName.lastIndexOf('.') + 1));
                } else {
                    // File non conforme (NF): va spostato singolarmente in _errata
                    ct.incrementaKo(fileName.substring(fileName.lastIndexOf('.') + 1), fileName);
                    spostaFile(file, Paths.get(errataPrefix, ctx.utenteSftp, ctx.dataConsegna, ctx.cartellaTestata, dataAttesa));
                }
            });
        } catch (IOException e) { Log.error("Errore lettura file nell'edizione " + dataAttesa, e); }
        return ct;
    }

     // Metodo centralizzato per gestire lo scarto di un'edizione: salva il log e sposta la cartella in _errata.
    private void registraESposta(Path path, Context ctx, TipoEdizione tipo, ContatoriStorico ct, String msg) {
        salvaLogEdizione(ctx.idLog, path, tipo, ct != null ? ct : new ContatoriStorico(), null, msg);
        try {
            Path dest = Paths.get(errataPrefix, ctx.utenteSftp, ctx.dataConsegna, ctx.cartellaTestata, path.getFileName().toString());
            Files.createDirectories(dest.getParent());
            Files.move(path, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { Log.error("Impossibile spostare l'edizione in errata: " + path, e); }
    }

     // Sposta fisicamente un file singolo (usato per file non conformi).
    private void spostaFile(Path file, Path destDir) {
        try {
            Files.createDirectories(destDir);
            Files.move(file, destDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { Log.error("Errore spostamento file non conforme: " + file, e); }
    }

     //Crea il record nella tabella GDP_LOG_EDIZIONE.
    private void salvaLogEdizione(Integer idLog, Path path, TipoEdizione tipo, ContatoriStorico ct, Integer idEdiz, String desc) {
        GdpLogEdizione le = new GdpLogEdizione();
        le.fkGdpLog = idLog;
        le.tipoEdizione = tipo;
        le.pathEdizione = path.toString();
        // Per le edizioni errate (AS) i contatori pagine sono forzati a 0
        le.nroPagAcquisite = (tipo == TipoEdizione.AS) ? 0 : (ct.pdfOk + ct.pdfKo);
        le.nroPagValide = (tipo == TipoEdizione.AS) ? 0 : ct.pdfOk;
        le.nroPagErrate = (tipo == TipoEdizione.AS || ct.erratiTotali == 0) ? null : ct.erratiTotali;
        // Accoda i dettagli dei file non conformi (NF) alla descrizione
        le.descrizione = ct.dettagliNF.length() > 0 ? desc + " | " + ct.dettagliNF : desc;
        le.fkGdpEdizione = idEdiz;
        logEdizioneRepository.persist(le);
    }

     //Formatta il riepilogo finale dell'elaborazione per il log principale.
    private void concludiElaborazione(Context ctx, ContatoriGlobali cg) {
        String riepilogo = String.format(GdpMessage.F07_OK.getDescrizioneDefault(),
                ctx.idTestata, ctx.cartellaTestata, cg.nroEdizioni,
                (cg.pdfTot + cg.pdfKo), cg.pdfKo, (cg.txtTot + cg.txtKo), cg.txtKo, (cg.tifTot + cg.tifKo), cg.tifKo);
        aggiornaLogFinale(ctx.idLog, GdpMessage.F_OK.getCodice(), riepilogo);
    }

     // Helper per costruire messaggi formattati dall'enum GdpMessage.
    private String buildMsg(GdpMessage msg, Object... args) {
        return String.format(msg.getDescrizioneDefault(), args);
    }

     // Risale allo username SFTP partendo dall'ID del log.
    private String recuperaUsernameDaLog(Integer idLog) {
        GdpLog log = logRepository.findById(idLog);
        if (log == null || log.fkGdpUtenteFtp == null) return null;
        GdpUtenteSftp u = utenteSftpRepository.findById(log.fkGdpUtenteFtp);
        return (u != null) ? u.username : null;
    }

     // Aggiorna lo stato e la descrizione del log principale (GDP_LOG).
    private void aggiornaLogFinale(Integer idLog, String esito, String desc) {
        GdpLog log = logRepository.findById(idLog);
        if (log != null) { log.esito = esito; logRepository.persist(log); }
        Log.infof("F07 conclusa. Riepilogo per Log %d: %s", idLog, desc);
    }

     // Modello interno per gestire i contatori della singola edizione.
    private static class ContatoriStorico {
        int pdfOk, pdfKo, txtOk, txtKo, tifOk, tifKo, erratiTotali;
        StringBuilder dettagliNF = new StringBuilder(); // Accumula i nomi dei file con naming errato

        void incrementaOk(String ext) {
            if ("pdf".equalsIgnoreCase(ext)) pdfOk++;
            else if ("txt".equalsIgnoreCase(ext)) txtOk++;
            else if ("tif".equalsIgnoreCase(ext)) tifOk++;
        }

        void incrementaKo(String ext, String name) {
            erratiTotali++;
            if ("pdf".equalsIgnoreCase(ext)) pdfKo++;
            else if ("txt".equalsIgnoreCase(ext)) txtKo++;
            else if ("tif".equalsIgnoreCase(ext)) tifKo++;
            // Aggiunge il file alla lista dei non conformi per il log
            if (dettagliNF.length() > 0) dettagliNF.append("; ");
            dettagliNF.append("NF - ").append(name);
        }
    }

     // Modello interno per gestire i contatori totali della testata (MSG00009).
    private static class ContatoriGlobali {
        int nroEdizioni, nroEdizioniERR, pdfTot, pdfKo, txtTot, txtKo, tifTot, tifKo;

        /** Somma i risultati dell'edizione corrente ai totali generali */
        void aggiorna(ContatoriStorico ct) {
            pdfTot += ct.pdfOk; pdfKo += ct.pdfKo;
            txtTot += ct.txtOk; txtKo += ct.txtKo;
            tifTot += ct.tifOk; tifKo += ct.tifKo;
        }
    }
}