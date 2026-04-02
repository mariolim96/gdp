package it.csipiemonte.gdp.gdporch.model.entity;

public class GdpSospensioneResponse {

    public String esito;
    public Integer giorniEdizione;

    public GdpSospensioneResponse(String esito, Integer giorniEdizione) {
        this.esito = esito;
        this.giorniEdizione = giorniEdizione;
    }
}
