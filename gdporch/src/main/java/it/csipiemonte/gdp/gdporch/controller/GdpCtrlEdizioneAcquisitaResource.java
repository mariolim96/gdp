package it.csipiemonte.gdp.gdporch.controller;

import it.csipiemonte.gdp.gdporch.api.ApiApi;
import it.csipiemonte.gdp.gdporch.dto.EdizioneInsertResponse;
import it.csipiemonte.gdp.gdporch.dto.GenericProcessResponse;
import it.csipiemonte.gdp.gdporch.dto.XmlCreationRequest;
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
    public Response orchInternalValidaEdizione(Integer idTestata, String cartellaTestata, LocalDate dataEdizione,
            Integer idLog) {
        GenericProcessResponse result = ctrlEdizioneAcquisitaService.ctrlEdizioneAcquisita(idLog, cartellaTestata,
                dataEdizione.toString(), "MANUAL", 0);
        return Response.ok(result).build();
    }

    @Override
    public Response orchInternalInsEdizione(String path, LocalDate dataEdizione, Integer idLog,
            Integer idTestata) {
        EdizioneInsertResponse result = edizioneService.insEdizione(idTestata, path, dataEdizione, idLog);
        return Response.ok(result).build();
    }

    @Override
    public Response orchInternalCreaXML(Integer idTestata, Integer idEdizione, XmlCreationRequest xmlCreationRequest) {
        XmlCreationResponse result = trasmissionService.creaXMLEdizione(idTestata, xmlCreationRequest.getIdLog(), idEdizione, 
                xmlCreationRequest.getPriorita() != null ? xmlCreationRequest.getPriorita().value() : 0);
        return Response.ok(result).build();
    }
}
