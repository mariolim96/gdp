package it.csipiemonte.gdp.gdporch.exception;

import it.csipiemonte.gdp.gdporch.dto.GenericProcessResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Global Exception Handier for the GdP application.
 * catches GdpException and converts it to a GenericProcessResponse with code and message.
 */
@Provider
public class GdpExceptionHandler implements ExceptionMapper<GdpException> {

    private static final Logger LOG = Logger.getLogger(GdpExceptionHandler.class);

    @Override
    public Response toResponse(GdpException exception) {
        LOG.warnf("Caught GdpException [code=%s]: %s", exception.getCodice(), exception.getMessage());

        GenericProcessResponse errorResponse = new GenericProcessResponse();
        errorResponse.setCodice(exception.getCodice());
        errorResponse.setMessaggio(exception.getMessage());

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
    }
}
