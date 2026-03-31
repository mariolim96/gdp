package it.csipiemonte.gdp.gdporch.model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "GDP_EDIZIONE")
public class GdpEdizione extends PanacheEntityBase {

    @Id
    @Column(name = "ID_GDP_EDIZIONE")
    public Long id;

    @Column(name = "FK_GDP_TESTATA", nullable = false)
    public Long fkGdpTestata;

    @Column(name = "DATA_EDIZIONE", nullable = false)
    public LocalDate dataEdizione;

    @Column(name = "DATA_PUBBLICAZIONE", nullable = false)
    public LocalDate dataPubblicazione;

    @Column(name = "STATO")
    public Integer stato;

    @Column(name = "TOTALE_PAGINE", nullable = false)
    public Integer totalePagine;
}
