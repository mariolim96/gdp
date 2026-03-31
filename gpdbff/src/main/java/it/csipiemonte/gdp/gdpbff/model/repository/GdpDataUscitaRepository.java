package it.csipiemonte.gdp.gdpbff.model.repository;

import it.csipiemonte.gdp.gdpbff.model.entity.GdpDataUscita;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class GdpDataUscitaRepository implements PanacheRepository<GdpDataUscita> {

    public List<GdpDataUscita> findExpectedByDate(LocalDate dataAttesa) {
        return list("dataAttesa = ?1 and sospesa = false", dataAttesa);
    }

    public List<GdpDataUscita> findByPeriodicitaInRange(Long fkGdpPeriodicita, LocalDate dataInizio, LocalDate dataFine) {
        return list("fkGdpPeriodicita = ?1 and dataAttesa >= ?2 and dataAttesa <= ?3", fkGdpPeriodicita, dataInizio, dataFine);
    }
}
