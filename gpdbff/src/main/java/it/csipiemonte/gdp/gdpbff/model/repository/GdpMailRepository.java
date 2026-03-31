package it.csipiemonte.gdp.gdpbff.model.repository;

import it.csipiemonte.gdp.gdpbff.model.entity.GdpMail;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GdpMailRepository implements PanacheRepositoryBase<GdpMail, String> {
}
