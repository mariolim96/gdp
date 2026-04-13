package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpLog;
import it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione;
import it.csipiemonte.gdp.gdporch.model.projection.AcquisizioneRicercaProjection;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import it.csipiemonte.gdp.gdporch.model.enums.TipoAcquisizione;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
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

    public List<AcquisizioneRicercaProjection> searchAcquisizioni(
            TipoAcquisizione tipoAcquisizione,
            Integer idTestata,
            LocalDate dataDa,
            LocalDate dataAExclusive,
            TipoEdizione tipoEdizione) {
        TypedQuery<AcquisizioneRicercaProjection> query = getEntityManager().createQuery(
                "select new it.csipiemonte.gdp.gdporch.model.projection.AcquisizioneRicercaProjection(" +
                        "log.id, testata.id, testata.nomeTestata, logEdizione.tipoEdizione, edizione.dataEdizione, " +
                        "log.dataAcquisizione, logEdizione.nroPagAcquisite, logEdizione.nroPagValide) " +
                        "from GdpLog log " +
                        "join GdpLogEdizione logEdizione on logEdizione.fkGdpLog = log.id " +
                        "join GdpTestata testata on testata.id = log.fkGdpTestata " +
                        "join GdpEdizione edizione on edizione.id = logEdizione.fkGdpEdizione " +
                        "where log.tipoAcquisizione = :tipoAcquisizione " +
                        "and log.fkGdpTestata = :idTestata " +
                        "and log.dataAcquisizione >= :dataDa " +
                        "and log.dataAcquisizione < :dataAExclusive " +
                        "and logEdizione.tipoEdizione = :tipoEdizione " +
                        "order by log.dataAcquisizione desc, edizione.dataEdizione desc, log.id desc",
                AcquisizioneRicercaProjection.class);

        query.setParameter("tipoAcquisizione", tipoAcquisizione);
        query.setParameter("idTestata", idTestata);
        query.setParameter("dataDa", dataDa);
        query.setParameter("dataAExclusive", dataAExclusive);
        query.setParameter("tipoEdizione", tipoEdizione);
        return query.getResultList();
    }

    public List<GdpLog> findByTipoAcquisizioneAndDataAcquisizione(TipoAcquisizione tipoAcquisizione,
            LocalDate dataAcquisizione) {
        return list("tipoAcquisizione = ?1 and date(dataAcquisizione) = ?2", tipoAcquisizione, dataAcquisizione);
    }
}