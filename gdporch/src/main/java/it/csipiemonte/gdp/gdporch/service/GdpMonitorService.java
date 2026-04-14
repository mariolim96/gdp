package it.csipiemonte.gdp.gdporch.service;

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


    /*F21 MONITOR.attivaCODA
     *@param idLog identificativo del log di trasmissione (richiesto da specifica)
     * @param idLogEdizione identificativo specifico dell'edizione (indispensabile per univocità)
     * @return Codice esito: MSG00009 (Successo) o MSG00001 (Superato limite tentativi)
     */

    String attivaCoda(Integer idLog,Integer idGdpEdizione);
}