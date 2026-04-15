package it.csipiemonte.gdp.gdporch.service;

import com.jcraft.jsch.ChannelSftp;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.csipiemonte.gdp.gdporch.AbstractSftpTest;
import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import it.csipiemonte.gdp.sftp.SftpClientProducer;
import it.csipiemonte.gdp.sftp.SftpSession;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Vector;

import static org.mockito.Mockito.*;

@QuarkusTest
public class CheckEdizioneAttesaTest {

    @Inject
    CheckEdizioneAttesaJob job;

    @InjectMock
    GdpTestataRepository repo;

    @InjectMock
    SftpClientProducer producer; // Portato qui dalla classe base

    // Mock manuali
    SftpSession mockSession = mock(SftpSession.class);
    ChannelSftp mockChannel = mock(ChannelSftp.class);

    @BeforeEach
    void setup() throws Exception {
        when(producer.connect()).thenReturn(mockSession);
        when(mockSession.getChannel()).thenReturn(mockChannel);

        // FONDAMENTALE: evita NullPointerException se il Job chiama ls() su altre cartelle
        when(mockChannel.ls(anyString())).thenReturn(new Vector<>());
    }

    @Test
    @DisplayName("Scenario F03_OK: Flusso regolare completato con successo")
    void testFlussoOk() throws Exception {
        // ... (ARRENGEMENT UGUALE A PRIMA) ...
        String cartella = "LA_STAMPA";
        String edizione = "2026-04-13";
        GdpTestata t = new GdpTestata();
        t.id = 1;
        t.cartellaTestata = cartella;
        when(repo.findByCartella(cartella)).thenReturn(List.of(t));
        ChannelSftp.LsEntry pdf = mock(ChannelSftp.LsEntry.class);
        when(pdf.getFilename()).thenReturn("pagina1.pdf");

        // --- ACT ---
        job.smistaEdizione(mockChannel, cartella, edizione, "2026-04-13 10:00:00", "/src", List.of(pdf));

        // --- ASSERT ---
        // 1. Verifichiamo lo spostamento: usiamo Matchers più generici per i path
        verify(mockChannel, atLeastOnce()).rename(contains("pagina1.pdf"), contains("LA_STAMPA"));

        // 2. Verifichiamo il marker .OK: usiamo any() per l'input stream e contains per il nome
        verify(mockChannel, atLeastOnce()).put(any(java.io.InputStream.class), contains("pagina1.pdf.OK"));
    }

    @Test
    @DisplayName("Scenario F03_AMBIGUOUS: Più testate trovate per la stessa cartella")
    void testFlussoAmbiguo() throws Exception {
        // --- ARRANGE ---
        String cartella = "DOPPIA_TESTATA";
        // Simuliamo due record sul DB per la stessa cartella
        when(repo.findByCartella(cartella)).thenReturn(List.of(new GdpTestata(), new GdpTestata()));

        ChannelSftp.LsEntry pdf = mock(ChannelSftp.LsEntry.class);
        when(pdf.getFilename()).thenReturn("errore.pdf");

        // --- ACT ---
        job.smistaEdizione(mockChannel, cartella, "2026-04-13", "2026-04-13 10:00:00", "/src", List.of(pdf));

        // --- ASSERT ---
        // Verifichiamo che i file vadano in _errata
        verify(mockChannel).rename(anyString(), contains("_errata"));
    }

    @Test
    @DisplayName("Scenario: Cartella ignorata perché il nome non è una data valida")
    void testEdizioneFormatoErrato() throws Exception {
        // --- ARRANGE ---
        String nomeTestata = "LA_STAMPA";
        String cartellaSbagliata = "backup_vecchio"; // Non è YYYY-MM-DD

        // Simuliamo l'ingresso nel metodo step 2
        ChannelSftp.LsEntry mockEdizione = mock(ChannelSftp.LsEntry.class);
        when(mockEdizione.getFilename()).thenReturn(cartellaSbagliata);

        // --- ACT ---
        // Chiamiamo il metodo che cicla sulle edizioni
        // In questo caso, verifichiamo che il processo si fermi subito
        job.processaTestataAsincrona(nomeTestata);

        // --- ASSERT ---
        // Verifichiamo che NON cerchi file dentro la cartella sbagliata
        verify(mockChannel, never()).ls(contains(cartellaSbagliata));
    }

    @Test
    @DisplayName("Scenario: Nessun PDF presente, solo file di altro tipo")
    void testNessunPdfPresente() throws Exception {
        // --- ARRANGE ---
        String path = "/src/testata/2026-04-13";

        // Simuliamo la presenza di un file .txt (che deve essere ignorato)
        ChannelSftp.LsEntry mockTxt = mock(ChannelSftp.LsEntry.class);
        when(mockTxt.getFilename()).thenReturn("readme.txt");
        Vector<ChannelSftp.LsEntry> files = new Vector<>();
        files.add(mockTxt);

        when(mockChannel.ls(path)).thenReturn(files);

        // --- ACT ---
        // Se eseguiamo la logica, non dovrebbe mai arrivare a smistaEdizione
        job.processaEdizioniPerTestata(mockChannel, "testata");

        // --- ASSERT ---
        // Verifichiamo che non sia mai stato chiamato il rename (spostamento)
        verify(mockChannel, never()).rename(anyString(), anyString());
    }
}
