package it.csipiemonte.gdp.gdporch.model.enums;

import java.util.Arrays;

/**
 * Stabilized Edition Types from GDP_TIPO_EDIZIONE table.
 * Enum names match DB codes for direct mapping.
 */
public enum TipoEdizione {
    OK("Regolare (Corrispondente)"),
    SO("Sospesa"),
    AN("Anticipataria"),
    PO("Posticipataria"),
    AA("Anomala edizione attesa"),
    ST("Edizione storica"),
    AS("Edizione storica con anomalia");

    private final String descrizione;

    TipoEdizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public static TipoEdizione fromValue(String value) {
        return Arrays.stream(TipoEdizione.values())
                .filter(t -> t.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(AA);
    }
}
