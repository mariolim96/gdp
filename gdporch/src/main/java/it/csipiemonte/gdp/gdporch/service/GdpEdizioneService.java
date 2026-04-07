package it.csipiemonte.gdp.gdporch.service;

import it.csipiemonte.gdp.gdporch.dto.EdizioneInsertResponse;
import java.time.LocalDate;

public interface GdpEdizioneService {
    
    /**
     * F08 — DB.insEdizione
     * Insert or update edition and page records in DB.
     * 
     * @param idTestata ID of the testata
     * @param path Staging path on SFTP where files are located
     * @param dataEdizione Edition date
     * @param idLog ID of the GdpLog record
     * @return Execution result with idEdizione
     */
    EdizioneInsertResponse insEdizione(Integer idTestata, String path, LocalDate dataEdizione, Integer idLog);
}
