package it.csipiemonte.gdp.gdporch.service;

import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import it.csipiemonte.gdp.gdporch.service.impl.GdpCtrlEdizioneAcquisitaServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione;
import java.time.LocalDate;
import java.lang.reflect.Method;

class GdpCtrlEdizioneAcquisitaServiceTest {

    @Test
    void testClassifyEditionType() throws Exception {
        GdpCtrlEdizioneAcquisitaServiceImpl service = new GdpCtrlEdizioneAcquisitaServiceImpl(null, null, null, null, null, null, null, null);

        Method method = GdpCtrlEdizioneAcquisitaServiceImpl.class.getDeclaredMethod("classifyEditionType",
                LocalDate.class, GdpDataUscita.class);
        method.setAccessible(true);

        LocalDate today = LocalDate.now();
        GdpDataUscita dataUscita = new GdpDataUscita();
        dataUscita.sospesa = false;

        // OK
        TipoEdizione result = (TipoEdizione) method.invoke(service, today, dataUscita);
        Assertions.assertEquals(TipoEdizione.OK, result);

        // AN (anticipataria)
        result = (TipoEdizione) method.invoke(service, today.plusDays(1), dataUscita);
        Assertions.assertEquals(TipoEdizione.AN, result);

        // PO (posticipataria)
        result = (TipoEdizione) method.invoke(service, today.minusDays(1), dataUscita);
        Assertions.assertEquals(TipoEdizione.PO, result);

        // SO (sospesa)
        dataUscita.sospesa = true;
        result = (TipoEdizione) method.invoke(service, today, dataUscita);
        Assertions.assertEquals(TipoEdizione.SO, result);

        // AA (non presente)
        result = (TipoEdizione) method.invoke(service, today, null);
        Assertions.assertEquals(TipoEdizione.AA, result);
    }

    @Test
    void testCheckHeuristicDate() throws Exception {
        GdpCtrlEdizioneAcquisitaServiceImpl service = new GdpCtrlEdizioneAcquisitaServiceImpl(null, null, null, null, null, null, null, null);
        Method method = GdpCtrlEdizioneAcquisitaServiceImpl.class.getDeclaredMethod("checkHeuristicDate",
                String.class, LocalDate.class, String.class, java.util.List.class);
        method.setAccessible(true);

        LocalDate expected = LocalDate.of(2026, 3, 10);
        java.util.List<String> errors = new java.util.ArrayList<>();

        // Test matching date 10/03/2026
        method.invoke(service, "Testata del 10-03-2026", expected, "file.pdf", errors);
        Assertions.assertTrue(errors.isEmpty());

        // Test matching date 10 marzo 2026
        method.invoke(service, "Edizione di martedì 10 marzo 2026", expected, "file.pdf", errors);
        Assertions.assertTrue(errors.isEmpty());

        // Test non-matching date
        method.invoke(service, "Edizione del 11/03/2026", expected, "file.pdf", errors);
        Assertions.assertFalse(errors.isEmpty());
        Assertions.assertEquals("DA - file.pdf", errors.get(0));
    }

    @Test
    void testParseMonth() throws Exception {
        GdpCtrlEdizioneAcquisitaServiceImpl service = new GdpCtrlEdizioneAcquisitaServiceImpl(null, null, null, null, null, null, null, null);
        Method method = GdpCtrlEdizioneAcquisitaServiceImpl.class.getDeclaredMethod("parseMonth", String.class);
        method.setAccessible(true);

        Assertions.assertEquals(1, method.invoke(service, "gennaio"));
        Assertions.assertEquals(1, method.invoke(service, "gen"));
        Assertions.assertEquals(5, method.invoke(service, "maggio"));
        Assertions.assertEquals(12, method.invoke(service, "dicembre"));
        Assertions.assertEquals(8, method.invoke(service, "08"));
    }
}
