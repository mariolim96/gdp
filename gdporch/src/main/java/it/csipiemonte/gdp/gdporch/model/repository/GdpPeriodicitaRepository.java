package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GdpPeriodicitaRepository implements PanacheRepository<GdpPeriodicita> {

    public List<GdpPeriodicita> findByTestata(Long fkGdpTestata) {
        return list("fkGdpTestata", fkGdpTestata);
    }

    public Optional<GdpPeriodicita> findFirstByTestata(Long fkGdpTestata) {
        return find("fkGdpTestata", fkGdpTestata).firstResultOptional();
    }
}
