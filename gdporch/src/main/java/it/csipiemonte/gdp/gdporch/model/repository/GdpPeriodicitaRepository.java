package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GdpPeriodicitaRepository implements PanacheRepositoryBase<GdpPeriodicita, Integer> {

    public List<GdpPeriodicita> findByTestata(Integer fkGdpTestata) {
        return list("fkGdpTestata", fkGdpTestata);
    }

    public Optional<GdpPeriodicita> findFirstByTestata(Integer fkGdpTestata) {
        return find("fkGdpTestata", fkGdpTestata).firstResultOptional();
    }
}
