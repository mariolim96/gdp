package it.csipiemonte.gdp.gdporch.model.enums;

/**
 * Mail notification types from GDP_MAIL.COD_MAIL.
 */
public enum TipoMail {
    DAILY_INTEGRATION_WARNING("GW001"),
    DAILY_ERROR("GE001"),
    HISTORICAL_INTEGRATION_WARNING("SW001"),
    HISTORICAL_ERROR("SE001"),
    HISTORICAL_FULL_ERROR("ST001");

    private final String value;

    TipoMail(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TipoMail fromValue(String value) {
        for (TipoMail t : TipoMail.values()) {
            if (t.value.equalsIgnoreCase(value)) return t;
        }
        return DAILY_INTEGRATION_WARNING;
    }
}
