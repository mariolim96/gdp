package it.csipiemonte.gdp.gdporch.service;

import it.csipiemonte.gdp.gdporch.dto.TestataDetail;
import it.csipiemonte.gdp.gdporch.dto.TestataSummaryList;

public interface GdpTestataService {

    /**
     * F16 — DB.getElencoTestate
     * Returns a filtered list of {@code TestataSummary}. Only one filter may be
     * provided at a time.
     *
     * @param invioEdizione filter senders only when true
     * @param prov          filter by province code
     * @param idTestata     filter by specific testata ID
     * @return filtered list of testata summaries
     */
    TestataSummaryList elencoTestate(Boolean invioEdizione, String prov, Integer idTestata);

    /**
     * F17 — DB.getTestata
     * Returns all fields of a specific testata.
     *
     * @param idTestata the testata ID
     * @return detailed testata information
     */
    TestataDetail getTestataById(Integer idTestata);
}
