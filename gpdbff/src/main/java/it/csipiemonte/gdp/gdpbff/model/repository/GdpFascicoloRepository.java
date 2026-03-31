package it.csipiemonte.gdp.gdpbff.model.repository;

import it.csipiemonte.gdp.gdpbff.model.entity.GdpFascicolo;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class GdpFascicoloRepository implements PanacheRepository<GdpFascicolo> {

    public List<GdpFascicolo> findByUtente(Long fkGdpUtenteWeb) {
        return list("fkGdpUtenteWeb", fkGdpUtenteWeb);
    }
}
