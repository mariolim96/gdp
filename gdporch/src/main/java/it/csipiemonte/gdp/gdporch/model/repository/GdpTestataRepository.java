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

<<<<<<< feat/f18
    public List<Object[]> findDateAttese(LocalDate dataInizio,
            LocalDate dataFine,
            Integer idTestata) {

        if (idTestata != null) {
            String query = """
                        SELECT
                            t.id,
                            t.cartellaTestata,
                            d.dataAttesa,
                            d.sospesa
                        FROM GdpTestata t,
                             GdpPeriodicita p,
                             GdpDataUscita d
                        WHERE
                            p.fkGdpTestata = t.id
                            AND d.fkGdpPeriodicita = p.id
                            AND t.id = :idTestata
                            AND d.dataAttesa BETWEEN :dataInizio AND :dataFine
                        ORDER BY t.id, d.dataAttesa
                    """;

            return getEntityManager()
                    .createQuery(query, Object[].class)
                    .setParameter("idTestata", idTestata)
                    .setParameter("dataInizio", dataInizio)
                    .setParameter("dataFine", dataFine)
                    .getResultList();
        } else {
            String query = """
                        SELECT
                            t.id,
                            t.cartellaTestata,
                            d.dataAttesa,
                            d.sospesa
                        FROM GdpTestata t,
                             GdpPeriodicita p,
                             GdpDataUscita d
                        WHERE
                            p.fkGdpTestata = t.id
                            AND d.fkGdpPeriodicita = p.id
                            AND t.invioEdizione = true
                            AND d.dataAttesa BETWEEN :dataInizio AND :dataFine
                        ORDER BY t.id, d.dataAttesa
                    """;

            return getEntityManager()
                    .createQuery(query, Object[].class)
                    .setParameter("dataInizio", dataInizio)
                    .setParameter("dataFine", dataFine)
                    .getResultList();
        }
=======
    public List<GdpTestata> findByInvioEdizione(Boolean invioEdizione) {
        return list("invioEdizione", invioEdizione);
    }

    public List<GdpTestata> findByCartella(String cartellaTestata) {
        return list("cartellaTestata", cartellaTestata);
>>>>>>> main
    }
}
