package it.csipiemonte.gdp.gdporch.service;

import it.csipiemonte.gdp.gdporch.dto.XmlCreationResponse;

public interface DamTrasmissioneService {
    
    /**
     * F09 — DAMtrasmissione.creaXMLEdizione
     * Generate XML metadata + .zip package for DAM transmission.
     * 
     * @param idTestata ID of the testata
     * @param idLog ID of the GdpLog record
     * @param idEdizione ID of the GdpEdizione record
     * @param priorita Priority (0 or 100)
     * @return Execution result with zip filename
     */
    XmlCreationResponse creaXMLEdizione(Integer idTestata, Integer idLog, Integer idEdizione, Integer priorita);

    /**
     * Re-enqueues a failed transmission (F21).
     */
    void retry(Integer idLog);

    /**
     * Triggers F10 immediately to flush the queue.
     */
    void flush();

    /**
     * Retrieves the status of a specific DAM job (F20).
     */
    String getJobStatus(String jobId);

    /**
     * Placeholder for manual cleanup (F19).
     */
    void cleanup();
}
