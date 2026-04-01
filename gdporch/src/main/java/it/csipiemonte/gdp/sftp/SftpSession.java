package it.csipiemonte.gdp.sftp;


import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

// "AutoCloseable" significa: "Caro Java, quando finisco di usarmi, chiudimi da solo"
public class SftpSession implements AutoCloseable {
    private final Session session;      // La connessione SSH generale
    private final ChannelSftp channel;  // Il canale specifico per spostare file

    public SftpSession(Session session, ChannelSftp channel) {
        this.session = session;
        this.channel = channel;
    }

    // Ci serve per dare il canale al Service quando deve lavorare
    public ChannelSftp getChannel() {
        return channel;
    }

    // Questo metodo viene chiamato AUTOMATICAMENTE da Java alla fine del lavoro
    @Override
    public void close() {
        if (channel != null && channel.isConnected()) {
            channel.disconnect(); // Chiude il canale
        }
        if (session != null && session.isConnected()) {
            session.disconnect(); // Chiude la sessione generale
        }
    }
}