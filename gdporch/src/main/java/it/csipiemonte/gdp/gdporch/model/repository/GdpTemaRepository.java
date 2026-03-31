package it.csipiemonte.gdp.gdporch.model.repository;

import it.csipiemonte.gdp.gdporch.model.entity.GdpTema;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GdpTemaRepository implements PanacheRepositoryBase<GdpTema, Integer> {
}
