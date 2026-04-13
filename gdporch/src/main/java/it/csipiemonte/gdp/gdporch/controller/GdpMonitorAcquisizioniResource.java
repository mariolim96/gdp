package it.csipiemonte.gdp.gdporch.controller;

import it.csipiemonte.gdp.gdporch.dto.AcquisizioneRicercaList;
import it.csipiemonte.gdp.gdporch.dto.TipoEdizione;
import it.csipiemonte.gdp.gdporch.service.GdpMonitorAcquisizioniService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;

@Path("/bo/acquisizioni")
@Produces(MediaType.APPLICATION_JSON)
public class GdpMonitorAcquisizioniResource {

    private final GdpMonitorAcquisizioniService monitorAcquisizioniService;

    @Inject
    public GdpMonitorAcquisizioniResource(GdpMonitorAcquisizioniService monitorAcquisizioniService) {
        this.monitorAcquisizioniService = monitorAcquisizioniService;
    }

    @GET
    @Path("/ricerca")
    public Response ricercaAcquisizioni(
            @QueryParam("tipoAcquisizione") String tipoAcquisizione,
            @QueryParam("idTestata") Integer idTestata,
            @QueryParam("dataA") LocalDate dataA,
            @QueryParam("tipoEdizione") TipoEdizione tipoEdizione,
            @QueryParam("dataDA") LocalDate dataDA) {
        AcquisizioneRicercaList result = monitorAcquisizioniService.ricercaAcquisizioni(
                tipoAcquisizione,
                idTestata,
                dataA,
                tipoEdizione,
                dataDA);
        return Response.ok(result).build();
    }
}