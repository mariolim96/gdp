package it.csipiemonte.gdp.gdpbff.model.repository;

import it.csipiemonte.gdp.gdpbff.model.entity.GdpTema;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GdpTemaRepository implements PanacheRepositoryBase<GdpTema, Integer> {
}
