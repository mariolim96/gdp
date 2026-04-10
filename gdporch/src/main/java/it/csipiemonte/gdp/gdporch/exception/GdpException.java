package it.csipiemonte.gdp.gdporch.exception;

import jakarta.ws.rs.core.Response;

/**
 * Custom runtime exception to represent business errors in the GdP application.
 * Stores a code, a detailed message, and an optional HTTP status for standard response generation.
 */
public class GdpException extends RuntimeException {

    private final String codice;
    private final Response.Status status;

    public GdpException(GdpMessage message) {
        this(message, message.getDescrizioneDefault(), Response.Status.BAD_REQUEST);
    }

    public GdpException(GdpMessage message, String customMessage) {
        this(message, customMessage, Response.Status.BAD_REQUEST);
    }

    public GdpException(GdpMessage message, String customMessage, Response.Status status) {
        super(customMessage);
        this.codice = message.getCodice();
        this.status = status;
    }

    public GdpException(String codice, String message) {
        this(codice, message, Response.Status.BAD_REQUEST);
    }

    public GdpException(String codice, String message, Response.Status status) {
        super(message);
        this.codice = codice;
        this.status = status;
    }

    public String getCodice() {
        return codice;
    }

    public Response.Status getStatus() {
        return status;
    }

    /**
     * Utility to throw a NOT_FOUND exception with F_NOT_FOUND code.
     */
    public static GdpException notFound(String message) {
        return new GdpException(GdpMessage.F_NOT_FOUND, message, Response.Status.NOT_FOUND);
    }

    /**
     * Utility to throw an internal error with F_ERROR code.
     */
    /**
     * Utility to throw a CONFLICT exception with F04_DB_ERROR code (or generic if preferred).
     */
    public static GdpException conflict(String message) {
        return new GdpException(GdpMessage.F_ERROR, message, Response.Status.CONFLICT);
    }
}


