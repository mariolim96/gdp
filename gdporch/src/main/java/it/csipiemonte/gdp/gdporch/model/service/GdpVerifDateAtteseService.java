package it.csipiemonte.gdp.gdporch.model.service;

import it.csipiemonte.gdp.gdporch.model.dto.GdpVerifDateAtteseRequest;
import it.csipiemonte.gdp.gdporch.model.dto.GdpVerifDateAtteseResponse;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
public interface GdpVerifDateAtteseService {

    public GdpVerifDateAtteseResponse execute(GdpVerifDateAtteseRequest request);
}
