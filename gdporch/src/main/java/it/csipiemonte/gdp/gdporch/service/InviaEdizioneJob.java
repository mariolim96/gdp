package it.csipiemonte.gdp.gdporch.service;

import com.jcraft.jsch.ChannelSftp;
import it.csipiemonte.gdp.gdporch.client.LibraClient;
import it.csipiemonte.gdp.gdporch.dto.LibraImportResponse;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import it.csipiemonte.gdp.gdporch.model.entity.GdpCodaCaricamento;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLog;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLogEdizione;
import it.csipiemonte.gdp.gdporch.model.enums.StatoCodaCaricamento;
import it.csipiemonte.gdp.gdporch.model.repository.GdpCodaCaricamentoRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogEdizioneRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogRepository;
import it.csipiemonte.gdp.sftp.SftpClientProducer;
import it.csipiemonte.gdp.sftp.SftpSession;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class InviaEdizioneJob {

    private static final Logger LOG = Logger.getLogger(InviaEdizioneJob.class);

    @Inject
    GdpCodaCaricamentoRepository codaCaricamentoRepository;

    @Inject
    GdpLogEdizioneRepository logEdizioneRepository;

    @Inject
    GdpLogRepository logRepository;

    @Inject
    SftpClientProducer sftpClientProducer;

    @Inject
    @RestClient
    LibraClient libraClient;

    @ConfigProperty(name = "libra.api.token", defaultValue = "dummy-token-for-dev")
    String libraToken;

    //@Scheduled(every = "30m", identity = "F10-inviaEdizione") // commentato per test locali
    @Transactional
    public void execute() {
        LOG.info("Inizio job F10 - DAMtrasmissione.inviaEdizione");

        // F10 step 1 & 2: select READY ordered by priorita, data_inserim
        List<GdpCodaCaricamento> tasks = codaCaricamentoRepository.find(
                "stato = ?1 order by priorita asc, dataInserimento asc", 
                StatoCodaCaricamento.READY).list();

        if (tasks.isEmpty()) {
            LOG.debug("Nessun task in stato READY nella coda DAM");
            return;
        }

        try (SftpSession sftpSession = sftpClientProducer.connect()) {
            ChannelSftp channel = sftpSession.getChannel();

            for (GdpCodaCaricamento task : tasks) {
                processTask(task, channel);
            }

        } catch (Exception e) {
            LOG.error("Errore generico in F10 InviaEdizioneJob", e);
        }
    }

    private void processTask(GdpCodaCaricamento task, ChannelSftp channel) {
        LOG.infof("Elaborazione task caricamento DAM id: %d, path: %s", task.id, task.sftpPath);
        File tempFile = null;

        try {
            // Aggiornamento task: inizio processo
            task.nroTentativo++;
            task.dataTentativo = LocalDateTime.now();

            // SCARICAMENTO SFTP (Step 4)
            String localTempPath = System.getProperty("java.io.tmpdir") + File.separator + 
                    task.sftpPath.substring(task.sftpPath.lastIndexOf('/') + 1);
            tempFile = new File(localTempPath);
            
            try (InputStream is = channel.get(task.sftpPath);
                 FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            } catch (Exception e) {
                LOG.errorf("Errore download file da SFTP: %s", task.sftpPath);
                failTask(task, GdpMessage.F10_DAM_TRANSMISSION_FAILED, e.getMessage());
                return;
            }

            // CHIAMATA API DAM LIBRA (Step 4)
            String authHeader = "Bearer " + libraToken;
            LibraImportResponse response = null;
            try {
                response = libraClient.uploadZip(authHeader, tempFile);
            } catch (Exception e) {
                LOG.error("Dam API communication error", e);
                failTask(task, GdpMessage.F10_DAM_TRANSMISSION_FAILED, "API Exception: " + e.getMessage());
                return;
            }

            // GESTIONE RISPOSTA (Step 5)
            if (response != null && "SUBMITTED".equalsIgnoreCase(response.getStatus())) {
                successTask(task, response.getJobId(), channel);
            } else if (response != null && "FAILED".equalsIgnoreCase(response.getStatus())) {
                failTask(task, GdpMessage.F10_DAM_TRANSMISSION_FAILED, "API FAILED, jobId: " + response.getJobId());
            } else {
                failTask(task, GdpMessage.F10_DAM_TRANSMISSION_FAILED, "Unexpected API status");
            }

        } catch (Exception e) {
            LOG.errorf(e, "Eccezione inattesa elaborazione task %d", task.id);
            failTask(task, GdpMessage.F10_DAM_TRANSMISSION_FAILED, e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) LOG.warnf("Impossibile cancellare file locale temp: %s", tempFile.getAbsolutePath());
            }
        }
    }

    private void successTask(GdpCodaCaricamento task, String jobId, ChannelSftp channel) {
        LOG.infof("Invio DAM completato con successo. JobId: %s", jobId);

        task.stato = StatoCodaCaricamento.SUBMITTED;

        GdpLogEdizione logEdizione = logEdizioneRepository.findById(task.fkGdpLogEdizione);
        if (logEdizione != null) {
            logEdizione.jobId = jobId;
            logEdizione.descrizione = "SUBMITTED";
            logEdizione.fileZip = true;
            
            GdpLog log = logRepository.findById(logEdizione.fkGdpLog);
            if (log != null) {
                appendEsito(log, GdpMessage.F10_UPLOAD_EXECUTED.getCodice());
                logRepository.persist(log);
            }
            logEdizioneRepository.persist(logEdizione);
        }
        
        codaCaricamentoRepository.persist(task);

        // Rimozione da SFTP
        try {
            channel.rm(task.sftpPath);
            LOG.infof("File %s rimosso da SFTP", task.sftpPath);
        } catch (Exception e) {
            LOG.warnf("Impossibile rimuovere il file %s da SFTP", task.sftpPath);
        }
    }

    private void failTask(GdpCodaCaricamento task, GdpMessage msgEnum, String errorDesc) {
        LOG.errorf("Task fallito: %s", errorDesc);
        
        if (task.nroTentativo >= task.nroMaxTentativi) {
            task.stato = StatoCodaCaricamento.FAILED;
        }
        // Se non e' l'ultimo tentativo, rimane READY e riprova alla prossima iterazione
        
        GdpLogEdizione logEdizione = logEdizioneRepository.findById(task.fkGdpLogEdizione);
        if (logEdizione != null) {
            logEdizione.descrizione = "FAILED: " + errorDesc;
            
            GdpLog log = logRepository.findById(logEdizione.fkGdpLog);
            if (log != null) {
                appendEsito(log, msgEnum.getCodice());
                logRepository.persist(log);
            }
            logEdizioneRepository.persist(logEdizione);
        }
        
        codaCaricamentoRepository.persist(task);
    }
    
    private void appendEsito(GdpLog log, String codiceMsg) {
        if (log.esito == null || log.esito.isEmpty()) {
            log.esito = codiceMsg;
        } else if (!log.esito.contains(codiceMsg)) {
            log.esito += " - " + codiceMsg; // Concat MSG
        }
    }
}
