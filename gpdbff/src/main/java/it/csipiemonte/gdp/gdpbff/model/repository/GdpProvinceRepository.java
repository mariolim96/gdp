package it.csipiemonte.gdp.gdpbff.model.repository;

import it.csipiemonte.gdp.gdpbff.model.entity.GdpProvince;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GdpProvinceRepository implements PanacheRepositoryBase<GdpProvince, String> {
}
