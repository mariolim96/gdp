package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpUtenteWeb;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class GdpUtenteWebRepository implements PanacheRepositoryBase<GdpUtenteWeb, Integer> {

    public Optional<GdpUtenteWeb> findByCodiceFiscale(String codiceFiscale) {
        return find("codiceFiscale", codiceFiscale).firstResultOptional();
    }

    public Optional<GdpUtenteWeb> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
}
