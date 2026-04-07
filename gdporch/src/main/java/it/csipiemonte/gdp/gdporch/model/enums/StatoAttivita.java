package it.csipiemonte.gdp.gdporch.model.enums;

import java.util.Arrays;

/**
 * Status of activity (0 = Attiva, 1 = Storica).
 */
public enum StatoAttivita {
    ATTIVA(0),
    STORICA(1);

    private final Integer value;

    StatoAttivita(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static StatoAttivita fromValue(Integer value) {
        return Arrays.stream(StatoAttivita.values())
                .filter(s -> s.value.equals(value))
                .findFirst()
                .orElse(ATTIVA);
    }
}
