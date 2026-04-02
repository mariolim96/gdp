package it.csipiemonte.gdp.gdporch.model.controller;

import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.dto.ResponseDTO;
import it.csipiemonte.gdp.gdporch.service.UsciteManager;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

@Path("/date-uscite")
@Consumes({MediaType.APPLICATION_JSON})
public class GdpDTEdizioneAttese {

    @Inject
    UsciteManager usciteManager;

    @POST
    public ResponseDTO dateAttese(DateRangeRequest dateRangeRequest) {
        ResponseDTO responseDTO = usciteManager.calcoloUscite(dateRangeRequest);
        return responseDTO;
    }

}