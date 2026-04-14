package it.csipiemonte.gdp.gdporch.service;

import it.csipiemonte.gdp.gdporch.dto.AcquisizioneDetail;
import it.csipiemonte.gdp.gdporch.dto.AcquisizioneList;
import java.time.LocalDate;

public interface GdpMonitorService {

    /**
     * F12 — MONITOR.elencoAcquisizioni
     * Returns the list of acquisitions by type and date.
     *
     * @param tipoAcquisizione acquisition type (G or S)
     * @param dataAcquisizione acquisition date
     * @return list of acquisitions
     */
    AcquisizioneList elencoAcquisizioni(String tipoAcquisizione, LocalDate dataAcquisizione);

    /**
     * F13 — MONITOR.dettaglioAcquisizione
     * Returns detail for a single acquisition identified by idLog.
     *
     * @param idLog log identifier
     * @return acquisition detail
     */
    AcquisizioneDetail dettaglioAcquisizione(Integer idLog);
}