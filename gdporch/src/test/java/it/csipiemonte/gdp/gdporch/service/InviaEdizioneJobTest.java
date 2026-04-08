package it.csipiemonte.gdp.gdporch.service;

import com.jcraft.jsch.ChannelSftp;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import it.csipiemonte.gdp.gdporch.MockBeansTestProfile;
import it.csipiemonte.gdp.gdporch.client.LibraClient;
import it.csipiemonte.gdp.gdporch.dto.LibraImportResponse;
import it.csipiemonte.gdp.gdporch.model.entity.GdpCodaCaricamento;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLog;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLogEdizione;
import it.csipiemonte.gdp.gdporch.model.enums.StatoCodaCaricamento;
import it.csipiemonte.gdp.gdporch.model.repository.GdpCodaCaricamentoRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogEdizioneRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogRepository;
import it.csipiemonte.gdp.sftp.SftpClientProducer;
import it.csipiemonte.gdp.sftp.SftpSession;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestProfile(MockBeansTestProfile.class)
public class InviaEdizioneJobTest {

    @Inject
    InviaEdizioneJob job;

    @InjectMock
    GdpCodaCaricamentoRepository codaCaricamentoRepository;

    @InjectMock
    GdpLogEdizioneRepository logEdizioneRepository;

    @InjectMock
    GdpLogRepository logRepository;

    @InjectMock
    SftpClientProducer sftpClientProducer;

    @InjectMock
    @RestClient
    LibraClient libraClient;

    SftpSession mockSession;
    ChannelSftp mockChannel;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        mockSession = mock(SftpSession.class);
        mockChannel = mock(ChannelSftp.class);
        when(sftpClientProducer.connect()).thenReturn(mockSession);
        when(mockSession.getChannel()).thenReturn(mockChannel);
        
        InputStream mockInputStream = new ByteArrayInputStream("mock zip content".getBytes());
        when(mockChannel.get(anyString())).thenReturn(mockInputStream);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testExecute_NoReadyTasks() {
        PanacheQuery<GdpCodaCaricamento> mockQuery = mock(PanacheQuery.class);
        when(mockQuery.list()).thenReturn(Collections.emptyList());
        when(codaCaricamentoRepository.find(anyString(), any(Object[].class))).thenReturn(mockQuery);

        job.execute();

        verify(codaCaricamentoRepository, times(1)).find(anyString(), any(Object[].class));
        verifyNoInteractions(sftpClientProducer);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testExecute_SubmittedResponse() throws Exception {
        GdpCodaCaricamento task = new GdpCodaCaricamento();
        task.id = 1;
        task.sftpPath = "/_dam/test.zip";
        task.stato = StatoCodaCaricamento.READY;
        task.nroTentativo = 0;
        task.nroMaxTentativi = 3;
        task.fkGdpLogEdizione = 100;
        task.dataInserimento = LocalDate.now();
        task.priorita = 0;

        PanacheQuery<GdpCodaCaricamento> mockQuery = mock(PanacheQuery.class);
        when(mockQuery.list()).thenReturn(Collections.singletonList(task));
        when(codaCaricamentoRepository.find(anyString(), any(Object[].class))).thenReturn(mockQuery);

        LibraImportResponse response = new LibraImportResponse();
        response.setJobId("JOB-123");
        response.setStatus("SUBMITTED");
        when(libraClient.uploadZip(anyString(), any(File.class))).thenReturn(response);

        GdpLogEdizione logEd = new GdpLogEdizione();
        logEd.id = 100;
        logEd.fkGdpLog = 50;
        when(logEdizioneRepository.findById(100)).thenReturn(logEd);

        GdpLog log = new GdpLog();
        log.id = 50;
        log.esito = "";
        when(logRepository.findById(50)).thenReturn(log);

        job.execute();

        verify(mockChannel, times(1)).get("/_dam/test.zip");
        verify(libraClient, times(1)).uploadZip(anyString(), any(File.class));
        verify(mockChannel, times(1)).rm("/_dam/test.zip");
        verify(codaCaricamentoRepository, times(1)).persist(task);

        assert task.stato == StatoCodaCaricamento.SUBMITTED;
        assert logEd.jobId.equals("JOB-123");
        assert log.esito.contains("MSG00009");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testExecute_FailedResponse() throws Exception {
        GdpCodaCaricamento task = new GdpCodaCaricamento();
        task.id = 1;
        task.sftpPath = "/_dam/test_failed.zip";
        task.stato = StatoCodaCaricamento.READY;
        task.nroTentativo = 2; // Next will be 3 (max)
        task.nroMaxTentativi = 3;
        task.fkGdpLogEdizione = 101;
        task.dataInserimento = LocalDate.now();
        task.priorita = 0;

        PanacheQuery<GdpCodaCaricamento> mockQuery = mock(PanacheQuery.class);
        when(mockQuery.list()).thenReturn(Collections.singletonList(task));
        when(codaCaricamentoRepository.find(anyString(), any(Object[].class))).thenReturn(mockQuery);

        LibraImportResponse response = new LibraImportResponse();
        response.setJobId("JOB-456");
        response.setStatus("FAILED");
        when(libraClient.uploadZip(anyString(), any(File.class))).thenReturn(response);

        GdpLogEdizione logEd = new GdpLogEdizione();
        logEd.id = 101;
        logEd.fkGdpLog = 51;
        when(logEdizioneRepository.findById(101)).thenReturn(logEd);

        GdpLog log = new GdpLog();
        log.id = 51;
        log.esito = "";
        when(logRepository.findById(51)).thenReturn(log);

        job.execute();

        verify(mockChannel, times(1)).get("/_dam/test_failed.zip");
        verify(libraClient, times(1)).uploadZip(anyString(), any(File.class));
        verify(mockChannel, never()).rm(anyString()); // Must not delete SFTP file on failure
        verify(codaCaricamentoRepository, times(1)).persist(task);

        assert task.stato == StatoCodaCaricamento.FAILED; // Due to max attempts
        assert log.esito.contains("MSG00001");
    }
}
