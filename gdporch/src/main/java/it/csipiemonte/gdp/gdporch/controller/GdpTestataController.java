package it.csipiemonte.gdp.gdporch.controller;

import it.csipiemonte.gdp.gdporch.service.GdpTestataService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/bo/testate")
public class GdpTestataController {

    private final GdpTestataService gdpTestataService;

    @Inject
    public GdpTestataController(GdpTestataService gdpTestataService) {
        this.gdpTestataService = gdpTestataService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBoTestate(@QueryParam("invioEdizione") Boolean invioEdizione,
            @QueryParam("prov") String prov,
            @QueryParam("idTestata") Integer idTestata) {
        return Response.ok(gdpTestataService.elencoTestate(invioEdizione, prov, idTestata)).build();
    }
}
