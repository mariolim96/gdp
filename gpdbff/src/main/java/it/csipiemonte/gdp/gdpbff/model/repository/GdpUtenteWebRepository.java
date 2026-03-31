package it.csipiemonte.gdp.gdpbff.model.repository;

import it.csipiemonte.gdp.gdpbff.model.entity.GdpUtenteWeb;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class GdpUtenteWebRepository implements PanacheRepository<GdpUtenteWeb> {

    public Optional<GdpUtenteWeb> findByCodiceFiscale(String codiceFiscale) {
        return find("codiceFiscale", codiceFiscale).firstResultOptional();
    }

    public Optional<GdpUtenteWeb> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
}
