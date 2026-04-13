package it.csipiemonte.gdp.gdporch.service;

import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.dto.DateAtteseList;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public interface GdpVerifDateAtteseService {

    public DateAtteseList execute(DateRangeRequest request, Integer idTestata);
}