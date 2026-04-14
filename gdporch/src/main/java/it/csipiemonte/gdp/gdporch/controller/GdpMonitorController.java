package it.csipiemonte.gdp.gdporch.controller;

import it.csipiemonte.gdp.gdporch.service.GdpMonitorService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;

@Path("/bo")
public class GdpMonitorController {

    private final GdpMonitorService monitorService;

    @Inject
    public GdpMonitorController(GdpMonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GET
    @Path("/acquisizioni")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBoAcquisizioni(@QueryParam("dataAcquisizione") LocalDate data,
            @QueryParam("tipoAcquisizione") String tipo) {
        return Response.ok(monitorService.elencoAcquisizioni(tipo, data)).build();
    }

    @GET
    @Path("/acquisizioni/{idLog}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDettaglioAcquisizione(@PathParam("idLog") Integer idLog) {
        return Response.ok(monitorService.dettaglioAcquisizione(idLog)).build();
    }

    @GET
    @Path("/acquisizioni/{idLog}/edizioni-storiche")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDettaglioAcquisizioneEdizioniStoriche(@PathParam("idLog") Integer idLog) {
        return Response.ok(monitorService.dettaglioAcquisizione(idLog)).build();
    }
}