package it.csipiemonte.gdp.gdporch.controller;

import it.csipiemonte.gdp.gdporch.api.ApiApi;
import it.csipiemonte.gdp.gdporch.dto.EdizioneInsertResponse;
import it.csipiemonte.gdp.gdporch.dto.GenericProcessResponse;
import it.csipiemonte.gdp.gdporch.dto.XmlCreationResponse;
import it.csipiemonte.gdp.gdporch.service.DamTrasmissioneService;
import it.csipiemonte.gdp.gdporch.service.GdpCtrlEdizioneAcquisitaService;
import it.csipiemonte.gdp.gdporch.service.GdpEdizioneService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;

public class GdpCtrlEdizioneAcquisitaResource implements ApiApi {

    private final GdpCtrlEdizioneAcquisitaService ctrlEdizioneAcquisitaService;
    private final GdpEdizioneService edizioneService;
    private final DamTrasmissioneService trasmissionService;

    @Inject
    public GdpCtrlEdizioneAcquisitaResource(
            GdpCtrlEdizioneAcquisitaService ctrlEdizioneAcquisitaService,
            GdpEdizioneService edizioneService,
            DamTrasmissioneService trasmissionService) {
        this.ctrlEdizioneAcquisitaService = ctrlEdizioneAcquisitaService;
        this.edizioneService = edizioneService;
        this.trasmissionService = trasmissionService;
    }

    @Override
    public Response postInternalAcquisizioneValida(Integer idTestata, String cartellaTestata, LocalDate dataEdizione,
            Integer idLog) {
        GenericProcessResponse result = ctrlEdizioneAcquisitaService.ctrlEdizioneAcquisita(idLog, cartellaTestata,
                dataEdizione.toString(), "MANUAL", 0);
        return Response.ok(result).build();
    }

    @Override
    public Response postInternalEdizioneInserisci(String path, LocalDate dataEdizione, Integer idLog,
            Integer idTestata) {
        EdizioneInsertResponse result = edizioneService.insEdizione(idTestata, path, dataEdizione, idLog);
        return Response.ok(result).build();
    }

    @Override
    public Response postInternalEdizioneTrasmetti(Integer idLog, Integer priorita, Integer idTestata,
            Integer idEdizione) {
        XmlCreationResponse result = trasmissionService.creaXMLEdizione(idTestata, idLog, idEdizione, priorita);
        return Response.ok(result).build();
    }
}
