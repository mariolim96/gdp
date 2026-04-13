package it.csipiemonte.gdp.gdporch.service;


import it.csipiemonte.gdp.gdporch.dto.DamStatusResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "gdporch-api")
public interface GdporchRestClient {

    @GET
    @Path("/api/v2/success")
    @Produces(MediaType.APPLICATION_JSON)
    DamStatusResponse getStatoLibra(@QueryParam("jobID") String jobID);
}