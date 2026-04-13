package it.csipiemonte.gdp.gdporch.model.projection;

import it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AcquisizioneRicercaProjection(
        Integer idLog,
        Integer idTestata,
        String nomeTestata,
        TipoEdizione tipoEdizione,
        LocalDate dataEdizione,
        LocalDateTime dataAcquisizione,
        Integer nroTotFileAcq,
        Integer nroTotFileVal) {
}