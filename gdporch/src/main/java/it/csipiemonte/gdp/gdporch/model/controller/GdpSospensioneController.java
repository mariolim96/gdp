package it.csipiemonte.gdp.gdporch.model.controller;

import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.dto.SospensioneResponse;
import it.csipiemonte.gdp.gdporch.model.service.GdpSospensioneService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/bo/testate/{idTestata}/sospensioni")
public class GdpSospensioneController {

    @Inject
    GdpSospensioneService sospensioneService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postBoSospensioni(@PathParam("idTestata") Long idTestata, @Valid DateRangeRequest request) {
        SospensioneResponse response = sospensioneService.sospendi(idTestata, request);
        return Response.ok(response).build();
    }
}
