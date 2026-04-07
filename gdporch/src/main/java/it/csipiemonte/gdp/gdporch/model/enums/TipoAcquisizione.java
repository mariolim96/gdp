package it.csipiemonte.gdp.gdporch.model.enums;

import java.util.Arrays;

/**
 * Acquisition types from GDP_LOG.TIPO_ACQUISIZIONE.
 */
public enum TipoAcquisizione {
    G("Giornaliera (Daily)"),
    S("Storica (Historical)");

    private final String descrizione;

    TipoAcquisizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public static TipoAcquisizione fromValue(String value) {
        return Arrays.stream(TipoAcquisizione.values())
                .filter(t -> t.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(G);
    }
}
