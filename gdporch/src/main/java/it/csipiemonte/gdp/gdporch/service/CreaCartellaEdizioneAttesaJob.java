package it.csipiemonte.gdp.gdporch.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import it.csipiemonte.gdp.gdporch.model.entity.GdpUtenteSftp;
import it.csipiemonte.gdp.gdporch.model.repository.GdpDataUscitaRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpPeriodicitaRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpUtenteSftpRepository;
import it.csipiemonte.gdp.sftp.SftpClientProducer;
import it.csipiemonte.gdp.sftp.SftpSession;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class CreaCartellaEdizioneAttesaJob {

    private static final Logger LOG = Logger.getLogger(CreaCartellaEdizioneAttesaJob.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Inject
    GdpDataUscitaRepository dataUscitaRepository;

    @Inject
    GdpPeriodicitaRepository periodicitaRepository;

    @Inject
    GdpTestataRepository testataRepository;

    @Inject
    GdpUtenteSftpRepository utenteSftpRepository;

    @Inject
    SftpClientProducer sftpClientProducer;

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "sftp.root.prefix", defaultValue = "")
    String sftpRootPrefix;

    //@Scheduled(cron = "0/10 * * * * ?", identity = "F02-creaCartellaDomani")
    @Transactional
    public void execute() {
        LOG.info("Inizio job F02 - creaCartellaEdizioneAttesa");

        List<GdpDataUscita> expectedEditions = dataUscitaRepository.findExpectedTomorrow();

        if (expectedEditions.isEmpty()) {
            LOG.infof("Nessuna edizione attesa per domani (%s)", GdpMessage.F02_NO_OCCURRENCES.getCodice());
            return;
        }

        try (SftpSession sftpSession = sftpClientProducer.connect()) {
            ChannelSftp channel = sftpSession.getChannel();

            int successCount = 0;
            for (GdpDataUscita dataUscita : expectedEditions) {
                if (processEdition(channel, dataUscita)) {
                    successCount++;
                }
            }

            if (successCount == expectedEditions.size()) {
                LOG.infof("Job F02 - Esito OK (%s)", GdpMessage.F02_OK.getCodice());
            } else {
                LOG.warnf("Job F02 completato con %d errori su %d (%s parziale)", expectedEditions.size() - successCount, expectedEditions.size(), GdpMessage.F02_OK.getCodice());
            }
        } catch (Exception e) {
            LOG.error("Errore durante l'esecuzione del job F02", e);
        }
    }

    private boolean processEdition(ChannelSftp channel, GdpDataUscita dataUscita) {
        try {
            GdpPeriodicita periodicita = periodicitaRepository.findById(dataUscita.fkGdpPeriodicita);
            if (periodicita == null) {
                LOG.warnf("Periodicita non trovata per id %d", dataUscita.fkGdpPeriodicita);
                return false;
            }

            GdpTestata testata = testataRepository.findById(periodicita.fkGdpTestata);
            if (testata == null) {
                LOG.warnf("Testata non trovata per id %d", periodicita.fkGdpTestata);
                return false;
            }

            // Se la cartella testata è null, usiamo il nome della testata (pulito) come fallback
            String cartellaTestata = testata.cartellaTestata;
            if (cartellaTestata == null || cartellaTestata.trim().isEmpty()) {
                LOG.warnf("CARTELLA_TESTATA mancante per testata %s, uso fallback...", testata.nomeTestata);
                cartellaTestata = testata.nomeTestata.toLowerCase().replaceAll("[^a-z0-9]", "_");
            }

            GdpUtenteSftp utenteSftp = utenteSftpRepository.findByRifTestata(testata.id.toString());
            if (utenteSftp == null) {
                LOG.warnf("Utente SFTP non trovato per testata %s", testata.nomeTestata);
                return false;
            }

            String dataStr = dataUscita.dataAttesa.format(DATE_FORMATTER);

            // Costruiamo il percorso: [prefix]/<cartellaTestata>/<dataStr>
            // Esempio: flusso_regolare/la-sentinella-del-canavese/2024-01-15
            String fullPath = String.format("%s%s/%s", sftpRootPrefix, cartellaTestata, dataStr);
            
            LOG.infof("Creazione cartella SFTP: %s", fullPath);
            createRecursiveDir(channel, fullPath);
            return true;

        } catch (Exception e) {
            LOG.errorf(e, "Errore durante la creazione della cartella per l'uscita %d", dataUscita.id);
            return false;
        }
    }

    private void createRecursiveDir(ChannelSftp channel, String path) throws SftpException {
        String[] folders = path.split("/");
        StringBuilder currentPath = new StringBuilder();
        for (String folder : folders) {
            if (folder.isEmpty()) {
                currentPath.append("/");
                continue;
            }
            if (!currentPath.toString().endsWith("/")) {
                currentPath.append("/");
            }
            currentPath.append(folder);
            try {
                channel.cd(currentPath.toString());
            } catch (SftpException e) {
                LOG.debugf("Creazione sottodirectory: %s", currentPath.toString());
                channel.mkdir(currentPath.toString());
                channel.cd(currentPath.toString());
            }
        }
    }
}
