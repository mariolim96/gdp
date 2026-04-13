package it.csipiemonte.gdp.gdporch.service;

/**
 * Operazione: F07 - FTPsaltuario.ctrlEdizioniStoriche
 */
public interface GdpCtrlEdizioniStoricheService {

    /**
     * @param idTestata ID della testata (es. 10)
     * @param cartellaTestata Nome cartella (es. 'la-sentinella')
     * @param dataConsegna Data della consegna (formato YYYYMMDD)
     * @param idLog ID record GDP_LOG
     */
    void ctrlEdizioniStoriche(Integer idTestata, String cartellaTestata,
                              String dataConsegna, Integer idLog);
}