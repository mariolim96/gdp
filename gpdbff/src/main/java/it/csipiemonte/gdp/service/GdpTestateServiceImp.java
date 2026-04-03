package it.csipiemonte.gdp.service;

import it.csipiemonte.gdp.gdpbff.model.dto.TestataSummaryList;
import it.csipiemonte.gdp.gdpbff.model.repository.GdpTestataRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GdpTestateServiceImp {
    private final GdpTestataRepository gdpTestataRepository;

    public GdpTestateServiceImp(GdpTestataRepository gdpTestataRepository) {
        this.gdpTestataRepository = gdpTestataRepository;
    }


    //Dto generati da swaggerOpenApi
    public TestataSummaryList getElencoTestate(){

    }
}
