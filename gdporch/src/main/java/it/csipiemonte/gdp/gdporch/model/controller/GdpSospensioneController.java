package it.csipiemonte.gdp.gdporch.model.controller;

import it.csipiemonte.gdp.gdporch.model.entity.GdpSospensioneRequest;
import it.csipiemonte.gdp.gdporch.model.entity.GdpSospensioneResponse;
import it.csipiemonte.gdp.gdporch.model.service.GdpSospensioneService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/sospensione")
@Consumes("application/json")
@Produces("application/json")
public class GdpSospensioneController {

    @Inject
    GdpSospensioneService service;

    @POST
    public GdpSospensioneResponse sospendi(GdpSospensioneRequest request) {
        return service.sospendi(request);
    }
}
