package it.csipiemonte.gdp.gdporch.service;


import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class GdpDateUsciteTest {

    @Inject
    GdpDataUscitaService service;

    private GdpPeriodicita periodicita(String ggPeriodicita, Integer mensilita) {
        GdpPeriodicita p = new GdpPeriodicita();
        p.setGgPeriodicita(ggPeriodicita);
        p.setMensilita(mensilita);
        return p;
    }
    /**
     * Caso A: Giorno del mese G01 (primo giorno del mese)
     */
    @Test
    void testCasoA_GiornoDelMese() {
        GdpPeriodicita p = new GdpPeriodicita();
        p.setMensilita(1);
        p.setGgPeriodicita("G01");

        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        List<LocalDate> result = service.calcolaDateUscite(p, start, end);

        assertTrue(result.contains(LocalDate.of(2026, 1, 1)));
        assertTrue(result.contains(LocalDate.of(2026, 2, 1)));
        assertTrue(result.contains(LocalDate.of(2026, 3, 1)));
    }

    /**
     * Caso A: Giorno della settimana GnSm (primo sabato del mese)
     */
    @Test
    void testCasoA_GiornoSettimana() {
        GdpPeriodicita p = new GdpPeriodicita();
        p.setMensilita(1);
        p.setGgPeriodicita("G1S6"); // primo sabato

        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        List<LocalDate> result = service.calcolaDateUscite(p, start, end);

        assertFalse(result.isEmpty());
    }

    /**
     * Caso A: Sub-mensile GnS0 (giorni sub-mensili, esempio G1S0, G2S0, G3S0, G4S0)
     */
    @Test
    void testCasoA_SubMensile() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        for (int i = 1; i <= 4; i++) {
            GdpPeriodicita p = new GdpPeriodicita();
            p.setMensilita(1); // was 0.5, but mensilita is Integer. Using 1 for monthly-based quindicinale logic.
            p.setGgPeriodicita("G" + i + "S0"); // G1S0, G2S0, G3S0, G4S0

            List<LocalDate> result = service.calcolaDateUscite(p, start, end);

            assertFalse(result.isEmpty(), "Fallito per G" + i + "S0");
        }
    }

    /**
     * Caso B: Settimanale 1WS3 (ogni mercoledì)
     */
    @Test
    void testCasoB_Settimanale() {
        GdpPeriodicita p = new GdpPeriodicita();
        p.setMensilita(0);
        p.setGgPeriodicita("1WS3"); // ogni mercoledì

        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        List<LocalDate> result = service.calcolaDateUscite(p, start, end);

        assertFalse(result.isEmpty());
    }

    /**
     * Caso B: Quotidiano 1WS0
     */
    @Test
    void testCasoB_Quotidiano() {
        GdpPeriodicita p = new GdpPeriodicita();
        p.setMensilita(0);
        p.setGgPeriodicita("1WS0");

        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 5);

        List<LocalDate> result = service.calcolaDateUscite(p, start, end);

        assertEquals(5, result.size()); // 5 giorni consecutivi
    }

    /**
     * Caso B: Sub-settimanale 2WS0, 3WS0, 4WS0
     */
    @Test
    void testCasoB_2WS0_settimanaCompletaAlternata() {
        // 2WS0 → tutti i giorni di ogni seconda settimana (lun-dom)
        // Il conteggio parte dal primo lunedì dell'anno: lun 05/01/2026
        //
        // lun 05/01 - dom 11/01 → settimana 0 (0 % 2 == 0) ✓ inclusa
        // lun 12/01 - dom 18/01 → settimana 1 (1 % 2 != 0) ✗ esclusa
        // lun 19/01 - dom 25/01 → settimana 2 (2 % 2 == 0) ✓ inclusa
        // lun 26/01 - dom 01/02 → settimana 3 (3 % 2 != 0) ✗ esclusa
        //
        // giorni 01-04 gen: prima del primo lunedì → non inclusi
        // giorni 29-31 gen: settimana esclusa → non inclusi
        // totale: 7 + 7 = 14
        List<LocalDate> result = service.calcolaDateUscite(
                periodicita("2WS0", 0),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        assertEquals(14, result.size());
        assertEquals(LocalDate.of(2026, 1, 5), result.get(0));   // lunedì
        assertEquals(LocalDate.of(2026, 1, 11), result.get(6));  // domenica
        assertEquals(LocalDate.of(2026, 1, 19), result.get(7));  // lunedì
        assertEquals(LocalDate.of(2026, 1, 25), result.get(13)); // domenica
    }
    }
