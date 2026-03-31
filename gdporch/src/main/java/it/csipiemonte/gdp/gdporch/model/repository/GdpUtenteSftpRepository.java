package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpUtenteSftp;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class GdpUtenteSftpRepository implements PanacheRepository<GdpUtenteSftp> {

    public Optional<GdpUtenteSftp> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }
}
