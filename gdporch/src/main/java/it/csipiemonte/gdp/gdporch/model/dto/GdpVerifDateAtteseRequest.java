package it.csipiemonte.gdp.gdporch.model.dto;

import java.time.LocalDate;

import io.smallrye.common.constraint.NotNull;

public class GdpVerifDateAtteseRequest {
    
    @NotNull
    public LocalDate dataInizio;

    @NotNull
    public LocalDate dataFine;

    public Long idTestata; // opzionale
    
}