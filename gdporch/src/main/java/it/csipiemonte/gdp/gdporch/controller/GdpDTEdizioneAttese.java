package it.csipiemonte.gdp.gdporch.controller;

import io.quarkus.logging.Log;
import it.csipiemonte.gdp.gdporch.dto.DateAtteseList;
import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import it.csipiemonte.gdp.gdporch.dto.DateAttesePerTestata;
import it.csipiemonte.gdp.gdporch.mapper.GdpDataUscitaMapper;
import it.csipiemonte.gdp.gdporch.model.repository.GdpPeriodicitaRepository;
import it.csipiemonte.gdp.gdporch.service.ConfigDTEdizioneAttesaService;
import it.csipiemonte.gdp.gdporch.service.VerifDateAtteseService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Path("/bo/testate/{idTestata}/date-attese")
public class GdpDTEdizioneAttese {
    @Inject
    ConfigDTEdizioneAttesaService configDTEdizioneAttesaService;

    @Inject
    GdpPeriodicitaRepository periodicitaRepo;

    @Inject
    GdpDataUscitaMapper mapper;

    @Inject
    VerifDateAtteseService verifService;

    // GET /bo/testate/{idTestata}/date-attese
    @GET
    @Produces("application/json")
    public Response getDateAttese(
            @PathParam("idTestata") Integer idTestata,
            @QueryParam("dataInizio") LocalDate dataInizio,
            @QueryParam("dataFine") LocalDate dataFine) {

        DateAtteseList result = verifService.recuperaDateAttese(idTestata, dataInizio, dataFine);
        return Response.ok(result).build();
    }

    // POST /bo/testate/{idTestata}/date-attese
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response postDateAttese(@PathParam("idTestata") Integer idTestata, DateRangeRequest request) {

        if (idTestata != null) {
            request.setIdTestata(idTestata);
        }

        CompletableFuture.runAsync(() -> {
            try {
                List<GdpDataUscita> entities = configDTEdizioneAttesaService.calcoloUscite(request);

                Map<Integer, GdpPeriodicita> periodicitaMap = periodicitaRepo.findAll()
                        .stream()
                        .collect(Collectors.toMap(GdpPeriodicita::getId, p -> p));

                List<DateAttesePerTestata> testateDto =
                        mapper.toDateAttesePerTestata(entities, periodicitaMap);

                Log.infof("Elaborate %d testate", testateDto.size());

            } catch (Exception e) {
                Log.error("Errore nell'elaborazione asincrona delle date attese", e);
            }
        });

        return Response.accepted().build();
    }
}