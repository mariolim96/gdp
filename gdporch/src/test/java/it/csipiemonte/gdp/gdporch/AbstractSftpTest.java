package it.csipiemonte.gdp.gdporch;

import com.jcraft.jsch.ChannelSftp;
import io.quarkus.test.InjectMock;
import it.csipiemonte.gdp.sftp.SftpClientProducer;
import it.csipiemonte.gdp.sftp.SftpSession;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractSftpTest {

    @InjectMock
    protected SftpClientProducer producer;

    protected SftpSession mockSession = mock(SftpSession.class);
    protected ChannelSftp mockChannel = mock(ChannelSftp.class);

    @BeforeEach
    void setupSftp() throws Exception {
        when(producer.connect()).thenReturn(mockSession);
        when(mockSession.getChannel()).thenReturn(mockChannel);
    }
}
