package it.csipiemonte.gdp.gdporch.service;

import it.csipiemonte.gdp.gdporch.dto.GenericProcessResponse;
import java.time.LocalDate;

public interface GdpCtrlEdizioneAcquisitaService {
    
    /**
     * F04 — FTPregolare.ctrlEdizioneAcquisita
     * Validate acquired periodic edition — full PDF-level control pipeline.
     * 
     * @param idTestata ID of the testata
     * @param carteliaTestata Folder name on SFTP
     * @param dataEdizione Expected edition date
     * @param idLog Reference ID for GDP_LOG
     * @return Execution result with code (e.g., MSG00009, MSG00001)
     */
    GenericProcessResponse ctrlEdizioneAcquisita(Integer idTestata, String carteliaTestata, LocalDate dataEdizione, Integer idLog);
}
