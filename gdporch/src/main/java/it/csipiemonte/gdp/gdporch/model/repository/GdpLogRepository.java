package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpLog;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import it.csipiemonte.gdp.gdporch.model.enums.TipoAcquisizione;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class GdpLogRepository implements PanacheRepositoryBase<GdpLog, Integer> {

    public List<GdpLog> findByTestata(Integer fkGdpTestata) {
        return list("fkGdpTestata", fkGdpTestata);
    }

    public List<GdpLog> findByTipo(TipoAcquisizione tipoAcquisizione) {
        return list("tipoAcquisizione", tipoAcquisizione);
    }

    public List<GdpLog> findByTipoAcquisizioneAndDataAcquisizione(TipoAcquisizione tipoAcquisizione,
            LocalDate dataAcquisizione) {
        return list("tipoAcquisizione = ?1 and date(dataAcquisizione) = ?2", tipoAcquisizione, dataAcquisizione);
    }
}
