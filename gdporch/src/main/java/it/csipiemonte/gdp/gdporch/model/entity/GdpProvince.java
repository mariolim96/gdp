package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "GDP_PROVINCE")
public class GdpProvince extends PanacheEntityBase {

    @Id
    @Column(name = "SIGLA", length = 2)
    public String sigla;

    @Column(name = "PROVINCIA", nullable = false, length = 128)
    public String provincia;
}
