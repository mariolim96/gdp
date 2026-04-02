package it.csipiemonte.gdp.gdporch.model.entity;

import java.time.LocalDate;

import io.smallrye.common.constraint.NotNull;

public class GdpSospensioneRequest {

    @NotNull
    public Integer idTestata;

    @NotNull
    public LocalDate dataInizio;

    @NotNull
    public LocalDate dataFine;
}