package it.csipiemonte.gdp.gdporch.service;

import com.jcraft.jsch.ChannelSftp;
import it.csipiemonte.gdp.gdporch.dto.XmlCreationRequest;
import it.csipiemonte.gdp.gdporch.dto.XmlCreationResponse;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import it.csipiemonte.gdp.gdporch.model.entity.*;
import it.csipiemonte.gdp.gdporch.model.enums.StatoCodaCaricamento;
import it.csipiemonte.gdp.gdporch.model.repository.*;
import it.csipiemonte.gdp.gdporch.model.xml.EdizioneXml;
import it.csipiemonte.gdp.gdporch.model.xml.EdizioneXmlMapper;
import it.csipiemonte.gdp.sftp.SftpClientProducer;
import it.csipiemonte.gdp.sftp.SftpSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.io.*;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Servizio DAMtrasmissione - Operazione F09
 * Crea il pacchetto XML + PDF/TXT e lo deposita per il DAM.
 */
@ApplicationScoped
public class CreaXmlEdizioneJob {

    private final GdpTestataRepository testataRepository;
    private final GdpEdizioneRepository edizioneRepository;
    private final GdpPaginaRepository paginaRepository;
    private final GdpLogEdizioneRepository logEdizioneRepository;
    private final EdizioneXmlMapper edizioneXmlMapper;
    private final SftpClientProducer sftpProducer;
    private final GdpCodaCaricamentoRepository codaCaricamentoRepository;

    @ConfigProperty(name = "sftp.root.prefix.dam")
    String damPrefix;

    public CreaXmlEdizioneJob(GdpTestataRepository testataRepository,
                              GdpEdizioneRepository edizioneRepository,
                              GdpPaginaRepository paginaRepository,
                              GdpLogEdizioneRepository logEdizioneRepository,
                              EdizioneXmlMapper edizioneXmlMapper,
                              SftpClientProducer sftpProducer,
                              GdpCodaCaricamentoRepository codaCaricamentoRepository) {
        this.testataRepository = testataRepository;
        this.edizioneRepository = edizioneRepository;
        this.paginaRepository = paginaRepository;
        this.logEdizioneRepository = logEdizioneRepository;
        this.edizioneXmlMapper = edizioneXmlMapper;
        this.sftpProducer = sftpProducer;
        this.codaCaricamentoRepository = codaCaricamentoRepository;
    }

    /**
     * Esegue l'intero flusso della F09 usando GdpMessage per i codici e i messaggi.
     */
    public XmlCreationResponse creaXMLEdizione(XmlCreationRequest body) {

        XmlCreationResponse response = new XmlCreationResponse();

        //Recuperiamo i dati
        GdpLogEdizione logEdizione = logEdizioneRepository.findById(body.getIdLog());
        GdpTestata testata = testataRepository.findById(body.getIdTestata());
        GdpEdizione edizione = edizioneRepository.findByData(body.getDataEdizione());

        // Se l'edizione non esiste, usiamo il messaggio di errore dell'Enum
        if (edizione == null) {
            gestisciErroreXml(logEdizione);
            return response
                    .codice(GdpMessage.F09_XML_CREATION_FAILED.getCodice())
                    .messaggio(GdpMessage.F09_XML_CREATION_FAILED.getDescrizioneDefault());
        }

        List<GdpPagina> pagine = paginaRepository.findByEdizione(edizione.id);

        // Generiamo XML
        byte[] xmlContent;
        try {
            xmlContent = generaContenutoXml(testata, edizione, pagine);
        } catch (Exception e) {
            gestisciErroreXml(logEdizione);
            return response
                    .codice(GdpMessage.F09_XML_CREATION_FAILED.getCodice())
                    .messaggio(GdpMessage.F09_XML_CREATION_FAILED.getDescrizioneDefault());
        }

        // Creazione ZIP e invio SFTP
        String nomeFileZip;
        try {
            nomeFileZip = creaEInviaPacchettoZip(testata, edizione, logEdizione, xmlContent);
        } catch (Exception e) {
            gestisciErroreZip(logEdizione);
            return response
                    .codice(GdpMessage.F09_ZIP_CREATION_FAILED.getCodice())
                    .messaggio(GdpMessage.F09_ZIP_CREATION_FAILED.getDescrizioneDefault());
        }

        // Successo
        logEdizione.fileXml = true;
        logEdizione.fileZip = true;
        logEdizioneRepository.persist(logEdizione);

        inserisciCodaCaricamento(logEdizione, nomeFileZip, body.getPriorita().value());

        // Risposta OK finale sempre dall'Enum
        return response
                .codice(GdpMessage.F09_XML_OK.getCodice())
                .messaggio(GdpMessage.F09_XML_OK.getDescrizioneDefault())
                .nomeFileCompresso(nomeFileZip);
    }

