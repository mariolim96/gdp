package it.csipiemonte.gdp.gdporch.service.impl;

import java.util.List;

import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.dto.SospensioneResponse;
import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import it.csipiemonte.gdp.gdporch.model.repository.GdpDataUscitaRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpPeriodicitaRepository;
import it.csipiemonte.gdp.gdporch.service.GdpSospensioneService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class GdpSospensioneServiceImpl implements GdpSospensioneService {
    
    @Inject
    GdpDataUscitaRepository dataUscitaRepository;
    
    @Inject
    GdpPeriodicitaRepository periodicitaRepository;
    
    @Override
    @Transactional
    public SospensioneResponse sospendi(Integer idTestata, DateRangeRequest request) {
    
        // Validation
        if (request.getDataInizio() == null || request.getDataFine() == null || idTestata == null) {
            throw new IllegalArgumentException("Missing required parameters");
        }
        
        if (request.getDataInizio().isAfter(request.getDataFine())) {
            throw new IllegalArgumentException("Data inizio > data fine");
        }
    
        // 1. Get Periodicita
        // Search for active periodicity (dataFineValidita IS NULL)
        GdpPeriodicita periodicita = periodicitaRepository.findActiveByTestata(idTestata);
    
        if (periodicita == null) {
            return new SospensioneResponse().message("MSG00001").giorniSospesi(0);
        }
    
        // 2. Query expected dates for this periodicity
        List<GdpDataUscita> date = dataUscitaRepository.findByPeriodicitaAndRange(
                            periodicita.id,
                            request.getDataInizio(),
                            request.getDataFine()
        );
    
        if (date.isEmpty()) {
            return new SospensioneResponse().message("MSG00001").giorniSospesi(0);
        }
    
        // 3. Update periodicity metadata (Inizio/Fine Sospensione)
        periodicita.inizioSospensione = request.getDataInizio();
        periodicita.fineSospensione = request.getDataFine();
    
        // 4. Set SOSPESA = True for all found dates
        for (GdpDataUscita d : date) {
            d.sospesa = true;
        }
    
        return new SospensioneResponse().message("MSG00009").giorniSospesi(date.size());
    }
}
