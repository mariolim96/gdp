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

    public String findEmailByLogId(Integer idLog) {
        return find("SELECT u.email FROM GdpLog l, GdpUtenteSftp u " +
                "WHERE l.fkGdpUtenteFtp = u.id AND l.id = ?1", idLog)
                .project(String.class)
                .firstResult();
    }

    public String findNomeTestataByLogId(Integer idLog) {
        return find("SELECT t.nomeTestata FROM GdpLog l, GdpTestata t " +
                "WHERE l.fkGdpTestata = t.id AND l.id = ?1", idLog)
                .project(String.class)
                .firstResult();
    }

    public LocalDate findDataEdizioneByLogId(Integer idLog) {
        return find("SELECT e.dataEdizione FROM GdpLogEdizione le, GdpEdizione e " +
                "WHERE le.fkGdpEdizione = e.id AND le.fkGdpLog = ?1", idLog)
                .project(LocalDate.class)
                .firstResult();
    }

    public List<GdpLog> findByTipoAcquisizioneAndDataAcquisizione(TipoAcquisizione tipoAcquisizione,
            LocalDate dataAcquisizione) {
        return list("tipoAcquisizione = ?1 and date(dataAcquisizione) = ?2", tipoAcquisizione, dataAcquisizione);
    }
}
