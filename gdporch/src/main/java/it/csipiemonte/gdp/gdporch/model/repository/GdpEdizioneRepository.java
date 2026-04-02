package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpEdizione;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.Optional;

@ApplicationScoped
public class GdpEdizioneRepository implements PanacheRepositoryBase<GdpEdizione, Integer> {

    public Optional<GdpEdizione> findByTestataAndData(Integer fkGdpTestata, LocalDate dataEdizione) {
        return find("fkGdpTestata = ?1 and dataEdizione = ?2", fkGdpTestata, dataEdizione).firstResultOptional();
    }
}
