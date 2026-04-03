package it.csipiemonte.gdp.gdporch.service;

import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.dto.SospensioneResponse;

public interface GdpSospensioneService {

    SospensioneResponse sospendi(Long idTestata, DateRangeRequest request);

}
