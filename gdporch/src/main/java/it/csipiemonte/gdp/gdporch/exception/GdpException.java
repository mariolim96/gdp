package it.csipiemonte.gdp.gdporch.exception;

/**
 * Custom runtime exception to represent business errors in the GdP application.
 * Stores a code and a detailed message for standard response generation.
 */
public class GdpException extends RuntimeException {

    private final String codice;

    public GdpException(GdpMessage message) {
        super(message.getDescrizioneDefault());
        this.codice = message.getCodice();
    }

    public GdpException(GdpMessage message, String customMessage) {
        super(customMessage);
        this.codice = message.getCodice();
    }

    public GdpException(String codice, String message) {
        super(message);
        this.codice = codice;
    }

    public String getCodice() {
        return codice;
    }
}
