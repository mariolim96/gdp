package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "GDP_TEMA")
public class GdpTema extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gdp_tema")
    @SequenceGenerator(name = "seq_gdp_tema", sequenceName = "seq_gdp_tema", allocationSize = 1)
    @Column(name = "COD_TEMA")
    public Integer id;

    @Column(name = "TEMA", nullable = false, length = 256)
    public String tema;
}
