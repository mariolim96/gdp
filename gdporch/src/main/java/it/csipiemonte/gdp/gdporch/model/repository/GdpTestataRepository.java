package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GdpTestataRepository implements PanacheRepositoryBase<GdpTestata, Integer> {

    public Optional<GdpTestata> findByCartellaTestata(String cartellaTestata) {
        return find("cartellaTestata", cartellaTestata).firstResultOptional();
    }

    public List<GdpTestata> findActiveSenders() {
        return list("invioEdizione", true);
    }

    public List<GdpTestata> findByProvincia(String provincia) {
        return list("provincia", provincia);
    }

    public List<Object[]> findDateAttese(LocalDate dataInizio,
            LocalDate dataFine,
            Long idTestata) {

        String query = """
                    SELECT
                        t.id,
                        t.nomeTestata,
                        d.dataAttesa,
                        d.sospesa
                    FROM TestataEntity t
                    JOIN PeriodicitaEntity p ON p.id = t.id
                    JOIN DataUscitaEntity d ON d.periodicita.id = p.id
                    WHERE
                        (:idTestata IS NULL OR t.id = :idTestata)
                    AND
                        (:idTestata IS NOT NULL OR t.invioEdizioni = 1)
                    AND
                        d.dataAttesa BETWEEN :dataInizio AND :dataFine
                """;

        return getEntityManager()
                .createQuery(query, Object[].class)
                .setParameter("idTestata", idTestata)
                .setParameter("dataInizio", dataInizio)
                .setParameter("dataFine", dataFine)
                .getResultList();
    }
}
