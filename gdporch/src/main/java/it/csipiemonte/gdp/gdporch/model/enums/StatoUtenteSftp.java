package it.csipiemonte.gdp.gdporch.model.enums;

/**
 * SFTP user status.
 */
public enum StatoUtenteSftp {
    ATTIVO("attivo"),
    SOSPESO("sospeso");

    private final String value;

    StatoUtenteSftp(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StatoUtenteSftp fromValue(String value) {
        for (StatoUtenteSftp s : StatoUtenteSftp.values()) {
            if (s.value.equalsIgnoreCase(value)) return s;
        }
        return ATTIVO;
    }
}
