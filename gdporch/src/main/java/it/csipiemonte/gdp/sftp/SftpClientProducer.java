package it.csipiemonte.gdp.sftp;

import com.jcraft.jsch.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import java.util.Properties;

@ApplicationScoped
public class SftpClientProducer {

    private static final Logger LOG = Logger.getLogger(SftpClientProducer.class);

    // Leggiamo i dati dal file application.properties
    @ConfigProperty(name = "sftp.host") String host;
    @ConfigProperty(name = "sftp.port") int port;
    @ConfigProperty(name = "sftp.username") String user;
    @ConfigProperty(name = "sftp.password") String password;

    public SftpSession connect() throws JSchException {
        JSch jsch = new JSch(); // La libreria che "parla" SFTP
        LOG.infof("Connessione a %s:%d...", host, port);

        // Prepariamo la sessione (il tunnel)
        Session session = jsch.getSession(user, host, port);
        session.setPassword(password);

        Properties config = new Properties();
        // Diciamo a Java di non spaventarsi se non conosce già il server
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.connect(10000); // Prova a connetterti, aspetta max 10 secondi

        // Apriamo il canale specifico per i file (SFTP)
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();

        // Restituiamo il nostro "pacchetto" sessione+canale
        return new SftpSession(session, channel);
    }
}