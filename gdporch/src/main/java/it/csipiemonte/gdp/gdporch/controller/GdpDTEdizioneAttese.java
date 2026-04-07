package it.csipiemonte.gdp.gdporch.controller;

import io.quarkus.logging.Log;
import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import it.csipiemonte.gdp.gdporch.dto.DateAtteseList;
import it.csipiemonte.gdp.gdporch.dto.DateAttesePerTestata;
import it.csipiemonte.gdp.gdporch.mapper.GdpDataUscitaMapper;
import it.csipiemonte.gdp.gdporch.model.repository.GdpPeriodicitaRepository;
import it.csipiemonte.gdp.gdporch.service.ConfigDTEdizioneAttesaService;
import it.csipiemonte.gdp.gdporch.service.VerifDateAtteseService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;


import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GdpDTEdizioneAttese extends BoApiDelegate {

    @Inject
    ConfigDTEdizioneAttesaService configDTEdizioneAttesaService;

    @Inject
    GdpPeriodicitaRepository periodicitaRepo;

    @Inject
    GdpDataUscitaMapper mapper;

    @Inject
    VerifDateAtteseService verifService;

    @Override
    public Response getBoDateAttese(java.time.LocalDate dataInizio, java.time.LocalDate dataFine, Long idTestata) {
        DateAtteseList result = verifService.recuperaDateAttese(idTestata, dataInizio, dataFine);
        return Response.ok(result).build(); // 200 OK
    }

    @Override
    public Response postBoDateAttese(Long idTestata, DateRangeRequest request) {
        if (idTestata != null) {
            request.setIdTestata(idTestata.intValue());
        }

        // Esecuzione asincrona con CompletableFuture
        CompletableFuture.runAsync(() -> {
            try {
                List<GdpDataUscita> entities = configDTEdizioneAttesaService.calcoloUscite(request);
                Map<Integer, GdpPeriodicita> periodicitaMap = periodicitaRepo.findAll()
                        .stream()
                        .collect(Collectors.toMap(GdpPeriodicita::getId, p -> p));
                List<DateAttesePerTestata> testateDto = mapper.toDateAttesePerTestata(entities, periodicitaMap);

                Log.infof("Elaborate %d testate", testateDto.size());

            } catch (Exception e) {
                Log.error("Errore nell'elaborazione asincrona delle date attese", e);
            }
        });

        return Response.accepted().build();
    }
}