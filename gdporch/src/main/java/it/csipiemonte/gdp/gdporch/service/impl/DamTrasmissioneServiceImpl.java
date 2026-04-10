package it.csipiemonte.gdp.gdporch.service.impl;

import it.csipiemonte.gdp.gdporch.dto.XmlCreationResponse;
import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import it.csipiemonte.gdp.gdporch.model.entity.GdpEdizione;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPagina;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLogEdizione;
import it.csipiemonte.gdp.gdporch.model.entity.GdpCodaCaricamento;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpEdizioneRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpPaginaRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogEdizioneRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpCodaCaricamentoRepository;
import it.csipiemonte.gdp.gdporch.model.enums.StatoCodaCaricamento;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import it.csipiemonte.gdp.gdporch.service.DamTrasmissioneService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@ApplicationScoped
public class DamTrasmissioneServiceImpl implements DamTrasmissioneService {

    private static final Logger LOG = Logger.getLogger(DamTrasmissioneServiceImpl.class);

    @ConfigProperty(name = "sftp.root.prefix.tmp", defaultValue = "_tmp/")
    String tmpPrefix;

    @ConfigProperty(name = "sftp.root.prefix.dam", defaultValue = "_dam/")
    String damPrefix;

    @Inject
    GdpTestataRepository testataRepository;

    @Inject
    GdpEdizioneRepository edizioneRepository;

    @Inject
    GdpPaginaRepository paginaRepository;

    @Inject
    GdpLogEdizioneRepository logEdizioneRepository;

    @Inject
    GdpCodaCaricamentoRepository codaCaricamentoRepository;

    @Override
    @Transactional
    public XmlCreationResponse creaXMLEdizione(Integer idTestata, Integer idLog, Integer idEdizione, Integer priorita,String pathEdizione) {
        LOG.infof("Avvio F09 - DAMtrasmissione.creaXMLEdizione idEdizione %d,path %s\"", idEdizione,pathEdizione);

        XmlCreationResponse response = new XmlCreationResponse();

        try {
            GdpTestata testata = testataRepository.findById(idTestata);
            GdpEdizione edizione = edizioneRepository.findById(idEdizione);
            List<GdpPagina> pagine = paginaRepository.list("fkGdpEdizione", idEdizione);

            if (testata == null || edizione == null) {
                response.setCodice(GdpMessage.F_NOT_FOUND.getCodice());
                response.setMessaggio(GdpMessage.F_NOT_FOUND.getDescrizioneDefault());
                return response;
            }

            // Generate XML
            String xmlContent = generateXml(testata, edizione, pagine);
            String zipName = String.format("%s.%s.zip", testata.cartellaTestata, edizione.dataEdizione.toString());

            // Files to ZIP (staged in _tmp)
            Path sourceDir = Paths.get(tmpPrefix, testata.cartellaTestata, edizione.dataEdizione.toString());
            Path destZip = Paths.get(damPrefix, zipName);

            if (!Files.exists(Paths.get(damPrefix))) {
                Files.createDirectories(Paths.get(damPrefix));
            }

            // Create ZIP
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destZip.toFile()))) {
                // Add XML
                ZipEntry xmlEntry = new ZipEntry(zipName.replace(".zip", ".xml"));
                zos.putNextEntry(xmlEntry);
                zos.write(xmlContent.getBytes());
                zos.closeEntry();

                // Add PDFs and TXTs
                for (GdpPagina p : pagine) {
                    addFileToZip(zos, sourceDir.resolve(p.filePdf));
                    if (p.fileTxt != null) {
                        addFileToZip(zos, sourceDir.resolve(p.fileTxt));
                    }
                }
            }

            // Update GDP_LOG_EDIZIONE
            Optional<GdpLogEdizione> logEdOpt = logEdizioneRepository.find("fkGdpLog = ?1 and pathEdizione = ?2", idLog,pathEdizione)
                    .firstResultOptional();
            if (logEdOpt.isPresent()) {
                GdpLogEdizione logEd = logEdOpt.get();
                logEd.fileXml = true;
                logEd.fileZip = true;

                // Insert Import Task (F10)
                GdpCodaCaricamento task = new GdpCodaCaricamento();
                task.fkGdpLogEdizione = logEd.id;
                task.dataInserimento = LocalDate.now();
                task.nroTentativo = 0;
                task.sftpPath = "/" + damPrefix + zipName;
                task.priorita = priorita;
                task.stato = StatoCodaCaricamento.PRO;
                codaCaricamentoRepository.persist(task);
            }

            response.setCodice(GdpMessage.F_OK.getCodice());
            response.setNomeFileCompresso(zipName);
            response.setMessaggio(GdpMessage.F_OK.getDescrizioneDefault());

        } catch (Exception e) {
            LOG.error("Errore creazione XML/ZIP", e);
            response.setCodice(GdpMessage.F09_ZIP_CREATION_FAILED.getCodice());
            response.setMessaggio(GdpMessage.F09_ZIP_CREATION_FAILED.getDescrizioneDefault() + ": " + e.getMessage());
        }

        return response;
    }


    private String generateXml(GdpTestata t, GdpEdizione e, List<GdpPagina> pagine) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<gdp_import>\n");
        sb.append("  <testata id=\"").append(t.id).append("\" nome=\"").append(t.nomeTestata).append("\"/>\n");
        sb.append("  <edizione data=\"").append(e.dataEdizione).append("\" pagine=\"").append(e.totalePagine)
                .append("\"/>\n");
        sb.append("  <pagine>\n");
        for (GdpPagina p : pagine) {
            sb.append("    <pagina numero=\"").append(p.numPagina).append("\" file=\"").append(p.filePdf)
                    .append("\"/>\n");
        }
        sb.append("  </pagine>\n");
        sb.append("</gdp_import>");
        return sb.toString();
    }

    private void addFileToZip(ZipOutputStream zos, Path filePath) throws IOException {
        if (!Files.exists(filePath))
            return;
        ZipEntry entry = new ZipEntry(filePath.getFileName().toString());
        zos.putNextEntry(entry);
        Files.copy(filePath, zos);
        zos.closeEntry();
    }


    @Override
    public void inviaEdizioneAsync(Integer idLog, Integer idEdizione, String nomeFileCompresso) {
        LOG.infof("F10 - Richiesta invio asincrono ricevuta per Log: %d, Edizione: %d, File: %s",
                idLog, idEdizione, nomeFileCompresso);
        // Qui in futuro potresti aggiungere l'annotazione @Asynchronous
    }

}
