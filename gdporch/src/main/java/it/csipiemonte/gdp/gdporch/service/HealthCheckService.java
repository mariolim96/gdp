package it.csipiemonte.gdp.gdporch.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import it.csipiemonte.gdp.sftp.SftpClientProducer;
import it.csipiemonte.gdp.sftp.SftpSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class HealthCheckService {

    private static final Logger LOG = Logger.getLogger(HealthCheckService.class);

    @Inject
    EntityManager em;

    @Inject
    SftpClientProducer sftpClientProducer;

    @ConfigProperty(name = "sftp.root.prefix", defaultValue = "")
    String sftpRootPrefix;

    @ConfigProperty(name = "sftp.root.prefix.tmp", defaultValue = "_tmp/")
    String tmpPrefix;

    @ConfigProperty(name = "sftp.root.prefix.dam", defaultValue = "_dam/")
    String damPrefix;

    @ConfigProperty(name = "sftp.root.prefix.errata", defaultValue = "_errata/")
    String errataPrefix;

    public List<String> checkDatabase() {
        List<String> results = new ArrayList<>();
        LOG.info("Checking Database health...");
        try {
            Object result = em.createNativeQuery("SELECT 1").getSingleResult();
            results.add("Connectivity: OK (" + result + ")");

            checkTable(results, "GDP_TIPO_EDIZIONE");
            checkTable(results, "GDP_MAIL");
            checkTable(results, "GDP_TIPO_FILE");

        } catch (Exception e) {
            results.add("Connectivity: FAILED - " + e.getMessage());
        }
        return results;
    }

    private void checkTable(List<String> results, String tableName) {
        try {
            Long count = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM " + tableName).getSingleResult()).longValue();
            results.add("Table " + tableName + ": OK (" + count + " records)");
        } catch (Exception e) {
            results.add("Table " + tableName + ": FAILED - " + e.getMessage());
        }
    }

    public List<String> checkSftp() {
        List<String> results = new ArrayList<>();
        LOG.info("Checking SFTP health...");
        try (SftpSession session = sftpClientProducer.connect()) {
            results.add("Connectivity: OK");
            ChannelSftp channel = session.getChannel();

            testWrite(channel, sftpRootPrefix, results);
            testWrite(channel, tmpPrefix, results);
            testWrite(channel, damPrefix, results);
            testWrite(channel, errataPrefix, results);

        } catch (Exception e) {
            results.add("Connectivity: FAILED - " + e.getMessage());
        }
        return results;
    }

    private void testWrite(ChannelSftp channel, String path, List<String> results) {
        if (path == null || path.isEmpty()) return;
        
        String testFile = path + (path.endsWith("/") ? "" : "/") + ".healthcheck_tmp";
        try {
            LOG.infof("Testing write permission on: %s", testFile);
            byte[] content = "healthcheck".getBytes(StandardCharsets.UTF_8);
            channel.put(new ByteArrayInputStream(content), testFile);
            channel.rm(testFile);
            results.add("Path " + path + ": WRITE OK");
        } catch (SftpException e) {
            results.add("Path " + path + ": WRITE FAILED - " + e.getMessage());
        }
    }
}
