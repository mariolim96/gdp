package it.csipiemonte.gdp.gdporch.model.service;

import it.csipiemonte.gdp.gdporch.model.entity.GdpSospensioneRequest;
import it.csipiemonte.gdp.gdporch.model.entity.GdpSospensioneResponse;
import it.csipiemonte.gdp.gdporch.model.service.serviceImpl.GdpSospensoioneServiceImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GdpSospensioneService {

    @Inject
    GdpSospensoioneServiceImpl gdpSospensoioneServiceImpl;

    public GdpSospensioneResponse sospendi(GdpSospensioneRequest request) {
        return gdpSospensoioneServiceImpl.sospendi(request);
    }

}
