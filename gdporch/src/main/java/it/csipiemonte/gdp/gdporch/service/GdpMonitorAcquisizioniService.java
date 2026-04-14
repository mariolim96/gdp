package it.csipiemonte.gdp.gdporch.service;

import it.csipiemonte.gdp.gdporch.dto.AcquisizioneRicercaList;
import it.csipiemonte.gdp.gdporch.dto.TipoEdizione;
import java.time.LocalDate;

public interface GdpMonitorAcquisizioniService {

    AcquisizioneRicercaList ricercaAcquisizioni(
            String tipoAcquisizione,
            Integer idTestata,
            LocalDate dataA,
            TipoEdizione tipoEdizione,
            LocalDate dataDA);
}