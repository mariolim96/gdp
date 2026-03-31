package it.csipiemonte.gdp.gdpbff.model.repository;

import it.csipiemonte.gdp.gdpbff.model.entity.GdpPrefEdizione;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class GdpPrefEdizioneRepository implements PanacheRepository<GdpPrefEdizione> {

    public List<GdpPrefEdizione> findByUtente(Long fkGdpUtenteWeb) {
        return list("fkGdpUtenteWeb", fkGdpUtenteWeb);
    }
}