    // Metodi privati di supporto

    private byte[] generaContenutoXml(GdpTestata t, GdpEdizione e, List<GdpPagina> p) throws JAXBException {
        EdizioneXml model = edizioneXmlMapper.toEdizioneXml(t, e, p);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JAXBContext context = JAXBContext.newInstance(EdizioneXml.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.marshal(model, baos);
        return baos.toByteArray();
    }

    private String creaEInviaPacchettoZip(GdpTestata t, GdpEdizione e, GdpLogEdizione log, byte[] xmlBytes) throws Exception {
        String dataEdizioneStr = e.dataEdizione.toString();
        String nomeZip = String.format("%s.%s.zip", t.nomeTestata, dataEdizioneStr);
        String pathDam = damPrefix + nomeZip;

        try (SftpSession session = sftpProducer.connect()) {
            ChannelSftp sftp = session.getChannel();
            ByteArrayOutputStream baosZip = new ByteArrayOutputStream();

            try (ZipOutputStream zos = new ZipOutputStream(baosZip)) {
                // XML interno obbligatorio come "edizione.xml"
                zos.putNextEntry(new ZipEntry("edizione.xml"));
                zos.write(xmlBytes);
                zos.closeEntry();

                // File PDF e TXT
                List<ChannelSftp.LsEntry> entries = sftp.ls(log.pathEdizione);
                for (ChannelSftp.LsEntry fileRemoto : entries) {
                    String fileName = fileRemoto.getFilename();
                    if (isMediaFile(fileName)) {
                        zos.putNextEntry(new ZipEntry(fileName));
                        try (InputStream is = sftp.get(log.pathEdizione + "/" + fileName)) {
                            is.transferTo(zos);
                        }
                        zos.closeEntry();
                    }
                }
            }
            // Caricamento su SFTP
            try (InputStream isZip = new ByteArrayInputStream(baosZip.toByteArray())) {
                sftp.put(isZip, pathDam);
            }
        }
        return nomeZip;
    }

    private void inserisciCodaCaricamento(GdpLogEdizione logEdizione, String nomeZip, Integer priorita) {
        GdpCodaCaricamento coda = new GdpCodaCaricamento();
        coda.dataInserimento    = LocalDate.now();
        coda.fkGdpLogEdizione   = logEdizione.id;
        coda.nroTentativo       = 0;
        coda.sftpPath           = damPrefix + nomeZip;
        coda.priorita           = priorita;
        coda.stato              = StatoCodaCaricamento.PRO;
        codaCaricamentoRepository.persist(coda);
    }

    private boolean isMediaFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".txt");
    }

    private void gestisciErroreXml(GdpLogEdizione log) {
        if (log != null) {
            log.fileXml = false;
            logEdizioneRepository.persist(log);
        }
    }

    private void gestisciErroreZip(GdpLogEdizione log) {
        if (log != null) {
            log.fileZip = false;
            logEdizioneRepository.persist(log);
        }
    }
}