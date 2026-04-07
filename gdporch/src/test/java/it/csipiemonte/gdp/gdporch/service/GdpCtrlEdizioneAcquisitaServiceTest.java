package it.csipiemonte.gdp.gdporch.service;

import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import it.csipiemonte.gdp.gdporch.service.impl.GdpCtrlEdizioneAcquisitaServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.lang.reflect.Method;

class GdpCtrlEdizioneAcquisitaServiceTest {

    @Test
    void testClassifyEditionType() throws Exception {
        GdpCtrlEdizioneAcquisitaServiceImpl service = new GdpCtrlEdizioneAcquisitaServiceImpl(null, null, null, null, null, null);

        Method method = GdpCtrlEdizioneAcquisitaServiceImpl.class.getDeclaredMethod("classifyEditionType",
                LocalDate.class, GdpDataUscita.class);
        method.setAccessible(true);

        LocalDate today = LocalDate.now();
        GdpDataUscita dataUscita = new GdpDataUscita();
        dataUscita.sospesa = false;

        // OK
        String result = (String) method.invoke(service, today, dataUscita);
        Assertions.assertEquals("OK", result);

        // AN (anticipataria)
        result = (String) method.invoke(service, today.plusDays(1), dataUscita);
        Assertions.assertEquals("AN", result);

        // PO (posticipataria)
        result = (String) method.invoke(service, today.minusDays(1), dataUscita);
        Assertions.assertEquals("PO", result);

        // SO (sospesa)
        dataUscita.sospesa = true;
        result = (String) method.invoke(service, today, dataUscita);
        Assertions.assertEquals("SO", result);

        // AA (non presente)
        result = (String) method.invoke(service, today, null);
        Assertions.assertEquals("AA", result);
    }
}
