package it.csipiemonte.gdp.gdporch;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Wrapper for atmoz/sftp Testcontainer.
 */
public class SftpTestServer {

    private GenericContainer<?> container;
    private boolean localMode = false;
    private static final String LOCAL_SFTP_ROOT = "../sftp-data"; // Assumes running from gdporch/

    public SftpTestServer() {
        this(false);
    }

    public SftpTestServer(boolean localMode) {
        this.localMode = localMode;
        if (!localMode) {
            this.container = new GenericContainer<>(DockerImageName.parse("atmoz/sftp:alpine"))
                    .withExposedPorts(22)
                    .withEnv("SFTP_USERS", "tester:password:1001:1001:flusso_regolare,flusso_saltuario,_tmp,_dam,_errata");
        }
    }

    public void start() {
        if (!localMode && container != null) {
            container.start();
        }
    }

    public void stop() {
        if (!localMode && container != null) {
            container.stop();
        }
    }

    public int getPort() {
        return localMode ? 2222 : container.getMappedPort(22);
    }

    public String getHost() {
        return localMode ? "localhost" : container.getHost();
    }

    /**
     * Deposits a fake PDF in the SFTP container.
     */
    public void depositaPdf(String cartella, String data, String nomefile) throws IOException {
        byte[] content = TestPdfFactory.singlePage(nomefile);
        
        if (localMode) {
            Path targetDir = Path.of(LOCAL_SFTP_ROOT, "flusso_regolare", cartella, data);
            Files.createDirectories(targetDir);
            Files.write(targetDir.resolve(nomefile), content);
            return;
        }

        String path = String.format("/home/tester/flusso_regolare/%s/%s/%s", cartella, data, nomefile);
        String dirPath = String.format("/home/tester/flusso_regolare/%s/%s", cartella, data);
        try {
            container.execInContainer("mkdir", "-p", dirPath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Failed to create directory in SFTP container", e);
        }

        container.copyFileToContainer(Transferable.of(content), path);
    }
    
    public String getHomePath() {
        return "/home/tester";
    }

    public GenericContainer<?> getContainer() {
        return container;
    }

    public boolean isLocalMode() {
        return localMode;
    }
}
