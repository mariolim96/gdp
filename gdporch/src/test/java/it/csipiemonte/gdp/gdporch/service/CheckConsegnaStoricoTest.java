package it.csipiemonte.gdp.gdporch.service;

import com.jcraft.jsch.ChannelSftp;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
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
import static org.mockito.ArgumentMatchers.*;

@QuarkusTest
public class CheckConsegnaStoricoTest {

    @Inject
    CheckConsegnaStoricoJob job;

    @InjectMock
    GdpTestataRepository repo;

    @InjectMock
    SftpClientProducer producer;

    @InjectMock
    GdpCtrlEdizioniStoricheService ctrlEdizioniStoricheService;

    SftpSession mockSession = mock(SftpSession.class);
    ChannelSftp mockChannel = mock(ChannelSftp.class);

    @BeforeEach
    void setup() throws Exception {
        // 1. Allineiamo la variabile del Job a quello che lui cerca realmente
        job.saltuarioDir = "flusso_saltuario/";
        job.tmpDir = "_tmp";
        job.errataDir = "_errata";

        when(producer.connect()).thenReturn(mockSession);
        when(mockSession.getChannel()).thenReturn(mockChannel);

        // Default safe: se non specifichiamo altro, restituisci lista vuota
        when(mockChannel.ls(anyString())).thenReturn(new Vector<>());
    }

    @Test
    void testFlussoOk() throws Exception {
        String utente = "utente1";
        String consegna = "CONS_2026-04-14";
        String cartellaTestata = "LA_STAMPA";

        // 1. Mock scansione Root: DEVE corrispondere a "flusso_saltuario/"
        Vector<ChannelSftp.LsEntry> vUtenti = new Vector<>();
        vUtenti.add(creaMockEntry(utente, true));
        // Usiamo contains per non sbagliare con gli slash
        when(mockChannel.ls(contains("flusso_saltuario"))).thenReturn(vUtenti);

        // 2. Mock scansione Utente -> Consegna
        Vector<ChannelSftp.LsEntry> vConsegne = new Vector<>();
        vConsegne.add(creaMockEntry(consegna, true));
        when(mockChannel.ls(contains(utente))).thenReturn(vConsegne);

        // 3. Mock scansione Consegna -> Testata
        Vector<ChannelSftp.LsEntry> vTestate = new Vector<>();
        vTestate.add(creaMockEntry(cartellaTestata, true));
        when(mockChannel.ls(contains(consegna))).thenReturn(vTestate);

        // 4. Mock scansione Testata (per il conteggio file interno)
        Vector<ChannelSftp.LsEntry> vFile = new Vector<>();
        vFile.add(creaMockEntry("documento.pdf", false));
        when(mockChannel.ls(contains(cartellaTestata))).thenReturn(vFile);

        // Mock DB
        GdpTestata t = new GdpTestata();
        t.id = 12;
        when(repo.findByCartella(eq(cartellaTestata))).thenReturn(List.of(t));

        // --- ACT ---
        job.eseguiCheckConsegnaStorico();

        // --- ASSERT ---
        // Ora verifichiamo che sia arrivato in fondo
        verify(mockChannel, atLeastOnce()).rename(anyString(), anyString());
    }

    @Test
    @DisplayName("Scenario: Testata non censita -> Spostamento in ERRATA")
    void testTestataNonTrovata() throws Exception {
        String utente = "utente1";
        String consegna = "CONS_2026-04-14";
        String cartellaIgnota = "TESTATA_SCONOSCIUTA";

        // Mock SFTP per far arrivare il job fino alla testata
        configuraPercorsoSftp(utente, consegna, cartellaIgnota);

        // Mock DB: restituisce lista VUOTA
        when(repo.findByCartella(cartellaIgnota)).thenReturn(List.of());

        job.eseguiCheckConsegnaStorico();

        // VERIFICA: deve aver spostato in _errata (errataDir)
        verify(mockChannel).rename(contains(cartellaIgnota), contains("_errata"));
        // VERIFICA: NON deve aver chiamato il servizio di successo
        verifyNoInteractions(ctrlEdizioniStoricheService);
    }

    @Test
    @DisplayName("Scenario: Testata ambigua (duplicata) -> Spostamento in ERRATA")
    void testTestataAmbigua() throws Exception {
        String cartella = "TESTATA_DOPPIA";
        configuraPercorsoSftp("utente1", "CONS_2026-04-14", cartella);

        // Mock DB: restituisce DUE testate
        when(repo.findByCartella(cartella)).thenReturn(List.of(new GdpTestata(), new GdpTestata()));

        job.eseguiCheckConsegnaStorico();

        // VERIFICA: Deve finire in errata per ambiguità
        verify(mockChannel).rename(contains(cartella), contains("_errata"));
    }

    @Test
    @DisplayName("Scenario: Cartella non conforme al pattern CONS_ -> Ignorata")
    void testCartellaPatternErrato() throws Exception {
        String utente = "utente1";
        String cartellaSbagliata = "NON_UNA_CONSEGNA";

        // Mock Root
        Vector<ChannelSftp.LsEntry> vUtenti = new Vector<>();
        vUtenti.add(creaMockEntry(utente, true));
        when(mockChannel.ls(contains("flusso_saltuario"))).thenReturn(vUtenti);

        // Mock Utente con cartella che non rispetta il pattern CONS_yyyy-mm-dd
        Vector<ChannelSftp.LsEntry> vError = new Vector<>();
        vError.add(creaMockEntry(cartellaSbagliata, true));
        when(mockChannel.ls(contains(utente))).thenReturn(vError);

        job.eseguiCheckConsegnaStorico();

        // VERIFICA: Il rename non deve mai essere chiamato perché il filtro regex scarta la cartella
        verify(mockChannel, never()).rename(anyString(), anyString());
    }

    private ChannelSftp.LsEntry creaMockEntry(String nome, boolean isDir) {
        ChannelSftp.LsEntry entry = mock(ChannelSftp.LsEntry.class);
        com.jcraft.jsch.SftpATTRS attrs = mock(com.jcraft.jsch.SftpATTRS.class);
        when(entry.getFilename()).thenReturn(nome);
        when(entry.getAttrs()).thenReturn(attrs);
        when(attrs.isDir()).thenReturn(isDir);
        when(attrs.getMTime()).thenReturn((int) (System.currentTimeMillis() / 1000));
        return entry;
    }

    private void configuraPercorsoSftp(String utente, String consegna, String testata) throws Exception {
        Vector<ChannelSftp.LsEntry> vUtenti = new Vector<>();
        vUtenti.add(creaMockEntry(utente, true));
        when(mockChannel.ls(contains("flusso_saltuario"))).thenReturn(vUtenti);

        Vector<ChannelSftp.LsEntry> vConsegne = new Vector<>();
        vConsegne.add(creaMockEntry(consegna, true));
        when(mockChannel.ls(contains(utente))).thenReturn(vConsegne);

        Vector<ChannelSftp.LsEntry> vTestate = new Vector<>();
        vTestate.add(creaMockEntry(testata, true));
        when(mockChannel.ls(contains(consegna))).thenReturn(vTestate);

        // Per il contaFile
        when(mockChannel.ls(contains(testata))).thenReturn(vTestate);
    }
}