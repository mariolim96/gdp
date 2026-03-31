package it.csipiemonte.gdp.gdpbff.model.repository;

import it.csipiemonte.gdp.gdpbff.model.entity.GdpLog;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class GdpLogRepository implements PanacheRepository<GdpLog> {

    public List<GdpLog> findByTestata(Long fkGdpTestata) {
        return list("fkGdpTestata", fkGdpTestata);
    }

    public List<GdpLog> findByTipo(String tipoAcquisizione) {
        return list("tipoAcquisizione", tipoAcquisizione);
    }
}
