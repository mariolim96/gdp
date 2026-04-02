package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpLogEdizione;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GdpLogEdizioneRepository implements PanacheRepositoryBase<GdpLogEdizione, Integer> {

    public List<GdpLogEdizione> findByLog(Integer fkGdpLog) {
        return list("fkGdpLog", fkGdpLog);
    }

    public Optional<GdpLogEdizione> findByEdizione(Integer fkGdpEdizione) {
        return find("fkGdpEdizione", fkGdpEdizione).firstResultOptional();
    }
}
