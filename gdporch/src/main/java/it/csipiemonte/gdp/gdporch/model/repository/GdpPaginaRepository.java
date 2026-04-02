package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpPagina;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GdpPaginaRepository implements PanacheRepositoryBase<GdpPagina, Integer> {

    public List<GdpPagina> findByEdizione(Integer fkGdpEdizione) {
        return list("fkGdpEdizione", fkGdpEdizione);
    }

    public Optional<GdpPagina> findByEdizioneAndNum(Integer fkGdpEdizione, Integer numPagina) {
        return find("fkGdpEdizione = ?1 and numPagina = ?2", fkGdpEdizione, numPagina).firstResultOptional();
    }
}
