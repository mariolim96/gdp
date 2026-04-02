package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class GdpDataUscitaRepository implements PanacheRepositoryBase<GdpDataUscita, Integer> {

    public List<GdpDataUscita> findExpectedByDate(LocalDate dataAttesa) {
        return list("dataAttesa = ?1 and sospesa = false", dataAttesa);
    }

    public List<GdpDataUscita> findByPeriodicitaInRange(Integer fkGdpPeriodicita, LocalDate dataInizio,
            LocalDate dataFine) {
        return list("fkGdpPeriodicita = ?1 and dataAttesa >= ?2 and dataAttesa <= ?3", fkGdpPeriodicita, dataInizio,
                dataFine);
    }

    public List<GdpDataUscita> findExpectedTomorrow() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return list("dataAttesa = ?1 and sospesa = false", tomorrow);
    }

    public List<GdpDataUscita> findByTestataAndRange(Integer idTestata, LocalDate start, LocalDate end) {
        return list("idTestata = ?1 and dataUscita between ?2 and ?3",
                idTestata, start, end);
    }
}
