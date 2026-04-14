package it.csipiemonte.gdp.gdporch.controller;

import it.csipiemonte.gdp.gdporch.api.ApiApi;
import it.csipiemonte.gdp.gdporch.dto.EdizioneInsertResponse;
import it.csipiemonte.gdp.gdporch.dto.GenericProcessResponse;
import it.csipiemonte.gdp.gdporch.dto.XmlCreationRequest;
import it.csipiemonte.gdp.gdporch.dto.XmlCreationResponse;
import it.csipiemonte.gdp.gdporch.model.entity.GdpEdizione;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLogEdizione;
import it.csipiemonte.gdp.gdporch.model.repository.GdpEdizioneRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogEdizioneRepository;
import it.csipiemonte.gdp.gdporch.service.DamTrasmissioneService;
import it.csipiemonte.gdp.gdporch.service.GdpCtrlEdizioneAcquisitaService;
import it.csipiemonte.gdp.gdporch.service.GdpEdizioneService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.Optional;

public class GdpCtrlEdizioneAcquisitaResource implements ApiApi {

    private final GdpCtrlEdizioneAcquisitaService ctrlEdizioneAcquisitaService;
    private final GdpEdizioneService edizioneService;
    private final DamTrasmissioneService trasmissionService;
    private final GdpLogEdizioneRepository  gdpLogEdizioneRepository;
    private final GdpEdizioneRepository edizioneRepository;

    @Inject
    public GdpCtrlEdizioneAcquisitaResource(
            GdpCtrlEdizioneAcquisitaService ctrlEdizioneAcquisitaService,
            GdpEdizioneService edizioneService,
            DamTrasmissioneService trasmissionService,
            GdpLogEdizioneRepository gdpLogEdizioneRepository,
            GdpEdizioneRepository edizioneRepository) {
        this.ctrlEdizioneAcquisitaService = ctrlEdizioneAcquisitaService;
        this.edizioneService = edizioneService;
        this.trasmissionService = trasmissionService;
        this.gdpLogEdizioneRepository = gdpLogEdizioneRepository;
        this.edizioneRepository = edizioneRepository;
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
    public Response orchInternalCreaXML(XmlCreationRequest req) {
        // Find idEdizione from testata and date as it's missing from the request body in OpenAPI spec
        var edizione = edizioneRepository.findByTestataAndData(req.getIdTestata(), req.getDataEdizione())
                .orElseThrow(() -> new jakarta.ws.rs.WebApplicationException("Edizione non trovata", 404));

        XmlCreationResponse result = trasmissionService.creaXMLEdizione(
                req.getIdTestata(), 
                req.getIdLog(), 
                edizione.id, 
                req.getPriorita().value()
        );
        return Response.ok(result).build();
    }
}
