package it.csipiemonte.gdp.gdporch.controller;

import it.csipiemonte.gdp.gdporch.api.ApiApi;
import it.csipiemonte.gdp.gdporch.dto.EdizioneInsertResponse;
import it.csipiemonte.gdp.gdporch.dto.GenericProcessResponse;
import it.csipiemonte.gdp.gdporch.dto.XmlCreationResponse;
import it.csipiemonte.gdp.gdporch.model.entity.GdpEdizione;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLogEdizione;
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
    @Inject
    public GdpCtrlEdizioneAcquisitaResource(
            GdpCtrlEdizioneAcquisitaService ctrlEdizioneAcquisitaService,
            GdpEdizioneService edizioneService,
            DamTrasmissioneService trasmissionService,
            GdpLogEdizioneRepository gdpLogEdizioneRepository) {
        this.ctrlEdizioneAcquisitaService = ctrlEdizioneAcquisitaService;
        this.edizioneService = edizioneService;
        this.trasmissionService = trasmissionService;
        this.gdpLogEdizioneRepository = gdpLogEdizioneRepository;
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
    public Response orchInternalCreaXML(Integer idLog, Integer priorita, Integer idTestata,
            Integer idEdizione) {
        Optional<GdpLogEdizione> logEd = gdpLogEdizioneRepository.find("fkGdpLog = ?1 and fkGdpEdizione = ?2",idLog,idEdizione).stream().findFirst();
        String pathRecuperato = logEd.map(le -> le.pathEdizione).orElse("");
        //Chiamata F09 con firma nuova
        XmlCreationResponse result = trasmissionService.creaXMLEdizione(idTestata, idLog, idEdizione, priorita);
        return Response.ok(result).build();
    }
}
