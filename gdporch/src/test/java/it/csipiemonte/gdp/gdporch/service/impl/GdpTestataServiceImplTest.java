package it.csipiemonte.gdp.gdporch.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.csipiemonte.gdp.gdporch.dto.TestataDetail;
import it.csipiemonte.gdp.gdporch.dto.TestataSummaryList;
import it.csipiemonte.gdp.gdporch.exception.GdpException;
import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GdpTestataServiceImplTest {

    @Mock
    private GdpTestataRepository gdpTestataRepository;

    private GdpTestataServiceImpl service;

    @BeforeEach
    void setUp() {
        // Inizializza i mock e inietta il repository nel service da testare.
        MockitoAnnotations.openMocks(this);
        service = new GdpTestataServiceImpl();
        service.gdpTestataRepository = gdpTestataRepository;
    }

    @Test
    void elencoTestateSenzaFiltriRestituisceTutteLeTestate() {
        // Obiettivo (TB-F16-I01):
        // - verificare che senza filtri il service interroghi listAll()
        // - verificare che il mapping verso TestataSummaryList contenga i campi
        // principali.
        // Dipendenze usate:
        // - gdpTestataRepository.listAll() mockato con 3 testate.
        GdpTestata t1 = testata(1, "Testata TO", "testata-to", true, "TO");
        GdpTestata t2 = testata(2, "Testata CN", "testata-cn", true, "CN");
        GdpTestata t3 = testata(99, "Testata AL", "testata-al", false, "AL");

        when(gdpTestataRepository.listAll()).thenReturn(List.of(t1, t2, t3));

        TestataSummaryList result = service.elencoTestate(null, null, null);

        assertNotNull(result);
        assertEquals(3, result.getTestate().size());
        assertEquals(1, result.getTestate().get(0).getIdTestata());
        assertEquals("Testata TO", result.getTestate().get(0).getNomeTestata());
        assertEquals("testata-to", result.getTestate().get(0).getCartellaTestata());
        assertEquals(true, result.getTestate().get(0).getInvioEdizione());
        assertEquals("TO", result.getTestate().get(0).getProvincia());

        verify(gdpTestataRepository).listAll();
    }

    @Test
    void elencoTestateFiltroInvioEdizioneRestituisceSoloInvianti() {
        // Obiettivo (TB-F16-I02):
        // - verificare l'uso del filtro invioEdizione=true.
        // Dipendenze usate:
        // - gdpTestataRepository.findByInvioEdizione(true) mockato con 2 testate.
        GdpTestata t1 = testata(1, "Testata TO", "testata-to", true, "TO");
        GdpTestata t2 = testata(2, "Testata CN", "testata-cn", true, "CN");

        when(gdpTestataRepository.findByInvioEdizione(true)).thenReturn(List.of(t1, t2));

        TestataSummaryList result = service.elencoTestate(true, null, null);

        assertNotNull(result);
        assertEquals(2, result.getTestate().size());
        assertEquals(true, result.getTestate().get(0).getInvioEdizione());
        assertEquals(true, result.getTestate().get(1).getInvioEdizione());

        verify(gdpTestataRepository).findByInvioEdizione(true);
    }

    @Test
    void elencoTestateFiltroProvinciaRestituisceSoloProvinciaRichiesta() {
        // Obiettivo (TB-F16-I03):
        // - verificare il filtro provincia (es. TO) e il mapping in output.
        // Dipendenze usate:
        // - gdpTestataRepository.findByProvincia("TO") mockato con testate della
        // provincia richiesta.
        GdpTestata t1 = testata(10, "Giornale Torino 1", "to-1", true, "TO");
        GdpTestata t2 = testata(11, "Giornale Torino 2", "to-2", false, "TO");

        when(gdpTestataRepository.findByProvincia("TO")).thenReturn(List.of(t1, t2));

        TestataSummaryList result = service.elencoTestate(null, "TO", null);

        assertNotNull(result);
        assertEquals(2, result.getTestate().size());
        assertEquals("TO", result.getTestate().get(0).getProvincia());
        assertEquals("TO", result.getTestate().get(1).getProvincia());

        verify(gdpTestataRepository).findByProvincia("TO");
    }

    @Test
    void elencoTestateConPiuFiltriLanciaEccezione() {
        // Obiettivo (vincolo implementativo F16):
        // - verificare che sia accettato un solo filtro alla volta.
        // Dipendenze usate:
        // - nessuna query DB, validazione eseguita prima dell'accesso al repository.
        GdpException exception = assertThrows(GdpException.class,
                () -> service.elencoTestate(true, "TO", null));

        assertEquals("MSG00001", exception.getCodice());
        assertEquals("Solo un filtro alla volta è consentito", exception.getMessage());
    }

    @Test
    void elencoTestateFiltroIdTestataRestituisceElementoSingolo() {
        // Obiettivo (comportamento implementato):
        // - verificare il filtro per idTestata e il mapping dell'unico record trovato.
        // Dipendenze usate:
        // - gdpTestataRepository.findById(idTestata) mockato con testata esistente.
        GdpTestata testata = testata(7, "Testata Singola", "singola", true, "AT");
        when(gdpTestataRepository.findById(7)).thenReturn(testata);

        TestataSummaryList result = service.elencoTestate(null, null, 7);

        assertNotNull(result);
        assertEquals(1, result.getTestate().size());
        assertEquals(7, result.getTestate().get(0).getIdTestata());

        verify(gdpTestataRepository).findById(7);
    }

    @Test
    void getTestataByIdRestituisceDettaglioCompleto() {
        // Obiettivo (TB-F17-I01):
        // - verificare che il dettaglio testata venga mappato su tutti i campi
        // principali
        // del DTO generato TestataDetail.
        // Dipendenze usate:
        // - gdpTestataRepository.findById(idTestata) mockato con record completo.
        GdpTestata testata = new GdpTestata();
        testata.id = 1;
        testata.nomeTestata = "La Sentinella del Canavese";
        testata.cartellaTestata = "sentinella";
        testata.invioEdizione = true;
        testata.stato = 0;
        testata.dataStato = LocalDate.of(2026, 3, 1);
        testata.cancellazione = null;
        testata.codTema = 12;
        testata.tema = "Cultura";
        testata.socEditrice = "Editrice Test";
        testata.enteProponente = "Ente Test";
        testata.annoFondazione = 1980;
        testata.periodoFreq = "Settimanale";
        testata.periodoGg = "Lun";
        testata.descrizione = "Descrizione di test";
        testata.www = "www.sentinella.test";
        testata.mail = "redazione@sentinella.test";
        testata.provincia = "TO";
        testata.comune = "Torino";
        testata.indirizzo = "Via Roma 1";
        testata.cap = "10100";
        testata.longitudine = new BigDecimal("7.6869");
        testata.latitudine = new BigDecimal("45.0703");

        when(gdpTestataRepository.findById(1)).thenReturn(testata);

        TestataDetail result = service.getTestataById(1);

        assertNotNull(result);
        assertEquals(1, result.getIdTestata());
        assertEquals("La Sentinella del Canavese", result.getNomeTestata());
        assertEquals("sentinella", result.getCartellaTestata());
        assertEquals(true, result.getInvioEdizione());
        assertEquals("TO", result.getProvincia());
        assertEquals("Cultura", result.getTema());
        assertEquals("redazione@sentinella.test", result.getMail());
        assertEquals("http://www.sentinella.test", result.getSitoWeb().toString());
        assertEquals(45.0703, result.getLatitudine());
        assertEquals(7.6869, result.getLongitudine());

        verify(gdpTestataRepository).findById(1);
    }

    @Test
    void getTestataByIdIdInesistenteLanciaEccezione() {
        // Obiettivo (TB-F17-I02, coerente con implementazione corrente):
        // - verificare che id testata inesistente produca eccezione business.
        // Dipendenze usate:
        // - gdpTestataRepository.findById(idTestata) mockato con null.
        when(gdpTestataRepository.findById(999)).thenReturn(null);

        GdpException exception = assertThrows(GdpException.class, () -> service.getTestataById(999));

        // F17_NOT_FOUND nella codebase corrente usa MSG00002.
        assertEquals("MSG00002", exception.getCodice());
        assertEquals("Testata non trovata", exception.getMessage());
    }

    private static GdpTestata testata(Integer id, String nome, String cartella, Boolean invioEdizione,
            String provincia) {
        // Helper: crea una testata minima necessaria al mapping F16.
        GdpTestata t = new GdpTestata();
        t.id = id;
        t.nomeTestata = nome;
        t.cartellaTestata = cartella;
        t.invioEdizione = invioEdizione;
        t.provincia = provincia;
        return t;
    }
}
