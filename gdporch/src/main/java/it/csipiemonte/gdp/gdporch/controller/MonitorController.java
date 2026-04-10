package it.csipiemonte.gdp.gdporch.controller;

import it.csipiemonte.gdp.gdporch.service.GdpMonitorStatoDamService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/orch/acquisizioni")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MonitorController {

    @Inject
    GdpMonitorStatoDamService gdpMonitorStatoDamService;

    @GET
    @Path("/{idLog}/stato-dam")
    public Response getStatoDam(@PathParam("idLog") Long idLog) {
        String outcome = gdpMonitorStatoDamService.recuperaStatoDam(idLog);
        return Response.ok(outcome).build();
    }
}