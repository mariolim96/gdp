package it.csipiemonte.gdp.gdpbff.model.repository;

import it.csipiemonte.gdp.gdpbff.model.entity.GdpFascicoloPag;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class GdpFascicoloPagRepository implements PanacheRepository<GdpFascicoloPag> {

    public List<GdpFascicoloPag> findByFascicolo(Long fkGdpFascicolo) {
        return find("fkGdpFascicolo = ?1 and dataEliminazione is null order by posizione asc", fkGdpFascicolo).list();
    }
}
