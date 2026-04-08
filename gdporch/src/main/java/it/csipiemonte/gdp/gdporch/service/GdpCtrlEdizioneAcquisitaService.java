package it.csipiemonte.gdp.gdporch.service;

import it.csipiemonte.gdp.gdporch.dto.GenericProcessResponse;


public interface GdpCtrlEdizioneAcquisitaService {
    
    /**
     * F04 — FTPregolare.ctrlEdizioneAcquisita
     * Validate acquired periodic edition — full PDF-level control pipeline.
     * 
     * @param idTestata ID of the testata
     * @param carteliaTestata Folder name on SFTP
     * @param dataEdizione Expected edition date
     * @param idLog Reference ID for GDP_LOG
     * @return Execution result with GdpMessage code (e.g., F_OK, F_ERROR)
     */
    GenericProcessResponse ctrlEdizioneAcquisita(Integer idLog, String cartellaTestata, String nomeEdizione, String dataAcquisizione, Integer fileCount);
}
