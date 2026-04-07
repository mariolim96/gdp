package it.csipiemonte.gdp.gdporch.model.enums;

/**
 * Standard states for GDP_CODA_CARICAMENTO (Import Task).
 */
public enum StatoCodaCaricamento {
    PRO("PRO"),
    READY("READY"),
    SUBMITTED("SUBMITTED"),
    FAILED("FAILED");

    private final String value;

    StatoCodaCaricamento(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StatoCodaCaricamento fromValue(String value) {
        for (StatoCodaCaricamento s : StatoCodaCaricamento.values()) {
            if (s.value.equalsIgnoreCase(value)) return s;
        }
        return READY; // Default if unknown
    }
}
