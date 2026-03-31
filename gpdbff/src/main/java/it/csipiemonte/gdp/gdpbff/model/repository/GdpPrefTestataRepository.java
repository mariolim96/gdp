package it.csipiemonte.gdp.gdpbff.model.repository;

import it.csipiemonte.gdp.gdpbff.model.entity.GdpPrefTestata;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class GdpPrefTestataRepository implements PanacheRepository<GdpPrefTestata> {

    public List<GdpPrefTestata> findByUtente(Long fkGdpUtenteWeb) {
        return list("fkGdpUtenteWeb", fkGdpUtenteWeb);
    }
}
