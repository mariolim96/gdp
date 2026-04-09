package it.csipiemonte.gdp.gdporch.exception;

import it.csipiemonte.gdp.gdporch.dto.GenericProcessResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Fallback Exception Handler to catch all unexpected errors.
 * Ensures the client always receives a standard GenericProcessResponse.
 */
@Provider
public class GenericExceptionHandler implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionHandler.class);

    @Override
    public Response toResponse(Throwable exception) {
        LOG.error("Unexpected error occurred", exception);

        GenericProcessResponse errorResponse = new GenericProcessResponse();
        errorResponse.setCodice(GdpMessage.F_ERROR.getCodice());
        errorResponse.setMessaggio("Errore interno del server: " + exception.getMessage());

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
    }
}
