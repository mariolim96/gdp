package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpCodaCaricamento;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class GdpCodaCaricamentoRepository implements PanacheRepository<GdpCodaCaricamento> {

    public List<GdpCodaCaricamento> findReadyToProcess() {
        return find("stato = 'READY' order by priorita asc, dataInserimento asc").list();
    }

    public List<GdpCodaCaricamento> findByLogEdizione(Long fkGdpLogEdizione) {
        return list("fkGdpLogEdizione", fkGdpLogEdizione);
    }
}
