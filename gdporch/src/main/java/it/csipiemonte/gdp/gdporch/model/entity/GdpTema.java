package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "GDP_TEMA")
public class GdpTema extends PanacheEntityBase {

    @Id
    @Column(name = "COD_TEMA")
    public Integer id;

    @Column(name = "TEMA", nullable = false, length = 256)
    public String tema;
}
