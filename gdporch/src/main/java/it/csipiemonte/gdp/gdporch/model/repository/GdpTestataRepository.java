package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GdpTestataRepository implements PanacheRepository<GdpTestata> {

    public Optional<GdpTestata> findByCartellaTestata(String cartellaTestata) {
        return find("cartellaTestata", cartellaTestata).firstResultOptional();
    }

    public List<GdpTestata> findActiveSenders() {
        return list("invioEdizione", true);
    }

    public List<GdpTestata> findByProvincia(String provincia) {
        return list("provincia", provincia);
    }
}
