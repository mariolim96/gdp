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

    // F10: Notifica che il file è pronto per essere inviato al DAM
    void inviaEdizioneAsync(Integer idLog, Integer idEdizione, String nomeFileCompresso);
}
