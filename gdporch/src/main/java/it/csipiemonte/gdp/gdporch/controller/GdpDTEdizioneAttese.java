package it.csipiemonte.gdp.gdporch.controller;

import io.quarkus.logging.Log;
import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.dto.AsyncJobResponse;
import it.csipiemonte.gdp.gdporch.service.ConfigDTEdizioneAttesaService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Path("/bo/date-attese")
public class GdpDTEdizioneAttese {

    @Inject
    ConfigDTEdizioneAttesaService configDTEdizioneAttesaService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postDateAttese(@Valid DateRangeRequest request) {

        String jobId = UUID.randomUUID().toString();
        Log.infof("Ricevuta richiesta calcolo date attese. JobId: %s", jobId);

        CompletableFuture.runAsync(() -> {
            try {
                configDTEdizioneAttesaService.calcoloUscite(request);
                Log.infof("Job %s completato con successo", jobId);
            } catch (Exception e) {
                Log.errorf("Errore nell'elaborazione asincrona delle date attese (JobId: %s)", jobId, e);
            }
        });

        AsyncJobResponse response = new AsyncJobResponse();
        response.setJobId(jobId);
        response.setStatus(AsyncJobResponse.StatusEnum.PROCESSING);

        return Response.accepted(response).build();
    }
}