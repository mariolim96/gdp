package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpUtenteSftp;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class GdpUtenteSftpRepository implements PanacheRepositoryBase<GdpUtenteSftp, Integer> {

    public Optional<GdpUtenteSftp> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    public GdpUtenteSftp findByRifTestata(String rifTestata) {
        return find("rifTestata", rifTestata).firstResult();
    }
}
