package it.csipiemonte.gdp.gdporch.exception;

import it.csipiemonte.gdp.gdporch.dto.GenericProcessResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Global Exception Handler for the GdP application.
 * Catches GdpException and converts it to a GenericProcessResponse with code and message,
 * respecting the HTTP status defined in the exception.
 */
@Provider
public class GdpExceptionHandler implements ExceptionMapper<GdpException> {

    private static final Logger LOG = Logger.getLogger(GdpExceptionHandler.class);

    @Override
    public Response toResponse(GdpException exception) {
        LOG.warnf("Caught GdpException [code=%s, status=%s]: %s", 
                exception.getCodice(), exception.getStatus(), exception.getMessage());

        GenericProcessResponse errorResponse = new GenericProcessResponse();
        errorResponse.setCodice(exception.getCodice());
        errorResponse.setMessaggio(exception.getMessage());

        return Response.status(exception.getStatus())
                .entity(errorResponse)
                .build();
    }
}

