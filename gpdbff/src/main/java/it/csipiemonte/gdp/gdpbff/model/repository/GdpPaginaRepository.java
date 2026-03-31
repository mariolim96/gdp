package it.csipiemonte.gdp.gdpbff.model.repository;

import it.csipiemonte.gdp.gdpbff.model.entity.GdpPagina;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GdpPaginaRepository implements PanacheRepository<GdpPagina> {

    public List<GdpPagina> findByEdizione(Long fkGdpEdizione) {
        return list("fkGdpEdizione", fkGdpEdizione);
    }

    public Optional<GdpPagina> findByEdizioneAndNum(Long fkGdpEdizione, Integer numPagina) {
        return find("fkGdpEdizione = ?1 and numPagina = ?2", fkGdpEdizione, numPagina).firstResultOptional();
    }
}
