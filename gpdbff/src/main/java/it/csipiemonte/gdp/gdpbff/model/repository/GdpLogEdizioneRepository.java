package it.csipiemonte.gdp.gdpbff.model.repository;

import it.csipiemonte.gdp.gdpbff.model.entity.GdpLogEdizione;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GdpLogEdizioneRepository implements PanacheRepository<GdpLogEdizione> {

    public List<GdpLogEdizione> findByLog(Long fkGdpLog) {
        return list("fkGdpLog", fkGdpLog);
    }

    public Optional<GdpLogEdizione> findByEdizione(Long fkGdpEdizione) {
        return find("fkGdpEdizione", fkGdpEdizione).firstResultOptional();
    }
}
