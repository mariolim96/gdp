package it.csipiemonte.gdp.gdporch.client;

import it.csipiemonte.gdp.gdporch.dto.LibraImportResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestHeader;

import java.io.File;

@RegisterRestClient(configKey = "libra-api")
@Path("/api/v2")
public interface LibraClient {

    @POST
    @Path("/imports")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    LibraImportResponse uploadZip(
            @RestHeader("Authorization") String authorization,
            @RestForm("file") File file);
}
