package it.csipiemonte.gdp.gdporch.controller;

import it.csipiemonte.gdp.gdporch.dto.DateAtteseList;
import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.service.GdpVerifDateAtteseService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;

@Path("/bo/testate")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GdpVerifDateAtteseController {

    @Inject
    GdpVerifDateAtteseService service;

    @GET
    @Path("/{idTestata}/date-attese")
    public Response getBoDateAttese(@QueryParam("dataInizio") @Valid LocalDate dataInizio,
            @QueryParam("dataFine") @Valid LocalDate dataFine,
            @PathParam("idTestata") Integer idTestata) {

        DateRangeRequest request = new DateRangeRequest().dataInizio(dataInizio).dataFine(dataFine);
        DateAtteseList response = service.execute(request, idTestata);

        return Response.ok(response).build();
    }

    @GET
    @Path("/date-attese")
    public Response getBoDateAtteseWithoutTestata(@QueryParam("dataInizio") @Valid LocalDate dataInizio,
            @QueryParam("dataFine") @Valid LocalDate dataFine) {

        DateRangeRequest request = new DateRangeRequest().dataInizio(dataInizio).dataFine(dataFine);
        DateAtteseList response = service.execute(request, null);

        return Response.ok(response).build();
    }
}