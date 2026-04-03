package it.csipiemonte.gdp.gdporch.model.controller;

import it.csipiemonte.gdp.gdporch.model.dto.GdpVerifDateAtteseRequest;
import it.csipiemonte.gdp.gdporch.model.dto.GdpVerifDateAtteseResponse;
import it.csipiemonte.gdp.gdporch.model.service.GdpVerifDateAtteseService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/bo/testate/verif-date-attese")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GdpVerifDateAtteseController {

    @Inject
    GdpVerifDateAtteseService service;

    @POST
    public Response postBoExecute(@Valid GdpVerifDateAtteseRequest request) {

        GdpVerifDateAtteseResponse response = service.execute(request);

        return Response.ok(response).build();
    }
}