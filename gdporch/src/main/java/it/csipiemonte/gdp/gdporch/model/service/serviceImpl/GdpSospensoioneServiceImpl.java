package it.csipiemonte.gdp.gdporch.model.service.serviceImpl;

import java.util.List;

import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpSospensioneRequest;
import it.csipiemonte.gdp.gdporch.model.entity.GdpSospensioneResponse;
import it.csipiemonte.gdp.gdporch.model.repository.GdpDataUscitaRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpPeriodicitaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class GdpSospensoioneServiceImpl {
    
    @Inject
    GdpDataUscitaRepository dataUscitaRepository;
    
    @Inject
    GdpPeriodicitaRepository periodicitaRepository;
    
    @Transactional
    public GdpSospensioneResponse sospendi(GdpSospensioneRequest request) {
    
        // Validazione
        if (request.dataInizio.isAfter(request.dataFine)) {
            throw new IllegalArgumentException("Data inizio > data fine");
        }
    
        // 1. Recupero date
        List<GdpDataUscita> date = dataUscitaRepository.findByTestataAndRange(
                            request.idTestata,
                            request.dataInizio,
                            request.dataFine
        );
    
        if (date.isEmpty()) {
            return new GdpSospensioneResponse("MSG00001", 0);
        }
    
        // 2. Update periodicità
        GdpPeriodicita periodicita = periodicitaRepository.findByTestataId(request.idTestata);
    
        if (periodicita == null) {
            throw new RuntimeException("Periodicità non trovata");
        }
    
        periodicita.inizioSospensione = request.dataInizio;
        periodicita.fineSospensione = request.dataFine;
    
        // 3. Update sospesa
        for (GdpDataUscita d : date) {
            d.sospesa = true;
        }
    
        return new GdpSospensioneResponse("MSG00009", date.size());
    }
}
