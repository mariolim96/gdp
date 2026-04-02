package it.csipiemonte.gdp.gdporch.service;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.TestProfile;
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
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Arrays;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@QuarkusTest
@TestProfile(MockBeansTestProfile.class)
public class CreaCartellaEdizioneAttesaJobTest {

    @Inject
    CreaCartellaEdizioneAttesaJob job;

    @InjectMock
    GdpDataUscitaRepository dataUscitaRepository;

    @InjectMock
    GdpPeriodicitaRepository periodicitaRepository;

    @InjectMock
    GdpTestataRepository testataRepository;

    @InjectMock
    GdpUtenteSftpRepository utenteSftpRepository;

    @InjectMock
    SftpClientProducer sftpClientProducer;

    SftpSession mockSession;
    ChannelSftp mockChannel;

    @BeforeEach
    void setUp() throws Exception {
        mockSession = mock(SftpSession.class);
        mockChannel = mock(ChannelSftp.class);
        when(sftpClientProducer.connect()).thenReturn(mockSession);
        when(mockSession.getChannel()).thenReturn(mockChannel);
        // Simulate that the first directory segment doesn't exist yet:
        // cd() throws SftpException → createRecursiveDir calls mkdir() + cd() again.
        // All subsequent cd() calls succeed (doNothing) so traversal completes normally.
        doThrow(new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "Directory does not exist"))
                .doNothing()
                .when(mockChannel).cd(anyString());
    }

    @Test
    void testExecute_HappyPath() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        GdpDataUscita data1 = new GdpDataUscita();
        data1.id = 100;
        data1.dataAttesa = tomorrow;
        data1.fkGdpPeriodicita = 1;

        when(dataUscitaRepository.findExpectedTomorrow()).thenReturn(Arrays.asList(data1));

        GdpPeriodicita periodicita = new GdpPeriodicita();
        periodicita.id = 1;
        periodicita.fkGdpTestata = 10;
        when(periodicitaRepository.findById(1)).thenReturn(periodicita);

        GdpTestata testata = new GdpTestata();
        testata.id = 10;
        testata.nomeTestata = "Gazzetta della Test";
        testata.cartellaTestata = "gazzetta_test";
        when(testataRepository.findById(10)).thenReturn(testata);

        GdpUtenteSftp utente = new GdpUtenteSftp();
        utente.homeSftp = "gazzetta";
        when(utenteSftpRepository.findByRifTestata("10")).thenReturn(utente);

        job.execute();

        // Verify that it tried to create directories
        verify(mockChannel, atLeastOnce()).mkdir(anyString());
    }

    @Test
    void testExecute_SftpErrorDoesNotCrash() throws Exception {
        GdpDataUscita data1 = new GdpDataUscita();
        data1.dataAttesa = LocalDate.now().plusDays(1);
        data1.fkGdpPeriodicita = 1;

        when(dataUscitaRepository.findExpectedTomorrow()).thenReturn(Arrays.asList(data1));

        GdpPeriodicita periodicita = new GdpPeriodicita();
        periodicita.id = 1;
        periodicita.fkGdpTestata = 10;
        when(periodicitaRepository.findById(1)).thenReturn(periodicita);

        GdpTestata testata = new GdpTestata();
        testata.id = 10;
        testata.cartellaTestata = "gazzetta_test";
        when(testataRepository.findById(10)).thenReturn(testata);

        GdpUtenteSftp utente = new GdpUtenteSftp();
        utente.homeSftp = "gazzetta";
        when(utenteSftpRepository.findByRifTestata("10")).thenReturn(utente);

        // Simulate SFTP failure
        doThrow(new SftpException(ChannelSftp.SSH_FX_FAILURE, "Mock Failure")).when(mockChannel).mkdir(anyString());

        // Should not throw exception
        job.execute();
    }
}
