package it.csipiemonte.gdp.gdporch.dto;

import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;

public class ResponseDTO {
    public List<@Valid GdpDataUscita> uscite = new ArrayList<>();
    public  List<String> errori = new ArrayList<>();

    public List<GdpDataUscita> getUscite() {
        return uscite;
    }

    public void setUscite(List<GdpDataUscita> uscite) {
        this.uscite = uscite;
    }

    public List<String> getErrori() {
        return errori;
    }

    public void setErrori(List<String> errori) {
        this.errori = errori;
    }
}
