package it.csipiemonte.gdp.gdporch.utils;

import it.csipiemonte.gdp.gdporch.exception.GdpException;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import jakarta.ws.rs.core.Response;
import java.util.Optional;

/**
 * Utility class for common preconditions and assertions in the GdP application.
 * Throws GdpException when conditions are not met.
 */
public class GdpPreconditions {

    private GdpPreconditions() {
        // Utility class
    }

    /**
     * Ensures an Optional is present, otherwise throws GdpException with NOT_FOUND status.
     */
    public static <T> T checkFound(Optional<T> optional, String message) {
        return optional.orElseThrow(() -> GdpException.notFound(message));
    }

    /**
     * Ensures an Optional is present, otherwise throws GdpException with specific GdpMessage.
     */
    public static <T> T checkFound(Optional<T> optional, GdpMessage message) {
        return optional.orElseThrow(() -> new GdpException(message, message.getDescrizioneDefault(), Response.Status.NOT_FOUND));
    }

    /**
     * Ensures a condition is true, otherwise throws GdpException with BAD_REQUEST status.
     */
    public static void checkArgument(boolean condition, GdpMessage message) {
        if (!condition) {
            throw new GdpException(message);
        }
    }

    /**
     * Ensures a condition is true, otherwise throws GdpException with BAD_REQUEST status and custom message.
     */
    public static void checkArgument(boolean condition, GdpMessage message, String customMessage) {
        if (!condition) {
            throw new GdpException(message, customMessage);
        }
    }

    /**
     * Ensures an object is not null, otherwise throws GdpException with BAD_REQUEST status.
     */
    public static <T> T checkNotNull(T reference, GdpMessage message) {
        if (reference == null) {
            throw new GdpException(message);
        }
        return reference;
    }
}
